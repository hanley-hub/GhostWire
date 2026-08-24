import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {

        String serverAddress = "localhost";
        int port = 5000;
        Scanner scanner = new Scanner(System.in);

        try {
            Socket socket = new Socket(serverAddress, port);

            System.out.println("Connected to the GhostWire Server!");

            PrintWriter output = new PrintWriter(
                    socket.getOutputStream(), true);
            BufferedReader input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            System.out.print("Register or login? (register/login): ");
            String action = scanner.nextLine();

            System.out.print("Username: ");
            String username = scanner.nextLine();

            System.out.print("Password: ");
            String password = scanner.nextLine();

            if (action.equalsIgnoreCase("register")) {
                output.println("REGISTER|" + username + "|" + password);

            } else if (action.equalsIgnoreCase("login")) {
                output.println("LOGIN|" + username + "|" + password);

            } else {
                System.out.println("Choose register or login.");
                socket.close();
                return;
            }

            String serverResponse = input.readLine();

            if (!"AUTHENTICATION_ACCEPTED".equals(serverResponse)) {
                System.out.println(serverResponse);
                socket.close();
                return;
            }
            String recipient = null;
            Thread receiver = new Thread(() -> {
                try {
                    String message;

                    while ((message = input.readLine()) != null) {
                        System.out.println("\n" + message);
                        System.out.print("You: ");
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });
            receiver.start();
            while (true) {
                System.out.print("You: ");
                String message = scanner.nextLine();

                if (message.equalsIgnoreCase("/users")) {
                    output.println("/users");
                    continue;
                }

                if (message.startsWith("/chat ")) {
                    String newRecipient = message.substring(6).trim();

                    if (newRecipient.isEmpty()) {
                        System.out.println("Use: /chat username");
                    } else {
                        recipient = newRecipient;
                        System.out.println("Now chatting with: " + recipient);
                    }

                    continue;
                }

                if (message.equalsIgnoreCase("exit")) {
                    break;
                }

                if (recipient == null) {
                    System.out.println("Choose a user first with: /chat username");
                    continue;
                }

                output.println("@" + recipient + " " + message);
            }
            socket.close();

        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }
}
