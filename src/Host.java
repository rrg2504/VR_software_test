import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Host {
    private static ServerSocket serverSocket;
    private static int counter = 0;
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static AtomicInteger connectedClients = new AtomicInteger(0);

    public static void updateClients(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }
    public static void main(String[] args){
        int port = 6868;

        startServer();

        // TODO I can do the configuration file here

        // TODO the allowed ip's from config file
        String allowedClient1IP = "192.168.1.218";
        String allowedClient2IP = "192.168.1.219";
        while (connectedClients.get() < 2) {
            System.out.println(connectedClients.get());
            try{
                Socket clientSocket = serverSocket.accept();
                String clientIP = clientSocket.getInetAddress().getHostAddress();
                System.out.println("New client connected");

                // Validate client IP
                if (validateClientIP(clientIP, allowedClient1IP, allowedClient2IP)) {
                    System.out.println("Accepted connection from client IP: " + clientIP);
                    // Handle the client connection here (send/receive data, etc.)
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    clients.add(clientHandler);
                    clientHandler.start();

                    // Increment the connected clients counter
                    connectedClients.incrementAndGet();
                    counter++;
                    System.out.println(counter);
                    System.out.println("Connected clients: " + connectedClients.get());
                } else {
                    System.out.println("Rejected connection from client IP: " + clientIP);
                    // Optionally, you can close the socket for rejected connections
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        System.out.println("end of loop");
        updateClients("Welcome, gamers! The game is starting!");

    }

    public static void startServer() {
        try{
            //TODO: custom port. For now its plugged in
            serverSocket = new ServerSocket(6868);
            System.out.println("Server started listening on port 6868");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean validateClientIP(String clientIP, String allowedClient1IP, String allowedClient2IP) {
        return clientIP.equals(allowedClient1IP) || clientIP.equals(allowedClient2IP);
    }

    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter writer;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (
                    InputStream inputStream = clientSocket.getInputStream();
                    OutputStream outputStream = clientSocket.getOutputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))
            ) {
                writer = new PrintWriter(new OutputStreamWriter(outputStream), true);

                // Perform communication with the client here
                // For example, read/write data using reader and writer

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                connectedClients.decrementAndGet();
                clients.remove(this); // Remove from the list of active clients
            }
        }

        public void sendMessage(String message) {
            if (writer != null) {
                writer.println(message);
            }
        }
    }
}
