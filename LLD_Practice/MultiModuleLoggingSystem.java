import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/*
 * MULTI-MODULE LOGGING FRAMEWORK - Low Level Design
 * =====================================================
 *
 * REQUIREMENTS:
 * 1. Three modules (Module1, Module2, Module3) each running on their own thread
 * 2. Each module produces log data and writes it to a shared buffer
 * 3. A framework consumer thread reads from the buffer and writes to file/sink
 * 4. ACK mechanism: consumer acknowledges back to the producer when log is persisted
 * 5. Three threads exchanging data: producer threads → buffer → consumer thread → ACK back
 * 6. Thread-safe R/W on the shared buffer
 * 7. Graceful shutdown: drain remaining logs before stopping
 *
 * KEY DATA STRUCTURES:
 * - LinkedBlockingQueue<LogEntry>: shared buffer between producers and consumer
 * - CompletableFuture<WriteAck>: each log entry carries a future; consumer completes
 *   it after writing to sink, producer can wait on it for acknowledgment
 * - CopyOnWriteArrayList<String>: simulated file (thread-safe append-only list)
 *
 * DESIGN PATTERNS:
 * - Producer-Consumer: modules produce, framework consumes via BlockingQueue
 * - Future/Promise: ACK communicated back via CompletableFuture per log entry
 * - Observer: optional listeners notified on successful writes
 *
 * THREAD MODEL:
 *   [Module1 Thread] ──write──╲
 *   [Module2 Thread] ──write──→ [BlockingQueue] ──read──→ [Consumer Thread] ──→ [File/Sink]
 *   [Module3 Thread] ──write──╱                                    │
 *         ╲                                                        │
 *          ╰──── CompletableFuture<WriteAck> ◄── completed ────────╯
 *
 * COMPLEXITY:
 *   produce (enqueue):  O(1) amortized (BlockingQueue offer)
 *   consume (dequeue):  O(1) (BlockingQueue poll)
 *   ACK (future.get):   O(1) once consumer completes it
 */

// ==================== DOMAIN CLASSES ====================

enum LogSeverity { DEBUG, INFO, WARN, ERROR }

/** Acknowledgment returned to producer after log is persisted */
class WriteAck {
    final String logId;
    final long writtenAt;
    final boolean success;
    final String error;      // null if success

    WriteAck(String logId, boolean success, String error) {
        this.logId = logId; this.writtenAt = System.currentTimeMillis();
        this.success = success; this.error = error;
    }

    static WriteAck ok(String logId) { return new WriteAck(logId, true, null); }
    static WriteAck fail(String logId, String err) { return new WriteAck(logId, false, err); }
}

/** A single log entry — carries a CompletableFuture for ACK back to producer */
class LogEntry {
    final String logId;
    final String moduleName;
    final LogSeverity severity;
    final String message;
    final long producedAt;
    final CompletableFuture<WriteAck> ackFuture;  // producer waits on this for ACK

    LogEntry(String logId, String moduleName, LogSeverity severity, String message) {
        this.logId = logId; this.moduleName = moduleName;
        this.severity = severity; this.message = message;
        this.producedAt = System.currentTimeMillis();
        this.ackFuture = new CompletableFuture<>();
    }

    String format() {
        return String.format("[%d] [%s] [%s] %s: %s", producedAt, logId, severity, moduleName, message);
    }
}

// ==================== SINK INTERFACE ====================

/** Where the consumer writes logs to — Strategy pattern */
interface LogSinkWriter {
    void write(String formattedLog) throws Exception;
    int getWriteCount();
}

/** Simulated file sink — stores logs in a list (thread-safe for verification) */
class FileLogSink implements LogSinkWriter {
    private final List<String> lines = new CopyOnWriteArrayList<>();

    public void write(String formattedLog) {
        // TODO: Implement
        // HINT: lines.add(formattedLog);
    }

    public int getWriteCount() { return lines.size(); }
    List<String> getLines() { return Collections.unmodifiableList(lines); }
}

/** Flaky sink — fails every N writes (for testing error ACKs) */
class FlakySink implements LogSinkWriter {
    private final AtomicInteger count = new AtomicInteger(0);
    private final int failEvery;

    FlakySink(int failEvery) { this.failEvery = failEvery; }

    public void write(String formattedLog) throws Exception {
        // TODO: Implement
        // HINT: if (count.incrementAndGet() % failEvery == 0)
        // HINT:     throw new Exception("Simulated write failure");
    }

    public int getWriteCount() { return count.get(); }
}

// ==================== WRITE LISTENER (Observer) ====================

interface WriteListener {
    void onWrite(String logId, String moduleName, boolean success);
}

class WriteLogger implements WriteListener {
    final List<String> events = new CopyOnWriteArrayList<>();
    public void onWrite(String logId, String moduleName, boolean success) {
        events.add(logId + ":" + (success ? "OK" : "FAIL"));
    }
}

// ==================== LOG BUFFER (Shared Queue) ====================

/**
 * The shared buffer between producer (module) threads and consumer (framework) thread.
 * Wraps a BlockingQueue with bounded capacity for backpressure.
 */
class LogBuffer {
    private final BlockingQueue<LogEntry> queue;
    private final AtomicInteger enqueuedCount = new AtomicInteger(0);
    private final AtomicInteger droppedCount = new AtomicInteger(0);

    LogBuffer(int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
    }

    /**
     * Producer calls this to enqueue a log entry.
     * Returns false if buffer is full (backpressure — entry dropped).
     */
    boolean offer(LogEntry entry) {
        // TODO: Implement
        // HINT: boolean accepted = queue.offer(entry);
        // HINT: if (accepted) enqueuedCount.incrementAndGet();
        // HINT: else droppedCount.incrementAndGet();
        // HINT: return accepted;
        return false;
    }

    /**
     * Blocking offer — waits up to timeout for space. Used when producers must not drop.
     */
    boolean offer(LogEntry entry, long timeoutMs) throws InterruptedException {
        // TODO: Implement
        // HINT: boolean accepted = queue.offer(entry, timeoutMs, TimeUnit.MILLISECONDS);
        // HINT: if (accepted) enqueuedCount.incrementAndGet();
        // HINT: else droppedCount.incrementAndGet();
        // HINT: return accepted;
        return false;
    }

    /**
     * Consumer calls this to dequeue. Blocks up to timeout.
     * Returns null if no entry available within timeout.
     */
    LogEntry poll(long timeoutMs) throws InterruptedException {
        // TODO: Implement
        // HINT: return queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        return null;
    }

    int size() { return queue.size(); }
    int getEnqueuedCount() { return enqueuedCount.get(); }
    int getDroppedCount() { return droppedCount.get(); }
    boolean isEmpty() { return queue.isEmpty(); }
}

// ==================== CONSUMER THREAD (Framework Reader) ====================

/**
 * The framework thread that:
 *   1. Reads log entries from the shared buffer
 *   2. Writes them to the sink (file/tool)
 *   3. Completes the CompletableFuture on each entry → ACK back to producer
 *
 * This is the "framework thread reading from buffer and ack when written to file"
 */
class LogConsumer implements Runnable {
    private final LogBuffer buffer;
    private final LogSinkWriter sink;
    private final List<WriteListener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean running = true;
    private final AtomicInteger processedCount = new AtomicInteger(0);

    LogConsumer(LogBuffer buffer, LogSinkWriter sink) {
        this.buffer = buffer; this.sink = sink;
    }

    void addListener(WriteListener l) { listeners.add(l); }

    @Override
    public void run() {
        // TODO: Implement
        // HINT: while (running || !buffer.isEmpty()) {    // drain remaining on shutdown
        // HINT:     try {
        // HINT:         LogEntry entry = buffer.poll(100); // wait up to 100ms
        // HINT:         if (entry == null) continue;       // timeout, check running flag
        // HINT:
        // HINT:         try {
        // HINT:             sink.write(entry.format());    // write to file/sink
        // HINT:             entry.ackFuture.complete(WriteAck.ok(entry.logId));  // ACK success
        // HINT:             for (WriteListener l : listeners)
        // HINT:                 l.onWrite(entry.logId, entry.moduleName, true);
        // HINT:         } catch (Exception e) {
        // HINT:             entry.ackFuture.complete(WriteAck.fail(entry.logId, e.getMessage()));  // ACK failure
        // HINT:             for (WriteListener l : listeners)
        // HINT:                 l.onWrite(entry.logId, entry.moduleName, false);
        // HINT:         }
        // HINT:         processedCount.incrementAndGet();
        // HINT:     } catch (InterruptedException e) {
        // HINT:         Thread.currentThread().interrupt();
        // HINT:         break;
        // HINT:     }
        // HINT: }
    }

    void shutdown() { running = false; }
    int getProcessedCount() { return processedCount.get(); }
}

// ==================== PRODUCER (Module) ====================

/**
 * A module that produces logs. Runs on its own thread.
 * Writes log entries to the shared buffer, then can optionally wait for ACK.
 *
 * Flow: produce log → enqueue to buffer → (optionally) wait for ACK future
 */
class LogModule {
    final String moduleName;
    private final LogBuffer buffer;
    private final AtomicInteger seqNum = new AtomicInteger(0);

    LogModule(String moduleName, LogBuffer buffer) {
        this.moduleName = moduleName; this.buffer = buffer;
    }

    /**
     * Fire-and-forget: enqueue log, don't wait for ACK.
     * Returns the future so caller can optionally check later.
     */
    CompletableFuture<WriteAck> log(LogSeverity severity, String message) {
        // TODO: Implement
        // HINT: String logId = moduleName + "-" + seqNum.incrementAndGet();
        // HINT: LogEntry entry = new LogEntry(logId, moduleName, severity, message);
        // HINT: boolean accepted = buffer.offer(entry);
        // HINT: if (!accepted) {
        // HINT:     entry.ackFuture.complete(WriteAck.fail(logId, "Buffer full — dropped"));
        // HINT: }
        // HINT: return entry.ackFuture;
        return null;
    }

    /**
     * Blocking log with ACK: enqueue and WAIT for the consumer to write + ACK.
     * This is how "ack when it is written to the file" works.
     */
    WriteAck logAndWaitAck(LogSeverity severity, String message, long timeoutMs)
            throws InterruptedException, ExecutionException, TimeoutException {
        // TODO: Implement
        // HINT: CompletableFuture<WriteAck> future = log(severity, message);
        // HINT: return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        return null;
    }
}

// ==================== FRAMEWORK ORCHESTRATOR ====================

/**
 * Ties everything together: creates buffer, consumer thread, and provides
 * module registration. This is the "logging framework" that modules interact with.
 */
class LoggingEngine {
    private final LogBuffer buffer;
    private final LogConsumer consumer;
    private final Thread consumerThread;
    private final Map<String, LogModule> modules = new ConcurrentHashMap<>();

    LoggingEngine(int bufferCapacity, LogSinkWriter sink) {
        this.buffer = new LogBuffer(bufferCapacity);
        this.consumer = new LogConsumer(buffer, sink);
        this.consumerThread = new Thread(consumer, "LogConsumer");
    }

    void start() { consumerThread.start(); }

    LogModule registerModule(String name) {
        // TODO: Implement
        // HINT: LogModule module = new LogModule(name, buffer);
        // HINT: modules.put(name, module);
        // HINT: return module;
        return null;
    }

    void addWriteListener(WriteListener l) { consumer.addListener(l); }

    /** Graceful shutdown: signal consumer to stop, wait for it to drain buffer */
    void shutdown() throws InterruptedException {
        // TODO: Implement
        // HINT: consumer.shutdown();
        // HINT: consumerThread.join(5000);  // wait up to 5s for consumer to finish
    }

    int getBufferSize() { return buffer.size(); }
    int getTotalEnqueued() { return buffer.getEnqueuedCount(); }
    int getTotalDropped() { return buffer.getDroppedCount(); }
    int getTotalProcessed() { return consumer.getProcessedCount(); }
}

// ==================== MAIN / TESTS ====================

public class MultiModuleLoggingSystem {
    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║  MULTI-MODULE LOGGING - 3 Thread Producer/Consumer║");
        System.out.println("╚══════════════════════════════════════════════════╝\n");

        // --- Test 1: Basic 3-module setup ---
        System.out.println("=== Test 1: Basic 3-module fire-and-forget ===");
        FileLogSink sink1 = new FileLogSink();
        LoggingEngine engine1 = new LoggingEngine(100, sink1);
        engine1.start();

        LogModule mod1 = engine1.registerModule("AuthModule");
        LogModule mod2 = engine1.registerModule("PaymentModule");
        LogModule mod3 = engine1.registerModule("OrderModule");

        mod1.log(LogSeverity.INFO, "User logged in");
        mod2.log(LogSeverity.INFO, "Payment processed");
        mod3.log(LogSeverity.INFO, "Order created");

        Thread.sleep(200); // let consumer process
        engine1.shutdown();
        System.out.println("File lines: " + sink1.getWriteCount() + " (expected 3)");
        sink1.getLines().forEach(l -> System.out.println("  " + l));
        System.out.println("✓ 3 modules → buffer → consumer → file\n");

        // --- Test 2: ACK mechanism — producer waits for confirmation ---
        System.out.println("=== Test 2: Log with ACK (producer waits for write confirmation) ===");
        FileLogSink sink2 = new FileLogSink();
        LoggingEngine engine2 = new LoggingEngine(100, sink2);
        engine2.start();

        LogModule ackMod = engine2.registerModule("AuditModule");

        // Producer sends log AND waits for ACK — blocks until consumer writes to file
        WriteAck ack1 = ackMod.logAndWaitAck(LogSeverity.WARN, "Suspicious login attempt", 2000);
        System.out.println("ACK received: logId=" + ack1.logId + ", success=" + ack1.success);

        WriteAck ack2 = ackMod.logAndWaitAck(LogSeverity.ERROR, "Failed auth from unknown IP", 2000);
        System.out.println("ACK received: logId=" + ack2.logId + ", success=" + ack2.success);

        engine2.shutdown();
        System.out.println("✓ Producer received ACK after consumer wrote to file\n");

        // --- Test 3: Three concurrent module threads ---
        System.out.println("=== Test 3: 3 concurrent module threads ===");
        FileLogSink sink3 = new FileLogSink();
        LoggingEngine engine3 = new LoggingEngine(200, sink3);
        engine3.start();

        LogModule m1 = engine3.registerModule("Module1");
        LogModule m2 = engine3.registerModule("Module2");
        LogModule m3 = engine3.registerModule("Module3");

        int logsPerModule = 20;
        CountDownLatch producersDone = new CountDownLatch(3);

        // 3 producer threads, each writing to the shared buffer
        for (LogModule m : new LogModule[]{m1, m2, m3}) {
            new Thread(() -> {
                for (int i = 0; i < logsPerModule; i++) {
                    m.log(LogSeverity.INFO, "Activity " + i);
                }
                producersDone.countDown();
            }, m.moduleName + "-Thread").start();
        }

        producersDone.await(5, TimeUnit.SECONDS);
        Thread.sleep(500); // let consumer drain
        engine3.shutdown();

        System.out.println("Total enqueued: " + engine3.getTotalEnqueued() + " (expected " + (3 * logsPerModule) + ")");
        System.out.println("Total processed: " + engine3.getTotalProcessed());
        System.out.println("Total in file: " + sink3.getWriteCount());
        System.out.println("Buffer remaining: " + engine3.getBufferSize());
        System.out.println("✓ All 3 threads wrote to shared buffer, consumer drained to file\n");

        // --- Test 4: ACK with concurrent producers ---
        System.out.println("=== Test 4: Concurrent producers with ACK collection ===");
        FileLogSink sink4 = new FileLogSink();
        LoggingEngine engine4 = new LoggingEngine(200, sink4);
        engine4.start();

        LogModule cm1 = engine4.registerModule("Mod-A");
        LogModule cm2 = engine4.registerModule("Mod-B");
        LogModule cm3 = engine4.registerModule("Mod-C");

        List<CompletableFuture<WriteAck>> allFutures = new CopyOnWriteArrayList<>();
        CountDownLatch done4 = new CountDownLatch(3);

        for (LogModule cm : new LogModule[]{cm1, cm2, cm3}) {
            new Thread(() -> {
                for (int i = 0; i < 10; i++) {
                    CompletableFuture<WriteAck> f = cm.log(LogSeverity.INFO, "Event " + i);
                    allFutures.add(f);
                }
                done4.countDown();
            }, cm.moduleName + "-Thread").start();
        }

        done4.await(5, TimeUnit.SECONDS);

        // Wait for all ACKs
        CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);
        long successCount = allFutures.stream().filter(f -> {
            try { return f.get().success; } catch (Exception e) { return false; }
        }).count();
        engine4.shutdown();

        System.out.println("Total futures: " + allFutures.size() + ", successes: " + successCount);
        System.out.println("✓ All producers collected ACKs from consumer\n");

        // --- Test 5: Error ACK (flaky sink) ---
        System.out.println("=== Test 5: Error ACK from flaky sink ===");
        FlakySink flaky = new FlakySink(3); // fails every 3rd write
        LoggingEngine engine5 = new LoggingEngine(100, flaky);
        engine5.start();

        LogModule errMod = engine5.registerModule("ErrorModule");
        int oks = 0, fails = 0;
        for (int i = 0; i < 6; i++) {
            WriteAck ack = errMod.logAndWaitAck(LogSeverity.INFO, "msg-" + i, 2000);
            if (ack.success) oks++;
            else { fails++; System.out.println("  FAIL ACK: " + ack.error); }
        }
        engine5.shutdown();
        System.out.println("OK: " + oks + ", FAIL: " + fails + " (expected 2 failures at write 3 and 6)");
        System.out.println("✓ Producer received failure ACKs for bad writes\n");

        // --- Test 6: Backpressure (buffer full → drop) ---
        System.out.println("=== Test 6: Backpressure — tiny buffer ===");
        FileLogSink sink6 = new FileLogSink();
        LoggingEngine engine6 = new LoggingEngine(3, sink6);  // buffer size = 3
        // Don't start consumer — buffer will fill up!

        LogModule bpMod = engine6.registerModule("FastModule");
        int accepted = 0, dropped = 0;
        for (int i = 0; i < 10; i++) {
            CompletableFuture<WriteAck> f = bpMod.log(LogSeverity.INFO, "flood-" + i);
            try {
                WriteAck a = f.get(100, TimeUnit.MILLISECONDS);
                if (!a.success) dropped++;
                else accepted++;
            } catch (TimeoutException e) {
                accepted++; // still in buffer, not yet processed
            }
        }
        System.out.println("Accepted/pending: " + accepted + ", Dropped: " + dropped);
        System.out.println("Buffer enqueued: " + engine6.getTotalEnqueued() + ", dropped: " + engine6.getTotalDropped());

        engine6.start(); // now start consumer to drain
        Thread.sleep(300);
        engine6.shutdown();
        System.out.println("✓ Backpressure: excess logs dropped when buffer full\n");

        // --- Test 7: Write listener (Observer) ---
        System.out.println("=== Test 7: Write listener notifications ===");
        FileLogSink sink7 = new FileLogSink();
        LoggingEngine engine7 = new LoggingEngine(100, sink7);
        WriteLogger wl = new WriteLogger();
        engine7.addWriteListener(wl);
        engine7.start();

        LogModule obsMod = engine7.registerModule("ObservedModule");
        obsMod.logAndWaitAck(LogSeverity.INFO, "event-1", 2000);
        obsMod.logAndWaitAck(LogSeverity.ERROR, "event-2", 2000);
        engine7.shutdown();

        System.out.println("Listener events: " + wl.events);
        System.out.println("✓ Observer notified on each write\n");

        // --- Test 8: Graceful shutdown drains buffer ---
        System.out.println("=== Test 8: Graceful shutdown drains remaining ===");
        FileLogSink sink8 = new FileLogSink();
        LoggingEngine engine8 = new LoggingEngine(100, sink8);
        engine8.start();

        LogModule drainMod = engine8.registerModule("DrainModule");
        for (int i = 0; i < 50; i++) {
            drainMod.log(LogSeverity.INFO, "batch-" + i);
        }
        // Immediately shutdown — consumer should drain remaining
        engine8.shutdown();
        System.out.println("Enqueued: " + engine8.getTotalEnqueued() + ", Processed: " + engine8.getTotalProcessed());
        System.out.println("File lines: " + sink8.getWriteCount());
        System.out.println("✓ Graceful shutdown drained buffer\n");

        // --- Test 9: Multiple severity levels ---
        System.out.println("=== Test 9: Multiple severity levels ===");
        FileLogSink sink9 = new FileLogSink();
        LoggingEngine engine9 = new LoggingEngine(100, sink9);
        engine9.start();

        LogModule sevMod = engine9.registerModule("SeverityModule");
        sevMod.logAndWaitAck(LogSeverity.DEBUG, "debug info", 2000);
        sevMod.logAndWaitAck(LogSeverity.INFO, "informational", 2000);
        sevMod.logAndWaitAck(LogSeverity.WARN, "warning issued", 2000);
        sevMod.logAndWaitAck(LogSeverity.ERROR, "error occurred", 2000);
        engine9.shutdown();

        sink9.getLines().forEach(l -> System.out.println("  " + l));
        System.out.println("✓ All severity levels persisted\n");

        // --- Test 10: High-throughput stress test ---
        System.out.println("=== Test 10: Stress test — 3 threads × 1000 logs ===");
        FileLogSink sink10 = new FileLogSink();
        LoggingEngine engine10 = new LoggingEngine(5000, sink10);
        engine10.start();

        LogModule s1 = engine10.registerModule("Stress-1");
        LogModule s2 = engine10.registerModule("Stress-2");
        LogModule s3 = engine10.registerModule("Stress-3");

        long t0 = System.nanoTime();
        CountDownLatch done10 = new CountDownLatch(3);
        for (LogModule s : new LogModule[]{s1, s2, s3}) {
            new Thread(() -> {
                for (int i = 0; i < 1000; i++)
                    s.log(LogSeverity.INFO, "stress-" + i);
                done10.countDown();
            }).start();
        }
        done10.await(5, TimeUnit.SECONDS);
        Thread.sleep(2000); // let consumer drain
        engine10.shutdown();

        long elapsed = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("Enqueued: " + engine10.getTotalEnqueued());
        System.out.println("Processed: " + engine10.getTotalProcessed());
        System.out.println("File lines: " + sink10.getWriteCount());
        System.out.printf("Time: %d ms%n", elapsed);
        System.out.println("✓ High throughput 3-thread stress test\n");

        System.out.println("════════ ALL 10 TESTS PASSED ✓ ════════");
    }
}

/*
 * INTERVIEW NOTES:
 *
 * 1. CORE DESIGN — 3 THREAD PRODUCER-CONSUMER WITH ACK:
 *
 *    Thread 1,2,3 (Modules):  produce LogEntry → enqueue to BlockingQueue
 *    Thread 4 (Consumer):     dequeue from BlockingQueue → write to Sink → complete Future
 *
 *    The CompletableFuture<WriteAck> on each LogEntry is the ACK mechanism:
 *    - Producer creates LogEntry with an empty future
 *    - Producer enqueues entry, optionally calls future.get() to block for ACK
 *    - Consumer dequeues, writes to file, then calls future.complete(WriteAck)
 *    - Producer's future.get() unblocks with the ACK (success or failure)
 *
 * 2. WHY BLOCKINGQUEUE:
 *    - Thread-safe: no explicit locking needed for R/W
 *    - Bounded: provides backpressure when producers are faster than consumer
 *    - Decouples producers from consumer: modules don't know about the file
 *    - LinkedBlockingQueue: O(1) enqueue/dequeue, separate head/tail locks
 *    - Alternative: ArrayBlockingQueue (fixed array, single lock, slightly faster)
 *
 * 3. ACK MECHANISM — COMPLETABLEFUTURE VS ALTERNATIVES:
 *    | Approach            | Pros                        | Cons                     |
 *    |--------------------|-----------------------------|--------------------------|
 *    | CompletableFuture  | Per-entry ACK, composable   | Object per log entry     |
 *    | Callback/Listener  | Simple, no blocking         | No per-entry tracking    |
 *    | ACK Queue          | Batched, efficient          | Complex routing back     |
 *    | CountDownLatch     | Simple wait-for-all         | No per-entry granularity |
 *
 *    CompletableFuture is ideal here because:
 *    - Each log entry has its own ACK (fine-grained)
 *    - Producer can choose fire-and-forget OR wait-for-ACK
 *    - Composable: allOf() to wait for batch ACKs
 *
 * 4. BACKPRESSURE STRATEGIES:
 *    - Drop newest (what we do with offer()): fast, loses latest data
 *    - Drop oldest: keeps recent, needs custom eviction
 *    - Block producer (put()): guarantees delivery, may slow modules
 *    - Timed wait (offer with timeout): balance between drop and block
 *
 * 5. GRACEFUL SHUTDOWN:
 *    - Set running=false on consumer
 *    - Consumer loop: `while (running || !buffer.isEmpty())` — drains remaining
 *    - Join consumer thread with timeout
 *    - Alternative: use poison pill (special LogEntry that signals shutdown)
 *
 * 6. SCALABILITY:
 *    - Multiple consumer threads: partition by module name or round-robin
 *    - Batched writes: consumer dequeues N entries, writes batch to file
 *    - Ring buffer (LMAX Disruptor): lock-free, pre-allocated, ~100M msgs/sec
 *    - Async I/O: NIO FileChannel for non-blocking file writes
 *    - External sink: Kafka/Kinesis for durability + multiple consumers
 *
 * 7. REAL-WORLD PARALLELS:
 *    - Log4j2 AsyncAppender: BlockingQueue + daemon consumer thread
 *    - LMAX Disruptor: ring buffer, used in Log4j2 for ultra-low latency
 *    - Kafka: producer→topic→consumer with offset-based ACK
 *    - Logstash: pipeline of input→filter→output with backpressure
 *    - Java Flight Recorder: lock-free ring buffer for JVM events
 *
 * 8. COMPLEXITY:
 *    | Operation         | Time     | Notes                           |
 *    |------------------|----------|---------------------------------|
 *    | log (enqueue)    | O(1)     | BlockingQueue offer             |
 *    | consume (dequeue)| O(1)     | BlockingQueue poll              |
 *    | ACK (future.get) | O(1)     | Unparks when completed          |
 *    | shutdown + drain | O(k)     | k = remaining entries in buffer |
 */
