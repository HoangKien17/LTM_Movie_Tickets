package web;

import java.awt.*;
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
        dialog.setContentPane(content(ticket, dialog));
        dialog.pack();
        dialog.setMinimumSize(new Dimension(960, 650));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    static JPanel content(String[] ticket, Component dialog) {
        JPanel root = new JPanel(new BorderLayout(20, 18));
        root.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        root.setBackground(UiTheme.BACKGROUND);
        root.setPreferredSize(new Dimension(1000, 670));
        JLabel heading = new JLabel("Thanh toán & vé điện tử");
        heading.setFont(UiTheme.font(25, Font.BOLD));
        root.add(heading, BorderLayout.NORTH);
        boolean active = "active".equals(ticket[5]) && ticket[6].isEmpty();
        String state = !"active".equals(ticket[5]) ? "Đơn không còn hiệu lực"
                : ticket[6].isEmpty() ? "Vé hợp lệ • Chưa sử dụng" : "Đã sử dụng: " + ticket[6].replace('T', ' ');
        String food = ticket.length > 8 && !ticket[8].isEmpty() ? ticket[8] : "Không có";
        String amount = ticket.length > 9 ? BasicClientUI.formatMoney(ticket[9]) : "—";
        JPanel columns = new JPanel(new GridLayout(1, 2, 20, 0));
        columns.setOpaque(false);
        UiTheme.RoundedPanel details = UiTheme.surface();
        details.setLayout(new BorderLayout(0, 18));
        JLabel success = new JLabel(active ? "Thanh toán thành công" : state);
        success.setForeground(active ? UiTheme.GREEN : UiTheme.RED);
        success.setFont(UiTheme.font(21, Font.BOLD));
        details.add(success, BorderLayout.NORTH);
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(UiTheme.copy("Đơn #" + ticket[0] + "\n\nPhim: " + ticket[1] + "\nPhòng: " + ticket[2]
                + "\nSuất chiếu: " + ticket[3].replace('T', ' ') + "\nGhế: " + ticket[4]
                + "\n\nBắp nước: " + food, 15));
        body.add(Box.createVerticalStrut(20));
        JLabel total = new JLabel("Tổng thanh toán  " + amount);
        total.setForeground(UiTheme.PRIMARY);
        total.setFont(UiTheme.font(22, Font.BOLD));
        body.add(total);
        for (Component c : body.getComponents()) {
            if (c instanceof JComponent) {
                ((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
            }
        }
        details.add(body, BorderLayout.CENTER);
        JTextArea note = UiTheme.copy("Xuất trình mã vé tại quầy soát vé. Một mã áp dụng cho toàn bộ ghế trong đơn.", 13);
        note.setRows(3);
        details.add(note, BorderLayout.SOUTH);
        columns.add(details);
        TicketCard stub = new TicketCard();
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JLabel brand = new JLabel("GALAXY CINEMA  /  VÉ ĐIỆN TỬ");
        brand.setFont(UiTheme.font(13, Font.BOLD));
        brand.setForeground(UiTheme.PRIMARY);
        top.add(brand);
        top.add(Box.createVerticalStrut(14));
        JTextArea film = UiTheme.copy(ticket[1], 23);
        film.setFont(UiTheme.font(23, Font.BOLD));
        film.setForeground(UiTheme.TEXT);
        film.setRows(2);
        top.add(film);
        top.add(Box.createVerticalStrut(6));
        top.add(UiTheme.copy(ticket[2] + " • " + ticket[3].replace('T', ' ') + "\nGhế: " + ticket[4], 13));
        for (Component c : top.getComponents()) {
            if (c instanceof JComponent) {
                ((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
            }
        }
        stub.header(top);
        JPanel qrArea = new JPanel(new BorderLayout(0, 8));
        qrArea.setOpaque(false);
        if (active) {
            // Token only keeps the QR small; the surrounding ticket carries readable details.
            String payload = TicketCode.payload(ticket[7]);
            BufferedImage qr = QrImages.encode(payload, 224);
            JLabel codeImage = new JLabel(new ImageIcon(qr), SwingConstants.CENTER);
            codeImage.setToolTipText("Mã vé cho toàn bộ ghế trong đơn #" + ticket[0]);
            qrArea.add(codeImage, BorderLayout.CENTER);
        } else {
            JLabel unavailable = new JLabel("QR không còn khả dụng", SwingConstants.CENTER);
            unavailable.setForeground(UiTheme.MUTED);
            qrArea.add(unavailable, BorderLayout.CENTER);
        }
        JLabel id = new JLabel("Mã đơn: GC-" + ticket[0], SwingConstants.CENTER);
        id.setFont(UiTheme.font(12, Font.BOLD));
        qrArea.add(id, BorderLayout.SOUTH);
        stub.add(qrArea, BorderLayout.CENTER);
        JLabel badge = new JLabel(state, SwingConstants.CENTER);
        badge.setFont(UiTheme.font(12, Font.BOLD));
        badge.setForeground(active ? UiTheme.GREEN : UiTheme.RED);
        stub.add(badge, BorderLayout.SOUTH);
        columns.add(stub);
        root.add(columns, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(16, 0));
        bottom.setOpaque(false);
        if (active) {
            JTextField code = new JTextField(TicketCode.payload(ticket[7]));
            code.setEditable(false);
            UiTheme.styleField(code);
            code.setToolTipText("Sao chép mã này để nhân viên nhập thủ công");
            bottom.add(code, BorderLayout.CENTER);
            JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0));
            actions.setOpaque(false);
            JButton save = UiTheme.primary("Tải vé PNG");
            save.addActionListener(e -> saveTicket(stub, dialog, ticket[0]));
            JButton print = UiTheme.secondary("In vé");
            print.addActionListener(e -> printTicket(stub, dialog));
            actions.add(save);
            actions.add(print);
            bottom.add(actions, BorderLayout.EAST);
        }
        root.add(bottom, BorderLayout.SOUTH);
        return root;
    }

    private static void saveTicket(JPanel ticket, Component parent, String id) {
        JFileChooser picker = new JFileChooser();
        picker.setFileFilter(new FileNameExtensionFilter("PNG", "png"));
        picker.setSelectedFile(new File("ve-galaxy-" + id + ".png"));
        if (picker.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = picker.getSelectedFile();
        if (!file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".png")) {
            file = new File(file + ".png");
        }
        if (file.exists() && JOptionPane.showConfirmDialog(parent, "Ghi đè ảnh đã có?", "Tải vé",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            BufferedImage image = new BufferedImage(ticket.getWidth() * 2, ticket.getHeight() * 2, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(UiTheme.BACKGROUND);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.scale(2, 2);
            ticket.printAll(g);
            g.dispose();
            if (!ImageIO.write(image, "png", file)) {
                throw new IOException("Không ghi được PNG");
            }
            JOptionPane.showMessageDialog(parent, "Đã lưu vé điện tử");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Không lưu được vé: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void printTicket(JPanel ticket, Component parent) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Vé Galaxy Cinema");
        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) {
                return java.awt.print.Printable.NO_SUCH_PAGE;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            double scale = Math.min(pageFormat.getImageableWidth() / ticket.getWidth(),
                    pageFormat.getImageableHeight() / ticket.getHeight());
            g.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            g.scale(scale, scale);
            ticket.printAll(g);
            g.dispose();
            return java.awt.print.Printable.PAGE_EXISTS;
        });
        try {
            if (job.printDialog()) {
                job.print();
            }
        } catch (java.awt.print.PrinterException ex) {
            JOptionPane.showMessageDialog(parent, "Không thể in vé: " + ex.getMessage(),
                    "In vé", JOptionPane.ERROR_MESSAGE);
        }
    }
}
