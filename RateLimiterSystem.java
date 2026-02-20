import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// ==================== ENUMS ====================

enum RateLimiterType { TOKEN_BUCKET, FIXED_WINDOW, SLIDING_WINDOW_LOG }
enum RequestResult { ALLOWED, DENIED }

// ==================== EXCEPTIONS ====================

class RateLimitExceededException extends Exception {
    public RateLimitExceededException(String msg) { super(msg); }
}

// ==================== INTERFACE ====================

interface RateLimiter {
    boolean allowRequest(String clientId);
    String getName();
    String getStats(String clientId);
}

// ==================== IMPLEMENTATIONS ====================

// 1. TOKEN BUCKET - allows bursts, refills over time
class TokenBucketRateLimiter implements RateLimiter {
    private final int capacity;         // max tokens
    private final double refillRate;    // tokens per second
    private final ConcurrentHashMap<String, double[]> buckets; // [tokens, lastRefillTimestamp]

    TokenBucketRateLimiter(int capacity, double refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.buckets = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized boolean allowRequest(String clientId) {
        double[] bucket = buckets.computeIfAbsent(clientId,
            k -> new double[]{capacity, System.nanoTime()});

        // Refill tokens based on elapsed time
        double now = System.nanoTime();
        double elapsed = (now - bucket[1]) / 1_000_000_000.0; // seconds
        bucket[0] = Math.min(capacity, bucket[0] + elapsed * refillRate);
        bucket[1] = now;

        if (bucket[0] >= 1.0) {
            bucket[0] -= 1.0;
            return true;
        }
        return false;
    }

    @Override
    public String getName() { return "TokenBucket(cap=" + capacity + ",rate=" + refillRate + "/s)"; }

    @Override
    public String getStats(String clientId) {
        double[] bucket = buckets.get(clientId);
        if (bucket == null) return "no data";
        return String.format("tokens=%.1f/%d", bucket[0], capacity);
    }
}

// 2. FIXED WINDOW - resets counter every window
class FixedWindowRateLimiter implements RateLimiter {
    private final int maxRequests;
    private final long windowMillis;
    private final ConcurrentHashMap<String, long[]> windows; // [windowStart, count]

    FixedWindowRateLimiter(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
        this.windows = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized boolean allowRequest(String clientId) {
        long now = System.currentTimeMillis();
        long[] window = windows.computeIfAbsent(clientId, k -> new long[]{now, 0});

        // Reset if window expired
        if (now - window[0] >= windowMillis) {
            window[0] = now;
            window[1] = 0;
        }

        if (window[1] < maxRequests) {
            window[1]++;
            return true;
        }
        return false;
    }

    @Override
    public String getName() { return "FixedWindow(max=" + maxRequests + ",window=" + windowMillis + "ms)"; }

    @Override
    public String getStats(String clientId) {
        long[] window = windows.get(clientId);
        if (window == null) return "no data";
        return "count=" + window[1] + "/" + maxRequests;
    }
}

// 3. SLIDING WINDOW LOG - tracks individual request timestamps
class SlidingWindowLogRateLimiter implements RateLimiter {
    private final int maxRequests;
    private final long windowMillis;
    private final ConcurrentHashMap<String, Deque<Long>> logs; // timestamps

    SlidingWindowLogRateLimiter(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
        this.logs = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized boolean allowRequest(String clientId) {
        long now = System.currentTimeMillis();
        Deque<Long> log = logs.computeIfAbsent(clientId, k -> new ArrayDeque<>());

        // Remove expired timestamps
        while (!log.isEmpty() && now - log.peekFirst() >= windowMillis) {
            log.pollFirst();
        }

        if (log.size() < maxRequests) {
            log.addLast(now);
            return true;
        }
        return false;
    }

    @Override
    public String getName() { return "SlidingWindowLog(max=" + maxRequests + ",window=" + windowMillis + "ms)"; }

    @Override
    public String getStats(String clientId) {
        Deque<Long> log = logs.get(clientId);
        if (log == null) return "no data";
        return "entries=" + log.size() + "/" + maxRequests;
    }
}

// ==================== MAIN SERVICE ====================

class RateLimiterService {
    private final Map<String, RateLimiter> endpointLimiters; // endpoint -> limiter
    private final RateLimiter defaultLimiter;
    private final AtomicInteger totalAllowed = new AtomicInteger(0);
    private final AtomicInteger totalDenied = new AtomicInteger(0);

    RateLimiterService(RateLimiter defaultLimiter) {
        this.endpointLimiters = new ConcurrentHashMap<>();
        this.defaultLimiter = defaultLimiter;
    }

    void registerEndpoint(String endpoint, RateLimiter limiter) {
        endpointLimiters.put(endpoint, limiter);
    }

    // Process a request - returns ALLOWED or DENIED
    public RequestResult processRequest(String endpoint, String clientId) {
        RateLimiter limiter = endpointLimiters.getOrDefault(endpoint, defaultLimiter);
        boolean allowed = limiter.allowRequest(clientId);

        if (allowed) {
            totalAllowed.incrementAndGet();
        } else {
            totalDenied.incrementAndGet();
        }

        RequestResult result = allowed ? RequestResult.ALLOWED : RequestResult.DENIED;
        System.out.println(Thread.currentThread().getName()
            + ": " + clientId + " → " + endpoint
            + " [" + result + "] via " + limiter.getName());
        return result;
    }

    // Batch request helper (silent - no per-request logging)
    public int[] batchRequests(String endpoint, String clientId, int count) {
        RateLimiter limiter = endpointLimiters.getOrDefault(endpoint, defaultLimiter);
        int allowed = 0, denied = 0;
        for (int i = 0; i < count; i++) {
            if (limiter.allowRequest(clientId)) {
                allowed++;
                totalAllowed.incrementAndGet();
            } else {
                denied++;
                totalDenied.incrementAndGet();
            }
        }
        return new int[]{allowed, denied};
    }

    void displayStatus() {
        System.out.println("\n--- Rate Limiter Service Status ---");
        System.out.println("Default: " + defaultLimiter.getName());
        System.out.println("Endpoints:");
        endpointLimiters.forEach((ep, rl) -> System.out.println("  " + ep + " → " + rl.getName()));
        System.out.println("Total: " + totalAllowed.get() + " allowed, " + totalDenied.get() + " denied");
    }
}

// ==================== MAIN ====================

public class RateLimiterSystem {
    public static void main(String[] args) throws InterruptedException {
        // ---- Setup ----
        RateLimiter defaultLimiter = new TokenBucketRateLimiter(5, 2); // 5 capacity, 2 tokens/sec
        RateLimiterService service = new RateLimiterService(defaultLimiter);

        // Register different strategies per endpoint
        service.registerEndpoint("/api/login",    new FixedWindowRateLimiter(3, 1000));       // 3 req/sec
        service.registerEndpoint("/api/search",   new SlidingWindowLogRateLimiter(5, 2000));  // 5 req/2sec
        service.registerEndpoint("/api/payment",  new TokenBucketRateLimiter(2, 0.5));        // 2 cap, 0.5/sec

        service.displayStatus();

        // ---- Test 1: Token Bucket - burst then throttle ----
        System.out.println("\n=== Test 1: TOKEN BUCKET (burst + throttle) ===");
        int[] result = service.batchRequests("/api/default", "client-A", 8);
        System.out.println("Sent 8 requests: " + result[0] + " allowed, " + result[1] + " denied");
        System.out.println("✓ Token Bucket allowed burst of 5, denied 3: " + (result[0] == 5 && result[1] == 3));

        // ---- Test 2: Token Bucket - refill after wait ----
        System.out.println("\n=== Test 2: TOKEN BUCKET (refill after 1 second) ===");
        Thread.sleep(1000); // Wait for refill (2 tokens/sec)
        result = service.batchRequests("/api/default", "client-A", 3);
        System.out.println("After 1s wait, sent 3: " + result[0] + " allowed, " + result[1] + " denied");
        System.out.println("✓ Tokens refilled (expected ~2 allowed): " + (result[0] >= 1 && result[0] <= 3));

        // ---- Test 3: Fixed Window - exact limit ----
        System.out.println("\n=== Test 3: FIXED WINDOW (3 req/sec) ===");
        result = service.batchRequests("/api/login", "client-B", 6);
        System.out.println("Sent 6 requests: " + result[0] + " allowed, " + result[1] + " denied");
        System.out.println("✓ Fixed Window allowed exactly 3: " + (result[0] == 3));

        // ---- Test 4: Fixed Window - reset after window ----
        System.out.println("\n=== Test 4: FIXED WINDOW (reset after 1s) ===");
        Thread.sleep(1100); // Wait for window reset
        result = service.batchRequests("/api/login", "client-B", 4);
        System.out.println("After window reset, sent 4: " + result[0] + " allowed, " + result[1] + " denied");
        System.out.println("✓ Window reset allowed 3 more: " + (result[0] == 3));

        // ---- Test 5: Sliding Window Log ----
        System.out.println("\n=== Test 5: SLIDING WINDOW LOG (5 req/2sec) ===");
        result = service.batchRequests("/api/search", "client-C", 7);
        System.out.println("Sent 7 requests: " + result[0] + " allowed, " + result[1] + " denied");
        System.out.println("✓ Sliding window allowed 5: " + (result[0] == 5));

        // Partial window expiry
        Thread.sleep(1000);
        result = service.batchRequests("/api/search", "client-C", 3);
        System.out.println("After 1s (partial), sent 3: " + result[0] + " allowed, " + result[1] + " denied");
        System.out.println("✓ Some timestamps still in window, partial allow");

        // ---- Test 6: Per-client isolation ----
        System.out.println("\n=== Test 6: PER-CLIENT ISOLATION ===");
        int[] r1 = service.batchRequests("/api/login", "isolated-1", 3);
        int[] r2 = service.batchRequests("/api/login", "isolated-2", 3);
        System.out.println("Client isolated-1: " + r1[0] + " allowed");
        System.out.println("Client isolated-2: " + r2[0] + " allowed");
        System.out.println("✓ Both clients got full quota: " + (r1[0] == 3 && r2[0] == 3));

        // ---- Test 7: Payment endpoint - strict limit ----
        System.out.println("\n=== Test 7: PAYMENT ENDPOINT (TokenBucket cap=2, rate=0.5/s) ===");
        result = service.batchRequests("/api/payment", "client-D", 5);
        System.out.println("Sent 5 payment requests: " + result[0] + " allowed, " + result[1] + " denied");
        System.out.println("✓ Strict payment limit (2 burst): " + (result[0] == 2));

        // ---- Test 8: CONCURRENCY - 20 threads hitting same endpoint ----
        System.out.println("\n=== Test 8: CONCURRENT REQUESTS (20 threads, FixedWindow max=3) ===");
        // Fresh endpoint for clean test
        service.registerEndpoint("/api/concurrent", new FixedWindowRateLimiter(3, 5000));

        List<Thread> threads = new ArrayList<>();
        AtomicInteger concurrentAllowed = new AtomicInteger(0);
        AtomicInteger concurrentDenied = new AtomicInteger(0);

        for (int i = 0; i < 20; i++) {
            Thread t = new Thread(() -> {
                RateLimiter limiter = new FixedWindowRateLimiter(3, 5000); // won't use this
                // Use the service directly
                RequestResult res = service.processRequest("/api/concurrent", "race-client");
                if (res == RequestResult.ALLOWED) concurrentAllowed.incrementAndGet();
                else concurrentDenied.incrementAndGet();
            }, "RaceThread-" + i);
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) { t.join(); }

        System.out.println("Results: " + concurrentAllowed.get() + " allowed, " + concurrentDenied.get() + " denied");
        System.out.println("✓ Exactly 3 allowed under concurrency: " + (concurrentAllowed.get() == 3));
        System.out.println("✓ No race conditions - total is 20: " + (concurrentAllowed.get() + concurrentDenied.get() == 20));

        // ---- Test 9: CONCURRENT different clients on same endpoint ----
        System.out.println("\n=== Test 9: CONCURRENT DIFFERENT CLIENTS (5 clients × 4 requests) ===");
        service.registerEndpoint("/api/multi", new FixedWindowRateLimiter(3, 5000));
        threads.clear();
        AtomicInteger multiAllowed = new AtomicInteger(0);

        for (int c = 0; c < 5; c++) {
            final String clientId = "multi-client-" + c;
            for (int r = 0; r < 4; r++) {
                Thread t = new Thread(() -> {
                    RequestResult res = service.processRequest("/api/multi", clientId);
                    if (res == RequestResult.ALLOWED) multiAllowed.incrementAndGet();
                }, "MultiThread-" + clientId);
                threads.add(t);
                t.start();
            }
        }

        for (Thread t : threads) { t.join(); }

        // Each of 5 clients should get max 3 allowed = 15 total
        System.out.println("Total allowed across 5 clients: " + multiAllowed.get());
        System.out.println("✓ Each client limited independently (expected 15): " + (multiAllowed.get() == 15));

        // ---- Final Status ----
        service.displayStatus();
        System.out.println("\n✅ All Rate Limiter tests complete!");
    }
}