import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// ===== EXCEPTIONS =====

class JobNotFoundException extends Exception {
    public JobNotFoundException(String id) { super("Job not found: " + id); }
}

class WorkerNotFoundException extends Exception {
    public WorkerNotFoundException(String id) { super("Worker not found: " + id); }
}

// ===== ENUMS =====

enum ScheduleType { IMMEDIATE, ONE_TIME, CRON }

enum JobStatus { ACTIVE, PAUSED }

enum ExecutionStatus { QUEUED, RUNNING, SUCCEEDED, FAILED, RETRYING }

enum JobPriority { LOW, MEDIUM, HIGH, CRITICAL }

// ===== DOMAIN CLASSES =====

/**
 * Job Definition — what to run, when, and retry policy
 * Maps to: Entity 1 from HLD (job_definitions table)
 */
class JobDefinition {
    private final String jobId;
    private final String name;
    private final String callbackUrl;        // URL to call when job triggers
    private final String payload;            // data to send with callback
    private final ScheduleType scheduleType;
    private final String cronExpression;     // only for CRON (e.g., "*/5 * * * *")
    private LocalDateTime scheduledTime;     // only for ONE_TIME
    private LocalDateTime nextRunAt;         // when to trigger next
    private final JobPriority priority;
    private JobStatus status;
    private final int maxRetries;
    private final long retryBackoffMs;       // base backoff for exponential retry
    private final LocalDateTime createdAt;
    
    public JobDefinition(String name, String callbackUrl, String payload,
            ScheduleType scheduleType, String cronExpression, LocalDateTime scheduledTime,
            JobPriority priority, int maxRetries, long retryBackoffMs) {
        this.jobId = "JOB-" + UUID.randomUUID().toString().substring(0, 6);
        this.name = name;
        this.callbackUrl = callbackUrl;
        this.payload = payload;
        this.scheduleType = scheduleType;
        this.cronExpression = cronExpression;
        this.scheduledTime = scheduledTime;
        this.priority = priority;
        this.status = JobStatus.ACTIVE;
        this.maxRetries = maxRetries;
        this.retryBackoffMs = retryBackoffMs;
        this.createdAt = LocalDateTime.now();
        
        // Set initial nextRunAt
        if (scheduleType == ScheduleType.IMMEDIATE) this.nextRunAt = LocalDateTime.now();
        else if (scheduleType == ScheduleType.ONE_TIME) this.nextRunAt = scheduledTime;
        else this.nextRunAt = LocalDateTime.now(); // CRON: simplified, would parse cron
    }
    
    public String getJobId() { return jobId; }
    public String getName() { return name; }
    public String getCallbackUrl() { return callbackUrl; }
    public String getPayload() { return payload; }
    public ScheduleType getScheduleType() { return scheduleType; }
    public String getCronExpression() { return cronExpression; }
    public LocalDateTime getNextRunAt() { return nextRunAt; }
    public JobPriority getPriority() { return priority; }
    public JobStatus getStatus() { return status; }
    public int getMaxRetries() { return maxRetries; }
    public long getRetryBackoffMs() { return retryBackoffMs; }
    
    public void setStatus(JobStatus s) { this.status = s; }
    public void setNextRunAt(LocalDateTime t) { this.nextRunAt = t; }
    
    @Override
    public String toString() {
        return jobId + "[" + name + ", " + scheduleType + ", " + priority + ", " + status 
            + ", next=" + nextRunAt + "]";
    }
}

/**
 * Job Execution — one run of a job (history record)
 * Maps to: Entity 2 from HLD (job_executions table)
 */
class JobExecution {
    private final String executionId;
    private final String jobId;
    private String workerId;
    private ExecutionStatus status;
    private int attemptNumber;
    private final LocalDateTime scheduledTime;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String result;
    private String error;
    
    public JobExecution(String jobId, LocalDateTime scheduledTime) {
        this.executionId = "EXEC-" + UUID.randomUUID().toString().substring(0, 6);
        this.jobId = jobId;
        this.status = ExecutionStatus.QUEUED;
        this.attemptNumber = 0;
        this.scheduledTime = scheduledTime;
    }
    
    public String getExecutionId() { return executionId; }
    public String getJobId() { return jobId; }
    public String getWorkerId() { return workerId; }
    public ExecutionStatus getStatus() { return status; }
    public int getAttemptNumber() { return attemptNumber; }
    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public String getResult() { return result; }
    public String getError() { return error; }
    
    public void setWorkerId(String id) { this.workerId = id; }
    public void setStatus(ExecutionStatus s) { this.status = s; }
    public void incrementAttempt() { this.attemptNumber++; }
    public void setStartedAt(LocalDateTime t) { this.startedAt = t; }
    public void setFinishedAt(LocalDateTime t) { this.finishedAt = t; }
    public void setResult(String r) { this.result = r; }
    public void setError(String e) { this.error = e; }
    
    @Override
    public String toString() {
        return executionId + "[job=" + jobId + ", " + status + ", attempt=" + attemptNumber 
            + (workerId != null ? ", worker=" + workerId : "") + "]";
    }
}

/**
 * Worker — processes job executions
 * Maps to: Entity 3 from HLD (workers table)
 */
class Worker {
    private final String workerId;
    private boolean alive;
    private LocalDateTime lastHeartbeat;
    private int runningJobs;
    private final int maxCapacity;
    
    public Worker(String workerId, int maxCapacity) {
        this.workerId = workerId;
        this.alive = true;
        this.lastHeartbeat = LocalDateTime.now();
        this.runningJobs = 0;
        this.maxCapacity = maxCapacity;
    }
    
    public String getWorkerId() { return workerId; }
    public boolean isAlive() { return alive; }
    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public int getRunningJobs() { return runningJobs; }
    public int getMaxCapacity() { return maxCapacity; }
    public boolean hasCapacity() { return alive && runningJobs < maxCapacity; }
    
    public void setAlive(boolean a) { this.alive = a; }
    public void heartbeat() { this.lastHeartbeat = LocalDateTime.now(); }
    public void incrementRunning() { this.runningJobs++; }
    public void decrementRunning() { if (runningJobs > 0) runningJobs--; }
    
    @Override
    public String toString() { return workerId + "[alive=" + alive + ", jobs=" + runningJobs + "/" + maxCapacity + "]"; }
}

// ===== INTERFACE =====

/**
 * Simulates job execution (in real system: HTTP callback to callbackUrl)
 */
interface JobExecutor {
    boolean execute(JobDefinition job, JobExecution execution) throws Exception;
}

/** Simulated executor: succeeds most of the time */
class SimulatedExecutor implements JobExecutor {
    private final double successRate;
    private final Random random = new Random();
    
    public SimulatedExecutor(double successRate) { this.successRate = successRate; }
    
    @Override
    public boolean execute(JobDefinition job, JobExecution execution) throws Exception {
        Thread.sleep(30); // simulate work
        if (random.nextDouble() < successRate) return true;
        throw new RuntimeException("Callback failed for " + job.getCallbackUrl());
    }
}

class AlwaysSucceedExecutor implements JobExecutor {
    @Override
    public boolean execute(JobDefinition job, JobExecution execution) { return true; }
}

class AlwaysFailExecutor implements JobExecutor {
    @Override
    public boolean execute(JobDefinition job, JobExecution execution) throws Exception {
        throw new RuntimeException("Service unavailable");
    }
}

// ===== SERVICE =====

/**
 * Job Scheduler System - Low Level Design (LLD)
 * 
 * Based on: Hello Interview Job Scheduler HLD
 * 
 * PROBLEM: Design a distributed job scheduler that supports:
 * 1. IMMEDIATE jobs (ad-hoc, run now)
 * 2. ONE_TIME jobs (run at scheduled time)
 * 3. CRON jobs (recurring schedule)
 * 4. Retry with exponential backoff
 * 5. Execution history
 * 6. Worker management with heartbeats
 * 7. Priority-based execution (CRITICAL > HIGH > MEDIUM > LOW)
 * 
 * KEY ARCHITECTURE (from HLD):
 *   API → JobDefinition DB → Scheduler polls due jobs → Queue → Workers execute
 *   Separation: Scheduler (WHEN) is separate from Worker (HOW)
 * 
 * PATTERNS: Strategy (executor), Producer-Consumer (queue + workers)
 */
class JobScheduler {
    private final Map<String, JobDefinition> jobs;
    private final Map<String, JobExecution> executions;          // executionId → execution
    private final Map<String, List<String>> jobExecutionHistory; // jobId → [executionIds]
    private final PriorityBlockingQueue<JobExecution> queue;     // priority queue
    private final Map<String, Worker> workers;
    private JobExecutor executor;
    private final AtomicInteger totalSucceeded;
    private final AtomicInteger totalFailed;
    
    public JobScheduler(JobExecutor executor) {
        this.jobs = new ConcurrentHashMap<>();
        this.executions = new ConcurrentHashMap<>();
        this.jobExecutionHistory = new ConcurrentHashMap<>();
        this.queue = new PriorityBlockingQueue<>(100, (a, b) -> {
            JobDefinition ja = jobs.get(a.getJobId());
            JobDefinition jb = jobs.get(b.getJobId());
            if (ja == null || jb == null) return 0;
            return jb.getPriority().ordinal() - ja.getPriority().ordinal();
        });
        this.workers = new ConcurrentHashMap<>();
        this.executor = executor;
        this.totalSucceeded = new AtomicInteger(0);
        this.totalFailed = new AtomicInteger(0);
    }
    
    // ===== JOB CRUD =====
    
    /**
     * Create a job definition
     * 
     * IMPLEMENTATION HINTS:
     * 1. Create JobDefinition with given params
     * 2. Store in jobs map
     * 3. Init empty execution history list
     * 4. If IMMEDIATE → trigger immediately
     * 5. Return job
     */
    public JobDefinition createJob(String name, String callbackUrl, String payload,
            ScheduleType scheduleType, String cronExpr, LocalDateTime scheduledTime,
            JobPriority priority, int maxRetries, long retryBackoffMs) {
        // TODO: Implement
        // HINT: JobDefinition job = new JobDefinition(name, callbackUrl, payload,
        //     scheduleType, cronExpr, scheduledTime, priority, maxRetries, retryBackoffMs);
        // HINT: jobs.put(job.getJobId(), job);
        // HINT: jobExecutionHistory.put(job.getJobId(), new ArrayList<>());
        // HINT: System.out.println("  ✓ Created: " + job);
        // HINT: if (scheduleType == ScheduleType.IMMEDIATE) triggerJob(job.getJobId());
        // HINT: return job;
        return null;
    }
    
    /** Convenience: create immediate job */
    public JobDefinition createImmediateJob(String name, String callbackUrl, String payload,
            JobPriority priority, int maxRetries) {
        return createJob(name, callbackUrl, payload, ScheduleType.IMMEDIATE, null, null,
            priority, maxRetries, 1000);
    }
    
    /** Convenience: create one-time scheduled job */
    public JobDefinition createOneTimeJob(String name, String callbackUrl, String payload,
            LocalDateTime runAt, JobPriority priority, int maxRetries) {
        return createJob(name, callbackUrl, payload, ScheduleType.ONE_TIME, null, runAt,
            priority, maxRetries, 1000);
    }
    
    /** Convenience: create cron job */
    public JobDefinition createCronJob(String name, String callbackUrl, String payload,
            String cronExpr, JobPriority priority, int maxRetries) {
        return createJob(name, callbackUrl, payload, ScheduleType.CRON, cronExpr, null,
            priority, maxRetries, 2000);
    }
    
    /**
     * Pause/Resume a job
     */
    public void pauseJob(String jobId) throws JobNotFoundException {
        // TODO: Implement
        // HINT: JobDefinition job = jobs.get(jobId);
        // HINT: if (job == null) throw new JobNotFoundException(jobId);
        // HINT: job.setStatus(JobStatus.PAUSED);
        // HINT: System.out.println("  ⏸️ Paused: " + jobId);
    }
    
    public void resumeJob(String jobId) throws JobNotFoundException {
        // TODO: Implement
        // HINT: JobDefinition job = jobs.get(jobId);
        // HINT: if (job == null) throw new JobNotFoundException(jobId);
        // HINT: job.setStatus(JobStatus.ACTIVE);
        // HINT: System.out.println("  ▶️ Resumed: " + jobId);
    }
    
    // ===== TRIGGER & EXECUTE =====
    
    /**
     * Trigger a job — creates an execution and adds to queue
     * (Called by scheduler when nextRunAt is due, or for ad-hoc trigger)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get job → validate exists and is ACTIVE
     * 2. Create JobExecution with QUEUED status
     * 3. Store in executions map
     * 4. Add executionId to job's history
     * 5. Add to priority queue
     * 6. Process queue
     */
    public JobExecution triggerJob(String jobId) {
        // TODO: Implement
        // HINT: JobDefinition job = jobs.get(jobId);
        // HINT: if (job == null || job.getStatus() != JobStatus.ACTIVE) return null;
        //
        // HINT: JobExecution exec = new JobExecution(jobId, LocalDateTime.now());
        // HINT: executions.put(exec.getExecutionId(), exec);
        // HINT: jobExecutionHistory.get(jobId).add(exec.getExecutionId());
        // HINT: queue.offer(exec);
        // HINT: System.out.println("  ⚡ Triggered: " + exec);
        // HINT: processQueue();
        // HINT: return exec;
        return null;
    }
    
    /**
     * Process the queue — assign executions to available workers
     * 
     * IMPLEMENTATION HINTS:
     * 1. While queue not empty:
     * 2. Poll next execution (highest priority)
     * 3. Find available worker (hasCapacity)
     * 4. If no worker available → put back in queue, break
     * 5. Assign worker, call executeJob()
     */
    private void processQueue() {
        // TODO: Implement
        // HINT: while (!queue.isEmpty()) {
        //     JobExecution exec = queue.poll();
        //     if (exec == null) break;
        //     Worker worker = findAvailableWorker();
        //     if (worker == null) { queue.offer(exec); break; }
        //     executeJob(exec, worker);
        // }
    }
    
    private Worker findAvailableWorker() {
        for (Worker w : workers.values()) {
            if (w.hasCapacity()) return w;
        }
        return null;
    }
    
    /**
     * Execute a job on a worker with retry logic
     * 
     * IMPLEMENTATION HINTS:
     * 1. Set execution: status=RUNNING, workerId, startedAt, incrementAttempt
     * 2. Worker incrementRunning
     * 3. Get JobDefinition for this execution
     * 4. Try: executor.execute(job, execution)
     *    On success:
     *    → status=SUCCEEDED, result="OK", finishedAt, totalSucceeded++
     *    → For CRON: calculate nextRunAt (simplified: now + 60s)
     *    On failure:
     *    → If attemptNumber < maxRetries: status=RETRYING, re-add to queue
     *      (Exponential backoff: wait retryBackoffMs * 2^attempt — simulated)
     *    → Else: status=FAILED, error=message, totalFailed++
     * 5. Worker decrementRunning
     */
    private void executeJob(JobExecution exec, Worker worker) {
        // TODO: Implement
        // HINT: exec.setStatus(ExecutionStatus.RUNNING);
        // HINT: exec.setWorkerId(worker.getWorkerId());
        // HINT: exec.setStartedAt(LocalDateTime.now());
        // HINT: exec.incrementAttempt();
        // HINT: worker.incrementRunning();
        //
        // HINT: JobDefinition job = jobs.get(exec.getJobId());
        //
        // HINT: try {
        //     executor.execute(job, exec);
        //     exec.setStatus(ExecutionStatus.SUCCEEDED);
        //     exec.setResult("OK");
        //     exec.setFinishedAt(LocalDateTime.now());
        //     totalSucceeded.incrementAndGet();
        //     System.out.println("    ✅ " + exec.getExecutionId() + " succeeded on " + worker.getWorkerId());
        //
        //     // CRON: schedule next run
        //     if (job.getScheduleType() == ScheduleType.CRON) {
        //         job.setNextRunAt(LocalDateTime.now().plusSeconds(60)); // simplified
        //     }
        // } catch (Exception e) {
        //     if (exec.getAttemptNumber() < job.getMaxRetries()) {
        //         exec.setStatus(ExecutionStatus.RETRYING);
        //         long backoff = job.getRetryBackoffMs() * (long) Math.pow(2, exec.getAttemptNumber());
        //         System.out.println("    🔄 Retry " + exec.getAttemptNumber() + "/" + job.getMaxRetries()
        //             + " (backoff " + backoff + "ms) for " + exec.getExecutionId());
        //         queue.offer(exec);
        //         processQueue();
        //     } else {
        //         exec.setStatus(ExecutionStatus.FAILED);
        //         exec.setError(e.getMessage());
        //         exec.setFinishedAt(LocalDateTime.now());
        //         totalFailed.incrementAndGet();
        //         System.out.println("    ❌ " + exec.getExecutionId() + " failed: " + e.getMessage());
        //     }
        // } finally {
        //     worker.decrementRunning();
        // }
    }
    
    // ===== WORKER MANAGEMENT =====
    
    /**
     * Register a worker
     * 
     * IMPLEMENTATION HINTS:
     * 1. Create Worker with given capacity
     * 2. Store in workers map
     */
    public Worker registerWorker(String workerId, int capacity) {
        // TODO: Implement
        // HINT: Worker w = new Worker(workerId, capacity);
        // HINT: workers.put(workerId, w);
        // HINT: System.out.println("  ✓ Worker registered: " + w);
        // HINT: return w;
        return null;
    }
    
    /**
     * Worker heartbeat
     */
    public void workerHeartbeat(String workerId) {
        // TODO: Implement
        // HINT: Worker w = workers.get(workerId);
        // HINT: if (w != null) w.heartbeat();
    }
    
    /**
     * Check stale workers (no heartbeat in timeout) → mark dead
     */
    public void checkWorkerHealth(long timeoutMs) {
        // TODO: Implement
        // HINT: LocalDateTime now = LocalDateTime.now();
        // HINT: for (Worker w : workers.values()) {
        //     if (w.isAlive() && Duration.between(w.getLastHeartbeat(), now).toMillis() > timeoutMs) {
        //         w.setAlive(false);
        //         System.out.println("    ⚠️ Worker " + w.getWorkerId() + " marked dead");
        //     }
        // }
    }
    
    // ===== QUERIES =====
    
    public JobDefinition getJob(String jobId) throws JobNotFoundException {
        JobDefinition j = jobs.get(jobId);
        if (j == null) throw new JobNotFoundException(jobId);
        return j;
    }
    
    /**
     * Get execution history for a job
     */
    public List<JobExecution> getExecutionHistory(String jobId) throws JobNotFoundException {
        // TODO: Implement
        // HINT: if (!jobExecutionHistory.containsKey(jobId)) throw new JobNotFoundException(jobId);
        // HINT: List<JobExecution> history = new ArrayList<>();
        // HINT: for (String execId : jobExecutionHistory.get(jobId)) {
        //     JobExecution e = executions.get(execId);
        //     if (e != null) history.add(e);
        // }
        // HINT: return history;
        return null;
    }
    
    public JobExecution getExecution(String executionId) { return executions.get(executionId); }
    
    public void setExecutor(JobExecutor ex) { this.executor = ex; }
    
    public void displayStatus() {
        System.out.println("\n--- Job Scheduler Status ---");
        System.out.println("Jobs: " + jobs.size() + ", Workers: " + workers.size() 
            + ", Queue: " + queue.size());
        System.out.println("Succeeded: " + totalSucceeded.get() + ", Failed: " + totalFailed.get());
        System.out.println("\nJobs:");
        jobs.values().forEach(j -> System.out.println("  " + j));
        System.out.println("Workers:");
        workers.values().forEach(w -> System.out.println("  " + w));
    }
}

// ===== MAIN TEST CLASS =====

public class JobSchedulerSystem {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Job Scheduler LLD (Hello Interview) ===\n");
        
        // Test 1: Setup — register workers, create immediate job
        System.out.println("=== Test 1: Immediate Job ===");
        JobScheduler scheduler = new JobScheduler(new AlwaysSucceedExecutor());
        scheduler.registerWorker("worker-1", 5);
        scheduler.registerWorker("worker-2", 5);
        
        JobDefinition j1 = scheduler.createImmediateJob("send-welcome-email",
            "https://email-svc/send", "{to:'alice@mail.com'}", JobPriority.HIGH, 2);
        if (j1 != null) {
            System.out.println("✓ Created: " + j1);
            List<JobExecution> history = scheduler.getExecutionHistory(j1.getJobId());
            System.out.println("  Executions: " + (history != null ? history.size() : 0));
        }
        System.out.println();
        
        // Test 2: One-time scheduled job
        System.out.println("=== Test 2: One-Time Job ===");
        JobDefinition j2 = scheduler.createOneTimeJob("generate-report",
            "https://report-svc/generate", "{type:'Q4'}", 
            LocalDateTime.now().plusHours(1), JobPriority.MEDIUM, 3);
        System.out.println("✓ Scheduled: " + j2);
        // Manually trigger (simulating scheduler poll)
        if (j2 != null) {
            JobExecution exec = scheduler.triggerJob(j2.getJobId());
            System.out.println("  Triggered: " + exec);
        }
        System.out.println();
        
        // Test 3: Cron job (recurring)
        System.out.println("=== Test 3: Cron Job ===");
        JobDefinition j3 = scheduler.createCronJob("cleanup-logs",
            "https://cleanup-svc/run", "{olderThan:'30d'}", 
            "0 * * * *", JobPriority.LOW, 1);
        System.out.println("✓ Cron job: " + j3);
        if (j3 != null) {
            scheduler.triggerJob(j3.getJobId()); // simulate first cron trigger
            scheduler.triggerJob(j3.getJobId()); // simulate second trigger
        }
        System.out.println();
        
        // Test 4: Priority ordering
        System.out.println("=== Test 4: Priority Ordering ===");
        JobScheduler pScheduler = new JobScheduler(new AlwaysSucceedExecutor());
        pScheduler.registerWorker("w1", 1); // only 1 slot
        
        JobDefinition low = pScheduler.createJob("low-task", "http://x", "{}", 
            ScheduleType.IMMEDIATE, null, null, JobPriority.LOW, 0, 0);
        JobDefinition crit = pScheduler.createJob("critical-task", "http://x", "{}",
            ScheduleType.IMMEDIATE, null, null, JobPriority.CRITICAL, 0, 0);
        System.out.println("✓ CRITICAL should be processed before LOW");
        System.out.println();
        
        // Test 5: Retry with exponential backoff
        System.out.println("=== Test 5: Retry on Failure ===");
        JobScheduler retryScheduler = new JobScheduler(new AlwaysFailExecutor());
        retryScheduler.registerWorker("rw1", 3);
        
        JobDefinition failJob = retryScheduler.createImmediateJob("flaky-payment",
            "https://payment-svc/charge", "{amount:99.99}", JobPriority.HIGH, 3);
        
        if (failJob != null) {
            List<JobExecution> execs = retryScheduler.getExecutionHistory(failJob.getJobId());
            System.out.println("✓ Execution history: " + (execs != null ? execs.size() : 0));
            if (execs != null && !execs.isEmpty()) {
                JobExecution last = execs.get(execs.size() - 1);
                System.out.println("  Last status: " + last.getStatus());
                System.out.println("  Attempts: " + last.getAttemptNumber());
            }
        }
        System.out.println();
        
        // Test 6: Pause/Resume job
        System.out.println("=== Test 6: Pause/Resume ===");
        try {
            if (j3 != null) {
                scheduler.pauseJob(j3.getJobId());
                JobExecution paused = scheduler.triggerJob(j3.getJobId());
                System.out.println("✓ Trigger while paused: " + paused + " (expect null)");
                
                scheduler.resumeJob(j3.getJobId());
                JobExecution resumed = scheduler.triggerJob(j3.getJobId());
                System.out.println("✓ Trigger after resume: " + resumed);
            }
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 7: Execution history
        System.out.println("=== Test 7: Execution History ===");
        try {
            if (j3 != null) {
                List<JobExecution> history = scheduler.getExecutionHistory(j3.getJobId());
                System.out.println("✓ History for " + j3.getName() + ":");
                if (history != null) history.forEach(e -> System.out.println("    " + e));
            }
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 8: Worker heartbeat & health check
        System.out.println("=== Test 8: Worker Health ===");
        scheduler.workerHeartbeat("worker-1");
        // Simulate worker-2 going stale
        Thread.sleep(100);
        scheduler.checkWorkerHealth(50); // 50ms timeout (worker-2 will be stale)
        System.out.println("✓ Checked worker health");
        System.out.println();
        
        // Test 9: Exception — job not found
        System.out.println("=== Test 9: Exception - Job Not Found ===");
        try {
            scheduler.getJob("FAKE-ID");
            System.out.println("✗ Should have thrown");
        } catch (JobNotFoundException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        }
        System.out.println();
        
        // Display
        scheduler.displayStatus();
        
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION (aligned with Hello Interview HLD):
 * ========================================================
 * 
 * 1. CORE ENTITIES:
 *    JobDefinition: WHAT to run, WHEN (schedule), retry policy
 *    JobExecution:  ONE run of a job (history record, tracks attempts)
 *    Worker:        WHO executes (heartbeat, capacity)
 * 
 * 2. KEY SEPARATION — SCHEDULER vs WORKER:
 *    Scheduler: "WHEN to run" — polls DB for due jobs, enqueues
 *    Worker:    "HOW to run"  — picks from queue, calls callback
 *    Why: scale independently, scheduler is lightweight, workers are heavy
 * 
 * 3. SCHEDULE TYPES:
 *    IMMEDIATE: run now (ad-hoc trigger)
 *    ONE_TIME:  run at specific future time
 *    CRON:      recurring (parse cron expression → compute nextRunAt)
 * 
 * 4. RETRY WITH EXPONENTIAL BACKOFF:
 *    Attempt 1: wait 1s
 *    Attempt 2: wait 2s  
 *    Attempt 3: wait 4s
 *    Formula: backoffMs * 2^attempt
 *    After maxRetries → FAILED (dead letter queue in real system)
 * 
 * 5. AT-LEAST-ONCE DELIVERY:
 *    Job may execute more than once (retry, worker crash)
 *    Workers must be idempotent (use execution ID to deduplicate)
 * 
 * 6. THUNDERING HERD (top-of-minute):
 *    Many cron jobs trigger at :00 → burst
 *    Solution: jitter, pre-fetch, partition jobs across schedulers
 * 
 * 7. WORKER HEARTBEATS:
 *    Workers send heartbeat every 10-30s
 *    No heartbeat in timeout → mark dead, reassign in-flight jobs
 * 
 * 8. ARCHITECTURE:
 *    API → DB (job definitions) → Scheduler → Queue (Kafka) → Workers → Callback
 *                                                                  ↓
 *                                                          Execution History DB
 * 
 * 9. REAL-WORLD: Airflow, Celery, AWS EventBridge, Quartz, Temporal
 * 
 * 10. API:
 *     POST   /jobs                    — create job
 *     GET    /jobs/{id}               — get details
 *     PUT    /jobs/{id}/pause         — pause
 *     PUT    /jobs/{id}/resume        — resume
 *     POST   /jobs/{id}/trigger       — ad-hoc trigger
 *     GET    /jobs/{id}/executions    — execution history
 *     GET    /executions/{id}         — single execution details
 */
