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
        setBorder(new EmptyBorder(24, 28, 24, 28));
        setPreferredSize(new Dimension(500, 200));
        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        eyebrow.setForeground(UiTheme.PRIMARY);
        eyebrow.setFont(UiTheme.font(11, Font.BOLD));
        words.add(eyebrow);
        words.add(Box.createVerticalStrut(12));
        title.setText("TRẢI NGHIỆM\nMÀN ẢNH RỘNG LỚN");
        title.setFont(UiTheme.font(28, Font.BOLD));
        title.setForeground(UiTheme.TEXT);
        title.setEditable(false);
        title.setFocusable(false);
        title.setOpaque(false);
        title.setLineWrap(true);
        title.setWrapStyleWord(true);
        title.setPreferredSize(new Dimension(390, 78));
        title.setMaximumSize(new Dimension(440, 78));
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
        title.setText(name);
        subtitle.setText(meta);
        subtitle.setToolTipText(meta);
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
        Shape originalClip = g.getClip();
        g.clip(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 24, 24));
        BufferedImage background = poster == null ? CYBER_HERO : poster;
        if (background != null) {
            double scale = Math.max(getWidth() / (double) background.getWidth(), getHeight() / (double) background.getHeight());
            int w = (int) (background.getWidth() * scale), h = (int) (background.getHeight() * scale);
            g.drawImage(background, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
        } else {
            g.setPaint(new GradientPaint(0, 0, UiTheme.NAVY, getWidth(), getHeight(), new Color(35, 30, 70)));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        g.setPaint(new GradientPaint(0, 0, new Color(8, 12, 28, 250), getWidth(), 0, new Color(8, 12, 28, 35)));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setClip(originalClip);
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
