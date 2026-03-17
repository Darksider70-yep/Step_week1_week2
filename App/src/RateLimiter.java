import java.util.concurrent.*;
import java.util.*;

class TokenBucket {
    private int tokens;
    private final int maxTokens;
    private final double refillRate; // tokens per second
    private long lastRefillTime;

    public TokenBucket(int maxTokens, int refillRatePerHour) {
        this.maxTokens = maxTokens;
        this.tokens = maxTokens;
        this.refillRate = refillRatePerHour / 3600.0; // convert to tokens/sec
        this.lastRefillTime = System.currentTimeMillis();
    }

    // Refill tokens based on elapsed time
    private void refill() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRefillTime;
        int refillTokens = (int) (elapsed / 1000.0 * refillRate);
        if (refillTokens > 0) {
            tokens = Math.min(maxTokens, tokens + refillTokens);
            lastRefillTime = now;
        }
    }

    // Try to consume a token
    public synchronized boolean allowRequest() {
        refill();
        if (tokens > 0) {
            tokens--;
            return true;
        }
        return false;
    }

    public synchronized int getRemainingTokens() {
        refill();
        return tokens;
    }

    public synchronized long getResetTime() {
        return lastRefillTime + 3600_000; // next hour reset
    }
}

public class RateLimiter {
    private ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final int MAX_TOKENS = 1000;

    public boolean checkRateLimit(String clientId) {
        TokenBucket bucket = buckets.computeIfAbsent(clientId,
                k -> new TokenBucket(MAX_TOKENS, MAX_TOKENS));
        return bucket.allowRequest();
    }

    public Map<String, Object> getRateLimitStatus(String clientId) {
        TokenBucket bucket = buckets.get(clientId);
        if (bucket == null) return Map.of("used", 0, "limit", MAX_TOKENS, "reset", System.currentTimeMillis());
        int remaining = bucket.getRemainingTokens();
        return Map.of(
                "used", MAX_TOKENS - remaining,
                "limit", MAX_TOKENS,
                "reset", bucket.getResetTime()
        );
    }

    // For testing
    public static void main(String[] args) {
        RateLimiter limiter = new RateLimiter();
        String clientId = "abc123";

        for (int i = 0; i < 1002; i++) {
            boolean allowed = limiter.checkRateLimit(clientId);
            if (!allowed) {
                System.out.println("Denied (0 requests remaining, retry after " +
                        (limiter.getRateLimitStatus(clientId).get("reset")) + ")");
                break;
            }
        }
        System.out.println(limiter.getRateLimitStatus(clientId));
    }
}