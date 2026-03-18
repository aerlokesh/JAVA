import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// ===== CUSTOM EXCEPTION CLASSES =====

/**
 * Exception thrown when rate limit is exceeded
 * WHEN TO THROW:
 * - Client exceeds allowed request rate
 * - Request should be denied/throttled
 */
class RateLimitExceededException extends Exception {
    private String clientId;
    private long retryAfterMs;
    
    public RateLimitExceededException(String clientId, String message, long retryAfterMs) {
        super("Rate limit exceeded for client " + clientId + ": " + message);
        this.clientId = clientId;
        this.retryAfterMs = retryAfterMs;
    }
    
    public String getClientId() { return clientId; }
    public long getRetryAfterMs() { return retryAfterMs; }
}

/**
 * Exception thrown when rate limiter configuration is invalid
 * WHEN TO THROW:
 * - Invalid capacity, rate, or window values
 * - Negative or zero values where positive required
 */
class InvalidRateLimiterConfigException extends Exception {
    public InvalidRateLimiterConfigException(String message) {
        super(message);
    }
}

// ===== ENUMS =====

enum RateLimiterType { 
    TOKEN_BUCKET,        // Smooth rate limiting with burst support
    FIXED_WINDOW,        // Simple counter per time window
    SLIDING_WINDOW_LOG   // Accurate sliding window
}

enum RequestResult { 
    ALLOWED,   // Request within rate limit
    DENIED     // Request exceeds rate limit
}

// ===== INTERFACE =====

/**
 * Interface for different rate limiting strategies
 */
interface RateLimiter {
    boolean allowRequest(String clientId);
    String getName();
    String getStats(String clientId);
}

/**
 * Rate Limiter System - Low Level Design (LLD)
 * 
 * PROBLEM STATEMENT:
 * Design a rate limiting system that can:
 * 1. Limit requests per client using different algorithms
 * 2. Support multiple strategies (Token Bucket, Fixed Window, Sliding Window)
 * 3. Handle concurrent requests safely
 * 4. Track statistics per client
 * 5. Support per-endpoint rate limits
 * 
 * REQUIREMENTS:
 * - Functional: Allow/deny requests, multiple algorithms, per-client limits
 * - Non-Functional: Thread-safe, low latency, memory efficient
 * 
 * INTERVIEW HINTS:
 * - Discuss trade-offs between algorithms (accuracy vs memory)
 * - Talk about distributed rate limiting (Redis-based)
 * - Mention thread safety (synchronized, ConcurrentHashMap)
 * - Consider memory vs accuracy trade-offs
 * - Discuss real-world implementation (API Gateway, CDN)
 */

// ===== TOKEN BUCKET ALGORITHM =====

/**
 * TOKEN BUCKET ALGORITHM
 * ======================
 * HOW IT WORKS:
 *   - Bucket holds tokens (max = capacity)
 *   - Tokens refill at constant rate per second
 *   - Each request consumes 1 token
 *   - If tokens available → ALLOW, else → DENY
 * 
 * EXAMPLE: capacity=5, refillRate=2/sec
 *   - Can handle burst of 5 requests immediately
 *   - Then 2 requests/sec sustained
 * 
 * PROS:
 *   - Allows bursts up to capacity
 *   - Smooth rate limiting
 *   - Memory efficient (2 values per client)
 * 
 * CONS:
 *   - Requires timestamp tracking per client
 *   - Floating point calculations
 * 
 * DATA STRUCTURE:
 *   ConcurrentHashMap<clientId, double[2]>
 *   [0] = current tokens (can be fractional)
 *   [1] = last refill timestamp (nanoseconds)
 * 
 * USE CASE: APIs that can tolerate bursts (search, browse, read operations)
 */
class TokenBucketRateLimiter implements RateLimiter {
    private final int capacity;           // Max tokens in bucket
    private final double refillRate;      // Tokens added per second
    private final ConcurrentHashMap<String, double[]> buckets;
    
    public TokenBucketRateLimiter(int capacity, double refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.buckets = new ConcurrentHashMap<>();
    }
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Get or create bucket for client (initialize with full capacity)
     * 2. Calculate elapsed time since last refill (in seconds)
     * 3. Refill tokens: current + (elapsed * refillRate), capped at capacity
     * 4. Update last refill timestamp to now
     * 5. If tokens >= 1, consume 1 token and return true
     * 6. Otherwise return false
     * 
     * FORMULA:
     *   elapsedSeconds = (now - lastRefill) / 1_000_000_000.0
     *   newTokens = min(capacity, currentTokens + elapsedSeconds * refillRate)
     * 
     * @param clientId Client making the request
     * @return true if allowed, false if denied
     */
    @Override
    public synchronized boolean allowRequest(String clientId) {
        // HINT: long now = System.nanoTime();
        // HINT: double[] bucket = buckets.computeIfAbsent(clientId, 
        //           k -> new double[]{capacity, now});
        // HINT: double elapsed = (now - bucket[1]) / 1_000_000_000.0;
        // HINT: bucket[0] = Math.min(capacity, bucket[0] + elapsed * refillRate);
        // HINT: bucket[1] = now;
        // HINT: if (bucket[0] >= 1) { bucket[0] -= 1; return true; }
        // HINT: return false;
        long now=System.nanoTime();
        double[] bucket = buckets.computeIfAbsent(clientId, k->new double[]{capacity,now});
        return false;
    }
    
    @Override
    public String getName() { 
        return "TokenBucket(cap=" + capacity + ",rate=" + refillRate + "/s)"; 
    }
    
    @Override
    public String getStats(String clientId) {
        // TODO: Implement
        // HINT: Get bucket and return formatted string with current tokens
        // HINT: String.format("%.2f tokens available", bucket[0])
        return "not implemented";
    }
}

// ===== FIXED WINDOW ALGORITHM =====

/**
 * FIXED WINDOW ALGORITHM
 * =======================
 * HOW IT WORKS:
 *   - Time divided into fixed windows (e.g., every 1 second)
 *   - Counter per window resets when window expires
 *   - If count < max → ALLOW, else → DENY
 * 
 * EXAMPLE: maxRequests=10, window=1000ms
 *   - Up to 10 requests allowed per second
 *   - Counter resets every second
 * 
 * PROS:
 *   - Simple to implement
 *   - Memory efficient (counter + timestamp per client)
 *   - Predictable behavior
 * 
 * CONS:
 *   - Boundary burst problem (can get 2x at window edges)
 *   - Example: 10 req at 0.9s, 10 req at 1.1s = 20 req in 0.2s
 * 
 * DATA STRUCTURE:
 *   ConcurrentHashMap<clientId, long[2]>
 *   [0] = window start timestamp (millis)
 *   [1] = request count in current window
 * 
 * USE CASE: Simple rate limiting (login attempts, password resets)
 */
class FixedWindowRateLimiter implements RateLimiter {
    private final int maxRequests;
    private final long windowMillis;
    private final ConcurrentHashMap<String, long[]> windows;
    
    public FixedWindowRateLimiter(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
        this.windows = new ConcurrentHashMap<>();
    }
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Get or create window for client
     * 2. Check if window expired: (now - window[0] >= windowMillis)
     * 3. If expired, reset: window[0] = now, window[1] = 0
     * 4. If count < maxRequests, increment count and return true
     * 5. Otherwise return false
     * 
     * @param clientId Client making the request
     * @return true if allowed, false if denied
     */
    @Override
    public synchronized boolean allowRequest(String clientId) {
        // TODO: Implement
        // HINT: long now = System.currentTimeMillis();
        // HINT: long[] window = windows.computeIfAbsent(clientId, k -> new long[]{now, 0});
        // HINT: if (now - window[0] >= windowMillis) { window[0] = now; window[1] = 0; }
        // HINT: if (window[1] < maxRequests) { window[1]++; return true; }
        // HINT: return false;
        return false;
    }
    
    @Override
    public String getName() { 
        return "FixedWindow(max=" + maxRequests + ",window=" + windowMillis + "ms)"; 
    }
    
    @Override
    public String getStats(String clientId) {
        // TODO: Implement
        // HINT: Return request count in current window
        return "not implemented";
    }
}

// ===== SLIDING WINDOW LOG ALGORITHM =====

/**
 * SLIDING WINDOW LOG ALGORITHM
 * =============================
 * HOW IT WORKS:
 *   - Store exact timestamp of each request in a log
 *   - On new request: evict timestamps older than window
 *   - If remaining count < max → ALLOW, else → DENY
 * 
 * EXAMPLE: maxRequests=10, window=1000ms
 *   - Keep last 10 request timestamps
 *   - Allow if fewer than 10 requests in last 1000ms
 * 
 * PROS:
 *   - Most accurate (no boundary burst problem)
 *   - True sliding window
 *   - Precise rate limiting
 * 
 * CONS:
 *   - Higher memory usage (stores all recent timestamps)
 *   - O(n) eviction per request (where n = maxRequests)
 *   - More CPU intensive
 * 
 * DATA STRUCTURE:
 *   ConcurrentHashMap<clientId, Deque<Long>>
 *   Deque stores timestamps in chronological order (FIFO)
 * 
 * USE CASE: Critical APIs where precise limiting is required (payment, write operations)
 */
class SlidingWindowLogRateLimiter implements RateLimiter {
    private final int maxRequests;
    private final long windowMillis;
    private final ConcurrentHashMap<String, Deque<Long>> logs;
    
    public SlidingWindowLogRateLimiter(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
        this.logs = new ConcurrentHashMap<>();
    }
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Get or create timestamp log for client
     * 2. Remove expired timestamps from front of deque
     *    - while (log not empty && oldest timestamp is expired) remove it
     * 3. If log.size() < maxRequests, add current timestamp and return true
     * 4. Otherwise return false (limit exceeded)
     * 
     * TIME COMPLEXITY: O(n) where n = number of timestamps to evict
     * 
     * @param clientId Client making the request
     * @return true if allowed, false if denied
     */
    @Override
    public synchronized boolean allowRequest(String clientId) {
        // TODO: Implement
        // HINT: long now = System.currentTimeMillis();
        // HINT: Deque<Long> log = logs.computeIfAbsent(clientId, k -> new ArrayDeque<>());
        // HINT: while (!log.isEmpty() && (now - log.peekFirst()) >= windowMillis) {
        //           log.pollFirst();
        //       }
        // HINT: if (log.size() < maxRequests) { log.addLast(now); return true; }
        // HINT: return false;
        return false;
    }
    
    @Override
    public String getName() { 
        return "SlidingWindowLog(max=" + maxRequests + ",window=" + windowMillis + "ms)"; 
    }
    
    @Override
    public String getStats(String clientId) {
        // TODO: Implement
        // HINT: Return number of requests in current window
        return "not implemented";
    }
}

// ===== RATE LIMITER SERVICE =====

/**
 * Service managing rate limiters for different endpoints
 */
class RateLimiterService {
    private final Map<String, RateLimiter> endpointLimiters;
    private final RateLimiter defaultLimiter;
    private final AtomicInteger totalAllowed = new AtomicInteger(0);
    private final AtomicInteger totalDenied = new AtomicInteger(0);
    
    public RateLimiterService(RateLimiter defaultLimiter) {
        this.endpointLimiters = new ConcurrentHashMap<>();
        this.defaultLimiter = defaultLimiter;
    }
    
    /**
     * Register a rate limiter for specific endpoint
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate endpoint and limiter not null
     * 2. Store in endpointLimiters map
     * 
     * @param endpoint API endpoint (e.g., "/api/login")
     * @param limiter Rate limiter for this endpoint
     */
    public void registerEndpoint(String endpoint, RateLimiter limiter) {
        // TODO: Implement
        // HINT: if (endpoint == null || limiter == null) return;
        // HINT: endpointLimiters.put(endpoint, limiter);
        // HINT: System.out.println("Registered: " + endpoint + " → " + limiter.getName());
    }
    
    /**
     * Process a request
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get limiter for endpoint (or use default)
     * 2. Call limiter.allowRequest(clientId)
     * 3. Update totalAllowed or totalDenied counters
     * 4. Return ALLOWED or DENIED
     * 
     * @param endpoint API endpoint
     * @param clientId Client identifier
     * @return ALLOWED if within limit, DENIED otherwise
     */
    public RequestResult processRequest(String endpoint, String clientId) {
        // TODO: Implement
        // HINT: RateLimiter limiter = endpointLimiters.getOrDefault(endpoint, defaultLimiter);
        // HINT: boolean allowed = limiter.allowRequest(clientId);
        // HINT: if (allowed) totalAllowed.incrementAndGet(); else totalDenied.incrementAndGet();
        // HINT: return allowed ? RequestResult.ALLOWED : RequestResult.DENIED;
        return null;
    }
    
    /**
     * Batch requests for testing
     * 
     * IMPLEMENTATION HINTS:
     * 1. Loop count times
     * 2. Call processRequest for each
     * 3. Track allowed and denied counts
     * 4. Return [allowed, denied]
     * 
     * @param endpoint API endpoint
     * @param clientId Client ID
     * @param count Number of requests to send
     * @return Array [allowed, denied]
     */
    public int[] batchRequests(String endpoint, String clientId, int count) {
        // TODO: Implement
        // HINT: int allowed = 0, denied = 0;
        // HINT: for (int i = 0; i < count; i++) {
        //     if (processRequest(endpoint, clientId) == RequestResult.ALLOWED) allowed++; else denied++;
        // }
        // HINT: return new int[]{allowed, denied};
        return new int[]{0, 0};
    }
    
    /**
     * Display service status
     */
    public void displayStatus() {
        System.out.println("\n--- Rate Limiter Service Status ---");
        System.out.println("Default Limiter: " + defaultLimiter.getName());
        System.out.println("Endpoint-specific limiters:");
        endpointLimiters.forEach((ep, rl) -> 
            System.out.println("  " + ep + " → " + rl.getName()));
        System.out.println("Total: " + totalAllowed.get() + " allowed, " + 
                         totalDenied.get() + " denied");
    }
}

// ===== MAIN TEST CLASS =====

public class RateLimiterSystem {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Rate Limiter System Test Cases ===\n");
        
        // Setup service with different limiters per endpoint
        RateLimiter defaultLimiter = new TokenBucketRateLimiter(5, 2.0);
        RateLimiterService service = new RateLimiterService(defaultLimiter);
        
        service.registerEndpoint("/api/login", new FixedWindowRateLimiter(3, 1000));
        service.registerEndpoint("/api/search", new SlidingWindowLogRateLimiter(5, 2000));
        service.registerEndpoint("/api/payment", new TokenBucketRateLimiter(2, 0.5));
        
        service.displayStatus();
        
        // Test Case 1: Token Bucket - Burst Capability
        System.out.println("\n=== Test Case 1: TOKEN BUCKET (burst of 8) ===");
        int[] result = service.batchRequests("/api/default", "client-A", 8);
        System.out.println("Sent 8 burst requests: " + result[0] + " allowed, " + result[1] + " denied");
        System.out.println("✓ Allowed 5 (capacity): " + (result[0] == 5));
        System.out.println("✓ Denied 3 (over capacity): " + (result[1] == 3));
        System.out.println();
        
        // Test Case 2: Token Bucket - Refill After Wait
        System.out.println("=== Test Case 2: TOKEN BUCKET (refill after 1s) ===");
        Thread.sleep(1000);  // Wait 1 second for 2 tokens to refill
        result = service.batchRequests("/api/default", "client-A", 3);
        System.out.println("After 1s wait, sent 3 requests: " + result[0] + " allowed");
        System.out.println("✓ Got ~2 tokens back: " + (result[0] >= 2));
        System.out.println();
        
        // Test Case 3: Fixed Window - Exact Limit
        System.out.println("=== Test Case 3: FIXED WINDOW (3 req/sec) ===");
        result = service.batchRequests("/api/login", "client-B", 6);
        System.out.println("Sent 6 requests: " + result[0] + " allowed, " + result[1] + " denied");
        System.out.println("✓ Exactly 3 allowed per window: " + (result[0] == 3));
        System.out.println();
        
        // Test Case 4: Fixed Window - Window Reset
        System.out.println("=== Test Case 4: FIXED WINDOW (reset after 1s) ===");
        Thread.sleep(1100);  // Wait for window to reset
        result = service.batchRequests("/api/login", "client-B", 4);
        System.out.println("After window reset, sent 4: " + result[0] + " allowed");
        System.out.println("✓ New window allows 3: " + (result[0] == 3));
        System.out.println();
        
        // Test Case 5: Sliding Window Log - Accurate Limiting
        System.out.println("=== Test Case 5: SLIDING WINDOW LOG (5 in 2s) ===");
        result = service.batchRequests("/api/search", "client-C", 7);
        System.out.println("Sent 7 requests: " + result[0] + " allowed, " + result[1] + " denied");
        System.out.println("✓ Exactly 5 allowed: " + (result[0] == 5));
        System.out.println();
        
        // Test Case 6: Sliding Window - Time-based Eviction
        System.out.println("=== Test Case 6: SLIDING WINDOW (evict after 2s) ===");
        Thread.sleep(2100);  // Wait for window to slide
        result = service.batchRequests("/api/search", "client-C", 5);
        System.out.println("After 2s, sent 5: " + result[0] + " allowed");
        System.out.println("✓ Old timestamps evicted: " + (result[0] == 5));
        System.out.println();
        
        // Test Case 7: Per-Client Isolation
        System.out.println("=== Test Case 7: PER-CLIENT ISOLATION ===");
        int[] r1 = service.batchRequests("/api/login", "isolated-client-1", 3);
        int[] r2 = service.batchRequests("/api/login", "isolated-client-2", 3);
        System.out.println("Client-1: " + r1[0] + " allowed");
        System.out.println("Client-2: " + r2[0] + " allowed");
        System.out.println("✓ Both got full quota: " + (r1[0] == 3 && r2[0] == 3));
        System.out.println();
        
        // Test Case 8: Different Endpoints Different Limits
        System.out.println("=== Test Case 8: ENDPOINT-SPECIFIC LIMITS ===");
        result = service.batchRequests("/api/payment", "client-D", 4);
        System.out.println("Payment (strict): " + result[0] + " allowed out of 4");
        result = service.batchRequests("/api/search", "client-D", 4);
        System.out.println("Search (lenient): " + result[0] + " allowed out of 4");
        System.out.println("✓ Different limits for different endpoints");
        System.out.println();
        
        service.displayStatus();
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION TOPICS:
 * ============================
 * 
 * 1. ALGORITHM COMPARISON:
 *    
 *    Token Bucket:
 *      - Pros: Smooth, allows bursts, memory efficient
 *      - Cons: Complex refill logic, floating point math
 *      - Use: APIs that tolerate bursts (search, browse)
 *      - Example: 5 capacity, 2/s rate → can handle 5 burst then 2/s sustained
 *    
 *    Fixed Window:
 *      - Pros: Simple, predictable, memory efficient
 *      - Cons: Boundary burst problem (2x at window edges)
 *      - Use: Login endpoints, simple limits
 *      - Example: At 00:00:59 get 10 req, at 00:01:01 get 10 more = 20 in 2 seconds!
 *    
 *    Sliding Window Log:
 *      - Pros: Most accurate, no burst problem
 *      - Cons: Higher memory (stores all timestamps), O(n) eviction
 *      - Use: Payment APIs, strict limits, write-heavy operations
 *      - Example: True sliding window - always checks last N milliseconds
 *    
 *    Sliding Window Counter (Hybrid):
 *      - Pros: Balances accuracy and memory
 *      - Cons: More complex implementation
 *      - Formula: weighted count from current + previous window
 * 
 * 2. DISTRIBUTED RATE LIMITING:
 *    Redis-based Implementation:
 *      - Use Redis INCR + EXPIRE for Fixed Window
 *      - Use Redis Sorted Sets (ZADD/ZREMRANGEBYSCORE) for Sliding Window
 *      - Lua scripts for atomic operations
 *    
 *    Consistency vs Availability:
 *      - Strict: Central rate limiter (single source of truth)
 *      - Relaxed: Local limiters with eventual consistency
 *      - Trade-off: Accuracy vs latency vs availability
 *    
 *    Example Redis Commands:
 *      ```
 *      INCR rate:client123
 *      EXPIRE rate:client123 1
 *      ```
 * 
 * 3. THREAD SAFETY:
 *    - synchronized on allowRequest (critical section)
 *    - ConcurrentHashMap for client storage
 *    - AtomicInteger for global counters
 *    - Alternative: Lock-free algorithms with CAS
 * 
 * 4. MEMORY OPTIMIZATION:
 *    - TTL for client entries (remove after inactivity)
 *    - Approximate algorithms (Count-Min Sketch, Bloom Filter)
 *    - Trade-off: Memory vs accuracy
 *    - Consider LRU eviction for rarely-used clients
 * 
 * 5. ADVANCED FEATURES:
 *    - Priority tiers (premium users get higher limits)
 *    - Dynamic rate adjustment (scale based on system load)
 *    - Rate limit headers (X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset)
 *    - Graceful degradation (allow some requests even over limit)
 *    - Whitelisting/Blacklisting
 *    - Geographic rate limits
 * 
 * 6. REAL-WORLD IMPLEMENTATION:
 *    API Gateway Level:
 *      - AWS API Gateway: Token bucket
 *      - Kong, Tyk: Configurable algorithms
 *      - Nginx: limit_req module (leaky bucket variant)
 *    
 *    Application Level:
 *      - Guava RateLimiter (Token Bucket)
 *      - Bucket4j (Java library)
 *      - Resilience4j (Circuit breaker + rate limiter)
 * 
 * 7. MONITORING & OBSERVABILITY:
 *    Metrics to Track:
 *      - Rate limit hit rate (denied / total)
 *      - Per-client rate limit status
 *      - 429 error rate
 *      - P99 latency of rate limiter check
 *    
 *    Alerts:
 *      - High denial rate (might need limit increase)
 *      - Suspicious patterns (potential attack)
 * 
 * 8. SECURITY CONSIDERATIONS:
 *    - DDoS protection (rate limit by IP)
 *    - Credential stuffing prevention (strict login limits)
 *    - API key-based limits
 *    - Captcha integration after threshold
 * 
 * 9. EDGE CASES:
 *    - System clock changes (use monotonic time)
 *    - Integer overflow (use long for timestamps)
 *    - Concurrent requests from same client
 *    - Rate limiter service restarts (state loss)
 * 
 * 10. API DESIGN:
 *     POST /ratelimit/check    - Check if request allowed
 *     GET  /ratelimit/status   - Get client's current status
 *     POST /ratelimit/config   - Configure endpoint limits (admin)
 *     GET  /ratelimit/stats    - Get global statistics
 *     DELETE /ratelimit/client/{id} - Reset client's limit
 */
