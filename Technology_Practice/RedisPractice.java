/**
 * RedisPractice.java
 * 
 * A comprehensive Java practice file for mastering Redis operations using Jedis library.
 * Complete all the TODO methods below and run the main method to test your solutions.
 * 
 * QUICK START (Single Command):
 * ============================
 * ./run-redis.sh
 * 
 * This script automatically:
 * - Starts Redis server if not running
 * - Compiles with all dependencies
 * - Runs the test suite
 * 
 * 
 * Instructions:
 * 1. Read each method's description carefully
 * 2. Replace the "TODO: Implement this method" with your solution
 * 3. All methods use Jedis client to interact with Redis
 * 4. Run ./run-redis.sh to test your solutions
 */

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import java.util.*;

public class RedisPractice {
    
    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;
    
    // ==================== STRING OPERATIONS ====================
    
    /**
     * Task 1: Set a string value in Redis
     * @param jedis Redis client
     * @param key The key
     * @param value The value
     */
    public static void setString(Jedis jedis, String key, String value) {
        // TODO: Use jedis.set(key, value)
        jedis.set(key,value);

    }
    
    /**
     * Task 2: Get a string value from Redis
     * @param jedis Redis client
     * @param key The key
     * @return The value or null
     */
    public static String getString(Jedis jedis, String key) {
        // TODO: Use jedis.get(key)
        return jedis.get(key);
    }
    
    /**
     * Task 3: Set string with expiration (TTL)
     * @param jedis Redis client
     * @param key The key
     * @param value The value
     * @param seconds Expiration in seconds
     */
    public static void setWithExpiry(Jedis jedis, String key, String value, int seconds) {
        // TODO: Use jedis.setex(key, (long)seconds, value) - parameter order: key, long, value
        jedis.setex(key, (long)seconds, value);
    }
    
    /**
     * Task 4: Increment a counter
     * @param jedis Redis client
     * @param key The key
     * @return New value after increment
     */
    public static long incrementCounter(Jedis jedis, String key) {
        // TODO: Use jedis.incr(key)
        return jedis.incr(key);
    }
    
    /**
     * Task 5: Increment by a specific amount
     * @param jedis Redis client
     * @param key The key
     * @param amount Amount to increment
     * @return New value
     */
    public static long incrementBy(Jedis jedis, String key, long amount) {
        // TODO: Use jedis.incrBy(key, amount) - needs BOTH key and amount
        return jedis.incrBy(key, amount);
    }
    
    /**
     * Task 6: Check if key exists
     * @param jedis Redis client
     * @param key The key
     * @return true if exists
     */
    public static boolean exists(Jedis jedis, String key) {
        // TODO: Use jedis.exists(key)
        return jedis.exists(key);
    }
    
    /**
     * Task 7: Delete a key
     * @param jedis Redis client
     * @param key The key
     * @return Number of keys deleted
     */
    public static long deleteKey(Jedis jedis, String key) {
        // TODO: Use jedis.del(key)
        return jedis.del(key);
    }
    
    /**
     * Task 8: Set multiple key-value pairs at once
     * @param jedis Redis client
     * @param keyValues Map of key-value pairs
     */
    public static void setMultiple(Jedis jedis, Map<String, String> keyValues) {
        // TODO: Convert map to String array, then use jedis.mset(String...)
        // Example: flatten map to ["key1", "value1", "key2", "value2", ...]
        String[] keysAndValues = keyValues.entrySet().stream()
            .flatMap(e -> java.util.stream.Stream.of(e.getKey(), e.getValue()))
            .toArray(String[]::new);
        jedis.mset(keysAndValues);
    }
    
    /**
     * Task 9: Get multiple values at once
     * @param jedis Redis client
     * @param keys Array of keys
     * @return List of values
     */
    public static List<String> getMultiple(Jedis jedis, String... keys) {
        // TODO: Use jedis.mget(keys)
        return jedis.mget(keys);
    }
    
    /**
     * Task 10: Append to existing string
     * @param jedis Redis client
     * @param key The key
     * @param value Value to append
     * @return New length
     */
    public static long appendString(Jedis jedis, String key, String value) {
        // TODO: Use jedis.append(key, value)
        return jedis.append(key,value);
    }
    
    // ==================== LIST OPERATIONS ====================
    
    /**
     * Task 11: Push elements to list (left side)
     * @param jedis Redis client
     * @param key List key
     * @param values Values to push
     * @return Length of list after push
     */
    public static long leftPush(Jedis jedis, String key, String... values) {
        // TODO: Use jedis.lpush(key, values)
        return jedis.lpush(key,values);
    }
    
    /**
     * Task 12: Push elements to list (right side)
     * @param jedis Redis client
     * @param key List key
     * @param values Values to push
     * @return Length of list after push
     */
    public static long rightPush(Jedis jedis, String key, String... values) {
        // TODO: Use jedis.rpush(key, values)
        return jedis.rpush(key,values);
    }
    
    /**
     * Task 13: Pop element from left
     * @param jedis Redis client
     * @param key List key
     * @return Popped element
     */
    public static String leftPop(Jedis jedis, String key) {
        // TODO: Use jedis.lpop(key)
        return jedis.lpop(key);
    }
    
    /**
     * Task 14: Pop element from right
     * @param jedis Redis client
     * @param key List key
     * @return Popped element
     */
    public static String rightPop(Jedis jedis, String key) {
        // TODO: Use jedis.rpop(key)
        return jedis.rpop(key);
    }
    
    /**
     * Task 15: Get list length
     * @param jedis Redis client
     * @param key List key
     * @return Length of list
     */
    public static long getListLength(Jedis jedis, String key) {
        // TODO: Use jedis.llen(key)
        return jedis.llen(key);
    }
    
    /**
     * Task 16: Get range of elements from list
     * @param jedis Redis client
     * @param key List key
     * @param start Start index
     * @param end End index
     * @return List of elements
     */
    public static List<String> getListRange(Jedis jedis, String key, long start, long end) {
        // TODO: Use jedis.lrange(key, start, end)
        return jedis.lrange(key,start,end);
    }
    
    /**
     * Task 17: Get element at index
     * @param jedis Redis client
     * @param key List key
     * @param index Index
     * @return Element at index
     */
    public static String getListIndex(Jedis jedis, String key, long index) {
        // TODO: Use jedis.lindex(key, index)
        return jedis.lindex(key,index);
    }
    
    /**
     * Task 18: Set element at index
     * @param jedis Redis client
     * @param key List key
     * @param index Index
     * @param value New value
     */
    public static void setListIndex(Jedis jedis, String key, long index, String value) {
        // TODO: Use jedis.lset(key, index, value)
        jedis.lset(key,index,value);
    }
    
    /**
     * Task 19: Trim list to specified range
     * @param jedis Redis client
     * @param key List key
     * @param start Start index
     * @param end End index
     */
    public static void trimList(Jedis jedis, String key, long start, long end) {
        // TODO: Use jedis.ltrim(key, start, end)
        jedis.ltrim(key,start,end);
    }
    
    /**
     * Task 20: Remove elements from list
     * @param jedis Redis client
     * @param key List key
     * @param count Number of occurrences to remove
     * @param value Value to remove
     * @return Number of removed elements
     */
    public static long removeFromList(Jedis jedis, String key, long count, String value) {
        // TODO: Use jedis.lrem(key, count, value)
        return jedis.lrem(key,count,value);
    }
    
    // ==================== SET OPERATIONS ====================
    
    /**
     * Task 21: Add members to set
     * @param jedis Redis client
     * @param key Set key
     * @param members Members to add
     * @return Number of added members
     */
    public static long addToSet(Jedis jedis, String key, String... members) {
        // TODO: Use jedis.sadd(key, members)
        return jedis.sadd(key,members);
    }
    
    /**
     * Task 22: Get all members of set
     * @param jedis Redis client
     * @param key Set key
     * @return Set of members
     */
    public static Set<String> getSetMembers(Jedis jedis, String key) {
        // TODO: Use jedis.smembers(key)
        return jedis.smembers(key);
    }
    
    /**
     * Task 23: Check if member exists in set
     * @param jedis Redis client
     * @param key Set key
     * @param member Member to check
     * @return true if member exists
     */
    public static boolean isMemberOfSet(Jedis jedis, String key, String member) {
        // TODO: Use jedis.sismember(key, member)
        return jedis.sismember(key,member);
    }
    
    /**
     * Task 24: Remove member from set
     * @param jedis Redis client
     * @param key Set key
     * @param members Members to remove
     * @return Number of removed members
     */
    public static long removeFromSet(Jedis jedis, String key, String... members) {
        // TODO: Use jedis.srem(key, members)
        return jedis.srem(key,members);
    }
    
    /**
     * Task 25: Get set size
     * @param jedis Redis client
     * @param key Set key
     * @return Size of set
     */
    public static long getSetSize(Jedis jedis, String key) {
        // TODO: Use jedis.scard(key)
        return jedis.scard(key);
    }
    
    /**
     * Task 26: Get random member from set
     * @param jedis Redis client
     * @param key Set key
     * @return Random member
     */
    public static String getRandomMember(Jedis jedis, String key) {
        // TODO: Use jedis.srandmember(key)
        return jedis.srandmember(key);
    }
    
    /**
     * Task 27: Pop random member from set
     * @param jedis Redis client
     * @param key Set key
     * @return Popped member
     */
    public static String popFromSet(Jedis jedis, String key) {
        // TODO: Use jedis.spop(key)
        return jedis.spop(key);
    }
    
    /**
     * Task 28: Get union of two sets
     * @param jedis Redis client
     * @param key1 First set
     * @param key2 Second set
     * @return Union of sets
     */
    public static Set<String> setUnion(Jedis jedis, String key1, String key2) {
        // TODO: Use jedis.sunion(key1, key2)
        return jedis.sunion(key1, key2);
    }
    
    /**
     * Task 29: Get intersection of two sets
     * @param jedis Redis client
     * @param key1 First set
     * @param key2 Second set
     * @return Intersection of sets
     */
    public static Set<String> setIntersection(Jedis jedis, String key1, String key2) {
        // TODO: Use jedis.sinter(key1, key2)
        return jedis.sinter(key1,key2);
    }
    
    /**
     * Task 30: Get difference of two sets (key1 - key2)
     * @param jedis Redis client
     * @param key1 First set
     * @param key2 Second set
     * @return Difference of sets
     */
    public static Set<String> setDifference(Jedis jedis, String key1, String key2) {
        // TODO: Use jedis.sdiff(key1, key2)
        return jedis.sdiff(key1,key2);
    }
    
    // ==================== HASH OPERATIONS ====================
    
    /**
     * Task 31: Set field in hash
     * @param jedis Redis client
     * @param key Hash key
     * @param field Field name
     * @param value Field value
     */
    public static void setHashField(Jedis jedis, String key, String field, String value) {
        // TODO: Use jedis.hset(key, field, value)
        jedis.hset(key, field, value);
    }
    
    /**
     * Task 32: Get field from hash
     * @param jedis Redis client
     * @param key Hash key
     * @param field Field name
     * @return Field value
     */
    public static String getHashField(Jedis jedis, String key, String field) {
        // TODO: Use jedis.hget(key, field)
        return jedis.hget(key, field);
    }
    
    /**
     * Task 33: Set multiple fields in hash
     * @param jedis Redis client
     * @param key Hash key
     * @param hash Map of field-value pairs
     */
    public static void setHashMultiple(Jedis jedis, String key, Map<String, String> hash) {
        // TODO: Use jedis.hset(key, hash)
        jedis.hset(key, hash);
    }
    
    /**
     * Task 34: Get all fields and values from hash
     * @param jedis Redis client
     * @param key Hash key
     * @return Map of field-value pairs
     */
    public static Map<String, String> getAllHash(Jedis jedis, String key) {
        // TODO: Use jedis.hgetAll(key)
        return jedis.hgetAll(key);
    }
    
    /**
     * Task 35: Check if field exists in hash
     * @param jedis Redis client
     * @param key Hash key
     * @param field Field name
     * @return true if field exists
     */
    public static boolean hashFieldExists(Jedis jedis, String key, String field) {
        // TODO: Use jedis.hexists(key, field)
        return jedis.hexists(key, field);
    }
    
    /**
     * Task 36: Delete field from hash
     * @param jedis Redis client
     * @param key Hash key
     * @param fields Fields to delete
     * @return Number of deleted fields
     */
    public static long deleteHashField(Jedis jedis, String key, String... fields) {
        // TODO: Use jedis.hdel(key, fields)
        return jedis.hdel(key, fields);
    }
    
    /**
     * Task 37: Get all field names from hash
     * @param jedis Redis client
     * @param key Hash key
     * @return Set of field names
     */
    public static Set<String> getHashKeys(Jedis jedis, String key) {
        // TODO: Use jedis.hkeys(key)
        return jedis.hkeys(key);
    }
    
    /**
     * Task 38: Get all values from hash
     * @param jedis Redis client
     * @param key Hash key
     * @return List of values
     */
    public static List<String> getHashValues(Jedis jedis, String key) {
        // TODO: Use jedis.hvals(key)
        return jedis.hvals(key);
    }
    
    /**
     * Task 39: Get hash size
     * @param jedis Redis client
     * @param key Hash key
     * @return Number of fields in hash
     */
    public static long getHashSize(Jedis jedis, String key) {
        // TODO: Use jedis.hlen(key)
        return jedis.hlen(key);
    }
    
    /**
     * Task 40: Increment hash field value
     * @param jedis Redis client
     * @param key Hash key
     * @param field Field name
     * @param amount Amount to increment
     * @return New value
     */
    public static long incrementHashField(Jedis jedis, String key, String field, long amount) {
        // TODO: Use jedis.hincrBy(key, field, amount)
        return jedis.hincrBy(key, field, amount);
    }
    
    // ==================== SORTED SET OPERATIONS ====================
    
    /**
     * Task 41: Add member to sorted set with score
     * @param jedis Redis client
     * @param key Sorted set key
     * @param score Score
     * @param member Member
     * @return Number of added members
     */
    public static long addToSortedSet(Jedis jedis, String key, double score, String member) {
        // TODO: Use jedis.zadd(key, score, member)
        return jedis.zadd(key, score, member);
    }
    
    /**
     * Task 42: Get sorted set range (by rank, ascending)
     * @param jedis Redis client
     * @param key Sorted set key
     * @param start Start index
     * @param end Index
     * @return List of members
     */
    public static List<String> getSortedSetRange(Jedis jedis, String key, long start, long end) {
        // TODO: Use jedis.zrange(key, start, end)
        return new ArrayList<>(jedis.zrange(key, start, end));
    }
    
    /**
     * Task 43: Get sorted set range (descending)
     * @param jedis Redis client
     * @param key Sorted set key
     * @param start Start index
     * @param end End index
     * @return List of members (highest score first)
     */
    public static List<String> getSortedSetRangeReverse(Jedis jedis, String key, long start, long end) {
        // TODO: Use jedis.zrevrange(key, start, end)
        return new ArrayList<>(jedis.zrevrange(key, start, end));
    }
    
    /**
     * Task 44: Get member score
     * @param jedis Redis client
     * @param key Sorted set key
     * @param member Member
     * @return Score of member
     */
    public static Double getScore(Jedis jedis, String key, String member) {
        // TODO: Use jedis.zscore(key, member)
        return jedis.zscore(key,member);
    }
    
    /**
     * Task 45: Get member rank (0-based, ascending)
     * @param jedis Redis client
     * @param key Sorted set key
     * @param member Member
     * @return Rank of member
     */
    public static Long getRank(Jedis jedis, String key, String member) {
        // TODO: Use jedis.zrank(key, member)
        return jedis.zrank(key,member);
    }
    
    /**
     * Task 46: Increment member score
     * @param jedis Redis client
     * @param key Sorted set key
     * @param increment Amount to increment
     * @param member Member
     * @return New score
     */
    public static double incrementScore(Jedis jedis, String key, double increment, String member) {
        // TODO: Use jedis.zincrby(key, increment, member)
        return jedis.zincrby(key,increment,member);
    }
    
    /**
     * Task 47: Remove member from sorted set
     * @param jedis Redis client
     * @param key Sorted set key
     * @param members Members to remove
     * @return Number of removed members
     */
    public static long removeFromSortedSet(Jedis jedis, String key, String... members) {
        // TODO: Use jedis.zrem(key, members)
        return jedis.zrem(key,members);
    }
    
    /**
     * Task 48: Get sorted set size
     * @param jedis Redis client
     * @param key Sorted set key
     * @return Number of members
     */
    public static long getSortedSetSize(Jedis jedis, String key) {
        // TODO: Use jedis.zcard(key)
        return jedis.zcard(key);
    }
    
    /**
     * Task 49: Count members in score range
     * @param jedis Redis client
     * @param key Sorted set key
     * @param min Minimum score
     * @param max Maximum score
     * @return Count of members in range
     */
    public static long countInScoreRange(Jedis jedis, String key, double min, double max) {
        // TODO: Use jedis.zcount(key, min, max)
        return jedis.zcount(key,min,max);
    }
    
    /**
     * Task 50: Get members by score range
     * @param jedis Redis client
     * @param key Sorted set key
     * @param min Minimum score
     * @param max Maximum score
     * @return List of members
     */
    public static List<String> getMembersByScoreRange(Jedis jedis, String key, double min, double max) {
        // TODO: Use jedis.zrangeByScore(key, min, max)
        return jedis.zrangeByScore(key,min,max);
    }
    
    // ==================== ADVANCED OPERATIONS ====================
    
    /**
     * Task 51: Set key expiration time
     * @param jedis Redis client
     * @param key The key
     * @param seconds Expiration in seconds
     * @return true if timeout was set
     */
    public static boolean setExpire(Jedis jedis, String key, int seconds) {
        // TODO: Use jedis.expire(key, seconds) - returns long, convert to boolean
        return jedis.expire(key, seconds) == 1;
    }
    
    /**
     * Task 52: Get time to live for key
     * @param jedis Redis client
     * @param key The key
     * @return TTL in seconds (-1 if no expiry, -2 if key doesn't exist)
     */
    public static long getTTL(Jedis jedis, String key) {
        // TODO: Use jedis.ttl(key)
        return jedis.ttl(key);
    }
    
    /**
     * Task 53: Set if not exists (SETNX)
     * @param jedis Redis client
     * @param key The key
     * @param value The value
     * @return true if set, false if key already exists
     */
    public static boolean setIfNotExists(Jedis jedis, String key, String value) {
        // TODO: Use jedis.setnx(key, value) == 1
        return jedis.setnx(key, value) == 1;
    }
    
    /**
     * Task 54: Get and set atomically
     * @param jedis Redis client
     * @param key The key
     * @param newValue New value to set
     * @return Old value
     */
    public static String getAndSet(Jedis jedis, String key, String newValue) {
        // TODO: Use jedis.getSet(key, newValue)
        return jedis.getSet(key,newValue);
    }
    
    /**
     * Task 55: Decrement a counter
     * @param jedis Redis client
     * @param key The key
     * @return New value after decrement
     */
    public static long decrementCounter(Jedis jedis, String key) {
        // TODO: Use jedis.decr(key)
        return jedis.decr(key);
    }
    
    // ==================== TEST CASES ====================
    
    public static void main(String[] args) {
        int totalTests = 0;
        int passedTests = 0;
        
        System.out.println("=".repeat(60));
        System.out.println("REDIS PRACTICE - TEST SUITE");
        System.out.println("=".repeat(60));
        System.out.println("\nConnecting to Redis at " + REDIS_HOST + ":" + REDIS_PORT + "...");
        
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            // Test connection
            jedis.ping();
            System.out.println("✓ Connected to Redis successfully!");
            
            // Clean up before tests
            jedis.flushDB();
            System.out.println("✓ Database cleaned\n");
            
            // ==================== STRING TESTS ====================
            System.out.println("=== STRING OPERATIONS ===");
            
            // Test 1: setString & getString
            System.out.println("\n[Test 1-2] String set/get");
            setString(jedis, "name", "Redis");
            String result1 = getString(jedis, "name");
            totalTests++; if (testString(result1, "Redis", "Set and get string")) passedTests++;
            
            // Test 3: setWithExpiry
            System.out.println("\n[Test 3] String with expiry");
            setWithExpiry(jedis, "temp", "value", 2);
            String result3a = getString(jedis, "temp");
            totalTests++; if (testString(result3a, "value", "Set with expiry (before)")) passedTests++;
            try { Thread.sleep(2100); } catch (InterruptedException e) {}
            String result3b = getString(jedis, "temp");
            totalTests++; if (testString(result3b, null, "Set with expiry (after)")) passedTests++;
            
            // Test 4: incrementCounter
            System.out.println("\n[Test 4] Increment counter");
            long result4 = incrementCounter(jedis, "counter");
            totalTests++; if (testLong(result4, 1L, "Increment counter")) passedTests++;
            
            // Test 5: incrementBy
            System.out.println("\n[Test 5] Increment by amount");
            long result5 = incrementBy(jedis, "counter", 5);
            totalTests++; if (testLong(result5, 6L, "Increment by 5")) passedTests++;
            
            // Test 6: exists
            System.out.println("\n[Test 6] Key exists");
            boolean result6 = exists(jedis, "counter");
            totalTests++; if (testBoolean(result6, true, "Key exists")) passedTests++;
            
            // Test 7: deleteKey
            System.out.println("\n[Test 7] Delete key");
            long result7 = deleteKey(jedis, "counter");
            totalTests++; if (testLong(result7, 1L, "Delete key")) passedTests++;
            
            // Test 8-9: setMultiple & getMultiple
            System.out.println("\n[Test 8-9] Multiple set/get");
            Map<String, String> kvMap = new HashMap<>();
            kvMap.put("key1", "value1");
            kvMap.put("key2", "value2");
            setMultiple(jedis, kvMap);
            List<String> result9 = getMultiple(jedis, "key1", "key2");
            totalTests++; if (result9 != null && result9.equals(Arrays.asList("value1", "value2"))) {
                System.out.println("  ✓ PASS: Multiple set/get");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Multiple set/get");
            }
            
            // Test 10: appendString
            System.out.println("\n[Test 10] Append string");
            setString(jedis, "msg", "Hello");
            long result10 = appendString(jedis, "msg", " World");
            totalTests++; if (testLong(result10, 11L, "Append string")) passedTests++;
            
            // ==================== LIST TESTS ====================
            System.out.println("\n=== LIST OPERATIONS ===");
            
            // Test 11-12: leftPush & rightPush
            System.out.println("\n[Test 11-12] List push operations");
            leftPush(jedis, "list1", "a", "b");
            rightPush(jedis, "list1", "c", "d");
            List<String> result11 = getListRange(jedis, "list1", 0, -1);
            totalTests++; if (result11 != null && result11.equals(Arrays.asList("b", "a", "c", "d"))) {
                System.out.println("  ✓ PASS: List push operations");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: List push operations");
            }
            
            // Test 13-14: leftPop & rightPop
            System.out.println("\n[Test 13-14] List pop operations");
            String result13 = leftPop(jedis, "list1");
            String result14 = rightPop(jedis, "list1");
            totalTests++; if (testString(result13, "b", "Left pop") && testString(result14, "d", "Right pop")) passedTests++;
            
            // Test 15: getListLength
            System.out.println("\n[Test 15] List length");
            long result15 = getListLength(jedis, "list1");
            totalTests++; if (testLong(result15, 2L, "List length")) passedTests++;
            
            // Test 16: getListRange
            System.out.println("\n[Test 16] List range");
            rightPush(jedis, "list2", "1", "2", "3", "4", "5");
            List<String> result16 = getListRange(jedis, "list2", 1, 3);
            totalTests++; if (result16 != null && result16.equals(Arrays.asList("2", "3", "4"))) {
                System.out.println("  ✓ PASS: List range [1, 3]");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: List range");
            }
            
            // Test 17-18: getListIndex & setListIndex
            System.out.println("\n[Test 17-18] List index operations");
            String result17 = getListIndex(jedis, "list2", 2);
            setListIndex(jedis, "list2", 2, "X");
            String result18 = getListIndex(jedis, "list2", 2);
            totalTests++; if (testString(result17, "3", "Get index") && testString(result18, "X", "Set index")) passedTests++;
            
            // Test 19: trimList
            System.out.println("\n[Test 19] Trim list");
            trimList(jedis, "list2", 0, 2);
            long result19 = getListLength(jedis, "list2");
            totalTests++; if (testLong(result19, 3L, "Trim list to 3 elements")) passedTests++;
            
            // Test 20: removeFromList
            System.out.println("\n[Test 20] Remove from list");
            rightPush(jedis, "list3", "a", "b", "a", "c", "a");
            long result20 = removeFromList(jedis, "list3", 2, "a");
            totalTests++; if (testLong(result20, 2L, "Remove 2 occurrences of 'a'")) passedTests++;
            
            // ==================== SET TESTS ====================
            System.out.println("\n=== SET OPERATIONS ===");
            
            // Test 21-22: addToSet & getSetMembers
            System.out.println("\n[Test 21-22] Set add/get");
            addToSet(jedis, "set1", "apple", "banana", "cherry");
            Set<String> result22 = getSetMembers(jedis, "set1");
            totalTests++; if (result22 != null && result22.size() == 3) {
                System.out.println("  ✓ PASS: Set add/get (size: 3)");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Set add/get");
            }
            
            // Test 23: isMemberOfSet
            System.out.println("\n[Test 23] Is member of set");
            boolean result23 = isMemberOfSet(jedis, "set1", "banana");
            totalTests++; if (testBoolean(result23, true, "Is member of set")) passedTests++;
            
            // Test 24: removeFromSet
            System.out.println("\n[Test 24] Remove from set");
            long result24 = removeFromSet(jedis, "set1", "banana");
            totalTests++; if (testLong(result24, 1L, "Remove from set")) passedTests++;
            
            // Test 25: getSetSize
            System.out.println("\n[Test 25] Set size");
            long result25 = getSetSize(jedis, "set1");
            totalTests++; if (testLong(result25, 2L, "Set size")) passedTests++;
            
            // Test 26: getRandomMember
            System.out.println("\n[Test 26] Random member");
            String result26 = getRandomMember(jedis, "set1");
            totalTests++; if (result26 != null && (result26.equals("apple") || result26.equals("cherry"))) {
                System.out.println("  ✓ PASS: Get random member => " + result26);
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Get random member");
            }
            
            // Test 27: popFromSet
            System.out.println("\n[Test 27] Pop from set");
            String result27 = popFromSet(jedis, "set1");
            totalTests++; if (result27 != null) {
                System.out.println("  ✓ PASS: Pop from set => " + result27);
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Pop from set");
            }
            
            // Test 28-30: Set operations (union, intersection, difference)
            System.out.println("\n[Test 28-30] Set operations");
            addToSet(jedis, "setA", "1", "2", "3");
            addToSet(jedis, "setB", "2", "3", "4");
            Set<String> union = setUnion(jedis, "setA", "setB");
            Set<String> inter = setIntersection(jedis, "setA", "setB");
            Set<String> diff = setDifference(jedis, "setA", "setB");
            totalTests++; if (union != null && union.size() == 4) {
                System.out.println("  ✓ PASS: Set union (size: 4)");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Set union");
            }
            totalTests++; if (inter != null && inter.size() == 2) {
                System.out.println("  ✓ PASS: Set intersection (size: 2)");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Set intersection");
            }
            totalTests++; if (diff != null && diff.size() == 1) {
                System.out.println("  ✓ PASS: Set difference (size: 1)");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Set difference");
            }
            
            // ==================== HASH TESTS ====================
            System.out.println("\n=== HASH OPERATIONS ===");
            
            // Test 31-32: setHashField & getHashField
            System.out.println("\n[Test 31-32] Hash set/get field");
            setHashField(jedis, "user:1000", "name", "John");
            String result32 = getHashField(jedis, "user:1000", "name");
            totalTests++; if (testString(result32, "John", "Hash set/get field")) passedTests++;
            
            // Test 33-34: setHashMultiple & getAllHash
            System.out.println("\n[Test 33-34] Hash multiple operations");
            Map<String, String> user = new HashMap<>();
            user.put("name", "Alice");
            user.put("age", "30");
            user.put("city", "NYC");
            setHashMultiple(jedis, "user:2000", user);
            Map<String, String> result34 = getAllHash(jedis, "user:2000");
            totalTests++; if (result34 != null && result34.size() == 3 && result34.get("name").equals("Alice")) {
                System.out.println("  ✓ PASS: Hash multiple operations");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Hash multiple operations");
            }
            
            // Test 35: hashFieldExists
            System.out.println("\n[Test 35] Hash field exists");
            boolean result35 = hashFieldExists(jedis, "user:2000", "age");
            totalTests++; if (testBoolean(result35, true, "Hash field exists")) passedTests++;
            
            // Test 36: deleteHashField
            System.out.println("\n[Test 36] Delete hash field");
            long result36 = deleteHashField(jedis, "user:2000", "age");
            totalTests++; if (testLong(result36, 1L, "Delete hash field")) passedTests++;
            
            // Test 37: getHashKeys
            System.out.println("\n[Test 37] Get hash keys");
            Set<String> result37 = getHashKeys(jedis, "user:2000");
            totalTests++; if (result37 != null && result37.size() == 2) {
                System.out.println("  ✓ PASS: Get hash keys (size: 2)");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Get hash keys");
            }
            
            // Test 38: getHashValues
            System.out.println("\n[Test 38] Get hash values");
            List<String> result38 = getHashValues(jedis, "user:2000");
            totalTests++; if (result38 != null && result38.size() == 2) {
                System.out.println("  ✓ PASS: Get hash values (size: 2)");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Get hash values");
            }
            
            // Test 39: getHashSize
            System.out.println("\n[Test 39] Hash size");
            long result39 = getHashSize(jedis, "user:2000");
            totalTests++; if (testLong(result39, 2L, "Hash size")) passedTests++;
            
            // Test 40: incrementHashField
            System.out.println("\n[Test 40] Increment hash field");
            setHashField(jedis, "stats", "views", "100");
            long result40 = incrementHashField(jedis, "stats", "views", 50);
            totalTests++; if (testLong(result40, 150L, "Increment hash field")) passedTests++;
            
            // ==================== SORTED SET TESTS ====================
            System.out.println("\n=== SORTED SET OPERATIONS ===");
            
            // Test 41-42: addToSortedSet & getSortedSetRange
            System.out.println("\n[Test 41-42] Sorted set add/range");
            addToSortedSet(jedis, "scores", 85.5, "Alice");
            addToSortedSet(jedis, "scores", 92.0, "Bob");
            addToSortedSet(jedis, "scores", 78.5, "Charlie");
            List<String> result42 = getSortedSetRange(jedis, "scores", 0, -1);
            totalTests++; if (result42 != null && result42.equals(Arrays.asList("Charlie", "Alice", "Bob"))) {
                System.out.println("  ✓ PASS: Sorted set range (ascending)");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Sorted set range");
            }
            
            // Test 43: getSortedSetRangeReverse
            System.out.println("\n[Test 43] Sorted set range (descending)");
            List<String> result43 = getSortedSetRangeReverse(jedis, "scores", 0, -1);
            totalTests++; if (result43 != null && result43.equals(Arrays.asList("Bob", "Alice", "Charlie"))) {
                System.out.println("  ✓ PASS: Sorted set range (descending)");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Sorted set range (descending)");
            }
            
            // Test 44: getScore
            System.out.println("\n[Test 44] Get score");
            Double result44 = getScore(jedis, "scores", "Bob");
            totalTests++; if (result44 != null && Math.abs(result44 - 92.0) < 0.01) {
                System.out.println("  ✓ PASS: Get score => 92.0");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Get score");
            }
            
            // Test 45: getRank
            System.out.println("\n[Test 45] Get rank");
            Long result45 = getRank(jedis, "scores", "Alice");
            totalTests++; if (testLongObject(result45, 1L, "Get rank (0-based)")) passedTests++;
            
            // Test 46: incrementScore
            System.out.println("\n[Test 46] Increment score");
            double result46 = incrementScore(jedis, "scores", 5.0, "Charlie");
            totalTests++; if (Math.abs(result46 - 83.5) < 0.01) {
                System.out.println("  ✓ PASS: Increment score => 83.5");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Increment score");
            }
            
            // Test 47: removeFromSortedSet
            System.out.println("\n[Test 47] Remove from sorted set");
            long result47 = removeFromSortedSet(jedis, "scores", "Bob");
            totalTests++; if (testLong(result47, 1L, "Remove from sorted set")) passedTests++;
            
            // Test 48: getSortedSetSize
            System.out.println("\n[Test 48] Sorted set size");
            long result48 = getSortedSetSize(jedis, "scores");
            totalTests++; if (testLong(result48, 2L, "Sorted set size")) passedTests++;
            
            // Test 49-50: countInScoreRange & getMembersByScoreRange
            System.out.println("\n[Test 49-50] Score range operations");
            long result49 = countInScoreRange(jedis, "scores", 80.0, 90.0);
            List<String> result50 = getMembersByScoreRange(jedis, "scores", 80.0, 90.0);
            totalTests++; if (testLong(result49, 2L, "Count in score range")) passedTests++;
            totalTests++; if (result50 != null && result50.size() == 2) {
                System.out.println("  ✓ PASS: Get members by score range (size: 2)");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Get members by score range");
            }
            
            // ==================== ADVANCED TESTS ====================
            System.out.println("\n=== ADVANCED OPERATIONS ===");
            
            // Test 51-52: setExpire & getTTL
            System.out.println("\n[Test 51-52] Expiration operations");
            setString(jedis, "expireTest", "value");
            boolean result51 = setExpire(jedis, "expireTest", 10);
            long result52 = getTTL(jedis, "expireTest");
            totalTests++; if (testBoolean(result51, true, "Set expire")) passedTests++;
            totalTests++; if (result52 > 0 && result52 <= 10) {
                System.out.println("  ✓ PASS: Get TTL => " + result52 + "s");
                passedTests++;
            } else {
                System.out.println("  ✗ FAIL: Get TTL");
            }
            
            // Test 53: setIfNotExists
            System.out.println("\n[Test 53] Set if not exists");
            boolean result53a = setIfNotExists(jedis, "unique", "first");
            boolean result53b = setIfNotExists(jedis, "unique", "second");
            totalTests++; if (testBoolean(result53a, true, "SETNX (first)") && testBoolean(result53b, false, "SETNX (exists)")) passedTests++;
            
            // Test 54: getAndSet
            System.out.println("\n[Test 54] Get and set");
            setString(jedis, "atomic", "old");
            String result54 = getAndSet(jedis, "atomic", "new");
            totalTests++; if (testString(result54, "old", "Get and set (returns old)")) passedTests++;
            
            // Test 55: decrementCounter
            System.out.println("\n[Test 55] Decrement counter");
            setString(jedis, "counter2", "10");
            long result55 = decrementCounter(jedis, "counter2");
            totalTests++; if (testLong(result55, 9L, "Decrement counter")) passedTests++;
            
            // Clean up after tests
            jedis.flushDB();
            
        } catch (Exception e) {
            System.out.println("\n✗ ERROR: " + e.getMessage());
            System.out.println("\nMake sure Redis is running:");
            System.out.println("  1. Install: brew install redis");
            System.out.println("  2. Start: redis-server");
            System.out.println("  3. Or run: redis-server &");
            return;
        }
        
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
            System.out.println("You have successfully mastered Redis operations!");
        } else {
            System.out.println("\n⚠️  Keep practicing! Review the failed tests and try again.");
        }
    }
    
    // ==================== TEST HELPER METHODS ====================
    
    private static boolean testString(String actual, String expected, String testName) {
        if ((actual == null && expected == null) || (actual != null && actual.equals(expected))) {
            System.out.println("  ✓ PASS: " + testName + " => \"" + actual + "\"");
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: \"" + expected + "\", Actual: \"" + actual + "\"");
            return false;
        }
    }
    
    private static boolean testLong(long actual, long expected, String testName) {
        if (actual == expected) {
            System.out.println("  ✓ PASS: " + testName + " => " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: " + expected + ", Actual: " + actual);
            return false;
        }
    }
    
    private static boolean testLongObject(Long actual, Long expected, String testName) {
        if ((actual == null && expected == null) || (actual != null && actual.equals(expected))) {
            System.out.println("  ✓ PASS: " + testName + " => " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: " + expected + ", Actual: " + actual);
            return false;
        }
    }
    
    private static boolean testBoolean(boolean actual, boolean expected, String testName) {
        if (actual == expected) {
            System.out.println("  ✓ PASS: " + testName + " => " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: " + expected + ", Actual: " + actual);
            return false;
        }
    }
}
