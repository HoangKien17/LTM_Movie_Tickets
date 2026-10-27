package repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import mysql.CSDL;

/** Lịch sử chat tồn tại qua lần khởi động server; mọi thời điểm do MySQL quyết định. */
public class ChatRepository {
    public static final int PAGE_SIZE = 50;

    public void verifyReady() throws SQLException {
        try (Connection c = CSDL.getConnection(); Statement s = c.createStatement()) {
            s.executeQuery("SELECT id,xuat_chieu_id,noi_dung FROM tin_nhan_chat LIMIT 0").close();
        }
    }

    /** showId, phim, phòng, bắt đầu, kết thúc, open(1/0), thời gian server. */
    public String[] room(int showId) throws SQLException {
        try (Connection c = CSDL.getConnection()) { return room(c, showId); }
    }

    private String[] room(Connection c, int showId) throws SQLException {
        String sql = "SELECT x.id,p.ten_phim,r.ten_phong,x.bat_dau,"
                + "TIMESTAMPADD(MINUTE,p.thoi_luong,x.bat_dau),NOW(6) "
                + "FROM xuat_chieu x JOIN phim p ON p.id=x.phim_id "
                + "JOIN phong_chieu r ON r.id=x.phong_chieu_id WHERE x.id=?";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, showId);
            try (ResultSet rs = p.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Suất chiếu không tồn tại");
                LocalDateTime end = rs.getTimestamp(5).toLocalDateTime();
                LocalDateTime now = rs.getTimestamp(6).toLocalDateTime();
                return new String[]{rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getTimestamp(4).toLocalDateTime().toString(), end.toString(),
                    now.isBefore(end) ? "1" : "0", now.toString()};
            }
        }
    }

    /** Tối đa PAGE_SIZE+1 dòng, từ cũ đến mới; dòng dư để xác định còn trang trước. */
    public List<String[]> history(int showId, long beforeId) throws SQLException {
        String sql = "SELECT m.id,m.xuat_chieu_id,m.nguoi_dung_id,u.ho_ten,m.ngay_gui,m.noi_dung "
                + "FROM tin_nhan_chat m JOIN nguoi_dung u ON u.id=m.nguoi_dung_id "
                + "WHERE m.xuat_chieu_id=? AND (?=0 OR m.id<?) ORDER BY m.id DESC LIMIT 51";
        List<String[]> result = new ArrayList<>();
        try (Connection c = CSDL.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, showId); p.setLong(2, beforeId); p.setLong(3, beforeId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) result.add(new String[]{rs.getString(1),rs.getString(2),rs.getString(3),
                    rs.getString(4),rs.getTimestamp(5).toLocalDateTime().toString(),rs.getString(6)});
            }
        }
        Collections.reverse(result);
        return result;
    }

    /** Khóa suất trước khi kiểm tra thời hạn và ghi, không tin đồng hồ/client userId. */
    public String[] send(int showId, int userId, String text) throws SQLException {
        try (Connection c = CSDL.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement("SELECT id FROM xuat_chieu WHERE id=? FOR UPDATE")) {
                    p.setInt(1, showId);
                    try (ResultSet rs = p.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Suất chiếu không tồn tại");
                    }
                }
                if (!"1".equals(room(c, showId)[5]))
                    throw new IllegalArgumentException("Suất chiếu đã kết thúc. Phòng chat đã đóng.");
                long id;
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO tin_nhan_chat(xuat_chieu_id,nguoi_dung_id,noi_dung,ngay_gui) VALUES(?,?,?,NOW(6))",
                        Statement.RETURN_GENERATED_KEYS)) {
                    p.setInt(1,showId); p.setInt(2,userId); p.setString(3,text); p.executeUpdate();
                    try (ResultSet rs = p.getGeneratedKeys()) {
                        if (!rs.next()) throw new SQLException("Không tạo được tin nhắn");
                        id = rs.getLong(1);
                    }
                }
                String[] result;
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT u.ho_ten,m.ngay_gui FROM tin_nhan_chat m JOIN nguoi_dung u ON u.id=m.nguoi_dung_id WHERE m.id=?")) {
                    p.setLong(1,id);
                    try (ResultSet rs = p.executeQuery()) {
                        rs.next();
                        result = new String[]{Long.toString(id),Integer.toString(showId),Integer.toString(userId),
                            rs.getString(1),rs.getTimestamp(2).toLocalDateTime().toString(),text};
                    }
                }
                c.commit(); return result;
            } catch (SQLException | RuntimeException ex) { c.rollback(); throw ex; }
        }
    }
}
