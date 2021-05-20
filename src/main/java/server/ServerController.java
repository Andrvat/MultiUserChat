package server;

import connection.*;
import utilities.FormatMessagesBuilder;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class ServerController {
    private ServerSocket serverSocket;
    private ServerSwingView graphicView;
    private ServerModel serverModel;
    private volatile boolean hasServerStarted = false;

    private static final int PASSWORD_EXPIRATION_MILLIS_TIME = 30000;

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
            generateNewSessionPassword();
            new SessionPasswordUpdater().start();
            graphicView.addServiceMessageToServerLogsTextArea(FormatMessagesBuilder.buildMessageWithDateNow(
                    "Server has launched on port " + port));
        } catch (Exception exception) {
            graphicView.addServiceMessageToServerLogsTextArea(FormatMessagesBuilder.buildMessageWithDateNow(
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
            graphicView.addServiceMessageToServerLogsTextArea(FormatMessagesBuilder.buildMessageWithDateNow(finalMessage));
        }
    }

    protected void generateNewSessionPassword() {
        if (hasServerStarted) {
            serverModel.updateCurrentSessionPassword();
            graphicView.addServiceMessageToServerLogsTextArea(FormatMessagesBuilder.buildMessageWithDateNow(
                    "Password for current session: " + serverModel.getCurrentSessionPassword()));
        } else {
            graphicView.addServiceMessageToServerLogsTextArea(FormatMessagesBuilder.buildMessageWithDateNow(
                    "Invalid operation. Server is not running yet"));
        }
    }

    protected String getCurrentSessionPassword() throws ConnectException {
        if (hasServerStarted) {
            return serverModel.getCurrentSessionPassword();
        } else {
            graphicView.addServiceMessageToServerLogsTextArea(FormatMessagesBuilder.buildMessageWithDateNow(
                    "Invalid operation. Server is not running yet"));
            throw new ConnectException();
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
                graphicView.addServiceMessageToServerLogsTextArea(FormatMessagesBuilder.buildMessageWithDateNow(
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
                graphicView.addServiceMessageToServerLogsTextArea(FormatMessagesBuilder.buildMessageWithDateNow(
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
                    Message responseForUsername = requestUsernameFromNewUser(userConnection);
                    Message responseForPassword = requestCurrentSessionPasswordFromNewUser(userConnection);
                    userRecord = new ChatUserRecord(userConnection, getUsernameFromResponseMessage(responseForUsername));
                    if (MessageType.isTypeNewUsername(responseForUsername.getMessageType())
                            && MessageType.isTypeNewPassword(responseForPassword.getMessageType())
                            && isUsernameAvailableToAdd(userRecord.username())
                            && serverModel.isCurrentSessionPasswordCorrect(responseForPassword.getMessageText())) {
                        addNewUserToServerModel();
                        graphicView.addServiceMessageToServerLogsTextArea(FormatMessagesBuilder.buildMessageWithDateNow(
                                "YEAH"));
                        sendToNewUserAllOnlineUsernamesByConnection(userConnection);
                        sendBroadcastMessage(new Message(MessageType.NEW_USER_ADDED, userRecord.username()));
                        break;
                    } else {
                        userConnection.send(new Message(MessageType.LOGIN_ERROR));
                    }
                } catch (Exception exception) {
                    graphicView.addServiceMessageToServerLogsTextArea(FormatMessagesBuilder.buildMessageWithDateNow(
                            "An error occurred when connecting a new user"));
                }
            }
        }

        private void addNewUserToServerModel() {
            serverModel.addNewUserConnection(userRecord.username(), userRecord.userConnection());
            serverModel.addNewUserMetaInfo(userRecord.username(),
                    UserMetaInfo.builder()
                            .firstConnectionTime(FormatMessagesBuilder.buildDateNow())
                            .username(userRecord.username())
                            .allSentMessagesNumber(0)
                            .lastMessageTime(FormatMessagesBuilder.buildDateNow())
                            .build());
            notifyObservers(new Message(MessageType.NOTIFY_ADD, userRecord.username()));
        }

        private Message requestUsernameFromNewUser(UserConnection userConnection) throws IOException {
            userConnection.send(new Message(MessageType.REQUEST_USERNAME));
            return userConnection.receive();
        }

        private Message requestCurrentSessionPasswordFromNewUser(UserConnection userConnection) throws IOException {
            userConnection.send(new Message(MessageType.REQUEST_PASSWORD));
            return userConnection.receive();
        }

        private String getUsernameFromResponseMessage(Message responseMessage) {
            return responseMessage.getMessageText();
        }

        private boolean isUsernameAvailableToAdd(String username) {
            return username != null && !username.isEmpty() && !serverModel.getOnlineUsersConnections().containsKey(username);
        }

        private void sendToNewUserAllOnlineUsernamesByConnection(UserConnection userConnection) {
            Set<String> listUsers = new HashSet<>(serverModel.getOnlineUsersConnections().keySet());
            userConnection.send(new Message(MessageType.LOGIN_ACCEPTED, listUsers));
        }

        private void startMessagingBetweenUsers() {
            while (true) {
                try {
                    Message messageFromUser = userRecord.userConnection().receive();

                    if (MessageType.isTypeTextMessage(messageFromUser.getMessageType())) {
                        sendMessageFromUserToEveryone(messageFromUser);
                    }

                    if (MessageType.isTypeDisconnect(messageFromUser.getMessageType())) {
                        disableExistedUserFromChat();
                        break;
                    }
                } catch (Exception exception) {
                    graphicView.addServiceMessageToServerLogsTextArea(FormatMessagesBuilder.buildMessageWithDateNow(
                            "An error occurred when sending a message from user " + userRecord.username() + " with address " + userSocket.getRemoteSocketAddress()));
                    removeUserFromServerModel();
                    break;
                }
            }
        }

        private void sendMessageFromUserToEveryone(Message message) {
            String textMessage = FormatMessagesBuilder.buildChatTextAreaUserMessage(userRecord.username(), message.getMessageText());
            sendBroadcastMessage(new Message(MessageType.TEXT_MESSAGE, textMessage));
            serverModel.getUserMetaInfoByUsername(userRecord.username()).updateLastMessageTime();
        }

        private void disableExistedUserFromChat() throws IOException {
            sendBroadcastMessage(new Message(MessageType.USER_DELETED, userRecord.username()));
            removeUserFromServerModel();
            userRecord.userConnection().close();
            graphicView.addServiceMessageToServerLogsTextArea(FormatMessagesBuilder.buildMessageWithDateNow(
                    "The user with remote address " + userSocket.getRemoteSocketAddress() + " has disconnected"));
        }

        private void removeUserFromServerModel() {
            serverModel.removeUserConnectionByUsername(userRecord.username());
            serverModel.removeUserMetaInfoByUsername(userRecord.username());
            notifyObservers(new Message(MessageType.NOTIFY_REMOVE, userRecord.username()));
        }

        @Override
        public void run() {
            graphicView.addServiceMessageToServerLogsTextArea(FormatMessagesBuilder.buildMessageWithDateNow(
                    "A new user connected with a remote socket " + userSocket.getRemoteSocketAddress().toString()));
            try {
                UserConnection userConnection = new UserConnection(userSocket);
                connectNewUser(userConnection);
                startMessagingBetweenUsers();
            } catch (Exception exception) {
                graphicView.addServiceMessageToServerLogsTextArea(FormatMessagesBuilder.buildMessageWithDateNow(
                        "An error occurred when sending a message from a user"));
            }
        }
    }

    private class SessionPasswordUpdater extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(PASSWORD_EXPIRATION_MILLIS_TIME);
                    generateNewSessionPassword();
                } catch (InterruptedException exception) {
                    graphicView.addServiceMessageToServerLogsTextArea(FormatMessagesBuilder.buildMessageWithDateNow(
                            "SessionPasswordUpdater was stopped by interrupt"));
                    break;
                }
            }
        }
    }
}