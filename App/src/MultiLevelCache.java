import java.util.*;

class VideoData {
    String videoId;
    String content; // simplified representation
    int accessCount;

    VideoData(String videoId, String content) {
        this.videoId = videoId;
        this.content = content;
        this.accessCount = 0;
    }
}

class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    public LRUCache(int capacity) {
        super(capacity, 0.75f, true); // access-order
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}

public class MultiLevelCache {
    private LRUCache<String, VideoData> L1 = new LRUCache<>(10_000);
    private LRUCache<String, VideoData> L2 = new LRUCache<>(100_000);
    private Map<String, VideoData> L3 = new HashMap<>(); // simulate DB

    private long L1Hits = 0, L2Hits = 0, L3Hits = 0;
    private long L1Time = 0, L2Time = 0, L3Time = 0;

    public MultiLevelCache() {
        // preload DB with sample videos
        for (int i = 1; i <= 200_000; i++) {
            L3.put("video_" + i, new VideoData("video_" + i, "Content for video " + i));
        }
    }

    public VideoData getVideo(String videoId) {
        long start = System.nanoTime();

        // L1 Cache
        if (L1.containsKey(videoId)) {
            L1Hits++;
            L1Time += (System.nanoTime() - start);
            VideoData v = L1.get(videoId);
            v.accessCount++;
            return v;
        }

        // L2 Cache
        if (L2.containsKey(videoId)) {
            L2Hits++;
            L2Time += (System.nanoTime() - start);
            VideoData v = L2.get(videoId);
            v.accessCount++;

            // Promote to L1 if popular
            if (v.accessCount > 5) {
                L1.put(videoId, v);
            }
            return v;
        }

        // L3 Database
        if (L3.containsKey(videoId)) {
            L3Hits++;
            L3Time += (System.nanoTime() - start);
            VideoData v = L3.get(videoId);
            v.accessCount++;

            // Add to L2
            L2.put(videoId, v);
            return v;
        }

        return null; // not found
    }

    public String getStatistics() {
        long totalHits = L1Hits + L2Hits + L3Hits;
        double L1Rate = totalHits == 0 ? 0 : (L1Hits * 100.0 / totalHits);
        double L2Rate = totalHits == 0 ? 0 : (L2Hits * 100.0 / totalHits);
        double L3Rate = totalHits == 0 ? 0 : (L3Hits * 100.0 / totalHits);

        double L1Avg = L1Hits == 0 ? 0 : (L1Time / L1Hits) / 1_000_000.0;
        double L2Avg = L2Hits == 0 ? 0 : (L2Time / L2Hits) / 1_000_000.0;
        double L3Avg = L3Hits == 0 ? 0 : (L3Time / L3Hits) / 1_000_000.0;

        double overallAvg = totalHits == 0 ? 0 :
                (L1Time + L2Time + L3Time) / totalHits / 1_000_000.0;

        return String.format(
                "L1: Hit Rate %.1f%%, Avg Time: %.2fms\n" +
                        "L2: Hit Rate %.1f%%, Avg Time: %.2fms\n" +
                        "L3: Hit Rate %.1f%%, Avg Time: %.2fms\n" +
                        "Overall: Hit Rate %.1f%%, Avg Time: %.2fms",
                L1Rate, L1Avg, L2Rate, L2Avg, L3Rate, L3Avg,
                (L1Rate + L2Rate + L3Rate), overallAvg
        );
    }

    // For testing
    public static void main(String[] args) {
        MultiLevelCache cache = new MultiLevelCache();

        System.out.println(cache.getVideo("video_123")); // L3 HIT → L2
        System.out.println(cache.getVideo("video_123")); // L2 HIT → maybe L1
        System.out.println(cache.getVideo("video_123")); // L1 HIT after promotion
        System.out.println(cache.getVideo("video_999")); // L3 HIT → L2

        System.out.println(cache.getStatistics());
    }
}