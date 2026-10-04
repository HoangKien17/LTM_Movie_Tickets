package repository;

import mysql.CSDL;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Mọi truy vấn suất chiếu, ghế, vé và quản trị đều ở phía Server. */
public final class CinemaRepository {
    public List<String[]> shows(int movieId) throws SQLException {
        String sql = "SELECT x.id, p.id, p.ten_phim, r.ten_phong, x.bat_dau, x.gia_ve "
                + "FROM xuat_chieu x JOIN phim p ON p.id=x.phim_id "
                + "JOIN phong_chieu r ON r.id=x.phong_chieu_id "
                + "WHERE (?=0 OR p.id=?) ORDER BY x.bat_dau, x.id";
        List<String[]> rows = new ArrayList<>();
        try (Connection c = CSDL.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            ps.setInt(2, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getTimestamp(5).toLocalDateTime().toString(),
                            rs.getBigDecimal(6).toPlainString()});
                }
            }
        }
        return rows;
    }

    public List<String[]> seats(int showId) throws SQLException {
        String sql = "SELECT g.ma_ghe, CASE WHEN v.id IS NULL THEN 'free' ELSE 'taken' END "
                + "FROM xuat_chieu x JOIN ghe g ON g.phong_chieu_id=x.phong_chieu_id "
                + "LEFT JOIN ve v ON v.xuat_chieu_id=x.id AND v.ghe_id=g.id "
                + "AND v.trang_thai='active' WHERE x.id=? ORDER BY g.ma_ghe";
        List<String[]> rows = new ArrayList<>();
        try (Connection c = CSDL.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, showId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(new String[]{rs.getString(1), rs.getString(2)});
            }
        }
        return rows;
    }

    public long book(int userId, int showId, List<String> seatCodes) throws SQLException {
        try (Connection c = CSDL.getConnection()) {
            c.setAutoCommit(false);
            try {
                int roomId;
                BigDecimal price;
                LocalDateTime start;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT phong_chieu_id, gia_ve, bat_dau FROM xuat_chieu WHERE id=? FOR UPDATE")) {
                    ps.setInt(1, showId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Suất chiếu không tồn tại");
                        roomId = rs.getInt(1);
                        price = rs.getBigDecimal(2);
                        start = rs.getTimestamp(3).toLocalDateTime();
                    }
                }
                if (!start.isAfter(LocalDateTime.now())) {
                    throw new IllegalArgumentException("Suất chiếu đã bắt đầu");
                }
                List<Integer> seatIds = new ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT id FROM ghe WHERE phong_chieu_id=? AND ma_ghe=?")) {
                    for (String code : seatCodes) {
                        ps.setInt(1, roomId);
                        ps.setString(2, code);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) throw new IllegalArgumentException("Ghế " + code + " không thuộc phòng chiếu");
                            seatIds.add(rs.getInt(1));
                        }
                    }
                }
                long bookingId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO don_dat_ve (nguoi_dung_id,xuat_chieu_id,tong_tien) VALUES (?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, showId);
                    ps.setBigDecimal(3, price.multiply(BigDecimal.valueOf(seatCodes.size())));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Không nhận được mã đặt vé");
                        bookingId = keys.getLong(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO ve (don_dat_ve_id,xuat_chieu_id,ghe_id) VALUES (?,?,?)")) {
                    for (int seatId : seatIds) {
                        ps.setLong(1, bookingId);
                        ps.setInt(2, showId);
                        ps.setInt(3, seatId);
                        ps.executeUpdate();
                    }
                }
                c.commit();
                return bookingId;
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                if (ex instanceof SQLException && ((SQLException) ex).getErrorCode() == 1062) {
                    throw new IllegalArgumentException("Có ghế vừa được người khác đặt. Hãy tải lại sơ đồ ghế.");
                }
                throw ex;
            }
        }
    }

    public List<String[]> bookings(int userId, boolean all) throws SQLException {
        String sql = "SELECT d.id, u.ho_ten, p.ten_phim, r.ten_phong, x.bat_dau, "
                + "d.tong_tien, d.trang_thai, "
                + "(SELECT GROUP_CONCAT(g.ma_ghe ORDER BY g.ma_ghe SEPARATOR ',') "
                + "FROM ve v JOIN ghe g ON g.id=v.ghe_id WHERE v.don_dat_ve_id=d.id) "
                + "FROM don_dat_ve d JOIN nguoi_dung u ON u.id=d.nguoi_dung_id "
                + "JOIN xuat_chieu x ON x.id=d.xuat_chieu_id "
                + "JOIN phim p ON p.id=x.phim_id JOIN phong_chieu r ON r.id=x.phong_chieu_id "
                + "WHERE (?=1 OR d.nguoi_dung_id=?) ORDER BY d.id DESC";
        List<String[]> rows = new ArrayList<>();
        try (Connection c = CSDL.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, all ? 1 : 0);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getTimestamp(5).toLocalDateTime().toString(),
                            rs.getBigDecimal(6).toPlainString(), rs.getString(7), rs.getString(8)});
                }
            }
        }
        return rows;
    }

    public void cancel(int userId, long bookingId, boolean admin) throws SQLException {
        try (Connection c = CSDL.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT d.nguoi_dung_id,d.trang_thai,x.bat_dau FROM don_dat_ve d "
                        + "JOIN xuat_chieu x ON x.id=d.xuat_chieu_id WHERE d.id=? FOR UPDATE")) {
                    ps.setLong(1, bookingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || (!admin && rs.getInt(1) != userId)) {
                            throw new IllegalArgumentException("Không tìm thấy đơn đặt vé của bạn");
                        }
                        if (!"active".equals(rs.getString(2))) {
                            throw new IllegalArgumentException("Đơn đặt vé đã hủy");
                        }
                        if (!rs.getTimestamp(3).toLocalDateTime().isAfter(LocalDateTime.now())) {
                            throw new IllegalArgumentException("Suất chiếu đã bắt đầu");
                        }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE ve SET trang_thai='cancelled' WHERE don_dat_ve_id=?")) {
                    ps.setLong(1, bookingId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE don_dat_ve SET trang_thai='cancelled' WHERE id=?")) {
                    ps.setLong(1, bookingId);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    public int addMovie(String title, String genre, int minutes, String description) throws SQLException {
        try (Connection c = CSDL.getConnection(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO phim (ten_phim,the_loai,thoi_luong,ngay_khoi_chieu,mo_ta,anh) "
                + "VALUES (?,?,?,CURRENT_DATE,?,'')", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, genre);
            ps.setInt(3, minutes);
            ps.setString(4, description);
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    public void updateMovie(int id, String title, String genre, int minutes, String description)
            throws SQLException {
        try (Connection c = CSDL.getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE phim SET ten_phim=?,the_loai=?,thoi_luong=?,mo_ta=? WHERE id=?")) {
            ps.setString(1, title);
            ps.setString(2, genre);
            ps.setInt(3, minutes);
            ps.setString(4, description);
            ps.setInt(5, id);
            if (ps.executeUpdate() == 0) throw new IllegalArgumentException("Phim không tồn tại");
        }
    }

    public void deleteMovie(int id) throws SQLException {
        try (Connection c = CSDL.getConnection(); PreparedStatement ps = c.prepareStatement(
                "DELETE FROM phim WHERE id=?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) throw new IllegalArgumentException("Phim không tồn tại");
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1451) throw new IllegalArgumentException("Phim đang có suất chiếu");
            throw ex;
        }
    }

    public List<String[]> rooms() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        try (Connection c = CSDL.getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT r.id,r.ten_phong,COUNT(g.id) FROM phong_chieu r "
                + "LEFT JOIN ghe g ON g.phong_chieu_id=r.id GROUP BY r.id,r.ten_phong ORDER BY r.id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3)});
        }
        return rows;
    }

    public int addRoom(String name, int rows, int columns) throws SQLException {
        try (Connection c = CSDL.getConnection()) {
            c.setAutoCommit(false);
            try {
                int id;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO phong_chieu (ten_phong) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, name);
                    ps.executeUpdate();
                    id = generatedId(ps);
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO ghe (phong_chieu_id,ma_ghe) VALUES (?,?)")) {
                    for (int row = 0; row < rows; row++) {
                        for (int col = 1; col <= columns; col++) {
                            ps.setInt(1, id);
                            ps.setString(2, Character.toString((char) ('A' + row)) + col);
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }
                c.commit();
                return id;
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                if (ex instanceof SQLException && ((SQLException) ex).getErrorCode() == 1062) {
                    throw new IllegalArgumentException("Tên phòng đã tồn tại");
                }
                throw ex;
            }
        }
    }

    public void updateRoom(int id, String name) throws SQLException {
        try (Connection c = CSDL.getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE phong_chieu SET ten_phong=? WHERE id=?")) {
            ps.setString(1, name);
            ps.setInt(2, id);
            if (ps.executeUpdate() == 0) throw new IllegalArgumentException("Phòng không tồn tại");
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) throw new IllegalArgumentException("Tên phòng đã tồn tại");
            throw ex;
        }
    }

    public void deleteRoom(int id) throws SQLException {
        try (Connection c = CSDL.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM ghe WHERE phong_chieu_id=?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM phong_chieu WHERE id=?")) {
                    ps.setInt(1, id);
                    if (ps.executeUpdate() == 0) throw new IllegalArgumentException("Phòng không tồn tại");
                }
                c.commit();
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                if (ex instanceof SQLException && ((SQLException) ex).getErrorCode() == 1451) {
                    throw new IllegalArgumentException("Phòng đang có suất chiếu hoặc vé");
                }
                throw ex;
            }
        }
    }

    public int addShow(int movieId, int roomId, LocalDateTime start, BigDecimal price)
            throws SQLException {
        try (Connection c = CSDL.getConnection()) {
            c.setAutoCommit(false);
            try {
                int minutes;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT thoi_luong FROM phim WHERE id=?")) {
                    ps.setInt(1, movieId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Phim không tồn tại");
                        minutes = rs.getInt(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT id FROM phong_chieu WHERE id=? FOR UPDATE")) {
                    ps.setInt(1, roomId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Phòng không tồn tại");
                    }
                }
                checkOverlap(c, roomId, start, minutes, 0);
                int id;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO xuat_chieu (phim_id,phong_chieu_id,bat_dau,gia_ve) "
                        + "VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, movieId);
                    ps.setInt(2, roomId);
                    ps.setTimestamp(3, Timestamp.valueOf(start));
                    ps.setBigDecimal(4, price);
                    ps.executeUpdate();
                    id = generatedId(ps);
                }
                c.commit();
                return id;
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    public void updateShow(int id, int movieId, int roomId, LocalDateTime start,
                           BigDecimal price) throws SQLException {
        try (Connection c = CSDL.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT id FROM xuat_chieu WHERE id=? FOR UPDATE")) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Suất chiếu không tồn tại");
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT id FROM don_dat_ve WHERE xuat_chieu_id=? LIMIT 1")) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) throw new IllegalArgumentException("Suất chiếu đã có vé");
                    }
                }
                int minutes;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT thoi_luong FROM phim WHERE id=?")) {
                    ps.setInt(1, movieId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Phim không tồn tại");
                        minutes = rs.getInt(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT id FROM phong_chieu WHERE id=? FOR UPDATE")) {
                    ps.setInt(1, roomId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Phòng không tồn tại");
                    }
                }
                checkOverlap(c, roomId, start, minutes, id);
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE xuat_chieu SET phim_id=?,phong_chieu_id=?,bat_dau=?,gia_ve=? WHERE id=?")) {
                    ps.setInt(1, movieId);
                    ps.setInt(2, roomId);
                    ps.setTimestamp(3, Timestamp.valueOf(start));
                    ps.setBigDecimal(4, price);
                    ps.setInt(5, id);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    private static void checkOverlap(Connection c, int roomId, LocalDateTime start,
                                     int minutes, int excludeId) throws SQLException {
        String sql = "SELECT x.bat_dau,p.thoi_luong FROM xuat_chieu x "
                + "JOIN phim p ON p.id=x.phim_id WHERE x.phong_chieu_id=? AND x.id<>? FOR UPDATE";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                LocalDateTime end = start.plusMinutes(minutes + 15);
                while (rs.next()) {
                    LocalDateTime otherStart = rs.getTimestamp(1).toLocalDateTime();
                    LocalDateTime otherEnd = otherStart.plusMinutes(rs.getInt(2) + 15);
                    if (start.isBefore(otherEnd) && end.isAfter(otherStart)) {
                        throw new IllegalArgumentException("Phòng bị trùng giờ chiếu");
                    }
                }
            }
        }
    }

    public void deleteShow(int id) throws SQLException {
        try (Connection c = CSDL.getConnection(); PreparedStatement ps = c.prepareStatement(
                "DELETE FROM xuat_chieu WHERE id=?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) throw new IllegalArgumentException("Suất chiếu không tồn tại");
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1451) throw new IllegalArgumentException("Suất chiếu đã có vé");
            throw ex;
        }
    }

    public List<String[]> users() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        try (Connection c = CSDL.getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT id,ho_ten,gmail,so_dien_thoai,vai_tro FROM nguoi_dung ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5)});
            }
        }
        return rows;
    }

    public String[] stats() throws SQLException {
        String sql = "SELECT COUNT(*), COALESCE(SUM(CASE WHEN trang_thai='active' "
                + "THEN tong_tien ELSE 0 END),0) FROM don_dat_ve";
        try (Connection c = CSDL.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new String[]{rs.getString(1), rs.getBigDecimal(2).toPlainString()};
        }
    }

    private static int generatedId(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (!keys.next()) throw new SQLException("Không nhận được mã mới");
            return keys.getInt(1);
        }
    }
}
