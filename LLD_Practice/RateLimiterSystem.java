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
    
    public RateLimitExceededException(String clientId, String message) {
        super("Rate limit exceeded for client " + clientId + ": " + message);
        this.clientId = clientId;
    }
    
    public String getClientId() { return clientId; }
}

/**
 * Exception thrown when rate limiter configuration is invalid
 */
class InvalidRateLimiterConfigException extends Exception {
    public InvalidRateLimiterConfigException(String message) {
        super(message);
    }
}

// ===== ENUMS =====

enum RateLimiterType { 
    TOKEN_BUCKET, 
    FIXED_WINDOW, 
    SLIDING_WINDOW_LOG 
}

enum RequestResult { 
    ALLOWED, 
    DENIED 
}

// ===== INTERFACE =====

interface RateLimiter {
    boolean allowRequest(String clientId);
    String getName();
    String getStats(String clientId);
}

/**
 * Rate Limiter System - Low Level Design
 * 
 * PROBLEM STATEMENT:
 * Design a rate limiting system that can:
 * 1. Limit requests per client using different algorithms
 * 2. Support multiple strategies (Token Bucket, Fixed Window, Sliding Window)
 * 3. Handle concurrent requests safely
 * 4. Track statistics
 * 
 * INTERVIEW HINTS:
 * - Discuss trade-offs between algorithms
 * - Talk about distributed rate limiting (Redis-based)
 * - Mention thread safety (synchronized, ConcurrentHashMap)
 * - Consider memory vs accuracy trade-offs
 */

// ===== TOKEN BUCKET ALGORITHM =====

/**
 * TOKEN BUCKET ALGORITHM
 * ======================
 * HOW IT WORKS:
 *   - Bucket holds tokens (max = capacity)
 *   - Tokens refill at constant rate
 *   - Each request consumes 1 token
 *   - If tokens available → ALLOW, else → DENY
 * 
 * PROS:
 *   - Allows bursts up to capacity
 *   - Smooth rate limiting
 *   - Memory efficient
 * 
 * CONS:
 *   - Requires timestamp tracking per client
 * 
 * DATA STRUCTURE:
 *   ConcurrentHashMap<clientId, double[2]>
 *   [0] = current tokens (double for fractional)
 *   [1] = last refill timestamp
 */
class TokenBucketRateLimiter implements RateLimiter {
    private final int capacity;
    private final double refillRate;
    private final ConcurrentHashMap<String, double[]> buckets;
    
    public TokenBucketRateLimiter(int capacity, double refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.buckets = new ConcurrentHashMap<>();
    }
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Get or create bucket for client (start with full capacity)
     * 2. Calculate elapsed time since last refill
     * 3. Add tokens: min(capacity, current + elapsed * refillRate)
     * 4. If tokens >= 1, consume and return true
     * 5. Otherwise return false
     */
    @Override
    public synchronized boolean allowRequest(String clientId) {
        // TODO: Implement
        // HINT: buckets.computeIfAbsent(clientId, k -> new double[]{capacity, System.nanoTime()});
        // HINT: double elapsed = (now - bucket[1]) / 1_000_000_000.0;
        // HINT: bucket[0] = Math.min(capacity, bucket[0] + elapsed * refillRate);
        return false;
    }
    
    @Override
    public String getName() { 
        return "TokenBucket(cap=" + capacity + ",rate=" + refillRate + "/s)"; 
    }
    
    @Override
    public String getStats(String clientId) {
        // TODO: Implement
        // HINT: Return current token count for client
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
 * PROS:
 *   - Simple to implement
 *   - Memory efficient (just counter + timestamp)
 * 
 * CONS:
 *   - Boundary burst problem (2x requests at window edges)
 * 
 * DATA STRUCTURE:
 *   ConcurrentHashMap<clientId, long[2]>
 *   [0] = window start timestamp
 *   [1] = request count in current window
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
     * 4. If count < maxRequests, increment and return true
     * 5. Otherwise return false
     */
    @Override
    public synchronized boolean allowRequest(String clientId) {
        // TODO: Implement
        // HINT: windows.computeIfAbsent(clientId, k -> new long[]{now, 0});
        // HINT: Check window expiration and reset if needed
        return false;
    }
    
    @Override
    public String getName() { 
        return "FixedWindow(max=" + maxRequests + ",window=" + windowMillis + "ms)"; 
    }
    
    @Override
    public String getStats(String clientId) {
        // TODO: Implement
        return "not implemented";
    }
}

// ===== SLIDING WINDOW LOG ALGORITHM =====

/**
 * SLIDING WINDOW LOG ALGORITHM
 * =============================
 * HOW IT WORKS:
 *   - Store exact timestamp of each request
 *   - On new request: evict timestamps older than window
 *   - If remaining count < max → ALLOW, else → DENY
 * 
 * PROS:
 *   - Most accurate (no boundary burst problem)
 *   - True sliding window
 * 
 * CONS:
 *   - Higher memory usage (stores all timestamps)
 *   - O(n) eviction per request
 * 
 * DATA STRUCTURE:
 *   ConcurrentHashMap<clientId, Deque<Long>>
 *   Deque stores timestamps in chronological order
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
     * 2. Remove expired timestamps: while (now - log.peekFirst() >= windowMillis) log.pollFirst()
     * 3. If log.size() < maxRequests, add timestamp and return true
     * 4. Otherwise return false
     */
    @Override
    public synchronized boolean allowRequest(String clientId) {
        // TODO: Implement
        // HINT: logs.computeIfAbsent(clientId, k -> new ArrayDeque<>());
        // HINT: Evict old timestamps from front of deque
        return false;
    }
    
    @Override
    public String getName() { 
        return "SlidingWindowLog(max=" + maxRequests + ",window=" + windowMillis + "ms)"; 
    }
    
    @Override
    public String getStats(String clientId) {
        // TODO: Implement
        return "not implemented";
    }
}

// ===== RATE LIMITER SERVICE =====

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
     */
    public void registerEndpoint(String endpoint, RateLimiter limiter) {
        endpointLimiters.put(endpoint, limiter);
    }
    
    /**
     * Process a request
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get limiter for endpoint (or use default)
     * 2. Call limiter.allowRequest(clientId)
     * 3. Update totalAllowed or totalDenied counters
     * 4. Return ALLOWED or DENIED
     */
    public RequestResult processRequest(String endpoint, String clientId) {
        // TODO: Implement
        // HINT: RateLimiter limiter = endpointLimiters.getOrDefault(endpoint, defaultLimiter);
        // HINT: if (allowed) totalAllowed.incrementAndGet();
        return null;
    }
    
    /**
     * Batch requests for testing
     */
    public int[] batchRequests(String endpoint, String clientId, int count) {
        // TODO: Implement
        // HINT: Loop count times, track allowed vs denied
        return new int[]{0, 0};
    }
    
    /**
     * Display service status
     */
    public void displayStatus() {
        System.out.println("\n--- Rate Limiter Service Status ---");
        System.out.println("Default: " + defaultLimiter.getName());
        System.out.println("Endpoints:");
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
        
        // Setup
        RateLimiter defaultLimiter = new TokenBucketRateLimiter(5, 2);
        RateLimiterService service = new RateLimiterService(defaultLimiter);
        
        service.registerEndpoint("/api/login", new FixedWindowRateLimiter(3, 1000));
        service.registerEndpoint("/api/search", new SlidingWindowLogRateLimiter(5, 2000));
        service.registerEndpoint("/api/payment", new TokenBucketRateLimiter(2, 0.5));
        
        service.displayStatus();
        
        // Test 1: Token Bucket - Burst
        System.out.println("\n=== Test 1: TOKEN BUCKET (burst) ===");
        int[] result = service.batchRequests("/api/default", "client-A", 8);
        System.out.println("Sent 8 requests: " + result[0] + " allowed, " + result[1] + " denied");
        System.out.println("✓ Expected 5 allowed (burst): " + (result[0] == 5));
        
        // Test 2: Token Bucket - Refill
        System.out.println("\n=== Test 2: TOKEN BUCKET (refill after 1s) ===");
        Thread.sleep(1000);
        result = service.batchRequests("/api/default", "client-A", 3);
        System.out.println("After 1s, sent 3: " + result[0] + " allowed");
        
        // Test 3: Fixed Window
        System.out.println("\n=== Test 3: FIXED WINDOW (3 req/sec) ===");
        result = service.batchRequests("/api/login", "client-B", 6);
        System.out.println("Sent 6: " + result[0] + " allowed, " + result[1] + " denied");
        System.out.println("✓ Expected exactly 3: " + (result[0] == 3));
        
        // Test 4: Fixed Window Reset
        System.out.println("\n=== Test 4: FIXED WINDOW (reset) ===");
        Thread.sleep(1100);
        result = service.batchRequests("/api/login", "client-B", 4);
        System.out.println("After window reset: " + result[0] + " allowed");
        
        // Test 5: Sliding Window Log
        System.out.println("\n=== Test 5: SLIDING WINDOW LOG ===");
        result = service.batchRequests("/api/search", "client-C", 7);
        System.out.println("Sent 7: " + result[0] + " allowed");
        System.out.println("✓ Expected 5: " + (result[0] == 5));
        
        // Test 6: Per-Client Isolation
        System.out.println("\n=== Test 6: PER-CLIENT ISOLATION ===");
        int[] r1 = service.batchRequests("/api/login", "isolated-1", 3);
        int[] r2 = service.batchRequests("/api/login", "isolated-2", 3);
        System.out.println("Client-1: " + r1[0] + " allowed");
        System.out.println("Client-2: " + r2[0] + " allowed");
        System.out.println("✓ Both got full quota: " + (r1[0] == 3 && r2[0] == 3));
        
        service.displayStatus();
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION TOPICS:
 * ============================
 * 
 * 1. ALGORITHM COMPARISON:
 *    Token Bucket:
 *      - Pros: Smooth, allows bursts, memory efficient
 *      - Cons: Complex refill logic
 *      - Use: APIs that tolerate bursts (search, browse)
 *    
 *    Fixed Window:
 *      - Pros: Simple, predictable, memory efficient
 *      - Cons: Boundary burst problem (2x at edges)
 *      - Use: Login endpoints, simple limits
 *    
 *    Sliding Window Log:
 *      - Pros: Most accurate, no burst problem
 *      - Cons: Higher memory (stores all timestamps)
 *      - Use: Payment APIs, strict limits
 * 
 * 2. DISTRIBUTED RATE LIMITING:
 *    - Use Redis with INCR + EXPIRE for counters
 *    - Redis Sorted Sets for sliding window
 *    - Consider consistency vs availability trade-off
 * 
 * 3. THREAD SAFETY:
 *    - Use synchronized on allowRequest
 *    - ConcurrentHashMap for client storage
 *    - AtomicInteger for counters
 * 
 * 4. ADVANCED FEATURES:
 *    - Priority tiers (premium users get higher limits)
 *    - Dynamic rate adjustment
 *    - Rate limit headers (X-RateLimit-Remaining)
 *    - Graceful degradation
 * 
 * 5. SCALABILITY:
 *    - Centralized (single server) vs Distributed (Redis)
 *    - Approximate algorithms (Count-Min Sketch)
 *    - Sticky sessions for better cache hit rate
 */
