package tests;

import protocol.Protocol;
import protocol.TicketCode;
import server.ServerMonitor;
import utils.QrTickets;
import web.SeatMapPanel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.function.*;
import javax.imageio.ImageIO;
import javax.swing.*;

public final class FeatureUnitTest {
    private static final Path QA=Path.of("../feature-qa");
    public static void main(String[] args) throws Exception {
        Files.createDirectories(QA);
        String token="a".repeat(64), payload=TicketCode.payload(token);
        BufferedImage qr=QrTickets.encode(payload);
        Path qrFile=QA.resolve("qr-roundtrip.png");
        ImageIO.write(qr,"png",qrFile.toFile());
        check(QrTickets.decode(qrFile.toFile()).equals(payload),"QR encode/decode round-trip");
        try { TicketCode.token("anything"); throw new AssertionError("invalid token accepted"); }
        catch(IllegalArgumentException expected) { }
        Path invalid=QA.resolve("not-an-image.txt");
        Files.writeString(invalid,"not a QR");
        try { QrTickets.decode(invalid.toFile()); throw new AssertionError("invalid image accepted"); }
        catch(java.io.IOException expected) { }
        monitorSafety();
        FutureTask<Void> task=new FutureTask<>(() -> {
            seatLayout();
            ticketView(token);
            checkInView(token);
            monitorView();
            return null;
        });
        SwingUtilities.invokeAndWait(task);task.get();
        System.out.println("PASS: QR round-trip, invalid inputs, safe/bounded monitor, seat coordinates, QR/check-in/monitor Swing panels");
    }

    private static void monitorSafety() {
        ServerMonitor monitor=new ServerMonitor();
        long id=monitor.connected("127.0.0.1:1");
        String secret="private-password-test";
        monitor.completed(id,"LOGIN;"+Protocol.encode("private-email")+";"+Protocol.encode(secret),
                Arrays.asList("ERR;Login failed"),123);
        monitor.completed(id,secret,Arrays.asList("ERR;Unknown"),123);
        StringBuilder all=new StringBuilder();
        for(String line:monitor.snapshot()) {
            String[] f=line.split(";",-1);
            if(f[0].equals("OK"))continue;
            for(int i=1;i<f.length;i++)all.append(Protocol.decode(f[i]));
        }
        check(!all.toString().contains(secret) && !all.toString().contains(Protocol.encode(secret)),"log redaction");
        for(int i=0;i<600;i++)monitor.completed(id,"UNKNOWN",Arrays.asList("ERR;Bad"),200);
        long count=monitor.snapshot().stream().filter(l->l.startsWith("LOG;")).count();
        check(count==500,"bounded event log");
    }

    private static void seatLayout() throws Exception {
        SeatMapPanel panel=new SeatMapPanel();
        List<String[]> rows=new ArrayList<>();
        for(char r='A';r<='B';r++)for(int col=1;col<=12;col++) {
            String code=""+r+col;
            rows.add(new String[]{code,code.equals("A10")?"taken":"free"});
        }
        Collections.shuffle(rows,new Random(42));
        Method render=SeatMapPanel.class.getDeclaredMethod("render",List.class,Set.class,Runnable.class);
        render.setAccessible(true);
        render.invoke(panel,rows,new HashSet<>(Arrays.asList("A2","A10")), (Runnable)()->{});
        Map<String,JToggleButton> buttons=new HashMap<>();
        for(Component c:panel.getComponents())if(c instanceof JToggleButton)buttons.put(((JToggleButton)c).getText(),(JToggleButton)c);
        GridBagLayout layout=(GridBagLayout)panel.getLayout();
        check(buttons.size()==24,"24 seats");
        check(layout.getConstraints(buttons.get("A2")).gridx<layout.getConstraints(buttons.get("A10")).gridx,"numeric ordering");
        check(layout.getConstraints(buttons.get("B1")).gridy>layout.getConstraints(buttons.get("A1")).gridy,"row layout");
        check(layout.getConstraints(buttons.get("A7")).gridx-layout.getConstraints(buttons.get("A6")).gridx==2,"center aisle");
        check(buttons.get("A2").isSelected()&&!buttons.get("A10").isSelected()&&!buttons.get("A10").isEnabled(),"seat state preserved");
        capture(panel,"seat-layout.png",1000,300);
    }

    private static String[] ticket(String token,String checked) {
        return new String[]{"123","Phim mẫu Galaxy Cinema","Phòng 2","2026-10-25T19:30",
                "A1,A2,A10","active",checked,token};
    }

    private static void ticketView(String token) throws Exception {
        Class<?> type=Class.forName("web.ElectronicTicketDialog");
        Method content=type.getDeclaredMethod("content",String[].class,Component.class);
        content.setAccessible(true);
        JPanel panel=(JPanel)content.invoke(null,ticket(token,""),null);
        capture(panel,"electronic-ticket.png",900,410);
        JPanel used=(JPanel)content.invoke(null,ticket(token,"2026-10-25T19:00"),null);
        check(countIcons(used)==0,"used ticket hides QR");
    }

    private static int countIcons(Container c) {
        int found=0;
        for(Component child:c.getComponents()) {
            if(child instanceof JLabel && ((JLabel)child).getIcon()!=null)found++;
            if(child instanceof Container)found+=countIcons((Container)child);
        }
        return found;
    }

    private static void checkInView(String token) throws Exception {
        Class<?> type=Class.forName("web.TicketCheckInPanel");
        Constructor<?> constructor=type.getDeclaredConstructor(BiConsumer.class,BooleanSupplier.class);
        constructor.setAccessible(true);
        BiConsumer<String,Consumer<List<String>>> sender=(command,done)-> {
            boolean checked=command.startsWith("CHECK_IN;");
            StringBuilder row=new StringBuilder("TICKET");
            for(String value:ticket(token,checked?"2026-10-25T19:00":""))row.append(';').append(Protocol.encode(value));
            done.accept(Arrays.asList("OK;"+(checked?"CHECKED_IN":"E_TICKET")+";1",row.toString()));
        };
        JPanel panel=(JPanel)constructor.newInstance(sender,(BooleanSupplier)()->true);
        JTextArea input=(JTextArea)field(panel,"input");
        input.setText(TicketCode.payload(token));
        invoke(panel,"inspect");
        check(((JButton)field(panel,"confirm")).isEnabled(),"valid preview allows confirm");
        capture(panel,"check-in.png",1000,600);
        invoke(panel,"checkIn");
        check(!((JButton)field(panel,"confirm")).isEnabled(),"successful check-in disables confirm");
        check(((JTextArea)field(panel,"result")).getText().contains("ĐÃ XÁC NHẬN"),"check-in result shown");
        input.setText("");
        check(!((JButton)field(panel,"confirm")).isEnabled(),"edited code invalidates preview");
    }

    private static void monitorView() throws Exception {
        ServerMonitor monitor=new ServerMonitor();
        long a=monitor.connected("192.168.1.10:51742"),b=monitor.connected("192.168.1.11:51802");
        monitor.completed(a,"BOOK_TICKET;1;"+Protocol.encode("A1"),Arrays.asList("OK;BOOKED;123"),12500000);
        monitor.completed(b,"CANCEL_TICKET;124",Arrays.asList("OK;CANCELLED"),2300000);
        Class<?> type=Class.forName("web.ServerMonitorPanel");
        Constructor<?> constructor=type.getDeclaredConstructor(BiConsumer.class,BooleanSupplier.class);
        constructor.setAccessible(true);
        BiConsumer<String,Consumer<List<String>>> sender=(command,done)->done.accept(monitor.snapshot());
        JPanel panel=(JPanel)constructor.newInstance(sender,(BooleanSupplier)()->true);
        invoke(panel,"start");
        invoke(panel,"stop");
        javax.swing.table.DefaultTableModel logModel=(javax.swing.table.DefaultTableModel)field(panel,"logs");
        check(logModel.getValueAt(2,3).equals("BOOK_TICKET"),"event labels preserved");
        capture(panel,"server-monitor.png",1100,650);
    }

    private static Object field(Object object,String name) throws Exception {
        Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);
    }
    private static void invoke(Object object,String name) throws Exception {
        Method m=object.getClass().getDeclaredMethod(name);m.setAccessible(true);m.invoke(object);
    }
    private static void capture(JPanel panel,String name,int width,int height) throws Exception {
        panel.setSize(width,height);
        layout(panel);
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();
        ImageIO.write(image,"png",QA.resolve(name).toFile());
    }
    private static void layout(Container c) {c.doLayout();for(Component child:c.getComponents())if(child instanceof Container)layout((Container)child);}
    private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}
}
