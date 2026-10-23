package service;

import java.sql.SQLException;
import repository.TicketRepository;

public final class TicketService {

    private final TicketRepository repository = new TicketRepository();

    public void verifyReady() throws SQLException {
        repository.verifyReady();
    }

    public String[] ownTicket(int userId, long bookingId) throws SQLException {
        if (bookingId < 1) {
            throw new IllegalArgumentException("Mã đơn không hợp lệ");
        }
        return repository.ownTicket(userId, bookingId);
    }

    public String[] inspect(String payload) throws SQLException {
        Long id = protocol.TicketReference.bookingId(payload);
        return id == null ? repository.inspect(token(payload)) : repository.inspectBooking(id);
    }

    public String[] checkIn(int staffId, String payload) throws SQLException {
        Long id = protocol.TicketReference.bookingId(payload);
        return id == null ? repository.checkIn(staffId, token(payload)) : repository.checkIn(staffId, id);
    }

    public static String token(String payload) {
        return protocol.TicketCode.token(payload);
    }
}
