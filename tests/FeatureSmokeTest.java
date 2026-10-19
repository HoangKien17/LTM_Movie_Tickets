package tests;

import model.NguoiDung;
import mysql.CSDL;
import protocol.Protocol;
import protocol.TicketCode;
import service.BasicService;
import service.CinemaService;
import web.MovieClient;
import java.sql.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

/** Chỉ chạy với instance MySQL thử nghiệm riêng và MOVIE_TEST_ISOLATED=1. Tạo dữ liệu test thật. */
public final class FeatureSmokeTest {
    private static final String PASSWORD="test-only-password-93827";
    private static final String HOST=System.getenv().getOrDefault("MOVIE_SERVER_HOST","127.0.0.1");
    private static final int PORT=Integer.parseInt(System.getenv().getOrDefault("MOVIE_SERVER_PORT","22040"));

    public static void main(String[] args) throws Exception {
        if (!"1".equals(System.getenv("MOVIE_TEST_ISOLATED"))) {
            throw new IllegalStateException("Chỉ chạy trên database thử nghiệm: đặt MOVIE_TEST_ISOLATED=1");
        }
        String unique=Long.toString(System.currentTimeMillis());
        BasicService basic=new BasicService();
        NguoiDung owner=account(basic,unique,0,"user");
        NguoiDung stranger=account(basic,unique,1,"user");
        NguoiDung staff1=account(basic,unique,2,"staff");
        NguoiDung staff2=account(basic,unique,3,"staff");
        NguoiDung admin=account(basic,unique,4,"admin");
        CinemaService cinema=new CinemaService();
        int movie=cinema.addMovie("Feature test "+unique,"Test",90,"Test isolation");
        int room=cinema.addRoom("Feature "+unique,2,12);
        int show=cinema.addShow(movie,room,LocalDateTime.now().plusDays(2),new BigDecimal("75000"));
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try (MovieClient a=login(owner); MovieClient b=login(stranger);
             MovieClient s1=login(staff1); MovieClient s2=login(staff2); MovieClient manager=login(admin)) {
            error(b.request("ADMIN_MONITOR"),"user cannot monitor");
            error(s1.request("ADMIN_MONITOR"),"staff cannot monitor");
            error(s1.request("ADMIN_GET_USERS"),"staff cannot administer");
            error(a.request("CHECK_IN;"+Protocol.encode("a".repeat(64))),"user cannot check in");
            error(a.request("INSPECT_TICKET;"+Protocol.encode("a".repeat(64))),"user cannot inspect");
            List<String[]> seats=rows(a.request("GET_SEAT_MAP;"+show),"SEATS");
            check(seats.size()==24,"room seat count");
            for (int i=0;i<12;i++) check(seats.get(i)[0].equals("A"+(i+1)),"numeric seat order");
            long booking=book(a,show,"A1,A2");
            error(b.request("GET_E_TICKET;"+booking),"ticket ownership");
            String[] ticket=rows(a.request("GET_E_TICKET;"+booking),"E_TICKET").get(0);
            String token=ticket[7], payload=TicketCode.payload(token);
            check(token.matches("[0-9a-f]{64}"),"secure token shape");
            check(rows(a.request("GET_E_TICKET;"+booking),"E_TICKET").get(0)[7].equals(token),"stable token");
            check(rows(s1.request("INSPECT_TICKET;"+Protocol.encode(payload)),"E_TICKET").get(0)[4].equals("A1,A2"),"staff preview");
            CountDownLatch start=new CountDownLatch(1);
            Future<List<String>> one=pool.submit(() -> { start.await();return s1.request("CHECK_IN;"+Protocol.encode(payload)); });
            Future<List<String>> two=pool.submit(() -> { start.await();return s2.request("CHECK_IN;"+Protocol.encode(payload)); });
            start.countDown();
            List<String> result1=one.get(10,TimeUnit.SECONDS),result2=two.get(10,TimeUnit.SECONDS);
            check(ok(result1)!=ok(result2),"exactly one staff accepts same QR");
            error(s1.request("CHECK_IN;"+Protocol.encode(payload)),"repeat check-in denied");
            error(a.request("CANCEL_TICKET;"+booking),"used booking cannot cancel");
            error(manager.request("ADMIN_CANCEL_TICKET;"+booking),"admin cannot cancel used booking");
            try (Connection c=CSDL.getConnection(); PreparedStatement ps=c.prepareStatement(
                    "SELECT checked_in_at,checked_in_by FROM don_dat_ve WHERE id=?")) {
                ps.setLong(1,booking);
                try(ResultSet rs=ps.executeQuery()) {
                    check(rs.next() && rs.getTimestamp(1)!=null,"check-in timestamp persisted");
                    check(rs.getInt(2)==(ok(result1)?staff1.getId():staff2.getId()),"winning staff persisted");
                }
            }
            long cancelled=book(a,show,"B1");
            String canceledCode=code(a,cancelled);
            success(a.request("CANCEL_TICKET;"+cancelled),"cancel unused");
            error(s1.request("CHECK_IN;"+Protocol.encode(canceledCode)),"cancelled QR rejected");
            error(s1.request("CHECK_IN;"+Protocol.encode("a".repeat(64))),"unknown QR rejected");
            error(s1.request("CHECK_IN;"+Protocol.encode("invalid;payload")),"invalid QR rejected");
            for(int i=2;i<=5;i++) {
                long race=book(a,show,"B"+i);
                String raceCode=code(a,race);
                CountDownLatch gate=new CountDownLatch(1);
                Future<List<String>> check=pool.submit(() -> {gate.await();return s1.request("CHECK_IN;"+Protocol.encode(raceCode));});
                Future<List<String>> cancel=pool.submit(() -> {gate.await();return a.request("CANCEL_TICKET;"+race);});
                gate.countDown();
                List<String> cr=check.get(10,TimeUnit.SECONDS),xr=cancel.get(10,TimeUnit.SECONDS);
                check(ok(cr)!=ok(xr),"cancel/check-in atomic race");
            }
            long adminBooking=book(a,show,"A3");
            success(manager.request("CHECK_IN;"+Protocol.encode(code(a,adminBooking))),"admin can check in");

            // Chứng minh token được cấp cho đơn cũ (NULL) mà không đổi mã lần sau.
            long legacy=book(a,show,"A4");
            try(Connection c=CSDL.getConnection();PreparedStatement ps=c.prepareStatement(
                    "SELECT ticket_token FROM don_dat_ve WHERE id=?")) {
                ps.setLong(1,legacy);
                try(ResultSet rs=ps.executeQuery()) { check(rs.next() && rs.getString(1)==null,"lazy legacy token"); }
            }
            check(!code(a,legacy).isEmpty(),"legacy QR issued");

            a.request("SECRET_COMMAND_SENTINEL");
            List<String> monitoring=manager.request("ADMIN_MONITOR");
            check(monitoring.get(0).equals("OK;MONITOR"),"admin monitor access");
            String decoded=decodeAll(monitoring);
            check(!decoded.contains(PASSWORD) && !decoded.contains(Protocol.encode(PASSWORD))
                    && !decoded.contains(token) && !decoded.contains("SECRET_COMMAND_SENTINEL"),"no secrets in logs");
            check(decoded.contains("BOOK_TICKET") && decoded.contains("CHECK_IN")
                    && decoded.contains("CANCEL_TICKET") && decoded.contains("UNKNOWN"),"business logs exist");
            int before=countClients(monitoring);
            b.close();
            int after=before;
            for(int i=0;i<30 && after==before;i++) {
                Thread.sleep(30); after=countClients(manager.request("ADMIN_MONITOR"));
            }
            check(after==before-1,"live connection count decreases");
            System.out.println("PASS: permissions, numeric seats, ownership, stable QR, concurrent check-in, cancellation race, admin check-in, legacy tickets, safe monitor and disconnect metrics");
        } finally { pool.shutdownNow(); }
    }

    private static NguoiDung account(BasicService basic,String unique,int n,String role) throws Exception {
        String phone=unique.substring(unique.length()-8)+n;
        NguoiDung user=basic.register("Test "+role,unique+"-"+n+"@example.com",phone,PASSWORD);
        try(Connection c=CSDL.getConnection();PreparedStatement ps=c.prepareStatement(
                "UPDATE nguoi_dung SET vai_tro=? WHERE id=?")) {
            ps.setString(1,role);ps.setInt(2,user.getId());ps.executeUpdate();
        }
        return user;
    }
    private static MovieClient login(NguoiDung user) throws Exception {
        MovieClient c=new MovieClient(HOST,PORT,8000);
        success(c.request("LOGIN;"+Protocol.encode(user.getGmail())+";"+Protocol.encode(PASSWORD)),"login");
        return c;
    }
    private static long book(MovieClient c,int show,String seats) throws Exception {
        List<String> r=c.request("BOOK_TICKET;"+show+";"+Protocol.encode(seats));
        success(r,"book");return Long.parseLong(r.get(0).split(";")[2]);
    }
    private static String code(MovieClient c,long id) throws Exception {
        return TicketCode.payload(rows(c.request("GET_E_TICKET;"+id),"E_TICKET").get(0)[7]);
    }
    private static boolean ok(List<String> lines) {return !lines.isEmpty() && lines.get(0).startsWith("OK;");}
    private static void success(List<String> r,String m) {check(ok(r),m+": "+r.get(0));}
    private static void error(List<String> r,String m) {check(!r.isEmpty()&&r.get(0).startsWith("ERR;"),m);}
    private static List<String[]> rows(List<String> r,String kind) {
        check(r.get(0).startsWith("OK;"+kind+";"),"response kind "+kind+": "+r.get(0));
        List<String[]> result=new ArrayList<>();
        for(int i=1;i<r.size();i++) {
            String[] p=r.get(i).split(";",-1),v=new String[p.length-1];
            for(int j=0;j<v.length;j++) v[j]=Protocol.decode(p[j+1]);
            result.add(v);
        }
        return result;
    }
    private static int countClients(List<String> r) {
        return (int)r.stream().filter(line -> line.startsWith("CLIENT;")).count();
    }
    private static String decodeAll(List<String> r) {
        StringBuilder text=new StringBuilder();
        for(int i=1;i<r.size();i++) {
            String[] fields=r.get(i).split(";",-1);
            for(int j=1;j<fields.length;j++) text.append(Protocol.decode(fields[j])).append('|');
        }
        return text.toString();
    }
    private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}
}
