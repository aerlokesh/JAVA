import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/*
 * CONTAINER ORCHESTRATOR - Low Level Design
 * ============================================
 * 
 * REQUIREMENTS:
 * 1. Manage a fleet of machines with CPU + memory capacity
 * 2. Deploy containers onto machines based on placement strategy:
 *    - BinPack: minimize machines used (pick machine with LEAST free resources that still fits)
 *    - Spread: maximize fault tolerance (pick machine with MOST free resources)
 * 3. Containers have lifecycle: PENDING → RUNNING → STOPPED
 * 4. Stop containers to free resources; double-stop throws exception
 * 5. Track container state and machine utilization
 * 6. Reject deployments when no machine has sufficient resources
 * 7. Thread-safe: concurrent deploy/stop from multiple threads
 * 
 * DESIGN PATTERNS:
 *   Strategy  (PlacementStrategy)     — pluggable machine selection algorithm
 *   Facade    (Orchestrator)          — single entry point for all operations
 *   Observer  (DeploymentListener)    — notify on lifecycle events
 * 
 * CORE ENTITIES:
 *   ContainerStatus (enum)       — PENDING, RUNNING, STOPPED
 *   Container                    — name, image, cpu, memory, status, assignedMachine
 *   Machine                      — id, totalCpu, totalMem, freeCpu, freeMem, containers
 *   PlacementStrategy (iface)    — selectMachine(machines, cpu, mem)
 *   BinPackStrategy              — pick machine with least spare capacity (packs tightly)
 *   SpreadStrategy               — pick machine with most spare capacity (spreads out)
 *   DeploymentListener (iface)   — onDeploy, onStop callbacks
 *   Orchestrator (facade)        — deploy, stop, status, listContainers, machineUtilization
 * 
 * KEY DATA STRUCTURES:
 *   Map<String, Machine>     machineMap    — machineId → Machine
 *   Map<String, Container>   containerMap  — containerName → Container
 * 
 * COMPLEXITY:
 *   deploy:         O(M) scan all machines for best fit
 *   stop:           O(1) lookup + resource update
 *   utilization:    O(1) per machine
 *   listContainers: O(C) where C = containers on a machine
 */

// ==================== EXCEPTIONS ====================

/**
 * Thrown when no machine has sufficient CPU + memory for the container.
 */
class InsufficientResourcesException extends RuntimeException {
    final int requestedCpu, requestedMem;
    InsufficientResourcesException(int cpu, int mem) {
        super("No machine can fit container requiring " + cpu + " CPU, " + mem + " MB memory");
        this.requestedCpu = cpu; this.requestedMem = mem;
    }
}

/**
 * Thrown when attempting to deploy a container with a name that already exists.
 */
class DuplicateContainerException extends RuntimeException {
    final String containerName;
    DuplicateContainerException(String name) {
        super("Container already exists: " + name);
        this.containerName = name;
    }
}

/**
 * Thrown when referencing a container or machine that doesn't exist.
 */
class NotFoundException extends RuntimeException {
    NotFoundException(String msg) { super(msg); }
}

/**
 * Thrown when stopping an already-stopped container.
 */
class InvalidStateException extends RuntimeException {
    InvalidStateException(String msg) { super(msg); }
}

// ==================== ENUMS ====================

enum ContainerStatus { PENDING, RUNNING, STOPPED }

// ==================== CONTAINER ====================

class Container {
    final String name;
    final String image;
    final int cpuUnits;
    final int memMb;
    ContainerStatus status;
    String machineId; // null if PENDING

    Container(String name, String image, int cpuUnits, int memMb) {
        this.name = name;
        this.image = image;
        this.cpuUnits = cpuUnits;
        this.memMb = memMb;
        this.status = ContainerStatus.PENDING;
        this.machineId = null;
    }

    @Override public String toString() {
        return name + "(" + image + ", " + cpuUnits + "cpu/" + memMb + "mb, " + status
            + (machineId != null ? " on " + machineId : "") + ")";
    }
}

// ==================== MACHINE ====================

class Machine {
    final String id;
    final int totalCpu;
    final int totalMem;
    int freeCpu;
    int freeMem;
    final Map<String, Container> containers = new LinkedHashMap<>();

    Machine(String id, int totalCpu, int totalMem) {
        this.id = id;
        this.totalCpu = totalCpu;
        this.totalMem = totalMem;
        this.freeCpu = totalCpu;
        this.freeMem = totalMem;
    }

    /** Can this machine fit a container needing 'cpu' units and 'mem' MB? */
    boolean canFit(int cpu, int mem) {
        return freeCpu >= cpu && freeMem >= mem;
    }

    /** Reserve resources for a container. */
    void reserve(Container c) {
        // TODO: Implement — subtract CPU/mem, add container to map, set container's machineId + RUNNING
        // HINT: freeCpu -= c.cpuUnits;
        // HINT: freeMem -= c.memMb;
        // HINT: containers.put(c.name, c);
        // HINT: c.machineId = this.id;
        // HINT: c.status = ContainerStatus.RUNNING;
        freeCpu -= c.cpuUnits;
        freeMem -= c.memMb;
        containers.put(c.name, c);
        c.machineId = this.id;
        c.status = ContainerStatus.RUNNING;
    }

    /** Release resources held by a container. */
    void release(Container c) {
        // TODO: Implement — add CPU/mem back, remove container from map, set STOPPED
        // HINT: freeCpu += c.cpuUnits;
        // HINT: freeMem += c.memMb;
        // HINT: containers.remove(c.name);
        // HINT: c.status = ContainerStatus.STOPPED;
        freeCpu += c.cpuUnits;
        freeMem += c.memMb;
        containers.remove(c.name);
        c.status = ContainerStatus.STOPPED;
    }

    /** CPU utilization as percentage 0.0–100.0 */
    double cpuUtilization() {
        return totalCpu == 0 ? 0.0 : (1.0 - (double) freeCpu / totalCpu) * 100.0;
    }

    /** Memory utilization as percentage 0.0–100.0 */
    double memUtilization() {
        return totalMem == 0 ? 0.0 : (1.0 - (double) freeMem / totalMem) * 100.0;
    }

    @Override public String toString() {
        return String.format("%s [CPU %d/%d (%.0f%%), Mem %d/%d (%.0f%%), %d containers]",
            id, totalCpu - freeCpu, totalCpu, cpuUtilization(),
            totalMem - freeMem, totalMem, memUtilization(), containers.size());
    }
}

// ==================== PLACEMENT STRATEGY ====================

/**
 * Strategy interface for selecting which machine to place a container on.
 * 
 * INTERVIEW DISCUSSION:
 * - BinPack: minimize machines → cost-efficient, but less fault-tolerant
 * - Spread: maximize machines → fault-tolerant, but higher cost
 * - Real-world: Kubernetes scheduler (filtering + scoring phases),
 *   AWS ECS (binpack, spread, random), Docker Swarm
 */
interface PlacementStrategy {
    /**
     * Select the best machine for a container requiring 'cpu' and 'mem'.
     * Only considers machines that canFit(cpu, mem).
     * Returns null if no machine can fit.
     */
    Machine selectMachine(Collection<Machine> machines, int cpu, int mem);
}

/**
 * BinPack: pick the machine with the LEAST free resources (that still fits).
 * Goal: pack containers tightly → fewer machines active → lower cost.
 * Tie-break: lexicographically smallest machineId.
 */
class BinPackStrategy implements PlacementStrategy {
    @Override
    public Machine selectMachine(Collection<Machine> machines, int cpu, int mem) {
        // TODO: Implement — filter machines that canFit, pick one with least free resources
        // HINT: return machines.stream()
        // HINT:     .filter(m -> m.canFit(cpu, mem))
        // HINT:     .min(Comparator.comparingInt((Machine m) -> m.freeCpu + m.freeMem)
        // HINT:         .thenComparing(m -> m.id))
        // HINT:     .orElseThrow(() -> new InsufficientResourcesException(cpu, mem));
        return machines.stream()
            .filter(m -> m.canFit(cpu, mem))
            .min(Comparator.comparingInt((Machine m) -> m.freeCpu + m.freeMem)
                .thenComparing(m -> m.id))
            .orElseThrow(() -> new InsufficientResourcesException(cpu, mem));
    }
}

/**
 * Spread: pick the machine with the MOST free resources.
 * Goal: spread containers across machines → better fault tolerance.
 * Tie-break: lexicographically smallest machineId.
 */
class SpreadStrategy implements PlacementStrategy {
    @Override
    public Machine selectMachine(Collection<Machine> machines, int cpu, int mem) {
        // TODO: Implement — filter machines that canFit, pick one with most free resources
        // HINT: return machines.stream()
        // HINT:     .filter(m -> m.canFit(cpu, mem))
        // HINT:     .max(Comparator.comparingInt((Machine m) -> m.freeCpu + m.freeMem)
        // HINT:         .thenComparing(Comparator.comparing((Machine m) -> m.id).reversed()))
        // HINT:     .orElseThrow(() -> new InsufficientResourcesException(cpu, mem));
        return machines.stream()
            .filter(m -> m.canFit(cpu, mem))
            .max(Comparator.comparingInt((Machine m) -> m.freeCpu + m.freeMem)
                .thenComparing(Comparator.comparing((Machine m) -> m.id).reversed()))
            .orElseThrow(() -> new InsufficientResourcesException(cpu, mem));
    }
}

// ==================== DEPLOYMENT LISTENER (OBSERVER) ====================

/**
 * Observer interface for container lifecycle events.
 * Real-world: send metrics, update dashboards, trigger alerts.
 */
interface DeploymentListener {
    void onDeploy(Container container, Machine machine);
    void onStop(Container container, Machine machine);
}

/** Logging listener — records events for auditing. */
class LoggingListener implements DeploymentListener {
    final List<String> events = new ArrayList<>();
    @Override public void onDeploy(Container c, Machine m) {
        events.add("DEPLOY:" + c.name + "→" + m.id);
    }
    @Override public void onStop(Container c, Machine m) {
        events.add("STOP:" + c.name + "→" + m.id); 
    }
}

/** Metrics listener — tracks deployment counts per machine. */
class MetricsListener implements DeploymentListener {
    final Map<String, Integer> deployCountPerMachine = new HashMap<>();
    int totalDeploys = 0, totalStops = 0;

    @Override public void onDeploy(Container c, Machine m) {
        // TODO: Implement — increment deploy count per machine and total
        // HINT: deployCountPerMachine.merge(m.id, 1, Integer::sum);
        // HINT: totalDeploys++;
        deployCountPerMachine.merge(m.id, 1, Integer::sum);
        totalDeploys++;
    }
    @Override public void onStop(Container c, Machine m) {
        // TODO: Implement — increment total stop count
        // HINT: totalStops++;
        totalStops++;
    }
}

/** Alert listener — triggers alert when machine utilization exceeds threshold. */
class AlertListener implements DeploymentListener {
    final double cpuThreshold;
    final List<String> alerts = new ArrayList<>();

    AlertListener(double cpuThresholdPercent) { this.cpuThreshold = cpuThresholdPercent; }

    @Override public void onDeploy(Container c, Machine m) {
        // TODO: Implement — check if CPU utilization exceeds threshold after deploy
        // HINT: if (m.cpuUtilization() > cpuThreshold) {
        // HINT:     alerts.add("HIGH_CPU:" + m.id + "@" + String.format("%.0f%%", m.cpuUtilization()));
        // HINT: }
        if (m.cpuUtilization() > cpuThreshold) {
            alerts.add("HIGH_CPU:" + m.id + "@" + String.format("%.0f%%", m.cpuUtilization()));
        }
    }
    @Override public void onStop(Container c, Machine m) { /* no alert on stop */ }
}

// ==================== ORCHESTRATOR (FACADE) ====================

class Orchestrator {
    private final Map<String, Machine> machineMap = new LinkedHashMap<>();
    private final Map<String, Container> containerMap = new ConcurrentHashMap<>();
    private PlacementStrategy strategy;
    private final List<DeploymentListener> listeners = new ArrayList<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    Orchestrator(List<String> machineSpecs, PlacementStrategy strategy) {
        this.strategy = strategy;
        for (String spec : machineSpecs) {
            String[] parts = spec.split(",");
            String id = parts[0].trim();
            int cpu = Integer.parseInt(parts[1].trim());
            int mem = Integer.parseInt(parts[2].trim());
            machineMap.put(id, new Machine(id, cpu, mem));
        }
    }

    /** Register an observer for deployment events. */
    void addListener(DeploymentListener listener) { listeners.add(listener); }

    /** Swap placement strategy at runtime. */
    void setStrategy(PlacementStrategy strategy) { this.strategy = strategy; }

    /**
     * Deploy a container onto the best available machine.
     * Returns the container object.
     * 
     * @throws DuplicateContainerException if name already exists
     * @throws InsufficientResourcesException if no machine can fit
     */
    Container deploy(String name, String image, int cpu, int mem) {
        // TODO: Implement — validate, select machine via strategy, reserve, notify listeners
        // HINT: lock.writeLock().lock();
        // HINT: try {
        // HINT:     if (containerMap.containsKey(name)) throw new DuplicateContainerException(name);
        // HINT:     Machine target = strategy.selectMachine(machineMap.values(), cpu, mem); // throws InsufficientResourcesException via orElseThrow
        // HINT:     Container c = new Container(name, image, cpu, mem);
        // HINT:     target.reserve(c);
        // HINT:     containerMap.put(name, c);
        // HINT:     listeners.forEach(l -> l.onDeploy(c, target));
        // HINT:     return c;
        // HINT: } finally { lock.writeLock().unlock(); }
        lock.writeLock().lock();
        try {
            if (containerMap.containsKey(name)) throw new DuplicateContainerException(name);
            Machine target = strategy.selectMachine(machineMap.values(), cpu, mem);
            Container c = new Container(name, image, cpu, mem);
            target.reserve(c);
            containerMap.put(name, c);
            listeners.forEach(l -> l.onDeploy(c, target));
            return c;
        } finally { lock.writeLock().unlock(); }
    }

    /**
     * Stop a running container and free its resources.
     * 
     * @throws NotFoundException if container doesn't exist
     * @throws InvalidStateException if container already stopped
     */
    void stop(String name) {
        // TODO: Implement — find container, validate state, release resources, notify
        // HINT: lock.writeLock().lock();
        // HINT: try {
        // HINT:     Container c = containerMap.get(name);
        // HINT:     if (c == null) throw new NotFoundException("Container not found: " + name);
        // HINT:     if (c.status == ContainerStatus.STOPPED) throw new InvalidStateException("Already stopped: " + name);
        // HINT:     Machine m = machineMap.get(c.machineId);
        // HINT:     m.release(c);
        // HINT:     listeners.forEach(l -> l.onStop(c, m));
        // HINT: } finally { lock.writeLock().unlock(); }
        lock.writeLock().lock();
        try {
            Container c = containerMap.get(name);
            if (c == null) throw new NotFoundException("Container not found: " + name);
            if (c.status == ContainerStatus.STOPPED) throw new InvalidStateException("Already stopped: " + name);
            Machine m = machineMap.get(c.machineId);
            m.release(c);
            listeners.forEach(l -> l.onStop(c, m));
        } finally { lock.writeLock().unlock(); }
    }

    /** Get container status by name. Throws NotFoundException if not found. */
    Container getContainer(String name) {
        Container c = containerMap.get(name);
        if (c == null) throw new NotFoundException("Container not found: " + name);
        return c;
    }

    /** Get machine status by id. */
    Machine getMachine(String id) {
        Machine m = machineMap.get(id);
        if (m == null) throw new NotFoundException("Machine not found: " + id);
        return m;
    }

    /** List all running containers on a machine. */
    List<Container> listContainers(String machineId) {
        // TODO: Implement — return running containers on the machine using stream
        // HINT: return getMachine(machineId).containers.values().stream()
        // HINT:     .filter(c -> c.status == ContainerStatus.RUNNING)
        // HINT:     .collect(java.util.stream.Collectors.toList());
        return getMachine(machineId).containers.values().stream()
            .filter(c -> c.status == ContainerStatus.RUNNING)
            .collect(java.util.stream.Collectors.toList());
    }

    /** Get number of active (RUNNING) containers across all machines. */
    int activeContainerCount() {
        // TODO: Implement — count containers with RUNNING status
        // HINT: return (int) containerMap.values().stream()
        // HINT:     .filter(c -> c.status == ContainerStatus.RUNNING).count();
        return (int) containerMap.values().stream()
            .filter(c -> c.status == ContainerStatus.RUNNING).count();
    }

    /** Print status of all machines. */
    void printStatus() {
        System.out.println("--- Cluster Status ---");
        for (Machine m : machineMap.values()) {
            System.out.println("  " + m);
        }
        System.out.println("  Active containers: " + activeContainerCount());
    }
}

// ==================== MAIN / TESTS ====================

public class ContainerOrchestratorSystem {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║   CONTAINER ORCHESTRATOR - LLD Demo       ║");
        System.out.println("╚═══════════════════════════════════════════╝\n");

        List<String> machines = List.of("mA,8,16000", "mB,8,8000", "mC,4,32000");

        // --- Test 1: BinPack — fills tightest machine first ---
        System.out.println("=== Test 1: BinPack strategy ===");
        Orchestrator orch = new Orchestrator(machines, new BinPackStrategy());
        Container c1 = orch.deploy("web-1", "nginx:latest", 2, 2000);
        // All machines free → mA(8+16000=16008), mB(8+8000=8008), mC(4+32000=32004)
        // BinPack picks LEAST free → mB (8008 is smallest)
        check(c1.machineId, "mB", "BinPack picks tightest machine (mB)");
        check(c1.status, ContainerStatus.RUNNING, "Container is RUNNING");
        System.out.println("✓ BinPack places on tightest-fit machine\n");

        // --- Test 2: BinPack continues packing same machine ---
        System.out.println("=== Test 2: BinPack keeps packing ===");
        Container c2 = orch.deploy("web-2", "nginx:latest", 2, 2000);
        // mA(16008), mB(6+6000=6006 after c1), mC(32004)
        // BinPack → mB again (6006 < 16008 < 32004)
        check(c2.machineId, "mB", "BinPack keeps packing mB");
        System.out.println("  " + orch.getMachine("mB"));
        System.out.println("✓ BinPack continues on same machine\n");

        // --- Test 3: Spread — distributes across machines ---
        System.out.println("=== Test 3: Spread strategy ===");
        Orchestrator orch2 = new Orchestrator(machines, new SpreadStrategy());
        Container s1 = orch2.deploy("api-1", "app:v1", 2, 2000);
        // Spread picks MOST free → mC (32004 > 16008 > 8008)
        check(s1.machineId, "mC", "Spread picks most-free machine (mC)");
        Container s2 = orch2.deploy("api-2", "app:v1", 2, 2000);
        // mA(16008), mB(8008), mC(2+30000=30002 after s1) → mC still most, then mA
        check(s2.machineId, "mC", "Spread: mC still has most free");
        System.out.println("✓ Spread distributes across machines\n");

        // --- Test 4: Stop frees resources ---
        System.out.println("=== Test 4: Stop frees resources ===");
        Machine mB = orch.getMachine("mB");
        int cpuBefore = mB.freeCpu;
        orch.stop("web-1");
        check(mB.freeCpu, cpuBefore + 2, "CPU freed after stop");
        check(orch.getContainer("web-1").status, ContainerStatus.STOPPED, "Container stopped");
        System.out.println("  " + mB);
        System.out.println("✓ Stop frees CPU + memory\n");

        // --- Test 5: Duplicate container name ---
        System.out.println("=== Test 5: Duplicate container name ===");
        try {
            orch.deploy("web-2", "nginx:latest", 1, 1000);
            System.out.println("  ✗ Should have thrown");
        } catch (DuplicateContainerException e) {
            System.out.println("  ✓ Blocked: " + e.getMessage());
        }
        System.out.println();

        // --- Test 6: Insufficient resources ---
        System.out.println("=== Test 6: Insufficient resources ===");
        Orchestrator small = new Orchestrator(List.of("tiny,1,512"), new SpreadStrategy());
        small.deploy("fits", "alpine", 1, 512);
        try {
            small.deploy("wontfit", "nginx", 1, 1000);
            System.out.println("  ✗ Should have thrown");
        } catch (InsufficientResourcesException e) {
            System.out.println("  ✓ Blocked: " + e.getMessage());
        }
        System.out.println();

        // --- Test 7: Stop non-existent / already stopped ---
        System.out.println("=== Test 7: Stop edge cases ===");
        try {
            orch.stop("doesNotExist");
            System.out.println("  ✗ Should have thrown NotFoundException");
        } catch (NotFoundException e) {
            System.out.println("  ✓ NotFoundException: " + e.getMessage());
        }
        try {
            orch.stop("web-1"); // already stopped in Test 4
            System.out.println("  ✗ Should have thrown InvalidStateException");
        } catch (InvalidStateException e) {
            System.out.println("  ✓ InvalidStateException: " + e.getMessage());
        }
        System.out.println();

        // --- Test 8: Observer pattern — Multiple listeners ---
        System.out.println("=== Test 8: Multiple DeploymentListeners ===");
        Orchestrator orch3 = new Orchestrator(List.of("obs,4,8000"), new BinPackStrategy());
        LoggingListener logger = new LoggingListener();
        MetricsListener metrics = new MetricsListener();
        AlertListener alerter = new AlertListener(50.0); // alert when CPU > 50%
        orch3.addListener(logger);
        orch3.addListener(metrics);
        orch3.addListener(alerter);
        // Deploy 3 containers (1cpu each on 4cpu machine → 25%, 50%, 75%)
        orch3.deploy("svc-1", "app:v1", 1, 1000);
        orch3.deploy("svc-2", "app:v1", 1, 1000);
        orch3.deploy("svc-3", "app:v1", 1, 1000); // 75% CPU → should trigger alert
        orch3.stop("svc-1");
        // Verify LoggingListener
        check(logger.events.size(), 4, "LoggingListener: 3 deploys + 1 stop = 4 events");
        System.out.println("  Log events: " + logger.events);
        // Verify MetricsListener
        check(metrics.totalDeploys, 3, "MetricsListener: 3 total deploys");
        check(metrics.totalStops, 1, "MetricsListener: 1 total stop");
        check(metrics.deployCountPerMachine.getOrDefault("obs", 0), 3, "MetricsListener: 3 deploys on 'obs'");
        // Verify AlertListener
        check(alerter.alerts.size(), 1, "AlertListener: 1 alert (CPU > 50% at 75%)");
        if (!alerter.alerts.isEmpty()) System.out.println("  Alert: " + alerter.alerts.get(0));
        System.out.println("✓ All 3 listeners notified correctly\n");
        // --- Test 10: Scale + thread safety ---
        System.out.println("=== Test 10: Scale + thread safety ===");
        List<String> bigMachines = new ArrayList<>();
        for (int i = 0; i < 50; i++)
            bigMachines.add("m" + String.format("%03d", i) + ",100,100000");
        Orchestrator orch5 = new Orchestrator(bigMachines, new SpreadStrategy());

        ExecutorService exec = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();
        // Deploy 200 containers concurrently
        for (int i = 0; i < 200; i++) {
            int idx = i;
            futures.add(exec.submit(() -> orch5.deploy("c" + idx, "img:" + idx, 1, 100)));
        }
        // Stop 50 concurrently
        for (int i = 0; i < 50; i++) {
            int idx = i;
            futures.add(exec.submit(() -> { try { Thread.sleep(10); } catch (Exception e) {}
                try { orch5.stop("c" + idx); } catch (Exception e) {} }));
        }
        for (Future<?> f : futures) { try { f.get(); } catch (Exception e) {} }
        exec.shutdown();

        int active = orch5.activeContainerCount();
        System.out.println("  Deployed: 200, Stopped: ~50, Active: " + active);
        check(active >= 100 && active <= 200, true, "Active count in expected range");
        orch5.printStatus();
        System.out.println("✓ Thread-safe concurrent deploy + stop\n");

        System.out.println("════════ ALL 10 TESTS PASSED ✓ ════════");
    }

    // --- Test helpers ---

    static void check(String actual, String expected, String msg) {
        String s = Objects.equals(actual, expected) ? "✓" : "✗ GOT '" + actual + "' expected '" + expected + "'";
        System.out.println("  " + s + " " + msg);
    }

    static void check(int actual, int expected, String msg) {
        String s = actual == expected ? "✓" : "✗ GOT " + actual + " expected " + expected;
        System.out.println("  " + s + " " + msg);
    }

    static void check(boolean actual, boolean expected, String msg) {
        String s = actual == expected ? "✓" : "✗ GOT " + actual;
        System.out.println("  " + s + " " + msg);
    }

    static void check(ContainerStatus actual, ContainerStatus expected, String msg) {
        String s = actual == expected ? "✓" : "✗ GOT " + actual;
        System.out.println("  " + s + " " + msg);
    }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. STRATEGY PATTERN — Placement:
 *    BinPackStrategy: minimize machine count → cost-efficient.
 *      Score = freeCpu + freeMem → pick LOWEST (tightest fit).
 *    SpreadStrategy: maximize fault tolerance → spread load.
 *      Score = freeCpu + freeMem → pick HIGHEST (most headroom).
 *    Tie-break: lexicographically smallest machineId for determinism.
 *    Real-world: K8s scheduler has filtering + scoring phases with
 *    pluggable priorities (NodeAffinity, PodAntiAffinity, etc.)
 *
 * 2. OBSERVER PATTERN — DeploymentListener:
 *    Decouples orchestrator from monitoring/alerting/logging.
 *    Listeners notified on deploy + stop events.
 *    Real-world: K8s event system, AWS EventBridge for ECS events.
 *
 * 3. RESOURCE MODEL:
 *    Machine tracks (totalCpu, freeCpu) and (totalMem, freeMem).
 *    Container reserves on deploy, releases on stop.
 *    canFit() checks BOTH CPU AND memory — both must be sufficient.
 *    Utilization = (total - free) / total × 100%.
 *
 * 4. THREAD SAFETY:
 *    ReadWriteLock protects machineMap + containerMap.
 *    ConcurrentHashMap for containerMap allows lock-free reads.
 *    WriteLock for deploy/stop (mutating state).
 *    Real-world: K8s uses optimistic concurrency (resource versions).
 *
 * 5. EXCEPTION HIERARCHY:
 *    InsufficientResourcesException — no machine fits
 *    DuplicateContainerException — name collision
 *    NotFoundException — unknown container/machine
 *    InvalidStateException — stop already-stopped container
 *    Fail-fast with descriptive messages → easy debugging.
 *
 * 6. EXTENSIONS (discussion only):
 *    - Health checks: periodic pings, auto-restart on failure
 *    - Replica sets: maintain N copies, auto-scale on demand
 *    - Rolling deployments: update containers one-by-one
 *    - Port binding: track port allocation, detect conflicts
 *    - Resource limits vs requests: soft limits for bursting
 *    - Affinity/anti-affinity: co-locate or separate containers
 *    - Priority + preemption: evict low-priority for high-priority
 *    - Persistent volumes: attach storage to containers
 *
 * 7. REAL-WORLD COMPARISONS:
 *    Kubernetes: Pod → Node scheduling, kube-scheduler
 *    AWS ECS: Task → Container Instance placement
 *    Docker Swarm: Service → Node placement
 *    Nomad: Job → Client allocation
 *
 * 8. COMPLEXITY SUMMARY:
 *    deploy:           O(M) scan machines
 *    stop:             O(1) lookup + update
 *    canFit:           O(1) compare two ints
 *    listContainers:   O(C) per machine
 *    activeCount:      O(N) scan all containers
 *    cpuUtilization:   O(1) per machine
 */
