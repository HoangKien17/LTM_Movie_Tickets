package mysql;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Cấu hình JDBC chỉ dùng ở phía Server.
 */
public final class CSDL {

    private static final String DEFAULT_URL
            = "jdbc:mysql://127.0.0.1:3306/movie_tickets_ltm";
    private static Properties localConfig;

    private CSDL() {
    }

    public static Connection getConnection() throws SQLException {
        Properties config = localConfig();
        String url = envOrDefault("MOVIE_DB_URL", DEFAULT_URL, config);
        String user = envOrDefault("MOVIE_DB_USER", "root", config);
        String password = System.getenv("MOVIE_DB_PASSWORD");
        if (password == null) {
            password = config.getProperty("MOVIE_DB_PASSWORD", "");
        }
        return DriverManager.getConnection(url, user,
                password);
    }

    private static String envOrDefault(String name, String fallback, Properties config) {
        String value = System.getenv(name);
        if (value == null) {
            value = config.getProperty(name);
        }
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static synchronized Properties localConfig() throws SQLException {
        if (localConfig != null) {
            return localConfig;
        }
        Properties values = new Properties();
        Path path = Path.of(".env");
        if (Files.exists(path)) {
            try {
                int lineNumber = 0;
                for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    lineNumber++;
                    if (lineNumber == 1 && rawLine.startsWith("\uFEFF")) {
                        rawLine = rawLine.substring(1);
                    }
                    String line = rawLine.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int separator = line.indexOf('=');
                    if (separator < 1) {
                        throw new SQLException("Dòng " + lineNumber + " trong .env không hợp lệ");
                    }
                    String key = line.substring(0, separator).trim();
                    if (key.isEmpty()) {
                        throw new SQLException("Dòng " + lineNumber + " trong .env không hợp lệ");
                    }
                    String value = line.substring(separator + 1).trim();
                    if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'")))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    values.setProperty(key, value);
                }
            } catch (IOException ex) {
                throw new SQLException("Không đọc được file .env ở thư mục chạy Server", ex);
            }
        }
        localConfig = values;
        return values;
    }
}
