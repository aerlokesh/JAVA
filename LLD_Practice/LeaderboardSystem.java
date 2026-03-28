import java.util.*;
import java.util.stream.Collectors;

// ===== ENUMS =====
enum LeaderboardType { GLOBAL, REGIONAL, FRIENDS }
enum TimePeriod { ALL_TIME, DAILY, WEEKLY, MONTHLY }

// ===== DOMAIN =====

class PlayerEntry implements Comparable<PlayerEntry> {
    String playerId, region;
    long score, lastUpdated;

    PlayerEntry(String playerId, String region) {
        this.playerId = playerId;
        this.region = region;
    }

    void addScore(long points) { this.score += points; this.lastUpdated = System.currentTimeMillis(); }

    /** Sort: highest score first. Tie-break: earlier update wins */
    @Override
    public int compareTo(PlayerEntry o) {
        if (o.score != this.score) return Long.compare(o.score, this.score);
        return Long.compare(this.lastUpdated, o.lastUpdated);
    }

}

/**
 * Single leaderboard backed by TreeSet (simulates Redis Sorted Set)
 * TreeSet gives O(log n) insert/remove, headSet for rank
 */
class Leaderboard {
    String name;
    TreeSet<PlayerEntry> ranked = new TreeSet<>();        // sorted by score desc
    Map<String, PlayerEntry> playerMap = new HashMap<>();  // playerId → entry

    Leaderboard(String name) { this.name = name; }

    /**
     * Update score: MUST remove from TreeSet BEFORE modifying score, then re-add.
     * TreeSet uses compareTo — changing score while inside breaks ordering!
     */
    void updateScore(String playerId, long score, String region) {
        // HINT: entry = playerMap.get(playerId)
        // if exists: ranked.remove(entry) → entry.addScore(score) → ranked.add(entry)
        // if new: create entry → addScore → put in playerMap + ranked
        if(playerMap.containsKey(playerId)){
            PlayerEntry entry=playerMap.get(playerId);
            ranked.remove(entry);
            entry.addScore(score);
            ranked.add(entry);
        }else{
            PlayerEntry entry=new PlayerEntry(playerId, region);
            entry.addScore(score);  // BUG FIX: was missing — score never set for new player!
            playerMap.put(playerId,entry);
            ranked.add(entry);
        }
    }

    /** Rank = headSet(entry).size() + 1  (In Redis: ZREVRANK) */
    int getRank(String playerId) {
        PlayerEntry entry = playerMap.get(playerId);
        if (entry == null) return -1;  // BUG FIX: was NPE if player not found
        return ranked.headSet(entry).size()+1;
    }

    /** Iterate TreeSet (already sorted), take first N  (In Redis: ZREVRANGE 0 N-1) */
    List<PlayerEntry> getTopN(int n) {
        // HINT: iterate ranked, collect first n entries
        return ranked.stream().limit(n).collect(Collectors.toList());
    }

    /** Get K players above and below a player */
    List<PlayerEntry> getNeighborhood(String playerId, int k) {
        // HINT: convert ranked to list, find idx, return subList [idx-k, idx+k+1]
        PlayerEntry entry=playerMap.get(playerId);
        List<PlayerEntry> list=new ArrayList<>(ranked);
        int idx=list.indexOf(entry);
        return list.subList(Math.max(0, idx-k), Math.min(list.size(), idx+k+1));  // BUG FIX: was idx+k, missed the player itself
    }

    /** percentile = (1 - rank/total) * 100 */
    double getPercentile(String playerId) {
        int rank = getRank(playerId);
        if (rank == -1 || ranked.isEmpty()) return 0;
        return (1.0 - (double) rank / ranked.size()) * 100;
    }

    void resetPlayer(String playerId) {
        PlayerEntry e = playerMap.remove(playerId);
        if (e != null) ranked.remove(e);
    }

    void resetAll() { ranked.clear(); playerMap.clear(); }
}

// ===== SERVICE =====

/**
 * Leaderboard Service — manages multiple leaderboards (global/regional/daily/weekly)
 * Each combo of (type, period, qualifier) is a separate Leaderboard (Redis Sorted Set).
 * submitScore fans out to all relevant boards.
 */
class LeaderboardService {
    Map<String, Leaderboard> boards = new HashMap<>();      // "global:all_time" → Leaderboard
    Map<String, Set<String>> friendsGraph = new HashMap<>(); // userId → friendIds

    Leaderboard getOrCreate(String key) { return boards.computeIfAbsent(key, Leaderboard::new); }

    String key(LeaderboardType t, TimePeriod p) { return t + ":" + p; }
    String key(LeaderboardType t, TimePeriod p, String q) { return t + ":" + p + ":" + q; }

    /** Submit score — fan-out to global (all periods) + regional (all periods) */
    void submitScore(String playerId, long score, String region) {
        // HINT: for each TimePeriod → update global board + regional board
        // for (TimePeriod p : TimePeriod.values()) {
        //     getOrCreate(key(GLOBAL, p)).updateScore(playerId, score, region);
        //     getOrCreate(key(REGIONAL, p, region)).updateScore(playerId, score, region);
        // }
        for(TimePeriod p:TimePeriod.values()){
            getOrCreate(key(LeaderboardType.GLOBAL, p)).updateScore(playerId, score, region);
            getOrCreate(key(LeaderboardType.REGIONAL, p, region)).updateScore(playerId, score, region);  // BUG FIX: was missing region qualifier — all regions lumped into one board!
        }
    }

    List<PlayerEntry> getTopN(LeaderboardType t, TimePeriod p, int n) {
        Leaderboard b = boards.get(key(t, p));
        return b == null ? Collections.emptyList() : b.getTopN(n);
    }

    List<PlayerEntry> getTopN(LeaderboardType t, TimePeriod p, String region, int n) {
        Leaderboard b = boards.get(key(t, p, region));
        return b == null ? Collections.emptyList() : b.getTopN(n);
    }

    int getRank(String playerId, LeaderboardType t, TimePeriod p) {
        Leaderboard b = boards.get(key(t, p));
        return b == null ? -1 : b.getRank(playerId);
    }

    List<PlayerEntry> getNeighborhood(String playerId, LeaderboardType t, TimePeriod p, int k) {
        Leaderboard b = boards.get(key(t, p));
        return b == null ? Collections.emptyList() : b.getNeighborhood(playerId, k);
    }

    double getPercentile(String playerId, LeaderboardType t, TimePeriod p) {
        Leaderboard b = boards.get(key(t, p));
        return b == null ? 0 : b.getPercentile(playerId);
    }

    /** Friends leaderboard: collect friends' entries from global board, sort, return top N */
    List<PlayerEntry> getFriendsTopN(String playerId, int n) {
        // HINT: get friends from friendsGraph
        // HINT: for each friend, get PlayerEntry from global:all_time board
        // HINT: include self, sort by compareTo, return top n
        Leaderboard globalBoard = boards.get(key(LeaderboardType.GLOBAL, TimePeriod.ALL_TIME));
        Set<String> friends = friendsGraph.getOrDefault(playerId, Collections.emptySet());
        List<PlayerEntry> result = new ArrayList<>();
        PlayerEntry selfEntry = globalBoard.playerMap.get(playerId);
        if (selfEntry != null) result.add(selfEntry);
        for (String friendId : friends) {
        PlayerEntry entry = globalBoard.playerMap.get(friendId);
        if (entry != null) {
                result.add(entry);
            }
        }
        result.sort(null);
        
        return result.stream()
            .limit(n)
            .collect(Collectors.toList());
    }

    void addFriend(String u1, String u2) {
        friendsGraph.computeIfAbsent(u1, k -> new HashSet<>()).add(u2);
        friendsGraph.computeIfAbsent(u2, k -> new HashSet<>()).add(u1);
    }

    void resetLeaderboard(LeaderboardType t, TimePeriod p) {
        Leaderboard b = boards.get(key(t, p));
        if (b != null) b.resetAll();
    }
}

// ===== MAIN =====

public class LeaderboardSystem {
    public static void main(String[] args) {
        System.out.println("=== Leaderboard System ===\n");
        LeaderboardService svc = new LeaderboardService();

        svc.addFriend("alice", "bob");
        svc.addFriend("alice", "charlie");

        System.out.println("--- Submit Scores ---");
        svc.submitScore("alice", 150, "US");
        svc.submitScore("bob", 200, "US");
        svc.submitScore("charlie", 180, "EU");
        svc.submitScore("diana", 220, "EU");
        svc.submitScore("eve", 170, "US");
        svc.submitScore("frank", 190, "AP");

        System.out.println("\n--- Global Top 3 (expect diana=220, bob=200, frank=190) ---");
        System.out.println(svc.getTopN(LeaderboardType.GLOBAL, TimePeriod.ALL_TIME, 3));

        System.out.println("\n--- Ranks ---");
        System.out.println("Alice rank: " + svc.getRank("alice", LeaderboardType.GLOBAL, TimePeriod.ALL_TIME) + " (expect 6)");
        System.out.println("Diana rank: " + svc.getRank("diana", LeaderboardType.GLOBAL, TimePeriod.ALL_TIME) + " (expect 1)");

        System.out.println("\n--- Regional US Top (expect bob=200, eve=170, alice=150) ---");
        System.out.println(svc.getTopN(LeaderboardType.REGIONAL, TimePeriod.ALL_TIME, "US", 5));

        System.out.println("\n--- Friends of Alice (expect bob=200, charlie=180, alice=150) ---");
        System.out.println(svc.getFriendsTopN("alice", 5));

        System.out.println("\n--- Neighborhood around eve (2 above + eve + 2 below) ---");
        System.out.println(svc.getNeighborhood("eve", LeaderboardType.GLOBAL, TimePeriod.ALL_TIME, 2));

        System.out.println("\n--- Percentile ---");
        System.out.printf("Bob percentile: %.1f%%\n", svc.getPercentile("bob", LeaderboardType.GLOBAL, TimePeriod.ALL_TIME));

        System.out.println("\n--- Score Update: alice +100 → should be rank 1 ---");
        svc.submitScore("alice", 100, "US");
        System.out.println("Alice rank: " + svc.getRank("alice", LeaderboardType.GLOBAL, TimePeriod.ALL_TIME));

        System.out.println("\n--- Daily Reset ---");
        svc.resetLeaderboard(LeaderboardType.GLOBAL, TimePeriod.DAILY);
        System.out.println("Daily after reset: " + svc.getTopN(LeaderboardType.GLOBAL, TimePeriod.DAILY, 3).size() + " (expect 0)");
        System.out.println("All-time intact: " + svc.getTopN(LeaderboardType.GLOBAL, TimePeriod.ALL_TIME, 1));

        System.out.println("\n=== Done ===");
    }
}

/**
 * KEY POINTS FOR INTERVIEW:
 * 
 * 1. DATA STRUCTURE: TreeSet (simulates Redis Sorted Set ZADD/ZREVRANK/ZREVRANGE)
 * 2. CRITICAL: Remove from TreeSet BEFORE changing score, then re-add
 * 3. MULTIPLE BOARDS: Separate sorted set per (type, period) — fan-out on write
 * 4. TIME RESET: Cron job clears daily/weekly boards; all-time never resets
 * 5. FRIENDS: Filter global board by friend list, or maintain per-user boards
 * 6. TIE-BREAK: Same score → earlier timestamp wins
 * 7. SCALE: Redis cluster sharded by board key, Kafka for async fan-out
 */
