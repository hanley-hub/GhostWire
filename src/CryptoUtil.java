import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Key;
import java.util.Base64;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtil {
    public static KeyPair generateKeyPair()
            throws GeneralSecurityException {

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");

        generator.initialize(2048);

        return generator.generateKeyPair();
    }

    public static String keyToText(Key key) {
        return Base64.getEncoder().encodeToString(
                key.getEncoded());
    }

    public static PublicKey textToPublicKey(String keyText)
            throws GeneralSecurityException {

        byte[] keyBytes = Base64.getDecoder().decode(keyText);

        return KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(keyBytes));
    }

    public static PrivateKey textToPrivateKey(String keyText)
            throws GeneralSecurityException {

        byte[] keyBytes = Base64.getDecoder().decode(keyText);

        return KeyFactory.getInstance("RSA").generatePrivate(
                new PKCS8EncodedKeySpec(keyBytes));
    }

    public static SecretKey generateMessageKey()
            throws GeneralSecurityException {

        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);

        return generator.generateKey();
    }

    public static String encryptText(String text, SecretKey messageKey)
            throws GeneralSecurityException {

        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.ENCRYPT_MODE,
                messageKey,
                new GCMParameterSpec(128, iv));

        byte[] encryptedText = cipher.doFinal(
                text.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[iv.length + encryptedText.length];

        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(
                encryptedText,
                0,
                combined,
                iv.length,
                encryptedText.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    public static String encryptMessageKey(
            SecretKey messageKey,
            PublicKey recipientPublicKey)
            throws GeneralSecurityException {

        Cipher cipher = Cipher.getInstance(
                "RSA/ECB/OAEPWithSHA-256AndMGF1Padding");

        cipher.init(Cipher.ENCRYPT_MODE, recipientPublicKey);

        return Base64.getEncoder().encodeToString(
                cipher.doFinal(messageKey.getEncoded()));
    }

    public static SecretKey decryptMessageKey(
            String encryptedMessageKey,
            PrivateKey privateKey)
            throws GeneralSecurityException {

        Cipher cipher = Cipher.getInstance(
                "RSA/ECB/OAEPWithSHA-256AndMGF1Padding");

        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] messageKeyBytes = cipher.doFinal(
                Base64.getDecoder().decode(encryptedMessageKey));

        return new SecretKeySpec(messageKeyBytes, "AES");
    }

    public static String decryptText(
            String encryptedText,
            SecretKey messageKey)
            throws GeneralSecurityException {

        byte[] combined = Base64.getDecoder().decode(encryptedText);

        byte[] iv = new byte[12];
        byte[] encryptedBytes = new byte[combined.length - iv.length];

        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(
                combined,
                iv.length,
                encryptedBytes,
                0,
                encryptedBytes.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.DECRYPT_MODE,
                messageKey,
                new GCMParameterSpec(128, iv));

        return new String(
                cipher.doFinal(encryptedBytes),
                StandardCharsets.UTF_8);
    }

    public static String encryptBytes(
            byte[] fileBytes,
            SecretKey messageKey)
            throws GeneralSecurityException {

        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.ENCRYPT_MODE,
                messageKey,
                new GCMParameterSpec(128, iv));

        byte[] encryptedBytes = cipher.doFinal(fileBytes);

        byte[] combined = new byte[iv.length + encryptedBytes.length];

        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(
                encryptedBytes,
                0,
                combined,
                iv.length,
                encryptedBytes.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    public static byte[] decryptBytes(
            String encryptedFile,
            SecretKey messageKey)
            throws GeneralSecurityException {

        byte[] combined = Base64.getDecoder().decode(encryptedFile);

        byte[] iv = new byte[12];
        byte[] encryptedBytes = new byte[combined.length - iv.length];

        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(
                combined,
                iv.length,
                encryptedBytes,
                0,
                encryptedBytes.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.DECRYPT_MODE,
                messageKey,
                new GCMParameterSpec(128, iv));

        return cipher.doFinal(encryptedBytes);
    }
}