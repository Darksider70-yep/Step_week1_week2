import java.util.*;

class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    PriorityQueue<Map.Entry<String, Integer>> topQueries =
            new PriorityQueue<>(Map.Entry.comparingByValue()); // min-heap
    boolean isEndOfWord = false;
}

public class AutocompleteSystem {
    private TrieNode root;
    private Map<String, Integer> queryFrequency;
    private final int TOP_K = 10;

    public AutocompleteSystem() {
        root = new TrieNode();
        queryFrequency = new HashMap<>();
    }

    // Insert or update query
    public void insertQuery(String query) {
        queryFrequency.put(query, queryFrequency.getOrDefault(query, 0) + 1);
        TrieNode node = root;

        for (char c : query.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
            updateTopQueries(node, query);
        }
        node.isEndOfWord = true;
    }

    // Update top queries for a prefix
    private void updateTopQueries(TrieNode node, String query) {
        Map.Entry<String, Integer> entry = Map.entry(query, queryFrequency.get(query));
        node.topQueries.removeIf(e -> e.getKey().equals(query));
        node.topQueries.add(entry);
        if (node.topQueries.size() > TOP_K) {
            node.topQueries.poll(); // remove lowest frequency
        }
    }

    // Get top suggestions for a prefix
    public List<String> getSuggestions(String prefix) {
        TrieNode node = root;
        for (char c : prefix.toCharArray()) {
            node = node.children.get(c);
            if (node == null) return Collections.emptyList();
        }
        List<Map.Entry<String, Integer>> results = new ArrayList<>(node.topQueries);
        results.sort((a, b) -> b.getValue() - a.getValue()); // sort by frequency
        List<String> suggestions = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : results) {
            suggestions.add(prefix + entry.getKey().substring(prefix.length()) +
                    " (" + entry.getValue() + " searches)");
        }
        return suggestions;
    }

    // For testing
    public static void main(String[] args) {
        AutocompleteSystem system = new AutocompleteSystem();
        system.insertQuery("java tutorial");
        system.insertQuery("javascript");
        system.insertQuery("java download");
        system.insertQuery("java tutorial"); // frequency update

        System.out.println(system.getSuggestions("jav"));
        system.insertQuery("java 21 features");
        system.insertQuery("java 21 features");
        System.out.println(system.getSuggestions("java"));
    }
}