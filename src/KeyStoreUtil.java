import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Base64;

public class KeyStoreUtil {
    private static final String KEY_FOLDER = "ghostwire-keys";

    public static KeyPair loadOrCreate(String username)
            throws IOException, GeneralSecurityException {

        File folder = new File(KEY_FOLDER);

        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("Could not create key folder.");
        }

        String safeFileName = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        username.toLowerCase()
                                .getBytes(StandardCharsets.UTF_8)
                );

        File keyFile = new File(folder, safeFileName + ".key");

        if (keyFile.exists()) {
            try (BufferedReader reader = new BufferedReader(
                    new FileReader(keyFile))) {

                String publicKeyText = reader.readLine();
                String privateKeyText = reader.readLine();

                if (publicKeyText == null || privateKeyText == null) {
                    throw new IOException("Saved key is incomplete.");
                }

                return new KeyPair(
                        CryptoUtil.textToPublicKey(publicKeyText),
                        CryptoUtil.textToPrivateKey(privateKeyText)
                );
            }
        }

        KeyPair keyPair = CryptoUtil.generateKeyPair();

        try (PrintWriter writer = new PrintWriter(
                new FileWriter(keyFile))) {

            writer.println(CryptoUtil.keyToText(keyPair.getPublic()));
            writer.println(CryptoUtil.keyToText(keyPair.getPrivate()));
        }

        return keyPair;
    }
}