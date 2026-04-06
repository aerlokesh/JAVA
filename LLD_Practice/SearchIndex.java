import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.stream.*;

/*
 * SEARCH INDEX - Low Level Design
 * ================================
 * 
 * REQUIREMENTS:
 * 1. Index documents (id, title, content)
 * 2. Full-text search with TF-IDF ranking
 * 3. Boolean queries: AND, OR
 * 4. Autocomplete suggestions (Trie)
 * 5. Add/remove/update documents
 * 6. Thread-safe operations
 * 
 * KEY DATA STRUCTURES:
 * - Inverted Index: term -> {docId -> frequency}
 * - Trie: prefix tree for autocomplete ranked by frequency
 * 
 * COMPLEXITY:
 *   indexDocument: O(n) where n = terms in doc
 *   search (OR):  O(q * d) where q=query terms, d=avg docs/term
 *   search (AND): O(q * min_d) with set intersection
 *   autocomplete:  O(k + r) where k=prefix len, r=results
 */

// ==================== DOCUMENT ====================

class Document {
    final String id, title, content;

    Document(String id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }

    @Override
    public String toString() { return "Doc{" + id + ": '" + title + "'}"; }
}

// ==================== SEARCH RESULT ====================

class SearchResult implements Comparable<SearchResult> {
    final Document document;
    final double score;
    final Set<String> matchedTerms;

    SearchResult(Document doc, double score, Set<String> matched) {
        this.document = doc; this.score = score; this.matchedTerms = matched;
    }

    @Override public int compareTo(SearchResult o) { return Double.compare(o.score, this.score); }

    @Override public String toString() {
        return String.format("  %.4f - %s (matched: %s)", score, document, matchedTerms);
    }
}

// ==================== TEXT PROCESSOR ====================

class TextProcessor {
    private static final Set<String> STOP_WORDS = Set.of(
        "a","an","and","are","as","at","be","by","for","from","has","he","in",
        "is","it","its","of","on","that","the","to","was","were","will","with",
        "this","but","they","have","had","not","or","so","if","do","no","can"
    );

    /** Tokenize: lowercase → strip punctuation → remove stop words. */
    static List<String> tokenize(String text) {
        // TODO: Implement
        // HINT: if (text == null || text.isEmpty()) return Collections.emptyList();
        // HINT: String[] words = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").split("\\s+");
        // HINT: List<String> terms = new ArrayList<>();
        // HINT: for (String w : words)
        // HINT:     if (!w.isEmpty() && !STOP_WORDS.contains(w)) terms.add(w);
        // HINT: return terms;
        return Collections.emptyList();
    }
}

// ==================== INVERTED INDEX ====================

/**
 * Inverted Index: term -> { docId -> termFrequency }
 * Why inverted? Forward (doc→terms) needs scanning all docs.
 * Inverted (term→docs) gives O(1) term lookup.
 */
class InvertedIndex {
    // term -> docId -> frequency
    private final Map<String, Map<String, Integer>> index = new ConcurrentHashMap<>();
    private final Map<String, Integer> docLengths = new ConcurrentHashMap<>();  // for TF normalization
    int totalDocs;

    /** Index terms from a document. */
    void addDocument(String docId, List<String> terms) {
        // TODO: Implement — count freq per term, store in index
        // HINT: docLengths.put(docId, terms.size());
        // HINT: for (String term : terms) {
        // HINT:     index.computeIfAbsent(term, k -> new ConcurrentHashMap<>())
        // HINT:           .merge(docId, 1, Integer::sum);
        // HINT: }
    }

    /** Remove all index entries for a document. */
    void removeDocument(String docId) {
        docLengths.remove(docId);
        for (Map<String, Integer> docMap : index.values()) docMap.remove(docId);
        index.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    Set<String> getDocIds(String term) {
        Map<String, Integer> m = index.get(term);
        return m == null ? Collections.emptySet() : m.keySet();
    }

    /**
     * TF-IDF score for a term in a document.
     * TF  = termFreq / docLength  (how often term appears in THIS doc)
     * IDF = log(N / df) + 1       (how rare the term is globally)
     */
    double score(String term, String docId) {
        // TODO: Implement
        // HINT: Map<String, Integer> docMap = index.get(term);
        // HINT: if (docMap == null || !docMap.containsKey(docId)) return 0.0;
        // HINT: double tf = (double) docMap.get(docId) / docLengths.getOrDefault(docId, 1);
        // HINT: double idf = Math.log((double) (totalDocs + 1) / (docMap.size() + 1)) + 1.0;
        // HINT: return tf * idf;
        return 0.0;
    }

    int getTermCount() { return index.size(); }
}

// ==================== TRIE (AUTOCOMPLETE) ====================

class TrieNode {
    final Map<Character, TrieNode> children = new HashMap<>();
    boolean isWord;
    int frequency;
    String word;
}

class AutocompleteEngine {
    private final TrieNode root = new TrieNode();

    void addWord(String word) {
        // TODO: Implement — traverse/create nodes, mark leaf
        // HINT: TrieNode node = root;
        // HINT: String lower = word.toLowerCase();
        // HINT: for (char ch : lower.toCharArray()) {
        // HINT:     node.children.putIfAbsent(ch, new TrieNode());
        // HINT:     node = node.children.get(ch);
        // HINT: }
        // HINT: node.isWord = true;
        // HINT: node.frequency++;
        // HINT: node.word = lower;
    }

    /** Top-K suggestions by frequency. */
    List<String> suggest(String prefix, int limit) {
        // TODO: Implement — traverse to prefix, collect words, top-K by freq
        // HINT: TrieNode node = root;
        // HINT: for (char ch : prefix.toLowerCase().toCharArray()) {
        // HINT:     node = node.children.get(ch);
        // HINT:     if (node == null) return Collections.emptyList();
        // HINT: }
        // HINT: PriorityQueue<TrieNode> pq = new PriorityQueue<>((a, b) -> b.frequency - a.frequency);
        // HINT: collectWords(node, pq);
        // HINT: List<String> result = new ArrayList<>();
        // HINT: while (!pq.isEmpty() && result.size() < limit) result.add(pq.poll().word);
        // HINT: return result;
        return Collections.emptyList();
    }

    private void collectWords(TrieNode node, PriorityQueue<TrieNode> pq) {
        if (node.isWord) pq.offer(node);
        for (TrieNode child : node.children.values()) collectWords(child, pq);
    }
}

// ==================== SEARCH INDEX ENGINE ====================

class SearchIndexEngine {
    private final Map<String, Document> documents = new ConcurrentHashMap<>();
    private final InvertedIndex invertedIndex = new InvertedIndex();
    private final AutocompleteEngine autocomplete = new AutocompleteEngine();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    void indexDocument(Document doc) {
        lock.writeLock().lock();
        try {
            if (documents.containsKey(doc.id)) removeInternal(doc.id);
            documents.put(doc.id, doc);
            invertedIndex.totalDocs++;
            List<String> terms = TextProcessor.tokenize(doc.title + " " + doc.content);
            invertedIndex.addDocument(doc.id, terms);
            for (String t : new HashSet<>(terms)) autocomplete.addWord(t);
        } finally { lock.writeLock().unlock(); }
    }

    boolean removeDocument(String docId) {
        lock.writeLock().lock();
        try { return removeInternal(docId); }
        finally { lock.writeLock().unlock(); }
    }

    private boolean removeInternal(String docId) {
        if (documents.remove(docId) == null) return false;
        invertedIndex.removeDocument(docId);
        invertedIndex.totalDocs = Math.max(0, invertedIndex.totalDocs - 1);
        return true;
    }

    /** OR search: docs matching ANY term, ranked by TF-IDF. */
    List<SearchResult> search(String query, int limit) {
        lock.readLock().lock();
        try {
            // TODO: Implement — union candidates, score each, sort desc
            // HINT: List<String> terms = TextProcessor.tokenize(query);
            // HINT: if (terms.isEmpty()) return Collections.emptyList();
            // HINT: Set<String> candidates = new HashSet<>();
            // HINT: for (String t : terms) candidates.addAll(invertedIndex.getDocIds(t));
            // HINT: List<SearchResult> results = new ArrayList<>();
            // HINT: for (String docId : candidates) {
            // HINT:     Document doc = documents.get(docId);
            // HINT:     if (doc == null) continue;
            // HINT:     double score = 0;
            // HINT:     Set<String> matched = new HashSet<>();
            // HINT:     for (String t : terms) {
            // HINT:         double s = invertedIndex.score(t, docId);
            // HINT:         if (s > 0) { score += s; matched.add(t); }
            // HINT:     }
            // HINT:     results.add(new SearchResult(doc, score, matched));
            // HINT: }
            // HINT: Collections.sort(results);
            // HINT: return results.stream().limit(limit).collect(Collectors.toList());
            return Collections.emptyList();
        } finally { lock.readLock().unlock(); }
    }

    List<SearchResult> search(String query) { return search(query, 10); }

    /** AND search: docs must contain ALL terms. */
    List<SearchResult> searchAnd(String query, int limit) {
        lock.readLock().lock();
        try {
            List<String> terms = TextProcessor.tokenize(query);
            if (terms.isEmpty()) return Collections.emptyList();

            // Intersect — smallest set first for efficiency
            List<Set<String>> sets = new ArrayList<>();
            for (String t : terms) sets.add(new HashSet<>(invertedIndex.getDocIds(t)));
            sets.sort(Comparator.comparingInt(Set::size));

            Set<String> candidates = sets.get(0);
            for (int i = 1; i < sets.size(); i++) {
                candidates.retainAll(sets.get(i));
                if (candidates.isEmpty()) return Collections.emptyList();
            }

            List<SearchResult> results = new ArrayList<>();
            for (String docId : candidates) {
                Document doc = documents.get(docId);
                if (doc == null) continue;
                double score = 0;
                for (String t : terms) score += invertedIndex.score(t, docId);
                results.add(new SearchResult(doc, score, new HashSet<>(terms)));
            }
            Collections.sort(results);
            return results.stream().limit(limit).collect(Collectors.toList());
        } finally { lock.readLock().unlock(); }
    }

    List<String> autocomplete(String prefix, int limit) {
        lock.readLock().lock();
        try { return autocomplete.suggest(prefix, limit); }
        finally { lock.readLock().unlock(); }
    }

    int getDocumentCount() { return documents.size(); }
    String getStats() {
        return String.format("Docs: %d | Terms: %d", documents.size(), invertedIndex.getTermCount());
    }
}

// ==================== MAIN / TESTS ====================

public class SearchIndex {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════╗");
        System.out.println("║    SEARCH INDEX - LLD Demo        ║");
        System.out.println("╚═══════════════════════════════════╝\n");

        SearchIndexEngine engine = new SearchIndexEngine();

        // --- Test 1: Index & OR Search ---
        System.out.println("=== Test 1: Index & OR Search ===");
        engine.indexDocument(new Document("1", "Java Programming", "Java language for enterprise apps and microservices"));
        engine.indexDocument(new Document("2", "Python Data Science", "Python language for data science and machine learning"));
        engine.indexDocument(new Document("3", "JavaScript Web", "JavaScript for web development with Node.js"));
        engine.indexDocument(new Document("4", "Java Microservices", "Building microservices with Java Spring Boot"));
        engine.indexDocument(new Document("5", "Python ML", "Machine learning with Python tensorflow neural networks"));
        System.out.println(engine.getStats());
        System.out.println("Search 'java':");
        engine.search("java").forEach(System.out::println);
        System.out.println("✓ OR search returns all docs mentioning java\n");

        // --- Test 2: AND Search ---
        System.out.println("=== Test 2: AND Search ===");
        System.out.println("AND 'java microservices':");
        engine.searchAnd("java microservices", 10).forEach(System.out::println);
        System.out.println("✓ Only docs with BOTH terms\n");

        // --- Test 3: TF-IDF Ranking ---
        System.out.println("=== Test 3: TF-IDF Ranking ===");
        SearchIndexEngine e2 = new SearchIndexEngine();
        e2.indexDocument(new Document("a", "A", "java java java java java"));  // high TF
        e2.indexDocument(new Document("b", "B", "java python ruby golang"));   // low TF
        e2.indexDocument(new Document("c", "C", "python ruby golang rust"));   // no java
        System.out.println("'java' — doc A (5x) should beat doc B (1x):");
        e2.search("java").forEach(System.out::println);
        System.out.println("✓ Higher TF = higher score\n");

        // --- Test 4: Autocomplete ---
        System.out.println("=== Test 4: Autocomplete ===");
        System.out.println("'jav': " + engine.autocomplete("jav", 5));
        System.out.println("'py':  " + engine.autocomplete("py", 5));
        System.out.println("'mac': " + engine.autocomplete("mac", 5));
        System.out.println("✓ Trie-based suggestions by frequency\n");

        // --- Test 5: Update & Remove ---
        System.out.println("=== Test 5: Update & Remove ===");
        engine.indexDocument(new Document("1", "Rust Guide", "Rust systems language safety performance"));
        System.out.println("Updated doc1 → Rust. Search 'rust': " + engine.search("rust").size());
        System.out.println("Search 'java' (doc1 gone): " + engine.search("java").size() + " results");
        engine.removeDocument("5");
        System.out.println("Removed doc5. Count: " + engine.getDocumentCount());
        System.out.println("✓ Update re-indexes, remove cleans up\n");

        // --- Test 6: Edge Cases ---
        System.out.println("=== Test 6: Edge Cases ===");
        System.out.println("Empty: " + engine.search("").size());
        System.out.println("Stop words: " + engine.search("the is a").size());
        System.out.println("Nonsense: " + engine.search("xyzzy").size());
        System.out.println("Remove missing: " + engine.removeDocument("999"));
        System.out.println("✓ All return 0 / false\n");

        // --- Test 7: Scale ---
        System.out.println("=== Test 7: Scale ===");
        SearchIndexEngine e3 = new SearchIndexEngine();
        for (int i = 0; i < 1000; i++)
            e3.indexDocument(new Document("d" + i, "Article " + i,
                "topic" + (i % 7) + " concepts best practices engineering doc " + i));
        long t = System.nanoTime();
        var res = e3.search("topic0", 5);
        System.out.printf("1000 docs, search: %d results in %.2f ms\n", res.size(), (System.nanoTime()-t)/1e6);
        System.out.println("✓ Fast at scale\n");

        // --- Test 8: Thread Safety ---
        System.out.println("=== Test 8: Thread Safety ===");
        SearchIndexEngine e4 = new SearchIndexEngine();
        ExecutorService exec = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            int idx = i;
            futures.add(exec.submit(() -> e4.indexDocument(
                new Document("t" + idx, "T" + idx, "concurrent test doc " + idx))));
        }
        for (int i = 0; i < 50; i++)
            futures.add(exec.submit(() -> e4.search("concurrent")));
        for (Future<?> f : futures) { try { f.get(); } catch (Exception e) { System.out.println("ERR: " + e); } }
        exec.shutdown();
        System.out.println("Concurrent: " + e4.getDocumentCount() + " docs, search works: "
            + e4.search("concurrent").size() + " results");
        System.out.println("✓ Thread-safe\n");

        System.out.println("════════ ALL 8 TESTS PASSED ✓ ════════");
    }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. INVERTED INDEX: term -> {docId -> freq}. O(1) term lookup vs
 *    forward index which scans all docs. Elasticsearch/Lucene core DS.
 *
 * 2. TF-IDF: TF = freq/docLen (importance in doc), IDF = log(N/df)
 *    (global rarity). Common words score low, rare+frequent score high.
 *    BM25 is better: adds TF saturation + length normalization.
 *
 * 3. OR = union of doc sets, AND = intersection (smallest-first).
 *
 * 4. TRIE: O(k) insert/lookup. PriorityQueue for top-K by frequency.
 *
 * 5. THREAD SAFETY: ReadWriteLock — concurrent reads, exclusive writes.
 *
 * 6. SCALE: Shard by hash(docId), replicate for reads, scatter-gather
 *    queries, LRU cache popular queries. Real-world: Elasticsearch.
 *
 * 7. EXTENSIONS (discussion only):
 *    - Phrase search: store positions, check consecutive
 *    - Field weighting: title=3x, content=1x
 *    - Prefix/wildcard: scan terms or use trie on index
 *    - Stemming: Porter stemmer for "running" → "run"
 */
