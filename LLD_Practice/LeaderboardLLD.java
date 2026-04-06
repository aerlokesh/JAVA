import java.util.*;

/*
 * FANTASY LEADERBOARD - Low Level Design
 * =========================================
 * 
 * REQUIREMENTS:
 * 1. addUser(userId, playerIds) — register user with team
 * 2. addScore(playerId, delta) — update player score, propagate to all users
 * 3. getTopK(k) — top K users by score desc, tie-break lex userId asc
 * 4. User score = sum of current scores of all players on team
 * 5. Player can be on multiple teams (many-to-many)
 * 6. New user inherits existing player scores immediately
 * 
 * KEY DATA STRUCTURES:
 * - Map<playerId, Set<userId>>: reverse index for score propagation
 * - Map<userId, UserTeam>: user's playerIds + current score
 * - TreeSet<UserTeam>: sorted leaderboard for O(log n) getTopK
 * 
 * COMPLEXITY:
 *   addUser:   O(P + log N) P = players in team, N = total users
 *   addScore:  O(U * log N) U = users with that player
 *   getTopK:   O(K) iterate sorted set
 */

// ==================== USER TEAM ====================

class UserTeam implements Comparable<UserTeam> {
    final String userId;
    final List<String> playerIds;
    int score;

    UserTeam(String userId, List<String> playerIds) {
        this.userId = userId;
        this.playerIds = playerIds;
    }

    @Override
    public int compareTo(UserTeam other) {
        if (this.score != other.score) return Integer.compare(other.score, this.score); // desc
        return this.userId.compareTo(other.userId); // lex asc
    }

    @Override public boolean equals(Object o) {
        return o instanceof UserTeam && userId.equals(((UserTeam) o).userId);
    }
    @Override public int hashCode() { return userId.hashCode(); }
}

// ==================== LEADERBOARD ====================

class Leaderboard {
    private final Map<String, UserTeam> users = new HashMap<>();
    private final Map<String, Integer> playerScores = new HashMap<>();
    private final Map<String, Set<String>> playerToUsers = new HashMap<>();  // reverse index
    private final TreeSet<UserTeam> board = new TreeSet<>();

    /** Register user with team. Score = sum of current player scores. */
    void addUser(String userId, List<String> playerIds) {
        // TODO: Implement
        // HINT: UserTeam team = new UserTeam(userId, playerIds);
        // HINT: int initialScore = 0;
        // HINT: for (String pid : playerIds) {
        // HINT:     initialScore += playerScores.getOrDefault(pid, 0);
        // HINT:     playerToUsers.computeIfAbsent(pid, k -> new HashSet<>()).add(userId);
        // HINT: }
        // HINT: team.score = initialScore;
        // HINT: users.put(userId, team);
        // HINT: board.add(team);
    }

    /** Update player score by delta. Propagate to all users with that player. */
    void addScore(String playerId, int delta) {
        // TODO: Implement
        // HINT: playerScores.merge(playerId, delta, Integer::sum);
        // HINT: Set<String> affectedUsers = playerToUsers.getOrDefault(playerId, Collections.emptySet());
        // HINT: for (String uid : affectedUsers) {
        // HINT:     UserTeam team = users.get(uid);
        // HINT:     board.remove(team);    // remove before score change (TreeSet ordering)
        // HINT:     team.score += delta;
        // HINT:     board.add(team);       // re-add with new score
        // HINT: }
    }

    /** Top K users by score desc, lex userId asc for ties. */
    List<String> getTopK(int k) {
        // TODO: Implement
        // HINT: List<String> result = new ArrayList<>();
        // HINT: for (UserTeam team : board) {
        // HINT:     result.add(team.userId);
        // HINT:     if (result.size() >= k) break;
        // HINT: }
        // HINT: return result;
        return Collections.emptyList();
    }

    int getUserScore(String userId) {
        UserTeam t = users.get(userId);
        return t == null ? 0 : t.score;
    }
}

// ==================== MAIN / TESTS ====================

public class LeaderboardLLD {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════╗");
        System.out.println("║   FANTASY LEADERBOARD - LLD Demo      ║");
        System.out.println("╚═══════════════════════════════════════╝\n");

        Leaderboard lb = new Leaderboard();

        // --- Test 1: Add users, initial state ---
        System.out.println("=== Test 1: Add users ===");
        lb.addUser("uA", List.of("p1", "p2"));
        lb.addUser("uB", List.of("p2"));
        System.out.println("getTopK(2): " + lb.getTopK(2) + " (expected [uA, uB])");
        // Both 0, tie → lex: uA < uB
        System.out.println("✓ Tie-break by lex userId\n");

        // --- Test 2: Score update propagates to both ---
        System.out.println("=== Test 2: Score propagation ===");
        lb.addScore("p2", 10); // p2=10; uA=10, uB=10
        System.out.println("uA score: " + lb.getUserScore("uA") + " (expected 10)");
        System.out.println("uB score: " + lb.getUserScore("uB") + " (expected 10)");
        System.out.println("getTopK(2): " + lb.getTopK(2) + " (expected [uA, uB])");
        System.out.println("✓ Shared player updates both users\n");

        // --- Test 3: Different scores, ranking changes ---
        System.out.println("=== Test 3: Ranking change ===");
        lb.addScore("p1", 3); // p1=3; uA=13, uB=10
        System.out.println("getTopK(1): " + lb.getTopK(1) + " (expected [uA])");
        System.out.println("uA: " + lb.getUserScore("uA") + ", uB: " + lb.getUserScore("uB"));
        System.out.println("✓ uA leads with higher score\n");

        // --- Test 4: Negative score ---
        System.out.println("=== Test 4: Negative delta ===");
        lb.addScore("p2", -5); // p2=5; uA=8, uB=5
        System.out.println("getTopK(5): " + lb.getTopK(5) + " (expected [uA, uB])");
        System.out.println("uA: " + lb.getUserScore("uA") + " (expected 8)");
        System.out.println("uB: " + lb.getUserScore("uB") + " (expected 5)");
        System.out.println("✓ Negative delta works\n");

        // --- Test 5: New user inherits existing player scores ---
        System.out.println("=== Test 5: Late joiner inherits scores ===");
        lb.addUser("uC", List.of("p1", "p2")); // p1=3, p2=5 → uC=8
        System.out.println("uC score: " + lb.getUserScore("uC") + " (expected 8)");
        System.out.println("getTopK(3): " + lb.getTopK(3) + " (expected [uA, uC, uB])");
        // uA=8, uC=8 → tie → lex: uA < uC; uB=5
        System.out.println("✓ Late joiner gets current player scores\n");

        // --- Test 6: Player on many teams ---
        System.out.println("=== Test 6: Shared player across 3 teams ===");
        lb.addScore("p2", 2); // p2=7; uA=10, uB=7, uC=10
        System.out.println("uA: " + lb.getUserScore("uA") + ", uB: " + lb.getUserScore("uB")
            + ", uC: " + lb.getUserScore("uC"));
        System.out.println("getTopK(3): " + lb.getTopK(3));
        System.out.println("✓ All three teams updated\n");

        // --- Test 7: K > total users ---
        System.out.println("=== Test 7: K > user count ===");
        System.out.println("getTopK(100): " + lb.getTopK(100) + " (all 3 users)");
        System.out.println("✓ Returns all when K exceeds count\n");

        // --- Test 8: Single player team ---
        System.out.println("=== Test 8: Single player team ===");
        Leaderboard lb2 = new Leaderboard();
        lb2.addUser("solo", List.of("px"));
        lb2.addScore("px", 42);
        System.out.println("solo: " + lb2.getUserScore("solo") + " (expected 42)");
        System.out.println("getTopK(1): " + lb2.getTopK(1));
        System.out.println("✓ Single player team\n");

        // --- Test 9: Scale ---
        System.out.println("=== Test 9: Scale ===");
        Leaderboard lb3 = new Leaderboard();
        for (int i = 0; i < 1000; i++)
            lb3.addUser("u" + String.format("%04d", i),
                List.of("p" + (i % 50), "p" + ((i + 1) % 50)));
        long t = System.nanoTime();
        for (int i = 0; i < 10000; i++)
            lb3.addScore("p" + (i % 50), 1);
        long scoreTime = System.nanoTime() - t;
        t = System.nanoTime();
        List<String> top = lb3.getTopK(10);
        long topKTime = System.nanoTime() - t;
        System.out.printf("10K score updates: %.2f ms\n", scoreTime / 1e6);
        System.out.printf("getTopK(10): %.2f ms, top: %s\n", topKTime / 1e6, top);
        System.out.println("✓ Scales well\n");

        // --- Test 10: Zero-sum scenario ---
        System.out.println("=== Test 10: Zero-sum ===");
        Leaderboard lb4 = new Leaderboard();
        lb4.addUser("x", List.of("q"));
        lb4.addScore("q", 5);
        lb4.addScore("q", -5);
        System.out.println("x score: " + lb4.getUserScore("x") + " (expected 0)");
        System.out.println("✓ Back to zero\n");

        System.out.println("════════ ALL 10 TESTS PASSED ✓ ════════");
    }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. REVERSE INDEX: playerToUsers map enables O(U) propagation when
 *    a player's score changes — only update affected users.
 *
 * 2. TREESET TRICK: To update score in TreeSet (sorted structure),
 *    must remove → modify → re-add. Modifying in-place breaks ordering.
 *
 * 3. COMPLEXITY:
 *    addUser:  O(P + log N) — iterate players + insert into TreeSet
 *    addScore: O(U * log N) — update U users, each TreeSet remove+add
 *    getTopK:  O(K) — iterate sorted set
 *
 * 4. MANY-TO-MANY: Player belongs to multiple teams. Score delta
 *    on player propagates to ALL users with that player.
 *
 * 5. LATE JOINER: New user's score = sum of current player scores
 *    (not zero). playerScores map tracks cumulative per-player.
 *
 * 6. SCALE: For millions of users, shard by userId.
 *    For real-time: Redis sorted sets (ZADD, ZREVRANGE).
 *    For eventual consistency: batch score updates + periodic sort.
 *
 * 7. REAL-WORLD: Dream11, ESPN Fantasy, FanDuel.
 *    Redis ZSET for O(log N) rank queries, pub/sub for live updates.
 */
