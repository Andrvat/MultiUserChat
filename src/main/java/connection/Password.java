package connection;

import java.util.UUID;

public class Password {
    private String password;

    public Password() {
        password = UUID.randomUUID().toString();
    }

    public String getValue() {
        return password;
    }

    public void setValue(String password) {
        this.password = password;
    }
}
