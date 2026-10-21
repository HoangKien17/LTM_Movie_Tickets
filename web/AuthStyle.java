package web;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.*;
import java.util.Collections;
import javax.swing.*;

/** Auth-only design tokens. Other pages keep their own UiTheme sizing. */
final class AuthStyle {
    static final Color TEXT = new Color(241, 245, 252);
    static final Color MUTED = new Color(165, 178, 199);
    static final Color CYAN = new Color(33, 225, 244);
    static final Color PURPLE = new Color(174, 108, 255);
    static final Color LINE = new Color(74, 89, 114);
    private static final String FAMILY = family();

    private AuthStyle() { }

    private static String family() {
        for (String wanted : new String[]{"Segoe UI", "Arial", "SansSerif"}) {
            Font candidate = new Font(wanted, Font.PLAIN, 18);
            if (candidate.canDisplayUpTo("Mỗi suất chiếu, một trải nghiệm mới. Đăng ký") == -1) return wanted;
        }
        return Font.SANS_SERIF;
    }

    static Font font(float size, boolean bold) {
        return new Font(FAMILY, bold ? Font.BOLD : Font.PLAIN, 1).deriveFont(size);
    }

    static Font wordmark(float size) {
        return font(size, false).deriveFont(Collections.singletonMap(TextAttribute.TRACKING, .12f));
    }

    static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    static Icon icon(String kind, int size) {
        return new Icon() {
            public int getIconWidth() { return size; }
            public int getIconHeight() { return size; }
            public void paintIcon(Component c, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                quality(g);
                g.translate(x, y);
                g.scale(size / 24.0, size / 24.0);
                g.setColor(c.getForeground());
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                switch (kind) {
                    case "user":
                        g.draw(new Ellipse2D.Double(8, 3, 8, 8));
                        g.draw(new Arc2D.Double(4, 13, 16, 10, 0, 180, Arc2D.OPEN));
                        g.drawLine(4, 18, 20, 18);
                        break;
                    case "lock":
                        g.draw(new Arc2D.Double(7, 2, 10, 15, 0, 180, Arc2D.OPEN));
                        g.draw(new RoundRectangle2D.Double(4, 10, 16, 12, 2, 2));
                        g.drawLine(12, 14, 12, 17);
                        break;
                    case "mail":
                        g.draw(new RoundRectangle2D.Double(3, 5, 18, 14, 3, 3));
                        g.drawLine(4, 6, 12, 13);
                        g.drawLine(12, 13, 20, 6);
                        break;
                    case "phone":
                        g.draw(new RoundRectangle2D.Double(7, 2, 10, 20, 3, 3));
                        g.drawLine(10, 18, 14, 18);
                        break;
                    case "eye":
                    case "eye-off":
                        Path2D eye = new Path2D.Double();
                        eye.moveTo(2, 12); eye.quadTo(12, 0, 22, 12); eye.quadTo(12, 24, 2, 12);
                        g.draw(eye);
                        g.draw(new Ellipse2D.Double(9, 9, 6, 6));
                        if ("eye-off".equals(kind)) g.drawLine(3, 3, 21, 21);
                        break;
                    case "arrow":
                        g.drawLine(3, 12, 21, 12);
                        g.drawLine(14, 5, 21, 12);
                        g.drawLine(14, 19, 21, 12);
                        break;
                    default: break;
                }
                g.dispose();
            }
        };
    }

    static void orbit(Graphics2D graphics, double x, double y, double size) {
        Graphics2D g = (Graphics2D) graphics.create();
        quality(g);
        g.translate(x, y);
        g.scale(size / 76, size / 76);
        g.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(new GradientPaint(0, 55, CYAN, 65, 6, new Color(143, 90, 255)));
        g.rotate(-.34, 38, 38);
        g.draw(new Ellipse2D.Double(18, 11, 40, 54));
        g.rotate(-.22, 38, 38);
        g.draw(new Ellipse2D.Double(2, 27, 72, 25));
        g.dispose();
    }

    static final class CheckIcon implements Icon {
        private final int size;
        CheckIcon(int size) { this.size = size; }
        public int getIconWidth() { return size; }
        public int getIconHeight() { return size; }
        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            g.translate(x, y);
            boolean selected = c instanceof AbstractButton && ((AbstractButton) c).isSelected();
            g.setPaint(selected ? new GradientPaint(0, 0, CYAN, size, size, new Color(21, 195, 225)) : new Color(16, 26, 43));
            g.fillRoundRect(0, 0, size - 1, size - 1, 5, 5);
            g.setColor(selected ? CYAN : LINE);
            g.drawRoundRect(0, 0, size - 1, size - 1, 5, 5);
            if (selected) {
                g.setColor(new Color(4, 26, 40));
                g.setStroke(new BasicStroke(Math.max(1.5f, size / 11f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(size / 4, size / 2, size * 5 / 12, size * 2 / 3);
                g.drawLine(size * 5 / 12, size * 2 / 3, size * 3 / 4, size / 3);
            }
            g.dispose();
        }
    }
}
