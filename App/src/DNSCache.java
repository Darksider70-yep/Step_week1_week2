import java.util.*;
import java.util.concurrent.*;

class DNSEntry {
    String domain;
    String ipAddress;
    long timestamp;
    long expiryTime;

    DNSEntry(String domain, String ipAddress, long ttlSeconds) {
        this.domain = domain;
        this.ipAddress = ipAddress;
        this.timestamp = System.currentTimeMillis();
        this.expiryTime = this.timestamp + ttlSeconds * 1000;
    }

    boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }
}

public class DNSCache {
    private final int MAX_CACHE_SIZE = 1000;
    private LinkedHashMap<String, DNSEntry> cache;
    private long hits = 0, misses = 0;
    private long totalLookupTime = 0;

    public DNSCache() {
        cache = new LinkedHashMap<>(16, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<String, DNSEntry> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };

        // Background thread for cleaning expired entries
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            synchronized (cache) {
                Iterator<Map.Entry<String, DNSEntry>> it = cache.entrySet().iterator();
                while (it.hasNext()) {
                    if (it.next().getValue().isExpired()) {
                        it.remove();
                    }
                }
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    // Resolve domain
    public String resolve(String domain) {
        long start = System.nanoTime();
        synchronized (cache) {
            DNSEntry entry = cache.get(domain);
            if (entry != null && !entry.isExpired()) {
                hits++;
                totalLookupTime += (System.nanoTime() - start);
                return "Cache HIT → " + entry.ipAddress;
            } else {
                misses++;
                String ip = queryUpstreamDNS(domain);
                cache.put(domain, new DNSEntry(domain, ip, 300)); // TTL = 300s
                totalLookupTime += (System.nanoTime() - start);
                return (entry == null ? "Cache MISS → " : "Cache EXPIRED → ") + ip;
            }
        }
    }

    // Simulated upstream DNS query
    private String queryUpstreamDNS(String domain) {
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        return "172.217." + new Random().nextInt(255) + "." + new Random().nextInt(255);
    }

    // Cache statistics
    public String getCacheStats() {
        long total = hits + misses;
        double hitRate = total == 0 ? 0 : (hits * 100.0 / total);
        double avgLookupTime = total == 0 ? 0 : (totalLookupTime / total) / 1_000_000.0;
        return String.format("Hit Rate: %.2f%%, Avg Lookup Time: %.2fms", hitRate, avgLookupTime);
    }

    // For testing
    public static void main(String[] args) throws InterruptedException {
        DNSCache dnsCache = new DNSCache();

        System.out.println(dnsCache.resolve("google.com")); // MISS
        System.out.println(dnsCache.resolve("google.com")); // HIT
        Thread.sleep(301_000); // wait for TTL expiry
        System.out.println(dnsCache.resolve("google.com")); // EXPIRED
        System.out.println(dnsCache.getCacheStats()); // stats
    }
}