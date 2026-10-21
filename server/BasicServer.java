package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import model.NguoiDung;
import model.Phim;
import protocol.Protocol;
import service.BasicService;
import service.CinemaService;
import service.PosterStorage;
import service.TicketService;
import service.PaymentService;

/**
 * TCP Server: giữ phiên đăng nhập và chuyển yêu cầu đến Service.
 */
public final class BasicServer {

    private static final int PORT = Integer.parseInt(
            System.getenv().getOrDefault("MOVIE_SERVER_PORT", "2040"));
    private final ServerMonitor monitor = new ServerMonitor();
    private final TicketService tickets = new TicketService();
    private final BasicService basic = new BasicService();
    private final CinemaService cinema = new CinemaService();
    private final PaymentService payments = new PaymentService();
    private final service.PasswordResetService passwordResets;
    private final Set<Session> sessions = ConcurrentHashMap.newKeySet();

    public BasicServer() {
        this(new service.PasswordResetService());
    }

    public BasicServer(service.PasswordResetService passwordResets) {
        this.passwordResets = java.util.Objects.requireNonNull(passwordResets);
    }

    public static void main(String[] args) {
        try {
            BasicServer app = new BasicServer();
            app.basic.verifyReady();
            app.cinema.rooms();
            app.tickets.verifyReady();
            app.payments.verifyReady();
            app.serve();
        } catch (SQLException ex) {
            System.err.println("MySQL chưa sẵn sàng. Hãy chạy database/schema.sql "
                    + "(database cũ: chạy thêm database/migrate_v2.sql) và kiểm tra MOVIE_DB_*.");
            System.err.println(ex.getMessage());
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("Không thể mở cổng " + PORT + ": " + ex.getMessage());
            System.exit(1);
        } catch (IllegalStateException ex) {
            System.err.println("Cấu hình thanh toán không hợp lệ: " + ex.getMessage());
            System.exit(1);
        }
    }

    private void serve() throws IOException {
        ExecutorService pool = Executors.newCachedThreadPool();
        java.util.concurrent.ScheduledExecutorService paymentPoller = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "payment-poller");
            t.setDaemon(true);
            return t;
        });
        paymentPoller.scheduleWithFixedDelay(this::synchronizePayments, 2, 4, java.util.concurrent.TimeUnit.SECONDS);
        try (ServerSocket listener = new ServerSocket(PORT)) {
            System.out.println("Galaxy Cinema Server đã sẵn sàng tại cổng " + PORT);
            while (true) {
                Socket socket = listener.accept();
                pool.execute(() -> handleClient(socket));
            }
        } finally {
            passwordResets.close();
            paymentPoller.shutdownNow();
            pool.shutdown();
        }
    }

    private void synchronizePayments() {
        try {
            for (int showId : payments.synchronizePending()) {
                seatsChanged(showId);

            }
        } catch (SQLException ex) {
            System.err.println("Không đồng bộ được thanh toán: " + ex.getMessage());
        }
    }

    private void handleClient(Socket socket) {
        Session session = null;
        try (Socket client = socket; BufferedReader input = new BufferedReader(new InputStreamReader(
                client.getInputStream(), StandardCharsets.UTF_8))) {
            session = new Session(client);
            sessions.add(session);
            String request;
            while ((request = input.readLine()) != null) {
                long started = System.nanoTime();
                List<String> response = process(request, session);
                long elapsed = System.nanoTime() - started;
                monitor.identity(session.monitorId, session.user, session.watchedShowId);
                monitor.completed(session.monitorId, request, response, elapsed);
                session.send(response);
            }
        } catch (IOException ex) {
            if (session != null) {
                monitor.connectionError(session.monitorId, "Lỗi đọc kết nối TCP");
            }
        } finally {
            if (session != null) {
                sessions.remove(session);
                session.close();
            }
        }
    }

    private void seatsChanged(int showId) {
        for (Session viewer : sessions) {
            if (viewer.watchedShowId == showId) {
                viewer.send(one("EVENT;SEATS_CHANGED;" + showId));
            }
        }
    }

    private List<String> process(String request, Session session) {
        String[] p = request.split(";", -1);
        String command = p[0].trim().toUpperCase(Locale.ROOT);
        try {
            switch (command) {
                case "REQUEST_PASSWORD_RESET":
                    count(p, 2);
                    passwordResets.request(dec(p[1]), session.socket.getInetAddress().getHostAddress());
                    return one("OK;PASSWORD_RESET_REQUESTED");
                case "RESET_PASSWORD":
                    count(p, 4);
                    int resetUserId = passwordResets.reset(dec(p[1]), dec(p[2]), dec(p[3]),
                            session.socket.getInetAddress().getHostAddress());
                    for (Session active : sessions) {
                        NguoiDung loggedIn = active.user;
                        if (loggedIn != null && loggedIn.getId() == resetUserId) {
                            active.user = null;
                            active.watchedShowId = 0;
                            if (active != session) {
                                active.close();
                            }
                        }
                    }
                    return one("OK;PASSWORD_RESET");
                case "REGISTER":
                    count(p, 5);
                    NguoiDung created = basic.register(dec(p[1]), dec(p[2]), dec(p[3]), dec(p[4]));
                    return one("OK;REGISTER;" + created.getId());
                case "LOGIN":
                    count(p, 3);
                    NguoiDung user = basic.login(dec(p[1]), dec(p[2]));
                    if (user == null) {
                        return error("Sai tài khoản hoặc mật khẩu");
                    }
                    session.watchedShowId = 0;
                    session.user = user;
                    return one("OK;LOGIN;" + user.getId() + ";" + enc(user.getHoTen())
                            + ";" + enc(user.getGmail()) + ";" + enc(user.getVaiTro()));
                case "LOGOUT":
                    count(p, 1);
                    session.watchedShowId = 0;
                    session.user = null;
                    return one("OK;LOGOUT");
                case "GET_PROFILE":
                    count(p, 1);
                    signedIn(session);
                    session.user = basic.profile(session.user.getId());
                    return rows("PROFILE", "PROFILE", Collections.singletonList(profileRow(session.user)));
                case "UPDATE_PROFILE":
                    count(p, 6);
                    signedIn(session);
                    session.user = basic.updateProfile(session.user.getId(), dec(p[1]), dec(p[2]), dec(p[3]),
                            dec(p[4]), dec(p[5]));
                    return rows("PROFILE", "PROFILE", Collections.singletonList(profileRow(session.user)));
                case "GET_MOVIES":
                    count(p, 1);
                    signedIn(session);
                    List<Phim> movies = basic.getMovies();
                    List<String> movieLines = new ArrayList<>();
                    movieLines.add("OK;MOVIES;" + movies.size());
                    for (Phim m : movies) {
                        movieLines.add("MOVIE;" + m.getId() + ";" + enc(m.getTenPhim()) + ";"
                                + enc(empty(m.getTheLoai())) + ";" + m.getThoiLuong() + ";"
                                + (m.getNgayKhoiChieu() == null ? "" : m.getNgayKhoiChieu())
                                + ";" + enc(empty(m.getMoTa())) + ";"
                                + PosterStorage.readBase64(m.getAnh()));
                    }
                    return movieLines;
                case "GET_SHOWS":
                    count(p, 2);
                    signedIn(session);
                    return rows("SHOWS", "SHOW", cinema.shows(number(p[1])));
                case "WATCH_SHOW":
                    count(p, 2);
                    signedIn(session);
                    int watchedId = number(p[1]);
                    if (watchedId < 0) {
                        throw new IllegalArgumentException("Mã suất chiếu không hợp lệ");
                    }
                    // Đăng ký trước khi đọc snapshot để không bỏ lỡ thay đổi trong lúc tải.
                    session.watchedShowId = watchedId;
                    if (watchedId == 0) {
                        return one("OK;WATCHING;0");
                    }
                    return rows("SEATS", "SEAT", cinema.seats(watchedId));
                case "GET_SEAT_MAP":
                    count(p, 2);
                    signedIn(session);
                    return rows("SEATS", "SEAT", cinema.seats(number(p[1])));
                case "GET_PRODUCTS":
                    count(p, 1);
                    signedIn(session);
                    return rows("PRODUCTS", "PRODUCT", payments.products());
                case "CREATE_ORDER":
                    count(p, 4);
                    signedIn(session);
                    int paymentShowId = number(p[1]);
                    String[] payment = payments.create(session.user.getId(), paymentShowId, dec(p[2]), dec(p[3]));
                    seatsChanged(paymentShowId); // Các client khác thấy ghế đang được giữ.
                    return rows("PAYMENT", "PAYMENT", Collections.singletonList(payment));
                case "GET_PAYMENT":
                    count(p, 2);
                    signedIn(session);
                    return rows("PAYMENT", "PAYMENT", payments.payment(session.user.getId(), longNumber(p[1])));
                case "DEMO_CONFIRM_PAYMENT":
                    count(p, 2);
                    signedIn(session);
                    int paidShowId = payments.demoConfirm(session.user.getId(), longNumber(p[1]));
                    if (paidShowId > 0) {
                        seatsChanged(paidShowId);
                    }
                    return one("OK;PAID;" + p[1]);
                case "BOOK_TICKET":
                    return error("Client cũ không hỗ trợ thanh toán. Hãy cập nhật ứng dụng.");
                case "GET_BOOKINGS":
                    count(p, 1);
                    signedIn(session);
                    return rows("BOOKINGS", "BOOKING", cinema.bookings(session.user.getId()));
                case "GET_TICKET":
                    count(p, 2);
                    signedIn(session);
                    long ticketId = longNumber(p[1]);
                    for (String[] row : cinema.bookings(session.user.getId())) {
                        if (Long.parseLong(row[0]) == ticketId) {
                            return rows("BOOKINGS", "BOOKING", Collections.singletonList(row));
                        }
                    }
                    return error("Không tìm thấy đơn đặt vé của bạn");
                case "GET_E_TICKET":
                    count(p, 2);
                    signedIn(session);
                    return rows("E_TICKET", "TICKET", Collections.singletonList(
                            tickets.ownTicket(session.user.getId(), longNumber(p[1]))));
                case "INSPECT_TICKET":
                    count(p, 2);
                    staff(session);
                    return rows("E_TICKET", "TICKET", Collections.singletonList(tickets.inspect(dec(p[1]))));
                case "CHECK_IN":
                    count(p, 2);
                    staff(session);
                    return rows("CHECKED_IN", "TICKET", Collections.singletonList(
                            tickets.checkIn(session.user.getId(), dec(p[1]))));
                case "ADMIN_MONITOR":
                    count(p, 1);
                    admin(session);
                    return monitor.snapshot();
                case "CANCEL_TICKET":
                    count(p, 2);
                    signedIn(session);
                    seatsChanged(cinema.cancel(session.user.getId(), longNumber(p[1])));
                    return one("OK;CANCELLED");
                case "ADMIN_GET_ROOMS":
                    count(p, 1);
                    admin(session);
                    return rows("ROOMS", "ROOM", cinema.rooms());
                case "ADMIN_GET_SHOWS":
                    count(p, 1);
                    admin(session);
                    return rows("SHOWS", "SHOW", cinema.shows(0));
                case "ADMIN_GET_USERS":
                    count(p, 1);
                    admin(session);
                    return rows("USERS", "USER", cinema.users());
                case "ADMIN_GET_BOOKINGS":
                    count(p, 1);
                    admin(session);
                    return rows("BOOKINGS", "BOOKING", cinema.allBookings());
                case "ADMIN_CANCEL_TICKET":
                    count(p, 2);
                    admin(session);
                    seatsChanged(cinema.adminCancel(longNumber(p[1])));
                    return one("OK;CANCELLED");
                case "ADMIN_GET_STATS":
                    count(p, 1);
                    admin(session);
                    return rows("STATS", "STAT", Collections.singletonList(cinema.stats()));
                case "ADMIN_GET_REVENUE":
                    count(p, 1);
                    admin(session);
                    return rows("REVENUE", "MOVIE_REVENUE", cinema.revenueByMovie());
                case "ADMIN_GET_PRODUCTS":
                    count(p, 1);
                    admin(session);
                    return rows("PRODUCTS", "PRODUCT", payments.allProducts());
                case "ADMIN_ADD_PRODUCT":
                    count(p, 4);
                    admin(session);
                    return one("OK;ADDED;" + payments.addProduct(dec(p[1]), p[2], p[3]));
                case "ADMIN_UPDATE_PRODUCT":
                    count(p, 6);
                    admin(session);
                    payments.updateProduct(number(p[1]), dec(p[2]), p[3], p[4], p[5]);
                    return one("OK;UPDATED");
                case "ADMIN_DELETE_PRODUCT":
                    count(p, 2);
                    admin(session);
                    payments.deleteProduct(number(p[1]));
                    return one("OK;DELETED");
                case "ADMIN_ADD_MOVIE":
                    if (p.length != 5 && p.length != 6) {
                        throw new IllegalArgumentException("Số tham số không hợp lệ");
                    }
                    admin(session);
                    return one("OK;ADDED;" + cinema.addMovie(
                            dec(p[1]), dec(p[2]), number(p[3]), dec(p[4]),
                            p.length == 6 ? p[5] : ""));
                case "ADMIN_UPDATE_MOVIE":
                    if (p.length != 6 && p.length != 7) {
                        throw new IllegalArgumentException("Số tham số không hợp lệ");
                    }
                    admin(session);
                    cinema.updateMovie(number(p[1]), dec(p[2]), dec(p[3]),
                            optionalNumber(p[4]), dec(p[5]), p.length == 7 ? p[6] : "");
                    return one("OK;UPDATED");
                case "ADMIN_DELETE_MOVIE":
                    count(p, 2);
                    admin(session);
                    cinema.deleteMovie(number(p[1]));
                    return one("OK;DELETED");
                case "ADMIN_ADD_ROOM":
                    count(p, 4);
                    admin(session);
                    return one("OK;ADDED;" + cinema.addRoom(dec(p[1]),
                            number(p[2]), number(p[3])));
                case "ADMIN_DELETE_ROOM":
                    count(p, 2);
                    admin(session);
                    cinema.deleteRoom(number(p[1]));
                    return one("OK;DELETED");
                case "ADMIN_UPDATE_ROOM":
                    if (p.length != 3 && p.length != 5) {
                        throw new IllegalArgumentException("Số tham số không hợp lệ");
                    }
                    admin(session);
                    if (p.length == 3) {
                        cinema.updateRoom(number(p[1]), dec(p[2]));
                    } else {
                        cinema.updateRoom(number(p[1]), dec(p[2]),
                                optionalNumber(p[3]), optionalNumber(p[4]));
                    }
                    return one("OK;UPDATED");
                case "ADMIN_ADD_SHOW":
                    count(p, 5);
                    admin(session);
                    return one("OK;ADDED;" + cinema.addShow(number(p[1]), number(p[2]),
                            showTime(p[3]), new BigDecimal(p[4])));
                case "ADMIN_DELETE_SHOW":
                    count(p, 2);
                    admin(session);
                    cinema.deleteShow(number(p[1]));
                    return one("OK;DELETED");
                case "ADMIN_UPDATE_SHOW":
                    count(p, 6);
                    admin(session);
                    cinema.updateShow(number(p[1]), number(p[2]), number(p[3]),
                            showTime(p[4]), new BigDecimal(p[5]));
                    return one("OK;UPDATED");
                default:
                    return error("Lệnh không được hỗ trợ");
            }
        } catch (IllegalArgumentException ex) {
            return error(ex.getMessage() == null ? "Tham số không hợp lệ" : ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Lỗi database: " + ex.getMessage());
            return error("Không thể truy cập database");
        } catch (RuntimeException ex) {
            System.err.println("Lỗi xử lý yêu cầu: " + ex.getMessage());
            return error("Không thể xử lý yêu cầu");
        }
    }

    private static List<String> rows(String kind, String tag, List<String[]> data) {
        List<String> lines = new ArrayList<>();
        lines.add("OK;" + kind + ";" + data.size());
        for (String[] row : data) {
            StringBuilder line = new StringBuilder(tag);
            for (String value : row) {
                line.append(';').append(enc(empty(value)));
            }
            lines.add(line.toString());
        }
        return lines;
    }

    private static String[] profileRow(NguoiDung user) {
        return new String[]{Integer.toString(user.getId()), user.getHoTen(), user.getGmail(),
            user.getSoDienThoai(), user.getVaiTro()};
    }

    private static void count(String[] p, int expected) {
        if (p.length != expected) {
            throw new IllegalArgumentException("Số tham số không hợp lệ");
        }
    }

    private static void signedIn(Session session) {
        if (session.user == null) {
            throw new IllegalArgumentException("Bạn cần đăng nhập");
        }
    }

    private static void staff(Session session) {
        signedIn(session);
        String role = session.user.getVaiTro();
        if (!"admin".equals(role) && !"staff".equals(role)) {
            throw new IllegalArgumentException("Chỉ nhân viên hoặc quản trị viên được soát vé");
        }
    }

    private static void admin(Session session) {
        signedIn(session);
        if (!"admin".equals(session.user.getVaiTro())) {
            throw new IllegalArgumentException("Chỉ quản trị viên được thực hiện thao tác này");
        }
    }

    private static int number(String text) {
        return Integer.parseInt(text);
    }

    private static long longNumber(String text) {
        return Long.parseLong(text);
    }

    private static Integer optionalNumber(String text) {
        return text.isEmpty() ? null : number(text);
    }

    private static LocalDateTime showTime(String text) {
        String value = text.trim();
        try {
            if (value.indexOf('/') >= 0) {
                DateTimeFormatter vietnamese = DateTimeFormatter
                        .ofPattern("dd/MM/uuuu HH:mm")
                        .withResolverStyle(ResolverStyle.STRICT);
                return LocalDateTime.parse(value, vietnamese);
            }
            return LocalDateTime.parse(value.replace(' ', 'T'));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Thời gian không hợp lệ. Ví dụ: "
                    + "2026-10-06T19:30, 2026-10-06 19:30 hoặc 06/10/2026 19:30");
        }
    }

    private static String enc(String value) {
        return Protocol.encode(value);
    }

    private static String dec(String value) {
        return Protocol.decode(value);
    }

    private static String empty(String value) {
        return value == null ? "" : value;
    }

    private static List<String> one(String line) {
        return Arrays.asList(line);
    }

    private static List<String> error(String message) {
        return one("ERR;" + message.replace(';', ',').replace('\n', ' '));
    }

    private final class Session implements AutoCloseable {

        private volatile NguoiDung user;
        private final long monitorId;
        private volatile int watchedShowId;
        private final Socket socket;
        private final BufferedWriter output;
        private final BlockingQueue<List<String>> outgoing = new ArrayBlockingQueue<>(64);
        private final AtomicBoolean closed = new AtomicBoolean();
        private final Thread writer;

        private Session(Socket socket) throws IOException {
            this.socket = socket;
            socket.setTcpNoDelay(true);
            output = new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8));
            monitorId = monitor.connected(socket.getRemoteSocketAddress().toString());
            writer = new Thread(this::writeLoop, "cinema-writer-" + socket.getPort());
            writer.setDaemon(true);
            writer.start();
        }

        private void send(List<String> frame) {
            // Không để client chậm làm kẹt thread xử lý đặt vé hoặc broadcast.
            if (!closed.get() && !outgoing.offer(new ArrayList<>(frame))) {
                monitor.connectionError(monitorId, "Client chậm: hàng đợi gửi đã đầy");
                close();
            }
        }

        private void writeLoop() {
            try {
                while (!closed.get()) {
                    List<String> frame = outgoing.take();
                    // Một writer duy nhất: response và event không xen dòng vào nhau.
                    for (String line : frame) {
                        output.write(line);
                        output.newLine();
                    }
                    output.write("END");
                    output.newLine();
                    output.flush();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (IOException ex) {
                monitor.connectionError(monitorId, "Lỗi ghi kết nối TCP");
            } finally {
                close();
            }
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                monitor.disconnected(monitorId);
                watchedShowId = 0;
                outgoing.clear();
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
                writer.interrupt();
            }
        }
    }
}
