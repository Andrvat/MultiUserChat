package connection;

import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;

public class UserConnection implements Closeable {
    private final Socket userSocket;

    private final PrintWriter printWriter;
    private final BufferedReader bufferedReader;

    private final Gson gson = new Gson();

    public UserConnection(Socket userSocket) throws IOException {
        this.userSocket = userSocket;
        this.printWriter = new PrintWriter(new DataOutputStream(userSocket.getOutputStream()));
        this.bufferedReader = new BufferedReader(new InputStreamReader(new DataInputStream(userSocket.getInputStream())));
    }

    public void send(Message message) {
        synchronized (printWriter) {
            String jsonMessage = gson.toJson(message);
            printWriter.write(jsonMessage);
            printWriter.write("\n");
            printWriter.flush();
        }
    }

    public Message receive() throws IOException {
        synchronized (bufferedReader) {
            String jsonMessage = bufferedReader.readLine();
            return gson.fromJson(jsonMessage, Message.class);
        }
    }

    @Override
    public void close() throws IOException {
        userSocket.shutdownOutput();
        userSocket.shutdownInput();
        userSocket.close();
    }
}
