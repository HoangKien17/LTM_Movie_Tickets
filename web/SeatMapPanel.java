package web;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * Bố cục theo mã ghế thực: A1..A99, B1..B99. Lối đi ở giữa chỉ là khoảng hiển
 * thị.
 */
public final class SeatMapPanel extends JPanel {

    public SeatMapPanel() {
        super(new GridBagLayout());
        setBackground(UiTheme.SURFACE);
    }

    List<UiTheme.SeatButton> render(List<String[]> seats, Set<String> selected, Runnable changed) {
        removeAll();
        Map<Character, SortedMap<Integer, Boolean>> rows = new TreeMap<>();
        int columns = 0;
        for (String[] seat : seats) {
            String code = seat[0];
            if (!code.matches("[A-Z][1-9][0-9]?")) {
                continue;
            }
            int column = Integer.parseInt(code.substring(1));
            rows.computeIfAbsent(code.charAt(0), key -> new TreeMap<>()).put(column, "taken".equals(seat[1]));
            columns = Math.max(columns, column);
        }
        List<UiTheme.SeatButton> available = new ArrayList<>();
        if (rows.isEmpty()) {
            add(new JLabel("Suất chiếu không có ghế hoặc đã bị xóa"));
        } else {
            int aisleAfter = columns >= 4 ? columns / 2 : columns;
            int span = columns + 3 + (aisleAfter < columns ? 1 : 0);
            JPanel screen = new JPanel() {
                protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    java.awt.geom.QuadCurve2D curve = new java.awt.geom.QuadCurve2D.Double(
                            8, 22, getWidth() / 2.0, -8, getWidth() - 8, 22);
                    for (int stroke = 14; stroke >= 4; stroke -= 2) {
                        g.setStroke(new BasicStroke(stroke));
                        g.setColor(new Color(92, 226, 239, 12));
                        g.draw(curve);
                    }
                    g.setStroke(new BasicStroke(3));
                    g.setPaint(new GradientPaint(0, 0, UiTheme.PRIMARY, getWidth(), 0, UiTheme.PURPLE));
                    g.draw(curve);
                    g.setColor(UiTheme.MUTED);
                    g.setFont(UiTheme.font(11, Font.BOLD));
                    String text = "M À N  C H I Ế U";
                    g.drawString(text, (getWidth() - g.getFontMetrics().stringWidth(text)) / 2, 46);
                    g.dispose();
                }
            };
            screen.setOpaque(false);
            screen.setPreferredSize(new Dimension(100, 54));
            GridBagConstraints top = cell(0, 0);
            top.gridwidth = span;
            top.fill = GridBagConstraints.HORIZONTAL;
            top.insets = new Insets(5, 10, 14, 10);
            add(screen, top);
            int y = 1;
            for (Map.Entry<Character, SortedMap<Integer, Boolean>> row : rows.entrySet()) {
                JLabel name = new JLabel(String.valueOf(row.getKey()), SwingConstants.CENTER);
                name.setFont(UiTheme.font(14, Font.BOLD));
                name.setPreferredSize(new Dimension(28, 42));
                add(name, cell(0, y));
                for (int col = 1; col <= columns; col++) {
                    int x = col + (col > aisleAfter ? 1 : 0);
                    Boolean taken = row.getValue().get(col);
                    if (taken == null) {
                        add(Box.createRigidArea(new Dimension(46, 38)), cell(x, y));
                    } else {
                        String code = row.getKey().toString() + col;
                        UiTheme.SeatButton button = new UiTheme.SeatButton(code, taken);
                        button.setPreferredSize(new Dimension(46, 38));
                        button.setToolTipText(code + (taken ? " — đã đặt hoặc đang giữ" : " — ghế trống"));
                        button.setSelected(!taken && selected.contains(code));
                        button.addActionListener(event -> changed.run());
                        add(button, cell(x, y));
                        if (!taken) {
                            available.add(button);
                        }
                    }
                }
                if (aisleAfter < columns) {
                    add(Box.createRigidArea(new Dimension(26, 42)), cell(aisleAfter + 1, y));
                }
                y++;
            }
        }
        revalidate();
        repaint();
        return available;
    }

    private static GridBagConstraints cell(int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.CENTER;
        return c;
    }
}
