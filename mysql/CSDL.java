package mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Cấu hình JDBC chỉ dùng ở phía Server. */
public final class CSDL {
    private static final String DEFAULT_URL =
            "jdbc:mysql://127.0.0.1:3306/movie_tickets_ltm";

    private CSDL() {
    }

    public static Connection getConnection() throws SQLException {
        String url = envOrDefault("MOVIE_DB_URL", DEFAULT_URL);
        String user = envOrDefault("MOVIE_DB_USER", "root");
        String password = System.getenv("MOVIE_DB_PASSWORD");
        return DriverManager.getConnection(url, user,
                password == null ? "" : password);
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
