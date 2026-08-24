import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Base64;

public class MessageStore {
    private static final String FILE_NAME = "ghostwire-messages.txt";

    public static void saveMessage(ChatMessage message) {
        String line =
                encode(message.getSender()) + "|" +
                encode(message.getRecipient()) + "|" +
                encode(message.getContent());

        try (PrintWriter writer = new PrintWriter(
                new FileWriter(FILE_NAME, true))) {

            writer.println(line);

        } catch (IOException e) {
            System.out.println("Could not save message: " + e.getMessage());
        }
    }

    public static ArrayList<ChatMessage> loadConversation(
            String firstUser,
            String secondUser) {

        ArrayList<ChatMessage> messages = new ArrayList<>();
        File messageFile = new File(FILE_NAME);

        if (!messageFile.exists()) {
            return messages;
        }

        try (BufferedReader reader = new BufferedReader(
                new FileReader(messageFile))) {

            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", 3);

                if (parts.length != 3) {
                    continue;
                }

                String sender = decode(parts[0]);
                String recipient = decode(parts[1]);
                String content = decode(parts[2]);

                boolean belongsToConversation =
                        (sender.equalsIgnoreCase(firstUser) &&
                        recipient.equalsIgnoreCase(secondUser))
                        ||
                        (sender.equalsIgnoreCase(secondUser) &&
                        recipient.equalsIgnoreCase(firstUser));

                if (belongsToConversation) {
                    messages.add(
                            new ChatMessage(sender, recipient, content)
                    );
                }
            }

        } catch (IOException | IllegalArgumentException e) {
            System.out.println(
                    "Could not load messages: " + e.getMessage()
            );
        }

        return messages;
    }

    private static String encode(String text) {
        return Base64.getEncoder().encodeToString(
                text.getBytes()
        );
    }

    private static String decode(String text) {
        return new String(
                Base64.getDecoder().decode(text)
        );
    }
}