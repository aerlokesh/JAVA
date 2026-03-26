import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

// ===== EXCEPTIONS =====

class ContentNotFoundException extends Exception {
    public ContentNotFoundException(String key) { super("Content not found: " + key); }
}

// ===== ENUMS =====

enum CacheStatus { HIT, MISS, EXPIRED }

// ===== DOMAIN CLASSES =====

/**
 * Cached content on an edge server
 */
class CachedContent {
    private final String key;           // URL path: "/images/logo.png"
    private final String content;       // the actual content (bytes in real system)
    private final long sizeBytes;
    private final LocalDateTime cachedAt;
    private LocalDateTime lastAccessedAt;
    private final long ttlMs;           // time-to-live in ms
    private int accessCount;
    
    public CachedContent(String key, String content, long sizeBytes, long ttlMs) {
        this.key = key;
        this.content = content;
        this.sizeBytes = sizeBytes;
        this.ttlMs = ttlMs;
        this.cachedAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
        this.accessCount = 0;
    }
    
    public String getKey() { return key; }
    public String getContent() { return content; }
    public long getSizeBytes() { return sizeBytes; }
    public LocalDateTime getCachedAt() { return cachedAt; }
    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public long getTtlMs() { return ttlMs; }
    public int getAccessCount() { return accessCount; }
    
    public void touch() { this.lastAccessedAt = LocalDateTime.now(); this.accessCount++; }
    
    public boolean isExpired() {
        return Duration.between(cachedAt, LocalDateTime.now()).toMillis() > ttlMs;
    }
    
    @Override
    public String toString() { return key + "[" + sizeBytes + "B, hits=" + accessCount + ", expired=" + isExpired() + "]"; }
}

/**
 * Edge Server (PoP - Point of Presence)
 * Each edge server has a local cache with capacity limit
 */
class EdgeServer {
    private final String serverId;
    private final String region;          // "us-east", "eu-west", "ap-south"
    private final long maxCacheBytes;     // max cache size
    private long usedCacheBytes;
    private final Map<String, CachedContent> cache;    // key → content
    private final LinkedList<String> lruOrder;          // for LRU eviction
    private final AtomicLong cacheHits;
    private final AtomicLong cacheMisses;
    private boolean healthy;
    
    public EdgeServer(String serverId, String region, long maxCacheBytes) {
        this.serverId = serverId;
        this.region = region;
        this.maxCacheBytes = maxCacheBytes;
        this.usedCacheBytes = 0;
        this.cache = new ConcurrentHashMap<>();
        this.lruOrder = new LinkedList<>();
        this.cacheHits = new AtomicLong(0);
        this.cacheMisses = new AtomicLong(0);
        this.healthy = true;
    }
    
    public String getServerId() { return serverId; }
    public String getRegion() { return region; }
    public long getMaxCacheBytes() { return maxCacheBytes; }
    public long getUsedCacheBytes() { return usedCacheBytes; }
    public long getCacheHits() { return cacheHits.get(); }
    public long getCacheMisses() { return cacheMisses.get(); }
    public boolean isHealthy() { return healthy; }
    public void setHealthy(boolean h) { this.healthy = h; }
    public int getCacheSize() { return cache.size(); }
    
    /**
     * Get content from this edge's cache
     * 
     * IMPLEMENTATION HINTS:
     * 1. Look up key in cache
     * 2. If not found → return null (MISS)
     * 3. If found but expired → evict it, return null (EXPIRED)
     * 4. If found and valid → touch() it, move to front of LRU, increment hits
     * 5. Return the content
     */
    public CachedContent get(String key) {
        // TODO: Implement
        // HINT: CachedContent content = cache.get(key);
        // HINT: if (content == null) { cacheMisses.incrementAndGet(); return null; }
        // HINT: if (content.isExpired()) {
        //     evict(key);
        //     cacheMisses.incrementAndGet();
        //     return null;
        // }
        // HINT: content.touch();
        // HINT: synchronized(lruOrder) { lruOrder.remove(key); lruOrder.addFirst(key); }
        // HINT: cacheHits.incrementAndGet();
        // HINT: return content;
        return null;
    }
    
    /**
     * Put content into this edge's cache (with LRU eviction)
     * 
     * IMPLEMENTATION HINTS:
     * 1. If key already exists → evict old first
     * 2. While usedCacheBytes + newSize > maxCacheBytes → evict LRU item
     * 3. Store in cache map
     * 4. Add to front of LRU list
     * 5. Update usedCacheBytes
     */
    public void put(String key, CachedContent content) {
        // TODO: Implement
        // HINT: if (cache.containsKey(key)) evict(key);
        // HINT: while (usedCacheBytes + content.getSizeBytes() > maxCacheBytes && !lruOrder.isEmpty()) {
        //     String lruKey = lruOrder.removeLast();
        //     evict(lruKey);
        //     System.out.println("      🗑️ Evicted (LRU): " + lruKey);
        // }
        // HINT: cache.put(key, content);
        // HINT: synchronized(lruOrder) { lruOrder.addFirst(key); }
        // HINT: usedCacheBytes += content.getSizeBytes();
    }
    
    /**
     * Evict content from cache
     */
    public void evict(String key) {
        // TODO: Implement
        // HINT: CachedContent removed = cache.remove(key);
        // HINT: if (removed != null) usedCacheBytes -= removed.getSizeBytes();
        // HINT: synchronized(lruOrder) { lruOrder.remove(key); }
    }
    
    /**
     * Invalidate (purge) content — called when origin updates
     */
    public void invalidate(String key) {
        // TODO: Implement
        // HINT: evict(key);
        // HINT: System.out.println("      🚫 Invalidated: " + key + " on " + serverId);
    }
    
    public double getHitRate() {
        long total = cacheHits.get() + cacheMisses.get();
        return total == 0 ? 0 : (double) cacheHits.get() / total * 100;
    }
    
    @Override
    public String toString() {
        return serverId + "[" + region + ", " + usedCacheBytes + "/" + maxCacheBytes + "B"
            + ", items=" + cache.size() + ", hitRate=" + String.format("%.1f", getHitRate()) + "%]";
    }
}

/**
 * Origin Server — source of truth for all content
 */
class OriginServer {
    private final Map<String, String> content;  // key → content
    private final AtomicLong requestCount;
    
    public OriginServer() {
        this.content = new ConcurrentHashMap<>();
        this.requestCount = new AtomicLong(0);
    }
    
    public void addContent(String key, String value) { content.put(key, value); }
    
    public String fetch(String key) {
        requestCount.incrementAndGet();
        return content.get(key);
    }
    
    public boolean hasContent(String key) { return content.containsKey(key); }
    public long getRequestCount() { return requestCount.get(); }
}

// ===== SERVICE =====

/**
 * CDN System - Low Level Design (LLD)
 * 
 * PROBLEM: Design a Content Delivery Network that can:
 * 1. Route users to nearest edge server (by region)
 * 2. Cache content at edge servers (cache HIT/MISS)
 * 3. Fetch from origin on cache MISS → cache at edge
 * 4. LRU eviction when edge cache is full
 * 5. TTL-based expiration
 * 6. Cache invalidation (purge on content update)
 * 7. Track hit rates and metrics
 * 
 * KEY FLOW:
 *   User → DNS/Router → Edge Server → Cache HIT? → Return
 *                                   → Cache MISS? → Fetch Origin → Cache → Return
 * 
 * PATTERNS: Strategy (eviction policy), Proxy (edge as cache proxy for origin)
 */
class CDNService {
    private final Map<String, EdgeServer> edgeServers;           // serverId → edge
    private final Map<String, List<String>> regionToServers;     // region → [serverIds]
    private final OriginServer origin;
    private final long defaultTtlMs;
    private final AtomicLong totalRequests;
    
    public CDNService(OriginServer origin, long defaultTtlMs) {
        this.edgeServers = new ConcurrentHashMap<>();
        this.regionToServers = new ConcurrentHashMap<>();
        this.origin = origin;
        this.defaultTtlMs = defaultTtlMs;
        this.totalRequests = new AtomicLong(0);
    }
    
    /**
     * Register an edge server in a region
     */
    public EdgeServer addEdgeServer(String serverId, String region, long cacheSize) {
        // TODO: Implement
        // HINT: EdgeServer edge = new EdgeServer(serverId, region, cacheSize);
        // HINT: edgeServers.put(serverId, edge);
        // HINT: regionToServers.computeIfAbsent(region, k -> new ArrayList<>()).add(serverId);
        // HINT: System.out.println("  ✓ Edge added: " + edge);
        // HINT: return edge;
        return null;
    }
    
    /**
     * Fetch content — the main CDN flow
     * 
     * IMPLEMENTATION HINTS:
     * 1. Route to best edge server for the region
     * 2. Try edge cache → if HIT, return content (fast!)
     * 3. If MISS → fetch from origin
     * 4. If origin has it → cache at edge, return content
     * 5. If origin doesn't have it → throw ContentNotFoundException
     * 6. Increment totalRequests
     * 7. Return content + cache status
     */
    public String fetchContent(String key, String userRegion) throws ContentNotFoundException {
        // TODO: Implement
        // HINT: totalRequests.incrementAndGet();
        //
        // HINT: EdgeServer edge = routeToEdge(userRegion);
        // HINT: if (edge == null) throw new ContentNotFoundException("No edge server for region: " + userRegion);
        //
        // HINT: // Try cache
        // HINT: CachedContent cached = edge.get(key);
        // HINT: if (cached != null) {
        //     System.out.println("    ⚡ CACHE HIT on " + edge.getServerId() + ": " + key);
        //     return cached.getContent();
        // }
        //
        // HINT: // Cache MISS → fetch from origin
        // HINT: System.out.println("    🔄 CACHE MISS on " + edge.getServerId() + " → fetching from origin");
        // HINT: String content = origin.fetch(key);
        // HINT: if (content == null) throw new ContentNotFoundException(key);
        //
        // HINT: // Cache at edge
        // HINT: CachedContent newCached = new CachedContent(key, content, content.length(), defaultTtlMs);
        // HINT: edge.put(key, newCached);
        // HINT: return content;
        return null;
    }
    
    /**
     * Route to best edge server in region (simple: round-robin or first healthy)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get server list for region
     * 2. Find first healthy server
     * 3. If no servers in region → try any healthy server (fallback)
     */
    private EdgeServer routeToEdge(String region) {
        // TODO: Implement
        // HINT: List<String> serverIds = regionToServers.get(region);
        // HINT: if (serverIds != null) {
        //     for (String id : serverIds) {
        //         EdgeServer e = edgeServers.get(id);
        //         if (e != null && e.isHealthy()) return e;
        //     }
        // }
        // HINT: // Fallback: any healthy server
        // HINT: for (EdgeServer e : edgeServers.values()) {
        //     if (e.isHealthy()) return e;
        // }
        // HINT: return null;
        return null;
    }
    
    /**
     * Invalidate (purge) content across ALL edge servers
     * Called when origin content is updated
     * 
     * IMPLEMENTATION HINTS:
     * 1. For each edge server → call invalidate(key)
     */
    public void invalidateContent(String key) {
        // TODO: Implement
        // HINT: System.out.println("  🚫 Purging " + key + " from all edges");
        // HINT: for (EdgeServer edge : edgeServers.values()) {
        //     edge.invalidate(key);
        // }
    }
    
    /**
     * Pre-warm: push content to edge servers before users request it
     * Used for popular content (e.g., homepage, viral video)
     */
    public void prewarm(String key, String region) {
        // TODO: Implement
        // HINT: String content = origin.fetch(key);
        // HINT: if (content == null) return;
        // HINT: List<String> serverIds = regionToServers.getOrDefault(region, new ArrayList<>());
        // HINT: for (String id : serverIds) {
        //     EdgeServer edge = edgeServers.get(id);
        //     if (edge != null) {
        //         edge.put(key, new CachedContent(key, content, content.length(), defaultTtlMs));
        //         System.out.println("    🔥 Prewarmed " + key + " on " + id);
        //     }
        // }
    }
    
    // ===== QUERIES =====
    
    public EdgeServer getEdge(String id) { return edgeServers.get(id); }
    public long getTotalRequests() { return totalRequests.get(); }
    public long getOriginRequests() { return origin.getRequestCount(); }
    
    public void displayStatus() {
        System.out.println("\n--- CDN Status ---");
        System.out.println("Edges: " + edgeServers.size() + ", Total requests: " + totalRequests.get());
        System.out.println("Origin requests: " + origin.getRequestCount() + " (lower = better caching)");
        edgeServers.values().forEach(e -> System.out.println("  " + e));
    }
}

// ===== MAIN TEST CLASS =====

public class CDNSystem {
    public static void main(String[] args) throws Exception {
        System.out.println("=== CDN System LLD ===\n");
        
        // Setup origin
        OriginServer origin = new OriginServer();
        origin.addContent("/images/logo.png", "LOGO_BINARY_DATA_12345");
        origin.addContent("/css/style.css", "body { margin: 0; }");
        origin.addContent("/js/app.js", "console.log('hello')");
        origin.addContent("/video/intro.mp4", "VIDEO_DATA_" + "x".repeat(500));
        
        CDNService cdn = new CDNService(origin, 5000); // 5s TTL for testing
        
        // Register edge servers (small cache for testing eviction)
        System.out.println("=== Setup: Edge Servers ===");
        cdn.addEdgeServer("edge-us-1", "us-east", 200);  // 200 bytes cache
        cdn.addEdgeServer("edge-us-2", "us-east", 200);
        cdn.addEdgeServer("edge-eu-1", "eu-west", 200);
        cdn.addEdgeServer("edge-ap-1", "ap-south", 200);
        System.out.println();
        
        // Test 1: Cache MISS → fetch from origin → cache at edge
        System.out.println("=== Test 1: Cache MISS (first request) ===");
        try {
            String content = cdn.fetchContent("/images/logo.png", "us-east");
            System.out.println("✓ Got: " + (content != null ? content.substring(0, Math.min(20, content.length())) : "null"));
            System.out.println("  Origin requests: " + cdn.getOriginRequests());
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 2: Cache HIT (same content, same region)
        System.out.println("=== Test 2: Cache HIT (second request) ===");
        try {
            String content = cdn.fetchContent("/images/logo.png", "us-east");
            System.out.println("✓ Got from cache (no origin hit)");
            System.out.println("  Origin requests still: " + cdn.getOriginRequests() + " (expect same)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 3: Different region → separate cache MISS
        System.out.println("=== Test 3: Different Region (MISS) ===");
        try {
            cdn.fetchContent("/images/logo.png", "eu-west");
            System.out.println("✓ EU edge fetched from origin (separate cache)");
            System.out.println("  Origin requests: " + cdn.getOriginRequests());
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 4: Cache eviction (LRU — fill cache then add more)
        System.out.println("=== Test 4: LRU Eviction ===");
        try {
            cdn.fetchContent("/css/style.css", "us-east");
            cdn.fetchContent("/js/app.js", "us-east");
            // Cache is ~60 bytes used, now add big item → should evict LRU
            cdn.fetchContent("/video/intro.mp4", "us-east"); // 510 bytes > 200 limit → evicts others
            
            EdgeServer usEdge = cdn.getEdge("edge-us-1");
            System.out.println("✓ Cache after eviction: " + (usEdge != null ? usEdge.getCacheSize() + " items" : "null"));
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 5: TTL expiration
        System.out.println("=== Test 5: TTL Expiration ===");
        try {
            // Use short TTL CDN
            CDNService shortTtl = new CDNService(origin, 100); // 100ms TTL
            shortTtl.addEdgeServer("ttl-edge", "us-east", 1000);
            
            shortTtl.fetchContent("/css/style.css", "us-east"); // MISS → cache
            shortTtl.fetchContent("/css/style.css", "us-east"); // HIT
            
            Thread.sleep(150); // wait for TTL
            
            shortTtl.fetchContent("/css/style.css", "us-east"); // EXPIRED → fetch again
            System.out.println("✓ After TTL: fetched from origin again");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 6: Cache invalidation (purge)
        System.out.println("=== Test 6: Cache Invalidation ===");
        try {
            // Content is cached on multiple edges
            cdn.fetchContent("/css/style.css", "eu-west"); // cache on EU
            
            // Origin updates content
            origin.addContent("/css/style.css", "body { margin: 0; color: red; }");
            cdn.invalidateContent("/css/style.css"); // purge from all edges
            
            // Next request gets new content from origin
            String fresh = cdn.fetchContent("/css/style.css", "eu-west");
            System.out.println("✓ After purge, got fresh: " + fresh);
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 7: Pre-warm
        System.out.println("=== Test 7: Pre-warm ===");
        cdn.prewarm("/js/app.js", "ap-south");
        try {
            long originBefore = cdn.getOriginRequests();
            cdn.fetchContent("/js/app.js", "ap-south"); // should be HIT (pre-warmed)
            System.out.println("✓ Pre-warmed content served (origin requests: " + cdn.getOriginRequests() + ")");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 8: Hit rate
        System.out.println("=== Test 8: Hit Rate ===");
        EdgeServer usEdge = cdn.getEdge("edge-us-1");
        if (usEdge != null) {
            System.out.println("✓ US-East-1 hit rate: " + String.format("%.1f", usEdge.getHitRate()) + "%");
            System.out.println("  Hits: " + usEdge.getCacheHits() + ", Misses: " + usEdge.getCacheMisses());
        }
        System.out.println();
        
        // Test 9: Content not found
        System.out.println("=== Test 9: Exception - Content Not Found ===");
        try {
            cdn.fetchContent("/nonexistent.txt", "us-east");
            System.out.println("✗ Should have thrown");
        } catch (ContentNotFoundException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        }
        System.out.println();
        
        // Test 10: Unhealthy edge → fallback
        System.out.println("=== Test 10: Unhealthy Edge Fallback ===");
        try {
            EdgeServer eu = cdn.getEdge("edge-eu-1");
            if (eu != null) eu.setHealthy(false);
            // Should fall back to any healthy server
            cdn.fetchContent("/images/logo.png", "eu-west");
            System.out.println("✓ Fell back to healthy edge");
        } catch (Exception e) {
            System.out.println("  Fallback: " + e.getMessage());
        }
        System.out.println();
        
        // Display
        cdn.displayStatus();
        
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION:
 * =====================
 * 
 * 1. CDN FLOW:
 *    User → DNS (GeoDNS) → Nearest Edge PoP → Cache HIT? → Return
 *                                            → MISS? → Origin → Cache → Return
 * 
 * 2. CACHE STRATEGIES:
 *    LRU: evict least recently used (used here)
 *    LFU: evict least frequently used
 *    TTL: expire after time-to-live
 *    Best: combine TTL + LRU (expire stale, evict cold)
 * 
 * 3. CACHE INVALIDATION:
 *    TTL-based: content expires after N seconds (simple)
 *    Purge/Invalidate: explicit API call to clear (used here)
 *    Versioned URLs: /style.v2.css (cache forever, new URL on update)
 *    Best practice: versioned URLs + long TTL
 * 
 * 4. CONSISTENCY:
 *    Eventual: edges may serve stale until TTL expires
 *    Purge: origin pushes invalidation to all edges
 *    Trade-off: fresher = more origin load, staler = better performance
 * 
 * 5. PRE-WARMING:
 *    Push popular content to edges before users request
 *    For: homepage, viral content, new product launch
 *    Reduces cold-start cache misses
 * 
 * 6. METRICS:
 *    Cache hit rate: #hits / (#hits + #misses) — target >90%
 *    Origin offload: % of requests served by edge
 *    Latency: edge ~5ms vs origin ~200ms
 *    Bandwidth savings: less origin egress
 * 
 * 7. ARCHITECTURE:
 *    Edge PoPs: 200+ locations worldwide
 *    Origin Shield: intermediate cache between edge and origin
 *    Origin: S3/server — only handles cache misses
 * 
 * 8. REAL-WORLD: CloudFront, Cloudflare, Akamai, Fastly
 * 
 * 9. API:
 *    GET  /{path}                    — fetch content (CDN routing)
 *    POST /purge                     — invalidate content
 *    POST /prewarm                   — pre-warm edges
 *    GET  /stats                     — hit rates, metrics
 *    PUT  /edges/{id}/health         — mark healthy/unhealthy
 */
