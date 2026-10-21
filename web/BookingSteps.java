package web;

import java.awt.*;
import javax.swing.JPanel;

/**
 * Thanh tiến trình 4 bước, dùng chung cho các màn đặt vé.
 */
final class BookingSteps extends JPanel {

    private final int current;
    private static final String[] NAMES = {"Chọn phim", "Suất chiếu", "Chọn ghế", "Thanh toán"};

    BookingSteps(int current) {
        this.current = current;
        setOpaque(false);
        setPreferredSize(new Dimension(600, 54));
        getAccessibleContext().setAccessibleName("Bước " + (current + 1) + ": " + NAMES[current]);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int cell = getWidth() / 4, y = getHeight() / 2;
        for (int i = 0; i < 4; i++) {
            int x = i * cell + 10;
            boolean active = i == current, passed = i < current;
            Color color = active ? UiTheme.PRIMARY : passed ? UiTheme.PURPLE : UiTheme.MUTED;
            if (active) {
                g.setColor(new Color(92, 226, 239, 22));
                g.fillOval(x - 5, y - 21, 42, 42);
            }
            g.setColor(color);
            g.setStroke(new BasicStroke(active ? 2 : 1));
            g.drawOval(x, y - 16, 32, 32);
            g.setFont(UiTheme.font(12, Font.BOLD));
            if (passed) {
                g.setStroke(new BasicStroke(2f));
                g.drawLine(x + 9, y, x + 14, y + 5);
                g.drawLine(x + 14, y + 5, x + 23, y - 5);
            } else {
                String number = String.format("%02d", i + 1);
                g.drawString(number, x + (32 - g.getFontMetrics().stringWidth(number)) / 2, y + 5);
            }
            g.setFont(UiTheme.font(12, active ? Font.BOLD : Font.PLAIN));
            g.drawString(NAMES[i], x + 43, y + 5);
            int start = x + 53 + g.getFontMetrics().stringWidth(NAMES[i]);
            if (i < 3 && start < (i + 1) * cell - 8) {
                g.setColor(i < current ? UiTheme.PRIMARY : UiTheme.LINE);
                g.drawLine(start, y, (i + 1) * cell - 8, y);
            }
        }
        g.dispose();
    }
}
