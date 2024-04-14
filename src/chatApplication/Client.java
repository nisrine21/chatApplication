package chatApplication;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class Client {
    private static final int PORT = 12345;
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final String[] ALLOWED_EXTENSIONS = {"png", "jpeg", "jpg", "pdf", "txt"};
    private static final int DEFAULT_BUFFER_SIZE = 8192; // Default buffer size

    private BufferedReader reader;
    private PrintWriter writer;
    private Socket socket;
    private OutputStream outputStream;

    private String username;

    //Composants d'interface utilisateur Swing
    private JTextArea chatArea;
    private JTextField messageField;
    private JComboBox<String> sendToComboBox; // Dropdown to choose recipient
    private JList<String> connectedUsersList; // List of connected users
    private DefaultListModel<String> connectedUsersListModel; // Model for connected users list
    private JFrame loginFrame;

    public Client() {
        initializeLoginUI();
    }

    private void initializeLoginUI() {
        loginFrame = new JFrame("Login");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(300, 150);
        loginFrame.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(2, 1));
        JLabel label = new JLabel("Enter your username:");
        JTextField textField = new JTextField();
        JButton button = new JButton("Connect");

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String inputUsername = textField.getText().trim();
                if (!inputUsername.isEmpty()) {
                    username = inputUsername;
                    connectToServer();
                } else {
                    JOptionPane.showMessageDialog(loginFrame, "Please enter a username.");
                }
            }
        });

        textField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                button.doClick();
            }
        });

        panel.add(label);
        panel.add(textField);
        loginFrame.add(panel, BorderLayout.CENTER);
        loginFrame.add(button, BorderLayout.SOUTH);

        loginFrame.setLocationRelativeTo(null); // Center the frame
        loginFrame.setVisible(true);
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            outputStream = socket.getOutputStream();

            writer.println(username);


            String response = reader.readLine();
            if (response != null && response.equals("Username already in use. Please choose a different username.")) {
                JOptionPane.showMessageDialog(loginFrame, response);
                socket.close();
            } else {
                loginFrame.dispose(); // Close login window
                initializeChatUI();

                // Start a separate thread to listen for messages from the server
                new Thread(new ServerListener()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeChatUI() {
        JFrame chatFrame = new JFrame("Chat Client - " + username);
        chatFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        chatFrame.setSize(400, 300);

        chatArea = new JTextArea();
        chatArea.setEditable(false); // Ensures that users cannot edit the chat history
        JScrollPane scrollPane = new JScrollPane(chatArea); // Provides scrolling functionality
        chatFrame.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        messageField = new JTextField();
        messageField.addActionListener(new MessageFieldListener());
        bottomPanel.add(messageField, BorderLayout.CENTER);

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new MessageFieldListener());
        bottomPanel.add(sendButton, BorderLayout.EAST);

        JButton sendFileButton = new JButton("Choose File");
        sendFileButton.addActionListener(new SendFileButtonListener());
        bottomPanel.add(sendFileButton, BorderLayout.WEST);

        JButton logoutButton = new JButton("Log Out");
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                writer.println("/quit");
                logout();
            }
        });
        bottomPanel.add(logoutButton, BorderLayout.SOUTH);

        // Dropdown to choose recipient
        JPanel sendToPanel = new JPanel(new FlowLayout());
        JLabel sendToLabel = new JLabel("Send to:");
        sendToComboBox = new JComboBox<>();
        sendToComboBox.addItem("All Clients");
        sendToComboBox.addItem("Specific Client");
        sendToPanel.add(sendToLabel);
        sendToPanel.add(sendToComboBox);
        bottomPanel.add(sendToPanel, BorderLayout.NORTH);

        // Initialize connected users list
        connectedUsersListModel = new DefaultListModel<>();
        connectedUsersList = new JList<>(connectedUsersListModel);
        JScrollPane userListScrollPane = new JScrollPane(connectedUsersList);
        userListScrollPane.setPreferredSize(new Dimension(150, 0));

        // Add connected users list to the chat UI
        JPanel userListPanel = new JPanel(new BorderLayout());
        userListPanel.add(new JLabel("Connected Users"), BorderLayout.NORTH);
        userListPanel.add(userListScrollPane, BorderLayout.CENTER);
        chatFrame.add(userListPanel, BorderLayout.EAST);

        chatFrame.add(bottomPanel, BorderLayout.SOUTH);

        chatFrame.setLocationRelativeTo(null); // Center the frame
        chatFrame.setVisible(true);
    }

    private class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = reader.readLine()) != null) {
                    if (message.equals("Username already in use. Please choose a different username.")) {
                        JOptionPane.showMessageDialog(loginFrame, message);
                        logout(); // Close the client connection
                        break;
                    } else if (message.startsWith("/users ")) {
                        // Update connected users list
                        String[] users = message.substring(7).split(",");
                        connectedUsersListModel.clear();
                        connectedUsersListModel.addAll(Arrays.asList(users));
                    } else {
                        processMessage(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void processMessage(String message) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            String timestamp = dateFormat.format(new Date());
            chatArea.append("[" + timestamp + "] " + message + "\n");
        }
    }

    private class MessageFieldListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String input = messageField.getText();
            if (input.trim().isEmpty()) {
                return;
            }
            if (input.equalsIgnoreCase("/quit")) {
                writer.println("/quit");
                logout();
            } else {
                String recipient = (String) sendToComboBox.getSelectedItem();
                if (recipient.equals("All Clients")) {
                    writer.println(input);
                } else {
                    String selectedUser = connectedUsersList.getSelectedValue();
                    if (selectedUser != null) {
                        writer.println("/msgto " + selectedUser + " " + input);
                    } else {
                        JOptionPane.showMessageDialog(null,
                                "Please select a recipient from the connected users list.",
                                "Recipient Selection", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
            messageField.setText(""); //clears the content of the text field after the user has sent a message.
        }
    }

    private class SendFileButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            sendFileToServer();
        }

        private void sendFileToServer() {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                try {
                    long fileSize = selectedFile.length();
                    if (fileSize > MAX_FILE_SIZE) {
                        JOptionPane.showMessageDialog(null,
                                "File size exceeds the maximum allowed size.",
                                "File Size Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    // Check file extension against allowed extensions
                    String fileExtension = getFileExtension(selectedFile);
                    if (!isAllowedExtension(fileExtension)) {
                        JOptionPane.showMessageDialog(null,
                                "Only PNG, JPEG, TXT, and PDF files are allowed to be sent.",
                                "File Extension Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // Create input stream for reading file data
                    FileInputStream fileInputStream = new FileInputStream(selectedFile);
                    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                    int bytesRead;

                    // Send file metadata to the server
                    writer.println("/file");
                    writer.println(selectedFile.getName());
                    writer.println(fileSize);
                    writer.flush();

                    // Read from file and write to the output stream
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();

                    JOptionPane.showMessageDialog(null,
                            "File sent successfully.", "File Sent", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null,
                            "Error occurred while sending the file.",
                            "File Sending Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

    }

    private void logout() {
        try {
            reader.close();
            writer.close();
            socket.close();
            System.exit(0);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private String getFileExtension(File file) {
        String fileName = file.getName();
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex != -1 && lastIndex != 0) {
            return fileName.substring(lastIndex + 1);
        }
        return "";
    }

    private boolean isAllowedExtension(String extension) {
        // List of allowed extensions
        for (String allowedExtension : ALLOWED_EXTENSIONS) {
            if (extension.equalsIgnoreCase(allowedExtension)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}
