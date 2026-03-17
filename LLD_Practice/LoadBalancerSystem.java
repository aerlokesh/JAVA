import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// ===== CUSTOM EXCEPTION CLASSES =====

/**
 * Exception thrown when server is not found
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
 */
class NoHealthyServersException extends Exception {
    public NoHealthyServersException() {
        super("No healthy servers available to handle request");
    }
}

/**
 * Exception thrown when server configuration is invalid
 */
class InvalidServerException extends Exception {
    public InvalidServerException(String message) {
        super(message);
    }
}

// ===== ENUMS =====

enum ServerStatus { 
    HEALTHY, 
    UNHEALTHY 
}

// ===== INTERFACE - STRATEGY PATTERN =====

interface LoadBalancingStrategy {
    Server selectServer(List<Server> servers);
    String getName();
}

// ===== DOMAIN CLASSES =====

class Server {
    String id;
    String host;
    int port;
    ServerStatus status;
    AtomicInteger activeConnections;
    
    public Server(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.status = ServerStatus.HEALTHY;
        this.activeConnections = new AtomicInteger(0);
    }
    
    @Override
    public String toString() {
        return id + "(" + host + ":" + port + ", conn=" + 
               activeConnections.get() + ", " + status + ")";
    }
}

/**
 * Load Balancer System - Low Level Design
 * 
 * PROBLEM STATEMENT:
 * Design a load balancer that can:
 * 1. Distribute requests across multiple servers
 * 2. Support multiple algorithms (Round Robin, Least Connections, Random)
 * 3. Perform health checks and failover
 * 4. Handle concurrent requests safely
 * 5. Track server statistics
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
 * Rotates through servers in order
 * 
 * PROS: Simple, fair distribution
 * CONS: Doesn't consider server load
 */
class RoundRobinStrategy implements LoadBalancingStrategy {
    private final AtomicInteger index = new AtomicInteger(0);
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Get current index and increment atomically
     * 2. Use modulo to wrap around: index % servers.size()
     * 3. Return server at that position
     */
    @Override
    public Server selectServer(List<Server> servers) {
        // TODO: Implement
        // HINT: return servers.get(Math.abs(index.getAndIncrement()) % servers.size());
        return null;
    }
    
    @Override
    public String getName() { return "RoundRobin"; }
}

/**
 * LEAST CONNECTIONS STRATEGY
 * ===========================
 * Picks server with fewest active connections
 * 
 * PROS: Better load distribution, considers actual load
 * CONS: Slightly more complex
 */
class LeastConnectionsStrategy implements LoadBalancingStrategy {
    /**
     * IMPLEMENTATION HINTS:
     * 1. Use stream to find server with minimum activeConnections
     * 2. Return server with lowest count
     * 3. Handle empty list
     */
    @Override
    public Server selectServer(List<Server> servers) {
        // TODO: Implement
        // HINT: servers.stream().min(Comparator.comparingInt(s -> s.activeConnections.get()))
        return null;
    }
    
    @Override
    public String getName() { return "LeastConnections"; }
}

/**
 * RANDOM STRATEGY
 * ================
 * Randomly selects a server
 * 
 * PROS: Simple, good distribution over time
 * CONS: Can be uneven in short term
 */
class RandomStrategy implements LoadBalancingStrategy {
    private final Random random = new Random();
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Generate random index: random.nextInt(servers.size())
     * 2. Return server at that index
     */
    @Override
    public Server selectServer(List<Server> servers) {
        // TODO: Implement
        return null;
    }
    
    @Override
    public String getName() { return "Random"; }
}

// ===== LOAD BALANCER SERVICE =====

class LoadBalancer {
    private List<Server> servers;
    private LoadBalancingStrategy strategy;
    private AtomicInteger totalRequests = new AtomicInteger(0);
    
    public LoadBalancer(LoadBalancingStrategy strategy) {
        this.servers = new CopyOnWriteArrayList<>();
        this.strategy = strategy;
    }
    
    /**
     * Add a server to the pool
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate server is not null
     * 2. Check for duplicate server ID
     * 3. Add to servers list
     */
    public void addServer(Server server) throws InvalidServerException {
        // TODO: Implement
        // HINT: Check if server with same ID exists
        // HINT: servers.add(server);
    }
    
    /**
     * Remove a server from the pool
     * 
     * IMPLEMENTATION HINTS:
     * 1. Find server by ID
     * 2. Remove from list
     * 3. Throw exception if not found
     */
    public void removeServer(String serverId) throws ServerNotFoundException {
        // TODO: Implement
        // HINT: boolean removed = servers.removeIf(s -> s.id.equals(serverId));
        // HINT: if (!removed) throw new ServerNotFoundException(serverId);
    }
    
    /**
     * Change load balancing strategy
     */
    public void setStrategy(LoadBalancingStrategy strategy) {
        this.strategy = strategy;
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
     * 5. Increment totalRequests
     * 6. Return selected server
     */
    public synchronized Server routeRequest(String requestId) throws NoHealthyServersException {
        // TODO: Implement
        // HINT: List<Server> healthy = servers.stream().filter(s -> s.status == ServerStatus.HEALTHY).toList();
        // HINT: if (healthy.isEmpty()) throw new NoHealthyServersException();
        // HINT: Server selected = strategy.selectServer(healthy);
        return null;
    }
    
    /**
     * Complete a request - release connection
     * 
     * IMPLEMENTATION HINTS:
     * 1. Decrement server's activeConnections
     * 2. Handle null server gracefully
     */
    public void completeRequest(Server server) {
        // TODO: Implement
        // HINT: if (server != null) server.activeConnections.decrementAndGet();
    }
    
    /**
     * Mark server as unhealthy
     * 
     * IMPLEMENTATION HINTS:
     * 1. Find server by ID
     * 2. Set status to UNHEALTHY
     * 3. Throw exception if server not found
     */
    public void markUnhealthy(String serverId) throws ServerNotFoundException {
        // TODO: Implement
        // HINT: servers.stream().filter(s -> s.id.equals(serverId)).findFirst()
    }
    
    /**
     * Mark server as healthy
     */
    public void markHealthy(String serverId) throws ServerNotFoundException {
        // TODO: Implement
    }
    
    /**
     * Display load balancer status
     */
    public void displayStatus() {
        System.out.println("\n--- Load Balancer Status [" + strategy.getName() + "] ---");
        servers.forEach(s -> System.out.println("  " + s));
        long healthy = servers.stream().filter(s -> s.status == ServerStatus.HEALTHY).count();
        System.out.println("Servers: " + servers.size() + " (Healthy: " + healthy + 
                         ") | Total requests: " + totalRequests.get());
    }
}

// ===== MAIN TEST CLASS =====

public class LoadBalancerSystem {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Load Balancer System Test Cases ===\n");
        
        LoadBalancer lb = new LoadBalancer(new RoundRobinStrategy());
        
        // Test 1: Add Servers
        System.out.println("=== Test 1: Add Servers ===");
        try {
            lb.addServer(new Server("server-1", "192.168.1.1", 8080));
            lb.addServer(new Server("server-2", "192.168.1.2", 8080));
            lb.addServer(new Server("server-3", "192.168.1.3", 8080));
            System.out.println("✓ Added 3 servers");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        lb.displayStatus();
        System.out.println();
        
        // Test 2: Round Robin
        System.out.println("=== Test 2: ROUND ROBIN (6 requests) ===");
        try {
            for (int i = 0; i < 6; i++) {
                Server s = lb.routeRequest("req-" + i);
                lb.completeRequest(s);
            }
            System.out.println("✓ Round robin distribution");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test 3: Least Connections
        System.out.println("=== Test 3: LEAST CONNECTIONS ===");
        try {
            lb.setStrategy(new LeastConnectionsStrategy());
            Server s = lb.routeRequest("req-lc");
            System.out.println("✓ Selected server: " + (s != null ? s.id : "null"));
            lb.completeRequest(s);
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test 4: Health Check
        System.out.println("=== Test 4: HEALTH CHECK (mark server unhealthy) ===");
        try {
            lb.setStrategy(new RoundRobinStrategy());
            lb.markUnhealthy("server-2");
            
            Set<String> routedTo = new HashSet<>();
            for (int i = 0; i < 4; i++) {
                Server s = lb.routeRequest("health-req-" + i);
                if (s != null) {
                    routedTo.add(s.id);
                    lb.completeRequest(s);
                }
            }
            System.out.println("Routed to: " + routedTo);
            System.out.println("✓ server-2 skipped: " + !routedTo.contains("server-2"));
            
            lb.markHealthy("server-2");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test 5: All Servers Down
        System.out.println("=== Test 5: ALL SERVERS DOWN ===");
        try {
            lb.markUnhealthy("server-1");
            lb.markUnhealthy("server-2");
            lb.markUnhealthy("server-3");
            Server result = lb.routeRequest("fail-req");
            System.out.println("✗ Should have thrown NoHealthyServersException");
        } catch (NoHealthyServersException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Recover servers
        try {
            lb.markHealthy("server-1");
            lb.markHealthy("server-2");
            lb.markHealthy("server-3");
        } catch (Exception e) {}
        
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
 *      - Doesn't consider server capacity
 *      - Good for homogeneous servers
 *    
 *    Least Connections:
 *      - Better for varying request durations
 *      - Considers actual load
 *      - Good for long-lived connections
 *    
 *    Weighted Round Robin:
 *      - Assign weights based on capacity
 *      - Better for heterogeneous servers
 *    
 *    Consistent Hashing:
 *      - For distributed caching
 *      - Minimal redistribution on server changes
 * 
 * 2. HEALTH CHECK MECHANISMS:
 *    - Active: Send periodic probes (HTTP/TCP)
 *    - Passive: Monitor request failures
 *    - Timeout-based: Mark unhealthy after N failures
 *    - Circuit breaker pattern
 * 
 * 3. THREAD SAFETY:
 *    - Synchronized routeRequest to prevent races
 *    - CopyOnWriteArrayList for server list
 *    - AtomicInteger for counters
 * 
 * 4. ADVANCED FEATURES:
 *    - Sticky sessions (session affinity)
 *    - Geographic routing
 *    - Priority queuing
 *    - Auto-scaling integration
 *    - Metrics and monitoring
 * 
 * 5. LAYER 4 VS LAYER 7:
 *    - L4: TCP/UDP level (faster, simple)
 *    - L7: HTTP level (content-based routing, SSL termination)
 * 
 * 6. SCALABILITY:
 *    - DNS-based load balancing
 *    - Hardware vs software load balancers
 *    - Anycast for global distribution
 *    - CDN integration
 */
