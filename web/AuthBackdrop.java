package web;

import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/** Decorative artwork only; all forms remain real, focusable Swing controls. */
final class AuthBackdrop extends JPanel {
    private static final BufferedImage BACKDROP = load(new File("assets/galaxy-cyber-hero-v1.png"));
    private static final List<BufferedImage> POSTERS = posters();

    AuthBackdrop() { setBackground(UiTheme.BACKGROUND); }

    private static BufferedImage load(File file) {
        try { return ImageIO.read(file); } catch (Exception ignored) { return null; }
    }

    private static List<BufferedImage> posters() {
        List<BufferedImage> images = new ArrayList<>();
        File[] files = {new File("img/conan.jpeg"), new File("img/doraemon.jpeg"), new File("img/dragonball.jpeg")};
        if (files != null) {
            Arrays.sort(files, java.util.Comparator.comparing(File::getName));
            for (File file : files) {
                BufferedImage image = load(file);
                if (image != null) images.add(image);
                if (images.size() == 5) break;
            }
        }
        return images;
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int w = getWidth(), h = getHeight();
        if (BACKDROP != null) {
            double scale = Math.max(w / (double) BACKDROP.getWidth(), h / (double) BACKDROP.getHeight());
            int iw = (int) (BACKDROP.getWidth() * scale), ih = (int) (BACKDROP.getHeight() * scale);
            g.drawImage(BACKDROP, (w - iw) / 2, (h - ih) / 2, iw, ih, null);
        }
        g.setColor(new Color(5, 10, 23, 175));
        g.fillRect(0, 0, w, h);
        g.setPaint(new RadialGradientPaint(new Point(0, h / 3), Math.max(1, w * .7f),
                new float[]{0, 1}, new Color[]{new Color(36, 214, 239, 55), new Color(0, 0, 0, 0)}));
        g.fillRect(0, 0, w, h);
        g.setPaint(new RadialGradientPaint(new Point(w, h / 2), Math.max(1, w * .6f),
                new float[]{0, 1}, new Color[]{new Color(153, 70, 255, 65), new Color(0, 0, 0, 0)}));
        g.fillRect(0, 0, w, h);
        // A poster ribbon below the headline, loaded from the project's existing assets.
        int pw = Math.max(90, w / 9), ph = pw * 3 / 2;
        for (int i = 0; i < POSTERS.size(); i++) {
            Graphics2D tile = (Graphics2D) g.create();
            int x = -15 + i * (pw - 8), y = h - ph - 65 + (i % 2) * 24;
            tile.rotate((i - 2) * .045, x + pw / 2.0, y + ph / 2.0);
            tile.setComposite(AlphaComposite.SrcOver.derive(.65f));
            tile.clip(new java.awt.geom.RoundRectangle2D.Double(x, y, pw, ph, 12, 12));
            tile.drawImage(POSTERS.get(i), x, y, pw, ph, null);
            tile.dispose();
        }
        g.setPaint(new GradientPaint(0, h - 90, new Color(9, 13, 25, 0), 0, h, UiTheme.BACKGROUND));
        g.fillRect(0, h - 90, w, 90);
        g.dispose();
    }
}
