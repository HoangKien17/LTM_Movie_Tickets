package web;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Movie-specific artwork with independent description scrolling and a fixed
 * CTA.
 */
final class CinemaBanner extends JPanel {

    private BufferedImage poster;
    private final JPanel words = new JPanel(new BorderLayout(0, 8));
    private final JPanel heading = new JPanel(new BorderLayout(0, 5));
    private final JTextArea title = new JTextArea();
    private final JLabel eyebrow = new JLabel("GALAXY CINEMA  /  KHÁM PHÁ");
    private final JLabel subtitle = new JLabel("Chọn phim. Chọn ghế. Tận hưởng.");
    private final JTextArea description = UiTheme.copy("Khám phá câu chuyện tiếp theo của bạn.", 16);
    private final JScrollPane descriptionScroll = UiTheme.scroll(description);
    private final JButton book = UiTheme.primary("Chọn suất chiếu");
    private Runnable selected;

    CinemaBanner() {
        super(new BorderLayout());
        setName("cinema-banner");
        setOpaque(false);
        setBorder(new EmptyBorder(22, 36, 22, 30));
        setPreferredSize(new Dimension(500, 310));
        words.setOpaque(false);
        heading.setOpaque(false);
        eyebrow.setForeground(UiTheme.PURPLE);
        eyebrow.setFont(UiTheme.font(12, Font.BOLD));
        heading.add(eyebrow, BorderLayout.NORTH);
        title.setName("banner-title");
        title.setText("TRẢI NGHIỆM ĐIỆN ẢNH");
        title.setFont(UiTheme.font(42, Font.BOLD));
        title.setForeground(UiTheme.TEXT);
        title.setEditable(false);
        title.setFocusable(false);
        title.setOpaque(false);
        title.setLineWrap(true);
        title.setWrapStyleWord(true);
        title.setBorder(null);
        heading.add(title, BorderLayout.CENTER);
        subtitle.setForeground(UiTheme.MUTED);
        subtitle.setFont(UiTheme.font(14, Font.PLAIN));
        heading.add(subtitle, BorderLayout.SOUTH);
        words.add(heading, BorderLayout.NORTH);
        description.setName("banner-description");
        description.setForeground(UiTheme.TEXT);
        description.setEditable(false);
        description.setFocusable(false);
        description.setOpaque(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setBorder(new EmptyBorder(0, 0, 0, 9));
        descriptionScroll.setName("banner-description-scroll");
        descriptionScroll.setOpaque(false);
        descriptionScroll.getViewport().setOpaque(false);
        descriptionScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        descriptionScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        descriptionScroll.getVerticalScrollBar().setUnitIncrement(18);
        descriptionScroll.setWheelScrollingEnabled(true);
        descriptionScroll.setPreferredSize(new Dimension(430, 60));
        descriptionScroll.setMinimumSize(new Dimension(0, 18));
        words.add(descriptionScroll, BorderLayout.CENTER);
        book.setName("banner-book");
        book.setIcon(HomeIcons.icon("arrow", 22));
        book.setHorizontalTextPosition(SwingConstants.LEFT);
        book.setIconTextGap(16);
        book.setFont(UiTheme.font(17, Font.BOLD));
        book.setPreferredSize(new Dimension(250, 48));
        book.addActionListener(e -> {
            if (selected != null) {
                selected.run();
        
            }});
        book.setEnabled(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.setOpaque(false);
        actions.setBorder(new EmptyBorder(5, 0, 0, 0));
        actions.add(book);
        words.add(actions, BorderLayout.SOUTH);
        add(words, BorderLayout.WEST);
    }

    void movie(String name, String meta, BufferedImage image, Runnable action) {
        title.setText(name);
        title.setToolTipText(name);
        subtitle.setText(meta);
        subtitle.setToolTipText(meta);
        poster = image;
        selected = action;
        book.setEnabled(action != null);
        revalidate();
        repaint();
    }

    void caption(String text) {
        eyebrow.setText(text);
    }

    void description(String value) {
        description.setText(value == null || value.trim().isEmpty()
                ? "Trải nghiệm điện ảnh cùng Galaxy Cinema." : value);
        description.setCaretPosition(0);
        descriptionScroll.getViewport().setViewPosition(new Point());
    }

    /**
     * Keep the current film stable while the pointer is over its text/artwork.
     */
    boolean canRotate() {
        return getMousePosition(true) == null;
    }

    @Override
    public void doLayout() {
        Insets insets = getInsets();
        int inside = Math.max(1, getWidth() - insets.left - insets.right);
        int wordWidth = Math.max(180, Math.min(560, (int) (inside * .58)));
        words.setPreferredSize(new Dimension(wordWidth, 0));
        int fontSize = getWidth() < 850 || title.getText().length() > 28 ? 30 : 44;
        title.setFont(UiTheme.font(fontSize, Font.BOLD));
        FontMetrics metrics = title.getFontMetrics(title.getFont());
        int titleHeight = metrics.getHeight() * (metrics.stringWidth(title.getText()) > wordWidth ? 2 : 1);
        int maximumTitle = Math.max(metrics.getHeight(), getHeight() - insets.top - insets.bottom - 130);
        title.setPreferredSize(new Dimension(wordWidth, Math.min(titleHeight, maximumTitle)));
        super.doLayout();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        AuthStyle.quality(g);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        Shape originalClip = g.getClip();
        g.clip(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 24, 24));
        g.setPaint(new GradientPaint(0, 0, new Color(8, 14, 28), getWidth(), getHeight(), new Color(33, 24, 61)));
        g.fillRect(0, 0, getWidth(), getHeight());
        int left = words.getX() + words.getWidth() + 30;
        int availableWidth = Math.max(0, getWidth() - left - 30), availableHeight = Math.max(0, getHeight() - 32);
        if (availableWidth > 0 && availableHeight > 0) {
            float centerX = left + availableWidth / 2f, centerY = getHeight() / 2f;
            g.setPaint(new RadialGradientPaint(centerX, centerY, Math.max(1, availableWidth * .7f),
                    new float[]{0, 1}, new Color[]{new Color(69, 102, 159, 105), new Color(28, 21, 53, 0)}));
            g.fillRect(left, 0, availableWidth + 30, getHeight());
            if (poster != null) {
                // Contain the entire image; never stretch or enlarge the server's thumbnail.
                double scale = Math.min(1.0, Math.min(availableWidth / (double) poster.getWidth(), availableHeight / (double) poster.getHeight()));
                int w = Math.max(1, (int) Math.round(poster.getWidth() * scale));
                int h = Math.max(1, (int) Math.round(poster.getHeight() * scale));
                int x = left + (availableWidth - w) / 2, y = (getHeight() - h) / 2;
                g.setColor(new Color(0, 0, 0, 90));
                g.fillRoundRect(x - 7, y - 5, w + 14, h + 14, 18, 18);
                g.drawImage(poster, x, y, w, h, null);
                g.setColor(new Color(92, 226, 239, 110));
                g.drawRect(x - 1, y - 1, w + 1, h + 1);
            } else {
                g.setColor(UiTheme.PURPLE);
                g.setFont(UiTheme.font(12, Font.BOLD));
                String missing = "CHƯA CÓ POSTER";
                g.drawString(missing, left + Math.max(0, (availableWidth - g.getFontMetrics().stringWidth(missing)) / 2), getHeight() / 2);
            }
        }
        g.setClip(originalClip);
        g.setColor(UiTheme.LINE);
        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
        g.dispose();
        super.paintComponent(graphics);
    }
}
