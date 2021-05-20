package client;

import javax.naming.InvalidNameException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ClientSwingView {
    private final ClientController clientController;

    private final JFrame clientMainFrame = new JFrame("Multi-user chat client");

    private final JTextArea clientsMessagesTextArea = new JTextArea(20, 80);

    private final DefaultListModel<String> usernamesListModel = new DefaultListModel<>() {{
        addElement("Online users:");
    }};

    private final JList<String> connectedUsernamesList = new JList<>(usernamesListModel);

    private final JPanel interactionPanel = new JPanel();

    private final JTextField inputTextField = new JTextField(40);

    private final JButton disconnectButton = new JButton("Disconnect");

    private final JButton connectButton = new JButton("Connect");

    public ClientSwingView(ClientController clientController) {
        this.clientController = clientController;
        initClientGraphicInterface();
        showInitScreen();
    }

    private void initClientGraphicInterface() {
        configureInitClientsMessagesTextArea();
        configureInitInputTextField();
        configureInitButtonsPanel();
        configureInitServerMainFrame();
        configureUsernamesList();
        addButtonClickListenerToDisconnect();
        addButtonClickListenerToConnect();
        addControllerForInputTextField();
    }

    private void configureInitClientsMessagesTextArea() {
        clientsMessagesTextArea.setEditable(false);
        clientsMessagesTextArea.setLineWrap(true);
        Font boldFont = new Font(clientsMessagesTextArea.getFont().getName(), Font.BOLD, clientsMessagesTextArea.getFont().getSize());
        clientsMessagesTextArea.setFont(boldFont);
    }

    private void configureInitButtonsPanel() {
        interactionPanel.add(connectButton);
        interactionPanel.add(disconnectButton);
    }

    private void configureUsernamesList() {
        connectedUsernamesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    private void configureInitInputTextField() {
        interactionPanel.add(inputTextField);
    }

    private void configureInitServerMainFrame() {
        clientMainFrame.add(new JScrollPane(clientsMessagesTextArea), BorderLayout.CENTER);
        clientMainFrame.add(interactionPanel, BorderLayout.SOUTH);
        clientMainFrame.pack();
        clientMainFrame.add(new JScrollPane(connectedUsernamesList) {{
            Dimension dimension = connectedUsernamesList.getPreferredSize();
            dimension.width = 250;
            setPreferredSize(dimension);
        }}, BorderLayout.EAST);

        setInitWindowSize();
        addWindowListenerForOperateClosing();
        setInitServerWindowInScreenCenter();

    }

    private void setInitWindowSize() {
        clientMainFrame.setSize(1280, 720);
    }

    private void setInitServerWindowInScreenCenter() {
        clientMainFrame.setLocationRelativeTo(null);
    }

    private void addWindowListenerForOperateClosing() {
        clientMainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        clientMainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int chosenIndex = JOptionPane.showConfirmDialog(clientMainFrame,
                        "Are you sure?",
                        "Exit",
                        JOptionPane.YES_NO_OPTION);
                if (hasOkOptionChosen(chosenIndex)) {
                    if (clientController.hasClientStarted()) {
                        clientController.disconnectFromServer();
                    }
                    System.exit(0);
                }
            }

            private boolean hasOkOptionChosen(int chosenIndex) {
                return chosenIndex == 0;
            }
        });
    }

    private void addButtonClickListenerToDisconnect() {
        disconnectButton.addActionListener(e -> clientController.disconnectFromServer());
    }

    private void addButtonClickListenerToConnect() {
        connectButton.addActionListener(e -> clientController.establishConnectionToServer());
    }

    private void addControllerForInputTextField() {
        inputTextField.addActionListener(e -> {
            clientController.sendMessageToCommonChat(inputTextField.getText());
            inputTextField.setText("");
        });
    }


    private void showInitScreen() {
        clientMainFrame.setVisible(true);
    }

    protected void addMessageToCommonChat(String text) {
        clientsMessagesTextArea.append(text + "\n");
    }

    protected void clearUsernamesList() {
        usernamesListModel.clear();
        usernamesListModel.addElement("Online users:");
    }

    protected void addNewUserTpConnectedUsernamesList(String username) {
        usernamesListModel.addElement(username);
    }

    protected void removeNewUserTpConnectedUsernamesList(String username) {
        usernamesListModel.removeElement(username);
    }

    protected String requestServerAddressByShowingInputDialog() {
        while (true) {
            String serverAddress = JOptionPane.showInputDialog(
                    clientMainFrame,
                    "Enter the server IPv4 address:",
                    "Entering the server address",
                    JOptionPane.QUESTION_MESSAGE);
            if (isSeverAddressCorrect(serverAddress)) {
                return serverAddress;
            } else {
                JOptionPane.showMessageDialog(
                        clientMainFrame,
                        "Invalid server address entered. Try again.",
                        "Error entering the server address",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private boolean isSeverAddressCorrect(String serverAddress) {
        return serverAddress != null && !serverAddress.isEmpty();
    }

    protected int requestServerPortByShowingInputDialog() {
        while (true) {
            String port = JOptionPane.showInputDialog(
                    clientMainFrame,
                    "Enter the server port:",
                    "Entering the server port",
                    JOptionPane.QUESTION_MESSAGE);

            try {
                return Integer.parseInt(port.trim());
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(
                        clientMainFrame,
                        "Invalid port entered. Try again.",
                        "Error entering the server port",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    protected String requestUsernameByShowingInputDialog() {
        /// return user's input
        return JOptionPane.showInputDialog(
                clientMainFrame,
                "Enter the user name:",
                "Entering the user name",
                JOptionPane.QUESTION_MESSAGE);
    }

    protected String requestPasswordByShowingInputDialog() {
        /// return user's input
        return JOptionPane.showInputDialog(
                clientMainFrame,
                "Enter the current session password:",
                "Entering the password",
                JOptionPane.QUESTION_MESSAGE);
    }

    protected void showErrorMessageDialog(String errorText) {
        JOptionPane.showMessageDialog(
                clientMainFrame,
                errorText,
                "Error",
                JOptionPane.ERROR_MESSAGE);
    }
}
