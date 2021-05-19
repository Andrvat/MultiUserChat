package utilities;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ServeMessagesBuilder {
    public static String buildMessageWithDateNow(String message) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(Calendar.getInstance().getTime()) + " | " + message + "\n";
    }

    public static String buildDateNow() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(Calendar.getInstance().getTime());
    }

    public static String buildChatTextAreaMessage(String username, String text) {
        return username + ": " + text + "\n";
    }
}
