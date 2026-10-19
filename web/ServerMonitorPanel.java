package web;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import protocol.Protocol;

final class ServerMonitorPanel extends JPanel {

    private final BiConsumer<String, Consumer<List<String>>> send;
    private final BooleanSupplier ready;
    private final JLabel summary = new JLabel("Chưa có dữ liệu");
    private final DefaultTableModel clients = model("Phiên", "Địa chỉ", "Kết nối lúc", "User ID", "Quyền", "Suất", "Yêu cầu", "TB (ms)");
    private final DefaultTableModel logs = model("#", "Thời điểm", "Phiên", "Thao tác", "Kết quả");
    private final Timer timer;
    private final JTable connections = new JTable(clients);
    private final JLabel logTitle = new JLabel();
    private final JButton allSessions = UiTheme.secondary("Tất cả phiên");
    private final List<String[]> allLogs = new ArrayList<>();
    private String selectedSession;
    private boolean updatingClients;
    private boolean active;

    ServerMonitorPanel(BiConsumer<String, Consumer<List<String>>> send, BooleanSupplier ready) {
        super(new BorderLayout(0, 14));
        this.send = send;
        this.ready = ready;
        setBorder(BorderFactory.createEmptyBorder(24, 27, 24, 27));
        setBackground(UiTheme.BACKGROUND);
        JPanel top = new JPanel(new BorderLayout(10, 10));
        summary.setFont(UiTheme.font(13, Font.BOLD));
        top.add(summary, BorderLayout.CENTER);
        JButton refresh = UiTheme.secondary("Làm mới");
        refresh.addActionListener(event -> refresh());
        top.add(refresh, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        JTable history = new JTable(logs);
        connections.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        connections.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || updatingClients) {
                return;
            }
            int row = connections.getSelectedRow();
            if (row < 0) {
                return;
            }
            selectedSession = clients.getValueAt(connections.convertRowIndexToModel(row), 0).toString();
            renderLogs();
        });
        allSessions.addActionListener(event -> {
            selectedSession = null;
            connections.clearSelection();
            renderLogs();
        });
        UiTheme.styleTable(connections);
        UiTheme.styleTable(history);
        connections.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        history.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] clientWidths = {55, 175, 170, 70, 70, 65, 90, 85};
        int[] logWidths = {55, 170, 60, 190, 380};
        for (int i = 0; i < clientWidths.length; i++) {
            connections.getColumnModel().getColumn(i).setPreferredWidth(clientWidths[i]);
        }
        for (int i = 0; i < logWidths.length; i++) {
            history.getColumnModel().getColumn(i).setPreferredWidth(logWidths[i]);
        }
        JPanel logSection = section("", history);
        JPanel logHeader = new JPanel(new BorderLayout(10, 0));
        logHeader.add(logTitle, BorderLayout.CENTER);
        logHeader.add(allSessions, BorderLayout.EAST);
        logSection.add(logHeader, BorderLayout.NORTH);
        renderLogs();
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, section("Client đang kết nối — chọn phiên để xem nhật ký", connections),
                logSection);
        split.setResizeWeight(0.4);
        split.setDividerLocation(210);
        add(split, BorderLayout.CENTER);
        add(new JLabel("Tự cập nhật mỗi 2 giây • Thời gian xử lý không gồm gửi qua mạng • Bộ đếm theo phiên chạy server"),
                BorderLayout.SOUTH);
        timer = new Timer(2000, event -> refresh());
    }

    void start() {
        active = true;
        timer.start();
        refresh();
    }

    void stop() {
        active = false;
        timer.stop();
    }

    void reset() {
        stop();
        selectedSession = null;
        clients.setRowCount(0);
        allLogs.clear();
        renderLogs();
        summary.setText("Chưa có dữ liệu");
    }

    private void renderLogs() {
        logs.setRowCount(0);
        // Sequence is monotonic even when events share the same timestamp second.
        allLogs.sort(Comparator.comparingLong((String[] row) -> Long.parseLong(row[0])).reversed());
        for (String[] row : allLogs) {
            if (selectedSession == null || selectedSession.equals(row[2])) {
                logs.addRow(row);
            }
        }
        logTitle.setText("Nhật ký — " + (selectedSession == null ? "tất cả phiên" : "phiên " + selectedSession)
                + " — mới nhất trước (" + logs.getRowCount() + " dòng)");
        logTitle.setToolTipText("Lọc trong tối đa 500 dòng gần nhất của server; giữ bộ lọc khi phiên ngắt kết nối.");
        allSessions.setEnabled(selectedSession != null);
    }

    private void refresh() {
        if (!active || !ready.getAsBoolean()) {
            return;
        }
        send.accept("ADMIN_MONITOR", lines -> {
            if (!active) {
                return;
            }
            if (lines.isEmpty() || !"OK;MONITOR".equals(lines.get(0))) {
                summary.setText("Không tải được dữ liệu giám sát");
                return;
            }
            updatingClients = true;
            try {
                clients.setRowCount(0);
                allLogs.clear();
                for (int i = 1; i < lines.size(); i++) {
                    String[] row = lines.get(i).split(";", -1);
                    String[] fields = new String[row.length - 1];
                    for (int j = 0; j < fields.length; j++) {
                        fields[j] = Protocol.decode(row[j + 1]);
                    }
                    if ("SUMMARY".equals(row[0]) && fields.length == 6) {
                        summary.setText("Client: " + fields[0] + "   |   Yêu cầu: " + fields[1] + "   |   Lỗi yêu cầu: " + fields[2]
                                + "   |   TB: " + fields[3] + " ms   |   Uptime: " + fields[4] + "s");
                        summary.setToolTipText("Server khởi động: " + fields[5]);
                    } else if ("CLIENT".equals(row[0]) && fields.length == 8) {
                        clients.addRow(fields); 
                    }else if ("LOG".equals(row[0]) && fields.length == 5) {
                        allLogs.add(fields);
                    }
                }
                for (int row = 0; row < clients.getRowCount(); row++) {
                    if (clients.getValueAt(row, 0).equals(selectedSession)) {
                        int viewRow = connections.convertRowIndexToView(row);
                        connections.setRowSelectionInterval(viewRow, viewRow);
                        break;
                    }
                }
            } finally {
                updatingClients = false;
            }
            renderLogs();
        });
    }

    private static JPanel section(String title, JTable table) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel(title), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private static DefaultTableModel model(String... names) {
        return new DefaultTableModel(names, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
    }
}
