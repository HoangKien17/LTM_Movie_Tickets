package payment;

public final class DemoPaymentProvider implements PaymentProvider {
    public PaymentLink create(long orderCode,long amount,String description,long expiresAtEpoch) {
        String qr="GALAXY CINEMA - THANH TOAN DEMO\nMa don: "+orderCode+"\nSo tien: "+amount+" VND";
        return new PaymentLink("DEMO-"+orderCode,qr,"","PENDING");
    }
    public PaymentState status(long orderCode) { return new PaymentState("PENDING",""); }
    public String name() { return "demo"; }
}
