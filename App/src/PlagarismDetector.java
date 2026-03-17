import java.util.*;
import java.util.stream.Collectors;

class PlagiarismDetector {
    private Map<String, Set<String>> nGramIndex; // n-gram -> documents
    private int n; // size of n-gram

    public PlagiarismDetector(int n) {
        this.n = n;
        this.nGramIndex = new HashMap<>();
    }

    // Index a document into the system
    public void indexDocument(String docId, String text) {
        List<String> words = Arrays.asList(text.split("\\s+"));
        for (int i = 0; i <= words.size() - n; i++) {
            String nGram = String.join(" ", words.subList(i, i + n));
            nGramIndex.computeIfAbsent(nGram, k -> new HashSet<>()).add(docId);
        }
    }

    // Analyze a new document for plagiarism
    public Map<String, Double> analyzeDocument(String docId, String text) {
        List<String> words = Arrays.asList(text.split("\\s+"));
        int totalNGrams = Math.max(0, words.size() - n + 1);
        Map<String, Integer> matchCount = new HashMap<>();

        for (int i = 0; i <= words.size() - n; i++) {
            String nGram = String.join(" ", words.subList(i, i + n));
            Set<String> docs = nGramIndex.getOrDefault(nGram, Collections.emptySet());
            for (String d : docs) {
                if (!d.equals(docId)) {
                    matchCount.merge(d, 1, Integer::sum);
                }
            }
        }

        // Calculate similarity percentages
        Map<String, Double> similarityScores = new HashMap<>();
        for (Map.Entry<String, Integer> entry : matchCount.entrySet()) {
            double similarity = (entry.getValue() * 100.0) / totalNGrams;
            similarityScores.put(entry.getKey(), similarity);
        }

        return similarityScores;
    }

    // Get most similar documents
    public List<Map.Entry<String, Double>> getMostSimilar(Map<String, Double> scores) {
        return scores.entrySet()
                .stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());
    }

    // For testing
    public static void main(String[] args) {
        PlagiarismDetector detector = new PlagiarismDetector(5);

        // Index previous essays
        detector.indexDocument("essay_089", "This is a sample essay with some unique content");
        detector.indexDocument("essay_092", "This essay contains a lot of similar words and repeated phrases");

        // Analyze new essay
        Map<String, Double> scores = detector.analyzeDocument("essay_123",
                "This essay contains a lot of similar words and repeated phrases with some unique content");

        System.out.println("Similarity Scores: " + scores);
        System.out.println("Most Similar: " + detector.getMostSimilar(scores));
    }
}