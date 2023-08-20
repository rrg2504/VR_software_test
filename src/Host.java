import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Host {
    private static ServerSocket serverSocket;
    private static int counter = 0;
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static AtomicInteger connectedClients = new AtomicInteger(0);
    private static final Object clientsLock = new Object();
    private static ServerGUI serverGUI;

    private static final String GAME_STARTED_RESPONSE = "INITIATED";

    private static final String GAME_ALREADY_STARTED_RESPONSE = "ALREADY_STARTED";

    private static final String GAME_HALTED_RESPONSE = "HALTED";

    private static final String NO_GAME_IS_PLAYING ="NO_GAME_PLAYING";
    private static final String FILE_NAME = "config/host_config.properties";

    public static void updateClients(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }
    public static void main(String[] args){

        Properties config = new Properties();
        try (FileInputStream inputStream = new FileInputStream(FILE_NAME)){
            config.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        String allowedClient1IP = config.getProperty("allowedClient1IP");
        String allowedClient2IP = config.getProperty("allowedClient2IP");
        int serverPort = Integer.parseInt(config.getProperty("serverPort")); //TODO Global var

        startServer(serverPort);

        // TODO I can do the configuration file here
        // Create an instance of ServerGUI
        serverGUI = new ServerGUI(clients);
        serverGUI.updateLog("Server started listening on port: " + serverPort);
        // TODO the allowed ip's from config file
        Thread acceptThread = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String clientIP = clientSocket.getInetAddress().getHostAddress();

                    serverGUI.updateLog("New client connected");

                    // Validate client IP and handle the connection
                    if (validateClientIP(clientIP, allowedClient1IP, allowedClient2IP)) {
                        serverGUI.updateLog("Accepted connection from client IP: " + clientIP);
                        ClientHandler clientHandler = new ClientHandler(clientSocket,clientIP);
                        synchronized (clientsLock) {
                            clients.add(clientHandler);
                        }
                        clientHandler.start();
                        connectedClients.incrementAndGet(); //TODO
                    } else {
                        serverGUI.updateLog("Rejected connection from client IP: " + clientIP);
                        // Optionally, you can close the socket for rejected connections
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        acceptThread.start();
//        boolean statementPrinted = false;
//        while(true){
//            if(!statementPrinted && connectedClients.get() > 1) {
//                System.out.println("end of loop");
//                System.out.println(clients);
//                updateClients("Welcome, gamers! The game is starting!");
//                statementPrinted = true;
//            }
//
//            try {
//                Thread.sleep(1000); // Add a delay to avoid busy waiting
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }


    }

    public static void startServer(int port) {
        try{
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean validateClientIP(String clientIP, String allowedClient1IP, String allowedClient2IP) {
        return clientIP.equals(allowedClient1IP) || clientIP.equals(allowedClient2IP);
    }

    public static class ClientHandler extends Thread {
        private final Socket clientSocket;
        private BufferedReader reader;
        private PrintWriter writer;

        private final String clientIP;

        public ClientHandler(Socket socket, String clientIP) {
            this.clientSocket = socket;
            this.clientIP = clientIP;
            try {
                InputStream inputStream = clientSocket.getInputStream();
                OutputStream outputStream = clientSocket.getOutputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream));
                writer = new PrintWriter(new OutputStreamWriter(outputStream), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                String clientMessage;
                while ((clientMessage = reader.readLine()) != null) {

                    // Check if the message is a command/event
                    if (clientMessage.equals(GAME_STARTED_RESPONSE)) {
                        if (!clients.isEmpty()) {
                            serverGUI.updateLog("Received Game Confirmation from client with ip=" + this.clientIP);
                        }
                    }

                    if(clientMessage.equals(GAME_ALREADY_STARTED_RESPONSE)) {
                        if (!clients.isEmpty()) {
                            serverGUI.updateLog("Client ip=" + this.clientIP + " has already started its game");
                        }
                    }

                    if (clientMessage.equals(GAME_HALTED_RESPONSE)) {
                        if (!clients.isEmpty()) {
                            serverGUI.updateLog("Client ip=" + this.clientIP + " has succesfully ended its game");
                        }
                    }

                    if(clientMessage.equals(NO_GAME_IS_PLAYING)) {
                        if (!clients.isEmpty()) {
                            serverGUI.updateLog("Client ip=" + this.clientIP + " Cant halt game since no game is being played");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // Close resources if needed
                serverGUI.updateLog("Client ip=" + this.clientIP + " has disconnected");
                closeResources();
                synchronized (clientsLock) {
                    clients.add(this);
                }
            }
        }

        public void sendMessage(String message) {
            if (writer != null) {
                writer.println(message);
            }
        }

        private void closeResources() {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (writer != null) {
                    writer.close();
                }
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}

/**
 * Created a GUI for the host that allows sending commands to start
 * or halt the game. It also logs any actions that happen
 */
class ServerGUI extends JFrame {

    private final String HALT_GAME_COMMAND = "COMMAND:HALT_GAME";

    private final String START_GAME_COMMAND = "COMMAND:START_GAME";
    private final JTextArea textArea;
    private List<Host.ClientHandler> clients;

    public ServerGUI(List<Host.ClientHandler> clients) {
        this.clients = clients;
        setTitle("Server GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);

        // Create components
        JPanel panel = new JPanel();
        // button for sending message
        JButton sendStartGameMessage = new JButton("Send Start Game Command");
        JButton sendHaltGameMessage = new JButton("Send Halt Game Command");

        textArea = new JTextArea(10, 30);
        textArea.setEditable(false);

        // Add components to panel
        panel.add(sendStartGameMessage);
        panel.add(sendHaltGameMessage);
        panel.add(new JScrollPane(textArea));

        getContentPane().add(panel);

        // Make the GUI visible
        setVisible(true);

        sendStartGameMessage.addActionListener(e -> {
            Host.updateClients(START_GAME_COMMAND);
            if(!clients.isEmpty()){
                updateLog("Sent command to start the game to all clients.");
            }else{
                updateLog("There are no clients currently connected");
            }
        });

        sendHaltGameMessage.addActionListener(e -> {
            Host.updateClients(HALT_GAME_COMMAND);
            if(!clients.isEmpty()){
                updateLog("Sent command to start the game to all clients.");
            }else{
                updateLog("There are no clients currently connected");
            }
        });

    }

    public void updateLog(String message) {
        textArea.append(message + "\n");
    }
}
