package client;

import connection.*;

import java.io.IOException;
import java.net.Socket;

public class ClientController {
    private UserConnection userConnection;
    private ClientModel clientModel;
    private ClientSwingView graphicView;
    private volatile boolean hasClientConnectedToServer = false;

    public boolean hasClientStarted() {
        return hasClientConnectedToServer;
    }

    public void setGraphicView(ClientSwingView graphicView) {
        this.graphicView = graphicView;
    }

    public void setClientModel(ClientModel clientModel) {
        this.clientModel = clientModel;
    }

    public void launch() {
        while (true) {
            if (hasClientConnectedToServer) {
                registerOnServer();
                receiveMessageFromCommonChat();
                hasClientConnectedToServer = false;
            }
        }
    }

    protected void establishConnectionToServer() {
        if (!hasClientConnectedToServer) {
            try {
                String serverAddress = graphicView.requestServerAddressByShowingInputDialog();
                int port = graphicView.requestServerPortByShowingInputDialog();

                Socket socket = new Socket(serverAddress, port);
                userConnection = new UserConnection(socket);
                hasClientConnectedToServer = true;
                graphicView.addMessageToCommonChat("You have connected to the server");
            } catch (Exception exception) {
                graphicView.showErrorMessageDialog(
                        "An error has occurred! " +
                                "You may have entered the wrong server inet address or port. Try again");
            }
        } else {
            graphicView.showErrorMessageDialog("You are already connected!");
        }
    }

    protected void registerOnServer() {
        while (true) {
            try {
                Message serverResponse = userConnection.receive();

                if (MessageType.isTypeRequestUsername(serverResponse.getMessageType())) {
                    String username = graphicView.requestUsernameByShowingInputDialog();
                    userConnection.send(new Message(MessageType.NEW_USERNAME, username));
                }

                if (MessageType.isTypeRequestPassword(serverResponse.getMessageType())) {
                    String password = graphicView.requestPasswordByShowingInputDialog();
                    userConnection.send(new Message(MessageType.NEW_PASSWORD, password));
                }

                if (MessageType.isTypeLoginError(serverResponse.getMessageType())) {
                    graphicView.showErrorMessageDialog("This name is already in use, enter another one...");
                    continue;
                }

                if (MessageType.isTypeLoginAccepted(serverResponse.getMessageType())) {
                    graphicView.addMessageToCommonChat("Your name is accepted! Welcome to common chat!");
                    clientModel.setConnectedUsernames(serverResponse.getConnectedUsernames());
                    break;
                }

            } catch (Exception exception) {
                graphicView.showErrorMessageDialog(
                        "An error occurred while registering. Try reconnecting...");
                try {
                    userConnection.close();
                    hasClientConnectedToServer = false;
                    break;
                } catch (IOException ioException) {
                    graphicView.showErrorMessageDialog(
                            "Error when closing the connection");
                }
            }

        }
    }

    protected void sendMessageToCommonChat(String textToSend) {
        try {
            userConnection.send(new Message(MessageType.TEXT_MESSAGE, textToSend));
        } catch (Exception exception) {
            graphicView.showErrorMessageDialog("Error sending the message");
        }
    }

    protected void receiveMessageFromCommonChat() {
        while (hasClientConnectedToServer) {
            try {
                Message serverResponse = userConnection.receive();

                if (MessageType.isTypeTextMessage(serverResponse.getMessageType())) {
                    graphicView.addMessageToCommonChat(serverResponse.getMessageText());
                }

                if (MessageType.isTypeNewUserAdded(serverResponse.getMessageType())) {
                    clientModel.addUserToConnectedOnes(serverResponse.getMessageText());
                    graphicView.refreshUsernamesList(clientModel.getConnectedUsernames());
                    graphicView.addMessageToCommonChat("The user " + serverResponse.getMessageText() + " joined to the chat");
                }

                if (MessageType.isTypeUserDeleted(serverResponse.getMessageType())) {
                    clientModel.removeUserFromConnectedOnes(serverResponse.getMessageText());
                    graphicView.refreshUsernamesList(clientModel.getConnectedUsernames());
                    graphicView.addMessageToCommonChat("The user " + serverResponse.getMessageText() + " left from the chat");
                }
            } catch (Exception exception) {
                graphicView.showErrorMessageDialog("Error when receiving a message from the server");
                graphicView.refreshUsernamesList(clientModel.getConnectedUsernames());
                hasClientConnectedToServer = false;
                break;
            }
        }
    }

    protected void disconnectFromServer() {
        try {
            if (hasClientConnectedToServer) {
                userConnection.send(new Message(MessageType.DISCONNECT));
                clientModel.getConnectedUsernames().clear();
                hasClientConnectedToServer = false;
                graphicView.refreshUsernamesList(clientModel.getConnectedUsernames());
            } else {
                graphicView.showErrorMessageDialog("You are already disabled");
            }
        } catch (Exception exception) {
            graphicView.showErrorMessageDialog("Error occurred while disconnecting");
        }
    }
}
