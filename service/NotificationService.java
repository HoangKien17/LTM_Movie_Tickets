package service;

import java.sql.SQLException;
import java.util.*;
import repository.NotificationRepository;

public final class NotificationService {
    private final NotificationRepository repository = new NotificationRepository();
    public void verifyReady() throws SQLException { repository.verifyReady(); }
    public List<String[]> list(int userId) throws SQLException { return repository.list(userId); }
    public void markRead(int userId, String values) throws SQLException {
        if (values == null || values.isEmpty()) return;
        String[] parts=values.split(",",-1);
        if (parts.length>50) throw new IllegalArgumentException("Chỉ đánh dấu tối đa 50 thông báo mỗi lần");
        List<Long> ids = new ArrayList<>();
        for (String part:parts) {
            if (!part.matches("[1-9][0-9]{0,18}")) throw new IllegalArgumentException("Mã thông báo không hợp lệ");
            try { ids.add(Long.parseLong(part)); }
            catch (NumberFormatException ex) { throw new IllegalArgumentException("Mã thông báo không hợp lệ"); }
        }
        repository.markRead(userId, ids);
    }
}
