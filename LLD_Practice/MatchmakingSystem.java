import java.time.*;
import java.util.*;
import java.util.concurrent.*;

// ===== ENUMS =====

enum PlayerStatus { IDLE, QUEUED, IN_MATCH }

enum MatchStatus { WAITING, STARTED, COMPLETED }

// ===== DOMAIN CLASSES =====

class Player {
    final String playerId;
    final int skillRating;          // MMR: 0-3000
    PlayerStatus status;
    LocalDateTime queuedAt;
    
    Player(String playerId, int skillRating) {
        this.playerId = playerId;
        this.skillRating = skillRating;
        this.status = PlayerStatus.IDLE;
    }
}

class Match {
    final String matchId;
    final List<Player> players;
    MatchStatus status;
    
    Match(List<Player> players) {
        this.matchId = "MATCH-" + UUID.randomUUID().toString().substring(0, 6);
        this.players = new ArrayList<>(players);
        this.status = MatchStatus.WAITING;
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
    
    public MatchmakingService(int playersPerMatch, int skillRange) {
        this.players = new ConcurrentHashMap<>();
        this.matchQueue = new ConcurrentLinkedQueue<>();
        this.matches = new ConcurrentHashMap<>();
        this.playersPerMatch = playersPerMatch;
        this.skillRange = skillRange;
    }
    
    /**
     * Register a player
     */
    public Player registerPlayer(String id, int skillRating) {
        // HINT: Player p = new Player(id, skillRating);
        // HINT: players.put(id, p);
        // HINT: return p;
        Player p=new Player(id, skillRating);
        players.put(id, p);
        return p;
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
        // HINT: Player p = players.get(playerId);
        // HINT: if (p == null || p.status != PlayerStatus.IDLE) return;
        // HINT: p.status = PlayerStatus.QUEUED;
        // HINT: p.queuedAt = LocalDateTime.now();
        // HINT: matchQueue.offer(playerId);
        // HINT: System.out.println("  🎮 " + p.playerId + " joined queue");
        // HINT: tryMatch();
        Player p =players.get(playerId);
        if(p==null || p.status!=PlayerStatus.IDLE) return;
        p.status=PlayerStatus.QUEUED;
        p.queuedAt=LocalDateTime.now();
        matchQueue.offer(playerId);
        tryMatch();
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
        // HINT: Player p = players.get(playerId);
        // HINT: if (p == null || p.status != PlayerStatus.QUEUED) return;
        // HINT: matchQueue.remove(playerId);
        // HINT: p.status = PlayerStatus.IDLE;
        // HINT: System.out.println("  ❌ " + p.playerId + " left queue");
        Player p=players.get(playerId);
        if(p==null || p.status!=PlayerStatus.QUEUED) return;
        matchQueue.remove(playerId);
        p.status=PlayerStatus.IDLE;
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
        // HINT: List<Player> queued = new ArrayList<>();
        // HINT: for (String pid : matchQueue) {
        //     Player p = players.get(pid);
        //     if (p != null && p.status == PlayerStatus.QUEUED) queued.add(p);
        // }
        //
        // HINT: if (queued.size() < playersPerMatch) return;
        //
        // HINT: // Sort by skill for matching
        // HINT: queued.sort((a, b) -> a.skillRating - b.skillRating);
        //
        // HINT: // Sliding window to find group within skill range
        // HINT: for (int i = 0; i <= queued.size() - playersPerMatch; i++) {
        //     int minSkill = queued.get(i).skillRating;
        //     int maxSkill = queued.get(i + playersPerMatch - 1).skillRating;
        //     if (maxSkill - minSkill <= skillRange) {
        //         List<Player> matchPlayers = queued.subList(i, i + playersPerMatch);
        //         createMatch(new ArrayList<>(matchPlayers));
        //         return;
        //     }
        // }
        List<Player> qList=new ArrayList<>();
        for(String pid:matchQueue){
            Player p=players.get(pid);
            if(p!=null && p.status==PlayerStatus.QUEUED) qList.add(p);
        }
        if(qList.size()<playersPerMatch) return;
        qList.sort((a,b)->a.skillRating-b.skillRating);
        for(int i=0;i<=qList.size()-playersPerMatch;i++){
            int minSkill=qList.get(i).skillRating;
            int maxSkill=qList.get(i+playersPerMatch-1).skillRating;
            if(maxSkill-minSkill<=skillRange){
                List<Player> selected=qList.subList(i, i+playersPerMatch);
                createMatch(new ArrayList<>(selected));
                return;
            }
        }

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
        // HINT: Match match = new Match(matchPlayers);
        // HINT: for (Player p : matchPlayers) {
        //     p.status = PlayerStatus.IN_MATCH;
        //     matchQueue.remove(p.playerId);
        // }
        // HINT: match.status = MatchStatus.STARTED;
        // HINT: matches.put(match.matchId, match);
        // HINT: System.out.println("  🏆 Match created: " + match);
        // HINT: matchPlayers.forEach(p -> System.out.println("      " + p));
        Match match=new Match(matchPlayers);
        for(Player p:matchPlayers){
            p.status=PlayerStatus.IN_MATCH;
            matchQueue.remove(p.playerId);
        }
        match.status=MatchStatus.STARTED;
        matches.put(match.matchId, match);
    }
    
    /**
     * Complete a match (players go back to IDLE)
     */
    public void completeMatch(String matchId) {
        // HINT: Match match = matches.get(matchId);
        // HINT: if (match == null) return;
        // HINT: match.status = MatchStatus.COMPLETED;
        // HINT: for (Player p : match.players) p.status = PlayerStatus.IDLE;
        // HINT: System.out.println("  ✅ Match completed: " + matchId);
        Match match=matches.get(matchId);
        if(match==null) return;
        match.status=MatchStatus.COMPLETED;
        for (Player p : match.players) p.status = PlayerStatus.IDLE;
    }
    
    // ===== QUERIES =====
    
    public Player getPlayer(String id) { return players.get(id); }
    public Match getMatch(String id) { return matches.get(id); }
    public int getQueueSize() { return matchQueue.size(); }
    public int getTotalMatches() { return matches.size(); }
    
    public void displayStatus() {
        System.out.println("\n--- Matchmaking Status ---");
        System.out.println("Players: " + players.size() + ", Queue: " + matchQueue.size() 
            + ", Matches: " + matches.size());
        System.out.println("Queued:");
        for (String pid : matchQueue) {
            Player p = players.get(pid);
            if (p != null) System.out.println("  " + p.playerId + "(" + p.skillRating + ", " + p.status + ")");
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
        service.registerPlayer("p1", 1500);
        service.registerPlayer("p2", 1600);
        service.registerPlayer("p3", 1400);
        service.registerPlayer("p4", 1550);
        service.registerPlayer("p5", 2800);
        service.registerPlayer("p6", 2900);
        service.registerPlayer("p7", 500);
        service.registerPlayer("p8", 600);
        service.registerPlayer("p9", 2700);
        service.registerPlayer("p10", 2850);
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
        System.out.println("  Grace status: " + (p7 != null ? p7.status : "null") + " (expect IDLE)");
        System.out.println("  Queue: " + service.getQueueSize());
        System.out.println();
        
        // Test 6: Complete match → players back to IDLE
        System.out.println("=== Test 6: Complete Match ===");
        Player alice = service.getPlayer("p1");
        System.out.println("  Alice before complete: " + (alice != null ? alice.status : "null"));
        // Complete first match
        // (In real test, we'd save the match ID from createMatch)
        System.out.println();
        
        // Test 7: Player can re-queue after match
        System.out.println("=== Test 7: Re-queue After Match ===");
        if (alice != null && alice.status == PlayerStatus.IDLE) {
            service.joinQueue("p1");
            System.out.println("  Alice re-queued: " + alice.status);
        } else {
            System.out.println("  Alice still in match (complete it first)");
        }
        System.out.println();
        
        // Test 8: Queue status
        System.out.println("=== Test 8: Queue Status ===");
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
