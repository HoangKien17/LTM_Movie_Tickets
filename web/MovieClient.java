package web;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Một kết nối TCP dùng chung cho một phiên làm việc của giao diện Swing.
 * Mỗi yêu cầu là một dòng; phản hồi kết thúc bằng dòng END.
 */
public final class MovieClient implements AutoCloseable {
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 2040;
    public static final int DEFAULT_TIMEOUT_MS = 8000;

    private final Socket socket;
    private final BufferedReader input;
    private final BufferedWriter output;
    private boolean closed;

    public MovieClient() throws IOException {
        this(DEFAULT_HOST, DEFAULT_PORT, DEFAULT_TIMEOUT_MS);
    }

    public MovieClient(String host, int port, int timeoutMs) throws IOException {
        Objects.requireNonNull(host, "host");
        if (host.trim().isEmpty() || port < 1 || port > 65535 || timeoutMs < 1) {
            throw new IllegalArgumentException("Thông số kết nối không hợp lệ");
        }

        socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            input = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8));
            output = new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException ex) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // Giữ nguyên lỗi kết nối ban đầu.
            }
            throw ex;
        }
    }

    public synchronized List<String> request(String command) throws IOException {
        if (closed) {
            throw new IOException("Kết nối đã đóng");
        }
        Objects.requireNonNull(command, "command");
        if (command.trim().isEmpty() || command.indexOf('\n') >= 0
                || command.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Yêu cầu phải nằm trên một dòng");
        }

        try {
            output.write(command);
            output.newLine();
            output.flush();

            List<String> lines = new ArrayList<>();
            String line;
            while ((line = input.readLine()) != null) {
                if ("END".equals(line)) {
                    return Collections.unmodifiableList(lines);
                }
                lines.add(line);
            }
            throw new EOFException("Server đóng kết nối trước dòng END");
        } catch (IOException ex) {
            try {
                close();
            } catch (IOException closeEx) {
                ex.addSuppressed(closeEx);
            }
            throw ex;
        }
    }

    public synchronized String requestFirstLine(String command) throws IOException {
        List<String> lines = request(command);
        if (lines.isEmpty()) {
            throw new EOFException("Server trả phản hồi rỗng");
        }
        return lines.get(0);
    }

    @Override
    public synchronized void close() throws IOException {
        if (!closed) {
            closed = true;
            socket.close();
        }
    }
}
