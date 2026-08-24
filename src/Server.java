import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Server {
    static CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    static HashMap<String, User> registeredUsers = UserStore.loadUsers();

    static boolean registerUser(String username, String password) {
        String accountKey = username.toLowerCase();

        if (registeredUsers.containsKey(accountKey)) {
            return false;
        }

        String salt = PasswordUtil.generateSalt();
        String passwordHash = PasswordUtil.hashPassword(password, salt);

        User user = new User(username, passwordHash, salt);
        registeredUsers.put(accountKey, user);
        UserStore.saveUsers(registeredUsers);

        return true;
    }

    static User authenticateUser(String username, String password) {
        User user = registeredUsers.get(username.toLowerCase());

        if (user == null) {
            return null;
        }

        if (PasswordUtil.passwordsMatch(
                password,
                user.getSalt(),
                user.getPasswordHash())) {
            return user;
        }

        return null;
    }

    static boolean savePublicKey(String username, String publicKey) {
        User user = registeredUsers.get(username.toLowerCase());

        if (user == null) {
            return false;
        }

        user.setPublicKey(publicKey);
        UserStore.saveUsers(registeredUsers);

        return true;
    }

    static String getPublicKey(String username) {
        User user = registeredUsers.get(username.toLowerCase());

        if (user == null) {
            return null;
        }

        return user.getPublicKey();
    }

    public static void main(String[] args) {
        int port = 5000;
        try {
            ServerSocket serverSocket = new ServerSocket(port);

            System.out.println("GhostWire Server started. ");
            System.out.println("waiting for a client...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected!");
                ClientHandler clienthandler = new ClientHandler(clientSocket);
                clients.add(clienthandler);
                Thread thread = new Thread(clienthandler);
                thread.start();
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}
