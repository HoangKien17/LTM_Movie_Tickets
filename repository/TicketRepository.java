package repository;

import mysql.CSDL;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Locale;

/** QR là một bearer token ngẫu nhiên cho toàn bộ đơn, không chứa mật khẩu/PII. */
public final class TicketRepository {
    private static final SecureRandom RANDOM = new SecureRandom();

    public void verifyReady() throws SQLException {
        try (Connection c = CSDL.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT ticket_token,checked_in_at,checked_in_by FROM don_dat_ve LIMIT 0")) {
            ps.executeQuery().close();
        }
    }

    public String[] ownTicket(int userId, long bookingId) throws SQLException {
        try (Connection c = CSDL.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT nguoi_dung_id,ticket_token FROM don_dat_ve WHERE id=? FOR UPDATE")) {
                    ps.setLong(1, bookingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || rs.getInt(1) != userId) {
                            throw new IllegalArgumentException("Không tìm thấy đơn đặt vé của bạn");
                        }
                        if (rs.getString(2) == null) {
                            try (PreparedStatement update = c.prepareStatement(
                                    "UPDATE don_dat_ve SET ticket_token=? WHERE id=?")) {
                                update.setString(1, newToken());
                                update.setLong(2, bookingId);
                                update.executeUpdate();
                            }
                        }
                    }
                }
                String[] ticket = detail(c, bookingId);
                c.commit();
                return ticket;
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    public String[] inspect(String token) throws SQLException {
        try (Connection c = CSDL.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id FROM don_dat_ve WHERE ticket_token=?")) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Mã vé không tồn tại");
                return detail(c, rs.getLong(1));
            }
        }
    }

    public String[] checkIn(int staffId, String token) throws SQLException {
        try (Connection c = CSDL.getConnection()) {
            c.setAutoCommit(false);
            try {
                long id;
                // Cùng khóa bản ghi đơn với thao tác hủy: chỉ một kết quả được commit.
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT id,trang_thai,checked_in_at FROM don_dat_ve WHERE ticket_token=? FOR UPDATE")) {
                    ps.setString(1, token);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Mã vé không tồn tại");
                        id = rs.getLong(1);
                        if (!"active".equals(rs.getString(2))) {
                            throw new IllegalArgumentException("Vé đã hủy, không được vào rạp");
                        }
                        if (rs.getTimestamp(3) != null) {
                            throw new IllegalArgumentException("Vé đã được sử dụng");
                        }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE don_dat_ve SET checked_in_at=CURRENT_TIMESTAMP,checked_in_by=? WHERE id=?")) {
                    ps.setInt(1, staffId);
                    ps.setLong(2, id);
                    ps.executeUpdate();
                }
                String[] ticket = detail(c, id);
                c.commit();
                return ticket;
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    // id, phim, phòng, giờ, ghế, trạng thái đơn, lúc soát, token.
    private static String[] detail(Connection c, long id) throws SQLException {
        String sql = "SELECT d.id,p.ten_phim,r.ten_phong,x.bat_dau,d.trang_thai,"
                + "d.checked_in_at,d.ticket_token,"
                + "(SELECT GROUP_CONCAT(g.ma_ghe ORDER BY LEFT(g.ma_ghe,1),"
                + "CAST(SUBSTRING(g.ma_ghe,2) AS UNSIGNED) SEPARATOR ',') "
                + "FROM ve v JOIN ghe g ON g.id=v.ghe_id WHERE v.don_dat_ve_id=d.id) "
                + "FROM don_dat_ve d JOIN xuat_chieu x ON x.id=d.xuat_chieu_id "
                + "JOIN phim p ON p.id=x.phim_id JOIN phong_chieu r ON r.id=x.phong_chieu_id WHERE d.id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Đơn không tồn tại");
                Timestamp checked = rs.getTimestamp(6);
                return new String[]{rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getTimestamp(4).toLocalDateTime().toString(), rs.getString(8),
                        rs.getString(5), checked == null ? "" : checked.toLocalDateTime().toString(),
                        rs.getString(7)};
            }
        }
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        StringBuilder text = new StringBuilder(64);
        for (byte value : bytes) text.append(String.format(Locale.ROOT, "%02x", value & 255));
        return text.toString();
    }
}
