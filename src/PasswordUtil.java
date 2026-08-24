import java.security.SecureRandom;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordUtil {
    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateSalt() {
        byte[] saltBytes = new byte[SALT_LENGTH];
        RANDOM.nextBytes(saltBytes);

        return Base64.getEncoder().encodeToString(saltBytes);
    }

    public static String hashPassword(String password, String salt) {
        byte[] saltBytes = Base64.getDecoder().decode(salt);

        PBEKeySpec specification = new PBEKeySpec(
                password.toCharArray(),
                saltBytes,
                ITERATIONS,
                KEY_LENGTH);

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            byte[] hashBytes = factory.generateSecret(specification).getEncoded();

            return Base64.getEncoder().encodeToString(hashBytes);

        } catch (Exception e) {
            throw new RuntimeException("Could not hash password.", e);
        } finally {
            specification.clearPassword();
        }
    }

    public static boolean passwordsMatch(
            String password,
            String salt,
            String expectedHash) {
        String enteredHash = hashPassword(password, salt);

        return MessageDigest.isEqual(
                enteredHash.getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8));
    }
}