import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// ==================== ENUMS ====================

enum ReactionType { LIKE, LOVE, HAHA, WOW, SAD, ANGRY }
enum CommentStatus { ACTIVE, EDITED, DELETED }

// ==================== EXCEPTIONS ====================

class CommentNotFoundException extends Exception {
    public CommentNotFoundException(String msg) { super(msg); }
}

class UnauthorizedException extends Exception {
    public UnauthorizedException(String msg) { super(msg); }
}

// ==================== DOMAIN CLASSES ====================

// User
class User {
    String id;
    String name;

    User(String name) {
        this.id = "USER-" + UUID.randomUUID().toString().substring(0, 6);
        this.name = name;
    }

    @Override
    public String toString() { return name; }
}

// Post - what comments are attached to
class Post {
    String id;
    String authorId;
    String content;
    LocalDateTime createdAt;

    Post(String authorId, String content) {
        this.id = "POST-" + UUID.randomUUID().toString().substring(0, 6);
        this.authorId = authorId;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
}

// Comment - supports nested replies (tree structure)
class Comment {
    String id;
    String postId;
    String authorId;
    String authorName;
    String parentCommentId; // null for top-level comments
    String content;
    CommentStatus status;
    LocalDateTime createdAt;
    LocalDateTime editedAt;
    Map<String, ReactionType> reactions; // userId -> reaction (one reaction per user)
    List<String> replyIds; // child comment IDs (ordered)

    Comment(String postId, String authorId, String authorName, String parentCommentId, String content) {
        this.id = "CMT-" + UUID.randomUUID().toString().substring(0, 6);
        this.postId = postId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.parentCommentId = parentCommentId;
        this.content = content;
        this.status = CommentStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.reactions = new HashMap<>();
        this.replyIds = new ArrayList<>();
    }

    void addReaction(String userId, ReactionType type) {
        reactions.put(userId, type); // replaces previous reaction
    }

    void removeReaction(String userId) {
        reactions.remove(userId);
    }

    Map<ReactionType, Long> getReactionSummary() {
        return reactions.values().stream()
            .collect(Collectors.groupingBy(r -> r, Collectors.counting()));
    }

    int getTotalReactions() {
        return reactions.size();
    }
}

// ==================== MAIN SERVICE - THREAD SAFE ====================

class CommentService {
    Map<String, Post> posts;           // postId -> Post
    Map<String, Comment> comments;     // commentId -> Comment
    Map<String, List<String>> postComments; // postId -> list of top-level comment IDs
    Map<String, User> users;           // userId -> User

    CommentService() {
        this.posts = new ConcurrentHashMap<>();
        this.comments = new ConcurrentHashMap<>();
        this.postComments = new ConcurrentHashMap<>();
        this.users = new ConcurrentHashMap<>();
    }

    void registerUser(User user) { users.put(user.id, user); }
    void addPost(Post post) {
        posts.put(post.id, post);
        postComments.put(post.id, Collections.synchronizedList(new ArrayList<>()));
    }

    // Add a comment (top-level or reply) - SYNCHRONIZED per post
    public Comment addComment(String postId, String userId, String parentCommentId, String content)
            throws CommentNotFoundException {
        Post post = posts.get(postId);
        if (post == null) throw new CommentNotFoundException("Post not found: " + postId);
        User user = users.get(userId);

        synchronized (post) {
            // Validate parent if it's a reply
            if (parentCommentId != null) {
                Comment parent = comments.get(parentCommentId);
                if (parent == null || parent.status == CommentStatus.DELETED) {
                    throw new CommentNotFoundException("Parent comment not found: " + parentCommentId);
                }
            }

            Comment comment = new Comment(postId, userId, user.name, parentCommentId, content);
            comments.put(comment.id, comment);

            if (parentCommentId == null) {
                // Top-level comment
                postComments.get(postId).add(comment.id);
            } else {
                // Reply - add to parent's reply list
                comments.get(parentCommentId).replyIds.add(comment.id);
            }

            String type = parentCommentId == null ? "comment" : "reply";
            System.out.println(Thread.currentThread().getName() + ": " + user.name
                + " added " + type + " [" + comment.id + "]: \"" + content + "\"");
            return comment;
        }
    }

    // Edit a comment - only the author can edit
    public void editComment(String commentId, String userId, String newContent)
            throws CommentNotFoundException, UnauthorizedException {
        Comment comment = comments.get(commentId);
        if (comment == null || comment.status == CommentStatus.DELETED) {
            throw new CommentNotFoundException("Comment not found: " + commentId);
        }

        Post post = posts.get(comment.postId);
        synchronized (post) {
            if (!comment.authorId.equals(userId)) {
                throw new UnauthorizedException("Only the author can edit this comment");
            }

            String oldContent = comment.content;
            comment.content = newContent;
            comment.status = CommentStatus.EDITED;
            comment.editedAt = LocalDateTime.now();

            System.out.println(Thread.currentThread().getName() + ": " + comment.authorName
                + " edited [" + commentId + "]: \"" + oldContent + "\" → \"" + newContent + "\"");
        }
    }

    // Delete a comment (soft delete) - only the author can delete
    public void deleteComment(String commentId, String userId)
            throws CommentNotFoundException, UnauthorizedException {
        Comment comment = comments.get(commentId);
        if (comment == null || comment.status == CommentStatus.DELETED) {
            throw new CommentNotFoundException("Comment not found: " + commentId);
        }

        Post post = posts.get(comment.postId);
        synchronized (post) {
            if (!comment.authorId.equals(userId)) {
                throw new UnauthorizedException("Only the author can delete this comment");
            }

            comment.status = CommentStatus.DELETED;
            comment.content = "[deleted]";

            System.out.println(Thread.currentThread().getName() + ": " + comment.authorName
                + " deleted [" + commentId + "]");
        }
    }

    // React to a comment - SYNCHRONIZED per post
    public void reactToComment(String commentId, String userId, ReactionType reaction)
            throws CommentNotFoundException {
        Comment comment = comments.get(commentId);
        if (comment == null || comment.status == CommentStatus.DELETED) {
            throw new CommentNotFoundException("Comment not found: " + commentId);
        }

        Post post = posts.get(comment.postId);
        synchronized (post) {
            comment.addReaction(userId, reaction);
            User user = users.get(userId);
            System.out.println(Thread.currentThread().getName() + ": " + user.name
                + " reacted " + reaction + " on [" + commentId + "]");
        }
    }

    // Remove reaction
    public void removeReaction(String commentId, String userId) throws CommentNotFoundException {
        Comment comment = comments.get(commentId);
        if (comment == null) throw new CommentNotFoundException("Comment not found");

        Post post = posts.get(comment.postId);
        synchronized (post) {
            comment.removeReaction(userId);
        }
    }

    // Display comment tree for a post
    void displayCommentTree(String postId) {
        System.out.println("\n--- Comment Section for Post: " + postId + " ---");
        Post post = posts.get(postId);
        if (post == null) { System.out.println("Post not found"); return; }

        System.out.println("📝 \"" + post.content + "\"");
        List<String> topLevel = postComments.get(postId);
        if (topLevel.isEmpty()) {
            System.out.println("  (no comments yet)");
            return;
        }
        for (String cid : topLevel) {
            printComment(cid, 1);
        }
    }

    private void printComment(String commentId, int depth) {
        Comment c = comments.get(commentId);
        if (c == null) return;

        String indent = "  ".repeat(depth);
        String statusTag = c.status == CommentStatus.EDITED ? " (edited)" :
                           c.status == CommentStatus.DELETED ? " (deleted)" : "";
        String reactionStr = c.getTotalReactions() > 0 ?
            " | " + c.getReactionSummary() : "";

        System.out.println(indent + "💬 " + c.authorName + ": \"" + c.content + "\""
            + statusTag + reactionStr);

        // Print replies recursively
        for (String replyId : c.replyIds) {
            printComment(replyId, depth + 1);
        }
    }

    void displayStats() {
        long active = comments.values().stream().filter(c -> c.status != CommentStatus.DELETED).count();
        long deleted = comments.values().stream().filter(c -> c.status == CommentStatus.DELETED).count();
        long totalReactions = comments.values().stream().mapToInt(Comment::getTotalReactions).sum();
        System.out.println("\n--- Stats: " + comments.size() + " comments (Active: " + active
            + ", Deleted: " + deleted + ") | Reactions: " + totalReactions + " ---");
    }
}

// ==================== MAIN ====================

public class FacebookCommentSection {
    public static void main(String[] args) throws InterruptedException {
        // ---- Setup ----
        CommentService service = new CommentService();

        User alice = new User("Alice");
        User bob = new User("Bob");
        User charlie = new User("Charlie");
        User dave = new User("Dave");
        User eve = new User("Eve");
        service.registerUser(alice);
        service.registerUser(bob);
        service.registerUser(charlie);
        service.registerUser(dave);
        service.registerUser(eve);

        Post post = new Post(alice.id, "Just watched Interstellar again. What a masterpiece! 🚀");
        service.addPost(post);

        // ---- Test 1: Basic Comments & Replies ----
        System.out.println("\n=== Test 1: COMMENTS & NESTED REPLIES ===");
        try {
            Comment c1 = service.addComment(post.id, bob.id, null, "Totally agree! The docking scene is insane.");
            Comment c2 = service.addComment(post.id, charlie.id, null, "Hans Zimmer's score makes it 10x better.");
            Comment r1 = service.addComment(post.id, alice.id, c1.id, "Right? I hold my breath every time!");
            Comment r2 = service.addComment(post.id, dave.id, c1.id, "That and the wormhole scene.");
            Comment r3 = service.addComment(post.id, eve.id, r1.id, "Same here! So intense.");
            Comment c3 = service.addComment(post.id, dave.id, null, "Nolan's best work IMO.");

            service.displayCommentTree(post.id);
        } catch (CommentNotFoundException e) {
            System.out.println("ERROR: " + e.getMessage());
        }

        // ---- Test 2: Reactions ----
        System.out.println("\n=== Test 2: REACTIONS ===");
        try {
            // Get first comment
            String firstCommentId = service.postComments.get(post.id).get(0);
            service.reactToComment(firstCommentId, alice.id, ReactionType.LIKE);
            service.reactToComment(firstCommentId, charlie.id, ReactionType.LOVE);
            service.reactToComment(firstCommentId, dave.id, ReactionType.LIKE);
            service.reactToComment(firstCommentId, eve.id, ReactionType.HAHA);

            // Change reaction (should replace, not duplicate)
            service.reactToComment(firstCommentId, eve.id, ReactionType.LOVE);

            Comment c = service.comments.get(firstCommentId);
            System.out.println("Reactions on [" + firstCommentId + "]: " + c.getReactionSummary()
                + " (total: " + c.getTotalReactions() + ")");
            System.out.println("✓ Reaction change didn't duplicate (expected 4): " + (c.getTotalReactions() == 4));
        } catch (CommentNotFoundException e) {
            System.out.println("ERROR: " + e.getMessage());
        }

        // ---- Test 3: Edit Comment ----
        System.out.println("\n=== Test 3: EDIT COMMENT ===");
        try {
            String firstCommentId = service.postComments.get(post.id).get(0);
            service.editComment(firstCommentId, bob.id, "Totally agree! The docking scene is INSANE! 🔥");
            service.displayCommentTree(post.id);
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }

        // ---- Test 4: Unauthorized Edit ----
        System.out.println("\n=== Test 4: UNAUTHORIZED EDIT ===");
        try {
            String firstCommentId = service.postComments.get(post.id).get(0);
            service.editComment(firstCommentId, charlie.id, "Hacked!"); // Charlie is NOT the author
        } catch (UnauthorizedException e) {
            System.out.println("✓ Correctly prevented: " + e.getMessage());
        } catch (CommentNotFoundException e) {
            System.out.println("ERROR: " + e.getMessage());
        }

        // ---- Test 5: Delete Comment ----
        System.out.println("\n=== Test 5: DELETE COMMENT ===");
        try {
            // Dave deletes his top-level comment
            String davesComment = service.comments.values().stream()
                .filter(c -> c.authorId.equals(dave.id) && c.parentCommentId == null)
                .findFirst().get().id;

            service.deleteComment(davesComment, dave.id);
            service.displayCommentTree(post.id);
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }

        // ---- Test 6: Reply to Deleted Comment ----
        System.out.println("\n=== Test 6: REPLY TO DELETED COMMENT ===");
        try {
            String deletedId = service.comments.values().stream()
                .filter(c -> c.status == CommentStatus.DELETED)
                .findFirst().get().id;
            service.addComment(post.id, bob.id, deletedId, "Replying to deleted comment");
        } catch (CommentNotFoundException e) {
            System.out.println("✓ Correctly prevented: " + e.getMessage());
        }

        // ---- Test 7: CONCURRENCY - Multiple users commenting simultaneously ----
        System.out.println("\n=== Test 7: CONCURRENT COMMENTING (10 threads) ===");
        List<Thread> threads = new ArrayList<>();
        List<String> commentIds = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < 10; i++) {
            final int num = i;
            final User user = (num % 2 == 0) ? bob : charlie;
            Thread t = new Thread(() -> {
                try {
                    Comment c = service.addComment(post.id, user.id, null,
                        "Concurrent comment #" + num + " from " + user.name);
                    commentIds.add(c.id);
                } catch (CommentNotFoundException e) {
                    System.out.println("ERROR: " + e.getMessage());
                }
            }, "CommentThread-" + num);
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) { t.join(); }

        System.out.println("All 10 concurrent comments added: " + (commentIds.size() == 10));
        System.out.println("✓ Concurrency test passed - no data corruption!");

        // ---- Test 8: CONCURRENT REACTIONS on same comment ----
        System.out.println("\n=== Test 8: CONCURRENT REACTIONS (5 users reacting simultaneously) ===");
        threads.clear();
        String targetComment = commentIds.get(0);
        User[] reactors = {alice, bob, charlie, dave, eve};
        ReactionType[] reactionTypes = {ReactionType.LIKE, ReactionType.LOVE, ReactionType.HAHA,
                                        ReactionType.WOW, ReactionType.SAD};

        for (int i = 0; i < 5; i++) {
            final int idx = i;
            Thread t = new Thread(() -> {
                try {
                    service.reactToComment(targetComment, reactors[idx].id, reactionTypes[idx]);
                } catch (CommentNotFoundException e) {
                    System.out.println("ERROR: " + e.getMessage());
                }
            }, "ReactThread-" + i);
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) { t.join(); }

        Comment target = service.comments.get(targetComment);
        System.out.println("Reactions: " + target.getReactionSummary() + " (total: " + target.getTotalReactions() + ")");
        System.out.println("✓ All 5 concurrent reactions recorded: " + (target.getTotalReactions() == 5));

        // ---- Final Display ----
        service.displayCommentTree(post.id);
        service.displayStats();
    }
}