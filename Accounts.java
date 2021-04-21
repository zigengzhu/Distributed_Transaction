import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Accounts {
    private boolean transacting;
    private final ConcurrentHashMap<String, Account> accounts;
    private final ConcurrentHashMap<String, Account> temp;

    public Accounts() {
        this.transacting = false;
        this.accounts = new ConcurrentHashMap<>();
        this.temp = new ConcurrentHashMap<>();
    }

    public void begin() {
        this.temp.clear();
        this.temp.putAll(this.accounts);
        this.transacting = true;
    }

    public boolean commit() {
        this.transacting = false;
        if (isConsistent()) {
            this.accounts.putAll(temp);
            this.temp.clear();
            return true;
        } else {
            this.temp.clear();
            return false;
        }
    }

    public void desposit(String name, int amount) {
        if (!this.transacting) {
            return;
        }
        if (!this.accounts.containsKey(name)) {
            Account account = new Account(amount);
            this.accounts.put(name, account);
        } else {
            this.accounts.get(name).deposit(amount);
        }
    }

    public boolean withdraw(String name, int amount) {
        if (!this.transacting || !this.accounts.containsKey(name)) {
            return false;
        } else {
            this.accounts.get(name).withdraw(amount);
            return true;
        }
    }

    public int getBalance(String name) {
        if (!this.transacting || !this.accounts.containsKey(name)) {
            return -1;
        } else {
            return this.accounts.get(name).getBalance();
        }
    }

    public void printAccounts() {
        System.out.println(this.accounts);
    }

    public void printTemp() {
        System.out.println(this.temp);
    }

    private boolean isConsistent() {
        // Check that accounts in this.temp include that of this.accounts
        boolean nameCheck = this.temp.keySet().containsAll(this.accounts.keySet());
        // Check that all account balances are consistent (multithreading)
        AtomicBoolean balanceCheck = new AtomicBoolean(true);
        this.temp.forEachValue(Long.MAX_VALUE, account -> {
            if (account.balance < 0 || account.balance > 100000000) {
                balanceCheck.set(false);
            }
        });
        return nameCheck && balanceCheck.get();
    }

    private class Account {
        private int balance;

        private Account(int balance) {
            this.balance = balance;
        }

        private int getBalance() {
            return this.balance;
        }

        private void deposit(int amount) {
            this.balance += amount;
        }

        private void withdraw(int amount) {
            this.balance -= amount;
        }
    }
}
