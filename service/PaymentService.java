package service;

import java.sql.SQLException;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import payment.*;
import repository.OrderRepository;

public final class PaymentService {

    private static final int HOLD_MINUTES = 10;
    private static final AtomicLong ORDER_CODES = new AtomicLong(System.currentTimeMillis());
    private final OrderRepository repository = new OrderRepository();
    private final PaymentProvider provider = PaymentProviders.create();

    public void verifyReady() throws SQLException {
        repository.verifyReady();
    }

    public boolean demoMode() {
        return "demo".equals(provider.name());
    }

    public List<String[]> products() throws SQLException {
        return repository.products(false);
    }

    public List<String[]> allProducts() throws SQLException {
        return repository.products(true);
    }

    public int addProduct(String name, String type, String price) throws SQLException {
        return repository.addProduct(productName(name), productType(type), productPrice(price));
    }

    public void updateProduct(int id, String name, String type, String price, String active) throws SQLException {
        if (id < 1) {
            throw new IllegalArgumentException("Mã sản phẩm không hợp lệ");
        }
        if (!"0".equals(active) && !"1".equals(active)) {
            throw new IllegalArgumentException("Trạng thái chỉ nhận 1 hoặc 0");
        }
        repository.updateProduct(id, productName(name), productType(type), productPrice(price), "1".equals(active));
    }

    public void deleteProduct(int id) throws SQLException {
        if (id < 1) {
            throw new IllegalArgumentException("Mã sản phẩm không hợp lệ");
        }
        repository.deleteProduct(id);
    }

    public String[] create(int userId, int showId, String seatsCsv, String itemsCsv) throws SQLException {
        List<String> seats = parseSeats(seatsCsv);
        Map<Integer, Integer> items = parseItems(itemsCsv);
        long orderCode = ORDER_CODES.incrementAndGet();
        OrderRepository.Reservation reservation = repository.reserve(userId, showId, seats, items, orderCode, provider.name(), HOLD_MINUTES);
        try {
            PaymentProvider.PaymentLink link = provider.create(orderCode, reservation.amount, "GALAXY" + reservation.bookingId,
                    reservation.expires.atZone(ZoneId.systemDefault()).toEpochSecond());
            repository.attach(reservation.bookingId, link.providerId, link.qrPayload, link.checkoutUrl);
            return repository.payment(userId, reservation.bookingId).get(0);
        } catch (Exception ex) {
            repository.failReservation(reservation.bookingId);
            throw new IllegalStateException("Không tạo được giao dịch thanh toán: " + ex.getMessage(), ex);
        }
    }

    public List<String[]> payment(int userId, long bookingId) throws SQLException {
        return repository.payment(userId, bookingId);
    }

    public int demoConfirm(int userId, long bookingId) throws SQLException {
        if (!demoMode()) {
            throw new IllegalArgumentException("Chức năng giả lập đã tắt");
        }
        return repository.demoConfirm(userId, bookingId);
    }

    public List<Integer> synchronizePending() throws SQLException {
        List<Integer> changed = new ArrayList<>();
        for (OrderRepository.Pending pending : repository.pending()) {
            if (pending.expires.isBefore(java.time.LocalDateTime.now())) {
                continue;
            }
            PaymentProvider.PaymentState state;
            try {
                state = provider.status(pending.orderCode);
            } catch (Exception ex) {
                System.err.println("Không đồng bộ được payOS cho đơn #" + pending.bookingId + ": " + ex.getMessage());
                continue;
            }
            if ("PAID".equalsIgnoreCase(state.status)) {
                int show = repository.confirm(pending.orderCode, state.reference);
                if (show > 0) {
                    changed.add(show);
                }
            }
        }
        changed.addAll(repository.expireStale());
        return changed;
    }

    private static List<String> parseSeats(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            throw new IllegalArgumentException("Hãy chọn ít nhất một ghế");
        }
        String[] parts = csv.split(",", -1);
        if (parts.length > 10) {
            throw new IllegalArgumentException("Mỗi lần đặt tối đa 10 ghế");
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String raw : parts) {
            String code = raw.trim().toUpperCase(Locale.ROOT);
            if (!code.matches("[A-Z][1-9][0-9]?") || !unique.add(code)) {
                throw new IllegalArgumentException("Danh sách ghế không hợp lệ");
        
            }}
        return new ArrayList<>(unique);
    }

    private static Map<Integer, Integer> parseItems(String csv) {
        Map<Integer, Integer> items = new LinkedHashMap<>();
        if (csv == null || csv.trim().isEmpty()) {
            return items;
        }
        for (String part : csv.split(",", -1)) {
            String[] pair = part.split(":", -1);
            if (pair.length != 2) {
                throw new IllegalArgumentException("Danh sách đồ ăn không hợp lệ");
            }
            int id = Integer.parseInt(pair[0]), quantity = Integer.parseInt(pair[1]);
            if (id < 1 || quantity < 1 || quantity > 20 || items.put(id, quantity) != null) {
                throw new IllegalArgumentException("Số lượng đồ ăn không hợp lệ");
            }
        }
        return items;
    }

    private static String productName(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty() || value.length() > 120) {
            throw new IllegalArgumentException("Tên sản phẩm phải có từ 1 đến 120 ký tự");
        }
        return value;
    }

    private static String productType(String type) {
        String value = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if (!"food".equals(value) && !"drink".equals(value)) {
            throw new IllegalArgumentException("Loại sản phẩm chỉ nhận food hoặc drink");
        }
        return value;
    }

    private static java.math.BigDecimal productPrice(String price) {
        try {
            java.math.BigDecimal value = new java.math.BigDecimal(price.trim());
            if (value.signum() < 0 || value.scale() > 2 || value.compareTo(new java.math.BigDecimal("9999999999.99")) > 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Giá sản phẩm không hợp lệ");
        }
    }
}
