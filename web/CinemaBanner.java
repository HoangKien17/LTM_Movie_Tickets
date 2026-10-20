package web;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Reusable movie hero using the actual poster, never a screenshot of a mockup.
 */
final class CinemaBanner extends JPanel {

    private static final BufferedImage CYBER_HERO = loadCyberHero();
    private BufferedImage poster;
    private final JTextArea title = new JTextArea();
    private final JLabel eyebrow = new JLabel("GALAXY CINEMA  /  KHÁM PHÁ");
    private final JLabel subtitle = new JLabel("Chọn phim. Chọn ghế. Tận hưởng.");
    private final JButton book = UiTheme.primary("Khám phá suất chiếu");
    private Runnable selected = () -> {
    };

    CinemaBanner() {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(34, 46, 34, 46));
        setPreferredSize(new Dimension(500, 330));
        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        eyebrow.setForeground(UiTheme.PRIMARY);
        eyebrow.setFont(UiTheme.font(11, Font.BOLD));
        words.add(eyebrow);
        words.add(Box.createVerticalStrut(12));
        title.setText("TRẢI NGHIỆM\nMÀN ẢNH RỘNG LỚN");
        title.setFont(UiTheme.font(32, Font.BOLD));
        title.setForeground(UiTheme.TEXT);
        title.setEditable(false);
        title.setFocusable(false);
        title.setOpaque(false);
        title.setLineWrap(true);
        title.setWrapStyleWord(true);
        title.setPreferredSize(new Dimension(540, 96));
        title.setMaximumSize(new Dimension(560, 96));
        words.add(title);
        words.add(Box.createVerticalStrut(6));
        subtitle.setForeground(UiTheme.MUTED);
        subtitle.setFont(UiTheme.font(12, Font.PLAIN));
        words.add(subtitle);
        words.add(Box.createVerticalStrut(16));
        book.setText("Đặt vé ngay  →");
        book.addActionListener(e -> selected.run());
        words.add(book);
        for (Component c : words.getComponents()) {
            if (c instanceof JComponent) {
                ((JComponent) c).setAlignmentX(LEFT_ALIGNMENT);
            }
        }
        add(words, BorderLayout.WEST);
    }

    void movie(String name, String meta, BufferedImage image, Runnable action) {
        title.setText("TRẢI NGHIỆM\nMÀN ẢNH RỘNG LỚN");
        subtitle.setText("Khám phá " + name + "  •  " + meta);
        poster = image;
        selected = action;
        book.setEnabled(action != null);
        repaint();
    }

    void caption(String text) {
        eyebrow.setText(text);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 24, 24));
        if (CYBER_HERO != null) {
            double scale = Math.max(getWidth() / (double) CYBER_HERO.getWidth(), getHeight() / (double) CYBER_HERO.getHeight());
            int width = (int) (CYBER_HERO.getWidth() * scale), height = (int) (CYBER_HERO.getHeight() * scale);
            g.drawImage(CYBER_HERO, (getWidth() - width) / 2, (getHeight() - height) / 2, width, height, null);
            g.setPaint(new GradientPaint(0, 0, new Color(8, 12, 28, 235), getWidth() * 3 / 5, 0, new Color(8, 12, 28, 30)));
            g.fillRect(0, 0, getWidth(), getHeight());
        } else {
            g.setPaint(new GradientPaint(0, 0, new Color(27, 23, 58), getWidth(), getHeight(), new Color(11, 48, 60)));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        if (poster != null && CYBER_HERO == null) {
            int x = getWidth() * 3 / 5;
            double scale = Math.max((getWidth() - x) / (double) poster.getWidth(),
                    getHeight() / (double) poster.getHeight());
            int w = (int) (poster.getWidth() * scale), h = (int) (poster.getHeight() * scale);
            g.drawImage(poster, x + (getWidth() - x - w) / 2, (getHeight() - h) / 2, w, h, null);
            g.setPaint(new GradientPaint(x - 30, 0, new Color(19, 29, 47), getWidth(), 0, new Color(12, 22, 40, 35)));
            g.fillRect(x - 30, 0, getWidth() - x + 30, getHeight());
        } else {
            g.setStroke(new BasicStroke(2));
            for (int i = 0; i < 6; i++) {
                g.setColor(new Color(92, 226, 239, 25 + i * 7));
                g.drawOval(getWidth() - 300 + i * 18, -65 + i * 18, 360, 360);
            }
            g.setColor(new Color(166, 117, 255, 35));
            g.fillOval(getWidth() - 170, 50, 190, 190);
        }
        g.setClip(null);
        g.setColor(UiTheme.LINE);
        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
        g.dispose();
        super.paintComponent(graphics);
    }

    private static BufferedImage loadCyberHero() {
        try {
            return ImageIO.read(new File("assets/galaxy-cyber-hero-v1.png"));
        } catch (Exception ignored) {
            return null;
        }
    }
}
