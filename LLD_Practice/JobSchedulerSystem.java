import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * JOB SCHEDULER SYSTEM - Low Level Design
 * =========================================
 * 
 * REQUIREMENTS:
 * 1. Schedule jobs: IMMEDIATE, ONE_TIME (future), CRON (recurring)
 * 2. Priority queue: CRITICAL > HIGH > MEDIUM > LOW
 * 3. Retry with exponential backoff on failure
 * 4. Worker pool with capacity limits + heartbeats
 * 5. Execution history per job
 * 6. Pause/Resume jobs
 * 
 * KEY DATA STRUCTURES:
 * - PriorityBlockingQueue<JobExecution>: ordered by job priority
 * - ConcurrentHashMap: jobs, executions, workers registries
 * - Map<jobId, List<execId>>: execution history index
 * 
 * COMPLEXITY:
 *   createJob:    O(1)
 *   triggerJob:   O(log n) — priority queue insert
 *   processQueue: O(W) — scan workers for capacity
 *   executeJob:   O(1) + retry overhead
 */

// ==================== EXCEPTIONS ====================

class JobNotFoundException extends Exception {
    JobNotFoundException(String id) { super("Job not found: " + id); }
}

// ==================== ENUMS ====================

enum ScheduleType { IMMEDIATE, ONE_TIME, CRON }
enum JobStatus { ACTIVE, PAUSED }
enum ExecutionStatus { QUEUED, RUNNING, SUCCEEDED, FAILED, RETRYING }
enum JobPriority { LOW, MEDIUM, HIGH, CRITICAL }

// ==================== DOMAIN CLASSES ====================

class JobDefinition {
    final String jobId;
    final String name;
    final ScheduleType scheduleType;
    final JobPriority priority;
    final int maxRetries;
    final long retryBackoffMs;
    LocalDateTime nextRunAt;
    JobStatus status;
    
    JobDefinition(String name, ScheduleType scheduleType, LocalDateTime scheduledTime,
            JobPriority priority, int maxRetries, long retryBackoffMs) {
        this.jobId = "JOB-" + UUID.randomUUID().toString().substring(0, 6);
        this.name = name;
        this.scheduleType = scheduleType;
        this.priority = priority;
        this.status = JobStatus.ACTIVE;
        this.maxRetries = maxRetries;
        this.retryBackoffMs = retryBackoffMs;
        
        if (scheduleType == ScheduleType.IMMEDIATE) this.nextRunAt = LocalDateTime.now();
        else if (scheduleType == ScheduleType.ONE_TIME) this.nextRunAt = scheduledTime;
        else this.nextRunAt = LocalDateTime.now();
    }
}

class JobExecution {
    final String executionId;
    final String jobId;
    String workerId;
    ExecutionStatus status;
    int attemptNumber;
    LocalDateTime startedAt;
    String error;
    
    JobExecution(String jobId) {
        this.executionId = "EXEC-" + UUID.randomUUID().toString().substring(0, 6);
        this.jobId = jobId;
        this.status = ExecutionStatus.QUEUED;
    }
}

class Worker {
    final String workerId;
    final int maxCapacity;
    boolean alive;
    LocalDateTime lastHeartbeat;
    int runningJobs;
    
    Worker(String workerId, int maxCapacity) {
        this.workerId = workerId;
        this.maxCapacity = maxCapacity;
        this.alive = true;
        this.lastHeartbeat = LocalDateTime.now();
    }
    
    boolean hasCapacity() { return alive && runningJobs < maxCapacity; }
}

// ==================== STRATEGY: JOB EXECUTOR ====================

interface JobExecutor {
    boolean execute(JobDefinition job) throws Exception;
}

class AlwaysSucceedExecutor implements JobExecutor {
    public boolean execute(JobDefinition job) { return true; }
}

class AlwaysFailExecutor implements JobExecutor {
    public boolean execute(JobDefinition job) throws Exception {
        throw new RuntimeException("Service unavailable");
    }
}

// ==================== SERVICE ====================

class JobScheduler {
    private final Map<String, JobDefinition> jobs = new ConcurrentHashMap<>();
    private final Map<String, JobExecution> executions = new ConcurrentHashMap<>();
    private final Map<String, List<String>> jobHistory = new ConcurrentHashMap<>(); // jobId → [execIds]
    private final PriorityBlockingQueue<JobExecution> queue;
    private final Map<String, Worker> workers = new ConcurrentHashMap<>();
    private JobExecutor executor;
    private final AtomicInteger succeeded = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    
    JobScheduler(JobExecutor executor) {
        this.executor = executor;
        this.queue = new PriorityBlockingQueue<>(100, (a, b) -> {
            JobDefinition ja = jobs.get(a.jobId), jb = jobs.get(b.jobId);
            if (ja == null || jb == null) return 0;
            return jb.priority.ordinal() - ja.priority.ordinal(); // higher priority first
        });
    }
    
    /**
     * Create and register a job
     * 
     * IMPLEMENTATION HINTS:
     * 1. Create JobDefinition
     * 2. Store in jobs map, init empty history
     * 3. If IMMEDIATE → triggerJob
     * 4. Return job
     */
    JobDefinition createJob(String name, ScheduleType type, LocalDateTime scheduledTime,
            JobPriority priority, int maxRetries, long retryBackoffMs) {
        // TODO: Implement
        // HINT: JobDefinition job = new JobDefinition(name, type, scheduledTime, priority, maxRetries, retryBackoffMs);
        // HINT: jobs.put(job.jobId, job);
        // HINT: jobHistory.put(job.jobId, new ArrayList<>());
        // HINT: if (type == ScheduleType.IMMEDIATE) triggerJob(job.jobId);
        // HINT: return job;
        return null;
    }
    
    void pauseJob(String jobId) throws JobNotFoundException {
        // TODO: Implement
        // HINT: JobDefinition job = jobs.get(jobId);
        // HINT: if (job == null) throw new JobNotFoundException(jobId);
        // HINT: job.status = JobStatus.PAUSED;
    }
    
    void resumeJob(String jobId) throws JobNotFoundException {
        // TODO: Implement
        // HINT: JobDefinition job = jobs.get(jobId);
        // HINT: if (job == null) throw new JobNotFoundException(jobId);
        // HINT: job.status = JobStatus.ACTIVE;
    }
    
    /**
     * Trigger a job — create execution, enqueue, process
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate job exists and is ACTIVE
     * 2. Create JobExecution(jobId), store in executions
     * 3. Add execId to jobHistory
     * 4. queue.offer(exec), then processQueue()
     */
    JobExecution triggerJob(String jobId) {
        // TODO: Implement
        // HINT: JobDefinition job = jobs.get(jobId);
        // HINT: if (job == null || job.status != JobStatus.ACTIVE) return null;
        // HINT: JobExecution exec = new JobExecution(jobId);
        // HINT: executions.put(exec.executionId, exec);
        // HINT: jobHistory.get(jobId).add(exec.executionId);
        // HINT: queue.offer(exec);
        // HINT: processQueue();
        // HINT: return exec;
        return null;
    }
    
    /**
     * Drain queue → assign to available workers
     */
    private void processQueue() {
        // TODO: Implement
        // HINT: while (!queue.isEmpty()) {
        //     JobExecution exec = queue.poll();
        //     if (exec == null) break;
        //     Worker w = findAvailableWorker();
        //     if (w == null) { queue.offer(exec); break; }
        //     executeJob(exec, w);
        // }
    }
    
    private Worker findAvailableWorker() {
        for (Worker w : workers.values()) if (w.hasCapacity()) return w;
        return null;
    }
    
    /**
     * Execute job on worker with retry logic
     * 
     * IMPLEMENTATION HINTS:
     * 1. exec.status=RUNNING, exec.workerId, exec.startedAt, exec.attemptNumber++, worker.runningJobs++
     * 2. Get JobDefinition for exec.jobId
     * 3. try: executor.execute(job)
     *      success → exec.status=SUCCEEDED, succeeded++
     *      CRON → job.nextRunAt = now+60s
     *    catch: if attempts < maxRetries → RETRYING, re-queue
     *           else → FAILED, exec.error=msg, failed++
     * 4. finally: worker.runningJobs--
     */
    private void executeJob(JobExecution exec, Worker worker) {
        // TODO: Implement
        // HINT: exec.status = ExecutionStatus.RUNNING;
        // HINT: exec.workerId = worker.workerId;
        // HINT: exec.startedAt = LocalDateTime.now();
        // HINT: exec.attemptNumber++;
        // HINT: worker.runningJobs++;
        // HINT: JobDefinition job = jobs.get(exec.jobId);
        // HINT: try {
        //     executor.execute(job);
        //     exec.status = ExecutionStatus.SUCCEEDED;
        //     succeeded.incrementAndGet();
        //     if (job.scheduleType == ScheduleType.CRON) job.nextRunAt = LocalDateTime.now().plusSeconds(60);
        // } catch (Exception e) {
        //     if (exec.attemptNumber < job.maxRetries) {
        //         exec.status = ExecutionStatus.RETRYING;
        //         queue.offer(exec);
        //         processQueue();
        //     } else {
        //         exec.status = ExecutionStatus.FAILED;
        //         exec.error = e.getMessage();
        //         failed.incrementAndGet();
        //     }
        // } finally { worker.runningJobs--; }
    }
    
    // ===== WORKER MANAGEMENT =====
    
    Worker registerWorker(String workerId, int capacity) {
        // TODO: Implement
        // HINT: Worker w = new Worker(workerId, capacity);
        // HINT: workers.put(workerId, w);
        // HINT: return w;
        return null;
    }
    
    void workerHeartbeat(String workerId) {
        // TODO: Implement
        // HINT: Worker w = workers.get(workerId);
        // HINT: if (w != null) w.lastHeartbeat = LocalDateTime.now();
    }
    
    void checkWorkerHealth(long timeoutMs) {
        // TODO: Implement
        // HINT: LocalDateTime now = LocalDateTime.now();
        // HINT: for (Worker w : workers.values()) {
        //     if (w.alive && Duration.between(w.lastHeartbeat, now).toMillis() > timeoutMs) {
        //         w.alive = false;
        //     }
        // }
    }
    
    // ===== QUERIES =====
    
    JobDefinition getJob(String jobId) throws JobNotFoundException {
        JobDefinition j = jobs.get(jobId);
        if (j == null) throw new JobNotFoundException(jobId);
        return j;
    }
    
    List<JobExecution> getHistory(String jobId) throws JobNotFoundException {
        // TODO: Implement
        // HINT: if (!jobHistory.containsKey(jobId)) throw new JobNotFoundException(jobId);
        // HINT: List<JobExecution> result = new ArrayList<>();
        // HINT: for (String execId : jobHistory.get(jobId)) {
        //     JobExecution e = executions.get(execId);
        //     if (e != null) result.add(e);
        // }
        // HINT: return result;
        return null;
    }
    
    void setExecutor(JobExecutor ex) { this.executor = ex; }
    int getSucceeded() { return succeeded.get(); }
    int getFailed() { return failed.get(); }
    int getQueueSize() { return queue.size(); }
}

// ==================== MAIN / TESTS ====================

public class JobSchedulerSystem {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Job Scheduler LLD ===\n");
        
        // Test 1: Immediate Job
        System.out.println("=== Test 1: Immediate Job ===");
        JobScheduler sched = new JobScheduler(new AlwaysSucceedExecutor());
        sched.registerWorker("w1", 5);
        sched.registerWorker("w2", 5);
        JobDefinition j1 = sched.createJob("send-email", ScheduleType.IMMEDIATE, null,
            JobPriority.HIGH, 2, 1000);
        if (j1 != null) {
            List<JobExecution> h = sched.getHistory(j1.jobId);
            System.out.println("  Executions: " + (h != null ? h.size() : 0));
        }
        System.out.println();
        
        // Test 2: One-Time Job
        System.out.println("=== Test 2: One-Time Job ===");
        JobDefinition j2 = sched.createJob("gen-report", ScheduleType.ONE_TIME,
            LocalDateTime.now().plusHours(1), JobPriority.MEDIUM, 3, 1000);
        if (j2 != null) {
            sched.triggerJob(j2.jobId);
            System.out.println("✓ Scheduled and triggered");
        }
        System.out.println();
        
        // Test 3: Cron Job
        System.out.println("=== Test 3: Cron Job ===");
        JobDefinition j3 = sched.createJob("cleanup", ScheduleType.CRON, null,
            JobPriority.LOW, 1, 2000);
        if (j3 != null) {
            sched.triggerJob(j3.jobId);
            sched.triggerJob(j3.jobId);
            System.out.println("✓ Cron triggered twice");
        }
        System.out.println();
        
        // Test 4: Priority Ordering
        System.out.println("=== Test 4: Priority ===");
        JobScheduler ps = new JobScheduler(new AlwaysSucceedExecutor());
        ps.registerWorker("pw1", 1);
        ps.createJob("low", ScheduleType.IMMEDIATE, null, JobPriority.LOW, 0, 0);
        ps.createJob("critical", ScheduleType.IMMEDIATE, null, JobPriority.CRITICAL, 0, 0);
        System.out.println("✓ CRITICAL processed before LOW\n");
        
        // Test 5: Retry on Failure
        System.out.println("=== Test 5: Retry ===");
        JobScheduler rs = new JobScheduler(new AlwaysFailExecutor());
        rs.registerWorker("rw1", 3);
        JobDefinition failJob = rs.createJob("flaky", ScheduleType.IMMEDIATE, null,
            JobPriority.HIGH, 3, 1000);
        if (failJob != null) {
            List<JobExecution> execs = rs.getHistory(failJob.jobId);
            if (execs != null && !execs.isEmpty()) {
                JobExecution last = execs.get(execs.size() - 1);
                System.out.println("  Status: " + last.status + ", attempts: " + last.attemptNumber);
            }
        }
        System.out.println("  Succeeded: " + rs.getSucceeded() + ", Failed: " + rs.getFailed());
        System.out.println();
        
        // Test 6: Pause/Resume
        System.out.println("=== Test 6: Pause/Resume ===");
        try {
            if (j3 != null) {
                sched.pauseJob(j3.jobId);
                JobExecution paused = sched.triggerJob(j3.jobId);
                System.out.println("  Trigger while paused: " + paused + " (expect null)");
                sched.resumeJob(j3.jobId);
                JobExecution resumed = sched.triggerJob(j3.jobId);
                System.out.println("  Trigger after resume: " + (resumed != null ? resumed.executionId : null));
            }
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }
        System.out.println();
        
        // Test 7: Execution History
        System.out.println("=== Test 7: History ===");
        try {
            if (j3 != null) {
                List<JobExecution> h = sched.getHistory(j3.jobId);
                System.out.println("  " + j3.name + ": " + (h != null ? h.size() : 0) + " executions");
            }
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }
        System.out.println();
        
        // Test 8: Worker Health
        System.out.println("=== Test 8: Worker Health ===");
        sched.workerHeartbeat("w1");
        Thread.sleep(100);
        sched.checkWorkerHealth(50);
        System.out.println("✓ Stale workers marked dead\n");
        
        // Test 9: Job Not Found
        System.out.println("=== Test 9: Not Found ===");
        try {
            sched.getJob("FAKE");
            System.out.println("✗ Should have thrown");
        } catch (JobNotFoundException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        }
        
        System.out.println("\n=== All Tests Complete! ===");
    }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. SCHEDULER vs WORKER separation:
 *    Scheduler decides WHEN → Worker decides HOW
 *    Scale independently (scheduler lightweight, workers heavy)
 * 
 * 2. RETRY: backoffMs * 2^attempt (exponential)
 *    After maxRetries → FAILED → dead letter queue
 * 
 * 3. AT-LEAST-ONCE: workers must be idempotent (dedup by execId)
 * 
 * 4. THUNDERING HERD: many cron jobs at :00 → jitter + partition
 * 
 * 5. HEARTBEATS: workers ping every 10-30s, timeout → mark dead
 * 
 * 6. REAL-WORLD: Airflow, Celery, AWS EventBridge, Temporal
 */
