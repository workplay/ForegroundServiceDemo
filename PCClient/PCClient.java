import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class PCClient {

    public static void main(String[] args) throws IOException {
        System.out.println("任意字符, 回车键发送Toast");
        Scanner scanner = new Scanner(System.in);
        
	Socket socket = new Socket("127.0.0.1", 8000);        
	DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        
        while (true) {
            // String msg = scanner.next();
            // if (msg == "exit") {
            //      break;
            // }
            // outputStream.writeUTF(msg);
	    String msg = inputStream.readChar();
            System.out.println(msg);
	}
	//socket.close();
    
    }
}