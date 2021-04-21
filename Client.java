import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {
    private static String id;
    private static final ArrayList<Peer> peers = new ArrayList<>();

    private static void readConfig(String config_path) {
        try {
            File config = new File(config_path);
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

    public static void main(String[] args) throws FileNotFoundException  {
        id = args[0];
        readConfig(args[1]);
    }
}
