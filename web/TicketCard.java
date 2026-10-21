package web;

import java.awt.*;
import javax.swing.*;

/** Ticket paper with perforation; QR stays a real unfiltered black/white image. */
final class TicketCard extends UiTheme.RoundedPanel {
    private JComponent top;
    TicketCard() {
        super(UiTheme.SURFACE, 20);
        putClientProperty("glow", Boolean.TRUE);
        setLayout(new BorderLayout(0, 22));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
    }
    void header(JComponent header) { top = header; add(header, BorderLayout.NORTH); }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        int y = top == null ? getHeight() / 2 : top.getY() + top.getHeight() + 11;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{5, 5}, 0));
        g.setPaint(new GradientPaint(0, 0, UiTheme.PRIMARY, getWidth(), 0, UiTheme.PURPLE));
        g.drawLine(14, y, getWidth() - 14, y);
        g.setColor(UiTheme.BACKGROUND);
        g.fillOval(-10, y - 10, 20, 20);
        g.fillOval(getWidth() - 10, y - 10, 20, 20);
        g.dispose();
    }
}
