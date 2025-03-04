import java.io.IOException;
import java.net.Socket;
import java.util.logging.SocketHandler;

public class CustomSocket{

    private Socket socket;

    // Create a SocketHandler to connect to the server this will be changed to allow users to select their VPS
    SocketHandler client = new SocketHandler("127.0.0.1", 8080);

    public CustomSocket() throws IOException {
    
    }
}
