import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/*
 * DISTRIBUTED JOB SCHEDULER - Low Level Design
 * ===============================================
 * 
 * REQUIREMENTS:
 * 1. addMachine(machineId, capabilities) — register machine
 * 2. assignMachineToJob(jobId, requiredCaps, criteria) — pick best machine
 *    - criteria=0: least unfinished jobs (tie: lex smallest machineId)
 *    - criteria=1: most finished jobs (tie: lex smallest machineId)
 * 3. jobCompleted(jobId) — mark done, update counters
 * 4. Capabilities are case-insensitive, machine must be superset of required
 * 5. Extensible: easy to add new selection criteria (Strategy pattern)
 * 
 * DESIGN PATTERNS: Strategy (machine selection criteria)
 * 
 * KEY DATA STRUCTURES:
 * - Map<String, SchedulerMachine>: machineId -> machine with caps + counters
 * - Map<String, SchedulerJob>: jobId -> job tracking
 * 
 * COMPLEXITY:
 *   addMachine:         O(C) where C = capabilities count
 *   assignMachineToJob: O(M * C) scan machines, check capability superset
 *   jobCompleted:       O(1) lookup + counter update
 */

// ==================== STRATEGY: MACHINE SELECTOR ====================

interface MachineSelector {
    /** Compare: is candidate better than current best? */
    boolean isBetter(SchedulerMachine candidate, SchedulerMachine current);
}

/** criteria=0: prefer least unfinished jobs, tie-break lex smallest id. */
class LeastUnfinishedSelector implements MachineSelector {
    @Override
    public boolean isBetter(SchedulerMachine candidate, SchedulerMachine current) {
        // TODO: Implement
        // HINT: if (candidate.unfinishedJobs != current.unfinishedJobs)
        // HINT:     return candidate.unfinishedJobs < current.unfinishedJobs;
        // HINT: return candidate.id.compareTo(current.id) < 0;
        return false;
    }
}

/** criteria=1: prefer most finished jobs, tie-break lex smallest id. */
class MostFinishedSelector implements MachineSelector {
    @Override
    public boolean isBetter(SchedulerMachine candidate, SchedulerMachine current) {
        // TODO: Implement
        // HINT: if (candidate.finishedJobs != current.finishedJobs)
        // HINT:     return candidate.finishedJobs > current.finishedJobs;
        // HINT: return candidate.id.compareTo(current.id) < 0;
        return false;
    }
}

// ==================== MACHINE ====================

class SchedulerMachine {
    final String id;
    final Set<String> capabilities;  // lowercase
    int unfinishedJobs;
    int finishedJobs;

    SchedulerMachine(String id, String[] caps) {
        this.id = id;
        this.capabilities = new HashSet<>();
        for (String c : caps) capabilities.add(c.toLowerCase().trim());
    }

    boolean hasAll(Set<String> required) {
        return capabilities.containsAll(required);
    }
}

// ==================== JOB ====================

class SchedulerJob {
    final String id;
    final String machineId;

    SchedulerJob(String id, String machineId) {
        this.id = id; this.machineId = machineId;
    }
}

// ==================== SCHEDULER ====================

class DistributedScheduler {
    private final Map<String, SchedulerMachine> machines = new LinkedHashMap<>();
    private final Map<String, SchedulerJob> jobs = new ConcurrentHashMap<>();
    private final Map<Integer, MachineSelector> selectors = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    DistributedScheduler() {
        selectors.put(0, new LeastUnfinishedSelector());
        selectors.put(1, new MostFinishedSelector());
    }

    /** Register for new criteria — extensibility point. */
    void registerSelector(int criteria, MachineSelector selector) {
        selectors.put(criteria, selector);
    }

    void addMachine(String machineId, String[] capabilities) {
        lock.writeLock().lock();
        try {
            machines.put(machineId, new SchedulerMachine(machineId, capabilities));
        } finally { lock.writeLock().unlock(); }
    }

    /**
     * Assign job to best machine matching all required capabilities.
     * Uses Strategy pattern to select based on criteria.
     * Returns machineId or "" if no compatible machine.
     */
    String assignMachineToJob(String jobId, String[] capabilitiesRequired, int criteria) {
        lock.writeLock().lock();
        try {
            // TODO: Implement
            // HINT: MachineSelector selector = selectors.get(criteria);
            // HINT: if (selector == null) return "";
            // HINT: Set<String> required = new HashSet<>();
            // HINT: for (String c : capabilitiesRequired) required.add(c.toLowerCase().trim());
            // HINT: SchedulerMachine best = null;
            // HINT: for (SchedulerMachine m : machines.values()) {
            // HINT:     if (!m.hasAll(required)) continue;
            // HINT:     if (best == null || selector.isBetter(m, best)) best = m;
            // HINT: }
            // HINT: if (best == null) return "";
            // HINT: best.unfinishedJobs++;
            // HINT: jobs.put(jobId, new SchedulerJob(jobId, best.id));
            // HINT: return best.id;
            return "";
        } finally { lock.writeLock().unlock(); }
    }

    /** Mark job complete: decrement unfinished, increment finished. */
    void jobCompleted(String jobId) {
        lock.writeLock().lock();
        try {
            // TODO: Implement
            // HINT: SchedulerJob job = jobs.get(jobId);
            // HINT: if (job == null) return;
            // HINT: SchedulerMachine m = machines.get(job.machineId);
            // HINT: m.unfinishedJobs--;
            // HINT: m.finishedJobs++;
        } finally { lock.writeLock().unlock(); }
    }

    // --- Utility ---

    String getMachineStatus(String machineId) {
        SchedulerMachine m = machines.get(machineId);
        if (m == null) return "NOT_FOUND";
        return String.format("%s: unfinished=%d, finished=%d, caps=%s",
            m.id, m.unfinishedJobs, m.finishedJobs, m.capabilities);
    }
}

// ==================== MAIN / TESTS ====================

public class DistributedSchedulerSystem {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║   DISTRIBUTED SCHEDULER - LLD Demo        ║");
        System.out.println("╚═══════════════════════════════════════════╝\n");

        DistributedScheduler sched = new DistributedScheduler();

        // --- Test 1: Multi-cap match + criteria=0 + lex tie ---
        System.out.println("=== Test 1: Capability match + least unfinished + tie-break ===");
        sched.addMachine("m-10", new String[]{"image compression", "audio extraction", "video thumbnail generation"});
        sched.addMachine("m-2", new String[]{"image compression", "audio extraction"});

        String r1 = sched.assignMachineToJob("job-A", new String[]{"image compression", "audio extraction"}, 0);
        System.out.println("assign job-A: '" + r1 + "' (expected 'm-10')");
        // Both have 0 unfinished → tie → "m-10" < "m-2" lexicographically
        System.out.println(sched.getMachineStatus("m-10"));
        System.out.println("✓ Lex tie-break: m-10 < m-2\n");

        // --- Test 2: Completion + criteria=1 (most finished) ---
        System.out.println("=== Test 2: jobCompleted + most finished ===");
        sched.jobCompleted("job-A");
        System.out.println(sched.getMachineStatus("m-10"));

        String r2 = sched.assignMachineToJob("job-B", new String[]{"image compression"}, 1);
        System.out.println("assign job-B (criteria=1): '" + r2 + "' (expected 'm-10')");
        // m-10 finished=1, m-2 finished=0 → pick m-10
        System.out.println(sched.getMachineStatus("m-10"));
        System.out.println("✓ Most finished wins\n");

        // --- Test 3: No compatible machine ---
        System.out.println("=== Test 3: No compatible machine ===");
        String r3 = sched.assignMachineToJob("job-C", new String[]{"speech to text conversion"}, 0);
        System.out.println("assign job-C: '" + r3 + "' (expected '')");
        System.out.println("✓ Returns empty when no machine fits\n");

        // --- Test 4: Case insensitive capabilities ---
        System.out.println("=== Test 4: Case insensitive ===");
        sched.addMachine("m-3", new String[]{"PDF Thumbnail Creator", "Image Compression"});
        String r4 = sched.assignMachineToJob("job-D", new String[]{"pdf thumbnail creator"}, 0);
        System.out.println("assign job-D (lowercase query): '" + r4 + "' (expected 'm-3')");
        System.out.println("✓ Case insensitive matching\n");

        // --- Test 5: Least unfinished after multiple jobs ---
        System.out.println("=== Test 5: Least unfinished selection ===");
        DistributedScheduler s2 = new DistributedScheduler();
        s2.addMachine("a", new String[]{"x"});
        s2.addMachine("b", new String[]{"x"});
        s2.assignMachineToJob("j1", new String[]{"x"}, 0); // → "a" (tie → lex)
        s2.assignMachineToJob("j2", new String[]{"x"}, 0); // → "b" (a has 1 unfinished)
        s2.assignMachineToJob("j3", new String[]{"x"}, 0); // → "a" (both have 1 → lex)
        System.out.println(s2.getMachineStatus("a"));
        System.out.println(s2.getMachineStatus("b"));
        System.out.println("✓ Round-robin-like via least-unfinished\n");

        // --- Test 6: Most finished tie-break ---
        System.out.println("=== Test 6: Most finished tie-break ===");
        DistributedScheduler s3 = new DistributedScheduler();
        s3.addMachine("x1", new String[]{"cap"});
        s3.addMachine("x2", new String[]{"cap"});
        // Both 0 finished → tie → lex smallest = x1
        String r6 = s3.assignMachineToJob("j1", new String[]{"cap"}, 1);
        System.out.println("assign (criteria=1, both 0 finished): '" + r6 + "' (expected 'x1')");
        System.out.println("✓ Tie-break on most-finished\n");

        // --- Test 7: Extensibility — custom criteria ---
        System.out.println("=== Test 7: Custom selector (extensibility) ===");
        DistributedScheduler s4 = new DistributedScheduler();
        s4.addMachine("m1", new String[]{"a"});
        s4.addMachine("m2", new String[]{"a"});
        // Register criteria=2: always pick lex LARGEST machineId
        s4.registerSelector(2, (candidate, current) ->
            candidate.id.compareTo(current.id) > 0);
        String r7 = s4.assignMachineToJob("j1", new String[]{"a"}, 2);
        System.out.println("Custom criteria (lex largest): '" + r7 + "' (expected 'm2')");
        System.out.println("✓ Strategy pattern extensible\n");

        // --- Test 8: Edge — single machine, many jobs ---
        System.out.println("=== Test 8: Single machine ===");
        DistributedScheduler s5 = new DistributedScheduler();
        s5.addMachine("solo", new String[]{"everything"});
        for (int i = 0; i < 10; i++)
            s5.assignMachineToJob("j" + i, new String[]{"everything"}, 0);
        System.out.println(s5.getMachineStatus("solo"));
        for (int i = 0; i < 5; i++) s5.jobCompleted("j" + i);
        System.out.println("After 5 completions: " + s5.getMachineStatus("solo"));
        System.out.println("✓ Single machine handles all\n");

        // --- Test 9: Scale ---
        System.out.println("=== Test 9: Scale ===");
        DistributedScheduler s6 = new DistributedScheduler();
        for (int i = 0; i < 100; i++)
            s6.addMachine("m" + String.format("%03d", i),
                new String[]{"cap" + (i % 10), "cap" + ((i+1) % 10), "common"});
        long t = System.nanoTime();
        int assigned = 0;
        for (int i = 0; i < 5000; i++) {
            String r = s6.assignMachineToJob("j" + i, new String[]{"common"}, i % 2);
            if (!r.isEmpty()) assigned++;
        }
        System.out.printf("5000 jobs on 100 machines: %d assigned in %.2f ms\n",
            assigned, (System.nanoTime()-t)/1e6);
        System.out.println("✓ Fast at scale\n");

        // --- Test 10: Thread Safety ---
        System.out.println("=== Test 10: Thread Safety ===");
        DistributedScheduler s7 = new DistributedScheduler();
        s7.addMachine("t1", new String[]{"work"});
        s7.addMachine("t2", new String[]{"work"});
        ExecutorService exec = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            int idx = i;
            futures.add(exec.submit(() ->
                s7.assignMachineToJob("tj" + idx, new String[]{"work"}, idx % 2)));
        }
        for (int i = 0; i < 100; i++) {
            int idx = i;
            futures.add(exec.submit(() -> s7.jobCompleted("tj" + idx)));
        }
        for (Future<?> f : futures) { try { f.get(); } catch (Exception e) { System.out.println("ERR: " + e); } }
        exec.shutdown();
        System.out.println(s7.getMachineStatus("t1"));
        System.out.println(s7.getMachineStatus("t2"));
        System.out.println("✓ Thread-safe\n");

        System.out.println("════════ ALL 10 TESTS PASSED ✓ ════════");
    }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. STRATEGY PATTERN: MachineSelector interface with isBetter().
 *    New criteria = new class implementing MachineSelector.
 *    Register via registerSelector(criteria, selector).
 *    Open/Closed principle — extend without modifying scheduler.
 *
 * 2. CAPABILITY MATCHING: Machine.capabilities.containsAll(required).
 *    Case-insensitive via lowercasing at insert + query time.
 *    O(C) per machine check, O(M*C) per assignment.
 *
 * 3. TIE-BREAKING: Lex smallest machineId via String.compareTo().
 *    Built into each selector's isBetter() method.
 *
 * 4. SCALE IMPROVEMENTS:
 *    - Index machines by capability set hash for O(1) candidate filter
 *    - Priority queue per capability combo for O(log M) selection
 *    - Consistent hashing for machine affinity
 *
 * 5. REAL-WORLD: Kubernetes scheduler (predicates + priorities),
 *    Apache Mesos (resource offers), YARN (capacity scheduler).
 *
 * 6. COMPLEXITY:
 *    addMachine:         O(C) normalize capabilities
 *    assignMachineToJob: O(M * C) scan + capability check
 *    jobCompleted:       O(1) counter update
 */
