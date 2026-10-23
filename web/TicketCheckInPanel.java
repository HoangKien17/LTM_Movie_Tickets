package web;

import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.function.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import protocol.Protocol;
import protocol.TicketReference;
import utils.QrTickets;

final class TicketCheckInPanel extends JPanel {

    private final BiConsumer<String, Consumer<List<String>>> send;
    private final BooleanSupplier ready;
    private final JTextField input = UiTheme.hintField(22, "Ví dụ: #32 hoặc 32");
    private final JTextArea result = TicketDetailsView.text("Nhập mã đơn hoặc đọc ảnh QR để bắt đầu.", 13, UiTheme.MUTED);
    private final JButton confirm = UiTheme.primary("Xác nhận vào rạp");
    private final JPanel preview = new JPanel(new BorderLayout());
    private final Timer lookupTimer;
    private String inspected;
    private int generation, imageGeneration;
    private boolean reading;

    TicketCheckInPanel(BiConsumer<String, Consumer<List<String>>> send, BooleanSupplier ready) {
        super(new BorderLayout(18, 18));
        this.send = send;
        this.ready = ready;
        setBorder(BorderFactory.createEmptyBorder(24, 27, 24, 27));
        setBackground(UiTheme.BACKGROUND);
        JPanel heading = new JPanel(new BorderLayout(0, 7));
        heading.setOpaque(false);
        heading.add(TicketDetailsView.label("Soát vé điện tử", 27, Font.BOLD, UiTheme.TEXT), BorderLayout.NORTH);
        heading.add(TicketDetailsView.text("Tra cứu đơn vé, kiểm tra thông tin và xác nhận khách vào rạp.", 14, UiTheme.MUTED), BorderLayout.CENTER);
        add(heading, BorderLayout.NORTH);

        UiTheme.RoundedPanel desk = UiTheme.surface();
        desk.setPreferredSize(new Dimension(330, 0));
        desk.setLayout(new BorderLayout(0, 22));
        JPanel controls = new JPanel();
        controls.setOpaque(false);
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.add(TicketDetailsView.label("TRA CỨU VÉ", 12, Font.BOLD, UiTheme.PURPLE));
        controls.add(Box.createVerticalStrut(18));
        controls.add(TicketDetailsView.label("Mã đơn hoặc mã vé QR", 14, Font.BOLD, UiTheme.TEXT));
        controls.add(Box.createVerticalStrut(9));
        input.setName("check-in-reference");
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        input.setPreferredSize(new Dimension(260, 48));
        input.setToolTipText("Nhập #32, 32, GC-32 hoặc dán mã vé QR. Enter để tra cứu ngay.");
        controls.add(input);
        controls.add(Box.createVerticalStrut(9));
        JTextArea help = TicketDetailsView.text("Mã đơn sẽ tự tra cứu sau khi nhập. Bạn cũng có thể nhấn Enter hoặc bấm Tra cứu vé.", 12, UiTheme.MUTED);
        help.setRows(3);
        controls.add(help);
        controls.add(Box.createVerticalStrut(18));
        JButton inspect = UiTheme.primary("Tra cứu vé");
        inspect.setName("check-in-inspect");
        inspect.addActionListener(event -> inspect());
        inspect.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        controls.add(inspect);
        controls.add(Box.createVerticalStrut(10));
        JButton image = UiTheme.secondary("Đọc ảnh QR");
        image.setName("check-in-read-image");
        image.setIcon(HomeIcons.icon("ticket", 19));
        image.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        image.addActionListener(event -> readImage());
        controls.add(image);
        controls.add(Box.createVerticalStrut(10));
        JButton clear = UiTheme.subtle("Tra cứu đơn khác");
        clear.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        clear.addActionListener(event -> {
            reset();
            input.requestFocusInWindow();
        });
        controls.add(clear);
        controls.add(Box.createVerticalStrut(22));
        result.setName("check-in-feedback");
        result.setRows(4);
        controls.add(result);
        for (Component component : controls.getComponents()) {
            if (component instanceof JComponent) {
                ((JComponent) component).setAlignmentX(LEFT_ALIGNMENT);
            }
        }
        JScrollPane controlScroll = UiTheme.scrollContent(controls);
        controlScroll.setOpaque(false);
        controlScroll.getViewport().setOpaque(false);
        desk.add(controlScroll, BorderLayout.CENTER);
        JPanel admission = new JPanel(new BorderLayout(0, 12));
        admission.setOpaque(false);
        admission.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.LINE), BorderFactory.createEmptyBorder(16, 0, 0, 0)));
        admission.add(TicketDetailsView.text("Một lần xác nhận áp dụng cho toàn bộ ghế trong đơn.", 12, UiTheme.MUTED), BorderLayout.NORTH);
        confirm.setName("check-in-confirm");
        confirm.setPreferredSize(new Dimension(260, 48));
        confirm.setEnabled(false);
        confirm.addActionListener(event -> checkIn());
        admission.add(confirm, BorderLayout.SOUTH);
        desk.add(admission, BorderLayout.SOUTH);

        preview.setOpaque(false);
        emptyPreview();
        JPanel columns = new JPanel(new BorderLayout(18, 0));
        columns.setOpaque(false);
        columns.add(desk, BorderLayout.WEST);
        columns.add(preview, BorderLayout.CENTER);
        add(columns, BorderLayout.CENTER);

        lookupTimer = new Timer(650, event -> {
            if (!ready.getAsBoolean()) {
                if (isShowing()) {
                    lookupTimerRestart();
                }
                return;
            }
            try {
                TicketReference.normalize(input.getText());
                inspect();
            } catch (IllegalArgumentException ignored) {
                if (!input.getText().trim().isEmpty()) {
                    error("Nhập mã đơn như #32, 32 hoặc mã vé QR hợp lệ.");
                }
            }
        });
        lookupTimer.setRepeats(false);
        input.addActionListener(event -> inspect());
        input.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) {
                changed();
            }

            public void removeUpdate(DocumentEvent event) {
                changed();
            }

            public void changedUpdate(DocumentEvent event) {
                changed();
            }

            private void changed() {
                generation++;
                inspected = null;
                confirm.setEnabled(false);
                emptyPreview();
                result.setText(input.getText().trim().isEmpty() ? "Nhập mã đơn hoặc đọc ảnh QR để bắt đầu." : "Đang chờ tra cứu mã vừa nhập...");
                result.setForeground(UiTheme.MUTED);
                lookupTimer.restart();
            }
        });
    }

    private void lookupTimerRestart() {
        lookupTimer.restart();
    }

    @Override
    public void removeNotify() {
        lookupTimer.stop();
        super.removeNotify();
    }

    void reset() {
        generation++;
        imageGeneration++;
        reading = false;
        inspected = null;
        input.setText("");
        lookupTimer.stop();
        result.setText("Nhập mã đơn hoặc đọc ảnh QR để bắt đầu.");
        result.setForeground(UiTheme.MUTED);
        confirm.setEnabled(false);
        emptyPreview();
    }

    private void emptyPreview() {
        preview.removeAll();
        UiTheme.RoundedPanel empty = UiTheme.surface();
        empty.setLayout(new GridBagLayout());
        JPanel words = new JPanel(new GridLayout(3, 1, 0, 14));
        words.setOpaque(false);
        JLabel icon = new JLabel(HomeIcons.icon("ticket", 50), SwingConstants.CENTER);
        icon.setForeground(UiTheme.PRIMARY);
        words.add(icon);
        JLabel title = TicketDetailsView.label("Chưa có thông tin vé", 21, Font.BOLD, UiTheme.TEXT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        words.add(title);
        JLabel hint = TicketDetailsView.label("Nhập mã đơn hoặc đọc ảnh QR ở bên trái", 13, Font.PLAIN, UiTheme.MUTED);
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        words.add(hint);
        empty.add(words);
        preview.add(empty, BorderLayout.CENTER);
        preview.revalidate();
        preview.repaint();
    }

    private void showTicket(String[] ticket) {
        preview.removeAll();
        JScrollPane scroll = UiTheme.scrollContent(new TicketDetailsView(ticket));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        preview.add(scroll, BorderLayout.CENTER);
        preview.revalidate();
        preview.repaint();
    }

    private void inspect() {
        lookupTimer.stop();
        if (!ready.getAsBoolean() || reading) {
            if (isShowing()) {
                lookupTimer.restart();
            }
            return;
        }
        try {
            String reference = TicketReference.normalize(input.getText());
            int current = generation;
            inspected = null;
            confirm.setEnabled(false);
            result.setText("Đang tra cứu thông tin vé...");
            send.accept("INSPECT_TICKET;" + Protocol.encode(reference), lines -> {
                if (current != generation) {
                    return;
                }
                String[] ticket = decode(lines, "E_TICKET");
                if (ticket == null) {
                    emptyPreview();
                    return;
                }
                showTicket(ticket);
                inspected = reference;
                confirm.setEnabled(TicketDetailsView.usable(ticket));
                result.setForeground(TicketDetailsView.stateColor(ticket));
                result.setText("Đơn #" + ticket[0] + "\n" + TicketDetailsView.state(ticket)
                        + (TicketDetailsView.usable(ticket) ? "\nKiểm tra thông tin trước khi xác nhận." : "\nKhông thể xác nhận vào rạp."));
            });
        } catch (IllegalArgumentException ex) {
            error("Nhập mã đơn như #32, 32 hoặc mã vé QR hợp lệ.");
        }
    }

    private void checkIn() {
        lookupTimer.stop();
        if (!ready.getAsBoolean() || inspected == null || reading || !confirm.isEnabled()) {
            return;
        }
        String reference = inspected;
        int current = generation;
        inspected = null;
        confirm.setEnabled(false);
        send.accept("CHECK_IN;" + Protocol.encode(reference), lines -> {
            if (current != generation) {
                return;
            }
            String[] ticket = decode(lines, "CHECKED_IN");
            if (ticket == null) {
                emptyPreview();
                return;
            }
            if (ticket != null) {
                showTicket(ticket);
                result.setForeground(UiTheme.GREEN);
                result.setText("ĐÃ XÁC NHẬN VÀO RẠP\nĐơn #" + ticket[0] + "\nChúc quý khách xem phim vui vẻ!");
            }
        });
    }

    private void readImage() {
        if (reading || !ready.getAsBoolean()) {
            return;
        }
        JFileChooser picker = new JFileChooser();
        picker.setFileFilter(new FileNameExtensionFilter("Ảnh QR PNG/JPG", "png", "jpg", "jpeg"));
        if (picker.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            readImage(picker.getSelectedFile());
        }
    }

    private void readImage(File file) {
        lookupTimer.stop();
        int current = ++imageGeneration;
        int textGeneration = generation;
        inspected = null;
        confirm.setEnabled(false);
        reading = true;
        emptyPreview();
        result.setText("Đang đọc ảnh QR...");
        new SwingWorker<String, Void>() {
            protected String doInBackground() throws Exception {
                return QrTickets.decode(file);
            }

            protected void done() {
                if (current != imageGeneration) {
                    return;
                }
                reading = false;
                if (textGeneration != generation) {
                    lookupTimer.restart();
                    return;
                }
                try {
                    input.setText(get());
                    inspect();
                } catch (Exception ex) {
                    error("Không đọc được mã vé. Hãy chọn ảnh QR rõ nét hoặc nhập mã đơn.");
                }
            }
        }.execute();
    }

    private void error(String message) {
        result.setText(message);
        result.setForeground(UiTheme.RED);
    }

    private String[] decode(List<String> lines, String kind) {
        if (!lines.isEmpty() && lines.get(0).startsWith("ERR;")) {
            error(lines.get(0).substring(4));
            return null;
        }
        if (lines.size() != 2 || !lines.get(0).equals("OK;" + kind + ";1")) {
            error("Phản hồi máy chủ không hợp lệ");
            return null;
        }
        String[] row = lines.get(1).split(";", -1);
        if ((row.length != 9 && row.length != 11) || !"TICKET".equals(row[0])) {
            error("Dữ liệu vé không hợp lệ");
            return null;
        }
        String[] values = new String[row.length - 1];
        for (int i = 0; i < values.length; i++) {
            values[i] = Protocol.decode(row[i + 1]);
        }
        return values;
    }
}
