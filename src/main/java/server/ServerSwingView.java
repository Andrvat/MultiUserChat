package server;

import connection.Message;
import connection.MessageType;
import connection.ServerObserver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class ServerSwingView implements ServerObserver {
    private final JFrame serverMainFrame = new JFrame("Multi-user char server");

    private final JTextArea serverLogsTextArea = new JTextArea(20, 80) {{
        append("Server logging messages:\n");
    }};

    private final JButton serverStartButton = new JButton("Launch server");

    private final JButton serverStopButton = new JButton("Stop server");

    private final JPanel buttonsPanel = new JPanel();

    private final DefaultListModel<String> usernamesListModel = new DefaultListModel<>() {{
        addElement("Online users:");
    }};
    private final JList<String> connectedUsernamesList = new JList<>(usernamesListModel);

    private final JMenuBar menuBar = new JMenuBar();

    private final ServerController serverController;

    public ServerSwingView(ServerController serverController) {
        this.serverController = serverController;
        initServerGraphicInterface();
        showInitScreen();
    }

    private void initServerGraphicInterface() {
        configureInitServerLogsTextArea();
        configureInitButtonsPanel();
        configureUsernamesList();
        configureInitServerMainFrame();
        configureInitMenuBar();
        addButtonClickListenerToStartServer();
        addButtonClickListenerToStopServer();
    }

    private void configureInitServerLogsTextArea() {
        serverLogsTextArea.setEditable(false);
        serverLogsTextArea.setLineWrap(true);
        Font boldFont = new Font(serverLogsTextArea.getFont().getName(), Font.BOLD, serverLogsTextArea.getFont().getSize());
        serverLogsTextArea.setFont(boldFont);
    }

    private void configureInitButtonsPanel() {
        buttonsPanel.add(serverStartButton);
        buttonsPanel.add(serverStopButton);
    }

    private void configureInitMenuBar() {
        menuBar.add(getBuiltHelpMenuBar());
    }

    private void configureUsernamesList() {
        connectedUsernamesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        connectedUsernamesList.addListSelectionListener(e -> {
            String selectedUsername = connectedUsernamesList.getSelectedValue();
            JOptionPane.showMessageDialog(
                    serverMainFrame,
                    serverController.getServerModel().getUserMetaInfoByUsername(selectedUsername).toString(),
                    "Meta-Info about user",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private JMenu getBuiltHelpMenuBar() {
        JMenu helpMenu = new JMenu("Help");
        JMenuItem itemAbout = new JMenuItem("About...");
        itemAbout.addActionListener(e -> JOptionPane.showMessageDialog(
                serverMainFrame,
                "Andrvat",
                "Information about developers",
                JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(itemAbout);

        return helpMenu;
    }

    private void configureInitServerMainFrame() {
        serverMainFrame.add(new JScrollPane(serverLogsTextArea), BorderLayout.CENTER);
        serverMainFrame.add(new JScrollPane(connectedUsernamesList) {{
            Dimension dimension = connectedUsernamesList.getPreferredSize();
            dimension.width = 250;
            setPreferredSize(dimension);
        }}, BorderLayout.EAST);
        serverMainFrame.add(buttonsPanel, BorderLayout.SOUTH);
        serverMainFrame.setJMenuBar(menuBar);
        serverMainFrame.pack();

        setInitWindowSize();
        setInitServerWindowInScreenCenter();
        addWindowListenerForOperateClosing();
    }

    private void setInitServerWindowInScreenCenter() {
        serverMainFrame.setLocationRelativeTo(null);
    }

    private void setInitWindowSize() {
        serverMainFrame.setSize(1280, 720);
    }

    private void addWindowListenerForOperateClosing() {
        serverMainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        serverMainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int userAnswer = JOptionPane.showConfirmDialog(serverMainFrame,
                        "Are you sure?",
                        "Exit",
                        JOptionPane.YES_NO_OPTION);
                if (userAnswer == 0) {
                    serverController.stopServer();
                    System.exit(0);
                }
            }
        });
    }

    private void addButtonClickListenerToStartServer() {
        serverStartButton.addActionListener(e -> {
            try {
                int serverPort = getServerPortFromPopupOptionPane();
                serverController.startServerOnPort(serverPort);
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(
                        serverMainFrame,
                        "The server cannot be started on this port. Try changing the port.",
                        "Server launching error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void addButtonClickListenerToStopServer() {
        serverStopButton.addActionListener(e -> serverController.stopServer());
    }

    private void showInitScreen() {
        serverMainFrame.setVisible(true);
    }

    public void addServiceMessageToServerLogsTextArea(String serviceMessage) {
        serverLogsTextArea.append(serviceMessage);
    }

    private int getServerPortFromPopupOptionPane() {
        while (true) {
            String stringServerPort = JOptionPane.showInputDialog(
                    serverMainFrame,
                    "Enter the server port number:",
                    "Entering the server port",
                    JOptionPane.QUESTION_MESSAGE);

            try {
                return Integer.parseInt(stringServerPort.trim());
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(
                        serverMainFrame,
                        "An invalid server port was entered. Please, try again...",
                        "Server port input error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void update(Message message) {
        if (MessageType.isTypeNotifyToAdd(message.getMessageType())) {
            usernamesListModel.addElement(message.getMessageText());
        }

        if (MessageType.isTypeNotifyToRemove(message.getMessageType())) {
            usernamesListModel.removeElement(message.getMessageText());
        }
    }
}