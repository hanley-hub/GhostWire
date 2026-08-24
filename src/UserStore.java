import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

public class UserStore {
    private static final String FILE_NAME = "ghostwire-users.txt";

    public static HashMap<String, User> loadUsers() {
        HashMap<String, User> users = new HashMap<>();
        File file = new File(FILE_NAME);

        if (!file.exists()) {
            return users;
        }

        try (BufferedReader reader = new BufferedReader(
                new FileReader(file))) {

            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", 4);

                if (parts.length >= 3) {
                    String username = parts[0];
                    String salt = parts[1];
                    String passwordHash = parts[2];

                    String publicKey = null;

                    if (parts.length == 4 && !parts[3].isEmpty()) {
                        publicKey = parts[3];
                    }

                    User user = new User(username, passwordHash, salt);
                    user.setPublicKey(publicKey);

                    users.put(username.toLowerCase(), user);
                }
            }

        } catch (IOException e) {
            System.out.println(
                    "Could not load accounts: " + e.getMessage());
        }

        return users;
    }

    public static void saveUsers(HashMap<String, User> users) {
        try (PrintWriter writer = new PrintWriter(
                new FileWriter(FILE_NAME))) {

            for (User user : users.values()) {
                writer.println(
                        user.getUsername() + "|" +
                                user.getSalt() + "|" +
                                user.getPasswordHash() + "|" +
                                (user.getPublicKey() == null ? "" : user.getPublicKey()));
            }

        } catch (IOException e) {
            System.out.println(
                    "Could not save accounts: " + e.getMessage());
        }
    }
}