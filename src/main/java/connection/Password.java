package connection;

import java.util.UUID;

public class Password {
    private String passwordValue;

    public Password() {
        passwordValue = UUID.randomUUID().toString();
    }

    public String getValue() {
        return passwordValue;
    }

    public void setValue(String password) {
        this.passwordValue = password;
    }
}
