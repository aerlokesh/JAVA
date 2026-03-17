import java.time.*;
import java.util.*;

// ===== CUSTOM EXCEPTION CLASSES =====



class UserAlreadyExistsException extends Exception {
    private String userId;
    public UserAlreadyExistsException(String userId) {
        super("User already exists: " + userId);
        this.userId = userId;
    }
    public String getUserId() { return userId; }
}

class TweetNotFoundException extends Exception {
    private String tweetId;
    public TweetNotFoundException(String tweetId) {
        super("Tweet not found: " + tweetId);
        this.tweetId = tweetId;
    }
    public String getTweetId() { return tweetId; }
}

class InvalidTweetException extends Exception {
    public InvalidTweetException(String message) {
        super(message);
    }
}

// ===== ENUMS & SUPPORTING CLASSES =====

enum FeedGenerationStrategyType { CHRONOLOGICAL, ENGAGEMENT_BASED }

class User {
    private final String userId;
    private final String userName;
    private final Set<String> followers;
    private final Set<String> following;
    
    public User(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
        this.followers = new HashSet<>();
        this.following = new HashSet<>();
    }
    
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public Set<String> getFollowers() { return followers; }
    public Set<String> getFollowing() { return following; }
    public void addFollower(String id) { followers.add(id); }
    public void removeFollower(String id) { followers.remove(id); }
    public void addFollowing(String id) { following.add(id); }
    public void removeFollowing(String id) { following.remove(id); }
    
    @Override
    public String toString() { return "User [" + userId + ", " + userName + "]"; }
}

class Tweet {
    private final String tweetId;
    private final String tweetText;
    private final LocalDateTime timestamp;
    private final String userId;
    private int likeCount;
    private int retweetCount;
    
    public Tweet(String tweetId, String tweetText, String userId) {
        this.tweetId = tweetId;
        this.tweetText = tweetText;
        this.userId = userId;
        this.timestamp = LocalDateTime.now();
    }
    
    public String getTweetId() { return tweetId; }
    public String getTweetText() { return tweetText; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getUserId() { return userId; }
    public int getLikeCount() { return likeCount; }
    public int getRetweetCount() { return retweetCount; }
    public void incrementLikeCount() { likeCount++; }
    public void incrementRetweetCount() { retweetCount++; }
    
    @Override
    public String toString() {
        return "[Tweet " + tweetId + ": " + tweetText + ", likes=" + likeCount + "]";
    }
}

interface FeedGenerationStrategy {
    List<Tweet> getFeed(String userId, List<Tweet> tweets, int limit);
}

class ChronologicalFeedGenerationStrategy implements FeedGenerationStrategy {
    @Override
    public List<Tweet> getFeed(String userId, List<Tweet> tweets, int limit) {
        // TODO: Implement - Sort by timestamp descending, limit results
        return null;
    }
}

class EngagementBasedFeedGenerationStrategy implements FeedGenerationStrategy {
    @Override
    public List<Tweet> getFeed(String userId, List<Tweet> tweets, int limit) {
        // TODO: Implement - Sort by (likes + retweets), then timestamp
        return null;
    }
}

class TwitterService {
    private final Map<String, User> users;
    private final Map<String, Tweet> tweets;
    private final Map<String, List<Tweet>> tweetsByUser;
    private int tweetCount;
    private FeedGenerationStrategy feedGenerationStrategy;
    private static final int MAX_TWEET_LENGTH = 280;
    
    public TwitterService() {
        users = new HashMap<>();
        tweets = new HashMap<>();
        tweetsByUser = new HashMap<>();
        tweetCount = 0;
        feedGenerationStrategy = new ChronologicalFeedGenerationStrategy();
    }
    
    public User addUser(String userId, String userName) throws UserAlreadyExistsException {
        // TODO: Implement
        // HINT: Check if user exists, validate userName uniqueness
        return null;
    }
    
    public boolean follow(String userId, String targetUserId) throws UserNotFoundException {
        // TODO: Implement
        // HINT: Validate users exist, update following/followers sets
        return false;
    }
    
    public boolean unfollow(String userId, String targetUserId) throws UserNotFoundException {
        // TODO: Implement
        return false;
    }
    
    public List<User> getFollowers(String userId) throws UserNotFoundException {
        // TODO: Implement
        return null;
    }
    
    public Tweet addTweet(String userId, String tweetText) 
            throws UserNotFoundException, InvalidTweetException {
        // TODO: Implement
        // HINT: Validate length <= MAX_TWEET_LENGTH
        return null;
    }
    
    public int likeTweet(String userId, String tweetId) 
            throws UserNotFoundException, TweetNotFoundException {
        // TODO: Implement
        return 0;
    }
    
    public Tweet retweetTweet(String userId, String tweetId) 
            throws UserNotFoundException, TweetNotFoundException, InvalidTweetException {
        // TODO: Implement
        return null;
    }
    
    public List<Tweet> getAllTweetsByUser(String userId) throws UserNotFoundException {
        // TODO: Implement
        return null;
    }
    
    public List<Tweet> getFeed(String userId, int limit) throws UserNotFoundException {
        // TODO: Implement
        // HINT: Collect tweets from followed users, apply strategy
        return null;
    }
    
    public void setFeedGenerationStrategy(FeedGenerationStrategy strategy) {
        this.feedGenerationStrategy = strategy;
    }
}

public class TwitterLLD {
    public static void main(String[] args) {
        System.out.println("=== Twitter LLD Test Cases ===\n");
        TwitterService twitter = new TwitterService();
        
        try {
            twitter.addUser("1", "sachin");
            twitter.addUser("2", "virat");
            twitter.follow("1", "2");
            twitter.addTweet("1", "Hello Twitter!");
            System.out.println("✓ Basic functionality works");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
