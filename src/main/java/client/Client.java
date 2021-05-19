package client;

import connection.*;

import java.io.IOException;
import java.net.Socket;

public class Client {
    private UserConnection userConnection;
    private static ClientModel model;
    private static ClientSwingView gui;
    private volatile boolean isConnect = false; //флаг отобаржающий состояние подключения клиента  серверу

    public boolean isConnect() {
        return isConnect;
    }

    public void setConnect(boolean connect) {
        isConnect = connect;
    }

    //точка входа в клиентское приложение
    public static void main(String[] args) {
        Client client = new Client();
        model = new ClientModel();
        gui = new ClientSwingView(client);
        gui.initFrameClient();
        while (true) {
            if (client.isConnect()) {
                client.nameUserRegistration();
                client.receiveMessageFromServer();
                client.setConnect(false);
            }
        }
    }

    //метод подключения клиента  серверу
    protected void connectToServer() {
        //если клиент не подключен  сервере то..
        if (!isConnect) {
            while (true) {
                try {
                    //вызываем окна ввода адреса, порта сервера
                    String addressServer = gui.getServerAddressFromOptionPane();
                    int port = gui.getPortServerFromOptionPane();
                    //создаем сокет и объект connection
                    Socket socket = new Socket(addressServer, port);
                    userConnection = new UserConnection(socket);
                    isConnect = true;
                    gui.addMessage("Сервисное сообщение: Вы подключились к серверу.\n");
                    break;
                } catch (Exception e) {
                    gui.errorDialogWindow("Произошла ошибка! Возможно Вы ввели не верный адрес сервера или порт. Попробуйте еще раз");
                    break;
                }
            }
        } else gui.errorDialogWindow("Вы уже подключены!");
    }

    //метод, реализующий регистрацию имени пользователя со стороны клиентского приложения
    protected void nameUserRegistration() {
        while (true) {
            try {
                Message message = userConnection.receive();
                //приняли от сервера сообщение, если это запрос имени, то вызываем окна ввода имени, отправляем на сервер имя
                if (message.getMessageType() == MessageType.REQUEST_USERNAME) {
                    String nameUser = gui.getNameUser();
                    userConnection.send(new Message(MessageType.USER_NAME, nameUser));
                }
                //если сообщение - имя уже используется, выводим соответствующее оуно с ошибой, повторяем ввод имени
                if (message.getMessageType() == MessageType.NAME_USED) {
                    gui.errorDialogWindow("Данное имя уже используется, введите другое");
                    String nameUser = gui.getNameUser();
                    userConnection.send(new Message(MessageType.USER_NAME, nameUser));
                }
                //если имя принято, получаем множество всех подключившихся пользователей, выходим из цикла
                if (message.getMessageType() == MessageType.NAME_ACCEPTED) {
                    gui.addMessage("Сервисное сообщение: ваше имя принято!\n");
                    model.setConnectedUsernames(message.getConnectedUsernames());
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                gui.errorDialogWindow("Произошла ошибка при регистрации имени. Попробуйте переподключиться");
                try {
                    userConnection.close();
                    isConnect = false;
                    break;
                } catch (IOException ex) {
                    gui.errorDialogWindow("Ошибка при закрытии соединения");
                }
            }

        }
    }

    //метод отправки сообщения предназначенного для других пользователей на сервер
    protected void sendMessageOnServer(String text) {
        try {
            userConnection.send(new Message(MessageType.TEXT_MESSAGE, text));
        } catch (Exception e) {
            gui.errorDialogWindow("Ошибка при отправки сообщения");
        }
    }

    //метод принимающий с сервера собщение от других клиентов
    protected void receiveMessageFromServer() {
        while (isConnect) {
            try {
                Message message = userConnection.receive();
                //если тип TEXT_MESSAGE, то добавляем текст сообщения в окно переписки
                if (message.getMessageType() == MessageType.TEXT_MESSAGE) {
                    gui.addMessage(message.getMessageText());
                }
                //если сообщение с типо USER_ADDED добавляем сообщение в окно переписки о новом пользователе
                if (message.getMessageType() == MessageType.USER_ADDED) {
                    model.addUser(message.getMessageText());
                    gui.refreshListUsers(model.getConnectedUsernames());
                    gui.addMessage(String.format("Сервисное сообщение: пользователь %s присоединился к чату.\n", message.getMessageText()));
                }
                //аналогично для отключения других пользователей
                if (message.getMessageType() == MessageType.REMOVED_USER) {
                    model.removeUser(message.getMessageText());
                    gui.refreshListUsers(model.getConnectedUsernames());
                    gui.addMessage(String.format("Сервисное сообщение: пользователь %s покинул чат.\n", message.getMessageText()));
                }
            } catch (Exception e) {
                gui.errorDialogWindow("Ошибка при приеме сообщения от сервера.");
                setConnect(false);
                gui.refreshListUsers(model.getConnectedUsernames());
                break;
            }
        }
    }

    //метод реализующий отключение нашего клиента от чата
    protected void disableClient() {
        try {
            if (isConnect) {
                userConnection.send(new Message(MessageType.DISABLE_USER));
                model.getConnectedUsernames().clear();
                isConnect = false;
                gui.refreshListUsers(model.getConnectedUsernames());
            } else gui.errorDialogWindow("Вы уже отключены.");
        } catch (Exception e) {
            gui.errorDialogWindow("Сервисное сообщение: произошла ошибка при отключении.");
        }
    }
}
