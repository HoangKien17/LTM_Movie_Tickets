package web;

import java.awt.*;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import protocol.Protocol;

/**
 * Một phòng theo suất đang chọn. Chỉ EDT chạm vào trạng thái/UI; không đọc
 * socket.
 */
final class ShowChatPanel extends JPanel {

    static final String CLOSED = "Suất chiếu đã kết thúc. Phòng chat đã đóng.";
    private final Supplier<MovieClient> connection;
    private final IntSupplier ownUser;
    private final BooleanSupplier reading;
    private final IntConsumer unreadChanged;
    private final ExecutorService requests = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "chat-requests");
        t.setDaemon(true);
        return t;
    });
    private final JLabel title = label("Phòng chat suất chiếu", 18, UiTheme.TEXT);
    private final JLabel movie = label("Chọn suất chiếu để trò chuyện", 15, UiTheme.TEXT);
    private final JLabel details = label(" ", 12, UiTheme.MUTED);
    private final JLabel roomStatus = label(" ", 12, UiTheme.MUTED);
    private final JLabel members = label("Chưa tham gia phòng", 12, UiTheme.GREEN);
    private final JPanel avatars = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JLabel notice = label(" ", 12, UiTheme.MUTED);
    private final JLabel typing = label(" ", 12, UiTheme.MUTED);
    private final JLabel feedback = label(" ", 12, UiTheme.MUTED);
    private final JTextField input = UiTheme.hintField(20, "Nhắn tin trong phòng...");
    private final JButton send = UiTheme.primary("Gửi  →");
    private final JButton older = UiTheme.secondary("Tải tin nhắn cũ hơn");
    private final JPanel messages = new MessageList();
    private final JScrollPane scroll = UiTheme.scroll(messages);
    private final NavigableMap<Long, String[]> history = new TreeMap<>();
    private final Map<Integer, String> people = new TreeMap<>();
    private final javax.swing.Timer clock = new javax.swing.Timer(500, e -> updateClock());
    private int showId, generation, unread, memberRevision, stateRevision;
    private boolean ready, open, sending, loadingOlder;
    private LocalDateTime serverNow, end;
    private long receivedNanos, typingUntil, lastTypingNanos;
    private String[] typingPerson;

    ShowChatPanel(Supplier<MovieClient> connection, IntSupplier ownUser, BooleanSupplier reading, IntConsumer unreadChanged) {
        super(new BorderLayout(0, 10));
        this.connection = connection;
        this.ownUser = ownUser;
        this.reading = reading;
        this.unreadChanged = unreadChanged;
        setOpaque(false);
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        for (JLabel item : new JLabel[]{title, movie, details, roomStatus, members, notice}) {
            item.setAlignmentX(LEFT_ALIGNMENT);
            top.add(item);
            top.add(Box.createVerticalStrut(5));
        }
        avatars.setOpaque(false);
        avatars.setAlignmentX(LEFT_ALIGNMENT);
        avatars.setPreferredSize(new Dimension(200, 44));
        avatars.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        top.add(avatars);
        older.setAlignmentX(LEFT_ALIGNMENT);
        older.addActionListener(e -> loadOlder());
        top.add(older);
        add(top, BorderLayout.NORTH);
        messages.setOpaque(false);
        messages.setLayout(new BoxLayout(messages, BoxLayout.Y_AXIS));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(UiTheme.SURFACE);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                renderMessages(false);
            }
        });
        add(scroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 5));
        bottom.setOpaque(false);
        bottom.add(typing, BorderLayout.NORTH);
        JPanel composer = new JPanel(new BorderLayout(8, 0));
        composer.setOpaque(false);
        input.setPreferredSize(new Dimension(150, 42));
        send.setPreferredSize(new Dimension(85, 42));
        composer.add(input, BorderLayout.CENTER);
        composer.add(send, BorderLayout.EAST);
        bottom.add(composer, BorderLayout.CENTER);
        bottom.add(feedback, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
        ((AbstractDocument) input.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                String next = current.substring(0, offset) + (text == null ? "" : text) + current.substring(offset + length);
                if (next.codePointCount(0, next.length()) <= 500) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }

            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs) throws BadLocationException {
                replace(fb, offset, 0, text, attrs);
            }
        });
        input.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                localTyping();
            }

            public void removeUpdate(DocumentEvent e) {
                localTyping();
            }

            public void changedUpdate(DocumentEvent e) {
                localTyping();
            }
        });
        input.addActionListener(e -> sendMessage());
        send.addActionListener(e -> sendMessage());
        reset();
    }

    void setRoom(int id) {
        if (id == showId) {
            return;
        }
        reset();
        showId = id;
        if (id < 1) {
            return;
        }
        clock.start();
        feedback.setText("Đang tải phòng chat...");
        int membersBefore = memberRevision, stateBefore = stateRevision;
        request("JOIN_CHAT;" + id, lines -> {
            if (!ok(lines, "CHAT")) {
                return;
            }
            ready = true;
            if (stateRevision == stateBefore) {
                applyRoom(lines);
            }
            if (memberRevision == membersBefore) {
                applyMembers(lines);
            }
            mergeHistory(lines);
            feedback.setText("Enter để gửi • tối đa 500 ký tự");
            updateControls();
            renderMessages(true);
        });
    }

    void leaveRoom() {
        if (showId > 0 && connection.get() != null) {
            request("LEAVE_CHAT", lines -> {
            });
        }
        reset();
    }

    void reset() {
        generation++;
        showId = 0;
        ready = false;
        open = false;
        sending = false;
        loadingOlder = false;
        memberRevision = 0;
        stateRevision = 0;
        unread = 0;
        unreadChanged.accept(0);
        history.clear();
        people.clear();
        end = null;
        serverNow = null;
        typingPerson = null;
        typingUntil = 0;
        lastTypingNanos = 0;
        input.setText("");
        movie.setText("Chọn suất chiếu để trò chuyện");
        details.setText(" ");
        roomStatus.setText(" ");
        members.setText("Chưa tham gia phòng");
        notice.setText(" ");
        typing.setText(" ");
        feedback.setText("Chọn suất chiếu ở phía trên");
        older.setVisible(false);
        avatars.removeAll();
        clock.stop();
        messages.removeAll();
        messages.revalidate();
        messages.repaint();
        updateControls();
    }

    void shutdown() {
        reset();
        requests.shutdownNow();
    }

    void markRead() {
        unread = 0;
        unreadChanged.accept(0);
    }

    void event(List<String> frame) {
        if (frame.isEmpty()) {
            return;
        }
        String[] header = frame.get(0).split(";", -1);
        if (header.length < 3 || Integer.parseInt(header[2]) != showId) {
            return;
        }
        switch (header[1]) {
            case "CHAT_MESSAGE":
                for (String line : frame) {
                    if (line.startsWith("CHAT_MSG;")) {
                        String[] data = decode(line, 6);
                        long id = Long.parseLong(data[0]);
                        if (!history.containsKey(id)) {
                            history.put(id, data);
                            if (Integer.parseInt(data[2]) != ownUser.getAsInt() && !reading.getAsBoolean()) {
                                unreadChanged.accept(++unread);
                            }
                        }
                        if (typingPerson != null && typingPerson[0].equals(data[2])) {
                            typingPerson = null;
                        }
                    }
                }
                renderMessages(true);
                break;
            case "CHAT_MEMBERS":
                memberRevision++;
                applyMembers(frame);
                for (String line : frame) {
                    if (line.startsWith("CHAT_NOTICE;")) {
                        notice.setText(decode(line, 1)[0]);
                    }
                }
                break;
            case "CHAT_STATE":
                stateRevision++;
                applyRoom(frame);
                break;
            case "CHAT_CLOSED":
                stateRevision++;
                open = false;
                roomStatus.setText(CLOSED);
                updateControls();
                break;
            case "CHAT_TYPING":
                if (open && Integer.parseInt(Protocol.decode(header[3])) != ownUser.getAsInt()) {
                    typingPerson = new String[]{Protocol.decode(header[3]), Protocol.decode(header[4])};
                    typingUntil = System.nanoTime() + 3_000_000_000L;
                    updateClock();
                }
                break;
            default:
                break;
        }
    }

    private void applyRoom(List<String> lines) {
        for (String line : lines) {
            if (line.startsWith("CHAT_ROOM;")) {
                String[] data = decode(line, 7);
                movie.setText(data[1]);
                movie.setToolTipText(data[1]);
                details.setText(data[2] + " • " + format(data[3], "HH:mm • dd/MM/yyyy"));
                details.setToolTipText("Kết thúc: " + format(data[4], "HH:mm • dd/MM/yyyy"));
                end = LocalDateTime.parse(data[4]);
                serverNow = LocalDateTime.parse(data[6]);
                receivedNanos = System.nanoTime();
                open = "1".equals(data[5]);
                updateClock();
            }
        }
    }

    private void applyMembers(List<String> lines) {
        people.clear();
        for (String line : lines) {
            if (line.startsWith("CHAT_MEMBER;")) {
                String[] data = decode(line, 2);
                people.put(Integer.parseInt(data[0]), data[1]);
            }
        }
        members.setText("● " + people.size() + " người trong phòng");
        String names = String.join(", ", people.values());
        notice.setText(names.isEmpty() ? " " : names);
        members.setToolTipText(names);
        avatars.removeAll();
        int count = 0;
        for (Map.Entry<Integer, String> person : people.entrySet()) {
            if (count++ >= 8) {
                break;
            }
            String name = person.getValue();
            String initial = name.isEmpty() ? "?" : new String(Character.toChars(name.codePointAt(0))).toUpperCase(Locale.ROOT);
            JPanel avatar = new JPanel() {
                @Override
                protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(UiTheme.PRIMARY_PALE);
                    g.fillOval(1, 1, 34, 34);
                    g.setColor(UiTheme.PURPLE);
                    g.drawOval(1, 1, 34, 34);
                    g.setFont(UiTheme.font(15, Font.BOLD));
                    g.setColor(UiTheme.TEXT);
                    g.drawString(initial, 18 - g.getFontMetrics().stringWidth(initial) / 2, 24);
                    g.setColor(UiTheme.GREEN);
                    g.fillOval(27, 27, 8, 8);
                    g.dispose();
                }
            };
            avatar.setOpaque(false);
            avatar.setPreferredSize(new Dimension(38, 40));
            avatar.setToolTipText(name);
            avatars.add(avatar);
        }
        avatars.revalidate();
        avatars.repaint();
    }

    private void mergeHistory(List<String> lines) {
        for (String line : lines) {
            if (line.startsWith("CHAT_MSG;")) {
                String[] data = decode(line, 6);
                history.put(Long.parseLong(data[0]), data);
            } else if (line.startsWith("MORE;")) {
                older.setVisible("MORE;1".equals(line));
            }
        }
    }

    private void loadOlder() {
        if (!ready || loadingOlder || history.isEmpty()) {
            return;
        }
        loadingOlder = true;
        older.setEnabled(false);
        request("GET_CHAT_HISTORY;" + showId + ";" + history.firstKey(), lines -> {
            loadingOlder = false;
            older.setEnabled(true);
            if (ok(lines, "CHAT_HISTORY")) {
                mergeHistory(lines);
                renderMessages(false);
            }
        });
    }

    private void sendMessage() {
        updateClock();
        if (!ready || !open || sending || input.getText().strip().isEmpty()) {
            return;
        }
        String text = input.getText();
        sending = true;
        updateControls();
        feedback.setText("Đang gửi...");
        request("SEND_CHAT;" + showId + ";" + Protocol.encode(text), lines -> {
            sending = false;
            if (ok(lines, "CHAT_SENT")) {
                input.setText("");
                feedback.setText("Đã gửi • Enter để gửi tiếp");
            }
            updateControls();
            if (open) {
                input.requestFocusInWindow();
            }
        });
    }

    private void localTyping() {
        long now = System.nanoTime();
        if (!ready || !open || input.getText().isBlank() || sending || now - lastTypingNanos < 1_500_000_000L) {
            return;
        }
        lastTypingNanos = now;
        request("CHAT_TYPING;" + showId + ";1", lines -> {
        });
    }

    private void updateClock() {
        if (open && serverNow != null && end != null
                && !serverNow.plusNanos(Math.max(0, System.nanoTime() - receivedNanos)).isBefore(end)) {
            open = false;
        }
        if (end != null) {
            roomStatus.setText(open ? "Đang mở • kết thúc lúc " + format(end.toString(), "HH:mm") : CLOSED);
            roomStatus.setForeground(open ? UiTheme.GREEN : UiTheme.GOLD);
            roomStatus.setToolTipText(roomStatus.getText());
        }
        typing.setText(open && typingPerson != null && System.nanoTime() < typingUntil ? typingPerson[1] + " đang nhập..." : " ");
        updateControls();
    }

    private void updateControls() {
        input.setEnabled(ready && open && !sending);
        send.setEnabled(ready && open && !sending);
    }

    private void request(String command, Consumer<List<String>> done) {
        MovieClient target = connection.get();
        int revision = generation;
        if (target == null) {
            feedback.setText("Chưa kết nối máy chủ");
            return;
        }
        if (requests.isShutdown()) {
            return;
        }
        requests.execute(() -> {
            try {
                List<String> result = target.request(command);
                SwingUtilities.invokeLater(() -> {
                    if (revision != generation || target != connection.get()) {
                        return;
                    }
                    try {
                        done.accept(result);
                    } catch (RuntimeException ex) {
                        feedback.setText("Dữ liệu chat không hợp lệ");
                    }
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    if (revision == generation && target == connection.get()) {
                        ready = false;
                        sending = false;
                        loadingOlder = false;
                        updateControls();
                        feedback.setText("Mất kết nối máy chủ. Hãy đăng nhập lại.");
                    }
                });
            }
        });
    }

    private boolean ok(List<String> lines, String kind) {
        boolean success = !lines.isEmpty() && (lines.get(0).equals("OK;" + kind) || lines.get(0).startsWith("OK;" + kind + ";"));
        if (!success) {
            String error = !lines.isEmpty() && lines.get(0).startsWith("ERR;") ? lines.get(0).substring(4) : "Không tải được chat";
            feedback.setText(error);
            feedback.setToolTipText(error);
            if (error.contains("Phòng chat đã đóng")) {
                open = false;
                roomStatus.setText(CLOSED);
                updateControls();
            }
        }
        return success;
    }

    private void renderMessages(boolean bottom) {
        int previous = scroll.getVerticalScrollBar().getValue();
        messages.removeAll();
        int viewportWidth = Math.max(1, scroll.getViewport().getWidth());
        int maxBubbleWidth = Math.max(1, (int) (viewportWidth * .80) - 10);
        int maxContentWidth = Math.max(1, maxBubbleWidth - 20);
        for (String[] data : history.values()) {
            boolean own = Integer.parseInt(data[2]) == ownUser.getAsInt();
            JPanel row = new JPanel(new FlowLayout(own ? FlowLayout.RIGHT : FlowLayout.LEFT, 5, 4));
            row.setOpaque(false);
            JPanel bubble = new UiTheme.RoundedPanel(own ? new Color(22, 63, 80) : UiTheme.NAVY_LIGHT, 12);
            bubble.setLayout(new BorderLayout(0, 5));
            bubble.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            JLabel sender = label(data[3] + (own ? " • Bạn" : "") + "   " + format(data[4], "HH:mm"), 11, own ? UiTheme.PRIMARY : UiTheme.MUTED);
            sender.setToolTipText(sender.getText());
            bubble.add(sender, BorderLayout.NORTH);
            JTextArea text = UiTheme.copy(data[5], 13);
            text.setForeground(UiTheme.TEXT);
            text.setEditable(false);
            text.setFocusable(false);
            text.setBorder(null);
            text.setMargin(new Insets(0, 0, 0, 0));
            text.setLineWrap(false);
            int naturalWidth = text.getPreferredSize().width + 4;
            text.setLineWrap(true);
            // Tin ngắn co theo nội dung và tên/giờ gửi; tin dài giới hạn 80% khung chat.
            int width = Math.max(1, Math.min(maxContentWidth, Math.max(naturalWidth, sender.getPreferredSize().width)));
            text.setSize(width, Short.MAX_VALUE);
            int height = Math.max(text.getFontMetrics(text.getFont()).getHeight(), text.getPreferredSize().height) + 2;
            text.setPreferredSize(new Dimension(width, height));
            int bubbleHeight = 16 + sender.getPreferredSize().height + 5 + height;
            bubble.setPreferredSize(new Dimension(width + 20, bubbleHeight));
            bubble.add(text, BorderLayout.CENTER);
            row.add(bubble);
            row.setAlignmentX(LEFT_ALIGNMENT);
            row.setPreferredSize(new Dimension(viewportWidth, bubbleHeight + 8));
            row.setMinimumSize(new Dimension(0, bubbleHeight + 8));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, bubbleHeight + 8));
            messages.add(row);
        }
        if (history.isEmpty()) {
            messages.add(label("Phòng chưa có tin nhắn.", 12, UiTheme.MUTED));
        }
        messages.revalidate();
        messages.repaint();
        SwingUtilities.invokeLater(() -> {
            if (bottom) {
                scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum()); 
            }else {
                scroll.getVerticalScrollBar().setValue(previous);
            }
        });
    }

    private static String[] decode(String line, int count) {
        String[] fields = line.split(";", -1);
        if (fields.length != count + 1) {
            throw new IllegalArgumentException("Sai số trường chat");
        }
        String[] data = new String[count];
        for (int i = 0; i < count; i++) {
            data[i] = Protocol.decode(fields[i + 1]);
        
        }return data;
    }

    private static String format(String value, String pattern) {
        try {
            return LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern(pattern));
        } catch (RuntimeException ex) {
            return value;
        }
    }

    private static JLabel label(String text, int size, Color color) {
        JLabel result = new JLabel(text);
        result.putClientProperty("html.disable", Boolean.TRUE);
        result.setFont(UiTheme.font(size, size >= 15 ? Font.BOLD : Font.PLAIN));
        result.setForeground(color);
        return result;
    }

    @Override
    public void doLayout() {
        boolean compact = getHeight() < 380;
        title.setVisible(!compact);
        notice.setVisible(!compact);
        avatars.setVisible(!compact && !people.isEmpty());
        typing.setVisible(!compact || typingPerson != null && System.nanoTime() < typingUntil);
        feedback.setVisible(getHeight() >= 340);
        super.doLayout();
    }

    private static final class MessageList extends JPanel implements Scrollable {

        public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(250, 260);
        }

        public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
            return 18;
        }

        public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return 150;
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
