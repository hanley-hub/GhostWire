import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.DefaultListModel;
import javax.swing.JList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.security.GeneralSecurityException;
import java.security.KeyPair;

import java.security.PublicKey;
import javax.crypto.SecretKey;
import java.nio.file.Files;

import javax.swing.JFileChooser;
import java.awt.Desktop;

import java.io.InputStream;
import java.net.URL;

public class GhostWireGUI extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel statusLabel;
    private Socket socket;
    private PrintWriter output;
    private BufferedReader input;

    private JTextField recipientField;
    private JTextField messageField;
    private JTextPane chatArea;
    private DefaultListModel<String> onlineUsersModel;
    private JList<String> onlineUsersList;
    private JLabel activeChatLabel;

    private Map<String, ArrayList<String>> conversations = new HashMap<>();
    private String activeRecipient;
    private boolean loggingOut;

    private KeyPair encryptionKeys;
    private Map<String, PublicKey> recipientPublicKeys = new HashMap<>();
    private File lastReceivedFile;
    private JButton openFileButton;

    public GhostWireGUI() {
        setTitle("GhostWire");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        Image backgroundImage = loadImage("/assets/background.jpg");

        BackgroundPanel backgroundPanel = new BackgroundPanel(backgroundImage);

        backgroundPanel.setLayout(new GridBagLayout());

        JPanel loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBackground(new Color(5, 12, 18, 220));
        loginPanel.setBorder(
                javax.swing.BorderFactory.createEmptyBorder(28, 40, 28, 40));

        GridBagConstraints layout = new GridBagConstraints();
        layout.insets = new Insets(8, 8, 8, 8);
        layout.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("GHOSTWIRE", SwingConstants.CENTER);
        title.setFont(loadFont(28));
        title.setForeground(new Color(235, 245, 255));

        JLabel subtitle = new JLabel(
                "ENTER THE WIRE",
                SwingConstants.CENTER);
        subtitle.setFont(loadFont(13));
        subtitle.setForeground(new Color(170, 210, 230));

        JLabel usernameLabel = new JLabel("USERNAME");
        usernameLabel.setFont(loadFont(12));
        usernameLabel.setForeground(Color.WHITE);

        usernameField = new JTextField(18);
        usernameField.setFont(loadFont(14));

        JLabel passwordLabel = new JLabel("PASSWORD");
        passwordLabel.setFont(loadFont(12));
        passwordLabel.setForeground(Color.WHITE);

        passwordField = new JPasswordField(18);
        passwordField.setFont(loadFont(14));

        JButton loginButton = new JButton("LOGIN");
        loginButton.setFont(loadFont(13));

        JButton registerButton = new JButton("REGISTER");
        registerButton.setFont(loadFont(13));
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(loadFont(11));
        statusLabel.setForeground(new Color(255, 190, 190));
        loginButton.addActionListener(event -> authenticate("LOGIN"));
        registerButton.addActionListener(event -> authenticate("REGISTER"));

        layout.gridx = 0;
        layout.gridy = 5;
        layout.gridwidth = 2;
        loginPanel.add(statusLabel, layout);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 2;
        loginPanel.add(title, layout);

        layout.gridy = 1;
        loginPanel.add(subtitle, layout);

        layout.gridy = 2;
        layout.gridwidth = 1;
        loginPanel.add(usernameLabel, layout);

        layout.gridx = 1;
        loginPanel.add(usernameField, layout);

        layout.gridx = 0;
        layout.gridy = 3;
        loginPanel.add(passwordLabel, layout);

        layout.gridx = 1;
        loginPanel.add(passwordField, layout);

        layout.gridx = 0;
        layout.gridy = 4;
        loginPanel.add(loginButton, layout);

        layout.gridx = 1;
        loginPanel.add(registerButton, layout);

        backgroundPanel.add(loginPanel);
        setContentPane(backgroundPanel);
    }

    private void authenticate(String action) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Enter a username and password.");
            return;
        }

        statusLabel.setForeground(new Color(190, 220, 235));
        statusLabel.setText("Connecting...");

        new Thread(() -> {
            try {
                socket = new Socket("129.225.75.62", 5000);

                output = new PrintWriter(
                        socket.getOutputStream(),
                        true);

                input = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                output.println(action + "|" + username + "|" + password);

                String serverResponse = input.readLine();

                SwingUtilities.invokeLater(() -> {
                    if ("AUTHENTICATION_ACCEPTED".equals(serverResponse)) {
                        try {
                            encryptionKeys = KeyStoreUtil.loadOrCreate(username);
                            output.println(
                                    "PUBLIC_KEY|" +
                                            CryptoUtil.keyToText(encryptionKeys.getPublic()));

                            showChatScreen();
                            startMessageReceiver();

                        } catch (IOException | GeneralSecurityException e) {
                            statusLabel.setForeground(
                                    new Color(255, 190, 190));
                            statusLabel.setText("Could not prepare encryption keys.");
                            closeConnection();
                        }
                    } else {
                        statusLabel.setForeground(
                                new Color(255, 190, 190));
                        statusLabel.setText(serverResponse);
                        closeConnection();
                    }
                });

            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setForeground(
                            new Color(255, 190, 190));
                    statusLabel.setText("Could not connect to server.");
                });
            }
        }, "GhostWire-Authentication").start();
    }

    private void closeConnection() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            // The connection is already closed.
        }

        socket = null;
        output = null;
        input = null;
    }

    private void showChatScreen() {
        Image backgroundImage = loadImage(
                "/assets/chat-background.png");

        BackgroundPanel backgroundPanel = new BackgroundPanel(backgroundImage);
        onlineUsersModel = new DefaultListModel<>();
        onlineUsersList = new JList<>(onlineUsersModel);

        onlineUsersList.setFont(loadFont(14));
        onlineUsersList.setForeground(new Color(220, 240, 255));
        onlineUsersList.setBackground(new Color(5, 12, 18, 220));

        onlineUsersList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                String selectedUser = onlineUsersList.getSelectedValue();

                if (selectedUser != null) {
                    recipientField.setText(selectedUser);
                    activeChatLabel.setText("CHAT WITH: " + selectedUser.toUpperCase());
                    activeRecipient = selectedUser;

                    conversations.remove(activeRecipient);
                    showConversation(activeRecipient);

                    output.println("HISTORY_REQUEST|" + activeRecipient);
                    output.println("GET_PUBLIC_KEY|" + activeRecipient);
                }
            }
        });

        backgroundPanel.setLayout(new BorderLayout(12, 12));
        backgroundPanel.setBorder(
                javax.swing.BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel topPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 10, 10));
        activeChatLabel = new JLabel("SELECT A USER");
        activeChatLabel.setFont(loadFont(18));
        activeChatLabel.setForeground(new Color(170, 220, 255));
        topPanel.setBackground(new Color(5, 12, 18, 220));

        JLabel recipientLabel = new JLabel("CHAT WITH");
        recipientLabel.setFont(loadFont(12));
        recipientLabel.setForeground(Color.WHITE);

        recipientField = new JTextField(16);
        recipientField.setFont(loadFont(13));

        JButton onlineUsersButton = new JButton("ONLINE USERS");
        onlineUsersButton.setFont(loadFont(11));
        onlineUsersButton.addActionListener(
                event -> output.println("/users"));
        JButton logoutButton = new JButton("LOG OUT");
        logoutButton.setFont(loadFont(11));
        logoutButton.addActionListener(event -> logout());
        openFileButton = new JButton("OPEN RECEIVED FILE");
        openFileButton.setFont(loadFont(11));
        openFileButton.setEnabled(false);
        openFileButton.addActionListener(
                event -> openLastReceivedFile());

        topPanel.add(recipientLabel);
        topPanel.add(recipientField);
        topPanel.add(onlineUsersButton);
        topPanel.add(logoutButton);
        topPanel.add(openFileButton);
        topPanel.add(activeChatLabel);

        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setFont(loadFont(13));
        chatArea.setOpaque(false);

        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setOpaque(false);
        chatScrollPane.getViewport().setOpaque(false);
        chatScrollPane.setBorder(
                javax.swing.BorderFactory.createEmptyBorder());

        JPanel messagePanel = new JPanel(
                new BorderLayout(10, 0));
        messagePanel.setBackground(new Color(5, 12, 18, 220));

        messageField = new JTextField();
        messageField.setFont(loadFont(13));

        JButton fileButton = new JButton("SEND FILE");
        fileButton.setFont(loadFont(11));
        fileButton.addActionListener(event -> chooseAndSendFile());

        JButton sendButton = new JButton("SEND");
        sendButton.setFont(loadFont(12));
        sendButton.addActionListener(event -> sendChatMessage());
        messageField.addActionListener(event -> sendChatMessage());

        messagePanel.add(fileButton, BorderLayout.WEST);
        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);
        JPanel usersPanel = new JPanel(new BorderLayout(8, 8));
        usersPanel.setBackground(new Color(5, 12, 18, 220));
        usersPanel.setBorder(
                javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel usersTitle = new JLabel("ONLINE USERS");
        usersTitle.setFont(loadFont(14));
        usersTitle.setForeground(new Color(170, 220, 255));

        JScrollPane usersScrollPane = new JScrollPane(onlineUsersList);
        usersScrollPane.setPreferredSize(new java.awt.Dimension(190, 0));
        usersScrollPane.setBorder(
                javax.swing.BorderFactory.createLineBorder(
                        new Color(80, 180, 230)));

        usersPanel.add(usersTitle, BorderLayout.NORTH);
        usersPanel.add(usersScrollPane, BorderLayout.CENTER);

        backgroundPanel.add(topPanel, BorderLayout.NORTH);
        backgroundPanel.add(usersPanel, BorderLayout.WEST);
        backgroundPanel.add(chatScrollPane, BorderLayout.CENTER);
        backgroundPanel.add(messagePanel, BorderLayout.SOUTH);

        setContentPane(backgroundPanel);
        revalidate();
        repaint();
        output.println("/users");
    }

    private void handleIncomingFile(String receivedMessage) {
        String[] fileParts = receivedMessage.split("\\|", 5);

        if (fileParts.length != 5) {
            SwingUtilities.invokeLater(() -> {
                appendChatMessage("Received an invalid file transfer.");
            });
            return;
        }

        try {
            String sender = fileParts[1];
            String encryptedFileName = fileParts[2];
            String encryptedMessageKey = fileParts[3];
            String encryptedFile = fileParts[4];

            SecretKey messageKey = CryptoUtil.decryptMessageKey(
                    encryptedMessageKey,
                    encryptionKeys.getPrivate());

            String fileName = CryptoUtil.decryptText(
                    encryptedFileName,
                    messageKey);

            byte[] fileBytes = CryptoUtil.decryptBytes(
                    encryptedFile,
                    messageKey);

            if (fileBytes.length > 5 * 1024 * 1024) {
                throw new IOException("Received file is too large.");
            }

            File downloadsFolder = new File("ghostwire-downloads");

            if (!downloadsFolder.exists() &&
                    !downloadsFolder.mkdirs()) {
                throw new IOException(
                        "Could not create downloads folder.");
            }

            String safeFileName = new File(fileName).getName();

            if (safeFileName.isEmpty()) {
                safeFileName = "received-file";
            }

            File savedFile = new File(
                    downloadsFolder,
                    System.currentTimeMillis() + "_" + safeFileName);

            Files.write(savedFile.toPath(), fileBytes);

            lastReceivedFile = savedFile;

            String savedFileName = savedFile.getName();

            SwingUtilities.invokeLater(() -> {
                openFileButton.setEnabled(true);

                appendChatMessage(
                        sender + " sent a file: " + savedFileName);
            });

        } catch (IOException | GeneralSecurityException e) {
            SwingUtilities.invokeLater(() -> {
                appendChatMessage(
                        "Could not decrypt or save received file.");
            });
        }
    }

    private void openLastReceivedFile() {
        if (lastReceivedFile == null ||
                !lastReceivedFile.exists()) {

            appendChatMessage("No received file is available.");
            openFileButton.setEnabled(false);
            return;
        }

        if (!Desktop.isDesktopSupported()) {
            appendChatMessage(
                    "This computer cannot open files from GhostWire.");
            return;
        }

        try {
            Desktop.getDesktop().open(lastReceivedFile);

        } catch (IOException e) {
            appendChatMessage(
                    "Could not open the received file.");
        }
    }

    private void startMessageReceiver() {
        new Thread(() -> {
            try {
                String message;

                while ((message = input.readLine()) != null) {
                    if (message.startsWith("FILE|")) {
                        handleIncomingFile(message);
                        continue;
                    }

                    String receivedMessage = message;

                    SwingUtilities.invokeLater(() -> {
                        if (receivedMessage.startsWith("HISTORY|")) {
                            String[] historyParts = receivedMessage.split("\\|", 4);

                            if (historyParts.length == 4) {
                                String historyUser = historyParts[1];
                                String direction = historyParts[2];
                                String content = historyParts[3];

                                String displayMessage;

                                if (content.startsWith("ENCRYPTED|")) {
                                    String[] encryptedParts = content.split("\\|", 4);

                                    if (encryptedParts.length != 4) {
                                        return;
                                    }

                                    try {
                                        String encryptedMessageKey;

                                        if (direction.equals("OUT")) {
                                            encryptedMessageKey = encryptedParts[2];
                                        } else {
                                            encryptedMessageKey = encryptedParts[1];
                                        }

                                        SecretKey messageKey = CryptoUtil.decryptMessageKey(
                                                encryptedMessageKey,
                                                encryptionKeys.getPrivate());

                                        String decryptedText = CryptoUtil.decryptText(
                                                encryptedParts[3],
                                                messageKey);

                                        if (direction.equals("OUT")) {
                                            displayMessage = "You: " + decryptedText;
                                        } else {
                                            displayMessage = historyUser + ": " + decryptedText;
                                        }

                                    } catch (GeneralSecurityException e) {
                                        displayMessage = "Could not decrypt a saved message.";
                                    }

                                } else if (direction.equals("OUT")) {
                                    displayMessage = "You: " + content;

                                } else {
                                    displayMessage = historyUser + ": " + content;
                                }

                                saveConversationMessage(
                                        historyUser,
                                        displayMessage);
                            }

                        } else if (receivedMessage.equals("HISTORY_END")) {
                            // The server has finished sending saved messages.

                        } else if (receivedMessage.equals("PUBLIC_KEY_SAVED")) {
                            // The server saved this user's public key.

                        } else if (receivedMessage.startsWith("PUBLIC_KEY|")) {
                            String[] keyParts = receivedMessage.split("\\|", 3);

                            if (keyParts.length == 3) {
                                try {
                                    recipientPublicKeys.put(
                                            keyParts[1].toLowerCase(),
                                            CryptoUtil.textToPublicKey(
                                                    keyParts[2]));
                                } catch (GeneralSecurityException e) {
                                    appendChatMessage(
                                            "Could not read encryption key for " +
                                                    keyParts[1] + ".");
                                }
                            }

                        } else if (receivedMessage.startsWith(
                                "PUBLIC_KEY_NOT_FOUND|")) {

                            String requestedUser = receivedMessage.substring(
                                    "PUBLIC_KEY_NOT_FOUND|".length());

                            appendChatMessage(
                                    "No public encryption key found for " +
                                            requestedUser + ".");

                        } else if (receivedMessage.startsWith(
                                "Online users:")) {

                            updateOnlineUsers(
                                    receivedMessage.substring(
                                            "Online users:".length()).trim());

                        } else if (receivedMessage.equals(
                                "No other users are online.")) {

                            onlineUsersModel.clear();

                        } else if (receivedMessage.contains(
                                " has joined the chat.") ||
                                receivedMessage.contains(
                                        " has left the chat.")) {

                            appendChatMessage(receivedMessage);
                            output.println("/users");

                        } else if (receivedMessage.startsWith("ENCRYPTED|")) {
                            String[] encryptedParts = receivedMessage.split("\\|", 4);

                            if (encryptedParts.length == 4) {
                                try {
                                    String sender = encryptedParts[1];
                                    String encryptedMessageKey = encryptedParts[2];
                                    String encryptedText = encryptedParts[3];

                                    SecretKey messageKey = CryptoUtil.decryptMessageKey(
                                            encryptedMessageKey,
                                            encryptionKeys.getPrivate());

                                    String decryptedText = CryptoUtil.decryptText(
                                            encryptedText,
                                            messageKey);

                                    saveConversationMessage(
                                            sender,
                                            sender + ": " + decryptedText);

                                } catch (GeneralSecurityException e) {
                                    appendChatMessage(
                                            "Could not decrypt message. " +
                                                    "It may be damaged.");
                                }
                            }

                        } else if (receivedMessage.contains(": ")) {
                            int separator = receivedMessage.indexOf(": ");

                            String sender = receivedMessage.substring(
                                    0,
                                    separator);

                            saveConversationMessage(
                                    sender,
                                    receivedMessage);

                        } else {
                            appendChatMessage(receivedMessage);
                        }

                        chatArea.setCaretPosition(
                                chatArea.getDocument().getLength());
                    });
                }

            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    appendChatMessage("Disconnected from server.");
                });
            }
        }, "GhostWire-Message-Receiver").start();
    }

    private void appendChatMessage(String message) {
        Color messageColor;

        if (message.startsWith("You:")) {
            messageColor = new Color(80, 225, 255);

        } else if (message.startsWith("Online users:") ||
                message.startsWith("No other users") ||
                message.startsWith("Choose ") ||
                message.contains(" has joined the chat.") ||
                message.contains(" has left the chat.") ||
                message.startsWith("Disconnected")) {
            messageColor = new Color(180, 195, 205);

        } else {
            messageColor = new Color(255, 175, 85);
        }

        StyledDocument document = chatArea.getStyledDocument();
        SimpleAttributeSet style = new SimpleAttributeSet();

        StyleConstants.setForeground(style, messageColor);
        StyleConstants.setFontSize(style, 22);
        StyleConstants.setBold(style, true);

        try {
            document.insertString(
                    document.getLength(),
                    message + "\n",
                    style);

            chatArea.setCaretPosition(document.getLength());

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void logout() {
        loggingOut = true;

        if (output != null) {
            output.println("exit");
        }

        closeConnection();
        dispose();

        GhostWireGUI loginWindow = new GhostWireGUI();
        loginWindow.setVisible(true);
    }

    private void chooseAndSendFile() {
        String recipient = recipientField.getText().trim();

        if (recipient.isEmpty()) {
            appendChatMessage("Choose a recipient first.");
            return;
        }

        PublicKey recipientPublicKey = recipientPublicKeys.get(
                recipient.toLowerCase());

        if (recipientPublicKey == null) {
            appendChatMessage(
                    "Waiting for " + recipient + "'s encryption key.");
            output.println("GET_PUBLIC_KEY|" + recipient);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();

        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = fileChooser.getSelectedFile();

        if (selectedFile.length() > 5L * 1024 * 1024) {
            appendChatMessage(
                    "File is too large. Maximum size is 5 MB.");
            return;
        }

        try {
            byte[] fileBytes = Files.readAllBytes(
                    selectedFile.toPath());

            SecretKey messageKey = CryptoUtil.generateMessageKey();

            String encryptedFile = CryptoUtil.encryptBytes(
                    fileBytes,
                    messageKey);

            String encryptedFileName = CryptoUtil.encryptText(
                    selectedFile.getName(),
                    messageKey);

            String encryptedRecipientKey = CryptoUtil.encryptMessageKey(
                    messageKey,
                    recipientPublicKey);

            output.println(
                    "FILE|" + recipient + "|" +
                            encryptedFileName + "|" +
                            encryptedRecipientKey + "|" +
                            encryptedFile);

            appendChatMessage(
                    "You sent a file: " + selectedFile.getName());

        } catch (IOException | GeneralSecurityException e) {
            appendChatMessage("Could not encrypt or read this file.");
        }
    }

    private void sendChatMessage() {
        String recipient = recipientField.getText().trim();
        String message = messageField.getText().trim();

        if (recipient.isEmpty()) {
            appendChatMessage("Choose a recipient first.");
            return;
        }

        if (message.isEmpty()) {
            return;
        }

        PublicKey recipientPublicKey = recipientPublicKeys.get(
                recipient.toLowerCase());

        if (recipientPublicKey == null) {
            appendChatMessage(
                    "Waiting for " + recipient + "'s encryption key.");
            output.println("GET_PUBLIC_KEY|" + recipient);
            return;
        }

        try {
            SecretKey messageKey = CryptoUtil.generateMessageKey();

            String encryptedText = CryptoUtil.encryptText(
                    message,
                    messageKey);

            String encryptedRecipientKey = CryptoUtil.encryptMessageKey(
                    messageKey,
                    recipientPublicKey);

            String encryptedSenderKey = CryptoUtil.encryptMessageKey(
                    messageKey,
                    encryptionKeys.getPublic());

            output.println(
                    "ENCRYPTED|" + recipient + "|" +
                            encryptedRecipientKey + "|" +
                            encryptedSenderKey + "|" +
                            encryptedText);

            saveConversationMessage(recipient, "You: " + message);
            messageField.setText("");

        } catch (GeneralSecurityException e) {
            appendChatMessage("Could not encrypt message.");
        }
    }

    private void saveConversationMessage(String person, String message) {
        conversations
                .computeIfAbsent(person, key -> new ArrayList<>())
                .add(message);

        if (person.equalsIgnoreCase(activeRecipient)) {
            appendChatMessage(message);
        }
    }

    private void showConversation(String recipient) {
        chatArea.setText("");

        ArrayList<String> messages = conversations.get(recipient);

        if (messages == null) {
            return;
        }

        for (String message : messages) {
            appendChatMessage(message);
        }
    }

    private void updateOnlineUsers(String usersText) {
        onlineUsersModel.clear();

        if (usersText.isEmpty()) {
            return;
        }

        for (String user : usersText.split(",")) {
            String cleanUsername = user.trim();

            if (!cleanUsername.isEmpty()) {
                onlineUsersModel.addElement(cleanUsername);
            }
        }
    }

    private Image loadImage(String resourcePath) {
        URL imageUrl = getClass().getResource(resourcePath);

        if (imageUrl == null) {
            System.out.println("Could not find image: " + resourcePath);
            return null;
        }

        return new ImageIcon(imageUrl).getImage();
    }

    private Font loadFont(float size) {
        try (InputStream fontStream = getClass().getResourceAsStream(
                "/assets/Moki-Mono.otf")) {

            if (fontStream == null) {
                return new Font("Monospaced", Font.BOLD, (int) size);
            }

            Font customFont = Font.createFont(
                    Font.TRUETYPE_FONT,
                    fontStream);

            return customFont.deriveFont(size);

        } catch (IOException | java.awt.FontFormatException e) {
            return new Font("Monospaced", Font.BOLD, (int) size);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GhostWireGUI window = new GhostWireGUI();
            window.setVisible(true);
        });
    }

    private static class BackgroundPanel extends JPanel {
        private final Image backgroundImage;

        public BackgroundPanel(Image backgroundImage) {
            this.backgroundImage = backgroundImage;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D graphics2D = (Graphics2D) graphics;
            graphics2D.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            graphics2D.drawImage(
                    backgroundImage,
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    this);

            graphics2D.setColor(new Color(0, 0, 0, 120));
            graphics2D.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}