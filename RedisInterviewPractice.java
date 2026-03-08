/**
 * RedisInterviewPractice.java
 * 
 * Real-World & Interview-Focused Redis Use Cases
 * Based on High-Level Design (HLD) Patterns from FAANG/Big Tech Companies
 * 
 * This file focuses on practical Redis patterns commonly used in system design interviews:
 * - Caching strategies, Rate limiting, Session management
 * - Leaderboards, Real-time analytics, Distributed locks
 * - Pub/Sub messaging, Geospatial queries
 * 
 * QUICK START: ./run-redis-interview.sh
 * 
 * Instructions:
 * 1. Read each use case description
 * 2. Implement the solution using Redis/Jedis
 * 3. Run tests to verify
 */

import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;
import java.util.*;
import java.util.stream.Collectors;

public class RedisInterviewPractice {
    
    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;
    
    // ==================== CACHING PATTERNS ====================
    
    /**
     * INTERVIEW Q1: Implement Cache-Aside Pattern
     * Problem: Get user data from cache, if not found, fetch from DB and cache it
     * Companies: Amazon, Netflix, Spotify
     * Use Case: User profile caching
     * 
     * @param jedis Redis client
     * @param userId User ID
     * @param databaseFetcher Function to fetch from database
     * @param ttlSeconds TTL for cache
     * @return User data
     */
    public static String getCachedUserData(Jedis jedis, String userId, 
                                           java.util.function.Function<String, String> databaseFetcher,
                                           int ttlSeconds) {
        // TODO: Check cache first, if miss then fetch from DB and cache with TTL
        String cacheKey = "user:" + userId;
        String cached = jedis.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String data = databaseFetcher.apply(userId);
        if (data != null) {
            jedis.setex(cacheKey, ttlSeconds, data);
        }
        return data;
    }
    
    /**
     * INTERVIEW Q2: Implement Write-Through Cache
     * Problem: Update data in cache and database simultaneously
     * Companies: Facebook, Twitter
     * Use Case: Post/Tweet updates
     */
    public static void writeThroughCache(Jedis jedis, String key, String value,
                                         java.util.function.BiConsumer<String, String> databaseWriter) {
        // TODO: Write to cache and database together
        databaseWriter.accept(key, value);
        jedis.set(key, value);
    }
    
    /**
     * INTERVIEW Q3: Cache Invalidation
     * Problem: Invalidate cache when data changes
     * Companies: LinkedIn, Airbnb
     */
    public static void invalidateCache(Jedis jedis, String pattern) {
        // TODO: Delete all keys matching pattern (use SCAN for production)
        Set<String> keys = jedis.keys(pattern);
        if (!keys.isEmpty()) {
            jedis.del(keys.toArray(new String[0]));
        }
    }
    
    // ==================== RATE LIMITING ====================
    
    /**
     * INTERVIEW Q4: Fixed Window Rate Limiter
     * Problem: Allow max N requests per time window
     * Companies: Stripe, Twilio, SendGrid
     * Use Case: API rate limiting
     * 
     * @param jedis Redis client
     * @param userId User ID
     * @param maxRequests Max requests allowed
     * @param windowSeconds Time window in seconds
     * @return true if request allowed
     */
    public static boolean fixedWindowRateLimiter(Jedis jedis, String userId, int maxRequests, int windowSeconds) {
        // TODO: Use INCR with EXPIRE, check if count <= maxRequests
        String key = "rate:" + userId;
        Long count = jedis.incr(key);
        if (count == 1) {
            jedis.expire(key, windowSeconds);
        }
        return count <= maxRequests;
    }
    
    /**
     * INTERVIEW Q5: Sliding Window Rate Limiter
     * Problem: More accurate rate limiting using sorted sets
     * Companies: Uber, Lyft
     * Use Case: Ride request limiting
     */
    public static boolean slidingWindowRateLimiter(Jedis jedis, String userId, int maxRequests, long windowMs) {
        // TODO: Use ZSET with timestamps, remove old entries, count remaining
        String key = "rate:sliding:" + userId;
        long now = System.currentTimeMillis();
        long windowStart = now - windowMs;
        
        // Remove old entries
        jedis.zremrangeByScore(key, 0, windowStart);
        
        // Count current requests
        long count = jedis.zcard(key);
        
        if (count < maxRequests) {
            jedis.zadd(key, now, String.valueOf(now));
            jedis.expire(key, (int)(windowMs / 1000) + 1);
            return true;
        }
        return false;
    }
    
    /**
     * INTERVIEW Q6: Token Bucket Rate Limiter
     * Problem: Allow bursts but limit average rate
     * Companies: AWS, CloudFlare
     */
    public static boolean tokenBucketRateLimiter(Jedis jedis, String userId, int capacity, double refillRate) {
        // TODO: Implement token bucket using hash (tokens, lastRefill)
        String key = "rate:bucket:" + userId;
        long now = System.currentTimeMillis();
        
        String tokensStr = jedis.hget(key, "tokens");
        String lastRefillStr = jedis.hget(key, "lastRefill");
        
        double tokens = tokensStr != null ? Double.parseDouble(tokensStr) : capacity;
        long lastRefill = lastRefillStr != null ? Long.parseLong(lastRefillStr) : now;
        
        // Refill tokens
        long elapsed = now - lastRefill;
        double refilled = (elapsed / 1000.0) * refillRate;
        tokens = Math.min(capacity, tokens + refilled);
        
        if (tokens >= 1) {
            tokens -= 1;
            Map<String, String> data = new HashMap<>();
            data.put("tokens", String.valueOf(tokens));
            data.put("lastRefill", String.valueOf(now));
            jedis.hset(key, data);
            return true;
        }
        return false;
    }
    
    // ==================== SESSION MANAGEMENT ====================
    
    /**
     * INTERVIEW Q7: Create User Session
     * Problem: Store session data with automatic expiration
     * Companies: Google, Microsoft, Auth0
     */
    public static void createSession(Jedis jedis, String sessionId, Map<String, String> sessionData, int ttlSeconds) {
        // TODO: Store session as hash with TTL
        String key = "session:" + sessionId;
        jedis.hset(key, sessionData);
        jedis.expire(key, ttlSeconds);
    }
    
    /**
     * INTERVIEW Q8: Get Session Data
     * Problem: Retrieve and refresh session TTL
     * Companies: Okta, Firebase
     */
    public static Map<String, String> getSession(Jedis jedis, String sessionId, int ttlSeconds) {
        // TODO: Get session and refresh TTL
        String key = "session:" + sessionId;
        Map<String, String> session = jedis.hgetAll(key);
        if (!session.isEmpty()) {
            jedis.expire(key, ttlSeconds);
        }
        return session;
    }
    
    /**
     * INTERVIEW Q9: Invalidate Session
     * Problem: Logout/session cleanup
     */
    public static void invalidateSession(Jedis jedis, String sessionId) {
        // TODO: Delete session
        jedis.del("session:" + sessionId);
    }
    
    // ==================== LEADERBOARD/RANKING ====================
    
    /**
     * INTERVIEW Q10: Add Score to Leaderboard
     * Problem: Track user scores in real-time
     * Companies: Gaming companies, Duolingo, Strava
     * Use Case: Game leaderboards, fitness tracking
     */
    public static void addScore(Jedis jedis, String leaderboardName, String userId, double score) {
        // TODO: Use ZADD to add/update score
        String key = "leaderboard:" + leaderboardName;
        jedis.zadd(key, score, userId);
    }
    
    /**
     * INTERVIEW Q11: Get Top N Players
     * Problem: Get leaderboard top ranks
     * Companies: Epic Games, Roblox
     */
    public static List<String> getTopN(Jedis jedis, String leaderboardName, int n) {
        // TODO: Use ZREVRANGE to get top N (highest scores)
        String key = "leaderboard:" + leaderboardName;
        return new ArrayList<>(jedis.zrevrange(key, 0, n - 1));
    }
    
    /**
     * INTERVIEW Q12: Get User Rank
     * Problem: Find user's position in leaderboard
     * Companies: Chess.com, Lichess
     */
    public static Long getUserRank(Jedis jedis, String leaderboardName, String userId) {
        // TODO: Use ZREVRANK to get rank (0-based, highest first)
        String key = "leaderboard:" + leaderboardName;
        Long rank = jedis.zrevrank(key, userId);
        return rank != null ? rank + 1 : null; // Convert to 1-based
    }
    
    /**
     * INTERVIEW Q13: Get Users in Score Range
     * Problem: Find all users with scores between min and max
     * Companies: LinkedIn (endorsements), Stack Overflow (reputation)
     */
    public static List<String> getUsersInScoreRange(Jedis jedis, String leaderboardName, double minScore, double maxScore) {
        // TODO: Use ZRANGEBYSCORE
        String key = "leaderboard:" + leaderboardName;
        return jedis.zrangeByScore(key, minScore, maxScore);
    }
    
    // ==================== REAL-TIME ANALYTICS ====================
    
    /**
     * INTERVIEW Q14: Track Page Views
     * Problem: Count page views per URL
     * Companies: Google Analytics, Mixpanel
     */
    public static long incrementPageViews(Jedis jedis, String url) {
        // TODO: Use HINCRBY on analytics hash
        return jedis.hincrBy("analytics:pageviews", url, 1);
    }
    
    /**
     * INTERVIEW Q15: Track Unique Visitors (HyperLogLog)
     * Problem: Count unique visitors with minimal memory
     * Companies: Twitter, Reddit
     * Use Case: Daily active users
     */
    public static void trackUniqueVisitor(Jedis jedis, String date, String userId) {
        // TODO: Use PFADD (HyperLogLog) for unique counting
        String key = "visitors:" + date;
        jedis.pfadd(key, userId);
    }
    
    /**
     * INTERVIEW Q16: Get Unique Visitor Count
     */
    public static long getUniqueVisitorCount(Jedis jedis, String date) {
        // TODO: Use PFCOUNT
        String key = "visitors:" + date;
        return jedis.pfcount(key);
    }
    
    /**
     * INTERVIEW Q17: Real-Time Counter with Window
     * Problem: Track events in last N seconds
     * Companies: DataDog, New Relic
     * Use Case: Requests per second monitoring
     */
    public static long trackEventInWindow(Jedis jedis, String eventType, int windowSeconds) {
        // TODO: Use LIST with LPUSH + LTRIM or ZSET with timestamp
        String key = "events:" + eventType;
        long now = System.currentTimeMillis();
        
        jedis.zadd(key, now, String.valueOf(now));
        long cutoff = now - (windowSeconds * 1000);
        jedis.zremrangeByScore(key, 0, cutoff);
        jedis.expire(key, windowSeconds + 1);
        
        return jedis.zcard(key);
    }
    
    // ==================== DISTRIBUTED SYSTEMS ====================
    
    /**
     * INTERVIEW Q18: Distributed Lock (Simple)
     * Problem: Implement distributed lock for coordinating multiple servers
     * Companies: Amazon, Uber, Airbnb
     * Use Case: Preventing duplicate processing, job scheduling
     */
    public static boolean acquireLock(Jedis jedis, String resource, String lockId, int ttlSeconds) {
        // TODO: Use SETNX with EXPIRE (or SET with NX and EX options)
        String key = "lock:" + resource;
        SetParams params = new SetParams().nx().ex(ttlSeconds);
        String result = jedis.set(key, lockId, params);
        return "OK".equals(result);
    }
    
    /**
     * INTERVIEW Q19: Release Distributed Lock
     */
    public static boolean releaseLock(Jedis jedis, String resource, String lockId) {
        // TODO: Check if lock owner matches, then delete (use Lua script for atomicity)
        String key = "lock:" + resource;
        String currentLock = jedis.get(key);
        if (lockId.equals(currentLock)) {
            jedis.del(key);
            return true;
        }
        return false;
    }
    
    /**
     * INTERVIEW Q20: Job Queue (Simple)
     * Problem: Implement job queue using lists
     * Companies: Sidekiq, Celery users
     * Use Case: Background job processing
     */
    public static void enqueueJob(Jedis jedis, String queueName, String jobData) {
        // TODO: Use RPUSH to add job to queue
        String key = "queue:" + queueName;
        jedis.rpush(key, jobData);
    }
    
    /**
     * INTERVIEW Q21: Dequeue Job
     */
    public static String dequeueJob(Jedis jedis, String queueName, int timeoutSeconds) {
        // TODO: Use BLPOP for blocking dequeue
        String key = "queue:" + queueName;
        List<String> result = jedis.blpop(timeoutSeconds, key);
        return result != null && result.size() > 1 ? result.get(1) : null;
    }
    
    // ==================== SOCIAL FEATURES ====================
    
    /**
     * INTERVIEW Q22: Follow/Unfollow System
     * Problem: Track followers and following relationships
     * Companies: Twitter, Instagram, TikTok
     */
    public static void followUser(Jedis jedis, String userId, String targetUserId) {
        // TODO: Add to both followers and following sets
        jedis.sadd("followers:" + targetUserId, userId);
        jedis.sadd("following:" + userId, targetUserId);
    }
    
    public static void unfollowUser(Jedis jedis, String userId, String targetUserId) {
        // TODO: Remove from both sets
        jedis.srem("followers:" + targetUserId, userId);
        jedis.srem("following:" + userId, targetUserId);
    }
    
    /**
     * INTERVIEW Q23: Get Follower Count
     */
    public static long getFollowerCount(Jedis jedis, String userId) {
        // TODO: Use SCARD
        return jedis.scard("followers:" + userId);
    }
    
    /**
     * INTERVIEW Q24: Find Common Followers
     * Problem: Find mutual followers between two users
     * Companies: LinkedIn (mutual connections)
     */
    public static Set<String> findCommonFollowers(Jedis jedis, String user1, String user2) {
        // TODO: Use SINTER on follower sets
        return jedis.sinter("followers:" + user1, "followers:" + user2);
    }
    
    // ==================== E-COMMERCE PATTERNS ====================
    
    /**
     * INTERVIEW Q25: Shopping Cart
     * Problem: Manage shopping cart items
     * Companies: Amazon, eBay, Shopify
     */
    public static void addToCart(Jedis jedis, String userId, String productId, int quantity) {
        // TODO: Use HASH to store cart items with quantities
        String key = "cart:" + userId;
        jedis.hset(key, productId, String.valueOf(quantity));
        jedis.expire(key, 86400); // 24 hour expiry
    }
    
    public static Map<String, String> getCart(Jedis jedis, String userId) {
        // TODO: Get all cart items
        return jedis.hgetAll("cart:" + userId);
    }
    
    public static void removeFromCart(Jedis jedis, String userId, String productId) {
        // TODO: Remove item from cart
        jedis.hdel("cart:" + userId, productId);
    }
    
    /**
     * INTERVIEW Q26: Inventory Management
     * Problem: Track available product inventory
     * Companies: Walmart, Target
     * Use Case: Prevent overselling
     */
    public static boolean reserveInventory(Jedis jedis, String productId, int quantity) {
        // TODO: Use DECRBY, check if result >= 0, rollback if negative
        String key = "inventory:" + productId;
        long remaining = jedis.decrBy(key, quantity);
        if (remaining < 0) {
            jedis.incrBy(key, quantity); // Rollback
            return false;
        }
        return true;
    }
    
    // ==================== RECOMMENDATION/TRENDING ====================
    
    /**
     * INTERVIEW Q27: Track Trending Topics
     * Problem: Implement trending hashtags/topics with decay
     * Companies: Twitter, Reddit, YouTube
     */
    public static void incrementTrendingScore(Jedis jedis, String topic, double boost) {
        // TODO: Use ZINCRBY on trending sorted set
        jedis.zincrby("trending:topics", boost, topic);
    }
    
    public static List<String> getTrendingTopics(Jedis jedis, int topN) {
        // TODO: Get top N trending topics
        return new ArrayList<>(jedis.zrevrange("trending:topics", 0, topN - 1));
    }
    
    /**
     * INTERVIEW Q28: Recently Viewed Items
     * Problem: Track last N viewed items per user
     * Companies: Netflix, Amazon, YouTube
     */
    public static void addRecentlyViewed(Jedis jedis, String userId, String itemId, int maxItems) {
        // TODO: Use LIST with LPUSH + LTRIM to keep only last N items
        String key = "recent:" + userId;
        jedis.lpush(key, itemId);
        jedis.ltrim(key, 0, maxItems - 1);
        jedis.expire(key, 86400 * 30); // 30 days
    }
    
    public static List<String> getRecentlyViewed(Jedis jedis, String userId) {
        // TODO: Get all recently viewed items
        return jedis.lrange("recent:" + userId, 0, -1);
    }
    
    // ==================== GEOSPATIAL ====================
    
    /**
     * INTERVIEW Q29: Add Location (Geo)
     * Problem: Track user/driver locations
     * Companies: Uber, Lyft, DoorDash
     */
    public static void addLocation(Jedis jedis, String entityType, String entityId, double longitude, double latitude) {
        // TODO: Use GEOADD
        String key = "geo:" + entityType;
        jedis.geoadd(key, longitude, latitude, entityId);
    }
    
    /**
     * INTERVIEW Q30: Find Nearby Entities
     * Problem: Find drivers/restaurants within radius
     */
    public static List<String> findNearby(Jedis jedis, String entityType, double longitude, double latitude, double radiusKm) {
        // TODO: Use GEORADIUS
        String key = "geo:" + entityType;
        return jedis.georadius(key, longitude, latitude, radiusKm, 
            redis.clients.jedis.args.GeoUnit.KM).stream()
            .map(gr -> gr.getMemberByString())
            .collect(Collectors.toList());
    }
    
    // ==================== MESSAGING/NOTIFICATIONS ====================
    
    /**
     * INTERVIEW Q31: Publish Message (Pub/Sub)
     * Problem: Real-time notifications
     * Companies: Slack, Discord, WhatsApp
     */
    public static void publishMessage(Jedis jedis, String channel, String message) {
        // TODO: Use PUBLISH
        jedis.publish(channel, message);
    }
    
    /**
     * INTERVIEW Q32: Message Queue with Priority
     * Problem: Process high-priority jobs first
     * Companies: Task queue systems
     */
    public static void addPriorityJob(Jedis jedis, String queueName, String jobId, double priority) {
        // TODO: Use ZADD with priority as score
        String key = "priority:queue:" + queueName;
        jedis.zadd(key, -priority, jobId); // Negative for highest first
    }
    
    public static String getHighestPriorityJob(Jedis jedis, String queueName) {
        // TODO: Use ZPOPMIN to get and remove highest priority
        String key = "priority:queue:" + queueName;
        List<String> result = jedis.zrange(key, 0, 0);
        if (!result.isEmpty()) {
            String job = result.get(0);
            jedis.zrem(key, job);
            return job;
        }
        return null;
    }
    
    // ==================== TIME SERIES/METRICS ====================
    
    /**
     * INTERVIEW Q33: Store Time Series Data
     * Problem: Track metrics over time
     * Companies: Prometheus, Grafana, DataDog
     */
    public static void recordMetric(Jedis jedis, String metricName, long timestamp, double value) {
        // TODO: Use ZADD with timestamp as score
        String key = "metrics:" + metricName;
        jedis.zadd(key, timestamp, String.valueOf(value));
        // Keep only last 7 days
        long cutoff = timestamp - (86400 * 7 * 1000);
        jedis.zremrangeByScore(key, 0, cutoff);
    }
    
    /**
     * INTERVIEW Q34: Get Metrics in Time Range
     */
    public static List<String> getMetricsInRange(Jedis jedis, String metricName, long startTs, long endTs) {
        // TODO: Use ZRANGEBYSCORE
        String key = "metrics:" + metricName;
        return jedis.zrangeByScore(key, startTs, endTs);
    }
    
    // ==================== ADVANCED PATTERNS ====================
    
    /**
     * INTERVIEW Q35: Bloom Filter (Check Membership)
     * Problem: Check if element exists with low memory
     * Companies: Medium, Quora (spam detection)
     * Use Case: Check if username taken, email exists
     * Note: Jedis doesn't have native bloom filter, use BF.ADD/BF.EXISTS with RedisBloom module
     * For this practice, we'll use SET as simplified version
     */
    public static void bloomFilterAdd(Jedis jedis, String filterName, String element) {
        // Simplified: Use SET (production would use RedisBloom module)
        jedis.sadd("bloom:" + filterName, element);
    }
    
    public static boolean bloomFilterCheck(Jedis jedis, String filterName, String element) {
        // Simplified: Use SISMEMBER
        return jedis.sismember("bloom:" + filterName, element);
    }
    
    // ==================== TEST CASES ====================
    
    public static void main(String[] args) {
        int totalTests = 0;
        int passedTests = 0;
        
        System.out.println("=".repeat(70));
        System.out.println("REDIS INTERVIEW PRACTICE - HLD USE CASES");
        System.out.println("Real-World Patterns from System Design Interviews");
        System.out.println("=".repeat(70));
        
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            jedis.ping();
            System.out.println("✓ Connected to Redis\n");
            jedis.flushDB();
            
            // Test 1: Cache-Aside Pattern
            System.out.println("=== CACHING PATTERNS ===\n");
            System.out.println("[Test 1] Cache-Aside Pattern");
            String userData = getCachedUserData(jedis, "123", 
                id -> "User data for " + id, 60);
            totalTests++;
            if (userData != null && userData.contains("User data")) {
                System.out.println("  ✓ PASS: Cache-aside working");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL");
            }
            
            // Test 4: Fixed Window Rate Limiter
            System.out.println("\n=== RATE LIMITING ===\n");
            System.out.println("[Test 4] Fixed Window Rate Limiter");
            boolean r1 = fixedWindowRateLimiter(jedis, "user1", 3, 10);
            boolean r2 = fixedWindowRateLimiter(jedis, "user1", 3, 10);
            boolean r3 = fixedWindowRateLimiter(jedis, "user1", 3, 10);
            boolean r4 = fixedWindowRateLimiter(jedis, "user1", 3, 10);
            totalTests++;
            if (r1 && r2 && r3 && !r4) {
                System.out.println("  ✓ PASS: Rate limiter blocks 4th request");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL");
            }
            
            // Test 5: Sliding Window Rate Limiter
            System.out.println("\n[Test 5] Sliding Window Rate Limiter");
            jedis.del("rate:sliding:user2");
            boolean s1 = slidingWindowRateLimiter(jedis, "user2", 3, 1000);
            boolean s2 = slidingWindowRateLimiter(jedis, "user2", 3, 1000);
            boolean s3 = slidingWindowRateLimiter(jedis, "user2", 3, 1000);
            boolean s4 = slidingWindowRateLimiter(jedis, "user2", 3, 1000);
            totalTests++;
            if (s1 && s2 && s3 && !s4) {
                System.out.println("  ✓ PASS: Sliding window blocks 4th request");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL");
            }
            
            // Test 7-9: Session Management
            System.out.println("\n=== SESSION MANAGEMENT ===\n");
            System.out.println("[Test 7-9] Session Management");
            Map<String, String> session = new HashMap<>();
            session.put("userId", "123");
            session.put("role", "admin");
            createSession(jedis, "sess123", session, 3600);
            Map<String, String> retrieved = getSession(jedis, "sess123", 3600);
            invalidateSession(jedis, "sess123");
            Map<String, String> afterInvalidate = getSession(jedis, "sess123", 3600);
            totalTests++;
            if (retrieved.size() == 2 && afterInvalidate.isEmpty()) {
                System.out.println("  ✓ PASS: Session create/get/invalidate working");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL");
            }
            
            // Test 10-13: Leaderboard
            System.out.println("\n=== LEADERBOARD/RANKING ===\n");
            System.out.println("[Test 10-13] Leaderboard Operations");
            addScore(jedis, "game1", "player1", 100);
            addScore(jedis, "game1", "player2", 150);
            addScore(jedis, "game1", "player3", 120);
            addScore(jedis, "game1", "player4", 90);
            List<String> top3 = getTopN(jedis, "game1", 3);
            Long rank = getUserRank(jedis, "game1", "player3");
            List<String> range = getUsersInScoreRange(jedis, "game1", 100, 130);
            totalTests++;
            if (top3.get(0).equals("player2") && rank == 3 && range.size() == 2) {
                System.out.println("  ✓ PASS: Leaderboard operations working");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL");
            }
            
            // Test 14: Page Views
            System.out.println("\n=== REAL-TIME ANALYTICS ===\n");
            System.out.println("[Test 14] Page View Tracking");
            incrementPageViews(jedis, "/home");
            incrementPageViews(jedis, "/home");
            long views = incrementPageViews(jedis, "/home");
            totalTests++;
            if (views == 3) {
                System.out.println("  ✓ PASS: Page views = 3");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL");
            }
            
            // Test 15-16: Unique Visitors
            System.out.println("\n[Test 15-16] Unique Visitor Tracking");
            trackUniqueVisitor(jedis, "2024-01-01", "user1");
            trackUniqueVisitor(jedis, "2024-01-01", "user2");
            trackUniqueVisitor(jedis, "2024-01-01", "user1"); // Duplicate
            long unique = getUniqueVisitorCount(jedis, "2024-01-01");
            totalTests++;
            if (unique == 2) {
                System.out.println("  ✓ PASS: Unique visitors = 2");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Expected 2, got " + unique);
            }
            
            // Test 18-19: Distributed Lock
            System.out.println("\n=== DISTRIBUTED SYSTEMS ===\n");
            System.out.println("[Test 18-19] Distributed Lock");
            boolean acquired = acquireLock(jedis, "resource1", "lock123", 10);
            boolean acquiredAgain = acquireLock(jedis, "resource1", "lock456", 10);
            boolean released = releaseLock(jedis, "resource1", "lock123");
            boolean acquiredAfterRelease = acquireLock(jedis, "resource1", "lock789", 10);
            totalTests++;
            if (acquired && !acquiredAgain && released && acquiredAfterRelease) {
                System.out.println("  ✓ PASS: Distributed lock working");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL");
            }
            
            // Test 20-21: Job Queue
            System.out.println("\n[Test 20-21] Job Queue");
            enqueueJob(jedis, "emails", "job1");
            enqueueJob(jedis, "emails", "job2");
            String job1 = dequeueJob(jedis, "emails", 1);
            String job2 = dequeueJob(jedis, "emails", 1);
            totalTests++;
            if ("job1".equals(job1) && "job2".equals(job2)) {
                System.out.println("  ✓ PASS: Job queue FIFO working");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL");
            }
            
            // Test 22-24: Social Features
            System.out.println("\n=== SOCIAL FEATURES ===\n");
            System.out.println("[Test 22-24] Follow System");
            followUser(jedis, "alice", "bob");
            followUser(jedis, "charlie", "bob");
            followUser(jedis, "alice", "charlie");
            long bobFollowers = getFollowerCount(jedis, "bob");
            Set<String> common = findCommonFollowers(jedis, "bob", "charlie");
            totalTests++;
            if (bobFollowers == 2 && common.size() == 1) {
                System.out.println("  ✓ PASS: Follow system working");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL");
            }
            
            // Test 25: Shopping Cart
            System.out.println("\n=== E-COMMERCE ===\n");
            System.out.println("[Test 25] Shopping Cart");
            addToCart(jedis, "user1", "product1", 2);
            addToCart(jedis, "user1", "product2", 1);
            Map<String, String> cart = getCart(jedis, "user1");
            removeFromCart(jedis, "user1", "product1");
            Map<String, String> afterRemove = getCart(jedis, "user1");
            totalTests++;
            if (cart.size() == 2 && afterRemove.size() == 1) {
                System.out.println("  ✓ PASS: Shopping cart working");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL");
            }
            
            // Test 26: Inventory
            System.out.println("\n[Test 26] Inventory Management");
            jedis.set("inventory:prod1", "10");
            boolean res1 = reserveInventory(jedis, "prod1", 5);
            boolean res2 = reserveInventory(jedis, "prod1", 6);
            totalTests++;
            if (res1 && !res2) {
                System.out.println("  ✓ PASS: Inventory prevents overselling");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL");
            }
            
            // Test 27: Trending Topics
            System.out.println("\n=== RECOMMENDATION/TRENDING ===\n");
            System.out.println("[Test 27] Trending Topics");
            incrementTrendingScore(jedis, "#java", 10);
            incrementTrendingScore(jedis, "#python", 20);
            incrementTrendingScore(jedis, "#javascript", 15);
            List<String> trending = getTrendingTopics(jedis, 2);
            totalTests++;
            if (trending.get(0).equals("#python")) {
                System.out.println("  ✓ PASS: Trending topics sorted correctly");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL");
            }
            
            // Test 28: Recently Viewed
            System.out.println("\n[Test 28] Recently Viewed");
            addRecentlyViewed(jedis, "user1", "item1", 5);
            addRecentlyViewed(jedis, "user1", "item2", 5);
            addRecentlyViewed(jedis, "user1", "item3", 5);
            List<String> recent = getRecentlyViewed(jedis, "user1");
            totalTests++;
            if (recent.size() == 3 && recent.get(0).equals("item3")) {
                System.out.println("  ✓ PASS: Recently viewed (newest first)");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL");
            }
            
            // Test 29-30: Geospatial
            System.out.println("\n=== GEOSPATIAL ===\n");
            System.out.println("[Test 29-30] Location Tracking");
            addLocation(jedis, "drivers", "driver1", -122.4194, 37.7749); // SF
            addLocation(jedis, "drivers", "driver2", -122.4094, 37.7849); // Near SF
            List<String> nearby = findNearby(jedis, "drivers", -122.4194, 37.7749, 5);
            totalTests++;
            if (nearby.size() >= 1) {
                System.out.println("  ✓ PASS: Found " + nearby.size() + " nearby drivers");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL");
            }
            
            // Test 31: Pub/Sub
            System.out.println("\n=== MESSAGING ===\n");
            System.out.println("[Test 31] Publish Message");
            try {
                publishMessage(jedis, "notifications", "Hello");
                System.out.println("  ✓ PASS: Message published");
                totalTests++;
                passedTests++;
            } catch (Exception e) {
                System.out.println("  ✗ FAIL");
                totalTests++;
            }
            
            // Test 32: Priority Queue
            System.out.println("\n[Test 32] Priority Queue");
            addPriorityJob(jedis, "tasks", "low", 1);
            addPriorityJob(jedis, "tasks", "high", 10);
            addPriorityJob(jedis, "tasks", "medium", 5);
            String firstJob = getHighestPriorityJob(jedis, "tasks");
            totalTests++;
            if ("high".equals(firstJob)) {
                System.out.println("  ✓ PASS: Highest priority job retrieved");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Expected 'high', got: " + firstJob);
            }
            
            // Test 33-34: Time Series
            System.out.println("\n=== TIME SERIES ===\n");
            System.out.println("[Test 33-34] Metrics Tracking");
            long ts1 = System.currentTimeMillis();
            recordMetric(jedis, "cpu", ts1, 45.5);
            recordMetric(jedis, "cpu", ts1 + 1000, 55.0);
            List<String> metrics = getMetricsInRange(jedis, "cpu", ts1, ts1 + 2000);
            totalTests++;
            if (metrics.size() == 2) {
                System.out.println("  ✓ PASS: Time series tracking working");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL");
            }
            
            jedis.flushDB();
            
        } catch (Exception e) {
            System.out.println("\n✗ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Results
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST RESULTS");
        System.out.println("=".repeat(70));
        System.out.println("Total Tests: " + totalTests);
        System.out.println("Passed: " + passedTests);
        System.out.println("Failed: " + (totalTests - passedTests));
        System.out.println("Success Rate: " + String.format("%.2f", (passedTests * 100.0 / totalTests)) + "%");
        System.out.println("=".repeat(70));
        
        if (passedTests == totalTests) {
            System.out.println("\n🎉 CONGRATULATIONS! All HLD patterns working! 🎉");
            System.out.println("You're ready for system design interviews!");
        } else {
            System.out.println("\n⚠️  Review the failed use cases.");
        }
        
        System.out.println("\n📚 HLD Patterns Covered:");
        System.out.println("  ✓ Caching (Cache-aside, Write-through, Invalidation)");
        System.out.println("  ✓ Rate Limiting (Fixed window, Sliding window, Token bucket)");
        System.out.println("  ✓ Session Management (Create, Get, Invalidate)");
        System.out.println("  ✓ Leaderboards (Add score, Top N, Rank, Range queries)");
        System.out.println("  ✓ Analytics (Page views, Unique visitors, Time windows)");
        System.out.println("  ✓ Distributed Systems (Locks, Job queues)");
        System.out.println("  ✓ Social Features (Follow/Unfollow, Mutual followers)");
        System.out.println("  ✓ E-Commerce (Shopping cart, Inventory)");
        System.out.println("  ✓ Geospatial (Location tracking, Nearby search)");
        System.out.println("  ✓ Messaging (Pub/Sub, Priority queues)");
        System.out.println("  ✓ Time Series (Metrics storage and querying)");
    }
}
