package service;

import repository.CinemaRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Kiểm tra quy tắc nghiệp vụ trước khi gọi repository. */
public final class CinemaService {
    private final CinemaRepository repository = new CinemaRepository();

    public List<String[]> shows(int movieId) throws SQLException {
        if (movieId < 0) throw new IllegalArgumentException("Mã phim không hợp lệ");
        return repository.shows(movieId);
    }

    public List<String[]> seats(int showId) throws SQLException {
        requirePositive(showId, "Mã suất chiếu");
        return repository.seats(showId);
    }

    public long book(int userId, int showId, String csv) throws SQLException {
        requirePositive(showId, "Mã suất chiếu");
        if (csv == null || csv.trim().isEmpty()) {
            throw new IllegalArgumentException("Hãy chọn ít nhất một ghế");
        }
        String[] parts = csv.split(",", -1);
        if (parts.length > 10) throw new IllegalArgumentException("Mỗi lần đặt tối đa 10 ghế");
        List<String> codes = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        for (String raw : parts) {
            String code = raw.trim().toUpperCase(Locale.ROOT);
            if (!code.matches("[A-Z][1-9][0-9]?") || !unique.add(code)) {
                throw new IllegalArgumentException("Danh sách ghế không hợp lệ hoặc bị lặp");
            }
            codes.add(code);
        }
        return repository.book(userId, showId, codes);
    }

    public List<String[]> bookings(int userId) throws SQLException {
        return repository.bookings(userId, false);
    }

    public List<String[]> allBookings() throws SQLException {
        return repository.bookings(0, true);
    }

    public void cancel(int userId, long bookingId) throws SQLException {
        if (bookingId < 1) throw new IllegalArgumentException("Mã đơn không hợp lệ");
        repository.cancel(userId, bookingId, false);
    }

    public void adminCancel(long bookingId) throws SQLException {
        if (bookingId < 1) throw new IllegalArgumentException("Mã đơn không hợp lệ");
        repository.cancel(0, bookingId, true);
    }

    public int addMovie(String title, String genre, int minutes, String description)
            throws SQLException {
        validateMovie(title, genre, minutes, description);
        return repository.addMovie(title.trim(), genre.trim(), minutes, description.trim());
    }

    public void updateMovie(int id, String title, String genre, int minutes, String description)
            throws SQLException {
        requirePositive(id, "Mã phim");
        validateMovie(title, genre, minutes, description);
        repository.updateMovie(id, title.trim(), genre.trim(), minutes, description.trim());
    }

    public void deleteMovie(int id) throws SQLException {
        requirePositive(id, "Mã phim");
        repository.deleteMovie(id);
    }

    public List<String[]> rooms() throws SQLException {
        return repository.rooms();
    }

    public int addRoom(String name, int rows, int columns) throws SQLException {
        if (name == null || name.trim().isEmpty() || name.length() > 100) {
            throw new IllegalArgumentException("Tên phòng phải có từ 1 đến 100 ký tự");
        }
        if (rows < 1 || rows > 26 || columns < 1 || columns > 99) {
            throw new IllegalArgumentException("Số hàng 1–26, số cột 1–99");
        }
        return repository.addRoom(name.trim(), rows, columns);
    }

    public void deleteRoom(int id) throws SQLException {
        requirePositive(id, "Mã phòng");
        repository.deleteRoom(id);
    }

    public void updateRoom(int id, String name) throws SQLException {
        requirePositive(id, "Mã phòng");
        if (name == null || name.trim().isEmpty() || name.length() > 100) {
            throw new IllegalArgumentException("Tên phòng phải có từ 1 đến 100 ký tự");
        }
        repository.updateRoom(id, name.trim());
    }

    public int addShow(int movieId, int roomId, LocalDateTime start, BigDecimal price)
            throws SQLException {
        requirePositive(movieId, "Mã phim");
        requirePositive(roomId, "Mã phòng");
        if (start == null || !start.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Giờ chiếu phải ở tương lai");
        }
        if (price == null || price.signum() <= 0 || price.scale() > 2
                || price.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException("Giá vé không hợp lệ");
        }
        return repository.addShow(movieId, roomId, start, price);
    }

    public void deleteShow(int id) throws SQLException {
        requirePositive(id, "Mã suất chiếu");
        repository.deleteShow(id);
    }

    public void updateShow(int id, int movieId, int roomId, LocalDateTime start,
                           BigDecimal price) throws SQLException {
        requirePositive(id, "Mã suất chiếu");
        requirePositive(movieId, "Mã phim");
        requirePositive(roomId, "Mã phòng");
        if (start == null || !start.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Giờ chiếu phải ở tương lai");
        }
        if (price == null || price.signum() <= 0 || price.scale() > 2
                || price.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException("Giá vé không hợp lệ");
        }
        repository.updateShow(id, movieId, roomId, start, price);
    }

    public List<String[]> users() throws SQLException {
        return repository.users();
    }

    public String[] stats() throws SQLException {
        return repository.stats();
    }

    private static void validateMovie(String title, String genre, int minutes, String description) {
        if (title == null || title.trim().isEmpty() || title.length() > 200
                || genre == null || genre.length() > 100
                || description == null || description.length() > 10000
                || minutes < 1 || minutes > 1000) {
            throw new IllegalArgumentException("Thông tin phim không hợp lệ");
        }
    }

    private static void requirePositive(int id, String label) {
        if (id < 1) throw new IllegalArgumentException(label + " không hợp lệ");
    }
}
