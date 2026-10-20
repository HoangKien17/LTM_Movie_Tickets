package payment;

public interface PaymentProvider {
    PaymentLink create(long orderCode,long amount,String description,long expiresAtEpoch) throws Exception;
    PaymentState status(long orderCode) throws Exception;
    String name();

    final class PaymentLink {
        public final String providerId,qrPayload,checkoutUrl,status;
        public PaymentLink(String providerId,String qrPayload,String checkoutUrl,String status) {
            this.providerId=providerId; this.qrPayload=qrPayload; this.checkoutUrl=checkoutUrl; this.status=status;
        }
    }
    final class PaymentState {
        public final String status,reference;
        public PaymentState(String status,String reference) { this.status=status; this.reference=reference; }
    }
}
