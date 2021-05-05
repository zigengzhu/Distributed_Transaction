public class Transaction {
    String client;
    String command;
    String account;
    int amount;
    int timestamp;

    Transaction(String client, String command, String account, int amount) {
        this.client = client;
        this.command = command;
        this.account = account;
        this.amount = amount;
    }

    void setTimestamp(int ts) {
        this.timestamp = ts;
    }
}