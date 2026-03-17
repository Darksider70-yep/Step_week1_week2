import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Flashsalemanager {
    private ConcurrentHashMap<String, AtomicInteger> stockMap;
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<Integer>> waitingListMap;

    public Flashsalemanager() {
        stockMap = new ConcurrentHashMap<>();
        waitingListMap = new ConcurrentHashMap<>();
    }

    // Initialize product stock
    public void addProduct(String productId, int stockCount) {
        stockMap.put(productId, new AtomicInteger(stockCount));
        waitingListMap.put(productId, new ConcurrentLinkedQueue<>());
    }

    // Check stock availability
    public int checkStock(String productId) {
        return stockMap.getOrDefault(productId, new AtomicInteger(0)).get();
    }

    // Process purchase request
    public String purchaseItem(String productId, int userId) {
        AtomicInteger stock = stockMap.get(productId);
        if (stock == null) return "Product not found";

        // Atomic decrement
        int currentStock;
        do {
            currentStock = stock.get();
            if (currentStock == 0) {
                waitingListMap.get(productId).add(userId);
                return "Added to waiting list, position #" + waitingListMap.get(productId).size();
            }
        } while (!stock.compareAndSet(currentStock, currentStock - 1));

        return "Success, " + stock.get() + " units remaining";
    }

    // Get waiting list for a product
    public List<Integer> getWaitingList(String productId) {
        return new ArrayList<>(waitingListMap.getOrDefault(productId, new ConcurrentLinkedQueue<>()));
    }

    // For testing
    public static void main(String[] args) {
        Flashsalemanager manager = new Flashsalemanager();
        manager.addProduct("IPHONE15_256GB", 100);

        System.out.println(manager.checkStock("IPHONE15_256GB")); // 100
        System.out.println(manager.purchaseItem("IPHONE15_256GB", 12345)); // Success, 99 units remaining
        System.out.println(manager.purchaseItem("IPHONE15_256GB", 67890)); // Success, 98 units remaining

        // Simulate overselling prevention
        for (int i = 0; i < 100; i++) {
            manager.purchaseItem("IPHONE15_256GB", i);
        }
        System.out.println(manager.purchaseItem("IPHONE15_256GB", 99999)); // Added to waiting list, position #1
    }
}
