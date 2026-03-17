import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;

class PageViewEvent {
    String url;
    String userId;
    String source;

    PageViewEvent(String url, String userId, String source) {
        this.url = url;
        this.userId = userId;
        this.source = source;
    }
}

public class AnalyticsDashboard {
    private ConcurrentHashMap<String, AtomicInteger> pageViews = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Set<String>> uniqueVisitors = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, AtomicInteger> trafficSources = new ConcurrentHashMap<>();

    public void processEvent(PageViewEvent event) {
        // Update page views
        pageViews.computeIfAbsent(event.url, k -> new AtomicInteger(0)).incrementAndGet();

        // Update unique visitors
        uniqueVisitors.computeIfAbsent(event.url, k -> ConcurrentHashMap.newKeySet()).add(event.userId);

        // Update traffic sources
        trafficSources.computeIfAbsent(event.source, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void getDashboard() {
        // Top 10 pages
        List<Map.Entry<String, Integer>> topPages = pageViews.entrySet().stream()
                .sorted((a, b) -> b.getValue().get() - a.getValue().get())
                .limit(10)
                .map(e -> Map.entry(e.getKey(), e.getValue().get()))
                .collect(Collectors.toList());

        System.out.println("Top Pages:");
        for (Map.Entry<String, Integer> entry : topPages) {
            String url = entry.getKey();
            int views = entry.getValue();
            int unique = uniqueVisitors.getOrDefault(url, Collections.emptySet()).size();
            System.out.println(url + " - " + views + " views (" + unique + " unique)");
        }

        // Traffic sources
        int totalSources = trafficSources.values().stream().mapToInt(AtomicInteger::get).sum();
        System.out.println("\nTraffic Sources:");
        for (Map.Entry<String, AtomicInteger> entry : trafficSources.entrySet()) {
            double percentage = (entry.getValue().get() * 100.0) / totalSources;
            System.out.printf("%s: %.1f%%\n", entry.getKey(), percentage);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        AnalyticsDashboard dashboard = new AnalyticsDashboard();

        // Simulate events
        dashboard.processEvent(new PageViewEvent("/article/breaking-news", "user_123", "google"));
        dashboard.processEvent(new PageViewEvent("/article/breaking-news", "user_456", "facebook"));
        dashboard.processEvent(new PageViewEvent("/sports/championship", "user_789", "direct"));

        // Scheduled dashboard updates every 5 seconds
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(dashboard::getDashboard, 0, 5, TimeUnit.SECONDS);
    }
}