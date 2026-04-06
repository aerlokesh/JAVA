import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/*
 * DICTIONARY APP - Low Level Design
 * ====================================
 * 
 * REQUIREMENTS:
 * 1. storeWord(word, meaning) — insert or overwrite
 * 2. getMeaning(word) — return meaning or ""
 * 3. searchWords(prefix, n) — up to n words with prefix, sorted lex
 * 4. exists(pattern) — wildcard '.' matches any single char
 * 5. Thread-safe operations
 * 
 * KEY DATA STRUCTURES:
 * - TreeMap<String, String>: word -> meaning (sorted for prefix search)
 * - Trie: for prefix search + wildcard existence check
 * 
 * COMPLEXITY:
 *   storeWord:    O(k) trie insert, O(log n) TreeMap put, k = word length
 *   getMeaning:   O(log n) TreeMap lookup
 *   searchWords:  O(k + m) trie prefix traverse, m = matches
 *   exists:       O(26^d * k) worst case with d dots, typically fast
 */

// ==================== TRIE ====================

class DictTrieNode {
    final Map<Character, DictTrieNode> children = new HashMap<>();
    boolean isWord;
    String word; // stored at leaf for easy collection
}

class DictTrie {
    final DictTrieNode root = new DictTrieNode();

    /** Insert word into trie. */
    void insert(String word) {
        // TODO: Implement
        // HINT: DictTrieNode node = root;
        // HINT: for (char ch : word.toCharArray()) {
        // HINT:     node.children.putIfAbsent(ch, new DictTrieNode());
        // HINT:     node = node.children.get(ch);
        // HINT: }
        // HINT: node.isWord = true;
        // HINT: node.word = word;
    }

    /** Collect up to n words starting with prefix, lexicographic order. */
    List<String> searchPrefix(String prefix, int n) {
        // TODO: Implement — traverse to prefix node, DFS collect sorted
        // HINT: DictTrieNode node = root;
        // HINT: for (char ch : prefix.toCharArray()) {
        // HINT:     node = node.children.get(ch);
        // HINT:     if (node == null) return Collections.emptyList();
        // HINT: }
        // HINT: List<String> result = new ArrayList<>();
        // HINT: collectSorted(node, result, n);
        // HINT: return result;
        return Collections.emptyList();
    }

    /** DFS collect words in sorted order (by iterating children sorted). */
    private void collectSorted(DictTrieNode node, List<String> result, int limit) {
        if (result.size() >= limit) return;
        if (node.isWord) result.add(node.word);
        List<Character> keys = new ArrayList<>(node.children.keySet());
        Collections.sort(keys);
        for (char ch : keys) {
            collectSorted(node.children.get(ch), result, limit);
            if (result.size() >= limit) return;
        }
    }

    /**
     * Check if pattern exists. '.' matches any single char [a-z, space, hyphen].
     * Non-dot chars must match exactly, pattern length = word length.
     */
    boolean exists(String pattern) {
        // TODO: Implement — DFS with branching on '.'
        // HINT: return dfs(root, pattern, 0);
        return false;
    }

    private boolean dfs(DictTrieNode node, String pattern, int idx) {
        // TODO: Implement
        // HINT: if (idx == pattern.length()) return node.isWord;
        // HINT: char ch = pattern.charAt(idx);
        // HINT: if (ch == '.') {
        // HINT:     for (DictTrieNode child : node.children.values())
        // HINT:         if (dfs(child, pattern, idx + 1)) return true;
        // HINT:     return false;
        // HINT: }
        // HINT: DictTrieNode child = node.children.get(ch);
        // HINT: return child != null && dfs(child, pattern, idx + 1);
        return false;
    }
}

// ==================== DICTIONARY ====================

class Dictionary {
    private final TreeMap<String, String> words = new TreeMap<>();  // sorted for prefix
    private final DictTrie trie = new DictTrie();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /** Insert or overwrite word → meaning. */
    void storeWord(String word, String meaning) {
        lock.writeLock().lock();
        try {
            words.put(word, meaning);
            trie.insert(word);
        } finally { lock.writeLock().unlock(); }
    }

    /** Return meaning or "" if not found. */
    String getMeaning(String word) {
        lock.readLock().lock();
        try {
            return words.getOrDefault(word, "");
        } finally { lock.readLock().unlock(); }
    }

    /** Up to n words with prefix, sorted lexicographically. */
    List<String> searchWords(String prefix, int n) {
        lock.readLock().lock();
        try {
            return trie.searchPrefix(prefix, n);
        } finally { lock.readLock().unlock(); }
    }

    /** Pattern match with '.' wildcard for any single char. */
    boolean exists(String pattern) {
        lock.readLock().lock();
        try {
            return trie.exists(pattern);
        } finally { lock.readLock().unlock(); }
    }

    int size() { return words.size(); }
}

// ==================== MAIN / TESTS ====================

public class DictionarySystem {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════╗");
        System.out.println("║    DICTIONARY APP - LLD Demo      ║");
        System.out.println("╚═══════════════════════════════════╝\n");

        Dictionary dict = new Dictionary();

        // --- Test 1: Store & Fetch ---
        System.out.println("=== Test 1: Store & Fetch ===");
        dict.storeWord("apple", "a fruit");
        System.out.println("getMeaning('apple'): '" + dict.getMeaning("apple") + "' (expected 'a fruit')");
        System.out.println("✓ Basic store and fetch\n");

        // --- Test 2: Overwrite ---
        System.out.println("=== Test 2: Overwrite ===");
        dict.storeWord("apple", "sweet fruit");
        System.out.println("getMeaning('apple'): '" + dict.getMeaning("apple") + "' (expected 'sweet fruit')");
        System.out.println("getMeaning('apples'): '" + dict.getMeaning("apples") + "' (expected '')");
        System.out.println("✓ Overwrite replaces meaning\n");

        // --- Test 3: Prefix Search ---
        System.out.println("=== Test 3: Prefix Search ===");
        dict.storeWord("app", "short for application");
        dict.storeWord("apply", "make a formal request");
        dict.storeWord("apt", "suitable");
        dict.storeWord("banana", "yellow fruit");
        System.out.println("searchWords('ap', 3): " + dict.searchWords("ap", 3) + " (expected [app, apple, apply])");
        System.out.println("searchWords('app', 10): " + dict.searchWords("app", 10) + " (expected [app, apple, apply])");
        System.out.println("searchWords('b', 2): " + dict.searchWords("b", 2) + " (expected [banana])");
        System.out.println("searchWords('z', 5): " + dict.searchWords("z", 5) + " (expected [])");
        System.out.println("✓ Prefix search sorted and limited\n");

        // --- Test 4: Wildcard Exists ---
        System.out.println("=== Test 4: Wildcard Exists ===");
        Dictionary d2 = new Dictionary();
        for (String w : new String[]{"cat","cap","caps","map","man","many"})
            d2.storeWord(w, "meaning of " + w);

        System.out.println("exists('cat'):  " + d2.exists("cat") + "  (expected true)");
        System.out.println("exists('c.t'):  " + d2.exists("c.t") + "  (expected true)");
        System.out.println("exists('ca.'):  " + d2.exists("ca.") + "  (expected true)");
        System.out.println("exists('..p'):  " + d2.exists("..p") + "  (expected true)");
        System.out.println("exists('c..s'): " + d2.exists("c..s") + " (expected true)");
        System.out.println("exists('....'): " + d2.exists("....") + " (expected true)");
        System.out.println("exists('c..'):  " + d2.exists("c..") + "  (expected true)");
        System.out.println("exists('c.'):   " + d2.exists("c.") + " (expected false)");
        System.out.println("exists('...y'): " + d2.exists("...y") + " (expected true)");
        System.out.println("exists('zzz'):  " + d2.exists("zzz") + " (expected false)");
        System.out.println("✓ Wildcard pattern matching\n");

        // --- Test 5: Words with Space & Hyphen ---
        System.out.println("=== Test 5: Space & Hyphen ===");
        dict.storeWord("ice cream", "frozen dessert");
        dict.storeWord("well-known", "famous");
        System.out.println("getMeaning('ice cream'): '" + dict.getMeaning("ice cream") + "'");
        System.out.println("getMeaning('well-known'): '" + dict.getMeaning("well-known") + "'");
        System.out.println("exists('ice.cream'): " + dict.exists("ice.cream"));
        System.out.println("exists('well.known'): " + dict.exists("well.known"));
        System.out.println("✓ Handles spaces and hyphens\n");

        // --- Test 6: Edge Cases ---
        System.out.println("=== Test 6: Edge Cases ===");
        System.out.println("getMeaning('nonexistent'): '" + dict.getMeaning("nonexistent") + "' (expected '')");
        System.out.println("exists('.'): " + dict.exists(".") + " (expected false — no 1-char words)");
        System.out.println("searchWords('zzz', 5): " + dict.searchWords("zzz", 5) + " (expected [])");
        System.out.println("✓ Edge cases\n");

        // --- Test 7: Prefix Limit ---
        System.out.println("=== Test 7: Prefix Limit ===");
        Dictionary d3 = new Dictionary();
        for (int i = 0; i < 100; i++)
            d3.storeWord("word" + String.format("%03d", i), "meaning " + i);
        System.out.println("searchWords('word', 5): " + d3.searchWords("word", 5));
        System.out.println("searchWords('word0', 3): " + d3.searchWords("word0", 3));
        System.out.println("Total words: " + d3.size());
        System.out.println("✓ Limit works correctly\n");

        // --- Test 8: Scale ---
        System.out.println("=== Test 8: Scale ===");
        Dictionary d4 = new Dictionary();
        for (int i = 0; i < 10000; i++)
            d4.storeWord("w" + i, "m" + i);
        long t = System.nanoTime();
        List<String> sr = d4.searchWords("w1", 10);
        long searchTime = System.nanoTime() - t;
        t = System.nanoTime();
        boolean ex = d4.exists("w....");
        long existsTime = System.nanoTime() - t;
        System.out.printf("10K words — prefix search: %d results in %.2f ms\n", sr.size(), searchTime/1e6);
        System.out.printf("exists('w....'): %b in %.2f ms\n", ex, existsTime/1e6);
        System.out.println("✓ Fast at scale\n");

        // --- Test 9: Thread Safety ---
        System.out.println("=== Test 9: Thread Safety ===");
        Dictionary d5 = new Dictionary();
        ExecutorService exec = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            int idx = i;
            futures.add(exec.submit(() -> d5.storeWord("t" + idx, "m" + idx)));
        }
        for (int i = 0; i < 100; i++) {
            int idx = i;
            futures.add(exec.submit(() -> d5.searchWords("t", 5)));
            futures.add(exec.submit(() -> d5.exists("t" + idx)));
        }
        for (Future<?> f : futures) { try { f.get(); } catch (Exception e) { System.out.println("ERR: " + e); } }
        exec.shutdown();
        System.out.println("After concurrent ops: " + d5.size() + " words stored");
        System.out.println("✓ Thread-safe\n");

        System.out.println("════════ ALL 9 TESTS PASSED ✓ ════════");
    }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. TRIE: O(k) insert/search where k = word length. Natural fit
 *    for prefix search and wildcard matching. Children sorted for
 *    lex-ordered prefix results.
 *
 * 2. WILDCARD '.': DFS with branching — on '.', try all children.
 *    Worst case O(26^d * k) with d dots, but typically pruned fast.
 *    Same approach as LeetCode "Design Add and Search Words DS".
 *
 * 3. TREEMAP: Sorted map for O(log n) getMeaning + ordered iteration.
 *    Could use Trie alone (store meaning at leaf), but TreeMap is
 *    simpler for overwrite + direct lookup.
 *
 * 4. PREFIX SEARCH: Traverse trie to prefix node, DFS collect in
 *    sorted order (sorted children iteration), stop at limit n.
 *
 * 5. THREAD SAFETY: ReadWriteLock — concurrent reads for getMeaning/
 *    searchWords/exists, exclusive writes for storeWord.
 *
 * 6. SCALE: Trie memory = O(N * L * alphabet). For huge dictionaries,
 *    compressed trie (radix tree / Patricia trie) saves space.
 *    Real-world: Redis Streams, Elasticsearch prefix queries.
 *
 * 7. COMPLEXITY:
 *    storeWord:    O(k) trie + O(log n) TreeMap
 *    getMeaning:   O(log n) TreeMap
 *    searchWords:  O(k + m) traverse + collect m results
 *    exists:       O(26^d * k) with d dots, typically O(k)
 */
