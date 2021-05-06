import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Peer {
    final String branch;
    final String host;
    final int    port;
    Socket socket;
    DataInputStream in;
    DataOutputStream out;

    public Peer(String branch, String host, int port) throws IOException {
        this.branch = branch;
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        this.socket = new Socket(this.host, this.port);
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        //System.out.println("Server " + branch + " is connected.");
    }

    public void close() throws IOException {
        if (this.socket != null) {
            this.socket.close();
            this.in = null;
            this.out = null;
            this.socket = null;
        }
    }
}
