import java.time.*;
import java.util.*;

// ===== EXCEPTIONS =====

class CommentNotFoundException extends Exception {
    public CommentNotFoundException(String id) { super("Comment not found: " + id); }
}

class PostNotFoundException extends Exception {
    public PostNotFoundException(String id) { super("Post not found: " + id); }
}

class UnauthorizedException extends Exception {
    public UnauthorizedException(String msg) { super("Unauthorized: " + msg); }
}

// ===== ENUMS =====

enum ReactionType { LIKE, LOVE, HAHA, WOW, SAD, ANGRY }

// ===== DOMAIN CLASSES =====

class FBUser {
    private final String id;
    private final String name;
    
    public FBUser(String id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    
    @Override
    public String toString() { return name + "(" + id + ")"; }
}

class Comment {
    private final String id;
    private final String postId;
    private final String authorId;
    private final String parentId;   // null if top-level comment
    private String content;
    private boolean deleted;
    private final LocalDateTime createdAt;
    private LocalDateTime editedAt;
    private final Map<String, ReactionType> reactions;   // userId → reaction (1 per user)
    private final List<String> replyIds;                  // child comment IDs (ordered)
    
    public Comment(String postId, String authorId, String parentId, String content) {
        this.id = "CMT-" + UUID.randomUUID().toString().substring(0, 6);
        this.postId = postId;
        this.authorId = authorId;
        this.parentId = parentId;
        this.content = content;
        this.deleted = false;
        this.createdAt = LocalDateTime.now();
        this.reactions = new HashMap<>();
        this.replyIds = new ArrayList<>();
    }
    
    public String getId() { return id; }
    public String getPostId() { return postId; }
    public String getAuthorId() { return authorId; }
    public String getParentId() { return parentId; }
    public String getContent() { return deleted ? "[deleted]" : content; }
    public boolean isDeleted() { return deleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getEditedAt() { return editedAt; }
    public Map<String, ReactionType> getReactions() { return reactions; }
    public List<String> getReplyIds() { return replyIds; }
    
    public void setContent(String content) { this.content = content; this.editedAt = LocalDateTime.now(); }
    public void setDeleted(boolean d) { this.deleted = d; }
    public void addReply(String replyId) { replyIds.add(replyId); }
    
    @Override
    public String toString() {
        return id + " [" + (deleted ? "DELETED" : content) + "] reactions=" + reactions.size() 
            + " replies=" + replyIds.size();
    }
}

// ===== SERVICE =====

/**
 * Facebook Comment Section - Low Level Design (LLD)
 * 
 * PROBLEM: Design a nested comment system (like Facebook/Reddit) that can:
 * 1. Add top-level comments on a post
 * 2. Reply to comments (nested/threaded)
 * 3. Edit and delete comments (soft delete)
 * 4. React to comments (like, love, etc. — one per user)
 * 5. Display comment tree with indentation
 * 6. Get top comments sorted by reactions
 * 
 * KEY DATA STRUCTURE:
 *   Comment has parentId (null = top-level) and replyIds list
 *   This forms a tree: Post → [top-level comments] → [replies] → [replies of replies]
 * 
 * PATTERNS: Composite (tree structure)
 */
class CommentService {
    private final Map<String, FBUser> users;
    private final Map<String, Comment> comments;          // commentId → Comment
    private final Map<String, List<String>> postComments;  // postId → top-level comment IDs
    
    public CommentService() {
        this.users = new HashMap<>();
        this.comments = new HashMap<>();
        this.postComments = new HashMap<>();
    }
    
    public void registerUser(FBUser user) { users.put(user.getId(), user); }
    
    public void createPost(String postId) { postComments.put(postId, new ArrayList<>()); }
    
    // ===== ADD COMMENT =====
    
    /**
     * Add a top-level comment or reply to a post
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate post exists → throw PostNotFoundException
     * 2. If parentId != null, validate parent comment exists and not deleted
     * 3. Create Comment object
     * 4. Store in comments map
     * 5. If top-level (parentId == null): add to postComments list
     * 6. If reply (parentId != null): add to parent's replyIds
     * 7. Return comment
     */
    public Comment addComment(String postId, String userId, String parentId, String content) 
            throws PostNotFoundException, CommentNotFoundException {
        // TODO: Implement
        // HINT: if (!postComments.containsKey(postId)) throw new PostNotFoundException(postId);
        //
        // HINT: if (parentId != null) {
        //     Comment parent = comments.get(parentId);
        //     if (parent == null) throw new CommentNotFoundException(parentId);
        //     if (parent.isDeleted()) throw new CommentNotFoundException("Parent deleted: " + parentId);
        // }
        //
        // HINT: Comment comment = new Comment(postId, userId, parentId, content);
        // HINT: comments.put(comment.getId(), comment);
        //
        // HINT: if (parentId == null) {
        //     postComments.get(postId).add(comment.getId());
        // } else {
        //     comments.get(parentId).addReply(comment.getId());
        // }
        //
        // HINT: String userName = users.containsKey(userId) ? users.get(userId).getName() : userId;
        // HINT: System.out.println("  💬 " + userName + ": " + content 
        //     + (parentId != null ? " (reply to " + parentId + ")" : ""));
        // HINT: return comment;
        return null;
    }
    
    // ===== EDIT COMMENT =====
    
    /**
     * Edit a comment's content
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get comment → throw CommentNotFoundException
     * 2. Check not deleted
     * 3. Check userId == authorId → throw UnauthorizedException
     * 4. Update content, editedAt gets set automatically
     */
    public void editComment(String commentId, String userId, String newContent) 
            throws CommentNotFoundException, UnauthorizedException {
        // TODO: Implement
        // HINT: Comment c = comments.get(commentId);
        // HINT: if (c == null || c.isDeleted()) throw new CommentNotFoundException(commentId);
        // HINT: if (!c.getAuthorId().equals(userId)) throw new UnauthorizedException("Not your comment");
        // HINT: c.setContent(newContent);
        // HINT: System.out.println("  ✏️ Edited " + commentId + " → " + newContent);
    }
    
    // ===== DELETE COMMENT =====
    
    /**
     * Soft-delete a comment
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get comment → throw if not found
     * 2. Check authorization
     * 3. Mark as deleted (content shows "[deleted]")
     * 4. Keep the comment in tree so replies still visible
     */
    public void deleteComment(String commentId, String userId) 
            throws CommentNotFoundException, UnauthorizedException {
        // TODO: Implement
        // HINT: Comment c = comments.get(commentId);
        // HINT: if (c == null) throw new CommentNotFoundException(commentId);
        // HINT: if (!c.getAuthorId().equals(userId)) throw new UnauthorizedException("Not your comment");
        // HINT: c.setDeleted(true);
        // HINT: System.out.println("  🗑️ Deleted " + commentId);
    }
    
    // ===== REACT TO COMMENT =====
    
    /**
     * Add/change reaction on a comment (one per user, replaces previous)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get comment → throw if not found or deleted
     * 2. Put reaction in map (userId → ReactionType)
     *    (Map.put replaces if exists, so toggle/change is automatic)
     */
    public void react(String commentId, String userId, ReactionType reaction) 
            throws CommentNotFoundException {
        // TODO: Implement
        // HINT: Comment c = comments.get(commentId);
        // HINT: if (c == null || c.isDeleted()) throw new CommentNotFoundException(commentId);
        // HINT: c.getReactions().put(userId, reaction);
        // HINT: System.out.println("  " + reaction + " on " + commentId + " by " + userId);
    }
    
    /**
     * Remove reaction
     */
    public void removeReaction(String commentId, String userId) throws CommentNotFoundException {
        // TODO: Implement
        // HINT: Comment c = comments.get(commentId);
        // HINT: if (c == null) throw new CommentNotFoundException(commentId);
        // HINT: c.getReactions().remove(userId);
    }
    
    // ===== DISPLAY COMMENT TREE =====
    
    /**
     * Display comments as a tree with indentation
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get top-level comment IDs for the post
     * 2. For each, call recursive helper: printComment(commentId, depth=0)
     * 3. In helper:
     *    a. Get comment, print with indent (depth * 2 spaces)
     *    b. For each replyId, call printComment(replyId, depth+1)
     */
    public void displayTree(String postId) {
        System.out.println("\n--- Comment Tree for " + postId + " ---");
        // TODO: Implement
        // HINT: List<String> topLevel = postComments.getOrDefault(postId, new ArrayList<>());
        // HINT: for (String cid : topLevel) {
        //     printComment(cid, 0);
        // }
    }
    
    /**
     * Recursive helper to print a comment and its replies
     */
    private void printComment(String commentId, int depth) {
        // TODO: Implement
        // HINT: Comment c = comments.get(commentId);
        // HINT: if (c == null) return;
        // HINT: String indent = "  ".repeat(depth);
        // HINT: String userName = users.containsKey(c.getAuthorId()) ? users.get(c.getAuthorId()).getName() : c.getAuthorId();
        // HINT: String edited = c.getEditedAt() != null ? " (edited)" : "";
        // HINT: System.out.println(indent + userName + ": " + c.getContent() + edited
        //     + " [" + c.getReactions().size() + " reactions]");
        // HINT: for (String replyId : c.getReplyIds()) {
        //     printComment(replyId, depth + 1);
        // }
    }
    
    // ===== QUERIES =====
    
    /**
     * Get top-level comments sorted by reaction count (most first)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get top-level comment IDs for post
     * 2. Map to Comment objects
     * 3. Sort by reactions size descending
     * 4. Return list
     */
    public List<Comment> getTopComments(String postId) {
        // TODO: Implement
        // HINT: return postComments.getOrDefault(postId, new ArrayList<>()).stream()
        //     .map(comments::get)
        //     .filter(c -> c != null && !c.isDeleted())
        //     .sorted((a, b) -> b.getReactions().size() - a.getReactions().size())
        //     .collect(Collectors.toList());
        return null;
    }
    
    /**
     * Get reply count for a comment (direct replies only)
     */
    public int getReplyCount(String commentId) {
        // TODO: Implement
        // HINT: Comment c = comments.get(commentId);
        // HINT: return c != null ? c.getReplyIds().size() : 0;
        return 0;
    }
    
    /**
     * Get reaction breakdown for a comment
     * Returns map: ReactionType → count
     */
    public Map<ReactionType, Long> getReactionBreakdown(String commentId) {
        // TODO: Implement
        // HINT: Comment c = comments.get(commentId);
        // HINT: if (c == null) return Collections.emptyMap();
        // HINT: return c.getReactions().values().stream()
        //     .collect(Collectors.groupingBy(r -> r, Collectors.counting()));
        return null;
    }
    
    public Comment getComment(String id) { return comments.get(id); }
}

// ===== MAIN TEST CLASS =====

public class FacebookCommentSection {
    public static void main(String[] args) {
        System.out.println("=== Facebook Comment Section LLD ===\n");
        
        CommentService service = new CommentService();
        
        // Setup
        FBUser alice = new FBUser("u1", "Alice");
        FBUser bob = new FBUser("u2", "Bob");
        FBUser charlie = new FBUser("u3", "Charlie");
        service.registerUser(alice);
        service.registerUser(bob);
        service.registerUser(charlie);
        service.createPost("post1");
        
        // Test 1: Add top-level comments
        String c1Id = null, c2Id = null;
        System.out.println("=== Test 1: Add Comments ===");
        try {
            Comment c1 = service.addComment("post1", "u1", null, "Great post!");
            Comment c2 = service.addComment("post1", "u2", null, "I agree!");
            c1Id = c1 != null ? c1.getId() : null;
            c2Id = c2 != null ? c2.getId() : null;
            System.out.println("✓ Added 2 top-level comments");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 2: Reply to comment (nested)
        String r1Id = null;
        System.out.println("=== Test 2: Reply to Comment ===");
        try {
            Comment r1 = service.addComment("post1", "u2", c1Id, "Thanks Alice!");
            Comment r2 = service.addComment("post1", "u3", c1Id, "Same here!");
            r1Id = r1 != null ? r1.getId() : null;
            System.out.println("✓ Added 2 replies to comment " + c1Id);
            System.out.println("  Reply count: " + service.getReplyCount(c1Id));
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 3: Nested reply (reply to reply)
        System.out.println("=== Test 3: Nested Reply ===");
        try {
            Comment rr = service.addComment("post1", "u1", r1Id, "You're welcome!");
            System.out.println("✓ Reply to reply added");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 4: React to comments
        System.out.println("=== Test 4: Reactions ===");
        try {
            if (c1Id != null) {
                service.react(c1Id, "u2", ReactionType.LIKE);
                service.react(c1Id, "u3", ReactionType.LOVE);
                service.react(c2Id, "u1", ReactionType.HAHA);
                System.out.println("✓ Reactions added");
                
                Map<ReactionType, Long> breakdown = service.getReactionBreakdown(c1Id);
                System.out.println("  Breakdown for c1: " + (breakdown != null ? breakdown : "null (implement!)"));
            }
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 5: Change reaction (one per user — replaces)
        System.out.println("=== Test 5: Change Reaction ===");
        try {
            if (c1Id != null) {
                service.react(c1Id, "u2", ReactionType.WOW);  // was LIKE → now WOW
                Comment c = service.getComment(c1Id);
                System.out.println("✓ u2 reaction changed to: " + (c != null ? c.getReactions().get("u2") : "null"));
                System.out.println("  Total reactions still 2: " + (c != null ? c.getReactions().size() : 0));
            }
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 6: Edit comment
        System.out.println("=== Test 6: Edit Comment ===");
        try {
            if (c1Id != null) {
                service.editComment(c1Id, "u1", "Great post! (edited)");
                Comment c = service.getComment(c1Id);
                System.out.println("✓ Content: " + (c != null ? c.getContent() : "null"));
                System.out.println("  Edited at: " + (c != null ? c.getEditedAt() : "null"));
            }
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 7: Delete comment (soft delete, replies still visible)
        System.out.println("=== Test 7: Delete Comment ===");
        try {
            if (c2Id != null) {
                service.deleteComment(c2Id, "u2");
                Comment c = service.getComment(c2Id);
                System.out.println("✓ Content shows: " + (c != null ? c.getContent() : "null") + " (expect [deleted])");
                System.out.println("  isDeleted: " + (c != null ? c.isDeleted() : false));
            }
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 8: Display comment tree
        System.out.println("=== Test 8: Comment Tree ===");
        service.displayTree("post1");
        System.out.println();
        
        // Test 9: Top comments (sorted by reactions)
        System.out.println("=== Test 9: Top Comments ===");
        try {
            List<Comment> top = service.getTopComments("post1");
            System.out.println("✓ Top comments: " + (top != null ? top.size() : 0));
            if (top != null) top.forEach(c -> System.out.println("  " + c));
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 10: Exception — unauthorized edit
        System.out.println("=== Test 10: Exception - Unauthorized Edit ===");
        try {
            service.editComment(c1Id, "u2", "Hacked!");  // u2 can't edit u1's comment
            System.out.println("✗ Should have thrown");
        } catch (UnauthorizedException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong: " + e.getMessage());
        }
        System.out.println();
        
        // Test 11: Exception — comment not found
        System.out.println("=== Test 11: Exception - Not Found ===");
        try {
            service.editComment("FAKE-ID", "u1", "test");
            System.out.println("✗ Should have thrown");
        } catch (CommentNotFoundException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong: " + e.getMessage());
        }
        System.out.println();
        
        // Test 12: Exception — post not found
        System.out.println("=== Test 12: Exception - Post Not Found ===");
        try {
            service.addComment("FAKE-POST", "u1", null, "test");
            System.out.println("✗ Should have thrown");
        } catch (PostNotFoundException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong: " + e.getMessage());
        }
        
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION:
 * =====================
 * 
 * 1. DATA STRUCTURE:
 *    Comment tree = each comment has parentId + list of replyIds
 *    Top-level: parentId == null
 *    Display: DFS recursion with depth for indentation
 * 
 * 2. SOFT DELETE:
 *    - Don't remove from tree (replies still need parent)
 *    - Set deleted=true, content shows "[deleted]"
 *    - Like Reddit: "[deleted]" with replies still visible
 * 
 * 3. REACTIONS:
 *    - Map<userId, ReactionType> → one per user
 *    - put() replaces previous (toggle/change)
 *    - GroupBy for breakdown: {LIKE: 5, LOVE: 3}
 * 
 * 4. SORTING:
 *    - Top comments: by reaction count (most popular first)
 *    - Chronological: by createdAt
 *    - Could add: best (weighted score), newest, controversial
 * 
 * 5. SCALABILITY:
 *    - DB: comments table with parent_id (adjacency list)
 *    - Pagination: LIMIT/OFFSET or cursor-based
 *    - Cache hot posts' comment trees in Redis
 *    - Denormalize: store reply_count on comment
 * 
 * 6. API:
 *    POST /posts/{id}/comments           — add comment
 *    POST /comments/{id}/replies         — add reply
 *    PUT  /comments/{id}                 — edit
 *    DELETE /comments/{id}               — soft delete
 *    POST /comments/{id}/reactions       — react
 *    GET  /posts/{id}/comments?sort=top  — get tree
 */
