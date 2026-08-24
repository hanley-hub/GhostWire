import java.io.BufferedReader;
import java.net.Socket;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter output;
    private String username;
    private static final int MAX_FILE_DATA_LENGTH = 7_000_000;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    private void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : Server.clients) {
            if (client != sender) {
                client.output.println(message);
            }
        }
    }

    private boolean sendDirectMessage(String recipient, String message) {
        for (ClientHandler client : Server.clients) {
            if (client.username != null &&
                    client.username.equalsIgnoreCase(recipient)) {

                client.output.println(username + ": " + message);
                return true;
            }
        }

        output.println(recipient + " is not online.");
        return false;
    }

    private boolean sendEncryptedDirectMessage(
            String recipient,
            String encryptedRecipientKey,
            String encryptedText) {

        for (ClientHandler client : Server.clients) {
            if (client.username != null &&
                    client.username.equalsIgnoreCase(recipient)) {

                client.output.println(
                        "ENCRYPTED|" + username + "|" +
                                encryptedRecipientKey + "|" +
                                encryptedText);

                return true;
            }
        }

        output.println(recipient + " is not online.");
        return false;
    }

    private boolean sendEncryptedFile(
            String recipient,
            String encodedFileName,
            String encryptedRecipientKey,
            String encryptedFile) {

        for (ClientHandler client : Server.clients) {
            if (client.username != null &&
                    client.username.equalsIgnoreCase(recipient)) {

                client.output.println(
                        "FILE|" + username + "|" +
                                encodedFileName + "|" +
                                encryptedRecipientKey + "|" +
                                encryptedFile);

                return true;
            }
        }

        output.println(recipient + " is not online.");
        return false;
    }

    private void sendConversationHistory(String otherUser) {
        for (ChatMessage savedMessage : MessageStore.loadConversation(username, otherUser)) {

            String direction;

            if (savedMessage.getSender().equalsIgnoreCase(username)) {
                direction = "OUT";
            } else {
                direction = "IN";
            }

            output.println(
                    "HISTORY|" + otherUser + "|" + direction + "|" +
                            savedMessage.getContent());
        }

        output.println("HISTORY_END");
    }

    private void sendOnlineUsers() {
        StringBuilder users = new StringBuilder();
        boolean firstUser = true;

        for (ClientHandler client : Server.clients) {
            if (client != this && client.username != null) {
                if (!firstUser) {
                    users.append(", ");
                }

                users.append(client.username);
                firstUser = false;
            }
        }

        if (firstUser) {
            output.println("No other users are online.");
        } else {
            output.println("Online users: " + users);
        }
    }

    private boolean usernameIsTaken(String requestedUsername) {
        for (ClientHandler client : Server.clients) {
            if (client != this &&
                    client.username != null &&
                    client.username.equalsIgnoreCase(requestedUsername)) {

                return true;
            }
        }

        return false;
    }

    private boolean authenticateConnection(BufferedReader input)
            throws IOException {

        String request = input.readLine();

        if (request == null) {
            return false;
        }

        String[] parts = request.split("\\|", 3);

        if (parts.length != 3) {
            output.println("Invalid authentication request.");
            return false;
        }

        String action = parts[0];
        String requestedUsername = parts[1].trim();
        String password = parts[2];

        if (requestedUsername.isEmpty() || password.isEmpty()) {
            output.println("Username and password are required.");
            return false;
        }

        if (action.equalsIgnoreCase("REGISTER")) {
            if (!Server.registerUser(requestedUsername, password)) {
                output.println("Registration failed: username already exists.");
                return false;
            }

            this.username = requestedUsername;
            output.println("AUTHENTICATION_ACCEPTED");
            return true;
        }

        if (action.equalsIgnoreCase("LOGIN")) {
            User user = Server.authenticateUser(
                    requestedUsername,
                    password);

            if (user == null) {
                output.println("Login failed: username or password is incorrect.");
                return false;
            }

            if (usernameIsTaken(user.getUsername())) {
                output.println("This user is already logged in.");
                return false;
            }

            this.username = user.getUsername();
            output.println("AUTHENTICATION_ACCEPTED");
            return true;
        }

        output.println("Choose REGISTER or LOGIN.");
        return false;
    }

    @Override
    public void run() {
        try {
            BufferedReader input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            this.output = new PrintWriter(
                    socket.getOutputStream(), true);

            if (!authenticateConnection(input)) {
                return;
            }
            broadcast(username + " has joined the chat.", this);

            while (true) {
                String message = input.readLine();

                if (message == null || message.equalsIgnoreCase("exit")) {
                    break;
                }
                if (message.startsWith("FILE|")) {
                    String[] fileParts = message.split("\\|", 5);

                    if (fileParts.length != 5) {
                        output.println("Could not send file.");
                        continue;
                    }

                    String recipient = fileParts[1];
                    String encodedFileName = fileParts[2];
                    String encryptedRecipientKey = fileParts[3];
                    String encryptedFile = fileParts[4];

                    if (encryptedFile.length() > MAX_FILE_DATA_LENGTH) {
                        output.println("File is too large. Maximum size is 5 MB.");
                        continue;
                    }

                    sendEncryptedFile(
                            recipient,
                            encodedFileName,
                            encryptedRecipientKey,
                            encryptedFile);

                    continue;
                }

                if (message.startsWith("ENCRYPTED|")) {
                    String[] encryptedParts = message.split("\\|", 5);

                    if (encryptedParts.length != 5) {
                        output.println("Could not send encrypted message.");
                        continue;
                    }

                    String recipient = encryptedParts[1];
                    String encryptedRecipientKey = encryptedParts[2];
                    String encryptedSenderKey = encryptedParts[3];
                    String encryptedText = encryptedParts[4];

                    if (sendEncryptedDirectMessage(
                            recipient,
                            encryptedRecipientKey,
                            encryptedText)) {

                        MessageStore.saveMessage(
                                new ChatMessage(
                                        username,
                                        recipient,
                                        "ENCRYPTED|" +
                                                encryptedRecipientKey + "|" +
                                                encryptedSenderKey + "|" +
                                                encryptedText));
                    }

                    continue;
                }
                if (message.startsWith("HISTORY_REQUEST|")) {
                    String otherUser = message.substring(
                            "HISTORY_REQUEST|".length()).trim();

                    if (otherUser.isEmpty()) {
                        output.println("Choose a user first.");
                    } else {
                        sendConversationHistory(otherUser);
                    }

                    continue;
                }

                if (message.startsWith("PUBLIC_KEY|")) {
                    String publicKey = message.substring("PUBLIC_KEY|".length()).trim();

                    if (publicKey.isEmpty()) {
                        output.println("PUBLIC_KEY_ERROR");
                    } else if (Server.savePublicKey(username, publicKey)) {
                        output.println("PUBLIC_KEY_SAVED");
                    } else {
                        output.println("PUBLIC_KEY_ERROR");
                    }

                    continue;
                }

                if (message.startsWith("GET_PUBLIC_KEY|")) {
                    String requestedUser = message.substring(
                            "GET_PUBLIC_KEY|".length()).trim();

                    String publicKey = Server.getPublicKey(requestedUser);

                    if (publicKey == null || publicKey.isEmpty()) {
                        output.println("PUBLIC_KEY_NOT_FOUND|" + requestedUser);
                    } else {
                        output.println(
                                "PUBLIC_KEY|" + requestedUser + "|" + publicKey);
                    }

                    continue;
                }
                if (message.equalsIgnoreCase("/users")) {
                    sendOnlineUsers();
                    continue;
                }

                if (!message.startsWith("@") || !message.contains(" ")) {
                    output.println("Use: @username message");
                    continue;
                }

                int firstSpace = message.indexOf(" ");
                String recipient = message.substring(1, firstSpace);
                String directMessage = message.substring(firstSpace + 1);

                if (recipient.isEmpty() || directMessage.isEmpty()) {
                    output.println("Use: @username message");
                    continue;
                }

                System.out.println(
                        username + " to " + recipient + ": " + directMessage);

                if (sendDirectMessage(recipient, directMessage)) {
                    MessageStore.saveMessage(
                            new ChatMessage(username, recipient, directMessage));
                }
            }

        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        } finally {
            if (username != null) {
                broadcast(username + " has left the chat.", this);
            }

            Server.clients.remove(this);
        }
    }
}