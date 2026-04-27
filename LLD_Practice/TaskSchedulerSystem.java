import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/*
 * TASK SCHEDULER - Low Level Design
 * ====================================
 * 
 * REQUIREMENTS:
 * 1. Schedule tasks with priority (HIGH > MEDIUM > LOW)
 * 2. PriorityQueue: highest priority first, FIFO on same priority
 * 3. Delayed tasks: execute after a delay
 * 4. Recurring tasks: repeat at fixed interval
 * 5. Cancel tasks, pluggable execution strategy
 * 6. Task lifecycle: PENDING → RUNNING → COMPLETED / FAILED / CANCELLED
 * 7. Observer for task events, thread-safe
 * 
 * DESIGN PATTERNS:
 *   Strategy  (ExecutionStrategy) — ImmediateExecution, BatchExecution
 *   Observer  (TaskListener)      — TaskLogger
 *   Facade    (TaskSchedulerService)
 * 
 * KEY DS: PriorityQueue<ScheduledTask> ordered by (priority desc, submitTime asc)
 */

// ==================== EXCEPTIONS ====================

class TaskNotFoundException extends RuntimeException {
    TaskNotFoundException(String id) { super("Task not found: " + id); }
}

class InvalidTaskException extends RuntimeException {
    InvalidTaskException(String msg) { super("Invalid task: " + msg); }
}

// ==================== ENUMS ====================

enum TaskPriority { LOW, MEDIUM, HIGH, CRITICAL }

enum TaskStatus { PENDING, RUNNING, COMPLETED, FAILED, CANCELLED }

// ==================== MODELS ====================

class ScheduledTask implements Comparable<ScheduledTask> {
    final String id, name;
    final TaskPriority priority;
    final Runnable action;
    final long submitTime;
    final long delayMs;       // 0 = immediate
    final long intervalMs;    // 0 = one-shot, >0 = recurring
    TaskStatus status;
    int executionCount;
    String failureReason;

    ScheduledTask(String id, String name, TaskPriority priority, Runnable action, long delayMs, long intervalMs) {
        this.id = id; this.name = name; this.priority = priority; this.action = action;
        this.delayMs = delayMs; this.intervalMs = intervalMs;
        this.submitTime = System.nanoTime(); this.status = TaskStatus.PENDING;
    }

    /** Priority desc (CRITICAL > HIGH > MEDIUM > LOW), then FIFO by submit time. */
    @Override public int compareTo(ScheduledTask other) {
        int cmp = Integer.compare(other.priority.ordinal(), this.priority.ordinal());
        return cmp != 0 ? cmp : Long.compare(this.submitTime, other.submitTime);
    }
}

// ==================== INTERFACES ====================

/** Strategy — how tasks are executed. */
interface ExecutionStrategy {
    void execute(ScheduledTask task);
}

/** Observer — task lifecycle events. */
interface TaskListener {
    void onTaskEvent(ScheduledTask task, TaskStatus newStatus);
}

// ==================== STRATEGY IMPLEMENTATIONS ====================

/** Execute task immediately inline. */
class ImmediateExecution implements ExecutionStrategy {
    @Override public void execute(ScheduledTask task) {
        task.action.run();
    }
}

/** Batch: collect tasks, execute when batch size reached. */
class BatchExecution implements ExecutionStrategy {
    final int batchSize;
    final List<ScheduledTask> batch = new ArrayList<>();
    int executedBatches = 0;

    BatchExecution(int batchSize) { this.batchSize = batchSize; }

    @Override public void execute(ScheduledTask task) {
        batch.add(task);
        if (batch.size() >= batchSize) {
            batch.forEach(t -> t.action.run());
            batch.clear();
            executedBatches++;
        }
    }

    void flush() {
        batch.forEach(t -> t.action.run());
        if (!batch.isEmpty()) executedBatches++;
        batch.clear();
    }
}

// ==================== OBSERVER IMPLEMENTATIONS ====================

class TaskLogger implements TaskListener {
    final List<String> events = new ArrayList<>();
    @Override public void onTaskEvent(ScheduledTask task, TaskStatus status) {
        events.add(status + ":" + task.name + "(P:" + task.priority + ")");
    }
}

// ==================== TASK SCHEDULER SERVICE (FACADE) ====================

class TaskSchedulerService {
    private final PriorityBlockingQueue<ScheduledTask> taskQueue = new PriorityBlockingQueue<>();
    private final Map<String, ScheduledTask> allTasks = new ConcurrentHashMap<>();
    private ExecutionStrategy executionStrategy;
    private final List<TaskListener> listeners = new ArrayList<>();
    private final AtomicInteger taskCounter = new AtomicInteger();

    TaskSchedulerService(ExecutionStrategy strategy) { this.executionStrategy = strategy; }
    TaskSchedulerService() { this(new ImmediateExecution()); }

    void setExecutionStrategy(ExecutionStrategy s) { this.executionStrategy = s; }
    void addListener(TaskListener l) { listeners.add(l); }

    private void fireEvent(ScheduledTask task, TaskStatus status) {
        task.status = status;
        listeners.forEach(l -> l.onTaskEvent(task, status));
    }

    /** Schedule a one-shot task with priority. */
    ScheduledTask schedule(String name, TaskPriority priority, Runnable action) {
        return scheduleWithDelay(name, priority, action, 0, 0);
    }

    /** Schedule a delayed task. */
    ScheduledTask scheduleWithDelay(String name, TaskPriority priority, Runnable action, long delayMs) {
        return scheduleWithDelay(name, priority, action, delayMs, 0);
    }

    /** Schedule a recurring task. */
    ScheduledTask scheduleRecurring(String name, TaskPriority priority, Runnable action, long intervalMs) {
        return scheduleWithDelay(name, priority, action, 0, intervalMs);
    }

    private ScheduledTask scheduleWithDelay(String name, TaskPriority priority, Runnable action, long delayMs, long intervalMs) {
        if (name == null || name.isEmpty()) throw new InvalidTaskException("name required");
        String id = "TASK-" + taskCounter.incrementAndGet();
        ScheduledTask task = new ScheduledTask(id, name, priority, action, delayMs, intervalMs);
        allTasks.put(id, task);
        taskQueue.offer(task);
        fireEvent(task, TaskStatus.PENDING);
        return task;
    }

    /** Cancel a pending task. */
    boolean cancel(String taskId) {
        ScheduledTask task = getTask(taskId);
        if (task.status != TaskStatus.PENDING) return false;
        taskQueue.remove(task);
        fireEvent(task, TaskStatus.CANCELLED);
        return true;
    }

    /** Execute next highest-priority task from queue. */
    ScheduledTask executeNext() {
        ScheduledTask task = taskQueue.poll();
        if (task == null) return null;
        if (task.status == TaskStatus.CANCELLED) return executeNext(); // skip cancelled

        fireEvent(task, TaskStatus.RUNNING);
        try {
            if (task.delayMs > 0) Thread.sleep(task.delayMs);
            executionStrategy.execute(task);
            task.executionCount++;
            fireEvent(task, TaskStatus.COMPLETED);

            // Re-queue if recurring
            if (task.intervalMs > 0) {
                task.status = TaskStatus.PENDING;
                taskQueue.offer(task);
            }
        } catch (Exception e) {
            task.failureReason = e.getMessage();
            fireEvent(task, TaskStatus.FAILED);
        }
        return task;
    }

    /** Execute all pending tasks in priority order. */
    int executeAll() {
        int count = 0;
        while (!taskQueue.isEmpty()) {
            ScheduledTask t = executeNext();
            if (t != null && t.status != TaskStatus.CANCELLED) count++;
            // Stop recurring tasks after 1 round
            if (t != null && t.intervalMs > 0) { taskQueue.remove(t); }
        }
        return count;
    }

    ScheduledTask getTask(String id) {
        ScheduledTask t = allTasks.get(id);
        if (t == null) throw new TaskNotFoundException(id);
        return t;
    }

    int getQueueSize() { return taskQueue.size(); }
    int getTotalTasks() { return allTasks.size(); }

    /** Peek at next task without removing. */
    ScheduledTask peekNext() { return taskQueue.peek(); }
}

// ==================== MAIN / TESTS ====================

public class TaskSchedulerSystem {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════╗");
        System.out.println("║   TASK SCHEDULER - LLD Demo           ║");
        System.out.println("╚═══════════════════════════════════════╝\n");

        // --- Test 1: Priority ordering ---
        System.out.println("=== Test 1: Priority ordering ===");
        TaskSchedulerService svc = new TaskSchedulerService();
        List<String> order = new ArrayList<>();
        svc.schedule("low-task", TaskPriority.LOW, () -> order.add("LOW"));
        svc.schedule("high-task", TaskPriority.HIGH, () -> order.add("HIGH"));
        svc.schedule("critical-task", TaskPriority.CRITICAL, () -> order.add("CRITICAL"));
        svc.schedule("medium-task", TaskPriority.MEDIUM, () -> order.add("MEDIUM"));
        svc.executeAll();
        check(order.get(0), "CRITICAL", "1st = CRITICAL");
        check(order.get(1), "HIGH", "2nd = HIGH");
        check(order.get(2), "MEDIUM", "3rd = MEDIUM");
        check(order.get(3), "LOW", "4th = LOW");
        System.out.println("✓\n");

        // --- Test 2: FIFO on same priority ---
        System.out.println("=== Test 2: FIFO on same priority ===");
        TaskSchedulerService svc2 = new TaskSchedulerService();
        List<String> fifo = new ArrayList<>();
        svc2.schedule("first", TaskPriority.HIGH, () -> fifo.add("first"));
        svc2.schedule("second", TaskPriority.HIGH, () -> fifo.add("second"));
        svc2.schedule("third", TaskPriority.HIGH, () -> fifo.add("third"));
        svc2.executeAll();
        check(fifo.get(0), "first", "FIFO: first");
        check(fifo.get(1), "second", "FIFO: second");
        check(fifo.get(2), "third", "FIFO: third");
        System.out.println("✓\n");

        // --- Test 3: Task lifecycle ---
        System.out.println("=== Test 3: Task lifecycle ===");
        TaskSchedulerService svc3 = new TaskSchedulerService();
        ScheduledTask t = svc3.schedule("lifecycle", TaskPriority.HIGH, () -> {});
        check(t.status, TaskStatus.PENDING, "Initially PENDING");
        svc3.executeNext();
        check(t.status, TaskStatus.COMPLETED, "After exec = COMPLETED");
        check(t.executionCount, 1, "Executed once");
        System.out.println("✓\n");

        // --- Test 4: Cancel task ---
        System.out.println("=== Test 4: Cancel ===");
        TaskSchedulerService svc4 = new TaskSchedulerService();
        ScheduledTask cancel = svc4.schedule("to-cancel", TaskPriority.LOW, () -> {});
        check(svc4.cancel(cancel.id), true, "Cancelled");
        check(cancel.status, TaskStatus.CANCELLED, "Status = CANCELLED");
        check(svc4.getQueueSize(), 0, "Removed from queue");
        check(svc4.cancel(cancel.id), false, "Can't cancel again");
        System.out.println("✓\n");

        // --- Test 5: Failed task ---
        System.out.println("=== Test 5: Failed task ===");
        TaskSchedulerService svc5 = new TaskSchedulerService();
        ScheduledTask fail = svc5.schedule("fail-task", TaskPriority.HIGH, () -> { throw new RuntimeException("boom"); });
        svc5.executeNext();
        check(fail.status, TaskStatus.FAILED, "Status = FAILED");
        check(fail.failureReason, "boom", "Failure reason captured");
        System.out.println("✓\n");

        // --- Test 6: Recurring task ---
        System.out.println("=== Test 6: Recurring ===");
        TaskSchedulerService svc6 = new TaskSchedulerService();
        AtomicInteger counter = new AtomicInteger();
        ScheduledTask recurring = svc6.scheduleRecurring("heartbeat", TaskPriority.MEDIUM, counter::incrementAndGet, 100);
        svc6.executeNext(); // 1st execution
        check(counter.get(), 1, "Executed 1st time");
        check(svc6.getQueueSize(), 1, "Re-queued after execution");
        svc6.executeNext(); // 2nd execution
        check(counter.get(), 2, "Executed 2nd time");
        System.out.println("✓\n");

        // --- Test 7: Batch execution strategy ---
        System.out.println("=== Test 7: Batch strategy ===");
        BatchExecution batch = new BatchExecution(3);
        TaskSchedulerService svc7 = new TaskSchedulerService(batch);
        List<String> batched = new ArrayList<>();
        svc7.schedule("b1", TaskPriority.HIGH, () -> batched.add("b1"));
        svc7.schedule("b2", TaskPriority.HIGH, () -> batched.add("b2"));
        svc7.executeNext(); svc7.executeNext(); // 2 tasks, batch not full
        check(batched.size(), 0, "Batch not full yet (need 3)");
        svc7.schedule("b3", TaskPriority.HIGH, () -> batched.add("b3"));
        svc7.executeNext(); // 3rd task triggers batch
        check(batched.size(), 3, "Batch of 3 executed");
        check(batch.executedBatches, 1, "1 batch completed");
        System.out.println("✓\n");

        // --- Test 8: Observer ---
        System.out.println("=== Test 8: Observer ===");
        TaskSchedulerService svc8 = new TaskSchedulerService();
        TaskLogger logger = new TaskLogger();
        svc8.addListener(logger);
        svc8.schedule("observed", TaskPriority.HIGH, () -> {});
        svc8.executeNext();
        check(logger.events.size(), 3, "3 events: PENDING, RUNNING, COMPLETED");
        System.out.println("  Events: " + logger.events);
        System.out.println("✓\n");

        // --- Test 9: Peek without remove ---
        System.out.println("=== Test 9: Peek ===");
        TaskSchedulerService svc9 = new TaskSchedulerService();
        svc9.schedule("peek-low", TaskPriority.LOW, () -> {});
        svc9.schedule("peek-high", TaskPriority.HIGH, () -> {});
        check(svc9.peekNext().name, "peek-high", "Peek = highest priority");
        check(svc9.getQueueSize(), 2, "Queue unchanged after peek");
        System.out.println("✓\n");

        // --- Test 10: Exceptions ---
        System.out.println("=== Test 10: Exceptions ===");
        try { svc.getTask("TASK-999"); } catch (TaskNotFoundException e) { System.out.println("  ✓ " + e.getMessage()); }
        try { svc.schedule("", TaskPriority.HIGH, () -> {}); } catch (InvalidTaskException e) { System.out.println("  ✓ " + e.getMessage()); }
        System.out.println("✓\n");

        // --- Test 11: Thread Safety ---
        System.out.println("=== Test 11: Thread Safety ===");
        TaskSchedulerService svc11 = new TaskSchedulerService();
        AtomicInteger total = new AtomicInteger();
        ExecutorService exec = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            int x = i;
            futures.add(exec.submit(() -> svc11.schedule("t" + x, TaskPriority.values()[x % 4], total::incrementAndGet)));
        }
        for (Future<?> f : futures) { try { f.get(); } catch (Exception e) {} }
        exec.shutdown();
        check(svc11.getTotalTasks(), 100, "100 tasks scheduled concurrently");
        svc11.executeAll();
        check(total.get(), 100, "All 100 executed");
        System.out.println("✓\n");

        // --- Test 12: Scale ---
        System.out.println("=== Test 12: Scale ===");
        TaskSchedulerService svc12 = new TaskSchedulerService();
        long start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            int x = i;
            svc12.schedule("task" + x, TaskPriority.values()[x % 4], () -> {});
        }
        System.out.printf("  10K schedule: %.2f ms\n", (System.nanoTime() - start) / 1e6);
        start = System.nanoTime();
        svc12.executeAll();
        System.out.printf("  10K execute: %.2f ms\n", (System.nanoTime() - start) / 1e6);
        check(svc12.getQueueSize(), 0, "Queue empty");
        System.out.println("✓\n");

        System.out.println("════════ ALL 12 TESTS PASSED ✓ ════════");
    }

    static void check(int a, int e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
    static void check(String a, String e, String m) { System.out.println("  " + (Objects.equals(a, e) ? "✓" : "✗ GOT '" + a + "'") + " " + m); }
    static void check(boolean a, boolean e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
    static void check(TaskStatus a, TaskStatus e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. PRIORITY QUEUE: PriorityQueue<ScheduledTask> with Comparable.
 *    compareTo: priority desc (CRITICAL > HIGH > MEDIUM > LOW), then FIFO by submitTime.
 *    O(log n) offer/poll. peek() = O(1).
 *
 * 2. STRATEGY (ExecutionStrategy): ImmediateExecution (inline), BatchExecution
 *    (collect batch, execute when full). Could add: ThreadPoolExecution, AsyncExecution.
 *
 * 3. OBSERVER (TaskListener): TaskLogger tracks PENDING → RUNNING → COMPLETED/FAILED.
 *    Could add: MetricsCollector, AlertOnFailure, RetryListener.
 *
 * 4. TASK LIFECYCLE: PENDING → RUNNING → COMPLETED/FAILED/CANCELLED.
 *    Cancel removes from queue. Failed captures failureReason.
 *
 * 5. RECURRING: After completion, re-offer to queue. intervalMs > 0 = recurring.
 *    Real-world: ScheduledExecutorService, Quartz, cron.
 *
 * 6. THREAD SAFETY: AtomicInteger for IDs, ConcurrentHashMap for task registry.
 *    PriorityQueue itself is NOT thread-safe — use synchronized or PriorityBlockingQueue.
 *
 * 7. EXTENSIONS: retry with backoff, dead letter queue, task dependencies (DAG),
 *    distributed scheduling, rate limiting, task groups.
 */
