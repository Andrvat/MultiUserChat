package client;

import java.util.HashSet;
import java.util.Set;

public class ClientModel {
    //в модели лиентского приложения хранится множетство подключившихся пользователей
    private Set<String> connectedUsernames = new HashSet<>();

    protected Set<String> getConnectedUsernames() {
        return connectedUsernames;
    }

    protected void addUser(String nameUser) {
        connectedUsernames.add(nameUser);
    }

    protected void removeUser(String nameUser) {
        connectedUsernames.remove(nameUser);
    }

    protected void setConnectedUsernames(Set<String> connectedUsernames) {
        this.connectedUsernames = connectedUsernames;
    }
}
