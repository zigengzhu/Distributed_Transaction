import java.util.*;

public class Account {
    String name;
    int balance;
    int last_committed;
    List<Integer> rts = new ArrayList<>();
    Map<Integer, Integer> tw = new HashMap<>();
    Map<Integer, Integer> committed_history = new HashMap<>();

    Account(String name, int balance) {
        this.name = name;
        this.balance = balance;
        this.last_committed = 0;
        this.committed_history.put(this.last_committed, this.balance); //0, 0
    }

    int maxRTS() {
        if (rts.isEmpty()) {
            return 0;
        } else {
            return Collections.max(rts);
        }
    }

    String[] getD(int timestamp) {
        String[] ans = new String[2]; // canRead, D
        if (tw.isEmpty()) {
            ans[0] = "true";
            ans[1] = Integer.toString(committed_history.get(last_committed));
        } else  {
            int max_prev_time = 0;
            for (int t : tw.keySet()) {
                if (t > max_prev_time && t <= timestamp) {
                    max_prev_time = t;
                }
            }
            if (max_prev_time == timestamp) {
                if (tw.containsKey(timestamp)) {
                    ans[0] = "true";
                    ans[1] = Integer.toString(tw.get(timestamp) + balance);
                } else {
                    ans[0] = "true";
                    ans[1] = Integer.toString(balance);
                }
            } else {
                ans[0] = "wait";
                ans[1] = "";
            }
        }
        return ans;
    }
}
