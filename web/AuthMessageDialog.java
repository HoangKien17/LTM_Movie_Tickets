package web;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Compact modal matching the auth card, with no native white title bar. */
final class AuthMessageDialog extends JDialog {
    AuthMessageDialog(Window owner, String title, String message, boolean error) {
        super(owner, title, ModalityType.DOCUMENT_MODAL);
        setName("auth-message-dialog");
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        Color accent = error ? new Color(247, 111, 153) : AuthStyle.CYAN;
        JPanel panel = new JPanel(new BorderLayout(0, 20)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create(); AuthStyle.quality(g);
                g.setPaint(new GradientPaint(0, 0, new Color(12, 25, 44), getWidth(), getHeight(), new Color(23, 14, 44)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setPaint(new GradientPaint(0, 0, accent, getWidth(), getHeight(), AuthStyle.PURPLE));
                g.setStroke(new BasicStroke(1.6f));
                g.draw(new RoundRectangle2D.Double(1, 1, getWidth() - 3, getHeight() - 3, 24, 24));
                g.dispose();
            }
        };
        panel.setName("auth-message-panel");
        panel.setBorder(new EmptyBorder(24, 28, 26, 28));
        JLabel brand = new JLabel("GALAXY CINEMA");
        brand.setForeground(AuthStyle.MUTED); brand.setFont(AuthStyle.wordmark(12));
        JButton close = new JButton("×");
        close.setName("auth-message-close"); close.setToolTipText("Đóng");
        close.setForeground(AuthStyle.MUTED); close.setFont(AuthStyle.font(24, false));
        close.setBorder(new EmptyBorder(0, 10, 0, 0)); close.setContentAreaFilled(false);
        close.addActionListener(e -> dispose());
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false);
        top.add(brand, BorderLayout.WEST); top.add(close, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);
        JPanel content = new JPanel(new BorderLayout(0, 13)); content.setOpaque(false);
        JLabel heading = new JLabel(title, statusIcon(error, accent), SwingConstants.LEFT);
        heading.setName("auth-message-heading");
        heading.setIconTextGap(12);
        heading.getAccessibleContext().setAccessibleDescription(error ? "Thông báo lỗi" : "Thông báo thành công");
        heading.setForeground(accent); heading.setFont(AuthStyle.font(24, true));
        content.add(heading, BorderLayout.NORTH);
        JTextArea body = new JTextArea(message);
        body.setName("auth-message-body");
        body.setEditable(false); body.setFocusable(false); body.setOpaque(false);
        body.setLineWrap(true); body.setWrapStyleWord(true);
        body.setForeground(AuthStyle.TEXT); body.setFont(AuthStyle.font(16, false));
        body.setSize(460, Short.MAX_VALUE);
        int height = Math.min(230, Math.max(48, body.getPreferredSize().height + 5));
        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null); scroll.setOpaque(false); scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(460, height));
        content.add(scroll, BorderLayout.CENTER); panel.add(content, BorderLayout.CENTER);
        JButton okay = UiTheme.primary(error ? "Đã hiểu" : "Tiếp tục");
        okay.setName("auth-message-ok");
        okay.setFont(AuthStyle.font(17, true)); okay.setPreferredSize(new Dimension(150, 44));
        okay.addActionListener(e -> dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); actions.setOpaque(false);
        actions.add(okay); panel.add(actions, BorderLayout.SOUTH);
        setContentPane(panel); getRootPane().setDefaultButton(okay);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack(); setResizable(false); setLocationRelativeTo(owner);
        try { setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 24, 24)); }
        catch (UnsupportedOperationException ignored) { }
        MouseAdapter drag = new MouseAdapter() {
            private Point start;
            public void mousePressed(MouseEvent e) { start = e.getPoint(); }
            public void mouseDragged(MouseEvent e) {
                if (start != null) setLocation(e.getXOnScreen() - start.x, e.getYOnScreen() - start.y);
            }
        };
        top.addMouseListener(drag); top.addMouseMotionListener(drag);
    }
    /** Draw status as geometry so it never depends on a font's symbol glyphs. */
    private static Icon statusIcon(boolean error, Color accent) {
        return new Icon() {
            public int getIconWidth() { return 30; }
            public int getIconHeight() { return 30; }
            public void paintIcon(Component c, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                AuthStyle.quality(g);
                g.translate(x, y);
                g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28));
                g.fillOval(1, 1, 28, 28);
                g.setColor(accent);
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawOval(1, 1, 28, 28);
                g.setStroke(new BasicStroke(2.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                if (error) {
                    g.drawLine(15, 8, 15, 17);
                    g.fillOval(14, 21, 3, 3);
                } else {
                    g.drawLine(8, 15, 13, 20);
                    g.drawLine(13, 20, 22, 10);
                }
                g.dispose();
            }
        };
    }

    static void show(Window owner, String title, String message, boolean error) {
        AuthMessageDialog dialog = new AuthMessageDialog(owner, title, message, error);
        try { dialog.setVisible(true); } finally { dialog.dispose(); }
    }
}
