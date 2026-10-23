package web;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;

/** Shared, readable ticket presentation for the customer and the check-in desk. */
final class TicketDetailsView extends UiTheme.RoundedPanel {
    TicketDetailsView(String[] ticket) {
        super(UiTheme.SURFACE, 20);
        setName("ticket-details");
        setBorder(new EmptyBorder(22, 24, 22, 24));
        setLayout(new BorderLayout(0, 20));
        JPanel heading = new JPanel(new BorderLayout(0, 14));
        heading.setOpaque(false);
        JPanel reference = new JPanel(new BorderLayout(12, 0));
        reference.setOpaque(false);
        reference.add(label("ĐƠN #" + value(ticket, 0), 12, Font.BOLD, UiTheme.PRIMARY), BorderLayout.WEST);
        JLabel badge = label(state(ticket), 12, Font.BOLD, stateColor(ticket));
        badge.setName("ticket-state");
        reference.add(badge, BorderLayout.EAST);
        heading.add(reference, BorderLayout.NORTH);
        JTextArea title = text(value(ticket, 1), 26, UiTheme.TEXT);
        title.setFont(UiTheme.font(26, Font.BOLD));
        title.setName("ticket-movie");
        heading.add(title, BorderLayout.CENTER);
        add(heading, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JPanel meta = new JPanel(new GridLayout(2, 2, 16, 16));
        meta.setOpaque(false);
        meta.add(field("PHÒNG CHIẾU", value(ticket, 2)));
        meta.add(field("NGÀY CHIẾU", date(value(ticket, 3), "dd/MM/yyyy")));
        meta.add(field("GIỜ CHIẾU", date(value(ticket, 3), "HH:mm")));
        String[] seats = value(ticket, 4).isEmpty() ? new String[0] : value(ticket, 4).split(",");
        meta.add(field("SỐ LƯỢNG", seats.length + " ghế"));
        content.add(meta);
        content.add(Box.createVerticalStrut(20));
        JPanel seatSection = new JPanel(new BorderLayout(0, 9));
        seatSection.setOpaque(false);
        seatSection.add(label("GHẾ CỦA BẠN", 11, Font.BOLD, UiTheme.MUTED), BorderLayout.NORTH);
        JPanel chips = new JPanel(new GridLayout(0, 5, 7, 7));
        chips.setOpaque(false);
        chips.setName("ticket-seats");
        for (String code : seats) {
            JLabel seat = label(code.trim(), 14, Font.BOLD, UiTheme.PRIMARY);
            seat.setHorizontalAlignment(SwingConstants.CENTER);
            seat.setOpaque(true);
            seat.setBackground(UiTheme.PRIMARY_PALE);
            seat.setBorder(new EmptyBorder(8, 4, 8, 4));
            chips.add(seat);
        }
        seatSection.add(chips, BorderLayout.CENTER);
        content.add(seatSection);
        content.add(Box.createVerticalStrut(20));
        content.add(field("BẮP NƯỚC", value(ticket, 8).isEmpty() ? "Không chọn đồ ăn/nước uống" : value(ticket, 8)));
        if (!value(ticket, 6).isEmpty()) {
            content.add(Box.createVerticalStrut(16));
            content.add(field("ĐÃ SOÁT VÉ LÚC", date(value(ticket, 6), "HH:mm • dd/MM/yyyy")));
        }
        for (Component component : content.getComponents()) {
            if (component instanceof JComponent) ((JComponent) component).setAlignmentX(LEFT_ALIGNMENT);
        }
        add(content, BorderLayout.CENTER);
        JPanel total = new JPanel(new BorderLayout(12, 0));
        total.setOpaque(false);
        total.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.LINE), new EmptyBorder(16, 0, 0, 0)));
        total.add(label("pending_payment".equals(value(ticket, 5)) ? "Tổng dự kiến" : "Tổng tiền đơn", 13, Font.PLAIN, UiTheme.MUTED), BorderLayout.WEST);
        JLabel amount = label(value(ticket, 9).isEmpty() ? "—" : BasicClientUI.formatMoney(value(ticket, 9)), 24, Font.BOLD, UiTheme.PRIMARY);
        amount.setName("ticket-total");
        amount.setHorizontalAlignment(SwingConstants.RIGHT);
        total.add(amount, BorderLayout.CENTER);
        add(total, BorderLayout.SOUTH);
    }

    static boolean usable(String[] ticket) {
        return "active".equals(value(ticket, 5)) && value(ticket, 6).isEmpty();
    }

    static String state(String[] ticket) {
        switch (value(ticket, 5)) {
            case "active": return value(ticket, 6).isEmpty() ? "Hợp lệ • Chưa sử dụng" : "Đã sử dụng";
            case "pending_payment": return "Chờ thanh toán";
            case "expired": return "Đã hết hạn";
            case "cancelled": return "Đã hủy";
            default: return "Không còn hiệu lực";
        }
    }

    static Color stateColor(String[] ticket) {
        return usable(ticket) ? UiTheme.GREEN : "active".equals(value(ticket, 5)) ? UiTheme.PURPLE
                : "pending_payment".equals(value(ticket, 5)) ? UiTheme.GOLD : UiTheme.RED;
    }

    static String value(String[] ticket, int index) {
        return index < ticket.length && ticket[index] != null ? ticket[index] : "";
    }

    static String date(String value, String pattern) {
        try {
            return LocalDateTime.parse(value.replace(' ', 'T')).format(DateTimeFormatter.ofPattern(pattern));
        } catch (RuntimeException ignored) { return value; }
    }

    static JPanel field(String caption, String value) {
        JPanel panel = new JPanel(new BorderLayout(0, 7));
        panel.setOpaque(false);
        panel.add(label(caption, 11, Font.BOLD, UiTheme.MUTED), BorderLayout.NORTH);
        panel.add(text(value, 15, UiTheme.TEXT), BorderLayout.CENTER);
        return panel;
    }

    static JLabel label(String value, int size, int style, Color color) {
        JLabel label = new JLabel(value);
        label.setFont(UiTheme.font(size, style));
        label.setForeground(color);
        return label;
    }

    static JTextArea text(String value, int size, Color color) {
        JTextArea area = UiTheme.copy(value, size);
        area.setForeground(color);
        area.setBorder(null);
        ((DefaultCaret) area.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        return area;
    }
}
