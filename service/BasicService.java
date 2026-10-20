package service;

import java.sql.SQLException;
import java.util.List;
import model.NguoiDung;
import model.Phim;
import repository.NguoiDungRepository;
import repository.PhimRepository;
import utils.PasswordUtil;

/**
 * Quy tắc nghiệp vụ của luồng cơ bản: tài khoản và danh sách phim.
 */
public final class BasicService {

    private final NguoiDungRepository nguoiDungRepository = new NguoiDungRepository();
    private final PhimRepository phimRepository = new PhimRepository();

    public NguoiDung register(String hoTen, String gmail, String soDienThoai,
            String matKhau) throws SQLException {
        hoTen = hoTen.trim();
        gmail = gmail.trim();
        soDienThoai = soDienThoai.trim();
        if (hoTen.isEmpty() || hoTen.length() > 100) {
            throw new IllegalArgumentException("Họ tên phải có từ 1 đến 100 ký tự");
        }
        if (!PasswordUtil.isValidEmail(gmail) || gmail.length() > 100) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }
        if (!PasswordUtil.isValidPhone(soDienThoai)) {
            throw new IllegalArgumentException("Số điện thoại phải có 9–11 chữ số");
        }
        if (matKhau.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
        }
        String hash = PasswordUtil.hashPassword(matKhau);
        if (hash == null) {
            throw new IllegalStateException("Không thể xử lý mật khẩu");
        }
        try {
            return nguoiDungRepository.insert(hoTen, gmail, soDienThoai, hash);
        } catch (SQLException ex) {
            if (ex.getSQLState() != null && ex.getSQLState().startsWith("23")) {
                throw new IllegalArgumentException("Email hoặc số điện thoại đã tồn tại");
            }
            throw ex;
        }
    }

    public NguoiDung login(String login, String matKhau) throws SQLException {
        if (login.trim().isEmpty() || matKhau.isEmpty()) {
            throw new IllegalArgumentException("Hãy nhập tài khoản và mật khẩu");
        }
        NguoiDung user = nguoiDungRepository.findByLogin(login.trim());
        if (user == null || !PasswordUtil.verifyPassword(matKhau, user.getMatKhauHash())) {
            return null;
        }
        return user;
    }

    public List<Phim> getMovies() throws SQLException {
        return phimRepository.findAll();
    }

    public NguoiDung profile(int userId) throws SQLException {
        NguoiDung user = nguoiDungRepository.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Tài khoản không tồn tại");
        }
        return user;
    }

    public NguoiDung updateProfile(int userId, String name, String email, String phone,
            String currentPassword, String newPassword) throws SQLException {
        name = name.trim();
        email = email.trim();
        phone = phone.trim();
        if (name.isEmpty() || name.length() > 100) {
            throw new IllegalArgumentException("Họ tên phải có từ 1 đến 100 ký tự");
        }
        if (!PasswordUtil.isValidEmail(email) || email.length() > 100) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }
        if (!PasswordUtil.isValidPhone(phone)) {
            throw new IllegalArgumentException("Số điện thoại phải có 9–11 chữ số");
        }
        NguoiDung current = profile(userId);
        if (!PasswordUtil.verifyPassword(currentPassword, current.getMatKhauHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }
        String replacement = null;
        if (!newPassword.isEmpty()) {
            if (newPassword.length() < 6) {
                throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự");
            }
            replacement = PasswordUtil.hashPassword(newPassword);
            if (replacement == null) {
                throw new IllegalStateException("Không thể xử lý mật khẩu");
            }
        }
        try {
            return nguoiDungRepository.updateProfile(userId, name, email, phone, replacement);
        } catch (SQLException ex) {
            if (ex.getSQLState() != null && ex.getSQLState().startsWith("23")) {
                throw new IllegalArgumentException("Email hoặc số điện thoại đã tồn tại");
            }
            throw ex;
        }
    }

    public void verifyReady() throws SQLException {
        nguoiDungRepository.findByLogin("__health_check__");
        phimRepository.findAll();
    }
}
