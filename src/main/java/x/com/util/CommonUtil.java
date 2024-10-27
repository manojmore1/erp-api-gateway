package x.com.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

@Component
public class CommonUtil {
    // Generate a unique small token using Base64-encoded UUID
    public static String generateSmallToken() {
        UUID uuid = UUID.randomUUID();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(uuid.toString().getBytes(StandardCharsets.UTF_8))
                .substring(0, 12); // Truncate to a short length if needed
    }

    // Optionally, generate a SHA-256 hash if more uniqueness is required
    public static String generateHash(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 12);
    }
}
