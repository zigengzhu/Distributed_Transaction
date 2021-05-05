import java.util.concurrent.ConcurrentHashMap;

public class Accounts {

    final ConcurrentHashMap<String, Account> accounts;
    final ConcurrentHashMap<String, Account> temp;
    final ConcurrentHashMap<String, Integer> client_timestamp;
    int latest_timestamp;

    public Accounts() {
        this.accounts = new ConcurrentHashMap<>();
        this.temp = new ConcurrentHashMap<>();
        this.client_timestamp = new ConcurrentHashMap<>();
        this.latest_timestamp = 1;
    }

    public String execute(Transaction tx) {
        // Assign Timestamp to each transaction
        if (!client_timestamp.containsKey(tx.client)) {
            client_timestamp.put(tx.client, latest_timestamp);
            tx.setTimestamp(latest_timestamp);
            latest_timestamp++;
        } else {
            tx.setTimestamp(client_timestamp.get(tx.client));
        }
        if (tx.command.equals("DEPOSIT")) {
            return desposit(tx);
        }
        if (tx.command.equals("WITHDRAW")) {
            return withdraw(tx);
        }
        if (tx.command.equals("BALANCE")) {
            String output = getBalance(tx);
            while (output.equals("WAIT")) {
                output = getBalance(tx);
            }
            return output;
        }
        return "";
    }

    public String getBalance(Transaction tx) {
        printAccounts();
        Account account = accounts.get(tx.account);
        if (tx.timestamp > account.last_committed) {
            String[] ans = account.getD(tx.timestamp);
            if (ans[0].equals("true")) {
                if (!account.rts.contains(tx.timestamp)) {
                    account.rts.add(tx.timestamp);
                }
                return ans[1];
            } else {
                return "WAIT";
            }
        } else {
            abort(tx.client);
            return "ABORTED";
        }
    }

    public String desposit(Transaction tx) {
        if (!accounts.containsKey(tx.account)) {
            Account account = new Account(0);
            account.tw.put(tx.timestamp, tx.amount);
            accounts.put(tx.account, account);
            printAccounts();
            return "OK";
        } else {
            Account account = accounts.get(tx.account);
            if (tx.timestamp >= account.maxRTS() && tx.timestamp > account.last_committed) {
                int combinedTxValue = account.tw.get(tx.timestamp);
                combinedTxValue += tx.amount;
                account.tw.put(tx.timestamp, combinedTxValue);
                printAccounts();
                return "OK";
            } else {
                abort(tx.client);
                return "ABORTED";
            }
        }
    }

    public String withdraw(Transaction tx) {
        if (!accounts.containsKey(tx.account)) {
            abort(tx.client);
            return "NOT FOUND, ABORTED";
        } else {
            Account account = accounts.get(tx.account);
            if (tx.timestamp >= account.maxRTS() && tx.timestamp > account.last_committed) {
                int combinedTxValue = account.tw.get(tx.timestamp);
                combinedTxValue -= tx.amount;
                account.tw.put(tx.timestamp, combinedTxValue);
                printAccounts();
                return "OK";
            } else {
                abort(tx.client);
                return "ABORTED";
            }
        }
    }

    public String checkCommit(String client) {
        int timestamp = client_timestamp.get(client);
        for (Account acc: accounts.values()) {
            if (acc.tw.containsKey(timestamp)) {
                // Check whether previous uncommitted tx exists, if true, return wait
                for (int tw_timestamp: acc.tw.keySet()) {
                    if (tw_timestamp < timestamp) {
                        return "WAIT";
                    }
                }
                // Check consistency
                if (acc.balance + acc.tw.get(timestamp) < 0) {
                    abort(client);
                    return "ABORTED";
                }
            }
        }
        return "CAN COMMIT";
    }

    public void Commit(String client) {
        int timestamp = client_timestamp.get(client);
        for (Account acc: accounts.values()) {
            acc.balance += acc.tw.get(timestamp);
            acc.last_committed = timestamp;
            acc.tw.remove(timestamp);
            acc.committed_history.put(timestamp, acc.balance);
        }
        printAccounts();
    }

    public void abort(String client) {
        int timestamp = client_timestamp.get(client);
        for (Account acct: accounts.values()) {
            if (acct.rts.contains(timestamp)) {
                acct.rts.remove(timestamp);
            }
            acct.tw.remove(timestamp);
        }
        client_timestamp.remove(client);
    }

    public void printAccounts() {
        this.accounts.forEach((k,v)-> System.out.println("Account: "+k+", Balance: "+v.balance));
    }
}
