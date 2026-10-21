package repository;

import java.sql.*;
import java.util.*;
import mysql.CSDL;

/** Persistent feed, always scoped to the authenticated user supplied by the server. */
public final class NotificationRepository {
    public void verifyReady() throws SQLException {
        try (Connection c = CSDL.getConnection(); Statement s = c.createStatement()) {
            s.executeQuery("SELECT id,da_doc FROM thong_bao LIMIT 0").close();
        }
    }

    public List<String[]> list(int userId) throws SQLException {
        try (Connection c = CSDL.getConnection()) {
            c.setAutoCommit(false);
            try {
                // Unique key makes concurrent polling from several clients idempotent.
                String success = "INSERT INTO thong_bao(nguoi_dung_id,don_dat_ve_id,loai,ngay_tao) "
                        + "SELECT d.nguoi_dung_id,d.id,'booking_success',COALESCE(d.paid_at,d.ngay_dat) "
                        + "FROM don_dat_ve d WHERE d.nguoi_dung_id=? AND d.trang_thai='active' "
                        + "ON DUPLICATE KEY UPDATE id=thong_bao.id";
                try (PreparedStatement p = c.prepareStatement(success)) { p.setInt(1, userId); p.executeUpdate(); }
                String reminder = "INSERT INTO thong_bao(nguoi_dung_id,don_dat_ve_id,loai,ngay_tao) "
                        + "SELECT d.nguoi_dung_id,d.id,'show_reminder',GREATEST(DATE_SUB(x.bat_dau,INTERVAL 30 MINUTE),COALESCE(d.paid_at,d.ngay_dat)) "
                        + "FROM don_dat_ve d JOIN xuat_chieu x ON x.id=d.xuat_chieu_id "
                        + "WHERE d.nguoi_dung_id=? AND d.trang_thai='active' AND x.bat_dau>NOW() "
                        + "AND x.bat_dau<=DATE_ADD(NOW(),INTERVAL 30 MINUTE) AND d.checked_in_at IS NULL "
                        + "ON DUPLICATE KEY UPDATE id=thong_bao.id";
                try (PreparedStatement p = c.prepareStatement(reminder)) { p.setInt(1, userId); p.executeUpdate(); }
                String sql = "SELECT n.id,n.loai,d.id,p.ten_phim,r.ten_phong,x.bat_dau,"
                        + "COALESCE((SELECT GROUP_CONCAT(g.ma_ghe ORDER BY g.ma_ghe SEPARATOR ', ') FROM ve v JOIN ghe g ON g.id=v.ghe_id WHERE v.don_dat_ve_id=d.id),''),"
                        + "n.ngay_tao,n.da_doc FROM thong_bao n JOIN don_dat_ve d ON d.id=n.don_dat_ve_id "
                        + "JOIN xuat_chieu x ON x.id=d.xuat_chieu_id JOIN phim p ON p.id=x.phim_id "
                        + "JOIN phong_chieu r ON r.id=x.phong_chieu_id WHERE n.nguoi_dung_id=? "
                        + "AND (n.loai<>'show_reminder' OR (d.trang_thai='active' AND x.bat_dau>NOW() AND d.checked_in_at IS NULL)) "
                        + "ORDER BY n.ngay_tao DESC,n.id DESC LIMIT 50";
                List<String[]> result = new ArrayList<>();
                try (PreparedStatement p = c.prepareStatement(sql)) {
                    p.setInt(1, userId);
                    try (ResultSet rs = p.executeQuery()) {
                        while (rs.next()) result.add(new String[]{rs.getString(1),rs.getString(2),rs.getString(3),
                                rs.getString(4),rs.getString(5),rs.getTimestamp(6).toLocalDateTime().toString(),
                                rs.getString(7),rs.getTimestamp(8).toLocalDateTime().toString(),rs.getBoolean(9)?"1":"0"});
                    }
                }
                c.commit(); return result;
            } catch (SQLException | RuntimeException ex) { c.rollback(); throw ex; }
        }
    }

    public void markRead(int userId, List<Long> ids) throws SQLException {
        if (ids.isEmpty()) return;
        String slots = String.join(",", Collections.nCopies(ids.size(), "?"));
        try (Connection c = CSDL.getConnection(); PreparedStatement p = c.prepareStatement(
                "UPDATE thong_bao SET da_doc=TRUE WHERE nguoi_dung_id=? AND id IN (" + slots + ")")) {
            p.setInt(1, userId);
            for (int i=0;i<ids.size();i++) p.setLong(i+2,ids.get(i));
            p.executeUpdate();
        }
    }
}
