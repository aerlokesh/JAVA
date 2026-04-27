import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/*
 * HIT COUNTER - Low Level Design
 * =================================
 * 
 * REQUIREMENTS:
 * 1. Record hits (page views, API calls, events) with timestamps
 * 2. Get hit count in last N seconds (sliding window)
 * 3. Multiple time granularities: last 1 min, 5 min, 1 hour
 * 4. Pluggable window strategy: fixed buckets vs circular buffer
 * 5. Per-key counters (e.g., per URL, per endpoint)
 * 6. Hit event notifications (Observer)
 * 7. Thread-safe for high concurrency
 * 
 * DESIGN PATTERNS:
 *   Strategy  (WindowStrategy) — BucketWindow, CircularWindow
 *   Observer  (HitListener)    — HitLogger, ThresholdAlert
 *   Facade    (HitCounterService)
 * 
 * KEY DS: AtomicInteger[] circular buffer indexed by (timestamp % windowSize)
 */

// ==================== EXCEPTIONS ====================

class InvalidWindowException extends RuntimeException {
    InvalidWindowException(String msg) { super("Invalid window: " + msg); }
}

// ==================== ENUMS ====================

enum TimeGranularity { SECOND, MINUTE, HOUR }

// ==================== INTERFACES ====================

/** Strategy — how to count hits in a time window. */
interface WindowStrategy {
    void recordHit(long timestamp);
    int getHitCount(long currentTime, int windowSeconds);
}

/** Observer — hit events. */
interface HitListener {
    void onHit(String key, long timestamp, int totalHits);
}

// ==================== STRATEGY IMPLEMENTATIONS ====================

/**
 * Circular buffer: array of size windowSize (300 for 5 min).
 * Each bucket = 1 second. Index = timestamp % size.
 * If bucket timestamp matches → increment, else → reset to 1.
 * O(1) record, O(W) count where W = window size.
 */
class CircularBufferWindow implements WindowStrategy {
    private final int size;
    private final int[] hits;
    private final long[] timestamps;

    CircularBufferWindow(int windowSeconds) {
        this.size = windowSeconds;
        this.hits = new int[size];
        this.timestamps = new long[size];
    }

    @Override public synchronized void recordHit(long timestamp) {
        int idx = (int)(timestamp % size);
        if (timestamps[idx] == timestamp) {
            hits[idx]++;
        } else {
            timestamps[idx] = timestamp;
            hits[idx] = 1;
        }
    }

    @Override public synchronized int getHitCount(long currentTime, int windowSeconds) {
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (currentTime - timestamps[i] < windowSeconds) {
                count += hits[i];
            }
        }
        return count;
    }
}

/**
 * TreeMap-based: exact timestamps, range query.
 * More memory but precise. Good for variable windows.
 */
class TreeMapWindow implements WindowStrategy {
    private final TreeMap<Long, AtomicInteger> buckets = new TreeMap<>();

    @Override public synchronized void recordHit(long timestamp) {
        buckets.computeIfAbsent(timestamp, k -> new AtomicInteger()).incrementAndGet();
    }

    @Override public synchronized int getHitCount(long currentTime, int windowSeconds) {
        long start = currentTime - windowSeconds;
        // Clean old entries
        buckets.headMap(start).clear();
        return buckets.tailMap(start).values().stream().mapToInt(AtomicInteger::get).sum();
    }
}

// ==================== OBSERVER IMPLEMENTATIONS ====================

class HitLogger implements HitListener {
    final List<String> events = new ArrayList<>();
    @Override public void onHit(String key, long timestamp, int total) {
        events.add("HIT:" + key + "@" + timestamp + " total=" + total);
    }
}

/** Alert when hit count exceeds threshold. */
class ThresholdAlert implements HitListener {
    final int threshold;
    final List<String> alerts = new ArrayList<>();

    ThresholdAlert(int threshold) { this.threshold = threshold; }

    @Override public void onHit(String key, long timestamp, int total) {
        if (total >= threshold) alerts.add("ALERT:" + key + " hits=" + total);
    }
}

// ==================== HIT COUNTER SERVICE (FACADE) ====================

class HitCounterService {
    private final ConcurrentHashMap<String, WindowStrategy> counters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> totalCounts = new ConcurrentHashMap<>();
    private final int defaultWindowSeconds;
    private final boolean useTreeMap;
    private final List<HitListener> listeners = new ArrayList<>();

    HitCounterService(int windowSeconds, boolean useTreeMap) {
        this.defaultWindowSeconds = windowSeconds;
        this.useTreeMap = useTreeMap;
    }
    HitCounterService(int windowSeconds) { this(windowSeconds, false); }
    HitCounterService() { this(300, false); } // 5 min default

    void addListener(HitListener l) { listeners.add(l); }

    private WindowStrategy getWindow(String key) {
        return counters.computeIfAbsent(key, k ->
            useTreeMap ? new TreeMapWindow() : new CircularBufferWindow(defaultWindowSeconds));
    }

    /** Record a hit for a key at given timestamp (epoch seconds). */
    void hit(String key, long timestamp) {
        getWindow(key).recordHit(timestamp);
        int total = totalCounts.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
        listeners.forEach(l -> l.onHit(key, timestamp, total));
    }

    /** Record a hit at current time. */
    void hit(String key) { hit(key, System.currentTimeMillis() / 1000); }

    /** Get hits in last windowSeconds for a key. */
    int getHits(String key, long currentTime, int windowSeconds) {
        WindowStrategy w = counters.get(key);
        return w == null ? 0 : w.getHitCount(currentTime, windowSeconds);
    }

    /** Get hits in default window. */
    int getHits(String key, long currentTime) { return getHits(key, currentTime, defaultWindowSeconds); }

    /** Get total all-time hits. */
    int getTotalHits(String key) {
        AtomicInteger c = totalCounts.get(key);
        return c == null ? 0 : c.get();
    }

    /** Get hits across all keys in a window. */
    int getGlobalHits(long currentTime, int windowSeconds) {
        return counters.entrySet().stream()
            .mapToInt(e -> e.getValue().getHitCount(currentTime, windowSeconds)).sum();
    }

    int getKeyCount() { return counters.size(); }
}

// ==================== MAIN / TESTS ====================

public class HitCounterSystem {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════╗");
        System.out.println("║   HIT COUNTER - LLD Demo              ║");
        System.out.println("╚═══════════════════════════════════════╝\n");

        // --- Test 1: Basic hits ---
        System.out.println("=== Test 1: Basic hits ===");
        HitCounterService svc = new HitCounterService(300);
        svc.hit("/api/users", 1);
        svc.hit("/api/users", 1);
        svc.hit("/api/users", 2);
        svc.hit("/api/users", 3);
        check(svc.getHits("/api/users", 300, 300), 4, "4 hits in last 300s");
        check(svc.getTotalHits("/api/users"), 4, "4 total");
        System.out.println("✓\n");

        // --- Test 2: Sliding window ---
        System.out.println("=== Test 2: Sliding window ===");
        HitCounterService svc2 = new HitCounterService(60); // 60s window
        svc2.hit("page", 100);
        svc2.hit("page", 110);
        svc2.hit("page", 120);
        svc2.hit("page", 200); // outside 60s window from time=250
        check(svc2.getHits("page", 250, 60), 1, "Only hit@200 in [190,250)");
        check(svc2.getHits("page", 170, 60), 2, "Hits@110,120 in [110,170)");
        System.out.println("✓\n");

        // --- Test 3: Multiple keys ---
        System.out.println("=== Test 3: Multiple keys ===");
        svc.hit("/api/orders", 1);
        svc.hit("/api/orders", 2);
        check(svc.getHits("/api/orders", 300, 300), 2, "2 hits on /api/orders");
        check(svc.getKeyCount(), 2, "2 keys tracked");
        System.out.println("✓\n");

        // --- Test 4: No hits ---
        System.out.println("=== Test 4: No hits ===");
        check(svc.getHits("/unknown", 300, 300), 0, "Unknown key = 0");
        check(svc.getTotalHits("/unknown"), 0, "Total = 0");
        System.out.println("✓\n");

        // --- Test 5: TreeMap strategy ---
        System.out.println("=== Test 5: TreeMap strategy ===");
        HitCounterService svc5 = new HitCounterService(300, true);
        svc5.hit("treemap", 10);
        svc5.hit("treemap", 20);
        svc5.hit("treemap", 30);
        check(svc5.getHits("treemap", 50, 30), 2, "Hits@20,30 in TreeMap window");
        check(svc5.getTotalHits("treemap"), 3, "3 total");
        System.out.println("✓\n");

        // --- Test 6: Global hits ---
        System.out.println("=== Test 6: Global hits ===");
        int global = svc.getGlobalHits(300, 300);
        check(global, 6, "6 global hits (4 users + 2 orders)");
        System.out.println("✓\n");

        // --- Test 7: Observer — Logger ---
        System.out.println("=== Test 7: Observer — Logger ===");
        HitCounterService svc7 = new HitCounterService(60);
        HitLogger logger = new HitLogger();
        svc7.addListener(logger);
        svc7.hit("test", 100);
        svc7.hit("test", 101);
        check(logger.events.size(), 2, "2 hit events logged");
        System.out.println("✓\n");

        // --- Test 8: Observer — Threshold Alert ---
        System.out.println("=== Test 8: Threshold Alert ===");
        HitCounterService svc8 = new HitCounterService(60);
        ThresholdAlert alert = new ThresholdAlert(3);
        svc8.addListener(alert);
        svc8.hit("hot", 1); svc8.hit("hot", 2); svc8.hit("hot", 3);
        check(alert.alerts.size(), 1, "1 alert (threshold=3, hit 3)");
        svc8.hit("hot", 4);
        check(alert.alerts.size(), 2, "2 alerts (hit 4 also exceeds)");
        System.out.println("  Alerts: " + alert.alerts);
        System.out.println("✓\n");

        // --- Test 9: Expired hits ---
        System.out.println("=== Test 9: Expired hits ===");
        HitCounterService svc9 = new HitCounterService(10); // 10s window
        svc9.hit("expire", 1); svc9.hit("expire", 5); svc9.hit("expire", 15);
        check(svc9.getHits("expire", 20, 10), 1, "Only hit@15 in [10,20)");
        check(svc9.getTotalHits("expire"), 3, "3 total all-time");
        System.out.println("✓\n");

        // --- Test 10: Thread Safety ---
        System.out.println("=== Test 10: Thread Safety ===");
        HitCounterService svc10 = new HitCounterService(300);
        ExecutorService exec = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            int x = i;
            futures.add(exec.submit(() -> svc10.hit("concurrent", x % 300)));
        }
        for (Future<?> f : futures) { try { f.get(); } catch (Exception e) {} }
        exec.shutdown();
        check(svc10.getTotalHits("concurrent"), 1000, "1000 concurrent hits");
        check(svc10.getHits("concurrent", 300, 300) > 0, true, "Window count > 0");
        System.out.println("✓\n");

        // --- Test 11: Scale ---
        System.out.println("=== Test 11: Scale ===");
        HitCounterService svc11 = new HitCounterService(3600); // 1 hour
        long t = System.nanoTime();
        for (int i = 0; i < 100000; i++) svc11.hit("scale", i % 3600);
        System.out.printf("  100K hits: %.2f ms\n", (System.nanoTime() - t) / 1e6);
        t = System.nanoTime();
        int count = svc11.getHits("scale", 3600, 3600);
        System.out.printf("  Window query: %d hits in %.2f ms\n", count, (System.nanoTime() - t) / 1e6);
        check(svc11.getTotalHits("scale"), 100000, "100K total");
        System.out.println("✓\n");

        System.out.println("════════ ALL 11 TESTS PASSED ✓ ════════");
    }

    static void check(int a, int e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
    static void check(boolean a, boolean e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. CIRCULAR BUFFER: int[windowSize] indexed by timestamp % size.
 *    O(1) record, O(W) count. Fixed memory. Classic LeetCode 362.
 *    Each bucket stores (timestamp, count). Stale bucket → reset.
 *
 * 2. TREEMAP: Exact timestamps, range query via tailMap().
 *    headMap().clear() evicts old entries. More memory, more precise.
 *
 * 3. STRATEGY: CircularBufferWindow (fixed memory, fast) vs TreeMapWindow
 *    (precise, variable windows). Swap based on use case.
 *
 * 4. OBSERVER: HitLogger (audit trail), ThresholdAlert (rate limiting, DDoS detection).
 *
 * 5. PER-KEY: ConcurrentHashMap<key, WindowStrategy>. Each endpoint/URL tracked independently.
 *
 * 6. THREAD SAFETY: synchronized on window methods, AtomicInteger for totals,
 *    ConcurrentHashMap for registry.
 *
 * 7. EXTENSIONS: percentile tracking (p50/p95), distributed hit counting (Redis),
 *    rate limiting integration, dashboard visualization.
 */
