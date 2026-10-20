package protocol;

public final class TicketCode {

    public static final String PREFIX = "GALAXY-TICKET:";
    private static final java.util.regex.Pattern TOKEN = java.util.regex.Pattern.compile(
            "(?:GALAXY-TICKET:)?([0-9a-f]{64})", java.util.regex.Pattern.CASE_INSENSITIVE);

    private TicketCode() {
    }

    public static String token(String payload) {
        String value = payload == null ? "" : payload.trim();
        java.util.regex.Matcher matcher = TOKEN.matcher(value);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Mã QR/mã vé không hợp lệ");
        }
        return matcher.group(1).toLowerCase(java.util.Locale.ROOT);
    }

    public static String payload(String token) {
        return PREFIX + token(token);
    }
}
