import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {
    private static final Accounts acc = new Accounts();

    private static Peer me;
    private static final Map<String, Peer> peers = new HashMap<>();

    private static ServerSocket server;
    private static final Map<String, ConnectionHandler> clients = new HashMap<>();
    private static final Map<String, DataOutputStream> clients_pending_reply = new HashMap<>();

    private static void readConfig(String myBranch, String configPath) {
        try {
            File config = new File(configPath);
            Scanner s = new Scanner(config);
            while(s.hasNextLine()) {
                String[] line = s.nextLine().split("\\s+");
                if (line[0].equals(myBranch)) {
                    me = new Peer(line[0], line[1], Integer.parseInt(line[2]));
                } else {
                    peers.put(line[0], new Peer(line[0], line[1], Integer.parseInt(line[2])));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendToClient (DataOutputStream destOut, String msg) {
        if (msg.isEmpty()) {
            return;
        }
        try {
            destOut.writeUTF(msg);
            destOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendToServer (String destServer, String destClient, String msg, boolean isReply) {
        Peer sendTo = peers.get(destServer);
        if (server == null || sendTo == null || msg.isEmpty()) { return; }
        try {
            if (sendTo.socket == null) {
                sendTo.connect();
            }
            String message;
            if (isReply) {
                message = "REPLY " + destClient + " " + msg;

            } else {
                message = "FROM " + me.branch + " " + destClient + " " + msg;
            }
            System.out.println(message);
            sendTo.out.writeUTF(message);
            sendTo.out.flush();
        } catch (IOException e) {
            System.out.println("Error occured while communicating with server: " + sendTo.branch + "\n");
            e.printStackTrace();
        }
    }

    private static void listen(){
        try{
            server = new ServerSocket(me.port);
            server.setReuseAddress(true);
            System.out.println("Listening...");

            while(true) {
                Socket client = server.accept();
                String hostname = client.getInetAddress().getHostAddress();

                System.out.println("Connected with client " + hostname + " Port: " +
                        client.getPort());
                ConnectionHandler connection = new ConnectionHandler(client);
                clients.put(hostname, connection);
                (new Thread(connection)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (server != null) {
                    server.close();
                }
                if (!clients.isEmpty()) {
                    clients.clear();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class CommandHandler implements Runnable {
        private final String original;
        private String[] command;
        private final DataOutputStream out;
        private final String client;
        private final String fromServer;
        private final boolean isReply;

        public CommandHandler(String command, DataOutputStream out) {
            this.original = command;
            this.command = command.split("\\s+");
            this.out = out;
            // Reply from other servers received
            if (this.command[0].equals("REPLY")) {
                this.client = this.command[1];
                this.fromServer = "";
                this.isReply = true;
            } else if (this.command[0].equals("FROM")) { // Command from other servers received
                this.fromServer = this.command[1];
                this.client = this.command[2];
                this.command = Arrays.copyOfRange(this.command, 3, this.command.length + 1);
                this.isReply = false;
            } else { // Command from client received
                this.fromServer = "";
                this.client = this.command[0];
                this.command = Arrays.copyOfRange(this.command, 1, this.command.length + 1);
                this.isReply = false;
            }
        }

        @Override
        public void run() {
            // Reply
            if (this.isReply) {
                StringBuilder sb = new StringBuilder();
                for (int i = 2; i < command.length; i++) {
                    sb.append(command[i]);
                    sb.append(" ");
                }
                sendToClient(clients_pending_reply.get(client), sb.toString());
                clients_pending_reply.remove(client);
                return;
            }
            // Commands only from Client: BEGIN / COMMIT
            if (command[0].equals("BEGIN")) {
                sendToClient(out, "OK");
                return;
            }
            if (command[0].equals("COMMIT")) {

                // broadcast Precommit to all servers
                // if all good to commit received, broadcast commit
                // if received fail to commit, broadcast abort, delete all previous transactions of the client
                // rerun the current client, if some client's transaction won't met concurrency, abort that too



                sendToClient(out, "COMMIT");
                return;
            }
            if (command[0].equals("ABORT")) {
                acc.abort(client);
                return;
            }
            // other commands
            String[] destAccount = command[1].split("\\.");
            if (!destAccount[0].equals(me.branch)) {
                sendToServer(destAccount[0], "", original, false);
                clients_pending_reply.put(client, out);
            } else {
                Transaction tx;
                if (command[0].equals("BALANCE")) {
                    tx = new Transaction(client, "BALANCE", destAccount[1], 0);
                } else {
                    tx = new Transaction(client, command[0], destAccount[1], Integer.parseInt(command[2]));
                }
                String output = null;
                try {
                    output = acc.execute(tx);
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
                if (output.equals("ABORTED") || output.equals("NOT FOUND, ABORTED")) {
                    // Server abort the tx within the server, send abort command to peers to abort the same tx
                    acc.abort(client);
                    for (Peer p: peers.values()) {
                        sendToServer(p.branch, client, "ABORT", false);
                    }
                }
                if (fromServer.isEmpty()) {
                    // send back to client
                    sendToClient(out, output);
                } else {
                    sendToServer(fromServer, client, output, true);
                }
            }
        }
    }


    private static class ConnectionHandler implements Runnable{
        private final Socket client;
        private final boolean killed;

        public ConnectionHandler(Socket s) {
            this.client = s;
            this.killed = false;
        }
        public void run() {
            try{
                DataInputStream in = new DataInputStream(client.getInputStream());
                DataOutputStream out = new DataOutputStream(client.getOutputStream());
                String command;
                while(true) {
                    if (this.killed) {
                        System.out.println("KILL");
                        client.close();
                        System.out.println("Client is disconnected.");
                        return;
                    }
                    command = in.readUTF();
                    if (!command.isEmpty()) {
                        System.out.println(command);
                        new Thread(new CommandHandler(command, out)).start();
                    }
                }
            } catch (IOException e) {
                try {
                    this.client.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
    }
    
    public static void main(String[] args) {
        String branch = args[0];
        readConfig(branch, args[1]);
        System.out.println("Server: " + branch + " Host: " + me.host + " Port: " + me.port + " is up and running.");
        listen();
    }
}
