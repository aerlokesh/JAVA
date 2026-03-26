import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

// ===== ENUMS =====

enum CrawlStatus { PENDING, CRAWLING, SUCCESS, FAILED, SKIPPED }

// ===== INTERFACE =====

/**
 * Strategy: how to fetch page content
 * In real system: HTTP client with timeouts, user-agent, retries
 */
interface PageFetcher {
    CrawlResult fetch(String url) throws Exception;
}

// ===== DOMAIN CLASSES =====

class CrawlResult {
    private final String url;
    private final String content;         // HTML body
    private final List<String> links;     // extracted URLs from page
    private final int statusCode;
    
    public CrawlResult(String url, String content, List<String> links, int statusCode) {
        this.url = url;
        this.content = content;
        this.links = links;
        this.statusCode = statusCode;
    }
    
    public String getUrl() { return url; }
    public String getContent() { return content; }
    public List<String> getLinks() { return links; }
    public int getStatusCode() { return statusCode; }
}

class CrawledPage {
    private final String url;
    private CrawlStatus status;
    private String content;
    private List<String> outLinks;    // links found on this page
    private int depth;                 // how many hops from seed URL
    private final LocalDateTime crawledAt;
    
    public CrawledPage(String url, int depth) {
        this.url = url;
        this.depth = depth;
        this.status = CrawlStatus.PENDING;
        this.outLinks = new ArrayList<>();
        this.crawledAt = LocalDateTime.now();
    }
    
    public String getUrl() { return url; }
    public CrawlStatus getStatus() { return status; }
    public String getContent() { return content; }
    public List<String> getOutLinks() { return outLinks; }
    public int getDepth() { return depth; }
    
    public void setStatus(CrawlStatus s) { this.status = s; }
    public void setContent(String c) { this.content = c; }
    public void setOutLinks(List<String> l) { this.outLinks = l; }
    
    @Override
    public String toString() { return url + "[" + status + ", depth=" + depth + ", links=" + outLinks.size() + "]"; }
}

// ===== FETCHER IMPLEMENTATIONS =====

/**
 * Simulated fetcher with a fake web graph
 */
class SimulatedFetcher implements PageFetcher {
    private final Map<String, List<String>> webGraph;  // url → links on that page
    
    public SimulatedFetcher() {
        this.webGraph = new HashMap<>();
    }
    
    public void addPage(String url, String... links) {
        webGraph.put(url, Arrays.asList(links));
    }
    
    @Override
    public CrawlResult fetch(String url) throws Exception {
        List<String> links = webGraph.get(url);
        if (links == null) throw new Exception("404 Not Found: " + url);
        return new CrawlResult(url, "<html>" + url + "</html>", links, 200);
    }
}

// ===== SERVICE =====

/**
 * Web Crawler - Low Level Design (LLD)
 * 
 * PROBLEM: Design a web crawler that can:
 * 1. Start from seed URL(s)
 * 2. Fetch page, extract links
 * 3. BFS crawl to maxDepth
 * 4. Avoid revisiting URLs (deduplication)
 * 5. Respect maxPages limit
 * 6. Filter URLs by domain (stay within allowed domains)
 * 7. Track crawl statistics
 * 
 * KEY DATA STRUCTURES:
 * - Queue (BFS frontier): URLs to crawl next
 * - Set (visited): URLs already seen (dedup)
 * - Map (results): url → CrawledPage
 * 
 * ALGORITHM: BFS (breadth-first)
 *   1. Add seed URLs to queue
 *   2. While queue not empty and under limits:
 *      a. Poll URL from queue
 *      b. Skip if visited
 *      c. Fetch page → extract links
 *      d. Add new links to queue (if within depth + domain)
 *      e. Mark as visited
 * 
 * PATTERNS: Strategy (fetcher), BFS traversal
 */
class WebCrawler {
    private final PageFetcher fetcher;
    private final Map<String, CrawledPage> crawledPages;   // url → result
    private final Set<String> visited;                      // dedup set
    private final Queue<String[]> frontier;                 // BFS queue: [url, depth]
    private final Set<String> allowedDomains;               // only crawl these domains
    private final int maxDepth;
    private final int maxPages;
    private final AtomicInteger successCount;
    private final AtomicInteger failedCount;
    
    public WebCrawler(PageFetcher fetcher, int maxDepth, int maxPages) {
        this.fetcher = fetcher;
        this.crawledPages = new LinkedHashMap<>();
        this.visited = new HashSet<>();
        this.frontier = new LinkedList<>();
        this.allowedDomains = new HashSet<>();
        this.maxDepth = maxDepth;
        this.maxPages = maxPages;
        this.successCount = new AtomicInteger(0);
        this.failedCount = new AtomicInteger(0);
    }
    
    /**
     * Add allowed domain (only crawl URLs matching these domains)
     * If empty, allow all domains
     */
    public void addAllowedDomain(String domain) {
        allowedDomains.add(domain);
    }
    
    /**
     * Start crawling from seed URLs
     * 
     * IMPLEMENTATION HINTS:
     * 1. Add each seed URL to frontier with depth=0
     * 2. Call processQueue()
     * 3. Return all crawled pages
     */
    public List<CrawledPage> crawl(String... seedUrls) {
        // TODO: Implement
        // HINT: for (String url : seedUrls) {
        //     frontier.offer(new String[]{url, "0"});
        // }
        // HINT: processQueue();
        // HINT: return new ArrayList<>(crawledPages.values());
        return null;
    }
    
    /**
     * BFS processing loop
     * 
     * IMPLEMENTATION HINTS:
     * 1. While frontier not empty AND crawled < maxPages:
     * 2. Poll [url, depth] from queue
     * 3. Skip if already visited
     * 4. Skip if depth > maxDepth
     * 5. Skip if URL not in allowed domain (if domains specified)
     * 6. Mark as visited
     * 7. Call crawlPage(url, depth)
     */
    private void processQueue() {
        // TODO: Implement
        // HINT: while (!frontier.isEmpty() && crawledPages.size() < maxPages) {
        //     String[] entry = frontier.poll();
        //     String url = entry[0];
        //     int depth = Integer.parseInt(entry[1]);
        //     
        //     if (visited.contains(url)) continue;
        //     if (depth > maxDepth) continue;
        //     if (!isAllowedDomain(url)) continue;
        //     
        //     visited.add(url);
        //     crawlPage(url, depth);
        // }
    }
    
    /**
     * Crawl a single page
     * 
     * IMPLEMENTATION HINTS:
     * 1. Create CrawledPage with CRAWLING status
     * 2. Try: fetch page using fetcher
     * 3. On success:
     *    → Set status=SUCCESS, store content and links
     *    → For each extracted link: add to frontier with depth+1 (if not visited)
     *    → Increment successCount
     * 4. On failure:
     *    → Set status=FAILED
     *    → Increment failedCount
     * 5. Store in crawledPages map
     */
    private void crawlPage(String url, int depth) {
        // TODO: Implement
        // HINT: CrawledPage page = new CrawledPage(url, depth);
        // HINT: page.setStatus(CrawlStatus.CRAWLING);
        //
        // HINT: try {
        //     CrawlResult result = fetcher.fetch(url);
        //     page.setStatus(CrawlStatus.SUCCESS);
        //     page.setContent(result.getContent());
        //     page.setOutLinks(result.getLinks());
        //     successCount.incrementAndGet();
        //     System.out.println("  ✅ [depth=" + depth + "] " + url + " → " + result.getLinks().size() + " links");
        //     
        //     // Add discovered links to frontier
        //     for (String link : result.getLinks()) {
        //         if (!visited.contains(link)) {
        //             frontier.offer(new String[]{link, String.valueOf(depth + 1)});
        //         }
        //     }
        // } catch (Exception e) {
        //     page.setStatus(CrawlStatus.FAILED);
        //     failedCount.incrementAndGet();
        //     System.out.println("  ❌ [depth=" + depth + "] " + url + " → " + e.getMessage());
        // }
        //
        // HINT: crawledPages.put(url, page);
    }
    
    /**
     * Check if URL belongs to an allowed domain
     * 
     * IMPLEMENTATION HINTS:
     * 1. If no domains specified → allow all
     * 2. Check if URL contains any allowed domain
     */
    private boolean isAllowedDomain(String url) {
        // TODO: Implement
        // HINT: if (allowedDomains.isEmpty()) return true;
        // HINT: for (String domain : allowedDomains) {
        //     if (url.contains(domain)) return true;
        // }
        // HINT: return false;
        return true;
    }
    
    // ===== QUERIES =====
    
    public CrawledPage getPage(String url) { return crawledPages.get(url); }
    public int getSuccessCount() { return successCount.get(); }
    public int getFailedCount() { return failedCount.get(); }
    public int getTotalCrawled() { return crawledPages.size(); }
    public int getVisitedCount() { return visited.size(); }
    
    /**
     * Get all successfully crawled pages at a specific depth
     */
    public List<CrawledPage> getPagesAtDepth(int depth) {
        // TODO: Implement
        // HINT: List<CrawledPage> result = new ArrayList<>();
        // HINT: for (CrawledPage p : crawledPages.values()) {
        //     if (p.getDepth() == depth && p.getStatus() == CrawlStatus.SUCCESS) result.add(p);
        // }
        // HINT: return result;
        return null;
    }
    
    public void displayStats() {
        System.out.println("\n--- Crawl Stats ---");
        System.out.println("Crawled: " + crawledPages.size() + ", Success: " + successCount.get() 
            + ", Failed: " + failedCount.get());
        System.out.println("Visited URLs: " + visited.size() + ", Frontier remaining: " + frontier.size());
        for (CrawledPage p : crawledPages.values()) {
            System.out.println("  " + p);
        }
    }
}

// ===== MAIN TEST CLASS =====

public class WebCrawlerSystem {
    public static void main(String[] args) {
        System.out.println("=== Web Crawler LLD ===\n");
        
        // Build simulated web graph
        SimulatedFetcher web = new SimulatedFetcher();
        web.addPage("https://example.com", 
            "https://example.com/about", "https://example.com/blog", "https://example.com/contact");
        web.addPage("https://example.com/about", 
            "https://example.com", "https://example.com/team");
        web.addPage("https://example.com/blog", 
            "https://example.com/blog/post1", "https://example.com/blog/post2");
        web.addPage("https://example.com/contact", 
            "https://example.com");
        web.addPage("https://example.com/team", 
            "https://example.com/about");
        web.addPage("https://example.com/blog/post1", 
            "https://example.com/blog", "https://external.com/ad");
        web.addPage("https://example.com/blog/post2", 
            "https://example.com/blog");
        web.addPage("https://external.com/ad"); // dead end
        
        // Test 1: Basic crawl from seed
        System.out.println("=== Test 1: Basic Crawl (depth=2) ===");
        WebCrawler crawler = new WebCrawler(web, 2, 100);
        List<CrawledPage> pages = crawler.crawl("https://example.com");
        System.out.println("✓ Crawled: " + (pages != null ? pages.size() : 0) + " pages");
        System.out.println("  Success: " + crawler.getSuccessCount() + ", Failed: " + crawler.getFailedCount());
        System.out.println();
        
        // Test 2: Deduplication (no URL crawled twice)
        System.out.println("=== Test 2: Deduplication ===");
        System.out.println("✓ Visited: " + crawler.getVisitedCount() + " unique URLs");
        System.out.println("  Total crawled: " + crawler.getTotalCrawled() + " (should equal visited)");
        System.out.println();
        
        // Test 3: Depth limit
        System.out.println("=== Test 3: Depth Limit ===");
        WebCrawler shallow = new WebCrawler(web, 1, 100);
        List<CrawledPage> shallowPages = shallow.crawl("https://example.com");
        System.out.println("✓ Depth=1 crawled: " + (shallowPages != null ? shallowPages.size() : 0) + " pages");
        List<CrawledPage> depth0 = shallow.getPagesAtDepth(0);
        List<CrawledPage> depth1 = shallow.getPagesAtDepth(1);
        System.out.println("  Depth 0: " + (depth0 != null ? depth0.size() : 0));
        System.out.println("  Depth 1: " + (depth1 != null ? depth1.size() : 0));
        System.out.println();
        
        // Test 4: Max pages limit
        System.out.println("=== Test 4: Max Pages Limit ===");
        WebCrawler limited = new WebCrawler(web, 10, 3); // deep but only 3 pages
        List<CrawledPage> limitedPages = limited.crawl("https://example.com");
        System.out.println("✓ Max 3 pages: " + (limitedPages != null ? limitedPages.size() : 0) + " (expect ≤3)");
        System.out.println();
        
        // Test 5: Domain filtering
        System.out.println("=== Test 5: Domain Filter ===");
        WebCrawler filtered = new WebCrawler(web, 5, 100);
        filtered.addAllowedDomain("example.com");
        List<CrawledPage> filteredPages = filtered.crawl("https://example.com");
        System.out.println("✓ Only example.com: " + (filteredPages != null ? filteredPages.size() : 0) + " pages");
        // Should NOT have crawled external.com
        CrawledPage external = filtered.getPage("https://external.com/ad");
        System.out.println("  external.com crawled: " + (external != null) + " (expect false)");
        System.out.println();
        
        // Test 6: Handle failed pages (404)
        System.out.println("=== Test 6: Failed Pages ===");
        web.addPage("https://example.com/broken-links", "https://example.com/404page");
        // 404page not in web graph → will fail
        WebCrawler failCrawler = new WebCrawler(web, 1, 100);
        failCrawler.crawl("https://example.com/broken-links");
        System.out.println("✓ Failed: " + failCrawler.getFailedCount());
        System.out.println();
        
        // Test 7: Multiple seed URLs
        System.out.println("=== Test 7: Multiple Seeds ===");
        WebCrawler multiSeed = new WebCrawler(web, 0, 100);
        List<CrawledPage> multi = multiSeed.crawl("https://example.com/about", "https://example.com/blog");
        System.out.println("✓ 2 seeds (depth=0): " + (multi != null ? multi.size() : 0) + " (expect 2)");
        System.out.println();
        
        // Test 8: Crawl stats
        System.out.println("=== Test 8: Crawl Stats ===");
        crawler.displayStats();
        
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION:
 * =====================
 * 
 * 1. ALGORITHM — BFS:
 *    Queue (frontier): URLs to visit next
 *    Set (visited): already seen (dedup)
 *    BFS guarantees we crawl closer pages first (by depth)
 *    DFS alternative: goes deep first, risk of infinite depth
 * 
 * 2. DEDUPLICATION:
 *    HashSet of visited URLs → O(1) lookup
 *    Normalize URLs first: remove trailing /, lowercase, remove fragments
 *    At scale: Bloom filter (probabilistic, space-efficient)
 * 
 * 3. POLITENESS:
 *    Respect robots.txt (which paths are allowed)
 *    Rate limit per domain (max 1 req/sec per domain)
 *    Random delay between requests
 *    User-Agent header identifying the crawler
 * 
 * 4. SCALABILITY:
 *    Single machine: ~100 pages/sec
 *    Distributed: partition URLs by domain hash → each worker handles subset
 *    URL frontier in Redis/Kafka (distributed queue)
 *    Dedup in Redis SET or Bloom filter
 * 
 * 5. ARCHITECTURE (Distributed):
 *    URL Frontier (Kafka) → Crawler Workers → Content Store (S3)
 *         ↑                      ↓                    ↓
 *    Link Extractor ←── HTML Parser ──→ URL Filter → Dedup
 * 
 * 6. CHALLENGES:
 *    Spider traps: infinite URLs (calendar?date=...) → max depth
 *    Duplicate content: same page, different URL → content hash
 *    Dynamic JS: need headless browser (Puppeteer)
 *    Rate limits: respect 429, exponential backoff
 * 
 * 7. REAL-WORLD: Googlebot, Bingbot, Scrapy, Apache Nutch
 * 
 * 8. API (if building as service):
 *    POST /crawl          — start crawl with seeds + config
 *    GET  /crawl/{id}     — get crawl status
 *    GET  /pages?url=X    — get crawled page
 *    GET  /stats           — crawl metrics
 */
