package protocol;

public final class TicketCode {
    public static final String PREFIX = "GALAXY-TICKET:";
    private TicketCode() { }
    public static String token(String payload) {
        String value = payload == null ? "" : payload.trim();
        if (value.startsWith(PREFIX)) value = value.substring(PREFIX.length());
        if (!value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Mã QR/mã vé không hợp lệ");
        }
        return value;
    }
    public static String payload(String token) { return PREFIX + token(token); }
}
