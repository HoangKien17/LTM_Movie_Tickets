package web;

import java.awt.*;
import java.net.URI;
import java.time.*;
import java.util.List;
import java.util.function.*;
import javax.swing.*;
import protocol.Protocol;
import utils.QrImages;

/**
 * Payment presentation; the server remains the only source of payment status.
 */
final class PaymentView {

    final JDialog dialog;
    final BiConsumer<String, Consumer<List<String>>> send;
    final Runnable paid;
    final JLabel status = new JLabel(), countdown = new JLabel(), error = new JLabel(" ");
    final JLabel qr = new JLabel();
    final JButton demo = UiTheme.secondary("Giả lập thanh toán thành công");
    final JButton checkout = UiTheme.primary("Mở trang thanh toán");
    final Timer timer;
    String[] payment;
    boolean finished;

    PaymentView(JFrame owner, String[] payment, String order,
            BiConsumer<String, Consumer<List<String>>> send, Runnable paid) {
        this.payment = payment;
        this.send = send;
        this.paid = paid;
        dialog = new JDialog(owner, "Thanh toán • Đơn #" + payment[0], true);
        JPanel root = new JPanel(new BorderLayout(20, 20));
        root.setBackground(UiTheme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        root.add(new BookingSteps(3), BorderLayout.NORTH);
        JPanel columns = new JPanel(new GridLayout(1, 2, 20, 0));
        columns.setOpaque(false);
        UiTheme.RoundedPanel info = UiTheme.surface();
        info.setLayout(new BorderLayout(0, 16));
        JLabel heading = new JLabel("THANH TOÁN ĐƠN #" + payment[0]);
        heading.setFont(UiTheme.font(17, Font.BOLD));
        info.add(heading, BorderLayout.NORTH);
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JTextArea details = text(order, 14);
        details.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        content.add(details);
        content.add(Box.createVerticalStrut(18));
        JLabel amountLabel = new JLabel("TỔNG THANH TOÁN");
        amountLabel.setForeground(UiTheme.MUTED);
        content.add(amountLabel);
        JLabel amount = new JLabel(BasicClientUI.formatMoney(payment[1]));
        amount.setFont(UiTheme.font(30, Font.BOLD));
        amount.setForeground(UiTheme.PRIMARY);
        content.add(amount);
        content.add(Box.createVerticalStrut(22));
        status.setFont(UiTheme.font(14, Font.BOLD));
        content.add(status);
        content.add(Box.createVerticalStrut(8));
        countdown.setForeground(UiTheme.MUTED);
        countdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        content.add(countdown);
        content.add(Box.createVerticalStrut(18));
        content.add(text("Vé điện tử xuất hiện sau khi server xác nhận thanh toán. "
                + "Bạn có thể mở lại giao dịch trong mục Vé của tôi.", 13));
        for (Component c : content.getComponents()) {
            if (c instanceof JComponent) {
                ((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
            }
        }
        info.add(content, BorderLayout.CENTER);
        JButton close = UiTheme.subtle("Đóng • xem lại trong Vé của tôi");
        close.addActionListener(e -> dialog.dispose());
        info.add(close, BorderLayout.SOUTH);
        columns.add(info);
        UiTheme.RoundedPanel scan = UiTheme.surface();
        scan.setLayout(new BorderLayout(0, 12));
        boolean isDemo = "demo".equals(payment[6]);
        JLabel method = new JLabel(isDemo ? "DEMO • KHÔNG CHUYỂN TIỀN" : "QUÉT QR BẰNG ỨNG DỤNG NGÂN HÀNG",
                SwingConstants.CENTER);
        method.setFont(UiTheme.font(12, Font.BOLD));
        method.setForeground(isDemo ? UiTheme.GOLD : UiTheme.PRIMARY);
        scan.add(method, BorderLayout.NORTH);
        qr.setHorizontalAlignment(SwingConstants.CENTER);
        qr.setIcon(new ImageIcon(QrImages.encode(payment[3], 300)));
        scan.add(qr, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(0, 1, 0, 8));
        actions.setOpaque(false);
        error.setForeground(UiTheme.RED);
        actions.add(error);
        checkout.setVisible(!payment[4].isEmpty());
        checkout.addActionListener(e -> browse(payment[4]));
        actions.add(checkout);
        demo.setVisible(isDemo);
        demo.addActionListener(e -> confirmDemo());
        actions.add(demo);
        JButton check = UiTheme.secondary("Kiểm tra giao dịch");
        check.addActionListener(e -> refresh());
        actions.add(check);
        scan.add(actions, BorderLayout.SOUTH);
        columns.add(scan);
        root.add(columns, BorderLayout.CENTER);
        dialog.setContentPane(root);
        dialog.setSize(900, 620);
        dialog.setMinimumSize(new Dimension(850, 580));
        dialog.setLocationRelativeTo(owner);
        timer = new Timer(3000, e -> {
            update();
            refresh();
        });
        update();
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                timer.stop();
            }
        });
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    void open() {
        timer.start();
        dialog.setVisible(true);
        timer.stop();
    }

    void update() {
        boolean pending = "pending".equals(payment[2]);
        String state = payment[2];
        status.setText("paid".equals(state) ? "ĐÃ THANH TOÁN"
                : "expired".equals(state) ? "ĐƠN ĐÃ HẾT HẠN"
                : "cancelled".equals(state) || "failed".equals(state) ? "GIAO DỊCH ĐÃ ĐÓNG"
                : pending ? "ĐANG CHỜ THANH TOÁN" : "TRẠNG THÁI: " + state);
        status.setForeground("paid".equals(state) ? UiTheme.GREEN : pending ? UiTheme.GOLD : UiTheme.RED);
        boolean timeLeft = true;
        try {
            long seconds = Duration.between(LocalDateTime.now(), LocalDateTime.parse(payment[5])).getSeconds();
            timeLeft = seconds > 0;
            countdown.setText(seconds > 0 ? String.format("Giữ ghế còn lại: %02d:%02d", seconds / 60, seconds % 60)
                    : "Hết thời gian giữ ghế • đang đối chiếu server");
        } catch (RuntimeException ex) {
            countdown.setText("");
        }
        demo.setEnabled(pending && timeLeft);
        checkout.setEnabled(pending && timeLeft);
        if (!pending || !timeLeft) {
            qr.setIcon(null);
            qr.setText("paid".equals(state) ? "THANH TOÁN THÀNH CÔNG" : "MÃ THANH TOÁN KHÔNG CÒN KHẢ DỤNG");
            qr.setForeground(UiTheme.MUTED);
        }
        if (!pending) {
            timer.stop();
        }
    }

    void refresh() {
        if (!dialog.isDisplayable() || finished) {
            return;
        }
        send.accept("GET_PAYMENT;" + payment[0], lines -> {
            if (!dialog.isDisplayable() || finished) {
                return;
            }
            String[] next = decode(lines);
            if (next == null) {
                error.setText("Chưa cập nhật được. Hãy kiểm tra lại.");
                return;
            }
            error.setText(" ");
            payment = next;
            update();
            if ("paid".equals(next[2])) {
                finish();
            }
        });
    }

    void confirmDemo() {
        send.accept("DEMO_CONFIRM_PAYMENT;" + payment[0], lines -> {
            if (!dialog.isDisplayable() || finished) {
                return;
            }
            if (!lines.isEmpty() && lines.get(0).startsWith("OK;PAID;")) {
                refresh(); 
            }else {
                error.setText("Chưa xác nhận được. Hãy kiểm tra lại.");
            }
        });
    }

    void finish() {
        if (finished) {
            return;
        }
        finished = true;
        timer.stop();
        dialog.dispose();
        SwingUtilities.invokeLater(paid);
    }

    static String[] decode(List<String> lines) {
        if (lines.size() != 2 || !lines.get(0).equals("OK;PAYMENT;1")) {
            return null;
        }
        String[] p = lines.get(1).split(";", -1);
        if (p.length != 9 || !"PAYMENT".equals(p[0])) {
            return null;
        }
        String[] row = new String[8];
        for (int i = 0; i < 8; i++) {
            row[i] = Protocol.decode(p[i + 1]);
        }
        return row;
    }

    void browse(String url) {
        try {
            if (!Desktop.isDesktopSupported()) {
                throw new IllegalStateException();
            }
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception ex) {
            error.setText("Không mở được trình duyệt; hãy quét QR.");
        }
    }

    private static JTextArea text(String value, int size) {
        JTextArea area = new JTextArea(value);
        area.setOpaque(false);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(UiTheme.font(size, Font.PLAIN));
        area.setForeground(UiTheme.MUTED);
        area.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        return area;
    }
}
