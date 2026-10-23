package web;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterJob;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import protocol.TicketCode;
import utils.QrImages;

final class ElectronicTicketDialog {

    private ElectronicTicketDialog() {
    }

    static void show(JFrame owner, String[] ticket) {
        JDialog dialog = new JDialog(owner, "Vé điện tử — đơn #" + ticket[0], true);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(content(ticket, dialog));
        dialog.pack();
        Rectangle bounds = dialog.getGraphicsConfiguration().getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(dialog.getGraphicsConfiguration());
        dialog.setSize(Math.min(dialog.getWidth(), bounds.width - insets.left - insets.right - 32),
                Math.min(dialog.getHeight(), bounds.height - insets.top - insets.bottom - 32));
        dialog.setLocationRelativeTo(owner);
        dialog.getRootPane().registerKeyboardAction(event -> dialog.dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        try {
            dialog.setShape(new RoundRectangle2D.Double(0, 0, dialog.getWidth(), dialog.getHeight(), 26, 26));
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            dialog.setVisible(true);
        } finally {
            dialog.dispose();
        }
    }

    static JPanel content(String[] ticket, Component dialog) {
        JPanel root = new JPanel(new BorderLayout(0, 20)) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                AuthStyle.quality(g);
                g.setColor(UiTheme.BACKGROUND);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 26, 26);
                g.setPaint(new GradientPaint(0, 0, UiTheme.PRIMARY, getWidth(), getHeight(), UiTheme.PURPLE));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 26, 26);
                g.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(24, 26, 24, 26));
        root.setPreferredSize(new Dimension(1020, 800));
        root.add(header(dialog), BorderLayout.NORTH);
        JPanel sheet = ticketSheet(ticket);
        JScrollPane scroll = UiTheme.scrollContent(sheet);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        root.add(scroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(16, 0));
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.LINE), BorderFactory.createEmptyBorder(16, 0, 0, 0)));
        bottom.add(TicketDetailsView.text("Mã tra cứu tại quầy: #" + ticket[0] + "\nMột mã áp dụng cho toàn bộ ghế trong đơn.", 13, UiTheme.MUTED), BorderLayout.CENTER);
        if (TicketDetailsView.usable(ticket)) {
            JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0));
            actions.setOpaque(false);
            JButton save = UiTheme.primary("Tải vé PNG");
            save.setName("save-electronic-ticket");
            save.addActionListener(event -> saveTicket(ticket, dialog));
            JButton print = UiTheme.secondary("In vé");
            print.addActionListener(event -> printTicket(ticket, dialog));
            actions.add(save);
            actions.add(print);
            bottom.add(actions, BorderLayout.EAST);
        }
        root.add(bottom, BorderLayout.SOUTH);
        return root;
    }

    private static JPanel header(Component parent) {
        JPanel header = new JPanel(new BorderLayout(0, 16));
        header.setOpaque(false);
        JPanel brandRow = new JPanel(new BorderLayout());
        brandRow.setOpaque(false);
        JLabel brand = new JLabel("GALAXY CINEMA", UiTheme.orbitIcon(30), SwingConstants.LEFT);
        brand.setFont(AuthStyle.wordmark(13));
        brand.setForeground(UiTheme.TEXT);
        brand.setIconTextGap(10);
        brandRow.add(brand, BorderLayout.WEST);
        JButton close = new JButton(HomeIcons.icon("close", 20));
        close.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 0));
        close.setContentAreaFilled(false);
        close.setBorderPainted(false);
        close.setForeground(UiTheme.MUTED);
        close.setToolTipText("Đóng vé điện tử");
        close.addActionListener(event -> {
            if (parent instanceof Window) {
                ((Window) parent).dispose();
        
            }});
        brandRow.add(close, BorderLayout.EAST);
        header.add(brandRow, BorderLayout.NORTH);
        header.add(TicketDetailsView.label("Vé điện tử của bạn", 27, Font.BOLD, UiTheme.TEXT), BorderLayout.CENTER);
        if (parent instanceof Window) {
            Window window = (Window) parent;
            MouseAdapter drag = new MouseAdapter() {
                private Point pressed;

                public void mousePressed(MouseEvent event) {
                    if (SwingUtilities.isLeftMouseButton(event)) {
                        pressed = event.getLocationOnScreen();
                
                    }}

                public void mouseDragged(MouseEvent event) {
                    if (pressed == null) {
                        return;
                    }
                    Point next = event.getLocationOnScreen();
                    window.setLocation(window.getX() + next.x - pressed.x, window.getY() + next.y - pressed.y);
                    pressed = next;
                }

                public void mouseReleased(MouseEvent event) {
                    pressed = null;
                }
            };
            brandRow.addMouseListener(drag);
            brandRow.addMouseMotionListener(drag);
            brand.addMouseListener(drag);
            brand.addMouseMotionListener(drag);
        }
        return header;
    }

    private static JPanel ticketSheet(String[] ticket) {
        JPanel sheet = new JPanel(new GridLayout(1, 2, 18, 0));
        sheet.setOpaque(false);
        JPanel left = new JPanel(new BorderLayout(0, 16));
        left.setOpaque(false);
        JPanel success = new JPanel(new BorderLayout(11, 6));
        success.setOpaque(false);
        boolean paid = "active".equals(TicketDetailsView.value(ticket, 5));
        JLabel icon = new JLabel(HomeIcons.icon(paid ? "check" : "ticket", 32));
        icon.setForeground(paid ? UiTheme.GREEN : TicketDetailsView.stateColor(ticket));
        success.add(icon, BorderLayout.WEST);
        success.add(TicketDetailsView.label(paid ? "Thanh toán thành công" : TicketDetailsView.state(ticket), 20, Font.BOLD,
                paid ? UiTheme.GREEN : TicketDetailsView.stateColor(ticket)), BorderLayout.CENTER);
        left.add(success, BorderLayout.NORTH);
        left.add(new TicketDetailsView(ticket), BorderLayout.CENTER);
        sheet.add(left);

        TicketCard stub = new TicketCard();
        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(TicketDetailsView.label("GALAXY CINEMA  /  VÉ ĐIỆN TỬ", 11, Font.BOLD, UiTheme.PRIMARY), BorderLayout.NORTH);
        JTextArea movie = TicketDetailsView.text(ticket[1], 23, UiTheme.TEXT);
        movie.setFont(UiTheme.font(23, Font.BOLD));
        movie.setRows(2);
        top.add(movie, BorderLayout.CENTER);
        top.add(TicketDetailsView.text(ticket[2] + " • " + TicketDetailsView.date(ticket[3], "HH:mm • dd/MM/yyyy")
                + "\nGhế: " + ticket[4].replace(",", ", "), 13, UiTheme.MUTED), BorderLayout.SOUTH);
        stub.header(top);
        JPanel qrArea = new JPanel(new BorderLayout(0, 12));
        qrArea.setOpaque(false);
        if (TicketDetailsView.usable(ticket)) {
            JLabel qr = new JLabel(new ImageIcon(QrImages.encode(TicketCode.payload(ticket[7]), 232)), SwingConstants.CENTER);
            qr.setName("ticket-qr-image");
            qr.setToolTipText("Mã vé cho toàn bộ ghế trong đơn #" + ticket[0]);
            qrArea.add(qr, BorderLayout.CENTER);
        } else {
            JLabel unavailable = TicketDetailsView.label("QR không còn khả dụng", 14, Font.PLAIN, UiTheme.MUTED);
            unavailable.setHorizontalAlignment(SwingConstants.CENTER);
            unavailable.setPreferredSize(new Dimension(232, 232));
            qrArea.add(unavailable, BorderLayout.CENTER);
        }
        JLabel code = TicketDetailsView.label("MÃ ĐƠN  #" + ticket[0], 19, Font.BOLD, UiTheme.TEXT);
        code.setHorizontalAlignment(SwingConstants.CENTER);
        qrArea.add(code, BorderLayout.SOUTH);
        stub.add(qrArea, BorderLayout.CENTER);
        JPanel footer = new JPanel(new GridLayout(2, 1, 0, 7));
        footer.setOpaque(false);
        JLabel state = TicketDetailsView.label(TicketDetailsView.state(ticket), 12, Font.BOLD, TicketDetailsView.stateColor(ticket));
        state.setHorizontalAlignment(SwingConstants.CENTER);
        footer.add(state);
        JLabel instruction = TicketDetailsView.label("Xuất trình QR hoặc mã đơn tại quầy soát vé", 11, Font.PLAIN, UiTheme.MUTED);
        instruction.setHorizontalAlignment(SwingConstants.CENTER);
        footer.add(instruction);
        stub.add(footer, BorderLayout.SOUTH);
        sheet.add(stub);
        return sheet;
    }

    static BufferedImage renderTicket(String[] ticket) {
        JPanel sheet = ticketSheet(ticket);
        sheet.setSize(960, 640);
        layout(sheet);
        sheet.setSize(960, Math.max(640, sheet.getPreferredSize().height));
        layout(sheet);
        BufferedImage image = new BufferedImage(sheet.getWidth() * 2, sheet.getHeight() * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(UiTheme.BACKGROUND);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.scale(2, 2);
        sheet.printAll(graphics);
        graphics.dispose();
        return image;
    }

    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) {
            if (child instanceof Container) {
                layout((Container) child);
            }
        }
    }

    private static Window owner(Component parent) {
        return parent instanceof Window ? (Window) parent : parent == null ? null : SwingUtilities.getWindowAncestor(parent);
    }

    private static void saveTicket(String[] ticket, Component parent) {
        JFileChooser picker = new JFileChooser();
        picker.setFileFilter(new FileNameExtensionFilter("PNG", "png"));
        picker.setSelectedFile(new File("ve-galaxy-" + ticket[0] + ".png"));
        if (picker.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = picker.getSelectedFile();
        if (!file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".png")) {
            file = new File(file + ".png");
        }
        if (file.exists() && !AdminDialog.confirm(owner(parent), "Thay thế ảnh vé?", "Ảnh đã tồn tại:\n" + file.getName(), "Thay thế ảnh")) {
            return;
        }
        try {
            if (!ImageIO.write(renderTicket(ticket), "png", file)) {
                throw new IOException("Không ghi được PNG");
            }
            AuthMessageDialog.show(owner(parent), "Đã lưu vé điện tử", "Vé đơn #" + ticket[0] + " đã được lưu thành công.\n\n" + file.getAbsolutePath(), false);
        } catch (IOException | RuntimeException ex) {
            AuthMessageDialog.show(owner(parent), "Không lưu được vé", ex.getMessage(), true);
        }
    }

    private static void printTicket(String[] ticket, Component parent) {
        BufferedImage image = renderTicket(ticket);
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Vé Galaxy Cinema #" + ticket[0]);
        job.setPrintable((graphics, format, page) -> {
            if (page > 0) {
                return java.awt.print.Printable.NO_SUCH_PAGE;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            double scale = Math.min(format.getImageableWidth() / image.getWidth(), format.getImageableHeight() / image.getHeight());
            g.translate(format.getImageableX(), format.getImageableY());
            g.scale(scale, scale);
            g.drawImage(image, 0, 0, null);
            g.dispose();
            return java.awt.print.Printable.PAGE_EXISTS;
        });
        try {
            if (job.printDialog()) {
                job.print();
        
            }} catch (java.awt.print.PrinterException ex) {
            AuthMessageDialog.show(owner(parent), "Không thể in vé", ex.getMessage(), true);
        }
    }
}
