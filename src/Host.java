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

        // TODO the allowed ip's from config file
        Thread acceptThread = new Thread(() -> {
            while (connectedClients.get() < 2) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String clientIP = clientSocket.getInetAddress().getHostAddress();
                    System.out.println("New client connected");

                    // Validate client IP and handle the connection
                    if (validateClientIP(clientIP, allowedClient1IP, allowedClient2IP)) {
                        System.out.println("Accepted connection from client IP: " + clientIP);
                        ClientHandler clientHandler = new ClientHandler(clientSocket);
                        synchronized (clientsLock) {
                            clients.add(clientHandler);
                        }
                        clientHandler.start();
                        connectedClients.incrementAndGet();
                    } else {
                        System.out.println("Rejected connection from client IP: " + clientIP);
                        // Optionally, you can close the socket for rejected connections
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        acceptThread.start();
        boolean statementPrinted = false;
        while(true){
            if(!statementPrinted && connectedClients.get() > 1) {
                System.out.println("end of loop");
                System.out.println(clients);
                updateClients("Welcome, gamers! The game is starting!");
                statementPrinted = true;
            }

            try {
                Thread.sleep(1000); // Add a delay to avoid busy waiting
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }

    public static void startServer(int port) {
        try{
            //TODO: custom port. For now its plugged in
            serverSocket = new ServerSocket(port);
            System.out.println("Server started listening on port 6869");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean validateClientIP(String clientIP, String allowedClient1IP, String allowedClient2IP) {
        return clientIP.equals(allowedClient1IP) || clientIP.equals(allowedClient2IP);
    }

    public static class ClientHandler extends Thread {
        private Socket clientSocket;
        private BufferedReader reader;
        private PrintWriter writer;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
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
                    // Process client messages

                    // Check if the message is a command/event
                    if (clientMessage.equals(GAME_STARTED_RESPONSE)) {
                        if (!clients.isEmpty()) {
                            serverGUI.updateLog("Received initiation from the clients.");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // Close resources if needed
            }
        }

        public void sendMessage(String message) {
            if (writer != null) {
                writer.println(message);
            }
        }
    }


}

class ServerGUI extends JFrame {
    private final JButton sendMessageButton;
    private final JTextArea textArea;
    private List<Host.ClientHandler> clients;

    public ServerGUI(List<Host.ClientHandler> clients) {
        // Initialize frame properties
        setTitle("Server GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);

        // Create components
        JPanel panel = new JPanel();
        sendMessageButton = new JButton("Send Message to Clients");
        textArea = new JTextArea(10, 30);
        textArea.setEditable(false);

        // Add components to panel
        panel.add(sendMessageButton);
        panel.add(new JScrollPane(textArea));

        getContentPane().add(panel);

        // Make the GUI visible
        setVisible(true);

        // Add ActionListener to the button
        sendMessageButton.addActionListener(e -> {
            String command = "COMMAND:START_GAME";
            Host.updateClients(command);
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
