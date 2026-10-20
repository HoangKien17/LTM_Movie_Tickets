package web;

import java.awt.*;
import java.awt.image.BufferedImage;
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
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    static JPanel content(String[] ticket, Component dialog) {
        JPanel root = new JPanel(new BorderLayout(18, 14));
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        boolean active = "active".equals(ticket[5]) && ticket[6].isEmpty();
        String state = !"active".equals(ticket[5]) ? "Đã hủy"
                : ticket[6].isEmpty() ? "Chưa sử dụng" : "Đã sử dụng lúc " + ticket[6].replace('T', ' ');
        String food = ticket.length > 8 && !ticket[8].isEmpty() ? ticket[8] : "Không có";
        String amount = ticket.length > 9 ? ticket[9] + " VND" : "";
        JTextArea details = new JTextArea("Đơn #" + ticket[0] + "\nPhim: " + ticket[1] + "\nPhòng: " + ticket[2]
                + "\nSuất: " + ticket[3].replace('T', ' ') + "\nGhế: " + ticket[4] + "\nTrạng thái: " + state
                + "\nĐồ ăn/nước: " + food + (amount.isEmpty() ? "" : "\nTổng tiền: " + amount)
                + "\n\nMột mã áp dụng cho toàn bộ ghế trong đơn.");
        details.setEditable(false);
        details.setOpaque(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setFont(UiTheme.font(14, Font.PLAIN));
        details.setPreferredSize(new Dimension(330, 220));
        root.add(details, BorderLayout.CENTER);
        if (active) {
            String payload = "GALAXY CINEMA\nMã đơn: " + ticket[0] + "\nPhim: " + ticket[1]
                    + "\nPhòng: " + ticket[2] + "\nSuất: " + ticket[3].replace('T', ' ') + "\nGhế: " + ticket[4]
                    + "\nĐồ ăn/nước: " + food + (amount.isEmpty() ? "" : "\nTổng tiền: " + amount)
                    + "\nMã xác thực: " + TicketCode.payload(ticket[7]);
            BufferedImage qr = QrImages.encode(payload, 360);
            root.add(new JLabel(new ImageIcon(qr)), BorderLayout.WEST);
            JPanel bottom = new JPanel(new BorderLayout(8, 8));
            JTextField code = new JTextField(payload);
            code.setEditable(false);
            code.setToolTipText("Có thể sao chép để nhân viên nhập mã");
            bottom.add(code, BorderLayout.CENTER);
            JButton save = UiTheme.primary("Lưu ảnh QR");
            save.addActionListener(event -> {
                JFileChooser picker = new JFileChooser();
                picker.setFileFilter(new FileNameExtensionFilter("PNG", "png"));
                picker.setSelectedFile(new File("ve-" + ticket[0] + ".png"));
                if (picker.showSaveDialog(dialog) != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                File file = picker.getSelectedFile();
                if (!file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".png")) {
                    file = new File(file + ".png");
                }
                if (file.exists() && JOptionPane.showConfirmDialog(dialog, "Ghi đè ảnh đã có?",
                        "Lưu QR", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                    return;
                }
                try {
                    if (!ImageIO.write(qr, "png", file)) {
                        throw new IOException("Không ghi được PNG");
                    }
                    JOptionPane.showMessageDialog(dialog, "Đã lưu ảnh QR");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(dialog, "Không lưu được ảnh QR", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            });
            bottom.add(save, BorderLayout.EAST);
            root.add(bottom, BorderLayout.SOUTH);
        }
        return root;
    }
}
