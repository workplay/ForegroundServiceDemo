import java.io.*;
import java.net.*;


public class PCServer {

    public static void main(String[] args) {
        Boolean isLoop = true;
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(9000);

            while (isLoop) {
                Socket socket = serverSocket.accept();

                InputStreamReader streamReader = new InputStreamReader(socket.getInputStream());
                BufferedReader reader = new BufferedReader(streamReader);

                System.out.println("" + reader.readLine());

                socket.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}