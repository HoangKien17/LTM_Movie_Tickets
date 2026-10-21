package web;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import utils.PasswordUtil;

/** Personal details only; server account IDs and roles never enter this view. */
final class ProfileDialog extends JDialog {
    private final JTextField name = new JTextField(), email = new JTextField(), phone = new JTextField();
    private final JPasswordField current = new JPasswordField(), replacement = new JPasswordField(), confirm = new JPasswordField();
    private final JTextArea feedback = UiTheme.copy(" ", 13);
    private final JButton save = UiTheme.primary("Lưu thay đổi"), cancel = UiTheme.subtle("Hủy"), close = new JButton(HomeIcons.icon("close", 20));
    private final Consumer<Values> submit;
    private boolean saving;

    ProfileDialog(Window owner, String displayName, String address, String number, Consumer<Values> submit) {
        super(owner, "Thông tin cá nhân", ModalityType.DOCUMENT_MODAL);
        this.submit = submit;
        setName("profile-dialog"); setUndecorated(true); setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { dismiss(); } });
        name.setName("profile-name"); email.setName("profile-email"); phone.setName("profile-phone");
        current.setName("profile-current-password"); replacement.setName("profile-new-password"); confirm.setName("profile-confirm-password");
        name.setText(displayName); email.setText(address); phone.setText(number);

        JPanel root = new JPanel(new BorderLayout(24, 20)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create(); AuthStyle.quality(g);
                g.setPaint(new GradientPaint(0, 0, new Color(10, 25, 42), getWidth(), getHeight(), new Color(24, 15, 41)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setPaint(new GradientPaint(0, 0, UiTheme.PRIMARY, getWidth(), getHeight(), UiTheme.PURPLE));
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 26, 26); g.dispose();
            }
        };
        root.setBorder(new EmptyBorder(23, 26, 23, 26));
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false);
        JLabel brand = new JLabel("GALAXY CINEMA", UiTheme.orbitIcon(34), SwingConstants.LEFT);
        brand.setFont(AuthStyle.wordmark(16)); brand.setForeground(UiTheme.TEXT); brand.setIconTextGap(10);
        close.setName("profile-close"); close.setToolTipText("Đóng"); close.setForeground(UiTheme.MUTED);
        close.setFocusPainted(false); close.setBorderPainted(false);
        close.setBorder(new EmptyBorder(0, 6, 0, 0)); close.setContentAreaFilled(false); close.addActionListener(e -> dismiss());
        top.add(brand, BorderLayout.WEST); top.add(close, BorderLayout.EAST); root.add(top, BorderLayout.NORTH);

        JPanel columns = new JPanel(new BorderLayout(26, 0)); columns.setOpaque(false);
        JPanel identity = new JPanel(); identity.setOpaque(false); identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        identity.setPreferredSize(new Dimension(202, 450));
        JLabel avatar = new JLabel(HomeIcons.icon("user", 57)); avatar.setForeground(UiTheme.PRIMARY);
        avatar.setBorder(new EmptyBorder(12, 0, 18, 0)); identity.add(avatar);
        JTextArea greeting = UiTheme.copy(displayName, 25); greeting.setForeground(UiTheme.TEXT); greeting.setFont(UiTheme.font(25, Font.BOLD));
        greeting.setMaximumSize(new Dimension(202, 100)); greeting.setPreferredSize(new Dimension(202, 90)); identity.add(greeting);
        JTextArea description = UiTheme.copy("Không gian cá nhân của bạn.\n\nCập nhật thông tin để nhận vé và lời nhắc cho những suất chiếu tiếp theo.", 15);
        description.setMaximumSize(new Dimension(202, 200)); identity.add(description);
        for (Component c : identity.getComponents()) if (c instanceof JComponent) ((JComponent) c).setAlignmentX(LEFT_ALIGNMENT);
        columns.add(identity, BorderLayout.WEST);

        JPanel form = new JPanel(new GridBagLayout()); form.setOpaque(false);
        form.setPreferredSize(new Dimension(520,510));
        add(form, section("Thông tin cá nhân", "Thông tin liên hệ dùng cho tài khoản và vé điện tử."), 0, 0, 2);
        add(form, field("Họ và tên", name, "user"), 0, 1, 2);
        add(form, field("Email", email, "mail"), 0, 2, 1);
        add(form, field("Số điện thoại", phone, "phone"), 1, 2, 1);
        add(form, section("Bảo mật tài khoản", "Nhập mật khẩu hiện tại để lưu. Để trống mật khẩu mới nếu giữ nguyên."), 0, 3, 2);
        add(form, field("Mật khẩu hiện tại", current, "lock"), 0, 4, 2);
        add(form, field("Mật khẩu mới", replacement, "lock"), 0, 5, 1);
        add(form, field("Nhập lại mật khẩu mới", confirm, "lock"), 1, 5, 1);
        JScrollPane scroll = UiTheme.scrollContent(form); scroll.setOpaque(false); scroll.getViewport().setOpaque(false);
        columns.add(scroll, BorderLayout.CENTER); root.add(columns, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(12, 10)); bottom.setOpaque(false);
        feedback.setName("profile-feedback"); feedback.setForeground(UiTheme.RED); feedback.setRows(2);
        feedback.setPreferredSize(new Dimension(450, 38)); bottom.add(feedback, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); actions.setOpaque(false);
        save.setName("profile-save"); cancel.setName("profile-cancel");
        save.setPreferredSize(new Dimension(180, 47)); save.setFont(UiTheme.font(16, Font.BOLD));
        cancel.setPreferredSize(new Dimension(80, 47)); cancel.setFont(UiTheme.font(15, Font.PLAIN));
        save.addActionListener(e -> validateAndSubmit()); cancel.addActionListener(e -> dismiss());
        actions.add(cancel); actions.add(save); bottom.add(actions, BorderLayout.EAST); root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root); getRootPane().setDefaultButton(save);
        getRootPane().registerKeyboardAction(e -> dismiss(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        name.addActionListener(e -> email.requestFocusInWindow()); email.addActionListener(e -> phone.requestFocusInWindow());
        phone.addActionListener(e -> current.requestFocusInWindow()); current.addActionListener(e -> validateAndSubmit());
        replacement.addActionListener(e -> confirm.requestFocusInWindow()); confirm.addActionListener(e -> validateAndSubmit());
        Rectangle bounds = owner == null ? GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds()
                : owner.getGraphicsConfiguration().getBounds();
        setSize(Math.min(950, Math.max(760, bounds.width - 60)), Math.min(710, Math.max(530, bounds.height - 80)));
        setResizable(false); setLocationRelativeTo(owner);
        try { setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 26, 26)); }
        catch (UnsupportedOperationException ignored) { }
        MouseAdapter drag = new MouseAdapter() {
            private Point start;
            public void mousePressed(MouseEvent e) { start = e.getLocationOnScreen(); }
            public void mouseDragged(MouseEvent e) { if (start != null) { Point now = e.getLocationOnScreen(); setLocation(getX()+now.x-start.x,getY()+now.y-start.y); start=now; } }
        };
        top.addMouseListener(drag); top.addMouseMotionListener(drag); brand.addMouseListener(drag); brand.addMouseMotionListener(drag);
    }

    void setSaving(boolean value) {
        saving = value; save.setEnabled(!value); cancel.setEnabled(!value); close.setEnabled(!value);
        save.setText(value ? "Đang lưu..." : "Lưu thay đổi");
        for (JTextField input : new JTextField[]{name,email,phone,current,replacement,confirm}) input.setEditable(!value);
    }
    void error(String message) { feedback.setText(message); }
    private void dismiss() { if (!saving) dispose(); }
    @Override public void dispose() {
        current.setText(""); replacement.setText(""); confirm.setText(""); super.dispose();
    }
    private void validateAndSubmit() {
        if (saving) return;
        String n = name.getText().trim(), e = email.getText().trim(), p = phone.getText().trim();
        String old = new String(current.getPassword()), fresh = new String(replacement.getPassword());
        if (n.isEmpty() || n.length() > 100) { error("Họ tên phải có từ 1 đến 100 ký tự."); name.requestFocusInWindow(); return; }
        if (e.length() > 100 || !PasswordUtil.isValidEmail(e)) { error("Hãy nhập địa chỉ email hợp lệ."); email.requestFocusInWindow(); return; }
        if (!PasswordUtil.isValidPhone(p)) { error("Số điện thoại phải có 9–11 chữ số."); phone.requestFocusInWindow(); return; }
        if (old.isEmpty()) { error("Hãy nhập mật khẩu hiện tại để lưu thay đổi."); current.requestFocusInWindow(); return; }
        if (!fresh.isEmpty() && (fresh.length() < 6 || fresh.length() > 128)) { error("Mật khẩu mới phải có từ 6 đến 128 ký tự."); return; }
        if (!fresh.equals(new String(confirm.getPassword()))) { error("Mật khẩu mới nhập lại không khớp."); confirm.requestFocusInWindow(); return; }
        error(" "); submit.accept(new Values(n,e,p,old,fresh));
    }

    private static JPanel section(String title, String subtitle) {
        JPanel panel = new JPanel(new BorderLayout(0, 5)); panel.setOpaque(false);
        JLabel heading = new JLabel(title); heading.setFont(UiTheme.font(23, Font.BOLD)); heading.setForeground(UiTheme.TEXT);
        JTextArea copy = UiTheme.copy(subtitle, 13); copy.setRows(2);
        panel.add(heading, BorderLayout.NORTH); panel.add(copy, BorderLayout.CENTER); return panel;
    }
    private static void add(JPanel form, JComponent component, int x, int y, int span) {
        GridBagConstraints c = new GridBagConstraints(); c.gridx=x; c.gridy=y; c.gridwidth=span;
        c.weightx=1; c.fill=GridBagConstraints.HORIZONTAL; c.anchor=GridBagConstraints.NORTHWEST;
        c.insets=new Insets(y==0?0:12,x==0?0:8,0,span==1&&x==0?8:0); form.add(component,c);
    }
    private static JPanel field(String caption, JTextField input, String kind) {
        JPanel panel = new JPanel(new BorderLayout(0, 7)); panel.setOpaque(false);
        JLabel label = new JLabel(caption); label.setFont(UiTheme.font(14, Font.PLAIN)); label.setForeground(UiTheme.TEXT); label.setLabelFor(input);
        input.getAccessibleContext().setAccessibleName(caption); panel.add(label,BorderLayout.NORTH);
        JPanel shell = new JPanel(new BorderLayout(10,0)) {
            protected void paintComponent(Graphics graphics) {
                Graphics2D g=(Graphics2D)graphics.create(); AuthStyle.quality(g);
                g.setColor(new Color(10,19,33)); g.fillRoundRect(1,1,getWidth()-2,getHeight()-2,14,14);
                g.setColor(input.hasFocus()?UiTheme.PRIMARY:UiTheme.LINE); g.drawRoundRect(1,1,getWidth()-3,getHeight()-3,14,14); g.dispose();
            }
        };
        shell.setOpaque(false); shell.setBorder(new EmptyBorder(0,13,0,12)); shell.setPreferredSize(new Dimension(180,48));
        shell.setMinimumSize(new Dimension(0,48)); panel.setMinimumSize(new Dimension(0,72));
        JLabel icon = new JLabel(AuthStyle.icon(kind,19)); icon.setForeground(UiTheme.MUTED); shell.add(icon,BorderLayout.WEST);
        input.setOpaque(false); input.setBorder(BorderFactory.createEmptyBorder()); input.setFont(UiTheme.font(15,Font.PLAIN));
        input.setForeground(UiTheme.TEXT); input.setCaretColor(UiTheme.PRIMARY); input.setSelectionColor(UiTheme.PRIMARY_PALE);
        input.addFocusListener(new FocusAdapter(){public void focusGained(FocusEvent e){shell.repaint();}public void focusLost(FocusEvent e){shell.repaint();}});
        shell.add(input,BorderLayout.CENTER);
        if (input instanceof JPasswordField) {
            JPasswordField password=(JPasswordField)input; password.setEchoChar('\u2022');
            JButton eye=new JButton(AuthStyle.icon("eye",19)); eye.setName(input.getName()+"-eye"); eye.setForeground(UiTheme.MUTED);
            eye.setBorder(BorderFactory.createEmptyBorder()); eye.setContentAreaFilled(false); eye.setToolTipText("Hiện mật khẩu");
            eye.addActionListener(event->{boolean hidden=password.getEchoChar()!=0;password.setEchoChar(hidden?(char)0:'\u2022');eye.setIcon(AuthStyle.icon(hidden?"eye-off":"eye",19));eye.setToolTipText(hidden?"Ẩn mật khẩu":"Hiện mật khẩu");});
            shell.add(eye,BorderLayout.EAST);
        }
        panel.add(shell,BorderLayout.CENTER); return panel;
    }

    static final class Values {
        final String name,email,phone,currentPassword,newPassword;
        Values(String name,String email,String phone,String currentPassword,String newPassword) {
            this.name=name;this.email=email;this.phone=phone;this.currentPassword=currentPassword;this.newPassword=newPassword;
        }
    }
}
