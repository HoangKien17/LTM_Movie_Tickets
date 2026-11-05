package protocol;

import config.AppConfig;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public final class TicketCode {

    public static final String PREFIX = "GALAXY-TICKET:";
    private static final java.util.regex.Pattern TOKEN = java.util.regex.Pattern.compile(
            "([0-9a-f]{64})", java.util.regex.Pattern.CASE_INSENSITIVE);

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
        String cleanToken = token(token);
        String publicUrl = AppConfig.get("PUBLIC_TICKET_URL", "").trim();
        if (publicUrl.isEmpty()) {
            publicUrl = AppConfig.get("SERVER_PUBLIC_URL", "").trim();
        }
        if (!publicUrl.isEmpty()) {
            if (publicUrl.endsWith("/")) {
                publicUrl = publicUrl.substring(0, publicUrl.length() - 1);
            }
            return publicUrl + "/ticket?token=" + cleanToken;
        }

        String lan = findLanIp();
        String port = AppConfig.get("MOVIE_HTTP_PORT", "8080").trim();
        if (lan != null && !lan.isEmpty() && !"127.0.0.1".equals(lan)) {
            return "http://" + lan + ":" + port + "/ticket?token=" + cleanToken;
        }

        return PREFIX + cleanToken;
    }

    private static String findLanIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
