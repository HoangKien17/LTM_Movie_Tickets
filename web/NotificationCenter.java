package web;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** The bell holds server data for the current session only. Read state lives in MySQL. */
final class NotificationCenter extends JButton {
    private final List<String[]> rows=new ArrayList<>();
    private final JPopupMenu popup=new JPopupMenu();
    private BiConsumer<String,String> open=(id,booking)->{};
    private Consumer<String> mark=ids->{};
    private Runnable refresh=()->{};
    NotificationCenter() {
        setName("notification-bell");setIcon(HomeIcons.icon("bell",24));setForeground(UiTheme.MUTED);
        setContentAreaFilled(false);setBorderPainted(false);setFocusPainted(false);setToolTipText("Thông báo");
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        popup.setName("notification-popup");popup.setBorder(BorderFactory.createLineBorder(UiTheme.LINE));
        addActionListener(e->{rebuild();popup.show(this,Math.min(0,getWidth()-400),getHeight()+12);refresh.run();});
    }
    void actions(Runnable refresh,BiConsumer<String,String> open,Consumer<String> mark){this.refresh=refresh;this.open=open;this.mark=mark;}
    void markLocally(String ids){java.util.Set<String> seen=new java.util.HashSet<>(java.util.Arrays.asList(ids.split(",")));for(String[] row:rows)if(seen.contains(row[0]))row[8]="1";repaint();if(popup.isVisible()){rebuild();popup.pack();}}
    void update(List<String[]> values) {rows.clear();for(String[] row:values)rows.add(row.clone());setToolTipText("Thông báo • "+unread()+" chưa đọc");repaint();if(popup.isVisible()){rebuild();popup.pack();}}
    void clear(){rows.clear();popup.setVisible(false);popup.removeAll();setToolTipText("Thông báo");repaint();}
    int unread(){int n=0;for(String[] row:rows)if("0".equals(row[8]))n++;return n;}
    JPanel panel(){rebuild();return (JPanel)popup.getComponent(0);}
    private void rebuild(){
        popup.removeAll();JPanel panel=new JPanel(new BorderLayout(0,14));panel.setBackground(UiTheme.NAVY);panel.setBorder(new EmptyBorder(18,18,18,18));
        JLabel title=new JLabel("Thông báo");title.setFont(UiTheme.font(21,Font.BOLD));
        JButton read=UiTheme.subtle("Đánh dấu đã đọc");read.setName("notifications-read-all");read.setEnabled(unread()>0);
        read.addActionListener(e->{List<String> ids=new ArrayList<>();for(String[] row:rows)if("0".equals(row[8]))ids.add(row[0]);mark.accept(String.join(",",ids));});
        JPanel head=new JPanel(new BorderLayout());head.setOpaque(false);head.add(title,BorderLayout.WEST);head.add(read,BorderLayout.EAST);panel.add(head,BorderLayout.NORTH);
        JPanel list=new JPanel();list.setOpaque(false);list.setLayout(new BoxLayout(list,BoxLayout.Y_AXIS));
        if(rows.isEmpty()){JTextArea empty=UiTheme.copy("Bạn chưa có thông báo. Vé đã thanh toán và lời nhắc trước giờ chiếu sẽ xuất hiện tại đây.",14);empty.setBorder(new EmptyBorder(14,0,14,0));list.add(empty);}
        for(String[] row:rows){
            boolean reminder="show_reminder".equals(row[1]);
            JButton item=new JButton();item.setName("notification-"+row[0]);item.setLayout(new BorderLayout(10,6));
            item.setBackground("0".equals(row[8])?new Color(20,40,57):UiTheme.SURFACE);item.setContentAreaFilled(true);item.setOpaque(true);
            item.setBorder(new EmptyBorder(13,12,13,12));item.setMaximumSize(new Dimension(Integer.MAX_VALUE,145));
            JLabel icon=new JLabel(HomeIcons.icon(reminder?"calendar":"check",23));icon.setForeground(reminder?UiTheme.PURPLE:UiTheme.PRIMARY);icon.setVerticalAlignment(SwingConstants.TOP);item.add(icon,BorderLayout.WEST);
            JPanel words=new JPanel(new BorderLayout(0,5));words.setOpaque(false);
            JLabel label=new JLabel(reminder?"Phim sắp bắt đầu":"Đặt vé thành công");label.setFont(UiTheme.font(14,Font.BOLD));label.setForeground(UiTheme.TEXT);words.add(label,BorderLayout.NORTH);
            String reminderText="";
            if(reminder){try{long remaining=Math.max(1,java.time.Duration.between(LocalDateTime.now(),LocalDateTime.parse(row[5])).toMinutes());reminderText="Còn khoảng "+remaining+" phút\n";}catch(RuntimeException ignored){}}
            JTextArea detail=UiTheme.copy(reminderText+row[3]+"\n"+row[4]+" • "+date(row[5])+"\nGhế "+row[6]+" • Đơn #"+row[2],13);words.add(detail,BorderLayout.CENTER);item.add(words,BorderLayout.CENTER);
            item.addActionListener(e->{popup.setVisible(false);open.accept(row[0],row[2]);});
            list.add(item);list.add(Box.createVerticalStrut(8));
        }
        JScrollPane scroll=UiTheme.scroll(list);scroll.setPreferredSize(new Dimension(365,rows.isEmpty()?100:Math.min(430,rows.size()*145)));panel.add(scroll,BorderLayout.CENTER);
        popup.add(panel);
    }
    private static String date(String value){try{return LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("HH:mm • dd/MM/yyyy"));}catch(RuntimeException ex){return value;}}
    @Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);int n=unread();if(n==0)return;Graphics2D g=(Graphics2D)graphics.create();AuthStyle.quality(g);g.setColor(UiTheme.PRIMARY);g.fillOval(getWidth()-19,2,18,18);g.setColor(UiTheme.BACKGROUND);g.setFont(UiTheme.font(10,Font.BOLD));String text=n>9?"9+":Integer.toString(n);g.drawString(text,getWidth()-10-g.getFontMetrics().stringWidth(text)/2,15);g.dispose();}
}
