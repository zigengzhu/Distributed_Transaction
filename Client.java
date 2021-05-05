import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class Client {
    private static String id;
    private static String configPath;
    private static boolean transacting;
    private static final ArrayList<Peer> peers = new ArrayList<>();
    private static int coordinator;

    private static void readConfig() {
        try {
            File config = new File(configPath);
            Scanner s = new Scanner(config);
            while(s.hasNextLine()) {
                String[] line = s.nextLine().split("\\s+");
                Peer peer = new Peer(line[0], line[1], Integer.parseInt(line[2]));
                peers.add(peer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startConnection() throws InterruptedException {
        try{
            readConfig();
            for (Peer p: peers) {
                p.connect();
                new Thread(new MsgReceiver(p)).start();
            }
        } catch (IOException e) {
            new Thread(new TerminateConnection()).start();
            System.out.println("Not all servers are ready, reconnect in 5 seconds");
            Thread.sleep(5000);
            startConnection();
        }
    }

    private static void setRandomCoordinator() {
        if (peers.size() >= 1) {
            int newCoordinator = ThreadLocalRandom.current().nextInt(0, peers.size());
            coordinator = newCoordinator == coordinator ? coordinator : newCoordinator;
            System.out.println("Coordinator: " + peers.get(coordinator).branch);
        } else {
            throw new IllegalStateException();
        }
    }

    private static void send (String msg) {
        if (msg.isEmpty()) return;
        try {
            DataOutputStream out = peers.get(coordinator).out;
            out.writeUTF(id + ' ' + msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class TerminateConnection extends Thread {
        public void run() {
            for (Peer p: peers) {
                try {
                    p.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            peers.clear();
            System.out.println("Connections Terminated");
        }
    }

    private static class MsgReceiver implements Runnable {
        Peer peer;
        public MsgReceiver(Peer peer) {
            this.peer = peer;
        }
        public void run() {
            System.out.println("Start receiving messages from servers...");
            String msg;
            while(true) {
                try {
                    Runtime.getRuntime().addShutdownHook(new TerminateConnection());
                    msg = this.peer.in.readUTF();
                    if (!msg.isEmpty()) {
                        if (msg.equals("ABORTED") || msg.equals("NOT FOUND, ABORTED")) {
                            transacting = false;
                        }
                        System.out.println(msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        peer.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    return;
                }
            }
        }
    }

    private static class InputReceiver implements Runnable {
        public void run() {
            System.out.println("Start receiving inputs...");
            Scanner recv = new Scanner(System.in);
            try {
                while(recv.hasNextLine()) {
                    String command = recv.nextLine();
                    if (!transacting && command.equals("BEGIN")) {
                        transacting = true;
                        setRandomCoordinator();
                        send(command);
                    } else if (transacting) {
                        if (command.equals("COMMIT")) {
                            transacting = false;
                        }
                        send(command);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        id = args[0];
        configPath = args[1];
        transacting = false;
        startConnection();
        new Thread(new InputReceiver()).start();
    }
}
