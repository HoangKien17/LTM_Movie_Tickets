package web;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/**
 * Màu sắc và thành phần hiển thị. Không chứa giao thức hay truy cập dữ liệu.
 */
final class UiTheme {

    static final Color BACKGROUND = new Color(9, 13, 25);
    static final Color SURFACE = new Color(18, 25, 43);
    static final Color WHITE = Color.WHITE;
    static final Color NAVY = new Color(12, 17, 31);
    static final Color NAVY_LIGHT = new Color(26, 35, 57);
    static final Color TEXT = new Color(236, 242, 255);
    static final Color MUTED = new Color(154, 170, 194);
    static final Color LINE = new Color(43, 57, 79);
    static final Color PRIMARY = new Color(92, 226, 239);
    static final Color PURPLE = new Color(166, 117, 255);
    static final Color PRIMARY_DARK = PRIMARY;
    static final Color PRIMARY_PALE = new Color(35, 42, 74);
    static final Color GREEN = new Color(104, 229, 190);
    static final Color GREEN_PALE = new Color(24, 53, 56);
    static final Color RED = new Color(255, 135, 158);
    static final Color RED_PALE = new Color(64, 31, 48);
    static final Color GOLD = new Color(244, 184, 80);

    private UiTheme() {
    }

    static {
        install();
    }

    /**
     * Defaults also apply to account forms, file pickers and native Swing
     * dialogs.
     */
    static void install() {
        String[] components = {"Panel", "OptionPane", "Label", "Button", "ToggleButton",
            "CheckBox", "RadioButton", "ComboBox", "List", "Tree", "TextField", "PasswordField",
            "TextArea", "TextPane", "Spinner", "Menu", "MenuItem", "PopupMenu", "TabbedPane",
            "ScrollPane", "Viewport", "ToolTip"};
        for (String component : components) {
            javax.swing.UIManager.put(component + ".background", SURFACE);
            javax.swing.UIManager.put(component + ".foreground", TEXT);
            javax.swing.UIManager.put(component + ".font", font(13, Font.PLAIN));
        }
        for (String component : new String[]{"TextField", "PasswordField", "TextArea", "TextPane"}) {
            javax.swing.UIManager.put(component + ".caretForeground", PRIMARY);
            javax.swing.UIManager.put(component + ".selectionBackground", PRIMARY_PALE);
            javax.swing.UIManager.put(component + ".selectionForeground", WHITE);
            javax.swing.UIManager.put(component + ".inactiveForeground", MUTED);
        }
        javax.swing.UIManager.put("OptionPane.messageForeground", TEXT);
        javax.swing.UIManager.put("OptionPane.messageFont", font(13, Font.PLAIN));
        javax.swing.UIManager.put("OptionPane.buttonFont", font(13, Font.BOLD));
        javax.swing.UIManager.put("ComboBox.selectionBackground", PRIMARY_PALE);
        javax.swing.UIManager.put("ComboBox.selectionForeground", TEXT);
        javax.swing.UIManager.put("List.selectionBackground", PRIMARY_PALE);
        javax.swing.UIManager.put("List.selectionForeground", TEXT);
        javax.swing.UIManager.put("MenuItem.selectionBackground", PRIMARY_PALE);
        javax.swing.UIManager.put("MenuItem.selectionForeground", PRIMARY);
        javax.swing.UIManager.put("TabbedPane.selected", PRIMARY_PALE);
        javax.swing.UIManager.put("TabbedPane.contentAreaColor", SURFACE);
        javax.swing.UIManager.put("TabbedPane.focus", PRIMARY);
        javax.swing.UIManager.put("ScrollBar.thumb", LINE);
        javax.swing.UIManager.put("ScrollBar.track", BACKGROUND);
        javax.swing.UIManager.put("ScrollBar.width", 10);
        javax.swing.UIManager.put("SplitPane.background", BACKGROUND);
        javax.swing.UIManager.put("SplitPane.dividerSize", 8);
        for (String key : new String[]{"controlShadow", "controlDkShadow", "controlHighlight",
            "controlLtHighlight", "TabbedPane.shadow", "TabbedPane.darkShadow",
            "TabbedPane.highlight", "TabbedPane.light", "TabbedPane.borderHightlightColor"}) {
            javax.swing.UIManager.put(key, LINE);
        }
        javax.swing.UIManager.put("control", SURFACE);
        javax.swing.UIManager.put("controlText", TEXT);
    }

    static Font font(int size, int style) {
        return new Font("Segoe UI", style, size);
    }

    static RoundedPanel surface() {
        RoundedPanel panel = new RoundedPanel(SURFACE, 20);
        panel.setBorder(new EmptyBorder(18, 20, 18, 20));
        return panel;
    }

    static JTextField field(int columns) {
        JTextField field = new JTextField(columns);
        styleField(field);
        return field;
    }

    static JTextField hintField(int columns, String hint) {
        JTextField field = new HintField(columns, hint);
        styleField(field);
        return field;
    }

    static JPasswordField password(int columns) {
        JPasswordField field = new JPasswordField(columns);
        styleField(field);
        return field;
    }

    static void styleField(JTextField field) {
        field.setFont(font(14, Font.PLAIN));
        field.setForeground(TEXT);
        field.setCaretColor(PRIMARY);
        field.setBackground(BACKGROUND);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE, 1, true),
                new EmptyBorder(10, 12, 10, 12)));
        field.setPreferredSize(new Dimension(220, 43));
    }

    static StyledButton primary(String text) {
        return new StyledButton(text, PRIMARY, NAVY, 14);
    }

    static StyledButton secondary(String text) {
        return new StyledButton(text, PRIMARY_PALE, PRIMARY_DARK, 12);
    }

    static StyledButton subtle(String text) {
        return new StyledButton(text, BACKGROUND, TEXT, 12);
    }

    static StyledButton danger(String text) {
        return new StyledButton(text, RED_PALE, RED, 12);
    }

    static JScrollPane scroll(JComponent view) {
        JScrollPane pane = new JScrollPane(view);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.getViewport().setBackground(SURFACE);
        if (view instanceof JTable) {
            pane.setColumnHeaderView(((JTable) view).getTableHeader());
        }
        pane.getVerticalScrollBar().setUnitIncrement(18);
        darkScrollbar(pane.getVerticalScrollBar());
        darkScrollbar(pane.getHorizontalScrollBar());
        return pane;
    }

    private static void darkScrollbar(javax.swing.JScrollBar bar) {
        bar.setPreferredSize(new Dimension(10, 10));
        bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = LINE;
                trackColor = BACKGROUND;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return blank();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return blank();
            }

            private JButton blank() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }

            @Override
            protected void paintThumb(Graphics graphics, JComponent c, java.awt.Rectangle bounds) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(isThumbRollover() ? MUTED : LINE);
                g.fillRoundRect(bounds.x + 1, bounds.y + 1, Math.max(0, bounds.width - 2),
                        Math.max(0, bounds.height - 2), 8, 8);
                g.dispose();
            }
        });
    }

    static void styleCombo(javax.swing.JComboBox<?> combo) {
        combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton arrow = new JButton() {
                    @Override
                    protected void paintComponent(Graphics graphics) {
                        super.paintComponent(graphics);
                        Graphics2D g = (Graphics2D) graphics.create();
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g.setColor(isEnabled() ? PRIMARY : MUTED);
                        g.setStroke(new BasicStroke(1.7f));
                        int x = getWidth() / 2, y = getHeight() / 2;
                        g.drawLine(x - 4, y - 2, x, y + 2);
                        g.drawLine(x, y + 2, x + 4, y - 2);
                        g.dispose();
                    }
                };
                arrow.setPreferredSize(new Dimension(30, 30));
                arrow.setForeground(PRIMARY);
                arrow.setBackground(NAVY_LIGHT);
                arrow.setBorder(new EmptyBorder(0, 5, 0, 5));
                return arrow;
            }
        });
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE, 1, true), new EmptyBorder(3, 7, 3, 3)));
        combo.setBackground(SURFACE);
        combo.setForeground(TEXT);
        combo.setFont(font(13, Font.PLAIN));
    }

    static void styleTabs(javax.swing.JTabbedPane tabs) {
        tabs.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                tabInsets = new java.awt.Insets(10, 18, 10, 18);
                tabAreaInsets = new java.awt.Insets(0, 0, 10, 0);
                contentBorderInsets = new java.awt.Insets(0, 0, 0, 0);
            }

            @Override
            protected void paintTabBackground(Graphics graphics, int placement, int index,
                    int x, int y, int w, int h, boolean selected) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(selected ? PRIMARY_PALE : NAVY);
                g.fillRoundRect(x + 2, y + 2, w - 4, h - 4, 12, 12);
                if (selected) {
                    g.setColor(PRIMARY);
                    g.fillRoundRect(x + 15, y + h - 3, w - 30, 2, 2, 2);
                }
                g.dispose();
            }

            @Override
            protected void paintTabBorder(Graphics g, int placement, int index,
                    int x, int y, int w, int h, boolean selected) {
            }

            @Override
            protected void paintContentBorder(Graphics g, int placement, int selected) {
            }
        });
    }

    static void styleTable(JTable table) {
        table.setFont(font(13, Font.PLAIN));
        table.setForeground(TEXT);
        table.setBackground(SURFACE);
        table.setSelectionBackground(PRIMARY_PALE);
        table.setSelectionForeground(TEXT);
        table.setRowHeight(44);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        JTableHeader header = table.getTableHeader();
        header.setFont(font(12, Font.BOLD));
        header.setForeground(MUTED);
        header.setBackground(NAVY_LIGHT);
        header.setPreferredSize(new Dimension(0, 44));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable source, Object value, boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(source, value, selected, focus, row, column);
                setFont(font(12, Font.BOLD));
                setForeground(MUTED);
                setBackground(NAVY_LIGHT);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, LINE), new EmptyBorder(0, 12, 0, 8)));
                return this;
            }
        });
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable source, Object value, boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(source, value, selected, focus, row, column);
                setBorder(new EmptyBorder(0, 12, 0, 8));
                setFont(font(13, Font.PLAIN));
                setForeground(TEXT);
                setBackground(selected ? PRIMARY_PALE
                        : (row % 2 == 0 ? SURFACE : NAVY));
                return this;
            }
        });
    }

    static class RoundedPanel extends javax.swing.JPanel {

        private final Color fill;
        private final int radius;

        RoundedPanel(Color fill, int radius) {
            this.fill = fill;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, blend(fill, WHITE, 0.025f), getWidth(), getHeight(), fill));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g.setColor(LINE);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class HintField extends JTextField {

        private final String hint;

        HintField(int columns, String hint) {
            super(columns);
            this.hint = hint;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!getText().isEmpty() || isFocusOwner()) {
                return;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            g.setFont(getFont());
            g.setColor(MUTED);
            java.awt.Insets insets = getInsets();
            int y = (getHeight() - g.getFontMetrics().getHeight()) / 2 + g.getFontMetrics().getAscent();
            g.drawString(hint, insets.left, y);
            g.dispose();
        }
    }

    static class StyledButton extends JButton {

        private final Color fill;
        private final Color textColor;
        private final int radius;

        StyledButton(String text, Color fill, Color textColor, int radius) {
            super(text);
            this.fill = fill;
            this.textColor = textColor;
            this.radius = radius;
            setFont(font(13, Font.BOLD));
            setForeground(textColor);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 16, 10, 16));
            setPreferredSize(new Dimension(Math.max(92, text.length() * 8 + 34), 40));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color color = fill;
            if (getModel().isPressed()) {
                color = blend(fill, NAVY, 0.16f);
            } else if (getModel().isRollover()) {
                color = blend(fill, NAVY, 0.08f);
            }
            if (!isEnabled()) {
                color = LINE;
            }
            g.setPaint(fill.equals(PRIMARY) && isEnabled()
                    ? new GradientPaint(0, 0, PURPLE, getWidth(), 0, color) : color);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            if (hasFocus()) {
                g.setColor(WHITE);
                g.drawRoundRect(3, 3, getWidth() - 7, getHeight() - 7, radius, radius);
            }
            g.dispose();
            setForeground(isEnabled() ? textColor : MUTED);
            super.paintComponent(graphics);
        }
    }

    static class SeatButton extends JToggleButton {

        private final boolean taken;

        SeatButton(String code, boolean taken) {
            super(code);
            this.taken = taken;
            setEnabled(!taken);
            setFont(font(12, Font.BOLD));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setPreferredSize(new Dimension(66, 42));
            setMargin(new java.awt.Insets(1, 1, 4, 1));
            setBorder(new EmptyBorder(1, 1, 4, 1));
            if (!taken) {
                setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = taken ? new Color(39, 44, 58) : (isSelected() ? PRIMARY : NAVY_LIGHT);
            Color foreground = taken ? MUTED : (isSelected() ? NAVY : TEXT);
            g.setColor(fill);
            g.fillRoundRect(3, 2, getWidth() - 6, getHeight() - 6, 9, 9);
            g.setColor(taken ? LINE : isSelected() ? PRIMARY : PURPLE);
            g.setStroke(new BasicStroke(hasFocus() ? 2f : 1f));
            g.drawRoundRect(3, 2, getWidth() - 7, getHeight() - 7, 9, 9);
            g.drawLine(8, getHeight() - 8, getWidth() - 9, getHeight() - 8);
            g.dispose();
            setForeground(foreground);
            super.paintComponent(graphics);
        }
    }

    static class PosterPanel extends javax.swing.JPanel {

        private final String label;

        PosterPanel(String label) {
            this.label = label;
            setOpaque(false);
            setPreferredSize(new Dimension(122, 170));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, NAVY_LIGHT, getWidth(), getHeight(), new Color(71, 39, 112)));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g.setColor(new Color(255, 255, 255, 28));
            g.fillOval(getWidth() - 85, -25, 115, 115);
            g.fillOval(-60, getHeight() - 70, 130, 130);
            g.setColor(GOLD);
            g.setFont(font(10, Font.BOLD));
            g.drawString("GALAXY CINEMA", 13, 26);
            g.setColor(WHITE);
            g.setFont(font(34, Font.BOLD));
            String initial = label.isEmpty() ? "P" : label.substring(0, 1).toUpperCase();
            g.drawString(initial, (getWidth() - g.getFontMetrics().stringWidth(initial)) / 2,
                    getHeight() / 2 + 12);
            g.dispose();
        }
    }

    private static Color blend(Color a, Color b, float amount) {
        float keep = 1f - amount;
        return new Color((int) (a.getRed() * keep + b.getRed() * amount),
                (int) (a.getGreen() * keep + b.getGreen() * amount),
                (int) (a.getBlue() * keep + b.getBlue() * amount));
    }
}
