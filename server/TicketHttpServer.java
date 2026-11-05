package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import config.AppConfig;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import protocol.TicketCode;
import service.TicketService;

/**
 * HTTP Web Server phục vụ việc xem vé điện tử trên điện thoại (quét mã QR qua Wi-Fi hoặc 5G/4G).
 */
public final class TicketHttpServer {

    private final TicketService tickets;
    private final int port;
    private HttpServer server;
    private java.util.concurrent.ExecutorService executor;

    public TicketHttpServer(TicketService tickets) {
        this.tickets = tickets;
        this.port = Integer.parseInt(AppConfig.get("MOVIE_HTTP_PORT", "8080"));
    }

    public synchronized void start() {
        if (server != null) {
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/ticket", new TicketHandler());
            server.createContext("/health", new HealthHandler());
            server.createContext("/", new HomeHandler());
            executor = Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r, "ticket-http");
                t.setDaemon(true);
                return t;
            });
            server.setExecutor(executor);
            server.start();

            String lanIp = getLanIp();
            String publicUrl = AppConfig.get("PUBLIC_TICKET_URL", "").trim();
            if (publicUrl.isEmpty()) {
                publicUrl = AppConfig.get("SERVER_PUBLIC_URL", "").trim();
            }

            System.out.println("======================================================================");
            System.out.println("🎬 [Galaxy Cinema] Ticket HTTP Web Server đã sẵn sàng tại cổng: " + port);
            System.out.println("📱 Xem vé nội bộ (cùng Wi-Fi): http://" + (lanIp != null ? lanIp : "localhost") + ":" + port + "/ticket?token=<token>");
            if (!publicUrl.isEmpty()) {
                System.out.println("🌍 Xem vé từ xa (5G/4G qua Tunnel): " + publicUrl + "/ticket?token=<token>");
            } else {
                System.out.println("💡 Mẹo quét bằng 5G/4G từ xa:");
                System.out.println("   1. Mở cửa sổ cmd/terminal mới và gõ: ngrok http " + port);
                System.out.println("   2. Thêm vào file .env dòng: PUBLIC_TICKET_URL=https://<id>.ngrok-free.app");
            }
            System.out.println("======================================================================");
        } catch (IOException ex) {
            System.err.println("Không thể khởi động Ticket HTTP Server tại cổng " + port + ": " + ex.getMessage());
        }
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public static String getLanIp() {
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
        return "127.0.0.1";
    }

    private class TicketHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query);
            String tokenParam = params.get("token");

            if (tokenParam == null || tokenParam.trim().isEmpty()) {
                sendHtml(exchange, 400, renderErrorPage("Thiếu thông tin vé", "Vui lòng quét đúng mã QR của vé hoặc cung cấp mã tra cứu token."));
                return;
            }

            try {
                String token = TicketCode.token(tokenParam);
                String[] ticket = tickets.inspect(token);
                if (ticket == null) {
                    sendHtml(exchange, 404, renderErrorPage("Không tìm thấy vé", "Mã vé không tồn tại hoặc đã bị xóa khỏi hệ thống."));
                    return;
                }
                sendHtml(exchange, 200, renderTicketPage(ticket));
            } catch (IllegalArgumentException ex) {
                sendHtml(exchange, 400, renderErrorPage("Mã vé không hợp lệ", ex.getMessage()));
            } catch (SQLException ex) {
                sendHtml(exchange, 500, renderErrorPage("Lỗi hệ thống", "Không thể truy vấn thông tin vé: " + ex.getMessage()));
            }
        }
    }

    private static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] bytes = "{\"status\":\"OK\",\"service\":\"Galaxy Cinema Ticket Service\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static class HomeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            String html = "<!DOCTYPE html><html lang='vi'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                    + "<title>Galaxy Cinema</title><style>body{background:#0b0f19;color:#fff;font-family:sans-serif;text-align:center;padding:50px;}</style></head>"
                    + "<body><h1>🎬 GALAXY CINEMA</h1><p>Hệ thống vé điện tử trực tuyến đang hoạt động.</p></body></html>";
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private void sendHtml(HttpExchange exchange, int statusCode, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return map;
        }
        for (String pair : query.split("&")) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                map.put(pair.substring(0, idx), pair.substring(idx + 1));
            } else {
                map.put(pair, "");
            }
        }
        return map;
    }

    private static String formatDate(String iso) {
        if (iso == null || iso.isEmpty()) {
            return "Chưa xác định";
        }
        try {
            LocalDateTime time = LocalDateTime.parse(iso);
            return time.format(DateTimeFormatter.ofPattern("HH:mm • dd/MM/yyyy"));
        } catch (Exception ex) {
            return iso;
        }
    }

    private static String formatMoney(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "0 đ";
        }
        try {
            double value = Double.parseDouble(raw);
            return String.format(java.util.Locale.GERMAN, "%,.0f đ", value);
        } catch (Exception ex) {
            return raw + " đ";
        }
    }

    private String renderTicketPage(String[] t) {
        String id = escape(t[0]);
        String movie = escape(t[1]);
        String room = escape(t[2]);
        String time = formatDate(t[3]);
        String seats = escape(t[4]);
        String state = t[5];
        String checkedInAt = t[6];
        String token = escape(t[7]);
        String food = t.length > 8 && !t[8].isEmpty() ? escape(t[8]) : null;
        String price = t.length > 9 ? formatMoney(t[9]) : "";

        boolean isChecked = checkedInAt != null && !checkedInAt.trim().isEmpty();
        boolean isActive = "active".equalsIgnoreCase(state);

        String badgeClass;
        String badgeText;
        String badgeIcon;

        if (isChecked) {
            badgeClass = "badge-success";
            badgeText = "ĐÃ SOÁT VÉ VÀO RẠP (" + formatDate(checkedInAt) + ")";
            badgeIcon = "✓";
        } else if (isActive) {
            badgeClass = "badge-valid";
            badgeText = "VÉ HỢP LỆ • SẴN SÀNG VÀO RẠP";
            badgeIcon = "★";
        } else {
            badgeClass = "badge-danger";
            badgeText = "VÉ KHÔNG CÒN KHẢ DỤNG (" + escape(state) + ")";
            badgeIcon = "✕";
        }

        StringBuilder seatBadges = new StringBuilder();
        for (String seat : seats.split(",")) {
            String s = seat.trim();
            if (!s.isEmpty()) {
                seatBadges.append("<span class='seat-pill'>").append(s).append("</span>");
            }
        }

        return "<!DOCTYPE html>"
                + "<html lang='vi'>"
                + "<head>"
                + "<meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<title>Vé điện tử #" + id + " - Galaxy Cinema</title>"
                + "<style>"
                + "* { box-sizing: border-box; margin: 0; padding: 0; }"
                + "body { background: #0B0F19; color: #E2E8F0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; min-height: 100vh; display: flex; justify-content: center; align-items: center; padding: 20px 16px; background-image: radial-gradient(circle at 50% 20%, #1e1b4b 0%, #0B0F19 80%); }"
                + ".container { width: 100%; max-width: 440px; margin: auto; }"
                + ".ticket-card { background: #131B2E; border-radius: 24px; border: 1px solid rgba(139, 92, 246, 0.35); box-shadow: 0 20px 50px rgba(0, 0, 0, 0.7), 0 0 30px rgba(124, 58, 237, 0.15); overflow: hidden; position: relative; }"
                + ".ticket-header { background: linear-gradient(135deg, #1e1b4b 0%, #2e1065 100%); padding: 22px 24px 18px; border-bottom: 1px solid rgba(255, 255, 255, 0.08); text-align: center; }"
                + ".brand-title { color: #a78bfa; font-size: 13px; font-weight: 800; letter-spacing: 2.5px; text-transform: uppercase; margin-bottom: 6px; display: flex; align-items: center; justify-content: center; gap: 8px; }"
                + ".movie-title { color: #FFFFFF; font-size: 22px; font-weight: 800; line-height: 1.3; margin: 10px 0 14px; text-shadow: 0 2px 10px rgba(0,0,0,0.5); }"
                + ".badge { display: inline-flex; align-items: center; gap: 6px; padding: 6px 14px; border-radius: 999px; font-size: 12px; font-weight: 700; letter-spacing: 0.5px; }"
                + ".badge-valid { background: rgba(56, 189, 248, 0.15); color: #38bdf8; border: 1px solid rgba(56, 189, 248, 0.4); }"
                + ".badge-success { background: rgba(52, 211, 153, 0.15); color: #34d399; border: 1px solid rgba(52, 211, 153, 0.4); }"
                + ".badge-danger { background: rgba(248, 113, 113, 0.15); color: #f87171; border: 1px solid rgba(248, 113, 113, 0.4); }"
                + ".cut-divider { position: relative; height: 32px; background: #131B2E; display: flex; align-items: center; justify-content: space-between; }"
                + ".notch { width: 24px; height: 32px; background: #0B0F19; }"
                + ".notch-left { border-radius: 0 16px 16px 0; border: 1px solid rgba(139, 92, 246, 0.35); border-left: none; }"
                + ".notch-right { border-radius: 16px 0 0 16px; border: 1px solid rgba(139, 92, 246, 0.35); border-right: none; }"
                + ".dashed-line { flex: 1; border-top: 2px dashed rgba(255, 255, 255, 0.15); margin: 0 10px; }"
                + ".ticket-body { padding: 18px 24px 24px; background: #131B2E; }"
                + ".info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px 14px; margin-bottom: 20px; }"
                + ".info-label { font-size: 11px; text-transform: uppercase; color: #94A3B8; font-weight: 600; letter-spacing: 0.8px; margin-bottom: 4px; }"
                + ".info-value { font-size: 15px; color: #F8FAFC; font-weight: 700; }"
                + ".seats-container { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 4px; }"
                + ".seat-pill { background: linear-gradient(135deg, #7c3aed 0%, #4f46e5 100%); color: #FFFFFF; font-weight: 800; font-size: 13px; padding: 3px 10px; border-radius: 6px; box-shadow: 0 2px 6px rgba(124, 58, 237, 0.3); }"
                + ".food-box { background: rgba(255, 255, 255, 0.03); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 12px; padding: 12px 14px; margin-bottom: 20px; font-size: 13px; color: #cbd5e1; display: flex; align-items: center; gap: 10px; }"
                + ".food-icon { font-size: 20px; }"
                + ".ticket-footer { border-top: 1px solid rgba(255, 255, 255, 0.08); padding-top: 16px; text-align: center; }"
                + ".order-code { font-size: 14px; color: #94A3B8; font-weight: 600; margin-bottom: 12px; }"
                + ".order-code span { color: #a78bfa; font-weight: 800; }"
                + ".btn-refresh { display: inline-flex; align-items: center; justify-content: center; width: 100%; background: #2563EB; color: #fff; font-size: 14px; font-weight: 700; padding: 12px; border-radius: 12px; border: none; cursor: pointer; text-decoration: none; transition: 0.2s; }"
                + ".btn-refresh:active { transform: scale(0.98); background: #1d4ed8; }"
                + ".hint-text { font-size: 11px; color: #64748B; margin-top: 14px; line-height: 1.5; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='container'>"
                + "  <div class='ticket-card'>"
                + "    <div class='ticket-header'>"
                + "      <div class='brand-title'>🪐 GALAXY CINEMA • VÉ ĐIỆN TỬ</div>"
                + "      <div class='movie-title'>" + movie + "</div>"
                + "      <div class='badge " + badgeClass + "'>" + badgeIcon + " " + badgeText + "</div>"
                + "    </div>"
                + "    <div class='cut-divider'>"
                + "      <div class='notch notch-left'></div>"
                + "      <div class='dashed-line'></div>"
                + "      <div class='notch notch-right'></div>"
                + "    </div>"
                + "    <div class='ticket-body'>"
                + "      <div class='info-grid'>"
                + "        <div>"
                + "          <div class='info-label'>Phòng chiếu</div>"
                + "          <div class='info-value'>🏛 " + room + "</div>"
                + "        </div>"
                + "        <div>"
                + "          <div class='info-label'>Suất chiếu</div>"
                + "          <div class='info-value'>🕒 " + time + "</div>"
                + "        </div>"
                + "        <div style='grid-column: span 2;'>"
                + "          <div class='info-label'>Ghế đã đặt</div>"
                + "          <div class='seats-container'>" + seatBadges + "</div>"
                + "        </div>"
                + "      </div>"
                + (food != null ? "      <div class='food-box'><span class='food-icon'>🍿</span><div><div style='font-size:11px;color:#94a3b8;font-weight:700;'>BẮP NƯỚC / ĐỒ ĂN:</div><div>" + food + "</div></div></div>" : "")
                + "      <div class='ticket-footer'>"
                + "        <div class='order-code'>MÃ TRA CỨU ĐƠN: <span>#" + id + "</span> • Tổng tiền: <span style='color:#38bdf8;'>" + price + "</span></div>"
                + "        <button class='btn-refresh' onclick='window.location.reload()'>🔄 Làm mới trạng thái vé</button>"
                + "        <div class='hint-text'>Xuất trình màn hình vé này hoặc mã đơn tại quầy soát vé Galaxy Cinema khi vào phòng chiếu.</div>"
                + "      </div>"
                + "    </div>"
                + "  </div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }

    private String renderErrorPage(String title, String message) {
        return "<!DOCTYPE html>"
                + "<html lang='vi'>"
                + "<head>"
                + "<meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<title>Lỗi tra cứu vé - Galaxy Cinema</title>"
                + "<style>"
                + "body { background: #0B0F19; color: #E2E8F0; font-family: sans-serif; display: flex; justify-content: center; align-items: center; min-height: 100vh; padding: 20px; text-align: center; }"
                + ".card { background: #131B2E; border: 1px solid rgba(239, 68, 68, 0.4); border-radius: 20px; padding: 32px 24px; max-width: 400px; width: 100%; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }"
                + ".icon { font-size: 48px; margin-bottom: 16px; }"
                + "h2 { color: #f87171; font-size: 20px; margin-bottom: 10px; }"
                + "p { color: #94A3B8; font-size: 14px; line-height: 1.5; margin-bottom: 20px; }"
                + ".btn { display: inline-block; background: #374151; color: #fff; text-decoration: none; padding: 10px 20px; border-radius: 10px; font-weight: 600; font-size: 13px; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='card'>"
                + "  <div class='icon'>⚠️</div>"
                + "  <h2>" + escape(title) + "</h2>"
                + "  <p>" + escape(message) + "</p>"
                + "  <a href='javascript:history.back()' class='btn'>Quay lại</a>"
                + "</div>"
                + "</body>"
                + "</html>";
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
