package config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Đọc biến môi trường trước, sau đó mới dùng file .env ở thư mục chạy server. */
public final class AppConfig {
    private static Properties fileValues;
    private AppConfig() { }

    public static String get(String name, String fallback) {
        String value=System.getenv(name);
        if (value==null || value.trim().isEmpty()) value=values().getProperty(name);
        return value==null || value.trim().isEmpty()?fallback:value.trim();
    }

    private static synchronized Properties values() {
        if (fileValues!=null) return fileValues;
        Properties result=new Properties();
        Path path=Path.of(".env");
        if (Files.exists(path)) try {
            int lineNumber=0;
            for (String raw:Files.readAllLines(path,StandardCharsets.UTF_8)) {
                lineNumber++;
                if (lineNumber==1 && raw.startsWith("\uFEFF")) raw=raw.substring(1);
                String line=raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int separator=line.indexOf('=');
                if (separator<1) throw new IllegalStateException("Dòng "+lineNumber+" trong .env không hợp lệ");
                String key=line.substring(0,separator).trim();
                String value=line.substring(separator+1).trim();
                if (value.length()>=2 && ((value.startsWith("\"")&&value.endsWith("\""))
                        || (value.startsWith("'")&&value.endsWith("'")))) value=value.substring(1,value.length()-1);
                result.setProperty(key,value);
            }
        } catch (IOException ex) { throw new IllegalStateException("Không đọc được file .env",ex); }
        fileValues=result;
        return result;
    }
}
