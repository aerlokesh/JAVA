import java.util.*;

// ===== CUSTOM EXCEPTION CLASSES =====

/**
 * Exception thrown when key is null (if null keys not supported)
 * WHEN TO THROW:
 * - Null key passed to put/get/remove
 */
class NullKeyException extends Exception {
    public NullKeyException() {
        super("Null keys are not supported");
    }
}

// ===== ENTRY NODE (LINKED LIST FOR CHAINING) =====

/**
 * Represents a key-value pair in a hash bucket
 * Forms a singly linked list for separate chaining
 * 
 * INTERVIEW DISCUSSION:
 * - Why linked list? (Simple collision resolution - separate chaining)
 * - Alternatives: Open addressing (linear probing, quadratic probing, double hashing)
 * - Java 8+: Treeify bucket when > 8 entries (O(log n) worst case)
 */
class Entry<K, V> {
    K key;
    V value;
    int hashCode;
    Entry<K, V> next;  // Next entry in the chain (collision resolution)
    
    public Entry(K key, V value, int hashCode) {
        this.key = key;
        this.value = value;
        this.hashCode = hashCode;
        this.next = null;
    }
    
    @Override
    public String toString() {
        return key + "=" + value;
    }
}

// ===== MAIN HASHMAP CLASS =====

/**
 * Custom HashMap - Low Level Design (LLD)
 * 
 * PROBLEM STATEMENT:
 * Design a HashMap from scratch that can:
 * 1. Store key-value pairs with O(1) average get/put/remove
 * 2. Handle hash collisions using separate chaining
 * 3. Automatically resize when load factor exceeded
 * 4. Support iteration over entries
 * 
 * CORE CONCEPTS:
 *   Array of Buckets:  [bucket0] -> Entry -> Entry -> null
 *                      [bucket1] -> Entry -> null
 *                      [bucket2] -> null
 *                      [bucket3] -> Entry -> Entry -> Entry -> null
 * 
 *   Hash Function: key.hashCode() → spread bits → mod bucketCount → bucket index
 *   Collision Resolution: Separate Chaining (linked list per bucket)
 *   Resize: When size/capacity > loadFactor, double capacity and rehash
 * 
 * REQUIREMENTS:
 * - put(key, value): Insert/update in O(1) avg
 * - get(key): Retrieve in O(1) avg
 * - remove(key): Delete in O(1) avg
 * - Automatic resize at load factor threshold
 * 
 * INTERVIEW HINTS:
 * - Discuss hash function design (uniform distribution, minimize collisions)
 * - Talk about collision resolution strategies
 * - Mention load factor and rehashing
 * - Discuss Java's HashMap internals (treeification at 8)
 * - Consider thread safety (ConcurrentHashMap)
 */
class MyHashMap<K, V> {
    private static final int DEFAULT_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    
    private Entry<K, V>[] buckets;   // Array of bucket heads
    private int size;                 // Number of key-value pairs
    private int capacity;             // Number of buckets
    private float loadFactor;         // Threshold for resize
    private int resizeCount;          // Number of resizes (for stats)
    
    /**
     * Constructor with default capacity and load factor
     */
    @SuppressWarnings("unchecked")
    public MyHashMap() {
        this.capacity = DEFAULT_CAPACITY;
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        this.buckets = new Entry[capacity];
        this.size = 0;
        this.resizeCount = 0;
    }
    
    /**
     * Constructor with custom capacity
     */
    @SuppressWarnings("unchecked")
    public MyHashMap(int initialCapacity) {
        this.capacity = initialCapacity;
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        this.buckets = new Entry[capacity];
        this.size = 0;
        this.resizeCount = 0;
    }
    
    /**
     * Compute hash for a key
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get key's hashCode()
     * 2. Spread bits to reduce clustering: hash ^ (hash >>> 16)
     * 3. This is what Java's HashMap does internally
     * 
     * WHY SPREAD BITS?
     * - hashCode() may have patterns (e.g., sequential integers)
     * - XOR with upper bits mixes high/low bits
     * - Reduces collisions when capacity is power of 2
     * 
     * @param key The key to hash
     * @return Spread hash value
     */
    private int hash(K key) {
        // HINT: if (key == null) return 0;
        // HINT: int h = key.hashCode();
        // HINT: return h ^ (h >>> 16);  // Spread bits
        if(key==null) return 0;
        int h=key.hashCode();
        return h ^ (h >>> 16);
    }
    
    /**
     * Get bucket index from hash
     * 
     * IMPLEMENTATION HINTS:
     * 1. Use bitwise AND instead of modulo: hash & (capacity - 1)
     * 2. This works because capacity is always power of 2
     * 3. Faster than % operator
     * 
     * @param hash The hash value
     * @return Bucket index (0 to capacity-1)
     */
    private int getBucketIndex(int hash) {
        // HINT: return hash & (capacity - 1);
        return hash & (capacity-1);
    }
    
    /**
     * Put a key-value pair into the map
     * 
     * IMPLEMENTATION HINTS:
     * 1. Compute hash and bucket index
     * 2. Walk the chain in that bucket
     * 3. If key exists (equals check): update value, return old value
     * 4. If key not found: create new Entry, add at head of chain
     * 5. Increment size
     * 6. Check if resize needed: size > capacity * loadFactor
     * 7. If resize needed, call resize()
     * 
     * TIME COMPLEXITY: O(1) average, O(n) worst case (all keys in one bucket)
     * 
     * @param key Key
     * @param value Value
     * @return Previous value for key, or null if new
     */
    public V put(K key, V value) {
        // HINT: int hash = hash(key);
        // HINT: int index = getBucketIndex(hash);
        // HINT: Entry<K, V> current = buckets[index];
        // HINT: while (current != null) {
        //     if (current.hashCode == hash && 
        //         (current.key == key || (key != null && key.equals(current.key)))) {
        //         V oldValue = current.value;
        //         current.value = value;
        //         return oldValue;
        //     }
        //     current = current.next;
        // }
        // HINT: Entry<K, V> newEntry = new Entry<>(key, value, hash);
        // HINT: newEntry.next = buckets[index];  // Add at head
        // HINT: buckets[index] = newEntry;
        // HINT: size++;
        // HINT: if (size > capacity * loadFactor) resize();
        // HINT: return null;
        int hash = hash(key);
        int idx=getBucketIndex(hash);
        Entry<K,V> current = buckets[idx];
        while(current!=null){
            if(current.hashCode==hash && (current.key==key || (key!=null && key.equals(current.key)))){
                V oldValue = current.value;
                current.value=value;
                return oldValue;
            }
            current=current.next;
        }
        Entry<K, V> newEntry = new Entry<>(key, value, hash);
        newEntry.next = buckets[idx]; 
        buckets[idx] = newEntry;
        size++;
        if(size>capacity*loadFactor) resize();
        return null;
    }
    
    /**
     * Get value by key
     * 
     * IMPLEMENTATION HINTS:
     * 1. Compute hash and bucket index
     * 2. Walk the chain in that bucket
     * 3. Compare hash first (fast), then equals (thorough)
     * 4. Return value if found, null if not
     * 
     * WHY CHECK HASH FIRST?
     * - hashCode comparison is O(1)
     * - equals() can be expensive (String comparison)
     * - Short-circuit: different hash → definitely different key
     * 
     * TIME COMPLEXITY: O(1) average
     * 
     * @param key Key to look up
     * @return Value or null if not found
     */
    public V get(K key) {
        // HINT: int hash = hash(key);
        // HINT: int index = getBucketIndex(hash);
        // HINT: Entry<K, V> current = buckets[index];
        // HINT: while (current != null) {
        //     if (current.hashCode == hash && 
        //         (current.key == key || (key != null && key.equals(current.key)))) {
        //         return current.value;
        //     }
        //     current = current.next;
        // }
        // HINT: return null;
        int hash=hash(key);
        int idx=getBucketIndex(hash);
        Entry<K,V> current=buckets[idx];
        while(current!=null){
            if(current.hashCode==hash && (current.key==key || key.equals(current.key))){
                return current.value;
            }
            current=current.next;
        }
        return null;
    }
    
    /**
     * Remove a key-value pair
     * 
     * IMPLEMENTATION HINTS:
     * 1. Compute hash and bucket index
     * 2. Walk the chain, keeping track of previous node
     * 3. If found at head: update bucket head to next
     * 4. If found in middle/end: prev.next = current.next
     * 5. Decrement size
     * 6. Return removed value
     * 
     * TIME COMPLEXITY: O(1) average
     * 
     * @param key Key to remove
     * @return Removed value, or null if not found
     */
    public V remove(K key) {
        // HINT: int hash = hash(key);
        // HINT: int index = getBucketIndex(hash);
        // HINT: Entry<K, V> current = buckets[index];
        // HINT: Entry<K, V> prev = null;
        // HINT: while (current != null) {
        //     if (current.hashCode == hash && 
        //         (current.key == key || (key != null && key.equals(current.key)))) {
        //         if (prev == null) buckets[index] = current.next;
        //         else prev.next = current.next;
        //         size--;
        //         return current.value;
        //     }
        //     prev = current;
        //     current = current.next;
        // }
        // HINT: return null;
        int hash=hash(key);
        int idx=getBucketIndex(hash);
        Entry<K,V> current=buckets[idx];
        Entry<K,V> prev=null;
        while(current!=null){
            if(current.hashCode==hash && (key!=null && key.equals(current.key))){
                if(prev==null) buckets[idx]=current.next;
                else prev.next=current.next;
                size--;
                return current.value;
            }
            prev=current;
            current=current.next;
        }
        return null;
    }
    
    /**
     * Check if key exists
     * 
     * @param key Key to check
     * @return true if key exists
     */
    public boolean containsKey(K key) {
        // TODO: Implement
        // HINT: return get(key) != null;
        // NOTE: Can't use get(key) != null because value might be null
        int hash = hash(key);
        int idx = getBucketIndex(hash);
        Entry<K, V> current = buckets[idx];
        while (current != null) {
            if (current.hashCode == hash && 
                (current.key == key || (key != null && key.equals(current.key)))) {
                return true;
            }
            current = current.next;
        }
        return false;
    }
    
    /**
     * Resize the hash map (double capacity and rehash all entries)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Double the capacity
     * 2. Create new bucket array with new capacity
     * 3. Rehash ALL existing entries into new buckets
     *    - For each bucket, walk the chain
     *    - Recompute bucket index with new capacity
     *    - Insert into new bucket array
     * 4. Replace old buckets with new buckets
     * 5. Increment resizeCount
     * 
     * WHY REHASH?
     * - Bucket index = hash & (capacity - 1)
     * - When capacity changes, same hash maps to different bucket
     * - Must reposition every entry
     * 
     * TIME COMPLEXITY: O(n) - must visit every entry
     * AMORTIZED: O(1) per put (resize happens infrequently)
     */
    @SuppressWarnings("unchecked")
    private void resize() {
        // HINT: int newCapacity = capacity * 2;
        // HINT: Entry<K, V>[] newBuckets = new Entry[newCapacity];
        // HINT: for (int i = 0; i < capacity; i++) {
        //     Entry<K, V> current = buckets[i];
        //     while (current != null) {
        //         Entry<K, V> next = current.next;
        //         int newIndex = current.hashCode & (newCapacity - 1);
        //         current.next = newBuckets[newIndex];
        //         newBuckets[newIndex] = current;
        //         current = next;
        //     }
        // }
        // HINT: buckets = newBuckets;
        // HINT: capacity = newCapacity;
        // HINT: resizeCount++;
        int newCapacity=capacity*2;
        Entry<K,V>[] newBuckets=new Entry[newCapacity];
        for(int i=0;i<capacity;i++){
            Entry<K,V> current=buckets[i];
            while(current!=null){
                Entry<K,V> next=current.next;
                int newIdx=current.hashCode & (newCapacity-1);
                current.next=newBuckets[newIdx];
                newBuckets[newIdx]=current;
                current=next;
            }
        }
        buckets=newBuckets;
        capacity=newCapacity;
        resizeCount++;
    }


    
    /**
     * Get all keys
     * 
     * IMPLEMENTATION HINTS:
     * 1. Iterate all buckets
     * 2. Walk each chain
     * 3. Collect all keys
     * 
     * @return Set of all keys
     */
    public Set<K> keySet() {
        // HINT: Set<K> keys = new HashSet<>();
        // HINT: for (int i = 0; i < capacity; i++) {
        //     Entry<K, V> current = buckets[i];
        //     while (current != null) {
        //         keys.add(current.key);
        //         current = current.next;
        //     }
        // }
        // HINT: return keys;
        Set<K> keys=new HashSet<>();
        for (int i = 0; i < capacity; i++) {
            Entry<K, V> current = buckets[i];
            while (current != null) {
                keys.add(current.key);
                current=current.next;
            }
        }
        return keys;
    }
    
    /**
     * Get all values
     * 
     * @return List of all values
     */
    public List<V> values() {
        // HINT: Similar to keySet but collect values
        List<V> values=new ArrayList<>();
        for (int i = 0; i < capacity; i++) {
            Entry<K, V> current = buckets[i];
            while (current != null) {
                values.add(current.value);
                current=current.next;
            }
        }
        return values;
    }
    
    // ===== UTILITY METHODS =====
    
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int getCapacity() { return capacity; }
    public int getResizeCount() { return resizeCount; }
    
    /**
     * Get bucket distribution (for analysis)
     */
    public String getBucketStats() {
        int emptyBuckets = 0;
        int maxChainLen = 0;
        int totalChainLen = 0;
        
        for (int i = 0; i < capacity; i++) {
            int chainLen = 0;
            Entry<K, V> current = buckets[i];
            while (current != null) {
                chainLen++;
                current = current.next;
            }
            if (chainLen == 0) emptyBuckets++;
            maxChainLen = Math.max(maxChainLen, chainLen);
            totalChainLen += chainLen;
        }
        
        double avgChainLen = capacity > 0 ? (double) totalChainLen / (capacity - emptyBuckets + 1) : 0;
        return String.format("Capacity: %d, Size: %d, Load: %.2f, Empty buckets: %d/%d, " +
                "Max chain: %d, Avg chain: %.2f, Resizes: %d",
                capacity, size, (double) size / capacity, emptyBuckets, capacity,
                maxChainLen, avgChainLen, resizeCount);
    }
}

// ===== MAIN TEST CLASS =====

public class CustomHashMap {
    public static void main(String[] args) {
        System.out.println("=== Custom HashMap Test Cases ===\n");
        
        // Test Case 1: Basic Put and Get
        System.out.println("=== Test Case 1: Basic Put and Get ===");
        MyHashMap<String, Integer> map = new MyHashMap<>();
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        
        System.out.println("get(apple): " + map.get("apple") + " (expected 1)");
        System.out.println("get(banana): " + map.get("banana") + " (expected 2)");
        System.out.println("get(cherry): " + map.get("cherry") + " (expected 3)");
        System.out.println("size: " + map.size() + " (expected 3)");
        System.out.println("✓ Basic put/get working");
        System.out.println();
        
        // Test Case 2: Update Existing Key
        System.out.println("=== Test Case 2: Update Existing Key ===");
        Integer oldVal = map.put("apple", 100);
        System.out.println("Old value: " + oldVal + " (expected 1)");
        System.out.println("New get(apple): " + map.get("apple") + " (expected 100)");
        System.out.println("size: " + map.size() + " (expected 3 - no duplicates)");
        System.out.println("✓ Update working");
        System.out.println();
        
        // Test Case 3: Get Non-existent Key
        System.out.println("=== Test Case 3: Get Non-existent Key ===");
        System.out.println("get(nonexistent): " + map.get("nonexistent") + " (expected null)");
        System.out.println("✓ Miss returns null");
        System.out.println();
        
        // Test Case 4: Remove
        System.out.println("=== Test Case 4: Remove ===");
        Integer removed = map.remove("banana");
        System.out.println("remove(banana): " + removed + " (expected 2)");
        System.out.println("get(banana): " + map.get("banana") + " (expected null)");
        System.out.println("size: " + map.size() + " (expected 2)");
        
        Integer removeMissing = map.remove("nonexistent");
        System.out.println("remove(nonexistent): " + removeMissing + " (expected null)");
        System.out.println("✓ Remove working");
        System.out.println();
        
        // Test Case 5: Contains Key
        System.out.println("=== Test Case 5: Contains Key ===");
        System.out.println("containsKey(apple): " + map.containsKey("apple") + " (expected true)");
        System.out.println("containsKey(banana): " + map.containsKey("banana") + " (expected false)");
        System.out.println("✓ ContainsKey working");
        System.out.println();
        
        // Test Case 6: Auto Resize
        System.out.println("=== Test Case 6: Auto Resize ===");
        MyHashMap<Integer, String> resizeMap = new MyHashMap<>(4); // Small initial capacity
        System.out.println("Initial capacity: " + resizeMap.getCapacity());
        
        for (int i = 0; i < 20; i++) {
            resizeMap.put(i, "val-" + i);
        }
        
        System.out.println("After 20 puts:");
        System.out.println("  Size: " + resizeMap.size() + " (expected 20)");
        System.out.println("  Capacity: " + resizeMap.getCapacity() + " (expected > 4)");
        System.out.println("  Resizes: " + resizeMap.getResizeCount());
        
        // Verify all values still accessible after resize
        boolean allPresent = true;
        for (int i = 0; i < 20; i++) {
            if (!"val-" .equals(resizeMap.get(i) != null ? resizeMap.get(i).substring(0, 4) : "")) {
                allPresent = false;
                break;
            }
        }
        System.out.println("  All values preserved: " + allPresent);
        System.out.println("✓ Auto resize working");
        System.out.println();
        
        // Test Case 7: Hash Collision Handling
        System.out.println("=== Test Case 7: Hash Collision Handling ===");
        MyHashMap<CollisionKey, String> collisionMap = new MyHashMap<>(4);
        // These keys will all hash to the same bucket
        collisionMap.put(new CollisionKey("a", 1), "value-a");
        collisionMap.put(new CollisionKey("b", 1), "value-b");
        collisionMap.put(new CollisionKey("c", 1), "value-c");
        
        System.out.println("get(a): " + collisionMap.get(new CollisionKey("a", 1)) + " (expected value-a)");
        System.out.println("get(b): " + collisionMap.get(new CollisionKey("b", 1)) + " (expected value-b)");
        System.out.println("get(c): " + collisionMap.get(new CollisionKey("c", 1)) + " (expected value-c)");
        System.out.println("size: " + collisionMap.size() + " (expected 3)");
        System.out.println("✓ Collision handling working");
        System.out.println();
        
        // Test Case 8: Integer Keys
        System.out.println("=== Test Case 8: Integer Keys ===");
        MyHashMap<Integer, Integer> intMap = new MyHashMap<>();
        for (int i = 0; i < 100; i++) {
            intMap.put(i, i * i);
        }
        System.out.println("size: " + intMap.size() + " (expected 100)");
        System.out.println("get(0): " + intMap.get(0) + " (expected 0)");
        System.out.println("get(10): " + intMap.get(10) + " (expected 100)");
        System.out.println("get(99): " + intMap.get(99) + " (expected 9801)");
        System.out.println("✓ Integer keys working");
        System.out.println();
        
        // Test Case 9: KeySet and Values
        System.out.println("=== Test Case 9: KeySet and Values ===");
        MyHashMap<String, Integer> kvMap = new MyHashMap<>();
        kvMap.put("x", 10);
        kvMap.put("y", 20);
        kvMap.put("z", 30);
        System.out.println("keys: " + kvMap.keySet());
        System.out.println("values: " + kvMap.values());
        System.out.println("✓ KeySet and Values working");
        System.out.println();
        
        // Test Case 10: Null Value
        System.out.println("=== Test Case 10: Null Value ===");
        MyHashMap<String, String> nullMap = new MyHashMap<>();
        nullMap.put("key", null);
        System.out.println("get(key): " + nullMap.get("key") + " (expected null)");
        System.out.println("containsKey(key): " + nullMap.containsKey("key"));
        System.out.println("size: " + nullMap.size() + " (expected 1)");
        System.out.println();
        
        // Test Case 11: Bucket Statistics
        System.out.println("=== Test Case 11: Bucket Statistics ===");
        MyHashMap<String, Integer> statsMap = new MyHashMap<>(16);
        for (int i = 0; i < 50; i++) {
            statsMap.put("key" + i, i);
        }
        System.out.println(statsMap.getBucketStats());
        System.out.println("✓ Stats available");
        System.out.println();
        
        // Test Case 12: Stress Test
        System.out.println("=== Test Case 12: Stress Test (10000 entries) ===");
        MyHashMap<Integer, Integer> stressMap = new MyHashMap<>();
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10000; i++) {
            stressMap.put(i, i);
        }
        
        long putTime = System.currentTimeMillis() - startTime;
        startTime = System.currentTimeMillis();
        
        int found = 0;
        for (int i = 0; i < 10000; i++) {
            if (stressMap.get(i) != null) found++;
        }
        
        long getTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Put 10000: " + putTime + "ms");
        System.out.println("Get 10000: " + getTime + "ms");
        System.out.println("Found: " + found + "/10000");
        System.out.println(stressMap.getBucketStats());
        System.out.println("✓ Stress test complete");
        System.out.println();
        
        // Test Case 13: Empty Map
        System.out.println("=== Test Case 13: Empty Map ===");
        MyHashMap<String, String> emptyMap = new MyHashMap<>();
        System.out.println("isEmpty: " + emptyMap.isEmpty() + " (expected true)");
        System.out.println("size: " + emptyMap.size() + " (expected 0)");
        System.out.println("get(any): " + emptyMap.get("any") + " (expected null)");
        System.out.println("remove(any): " + emptyMap.remove("any") + " (expected null)");
        System.out.println("✓ Empty map handling");
        System.out.println();
        
        System.out.println("=== All Test Cases Complete! ===");
    }
}

/**
 * Helper class for collision testing - all instances hash to same value
 */
class CollisionKey {
    String name;
    int forcedHash;
    
    public CollisionKey(String name, int forcedHash) {
        this.name = name;
        this.forcedHash = forcedHash;
    }
    
    @Override
    public int hashCode() { return forcedHash; }  // Force same hash
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CollisionKey)) return false;
        return name.equals(((CollisionKey) o).name);
    }
}

/**
 * INTERVIEW DISCUSSION TOPICS:
 * ============================
 * 
 * 1. HASH FUNCTION:
 *    Requirements:
 *      - Deterministic: same key → same hash
 *      - Uniform distribution: minimize clustering
 *      - Fast to compute
 *    
 *    Java's Approach:
 *      h = key.hashCode()
 *      h ^ (h >>> 16)  // Mix high and low bits
 *      index = h & (capacity - 1)  // Fast modulo for power-of-2
 *    
 *    Why XOR with upper bits?
 *      - Small tables only use lower bits
 *      - Upper bits carry information too
 *      - XOR mixes them for better distribution
 * 
 * 2. COLLISION RESOLUTION:
 *    Separate Chaining (used here):
 *      - Each bucket is a linked list
 *      - Pros: Simple, handles many collisions
 *      - Cons: Extra memory for pointers, cache unfriendly
 *    
 *    Open Addressing:
 *      - Linear Probing: check next slot
 *      - Quadratic Probing: check i² slots away
 *      - Double Hashing: second hash for step size
 *      - Pros: Cache friendly, no extra pointers
 *      - Cons: Clustering, deletion complex (tombstones)
 *    
 *    Java 8 Treeification:
 *      - When bucket has > 8 entries, convert to Red-Black tree
 *      - O(log n) worst case instead of O(n)
 *      - Convert back to list when < 6 entries
 * 
 * 3. LOAD FACTOR & RESIZING:
 *    Load Factor = size / capacity
 *      - Default: 0.75 (good balance of time/space)
 *      - Lower: fewer collisions, more memory
 *      - Higher: more collisions, less memory
 *    
 *    Resize Strategy:
 *      - Double capacity when load > threshold
 *      - Rehash ALL entries (O(n) operation)
 *      - Amortized O(1) per put
 *    
 *    Why Power of 2?
 *      - hash & (capacity - 1) is faster than hash % capacity
 *      - Bit manipulation vs integer division
 * 
 * 4. EQUALS AND HASHCODE CONTRACT:
 *    Rules:
 *      - a.equals(b) → a.hashCode() == b.hashCode()
 *      - Same hash ≠ equals (collisions happen)
 *      - Must override BOTH or NEITHER
 *    
 *    Common Mistakes:
 *      - Override equals but not hashCode
 *      - Mutable fields in hashCode (key changes bucket!)
 *      - Using identity (==) instead of equals
 * 
 * 5. THREAD SAFETY:
 *    HashMap: NOT thread-safe
 *    Collections.synchronizedMap(): Coarse-grained locking
 *    ConcurrentHashMap: Segment-level locking (Java 7) / CAS + synchronized (Java 8)
 *    
 *    ConcurrentHashMap internals:
 *      - Lock-free reads
 *      - CAS for simple updates
 *      - Synchronized for complex operations
 *      - No null keys or values (ambiguity with concurrent access)
 * 
 * 6. TIME COMPLEXITY:
 *    Operation | Average | Worst (no treeify) | Worst (treeified)
 *    get       | O(1)    | O(n)               | O(log n)
 *    put       | O(1)*   | O(n)               | O(log n)
 *    remove    | O(1)    | O(n)               | O(log n)
 *    resize    | O(n)    | O(n)               | O(n)
 *    * amortized
 * 
 * 7. SPACE COMPLEXITY:
 *    - O(n) for n entries
 *    - Extra: bucket array (capacity) + Entry objects
 *    - Each Entry: key + value + hash + next pointer
 * 
 * 8. ALTERNATIVE DATA STRUCTURES:
 *    TreeMap: O(log n) all ops, sorted keys
 *    LinkedHashMap: HashMap + insertion/access order
 *    EnumMap: Specialized for enum keys, array-backed
 *    IdentityHashMap: Uses == instead of equals
 * 
 * 9. LEETCODE REFERENCES:
 *    LeetCode 706 - Design HashMap
 *    LeetCode 705 - Design HashSet
 *    - Both test fundamental understanding
 * 
 * 10. API DESIGN:
 *     map.put(key, value)     - Insert/update
 *     map.get(key)            - Retrieve
 *     map.remove(key)         - Delete
 *     map.containsKey(key)    - Check existence
 *     map.size()              - Entry count
 *     map.isEmpty()           - Empty check
 *     map.keySet()            - All keys
 *     map.values()            - All values
 */
