package web;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import javax.swing.*;

/**
 * Home sidebar; choices are actual server show IDs, never grid indices.
 */
final class ShowtimePanel extends UiTheme.RoundedPanel {

    private final IntConsumer choose;
    private final JPanel days = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final JPanel slots = new JPanel(new GridLayout(0, 2, 8, 8));
    private final JLabel month = new JLabel("Chọn phim để xem lịch chiếu");
    private final JLabel price = new JLabel("—");
    private final JTextArea title = UiTheme.copy("Chọn một bộ phim", 17);
    private final JTextArea meta = UiTheme.copy("Ngày và giờ chiếu sẽ xuất hiện tại đây.", 12);
    private final JPanel posterHolder = new JPanel(new BorderLayout());
    private final JButton next = UiTheme.primary("Chọn ghế  →");
    private final List<String[]> shows = new ArrayList<>();
    private int movieId, selectedShow;
    private String date;
    private int datePage;
    private final JLabel roomInfo = new JLabel("Chọn ngày và giờ phù hợp");
    private final JButton previousDay = new JButton(HomeIcons.icon("chevron-left", 18)), nextDay = new JButton(HomeIcons.icon("chevron-right", 18));
    private JScrollPane timeViewport;

    ShowtimePanel(IntConsumer choose) {
        super(UiTheme.NAVY, 20);
        this.choose = choose;
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setPreferredSize(new Dimension(306, 0));
        setLayout(new BorderLayout(0, 20));
        JLabel heading = new JLabel("Chọn suất chiếu");
        heading.setFont(UiTheme.font(26, Font.BOLD));
        add(heading, BorderLayout.NORTH);
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JPanel film = new JPanel(new BorderLayout(12, 0));
        film.setOpaque(false);
        posterHolder.setOpaque(false);
        posterHolder.setPreferredSize(new Dimension(94, 130));
        film.add(posterHolder, BorderLayout.WEST);
        JPanel words = new JPanel(new GridLayout(2, 1, 0, 4));
        words.setOpaque(false);
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(23, Font.BOLD));
        meta.setFont(UiTheme.font(14, Font.PLAIN));
        words.add(title);
        words.add(meta);
        film.add(words, BorderLayout.CENTER);
        film.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));
        body.add(film);
        body.add(Box.createVerticalStrut(24));
        month.setFont(UiTheme.font(18, Font.BOLD));
        JPanel monthRow = new JPanel(new BorderLayout());
        monthRow.setOpaque(false);
        monthRow.add(month, BorderLayout.CENTER);
        JPanel arrows = new JPanel(new GridLayout(1, 2));
        arrows.setOpaque(false);
        previousDay.setPreferredSize(new Dimension(28, 30));
        nextDay.setPreferredSize(new Dimension(28, 30));
        for (JButton arrow : new JButton[]{previousDay, nextDay}) {
            arrow.setBorder(BorderFactory.createEmptyBorder());
            arrow.setContentAreaFilled(false);
            arrow.setForeground(UiTheme.MUTED);
        }
        previousDay.setToolTipText("Các ngày trước");
        nextDay.setToolTipText("Các ngày tiếp theo");
        previousDay.addActionListener(e -> {
            datePage = Math.max(0, datePage - 4);
            render();
        });
        nextDay.addActionListener(e -> {
            datePage += 4;
            render();
        });
        arrows.add(previousDay);
        arrows.add(nextDay);
        monthRow.add(arrows, BorderLayout.EAST);
        monthRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        body.add(monthRow);
        body.add(Box.createVerticalStrut(12));
        days.setOpaque(false);
        JScrollPane dates = UiTheme.scroll(days);
        dates.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        dates.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        dates.setOpaque(false);
        dates.getViewport().setOpaque(false);
        dates.setPreferredSize(new Dimension(250, 86));
        dates.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));
        body.add(dates);
        body.add(Box.createVerticalStrut(16));
        roomInfo.setFont(UiTheme.font(14, Font.BOLD));
        roomInfo.setForeground(UiTheme.MUTED);
        body.add(roomInfo);
        body.add(Box.createVerticalStrut(12));
        slots.setOpaque(false);
        JPanel slotWrap = new JPanel(new BorderLayout());
        slotWrap.setOpaque(false);
        slotWrap.add(slots, BorderLayout.NORTH);
        JScrollPane times = UiTheme.scroll(slotWrap);
        timeViewport = times;
        times.setOpaque(false);
        times.getViewport().setOpaque(false);
        times.setPreferredSize(new Dimension(250, 220));
        body.add(times);
        for (Component c : body.getComponents()) {
            if (c instanceof JComponent) {
                ((JComponent) c).setAlignmentX(LEFT_ALIGNMENT);
            }
        }
        JScrollPane bodyScroll = UiTheme.scrollContent(body);
        bodyScroll.setOpaque(false);
        bodyScroll.getViewport().setOpaque(false);
        add(bodyScroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 12));
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.LINE), BorderFactory.createEmptyBorder(14, 0, 0, 0)));
        price.setFont(UiTheme.font(24, Font.BOLD));
        price.setForeground(UiTheme.TEXT);
        bottom.add(price, BorderLayout.NORTH);
        next.addActionListener(e -> {
            if (selectedShow > 0) {
                choose.accept(selectedShow);
        
            }});
        next.setEnabled(false);
        next.setText("Chọn ghế");
        next.setIcon(HomeIcons.icon("arrow", 25));
        next.setHorizontalTextPosition(SwingConstants.LEFT);
        next.setIconTextGap(22);
        next.setFont(UiTheme.font(21, Font.BOLD));
        next.setPreferredSize(new Dimension(250, 60));
        bottom.add(next, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
    }

    void movie(int id, String name, String details, BufferedImage poster) {
        clear();
        movieId = id;
        title.setText(name);
        meta.setText(details);
        posterHolder.add(new PosterView(poster, name), BorderLayout.CENTER);
        month.setText("Đang tải lịch chiếu...");
        revalidate();
        repaint();
    }

    void clear() {
        movieId = selectedShow = 0;
        date = null;
        datePage = 0;
        shows.clear();
        days.removeAll();
        slots.removeAll();
        posterHolder.removeAll();
        title.setText("Chọn một bộ phim");
        meta.setText("Ngày và giờ chiếu sẽ xuất hiện tại đây.");
        month.setText("Chọn phim để xem lịch chiếu");
        roomInfo.setText("Chọn ngày và giờ phù hợp");
        price.setText("—");
        next.setEnabled(false);
        revalidate();
        repaint();
    }

    void shows(List<String[]> rows) {
        shows.clear();
        for (String[] row : rows) {
            if (row[1].equals(Integer.toString(movieId))) {
                shows.add(row);
            }
        }
        shows.sort(java.util.Comparator.comparing(row -> row[4]));
        date = null;
        for (String[] row : shows) {
            if (future(row)) {
                date = row[4].substring(0, 10);
                break;
            }
        }
        if (date == null && !shows.isEmpty()) {
            date = shows.get(0)[4].substring(0, 10);
        }
        java.util.List<String> available = new ArrayList<>();
        for (String[] row : shows) {
            if (!available.contains(row[4].substring(0, 10))) {
                available.add(row[4].substring(0, 10));
            }
        }
        datePage = Math.max(0, available.indexOf(date) / 4 * 4);
        selectedShow = 0;
        render();
    }

    private static boolean future(String[] row) {
        try {
            return LocalDateTime.parse(row[4]).isAfter(LocalDateTime.now().plusMinutes(10));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private void render() {
        days.removeAll();
        slots.removeAll();
        java.util.Set<String> dates = new java.util.LinkedHashSet<>();
        for (String[] row : shows) {
            dates.add(row[4].substring(0, 10));
        }
        java.util.List<String> pageDates = new ArrayList<>(dates);
        datePage = Math.max(0, Math.min(datePage, Math.max(0, (pageDates.size() - 1) / 4 * 4)));
        previousDay.setEnabled(datePage > 0);
        nextDay.setEnabled(datePage + 4 < pageDates.size());
        for (String day : pageDates.subList(datePage, Math.min(datePage + 4, pageDates.size()))) {
            LocalDate value = LocalDate.parse(day);
            JButton button = new DateTile(value, day.equals(date));
            button.setToolTipText(value.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, new Locale("vi", "VN")));
            button.setPreferredSize(new Dimension(Math.max(53, (getWidth() - 60) / 4 - 6), 72));
            button.addActionListener(e -> {
                date = day;
                selectedShow = 0;
                render();
            });
            days.add(button);
        }
        month.setText(date == null ? "Chưa có suất chiếu" : "Tháng " + date.substring(5, 7) + ", " + date.substring(0, 4));
        for (String[] row : shows) {
            if (!row[4].startsWith(date == null ? "?" : date)) {
                continue;
            }
            int id = Integer.parseInt(row[0]);
            JButton time = new TimeSlotButton(id == selectedShow);
            String room = row[3].replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            time.setText("<html><center>" + row[4].substring(11, 16) + "<br><small>" + room + "</small></center></html>");
            time.putClientProperty("showId", id);
            time.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
            time.setPreferredSize(new Dimension(116, 48));
            time.setFont(UiTheme.font(16, Font.BOLD));
            time.setToolTipText(row[3] + " • " + BasicClientUI.formatMoney(row[5]));
            time.getAccessibleContext().setAccessibleName(row[3] + ", " + row[4] + ", " + BasicClientUI.formatMoney(row[5]));
            time.setEnabled(future(row));
            time.addActionListener(e -> {
                selectedShow = id;
                render();
            });
            slots.add(time);
        }
        if (slots.getComponentCount() == 0) {
            slots.add(new JLabel("Chưa có suất chiếu."));
        }
        int timeHeight = Math.max(80, Math.min(172, (slots.getComponentCount() + 1) / 2 * 56));
        timeViewport.setPreferredSize(new Dimension(250, timeHeight));
        timeViewport.setMaximumSize(new Dimension(Integer.MAX_VALUE, timeHeight));
        java.math.BigDecimal minimum = null;
        for (String[] row : shows) {
            if (future(row) && row[4].startsWith(date == null ? "?" : date)) {
                java.math.BigDecimal amount = new java.math.BigDecimal(row[5]);
                if (minimum == null || amount.compareTo(minimum) < 0) {
                    minimum = amount;
                }
            }
        }
        price.setText(minimum == null ? "Chưa có suất phù hợp" : "Từ " + BasicClientUI.formatMoney(minimum.toPlainString()));
        roomInfo.setText("Chọn giờ chiếu • Giá theo từng phòng");
        for (String[] row : shows) {
            if (Integer.toString(selectedShow).equals(row[0])) {
                price.setText(BasicClientUI.formatMoney(row[5]) + " / ghế");
                roomInfo.setText(row[3]);
            }
        }
        next.setEnabled(selectedShow > 0);
        revalidate();
        repaint();
    }

    @Override
    public void doLayout() {
        super.doLayout();
        if (days.getComponentCount() > 0) {
            int width = Math.max(53, (getWidth() - 60) / 4 - 6);
            for (Component c : days.getComponents()) {
                c.setPreferredSize(new Dimension(width, 72));
        
            }}
    }

    private static final class DateTile extends JButton {

        private final LocalDate day;
        private final boolean selected;

        DateTile(LocalDate day, boolean selected) {
            this.day = day;
            this.selected = selected;
            setBorderPainted(false);
            setContentAreaFilled(false);
            setToolTipText(day.toString());
        }

        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            AuthStyle.quality(g);
            int w = getWidth(), h = getHeight();
            g.setColor(selected ? new Color(15, 56, 70) : new Color(13, 22, 36));
            g.fillRoundRect(1, 1, w - 3, h - 3, 16, 16);
            g.setColor(selected ? UiTheme.PRIMARY : UiTheme.LINE);
            g.setStroke(new BasicStroke(selected ? 1.8f : 1));
            g.drawRoundRect(1, 1, w - 3, h - 3, 16, 16);
            String weekday = day.getDayOfWeek() == DayOfWeek.SUNDAY ? "CN" : "T" + (day.getDayOfWeek().getValue() + 1);
            g.setColor(selected ? UiTheme.PRIMARY : UiTheme.MUTED);
            g.setFont(UiTheme.font(13, Font.PLAIN));
            g.drawString(weekday, (w - g.getFontMetrics().stringWidth(weekday)) / 2, 26);
            String number = Integer.toString(day.getDayOfMonth());
            g.setColor(selected ? UiTheme.PRIMARY : UiTheme.TEXT);
            g.setFont(UiTheme.font(22, Font.BOLD));
            g.drawString(number, (w - g.getFontMetrics().stringWidth(number)) / 2, 54);
            g.dispose();
        }
    }

    private static final class TimeSlotButton extends JButton {

        private final boolean selected;

        TimeSlotButton(boolean selected) {
            this.selected = selected;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setForeground(selected ? UiTheme.BACKGROUND : UiTheme.TEXT);
        }

        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            AuthStyle.quality(g);
            g.setColor(!isEnabled() ? UiTheme.NAVY_LIGHT : selected ? UiTheme.PRIMARY : new Color(16, 25, 42));
            g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 14, 14);
            g.setColor(selected ? UiTheme.PRIMARY : UiTheme.LINE);
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
