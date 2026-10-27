package tests;

import java.io.*;
import java.net.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import mysql.CSDL;
import protocol.Protocol;
import server.BasicServer;
import service.PasswordResetService;
import web.MovieClient;

/** TCP handler + repository MySQL thật. Chỉ dùng database galaxy_chat_test riêng. */
public final class ChatFeatureTest {
    public static final int FUTURE=900001, RUNNING=900002, ENDED=900003, OTHER=900004;
    public static final String PASSWORD="ChatTest123!";
    public static void main(String[] args) throws Exception {
        prepare();
        try (Harness server=new Harness(); MovieClient a=server.client(); MovieClient b=server.client(); MovieClient c=server.client()) {
            BlockingQueue<List<String>> ea=new LinkedBlockingQueue<>(), eb=new LinkedBlockingQueue<>(), ec=new LinkedBlockingQueue<>();
            a.setChatEventListener(ea::offer); b.setChatEventListener(eb::offer); c.setChatEventListener(ec::offer);
            error(a.request("JOIN_CHAT;"+FUTURE),"unauthenticated join");
            login(a,1); login(b,2); login(c,3);
            success(a.request("JOIN_CHAT;"+FUTURE),"CHAT");
            success(b.request("JOIN_CHAT;"+FUTURE),"CHAT");
            success(c.request("JOIN_CHAT;"+OTHER),"CHAT");
            List<String> members=event(ea,"CHAT_MEMBERS",2);
            while (members.stream().filter(l->l.startsWith("CHAT_MEMBER;")).count()!=2) members=event(ea,"CHAT_MEMBERS",2);
            ea.clear(); eb.clear(); ec.clear();
            String text="Chào Linh; ghế C4 còn trống? 🎬 <html>";
            success(a.request("SEND_CHAT;"+FUTURE+";"+Protocol.encode(text)),"CHAT_SENT");
            List<String> message=event(eb,"CHAT_MESSAGE",2);
            check(decode(message.get(1))[5].equals(text),"Unicode, delimiters and literal text round-trip");
            check(decode(event(ea,"CHAT_MESSAGE",2).get(1))[2].equals("900001"),"server supplies sender identity");
            c.request("UNKNOWN"); check(ec.isEmpty(),"room isolation");
            error(c.request("SEND_CHAT;"+FUTURE+";"+Protocol.encode("forged")),"membership checked");
            error(c.request("GET_CHAT_HISTORY;"+FUTURE+";0"),"history membership checked");
            error(c.request("JOIN_CHAT;99999999"),"nonexistent room");
            error(b.request("SEND_CHAT;"+FUTURE+";"+Protocol.encode(" ")),"blank rejected");
            error(b.request("SEND_CHAT;"+FUTURE+";"+Protocol.encode("a".repeat(501))),"length checked");
            error(b.request("SEND_CHAT;"+FUTURE+";"+Protocol.encode("a\nb")),"control characters rejected");
            success(b.request("CHAT_TYPING;"+FUTURE+";1"),"CHAT_TYPING");
            event(ea,"CHAT_TYPING",2);
            success(a.request("JOIN_CHAT;"+RUNNING),"CHAT");
            success(a.request("SEND_CHAT;"+RUNNING+";"+Protocol.encode("Phim đang chiếu vẫn chat được")),"CHAT_SENT");
            event(ea,"CHAT_MESSAGE",2);
            success(c.request("JOIN_CHAT;"+ENDED),"CHAT");
            List<String> closed=c.request("JOIN_CHAT;"+ENDED);
            check(decode(closed.stream().filter(l->l.startsWith("CHAT_ROOM;")).findFirst().get())[5].equals("0"),"ended room read-only");
            check(closed.stream().anyMatch(l->l.startsWith("CHAT_MSG;")),"ended room preserves readable history");
            error(c.request("SEND_CHAT;"+ENDED+";"+Protocol.encode("late")),"closed send rejected");
            try (Connection db=CSDL.getConnection(); Statement s=db.createStatement()) {
                s.executeUpdate("UPDATE xuat_chieu SET bat_dau=DATE_SUB(NOW(),INTERVAL 3 MINUTE) WHERE id="+RUNNING);
                for (int i=0;i<55;i++) s.executeUpdate("INSERT INTO tin_nhan_chat(xuat_chieu_id,nguoi_dung_id,noi_dung) VALUES("+FUTURE+",900002,'Lịch sử "+i+"')");
            }
            server.tick();
            List<String> state=event(ea,"CHAT_STATE",2);
            check(decode(state.get(1))[5].equals("0"),"server publishes closure");
            Thread.sleep(550);
            List<String> rejected=a.request("SEND_CHAT;"+RUNNING+";"+Protocol.encode("too late"));
            check(rejected.get(0).contains("Suất chiếu đã kết thúc"),"server enforces end time");
            List<String> history=a.request("JOIN_CHAT;"+FUTURE);
            check(history.contains("MORE;1"),"history pagination marker");
            List<String> batch=new ArrayList<>(); for(String l:history) if(l.startsWith("CHAT_MSG;")) batch.add(l);
            check(batch.size()==50,"bounded history snapshot");
            success(a.request("GET_CHAT_HISTORY;"+FUTURE+";"+decode(batch.get(0))[0]),"CHAT_HISTORY");
            success(b.request("LOGOUT"),"LOGOUT");
            error(b.request("SEND_CHAT;"+FUTURE+";"+Protocol.encode("logged out")),"logout authorization");
            List<String> left=event(ea,"CHAT_MEMBERS",2);
            while(left.stream().filter(l->l.startsWith("CHAT_MEMBER;")).count()!=1) left=event(ea,"CHAT_MEMBERS",2);
            login(b,2); success(b.request("JOIN_CHAT;"+FUTURE),"CHAT");
            ea.clear(); eb.clear();
            ExecutorService parallel=Executors.newFixedThreadPool(2);
            try {
                CountDownLatch start=new CountDownLatch(1);
                Future<List<String>> one=parallel.submit(()->{start.await();return a.request("SEND_CHAT;"+FUTURE+";"+Protocol.encode("Đồng thời A"));});
                Future<List<String>> two=parallel.submit(()->{start.await();return b.request("SEND_CHAT;"+FUTURE+";"+Protocol.encode("Đồng thời B"));});
                start.countDown(); success(one.get(3,TimeUnit.SECONDS),"CHAT_SENT"); success(two.get(3,TimeUnit.SECONDS),"CHAT_SENT");
                Set<String> ids=new HashSet<>();
                ids.add(decode(event(ea,"CHAT_MESSAGE",2).get(1))[0]); ids.add(decode(event(ea,"CHAT_MESSAGE",2).get(1))[0]);
                check(ids.size()==2,"concurrent messages both delivered exactly once");
            } finally { parallel.shutdownNow(); }
            List<String> seats=a.request("WATCH_SHOW;"+FUTURE);
            String free=seats.stream().filter(l->l.startsWith("SEAT;")).map(ChatFeatureTest::decode)
                    .filter(row->row[1].equals("free")).findFirst().get()[0];
            List<String> order=a.request("CREATE_ORDER;"+FUTURE+";"+Protocol.encode(free)+";"+Protocol.encode(""));
            success(order,"PAYMENT");
            String booking=decode(order.get(1))[0];
            success(a.request("DEMO_CONFIRM_PAYMENT;"+booking),"PAID");
            success(a.request("GET_E_TICKET;"+booking),"E_TICKET");
            success(a.request("CANCEL_TICKET;"+booking),"CANCELLED");
            c.request("JOIN_CHAT;"+FUTURE); c.close();
            List<String> disconnected=event(ea,"CHAT_MEMBERS",2);
            while(disconnected.stream().filter(l->l.startsWith("CHAT_MEMBER;")).count()!=2) disconnected=event(ea,"CHAT_MEMBERS",2);
        }
        try (Harness restarted=new Harness(); MovieClient client=restarted.client()) {
            login(client,1);
            List<String> saved=client.request("JOIN_CHAT;"+FUTURE);
            check(saved.stream().anyMatch(l->l.startsWith("CHAT_MSG;")),"history survives server recreation");
            success(client.request("LEAVE_CHAT"),"CHAT_LEFT");
            error(client.request("SEND_CHAT;"+FUTURE+";"+Protocol.encode("left")),"leave enforced");
        }
        System.out.println("PASS: real MySQL + TCP, auth, rooms, concurrent push, typing, Unicode, membership, active screening, closure, pagination, logout, persistent history and booking/demo payment/cancellation alongside chat");
    }

    public static void prepare() throws Exception {
        String url=System.getenv("MOVIE_DB_URL");
        if (!"1".equals(System.getenv("MOVIE_TEST_ISOLATED")) || url==null || !url.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/galaxy_chat_test(?:\\?.*)?"))
            throw new IllegalStateException("Chỉ chạy trên MySQL riêng: MOVIE_TEST_ISOLATED=1 và URL localhost/galaxy_chat_test");
        if (!"demo".equals(config.AppConfig.get("PAYMENT_MODE","demo")))
            throw new IllegalStateException("Kiểm thử chỉ dùng PAYMENT_MODE=demo");
        String sql=Files.readString(Path.of("database/schema.sql")).replace("movie_tickets_ltm","galaxy_chat_test");
        sql=sql.replaceAll("(?m)^\\s*--.*$","");
        try(Connection c=CSDL.getConnection(); Statement s=c.createStatement()) {
            for(String part:sql.split(";")) if(!part.isBlank()) s.execute(part);
            s.executeUpdate("DELETE FROM tin_nhan_chat WHERE xuat_chieu_id BETWEEN 900001 AND 900004");
            for(int i=1;i<=3;i++) {
                try(PreparedStatement p=c.prepareStatement("INSERT INTO nguoi_dung(id,ho_ten,gmail,so_dien_thoai,mat_khau,vai_tro) VALUES(?,?,?,?,?,'user') ON DUPLICATE KEY UPDATE ho_ten=VALUES(ho_ten),mat_khau=VALUES(mat_khau)")) {
                    p.setInt(1,900000+i); p.setString(2,new String[]{"Kiên","Linh","Minh"}[i-1]);
                    p.setString(3,"chat"+i+"@example.com"); p.setString(4,"099990000"+i);
                    p.setString(5,utils.PasswordUtil.hashPassword(PASSWORD)); p.executeUpdate();
                }
            }
            s.executeUpdate("INSERT INTO phim(id,ten_phim,the_loai,thoi_luong) VALUES(900001,'Hành trình ánh sao','Phiêu lưu',1) ON DUPLICATE KEY UPDATE thoi_luong=1");
            s.executeUpdate("INSERT INTO phong_chieu(id,ten_phong) VALUES(900001,'Phòng chat demo') ON DUPLICATE KEY UPDATE id=id");
            for(char row='A';row<='F';row++) for(int col=1;col<=8;col++)
                s.executeUpdate("INSERT INTO ghe(phong_chieu_id,ma_ghe) VALUES(900001,'"+row+col+"') ON DUPLICATE KEY UPDATE id=id");
            String[] starts={"DATE_ADD(NOW(),INTERVAL 1 DAY)","DATE_SUB(NOW(),INTERVAL 5 SECOND)","DATE_SUB(NOW(),INTERVAL 2 MINUTE)","DATE_ADD(NOW(),INTERVAL 2 DAY)"};
            for(int i=0;i<4;i++) s.executeUpdate("INSERT INTO xuat_chieu(id,phim_id,phong_chieu_id,bat_dau,gia_ve) VALUES("+(900001+i)+",900001,900001,"+starts[i]+",75000) ON DUPLICATE KEY UPDATE bat_dau=VALUES(bat_dau)");
            s.executeUpdate("INSERT INTO tin_nhan_chat(xuat_chieu_id,nguoi_dung_id,noi_dung,ngay_gui) VALUES("+ENDED+",900002,'Cảm ơn mọi người, hẹn gặp ở rạp!',DATE_SUB(NOW(),INTERVAL 3 MINUTE))");
        }
    }

    public static void login(MovieClient client,int index) throws Exception {
        success(client.request("LOGIN;"+Protocol.encode("chat"+index+"@example.com")+";"+Protocol.encode(PASSWORD)),"LOGIN");
    }
    public static String[] decode(String row) {
        String[] p=row.split(";",-1); String[] result=new String[p.length-1];
        for(int i=1;i<p.length;i++) result[i-1]=Protocol.decode(p[i]); return result;
    }
    private static List<String> event(BlockingQueue<List<String>> q,String kind,int seconds) throws Exception {
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(seconds);
        while(System.nanoTime()<until) {
            List<String> frame=q.poll(Math.max(1,until-System.nanoTime()),TimeUnit.NANOSECONDS);
            if(frame!=null && frame.get(0).startsWith("EVENT;"+kind+";")) return frame;
        }
        throw new AssertionError("Missing event "+kind);
    }
    private static void success(List<String> r,String kind) { check(!r.isEmpty()&&(r.get(0).equals("OK;"+kind)||r.get(0).startsWith("OK;"+kind+";")),kind+": "+r); }
    private static void error(List<String> r,String label) { check(!r.isEmpty()&&r.get(0).startsWith("ERR;"),label); }
    private static void check(boolean b,String label) { if(!b) throw new AssertionError(label); }

    public static final class Harness implements AutoCloseable {
        private final ServerSocket listener=new ServerSocket(0,50,InetAddress.getLoopbackAddress());
        private final Set<Socket> sockets=ConcurrentHashMap.newKeySet();
        private final ExecutorService pool=Executors.newCachedThreadPool(r->{Thread t=new Thread(r,"chat-test");t.setDaemon(true);return t;});
        private final PasswordResetService resets=new PasswordResetService();
        private final BasicServer server=new BasicServer(resets);
        private final Method handler=BasicServer.class.getDeclaredMethod("handleClient",Socket.class);
        public Harness() throws Exception {
            handler.setAccessible(true);
            pool.execute(()->{
                while(!listener.isClosed()) try {
                    Socket socket=listener.accept(); sockets.add(socket);
                    pool.execute(()->{try{handler.invoke(server,socket);}catch(Exception ex){throw new RuntimeException(ex);}finally{sockets.remove(socket);}});
                } catch(IOException ex) { if(!listener.isClosed()) throw new RuntimeException(ex); }
            });
        }
        public MovieClient client() throws Exception { return new MovieClient("127.0.0.1",listener.getLocalPort(),5000); }
        public void tick() throws Exception { Method m=BasicServer.class.getDeclaredMethod("refreshChatRooms");m.setAccessible(true);m.invoke(server); }
        public void close() throws Exception {
            listener.close(); for(Socket socket:sockets) socket.close(); pool.shutdownNow(); resets.close();
        }
    }
}
