# ☕ Java Basics Practice Collection

This folder contains core Java concept practice files covering fundamental and advanced Java topics.

## 🎯 Purpose

Master essential Java programming concepts through hands-on practice. These files help you:
- Strengthen core Java fundamentals
- Practice modern Java features (Streams, Lambdas, Concurrency)
- Prepare for technical interviews
- Build muscle memory for common patterns

---

## 📁 Files Overview

### 1. **StreamsPractice.java** (47KB)
**Topics Covered:**
- Stream API basics (filter, map, reduce)
- Collectors and grouping operations
- FlatMap and stream chaining
- Parallel streams
- Stream optimization techniques
- Common interview patterns

**Best For:**
- Java 8+ features
- Functional programming concepts
- Data transformation problems
- Performance optimization discussions

---

### 2. **StreamsInterviewPractice.java** (53KB)
**Topics Covered:**
- Interview-specific stream problems
- Real-world scenarios
- Complex transformations
- Performance considerations
- Edge case handling

**Best For:**
- Interview preparation
- Advanced stream operations
- Problem-solving practice
- Code optimization

---

### 3. **ConcurrencyPractice.java** (26KB)
**Topics Covered:**
- Thread creation and management
- Synchronization mechanisms
- Locks and concurrent collections
- ExecutorService and thread pools
- CompletableFuture and async programming
- Race conditions and deadlock prevention

**Best For:**
- Multi-threading concepts
- Concurrent programming
- Thread safety patterns
- Performance tuning

**Key Concepts:**
- `synchronized` keyword
- `ReentrantLock`, `ReadWriteLock`
- `CountDownLatch`, `CyclicBarrier`, `Semaphore`
- `ConcurrentHashMap`, `CopyOnWriteArrayList`
- `volatile`, `atomic` operations

---

### 4. **StringPractice.java** (38KB)
**Topics Covered:**
- String manipulation techniques
- StringBuilder vs StringBuffer
- String immutability
- Pattern matching and regex
- String algorithms (palindrome, anagram, etc.)
- Common string interview questions

**Best For:**
- String algorithms
- Pattern matching
- Interview problem solving
- Performance optimization (avoiding String concat in loops)

---

### 5. **CollectionsPractice.java** (41KB)
**Topics Covered:**
- List, Set, Map implementations
- ArrayList vs LinkedList
- HashMap vs TreeMap vs LinkedHashMap
- HashSet vs TreeSet vs LinkedHashSet
- Queue, Deque, PriorityQueue
- Custom sorting with Comparator
- Collection utilities

**Best For:**
- Data structure selection
- Collection framework mastery
- Time/space complexity analysis
- Interview fundamentals

**Key Concepts:**
- O(1) vs O(log n) vs O(n) operations
- Hash collision handling
- Tree balancing
- Fail-fast vs fail-safe iterators

---

## 🚀 How to Practice

### Daily Practice Routine:
1. **Pick a topic** based on your weak areas
2. **Read through examples** in the file
3. **Solve problems** without looking at solutions
4. **Run and test** your implementations
5. **Compare** with provided solutions
6. **Note patterns** and techniques

### Running Files:
```bash
cd JavaBasics

# Compile
javac StreamsPractice.java
javac ConcurrencyPractice.java
javac StringPractice.java
javac CollectionsPractice.java

# Run
java StreamsPractice
java ConcurrencyPractice
java StringPractice
java CollectionsPractice
```

---

## 📊 Practice Tracker

| Topic | File | Size | Status | Last Practiced | Notes |
|-------|------|------|--------|----------------|-------|
| Streams | StreamsPractice.java | 47KB | ✅ | Mar 7, 2026 | - |
| Streams Interview | StreamsInterviewPractice.java | 53KB | ✅ | Mar 8, 2026 | - |
| Concurrency | ConcurrencyPractice.java | 26KB | ✅ | Jan 11, 2026 | - |
| Strings | StringPractice.java | 38KB | ✅ | Jan 10, 2026 | - |
| Collections | CollectionsPractice.java | 41KB | ✅ | Jan 10, 2026 | - |

---

## 🎯 Interview Focus Areas

### Must-Know for Interviews:

#### **Streams (High Priority)**
- Converting loops to streams
- Collectors.groupingBy() and partitioningBy()
- FlatMap for nested structures
- Stream vs parallelStream performance
- Common pitfalls (infinite streams, stateful operations)

#### **Concurrency (High Priority)**
- Thread safety and synchronization
- When to use synchronized vs locks
- Concurrent collections vs synchronized wrappers
- Producer-Consumer pattern
- Thread pool sizing

#### **Collections (Critical Foundation)**
- HashMap internals (buckets, load factor, resize)
- ArrayList vs LinkedList performance
- TreeMap/TreeSet for sorted data
- PriorityQueue for heap operations
- Which collection for which use case

#### **Strings (Frequent in Interviews)**
- String pool and immutability
- StringBuilder for concatenation in loops
- Common algorithms (anagram, palindrome, substring)
- Regex for pattern matching

---

## 💡 Common Interview Patterns

### Pattern 1: Stream Processing
```java
// Group, count, sort
list.stream()
    .collect(Collectors.groupingBy(..., Collectors.counting()))
    .entrySet().stream()
    .sorted(Map.Entry.comparingByValue())
    .collect(...);
```

### Pattern 2: Thread-Safe Singleton
```java
// Double-checked locking
private static volatile Singleton instance;
```

### Pattern 3: Producer-Consumer
```java
// BlockingQueue for thread communication
BlockingQueue<Task> queue = new LinkedBlockingQueue<>();
```

---

## 🔥 Quick Reference

### Collections Time Complexity:
| Operation | ArrayList | LinkedList | HashMap | TreeMap |
|-----------|-----------|------------|---------|---------|
| Add | O(1)* | O(1) | O(1)* | O(log n) |
| Get | O(1) | O(n) | O(1)* | O(log n) |
| Remove | O(n) | O(1)** | O(1)* | O(log n) |
| Contains | O(n) | O(n) | O(1)* | O(log n) |

\* Average case, O(n) worst case for HashMap
\** O(1) if you have reference to node

### Concurrency Tools:
- **synchronized**: Method/block level locking
- **ReentrantLock**: Explicit lock with tryLock()
- **Semaphore**: Limit concurrent access
- **CountDownLatch**: Wait for N threads
- **CyclicBarrier**: Sync point for threads
- **CompletableFuture**: Async pipelines

---

## 📖 Recommended Study Order

1. **Week 1**: Collections (Foundation)
2. **Week 2**: Strings (Common in interviews)
3. **Week 3**: Streams (Modern Java, frequent)
4. **Week 4**: Concurrency (Advanced, high value)
5. **Week 5**: Review and mixed practice

---

## 🏆 Interview Tips

1. **Always discuss trade-offs** (time vs space, simplicity vs performance)
2. **Start with brute force**, then optimize
3. **Think about edge cases** (null, empty, single element)
4. **Explain your thought process** out loud
5. **Write clean, readable code** first, optimize later
6. **Test with examples** as you code
7. **Know Big O complexity** of your solutions

---

**Keep practicing and you'll ace those interviews!** 💪

*Last Updated: March 16, 2026*
