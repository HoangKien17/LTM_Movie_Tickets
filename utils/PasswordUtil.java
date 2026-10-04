package utils;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {
    
    /**
     * Hash password using SHA-256 + salt
     */
    public static String hashPassword(String password) {
        try {
            // Tạo salt ngẫu nhiên
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            
            // Hash password với salt
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());
            
            // Kết hợp salt + hash
            byte[] saltAndHash = new byte[salt.length + hashedPassword.length];
            System.arraycopy(salt, 0, saltAndHash, 0, salt.length);
            System.arraycopy(hashedPassword, 0, saltAndHash, salt.length, hashedPassword.length);
            
            // Encode to Base64 để lưu trong DB
            return Base64.getEncoder().encodeToString(saltAndHash);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Verify password by comparing with hashed password
     */
    public static boolean verifyPassword(String password, String hashedPassword) {
        try {
            // Decode Base64
            byte[] saltAndHash = Base64.getDecoder().decode(hashedPassword);
            
            // Tách salt và hash
            byte[] salt = new byte[16];
            System.arraycopy(saltAndHash, 0, salt, 0, 16);
            
            // Hash lại password với salt đó
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedInput = md.digest(password.getBytes());
            
            // So sánh hash mới với hash cũ
            byte[] storedHash = new byte[saltAndHash.length - 16];
            System.arraycopy(saltAndHash, 16, storedHash, 0, storedHash.length);
            
            // So sánh byte array
            return MessageDigest.isEqual(hashedInput, storedHash);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Simple email validation
     */
    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }
    
    /**
     * Simple phone validation
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("\\d{9,11}");
    }
}
