package service;

import config.AppConfig;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import utils.PasswordUtil;

/** Gmail SMTP submission over implicit TLS (465), using only the JDK. */
public final class SmtpResetMailer implements PasswordResetService.Mailer {
    @Override public void verifyConfiguration() {
        String user = AppConfig.get("MOVIE_SMTP_USER", "");
        String secret = AppConfig.get("MOVIE_SMTP_APP_PASSWORD", "").replace(" ", "");
        if (!PasswordUtil.isValidEmail(user) || secret.isEmpty()) {
            throw new IllegalArgumentException("Chức năng gửi email chưa được thiết lập. Vui lòng liên hệ quản trị viên.");
        }
    }
    @Override public void send(String recipient, String code) throws IOException {
        verifyConfiguration();
        String user = AppConfig.get("MOVIE_SMTP_USER", "");
        String secret = AppConfig.get("MOVIE_SMTP_APP_PASSWORD", "").replace(" ", "");
        String host = AppConfig.get("MOVIE_SMTP_HOST", "smtp.gmail.com");
        if (!PasswordUtil.isValidEmail(recipient) || !code.matches("[0-9]{6}")) {
            throw new IllegalArgumentException("Thông tin email khôi phục không hợp lệ");
        }
        try (Socket transport = new Socket()) {
            transport.connect(new InetSocketAddress(host, 465), 5000);
            try (SSLSocket socket = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault())
                    .createSocket(transport, host, 465, true)) {
                socket.setSoTimeout(8000);
                SSLParameters parameters = socket.getSSLParameters();
                parameters.setEndpointIdentificationAlgorithm("HTTPS");
                socket.setSSLParameters(parameters);
                socket.startHandshake();
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));
                deliver(input, output, user, secret, recipient, code);
            }
        }
    }

    static void deliver(BufferedReader input, BufferedWriter output, String user, String secret,
            String recipient, String code) throws IOException {
        expect(input, 220);
        command(input, output, "EHLO galaxy-cinema.local", 250);
        command(input, output, "AUTH LOGIN", 334);
        command(input, output, base64(user), 334);
        command(input, output, base64(secret), 235);
        command(input, output, "MAIL FROM:<" + user + ">", 250);
        command(input, output, "RCPT TO:<" + recipient + ">", 250);
        command(input, output, "DATA", 354);
        String body = "GALAXY CINEMA\n\nMã khôi phục mật khẩu của bạn: " + code
                + "\n\nMã có hiệu lực trong 5 phút và chỉ dùng được một lần. Không chia sẻ mã này."
                + "\nNếu không yêu cầu khôi phục, bạn có thể bỏ qua email này. Mật khẩu hiện tại vẫn giữ nguyên.\n";
        String message = "From: Galaxy Cinema <" + user + ">\r\nTo: <" + recipient + ">\r\n"
                + "Date: " + DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now()) + "\r\n"
                + "Message-ID: <" + UUID.randomUUID() + "@galaxy-cinema.local>\r\n"
                + "Subject: =?UTF-8?B?" + base64("Mã khôi phục mật khẩu Galaxy Cinema") + "?=\r\n"
                + "MIME-Version: 1.0\r\nContent-Type: text/plain; charset=UTF-8\r\n"
                + "Content-Transfer-Encoding: base64\r\n\r\n"
                + Base64.getMimeEncoder(76, new byte[]{13, 10}).encodeToString(body.getBytes(StandardCharsets.UTF_8)) + "\r\n.\r\n";
        output.write(message); output.flush(); expect(input, 250);
        // Delivery has succeeded; a failure during QUIT must not invalidate the emailed code.
        try { output.write("QUIT\r\n"); output.flush(); } catch (IOException ignored) { }
    }
    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
    private static void command(BufferedReader input, BufferedWriter output, String command, int expected) throws IOException {
        output.write(command + "\r\n"); output.flush(); expect(input, expected);
    }
    private static void expect(BufferedReader input, int expected) throws IOException {
        for (int lines = 0; lines < 100; lines++) {
            String line = input.readLine();
            if (line == null || line.length() < 3 || !line.startsWith(Integer.toString(expected))) {
                throw new IOException("SMTP từ chối bước gửi mail (cần mã " + expected + ").");
            }
            if (line.length() == 3 || line.charAt(3) == ' ') return;
            if (line.charAt(3) != '-') throw new IOException("Phản hồi SMTP không hợp lệ.");
        }
        throw new IOException("Phản hồi SMTP quá dài.");
    }
}
