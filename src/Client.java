import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;

    public Client(Socket socket){
        try{
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            //close everything
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
                        if (serverMessage.startsWith("COMMAND:")) {
                            String command = serverMessage.substring("COMMAND:".length());
                            handleCommand(command); // Implement this method to handle the command
                        }
                    } catch (IOException e) {
                        //close everything
                    }
                }
            }
        }).start();
    }

    private void handleCommand(String command) {
        // Implement this method to perform actions based on the received command
        if (command.equals("START_GAME")) {
            // Start the game or perform relevant action
            System.out.println("Received command to start the game.");
            sendMessage("Initiated");
        }
        // Add more cases for different commands as needed
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
        //Dont have to take user input here. GUI instead before we start anything TODO
        String hostname = "192.168.1.127";
        int port = 6869;
        Socket socket = new Socket(hostname, port);
        Client client = new Client(socket);
        client.listenForMessage();

    }
}
