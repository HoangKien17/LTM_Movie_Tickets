package tests;

import protocol.Protocol;
import web.BasicClientUI;
import web.MovieClient;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Kiểm thử logic Swing thật với TCP server giả lập, không mở cửa sổ và không cần MySQL. */
public final class RealtimeUiTest {
    public static void main(String[] args) throws Exception {
        try (FakeServer server = new FakeServer();
             MovieClient client = new MovieClient("127.0.0.1", server.port(), 3000)) {
            BasicClientUI ui = edt(() -> new BasicClientUI());
            try {
                client.setSeatChangeListener(id -> SwingUtilities.invokeLater(() -> {
                    try {
                        call(ui, "onSeatsChanged", new Class<?>[]{int.class}, id);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }));
                edt(() -> {
                    put(ui, "client", client);
                    put(ui, "activePage", "SHOWS");
                    DefaultTableModel model = (DefaultTableModel) get(ui, "showModel");
                    model.addRow(new Object[]{"11", "Phim A", "Phong A", "", ""});
                    model.addRow(new Object[]{"22", "Phim B", "Phong B", "", ""});
                    ((JTable) get(ui, "showTable")).setRowSelectionInterval(0, 0);
                    return null;
                });
                await(() -> ready(ui, 11) && seat(ui, "A1") != null, "initial seat map");
                edt(() -> {
                    seat(ui, "A1").setSelected(true);
                    seat(ui, "A2").setSelected(true);
                    return null;
                });
                server.taken = true;
                server.push(11);
                await(() -> ready(ui, 11) && !seat(ui, "A1").isEnabled(), "booking event applied");
                check(edt(() -> !seat(ui, "A1").isSelected() && seat(ui, "A2").isSelected()),
                        "only taken seat loses selection");
                server.taken = false;
                server.push(11);
                await(() -> ready(ui, 11) && seat(ui, "A1").isEnabled(), "cancellation frees seat");
                check(edt(() -> seat(ui, "A2").isSelected()), "free selection retained");

                int before = server.watches.get();
                server.push(22);
                client.request("BARRIER");
                check(edt(() -> server.watches.get() == before), "other show event ignored");

                // Giữ response cũ lại, phát event mới trong lúc Swing đang busy.
                Gate gate = server.holdNext();
                edt(() -> { call(ui, "loadSeats", new Class<?>[0]); return null; });
                check(gate.entered.await(2, TimeUnit.SECONDS), "snapshot request started");
                server.taken = true;
                server.push(11);
                await(() -> (int) get(ui, "pendingSeatRefreshShowId") == 11, "busy event queued");
                gate.release.countDown();
                await(() -> ready(ui, 11) && !seat(ui, "A1").isEnabled()
                        && server.watches.get() >= before + 2, "queued event refreshes stale snapshot");

                // Đổi suất khi request trước chưa xong: snapshot cũ không được vẽ sang suất mới.
                Gate switchGate = server.holdNext();
                edt(() -> { call(ui, "loadSeats", new Class<?>[0]); return null; });
                check(switchGate.entered.await(2, TimeUnit.SECONDS), "old show request started");
                edt(() -> {
                    ((JTable) get(ui, "showTable")).setRowSelectionInterval(1, 1);
                    return null;
                });
                switchGate.release.countDown();
                await(() -> ready(ui, 22) && seat(ui, "B1") != null, "latest show loaded");
                check(edt(() -> seat(ui, "A1") == null && !seat(ui, "B1").isSelected()),
                        "no seats or selection carried across shows");
                System.out.println("PASS: Swing push refresh, selection preservation, busy events and rapid show switching");
            } finally {
                edt(() -> {
                    call(ui, "closeClient", new Class<?>[0]);
                    ui.dispose();
                    return null;
                });
            }
        }
    }

    private static boolean ready(BasicClientUI ui, int id) throws Exception {
        return !(boolean) get(ui, "busy") && (int) get(ui, "seatMapShowId") == id;
    }

    private static JToggleButton seat(BasicClientUI ui, String code) throws Exception {
        for (java.awt.Component component : ((JPanel) get(ui, "seatGrid")).getComponents()) {
            if (component instanceof JToggleButton && code.equals(((JToggleButton) component).getText())) {
                return (JToggleButton) component;
            }
        }
        return null;
    }

    private static Object get(Object object, String field) throws Exception {
        Field f = object.getClass().getDeclaredField(field);
        f.setAccessible(true);
        return f.get(object);
    }

    private static void put(Object object, String field, Object value) throws Exception {
        Field f = object.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(object, value);
    }

    private static void call(Object object, String method, Class<?>[] types, Object... args) throws Exception {
        Method m = object.getClass().getDeclaredMethod(method, types);
        m.setAccessible(true);
        m.invoke(object, args);
    }

    private static <T> T edt(Callable<T> action) throws Exception {
        FutureTask<T> result = new FutureTask<>(action);
        SwingUtilities.invokeAndWait(result);
        return result.get();
    }

    private static void await(Callable<Boolean> condition, String message) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
        while (System.nanoTime() < deadline) {
            if (edt(condition)) return;
            Thread.sleep(15);
        }
        throw new AssertionError("Timeout: " + message);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Gate {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
    }

    private static final class FakeServer implements AutoCloseable {
        final ServerSocket listener = new ServerSocket(0);
        final AtomicInteger watches = new AtomicInteger();
        volatile boolean taken;
        volatile Gate nextGate;
        volatile Socket socket;
        volatile BufferedWriter output;
        final FutureTask<Void> serving;

        FakeServer() throws IOException {
            serving = new FutureTask<>(() -> {
                try (Socket connection = listener.accept()) {
                    socket = connection;
                    BufferedReader input = new BufferedReader(new InputStreamReader(
                            connection.getInputStream(), StandardCharsets.UTF_8));
                    output = new BufferedWriter(new OutputStreamWriter(
                            connection.getOutputStream(), StandardCharsets.UTF_8));
                    String line;
                    while ((line = input.readLine()) != null) {
                        if (line.startsWith("WATCH_SHOW;")) {
                            watches.incrementAndGet();
                            int id = Integer.parseInt(line.substring("WATCH_SHOW;".length()));
                            List<String> snapshot = new ArrayList<>();
                            snapshot.add("OK;SEATS;" + (id == 11 ? 2 : 1));
                            snapshot.add(row(id == 11 ? "A1" : "B1", id == 11 && taken));
                            if (id == 11) snapshot.add(row("A2", false));
                            Gate gate = nextGate;
                            nextGate = null;
                            if (gate != null) {
                                gate.entered.countDown();
                                if (!gate.release.await(4, TimeUnit.SECONDS)) {
                                    throw new AssertionError("Gate not released");
                                }
                            }
                            send(snapshot);
                        } else {
                            send(Collections.singletonList("OK;BARRIER"));
                        }
                    }
                } catch (SocketException ex) {
                    if (!listener.isClosed()) throw ex;
                }
                return null;
            });
            Thread thread = new Thread(serving, "fake-cinema-server");
            thread.setDaemon(true);
            thread.start();
        }

        int port() { return listener.getLocalPort(); }

        Gate holdNext() {
            Gate gate = new Gate();
            nextGate = gate;
            return gate;
        }

        void push(int id) throws IOException {
            send(Collections.singletonList("EVENT;SEATS_CHANGED;" + id));
        }

        synchronized void send(List<String> frame) throws IOException {
            for (String line : frame) {
                output.write(line);
                output.newLine();
            }
            output.write("END\n");
            output.flush();
        }

        static String row(String code, boolean taken) {
            return "SEAT;" + Protocol.encode(code) + ";" + Protocol.encode(taken ? "taken" : "free");
        }

        @Override
        public void close() throws Exception {
            listener.close();
            if (socket != null) socket.close();
            serving.get(5, TimeUnit.SECONDS);
        }
    }
}
