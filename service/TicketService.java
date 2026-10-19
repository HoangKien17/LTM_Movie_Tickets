package service;

import repository.TicketRepository;
import java.sql.SQLException;

public final class TicketService {
    private final TicketRepository repository = new TicketRepository();

    public void verifyReady() throws SQLException { repository.verifyReady(); }

    public String[] ownTicket(int userId, long bookingId) throws SQLException {
        if (bookingId < 1) throw new IllegalArgumentException("Mã đơn không hợp lệ");
        return repository.ownTicket(userId, bookingId);
    }

    public String[] inspect(String payload) throws SQLException {
        return repository.inspect(token(payload));
    }

    public String[] checkIn(int staffId, String payload) throws SQLException {
        return repository.checkIn(staffId, token(payload));
    }

    public static String token(String payload) {
        return protocol.TicketCode.token(payload);
    }
}
