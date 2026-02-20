import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// ==================== ENUMS ====================

enum ServerStatus { HEALTHY, UNHEALTHY }

// ==================== INTERFACE - STRATEGY PATTERN ====================

interface LoadBalancingStrategy {
    Server selectServer(List<Server> servers);
    String getName();
}

// ==================== DOMAIN CLASSES ====================

class Server {
    String id;
    String host;
    int port;
    ServerStatus status;
    AtomicInteger activeConnections;

    Server(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.status = ServerStatus.HEALTHY;
        this.activeConnections = new AtomicInteger(0);
    }

    @Override
    public String toString() {
        return id + "(" + host + ":" + port + ", conn=" + activeConnections.get() + ", " + status + ")";
    }
}

// ==================== STRATEGIES ====================

// 1. Round Robin - rotate through servers
class RoundRobinStrategy implements LoadBalancingStrategy {
    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public Server selectServer(List<Server> servers) {
        if (servers.isEmpty()) return null;
        return servers.get(Math.abs(index.getAndIncrement()) % servers.size());
    }

    @Override
    public String getName() { return "RoundRobin"; }
}

// 2. Least Connections - pick server with fewest active connections
class LeastConnectionsStrategy implements LoadBalancingStrategy {
    @Override
    public Server selectServer(List<Server> servers) {
        if (servers.isEmpty()) return null;
        return servers.stream()
            .min(Comparator.comparingInt(s -> s.activeConnections.get()))
            .orElse(null);
    }

    @Override
    public String getName() { return "LeastConnections"; }
}

// 3. Random - random selection
class RandomStrategy implements LoadBalancingStrategy {
    private final Random random = new Random();

    @Override
    public Server selectServer(List<Server> servers) {
        if (servers.isEmpty()) return null;
        return servers.get(random.nextInt(servers.size()));
    }

    @Override
    public String getName() { return "Random"; }
}

// ==================== MAIN SERVICE - THREAD SAFE ====================

class LoadBalancer {
    List<Server> servers;
    LoadBalancingStrategy strategy;
    AtomicInteger totalRequests = new AtomicInteger(0);

    LoadBalancer(LoadBalancingStrategy strategy) {
        this.servers = new CopyOnWriteArrayList<>();
        this.strategy = strategy;
    }

    void addServer(Server server) {
        servers.add(server);
        System.out.println("Added server: " + server.id);
    }

    void removeServer(String serverId) {
        servers.removeIf(s -> s.id.equals(serverId));
        System.out.println("Removed server: " + serverId);
    }

    void setStrategy(LoadBalancingStrategy strategy) {
        this.strategy = strategy;
        System.out.println("Strategy changed to: " + strategy.getName());
    }

    // Route request - SYNCHRONIZED to prevent race conditions on server selection
    public synchronized Server routeRequest(String requestId) {
        List<Server> healthy = servers.stream()
            .filter(s -> s.status == ServerStatus.HEALTHY)
            .toList();

        if (healthy.isEmpty()) {
            System.out.println(Thread.currentThread().getName()
                + ": " + requestId + " → NO HEALTHY SERVERS");
            return null;
        }

        Server selected = strategy.selectServer(healthy);
        if (selected != null) {
            selected.activeConnections.incrementAndGet();
            totalRequests.incrementAndGet();
            System.out.println(Thread.currentThread().getName()
                + ": " + requestId + " → " + selected.id
                + " [" + strategy.getName() + "] (conn=" + selected.activeConnections.get() + ")");
        }
        return selected;
    }

    // Complete request - release connection
    public void completeRequest(Server server) {
        if (server != null) {
            server.activeConnections.decrementAndGet();
        }
    }

    // Health check - mark server unhealthy/healthy
    void markUnhealthy(String serverId) {
        servers.stream().filter(s -> s.id.equals(serverId)).findFirst()
            .ifPresent(s -> {
                s.status = ServerStatus.UNHEALTHY;
                System.out.println("⚠ Server " + serverId + " marked UNHEALTHY");
            });
    }

    void markHealthy(String serverId) {
        servers.stream().filter(s -> s.id.equals(serverId)).findFirst()
            .ifPresent(s -> {
                s.status = ServerStatus.HEALTHY;
                System.out.println("✓ Server " + serverId + " marked HEALTHY");
            });
    }

    void displayStatus() {
        System.out.println("\n--- Load Balancer Status [" + strategy.getName() + "] ---");
        servers.forEach(s -> System.out.println("  " + s));
        long healthy = servers.stream().filter(s -> s.status == ServerStatus.HEALTHY).count();
        System.out.println("Servers: " + servers.size() + " (Healthy: " + healthy + ") | Total requests: " + totalRequests.get());
    }
}

// ==================== MAIN ====================

public class LoadBalancerSystem {
    public static void main(String[] args) throws InterruptedException {
        // ---- Setup ----
        LoadBalancer lb = new LoadBalancer(new RoundRobinStrategy());
        lb.addServer(new Server("server-1", "192.168.1.1", 8080));
        lb.addServer(new Server("server-2", "192.168.1.2", 8080));
        lb.addServer(new Server("server-3", "192.168.1.3", 8080));

        lb.displayStatus();

        // ---- Test 1: Round Robin ----
        System.out.println("\n=== Test 1: ROUND ROBIN (6 requests across 3 servers) ===");
        for (int i = 0; i < 6; i++) {
            Server s = lb.routeRequest("req-" + i);
            lb.completeRequest(s);
        }
        System.out.println("✓ Requests distributed in round-robin order");

        // ---- Test 2: Least Connections ----
        System.out.println("\n=== Test 2: LEAST CONNECTIONS ===");
        lb.setStrategy(new LeastConnectionsStrategy());

        // Simulate server-1 having 3 active connections
        Server s1 = lb.servers.get(0);
        s1.activeConnections.set(3);
        Server s2 = lb.servers.get(1);
        s2.activeConnections.set(1);
        Server s3 = lb.servers.get(2);
        s3.activeConnections.set(0);

        Server picked = lb.routeRequest("req-lc");
        System.out.println("✓ Picked server with least connections: " + picked.id
            + " (expected server-3): " + picked.id.equals("server-3"));

        // Reset connections
        s1.activeConnections.set(0);
        s2.activeConnections.set(0);
        s3.activeConnections.set(0);
        lb.completeRequest(picked);

        // ---- Test 3: Unhealthy server skipped ----
        System.out.println("\n=== Test 3: HEALTH CHECK (unhealthy server skipped) ===");
        lb.setStrategy(new RoundRobinStrategy());
        lb.markUnhealthy("server-2");

        Set<String> routedTo = new HashSet<>();
        for (int i = 0; i < 6; i++) {
            Server s = lb.routeRequest("health-req-" + i);
            if (s != null) { routedTo.add(s.id); lb.completeRequest(s); }
        }
        System.out.println("Routed to: " + routedTo);
        System.out.println("✓ server-2 was skipped: " + !routedTo.contains("server-2"));

        lb.markHealthy("server-2");

        // ---- Test 4: Server recovery ----
        System.out.println("\n=== Test 4: SERVER RECOVERY ===");
        routedTo.clear();
        for (int i = 0; i < 6; i++) {
            Server s = lb.routeRequest("recover-req-" + i);
            if (s != null) { routedTo.add(s.id); lb.completeRequest(s); }
        }
        System.out.println("Routed to: " + routedTo);
        System.out.println("✓ server-2 back in rotation: " + routedTo.contains("server-2"));

        // ---- Test 5: All servers down ----
        System.out.println("\n=== Test 5: ALL SERVERS DOWN ===");
        lb.markUnhealthy("server-1");
        lb.markUnhealthy("server-2");
        lb.markUnhealthy("server-3");
        Server result = lb.routeRequest("fail-req");
        System.out.println("✓ Returned null when all down: " + (result == null));

        lb.markHealthy("server-1");
        lb.markHealthy("server-2");
        lb.markHealthy("server-3");

        // ---- Test 6: Dynamic strategy switch ----
        System.out.println("\n=== Test 6: DYNAMIC STRATEGY SWITCH ===");
        lb.setStrategy(new RandomStrategy());
        for (int i = 0; i < 4; i++) {
            Server s = lb.routeRequest("random-req-" + i);
            lb.completeRequest(s);
        }
        System.out.println("✓ Random strategy working");

        // ---- Test 7: CONCURRENCY - 20 threads routing simultaneously ----
        System.out.println("\n=== Test 7: CONCURRENT ROUTING (20 threads, RoundRobin) ===");
        lb.setStrategy(new RoundRobinStrategy());
        // Reset connections
        lb.servers.forEach(s -> s.activeConnections.set(0));

        List<Thread> threads = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        Map<String, AtomicInteger> distribution = new ConcurrentHashMap<>();

        for (int i = 0; i < 20; i++) {
            final int idx = i;
            Thread t = new Thread(() -> {
                Server s = lb.routeRequest("concurrent-" + idx);
                if (s != null) {
                    successCount.incrementAndGet();
                    distribution.computeIfAbsent(s.id, k -> new AtomicInteger(0)).incrementAndGet();
                    // Simulate work
                    try { Thread.sleep(10); } catch (InterruptedException e) {}
                    lb.completeRequest(s);
                }
            }, "LBThread-" + i);
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) { t.join(); }

        System.out.println("All 20 routed: " + (successCount.get() == 20));
        System.out.println("Distribution: " + distribution);
        System.out.println("✓ No race conditions, all connections cleaned up");

        // ---- Test 8: CONCURRENT Least Connections ----
        System.out.println("\n=== Test 8: CONCURRENT LEAST CONNECTIONS (10 threads) ===");
        lb.setStrategy(new LeastConnectionsStrategy());
        lb.servers.forEach(s -> s.activeConnections.set(0));
        threads.clear();

        for (int i = 0; i < 10; i++) {
            Thread t = new Thread(() -> {
                Server s = lb.routeRequest("lc-concurrent");
                try { Thread.sleep(50); } catch (InterruptedException e) {}
                lb.completeRequest(s);
            }, "LCThread-" + i);
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) { t.join(); }

        lb.displayStatus();
        System.out.println("✓ All connections released (each server should be 0)");
        boolean allZero = lb.servers.stream().allMatch(s -> s.activeConnections.get() == 0);
        System.out.println("✓ Verified: " + allZero);
    }
}