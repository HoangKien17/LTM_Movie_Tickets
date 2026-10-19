package utils;

import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import protocol.TicketCode;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;

/** QR xử lý hoàn toàn cục bộ bằng ZXing; không gửi mã vé ra dịch vụ ngoài. */
public final class QrTickets {
    private QrTickets() { }

    public static BufferedImage encode(String payload) {
        String valid = TicketCode.payload(TicketCode.token(payload));
        try {
            Map<EncodeHintType,Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 4);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            BitMatrix matrix = new QRCodeWriter().encode(valid, BarcodeFormat.QR_CODE, 320, 320, hints);
            BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int y=0; y<matrix.getHeight(); y++) {
                for (int x=0; x<matrix.getWidth(); x++) image.setRGB(x,y,matrix.get(x,y)?0xFF000000:0xFFFFFFFF);
            }
            return image;
        } catch (WriterException ex) {
            throw new IllegalArgumentException("Không tạo được mã QR", ex);
        }
    }

    public static String decode(File file) throws IOException {
        if (!file.isFile() || Files.size(file.toPath()) > 10L*1024*1024) {
            throw new IOException("Ảnh QR tối đa 10 MB");
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(file)) {
            if (input == null) throw new IOException("Không đọc được ảnh");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Định dạng ảnh không hỗ trợ");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int w=reader.getWidth(0), h=reader.getHeight(0);
                if (w<1 || h<1 || (long)w*h>20_000_000) throw new IOException("Ảnh QR quá lớn");
                BufferedImage image = reader.read(0);
                int[] pixels = image.getRGB(0,0,w,h,null,0,w);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(w,h,pixels)));
                Map<DecodeHintType,Object> hints = new EnumMap<>(DecodeHintType.class);
                hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));
                hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
                String text = new MultiFormatReader().decode(bitmap,hints).getText();
                return TicketCode.payload(TicketCode.token(text));
            } finally { reader.dispose(); }
        } catch (NotFoundException | IllegalArgumentException ex) {
            throw new IOException("Ảnh không chứa mã vé Galaxy Cinema hợp lệ", ex);
        }
    }
}
