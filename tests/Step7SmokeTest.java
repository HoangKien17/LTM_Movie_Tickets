package tests;

import protocol.Protocol;
import web.MovieClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Kiểm thử qua TCP Server thật. Cần hai tài khoản thường và một suất chiếu còn ghế.
 * Bài kiểm thử đặt một ghế rồi hủy trong phần dọn dẹp.
 */
public final class Step7SmokeTest {
    private Step7SmokeTest() { }

    public static void main(String[] args) throws Exception {
        String user1 = required("MOVIE_TEST_USER_1");
        String pass1 = required("MOVIE_TEST_PASSWORD_1");
        String user2 = required("MOVIE_TEST_USER_2");
        String pass2 = required("MOVIE_TEST_PASSWORD_2");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        MovieClient winner = null;
        long bookingId = 0;
        boolean cancelled = false;

        try (MovieClient client1 = connect(); MovieClient client2 = connect()) {
            try {
                expectError(client1.request("GET_BOOKINGS"), "Chưa đăng nhập phải bị từ chối");
                expectError(client1.request("ADMIN_GET_USERS"), "Quản trị chưa đăng nhập phải bị từ chối");
                expectError(client1.request("REGISTER;" + encode("") + ";" + encode("invalid")
                        + ";" + encode("123") + ";" + encode("123456")),
                        "Đăng ký sai dữ liệu phải bị từ chối");
                System.out.println("[1/7] Kiểm tra chưa đăng nhập và dữ liệu đăng ký sai: OK");

                expectError(client1.request("LOGIN;" + encode(user1) + ";" + encode("mat-khau-sai")),
                        "Sai mật khẩu phải bị từ chối");
                int id1 = login(client1, user1, pass1);
                int id2 = login(client2, user2, pass2);
                check(id1 != id2, "Hai tài khoản kiểm thử phải khác nhau");
                expectError(client1.request("ADMIN_GET_USERS"),
                        "Tài khoản thường không được xem danh sách người dùng");
                System.out.println("[2/7] Đăng nhập và phân quyền: OK");

                List<String> movies = client1.request("GET_MOVIES");
                expectOk(movies, "MOVIES");
                check(movies.size() > 1, "Không có phim trong database");
                SeatTarget target = findFreeSeat(client1);
                System.out.println("[3/7] Phim, suất chiếu và sơ đồ ghế: OK");

                expectError(client1.request("BOOK_TICKET;" + target.showId + ";"
                        + encode(target.seatCode + "," + target.seatCode)),
                        "Một ghế bị lặp trong cùng yêu cầu phải bị từ chối");
                System.out.println("[4/7] Kiểm tra ghế lặp: OK");

                String bookCommand = "BOOK_TICKET;" + target.showId + ";" + encode(target.seatCode);
                CountDownLatch start = new CountDownLatch(1);
                Future<List<String>> first = pool.submit(() -> {
                    start.await();
                    return client1.request(bookCommand);
                });
                Future<List<String>> second = pool.submit(() -> {
                    start.await();
                    return client2.request(bookCommand);
                });
                start.countDown();
                List<String> result1 = first.get(30, TimeUnit.SECONDS);
                List<String> result2 = second.get(30, TimeUnit.SECONDS);
                boolean firstWon = isOk(result1, "BOOKED");
                boolean secondWon = isOk(result2, "BOOKED");
                check(firstWon != secondWon,
                        "Hai Client đặt cùng một ghế: phải đúng một thành công. Kết quả: "
                                + result1 + " / " + result2);
                expectError(firstWon ? result2 : result1,
                        "Client thua phải nhận lỗi ghế đã đặt");
                winner = firstWon ? client1 : client2;
                MovieClient loser = firstWon ? client2 : client1;
                List<String> won = firstWon ? result1 : result2;
                bookingId = Long.parseLong(won.get(0).split(";", -1)[2]);
                System.out.println("[5/7] Hai Client đặt cùng ghế, chỉ một thành công: OK");

                List<String> detail = winner.request("GET_TICKET;" + bookingId);
                expectOk(detail, "BOOKINGS");
                check(detail.size() == 2, "Không tìm thấy đơn vừa đặt");
                String[] fields = detail.get(1).split(";", -1);
                check(fields.length == 9 && Protocol.decode(fields[8]).contains(target.seatCode),
                        "Chi tiết vé không chứa ghế đã đặt");
                expectError(loser.request("GET_TICKET;" + bookingId),
                        "Tài khoản khác không được xem vé");
                expectError(loser.request("CANCEL_TICKET;" + bookingId),
                        "Tài khoản khác không được hủy vé");
                check("taken".equals(seatState(winner, target)),
                        "Ghế phải chuyển sang đã đặt");
                System.out.println("[6/7] Chi tiết vé, quyền sở hữu và trạng thái ghế: OK");

                expectOk(winner.request("CANCEL_TICKET;" + bookingId), "CANCELLED");
                cancelled = true;
                expectError(winner.request("CANCEL_TICKET;" + bookingId),
                        "Không được hủy cùng đơn hai lần");
                check("free".equals(seatState(winner, target)),
                        "Ghế phải trống trở lại sau khi hủy");
                expectOk(client1.request("LOGOUT"), "LOGOUT");
                expectError(client1.request("GET_BOOKINGS"),
                        "Sau đăng xuất không được xem vé");
                expectOk(client2.request("LOGOUT"), "LOGOUT");
                System.out.println("[7/7] Hủy vé, mở lại ghế và đăng xuất: OK");
                System.out.println("BƯỚC 7: TẤT CẢ KIỂM THỬ TỰ ĐỘNG ĐÃ ĐẠT");
            } finally {
                if (winner != null && bookingId > 0 && !cancelled) {
                    try {
                        winner.request("CANCEL_TICKET;" + bookingId);
                        System.out.println("Đã hủy đơn kiểm thử #" + bookingId);
                    } catch (Exception ex) {
                        System.err.println("Hãy hủy thủ công đơn kiểm thử #" + bookingId
                                + ": " + ex.getMessage());
                    }
                }
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static SeatTarget findFreeSeat(MovieClient client) throws Exception {
        List<String> shows = client.request("GET_SHOWS;0");
        expectOk(shows, "SHOWS");
        for (int i = 1; i < shows.size(); i++) {
            String[] row = shows.get(i).split(";", -1);
            check(row.length == 7 && "SHOW".equals(row[0]), "Dữ liệu suất chiếu không hợp lệ");
            int showId = Integer.parseInt(Protocol.decode(row[1]));
            LocalDateTime start = LocalDateTime.parse(Protocol.decode(row[5]));
            if (!start.isAfter(LocalDateTime.now().plusMinutes(2))) continue;
            List<String> seats = client.request("GET_SEAT_MAP;" + showId);
            expectOk(seats, "SEATS");
            for (int j = 1; j < seats.size(); j++) {
                String[] seat = seats.get(j).split(";", -1);
                check(seat.length == 3 && "SEAT".equals(seat[0]), "Dữ liệu ghế không hợp lệ");
                if ("free".equals(Protocol.decode(seat[2]))) {
                    return new SeatTarget(showId, Protocol.decode(seat[1]));
                }
            }
        }
        throw new IllegalStateException("Không có suất chiếu tương lai còn ghế. "
                + "Hãy tạo một suất chiếu mới bằng tab Quản trị rồi chạy lại.");
    }

    private static String seatState(MovieClient client, SeatTarget target) throws Exception {
        List<String> lines = client.request("GET_SEAT_MAP;" + target.showId);
        expectOk(lines, "SEATS");
        for (int i = 1; i < lines.size(); i++) {
            String[] row = lines.get(i).split(";", -1);
            if (row.length == 3 && target.seatCode.equals(Protocol.decode(row[1]))) {
                return Protocol.decode(row[2]);
            }
        }
        throw new IllegalStateException("Không tìm thấy ghế " + target.seatCode);
    }

    private static int login(MovieClient client, String user, String password) throws Exception {
        List<String> lines = client.request("LOGIN;" + encode(user) + ";" + encode(password));
        expectOk(lines, "LOGIN");
        String[] row = lines.get(0).split(";", -1);
        check(row.length == 6 && "user".equals(Protocol.decode(row[5])),
                "Tài khoản kiểm thử phải có vai trò user");
        return Integer.parseInt(row[2]);
    }

    private static MovieClient connect() throws Exception {
        String host = System.getenv("MOVIE_SERVER_HOST");
        String port = System.getenv("MOVIE_SERVER_PORT");
        if (host == null || host.trim().isEmpty()) host = MovieClient.DEFAULT_HOST;
        int number = port == null || port.trim().isEmpty()
                ? MovieClient.DEFAULT_PORT : Integer.parseInt(port.trim());
        return new MovieClient(host, number, MovieClient.DEFAULT_TIMEOUT_MS);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Thiếu biến môi trường " + name);
        }
        return value;
    }

    private static String encode(String value) {
        return Protocol.encode(value);
    }

    private static boolean isOk(List<String> lines, String kind) {
        return !lines.isEmpty() && lines.get(0).startsWith("OK;" + kind);
    }

    private static void expectOk(List<String> lines, String kind) {
        check(isOk(lines, kind), "Chờ OK;" + kind + " nhưng nhận " + lines);
    }

    private static void expectError(List<String> lines, String message) {
        check(!lines.isEmpty() && lines.get(0).startsWith("ERR;"),
                message + ". Phản hồi: " + lines);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static final class SeatTarget {
        private final int showId;
        private final String seatCode;

        private SeatTarget(int showId, String seatCode) {
            this.showId = showId;
            this.seatCode = seatCode;
        }
    }
}
