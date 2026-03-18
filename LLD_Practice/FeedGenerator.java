import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

// ===== CUSTOM EXCEPTION CLASSES =====

/**
 * Exception thrown when user is not found
 * WHEN TO THROW:
 * - User ID doesn't exist in system
 * - Generating feed for non-existent user
 */
class FeedUserNotFoundException extends Exception {
    private String userId;
    
    public FeedUserNotFoundException(String userId) {
        super("User not found: " + userId);
        this.userId = userId;
    }
    
    public String getUserId() { return userId; }
}

/**
 * Exception thrown when content/post is not found
 * WHEN TO THROW:
 * - Post ID doesn't exist
 * - Interacting with deleted/expired content
 */
class ContentNotFoundException extends Exception {
    private String contentId;
    
    public ContentNotFoundException(String contentId) {
        super("Content not found: " + contentId);
        this.contentId = contentId;
    }
    
    public String getContentId() { return contentId; }
}

/**
 * Exception thrown when feed generation fails
 * WHEN TO THROW:
 * - No content sources available
 * - Ranking algorithm fails
 * - Cache corruption
 */
class FeedGenerationException extends Exception {
    public FeedGenerationException(String message) {
        super(message);
    }
}

// ===== ENUMS =====

/**
 * Types of content that can appear in a feed
 */
enum ContentType {
    POST,           // Regular user post (text, image, video)
    SHARED_POST,    // Reshared/retweeted content
    AD,             // Sponsored/advertisement content
    STORY,          // Ephemeral content (24h expiry)
    RECOMMENDATION  // Suggested content from non-followed sources
}

/**
 * Feed ranking strategy types
 */
enum RankingStrategyType {
    CHRONOLOGICAL,      // Newest first (simple timestamp sort)
    ENGAGEMENT_BASED,   // Ranked by likes, comments, shares
    PERSONALIZED        // ML-based scoring using user preferences
}

// ===== DOMAIN CLASSES =====

/**
 * Represents a piece of content (post) in the system
 */
class FeedContent {
    String contentId;
    String authorId;
    String text;
    ContentType type;
    LocalDateTime createdAt;
    int likes;
    int comments;
    int shares;
    double relevanceScore;  // Computed by ranking algorithm
    
    public FeedContent(String contentId, String authorId, String text, ContentType type) {
        this.contentId = contentId;
        this.authorId = authorId;
        this.text = text;
        this.type = type;
        this.createdAt = LocalDateTime.now();
        this.likes = 0;
        this.comments = 0;
        this.shares = 0;
        this.relevanceScore = 0.0;
    }
    
    public double getEngagementScore() {
        return likes * 1.0 + comments * 2.0 + shares * 3.0;
    }
    
    public long getAgeMinutes() {
        return Duration.between(createdAt, LocalDateTime.now()).toMinutes();
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s by %s (👍%d 💬%d 🔄%d score=%.1f)",
            type, text.substring(0, Math.min(30, text.length())), 
            authorId, likes, comments, shares, relevanceScore);
    }
}

/**
 * Represents a user in the feed system
 */
class FeedUser {
    String userId;
    String name;
    Set<String> following;       // User IDs this user follows
    Set<String> followers;       // User IDs following this user
    Set<String> interests;       // Topics of interest (for personalization)
    List<String> contentIds;     // Content created by this user
    
    public FeedUser(String userId, String name) {
        this.userId = userId;
        this.name = name;
        this.following = new HashSet<>();
        this.followers = new HashSet<>();
        this.interests = new HashSet<>();
        this.contentIds = new ArrayList<>();
    }
    
    @Override
    public String toString() {
        return name + " (" + userId + ") [following:" + following.size() + 
               ", followers:" + followers.size() + "]";
    }
}

// ===== INTERFACE - STRATEGY PATTERN FOR RANKING =====

/**
 * Strategy interface for feed ranking algorithms
 * 
 * INTERVIEW DISCUSSION:
 * - Why Strategy pattern? (Swap algorithms at runtime, A/B testing)
 * - How to combine multiple signals? (Weighted scoring)
 * - How does ML-based ranking work? (Feature extraction + model inference)
 */
interface FeedRankingStrategy {
    List<FeedContent> rank(List<FeedContent> content, FeedUser user);
    String getName();
}

// ===== RANKING STRATEGY IMPLEMENTATIONS =====

/**
 * CHRONOLOGICAL RANKING
 * =====================
 * Simply sorts by creation time (newest first)
 * 
 * PROS: Simple, predictable, no filter bubble
 * CONS: Doesn't surface best content, can be noisy
 * USE CASE: Twitter's "Latest" tab, real-time news feeds
 */
class ChronologicalRanking implements FeedRankingStrategy {
    /**
     * IMPLEMENTATION HINTS:
     * 1. Sort content by createdAt in descending order (newest first)
     * 2. Set relevanceScore to 0 (no ranking applied)
     * 3. Return sorted list
     * 
     * TIME COMPLEXITY: O(n log n)
     */
    @Override
    public List<FeedContent> rank(List<FeedContent> content, FeedUser user) {
        // HINT: return content.stream()
        //     .sorted(Comparator.comparing((FeedContent c) -> c.createdAt).reversed())
        //     .collect(Collectors.toList());
        return content.stream().sorted(Comparator.comparing((FeedContent c)->c.createdAt).reversed()).collect(Collectors.toList());
    }
    
    @Override
    public String getName() { return "Chronological"; }
}

/**
 * ENGAGEMENT-BASED RANKING
 * =========================
 * Ranks by engagement metrics (likes, comments, shares)
 * 
 * ALGORITHM:
 *   score = likes * 1.0 + comments * 2.0 + shares * 3.0
 *   Apply time decay: score / (1 + ageInHours * 0.1)
 * 
 * PROS: Surfaces popular content, rewards quality
 * CONS: Echo chamber effect, penalizes new content
 * USE CASE: Reddit "Hot", Hacker News ranking
 */
class EngagementRanking implements FeedRankingStrategy {
    /**
     * IMPLEMENTATION HINTS:
     * 1. For each content, calculate engagement score
     * 2. Apply time decay: divide by (1 + ageMinutes/60 * 0.1)
     * 3. Set relevanceScore on each content
     * 4. Sort by relevanceScore descending
     * 5. Return sorted list
     * 
     * TIME COMPLEXITY: O(n log n)
     */
    @Override
    public List<FeedContent> rank(List<FeedContent> content, FeedUser user) {
        // HINT: for (FeedContent c : content) {
        //     double engagement = c.getEngagementScore();
        //     double ageHours = c.getAgeMinutes() / 60.0;
        //     double timeDecay = 1.0 + ageHours * 0.1;
        //     c.relevanceScore = engagement / timeDecay;
        // }
        // HINT: return content.stream()
        //     .sorted(Comparator.comparingDouble((FeedContent c) -> c.relevanceScore).reversed())
        //     .collect(Collectors.toList());
        for(FeedContent c:content){
            double engagement=c.getEngagementScore();
            double ageHours = c.getAgeMinutes() / 60.0;
            double timeDecay = 1.0 + ageHours * 0.1;
            c.relevanceScore = engagement / timeDecay;
        }
        return content.stream().sorted(Comparator.comparingDouble((FeedContent c)->c.relevanceScore).reversed()).collect(Collectors.toList());
    }
    
    @Override
    public String getName() { return "EngagementBased"; }
}

/**
 * PERSONALIZED RANKING
 * =====================
 * Combines engagement, recency, and user interest matching
 * 
 * ALGORITHM:
 *   engagementScore = likes * 1 + comments * 2 + shares * 3
 *   recencyBoost = max(0, 24 - ageInHours) / 24  (1.0 for new, 0 for 24h+)
 *   interestBoost = (content matches user interest) ? 2.0 : 1.0
 *   typeBoost = (POST=1.0, AD=0.5, RECOMMENDATION=0.8, etc.)
 *   finalScore = (engagementScore * interestBoost + recencyBoost * 10) * typeBoost
 * 
 * PROS: Most relevant content for each user
 * CONS: Filter bubble, harder to debug, needs user data
 * USE CASE: Facebook News Feed, Instagram Explore, TikTok For You
 */
class PersonalizedRanking implements FeedRankingStrategy {
    /**
     * IMPLEMENTATION HINTS:
     * 1. Calculate engagement score for each content
     * 2. Calculate recency boost (newer = higher)
     * 3. Check if content author or topic matches user interests
     * 4. Apply content type boost (posts > ads)
     * 5. Combine all signals into final score
     * 6. Set relevanceScore and sort descending
     * 
     * TIME COMPLEXITY: O(n log n)
     */
    @Override
    public List<FeedContent> rank(List<FeedContent> content, FeedUser user) {
        // HINT: for (FeedContent c : content) {
        //     double engagement = c.getEngagementScore();
        //     double ageHours = c.getAgeMinutes() / 60.0;
        //     double recencyBoost = Math.max(0, 24.0 - ageHours) / 24.0;
        //     double interestBoost = user.interests.contains(c.authorId) ? 2.0 : 1.0;
        //     double typeBoost = switch(c.type) {
        //         case POST -> 1.0; case SHARED_POST -> 0.9;
        //         case AD -> 0.5; case STORY -> 1.1; case RECOMMENDATION -> 0.8;
        //     };
        //     c.relevanceScore = (engagement * interestBoost + recencyBoost * 10) * typeBoost;
        // }
        // HINT: Sort by relevanceScore descending
        for (FeedContent c : content) {
            double engagement = c.getEngagementScore();
            double ageHours = c.getAgeMinutes() / 60.0;
            double recencyBoost = Math.max(0, 24.0 - ageHours) / 24.0;
            double interestBoost = user.interests.contains(c.authorId) ? 2.0 : 1.0;
            double typeBoost = switch(c.type) {
                case POST -> 1.0; case SHARED_POST -> 0.9;
                case AD -> 0.5; case STORY -> 1.1; case RECOMMENDATION -> 0.8;
            };
            c.relevanceScore = (engagement * interestBoost + recencyBoost * 10) * typeBoost;
        }
        return content.stream().sorted(Comparator.comparing((FeedContent c)-> c.relevanceScore).reversed()).collect(Collectors.toList());
    }
    
    @Override
    public String getName() { return "Personalized"; }
}

// ===== OBSERVER PATTERN FOR NEW CONTENT =====

/**
 * Observer interface - notified when new content is published
 * 
 * INTERVIEW DISCUSSION:
 * - Why Observer? (Decouple content creation from feed invalidation)
 * - Real-world: Kafka events, pub/sub for feed fan-out
 */
interface FeedObserver {
    void onNewContent(FeedContent content);
}

// ===== FEED CACHE =====

/**
 * Per-user feed cache with TTL
 * 
 * INTERVIEW DISCUSSION:
 * - Why cache feeds? (Expensive to recompute on every request)
 * - Cache invalidation strategies? (TTL, event-based, hybrid)
 * - Push vs Pull model for feed generation?
 */
class FeedCache {
    private Map<String, List<FeedContent>> cache;  // userId -> cached feed
    private Map<String, Long> cacheTimestamps;     // userId -> cache time
    private long ttlMillis;
    
    public FeedCache(long ttlSeconds) {
        this.cache = new ConcurrentHashMap<>();
        this.cacheTimestamps = new ConcurrentHashMap<>();
        this.ttlMillis = ttlSeconds * 1000L;
    }
    
    /**
     * Get cached feed for user
     * 
     * IMPLEMENTATION HINTS:
     * 1. Check if cache entry exists for userId
     * 2. Check if cache entry has expired (current time - cacheTime > ttl)
     * 3. If valid, return cached feed
     * 4. If expired or missing, return null (cache miss)
     * 
     * @param userId User to get cached feed for
     * @return Cached feed or null if miss
     */
    public List<FeedContent> get(String userId) {
        // TODO: Implement
        // HINT: if (!cache.containsKey(userId)) return null;
        // HINT: long cachedAt = cacheTimestamps.getOrDefault(userId, 0L);
        // HINT: if (System.currentTimeMillis() - cachedAt > ttlMillis) {
        //     cache.remove(userId);
        //     cacheTimestamps.remove(userId);
        //     return null;
        // }
        // HINT: return cache.get(userId);
        return null;
    }
    
    /**
     * Store feed in cache
     * 
     * IMPLEMENTATION HINTS:
     * 1. Store feed list for userId
     * 2. Record current timestamp
     * 
     * @param userId User ID
     * @param feed Feed to cache
     */
    public void put(String userId, List<FeedContent> feed) {
        // TODO: Implement
        // HINT: cache.put(userId, new ArrayList<>(feed));
        // HINT: cacheTimestamps.put(userId, System.currentTimeMillis());
    }
    
    /**
     * Invalidate cache for a specific user
     * 
     * IMPLEMENTATION HINTS:
     * 1. Remove user's cache entry
     * 2. Remove timestamp entry
     * 
     * @param userId User whose cache to invalidate
     */
    public void invalidate(String userId) {
        // TODO: Implement
        // HINT: cache.remove(userId);
        // HINT: cacheTimestamps.remove(userId);
    }
    
    /**
     * Invalidate cache for multiple users (e.g., all followers of content author)
     * 
     * @param userIds Set of user IDs to invalidate
     */
    public void invalidateAll(Set<String> userIds) {
        // TODO: Implement
        // HINT: userIds.forEach(this::invalidate);
    }
    
    public boolean isCached(String userId) {
        return cache.containsKey(userId) && 
               System.currentTimeMillis() - cacheTimestamps.getOrDefault(userId, 0L) <= ttlMillis;
    }
    
    public int size() { return cache.size(); }
}

// ===== MAIN FEED GENERATOR SERVICE =====

/**
 * Feed Generator System - Low Level Design (LLD)
 * 
 * PROBLEM STATEMENT:
 * Design a social media feed generator that can:
 * 1. Aggregate content from multiple sources (friends, followed pages, ads)
 * 2. Rank content using pluggable strategies (chronological, engagement, personalized)
 * 3. Cache pre-computed feeds per user
 * 4. Invalidate cache on new high-priority content (observer pattern)
 * 5. Support pagination and lazy loading
 * 6. Handle different content types (posts, ads, stories, recommendations)
 * 
 * REQUIREMENTS:
 * - Functional: Generate feed, rank content, paginate, cache
 * - Non-Functional: Low latency (<100ms), handle millions of users, fresh content
 * 
 * INTERVIEW HINTS:
 * - Discuss push vs pull model for feed generation
 * - Talk about fan-out on write vs fan-out on read
 * - Mention cache invalidation strategies
 * - Consider ML-based ranking in production
 * - Discuss A/B testing different ranking algorithms
 */
class FeedGeneratorService implements FeedObserver {
    private Map<String, FeedUser> users;
    private Map<String, FeedContent> allContent;
    private FeedRankingStrategy rankingStrategy;
    private FeedCache feedCache;
    private int defaultPageSize;
    private int contentIdCounter;
    
    public FeedGeneratorService(FeedRankingStrategy rankingStrategy, int cacheTtlSeconds) {
        this.users = new ConcurrentHashMap<>();
        this.allContent = new ConcurrentHashMap<>();
        this.rankingStrategy = rankingStrategy;
        this.feedCache = new FeedCache(cacheTtlSeconds);
        this.defaultPageSize = 10;
        this.contentIdCounter = 0;
    }
    
    /**
     * Register a new user
     * 
     * @param userId User ID
     * @param name User name
     * @throws FeedUserNotFoundException if user already exists (reuse exception for simplicity)
     */
    public void addUser(String userId, String name) {
        // TODO: Implement
        // HINT: users.put(userId, new FeedUser(userId, name));
    }
    
    /**
     * Make user1 follow user2
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate both users exist
     * 2. Add user2 to user1's following set
     * 3. Add user1 to user2's followers set
     * 4. Invalidate user1's feed cache (new content source)
     * 
     * @param followerId User who wants to follow
     * @param followeeId User to be followed
     * @throws FeedUserNotFoundException if either user not found
     */
    public void follow(String followerId, String followeeId) throws FeedUserNotFoundException {
        // TODO: Implement
        // HINT: FeedUser follower = users.get(followerId);
        // HINT: FeedUser followee = users.get(followeeId);
        // HINT: if (follower == null) throw new FeedUserNotFoundException(followerId);
        // HINT: if (followee == null) throw new FeedUserNotFoundException(followeeId);
        // HINT: follower.following.add(followeeId);
        // HINT: followee.followers.add(followerId);
        // HINT: feedCache.invalidate(followerId);
    }
    
    /**
     * Add interest/topic to user's profile (for personalized ranking)
     * 
     * @param userId User ID
     * @param interest Interest/topic string
     * @throws FeedUserNotFoundException if user not found
     */
    public void addInterest(String userId, String interest) throws FeedUserNotFoundException {
        // TODO: Implement
        // HINT: FeedUser user = users.get(userId);
        // HINT: if (user == null) throw new FeedUserNotFoundException(userId);
        // HINT: user.interests.add(interest);
    }
    
    /**
     * Publish new content
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate author exists
     * 2. Generate unique content ID
     * 3. Create FeedContent object
     * 4. Store in allContent map
     * 5. Add content ID to author's contentIds list
     * 6. Trigger observer: invalidate followers' caches
     * 7. Return the content
     * 
     * @param authorId Author's user ID
     * @param text Content text
     * @param type Content type
     * @return The created content
     * @throws FeedUserNotFoundException if author not found
     */
    public FeedContent publishContent(String authorId, String text, ContentType type) 
            throws FeedUserNotFoundException {
        // TODO: Implement
        // HINT: FeedUser author = users.get(authorId);
        // HINT: if (author == null) throw new FeedUserNotFoundException(authorId);
        // HINT: String contentId = "CONTENT-" + (++contentIdCounter);
        // HINT: FeedContent content = new FeedContent(contentId, authorId, text, type);
        // HINT: allContent.put(contentId, content);
        // HINT: author.contentIds.add(contentId);
        // HINT: onNewContent(content);  // Trigger observer
        // HINT: return content;
        return null;
    }
    
    /**
     * Add engagement to content (like, comment, share)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Find content by ID
     * 2. Increment the appropriate counter
     * 3. Optionally invalidate caches for users who follow this author
     * 
     * @param contentId Content to engage with
     * @param addLikes Likes to add
     * @param addComments Comments to add
     * @param addShares Shares to add
     * @throws ContentNotFoundException if content not found
     */
    public void addEngagement(String contentId, int addLikes, int addComments, int addShares) 
            throws ContentNotFoundException {
        // TODO: Implement
        // HINT: FeedContent content = allContent.get(contentId);
        // HINT: if (content == null) throw new ContentNotFoundException(contentId);
        // HINT: content.likes += addLikes;
        // HINT: content.comments += addComments;
        // HINT: content.shares += addShares;
    }
    
    /**
     * Generate feed for a user (with caching)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate user exists
     * 2. Check feed cache first - return if cached
     * 3. If cache miss: aggregate content from all followed users
     * 4. Include user's own content
     * 5. Optionally mix in ads/recommendations
     * 6. Apply ranking strategy to sort content
     * 7. Cache the ranked feed
     * 8. Return the full ranked feed
     * 
     * INTERVIEW DISCUSSION:
     * - Fan-out on write: Pre-compute feed when content published (good for users with few followers)
     * - Fan-out on read: Compute at request time (good for celebrity users with millions of followers)
     * - Hybrid: Use write for normal users, read for celebrities
     * 
     * @param userId User to generate feed for
     * @return Ranked list of feed content
     * @throws FeedUserNotFoundException if user not found
     * @throws FeedGenerationException if feed generation fails
     */
    public List<FeedContent> generateFeed(String userId) 
            throws FeedUserNotFoundException, FeedGenerationException {
        // TODO: Implement
        // HINT: FeedUser user = users.get(userId);
        // HINT: if (user == null) throw new FeedUserNotFoundException(userId);
        // 
        // HINT: // Check cache
        // HINT: List<FeedContent> cached = feedCache.get(userId);
        // HINT: if (cached != null) return cached;
        // 
        // HINT: // Aggregate content from followed users
        // HINT: List<FeedContent> feedContent = new ArrayList<>();
        // HINT: for (String followedId : user.following) {
        //     FeedUser followed = users.get(followedId);
        //     if (followed != null) {
        //         for (String contentId : followed.contentIds) {
        //             FeedContent content = allContent.get(contentId);
        //             if (content != null) feedContent.add(content);
        //         }
        //     }
        // }
        // 
        // HINT: // Also include own content
        // HINT: for (String contentId : user.contentIds) {
        //     FeedContent c = allContent.get(contentId);
        //     if (c != null) feedContent.add(c);
        // }
        // 
        // HINT: // Rank the content
        // HINT: List<FeedContent> ranked = rankingStrategy.rank(feedContent, user);
        // 
        // HINT: // Cache the result
        // HINT: feedCache.put(userId, ranked);
        // HINT: return ranked;
        return new ArrayList<>();
    }
    
    /**
     * Get paginated feed
     * 
     * IMPLEMENTATION HINTS:
     * 1. Generate full feed (or use cache)
     * 2. Calculate start index: page * pageSize
     * 3. Calculate end index: min(start + pageSize, feed.size())
     * 4. Return sublist
     * 5. Handle out-of-bounds (return empty list)
     * 
     * @param userId User ID
     * @param page Page number (0-indexed)
     * @param pageSize Items per page
     * @return Paginated feed content
     * @throws FeedUserNotFoundException if user not found
     * @throws FeedGenerationException if feed generation fails
     */
    public List<FeedContent> getFeedPage(String userId, int page, int pageSize) 
            throws FeedUserNotFoundException, FeedGenerationException {
        // TODO: Implement
        // HINT: List<FeedContent> fullFeed = generateFeed(userId);
        // HINT: int start = page * pageSize;
        // HINT: if (start >= fullFeed.size()) return new ArrayList<>();
        // HINT: int end = Math.min(start + pageSize, fullFeed.size());
        // HINT: return fullFeed.subList(start, end);
        return new ArrayList<>();
    }
    
    /**
     * Change ranking strategy at runtime (for A/B testing)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Set new strategy
     * 2. Invalidate ALL caches (rankings will be different)
     * 
     * @param strategy New ranking strategy
     */
    public void setRankingStrategy(FeedRankingStrategy strategy) {
        // TODO: Implement
        // HINT: this.rankingStrategy = strategy;
        // HINT: feedCache = new FeedCache(60); // Reset cache
        // HINT: System.out.println("Ranking strategy changed to: " + strategy.getName());
    }
    
    /**
     * Observer callback - called when new content is published
     * Invalidates feed cache for all followers of the content author
     * 
     * IMPLEMENTATION HINTS:
     * 1. Find the content author
     * 2. Get all followers of the author
     * 3. Invalidate cache for each follower
     * 
     * @param content Newly published content
     */
    @Override
    public void onNewContent(FeedContent content) {
        // TODO: Implement
        // HINT: FeedUser author = users.get(content.authorId);
        // HINT: if (author != null) {
        //     feedCache.invalidateAll(author.followers);
        // }
    }
    
    /**
     * Get content by ID
     * 
     * @param contentId Content ID
     * @return FeedContent
     * @throws ContentNotFoundException if not found
     */
    public FeedContent getContent(String contentId) throws ContentNotFoundException {
        // TODO: Implement
        // HINT: FeedContent content = allContent.get(contentId);
        // HINT: if (content == null) throw new ContentNotFoundException(contentId);
        // HINT: return content;
        return null;
    }
    
    /**
     * Get user by ID
     * 
     * @param userId User ID
     * @return FeedUser
     * @throws FeedUserNotFoundException if not found
     */
    public FeedUser getUser(String userId) throws FeedUserNotFoundException {
        // TODO: Implement
        // HINT: FeedUser user = users.get(userId);
        // HINT: if (user == null) throw new FeedUserNotFoundException(userId);
        // HINT: return user;
        return null;
    }
    
    // Getters for testing
    public FeedCache getCache() { return feedCache; }
    public FeedRankingStrategy getCurrentStrategy() { return rankingStrategy; }
    public int getTotalContent() { return allContent.size(); }
    public int getTotalUsers() { return users.size(); }
}

// ===== MAIN TEST CLASS =====

public class FeedGenerator {
    public static void main(String[] args) {
        System.out.println("=== Feed Generator System Test Cases ===\n");
        
        // Create system with chronological ranking, 60s cache TTL
        FeedGeneratorService generator = new FeedGeneratorService(new ChronologicalRanking(), 60);
        
        // Test Case 1: Setup Users
        System.out.println("=== Test Case 1: Add Users ===");
        try {
            generator.addUser("alice", "Alice");
            generator.addUser("bob", "Bob");
            generator.addUser("charlie", "Charlie");
            generator.addUser("diana", "Diana");
            System.out.println("✓ Added 4 users");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 2: Follow Users
        System.out.println("=== Test Case 2: Follow Users ===");
        try {
            generator.follow("alice", "bob");
            generator.follow("alice", "charlie");
            generator.follow("alice", "diana");
            generator.follow("bob", "alice");
            generator.follow("charlie", "alice");
            System.out.println("✓ Alice follows Bob, Charlie, Diana");
            System.out.println("✓ Bob and Charlie follow Alice");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 3: Publish Content
        System.out.println("=== Test Case 3: Publish Content ===");
        try {
            FeedContent c1 = generator.publishContent("bob", "Bob's morning coffee ☕", ContentType.POST);
            FeedContent c2 = generator.publishContent("charlie", "Charlie's workout 💪", ContentType.POST);
            FeedContent c3 = generator.publishContent("diana", "Diana shared an article 📰", ContentType.SHARED_POST);
            FeedContent c4 = generator.publishContent("alice", "Alice's weekend trip 🌴", ContentType.POST);
            FeedContent c5 = generator.publishContent("bob", "Bob's lunch review 🍕", ContentType.POST);
            
            // Add engagement to some posts
            generator.addEngagement(c1.contentId, 50, 10, 5);   // Popular
            generator.addEngagement(c2.contentId, 100, 30, 20); // Very popular
            generator.addEngagement(c3.contentId, 5, 1, 0);     // Low engagement
            generator.addEngagement(c5.contentId, 200, 50, 30); // Most popular
            
            System.out.println("✓ Published 5 posts with varying engagement");
            System.out.println("  Total content: " + generator.getTotalContent());
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 4: Generate Chronological Feed
        System.out.println("=== Test Case 4: Chronological Feed for Alice ===");
        try {
            List<FeedContent> feed = generator.generateFeed("alice");
            System.out.println("Feed size: " + feed.size());
            for (int i = 0; i < feed.size(); i++) {
                System.out.println("  " + (i+1) + ". " + feed.get(i));
            }
            System.out.println("✓ Chronological: newest first");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 5: Engagement-Based Ranking
        System.out.println("=== Test Case 5: Engagement-Based Ranking ===");
        try {
            generator.setRankingStrategy(new EngagementRanking());
            List<FeedContent> feed = generator.generateFeed("alice");
            System.out.println("Feed ranked by engagement:");
            for (int i = 0; i < feed.size(); i++) {
                System.out.println("  " + (i+1) + ". " + feed.get(i));
            }
            System.out.println("✓ Most engaging content first");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 6: Personalized Ranking
        System.out.println("=== Test Case 6: Personalized Ranking ===");
        try {
            generator.addInterest("alice", "bob");  // Alice is interested in Bob's content
            generator.setRankingStrategy(new PersonalizedRanking());
            List<FeedContent> feed = generator.generateFeed("alice");
            System.out.println("Personalized feed for Alice:");
            for (int i = 0; i < feed.size(); i++) {
                System.out.println("  " + (i+1) + ". " + feed.get(i));
            }
            System.out.println("✓ Bob's content boosted by interest");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 7: Feed Caching
        System.out.println("=== Test Case 7: Feed Caching ===");
        try {
            boolean cachedBefore = generator.getCache().isCached("alice");
            List<FeedContent> feed1 = generator.generateFeed("alice");
            boolean cachedAfter = generator.getCache().isCached("alice");
            List<FeedContent> feed2 = generator.generateFeed("alice");  // Should hit cache
            
            System.out.println("✓ Cached before generate: " + cachedBefore);
            System.out.println("✓ Cached after generate: " + cachedAfter);
            System.out.println("✓ Same result from cache: " + (feed1.size() == feed2.size()));
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 8: Cache Invalidation on New Content
        System.out.println("=== Test Case 8: Cache Invalidation ===");
        try {
            // Alice's feed is cached from TC7
            boolean cachedBefore = generator.getCache().isCached("alice");
            
            // Bob publishes new content → should invalidate Alice's cache
            generator.publishContent("bob", "Breaking: Bob's big announcement! 🎉", ContentType.POST);
            
            boolean cachedAfter = generator.getCache().isCached("alice");
            System.out.println("✓ Alice cached before Bob's post: " + cachedBefore);
            System.out.println("✓ Alice cached after Bob's post: " + cachedAfter + " (invalidated!)");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 9: Pagination
        System.out.println("=== Test Case 9: Pagination ===");
        try {
            generator.setRankingStrategy(new ChronologicalRanking());
            
            List<FeedContent> page0 = generator.getFeedPage("alice", 0, 2);
            List<FeedContent> page1 = generator.getFeedPage("alice", 1, 2);
            List<FeedContent> page99 = generator.getFeedPage("alice", 99, 2);
            
            System.out.println("Page 0 (size 2): " + page0.size() + " items");
            page0.forEach(c -> System.out.println("  " + c));
            System.out.println("Page 1 (size 2): " + page1.size() + " items");
            page1.forEach(c -> System.out.println("  " + c));
            System.out.println("Page 99 (out of bounds): " + page99.size() + " items");
            System.out.println("✓ Pagination working");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 10: Bob's Feed (different perspective)
        System.out.println("=== Test Case 10: Bob's Feed ===");
        try {
            List<FeedContent> bobFeed = generator.generateFeed("bob");
            System.out.println("Bob's feed (" + bobFeed.size() + " items):");
            bobFeed.forEach(c -> System.out.println("  " + c));
            System.out.println("✓ Bob only sees Alice's content (he follows Alice)");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // ===== EXCEPTION TEST CASES =====
        
        // Test Case 11: Exception - User Not Found
        System.out.println("=== Test Case 11: Exception - User Not Found ===");
        try {
            generator.generateFeed("nonexistent");
            System.out.println("✗ Should have thrown FeedUserNotFoundException");
        } catch (FeedUserNotFoundException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 12: Exception - Content Not Found
        System.out.println("=== Test Case 12: Exception - Content Not Found ===");
        try {
            generator.addEngagement("FAKE-ID", 10, 0, 0);
            System.out.println("✗ Should have thrown ContentNotFoundException");
        } catch (ContentNotFoundException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 13: Exception - Follow Non-existent User
        System.out.println("=== Test Case 13: Exception - Follow Non-existent ===");
        try {
            generator.follow("alice", "ghost_user");
            System.out.println("✗ Should have thrown FeedUserNotFoundException");
        } catch (FeedUserNotFoundException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Summary
        System.out.println("=== Summary ===");
        System.out.println("Total users: " + generator.getTotalUsers());
        System.out.println("Total content: " + generator.getTotalContent());
        System.out.println("Current strategy: " + generator.getCurrentStrategy().getName());
        System.out.println("Cache entries: " + generator.getCache().size());
        
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION TOPICS:
 * ============================
 * 
 * 1. FEED GENERATION MODELS:
 *    Fan-out on Write (Push):
 *      - When user publishes, push to all followers' feeds
 *      - Pre-computed feeds = fast reads
 *      - Expensive for celebrities (millions of followers)
 *      - Twitter uses this for normal users
 *    
 *    Fan-out on Read (Pull):
 *      - Compute feed at request time
 *      - Always fresh, no storage overhead
 *      - Slow for users following many people
 *      - Twitter uses this for celebrities
 *    
 *    Hybrid (Best of Both):
 *      - Push for normal users, pull for celebrities
 *      - Most production systems use this
 *      - Facebook, Instagram, Twitter all use hybrid
 * 
 * 2. RANKING ALGORITHMS:
 *    Simple:
 *      - Chronological (timestamp sort)
 *      - Engagement (likes + comments + shares)
 *    
 *    Advanced:
 *      - EdgeRank (Facebook): Affinity × Weight × Decay
 *      - Personalized: ML model with features
 *      - Content diversity: Avoid too much from one source
 *      - Novelty: Boost content user hasn't seen
 *    
 *    Features for ML Ranking:
 *      - User-content affinity (past interactions)
 *      - Content engagement rate
 *      - Content freshness
 *      - User activity patterns
 *      - Social graph proximity
 * 
 * 3. CACHING STRATEGY:
 *    What to Cache:
 *      - Pre-computed feeds per user
 *      - Content metadata (engagement counts)
 *      - Social graph (following/followers)
 *    
 *    Invalidation:
 *      - TTL-based (expire after N minutes)
 *      - Event-based (new post → invalidate followers)
 *      - Hybrid (TTL + event-based)
 *    
 *    Technology:
 *      - Redis for feed cache
 *      - Memcached for content metadata
 *      - CDN for media content
 * 
 * 4. PAGINATION APPROACHES:
 *    Offset-based:
 *      - page=0&size=20
 *      - Simple but inconsistent with real-time data
 *    
 *    Cursor-based:
 *      - after=<last_seen_id>&size=20
 *      - Consistent, no duplicates/gaps
 *      - Used by Twitter, Facebook APIs
 *    
 *    Infinite Scroll:
 *      - Client requests next page on scroll
 *      - Pre-fetch next page for smooth UX
 * 
 * 5. CONTENT MIXING:
 *    Organic Content: Posts from followed users
 *    Ads: Inserted at fixed intervals (every 5th item)
 *    Recommendations: Suggested from non-followed sources
 *    Stories: Ephemeral content (top of feed)
 *    
 *    Mix Ratio:
 *      - 70% organic, 15% recommendations, 15% ads
 *      - Vary by user engagement level
 * 
 * 6. SCALABILITY:
 *    Storage:
 *      - Posts in Cassandra/DynamoDB (write-heavy)
 *      - Social graph in Neo4j/Redis
 *      - Feed cache in Redis
 *    
 *    Processing:
 *      - Kafka for event streaming (new posts)
 *      - Spark/Flink for real-time ranking
 *      - Worker pools for feed generation
 *    
 *    Serving:
 *      - CDN for media content
 *      - GraphQL API for flexible queries
 *      - WebSocket for real-time updates
 * 
 * 7. DESIGN PATTERNS:
 *    Strategy Pattern: Pluggable ranking algorithms
 *    Observer Pattern: Cache invalidation on new content
 *    Factory Pattern: Create different content types
 *    Iterator Pattern: Paginated feed traversal
 *    Decorator Pattern: Add ads/recommendations to base feed
 *    Singleton: Global feed service instance
 * 
 * 8. A/B TESTING:
 *    - Run multiple ranking algorithms simultaneously
 *    - Assign users to experiment groups
 *    - Measure: engagement rate, time spent, satisfaction
 *    - Gradually roll out winning algorithm
 * 
 * 9. REAL-WORLD METRICS:
 *    - Feed generation latency (P50, P99)
 *    - Cache hit ratio
 *    - Content freshness (avg age of feed items)
 *    - User engagement with feed
 *    - Feed diversity score
 * 
 * 10. API DESIGN:
 *     GET  /feed?userId={id}&page={n}&size={s}  - Get feed page
 *     GET  /feed?userId={id}&after={cursor}      - Cursor-based pagination
 *     POST /content                               - Publish content
 *     POST /content/{id}/like                     - Like content
 *     POST /users/{id}/follow/{targetId}          - Follow user
 *     GET  /users/{id}/feed/refresh               - Force cache refresh
 *     PUT  /feed/strategy                          - Change ranking (admin)
 */
