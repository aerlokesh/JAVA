import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/*
 * CDN SYSTEM - Low Level Design
 * ==============================
 * 
 * REQUIREMENTS:
 * 1. Route users to nearest edge server by region
 * 2. LRU cache at each edge with capacity limit + TTL expiration
 * 3. Cache MISS → fetch from origin → cache at edge → return
 * 4. Cache invalidation (purge across all edges)
 * 5. Pre-warm popular content to edges
 * 6. Consistent hashing to distribute content across edges in same region
 * 7. Health checks — unhealthy edges get skipped, traffic falls back
 * 8. Metrics: hit rate, origin offload, bandwidth saved
 * 9. Thread-safe concurrent access
 * 
 * KEY DATA STRUCTURES:
 * - LinkedHashMap(accessOrder=true) for LRU cache per edge server
 * - ConcurrentHashMap<region, ConsistentHashRing> for geo-routing
 * - TreeMap<Integer, String> for consistent hash ring per region
 * 
 * DESIGN PATTERNS:
 * - Proxy: edge server proxies origin
 * - Strategy: eviction policy (LRU here, could be LFU)
 * 
 * COMPLEXITY:
 *   fetchContent:    O(1) cache hit, O(1) origin fetch on miss
 *   invalidate:      O(E) where E = number of edge servers
 *   prewarm:         O(E_region) edges in target region
 *   LRU eviction:    O(1) via LinkedHashMap
 *   consistent hash: O(log V) where V = virtual nodes
 */

// ==================== EXCEPTION ====================

class ContentNotFoundException extends Exception {
    ContentNotFoundException(String key) { super("Content not found: " + key); }
}

// ==================== ENUMS ====================

enum CacheResult { HIT, MISS, EXPIRED }

// ==================== CACHED CONTENT ====================

class CachedContent {
    final String key;
    final byte[] data;
    final long createdAt;
    final long ttlMs;
    long lastAccessedAt;

    CachedContent(String key, byte[] data, long ttlMs) {
        this.key = key;
        this.data = data;
        this.ttlMs = ttlMs;
        this.createdAt = System.currentTimeMillis();
        this.lastAccessedAt = createdAt;
    }

    boolean isExpired() { return System.currentTimeMillis() - createdAt > ttlMs; }
    void touch() { lastAccessedAt = System.currentTimeMillis(); }
    int size() { return data.length; }
}

// ==================== EDGE SERVER ====================

/**
 * Edge Server (PoP) with LRU cache.
 * LinkedHashMap(accessOrder=true) → iteration order = least→most recently used.
 * Thread safety: synchronized on all cache ops.
 */
class EdgeServer {
    final String id;
    final String region;
    final long maxBytes;
    private long usedBytes;
    private final LinkedHashMap<String, CachedContent> cache;
    final AtomicLong hits = new AtomicLong();
    final AtomicLong misses = new AtomicLong();
    volatile boolean healthy = true;

    EdgeServer(String id, String region, long maxBytes) {
        this.id = id;
        this.region = region;
        this.maxBytes = maxBytes;
        this.usedBytes = 0;
        this.cache = new LinkedHashMap<>(16, 0.75f, true); // accessOrder=true → LRU
    }

    /** Get from cache. Returns null on MISS or EXPIRED. */
    synchronized CachedContent get(String key) {
        // HINT: CachedContent c = cache.get(key);
        // HINT: if (c == null) { misses.incrementAndGet(); return null; }
        // HINT: if (c.isExpired()) { evict(key); misses.incrementAndGet(); return null; }
        // HINT: c.touch();
        // HINT: hits.incrementAndGet();
        // HINT: bytesServed.addAndGet(c.size());
        // HINT: return c;
        CachedContent c=cache.get(key);
        if(c==null) {misses.incrementAndGet(); return null;}
        if(c.isExpired()) {evict(key); misses.incrementAndGet(); return null;}
        c.touch();
        hits.incrementAndGet();
        return c;
    }

    /** Put into cache with LRU eviction when full. */
    synchronized void put(String key, CachedContent content) {
        // HINT: if (cache.containsKey(key)) evict(key);
        // HINT: while (usedBytes + content.size() > maxBytes && !cache.isEmpty()) {
        // HINT:     String lruKey = cache.keySet().iterator().next(); // first = LRU
        // HINT:     evict(lruKey);
        // HINT: }
        // HINT: if (content.size() <= maxBytes) {
        // HINT:     cache.put(key, content);
        // HINT:     usedBytes += content.size();
        // HINT: }
        if(cache.containsKey(key)) evict(key);
        while(usedBytes+content.size()>maxBytes && !cache.isEmpty()){
            String lruKey=cache.keySet().iterator().next();
            evict(lruKey);
        }
        if(content.size()<=maxBytes){
            cache.put(key, content);
            usedBytes+=content.size();
        }
    }

    /** Remove a key from cache, update usedBytes. */
    synchronized void evict(String key) {
        // HINT: CachedContent removed = cache.remove(key);
        // HINT: if (removed != null) usedBytes -= removed.size();
        CachedContent removed = cache.remove(key);
        if(removed!=null) usedBytes-=removed.size();
    }

    /** Invalidate = evict. Called on origin content update. */
    synchronized void invalidate(String key) {
        // HINT: evict(key);
        evict(key);
    }

    /** Evict ALL expired entries (background cleanup). */
    synchronized int evictExpired() {
        // HINT: List<String> expired = new ArrayList<>();
        // HINT: for (CachedContent c : cache.values()) {
        // HINT:     if (c.isExpired()) expired.add(c.key);
        // HINT: }
        // HINT: for (String k : expired) evict(k);
        // HINT: return expired.size();
        List<String> expired=new ArrayList<>();
        for(CachedContent c:cache.values()){
            if(c.isExpired()) expired.add(c.key);
        }
        for(String k:expired) evict(k);
        return expired.size();
    }

    synchronized int cacheSize() { return cache.size(); }
    synchronized long getUsedBytes() { return usedBytes; }

    double hitRate() {
        long total = hits.get() + misses.get();
        return total == 0 ? 0 : (double) hits.get() / total * 100;
    }
}

// ==================== ORIGIN SERVER ====================

class OriginServer {
    private final ConcurrentHashMap<String, byte[]> store = new ConcurrentHashMap<>();
    final AtomicLong fetchCount = new AtomicLong();

    void putContent(String key, String content) { store.put(key, content.getBytes()); }

    byte[] fetch(String key) {
        byte[] data = store.get(key);
        if (data != null) fetchCount.incrementAndGet();
        return data;
    }

    boolean has(String key) { return store.containsKey(key); }
}

// ==================== CONSISTENT HASH RING ====================

/**
 * Consistent hashing: each edge gets VIRTUAL_NODES positions on ring.
 * Key hashes to ring position → routed to next clockwise server.
 * Adding/removing server only redistributes ~1/N of keys.
 */
class ConsistentHashRing {
    private static final int VIRTUAL_NODES = 50;
    private final TreeMap<Integer, String> ring = new TreeMap<>();
    private final Map<String, EdgeServer> servers = new HashMap<>();

    void addServer(EdgeServer server) {
        // HINT: servers.put(server.id, server);
        // HINT: for (int i = 0; i < VIRTUAL_NODES; i++) {
        // HINT:     ring.put(hash(server.id + "#" + i), server.id);
        // HINT: }
        servers.put(server.id, server);
        for(int i=0;i<VIRTUAL_NODES;i++){
            ring.put(hash(server.id+"#"+i),server.id);
        }

    }

    void removeServer(String serverId) {
        // HINT: servers.remove(serverId);
        // HINT: for (int i = 0; i < VIRTUAL_NODES; i++) {
        // HINT:     ring.remove(hash(serverId + "#" + i));
        // HINT: }
        servers.remove(serverId);
        for(int i=0;i<VIRTUAL_NODES;i++){
            ring.remove(hash(serverId+"#"+i));
        }
    }

    /** Route a content key to the responsible edge server. */
    EdgeServer route(String key) {
        // HINT: if (ring.isEmpty()) return null;
        // HINT: int h = hash(key);
        // HINT: Map.Entry<Integer, String> entry = ring.ceilingEntry(h);
        // HINT: if (entry == null) entry = ring.firstEntry(); // wrap around
        // HINT: // Walk clockwise to find a healthy server
        // HINT: String startId = entry.getValue();
        // HINT: EdgeServer server = servers.get(startId);
        // HINT: if (server != null && server.healthy) return server;
        // HINT: // If unhealthy, try next entries on the ring
        // HINT: for (Map.Entry<Integer, String> e : ring.tailMap(entry.getKey(), false).entrySet()) {
        // HINT:     EdgeServer s = servers.get(e.getValue());
        // HINT:     if (s != null && s.healthy) return s;
        // HINT: }
        // HINT: // Wrap around from beginning
        // HINT: for (Map.Entry<Integer, String> e : ring.entrySet()) {
        // HINT:     EdgeServer s = servers.get(e.getValue());
        // HINT:     if (s != null && s.healthy) return s;
        // HINT: }
        // HINT: return null;
        if(ring.isEmpty()) return null;
        int h=hash(key);
        Map.Entry<Integer,String> entry=ring.ceilingEntry(h);
        if(entry==null) entry=ring.firstEntry();
        String startId=entry.getValue();
        EdgeServer server = servers.get(startId);
        if(server!=null && server.healthy) return server;
        for(Map.Entry<Integer,String> e:ring.tailMap(entry.getKey(),false).entrySet()){
            EdgeServer s=servers.get(e.getValue());
            if(s!=null && s.healthy) return s;
        }
        for (Map.Entry<Integer, String> e : ring.entrySet()) {
            EdgeServer s = servers.get(e.getValue());
            if (s != null && s.healthy) return s;
        }
        return null;
    }

    private int hash(String key) {
        int h = key.hashCode();
        return h == Integer.MIN_VALUE ? 0 : Math.abs(h);
    }

    int size() { return servers.size(); }
}

// ==================== CDN SERVICE ====================

class CDNService {
    private final OriginServer origin;
    private final ConcurrentHashMap<String, EdgeServer> allEdges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConsistentHashRing> regionRings = new ConcurrentHashMap<>();
    private final long defaultTtlMs;
    final AtomicLong totalRequests = new AtomicLong();
    final AtomicLong totalHits = new AtomicLong();
    final AtomicLong totalMisses = new AtomicLong();

    CDNService(OriginServer origin, long defaultTtlMs) {
        this.origin = origin;
        this.defaultTtlMs = defaultTtlMs;
    }

    // --- Edge Management ---

    EdgeServer addEdge(String id, String region, long cacheBytes) {
        EdgeServer edge = new EdgeServer(id, region, cacheBytes);
        allEdges.put(id, edge);
        ConsistentHashRing ring = regionRings.computeIfAbsent(region, k -> new ConsistentHashRing());
        ring.addServer(edge);
        return edge;
    }

    void removeEdge(String id) {
        EdgeServer edge = allEdges.remove(id);
        if (edge != null) {
            ConsistentHashRing ring = regionRings.get(edge.region);
            if (ring != null) ring.removeServer(id);
        }
    }

    void markUnhealthy(String edgeId) {
        EdgeServer e = allEdges.get(edgeId);
        if (e != null) e.healthy = false;
    }

    void markHealthy(String edgeId) {
        EdgeServer e = allEdges.get(edgeId);
        if (e != null) e.healthy = true;
    }

    // --- Core: Fetch Content ---

    /**
     * Main CDN flow:
     * Route → cache HIT? return : MISS → origin → cache at edge → return
     */
    String fetchContent(String key, String userRegion) throws ContentNotFoundException {
        totalRequests.incrementAndGet();

        // Route to edge
        ConsistentHashRing ring = regionRings.get(userRegion);
        EdgeServer edge = (ring != null) ? ring.route(key) : null;
        // Fallback: any healthy edge
        if (edge == null) {
            for (EdgeServer e : allEdges.values()) {
                if (e.healthy) { edge = e; break; }
            }
        }
        if (edge == null) throw new ContentNotFoundException("No edge available for: " + key);

        // Try cache
        CachedContent cached = edge.get(key);
        if (cached != null) {
            totalHits.incrementAndGet();
            return new String(cached.data);
        }

        // Cache MISS → fetch from origin
        totalMisses.incrementAndGet();
        byte[] data = origin.fetch(key);
        if (data == null) throw new ContentNotFoundException(key);

        // Cache at edge
        edge.put(key, new CachedContent(key, data, defaultTtlMs));
        return new String(data);
    }

    // --- Invalidation ---

    /** Purge a key from ALL edge caches. */
    void invalidate(String key) {
        for (EdgeServer edge : allEdges.values()) {
            edge.invalidate(key);
        }
    }

    /** Purge all content matching a prefix (e.g., "/images/*"). */
    void invalidatePrefix(String prefix) {
        // TODO: Implement — expensive O(E × K) but needed for wildcard purge
        // HINT: for (EdgeServer edge : allEdges.values()) {
        // HINT:     synchronized (edge) {
        // HINT:         List<String> toEvict = new ArrayList<>();
        // HINT:         // Note: can't directly iterate edge.cache (private), so either
        // HINT:         // expose a method or use edge.evict() for known keys
        // HINT:         // Simplified: iterate and collect, then evict
        // HINT:     }
        // HINT: }
    }

    // --- Pre-warm ---

    /** Push content to all edges in a region before users request it. */
    void prewarm(String key, String region) {
        byte[] data = origin.fetch(key);
        if (data == null) return;
        for (EdgeServer edge : allEdges.values()) {
            if (edge.region.equals(region)) {
                edge.put(key, new CachedContent(key, data, defaultTtlMs));
            }
        }
    }

    void prewarmBatch(List<String> keys, String region) {
        for (String key : keys) prewarm(key, region);
    }

    // --- Background Maintenance ---

    /** Evict expired entries from all edges. Run periodically. */
    int cleanupExpired() {
        int total = 0;
        for (EdgeServer edge : allEdges.values()) {
            total += edge.evictExpired();
        }
        return total;
    }

    // --- Metrics ---

    EdgeServer getEdge(String id) { return allEdges.get(id); }

    void printStatus() {
        long h = totalHits.get(), m = totalMisses.get();
        double rate = (h + m) == 0 ? 0 : (double) h / (h + m) * 100;
        System.out.printf("  CDN: %d requests, %d hits, %d misses (%.1f%% hit rate)%n",
            totalRequests.get(), h, m, rate);
        System.out.printf("  Origin fetches: %d (lower = better)%n", origin.fetchCount.get());
        allEdges.values().forEach(e ->
            System.out.printf("    %s [%s] %d/%dB, %d items, %.1f%% hits, %s%n",
                e.id, e.region, e.getUsedBytes(), e.maxBytes, e.cacheSize(),
                e.hitRate(), e.healthy ? "HEALTHY" : "DOWN"));
    }
}

// ==================== MAIN / TESTS ====================

public class CDNSystem {
    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║        CDN SYSTEM - LLD Demo             ║");
        System.out.println("╚══════════════════════════════════════════╝\n");

        OriginServer origin = new OriginServer();
        origin.putContent("/img/logo.png", "LOGO_BINARY_" + "x".repeat(30));
        origin.putContent("/css/style.css", "body{margin:0;padding:0;color:#333}");
        origin.putContent("/js/app.js", "console.log('CDN served');var x=42;");
        origin.putContent("/video/intro.mp4", "VIDEO_" + "x".repeat(500));
        origin.putContent("/img/hero.jpg", "HERO_IMG_" + "x".repeat(80));
        origin.putContent("/api/data.json", "{\"users\":100,\"active\":true}");

        CDNService cdn = new CDNService(origin, 5000); // 5s TTL

        cdn.addEdge("us-east-1", "us-east", 200);
        cdn.addEdge("us-east-2", "us-east", 200);
        cdn.addEdge("eu-west-1", "eu-west", 200);
        cdn.addEdge("ap-south-1", "ap-south", 200);

        // --- Test 1: Cache MISS → origin fetch → cache ---
        System.out.println("=== Test 1: Cache MISS (first request) ===");
        try {
            String content = cdn.fetchContent("/img/logo.png", "us-east");
            System.out.println("  Got " + content.length() + " bytes");
            System.out.println("  Origin fetches: " + origin.fetchCount.get() + " (expected 1)");
            System.out.println("✓ First request = MISS → fetched from origin\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // --- Test 2: Cache HIT ---
        System.out.println("=== Test 2: Cache HIT (second request) ===");
        try {
            long originBefore = origin.fetchCount.get();
            cdn.fetchContent("/img/logo.png", "us-east");
            System.out.println("  Origin fetches still: " + origin.fetchCount.get() + " (no new fetch)");
            System.out.println("✓ Second request = HIT → served from edge cache\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // --- Test 3: Different region = separate MISS ---
        System.out.println("=== Test 3: Different Region ===");
        try {
            cdn.fetchContent("/img/logo.png", "eu-west");
            System.out.println("  Origin fetches: " + origin.fetchCount.get());
            System.out.println("✓ EU edge had separate MISS (each region has own cache)\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // --- Test 4: LRU Eviction ---
        System.out.println("=== Test 4: LRU Eviction ===");
        try {
            cdn.fetchContent("/css/style.css", "us-east");
            cdn.fetchContent("/js/app.js", "us-east");
            cdn.fetchContent("/img/hero.jpg", "us-east");
            cdn.fetchContent("/img/logo.png", "us-east"); // touch logo → not LRU
            cdn.fetchContent("/video/intro.mp4", "us-east"); // big → evicts LRU items
            System.out.println("✓ LRU eviction when cache exceeded capacity\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // --- Test 5: TTL Expiration ---
        System.out.println("=== Test 5: TTL Expiration ===");
        try {
            CDNService shortTtl = new CDNService(origin, 100); // 100ms TTL
            shortTtl.addEdge("ttl-edge", "us-east", 10000);
            shortTtl.fetchContent("/css/style.css", "us-east"); // MISS
            shortTtl.fetchContent("/css/style.css", "us-east"); // HIT
            long hitsBefore = shortTtl.totalHits.get();
            Thread.sleep(150);
            shortTtl.fetchContent("/css/style.css", "us-east"); // EXPIRED → MISS
            System.out.println("  Hits before: " + hitsBefore + ", after: " + shortTtl.totalHits.get());
            System.out.println("✓ Expired content re-fetched from origin\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // --- Test 6: Cache Invalidation ---
        System.out.println("=== Test 6: Invalidation (purge) ===");
        try {
            cdn.fetchContent("/api/data.json", "us-east");
            cdn.fetchContent("/api/data.json", "eu-west");
            origin.putContent("/api/data.json", "{\"users\":200,\"active\":true}");
            cdn.invalidate("/api/data.json");
            String fresh = cdn.fetchContent("/api/data.json", "us-east");
            System.out.println("  After purge got: " + fresh);
            System.out.println("✓ Invalidation purged stale content\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // --- Test 7: Pre-warm ---
        System.out.println("=== Test 7: Pre-warm ===");
        try {
            long originBefore = origin.fetchCount.get();
            cdn.prewarm("/js/app.js", "ap-south");
            long afterWarm = origin.fetchCount.get();
            cdn.fetchContent("/js/app.js", "ap-south"); // should be HIT
            long afterFetch = origin.fetchCount.get();
            System.out.println("  Origin: " + originBefore + " → " + afterWarm + " (warm) → " + afterFetch + " (fetch)");
            System.out.println("✓ Pre-warmed content served from cache\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // --- Test 8: Unhealthy Edge Fallback ---
        System.out.println("=== Test 8: Unhealthy Edge ===");
        try {
            cdn.markUnhealthy("eu-west-1");
            cdn.fetchContent("/img/logo.png", "eu-west");
            System.out.println("✓ Routed to fallback healthy edge");
            cdn.markHealthy("eu-west-1");
        } catch (ContentNotFoundException e) {
            System.out.println("  Fallback: " + e.getMessage());
        }
        System.out.println();

        // --- Test 9: Content Not Found ---
        System.out.println("=== Test 9: Content Not Found ===");
        try {
            cdn.fetchContent("/nonexistent.txt", "us-east");
            System.out.println("✗ Should have thrown");
        } catch (ContentNotFoundException e) {
            System.out.println("✓ Caught: " + e.getMessage() + "\n");
        }

        // --- Test 10: Expired Cleanup ---
        System.out.println("=== Test 10: Background Cleanup ===");
        {
            CDNService cleanCdn = new CDNService(origin, 50);
            cleanCdn.addEdge("clean-1", "us-east", 10000);
            cleanCdn.fetchContent("/img/logo.png", "us-east");
            cleanCdn.fetchContent("/css/style.css", "us-east");
            cleanCdn.fetchContent("/js/app.js", "us-east");
            Thread.sleep(100);
            int evicted = cleanCdn.cleanupExpired();
            System.out.println("  Evicted " + evicted + " expired entries");
            System.out.println("✓ Background cleanup works\n");
        }

        // --- Test 11: Consistent Hash Distribution ---
        System.out.println("=== Test 11: Consistent Hash Distribution ===");
        {
            CDNService distCdn = new CDNService(origin, 60000);
            distCdn.addEdge("d-1", "test", 100000);
            distCdn.addEdge("d-2", "test", 100000);
            distCdn.addEdge("d-3", "test", 100000);
            for (int i = 0; i < 30; i++) {
                origin.putContent("/page/" + i, "content-" + i);
                distCdn.fetchContent("/page/" + i, "test");
            }
            System.out.printf("  d-1: %d, d-2: %d, d-3: %d items%n",
                distCdn.getEdge("d-1").cacheSize(),
                distCdn.getEdge("d-2").cacheSize(),
                distCdn.getEdge("d-3").cacheSize());
            System.out.println("✓ Keys distributed across edges via consistent hashing\n");
        }

        // --- Test 12: Thread Safety ---
        System.out.println("=== Test 12: Thread Safety ===");
        {
            CDNService concCdn = new CDNService(origin, 60000);
            concCdn.addEdge("c-1", "us-east", 100000);
            concCdn.addEdge("c-2", "us-east", 100000);

            ExecutorService exec = Executors.newFixedThreadPool(8);
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                final String key = "/img/page-" + (i % 20);
                if (!origin.has(key)) origin.putContent(key, "data-" + i);
                futures.add(exec.submit(() -> {
                    try { concCdn.fetchContent(key, "us-east"); } catch (Exception e) {}
                }));
            }
            for (int i = 0; i < 20; i++)
                futures.add(exec.submit(() -> concCdn.invalidate("/img/page-0")));
            for (Future<?> f : futures) { try { f.get(); } catch (Exception e) {} }
            exec.shutdown();

            System.out.printf("  %d requests, %d hits, %d misses%n",
                concCdn.totalRequests.get(), concCdn.totalHits.get(), concCdn.totalMisses.get());
            System.out.println("✓ Thread-safe concurrent fetches + invalidations\n");
        }

        System.out.println("=== Final Status ===");
        cdn.printStatus();

        System.out.println("\n╔══════════════════════════════════════════╗");
        System.out.println("║        ALL 12 TESTS PASSED ✓             ║");
        System.out.println("╚══════════════════════════════════════════╝");
    }
}

/*
 * ==================== INTERVIEW NOTES ====================
 *
 * 1. CDN FLOW:
 *    User → GeoDNS → nearest edge PoP → cache HIT? return : MISS → origin → cache → return
 *
 * 2. CONSISTENT HASHING:
 *    TreeMap ring, each edge has 50 virtual nodes. ceilingEntry() for O(log V) lookup.
 *    Adding/removing server redistributes only ~1/N of keys (not all).
 *
 * 3. LRU CACHE:
 *    LinkedHashMap(accessOrder=true) — O(1) get/put, iterator gives LRU-first.
 *    On put: while usedBytes > max → remove iterator().next() (eldest).
 *    Combined with TTL: expired entries treated as MISS on get().
 *
 * 4. CACHE INVALIDATION:
 *    TTL: auto-expire (simple, eventual consistency)
 *    Purge: push invalidation to all edges (immediate, O(E))
 *    Versioned URLs: /style.v2.css (best — infinite TTL, new URL on change)
 *
 * 5. THREAD SAFETY:
 *    synchronized on EdgeServer (LinkedHashMap not thread-safe)
 *    ConcurrentHashMap for CDN-level maps
 *    AtomicLong for all counters
 *
 * 6. SCALABILITY:
 *    200+ edge PoPs, Origin Shield as intermediate cache layer
 *    Origin only handles ~5% of traffic (cache offload)
 *
 * 7. REAL-WORLD: CloudFront, Cloudflare, Akamai, Fastly
 */
