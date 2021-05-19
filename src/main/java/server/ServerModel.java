package server;

import connection.UserConnection;
import connection.UserMetaInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ServerModel {
    private final Map<String, UserConnection> onlineUsersConnections = new HashMap<>();
    private final Map<String, UserMetaInfo> onlineUsersMetaInfos = new HashMap<>();

    public synchronized Map<String, UserConnection> getOnlineUsersConnections() {
        return onlineUsersConnections;
    }

    public Map<String, UserMetaInfo> getOnlineUsersMetaInfos() {
        return onlineUsersMetaInfos;
    }

    public synchronized void addNewUserConnection(String username, UserConnection userConnection) {
        onlineUsersConnections.put(username, userConnection);
    }

    public synchronized void removeUserConnectionByUsername(String username) {
        onlineUsersConnections.remove(username);
    }

    public synchronized void addNewUserMetaInfo(String username, UserMetaInfo metaInfo) {
        onlineUsersMetaInfos.put(username, metaInfo);
    }

    public synchronized void removeUserMetaInfoByUsername(String username) {
        onlineUsersMetaInfos.remove(username);
    }

    public synchronized UserMetaInfo getUserMetaInfoByUsername(String username) {
        return onlineUsersMetaInfos.get(username);
    }
}