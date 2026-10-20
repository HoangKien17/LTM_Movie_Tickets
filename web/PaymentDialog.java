package web;

import protocol.Protocol;
import utils.QrImages;
import java.awt.*;
import java.net.URI;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.*;
import javax.swing.*;

final class PaymentDialog {
    private PaymentDialog(){ }
    static void show(JFrame owner,String[] payment,BiConsumer<String,Consumer<List<String>>> send,Runnable paid) {
        new View(owner,payment,send,paid).open();
    }
    private static final class View {
        final JDialog dialog;final BiConsumer<String,Consumer<List<String>>> send;final Runnable paid;
        final JLabel status=new JLabel();final JLabel countdown=new JLabel();final Timer timer;
        String[] payment;
        View(JFrame owner,String[] payment,BiConsumer<String,Consumer<List<String>>> send,Runnable paid) {
            this.payment=payment;this.send=send;this.paid=paid;
            dialog=new JDialog(owner,"Thanh toán đơn #"+payment[0],true);
            JPanel root=new JPanel(new BorderLayout(18,12));root.setBorder(BorderFactory.createEmptyBorder(18,18,18,18));
            root.add(new JLabel(new ImageIcon(QrImages.encode(payment[3],360))),BorderLayout.WEST);
            JPanel info=new JPanel();info.setLayout(new BoxLayout(info,BoxLayout.Y_AXIS));
            info.add(new JLabel("Quét QR bằng ứng dụng ngân hàng"));info.add(Box.createVerticalStrut(12));
            JLabel amount=new JLabel(BasicClientUI.formatMoney(payment[1]));amount.setFont(UiTheme.font(24,Font.BOLD));info.add(amount);
            info.add(Box.createVerticalStrut(12));info.add(status);info.add(Box.createVerticalStrut(6));info.add(countdown);
            JTextArea note=new JTextArea("Server sẽ tự kiểm tra giao dịch. Không đóng ứng dụng hoặc đặt lại cùng ghế.\n"
                    +"Ghế được giữ đến thời điểm hiển thị bên dưới.");note.setEditable(false);note.setOpaque(false);note.setLineWrap(true);note.setWrapStyleWord(true);
            note.setMaximumSize(new Dimension(330,90));info.add(Box.createVerticalStrut(12));info.add(note);
            JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT));
            if(!payment[4].isEmpty()) { JButton open=UiTheme.secondary("Mở trang payOS");open.addActionListener(e->browse(payment[4]));buttons.add(open); }
            if("demo".equals(payment[6])) { JButton demo=UiTheme.primary("Giả lập đã thanh toán");demo.addActionListener(e->confirmDemo());buttons.add(demo); }
            JButton check=UiTheme.secondary("Kiểm tra ngay");check.addActionListener(e->refresh());buttons.add(check);
            info.add(Box.createVerticalGlue());info.add(buttons);root.add(info,BorderLayout.CENTER);
            dialog.setContentPane(root);dialog.pack();dialog.setResizable(false);dialog.setLocationRelativeTo(owner);
            timer=new Timer(3000,e->refresh());update();
        }
        void open(){timer.start();dialog.setVisible(true);timer.stop();}
        void update() {
            String state=payment[2];status.setText("Trạng thái: "+("paid".equals(state)?"ĐÃ THANH TOÁN":"Đang chờ thanh toán"));
            try { LocalDateTime expires=LocalDateTime.parse(payment[5]);long seconds=Duration.between(LocalDateTime.now(),expires).getSeconds();
                countdown.setText(seconds>0?"Hết hạn: "+expires.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                        :"Đơn đã hết thời gian thanh toán");
            }catch(RuntimeException ex){countdown.setText("");}
        }
        void refresh(){send.accept("GET_PAYMENT;"+payment[0],lines->{String[] next=decode(lines);if(next==null)return;payment=next;update();if("paid".equals(next[2]))finish();});}
        void confirmDemo(){send.accept("DEMO_CONFIRM_PAYMENT;"+payment[0],lines->{if(!lines.isEmpty()&&lines.get(0).startsWith("OK;PAID;"))refresh();});}
        void finish(){timer.stop();dialog.dispose();SwingUtilities.invokeLater(paid);}
        static String[] decode(List<String> lines) {
            if(lines.size()!=2||!lines.get(0).equals("OK;PAYMENT;1"))return null;
            String[] p=lines.get(1).split(";",-1);if(p.length!=9||!"PAYMENT".equals(p[0]))return null;
            String[] row=new String[8];for(int i=0;i<8;i++)row[i]=Protocol.decode(p[i+1]);return row;
        }
        static void browse(String url){try{if(Desktop.isDesktopSupported())Desktop.getDesktop().browse(URI.create(url));}catch(Exception ignored){}}
    }
}
