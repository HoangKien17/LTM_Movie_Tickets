package web;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Compact account popup with identity header and keyboard-accessible menu items. */
final class AccountMenu extends JPopupMenu {
    private final JLabel name = new JLabel("Tài khoản của bạn");
    private final JLabel email = new JLabel("GALAXY CINEMA");

    AccountMenu(Runnable profile, Runnable logout) {
        setName("account-menu");
        setOpaque(false);
        setBackground(UiTheme.NAVY);
        setBorder(new EmptyBorder(14, 12, 12, 12));
        JPanel identity = new JPanel(new BorderLayout(12, 0));
        identity.setOpaque(false);
        identity.setBorder(new EmptyBorder(6, 9, 16, 9));
        JLabel avatar = new JLabel(HomeIcons.icon("user", 30));
        avatar.setForeground(UiTheme.PRIMARY);
        avatar.setBorder(new EmptyBorder(5, 4, 5, 4));
        identity.add(avatar, BorderLayout.WEST);
        JPanel words = new JPanel(new GridLayout(2, 1, 0, 3));
        words.setOpaque(false);
        name.setFont(UiTheme.font(17, Font.BOLD)); name.setForeground(UiTheme.TEXT);
        email.setFont(UiTheme.font(12, Font.PLAIN)); email.setForeground(UiTheme.MUTED);
        words.add(name); words.add(email); identity.add(words, BorderLayout.CENTER);
        identity.setPreferredSize(new Dimension(286, 69));
        add(identity);
        JPanel divider = new JPanel();
        divider.setBackground(UiTheme.LINE);
        divider.setPreferredSize(new Dimension(286, 1));
        add(divider);
        add(Box.createVerticalStrut(8));
        add(new MenuEntry("Thông tin cá nhân", "Cập nhật liên hệ và bảo mật", "user", false, () -> {
            setVisible(false); profile.run();
        }));
        add(Box.createVerticalStrut(6));
        add(new MenuEntry("Đăng xuất", "Kết thúc phiên trên cửa sổ này", "logout", true, () -> {
            setVisible(false); logout.run();
        }));
    }

    void identity(String displayName, String address) {
        name.setText(displayName == null || displayName.isEmpty() ? "Tài khoản của bạn" : displayName);
        name.setToolTipText(displayName);
        email.setText(address == null || address.isEmpty() ? "GALAXY CINEMA" : address);
        email.setToolTipText(address);
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create(); AuthStyle.quality(g);
        g.setPaint(new GradientPaint(0, 0, new Color(14, 25, 44), getWidth(), getHeight(), new Color(24, 16, 42)));
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
        g.setPaint(new GradientPaint(0, 0, new Color(56, 174, 196), getWidth(), getHeight(), new Color(120, 85, 175)));
        g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 24, 24); g.dispose();
    }

    private static final class MenuEntry extends JMenuItem {
        private final String subtitle, icon;
        private final boolean danger;
        MenuEntry(String title, String subtitle, String icon, boolean danger, Runnable action) {
            super(title); this.subtitle = subtitle; this.icon = icon; this.danger = danger;
            setName(danger ? "account-logout" : "account-profile");
            setPreferredSize(new Dimension(286, 67)); setBorder(new EmptyBorder(0, 0, 0, 0));
            setOpaque(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getAccessibleContext().setAccessibleDescription(subtitle);
            addActionListener(e -> action.run());
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create(); AuthStyle.quality(g);
            boolean selected = getModel().isArmed() || getModel().isRollover() || hasFocus();
            if (selected) {
                g.setColor(danger ? new Color(61, 28, 49) : new Color(20, 49, 65));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            }
            Color accent = danger ? UiTheme.RED : UiTheme.PRIMARY;
            setForeground(accent);
            HomeIcons.icon(icon, 23).paintIcon(this, g, 12, 20);
            g.setFont(UiTheme.font(15, Font.BOLD)); g.setColor(danger ? UiTheme.RED : UiTheme.TEXT);
            g.drawString(getText(), 49, 27);
            g.setFont(UiTheme.font(12, Font.PLAIN)); g.setColor(UiTheme.MUTED); g.drawString(subtitle, 49, 47);
            setForeground(UiTheme.MUTED);
            HomeIcons.icon("chevron-right", 14).paintIcon(this, g, getWidth() - 23, 26);
            g.dispose();
        }
    }
}
