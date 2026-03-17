import java.time.*;
import java.util.*;

// ===== CUSTOM EXCEPTION CLASSES =====

class CommentNotFoundException extends Exception {
    public CommentNotFoundException(String msg) { super(msg); }
}

class UnauthorizedException extends Exception {
    public UnauthorizedException(String msg) { super(msg); }
}

// ===== ENUMS =====

enum ReactionType { LIKE, LOVE, HAHA, WOW, SAD, ANGRY }
enum CommentStatus { ACTIVE, EDITED, DELETED }

// ===== DOMAIN CLASSES =====

class FBUser {
    String id, name;
    FBUser(String name) {
        this.id = "USER-" + UUID.randomUUID().toString().substring(0, 6);
        this.name = name;
    }
}

class Post {
    String id, authorId, content;
    LocalDateTime createdAt;
    
    Post(String authorId, String content) {
        this.id = "POST-" + UUID.randomUUID().toString().substring(0, 6);
        this.authorId = authorId;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
}

class Comment {
    String id, postId, authorId, authorName, parentCommentId, content;
    CommentStatus status;
    LocalDateTime createdAt, editedAt;
    Map<String, ReactionType> reactions;
    List<String> replyIds;
    
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
    
    void addReaction(String userId, ReactionType type) { reactions.put(userId, type); }
    void removeReaction(String userId) { reactions.remove(userId); }
    int getTotalReactions() { return reactions.size(); }
}

class CommentService {
    Map<String, Post> posts;
    Map<String, Comment> comments;
    Map<String, List<String>> postComments;
    Map<String, FBUser> users;
    
    CommentService() {
        posts = new HashMap<>();
        comments = new HashMap<>();
        postComments = new HashMap<>();
        users = new HashMap<>();
    }
    
    void registerUser(FBUser user) { users.put(user.id, user); }
    void addPost(Post post) {
        posts.put(post.id, post);
        postComments.put(post.id, new ArrayList<>());
    }
    
    /**
     * Add comment or reply
     * IMPLEMENTATION HINTS:
     * 1. Validate post exists
     * 2. If parentCommentId provided, validate parent exists and is not deleted
     * 3. Create Comment object
     * 4. Add to comments map
     * 5. If top-level, add to postComments; if reply, add to parent.replyIds
     */
    public Comment addComment(String postId, String userId, String parentCommentId, String content)
            throws CommentNotFoundException {
        // TODO: Implement
        // HINT: synchronized(post) { ... } for thread safety
        return null;
    }
    
    /**
     * Edit comment
     * IMPLEMENTATION HINTS:
     * 1. Find comment, validate exists and not deleted
     * 2. Check authorization (userId == comment.authorId)
     * 3. Update content, set status to EDITED, set editedAt
     */
    public void editComment(String commentId, String userId, String newContent)
            throws CommentNotFoundException, UnauthorizedException {
        // TODO: Implement
    }
    
    /**
     * Delete comment (soft delete)
     * IMPLEMENTATION HINTS:
     * 1. Find comment
     * 2. Check authorization
     * 3. Set status to DELETED, content to "[deleted]"
     */
    public void deleteComment(String commentId, String userId)
            throws CommentNotFoundException, UnauthorizedException {
        // TODO: Implement
    }
    
    /**
     * React to comment
     * IMPLEMENTATION HINTS:
     * 1. Find comment
     * 2. Add or update reaction
     * 3. One reaction per user (replaces previous)
     */
    public void reactToComment(String commentId, String userId, ReactionType reaction)
            throws CommentNotFoundException {
        // TODO: Implement
    }
    
    void displayCommentTree(String postId) {
        System.out.println("\n--- Comments for Post: " + postId + " ---");
        // TODO: Implement recursive tree display
    }
}

public class FacebookCommentSection {
    public static void main(String[] args) {
        System.out.println("=== Facebook Comment Section Test Cases ===\n");
        
        CommentService service = new CommentService();
        FBUser alice = new FBUser("Alice");
        FBUser bob = new FBUser("Bob");
        service.registerUser(alice);
        service.registerUser(bob);
        
        Post post = new Post(alice.id, "Great post!");
        service.addPost(post);
        
        try {
            Comment c1 = service.addComment(post.id, bob.id, null, "Nice!");
            System.out.println("✓ Comment added");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
