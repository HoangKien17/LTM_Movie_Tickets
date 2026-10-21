package web;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * Decorative artwork only; all forms remain real, focusable Swing controls.
 */
class AuthBackdrop extends JPanel {

    static final double DESIGN_WIDTH = 1672, DESIGN_HEIGHT = 904;
    private static final BufferedImage BACKDROP = artwork();

    AuthBackdrop() {
        setBackground(UiTheme.BACKGROUND);
    }

    private static BufferedImage load(File file) {
        try {
            return ImageIO.read(file);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static BufferedImage artwork() {
        BufferedImage image = load(new File("assets/galaxy-auth-cinematic-v2.png"));
        return image == null ? load(new File("assets/galaxy-cyber-hero-v1.png")) : image;
    }

    final double designScale() {
        return Math.max(.1, Math.min(getWidth() / DESIGN_WIDTH, getHeight() / DESIGN_HEIGHT));
    }

    final double originX() {
        return (getWidth() - DESIGN_WIDTH * designScale()) / 2;
    }

    final double originY() {
        return (getHeight() - DESIGN_HEIGHT * designScale()) / 2;
    }

    final Graphics2D designGraphics(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        AuthStyle.quality(g);
        g.translate(originX(), originY());
        g.scale(designScale(), designScale());
        return g;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        AuthStyle.quality(g);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int w = getWidth(), h = getHeight();
        if (BACKDROP != null) {
            double scale = Math.max(w / (double) BACKDROP.getWidth(), h / (double) BACKDROP.getHeight());
            int iw = (int) (BACKDROP.getWidth() * scale), ih = (int) (BACKDROP.getHeight() * scale);
            g.drawImage(BACKDROP, (w - iw) / 2, (h - ih) / 2, iw, ih, null);
        }
        if (BACKDROP == null) {
            g.setPaint(new GradientPaint(0, 0, new Color(6, 30, 45), w, h, new Color(27, 12, 51)));
            g.fillRect(0, 0, w, h);
        }
        g.dispose();
    }
}
