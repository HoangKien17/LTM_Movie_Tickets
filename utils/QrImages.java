package utils;

import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.image.BufferedImage;
import java.util.*;

public final class QrImages {
    private QrImages(){ }
    public static BufferedImage encode(String text,int size) {
        if(text==null||text.isEmpty()||text.length()>3000)throw new IllegalArgumentException("Nội dung QR không hợp lệ");
        try {
            Map<EncodeHintType,Object> hints=new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET,"UTF-8");hints.put(EncodeHintType.MARGIN,3);
            hints.put(EncodeHintType.ERROR_CORRECTION,ErrorCorrectionLevel.M);
            BitMatrix matrix=new QRCodeWriter().encode(text,BarcodeFormat.QR_CODE,size,size,hints);
            BufferedImage image=new BufferedImage(size,size,BufferedImage.TYPE_INT_RGB);
            for(int y=0;y<size;y++)for(int x=0;x<size;x++)image.setRGB(x,y,matrix.get(x,y)?0xFF000000:0xFFFFFFFF);
            return image;
        }catch(WriterException ex){throw new IllegalArgumentException("Không tạo được QR",ex);}
    }
}
