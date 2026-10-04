package protocol;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/** Mã hóa từng tham số để dấu chấm phẩy và xuống dòng không làm hỏng giao thức. */
public final class Protocol {
    private Protocol() {
    }

    public static String encode(String value) {
        Objects.requireNonNull(value, "value");
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(String value) {
        Objects.requireNonNull(value, "value");
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
