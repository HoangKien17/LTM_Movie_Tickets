package web;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Thanh tiến trình 4 bước, dùng chung cho các màn đặt vé.
 */
final class BookingSteps extends JPanel {

    BookingSteps(int current) {
        super(new GridLayout(1, 4, 0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(2, 12, 2, 12));
        String[] names = {"Chọn phim", "Suất chiếu", "Ghế & combo", "Vé & QR"};
        for (int i = 0; i < names.length; i++) {
            add(step(i + 1, names[i], i <= current, i == current));
        }
    }

    private JPanel step(int number, String name, boolean passed, boolean active) {
        JPanel box = new JPanel(new GridLayout(2, 1, 0, 3));
        box.setOpaque(false);
        JLabel circle = new StepCircle(String.valueOf(number), passed, active);
        circle.setHorizontalAlignment(JLabel.CENTER);
        JLabel text = new JLabel(number + ". " + name, JLabel.CENTER);
        text.setFont(UiTheme.font(11, active ? Font.BOLD : Font.PLAIN));
        text.setForeground(passed ? UiTheme.PRIMARY : UiTheme.MUTED);
        box.add(circle);
        box.add(text);
        return box;
    }

    private static final class StepCircle extends JLabel {

        private final boolean passed, active;

        StepCircle(String text, boolean passed, boolean active) {
            super(text);
            this.passed = passed;
            this.active = active;
            setFont(UiTheme.font(12, Font.BOLD));
            setForeground(passed ? UiTheme.NAVY : UiTheme.MUTED);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(30, Math.min(getWidth(), getHeight()));
            int x = (getWidth() - size) / 2, y = (getHeight() - size) / 2;
            g.setColor(passed ? UiTheme.PRIMARY : UiTheme.NAVY_LIGHT);
            g.fillOval(x, y, size, size);
            g.setColor(active ? UiTheme.WHITE : (passed ? UiTheme.PRIMARY : UiTheme.LINE));
            g.setStroke(new BasicStroke(active ? 2f : 1f));
            g.drawOval(x, y, size - 1, size - 1);
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
