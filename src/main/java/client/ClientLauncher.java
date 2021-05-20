package client;

public class ClientLauncher {
    public static void main(String[] args) {
        ClientController clientController = new ClientController();
        ClientSwingView graphicView = new ClientSwingView(clientController);

        clientController.setGraphicView(graphicView);
        clientController.setClientModel(new ClientModel());

        clientController.launch();
    }
}
