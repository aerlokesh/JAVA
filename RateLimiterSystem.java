import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

interface RateLimiter{
    boolean allowRequest(String clientId);
    String getName();
}
class TokenBucketRateLimiter implements RateLimiter{
    private final int capacity;
    private final int refillRate;
    private ConcurrentHashMap<String, AtomicInteger> buckets;

    public TokenBucketRateLimiter(int capacity, int refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.buckets = new ConcurrentHashMap<>();
    }

    @Override
    public boolean allowRequest(String clientId) {
        AtomicInteger tokens = buckets.computeIfAbsent(clientId, k -> new AtomicInteger(capacity));
        while (true) {
            int currentTokens = tokens.get();
            if (currentTokens > 0) {
                if (tokens.compareAndSet(currentTokens, currentTokens - 1)) {
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    @Override
    public String getName() {
        return "TokenBucket";
    }
}
class WindowState{
    long windowStart;
    AtomicInteger count;
    WindowState(long windowStart){
        this.windowStart=windowStart;
        this.count=new AtomicInteger(0);
    }
}
class FixedWindowRateLimiter implements RateLimiter{
    private final int maxRequests;
    private final long windowSizeInMillis;
    private ConcurrentHashMap<String, WindowState> windows;

    public FixedWindowRateLimiter(int maxRequests, long windowSizeInMillis) {
        this.maxRequests = maxRequests;
        this.windowSizeInMillis = windowSizeInMillis;
        this.windows = new ConcurrentHashMap<>();
    }

    @Override
    public boolean allowRequest(String clientId) {
        long currentWindow = System.currentTimeMillis() / windowSizeInMillis;
        WindowState window = windows.computeIfAbsent(clientId, k -> new WindowState(currentWindow));
        if (currentWindow > window.windowStart) {
            window.count.set(0);
            window.windowStart = currentWindow;
        }
        return window.count.incrementAndGet() <= maxRequests;
    }

    @Override
    public String getName() {
        return "FixedWindow";
    }
}
class RateLimiterService{
    private final Map<String, RateLimiter> rateLimiters;
    private final RateLimiter defaultRateLimiter;
    public RateLimiterService(RateLimiter defaultRateLimiter){
        this.rateLimiters=new ConcurrentHashMap<>();
        this.defaultRateLimiter=defaultRateLimiter;
    }
    public void registerRateLimiter(String endpoint, RateLimiter rateLimiter){
        rateLimiters.put(endpoint,rateLimiter);
    }
    public boolean allowRequest(String endpoint, String clientId){
        RateLimiter rateLimiter=rateLimiters.getOrDefault(endpoint, defaultRateLimiter);
        boolean allowed= rateLimiter.allowRequest(clientId);
        System.out.println("Request from client "+clientId+" to endpoint "+endpoint+" is "+(allowed?"allowed":"denied")+" by "+rateLimiter.getName()+" limiter");
        return allowed;
    }
}
public class RateLimiterSystem {
    public static void main(String[] args) {
        System.out.println("Demo Rate Limiter System");
        RateLimiterService rateLimiterService=new RateLimiterService(new TokenBucketRateLimiter(5,1));
        rateLimiterService.registerRateLimiter("/api/v1/user", new FixedWindowRateLimiter(2, 1000));
        rateLimiterService.allowRequest("/api/v1/user", "client-1");
        rateLimiterService.allowRequest("/api/v1/user", "client-1");
        rateLimiterService.allowRequest("/api/v1/user", "client-1");
        rateLimiterService.allowRequest("/api/v1/user", "client-1");
        rateLimiterService.allowRequest("/api/v1/user", "client-1");
        rateLimiterService.allowRequest("/api/v1/user", "client-2");
        rateLimiterService.allowRequest("/api/v1/user", "client-2");
        rateLimiterService.allowRequest("/api/v1/user", "client-2");
        rateLimiterService.allowRequest("/api/v1/products", "client-1");
        rateLimiterService.allowRequest("/api/v1/products", "client-2");

        System.out.println("✅ Demo complete!");
    }
}
