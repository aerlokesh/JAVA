import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

// ===== CUSTOM EXCEPTIONS =====

class IDGenerationException extends Exception {
    public IDGenerationException(String message) {
        super("ID generation failed: " + message);
    }
}

class InvalidConfigException extends Exception {
    public InvalidConfigException(String message) {
        super("Invalid config: " + message);
    }
}

// ===== ENUMS =====

enum IDType { SNOWFLAKE, UUID, AUTO_INCREMENT }

// ===== INTERFACE (Strategy Pattern) =====

interface IDGenerator {
    long nextId() throws IDGenerationException;
    String nextIdString() throws IDGenerationException;
    IDType getType();
}

// ===== 1. SNOWFLAKE GENERATOR (Main Interview Question) =====

/**
 * Twitter Snowflake - 64-bit unique ID
 * 
 * Structure:
 *   [1 bit unused][41 bits timestamp][5 bits datacenter][5 bits machine][12 bits sequence]
 * 
 * - 41 bits timestamp = ~69 years
 * - 5 bits datacenter = 32 datacenters (0-31)
 * - 5 bits machine    = 32 machines per DC (0-31)
 * - 12 bits sequence  = 4096 IDs per millisecond per machine
 * 
 * KEY INTERVIEW POINTS:
 * - Time-sortable (timestamp in MSBs)
 * - No coordination needed between nodes
 * - ~4 billion IDs/sec across cluster
 * - Must handle clock going backward
 */
class SnowflakeGenerator implements IDGenerator {
    private static final long EPOCH = 1577836800000L;   // Jan 1, 2020
    
    private static final int SEQUENCE_BITS   = 12;
    private static final int MACHINE_BITS    = 5;
    private static final int DATACENTER_BITS = 5;
    
    private static final int MAX_SEQUENCE     = (1 << SEQUENCE_BITS) - 1;     // 4095
    private static final int MAX_MACHINE_ID   = (1 << MACHINE_BITS) - 1;     // 31
    private static final int MAX_DATACENTER_ID = (1 << DATACENTER_BITS) - 1; // 31
    
    private static final int MACHINE_SHIFT    = SEQUENCE_BITS;                // 12
    private static final int DATACENTER_SHIFT = SEQUENCE_BITS + MACHINE_BITS; // 17
    private static final int TIMESTAMP_SHIFT  = SEQUENCE_BITS + MACHINE_BITS + DATACENTER_BITS; // 22
    
    private final int datacenterId;
    private final int machineId;
    private long lastTimestamp;
    private int sequence;
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Validate datacenterId in [0, 31] and machineId in [0, 31]
     * 2. Throw InvalidConfigException if out of range
     * 3. Init lastTimestamp = -1, sequence = 0
     */
    public SnowflakeGenerator(int datacenterId, int machineId) throws InvalidConfigException {
        // TODO: Implement
        // HINT: if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID)
        //           throw new InvalidConfigException("DC ID must be 0-31");
        // HINT: if (machineId < 0 || machineId > MAX_MACHINE_ID)
        //           throw new InvalidConfigException("Machine ID must be 0-31");
        // HINT: this.datacenterId = datacenterId;
        // HINT: this.machineId = machineId;
        // HINT: this.lastTimestamp = -1;
        // HINT: this.sequence = 0;
        // BUG FIX: Was hardcoded to 0, must use params + validate!
        if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID)
            throw new InvalidConfigException("DC ID must be 0-" + MAX_DATACENTER_ID);
        if (machineId < 0 || machineId > MAX_MACHINE_ID)
            throw new InvalidConfigException("Machine ID must be 0-" + MAX_MACHINE_ID);
        this.datacenterId = datacenterId;
        this.machineId = machineId;
        this.lastTimestamp = -1;
        this.sequence = 0;
    }
    
    /**
     * Generate next Snowflake ID
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get timestamp = currentTimeMillis() - EPOCH
     * 2. If timestamp < lastTimestamp → clock went backward → throw exception
     * 3. If timestamp == lastTimestamp (same ms):
     *      sequence = (sequence + 1) & MAX_SEQUENCE  (bitmask wraps to 0)
     *      if sequence == 0 → wait for next ms
     * 4. If timestamp > lastTimestamp (new ms):
     *      sequence = 0
     * 5. lastTimestamp = timestamp
     * 6. Build ID with bit shifts:
     *      id = (timestamp << 22) | (datacenterId << 17) | (machineId << 12) | sequence
     * 7. Return id
     */
    @Override
    public synchronized long nextId() throws IDGenerationException {
        // HINT: long timestamp = System.currentTimeMillis() - EPOCH;
        //
        // HINT: if (timestamp < lastTimestamp)
        //     throw new IDGenerationException("Clock moved backward");
        //
        // HINT: if (timestamp == lastTimestamp) {
        //     sequence = (sequence + 1) & MAX_SEQUENCE;
        //     if (sequence == 0) {  // exhausted 4096 IDs in this ms
        //         while (timestamp <= lastTimestamp)
        //             timestamp = System.currentTimeMillis() - EPOCH;
        //     }
        // } else {
        //     sequence = 0;
        // }
        //
        // HINT: lastTimestamp = timestamp;
        //
        // HINT: return (timestamp << TIMESTAMP_SHIFT)
        //     | ((long) datacenterId << DATACENTER_SHIFT)
        //     | ((long) machineId << MACHINE_SHIFT)
        //     | sequence;
        long timestamp = System.currentTimeMillis()-EPOCH;
        if(timestamp<lastTimestamp) throw new IDGenerationException("Clock moved backward");
        if(timestamp==lastTimestamp){
            sequence=(sequence+1)&MAX_SEQUENCE;
            if(sequence==0){
                while(timestamp<=lastTimestamp){
                    timestamp=System.currentTimeMillis()-EPOCH;
                }
            }
        }else{
            sequence=0;
        }
        lastTimestamp=timestamp;
        return (timestamp<<TIMESTAMP_SHIFT)|(datacenterId<<DATACENTER_SHIFT)|(machineId<<MACHINE_SHIFT)|sequence;
    }
    
    /**
     * Parse a Snowflake ID back to its parts (useful for debugging)
     * 
     * IMPLEMENTATION HINTS:
     * 1. timestamp  = (id >> 22) + EPOCH  → gives back millis
     * 2. datacenter = (id >> 17) & 31
     * 3. machine    = (id >> 12) & 31
     * 4. sequence   = id & 4095
     */
    public String parseId(long id) {
        // HINT: long ts = (id >> TIMESTAMP_SHIFT) + EPOCH;
        // HINT: int dc   = (int)((id >> DATACENTER_SHIFT) & MAX_DATACENTER_ID);
        // HINT: int mc   = (int)((id >> MACHINE_SHIFT) & MAX_MACHINE_ID);
        // HINT: int seq  = (int)(id & MAX_SEQUENCE);
        // HINT: return "time=" + Instant.ofEpochMilli(ts) + ", dc=" + dc + ", machine=" + mc + ", seq=" + seq;
        long ts=(id>>TIMESTAMP_SHIFT)+EPOCH;
        long dc=(id>>DATACENTER_SHIFT)&MAX_DATACENTER_ID;
        long mc=(id>>MACHINE_SHIFT)&MAX_MACHINE_ID;  // BUG FIX: was (id & MAX_SEQUENCE) - that's sequence not machine!
        long seq=(id&MAX_SEQUENCE);
        return "time=" + Instant.ofEpochMilli(ts) + ", dc=" + dc + ", machine=" + mc + ", seq=" + seq;
    }
    
    @Override
    public String nextIdString() throws IDGenerationException {
        return String.valueOf(nextId());
    }
    
    @Override
    public IDType getType() { return IDType.SNOWFLAKE; }
}

// ===== 2. AUTO-INCREMENT GENERATOR (Simplest) =====

/**
 * Simple counter with optional prefix. Like MySQL AUTO_INCREMENT.
 * Single-node only. Thread-safe via AtomicLong.
 * 
 * INTERVIEW: Discuss why this fails for distributed systems
 * (single point of failure, reveals business metrics)
 */
class AutoIncrementGenerator implements IDGenerator {
    private final AtomicLong counter;
    private final String prefix;
    
    public AutoIncrementGenerator(long startFrom, String prefix) {
        this.counter = new AtomicLong(startFrom);
        this.prefix = prefix != null ? prefix : "";
    }
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. return counter.incrementAndGet();
     */
    @Override
    public long nextId() throws IDGenerationException {
        // HINT: return counter.incrementAndGet();
        return counter.incrementAndGet();
    }
    
    @Override
    public String nextIdString() throws IDGenerationException {
        // HINT: return prefix + counter.incrementAndGet();
        return prefix+counter.incrementAndGet();
    }
    
    @Override
    public IDType getType() { return IDType.AUTO_INCREMENT; }
}

// ===== 3. UUID GENERATOR =====

/**
 * 128-bit random UUID. No coordination needed.
 * INTERVIEW: Discuss why UUID is bad for DB indexes (random → page splits)
 */
class UUIDGenerator implements IDGenerator {
    
    @Override
    public long nextId() throws IDGenerationException {
        // HINT: return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        return UUID.randomUUID().getMostSignificantBits()&Long.MAX_VALUE;
    }
    
    @Override
    public String nextIdString() throws IDGenerationException {
        // HINT: return UUID.randomUUID().toString();
        return UUID.randomUUID().toString();
    }
    
    @Override
    public IDType getType() { return IDType.UUID; }
}

// ===== SERVICE =====

/**
 * ID Generator Service - Low Level Design (LLD)
 * 
 * PROBLEM: Design a distributed unique ID generation system
 * 
 * KEY REQUIREMENTS:
 * 1. Unique across all nodes (no duplicates)
 * 2. Roughly time-sortable (Snowflake)
 * 3. High throughput (millions/sec)
 * 4. 64-bit numeric (fits in long, good for DB)
 * 
 * PATTERNS: Strategy (swappable generators), Factory
 */
class IDGeneratorService {
    private final Map<String, IDGenerator> generators;
    
    public IDGeneratorService() {
        this.generators = new HashMap<>();
    }
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Put generator into map with name as key
     */
    public void register(String name, IDGenerator generator) {
        // HINT: generators.put(name, generator);
        // HINT: System.out.println("  ✓ Registered: " + name + " (" + generator.getType() + ")");
        generators.put(name, generator);
        System.out.println("  ✓ Registered: " + name + " (" + generator.getType() + ")");
    }
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Get generator from map
     * 2. If null → throw IDGenerationException
     * 3. Return generator.nextId()
     */
    public long generateId(String name) throws IDGenerationException {
        // HINT: IDGenerator gen = generators.get(name);
        // HINT: if (gen == null) throw new IDGenerationException("Unknown generator: " + name);
        // HINT: return gen.nextId();
        IDGenerator gen=generators.get(name);
        if(gen==null) throw new IDGenerationException("Unknow Generator "+name);
        return gen.nextId();
    }
    
    public String generateIdString(String name) throws IDGenerationException {
        // HINT: IDGenerator gen = generators.get(name);
        // HINT: if (gen == null) throw new IDGenerationException("Unknown generator: " + name);
        // HINT: return gen.nextIdString();
        IDGenerator gen=generators.get(name);
        if(gen==null)  throw new IDGenerationException("Unknown generator: " + name);
        return gen.nextIdString();
    }
    
    /**
     * Generate a batch of IDs
     * 
     * IMPLEMENTATION HINTS:
     * 1. Loop count times, call generateId each time
     * 2. Collect into list and return
     */
    public List<Long> generateBatch(String name, int count) throws IDGenerationException {
        // HINT: List<Long> ids = new ArrayList<>();
        // HINT: for (int i = 0; i < count; i++) ids.add(generateId(name));
        // HINT: return ids;
        List<Long> ids=new ArrayList<>();
        for (int i = 0; i < count; i++) ids.add(generateId(name));
        return ids;
    }
    
    public IDGenerator getGenerator(String name) { return generators.get(name); }
}

// ===== MAIN TEST CLASS =====

public class IDGeneratorSystem {
    public static void main(String[] args) {
        System.out.println("=== ID Generator System Test Cases ===\n");
        
        IDGeneratorService service = new IDGeneratorService();
        
        // Setup
        System.out.println("=== Setup: Register Generators ===");
        try {
            service.register("snowflake", new SnowflakeGenerator(1, 1));
            service.register("uuid", new UUIDGenerator());
            service.register("user-id", new AutoIncrementGenerator(1000, "USR-"));
            service.register("order-id", new AutoIncrementGenerator(5000, "ORD-"));
        } catch (Exception e) {
            System.out.println("✗ Setup error: " + e.getMessage());
        }
        System.out.println();
        
        // Test 1: Snowflake IDs
        System.out.println("=== Test 1: Snowflake IDs ===");
        try {
            long id1 = service.generateId("snowflake");
            long id2 = service.generateId("snowflake");
            long id3 = service.generateId("snowflake");
            System.out.println("✓ ID1: " + id1);
            System.out.println("✓ ID2: " + id2);
            System.out.println("✓ ID3: " + id3);
            System.out.println("  Sequential: " + (id1 < id2 && id2 < id3));
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test 2: Parse Snowflake ID
        System.out.println("=== Test 2: Parse Snowflake ID ===");
        try {
            long id = service.generateId("snowflake");
            SnowflakeGenerator sf = (SnowflakeGenerator) service.getGenerator("snowflake");
            String parsed = sf.parseId(id);
            System.out.println("✓ ID: " + id);
            System.out.println("  Parsed: " + (parsed != null ? parsed : "null (implement parseId!)"));
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test 3: UUID
        System.out.println("=== Test 3: UUID IDs ===");
        try {
            String uuid1 = service.generateIdString("uuid");
            String uuid2 = service.generateIdString("uuid");
            System.out.println("✓ UUID1: " + uuid1);
            System.out.println("✓ UUID2: " + uuid2);
            System.out.println("  Unique: " + (uuid1 != null && !uuid1.equals(uuid2)));
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test 4: Auto-Increment with Prefix
        System.out.println("=== Test 4: Auto-Increment IDs ===");
        try {
            System.out.println("✓ " + service.generateIdString("user-id"));
            System.out.println("✓ " + service.generateIdString("user-id"));
            System.out.println("✓ " + service.generateIdString("order-id"));
            System.out.println("✓ " + service.generateIdString("order-id"));
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test 5: Batch Generation
        System.out.println("=== Test 5: Batch Generation ===");
        try {
            List<Long> batch = service.generateBatch("snowflake", 5);
            System.out.println("✓ Batch of 5:");
            if (batch != null) {
                batch.forEach(id -> System.out.println("    " + id));
                System.out.println("  All unique: " + (new HashSet<>(batch).size() == 5));
            }
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test 6: Uniqueness (10K IDs)
        System.out.println("=== Test 6: Uniqueness Test (10K IDs) ===");
        try {
            Set<Long> ids = new HashSet<>();
            for (int i = 0; i < 10000; i++) ids.add(service.generateId("snowflake"));
            System.out.println("✓ Generated 10000, Unique: " + ids.size() + ", Dups: " + (10000 - ids.size()));
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test 7: Multi-Node Snowflake (different DC/machine → different IDs)
        System.out.println("=== Test 7: Multi-Node Snowflake ===");
        try {
            SnowflakeGenerator node1 = new SnowflakeGenerator(1, 1);
            SnowflakeGenerator node2 = new SnowflakeGenerator(1, 2);
            SnowflakeGenerator node3 = new SnowflakeGenerator(2, 1);
            long a = node1.nextId(), b = node2.nextId(), c = node3.nextId();
            System.out.println("✓ DC1-M1: " + a);
            System.out.println("✓ DC1-M2: " + b);
            System.out.println("✓ DC2-M1: " + c);
            System.out.println("  All different: " + (a != b && b != c));
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test 8: Exception - Invalid Config
        System.out.println("=== Test 8: Exception - Invalid Config ===");
        try {
            new SnowflakeGenerator(100, 1);
            System.out.println("✗ Should have thrown InvalidConfigException");
        } catch (InvalidConfigException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test 9: Exception - Unknown Generator
        System.out.println("=== Test 9: Exception - Unknown Generator ===");
        try {
            service.generateId("nonexistent");
            System.out.println("✗ Should have thrown IDGenerationException");
        } catch (IDGenerationException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION TOPICS:
 * ============================
 * 
 * 1. WHY SNOWFLAKE?
 *    - 64-bit → fits in a long, great for DB primary keys
 *    - Time-sortable → recent IDs > older IDs
 *    - No coordination → each node generates independently
 *    - ~4M IDs/sec/node (4096/ms)
 * 
 * 2. SNOWFLAKE BIT LAYOUT:
 *    [0][41 bits timestamp][5 bits DC][5 bits machine][12 bits sequence]
 *    
 *    To build:  id = (ts << 22) | (dc << 17) | (machine << 12) | sequence
 *    To parse:  ts  = (id >> 22) + EPOCH
 *               dc  = (id >> 17) & 0x1F
 *               mc  = (id >> 12) & 0x1F
 *               seq = id & 0xFFF
 * 
 * 3. CLOCK BACKWARD PROBLEM:
 *    - NTP can adjust clock backward
 *    - Solutions: throw error, wait, use last known ts
 * 
 * 4. COMPARISON:
 *    Snowflake:    64-bit, sortable, distributed, no coordination
 *    UUID:         128-bit, random, bad for DB indexes (page splits)
 *    Auto-Inc:     sequential, single-node, reveals business data
 *    Range-based:  pre-allocate blocks per node (Flickr approach)
 * 
 * 5. REAL-WORLD:
 *    Twitter/Discord: Snowflake
 *    Instagram: Modified Snowflake (41+13+10)
 *    MongoDB: ObjectId (timestamp + machine + PID + counter)
 * 
 * 6. DB INDEX IMPACT:
 *    Sequential IDs → B-tree append-only → fast inserts
 *    Random UUIDs → random B-tree inserts → page splits → slow
 * 
 * 7. API:
 *    POST /ids/generate?type=snowflake
 *    POST /ids/batch?type=snowflake&count=100
 *    GET  /ids/parse/{id}
 */
