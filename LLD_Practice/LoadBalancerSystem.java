import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

// ===== CUSTOM EXCEPTION CLASSES =====

/**
 * Exception thrown when server is not found
 * WHEN TO THROW:
 * - Server ID doesn't exist in the pool
 * - Operations on non-existent server
 */
class ServerNotFoundException extends Exception {
    private String serverId;
    
    public ServerNotFoundException(String serverId) {
        super("Server not found: " + serverId);
        this.serverId = serverId;
    }
    
    public String getServerId() { return serverId; }
}

/**
 * Exception thrown when no healthy servers available
 * WHEN TO THROW:
 * - All servers are unhealthy/down
 * - Cannot route request
 */
class NoHealthyServersException extends Exception {
    private int totalServers;
    
    public NoHealthyServersException(int totalServers) {
        super("No healthy servers available (Total: " + totalServers + ", Healthy: 0)");
        this.totalServers = totalServers;
    }
    
    public int getTotalServers() { return totalServers; }
}

/**
 * Exception thrown when server configuration is invalid
 * WHEN TO THROW:
 * - Null server object
 * - Invalid host/port
 * - Duplicate server ID
 */
class InvalidServerException extends Exception {
    public InvalidServerException(String message) {
        super(message);
    }
}

// ===== ENUMS =====

enum ServerStatus { 
    HEALTHY,   // Server is up and accepting requests
    UNHEALTHY  // Server is down or failing health checks
}

// ===== INTERFACE - STRATEGY PATTERN =====

/**
 * Strategy interface for load balancing algorithms
 */
interface LoadBalancingStrategy {
    Server selectServer(List<Server> servers);
    String getName();
}

// ===== DOMAIN CLASSES =====

/**
 * Represents a backend server
 */
class Server {
    String id;
    String host;
    int port;
    ServerStatus status;
    AtomicInteger activeConnections;
    AtomicInteger totalRequestsHandled;
    
    public Server(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.status = ServerStatus.HEALTHY;
        this.activeConnections = new AtomicInteger(0);
        this.totalRequestsHandled = new AtomicInteger(0);
    }
    
    @Override
    public String toString() {
        return id + "(" + host + ":" + port + ", conn=" + 
               activeConnections.get() + ", total=" + totalRequestsHandled.get() + 
               ", " + status + ")";
    }
}

/**
 * Load Balancer System - Low Level Design (LLD)
 * 
 * PROBLEM STATEMENT:
 * Design a load balancer that can:
 * 1. Distribute requests across multiple servers
 * 2. Support multiple algorithms (Round Robin, Least Connections, Random)
 * 3. Perform health checks and failover
 * 4. Handle concurrent requests safely
 * 5. Track server statistics
 * 
 * REQUIREMENTS:
 * - Functional: Add/remove servers, route requests, health checks
 * - Non-Functional: Thread-safe, low latency, high throughput
 * 
 * INTERVIEW HINTS:
 * - Discuss strategy pattern for different algorithms
 * - Talk about health check mechanisms
 * - Mention consistent hashing for distributed systems
 * - Consider sticky sessions and session affinity
 * - Discuss Layer 4 vs Layer 7 load balancing
 */

// ===== LOAD BALANCING STRATEGIES =====

/**
 * ROUND ROBIN STRATEGY
 * =====================
 * Rotates through servers in circular order
 * 
 * ALGORITHM:
 *   index = (index + 1) % servers.size()
 *   return servers[index]
 * 
 * PROS: 
 *   - Simple, fair distribution
 *   - No server starvation
 * CONS: 
 *   - Doesn't consider server load or capacity
 *   - Not optimal for heterogeneous servers
 * 
 * USE CASE: Homogeneous servers with similar capacity
 */
class RoundRobinStrategy implements LoadBalancingStrategy {
    private final AtomicInteger index = new AtomicInteger(0);
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Get current index and increment atomically
     * 2. Use modulo to wrap around: index % servers.size()
     * 3. Return server at that position
     * 4. Handle empty list (return null)
     * 
     * TIME COMPLEXITY: O(1)
     */
    @Override
    public Server selectServer(List<Server> servers) {
        // HINT: if (servers.isEmpty()) return null;
        // HINT: int idx = Math.abs(index.getAndIncrement() % servers.size());
        // HINT: return servers.get(idx);
        if(servers.isEmpty()) return null;
        int idx=Math.abs(index.getAndIncrement()%servers.size());
        return servers.get(idx);
    }
    
    @Override
    public String getName() { return "RoundRobin"; }
}

/**
 * LEAST CONNECTIONS STRATEGY
 * ===========================
 * Picks server with fewest active connections
 * 
 * ALGORITHM:
 *   for each server:
 *     track server with minimum activeConnections
 *   return server_with_min_connections
 * 
 * PROS: 
 *   - Better load distribution
 *   - Considers actual server load
 *   - Good for long-lived connections
 * CONS: 
 *   - Slightly more complex
 *   - Need to track active connections
 * 
 * USE CASE: WebSocket connections, database connections, varying request durations
 */
class LeastConnectionsStrategy implements LoadBalancingStrategy {
    /**
     * IMPLEMENTATION HINTS:
     * 1. Use stream to find server with minimum activeConnections
     * 2. Compare using activeConnections.get()
     * 3. Return server with lowest count
     * 4. Handle empty list
     * 
     * TIME COMPLEXITY: O(n)
     */
    @Override
    public Server selectServer(List<Server> servers) {
        // HINT: return servers.stream()
        //           .min(Comparator.comparingInt(s -> s.activeConnections.get()))
        //           .orElse(null);
        return servers.stream().min(Comparator.comparingInt(x->x.activeConnections.get())).orElse(null);
    }
    
    @Override
    public String getName() { return "LeastConnections"; }
}

/**
 * RANDOM STRATEGY
 * ================
 * Randomly selects a server
 * 
 * ALGORITHM:
 *   index = random(0, servers.size())
 *   return servers[index]
 * 
 * PROS: 
 *   - Simple implementation
 *   - Good distribution over time
 *   - No state to maintain
 * CONS: 
 *   - Can be uneven in short term
 *   - No fairness guarantee
 * 
 * USE CASE: Stateless HTTP requests, short-lived connections
 */
class RandomStrategy implements LoadBalancingStrategy {
    private final Random random = new Random();
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Generate random index: random.nextInt(servers.size())
     * 2. Return server at that index
     * 3. Handle empty list
     * 
     * TIME COMPLEXITY: O(1)
     */
    @Override
    public Server selectServer(List<Server> servers) {
        // HINT: if (servers.isEmpty()) return null;
        // HINT: return servers.get(random.nextInt(servers.size()));
        if(servers.isEmpty()) return null;
        return servers.get(random.nextInt(servers.size()));
    }
    
    @Override
    public String getName() { return "Random"; }
}

// ===== LOAD BALANCER SERVICE =====

/**
 * Main load balancer class managing server pool and routing
 */
class LoadBalancer {
    private List<Server> servers;
    private LoadBalancingStrategy strategy;
    private AtomicInteger totalRequests = new AtomicInteger(0);
    private AtomicInteger successfulRequests = new AtomicInteger(0);
    private AtomicInteger failedRequests = new AtomicInteger(0);
    
    public LoadBalancer(LoadBalancingStrategy strategy) {
        this.servers = new CopyOnWriteArrayList<>();  // Thread-safe list
        this.strategy = strategy;
    }
    
    /**
     * Add a server to the pool
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate server is not null
     * 2. Validate host is not empty
     * 3. Validate port is valid (1-65535)
     * 4. Check for duplicate server ID
     * 5. Add to servers list
     * 
     * @param server Server to add
     * @throws InvalidServerException if server is invalid or duplicate
     */
    public void addServer(Server server) throws InvalidServerException {
        // HINT: if (server == null) throw new InvalidServerException("Server cannot be null");
        // HINT: if (server.host == null || server.host.isEmpty()) throw new InvalidServerException(...)
        // HINT: if (server.port < 1 || server.port > 65535) throw new InvalidServerException(...)
        // HINT: Check duplicate: servers.stream().anyMatch(s -> s.id.equals(server.id))
        // HINT: servers.add(server);
        if(server==null) throw new InvalidServerException("Server cannot be null");
        if(server.host == null || server.host.isEmpty())  throw new InvalidServerException("Host cannot be null");
        if(server.port<0 || server.port>65535)  throw new InvalidServerException("Incorrect port");
        if(servers.stream().anyMatch(x->x.id.equals(server.id))) throw new InvalidServerException("server id exists");
        servers.add(server);
    }
    
    /**
     * Remove a server from the pool
     * 
     * IMPLEMENTATION HINTS:
     * 1. Find server by ID
     * 2. Remove from list
     * 3. Throw exception if not found
     * 
     * @param serverId Server ID to remove
     * @throws ServerNotFoundException if server not found
     */
    public void removeServer(String serverId) throws ServerNotFoundException {
        // HINT: boolean removed = servers.removeIf(s -> s.id.equals(serverId));
        // HINT: if (!removed) throw new ServerNotFoundException(serverId);
        boolean removed=servers.removeIf(x->x.id.equals(serverId));
        if(!removed) throw new ServerNotFoundException(serverId);
    }
    
    /**
     * Change load balancing strategy at runtime
     * 
     * @param strategy New strategy to use
     */
    public void setStrategy(LoadBalancingStrategy strategy) {
        // HINT: this.strategy = strategy;
        // HINT: System.out.println("Strategy changed to: " + strategy.getName());
        this.strategy=strategy;
        System.out.println("Strategy changed to: " + strategy.getName());
    }
    
    /**
     * Route a request to a server
     * 
     * IMPLEMENTATION HINTS:
     * 1. Filter for healthy servers only
     * 2. If no healthy servers, throw NoHealthyServersException
     * 3. Use strategy to select server
     * 4. Increment server's activeConnections
     * 5. Increment server's totalRequestsHandled
     * 6. Increment totalRequests counter
     * 7. Return selected server
     * 
     * INTERVIEW DISCUSSION:
     * - How to handle server failure mid-request?
     * - Should we retry on different server?
     * - How to implement circuit breaker?
     * 
     * @param requestId Request identifier for logging
     * @return Selected server for the request
     * @throws NoHealthyServersException if no healthy servers available
     */
    public synchronized Server routeRequest(String requestId) throws NoHealthyServersException {
        // HINT: List<Server> healthy = servers.stream()
        //           .filter(s -> s.status == ServerStatus.HEALTHY)
        //           .collect(Collectors.toList());
        // HINT: if (healthy.isEmpty()) throw new NoHealthyServersException(servers.size());
        // HINT: Server selected = strategy.selectServer(healthy);
        // HINT: selected.activeConnections.incrementAndGet();
        // HINT: selected.totalRequestsHandled.incrementAndGet();
        // HINT: totalRequests.incrementAndGet();
        // HINT: return selected;
        List<Server> hList=servers.stream().filter(x->x.status==ServerStatus.HEALTHY).collect(Collectors.toList());
        if(hList.isEmpty()) throw new NoHealthyServersException(servers.size());
        Server selected=strategy.selectServer(hList);
        selected.activeConnections.incrementAndGet();
        selected.totalRequestsHandled.incrementAndGet();
        totalRequests.incrementAndGet();
        return selected;
    }
    
    /**
     * Complete a request - release connection
     * 
     * IMPLEMENTATION HINTS:
     * 1. Decrement server's activeConnections
     * 2. Handle null server gracefully
     * 3. Increment successfulRequests counter
     * 
     * @param server Server that handled the request
     */
    public void completeRequest(Server server) {
        // HINT: if (server != null) {
        //     server.activeConnections.decrementAndGet();
        //     successfulRequests.incrementAndGet();
        // }
        if(server!=null){
            server.activeConnections.decrementAndGet();
            successfulRequests.incrementAndGet();
        }
    }
    
    /**
     * Mark request as failed
     * 
     * @param server Server that failed
     */
    public void failRequest(Server server) {
        // HINT: if (server != null) server.activeConnections.decrementAndGet();
        // HINT: failedRequests.incrementAndGet();
        if(server!=null) server.activeConnections.decrementAndGet();
        failedRequests.incrementAndGet();
    }
    
    /**
     * Mark server as unhealthy
     * 
     * IMPLEMENTATION HINTS:
     * 1. Find server by ID
     * 2. Set status to UNHEALTHY
     * 3. Throw exception if server not found
     * 
     * @param serverId Server ID to mark unhealthy
     * @throws ServerNotFoundException if server not found
     */
    public void markUnhealthy(String serverId) throws ServerNotFoundException {
        // HINT: Server server = servers.stream()
        //           .filter(s -> s.id.equals(serverId))
        //           .findFirst()
        //           .orElseThrow(() -> new ServerNotFoundException(serverId));
        // HINT: server.status = ServerStatus.UNHEALTHY;
        Server server=servers.stream().filter(x->x.id.equals(serverId)).findFirst().orElseThrow(()->new ServerNotFoundException(serverId));
        server.status=ServerStatus.UNHEALTHY;
    }
    
    /**
     * Mark server as healthy
     * 
     * IMPLEMENTATION HINTS:
     * 1. Find server by ID
     * 2. Set status to HEALTHY
     * 3. Throw exception if server not found
     * 
     * @param serverId Server ID to mark healthy
     * @throws ServerNotFoundException if server not found
     */
    public void markHealthy(String serverId) throws ServerNotFoundException {
        // HINT: Similar to markUnhealthy but set status to HEALTHY
        Server server=servers.stream().filter(x->x.id.equals(serverId)).findFirst().orElseThrow(()->new ServerNotFoundException(serverId));
        server.status=ServerStatus.HEALTHY;
    }
    
    /**
     * Get server by ID
     * 
     * @param serverId Server ID
     * @return Server object
     * @throws ServerNotFoundException if not found
     */
    public Server getServer(String serverId) throws ServerNotFoundException {
        return servers.stream().filter(x->x.id.equals(serverId)).findFirst().orElseThrow(()->new ServerNotFoundException(serverId));
    }
    
    /**
     * Get all healthy servers
     * 
     * @return List of healthy servers
     */
    public List<Server> getHealthyServers() {
        return servers.stream().filter(x->x.status==ServerStatus.HEALTHY).collect(Collectors.toList());
    }
    
    /**
     * Display load balancer status
     */
    public void displayStatus() {
        System.out.println("\n--- Load Balancer Status [" + strategy.getName() + "] ---");
        servers.forEach(s -> System.out.println("  " + s));
        long healthy = servers.stream().filter(s -> s.status == ServerStatus.HEALTHY).count();
        System.out.println("Servers: " + servers.size() + " (Healthy: " + healthy + ")");
        System.out.println("Requests: " + totalRequests.get() + " total, " + 
                         successfulRequests.get() + " success, " + 
                         failedRequests.get() + " failed");
    }
}

// ===== MAIN TEST CLASS =====

public class LoadBalancerSystem {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Load Balancer System Test Cases ===\n");
        
        LoadBalancer lb = new LoadBalancer(new RoundRobinStrategy());
        
        // Test Case 1: Add Servers
        System.out.println("=== Test Case 1: Add Servers ===");
        try {
            lb.addServer(new Server("server-1", "192.168.1.1", 8080));
            lb.addServer(new Server("server-2", "192.168.1.2", 8080));
            lb.addServer(new Server("server-3", "192.168.1.3", 8080));
            System.out.println("✓ Added 3 servers successfully");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        lb.displayStatus();
        System.out.println();
        
        // Test Case 2: Round Robin Distribution
        System.out.println("=== Test Case 2: ROUND ROBIN (9 requests) ===");
        try {
            Map<String, Integer> distribution = new HashMap<>();
            for (int i = 0; i < 9; i++) {
                Server s = lb.routeRequest("req-" + i);
                if (s != null) {
                    distribution.put(s.id, distribution.getOrDefault(s.id, 0) + 1);
                    lb.completeRequest(s);
                }
            }
            System.out.println("Distribution: " + distribution);
            System.out.println("✓ Each server got 3 requests: " + 
                             (distribution.values().stream().allMatch(c -> c == 3)));
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 3: Least Connections Strategy
        System.out.println("=== Test Case 3: LEAST CONNECTIONS ===");
        try {
            lb.setStrategy(new LeastConnectionsStrategy());
            
            // Create load imbalance
            Server s1 = lb.routeRequest("lc-1");
            Server s2 = lb.routeRequest("lc-2");
            lb.completeRequest(s1); // s1 done, s2 still active
            
            // Next request should go to s1 (fewer connections)
            Server s3 = lb.routeRequest("lc-3");
            System.out.println("✓ Server with least connections selected");
            
            lb.completeRequest(s2);
            lb.completeRequest(s3);
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 4: Random Strategy
        System.out.println("=== Test Case 4: RANDOM STRATEGY ===");
        try {
            lb.setStrategy(new RandomStrategy());
            Set<String> serversUsed = new HashSet<>();
            
            for (int i = 0; i < 20; i++) {
                Server s = lb.routeRequest("random-" + i);
                if (s != null) {
                    serversUsed.add(s.id);
                    lb.completeRequest(s);
                }
            }
            System.out.println("Servers used (out of 3): " + serversUsed.size());
            System.out.println("✓ Random distribution working: " + (serversUsed.size() >= 2));
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 5: Health Check - Mark Unhealthy
        System.out.println("=== Test Case 5: HEALTH CHECK (mark unhealthy) ===");
        try {
            lb.setStrategy(new RoundRobinStrategy());
            lb.markUnhealthy("server-2");
            
            Set<String> routedTo = new HashSet<>();
            for (int i = 0; i < 6; i++) {
                Server s = lb.routeRequest("health-req-" + i);
                if (s != null) {
                    routedTo.add(s.id);
                    lb.completeRequest(s);
                }
            }
            System.out.println("Routed to servers: " + routedTo);
            System.out.println("✓ server-2 skipped: " + !routedTo.contains("server-2"));
            
            lb.markHealthy("server-2"); // Restore
            System.out.println("✓ server-2 marked healthy again");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        lb.displayStatus();
        
        // ===== EXCEPTION TEST CASES =====
        
        // Test Case 6: Exception - All Servers Down
        System.out.println("\n=== Test Case 6: Exception - All Servers Down ===");
        try {
            lb.markUnhealthy("server-1");
            lb.markUnhealthy("server-2");
            lb.markUnhealthy("server-3");
            
            Server result = lb.routeRequest("fail-req");
            System.out.println("✗ Should have thrown NoHealthyServersException");
        } catch (NoHealthyServersException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
            System.out.println("  Total servers: " + e.getTotalServers());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Recover all servers
        try {
            lb.markHealthy("server-1");
            lb.markHealthy("server-2");
            lb.markHealthy("server-3");
        } catch (Exception e) {}
        
        // Test Case 7: Exception - Server Not Found
        System.out.println("=== Test Case 7: Exception - Server Not Found ===");
        try {
            lb.markHealthy("non-existent-server");
            System.out.println("✗ Should have thrown ServerNotFoundException");
        } catch (ServerNotFoundException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
            System.out.println("  Missing server ID: " + e.getServerId());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 8: Exception - Duplicate Server
        System.out.println("=== Test Case 8: Exception - Duplicate Server ===");
        try {
            lb.addServer(new Server("server-1", "192.168.1.10", 9000));
            System.out.println("✗ Should have thrown InvalidServerException");
        } catch (InvalidServerException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 9: Exception - Invalid Server Config
        System.out.println("=== Test Case 9: Exception - Invalid Port ===");
        try {
            lb.addServer(new Server("server-99", "192.168.1.99", 99999)); // Invalid port
            System.out.println("✗ Should have thrown InvalidServerException");
        } catch (InvalidServerException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        lb.displayStatus();
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION TOPICS:
 * ============================
 * 
 * 1. LOAD BALANCING ALGORITHMS:
 *    Round Robin:
 *      - Simple, fair distribution
 *      - Good for: Homogeneous servers, stateless requests
 *    
 *    Least Connections:
 *      - Better for varying request durations
 *      - Good for: Long-lived connections (WebSocket, DB)
 *    
 *    Weighted Round Robin:
 *      - Assign weights based on server capacity
 *      - Good for: Heterogeneous servers (different specs)
 *    
 *    Least Response Time:
 *      - Consider both connections and latency
 *      - Good for: Performance-critical applications
 *    
 *    Consistent Hashing:
 *      - For distributed caching with sticky sessions
 *      - Good for: Session affinity, cache locality
 *    
 *    IP Hash:
 *      - Same client always goes to same server
 *      - Good for: Session persistence without external storage
 * 
 * 2. HEALTH CHECK MECHANISMS:
 *    Active Health Checks:
 *      - Periodic probes (HTTP GET /health, TCP handshake)
 *      - Configurable interval and timeout
 *      - Mark unhealthy after N consecutive failures
 *    
 *    Passive Health Checks:
 *      - Monitor actual request failures
 *      - Circuit breaker pattern
 *      - Automatic recovery after cool-down
 *    
 *    Implementation:
 *      - Background thread for periodic checks
 *      - Exponential backoff for failed servers
 * 
 * 3. THREAD SAFETY:
 *    - synchronized on routeRequest (critical section)
 *    - CopyOnWriteArrayList for server list (read-heavy)
 *    - AtomicInteger for counters (thread-safe increments)
 *    - Consider lock-free algorithms for higher throughput
 * 
 * 4. ADVANCED FEATURES:
 *    - Sticky sessions (session affinity using cookies)
 *    - Geographic routing (route to nearest datacenter)
 *    - Priority queuing (premium users get priority)
 *    - Auto-scaling integration (add/remove servers dynamically)
 *    - Metrics and monitoring (Prometheus, Grafana)
 *    - Connection draining (graceful shutdown)
 *    - SSL termination
 *    - DDoS protection
 * 
 * 5. LAYER 4 VS LAYER 7 LOAD BALANCING:
 *    Layer 4 (Transport Layer):
 *      - Operates on IP + Port
 *      - Faster (no packet inspection)
 *      - Cannot route based on content
 *      - Examples: TCP/UDP load balancing
 *    
 *    Layer 7 (Application Layer):
 *      - Operates on HTTP/HTTPS
 *      - Content-based routing (URL, headers)
 *      - SSL termination
 *      - More flexible but slower
 *      - Examples: Nginx, HAProxy, ALB
 * 
 * 6. SCALABILITY & ARCHITECTURE:
 *    Single Load Balancer:
 *      - Simple but single point of failure
 *      - Use keepalived/VRRP for HA
 *    
 *    Multiple Load Balancers:
 *      - DNS round-robin to multiple LBs
 *      - Anycast routing for global distribution
 *    
 *    Hardware vs Software:
 *      - Hardware: F5, Citrix (expensive, high performance)
 *      - Software: Nginx, HAProxy, Envoy (flexible, cost-effective)
 *    
 *    Cloud Load Balancers:
 *      - AWS ELB/ALB/NLB
 *      - GCP Cloud Load Balancing
 *      - Azure Load Balancer
 * 
 * 7. DESIGN PATTERNS:
 *    - Strategy Pattern: Pluggable algorithms
 *    - Observer Pattern: Health check notifications
 *    - Circuit Breaker: Prevent cascade failures
 *    - Bulkhead: Isolate resources
 * 
 * 8. METRICS TO TRACK:
 *    - Requests per second (RPS)
 *    - Server utilization %
 *    - Average response time
 *    - Error rate
 *    - Connection pool saturation
 *    - Health check success rate
 * 
 * 9. API DESIGN:
 *    POST /servers              - Add server
 *    DELETE /servers/{id}       - Remove server
 *    PUT /servers/{id}/health   - Update health status
 *    GET /servers               - List all servers
 *    GET /servers/healthy       - List healthy servers
 *    GET /stats                 - Get metrics
 *    POST /route                - Route request (internal)
 */
