package connection;

import lombok.Builder;
import utilities.FormatMessagesBuilder;

@Builder
public class UserMetaInfo {
    private final String username;

    private final String firstConnectionTime;

    private String lastMessageTime;

    private int allSentMessagesNumber;

    public void updateLastMessageTime() {
        lastMessageTime = FormatMessagesBuilder.buildDateNow();
        allSentMessagesNumber++;
    }

    @Override
    public String toString() {
        return "Username: " + username + "\n" +
                "First connection time: " + firstConnectionTime + "\n" +
                "Last message time: " + lastMessageTime + "\n" +
                "All sent message number: " + allSentMessagesNumber + "\n";
    }
}
