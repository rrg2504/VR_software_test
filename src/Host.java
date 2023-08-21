import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.UUID;

public class Host {
    private static ServerSocket serverSocket;
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static final Object clientsLock = new Object();
    private static ServerGUI serverGUI;
    private static final String GAME_STARTED_RESPONSE = "INITIATED";

    private static final String GAME_ALREADY_STARTED_RESPONSE = "ALREADY_STARTED";

    private static final String GAME_HALTED_RESPONSE = "HALTED";

    private static final String SERVER_SHUTDOWN = "COMMAND:SERVER_SHUTDOWN";

    private static final String NO_GAME_IS_PLAYING ="NO_GAME_PLAYING";
    private static final String FILE_NAME = "config/host_config.properties";

    /**
     * Sends message to all the connected clients
     * @param message the message being sent
     */
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
        int serverPort = Integer.parseInt(config.getProperty("serverPort"));

        startServer(serverPort);

        // Thread that checks if the server/program has been closed
        // which then sends a command to clients
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (ClientHandler client : clients) {
                client.sendMessage(SERVER_SHUTDOWN);
                client.closeResources();
            }
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        serverGUI = new ServerGUI(clients);
        serverGUI.updateLog("Server started listening on port: " + serverPort);

        try {
            String uniqueIdentifier = generateUniqueIdentifier();

            String serverTimeResponse = getServerTime(uniqueIdentifier);

            // Parse server time from the response
            String serverTime = parseServerTime(serverTimeResponse);

            long serverTimestamp = parseTimestamp(serverTime);
            long localTimestamp = System.currentTimeMillis();
            long timeDifference = serverTimestamp - localTimestamp;

            serverGUI.updateLog("Time Difference between server time and local time(ms): " + timeDifference);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Thread acceptThread = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String clientIP = clientSocket.getInetAddress().getHostAddress();

                    serverGUI.updateLog("New client connected");

                    if (validateClientIP(clientIP, allowedClient1IP, allowedClient2IP)) {
                        serverGUI.updateLog("Accepted connection from client IP: " + clientIP);
                        ClientHandler clientHandler = new ClientHandler(clientSocket,clientIP);
                        synchronized (clientsLock) {
                            clients.add(clientHandler);
                        }
                        clientHandler.start();
                    } else {
                        serverGUI.updateLog("Rejected connection from client IP: " + clientIP);
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        acceptThread.start();
    }

    /**
     * generates a random UUID for the purposes of this project
     * @return a UUID
     */
    private static String generateUniqueIdentifier() {
        return UUID.randomUUID().toString();
    }

    /**
     * sends a post request to server in the URL
     * @param uniqueIdentifier the generated UUID
     * @return response from the server
     * @throws Exception thrown if failed to connect
     */
    private static String getServerTime(String uniqueIdentifier) throws Exception {
        String url = "https://vip.vr360action.com/machines/getServerTime";

        HttpURLConnection connection = null;
        try {
            URL apiUrl = new URL(url);
            connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String jsonInputString = "{\"uuid\": \"" + uniqueIdentifier + "\"}";

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString();
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * parses the server response
     * @param serverTimeResponse the server time response
     * @return the time given by the server
     */
    private static String parseServerTime(String serverTimeResponse) {
        // Parse the server time from the JSON response
        int startIndex = serverTimeResponse.indexOf("\"serverTime\":\"") + "\"serverTime\":\"".length();
        int endIndex = serverTimeResponse.indexOf("\"", startIndex);
        return serverTimeResponse.substring(startIndex, endIndex);
    }

    private static long parseTimestamp(String serverTime) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Date date = sdf.parse(serverTime);
        return date.getTime();
    }

    /**
     * establishes the server/host on the given port
     * @param port the port the server/host will use
     */
    public static void startServer(int port) {
        try{
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * validates the incoming IP address by checking if its equal to the two allowable
     * ip addresses that are set in the Host's config file. Could also update the function
     * to add more than just 2 allowed IPs
     * @param clientIP the IP of the client that is currently connecting
     * @param allowedClient1IP one of the allowed IPs
     * @param allowedClient2IP one of the allowed IPs
     * @return true if it's allowed. False if not
     */
    public static boolean validateClientIP(String clientIP, String allowedClient1IP, String allowedClient2IP) {
        return clientIP.equals(allowedClient1IP) || clientIP.equals(allowedClient2IP);
    }

    /**
     * Class to handle client connections. Allows reading of client messages
     * and sending of server messages
     */
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

        /**
         * thread that listens for incoming client messages
         */
        @Override
        public void run() {
            try {
                String clientMessage;
                while ((clientMessage = reader.readLine()) != null) {
                    if (clientMessage.equals("SERVER_SHUTDOWN")) {
                        // Handle server shutdown
                        closeResources();
                        synchronized (clientsLock) {
                            clients.remove(this);
                        }
                        serverGUI.updateLog("Client ip=" + this.clientIP + " received shutdown signal from server.");
                        break; // Exit the loop
                    }
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
                            serverGUI.updateLog("Client ip=" + this.clientIP + " has successfully ended its game");
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
                serverGUI.updateLog("Client ip=" + this.clientIP + " has disconnected");
                closeResources();
                synchronized (clientsLock) {
                    clients.add(this);
                }
            }
        }

        /**
         * sends a message to a client
         * @param message the message being sent
         */
        public void sendMessage(String message) {
            if (writer != null) {
                writer.println(message);
            }
        }

        /**
         * closes all resources
         */
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

    public ServerGUI(List<Host.ClientHandler> clients) {
        setTitle("Server GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);

        JPanel panel = new JPanel();
        // button for sending message
        JButton sendStartGameMessage = new JButton("Send Start Game Command");
        JButton sendHaltGameMessage = new JButton("Send Halt Game Command");

        textArea = new JTextArea(10, 30);
        textArea.setEditable(false);

        panel.add(sendStartGameMessage);
        panel.add(sendHaltGameMessage);
        panel.add(new JScrollPane(textArea));

        getContentPane().add(panel);

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

    /**
     * shows any messages to the gui log window
     * @param message message that will be displayed in the gui log
     */
    public void updateLog(String message) {
        textArea.append(message + "\n");
    }
}
