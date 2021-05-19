package connection;

import java.io.Serializable;
import java.util.Set;

public class Message implements Serializable {
    private final MessageType messageType;
    private final String messageText;
    private final Set<String> connectedUsernames;

    public Message(MessageType messageType, String messageText) {
        this.messageText = messageText;
        this.messageType = messageType;
        this.connectedUsernames = null;
    }

    public Message(MessageType messageType, Set<String> connectedUsernames) {
        this.messageType = messageType;
        this.messageText = null;
        this.connectedUsernames = connectedUsernames;
    }

    public Message(MessageType messageType) {
        this.messageType = messageType;
        this.messageText = null;
        this.connectedUsernames = null;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public Set<String> getConnectedUsernames() {
        return connectedUsernames;
    }

    public String getMessageText() {
        return messageText;
    }

}
