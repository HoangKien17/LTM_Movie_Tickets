package service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import model.NguoiDung;
import repository.NguoiDungRepository;
import utils.PasswordUtil;

/** Server-only, bounded reset state. No OTP or SMTP secret is returned to a client. */
public final class PasswordResetService implements AutoCloseable {
    public interface Accounts {
        NguoiDung find(String email) throws SQLException;
        boolean replace(NguoiDung expected, String hash) throws SQLException;
    }
    public interface Mailer {
        default void verifyConfiguration() { }
        void send(String email, String code) throws Exception;
    }
    public static final String REQUEST_MESSAGE = "Nếu email đã được đăng ký, mã khôi phục sẽ được gửi tới hộp thư. Hãy kiểm tra cả thư rác.";
    private static final long MINUTE = 60_000, TTL = 5 * MINUTE, HOUR = 60 * MINUTE;
    private static final int CAPACITY = 10_000;
    private static final String INVALID = "Mã không đúng, đã hết hạn hoặc đã dùng. Hãy kiểm tra email hoặc yêu cầu mã mới.";
    private final Accounts accounts;
    private final Mailer mailer;
    private final LongSupplier clock;
    private final Executor delivery;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Challenge> challenges = new HashMap<>();
    private final Map<String, Rate> rates = new HashMap<>();

    public PasswordResetService() {
        this(repositoryAccounts(), new SmtpResetMailer(), System::currentTimeMillis,
                new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(32), r -> {
                    Thread t = new Thread(r, "password-reset-mail"); t.setDaemon(true); return t;
                }, new ThreadPoolExecutor.AbortPolicy()));
    }
    public PasswordResetService(Accounts accounts, Mailer mailer, LongSupplier clock, Executor delivery) {
        this.accounts = accounts; this.mailer = mailer; this.clock = clock; this.delivery = delivery;
    }
    private static Accounts repositoryAccounts() {
        NguoiDungRepository repository = new NguoiDungRepository();
        return new Accounts() {
            public NguoiDung find(String email) throws SQLException { return repository.findByEmail(email); }
            public boolean replace(NguoiDung expected, String hash) throws SQLException {
                return repository.replacePassword(expected, hash);
            }
        };
    }

    public void request(String rawEmail, String source) {
        String email = email(rawEmail);
        mailer.verifyConfiguration();
        final String code;
        byte[] salt = new byte[16]; random.nextBytes(salt);
        final Challenge challenge;
        synchronized (this) {
            long now = clock.getAsLong(); cleanup(now);
            Rate address = rate("email:" + email, now), peer = rate("ip:" + source, now);
            if (address.sends > 0 && now - address.lastSend < MINUTE) {
                throw new IllegalArgumentException("Vui lòng chờ 60 giây trước khi yêu cầu mã tiếp theo.");
            }
            if (address.sends >= 6 || peer.sends >= 30) {
                throw new IllegalArgumentException("Đã đạt giới hạn gửi mã. Vui lòng thử lại sau.");
            }
            if (challenges.size() >= CAPACITY && !challenges.containsKey(email)) {
                throw new IllegalArgumentException("Hệ thống đang bận. Vui lòng thử lại sau.");
            }
            Challenge previous = challenges.get(email);
            String fresh;
            do { fresh = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000)); }
            while (previous != null && MessageDigest.isEqual(previous.hash, digest(previous.salt, fresh)));
            code = fresh;
            challenge = new Challenge(salt, digest(salt, code), now + TTL);
            challenges.put(email, challenge);
            address.sends++; address.lastSend = now; peer.sends++; peer.lastSend = now;
        }
        try {
            // Lookup and delivery happen off the request thread for both known/unknown emails.
            delivery.execute(() -> deliver(email, code, challenge));
        } catch (RejectedExecutionException ex) {
            synchronized (this) { challenges.remove(email, challenge); }
            throw new IllegalArgumentException("Hàng đợi gửi mail đang bận. Vui lòng thử lại sau.");
        }
    }

    private void deliver(String email, String code, Challenge challenge) {
        try {
            NguoiDung user = accounts.find(email);
            if (user == null) return;
            synchronized (this) {
                if (challenges.get(email) != challenge || clock.getAsLong() >= challenge.expires) return;
            }
            mailer.send(user.getGmail(), code);
            synchronized (this) {
                if (challenges.get(email) == challenge) challenge.account = user;
            }
        } catch (Exception ex) {
            synchronized (this) { challenges.remove(email, challenge); }
            // Fixed diagnostic only: credentials, email addresses and OTPs never enter logs.
            System.err.println("Không gửi được email khôi phục. Hãy kiểm tra SMTP/database trên server.");
        }
    }

    public synchronized int reset(String rawEmail, String code, String newPassword, String source) throws SQLException {
        String email = email(rawEmail);
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 128) {
            throw new IllegalArgumentException("Mật khẩu mới phải có từ 6 đến 128 ký tự.");
        }
        long now = clock.getAsLong(); cleanup(now);
        Rate peer = rate("ip:" + source, now);
        if (++peer.attempts > 60) throw new IllegalArgumentException("Đã thử quá nhiều lần. Vui lòng thử lại sau.");
        Challenge challenge = challenges.get(email);
        if (challenge == null || challenge.account == null) throw new IllegalArgumentException(INVALID);
        challenge.attempts++;
        boolean valid = code != null && code.matches("[0-9]{6}")
                && MessageDigest.isEqual(challenge.hash, digest(challenge.salt, code));
        if (!valid) {
            if (challenge.attempts >= 5) challenges.remove(email);
            throw new IllegalArgumentException(INVALID);
        }
        String hash = PasswordUtil.hashPassword(newPassword);
        if (hash == null) throw new IllegalStateException("Không thể xử lý mật khẩu");
        if (!accounts.replace(challenge.account, hash)) {
            challenges.remove(email);
            throw new IllegalArgumentException(INVALID);
        }
        challenges.remove(email);
        return challenge.account.getId();
    }

    private Rate rate(String key, long now) {
        Rate value = rates.get(key);
        if (value == null) {
            if (rates.size() >= CAPACITY) throw new IllegalArgumentException("Hệ thống đang bận. Vui lòng thử lại sau.");
            value = new Rate(now); rates.put(key, value);
        }
        return value;
    }
    private void cleanup(long now) {
        challenges.values().removeIf(c -> now >= c.expires);
        Iterator<Rate> iterator = rates.values().iterator();
        while (iterator.hasNext()) if (now - iterator.next().started >= HOUR) iterator.remove();
    }
    private static String email(String value) {
        if (value == null || value.trim().length() > 100 || !PasswordUtil.isValidEmail(value.trim())) {
            throw new IllegalArgumentException("Hãy nhập địa chỉ email hợp lệ đã đăng ký.");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
    private static byte[] digest(byte[] salt, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt); return digest.digest(code.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    @Override public void close() {
        if (delivery instanceof ExecutorService) ((ExecutorService) delivery).shutdownNow();
    }
    private static final class Challenge {
        final byte[] salt, hash;
        final long expires;
        int attempts;
        NguoiDung account;
        Challenge(byte[] salt, byte[] hash, long expires) { this.salt = salt; this.hash = hash; this.expires = expires; }
    }
    private static final class Rate {
        final long started;
        long lastSend;
        int sends, attempts;
        Rate(long started) { this.started = started; }
    }
}
