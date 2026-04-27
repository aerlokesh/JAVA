import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/*
 * MESSAGE QUEUE (Producer-Consumer) - Low Level Design
 * ======================================================
 * Deep dive: multithreading, synchronization, mutex vs semaphore,
 * deadlock prevention, and concurrency trade-offs.
 *
 * REQUIREMENTS:
 * 1. Named topics with publish/subscribe semantics
 * 2. Multiple producers can publish to a topic concurrently
 * 3. Multiple consumers per topic, each with independent offset (consumer groups)
 * 4. Bounded buffer per topic with configurable capacity (backpressure)
 * 5. Blocking publish when full, blocking consume when empty (condition variables)
 * 6. Consumer groups: each group gets every message, but only one consumer
 *    within a group processes it (competing consumers)
 * 7. ACK mechanism: consumer must acknowledge before offset advances
 * 8. Thread-safe: demonstrate mutex, semaphore, ReadWriteLock, condition variables
 *
 * KEY DATA STRUCTURES:
 * - ReentrantLock + Condition (notFull/notEmpty): classic bounded buffer
 * - Semaphore: controls max concurrent consumers per topic
 * - ReadWriteLock: topic metadata reads vs writes
 * - ConcurrentHashMap<groupId, AtomicInteger offset>: per-group consumption tracking
 *
 * CONCURRENCY PATTERNS DEMONSTRATED:
 * 1. Mutex (ReentrantLock): exclusive access to buffer during produce/consume
 * 2. Condition Variables (notFull/notEmpty): efficient wait/signal for bounded buffer
 * 3. Semaphore: limit concurrent consumer threads per topic
 * 4. ReadWriteLock: many readers of topic list, exclusive writer for create/delete
 * 5. AtomicInteger: lock-free offset tracking
 * 6. Deadlock prevention: consistent lock ordering, tryLock with timeout
 *
 * COMPLEXITY:
 *   publish:   O(1) amortized (array-backed circular buffer)
 *   consume:   O(1) (read at offset)
 *   subscribe: O(1)
 */

// ==================== EXCEPTIONS ====================

class TopicNotFoundException extends Exception {
    TopicNotFoundException(String t) { super("Topic not found: " + t); }
}

class TopicFullException extends Exception {
    TopicFullException(String t) { super("Topic buffer full: " + t); }
}

class QueueShutdownException extends Exception {
    QueueShutdownException() { super("Queue has been shut down"); }
}

// ==================== DOMAIN CLASSES ====================

class QueueMessage {
    final String id, topic, payload;
    final long timestamp;
    final String producerId;

    QueueMessage(String id, String topic, String payload, String producerId) {
        this.id = id; this.topic = topic; this.payload = payload;
        this.producerId = producerId; this.timestamp = System.currentTimeMillis();
    }
}

// ==================== BOUNDED BUFFER (Core Concurrency) ====================

/**
 * Thread-safe bounded buffer using ReentrantLock + Condition Variables.
 *
 * WHY NOT synchronized?
 *   - synchronized provides one implicit condition (the monitor)
 *   - We need TWO conditions: notFull (for producers) and notEmpty (for consumers)
 *   - With synchronized, notifyAll() wakes ALL waiters (producers AND consumers)
 *   - With explicit Conditions, signal() wakes only the relevant waiters
 *
 * WHY NOT BlockingQueue?
 *   - BlockingQueue hides the concurrency — we want to demonstrate it explicitly
 *   - In an interview about mutex/semaphore/deadlock, showing raw locks is the point
 *
 * MUTEX vs SEMAPHORE (demonstrated here):
 *   - The ReentrantLock IS a mutex (binary semaphore) — exactly 1 thread in critical section
 *   - A Semaphore(N) allows N threads — used below for consumer concurrency limit
 *   - Mutex: lock/unlock by SAME thread. Semaphore: can be released by DIFFERENT thread.
 */
class BoundedBuffer {
    private final QueueMessage[] buffer;
    private int head, tail, count;
    private final int capacity;

    // MUTEX: exactly one thread can modify buffer at a time
    private final ReentrantLock lock = new ReentrantLock(true); // fair lock
    // CONDITION VARIABLES: efficient wait/signal (no busy-waiting)
    private final Condition notFull = lock.newCondition();   // producers wait here
    private final Condition notEmpty = lock.newCondition();  // consumers wait here

    private volatile boolean shutdown = false;

    BoundedBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new QueueMessage[capacity];
        this.head = 0; this.tail = 0; this.count = 0;
    }

    /**
     * BLOCKING PUBLISH: waits if buffer is full.
     * Uses Condition.await() — releases lock and suspends thread until signaled.
     *
     * DEADLOCK PREVENTION: lock is always released in finally block.
     * If we used two locks (one for head, one for tail) we'd risk deadlock
     * unless we enforce a consistent ordering. Single lock avoids this entirely.
     */
    void put(QueueMessage msg) throws InterruptedException, QueueShutdownException {
        // TODO: Implement
        // HINT: lock.lock();
        // HINT: try {
        // HINT:     while (count == capacity) {  // ALWAYS use while, not if (spurious wakeups!)
        // HINT:         if (shutdown) throw new QueueShutdownException();
        // HINT:         notFull.await();          // release lock, wait for space
        // HINT:     }
        // HINT:     if (shutdown) throw new QueueShutdownException();
        // HINT:     buffer[tail] = msg;
        // HINT:     tail = (tail + 1) % capacity; // circular buffer
        // HINT:     count++;
        // HINT:     notEmpty.signal();             // wake ONE waiting consumer
        // HINT: } finally {
        // HINT:     lock.unlock();                 // ALWAYS unlock in finally (deadlock prevention)
        // HINT: }
    }

    /**
     * BLOCKING PUBLISH WITH TIMEOUT: returns false if buffer remains full.
     * tryLock + timed await = deadlock-safe pattern.
     */
    boolean offer(QueueMessage msg, long timeoutMs) throws InterruptedException {
        // TODO: Implement
        // HINT: if (!lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS)) return false;
        // HINT: try {
        // HINT:     if (count == capacity) {
        // HINT:         if (!notFull.await(timeoutMs, TimeUnit.MILLISECONDS)) return false;
        // HINT:     }
        // HINT:     if (count == capacity || shutdown) return false;
        // HINT:     buffer[tail] = msg;
        // HINT:     tail = (tail + 1) % capacity;
        // HINT:     count++;
        // HINT:     notEmpty.signal();
        // HINT:     return true;
        // HINT: } finally {
        // HINT:     lock.unlock();
        // HINT: }
        return false;
    }

    /**
     * BLOCKING CONSUME: waits if buffer is empty.
     */
    QueueMessage take() throws InterruptedException, QueueShutdownException {
        // TODO: Implement
        // HINT: lock.lock();
        // HINT: try {
        // HINT:     while (count == 0) {
        // HINT:         if (shutdown) throw new QueueShutdownException();
        // HINT:         notEmpty.await();
        // HINT:     }
        // HINT:     if (shutdown && count == 0) throw new QueueShutdownException();
        // HINT:     QueueMessage msg = buffer[head];
        // HINT:     buffer[head] = null;  // help GC
        // HINT:     head = (head + 1) % capacity;
        // HINT:     count--;
        // HINT:     notFull.signal();      // wake ONE waiting producer
        // HINT:     return msg;
        // HINT: } finally {
        // HINT:     lock.unlock();
        // HINT: }
        return null;
    }

    /** Non-blocking poll with timeout */
    QueueMessage poll(long timeoutMs) throws InterruptedException {
        // TODO: Implement
        // HINT: if (!lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS)) return null;
        // HINT: try {
        // HINT:     if (count == 0) {
        // HINT:         if (!notEmpty.await(timeoutMs, TimeUnit.MILLISECONDS)) return null;
        // HINT:     }
        // HINT:     if (count == 0) return null;
        // HINT:     QueueMessage msg = buffer[head];
        // HINT:     buffer[head] = null;
        // HINT:     head = (head + 1) % capacity;
        // HINT:     count--;
        // HINT:     notFull.signal();
        // HINT:     return msg;
        // HINT: } finally {
        // HINT:     lock.unlock();
        // HINT: }
        return null;
    }

    void shutdown() {
        // TODO: Implement
        // HINT: lock.lock();
        // HINT: try {
        // HINT:     shutdown = true;
        // HINT:     notFull.signalAll();   // wake all blocked producers
        // HINT:     notEmpty.signalAll();  // wake all blocked consumers
        // HINT: } finally {
        // HINT:     lock.unlock();
        // HINT: }
    }

    int size() {
        lock.lock();
        try { return count; } finally { lock.unlock(); }
    }

    int getCapacity() { return capacity; }
    boolean isFull() { return size() == capacity; }
    boolean isEmpty() { return size() == 0; }
}

// ==================== TOPIC WITH CONSUMER GROUPS ====================

/**
 * A topic uses a log-based model (like Kafka):
 * - Messages are appended to a log (ArrayList, not circular — retained)
 * - Each consumer group tracks its own offset
 * - Semaphore limits concurrent consumers per topic
 *
 * SEMAPHORE vs MUTEX here:
 *   - Mutex (lock): only 1 thread writes to the log at a time
 *   - Semaphore(N): up to N consumer threads can read concurrently
 *     (reading at different offsets is safe if log is append-only)
 */
class Topic {
    final String name;
    private final List<QueueMessage> log = new ArrayList<>();
    private final ReentrantLock writeLock = new ReentrantLock();
    private final Condition hasNewMessage = writeLock.newCondition();

    // Consumer group offsets: groupId → next offset to read
    private final ConcurrentHashMap<String, AtomicInteger> groupOffsets = new ConcurrentHashMap<>();

    // SEMAPHORE: limits how many consumer threads can run concurrently on this topic
    private final Semaphore consumerPermits;
    private volatile boolean shutdown = false;

    Topic(String name, int maxConcurrentConsumers) {
        this.name = name;
        this.consumerPermits = new Semaphore(maxConcurrentConsumers, true);
    }

    /** Append message to topic log (mutex-protected) */
    void publish(QueueMessage msg) {
        // TODO: Implement
        // HINT: writeLock.lock();
        // HINT: try {
        // HINT:     log.add(msg);
        // HINT:     hasNewMessage.signalAll();  // wake consumers waiting for new messages
        // HINT: } finally {
        // HINT:     writeLock.unlock();
        // HINT: }
    }

    /** Subscribe a consumer group (idempotent — creates offset if not exists) */
    void subscribe(String groupId) {
        // TODO: Implement
        // HINT: groupOffsets.putIfAbsent(groupId, new AtomicInteger(0));
    }

    /**
     * Consume next message for a consumer group.
     * SEMAPHORE acquire/release brackets the consumption.
     * Uses condition variable to block when no new messages.
     */
    QueueMessage consume(String groupId, long timeoutMs) throws InterruptedException {
        // TODO: Implement
        // HINT: // Step 1: Acquire semaphore permit (limits concurrency)
        // HINT: if (!consumerPermits.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) return null;
        // HINT: try {
        // HINT:     AtomicInteger offset = groupOffsets.get(groupId);
        // HINT:     if (offset == null) return null;
        // HINT:
        // HINT:     // Step 2: Check if there's a message at current offset
        // HINT:     writeLock.lock();
        // HINT:     try {
        // HINT:         int idx = offset.get();
        // HINT:         if (idx >= log.size()) {
        // HINT:             // Wait for new message or timeout
        // HINT:             if (!hasNewMessage.await(timeoutMs, TimeUnit.MILLISECONDS)) return null;
        // HINT:             idx = offset.get();
        // HINT:             if (idx >= log.size()) return null;
        // HINT:         }
        // HINT:         return log.get(idx);  // don't advance offset until ACK
        // HINT:     } finally {
        // HINT:         writeLock.unlock();
        // HINT:     }
        // HINT: } finally {
        // HINT:     consumerPermits.release();  // release semaphore permit
        // HINT: }
        return null;
    }

    /** ACK: advance consumer group offset (message processed successfully) */
    void ack(String groupId) {
        // TODO: Implement
        // HINT: AtomicInteger offset = groupOffsets.get(groupId);
        // HINT: if (offset != null) offset.incrementAndGet();
    }

    int getGroupOffset(String groupId) {
        AtomicInteger offset = groupOffsets.get(groupId);
        return offset != null ? offset.get() : -1;
    }

    int getLogSize() {
        writeLock.lock();
        try { return log.size(); } finally { writeLock.unlock(); }
    }

    Set<String> getGroups() { return groupOffsets.keySet(); }

    void shutdown() {
        shutdown = true;
        writeLock.lock();
        try { hasNewMessage.signalAll(); } finally { writeLock.unlock(); }
    }
}

// ==================== MESSAGE QUEUE SERVICE ====================

/**
 * Top-level orchestrator.
 * Uses ReadWriteLock for topic registry: many readers, exclusive writer.
 *
 * READWRITELOCK TRADE-OFF:
 *   - ReentrantLock: simple but blocks readers during other reads
 *   - ReadWriteLock: concurrent reads, exclusive writes
 *   - When to use: read-heavy workload (many consumers listing topics,
 *     rare topic creation/deletion)
 *   - Caveat: write starvation possible if reads are constant (use fair=true)
 */
class MessageQueueService {
    private final Map<String, Topic> topics = new HashMap<>();
    private final Map<String, BoundedBuffer> buffers = new HashMap<>();
    // ReadWriteLock: topic registry — many readers, exclusive writer
    private final ReadWriteLock registryLock = new ReentrantReadWriteLock(true);
    private final AtomicInteger msgIdCounter = new AtomicInteger(1);

    /**
     * Create a new topic (write lock on registry).
     */
    Topic createTopic(String name, int bufferCapacity, int maxConsumers) {
        // TODO: Implement
        // HINT: registryLock.writeLock().lock();
        // HINT: try {
        // HINT:     if (topics.containsKey(name)) return topics.get(name);
        // HINT:     Topic topic = new Topic(name, maxConsumers);
        // HINT:     topics.put(name, topic);
        // HINT:     buffers.put(name, new BoundedBuffer(bufferCapacity));
        // HINT:     return topic;
        // HINT: } finally {
        // HINT:     registryLock.writeLock().unlock();
        // HINT: }
        return null;
    }

    /**
     * List all topics (read lock — concurrent with other reads).
     */
    List<String> listTopics() {
        // TODO: Implement
        // HINT: registryLock.readLock().lock();
        // HINT: try {
        // HINT:     return new ArrayList<>(topics.keySet());
        // HINT: } finally {
        // HINT:     registryLock.readLock().unlock();
        // HINT: }
        return Collections.emptyList();
    }

    /**
     * Publish: producer → bounded buffer → topic log.
     * Two-phase: first buffer (backpressure), then topic log (durable).
     */
    QueueMessage publish(String topicName, String payload, String producerId)
            throws TopicNotFoundException, InterruptedException, QueueShutdownException {
        // TODO: Implement
        // HINT: registryLock.readLock().lock();
        // HINT: Topic topic;
        // HINT: BoundedBuffer buf;
        // HINT: try {
        // HINT:     topic = topics.get(topicName);
        // HINT:     buf = buffers.get(topicName);
        // HINT:     if (topic == null) throw new TopicNotFoundException(topicName);
        // HINT: } finally {
        // HINT:     registryLock.readLock().unlock();
        // HINT: }
        // HINT: String msgId = topicName + "-" + msgIdCounter.getAndIncrement();
        // HINT: QueueMessage msg = new QueueMessage(msgId, topicName, payload, producerId);
        // HINT: buf.put(msg);      // blocks if buffer full (backpressure)
        // HINT: topic.publish(msg); // append to durable log
        // HINT: return msg;
        return null;
    }

    /** Non-blocking publish with timeout */
    QueueMessage tryPublish(String topicName, String payload, String producerId, long timeoutMs)
            throws TopicNotFoundException, InterruptedException {
        // TODO: Implement
        // HINT: registryLock.readLock().lock();
        // HINT: Topic topic;
        // HINT: BoundedBuffer buf;
        // HINT: try {
        // HINT:     topic = topics.get(topicName);
        // HINT:     buf = buffers.get(topicName);
        // HINT:     if (topic == null) throw new TopicNotFoundException(topicName);
        // HINT: } finally {
        // HINT:     registryLock.readLock().unlock();
        // HINT: }
        // HINT: String msgId = topicName + "-" + msgIdCounter.getAndIncrement();
        // HINT: QueueMessage msg = new QueueMessage(msgId, topicName, payload, producerId);
        // HINT: if (buf.offer(msg, timeoutMs)) {
        // HINT:     topic.publish(msg);
        // HINT:     return msg;
        // HINT: }
        // HINT: return null;  // buffer full, publish failed
        return null;
    }

    void subscribe(String topicName, String groupId) throws TopicNotFoundException {
        // TODO: Implement
        // HINT: registryLock.readLock().lock();
        // HINT: try {
        // HINT:     Topic topic = topics.get(topicName);
        // HINT:     if (topic == null) throw new TopicNotFoundException(topicName);
        // HINT:     topic.subscribe(groupId);
        // HINT: } finally {
        // HINT:     registryLock.readLock().unlock();
        // HINT: }
    }

    QueueMessage consume(String topicName, String groupId, long timeoutMs)
            throws TopicNotFoundException, InterruptedException {
        // TODO: Implement
        // HINT: registryLock.readLock().lock();
        // HINT: Topic topic;
        // HINT: try {
        // HINT:     topic = topics.get(topicName);
        // HINT:     if (topic == null) throw new TopicNotFoundException(topicName);
        // HINT: } finally {
        // HINT:     registryLock.readLock().unlock();
        // HINT: }
        // HINT: return topic.consume(groupId, timeoutMs);
        return null;
    }

    void ack(String topicName, String groupId) throws TopicNotFoundException {
        // TODO: Implement
        // HINT: registryLock.readLock().lock();
        // HINT: try {
        // HINT:     Topic topic = topics.get(topicName);
        // HINT:     if (topic == null) throw new TopicNotFoundException(topicName);
        // HINT:     topic.ack(groupId);
        // HINT: } finally {
        // HINT:     registryLock.readLock().unlock();
        // HINT: }
    }

    Topic getTopic(String name) {
        registryLock.readLock().lock();
        try { return topics.get(name); } finally { registryLock.readLock().unlock(); }
    }

    void shutdown() {
        registryLock.writeLock().lock();
        try {
            for (Topic t : topics.values()) t.shutdown();
            for (BoundedBuffer b : buffers.values()) b.shutdown();
        } finally { registryLock.writeLock().unlock(); }
    }
}

// ==================== MAIN / TESTS ====================

public class MessageQueueSystem {
    public static void main(String[] args) throws Exception {
        System.out.println("╔════════════════════════════════════════════════════╗");
        System.out.println("║  MESSAGE QUEUE - Producer/Consumer Concurrency LLD ║");
        System.out.println("╚════════════════════════════════════════════════════╝\n");

        MessageQueueService mq = new MessageQueueService();

        // --- Test 1: Create topics ---
        System.out.println("=== Test 1: Create topics ===");
        mq.createTopic("orders", 10, 4);
        mq.createTopic("events", 5, 2);
        System.out.println("Topics: " + mq.listTopics());
        System.out.println("✓ Topics created with bounded buffers\n");

        // --- Test 2: Basic publish/consume ---
        System.out.println("=== Test 2: Basic publish/consume ===");
        mq.subscribe("orders", "group-A");
        QueueMessage pub1 = mq.publish("orders", "order-123", "producer-1");
        System.out.println("Published: " + pub1.id + " payload=" + pub1.payload);

        QueueMessage cons1 = mq.consume("orders", "group-A", 1000);
        System.out.println("Consumed: " + cons1.id + " payload=" + cons1.payload);
        mq.ack("orders", "group-A");
        System.out.println("ACKed. Offset now: " + mq.getTopic("orders").getGroupOffset("group-A"));
        System.out.println("✓ Basic pub/sub/ack flow\n");

        // --- Test 3: Multiple consumer groups (each gets every message) ---
        System.out.println("=== Test 3: Multiple consumer groups ===");
        mq.subscribe("orders", "group-B");
        mq.publish("orders", "order-456", "producer-1");

        QueueMessage gA = mq.consume("orders", "group-A", 1000);
        QueueMessage gB = mq.consume("orders", "group-B", 1000);
        System.out.println("Group-A got: " + (gA != null ? gA.payload : "null"));
        System.out.println("Group-B got: " + (gB != null ? gB.payload : "null"));
        mq.ack("orders", "group-A");
        mq.ack("orders", "group-B");
        System.out.println("✓ Both groups consumed same message independently\n");

        // --- Test 4: ACK before next consume (offset control) ---
        System.out.println("=== Test 4: ACK controls offset ===");
        mq.publish("orders", "msg-A", "p1");
        mq.publish("orders", "msg-B", "p1");

        QueueMessage first = mq.consume("orders", "group-A", 1000);
        System.out.println("First consume (no ACK yet): " + first.payload);

        // Without ACK, re-consume returns same message (redelivery)
        QueueMessage retry = mq.consume("orders", "group-A", 1000);
        System.out.println("Re-consume without ACK: " + retry.payload + " (same message!)");

        mq.ack("orders", "group-A");
        QueueMessage second = mq.consume("orders", "group-A", 1000);
        System.out.println("After ACK, next consume: " + second.payload);
        mq.ack("orders", "group-A");
        System.out.println("✓ Messages redeliver until ACKed\n");

        // --- Test 5: Concurrent producers ---
        System.out.println("=== Test 5: Concurrent producers (mutex test) ===");
        mq.createTopic("concurrent", 100, 8);
        mq.subscribe("concurrent", "workers");

        int numProducers = 5, msgsPerProducer = 20;
        CountDownLatch prodLatch = new CountDownLatch(numProducers);
        AtomicInteger publishCount = new AtomicInteger(0);

        for (int p = 0; p < numProducers; p++) {
            final int pid = p;
            new Thread(() -> {
                try {
                    for (int i = 0; i < msgsPerProducer; i++) {
                        mq.publish("concurrent", "p" + pid + "-msg" + i, "producer-" + pid);
                        publishCount.incrementAndGet();
                    }
                } catch (Exception e) { e.printStackTrace(); }
                prodLatch.countDown();
            }, "Producer-" + p).start();
        }
        prodLatch.await(5, TimeUnit.SECONDS);
        System.out.println("Published: " + publishCount.get() + " (expected " + (numProducers * msgsPerProducer) + ")");
        System.out.println("Topic log size: " + mq.getTopic("concurrent").getLogSize());
        System.out.println("✓ Concurrent producers — mutex ensured no data corruption\n");

        // --- Test 6: Concurrent consumers (semaphore test) ---
        System.out.println("=== Test 6: Concurrent consumers (semaphore test) ===");
        AtomicInteger consumeCount = new AtomicInteger(0);
        int numConsumers = 4;
        CountDownLatch consLatch = new CountDownLatch(numConsumers);

        for (int c = 0; c < numConsumers; c++) {
            new Thread(() -> {
                try {
                    while (true) {
                        QueueMessage msg = mq.consume("concurrent", "workers", 500);
                        if (msg == null) break;
                        mq.ack("concurrent", "workers");
                        consumeCount.incrementAndGet();
                    }
                } catch (Exception e) { /* timeout, done */ }
                consLatch.countDown();
            }, "Consumer-" + c).start();
        }
        consLatch.await(10, TimeUnit.SECONDS);
        System.out.println("Consumed: " + consumeCount.get());
        System.out.println("Group offset: " + mq.getTopic("concurrent").getGroupOffset("workers"));
        System.out.println("✓ Concurrent consumers — semaphore limited concurrency\n");

        // --- Test 7: Backpressure (bounded buffer blocks producer) ---
        System.out.println("=== Test 7: Backpressure — bounded buffer ===");
        mq.createTopic("tiny", 3, 1);  // buffer capacity = 3
        mq.subscribe("tiny", "slow-group");

        // Fill the buffer
        mq.publish("tiny", "fill-1", "p1");
        mq.publish("tiny", "fill-2", "p1");
        mq.publish("tiny", "fill-3", "p1");
        System.out.println("Buffer filled (size=3)");

        // Try non-blocking publish — should fail (buffer full)
        QueueMessage overflow = mq.tryPublish("tiny", "overflow", "p1", 200);
        System.out.println("tryPublish on full buffer: " + (overflow == null ? "null (rejected)" : overflow.id));

        // Consume one to make space
        QueueMessage drained = mq.consume("tiny", "slow-group", 1000);
        mq.ack("tiny", "slow-group");
        System.out.println("Consumed " + drained.payload + ", now space available");

        QueueMessage afterDrain = mq.tryPublish("tiny", "after-drain", "p1", 200);
        System.out.println("tryPublish after drain: " + (afterDrain != null ? afterDrain.payload : "null"));
        System.out.println("✓ Backpressure: bounded buffer blocks/rejects when full\n");

        // --- Test 8: Topic not found ---
        System.out.println("=== Test 8: Topic not found ===");
        try {
            mq.publish("nonexistent", "data", "p1");
            System.out.println("ERROR: Should have thrown!");
        } catch (TopicNotFoundException e) {
            System.out.println("Caught: " + e.getMessage());
        }
        System.out.println("✓ Topic validation\n");

        // --- Test 9: ReadWriteLock — concurrent topic listing ---
        System.out.println("=== Test 9: ReadWriteLock — concurrent reads ===");
        AtomicInteger readCount = new AtomicInteger(0);
        CountDownLatch rwLatch = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                mq.listTopics(); // read lock — all 10 can run concurrently
                readCount.incrementAndGet();
                rwLatch.countDown();
            }).start();
        }
        rwLatch.await(2, TimeUnit.SECONDS);
        System.out.println("Concurrent reads completed: " + readCount.get() + " (expected 10)");
        System.out.println("✓ ReadWriteLock allows concurrent readers\n");

        // --- Test 10: Deadlock prevention with tryLock timeout ---
        System.out.println("=== Test 10: Deadlock prevention — tryLock timeout ===");
        mq.createTopic("deadlock-test", 2, 1);
        mq.subscribe("deadlock-test", "dl-group");
        mq.publish("deadlock-test", "dl-1", "p1");
        mq.publish("deadlock-test", "dl-2", "p1");

        // Buffer is full (capacity 2). Non-blocking publish should timeout, not deadlock.
        long start = System.nanoTime();
        QueueMessage dlResult = mq.tryPublish("deadlock-test", "dl-3", "p1", 300);
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        System.out.println("tryPublish on full buffer: " + (dlResult == null ? "timeout" : "ok") +
                " in " + elapsed + "ms (should be ~300ms)");
        System.out.println("✓ No deadlock — tryLock timed out gracefully\n");

        // --- Test 11: Graceful shutdown ---
        System.out.println("=== Test 11: Graceful shutdown ===");
        mq.shutdown();
        try {
            mq.publish("orders", "post-shutdown", "p1");
            System.out.println("Published after shutdown (queue had leftover capacity)");
        } catch (QueueShutdownException e) {
            System.out.println("Caught: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Shutdown handled: " + e.getClass().getSimpleName());
        }
        System.out.println("✓ Shutdown signals all waiting threads\n");

        // --- Test 12: High-throughput stress test ---
        System.out.println("=== Test 12: Stress test — 5 producers × 1000 msgs ===");
        MessageQueueService mq2 = new MessageQueueService();
        mq2.createTopic("stress", 500, 8);
        mq2.subscribe("stress", "stress-group");

        AtomicInteger stressed = new AtomicInteger(0);
        AtomicInteger stressConsumed = new AtomicInteger(0);
        long t0 = System.nanoTime();

        // Start consumers first
        CountDownLatch consumersDone = new CountDownLatch(4);
        for (int c = 0; c < 4; c++) {
            new Thread(() -> {
                try {
                    while (true) {
                        QueueMessage msg = mq2.consume("stress", "stress-group", 500);
                        if (msg == null) break;
                        mq2.ack("stress", "stress-group");
                        stressConsumed.incrementAndGet();
                    }
                } catch (Exception e) { /* done */ }
                consumersDone.countDown();
            }).start();
        }

        // Producers
        CountDownLatch producersDone = new CountDownLatch(5);
        for (int p = 0; p < 5; p++) {
            final int pid = p;
            new Thread(() -> {
                try {
                    for (int i = 0; i < 1000; i++) {
                        mq2.publish("stress", "s" + pid + "-" + i, "sp" + pid);
                        stressed.incrementAndGet();
                    }
                } catch (Exception e) { e.printStackTrace(); }
                producersDone.countDown();
            }).start();
        }

        producersDone.await(10, TimeUnit.SECONDS);
        Thread.sleep(2000); // let consumers drain
        consumersDone.await(5, TimeUnit.SECONDS);
        long ms = (System.nanoTime() - t0) / 1_000_000;

        System.out.println("Published: " + stressed.get() + ", Consumed: " + stressConsumed.get());
        System.out.printf("Time: %d ms, Throughput: %.0f msgs/sec%n", ms,
                stressed.get() * 1000.0 / ms);
        mq2.shutdown();
        System.out.println("✓ High-throughput stress test\n");

        System.out.println("════════ ALL 12 TESTS PASSED ✓ ════════");
    }
}

/*
 * INTERVIEW NOTES — DEEP DIVE: CONCURRENCY & SYNCHRONIZATION
 *
 * 1. MUTEX vs SEMAPHORE:
 *    ┌──────────────┬────────────────────────────┬────────────────────────────┐
 *    │              │ Mutex (ReentrantLock)       │ Semaphore(N)               │
 *    ├──────────────┼────────────────────────────┼────────────────────────────┤
 *    │ Permits      │ Exactly 1                  │ N (configurable)           │
 *    │ Ownership    │ Thread that locked must     │ Any thread can release     │
 *    │              │ unlock (reentrant)          │ (no ownership)             │
 *    │ Use case     │ Critical section protection │ Resource pool limiting     │
 *    │ In this code │ BoundedBuffer.lock          │ Topic.consumerPermits      │
 *    │ Reentrant?   │ Yes (same thread can        │ N/A (permits, not locks)   │
 *    │              │ lock multiple times)        │                            │
 *    │ Deadlock risk│ Yes (if forget unlock)      │ Less (but starvation risk) │
 *    └──────────────┴────────────────────────────┴────────────────────────────┘
 *
 * 2. CONDITION VARIABLES vs wait/notify:
 *    - Object.wait/notify: one implicit condition per monitor
 *      → notifyAll wakes ALL waiters (producers AND consumers) — wasteful
 *    - Condition: multiple conditions per lock
 *      → notFull.signal() wakes only producers, notEmpty.signal() only consumers
 *    - ALWAYS use while(condition) { await(); } — never if() — spurious wakeups!
 *
 * 3. READWRITELOCK — WHEN TO USE:
 *    - Many readers, few writers (our topic registry: lots of reads, rare creates)
 *    - Read lock: shared (multiple threads)
 *    - Write lock: exclusive (one thread, blocks all readers too)
 *    - Trade-off: more complex than simple lock, but better throughput for read-heavy
 *    - Caveat: writer starvation if reads are constant → use fair=true
 *
 * 4. DEADLOCK PREVENTION STRATEGIES (demonstrated in code):
 *    a) Lock ordering: always acquire locks in same order (we use single lock per buffer)
 *    b) tryLock with timeout: offer() uses tryLock — gives up after timeout instead of blocking forever
 *    c) Always unlock in finally: prevents locks held after exceptions
 *    d) Minimize lock scope: hold lock only during buffer mutation, not during I/O
 *    e) Avoid nested locks: we never hold registryLock while acquiring buffer lock simultaneously
 *       (we release readLock before calling buf.put())
 *
 * 5. DEADLOCK vs LIVELOCK vs STARVATION:
 *    - Deadlock: A holds lock1, waits for lock2. B holds lock2, waits for lock1. Both stuck forever.
 *    - Livelock: Threads keep retrying but make no progress (e.g., both back off and retry in sync)
 *    - Starvation: Thread is runnable but never gets scheduled (low priority, unfair lock)
 *    - Our prevention: fair locks, tryLock timeouts, no nested locking
 *
 * 6. BOUNDED BUFFER — WHY CIRCULAR ARRAY:
 *    - Array-backed: O(1) index access, cache-friendly
 *    - Circular (head/tail modulo): no shifting, constant-time enqueue/dequeue
 *    - Alternative: LinkedList (unbounded, GC pressure) or ring buffer (LMAX Disruptor)
 *
 * 7. TRADE-OFFS:
 *    | Approach             | Pros                    | Cons                     |
 *    |---------------------|-------------------------|--------------------------|
 *    | synchronized        | Simple, built-in        | One condition, coarse    |
 *    | ReentrantLock       | Multiple conditions     | Must remember unlock     |
 *    | ReadWriteLock       | Concurrent reads        | Complex, write starvation|
 *    | Semaphore           | Resource pooling        | No ownership tracking    |
 *    | Lock-free (CAS)     | No blocking, fastest    | Hard to implement right  |
 *    | BlockingQueue       | Built-in, well-tested   | Hides concurrency detail |
 *
 * 8. SCALABILITY:
 *    - Partition topics across brokers (Kafka model)
 *    - Log-structured storage: append-only, sequential I/O (fast writes)
 *    - Consumer groups: horizontal scaling of consumers
 *    - Zero-copy: sendfile() to avoid user-space buffer copies
 *    - Batching: amortize lock overhead over multiple messages
 *
 * 9. REAL-WORLD PARALLELS:
 *    - Apache Kafka: log-based, consumer offsets, partition-level ordering
 *    - RabbitMQ: AMQP, exchange routing, per-message ACK
 *    - Amazon SQS: managed queue, visibility timeout, at-least-once
 *    - Redis Streams: XADD/XREAD, consumer groups
 *    - LMAX Disruptor: lock-free ring buffer, ~100M msgs/sec
 *
 * 10. COMPLEXITY:
 *    | Operation      | Time  | Concurrency primitive used        |
 *    |---------------|-------|-----------------------------------|
 *    | publish       | O(1)  | Mutex (ReentrantLock) + Condition |
 *    | consume       | O(1)  | Semaphore + Mutex + Condition     |
 *    | ack           | O(1)  | AtomicInteger CAS                 |
 *    | createTopic   | O(1)  | WriteLock (ReadWriteLock)          |
 *    | listTopics    | O(t)  | ReadLock (concurrent reads OK)     |
 */
