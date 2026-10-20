package repository;

import mysql.CSDL;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Giao dịch giữ ghế, đồ ăn và thanh toán; mọi giá tiền đều được tính lại ở
 * server.
 */
public final class OrderRepository {

    public void verifyReady() throws SQLException {
        try (Connection c = CSDL.getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT d.expires_at,d.paid_at,t.order_code FROM don_dat_ve d LEFT JOIN thanh_toan t ON t.don_dat_ve_id=d.id LIMIT 0")) {
            ps.executeQuery().close();
        }
    }

    public List<String[]> products(boolean all) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT id,ten_san_pham,loai,gia,dang_ban FROM san_pham " + (all ? "" : "WHERE dang_ban=1 ") + "ORDER BY loai,id";
        try (Connection c = CSDL.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getBigDecimal(4).toPlainString(), rs.getBoolean(5) ? "1" : "0"});
            }
        }
        return rows;
    }

    public int addProduct(String name, String type, BigDecimal price) throws SQLException {
        try (Connection c = CSDL.getConnection(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO san_pham(ten_san_pham,loai,gia,dang_ban) VALUES(?,?,?,1)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, type);
            ps.setBigDecimal(3, price);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không nhận được mã sản phẩm mới");
                }
                return keys.getInt(1);
            }
        }
    }

    public void updateProduct(int id, String name, String type, BigDecimal price, boolean active)
            throws SQLException {
        try (Connection c = CSDL.getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE san_pham SET ten_san_pham=?,loai=?,gia=?,dang_ban=? WHERE id=?")) {
            ps.setString(1, name);
            ps.setString(2, type);
            ps.setBigDecimal(3, price);
            ps.setBoolean(4, active);
            ps.setInt(5, id);
            if (ps.executeUpdate() == 0) {
                ensureProduct(c, id);
            }
        }
    }

    /**
     * Xóa mềm để đơn cũ vẫn giữ được tên và lịch sử sản phẩm.
     */
    public void deleteProduct(int id) throws SQLException {
        try (Connection c = CSDL.getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE san_pham SET dang_ban=0 WHERE id=?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) {
                ensureProduct(c, id);
            }
        }
    }

    private static void ensureProduct(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM san_pham WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Sản phẩm không tồn tại");
                }
            }
        }
    }

    public Reservation reserve(int userId, int showId, List<String> seats, Map<Integer, Integer> items,
            long orderCode, String provider, int holdMinutes) throws SQLException {
        try (Connection c = CSDL.getConnection()) {
            c.setAutoCommit(false);
            try {
                int roomId;
                BigDecimal ticketPrice;
                LocalDateTime start;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT phong_chieu_id,gia_ve,bat_dau FROM xuat_chieu WHERE id=? FOR UPDATE")) {
                    ps.setInt(1, showId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Suất chiếu không tồn tại");
                        }
                        roomId = rs.getInt(1);
                        ticketPrice = rs.getBigDecimal(2);
                        start = rs.getTimestamp(3).toLocalDateTime();
                    }
                }
                if (!start.isAfter(LocalDateTime.now().plusMinutes(holdMinutes))) {
                    throw new IllegalArgumentException("Suất chiếu quá gần hoặc đã bắt đầu");
                }
                List<Integer> seatIds = new ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement("SELECT id FROM ghe WHERE phong_chieu_id=? AND ma_ghe=?")) {
                    for (String code : seats) {
                        ps.setInt(1, roomId);
                        ps.setString(2, code);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                throw new IllegalArgumentException("Ghế " + code + " không thuộc phòng chiếu");
                            }
                            seatIds.add(rs.getInt(1));
                        }
                    }
                }
                Map<Integer, Product> products = new LinkedHashMap<>();
                BigDecimal foodTotal = BigDecimal.ZERO;
                if (!items.isEmpty()) try (PreparedStatement ps = c.prepareStatement(
                        "SELECT ten_san_pham,gia,dang_ban FROM san_pham WHERE id=?")) {
                    for (Map.Entry<Integer, Integer> item : items.entrySet()) {
                        ps.setInt(1, item.getKey());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next() || !rs.getBoolean(3)) {
                                throw new IllegalArgumentException("Sản phẩm không còn được bán");
                            }
                            Product p = new Product(rs.getString(1), rs.getBigDecimal(2), item.getValue());
                            products.put(item.getKey(), p);
                            foodTotal = foodTotal.add(p.price.multiply(BigDecimal.valueOf(p.quantity)));
                        }
                    }
                }
                BigDecimal total = ticketPrice.multiply(BigDecimal.valueOf(seats.size())).add(foodTotal);
                LocalDateTime expires = LocalDateTime.now().plusMinutes(holdMinutes);
                long bookingId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO don_dat_ve(nguoi_dung_id,xuat_chieu_id,tong_tien,trang_thai,expires_at) "
                        + "VALUES(?,?,?,'pending_payment',?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, showId);
                    ps.setBigDecimal(3, total);
                    ps.setTimestamp(4, Timestamp.valueOf(expires));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Không tạo được đơn");
                        
                        }bookingId = keys.getLong(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO ve(don_dat_ve_id,xuat_chieu_id,ghe_id,trang_thai) VALUES(?,?,?,'held')")) {
                    for (int seatId : seatIds) {
                        ps.setLong(1, bookingId);
                        ps.setInt(2, showId);
                        ps.setInt(3, seatId);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO chi_tiet_do_an(don_dat_ve_id,san_pham_id,so_luong,don_gia) VALUES(?,?,?,?)")) {
                    for (Map.Entry<Integer, Product> item : products.entrySet()) {
                        ps.setLong(1, bookingId);
                        ps.setInt(2, item.getKey());
                        ps.setInt(3, item.getValue().quantity);
                        ps.setBigDecimal(4, item.getValue().price);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO thanh_toan(don_dat_ve_id,nha_cung_cap,order_code,so_tien) VALUES(?,?,?,?)")) {
                    ps.setLong(1, bookingId);
                    ps.setString(2, provider);
                    ps.setLong(3, orderCode);
                    ps.setBigDecimal(4, total);
                    ps.executeUpdate();
                }
                c.commit();
                return new Reservation(bookingId, showId, orderCode, total.longValueExact(), expires);
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                if (ex instanceof SQLException && ((SQLException) ex).getErrorCode() == 1062) {
                    throw new IllegalArgumentException("Có ghế đang được người khác giữ hoặc đã đặt. Hãy chọn lại.");
                }
                throw ex;
            }
        }
    }

    public void attach(long bookingId, String providerId, String qr, String checkout) throws SQLException {
        try (Connection c = CSDL.getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE thanh_toan SET provider_payment_id=?,qr_payload=?,checkout_url=? WHERE don_dat_ve_id=? AND trang_thai='pending'")) {
            ps.setString(1, providerId);
            ps.setString(2, qr);
            ps.setString(3, checkout);
            ps.setLong(4, bookingId);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Không cập nhật được mã thanh toán");
            }
        }
    }

    public int failReservation(long bookingId) throws SQLException {
        return finishWithoutPayment(bookingId, "failed", "expired");
    }

    public List<Pending> pending() throws SQLException {
        List<Pending> rows = new ArrayList<>();
        try (Connection c = CSDL.getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT t.order_code,d.id,d.xuat_chieu_id,d.expires_at FROM thanh_toan t JOIN don_dat_ve d ON d.id=t.don_dat_ve_id "
                + "WHERE t.trang_thai='pending' AND d.trang_thai='pending_payment' ORDER BY d.id"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Pending(rs.getLong(1), rs.getLong(2), rs.getInt(3), rs.getTimestamp(4).toLocalDateTime()));
            }
        }
        return rows;
    }

    public int confirm(long orderCode, String reference) throws SQLException {
        try (Connection c = CSDL.getConnection()) {
            c.setAutoCommit(false);
            try {
                long bookingId;
                int showId;
                String paymentStatus, orderStatus;
                LocalDateTime expires;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT d.id,d.xuat_chieu_id,d.trang_thai,d.expires_at,t.trang_thai FROM thanh_toan t "
                        + "JOIN don_dat_ve d ON d.id=t.don_dat_ve_id WHERE t.order_code=? FOR UPDATE")) {
                    ps.setLong(1, orderCode);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Giao dịch không tồn tại");
                        }
                        bookingId = rs.getLong(1);
                        showId = rs.getInt(2);
                        orderStatus = rs.getString(3);
                        expires = rs.getTimestamp(4).toLocalDateTime();
                        paymentStatus = rs.getString(5);
                    }
                }
                if ("paid".equals(paymentStatus) && "active".equals(orderStatus)) {
                    c.commit();
                    return 0;
                }
                if (!"pending".equals(paymentStatus) || !"pending_payment".equals(orderStatus)) {
                    throw new IllegalArgumentException("Đơn không còn chờ thanh toán");
                }
                if (expires.isBefore(LocalDateTime.now())) {
                    throw new IllegalArgumentException("Thanh toán đến sau khi đơn hết hạn; cần đối soát thủ công");
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE don_dat_ve SET trang_thai='active',paid_at=NOW() WHERE id=?")) {
                    ps.setLong(1, bookingId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE ve SET trang_thai='active' WHERE don_dat_ve_id=? AND trang_thai='held'")) {
                    ps.setLong(1, bookingId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE thanh_toan SET trang_thai='paid',paid_at=NOW(),transaction_ref=? WHERE order_code=?")) {
                    ps.setString(1, reference);
                    ps.setLong(2, orderCode);
                    ps.executeUpdate();
                }
                c.commit();
                return showId;
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    public int demoConfirm(int userId, long bookingId) throws SQLException {
        long orderCode;
        try (Connection c = CSDL.getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT t.order_code FROM thanh_toan t JOIN don_dat_ve d ON d.id=t.don_dat_ve_id "
                + "WHERE d.id=? AND d.nguoi_dung_id=? AND t.nha_cung_cap='demo'")) {
            ps.setLong(1, bookingId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Đơn demo không tồn tại");
                
                }orderCode = rs.getLong(1);
            }
        }
        return confirm(orderCode, "DEMO-" + bookingId);
    }

    public List<String[]> payment(int userId, long bookingId) throws SQLException {
        try (Connection c = CSDL.getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT d.id,d.tong_tien,t.trang_thai,COALESCE(t.qr_payload,''),COALESCE(t.checkout_url,''),"
                + "d.expires_at,t.nha_cung_cap,d.xuat_chieu_id FROM don_dat_ve d JOIN thanh_toan t ON t.don_dat_ve_id=d.id "
                + "WHERE d.id=? AND d.nguoi_dung_id=?")) {
            ps.setLong(1, bookingId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Không tìm thấy thanh toán của bạn");
                }
                return Collections.singletonList(new String[]{rs.getString(1), rs.getBigDecimal(2).toPlainString(),
                    rs.getString(3), rs.getString(4), rs.getString(5), rs.getTimestamp(6).toLocalDateTime().toString(),
                    rs.getString(7), rs.getString(8)});
            }
        }
    }

    public List<Integer> expireStale() throws SQLException {
        List<Integer> shows = new ArrayList<>();
        try (Connection c = CSDL.getConnection()) {
            c.setAutoCommit(false);
            try {
                List<Long> ids = new ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement("SELECT id,xuat_chieu_id FROM don_dat_ve WHERE trang_thai='pending_payment' AND expires_at<=NOW() FOR UPDATE"); ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ids.add(rs.getLong(1));
                        shows.add(rs.getInt(2));
                    }
                }
                for (long id : ids) {
                    expireLocked(c, id, "expired");
                }
                c.commit();
                return shows;
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    private int finishWithoutPayment(long bookingId, String paymentState, String orderState) throws SQLException {
        try (Connection c = CSDL.getConnection()) {
            c.setAutoCommit(false);
            try {
                int showId;
                try (PreparedStatement ps = c.prepareStatement("SELECT xuat_chieu_id FROM don_dat_ve WHERE id=? FOR UPDATE")) {
                    ps.setLong(1, bookingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            return 0;
                        
                        }showId = rs.getInt(1);
                    }
                }
                expireLocked(c, bookingId, paymentState);
                if (!"expired".equals(orderState)) try (PreparedStatement ps = c.prepareStatement("UPDATE don_dat_ve SET trang_thai=? WHERE id=?")) {
                    ps.setString(1, orderState);
                    ps.setLong(2, bookingId);
                    ps.executeUpdate();
                }
                c.commit();
                return showId;
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    private static void expireLocked(Connection c, long bookingId, String paymentState) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE ve SET trang_thai='expired' WHERE don_dat_ve_id=? AND trang_thai='held'")) {
            ps.setLong(1, bookingId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement("UPDATE don_dat_ve SET trang_thai='expired' WHERE id=? AND trang_thai='pending_payment'")) {
            ps.setLong(1, bookingId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement("UPDATE thanh_toan SET trang_thai=? WHERE don_dat_ve_id=? AND trang_thai='pending'")) {
            ps.setString(1, paymentState);
            ps.setLong(2, bookingId);
            ps.executeUpdate();
        }
    }

    private static final class Product {

        final String name;
        final BigDecimal price;
        final int quantity;

        Product(String n, BigDecimal p, int q) {
            name = n;
            price = p;
            quantity = q;
        }
    }

    public static final class Reservation {

        public final long bookingId, orderCode, amount;
        public final int showId;
        public final LocalDateTime expires;

        Reservation(long b, int s, long o, long a, LocalDateTime e) {
            bookingId = b;
            showId = s;
            orderCode = o;
            amount = a;
            expires = e;
        }
    }

    public static final class Pending {

        public final long orderCode, bookingId;
        public final int showId;
        public final LocalDateTime expires;

        Pending(long o, long b, int s, LocalDateTime e) {
            orderCode = o;
            bookingId = b;
            showId = s;
            expires = e;
        }
    }
}
