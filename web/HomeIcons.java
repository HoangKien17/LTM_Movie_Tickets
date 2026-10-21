package web;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

/**
 * Font-independent header icons.
 */
final class HomeIcons {

    private HomeIcons() {
    }

    static Icon icon(String type, int size) {
        return new Icon() {
            public int getIconWidth() {
                return size;
            }

            public int getIconHeight() {
                return size;
            }

            public void paintIcon(Component c, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                AuthStyle.quality(g);
                g.translate(x, y);
                g.scale(size / 24.0, size / 24.0);
                g.setColor(c.getForeground());
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                switch (type) {
                    case "home":
                        Path2D house = new Path2D.Double();
                        house.moveTo(3, 11);
                        house.lineTo(12, 3);
                        house.lineTo(21, 11);
                        g.draw(house);
                        g.drawRect(6, 10, 12, 11);
                        g.drawRect(10, 15, 4, 6);
                        break;
                    case "calendar":
                        g.drawRoundRect(3, 5, 18, 16, 3, 3);
                        g.drawLine(3, 10, 21, 10);
                        g.drawLine(8, 2, 8, 7);
                        g.drawLine(16, 2, 16, 7);
                        g.drawRect(7, 14, 3, 3);
                        break;
                    case "ticket":
                        g.drawRoundRect(2, 5, 20, 14, 3, 3);
                        g.drawLine(8, 5, 8, 8);
                        g.drawLine(8, 11, 8, 13);
                        g.drawLine(8, 16, 8, 19);
                        break;
                    case "search":
                        g.drawOval(3, 3, 13, 13);
                        g.drawLine(14, 14, 21, 21);
                        break;
                    case "bell":
                        Path2D bell = new Path2D.Double();
                        bell.moveTo(5, 17);
                        bell.curveTo(7, 15, 7, 14, 7, 9);
                        bell.curveTo(7, 2, 17, 2, 17, 9);
                        bell.curveTo(17, 14, 17, 15, 19, 17);
                        bell.closePath();
                        g.draw(bell);
                        g.drawArc(10, 18, 4, 3, 180, 180);
                        break;
                    case "monitor":
                        g.drawRoundRect(2, 3, 20, 14, 2, 2);
                        g.drawLine(8, 21, 16, 21);
                        g.drawLine(12, 17, 12, 21);
                        g.drawLine(5, 11, 9, 8);
                        g.drawLine(9, 8, 13, 12);
                        g.drawLine(13, 12, 18, 7);
                        break;
                    case "check":
                        g.drawOval(2, 2, 20, 20);
                        g.drawLine(6, 12, 10, 16);
                        g.drawLine(10, 16, 18, 7);
                        break;
                    case "arrow":
                        g.drawLine(3, 12, 21, 12);
                        g.drawLine(15, 6, 21, 12);
                        g.drawLine(15, 18, 21, 12);
                        break;
                    case "user":
                        g.drawOval(8, 3, 8, 8);
                        g.drawArc(4, 13, 16, 12, 0, 180);
                        g.drawLine(4, 19, 20, 19);
                        break;
                    case "chevron":
                        g.drawLine(6, 9, 12, 15);
                        g.drawLine(12, 15, 18, 9);
                        break;
                    case "chevron-left":
                        g.drawLine(15, 5, 8, 12);
                        g.drawLine(8, 12, 15, 19);
                        break;
                    case "chevron-right":
                        g.drawLine(9, 5, 16, 12);
                        g.drawLine(16, 12, 9, 19);
                        break;
                    case "manage":
                        g.drawRoundRect(3, 3, 7, 7, 1, 1);
                        g.drawRoundRect(14, 3, 7, 7, 1, 1);
                        g.drawRoundRect(3, 14, 7, 7, 1, 1);
                        g.drawRoundRect(14, 14, 7, 7, 1, 1);
                        break;
                    case "logout":
                        g.drawLine(10, 3, 4, 3);
                        g.drawLine(4, 3, 4, 21);
                        g.drawLine(4, 21, 10, 21);
                        g.drawLine(9, 12, 21, 12);
                        g.drawLine(16, 7, 21, 12);
                        g.drawLine(16, 17, 21, 12);
                        break;
                    case "close":
                        g.drawLine(6, 6, 18, 18);
                        g.drawLine(6, 18, 18, 6);
                        break;
                    default:
                        break;
                }
                g.dispose();
            }
        };
    }
}
