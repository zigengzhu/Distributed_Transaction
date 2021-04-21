import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class Server {
    private final Accounts acc = new Accounts();

    private static Peer me;
    private static final ArrayList<Peer> peers = new ArrayList<>();

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

    public static void main(String[] args) throws FileNotFoundException {
        String branch = args[0];
        readConfig(branch, args[1]);
        System.out.println("Server: " + branch + " Host: " + me.getHost() + " Port: " + me.getPort() + " is up and running.");
    }
}
