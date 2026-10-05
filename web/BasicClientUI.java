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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import protocol.Protocol;

/**
 * Giao diện Swing hiện đại; mọi thao tác dữ liệu vẫn đi qua MovieClient và TCP
 * Server.
 */
public final class BasicClientUI extends JFrame {

    private final CardLayout screenLayout = new CardLayout();
    private final JPanel screens = new JPanel(screenLayout);
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pages = new JPanel(pageLayout);
    private final Map<String, NavButton> navigation = new LinkedHashMap<>();
    private final JLabel status = new JLabel("Sẵn sàng");
    private final JLabel pageTitle = label("", 23, Font.BOLD, UiTheme.TEXT);
    private final JLabel pageSubtitle = label("", 13, Font.PLAIN, UiTheme.MUTED);
    private final JLabel userLabel = label("", 13, Font.BOLD, UiTheme.TEXT);
    private final JLabel movieCount = label("", 13, Font.PLAIN, UiTheme.MUTED);
    private final JLabel bookingCount = label("", 13, Font.PLAIN, UiTheme.MUTED);

    private final JTextField loginName = UiTheme.field(24);
    private final JPasswordField loginPassword = UiTheme.password(24);
    private final JTextField registerName = UiTheme.field(24);
    private final JTextField registerEmail = UiTheme.field(24);
    private final JTextField registerPhone = UiTheme.field(24);
    private final JPasswordField registerPassword = UiTheme.password(24);

    private final JPanel movieGrid = new MovieGrid();
    private final List<MovieItem> movies = new ArrayList<>();
    private final Map<Integer, BigDecimal> showPrices = new LinkedHashMap<>();
    private final DefaultTableModel showModel = model("Mã", "Phim", "Phòng", "Bắt đầu", "Giá vé");
    private final JTable showTable = new JTable(showModel);
    private final JPanel seatGrid = new JPanel(new GridLayout(0, 8, 10, 10));
    private final JLabel seatTitle = label("Chọn một suất chiếu để xem ghế", 16, Font.BOLD, UiTheme.TEXT);
    private final JLabel seatHint = label("Ghế xanh còn trống, ghế tím đã chọn", 12, Font.PLAIN, UiTheme.MUTED);
    private final JLabel selectedSeats = label("Chưa chọn ghế", 13, Font.PLAIN, UiTheme.MUTED);
    private final JLabel totalPrice = label("0 ₫", 20, Font.BOLD, UiTheme.PRIMARY);
    private final List<UiTheme.SeatButton> seatButtons = new ArrayList<>();

    private final DefaultTableModel bookingModel = model("Mã đơn", "Phim", "Phòng",
            "Bắt đầu", "Ghế", "Tổng tiền", "Trạng thái");
    private final JTable bookingTable = new JTable(bookingModel);
    private final DefaultTableModel adminModel = model("Dữ liệu");
    private final JTable adminTable = new JTable(adminModel);
    private final JLabel adminResultTitle = label("Chọn một mục để xem dữ liệu", 17, Font.BOLD, UiTheme.TEXT);
    private final JLabel adminStats = label("", 13, Font.PLAIN, UiTheme.MUTED);
    private JPanel adminNavGroup;

    private MovieClient client;
    private boolean busy;
    private boolean admin;
    private boolean compactMovies;
    private String activePage = "MOVIES";

    public BasicClientUI() {
        super("LTM Movie Tickets");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1220, 780);
        setMinimumSize(new Dimension(980, 690));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UiTheme.BACKGROUND);
        screens.add(buildAuth(false), "LOGIN");
        screens.add(buildAuth(true), "REGISTER");
        screens.add(buildMain(), "MAIN");
        add(screens, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UiTheme.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.LINE),
                new EmptyBorder(5, 18, 5, 18)));
        status.setFont(UiTheme.font(12, Font.PLAIN));
        status.setForeground(UiTheme.MUTED);
        footer.add(status, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
        screenLayout.show(screens, "LOGIN");
        loginPassword.addActionListener(event -> login());
        loginName.addActionListener(event -> login());
        registerPassword.addActionListener(event -> register());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                closeClient();
            }
        });
    }

    private JPanel buildAuth(boolean registering) {
        JPanel form = authForm();
        if (registering) {
            form.add(authField("Họ và tên", registerName));
            form.add(authField("Email", registerEmail));
            form.add(authField("Số điện thoại", registerPhone));
            form.add(authField("Mật khẩu (ít nhất 6 ký tự)", passwordBox(registerPassword)));
            return authScreen("Tạo tài khoản", "Bắt đầu hành trình điện ảnh của bạn.",
                    form, "Đăng ký", this::register, "Đã có tài khoản? Đăng nhập",
                    () -> screenLayout.show(screens, "LOGIN"), 565);
        }
        form.add(authField("Email hoặc số điện thoại", loginName));
        form.add(authField("Mật khẩu", passwordBox(loginPassword)));
        return authScreen("Chào mừng trở lại", "Đặt vé nhanh, dễ dàng.",
                form, "Đăng nhập", this::login, "Chưa có tài khoản? Đăng ký ngay",
                () -> screenLayout.show(screens, "REGISTER"), 430);
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

        int row = 0;
        addAuthRow(content, authLabel("LTM MOVIE TICKETS", 12, Font.BOLD, UiTheme.PRIMARY), row++, 13);
        addAuthRow(content, authLabel(heading, 25, Font.BOLD, UiTheme.TEXT), row++, 4);
        addAuthRow(content, authLabel(subtitle, 12, Font.PLAIN, UiTheme.MUTED), row++, 23);
        addAuthRow(content, form, row++, 20);

        JButton submit = new PurpleButton(actionText);
        submit.setPreferredSize(new Dimension(320, 44));
        submit.addActionListener(event -> action.run());
        addAuthRow(content, submit, row++, 13);

        JButton switchPage = new JButton(switchText);
        switchPage.setFont(new Font("SansSerif", Font.PLAIN, 12));
        switchPage.setForeground(UiTheme.PRIMARY);
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
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
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
        field.add(authLabel(caption, 12, Font.BOLD, UiTheme.TEXT), BorderLayout.NORTH);
        if (input instanceof JTextField) {
            styleAuthInput((JTextField) input);
        }
        input.setPreferredSize(new Dimension(320, 43));
        field.add(input, BorderLayout.CENTER);
        return field;
    }

    private static JLabel authLabel(String value, int size, int weight, Color color) {
        JLabel label = new JLabel(value);
        label.setFont(new Font("SansSerif", weight, size));
        label.setForeground(color);
        return label;
    }

    private static void styleAuthInput(JTextField input) {
        input.setFont(new Font("SansSerif", Font.PLAIN, 14));
        input.setForeground(UiTheme.TEXT);
        input.setBackground(Color.WHITE);
        input.setCaretColor(UiTheme.PRIMARY);
        input.setBorder(new RoundedInputBorder());
    }

    private static JPanel passwordBox(JPasswordField password) {
        JPanel box = new JPanel(new BorderLayout());
        box.setOpaque(false);
        box.setBorder(new RoundedInputBorder());
        password.setFont(new Font("SansSerif", Font.PLAIN, 14));
        password.setForeground(UiTheme.TEXT);
        password.setOpaque(false);
        password.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 2));
        char hidden = password.getEchoChar();
        JButton toggle = new JButton("Hiện");
        toggle.setFont(new Font("SansSerif", Font.PLAIN, 11));
        toggle.setForeground(UiTheme.PRIMARY);
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
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UiTheme.BACKGROUND);
        root.add(buildSidebar(), BorderLayout.WEST);
        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(buildHeader(), BorderLayout.NORTH);
        pages.setOpaque(false);
        pages.add(buildMovies(), "MOVIES");
        pages.add(buildShows(), "SHOWS");
        pages.add(buildBookings(), "BOOKINGS");
        pages.add(buildAdmin(), "ADMIN");
        right.add(pages, BorderLayout.CENTER);
        root.add(right, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(UiTheme.NAVY);
        sidebar.setPreferredSize(new Dimension(218, 0));
        sidebar.setBorder(new EmptyBorder(28, 14, 20, 14));
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JLabel mark = label("LTM  /  CINEMA", 18, Font.BOLD, UiTheme.WHITE);
        mark.setBorder(new EmptyBorder(0, 12, 0, 0));
        top.add(mark);
        top.add(Box.createVerticalStrut(6));
        JLabel tagline = label("Movie ticket studio", 11, Font.PLAIN, new Color(166, 179, 208));
        tagline.setBorder(new EmptyBorder(0, 12, 0, 0));
        top.add(tagline);
        top.add(Box.createVerticalStrut(48));
        top.add(sideCaption("KHÁM PHÁ"));
        top.add(Box.createVerticalStrut(9));
        top.add(nav("MOVIES", "01   Phim đang chiếu"));
        top.add(Box.createVerticalStrut(6));
        top.add(nav("SHOWS", "02   Suất chiếu & ghế"));
        top.add(Box.createVerticalStrut(6));
        top.add(nav("BOOKINGS", "03   Vé của tôi"));
        adminNavGroup = new JPanel();
        adminNavGroup.setOpaque(false);
        adminNavGroup.setLayout(new BoxLayout(adminNavGroup, BoxLayout.Y_AXIS));
        adminNavGroup.add(Box.createVerticalStrut(28));
        adminNavGroup.add(sideCaption("HỆ THỐNG"));
        adminNavGroup.add(Box.createVerticalStrut(9));
        adminNavGroup.add(nav("ADMIN", "04   Quản trị"));
        adminNavGroup.setVisible(false);
        top.add(adminNavGroup);
        sidebar.add(top, BorderLayout.NORTH);
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.add(label("Đặt vé dễ dàng", 13, Font.BOLD, UiTheme.WHITE));
        bottom.add(Box.createVerticalStrut(5));
        bottom.add(label("Chọn phim. Chọn ghế. Tận hưởng.", 11,
                Font.PLAIN, new Color(166, 179, 208)));
        sidebar.add(bottom, BorderLayout.SOUTH);
        return sidebar;
    }

    private JLabel sideCaption(String text) {
        JLabel caption = label(text, 10, Font.BOLD, new Color(124, 142, 176));
        caption.setBorder(new EmptyBorder(0, 12, 0, 0));
        return caption;
    }

    private NavButton nav(String key, String text) {
        NavButton button = new NavButton(text);
        button.addActionListener(event -> navigate(key));
        navigation.put(key, button);
        return button;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(UiTheme.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.LINE),
                new EmptyBorder(17, 27, 17, 27)));
        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.add(pageTitle);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(pageSubtitle);
        header.add(titleBox, BorderLayout.WEST);
        JPanel userBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        userBox.setOpaque(false);
        userBox.add(userLabel);
        userBox.add(action("Đăng xuất", this::logout, "subtle"));
        header.add(userBox, BorderLayout.EAST);
        return header;
    }

    private JPanel buildMovies() {
        JPanel page = page();
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(label("Phim dành cho bạn", 19, Font.BOLD, UiTheme.TEXT));
        left.add(Box.createVerticalStrut(4));
        left.add(movieCount);
        head.add(left, BorderLayout.WEST);
        head.add(action("Làm mới", this::loadMovies, "secondary"), BorderLayout.EAST);
        page.add(head, BorderLayout.NORTH);
        movieGrid.setOpaque(false);
        movieGrid.setBorder(new EmptyBorder(2, 2, 10, 10));
        JScrollPane scroll = UiTheme.scroll(movieGrid);
        scroll.getViewport().setBackground(UiTheme.BACKGROUND);
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                boolean compact = scroll.getViewport().getWidth() < 790;
                if (compact != compactMovies) {
                    compactMovies = compact;
                    renderMovies();
                }
            }
        });
        page.add(scroll, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildShows() {
        JPanel page = page();
        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.add(label("Chọn suất chiếu", 19, Font.BOLD, UiTheme.TEXT), BorderLayout.WEST);
        toolbar.add(action("Xem tất cả", () -> loadShows(0), "secondary"), BorderLayout.EAST);
        top.add(toolbar, BorderLayout.NORTH);
        UiTheme.RoundedPanel listCard = UiTheme.surface();
        listCard.setLayout(new BorderLayout(0, 12));
        listCard.add(label("Nhấn vào một suất để xem ghế của phòng", 13,
                Font.PLAIN, UiTheme.MUTED), BorderLayout.NORTH);
        UiTheme.styleTable(showTable);
        showTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && showTable.getSelectedRow() >= 0) {
                loadSeats();
            }
        });
        listCard.add(UiTheme.scroll(showTable), BorderLayout.CENTER);
        listCard.setPreferredSize(new Dimension(0, 236));
        top.add(listCard, BorderLayout.CENTER);
        page.add(top, BorderLayout.NORTH);

        UiTheme.RoundedPanel seatCard = UiTheme.surface();
        seatCard.setLayout(new BorderLayout(0, 14));
        JPanel seatHeader = new JPanel(new BorderLayout());
        seatHeader.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(seatTitle);
        titles.add(Box.createVerticalStrut(4));
        titles.add(seatHint);
        seatHeader.add(titles, BorderLayout.WEST);
        seatHeader.add(action("Tải lại ghế", this::loadSeats, "subtle"), BorderLayout.EAST);
        seatCard.add(seatHeader, BorderLayout.NORTH);
        seatGrid.setBackground(UiTheme.WHITE);
        seatGrid.setBorder(new EmptyBorder(8, 5, 12, 5));
        JScrollPane seatScroll = UiTheme.scroll(seatGrid);
        seatCard.add(seatScroll, BorderLayout.CENTER);
        JPanel summary = new JPanel(new BorderLayout(15, 0));
        summary.setOpaque(false);
        summary.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.LINE));
        JPanel sumText = new JPanel();
        sumText.setOpaque(false);
        sumText.setLayout(new BoxLayout(sumText, BoxLayout.Y_AXIS));
        sumText.setBorder(new EmptyBorder(12, 0, 0, 0));
        sumText.add(selectedSeats);
        sumText.add(Box.createVerticalStrut(3));
        sumText.add(totalPrice);
        summary.add(sumText, BorderLayout.CENTER);
        summary.add(action("Đặt vé ngay", this::book, "primary"), BorderLayout.EAST);
        seatCard.add(summary, BorderLayout.SOUTH);
        page.add(seatCard, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildBookings() {
        JPanel page = page();
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(label("Đơn vé của tôi", 19, Font.BOLD, UiTheme.TEXT));
        heading.add(Box.createVerticalStrut(4));
        heading.add(bookingCount);
        head.add(heading, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(action("Làm mới", this::loadBookings, "secondary"));
        actions.add(action("Chi tiết", this::ticketDetail, "subtle"));
        actions.add(action("Hủy đơn", this::cancel, "danger"));
        head.add(actions, BorderLayout.EAST);
        page.add(head, BorderLayout.NORTH);
        UiTheme.RoundedPanel card = UiTheme.surface();
        card.setLayout(new BorderLayout(0, 12));
        card.add(label("Chọn một đơn để xem chi tiết hoặc hủy trước giờ chiếu", 13,
                Font.PLAIN, UiTheme.MUTED), BorderLayout.NORTH);
        UiTheme.styleTable(bookingTable);
        bookingTable.getColumnModel().getColumn(6).setCellRenderer(new StatusRenderer());
        card.add(UiTheme.scroll(bookingTable), BorderLayout.CENTER);
        page.add(card, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildAdmin() {
        JPanel page = page();
        JPanel groups = new JPanel(new GridLayout(2, 2, 14, 14));
        groups.setOpaque(false);
        groups.add(adminGroup("Quản lý phim", "Nội dung đang chiếu",
                action("Danh sách", this::adminMovies, "subtle"),
                action("Thêm", this::adminAddMovie, "secondary"),
                action("Sửa", this::adminUpdateMovie, "subtle"),
                action("Xóa", () -> adminDelete("ADMIN_DELETE_MOVIE", "Mã phim", this::adminMovies), "danger")));
        groups.add(adminGroup("Phòng chiếu", "Không gian và số ghế",
                action("Danh sách", this::adminRooms, "subtle"),
                action("Thêm", this::adminAddRoom, "secondary"),
                action("Đổi tên", this::adminUpdateRoom, "subtle"),
                action("Xóa", () -> adminDelete("ADMIN_DELETE_ROOM", "Mã phòng", this::adminRooms), "danger")));
        groups.add(adminGroup("Suất chiếu", "Lịch và giá vé",
                action("Danh sách", this::adminShows, "subtle"),
                action("Thêm", this::adminAddShow, "secondary"),
                action("Sửa", this::adminUpdateShow, "subtle"),
                action("Xóa", () -> adminDelete("ADMIN_DELETE_SHOW", "Mã suất chiếu", this::adminShows), "danger")));
        groups.add(adminGroup("Báo cáo", "Người dùng, đơn vé và doanh thu",
                action("Người dùng", this::adminUsers, "subtle"),
                action("Đơn vé", this::adminBookings, "subtle"),
                action("Doanh thu", this::adminStats, "secondary"),
                action("Hủy đơn", this::adminCancel, "danger")));
        groups.setPreferredSize(new Dimension(0, 320));
        page.add(groups, BorderLayout.NORTH);

        UiTheme.RoundedPanel results = UiTheme.surface();
        results.setLayout(new BorderLayout(0, 10));
        JPanel title = new JPanel(new BorderLayout());
        title.setOpaque(false);
        title.add(adminResultTitle, BorderLayout.WEST);
        title.add(adminStats, BorderLayout.EAST);
        results.add(title, BorderLayout.NORTH);
        UiTheme.styleTable(adminTable);
        results.add(UiTheme.scroll(adminTable), BorderLayout.CENTER);
        page.add(results, BorderLayout.CENTER);
        return page;
    }

    private JPanel adminGroup(String title, String subtitle, JButton... actions) {
        UiTheme.RoundedPanel card = UiTheme.surface();
        card.setLayout(new BorderLayout(0, 12));
        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.add(label(title, 15, Font.BOLD, UiTheme.TEXT));
        texts.add(Box.createVerticalStrut(3));
        texts.add(label(subtitle, 11, Font.PLAIN, UiTheme.MUTED));
        card.add(texts, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new GridLayout(2, 2, 6, 6));
        buttons.setOpaque(false);
        for (JButton button : actions) {
            buttons.add(button);
        }
        card.add(buttons, BorderLayout.CENTER);
        return card;
    }

    private static JPanel page() {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setBackground(UiTheme.BACKGROUND);
        page.setBorder(new EmptyBorder(23, 27, 23, 27));
        return page;
    }

    private void navigate(String page) {
        navigate(page, true);
    }

    private void navigate(String page, boolean fetch) {
        if (busy) {
            status.setText("Hãy chờ thao tác hiện tại hoàn tất");
            return;
        }
        if ("ADMIN".equals(page) && !admin) {
            return;
        }
        activePage = page;
        pageLayout.show(pages, page);
        for (Map.Entry<String, NavButton> entry : navigation.entrySet()) {
            entry.getValue().setActive(entry.getKey().equals(page));
        }
        switch (page) {
            case "MOVIES":
                pageTitle.setText("Khám phá phim");
                pageSubtitle.setText("Chọn một bộ phim để xem lịch chiếu.");
                if (fetch && movies.isEmpty()) {
                    loadMovies();
                }
                break;
            case "SHOWS":
                pageTitle.setText("Suất chiếu & ghế");
                pageSubtitle.setText("Ghế được xác nhận ngay khi bạn đặt vé.");
                if (fetch && showModel.getRowCount() == 0) {
                    loadShows(0);
                } else if (fetch && selectedShowId() != 0) {
                    loadSeats();
                }
                break;
            case "BOOKINGS":
                pageTitle.setText("Vé của tôi");
                pageSubtitle.setText("Theo dõi và quản lý những đơn vé đã đặt.");
                if (fetch) {
                    loadBookings();
                }
                break;
            case "ADMIN":
                pageTitle.setText("Trung tâm quản trị");
                pageSubtitle.setText("Quản lý nội dung, lịch chiếu và hoạt động đặt vé.");
                if (fetch) {
                    adminStats();
                }
                break;
            default:
                break;
        }
    }

    private void register() {
        String name = registerName.getText().trim();
        String email = registerEmail.getText().trim();
        String phone = registerPhone.getText().trim();
        String password = new String(registerPassword.getPassword());
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            showError("Hãy nhập đầy đủ thông tin đăng ký");
            return;
        }
        send("REGISTER;" + enc(name) + ";" + enc(email) + ";" + enc(phone) + ";" + enc(password),
                lines -> {
                    if (!isOk(lines, "REGISTER")) {
                        return;
                    }
                    loginName.setText(email);
                    registerPassword.setText("");
                    screenLayout.show(screens, "LOGIN");
                    info("Tạo tài khoản thành công. Hãy đăng nhập.");
                });
    }

    private void login() {
        String name = loginName.getText().trim();
        String password = new String(loginPassword.getPassword());
        if (name.isEmpty() || password.isEmpty()) {
            showError("Hãy nhập tài khoản và mật khẩu");
            return;
        }
        send("LOGIN;" + enc(name) + ";" + enc(password), lines -> {
            if (!isOk(lines, "LOGIN")) {
                return;
            }
            String[] data = lines.get(0).split(";", -1);
            if (data.length != 6) {
                showError("Phản hồi đăng nhập không hợp lệ");
                return;
            }
            userLabel.setText("Xin chào, " + dec(data[3]));
            admin = "admin".equals(dec(data[5]));
            adminNavGroup.setVisible(admin);
            loginPassword.setText("");
            screenLayout.show(screens, "MAIN");
            navigate("MOVIES");
            status.setText("Đã kết nối với máy chủ");
        });
    }

    private void logout() {
        send("LOGOUT", lines -> {
            if (!isOk(lines, "LOGOUT")) {
                return;
            }
            closeClient();
            admin = false;
            movies.clear();
            showPrices.clear();
            movieGrid.removeAll();
            showTable.clearSelection();
            showModel.setRowCount(0);
            bookingModel.setRowCount(0);
            seatGrid.removeAll();
            seatButtons.clear();
            adminModel.setRowCount(0);
            screenLayout.show(screens, "LOGIN");
            status.setText("Đã đăng xuất");
        });
    }

    private void loadMovies() {
        send("GET_MOVIES", lines -> {
            if (!isOk(lines, "MOVIES")) {
                return;
            }
            movies.clear();
            for (int i = 1; i < lines.size(); i++) {
                String[] p = lines.get(i).split(";", -1);
                if (p.length != 8 || !"MOVIE".equals(p[0])) {
                    showError("Dữ liệu phim không hợp lệ");
                    return;
                }
                movies.add(new MovieItem(Integer.parseInt(p[1]), dec(p[2]),
                        dec(p[3]), p[4], p[5], dec(p[6])));
            }
            renderMovies();
            status.setText("Đã tải " + movies.size() + " phim");
        });
    }

    private void renderMovies() {
        movieGrid.removeAll();
        movieCount.setText(movies.size() + " phim trong danh sách");
        List<JPanel> cards = new ArrayList<>();
        if (movies.isEmpty()) {
            cards.add(emptyState("Chưa có phim", "Quản trị viên có thể thêm phim mới."));
        } else {
            for (MovieItem movie : movies) {
                cards.add(movieCard(movie));
            }
        }
        int columns = compactMovies ? 1 : 2;
        for (int i = 0; i < cards.size(); i++) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = i % columns;
            c.gridy = i / columns;
            c.weightx = 1.0 / columns;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTH;
            c.insets = new java.awt.Insets(0, 0, 16,
                    columns == 2 && i % 2 == 0 ? 16 : 0);
            movieGrid.add(cards.get(i), c);
        }
        GridBagConstraints space = new GridBagConstraints();
        space.gridx = 0;
        space.gridy = Math.max(1, (cards.size() + columns - 1) / columns);
        space.gridwidth = columns;
        space.weighty = 1;
        space.fill = GridBagConstraints.BOTH;
        movieGrid.add(Box.createGlue(), space);
        int rows = Math.max(1, (cards.size() + columns - 1) / columns);
        int width = compactMovies && movieGrid.getParent() != null
                ? Math.max(300, movieGrid.getParent().getWidth() - 8) : 780;
        movieGrid.setPreferredSize(new Dimension(width, rows * 225 + 10));
        movieGrid.revalidate();
        movieGrid.repaint();
    }

    private JPanel movieCard(MovieItem movie) {
        UiTheme.RoundedPanel card = UiTheme.surface();
        card.setLayout(new BorderLayout(16, 0));
        card.setPreferredSize(new Dimension(380, 210));
        JPanel poster = new JPanel(new BorderLayout());
        poster.setOpaque(false);
        poster.add(new UiTheme.PosterPanel(movie.title), BorderLayout.NORTH);
        card.add(poster, BorderLayout.WEST);
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JTextArea title = new JTextArea(movie.title);
        title.setFont(UiTheme.font(17, Font.BOLD));
        title.setForeground(UiTheme.TEXT);
        title.setOpaque(false);
        title.setEditable(false);
        title.setFocusable(false);
        title.setLineWrap(true);
        title.setWrapStyleWord(true);
        title.setPreferredSize(new Dimension(190, 40));
        title.setMinimumSize(new Dimension(0, 40));
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        title.setAlignmentX(LEFT_ALIGNMENT);
        info.add(title);
        info.add(Box.createVerticalStrut(4));
        info.add(label(movie.genre + "   •   " + movie.minutes + " phút",
                12, Font.PLAIN, UiTheme.MUTED));
        info.add(Box.createVerticalStrut(3));
        info.add(label("Khởi chiếu: " + movie.release, 12, Font.PLAIN, UiTheme.MUTED));
        info.add(Box.createVerticalStrut(6));
        JTextArea description = new JTextArea(movie.description);
        description.setFont(UiTheme.font(12, Font.PLAIN));
        description.setForeground(UiTheme.MUTED);
        description.setOpaque(false);
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setFocusable(false);
        description.setAlignmentX(LEFT_ALIGNMENT);
        description.setPreferredSize(new Dimension(190, 32));
        description.setMinimumSize(new Dimension(0, 32));
        description.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        info.add(description);
        info.add(Box.createVerticalStrut(8));
        UiTheme.StyledButton view = action("Xem suất chiếu", () -> {
            navigate("SHOWS", false);
            loadShows(movie.id);
        }, "secondary");
        view.setAlignmentX(LEFT_ALIGNMENT);
        view.setMaximumSize(new Dimension(165, 40));
        info.add(view);
        card.add(info, BorderLayout.CENTER);
        return card;
    }

    private JPanel emptyState(String title, String message) {
        UiTheme.RoundedPanel card = UiTheme.surface();
        card.setLayout(new GridBagLayout());
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(label(title, 18, Font.BOLD, UiTheme.TEXT));
        text.add(Box.createVerticalStrut(7));
        text.add(label(message, 13, Font.PLAIN, UiTheme.MUTED));
        card.add(text);
        return card;
    }

    private void loadShows(int movieId) {
        send("GET_SHOWS;" + movieId, lines -> {
            List<String[]> data = rows(lines, "SHOWS", "SHOW", 6);
            if (data == null) {
                return;
            }
            showTable.clearSelection();
            showModel.setRowCount(0);
            showPrices.clear();
            clearSeats();
            for (String[] row : data) {
                int id = Integer.parseInt(row[0]);
                showPrices.put(id, new BigDecimal(row[5]));
                showModel.addRow(new Object[]{row[0], row[2], row[3],
                    formatDate(row[4]), formatMoney(row[5])});
            }
            status.setText("Đã tải " + data.size() + " suất chiếu"
                    + (movieId == 0 ? "" : " cho phim #" + movieId));
        });
    }

    private int selectedShowId() {
        int row = showTable.getSelectedRow();
        if (row < 0) {
            return 0;
        }
        return Integer.parseInt(showModel.getValueAt(
                showTable.convertRowIndexToModel(row), 0).toString());
    }

    private void clearSeats() {
        seatButtons.clear();
        seatGrid.removeAll();
        seatTitle.setText("Chọn một suất chiếu để xem ghế");
        selectedSeats.setText("Chưa chọn ghế");
        totalPrice.setText("0 ₫");
        seatGrid.revalidate();
        seatGrid.repaint();
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
            seatButtons.clear();
            seatGrid.removeAll();
            seatTitle.setText("Sơ đồ ghế — suất #" + showId);
            for (String[] row : data) {
                boolean taken = "taken".equals(row[1]);
                UiTheme.SeatButton button = new UiTheme.SeatButton(row[0], taken);
                button.addActionListener(event -> updateSelection());
                seatGrid.add(button);
                if (!taken) {
                    seatButtons.add(button);
                }
            }
            int rows = Math.max(1, (data.size() + 7) / 8);
            seatGrid.setPreferredSize(new Dimension(630, rows * 54));
            seatGrid.revalidate();
            seatGrid.repaint();
            updateSelection();
            status.setText("Đã tải " + data.size() + " ghế");
        });
    }

    private void updateSelection() {
        List<String> codes = new ArrayList<>();
        for (UiTheme.SeatButton button : seatButtons) {
            if (button.isSelected()) {
                codes.add(button.getText());
            }
        }
        selectedSeats.setText(codes.isEmpty() ? "Chưa chọn ghế"
                : codes.size() + " ghế: " + String.join(", ", codes));
        BigDecimal price = showPrices.get(selectedShowId());
        totalPrice.setText(price == null ? "0 ₫"
                : formatMoney(price.multiply(BigDecimal.valueOf(codes.size())).toPlainString()));
    }

    private void book() {
        int showId = selectedShowId();
        if (showId == 0) {
            showError("Hãy chọn một suất chiếu");
            return;
        }
        List<String> codes = new ArrayList<>();
        for (UiTheme.SeatButton button : seatButtons) {
            if (button.isSelected()) {
                codes.add(button.getText());
            }
        }
        if (codes.isEmpty()) {
            showError("Hãy chọn ít nhất một ghế");
            return;
        }
        if (codes.size() > 10) {
            showError("Mỗi lần đặt tối đa 10 ghế");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Đặt " + String.join(", ", codes) + " với tổng tiền " + totalPrice.getText() + "?",
                "Xác nhận đặt vé", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        send("BOOK_TICKET;" + showId + ";" + enc(String.join(",", codes)), lines -> {
            if (!isOk(lines, "BOOKED")) {
                loadSeats();
                return;
            }
            String[] p = lines.get(0).split(";", -1);
            info("Đặt vé thành công" + (p.length > 2 ? " — mã đơn #" + p[2] : ""));
            navigate("BOOKINGS");
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
                bookingModel.addRow(new Object[]{row[0], row[2], row[3], formatDate(row[4]),
                    row[7], formatMoney(row[5]), statusText(row[6])});
            }
            bookingCount.setText(data.size() + " đơn vé trong lịch sử");
            status.setText("Đã tải " + data.size() + " đơn vé");
        });
    }

    private String selectedBookingId() {
        int row = bookingTable.getSelectedRow();
        if (row < 0) {
            showError("Hãy chọn một đơn vé");
            return null;
        }
        return bookingModel.getValueAt(bookingTable.convertRowIndexToModel(row), 0).toString();
    }

    private void ticketDetail() {
        String id = selectedBookingId();
        if (id == null) {
            return;
        }
        send("GET_TICKET;" + id, lines -> {
            List<String[]> data = rows(lines, "BOOKINGS", "BOOKING", 8);
            if (data == null || data.isEmpty()) {
                return;
            }
            String[] row = data.get(0);
            JPanel detail = new JPanel(new GridLayout(0, 2, 12, 12));
            detail.setBorder(new EmptyBorder(12, 12, 12, 12));
            detailRow(detail, "Mã đơn", "#" + row[0]);
            detailRow(detail, "Phim", row[2]);
            detailRow(detail, "Phòng", row[3]);
            detailRow(detail, "Bắt đầu", formatDate(row[4]));
            detailRow(detail, "Ghế", row[7]);
            detailRow(detail, "Tổng tiền", formatMoney(row[5]));
            detailRow(detail, "Trạng thái", statusText(row[6]));
            JOptionPane.showMessageDialog(this, detail, "Chi tiết vé", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void detailRow(JPanel panel, String label, String value) {
        panel.add(label(label, 13, Font.PLAIN, UiTheme.MUTED));
        panel.add(label(value, 13, Font.BOLD, UiTheme.TEXT));
    }

    private void cancel() {
        String id = selectedBookingId();
        if (id == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Bạn muốn hủy toàn bộ đơn vé #" + id + "?",
                "Xác nhận hủy vé", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        send("CANCEL_TICKET;" + id, lines -> {
            if (!isOk(lines, "CANCELLED")) {
                return;
            }
            info("Đã hủy đơn vé #" + id);
            loadBookings();
        });
    }

    private void adminMovies() {
        send("GET_MOVIES", lines -> {
            if (!isOk(lines, "MOVIES")) {
                return;
            }
            List<Object[]> data = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String[] p = lines.get(i).split(";", -1);
                if (p.length != 8 || !"MOVIE".equals(p[0])) {
                    showError("Dữ liệu phim không hợp lệ");
                    return;
                }
                data.add(new Object[]{p[1], dec(p[2]), dec(p[3]), p[4], p[5]});
            }
            setAdminTable("Danh sách phim", new String[]{"Mã", "Tên phim", "Thể loại", "Phút", "Khởi chiếu"}, data);
        });
    }

    private void adminRooms() {
        adminList("ADMIN_GET_ROOMS", "ROOMS", "ROOM",
                new String[]{"Mã", "Tên phòng", "Số ghế"}, 3);
    }

    private void adminShows() {
        adminList("ADMIN_GET_SHOWS", "SHOWS", "SHOW",
                new String[]{"Mã", "Mã phim", "Phim", "Phòng", "Bắt đầu", "Giá vé"}, 6);
    }

    private void adminUsers() {
        adminList("ADMIN_GET_USERS", "USERS", "USER",
                new String[]{"Mã", "Họ tên", "Email", "Số điện thoại", "Vai trò"}, 5);
    }

    private void adminBookings() {
        adminList("ADMIN_GET_BOOKINGS", "BOOKINGS", "BOOKING",
                new String[]{"Mã đơn", "Người đặt", "Phim", "Phòng", "Bắt đầu",
                    "Tổng tiền", "Trạng thái", "Ghế"}, 8);
    }

    private void adminStats() {
        send("ADMIN_GET_STATS", lines -> {
            List<String[]> data = rows(lines, "STATS", "STAT", 2);
            if (data == null || data.isEmpty()) {
                return;
            }
            adminStats.setText("Tổng đơn: " + data.get(0)[0]
                    + "    •    Doanh thu còn hiệu lực: " + formatMoney(data.get(0)[1]));
            status.setText("Đã tải thống kê");
        });
    }

    private void adminList(String command, String kind, String tag, String[] headers, int fields) {
        send(command, lines -> {
            List<String[]> data = rows(lines, kind, tag, fields);
            if (data == null) {
                return;
            }
            List<Object[]> visible = new ArrayList<>();
            for (String[] row : data) {
                Object[] copy = row.clone();
                if ("SHOWS".equals(kind)) {
                    copy[4] = formatDate(row[4]);
                    copy[5] = formatMoney(row[5]);
                } else if ("BOOKINGS".equals(kind)) {
                    copy[4] = formatDate(row[4]);
                    copy[5] = formatMoney(row[5]);
                    copy[6] = statusText(row[6]);
                }
                visible.add(copy);
            }
            setAdminTable(kind.equals("ROOMS") ? "Danh sách phòng"
                    : kind.equals("SHOWS") ? "Danh sách suất chiếu"
                    : kind.equals("USERS") ? "Người dùng" : "Tất cả đơn vé", headers, visible);
        });
    }

    private void setAdminTable(String title, String[] headers, List<Object[]> rows) {
        adminResultTitle.setText(title);
        adminModel.setDataVector(rows.toArray(new Object[0][]), headers);
        status.setText("Đã tải " + rows.size() + " mục");
    }

    private void adminAddMovie() {
        String[] v = prompt("Thêm phim", "Tên phim", "Thể loại", "Thời lượng (phút)", "Mô tả");
        if (v == null) {
            return;
        }
        send("ADMIN_ADD_MOVIE;" + enc(v[0]) + ";" + enc(v[1]) + ";" + v[2] + ";" + enc(v[3]),
                lines -> {
                    if (isOk(lines, "ADDED")) {
                        movies.clear();
                        info("Đã thêm phim");
                        adminMovies();
                    }
                });
    }

    private void adminUpdateMovie() {
        String[] v = prompt("Sửa phim", "Mã phim", "Tên phim mới", "Thể loại",
                "Thời lượng (phút)", "Mô tả");
        if (v == null) {
            return;
        }
        send("ADMIN_UPDATE_MOVIE;" + v[0] + ";" + enc(v[1]) + ";" + enc(v[2])
                + ";" + v[3] + ";" + enc(v[4]),
                lines -> {
                    if (isOk(lines, "UPDATED")) {
                        movies.clear();
                        info("Đã sửa phim");
                        adminMovies();
                    }
                });
    }

    private void adminAddRoom() {
        String[] v = prompt("Thêm phòng", "Tên phòng", "Số hàng ghế (1–26)",
                "Số ghế mỗi hàng (1–99)");
        if (v == null) {
            return;
        }
        send("ADMIN_ADD_ROOM;" + enc(v[0]) + ";" + v[1] + ";" + v[2],
                lines -> {
                    if (isOk(lines, "ADDED")) {
                        info("Đã thêm phòng");
                        adminRooms();
                    }
                });
    }

    private void adminUpdateRoom() {
        String[] v = prompt("Đổi tên phòng", "Mã phòng", "Tên phòng mới");
        if (v == null) {
            return;
        }
        send("ADMIN_UPDATE_ROOM;" + v[0] + ";" + enc(v[1]),
                lines -> {
                    if (isOk(lines, "UPDATED")) {
                        showModel.setRowCount(0);
                        info("Đã đổi tên phòng");
                        adminRooms();
                    }
                });
    }

    private void adminAddShow() {
        String[] v = prompt("Thêm suất chiếu", "Mã phim", "Mã phòng",
                "Bắt đầu (yyyy-MM-ddTHH:mm)", "Giá vé");
        if (v == null) {
            return;
        }
        send("ADMIN_ADD_SHOW;" + String.join(";", v),
                lines -> {
                    if (isOk(lines, "ADDED")) {
                        showModel.setRowCount(0);
                        info("Đã thêm suất chiếu");
                        adminShows();
                    }
                });
    }

    private void adminUpdateShow() {
        String[] v = prompt("Sửa suất chiếu", "Mã suất chiếu", "Mã phim", "Mã phòng",
                "Bắt đầu (yyyy-MM-ddTHH:mm)", "Giá vé");
        if (v == null) {
            return;
        }
        send("ADMIN_UPDATE_SHOW;" + String.join(";", v),
                lines -> {
                    if (isOk(lines, "UPDATED")) {
                        showModel.setRowCount(0);
                        info("Đã sửa suất chiếu");
                        adminShows();
                    }
                });
    }

    private void adminCancel() {
        String id = JOptionPane.showInputDialog(this, "Mã đơn vé cần hủy");
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Hủy đơn vé #" + id.trim() + "?",
                "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        send("ADMIN_CANCEL_TICKET;" + id.trim(),
                lines -> {
                    if (isOk(lines, "CANCELLED")) {
                        info("Đã hủy đơn vé");
                        adminBookings();
                    }
                });
    }

    private void adminDelete(String command, String label, Runnable refresh) {
        String id = JOptionPane.showInputDialog(this, label);
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Xóa " + label.toLowerCase(Locale.ROOT)
                + " #" + id.trim() + "?", "Xác nhận", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) {
            return;
        }
        send(command + ";" + id.trim(), lines -> {
            if (!isOk(lines, "DELETED")) {
                return;
            }
            movies.clear();
            showModel.setRowCount(0);
            info("Đã xóa " + label.toLowerCase(Locale.ROOT) + " #" + id.trim());
            refresh.run();
        });
    }

    private String[] prompt(String title, String... labels) {
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBorder(new EmptyBorder(14, 10, 14, 10));
        JTextField[] fields = new JTextField[labels.length];
        for (int i = 0; i < labels.length; i++) {
            fields[i] = UiTheme.field(24);
            form.add(label(labels[i], 13, Font.PLAIN, UiTheme.TEXT));
            form.add(fields[i]);
        }
        if (JOptionPane.showConfirmDialog(this, form, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
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
        status.setText("Đang liên hệ máy chủ...");
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
                    admin = false;
                    adminNavGroup.setVisible(false);
                    screenLayout.show(screens, "LOGIN");
                    showError("Không liên hệ được máy chủ: " + cause.getMessage());
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
            showError("Máy chủ trả phản hồi rỗng");
            return false;
        }
        if (lines.get(0).startsWith("ERR;")) {
            showError(lines.get(0).substring(4));
            return false;
        }
        if (!lines.get(0).startsWith("OK;" + expected)) {
            showError("Phản hồi máy chủ không hợp lệ");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        status.setText(message);
        JOptionPane.showMessageDialog(this, message, "Có lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private void info(String message) {
        status.setText(message);
        JOptionPane.showMessageDialog(this, message, "LTM Cinema", JOptionPane.INFORMATION_MESSAGE);
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

    private static JLabel label(String text, int size, int weight, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(UiTheme.font(size, weight));
        label.setForeground(color);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private UiTheme.StyledButton action(String text, Runnable runnable, String kind) {
        UiTheme.StyledButton button;
        switch (kind) {
            case "primary":
                button = UiTheme.primary(text);
                break;
            case "danger":
                button = UiTheme.danger(text);
                break;
            case "subtle":
                button = UiTheme.subtle(text);
                break;
            default:
                button = UiTheme.secondary(text);
                break;
        }
        button.addActionListener(event -> runnable.run());
        return button;
    }

    private static void addField(JPanel panel, String title, JTextField field) {
        panel.add(label(title, 13, Font.BOLD, UiTheme.TEXT));
        panel.add(Box.createVerticalStrut(7));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        field.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(field);
        panel.add(Box.createVerticalStrut(16));
    }

    private JButton authLink(String text, Runnable action) {
        JButton link = new JButton(text);
        link.setFont(UiTheme.font(13, Font.PLAIN));
        link.setForeground(UiTheme.PRIMARY);
        link.setBorderPainted(false);
        link.setContentAreaFilled(false);
        link.setFocusPainted(false);
        link.setAlignmentX(LEFT_ALIGNMENT);
        link.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        link.addActionListener(event -> action.run());
        return link;
    }

    private static String formatDate(String value) {
        return value == null ? "" : value.replace('T', ' ');
    }

    private static String formatMoney(String value) {
        try {
            NumberFormat format = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            format.setMaximumFractionDigits(2);
            return format.format(new BigDecimal(value)) + " ₫";
        } catch (Exception ex) {
            return value + " ₫";
        }
    }

    private static String statusText(String value) {
        return "active".equals(value) ? "Còn hiệu lực" : "Đã hủy";
    }

    private static String enc(String value) {
        return Protocol.encode(value);
    }

    private static String dec(String value) {
        return Protocol.decode(value);
    }

    public static void main(String[] args) {
        UIManager.put("OptionPane.messageFont", UiTheme.font(13, Font.PLAIN));
        UIManager.put("OptionPane.buttonFont", UiTheme.font(12, Font.BOLD));
        SwingUtilities.invokeLater(() -> new BasicClientUI().setVisible(true));
    }

    private static final class MovieItem {

        private final int id;
        private final String title;
        private final String genre;
        private final String minutes;
        private final String release;
        private final String description;

        private MovieItem(int id, String title, String genre, String minutes,
                String release, String description) {
            this.id = id;
            this.title = title;
            this.genre = genre;
            this.minutes = minutes;
            this.release = release;
            this.description = description;
        }
    }

    private static final class MovieGrid extends JPanel implements Scrollable {

        MovieGrid() {
            super(new GridBagLayout());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(80, visible.height - 40);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static final class NavButton extends JButton {

        private boolean active;

        NavButton(String text) {
            super(text);
            setPreferredSize(new Dimension(188, 44));
            setMaximumSize(new Dimension(188, 44));
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new EmptyBorder(0, 13, 0, 8));
            setFont(UiTheme.font(13, Font.BOLD));
            setForeground(new Color(188, 200, 222));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }

        void setActive(boolean value) {
            active = value;
            setForeground(value ? UiTheme.WHITE : new Color(188, 200, 222));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active || getModel().isRollover()) {
                g.setColor(active ? UiTheme.PRIMARY : UiTheme.NAVY_LIGHT);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 11, 11);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class BrandPanel extends JPanel {

        BrandPanel() {
            setOpaque(false);
            setBorder(new EmptyBorder(82, 42, 42, 42));
            setLayout(new BorderLayout());
            JPanel top = new JPanel();
            top.setOpaque(false);
            top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
            top.add(label("LTM  /  CINEMA", 15, Font.BOLD, UiTheme.GOLD));
            top.add(Box.createVerticalStrut(80));
            top.add(label("Mỗi suất chiếu", 29, Font.BOLD, UiTheme.WHITE));
            top.add(Box.createVerticalStrut(6));
            top.add(label("một trải nghiệm mới.", 27, Font.BOLD, UiTheme.WHITE));
            top.add(Box.createVerticalStrut(18));
            JTextArea copy = new JTextArea("Khám phá phim hay, chọn chỗ ngồi yêu thích và giữ vé chỉ trong vài bước.");
            copy.setEditable(false);
            copy.setFocusable(false);
            copy.setOpaque(false);
            copy.setLineWrap(true);
            copy.setWrapStyleWord(true);
            copy.setForeground(new Color(202, 212, 233));
            copy.setFont(UiTheme.font(15, Font.PLAIN));
            copy.setAlignmentX(LEFT_ALIGNMENT);
            copy.setPreferredSize(new Dimension(310, 100));
            copy.setMaximumSize(new Dimension(310, 100));
            top.add(copy);
            add(top, BorderLayout.NORTH);
            add(label("KHÁM PHÁ   •   ĐẶT VÉ   •   THƯỞNG THỨC",
                    10, Font.BOLD, new Color(175, 190, 220)), BorderLayout.SOUTH);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, UiTheme.NAVY, getWidth(), getHeight(), UiTheme.PRIMARY_DARK));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(255, 255, 255, 14));
            g.fillOval(getWidth() - 210, 280, 330, 330);
            g.fillOval(-160, getHeight() - 250, 340, 340);
            g.setColor(new Color(255, 255, 255, 13));
            g.fillOval(getWidth() - 60, -70, 180, 180);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class StatusRenderer extends DefaultTableCellRenderer {

        @Override
        public java.awt.Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected, boolean focus, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            setBorder(new EmptyBorder(0, 12, 0, 8));
            setFont(UiTheme.font(12, Font.BOLD));
            boolean active = "Còn hiệu lực".equals(value);
            setForeground(active ? UiTheme.GREEN : UiTheme.RED);
            setBackground(selected ? UiTheme.PRIMARY_PALE
                    : (row % 2 == 0 ? UiTheme.WHITE : new Color(250, 251, 254)));
            return this;
        }
    }
}
