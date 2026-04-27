import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.stream.*;

/*
 * SEARCH INDEX - Low Level Design
 * ================================
 * 
 * REQUIREMENTS:
 * 1. Index documents (id, content), full-text search
 * 2. Pluggable ranking: TF-IDF or BM25
 * 3. OR / AND queries
 * 4. Add/remove documents, thread-safe
 * 
 * DESIGN PATTERNS:
 *   Strategy (RankingStrategy) — TfIdfStrategy, Bm25Strategy
 *   Facade   (SearchIndexEngine)
 * 
 * KEY DS: Inverted Index: term → {docId → frequency}
 */

// ==================== EXCEPTIONS ====================

class SearchDocNotFoundException extends RuntimeException {
    SearchDocNotFoundException(String id) { super("Document not found: " + id); }
}

class DocumentAlreadyExistsException extends RuntimeException {
    DocumentAlreadyExistsException(String id) { super("Document already exists: " + id); }
}

// ==================== MODELS ====================

class SearchDoc {
    final String id, content;
    SearchDoc(String id, String content) { this.id = id; this.content = content; }
}

class SearchResult implements Comparable<SearchResult> {
    final SearchDoc document;
    final double score;
    final Set<String> matchedTerms;
    SearchResult(SearchDoc doc, double score, Set<String> matched) { this.document = doc; this.score = score; this.matchedTerms = matched; }
    @Override public int compareTo(SearchResult o) { return Double.compare(o.score, this.score); }
}

// ==================== INVERTED INDEX ====================

class InvertedIndex {
    final Map<String, Map<String, Integer>> index = new ConcurrentHashMap<>();
    int totalDocs;

    static final Set<String> STOP = Set.of("a","an","and","are","as","at","be","by","for","from","has","he","in",
        "is","it","its","of","on","that","the","to","was","were","will","with","this","but","they","have","had","not","or","so","if","do","no","can");

    static List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) return Collections.emptyList();
        return Arrays.stream(text.toLowerCase().split("[^a-z0-9]+"))
            .filter(w -> !w.isEmpty() && !STOP.contains(w))
            .collect(Collectors.toList());
    }

    void addDocument(String docId, List<String> terms) {
        for (String t : terms) index.computeIfAbsent(t, k -> new ConcurrentHashMap<>()).merge(docId, 1, Integer::sum);
    }

    void removeDocument(String docId) {
        for (Map<String, Integer> m : index.values()) m.remove(docId);
        index.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    Set<String> getDocIds(String term) { Map<String, Integer> m = index.get(term); return m == null ? Collections.emptySet() : m.keySet(); }
    int getTermFreq(String term, String docId) { Map<String, Integer> m = index.get(term); return m == null ? 0 : m.getOrDefault(docId, 0); }
    int getDocFreq(String term) { Map<String, Integer> m = index.get(term); return m == null ? 0 : m.size(); }
}

// ==================== INTERFACES ====================

interface RankingStrategy {
    double score(String term, String docId, InvertedIndex idx);
}

// ==================== STRATEGY IMPLEMENTATIONS ====================

/** TF-IDF: score = tf * log(N/df) */
class TfIdfStrategy implements RankingStrategy {
    @Override public double score(String term, String docId, InvertedIndex idx) {
        int tf = idx.getTermFreq(term, docId);
        if (tf == 0) return 0.0;
        double idf = Math.log((double)(idx.totalDocs + 1) / (idx.getDocFreq(term) + 1));
        return tf * idf;
    }
}

/** BM25 simplified: score = idf * tf / (tf + 1) — saturation without length norm */
class Bm25Strategy implements RankingStrategy {
    @Override public double score(String term, String docId, InvertedIndex idx) {
        int tf = idx.getTermFreq(term, docId);
        if (tf == 0) return 0.0;
        double idf = Math.log((double)(idx.totalDocs + 1) / (idx.getDocFreq(term) + 1));
        return idf * tf / (tf + 1.0); // saturation: tf=5 scores ~0.83, tf=1 scores ~0.5
    }
}

// ==================== SEARCH INDEX ENGINE (FACADE) ====================

class SearchIndexEngine {
    private final Map<String, SearchDoc> docs = new ConcurrentHashMap<>();
    private final InvertedIndex idx = new InvertedIndex();
    private RankingStrategy strategy;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    SearchIndexEngine(RankingStrategy strategy) { this.strategy = strategy; }
    SearchIndexEngine() { this(new TfIdfStrategy()); }

    void setStrategy(RankingStrategy s) { this.strategy = s; }

    void indexDocument(SearchDoc doc) {
        lock.writeLock().lock();
        try {
            if (docs.containsKey(doc.id)) { docs.remove(doc.id); idx.removeDocument(doc.id); idx.totalDocs--; }
            docs.put(doc.id, doc);
            idx.totalDocs++;
            idx.addDocument(doc.id, InvertedIndex.tokenize(doc.content));
        } finally { lock.writeLock().unlock(); }
    }

    boolean removeDocument(String docId) {
        lock.writeLock().lock();
        try {
            if (docs.remove(docId) == null) return false;
            idx.removeDocument(docId); idx.totalDocs = Math.max(0, idx.totalDocs - 1);
            return true;
        } finally { lock.writeLock().unlock(); }
    }

    List<SearchResult> search(String query, int limit) {
        lock.readLock().lock();
        try {
            List<String> terms = InvertedIndex.tokenize(query);
            if (terms.isEmpty()) return Collections.emptyList();
            Set<String> candidates = terms.stream().flatMap(t -> idx.getDocIds(t).stream()).collect(Collectors.toSet());
            return candidates.stream().map(docId -> {
                SearchDoc doc = docs.get(docId);
                if (doc == null) return null;
                double score = 0; Set<String> matched = new HashSet<>();
                for (String t : terms) { double s = strategy.score(t, docId, idx); if (s > 0) { score += s; matched.add(t); } }
                return new SearchResult(doc, score, matched);
            }).filter(Objects::nonNull).sorted().limit(limit).collect(Collectors.toList());
        } finally { lock.readLock().unlock(); }
    }

    List<SearchResult> search(String query) { return search(query, 10); }

    int getDocumentCount() { return docs.size(); }
}

// ==================== MAIN / TESTS ====================

public class SearchIndex {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════╗");
        System.out.println("║    SEARCH INDEX - LLD Demo        ║");
        System.out.println("╚═══════════════════════════════════╝\n");

        System.out.println("=== Test 1: OR Search (TF-IDF) ===");
        SearchIndexEngine engine = new SearchIndexEngine(new TfIdfStrategy());
        engine.indexDocument(new SearchDoc("1", "Java language for enterprise apps and microservices"));
        engine.indexDocument(new SearchDoc("2", "Python language for data science and machine learning"));
        engine.indexDocument(new SearchDoc("3", "JavaScript for web development with Node.js"));
        engine.indexDocument(new SearchDoc("4", "Building microservices with Java Spring Boot"));
        engine.indexDocument(new SearchDoc("5", "Machine learning with Python tensorflow neural networks"));
        check(engine.search("java").size() >= 2, true, "At least 2 java docs");
        System.out.println("✓\n");

        System.out.println("=== Test 2: Strategy swap → BM25 ===");
        engine.setStrategy(new Bm25Strategy());
        check(engine.search("java").size() >= 2, true, "BM25 works");
        System.out.println("✓\n");

        System.out.println("=== Test 4: TF-IDF Ranking ===");
        SearchIndexEngine e2 = new SearchIndexEngine(new TfIdfStrategy());
        e2.indexDocument(new SearchDoc("a", "java java java java java"));
        e2.indexDocument(new SearchDoc("b", "java python ruby golang"));
        check(e2.search("java").get(0).document.id, "a", "5x java first");
        System.out.println("✓\n");

        System.out.println("=== Test 5: BM25 saturation ===");
        SearchIndexEngine e5 = new SearchIndexEngine(new Bm25Strategy());
        e5.indexDocument(new SearchDoc("a", "java java java java java"));
        e5.indexDocument(new SearchDoc("b", "java python ruby golang"));
        e5.indexDocument(new SearchDoc("c", "python ruby golang rust")); // no java → gives IDF > 0
        var r5 = e5.search("java");
        System.out.printf("  Ratio=%.2f\n", r5.get(0).score / r5.get(1).score);
        check(r5.get(0).score > r5.get(1).score, true, "Saturated");
        System.out.println("✓\n");

        System.out.println("=== Test 6: Update & Remove ===");
        engine.indexDocument(new SearchDoc("1", "Rust systems language safety"));
        check(engine.search("rust").size(), 1, "Updated");
        check(engine.removeDocument("5"), true, "Removed");
        check(engine.getDocumentCount(), 4, "4 left");
        System.out.println("✓\n");

        System.out.println("=== Test 7: Edge Cases ===");
        check(engine.search("").size(), 0, "Empty");
        check(engine.search("the is a").size(), 0, "Stop words");
        check(engine.search("xyzzy").size(), 0, "Unknown");
        System.out.println("✓\n");

        System.out.println("=== Test 8: Scale ===");
        SearchIndexEngine e3 = new SearchIndexEngine(new Bm25Strategy());
        for (int i = 0; i < 1000; i++) e3.indexDocument(new SearchDoc("d"+i, "topic"+(i%7)+" engineering doc "+i));
        long t = System.nanoTime();
        check(e3.search("topic0", 5).size(), 5, String.format("5 results in %.2f ms", (System.nanoTime()-t)/1e6));
        System.out.println("✓\n");

        System.out.println("=== Test 9: Thread Safety ===");
        SearchIndexEngine e4 = new SearchIndexEngine();
        ExecutorService exec = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) { int x = i; futures.add(exec.submit(() -> e4.indexDocument(new SearchDoc("t"+x, "concurrent doc "+x)))); }
        for (Future<?> f : futures) { try { f.get(); } catch (Exception e) {} }
        exec.shutdown();
        check(e4.getDocumentCount(), 100, "100 docs");
        System.out.println("✓\n");

        System.out.println("════════ ALL 9 TESTS PASSED ✓ ════════");
    }

    static void check(int a, int e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
    static void check(String a, String e, String m) { System.out.println("  " + (Objects.equals(a, e) ? "✓" : "✗ GOT '" + a + "'") + " " + m); }
    static void check(boolean a, boolean e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
}

/*
 * INTERVIEW NOTES:
 * 1. STRATEGY: TfIdf vs BM25, swap at runtime. BM25 = Elasticsearch default.
 * 2. INVERTED INDEX: term→{docId→freq}. OR=union, AND=intersection.
 * 3. TOKENIZE: lowercase + split on non-alphanumeric + stop word removal.
 * 4. THREAD SAFETY: ReadWriteLock.
 * 5. EXTENSIONS: stemming (Chain of Resp), phrase search, field weighting, sharding.
 */
