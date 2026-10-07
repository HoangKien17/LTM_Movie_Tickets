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

    static final Color BACKGROUND = new Color(245, 247, 251);
    static final Color WHITE = Color.WHITE;
    static final Color NAVY = new Color(18, 27, 48);
    static final Color NAVY_LIGHT = new Color(34, 46, 73);
    static final Color TEXT = new Color(29, 39, 59);
    static final Color MUTED = new Color(105, 118, 139);
    static final Color LINE = new Color(227, 232, 241);
    static final Color PRIMARY = new Color(88, 78, 230);
    static final Color PRIMARY_DARK = new Color(70, 60, 205);
    static final Color PRIMARY_PALE = new Color(236, 234, 255);
    static final Color GREEN = new Color(26, 158, 118);
    static final Color GREEN_PALE = new Color(226, 247, 239);
    static final Color RED = new Color(207, 73, 94);
    static final Color RED_PALE = new Color(255, 235, 239);
    static final Color GOLD = new Color(244, 184, 80);

    private UiTheme() {
    }

    static Font font(int size, int style) {
        return new Font("Segoe UI", style, size);
    }

    static RoundedPanel surface() {
        RoundedPanel panel = new RoundedPanel(WHITE, 20);
        panel.setBorder(new EmptyBorder(18, 20, 18, 20));
        return panel;
    }

    static JTextField field(int columns) {
        JTextField field = new JTextField(columns);
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
        field.setBackground(WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE, 1, true),
                new EmptyBorder(10, 12, 10, 12)));
        field.setPreferredSize(new Dimension(220, 43));
    }

    static StyledButton primary(String text) {
        return new StyledButton(text, PRIMARY, WHITE, 12);
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
        pane.getViewport().setBackground(WHITE);
        pane.getVerticalScrollBar().setUnitIncrement(18);
        return pane;
    }

    static void styleTable(JTable table) {
        table.setFont(font(13, Font.PLAIN));
        table.setForeground(TEXT);
        table.setBackground(WHITE);
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
        header.setBackground(new Color(250, 251, 254));
        header.setPreferredSize(new Dimension(0, 44));
        header.setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable source, Object value, boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(source, value, selected, focus, row, column);
                setBorder(new EmptyBorder(0, 12, 0, 8));
                setFont(font(13, Font.PLAIN));
                setForeground(TEXT);
                setBackground(selected ? PRIMARY_PALE
                        : (row % 2 == 0 ? WHITE : new Color(250, 251, 254)));
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
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g.dispose();
            super.paintComponent(graphics);
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
            }else if (getModel().isRollover()) {
                color = blend(fill, NAVY, 0.08f);
            }
            if (!isEnabled()) {
                color = LINE;
            }
            g.setColor(color);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
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
            if (!taken) {
                setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = taken ? LINE : (isSelected() ? PRIMARY : GREEN_PALE);
            Color foreground = taken ? MUTED : (isSelected() ? WHITE : GREEN);
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            if (!taken && !isSelected()) {
                g.setColor(new Color(187, 230, 212));
                g.setStroke(new BasicStroke(1f));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
            }
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
            g.setPaint(new GradientPaint(0, 0, NAVY_LIGHT, getWidth(), getHeight(), PRIMARY));
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
