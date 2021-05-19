package server;

public class ServerLauncher {
    public static void main(String[] args) {
        ServerController serverController = new ServerController();
        ServerSwingView graphicView = new ServerSwingView(serverController);

        serverController.addObserver(graphicView);
        serverController.setGraphicView(graphicView);
        serverController.setServerModel(new ServerModel());
        serverController.launch();
    }
}
