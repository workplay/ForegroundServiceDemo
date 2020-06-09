import java.io.*;
import java.net.*;


public class PCServer {

    public static void main(String[] args) {
        Boolean isLoop = true;
        ServerSocket serverSocket = null;
        Socket socket = null;

        try {
            serverSocket = new ServerSocket(8000);
            socket = serverSocket.accept();

            System.out.println("Client Connected.");

            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());

            while (isLoop) {
                Byte msg = inputStream.readByte();
                System.out.print(msg);
                if (msg == null) {
                    break;
                }
            }
            socket.close();
        } catch (Exception e) {

        }


    }

}