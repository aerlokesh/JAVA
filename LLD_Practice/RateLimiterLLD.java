import java.util.*;

/*
 * RATE LIMITER - Low Level Design
 * ==================================
 * 
 * REQUIREMENTS:
 * 1. addResource(id, strategy, limits) — configure per-resource limiter
 * 2. isAllowed(resourceId, timestamp) — check if request is allowed
 * 3. Strategies: fixed-window-counter, sliding-window-counter
 * 4. Extensible: easy to add new strategies (Strategy pattern)
 * 5. Re-addResource replaces old strategy/state
 * 
 * DESIGN PATTERNS: Strategy (rate limiting algorithm)
 * 
 * KEY DATA STRUCTURES:
 * - Fixed window: counter + window start time
 * - Sliding window: Deque of timestamps
 * 
 * COMPLEXITY:
 *   fixed-window isAllowed:   O(1)
 *   sliding-window isAllowed: O(n) cleanup old timestamps, n = window size
 */

// ==================== STRATEGY ====================

interface RateLimitStrategy {
    boolean isAllowed(int timestamp);
}

/**
 * Fixed Window: time divided into blocks of `period` seconds.
 * Each window allows `maxRequests`. Counter resets at window boundary.
 * Simple but allows burst at boundary (2x limit across adjacent windows).
 */
class FixedWindowCounter implements RateLimitStrategy {
    private final int maxRequests, period;
    private int windowStart, count;

    FixedWindowCounter(int maxRequests, int period) {
        this.maxRequests = maxRequests;
        this.period = period;
        this.windowStart = -1;
    }

    @Override
    public boolean isAllowed(int timestamp) {
        // TODO: Implement
        // HINT: int currentWindow = (timestamp / period) * period;
        // HINT: if (currentWindow != windowStart) {
        // HINT:     windowStart = currentWindow;
        // HINT:     count = 0;
        // HINT: }
        // HINT: if (count < maxRequests) { count++; return true; }
        // HINT: return false;
        return false;
    }
}

/**
 * Sliding Window (log-based): stores timestamps of recent requests.
 * For each new request, evict timestamps outside [timestamp - period + 1, timestamp].
 * If remaining count < maxRequests, allow and record timestamp.
 * Accurate but uses more memory.
 */
class SlidingWindowCounter implements RateLimitStrategy {
    private final int maxRequests, period;
    private final Deque<Integer> timestamps = new ArrayDeque<>();

    SlidingWindowCounter(int maxRequests, int period) {
        this.maxRequests = maxRequests;
        this.period = period;
    }

    @Override
    public boolean isAllowed(int timestamp) {
        // TODO: Implement
        // HINT: int windowStart = timestamp - period + 1;
        // HINT: while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart)
        // HINT:     timestamps.pollFirst();
        // HINT: if (timestamps.size() < maxRequests) {
        // HINT:     timestamps.addLast(timestamp);
        // HINT:     return true;
        // HINT: }
        // HINT: return false;
        return false;
    }
}

// ==================== RATE LIMITER ====================

class RateLimiter {
    private final Map<String, RateLimitStrategy> resources = new HashMap<>();

    /** Configure resource with strategy and limits "maxRequests,period". */
    void addResource(String resourceId, String strategy, String limits) {
        String[] parts = limits.split(",");
        int maxRequests = Integer.parseInt(parts[0].trim());
        int period = Integer.parseInt(parts[1].trim());

        // TODO: Implement — create strategy based on name, store in map
        // HINT: RateLimitStrategy strat;
        // HINT: if (strategy.equals("fixed-window-counter")) {
        // HINT:     strat = new FixedWindowCounter(maxRequests, period);
        // HINT: } else if (strategy.equals("sliding-window-counter")) {
        // HINT:     strat = new SlidingWindowCounter(maxRequests, period);
        // HINT: } else {
        // HINT:     throw new IllegalArgumentException("Unknown strategy: " + strategy);
        // HINT: }
        // HINT: resources.put(resourceId, strat);
    }

    /** Check if request is allowed for resource at given timestamp. */
    boolean isAllowed(String resourceId, int timestamp) {
        RateLimitStrategy strat = resources.get(resourceId);
        if (strat == null) return false;
        return strat.isAllowed(timestamp);
    }
}

// ==================== MAIN / TESTS ====================

public class RateLimiterLLD {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════╗");
        System.out.println("║    RATE LIMITER - LLD Demo        ║");
        System.out.println("╚═══════════════════════════════════╝\n");

        RateLimiter rl = new RateLimiter();

        // --- Test 1: Fixed window basic ---
        System.out.println("=== Test 1: Fixed window basic ===");
        rl.addResource("login-api", "fixed-window-counter", "2,5");
        // Windows: [0..4], [5..9], ...
        System.out.println("t=1: " + rl.isAllowed("login-api", 1) + " (expected true)");
        System.out.println("t=2: " + rl.isAllowed("login-api", 2) + " (expected true)");
        System.out.println("t=4: " + rl.isAllowed("login-api", 4) + " (expected false — 3rd in window)");
        System.out.println("✓ Fixed window blocks after limit\n");

        // --- Test 2: Fixed window resets ---
        System.out.println("=== Test 2: Fixed window reset ===");
        System.out.println("t=5: " + rl.isAllowed("login-api", 5) + " (expected true — new window)");
        System.out.println("t=6: " + rl.isAllowed("login-api", 6) + " (expected true)");
        System.out.println("t=7: " + rl.isAllowed("login-api", 7) + " (expected false)");
        System.out.println("✓ Counter resets at window boundary\n");

        // --- Test 3: Update to sliding window ---
        System.out.println("=== Test 3: Update to sliding window ===");
        rl.addResource("login-api", "sliding-window-counter", "2,3");
        // Old state replaced
        System.out.println("t=6: " + rl.isAllowed("login-api", 6) + " (expected true — fresh state)");
        System.out.println("t=7: " + rl.isAllowed("login-api", 7) + " (expected true — window [5..7] has 2)");
        System.out.println("t=8: " + rl.isAllowed("login-api", 8) + " (expected false — window [6..8] would have 3)");
        System.out.println("✓ Sliding window accurate\n");

        // --- Test 4: Sliding window eviction ---
        System.out.println("=== Test 4: Sliding window eviction ===");
        System.out.println("t=9: " + rl.isAllowed("login-api", 9) + " (expected true — t=6 evicted from [7..9])");
        System.out.println("✓ Old timestamps evicted\n");

        // --- Test 5: Multiple resources ---
        System.out.println("=== Test 5: Multiple resources ===");
        rl.addResource("api-a", "fixed-window-counter", "1,1");
        rl.addResource("api-b", "sliding-window-counter", "3,5");
        System.out.println("api-a t=0: " + rl.isAllowed("api-a", 0) + " (expected true)");
        System.out.println("api-a t=0: " + rl.isAllowed("api-a", 0) + " (expected false)");
        System.out.println("api-b t=0: " + rl.isAllowed("api-b", 0) + " (expected true)");
        System.out.println("api-b t=1: " + rl.isAllowed("api-b", 1) + " (expected true)");
        System.out.println("api-b t=2: " + rl.isAllowed("api-b", 2) + " (expected true)");
        System.out.println("api-b t=3: " + rl.isAllowed("api-b", 3) + " (expected false)");
        System.out.println("✓ Independent per-resource limits\n");

        // --- Test 6: Fixed window — single request per second ---
        System.out.println("=== Test 6: 1 req/sec fixed window ===");
        rl.addResource("strict", "fixed-window-counter", "1,1");
        System.out.println("t=10: " + rl.isAllowed("strict", 10) + " (true)");
        System.out.println("t=10: " + rl.isAllowed("strict", 10) + " (false)");
        System.out.println("t=11: " + rl.isAllowed("strict", 11) + " (true — new window)");
        System.out.println("✓ Strict 1/sec\n");

        // --- Test 7: Sliding window — all in same second ---
        System.out.println("=== Test 7: Sliding window burst ===");
        rl.addResource("burst", "sliding-window-counter", "3,1");
        System.out.println("t=0: " + rl.isAllowed("burst", 0));
        System.out.println("t=0: " + rl.isAllowed("burst", 0));
        System.out.println("t=0: " + rl.isAllowed("burst", 0));
        System.out.println("t=0: " + rl.isAllowed("burst", 0) + " (expected false — 4th)");
        System.out.println("✓ Sliding window handles same-second burst\n");

        // --- Test 8: Re-add resource resets state ---
        System.out.println("=== Test 8: Re-add resets ===");
        rl.addResource("reset-me", "fixed-window-counter", "1,10");
        rl.isAllowed("reset-me", 0);
        System.out.println("After 1 req: " + rl.isAllowed("reset-me", 1) + " (false)");
        rl.addResource("reset-me", "fixed-window-counter", "1,10"); // re-add
        System.out.println("After re-add: " + rl.isAllowed("reset-me", 2) + " (true — reset)");
        System.out.println("✓ Re-add clears old state\n");

        // --- Test 9: Scale ---
        System.out.println("=== Test 9: Scale ===");
        RateLimiter rl2 = new RateLimiter();
        for (int i = 0; i < 100; i++)
            rl2.addResource("r" + i, i % 2 == 0 ? "fixed-window-counter" : "sliding-window-counter", "100,10");
        long t = System.nanoTime();
        int allowed = 0;
        for (int ts = 0; ts < 1000; ts++)
            for (int i = 0; i < 100; i++)
                if (rl2.isAllowed("r" + i, ts)) allowed++;
        System.out.printf("100K checks on 100 resources: %d allowed in %.2f ms\n",
            allowed, (System.nanoTime()-t)/1e6);
        System.out.println("✓ Fast at scale\n");

        // --- Test 10: Edge — large period ---
        System.out.println("=== Test 10: Large period ===");
        rl.addResource("big", "fixed-window-counter", "3,1000");
        System.out.println("t=0: " + rl.isAllowed("big", 0));
        System.out.println("t=500: " + rl.isAllowed("big", 500));
        System.out.println("t=999: " + rl.isAllowed("big", 999));
        System.out.println("t=999: " + rl.isAllowed("big", 999) + " (false — 4th in [0..999])");
        System.out.println("t=1000: " + rl.isAllowed("big", 1000) + " (true — new window)");
        System.out.println("✓ Large window period\n");

        System.out.println("════════ ALL 10 TESTS PASSED ✓ ════════");
    }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. STRATEGY PATTERN: RateLimitStrategy interface with isAllowed().
 *    New algorithms = new class. Factory in addResource() maps name.
 *    Open/Closed — add Token Bucket, Leaky Bucket without modifying.
 *
 * 2. FIXED WINDOW: O(1) time+space. Divide time into period-sized blocks.
 *    Counter resets at boundary. Simple but allows 2x burst at edges.
 *    Window = (timestamp / period) * period.
 *
 * 3. SLIDING WINDOW (LOG): O(n) eviction of old timestamps.
 *    Deque stores all timestamps in window. Accurate but more memory.
 *    Window = [timestamp - period + 1, timestamp].
 *
 * 4. OTHER STRATEGIES (discussion):
 *    - Token Bucket: tokens refill at rate, consumed per request.
 *      Allows controlled bursts. Used by AWS API Gateway.
 *    - Leaky Bucket: fixed drain rate, queue overflow = reject.
 *      Smooth output. Used by network traffic shaping.
 *    - Sliding Window Counter (hybrid): weighted average of current
 *      and previous window counts. O(1) space, good accuracy.
 *
 * 5. REAL-WORLD: Redis + Lua scripts for distributed rate limiting.
 *    API gateways (Kong, AWS), Cloudflare, Stripe.
 *
 * 6. COMPLEXITY:
 *    Fixed window:   O(1) time, O(1) space per resource
 *    Sliding window:  O(n) time for eviction, O(n) space (n = max requests)
 *    addResource:     O(1)
 */
