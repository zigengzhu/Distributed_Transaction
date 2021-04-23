public class Peer {
    private final String branch;
    private final String host;
    private final int    port;

    public Peer(String branch, String host, int port) {
        this.branch = branch;
        this.host = host;
        this.port = port;
    }

    public String getBranch() { return this.branch; }

    public String getHost() { return this.host; }

    public boolean isSet() { return this.branch != null && this.host != null && this.port != 0; }

    public int getPort() { return this.port; }
}
