import java.util.*;
import java.time.*;

enum FeedGenerationStrategyType{
    CHRONOLOGICAL,
    ENGAGEMENT_BASED
}

class User {
    private final String userId;
    private final String userName;
    private final Set<String> followers;
    private final Set<String> following;
    public User(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
        followers = new HashSet<>();
        following = new HashSet<>();
    }
    // getters
    public String getUserId() { return userId;}
    public String getUserName() { return userName;}
    public Set<String> getFollowers() { return followers;}
    public Set<String> getFollowing() { return following;}
    // functions
    public void follow(String targetUserId) {
        following.add(targetUserId);
    }
    public void unfollow(String targetUserId) {
        following.remove(targetUserId);
    }
    public void addFollower(String targetUserId) {
        followers.add(targetUserId);
    }
    public void removeFollower(String targetUserId) {
        followers.remove(targetUserId);
    }
    public void addFollowing(String targetUserId) {
        following.add(targetUserId);
    }
    public void removeFollowing(String targetUserId) {
        following.remove(targetUserId);
    }
    @Override
    public String toString() {
        return "User [userId=" + userId + ", userName=" + userName + "]";
    }
}

class Tweet{
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
        this.likeCount = 0;
        this.retweetCount = 0;
    }
    // getter
    public String getTweetId() { return tweetId;}
    public String getTweetText() { return tweetText;}
    public LocalDateTime getTimestamp() { return timestamp;}
    public String getUserId() { return userId;}
    public int getLikeCount() { return likeCount;}
    public int getRetweetCount() { return retweetCount;}

    // setter
    public void incrementLikeCount() { likeCount++; }
    public void incrementRetweetCount() { retweetCount++; }
    public void addLikeCount() { likeCount++; }
    public void addRetweetCount() { retweetCount++; }

    @Override
    public String toString() {
        return "[ TweetId "+tweetId+", tweet text "+tweetText+", userId "+userId+", likes "+likeCount+", retweets "+retweetCount+" ]";
    }
}

interface FeedGenerationStrategy{
    List<Tweet> getFeed(String userId, List<Tweet> tweets, int limit);
    FeedGenerationStrategyType getStrategyType();
}

class ChronologicalFeedGenerationStrategy implements FeedGenerationStrategy{
    @Override
    public List<Tweet> getFeed(String userId, List<Tweet> tweets, int limit) {
        return tweets.stream().sorted((t1,t2)->t2.getTimestamp().compareTo(t1.getTimestamp())).limit(limit).toList();
    }
    @Override
    public FeedGenerationStrategyType getStrategyType() {
        return FeedGenerationStrategyType.CHRONOLOGICAL;
    }
}

class EngagementBasedFeedGenerationStrategy implements FeedGenerationStrategy{
    @Override
    public List<Tweet> getFeed(String userId, List<Tweet> tweets, int limit) {
        return tweets.stream().sorted((t1 ,t2)->{
            int score1=t1.getLikeCount()+t2.getLikeCount();
            int score2=t2.getRetweetCount()+t1.getRetweetCount();
            int scoreCompare=Integer.compare(score1,score2);
            if(scoreCompare!=0){
                return scoreCompare;
            }
            return t2.getTimestamp().compareTo(t1.getTimestamp());
        }).limit(limit).toList();
    }
    @Override
    public FeedGenerationStrategyType getStrategyType() {
        return FeedGenerationStrategyType.ENGAGEMENT_BASED;
    }
}

class TwitterService{
    private final Map<String, User> users;
    private final Map<String, Tweet> tweets;
    private final Map<String,List<Tweet>> tweetsByUser;
    private int tweetCount;
    private FeedGenerationStrategy feedGenerationStrategy;

    public TwitterService() {
        users = new HashMap<>();
        tweets = new HashMap<>();
        tweetsByUser = new HashMap<>();
        tweetCount = 0;
        feedGenerationStrategy = new ChronologicalFeedGenerationStrategy();
    }
    public User addUser(String userId, String userName) {
        if(users.containsKey(userId)) {
            throw new IllegalArgumentException("User with id "+userId+" already exists");
        }
        if(users.values().stream().anyMatch(user -> user.getUserName().equals(userName))){
            throw new IllegalArgumentException("User with name "+userName+" already exists");
        }
        User user = new User(userId, userName);
        users.put(userId, user);
        tweetsByUser.put(userId,new ArrayList<>());
        System.out.println("Adding a new user "+userId+ " username "+userName);
        return user;
    }
    public boolean follow(String userId, String targetUserId) {
        if(!users.containsKey(userId)) {
            throw new IllegalArgumentException("User with id "+userId+" does not exist");
        }
        if(!users.containsKey(targetUserId)) {
            throw new IllegalArgumentException("User with id "+targetUserId+" does not exist");
        }
        User user = users.get(userId);
        User userTarget = users.get(targetUserId);
        user.addFollowing(targetUserId);
        userTarget.addFollower(userId);
        System.out.println("User id " + userId+" started following "+targetUserId);
        return true;
    }
    public boolean unfollow(String userId, String targetUserId) {
        if(!users.containsKey(userId)) {
            throw new IllegalArgumentException("User with id "+userId+" does not exist");
        }
        if(!users.containsKey(targetUserId)) {
            throw new IllegalArgumentException("User with id "+targetUserId+" does not exist");
        }
        User user = users.get(userId);
        User userTarget = users.get(targetUserId);
        if(user.getFollowing().contains(targetUserId)){
            user.removeFollowing(targetUserId);
            userTarget.removeFollower(userId);
            System.out.println("User id " + userId+"  unfollowed "+targetUserId);
            return true;
        }else {
            System.out.println("User id " + userId+" not followed "+targetUserId);
            return false;
        }
    }
    public List<User> getFollowers(String targetUserId) {
        if(!users.containsKey(targetUserId)) {
            throw new IllegalArgumentException("User with id "+targetUserId+" does not exist");
        }
        User user = users.get(targetUserId);
        List<User> followers = new ArrayList<>();
        user.getFollowers().forEach(followerId -> followers.add(users.get(followerId)));
        System.out.println("Followers for "+user.toString());
        followers.forEach(followerUser -> System.out.println(followerUser.toString()));
        return followers;
    }
    public Tweet addTweet(String userId, String tweetText) {
        if(!users.containsKey(userId)) {
            throw new IllegalArgumentException("User with id "+userId+" does not exist");
        }
        String tweetId = "Tweet-"+tweetCount++;
        Tweet tweet = new Tweet(tweetId,tweetText, userId);
        List<Tweet> currentTweetsByUser = tweetsByUser.get(userId);
        currentTweetsByUser.add(tweet);
        tweetsByUser.put(userId,currentTweetsByUser);
        tweets.put(tweetId,tweet);
        System.out.println("Adding a new tweet "+tweetId+" tweet "+tweetText);
        return tweet;
    }
    public int likeTweet(String userId, String tweetId) {
        if(!users.containsKey(userId)) {
            throw new IllegalArgumentException("User with id "+userId+" does not exist");
        }
        Tweet tweet = tweets.get(tweetId);
        User user = users.get(userId);
        tweet.addLikeCount();
        System.out.println("Liked "+tweetId+" by user "+user.getUserName());
        return tweet.getLikeCount();
    }
    public Tweet retweetTweet(String userId, String tweetId) {
        if(!users.containsKey(userId)) {
            throw new IllegalArgumentException("User with id "+userId+" does not exist");
        }
        Tweet tweet = tweets.get(tweetId);
        Tweet retweet = addTweet(userId, "RETWEET: "+tweet.getTweetText());
        User user = users.get(userId);
        tweet.addRetweetCount();
        System.out.println("Retweeted "+retweet.toString()+" by user "+user.getUserName());
        return retweet;
    }
    public List<Tweet> getAllTweetsByUser(String userId) {
        if(!users.containsKey(userId)) {
            throw new IllegalArgumentException("User with id "+userId+" does not exist");
        }
        System.out.println("Get all tweets by user "+userId);
        tweetsByUser.get(userId).forEach(tweet -> System.out.println(tweet.toString()));
        return tweetsByUser.get(userId);
    }

    public List<Tweet> getFeed(String userId, int limit) {
        List<Tweet> feed = feedGenerationStrategy.getFeed(userId, getAllTweetsByUser(userId), limit);
        System.out.println("Feed generation strategy for "+userId+" "+tweets.size());
        feed.forEach(tweet -> System.out.println(tweet.toString()));
        return feed;
    }
    public void setFeedGenerationStrategy(FeedGenerationStrategy feedGenerationStrategy) {
        this.feedGenerationStrategy = feedGenerationStrategy;
    }
}



public class TwitterLLD {
    public static void main(String[] args) {
        System.out.println("Demo Twitter LLD");
        TwitterService twitterService = new TwitterService();
        // User related functionality
        System.out.println("Register a new user");
        User sachin = twitterService.addUser("1","sachin");
        User virat = twitterService.addUser("2","virat");
        User messi = twitterService.addUser("3","messi");
        User ronaldo = twitterService.addUser("4","ronaldo");
        System.out.println("Follow a user");
        twitterService.follow(sachin.getUserId(), virat.getUserId());
        twitterService.follow(virat.getUserId(), ronaldo.getUserId());
        twitterService.follow(sachin.getUserId(), messi.getUserId());
        twitterService.follow(virat.getUserId(), messi.getUserId());
        System.out.println("Unfollow a user");
        twitterService.unfollow(sachin.getUserId(), virat.getUserId());
        System.out.println("Get all followers of a user");
        twitterService.unfollow(sachin.getUserId(), messi.getUserId());
        System.out.println("Get all followings of a user");
        twitterService.getFollowers(virat.getUserId());
        // Tweet related functionality
        System.out.println("Post a tweet");
        Tweet sachinGoatTweet = twitterService.addTweet(sachin.getUserId(),"I am the GOAT of cricket");
        twitterService.addTweet(messi.getUserId(), "I am football GOAT");
        twitterService.addTweet(ronaldo.getUserId(),"MESSI sucks");
        System.out.println("Delete a tweet");
        System.out.println("Like a tweet");
        twitterService.likeTweet(sachin.getUserId(), sachinGoatTweet.getTweetId());
        System.out.println("Get all tweets of a user");
        // Feed related functionality
        twitterService.getAllTweetsByUser(messi.getUserId());
        System.out.println("Get news feed of a user in chronological order");
        twitterService.getFeed(virat.getUserId(),10);
        System.out.println("Change feed generation strategy to relevance based");
        twitterService.setFeedGenerationStrategy(new EngagementBasedFeedGenerationStrategy());
        twitterService.getFeed(virat.getUserId(),10);

        System.out.println("Retweet a tweet");
        twitterService.retweetTweet(virat.getUserId(), twitterService.getAllTweetsByUser(messi.getUserId()).getFirst().getTweetId());
    }
}