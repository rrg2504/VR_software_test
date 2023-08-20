import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Properties;

public class Client {
    // Only for the purpose of simulating the game starting
    private boolean gameStarted = false;
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private JTextArea logTextArea; // Reference to the GUI log area
    private static final String SERVER_IP = "serverIP";
    private static final String SERVER_PORT = "serverPort";
    private static final String FILE_NAME = "config/client_config.properties";
    private static final String COMMAND_STARTS_WITH = "COMMAND:";
    private static final String START_GAME_COMMAND = "START_GAME";
    private static final String GAME_STARTED_RESPONSE = "INITIATED";

    private static final String GAME_HALTED_RESPONSE = "HALTED";

    private static final String NO_GAME_IS_PLAYING ="NO_GAME_PLAYING";

    private static final String GAME_ALREADY_STARTED_RESPONSE = "ALREADY_STARTED";

    private static final String HALT_GAME_COMMAND = "HALT_GAME";

    private static final String SERVER_SHUTDOWN = "SERVER_SHUTDOWN";

    private static ClientGUI clientGUI;

    public Client(Socket socket, JTextArea logTextArea){
        this.socket = socket;
        this.logTextArea = logTextArea;

        try{
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            closeEverything(socket,bufferedReader,bufferedWriter);
        }
    }

    public void listenForMessage(){

        new Thread(new Runnable() {
            @Override
            public void run() {
                String serverMessage;

                while(socket.isConnected()){
                    try{
                        serverMessage = bufferedReader.readLine();
                        System.out.println(serverMessage);

                        // Check if the message is a command/event
                        if (serverMessage.startsWith(COMMAND_STARTS_WITH)) {
                            String command = serverMessage.substring(COMMAND_STARTS_WITH.length());
                            handleCommand(command); // Implement this method to handle the command
                        }

                    } catch (IOException e) {
                        closeEverything(socket,bufferedReader,bufferedWriter);
                    }
                }
            }
        }).start();
    }

    private void handleCommand(String command) {
        if (command.equals(START_GAME_COMMAND)) {
            if(!gameStarted){
                clientGUI.updateLog("Received command to start the game.");
                gameStarted = true;
                clientGUI.updateLog("Successfully started the game");
                sendMessage(GAME_STARTED_RESPONSE);
            }else{
                clientGUI.updateLog("Received command to start the game but we already are playing");
                sendMessage(GAME_ALREADY_STARTED_RESPONSE);
            }


        }
        if (command.equals(HALT_GAME_COMMAND)) {
            if(gameStarted){
                clientGUI.updateLog("Received command to halt the game.");
                gameStarted = false;
                clientGUI.updateLog("Successfully ended the game.");
                sendMessage(GAME_HALTED_RESPONSE);
            }else{
                clientGUI.updateLog("Received command to end the game but there is no game currently playing");
                sendMessage(NO_GAME_IS_PLAYING);
            }
        }

        if (command.equals(SERVER_SHUTDOWN)) {
            clientGUI.updateLog("Server is shutting down. Goodbye!");
            closeEverything(socket, bufferedReader, bufferedWriter);
            System.exit(0);
        }
    }



    public void sendMessage(String message) {
        try {
            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter){
        try{
            if (bufferedReader != null){
                bufferedReader.close();
            }
            if (bufferedWriter != null){
                bufferedWriter.close();
            }
            if (socket != null){
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        // Load configuration
        Properties config = new Properties();
        try (FileInputStream inputStream = new FileInputStream(FILE_NAME)) {
            config.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Read configuration values
        String serverIP = config.getProperty(SERVER_IP);
        int serverPort = Integer.parseInt(config.getProperty(SERVER_PORT));
        clientGUI = new ClientGUI();
        clientGUI.updateLog("Attempting to Connect to server with IP=" + serverIP);
        try {
            Socket socket = new Socket(serverIP, serverPort);
            clientGUI.updateLog("Successfully Connected to server!");
            JTextArea logTextArea = new JTextArea(20, 50);
            logTextArea.setEditable(false);
            Client client = new Client(socket, logTextArea);
            client.listenForMessage();
        } catch (IOException e) {
            // Handle connection failure
            clientGUI.updateLog("Failed to connect to the server.");
        }

    }
}

class ClientGUI extends JFrame {
    private final JTextArea textArea;

    public ClientGUI() {
        // Initialize frame properties
        setTitle("Client Log");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);

        // Create components
        JPanel panel = new JPanel();
        textArea = new JTextArea(10, 30);
        textArea.setEditable(false);

        // Add components to panel
        panel.add(new JScrollPane(textArea));

        getContentPane().add(panel);

        // Make the GUI visible
        setVisible(true);

    }

    public void updateLog(String message) {
        textArea.append(message + "\n");
    }

}