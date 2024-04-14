package chatApplication;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ServerGUI extends JFrame {
    private JLabel statusLabel;
    private JTextArea logArea;
    private JList<String> connectedUserList;
    private DefaultListModel<String> connectedUserListModel;
    private JList<String> disconnectedUserList;
    private DefaultListModel<String> disconnectedUserListModel;

    public ServerGUI() {
        setTitle("Server GUI");
        setSize(600, 400); // Adjusted size to accommodate both lists
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initComponents();
        setLocationRelativeTo(null); // Center the frame
    }

    private void initComponents() {
        statusLabel = new JLabel("Server Status: Running");
        JLabel connectedUserLabel = new JLabel("Connected Users:");
        JLabel disconnectedUserLabel = new JLabel("Disconnected Users:");
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);

        connectedUserListModel = new DefaultListModel<>();
        connectedUserList = new JList<>(connectedUserListModel);
        JScrollPane connectedUserScrollPane = new JScrollPane(connectedUserList);
        connectedUserScrollPane.setPreferredSize(new Dimension(150, 0));

        disconnectedUserListModel = new DefaultListModel<>();
        disconnectedUserList = new JList<>(disconnectedUserListModel);
        JScrollPane disconnectedUserScrollPane = new JScrollPane(disconnectedUserList);
        disconnectedUserScrollPane.setPreferredSize(new Dimension(150, 0));

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(statusLabel);

        JPanel connectedUserPanel = new JPanel(new BorderLayout());
        connectedUserPanel.add(connectedUserLabel, BorderLayout.NORTH);
        connectedUserPanel.add(connectedUserScrollPane, BorderLayout.CENTER);

        JPanel disconnectedUserPanel = new JPanel(new BorderLayout());
        disconnectedUserPanel.add(disconnectedUserLabel, BorderLayout.NORTH);
        disconnectedUserPanel.add(disconnectedUserScrollPane, BorderLayout.CENTER);

        JPanel userPanel = new JPanel(new GridLayout(1, 2));
        userPanel.add(connectedUserPanel);
        userPanel.add(disconnectedUserPanel);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(logScrollPane, BorderLayout.CENTER);
        centerPanel.add(userPanel, BorderLayout.EAST);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(statusPanel, BorderLayout.NORTH);
        getContentPane().add(centerPanel, BorderLayout.CENTER);
    }


    public void logMessage(String message) {
        logArea.append(message + "\n");
    }

    public void updateConnectedUserList(ArrayList<String> connectedUsernames) {
        connectedUserListModel.clear();
        for (String username : connectedUsernames) {
            connectedUserListModel.addElement(username);
        }
    }

    public void updateDisconnectedUserList(ArrayList<String> disconnectedUsernames) {
        disconnectedUserListModel.clear();
        for (String username : disconnectedUsernames) {
            disconnectedUserListModel.addElement(username);
        }
    }

    public void logConnection(String username) {
        logMessage(getTimestamp() + " " + username + " connected");
    }

    public void logDisconnection(String username) {
        logMessage(getTimestamp() + " " + username + " disconnected");
    }

    private String getTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        return "[" + dateFormat.format(new Date()) + "]";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerGUI serverGUI = new ServerGUI();
            serverGUI.setVisible(true);
        });
    }
}
