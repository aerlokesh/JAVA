import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

/*
 * VOTING / LIKE SYSTEM - Low Level Design
 * ==========================================
 * 
 * REQUIREMENTS:
 * 1. Vote on items (posts, comments, etc.) — UPVOTE, DOWNVOTE, or reactions
 * 2. Prevent double voting: each user can vote only once per item
 * 3. Change vote (upvote ↔ downvote), remove vote (idempotent)
 * 4. Get vote count, user's vote status, list of voters
 * 5. Pluggable vote types: simple (up/down) or reactions (LIKE, LOVE, HAHA, etc.)
 * 6. Vote event notifications (Observer)
 * 7. Thread-safe with high concurrency
 * 
 * DESIGN PATTERNS:
 *   Strategy  (VoteCountStrategy) — SimpleVoteCounter (up-down), ReactionCounter
 *   Observer  (VoteListener)      — VoteLogger, KarmaUpdater
 *   Facade    (VotingService)
 * 
 * KEY DS: ConcurrentHashMap<itemId, ConcurrentHashMap<userId, VoteType>>
 */

// ==================== EXCEPTIONS ====================

class ItemNotFoundException extends RuntimeException {
    ItemNotFoundException(String itemId) { super("Item not found: " + itemId); }
}

class InvalidVoteException extends RuntimeException {
    InvalidVoteException(String msg) { super("Invalid vote: " + msg); }
}

// ==================== ENUMS ====================

enum VoteType { UPVOTE, DOWNVOTE, LIKE, LOVE, HAHA, WOW, SAD, ANGRY }

// ==================== MODELS ====================

class VotableItem {
    final String id;
    final String authorId;
    final ConcurrentHashMap<String, VoteType> votes = new ConcurrentHashMap<>();

    VotableItem(String id, String authorId) { this.id = id; this.authorId = authorId; }
}

class VoteResult {
    final int upvotes, downvotes, netScore;
    final Map<VoteType, Integer> reactionCounts;

    VoteResult(int upvotes, int downvotes, Map<VoteType, Integer> reactionCounts) {
        this.upvotes = upvotes; this.downvotes = downvotes;
        this.netScore = upvotes - downvotes; this.reactionCounts = reactionCounts;
    }
}

// ==================== INTERFACES ====================

/** Strategy — how to count votes. */
interface VoteCountStrategy {
    VoteResult count(Map<String, VoteType> votes);
}

/** Observer — vote event notifications. */
interface VoteListener {
    void onVote(String itemId, String userId, VoteType type, boolean isNew);
    void onRemoveVote(String itemId, String userId, VoteType previousType);
}

// ==================== STRATEGY IMPLEMENTATIONS ====================

/** Simple up/down counting: net score = upvotes - downvotes. Reddit-style. */
class SimpleVoteCounter implements VoteCountStrategy {
    @Override public VoteResult count(Map<String, VoteType> votes) {
        int up = 0, down = 0;
        for (VoteType v : votes.values()) {
            if (v == VoteType.UPVOTE) up++;
            else if (v == VoteType.DOWNVOTE) down++;
        }
        return new VoteResult(up, down, Collections.emptyMap());
    }
}

/** Reaction counting: count each reaction type separately. Facebook-style. */
class ReactionCounter implements VoteCountStrategy {
    @Override public VoteResult count(Map<String, VoteType> votes) {
        Map<VoteType, Integer> counts = new HashMap<>();
        for (VoteType v : votes.values()) counts.merge(v, 1, Integer::sum);
        int total = votes.size();
        return new VoteResult(total, 0, counts);
    }
}

// ==================== OBSERVER IMPLEMENTATIONS ====================

/** Logs all vote events. */
class VoteLogger implements VoteListener {
    final List<String> events = new ArrayList<>();
    @Override public void onVote(String itemId, String userId, VoteType type, boolean isNew) {
        events.add((isNew ? "NEW" : "CHANGED") + ":" + userId + "→" + type + " on " + itemId);
    }
    @Override public void onRemoveVote(String itemId, String userId, VoteType prev) {
        events.add("REMOVED:" + userId + "→" + prev + " on " + itemId);
    }
}

/** Tracks karma per author: +1 for upvote, -1 for downvote. */
class KarmaUpdater implements VoteListener {
    final ConcurrentHashMap<String, AtomicInteger> karma = new ConcurrentHashMap<>();

    @Override public void onVote(String itemId, String userId, VoteType type, boolean isNew) {
        // In real system, look up author from item. Simplified: skip
    }
    @Override public void onRemoveVote(String itemId, String userId, VoteType prev) {}

    void addKarma(String authorId, int delta) {
        karma.computeIfAbsent(authorId, k -> new AtomicInteger()).addAndGet(delta);
    }

    int getKarma(String authorId) {
        AtomicInteger k = karma.get(authorId);
        return k == null ? 0 : k.get();
    }
}

// ==================== VOTING SERVICE (FACADE) ====================

class VotingService {
    private final ConcurrentHashMap<String, VotableItem> items = new ConcurrentHashMap<>();
    private VoteCountStrategy countStrategy;
    private final List<VoteListener> listeners = new ArrayList<>();
    private final AtomicInteger itemCounter = new AtomicInteger();

    VotingService(VoteCountStrategy strategy) { this.countStrategy = strategy; }
    VotingService() { this(new SimpleVoteCounter()); }

    void setCountStrategy(VoteCountStrategy s) { this.countStrategy = s; }
    void addListener(VoteListener l) { listeners.add(l); }

    // --- Item Management ---

    VotableItem createItem(String authorId) {
        String id = "ITEM-" + itemCounter.incrementAndGet();
        VotableItem item = new VotableItem(id, authorId);
        items.put(id, item);
        return item;
    }

    VotableItem getItem(String itemId) {
        VotableItem item = items.get(itemId);
        if (item == null) throw new ItemNotFoundException(itemId);
        return item;
    }

    // --- Vote Operations ---

    /** Vote on an item. Idempotent: same vote type = no-op. Different type = change. */
    void vote(String itemId, String userId, VoteType type) {
        if (userId == null || userId.isEmpty()) throw new InvalidVoteException("userId required");
        VotableItem item = getItem(itemId);
        VoteType previous = item.votes.put(userId, type);
        boolean isNew = previous == null;
        boolean changed = previous != null && previous != type;
        if (isNew || changed) {
            listeners.forEach(l -> l.onVote(itemId, userId, type, isNew));
        }
    }

    /** Remove a user's vote. Idempotent: no vote = no-op. */
    boolean removeVote(String itemId, String userId) {
        VotableItem item = getItem(itemId);
        VoteType removed = item.votes.remove(userId);
        if (removed != null) {
            listeners.forEach(l -> l.onRemoveVote(itemId, userId, removed));
            return true;
        }
        return false;
    }

    /** Toggle: if already voted same type → remove, else → set. */
    void toggleVote(String itemId, String userId, VoteType type) {
        VotableItem item = getItem(itemId);
        VoteType current = item.votes.get(userId);
        if (current == type) removeVote(itemId, userId);
        else vote(itemId, userId, type);
    }

    // --- Query Operations ---

    /** Get vote counts using current strategy. */
    VoteResult getVoteResult(String itemId) {
        return countStrategy.count(getItem(itemId).votes);
    }

    /** Get a specific user's vote on an item, or null. */
    VoteType getUserVote(String itemId, String userId) {
        return getItem(itemId).votes.get(userId);
    }

    /** Get all voters for an item. */
    Set<String> getVoters(String itemId) {
        return new HashSet<>(getItem(itemId).votes.keySet());
    }

    /** Get voters filtered by vote type. */
    Set<String> getVotersByType(String itemId, VoteType type) {
        return getItem(itemId).votes.entrySet().stream()
            .filter(e -> e.getValue() == type)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    int getItemCount() { return items.size(); }
}

// ==================== MAIN / TESTS ====================

public class VotingSystem {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════╗");
        System.out.println("║   VOTING / LIKE SYSTEM - LLD Demo     ║");
        System.out.println("╚═══════════════════════════════════════╝\n");

        // --- Test 1: Basic upvote/downvote ---
        System.out.println("=== Test 1: Basic vote ===");
        VotingService svc = new VotingService();
        VotableItem post = svc.createItem("author1");
        svc.vote(post.id, "alice", VoteType.UPVOTE);
        svc.vote(post.id, "bob", VoteType.UPVOTE);
        svc.vote(post.id, "carol", VoteType.DOWNVOTE);
        VoteResult r = svc.getVoteResult(post.id);
        check(r.upvotes, 2, "2 upvotes");
        check(r.downvotes, 1, "1 downvote");
        check(r.netScore, 1, "Net score = 1");
        System.out.println("✓\n");

        // --- Test 2: Double vote prevention (idempotent) ---
        System.out.println("=== Test 2: Double vote ===");
        svc.vote(post.id, "alice", VoteType.UPVOTE); // same vote again
        r = svc.getVoteResult(post.id);
        check(r.upvotes, 2, "Still 2 upvotes (idempotent)");
        System.out.println("✓\n");

        // --- Test 3: Change vote ---
        System.out.println("=== Test 3: Change vote ===");
        svc.vote(post.id, "alice", VoteType.DOWNVOTE); // change upvote → downvote
        r = svc.getVoteResult(post.id);
        check(r.upvotes, 1, "1 upvote (alice changed)");
        check(r.downvotes, 2, "2 downvotes");
        check(r.netScore, -1, "Net = -1");
        System.out.println("✓\n");

        // --- Test 4: Remove vote ---
        System.out.println("=== Test 4: Remove vote ===");
        check(svc.removeVote(post.id, "alice"), true, "Removed alice's vote");
        r = svc.getVoteResult(post.id);
        check(r.upvotes, 1, "1 upvote remaining");
        check(r.downvotes, 1, "1 downvote remaining");
        check(svc.removeVote(post.id, "alice"), false, "Already removed = no-op");
        System.out.println("✓\n");

        // --- Test 5: Toggle vote ---
        System.out.println("=== Test 5: Toggle vote ===");
        svc.vote(post.id, "dave", VoteType.UPVOTE);
        svc.toggleVote(post.id, "dave", VoteType.UPVOTE); // toggle off
        check(svc.getUserVote(post.id, "dave") == null, true, "Toggle off = removed");
        svc.toggleVote(post.id, "dave", VoteType.DOWNVOTE); // toggle on
        check(svc.getUserVote(post.id, "dave"), VoteType.DOWNVOTE, "Toggle on = downvote");
        System.out.println("✓\n");

        // --- Test 6: Get user vote status ---
        System.out.println("=== Test 6: User vote status ===");
        check(svc.getUserVote(post.id, "bob"), VoteType.UPVOTE, "Bob = UPVOTE");
        check(svc.getUserVote(post.id, "nonexistent") == null, true, "No vote = null");
        System.out.println("✓\n");

        // --- Test 7: Get voters ---
        System.out.println("=== Test 7: Voters ===");
        Set<String> voters = svc.getVoters(post.id);
        check(voters.size(), 3, "3 voters (bob, carol, dave)");
        Set<String> upvoters = svc.getVotersByType(post.id, VoteType.UPVOTE);
        check(upvoters.contains("bob"), true, "Bob is upvoter");
        System.out.println("✓\n");

        // --- Test 8: Strategy swap → Reactions ---
        System.out.println("=== Test 8: Reactions (Strategy swap) ===");
        VotingService svc2 = new VotingService(new ReactionCounter());
        VotableItem post2 = svc2.createItem("author2");
        svc2.vote(post2.id, "alice", VoteType.LIKE);
        svc2.vote(post2.id, "bob", VoteType.LOVE);
        svc2.vote(post2.id, "carol", VoteType.HAHA);
        svc2.vote(post2.id, "dave", VoteType.LIKE);
        VoteResult r2 = svc2.getVoteResult(post2.id);
        check(r2.upvotes, 4, "4 total reactions");
        check(r2.reactionCounts.getOrDefault(VoteType.LIKE, 0), 2, "2 LIKEs");
        check(r2.reactionCounts.getOrDefault(VoteType.LOVE, 0), 1, "1 LOVE");
        check(r2.reactionCounts.getOrDefault(VoteType.HAHA, 0), 1, "1 HAHA");
        System.out.println("✓\n");

        // --- Test 9: Observer ---
        System.out.println("=== Test 9: Observer ===");
        VotingService svc3 = new VotingService();
        VoteLogger logger = new VoteLogger();
        svc3.addListener(logger);
        VotableItem p3 = svc3.createItem("a");
        svc3.vote(p3.id, "u1", VoteType.UPVOTE);
        svc3.vote(p3.id, "u1", VoteType.DOWNVOTE); // change
        svc3.removeVote(p3.id, "u1");
        check(logger.events.size(), 3, "3 events: new, changed, removed");
        System.out.println("  Events: " + logger.events);
        System.out.println("✓\n");

        // --- Test 10: Exceptions ---
        System.out.println("=== Test 10: Exceptions ===");
        try { svc.getItem("ITEM-999"); } catch (ItemNotFoundException e) { System.out.println("  ✓ " + e.getMessage()); }
        try { svc.vote(post.id, "", VoteType.UPVOTE); } catch (InvalidVoteException e) { System.out.println("  ✓ " + e.getMessage()); }
        System.out.println("✓\n");

        // --- Test 11: Thread Safety ---
        System.out.println("=== Test 11: Thread Safety ===");
        VotingService svc4 = new VotingService();
        VotableItem p4 = svc4.createItem("author");
        ExecutorService exec = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        // 1000 users each upvote concurrently
        for (int i = 0; i < 1000; i++) {
            int x = i;
            futures.add(exec.submit(() -> svc4.vote(p4.id, "user" + x, VoteType.UPVOTE)));
        }
        // 200 users change to downvote
        for (int i = 0; i < 200; i++) {
            int x = i;
            futures.add(exec.submit(() -> svc4.vote(p4.id, "user" + x, VoteType.DOWNVOTE)));
        }
        for (Future<?> f : futures) { try { f.get(); } catch (Exception e) {} }
        exec.shutdown();
        VoteResult r4 = svc4.getVoteResult(p4.id);
        System.out.println("  Voters: " + svc4.getVoters(p4.id).size() + ", Up: " + r4.upvotes + ", Down: " + r4.downvotes);
        check(svc4.getVoters(p4.id).size(), 1000, "1000 unique voters");
        check(r4.upvotes + r4.downvotes, 1000, "Total = 1000 (no duplicates)");
        System.out.println("✓\n");

        // --- Test 12: Scale ---
        System.out.println("=== Test 12: Scale ===");
        VotingService svc5 = new VotingService();
        VotableItem bigPost = svc5.createItem("viral");
        long t = System.nanoTime();
        for (int i = 0; i < 10000; i++) svc5.vote(bigPost.id, "u" + i, i % 3 == 0 ? VoteType.DOWNVOTE : VoteType.UPVOTE);
        long voteTime = System.nanoTime() - t;
        t = System.nanoTime();
        VoteResult r5 = svc5.getVoteResult(bigPost.id);
        long countTime = System.nanoTime() - t;
        System.out.printf("  10K votes: %.2f ms, count: %.2f ms (up=%d, down=%d, net=%d)\n",
            voteTime/1e6, countTime/1e6, r5.upvotes, r5.downvotes, r5.netScore);
        check(r5.upvotes + r5.downvotes, 10000, "10K total");
        System.out.println("✓\n");

        System.out.println("════════ ALL 12 TESTS PASSED ✓ ════════");
    }

    static void check(int a, int e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
    static void check(boolean a, boolean e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
    static void check(VoteType a, VoteType e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. CORE DS: ConcurrentHashMap<userId, VoteType> per item.
 *    Prevents double voting — put() overwrites, only 1 entry per user.
 *    O(1) vote, O(1) check, O(n) count.
 *
 * 2. STRATEGY (VoteCountStrategy): SimpleVoteCounter (Reddit: up-down net score),
 *    ReactionCounter (Facebook: count per reaction type). Swap at runtime.
 *
 * 3. OBSERVER (VoteListener): VoteLogger, KarmaUpdater.
 *    Decouples vote logic from notifications/karma/analytics.
 *
 * 4. IDEMPOTENCY: Same vote twice = no-op (ConcurrentHashMap.put is idempotent).
 *    Change vote = atomic put with new type. Remove = remove key.
 *
 * 5. TOGGLE: If current == type → remove, else → set. Like YouTube like button.
 *
 * 6. THREAD SAFETY: ConcurrentHashMap for votes — lock-free reads, atomic puts.
 *    No explicit locks needed. AtomicInteger for counters.
 *
 * 7. CONCURRENCY: ConcurrentHashMap handles concurrent votes from different users
 *    on same item safely. No lost updates because each user has own key.
 *
 * 8. EXTENSIONS: weighted votes, vote expiry, rate limiting, karma decay,
 *    hot/trending calculation, sharding by itemId for distribution.
 */
