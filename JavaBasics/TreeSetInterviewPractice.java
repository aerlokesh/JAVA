/**
 * TreeSetInterviewPractice.java
 * 
 * A comprehensive Java TreeSet practice file covering ALL interview topics.
 * TreeSet is a NavigableSet backed by a TreeMap (Red-Black tree).
 * Elements are sorted in natural order or by a custom Comparator.
 * 
 * KEY INTERVIEW POINTS:
 * - Implements NavigableSet → SortedSet → Set → Collection
 * - O(log n) for add, remove, contains (Red-Black tree)
 * - Does NOT allow null (throws NullPointerException)
 * - Does NOT allow duplicates
 * - NOT thread-safe (use Collections.synchronizedSortedSet())
 * - Ordering must be consistent with equals() for correct Set behavior
 * - Uses compareTo() (Comparable) or compare() (Comparator) — NOT hashCode/equals
 * 
 * Instructions:
 * 1. Read each method's description carefully
 * 2. Replace the "TODO: Implement this method" with your solution
 * 3. Run: javac TreeSetInterviewPractice.java && java TreeSetInterviewPractice
 * 4. Check if all test cases pass
 */

import java.util.*;

public class TreeSetInterviewPractice {

    // ==================== SECTION 1: BASIC CREATION & OPERATIONS ====================

    /**
     * Task 1: Create a TreeSet from an array of integers
     * The TreeSet should contain elements in sorted (natural) order.
     * 
     * Example: [5, 3, 8, 1, 3, 9] → [1, 3, 5, 8, 9]  (duplicates removed, sorted)
     * 
     * @param elements Array of integers
     * @return TreeSet with elements in natural sorted order
     */
    public static TreeSet<Integer> createTreeSet(Integer[] elements) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Task 2: Create a TreeSet with DESCENDING (reverse) order
     * 
     * Interview Tip: Use Comparator.reverseOrder() or Collections.reverseOrder()
     * 
     * Example: [5, 3, 8, 1] → [8, 5, 3, 1]
     * 
     * @param elements Array of integers
     * @return TreeSet with elements in descending order
     */
    public static TreeSet<Integer> createDescendingTreeSet(Integer[] elements) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Task 3: Create a TreeSet of strings sorted by LENGTH (shortest first),
     * and if same length, sorted alphabetically.
     * 
     * Interview Tip: Custom Comparator with chained comparison
     * 
     * Example: ["banana", "fig", "apple", "kiwi", "date"] → ["fig", "date", "kiwi", "apple", "banana"]
     * 
     * @param words Array of strings
     * @return TreeSet sorted by length then alphabetically
     */
    public static TreeSet<String> createLengthSortedTreeSet(String[] words) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Task 4: Add elements to TreeSet and return how many were actually NEW additions.
     * 
     * Interview Tip: add() returns boolean — true if element was actually added.
     * 
     * Example: set=[1,2,3], elements=[3,4,5,5] → returns 2 (only 4 and 5 were new)
     * 
     * @param set The existing TreeSet
     * @param elements Elements to add
     * @return Count of elements that were actually new additions
     */
    public static int addAndCountNew(TreeSet<Integer> set, Integer[] elements) {
        // TODO: Implement this method
        return -1;
    }

    /**
     * Task 5: Remove all elements from the TreeSet that are in the given collection.
     * Return the set after removal.
     * 
     * Interview Tip: Use removeAll() — O(n * log n)
     * 
     * Example: set=[1,2,3,4,5], toRemove=[2,4,6] → [1,3,5]
     * 
     * @param set The TreeSet
     * @param toRemove Collection of elements to remove
     * @return Modified TreeSet
     */
    public static TreeSet<Integer> removeAll(TreeSet<Integer> set, Collection<Integer> toRemove) {
        // TODO: Implement this method
        return null;
    }

    // ==================== SECTION 2: NAVIGATION METHODS (KEY INTERVIEW TOPIC!) ====================

    /**
     * Task 6: Get the GREATEST element LESS THAN the given value (strict lower bound).
     * 
     * Interview Tip: lower() returns strictly less than. Returns null if no such element.
     * 
     * Example: set=[1,3,5,7,9], value=6 → returns 5
     * Example: set=[1,3,5,7,9], value=1 → returns null
     * 
     * @param set The TreeSet
     * @param value The reference value
     * @return Greatest element < value, or null
     */
    public static Integer strictLowerBound(TreeSet<Integer> set, int value) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Task 7: Get the GREATEST element LESS THAN OR EQUAL TO the given value (floor).
     * 
     * Interview Tip: floor() returns ≤ value. Returns null if no such element.
     * 
     * Example: set=[1,3,5,7,9], value=6 → returns 5
     * Example: set=[1,3,5,7,9], value=5 → returns 5  (equal counts!)
     * 
     * @param set The TreeSet
     * @param value The reference value
     * @return Greatest element ≤ value, or null
     */
    public static Integer floorValue(TreeSet<Integer> set, int value) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Task 8: Get the SMALLEST element GREATER THAN the given value (strict upper bound).
     * 
     * Interview Tip: higher() returns strictly greater than.
     * 
     * Example: set=[1,3,5,7,9], value=6 → returns 7
     * Example: set=[1,3,5,7,9], value=9 → returns null
     * 
     * @param set The TreeSet
     * @param value The reference value
     * @return Smallest element > value, or null
     */
    public static Integer strictUpperBound(TreeSet<Integer> set, int value) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Task 9: Get the SMALLEST element GREATER THAN OR EQUAL TO the given value (ceiling).
     * 
     * Interview Tip: ceiling() returns ≥ value.
     * 
     * Example: set=[1,3,5,7,9], value=6 → returns 7
     * Example: set=[1,3,5,7,9], value=5 → returns 5  (equal counts!)
     * 
     * @param set The TreeSet
     * @param value The reference value
     * @return Smallest element ≥ value, or null
     */
    public static Integer ceilingValue(TreeSet<Integer> set, int value) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Task 10: Get the first (smallest) and last (largest) elements.
     * Return them as a 2-element array [first, last].
     * 
     * Interview Tip: first() and last() throw NoSuchElementException on empty set.
     * 
     * Example: set=[3,1,7,2,9] → [1, 9]
     * 
     * @param set The TreeSet
     * @return Array of [first, last] or null if empty
     */
    public static Integer[] getFirstAndLast(TreeSet<Integer> set) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Task 11: Poll (remove and return) the first and last elements.
     * Return them as a 2-element array [polledFirst, polledLast].
     * 
     * Interview Tip: pollFirst()/pollLast() return null on empty set (don't throw).
     *   vs first()/last() which THROW NoSuchElementException.
     * 
     * Example: set=[1,3,5,7,9] → returns [1,9], set becomes [3,5,7]
     * 
     * @param set The TreeSet (will be modified)
     * @return Array of [polledFirst, polledLast]
     */
    public static Integer[] pollFirstAndLast(TreeSet<Integer> set) {
        // TODO: Implement this method
        return null;
    }

    // ==================== SECTION 3: RANGE VIEWS (SUBSETS) ====================

    /**
     * Task 12: Get all elements LESS THAN the given value (exclusive upper bound).
     * Return as a new TreeSet.
     * 
     * Interview Tip: headSet(toElement) → elements < toElement
     *               headSet(toElement, true) → elements ≤ toElement
     * 
     * Example: set=[1,3,5,7,9], value=7 → [1,3,5]
     * 
     * @param set The TreeSet
     * @param toElement Upper bound (exclusive)
     * @return New TreeSet with elements < toElement
     */
    public static TreeSet<Integer> getHeadSet(TreeSet<Integer> set, int toElement) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Task 13: Get all elements GREATER THAN OR EQUAL TO the given value.
     * Return as a new TreeSet.
     * 
     * Interview Tip: tailSet(fromElement) → elements ≥ fromElement
     *               tailSet(fromElement, false) → elements > fromElement
     * 
     * Example: set=[1,3,5,7,9], value=5 → [5,7,9]
     * 
     * @param set The TreeSet
     * @param fromElement Lower bound (inclusive)
     * @return New TreeSet with elements ≥ fromElement
     */
    public static TreeSet<Integer> getTailSet(TreeSet<Integer> set, int fromElement) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Task 14: Get all elements in range [fromElement, toElement) — inclusive start, exclusive end.
     * Return as a new TreeSet.
     * 
     * Interview Tip: subSet(from, to) → from ≤ elements < to
     *               subSet(from, fromInclusive, to, toInclusive) → flexible bounds
     * 
     * Example: set=[1,3,5,7,9], from=3, to=8 → [3,5,7]
     * 
     * @param set The TreeSet
     * @param fromElement Start (inclusive)
     * @param toElement End (exclusive)
     * @return New TreeSet with elements in range
     */
    public static TreeSet<Integer> getSubSet(TreeSet<Integer> set, int fromElement, int toElement) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Task 15: Get all elements in INCLUSIVE range [from, to].
     * Return as a new TreeSet.
     * 
     * Interview Tip: Use the 4-parameter subSet(from, true, to, true)
     * 
     * Example: set=[1,3,5,7,9], from=3, to=7 → [3,5,7]
     * 
     * @param set The TreeSet
     * @param from Start (inclusive)
     * @param to End (inclusive)
     * @return New TreeSet with elements in inclusive range
     */
    public static TreeSet<Integer> getInclusiveSubSet(TreeSet<Integer> set, int from, int to) {
        // TODO: Implement this method
        return null;
    }

    // ==================== SECTION 4: DESCENDING VIEWS & ITERATION ====================

    /**
     * Task 16: Get a DESCENDING view of the TreeSet.
     * Return elements as a List in descending order.
     * 
     * Interview Tip: descendingSet() returns a NavigableSet view in reverse order.
     *   It's a VIEW, not a copy — changes reflect in the original set.
     * 
     * Example: set=[1,3,5,7,9] → [9,7,5,3,1]
     * 
     * @param set The TreeSet
     * @return List of elements in descending order
     */
    public static List<Integer> getDescendingList(TreeSet<Integer> set) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Task 17: Get a descending iterator and collect elements into a List.
     * 
     * Interview Tip: descendingIterator() — another way to iterate in reverse.
     *   Useful when you need element-by-element control during reverse iteration.
     * 
     * Example: set=[1,3,5,7,9] → [9,7,5,3,1]
     * 
     * @param set The TreeSet
     * @return List of elements via descending iterator
     */
    public static List<Integer> iterateDescending(TreeSet<Integer> set) {
        // TODO: Implement this method
        return null;
    }

    // ==================== SECTION 5: INTERVIEW PROBLEM-SOLVING ====================

    /**
     * Task 18: Find the K-th smallest element in the TreeSet.
     * (1-indexed: k=1 means the smallest element)
     * 
     * Interview Tip: TreeSet doesn't support index-based access.
     *   You must iterate or convert. This is O(k) — not O(1)!
     * 
     * Example: set=[10,20,30,40,50], k=3 → 30
     * 
     * @param set The TreeSet
     * @param k The position (1-indexed)
     * @return K-th smallest element, or null if k is invalid
     */
    public static Integer kthSmallest(TreeSet<Integer> set, int k) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Task 19: Find the K-th largest element in the TreeSet.
     * (1-indexed: k=1 means the largest element)
     * 
     * Interview Tip: Use descendingIterator() or descendingSet()
     * 
     * Example: set=[10,20,30,40,50], k=2 → 40
     * 
     * @param set The TreeSet
     * @param k The position (1-indexed)
     * @return K-th largest element, or null if k is invalid
     */
    public static Integer kthLargest(TreeSet<Integer> set, int k) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Task 20: Find the number of elements in range [low, high] (inclusive).
     * 
     * Interview Tip: Use subSet(low, true, high, true).size()
     *   This is O(log n + k) where k is the count of elements in range.
     * 
     * Example: set=[1,3,5,7,9,11], low=3, high=9 → 4 (elements: 3,5,7,9)
     * 
     * @param set The TreeSet
     * @param low Lower bound (inclusive)
     * @param high Upper bound (inclusive)
     * @return Count of elements in range
     */
    public static int countInRange(TreeSet<Integer> set, int low, int high) {
        // TODO: Implement this method
        return -1;
    }

    /**
     * Task 21: Find the closest element to the given value.
     * If two elements are equally close, return the smaller one.
     * 
     * Interview Tip: Use floor() and ceiling() to find the two candidates,
     *   then compare distances. Classic interview question!
     * 
     * Example: set=[1,5,10,15,20], value=12 → 10 (distance 2 vs 3)
     * Example: set=[1,5,10,15,20], value=13 → 15 (distance 3 vs 2, 15 is closer)
     * Example: set=[1,5,10,15,20], value=7  → 5  (distance 2 vs 3, 5 is closer)
     * Example: set=[10,20], value=15 → 10 (equal distance, pick smaller)
     * 
     * @param set The TreeSet
     * @param value Target value
     * @return Closest element in set, or null if set is empty
     */
    public static Integer findClosest(TreeSet<Integer> set, int value) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Task 22: Given an array, find pairs of elements whose difference equals the target.
     * Use TreeSet for efficient lookup. Return sorted list of pairs as "a,b" strings where a < b.
     * 
     * Interview Tip: For each element x, check if (x + target) exists in the set. O(n log n)
     * 
     * Example: arr=[1,5,3,4,2], target=2 → ["1,3", "2,4", "3,5"]
     * 
     * @param arr Array of integers
     * @param target Target difference
     * @return Sorted list of pair strings
     */
    public static List<String> findPairsWithDifference(int[] arr, int target) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Task 23: Merge two sorted TreeSets into a single sorted TreeSet without duplicates.
     * 
     * Interview Tip: addAll is simple, but you can also discuss merge-like approaches.
     *   TreeSet handles sorting & dedup automatically.
     * 
     * Example: set1=[1,3,5], set2=[2,3,6] → [1,2,3,5,6]
     * 
     * @param set1 First TreeSet
     * @param set2 Second TreeSet
     * @return Merged TreeSet
     */
    public static TreeSet<Integer> mergeSortedSets(TreeSet<Integer> set1, TreeSet<Integer> set2) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Task 24: Find the SYMMETRIC DIFFERENCE of two TreeSets.
     * (Elements in either set but NOT in both)
     * 
     * Interview Tip: (A ∪ B) - (A ∩ B), or equivalently (A - B) ∪ (B - A)
     * 
     * Example: set1=[1,2,3,4], set2=[3,4,5,6] → [1,2,5,6]
     * 
     * @param set1 First TreeSet
     * @param set2 Second TreeSet
     * @return Symmetric difference as a new TreeSet
     */
    public static TreeSet<Integer> symmetricDifference(TreeSet<Integer> set1, TreeSet<Integer> set2) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Task 25: Implement a "sliding window min" using TreeSet.
     * Given an array and window size k, find the minimum of each window.
     * 
     * Interview Tip: TreeSet provides O(log n) first() for min.
     *   Challenge: TreeSet doesn't allow duplicates, so use a wrapper or TreeMap<Integer,Integer> for counts.
     *   Here we simplify: assume all elements are unique for this exercise.
     * 
     * Example: arr=[4,2,5,1,3,6], k=3 → [2,1,1,1]
     *   Window [4,2,5]→min=2, [2,5,1]→min=1, [5,1,3]→min=1, [1,3,6]→min=1
     * 
     * @param arr Array of UNIQUE integers
     * @param k Window size
     * @return List of minimums for each window
     */
    public static List<Integer> slidingWindowMin(int[] arr, int k) {
        // TODO: Implement this method
        return null;
    }

    // ==================== SECTION 6: CUSTOM OBJECTS WITH TREESET ====================

    /**
     * Task 26: Create a TreeSet of Employee objects sorted by salary (ascending),
     * and if same salary, by name alphabetically.
     * 
     * Interview Tip: You must provide a Comparator OR have Employee implement Comparable.
     *   If neither, you get ClassCastException at runtime!
     * 
     * Example: [("Bob",50000), ("Alice",60000), ("Charlie",50000)]
     *        → [("Bob",50000), ("Charlie",50000), ("Alice",60000)]
     * 
     * @param employees Array of Employee objects
     * @return TreeSet sorted by salary then name
     */
    public static TreeSet<Employee> createEmployeeTreeSet(Employee[] employees) {
        // TODO: Implement this method
        // Hint: TreeSet<Employee> set = new TreeSet<>(Comparator...);
        return null;
    }

    /**
     * Task 27: Find all employees with salary in range [minSalary, maxSalary].
     * The TreeSet is sorted by salary (same comparator as Task 26).
     * 
     * Interview Tip: Use subSet() with dummy Employee objects as bounds.
     *   This is the power of NavigableSet — range queries on custom objects!
     * 
     * @param set TreeSet of employees sorted by salary
     * @param minSalary Minimum salary (inclusive)
     * @param maxSalary Maximum salary (inclusive)
     * @return List of employee names in the salary range
     */
    public static List<String> employeesInSalaryRange(TreeSet<Employee> set, int minSalary, int maxSalary) {
        // TODO: Implement this method
        // Hint: Create dummy Employee objects as range bounds
        return null;
    }

    // ==================== SECTION 7: TREESET vs OTHER SETS (INTERVIEW COMPARISON) ====================

    /**
     * Task 28: Demonstrate that TreeSet uses compareTo() NOT equals() for uniqueness.
     * Create a TreeSet with case-insensitive String comparison.
     * Add both "Apple" and "apple" — only one should remain!
     * 
     * Interview Tip: This is a CLASSIC gotcha!
     *   TreeSet considers two elements "equal" if compareTo returns 0.
     *   So with case-insensitive comparator, "Apple" and "apple" are the SAME element.
     * 
     * Example: words=["Apple", "banana", "apple", "BANANA", "Cherry"]
     *        → size should be 3, containing one version of each
     * 
     * @param words Array of strings
     * @return TreeSet with case-insensitive deduplication
     */
    public static TreeSet<String> caseInsensitiveSet(String[] words) {
        // TODO: Implement this method
        // Hint: Use String.CASE_INSENSITIVE_ORDER as comparator
        return null;
    }

    /**
     * Task 29: Convert a TreeSet to an unmodifiable/immutable set.
     * 
     * Interview Tip: Collections.unmodifiableSortedSet() preserves ordering.
     *   In Java 10+: Set.copyOf() loses ordering (returns unordered Set).
     *   Choose based on whether you need sorted property.
     * 
     * @param set The TreeSet
     * @return Unmodifiable sorted set
     */
    public static SortedSet<Integer> toUnmodifiable(TreeSet<Integer> set) {
        // TODO: Implement this method
        return null;
    }

    /**
     * Task 30: Check if a TreeSet is a SUBSET of another TreeSet.
     * 
     * Interview Tip: Use containsAll() — O(n * log m) where n=subset size, m=superset size.
     * 
     * Example: superset=[1,2,3,4,5], subset=[2,4] → true
     * Example: superset=[1,2,3,4,5], subset=[2,6] → false
     * 
     * @param superset The larger set
     * @param subset The potential subset
     * @return true if subset ⊆ superset
     */
    public static boolean isSubset(TreeSet<Integer> superset, TreeSet<Integer> subset) {
        // TODO: Implement this method
        return false;
    }

    // ==================== HELPER CLASS ====================

    static class Employee {
        String name;
        int salary;

        Employee(String name, int salary) {
            this.name = name;
            this.salary = salary;
        }

        @Override
        public String toString() {
            return name + "(" + salary + ")";
        }
    }

    // ==================== TEST CASES ====================

    public static void main(String[] args) {
        int totalTests = 0;
        int passedTests = 0;

        System.out.println("=".repeat(70));
        System.out.println("   TREESET INTERVIEW PRACTICE - COMPREHENSIVE TEST SUITE");
        System.out.println("=".repeat(70));

        // ---- SECTION 1: BASIC CREATION & OPERATIONS ----
        System.out.println("\n" + "-".repeat(50));
        System.out.println("SECTION 1: BASIC CREATION & OPERATIONS");
        System.out.println("-".repeat(50));

        // Test 1: createTreeSet
        System.out.println("\n[Test 1] createTreeSet");
        TreeSet<Integer> ts1 = createTreeSet(new Integer[]{5, 3, 8, 1, 3, 9});
        totalTests++;
        if (testTreeSet(ts1, new TreeSet<>(Arrays.asList(1, 3, 5, 8, 9)), "Create TreeSet with duplicates")) passedTests++;

        // Test 2: createDescendingTreeSet
        System.out.println("\n[Test 2] createDescendingTreeSet");
        TreeSet<Integer> ts2 = createDescendingTreeSet(new Integer[]{5, 3, 8, 1});
        totalTests++;
        if (ts2 != null && ts2.size() == 4 && ts2.first() == 8 && ts2.last() == 1) {
            System.out.println("  ✓ PASS: Descending TreeSet => " + ts2);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Descending TreeSet");
            System.out.println("    Expected: [8, 5, 3, 1], Actual: " + ts2);
        }

        // Test 3: createLengthSortedTreeSet
        System.out.println("\n[Test 3] createLengthSortedTreeSet");
        TreeSet<String> ts3 = createLengthSortedTreeSet(new String[]{"banana", "fig", "apple", "kiwi", "date"});
        totalTests++;
        List<String> ts3List = ts3 != null ? new ArrayList<>(ts3) : null;
        if (testStringList(ts3List, Arrays.asList("fig", "date", "kiwi", "apple", "banana"), "Length-sorted TreeSet")) passedTests++;

        // Test 4: addAndCountNew
        System.out.println("\n[Test 4] addAndCountNew");
        TreeSet<Integer> ts4 = new TreeSet<>(Arrays.asList(1, 2, 3));
        int newCount = addAndCountNew(ts4, new Integer[]{3, 4, 5, 5});
        totalTests++;
        if (testInt(newCount, 2, "Count of new additions")) passedTests++;

        // Test 5: removeAll
        System.out.println("\n[Test 5] removeAll");
        TreeSet<Integer> ts5 = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5));
        TreeSet<Integer> ts5Result = removeAll(ts5, Arrays.asList(2, 4, 6));
        totalTests++;
        if (testTreeSet(ts5Result, new TreeSet<>(Arrays.asList(1, 3, 5)), "Remove all")) passedTests++;

        // ---- SECTION 2: NAVIGATION METHODS ----
        System.out.println("\n" + "-".repeat(50));
        System.out.println("SECTION 2: NAVIGATION METHODS (lower/floor/higher/ceiling)");
        System.out.println("-".repeat(50));

        TreeSet<Integer> navSet = new TreeSet<>(Arrays.asList(1, 3, 5, 7, 9));

        // Test 6: strictLowerBound (lower)
        System.out.println("\n[Test 6] strictLowerBound (lower)");
        totalTests++;
        if (testInteger(strictLowerBound(navSet, 6), 5, "lower(6) in [1,3,5,7,9]")) passedTests++;
        totalTests++;
        if (testInteger(strictLowerBound(navSet, 5), 3, "lower(5) in [1,3,5,7,9]")) passedTests++;
        totalTests++;
        if (testInteger(strictLowerBound(navSet, 1), null, "lower(1) in [1,3,5,7,9]")) passedTests++;

        // Test 7: floorValue
        System.out.println("\n[Test 7] floorValue (floor)");
        totalTests++;
        if (testInteger(floorValue(navSet, 6), 5, "floor(6) in [1,3,5,7,9]")) passedTests++;
        totalTests++;
        if (testInteger(floorValue(navSet, 5), 5, "floor(5) in [1,3,5,7,9]")) passedTests++;
        totalTests++;
        if (testInteger(floorValue(navSet, 0), null, "floor(0) in [1,3,5,7,9]")) passedTests++;

        // Test 8: strictUpperBound (higher)
        System.out.println("\n[Test 8] strictUpperBound (higher)");
        totalTests++;
        if (testInteger(strictUpperBound(navSet, 6), 7, "higher(6) in [1,3,5,7,9]")) passedTests++;
        totalTests++;
        if (testInteger(strictUpperBound(navSet, 5), 7, "higher(5) in [1,3,5,7,9]")) passedTests++;
        totalTests++;
        if (testInteger(strictUpperBound(navSet, 9), null, "higher(9) in [1,3,5,7,9]")) passedTests++;

        // Test 9: ceilingValue
        System.out.println("\n[Test 9] ceilingValue (ceiling)");
        totalTests++;
        if (testInteger(ceilingValue(navSet, 6), 7, "ceiling(6) in [1,3,5,7,9]")) passedTests++;
        totalTests++;
        if (testInteger(ceilingValue(navSet, 5), 5, "ceiling(5) in [1,3,5,7,9]")) passedTests++;
        totalTests++;
        if (testInteger(ceilingValue(navSet, 10), null, "ceiling(10) in [1,3,5,7,9]")) passedTests++;

        // Test 10: getFirstAndLast
        System.out.println("\n[Test 10] getFirstAndLast");
        TreeSet<Integer> ts10 = new TreeSet<>(Arrays.asList(3, 1, 7, 2, 9));
        Integer[] firstLast = getFirstAndLast(ts10);
        totalTests++;
        if (firstLast != null && firstLast.length == 2) {
            if (testInteger(firstLast[0], 1, "First element") && testInteger(firstLast[1], 9, "Last element")) passedTests++;
        } else {
            System.out.println("  ✗ FAIL: getFirstAndLast returned null or wrong size");
        }

        // Test 11: pollFirstAndLast
        System.out.println("\n[Test 11] pollFirstAndLast");
        TreeSet<Integer> ts11 = new TreeSet<>(Arrays.asList(1, 3, 5, 7, 9));
        Integer[] polled = pollFirstAndLast(ts11);
        totalTests++;
        if (polled != null && polled[0] == 1 && polled[1] == 9 && ts11.size() == 3) {
            System.out.println("  ✓ PASS: Polled [1,9], remaining: " + ts11);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: pollFirstAndLast");
            System.out.println("    Expected: polled=[1,9] remaining=[3,5,7], Actual: polled=" + Arrays.toString(polled) + " remaining=" + ts11);
        }

        // ---- SECTION 3: RANGE VIEWS ----
        System.out.println("\n" + "-".repeat(50));
        System.out.println("SECTION 3: RANGE VIEWS (headSet/tailSet/subSet)");
        System.out.println("-".repeat(50));

        TreeSet<Integer> rangeSet = new TreeSet<>(Arrays.asList(1, 3, 5, 7, 9));

        // Test 12: getHeadSet
        System.out.println("\n[Test 12] getHeadSet");
        totalTests++;
        if (testTreeSet(getHeadSet(rangeSet, 7), new TreeSet<>(Arrays.asList(1, 3, 5)), "headSet(7)")) passedTests++;

        // Test 13: getTailSet
        System.out.println("\n[Test 13] getTailSet");
        totalTests++;
        if (testTreeSet(getTailSet(rangeSet, 5), new TreeSet<>(Arrays.asList(5, 7, 9)), "tailSet(5)")) passedTests++;

        // Test 14: getSubSet
        System.out.println("\n[Test 14] getSubSet [from, to)");
        totalTests++;
        if (testTreeSet(getSubSet(rangeSet, 3, 8), new TreeSet<>(Arrays.asList(3, 5, 7)), "subSet(3,8)")) passedTests++;

        // Test 15: getInclusiveSubSet
        System.out.println("\n[Test 15] getInclusiveSubSet [from, to]");
        totalTests++;
        if (testTreeSet(getInclusiveSubSet(rangeSet, 3, 7), new TreeSet<>(Arrays.asList(3, 5, 7)), "subSet(3,true,7,true)")) passedTests++;

        // ---- SECTION 4: DESCENDING VIEWS ----
        System.out.println("\n" + "-".repeat(50));
        System.out.println("SECTION 4: DESCENDING VIEWS & ITERATION");
        System.out.println("-".repeat(50));

        TreeSet<Integer> descSet = new TreeSet<>(Arrays.asList(1, 3, 5, 7, 9));

        // Test 16: getDescendingList
        System.out.println("\n[Test 16] getDescendingList");
        totalTests++;
        if (testIntList(getDescendingList(descSet), Arrays.asList(9, 7, 5, 3, 1), "Descending list")) passedTests++;

        // Test 17: iterateDescending
        System.out.println("\n[Test 17] iterateDescending");
        totalTests++;
        if (testIntList(iterateDescending(descSet), Arrays.asList(9, 7, 5, 3, 1), "Descending iterator")) passedTests++;

        // ---- SECTION 5: INTERVIEW PROBLEMS ----
        System.out.println("\n" + "-".repeat(50));
        System.out.println("SECTION 5: INTERVIEW PROBLEM-SOLVING");
        System.out.println("-".repeat(50));

        TreeSet<Integer> probSet = new TreeSet<>(Arrays.asList(10, 20, 30, 40, 50));

        // Test 18: kthSmallest
        System.out.println("\n[Test 18] kthSmallest");
        totalTests++;
        if (testInteger(kthSmallest(probSet, 3), 30, "3rd smallest in [10,20,30,40,50]")) passedTests++;
        totalTests++;
        if (testInteger(kthSmallest(probSet, 1), 10, "1st smallest")) passedTests++;

        // Test 19: kthLargest
        System.out.println("\n[Test 19] kthLargest");
        totalTests++;
        if (testInteger(kthLargest(probSet, 2), 40, "2nd largest in [10,20,30,40,50]")) passedTests++;
        totalTests++;
        if (testInteger(kthLargest(probSet, 1), 50, "1st largest")) passedTests++;

        // Test 20: countInRange
        System.out.println("\n[Test 20] countInRange");
        TreeSet<Integer> countSet = new TreeSet<>(Arrays.asList(1, 3, 5, 7, 9, 11));
        totalTests++;
        if (testInt(countInRange(countSet, 3, 9), 4, "Count in [3,9]")) passedTests++;
        totalTests++;
        if (testInt(countInRange(countSet, 4, 8), 2, "Count in [4,8] → {5,7}")) passedTests++;

        // Test 21: findClosest
        System.out.println("\n[Test 21] findClosest");
        TreeSet<Integer> closeSet = new TreeSet<>(Arrays.asList(1, 5, 10, 15, 20));
        totalTests++;
        if (testInteger(findClosest(closeSet, 12), 10, "Closest to 12 in [1,5,10,15,20]")) passedTests++;
        totalTests++;
        if (testInteger(findClosest(closeSet, 13), 15, "Closest to 13")) passedTests++;
        totalTests++;
        if (testInteger(findClosest(closeSet, 7), 5, "Closest to 7")) passedTests++;
        totalTests++;
        if (testInteger(findClosest(new TreeSet<>(Arrays.asList(10, 20)), 15), 10, "Equal distance → pick smaller")) passedTests++;

        // Test 22: findPairsWithDifference
        System.out.println("\n[Test 22] findPairsWithDifference");
        List<String> pairs = findPairsWithDifference(new int[]{1, 5, 3, 4, 2}, 2);
        totalTests++;
        if (testStringList(pairs, Arrays.asList("1,3", "2,4", "3,5"), "Pairs with diff=2")) passedTests++;

        // Test 23: mergeSortedSets
        System.out.println("\n[Test 23] mergeSortedSets");
        TreeSet<Integer> mergeA = new TreeSet<>(Arrays.asList(1, 3, 5));
        TreeSet<Integer> mergeB = new TreeSet<>(Arrays.asList(2, 3, 6));
        totalTests++;
        if (testTreeSet(mergeSortedSets(mergeA, mergeB), new TreeSet<>(Arrays.asList(1, 2, 3, 5, 6)), "Merge sets")) passedTests++;

        // Test 24: symmetricDifference
        System.out.println("\n[Test 24] symmetricDifference");
        TreeSet<Integer> symA = new TreeSet<>(Arrays.asList(1, 2, 3, 4));
        TreeSet<Integer> symB = new TreeSet<>(Arrays.asList(3, 4, 5, 6));
        totalTests++;
        if (testTreeSet(symmetricDifference(symA, symB), new TreeSet<>(Arrays.asList(1, 2, 5, 6)), "Symmetric diff")) passedTests++;

        // Test 25: slidingWindowMin
        System.out.println("\n[Test 25] slidingWindowMin");
        totalTests++;
        if (testIntList(slidingWindowMin(new int[]{4, 2, 5, 1, 3, 6}, 3),
                Arrays.asList(2, 1, 1, 1), "Sliding window min k=3")) passedTests++;

        // ---- SECTION 6: CUSTOM OBJECTS ----
        System.out.println("\n" + "-".repeat(50));
        System.out.println("SECTION 6: CUSTOM OBJECTS WITH TREESET");
        System.out.println("-".repeat(50));

        // Test 26: createEmployeeTreeSet
        System.out.println("\n[Test 26] createEmployeeTreeSet");
        Employee[] emps = {
            new Employee("Bob", 50000),
            new Employee("Alice", 60000),
            new Employee("Charlie", 50000)
        };
        TreeSet<Employee> empSet = createEmployeeTreeSet(emps);
        totalTests++;
        if (empSet != null && empSet.size() == 3) {
            List<String> empNames = new ArrayList<>();
            for (Employee e : empSet) empNames.add(e.name);
            if (testStringList(empNames, Arrays.asList("Bob", "Charlie", "Alice"), "Employee sort by salary")) passedTests++;
        } else {
            System.out.println("  ✗ FAIL: createEmployeeTreeSet returned null or wrong size");
        }

        // Test 27: employeesInSalaryRange
        System.out.println("\n[Test 27] employeesInSalaryRange");
        Employee[] emps2 = {
            new Employee("Alice", 40000),
            new Employee("Bob", 50000),
            new Employee("Charlie", 60000),
            new Employee("Diana", 70000),
            new Employee("Eve", 80000)
        };
        TreeSet<Employee> empSet2 = createEmployeeTreeSet(emps2);
        List<String> inRange = employeesInSalaryRange(empSet2, 50000, 70000);
        totalTests++;
        if (testStringList(inRange, Arrays.asList("Bob", "Charlie", "Diana"), "Employees in salary [50k,70k]")) passedTests++;

        // ---- SECTION 7: TREESET vs OTHER SETS ----
        System.out.println("\n" + "-".repeat(50));
        System.out.println("SECTION 7: TREESET SPECIAL BEHAVIORS");
        System.out.println("-".repeat(50));

        // Test 28: caseInsensitiveSet
        System.out.println("\n[Test 28] caseInsensitiveSet");
        TreeSet<String> ciSet = caseInsensitiveSet(new String[]{"Apple", "banana", "apple", "BANANA", "Cherry"});
        totalTests++;
        if (ciSet != null && ciSet.size() == 3) {
            System.out.println("  ✓ PASS: Case-insensitive set size=3 => " + ciSet);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Case-insensitive set");
            System.out.println("    Expected size: 3, Actual: " + (ciSet != null ? ciSet.size() + " " + ciSet : "null"));
        }

        // Test 29: toUnmodifiable
        System.out.println("\n[Test 29] toUnmodifiable");
        TreeSet<Integer> ts29 = new TreeSet<>(Arrays.asList(1, 2, 3));
        SortedSet<Integer> unmod = toUnmodifiable(ts29);
        totalTests++;
        boolean threw = false;
        try {
            if (unmod != null) unmod.add(4);
        } catch (UnsupportedOperationException e) {
            threw = true;
        }
        if (threw) {
            System.out.println("  ✓ PASS: Unmodifiable set throws UnsupportedOperationException on add");
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Unmodifiable set should throw on modification");
        }

        // Test 30: isSubset
        System.out.println("\n[Test 30] isSubset");
        TreeSet<Integer> superSet = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5));
        totalTests++;
        if (testBoolean(isSubset(superSet, new TreeSet<>(Arrays.asList(2, 4))), true, "[2,4] ⊆ [1,2,3,4,5]")) passedTests++;
        totalTests++;
        if (testBoolean(isSubset(superSet, new TreeSet<>(Arrays.asList(2, 6))), false, "[2,6] ⊄ [1,2,3,4,5]")) passedTests++;

        // ==================== FINAL RESULTS ====================
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST RESULTS");
        System.out.println("=".repeat(70));
        System.out.println("Total Tests: " + totalTests);
        System.out.println("Passed: " + passedTests);
        System.out.println("Failed: " + (totalTests - passedTests));
        System.out.println("Success Rate: " + String.format("%.2f", (passedTests * 100.0 / totalTests)) + "%");
        System.out.println("=".repeat(70));

        if (passedTests == totalTests) {
            System.out.println("\n🎉 CONGRATULATIONS! All tests passed! 🎉");
            System.out.println("You have mastered TreeSet for interviews!");
        } else {
            System.out.println("\n⚠️  Keep practicing! Review the failed tests and try again.");
        }

        // ==================== QUICK REFERENCE CHEAT SHEET ====================
        System.out.println("\n" + "=".repeat(70));
        System.out.println("📋 TREESET INTERVIEW CHEAT SHEET");
        System.out.println("=".repeat(70));
        System.out.println("""
            
            COMPLEXITY:
              add/remove/contains  → O(log n)
              first/last           → O(log n)
              lower/floor/higher/ceiling → O(log n)
              headSet/tailSet/subSet     → O(log n) to create view
            
            NAVIGATION METHODS (MUST KNOW!):
              lower(e)   → greatest element STRICTLY LESS than e
              floor(e)   → greatest element LESS THAN OR EQUAL to e
              higher(e)  → smallest element STRICTLY GREATER than e
              ceiling(e) → smallest element GREATER THAN OR EQUAL to e
            
              first()     → smallest element (throws if empty)
              last()      → largest element (throws if empty)
              pollFirst() → removes & returns smallest (null if empty)
              pollLast()  → removes & returns largest (null if empty)
            
            RANGE VIEWS:
              headSet(to)          → elements < to
              headSet(to, true)    → elements ≤ to
              tailSet(from)        → elements ≥ from
              tailSet(from, false) → elements > from
              subSet(from, to)     → from ≤ elements < to
              subSet(from, fi, to, ti) → flexible inclusive/exclusive
            
            KEY GOTCHAS:
              ✗ No null elements allowed (NullPointerException)
              ✗ No index-based access (it's a tree, not a list!)
              ✗ Uses compareTo()/compare() for ordering AND equality
              ✗ Not thread-safe
              ✗ compareTo() returning 0 means "equal" — element won't be added
            
            WHEN TO USE TREESET:
              ✓ Need sorted unique elements
              ✓ Need range queries (elements between X and Y)
              ✓ Need floor/ceiling/lower/higher operations
              ✓ Need first/last (min/max) efficiently
            
            TREESET vs HASHSET vs LINKEDHASHSET:
              HashSet       → O(1) ops, unordered
              LinkedHashSet → O(1) ops, insertion-ordered
              TreeSet       → O(log n) ops, sorted order
            """);
    }

    // ==================== TEST HELPER METHODS ====================

    private static boolean testInteger(Integer actual, Integer expected, String testName) {
        if ((actual == null && expected == null) || (actual != null && actual.equals(expected))) {
            System.out.println("  ✓ PASS: " + testName + " => " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: " + expected + ", Actual: " + actual);
            return false;
        }
    }

    private static boolean testInt(int actual, int expected, String testName) {
        if (actual == expected) {
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

    private static boolean testTreeSet(TreeSet<Integer> actual, TreeSet<Integer> expected, String testName) {
        if (actual != null && actual.equals(expected)) {
            System.out.println("  ✓ PASS: " + testName + " => " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: " + expected + ", Actual: " + actual);
            return false;
        }
    }

    private static boolean testStringList(List<String> actual, List<String> expected, String testName) {
        if (actual != null && actual.equals(expected)) {
            System.out.println("  ✓ PASS: " + testName + " => " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: " + expected + ", Actual: " + actual);
            return false;
        }
    }

    private static boolean testIntList(List<Integer> actual, List<Integer> expected, String testName) {
        if (actual != null && actual.equals(expected)) {
            System.out.println("  ✓ PASS: " + testName + " => " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: " + expected + ", Actual: " + actual);
            return false;
        }
    }
}
