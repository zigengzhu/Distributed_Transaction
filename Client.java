import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class Client {
    private static String id;
    private static final ArrayList<Peer> peers = new ArrayList<>();
    private static Peer coordinator;

    private static Socket socket;
    private static InputStream inputStream;
    private static OutputStream outputStream;
    private static DataInputStream in;
    private static DataOutputStream out;

    private static void readConfig(String configPath) {
        try {
            File config = new File(configPath);
            Scanner s = new Scanner(config);
            while(s.hasNextLine()) {
                String[] line = s.nextLine().split("\\s+");
                peers.add(new Peer(line[0], line[1], Integer.parseInt(line[2])));
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found.");
            e.printStackTrace();
        }
    }

    private static void setRandomCoordinator() throws IOException {
        if (peers.size() >= 1) {
            int coordinatorIndex = ThreadLocalRandom.current().nextInt(0, peers.size());
            coordinator =  peers.get(coordinatorIndex);
            try {
                if (in != null) { in.close(); }
                if (out != null) { out.close(); }
                if (inputStream != null) { inputStream.close(); }
                if (outputStream != null) { outputStream.close(); }
                if (socket != null && socket.isConnected()) { socket.close(); }

                socket = new Socket(coordinator.getHost(), coordinator.getPort());
                inputStream = socket.getInputStream();
                in = new DataInputStream(inputStream);
                outputStream = socket.getOutputStream();
                out = new DataOutputStream(outputStream);

            } catch (IOException e) {
                System.out.println("Socket can't be created.");
                e.printStackTrace();
            }
            System.out.println("New Coordinator: " + coordinator.getBranch());
        } else {
            throw new IllegalStateException();
        }
    }

    private static void send (String msg) throws IOException {
        if (msg.isEmpty()) {
            return;
        }
        try { // maybe add a loop to wait for valid output stream
            out.writeUTF(msg);
            out.flush();
        } catch (IOException e) {
            System.out.println("Error sending msg: " + msg);
            e.printStackTrace();
        }
    }

    private static class MsgReceiver implements Runnable {
        @Override
        public void run() {
            System.out.println("Start receiving messages from servers...");
            //while (true) {
                try {
                    while(in.available() > 0) {
                        String msg = in.readUTF();
                        System.out.println(msg);
                    }
                } catch (Exception e) {
                    System.out.println("Error receiving msg.");
                    e.printStackTrace();
                }
            //}
            System.out.println("MsgReceiver Ended.");
        }
    }

    private static class InputReceiver implements Runnable {
        @Override
        public void run() {
            System.out.println("Start receiving inputs...");
            Scanner recv = new Scanner(System.in);
            try {
                while(recv.hasNextLine()) {
                    String command = recv.nextLine();
                    if (command.equals("BEGIN")) {
                        setRandomCoordinator();
                        send(command);
                    }
                }
            } catch (Exception e) {
                System.out.println("InputReceiver Aborted.");
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws FileNotFoundException  {
        id = args[0];
        readConfig(args[1]);

        InputReceiver ir = new InputReceiver();
        ir.run();
        MsgReceiver mr = new MsgReceiver();
        mr.run();


    }
}
