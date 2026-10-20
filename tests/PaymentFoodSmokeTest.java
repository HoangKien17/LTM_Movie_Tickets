package tests;

import model.NguoiDung;
import mysql.CSDL;
import protocol.*;
import service.*;
import utils.*;
import web.MovieClient;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import java.nio.file.*;

/** Chỉ chạy với database thử nghiệm riêng và PAYMENT_MODE=demo. */
public final class PaymentFoodSmokeTest {
    public static void main(String[] args) throws Exception {
        if(!"1".equals(System.getenv("MOVIE_TEST_ISOLATED")))throw new IllegalStateException("Chỉ chạy khi MOVIE_TEST_ISOLATED=1");
        String mark=Long.toString(System.currentTimeMillis());
        BasicService basic=new BasicService();
        NguoiDung u1=basic.register("Payment One","pay1"+mark+"@test.local","091"+mark.substring(mark.length()-7),"Test1234");
        NguoiDung u2=basic.register("Payment Two","pay2"+mark+"@test.local","092"+mark.substring(mark.length()-7),"Test1234");
        CinemaService cinema=new CinemaService();
        int movie=cinema.addMovie("Payment movie "+mark,"Test",90,"Payment test");
        int room=cinema.addRoom("Payment room "+mark,1,3);
        int show=cinema.addShow(movie,room,LocalDateTime.now().plusDays(2),new BigDecimal("50000"));
        String host=System.getenv().getOrDefault("MOVIE_SERVER_HOST","127.0.0.1");
        int port=Integer.parseInt(System.getenv().getOrDefault("MOVIE_SERVER_PORT","2040"));
        try(MovieClient a=new MovieClient(host,port,8000);MovieClient b=new MovieClient(host,port,8000);
            MovieClient observer=new MovieClient(host,port,8000)) {
            login(a,u1.getGmail());login(b,u2.getGmail());login(observer,u2.getGmail());
            BlockingQueue<Integer> events=new LinkedBlockingQueue<>();observer.setSeatChangeListener(events::offer);
            success(observer.request("WATCH_SHOW;"+show),"SEATS");
            List<String[]> products=rows(a.request("GET_PRODUCTS"),"PRODUCTS");
            check(products.size()>=4,"products available");
            String productId=products.get(0)[0],productPrice=products.get(0)[3];
            List<String> created=a.request("CREATE_ORDER;"+show+";"+Protocol.encode("A1")+";"+Protocol.encode(productId+":2"));
            String[] payment=rows(created,"PAYMENT").get(0);long booking=Long.parseLong(payment[0]);
            long expected=50000L+new BigDecimal(productPrice).multiply(BigDecimal.valueOf(2)).longValueExact();
            check(new BigDecimal(payment[1]).longValueExact()==expected,"server-calculated exact total");
            check(!payment[3].isEmpty()&&"demo".equals(payment[6]),"payment QR and provider");
            check(events.poll(3,TimeUnit.SECONDS)==show,"realtime hold event");
            error(b.request("CREATE_ORDER;"+show+";"+Protocol.encode("A1")+";"+Protocol.encode("")),"held seat rejected");
            success(a.request("DEMO_CONFIRM_PAYMENT;"+booking),"PAID");
            check(events.poll(3,TimeUnit.SECONDS)==show,"realtime paid event");
            check("paid".equals(rows(a.request("GET_PAYMENT;"+booking),"PAYMENT").get(0)[2]),"payment persisted paid");
            String[] ticket=rows(a.request("GET_E_TICKET;"+booking),"E_TICKET").get(0);
            check(ticket.length==10&&ticket[8].contains("x2")&&new BigDecimal(ticket[9]).longValueExact()==expected,"ticket includes food and total");
            String readable="GALAXY CINEMA\nMã đơn: "+ticket[0]+"\nPhim: "+ticket[1]+"\nGhế: "+ticket[4]
                    +"\nMã xác thực: "+TicketCode.payload(ticket[7]);
            Path image=Files.createTempFile("galaxy-readable-ticket-",".png");
            ImageIO.write(QrImages.encode(readable,360),"png",image.toFile());
            check(QrTickets.decode(image.toFile()).equals(TicketCode.payload(ticket[7])),"phone-readable QR remains scannable by staff");
            Files.deleteIfExists(image);

            long expiring=Long.parseLong(rows(a.request("CREATE_ORDER;"+show+";"+Protocol.encode("A2")+";"+Protocol.encode("")),"PAYMENT").get(0)[0]);
            try(Connection c=CSDL.getConnection();PreparedStatement ps=c.prepareStatement("UPDATE don_dat_ve SET expires_at=DATE_SUB(NOW(),INTERVAL 1 SECOND) WHERE id=?")) {ps.setLong(1,expiring);ps.executeUpdate();}
            new PaymentService().synchronizePending();
            check("expired".equals(rows(a.request("GET_PAYMENT;"+expiring),"PAYMENT").get(0)[2]),"expired hold released");

            ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch gate=new CountDownLatch(1);
            Future<List<String>> one=pool.submit(()->{gate.await();return a.request("CREATE_ORDER;"+show+";"+Protocol.encode("A3")+";"+Protocol.encode(""));});
            Future<List<String>> two=pool.submit(()->{gate.await();return b.request("CREATE_ORDER;"+show+";"+Protocol.encode("A3")+";"+Protocol.encode(""));});
            gate.countDown();List<String> r1=one.get(),r2=two.get();pool.shutdownNow();
            check(ok(r1,"PAYMENT")!=ok(r2,"PAYMENT"),"concurrent hold has exactly one winner");
        }
        System.out.println("PASS: food totals, holds, exact payment QR, demo confirmation, realtime, expiry, readable ticket QR, concurrency");
    }
    private static void login(MovieClient client,String email)throws Exception{success(client.request("LOGIN;"+Protocol.encode(email)+";"+Protocol.encode("Test1234")),"LOGIN");}
    private static List<String[]> rows(List<String> response,String kind){success(response,kind);List<String[]> result=new ArrayList<>();for(int i=1;i<response.size();i++){String[] p=response.get(i).split(";",-1),row=new String[p.length-1];for(int j=0;j<row.length;j++)row[j]=Protocol.decode(p[j+1]);result.add(row);}return result;}
    private static boolean ok(List<String> r,String kind){return !r.isEmpty()&&r.get(0).startsWith("OK;"+kind);}
    private static void success(List<String> r,String kind){check(ok(r,kind),"expected OK "+kind+": "+r);}
    private static void error(List<String> r,String message){check(!r.isEmpty()&&r.get(0).startsWith("ERR;"),message+": "+r);}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
