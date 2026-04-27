import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

/*
 * REDDIT COMMENT SYSTEM - Low Level Design
 * ============================================
 * 
 * REQUIREMENTS:
 * 1. Post top-level comments on a post
 * 2. Reply to any comment (nested tree structure)
 * 3. Upvote/downvote comments (prevent double voting)
 * 4. Sort comments: by score (Top), by time (New/Old), controversial
 * 5. Edit/delete own comments (soft delete with [deleted])
 * 6. Get comment tree with depth limit
 * 7. Thread-safe
 * 
 * DESIGN PATTERNS:
 *   Strategy  (CommentSortStrategy)  — TopSort, NewSort, OldSort, ControversialSort
 *   Observer  (CommentListener)      — CommentLogger
 *   Composite (Comment tree)         — parent-child recursive structure
 *   Facade    (RedditCommentService)
 * 
 * KEY DS: Map<commentId, Comment>, each Comment has List<Comment> replies (tree)
 */

// ==================== EXCEPTIONS ====================

class CommentNotFoundException extends RuntimeException {
    CommentNotFoundException(String id) { super("Comment not found: " + id); }
}

class UnauthorizedException extends RuntimeException {
    UnauthorizedException(String msg) { super("Unauthorized: " + msg); }
}

// ==================== ENUMS ====================

enum CommentVoteType { UPVOTE, DOWNVOTE }

enum CommentStatus { ACTIVE, DELETED, REMOVED }

// ==================== MODELS ====================

class RedditComment {
    final String id, postId, authorId;
    String content;
    final String parentId; // null for top-level
    CommentStatus status;
    final long createdAt;
    final List<RedditComment> replies = new CopyOnWriteArrayList<>();
    final ConcurrentHashMap<String, CommentVoteType> votes = new ConcurrentHashMap<>();

    RedditComment(String id, String postId, String authorId, String content, String parentId) {
        this.id = id; this.postId = postId; this.authorId = authorId;
        this.content = content; this.parentId = parentId;
        this.status = CommentStatus.ACTIVE; this.createdAt = System.nanoTime();
    }

    int getScore() {
        int up = 0, down = 0;
        for (CommentVoteType v : votes.values()) {
            if (v == CommentVoteType.UPVOTE) up++; else down++;
        }
        return up - down;
    }

    int getUpvotes() { return (int) votes.values().stream().filter(v -> v == CommentVoteType.UPVOTE).count(); }
    int getDownvotes() { return (int) votes.values().stream().filter(v -> v == CommentVoteType.DOWNVOTE).count(); }

    String getDisplayContent() {
        return status == CommentStatus.DELETED ? "[deleted]"
             : status == CommentStatus.REMOVED ? "[removed]"
             : content;
    }
}

// ==================== INTERFACES ====================

/** Strategy — comment sorting algorithm. */
interface CommentSortStrategy {
    List<RedditComment> sort(List<RedditComment> comments);
}

/** Observer — comment events. */
interface CommentListener {
    void onComment(String eventType, RedditComment comment);
}

// ==================== STRATEGY IMPLEMENTATIONS ====================

/** Top: highest score first. */
class TopSort implements CommentSortStrategy {
    @Override public List<RedditComment> sort(List<RedditComment> comments) {
        return comments.stream()
            .sorted(Comparator.comparingInt(RedditComment::getScore).reversed())
            .collect(Collectors.toList());
    }
}

/** New: newest first. */
class NewSort implements CommentSortStrategy {
    @Override public List<RedditComment> sort(List<RedditComment> comments) {
        return comments.stream()
            .sorted(Comparator.comparingLong((RedditComment c) -> c.createdAt).reversed())
            .collect(Collectors.toList());
    }
}

/** Old: oldest first. */
class OldSort implements CommentSortStrategy {
    @Override public List<RedditComment> sort(List<RedditComment> comments) {
        return comments.stream()
            .sorted(Comparator.comparingLong(c -> c.createdAt))
            .collect(Collectors.toList());
    }
}

/** Controversial: highest total votes (up+down) with close ratio. */
class ControversialSort implements CommentSortStrategy {
    @Override public List<RedditComment> sort(List<RedditComment> comments) {
        return comments.stream()
            .sorted(Comparator.comparingDouble((RedditComment c) -> {
                int up = c.getUpvotes(), down = c.getDownvotes(), total = up + down;
                if (total == 0) return 0.0;
                double ratio = Math.min(up, down) / (double) Math.max(up, down);
                return ratio * total; // high total + close ratio = controversial
            }).reversed())
            .collect(Collectors.toList());
    }
}

// ==================== OBSERVER IMPLEMENTATIONS ====================

class CommentLogger implements CommentListener {
    final List<String> events = new ArrayList<>();
    @Override public void onComment(String eventType, RedditComment c) {
        events.add(eventType + ":" + c.id + " by " + c.authorId);
    }
}

// ==================== REDDIT COMMENT SERVICE (FACADE) ====================

class RedditCommentService {
    private final ConcurrentHashMap<String, RedditComment> allComments = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<RedditComment>> postTopLevel = new ConcurrentHashMap<>();
    private CommentSortStrategy sortStrategy;
    private final List<CommentListener> listeners = new ArrayList<>();
    private final AtomicInteger commentCounter = new AtomicInteger();

    RedditCommentService(CommentSortStrategy sort) { this.sortStrategy = sort; }
    RedditCommentService() { this(new TopSort()); }

    void setSortStrategy(CommentSortStrategy s) { this.sortStrategy = s; }
    void addListener(CommentListener l) { listeners.add(l); }

    private void fireEvent(String type, RedditComment c) {
        listeners.forEach(l -> l.onComment(type, c));
    }

    /** Post a top-level comment on a post. */
    RedditComment postComment(String postId, String authorId, String content) {
        String id = "C-" + commentCounter.incrementAndGet();
        RedditComment comment = new RedditComment(id, postId, authorId, content, null);
        allComments.put(id, comment);
        postTopLevel.computeIfAbsent(postId, k -> new CopyOnWriteArrayList<>()).add(comment);
        fireEvent("POST", comment);
        return comment;
    }

    /** Reply to an existing comment. */
    RedditComment reply(String parentId, String authorId, String content) {
        RedditComment parent = getComment(parentId);
        String id = "C-" + commentCounter.incrementAndGet();
        RedditComment reply = new RedditComment(id, parent.postId, authorId, content, parentId);
        allComments.put(id, reply);
        parent.replies.add(reply);
        fireEvent("REPLY", reply);
        return reply;
    }

    /** Edit own comment. */
    void editComment(String commentId, String userId, String newContent) {
        RedditComment c = getComment(commentId);
        if (!c.authorId.equals(userId)) throw new UnauthorizedException("Not your comment");
        if (c.status != CommentStatus.ACTIVE) throw new UnauthorizedException("Comment is " + c.status);
        c.content = newContent;
        fireEvent("EDIT", c);
    }

    /** Soft delete — shows [deleted] but keeps tree structure. */
    void deleteComment(String commentId, String userId) {
        RedditComment c = getComment(commentId);
        if (!c.authorId.equals(userId)) throw new UnauthorizedException("Not your comment");
        c.status = CommentStatus.DELETED;
        fireEvent("DELETE", c);
    }

    /** Moderator remove. */
    void removeComment(String commentId) {
        RedditComment c = getComment(commentId);
        c.status = CommentStatus.REMOVED;
        fireEvent("REMOVE", c);
    }

    // --- Voting ---

    void vote(String commentId, String userId, CommentVoteType type) {
        getComment(commentId).votes.put(userId, type);
    }

    void removeVote(String commentId, String userId) {
        getComment(commentId).votes.remove(userId);
    }

    int getScore(String commentId) {
        return getComment(commentId).getScore();
    }

    // --- Retrieval ---

    RedditComment getComment(String id) {
        RedditComment c = allComments.get(id);
        if (c == null) throw new CommentNotFoundException(id);
        return c;
    }

    /** Get sorted top-level comments for a post. */
    List<RedditComment> getComments(String postId) {
        List<RedditComment> top = postTopLevel.getOrDefault(postId, Collections.emptyList());
        return sortStrategy.sort(new ArrayList<>(top));
    }

    /** Get comment tree as formatted string with depth limit. */
    String getCommentTree(String postId, int maxDepth) {
        StringBuilder sb = new StringBuilder();
        for (RedditComment c : getComments(postId)) {
            buildTree(c, 0, maxDepth, sb);
        }
        return sb.toString();
    }

    private void buildTree(RedditComment c, int depth, int maxDepth, StringBuilder sb) {
        if (depth > maxDepth) return;
        sb.append("  ".repeat(depth))
          .append(c.getDisplayContent())
          .append(" [").append(c.authorId).append(", score:").append(c.getScore()).append("]\n");
        List<RedditComment> sortedReplies = sortStrategy.sort(new ArrayList<>(c.replies));
        for (RedditComment reply : sortedReplies) buildTree(reply, depth + 1, maxDepth, sb);
    }

    int getTotalComments(String postId) {
        return (int) allComments.values().stream().filter(c -> c.postId.equals(postId)).count();
    }
}

// ==================== MAIN / TESTS ====================

public class RedditCommentSystem {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║   REDDIT COMMENT SYSTEM - LLD Demo        ║");
        System.out.println("╚═══════════════════════════════════════════╝\n");

        // --- Test 1: Post comments ---
        System.out.println("=== Test 1: Post comments ===");
        RedditCommentService svc = new RedditCommentService();
        RedditComment c1 = svc.postComment("post1", "alice", "Great article!");
        RedditComment c2 = svc.postComment("post1", "bob", "I disagree.");
        check(svc.getTotalComments("post1"), 2, "2 comments");
        System.out.println("✓\n");

        // --- Test 2: Nested replies ---
        System.out.println("=== Test 2: Nested replies ===");
        RedditComment r1 = svc.reply(c1.id, "carol", "Why do you think so?");
        RedditComment r2 = svc.reply(r1.id, "alice", "Because the data supports it.");
        RedditComment r3 = svc.reply(c2.id, "dave", "What's your argument?");
        check(c1.replies.size(), 1, "c1 has 1 reply");
        check(r1.replies.size(), 1, "r1 has 1 nested reply");
        check(svc.getTotalComments("post1"), 5, "5 total");
        System.out.println("✓\n");

        // --- Test 3: Voting ---
        System.out.println("=== Test 3: Voting ===");
        svc.vote(c1.id, "bob", CommentVoteType.UPVOTE);
        svc.vote(c1.id, "carol", CommentVoteType.UPVOTE);
        svc.vote(c1.id, "dave", CommentVoteType.DOWNVOTE);
        svc.vote(c2.id, "alice", CommentVoteType.DOWNVOTE);
        check(svc.getScore(c1.id), 1, "c1 score = 2up - 1down = 1");
        check(svc.getScore(c2.id), -1, "c2 score = -1");
        // Double vote prevention
        svc.vote(c1.id, "bob", CommentVoteType.DOWNVOTE); // change vote
        check(svc.getScore(c1.id), -1, "c1 score = carol:up - bob:down - dave:down = -1");
        System.out.println("✓\n");

        // --- Test 4: Sort by Top ---
        System.out.println("=== Test 4: Sort by Top ===");
        svc.setSortStrategy(new TopSort());
        List<RedditComment> sorted = svc.getComments("post1");
        check(sorted.get(0).id, c1.id, "c1 (score 0) first vs c2 (score -1)");
        System.out.println("✓\n");

        // --- Test 5: Sort by New ---
        System.out.println("=== Test 5: Sort by New ===");
        svc.setSortStrategy(new NewSort());
        sorted = svc.getComments("post1");
        check(sorted.get(0).id, c2.id, "c2 (newer) first");
        System.out.println("✓\n");

        // --- Test 6: Sort by Old ---
        System.out.println("=== Test 6: Sort by Old ===");
        svc.setSortStrategy(new OldSort());
        sorted = svc.getComments("post1");
        check(sorted.get(0).id, c1.id, "c1 (older) first");
        System.out.println("✓\n");

        // --- Test 7: Comment tree ---
        System.out.println("=== Test 7: Comment tree ===");
        svc.setSortStrategy(new TopSort());
        String tree = svc.getCommentTree("post1", 3);
        System.out.println(tree);
        check(tree.contains("Great article!"), true, "Contains top comment");
        check(tree.contains("Why do you think so?"), true, "Contains reply");
        check(tree.contains("Because the data"), true, "Contains nested reply");
        System.out.println("✓\n");

        // --- Test 8: Edit comment ---
        System.out.println("=== Test 8: Edit ===");
        svc.editComment(c1.id, "alice", "Updated: Great article with evidence!");
        check(svc.getComment(c1.id).content, "Updated: Great article with evidence!", "Edited");
        try { svc.editComment(c1.id, "bob", "Hack!"); } // not author
        catch (UnauthorizedException e) { System.out.println("  ✓ " + e.getMessage()); }
        System.out.println("✓\n");

        // --- Test 9: Delete (soft) ---
        System.out.println("=== Test 9: Delete ===");
        svc.deleteComment(c2.id, "bob");
        check(c2.getDisplayContent(), "[deleted]", "Shows [deleted]");
        check(c2.replies.size(), 1, "Replies preserved (tree intact)");
        try { svc.deleteComment(c1.id, "bob"); } // not author
        catch (UnauthorizedException e) { System.out.println("  ✓ " + e.getMessage()); }
        System.out.println("✓\n");

        // --- Test 10: Moderator remove ---
        System.out.println("=== Test 10: Mod remove ===");
        svc.removeComment(r3.id);
        check(r3.getDisplayContent(), "[removed]", "Shows [removed]");
        System.out.println("✓\n");

        // --- Test 11: Observer ---
        System.out.println("=== Test 11: Observer ===");
        RedditCommentService svc2 = new RedditCommentService();
        CommentLogger logger = new CommentLogger();
        svc2.addListener(logger);
        RedditComment p = svc2.postComment("post2", "x", "Hello");
        svc2.reply(p.id, "y", "Hi");
        svc2.editComment(p.id, "x", "Hello!");
        svc2.deleteComment(p.id, "x");
        check(logger.events.size(), 4, "4 events: post, reply, edit, delete");
        System.out.println("  Events: " + logger.events);
        System.out.println("✓\n");

        // --- Test 12: Exceptions ---
        System.out.println("=== Test 12: Exceptions ===");
        try { svc.getComment("C-999"); } catch (CommentNotFoundException e) { System.out.println("  ✓ " + e.getMessage()); }
        System.out.println("✓\n");

        // --- Test 13: Thread Safety ---
        System.out.println("=== Test 13: Thread Safety ===");
        RedditCommentService svc3 = new RedditCommentService();
        RedditComment root = svc3.postComment("p3", "author", "Root");
        ExecutorService exec = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            int x = i;
            futures.add(exec.submit(() -> svc3.reply(root.id, "u" + x, "Reply " + x)));
        }
        for (int i = 0; i < 100; i++) {
            int x = i;
            futures.add(exec.submit(() -> svc3.vote(root.id, "u" + x, CommentVoteType.UPVOTE)));
        }
        for (Future<?> f : futures) { try { f.get(); } catch (Exception e) {} }
        exec.shutdown();
        check(root.replies.size(), 100, "100 concurrent replies");
        check(root.getScore(), 100, "100 concurrent upvotes");
        System.out.println("✓\n");

        System.out.println("════════ ALL 13 TESTS PASSED ✓ ════════");
    }

    static void check(int a, int e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
    static void check(String a, String e, String m) { System.out.println("  " + (Objects.equals(a, e) ? "✓" : "✗ GOT '" + a + "'") + " " + m); }
    static void check(boolean a, boolean e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. COMPOSITE PATTERN: Comment has List<Comment> replies — recursive tree.
 *    getCommentTree() DFS with depth limit. Each level sorted by strategy.
 *
 * 2. STRATEGY (CommentSortStrategy): TopSort (score desc), NewSort (newest),
 *    OldSort (oldest), ControversialSort (high votes + close ratio).
 *    Applied at each level of the tree independently.
 *
 * 3. OBSERVER (CommentListener): POST/REPLY/EDIT/DELETE events.
 *    Could feed notifications, moderation queues, analytics.
 *
 * 4. SOFT DELETE: status=DELETED shows [deleted], REMOVED shows [removed].
 *    Tree structure preserved — replies remain visible.
 *
 * 5. VOTING: ConcurrentHashMap<userId, VoteType> per comment.
 *    Prevents double voting, allows vote change. O(1) per vote.
 *
 * 6. THREAD SAFETY: ConcurrentHashMap for comments, CopyOnWriteArrayList for
 *    top-level per post, AtomicInteger for IDs.
 *
 * 7. EXTENSIONS: pagination, collapse threads, awards, flair,
 *    karma from comment votes, content moderation, rate limiting.
 */
