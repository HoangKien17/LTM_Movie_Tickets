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
    private final JTextArea description = UiTheme.copy("Khám phá câu chuyện tiếp theo của bạn.", 17);
    private final JButton book = UiTheme.primary("Khám phá suất chiếu");
    private Runnable selected = () -> {
    };

    CinemaBanner() {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(28, 36, 24, 30));
        setPreferredSize(new Dimension(500, 310));
        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        eyebrow.setForeground(UiTheme.PURPLE);
        eyebrow.setFont(UiTheme.font(12, Font.BOLD));
        words.add(eyebrow);
        words.add(Box.createVerticalStrut(12));
        title.setText("TRẢI NGHIỆM\nMÀN ẢNH RỘNG LỚN");
        title.setFont(UiTheme.font(47, Font.BOLD));
        title.setForeground(UiTheme.TEXT);
        title.setEditable(false);
        title.setFocusable(false);
        title.setOpaque(false);
        title.setLineWrap(true);
        title.setWrapStyleWord(true);
        title.setPreferredSize(new Dimension(460, 100));
        title.setMaximumSize(new Dimension(540, 100));
        words.add(title);
        words.add(Box.createVerticalStrut(6));
        subtitle.setForeground(UiTheme.MUTED);
        subtitle.setFont(UiTheme.font(15, Font.PLAIN));
        words.add(subtitle);
        words.add(Box.createVerticalStrut(8));
        description.setForeground(UiTheme.TEXT);
        description.setMaximumSize(new Dimension(480, 44));
        description.setPreferredSize(new Dimension(420, 44));
        words.add(description);
        words.add(Box.createVerticalStrut(14));
        book.setText("Chọn suất chiếu");
        book.setIcon(HomeIcons.icon("arrow", 22));
        book.setHorizontalTextPosition(SwingConstants.LEFT);
        book.setIconTextGap(16);
        book.setFont(UiTheme.font(18, Font.BOLD));
        book.setPreferredSize(new Dimension(230, 48));
        book.setMaximumSize(new Dimension(250, 48));
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

    void description(String value) {
        description.setText(value == null || value.trim().isEmpty() ? "Trải nghiệm điện ảnh cùng Galaxy Cinema." : value);
    }

    @Override
    public void doLayout() {
        int width = Math.max(210, Math.min(540, getWidth() * 60 / 100));
        title.setPreferredSize(new Dimension(width, getHeight() < 285 ? 64 : 88));
        title.setMaximumSize(new Dimension(width, getHeight() < 285 ? 64 : 88));
        title.setFont(UiTheme.font(title.getText().length() > 24 ? 31 : getWidth() < 850 ? 34 : 47, Font.BOLD));
        title.setToolTipText(title.getText());
        description.setVisible(getHeight() >= 285);
        description.setMaximumSize(new Dimension(width, 40));
        super.doLayout();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Shape originalClip = g.getClip();
        g.clip(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 24, 24));
        BufferedImage background = poster != null && poster.getWidth() >= 800
                && poster.getWidth() > poster.getHeight() ? poster : CYBER_HERO;
        if (background == null) {
            background = poster;
        }
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
