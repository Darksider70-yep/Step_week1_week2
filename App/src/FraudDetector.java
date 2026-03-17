import java.util.*;

class Transaction {
    int id;
    int amount;
    String merchant;
    String account;
    long timestamp; // epoch ms

    Transaction(int id, int amount, String merchant, String account, long timestamp) {
        this.id = id;
        this.amount = amount;
        this.merchant = merchant;
        this.account = account;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "{id:" + id + ", amount:" + amount + ", merchant:'" + merchant +
                "', account:'" + account + "', time:" + timestamp + "}";
    }
}


public class FraudDetector {
    private Map<Integer, Transaction> complementMap = new HashMap<>();
    private Map<Integer, List<Transaction>> amountMap = new HashMap<>();
    private TreeMap<Long, List<Transaction>> timeIndex = new TreeMap<>();

    // Classic Two-Sum
    public List<int[]> findTwoSum(List<Transaction> transactions, int target) {
        List<int[]> result = new ArrayList<>();
        complementMap.clear();

        for (Transaction t : transactions) {
            if (complementMap.containsKey(t.amount)) {
                result.add(new int[]{complementMap.get(t.amount).id, t.id});
            }
            complementMap.put(target - t.amount, t);
        }
        return result;
    }

    // Two-Sum with time window (1 hour)
    public List<int[]> findTwoSumWithTime(List<Transaction> transactions, int target) {
        List<int[]> result = new ArrayList<>();
        timeIndex.clear();

        for (Transaction t : transactions) {
            long windowStart = t.timestamp - 3600_000; // 1 hour before
            long windowEnd = t.timestamp;

            NavigableMap<Long, List<Transaction>> candidates = timeIndex.subMap(windowStart, true, windowEnd, true);
            for (List<Transaction> list : candidates.values()) {
                for (Transaction other : list) {
                    if (t.amount + other.amount == target) {
                        result.add(new int[]{other.id, t.id});
                    }
                }
            }
            timeIndex.computeIfAbsent(t.timestamp, k -> new ArrayList<>()).add(t);
        }
        return result;
    }

    // K-Sum (recursive)
    public List<List<Integer>> findKSum(List<Transaction> transactions, int k, int target) {
        List<List<Integer>> result = new ArrayList<>();
        backtrack(transactions, k, target, 0, new ArrayList<>(), result);
        return result;
    }

    private void backtrack(List<Transaction> transactions, int k, int target, int start,
                           List<Integer> current, List<List<Integer>> result) {
        if (k == 0 && target == 0) {
            result.add(new ArrayList<>(current));
            return;
        }
        if (k == 0 || target < 0) return;

        for (int i = start; i < transactions.size(); i++) {
            current.add(transactions.get(i).id);
            backtrack(transactions, k - 1, target - transactions.get(i).amount, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    // Duplicate detection
    public List<Map<String, Object>> detectDuplicates(List<Transaction> transactions) {
        List<Map<String, Object>> duplicates = new ArrayList<>();
        amountMap.clear();

        for (Transaction t : transactions) {
            amountMap.computeIfAbsent(t.amount, k -> new ArrayList<>()).add(t);
        }

        for (Map.Entry<Integer, List<Transaction>> entry : amountMap.entrySet()) {
            Map<String, Map<String, Set<String>>> merchantAccounts = new HashMap<>();
            for (Transaction t : entry.getValue()) {
                merchantAccounts.computeIfAbsent(t.merchant, k -> new HashMap<>())
                        .computeIfAbsent("accounts", k -> new HashSet<>()).add(t.account);
            }
            for (Map.Entry<String, Map<String, Set<String>>> mEntry : merchantAccounts.entrySet()) {
                if (mEntry.getValue().get("accounts").size() > 1) {
                    duplicates.add(Map.of(
                            "amount", entry.getKey(),
                            "merchant", mEntry.getKey(),
                            "accounts", mEntry.getValue().get("accounts")
                    ));
                }
            }
        }
        return duplicates;
    }

    // For testing
    public static void main(String[] args) {
        FraudDetector detector = new FraudDetector();
        List<Transaction> transactions = List.of(
                new Transaction(1, 500, "Store A", "acc1", 10_00),
                new Transaction(2, 300, "Store B", "acc2", 10_15),
                new Transaction(3, 200, "Store C", "acc3", 10_30)
        );

        System.out.println(detector.findTwoSum(transactions, 500)); // [(2,3)]
        System.out.println(detector.detectDuplicates(transactions)); // example duplicates
        System.out.println(detector.findKSum(transactions, 3, 1000)); // [(1,2,3)]
    }
}