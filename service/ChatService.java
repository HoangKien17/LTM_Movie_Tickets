package service;

import java.sql.SQLException;
import java.util.*;
import protocol.Protocol;
import repository.ChatRepository;

/** Quy tắc chat; server đồng bộ trên instance này để snapshot/event giữ cùng thứ tự. */
public final class ChatService {
    private final ChatRepository repository;
    public ChatService() { this(new ChatRepository()); }
    public ChatService(ChatRepository repository) { this.repository = Objects.requireNonNull(repository); }
    public void verifyReady() throws SQLException { repository.verifyReady(); }
    public String[] room(int id) throws SQLException {
        if (id < 1) throw new IllegalArgumentException("Mã suất chiếu không hợp lệ");
        return repository.room(id);
    }
    public List<String> history(int showId, long beforeId) throws SQLException {
        if (beforeId < 0) throw new IllegalArgumentException("Mã tin nhắn không hợp lệ");
        List<String[]> data = new ArrayList<>(repository.history(showId, beforeId));
        boolean more = data.size() > ChatRepository.PAGE_SIZE;
        if (more) data.remove(0);
        List<String> result = new ArrayList<>();
        result.add("MORE;" + (more ? "1" : "0"));
        for (String[] message : data) result.add(row("CHAT_MSG", message));
        return result;
    }
    public String[] send(int showId, int userId, String text) throws SQLException {
        if (text == null) throw new IllegalArgumentException("Hãy nhập tin nhắn");
        String clean = text.strip();
        if (clean.isEmpty() || clean.codePointCount(0,clean.length()) > 500)
            throw new IllegalArgumentException("Tin nhắn cần từ 1 đến 500 ký tự");
        if (clean.codePoints().anyMatch(c -> Character.isISOControl(c)))
            throw new IllegalArgumentException("Tin nhắn không được chứa ký tự điều khiển");
        return repository.send(showId,userId,clean);
    }
    public static String row(String tag, String... fields) {
        StringBuilder result = new StringBuilder(tag);
        for (String field : fields) result.append(';').append(Protocol.encode(field));
        return result.toString();
    }
}
