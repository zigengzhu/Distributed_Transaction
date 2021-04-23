import java.io.*;
import java.io.FileNotFoundException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Server {
    private final Accounts acc = new Accounts();

    private static Peer me;
    private static final ArrayList<Peer> peers = new ArrayList<>();

    private static ServerSocket server;

    private static void readConfig(String my_branch, String config_path) {
        try {
            File config = new File(config_path);
            Scanner s = new Scanner(config);
            while(s.hasNextLine()) {
                String[] line = s.nextLine().split("\\s+");
                if (line[0].equals(my_branch)) {
                    me = new Peer(line[0], line[1], Integer.parseInt(line[2]));
                } else {
                    peers.add(new Peer(line[0], line[1], Integer.parseInt(line[2])));
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found.");
            e.printStackTrace();
        }
    }

    private static void listen() throws IOException {
        try{
            if (me.isSet()) {
                server = new ServerSocket(me.getPort());
                server.setReuseAddress(true);
                System.out.println("Listening...");
            }
            while(true) {
                Socket client = server.accept();
                System.out.println("Connected with client " + client.getInetAddress().getHostAddress() + " Port: " +
                        client.getPort());
                ConnectionHandler connection = new ConnectionHandler(client);
                new Thread(connection).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (server != null) {
                    server.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class ConnectionHandler implements Runnable{
        private final Socket client;
        private DataInputStream in;
        private DataOutputStream out;

        public ConnectionHandler(Socket s) {
            this.client = s;
        }
        public void run() {
            try{
                in = new DataInputStream(client.getInputStream());
                out = new DataOutputStream(client.getOutputStream());
                String msg;
                while((msg = in.readUTF()) != null) {
                    System.out.println(msg);
                    if (msg.equals("BEGIN")) {
                        out.writeUTF("OK");
                        out.flush();
                    }
                }
                System.out.println("listen eneded");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (in != null) { in.close(); }
                    if (out != null) { out.close(); }
                    if (client != null) { client.close(); }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String branch = args[0];
        readConfig(branch, args[1]);
        System.out.println("Server: " + branch + " Host: " + me.getHost() + " Port: " + me.getPort() + " is up and running.");
        listen();

    }
}
