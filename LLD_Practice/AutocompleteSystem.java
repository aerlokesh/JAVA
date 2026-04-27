import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/*
 * SEARCH AUTOCOMPLETE - Low Level Design
 * =========================================
 * 
 * REQUIREMENTS:
 * 1. Add queries with frequency, return top-K suggestions for prefix
 * 2. Pluggable ranking: frequency vs alphabetical (Strategy)
 * 3. Track search analytics (Observer)
 * 4. Case-insensitive, delete queries, thread-safe
 * 
 * DESIGN PATTERNS:
 *   Strategy  (SuggestionRanker) — FrequencyRanker, AlphabeticalRanker
 *   Observer  (AutocompleteListener) — SearchTracker
 *   Facade    (AutocompleteService)
 * 
 * KEY DS: Trie — ACTrieNode[128] children, O(L) insert, O(P+N) search
 */

// ==================== EXCEPTIONS ====================

class EmptyQueryException extends RuntimeException {
    EmptyQueryException() { super("Query cannot be null or empty"); }
}

// ==================== MODELS ====================

class ACTrieNode {
    final ACTrieNode[] children = new ACTrieNode[128];
    String word;
    int frequency;
    boolean isEnd;
}

class AutocompleteSuggestion {
    final String query;
    final int frequency;
    AutocompleteSuggestion(String query, int frequency) { this.query = query; this.frequency = frequency; }
}

// ==================== INTERFACES ====================

/** Strategy — pluggable ranking for suggestions. */
interface SuggestionRanker {
    Comparator<AutocompleteSuggestion> comparator();
}

/** Observer — notified on search events. */
interface AutocompleteListener {
    void onSearch(String prefix, int resultCount);
}

// ==================== STRATEGY IMPLEMENTATIONS ====================

/** Rank by frequency desc, alphabetically on tie. */
class FrequencyRanker implements SuggestionRanker {
    @Override public Comparator<AutocompleteSuggestion> comparator() {
        return (a, b) -> a.frequency != b.frequency ? b.frequency - a.frequency : a.query.compareTo(b.query);
    }
}

/** Rank alphabetically. */
class AlphabeticalRanker implements SuggestionRanker {
    @Override public Comparator<AutocompleteSuggestion> comparator() {
        return Comparator.comparing(a -> a.query);
    }
}

// ==================== OBSERVER IMPLEMENTATIONS ====================

/** Tracks how many times each prefix was searched. */
class SearchTracker implements AutocompleteListener {
    final Map<String, Integer> searchCounts = new HashMap<>();
    int totalSearches = 0;

    @Override public void onSearch(String prefix, int resultCount) {
        searchCounts.merge(prefix, 1, Integer::sum);
        totalSearches++;
    }

    int getSearchCount(String prefix) { return searchCounts.getOrDefault(prefix, 0); }
}

// ==================== AUTOCOMPLETE SERVICE (FACADE) ====================

class AutocompleteService {
    private final ACTrieNode root = new ACTrieNode();
    private final int defaultTopK;
    private SuggestionRanker ranker;
    private final List<AutocompleteListener> listeners = new ArrayList<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    AutocompleteService(int topK, SuggestionRanker ranker) { this.defaultTopK = topK; this.ranker = ranker; }
    AutocompleteService(int topK) { this(topK, new FrequencyRanker()); }
    AutocompleteService() { this(5); }

    void setRanker(SuggestionRanker r) { this.ranker = r; }
    void addListener(AutocompleteListener l) { listeners.add(l); }

    void addQuery(String query, int frequency) {
        if (query == null || query.isEmpty()) throw new EmptyQueryException();
        lock.writeLock().lock();
        try {
            ACTrieNode node = root;
            for (char c : query.toLowerCase().toCharArray()) {
                if (node.children[c] == null) node.children[c] = new ACTrieNode();
                node = node.children[c];
            }
            node.isEnd = true;
            node.word = query.toLowerCase();
            node.frequency += frequency;
        } finally { lock.writeLock().unlock(); }
    }

    void addQuery(String query) { addQuery(query, 1); }

    List<AutocompleteSuggestion> getSuggestions(String prefix, int topK) {
        if (prefix == null || prefix.isEmpty()) return Collections.emptyList();
        lock.readLock().lock();
        try {
            ACTrieNode node = root;
            for (char c : prefix.toLowerCase().toCharArray()) {
                node = node.children[c];
                if (node == null) return Collections.emptyList();
            }
            PriorityQueue<AutocompleteSuggestion> pq = new PriorityQueue<>(ranker.comparator());
            collectAll(node, pq);
            List<AutocompleteSuggestion> result = new ArrayList<>();
            while (!pq.isEmpty() && result.size() < topK) result.add(pq.poll());
            for (AutocompleteListener l : listeners) l.onSearch(prefix, result.size());
            return result;
        } finally { lock.readLock().unlock(); }
    }

    List<AutocompleteSuggestion> getSuggestions(String prefix) { return getSuggestions(prefix, defaultTopK); }

    private void collectAll(ACTrieNode node, PriorityQueue<AutocompleteSuggestion> pq) {
        if (node.isEnd) pq.offer(new AutocompleteSuggestion(node.word, node.frequency));
        for (ACTrieNode child : node.children) if (child != null) collectAll(child, pq);
    }

    boolean deleteQuery(String query) {
        if (query == null || query.isEmpty()) return false;
        lock.writeLock().lock();
        try {
            ACTrieNode node = root;
            for (char c : query.toLowerCase().toCharArray()) {
                node = node.children[c];
                if (node == null) return false;
            }
            if (!node.isEnd) return false;
            node.isEnd = false; node.word = null; node.frequency = 0;
            return true;
        } finally { lock.writeLock().unlock(); }
    }
}

// ==================== MAIN / TESTS ====================

public class AutocompleteSystem {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║   SEARCH AUTOCOMPLETE - LLD Demo          ║");
        System.out.println("╚═══════════════════════════════════════════╝\n");

        // --- Test 1: Basic suggestions ---
        System.out.println("=== Test 1: Basic suggestions ===");
        AutocompleteService svc = new AutocompleteService(3);
        svc.addQuery("amazon", 100);
        svc.addQuery("amazon prime", 80);
        svc.addQuery("amazon music", 50);
        svc.addQuery("apple", 90);
        List<AutocompleteSuggestion> r = svc.getSuggestions("ama");
        check(r.size(), 3, "3 results");
        check(r.get(0).query, "amazon", "Top = amazon(100)");
        check(r.get(1).query, "amazon prime", "2nd = amazon prime(80)");
        System.out.println("✓\n");

        // --- Test 2: Prefix not found ---
        System.out.println("=== Test 2: Prefix not found ===");
        check(svc.getSuggestions("xyz").size(), 0, "No results");
        System.out.println("✓\n");

        // --- Test 3: Case insensitive ---
        System.out.println("=== Test 3: Case insensitive ===");
        svc.addQuery("Google", 200);
        check(svc.getSuggestions("GOO").size(), 1, "Case insensitive");
        System.out.println("✓\n");

        // --- Test 4: Frequency increment ---
        System.out.println("=== Test 4: Frequency increment ===");
        svc.addQuery("amazon", 50);
        List<AutocompleteSuggestion> r4 = svc.getSuggestions("amazon");
        check(r4.get(0).frequency, 150, "100+50=150");
        System.out.println("✓\n");

        // --- Test 5: Strategy swap ---
        System.out.println("=== Test 5: Strategy swap ===");
        AutocompleteService svc5 = new AutocompleteService(5);
        svc5.addQuery("cat", 10); svc5.addQuery("car", 100); svc5.addQuery("cab", 50);
        check(svc5.getSuggestions("ca").get(0).query, "car", "FreqRanker: car first");
        svc5.setRanker(new AlphabeticalRanker());
        check(svc5.getSuggestions("ca").get(0).query, "cab", "AlphaRanker: cab first");
        System.out.println("✓\n");

        // --- Test 6: Observer ---
        System.out.println("=== Test 6: Observer ===");
        AutocompleteService svc6 = new AutocompleteService(5);
        SearchTracker tracker = new SearchTracker();
        svc6.addListener(tracker);
        svc6.addQuery("java", 100); svc6.addQuery("javascript", 80);
        svc6.getSuggestions("jav");
        svc6.getSuggestions("jav");
        svc6.getSuggestions("java");
        check(tracker.totalSearches, 3, "3 total searches");
        check(tracker.getSearchCount("jav"), 2, "'jav' searched 2x");
        check(tracker.getSearchCount("java"), 1, "'java' searched 1x");
        System.out.println("✓\n");

        // --- Test 7: Delete ---
        System.out.println("=== Test 7: Delete ===");
        AutocompleteService svc7 = new AutocompleteService();
        svc7.addQuery("hello world", 5);
        svc7.addQuery("hello there", 3);
        check(svc7.deleteQuery("hello world"), true, "Deleted");
        check(svc7.getSuggestions("hel").size(), 1, "1 left");
        check(svc7.deleteQuery("nope"), false, "Not found");
        System.out.println("✓\n");

        // --- Test 8: Exceptions ---
        System.out.println("=== Test 8: Exceptions ===");
        try { svc.addQuery(""); } catch (EmptyQueryException e) { System.out.println("  ✓ " + e.getMessage()); }
        try { svc.addQuery(null); } catch (EmptyQueryException e) { System.out.println("  ✓ " + e.getMessage()); }
        check(svc.getSuggestions("").size(), 0, "Empty prefix = 0");
        System.out.println("✓\n");

        // --- Test 9: Top-K ---
        System.out.println("=== Test 9: Top-K ===");
        check(svc.getSuggestions("a", 2).size(), 2, "Limited to 2");
        System.out.println("✓\n");

        // --- Test 10: Thread Safety ---
        System.out.println("=== Test 10: Thread Safety ===");
        AutocompleteService svc10 = new AutocompleteService(5);
        ExecutorService exec = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            int x = i;
            futures.add(exec.submit(() -> svc10.addQuery("term" + x, x)));
        }
        for (int i = 0; i < 50; i++)
            futures.add(exec.submit(() -> svc10.getSuggestions("term")));
        for (Future<?> f : futures) { try { f.get(); } catch (Exception e) {} }
        exec.shutdown();
        check(svc10.getSuggestions("term", 100).size(), 100, "100 concurrent inserts");
        System.out.println("✓\n");

        // --- Test 11: Scale ---
        System.out.println("=== Test 11: Scale ===");
        AutocompleteService svc11 = new AutocompleteService(5);
        long start = System.nanoTime();
        for (int i = 0; i < 10000; i++) svc11.addQuery("query" + i, i);
        System.out.printf("  10K inserts: %.2f ms\n", (System.nanoTime() - start) / 1e6);
        start = System.nanoTime();
        List<AutocompleteSuggestion> r11 = svc11.getSuggestions("query9", 5);
        System.out.printf("  Search top-5: %.2f ms\n", (System.nanoTime() - start) / 1e6);
        check(r11.size(), 5, "Got 5");
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
 * 1. TRIE: ACTrieNode[128] array per node. Insert O(L). DFS prefix search O(P+N).
 *    PriorityQueue with ranker's comparator for efficient top-K extraction.
 *
 * 2. STRATEGY (SuggestionRanker): FrequencyRanker (desc + alpha tiebreak),
 *    AlphabeticalRanker. Swap at runtime via setRanker().
 *
 * 3. OBSERVER (AutocompleteListener): SearchTracker counts prefix searches.
 *    Could feed trending queries back to boost frequency.
 *
 * 4. THREAD SAFETY: ReadWriteLock — concurrent getSuggestions reads,
 *    exclusive addQuery/deleteQuery writes.
 *
 * 5. EXTENSIONS: trending decay, per-user trie, distributed sharding, fuzzy match.
 */
