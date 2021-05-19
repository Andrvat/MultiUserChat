package connection;

import java.io.*;
import java.net.Socket;

public class UserConnection implements Closeable {
    private final Socket userSocket;
    private final ObjectOutputStream objectOutputStream;
    private final ObjectInputStream objectInputStream;


    public UserConnection(Socket userSocket) throws IOException {
        this.userSocket = userSocket;
        this.objectOutputStream = new ObjectOutputStream(userSocket.getOutputStream());
        this.objectInputStream = new ObjectInputStream(userSocket.getInputStream());
    }

    public void send(Message message) throws IOException {
        synchronized (this.objectOutputStream) {
            objectOutputStream.writeObject(message);
        }
    }

    public Message receive() throws IOException, ClassNotFoundException {
        synchronized (this.objectInputStream) {
            return (Message) objectInputStream.readObject();
        }
    }

    @Override
    public void close() throws IOException {
        objectInputStream.close();
        objectOutputStream.close();
        userSocket.close();
    }
}
