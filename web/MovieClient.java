package web;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Một TCP connection cho cả phản hồi và sự kiện server push. Chỉ readerThread
 * được đọc socket. Mỗi frame kết thúc bằng END. Callback phải trả về nhanh,
 * không gọi request() trực tiếp trên readerThread.
 */
public final class MovieClient implements AutoCloseable {

    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 2040;
    public static final int DEFAULT_TIMEOUT_MS = 8000;

    private final Socket socket;
    private final BufferedReader input;
    private final BufferedWriter output;
    private final int timeoutMs;
    private final Object requestLock = new Object();
    private final Object stateLock = new Object();
    private CompletableFuture<List<String>> pending;
    private boolean closed;
    private volatile IntConsumer seatChangeListener = id -> {
    };
    private volatile Consumer<IOException> disconnectListener = error -> {
    };
    private volatile Consumer<List<String>> chatEventListener = event -> {
    };

    public MovieClient() throws IOException {
        this(DEFAULT_HOST, DEFAULT_PORT, DEFAULT_TIMEOUT_MS);
    }

    public MovieClient(String host, int port, int timeoutMs) throws IOException {
        Objects.requireNonNull(host, "host");
        if (host.trim().isEmpty() || port < 1 || port > 65535 || timeoutMs < 1) {
            throw new IllegalArgumentException("Thông số kết nối không hợp lệ");
        }
        this.timeoutMs = timeoutMs;
        socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            // Kết nối nhàn rỗi vẫn sống để nhận push; timeout đặt trên từng request.
            socket.setTcpNoDelay(true);
            input = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8));
            output = new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException ex) {
            try {
                socket.close();
            } catch (IOException closeEx) {
                ex.addSuppressed(closeEx);
            }
            throw ex;
        }
        Thread readerThread = new Thread(this::readLoop, "movie-client-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public void setSeatChangeListener(IntConsumer listener) {
        seatChangeListener = Objects.requireNonNull(listener, "listener");
    }

    public void setDisconnectListener(Consumer<IOException> listener) {
        disconnectListener = Objects.requireNonNull(listener, "listener");
    }

    public void setChatEventListener(Consumer<List<String>> listener) {
        chatEventListener = Objects.requireNonNull(listener, "listener");
    }

    public List<String> request(String command) throws IOException {
        Objects.requireNonNull(command, "command");
        if (command.trim().isEmpty() || command.indexOf('\n') >= 0
                || command.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Yêu cầu phải nằm trên một dòng");
        }
        // Giao thức cũ không có requestId: chỉ một request chờ phản hồi mỗi lần.
        synchronized (requestLock) {
            CompletableFuture<List<String>> response = new CompletableFuture<>();
            synchronized (stateLock) {
                if (closed) {
                    throw new IOException("Kết nối đã đóng");
                }
                pending = response;
            }
            try {
                output.write(command);
                output.newLine();
                output.flush();
                return response.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException ex) {
                SocketTimeoutException timeout = new SocketTimeoutException("Máy chủ phản hồi quá chậm");
                fail(timeout, true);
                throw timeout;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                IOException interrupted = new IOException("Thao tác bị gián đoạn", ex);
                fail(interrupted, true);
                throw interrupted;
            } catch (ExecutionException ex) {
                throw new IOException("Kết nối bị gián đoạn", ex.getCause());
            } catch (IOException ex) {
                fail(ex, true);
                throw ex;
            }
        }
    }

    public String requestFirstLine(String command) throws IOException {
        List<String> lines = request(command);
        if (lines.isEmpty()) {
            throw new EOFException("Server trả phản hồi rỗng");
        }
        return lines.get(0);
    }

    private void readLoop() {
        try {
            List<String> frame = new ArrayList<>();
            String line;
            while ((line = input.readLine()) != null) {
                if ("END".equals(line)) {
                    dispatch(frame);
                    frame = new ArrayList<>();
                } else {
                    frame.add(line);
                }
            }
            throw new EOFException("Server đã đóng kết nối");
        } catch (IOException | RuntimeException ex) {
            fail(ex instanceof IOException ? (IOException) ex
                    : new IOException("Phản hồi máy chủ không hợp lệ", ex), true);
        }
    }

    private void dispatch(List<String> frame) throws IOException {
        if (!frame.isEmpty() && frame.get(0).startsWith("EVENT;")) {
            String[] fields = frame.get(0).split(";", -1);
            if (fields.length >= 3 && java.util.Arrays.asList("CHAT_MESSAGE", "CHAT_MEMBERS",
                    "CHAT_STATE", "CHAT_CLOSED", "CHAT_TYPING").contains(fields[1])) {
                if (Integer.parseInt(fields[2]) < 1 || fields.length != ("CHAT_TYPING".equals(fields[1]) ? 5 : 3)) {
                    throw new IOException("Sự kiện chat không hợp lệ");
                }
                try {
                    chatEventListener.accept(Collections.unmodifiableList(new ArrayList<>(frame)));
                } catch (RuntimeException ex) {
                    System.err.println("Lỗi callback chat: " + ex.getMessage());
                }
                return;
            }
            if (frame.size() != 1 || fields.length != 3
                    || !"SEATS_CHANGED".equals(fields[1])) {
                throw new IOException("Sự kiện máy chủ không hợp lệ");
            }
            int showId = Integer.parseInt(fields[2]);
            if (showId < 1) {
                throw new IOException("Mã suất chiếu không hợp lệ");
            }
            try {
                seatChangeListener.accept(showId);
            } catch (RuntimeException ex) {
                System.err.println("Lỗi callback cập nhật ghế: " + ex.getMessage());
            }
            return;
        }
        CompletableFuture<List<String>> response;
        synchronized (stateLock) {
            response = pending;
            pending = null;
        }
        if (response == null) {
            throw new IOException("Nhận phản hồi khi không có yêu cầu đang chờ");
        }
        response.complete(Collections.unmodifiableList(frame));
    }

    private void fail(IOException error, boolean notify) {
        CompletableFuture<List<String>> response;
        synchronized (stateLock) {
            if (closed) {
                return;
            }
            closed = true;
            response = pending;
            pending = null;
        }
        try {
            socket.close();
        } catch (IOException closeEx) {
            error.addSuppressed(closeEx);
        }
        if (response != null) {
            response.completeExceptionally(error);
        }
        if (notify) {
            try {
                disconnectListener.accept(error);
            } catch (RuntimeException ex) {
                System.err.println("Lỗi callback ngắt kết nối: " + ex.getMessage());
            }
        }
    }

    @Override
    public void close() throws IOException {
        // Không chờ requestLock: đóng socket đánh thức request đang chờ ngay.
        fail(new IOException("Kết nối đã đóng"), false);
    }
}
