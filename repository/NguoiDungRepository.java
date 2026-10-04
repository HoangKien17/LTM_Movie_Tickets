package repository;

import model.NguoiDung;
import mysql.CSDL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Chỉ lớp này truy vấn bảng nguoi_dung cho luồng cơ bản. */
public final class NguoiDungRepository {
    public NguoiDung findByLogin(String login) throws SQLException {
        String sql = "SELECT id, ho_ten, gmail, so_dien_thoai, mat_khau, vai_tro "
                + "FROM nguoi_dung WHERE gmail = ? OR so_dien_thoai = ? LIMIT 1";
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, login);
            ps.setString(2, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new NguoiDung(rs.getInt("id"), rs.getString("ho_ten"),
                        rs.getString("gmail"), rs.getString("so_dien_thoai"),
                        rs.getString("mat_khau"), rs.getString("vai_tro"));
            }
        }
    }

    public NguoiDung insert(String hoTen, String gmail, String soDienThoai,
                            String matKhauHash) throws SQLException {
        String sql = "INSERT INTO nguoi_dung "
                + "(ho_ten, gmail, so_dien_thoai, mat_khau, vai_tro) "
                + "VALUES (?, ?, ?, ?, 'user')";
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, hoTen);
            ps.setString(2, gmail);
            ps.setString(3, soDienThoai);
            ps.setString(4, matKhauHash);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không nhận được mã tài khoản mới");
                }
                return new NguoiDung(keys.getInt(1), hoTen, gmail,
                        soDienThoai, matKhauHash, "user");
            }
        }
    }
}
