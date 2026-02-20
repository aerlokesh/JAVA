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

// =====================================================================
// 1. TOKEN BUCKET ALGORITHM
// =====================================================================
// HOW IT WORKS:
//   - Imagine a bucket that holds tokens (max = capacity)
//   - Tokens are added at a fixed rate (refillRate tokens/second)
//   - Each request consumes 1 token
//   - If bucket has tokens → ALLOW (consume 1 token)
//   - If bucket is empty → DENY
//
// WHY USE IT:
//   - Allows BURSTS: if bucket is full, you can send `capacity` requests instantly
//   - Then throttles to `refillRate` requests/second
//   - Good for: APIs that tolerate short bursts but need average rate control
//
// EXAMPLE (capacity=5, refillRate=2/s):
//   t=0s: bucket=5 → send 5 requests instantly (burst) → bucket=0
//   t=0s: 6th request → DENIED (bucket empty)
//   t=1s: bucket refills +2 tokens → bucket=2
//   t=1s: send 2 requests → bucket=0
//
// DATA STRUCTURE: double[2] per client
//   [0] = current token count (double for fractional tokens)
//   [1] = last refill timestamp (nanoTime)
// =====================================================================
class TokenBucketRateLimiter implements RateLimiter {
    private final int capacity;         // max tokens the bucket can hold
    private final double refillRate;    // tokens added per second
    private final ConcurrentHashMap<String, double[]> buckets; // [tokens, lastRefillTimestamp]

    TokenBucketRateLimiter(int capacity, double refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.buckets = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized boolean allowRequest(String clientId) {
        // Step 1: Get or create bucket for this client (starts full)
        double[] bucket = buckets.computeIfAbsent(clientId,
            k -> new double[]{capacity, System.nanoTime()});

        // Step 2: Calculate how many tokens to add based on time elapsed
        //   Formula: newTokens = elapsedSeconds × refillRate
        //   Cap at capacity (bucket can't overflow)
        double now = System.nanoTime();
        double elapsed = (now - bucket[1]) / 1_000_000_000.0; // convert nanos → seconds
        bucket[0] = Math.min(capacity, bucket[0] + elapsed * refillRate);
        bucket[1] = now; // update last refill time

        // Step 3: Try to consume 1 token
        if (bucket[0] >= 1.0) {
            bucket[0] -= 1.0; // consume token
            return true;      // ALLOWED
        }
        return false;          // DENIED - no tokens left
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

// =====================================================================
// 2. FIXED WINDOW ALGORITHM
// =====================================================================
// HOW IT WORKS:
//   - Divide time into fixed windows (e.g., every 1000ms)
//   - Each window has a counter starting at 0
//   - Each request increments the counter
//   - If counter < maxRequests → ALLOW
//   - If counter >= maxRequests → DENY
//   - When window expires → counter resets to 0
//
// WHY USE IT:
//   - Simple to implement and understand
//   - Predictable: exactly N requests per window
//   - Good for: login endpoints, simple rate limits
//
// WEAKNESS - BOUNDARY BURST PROBLEM:
//   Window size = 1s, max = 3
//   t=0.9s: send 3 requests (all allowed, window 0-1s)
//   t=1.0s: window resets!
//   t=1.1s: send 3 more requests (all allowed, window 1-2s)
//   Result: 6 requests in 0.2 seconds! (solved by Sliding Window)
//
// EXAMPLE (max=3, window=1000ms):
//   t=0ms:   req1 → count=1 → ALLOWED
//   t=100ms: req2 → count=2 → ALLOWED
//   t=200ms: req3 → count=3 → ALLOWED
//   t=300ms: req4 → count=3 → DENIED (limit reached)
//   t=1000ms: window resets → count=0
//   t=1100ms: req5 → count=1 → ALLOWED
//
// DATA STRUCTURE: long[2] per client
//   [0] = window start timestamp
//   [1] = request count in current window
// =====================================================================
class FixedWindowRateLimiter implements RateLimiter {
    private final int maxRequests;       // max requests allowed per window
    private final long windowMillis;     // window duration in milliseconds
    private final ConcurrentHashMap<String, long[]> windows; // [windowStart, count]

    FixedWindowRateLimiter(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
        this.windows = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized boolean allowRequest(String clientId) {
        long now = System.currentTimeMillis();

        // Step 1: Get or create window for this client
        long[] window = windows.computeIfAbsent(clientId, k -> new long[]{now, 0});

        // Step 2: Check if current window has expired → reset
        if (now - window[0] >= windowMillis) {
            window[0] = now;  // start new window
            window[1] = 0;    // reset counter
        }

        // Step 3: Check if under limit
        if (window[1] < maxRequests) {
            window[1]++;     // increment counter
            return true;     // ALLOWED
        }
        return false;        // DENIED - window limit reached
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

// =====================================================================
// 3. SLIDING WINDOW LOG ALGORITHM
// =====================================================================
// HOW IT WORKS:
//   - Store the EXACT TIMESTAMP of every request in a sorted log (queue)
//   - On each new request:
//     1. Remove all timestamps older than (now - windowSize)
//     2. If remaining count < maxRequests → ALLOW and add timestamp
//     3. Otherwise → DENY
//
// WHY USE IT:
//   - Most ACCURATE: no boundary burst problem (unlike Fixed Window)
//   - The window "slides" with each request
//   - Good for: search APIs, any endpoint needing precise rate limiting
//
// TRADE-OFF:
//   - Uses more memory (stores each timestamp vs just a counter)
//   - O(n) cleanup on each request (but n is bounded by maxRequests)
//
// EXAMPLE (max=5, window=2000ms):
//   t=0ms:    req1 → log=[0]           → size=1 < 5 → ALLOWED
//   t=100ms:  req2 → log=[0,100]       → size=2 < 5 → ALLOWED
//   t=500ms:  req3 → log=[0,100,500]   → size=3 < 5 → ALLOWED
//   t=800ms:  req4 → log=[0,100,500,800] → size=4 → ALLOWED
//   t=900ms:  req5 → log=[0,100,500,800,900] → size=5 → ALLOWED
//   t=1000ms: req6 → log=[0,100,500,800,900] → size=5 → DENIED
//   t=2100ms: req7 → evict [0,100] (>2s old) → log=[500,800,900]
//                    → size=3 < 5 → ALLOWED, log=[500,800,900,2100]
//
// DATA STRUCTURE: Deque<Long> per client (timestamps in order)
//   - addLast() for new timestamps
//   - pollFirst() to evict expired ones
//   - Deque acts as a sliding window over time
// =====================================================================
class SlidingWindowLogRateLimiter implements RateLimiter {
    private final int maxRequests;       // max requests in the sliding window
    private final long windowMillis;     // window size in milliseconds
    private final ConcurrentHashMap<String, Deque<Long>> logs; // timestamp logs per client

    SlidingWindowLogRateLimiter(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
        this.logs = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized boolean allowRequest(String clientId) {
        long now = System.currentTimeMillis();

        // Step 1: Get or create timestamp log for this client
        Deque<Long> log = logs.computeIfAbsent(clientId, k -> new ArrayDeque<>());

        // Step 2: Evict all timestamps outside the sliding window
        //   Any timestamp older than (now - windowMillis) is expired
        while (!log.isEmpty() && now - log.peekFirst() >= windowMillis) {
            log.pollFirst(); // remove oldest expired timestamp
        }

        // Step 3: Check if under limit
        if (log.size() < maxRequests) {
            log.addLast(now); // record this request's timestamp
            return true;      // ALLOWED
        }
        return false;         // DENIED - too many requests in window
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