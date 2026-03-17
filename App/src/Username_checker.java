import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Username_checker {
    private ConcurrentHashMap<String, Integer> usernames; // username -> userId
    private ConcurrentHashMap<String, Integer> attempts;  // username -> frequency

    public Username_checker() {
        usernames = new ConcurrentHashMap<>();
        attempts = new ConcurrentHashMap<>();
    }

    // Register a username (for simulation)
    public void registerUser(String username, int userId) {
        usernames.put(username, userId);
    }

    // Check availability in O(1)
    public boolean checkAvailability(String username) {
        attempts.merge(username, 1, Integer::sum); // track attempts
        return !usernames.containsKey(username);
    }

    // Suggest alternatives if taken
    public List<String> suggestAlternatives(String username) {
        List<String> suggestions = new ArrayList<>();
        if (usernames.containsKey(username)) {
            suggestions.add(username + "1");
            suggestions.add(username + "2");
            suggestions.add(username.replace("_", "."));
        }
        return suggestions;
    }

    // Get most attempted username
    public String getMostAttempted() {
        return attempts.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("No attempts yet");
    }

    // For testing
    public static void main(String[] args) {
        Username_checker checker = new Username_checker();
        checker.registerUser("john_doe", 1);
        checker.registerUser("admin", 2);

        System.out.println(checker.checkAvailability("john_doe"));
        System.out.println(checker.checkAvailability("jane_smith"));
        System.out.println(checker.suggestAlternatives("john_doe"));
        System.out.println(checker.getMostAttempted());
    }
}
