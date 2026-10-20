package tests;

import com.google.gson.JsonObject;
import java.lang.reflect.Method;
import payment.PayOsPaymentProvider;

/** Kiểm tra thuật toán chữ ký bằng dữ liệu mẫu công khai trong tài liệu payOS. */
public final class PayOsSignatureTest {
    public static void main(String[] args) throws Exception {
        JsonObject data=new JsonObject();
        data.addProperty("orderCode",123);data.addProperty("amount",3000);
        data.addProperty("description","VQRIO123");data.addProperty("accountNumber","12345678");
        data.addProperty("reference","TF230204212323");
        data.addProperty("transactionDateTime","2023-02-04 18:25:00");
        data.addProperty("currency","VND");
        data.addProperty("paymentLinkId","124c33293c43417ab7879e14c8d9eb18");
        data.addProperty("code","00");data.addProperty("desc","Thành công");
        data.addProperty("counterAccountBankId","");data.addProperty("counterAccountBankName","");
        data.addProperty("counterAccountName","");data.addProperty("counterAccountNumber","");
        data.addProperty("virtualAccountName","");data.addProperty("virtualAccountNumber","");
        Method canonical=PayOsPaymentProvider.class.getDeclaredMethod("canonical",JsonObject.class);
        canonical.setAccessible(true);
        Method hmac=PayOsPaymentProvider.class.getDeclaredMethod("hmac",String.class,String.class);
        hmac.setAccessible(true);
        String text=(String)canonical.invoke(null,data);
        String signature=(String)hmac.invoke(null,text,
                "1a54716c8f0efb2744fb28b6e38b25da7f67a925d98bc1c18bd8faaecadd7675");
        if(!"412e915d2871504ed31be63c8f62a149a4410d34c4c42affc9006ef9917eaa03".equals(signature))
            throw new AssertionError("Chữ ký payOS không khớp dữ liệu mẫu: "+signature);
        System.out.println("PASS: payOS HMAC signature matches official sample");
    }
}
