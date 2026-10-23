package web;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;

/** Booking preview only. The server creates the order after explicit confirmation. */
final class BookingConfirmDialog extends JDialog {

    private boolean confirmed;

    BookingConfirmDialog(Window owner, String movie, String room, String start,
            List<String> seats, String food, String ticketAmount, String foodAmount,
            String totalAmount) {
        super(owner, "Xác nhận đơn", ModalityType.DOCUMENT_MODAL);
        setName("booking-confirm-dialog");
        setUndecorated(true);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel surface = new JPanel(new BorderLayout(0, 22)) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                AuthStyle.quality(g);
                g.setPaint(new GradientPaint(0, 0, new Color(12, 25, 42),
                        getWidth(), getHeight(), new Color(21, 16, 37)));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 26, 26);
                g.setPaint(new GradientPaint(0, 0, UiTheme.PRIMARY,
                        getWidth(), getHeight(), UiTheme.PURPLE));
                g.setStroke(new BasicStroke(1.3f));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 26, 26);
                g.dispose();
            }
        };
        surface.setBorder(new EmptyBorder(24, 28, 24, 28));
        surface.add(header(), BorderLayout.NORTH);

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));

        UiTheme.RoundedPanel screening = UiTheme.surface();
        screening.setLayout(new BorderLayout(0, 14));
        JPanel movieHeading = new JPanel(new BorderLayout(0, 7));
        movieHeading.setOpaque(false);
        movieHeading.add(label("THÔNG TIN SUẤT CHIẾU", 11, Font.BOLD, UiTheme.PURPLE), BorderLayout.NORTH);
        JTextArea movieTitle = copy(movie, 21, UiTheme.TEXT);
        movieTitle.setName("confirmation-movie");
        movieTitle.setFont(UiTheme.font(21, Font.BOLD));
        movieTitle.setToolTipText(movie);
        movieHeading.add(movieTitle, BorderLayout.CENTER);
        screening.add(movieHeading, BorderLayout.NORTH);
        JPanel meta = new JPanel(new GridLayout(1, 2, 18, 0));
        meta.setOpaque(false);
        meta.add(metadata("PHÒNG CHIẾU", room, "ticket"));
        meta.add(metadata("NGÀY & GIỜ", date(start), "calendar"));
        screening.add(meta, BorderLayout.CENTER);
        details.add(screening);
        details.add(Box.createVerticalStrut(18));

        JPanel seatSection = new JPanel(new BorderLayout(0, 10));
        seatSection.setOpaque(false);
        JPanel seatHeading = new JPanel(new BorderLayout());
        seatHeading.setOpaque(false);
        seatHeading.add(label("Ghế đã chọn", 14, Font.BOLD, UiTheme.TEXT), BorderLayout.WEST);
        seatHeading.add(label(seats.size() + " ghế", 12, Font.PLAIN, UiTheme.MUTED), BorderLayout.EAST);
        seatSection.add(seatHeading, BorderLayout.NORTH);
        JPanel chips = new JPanel(new GridLayout(0, 5, 8, 8));
        chips.setName("confirmation-seats");
        chips.setOpaque(false);
        for (String seat : seats) {
            UiTheme.RoundedPanel chip = new UiTheme.RoundedPanel(UiTheme.PRIMARY_PALE, 10);
            chip.setLayout(new BorderLayout());
            chip.setBorder(new EmptyBorder(9, 8, 9, 8));
            JLabel code = label(seat, 14, Font.BOLD, UiTheme.PRIMARY);
            code.setHorizontalAlignment(SwingConstants.CENTER);
            chip.add(code, BorderLayout.CENTER);
            chips.add(chip);
        }
        seatSection.add(chips, BorderLayout.CENTER);
        details.add(seatSection);
        details.add(Box.createVerticalStrut(18));

        JPanel foodSection = new JPanel(new BorderLayout(0, 8));
        foodSection.setOpaque(false);
        foodSection.add(label("Bắp nước", 14, Font.BOLD, UiTheme.TEXT), BorderLayout.NORTH);
        JTextArea foodText = copy(food, 13, UiTheme.MUTED);
        foodText.setName("confirmation-food");
        foodSection.add(foodText, BorderLayout.CENTER);
        details.add(foodSection);
        details.add(Box.createVerticalStrut(18));

        UiTheme.RoundedPanel bill = UiTheme.surface();
        bill.setLayout(new BorderLayout(0, 14));
        JPanel subtotals = new JPanel(new GridLayout(2, 1, 0, 10));
        subtotals.setOpaque(false);
        subtotals.add(amountRow("Tiền vé (" + seats.size() + " ghế)", ticketAmount, false));
        subtotals.add(amountRow("Bắp nước", foodAmount, false));
        bill.add(subtotals, BorderLayout.CENTER);
        details.add(bill);

        JPanel hold = new JPanel(new BorderLayout(12, 0));
        hold.setOpaque(false);
        JLabel clock = new JLabel(HomeIcons.icon("calendar", 22));
        clock.setForeground(UiTheme.GOLD);
        clock.setVerticalAlignment(SwingConstants.TOP);
        hold.add(clock, BorderLayout.WEST);
        JPanel holdText = new JPanel(new BorderLayout(0, 5));
        holdText.setOpaque(false);
        holdText.add(label("Giữ ghế trong 10 phút", 13, Font.BOLD, UiTheme.GOLD), BorderLayout.NORTH);
        holdText.add(copy("Ghế được giữ khi tạo đơn. Vé điện tử được phát hành sau khi thanh toán thành công.",
                12, UiTheme.MUTED), BorderLayout.CENTER);
        hold.add(holdText, BorderLayout.CENTER);
        for (Component component : details.getComponents()) {
            if (component instanceof JComponent) {
                ((JComponent) component).setAlignmentX(Component.LEFT_ALIGNMENT);
            }
        }

        JScrollPane scroll = UiTheme.scrollContent(details);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(540, 390));
        surface.add(scroll, BorderLayout.CENTER);

        // Keep the total, hold notice and actions visible even for a long order.
        JPanel footer = new JPanel(new BorderLayout(0, 16));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.LINE), new EmptyBorder(16, 0, 0, 0)));
        JPanel summary = new JPanel(new BorderLayout(0, 12));
        summary.setOpaque(false);
        summary.add(amountRow("Tổng dự kiến", totalAmount, true), BorderLayout.NORTH);
        summary.add(hold, BorderLayout.CENTER);
        footer.add(summary, BorderLayout.NORTH);
        JPanel actions = new JPanel(new GridLayout(1, 2, 12, 0));
        actions.setOpaque(false);
        JButton edit = UiTheme.secondary("Chỉnh sửa đơn");
        edit.setName("booking-confirm-edit");
        edit.addActionListener(event -> dispose());
        JButton confirm = UiTheme.primary("Tiếp tục thanh toán");
        confirm.setName("booking-confirm-submit");
        confirm.addActionListener(event -> {
            confirmed = true;
            dispose();
        });
        for (JButton button : new JButton[]{edit, confirm}) {
            button.setFont(UiTheme.font(14, Font.BOLD));
            button.setPreferredSize(new Dimension(240, 48));
            actions.add(button);
        }
        footer.add(actions, BorderLayout.SOUTH);
        surface.add(footer, BorderLayout.SOUTH);
        setContentPane(surface);
        getRootPane().setDefaultButton(confirm);
        getRootPane().registerKeyboardAction(event -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getAccessibleContext().setAccessibleDescription("Kiểm tra đơn vé trước khi giữ ghế và tạo thanh toán.");
        pack();
        GraphicsConfiguration configuration = getGraphicsConfiguration();
        Rectangle bounds = configuration.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        setSize(Math.min(620, bounds.width - insets.left - insets.right - 32),
                Math.min(getHeight(), bounds.height - insets.top - insets.bottom - 32));
        setLocationRelativeTo(owner);
        try {
            setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 26, 26));
        } catch (UnsupportedOperationException ignored) {
        }
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout(0, 20));
        header.setOpaque(false);
        JPanel brandRow = new JPanel(new BorderLayout());
        brandRow.setOpaque(false);
        JLabel brand = new JLabel("GALAXY CINEMA", UiTheme.orbitIcon(32), SwingConstants.LEFT);
        brand.setIconTextGap(11);
        brand.setForeground(UiTheme.TEXT);
        brand.setFont(AuthStyle.wordmark(13));
        brandRow.add(brand, BorderLayout.WEST);
        JButton close = new JButton(HomeIcons.icon("close", 19));
        close.setName("booking-confirm-close");
        close.setToolTipText("Đóng và tiếp tục chọn ghế");
        close.getAccessibleContext().setAccessibleName("Đóng xác nhận đơn");
        close.setForeground(UiTheme.MUTED);
        close.setBorder(new EmptyBorder(7, 7, 7, 7));
        close.setBorderPainted(false);
        close.setContentAreaFilled(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addActionListener(event -> dispose());
        brandRow.add(close, BorderLayout.EAST);
        header.add(brandRow, BorderLayout.NORTH);
        JPanel heading = new JPanel(new BorderLayout(0, 7));
        heading.setOpaque(false);
        heading.add(label("Xác nhận đơn vé", 28, Font.BOLD, UiTheme.TEXT), BorderLayout.NORTH);
        heading.add(copy("Kiểm tra thông tin trước khi chuyển sang thanh toán.", 13, UiTheme.MUTED), BorderLayout.CENTER);
        header.add(heading, BorderLayout.CENTER);
        MouseAdapter drag = new MouseAdapter() {
            private Point pressed;

            @Override
            public void mousePressed(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event)) pressed = event.getLocationOnScreen();
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (pressed == null) return;
                Point next = event.getLocationOnScreen();
                setLocation(getX() + next.x - pressed.x, getY() + next.y - pressed.y);
                pressed = next;
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                pressed = null;
            }
        };
        brandRow.addMouseListener(drag);
        brandRow.addMouseMotionListener(drag);
        brand.addMouseListener(drag);
        brand.addMouseMotionListener(drag);
        return header;
    }

    private static JPanel metadata(String caption, String value, String icon) {
        JPanel panel = new JPanel(new BorderLayout(9, 0));
        panel.setOpaque(false);
        JLabel image = new JLabel(HomeIcons.icon(icon, 19));
        image.setForeground(UiTheme.PRIMARY);
        image.setVerticalAlignment(SwingConstants.TOP);
        panel.add(image, BorderLayout.WEST);
        JPanel words = new JPanel(new BorderLayout(0, 5));
        words.setOpaque(false);
        words.add(label(caption, 10, Font.BOLD, UiTheme.MUTED), BorderLayout.NORTH);
        words.add(copy(value, 13, UiTheme.TEXT), BorderLayout.CENTER);
        panel.add(words, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel amountRow(String caption, String amount, boolean total) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.add(label(caption, 13, total ? Font.BOLD : Font.PLAIN, total ? UiTheme.TEXT : UiTheme.MUTED), BorderLayout.WEST);
        JLabel value = label(amount, total ? 25 : 14, Font.BOLD, total ? UiTheme.PRIMARY : UiTheme.TEXT);
        value.setHorizontalAlignment(SwingConstants.RIGHT);
        if (total) value.setName("confirmation-total");
        row.add(value, BorderLayout.CENTER);
        return row;
    }

    private static JLabel label(String value, int size, int style, Color color) {
        JLabel label = new JLabel(value);
        label.setFont(UiTheme.font(size, style));
        label.setForeground(color);
        return label;
    }

    private static JTextArea copy(String value, int size, Color color) {
        JTextArea area = UiTheme.copy(value, size);
        area.setForeground(color);
        area.setBorder(null);
        ((DefaultCaret) area.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        return area;
    }

    private static String date(String value) {
        try {
            return LocalDateTime.parse(value.replace(' ', 'T'))
                    .format(DateTimeFormatter.ofPattern("HH:mm • dd/MM/yyyy"));
        } catch (RuntimeException ignored) {
            return value;
        }
    }

    static boolean show(Window owner, String movie, String room, String start,
            List<String> seats, String food, String ticketAmount, String foodAmount, String totalAmount) {
        BookingConfirmDialog dialog = new BookingConfirmDialog(owner, movie, room, start,
                seats, food, ticketAmount, foodAmount, totalAmount);
        try {
            dialog.setVisible(true);
            return dialog.confirmed;
        } finally {
            dialog.dispose();
        }
    }
}
