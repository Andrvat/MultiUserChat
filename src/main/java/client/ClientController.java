package client;

import connection.*;
import org.apache.commons.validator.routines.InetAddressValidator;
import utilities.FormatMessagesBuilder;

import javax.naming.InvalidNameException;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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

                createConnectionToServer(serverAddress, port);
                hasClientConnectedToServer = true;
                graphicView.addMessageToCommonChat(FormatMessagesBuilder.buildChatTextAreaServiceMessage(
                        "You have connected to the server"));
            } catch (InvalidNameException ignored) {
                ignored.printStackTrace();
            } catch (Exception exception) {
                graphicView.showErrorMessageDialog(
                        "An error has occurred! " +
                                "You may have entered the wrong server inet address or port. Try again");
            }
        } else {
            graphicView.showErrorMessageDialog("You are already connected!");
        }
    }

    private void createConnectionToServer(String serverAddress, int serverPort) throws IOException {
        if (isValidServerIPv4Address(serverAddress) && isValidServerPort(serverPort)) {
            Socket socket = new Socket(serverAddress, serverPort);
            userConnection = new UserConnection(socket);
        } else {
            throw new IOException();
        }
    }

    private boolean isValidServerPort(int port) {
        return 0 <= port && port <= 65535;
    }

    private boolean isValidServerIPv4Address(String address) {
        return InetAddressValidator.getInstance().isValidInet4Address(address) || address.equals("localhost");
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
                    graphicView.showErrorMessageDialog("You entered an incorrect username or password, enter other ones...");
                    continue;
                }

                if (MessageType.isTypeLoginAccepted(serverResponse.getMessageType())) {
                    graphicView.addMessageToCommonChat(FormatMessagesBuilder.buildChatTextAreaServiceMessage(
                            "Your name is accepted! Welcome to common chat!"));
                    clientModel.setConnectedUsernames(serverResponse.getConnectedUsernames());
                    break;
                }

            } catch (InvalidNameException exception) {
                if (hasClientConnectedToServer) {
                    disconnectFromServer();
                }
                break;
            } catch (Exception exception) {
                if (hasClientConnectedToServer) {
                    graphicView.showErrorMessageDialog(
                            "An error occurred while registering. Try reconnecting...");
                    disconnectFromServer();
                }
                break;
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
                    String usernameForAdd = serverResponse.getMessageText();
                    clientModel.addUserToConnectedOnes(usernameForAdd);
                    graphicView.setALlOnlineUsersToConnectedUsernamesList(clientModel.getConnectedUsernames());
                    graphicView.addMessageToCommonChat(FormatMessagesBuilder.buildChatTextAreaServiceMessage(
                            "The user " + usernameForAdd + " joined to the chat"));
                }

                if (MessageType.isTypeUserDeleted(serverResponse.getMessageType())) {
                    String usernameForDelete = serverResponse.getMessageText();
                    clientModel.removeUserFromConnectedOnes(usernameForDelete);
                    graphicView.removeNewUserFromConnectedUsernamesList(usernameForDelete);
                    graphicView.addMessageToCommonChat(FormatMessagesBuilder.buildChatTextAreaServiceMessage(
                            "The user " + usernameForDelete + " left from the chat"));
                }
            } catch (Exception exception) {
                if (hasClientConnectedToServer) {
                    graphicView.showErrorMessageDialog("Error when receiving a message from the server");
                    disconnectFromServer();
                }
                break;
            }
        }
    }

    protected void disconnectFromServer() {
        try {
            if (hasClientConnectedToServer) {
                userConnection.send(new Message(MessageType.DISCONNECT));
                clientModel.getConnectedUsernames().clear();
                graphicView.clearUsernamesList();
                userConnection.close();
                hasClientConnectedToServer = false;
            } else {
                graphicView.showErrorMessageDialog("You are already disabled");
            }
        } catch (Exception exception) {
            graphicView.showErrorMessageDialog("Error occurred while disconnecting");
        }
    }
}
