package payment;

import config.AppConfig;

public final class PaymentProviders {
    private PaymentProviders() { }
    public static PaymentProvider create() {
        String mode=AppConfig.get("PAYMENT_MODE","demo").toLowerCase(java.util.Locale.ROOT);
        if ("demo".equals(mode)) return new DemoPaymentProvider();
        if ("payos".equals(mode)) return new PayOsPaymentProvider();
        throw new IllegalStateException("PAYMENT_MODE chỉ nhận demo hoặc payos");
    }
}
