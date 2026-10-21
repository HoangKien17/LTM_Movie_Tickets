package web;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Dark window title bar with drag, maximize/restore, minimize, close and edge
 * resize.
 */
final class AppWindowChrome extends JPanel {

    private final JFrame window;
    private Rectangle normalBounds;
    private MouseAdapter windowDrag;

    AppWindowChrome(JFrame window) {
        super(new BorderLayout());
        this.window = window;
        setName("window-title-bar");
        setBackground(new Color(5, 12, 25));
        setPreferredSize(new Dimension(600, 34));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(44, 54, 75)));
        JLabel title = new JLabel("GALAXY CINEMA", new Icon() {
            public int getIconWidth() {
                return 22;
            }

            public int getIconHeight() {
                return 22;
            }

            public void paintIcon(Component c, Graphics g, int x, int y) {
                AuthStyle.orbit((Graphics2D) g, x, y, 22);
            }
        }, SwingConstants.LEFT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
        title.setIconTextGap(9);
        title.setFont(AuthStyle.font(13, true));
        title.setPreferredSize(new Dimension(210, 33));
        title.setForeground(AuthStyle.TEXT);
        add(title, BorderLayout.WEST);
        java.awt.image.BufferedImage appIcon = new java.awt.image.BufferedImage(48, 48, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D iconGraphics = appIcon.createGraphics();
        AuthStyle.orbit(iconGraphics, 0, 0, 48);
        iconGraphics.dispose();
        window.setIconImage(appIcon);
        JPanel controls = new JPanel(new GridLayout(1, 3));
        controls.setOpaque(false);
        controls.add(control("minimize", "Thu nhỏ", () -> window.setExtendedState(window.getExtendedState() | Frame.ICONIFIED)));
        controls.add(control("maximize", "Phóng to / Khôi phục", this::toggleMaximized));
        controls.add(control("close", "Đóng ứng dụng", () -> window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING))));
        add(controls, BorderLayout.EAST);
        MouseAdapter drag = new MouseAdapter() {
            private Point pressed;
            private Rectangle start;

            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                pressed = e.getLocationOnScreen();
                start = window.getBounds();
            }

            public void mouseDragged(MouseEvent e) {
                if (pressed == null) {
                    return;
                }
                Point now = e.getLocationOnScreen();
                if (maximized()) {
                    double ratio = (pressed.x - window.getX()) / (double) Math.max(1, window.getWidth());
                    Rectangle restore = normalBounds == null ? new Rectangle(0, 0, 1440, 900) : new Rectangle(normalBounds);
                    window.setExtendedState(Frame.NORMAL);
                    restore.setLocation(now.x - (int) (restore.width * ratio), now.y - 15);
                    window.setBounds(restore);
                    pressed = now;
                    start = restore;
                } else {
                    window.setLocation(start.x + now.x - pressed.x, start.y + now.y - pressed.y);
                }
            }

            public void mouseReleased(MouseEvent e) {
                pressed = null;
            }

            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    toggleMaximized();
                }
            }
        };
        windowDrag = drag;
        addMouseListener(drag);
        addMouseMotionListener(drag);
        title.addMouseListener(drag);
        title.addMouseMotionListener(drag);
        window.addWindowStateListener(e -> controls.repaint());
        window.setGlassPane(new ResizeEdges());
        window.getGlassPane().setVisible(true);
    }

    private boolean maximized() {
        return (window.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
    }

    void attachDrag(JComponent component) {
        component.addMouseListener(windowDrag);
        component.addMouseMotionListener(windowDrag);
    }

    JPanel embeddedControls() {
        JPanel controls = new JPanel(new GridLayout(1, 3));
        controls.setOpaque(false);
        controls.add(control("minimize", "Thu nhỏ", () -> window.setExtendedState(window.getExtendedState() | Frame.ICONIFIED)));
        controls.add(control("maximize", "Phóng to / Khôi phục", this::toggleMaximized));
        controls.add(control("close", "Đóng ứng dụng", () -> window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING))));
        window.addWindowStateListener(e -> controls.repaint());
        return controls;
    }

    private void toggleMaximized() {
        if (maximized()) {
            window.setExtendedState(Frame.NORMAL);
            if (normalBounds != null) {
                window.setBounds(normalBounds);
            }
        } else {
            normalBounds = window.getBounds();
            GraphicsConfiguration config = window.getGraphicsConfiguration();
            Rectangle bounds = new Rectangle(config.getBounds());
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
            bounds.x += insets.left;
            bounds.y += insets.top;
            bounds.width -= insets.left + insets.right;
            bounds.height -= insets.top + insets.bottom;
            window.setMaximizedBounds(bounds);
            window.setExtendedState(Frame.MAXIMIZED_BOTH);
        }
    }

    private JButton control(String type, String label, Runnable action) {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                AuthStyle.quality(g);
                if (getModel().isRollover() || hasFocus()) {
                    g.setColor("close".equals(type) ? new Color(190, 47, 75) : new Color(32, 41, 62));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
                g.setColor(new Color(196, 204, 225));
                g.setStroke(new BasicStroke(1.2f));
                int x = getWidth() / 2, y = getHeight() / 2;
                if ("minimize".equals(type)) {
                    g.drawLine(x - 6, y, x + 6, y); 
                }else if ("close".equals(type)) {
                    g.drawLine(x - 5, y - 5, x + 5, y + 5);
                    g.drawLine(x - 5, y + 5, x + 5, y - 5);
                } else if (maximized()) {
                    g.drawRect(x - 4, y - 6, 9, 9);
                    g.setColor(getBackground());
                    g.fillRect(x - 6, y - 3, 9, 9);
                    g.setColor(new Color(196, 204, 225));
                    g.drawRect(x - 6, y - 3, 9, 9);
                } else {
                    g.drawRoundRect(x - 5, y - 6, 11, 11, 1, 1);
                }
                g.dispose();
            }
        };
        button.setName("window-" + type);
        button.setToolTipText(label);
        button.getAccessibleContext().setAccessibleName(label);
        button.setPreferredSize(new Dimension(44, 33));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBackground(getBackground());
        button.addActionListener(e -> action.run());
        return button;
    }

    private final class ResizeEdges extends JComponent {

        private static final int EDGE = 6;
        private int direction;
        private Rectangle start;
        private Point pressed;

        ResizeEdges() {
            setOpaque(false);
            MouseAdapter resize = new MouseAdapter() {
                public void mouseMoved(MouseEvent e) {
                    setCursor(Cursor.getPredefinedCursor(cursor(e.getX(), e.getY())));
                }

                public void mouseExited(MouseEvent e) {
                    if (pressed == null) {
                        setCursor(Cursor.getDefaultCursor());
                
                    }}

                public void mousePressed(MouseEvent e) {
                    if (!SwingUtilities.isLeftMouseButton(e)) {
                        return;
                    }
                    direction = cursor(e.getX(), e.getY());
                    start = window.getBounds();
                    pressed = e.getLocationOnScreen();
                }

                public void mouseDragged(MouseEvent e) {
                    if (pressed == null || maximized()) {
                        return;
                    }
                    int dx = e.getXOnScreen() - pressed.x, dy = e.getYOnScreen() - pressed.y;
                    Rectangle bounds = new Rectangle(start);
                    boolean west = direction == Cursor.W_RESIZE_CURSOR || direction == Cursor.NW_RESIZE_CURSOR || direction == Cursor.SW_RESIZE_CURSOR;
                    boolean east = direction == Cursor.E_RESIZE_CURSOR || direction == Cursor.NE_RESIZE_CURSOR || direction == Cursor.SE_RESIZE_CURSOR;
                    boolean north = direction == Cursor.N_RESIZE_CURSOR || direction == Cursor.NW_RESIZE_CURSOR || direction == Cursor.NE_RESIZE_CURSOR;
                    boolean south = direction == Cursor.S_RESIZE_CURSOR || direction == Cursor.SW_RESIZE_CURSOR || direction == Cursor.SE_RESIZE_CURSOR;
                    if (west) {
                        bounds.x += dx;
                        bounds.width -= dx;
                    }
                    if (east) {
                        bounds.width += dx;
                    }
                    if (north) {
                        bounds.y += dy;
                        bounds.height -= dy;
                    }
                    if (south) {
                        bounds.height += dy;
                    }
                    Dimension min = window.getMinimumSize();
                    if (bounds.width < min.width) {
                        if (west) {
                            bounds.x = start.x + start.width - min.width;
                        
                        }bounds.width = min.width;
                    }
                    if (bounds.height < min.height) {
                        if (north) {
                            bounds.y = start.y + start.height - min.height;
                        
                        }bounds.height = min.height;
                    }
                    window.setBounds(bounds);
                }

                public void mouseReleased(MouseEvent e) {
                    pressed = null;
                }
            };
            addMouseListener(resize);
            addMouseMotionListener(resize);
        }

        @Override
        public boolean contains(int x, int y) {
            return !maximized() && window.isResizable() && (x < EDGE || y < EDGE || x >= getWidth() - EDGE || y >= getHeight() - EDGE);
        }

        private int cursor(int x, int y) {
            boolean w = x < EDGE, e = x >= getWidth() - EDGE, n = y < EDGE, s = y >= getHeight() - EDGE;
            if (n && w) {
                return Cursor.NW_RESIZE_CURSOR;
            }
            if (n && e) {
                return Cursor.NE_RESIZE_CURSOR;
            }
            if (s && w) {
                return Cursor.SW_RESIZE_CURSOR;
            }
            if (s && e) {
                return Cursor.SE_RESIZE_CURSOR;
            }
            return w ? Cursor.W_RESIZE_CURSOR : e ? Cursor.E_RESIZE_CURSOR : n ? Cursor.N_RESIZE_CURSOR : Cursor.S_RESIZE_CURSOR;
        }
    }
}
