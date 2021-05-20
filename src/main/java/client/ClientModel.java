package client;

import java.util.HashSet;
import java.util.Set;

public class ClientModel {
    private Set<String> connectedUsernames = new HashSet<>();

    protected Set<String> getConnectedUsernames() {
        return connectedUsernames;
    }

    protected void addUserToConnectedOnes(String username) {
        connectedUsernames.add(username);
    }

    protected void removeUserFromConnectedOnes(String username) {
        connectedUsernames.remove(username);
    }

    protected void setConnectedUsernames(Set<String> connectedUsernames) {
        this.connectedUsernames = connectedUsernames;
    }
}
