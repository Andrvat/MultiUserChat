package connection;

public enum MessageType {
    REQUEST_USERNAME,
    TEXT_MESSAGE,
    NAME_ACCEPTED,
    USER_NAME,
    NAME_USED,
    USER_ADDED,
    DISABLE_USER,
    REMOVED_USER,
    NOTIFY_ADD,
    NOTIFY_REMOVE;

    public static boolean isTypeUsername(MessageType messageType) {
        return messageType == USER_NAME;
    }

    public static boolean isTypeTextMessage(MessageType messageType) {
        return messageType == TEXT_MESSAGE;
    }

    public static boolean isTypeDisableUser(MessageType messageType) {
        return messageType == DISABLE_USER;
    }

    public static boolean isTypeNotifyToAdd(MessageType messageType) {
        return messageType == NOTIFY_ADD;
    }

    public static boolean isTypeNotifyToRemove(MessageType messageType) {
        return messageType == NOTIFY_REMOVE;
    }

}