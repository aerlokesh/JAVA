import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

// ===== CUSTOM EXCEPTION CLASSES =====

/**
 * Exception thrown when log level is invalid
 * WHEN TO THROW:
 * - Null or unrecognized log level
 * - Setting minimum level to null
 */
class InvalidLogLevelException extends Exception {
    public InvalidLogLevelException(String message) {
        super(message);
    }
}

/**
 * Exception thrown when logger configuration is invalid
 * WHEN TO THROW:
 * - Null logger name
 * - Invalid sink configuration
 * - Duplicate logger name
 */
class InvalidLoggerConfigException extends Exception {
    public InvalidLoggerConfigException(String message) {
        super(message);
    }
}

/**
 * Exception thrown when a log sink fails to write
 * WHEN TO THROW:
 * - File write failure
 * - Network sink unreachable
 * - Sink is closed/disposed
 */
class LogSinkException extends Exception {
    private String sinkName;
    
    public LogSinkException(String sinkName, String message) {
        super("Sink '" + sinkName + "' failed: " + message);
        this.sinkName = sinkName;
    }
    
    public String getSinkName() { return sinkName; }
}

// ===== ENUMS =====

/**
 * Log severity levels (ordered from least to most severe)
 * 
 * INTERVIEW DISCUSSION:
 * - Why use enum ordinal for comparison?
 * - How does level filtering work? (log only if message.level >= logger.minLevel)
 */
enum LogLevel {
    TRACE,   // Very detailed debugging (method entry/exit)
    DEBUG,   // Debugging information
    INFO,    // General operational messages
    WARN,    // Potential issues, recoverable errors
    ERROR,   // Error events, operation failed
    FATAL    // Critical failure, system may crash
}

// ===== DOMAIN CLASSES =====

/**
 * Represents a single log message with metadata
 */
class LogMessage {
    String loggerName;
    LogLevel level;
    String message;
    LocalDateTime timestamp;
    String threadName;
    
    public LogMessage(String loggerName, LogLevel level, String message) {
        this.loggerName = loggerName;
        this.level = level;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.threadName = Thread.currentThread().getName();
    }
    
    @Override
    public String toString() {
        return String.format("[%s] [%s] [%s] [%s] %s",
            timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")),
            level, threadName, loggerName, message);
    }
}

// ===== INTERFACE - STRATEGY PATTERN FOR LOG SINKS =====

/**
 * Strategy interface for different log output destinations
 * 
 * INTERVIEW DISCUSSION:
 * - Why use Strategy pattern here? (Open/Closed principle - add new sinks without modifying logger)
 * - What are common sinks? (Console, File, Database, Network/ELK, Cloud)
 */
interface LogSink {
    void write(LogMessage message) throws LogSinkException;
    String getName();
    void close();
}

// ===== SINK IMPLEMENTATIONS =====

/**
 * Console sink - writes to System.out / System.err
 */
class ConsoleSink implements LogSink {
    /**
     * IMPLEMENTATION HINTS:
     * 1. Use System.out.println for INFO and below
     * 2. Use System.err.println for WARN and above
     * 3. Format using message.toString()
     * 
     * @param message The log message to write
     */
    @Override
    public void write(LogMessage message) throws LogSinkException {
        // TODO: Implement
        // HINT: if (message.level.ordinal() >= LogLevel.WARN.ordinal())
        //           System.err.println(message);
        //       else
        //           System.out.println(message);
    }
    
    @Override
    public String getName() { return "ConsoleSink"; }
    
    @Override
    public void close() { /* Nothing to close for console */ }
}

/**
 * File sink - simulates writing to a file (stores in list for demo)
 * In real system: BufferedWriter, log rotation, etc.
 */
class FileSink implements LogSink {
    private String fileName;
    private List<String> fileContents;  // Simulates file storage
    private boolean closed;
    
    public FileSink(String fileName) {
        this.fileName = fileName;
        this.fileContents = new ArrayList<>();
        this.closed = false;
    }
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Check if sink is closed - throw LogSinkException if so
     * 2. Format the message
     * 3. Add to fileContents list (simulating file write)
     * 
     * @param message The log message to write
     * @throws LogSinkException if sink is closed
     */
    @Override
    public void write(LogMessage message) throws LogSinkException {
        // TODO: Implement
        // HINT: if (closed) throw new LogSinkException(getName(), "Sink is closed");
        // HINT: fileContents.add(message.toString());
    }
    
    @Override
    public String getName() { return "FileSink(" + fileName + ")"; }
    
    @Override
    public void close() { closed = true; }
    
    public List<String> getFileContents() { return fileContents; }
    public int getLineCount() { return fileContents.size(); }
}

/**
 * In-memory sink - stores log messages in a bounded buffer (useful for testing/debugging)
 */
class InMemorySink implements LogSink {
    private int maxSize;
    private List<LogMessage> buffer;
    
    public InMemorySink(int maxSize) {
        this.maxSize = maxSize;
        this.buffer = new ArrayList<>();
    }
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Add message to buffer
     * 2. If buffer exceeds maxSize, remove oldest entry (index 0)
     * 3. This creates a sliding window of recent logs
     * 
     * @param message The log message to store
     */
    @Override
    public void write(LogMessage message) throws LogSinkException {
        // TODO: Implement
        // HINT: buffer.add(message);
        // HINT: if (buffer.size() > maxSize) buffer.remove(0);
    }
    
    @Override
    public String getName() { return "InMemorySink(max=" + maxSize + ")"; }
    
    @Override
    public void close() { buffer.clear(); }
    
    public List<LogMessage> getBuffer() { return new ArrayList<>(buffer); }
    public int size() { return buffer.size(); }
}

// ===== LOGGER CLASS =====

/**
 * Individual logger instance with name, level, and sinks
 * 
 * INTERVIEW DISCUSSION:
 * - Why separate Logger from LoggerFactory? (SRP - Single Responsibility)
 * - Should loggers be thread-safe? (Yes - multiple threads may log simultaneously)
 * - How does log level filtering work? (Skip messages below minimum level)
 */
class Logger {
    private String name;
    private LogLevel minLevel;
    private List<LogSink> sinks;
    
    public Logger(String name, LogLevel minLevel) {
        this.name = name;
        this.minLevel = minLevel;
        this.sinks = new ArrayList<>();
    }
    
    /**
     * Add a sink to this logger
     * 
     * @param sink The sink to add
     */
    public void addSink(LogSink sink) {
        // TODO: Implement
        // HINT: sinks.add(sink);
    }
    
    /**
     * Set minimum log level
     * 
     * @param level New minimum level
     */
    public void setMinLevel(LogLevel level) {
        // TODO: Implement
        // HINT: this.minLevel = level;
    }
    
    /**
     * Core log method - filters by level and writes to all sinks
     * 
     * IMPLEMENTATION HINTS:
     * 1. Check if message level >= minLevel (use ordinal() comparison)
     * 2. If below minimum, return immediately (filtered out)
     * 3. Create LogMessage object
     * 4. Write to ALL registered sinks
     * 5. Handle sink failures gracefully (don't let one failed sink stop others)
     * 
     * INTERVIEW DISCUSSION:
     * - Should logging be synchronous or async? (async for performance)
     * - What if a sink throws? (catch and continue to next sink)
     * - How to handle backpressure? (bounded queue, drop policy)
     * 
     * @param level The severity level
     * @param message The log message text
     */
    public void log(LogLevel level, String message) {
        // TODO: Implement
        // HINT: if (level.ordinal() < minLevel.ordinal()) return;
        // HINT: LogMessage logMsg = new LogMessage(name, level, message);
        // HINT: for (LogSink sink : sinks) {
        //     try { sink.write(logMsg); }
        //     catch (LogSinkException e) { System.err.println("Sink error: " + e.getMessage()); }
        // }
    }
    
    /**
     * Convenience methods for each log level
     * 
     * IMPLEMENTATION HINTS:
     * Each method simply calls log() with the appropriate level
     */
    public void trace(String message) {
        // TODO: Implement
        // HINT: log(LogLevel.TRACE, message);
    }
    
    public void debug(String message) {
        // TODO: Implement
        // HINT: log(LogLevel.DEBUG, message);
    }
    
    public void info(String message) {
        // TODO: Implement
        // HINT: log(LogLevel.INFO, message);
    }
    
    public void warn(String message) {
        // TODO: Implement
        // HINT: log(LogLevel.WARN, message);
    }
    
    public void error(String message) {
        // TODO: Implement
        // HINT: log(LogLevel.ERROR, message);
    }
    
    public void fatal(String message) {
        // TODO: Implement
        // HINT: log(LogLevel.FATAL, message);
    }
    
    public String getName() { return name; }
    public LogLevel getMinLevel() { return minLevel; }
    public List<LogSink> getSinks() { return sinks; }
}

// ===== LOGGER FACTORY (SINGLETON) =====

/**
 * Logging Framework - Low Level Design (LLD)
 * 
 * PROBLEM STATEMENT:
 * Design a logging framework (like Log4j / SLF4J) that can:
 * 1. Support multiple log levels (TRACE, DEBUG, INFO, WARN, ERROR, FATAL)
 * 2. Filter messages by minimum log level
 * 3. Write to multiple sinks (Console, File, InMemory)
 * 4. Support named loggers with independent configurations
 * 5. Provide a global LoggerFactory (Singleton pattern)
 * 6. Be thread-safe for concurrent logging
 * 
 * REQUIREMENTS:
 * - Functional: Log messages, filter by level, multiple sinks, named loggers
 * - Non-Functional: Thread-safe, low overhead, extensible sinks
 * 
 * INTERVIEW HINTS:
 * - Discuss Singleton pattern for LoggerFactory
 * - Talk about Strategy pattern for sinks
 * - Mention async logging with queues for performance
 * - Consider structured logging (JSON format)
 * - Discuss log rotation and retention policies
 */
class LoggerFactory {
    private static LoggerFactory instance;
    private Map<String, Logger> loggers;
    private LogLevel defaultLevel;
    private List<LogSink> defaultSinks;
    
    private LoggerFactory() {
        this.loggers = new ConcurrentHashMap<>();
        this.defaultLevel = LogLevel.INFO;
        this.defaultSinks = new ArrayList<>();
    }
    
    /**
     * Get singleton instance (thread-safe)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Check if instance is null
     * 2. Use synchronized block for thread safety
     * 3. Double-checked locking pattern
     * 
     * @return The singleton LoggerFactory instance
     */
    public static LoggerFactory getInstance() {
        // TODO: Implement
        // HINT: if (instance == null) {
        //     synchronized (LoggerFactory.class) {
        //         if (instance == null) {
        //             instance = new LoggerFactory();
        //         }
        //     }
        // }
        // HINT: return instance;
        return null;
    }
    
    /**
     * Get or create a named logger
     * 
     * IMPLEMENTATION HINTS:
     * 1. Check if logger already exists (by name)
     * 2. If exists, return existing logger
     * 3. If not, create new logger with default level
     * 4. Add all default sinks to new logger
     * 5. Store in loggers map
     * 6. Return the logger
     * 
     * @param name Logger name (typically class name)
     * @return Logger instance
     */
    public Logger getLogger(String name) {
        // TODO: Implement
        // HINT: return loggers.computeIfAbsent(name, n -> {
        //     Logger logger = new Logger(n, defaultLevel);
        //     defaultSinks.forEach(logger::addSink);
        //     return logger;
        // });
        return null;
    }
    
    /**
     * Set the default log level for new loggers
     * 
     * @param level Default minimum level
     * @throws InvalidLogLevelException if level is null
     */
    public void setDefaultLevel(LogLevel level) throws InvalidLogLevelException {
        // TODO: Implement
        // HINT: if (level == null) throw new InvalidLogLevelException("Level cannot be null");
        // HINT: this.defaultLevel = level;
    }
    
    /**
     * Add a default sink (applied to all new loggers)
     * 
     * @param sink Sink to add as default
     * @throws InvalidLoggerConfigException if sink is null
     */
    public void addDefaultSink(LogSink sink) throws InvalidLoggerConfigException {
        // TODO: Implement
        // HINT: if (sink == null) throw new InvalidLoggerConfigException("Sink cannot be null");
        // HINT: defaultSinks.add(sink);
    }
    
    /**
     * Get all registered logger names
     * 
     * @return Set of logger names
     */
    public Set<String> getLoggerNames() {
        // TODO: Implement
        // HINT: return new HashSet<>(loggers.keySet());
        return new HashSet<>();
    }
    
    /**
     * Reset the factory (useful for testing)
     */
    public void reset() {
        loggers.clear();
        defaultSinks.clear();
        defaultLevel = LogLevel.INFO;
    }
    
    /**
     * Reset singleton (for testing only)
     */
    public static void resetInstance() {
        instance = null;
    }
}

// ===== MAIN TEST CLASS =====

public class LoggingFramework {
    public static void main(String[] args) {
        System.out.println("=== Logging Framework Test Cases ===\n");
        
        // Reset for clean test
        LoggerFactory.resetInstance();
        
        // Test Case 1: Singleton Factory
        System.out.println("=== Test Case 1: Singleton LoggerFactory ===");
        try {
            LoggerFactory factory = LoggerFactory.getInstance();
            LoggerFactory factory2 = LoggerFactory.getInstance();
            System.out.println("✓ Same instance: " + (factory == factory2));
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 2: Console Logging
        System.out.println("=== Test Case 2: Console Logging ===");
        try {
            LoggerFactory factory = LoggerFactory.getInstance();
            factory.addDefaultSink(new ConsoleSink());
            
            Logger logger = factory.getLogger("MyApp");
            logger.info("Application started");
            logger.debug("Debug message (should be filtered at INFO level)");
            logger.warn("Low memory warning");
            logger.error("Connection failed to database");
            System.out.println("✓ Console logging complete");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 3: Log Level Filtering
        System.out.println("=== Test Case 3: Log Level Filtering ===");
        try {
            LoggerFactory factory = LoggerFactory.getInstance();
            Logger logger = factory.getLogger("FilterTest");
            logger.setMinLevel(LogLevel.WARN);
            
            InMemorySink memSink = new InMemorySink(100);
            logger.addSink(memSink);
            
            logger.trace("TRACE - should be filtered");
            logger.debug("DEBUG - should be filtered");
            logger.info("INFO - should be filtered");
            logger.warn("WARN - should appear");
            logger.error("ERROR - should appear");
            logger.fatal("FATAL - should appear");
            
            System.out.println("✓ Messages in memory sink: " + memSink.size() + " (expected 3)");
            System.out.println("  Pass: " + (memSink.size() == 3));
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 4: File Sink
        System.out.println("=== Test Case 4: File Sink ===");
        try {
            FileSink fileSink = new FileSink("app.log");
            
            LoggerFactory factory = LoggerFactory.getInstance();
            Logger logger = factory.getLogger("FileLogger");
            logger.setMinLevel(LogLevel.DEBUG);
            logger.addSink(fileSink);
            
            logger.info("Writing to file sink");
            logger.error("Error logged to file");
            logger.debug("Debug info in file");
            
            System.out.println("✓ File sink lines: " + fileSink.getLineCount());
            System.out.println("  Contents:");
            fileSink.getFileContents().forEach(line -> System.out.println("    " + line));
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 5: Multiple Sinks
        System.out.println("=== Test Case 5: Multiple Sinks ===");
        try {
            ConsoleSink console = new ConsoleSink();
            FileSink file = new FileSink("multi.log");
            InMemorySink memory = new InMemorySink(50);
            
            LoggerFactory factory = LoggerFactory.getInstance();
            Logger logger = factory.getLogger("MultiSink");
            logger.setMinLevel(LogLevel.INFO);
            logger.addSink(console);
            logger.addSink(file);
            logger.addSink(memory);
            
            logger.info("This goes to all 3 sinks");
            logger.error("Error in all sinks");
            
            System.out.println("✓ Console: printed above");
            System.out.println("✓ File lines: " + file.getLineCount());
            System.out.println("✓ Memory messages: " + memory.size());
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 6: Named Loggers (independent config)
        System.out.println("=== Test Case 6: Named Loggers ===");
        try {
            LoggerFactory factory = LoggerFactory.getInstance();
            
            Logger authLogger = factory.getLogger("AuthService");
            Logger dbLogger = factory.getLogger("DatabaseService");
            
            InMemorySink authSink = new InMemorySink(100);
            InMemorySink dbSink = new InMemorySink(100);
            
            authLogger.addSink(authSink);
            authLogger.setMinLevel(LogLevel.WARN);
            
            dbLogger.addSink(dbSink);
            dbLogger.setMinLevel(LogLevel.DEBUG);
            
            authLogger.info("Auth info (filtered)");
            authLogger.error("Auth error (logged)");
            dbLogger.debug("DB debug (logged)");
            dbLogger.info("DB info (logged)");
            
            System.out.println("✓ Auth logs: " + authSink.size() + " (expected 1)");
            System.out.println("✓ DB logs: " + dbSink.size() + " (expected 2)");
            System.out.println("✓ Registered loggers: " + factory.getLoggerNames());
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 7: InMemory Sink Bounded Buffer
        System.out.println("=== Test Case 7: Bounded Buffer (max 3) ===");
        try {
            InMemorySink bounded = new InMemorySink(3);
            
            LoggerFactory factory = LoggerFactory.getInstance();
            Logger logger = factory.getLogger("BoundedTest");
            logger.setMinLevel(LogLevel.TRACE);
            logger.addSink(bounded);
            
            logger.info("Message 1");
            logger.info("Message 2");
            logger.info("Message 3");
            logger.info("Message 4");
            logger.info("Message 5");
            
            System.out.println("✓ Buffer size: " + bounded.size() + " (expected 3, oldest evicted)");
            System.out.println("  Messages:");
            bounded.getBuffer().forEach(m -> System.out.println("    " + m.message));
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // ===== EXCEPTION TEST CASES =====
        
        // Test Case 8: Exception - Closed Sink
        System.out.println("=== Test Case 8: Exception - Closed File Sink ===");
        try {
            FileSink closedSink = new FileSink("closed.log");
            closedSink.close();
            closedSink.write(new LogMessage("test", LogLevel.INFO, "Should fail"));
            System.out.println("✗ Should have thrown LogSinkException");
        } catch (LogSinkException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
            System.out.println("  Sink: " + e.getSinkName());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 9: Exception - Invalid Log Level
        System.out.println("=== Test Case 9: Exception - Null Log Level ===");
        try {
            LoggerFactory factory = LoggerFactory.getInstance();
            factory.setDefaultLevel(null);
            System.out.println("✗ Should have thrown InvalidLogLevelException");
        } catch (InvalidLogLevelException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 10: Exception - Null Sink
        System.out.println("=== Test Case 10: Exception - Null Sink ===");
        try {
            LoggerFactory factory = LoggerFactory.getInstance();
            factory.addDefaultSink(null);
            System.out.println("✗ Should have thrown InvalidLoggerConfigException");
        } catch (InvalidLoggerConfigException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 11: Same logger returned for same name
        System.out.println("=== Test Case 11: Logger Caching ===");
        try {
            LoggerFactory factory = LoggerFactory.getInstance();
            Logger l1 = factory.getLogger("CacheTest");
            Logger l2 = factory.getLogger("CacheTest");
            System.out.println("✓ Same logger instance: " + (l1 == l2));
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 12: Thread Safety
        System.out.println("=== Test Case 12: Concurrent Logging ===");
        try {
            InMemorySink sharedSink = new InMemorySink(1000);
            
            LoggerFactory factory = LoggerFactory.getInstance();
            Logger logger = factory.getLogger("ThreadTest");
            logger.setMinLevel(LogLevel.INFO);
            logger.addSink(sharedSink);
            
            int threadCount = 5;
            int msgsPerThread = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            
            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                new Thread(() -> {
                    for (int i = 0; i < msgsPerThread; i++) {
                        logger.info("Thread-" + threadId + " msg-" + i);
                    }
                    latch.countDown();
                }).start();
            }
            
            latch.await(5, TimeUnit.SECONDS);
            System.out.println("✓ Total messages logged: " + sharedSink.size() + 
                             " (expected " + (threadCount * msgsPerThread) + ")");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        System.out.println("=== All Test Cases Complete! ===");
        
        // Cleanup
        LoggerFactory.resetInstance();
    }
}

/**
 * INTERVIEW DISCUSSION TOPICS:
 * ============================
 * 
 * 1. DESIGN PATTERNS:
 *    Singleton Pattern:
 *      - LoggerFactory is singleton (one per application)
 *      - Thread-safe with double-checked locking
 *      - Why not static methods? (testability, flexibility)
 *    
 *    Strategy Pattern:
 *      - LogSink interface for different destinations
 *      - Easy to add: DatabaseSink, KafkaSink, ElasticSearchSink
 *      - Open/Closed Principle: extend without modifying
 *    
 *    Factory Pattern:
 *      - LoggerFactory creates/caches Logger instances
 *      - Ensures same name returns same logger
 *    
 *    Observer Pattern:
 *      - Logger notifies all sinks (observers) on log event
 *      - Sinks are independent - one failure doesn't affect others
 * 
 * 2. LOG LEVEL HIERARCHY:
 *    TRACE < DEBUG < INFO < WARN < ERROR < FATAL
 *    
 *    Filtering:
 *      - If minLevel = WARN, only WARN/ERROR/FATAL are logged
 *      - Uses ordinal() comparison for efficiency
 *    
 *    Best Practices:
 *      - Production: INFO or WARN
 *      - Development: DEBUG or TRACE
 *      - Critical systems: ERROR only (reduce noise)
 * 
 * 3. PERFORMANCE CONSIDERATIONS:
 *    Synchronous Logging:
 *      - Simple but blocks calling thread
 *      - Acceptable for low-throughput apps
 *    
 *    Asynchronous Logging:
 *      - Use BlockingQueue between producers and consumer thread
 *      - Log4j2 AsyncAppender approach
 *      - LMAX Disruptor for ultra-low latency
 *    
 *    Guard Clauses:
 *      - if (logger.isDebugEnabled()) logger.debug(expensiveMethod())
 *      - Avoids computing message string if level is filtered
 * 
 * 4. LOG FORMATTING:
 *    Plain Text:
 *      - [timestamp] [level] [thread] [logger] message
 *      - Human readable, easy to grep
 *    
 *    Structured (JSON):
 *      - {"timestamp":"...", "level":"INFO", "logger":"...", "message":"..."}
 *      - Machine parsable, great for ELK stack
 *    
 *    Pattern Layout:
 *      - Configurable format: "%d{yyyy-MM-dd} %-5p [%t] %c - %m%n"
 *      - Flexible, industry standard
 * 
 * 5. LOG ROTATION & RETENTION:
 *    Size-based:
 *      - Rotate when file exceeds 100MB
 *      - Keep last 10 files
 *    
 *    Time-based:
 *      - New file daily/hourly
 *      - Delete files older than 30 days
 *    
 *    Compression:
 *      - gzip old log files
 *      - Reduces storage by ~90%
 * 
 * 6. REAL-WORLD FRAMEWORKS:
 *    Java:
 *      - SLF4J (facade) + Logback (implementation)
 *      - Log4j2 (Apache)
 *      - java.util.logging (JUL, built-in)
 *    
 *    Architecture:
 *      - API/Facade layer (what developers use)
 *      - Implementation layer (actual logging logic)
 *      - Appender/Sink layer (output destinations)
 * 
 * 7. DISTRIBUTED LOGGING:
 *    Centralized Logging:
 *      - ELK Stack (Elasticsearch + Logstash + Kibana)
 *      - Splunk, Datadog, CloudWatch Logs
 *    
 *    Correlation IDs:
 *      - Trace requests across microservices
 *      - MDC (Mapped Diagnostic Context)
 *      - OpenTelemetry trace/span IDs
 *    
 *    Log Aggregation:
 *      - Collect from all instances
 *      - Central search and analysis
 *      - Alerting on error patterns
 * 
 * 8. THREAD SAFETY:
 *    - ConcurrentHashMap for logger registry
 *    - Synchronized writes to shared sinks
 *    - Thread-local buffers for high throughput
 *    - Lock-free approaches (Disruptor pattern)
 * 
 * 9. API DESIGN:
 *    LoggerFactory.getInstance()         - Get singleton
 *    factory.getLogger("name")           - Get/create logger
 *    logger.info("message")              - Log at level
 *    logger.setMinLevel(LogLevel.WARN)   - Configure level
 *    logger.addSink(new ConsoleSink())   - Add output
 *    factory.setDefaultLevel(level)      - Global default
 *    factory.addDefaultSink(sink)        - Default sink
 * 
 * 10. ADVANCED FEATURES:
 *     - MDC (Mapped Diagnostic Context) for request context
 *     - Parameterized messages: logger.info("User {} logged in", userId)
 *     - Exception logging: logger.error("Failed", exception)
 *     - Markers for categorization
 *     - Filters beyond level (regex, rate limiting)
 *     - Hot reload of configuration
 *     - Audit logging (tamper-proof, compliance)
 */
