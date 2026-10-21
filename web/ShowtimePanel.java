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

/** Home sidebar; choices are actual server show IDs, never grid indices. */
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

    ShowtimePanel(IntConsumer choose) {
        super(UiTheme.SURFACE, 20);
        this.choose = choose;
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setPreferredSize(new Dimension(306, 0));
        setLayout(new BorderLayout(0, 20));
        JLabel heading = new JLabel("Chọn suất chiếu");
        heading.setFont(UiTheme.font(21, Font.BOLD));
        add(heading, BorderLayout.NORTH);
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JPanel film = new JPanel(new BorderLayout(12, 0));
        film.setOpaque(false);
        posterHolder.setOpaque(false);
        posterHolder.setPreferredSize(new Dimension(66, 94));
        film.add(posterHolder, BorderLayout.WEST);
        JPanel words = new JPanel(new GridLayout(2, 1, 0, 4));
        words.setOpaque(false);
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(17, Font.BOLD));
        words.add(title);
        words.add(meta);
        film.add(words, BorderLayout.CENTER);
        film.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        body.add(film);
        body.add(Box.createVerticalStrut(24));
        month.setFont(UiTheme.font(14, Font.BOLD));
        body.add(month);
        body.add(Box.createVerticalStrut(12));
        days.setOpaque(false);
        JScrollPane dates = UiTheme.scroll(days);
        dates.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        dates.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        dates.setPreferredSize(new Dimension(250, 86));
        dates.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));
        body.add(dates);
        body.add(Box.createVerticalStrut(16));
        slots.setOpaque(false);
        JPanel slotWrap = new JPanel(new BorderLayout());
        slotWrap.setOpaque(false);
        slotWrap.add(slots, BorderLayout.NORTH);
        JScrollPane times = UiTheme.scroll(slotWrap);
        times.setPreferredSize(new Dimension(250, 220));
        body.add(times);
        for (Component c : body.getComponents())
            if (c instanceof JComponent) ((JComponent) c).setAlignmentX(LEFT_ALIGNMENT);
        add(body, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 12));
        bottom.setOpaque(false);
        price.setFont(UiTheme.font(18, Font.BOLD));
        price.setForeground(UiTheme.PRIMARY);
        bottom.add(price, BorderLayout.NORTH);
        next.addActionListener(e -> { if (selectedShow > 0) choose.accept(selectedShow); });
        next.setEnabled(false);
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
        shows.clear();
        days.removeAll();
        slots.removeAll();
        posterHolder.removeAll();
        title.setText("Chọn một bộ phim");
        meta.setText("Ngày và giờ chiếu sẽ xuất hiện tại đây.");
        month.setText("Chọn phim để xem lịch chiếu");
        price.setText("—");
        next.setEnabled(false);
        revalidate();
        repaint();
    }

    void shows(List<String[]> rows) {
        shows.clear();
        for (String[] row : rows) if (row[1].equals(Integer.toString(movieId))) shows.add(row);
        shows.sort(java.util.Comparator.comparing(row -> row[4]));
        date = null;
        for (String[] row : shows) if (future(row)) { date = row[4].substring(0, 10); break; }
        if (date == null && !shows.isEmpty()) date = shows.get(0)[4].substring(0, 10);
        selectedShow = 0;
        render();
    }

    private static boolean future(String[] row) {
        try { return LocalDateTime.parse(row[4]).isAfter(LocalDateTime.now().plusMinutes(10)); }
        catch (RuntimeException ex) { return false; }
    }

    private void render() {
        days.removeAll();
        slots.removeAll();
        java.util.Set<String> dates = new java.util.LinkedHashSet<>();
        for (String[] row : shows) dates.add(row[4].substring(0, 10));
        for (String day : dates) {
            LocalDate value = LocalDate.parse(day);
            JButton button = day.equals(date) ? UiTheme.primary(value.format(DateTimeFormatter.ofPattern("dd/MM")))
                    : UiTheme.secondary(value.format(DateTimeFormatter.ofPattern("dd/MM")));
            button.setToolTipText(value.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, new Locale("vi", "VN")));
            button.setPreferredSize(new Dimension(68, 56));
            button.addActionListener(e -> { date = day; selectedShow = 0; render(); });
            days.add(button);
        }
        month.setText(date == null ? "Chưa có suất chiếu" : "Tháng " + date.substring(5, 7) + ", " + date.substring(0, 4));
        for (String[] row : shows) {
            if (!row[4].startsWith(date == null ? "?" : date)) continue;
            int id = Integer.parseInt(row[0]);
            JButton time = id == selectedShow ? UiTheme.primary(row[4].substring(11, 16)) : UiTheme.secondary(row[4].substring(11, 16));
            String room = row[3].replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            time.setText("<html><center>" + row[4].substring(11, 16) + "<br><small>" + room + "</small></center></html>");
            time.putClientProperty("showId", id);
            time.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
            time.setPreferredSize(new Dimension(116, 48));
            time.setToolTipText(row[3] + " • " + BasicClientUI.formatMoney(row[5]));
            time.getAccessibleContext().setAccessibleName(row[3] + ", " + row[4] + ", " + BasicClientUI.formatMoney(row[5]));
            time.setEnabled(future(row));
            time.addActionListener(e -> { selectedShow = id; render(); });
            slots.add(time);
        }
        if (slots.getComponentCount() == 0) slots.add(new JLabel("Chưa có suất chiếu."));
        price.setText("Chọn giờ chiếu");
        for (String[] row : shows) if (Integer.toString(selectedShow).equals(row[0]))
            price.setText(BasicClientUI.formatMoney(row[5]) + " / ghế • " + row[3]);
        next.setEnabled(selectedShow > 0);
        revalidate();
        repaint();
    }
}
