import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
    public static void main(String[] args) {
        String hostname = "192.168.1.127";
        int port = 6868;

        try (Socket socket = new Socket(hostname, port)) {

            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            while (true) {
                String serverMessage = reader.readLine();
                if (serverMessage != null && serverMessage.equals("END")) {
                    break; // Exit the loop if server disconnects
                }
                if(serverMessage != null){
                    System.out.println("Received message from server: " + serverMessage);
                }
            }

        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());

        } catch (IOException ex) {

            System.out.println("I/O error: " + ex.getMessage());
        }
    }
}
