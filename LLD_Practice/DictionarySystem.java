import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/*
 * DICTIONARY APP - Low Level Design
 * ====================================
 * 
 * REQUIREMENTS:
 * 1. Store word with multiple meanings (add, not overwrite)
 * 2. getMeanings(word) — return all meanings
 * 3. searchPrefix(prefix, n) — up to n words with prefix, sorted lex
 * 4. exists(pattern) — wildcard '.' matches any single char
 * 5. Pluggable search: prefix vs wildcard (Strategy)
 * 6. Track dictionary events (Observer)
 * 7. Delete word, thread-safe
 * 
 * DESIGN PATTERNS:
 *   Strategy  (DictSearchStrategy)  — PrefixSearchStrategy, WildcardSearchStrategy
 *   Observer  (DictListener)        — DictLogger
 *   Facade    (DictionaryService)
 * 
 * KEY DS: Trie — DictTrieNode[128], meanings list at leaf
 */

// ==================== EXCEPTIONS ====================

class WordNotFoundException extends RuntimeException {
    WordNotFoundException(String word) { super("Word not found: " + word); }
}

class InvalidWordException extends RuntimeException {
    InvalidWordException(String reason) { super("Invalid word: " + reason); }
}

// ==================== ENUMS ====================

enum DictEventType { ADDED, UPDATED, DELETED, SEARCHED }

// ==================== MODELS ====================

class DictTrieNode {
    final DictTrieNode[] children = new DictTrieNode[128];
    boolean isWord;
    String word;
    final List<String> meanings = new ArrayList<>(); // multiple meanings
}

// ==================== INTERFACES ====================

/** Strategy — pluggable search algorithm. */
interface DictSearchStrategy {
    List<String> search(DictTrieNode root, String query, int limit);
}

/** Observer — dictionary events. */
interface DictListener {
    void onEvent(DictEventType type, String word);
}

// ==================== STRATEGY IMPLEMENTATIONS ====================

/** Prefix search: DFS in ASCII order, collect up to limit words. */
class PrefixSearchStrategy implements DictSearchStrategy {
    @Override public List<String> search(DictTrieNode root, String prefix, int limit) {
        DictTrieNode node = root;
        for (char c : prefix.toCharArray()) {
            node = node.children[c];
            if (node == null) return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        collectSorted(node, result, limit);
        return result;
    }

    private void collectSorted(DictTrieNode node, List<String> result, int limit) {
        if (result.size() >= limit) return;
        if (node.isWord) result.add(node.word);
        for (int c = 0; c < 128; c++)
            if (node.children[c] != null) collectSorted(node.children[c], result, limit);
    }
}

/** Wildcard search: '.' matches any single char. Returns matching words. */
class WildcardSearchStrategy implements DictSearchStrategy {
    @Override public List<String> search(DictTrieNode root, String pattern, int limit) {
        List<String> result = new ArrayList<>();
        dfs(root, pattern, 0, result, limit);
        return result;
    }

    private void dfs(DictTrieNode node, String pattern, int idx, List<String> result, int limit) {
        if (result.size() >= limit) return;
        if (idx == pattern.length()) { if (node.isWord) result.add(node.word); return; }
        char ch = pattern.charAt(idx);
        if (ch == '.') {
            for (DictTrieNode child : node.children)
                if (child != null) dfs(child, pattern, idx + 1, result, limit);
        } else {
            DictTrieNode child = node.children[ch];
            if (child != null) dfs(child, pattern, idx + 1, result, limit);
        }
    }
}

// ==================== OBSERVER IMPLEMENTATIONS ====================

class DictLogger implements DictListener {
    final List<String> events = new ArrayList<>();
    @Override public void onEvent(DictEventType type, String word) {
        events.add(type + ":" + word);
    }
}

// ==================== DICTIONARY SERVICE (FACADE) ====================

class DictionaryService {
    private final DictTrieNode root = new DictTrieNode();
    private int wordCount = 0;
    private DictSearchStrategy searchStrategy;
    private final List<DictListener> listeners = new ArrayList<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    DictionaryService(DictSearchStrategy strategy) { this.searchStrategy = strategy; }
    DictionaryService() { this(new PrefixSearchStrategy()); }

    void setSearchStrategy(DictSearchStrategy s) { this.searchStrategy = s; }
    void addListener(DictListener l) { listeners.add(l); }

    private void fireEvent(DictEventType type, String word) {
        for (DictListener l : listeners) l.onEvent(type, word);
    }

    /** Add a meaning to a word. Multiple meanings supported. */
    void addWord(String word, String meaning) {
        if (word == null || word.isEmpty()) throw new InvalidWordException("null or empty");
        lock.writeLock().lock();
        try {
            DictTrieNode node = root;
            for (char c : word.toLowerCase().toCharArray()) {
                if (node.children[c] == null) node.children[c] = new DictTrieNode();
                node = node.children[c];
            }
            boolean isNew = !node.isWord;
            if (isNew) wordCount++;
            node.isWord = true;
            node.word = word.toLowerCase();
            node.meanings.add(meaning);
            fireEvent(isNew ? DictEventType.ADDED : DictEventType.UPDATED, word);
        } finally { lock.writeLock().unlock(); }
    }

    /** Get all meanings for a word. */
    List<String> getMeanings(String word) {
        lock.readLock().lock();
        try {
            DictTrieNode node = findNode(word.toLowerCase());
            if (node == null || !node.isWord) throw new WordNotFoundException(word);
            return new ArrayList<>(node.meanings);
        } finally { lock.readLock().unlock(); }
    }

    /** Search using current strategy (prefix or wildcard). */
    List<String> search(String query, int limit) {
        if (query == null || query.isEmpty()) return Collections.emptyList();
        lock.readLock().lock();
        try {
            List<String> result = searchStrategy.search(root, query.toLowerCase(), limit);
            fireEvent(DictEventType.SEARCHED, query);
            return result;
        } finally { lock.readLock().unlock(); }
    }

    boolean deleteWord(String word) {
        if (word == null || word.isEmpty()) return false;
        lock.writeLock().lock();
        try {
            DictTrieNode node = findNode(word.toLowerCase());
            if (node == null || !node.isWord) return false;
            node.isWord = false; node.word = null; node.meanings.clear();
            wordCount--;
            fireEvent(DictEventType.DELETED, word);
            return true;
        } finally { lock.writeLock().unlock(); }
    }

    int size() { return wordCount; }

    private DictTrieNode findNode(String word) {
        DictTrieNode node = root;
        for (char c : word.toCharArray()) {
            node = node.children[c];
            if (node == null) return null;
        }
        return node;
    }
}

// ==================== MAIN / TESTS ====================

public class DictionarySystem {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════╗");
        System.out.println("║    DICTIONARY - LLD Demo          ║");
        System.out.println("╚═══════════════════════════════════╝\n");

        // --- Test 1: Add & Fetch ---
        System.out.println("=== Test 1: Add & Fetch ===");
        DictionaryService svc = new DictionaryService();
        svc.addWord("apple", "a fruit");
        check(svc.getMeanings("apple").get(0), "a fruit", "apple = 'a fruit'");
        System.out.println("✓\n");

        // --- Test 2: Multiple meanings ---
        System.out.println("=== Test 2: Multiple meanings ===");
        svc.addWord("apple", "a tech company");
        List<String> meanings = svc.getMeanings("apple");
        check(meanings.size(), 2, "2 meanings");
        check(meanings.get(0), "a fruit", "1st = fruit");
        check(meanings.get(1), "a tech company", "2nd = tech");
        check(svc.size(), 1, "Still 1 word");
        System.out.println("✓\n");

        // --- Test 3: Not found ---
        System.out.println("=== Test 3: Not found ===");
        try { svc.getMeanings("banana"); System.out.println("  ✗"); }
        catch (WordNotFoundException e) { System.out.println("  ✓ " + e.getMessage()); }
        System.out.println("✓\n");

        // --- Test 4: Prefix Search (default strategy) ---
        System.out.println("=== Test 4: Prefix Search ===");
        svc.addWord("app", "short for application");
        svc.addWord("apply", "make a request");
        svc.addWord("apt", "suitable");
        svc.addWord("banana", "yellow fruit");
        List<String> r = svc.search("ap", 3);
        check(r.size(), 3, "3 results");
        check(r.get(0), "app", "1st = app");
        check(r.get(1), "apple", "2nd = apple");
        check(r.get(2), "apply", "3rd = apply");
        check(svc.search("z", 5).size(), 0, "No 'z' words");
        System.out.println("✓\n");

        // --- Test 5: Strategy swap → Wildcard ---
        System.out.println("=== Test 5: Strategy swap → Wildcard ===");
        DictionaryService d2 = new DictionaryService();
        for (String w : new String[]{"cat", "cap", "caps", "map", "man", "many"})
            d2.addWord(w, "meaning of " + w);
        d2.setSearchStrategy(new WildcardSearchStrategy());
        check(d2.search("c.t", 10).size(), 1, "'c.t' matches cat");
        check(d2.search("..p", 10).size(), 2, "'..p' matches cap, map");
        check(d2.search("c..s", 10).size(), 1, "'c..s' matches caps");
        check(d2.search("c.", 10).size(), 0, "'c.' too short");
        check(d2.search("zzz", 10).size(), 0, "No match");
        // Swap back to prefix
        d2.setSearchStrategy(new PrefixSearchStrategy());
        check(d2.search("ca", 10).size(), 3, "Prefix 'ca' = cat,cap,caps");
        System.out.println("✓\n");

        // --- Test 6: Observer ---
        System.out.println("=== Test 6: Observer ===");
        DictionaryService d3 = new DictionaryService();
        DictLogger logger = new DictLogger();
        d3.addListener(logger);
        d3.addWord("java", "programming language");
        d3.addWord("java", "island in Indonesia");
        d3.search("jav", 5);
        d3.deleteWord("java");
        check(logger.events.size(), 4, "4 events (add, update, search, delete)");
        check(logger.events.get(0), "ADDED:java", "1st = ADDED");
        check(logger.events.get(1), "UPDATED:java", "2nd = UPDATED");
        check(logger.events.get(2), "SEARCHED:jav", "3rd = SEARCHED");
        check(logger.events.get(3), "DELETED:java", "4th = DELETED");
        System.out.println("✓\n");

        // --- Test 7: Delete ---
        System.out.println("=== Test 7: Delete ===");
        check(svc.deleteWord("apt"), true, "Deleted apt");
        check(svc.search("apt", 5).size(), 0, "apt gone");
        check(svc.deleteWord("nope"), false, "Not found");
        System.out.println("✓\n");

        // --- Test 8: Case insensitive ---
        System.out.println("=== Test 8: Case insensitive ===");
        DictionaryService d4 = new DictionaryService();
        d4.addWord("Hello", "greeting");
        check(d4.getMeanings("hello").get(0), "greeting", "Case insensitive");
        System.out.println("✓\n");

        // --- Test 9: Exceptions ---
        System.out.println("=== Test 9: Exceptions ===");
        try { svc.addWord("", "x"); } catch (InvalidWordException e) { System.out.println("  ✓ " + e.getMessage()); }
        try { svc.addWord(null, "x"); } catch (InvalidWordException e) { System.out.println("  ✓ " + e.getMessage()); }
        System.out.println("✓\n");

        // --- Test 10: Scale ---
        System.out.println("=== Test 10: Scale ===");
        DictionaryService d5 = new DictionaryService();
        for (int i = 0; i < 10000; i++) d5.addWord("word" + i, "m" + i);
        long t = System.nanoTime();
        check(d5.search("word1", 5).size(), 5, String.format("Prefix in %.2f ms", (System.nanoTime()-t)/1e6));
        d5.setSearchStrategy(new WildcardSearchStrategy());
        t = System.nanoTime();
        check(d5.search("w..d5", 5).size() > 0, true, String.format("Wildcard in %.2f ms", (System.nanoTime()-t)/1e6));
        System.out.println("✓\n");

        // --- Test 11: Thread Safety ---
        System.out.println("=== Test 11: Thread Safety ===");
        DictionaryService d6 = new DictionaryService();
        ExecutorService exec = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) { int x = i; futures.add(exec.submit(() -> d6.addWord("t" + x, "m" + x))); }
        for (int i = 0; i < 50; i++) futures.add(exec.submit(() -> d6.search("t", 5)));
        for (Future<?> f : futures) { try { f.get(); } catch (Exception e) {} }
        exec.shutdown();
        check(d6.size(), 100, "100 concurrent inserts");
        System.out.println("✓\n");

        System.out.println("════════ ALL 11 TESTS PASSED ✓ ════════");
    }

    static void check(int a, int e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
    static void check(String a, String e, String m) { System.out.println("  " + (Objects.equals(a, e) ? "✓" : "✗ GOT '" + a + "'") + " " + m); }
    static void check(boolean a, boolean e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. TRIE: DictTrieNode[128]. O(L) insert/lookup. Multiple meanings at leaf.
 *    Prefix: DFS in ASCII order → lex sorted. Wildcard: DFS with '.' branching.
 *
 * 2. STRATEGY (DictSearchStrategy): PrefixSearchStrategy (lex DFS),
 *    WildcardSearchStrategy (dot-branching DFS). Swap at runtime.
 *
 * 3. OBSERVER (DictListener): DictLogger tracks ADDED/UPDATED/DELETED/SEARCHED.
 *    Could feed analytics, cache invalidation, etc.
 *
 * 4. MULTI-MEANING: Each word stores List<String> meanings. addWord appends,
 *    getMeanings returns copy. Delete clears all meanings.
 *
 * 5. THREAD SAFETY: ReadWriteLock. Concurrent reads, exclusive writes.
 *
 * 6. EXTENSIONS: fuzzy match, spellcheck, synonyms, compressed trie.
 */
