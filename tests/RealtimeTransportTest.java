package tests;

import model.NguoiDung;
import server.BasicServer;
import web.MovieClient;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Kiểm thử TCP thật trên loopback, không cần MySQL. Chạy bằng main(). */
public final class RealtimeTransportTest {
    public static void main(String[] args) throws Exception {
        idlePushAndResponses();
        timeoutAndClose();
        serverRouting();
        System.out.println("PASS: idle push, framing, timeout, close, broadcast, logout and authorization");
    }

    private static void idlePushAndResponses() throws Exception {
        try (ServerSocket listener = new ServerSocket(0);
             MovieClient client = new MovieClient("127.0.0.1", listener.getLocalPort(), 1500);
             Socket peer = listener.accept()) {
            peer.setSoTimeout(3000);
            BufferedReader input = reader(peer);
            BufferedWriter output = writer(peer);
            BlockingQueue<Integer> events = new LinkedBlockingQueue<>();
            client.setSeatChangeListener(events::offer);
            // Chờ lâu hơn timeout của request: connection nhàn rỗi không được tự đóng.
            Thread.sleep(1650);
            output.write("EVENT;SEATS_CHANGED;7\nEND\n");
            output.flush();
            check(Integer.valueOf(7).equals(events.poll(2, TimeUnit.SECONDS)), "idle push");

            FutureTask<List<String>> request = run(() -> client.request("TEST"));
            check("TEST".equals(input.readLine()), "request written");
            // Fragment ở giữa END, rồi gộp nhiều frame trong cùng một lần ghi.
            output.write("EVENT;SEATS_CHANGED;8\nEN");
            output.flush();
            output.write("D\nOK;SEATS;1\nSEAT;QTE;ZnJlZQ\nEND\nEVENT;SEATS_CHANGED;9\nEND\n");
            output.flush();
            List<String> response = request.get(3, TimeUnit.SECONDS);
            check(response.equals(Arrays.asList("OK;SEATS;1", "SEAT;QTE;ZnJlZQ")), "response not polluted by events");
            check(Integer.valueOf(8).equals(events.poll(2, TimeUnit.SECONDS)), "event before response");
            check(Integer.valueOf(9).equals(events.poll(2, TimeUnit.SECONDS)), "event after response");

            CountDownLatch disconnected = new CountDownLatch(1);
            client.setDisconnectListener(error -> disconnected.countDown());
            peer.close();
            check(disconnected.await(2, TimeUnit.SECONDS), "idle EOF notification");
        }
        System.out.println("PASS: idle connection and mixed/fragmented frames");
    }

    private static void timeoutAndClose() throws Exception {
        try (ServerSocket listener = new ServerSocket(0);
             MovieClient client = new MovieClient("127.0.0.1", listener.getLocalPort(), 250);
             Socket peer = listener.accept()) {
            peer.setSoTimeout(2000);
            FutureTask<Boolean> request = run(() -> {
                try {
                    client.request("NO_RESPONSE");
                    return false;
                } catch (SocketTimeoutException expected) {
                    return true;
                }
            });
            check("NO_RESPONSE".equals(reader(peer).readLine()), "timeout request received");
            check(request.get(2, TimeUnit.SECONDS), "request timeout");
            try {
                client.request("LATE");
                throw new AssertionError("timed out connection reused");
            } catch (IOException expected) { }
        }
        try (ServerSocket listener = new ServerSocket(0);
             MovieClient client = new MovieClient("127.0.0.1", listener.getLocalPort(), 8000);
             Socket peer = listener.accept()) {
            peer.setSoTimeout(2000);
            FutureTask<Boolean> request = run(() -> {
                try {
                    client.request("WAIT");
                    return false;
                } catch (IOException expected) {
                    return true;
                }
            });
            check("WAIT".equals(reader(peer).readLine()), "pending request received");
            client.close();
            check(request.get(2, TimeUnit.SECONDS), "close unblocks pending request");
        }
        System.out.println("PASS: timeout and close while waiting");
    }

    @SuppressWarnings("unchecked")
    private static void serverRouting() throws Exception {
        // Chạy handler/socket/writer thật; chỉ gán phiên theo dõi để test không cần DB.
        // WATCH_SHOW + đặt/hủy qua MySQL được kiểm tra riêng trong Step7SmokeTest.
        BasicServer server = new BasicServer();
        Method handle = BasicServer.class.getDeclaredMethod("handleClient", Socket.class);
        handle.setAccessible(true);
        Method publish = BasicServer.class.getDeclaredMethod("seatsChanged", int.class);
        publish.setAccessible(true);
        Field sessionsField = BasicServer.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        Set<Object> sessions = (Set<Object>) sessionsField.get(server);
        try (ServerSocket listener = new ServerSocket(0);
             MovieClient a = new MovieClient("127.0.0.1", listener.getLocalPort(), 3000);
             Socket sa = listener.accept();
             MovieClient b = new MovieClient("127.0.0.1", listener.getLocalPort(), 3000);
             Socket sb = listener.accept()) {
            FutureTask<Void> handlerA = run(() -> { handle.invoke(server, sa); return null; });
            FutureTask<Void> handlerB = run(() -> { handle.invoke(server, sb); return null; });
            BlockingQueue<Integer> ea = new LinkedBlockingQueue<>();
            BlockingQueue<Integer> eb = new LinkedBlockingQueue<>();
            a.setSeatChangeListener(ea::offer);
            b.setSeatChangeListener(eb::offer);
            check(a.requestFirstLine("WATCH_SHOW;7").startsWith("ERR;"), "watch requires login");
            check(b.requestFirstLine("WATCH_SHOW;8").startsWith("ERR;"), "watch requires login B");
            Object sessionA = sessionFor(sessions, sa);
            Object sessionB = sessionFor(sessions, sb);
            set(sessionA, "user", new NguoiDung(1, "A", "a@test.com", "0123456789", "", "user"));
            set(sessionB, "user", new NguoiDung(2, "B", "b@test.com", "0123456788", "", "user"));
            set(sessionA, "watchedShowId", 7);
            set(sessionB, "watchedShowId", 8);

            publish.invoke(server, 7);
            check(Integer.valueOf(7).equals(ea.poll(2, TimeUnit.SECONDS)), "matching subscriber notified");
            b.request("UNKNOWN"); // FIFO barrier after broadcast
            check(eb.isEmpty(), "other show not notified");

            // Đồng thời gửi event và nhận phản hồi: không xen dòng hoặc mất END.
            FutureTask<Void> publisher = run(() -> {
                for (int i = 0; i < 20; i++) publish.invoke(server, 7);
                return null;
            });
            for (int i = 0; i < 20; i++) {
                List<String> response = a.request("UNKNOWN");
                check(response.size() == 1 && response.get(0).startsWith("ERR;"), "response framing");
            }
            publisher.get(3, TimeUnit.SECONDS);
            for (int i = 0; i < 20; i++) {
                check(Integer.valueOf(7).equals(ea.poll(2, TimeUnit.SECONDS)), "broadcast delivered");
            }
            check(a.requestFirstLine("WATCH_SHOW;0").equals("OK;WATCHING;0"), "unsubscribe");
            publish.invoke(server, 7);
            a.request("UNKNOWN");
            check(ea.isEmpty(), "unsubscribed client not notified");
            set(sessionA, "watchedShowId", 8);
            check(a.requestFirstLine("LOGOUT").equals("OK;LOGOUT"), "logout response");
            publish.invoke(server, 8);
            a.request("UNKNOWN");
            check(ea.isEmpty(), "logout clears subscription");
            check(Integer.valueOf(8).equals(eb.poll(2, TimeUnit.SECONDS)), "other session remains subscribed");
            a.close();
            b.close();
            handlerA.get(3, TimeUnit.SECONDS);
            handlerB.get(3, TimeUnit.SECONDS);
            check(sessions.isEmpty(), "disconnected sessions removed");
        }
        System.out.println("PASS: real server routing, writer framing, unsubscribe and session cleanup");
    }

    private static Object sessionFor(Set<Object> sessions, Socket socket) throws Exception {
        for (Object session : sessions) {
            Field field = session.getClass().getDeclaredField("socket");
            field.setAccessible(true);
            if (field.get(session) == socket) return session;
        }
        throw new AssertionError("Session missing");
    }

    private static void set(Object object, String name, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }

    private static BufferedReader reader(Socket socket) throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    }

    private static BufferedWriter writer(Socket socket) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    private static <T> FutureTask<T> run(Callable<T> task) {
        FutureTask<T> future = new FutureTask<>(task);
        Thread thread = new Thread(future, "realtime-test");
        thread.setDaemon(true);
        thread.start();
        return future;
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
