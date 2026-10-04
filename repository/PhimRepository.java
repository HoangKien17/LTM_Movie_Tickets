package repository;

import model.Phim;
import mysql.CSDL;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Truy vấn phim; giao diện và Server không chứa câu SQL này. */
public final class PhimRepository {
    public List<Phim> findAll() throws SQLException {
        String sql = "SELECT id, ten_phim, the_loai, thoi_luong, "
                + "ngay_khoi_chieu, mo_ta, anh FROM phim ORDER BY id";
        List<Phim> phim = new ArrayList<>();
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Date ngay = rs.getDate("ngay_khoi_chieu");
                phim.add(new Phim(rs.getInt("id"), rs.getString("ten_phim"),
                        rs.getString("the_loai"), rs.getInt("thoi_luong"),
                        ngay == null ? null : ngay.toLocalDate(),
                        rs.getString("mo_ta"), rs.getString("anh")));
            }
        }
        return phim;
    }
}
