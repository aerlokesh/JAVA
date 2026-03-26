import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// ===== ENUMS =====

enum PlayerStatus { IDLE, QUEUED, IN_MATCH }

enum MatchStatus { WAITING, STARTED, COMPLETED, CANCELLED }

enum SkillTier { BRONZE, SILVER, GOLD, PLATINUM, DIAMOND }

// ===== DOMAIN CLASSES =====

class Player {
    private final String playerId;
    private final String name;
    private final int skillRating;          // MMR: 0-3000
    private PlayerStatus status;
    private LocalDateTime queuedAt;
    private int matchesPlayed;
    private int wins;
    
    public Player(String playerId, String name, int skillRating) {
        this.playerId = playerId;
        this.name = name;
        this.skillRating = skillRating;
        this.status = PlayerStatus.IDLE;
        this.matchesPlayed = 0;
        this.wins = 0;
    }
    
    public String getPlayerId() { return playerId; }
    public String getName() { return name; }
    public int getSkillRating() { return skillRating; }
    public PlayerStatus getStatus() { return status; }
    public LocalDateTime getQueuedAt() { return queuedAt; }
    public int getMatchesPlayed() { return matchesPlayed; }
    public int getWins() { return wins; }
    
    public void setStatus(PlayerStatus s) { this.status = s; }
    public void setQueuedAt(LocalDateTime t) { this.queuedAt = t; }
    public void incrementMatches() { matchesPlayed++; }
    public void incrementWins() { wins++; }
    
    public SkillTier getTier() {
        if (skillRating < 600) return SkillTier.BRONZE;
        if (skillRating < 1200) return SkillTier.SILVER;
        if (skillRating < 1800) return SkillTier.GOLD;
        if (skillRating < 2400) return SkillTier.PLATINUM;
        return SkillTier.DIAMOND;
    }
    
    @Override
    public String toString() { return name + "(" + skillRating + ", " + getTier() + ", " + status + ")"; }
}

class Match {
    private final String matchId;
    private final List<Player> players;
    private MatchStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private int avgSkillRating;
    
    public Match(List<Player> players) {
        this.matchId = "MATCH-" + UUID.randomUUID().toString().substring(0, 6);
        this.players = new ArrayList<>(players);
        this.status = MatchStatus.WAITING;
        this.createdAt = LocalDateTime.now();
        this.avgSkillRating = (int) players.stream().mapToInt(Player::getSkillRating).average().orElse(0);
    }
    
    public String getMatchId() { return matchId; }
    public List<Player> getPlayers() { return Collections.unmodifiableList(players); }
    public MatchStatus getStatus() { return status; }
    public int getAvgSkillRating() { return avgSkillRating; }
    
    public void setStatus(MatchStatus s) { 
        this.status = s;
        if (s == MatchStatus.STARTED) this.startedAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return matchId + "[" + status + ", players=" + players.size() + ", avgMMR=" + avgSkillRating + "]";
    }
}

// ===== SERVICE =====

/**
 * Online Matchmaking System - Low Level Design (LLD)
 * 
 * PROBLEM: Design a matchmaking system (like online games) that can:
 * 1. Players join a queue
 * 2. When enough players ready → create a match
 * 3. Skill-based matching (similar MMR players together)
 * 4. Queue timeout (waited too long → widen skill range)
 * 5. Cancel queue
 * 6. Track match history
 * 
 * KEY CONCEPTS:
 * - Queue: ConcurrentLinkedQueue for thread-safe player queue
 * - Skill matching: group players by tier or skill range
 * - Widening: if waiting too long, expand acceptable skill range
 * 
 * PATTERNS: None complex — queue + matching logic
 */
class MatchmakingService {
    private final Map<String, Player> players;
    private final ConcurrentLinkedQueue<String> matchQueue;   // playerIds in queue order
    private final Map<String, Match> matches;
    private final int playersPerMatch;
    private final int skillRange;            // max MMR difference in a match
    private final AtomicInteger totalMatches;
    
    public MatchmakingService(int playersPerMatch, int skillRange) {
        this.players = new ConcurrentHashMap<>();
        this.matchQueue = new ConcurrentLinkedQueue<>();
        this.matches = new ConcurrentHashMap<>();
        this.playersPerMatch = playersPerMatch;
        this.skillRange = skillRange;
        this.totalMatches = new AtomicInteger(0);
    }
    
    /**
     * Register a player
     */
    public Player registerPlayer(String id, String name, int skillRating) {
        // TODO: Implement
        // HINT: Player p = new Player(id, name, skillRating);
        // HINT: players.put(id, p);
        // HINT: return p;
        return null;
    }
    
    /**
     * Player joins the matchmaking queue
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get player → validate exists and is IDLE
     * 2. Set status to QUEUED, record queuedAt time
     * 3. Add to matchQueue
     * 4. Try to form a match (tryMatch)
     */
    public void joinQueue(String playerId) {
        // TODO: Implement
        // HINT: Player p = players.get(playerId);
        // HINT: if (p == null || p.getStatus() != PlayerStatus.IDLE) return;
        // HINT: p.setStatus(PlayerStatus.QUEUED);
        // HINT: p.setQueuedAt(LocalDateTime.now());
        // HINT: matchQueue.offer(playerId);
        // HINT: System.out.println("  🎮 " + p.getName() + " joined queue");
        // HINT: tryMatch();
    }
    
    /**
     * Player cancels queue
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get player → must be QUEUED
     * 2. Remove from matchQueue
     * 3. Set status back to IDLE
     */
    public void cancelQueue(String playerId) {
        // TODO: Implement
        // HINT: Player p = players.get(playerId);
        // HINT: if (p == null || p.getStatus() != PlayerStatus.QUEUED) return;
        // HINT: matchQueue.remove(playerId);
        // HINT: p.setStatus(PlayerStatus.IDLE);
        // HINT: System.out.println("  ❌ " + p.getName() + " left queue");
    }
    
    /**
     * Try to form a match from queued players
     * 
     * IMPLEMENTATION HINTS:
     * 1. Collect all queued player objects from matchQueue
     * 2. Sort by skill rating
     * 3. Try to find a group of playersPerMatch with skill range within limit
     *    → Sliding window: for each starting player, check if player[i+N-1] - player[i] <= skillRange
     * 4. If found → create Match, update players to IN_MATCH, remove from queue
     * 5. If not enough players → do nothing (wait for more)
     */
    private synchronized void tryMatch() {
        // TODO: Implement
        // HINT: List<Player> queued = new ArrayList<>();
        // HINT: for (String pid : matchQueue) {
        //     Player p = players.get(pid);
        //     if (p != null && p.getStatus() == PlayerStatus.QUEUED) queued.add(p);
        // }
        //
        // HINT: if (queued.size() < playersPerMatch) return;
        //
        // HINT: // Sort by skill for matching
        // HINT: queued.sort((a, b) -> a.getSkillRating() - b.getSkillRating());
        //
        // HINT: // Sliding window to find group within skill range
        // HINT: for (int i = 0; i <= queued.size() - playersPerMatch; i++) {
        //     int minSkill = queued.get(i).getSkillRating();
        //     int maxSkill = queued.get(i + playersPerMatch - 1).getSkillRating();
        //     if (maxSkill - minSkill <= skillRange) {
        //         List<Player> matchPlayers = queued.subList(i, i + playersPerMatch);
        //         createMatch(new ArrayList<>(matchPlayers));
        //         return;
        //     }
        // }
    }
    
    /**
     * Create a match from selected players
     * 
     * IMPLEMENTATION HINTS:
     * 1. Create Match object
     * 2. For each player: set status=IN_MATCH, increment matches, remove from queue
     * 3. Set match status to STARTED
     * 4. Store match, increment counter
     */
    private void createMatch(List<Player> matchPlayers) {
        // TODO: Implement
        // HINT: Match match = new Match(matchPlayers);
        // HINT: for (Player p : matchPlayers) {
        //     p.setStatus(PlayerStatus.IN_MATCH);
        //     p.incrementMatches();
        //     matchQueue.remove(p.getPlayerId());
        // }
        // HINT: match.setStatus(MatchStatus.STARTED);
        // HINT: matches.put(match.getMatchId(), match);
        // HINT: totalMatches.incrementAndGet();
        // HINT: System.out.println("  🏆 Match created: " + match);
        // HINT: matchPlayers.forEach(p -> System.out.println("      " + p));
    }
    
    /**
     * Complete a match (players go back to IDLE)
     */
    public void completeMatch(String matchId) {
        // TODO: Implement
        // HINT: Match match = matches.get(matchId);
        // HINT: if (match == null) return;
        // HINT: match.setStatus(MatchStatus.COMPLETED);
        // HINT: for (Player p : match.getPlayers()) p.setStatus(PlayerStatus.IDLE);
        // HINT: System.out.println("  ✅ Match completed: " + matchId);
    }
    
    // ===== QUERIES =====
    
    public Player getPlayer(String id) { return players.get(id); }
    public Match getMatch(String id) { return matches.get(id); }
    public int getQueueSize() { return matchQueue.size(); }
    public int getTotalMatches() { return totalMatches.get(); }
    
    /**
     * Get queue players sorted by wait time
     */
    public List<Player> getQueuedPlayers() {
        // TODO: Implement
        // HINT: List<Player> result = new ArrayList<>();
        // HINT: for (String pid : matchQueue) {
        //     Player p = players.get(pid);
        //     if (p != null && p.getStatus() == PlayerStatus.QUEUED) result.add(p);
        // }
        // HINT: return result;
        return null;
    }
    
    public void displayStatus() {
        System.out.println("\n--- Matchmaking Status ---");
        System.out.println("Players: " + players.size() + ", Queue: " + matchQueue.size() 
            + ", Matches: " + totalMatches.get());
        System.out.println("Queued:");
        for (String pid : matchQueue) {
            Player p = players.get(pid);
            if (p != null) System.out.println("  " + p);
        }
    }
}

// ===== MAIN TEST CLASS =====

public class MatchmakingSystem {
    public static void main(String[] args) {
        System.out.println("=== Online Matchmaking LLD ===\n");
        
        // 4 players per match, max 500 MMR difference
        MatchmakingService service = new MatchmakingService(4, 500);
        
        // Register players with different skill levels
        System.out.println("=== Setup: Register Players ===");
        service.registerPlayer("p1", "Alice", 1500);     // Gold
        service.registerPlayer("p2", "Bob", 1600);       // Gold
        service.registerPlayer("p3", "Charlie", 1400);   // Gold
        service.registerPlayer("p4", "Diana", 1550);     // Gold
        service.registerPlayer("p5", "Eve", 2800);       // Diamond
        service.registerPlayer("p6", "Frank", 2900);     // Diamond
        service.registerPlayer("p7", "Grace", 500);      // Bronze
        service.registerPlayer("p8", "Hank", 600);       // Silver
        service.registerPlayer("p9", "Ivy", 2700);       // Diamond
        service.registerPlayer("p10", "Jack", 2850);     // Diamond
        System.out.println();
        
        // Test 1: Join queue — not enough players yet
        System.out.println("=== Test 1: Join Queue (3 players, need 4) ===");
        service.joinQueue("p1");
        service.joinQueue("p2");
        service.joinQueue("p3");
        System.out.println("  Queue size: " + service.getQueueSize() + " (need 4 for match)");
        System.out.println();
        
        // Test 2: 4th player joins → match created!
        System.out.println("=== Test 2: 4th Player → Match! ===");
        service.joinQueue("p4");
        System.out.println("  Queue after match: " + service.getQueueSize() + " (expect 0)");
        System.out.println("  Matches created: " + service.getTotalMatches());
        System.out.println();
        
        // Test 3: Skill-based matching (Diamond players together)
        System.out.println("=== Test 3: Skill-Based Match (Diamonds) ===");
        service.joinQueue("p5");   // 2800
        service.joinQueue("p6");   // 2900
        service.joinQueue("p9");   // 2700
        service.joinQueue("p10");  // 2850 → all within 500 range
        System.out.println("  Queue: " + service.getQueueSize());
        System.out.println("  Matches: " + service.getTotalMatches());
        System.out.println();
        
        // Test 4: Mixed skill — should NOT match (too far apart)
        System.out.println("=== Test 4: Skill Mismatch (no match) ===");
        service.joinQueue("p7");   // 500 Bronze
        service.joinQueue("p8");   // 600 Silver
        // Only 2 players and they can't match with Diamond players → no match
        System.out.println("  Queue: " + service.getQueueSize() + " (expect 2 — can't match with others)");
        System.out.println();
        
        // Test 5: Cancel queue
        System.out.println("=== Test 5: Cancel Queue ===");
        service.cancelQueue("p7");
        Player p7 = service.getPlayer("p7");
        System.out.println("  Grace status: " + (p7 != null ? p7.getStatus() : "null") + " (expect IDLE)");
        System.out.println("  Queue: " + service.getQueueSize());
        System.out.println();
        
        // Test 6: Complete match → players back to IDLE
        System.out.println("=== Test 6: Complete Match ===");
        Player alice = service.getPlayer("p1");
        System.out.println("  Alice before complete: " + (alice != null ? alice.getStatus() : "null"));
        // Complete first match
        // (In real test, we'd save the match ID from createMatch)
        System.out.println();
        
        // Test 7: Player can re-queue after match
        System.out.println("=== Test 7: Re-queue After Match ===");
        if (alice != null && alice.getStatus() == PlayerStatus.IDLE) {
            service.joinQueue("p1");
            System.out.println("  Alice re-queued: " + alice.getStatus());
        } else {
            System.out.println("  Alice still in match (complete it first)");
        }
        System.out.println();
        
        // Test 8: Queue status
        System.out.println("=== Test 8: Queue Status ===");
        List<Player> queued = service.getQueuedPlayers();
        System.out.println("  Queued players: " + (queued != null ? queued.size() : 0));
        if (queued != null) queued.forEach(p -> System.out.println("    " + p));
        System.out.println();
        
        // Display
        service.displayStatus();
        
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION:
 * =====================
 * 
 * 1. MATCHING ALGORITHM:
 *    Sort queued players by MMR
 *    Sliding window of size N: if max-min <= skillRange → match
 *    O(n log n) sort + O(n) scan
 * 
 * 2. SKILL RANGE WIDENING:
 *    Start: match within 200 MMR
 *    After 30s: widen to 500
 *    After 60s: widen to 1000
 *    After 120s: match anyone available
 *    Trade-off: fairness vs wait time
 * 
 * 3. CONCURRENCY:
 *    ConcurrentLinkedQueue: thread-safe queue
 *    synchronized tryMatch(): prevent double-matching
 *    ConcurrentHashMap: thread-safe player/match maps
 * 
 * 4. REAL-TIME:
 *    Background thread polls queue every 1-5 seconds
 *    Or: triggered on every joinQueue call
 *    WebSocket to notify players when match found
 * 
 * 5. FAIRNESS:
 *    Priority by wait time (FIFO within skill range)
 *    Don't starve long-waiting players → widen range
 *    Balanced teams: split by MMR (highest with lowest)
 * 
 * 6. REAL-WORLD: League of Legends, DOTA 2, Fortnite, Valorant
 * 
 * 7. API:
 *    POST /matchmaking/join      — join queue
 *    DELETE /matchmaking/cancel   — leave queue
 *    GET  /matchmaking/status     — queue position + ETA
 *    WS   /matchmaking/updates    — real-time match notification
 */
