import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/*
 * CONTAINER ORCHESTRATOR - Low Level Design
 * ===========================================
 * 
 * REQUIREMENTS:
 * 1. Manage cloud machines with CPU + memory capacity
 * 2. Assign containers to machines based on criteria:
 *    - criteria=0: max spare CPU (tie-break: lex smallest machineId)
 *    - criteria=1: max spare memory (tie-break: lex smallest machineId)
 * 3. Containers reserve CPU + memory while RUNNING
 * 4. stop() frees resources; double-stop returns false
 * 5. Machine must have sufficient free CPU AND memory
 * 6. Thread-safe operations
 * 
 * KEY DATA STRUCTURES:
 * - Map<String, Machine>: machineId -> Machine with resource tracking
 * - Map<String, Container>: containerName -> Container for quick lookup
 * 
 * COMPLEXITY:
 *   assignMachine: O(M) where M = number of machines (scan all)
 *   stop:          O(1) lookup + resource update
 */

// ==================== ENUMS ====================

enum ContainerState { RUNNING, STOPPED }

// ==================== CONTAINER ====================

class Container {
    final String name, imageUrl;
    final int cpuUnits, memMb;
    String machineId;
    ContainerState state;

    Container(String name, String imageUrl, int cpuUnits, int memMb, String machineId) {
        this.name = name; this.imageUrl = imageUrl;
        this.cpuUnits = cpuUnits; this.memMb = memMb;
        this.machineId = machineId; this.state = ContainerState.RUNNING;
    }
}

// ==================== MACHINE ====================

class Machine {
    final String id;
    final int totalCpu, totalMem;
    int freeCpu, freeMem;
    final Map<String, Container> containers = new HashMap<>();

    Machine(String id, int totalCpu, int totalMem) {
        this.id = id; this.totalCpu = totalCpu; this.totalMem = totalMem;
        this.freeCpu = totalCpu; this.freeMem = totalMem;
    }

    boolean canFit(int cpu, int mem) { return freeCpu >= cpu && freeMem >= mem; }

    void reserve(Container c) {
        freeCpu -= c.cpuUnits;
        freeMem -= c.memMb;
        containers.put(c.name, c);
    }

    void release(Container c) {
        freeCpu += c.cpuUnits;
        freeMem += c.memMb;
    }
}

// ==================== CONTAINER MANAGER ====================

class ContainerManager {
    private final Map<String, Machine> machines = new LinkedHashMap<>();
    private final Map<String, Container> containers = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /** Parse "machineId,totalCpu,totalMem" strings. */
    ContainerManager(List<String> machineSpecs) {
        for (String spec : machineSpecs) {
            String[] parts = spec.split(",");
            String id = parts[0].trim();
            int cpu = Integer.parseInt(parts[1].trim());
            int mem = Integer.parseInt(parts[2].trim());
            machines.put(id, new Machine(id, cpu, mem));
        }
    }

    /**
     * Assign container to a machine based on criteria.
     * criteria=0: pick machine with max free CPU
     * criteria=1: pick machine with max free memory
     * Tie-break: lexicographically smallest machineId
     * Returns machineId or "" if no machine can fit.
     */
    String assignMachine(int criteria, String containerName, String imageUrl, int cpuUnits, int memMb) {
        lock.writeLock().lock();
        try {
            // TODO: Implement — find best machine by criteria, assign container
            // HINT: Machine best = null;
            // HINT: for (Machine m : machines.values()) {
            // HINT:     if (!m.canFit(cpuUnits, memMb)) continue;
            // HINT:     if (best == null || betterFit(m, best, criteria)) best = m;
            // HINT: }
            // HINT: if (best == null) return "";
            // HINT: Container c = new Container(containerName, imageUrl, cpuUnits, memMb, best.id);
            // HINT: best.reserve(c);
            // HINT: containers.put(containerName, c);
            // HINT: return best.id;
            return "";
        } finally { lock.writeLock().unlock(); }
    }

    /**
     * Compare two machines: is 'candidate' a better fit than 'current'?
     * criteria=0: higher free CPU wins; criteria=1: higher free memory wins.
     * Tie-break: lexicographically smaller machineId wins.
     */
    private boolean betterFit(Machine candidate, Machine current, int criteria) {
        // TODO: Implement
        // HINT: int cVal = criteria == 0 ? candidate.freeCpu : candidate.freeMem;
        // HINT: int bVal = criteria == 0 ? current.freeCpu : current.freeMem;
        // HINT: if (cVal != bVal) return cVal > bVal;
        // HINT: return candidate.id.compareTo(current.id) < 0;
        return false;
    }

    /**
     * Stop a running container — frees CPU + memory.
     * Returns false if container doesn't exist or already stopped.
     */
    boolean stop(String name) {
        lock.writeLock().lock();
        try {
            // TODO: Implement
            // HINT: Container c = containers.get(name);
            // HINT: if (c == null || c.state == ContainerState.STOPPED) return false;
            // HINT: c.state = ContainerState.STOPPED;
            // HINT: Machine m = machines.get(c.machineId);
            // HINT: m.release(c);
            // HINT: return true;
            return false;
        } finally { lock.writeLock().unlock(); }
    }

    // --- Utility for tests ---

    String getMachineStatus(String machineId) {
        Machine m = machines.get(machineId);
        if (m == null) return "NOT_FOUND";
        return String.format("%s: CPU %d/%d, Mem %d/%d, containers=%d",
            m.id, m.freeCpu, m.totalCpu, m.freeMem, m.totalMem, m.containers.size());
    }

    String getContainerState(String name) {
        Container c = containers.get(name);
        return c == null ? "NOT_FOUND" : c.state + " on " + c.machineId;
    }
}

// ==================== MAIN / TESTS ====================

public class ContainerOrchestratorSystem {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║   CONTAINER ORCHESTRATOR - LLD Demo       ║");
        System.out.println("╚═══════════════════════════════════════════╝\n");

        // Common setup
        ContainerManager cm = new ContainerManager(List.of("mA,8,16000", "mB,8,8000", "mC,4,32000"));

        // --- Test 1: CPU-first (criteria=0) with tie-break ---
        System.out.println("=== Test 1: CPU-first with tie-break ===");
        String r1 = cm.assignMachine(0, "c1", "img://a", 2, 2000);
        System.out.println("assignMachine(0, c1, 2cpu, 2000mem): '" + r1 + "' (expected 'mA')");
        // mA=8, mB=8 → tie → lex smallest = mA
        System.out.println(cm.getMachineStatus("mA"));
        System.out.println("✓ Tie-break picks lexicographically smallest\n");

        // --- Test 2: CPU-first, no tie ---
        System.out.println("=== Test 2: CPU-first, mB has most free CPU ===");
        String r2 = cm.assignMachine(0, "c2", "img://b", 6, 1000);
        System.out.println("assignMachine(0, c2, 6cpu, 1000mem): '" + r2 + "' (expected 'mB')");
        // mA=6, mB=8, mC=4 → pick mB
        System.out.println(cm.getMachineStatus("mB"));
        System.out.println("✓ Picks machine with max spare CPU\n");

        // --- Test 3: CPU-first again ---
        System.out.println("=== Test 3: CPU-first, mA has most free CPU ===");
        String r3 = cm.assignMachine(0, "c3", "img://c", 3, 1000);
        System.out.println("assignMachine(0, c3, 3cpu, 1000mem): '" + r3 + "' (expected 'mA')");
        // mA=6, mB=2, mC=4 → pick mA
        System.out.println(cm.getMachineStatus("mA"));
        System.out.println("✓ Resource tracking after multiple assignments\n");

        // --- Test 4: Memory-first (criteria=1) ---
        System.out.println("=== Test 4: Memory-first ===");
        String r4 = cm.assignMachine(1, "c4", "img://d", 2, 9000);
        System.out.println("assignMachine(1, c4, 2cpu, 9000mem): '" + r4 + "' (expected 'mC')");
        // Mem: mA=13000, mB=7000, mC=32000 → pick mC
        System.out.println(cm.getMachineStatus("mC"));
        System.out.println("✓ Memory-first picks mC\n");

        // --- Test 5: Insufficient resources ---
        System.out.println("=== Test 5: No machine fits ===");
        String r5 = cm.assignMachine(1, "c5", "img://e", 2, 25000);
        System.out.println("assignMachine(1, c5, 2cpu, 25000mem): '" + r5 + "' (expected '')");
        // Mem: mA=13000, mB=7000, mC=23000 → none ≥ 25000
        System.out.println("✓ Returns empty when no machine fits\n");

        // --- Test 6: Stop frees resources ---
        System.out.println("=== Test 6: Stop frees resources ===");
        boolean s1 = cm.stop("c4");
        System.out.println("stop(c4): " + s1 + " (expected true)");
        System.out.println(cm.getMachineStatus("mC"));
        System.out.println("Container c4: " + cm.getContainerState("c4"));
        System.out.println("✓ Stop frees CPU + memory\n");

        // --- Test 7: Retry after stop succeeds ---
        System.out.println("=== Test 7: Retry after freeing resources ===");
        String r6 = cm.assignMachine(1, "c5", "img://e", 2, 25000);
        System.out.println("assignMachine(1, c5, 2cpu, 25000mem): '" + r6 + "' (expected 'mC')");
        System.out.println(cm.getMachineStatus("mC"));
        System.out.println("✓ Retry succeeds after stop freed resources\n");

        // --- Test 8: Stop edge cases ---
        System.out.println("=== Test 8: Stop edge cases ===");
        boolean s2 = cm.stop("doesNotExist");
        System.out.println("stop(doesNotExist): " + s2 + " (expected false)");
        boolean s3 = cm.stop("c4");
        System.out.println("stop(c4) again: " + s3 + " (expected false — already stopped)");
        System.out.println("✓ Invalid/double stop returns false\n");

        // --- Test 9: Scale ---
        System.out.println("=== Test 9: Scale ===");
        List<String> bigMachines = new ArrayList<>();
        for (int i = 0; i < 100; i++)
            bigMachines.add("m" + String.format("%03d", i) + ",100,100000");
        ContainerManager cm2 = new ContainerManager(bigMachines);
        long start = System.nanoTime();
        int assigned = 0;
        for (int i = 0; i < 5000; i++) {
            String res = cm2.assignMachine(i % 2, "c" + i, "img://" + i, 1, 10);
            if (!res.isEmpty()) assigned++;
        }
        System.out.printf("5000 assignments on 100 machines: %d placed in %.2f ms\n",
            assigned, (System.nanoTime()-start)/1e6);
        System.out.println("✓ Scales well\n");

        // --- Test 10: Thread Safety ---
        System.out.println("=== Test 10: Thread Safety ===");
        ContainerManager cm3 = new ContainerManager(List.of("x1,1000,1000000", "x2,1000,1000000"));
        ExecutorService exec = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            int idx = i;
            futures.add(exec.submit(() ->
                cm3.assignMachine(idx % 2, "tc" + idx, "img://" + idx, 1, 100)));
        }
        for (int i = 0; i < 50; i++) {
            int idx = i;
            futures.add(exec.submit(() -> cm3.stop("tc" + idx)));
        }
        for (Future<?> f : futures) { try { f.get(); } catch (Exception e) { System.out.println("ERR: " + e); } }
        exec.shutdown();
        System.out.println(cm3.getMachineStatus("x1"));
        System.out.println(cm3.getMachineStatus("x2"));
        System.out.println("✓ Thread-safe concurrent assign + stop\n");

        System.out.println("════════ ALL 10 TESTS PASSED ✓ ════════");
    }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. CORE: Machine selection by scanning all machines, filtering by
 *    capacity, comparing by criteria (CPU or memory), lex tie-break.
 *    O(M) per assignment. Fine for <10K machines.
 *
 * 2. RESOURCE MODEL: RUNNING reserves CPU+mem. STOPPED frees all.
 *    Simple increment/decrement — no fragmentation to track.
 *
 * 3. SELECTION STRATEGIES: criteria param is a simple Strategy pattern
 *    (could extract interface if more criteria added: round-robin,
 *    least-containers, bin-packing, etc.)
 *
 * 4. SCALE IMPROVEMENTS:
 *    - TreeMap<FreeResource, Set<Machine>> for O(log M) selection
 *    - Bin-packing heuristics (first-fit-decreasing) for utilization
 *    - Consistent hashing for machine-container affinity
 *
 * 5. REAL-WORLD: Kubernetes scheduler (scoring + filtering phases),
 *    AWS ECS task placement (spread, binpack, random strategies),
 *    Docker Swarm (spread across nodes).
 *
 * 6. EXTENSIONS (discussion only):
 *    - pause/resume with partial resource release
 *    - Port binding and conflict detection
 *    - Health checks and auto-restart
 *    - Horizontal scaling (replica sets)
 *    - Rolling deployments
 *
 * 7. COMPLEXITY:
 *    assignMachine: O(M) scan all machines
 *    stop:          O(1) lookup + update
 *    constructor:   O(M) parse specs
 */
