/*
 * TRENDING HASHTAGS SYSTEM - Low Level Design
 * =============================================
 *
 * REQUIREMENTS:
 * 1. Record hashtag usage from tweets/posts with timestamps
 * 2. Get top-K trending hashtags within a time window (1h, 24h, 7d)
 * 3. Sliding window — old events expire automatically
 * 4. Support multiple ranking strategies (frequency vs velocity)
 * 5. Thread-safe for concurrent hashtag recording
 * 6. Efficient top-K retrieval without sorting entire dataset
 * 7. Periodic cleanup of expired events to bound memory
 * 8. Query count/rank for a specific hashtag
 *
 * KEY DATA STRUCTURES:
 * - ConcurrentHashMap<String, Deque<Long>>: hashtag → timestamped events (sliding window)
 * - PriorityQueue (min-heap, size K): efficient top-K extraction O(n log K)
 * - AtomicLong: global event counter for velocity calculation
 *
 * DESIGN PATTERNS:
 * - Strategy: FrequencyTrending vs VelocityTrending for ranking
 *
 * COMPLEXITY:
 *   recordHashtag:   O(1) amortized (deque append + cleanup)
 *   getTopK:         O(n log K) where n = unique hashtags, K = result size
 *   getCount:        O(W) where W = events in window (cleanup scan)
 *   cleanup:         O(n * W) full sweep of expired events
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

// ===== ENUMS =====

enum TrendingWindow {
    LAST_HOUR(60 * 60 * 1000L),
    LAST_24_HOURS(24 * 60 * 60 * 1000L),
    LAST_7_DAYS(7 * 24 * 60 * 60 * 1000L);

    final long durationMs;
    TrendingWindow(long durationMs) { this.durationMs = durationMs; }
}

// ===== DOMAIN =====

/** Scored hashtag for ranking — used in PriorityQueue */
class ScoredHashtag implements Comparable<ScoredHashtag> {
    final String tag;
    final double score;
    final int count;

    ScoredHashtag(String tag, double score, int count) {
        this.tag = tag;
        this.score = score;
        this.count = count;
    }

    /** Min-heap: lowest score at top → easy eviction for top-K */
    @Override
    public int compareTo(ScoredHashtag o) {
        int cmp = Double.compare(this.score, o.score);
        return cmp != 0 ? cmp : this.tag.compareTo(o.tag);
    }

}

// ===== STRATEGY =====

interface TrendingStrategy {
    /** Score a hashtag given its event timestamps within the window */
    double score(String tag, Deque<Long> events, long windowStartMs, long nowMs);
}

/** Rank by raw frequency — count of events in window */
class FrequencyTrendingStrategy implements TrendingStrategy {
    @Override
    public double score(String tag, Deque<Long> events, long windowStartMs, long nowMs) {
        // TODO: Implement
        // HINT: int count = 0;
        // HINT: for (Long ts : events) { if (ts >= windowStartMs) count++; }
        // HINT: return count;
        return 0;
    }
}

/**
 * Rank by velocity — recent events weighted higher.
 * Events in the latest 1/4 of the window score 4x, next 1/4 scores 3x, etc.
 * This surfaces hashtags that are ACCELERATING, not just popular.
 */
class VelocityTrendingStrategy implements TrendingStrategy {
    @Override
    public double score(String tag, Deque<Long> events, long windowStartMs, long nowMs) {
        // TODO: Implement
        // HINT: long windowDuration = nowMs - windowStartMs;
        // HINT: long quarterDuration = windowDuration / 4;
        // HINT: double score = 0;
        // HINT: for (Long ts : events) {
        // HINT:     if (ts < windowStartMs) continue;
        // HINT:     long age = nowMs - ts;
        // HINT:     int quarter = (int) (age / quarterDuration);  // 0=newest, 3=oldest
        // HINT:     double weight = 4.0 - Math.min(quarter, 3);   // 4x, 3x, 2x, 1x
        // HINT:     score += weight;
        // HINT: }
        // HINT: return score;
        return 0;
    }
}

// ===== CORE ENGINE =====

/**
 * HashtagTracker — sliding window event store + top-K query engine.
 *
 * Each hashtag maps to a Deque of timestamps (append-only, FIFO cleanup).
 * Expired events are lazily pruned on read, and eagerly via periodic cleanup.
 */
class HashtagTracker {
    final ConcurrentHashMap<String, Deque<Long>> events = new ConcurrentHashMap<>();
    final AtomicLong totalEvents = new AtomicLong(0);

    /** Record a hashtag occurrence at current time */
    void record(String tag) {
        record(tag, System.currentTimeMillis());
    }

    /** Record a hashtag occurrence at a specific timestamp (for testing) */
    void record(String tag, long timestampMs) {
        // TODO: Implement
        // HINT: String normalized = tag.toLowerCase().replaceAll("[^a-z0-9_]", "");
        // HINT: if (normalized.isEmpty()) return;
        // HINT: events.computeIfAbsent(normalized, k -> new ConcurrentLinkedDeque<>()).addLast(timestampMs);
        // HINT: totalEvents.incrementAndGet();
    }

    /** Count events for a hashtag within the given window */
    int getCount(String tag, TrendingWindow window) {
        // TODO: Implement
        // HINT: String normalized = tag.toLowerCase();
        // HINT: Deque<Long> deque = events.get(normalized);
        // HINT: if (deque == null) return 0;
        // HINT: long cutoff = System.currentTimeMillis() - window.durationMs;
        // HINT: int count = 0;
        // HINT: for (Long ts : deque) { if (ts >= cutoff) count++; }
        // HINT: return count;
        return 0;
    }

    /**
     * Get top-K hashtags using min-heap of size K.
     * O(n log K) — much better than sorting all n hashtags O(n log n).
     */
    List<ScoredHashtag> getTopK(int k, TrendingWindow window, TrendingStrategy strategy) {
        // TODO: Implement
        // HINT: long now = System.currentTimeMillis();
        // HINT: long windowStart = now - window.durationMs;
        // HINT: PriorityQueue<ScoredHashtag> minHeap = new PriorityQueue<>(k + 1);
        // HINT: for (Map.Entry<String, Deque<Long>> entry : events.entrySet()) {
        // HINT:     String tag = entry.getKey();
        // HINT:     Deque<Long> deque = entry.getValue();
        // HINT:     double score = strategy.score(tag, deque, windowStart, now);
        // HINT:     if (score <= 0) continue;
        // HINT:     int count = countInWindow(deque, windowStart);
        // HINT:     minHeap.offer(new ScoredHashtag(tag, score, count));
        // HINT:     if (minHeap.size() > k) minHeap.poll();  // evict lowest
        // HINT: }
        // HINT: List<ScoredHashtag> result = new ArrayList<>(minHeap);
        // HINT: result.sort(Collections.reverseOrder());  // highest score first
        // HINT: return result;
        return Collections.emptyList();
    }

    /** Helper: count events in deque that fall within window */
    int countInWindow(Deque<Long> deque, long windowStart) {
        // TODO: Implement
        // HINT: int c = 0;
        // HINT: for (Long ts : deque) { if (ts >= windowStart) c++; }
        // HINT: return c;
        return 0;
    }

    /**
     * Cleanup expired events across ALL hashtags.
     * Removes timestamps older than the largest window (7 days).
     * Also removes hashtags with zero remaining events.
     */
    void cleanup() {
        // TODO: Implement
        // HINT: long cutoff = System.currentTimeMillis() - TrendingWindow.LAST_7_DAYS.durationMs;
        // HINT: Iterator<Map.Entry<String, Deque<Long>>> it = events.entrySet().iterator();
        // HINT: while (it.hasNext()) {
        // HINT:     Map.Entry<String, Deque<Long>> entry = it.next();
        // HINT:     Deque<Long> deque = entry.getValue();
        // HINT:     while (!deque.isEmpty() && deque.peekFirst() < cutoff) deque.pollFirst();
        // HINT:     if (deque.isEmpty()) it.remove();
        // HINT: }
    }

    int uniqueHashtagCount() { return events.size(); }
    long totalEventCount() { return totalEvents.get(); }
}

// ===== SERVICE =====

/**
 * TrendingService — public API for the trending hashtags system.
 * Composes HashtagTracker + TrendingStrategy.
 * Supports recording from tweet text (extracts #hashtags automatically).
 */
class TrendingService {
    final HashtagTracker tracker;
    TrendingStrategy strategy;

    TrendingService() {
        this.tracker = new HashtagTracker();
        this.strategy = new FrequencyTrendingStrategy();
    }

    void setStrategy(TrendingStrategy strategy) { this.strategy = strategy; }

    /** Extract and record all #hashtags from tweet text */
    void recordTweet(String tweetText) {
        recordTweet(tweetText, System.currentTimeMillis());
    }

    /** Extract and record hashtags at a specific timestamp (for testing) */
    void recordTweet(String tweetText, long timestampMs) {
        // TODO: Implement
        // HINT: extract hashtags from text using simple parsing
        // HINT: for each word in tweetText.split("\\s+"):
        // HINT:     if (word.startsWith("#") && word.length() > 1)
        // HINT:         tracker.record(word.substring(1), timestampMs);
    }

    /** Record a single hashtag directly */
    void recordHashtag(String tag) {
        tracker.record(tag);
    }

    void recordHashtag(String tag, long timestampMs) {
        tracker.record(tag, timestampMs);
    }

    /** Get top-K trending hashtags for a given time window */
    List<ScoredHashtag> getTrending(int k, TrendingWindow window) {
        // TODO: Implement
        // HINT: return tracker.getTopK(k, window, strategy);
        return Collections.emptyList();
    }

    /** Get count of a specific hashtag in the window */
    int getHashtagCount(String tag, TrendingWindow window) {
        return tracker.getCount(tag, window);
    }

    /** Check if a hashtag is currently in the top-K */
    boolean isTrending(String tag, int topK, TrendingWindow window) {
        // TODO: Implement
        // HINT: List<ScoredHashtag> trending = getTrending(topK, window);
        // HINT: return trending.stream().anyMatch(s -> s.tag.equalsIgnoreCase(tag));
        return false;
    }

    /** Get rank of a specific hashtag (1-based), -1 if not trending */
    int getHashtagRank(String tag, int topK, TrendingWindow window) {
        // TODO: Implement
        // HINT: List<ScoredHashtag> trending = getTrending(topK, window);
        // HINT: for (int i = 0; i < trending.size(); i++) {
        // HINT:     if (trending.get(i).tag.equalsIgnoreCase(tag)) return i + 1;
        // HINT: }
        // HINT: return -1;
        return -1;
    }

    void cleanup() { tracker.cleanup(); }
    int uniqueHashtagCount() { return tracker.uniqueHashtagCount(); }
    long totalEventCount() { return tracker.totalEventCount(); }
}

// ===== MAIN =====

public class TrendingHashtagsSystem {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Trending Hashtags System ===\n");
        TrendingService svc = new TrendingService();

        // ---- Test 1: Basic hashtag recording ----
        System.out.println("--- Test 1: Basic Recording ---");
        svc.recordHashtag("java");
        svc.recordHashtag("java");
        svc.recordHashtag("java");
        svc.recordHashtag("python");
        svc.recordHashtag("python");
        svc.recordHashtag("rust");
        int javaCount = svc.getHashtagCount("java", TrendingWindow.LAST_HOUR);
        System.out.println("java count: " + javaCount + " (expect 3)");
        System.out.println("python count: " + svc.getHashtagCount("python", TrendingWindow.LAST_HOUR) + " (expect 2)");
        System.out.println("✓ Basic recording works");

        // ---- Test 2: Top-K trending ----
        System.out.println("\n--- Test 2: Top-K Trending (Frequency) ---");
        List<ScoredHashtag> top = svc.getTrending(3, TrendingWindow.LAST_HOUR);
        for (ScoredHashtag s : top) System.out.println("  #" + s.tag + " count=" + s.count);
        System.out.println("Expected order: java(3) > python(2) > rust(1)");
        if (!top.isEmpty() && top.get(0).tag.equals("java") && top.get(0).count == 3) {
            System.out.println("✓ Top-K ranking correct");
        }

        // ---- Test 3: Tweet text extraction ----
        System.out.println("\n--- Test 3: Extract Hashtags from Tweets ---");
        svc.recordTweet("Loving #javascript and #react for web dev!");
        svc.recordTweet("Just shipped my #react project #coding");
        svc.recordTweet("#React is amazing for building UIs #webdev");
        System.out.println("react count: " + svc.getHashtagCount("react", TrendingWindow.LAST_HOUR) + " (expect 3)");
        System.out.println("javascript count: " + svc.getHashtagCount("javascript", TrendingWindow.LAST_HOUR) + " (expect 1)");
        System.out.println("✓ Tweet hashtag extraction works");

        // ---- Test 4: isTrending & rank ----
        System.out.println("\n--- Test 4: isTrending & Rank ---");
        boolean javaTrending = svc.isTrending("java", 5, TrendingWindow.LAST_HOUR);
        int javaRank = svc.getHashtagRank("java", 10, TrendingWindow.LAST_HOUR);
        System.out.println("java trending in top 5: " + javaTrending + " (expect true)");
        System.out.println("java rank: " + javaRank + " (expect 1 or 2)");
        boolean unknownTrending = svc.isTrending("nonexistent", 5, TrendingWindow.LAST_HOUR);
        System.out.println("nonexistent trending: " + unknownTrending + " (expect false)");
        System.out.println("✓ isTrending and rank work");

        // ---- Test 5: Velocity strategy ----
        System.out.println("\n--- Test 5: Velocity-Based Trending ---");
        TrendingService velocitySvc = new TrendingService();
        velocitySvc.setStrategy(new VelocityTrendingStrategy());
        long now = System.currentTimeMillis();
        // "oldtag" has many events but spread across the full hour
        for (int i = 0; i < 20; i++) {
            velocitySvc.recordHashtag("oldtag", now - 50 * 60 * 1000 + i * 1000); // 50 min ago
        }
        // "newtag" has fewer events but all very recent (last 5 min)
        for (int i = 0; i < 10; i++) {
            velocitySvc.recordHashtag("newtag", now - 3 * 60 * 1000 + i * 1000); // 3 min ago
        }
        List<ScoredHashtag> velocityTop = velocitySvc.getTrending(2, TrendingWindow.LAST_HOUR);
        for (ScoredHashtag s : velocityTop) System.out.println("  #" + s.tag + " score=" + String.format("%.1f", s.score));
        System.out.println("Expected: newtag should rank higher (recent burst)");
        if (!velocityTop.isEmpty() && velocityTop.get(0).tag.equals("newtag")) {
            System.out.println("✓ Velocity strategy favors recent bursts");
        }

        // ---- Test 6: Case insensitivity / normalization ----
        System.out.println("\n--- Test 6: Normalization ---");
        TrendingService normSvc = new TrendingService();
        normSvc.recordHashtag("Java");
        normSvc.recordHashtag("JAVA");
        normSvc.recordHashtag("java");
        System.out.println("java count: " + normSvc.getHashtagCount("Java", TrendingWindow.LAST_HOUR) + " (expect 3)");
        System.out.println("✓ Case-insensitive normalization");

        // ---- Test 7: Empty / edge cases ----
        System.out.println("\n--- Test 7: Edge Cases ---");
        TrendingService emptySvc = new TrendingService();
        System.out.println("Top 5 on empty: " + emptySvc.getTrending(5, TrendingWindow.LAST_HOUR).size() + " (expect 0)");
        System.out.println("Count of unknown: " + emptySvc.getHashtagCount("nope", TrendingWindow.LAST_HOUR) + " (expect 0)");
        emptySvc.recordTweet("No hashtags here!");
        System.out.println("After tweet with no tags: " + emptySvc.uniqueHashtagCount() + " (expect 0)");
        System.out.println("✓ Edge cases handled");

        // ---- Test 8: Window expiry ----
        System.out.println("\n--- Test 8: Window Expiry ---");
        TrendingService expirySvc = new TrendingService();
        long twoHoursAgo = System.currentTimeMillis() - 2 * 60 * 60 * 1000;
        expirySvc.recordHashtag("expired", twoHoursAgo);
        expirySvc.recordHashtag("expired", twoHoursAgo);
        expirySvc.recordHashtag("fresh");
        System.out.println("expired in LAST_HOUR: " + expirySvc.getHashtagCount("expired", TrendingWindow.LAST_HOUR) + " (expect 0)");
        System.out.println("expired in LAST_24H: " + expirySvc.getHashtagCount("expired", TrendingWindow.LAST_24_HOURS) + " (expect 2)");
        System.out.println("fresh in LAST_HOUR: " + expirySvc.getHashtagCount("fresh", TrendingWindow.LAST_HOUR) + " (expect 1)");
        System.out.println("✓ Window expiry works");

        // ---- Test 9: Cleanup ----
        System.out.println("\n--- Test 9: Cleanup ---");
        TrendingService cleanSvc = new TrendingService();
        long eightDaysAgo = System.currentTimeMillis() - 8L * 24 * 60 * 60 * 1000;
        cleanSvc.recordHashtag("ancient", eightDaysAgo);
        cleanSvc.recordHashtag("ancient", eightDaysAgo);
        cleanSvc.recordHashtag("recent");
        System.out.println("Before cleanup: " + cleanSvc.uniqueHashtagCount() + " unique tags (expect 2)");
        cleanSvc.cleanup();
        System.out.println("After cleanup: " + cleanSvc.uniqueHashtagCount() + " unique tags (expect 1)");
        System.out.println("✓ Cleanup removes expired hashtags");

        // ---- Test 10: Concurrent recording ----
        System.out.println("\n--- Test 10: Thread Safety ---");
        TrendingService concSvc = new TrendingService();
        ExecutorService exec = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            final int idx = i;
            futures.add(exec.submit(() -> {
                concSvc.recordHashtag("concurrent_tag_" + (idx % 10));
            }));
        }
        for (Future<?> f : futures) f.get();
        exec.shutdown();
        long total = concSvc.totalEventCount();
        System.out.println("Total events: " + total + " (expect 1000)");
        System.out.println("Unique tags: " + concSvc.uniqueHashtagCount() + " (expect 10)");
        if (total == 1000) System.out.println("✓ Thread-safe: all 1000 events recorded");

        // ---- Test 11: Scale test ----
        System.out.println("\n--- Test 11: Scale Test ---");
        TrendingService scaleSvc = new TrendingService();
        long start = System.nanoTime();
        for (int i = 0; i < 100_000; i++) {
            scaleSvc.recordHashtag("tag_" + (i % 500));
        }
        long recordTime = (System.nanoTime() - start) / 1_000_000;
        start = System.nanoTime();
        List<ScoredHashtag> scaleTop = scaleSvc.getTrending(10, TrendingWindow.LAST_HOUR);
        long queryTime = (System.nanoTime() - start) / 1_000_000;
        System.out.println("100K records across 500 tags: " + recordTime + "ms");
        System.out.println("Top-10 query: " + queryTime + "ms");
        for (ScoredHashtag s : scaleTop.subList(0, Math.min(3, scaleTop.size())))
            System.out.println("  #" + s.tag + " count=" + s.count);
        System.out.println("✓ Scale test complete");

        System.out.println("\n=== All Tests Complete ===");
    }
}

/*
 * INTERVIEW NOTES:
 * ================
 *
 * 1. CORE ALGORITHM:
 *    - Sliding window via Deque<Long> per hashtag (append timestamps, FIFO evict)
 *    - Top-K via min-heap (PriorityQueue size K): O(n log K) beats O(n log n) sort
 *    - Lazy expiry on read + periodic full cleanup
 *
 * 2. COMPLEXITY:
 *    | Operation      | Time          | Space        |
 *    |----------------|---------------|--------------|
 *    | recordHashtag  | O(1)          | O(1) per event |
 *    | getTopK        | O(n log K)    | O(K)         |
 *    | getCount       | O(W)          | O(1)         |
 *    | cleanup        | O(n * W)      | O(1)         |
 *    n = unique hashtags, K = result size, W = events per hashtag in window
 *
 * 3. STRATEGY PATTERN:
 *    - FrequencyTrending: raw count (simple, good for "popular now")
 *    - VelocityTrending: time-weighted (surfaces ACCELERATING topics)
 *    - Easy to add: GeoTrending, PersonalizedTrending, etc.
 *
 * 4. SCALABILITY (Twitter-scale):
 *    - Count-Min Sketch: approximate counts in O(1) space for millions of tags
 *    - Redis Sorted Sets: ZINCRBY for counts, ZREVRANGE for top-K
 *    - Kafka: stream hashtag events → Flink/Storm for real-time aggregation
 *    - Shard by hashtag prefix, replicate read-heavy top-K endpoints
 *    - Pre-compute top-K every 30s, cache result (avoid per-request heap scan)
 *
 * 5. TWITTER'S ACTUAL APPROACH:
 *    - Count-Min Sketch for approximate frequency
 *    - Decay function: score *= e^(-λt) for exponential time decay
 *    - Geographic trending: separate windows per country/city
 *    - Spam/abuse filter: suppress hashtags from bot accounts
 *    - Personalized trending: blend global trend + user interest graph
 *
 * 6. TRADE-OFFS:
 *    - Deque per hashtag = exact counts but O(W) space per tag
 *    - Count-Min Sketch = O(1) space but approximate (false positives)
 *    - Min-heap top-K = no pre-computation, good for dynamic K
 *    - Pre-computed cache = fast reads but stale by cache-TTL
 */
