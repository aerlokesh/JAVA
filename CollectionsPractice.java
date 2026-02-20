/**
 * CollectionsPractice.java
 * 
 * A comprehensive Java practice file for mastering Collections Framework.
 * Complete all the TODO methods below and run the main method to test your solutions.
 * 
 * Instructions:
 * 1. Read each method's description carefully
 * 2. Replace the "TODO: Implement this method" with your solution
 * 3. Run: javac CollectionsPractice.java && java CollectionsPractice
 * 4. Check if all test cases pass
 */

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CollectionsPractice {
    
    // ==================== ARRAYLIST OPERATIONS ====================
    
    /**
     * Task 1: Create and return an ArrayList with given elements
     * @param elements Array of elements to add
     * @return ArrayList containing the elements
     */
    public static ArrayList<Integer> createArrayList(Integer[] elements) {
        ArrayList<Integer> al=new ArrayList<>();
        for(Integer x:elements) al.add(x);
        return al;
    }
    
    /**
     * Task 2: Add an element to the end of ArrayList
     * @param list The ArrayList
     * @param element Element to add
     * @return The modified ArrayList
     */
    public static ArrayList<Integer> addElement(ArrayList<Integer> list, Integer element) {
        list.add(element);
        return list;
    }
    
    /**
     * Task 3: Get element at specific index
     * @param list The ArrayList
     * @param index Index position
     * @return Element at the index
     */
    public static Integer getElement(ArrayList<Integer> list, int index) {
        return list.get(index);
    }
    
    /**
     * Task 4: Remove element at specific index
     * @param list The ArrayList
     * @param index Index to remove
     * @return The modified ArrayList
     */
    public static ArrayList<Integer> removeAtIndex(ArrayList<Integer> list, int index) {
        list.remove(index);
        return list;
    }
    
    /**
     * Task 5: Find if element exists in ArrayList
     * @param list The ArrayList
     * @param element Element to find
     * @return true if found, false otherwise
     */
    public static boolean containsElement(ArrayList<Integer> list, Integer element) {
        return list.contains(element);
    }
    
    // ==================== LINKEDLIST OPERATIONS ====================
    
    /**
     * Task 6: Create a LinkedList from array
     * @param elements Array of elements
     * @return LinkedList containing elements
     */
    public static LinkedList<String> createLinkedList(String[] elements) {
        LinkedList<String> ll=new LinkedList<>();
        Collections.addAll(ll, elements);
        return ll;
    }
    
    /**
     * Task 7: Add element to the beginning of LinkedList
     * @param list The LinkedList
     * @param element Element to add
     * @return The modified LinkedList
     */
    public static LinkedList<String> addFirst(LinkedList<String> list, String element) {
        list.addFirst(element);
        return list;
    }
    
    /**
     * Task 8: Add element to the end of LinkedList
     * @param list The LinkedList
     * @param element Element to add
     * @return The modified LinkedList
     */
    public static LinkedList<String> addLast(LinkedList<String> list, String element) {
        list.addLast(element);
        return list;
    }
    
    /**
     * Task 9: Remove and return first element
     * @param list The LinkedList
     * @return First element (or null if empty)
     */
    public static String removeFirst(LinkedList<String> list) {
        return list.removeFirst();
    }
    
    /**
     * Task 10: Remove and return last element
     * @param list The LinkedList
     * @return Last element (or null if empty)
     */
    public static String removeLast(LinkedList<String> list) {
        return list.removeLast();
    }
    
    // ==================== HASHSET OPERATIONS ====================
    
    /**
     * Task 11: Create HashSet from array
     * @param elements Array of elements
     * @return HashSet containing unique elements
     */
    public static HashSet<Integer> createHashSet(Integer[] elements) {
        HashSet hs=new HashSet<>();
        Collections.addAll(hs, elements);
        return hs;
    }
    
    /**
     * Task 12: Add element to HashSet
     * @param set The HashSet
     * @param element Element to add
     * @return true if added (wasn't present), false otherwise
     */
    public static boolean addToSet(HashSet<Integer> set, Integer element) {
        return set.add(element);
    }
    
    /**
     * Task 13: Remove element from HashSet
     * @param set The HashSet
     * @param element Element to remove
     * @return true if removed (was present), false otherwise
     */
    public static boolean removeFromSet(HashSet<Integer> set, Integer element) {
        return set.remove(element);
    }
    
    /**
     * Task 14: Find union of two sets
     * @param set1 First set
     * @param set2 Second set
     * @return New set containing all elements from both sets
     */
    public static HashSet<Integer> unionSets(HashSet<Integer> set1, HashSet<Integer> set2) {
        HashSet<Integer> result = new HashSet<>(set1);
        result.addAll(set2);
        return result;
    }
    
    /**
     * Task 15: Find intersection of two sets
     * @param set1 First set
     * @param set2 Second set
     * @return New set containing common elements
     */
    public static HashSet<Integer> intersectionSets(HashSet<Integer> set1, HashSet<Integer> set2) {
        HashSet<Integer> result = new HashSet<>(set1);
        result.retainAll(set2);
        return result;
    }
    
    // ==================== TREESET OPERATIONS ====================
    
    /**
     * Task 16: Create TreeSet (sorted) from array
     * @param elements Array of elements
     * @return TreeSet with elements in sorted order
     */
    public static TreeSet<Integer> createTreeSet(Integer[] elements) {
        TreeSet<Integer> ts=new TreeSet<>();
        for(Integer x:elements) ts.add(x);
        return ts;
    }
    
    /**
     * Task 17: Get first (smallest) element from TreeSet
     * @param set The TreeSet
     * @return First element (or null if empty)
     */
    public static Integer getFirst(TreeSet<Integer> set) {
        // TODO: t this method
        return set.getFirst();
    }
    
    /**
     * Task 18: Get last (largest) element from TreeSet
     * @param set The TreeSet
     * @return Last element (or null if empty)
     */
    public static Integer getLast(TreeSet<Integer> set) {
        // TODO: Implement this method
        return set.getLast();
    }
    
    /**
     * Task 19: Get elements less than given value
     * @param set The TreeSet
     * @param value The value
     * @return Set of elements less than value
     */
    public static TreeSet<Integer> headSet(TreeSet<Integer> set, Integer value) {
        return new TreeSet<>(set.headSet(value));
    }
    
    /**
     * Task 20: Get elements greater than or equal to given value
     * @param set The TreeSet
     * @param value The value
     * @return Set of elements >= value
     */
    public static TreeSet<Integer> tailSet(TreeSet<Integer> set, Integer value) {
        return new TreeSet<>(set.tailSet(value));
    }
    
    // ==================== HASHMAP OPERATIONS ====================
    
    /**
     * Task 21: Create HashMap from two arrays (keys and values)
     * @param keys Array of keys
     * @param values Array of values
     * @return HashMap with key-value pairs
     */
    public static HashMap<String, Integer> createHashMap(String[] keys, Integer[] values) {
        HashMap<String, Integer> hm = new HashMap<>();
        for (int i = 0; i < keys.length; i++) {
            hm.put(keys[i], values[i]);
        }
        return hm;
    }
    
    /**
     * Task 22: Put key-value pair in HashMap
     * @param map The HashMap
     * @param key The key
     * @param value The value
     * @return The previous value associated with key, or null
     */
    public static Integer putEntry(HashMap<String, Integer> map, String key, Integer value) {
        // TODO: Implement this method
        return map.put(key, value);
    }
    
    /**
     * Task 23: Get value by key from HashMap
     * @param map The HashMap
     * @param key The key
     * @return The value, or null if key not found
     */
    public static Integer getValue(HashMap<String, Integer> map, String key) {
        // TODO: Implement this method
        return map.get(key);
    }
    
    /**
     * Task 24: Check if HashMap contains key
     * @param map The HashMap
     * @param key The key
     * @return true if key exists
     */
    public static boolean containsKey(HashMap<String, Integer> map, String key) {
        // TODO: Implement this method
        return map.containsKey(key);
    }
    
    /**
     * Task 25: Get all keys from HashMap
     * @param map The HashMap
     * @return Set of all keys
     */
    public static Set<String> getKeys(HashMap<String, Integer> map) {
        return map.keySet();
    }
    
    /**
     * Task 26: Get all values from HashMap
     * @param map The HashMap
     * @return Collection of all values
     */
    public static Collection<Integer> getValues(HashMap<String, Integer> map) {
        // TODO: Implement this method
        return map.values();
    }
    
    // ==================== TREEMAP OPERATIONS ====================
    
    /**
     * Task 27: Create TreeMap (sorted by keys)
     * @param keys Array of keys
     * @param values Array of values
     * @return TreeMap with sorted keys
     */
    public static TreeMap<Integer, String> createTreeMap(Integer[] keys, String[] values) {
        TreeMap<Integer, String> tm = new TreeMap<>();
        for (int idx = 0; idx < keys.length; idx++) {
            tm.put(keys[idx], values[idx]);
        }
        return tm;
    }
    
    /**
     * Task 28: Get first (smallest) key from TreeMap
     * @param map The TreeMap
     * @return First key (or null if empty)
     */
    public static Integer firstKey(TreeMap<Integer, String> map) {
        return map.firstKey();
    }
    
    /**
     * Task 29: Get last (largest) key from TreeMap
     * @param map The TreeMap
     * @return Last key (or null if empty)
     */
    public static Integer lastKey(TreeMap<Integer, String> map) {
        // TODO: Implement this method
        return map.lastKey();
    }
    
    // ==================== QUEUE OPERATIONS ====================
    
    /**
     * Task 30: Create Queue (using LinkedList) from array
     * @param elements Array of elements
     * @return Queue with elements
     */
    public static Queue<String> createQueue(String[] elements) {
        Queue<String> q = new LinkedList<>();
        Collections.addAll(q, elements);
        return q;

    }
    
    /**
     * Task 31: Add element to queue
     * @param queue The Queue
     * @param element Element to add
     * @return true if added successfully
     */
    public static boolean enqueue(Queue<String> queue, String element) {
        queue.add(element);
        return true;
    }
    
    /**
     * Task 32: Remove and return front element from queue
     * @param queue The Queue
     * @return Front element (or null if empty)
     */
    public static String dequeue(Queue<String> queue) {
        String x =queue.remove();
        return x;
    }
    
    /**
     * Task 33: Peek at front element without removing
     * @param queue The Queue
     * @return Front element (or null if empty)
     */
    public static String peekQueue(Queue<String> queue) {
        String x=queue.peek();
        return x;
    }
    
    // ==================== STACK OPERATIONS ====================
    
    /**
     * Task 34: Create Stack from array
     * @param elements Array of elements
     * @return Stack with elements
     */
    public static Stack<Integer> createStack(Integer[] elements) {
        Stack<Integer> s=new Stack<>();
        for(int x:elements) s.add(x);
        return s;
    }
    
    /**
     * Task 35: Push element onto stack
     * @param stack The Stack
     * @param element Element to push
     * @return The pushed element
     */
    public static Integer push(Stack<Integer> stack, Integer element) {
        stack.add(element);
        return element;
    }
    
    /**
     * Task 36: Pop element from stack
     * @param stack The Stack
     * @return Popped element (or null if empty)
     */
    public static Integer pop(Stack<Integer> stack) {
        int x=stack.pop();
        return x;
    }
    
    /**
     * Task 37: Peek at top element without removing
     * @param stack The Stack
     * @return Top element (or null if empty)
     */
    public static Integer peekStack(Stack<Integer> stack) {
        int x=stack.peek();
        return x;
    }
    
    // ==================== COLLECTION UTILITIES ====================
    
    /**
     * Task 38: Sort ArrayList in ascending order
     * @param list The ArrayList
     * @return Sorted ArrayList
     */
    public static ArrayList<Integer> sortAscending(ArrayList<Integer> list) {
        Collections.sort(list);
        return list;
    }
    
    /**
     * Task 39: Sort ArrayList in descending order
     * @param list The ArrayList
     * @return Sorted ArrayList (descending)
     */
    public static ArrayList<Integer> sortDescending(ArrayList<Integer> list) {
        list.sort(Collections.reverseOrder());
        return list;
    }
    
    /**
     * Task 40: Reverse the order of elements in ArrayList
     * @param list The ArrayList
     * @return Reversed ArrayList
     */
    public static ArrayList<String> reverseList(ArrayList<String> list) {
        Collections.reverse(list);
        return list;
    }
    
    /**
     * Task 41: Find maximum element in collection
     * @param list The List
     * @return Maximum element
     */
    public static Integer findMax(List<Integer> list) {
        int x=Collections.max(list);
        return x;
    }
    
    /**
     * Task 42: Find minimum element in collection
     * @param list The List
     * @return Minimum element
     */
    public static Integer findMin(List<Integer> list) {
        int x=Collections.min(list);
        return x;
    }
    
    /**
     * Task 43: Shuffle elements randomly
     * @param list The ArrayList
     * @return Shuffled ArrayList
     */
    public static ArrayList<Integer> shuffleList(ArrayList<Integer> list) {
        Collections.shuffle(list);
        return list;
    }
    
    /**
     * Task 44: Find frequency of element in list
     * @param list The List
     * @param element Element to count
     * @return Frequency of element
     */
    public static int findFrequency(List<Integer> list, Integer element) {
        AtomicInteger ai=new AtomicInteger();
        list.forEach(x->{
            if(element.equals(x)){
                ai.addAndGet(1);
            }
        });
        return ai.get();
    }
    
    /**
     * Task 45: Binary search in sorted list
     * @param list Sorted ArrayList
     * @param element Element to find
     * @return Index of element (or negative if not found)
     */
    public static int binarySearch(ArrayList<Integer> list, Integer element) {
        int x=Collections.binarySearch(list, element);
        return x;
    }
    
    // ==================== ADVANCED OPERATIONS ====================
    
    /**
     * Task 46: Remove duplicates from ArrayList
     * @param list The ArrayList
     * @return New ArrayList without duplicates
     */
    public static ArrayList<Integer> removeDuplicates(ArrayList<Integer> list) {
        HashSet<Integer> hs=new HashSet<>();
        ArrayList<Integer> al=new ArrayList<>();
        for(int i=0;i<list.size();i++){
            if(!hs.contains(list.get(i))) al.add(list.get(i));
            hs.add(list.get(i));
        }
        return al;
    }
    
    /**
     * Task 47: Convert ArrayList to Array
     * @param list The ArrayList
     * @return Array containing list elements
     */
    public static Integer[] toArray(ArrayList<Integer> list) {
        return list.toArray(new Integer[0]);
    }
    
    /**
     * Task 48: Convert Array to ArrayList
     * @param array The array
     * @return ArrayList containing array elements
     */
    public static ArrayList<String> arrayToList(String[] array) {
        ArrayList<String> al=new ArrayList<>();
        Collections.addAll(al,array);
        return al;
    }
    
    /**
     * Task 49: Merge two ArrayLists
     * @param list1 First ArrayList
     * @param list2 Second ArrayList
     * @return New ArrayList with all elements
     */
    public static ArrayList<Integer> mergeLists(ArrayList<Integer> list1, ArrayList<Integer> list2) {
        ArrayList<Integer> result = new ArrayList<>(list1);
        result.addAll(list2);
        return result;
    }
    
    /**
     * Task 50: Count occurrences of each element (frequency map)
     * @param list The List
     * @return Map with element frequencies
     */
    public static Map<Integer, Integer> frequencyMap(List<Integer> list) {
        Map<Integer,Integer> m=new HashMap<>();
        for(int x:list) m.put(x, m.getOrDefault(x, 0)+1);
        return m;
    }
    
    // ==================== TEST CASES ====================
    
    public static void main(String[] args) {
        int totalTests = 0;
        int passedTests = 0;
        
        System.out.println("=".repeat(60));
        System.out.println("COLLECTIONS PRACTICE - TEST SUITE");
        System.out.println("=".repeat(60));
        
        // Test 1: createArrayList
        System.out.println("\n[Test 1] createArrayList");
        ArrayList<Integer> list1 = createArrayList(new Integer[]{1, 2, 3});
        totalTests++; if (testList(list1, Arrays.asList(1, 2, 3), "Create ArrayList")) passedTests++;
        
        // Test 2: addElement
        System.out.println("\n[Test 2] addElement");
        ArrayList<Integer> list2 = new ArrayList<>(Arrays.asList(1, 2));
        addElement(list2, 3);
        totalTests++; if (testList(list2, Arrays.asList(1, 2, 3), "Add element")) passedTests++;
        
        // Test 3: getElement
        System.out.println("\n[Test 3] getElement");
        ArrayList<Integer> list3 = new ArrayList<>(Arrays.asList(10, 20, 30));
        totalTests++; if (testInteger(getElement(list3, 1), 20, "Get element at index 1")) passedTests++;
        
        // Test 4: removeAtIndex
        System.out.println("\n[Test 4] removeAtIndex");
        ArrayList<Integer> list4 = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        removeAtIndex(list4, 2);
        totalTests++; if (testList(list4, Arrays.asList(1, 2, 4), "Remove at index 2")) passedTests++;
        
        // Test 5: containsElement
        System.out.println("\n[Test 5] containsElement");
        ArrayList<Integer> list5 = new ArrayList<>(Arrays.asList(1, 2, 3));
        totalTests++; if (testBoolean(containsElement(list5, 2), true, "Contains 2")) passedTests++;
        totalTests++; if (testBoolean(containsElement(list5, 5), false, "Does not contain 5")) passedTests++;
        
        // Test 6: createLinkedList
        System.out.println("\n[Test 6] createLinkedList");
        LinkedList<String> linkedList1 = createLinkedList(new String[]{"a", "b", "c"});
        totalTests++; if (testLinkedList(linkedList1, Arrays.asList("a", "b", "c"), "Create LinkedList")) passedTests++;
        
        // Test 7: addFirst
        System.out.println("\n[Test 7] addFirst");
        LinkedList<String> linkedList2 = new LinkedList<>(Arrays.asList("b", "c"));
        addFirst(linkedList2, "a");
        totalTests++; if (testLinkedList(linkedList2, Arrays.asList("a", "b", "c"), "Add first")) passedTests++;
        
        // Test 8: addLast
        System.out.println("\n[Test 8] addLast");
        LinkedList<String> linkedList3 = new LinkedList<>(Arrays.asList("a", "b"));
        addLast(linkedList3, "c");
        totalTests++; if (testLinkedList(linkedList3, Arrays.asList("a", "b", "c"), "Add last")) passedTests++;
        
        // Test 9: removeFirst
        System.out.println("\n[Test 9] removeFirst");
        LinkedList<String> linkedList4 = new LinkedList<>(Arrays.asList("a", "b", "c"));
        String first = removeFirst(linkedList4);
        totalTests++; if (testString(first, "a", "Remove first returns 'a'")) passedTests++;
        totalTests++; if (testLinkedList(linkedList4, Arrays.asList("b", "c"), "List after removeFirst")) passedTests++;
        
        // Test 10: removeLast
        System.out.println("\n[Test 10] removeLast");
        LinkedList<String> linkedList5 = new LinkedList<>(Arrays.asList("a", "b", "c"));
        String last = removeLast(linkedList5);
        totalTests++; if (testString(last, "c", "Remove last returns 'c'")) passedTests++;
        totalTests++; if (testLinkedList(linkedList5, Arrays.asList("a", "b"), "List after removeLast")) passedTests++;
        
        // Test 11: createHashSet
        System.out.println("\n[Test 11] createHashSet");
        HashSet<Integer> set1 = createHashSet(new Integer[]{1, 2, 3, 2, 1});
        totalTests++; if (testSet(set1, new HashSet<>(Arrays.asList(1, 2, 3)), "Create HashSet")) passedTests++;
        
        // Test 12: addToSet
        System.out.println("\n[Test 12] addToSet");
        HashSet<Integer> set2 = new HashSet<>(Arrays.asList(1, 2));
        totalTests++; if (testBoolean(addToSet(set2, 3), true, "Add new element")) passedTests++;
        totalTests++; if (testBoolean(addToSet(set2, 2), false, "Add existing element")) passedTests++;
        
        // Test 13: removeFromSet
        System.out.println("\n[Test 13] removeFromSet");
        HashSet<Integer> set3 = new HashSet<>(Arrays.asList(1, 2, 3));
        totalTests++; if (testBoolean(removeFromSet(set3, 2), true, "Remove existing element")) passedTests++;
        totalTests++; if (testBoolean(removeFromSet(set3, 5), false, "Remove non-existing element")) passedTests++;
        
        // Test 14: unionSets
        System.out.println("\n[Test 14] unionSets");
        HashSet<Integer> setA = new HashSet<>(Arrays.asList(1, 2, 3));
        HashSet<Integer> setB = new HashSet<>(Arrays.asList(3, 4, 5));
        HashSet<Integer> union = unionSets(setA, setB);
        totalTests++; if (testSet(union, new HashSet<>(Arrays.asList(1, 2, 3, 4, 5)), "Union of sets")) passedTests++;
        
        // Test 15: intersectionSets
        System.out.println("\n[Test 15] intersectionSets");
        HashSet<Integer> setC = new HashSet<>(Arrays.asList(1, 2, 3));
        HashSet<Integer> setD = new HashSet<>(Arrays.asList(2, 3, 4));
        HashSet<Integer> intersection = intersectionSets(setC, setD);
        totalTests++; if (testSet(intersection, new HashSet<>(Arrays.asList(2, 3)), "Intersection of sets")) passedTests++;
        
        // Test 16: createTreeSet
        System.out.println("\n[Test 16] createTreeSet");
        TreeSet<Integer> treeSet1 = createTreeSet(new Integer[]{3, 1, 4, 1, 5, 9, 2});
        totalTests++; if (testTreeSet(treeSet1, new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5, 9)), "Create TreeSet")) passedTests++;
        
        // Test 17: getFirst
        System.out.println("\n[Test 17] getFirst TreeSet");
        TreeSet<Integer> treeSet2 = new TreeSet<>(Arrays.asList(5, 2, 8, 1));
        totalTests++; if (testInteger(getFirst(treeSet2), 1, "Get first element")) passedTests++;
        
        // Test 18: getLast
        System.out.println("\n[Test 18] getLast TreeSet");
        TreeSet<Integer> treeSet3 = new TreeSet<>(Arrays.asList(5, 2, 8, 1));
        totalTests++; if (testInteger(getLast(treeSet3), 8, "Get last element")) passedTests++;
        
        // Test 19: headSet
        System.out.println("\n[Test 19] headSet");
        TreeSet<Integer> treeSet4 = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5));
        TreeSet<Integer> head = headSet(treeSet4, 4);
        totalTests++; if (testTreeSet(head, new TreeSet<>(Arrays.asList(1, 2, 3)), "HeadSet < 4")) passedTests++;
        
        // Test 20: tailSet
        System.out.println("\n[Test 20] tailSet");
        TreeSet<Integer> treeSet5 = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5));
        TreeSet<Integer> tail = tailSet(treeSet5, 3);
        totalTests++; if (testTreeSet(tail, new TreeSet<>(Arrays.asList(3, 4, 5)), "TailSet >= 3")) passedTests++;
        
        // Test 21: createHashMap
        System.out.println("\n[Test 21] createHashMap");
        HashMap<String, Integer> map1 = createHashMap(new String[]{"a", "b", "c"}, new Integer[]{1, 2, 3});
        HashMap<String, Integer> expected1 = new HashMap<>();
        expected1.put("a", 1); expected1.put("b", 2); expected1.put("c", 3);
        totalTests++; if (testMap(map1, expected1, "Create HashMap")) passedTests++;
        
        // Test 22: putEntry
        System.out.println("\n[Test 22] putEntry");
        HashMap<String, Integer> map2 = new HashMap<>();
        map2.put("a", 1);
        totalTests++; if (testInteger(putEntry(map2, "b", 2), null, "Put new entry")) passedTests++;
        totalTests++; if (testInteger(putEntry(map2, "a", 10), 1, "Put existing key")) passedTests++;
        
        // Test 23: getValue
        System.out.println("\n[Test 23] getValue");
        HashMap<String, Integer> map3 = new HashMap<>();
        map3.put("key", 100);
        totalTests++; if (testInteger(getValue(map3, "key"), 100, "Get existing key")) passedTests++;
        totalTests++; if (testInteger(getValue(map3, "missing"), null, "Get missing key")) passedTests++;
        
        // Test 24: containsKey
        System.out.println("\n[Test 24] containsKey");
        HashMap<String, Integer> map4 = new HashMap<>();
        map4.put("exists", 1);
        totalTests++; if (testBoolean(containsKey(map4, "exists"), true, "Contains key")) passedTests++;
        totalTests++; if (testBoolean(containsKey(map4, "missing"), false, "Does not contain key")) passedTests++;
        
        // Test 25: getKeys
        System.out.println("\n[Test 25] getKeys");
        HashMap<String, Integer> map5 = new HashMap<>();
        map5.put("a", 1); map5.put("b", 2);
        Set<String> keys = getKeys(map5);
        totalTests++; if (testSet(keys, new HashSet<>(Arrays.asList("a", "b")), "Get keys")) passedTests++;
        
        // Test 26: getValues
        System.out.println("\n[Test 26] getValues");
        HashMap<String, Integer> map6 = new HashMap<>();
        map6.put("a", 1); map6.put("b", 2);
        Collection<Integer> values = getValues(map6);
        totalTests++; if (testCollection(values, Arrays.asList(1, 2), "Get values")) passedTests++;
        
        // Test 27: createTreeMap
        System.out.println("\n[Test 27] createTreeMap");
        TreeMap<Integer, String> treeMap1 = createTreeMap(new Integer[]{3, 1, 2}, new String[]{"c", "a", "b"});
        totalTests++; if (testInteger(treeMap1 != null ? treeMap1.firstKey() : null, 1, "TreeMap first key is 1")) passedTests++;
        
        // Test 28: firstKey
        System.out.println("\n[Test 28] firstKey TreeMap");
        TreeMap<Integer, String> treeMap2 = new TreeMap<>();
        treeMap2.put(5, "e"); treeMap2.put(2, "b"); treeMap2.put(8, "h");
        totalTests++; if (testInteger(firstKey(treeMap2), 2, "First key")) passedTests++;
        
        // Test 29: lastKey
        System.out.println("\n[Test 29] lastKey TreeMap");
        TreeMap<Integer, String> treeMap3 = new TreeMap<>();
        treeMap3.put(5, "e"); treeMap3.put(2, "b"); treeMap3.put(8, "h");
        totalTests++; if (testInteger(lastKey(treeMap3), 8, "Last key")) passedTests++;
        
        // Test 30: createQueue
        System.out.println("\n[Test 30] createQueue");
        Queue<String> queue1 = createQueue(new String[]{"a", "b", "c"});
        totalTests++; if (testQueue(queue1, Arrays.asList("a", "b", "c"), "Create Queue")) passedTests++;
        
        // Test 31: enqueue
        System.out.println("\n[Test 31] enqueue");
        Queue<String> queue2 = new LinkedList<>(Arrays.asList("a", "b"));
        totalTests++; if (testBoolean(enqueue(queue2, "c"), true, "Enqueue element")) passedTests++;
        
        // Test 32: dequeue
        System.out.println("\n[Test 32] dequeue");
        Queue<String> queue3 = new LinkedList<>(Arrays.asList("a", "b", "c"));
        String dequeued = dequeue(queue3);
        totalTests++; if (testString(dequeued, "a", "Dequeue returns 'a'")) passedTests++;
        
        // Test 33: peekQueue
        System.out.println("\n[Test 33] peekQueue");
        Queue<String> queue4 = new LinkedList<>(Arrays.asList("x", "y", "z"));
        totalTests++; if (testString(peekQueue(queue4), "x", "Peek queue")) passedTests++;
        
        // Test 34: createStack
        System.out.println("\n[Test 34] createStack");
        Stack<Integer> stack1 = createStack(new Integer[]{1, 2, 3});
        totalTests++; if (testStack(stack1, Arrays.asList(1, 2, 3), "Create Stack")) passedTests++;
        
        // Test 35: push
        System.out.println("\n[Test 35] push");
        Stack<Integer> stack2 = new Stack<>();
        stack2.push(1);
        totalTests++; if (testInteger(push(stack2, 2), 2, "Push returns element")) passedTests++;
        
        // Test 36: pop
        System.out.println("\n[Test 36] pop");
        Stack<Integer> stack3 = new Stack<>();
        stack3.push(1); stack3.push(2);
        totalTests++; if (testInteger(pop(stack3), 2, "Pop returns top")) passedTests++;
        
        // Test 37: peekStack
        System.out.println("\n[Test 37] peekStack");
        Stack<Integer> stack4 = new Stack<>();
        stack4.push(5);
        totalTests++; if (testInteger(peekStack(stack4), 5, "Peek stack")) passedTests++;
        
        // Test 38: sortAscending
        System.out.println("\n[Test 38] sortAscending");
        ArrayList<Integer> unsorted1 = new ArrayList<>(Arrays.asList(3, 1, 4, 1, 5));
        ArrayList<Integer> sorted1 = sortAscending(new ArrayList<>(unsorted1));
        totalTests++; if (testList(sorted1, Arrays.asList(1, 1, 3, 4, 5), "Sort ascending")) passedTests++;
        
        // Test 39: sortDescending
        System.out.println("\n[Test 39] sortDescending");
        ArrayList<Integer> unsorted2 = new ArrayList<>(Arrays.asList(3, 1, 4, 1, 5));
        ArrayList<Integer> sorted2 = sortDescending(new ArrayList<>(unsorted2));
        totalTests++; if (testList(sorted2, Arrays.asList(5, 4, 3, 1, 1), "Sort descending")) passedTests++;
        
        // Test 40: reverseList
        System.out.println("\n[Test 40] reverseList");
        ArrayList<String> strList = new ArrayList<>(Arrays.asList("a", "b", "c"));
        ArrayList<String> reversed = reverseList(new ArrayList<>(strList));
        totalTests++; if (testStringList(reversed, Arrays.asList("c", "b", "a"), "Reverse list")) passedTests++;
        
        // Test 41: findMax
        System.out.println("\n[Test 41] findMax");
        List<Integer> maxList = Arrays.asList(3, 7, 2, 9, 1);
        totalTests++; if (testInteger(findMax(maxList), 9, "Find max")) passedTests++;
        
        // Test 42: findMin
        System.out.println("\n[Test 42] findMin");
        List<Integer> minList = Arrays.asList(3, 7, 2, 9, 1);
        totalTests++; if (testInteger(findMin(minList), 1, "Find min")) passedTests++;
        
        // Test 43: shuffleList
        System.out.println("\n[Test 43] shuffleList");
        ArrayList<Integer> toShuffle = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        ArrayList<Integer> shuffled = shuffleList(new ArrayList<>(toShuffle));
        totalTests++; if (testBoolean(shuffled != null && shuffled.size() == 5, true, "Shuffle maintains size")) passedTests++;
        
        // Test 44: findFrequency
        System.out.println("\n[Test 44] findFrequency");
        List<Integer> freqList = Arrays.asList(1, 2, 2, 3, 2, 4);
        totalTests++; if (testInt(findFrequency(freqList, 2), 3, "Frequency of 2")) passedTests++;
        
        // Test 45: binarySearch
        System.out.println("\n[Test 45] binarySearch");
        ArrayList<Integer> searchList = new ArrayList<>(Arrays.asList(1, 3, 5, 7, 9));
        totalTests++; if (testInt(binarySearch(searchList, 5), 2, "Binary search finds element")) passedTests++;
        
        // Test 46: removeDuplicates
        System.out.println("\n[Test 46] removeDuplicates");
        ArrayList<Integer> withDups = new ArrayList<>(Arrays.asList(1, 2, 2, 3, 1, 4));
        ArrayList<Integer> noDups = removeDuplicates(withDups);
        totalTests++; if (testBoolean(noDups != null && noDups.size() == 4, true, "Remove duplicates")) passedTests++;
        
        // Test 47: toArray
        System.out.println("\n[Test 47] toArray");
        ArrayList<Integer> listToArray = new ArrayList<>(Arrays.asList(1, 2, 3));
        Integer[] array = toArray(listToArray);
        totalTests++; if (testArray(array, new Integer[]{1, 2, 3}, "List to array")) passedTests++;
        
        // Test 48: arrayToList
        System.out.println("\n[Test 48] arrayToList");
        String[] strArray = {"x", "y", "z"};
        ArrayList<String> arrayList = arrayToList(strArray);
        totalTests++; if (testStringList(arrayList, Arrays.asList("x", "y", "z"), "Array to list")) passedTests++;
        
        // Test 49: mergeLists
        System.out.println("\n[Test 49] mergeLists");
        ArrayList<Integer> listA = new ArrayList<>(Arrays.asList(1, 2));
        ArrayList<Integer> listB = new ArrayList<>(Arrays.asList(3, 4));
        ArrayList<Integer> merged = mergeLists(listA, listB);
        totalTests++; if (testList(merged, Arrays.asList(1, 2, 3, 4), "Merge lists")) passedTests++;
        
        // Test 50: frequencyMap
        System.out.println("\n[Test 50] frequencyMap");
        List<Integer> freqMapList = Arrays.asList(1, 2, 2, 3, 3, 3);
        Map<Integer, Integer> freqMap = frequencyMap(freqMapList);
        totalTests++; if (testBoolean(freqMap != null && freqMap.get(3) != null && freqMap.get(3) == 3, true, "Frequency map")) passedTests++;
        
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
            System.out.println("You have successfully mastered Java Collections Framework!");
        } else {
            System.out.println("\n⚠️  Keep practicing! Review the failed tests and try again.");
        }
    }
    
    // ==================== TEST HELPER METHODS ====================
    
    private static boolean testInteger(Integer actual, Integer expected, String testName) {
        if ((actual == null && expected == null) || (actual != null && actual.equals(expected))) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected Output: " + expected + ", Actual Output: " + actual);
            return false;
        }
    }
    
    private static boolean testInt(int actual, int expected, String testName) {
        if (actual == expected) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected Output: " + expected + ", Actual Output: " + actual);
            return false;
        }
    }
    
    private static boolean testString(String actual, String expected, String testName) {
        if ((actual == null && expected == null) || (actual != null && actual.equals(expected))) {
            System.out.println("  ✓ PASS: " + testName + " => Output: \"" + actual + "\"");
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected Output: \"" + expected + "\", Actual Output: \"" + actual + "\"");
            return false;
        }
    }
    
    private static boolean testBoolean(boolean actual, boolean expected, String testName) {
        if (actual == expected) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected Output: " + expected + ", Actual Output: " + actual);
            return false;
        }
    }
    
    private static boolean testList(ArrayList<Integer> actual, List<Integer> expected, String testName) {
        if (actual != null && actual.equals(expected)) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected Output: " + expected + ", Actual Output: " + actual);
            return false;
        }
    }
    
    private static boolean testStringList(ArrayList<String> actual, List<String> expected, String testName) {
        if (actual != null && actual.equals(expected)) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected Output: " + expected + ", Actual Output: " + actual);
            return false;
        }
    }
    
    private static boolean testLinkedList(LinkedList<String> actual, List<String> expected, String testName) {
        if (actual != null && actual.equals(expected)) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected Output: " + expected + ", Actual Output: " + actual);
            return false;
        }
    }
    
    private static <T> boolean testSet(Set<T> actual, Set<T> expected, String testName) {
        if (actual != null && actual.equals(expected)) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected Output: " + expected + ", Actual Output: " + actual);
            return false;
        }
    }
    
    private static boolean testTreeSet(TreeSet<Integer> actual, TreeSet<Integer> expected, String testName) {
        if (actual != null && actual.equals(expected)) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected Output: " + expected + ", Actual Output: " + actual);
            return false;
        }
    }
    
    private static <K, V> boolean testMap(Map<K, V> actual, Map<K, V> expected, String testName) {
        if (actual != null && actual.equals(expected)) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected Output: " + expected + ", Actual Output: " + actual);
            return false;
        }
    }
    
    private static <T> boolean testCollection(Collection<T> actual, List<T> expected, String testName) {
        if (actual != null && new ArrayList<>(actual).containsAll(expected) && actual.size() == expected.size()) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected Output: " + expected + ", Actual Output: " + actual);
            return false;
        }
    }
    
    private static boolean testQueue(Queue<String> actual, List<String> expected, String testName) {
        if (actual != null && new ArrayList<>(actual).equals(expected)) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected Output: " + expected + ", Actual Output: " + actual);
            return false;
        }
    }
    
    private static boolean testStack(Stack<Integer> actual, List<Integer> expected, String testName) {
        if (actual != null && actual.equals(expected)) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected Output: " + expected + ", Actual Output: " + actual);
            return false;
        }
    }
    
    private static boolean testArray(Integer[] actual, Integer[] expected, String testName) {
        if (actual != null && Arrays.equals(actual, expected)) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + Arrays.toString(actual));
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected Output: " + Arrays.toString(expected) + ", Actual Output: " + Arrays.toString(actual));
            return false;
        }
    }
}
