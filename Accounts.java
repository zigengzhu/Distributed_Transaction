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

    public String execute(Transaction tx) throws InterruptedException {
        // Assign Timestamp to each transaction
        if (!client_timestamp.containsKey(tx.client)) {
            client_timestamp.put(tx.client, latest_timestamp);
            tx.setTimestamp(latest_timestamp);
            latest_timestamp+=1;
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
                try {
                    Thread.sleep(10);
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }
            return output;
        }
        if (tx.command.equals("ABORT")) {
            abort(tx.client);
        }
        return "";
    }

    public String getBalance(Transaction tx) {
        if (!accounts.containsKey(tx.account)) {
            return "NOT FOUND, ABORTED";
        }
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
                if (!account.tw.containsKey(tx.timestamp)) {
                    account.tw.put(tx.timestamp, tx.amount);
                } else {
                    int combinedTxValue = account.tw.get(tx.timestamp);
                    combinedTxValue += tx.amount;
                    account.tw.put(tx.timestamp, combinedTxValue);
                }
                printAccounts();
                return "OK";
            } else {
                return "ABORTED";
            }
        }
    }

    public String withdraw(Transaction tx) {
        if (!accounts.containsKey(tx.account)) {
            return "NOT FOUND, ABORTED";
        } else {
            Account account = accounts.get(tx.account);
            if (tx.timestamp >= account.maxRTS() && tx.timestamp > account.last_committed) {
                if (!account.tw.containsKey(tx.timestamp)) {
                    account.tw.put(tx.timestamp, -1 * tx.amount);
                } else {
                    int combinedTxValue = account.tw.get(tx.timestamp);
                    combinedTxValue -= tx.amount;
                    account.tw.put(tx.timestamp, combinedTxValue);
                }
                return "OK";
            } else {
                return "ABORTED";
            }
        }
    }

    public String checkCommit(String client) {
        if (client_timestamp.containsKey(client)) {
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
                        return "CANNOTCOMMIT";
                    }
                }
            }
        }
        return "CANCOMMIT";
    }

    public void commit(String client) {
        if (client_timestamp.containsKey(client)) {
            int timestamp = client_timestamp.get(client);
            for (Account acc: accounts.values()) {
                if (!acc.tw.containsKey(timestamp)) {
                    continue;
                }
                acc.balance += acc.tw.get(timestamp);
                acc.last_committed = timestamp;
                acc.tw.remove(timestamp);
                acc.committed_history.put(timestamp, acc.balance);
            }
            client_timestamp.remove(client);
        }
        printAccounts();
    }

    public void abort(String client) {
        if (client_timestamp.containsKey(client)) {
            int timestamp = client_timestamp.get(client);
            for (Account acc: accounts.values()) {
                if (acc.rts.contains(timestamp)) {
                    acc.rts.remove(Integer.valueOf(timestamp));
                }
                acc.tw.remove(timestamp);
            }
            client_timestamp.remove(client);
        }
    }

    public void printAccounts() {
        this.accounts.forEach((k,v)-> System.out.println("Account: "+k+", Balance: "+v.balance));
    }
}
