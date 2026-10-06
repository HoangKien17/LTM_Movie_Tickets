package service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** Lưu poster trên máy Server và tạo ảnh nhỏ để gửi cho các Client qua TCP. */
public final class PosterStorage {
    public static final int MAX_UPLOAD_BYTES = 2 * 1024 * 1024;
    private static final int WIDTH = 244;
    private static final int HEIGHT = 340;
    private static final int MAX_SOURCE_PIXELS = 12_000_000;

    private PosterStorage() {
    }

    public static String save(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return "";
        }
        if (base64.length() > 2_800_000) {
            throw new IllegalArgumentException("Ảnh poster tối đa 2 MB");
        }
        byte[] source;
        try {
            source = Base64.getUrlDecoder().decode(base64);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Dữ liệu ảnh poster không hợp lệ");
        }
        if (source.length == 0 || source.length > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("Ảnh poster tối đa 2 MB");
        }

        BufferedImage image = readImage(source);
        BufferedImage poster = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = poster.createGraphics();
        try {
            g.setColor(new Color(18, 27, 49));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            double scale = Math.max((double) WIDTH / image.getWidth(),
                    (double) HEIGHT / image.getHeight());
            int w = (int) Math.ceil(image.getWidth() * scale);
            int h = (int) Math.ceil(image.getHeight() * scale);
            g.drawImage(image, (WIDTH - w) / 2, (HEIGHT - h) / 2, w, h, null);
        } finally {
            g.dispose();
        }

        String filename = UUID.randomUUID() + ".jpg";
        Path destination = directory().resolve(filename);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(poster, "jpg", output)) {
                throw new IOException("JPEG writer không khả dụng");
            }
            Files.createDirectories(destination.getParent());
            Files.write(destination, output.toByteArray(), StandardOpenOption.CREATE_NEW);
            return filename;
        } catch (IOException ex) {
            try {
                Files.deleteIfExists(destination);
            } catch (IOException ignored) {
                // Lỗi lưu ban đầu quan trọng hơn lỗi dọn dẹp.
            }
            throw new IllegalStateException("Server không lưu được ảnh poster", ex);
        }
    }

    public static String readBase64(String filename) {
        if (!isManaged(filename)) {
            return "";
        }
        try {
            Path file = directory().resolve(filename);
            if (!Files.isRegularFile(file) || Files.size(file) > 300_000) {
                return "";
            }
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(Files.readAllBytes(file));
        } catch (IOException ex) {
            System.err.println("Không đọc được poster " + filename + ": " + ex.getMessage());
            return "";
        }
    }

    public static void delete(String filename) {
        if (!isManaged(filename)) {
            return;
        }
        try {
            Files.deleteIfExists(directory().resolve(filename));
        } catch (IOException ex) {
            System.err.println("Không xóa được poster cũ " + filename + ": " + ex.getMessage());
        }
    }

    private static BufferedImage readImage(byte[] bytes) {
        try (ImageInputStream input = ImageIO.createImageInputStream(
                new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw new IllegalArgumentException("Không đọc được ảnh poster");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Ảnh phải là JPG hoặc PNG");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!"jpeg".equals(format) && !"jpg".equals(format) && !"png".equals(format)) {
                    throw new IllegalArgumentException("Ảnh phải là JPG hoặc PNG");
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > MAX_SOURCE_PIXELS) {
                    throw new IllegalArgumentException("Kích thước ảnh poster không hợp lệ");
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new IllegalArgumentException("Không đọc được ảnh poster");
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Không đọc được ảnh poster");
        }
    }

    private static Path directory() {
        String configured = System.getenv("MOVIE_POSTER_DIR");
        if (configured != null && !configured.trim().isEmpty()) {
            return Path.of(configured.trim()).toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.home"), ".ltm-movie-tickets", "posters")
                .toAbsolutePath().normalize();
    }

    private static boolean isManaged(String filename) {
        return filename != null && filename.matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg");
    }
}
