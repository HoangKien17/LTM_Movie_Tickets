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
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
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
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import protocol.Protocol;

/**
 * Giao diện Swing hiện đại; mọi thao tác dữ liệu vẫn đi qua MovieClient và TCP
 * Server.
 */
public final class BasicClientUI extends JFrame {

    static {
        UiTheme.install();
    }
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
    private final JLabel pageContext = label("", 12, Font.PLAIN, UiTheme.MUTED);

    private final JTextField loginName = UiTheme.field(24);
    private final JPasswordField loginPassword = UiTheme.password(24);
    private final JTextField registerName = UiTheme.field(24);
    private final JTextField registerEmail = UiTheme.field(24);
    private final JTextField registerPhone = UiTheme.field(24);
    private final JPasswordField registerPassword = UiTheme.password(24);

    private final JPanel movieGrid = new MovieGrid();
    private final List<MovieItem> movies = new ArrayList<>();
    private final JTextField movieSearch = UiTheme.hintField(20, "Tìm phim, thể loại...");
    private final CinemaBanner hero = new CinemaBanner();
    private final JPanel genreFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
    private String activeGenre = "";
    private final JPanel movieDetails = new JPanel(new BorderLayout(0, 16));
    private final JPanel timeSlots = new JPanel(new GridLayout(0, 3, 12, 12));
    private final JComboBox<String> showDates = new JComboBox<>();
    private final FoodOrderPanel foodOrder = new FoodOrderPanel(this::updateSelection);
    private final List<String[]> loadedShows = new ArrayList<>();
    private int heroIndex;
    private javax.swing.Timer heroTimer;
    private final Map<Integer, BigDecimal> showPrices = new LinkedHashMap<>();
    private final DefaultTableModel showModel = model("Mã", "Phim", "Phòng", "Bắt đầu", "Giá vé");
    private final JTable showTable = new JTable(showModel);
    private final JComboBox<String> showPicker = new JComboBox<>();
    private boolean updatingShowPicker;
    private final SeatMapPanel seatGrid = new SeatMapPanel();
    private final JLabel seatTitle = label("Chọn một suất chiếu để xem ghế", 16, Font.BOLD, UiTheme.TEXT);
    private final JLabel seatHint = label("Viền tím: còn trống   •   Cyan: đang chọn   •   Xám: đã đặt / đang giữ", 12, Font.PLAIN, UiTheme.MUTED);
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
    private JPanel staffNavGroup;
    private TicketCheckInPanel checkInPanel;
    private ServerMonitorPanel monitorPanel;
    private boolean staff;
    private String pendingElectronicTicket;

    private volatile MovieClient client;
    // Các trường trạng thái ghế chỉ được truy cập trên Swing EDT.
    private int seatMapShowId;
    private int pendingSeatRefreshShowId;
    private MovieClient disconnectedClient;
    private boolean busy;
    private boolean admin;
    private String activePage = "MOVIES";

    public BasicClientUI() {
        super("Galaxy Cinema");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1440, 920);
        setMinimumSize(new Dimension(1100, 760));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UiTheme.BACKGROUND);
        screens.add(buildAuth(false), "LOGIN");
        screens.add(buildAuth(true), "REGISTER");
        screens.add(buildMain(), "MAIN");
        add(screens, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UiTheme.NAVY);
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
        right.setBackground(UiTheme.BACKGROUND);
        JPanel card = new AuthCard();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(31, 34, 28, 34));
        card.setPreferredSize(new Dimension(390, cardHeight));
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);

        int row = 0;
        addAuthRow(content, authLabel("GALAXY CINEMA", 12, Font.BOLD, UiTheme.PRIMARY), row++, 13);
        addAuthRow(content, authLabel(heading, 25, Font.BOLD, UiTheme.TEXT), row++, 4);
        addAuthRow(content, authLabel(subtitle, 12, Font.PLAIN, UiTheme.MUTED), row++, 23);
        addAuthRow(content, form, row++, 20);

        if ("Đăng nhập".equals(actionText)) {
            JPanel options = new JPanel(new BorderLayout());
            options.setOpaque(false);
            JCheckBox remember = new JCheckBox("Ghi nhớ đăng nhập");
            remember.setOpaque(false);
            remember.setForeground(UiTheme.MUTED);
            remember.setFont(UiTheme.font(12, Font.PLAIN));
            remember.setFocusable(false);
            options.add(remember, BorderLayout.WEST);
            JButton help = new JButton("Quên mật khẩu?");
            help.setFont(UiTheme.font(12, Font.PLAIN));
            help.setForeground(UiTheme.PURPLE);
            help.setBorderPainted(false);
            help.setContentAreaFilled(false);
            help.addActionListener(event -> JOptionPane.showMessageDialog(this,
                    "Hãy đăng nhập bằng tài khoản hiện có, sau đó chọn Thông tin cá nhân để đổi mật khẩu.",
                    "Khôi phục tài khoản", JOptionPane.INFORMATION_MESSAGE));
            options.add(help, BorderLayout.EAST);
            addAuthRow(content, options, row++, 16);
        }

        JButton submit = UiTheme.primary(actionText);
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
        input.setBackground(UiTheme.BACKGROUND);
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
            g.setColor(c.hasFocus() ? UiTheme.PRIMARY : UiTheme.LINE);
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
            g.setColor(new Color(92, 226, 239, 20));
            g.fillRoundRect(7, 10, getWidth() - 14, getHeight() - 12, 20, 20);
            g.setPaint(new GradientPaint(0, 0, new Color(19, 30, 54), getWidth(), getHeight(), UiTheme.SURFACE));
            g.fillRoundRect(1, 1, getWidth() - 8, getHeight() - 10, 15, 15);
            g.setColor(new Color(102, 235, 255, 125));
            g.drawRoundRect(1, 1, getWidth() - 8, getHeight() - 10, 15, 15);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class CinemaPanel extends JPanel {

        private static final BufferedImage ARTWORK = loadArtwork();

        private CinemaPanel() {
            setOpaque(false);
        }

        private static BufferedImage loadArtwork() {
            try {
                return ImageIO.read(new File("assets/galaxy-cinema-auth.png"));
            } catch (IOException exception) {
                return null;
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g.setPaint(new GradientPaint(0, 0, new Color(10, 12, 29),
                    w, h, new Color(8, 28, 52)));
            g.fillRect(0, 0, w, h);
            g.setColor(new Color(125, 78, 220, 32));
            g.fillOval(-w / 3, -h / 4, w, h / 2);

            g.setFont(new Font("SansSerif", Font.BOLD, 25));
            g.setColor(UiTheme.PRIMARY);
            g.drawString("GALAXY CINEMA", 36, 70);
            g.setPaint(new GradientPaint(36, 95, new Color(255, 216, 154),
                    116, 95, UiTheme.PURPLE));
            g.fillRoundRect(36, 91, 80, 3, 3, 3);

            g.setFont(new Font("SansSerif", Font.BOLD, 30));
            g.setColor(Color.WHITE);
            g.drawString("Trải nghiệm", 36, 169);
            g.setColor(UiTheme.PRIMARY);
            g.drawString("màn ảnh rộng lớn.", 36, 208);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.setColor(new Color(203, 211, 230));
            g.drawString("Chọn phim, ghế và combo trong một", 36, 246);
            g.drawString("hành trình đặt vé liền mạch.", 36, 267);

            int artTop = 315;
            int artBottom = h - 56;
            if (ARTWORK != null && artBottom > artTop) {
                int availableHeight = artBottom - artTop;
                int artWidth = w + 24;
                int artHeight = Math.round(artWidth * (float) ARTWORK.getHeight() / ARTWORK.getWidth());
                int artX = (w - artWidth) / 2;
                int artY = artTop + (availableHeight - artHeight) / 2;
                g.drawImage(ARTWORK, artX, artY, artWidth, artHeight, null);
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
        // Giữ các nút sidebar ẩn để phân quyền/quy tắc điều hướng cũ không đổi;
        // khách hàng dùng thanh điều hướng ngang giống giao diện thiết kế mới.
        buildSidebar();
        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(buildHeader(), BorderLayout.NORTH);
        pages.setOpaque(false);
        pages.add(buildMovies(), "MOVIES");
        pages.add(buildMovieDetails(), "DETAIL");
        pages.add(buildShows(), "SHOWS");
        pages.add(buildBookings(), "BOOKINGS");
        pages.add(buildAdmin(), "ADMIN");
        checkInPanel = new TicketCheckInPanel(this::send,
                () -> !busy && staff && "CHECK_IN".equals(activePage));
        monitorPanel = new ServerMonitorPanel(this::send,
                () -> !busy && admin && "MONITOR".equals(activePage));
        pages.add(checkInPanel, "CHECK_IN");
        pages.add(monitorPanel, "MONITOR");
        right.add(pages, BorderLayout.CENTER);
        root.add(right, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(UiTheme.NAVY);
        sidebar.setPreferredSize(new Dimension(178, 0));
        sidebar.setBorder(new EmptyBorder(18, 14, 20, 14));
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JLabel mark = label("MENU", 10, Font.BOLD, UiTheme.MUTED);
        mark.setBorder(new EmptyBorder(0, 12, 0, 0));
        top.add(mark);
        top.add(Box.createVerticalStrut(20));
        top.add(sideCaption("KHÁM PHÁ"));
        top.add(Box.createVerticalStrut(9));
        top.add(nav("MOVIES", "01   Trang chủ"));
        top.add(Box.createVerticalStrut(6));
        top.add(nav("SHOWS", "02   Lịch chiếu"));
        top.add(Box.createVerticalStrut(6));
        top.add(nav("BOOKINGS", "03   Vé của tôi"));
        staffNavGroup = new JPanel();
        staffNavGroup.setOpaque(false);
        staffNavGroup.setLayout(new BoxLayout(staffNavGroup, BoxLayout.Y_AXIS));
        staffNavGroup.add(Box.createVerticalStrut(6));
        staffNavGroup.add(nav("CHECK_IN", "04   Soát vé"));
        staffNavGroup.setVisible(false);
        top.add(staffNavGroup);
        adminNavGroup = new JPanel();
        adminNavGroup.setOpaque(false);
        adminNavGroup.setLayout(new BoxLayout(adminNavGroup, BoxLayout.Y_AXIS));
        adminNavGroup.add(Box.createVerticalStrut(28));
        adminNavGroup.add(sideCaption("HỆ THỐNG"));
        adminNavGroup.add(Box.createVerticalStrut(9));
        adminNavGroup.add(nav("ADMIN", "05   Quản trị"));
        adminNavGroup.add(Box.createVerticalStrut(6));
        adminNavGroup.add(nav("MONITOR", "06   Giám sát server"));
        adminNavGroup.setVisible(false);
        top.add(adminNavGroup);
        sidebar.add(top, BorderLayout.NORTH);
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.add(label("GALAXY CINEMA", 12, Font.BOLD, UiTheme.PRIMARY));
        bottom.add(Box.createVerticalStrut(5));
        bottom.add(label("Đặt vé • QR • Tận hưởng", 10,
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
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setBackground(UiTheme.NAVY);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.LINE),
                new EmptyBorder(14, 27, 14, 27)));
        JPanel brand = new JPanel(new BorderLayout(8, 0));
        brand.setOpaque(false);
        JLabel orbit = label("G", 26, Font.BOLD, UiTheme.PRIMARY);
        orbit.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.PRIMARY, 2, true), new EmptyBorder(1, 6, 1, 6)));
        brand.add(orbit, BorderLayout.WEST);
        JLabel name = label("GALAXY CINEMA", 21, Font.BOLD, UiTheme.WHITE);
        brand.add(name, BorderLayout.CENTER);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        left.setOpaque(false);
        left.add(brand);
        left.add(headerNav("Trang chủ", "MOVIES"));
        left.add(headerNav("Phim", "MOVIES"));
        left.add(headerNav("Lịch chiếu", "SHOWS"));
        left.add(headerNav("Vé của tôi", "BOOKINGS"));
        header.add(left, BorderLayout.WEST);
        JPanel searchBox = new JPanel(new BorderLayout(9, 0));
        searchBox.setOpaque(false);
        JLabel glass = label("TÌM", 11, Font.BOLD, UiTheme.PRIMARY);
        searchBox.add(glass, BorderLayout.WEST);
        movieSearch.setPreferredSize(new Dimension(340, 42));
        movieSearch.setToolTipText("Tìm phim, thể loại...");
        searchBox.add(movieSearch, BorderLayout.CENTER);
        header.add(searchBox, BorderLayout.CENTER);
        JPanel userBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 0));
        userBox.setOpaque(false);
        UiTheme.StyledButton myTickets = action("Vé của tôi", () -> navigate("BOOKINGS"), "secondary");
        myTickets.setPreferredSize(new Dimension(104, 38));
        userBox.add(myTickets);
        pageContext.setHorizontalAlignment(SwingConstants.RIGHT);
        userBox.add(pageContext);
        userBox.add(userLabel);
        AvatarButton avatar = new AvatarButton();
        avatar.setToolTipText("Tài khoản • Thông tin cá nhân • Đăng xuất");
        JPopupMenu accountMenu = new JPopupMenu();
        JMenuItem profile = new JMenuItem("Thông tin cá nhân");
        profile.addActionListener(event -> openProfile());
        JMenuItem signOut = new JMenuItem("Đăng xuất");
        signOut.addActionListener(event -> logout());
        accountMenu.add(profile);
        accountMenu.addSeparator();
        accountMenu.add(signOut);
        avatar.addActionListener(event -> accountMenu.show(avatar, 0, avatar.getHeight() + 3));
        userBox.add(avatar);
        header.add(userBox, BorderLayout.EAST);
        return header;
    }

    private JButton headerNav(String text, String page) {
        JButton button = new JButton(text);
        button.setFont(UiTheme.font(12, Font.BOLD));
        button.setForeground(UiTheme.MUTED);
        button.setBorder(BorderFactory.createEmptyBorder(9, 5, 9, 5));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        button.addActionListener(event -> navigate(page));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent event) {
                button.setForeground(UiTheme.PRIMARY);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent event) {
                button.setForeground(UiTheme.MUTED);
            }
        });
        return button;
    }

    private JPanel buildMovies() {
        JPanel page = page();
        JPanel north = new JPanel(new BorderLayout(0, 18));
        north.setOpaque(false);
        north.add(new BookingSteps(0), BorderLayout.NORTH);
        north.add(hero, BorderLayout.CENTER);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(label("PHIM ĐANG CHIẾU", 19, Font.BOLD, UiTheme.TEXT));
        left.add(Box.createVerticalStrut(4));
        left.add(movieCount);
        head.add(left, BorderLayout.WEST);
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tools.setOpaque(false);
        movieSearch.getAccessibleContext().setAccessibleName("Tìm phim theo tên hoặc thể loại");
        tools.add(action("Xem tất cả", () -> {
            movieSearch.setText("");
            loadMovies();
        }, "secondary"));
        head.add(tools, BorderLayout.EAST);
        movieSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                renderMovies();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                renderMovies();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                renderMovies();
            }
        });
        genreFilters.setOpaque(false);
        JPanel discoveryHead = new JPanel(new BorderLayout(0, 9));
        discoveryHead.setOpaque(false);
        discoveryHead.add(head, BorderLayout.NORTH);
        discoveryHead.add(genreFilters, BorderLayout.SOUTH);
        north.add(discoveryHead, BorderLayout.SOUTH);
        movieGrid.setOpaque(false);
        movieGrid.setBorder(new EmptyBorder(2, 2, 10, 10));
        JPanel discovery = new MovieGrid();
        discovery.setLayout(new BorderLayout(0, 18));
        discovery.setOpaque(false);
        discovery.add(north, BorderLayout.NORTH);
        discovery.add(movieGrid, BorderLayout.CENTER);
        JScrollPane scroll = UiTheme.scroll(discovery);
        scroll.getViewport().setBackground(UiTheme.BACKGROUND);
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                renderMovies();
            }
        });
        page.add(scroll, BorderLayout.CENTER);
        JPanel heroActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        heroActions.setOpaque(false);
        heroActions.add(action("‹", () -> changeHero(-1), "subtle"));
        heroActions.add(action("›", () -> changeHero(1), "subtle"));
        hero.add(heroActions, BorderLayout.EAST);
        heroTimer = new javax.swing.Timer(7000, e -> {
            if ("MOVIES".equals(activePage) && hero.isShowing()) {
                changeHero(1);
            }
        });
        heroTimer.start();
        return page;
    }

    private void changeHero(int direction) {
        if (movies.isEmpty()) {
            return;
        }
        heroIndex = Math.floorMod(heroIndex + direction, movies.size());
        MovieItem movie = movies.get(heroIndex);
        hero.caption("PHIM NỔI BẬT  /  " + (heroIndex + 1) + " TRÊN " + movies.size());
        hero.movie(movie.title, movie.genre + "  •  " + movie.minutes + " phút", movie.poster,
                () -> openMovie(movie));
    }

    private JPanel buildMovieDetails() {
        JPanel page = page();
        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.setOpaque(false);
        top.add(new BookingSteps(1), BorderLayout.NORTH);
        movieDetails.setOpaque(false);
        top.add(movieDetails, BorderLayout.CENTER);
        page.add(top, BorderLayout.NORTH);
        UiTheme.RoundedPanel card = UiTheme.surface();
        card.setLayout(new BorderLayout(0, 16));
        JPanel controls = new JPanel(new BorderLayout(12, 0));
        controls.setOpaque(false);
        controls.add(label("CHỌN NGÀY & SUẤT CHIẾU", 15, Font.BOLD, UiTheme.TEXT), BorderLayout.WEST);
        showDates.addActionListener(e -> renderTimeSlots());
        UiTheme.styleCombo(showDates);
        showDates.setPreferredSize(new Dimension(170, 40));
        showDates.getAccessibleContext().setAccessibleName("Ngày xem phim");
        controls.add(showDates, BorderLayout.EAST);
        card.add(controls, BorderLayout.NORTH);
        timeSlots.setOpaque(false);
        JPanel slotsTop = new JPanel(new BorderLayout());
        slotsTop.setOpaque(false);
        slotsTop.add(timeSlots, BorderLayout.NORTH);
        card.add(UiTheme.scroll(slotsTop), BorderLayout.CENTER);
        page.add(card, BorderLayout.CENTER);
        return page;
    }

    private void openMovie(MovieItem movie) {
        if (busy) {
            return;
        }
        movieDetails.removeAll();
        CinemaBanner banner = new CinemaBanner();
        banner.caption("CHI TIẾT PHIM  /  GALAXY CINEMA");
        banner.movie(movie.title, movie.genre + "  •  " + movie.minutes + " phút  •  " + movie.release,
                movie.poster, () -> showDates.requestFocusInWindow());
        movieDetails.add(banner, BorderLayout.CENTER);
        JTextArea description = new JTextArea(movie.description);
        description.setFont(UiTheme.font(14, Font.PLAIN));
        description.setForeground(UiTheme.MUTED);
        description.setOpaque(false);
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setRows(3);
        JScrollPane descriptionScroll = UiTheme.scroll(description);
        descriptionScroll.setPreferredSize(new Dimension(0, 74));
        descriptionScroll.getViewport().setBackground(UiTheme.BACKGROUND);
        movieDetails.add(descriptionScroll, BorderLayout.SOUTH);
        navigate("DETAIL", false);
        loadShows(movie.id);
    }

    private void renderTimeSlots() {
        timeSlots.removeAll();
        String date = (String) showDates.getSelectedItem();
        for (int i = 0; i < loadedShows.size(); i++) {
            String[] row = loadedShows.get(i);
            if (date != null && !row[4].startsWith(date)) {
                continue;
            }
            final int index = i;
            UiTheme.RoundedPanel slot = UiTheme.surface();
            slot.setLayout(new BorderLayout(0, 10));
            String time = row[4].length() >= 16 ? row[4].substring(11, 16) : row[4];
            slot.add(label(time + "  •  " + row[3], 16, Font.BOLD, UiTheme.TEXT), BorderLayout.NORTH);
            slot.add(label(formatMoney(row[5]) + " / ghế", 13, Font.PLAIN, UiTheme.MUTED), BorderLayout.CENTER);
            slot.add(action("Chọn suất", () -> {
                if (busy) {
                    return;
                }
                navigate("SHOWS", false);
                showTable.setRowSelectionInterval(index, index);
            }, "secondary"), BorderLayout.SOUTH);
            timeSlots.add(slot);
        }
        if (timeSlots.getComponentCount() == 0) {
            timeSlots.add(label("Chưa có suất chiếu cho ngày này.", 14, Font.PLAIN, UiTheme.MUTED));
        }
        timeSlots.revalidate();
        timeSlots.repaint();
    }

    private JPanel buildShows() {
        JPanel page = page();
        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);
        toolbar.add(label("Suất chiếu", 15, Font.BOLD, UiTheme.TEXT), BorderLayout.WEST);
        showPicker.setPreferredSize(new Dimension(380, 42));
        UiTheme.styleCombo(showPicker);
        showPicker.getAccessibleContext().setAccessibleName("Suất chiếu");
        showPicker.setFont(UiTheme.font(13, Font.PLAIN));
        showPicker.addActionListener(e -> {
            if (!updatingShowPicker && showPicker.getSelectedIndex() >= 0) {
                showTable.setRowSelectionInterval(showPicker.getSelectedIndex(), showPicker.getSelectedIndex());
            }
        });
        toolbar.add(showPicker, BorderLayout.CENTER);
        toolbar.add(action("Xem tất cả", () -> loadShows(0), "secondary"), BorderLayout.EAST);
        JPanel heading = new JPanel(new BorderLayout(0, 12));
        heading.setOpaque(false);
        heading.add(new BookingSteps(2), BorderLayout.NORTH);
        heading.add(toolbar, BorderLayout.CENTER);
        top.add(heading, BorderLayout.NORTH);
        UiTheme.styleTable(showTable);
        showTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && showTable.getSelectedRow() >= 0) {
                int index = showTable.getSelectedRow();
                if (index < showPicker.getItemCount()) {
                    updatingShowPicker = true;
                    showPicker.setSelectedIndex(index);
                    showPicker.setToolTipText(showPicker.getItemAt(index));
                    updatingShowPicker = false;
                }
                loadSeats();
            }
        });
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
        seatHeader.add(titles, BorderLayout.CENTER);
        seatHeader.add(action("Tải lại ghế", this::loadSeats, "subtle"), BorderLayout.EAST);
        seatCard.add(seatHeader, BorderLayout.NORTH);
        seatGrid.setBackground(UiTheme.SURFACE);
        seatGrid.setBorder(new EmptyBorder(8, 5, 12, 5));
        JScrollPane seatScroll = UiTheme.scroll(seatGrid);
        JPanel selection = new JPanel(new BorderLayout(16, 0));
        selection.setOpaque(false);
        selection.add(seatScroll, BorderLayout.CENTER);
        selection.add(foodOrder, BorderLayout.EAST);
        seatCard.add(selection, BorderLayout.CENTER);
        JPanel summary = new JPanel(new BorderLayout(15, 0));
        summary.setOpaque(false);
        summary.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.LINE));
        JPanel sumText = new JPanel(new GridLayout(2, 1, 0, 3));
        sumText.setOpaque(false);
        sumText.setBorder(new EmptyBorder(12, 0, 0, 0));
        sumText.add(selectedSeats);
        sumText.add(totalPrice);
        summary.add(sumText, BorderLayout.CENTER);
        summary.add(action("Tiếp tục thanh toán", this::book, "primary"), BorderLayout.EAST);
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
        actions.add(action("Thanh toán", this::openSelectedPayment, "primary"));
        actions.add(action("Vé QR", () -> {
            String id = selectedBookingId();
            if (id != null) {
                electronicTicket(id);
            }
        }, "secondary"));
        actions.add(action("Hủy đơn", this::cancel, "danger"));
        head.add(actions, BorderLayout.SOUTH);
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
        javax.swing.JTabbedPane groups = new javax.swing.JTabbedPane();
        UiTheme.styleTabs(groups);
        groups.setOpaque(false);
        groups.add(adminGroup("Quản lý phim", "Nội dung đang chiếu",
                action("Danh sách", this::adminMovies, "subtle"),
                action("Thêm", this::adminAddMovie, "secondary"),
                action("Sửa", this::adminUpdateMovie, "subtle"),
                action("Xóa", () -> adminDelete("ADMIN_DELETE_MOVIE", "Mã phim", this::adminMovies), "danger")));
        groups.add(adminGroup("Phòng chiếu", "Không gian và số ghế",
                action("Danh sách", this::adminRooms, "subtle"),
                action("Thêm", this::adminAddRoom, "secondary"),
                action("Sửa phòng", this::adminUpdateRoom, "subtle"),
                action("Xóa", () -> adminDelete("ADMIN_DELETE_ROOM", "Mã phòng", this::adminRooms), "danger")));
        groups.add(adminGroup("Suất chiếu", "Lịch và giá vé",
                action("Danh sách", this::adminShows, "subtle"),
                action("Thêm", this::adminAddShow, "secondary"),
                action("Sửa", this::adminUpdateShow, "subtle"),
                action("Xóa", () -> adminDelete("ADMIN_DELETE_SHOW", "Mã suất chiếu", this::adminShows), "danger")));
        groups.add(adminGroup("Đồ ăn & nước uống", "Danh mục bán kèm",
                action("Danh sách", this::adminProducts, "subtle"),
                action("Thêm", this::adminAddProduct, "secondary"),
                action("Sửa", this::adminUpdateProduct, "subtle"),
                action("Ngừng bán", this::adminDeleteProduct, "danger")));
        groups.add(adminGroup("Báo cáo", "Người dùng, đơn vé và doanh thu",
                action("Người dùng", this::adminUsers, "subtle"),
                action("Đơn vé", this::adminBookings, "subtle"),
                action("Doanh thu", this::adminRevenue, "secondary"),
                action("Hủy đơn", this::adminCancel, "danger")));
        for (int i = 0; i < groups.getTabCount(); i++) {
            groups.setTitleAt(i, new String[]{"Phim", "Phòng chiếu", "Suất chiếu", "Đồ ăn & nước", "Báo cáo"}[i]);
        }
        groups.setFont(UiTheme.font(14, Font.BOLD));
        groups.setPreferredSize(new Dimension(0, 170));
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
        JPanel buttons = new JPanel(new GridLayout(1, 4, 10, 0));
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
        if (("ADMIN".equals(page) || "MONITOR".equals(page)) && !admin) {
            return;
        }
        if ("CHECK_IN".equals(page) && !staff) {
            return;
        }
        if (!"MONITOR".equals(page)) {
            monitorPanel.stop();
        }
        activePage = page;
        if (!"SHOWS".equals(page)) {
            pendingSeatRefreshShowId = 0;
        }
        pageLayout.show(pages, page);
        for (Map.Entry<String, NavButton> entry : navigation.entrySet()) {
            entry.getValue().setActive(entry.getKey().equals(page)
                    || "DETAIL".equals(page) && "MOVIES".equals(entry.getKey()));
        }
        switch (page) {
            case "DETAIL":
                pageTitle.setText("Chi tiết & suất chiếu");
                pageSubtitle.setText("Chọn ngày và giờ phù hợp với bạn.");
                break;
            case "MOVIES":
                pageTitle.setText("Khám phá phim");
                pageSubtitle.setText("Chọn một bộ phim để xem lịch chiếu.");
                if (fetch && movies.isEmpty()) {
                    loadMovies();
                }
                break;
            case "SHOWS":
                pageTitle.setText("Suất chiếu & ghế");
                pageSubtitle.setText("Giữ ghế khi tạo đơn. Vé được xác nhận sau khi thanh toán.");
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
            case "CHECK_IN":
                pageTitle.setText("Soát vé điện tử");
                pageSubtitle.setText("Kiểm tra mã và xác nhận khách vào rạp.");
                break;
            case "MONITOR":
                pageTitle.setText("Giám sát server");
                pageSubtitle.setText("Kết nối, yêu cầu và nhật ký hoạt động.");
                if (fetch) {
                    monitorPanel.start();
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
        pageContext.setText(pageTitle.getText());
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
            staff = admin || "staff".equals(dec(data[5]));
            adminNavGroup.setVisible(admin);
            staffNavGroup.setVisible(staff);
            loginPassword.setText("");
            screenLayout.show(screens, "MAIN");
            heroTimer.start();
            navigate("MOVIES");
            status.setText("Đã kết nối với máy chủ");
        });
    }

    private void logout() {
        send("LOGOUT", lines -> {
            if (!isOk(lines, "LOGOUT")) {
                return;
            }
            resetSession();
            screenLayout.show(screens, "LOGIN");
            status.setText("Đã đăng xuất");
        });
    }

    private void openProfile() {
        send("GET_PROFILE", lines -> {
            List<String[]> profiles = rows(lines, "PROFILE", "PROFILE", 5);
            if (profiles == null || profiles.isEmpty()) {
                return;
            }
            String[] profile = profiles.get(0);
            JTextField name = UiTheme.field(26);
            name.setText(profile[1]);
            JTextField email = UiTheme.field(26);
            email.setText(profile[2]);
            JTextField phone = UiTheme.field(26);
            phone.setText(profile[3]);
            JPasswordField current = UiTheme.password(26), replacement = UiTheme.password(26), confirm = UiTheme.password(26);
            JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
            form.setBorder(new EmptyBorder(12, 10, 12, 10));
            form.add(label("Mã tài khoản", 13, Font.PLAIN, UiTheme.MUTED));
            form.add(label("#" + profile[0], 13, Font.BOLD, UiTheme.TEXT));
            form.add(label("Vai trò", 13, Font.PLAIN, UiTheme.MUTED));
            form.add(label(profile[4], 13, Font.BOLD, UiTheme.TEXT));
            form.add(label("Họ và tên", 13, Font.PLAIN, UiTheme.TEXT));
            form.add(name);
            form.add(label("Email", 13, Font.PLAIN, UiTheme.TEXT));
            form.add(email);
            form.add(label("Số điện thoại", 13, Font.PLAIN, UiTheme.TEXT));
            form.add(phone);
            form.add(label("Mật khẩu hiện tại", 13, Font.PLAIN, UiTheme.TEXT));
            form.add(current);
            form.add(label("Mật khẩu mới (có thể để trống)", 13, Font.PLAIN, UiTheme.TEXT));
            form.add(replacement);
            form.add(label("Nhập lại mật khẩu mới", 13, Font.PLAIN, UiTheme.TEXT));
            form.add(confirm);
            if (JOptionPane.showConfirmDialog(this, form, "Thông tin cá nhân",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                return;
            }
            String oldPassword = new String(current.getPassword()), newPassword = new String(replacement.getPassword());
            if (oldPassword.isEmpty()) {
                showError("Hãy nhập mật khẩu hiện tại để lưu thay đổi");
                return;
            }
            if (!newPassword.equals(new String(confirm.getPassword()))) {
                showError("Mật khẩu mới nhập lại không khớp");
                return;
            }
            send("UPDATE_PROFILE;" + enc(name.getText().trim()) + ";" + enc(email.getText().trim()) + ";"
                    + enc(phone.getText().trim()) + ";" + enc(oldPassword) + ";" + enc(newPassword), response -> {
                List<String[]> updated = rows(response, "PROFILE", "PROFILE", 5);
                if (updated == null || updated.isEmpty()) {
                    return;
                }
                userLabel.setText("Xin chào, " + updated.get(0)[1]);
                info("Đã cập nhật thông tin cá nhân");
            });
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
                        dec(p[3]), p[4], p[5], dec(p[6]), decodePoster(p[7])));
            }
            renderMovies();
            heroIndex = 0;
            if (movies.isEmpty()) {
                hero.movie("HẸN BẠN TẠI RẠP", "Danh sách phim sẽ sớm được cập nhật.", null, null);
            } else {
                changeHero(0);
            }
            status.setText("Đã tải " + movies.size() + " phim");
        });
    }

    private void renderMovies() {
        movieGrid.removeAll();
        String query = movieSearch.getText().trim().toLowerCase(Locale.ROOT);
        renderGenreFilters();
        List<MovieItem> matches = new ArrayList<>();
        for (MovieItem movie : movies) {
            boolean matchesText = (movie.title + " " + movie.genre).toLowerCase(Locale.ROOT).contains(query);
            boolean matchesGenre = activeGenre.isEmpty()
                    || movie.genre.toLowerCase(Locale.ROOT).contains(activeGenre.toLowerCase(Locale.ROOT));
            if (matchesText && matchesGenre) {
                matches.add(movie);
            }
        }
        movieCount.setText(matches.size() + " phim • Chọn phim để xem lịch chiếu");
        int width = movieGrid.getParent() == null ? 1080 : Math.max(600, movieGrid.getParent().getWidth() - 30);
        int columns = Math.max(2, Math.min(4, width / 240));
        GridBagLayout gridLayout = (GridBagLayout) movieGrid.getLayout();
        gridLayout.columnWidths = null;
        gridLayout.columnWeights = new double[columns];
        java.util.Arrays.fill(gridLayout.columnWeights, 1);
        if (matches.isEmpty()) {
            GridBagConstraints c = new GridBagConstraints();
            c.weightx = 1;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            movieGrid.add(emptyState("Chưa tìm thấy phim", "Thử từ khóa khác hoặc tải lại danh sách."), c);
        } else {
            for (int i = 0; i < matches.size(); i++) {
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = i % columns;
                c.gridy = i / columns;
                c.weightx = 1.0 / columns;
                c.fill = GridBagConstraints.BOTH;
                c.anchor = GridBagConstraints.NORTHWEST;
                c.insets = new Insets(0, 0, 14, 12);
                JPanel card = movieCard(matches.get(i));
                card.setPreferredSize(new Dimension(width / columns - 14, 430));
                card.setMinimumSize(new Dimension(0, 430));
                movieGrid.add(card, c);
            }
            // Equal column widths even for an incomplete last row.
            GridBagConstraints space = new GridBagConstraints();
            space.gridy = (matches.size() + columns - 1) / columns;
            space.gridwidth = columns;
            space.weighty = 1;
            movieGrid.add(Box.createGlue(), space);
        }
        movieGrid.setPreferredSize(new Dimension(width, Math.max(1, (matches.size() + columns - 1) / columns) * 444 + 14));
        movieGrid.revalidate();
        movieGrid.repaint();
    }

    private void renderGenreFilters() {
        genreFilters.removeAll();
        java.util.LinkedHashSet<String> genres = new java.util.LinkedHashSet<>();
        for (MovieItem movie : movies) {
            if (movie.genre != null && !movie.genre.trim().isEmpty()) {
                genres.add(movie.genre.trim());
            }
        }
        genreFilters.add(genreFilter("Tất cả", ""));
        for (String genre : genres) {
            genreFilters.add(genreFilter(genre, genre));
        }
        genreFilters.revalidate();
        genreFilters.repaint();
    }

    private JButton genreFilter(String text, String value) {
        boolean selected = activeGenre.equalsIgnoreCase(value);
        UiTheme.StyledButton filter = selected ? UiTheme.primary(text) : UiTheme.secondary(text);
        filter.setFont(UiTheme.font(11, Font.BOLD));
        filter.setPreferredSize(new Dimension(Math.max(74, text.length() * 8 + 26), 32));
        filter.addActionListener(event -> {
            activeGenre = value;
            renderMovies();
        });
        return filter;
    }

    private JPanel movieCard(MovieItem movie) {
        UiTheme.RoundedPanel card = UiTheme.surface();
        card.setBorder(new EmptyBorder(10, 10, 14, 10));
        card.setLayout(new BorderLayout(0, 10));
        JPanel poster = movie.poster == null ? new UiTheme.PosterPanel(movie.title) : new PhotoPosterPanel(movie.poster);
        poster.setPreferredSize(new Dimension(150, 250));
        card.add(poster, BorderLayout.CENTER);
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel title = label(movie.title, 14, Font.BOLD, UiTheme.TEXT);
        title.setToolTipText(movie.title);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(title);
        info.add(Box.createVerticalStrut(4));
        JLabel meta = label(movie.genre + " • " + movie.minutes + " phút", 11, Font.PLAIN, UiTheme.MUTED);
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(meta);
        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        badges.setOpaque(false);
        badges.setAlignmentX(Component.LEFT_ALIGNMENT);
        badges.add(movieBadge("Đang chiếu"));
        badges.add(movieBadge(movie.genre));
        info.add(Box.createVerticalStrut(7));
        info.add(badges);
        info.add(Box.createVerticalStrut(8));
        UiTheme.StyledButton booking = action("Chọn suất  →", () -> openMovie(movie), "primary");
        booking.setAlignmentX(Component.LEFT_ALIGNMENT);
        booking.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        info.add(booking);
        card.add(info, BorderLayout.SOUTH);
        return card;
    }

    private JLabel movieBadge(String text) {
        JLabel badge = label(text, 10, Font.BOLD, UiTheme.PRIMARY);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.PRIMARY, 1, true), new EmptyBorder(3, 8, 3, 8)));
        return badge;
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
            loadedShows.clear();
            loadedShows.addAll(data);
            showDates.removeAllItems();
            updatingShowPicker = true;
            showPicker.removeAllItems();
            java.util.Set<String> dates = new java.util.TreeSet<>();
            for (String[] row : data) {
                if (row[4].length() >= 10) {
                    dates.add(row[4].substring(0, 10));
                }
                int id = Integer.parseInt(row[0]);
                showPrices.put(id, new BigDecimal(row[5]));
                showModel.addRow(new Object[]{row[0], row[2], row[3],
                    formatDate(row[4]), formatMoney(row[5])});
                showPicker.addItem(row[2] + " • " + row[3] + " • " + formatDate(row[4])
                        + " • " + formatMoney(row[5]));
            }
            showPicker.setSelectedIndex(-1);
            showPicker.setToolTipText("Chọn suất để tải sơ đồ ghế");
            updatingShowPicker = false;
            status.setText("Đã tải " + data.size() + " suất chiếu"
                    + (movieId == 0 ? "" : " cho phim #" + movieId));
            for (String date : dates) {
                showDates.addItem(date);
            }
            renderTimeSlots();
            foodOrder.reset();
            send("GET_PRODUCTS", response -> {
                List<String[]> products = rows(response, "PRODUCTS", "PRODUCT", 5);
                if (products != null) {
                    foodOrder.products(products);
                }
            });
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
        seatMapShowId = 0;
        pendingSeatRefreshShowId = 0;
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
        if (seatMapShowId != showId) {
            clearSeats();
        }
        // Nếu đang có request, giữ lại yêu cầu mới nhất thay vì bỏ qua thao tác chọn suất.
        pendingSeatRefreshShowId = showId;
        flushSeatRefresh();
    }

    private void onSeatsChanged(int showId) {
        if ("SHOWS".equals(activePage) && selectedShowId() == showId) {
            pendingSeatRefreshShowId = showId;
            flushSeatRefresh();
        }
    }

    private void flushSeatRefresh() {
        if (busy || pendingSeatRefreshShowId == 0 || client == null) {
            return;
        }
        int showId = pendingSeatRefreshShowId;
        pendingSeatRefreshShowId = 0;
        if (!"SHOWS".equals(activePage) || selectedShowId() != showId) {
            return;
        }
        // WATCH_SHOW trả snapshot đồng thời đăng ký nhận các thay đổi tiếp theo.
        send("WATCH_SHOW;" + showId, lines -> {
            List<String[]> data = rows(lines, "SEATS", "SEAT", 2);
            if (data == null || selectedShowId() != showId || !"SHOWS".equals(activePage)) {
                return;
            }
            // Đọc lựa chọn lúc áp dụng snapshot: người dùng có thể vừa chọn thêm ghế.
            List<String> selected = new ArrayList<>();
            if (seatMapShowId == showId) {
                for (UiTheme.SeatButton button : seatButtons) {
                    if (button.isSelected()) {
                        selected.add(button.getText());
                    }
                }
            }
            seatMapShowId = showId;
            seatTitle.setText("Sơ đồ ghế — suất #" + showId);
            seatButtons.clear();
            seatButtons.addAll(seatGrid.render(data, new HashSet<>(selected), this::updateSelection));
            updateSelection();
            status.setText("Đã cập nhật " + data.size() + " ghế — đồng bộ trực tiếp");
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
        BigDecimal seats = price == null ? BigDecimal.ZERO : price.multiply(BigDecimal.valueOf(codes.size()));
        totalPrice.setText(formatMoney(seats.add(foodOrder.total()).toPlainString()));
    }

    private void book() {
        if (busy) {
            status.setText("Hãy chờ cập nhật ghế hoàn tất");
            return;
        }
        int showId = selectedShowId();
        if (showId == 0) {
            showError("Hãy chọn một suất chiếu");
            return;
        }
        if (seatMapShowId != showId) {
            showError("Sơ đồ ghế đang tải. Hãy chờ rồi đặt vé.");
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
        if (!foodOrder.loaded()) {
            showError("Danh mục bắp nước chưa tải xong. Hãy bấm Xem tất cả để tải lại.");
            return;
        }
        String items = foodOrder.items();
        String summary = "Ghế: " + String.join(", ", codes) + "\n" + foodOrder.summary()
                + "\nTổng dự kiến: " + totalPrice.getText()
                + "\n\nGiữ ghế trong 10 phút và tạo mã thanh toán?";
        if (JOptionPane.showConfirmDialog(this, summary, "Xác nhận đơn",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        send("CREATE_ORDER;" + showId + ";" + enc(String.join(",", codes)) + ";" + enc(items), response -> {
            List<String[]> payments = rows(response, "PAYMENT", "PAYMENT", 8);
            if (payments == null || payments.size() != 1) {
                loadSeats();
                return;
            }
            showPayment(payments.get(0));
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
            if (pendingElectronicTicket != null) {
                String id = pendingElectronicTicket;
                pendingElectronicTicket = null;
                electronicTicket(id);
            }
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

    private void electronicTicket(String id) {
        send("GET_E_TICKET;" + id, lines -> {
            List<String[]> data = rows(lines, "E_TICKET", "TICKET", 10);
            if (data != null && data.size() == 1) {
                ElectronicTicketDialog.show(this, data.get(0));
            }
        });
    }

    private void openSelectedPayment() {
        String id = selectedBookingId();
        if (id == null) {
            return;
        }
        send("GET_PAYMENT;" + id, lines -> {
            List<String[]> data = rows(lines, "PAYMENT", "PAYMENT", 8);
            if (data != null && data.size() == 1) {
                showPayment(data.get(0));
            }
        });
    }

    private void showPayment(String[] payment) {
        if ("paid".equals(payment[2])) {
            pendingElectronicTicket = payment[0];
            navigate("BOOKINGS");
            return;
        }
        if (!"pending".equals(payment[2])) {
            showError("Đơn không còn chờ thanh toán");
            return;
        }
        send("GET_TICKET;" + payment[0], lines -> {
            List<String[]> data = rows(lines, "BOOKINGS", "BOOKING", 8);
            if (data == null || data.isEmpty()) {
                return;
            }
            String[] order = data.get(0);
            String summary = order[2] + "\n" + order[3] + " • " + formatDate(order[4])
                    + "\nGhế: " + order[7] + "\nTổng đã bao gồm đồ ăn/nước uống trong đơn.";
            boolean finished = PaymentDialog.show(this, payment, summary, this::send,
                    () -> afterPayment(payment[0]));
            if (!finished && "SHOWS".equals(activePage) && selectedShowId() != 0) {
                loadSeats();
            }
        });
    }

    private void afterPayment(String id) {
        pendingElectronicTicket = id;
        // A seat push can be in flight when the payment dialog closes.
        javax.swing.Timer ready = new javax.swing.Timer(100, null);
        ready.addActionListener(event -> {
            if (client == null || !id.equals(pendingElectronicTicket)) {
                ready.stop();
            } else if (!busy) {
                ready.stop();
                navigate("BOOKINGS");
            }
        });
        ready.setInitialDelay(0);
        ready.start();
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

    private void adminProducts() {
        send("ADMIN_GET_PRODUCTS", lines -> {
            List<String[]> data = rows(lines, "PRODUCTS", "PRODUCT", 5);
            if (data == null) {
                return;
            }
            List<Object[]> visible = new ArrayList<>();
            for (String[] row : data) {
                visible.add(new Object[]{row[0], row[1],
                    "food".equals(row[2]) ? "Đồ ăn" : "Nước uống", formatMoney(row[3]),
                    "1".equals(row[4]) ? "Đang bán" : "Ngừng bán"});
            }
            setAdminTable("Đồ ăn & nước uống", new String[]{"Mã", "Tên sản phẩm", "Loại", "Giá", "Trạng thái"}, visible);
        });
    }

    private void adminAddProduct() {
        String[] v = promptProduct(false);
        if (v == null) {
            return;
        }
        send("ADMIN_ADD_PRODUCT;" + enc(v[0]) + ";" + v[1] + ";" + v[2], lines -> {
            if (isOk(lines, "ADDED")) {
                info("Đã thêm sản phẩm");
                adminProducts();
            }
        });
    }

    private void adminUpdateProduct() {
        String[] v = promptProduct(true);
        if (v == null) {
            return;
        }
        send("ADMIN_UPDATE_PRODUCT;" + v[0] + ";" + enc(v[1]) + ";" + v[2] + ";" + v[3] + ";" + v[4], lines -> {
            if (isOk(lines, "UPDATED")) {
                info("Đã cập nhật sản phẩm");
                adminProducts();
            }
        });
    }

    private void adminDeleteProduct() {
        String id = JOptionPane.showInputDialog(this, "Mã sản phẩm cần ngừng bán");
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Ngừng bán sản phẩm #" + id.trim() + "?",
                "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        send("ADMIN_DELETE_PRODUCT;" + id.trim(), lines -> {
            if (isOk(lines, "DELETED")) {
                info("Đã ngừng bán sản phẩm #" + id.trim());
                adminProducts();
            }
        });
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
            adminStats.setText("Vé còn hiệu lực: " + data.get(0)[0]
                    + "    •    Doanh thu còn hiệu lực: " + formatMoney(data.get(0)[1]));
            status.setText("Đã tải thống kê");
        });
    }

    private void adminRevenue() {
        send("ADMIN_GET_REVENUE", lines -> {
            List<String[]> data = rows(lines, "REVENUE", "MOVIE_REVENUE", 3);
            if (data == null) {
                return;
            }
            List<Object[]> visible = new ArrayList<>();
            for (String[] row : data) {
                visible.add(new Object[]{row[0], row[1], formatMoney(row[2])});
            }
            setAdminTable("Doanh thu theo phim",
                    new String[]{"Tên phim", "Vé còn hiệu lực", "Doanh thu"}, visible);
            adminStats();
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
        MovieForm form = promptMovie("Thêm phim", "Tên phim", "Thể loại",
                "Thời lượng (phút)", "Mô tả");
        if (form == null) {
            return;
        }
        String posterData = posterPayload(form.posterFile);
        if (posterData == null) {
            return;
        }
        String[] v = form.values;
        send("ADMIN_ADD_MOVIE;" + enc(v[0]) + ";" + enc(v[1]) + ";" + v[2]
                + ";" + enc(v[3]) + ";" + posterData,
                lines -> {
                    if (isOk(lines, "ADDED")) {
                        movies.clear();
                        info("Đã thêm phim" + (form.posterFile == null ? "" : " và poster"));
                        adminMovies();
                    }
                });
    }

    private void adminUpdateMovie() {
        MovieForm form = promptMovie("Sửa phim", "Mã phim (bắt buộc)",
                "Tên phim mới (trống: giữ nguyên)",
                "Thể loại (trống: giữ nguyên)",
                "Thời lượng phút (trống: giữ nguyên)",
                "Mô tả (trống: giữ nguyên)");
        if (form == null) {
            return;
        }
        String posterData = posterPayload(form.posterFile);
        if (posterData == null) {
            return;
        }
        String[] v = form.values;
        send("ADMIN_UPDATE_MOVIE;" + v[0] + ";" + enc(v[1]) + ";" + enc(v[2])
                + ";" + v[3] + ";" + enc(v[4]) + ";" + posterData,
                lines -> {
                    if (isOk(lines, "UPDATED")) {
                        movies.clear();
                        info("Đã sửa phim" + (form.posterFile == null ? "" : " và poster"));
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
        String[] v = prompt("Sửa phòng", "Mã phòng",
                "Tên mới (trống: giữ nguyên)",
                "Số hàng mới (trống: giữ nguyên)",
                "Ghế mỗi hàng mới (trống: giữ nguyên)");
        if (v == null) {
            return;
        }
        if (v[1].isEmpty() && v[2].isEmpty() && v[3].isEmpty()) {
            showError("Hãy nhập tên mới hoặc số hàng và số ghế mỗi hàng");
            return;
        }
        if (v[2].isEmpty() != v[3].isEmpty()) {
            showError("Khi đổi số ghế, hãy nhập cả số hàng và số ghế mỗi hàng");
            return;
        }
        send("ADMIN_UPDATE_ROOM;" + v[0] + ";" + enc(v[1])
                + ";" + v[2] + ";" + v[3],
                lines -> {
                    if (isOk(lines, "UPDATED")) {
                        showModel.setRowCount(0);
                        clearSeats();
                        info("Đã cập nhật phòng chiếu");
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

    private String[] promptProduct(boolean updating) {
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBorder(new EmptyBorder(14, 10, 14, 10));
        JTextField id = UiTheme.field(24), name = UiTheme.field(24), price = UiTheme.field(24);
        JComboBox<String> type = new JComboBox<>(new String[]{"Đồ ăn", "Nước uống"});
        JComboBox<String> active = new JComboBox<>(new String[]{"Đang bán", "Ngừng bán"});
        UiTheme.styleCombo(type);
        UiTheme.styleCombo(active);
        if (updating) {
            form.add(label("Mã sản phẩm", 13, Font.PLAIN, UiTheme.TEXT));
            form.add(id);
        }
        form.add(label("Tên sản phẩm", 13, Font.PLAIN, UiTheme.TEXT));
        form.add(name);
        form.add(label("Loại", 13, Font.PLAIN, UiTheme.TEXT));
        form.add(type);
        form.add(label("Giá bán", 13, Font.PLAIN, UiTheme.TEXT));
        form.add(price);
        if (updating) {
            form.add(label("Trạng thái", 13, Font.PLAIN, UiTheme.TEXT));
            form.add(active);
        }
        if (JOptionPane.showConfirmDialog(this, form, updating ? "Sửa sản phẩm" : "Thêm sản phẩm",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return null;
        }
        String kind = type.getSelectedIndex() == 0 ? "food" : "drink";
        if (updating) {
            return new String[]{id.getText().trim(), name.getText().trim(), kind,
                price.getText().trim(), active.getSelectedIndex() == 0 ? "1" : "0"};
        }
        return new String[]{name.getText().trim(), kind, price.getText().trim()};
    }

    private MovieForm promptMovie(String title, String... labels) {
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBorder(new EmptyBorder(14, 10, 14, 10));
        JTextField[] fields = new JTextField[labels.length];
        for (int i = 0; i < labels.length; i++) {
            fields[i] = UiTheme.field(24);
            form.add(label(labels[i], 13, Font.PLAIN, UiTheme.TEXT));
            form.add(fields[i]);
        }
        File[] selected = new File[1];
        JLabel filename = label("Sửa phim".equals(title) ? "Giữ poster hiện tại" : "Chưa chọn ảnh",
                12, Font.PLAIN, UiTheme.MUTED);
        JButton choose = action("Chọn ảnh...", () -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Chọn poster cho phim");
            chooser.setFileFilter(new FileNameExtensionFilter("Ảnh JPG hoặc PNG", "jpg", "jpeg", "png"));
            chooser.setAcceptAllFileFilterUsed(false);
            if (chooser.showOpenDialog(form) == JFileChooser.APPROVE_OPTION) {
                selected[0] = chooser.getSelectedFile();
                filename.setText(selected[0].getName());
                filename.setToolTipText(selected[0].getAbsolutePath());
            }
        }, "subtle");
        JPanel picker = new JPanel(new BorderLayout(8, 0));
        picker.setOpaque(false);
        picker.add(choose, BorderLayout.WEST);
        picker.add(filename, BorderLayout.CENTER);
        form.add(label("Poster JPG/PNG (tối đa 2 MB)", 13, Font.PLAIN, UiTheme.TEXT));
        form.add(picker);
        if (JOptionPane.showConfirmDialog(this, form, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return null;
        }
        String[] values = new String[labels.length];
        for (int i = 0; i < labels.length; i++) {
            values[i] = fields[i].getText().trim();
        }
        return new MovieForm(values, selected[0]);
    }

    private String posterPayload(File file) {
        if (file == null) {
            return "";
        }
        try {
            if (!file.isFile() || Files.size(file.toPath()) > 2L * 1024 * 1024) {
                showError("Ảnh poster phải là JPG/PNG và không quá 2 MB");
                return null;
            }
            byte[] bytes = Files.readAllBytes(file.toPath());
            if (bytes.length == 0 || bytes.length > 2 * 1024 * 1024) {
                showError("Ảnh poster phải là JPG/PNG và không quá 2 MB");
                return null;
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (IOException ex) {
            showError("Không đọc được ảnh poster: " + ex.getMessage());
            return null;
        }
    }

    private static BufferedImage decodePoster(String payload) {
        if (payload == null || payload.isEmpty() || payload.length() > 400_000) {
            return null;
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(Base64.getUrlDecoder().decode(payload)));
        } catch (IOException | IllegalArgumentException ex) {
            return null;
        }
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
                    Throwable cause = ex instanceof ExecutionException ? ex.getCause() : ex;
                    resetSession();
                    screenLayout.show(screens, "LOGIN");
                    showError("Không liên hệ được máy chủ: " + cause.getMessage());
                } finally {
                    // Không bỏ mất EOF hoặc event đến khi request đang chạy.
                    handlePendingDisconnect();
                    flushSeatRefresh();
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
        MovieClient connected = new MovieClient(host, port, MovieClient.DEFAULT_TIMEOUT_MS);
        connected.setSeatChangeListener(showId -> SwingUtilities.invokeLater(() -> {
            if (client == connected) {
                onSeatsChanged(showId);
            }
        }));
        connected.setDisconnectListener(error -> SwingUtilities.invokeLater(() -> {
            if (client == connected) {
                disconnectedClient = connected;
                handlePendingDisconnect();
            }
        }));
        return connected;
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
        JOptionPane.showMessageDialog(this, message, "Galaxy Cinema", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handlePendingDisconnect() {
        if (!busy && disconnectedClient != null && client == disconnectedClient) {
            resetSession();
            screenLayout.show(screens, "LOGIN");
            showError("Mất kết nối máy chủ. Hãy đăng nhập lại.");
        }
    }

    private void resetSession() {
        staff = false;
        staffNavGroup.setVisible(false);
        pendingElectronicTicket = null;
        checkInPanel.reset();
        monitorPanel.reset();
        disconnectedClient = null;
        closeClient();
        admin = false;
        adminNavGroup.setVisible(false);
        activePage = "MOVIES";
        movies.clear();
        movieSearch.setText("");
        foodOrder.reset();
        loadedShows.clear();
        movieDetails.removeAll();
        timeSlots.removeAll();
        hero.movie("ĐIỆN ẢNH THEO CÁCH CỦA BẠN", "Chọn phim. Chọn ghế. Tận hưởng.", null, null);
        showPrices.clear();
        movieGrid.removeAll();
        showTable.clearSelection();
        showModel.setRowCount(0);
        updatingShowPicker = true;
        showPicker.removeAllItems();
        updatingShowPicker = false;
        bookingModel.setRowCount(0);
        adminModel.setRowCount(0);
        clearSeats();
    }

    private void closeClient() {
        if (heroTimer != null) {
            heroTimer.stop();
        }
        if (monitorPanel != null) {
            monitorPanel.stop();
        }
        pendingSeatRefreshShowId = 0;
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
        label.setBorder(new EmptyBorder(0, 0, 0, 12));
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, size + 12));
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

    static String formatMoney(String value) {
        try {
            NumberFormat format = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            format.setMaximumFractionDigits(2);
            return format.format(new BigDecimal(value)) + " ₫";
        } catch (Exception ex) {
            return value + " ₫";
        }
    }

    private static String statusText(String value) {
        switch (value) {
            case "active":
                return "Đã thanh toán";
            case "pending_payment":
                return "Chờ thanh toán";
            case "expired":
                return "Hết hạn";
            default:
                return "Đã hủy";
        }
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

    private static final class MovieForm {

        private final String[] values;
        private final File posterFile;

        private MovieForm(String[] values, File posterFile) {
            this.values = values;
            this.posterFile = posterFile;
        }
    }

    private static final class PhotoPosterPanel extends JPanel {

        private final BufferedImage poster;

        private PhotoPosterPanel(BufferedImage poster) {
            this.poster = poster;
            setOpaque(false);
            setPreferredSize(new Dimension(122, 170));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setClip(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 18, 18));
                double scale = Math.max(getWidth() / (double) poster.getWidth(), getHeight() / (double) poster.getHeight());
                int w = (int) (poster.getWidth() * scale), h = (int) (poster.getHeight() * scale);
                g.drawImage(poster, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
            } finally {
                g.dispose();
            }
        }
    }

    private static final class MovieItem {

        private final int id;
        private final String title;
        private final String genre;
        private final String minutes;
        private final String release;
        private final String description;
        private final BufferedImage poster;

        private MovieItem(int id, String title, String genre, String minutes,
                String release, String description, BufferedImage poster) {
            this.id = id;
            this.title = title;
            this.genre = genre;
            this.minutes = minutes;
            this.release = release;
            this.description = description;
            this.poster = poster;
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

    /**
     * Nút tài khoản tự vẽ để không phụ thuộc file ảnh hoặc font emoji.
     */
    private static final class AvatarButton extends JButton {

        AvatarButton() {
            setPreferredSize(new Dimension(38, 38));
            setToolTipText("Mở menu tài khoản");
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getModel().isRollover() ? UiTheme.PRIMARY : UiTheme.PRIMARY_PALE);
            g.fillOval(1, 1, getWidth() - 2, getHeight() - 2);
            g.setColor(getModel().isRollover() ? Color.WHITE : UiTheme.PRIMARY);
            int cx = getWidth() / 2;
            g.fillOval(cx - 5, 8, 10, 10);
            g.fillRoundRect(cx - 9, 20, 18, 11, 9, 9);
            g.dispose();
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
            setForeground(value ? UiTheme.PRIMARY : new Color(188, 200, 222));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active || getModel().isRollover()) {
                g.setColor(active ? UiTheme.PRIMARY_PALE : UiTheme.NAVY_LIGHT);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 11, 11);
                if (active) {
                    g.setColor(UiTheme.PRIMARY);
                    g.fillRoundRect(0, 10, 3, getHeight() - 20, 3, 3);
                }
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
            top.add(label("GALAXY CINEMA", 15, Font.BOLD, UiTheme.GOLD));
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
            boolean active = "Đã thanh toán".equals(value);
            setForeground(active ? UiTheme.GREEN : "Chờ thanh toán".equals(value) ? UiTheme.GOLD : UiTheme.RED);
            setBackground(selected ? UiTheme.PRIMARY_PALE
                    : (row % 2 == 0 ? UiTheme.SURFACE : UiTheme.NAVY));
            return this;
        }
    }
}
