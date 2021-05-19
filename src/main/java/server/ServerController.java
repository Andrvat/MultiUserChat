package server;

import connection.*;
import utilities.ServeMessagesBuilder;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class ServerController {
    private ServerSocket serverSocket;
    private ServerSwingView graphicView;
    private ServerModel serverModel;
    private volatile boolean hasServerStarted = false;

    private final List<ServerObserver> observers = new ArrayList<>();

    public void launch() {
        while (true) {
            if (hasServerStarted) {
                acceptNewUserConnections();
                hasServerStarted = false;
            }
        }
    }

    public void addObserver(ServerObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(ServerObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(Message message) {
        for (ServerObserver observer : observers) {
            observer.update(message);
        }
    }

    public ServerModel getServerModel() {
        return serverModel;
    }

    public void setGraphicView(ServerSwingView graphicView) {
        this.graphicView = graphicView;
    }

    public void setServerModel(ServerModel serverModel) {
        this.serverModel = serverModel;
    }

    protected void startServerOnPort(int port) throws Exception {
        try {
            serverSocket = new ServerSocket(port);
            hasServerStarted = true;
            graphicView.addServiceMessageToServerLogsTextArea(ServeMessagesBuilder.buildMessageWithDateNow(
                    "Server has launched on port " + port));
        } catch (Exception exception) {
            graphicView.addServiceMessageToServerLogsTextArea(ServeMessagesBuilder.buildMessageWithDateNow(
                    "Couldn't launch the server"));
            throw exception;
        }
    }

    protected void stopServer() {
        String finalMessage = null;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                closeConnectionsWithAllUsers();
                serverSocket.close();
                finalMessage = "Server was stopped";
            } else {
                finalMessage = "Invalid operation. Server is not running yet";
            }
        } catch (Exception exception) {
            finalMessage = "Couldn't stop the server. Try again...";
        } finally {
            graphicView.addServiceMessageToServerLogsTextArea(ServeMessagesBuilder.buildMessageWithDateNow(finalMessage));
        }
    }

    private void closeConnectionsWithAllUsers() throws IOException {
        Map<String, UserConnection> onlineUsersConnections = serverModel.getOnlineUsersConnections();
        for (UserConnection userConnection : onlineUsersConnections.values()) {
            userConnection.close();
        }
        for (String username : onlineUsersConnections.keySet()) {
            notifyObservers(new Message(MessageType.NOTIFY_REMOVE, username));
        }

        serverModel.getOnlineUsersConnections().clear();
        serverModel.getOnlineUsersMetaInfos().clear();
    }

    protected void acceptNewUserConnections() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                new UserConnectionHandler(socket).start();
            } catch (Exception e) {
                graphicView.addServiceMessageToServerLogsTextArea(ServeMessagesBuilder.buildMessageWithDateNow(
                        "Connection to the server is lost"));
                break;
            }
        }
    }

    protected void sendBroadcastMessage(Message message) {
        for (UserConnection userConnection : serverModel.getOnlineUsersConnections().values()) {
            try {
                userConnection.send(message);
            } catch (Exception e) {
                graphicView.addServiceMessageToServerLogsTextArea(ServeMessagesBuilder.buildMessageWithDateNow(
                        "Error sending a message to all users"));
            }
        }
    }

    private class UserConnectionHandler extends Thread {
        private final Socket userSocket;
        private ChatUserRecord userRecord;

        public UserConnectionHandler(Socket userSocket) {
            this.userSocket = userSocket;
        }

        private void connectNewUser(UserConnection userConnection) {
            while (true) {
                try {
                    Message responseMessage = requestUsernameFromNewUser(userConnection);
                    userRecord = new ChatUserRecord(userConnection, getUsernameFromResponseMessage(responseMessage));
                    if (MessageType.isTypeUsername(responseMessage.getMessageType())
                            && isUsernameAvailableToAdd(userRecord.username())) {
                        addNewUserToServerModel();
                        sendToNewUserAllOnlineUsernamesByConnection(userConnection);
                        sendBroadcastMessage(new Message(MessageType.USER_ADDED, userRecord.username()));
                        break;
                    } else {
                        userConnection.send(new Message(MessageType.NAME_USED));
                    }
                } catch (Exception exception) {
                    graphicView.addServiceMessageToServerLogsTextArea(ServeMessagesBuilder.buildMessageWithDateNow(
                            "An error occurred when connecting a new user"));
                }
            }
        }

        private void addNewUserToServerModel() {
            serverModel.addNewUserConnection(userRecord.username(), userRecord.userConnection());
            serverModel.addNewUserMetaInfo(userRecord.username(),
                    UserMetaInfo.builder()
                            .firstConnectionTime(ServeMessagesBuilder.buildDateNow())
                            .username(userRecord.username())
                            .allSentMessagesNumber(0)
                            .lastMessageTime(ServeMessagesBuilder.buildDateNow())
                            .build());
            notifyObservers(new Message(MessageType.NOTIFY_ADD, userRecord.username()));
        }

        private Message requestUsernameFromNewUser(UserConnection userConnection) throws IOException, ClassNotFoundException {
            userConnection.send(new Message(MessageType.REQUEST_USERNAME));
            return userConnection.receive();
        }

        private String getUsernameFromResponseMessage(Message responseMessage) {
            return responseMessage.getMessageText();
        }

        private boolean isUsernameAvailableToAdd(String username) {
            return username != null && !username.isEmpty() && !serverModel.getOnlineUsersConnections().containsKey(username);
        }

        private void sendToNewUserAllOnlineUsernamesByConnection(UserConnection userConnection) throws IOException {
            Set<String> listUsers = new HashSet<>(serverModel.getOnlineUsersConnections().keySet());
            userConnection.send(new Message(MessageType.NAME_ACCEPTED, listUsers));
        }

        private void startMessagingBetweenUsers() {
            while (true) {
                try {
                    Message messageFromUser = userRecord.userConnection().receive();

                    if (MessageType.isTypeTextMessage(messageFromUser.getMessageType())) {
                        sendMessageFromUserToEveryone(messageFromUser);
                    }

                    if (MessageType.isTypeDisableUser(messageFromUser.getMessageType())) {
                        disableExistedUserFromChat();
                        break;
                    }
                } catch (Exception exception) {
                    graphicView.addServiceMessageToServerLogsTextArea(ServeMessagesBuilder.buildMessageWithDateNow(
                            "An error occurred when sending a message from user " + userRecord.username() + " with address " + userSocket.getRemoteSocketAddress()));
                    removeUserFromServerModel();
                    break;
                }
            }
        }

        private void sendMessageFromUserToEveryone(Message message) {
            String textMessage = ServeMessagesBuilder.buildChatTextAreaMessage(userRecord.username(), message.getMessageText());
            sendBroadcastMessage(new Message(MessageType.TEXT_MESSAGE, textMessage));
            serverModel.getUserMetaInfoByUsername(userRecord.username()).updateLastMessageTime();
        }

        private void disableExistedUserFromChat() throws IOException {
            sendBroadcastMessage(new Message(MessageType.REMOVED_USER, userRecord.username()));
            removeUserFromServerModel();
            userRecord.userConnection().close();
            graphicView.addServiceMessageToServerLogsTextArea(ServeMessagesBuilder.buildMessageWithDateNow(
                    "The user with remote address " + userSocket.getRemoteSocketAddress() + " has disconnected"));
        }

        private void removeUserFromServerModel() {
            serverModel.removeUserConnectionByUsername(userRecord.username());
            serverModel.removeUserMetaInfoByUsername(userRecord.username());
            notifyObservers(new Message(MessageType.NOTIFY_REMOVE, userRecord.username()));
        }

        @Override
        public void run() {
            graphicView.addServiceMessageToServerLogsTextArea(ServeMessagesBuilder.buildMessageWithDateNow(
                    "A new user connected with a remote socket " + userSocket.getRemoteSocketAddress().toString()));
            try {
                UserConnection userConnection = new UserConnection(userSocket);
                connectNewUser(userConnection);
                startMessagingBetweenUsers();
            } catch (Exception exception) {
                graphicView.addServiceMessageToServerLogsTextArea(ServeMessagesBuilder.buildMessageWithDateNow(
                        "An error occurred when sending a message from a user"));
            }
        }
    }
}