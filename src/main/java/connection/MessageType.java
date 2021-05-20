package connection;

public enum MessageType {
    REQUEST_USERNAME,
    REQUEST_PASSWORD,
    TEXT_MESSAGE,
    LOGIN_ACCEPTED,
    NEW_USERNAME,
    NEW_PASSWORD,
    LOGIN_ERROR,
    NEW_USER_ADDED,
    DISCONNECT,
    USER_DELETED,
    NOTIFY_ADD,
    NOTIFY_REMOVE;

    public static boolean isTypeNewUsername(MessageType messageType) {
        return messageType == NEW_USERNAME;
    }

    public static boolean isTypeNewPassword(MessageType messageType) {
        return messageType == NEW_PASSWORD;
    }

    public static boolean isTypeRequestUsername(MessageType messageType) {
        return messageType == REQUEST_USERNAME;
    }

    public static boolean isTypeRequestPassword(MessageType messageType) {
        return messageType == REQUEST_PASSWORD;
    }

    public static boolean isTypeTextMessage(MessageType messageType) {
        return messageType == TEXT_MESSAGE;
    }

    public static boolean isTypeDisconnect(MessageType messageType) {
        return messageType == DISCONNECT;
    }

    public static boolean isTypeNotifyToAdd(MessageType messageType) {
        return messageType == NOTIFY_ADD;
    }

    public static boolean isTypeLoginError(MessageType messageType) {
        return messageType == LOGIN_ERROR;
    }

    public static boolean isTypeNotifyToRemove(MessageType messageType) {
        return messageType == NOTIFY_REMOVE;
    }

    public static boolean isTypeLoginAccepted(MessageType messageType) {
        return messageType == LOGIN_ACCEPTED;
    }

    public static boolean isTypeNewUserAdded(MessageType messageType) {
        return messageType == NEW_USER_ADDED;
    }

    public static boolean isTypeUserDeleted(MessageType messageType) {
        return messageType == USER_DELETED;
    }

}