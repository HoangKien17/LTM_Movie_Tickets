package web;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.table.DefaultTableModel;
import protocol.Protocol;

/**
 * Giao diện Swing Bước 6. Mọi thao tác dữ liệu đều đi qua MovieClient.
 */
public final class BasicClientUI extends JFrame {

    private static final Color INK = new Color(25, 28, 47);
    private static final Color MUTED = new Color(111, 116, 137);
    private static final Color PURPLE = new Color(77, 49, 174);
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final JLabel status = new JLabel("Sẵn sàng");
    private final JTextField loginName = new JTextField(22);
    private final JPasswordField loginPassword = new JPasswordField(22);
    private final JTextField registerName = new JTextField(22);
    private final JTextField registerEmail = new JTextField(22);
    private final JTextField registerPhone = new JTextField(22);
    private final JPasswordField registerPassword = new JPasswordField(22);
    private final JLabel welcome = new JLabel();
    private final JTabbedPane tabs = new JTabbedPane();

    private final DefaultTableModel movieModel = model("Mã", "Tên phim", "Thể loại", "Phút", "Khởi chiếu");
    private final JTable movieTable = new JTable(movieModel);
    private final JTextArea description = new JTextArea(4, 30);
    private final List<String> descriptions = new ArrayList<>();

    private final DefaultTableModel showModel = model("Mã", "Phim", "Phòng", "Bắt đầu", "Giá vé");
    private final JTable showTable = new JTable(showModel);
    private final JPanel seatPanel = new JPanel(new GridLayout(0, 6, 8, 8));
    private final JLabel seatTitle = new JLabel("Chọn một suất chiếu để xem ghế");
    private final List<JToggleButton> seatButtons = new ArrayList<>();

    private final DefaultTableModel bookingModel = model("Mã đơn", "Phim", "Phòng",
            "Bắt đầu", "Ghế", "Tổng tiền", "Trạng thái");
    private final JTable bookingTable = new JTable(bookingModel);
    private final JTextArea adminOutput = new JTextArea();

    private MovieClient client;
    private boolean busy;

    public BasicClientUI() {
        super("LTM Movie Tickets");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1000, 680);
        setMinimumSize(new Dimension(900, 680));
        setLocationRelativeTo(null);
        cards.add(buildLogin(), "LOGIN");
        cards.add(buildRegister(), "REGISTER");
        cards.add(buildMain(), "MAIN");
        add(cards, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        cardLayout.show(cards, "LOGIN");
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                closeClient();
            }
        });
    }

    private JPanel buildLogin() {
        JPanel form = authForm();
        form.add(authField("Email hoặc số điện thoại", loginName));
        form.add(authField("Mật khẩu", passwordBox(loginPassword)));
        loginPassword.addActionListener(event -> login());
        return authScreen("Chào mừng trở lại", "Đặt vé nhanh, dễ dàng.", form,
                "Đăng nhập", this::login, "Chưa có tài khoản? Đăng ký ngay",
                () -> cardLayout.show(cards, "REGISTER"), 430);
    }

    private JPanel buildRegister() {
        JPanel form = authForm();
        form.add(authField("Họ và tên", registerName));
        form.add(authField("Email", registerEmail));
        form.add(authField("Số điện thoại", registerPhone));
        form.add(authField("Mật khẩu (ít nhất 6 ký tự)", passwordBox(registerPassword)));
        registerPassword.addActionListener(event -> register());
        return authScreen("Tạo tài khoản", "Bắt đầu hành trình điện ảnh của bạn.", form,
                "Đăng ký", this::register, "Đã có tài khoản? Đăng nhập",
                () -> cardLayout.show(cards, "LOGIN"), 565);
    }

    private JPanel authScreen(String heading, String subtitle, JPanel form,
            String actionText, Runnable action, String switchText,
            Runnable switchAction, int cardHeight) {
        JPanel screen = new JPanel(new GridLayout(1, 2));
        screen.add(new CinemaPanel());

        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(new Color(250, 250, 252));
        JPanel card = new AuthCard();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(31, 34, 28, 34));
        card.setPreferredSize(new Dimension(390, cardHeight));
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);

        JLabel brand = authLabel("LTM MOVIE TICKETS", 12, Font.BOLD, PURPLE);
        JLabel title = authLabel(heading, 25, Font.BOLD, INK);
        JLabel hint = authLabel(subtitle, 12, Font.PLAIN, MUTED);
        int row = 0;
        addAuthRow(content, brand, row++, 13);
        addAuthRow(content, title, row++, 4);
        addAuthRow(content, hint, row++, 23);
        addAuthRow(content, form, row++, 20);

        JButton submit = new PurpleButton(actionText);
        submit.setAlignmentX(Component.LEFT_ALIGNMENT);
        submit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        submit.setPreferredSize(new Dimension(320, 44));
        submit.addActionListener(event -> action.run());
        addAuthRow(content, submit, row++, 13);

        JButton switchPage = new JButton(switchText);
        switchPage.setAlignmentX(Component.CENTER_ALIGNMENT);
        switchPage.setFont(new Font("SansSerif", Font.PLAIN, 12));
        switchPage.setForeground(PURPLE);
        switchPage.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        switchPage.setContentAreaFilled(false);
        switchPage.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        switchPage.addActionListener(event -> switchAction.run());
        addAuthRow(content, switchPage, row, 0);
        card.add(content, BorderLayout.CENTER);
        right.add(card);
        screen.add(right);
        return screen;
    }

    private static void addAuthRow(JPanel content, JComponent child, int row, int gapBelow) {
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 1;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, gapBelow, 0);
        content.add(child, gbc);
    }

    private static JPanel authForm() {
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 13));
        form.setOpaque(false);
        return form;
    }

    private static JPanel authField(String caption, JComponent input) {
        JPanel field = new JPanel(new BorderLayout(0, 6));
        field.setOpaque(false);
        JLabel label = authLabel(caption, 12, Font.BOLD, INK);
        field.add(label, BorderLayout.NORTH);
        if (input instanceof JTextField) {
            styleInput((JTextField) input);
        }
        input.setPreferredSize(new Dimension(320, 43));
        field.add(input, BorderLayout.CENTER);
        return field;
    }

    private static JLabel authLabel(String value, int size, int weight, Color color) {
        JLabel label = new JLabel(value);
        label.setFont(new Font("SansSerif", weight, size));
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static void styleInput(JTextField input) {
        input.setFont(new Font("SansSerif", Font.PLAIN, 14));
        input.setForeground(INK);
        input.setBackground(Color.WHITE);
        input.setCaretColor(PURPLE);
        input.setBorder(new RoundedInputBorder());
    }

    private static JPanel passwordBox(JPasswordField password) {
        JPanel box = new JPanel(new BorderLayout());
        box.setOpaque(false);
        box.setBorder(new RoundedInputBorder());
        password.setFont(new Font("SansSerif", Font.PLAIN, 14));
        password.setForeground(INK);
        password.setOpaque(false);
        password.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 2));
        char hidden = password.getEchoChar();
        JButton toggle = new JButton("Hiện");
        toggle.setFont(new Font("SansSerif", Font.PLAIN, 11));
        toggle.setForeground(PURPLE);
        toggle.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 10));
        toggle.setContentAreaFilled(false);
        toggle.addActionListener(event -> {
            boolean visible = password.getEchoChar() != 0;
            password.setEchoChar(visible ? (char) 0 : hidden);
            toggle.setText(visible ? "Ẩn" : "Hiện");
        });
        box.add(password, BorderLayout.CENTER);
        box.add(toggle, BorderLayout.EAST);
        return box;
    }

    private static final class RoundedInputBorder extends AbstractBorder {

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(10, 13, 10, 13);
        }

        @Override
        public void paintBorder(Component c, Graphics graphics,
                int x, int y, int width, int height) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(215, 218, 228));
            g.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f,
                    width - 1.5f, height - 1.5f, 10, 10));
            g.dispose();
        }
    }

    private static final class PurpleButton extends JButton {

        private PurpleButton(String text) {
            super(text);
            setForeground(Color.WHITE);
            setFont(new Font("SansSerif", Font.BOLD, 13));
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, new Color(98, 66, 205),
                    getWidth(), getHeight(), new Color(60, 36, 151)));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 11, 11);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class AuthCard extends JPanel {

        private AuthCard() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(32, 34, 55, 20));
            g.fillRoundRect(5, 8, getWidth() - 10, getHeight() - 10, 15, 15);
            g.setColor(Color.WHITE);
            g.fillRoundRect(1, 1, getWidth() - 8, getHeight() - 10, 15, 15);
            g.setColor(new Color(230, 231, 237));
            g.drawRoundRect(1, 1, getWidth() - 8, getHeight() - 10, 15, 15);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class CinemaPanel extends JPanel {

        private CinemaPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g.setPaint(new GradientPaint(0, 0, new Color(13, 14, 43),
                    w, h, new Color(6, 34, 61)));
            g.fillRect(0, 0, w, h);

            g.setFont(new Font("SansSerif", Font.BOLD, 24));
            g.setColor(new Color(174, 139, 227));
            g.drawString("LTM MOVIE TICKETS", 36, 69);
            g.setFont(new Font("SansSerif", Font.BOLD, 30));
            g.setColor(Color.WHITE);
            g.drawString("Mỗi suất chiếu", 36, 167);
            g.drawString("một trải nghiệm mới.", 36, 206);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.setColor(new Color(194, 202, 222));
            g.drawString("Khám phá phim hay, chọn chỗ ngồi yêu thích", 36, 240);
            g.drawString("và giữ vé chỉ trong vài bước.", 36, 261);

            int cx = Math.round(w * 0.38f);
            int cy = Math.round(h * 0.70f);
            int r = Math.min(89, Math.max(61, w / 6));
            g.setPaint(new GradientPaint(cx - r, cy - r, new Color(255, 216, 135),
                    cx + r, cy + r, new Color(165, 95, 49)));
            g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
            g.setColor(new Color(94, 49, 48));
            g.setStroke(new java.awt.BasicStroke(5));
            g.draw(new Ellipse2D.Float(cx - r + 8, cy - r + 8, r * 2 - 16, r * 2 - 16));
            for (int i = 0; i < 5; i++) {
                double angle = i * Math.PI * 2 / 5 - Math.PI / 2;
                int holeX = cx + (int) (r * 0.52 * Math.cos(angle));
                int holeY = cy + (int) (r * 0.52 * Math.sin(angle));
                g.setColor(new Color(28, 33, 62));
                g.fill(new Ellipse2D.Float(holeX - 15, holeY - 20, 30, 40));
            }
            g.setColor(new Color(91, 55, 56));
            g.fill(new Ellipse2D.Float(cx - 12, cy - 12, 24, 24));

            CubicCurve2D film = new CubicCurve2D.Float(cx + r - 10, cy + r / 3,
                    cx + r + 30, cy + r, w - 90, cy - r / 2,
                    w - 28, cy + r / 2);
            g.setColor(new Color(246, 187, 93));
            g.setStroke(new java.awt.BasicStroke(24, java.awt.BasicStroke.CAP_ROUND,
                    java.awt.BasicStroke.JOIN_ROUND));
            g.draw(film);
            g.setColor(new Color(32, 37, 58));
            g.setStroke(new java.awt.BasicStroke(15, java.awt.BasicStroke.CAP_ROUND,
                    java.awt.BasicStroke.JOIN_ROUND));
            g.draw(film);
            g.setColor(new Color(255, 218, 145));
            for (int i = 0; i < 16; i++) {
                int x = 46 + (i * 37) % Math.max(100, w - 75);
                int y = 294 + (i * 59) % Math.max(80, h - 380);
                g.fillOval(x, y, i % 3 == 0 ? 4 : 2, i % 3 == 0 ? 4 : 2);
            }
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.setColor(new Color(173, 181, 207));
            g.drawString("KHÁM PHÁ  ·  ĐẶT VÉ  ·  THƯỞNG THỨC", 36, h - 31);
            g.dispose();
        }
    }

    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout(8, 8));
        JPanel top = new JPanel(new BorderLayout());
        top.add(welcome, BorderLayout.CENTER);
        top.add(button("Đăng xuất", this::logout), BorderLayout.EAST);
        tabs.addTab("Phim", buildMovies());
        tabs.addTab("Suất chiếu và ghế", buildShows());
        tabs.addTab("Vé của tôi", buildBookings());
        tabs.addTab("Quản trị", buildAdmin());
        tabs.setEnabledAt(3, false);
        main.add(top, BorderLayout.NORTH);
        main.add(tabs, BorderLayout.CENTER);
        return main;
    }

    private JPanel buildMovies() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(button("Làm mới phim", this::loadMovies));
        actions.add(button("Xem suất chiếu", () -> {
            int row = movieTable.getSelectedRow();
            if (row < 0) {
                showError("Hãy chọn một phim");
                return;
            }
            int id = Integer.parseInt(movieModel.getValueAt(
                    movieTable.convertRowIndexToModel(row), 0).toString());
            tabs.setSelectedIndex(1);
            loadShows(id);
        }));
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        movieTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        movieTable.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }
            int row = movieTable.getSelectedRow();
            int index = row < 0 ? -1 : movieTable.convertRowIndexToModel(row);
            description.setText(index >= 0 && index < descriptions.size()
                    ? descriptions.get(index) : "");
        });
        panel.add(actions, BorderLayout.NORTH);
        panel.add(new JScrollPane(movieTable), BorderLayout.CENTER);
        panel.add(new JScrollPane(description), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildShows() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(button("Tất cả suất chiếu", () -> loadShows(0)));
        actions.add(button("Tải lại ghế", this::loadSeats));
        actions.add(button("Đặt ghế đã chọn", this::book));
        panel.add(actions, BorderLayout.NORTH);
        showTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        showTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && showTable.getSelectedRow() >= 0) {
                loadSeats();
            }
        });
        JPanel lower = new JPanel(new BorderLayout(5, 5));
        lower.add(seatTitle, BorderLayout.NORTH);
        lower.add(new JScrollPane(seatPanel), BorderLayout.CENTER);
        javax.swing.JSplitPane split = new javax.swing.JSplitPane(
                javax.swing.JSplitPane.VERTICAL_SPLIT, new JScrollPane(showTable), lower);
        split.setResizeWeight(0.45);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBookings() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(button("Làm mới vé", this::loadBookings));
        actions.add(button("Chi tiết đơn", this::ticketDetail));
        actions.add(button("Hủy đơn đã chọn", this::cancel));
        panel.add(actions, BorderLayout.NORTH);
        bookingTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(bookingTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildAdmin() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel actions = new JPanel(new GridLayout(0, 4, 6, 6));
        actions.add(button("Thêm phim", this::adminAddMovie));
        actions.add(button("Sửa phim", this::adminUpdateMovie));
        actions.add(button("Xóa phim", () -> adminDelete("ADMIN_DELETE_MOVIE", "Mã phim")));
        actions.add(button("Thêm phòng", this::adminAddRoom));
        actions.add(button("Sửa tên phòng", this::adminUpdateRoom));
        actions.add(button("Danh sách phòng", () -> adminList("ADMIN_GET_ROOMS", "ROOMS", "ROOM")));
        actions.add(button("Xóa phòng", () -> adminDelete("ADMIN_DELETE_ROOM", "Mã phòng")));
        actions.add(button("Thêm suất chiếu", this::adminAddShow));
        actions.add(button("Sửa suất chiếu", this::adminUpdateShow));
        actions.add(button("Danh sách suất", () -> adminList("ADMIN_GET_SHOWS", "SHOWS", "SHOW")));
        actions.add(button("Xóa suất chiếu", () -> adminDelete("ADMIN_DELETE_SHOW", "Mã suất chiếu")));
        actions.add(button("Người dùng", () -> adminList("ADMIN_GET_USERS", "USERS", "USER")));
        actions.add(button("Tất cả đơn vé", () -> adminList("ADMIN_GET_BOOKINGS", "BOOKINGS", "BOOKING")));
        actions.add(button("Hủy đơn vé", this::adminCancel));
        actions.add(button("Thống kê", () -> adminList("ADMIN_GET_STATS", "STATS", "STAT")));
        adminOutput.setEditable(false);
        panel.add(actions, BorderLayout.NORTH);
        panel.add(new JScrollPane(adminOutput), BorderLayout.CENTER);
        return panel;
    }

    private void register() {
        String email = registerEmail.getText().trim();
        String request = "REGISTER;" + enc(registerName.getText().trim()) + ";"
                + enc(email) + ";" + enc(registerPhone.getText().trim()) + ";"
                + enc(new String(registerPassword.getPassword()));
        send(request, lines -> {
            if (!isOk(lines, "REGISTER")) {
                return;
            }
            loginName.setText(email);
            registerPassword.setText("");
            cardLayout.show(cards, "LOGIN");
            JOptionPane.showMessageDialog(this, "Đăng ký thành công. Hãy đăng nhập.");
        });
    }

    private void login() {
        String request = "LOGIN;" + enc(loginName.getText().trim()) + ";"
                + enc(new String(loginPassword.getPassword()));
        send(request, lines -> {
            if (!isOk(lines, "LOGIN")) {
                return;
            }
            String[] p = lines.get(0).split(";", -1);
            if (p.length != 6) {
                showError("Phản hồi đăng nhập không hợp lệ");
                return;
            }
            welcome.setText("Xin chào, " + dec(p[3]));
            tabs.setEnabledAt(3, "admin".equals(dec(p[5])));
            loginPassword.setText("");
            cardLayout.show(cards, "MAIN");
            tabs.setSelectedIndex(0);
            loadMovies();
        });
    }

    private void logout() {
        send("LOGOUT", lines -> {
            if (!isOk(lines, "LOGOUT")) {
                return;
            }
            closeClient();
            movieModel.setRowCount(0);
            showModel.setRowCount(0);
            bookingModel.setRowCount(0);
            seatPanel.removeAll();
            adminOutput.setText("");
            cardLayout.show(cards, "LOGIN");
            status.setText("Đã đăng xuất");
        });
    }

    private void loadMovies() {
        send("GET_MOVIES", lines -> {
            if (!isOk(lines, "MOVIES")) {
                return;
            }
            movieModel.setRowCount(0);
            descriptions.clear();
            description.setText("");
            for (int i = 1; i < lines.size(); i++) {
                String[] p = lines.get(i).split(";", -1);
                if (p.length != 8 || !"MOVIE".equals(p[0])) {
                    showError("Dữ liệu phim không hợp lệ");
                    return;
                }
                movieModel.addRow(new Object[]{p[1], dec(p[2]), dec(p[3]), p[4], p[5]});
                descriptions.add(dec(p[6]));
            }
            status.setText("Đã tải " + movieModel.getRowCount() + " phim");
        });
    }

    private void loadShows(int movieId) {
        send("GET_SHOWS;" + movieId, lines -> {
            List<String[]> data = rows(lines, "SHOWS", "SHOW", 6);
            if (data == null) {
                return;
            }
            showModel.setRowCount(0);
            seatPanel.removeAll();
            seatTitle.setText("Chọn một suất chiếu để xem ghế");
            for (String[] row : data) {
                showModel.addRow(new Object[]{row[0], row[2], row[3], row[4], row[5]});
            }
            seatPanel.revalidate();
            seatPanel.repaint();
            status.setText("Đã tải " + data.size() + " suất chiếu");
        });
    }

    private int selectedShowId() {
        int row = showTable.getSelectedRow();
        if (row < 0) {
            return 0;
        }
        return Integer.parseInt(showModel.getValueAt(showTable.convertRowIndexToModel(row), 0).toString());
    }

    private void loadSeats() {
        int showId = selectedShowId();
        if (showId == 0) {
            showError("Hãy chọn một suất chiếu");
            return;
        }
        send("GET_SEAT_MAP;" + showId, lines -> {
            List<String[]> data = rows(lines, "SEATS", "SEAT", 2);
            if (data == null || selectedShowId() != showId) {
                return;
            }
            seatPanel.removeAll();
            seatButtons.clear();
            seatTitle.setText("Suất " + showId + " — chọn các ghế còn trống");
            for (String[] row : data) {
                JToggleButton seat = new JToggleButton(row[0]);
                boolean free = "free".equals(row[1]);
                seat.setEnabled(free);
                if (!free) {
                    seat.setText(row[0] + " (đã đặt)");
                }
                seatPanel.add(seat);
                if (free) {
                    seatButtons.add(seat);
                }
            }
            seatPanel.revalidate();
            seatPanel.repaint();
            status.setText("Đã tải sơ đồ ghế");
        });
    }

    private void book() {
        int showId = selectedShowId();
        if (showId == 0) {
            showError("Hãy chọn một suất chiếu");
            return;
        }
        List<String> chosen = new ArrayList<>();
        for (JToggleButton button : seatButtons) {
            if (button.isSelected()) {
                chosen.add(button.getText());
            }
        }
        if (chosen.isEmpty()) {
            showError("Hãy chọn ít nhất một ghế");
            return;
        }
        String request = "BOOK_TICKET;" + showId + ";" + enc(String.join(",", chosen));
        send(request, lines -> {
            if (!isOk(lines, "BOOKED")) {
                loadSeats();
                return;
            }
            JOptionPane.showMessageDialog(this, "Đặt vé thành công. " + lines.get(0));
            loadSeats();
        });
    }

    private void loadBookings() {
        send("GET_BOOKINGS", lines -> {
            List<String[]> data = rows(lines, "BOOKINGS", "BOOKING", 8);
            if (data == null) {
                return;
            }
            bookingModel.setRowCount(0);
            for (String[] row : data) {
                bookingModel.addRow(new Object[]{row[0], row[2], row[3], row[4],
                    row[7], row[5], row[6]});
            }
            status.setText("Đã tải " + data.size() + " đơn vé");
        });
    }

    private void cancel() {
        int row = bookingTable.getSelectedRow();
        if (row < 0) {
            showError("Hãy chọn một đơn vé");
            return;
        }
        String id = bookingModel.getValueAt(bookingTable.convertRowIndexToModel(row), 0).toString();
        int choice = JOptionPane.showConfirmDialog(this, "Hủy toàn bộ đơn vé #" + id + "?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        send("CANCEL_TICKET;" + id, lines -> {
            if (!isOk(lines, "CANCELLED")) {
                return;
            }
            loadBookings();
            JOptionPane.showMessageDialog(this, "Đã hủy đơn vé #" + id);
        });
    }

    private void ticketDetail() {
        int row = bookingTable.getSelectedRow();
        if (row < 0) {
            showError("Hãy chọn một đơn vé");
            return;
        }
        String id = bookingModel.getValueAt(bookingTable.convertRowIndexToModel(row), 0).toString();
        send("GET_TICKET;" + id, lines -> {
            List<String[]> data = rows(lines, "BOOKINGS", "BOOKING", 8);
            if (data == null || data.isEmpty()) {
                return;
            }
            String[] b = data.get(0);
            JOptionPane.showMessageDialog(this,
                    "Đơn #" + b[0] + "\nPhim: " + b[2] + "\nPhòng: " + b[3]
                    + "\nBắt đầu: " + b[4] + "\nGhế: " + b[7]
                    + "\nTổng tiền: " + b[5] + "\nTrạng thái: " + b[6]);
        });
    }

    private void adminAddMovie() {
        String[] v = prompt("Tên phim", "Thể loại", "Thời lượng (phút)", "Mô tả");
        if (v == null) {
            return;
        }
        send("ADMIN_ADD_MOVIE;" + enc(v[0]) + ";" + enc(v[1]) + ";" + v[2] + ";" + enc(v[3]),
                lines -> {
                    if (isOk(lines, "ADDED")) {
                        adminOutput.setText("Đã thêm phim: " + lines.get(0));
                        loadMovies();
                    }
                });
    }

    private void adminUpdateMovie() {
        String[] v = prompt("Mã phim", "Tên phim mới", "Thể loại", "Thời lượng (phút)", "Mô tả");
        if (v == null) {
            return;
        }
        send("ADMIN_UPDATE_MOVIE;" + v[0] + ";" + enc(v[1]) + ";" + enc(v[2])
                + ";" + v[3] + ";" + enc(v[4]),
                lines -> {
                    if (isOk(lines, "UPDATED")) {
                        adminOutput.setText("Đã sửa phim");
                        loadMovies();
                    }
                });
    }

    private void adminAddRoom() {
        String[] v = prompt("Tên phòng", "Số hàng ghế (1–26)", "Số ghế mỗi hàng (1–99)");
        if (v == null) {
            return;
        }
        send("ADMIN_ADD_ROOM;" + enc(v[0]) + ";" + v[1] + ";" + v[2],
                lines -> {
                    if (isOk(lines, "ADDED")) {
                        adminOutput.setText("Đã thêm phòng: " + lines.get(0));

                    }
                });
    }

    private void adminUpdateRoom() {
        String[] v = prompt("Mã phòng", "Tên phòng mới");
        if (v == null) {
            return;
        }
        send("ADMIN_UPDATE_ROOM;" + v[0] + ";" + enc(v[1]),
                lines -> {
                    if (isOk(lines, "UPDATED")) {
                        adminOutput.setText("Đã sửa tên phòng");

                    }
                });
    }

    private void adminAddShow() {
        String[] v = prompt("Mã phim", "Mã phòng", "Bắt đầu (yyyy-MM-ddTHH:mm)", "Giá vé");
        if (v == null) {
            return;
        }
        send("ADMIN_ADD_SHOW;" + String.join(";", v),
                lines -> {
                    if (isOk(lines, "ADDED")) {
                        adminOutput.setText("Đã thêm suất chiếu: " + lines.get(0));
                        loadShows(0);
                    }
                });
    }

    private void adminUpdateShow() {
        String[] v = prompt("Mã suất chiếu", "Mã phim", "Mã phòng",
                "Bắt đầu (yyyy-MM-ddTHH:mm)", "Giá vé");
        if (v == null) {
            return;
        }
        send("ADMIN_UPDATE_SHOW;" + String.join(";", v), lines -> {
            if (isOk(lines, "UPDATED")) {
                adminOutput.setText("Đã sửa suất chiếu");
                loadShows(0);
            }
        });
    }

    private void adminCancel() {
        String id = JOptionPane.showInputDialog(this, "Mã đơn vé cần hủy");
        if (id == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Hủy đơn vé #" + id + "?",
                "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        send("ADMIN_CANCEL_TICKET;" + id.trim(), lines -> {
            if (isOk(lines, "CANCELLED")) {
                adminOutput.setText("Đã hủy đơn vé #" + id);
            }
        });
    }

    private void adminDelete(String command, String label) {
        String id = JOptionPane.showInputDialog(this, label);
        if (id == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Xóa " + label.toLowerCase()
                + " " + id + "?", "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        send(command + ";" + id.trim(), lines -> {
            if (!isOk(lines, "DELETED")) {
                return;
            }
            adminOutput.setText("Đã xóa " + label.toLowerCase() + " " + id);
            loadMovies();
        });
    }

    private void adminList(String command, String kind, String tag) {
        send(command, lines -> {
            if (lines.isEmpty() || lines.get(0).startsWith("ERR;")) {
                isOk(lines, kind);
                return;
            }
            StringBuilder text = new StringBuilder(kind).append('\n');
            if ("STATS".equals(kind) && lines.size() == 2) {
                String[] stat = lines.get(1).split(";", -1);
                if (stat.length == 3 && "STAT".equals(stat[0])) {
                    adminOutput.setText("Tổng số đơn: " + dec(stat[1])
                            + "\nDoanh thu đơn còn hiệu lực: " + dec(stat[2]));
                    return;
                }
            }
            for (int i = 1; i < lines.size(); i++) {
                String[] p = lines.get(i).split(";", -1);
                if (!tag.equals(p[0])) {
                    showError("Phản hồi quản trị không hợp lệ");
                    return;
                }
                for (int j = 1; j < p.length; j++) {
                    if (j > 1) {
                        text.append(" | ");
                    }
                    text.append(dec(p[j]));
                }
                text.append('\n');
            }
            adminOutput.setText(text.toString());
        });
    }

    private String[] prompt(String... labels) {
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        JTextField[] fields = new JTextField[labels.length];
        for (int i = 0; i < labels.length; i++) {
            fields[i] = new JTextField(22);
            form.add(new JLabel(labels[i]));
            form.add(fields[i]);
        }
        if (JOptionPane.showConfirmDialog(this, form, "Nhập thông tin",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return null;
        }
        String[] values = new String[labels.length];
        for (int i = 0; i < labels.length; i++) {
            values[i] = fields[i].getText().trim();
        }
        return values;
    }

    private List<String[]> rows(List<String> lines, String kind, String tag, int fieldCount) {
        if (!isOk(lines, kind)) {
            return null;
        }
        List<String[]> data = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] p = lines.get(i).split(";", -1);
            if (p.length != fieldCount + 1 || !tag.equals(p[0])) {
                showError("Dữ liệu " + kind + " không hợp lệ");
                return null;
            }
            String[] row = new String[fieldCount];
            for (int j = 0; j < fieldCount; j++) {
                row[j] = dec(p[j + 1]);
            }
            data.add(row);
        }
        return data;
    }

    private void send(String request, Consumer<List<String>> onSuccess) {
        if (busy) {
            status.setText("Hãy chờ thao tác hiện tại hoàn tất");
            return;
        }
        busy = true;
        status.setText("Đang liên hệ Server...");
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                if (client == null) {
                    client = createClient();
                }
                return client.request(request);
            }

            @Override
            protected void done() {
                busy = false;
                try {
                    onSuccess.accept(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError("Thao tác bị gián đoạn");
                } catch (ExecutionException | RuntimeException ex) {
                    closeClient();
                    Throwable cause = ex instanceof ExecutionException ? ex.getCause() : ex;
                    showError("Không liên hệ được Server: " + cause.getMessage());
                }
            }
        }.execute();
    }

    private MovieClient createClient() throws IOException {
        String host = System.getenv("MOVIE_SERVER_HOST");
        String portText = System.getenv("MOVIE_SERVER_PORT");
        if (host == null || host.trim().isEmpty()) {
            host = MovieClient.DEFAULT_HOST;
        }
        int port = portText == null || portText.trim().isEmpty()
                ? MovieClient.DEFAULT_PORT : Integer.parseInt(portText.trim());
        return new MovieClient(host, port, MovieClient.DEFAULT_TIMEOUT_MS);
    }

    private boolean isOk(List<String> lines, String expected) {
        if (lines.isEmpty()) {
            showError("Server trả phản hồi rỗng");
            return false;
        }
        if (lines.get(0).startsWith("ERR;")) {
            showError(lines.get(0).substring(4));
            return false;
        }
        if (!lines.get(0).startsWith("OK;" + expected)) {
            showError("Phản hồi Server không hợp lệ");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        status.setText(message);
        JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private void closeClient() {
        if (client == null) {
            return;
        }
        try {
            client.close();
        } catch (IOException ignored) {
        }
        client = null;
    }

    private static DefaultTableModel model(String... names) {
        return new DefaultTableModel(names, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static JButton button(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(event -> action.run());
        return button;
    }

    private static JPanel wrap(String title, JPanel form, JPanel actions) {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(35, 50, 35, 50));
        panel.add(new JLabel(title), BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private static String enc(String value) {
        return Protocol.encode(value);
    }

    private static String dec(String value) {
        return Protocol.decode(value);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BasicClientUI().setVisible(true));
    }
}
