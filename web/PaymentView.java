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
    final JLabel qr = new JLabel() {
        @Override
        public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);
            fitQr(width, height);
        }
    };
    private int qrSize;
    private String qrPayload;
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
        JPanel root = new JPanel(new BorderLayout(20, 18));
        root.setBackground(UiTheme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JPanel heading = new JPanel(new BorderLayout(0, 14));
        heading.setOpaque(false);
        JLabel pageTitle = new JLabel("Thanh toán đơn vé");
        pageTitle.setFont(UiTheme.font(25, Font.BOLD));
        heading.add(pageTitle, BorderLayout.SOUTH);
        root.add(heading, BorderLayout.NORTH);
        JPanel columns = new JPanel(new GridLayout(1, 2, 20, 0));
        columns.setOpaque(false);
        UiTheme.RoundedPanel info = UiTheme.surface();
        info.setLayout(new BorderLayout(0, 18));
        JPanel methods = new JPanel(new GridLayout(1, 3, 8, 0));
        methods.setOpaque(false);
        JButton bank = UiTheme.secondary("QR ngân hàng");
        bank.setToolTipText("Quét bằng ứng dụng ngân hàng; giao dịch được máy chủ xác nhận.");
        JButton card = UiTheme.subtle("Thẻ ngân hàng");
        JButton wallet = UiTheme.subtle("Ví điện tử");
        card.setEnabled(false);
        wallet.setEnabled(false);
        card.setToolTipText("Chưa hỗ trợ thanh toán thẻ trực tiếp");
        wallet.setToolTipText("Chưa hỗ trợ ví điện tử trực tiếp");
        for (JButton b : new JButton[]{bank, card, wallet}) {
            b.setFont(UiTheme.font(11, Font.BOLD));
            b.setBorder(BorderFactory.createEmptyBorder(8, 2, 8, 2));
            methods.add(b);
        }
        info.add(methods, BorderLayout.NORTH);
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        JPanel orderInfo = new JPanel(new BorderLayout(0, 8));
        orderInfo.setOpaque(false);
        JLabel orderId = new JLabel("THÔNG TIN ĐƠN #" + payment[0]);
        orderId.setForeground(UiTheme.PURPLE);
        orderId.setFont(UiTheme.font(13, Font.BOLD));
        orderInfo.add(orderId, BorderLayout.NORTH);
        JTextArea details = text(order, 14);
        JScrollPane orderScroll = UiTheme.scroll(details);
        orderScroll.setPreferredSize(new Dimension(300, 86));
        orderInfo.add(orderScroll, BorderLayout.CENTER);
        content.add(orderInfo, BorderLayout.NORTH);
        JPanel bill = new JPanel();
        bill.setOpaque(false);
        bill.setLayout(new BoxLayout(bill, BoxLayout.Y_AXIS));
        JLabel totalLabel = new JLabel("TỔNG THANH TOÁN");
        totalLabel.setForeground(UiTheme.MUTED);
        totalLabel.setFont(UiTheme.font(12, Font.BOLD));
        bill.add(totalLabel);
        JLabel amount = new JLabel(BasicClientUI.formatMoney(payment[1]));
        amount.setForeground(UiTheme.PRIMARY);
        amount.setFont(UiTheme.font(32, Font.BOLD));
        bill.add(amount);
        bill.add(Box.createVerticalStrut(12));
        status.setFont(UiTheme.font(14, Font.BOLD));
        bill.add(status);
        bill.add(Box.createVerticalStrut(8));
        countdown.setForeground(UiTheme.GOLD);
        countdown.setFont(UiTheme.font(14, Font.BOLD));
        bill.add(countdown);
        content.add(bill, BorderLayout.CENTER);
        JTextArea hint = text("Giữ ghế tối đa 10 phút. Vé điện tử được phát hành sau khi giao dịch thành công.", 12);
        hint.setRows(3);
        content.add(hint, BorderLayout.SOUTH);
        info.add(UiTheme.scrollContent(content), BorderLayout.CENTER);
        JButton close = UiTheme.subtle("Đóng • mở lại trong Vé của tôi");
        close.addActionListener(e -> dialog.dispose());
        info.add(close, BorderLayout.SOUTH);
        columns.add(info);
        TicketCard scan = new TicketCard();
        boolean isDemo = "demo".equals(payment[6]);
        JPanel ticketHeading = new JPanel(new GridLayout(3, 1, 0, 8));
        ticketHeading.setOpaque(false);
        JLabel brand = new JLabel("GALAXY CINEMA", SwingConstants.CENTER);
        brand.setFont(UiTheme.font(18, Font.BOLD));
        brand.setForeground(UiTheme.PRIMARY);
        ticketHeading.add(brand);
        JLabel scanTitle = new JLabel("MÃ THANH TOÁN", SwingConstants.CENTER);
        scanTitle.setFont(UiTheme.font(13, Font.BOLD));
        ticketHeading.add(scanTitle);
        JLabel method = new JLabel(isDemo ? "DEMO • KHÔNG CHUYỂN TIỀN" : "Quét QR bằng ứng dụng ngân hàng", SwingConstants.CENTER);
        method.setFont(UiTheme.font(11, Font.BOLD));
        method.setForeground(isDemo ? UiTheme.GOLD : UiTheme.MUTED);
        ticketHeading.add(method);
        scan.header(ticketHeading);
        qr.setHorizontalAlignment(SwingConstants.CENTER);
        qr.setVerticalAlignment(SwingConstants.CENTER);
        qr.setIcon(new ImageIcon(QrImages.encode(payment[3], 248)));
        scan.add(qr, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(0, 1, 0, 8));
        actions.setOpaque(false);
        error.setForeground(UiTheme.RED);
        error.setFont(UiTheme.font(11, Font.PLAIN));

        checkout.setVisible(!payment[4].isEmpty());
        checkout.addActionListener(e -> browse(payment[4]));
        if (checkout.isVisible()) {
            actions.add(checkout);
        }
        demo.setVisible(isDemo);
        demo.addActionListener(e -> confirmDemo());
        if (isDemo) {
            actions.add(demo);
        }
        JPanel leftActions = new JPanel(new BorderLayout(0, 8));
        leftActions.setOpaque(false);
        leftActions.add(error, BorderLayout.NORTH);
        leftActions.add(actions, BorderLayout.CENTER);
        leftActions.add(close, BorderLayout.SOUTH);
        info.add(leftActions, BorderLayout.SOUTH);
        JButton check = UiTheme.secondary("Kiểm tra giao dịch");
        check.addActionListener(e -> refresh());
        JPanel checkArea = new JPanel(new BorderLayout(0, 8));
        checkArea.setOpaque(false);
        checkArea.add(check, BorderLayout.CENTER);
        JLabel qrHint = new JLabel("Vé QR xuất hiện sau khi thanh toán", SwingConstants.CENTER);
        qrHint.setFont(UiTheme.font(11, Font.PLAIN));
        qrHint.setForeground(UiTheme.MUTED);
        checkArea.add(qrHint, BorderLayout.SOUTH);
        scan.add(checkArea, BorderLayout.SOUTH);
        columns.add(scan);
        root.add(columns, BorderLayout.CENTER);
        dialog.setContentPane(root);
        dialog.setSize(1040, 740);
        dialog.setMinimumSize(new Dimension(920, 680));
        dialog.setLocationRelativeTo(owner);
        final int[] ticks = {0};
        timer = new Timer(1000, e -> {
            update();
            if (++ticks[0] % 3 == 0) {
                refresh();
            }
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

    private void fitQr(int width, int height) {
        if (payment == null || !"pending".equals(payment[2]) || qr.getIcon() == null || width <= 0 || height <= 0) {
            return;
        }
        int size = Math.min(248, Math.min(width, height));
        if (size == qrSize && payment[3].equals(qrPayload)) {
            return;
        }
        qrSize = size;
        qrPayload = payment[3];
        // Regenerate at integer QR modules; scaling a bitmap can blur scanner edges.
        qr.setIcon(new ImageIcon(QrImages.encode(qrPayload, size)));
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
            } else {
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
