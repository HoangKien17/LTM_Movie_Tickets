package web;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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
import javax.swing.JFileChooser;
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
    private final JPanel footer = new JPanel(new BorderLayout());
    private final JLabel status = new JLabel("Sẵn sàng");
    private final JLabel pageTitle = label("", 23, Font.BOLD, UiTheme.TEXT);
    private final JLabel pageSubtitle = label("", 13, Font.PLAIN, UiTheme.MUTED);
    private final JLabel userLabel = label("", 13, Font.BOLD, UiTheme.TEXT);
    private final JLabel movieCount = label("", 13, Font.PLAIN, UiTheme.MUTED);
    private final JLabel bookingCount = label("", 13, Font.PLAIN, UiTheme.MUTED);
    private final JLabel pageContext = label("", 12, Font.PLAIN, UiTheme.MUTED);

    private AppWindowChrome chrome;
    private final NotificationCenter notificationCenter = new NotificationCenter();
    private final javax.swing.Timer notificationTimer = new javax.swing.Timer(30_000, e -> refreshNotifications());
    private boolean loggedIn, notificationRefreshPending;
    private String selectedNotificationBooking;
    private final AccountMenu accountMenu = new AccountMenu(this::openProfile, this::logout);
    private ProfileDialog profileDialog;

    private final JCheckBox rememberLogin = new JCheckBox("Ghi nhớ tài khoản");
    private final ShowtimePanel homeShowtimes = new ShowtimePanel(this::chooseShow);
    private MovieItem activeMovie;
    private final JLabel bookingFilm = label("Chưa chọn suất chiếu", 15, Font.BOLD, UiTheme.TEXT);
    private final JLabel bookingMeta = label("Chọn phim và suất chiếu để bắt đầu", 12, Font.PLAIN, UiTheme.MUTED);
    private final JLabel ticketSubtotal = label("0 ₫", 14, Font.BOLD, UiTheme.TEXT);
    private final JLabel foodSubtotal = label("0 ₫", 14, Font.BOLD, UiTheme.TEXT);
    private final JTextField loginName = UiTheme.field(24);
    private final JPasswordField loginPassword = UiTheme.password(24);
    private final JTextField registerName = UiTheme.field(24);
    private final JTextField registerEmail = UiTheme.field(24);
    private final JTextField registerPhone = UiTheme.field(24);
    private final JPasswordField registerPassword = UiTheme.password(24);
    private final JTextField recoveryEmail = UiTheme.field(24);
    private final JTextField recoveryCode = UiTheme.field(8);
    private final JPasswordField recoveryPassword = UiTheme.password(24);
    private final JPasswordField recoveryConfirm = UiTheme.password(24);
    private String pendingRecoveryEmail = "";
    private AuthScreen resetCodeScreen;

    private final JPanel movieGrid = new MovieGrid();
    private final Map<Integer, PosterView> moviePosters = new LinkedHashMap<>();
    private int movieViewportWidth;
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
    private javax.swing.JTabbedPane adminTabs;

    private enum AdminView {
        MOVIES, ROOMS, SHOWS, PRODUCTS, USERS, BOOKINGS, REVENUE
    }
    private AdminView adminView = AdminView.MOVIES;
    private AdminView adminReportView = AdminView.BOOKINGS;
    private long adminViewRevision;
    private boolean adminRefreshPending, adminStatsPending, updatingAdminTabs;
    private String adminFocusId;
    private final JLabel adminFeedback = label(" ", 13, Font.PLAIN, UiTheme.GREEN);
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
        setUndecorated(true);
        chrome = new AppWindowChrome(this);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1440, 900);
        setMinimumSize(new Dimension(1100, 760));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UiTheme.BACKGROUND);
        screens.add(buildAuth(false), "LOGIN");
        screens.add(buildAuth(true), "REGISTER");
        screens.add(new AuthScreen(AuthScreen.Mode.RESET_EMAIL, new JTextField[]{recoveryEmail}, new JCheckBox(),
                this::returnFromRecovery, this::showRecoveryEmail, this::requestRecoveryCode, () -> {
                }), "RESET_EMAIL");
        resetCodeScreen = new AuthScreen(AuthScreen.Mode.RESET_PASSWORD,
                new JTextField[]{recoveryCode, recoveryPassword, recoveryConfirm}, new JCheckBox(),
                this::returnFromRecovery, this::showRecoveryEmail, this::confirmRecovery, this::resendRecoveryCode);
        screens.add(resetCodeScreen, "RESET_PASSWORD");
        screens.add(buildMain(), "MAIN");
        add(screens, BorderLayout.CENTER);
        add(chrome, BorderLayout.NORTH);
        footer.setBackground(UiTheme.NAVY);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.LINE),
                new EmptyBorder(5, 18, 5, 18)));
        status.setFont(UiTheme.font(12, Font.PLAIN));
        status.setForeground(UiTheme.MUTED);
        footer.add(status, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
        showAuth("LOGIN");
        loginPassword.addActionListener(event -> login());
        loginName.addActionListener(event -> login());
        registerPassword.addActionListener(event -> register());
        recoveryEmail.addActionListener(event -> requestRecoveryCode());
        recoveryCode.addActionListener(event -> confirmRecovery());
        recoveryPassword.addActionListener(event -> recoveryConfirm.requestFocusInWindow());
        recoveryConfirm.addActionListener(event -> confirmRecovery());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                closeClient();
            }
        });
    }

    private JPanel buildAuth(boolean registering) {
        JTextField[] fields = registering
                ? new JTextField[]{registerName, registerEmail, registerPhone, registerPassword}
                : new JTextField[]{loginName, loginPassword};
        return new AuthScreen(registering, fields, rememberLogin,
                () -> showAuth("LOGIN"), () -> showAuth("REGISTER"),
                registering ? this::register : this::login,
                this::openRecovery);
    }

    private void clearRecoverySecrets() {
        recoveryCode.setText("");
        recoveryPassword.setText("");
        recoveryConfirm.setText("");
    }

    private void openRecovery() {
        String email = loginName.getText().trim();
        recoveryEmail.setText(utils.PasswordUtil.isValidEmail(email) ? email : "");
        showRecoveryEmail();
    }

    private void showRecoveryEmail() {
        clearRecoverySecrets();
        showAuth("RESET_EMAIL");
        recoveryEmail.requestFocusInWindow();
    }

    private void returnFromRecovery() {
        clearRecoverySecrets();
        pendingRecoveryEmail = "";
        showAuth("LOGIN");
    }

    private void requestRecoveryCode() {
        String email = recoveryEmail.getText().trim();
        if (email.length() > 100 || !utils.PasswordUtil.isValidEmail(email)) {
            showError("Hãy nhập địa chỉ email hợp lệ đã đăng ký.");
            return;
        }
        send("REQUEST_PASSWORD_RESET;" + enc(email), lines -> {
            if (!isOk(lines, "PASSWORD_RESET_REQUESTED")) {
                return;
            }
            pendingRecoveryEmail = email;
            clearRecoverySecrets();
            resetCodeScreen.setRecoveryEmail(email);
            resetCodeScreen.startResendCooldown();
            showAuth("RESET_PASSWORD");
            recoveryCode.requestFocusInWindow();
        });
    }

    private void resendRecoveryCode() {
        if (pendingRecoveryEmail.isEmpty()) {
            showRecoveryEmail();
            return;
        }
        send("REQUEST_PASSWORD_RESET;" + enc(pendingRecoveryEmail), lines -> {
            if (!isOk(lines, "PASSWORD_RESET_REQUESTED")) {
                return;
            }
            recoveryCode.setText("");
            resetCodeScreen.startResendCooldown();
            info(service.PasswordResetService.REQUEST_MESSAGE);
        });
    }

    private void confirmRecovery() {
        if (pendingRecoveryEmail.isEmpty()) {
            showRecoveryEmail();
            return;
        }
        String code = recoveryCode.getText().trim();
        String password = new String(recoveryPassword.getPassword());
        String confirmation = new String(recoveryConfirm.getPassword());
        if (!code.matches("[0-9]{6}")) {
            showError("Mã xác nhận phải gồm 6 chữ số.");
            return;
        }
        if (password.length() < 6 || password.length() > 128) {
            showError("Mật khẩu mới phải có từ 6 đến 128 ký tự.");
            return;
        }
        if (!password.equals(confirmation)) {
            showError("Mật khẩu nhập lại không khớp.");
            return;
        }
        final String email = pendingRecoveryEmail;
        send("RESET_PASSWORD;" + enc(email) + ";" + enc(code) + ";" + enc(password), lines -> {
            if (!isOk(lines, "PASSWORD_RESET")) {
                return;
            }
            clearRecoverySecrets();
            pendingRecoveryEmail = "";
            loginName.setText(email);
            loginPassword.setText("");
            showAuth("LOGIN");
            info("Đặt lại mật khẩu thành công. Hãy đăng nhập bằng mật khẩu mới.");
        });
    }

    private void showAuth(String page) {
        footer.setVisible(false);
        chrome.setVisible(true);
        screenLayout.show(screens, page);
        revalidate();
    }

    private void setAuthBusy(boolean waiting) {
        for (Component page : screens.getComponents()) {
            if (page instanceof AuthScreen) {
                ((AuthScreen) page).setBusy(waiting);
            }
        }
    }

    private JPanel buildMain() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UiTheme.BACKGROUND);
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

    private JPanel buildHeader() {
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        nav.setOpaque(false);
        nav.add(headerNav("Trang chủ", "MOVIES"));
        nav.add(headerNav("Lịch chiếu", "SHOWS"));
        nav.add(headerNav("Vé của tôi", "BOOKINGS"));
        staffNavGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        staffNavGroup.setOpaque(false);
        staffNavGroup.add(headerNav("Soát vé", "CHECK_IN"));
        staffNavGroup.setVisible(false);
        adminNavGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        adminNavGroup.setOpaque(false);
        adminNavGroup.add(headerNav("Hoạt động phiên", "MONITOR"));
        adminNavGroup.add(headerNav("Quản lý & báo cáo", "ADMIN"));
        adminNavGroup.setVisible(false);
        nav.add(staffNavGroup);
        nav.add(adminNavGroup);
        JButton account = new JButton("Tài khoản") {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                HomeIcons.icon("chevron", 14).paintIcon(this, graphics, getWidth() - 15, (getHeight() - 14) / 2);
            }
        };
        account.setContentAreaFilled(false);
        account.setBorderPainted(false);
        account.setFocusPainted(false);
        account.setBorder(new EmptyBorder(0, 0, 0, 19));
        account.setForeground(UiTheme.TEXT);
        account.setName("account-menu-button");
        account.setIcon(HomeIcons.icon("user", 26));
        account.setIconTextGap(9);
        account.setFont(UiTheme.font(14, Font.PLAIN));
        userLabel.addPropertyChangeListener("text", e -> {
            account.setText(userLabel.getText());
            account.setToolTipText(userLabel.getText() + " • Mở menu tài khoản");
        });
        account.addActionListener(e -> accountMenu.show(account,
                Math.min(0, account.getWidth() - accountMenu.getPreferredSize().width), account.getHeight() + 12));
        notificationCenter.actions(this::refreshNotifications, this::openNotification,
                ids -> markNotifications(ids, () -> {
                }));
        HomeHeader header = new HomeHeader(nav, movieSearch, notificationCenter, account, chrome.embeddedControls());
        chrome.attachDrag(header);
        chrome.attachDrag(header.brand());
        return header;
    }

    private JButton headerNav(String text, String page) {
        NavButton button = new NavButton(text);
        String icon = "MOVIES".equals(page) ? "home" : "SHOWS".equals(page) ? "calendar" : "BOOKINGS".equals(page) ? "ticket" : "CHECK_IN".equals(page) ? "check" : "ADMIN".equals(page) ? "manage" : "monitor";
        button.setIcon(HomeIcons.icon(icon, 20));
        button.setIconTextGap(9);
        button.setPreferredSize(new Dimension(Math.max(120, button.getFontMetrics(button.getFont()).stringWidth(text) + 57), 80));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setName("nav-" + page.toLowerCase(Locale.ROOT));
        button.addActionListener(e -> navigate(page));
        navigation.put(page, button);
        return button;
    }

    private void refreshNotifications() {
        if (!loggedIn || client == null) {
            return;
        }
        if (busy) {
            notificationRefreshPending = true;
            return;
        }
        notificationRefreshPending = false;
        send("GET_NOTIFICATIONS", lines -> {
            List<String[]> data = rows(lines, "NOTIFICATIONS", "NOTIFICATION", 9);
            if (data != null) {
                notificationCenter.update(data);
            }
        });
    }

    private void markNotifications(String ids, Runnable after) {
        if (busy || !loggedIn || ids.isEmpty()) {
            return;
        }
        send("MARK_NOTIFICATIONS_READ;" + enc(ids), lines -> {
            if (!isOk(lines, "NOTIFICATIONS_READ")) {
                return;
            }
            notificationCenter.markLocally(ids);
            after.run();
        });
    }

    private void openNotification(String id, String bookingId) {
        markNotifications(id, () -> {
            selectedNotificationBooking = bookingId;
            navigate("BOOKINGS");
        });
    }

    private JPanel buildMovies() {
        JPanel page = page();
        page.setBorder(new EmptyBorder(18, 24, 18, 24));
        JPanel north = new JPanel(new BorderLayout(0, 18));
        north.setOpaque(false);
        north.add(hero, BorderLayout.CENTER);
        JPanel head = new JPanel(new BorderLayout()) {
            @Override
            public void doLayout() {
                Component filter = ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.EAST);
                if (filter != null) {
                    filter.setPreferredSize(new Dimension(Math.max(180, Math.min(630, getWidth() - 300)), 47));
                }
                super.doLayout();
            }
        };
        head.setOpaque(false);
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(label("Phim đang chiếu", 28, Font.BOLD, UiTheme.TEXT));
        left.add(Box.createVerticalStrut(4));
        left.add(movieCount);
        head.add(left, BorderLayout.WEST);
        movieSearch.getAccessibleContext().setAccessibleName("Tìm phim theo tên hoặc thể loại");
        movieSearch.addActionListener(e -> navigate("MOVIES", false));
        genreFilters.setOpaque(false);
        JScrollPane filterScroll = UiTheme.scroll(genreFilters);
        filterScroll.setOpaque(false);
        filterScroll.getViewport().setOpaque(false);
        filterScroll.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        filterScroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        filterScroll.setPreferredSize(new Dimension(510, 47));
        head.add(filterScroll, BorderLayout.EAST);
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
                int width = event.getComponent().getWidth();
                if (width != movieViewportWidth) {
                    movieViewportWidth = width;
                    renderMovies();
                }
            }
        });
        JPanel columns = new JPanel(new BorderLayout(12, 0)) {
            @Override
            public void doLayout() {
                int side = Math.max(292, Math.min(424, (int) (getWidth() * .265)));
                homeShowtimes.setPreferredSize(new Dimension(side, 0));
                hero.setPreferredSize(new Dimension(500, Math.max(255, Math.min(322, (int) ((getWidth() - side - 12) * .27)))));
                super.doLayout();
            }
        };
        columns.setOpaque(false);
        columns.add(scroll, BorderLayout.CENTER);
        columns.add(homeShowtimes, BorderLayout.EAST);
        page.add(columns, BorderLayout.CENTER);
        heroTimer = new javax.swing.Timer(7000, e -> {
            if ("MOVIES".equals(activePage) && hero.isShowing() && hero.canRotate()) {
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
                () -> selectMovie(movie));
        hero.description(movie.description);
    }

    private JPanel buildMovieDetails() {
        JPanel page = page();
        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.setOpaque(false);
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

    private void selectMovie(MovieItem movie) {
        if (busy) {
            return;
        }
        activeMovie = movie;
        homeShowtimes.movie(movie.id, movie.title, movie.genre + " • " + movie.minutes + " phút", movie.poster);
        hero.movie(movie.title, movie.genre + " • " + movie.minutes + " phút", movie.poster,
                () -> selectMovie(movie));
        hero.description(movie.description);
        // Keep the cards (and their focus/scroll position) while selecting a film.
        for (Map.Entry<Integer, PosterView> entry : moviePosters.entrySet()) {
            entry.getValue().putClientProperty("selected", entry.getKey() == movie.id);
            entry.getValue().repaint();
        }
        loadShows(movie.id);
    }

    private void chooseShow(int showId) {
        if (busy) {
            return;
        }
        for (int i = 0; i < showModel.getRowCount(); i++) {
            if (Integer.toString(showId).equals(showModel.getValueAt(i, 0).toString())) {
                navigate("SHOWS", false);
                showTable.setRowSelectionInterval(i, i);
                return;
            }
        }
        showError("Suất chiếu đã thay đổi. Hãy chọn lại phim để tải lịch mới.");
    }

    private void openMovie(MovieItem movie) {
        if (busy) {
            return;
        }
        activeMovie = movie;
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
        seatCard.setLayout(new BorderLayout(0, 16));
        JPanel seatHeader = new JPanel(new BorderLayout(10, 0));
        seatHeader.setOpaque(false);
        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
        titles.setOpaque(false);
        titles.add(label("Chọn chỗ ngồi của bạn", 23, Font.BOLD, UiTheme.TEXT));
        titles.add(seatTitle);
        seatTitle.setFont(UiTheme.font(12, Font.PLAIN));
        seatTitle.setForeground(UiTheme.MUTED);
        seatHeader.add(titles, BorderLayout.CENTER);
        seatHeader.add(action("Tải lại", this::loadSeats, "subtle"), BorderLayout.EAST);
        seatCard.add(seatHeader, BorderLayout.NORTH);
        seatGrid.setOpaque(false);
        JScrollPane seatScroll = UiTheme.scroll(seatGrid);
        seatScroll.getViewport().setBackground(UiTheme.SURFACE);
        seatCard.add(seatScroll, BorderLayout.CENTER);
        JPanel legend = new JPanel(new BorderLayout(0, 10));
        legend.setOpaque(false);
        JPanel swatches = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 2));
        swatches.setOpaque(false);
        swatches.add(UiTheme.legend("Còn trống", UiTheme.PRIMARY));
        swatches.add(UiTheme.legend("Đang chọn", UiTheme.SELECTED));
        swatches.add(UiTheme.legend("Đã đặt / đang giữ", UiTheme.MUTED));
        legend.add(swatches, BorderLayout.CENTER);
        JLabel limit = label("Bạn có thể chọn tối đa 10 ghế mỗi đơn", 11, Font.PLAIN, UiTheme.MUTED);
        limit.setHorizontalAlignment(SwingConstants.CENTER);
        legend.add(limit, BorderLayout.SOUTH);
        seatCard.add(legend, BorderLayout.SOUTH);

        UiTheme.RoundedPanel summary = UiTheme.glass();
        summary.setPreferredSize(new Dimension(320, 0));
        summary.setLayout(new BorderLayout(0, 16));
        JPanel movie = new JPanel(new GridLayout(3, 1, 0, 6));
        movie.setOpaque(false);
        movie.add(label("Thông tin đặt vé", 19, Font.BOLD, UiTheme.TEXT));
        movie.add(bookingFilm);
        movie.add(bookingMeta);
        summary.add(movie, BorderLayout.NORTH);
        summary.add(foodOrder, BorderLayout.CENTER);
        JPanel bill = new JPanel();
        bill.setOpaque(false);
        bill.setLayout(new BoxLayout(bill, BoxLayout.Y_AXIS));
        selectedSeats.setForeground(UiTheme.PRIMARY);
        selectedSeats.setFont(UiTheme.font(14, Font.BOLD));
        bill.add(selectedSeats);
        bill.add(Box.createVerticalStrut(12));
        bill.add(billRow("Tiền vé", ticketSubtotal));
        bill.add(Box.createVerticalStrut(6));
        bill.add(billRow("Bắp nước", foodSubtotal));
        bill.add(Box.createVerticalStrut(12));
        bill.add(billRow("Tổng thanh toán", totalPrice));
        bill.add(Box.createVerticalStrut(16));
        JButton pay = action("Xác nhận & thanh toán  →", this::book, "primary");
        pay.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        pay.setAlignmentX(Component.LEFT_ALIGNMENT);
        bill.add(pay);
        summary.add(bill, BorderLayout.SOUTH);
        JPanel columns = new JPanel(new BorderLayout(18, 0));
        columns.setOpaque(false);
        columns.add(seatCard, BorderLayout.CENTER);
        columns.add(summary, BorderLayout.EAST);
        page.add(columns, BorderLayout.CENTER);
        return page;
    }

    private JPanel billRow(String caption, JLabel amount) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        row.add(label(caption, 12, Font.PLAIN, UiTheme.MUTED), BorderLayout.WEST);
        amount.setHorizontalAlignment(SwingConstants.RIGHT);
        amount.setBorder(new EmptyBorder(0, 0, 0, 0));
        row.add(amount, BorderLayout.CENTER);
        return row;
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
        adminTabs = groups;
        groups.setName("admin-tabs");
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
        groups.addChangeListener(e -> {
            if (updatingAdminTabs || !admin || !"ADMIN".equals(activePage)) {
                return;
            }
            switch (groups.getSelectedIndex()) {
                case 0:
                    adminMovies();
                    break;
                case 1:
                    adminRooms();
                    break;
                case 2:
                    adminShows();
                    break;
                case 3:
                    adminProducts();
                    break;
                default:
                    requestAdminView(adminReportView);
                    break;
            }
        });
        groups.setFont(UiTheme.font(14, Font.BOLD));
        groups.setPreferredSize(new Dimension(0, 170));
        page.add(groups, BorderLayout.NORTH);

        UiTheme.RoundedPanel results = UiTheme.surface();
        results.setLayout(new BorderLayout(0, 10));
        JPanel title = new JPanel(new BorderLayout());
        title.setOpaque(false);
        title.add(adminResultTitle, BorderLayout.WEST);
        title.add(adminStats, BorderLayout.EAST);
        JPanel resultHeader = new JPanel(new BorderLayout(0, 7));
        resultHeader.setOpaque(false);
        resultHeader.add(title, BorderLayout.NORTH);
        adminFeedback.setName("admin-feedback");
        resultHeader.add(adminFeedback, BorderLayout.SOUTH);
        results.add(resultHeader, BorderLayout.NORTH);
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
                    requestAdminView(adminView);
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
                    showAuth("LOGIN");
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
            userLabel.setText("Chào " + dec(data[3]));
            userLabel.setToolTipText(dec(data[3]));
            admin = "admin".equals(dec(data[5]));
            staff = admin || "staff".equals(dec(data[5]));
            adminNavGroup.setVisible(admin);
            accountMenu.identity(dec(data[3]), dec(data[4]));
            staffNavGroup.setVisible(staff);
            loginPassword.setText("");
            footer.setVisible(false);
            chrome.setVisible(false);
            loggedIn = true;
            notificationRefreshPending = true;
            notificationTimer.start();
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
            showAuth("LOGIN");
            status.setText("Đã đăng xuất");
        });
    }

    private void openProfile() {
        if (busy) {
            return;
        }
        send("GET_PROFILE", lines -> {
            List<String[]> data = rows(lines, "PROFILE", "PROFILE", 5);
            if (data == null || data.size() != 1) {
                return;
            }
            String[] profile = data.get(0);
            // Only editable personal details enter the dialog, never account ID or role.
            ProfileDialog dialog = new ProfileDialog(this, profile[1], profile[2], profile[3], this::saveProfile);
            profileDialog = dialog;
            try {
                dialog.setVisible(true);
            } finally {
                dialog.dispose();
                if (profileDialog == dialog) {
                    profileDialog = null;

                }
            }
        });
    }

    private void saveProfile(ProfileDialog.Values values) {
        ProfileDialog dialog = profileDialog;
        if (dialog == null || !dialog.isDisplayable()) {
            return;
        }
        if (busy) {
            dialog.error("Đang đồng bộ dữ liệu. Vui lòng thử lưu lại sau giây lát.");
            return;
        }
        dialog.setSaving(true);
        send("UPDATE_PROFILE;" + enc(values.name) + ";" + enc(values.email) + ";" + enc(values.phone)
                + ";" + enc(values.currentPassword) + ";" + enc(values.newPassword), response -> {
            dialog.setSaving(false);
            if (response.isEmpty()) {
                dialog.error("Máy chủ chưa trả về dữ liệu. Vui lòng thử lại.");
                return;
            }
            if (response.get(0).startsWith("ERR;")) {
                dialog.error(response.get(0).substring(4));
                return;
            }
            if (response.size() != 2 || !"OK;PROFILE;1".equals(response.get(0))) {
                dialog.error("Phản hồi cập nhật không hợp lệ.");
                return;
            }
            String[] row = response.get(1).split(";", -1);
            if (row.length != 6 || !"PROFILE".equals(row[0])) {
                dialog.error("Dữ liệu tài khoản không hợp lệ.");
                return;
            }
            String updatedName = dec(row[2]), updatedEmail = dec(row[3]);
            userLabel.setText("Chào " + updatedName);
            userLabel.setToolTipText(updatedName);
            accountMenu.identity(updatedName, updatedEmail);
            loginName.setText(updatedEmail);
            dialog.dispose();
            if (profileDialog == dialog) {
                profileDialog = null;
            }
            info("Đã cập nhật thông tin cá nhân.");
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
                hero.caption("GALAXY CINEMA  /  KHÁM PHÁ");
                hero.description("Hiện chưa có phim trong danh sách. Hẹn gặp bạn ở những suất chiếu sắp tới.");
            } else {
                changeHero(0);
                selectMovie(movies.get(0));
            }
            status.setText("Đã tải " + movies.size() + " phim");
        });
    }

    private void renderMovies() {
        movieGrid.removeAll();
        moviePosters.clear();
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
        movieCount.setText(matches.size() + " phim • Khám phá câu chuyện tiếp theo của bạn");
        int width = movieGrid.getParent() == null ? 0 : movieGrid.getParent().getWidth();
        if (width <= 0) {
            width = 1080;
        }
        Insets padding = movieGrid.getInsets();
        int available = Math.max(1, width - padding.left - padding.right);
        int gap = 12;
        int columns = Math.max(1, Math.min(4, (available + gap) / 220));
        int cardWidth = Math.max(1, (available - (columns - 1) * gap) / columns);
        // Server posters are 244 x 340. Include the caption, gap and card border.
        int cardHeight = (int) Math.round(cardWidth * 340.0 / 244) + 43 + 8 + 2;
        GridBagLayout gridLayout = (GridBagLayout) movieGrid.getLayout();
        gridLayout.columnWidths = new int[columns];
        java.util.Arrays.fill(gridLayout.columnWidths, cardWidth);
        gridLayout.columnWeights = new double[columns];
        java.util.Arrays.fill(gridLayout.columnWeights, 1);
        if (matches.isEmpty()) {
            GridBagConstraints c = new GridBagConstraints();
            c.weightx = 1;
            c.weighty = 1;
            c.gridwidth = columns;
            c.fill = GridBagConstraints.BOTH;
            movieGrid.add(emptyState("Chưa tìm thấy phim", "Thử từ khóa khác hoặc tải lại danh sách."), c);
        } else {
            for (int i = 0; i < matches.size(); i++) {
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = i % columns;
                c.gridy = i / columns;
                c.weightx = 1.0 / columns;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.anchor = GridBagConstraints.NORTHWEST;
                c.insets = new Insets(0, 0, 14, c.gridx < columns - 1 ? gap : 0);
                JPanel card = movieCard(matches.get(i));
                card.setPreferredSize(new Dimension(cardWidth, cardHeight));
                card.setMinimumSize(new Dimension(0, cardHeight));
                movieGrid.add(card, c);
            }
            // Equal column widths even for an incomplete last row.
            GridBagConstraints space = new GridBagConstraints();
            space.gridy = (matches.size() + columns - 1) / columns;
            space.gridwidth = columns;
            space.weighty = 1;
            movieGrid.add(Box.createGlue(), space);
        }
        // Let GridBagLayout include every row and inset in the scrollable height.
        movieGrid.setPreferredSize(null);
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
        JButton filter = new JButton(text) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                AuthStyle.quality(g);
                g.setColor(selected ? new Color(14, 49, 61) : new Color(14, 22, 36));
                g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 36, 36);
                g.setColor(selected ? UiTheme.PRIMARY : UiTheme.LINE);
                g.setStroke(new java.awt.BasicStroke(selected ? 1.5f : 1));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 36, 36);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        filter.setContentAreaFilled(false);
        filter.setBorderPainted(false);
        filter.setFocusPainted(false);
        filter.setBorder(new EmptyBorder(0, 12, 0, 12));
        filter.setForeground(selected ? UiTheme.PRIMARY : UiTheme.MUTED);
        filter.setFont(UiTheme.font(13, Font.PLAIN));
        filter.setPreferredSize(new Dimension(Math.max(74, text.length() * 8 + 26), 38));
        filter.addActionListener(event -> {
            activeGenre = value;
            renderMovies();
        });
        return filter;
    }

    private JPanel movieCard(MovieItem movie) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(0, 0, 2, 0));
        PosterView poster = new PosterView(movie.poster, movie.title);
        moviePosters.put(movie.id, poster);
        boolean selected = activeMovie != null && activeMovie.id == movie.id;
        poster.putClientProperty("selected", selected);
        JButton select = new JButton();
        select.setName("movie-select-" + movie.id);
        select.setLayout(new BorderLayout());
        select.setBorder(BorderFactory.createEmptyBorder());
        select.setContentAreaFilled(false);
        select.setFocusable(true);
        select.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        select.add(poster, BorderLayout.CENTER);
        select.setToolTipText("Chọn suất chiếu: " + movie.title);
        select.getAccessibleContext().setAccessibleName("Chọn suất chiếu " + movie.title);
        select.addActionListener(e -> selectMovie(movie));
        card.add(select, BorderLayout.CENTER);
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        JLabel title = label(movie.title.toUpperCase(Locale.ROOT), 15, Font.BOLD, UiTheme.TEXT);
        title.setToolTipText(movie.title);
        text.add(title);
        text.add(label(movie.genre + " • " + movie.minutes + " phút", 12, Font.PLAIN, UiTheme.MUTED));
        text.setPreferredSize(new Dimension(0, 43));
        card.add(text, BorderLayout.SOUTH);
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
            homeShowtimes.shows(data);
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
        ticketSubtotal.setText("0 ₫");
        foodSubtotal.setText("0 ₫");
        bookingFilm.setText("Chưa chọn suất chiếu");
        bookingMeta.setText("Chọn phim và suất chiếu để bắt đầu");
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
            int selectedRow = showTable.getSelectedRow();
            String film = selectedRow >= 0 ? showModel.getValueAt(selectedRow, 1).toString() : "Suất #" + showId;
            String room = selectedRow >= 0 ? showModel.getValueAt(selectedRow, 2).toString() : "";
            String start = selectedRow >= 0 ? showModel.getValueAt(selectedRow, 3).toString() : "";
            seatTitle.setText(room + " • " + start);
            bookingFilm.setText(film);
            bookingFilm.setToolTipText(film);
            bookingMeta.setText(room + " • " + start);
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
                : "<html><div style='width: 240px'>" + codes.size() + " ghế: " + String.join(", ", codes) + "</div></html>");
        selectedSeats.setToolTipText(String.join(", ", codes));
        BigDecimal price = showPrices.get(selectedShowId());
        BigDecimal seats = price == null ? BigDecimal.ZERO : price.multiply(BigDecimal.valueOf(codes.size()));
        totalPrice.setText(formatMoney(seats.add(foodOrder.total()).toPlainString()));
        ticketSubtotal.setText(formatMoney(seats.toPlainString()));
        foodSubtotal.setText(formatMoney(foodOrder.total().toPlainString()));
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
        int row = showTable.convertRowIndexToModel(showTable.getSelectedRow());
        if (!BookingConfirmDialog.show(this,
                showModel.getValueAt(row, 1).toString(),
                showModel.getValueAt(row, 2).toString(),
                showModel.getValueAt(row, 3).toString(), codes, foodOrder.summary(),
                ticketSubtotal.getText(), foodSubtotal.getText(), totalPrice.getText())) {
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
            if (selectedNotificationBooking != null) {
                for (int i = 0; i < bookingModel.getRowCount(); i++) {
                    if (selectedNotificationBooking.equals(bookingModel.getValueAt(i, 0).toString())) {
                        bookingTable.setRowSelectionInterval(i, i);
                    }
                }
                selectedNotificationBooking = null;
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
        notificationRefreshPending = true;
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
        requestAdminView(AdminView.MOVIES);
    }

    private void adminRooms() {
        requestAdminView(AdminView.ROOMS);
    }

    private void adminShows() {
        requestAdminView(AdminView.SHOWS);
    }

    private void adminProducts() {
        requestAdminView(AdminView.PRODUCTS);
    }

    private void adminUsers() {
        requestAdminView(AdminView.USERS);
    }

    private void adminBookings() {
        requestAdminView(AdminView.BOOKINGS);
    }

    private void adminRevenue() {
        requestAdminView(AdminView.REVENUE);
    }

    private void requestAdminView(AdminView view) {
        adminView = view;
        if (view == AdminView.USERS || view == AdminView.BOOKINGS || view == AdminView.REVENUE) {
            adminReportView = view;
        }
        updatingAdminTabs = true;
        try {
            adminTabs.setSelectedIndex(adminTab(view));
        } finally {
            updatingAdminTabs = false;
        }
        adminViewRevision++;
        adminRefreshPending = true;
        adminStatsPending = true;
        adminFocusId = null;
        adminFeedback.setText(" ");
        adminModel.setRowCount(0);
        adminResultTitle.setText("Đang tải " + adminViewTitle(view) + "...");
        flushAdminRefresh();
    }

    private static int adminTab(AdminView view) {
        switch (view) {
            case MOVIES:
                return 0;
            case ROOMS:
                return 1;
            case SHOWS:
                return 2;
            case PRODUCTS:
                return 3;
            default:
                return 4;
        }
    }

    private static String adminViewTitle(AdminView view) {
        switch (view) {
            case MOVIES:
                return "danh sách phim";
            case ROOMS:
                return "danh sách phòng";
            case SHOWS:
                return "danh sách suất chiếu";
            case PRODUCTS:
                return "đồ ăn & nước uống";
            case USERS:
                return "người dùng";
            case BOOKINGS:
                return "đơn vé";
            default:
                return "doanh thu theo phim";
        }
    }

    private void flushAdminRefresh() {
        if (busy || !admin || !"ADMIN".equals(activePage) || client == null) {
            return;
        }
        if (adminRefreshPending) {
            adminRefreshPending = false;
            switch (adminView) {
                case MOVIES:
                    loadAdminMovies();
                    break;
                case ROOMS:
                    loadAdminRooms();
                    break;
                case SHOWS:
                    loadAdminShows();
                    break;
                case PRODUCTS:
                    loadAdminProducts();
                    break;
                case USERS:
                    loadAdminUsers();
                    break;
                case BOOKINGS:
                    loadAdminBookings();
                    break;
                case REVENUE:
                    loadAdminRevenue();
                    break;
            }
        } else if (adminStatsPending) {
            adminStatsPending = false;
            loadAdminStats();
        }
    }

    private void sendAdmin(String command, Consumer<List<String>> onSuccess) {
        long revision = adminViewRevision;
        send(command, lines -> {
            if (admin && "ADMIN".equals(activePage) && revision == adminViewRevision) {
                onSuccess.accept(lines);
            }
        });
    }

    private void adminChanged(String message, AdminView view, String focusId) {
        requestAdminView(view);
        adminFocusId = focusId;
        adminFeedback.setText(message + " • Đang cập nhật danh sách...");
    }

    private static String addedAdminId(List<String> lines) {
        String[] parts = lines.get(0).split(";", -1);
        return parts.length > 2 ? parts[2] : null;
    }

    private void loadAdminMovies() {
        sendAdmin("GET_MOVIES", lines -> {
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

    private void loadAdminRooms() {
        adminList("ADMIN_GET_ROOMS", "ROOMS", "ROOM",
                new String[]{"Mã", "Tên phòng", "Số ghế"}, 3);
    }

    private void loadAdminProducts() {
        sendAdmin("ADMIN_GET_PRODUCTS", lines -> {
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
                adminChanged("Đã thêm sản phẩm", AdminView.PRODUCTS, addedAdminId(lines));
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
                adminChanged("Đã cập nhật sản phẩm", AdminView.PRODUCTS, v[0]);
            }
        });
    }

    private void adminDeleteProduct() {
        String id = AdminDialog.inputId(this, "Ngừng bán sản phẩm", "Mã sản phẩm cần ngừng bán", false);
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        if (!AdminDialog.confirm(this, "Xác nhận ngừng bán",
                "Ngừng bán sản phẩm #" + id + "?\nSản phẩm sẽ không còn xuất hiện trong danh mục đặt mới.", "Ngừng bán")) {
            return;
        }
        send("ADMIN_DELETE_PRODUCT;" + id.trim(), lines -> {
            if (isOk(lines, "DELETED")) {
                adminChanged("Đã ngừng bán sản phẩm #" + id.trim(), AdminView.PRODUCTS, id.trim());
            }
        });
    }

    private void loadAdminShows() {
        adminList("ADMIN_GET_SHOWS", "SHOWS", "SHOW",
                new String[]{"Mã", "Mã phim", "Phim", "Phòng", "Bắt đầu", "Giá vé"}, 6);
    }

    private void loadAdminUsers() {
        adminList("ADMIN_GET_USERS", "USERS", "USER",
                new String[]{"Mã", "Họ tên", "Email", "Số điện thoại", "Vai trò"}, 5);
    }

    private void loadAdminBookings() {
        adminList("ADMIN_GET_BOOKINGS", "BOOKINGS", "BOOKING",
                new String[]{"Mã đơn", "Người đặt", "Phim", "Phòng", "Bắt đầu",
                    "Tổng tiền", "Trạng thái", "Ghế"}, 8);
    }

    private void loadAdminStats() {
        send("ADMIN_GET_STATS", lines -> {
            List<String[]> data = rows(lines, "STATS", "STAT", 2);
            if (data == null || data.isEmpty()) {
                return;
            }
            adminStats.setText("Vé còn hiệu lực: " + data.get(0)[0]
                    + "    •    Doanh thu còn hiệu lực: " + formatMoney(data.get(0)[1]));

        });
    }

    private void loadAdminRevenue() {
        sendAdmin("ADMIN_GET_REVENUE", lines -> {
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
        });
    }

    private void adminList(String command, String kind, String tag, String[] headers, int fields) {
        sendAdmin(command, lines -> {
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
        if (adminFocusId != null) {
            for (int row = 0; row < adminModel.getRowCount(); row++) {
                if (adminFocusId.equals(String.valueOf(adminModel.getValueAt(row, 0)))) {
                    int visibleRow = adminTable.convertRowIndexToView(row);
                    if (visibleRow >= 0) {
                        adminTable.setRowSelectionInterval(visibleRow, visibleRow);
                        adminTable.scrollRectToVisible(adminTable.getCellRect(visibleRow, 0, true));
                    }
                    break;
                }
            }
            adminFocusId = null;
        }
        if (adminFeedback.getText().contains("Đang cập nhật danh sách...")) {
            adminFeedback.setText(adminFeedback.getText().replace("Đang cập nhật danh sách...", "Danh sách đã cập nhật"));
        }
        status.setText("Đã tải " + rows.size() + " mục từ máy chủ");
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
                        adminChanged("Đã thêm phim" + (form.posterFile == null ? "" : " và poster"), AdminView.MOVIES, addedAdminId(lines));
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
                        adminChanged("Đã sửa phim" + (form.posterFile == null ? "" : " và poster"), AdminView.MOVIES, v[0]);
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
                        adminChanged("Đã thêm phòng", AdminView.ROOMS, addedAdminId(lines));
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
                        adminChanged("Đã cập nhật phòng chiếu", AdminView.ROOMS, v[0]);
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
                        adminChanged("Đã thêm suất chiếu", AdminView.SHOWS, addedAdminId(lines));
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
                        adminChanged("Đã sửa suất chiếu", AdminView.SHOWS, v[0]);
                    }
                });
    }

    private void adminCancel() {
        String id = AdminDialog.inputId(this, "Hủy đơn vé", "Mã đơn vé cần hủy", true);
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        if (!AdminDialog.confirm(this, "Xác nhận hủy đơn vé",
                "Hủy toàn bộ đơn vé #" + id + "?\nHãy đối chiếu mã đơn trong danh sách trước khi tiếp tục.", "Hủy đơn vé")) {
            return;
        }
        send("ADMIN_CANCEL_TICKET;" + id.trim(),
                lines -> {
                    if (isOk(lines, "CANCELLED")) {
                        adminChanged("Đã hủy đơn vé", AdminView.BOOKINGS, id.trim());
                    }
                });
    }

    private void adminDelete(String command, String label, Runnable refresh) {
        String id = AdminDialog.inputId(this, "Xóa dữ liệu", label, false);
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        if (!AdminDialog.confirm(this, "Xác nhận xóa dữ liệu",
                "Xóa mục có " + label.toLowerCase(Locale.ROOT) + " #" + id + "?\nThao tác này không có nút hoàn tác.", "Xác nhận xóa")) {
            return;
        }
        send(command + ";" + id.trim(), lines -> {
            if (!isOk(lines, "DELETED")) {
                return;
            }
            movies.clear();
            showModel.setRowCount(0);
            refresh.run();
            adminFeedback.setText("Đã xóa " + label.toLowerCase(Locale.ROOT) + " #" + id.trim() + " • Đang cập nhật danh sách...");
        });
    }

    private String[] prompt(String title, String... labels) {
        AdminDialog dialog = new AdminDialog(this, title,
                title.startsWith("Sửa") ? "Cập nhật thông tin. Những trường ghi giữ nguyên có thể để trống."
                : "Nhập thông tin bên dưới để thêm vào danh sách quản lý.", "Lưu thông tin", false);
        JTextField[] fields = new JTextField[labels.length];
        for (int i = 0; i < labels.length; i++) {
            fields[i] = AdminDialog.field();
            dialog.addField(labels[i], fields[i]);
        }
        if (!dialog.open(() -> AdminDialog.validate(title, formValues(fields)))) {
            return null;
        }
        return formValues(fields);
    }

    private static String[] formValues(JTextField[] fields) {
        String[] values = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            values[i] = fields[i].getText().trim();
        }
        return values;
    }

    private String[] promptProduct(boolean updating) {
        AdminDialog dialog = new AdminDialog(this, updating ? "Sửa sản phẩm" : "Thêm sản phẩm",
                "Quản lý đồ ăn, nước uống và trạng thái bán tại rạp.", "Lưu sản phẩm", false);
        JTextField id = AdminDialog.field(), name = AdminDialog.field(), price = AdminDialog.field();
        JComboBox<String> type = new JComboBox<>(new String[]{"Đồ ăn", "Nước uống"});
        JComboBox<String> active = new JComboBox<>(new String[]{"Đang bán", "Ngừng bán"});
        UiTheme.styleCombo(type);
        UiTheme.styleCombo(active);
        if (updating) {
            dialog.addField("Mã sản phẩm", id);
        }
        dialog.addField("Tên sản phẩm", name);
        dialog.addField("Loại", type);
        dialog.addField("Giá bán (đ)", price);
        if (updating) {
            dialog.addField("Trạng thái", active);
        }
        if (!dialog.open(() -> {
            if (updating && !AdminDialog.validId(id.getText().trim(), false)) {
                return "Mã sản phẩm phải là số nguyên dương.";
            }
            String value = name.getText().trim();
            if (value.isEmpty() || value.length() > 120) {
                return "Tên sản phẩm cần có từ 1 đến 120 ký tự.";
            }
            return AdminDialog.priceError(price.getText().trim(), true);
        })) {
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
        AdminDialog dialog = new AdminDialog(this, title,
                "Thông tin phim và poster sẽ được hiển thị trên trang chủ.", "Lưu phim", false);
        JTextField[] fields = new JTextField[labels.length];
        for (int i = 0; i < labels.length; i++) {
            fields[i] = AdminDialog.field();
            dialog.addField(labels[i], fields[i]);
        }
        File[] selected = new File[1];
        JLabel filename = label("Sửa phim".equals(title) ? "Giữ poster hiện tại" : "Chưa chọn ảnh",
                12, Font.PLAIN, UiTheme.MUTED);
        JButton choose = action("Chọn ảnh...", () -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Chọn poster cho phim");
            chooser.setFileFilter(new FileNameExtensionFilter("Ảnh JPG hoặc PNG", "jpg", "jpeg", "png"));
            chooser.setAcceptAllFileFilterUsed(false);
            if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                selected[0] = chooser.getSelectedFile();
                filename.setText(selected[0].getName());
                filename.setToolTipText(selected[0].getAbsolutePath());
            }
        }, "subtle");
        JPanel picker = new JPanel(new BorderLayout(8, 0));
        picker.setOpaque(false);
        picker.add(choose, BorderLayout.WEST);
        picker.add(filename, BorderLayout.CENTER);
        dialog.addField("Poster JPG/PNG (tối đa 2 MB)", picker);
        if (!dialog.open(() -> {
            String[] values = formValues(fields);
            String error = AdminDialog.validate(title, values);
            if (error != null) {
                return error;
            }
            if ("Sửa phim".equals(title) && selected[0] == null
                    && java.util.Arrays.stream(values).skip(1).allMatch(String::isEmpty)) {
                return "Hãy nhập thông tin mới hoặc chọn poster mới.";
            }
            if (selected[0] != null) {
                try {
                    if (!selected[0].isFile() || Files.size(selected[0].toPath()) > 2L * 1024 * 1024
                            || ImageIO.read(selected[0]) == null) {
                        return "Chọn ảnh JPG/PNG hợp lệ, không quá 2 MB.";
                    }
                } catch (IOException ex) {
                    return "Không đọc được ảnh poster. Hãy chọn lại ảnh.";
                }
            }
            return null;
        })) {
            return null;
        }
        return new MovieForm(formValues(fields), selected[0]);
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
        setAuthBusy(true);
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
                setAuthBusy(false);
                try {
                    onSuccess.accept(get());
                    if (!busy && "Đang liên hệ máy chủ...".equals(status.getText())) {
                        status.setText("Sẵn sàng • Đã kết nối máy chủ");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError("Thao tác bị gián đoạn");
                } catch (ExecutionException | RuntimeException ex) {
                    Throwable cause = ex instanceof ExecutionException ? ex.getCause() : ex;
                    resetSession();
                    showAuth("LOGIN");
                    showError("Không liên hệ được máy chủ: " + cause.getMessage());
                } finally {
                    // Không bỏ mất EOF hoặc event đến khi request đang chạy.
                    handlePendingDisconnect();
                    flushAdminRefresh();
                    flushSeatRefresh();
                    if (!busy && notificationRefreshPending && loggedIn) {
                        refreshNotifications();
                    }
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
        String prefix = "OK;" + expected;
        if (!lines.get(0).equals(prefix) && !lines.get(0).startsWith(prefix + ";")) {
            showError("Phản hồi máy chủ không hợp lệ");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        status.setText(message);
        AuthMessageDialog.show(this, "Không thể tiếp tục", message, true);
    }

    private void info(String message) {
        status.setText(message);
        AuthMessageDialog.show(this, "Galaxy Cinema", message, false);
    }

    private void handlePendingDisconnect() {
        if (!busy && disconnectedClient != null && client == disconnectedClient) {
            resetSession();
            showAuth("LOGIN");
            showError("Mất kết nối máy chủ. Hãy đăng nhập lại.");
        }
    }

    private void resetSession() {
        loggedIn = false;
        adminViewRevision++;
        adminRefreshPending = false;
        adminStatsPending = false;
        adminView = AdminView.MOVIES;
        adminReportView = AdminView.BOOKINGS;
        adminFocusId = null;
        updatingAdminTabs = true;
        try {
            adminTabs.setSelectedIndex(0);
        } finally {
            updatingAdminTabs = false;
        }
        adminFeedback.setText(" ");
        adminStats.setText("");
        adminResultTitle.setText("Chọn một mục để xem dữ liệu");
        notificationRefreshPending = false;
        notificationTimer.stop();
        notificationCenter.clear();
        selectedNotificationBooking = null;
        accountMenu.setVisible(false);
        accountMenu.identity("", "");
        if (profileDialog != null) {
            profileDialog.dispose();
            profileDialog = null;
        }
        clearRecoverySecrets();
        pendingRecoveryEmail = "";
        // Remember only in this window; never share credentials across clients.
        loginPassword.setText("");
        if (!rememberLogin.isSelected()) {
            loginName.setText("");
        }
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
        activeMovie = null;
        activeGenre = "";
        homeShowtimes.clear();
        movies.clear();
        movieSearch.setText("");
        foodOrder.reset();
        loadedShows.clear();
        movieDetails.removeAll();
        timeSlots.removeAll();
        hero.movie("ĐIỆN ẢNH THEO CÁCH CỦA BẠN", "Chọn phim. Chọn ghế. Tận hưởng.", null, null);
        hero.caption("GALAXY CINEMA  /  KHÁM PHÁ");
        hero.description("Khám phá câu chuyện tiếp theo của bạn.");
        showPrices.clear();
        movieGrid.removeAll();
        moviePosters.clear();
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
        notificationTimer.stop();
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

    private static final class NavButton extends JButton {

        private boolean active;

        NavButton(String text) {
            super(text);
            setPreferredSize(new Dimension(188, 44));
            setMaximumSize(new Dimension(188, 44));
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new EmptyBorder(0, 13, 0, 8));
            setFont(UiTheme.font(15, Font.PLAIN));
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
            if (active || hasFocus()) {
                g.setColor(UiTheme.PRIMARY);
                g.fillRoundRect(10, getHeight() - 4, getWidth() - 20, 3, 3, 3);
            } else if (getModel().isRollover()) {
                g.setColor(UiTheme.NAVY_LIGHT);
                g.fillRoundRect(4, 10, getWidth() - 8, getHeight() - 20, 8, 8);
            }
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
