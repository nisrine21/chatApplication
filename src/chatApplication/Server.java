package chatApplication;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Server {
    static ArrayList<ConnectionHandler> connections = new ArrayList<>();
    static ArrayList<String> connectedUsernames = new ArrayList<>();
    static ArrayList<String> disconnectedUsernames = new ArrayList<>();
    static final String FILE_DIRECTORY = "C:\\Users\\nis\\IdeaProjects\\chatApplication\\files\\";
    private static ServerGUI serverGUI;

    public static void main(String[] args) throws IOException {

        serverGUI = new ServerGUI(); // Instantiate the ServerGUI
        serverGUI.setVisible(true); // Make the GUI visible

        ServerSocket serverSocket = new ServerSocket(12345);
        System.out.println("Server is running...");

        while (true) {
            try {
                Socket client = serverSocket.accept();

                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);

                new Thread(handler).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class ConnectionHandler implements Runnable {
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;

        private String username;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out = new PrintWriter(client.getOutputStream(), true);

                // Get the username from the client
                username = in.readLine();

                // Check if the username is already in use
                if (isUsernameInUse(username)) {
                    sendUsernameAlreadyInUseMessage();
                    return; // Close connection
                }

                // Add the username to the list of connected users
                connectedUsernames.add(username);

                serverGUI.updateConnectedUserList(connectedUsernames); // Update connected user list in GUI
                serverGUI.logConnection(username); // Log connection

                // Notify clients that a new user has connected
                broadcast(username + " connected");
                sendConnectedUsersList();

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equalsIgnoreCase("/quit")) {
                        // User wants to log out
                        disconnectUser();
                        break;
                    } else if (message.equals("/file")) {
                        receiveFile();
                    } else if (message.startsWith("/msgto")) {
                        sendMessageToSpecificUser(message);
                    } else {
                        // Broadcast the message to all clients
                        broadcast(username + ": " + message);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                    out.close();
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void sendUsernameAlreadyInUseMessage() {
            out.println("Username already in use. Please choose a different username.");
            //out.println("/quit"); // Send /quit command to close the client's connection
        }

        private boolean isUsernameInUse(String username) {
            return connectedUsernames.contains(username);
        }

        private void disconnectUser() {
            connectedUsernames.remove(username);
            disconnectedUsernames.add(username);

            serverGUI.updateConnectedUserList(connectedUsernames); // Update connected user list in GUI
            serverGUI.updateDisconnectedUserList(disconnectedUsernames); // Update disconnected user list in GUI
            serverGUI.logDisconnection(username); // Log disconnection


            broadcast(username + " disconnected");
            sendConnectedUsersList();
        }


        private static final int DEFAULT_BUFFER_SIZE = 8192; // Default buffer size

        private void receiveFile() {
            try {
                String fileName = in.readLine();
                int fileSize = Integer.parseInt(in.readLine());

                System.out.println("Receiving file: " + fileName + " (" + fileSize + " bytes)");

                byte[] fileContent = new byte[fileSize];
                int bytesRead;
                int totalBytesRead = 0;
                int bufferSize = Math.min(DEFAULT_BUFFER_SIZE, fileSize); // Choose buffer size dynamically

                while ((bytesRead = client.getInputStream().read(fileContent, totalBytesRead, bufferSize)) != -1) {
                    totalBytesRead += bytesRead;
                    if (totalBytesRead == fileSize) break;
                    bufferSize = Math.min(DEFAULT_BUFFER_SIZE, fileSize - totalBytesRead); // Adjust buffer size for remaining bytes
                }

                System.out.println("Bytes read: " + totalBytesRead);

                if (totalBytesRead == fileSize) {
                    saveFile(fileName, fileContent);
                    broadcast(username + " sent a file: " + fileName);
                } else {
                    System.err.println("Error receiving file: " + fileName);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



        private void saveFile(String fileName, byte[] fileContent) {
            try {
                Files.write(Paths.get(FILE_DIRECTORY + fileName), fileContent);
                System.out.println("File saved successfully: " + FILE_DIRECTORY + fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        private void broadcast(String message) {
            for (ConnectionHandler connection : connections) {
                connection.sendMessage(message);
            }
        }

        private void sendMessage(String message) {
            out.println(message);
        }

        private void sendMessageToSpecificUser(String message) {
            String[] parts = message.split(" ");
            if (parts.length >= 3) {
                String recipient = parts[1];
                StringBuilder messageBuilder = new StringBuilder();
                for (int i = 2; i < parts.length; i++) {
                    messageBuilder.append(parts[i]).append(" ");
                }
                String messageToSend = messageBuilder.toString().trim();
                for (ConnectionHandler connection : connections) {
                    if (connection.username.equals(recipient)) {
                        connection.sendMessage(username + " (private): " + messageToSend);
                        break;
                    }
                }
            }
        }

        private void sendConnectedUsersList() {
            StringBuilder userListBuilder = new StringBuilder("/users ");
            for (String user : connectedUsernames) {
                userListBuilder.append(user).append(",");
            }
            broadcast(userListBuilder.toString());
        }

    }
}