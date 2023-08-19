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
                    } catch (IOException e) {
                        //close everything
                    }
                }
            }
        }).start();
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

//        String hostname = "192.168.1.127";
//        int port = 6869;
//
//        try (Socket socket = new Socket(hostname, port)) {
//
//            InputStream input = socket.getInputStream();
//            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
//            // Start a thread to read messages from the server
//            Thread readThread = new Thread(() -> {
//                try {
//                    String serverMessage;
//                    while (true) {
//                        serverMessage = reader.readLine();
//                        if (serverMessage == null) {
//                            System.out.println("Server has closed the connection.");
//                            break;
//                        }
//                        System.out.println("Received message from server: " + serverMessage);
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            });
//            readThread.start();
////            while (true) {
////                String serverMessage = reader.readLine();
////                if (serverMessage != null && serverMessage.equals("END")) {
////                    break; // Exit the loop if server disconnects
////                }
////                if(serverMessage != null){
////                    System.out.println("Received message from server: " + serverMessage);
////                }
////            }
//
//        } catch (UnknownHostException ex) {
//
//            System.out.println("Server not found: " + ex.getMessage());
//
//        } catch (IOException ex) {
//
//            System.out.println("I/O error: " + ex.getMessage());
//        }
    }
}
