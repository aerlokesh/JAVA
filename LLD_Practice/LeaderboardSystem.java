import java.util.*;

// ===== ENUMS =====

enum LeaderboardType { GLOBAL, REGIONAL, FRIENDS }

enum TimePeriod { ALL_TIME, DAILY, WEEKLY, MONTHLY }

// ===== DOMAIN CLASSES =====

class PlayerEntry implements Comparable<PlayerEntry> {
    private final String playerId;
    private final String region;
    private long score;
    private long lastUpdated;
    
    public PlayerEntry(String playerId, String region) {
        this.playerId = playerId;
        this.region = region;
        this.score = 0;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public String getPlayerId() { return playerId; }
    public String getRegion() { return region; }
    public long getScore() { return score; }
    public long getLastUpdated() { return lastUpdated; }
    
    public void addScore(long points) { this.score += points; this.lastUpdated = System.currentTimeMillis(); }
    public void setScore(long s) { this.score = s; this.lastUpdated = System.currentTimeMillis(); }
    
    /** Sort: highest score first. Tie-break: earlier update wins */
    @Override
    public int compareTo(PlayerEntry other) {
        if (other.score != this.score) return Long.compare(other.score, this.score);
        return Long.compare(this.lastUpdated, other.lastUpdated);
    }
    
    @Override
    public String toString() { return playerId + "(" + score + ", " + region + ")"; }
}

/**
 * A single leaderboard instance (backed by TreeSet for O(log n) rank queries)
 * 
 * In real system: Redis Sorted Set (ZADD, ZRANK, ZREVRANGE)
 * Here: TreeSet gives us sorted order + rank via headSet().size()
 */
class Leaderboard {
    private final String name;
    private final TreeSet<PlayerEntry> ranked;          // sorted by score desc
    private final Map<String, PlayerEntry> playerMap;   // playerId → entry (for O(1) lookup)
    
    public Leaderboard(String name) {
        this.name = name;
        this.ranked = new TreeSet<>();
        this.playerMap = new HashMap<>();
    }
    
    public String getName() { return name; }
    
    /**
     * Update score for a player
     * 
     * IMPLEMENTATION HINTS:
     * 1. If player exists in playerMap:
     *    a. Remove from TreeSet (must remove BEFORE modifying score!)
     *    b. Update score
     *    c. Re-add to TreeSet
     * 2. If new player:
     *    a. Create PlayerEntry
     *    b. Set score
     *    c. Add to both TreeSet and playerMap
     * 
     * KEY: Must remove-then-re-add because TreeSet uses compareTo for ordering.
     * If you change score while in TreeSet, ordering breaks!
     */
    public void updateScore(String playerId, long score, String region) {
        // TODO: Implement
        // HINT: PlayerEntry entry = playerMap.get(playerId);
        // HINT: if (entry != null) {
        //     ranked.remove(entry);   // remove with OLD score
        //     entry.addScore(score);
        //     ranked.add(entry);      // re-add with NEW score
        // } else {
        //     entry = new PlayerEntry(playerId, region);
        //     entry.addScore(score);
        //     playerMap.put(playerId, entry);
        //     ranked.add(entry);
        // }
    }
    
    /**
     * Get rank of a player (1-indexed)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get player entry from playerMap
     * 2. Use TreeSet.headSet(entry).size() + 1 for rank
     *    headSet returns all elements BEFORE this entry (higher scores)
     * 
     * In Redis: ZREVRANK key member → O(log n)
     */
    public int getRank(String playerId) {
        // TODO: Implement
        // HINT: PlayerEntry entry = playerMap.get(playerId);
        // HINT: if (entry == null) return -1;
        // HINT: return ranked.headSet(entry).size() + 1;
        return -1;
    }
    
    /**
     * Get top N players
     * 
     * IMPLEMENTATION HINTS:
     * 1. Iterate TreeSet (already sorted by score desc)
     * 2. Take first N entries
     * 
     * In Redis: ZREVRANGE key 0 N-1 → O(log n + N)
     */
    public List<PlayerEntry> getTopN(int n) {
        // TODO: Implement
        // HINT: List<PlayerEntry> result = new ArrayList<>();
        // HINT: int count = 0;
        // HINT: for (PlayerEntry e : ranked) {
        //     if (count >= n) break;
        //     result.add(e);
        //     count++;
        // }
        // HINT: return result;
        return null;
    }
    
    /**
     * Get players around a specific player (neighborhood)
     * Shows K players above and below
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get player's rank
     * 2. Convert TreeSet to list (or iterate)
     * 3. Return range [rank-k, rank+k]
     */
    public List<PlayerEntry> getNeighborhood(String playerId, int k) {
        // TODO: Implement
        // HINT: PlayerEntry entry = playerMap.get(playerId);
        // HINT: if (entry == null) return Collections.emptyList();
        // HINT: List<PlayerEntry> all = new ArrayList<>(ranked);
        // HINT: int idx = all.indexOf(entry);
        // HINT: int start = Math.max(0, idx - k);
        // HINT: int end = Math.min(all.size(), idx + k + 1);
        // HINT: return all.subList(start, end);
        return null;
    }
    
    /**
     * Get player's percentile ("top X%")
     * 
     * IMPLEMENTATION HINTS:
     * 1. percentile = (1 - rank / totalPlayers) * 100
     */
    public double getPercentile(String playerId) {
        // TODO: Implement
        // HINT: int rank = getRank(playerId);
        // HINT: if (rank == -1 || ranked.isEmpty()) return 0;
        // HINT: return (1.0 - (double) rank / ranked.size()) * 100;
        return 0;
    }
    
    /**
     * Reset player's score (remove from leaderboard)
     */
    public void resetPlayer(String playerId) {
        // TODO: Implement
        // HINT: PlayerEntry entry = playerMap.remove(playerId);
        // HINT: if (entry != null) ranked.remove(entry);
    }
    
    /**
     * Reset entire leaderboard (for daily/weekly reset)
     */
    public void resetAll() {
        // TODO: Implement
        // HINT: ranked.clear();
        // HINT: playerMap.clear();
    }
    
    public int size() { return ranked.size(); }
    public PlayerEntry getPlayer(String id) { return playerMap.get(id); }
}

// ===== SERVICE =====

/**
 * Leaderboard System - Low Level Design (LLD)
 * 
 * PROBLEM: Design a leaderboard system that supports:
 * 1. Multiple leaderboard types (global, regional, friends)
 * 2. Time-based periods (daily, weekly, monthly, all-time)
 * 3. Score updates, rank queries, top-N, neighborhood, percentile
 * 4. Reset for time-based leaderboards
 * 
 * KEY DATA STRUCTURE: TreeSet (simulates Redis Sorted Set)
 *   - updateScore: O(log n) — remove + add
 *   - getRank: O(log n) — headSet().size()
 *   - getTopN: O(N) — iterate first N
 *   In production: Redis ZADD/ZRANK/ZREVRANGE
 * 
 * ARCHITECTURE:
 *   Score Update → Leaderboard Service → Multiple Leaderboards (global/daily/regional)
 *   Each leaderboard is a separate TreeSet (or Redis Sorted Set)
 */
class LeaderboardService {
    private final Map<String, Leaderboard> leaderboards;    // "global:all_time" → Leaderboard
    private final Map<String, Set<String>> friendsGraph;    // userId → Set<friendIds>
    
    public LeaderboardService() {
        this.leaderboards = new HashMap<>();
        this.friendsGraph = new HashMap<>();
    }
    
    /**
     * Get or create a leaderboard by composite key
     */
    private Leaderboard getOrCreateBoard(String key) {
        return leaderboards.computeIfAbsent(key, Leaderboard::new);
    }
    
    private String boardKey(LeaderboardType type, TimePeriod period) {
        return type.name().toLowerCase() + ":" + period.name().toLowerCase();
    }
    
    private String boardKey(LeaderboardType type, TimePeriod period, String qualifier) {
        return type.name().toLowerCase() + ":" + period.name().toLowerCase() + ":" + qualifier;
    }
    
    /**
     * Submit a score — updates ALL relevant leaderboards
     * 
     * IMPLEMENTATION HINTS:
     * 1. Update global leaderboard (all time periods)
     * 2. Update regional leaderboard for player's region
     * 3. Update friends leaderboards for all friends
     * 
     * In real system: fan-out via message queue (Kafka)
     * Here: synchronous for simplicity
     */
    public void submitScore(String playerId, long score, String region) {
        // TODO: Implement
        // HINT: // Update global boards (all time periods)
        // HINT: for (TimePeriod period : TimePeriod.values()) {
        //     getOrCreateBoard(boardKey(LeaderboardType.GLOBAL, period))
        //         .updateScore(playerId, score, region);
        // }
        //
        // HINT: // Update regional boards
        // HINT: for (TimePeriod period : TimePeriod.values()) {
        //     getOrCreateBoard(boardKey(LeaderboardType.REGIONAL, period, region))
        //         .updateScore(playerId, score, region);
        // }
        //
        // HINT: System.out.println("  📊 " + playerId + " +" + score + " (region=" + region + ")");
    }
    
    /**
     * Get top N from a specific leaderboard
     */
    public List<PlayerEntry> getTopN(LeaderboardType type, TimePeriod period, int n) {
        // TODO: Implement
        // HINT: Leaderboard board = leaderboards.get(boardKey(type, period));
        // HINT: if (board == null) return Collections.emptyList();
        // HINT: return board.getTopN(n);
        return null;
    }
    
    /** Regional top N */
    public List<PlayerEntry> getTopN(LeaderboardType type, TimePeriod period, String region, int n) {
        // TODO: Implement
        // HINT: Leaderboard board = leaderboards.get(boardKey(type, period, region));
        // HINT: if (board == null) return Collections.emptyList();
        // HINT: return board.getTopN(n);
        return null;
    }
    
    /**
     * Get player's rank on a leaderboard
     */
    public int getRank(String playerId, LeaderboardType type, TimePeriod period) {
        // TODO: Implement
        // HINT: Leaderboard board = leaderboards.get(boardKey(type, period));
        // HINT: if (board == null) return -1;
        // HINT: return board.getRank(playerId);
        return -1;
    }
    
    /**
     * Get neighborhood (players around you)
     */
    public List<PlayerEntry> getNeighborhood(String playerId, LeaderboardType type, TimePeriod period, int k) {
        // TODO: Implement
        // HINT: Leaderboard board = leaderboards.get(boardKey(type, period));
        // HINT: if (board == null) return Collections.emptyList();
        // HINT: return board.getNeighborhood(playerId, k);
        return null;
    }
    
    /**
     * Get percentile
     */
    public double getPercentile(String playerId, LeaderboardType type, TimePeriod period) {
        // TODO: Implement
        // HINT: Leaderboard board = leaderboards.get(boardKey(type, period));
        // HINT: if (board == null) return 0;
        // HINT: return board.getPercentile(playerId);
        return 0;
    }
    
    /**
     * Friends leaderboard: get top N among friends
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get friend list for playerId
     * 2. Create temporary leaderboard with only friends' scores
     * 3. Return top N from that
     * Or: maintain per-user friends leaderboard (expensive but fast reads)
     */
    public List<PlayerEntry> getFriendsTopN(String playerId, int n) {
        // TODO: Implement
        // HINT: Set<String> friends = friendsGraph.getOrDefault(playerId, Collections.emptySet());
        // HINT: Leaderboard globalBoard = leaderboards.get(boardKey(LeaderboardType.GLOBAL, TimePeriod.ALL_TIME));
        // HINT: if (globalBoard == null) return Collections.emptyList();
        //
        // HINT: List<PlayerEntry> friendEntries = new ArrayList<>();
        // HINT: for (String fid : friends) {
        //     PlayerEntry e = globalBoard.getPlayer(fid);
        //     if (e != null) friendEntries.add(e);
        // }
        // HINT: // Include self
        // HINT: PlayerEntry self = globalBoard.getPlayer(playerId);
        // HINT: if (self != null) friendEntries.add(self);
        //
        // HINT: friendEntries.sort(PlayerEntry::compareTo);
        // HINT: return friendEntries.subList(0, Math.min(n, friendEntries.size()));
        return null;
    }
    
    /**
     * Add friend relationship (bidirectional)
     */
    public void addFriend(String userId1, String userId2) {
        // TODO: Implement
        // HINT: friendsGraph.computeIfAbsent(userId1, k -> new HashSet<>()).add(userId2);
        // HINT: friendsGraph.computeIfAbsent(userId2, k -> new HashSet<>()).add(userId1);
    }
    
    /**
     * Reset a time-based leaderboard (called by scheduler)
     */
    public void resetLeaderboard(LeaderboardType type, TimePeriod period) {
        // TODO: Implement
        // HINT: String key = boardKey(type, period);
        // HINT: Leaderboard board = leaderboards.get(key);
        // HINT: if (board != null) { board.resetAll(); System.out.println("  🔄 Reset: " + key); }
    }
}

// ===== MAIN TEST CLASS =====

public class LeaderboardSystem {
    public static void main(String[] args) {
        System.out.println("=== Leaderboard System LLD ===\n");
        
        LeaderboardService service = new LeaderboardService();
        
        // Setup friends
        service.addFriend("alice", "bob");
        service.addFriend("alice", "charlie");
        service.addFriend("bob", "diana");
        
        // Test 1: Submit scores
        System.out.println("=== Test 1: Submit Scores ===");
        service.submitScore("alice", 150, "US");
        service.submitScore("bob", 200, "US");
        service.submitScore("charlie", 180, "EU");
        service.submitScore("diana", 220, "EU");
        service.submitScore("eve", 170, "US");
        service.submitScore("frank", 190, "AP");
        System.out.println();
        
        // Test 2: Global Top N
        System.out.println("=== Test 2: Global Top 3 ===");
        List<PlayerEntry> top3 = service.getTopN(LeaderboardType.GLOBAL, TimePeriod.ALL_TIME, 3);
        System.out.println("✓ Top 3: " + (top3 != null ? top3 : "null"));
        System.out.println("  (expect diana=220, bob=200, frank=190)");
        System.out.println();
        
        // Test 3: Player rank
        System.out.println("=== Test 3: Player Rank ===");
        int aliceRank = service.getRank("alice", LeaderboardType.GLOBAL, TimePeriod.ALL_TIME);
        int dianaRank = service.getRank("diana", LeaderboardType.GLOBAL, TimePeriod.ALL_TIME);
        System.out.println("✓ Alice rank: " + aliceRank + " (expect 6)");
        System.out.println("✓ Diana rank: " + dianaRank + " (expect 1)");
        System.out.println();
        
        // Test 4: Regional leaderboard
        System.out.println("=== Test 4: Regional Top (US) ===");
        List<PlayerEntry> usTop = service.getTopN(LeaderboardType.REGIONAL, TimePeriod.ALL_TIME, "US", 5);
        System.out.println("✓ US Top: " + (usTop != null ? usTop : "null"));
        System.out.println("  (expect bob=200, eve=170, alice=150)");
        System.out.println();
        
        // Test 5: Friends leaderboard
        System.out.println("=== Test 5: Friends Leaderboard (Alice) ===");
        List<PlayerEntry> aliceFriends = service.getFriendsTopN("alice", 5);
        System.out.println("✓ Alice's friends: " + (aliceFriends != null ? aliceFriends : "null"));
        System.out.println("  (expect bob=200, charlie=180, alice=150)");
        System.out.println();
        
        // Test 6: Neighborhood (players around you)
        System.out.println("=== Test 6: Neighborhood ===");
        List<PlayerEntry> around = service.getNeighborhood("eve", LeaderboardType.GLOBAL, TimePeriod.ALL_TIME, 2);
        System.out.println("✓ Around eve: " + (around != null ? around : "null"));
        System.out.println("  (expect 2 above + eve + 2 below)");
        System.out.println();
        
        // Test 7: Percentile
        System.out.println("=== Test 7: Percentile ===");
        double pct = service.getPercentile("bob", LeaderboardType.GLOBAL, TimePeriod.ALL_TIME);
        System.out.println("✓ Bob's percentile: " + String.format("%.1f", pct) + "%");
        System.out.println();
        
        // Test 8: Score update (additive)
        System.out.println("=== Test 8: Score Update ===");
        service.submitScore("alice", 100, "US"); // alice: 150+100=250 → should be #1!
        int newRank = service.getRank("alice", LeaderboardType.GLOBAL, TimePeriod.ALL_TIME);
        System.out.println("✓ Alice after +100: rank=" + newRank + " (expect 1)");
        System.out.println();
        
        // Test 9: Daily leaderboard reset
        System.out.println("=== Test 9: Daily Reset ===");
        List<PlayerEntry> dailyBefore = service.getTopN(LeaderboardType.GLOBAL, TimePeriod.DAILY, 3);
        System.out.println("  Daily before reset: " + (dailyBefore != null ? dailyBefore.size() : 0));
        service.resetLeaderboard(LeaderboardType.GLOBAL, TimePeriod.DAILY);
        List<PlayerEntry> dailyAfter = service.getTopN(LeaderboardType.GLOBAL, TimePeriod.DAILY, 3);
        System.out.println("  Daily after reset: " + (dailyAfter != null ? dailyAfter.size() : 0) + " (expect 0)");
        System.out.println("  All-time still intact: " + service.getTopN(LeaderboardType.GLOBAL, TimePeriod.ALL_TIME, 1));
        
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION:
 * =====================
 * 
 * 1. KEY DATA STRUCTURE — Redis Sorted Set:
 *    ZADD leaderboard:global 200 "bob"   → O(log n) add/update
 *    ZREVRANK leaderboard:global "bob"    → O(log n) get rank
 *    ZREVRANGE leaderboard:global 0 9     → O(log n + K) top K
 *    ZSCORE leaderboard:global "bob"      → O(1) get score
 *    Here: TreeSet simulates this behavior
 * 
 * 2. MULTIPLE LEADERBOARDS:
 *    Separate sorted set per (type, period):
 *    global:all_time, global:daily, global:weekly
 *    regional:all_time:US, regional:daily:EU
 *    On submitScore → fan-out to all relevant boards
 * 
 * 3. TIME-BASED RESET:
 *    Cron job at midnight → reset daily board
 *    Monday 00:00 → reset weekly board
 *    All-time board never resets
 * 
 * 4. FRIENDS LEADERBOARD:
 *    Option A: filter global board by friend list (slow for large boards)
 *    Option B: maintain per-user friends board (fast reads, expensive writes)
 *    Option C: query Redis with friend IDs → ZSCORE for each → sort
 * 
 * 5. TIE-BREAKING:
 *    Same score → who achieved it first wins (earlier timestamp)
 *    compareTo: score desc, then timestamp asc
 * 
 * 6. SCALABILITY:
 *    Redis cluster: shard by leaderboard key
 *    Hot leaderboard (global): replicas for reads
 *    Fan-out on write: Kafka → workers update each board async
 * 
 * 7. REAL-WORLD: Gaming (Fortnite, LoL), Competitive coding (LeetCode), Fitness (Strava)
 * 
 * 8. API:
 *    POST /scores                                    — submit score
 *    GET  /leaderboard/global/all-time?top=100       — top N
 *    GET  /leaderboard/global/daily?top=100          — daily top N
 *    GET  /leaderboard/regional/US/weekly?top=50     — regional
 *    GET  /players/{id}/rank?board=global&period=all  — player rank
 *    GET  /players/{id}/neighborhood?k=5              — around me
 *    GET  /players/{id}/friends-leaderboard?top=10    — friends board
 *    GET  /players/{id}/percentile                    — top X%
 */
