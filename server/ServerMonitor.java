package server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import model.NguoiDung;
import protocol.Protocol;

/**
 * Bộ đếm theo vòng đời process; nhật ký 500 dòng trong RAM, không lưu request
 * thô.
 */
public final class ServerMonitor {

    private static final Set<String> COMMANDS = new HashSet<>(Arrays.asList(
            "REGISTER", "LOGIN", "LOGOUT", "REQUEST_PASSWORD_RESET", "RESET_PASSWORD", "GET_MOVIES", "GET_SHOWS", "WATCH_SHOW", "GET_SEAT_MAP",
            "BOOK_TICKET", "GET_BOOKINGS", "GET_TICKET", "GET_E_TICKET", "INSPECT_TICKET", "CHECK_IN",
            "GET_PRODUCTS", "CREATE_ORDER", "GET_PAYMENT", "DEMO_CONFIRM_PAYMENT", "GET_PROFILE", "UPDATE_PROFILE",
            "CANCEL_TICKET", "ADMIN_MONITOR", "ADMIN_GET_ROOMS", "ADMIN_GET_SHOWS", "ADMIN_GET_USERS",
            "ADMIN_GET_BOOKINGS", "ADMIN_CANCEL_TICKET", "ADMIN_GET_STATS", "ADMIN_GET_REVENUE",
            "ADMIN_ADD_MOVIE", "ADMIN_UPDATE_MOVIE", "ADMIN_DELETE_MOVIE", "ADMIN_ADD_ROOM",
            "ADMIN_UPDATE_ROOM", "ADMIN_DELETE_ROOM", "ADMIN_ADD_SHOW", "ADMIN_UPDATE_SHOW", "ADMIN_DELETE_SHOW",
            "ADMIN_GET_PRODUCTS", "ADMIN_ADD_PRODUCT", "ADMIN_UPDATE_PRODUCT", "ADMIN_DELETE_PRODUCT"));
    private final Map<Long, Client> clients = new LinkedHashMap<>();
    private final Deque<String[]> logs = new ArrayDeque<>();
    private final long startedNanos = System.nanoTime();
    private final String startedAt = timestamp();
    private long nextId, sequence, requests, failures, totalNanos;

    public synchronized long connected(String address) {
        long id = ++nextId;
        clients.put(id, new Client(id, address));
        log(id, "CONNECTED", "Kết nối mới");
        return id;
    }

    public synchronized void identity(long id, NguoiDung user, int showId) {
        Client c = clients.get(id);
        if (c != null) {
            c.user = user == null ? "-" : Integer.toString(user.getId());
            c.role = user == null ? "-" : user.getVaiTro();
            c.show = Integer.toString(showId);
        }
    }

    public synchronized void completed(long id, String request, List<String> response, long nanos) {
        // Chỉ tên lệnh đã biết được ghi. Mật khẩu, token QR, email, payload không vào log.
        int delimiter = request.indexOf(';');
        String candidate = request.substring(0, delimiter < 0 ? request.length() : delimiter)
                .trim().toUpperCase(Locale.ROOT);
        String command = COMMANDS.contains(candidate) ? candidate : "UNKNOWN";
        boolean failed = response.isEmpty() || !response.get(0).startsWith("OK;");
        // Lệnh tự làm mới màn hình giám sát không phải thao tác nghiệp vụ.
        if ("ADMIN_MONITOR".equals(command)) {
            return;
        }
        requests++;
        if (failed) {
            failures++;
        }
        totalNanos += nanos;
        Client c = clients.get(id);
        if (c != null) {
            c.requests++;
            c.nanos += nanos;
        }
        if (failed || command.equals("BOOK_TICKET") || command.equals("CREATE_ORDER")
                || command.equals("DEMO_CONFIRM_PAYMENT") || command.contains("CANCEL")
                || command.equals("CHECK_IN") || command.equals("LOGIN") || command.equals("LOGOUT")
                || command.equals("UPDATE_PROFILE") || command.contains("PASSWORD_RESET") || command.equals("RESET_PASSWORD")
                || (command.startsWith("ADMIN_") && command.contains("PRODUCT"))) {
            String detail = failed ? "Từ chối / lỗi" : "Thành công";
            if (!failed && command.equals("BOOK_TICKET")) {
                String[] fields = response.get(0).split(";");
                if (fields.length == 3 && fields[2].matches("[0-9]+")) {
                    detail += " — đơn #" + fields[2];
                }
            } else if (!failed && command.equals("CHECK_IN") && response.size() > 1) {
                String[] fields = response.get(1).split(";");
                if (fields.length > 1) {
                    String bookingId = Protocol.decode(fields[1]);
                    if (bookingId.matches("[0-9]+")) {
                        detail += " — đơn #" + bookingId;
                    }
                }
            } else if (!failed && command.contains("CANCEL")) {
                String[] fields = request.split(";", -1);
                if (fields.length == 2 && fields[1].matches("[0-9]{1,19}")) {
                    detail += " — đơn #" + fields[1];
                }
            }
            log(id, command, detail + " (" + ms(nanos) + " ms)");
        }
    }

    public synchronized void connectionError(long id, String reason) {
        // Caller chỉ dùng mô tả cố định, không truyền exception/request.
        log(id, "CONNECTION_ERROR", reason);
    }

    public synchronized void disconnected(long id) {
        if (clients.remove(id) != null) {
            log(id, "DISCONNECTED", "Đã đóng kết nối");
        }
    }

    public synchronized List<String> snapshot() {
        List<String> lines = new ArrayList<>();
        lines.add("OK;MONITOR");
        lines.add(row("SUMMARY", Integer.toString(clients.size()), Long.toString(requests),
                Long.toString(failures), ms(requests == 0 ? 0 : totalNanos / requests),
                Long.toString((System.nanoTime() - startedNanos) / 1_000_000_000), startedAt));
        for (Client c : clients.values()) {
            lines.add(row("CLIENT", Long.toString(c.id), c.address, c.since, c.user, c.role,
                    c.show, Long.toString(c.requests), ms(c.requests == 0 ? 0 : c.nanos / c.requests)));
        }
        for (String[] log : logs) {
            lines.add(row("LOG", log));
        }
        return lines;
    }

    private void log(long clientId, String event, String detail) {
        if (logs.size() == 500) {
            logs.removeFirst();
        }
        logs.addLast(new String[]{Long.toString(++sequence), timestamp(),
            Long.toString(clientId), event, detail});
    }

    private static String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private static String ms(long nanos) {
        return String.format(Locale.ROOT, "%.2f", nanos / 1_000_000.0);
    }

    private static String row(String tag, String... fields) {
        StringBuilder line = new StringBuilder(tag);
        for (String value : fields) {
            line.append(';').append(Protocol.encode(value));
        }
        return line.toString();
    }

    private static final class Client {

        final long id;
        final String address, since = timestamp();
        String user = "-", role = "-", show = "0";
        long requests, nanos;

        Client(long id, String address) {
            this.id = id;
            this.address = address;
        }
    }
}
