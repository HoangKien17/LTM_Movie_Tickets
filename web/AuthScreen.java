package web;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Responsive implementation of the supplied 1672×941 reference, with real
 * inputs.
 */
final class AuthScreen extends AuthBackdrop {

    enum Mode {
        LOGIN, REGISTER, RESET_EMAIL, RESET_PASSWORD
    }
    private final FormCard card;
    private final Timer resendTimer;
    private int resendSeconds;
    private boolean busy;

    AuthScreen(boolean registering, JTextField[] fields, JCheckBox remember,
            Runnable loginRoute, Runnable registerRoute, Runnable submit, Runnable help) {
        this(registering ? Mode.REGISTER : Mode.LOGIN, fields, remember, loginRoute, registerRoute, submit, help);
    }

    AuthScreen(Mode mode, JTextField[] fields, JCheckBox remember,
            Runnable loginRoute, Runnable registerRoute, Runnable submit, Runnable help) {
        setLayout(null);
        setName("auth-" + mode.name().toLowerCase(java.util.Locale.ROOT).replace('_', '-'));
        getAccessibleContext().setAccessibleName(mode == Mode.REGISTER ? "Đăng ký Galaxy Cinema"
                : mode == Mode.LOGIN ? "Đăng nhập Galaxy Cinema" : "Khôi phục tài khoản Galaxy Cinema");
        card = new FormCard(mode, fields, remember, loginRoute, registerRoute, submit, help);
        add(card);
        resendTimer = new Timer(1000, e -> {
            if (resendSeconds > 0) {
                resendSeconds--;
            }
            updateResend();
            if (resendSeconds == 0) {
                ((Timer) e.getSource()).stop();
            }
        });
    }

    @Override
    public void doLayout() {
        double scale = designScale();
        card.setBounds((int) Math.round(originX() + 1044 * scale), (int) Math.round(originY() + 58 * scale),
                (int) Math.round(560 * scale), (int) Math.round(780 * scale));
    }

    void setBusy(boolean busy) {
        this.busy = busy;
        card.submit.setEnabled(!busy);
        card.submit.setText(busy ? "Đang xử lý..." : card.actionText);
        card.submit.setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.HAND_CURSOR));
        card.login.setEnabled(!busy);
        card.register.setEnabled(!busy && !card.recovery);
        card.switchLink.setEnabled(!busy);
        if (card.mode == Mode.RESET_PASSWORD) {
            updateResend();
        } else {
            card.help.setEnabled(!busy);
        }
    }

    void setRecoveryEmail(String email) {
        String display = email.length() > 44 ? email.substring(0, 22) + "…" + email.substring(email.length() - 21) : email;
        card.subtitle.setText("<html>Mã xác nhận dành cho:<br>" + display.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</html>");
        card.subtitle.setToolTipText(email);
    }

    void startResendCooldown() {
        resendSeconds = 60;
        updateResend();
        resendTimer.restart();
    }

    private void updateResend() {
        card.help.setText(resendSeconds > 0 ? "Gửi lại mã (" + resendSeconds + "s)" : "Gửi lại mã qua email");
        card.help.setEnabled(!busy && resendSeconds == 0);
    }

    @Override
    public void removeNotify() {
        resendTimer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = designGraphics(graphics);
        AuthStyle.orbit(g, 108, 55, 76);
        g.setColor(AuthStyle.TEXT);
        g.setFont(AuthStyle.wordmark(31));
        g.drawString("GALAXY CINEMA", 201, 101);
        g.setFont(AuthStyle.font(82, true));
        g.drawString("Mỗi suất chiếu,", 107, 215);
        g.drawString("một trải nghiệm", 107, 294);
        g.setColor(AuthStyle.CYAN);
        g.drawString("mới.", 107, 373);
        g.setColor(new Color(212, 223, 237));
        g.setFont(AuthStyle.font(28, false));
        g.drawString("Chọn phim yêu thích. Giữ chỗ hoàn hảo.", 108, 420);
        // Typography on the physical decorative ticket, separate from the form.
        Graphics2D ticket = (Graphics2D) g.create();
        ticket.rotate(-.12, 535, 750);
        AuthStyle.orbit(ticket, 402, 714, 65);
        ticket.setColor(AuthStyle.TEXT);
        ticket.setFont(AuthStyle.wordmark(24));
        ticket.drawString("GALAXY", 482, 739);
        ticket.drawString("CINEMA", 482, 768);
        ticket.dispose();
        g.dispose();
    }

    private static final class FormCard extends JPanel {

        private final boolean registering;
        private final boolean recovery;
        private final Mode mode;
        private final String actionText;
        private final TabButton login, register;
        private final JLabel heading, subtitle;
        private final JLabel[] captions;
        private final FieldShell[] inputs;
        private final JCheckBox remember;
        private final JButton help;
        private final GlowButton submit;
        private final SwitchLink switchLink;
        private final JLabel footer;
        private final JLabel notice;

        FormCard(Mode mode, JTextField[] fields, JCheckBox remember,
                Runnable loginRoute, Runnable registerRoute, Runnable submitAction, Runnable helpAction) {
            this.mode = mode;
            this.registering = mode == Mode.REGISTER;
            this.recovery = mode == Mode.RESET_EMAIL || mode == Mode.RESET_PASSWORD;
            this.remember = remember;
            setOpaque(false);
            setLayout(null);
            login = new TabButton(recovery ? "01  Nhập email" : "Đăng nhập", recovery ? mode == Mode.RESET_EMAIL : !registering);
            register = new TabButton(recovery ? "02  Đặt lại mật khẩu" : "Đăng ký", recovery ? mode == Mode.RESET_PASSWORD : registering);
            login.setName("auth-tab-login");
            register.setName("auth-tab-register");
            login.addActionListener(e -> {
                if (recovery) {
                    registerRoute.run();
                } else {
                    loginRoute.run();
            
                }});
            register.addActionListener(e -> registerRoute.run());
            if (recovery) {
                register.setEnabled(false);
            }
            add(login);
            add(register);
            heading = label(mode == Mode.RESET_EMAIL ? "Khôi phục tài khoản" : mode == Mode.RESET_PASSWORD
                    ? "Đặt lại mật khẩu" : registering ? "Tạo tài khoản" : "Chào mừng trở lại", AuthStyle.TEXT);
            subtitle = label(recovery ? "Nhận mã xác nhận qua email đã đăng ký." : registering ? "Bắt đầu hành trình điện ảnh của bạn." : "Đăng nhập để bắt đầu hành trình điện ảnh.", AuthStyle.MUTED);
            add(heading);
            add(subtitle);
            String[] names = registering ? new String[]{"Họ và tên", "Email", "Số điện thoại", "Mật khẩu (ít nhất 6 ký tự)"}
                    : new String[]{"Email hoặc số điện thoại", "Mật khẩu"};
            String[] hints = registering ? new String[]{"Nhập họ và tên", "Nhập địa chỉ email", "Nhập số điện thoại", "Nhập mật khẩu"}
                    : new String[]{"Nhập email hoặc số điện thoại", "Nhập mật khẩu"};
            String[] kinds = registering ? new String[]{"user", "mail", "phone", "lock"} : new String[]{"user", "lock"};
            if (mode == Mode.RESET_EMAIL) {
                names = new String[]{"Email đã đăng ký"};
                hints = new String[]{"Nhập địa chỉ email của bạn"};
                kinds = new String[]{"mail"};
            } else if (mode == Mode.RESET_PASSWORD) {
                names = new String[]{"Mã xác nhận gồm 6 chữ số", "Mật khẩu mới (6–128 ký tự)", "Nhập lại mật khẩu mới"};
                hints = new String[]{"Nhập mã trong email", "Nhập mật khẩu mới", "Nhập lại mật khẩu mới"};
                kinds = new String[]{"mail", "lock", "lock"};
            }
            captions = new JLabel[fields.length];
            inputs = new FieldShell[fields.length];
            for (int i = 0; i < fields.length; i++) {
                captions[i] = label(names[i], AuthStyle.TEXT);
                captions[i].setLabelFor(fields[i]);
                fields[i].getAccessibleContext().setAccessibleName(names[i]);
                inputs[i] = new FieldShell(fields[i], hints[i], kinds[i]);
                add(captions[i]);
                add(inputs[i]);
            }
            help = new JButton(recovery ? "Gửi lại mã qua email" : "Quên mật khẩu?");
            help.setName("auth-help");
            transparent(help);
            help.setForeground(AuthStyle.PURPLE);
            help.addActionListener(e -> helpAction.run());
            if (mode == Mode.LOGIN) {
                remember.setText("Ghi nhớ đăng nhập");
                remember.setToolTipText("Chỉ giữ email/số điện thoại trong cửa sổ này. Client mới luôn trống; không lưu mật khẩu.");
                remember.setOpaque(false);
                remember.setForeground(new Color(193, 213, 227));
                remember.setBorder(BorderFactory.createEmptyBorder());
                remember.setFocusPainted(true);
                add(remember);
                add(help);
            }
            if (mode == Mode.RESET_PASSWORD) {
                add(help);
            }
            notice = label("<html>Mã có hiệu lực trong <b>5 phút</b> và chỉ dùng một lần.<br><br>"
                    + "Nếu email đã đăng ký, bạn sẽ nhận được mã xác nhận. Hãy kiểm tra cả thư rác.</html>", AuthStyle.MUTED);
            if (mode == Mode.RESET_EMAIL) {
                add(notice);
            }
            actionText = mode == Mode.RESET_EMAIL ? "Gửi mã xác nhận" : mode == Mode.RESET_PASSWORD ? "Đặt lại mật khẩu" : registering ? "Đăng ký" : "Đăng nhập";
            submit = new GlowButton(actionText);
            submit.setName("auth-submit");
            submit.addActionListener(e -> submitAction.run());
            add(submit);
            switchLink = new SwitchLink(recovery ? "Đã nhớ mật khẩu? " : registering ? "Đã có tài khoản? " : "Chưa có tài khoản? ",
                    recovery ? "Quay lại đăng nhập" : registering ? "Đăng nhập ngay" : "Đăng ký ngay");
            switchLink.setName("auth-switch");
            switchLink.addActionListener(e -> {
                if (registering || recovery) {
                    loginRoute.run();
                } else {
                    registerRoute.run();
            
                }});
            add(switchLink);
            footer = label("Đặt vé  •  Chọn ghế  •  Tận hưởng", AuthStyle.MUTED);
            footer.setHorizontalAlignment(SwingConstants.CENTER);
            add(footer);
        }

        private static JLabel label(String text, Color color) {
            JLabel label = new JLabel(text);
            label.setForeground(color);
            return label;
        }

        @Override
        public void doLayout() {
            double s = getWidth() / 560.0;
            place(login, 48, 26, 236, 60, s);
            place(register, 284, 26, 228, 60, s);
            login.setFont(AuthStyle.font((float) ((recovery ? 17 : 19) * s), login.selected));
            register.setFont(AuthStyle.font((float) ((recovery ? 17 : 19) * s), register.selected));
            place(heading, 48, 111, 464, 62, s);
            heading.setFont(AuthStyle.font((float) ((recovery ? 39 : 44) * s), true));
            place(subtitle, 48, 176, 464, 42, s);
            subtitle.setFont(AuthStyle.font((float) (20 * s), false));
            if (mode == Mode.RESET_PASSWORD) {
                place(subtitle, 48, 175, 464, 59, s);
                subtitle.setFont(AuthStyle.font((float) (17 * s), false));
            }
            for (int i = 0; i < inputs.length; i++) {
                int y = mode == Mode.RESET_PASSWORD ? 241 + i * 105 : registering ? 229 + i * 91 : 240 + i * 113;
                place(captions[i], 48, y, 464, 30, s);
                captions[i].setFont(AuthStyle.font((float) (18.5 * s), false));
                place(inputs[i], 48, y + 38, 464, registering ? 53 : 56, s);
            }
            if (mode == Mode.LOGIN) {
                place(remember, 48, 468, 273, 38, s);
                remember.setFont(AuthStyle.font((float) (17 * s), false));
                remember.setIcon(new AuthStyle.CheckIcon(Math.max(14, (int) Math.round(27 * s))));
                remember.setSelectedIcon(remember.getIcon());
                remember.setIconTextGap((int) Math.round(12 * s));
                place(help, 345, 468, 167, 38, s);
                help.setFont(AuthStyle.font((float) (16 * s), false).deriveFont(
                        java.util.Collections.singletonMap(java.awt.font.TextAttribute.UNDERLINE, java.awt.font.TextAttribute.UNDERLINE_ON)));
                place(submit, 48, 530, 464, 64, s);
                place(switchLink, 48, 628, 464, 34, s);
                place(footer, 48, 724, 464, 38, s);
            } else if (mode == Mode.RESET_EMAIL) {
                place(notice, 48, 360, 464, 110, s);
                notice.setFont(AuthStyle.font((float) (17 * s), false));
                place(submit, 48, 530, 464, 64, s);
                place(switchLink, 48, 628, 464, 34, s);
                place(footer, 48, 724, 464, 38, s);
            } else {
                place(submit, 48, 624, 464, 62, s);
                place(switchLink, 48, 704, 464, 34, s);
                place(footer, 48, 744, 464, 28, s);
                if (mode == Mode.RESET_PASSWORD) {
                    place(help, 48, 567, 464, 32, s);
                    help.setFont(AuthStyle.font((float) (16 * s), false));
                }
            }
            submit.setFont(AuthStyle.font((float) (22 * s), true));
            switchLink.setFont(AuthStyle.font((float) (16 * s), false));
            footer.setFont(AuthStyle.font((float) ((registering || mode == Mode.RESET_PASSWORD ? 14 : 17) * s), false));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            AuthStyle.quality(g);
            double s = getWidth() / 560.0;
            float radius = (float) (42 * s);
            g.setPaint(new GradientPaint(0, 0, new Color(8, 20, 37, 241), getWidth(), getHeight(), new Color(14, 12, 32, 242)));
            g.fill(new RoundRectangle2D.Double(2, 2, getWidth() - 4, getHeight() - 4, radius, radius));
            for (int width = 7; width >= 1; width -= 2) {
                int alpha = width == 1 ? 215 : 14;
                g.setPaint(new GradientPaint(0, 0, new Color(47, 228, 250, alpha), getWidth(), getHeight(), new Color(177, 103, 255, alpha)));
                g.setStroke(new BasicStroke((float) (width * s)));
                g.draw(new RoundRectangle2D.Double(2, 2, getWidth() - 5, getHeight() - 5, radius, radius));
            }
            if (!registering && mode != Mode.RESET_PASSWORD) {
                g.setColor(new Color(68, 84, 114, 165));
                g.setStroke(new BasicStroke(1));
                g.drawLine((int) (48 * s), (int) (710 * s), (int) (512 * s), (int) (710 * s));
            }
            g.dispose();
        }
    }

    private static void place(Component c, double x, double y, double width, double height, double scale) {
        c.setBounds((int) Math.round(x * scale), (int) Math.round(y * scale),
                (int) Math.round(width * scale), (int) Math.round(height * scale));
    }

    private static void transparent(JButton button) {
        button.setOpaque(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private static final class TabButton extends JButton {

        private final boolean selected;

        TabButton(String name, boolean selected) {
            super(name);
            this.selected = selected;
            transparent(this);
            getAccessibleContext().setAccessibleDescription(selected ? "Tab đang chọn" : "Chuyển màn hình");
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            AuthStyle.quality(g);
            g.setFont(getFont());
            g.setColor(selected ? new Color(210, 250, 255) : AuthStyle.MUTED);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (int) (getHeight() * .52));
            int y = getHeight() - 8;
            g.setColor(new Color(81, 84, 115, 170));
            g.fillRect(0, y, getWidth(), 1);
            if (selected || hasFocus()) {
                g.setColor(new Color(33, 225, 244, 35));
                g.fillRoundRect(0, y - 3, getWidth(), 8, 5, 5);
                g.setColor(AuthStyle.CYAN);
                g.fillRoundRect(0, y - 1, getWidth(), 3, 3, 3);
            }
            g.dispose();
        }
    }

    private static final class FieldShell extends JPanel {

        private final JTextField input;
        private final String hint, kind;
        private final JButton eye;

        FieldShell(JTextField input, String hint, String kind) {
            this.input = input;
            this.hint = hint;
            this.kind = kind;
            setOpaque(false);
            setLayout(null);
            input.setOpaque(false);
            input.setBorder(BorderFactory.createEmptyBorder());
            input.setForeground(new Color(225, 233, 246));
            input.setCaretColor(AuthStyle.CYAN);
            input.setSelectionColor(new Color(49, 79, 124));
            input.setSelectedTextColor(Color.WHITE);
            input.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    repaint();
                }

                public void focusLost(FocusEvent e) {
                    repaint();
                }
            });
            input.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    repaint();
                }

                public void removeUpdate(DocumentEvent e) {
                    repaint();
                }

                public void changedUpdate(DocumentEvent e) {
                    repaint();
                }
            });
            add(input);
            if (input instanceof JPasswordField) {
                JPasswordField password = (JPasswordField) input;
                password.setEchoChar('\u2022');
                eye = new JButton();
                transparent(eye);
                eye.setForeground(AuthStyle.MUTED);
                eye.setName("auth-password-eye");
                eye.setToolTipText("Hiện mật khẩu");
                eye.getAccessibleContext().setAccessibleName("Hiện mật khẩu");
                eye.addActionListener(e -> {
                    boolean hidden = password.getEchoChar() != 0;
                    password.setEchoChar(hidden ? (char) 0 : '\u2022');
                    String label = hidden ? "Ẩn mật khẩu" : "Hiện mật khẩu";
                    eye.setToolTipText(label);
                    eye.getAccessibleContext().setAccessibleName(label);
                    updateEye();
                });
                add(eye);
            } else {
                eye = null;
            }
        }

        private void updateEye() {
            if (eye != null) {
                eye.setIcon(AuthStyle.icon(((JPasswordField) input).getEchoChar() == 0 ? "eye-off" : "eye",
                        Math.max(14, (int) Math.round(24 * getHeight() / 56.0))));
            }
        }

        @Override
        public void doLayout() {
            double s = getHeight() / 56.0;
            input.setFont(AuthStyle.font((float) (18.5 * s), false));
            int lead = (int) Math.round(65 * s), tail = (int) Math.round((eye == null ? 16 : 56) * s);
            input.setBounds(lead, (int) (6 * s), Math.max(10, getWidth() - lead - tail), (int) (44 * s));
            if (eye != null) {
                eye.setBounds(getWidth() - (int) (52 * s), 0, (int) (44 * s), getHeight());
                updateEye();
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            AuthStyle.quality(g);
            double s = getHeight() / 56.0;
            int radius = Math.max(8, (int) (16 * s));
            g.setPaint(new GradientPaint(0, 0, new Color(21, 31, 48, 170), getWidth(), getHeight(), new Color(13, 22, 38, 155)));
            g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, radius, radius);
            g.setColor(input.hasFocus() ? AuthStyle.CYAN : new Color(102, 119, 147, 185));
            g.setStroke(new BasicStroke(input.hasFocus() ? 1.6f : 1f));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, radius, radius);
            setForeground(AuthStyle.MUTED);
            int size = Math.max(14, (int) Math.round(23 * s));
            AuthStyle.icon(kind, size).paintIcon(this, g, (int) (20 * s), (getHeight() - size) / 2);
            if (input.getDocument().getLength() == 0) {
                g.setFont(input.getFont());
                g.setColor(new Color(125, 139, 161));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(hint, (int) (65 * s), (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            }
            g.dispose();
        }
    }

    private static final class GlowButton extends JButton {

        GlowButton(String text) {
            super(text);
            transparent(this);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            AuthStyle.quality(g);
            float s = getHeight() / 64f;
            Color start = !isEnabled() ? new Color(59, 133, 155) : getModel().isPressed() ? new Color(17, 177, 211) : new Color(26, 220, 245);
            g.setPaint(new GradientPaint(0, 0, start, getWidth(), 0, isEnabled() ? new Color(52, 234, 245) : new Color(82, 161, 179)));
            g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, (int) (32 * s), (int) (32 * s));
            g.setColor(new Color(113, 247, 255, getModel().isRollover() ? 240 : 155));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, (int) (32 * s), (int) (32 * s));
            g.setFont(getFont());
            g.setColor(new Color(3, 29, 43));
            FontMetrics fm = g.getFontMetrics();
            int iconSize = (int) (25 * s), gap = (int) (18 * s);
            int x = (getWidth() - fm.stringWidth(getText()) - gap - iconSize) / 2;
            int baseline = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(getText(), x, baseline);
            setForeground(new Color(3, 29, 43));
            AuthStyle.icon("arrow", iconSize).paintIcon(this, g, x + fm.stringWidth(getText()) + gap, (getHeight() - iconSize) / 2);
            if (hasFocus()) {
                g.setColor(Color.WHITE);
                g.drawRoundRect(5, 5, getWidth() - 11, getHeight() - 11, (int) (14 * s), (int) (14 * s));
            }
            g.dispose();
        }
    }

    private static final class SwitchLink extends JButton {

        private final String prefix, link;

        SwitchLink(String prefix, String link) {
            super(prefix + link);
            this.prefix = prefix;
            this.link = link;
            transparent(this);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            AuthStyle.quality(g);
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            int total = fm.stringWidth(prefix + link), x = (getWidth() - total) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g.setColor(AuthStyle.MUTED);
            g.drawString(prefix, x, y);
            g.setColor(AuthStyle.PURPLE);
            g.drawString(link, x + fm.stringWidth(prefix), y);
            if (hasFocus() || getModel().isRollover()) {
                g.drawLine(x + fm.stringWidth(prefix), y + 3, x + total, y + 3);
            }
            g.setColor(new Color(63, 77, 104));
            g.drawLine(0, getHeight() / 2, Math.max(0, x - 14), getHeight() / 2);
            g.drawLine(Math.min(getWidth(), x + total + 14), getHeight() / 2, getWidth(), getHeight() / 2);
            g.dispose();
        }
    }
}
