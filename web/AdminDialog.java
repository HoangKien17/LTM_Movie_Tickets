package web;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Shared management forms. Closing a dialog never submits its values. */
final class AdminDialog extends JDialog {
    private final JPanel fields = new JPanel();
    private final JTextArea error = text("", 13, new Color(255, 141, 173));
    private final JButton submit, cancel;
    private boolean accepted;
    private Component firstField;

    AdminDialog(Window owner, String title, String description, String action, boolean danger) {
        super(owner, title, ModalityType.DOCUMENT_MODAL);
        setName("admin-dialog");
        setUndecorated(true);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel surface = new JPanel(new BorderLayout(0, 22)) {
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create(); AuthStyle.quality(g);
                g.setPaint(new GradientPaint(0, 0, new Color(13, 29, 46), getWidth(), getHeight(), new Color(23, 16, 41)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setPaint(new GradientPaint(0, 0, AuthStyle.CYAN, getWidth(), getHeight(), AuthStyle.PURPLE));
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new RoundRectangle2D.Double(1, 1, getWidth()-3, getHeight()-3, 26, 26));
                g.dispose();
            }
        };
        surface.setBorder(new EmptyBorder(25, 30, 26, 30));
        JPanel header = new JPanel(new BorderLayout(0, 20)); header.setOpaque(false);
        JPanel brand = new JPanel(new BorderLayout()); brand.setOpaque(false);
        JLabel wordmark = new JLabel("GALAXY CINEMA  /  QUẢN LÝ");
        wordmark.setFont(AuthStyle.wordmark(11)); wordmark.setForeground(AuthStyle.MUTED);
        brand.add(wordmark, BorderLayout.WEST);
        JButton close = new JButton(HomeIcons.icon("close", 18));
        close.setName("admin-close"); close.setForeground(AuthStyle.MUTED);
        close.setToolTipText("Đóng"); close.getAccessibleContext().setAccessibleName("Đóng");
        close.setBorder(new EmptyBorder(3, 6, 3, 0)); close.setContentAreaFilled(false);
        close.setBorderPainted(false); close.setFocusPainted(false);
        close.addActionListener(e -> dispose()); brand.add(close, BorderLayout.EAST);
        header.add(brand, BorderLayout.NORTH);
        JPanel heading = new JPanel(new BorderLayout(0, 10)); heading.setOpaque(false);
        JLabel titleLabel = new JLabel(title, HomeIcons.icon(danger ? "ticket" : "manage", 28), SwingConstants.LEFT);
        titleLabel.setIconTextGap(12); titleLabel.setFont(AuthStyle.font(27, true));
        titleLabel.setForeground(danger ? new Color(255, 141, 173) : AuthStyle.TEXT);
        heading.add(titleLabel, BorderLayout.NORTH);
        JTextArea subtitle = text(description, 14, AuthStyle.MUTED);
        subtitle.setPreferredSize(new Dimension(500, 44));
        heading.add(subtitle, BorderLayout.CENTER); header.add(heading, BorderLayout.CENTER);
        surface.add(header, BorderLayout.NORTH);
        fields.setOpaque(false); fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        JScrollPane scroll = UiTheme.scrollContent(fields);
        scroll.setOpaque(false); scroll.getViewport().setOpaque(false);
        surface.add(scroll, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(0, 14)); footer.setOpaque(false);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.LINE));
        error.setName("admin-error"); error.setPreferredSize(new Dimension(500, 40));
        error.setBorder(new EmptyBorder(10, 0, 0, 0)); footer.add(error, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0)); buttons.setOpaque(false);
        cancel = UiTheme.subtle(danger ? "Quay lại" : "Hủy bỏ"); cancel.setName("admin-cancel");
        submit = danger ? UiTheme.danger(action) : UiTheme.primary(action); submit.setName("admin-submit");
        cancel.setPreferredSize(new Dimension(125, 46)); submit.setPreferredSize(new Dimension(180, 46));
        cancel.setFont(AuthStyle.font(15, true)); submit.setFont(AuthStyle.font(15, true));
        cancel.addActionListener(e -> dispose()); buttons.add(cancel); buttons.add(submit);
        footer.add(buttons, BorderLayout.SOUTH); surface.add(footer, BorderLayout.SOUTH);
        setContentPane(surface);
        getRootPane().setDefaultButton(danger ? cancel : submit);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        MouseAdapter drag = new MouseAdapter() {
            private Point start;
            public void mousePressed(MouseEvent e) { start = e.getPoint(); }
            public void mouseDragged(MouseEvent e) { if (start != null) setLocation(e.getXOnScreen()-start.x, e.getYOnScreen()-start.y); }
        };
        brand.addMouseListener(drag); brand.addMouseMotionListener(drag);
        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) { (danger || firstField == null ? cancel : firstField).requestFocusInWindow(); }
        });
    }

    void addField(String label, JComponent field) {
        if (firstField == null && field.isFocusable()) firstField = field;
        JPanel row = new JPanel(new BorderLayout(0, 9)); row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel caption = new JLabel(label); caption.setFont(AuthStyle.font(14, true)); caption.setForeground(AuthStyle.TEXT);
        caption.setLabelFor(field); field.getAccessibleContext().setAccessibleName(label);
        if (field instanceof javax.swing.text.JTextComponent) {
            ((javax.swing.text.JTextComponent)field).getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e) { error.setText(""); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { error.setText(""); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { error.setText(""); }
            });
        }
        if (field instanceof JComboBox) ((JComboBox<?>)field).addActionListener(e -> error.setText(""));
        row.add(caption, BorderLayout.NORTH); row.add(field, BorderLayout.CENTER);
        int height = field instanceof JScrollPane ? 118 : 77;
        row.setPreferredSize(new Dimension(490, height)); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        row.setMinimumSize(new Dimension(0, height)); fields.add(row); fields.add(Box.createVerticalStrut(17));
    }

    static JTextField field() {
        JTextField field = new JTextField(24) {
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D)graphics.create(); AuthStyle.quality(g);
                g.setColor(UiTheme.BACKGROUND); g.fillRoundRect(0,0,getWidth(),getHeight(),12,12); g.dispose();
                super.paintComponent(graphics);
            }
            protected void paintBorder(Graphics graphics) {
                Graphics2D g = (Graphics2D)graphics.create(); AuthStyle.quality(g);
                g.setColor(hasFocus() ? AuthStyle.CYAN : UiTheme.LINE);
                g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12); g.dispose();
            }
        };
        UiTheme.styleField(field); field.setOpaque(false); field.setBorder(new EmptyBorder(10,12,10,12));
        field.setFont(AuthStyle.font(16, false)); field.setPreferredSize(new Dimension(490, 48));
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { field.repaint(); }
            public void focusLost(FocusEvent e) { field.repaint(); }
        });
        return field;
    }

    boolean open(Supplier<String> validation) {
        submit.addActionListener(e -> {
            String message = validation.get();
            error.setText(message == null ? "" : message);
            if (message == null) { accepted = true; dispose(); }
        });
        Rectangle bounds = getGraphicsConfiguration().getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
        int availableHeight = bounds.height-insets.top-insets.bottom-32;
        pack();
        setSize(Math.min(620, bounds.width-40), Math.min(availableHeight, getPreferredSize().height+24));
        setLocationRelativeTo(getOwner());
        try { setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 26, 26)); }
        catch (UnsupportedOperationException ignored) { }
        try { setVisible(true); return accepted; } finally { dispose(); }
    }

    static String inputId(Window owner, String title, String label, boolean booking) {
        AdminDialog dialog = new AdminDialog(owner, title,
                "Nhập mã trong danh sách. Bạn sẽ được kiểm tra lại trước khi xác nhận.", "Tiếp tục", false);
        JTextField id = field(); id.setName("admin-id"); dialog.addField(label, id);
        if (!dialog.open(() -> validId(id.getText().trim(), booking) ? null : "Mã phải là số nguyên dương hợp lệ.")) return null;
        return id.getText().trim();
    }

    static boolean confirm(Window owner, String title, String message, String action) {
        AdminDialog dialog = new AdminDialog(owner, title, "Kiểm tra thông tin trước khi thực hiện thao tác.", action, true);
        JTextArea summary = text(message, 17, AuthStyle.TEXT);
        summary.setBorder(new EmptyBorder(16, 16, 16, 16)); summary.setOpaque(true); summary.setBackground(UiTheme.BACKGROUND);
        JScrollPane box = UiTheme.scroll(summary); box.setPreferredSize(new Dimension(490, 118));
        dialog.addField("XÁC NHẬN THAO TÁC", box);
        return dialog.open(() -> null);
    }

    static boolean validId(String value, boolean booking) {
        try { return value.matches("[0-9]+") && Long.parseLong(value) > 0
                && (booking || Long.parseLong(value) <= Integer.MAX_VALUE); }
        catch (NumberFormatException ex) { return false; }
    }

    /** Local checks keep typos inside the form; the server still enforces business rules. */
    static String validate(String title, String[] v) {
        boolean update = title.startsWith("Sửa");
        try {
            int offset = update ? 1 : 0;
            if (update && !validId(v[0], false)) return "Mã phải là số nguyên dương hợp lệ.";
            if (title.endsWith("phòng")) {
                if ((!update && v[offset].isEmpty()) || v[offset].length() > 100) return "Tên phòng cần có từ 1 đến 100 ký tự.";
                String rows = v[offset+1], seats = v[offset+2];
                if (update && rows.isEmpty() && seats.isEmpty()) return v[1].isEmpty() ? "Hãy nhập tên mới hoặc cấu hình ghế mới." : null;
                if (!range(rows, 26) || !range(seats, 99)) return "Nhập đủ số hàng (1–26) và số ghế mỗi hàng (1–99).";
            } else if (title.endsWith("suất chiếu")) {
                if (!validId(v[offset], false) || !validId(v[offset+1], false)) return "Mã phim và mã phòng phải là số nguyên dương.";
                try { if (!showTime(v[offset+2]).isAfter(LocalDateTime.now())) return "Giờ chiếu phải ở tương lai."; }
                catch (java.time.format.DateTimeParseException ex) { return "Nhập giờ chiếu theo mẫu 2030-10-21T19:30."; }
                return priceError(v[offset+3], false);
            } else if (title.endsWith("phim")) {
                if ((!update && v[offset].isEmpty()) || v[offset].length()>200) return "Tên phim cần có từ 1 đến 200 ký tự.";
                if (v[offset+1].length()>100 || v[offset+3].length()>10000) return "Thể loại tối đa 100 ký tự; mô tả tối đa 10.000 ký tự.";
                if (!(update && v[offset+2].isEmpty()) && !range(v[offset+2], 1000)) return "Thời lượng phải từ 1 đến 1.000 phút.";
            }
            return null;
        } catch (NumberFormatException ex) { return "Vui lòng kiểm tra lại các trường dạng số."; }
    }

    static String priceError(String value, boolean product) {
        try {
            BigDecimal price = new BigDecimal(value);
            if ((product ? price.signum()<0 : price.signum()<=0) || price.scale()>2 || price.compareTo(new BigDecimal("9999999999.99"))>0)
                return "Giá " + (product ? "không được âm" : "phải lớn hơn 0") + ", tối đa 2 số thập phân; tối đa 9999999999.99.";
            return null;
        } catch (NumberFormatException ex) { return "Nhập giá dạng số, ví dụ 90000 (không có dấu phân cách nghìn)."; }
    }
    private static boolean range(String value, int max) { return validId(value, false) && Integer.parseInt(value)<=max; }
    private static LocalDateTime showTime(String value) {
        if (value.indexOf('/')>=0) return LocalDateTime.parse(value,
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm")
                        .withResolverStyle(java.time.format.ResolverStyle.STRICT));
        return LocalDateTime.parse(value.replace(' ', 'T'));
    }
    private static JTextArea text(String value, int size, Color color) {
        JTextArea area = new JTextArea(value); area.setEditable(false); area.setFocusable(false);
        area.setOpaque(false); area.setLineWrap(true); area.setWrapStyleWord(true);
        area.setFont(AuthStyle.font(size, false)); area.setForeground(color); return area;
    }
}
