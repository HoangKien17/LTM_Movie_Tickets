package payment;

import com.google.gson.*;
import config.AppConfig;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Tích hợp trực tiếp API payOS; khóa chỉ tồn tại ở server. */
public final class PayOsPaymentProvider implements PaymentProvider {
    private static final String API="https://api-merchant.payos.vn";
    private static final Gson GSON=new Gson();
    private final String clientId=required("PAYOS_CLIENT_ID");
    private final String apiKey=required("PAYOS_API_KEY");
    private final String checksumKey=required("PAYOS_CHECKSUM_KEY");
    private final String returnUrl=AppConfig.get("PAYOS_RETURN_URL","https://payos.vn");
    private final String cancelUrl=AppConfig.get("PAYOS_CANCEL_URL","https://payos.vn");
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public PaymentLink create(long orderCode,long amount,String description,long expiresAtEpoch) throws Exception {
        String safe=description.length()>9?description.substring(0,9):description;
        JsonObject body=new JsonObject();
        body.addProperty("orderCode",orderCode); body.addProperty("amount",amount);
        body.addProperty("description",safe); body.addProperty("cancelUrl",cancelUrl);
        body.addProperty("returnUrl",returnUrl); body.addProperty("expiredAt",expiresAtEpoch);
        String data="amount="+amount+"&cancelUrl="+cancelUrl+"&description="+safe
                +"&orderCode="+orderCode+"&returnUrl="+returnUrl;
        body.addProperty("signature",hmac(data));
        JsonObject response=call(HttpRequest.newBuilder(URI.create(API+"/v2/payment-requests"))
                .timeout(Duration.ofSeconds(20)).header("Content-Type","application/json")
                .header("x-client-id",clientId).header("x-api-key",apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body),StandardCharsets.UTF_8)).build());
        JsonObject value=verifiedData(response);
        return new PaymentLink(text(value,"paymentLinkId"),text(value,"qrCode"),
                text(value,"checkoutUrl"),text(value,"status"));
    }

    public PaymentState status(long orderCode) throws Exception {
        String id=URLEncoder.encode(Long.toString(orderCode),StandardCharsets.UTF_8.name());
        JsonObject response=call(HttpRequest.newBuilder(URI.create(API+"/v2/payment-requests/"+id))
                .timeout(Duration.ofSeconds(15)).header("x-client-id",clientId)
                .header("x-api-key",apiKey).GET().build());
        JsonObject data=verifiedData(response);
        String reference="";
        JsonElement transactions=data.get("transactions");
        if (transactions!=null && transactions.isJsonArray()
                && !transactions.getAsJsonArray().isEmpty()
                && transactions.getAsJsonArray().get(0).isJsonObject()) {
            reference=text(transactions.getAsJsonArray().get(0).getAsJsonObject(),"reference");
        } else if (transactions!=null && transactions.isJsonObject()) {
            JsonObject object=transactions.getAsJsonObject();
            if (object.has("reference")) reference=text(object,"reference");
            else if (!object.keySet().isEmpty()) {
                JsonElement first=object.get(object.keySet().iterator().next());
                if (first!=null && first.isJsonObject()) reference=text(first.getAsJsonObject(),"reference");
            }
        }
        if (reference.isEmpty()) reference=text(data,"paymentLinkId");
        return new PaymentState(text(data,"status"),reference);
    }
    public String name() { return "payos"; }

    private JsonObject call(HttpRequest request) throws Exception {
        HttpResponse<String> response=http.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode()<200 || response.statusCode()>=300)
            throw new IllegalStateException("payOS phản hồi HTTP "+response.statusCode());
        JsonObject json=JsonParser.parseString(response.body()).getAsJsonObject();
        if (!"00".equals(text(json,"code"))) throw new IllegalStateException("payOS: "+text(json,"desc"));
        return json;
    }

    private JsonObject verifiedData(JsonObject response) throws Exception {
        JsonObject data=response.getAsJsonObject("data");
        String signature=text(response,"signature");
        if (data==null || signature.isEmpty() || !constantTime(hmac(canonical(data)),signature))
            throw new SecurityException("Chữ ký phản hồi payOS không hợp lệ");
        return data;
    }

    private static String canonical(JsonObject data) {
        List<String> keys=new ArrayList<>(data.keySet()); Collections.sort(keys);
        List<String> pairs=new ArrayList<>();
        for (String key:keys) pairs.add(key+"="+value(data.get(key)));
        return String.join("&",pairs);
    }
    private static String value(JsonElement value) {
        if (value==null || value.isJsonNull()) return "";
        if (value.isJsonArray()) {
            JsonArray sorted=new JsonArray();
            for (JsonElement item:value.getAsJsonArray()) sorted.add(item.isJsonObject()?sortObject(item.getAsJsonObject()):item);
            return GSON.toJson(sorted);
        }
        if (value.isJsonObject()) return GSON.toJson(sortObject(value.getAsJsonObject()));
        return value.getAsString();
    }
    private static JsonObject sortObject(JsonObject object) {
        JsonObject sorted=new JsonObject();
        List<String> keys=new ArrayList<>(object.keySet()); Collections.sort(keys);
        for(String key:keys) sorted.add(key,object.get(key));
        return sorted;
    }
    private String hmac(String data) throws Exception {
        return hmac(data,checksumKey);
    }
    private static String hmac(String data,String key) throws Exception {
        Mac mac=Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        StringBuilder result=new StringBuilder();
        for(byte b:mac.doFinal(data.getBytes(StandardCharsets.UTF_8))) result.append(String.format(Locale.ROOT,"%02x",b&255));
        return result.toString();
    }
    private static boolean constantTime(String a,String b) {
        return java.security.MessageDigest.isEqual(a.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII),
                b.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }
    private static String text(JsonObject object,String key) {
        JsonElement value=object==null?null:object.get(key);
        return value==null||value.isJsonNull()?"":value.getAsString();
    }
    private static String required(String name) {
        String value=AppConfig.get(name,"");
        if(value.isEmpty()) throw new IllegalStateException("Thiếu cấu hình "+name);
        return value;
    }
}
