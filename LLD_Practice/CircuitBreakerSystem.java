import java.util.concurrent.atomic.AtomicInteger;

// ===== ENUMS =====

enum CircuitState { CLOSED, OPEN, HALF_OPEN }

// ===== INTERFACE =====

/**
 * Remote service call — wraps any operation that can fail
 */
interface RemoteCall<T> {
    T execute() throws Exception;
}

// ===== DOMAIN CLASS =====

/**
 * Circuit Breaker - Low Level Design (LLD)
 * 
 * PROBLEM: Design a circuit breaker that prevents cascading failures
 * 
 * STATE MACHINE:
 *   CLOSED  → failures >= threshold → OPEN
 *   OPEN    → timeout expires       → HALF_OPEN
 *   HALF_OPEN → success             → CLOSED
 *   HALF_OPEN → failure             → OPEN
 * 
 *   CLOSED:    Normal operation, requests go through
 *   OPEN:      Fail fast, don't even try (service is down)
 *   HALF_OPEN: Let ONE request through to test if service recovered
 * 
 * KEY INTERVIEW POINTS:
 * - Prevents thundering herd on failing service
 * - Gives failing service time to recover
 * - Fail fast = better UX than slow timeout
 * - Used in: Netflix Hystrix, Resilience4j, AWS SDK
 */
class CircuitBreaker {
    private final String name;
    private CircuitState state;
    private final int failureThreshold;      // failures before OPEN
    private final long openTimeoutMs;         // how long to stay OPEN before HALF_OPEN
    private final int halfOpenMaxAttempts;    // max test requests in HALF_OPEN
    
    private int failureCount;
    private int successCount;
    private int halfOpenAttempts;
    private long lastFailureTime;             // when we entered OPEN state
    private final AtomicInteger totalRequests;
    private final AtomicInteger totalFailures;
    private final AtomicInteger totalRejected; // rejected while OPEN
    
    public CircuitBreaker(String name, int failureThreshold, long openTimeoutMs, int halfOpenMaxAttempts) {
        this.name = name;
        this.state = CircuitState.CLOSED;
        this.failureThreshold = failureThreshold;
        this.openTimeoutMs = openTimeoutMs;
        this.halfOpenMaxAttempts = halfOpenMaxAttempts;
        this.failureCount = 0;
        this.successCount = 0;
        this.halfOpenAttempts = 0;
        this.lastFailureTime = 0;
        this.totalRequests = new AtomicInteger(0);
        this.totalFailures = new AtomicInteger(0);
        this.totalRejected = new AtomicInteger(0);
    }
    
    /**
     * Execute a call through the circuit breaker
     * 
     * IMPLEMENTATION HINTS:
     * 1. Check if state should transition (OPEN → HALF_OPEN if timeout expired)
     * 2. If OPEN → reject immediately (throw CircuitOpenException)
     * 3. If HALF_OPEN → allow limited attempts, if maxed out → reject
     * 4. Try to execute the call:
     *    a. On success → call onSuccess()
     *    b. On failure → call onFailure()
     * 5. Increment totalRequests
     * 6. Return result
     * 
     * STATE TRANSITIONS:
     *   CLOSED + failure count >= threshold → OPEN
     *   OPEN + timeout expired → HALF_OPEN
     *   HALF_OPEN + success → CLOSED
     *   HALF_OPEN + failure → OPEN
     */
    public synchronized <T> T execute(RemoteCall<T> call) throws Exception {
        // TODO: Implement
        // HINT: totalRequests.incrementAndGet();
        // HINT: checkStateTransition();
        //
        // HINT: if (state == CircuitState.OPEN) {
        //     totalRejected.incrementAndGet();
        //     throw new CircuitOpenException(name, "Circuit is OPEN — failing fast");
        // }
        //
        // HINT: if (state == CircuitState.HALF_OPEN && halfOpenAttempts >= halfOpenMaxAttempts) {
        //     totalRejected.incrementAndGet();
        //     throw new CircuitOpenException(name, "Circuit is HALF_OPEN — max attempts reached");
        // }
        //
        // HINT: try {
        //     T result = call.execute();
        //     onSuccess();
        //     return result;
        // } catch (Exception e) {
        //     onFailure();
        //     throw e;
        // }
        return null;
    }
    
    /**
     * Check if OPEN → HALF_OPEN transition should happen
     * 
     * IMPLEMENTATION HINTS:
     * 1. If state is OPEN and enough time has passed (openTimeoutMs):
     *    → Transition to HALF_OPEN
     *    → Reset halfOpenAttempts to 0
     */
    private void checkStateTransition() {
        // TODO: Implement
        // HINT: if (state == CircuitState.OPEN) {
        //     long elapsed = System.currentTimeMillis() - lastFailureTime;
        //     if (elapsed >= openTimeoutMs) {
        //         state = CircuitState.HALF_OPEN;
        //         halfOpenAttempts = 0;
        //         System.out.println("    ⚡ " + name + ": OPEN → HALF_OPEN (testing recovery)");
        //     }
        // }
    }
    
    /**
     * Handle successful call
     * 
     * IMPLEMENTATION HINTS:
     * 1. If HALF_OPEN → transition to CLOSED (service recovered!)
     *    → Reset failure count
     * 2. If CLOSED → reset failure count (streak broken)
     * 3. Increment successCount
     */
    private void onSuccess() {
        // TODO: Implement
        // HINT: if (state == CircuitState.HALF_OPEN) {
        //     state = CircuitState.CLOSED;
        //     failureCount = 0;
        //     halfOpenAttempts = 0;
        //     System.out.println("    ✅ " + name + ": HALF_OPEN → CLOSED (recovered!)");
        // } else {
        //     failureCount = 0; // reset on success
        // }
        // HINT: successCount++;
    }
    
    /**
     * Handle failed call
     * 
     * IMPLEMENTATION HINTS:
     * 1. Increment failureCount and totalFailures
     * 2. If HALF_OPEN → go back to OPEN (service still failing)
     *    → Set lastFailureTime
     * 3. If CLOSED and failureCount >= threshold → transition to OPEN
     *    → Set lastFailureTime
     */
    private void onFailure() {
        // TODO: Implement
        // HINT: failureCount++;
        // HINT: totalFailures.incrementAndGet();
        //
        // HINT: if (state == CircuitState.HALF_OPEN) {
        //     state = CircuitState.OPEN;
        //     lastFailureTime = System.currentTimeMillis();
        //     halfOpenAttempts = 0;
        //     System.out.println("    ❌ " + name + ": HALF_OPEN → OPEN (still failing)");
        // } else if (state == CircuitState.CLOSED && failureCount >= failureThreshold) {
        //     state = CircuitState.OPEN;
        //     lastFailureTime = System.currentTimeMillis();
        //     System.out.println("    🔴 " + name + ": CLOSED → OPEN (threshold reached: " + failureCount + "/" + failureThreshold + ")");
        // }
        //
        // HINT: if (state == CircuitState.HALF_OPEN) halfOpenAttempts++;
    }
    
    // Getters
    public String getName() { return name; }
    public CircuitState getState() { return state; }
    public int getFailureCount() { return failureCount; }
    public int getTotalRequests() { return totalRequests.get(); }
    public int getTotalFailures() { return totalFailures.get(); }
    public int getTotalRejected() { return totalRejected.get(); }
    
    /** Force reset to CLOSED (manual recovery) */
    public void reset() {
        state = CircuitState.CLOSED;
        failureCount = 0;
        halfOpenAttempts = 0;
        System.out.println("    🔄 " + name + " manually reset to CLOSED");
    }
    
    @Override
    public String toString() {
        return name + "[" + state + ", failures=" + failureCount + "/" + failureThreshold
            + ", total=" + totalRequests.get() + ", rejected=" + totalRejected.get() + "]";
    }
}

// ===== EXCEPTIONS =====

class CircuitOpenException extends Exception {
    private final String circuitName;
    public CircuitOpenException(String name, String msg) {
        super(msg);
        this.circuitName = name;
    }
    public String getCircuitName() { return circuitName; }
}

// ===== SIMULATED SERVICES =====

/** Service that always succeeds */
class ReliableService {
    public String call() { return "OK"; }
}

/** Service that always fails */
class BrokenService {
    public String call() throws Exception { throw new RuntimeException("Service down!"); }
}

/** Service that recovers after N failures */
class FlakyService {
    private int callCount = 0;
    private final int failUntil;
    
    public FlakyService(int failUntil) { this.failUntil = failUntil; }
    
    public String call() throws Exception {
        callCount++;
        if (callCount <= failUntil) throw new RuntimeException("Fail #" + callCount);
        return "Recovered at call #" + callCount;
    }
    
    public int getCallCount() { return callCount; }
}

// ===== MAIN TEST CLASS =====

public class CircuitBreakerSystem {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Circuit Breaker LLD ===\n");
        
        // Test 1: Normal operation (CLOSED, all succeed)
        System.out.println("=== Test 1: Normal Operation (CLOSED) ===");
        CircuitBreaker cb = new CircuitBreaker("payment-service", 3, 2000, 1);
        ReliableService reliable = new ReliableService();
        try {
            for (int i = 0; i < 5; i++) {
                String result = cb.execute(() -> reliable.call());
                System.out.println("  Call " + (i+1) + ": " + result);
            }
            System.out.println("✓ State: " + cb.getState() + " (expect CLOSED)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 2: CLOSED → OPEN after threshold failures
        System.out.println("=== Test 2: CLOSED → OPEN (3 failures) ===");
        CircuitBreaker cb2 = new CircuitBreaker("order-service", 3, 2000, 1);
        BrokenService broken = new BrokenService();
        for (int i = 0; i < 4; i++) {
            try {
                cb2.execute(() -> broken.call());
            } catch (CircuitOpenException e) {
                System.out.println("  Call " + (i+1) + ": REJECTED (circuit open)");
            } catch (Exception e) {
                System.out.println("  Call " + (i+1) + ": Failed - " + e.getMessage());
            }
        }
        System.out.println("✓ State: " + cb2.getState() + " (expect OPEN)");
        System.out.println("  Failures: " + cb2.getFailureCount() + ", Rejected: " + cb2.getTotalRejected());
        System.out.println();
        
        // Test 3: OPEN → fail fast (no actual calls made)
        System.out.println("=== Test 3: OPEN → Fail Fast ===");
        try {
            cb2.execute(() -> reliable.call());
            System.out.println("✗ Should have been rejected");
        } catch (CircuitOpenException e) {
            System.out.println("✓ Rejected immediately: " + e.getMessage());
        }
        System.out.println();
        
        // Test 4: OPEN → HALF_OPEN → CLOSED (recovery)
        System.out.println("=== Test 4: Recovery (OPEN → HALF_OPEN → CLOSED) ===");
        CircuitBreaker cb3 = new CircuitBreaker("user-service", 2, 1000, 1); // 1s timeout
        // Break it
        for (int i = 0; i < 2; i++) {
            try { cb3.execute(() -> broken.call()); } catch (Exception e) {}
        }
        System.out.println("  After failures: " + cb3.getState() + " (expect OPEN)");
        
        // Wait for timeout
        Thread.sleep(1100);
        
        // Next call should go through (HALF_OPEN) and succeed
        try {
            String result = cb3.execute(() -> reliable.call());
            System.out.println("  After timeout + success: " + cb3.getState() + " (expect CLOSED)");
            System.out.println("✓ Recovered! Result: " + result);
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 5: HALF_OPEN → failure → back to OPEN
        System.out.println("=== Test 5: HALF_OPEN → Failure → OPEN ===");
        CircuitBreaker cb4 = new CircuitBreaker("inventory-service", 2, 500, 1);
        // Break it
        for (int i = 0; i < 2; i++) {
            try { cb4.execute(() -> broken.call()); } catch (Exception e) {}
        }
        System.out.println("  State: " + cb4.getState() + " (expect OPEN)");
        
        // Wait for timeout → HALF_OPEN
        Thread.sleep(600);
        
        // Try again but still fails → back to OPEN
        try { cb4.execute(() -> broken.call()); } catch (Exception e) {
            System.out.println("  After HALF_OPEN failure: " + cb4.getState() + " (expect OPEN)");
        }
        System.out.println("✓ Went back to OPEN");
        System.out.println();
        
        // Test 6: Flaky service recovery
        System.out.println("=== Test 6: Flaky Service Recovery ===");
        CircuitBreaker cb5 = new CircuitBreaker("search-service", 3, 500, 1);
        FlakyService flaky = new FlakyService(3); // fails first 3 calls, then succeeds
        
        // Will fail 3 times → OPEN
        for (int i = 0; i < 4; i++) {
            try {
                String r = cb5.execute(() -> flaky.call());
                System.out.println("  Call: " + r);
            } catch (CircuitOpenException e) {
                System.out.println("  Rejected (OPEN)");
            } catch (Exception e) {
                System.out.println("  Failed: " + e.getMessage());
            }
        }
        System.out.println("  State: " + cb5.getState());
        
        // Wait → HALF_OPEN → flaky now succeeds → CLOSED
        Thread.sleep(600);
        try {
            String r = cb5.execute(() -> flaky.call());
            System.out.println("  Recovery call: " + r);
            System.out.println("✓ Final state: " + cb5.getState() + " (expect CLOSED)");
        } catch (Exception e) {
            System.out.println("  " + e.getMessage());
        }
        System.out.println();
        
        // Test 7: Manual reset
        System.out.println("=== Test 7: Manual Reset ===");
        CircuitBreaker cb6 = new CircuitBreaker("email-service", 2, 60000, 1); // very long timeout
        for (int i = 0; i < 2; i++) {
            try { cb6.execute(() -> broken.call()); } catch (Exception e) {}
        }
        System.out.println("  State before reset: " + cb6.getState() + " (expect OPEN)");
        cb6.reset();
        System.out.println("✓ State after reset: " + cb6.getState() + " (expect CLOSED)");
        System.out.println();
        
        // Test 8: Stats
        System.out.println("=== Test 8: Stats ===");
        System.out.println("✓ " + cb2);
        System.out.println("  Total requests: " + cb2.getTotalRequests());
        System.out.println("  Total failures: " + cb2.getTotalFailures());
        System.out.println("  Total rejected: " + cb2.getTotalRejected());
        
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION:
 * =====================
 * 
 * 1. STATE MACHINE:
 *    CLOSED ──(failures >= threshold)──→ OPEN
 *    OPEN ──(timeout expires)──→ HALF_OPEN
 *    HALF_OPEN ──(success)──→ CLOSED
 *    HALF_OPEN ──(failure)──→ OPEN
 * 
 * 2. WHY CIRCUIT BREAKER?
 *    Without: 1000 requests/sec → all timeout (30s each) → thread exhaustion
 *    With: after 3 failures → fail fast (1ms) → save resources
 *    Gives downstream service time to recover
 * 
 * 3. CONFIGURATION:
 *    failureThreshold: 3-5 (how many failures to trip)
 *    openTimeout: 30-60s (how long before trying again)
 *    halfOpenMaxAttempts: 1-3 (test requests in HALF_OPEN)
 *    
 *    Sliding window: count failures in last N seconds (not all-time)
 * 
 * 4. FALLBACKS:
 *    When circuit is OPEN, return:
 *    - Cached data (stale but available)
 *    - Default value
 *    - Error message ("Service temporarily unavailable")
 *    - Call alternate service
 * 
 * 5. MONITORING:
 *    Track: state changes, failure rate, rejected count
 *    Alert: when circuit opens (service is down)
 *    Dashboard: per-service circuit breaker status
 * 
 * 6. REAL-WORLD:
 *    Netflix Hystrix (deprecated), Resilience4j (Java)
 *    Polly (.NET), pybreaker (Python)
 *    AWS SDK built-in retry + circuit breaker
 *    Service mesh (Istio/Envoy) can do this at infra level
 * 
 * 7. RELATED PATTERNS:
 *    Retry: try again (with backoff) — before circuit breaker
 *    Bulkhead: isolate failures per service (separate thread pools)
 *    Timeout: don't wait forever
 *    Rate Limiter: limit request rate
 *    Together: Retry → Circuit Breaker → Timeout → Fallback
 * 
 * 8. API / USAGE:
 *    CircuitBreaker cb = new CircuitBreaker("payment", 3, 30000, 1);
 *    try {
 *        Result r = cb.execute(() -> paymentService.charge(amount));
 *    } catch (CircuitOpenException e) {
 *        return fallbackResponse();
 *    }
 */
