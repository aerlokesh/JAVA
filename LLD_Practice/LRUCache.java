import java.util.*;
import java.util.concurrent.locks.*;

// ===== CUSTOM EXCEPTION CLASSES =====

/**
 * Exception thrown when cache key is invalid
 * WHEN TO THROW:
 * - Null key provided
 */
class InvalidKeyException extends Exception {
    public InvalidKeyException(String message) {
        super(message);
    }
}

/**
 * Exception thrown when cache capacity is invalid
 * WHEN TO THROW:
 * - Capacity <= 0
 */
class InvalidCapacityException extends Exception {
    private int capacity;
    
    public InvalidCapacityException(int capacity) {
        super("Invalid capacity: " + capacity + ". Must be > 0");
        this.capacity = capacity;
    }
    
    public int getCapacity() { return capacity; }
}

/**
 * Exception thrown when key not found in cache
 * WHEN TO THROW:
 * - get() called with non-existent key (optional - can return null instead)
 */
class CacheKeyNotFoundException extends Exception {
    private String key;
    
    public CacheKeyNotFoundException(String key) {
        super("Key not found in cache: " + key);
        this.key = key;
    }
    
    public String getKey() { return key; }
}

// ===== DOUBLY LINKED LIST NODE =====

/**
 * Node in the doubly linked list
 * 
 * INTERVIEW DISCUSSION:
 * - Why doubly linked list? (O(1) removal from middle - need prev pointer)
 * - Why not singly linked? (Removal requires O(n) to find previous node)
 * - Why not ArrayList? (Remove from middle is O(n) due to shifting)
 */
class DLLNode<K, V> {
    K key;
    V value;
    DLLNode<K, V> prev;
    DLLNode<K, V> next;
    long lastAccessTime;
    
    public DLLNode(K key, V value) {
        this.key = key;
        this.value = value;
        this.lastAccessTime = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return key + "=" + value;
    }
}

// ===== EVICTION STRATEGY INTERFACE =====

/**
 * Strategy interface for cache eviction policies
 * 
 * INTERVIEW DISCUSSION:
 * - Why Strategy pattern? (Swap LRU/LFU/FIFO without changing cache logic)
 * - Common policies: LRU, LFU, FIFO, MRU, Random
 */
interface EvictionPolicy<K, V> {
    void onAccess(DLLNode<K, V> node);
    void onInsert(DLLNode<K, V> node);
    DLLNode<K, V> evict();
    void onRemove(DLLNode<K, V> node);
    String getName();
}

// ===== LRU EVICTION POLICY =====

/**
 * LRU (Least Recently Used) eviction policy using doubly linked list
 * 
 * DATA STRUCTURE:
 *   Head <-> Node1 <-> Node2 <-> ... <-> NodeN <-> Tail
 *   (MRU)                                          (LRU)
 * 
 * ALGORITHM:
 *   - On access/insert: Move node to head (most recently used)
 *   - On evict: Remove from tail (least recently used)
 * 
 * TIME COMPLEXITY: O(1) for all operations
 * SPACE COMPLEXITY: O(n) for n cached items
 */
class LRUEvictionPolicy<K, V> implements EvictionPolicy<K, V> {
    DLLNode<K, V> head;  // Sentinel head (MRU end)
    DLLNode<K, V> tail;  // Sentinel tail (LRU end)
    
    public LRUEvictionPolicy() {
        head = new DLLNode<>(null, null);
        tail = new DLLNode<>(null, null);
        head.next = tail;
        tail.prev = head;
    }
    
    /**
     * Move node to head (mark as most recently used)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Remove node from current position
     * 2. Insert right after head sentinel
     * 
     * @param node Node that was accessed
     */
    @Override
    public void onAccess(DLLNode<K, V> node) {
        // HINT: removeNode(node);
        // HINT: addToHead(node);
        removeNode(node);
        addToHead(node);
    }

    
    /**
     * Insert new node at head (most recently used)
     * 
     * @param node New node to insert
     */
    @Override
    public void onInsert(DLLNode<K, V> node) {
        // HINT: addToHead(node);
        addToHead(node);
    }
    
    /**
     * Evict least recently used (remove from tail)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get the node just before tail sentinel
     * 2. Remove it from the list
     * 3. Return the removed node (so cache can remove from HashMap)
     * 
     * @return The evicted node (LRU)
     */
    @Override
    public DLLNode<K, V> evict() {  
        // HINT: if (tail.prev == head) return null; // Empty list
        // HINT: DLLNode<K, V> lru = tail.prev;
        // HINT: removeNode(lru);
        // HINT: return lru;
        if(tail.prev == head) return null;
        DLLNode<K,V> lru=tail.prev;
        removeNode(lru);
        return lru;
    }
    
    /**
     * Remove a specific node from the list
     * 
     * @param node Node to remove
     */
    @Override
    public void onRemove(DLLNode<K, V> node) {
        // HINT: removeNode(node);
        removeNode(node);
    }
    
    /**
     * Helper: Remove node from its current position in the DLL
     * 
     * IMPLEMENTATION HINTS:
     * 1. Link node's prev to node's next
     * 2. Link node's next to node's prev
     * 3. This "unlinks" the node from the chain
     * 
     * TIME COMPLEXITY: O(1)
     * 
     * @param node Node to remove
     */
    private void removeNode(DLLNode<K, V> node) {
        // HINT: node.prev.next = node.next;
        // HINT: node.next.prev = node.prev;
        node.prev.next=node.next;
        node.next.prev=node.prev;
    }
    
    /**
     * Helper: Add node right after head sentinel (MRU position)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Set node's next to head's current next
     * 2. Set node's prev to head
     * 3. Update head.next.prev to node
     * 4. Update head.next to node
     * 
     * TIME COMPLEXITY: O(1)
     * 
     * @param node Node to add at head
     */
    private void addToHead(DLLNode<K, V> node) {
        // HINT: node.next = head.next;
        // HINT: node.prev = head;
        // HINT: head.next.prev = node;
        // HINT: head.next = node;
        node.next=head.next;
        node.prev=head;
        head.next.prev=node;
        head.next=node;
    }
    
    @Override
    public String getName() { return "LRU"; }
    
    /**
     * Get ordered list from MRU to LRU (for display)
     */
    public List<String> getOrder() {
        List<String> order = new ArrayList<>();
        DLLNode<K, V> curr = head.next;
        while (curr != tail) {
            order.add(curr.toString());
            curr = curr.next;
        }
        return order;
    }
}

// ===== MAIN CACHE CLASS =====

/**
 * LRU Cache - Low Level Design (LLD)
 * 
 * PROBLEM STATEMENT:
 * Design a cache with LRU eviction policy that can:
 * 1. Store key-value pairs with O(1) get and put
 * 2. Evict least recently used item when capacity is reached
 * 3. Support thread-safe operations
 * 4. Track cache statistics (hits, misses, evictions)
 * 
 * CORE DATA STRUCTURES:
 *   HashMap<K, DLLNode<K,V>> → O(1) lookup by key
 *   DoublyLinkedList          → O(1) reorder on access, O(1) eviction
 * 
 * REQUIREMENTS:
 * - get(key): Return value, move to MRU. O(1)
 * - put(key, value): Insert/update, evict LRU if full. O(1)
 * - delete(key): Remove entry. O(1)
 * 
 * INTERVIEW HINTS:
 * - Why HashMap + DLL? (HashMap for O(1) lookup, DLL for O(1) reorder)
 * - Why not LinkedHashMap? (Works but doesn't show understanding)
 * - Why not TreeMap? (O(log n) operations, overkill)
 * - Discuss thread safety (ReadWriteLock, ConcurrentHashMap)
 * - Mention Redis as real-world LRU cache
 */
class Cache<K, V> {
    private int capacity;
    private Map<K, DLLNode<K, V>> map;
    private EvictionPolicy<K, V> evictionPolicy;
    private ReadWriteLock lock;
    
    // Statistics
    private int hits;
    private int misses;
    private int evictions;
    private int totalPuts;
    
    /**
     * Constructor
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate capacity > 0
     * 2. Initialize HashMap
     * 3. Initialize eviction policy
     * 4. Initialize ReadWriteLock for thread safety
     * 5. Initialize statistics counters
     * 
     * @param capacity Maximum number of entries
     * @param evictionPolicy Eviction strategy to use
     * @throws InvalidCapacityException if capacity <= 0
     */
    public Cache(int capacity, EvictionPolicy<K, V> evictionPolicy) throws InvalidCapacityException {
        // HINT: if (capacity <= 0) throw new InvalidCapacityException(capacity);
        // HINT: this.capacity = capacity;
        // HINT: this.map = new HashMap<>();
        // HINT: this.evictionPolicy = evictionPolicy;
        // HINT: this.lock = new ReentrantReadWriteLock();
        // HINT: this.hits = this.misses = this.evictions = this.totalPuts = 0;
        if(capacity<=0) throw new InvalidCapacityException(capacity);
        this.capacity = capacity;
        this.map = new HashMap<>();
        this.evictionPolicy=evictionPolicy;
        this.lock=new ReentrantReadWriteLock();
        this.hits=this.misses=this.evictions=this.totalPuts=0;
    }
    
    /**
     * Get value by key
     * 
     * IMPLEMENTATION HINTS:
     * 1. Acquire read lock (or write lock if updating access order)
     * 2. Check if key exists in map
     * 3. If miss: increment misses, return null
     * 4. If hit: increment hits, update access order (move to MRU), return value
     * 5. Release lock in finally block
     * 
     * TIME COMPLEXITY: O(1)
     * 
     * @param key Cache key
     * @return Value or null if not found
     * @throws InvalidKeyException if key is null
     */
    public V get(K key) throws InvalidKeyException {
        // HINT: if (key == null) throw new InvalidKeyException("Key cannot be null");
        // HINT: DLLNode<K, V> node = map.get(key);
        // HINT: if (node == null) { misses++; return null; }
        // HINT: hits++;
        // HINT: evictionPolicy.onAccess(node);
        // HINT: return node.value;
        if(key==null) throw new InvalidKeyException("Key cannot be null");
        
        lock.writeLock().lock();  // Write lock because we modify DLL structure
        try {
            DLLNode<K,V> node=map.get(key);
            if(node==null){misses++; return null;}
            hits++;
            evictionPolicy.onAccess(node);
            return node.value;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Put key-value pair into cache
     * 
     * IMPLEMENTATION HINTS:
     * 1. Acquire write lock
     * 2. If key already exists: update value, move to MRU
     * 3. If key is new:
     *    a. If at capacity: evict LRU entry
     *    b. Create new node
     *    c. Add to map and DLL
     * 4. Increment totalPuts
     * 5. Release lock in finally block
     * 
     * TIME COMPLEXITY: O(1)
     * 
     * @param key Cache key
     * @param value Cache value
     * @throws InvalidKeyException if key is null
     */
    public void put(K key, V value) throws InvalidKeyException {
        // HINT: if (key == null) throw new InvalidKeyException("Key cannot be null");
        // HINT: if (map.containsKey(key)) {
        //     DLLNode<K, V> node = map.get(key);
        //     node.value = value;
        //     evictionPolicy.onAccess(node);
        // } else {
        //     if (map.size() >= capacity) {
        //         DLLNode<K, V> evicted = evictionPolicy.evict();
        //         if (evicted != null) { map.remove(evicted.key); evictions++; }
        //     }
        //     DLLNode<K, V> newNode = new DLLNode<>(key, value);
        //     map.put(key, newNode);
        //     evictionPolicy.onInsert(newNode);
        // }
        // HINT: totalPuts++;
        if(key==null) throw new InvalidKeyException("Key cannot be null");
        
        lock.writeLock().lock();
        try {
            if(map.containsKey(key)){
                DLLNode<K,V> node=map.get(key);
                node.value=value;
                evictionPolicy.onAccess(node);
            }else{
                if(map.size()>=capacity){
                    DLLNode<K,V> evicted=evictionPolicy.evict();
                    if(evicted!=null) {map.remove(evicted.key); evictions++;}
                }
                DLLNode<K,V> newNode=new DLLNode(key, value);
                map.put(key, newNode);
                evictionPolicy.onInsert(newNode);
            }
            totalPuts++;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Delete key from cache
     * 
     * IMPLEMENTATION HINTS:
     * 1. Acquire write lock
     * 2. Check if key exists
     * 3. Remove from map and DLL
     * 4. Return true if deleted, false if not found
     * 
     * TIME COMPLEXITY: O(1)
     * 
     * @param key Key to delete
     * @return true if deleted, false if not found
     * @throws InvalidKeyException if key is null
     */
    public boolean delete(K key) throws InvalidKeyException {
        // HINT: if (key == null) throw new InvalidKeyException("Key cannot be null");
        // HINT: DLLNode<K, V> node = map.remove(key);
        // HINT: if (node == null) return false;
        // HINT: evictionPolicy.onRemove(node);
        // HINT: return true;
        if(key==null) throw new InvalidKeyException("Key cannot be null");
        
        lock.writeLock().lock();
        try {
            DLLNode<K,V> node=map.remove(key);
            if(node==null) return false;
            evictionPolicy.onRemove(node);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Check if key exists in cache (without affecting LRU order)
     * 
     * @param key Key to check
     * @return true if key exists
     */
    public boolean containsKey(K key) {
        // HINT: return map.containsKey(key);
        return map.containsKey(key);
    }
    
    /**
     * Get current cache size
     * 
     * @return Number of entries in cache
     */
    public int size() {
        // HINT: return map.size();
        return map.size();
    }
    
    /**
     * Clear all entries from cache
     */
    public void clear() {
        // HINT: map.clear();
        // HINT: Reset the eviction policy's DLL
        map.clear();
        this.evictionPolicy=null;
    }
    
    /**
     * Get cache statistics
     */
    public String getStats() {
        double hitRate = (hits + misses) > 0 ? (double) hits / (hits + misses) * 100 : 0;
        return String.format("Hits: %d, Misses: %d, Hit Rate: %.1f%%, Evictions: %d, Puts: %d, Size: %d/%d",
            hits, misses, hitRate, evictions, totalPuts, map.size(), capacity);
    }
    
    public int getCapacity() { return capacity; }
    public int getHits() { return hits; }
    public int getMisses() { return misses; }
    public int getEvictions() { return evictions; }
}

// ===== MAIN TEST CLASS =====

public class LRUCache {
    public static void main(String[] args) {
        System.out.println("=== LRU Cache Test Cases ===\n");
        
        // Test Case 1: Basic Put and Get
        System.out.println("=== Test Case 1: Basic Put and Get ===");
        try {
            Cache<String, Integer> cache = new Cache<>(3, new LRUEvictionPolicy<>());
            cache.put("a", 1);
            cache.put("b", 2);
            cache.put("c", 3);
            
            System.out.println("get(a): " + cache.get("a") + " (expected 1)");
            System.out.println("get(b): " + cache.get("b") + " (expected 2)");
            System.out.println("get(c): " + cache.get("c") + " (expected 3)");
            System.out.println("✓ Size: " + cache.size() + "/3");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 2: LRU Eviction
        System.out.println("=== Test Case 2: LRU Eviction ===");
        try {
            Cache<String, Integer> cache = new Cache<>(3, new LRUEvictionPolicy<>());
            cache.put("a", 1);
            cache.put("b", 2);
            cache.put("c", 3);
            
            // Access "a" to make it MRU, so "b" becomes LRU
            cache.get("a");
            
            // Adding "d" should evict "b" (LRU)
            cache.put("d", 4);
            
            System.out.println("After put(d), get(b): " + cache.get("b") + " (expected null - evicted)");
            System.out.println("get(a): " + cache.get("a") + " (expected 1 - still here)");
            System.out.println("get(d): " + cache.get("d") + " (expected 4 - just added)");
            System.out.println("✓ LRU eviction working");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 3: Update Existing Key
        System.out.println("=== Test Case 3: Update Existing Key ===");
        try {
            Cache<String, String> cache = new Cache<>(3, new LRUEvictionPolicy<>());
            cache.put("key", "old_value");
            cache.put("key", "new_value");
            
            System.out.println("get(key): " + cache.get("key") + " (expected new_value)");
            System.out.println("size: " + cache.size() + " (expected 1 - no duplicate)");
            System.out.println("✓ Update working");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 4: Delete
        System.out.println("=== Test Case 4: Delete ===");
        try {
            Cache<String, Integer> cache = new Cache<>(3, new LRUEvictionPolicy<>());
            cache.put("a", 1);
            cache.put("b", 2);
            
            boolean deleted = cache.delete("a");
            System.out.println("delete(a): " + deleted + " (expected true)");
            System.out.println("get(a): " + cache.get("a") + " (expected null)");
            System.out.println("size: " + cache.size() + " (expected 1)");
            
            boolean deleteMissing = cache.delete("nonexistent");
            System.out.println("delete(nonexistent): " + deleteMissing + " (expected false)");
            System.out.println("✓ Delete working");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 5: Eviction Order
        System.out.println("=== Test Case 5: Eviction Order ===");
        try {
            Cache<Integer, String> cache = new Cache<>(3, new LRUEvictionPolicy<>());
            cache.put(1, "one");
            cache.put(2, "two");
            cache.put(3, "three");
            
            // Access pattern: 1, then 2 → order becomes: 2(MRU), 1, 3(LRU)
            cache.get(1);
            cache.get(2);
            
            // Add 4 → should evict 3 (LRU)
            cache.put(4, "four");
            System.out.println("get(3): " + cache.get(3) + " (expected null - evicted)");
            System.out.println("get(1): " + cache.get(1) + " (expected one)");
            System.out.println("get(2): " + cache.get(2) + " (expected two)");
            System.out.println("get(4): " + cache.get(4) + " (expected four)");
            System.out.println("✓ Eviction order correct");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 6: Capacity 1
        System.out.println("=== Test Case 6: Capacity 1 (edge case) ===");
        try {
            Cache<String, String> cache = new Cache<>(1, new LRUEvictionPolicy<>());
            cache.put("a", "first");
            System.out.println("get(a): " + cache.get("a"));
            
            cache.put("b", "second");  // Should evict "a"
            System.out.println("get(a): " + cache.get("a") + " (expected null)");
            System.out.println("get(b): " + cache.get("b") + " (expected second)");
            System.out.println("✓ Capacity 1 working");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 7: Cache Statistics
        System.out.println("=== Test Case 7: Cache Statistics ===");
        try {
            Cache<String, Integer> cache = new Cache<>(3, new LRUEvictionPolicy<>());
            cache.put("a", 1);
            cache.put("b", 2);
            cache.put("c", 3);
            
            cache.get("a");  // Hit
            cache.get("b");  // Hit
            cache.get("z");  // Miss
            cache.get("y");  // Miss
            
            cache.put("d", 4);  // Eviction
            
            System.out.println(cache.getStats());
            System.out.println("✓ Hits: " + cache.getHits() + " (expected 2)");
            System.out.println("✓ Misses: " + cache.getMisses() + " (expected 2)");
            System.out.println("✓ Evictions: " + cache.getEvictions() + " (expected 1)");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 8: Contains Key
        System.out.println("=== Test Case 8: Contains Key ===");
        try {
            Cache<String, Integer> cache = new Cache<>(3, new LRUEvictionPolicy<>());
            cache.put("exists", 42);
            
            System.out.println("contains(exists): " + cache.containsKey("exists") + " (expected true)");
            System.out.println("contains(missing): " + cache.containsKey("missing") + " (expected false)");
            System.out.println("✓ ContainsKey working");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // ===== EXCEPTION TEST CASES =====
        
        // Test Case 9: Exception - Invalid Capacity
        System.out.println("=== Test Case 9: Exception - Invalid Capacity ===");
        try {
            Cache<String, String> cache = new Cache<>(0, new LRUEvictionPolicy<>());
            System.out.println("✗ Should have thrown InvalidCapacityException");
        } catch (InvalidCapacityException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 10: Exception - Null Key on Put
        System.out.println("=== Test Case 10: Exception - Null Key ===");
        try {
            Cache<String, String> cache = new Cache<>(3, new LRUEvictionPolicy<>());
            cache.put(null, "value");
            System.out.println("✗ Should have thrown InvalidKeyException");
        } catch (InvalidKeyException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 11: Exception - Null Key on Get
        System.out.println("=== Test Case 11: Exception - Null Key on Get ===");
        try {
            Cache<String, String> cache = new Cache<>(3, new LRUEvictionPolicy<>());
            cache.get(null);
            System.out.println("✗ Should have thrown InvalidKeyException");
        } catch (InvalidKeyException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 12: Exception - Negative Capacity
        System.out.println("=== Test Case 12: Exception - Negative Capacity ===");
        try {
            Cache<String, String> cache = new Cache<>(-5, new LRUEvictionPolicy<>());
            System.out.println("✗ Should have thrown InvalidCapacityException");
        } catch (InvalidCapacityException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
            System.out.println("  Capacity: " + e.getCapacity());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 13: Large Stress Test
        System.out.println("=== Test Case 13: Stress Test (1000 items, capacity 100) ===");
        try {
            Cache<Integer, Integer> cache = new Cache<>(100, new LRUEvictionPolicy<>());
            for (int i = 0; i < 1000; i++) {
                cache.put(i, i * 10);
            }
            
            // Only last 100 should be in cache
            System.out.println("Size: " + cache.size() + " (expected 100)");
            System.out.println("get(0): " + cache.get(0) + " (expected null - evicted long ago)");
            System.out.println("get(999): " + cache.get(999) + " (expected 9990)");
            System.out.println("get(900): " + cache.get(900) + " (expected 9000)");
            System.out.println(cache.getStats());
            System.out.println("✓ Stress test complete");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        System.out.println("=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION TOPICS:
 * ============================
 * 
 * 1. WHY HashMap + Doubly Linked List?
 *    HashMap:
 *      - O(1) key lookup
 *      - Maps key → DLL node (direct pointer)
 *    
 *    Doubly Linked List:
 *      - O(1) removal from any position (given node pointer)
 *      - O(1) insertion at head/tail
 *      - Maintains access order
 *    
 *    Combined: O(1) for get, put, delete
 *    Alternative: Java's LinkedHashMap (but won't show understanding)
 * 
 * 2. SENTINEL NODES (Dummy Head/Tail):
 *    Why use them?
 *      - Eliminates null checks at boundaries
 *      - Simplifies add/remove logic
 *      - head.next = MRU, tail.prev = LRU
 *    
 *    Without sentinels:
 *      - Need special cases for empty list
 *      - Need to update head/tail pointers
 *      - More error-prone code
 * 
 * 3. EVICTION POLICIES:
 *    LRU (Least Recently Used):
 *      - Evict item not accessed longest
 *      - Good for: temporal locality
 *      - Used in: CPU caches, Redis
 *    
 *    LFU (Least Frequently Used):
 *      - Evict item accessed least often
 *      - Good for: frequency-based workloads
 *      - More complex (need frequency counter)
 *    
 *    FIFO (First In First Out):
 *      - Evict oldest item
 *      - Simplest to implement
 *      - Ignores access pattern
 *    
 *    Random:
 *      - Evict random item
 *      - O(1) eviction
 *      - Surprisingly good in practice
 * 
 * 4. THREAD SAFETY:
 *    ReadWriteLock:
 *      - Multiple readers OR single writer
 *      - Best for read-heavy caches
 *    
 *    synchronized:
 *      - Simple but coarse-grained
 *      - Only one thread at a time
 *    
 *    ConcurrentHashMap + fine-grained locks:
 *      - Lock per segment/stripe
 *      - Better concurrency
 *    
 *    Lock-free approaches:
 *      - CAS operations
 *      - Very complex to implement correctly
 * 
 * 5. REAL-WORLD IMPLEMENTATIONS:
 *    Redis:
 *      - allkeys-lru, volatile-lru policies
 *      - Approximate LRU (sampling-based)
 *      - Why approximate? (Exact LRU too expensive at scale)
 *    
 *    Memcached:
 *      - Slab allocation + LRU per slab class
 *      - Per-slab LRU lists
 *    
 *    CPU Cache:
 *      - L1/L2/L3 caches use LRU variants
 *      - PLRU (Pseudo-LRU) for hardware efficiency
 *    
 *    CDN:
 *      - Edge caches use LRU/LFU hybrid
 *      - Content-aware eviction
 * 
 * 6. DISTRIBUTED CACHE:
 *    Consistent Hashing:
 *      - Distribute keys across nodes
 *      - Minimize redistribution on node add/remove
 *    
 *    Replication:
 *      - Leader-follower for reads
 *      - Write-through vs write-behind
 *    
 *    Cache Coherence:
 *      - Cache stampede prevention
 *      - Distributed invalidation
 * 
 * 7. CACHE PATTERNS:
 *    Cache-Aside (Lazy Loading):
 *      - App checks cache → if miss → load from DB → store in cache
 *      - Most common pattern
 *    
 *    Write-Through:
 *      - Write to cache AND DB simultaneously
 *      - Consistent but slower writes
 *    
 *    Write-Behind (Write-Back):
 *      - Write to cache, async write to DB
 *      - Fast writes, risk of data loss
 *    
 *    Read-Through:
 *      - Cache loads from DB on miss (cache manages loading)
 *      - Simpler application code
 * 
 * 8. TIME COMPLEXITY SUMMARY:
 *    Operation     | HashMap | DLL     | Combined
 *    get(key)      | O(1)    | O(1)*   | O(1)
 *    put(key, val) | O(1)    | O(1)    | O(1)
 *    delete(key)   | O(1)    | O(1)*   | O(1)
 *    evict()       | O(1)    | O(1)    | O(1)
 *    * = given node pointer from HashMap
 * 
 * 9. LEETCODE REFERENCE:
 *    LeetCode 146 - LRU Cache (Medium)
 *    - Exact same problem
 *    - get(key) and put(key, value) in O(1)
 *    - Industry-standard interview question
 * 
 * 10. API DESIGN:
 *     cache.get(key)           - Get value (O(1))
 *     cache.put(key, value)    - Insert/update (O(1))
 *     cache.delete(key)        - Remove entry (O(1))
 *     cache.containsKey(key)   - Check existence
 *     cache.size()             - Current entries
 *     cache.clear()            - Remove all
 *     cache.getStats()         - Hit rate, misses, evictions
 */
