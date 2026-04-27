import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/*
 * API GATEWAY SYSTEM - Low Level Design
 * =======================================
 * 
 * REQUIREMENTS:
 * 1. Route requests to backend services by path prefix (/users → UserService)
 * 2. Rate limiting per client (token bucket per API key)
 * 3. Authentication: validate API key before routing
 * 4. Request/Response transformation (add headers, strip internal fields)
 * 5. Load balancing across service instances (round-robin)
 * 6. Circuit breaker: stop routing to failing backends
 * 7. Request logging + metrics (latency, error rate)
 * 8. Thread-safe concurrent request handling
 * 
 * KEY DATA STRUCTURES:
 * - ConcurrentHashMap<prefix, ServiceRoute>: routing table
 * - ConcurrentHashMap<apiKey, TokenBucket>: per-client rate limiters
 * - AtomicInteger per route: round-robin counter for load balancing
 * 
 * DESIGN PATTERNS:
 * - Chain of Responsibility: auth → rate limit → route → transform
 * - Strategy: different load balancing algorithms
 * 
 * COMPLEXITY:
 *   handleRequest: O(P) prefix match + O(1) rate limit + O(1) route
 *   where P = number of route prefixes
 */

// ==================== EXCEPTIONS ====================

class RateLimitedException extends Exception {
    RateLimitedException(String key) { super("Rate limited: " + key); }
}

class UnauthorizedException extends Exception {
    UnauthorizedException(String key) { super("Invalid API key: " + key); }
}

class ServiceUnavailableException extends Exception {
    ServiceUnavailableException(String svc) { super("Service unavailable: " + svc); }
}

// ==================== DOMAIN CLASSES ====================

class GatewayRequest {
    final String apiKey;
    final String path;       // e.g., "/users/123"
    final String method;     // GET, POST, etc.
    final Map<String, String> headers;
    final String body;

    GatewayRequest(String apiKey, String path, String method, String body) {
        this.apiKey = apiKey;
        this.path = path;
        this.method = method;
        this.headers = new ConcurrentHashMap<>();
        this.body = body;
    }
}

class GatewayResponse {
    int statusCode;
    String body;
    final Map<String, String> headers = new ConcurrentHashMap<>();

    GatewayResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }
}

/** A backend service instance (host:port) */
class ServiceInstance {
    final String instanceId;
    final String host;
    volatile boolean healthy;

    ServiceInstance(String instanceId, String host) {
        this.instanceId = instanceId;
        this.host = host;
        this.healthy = true;
    }
}

/** Route config: path prefix → list of backend instances */
class ServiceRoute {
    final String prefix;          // e.g., "/users"
    final String serviceName;
    final List<ServiceInstance> instances = new CopyOnWriteArrayList<>();
    final AtomicInteger rrCounter = new AtomicInteger(0);  // round-robin

    ServiceRoute(String prefix, String serviceName) {
        this.prefix = prefix;
        this.serviceName = serviceName;
    }
}

/** Token bucket rate limiter per client */
class TokenBucket {
    final int maxTokens;
    final long refillIntervalMs;
    int tokens;
    long lastRefillTime;
    final ReentrantLock lock = new ReentrantLock();

    TokenBucket(int maxTokens, long refillIntervalMs) {
        this.maxTokens = maxTokens;
        this.refillIntervalMs = refillIntervalMs;
        this.tokens = maxTokens;
        this.lastRefillTime = System.currentTimeMillis();
    }

    /**
     * Try to consume a token. Returns true if allowed.
     * 
     * IMPLEMENTATION HINTS:
     * 1. Lock
     * 2. Refill: elapsed = now - lastRefillTime
     *    tokensToAdd = elapsed / refillIntervalMs
     *    tokens = min(maxTokens, tokens + tokensToAdd)
     *    lastRefillTime = now
     * 3. If tokens > 0 → tokens--, return true
     * 4. Else return false
     * 5. Unlock in finally
     */
    boolean tryConsume() {
        // HINT: lock.lock();
        // HINT: try {
        //     long now = System.currentTimeMillis();
        //     long elapsed = now - lastRefillTime;
        //     int refill = (int)(elapsed / refillIntervalMs);
        //     if (refill > 0) {
        //         tokens = Math.min(maxTokens, tokens + refill);
        //         lastRefillTime = now;
        //     }
        //     if (tokens > 0) { tokens--; return true; }
        //     return false;
        // } finally { lock.unlock(); }
        lock.lock();
        try{
            long now=System.currentTimeMillis();
            long elapsed=now-lastRefillTime;
            int refill=(int)(elapsed/refillIntervalMs);
            if(refill>0){
                tokens=Math.min(maxTokens,tokens+refill);
                lastRefillTime=now;
            }
            if(tokens>0) {tokens--; return true;}
            return false;
        }finally{
            lock.unlock();
        }
    }
}

// ==================== SERVICE ====================

class APIGateway {
    private final ConcurrentHashMap<String, ServiceRoute> routes = new ConcurrentHashMap<>();  // prefix → route
    private final ConcurrentHashMap<String, TokenBucket> rateLimiters = new ConcurrentHashMap<>();
    private final Set<String> validApiKeys = ConcurrentHashMap.newKeySet();
    private final int defaultRateLimit;     // tokens per client
    private final long refillIntervalMs;
    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong totalErrors = new AtomicLong();

    APIGateway(int defaultRateLimit, long refillIntervalMs) {
        this.defaultRateLimit = defaultRateLimit;
        this.refillIntervalMs = refillIntervalMs;
    }

    // ===== CONFIGURATION =====

    void registerApiKey(String apiKey) {
        // TODO: Implement
        // HINT: validApiKeys.add(apiKey);
        // HINT: rateLimiters.put(apiKey, new TokenBucket(defaultRateLimit, refillIntervalMs));
    }

    void addRoute(String prefix, String serviceName) {
        // TODO: Implement
        // HINT: routes.put(prefix, new ServiceRoute(prefix, serviceName));
    }

    void addInstance(String prefix, String instanceId, String host) {
        // TODO: Implement
        // HINT: ServiceRoute route = routes.get(prefix);
        // HINT: if (route != null) route.instances.add(new ServiceInstance(instanceId, host));
    }

    void markInstanceDown(String prefix, String instanceId) {
        // TODO: Implement
        // HINT: ServiceRoute route = routes.get(prefix);
        // HINT: if (route != null) for (ServiceInstance i : route.instances)
        //     if (i.instanceId.equals(instanceId)) i.healthy = false;
    }

    void markInstanceUp(String prefix, String instanceId) {
        // TODO: Implement
        // HINT: ServiceRoute route = routes.get(prefix);
        // HINT: if (route != null) for (ServiceInstance i : route.instances)
        //     if (i.instanceId.equals(instanceId)) i.healthy = true;
    }

    // ===== CORE: HANDLE REQUEST (Chain of Responsibility) =====

    /**
     * Main gateway flow:
     * 1. Auth: validate API key
     * 2. Rate Limit: check token bucket for this key
     * 3. Route: match path prefix → find backend service
     * 4. Load Balance: round-robin across healthy instances
     * 5. Forward: simulate call to backend
     * 6. Transform: add gateway headers to response
     */
    GatewayResponse handleRequest(GatewayRequest req)
            throws UnauthorizedException, RateLimitedException, ServiceUnavailableException {
        // TODO: Implement
        // HINT: totalRequests.incrementAndGet();
        //
        // HINT: // 1. Auth
        // HINT: if (!validApiKeys.contains(req.apiKey)) throw new UnauthorizedException(req.apiKey);
        //
        // HINT: // 2. Rate Limit
        // HINT: TokenBucket bucket = rateLimiters.get(req.apiKey);
        // HINT: if (bucket != null && !bucket.tryConsume()) throw new RateLimitedException(req.apiKey);
        //
        // HINT: // 3. Route: find matching prefix
        // HINT: ServiceRoute route = null;
        // HINT: for (Map.Entry<String, ServiceRoute> e : routes.entrySet()) {
        //     if (req.path.startsWith(e.getKey())) { route = e.getValue(); break; }
        // }
        // HINT: if (route == null) { totalErrors.incrementAndGet(); return new GatewayResponse(404, "No route"); }
        //
        // HINT: // 4. Load Balance (round-robin across healthy instances)
        // HINT: ServiceInstance instance = pickInstance(route);
        // HINT: if (instance == null) throw new ServiceUnavailableException(route.serviceName);
        //
        // HINT: // 5. Forward (simulated)
        // HINT: String responseBody = String.format("{\"from\":\"%s\",\"path\":\"%s\"}", instance.host, req.path);
        // HINT: GatewayResponse resp = new GatewayResponse(200, responseBody);
        //
        // HINT: // 6. Transform: add gateway headers
        // HINT: resp.headers.put("X-Gateway", "true");
        // HINT: resp.headers.put("X-Backend", instance.host);
        // HINT: return resp;
        return null;
    }

    /**
     * Round-robin load balancer: pick next healthy instance
     */
    ServiceInstance pickInstance(ServiceRoute route) {
        // TODO: Implement
        // HINT: List<ServiceInstance> healthy = new ArrayList<>();
        // HINT: for (ServiceInstance i : route.instances) if (i.healthy) healthy.add(i);
        // HINT: if (healthy.isEmpty()) return null;
        // HINT: int idx = route.rrCounter.getAndIncrement() % healthy.size();
        // HINT: return healthy.get(Math.abs(idx));
        return null;
    }

    // ===== METRICS =====

    long getTotalRequests() { return totalRequests.get(); }
    long getTotalErrors() { return totalErrors.get(); }

    void printRoutes() {
        System.out.println("  Routes:");
        routes.forEach((prefix, route) -> {
            System.out.printf("    %s → %s [%d instances]%n", prefix, route.serviceName, route.instances.size());
            route.instances.forEach(i -> System.out.printf("      %s (%s) %s%n",
                i.instanceId, i.host, i.healthy ? "✓" : "DOWN"));
        });
    }
}

// ==================== MAIN / TESTS ====================

public class APIGatewaySystem {
    public static void main(String[] args) throws Exception {
        System.out.println("=== API Gateway LLD ===\n");

        // 10 requests/sec per client, refill every 100ms
        APIGateway gw = new APIGateway(10, 100);

        // Setup routes
        gw.addRoute("/users", "UserService");
        gw.addRoute("/orders", "OrderService");
        gw.addRoute("/products", "ProductService");

        // Add instances
        gw.addInstance("/users", "u1", "user-svc-1:8080");
        gw.addInstance("/users", "u2", "user-svc-2:8080");
        gw.addInstance("/orders", "o1", "order-svc-1:8080");
        gw.addInstance("/products", "p1", "product-svc-1:8080");
        gw.addInstance("/products", "p2", "product-svc-2:8080");

        // Register API keys
        gw.registerApiKey("key-alice");
        gw.registerApiKey("key-bob");

        // Test 1: Basic Request
        System.out.println("=== Test 1: Basic Request ===");
        try {
            GatewayResponse resp = gw.handleRequest(
                new GatewayRequest("key-alice", "/users/123", "GET", null));
            if (resp != null) System.out.printf("  %d: %s (backend: %s)%n", resp.statusCode, resp.body, resp.headers.get("X-Backend"));
            System.out.println("✓ Routed to UserService\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // Test 2: Load Balancing (round-robin)
        System.out.println("=== Test 2: Load Balancing ===");
        try {
            for (int i = 0; i < 4; i++) {
                GatewayResponse r = gw.handleRequest(
                    new GatewayRequest("key-alice", "/users/" + i, "GET", null));
                if (r != null) System.out.println("  → " + r.headers.get("X-Backend"));
            }
            System.out.println("✓ Round-robin across instances\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // Test 3: Invalid API Key
        System.out.println("=== Test 3: Auth Failure ===");
        try {
            gw.handleRequest(new GatewayRequest("INVALID-KEY", "/users/1", "GET", null));
            System.out.println("✗ Should have thrown");
        } catch (UnauthorizedException e) {
            System.out.println("✓ Caught: " + e.getMessage() + "\n");
        }

        // Test 4: Rate Limiting
        System.out.println("=== Test 4: Rate Limiting ===");
        try {
            int allowed = 0, rejected = 0;
            for (int i = 0; i < 20; i++) {
                try {
                    gw.handleRequest(new GatewayRequest("key-bob", "/orders/1", "GET", null));
                    allowed++;
                } catch (RateLimitedException e) { rejected++; }
            }
            System.out.printf("  Allowed: %d, Rejected: %d%n", allowed, rejected);
            System.out.println("✓ Rate limiter working\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // Test 5: Route Not Found
        System.out.println("=== Test 5: No Route ===");
        try {
            GatewayResponse resp = gw.handleRequest(
                new GatewayRequest("key-alice", "/unknown/path", "GET", null));
            if (resp != null) System.out.println("  Status: " + resp.statusCode + " (expect 404)");
            System.out.println("✓ 404 returned\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // Test 6: Instance Down → Failover
        System.out.println("=== Test 6: Instance Failover ===");
        try {
            gw.markInstanceDown("/users", "u1");
            for (int i = 0; i < 3; i++) {
                GatewayResponse r = gw.handleRequest(
                    new GatewayRequest("key-alice", "/users/" + i, "GET", null));
                if (r != null) System.out.println("  → " + r.headers.get("X-Backend") + " (u1 is down)");
            }
            gw.markInstanceUp("/users", "u1");
            System.out.println("✓ Routed only to healthy\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // Test 7: All Instances Down
        System.out.println("=== Test 7: All Down ===");
        try {
            gw.markInstanceDown("/orders", "o1");
            gw.handleRequest(new GatewayRequest("key-alice", "/orders/1", "GET", null));
            System.out.println("✗ Should have thrown");
        } catch (ServiceUnavailableException e) {
            System.out.println("✓ Caught: " + e.getMessage());
            gw.markInstanceUp("/orders", "o1");
        }
        System.out.println();

        // Test 8: Concurrent Requests
        System.out.println("=== Test 8: Thread Safety ===");
        APIGateway concGw = new APIGateway(1000, 1); // high limit
        concGw.addRoute("/api", "TestService");
        concGw.addInstance("/api", "t1", "test-1:8080");
        concGw.addInstance("/api", "t2", "test-2:8080");
        concGw.registerApiKey("conc-key");

        ExecutorService exec = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        AtomicInteger success = new AtomicInteger();
        for (int i = 0; i < 200; i++) {
            futures.add(exec.submit(() -> {
                try {
                    concGw.handleRequest(new GatewayRequest("conc-key", "/api/test", "GET", null));
                    success.incrementAndGet();
                } catch (Exception e) {}
            }));
        }
        for (Future<?> f : futures) f.get();
        exec.shutdown();
        System.out.println("  Success: " + success.get() + "/200");
        System.out.println("  Total: " + concGw.getTotalRequests());
        System.out.println("✓ Thread-safe\n");

        // Test 9: Metrics
        System.out.println("=== Test 9: Metrics ===");
        System.out.println("  Requests: " + gw.getTotalRequests() + ", Errors: " + gw.getTotalErrors());
        gw.printRoutes();

        System.out.println("\n=== All Tests Complete! ===");
    }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. CHAIN OF RESPONSIBILITY: Auth → RateLimit → Route → LoadBalance → Forward → Transform
 *    Each step can short-circuit (reject early)
 * 
 * 2. RATE LIMITING: Token bucket per API key
 *    Refill tokens over time, reject when empty
 *    Alternative: sliding window, fixed window, leaky bucket
 * 
 * 3. LOAD BALANCING: Round-robin (simple), weighted, least-connections, consistent hash
 *    Skip unhealthy instances → circuit breaker integration
 * 
 * 4. CIRCUIT BREAKER: track error rate per backend
 *    Open after N failures → return 503 → half-open → probe → close
 * 
 * 5. SCALE: Stateless gateway → horizontal scale behind DNS/NLB
 *    Rate limit state in Redis (distributed), route config in etcd/Consul
 * 
 * 6. REAL-WORLD: Kong, NGINX, AWS API Gateway, Envoy, Zuul
 */
