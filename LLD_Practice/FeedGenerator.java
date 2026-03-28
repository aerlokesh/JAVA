import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/*
 * FEED GENERATOR SYSTEM - Low Level Design
 * ==========================================
 * 
 * REQUIREMENTS:
 * 1. Users follow other users → see their content in feed
 * 2. Publish content (POST, SHARED_POST, AD)
 * 3. Rank feed: Chronological, Engagement-based, Personalized (Strategy)
 * 4. Cache pre-computed feeds per user with TTL
 * 5. Invalidate cache on new content (Observer)
 * 6. Pagination support
 * 7. Thread-safe
 * 
 * KEY DATA STRUCTURES:
 * - ConcurrentHashMap<userId, FeedUser>: user registry + social graph
 * - ConcurrentHashMap<contentId, FeedContent>: content store
 * - ConcurrentHashMap<userId, List<FeedContent>>: feed cache with TTL
 * 
 * DESIGN PATTERNS:
 * - Strategy: pluggable ranking algorithms (swap at runtime for A/B testing)
 * - Observer: cache invalidation when new content published
 * 
 * COMPLEXITY:
 *   generateFeed:  O(F*C + N log N) — F=followed users, C=content per user, N=total content, sort
 *   publishContent: O(1) + O(followers) for cache invalidation
 *   placeBid:       O(1)
 */

// ==================== EXCEPTIONS ====================

class FeedUserNotFoundException extends Exception {
    FeedUserNotFoundException(String id) { super("User not found: " + id); }
}

class ContentNotFoundException extends Exception {
    ContentNotFoundException(String id) { super("Content not found: " + id); }
}

// ==================== ENUMS ====================

enum ContentType { POST, SHARED_POST, AD }

// ==================== DOMAIN CLASSES ====================

class FeedContent {
    final String contentId;
    final String authorId;
    final String text;
    final ContentType type;
    final LocalDateTime createdAt;
    int likes, comments, shares;
    double relevanceScore;  // set by ranking strategy

    FeedContent(String contentId, String authorId, String text, ContentType type) {
        this.contentId = contentId;
        this.authorId = authorId;
        this.text = text;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

    double engagementScore() { return likes + comments * 2.0 + shares * 3.0; }
    long ageMinutes() { return Duration.between(createdAt, LocalDateTime.now()).toMinutes(); }
}

class FeedUser {
    final String userId;
    final Set<String> following = new HashSet<>();
    final Set<String> followers = new HashSet<>();
    final Set<String> interests = new HashSet<>();     // for personalized ranking
    final List<String> contentIds = new ArrayList<>();  // content authored
    
    FeedUser(String userId) { this.userId = userId; }
}

// ==================== STRATEGY: RANKING ====================

interface FeedRankingStrategy {
    List<FeedContent> rank(List<FeedContent> content, FeedUser user);
    String name();
}

class ChronologicalRanking implements FeedRankingStrategy {
    public List<FeedContent> rank(List<FeedContent> content, FeedUser user) {
        return content.stream()
            .sorted(Comparator.comparing((FeedContent c) -> c.createdAt).reversed())
            .collect(Collectors.toList());
    }
    public String name() { return "Chronological"; }
}

class EngagementRanking implements FeedRankingStrategy {
    /**
     * score = engagementScore / (1 + ageHours * 0.1)  — time decay
     */
    public List<FeedContent> rank(List<FeedContent> content, FeedUser user) {
        for (FeedContent c : content) {
            double decay = 1.0 + c.ageMinutes() / 60.0 * 0.1;
            c.relevanceScore = c.engagementScore() / decay;
        }
        return content.stream()
            .sorted(Comparator.comparingDouble((FeedContent c) -> c.relevanceScore).reversed())
            .collect(Collectors.toList());
    }
    public String name() { return "Engagement"; }
}

class PersonalizedRanking implements FeedRankingStrategy {
    /**
     * Combines engagement + recency + interest boost + content type weight
     */
    public List<FeedContent> rank(List<FeedContent> content, FeedUser user) {
        for (FeedContent c : content) {
            double engagement = c.engagementScore();
            double recency = Math.max(0, 24.0 - c.ageMinutes() / 60.0) / 24.0;
            double interestBoost = user.interests.contains(c.authorId) ? 2.0 : 1.0;
            double typeWeight = switch (c.type) {
                case POST -> 1.0; case SHARED_POST -> 0.9; case AD -> 0.5;
            };
            c.relevanceScore = (engagement * interestBoost + recency * 10) * typeWeight;
        }
        return content.stream()
            .sorted(Comparator.comparingDouble((FeedContent c) -> c.relevanceScore).reversed())
            .collect(Collectors.toList());
    }
    public String name() { return "Personalized"; }
}

// ==================== FEED CACHE ====================

class FeedCache {
    private final Map<String, List<FeedContent>> cache = new ConcurrentHashMap<>();
    private final Map<String, Long> timestamps = new ConcurrentHashMap<>();
    private final long ttlMs;

    FeedCache(long ttlSeconds) { this.ttlMs = ttlSeconds * 1000L; }

    /**
     * Get cached feed (null if expired or missing)
     */
    List<FeedContent> get(String userId) {
        // TODO: Implement
        // HINT: if (!cache.containsKey(userId)) return null;
        // HINT: if (System.currentTimeMillis() - timestamps.getOrDefault(userId, 0L) > ttlMs) {
        //     cache.remove(userId); timestamps.remove(userId); return null;
        // }
        // HINT: return cache.get(userId);
        return null;
    }

    void put(String userId, List<FeedContent> feed) {
        // TODO: Implement
        // HINT: cache.put(userId, new ArrayList<>(feed));
        // HINT: timestamps.put(userId, System.currentTimeMillis());
    }

    void invalidate(String userId) {
        // TODO: Implement
        // HINT: cache.remove(userId); timestamps.remove(userId);
    }

    void invalidateAll(Set<String> userIds) {
        // TODO: Implement
        // HINT: userIds.forEach(this::invalidate);
    }

    boolean isCached(String userId) {
        return cache.containsKey(userId) &&
            System.currentTimeMillis() - timestamps.getOrDefault(userId, 0L) <= ttlMs;
    }
}

// ==================== SERVICE ====================

class FeedService {
    private final Map<String, FeedUser> users = new ConcurrentHashMap<>();
    private final Map<String, FeedContent> content = new ConcurrentHashMap<>();
    private FeedRankingStrategy ranking;
    private FeedCache feedCache;
    private int idCounter;

    FeedService(FeedRankingStrategy ranking, int cacheTtlSec) {
        this.ranking = ranking;
        this.feedCache = new FeedCache(cacheTtlSec);
    }

    void addUser(String userId) {
        // TODO: Implement
        // HINT: users.put(userId, new FeedUser(userId));
    }

    void follow(String followerId, String followeeId) throws FeedUserNotFoundException {
        // TODO: Implement
        // HINT: FeedUser follower = users.get(followerId);
        // HINT: FeedUser followee = users.get(followeeId);
        // HINT: if (follower == null) throw new FeedUserNotFoundException(followerId);
        // HINT: if (followee == null) throw new FeedUserNotFoundException(followeeId);
        // HINT: follower.following.add(followeeId);
        // HINT: followee.followers.add(followerId);
        // HINT: feedCache.invalidate(followerId);
    }

    void addInterest(String userId, String interest) throws FeedUserNotFoundException {
        // TODO: Implement
        // HINT: FeedUser user = users.get(userId);
        // HINT: if (user == null) throw new FeedUserNotFoundException(userId);
        // HINT: user.interests.add(interest);
    }

    /**
     * Publish content → store + invalidate followers' caches
     */
    FeedContent publish(String authorId, String text, ContentType type) throws FeedUserNotFoundException {
        // TODO: Implement
        // HINT: FeedUser author = users.get(authorId);
        // HINT: if (author == null) throw new FeedUserNotFoundException(authorId);
        // HINT: String id = "C-" + (++idCounter);
        // HINT: FeedContent c = new FeedContent(id, authorId, text, type);
        // HINT: content.put(id, c);
        // HINT: author.contentIds.add(id);
        // HINT: feedCache.invalidateAll(author.followers); // Observer: invalidate
        // HINT: return c;
        return null;
    }

    void addEngagement(String contentId, int likes, int comments, int shares) throws ContentNotFoundException {
        // TODO: Implement
        // HINT: FeedContent c = content.get(contentId);
        // HINT: if (c == null) throw new ContentNotFoundException(contentId);
        // HINT: c.likes += likes; c.comments += comments; c.shares += shares;
    }

    /**
     * Generate feed: cache check → aggregate from followed → rank → cache → return
     */
    List<FeedContent> generateFeed(String userId) throws FeedUserNotFoundException {
        // TODO: Implement
        // HINT: FeedUser user = users.get(userId);
        // HINT: if (user == null) throw new FeedUserNotFoundException(userId);
        // HINT: List<FeedContent> cached = feedCache.get(userId);
        // HINT: if (cached != null) return cached;
        // HINT: List<FeedContent> feed = new ArrayList<>();
        // HINT: for (String fid : user.following) {
        //     FeedUser followed = users.get(fid);
        //     if (followed != null) for (String cid : followed.contentIds) {
        //         FeedContent c = content.get(cid);
        //         if (c != null) feed.add(c);
        //     }
        // }
        // HINT: for (String cid : user.contentIds) { // own content
        //     FeedContent c = content.get(cid);
        //     if (c != null) feed.add(c);
        // }
        // HINT: List<FeedContent> ranked = ranking.rank(feed, user);
        // HINT: feedCache.put(userId, ranked);
        // HINT: return ranked;
        return new ArrayList<>();
    }

    List<FeedContent> getFeedPage(String userId, int page, int size) throws FeedUserNotFoundException {
        // TODO: Implement
        // HINT: List<FeedContent> feed = generateFeed(userId);
        // HINT: int start = page * size;
        // HINT: if (start >= feed.size()) return new ArrayList<>();
        // HINT: return feed.subList(start, Math.min(start + size, feed.size()));
        return new ArrayList<>();
    }

    void setRanking(FeedRankingStrategy r) {
        // TODO: Implement
        // HINT: this.ranking = r;
        // HINT: this.feedCache = new FeedCache(60);
    }

    FeedCache getCache() { return feedCache; }
    int totalContent() { return content.size(); }
    int totalUsers() { return users.size(); }
    String currentStrategy() { return ranking.name(); }
}

// ==================== MAIN / TESTS ====================

public class FeedGenerator {
    public static void main(String[] args) {
        System.out.println("=== Feed Generator LLD ===\n");

        FeedService svc = new FeedService(new ChronologicalRanking(), 60);

        // Test 1: Users + Follow
        System.out.println("=== Test 1: Setup ===");
        try {
            svc.addUser("alice"); svc.addUser("bob"); svc.addUser("charlie");
            svc.follow("alice", "bob"); svc.follow("alice", "charlie");
            svc.follow("bob", "alice");
            System.out.println("✓ Users + follows\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // Test 2: Publish + Engagement
        System.out.println("=== Test 2: Publish ===");
        try {
            FeedContent c1 = svc.publish("bob", "Bob's coffee ☕", ContentType.POST);
            FeedContent c2 = svc.publish("charlie", "Charlie's workout 💪", ContentType.POST);
            FeedContent c3 = svc.publish("alice", "Alice's trip 🌴", ContentType.POST);
            if (c1 != null) svc.addEngagement(c1.contentId, 50, 10, 5);
            if (c2 != null) svc.addEngagement(c2.contentId, 100, 30, 20);
            System.out.println("  Content: " + svc.totalContent());
            System.out.println("✓ Published\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // Test 3: Chronological Feed
        System.out.println("=== Test 3: Chronological ===");
        try {
            List<FeedContent> feed = svc.generateFeed("alice");
            System.out.println("  Feed: " + feed.size() + " items");
            feed.forEach(c -> System.out.printf("    %s by %s (score=%.1f)%n", c.text, c.authorId, c.relevanceScore));
            System.out.println("✓ Newest first\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // Test 4: Engagement Ranking
        System.out.println("=== Test 4: Engagement ===");
        try {
            svc.setRanking(new EngagementRanking());
            List<FeedContent> feed = svc.generateFeed("alice");
            feed.forEach(c -> System.out.printf("    %s (score=%.1f)%n", c.text, c.relevanceScore));
            System.out.println("✓ Most engaging first\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // Test 5: Personalized Ranking
        System.out.println("=== Test 5: Personalized ===");
        try {
            svc.addInterest("alice", "bob");
            svc.setRanking(new PersonalizedRanking());
            List<FeedContent> feed = svc.generateFeed("alice");
            feed.forEach(c -> System.out.printf("    %s (score=%.1f)%n", c.text, c.relevanceScore));
            System.out.println("✓ Bob boosted\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // Test 6: Cache
        System.out.println("=== Test 6: Caching ===");
        try {
            boolean before = svc.getCache().isCached("alice");
            svc.generateFeed("alice");
            boolean after = svc.getCache().isCached("alice");
            System.out.println("  Before: " + before + ", After: " + after);
            System.out.println("✓ Cache works\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // Test 7: Cache Invalidation
        System.out.println("=== Test 7: Invalidation ===");
        try {
            boolean cached = svc.getCache().isCached("alice");
            svc.publish("bob", "Breaking news! 🎉", ContentType.POST);
            boolean afterPublish = svc.getCache().isCached("alice");
            System.out.println("  Before: " + cached + ", After: " + afterPublish + " (invalidated)");
            System.out.println("✓ Observer works\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // Test 8: Pagination
        System.out.println("=== Test 8: Pagination ===");
        try {
            svc.setRanking(new ChronologicalRanking());
            List<FeedContent> p0 = svc.getFeedPage("alice", 0, 2);
            List<FeedContent> p1 = svc.getFeedPage("alice", 1, 2);
            List<FeedContent> p99 = svc.getFeedPage("alice", 99, 2);
            System.out.println("  Page 0: " + p0.size() + ", Page 1: " + p1.size() + ", Page 99: " + p99.size());
            System.out.println("✓ Pagination\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // Test 9: User Not Found
        System.out.println("=== Test 9: Not Found ===");
        try {
            svc.generateFeed("ghost");
            System.out.println("✗ Should have thrown");
        } catch (FeedUserNotFoundException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // Test 10: Content Not Found
        System.out.println();
        System.out.println("=== Test 10: Content Not Found ===");
        try {
            svc.addEngagement("FAKE", 10, 0, 0);
            System.out.println("✗ Should have thrown");
        } catch (ContentNotFoundException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        System.out.println("\n=== All Tests Complete! ===");
    }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. FAN-OUT ON WRITE vs READ:
 *    Write: push to followers' feeds on publish (good for normal users)
 *    Read: compute at request time (good for celebrities with millions of followers)
 *    Hybrid: most production systems (Facebook, Twitter)
 * 
 * 2. RANKING: Strategy pattern → swap algorithms for A/B testing
 *    Chrono: simple timestamp sort
 *    Engagement: likes*1 + comments*2 + shares*3, with time decay
 *    Personalized: engagement + recency + interest affinity + content type weight
 * 
 * 3. CACHE: TTL-based + event-based invalidation (hybrid)
 *    New post → invalidate all followers' cached feeds
 *    Redis in production
 * 
 * 4. PAGINATION: offset-based (simple) or cursor-based (consistent with real-time)
 * 
 * 5. SCALE: Kafka for fan-out events, Redis for feed cache, Cassandra for content
 * 
 * 6. REAL-WORLD: Facebook News Feed, Twitter Timeline, Instagram Explore
 */
