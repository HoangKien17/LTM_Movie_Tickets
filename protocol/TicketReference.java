package protocol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Staff lookup accepts a booking number or an existing QR token. */
public final class TicketReference {
    private static final Pattern BOOKING = Pattern.compile("(?:#\\s*|GC-\\s*)?([0-9]{1,19})", Pattern.CASE_INSENSITIVE);

    private TicketReference() { }

    public static Long bookingId(String input) {
        String value = input == null ? "" : input.trim();
        // A token containing only digits is still a token, not a booking ID.
        if (value.matches("[0-9a-fA-F]{64}")) return null;
        Matcher match = BOOKING.matcher(value);
        if (!match.matches()) return null;
        try {
            long id = Long.parseLong(match.group(1));
            if (id > 0) return id;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Mã đơn phải là số nguyên dương hợp lệ, ví dụ #32");
    }

    public static String normalize(String input) {
        Long id = bookingId(input);
        return id == null ? TicketCode.payload(TicketCode.token(input)) : "#" + id;
    }
}
