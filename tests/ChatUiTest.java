package tests;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import protocol.Protocol;
import web.BasicClientUI;
import web.MovieClient;

/** Giao diện thật + TCP/MySQL thật, render PNG không cần mở cửa sổ. */
public final class ChatUiTest {
    private static final Path QA=Path.of("../chat-ui-qa");
    public static void main(String[] args) throws Exception {
        ChatFeatureTest.prepare(); Files.createDirectories(QA);
        try(ChatFeatureTest.Harness server=new ChatFeatureTest.Harness(); MovieClient a=server.client(); MovieClient b=server.client()) {
            ChatFeatureTest.login(a,1); ChatFeatureTest.login(b,2);
            b.request("JOIN_CHAT;"+ChatFeatureTest.FUTURE);
            b.request("SEND_CHAT;"+ChatFeatureTest.FUTURE+";"+Protocol.encode("Mọi người chọn hàng C nhé?"));
            BasicClientUI ui=edt(BasicClientUI::new);
            try {
                a.setChatEventListener(event->SwingUtilities.invokeLater(()->{
                    try { invoke(get(ui,"chatPanel"),"event",new Class[]{List.class},event); }
                    catch(Exception ex) { throw new RuntimeException(ex); }
                }));
                edt(()->{
                    ui.addNotify(); set(ui,"client",a); set(ui,"currentUserId",900001); set(ui,"loggedIn",true);
                    ((JLabel)get(ui,"userLabel")).setText("Xin chào, Kiên");
                    ((CardLayout)get(ui,"screenLayout")).show((JPanel)get(ui,"screens"),"MAIN");
                    invoke(ui,"navigate",new Class[]{String.class,boolean.class},"SHOWS",false);
                    invoke(ui,"loadShows",new Class[]{int.class},0);
                    return null;
                });
                await(()->!busy(ui)&&((JComboBox<?>)get(ui,"showPicker")).getItemCount()>0,"shows");
                edt(()->{
                    DefaultTableModel model=(DefaultTableModel)get(ui,"showModel");
                    for(int i=0;i<model.getRowCount();i++) if(model.getValueAt(i,0).equals(Integer.toString(ChatFeatureTest.FUTURE)))
                        ((JComboBox<?>)get(ui,"showPicker")).setSelectedIndex(i);
                    return null;
                });
                await(()->!busy(ui)&&(int)get(ui,"seatMapShowId")==ChatFeatureTest.FUTURE&&(boolean)get(get(ui,"chatPanel"),"ready"),"seats and chat");
                Thread.sleep(550);
                b.request("SEND_CHAT;"+ChatFeatureTest.FUTURE+";"+Protocol.encode("Mình chọn C7, gần lối đi."));
                await(()->(int)get(get(ui,"chatPanel"),"unread")==1,"unread badge in booking tab");
                edt(()->{
                    check(((JTabbedPane)get(ui,"bookingChatTabs")).getTitleAt(1).contains("1"),"badge title");
                    ((JTabbedPane)get(ui,"bookingChatTabs")).setSelectedIndex(1);
                    check((int)get(get(ui,"chatPanel"),"unread")==0,"opening chat clears unread");
                    JTextField composer=(JTextField)get(get(ui,"chatPanel"),"input");
                    composer.setText("Mình đang chọn C4 và C5.");
                    invoke(get(ui,"chatPanel"),"sendMessage",new Class[0]);
                    return null;
                });
                await(()->!((boolean)get(get(ui,"chatPanel"),"sending"))&&((java.util.Map<?,?>)get(get(ui,"chatPanel"),"history")).size()==3,"send UI and push");
                edt(()->{
                    for(Component c:((JPanel)get(ui,"seatGrid")).getComponents()) if(c instanceof JToggleButton
                            && (((JToggleButton)c).getText().equals("C4")||((JToggleButton)c).getText().equals("C5"))) ((JToggleButton)c).doClick();
                    capture(ui,"01-chat-open.png",1440,900);
                    JPanel seats=(JPanel)((JPanel)get(ui,"seatGrid")).getParent().getParent().getParent();
                    Container summary=((JTabbedPane)get(ui,"bookingChatTabs")).getParent();
                    check(Math.abs(seats.getWidth()/(double)(seats.getWidth()+summary.getWidth())-.58)<.01,"58/42 ratio");
                    capture(ui,"02-chat-open-small.png",1100,760);
                    JScrollPane transcript=(JScrollPane)get(get(ui,"chatPanel"),"scroll");
                    check(transcript.getViewport().getHeight()>=65,"small window retains readable transcript");
                    ((JTabbedPane)get(ui,"bookingChatTabs")).setSelectedIndex(0);
                    capture(ui,"03-booking-tab.png",1440,900);
                    ((JTabbedPane)get(ui,"bookingChatTabs")).setSelectedIndex(1);
                    DefaultTableModel model=(DefaultTableModel)get(ui,"showModel");
                    for(int i=0;i<model.getRowCount();i++) if(model.getValueAt(i,0).equals(Integer.toString(ChatFeatureTest.ENDED)))
                        ((JComboBox<?>)get(ui,"showPicker")).setSelectedIndex(i);
                    return null;
                });
                await(()->!busy(ui)&&(int)get(ui,"seatMapShowId")==ChatFeatureTest.ENDED
                        &&(boolean)get(get(ui,"chatPanel"),"ready")&&!(boolean)get(get(ui,"chatPanel"),"open"),"ended room read-only");
                edt(()->{
                    Object panel=get(ui,"chatPanel");
                    check(!((JTextField)get(panel,"input")).isEnabled(),"closed input disabled");
                    check(!((JButton)get(panel,"send")).isEnabled(),"closed send disabled");
                    check(((JLabel)get(panel,"roomStatus")).getText().equals("Suất chiếu đã kết thúc. Phòng chat đã đóng."),"exact closed copy");
                    capture(ui,"04-chat-closed.png",1440,900);
                    invoke(ui,"navigate",new Class[]{String.class,boolean.class},"MOVIES",false);
                    check((int)get(panel,"showId")==0,"navigation leaves room");
                    return null;
                });
                System.out.println("PASS: real Swing + TCP/MySQL, history, send, unread badge, tab reset, 1440/1100 layout, read-only copy/controls and navigation cleanup");
            } finally { edt(()->{ui.dispose();return null;}); }
        }
    }
    private static void capture(BasicClientUI ui,String name,int width,int height) throws Exception {
        Container pane=ui.getContentPane(); pane.setSize(width,height); layout(pane); layout(pane);
        // Component resize events and text wrapping need a second EDT pass; next capture remains deterministic.
        Object chat=get(ui,"chatPanel"); invoke(chat,"renderMessages",new Class[]{boolean.class},false); layout(pane);
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics(); pane.printAll(g);g.dispose();ImageIO.write(image,"png",QA.resolve(name).toFile());
    }
    private static void layout(Container c) { c.doLayout();for(Component child:c.getComponents())if(child instanceof Container)layout((Container)child); }
    private static boolean busy(BasicClientUI ui)throws Exception{return (boolean)get(ui,"busy");}
    private static Object get(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    private static void set(Object o,String name,Object value)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
    private static Object invoke(Object o,String name,Class<?>[] types,Object...args)throws Exception{Method m=o.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(o,args);}
    private static <T>T edt(Callable<T> task)throws Exception{FutureTask<T> f=new FutureTask<>(task);SwingUtilities.invokeAndWait(f);return f.get();}
    private static void await(Callable<Boolean> condition,String name)throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);while(System.nanoTime()<until){if(edt(condition))return;Thread.sleep(25);}throw new AssertionError("Timeout "+name);}
    private static void check(boolean b,String name){if(!b)throw new AssertionError(name);}
}
