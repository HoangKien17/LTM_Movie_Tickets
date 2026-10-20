package web;

import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.function.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import protocol.Protocol;
import protocol.TicketCode;
import utils.QrTickets;

final class TicketCheckInPanel extends JPanel {

    private final BiConsumer<String, Consumer<List<String>>> send;
    private final BooleanSupplier ready;
    private final JTextArea input = new JTextArea(3, 50);
    private final JTextArea result = new JTextArea();
    private final JButton confirm = UiTheme.primary("Xác nhận vào rạp");
    private String inspected;
    private int generation;
    private int imageGeneration;
    private boolean reading;

    TicketCheckInPanel(BiConsumer<String, Consumer<List<String>>> send, BooleanSupplier ready) {
        super(new BorderLayout(0, 16));
        this.send = send;
        this.ready = ready;
        setBorder(BorderFactory.createEmptyBorder(24, 27, 24, 27));
        setBackground(UiTheme.BACKGROUND);
        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.add(new JLabel("Nhập mã vé hoặc đọc ảnh QR — mỗi lần soát áp dụng cho toàn bộ đơn"),
                BorderLayout.NORTH);
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setFont(UiTheme.font(14, Font.PLAIN));
        top.add(new JScrollPane(input), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton image = UiTheme.secondary("Đọc ảnh QR");
        image.addActionListener(event -> readImage());
        JButton inspect = UiTheme.secondary("Kiểm tra vé");
        inspect.addActionListener(event -> inspect());
        confirm.setEnabled(false);
        confirm.addActionListener(event -> checkIn());
        buttons.add(image);
        buttons.add(inspect);
        buttons.add(confirm);
        top.add(buttons, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        result.setEditable(false);
        result.setLineWrap(true);
        result.setWrapStyleWord(true);
        result.setFont(UiTheme.font(16, Font.PLAIN));
        result.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        add(new JScrollPane(result), BorderLayout.CENTER);
        input.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            public void changedUpdate(DocumentEvent e) {
                changed();
            }

            private void changed() {
                generation++;
                inspected = null;
                confirm.setEnabled(false);
            }
        });
    }

    void reset() {
        generation++;
        imageGeneration++;
        reading = false;
        inspected = null;
        input.setText("");
        result.setText("");
        confirm.setEnabled(false);
    }

    private String payload() {
        return TicketCode.payload(TicketCode.token(input.getText()));
    }

    private void inspect() {
        if (!ready.getAsBoolean() || reading) {
            return;
        }
        try {
            String code = payload();
            int current = generation;
            confirm.setEnabled(false);
            result.setText("Đang kiểm tra...");
            send.accept("INSPECT_TICKET;" + Protocol.encode(code), lines -> {
                if (current != generation) {
                    return;
                }
                String[] ticket = decode(lines, "E_TICKET");
                if (ticket == null) {
                    return;
                }
                result.setText(describe(ticket));
                inspected = code;
                confirm.setEnabled("active".equals(ticket[5]) && ticket[6].isEmpty());
            });
        } catch (IllegalArgumentException ex) {
            result.setText(ex.getMessage());
        }
    }

    private void checkIn() {
        if (!ready.getAsBoolean() || inspected == null || reading) {
            return;
        }
        String code = inspected;
        int current = generation;
        confirm.setEnabled(false);
        send.accept("CHECK_IN;" + Protocol.encode(code), lines -> {
            if (current != generation) {
                return;
            }
            inspected = null;
            String[] ticket = decode(lines, "CHECKED_IN");
            if (ticket != null) {
                result.setText("ĐÃ XÁC NHẬN VÀO RẠP\n\n" + describe(ticket));
            }
        });
    }

    private void readImage() {
        if (reading || !ready.getAsBoolean()) {
            return;
        }
        JFileChooser picker = new JFileChooser();
        picker.setFileFilter(new FileNameExtensionFilter("Ảnh QR PNG/JPG", "png", "jpg", "jpeg"));
        if (picker.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = picker.getSelectedFile();
        int current = ++imageGeneration;
        int textGeneration = generation;
        inspected = null;
        confirm.setEnabled(false);
        reading = true;
        result.setText("Đang đọc ảnh QR...");
        new SwingWorker<String, Void>() {
            protected String doInBackground() throws Exception {
                return QrTickets.decode(file);
            }

            protected void done() {
                if (current != imageGeneration) {
                    return;
                }
                reading = false;
                if (textGeneration != generation) {
                    return;
                }
                try {
                    input.setText(get());
                    inspect();
                } catch (Exception ex) {
                    result.setText("Không đọc được mã vé từ ảnh. Hãy chọn ảnh QR rõ nét hoặc nhập mã.");
                }
            }
        }.execute();
    }

    private String[] decode(List<String> lines, String kind) {
        if (!lines.isEmpty() && lines.get(0).startsWith("ERR;")) {
            result.setText(lines.get(0).substring(4));
            return null;
        }
        if (lines.size() != 2 || !lines.get(0).equals("OK;" + kind + ";1")) {
            result.setText("Phản hồi máy chủ không hợp lệ");
            return null;
        }
        String[] row = lines.get(1).split(";", -1);
        if ((row.length != 9 && row.length != 11) || !"TICKET".equals(row[0])) {
            result.setText("Dữ liệu vé không hợp lệ");
            return null;
        }
        String[] values = new String[row.length - 1];
        for (int i = 0; i < values.length; i++) {
            values[i] = Protocol.decode(row[i + 1]);
        }
        return values;
    }

    private static String describe(String[] t) {
        String state = !"active".equals(t[5]) ? "ĐÃ HỦY"
                : t[6].isEmpty() ? "HỢP LỆ — CHƯA SỬ DỤNG" : "ĐÃ SỬ DỤNG: " + t[6].replace('T', ' ');
        return "Đơn #" + t[0] + "\nPhim: " + t[1] + "\nPhòng: " + t[2]
                + "\nSuất: " + t[3].replace('T', ' ') + "\nGhế: " + t[4]
                + (t.length > 8 && !t[8].isEmpty() ? "\nĐồ ăn/nước: " + t[8] : "") + "\n\n" + state;
    }
}
