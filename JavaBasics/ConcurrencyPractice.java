/**
 * ConcurrencyPractice.java
 * 
 * A comprehensive Java practice file for mastering Concurrency and Multithreading.
 * Complete all the TODO implementations below and run the main method to test your solutions.
 * 
 * Instructions:
 * 1. Read each task's description carefully
 * 2. Implement the required classes and methods
 * 3. Run: javac ConcurrencyPractice.java && java ConcurrencyPractice
 * 4. Check if all test cases pass
 * 
 * Topics Covered:
 * - Thread Creation (Thread, Runnable)
 * - Thread Synchronization
 * - ExecutorService and Thread Pools
 * - Callable and Future
 * - Locks and Conditions
 * - Atomic Variables
 * - Concurrent Collections
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

public class ConcurrencyPractice {
    
    // ==================== THREAD CREATION ====================
    
    /**
     * Task 1: Create a thread by extending Thread class
     * Requirements: Override run() method to print numbers 1-5
     */
    static class NumberPrinterThread extends Thread {
        // TODO: Implement run() method
        @Override
        public void run() {
            for (int i = 0; i < 5; i++) {
                System.out.println(i+1);
            }
        }
    }
    
    /**
     * Task 2: Create a thread using Runnable interface
     * Requirements: Implement run() method to print given message
     */
    static class MessagePrinter implements Runnable {
        private String message;
        
        public MessagePrinter(String message) {
            this.message = message;
        }
        
        // TODO: Implement run() method
        @Override
        public void run() {
            for (int i = 0; i < 5; i++) {
                System.out.println(i+1);
            }
        }
    }
    
    /**
     * Task 3: Create and start multiple threads
     * @param count Number of threads to create
     * @return List of created threads
     */
    public static List<Thread> createAndStartThreads(int count) {
        List<Thread> l=new ArrayList<>();
        while (count-- > 0) {
            l.add(new Thread());
        }
        // TODO: Create count threads, start them, return list
        return l;
    }
    
    // ==================== SYNCHRONIZATION ====================
    
    /**
     * Task 4: Thread-safe counter using synchronized
     * Requirements: Implement synchronized increment method
     */
    static class Counter {
        private int count = 0;
        
        // TODO: Add synchronized keyword to this method
        public synchronized void increment() {
            count++;
        }
        
        public int getCount() {
            return count;
        }
    }
    
    /**
     * Task 5: Thread-safe bank account
     * Requirements: Synchronized deposit and withdraw methods
     */
    static class BankAccount {
        private double balance;
        
        public BankAccount(double initialBalance) {
            this.balance = initialBalance;
        }
        
        // TODO: Add synchronized deposit method
        public synchronized void deposit(double amount) {
            balance+=amount;
        }
        
        // TODO: Add synchronized withdraw method
        public synchronized void withdraw(double amount) {
            balance-=amount;
        }
        
        public double getBalance() {
            return balance;
        }
    }
    
    // ==================== EXECUTOR SERVICE ====================
    
    /**
     * Task 6: Execute tasks using FixedThreadPool
     * @param tasks List of Runnable tasks
     * @param poolSize Size of thread pool
     */
    public static void executeWithFixedPool(List<Runnable> tasks, int poolSize) {
        // TODO: Create ExecutorService, execute all tasks, shutdown
        ExecutorService es=Executors.newFixedThreadPool(poolSize);
        try {
            for(Runnable r:tasks){
                es.execute(r);
            }
        } finally {
            es.shutdown();
        }

    }
    
    /**
     * Task 7: Submit callable tasks and get results
     * @param callables List of Callable<Integer> tasks
     * @return List of results
     */
    public static List<Integer> submitCallableTasks(List<Callable<Integer>> callables) throws Exception {
        // TODO: Use ExecutorService to submit callables and collect results
       return null;
    }
    
    /**
     * Task 8: Execute tasks with timeout
     * @param task Callable task
     * @param timeoutSeconds Timeout in seconds
     * @return Result or null if timeout
     */
    public static Integer executeWithTimeout(Callable<Integer> task, int timeoutSeconds) {
        // TODO: Use Future.get(timeout, TimeUnit.SECONDS)
        return null;
    }
    
    // ==================== LOCKS ====================
    
    /**
     * Task 9: Thread-safe counter using ReentrantLock
     * Requirements: Use lock() and unlock() properly
     */
    static class LockCounter {
        private int count = 0;
        private final Lock lock = new ReentrantLock();
        
        // TODO: Implement increment using lock
        public void increment() {
        }
        
        public int getCount() {
            return count;
        }
    }
    
    /**
     * Task 10: ReadWriteLock for shared resource
     * Requirements: Multiple readers, single writer
     */
    static class SharedResource {
        private String data = "";
        private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
        
        // TODO: Implement read method with read lock
        public String read() {
            return null;
        }
        
        // TODO: Implement write method with write lock
        public void write(String newData) {
        }
    }
    
    // ==================== ATOMIC VARIABLES ====================
    
    /**
     * Task 11: Thread-safe counter using AtomicInteger
     */
    static class AtomicCounter {
        private AtomicInteger count = new AtomicInteger(0);
        
        // TODO: Implement increment using AtomicInteger
        public void increment() {
            count.addAndGet(1);
        }
        
        public int getCount() {
            return count.get();
        }
    }
    
    /**
     * Task 12: Compare and swap operation
     * @param atomic AtomicInteger instance
     * @param expected Expected value
     * @param update New value
     * @return true if update successful
     */
    public static boolean compareAndSwap(AtomicInteger atomic, int expected, int update) {
        // TODO: Use compareAndSet method
        return atomic.compareAndSet(expected, update);
    }
    
    // ==================== CONCURRENT COLLECTIONS ====================
    
    /**
     * Task 13: Add elements concurrently to ConcurrentHashMap
     * @param count Number of elements to add
     * @return ConcurrentHashMap with elements
     */
    public static ConcurrentHashMap<Integer, String> fillConcurrentMap(int count) {
        // TODO: Create ConcurrentHashMap, add count key-value pairs
        return null;
    }
    
    /**
     * Task 14: Use CopyOnWriteArrayList for thread-safe list
     * @param elements Elements to add
     * @return CopyOnWriteArrayList
     */
    public static CopyOnWriteArrayList<String> createCopyOnWriteList(String[] elements) {
        // TODO: Create and populate CopyOnWriteArrayList
        return null;
    }
    
    /**
     * Task 15: Use BlockingQueue for producer-consumer
     * @param capacity Queue capacity
     * @return ArrayBlockingQueue
     */
    public static BlockingQueue<Integer> createBlockingQueue(int capacity) {
        // TODO: Create ArrayBlockingQueue with given capacity
        return null;
    }
    
    // ==================== SYNCHRONIZERS ====================
    
    /**
     * Task 16: Use CountDownLatch to wait for threads
     * @param threadCount Number of threads
     * @return CountDownLatch
     */
    public static CountDownLatch createCountDownLatch(int threadCount) {
        // TODO: Create CountDownLatch with count
        return null;
    }
    
    /**
     * Task 17: Use Semaphore for resource limiting
     * @param permits Number of permits
     * @return Semaphore
     */
    public static Semaphore createSemaphore(int permits) {
        // TODO: Create Semaphore with permits
        return new Semaphore(permits);
    }
    
    /**
     * Task 18: Use CyclicBarrier for coordination
     * @param parties Number of parties
     * @return CyclicBarrier
     */
    public static CyclicBarrier createCyclicBarrier(int parties) {
        // TODO: Create CyclicBarrier
        return null;
    }
    
    // ==================== COMPLETABLE FUTURE ====================
    
    /**
     * Task 19: Create completed CompletableFuture
     * @param value The value
     * @return CompletableFuture with value
     */
    public static CompletableFuture<Integer> createCompletedFuture(Integer value) {
        // TODO: Use CompletableFuture.completedFuture()
        return null;
    }
    
    /**
     * Task 20: Run async task
     * @param task Runnable task
     * @return CompletableFuture<Void>
     */
    public static CompletableFuture<Void> runAsync(Runnable task) {
        // TODO: Use CompletableFuture.runAsync()
        return null;
    }
    
    /**
     * Task 21: Supply async value
     * @param supplier Supplier function
     * @return CompletableFuture with result
     */
    public static CompletableFuture<String> supplyAsync(Callable<String> supplier) {
        // TODO: Use CompletableFuture.supplyAsync()
        return null;
    }
    
    /**
     * Task 22: Chain CompletableFutures
     * @param future Initial future
     * @return Transformed future (multiply by 2)
     */
    public static CompletableFuture<Integer> chainFutures(CompletableFuture<Integer> future) {
        // TODO: Use thenApply to multiply result by 2
        return null;
    }
    
    /**
     * Task 23: Combine two futures
     * @param future1 First future
     * @param future2 Second future
     * @return Combined result (sum)
     */
    public static CompletableFuture<Integer> combineFutures(
            CompletableFuture<Integer> future1, 
            CompletableFuture<Integer> future2) {
        // TODO: Use thenCombine to add results
        return null;
    }
    
    // ==================== THREAD POOL EXAMPLES ====================
    
    /**
     * Task 24: Create FixedThreadPool
     * @param size Pool size
     * @return ExecutorService
     */
    public static ExecutorService createFixedThreadPool(int size) {
        // TODO: Use Executors.newFixedThreadPool()
        return null;
    }
    
    /**
     * Task 25: Create CachedThreadPool
     * @return ExecutorService
     */
    public static ExecutorService createCachedThreadPool() {
        // TODO: Use Executors.newCachedThreadPool()
        return null;
    }
    
    /**
     * Task 26: Create SingleThreadExecutor
     * @return ExecutorService
     */
    public static ExecutorService createSingleThreadExecutor() {
        // TODO: Use Executors.newSingleThreadExecutor()
        return null;
    }
    
    /**
     * Task 27: Create ScheduledThreadPool
     * @param size Pool size
     * @return ScheduledExecutorService
     */
    public static ScheduledExecutorService createScheduledThreadPool(int size) {
        // TODO: Use Executors.newScheduledThreadPool()
        return null;
    }
    
    // ==================== PRACTICAL SCENARIOS ====================
    
    /**
     * Task 28: Producer-Consumer using BlockingQueue
     */
    static class ProducerConsumer {
        private BlockingQueue<Integer> queue;
        
        public ProducerConsumer(int capacity) {
            // TODO: Initialize ArrayBlockingQueue
        }
        
        public void produce(Integer item) throws InterruptedException {
            // TODO: Put item in queue
        }
        
        public Integer consume() throws InterruptedException {
            // TODO: Take item from queue
            return null;
        }
    }
    
    /**
     * Task 29: Thread-safe Singleton with double-checked locking
     */
    static class ThreadSafeSingleton {
        private static volatile ThreadSafeSingleton instance;
        
        private ThreadSafeSingleton() {}
        
        // TODO: Implement double-checked locking getInstance()
        public static ThreadSafeSingleton getInstance() {
            return null;
        }
    }
    
    /**
     * Task 30: Parallel sum using Fork/Join
     * @param numbers Array of numbers
     * @return Sum of all numbers
     */
    public static long parallelSum(int[] numbers) {
        // TODO: Use Arrays.stream().parallel().sum() or ForkJoinPool
        return 0;
    }
    
    // ==================== TEST CASES ====================
    
    public static void main(String[] args) throws Exception {
        int totalTests = 0;
        int passedTests = 0;
        
        System.out.println("=".repeat(60));
        System.out.println("JAVA CONCURRENCY PRACTICE - TEST SUITE");
        System.out.println("=".repeat(60));
        
        // Test 1: NumberPrinterThread (skip auto-pass, needs manual verification)
        System.out.println("\n[Test 1] NumberPrinterThread");
        System.out.println("  ⚠ Manual verification needed - check if numbers 1-5 are printed");
        NumberPrinterThread thread1 = new NumberPrinterThread();
        thread1.start();
        thread1.join();
        totalTests++; 
        
        // Test 2: MessagePrinter Runnable (skip auto-pass, needs manual verification)
        System.out.println("\n[Test 2] MessagePrinter");
        System.out.println("  ⚠ Manual verification needed - check if message is printed");
        Thread thread2 = new Thread(new MessagePrinter("Hello from Runnable"));
        thread2.start();
        thread2.join();
        totalTests++;
        
        // Test 3: createAndStartThreads
        System.out.println("\n[Test 3] createAndStartThreads");
        List<Thread> threads = createAndStartThreads(3);
        totalTests++; if (testBoolean(threads != null && threads.size() == 3, true, "Created 3 threads")) passedTests++;
        
        // Test 4: Synchronized Counter
        System.out.println("\n[Test 4] Synchronized Counter");
        Counter counter = new Counter();
        List<Thread> counterThreads = new ArrayList<>();
        // Increased threads and iterations to make race conditions more visible
        for (int i = 0; i < 100; i++) {
            Thread t = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    counter.increment();
                }
            });
            counterThreads.add(t);
            t.start();
        }
        for (Thread t : counterThreads) t.join();
        totalTests++; if (testInt(counter.getCount(), 100000, "Counter thread-safe (100 threads × 1000 increments)")) passedTests++;
        
        // Test 5: BankAccount
        System.out.println("\n[Test 5] BankAccount");
        BankAccount account = new BankAccount(1000);
        List<Thread> bankThreads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Thread t = new Thread(() -> {
                account.deposit(100);
                account.withdraw(50);
            });
            bankThreads.add(t);
            t.start();
        }
        for (Thread t : bankThreads) t.join();
        totalTests++; if (testDouble(account.getBalance(), 1250, "Bank account balance")) passedTests++;
        
        // Test 6: executeWithFixedPool
        System.out.println("\n[Test 6] executeWithFixedPool");
        AtomicInteger taskCounter = new AtomicInteger(0);
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tasks.add(() -> taskCounter.incrementAndGet());
        }
        try {
            executeWithFixedPool(tasks, 2);
            Thread.sleep(100); // Give time for tasks to complete
            totalTests++; if (testInt(taskCounter.get(), 5, "All tasks executed")) passedTests++;
        } catch (Exception e) {
            totalTests++;
            System.out.println("  ✗ FAIL: executeWithFixedPool threw exception or not implemented");
        }
        
        // Test 7: submitCallableTasks
        System.out.println("\n[Test 7] submitCallableTasks");
        List<Callable<Integer>> callables = new ArrayList<>();
        callables.add(() -> 10);
        callables.add(() -> 20);
        callables.add(() -> 30);
        List<Integer> results = submitCallableTasks(callables);
        totalTests++; if (testBoolean(results != null && results.size() == 3, true, "Callable results")) passedTests++;
        
        // Test 8: LockCounter
        System.out.println("\n[Test 8] LockCounter");
        LockCounter lockCounter = new LockCounter();
        List<Thread> lockThreads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Thread t = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    lockCounter.increment();
                }
            });
            lockThreads.add(t);
            t.start();
        }
        for (Thread t : lockThreads) t.join();
        totalTests++; if (testInt(lockCounter.getCount(), 1000, "Lock counter thread-safe")) passedTests++;
        
        // Test 9: AtomicCounter
        System.out.println("\n[Test 9] AtomicCounter");
        AtomicCounter atomicCounter = new AtomicCounter();
        List<Thread> atomicThreads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Thread t = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    atomicCounter.increment();
                }
            });
            atomicThreads.add(t);
            t.start();
        }
        for (Thread t : atomicThreads) t.join();
        totalTests++; if (testInt(atomicCounter.getCount(), 1000, "Atomic counter thread-safe")) passedTests++;
        
        // Test 10: compareAndSwap
        System.out.println("\n[Test 10] compareAndSwap");
        AtomicInteger atomic = new AtomicInteger(5);
        boolean result10 = compareAndSwap(atomic, 5, 10);
        totalTests++; if (testBoolean(result10, true, "CAS successful")) passedTests++;
        totalTests++; if (testInt(atomic.get(), 10, "Value updated")) passedTests++;
        
        // Test 11: fillConcurrentMap
        System.out.println("\n[Test 11] fillConcurrentMap");
        ConcurrentHashMap<Integer, String> map = fillConcurrentMap(5);
        totalTests++; if (testBoolean(map != null && map.size() == 5, true, "ConcurrentHashMap filled")) passedTests++;
        
        // Test 12: createCopyOnWriteList
        System.out.println("\n[Test 12] createCopyOnWriteList");
        CopyOnWriteArrayList<String> cowList = createCopyOnWriteList(new String[]{"a", "b", "c"});
        totalTests++; if (testBoolean(cowList != null && cowList.size() == 3, true, "CopyOnWriteArrayList created")) passedTests++;
        
        // Test 13: createBlockingQueue
        System.out.println("\n[Test 13] createBlockingQueue");
        BlockingQueue<Integer> queue = createBlockingQueue(10);
        totalTests++; if (testBoolean(queue != null && queue.remainingCapacity() == 10, true, "BlockingQueue created")) passedTests++;
        
        // Test 14: createCountDownLatch
        System.out.println("\n[Test 14] createCountDownLatch");
        CountDownLatch latch = createCountDownLatch(3);
        totalTests++; if (testBoolean(latch != null && latch.getCount() == 3, true, "CountDownLatch created")) passedTests++;
        
        // Test 15: createSemaphore
        System.out.println("\n[Test 15] createSemaphore");
        Semaphore semaphore = createSemaphore(5);
        totalTests++; if (testBoolean(semaphore != null && semaphore.availablePermits() == 5, true, "Semaphore created")) passedTests++;
        
        // Test 16: createCyclicBarrier
        System.out.println("\n[Test 16] createCyclicBarrier");
        CyclicBarrier barrier = createCyclicBarrier(3);
        totalTests++; if (testBoolean(barrier != null && barrier.getParties() == 3, true, "CyclicBarrier created")) passedTests++;
        
        // Test 17: createCompletedFuture
        System.out.println("\n[Test 17] createCompletedFuture");
        CompletableFuture<Integer> cf = createCompletedFuture(42);
        totalTests++; if (testBoolean(cf != null && cf.get() == 42, true, "CompletedFuture")) passedTests++;
        
        // Test 18: runAsync
        System.out.println("\n[Test 18] runAsync");
        AtomicBoolean executed = new AtomicBoolean(false);
        CompletableFuture<Void> asyncFuture = runAsync(() -> executed.set(true));
        if (asyncFuture != null) asyncFuture.join();
        totalTests++; if (testBoolean(executed.get(), true, "Async task executed")) passedTests++;
        
        // Test 19: chainFutures
        System.out.println("\n[Test 19] chainFutures");
        CompletableFuture<Integer> initial = CompletableFuture.completedFuture(5);
        CompletableFuture<Integer> chained = chainFutures(initial);
        totalTests++; if (testBoolean(chained != null && chained.get() == 10, true, "Chained future")) passedTests++;
        
        // Test 20: combineFutures
        System.out.println("\n[Test 20] combineFutures");
        CompletableFuture<Integer> f1 = CompletableFuture.completedFuture(10);
        CompletableFuture<Integer> f2 = CompletableFuture.completedFuture(20);
        CompletableFuture<Integer> combined = combineFutures(f1, f2);
        totalTests++; if (testBoolean(combined != null && combined.get() == 30, true, "Combined futures")) passedTests++;
        
        // Test 21: createFixedThreadPool
        System.out.println("\n[Test 21] createFixedThreadPool");
        ExecutorService pool = createFixedThreadPool(4);
        totalTests++; if (testBoolean(pool != null, true, "Fixed thread pool created")) passedTests++;
        if (pool != null) pool.shutdown();
        
        // Test 22: createCachedThreadPool
        System.out.println("\n[Test 22] createCachedThreadPool");
        ExecutorService cachedPool = createCachedThreadPool();
        totalTests++; if (testBoolean(cachedPool != null, true, "Cached thread pool created")) passedTests++;
        if (cachedPool != null) cachedPool.shutdown();
        
        // Test 23: createSingleThreadExecutor
        System.out.println("\n[Test 23] createSingleThreadExecutor");
        ExecutorService singlePool = createSingleThreadExecutor();
        totalTests++; if (testBoolean(singlePool != null, true, "Single thread executor created")) passedTests++;
        if (singlePool != null) singlePool.shutdown();
        
        // Test 24: createScheduledThreadPool
        System.out.println("\n[Test 24] createScheduledThreadPool");
        ScheduledExecutorService scheduledPool = createScheduledThreadPool(2);
        totalTests++; if (testBoolean(scheduledPool != null, true, "Scheduled thread pool created")) passedTests++;
        if (scheduledPool != null) scheduledPool.shutdown();
        
        // Test 25: ThreadSafeSingleton
        System.out.println("\n[Test 25] ThreadSafeSingleton");
        ThreadSafeSingleton singleton1 = ThreadSafeSingleton.getInstance();
        ThreadSafeSingleton singleton2 = ThreadSafeSingleton.getInstance();
        totalTests++; if (testBoolean(singleton1 != null && singleton1 == singleton2, true, "Singleton same instance")) passedTests++;
        totalTests++; if (testBoolean(singleton1 != null, true, "Singleton not null")) passedTests++;
        
        // Test 26: parallelSum
        System.out.println("\n[Test 26] parallelSum");
        int[] numbers = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        long sum = parallelSum(numbers);
        totalTests++; if (testLong(sum, 55L, "Parallel sum")) passedTests++;
        
        // Print final results
        System.out.println("\n" + "=".repeat(60));
        System.out.println("TEST RESULTS");
        System.out.println("=".repeat(60));
        System.out.println("Total Tests: " + totalTests);
        System.out.println("Passed: " + passedTests);
        System.out.println("Failed: " + (totalTests - passedTests));
        System.out.println("Success Rate: " + String.format("%.2f", (passedTests * 100.0 / totalTests)) + "%");
        System.out.println("=".repeat(60));
        
        if (passedTests == totalTests) {
            System.out.println("\n🎉 CONGRATULATIONS! All tests passed! 🎉");
            System.out.println("You have successfully mastered Java Concurrency!");
        } else {
            System.out.println("\n⚠️  Keep practicing! Review the failed tests and try again.");
        }
    }
    
    // ==================== TEST HELPER METHODS ====================
    
    private static boolean testInt(int actual, int expected, String testName) {
        if (actual == expected) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: " + expected + ", Actual: " + actual);
            return false;
        }
    }
    
    private static boolean testLong(long actual, long expected, String testName) {
        if (actual == expected) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: " + expected + ", Actual: " + actual);
            return false;
        }
    }
    
    private static boolean testDouble(double actual, double expected, String testName) {
        if (Math.abs(actual - expected) < 0.001) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: " + expected + ", Actual: " + actual);
            return false;
        }
    }
    
    private static boolean testBoolean(boolean actual, boolean expected, String testName) {
        if (actual == expected) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: " + expected + ", Actual: " + actual);
            return false;
        }
    }
}
