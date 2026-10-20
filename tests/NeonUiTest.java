package web;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import protocol.Protocol;
import server.ServerMonitor;

/**
 * UI flow against a local TCP fixture, plus rendered screenshots. No database
 * writes.
 */
public final class NeonUiTest {

    private static final Path QA = Path.of("../neon-ui-qa");

    public static void main(String[] args) throws Exception {
        Files.createDirectories(QA);
        try (Fixture server = new Fixture(); MovieClient client = new MovieClient("127.0.0.1", server.listener.getLocalPort(), 3000)) {
            BasicClientUI ui = edt(BasicClientUI::new);
            try {
                edt(() -> {
                    ui.addNotify();
                    set(ui, "client", client);
                    ((JLabel) get(ui, "userLabel")).setText("Xin chào, Kiên");
                    ((CardLayout) get(ui, "screenLayout")).show((JPanel) get(ui, "screens"), "MAIN");
                    invoke(ui, "navigate", new Class[]{String.class, boolean.class}, "MOVIES", false);
                    invoke(ui, "loadMovies", new Class[0]);
                    return null;
                });
                await(() -> !busy(ui) && ((List<?>) get(ui, "movies")).size() == 4, "movies loaded");
                edt(() -> {
                    capture(ui.getContentPane(), "01-client-home.png", 1440, 900);
                    capture(ui.getContentPane(), "01-client-home-small.png", 1100, 730);
                    JTextField search = (JTextField) get(ui, "movieSearch");
                    search.setText("HÀNH ĐỘNG");
                    check(((JLabel) get(ui, "movieCount")).getText().startsWith("2 phim"), "search ignores case");
                    search.setText("not-found");
                    check(((JLabel) get(ui, "movieCount")).getText().startsWith("0 phim"), "search empty state");
                    search.setText("");
                    Object movie = ((List<?>) get(ui, "movies")).get(0);
                    invoke(ui, "openMovie", new Class[]{movie.getClass()}, movie);
                    return null;
                });
                await(() -> !busy(ui) && ((FoodOrderPanel) get(ui, "foodOrder")).loaded(), "details and foods loaded");
                edt(() -> {
                    capture(ui.getContentPane(), "02-movie-details.png", 1440, 900);
                    ((JComboBox<?>) get(ui, "showDates")).setSelectedItem("2030-10-26");
                    findButton((Container) get(ui, "timeSlots"), "Chọn suất").doClick();
                    return null;
                });
                await(() -> !busy(ui) && (int) get(ui, "seatMapShowId") == 22, "filtered slot selects correct show");
                edt(() -> {
                    FoodOrderPanel food = (FoodOrderPanel) get(ui, "foodOrder");
                    JButton plus = findButton(food, "+");
                    plus.doClick();
                    plus.doClick();
                    check(food.items().equals("1:2"), "food wire payload");
                    check(food.total().intValueExact() == 70000, "food subtotal");
                    for (Component c : ((JPanel) get(ui, "seatGrid")).getComponents()) {
                        if (c instanceof JToggleButton && Arrays.asList("C4", "C5").contains(((JToggleButton) c).getText())) {
                            ((JToggleButton) c).doClick();
                        }
                    }
                    check(((JLabel) get(ui, "totalPrice")).getText().equals(BasicClientUI.formatMoney("220000")),
                            "seat and food grand total");
                    capture(ui.getContentPane(), "03-seats-food.png", 1440, 900);
                    capture(ui.getContentPane(), "03-seats-food-small.png", 1100, 730);
                    // Quantity limits and removing a food line.
                    for (int i = 0; i < 25; i++) {
                        plus.doClick();
                    }
                    check(food.items().equals("1:20"), "quantity cap");
                    JButton minus = findButton(food, "−");
                    for (int i = 0; i < 25; i++) {
                        minus.doClick();
                    }
                    check(food.items().isEmpty() && food.total().signum() == 0, "quantity lower bound");
                    paymentPreview(ui);
                    plus.doClick();
                    plus.doClick();
                    return null;
                });
                purchase(ui, server);
                edt(() -> {
                    adminPreview(ui);
                    return null;
                });
                await(() -> !busy(ui), "monitor loaded");
                edt(() -> {
                    capture(ui.getContentPane(), "06-admin-monitor.png", 1440, 900);
                    invoke(get(ui, "monitorPanel"), "stop", new Class[0]);
                    invoke(ui, "navigate", new Class[]{String.class, boolean.class}, "CHECK_IN", false);
                    capture(ui.getContentPane(), "07-staff-checkin.png", 1440, 900);
                    String token = "a".repeat(64);
                    JPanel ticket = ElectronicTicketDialog.content(new String[]{"123", "Hành trình ánh sao",
                        "Phòng 2", "2030-10-26T19:30", "C4,C5", "active", "", token,
                        "Bắp caramel × 2", "220000"}, null);
                    capture(ticket, "05-electronic-ticket.png", 900, 520);
                    invoke(ui, "resetSession", new Class[0]);
                    check(!((FoodOrderPanel) get(ui, "foodOrder")).loaded(), "logout clears basket");
                    ((CardLayout) get(ui, "screenLayout")).show((JPanel) get(ui, "screens"), "LOGIN");
                    capture(ui.getContentPane(), "08-login.png", 1440, 900);
                    return null;
                });
                System.out.println("PASS: search, date-to-show routing, basket totals/limits/reset, complete demo checkout to ticket QR, payment states, client/admin previews");
            } finally {
                edt(() -> {
                    invoke(ui, "closeClient", new Class[0]);
                    ui.dispose();
                    return null;
                });
            }
        }
    }

    private static void purchase(BasicClientUI ui, Fixture server) throws Exception {
        java.util.concurrent.atomic.AtomicBoolean paid = new java.util.concurrent.atomic.AtomicBoolean();
        java.util.concurrent.atomic.AtomicBoolean ticket = new java.util.concurrent.atomic.AtomicBoolean();
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        javax.swing.Timer automation = edt(() -> {
            javax.swing.Timer timer = new javax.swing.Timer(80, event -> {
                try {
                    for (Window window : Window.getWindows()) {
                        if (!(window instanceof JDialog) || !window.isVisible()) {
                            continue;
                        }
                        JDialog dialog = (JDialog) window;
                        if ("Xác nhận đơn".equals(dialog.getTitle())) {
                            JOptionPane pane = findOptionPane(dialog);
                            if (pane != null) {
                                pane.setValue(JOptionPane.YES_OPTION);
                            }
                        } else if (dialog.getTitle().startsWith("Thanh toán •") && paid.compareAndSet(false, true)) {
                            JButton button = findButton(dialog, "Giả lập thanh toán thành công");
                            if (button != null) {
                                button.doClick();
                            }
                        } else if (dialog.getTitle().startsWith("Vé điện tử")) {
                            ticket.set(true);
                            dialog.dispose();
                        }
                    }
                } catch (Throwable ex) {
                    failure.set(ex);
                }
            });
            timer.start();
            SwingUtilities.invokeLater(() -> {
                try {
                    invoke(ui, "book", new Class[0]);
                } catch (Throwable ex) {
                    failure.set(ex);
                }
            });
            return timer;
        });
        try {
            await(() -> ticket.get() || failure.get() != null, "automatic QR after successful payment");
            if (failure.get() != null) {
                throw new AssertionError(failure.get());
            }
            check(server.lastOrder.equals("CREATE_ORDER;22;" + Protocol.encode("C4,C5") + ";" + Protocol.encode("1:2")),
                    "checkout sends exact selected seats and food quantities");
            check(edt(() -> "BOOKINGS".equals(get(ui, "activePage"))), "payment navigates to tickets");
        } finally {
            edt(() -> {
                automation.stop();
                for (Window window : Window.getWindows()) {
                    if (window instanceof JDialog) {
                        window.dispose();
                    }
                }
                return null;
            });
        }
    }

    private static JOptionPane findOptionPane(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JOptionPane) {
                return (JOptionPane) c;
            }
            if (c instanceof Container) {
                JOptionPane p = findOptionPane((Container) c);
                if (p != null) {
                    return p;
            
                }}
        }
        return null;
    }

    private static void paymentPreview(JFrame owner) throws Exception {
        String[] payment = {"123", "220000", "pending", "DEMO-GALAXY-123", "",
            LocalDateTime.now().plusMinutes(9).toString(), "demo", "22"};
        PaymentView view = new PaymentView(owner, payment,
                "Hành trình ánh sao\nPhòng 2 • 26/10/2030 19:30\nGhế: C4, C5\nBắp caramel × 2",
                (request, done) -> {
                }, () -> {
                });
        try {
            capture(view.dialog.getContentPane(), "04-checkout.png", 900, 590);
            check(view.qr.getIcon() != null && view.demo.isEnabled(), "pending QR available");
            payment[2] = "expired";
            view.update();
            check(view.qr.getIcon() == null && !view.demo.isEnabled() && !view.checkout.isEnabled(),
                    "terminal payment hides QR and disables actions");
            payment[2] = "pending";
            payment[5] = LocalDateTime.now().minusSeconds(1).toString();
            view.update();
            check(!view.demo.isEnabled(), "local timeout cannot simulate a payment");
        } finally {
            view.dialog.dispose();
        }
    }

    private static void adminPreview(BasicClientUI ui) throws Exception {
        set(ui, "admin", true);
        set(ui, "staff", true);
        ((JPanel) get(ui, "adminNavGroup")).setVisible(true);
        ((JPanel) get(ui, "staffNavGroup")).setVisible(true);
        ((JLabel) get(ui, "userLabel")).setText("Xin chào, admin");
        invoke(ui, "navigate", new Class[]{String.class, boolean.class}, "ADMIN", false);
        DefaultTableModel model = (DefaultTableModel) get(ui, "adminModel");
        model.setColumnIdentifiers(new String[]{"Mã", "Sản phẩm", "Loại", "Giá", "Đang bán"});
        model.addRow(new String[]{"1", "Bắp caramel", "food", "35.000 ₫", "Có"});
        model.addRow(new String[]{"2", "Nước cam", "drink", "25.000 ₫", "Có"});
        ((JLabel) get(ui, "adminResultTitle")).setText("Danh mục đồ ăn & nước uống");
        JTabbedPane tabs = findTabs((Container) get(ui, "pages"));
        tabs.setSelectedIndex(3);
        capture(ui.getContentPane(), "06-admin-food.png", 1440, 900);
        capture(ui.getContentPane(), "06-admin-food-small.png", 1100, 730);
        invoke(ui, "navigate", new Class[]{String.class, boolean.class}, "MONITOR", true);
    }

    private static JTabbedPane findTabs(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JTabbedPane) {
                return (JTabbedPane) c;
            }
            if (c instanceof Container) {
                JTabbedPane p = findTabs((Container) c);
                if (p != null) {
                    return p;
            
                }}
        }
        return null;
    }

    private static JButton findButton(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton && text.equals(((JButton) c).getText())) {
                return (JButton) c;
            }
            if (c instanceof Container) {
                JButton b = findButton((Container) c, text);
                if (b != null) {
                    return b;
            
                }}
        }
        return null;
    }

    private static boolean busy(Object ui) throws Exception {
        return (boolean) get(ui, "busy");
    }

    private static Object get(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    private static void set(Object o, String name, Object value) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(o, value);
    }

    private static Object invoke(Object o, String name, Class<?>[] types, Object... args) throws Exception {
        Method m = o.getClass().getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(o, args);
    }

    private static <T> T edt(Callable<T> work) throws Exception {
        FutureTask<T> task = new FutureTask<>(work);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }

    private static void await(Callable<Boolean> ready, String label) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(6);
        while (System.nanoTime() < end) {
            if (edt(ready)) {
                return;
            
            }Thread.sleep(20);
        }
        throw new AssertionError("Timed out: " + label);
    }

    private static void check(boolean condition, String text) {
        if (!condition) {
            throw new AssertionError(text);
    
        }}

    private static void capture(Container panel, String name, int w, int h) throws Exception {
        panel.setSize(w, h);
        invalidate(panel);
        layout(panel);
        // Render the responsive grid after the viewport has its real width.
        Window owner = SwingUtilities.getWindowAncestor(panel);
        if (owner instanceof BasicClientUI) {
            invoke(owner, "renderMovies", new Class[0]);
        }
        invalidate(panel);
        layout(panel);
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        panel.printAll(graphics);
        graphics.dispose();
        ImageIO.write(image, "png", QA.resolve(name).toFile());
    }

    private static void layout(Container c) {
        c.doLayout();
        for (Component child : c.getComponents()) {
            if (child instanceof Container) {
                layout((Container) child);
            }
        }
    }

    private static void invalidate(Container c) {
        for (Component child : c.getComponents()) {
            if (child instanceof Container) {
                invalidate((Container) child);
            }
        }
        c.invalidate();
    }

    private static final class Fixture implements AutoCloseable {

        final ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        volatile Socket socket;
        volatile String lastOrder = "";
        volatile boolean paid;
        final FutureTask<Void> task;

        Fixture() throws IOException {
            task = new FutureTask<>(() -> {
                try (Socket s = listener.accept()) {
                    socket = s;
                    BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                    BufferedWriter output = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
                    String request;
                    while ((request = input.readLine()) != null) {
                        for (String line : response(request)) {
                            output.write(line);
                            output.newLine();
                        }
                        output.write("END\n");
                        output.flush();
                    }
                } catch (SocketException ex) {
                    if (!listener.isClosed()) {
                        throw ex;
                
                    }}
                return null;
            });
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        }

        List<String> response(String command) {
            if ("GET_MOVIES".equals(command)) {
                List<String> rows = new ArrayList<>();
                rows.add("OK;MOVIES;4");
                String[] titles = {"Hành trình ánh sao", "Mật mã thành phố", "Mùa hè rực rỡ", "Thế giới kỳ diệu"};
                for (int i = 0; i < 4; i++) {
                    rows.add("MOVIE;" + (i + 1) + ";" + Protocol.encode(titles[i])
                            + ";" + Protocol.encode(i < 2 ? "Hành động" : "Hoạt hình") + ";120;2030-10-20;"
                            + Protocol.encode("Một cuộc phiêu lưu đầy bất ngờ, nơi tình bạn và lòng can đảm đưa những người trẻ vượt qua giới hạn của chính mình.")
                            + ";" + Protocol.encode(""));
                }
                return rows;
            }
            if (command.startsWith("GET_SHOWS")) {
                return Arrays.asList("OK;SHOWS;3",
                        row("SHOW", "11", "1", "Hành trình ánh sao", "Phòng 1", "2030-10-25T19:30", "75000"),
                        row("SHOW", "12", "1", "Hành trình ánh sao", "Phòng 1", "2030-10-25T21:30", "75000"),
                        row("SHOW", "22", "1", "Hành trình ánh sao", "Phòng 2", "2030-10-26T19:30", "75000"));
            }
            if ("GET_PRODUCTS".equals(command)) {
                return Arrays.asList("OK;PRODUCTS;2",
                        row("PRODUCT", "1", "Bắp caramel", "food", "35000", "1"),
                        row("PRODUCT", "2", "Nước cam", "drink", "25000", "1"));
            }
            if (command.startsWith("CREATE_ORDER;")) {
                lastOrder = command;
            }
            if (command.startsWith("CREATE_ORDER;") || command.startsWith("GET_PAYMENT;")) {
                return Arrays.asList("OK;PAYMENT;1", row("PAYMENT", "123", "220000", paid ? "paid" : "pending",
                        "DEMO-GALAXY-123", "", LocalDateTime.now().plusMinutes(9).toString(), "demo", "22"));
            }
            if (command.startsWith("GET_TICKET;") || command.equals("GET_BOOKINGS")) {
                return Arrays.asList("OK;BOOKINGS;1", row("BOOKING", "123", "1", "Hành trình ánh sao",
                        "Phòng 2", "2030-10-26T19:30", "220000", paid ? "active" : "pending_payment", "C4,C5"));
            }
            if (command.startsWith("DEMO_CONFIRM_PAYMENT;")) {
                paid = true;
                return Arrays.asList("OK;PAID;123");
            }
            if (command.startsWith("GET_E_TICKET;")) {
                return Arrays.asList("OK;E_TICKET;1", row("TICKET", "123", "Hành trình ánh sao", "Phòng 2",
                        "2030-10-26T19:30", "C4,C5", "active", "", "a".repeat(64), "Bắp caramel × 2", "220000"));
            }
            if (command.startsWith("WATCH_SHOW")) {
                List<String> seats = new ArrayList<>();
                seats.add("OK;SEATS;48");
                for (char r = 'A'; r <= 'F'; r++) {
                    for (int c = 1; c <= 8; c++) {
                        seats.add(row("SEAT", "" + r + c, r == 'B' && c < 4 ? "taken" : "free"));
                    }
                }
                return seats;
            }
            if ("ADMIN_MONITOR".equals(command)) {
                ServerMonitor monitor = new ServerMonitor();
                long id = monitor.connected("127.0.0.1:52134");
                monitor.connected("127.0.0.1:52135");
                monitor.completed(id, "CREATE_ORDER;22", Arrays.asList("OK;PAYMENT;1"), 12300000);
                return monitor.snapshot();
            }
            throw new AssertionError("Unexpected request " + command);
        }

        static String row(String kind, String... values) {
            StringBuilder row = new StringBuilder(kind);
            for (String value : values) {
                row.append(';').append(Protocol.encode(value));
            }
            return row.toString();
        }

        public void close() throws Exception {
            listener.close();
            if (socket != null) {
                socket.close();
            
            }task.get(4, TimeUnit.SECONDS);
        }
    }
}
