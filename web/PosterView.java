package web;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/**
 * Cover-fit poster shared by the showtime sidebar and the ticket.
 */
final class PosterView extends JPanel {

    private final BufferedImage image;
    private final String title;

    PosterView(BufferedImage image, String title) {
        this.image = image;
        this.title = title;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        int w = getWidth(), h = getHeight();
        Shape originalClip = g.getClip();
        g.clip(new java.awt.geom.RoundRectangle2D.Double(0, 0, w, h, 12, 12));
        g.setPaint(new GradientPaint(0, 0, UiTheme.NAVY_LIGHT, w, h, new Color(71, 39, 112)));
        g.fillRect(0, 0, w, h);
        if (image != null) {
            double scale = Math.max(w / (double) image.getWidth(), h / (double) image.getHeight());
            int iw = (int) (image.getWidth() * scale), ih = (int) (image.getHeight() * scale);
            g.drawImage(image, (w - iw) / 2, (h - ih) / 2, iw, ih, null);
        } else {
            g.setColor(UiTheme.PRIMARY);
            g.setFont(UiTheme.font(Math.min(32, Math.max(12, w / 4)), Font.BOLD));
            String first = title.isEmpty() ? "G" : title.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
            g.drawString(first, (w - g.getFontMetrics().stringWidth(first)) / 2, h / 2 + 12);
        }
        g.setClip(originalClip);
        boolean selected = Boolean.TRUE.equals(getClientProperty("selected"));
        g.setColor(selected ? UiTheme.PRIMARY : UiTheme.LINE);
        g.setStroke(new BasicStroke(selected ? 2f : 1f));
        g.drawRoundRect(1, 1, w - 3, h - 3, 12, 12);
        g.dispose();
    }
}
