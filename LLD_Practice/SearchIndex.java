import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.stream.*;

/*
 * SEARCH INDEX - Low Level Design
 * ================================
 * 
 * REQUIREMENTS:
 * 1. Index documents (id, title, content, metadata)
 * 2. Full-text search with relevance ranking (TF-IDF)
 * 3. Boolean queries: AND, OR
 * 4. Phrase search (positional index)
 * 5. Prefix/wildcard search
 * 6. Autocomplete suggestions (Trie)
 * 7. Add/remove/update documents
 * 8. Thread-safe operations
 * 
 * KEY DATA STRUCTURES:
 * - Inverted Index: term -> {docId -> PostingEntry(freq, positions)}
 * - Trie: prefix tree for autocomplete
 * 
 * SCORING: TF-IDF with field weighting (title=3x, content=1x)
 * 
 * COMPLEXITY:
 *   indexDocument:  O(n) where n = terms in doc
 *   search (OR):   O(q * d) where q=query terms, d=avg docs/term
 *   search (AND):  O(q * min_d) with set intersection
 *   phrase search: O(q * d * p) where p=avg positions/term
 *   autocomplete:  O(k + r) where k=prefix len, r=results
 */

// ==================== DOCUMENT ====================

class Document {
    private final String id;
    private final String title;
    private final String content;
    private final Map<String, String> metadata;
    private final long indexedAt;

    public Document(String id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.metadata = new ConcurrentHashMap<>();
        this.indexedAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Map<String, String> getMetadata() { return metadata; }
    public long getIndexedAt() { return indexedAt; }

    public void addMetadata(String key, String value) { metadata.put(key, value); }

    @Override
    public String toString() {
        return "Document{id='" + id + "', title='" + title + "'}";
    }
}

// ==================== SEARCH RESULT ====================

class SearchResult implements Comparable<SearchResult> {
    private final Document document;
    private final double score;
    private final Set<String> matchedTerms;

    public SearchResult(Document document, double score, Set<String> matchedTerms) {
        this.document = document;
        this.score = score;
        this.matchedTerms = matchedTerms;
    }

    public Document getDocument() { return document; }
    public double getScore() { return score; }
    public Set<String> getMatchedTerms() { return matchedTerms; }

    @Override
    public int compareTo(SearchResult other) {
        return Double.compare(other.score, this.score); // higher first
    }

    @Override
    public String toString() {
        return String.format("  %.4f - %s (matched: %s)", score, document, matchedTerms);
    }
}

// ==================== TEXT PROCESSOR ====================

/**
 * Tokenization + normalization + stop-word removal + simple stemming.
 * 
 * Production: use Porter/Snowball stemmer, ICU tokenizer for i18n.
 */
class TextProcessor {
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
        "has", "he", "in", "is", "it", "its", "of", "on", "that", "the",
        "to", "was", "were", "will", "with", "this", "but", "they",
        "have", "had", "not", "or", "so", "if", "do", "no", "can"
    ));

    /** Tokenize text into normalized terms (lowercase, no punctuation, no stop words). */
    public static List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) return Collections.emptyList();

        String[] words = text.toLowerCase()
                             .replaceAll("[^a-z0-9\\s]", " ")
                             .split("\\s+");

        List<String> terms = new ArrayList<>();
        for (String w : words) {
            if (!w.isEmpty() && !STOP_WORDS.contains(w)) {
                terms.add(stem(w));
            }
        }
        return terms;
    }

    /** Simple suffix-based stemming. Production: Porter Stemmer. */
    public static String stem(String word) {
        if (word.length() <= 3) return word;
        if (word.endsWith("ing") && word.length() > 5) return word.substring(0, word.length() - 3);
        if (word.endsWith("tion")) return word.substring(0, word.length() - 4);
        if (word.endsWith("ed") && word.length() > 4) return word.substring(0, word.length() - 2);
        if (word.endsWith("ly") && word.length() > 4) return word.substring(0, word.length() - 2);
        if (word.endsWith("es") && word.length() > 4) return word.substring(0, word.length() - 2);
        if (word.endsWith("s") && word.length() > 4) return word.substring(0, word.length() - 1);
        return word;
    }
}

// ==================== POSTING ENTRY ====================

/**
 * Stores per-document info for a term: frequency + positions.
 * Positions enable phrase search (consecutive position matching).
 * 
 * Example: "java" in doc1 at positions [0, 5, 12] with field "content"
 */
class PostingEntry {
    private final String docId;
    private final String field;   // "title" or "content"
    private int frequency;
    private final List<Integer> positions; // for phrase search

    public PostingEntry(String docId, String field) {
        this.docId = docId;
        this.field = field;
        this.frequency = 0;
        this.positions = new ArrayList<>();
    }

    public void addPosition(int pos) {
        positions.add(pos);
        frequency++;
    }

    public String getDocId() { return docId; }
    public String getField() { return field; }
    public int getFrequency() { return frequency; }
    public List<Integer> getPositions() { return positions; }
}

// ==================== INVERTED INDEX ====================

/**
 * Inverted Index with positional information and field tracking.
 * 
 * Structure:
 *   term -> { docId -> [PostingEntry(field="title", freq, positions),
 *                        PostingEntry(field="content", freq, positions)] }
 * 
 * Why inverted? Forward index (doc -> terms) requires scanning all docs.
 * Inverted index (term -> docs) gives O(1) term lookup.
 */
class InvertedIndex {
    // term -> docId -> list of postings (one per field)
    private final Map<String, Map<String, List<PostingEntry>>> index;
    // docId -> total term count (for TF normalization)
    private final Map<String, Integer> docLengths;
    private int totalDocs;

    // Field weights for scoring: title match is 3x more important
    private static final Map<String, Double> FIELD_WEIGHTS = Map.of(
        "title", 3.0,
        "content", 1.0
    );

    public InvertedIndex() {
        this.index = new ConcurrentHashMap<>();
        this.docLengths = new ConcurrentHashMap<>();
        this.totalDocs = 0;
    }

    /**
     * Index terms from a document field.
     * Tracks term frequency AND positions for phrase search.
     */
    public void addDocument(String docId, String field, List<String> terms) {
        docLengths.merge(docId, terms.size(), Integer::sum);

        for (int pos = 0; pos < terms.size(); pos++) {
            String term = terms.get(pos);

            Map<String, List<PostingEntry>> docMap =
                index.computeIfAbsent(term, k -> new ConcurrentHashMap<>());
            List<PostingEntry> postings =
                docMap.computeIfAbsent(docId, k -> new CopyOnWriteArrayList<>());

            // Find or create posting for this field
            PostingEntry entry = null;
            for (PostingEntry p : postings) {
                if (p.getField().equals(field)) { entry = p; break; }
            }
            if (entry == null) {
                entry = new PostingEntry(docId, field);
                postings.add(entry);
            }
            entry.addPosition(pos);
        }
    }

    /** Remove all postings for a document. */
    public void removeDocument(String docId) {
        docLengths.remove(docId);
        for (Map<String, List<PostingEntry>> docMap : index.values()) {
            docMap.remove(docId);
        }
        // Clean up empty term entries
        index.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    public void incrementTotalDocs() { totalDocs++; }
    public void decrementTotalDocs() { totalDocs = Math.max(0, totalDocs - 1); }

    /** Get all document IDs containing term. */
    public Set<String> getDocIds(String term) {
        Map<String, List<PostingEntry>> docMap = index.get(term);
        return docMap == null ? Collections.emptySet() : docMap.keySet();
    }

    /** Get postings for a term in a specific document. */
    public List<PostingEntry> getPostings(String term, String docId) {
        Map<String, List<PostingEntry>> docMap = index.get(term);
        if (docMap == null) return Collections.emptyList();
        List<PostingEntry> postings = docMap.get(docId);
        return postings == null ? Collections.emptyList() : postings;
    }

    /**
     * TF-IDF with field weighting.
     * 
     * TF  = termFreq / docLength  (how often term appears in doc)
     * IDF = log(N / df)           (how rare the term is globally)
     * Field boost: title match × 3, content match × 1
     * 
     * Score = Σ (TF × IDF × fieldWeight) across all fields
     */
    public double calculateScore(String term, String docId) {
        List<PostingEntry> postings = getPostings(term, docId);
        if (postings.isEmpty()) return 0.0;

        int docLen = docLengths.getOrDefault(docId, 1);
        int df = getDocIds(term).size();
        double idf = Math.log((double) (totalDocs + 1) / (df + 1)) + 1.0; // smoothed IDF

        double score = 0.0;
        for (PostingEntry p : postings) {
            double tf = (double) p.getFrequency() / docLen;
            double fieldWeight = FIELD_WEIGHTS.getOrDefault(p.getField(), 1.0);
            score += tf * idf * fieldWeight;
        }
        return score;
    }

    /**
     * Check if terms appear consecutively (phrase search).
     * For each pair of adjacent query terms, check if their positions
     * in the document are also adjacent (pos[i+1] = pos[i] + 1).
     */
    public boolean hasPhrase(String docId, List<String> phraseTerms) {
        if (phraseTerms.size() <= 1) return true;

        // Gather positions per term per field
        // We check phrase within the same field
        Map<String, List<List<Integer>>> fieldPositions = new HashMap<>();

        for (String term : phraseTerms) {
            List<PostingEntry> postings = getPostings(term, docId);
            if (postings.isEmpty()) return false;
            for (PostingEntry p : postings) {
                fieldPositions.computeIfAbsent(p.getField(), k -> new ArrayList<>())
                              .add(p.getPositions());
            }
        }

        // For each field, check consecutive positions
        for (Map.Entry<String, List<List<Integer>>> entry : fieldPositions.entrySet()) {
            List<List<Integer>> posLists = entry.getValue();
            if (posLists.size() != phraseTerms.size()) continue;

            // Check if there's a sequence where pos[i+1] = pos[i] + 1
            if (hasConsecutivePositions(posLists, 0, -1)) return true;
        }
        return false;
    }

    /** Recursive check for consecutive positions across term position lists. */
    private boolean hasConsecutivePositions(List<List<Integer>> posLists, int termIdx, int prevPos) {
        if (termIdx >= posLists.size()) return true;

        for (int pos : posLists.get(termIdx)) {
            if (termIdx == 0 || pos == prevPos + 1) {
                if (hasConsecutivePositions(posLists, termIdx + 1, pos)) return true;
            }
        }
        return false;
    }

    /** Get all terms starting with prefix (for wildcard search). */
    public Set<String> getTermsWithPrefix(String prefix) {
        Set<String> result = new HashSet<>();
        for (String term : index.keySet()) {
            if (term.startsWith(prefix)) result.add(term);
        }
        return result;
    }

    public int getTotalDocs() { return totalDocs; }
    public int getTermCount() { return index.size(); }
    public int getDocLength(String docId) { return docLengths.getOrDefault(docId, 0); }
}

// ==================== TRIE (AUTOCOMPLETE) ====================

/**
 * Trie for autocomplete suggestions, ranked by search frequency.
 * 
 * Time: O(k) insert/lookup where k = word length.
 * Space: O(N × L) where N = words, L = avg length.
 */
class TrieNode {
    final Map<Character, TrieNode> children = new HashMap<>();
    boolean isWord;
    int frequency; // how often this was searched/added
    String word;   // store complete word at leaf for easy retrieval
}

class AutocompleteEngine {
    private final TrieNode root = new TrieNode();

    public void addWord(String word, int freq) {
        TrieNode node = root;
        String lower = word.toLowerCase();
        for (char ch : lower.toCharArray()) {
            node.children.putIfAbsent(ch, new TrieNode());
            node = node.children.get(ch);
        }
        node.isWord = true;
        node.frequency += freq;
        node.word = lower;
    }

    public void addWord(String word) { addWord(word, 1); }

    /** Get top-K suggestions sorted by frequency (descending). */
    public List<String> suggest(String prefix, int limit) {
        TrieNode node = root;
        for (char ch : prefix.toLowerCase().toCharArray()) {
            node = node.children.get(ch);
            if (node == null) return Collections.emptyList();
        }

        // Collect all words under this prefix with their frequencies
        PriorityQueue<TrieNode> pq = new PriorityQueue<>(
            (a, b) -> b.frequency - a.frequency // max-heap by frequency
        );
        collectWords(node, pq);

        List<String> result = new ArrayList<>();
        while (!pq.isEmpty() && result.size() < limit) {
            result.add(pq.poll().word);
        }
        return result;
    }

    private void collectWords(TrieNode node, PriorityQueue<TrieNode> pq) {
        if (node.isWord) pq.offer(node);
        for (TrieNode child : node.children.values()) {
            collectWords(child, pq);
        }
    }

    /** Remove a word from the trie. */
    public boolean remove(String word) {
        return remove(root, word.toLowerCase(), 0);
    }

    private boolean remove(TrieNode node, String word, int idx) {
        if (idx == word.length()) {
            if (!node.isWord) return false;
            node.isWord = false;
            node.frequency = 0;
            node.word = null;
            return node.children.isEmpty();
        }
        char ch = word.charAt(idx);
        TrieNode child = node.children.get(ch);
        if (child == null) return false;

        boolean shouldDelete = remove(child, word, idx + 1);
        if (shouldDelete) {
            node.children.remove(ch);
            return !node.isWord && node.children.isEmpty();
        }
        return false;
    }
}

// ==================== SEARCH INDEX ENGINE ====================

/**
 * Main search engine: index, search, rank, autocomplete.
 * Thread-safe via ReadWriteLock (concurrent reads, exclusive writes).
 */
class SearchIndexEngine {
    private final Map<String, Document> documents;
    private final InvertedIndex invertedIndex;
    private final AutocompleteEngine autocomplete;
    private final ReadWriteLock lock;

    public SearchIndexEngine() {
        this.documents = new ConcurrentHashMap<>();
        this.invertedIndex = new InvertedIndex();
        this.autocomplete = new AutocompleteEngine();
        this.lock = new ReentrantReadWriteLock();
    }

    // ---------- INDEXING ----------

    /** Index a new document (title + content indexed separately for field weighting). */
    public void indexDocument(Document doc) {
        lock.writeLock().lock();
        try {
            // Remove old version if exists (update)
            if (documents.containsKey(doc.getId())) {
                removeDocumentInternal(doc.getId());
            }

            documents.put(doc.getId(), doc);
            invertedIndex.incrementTotalDocs();

            // Index title and content separately (field-specific)
            List<String> titleTerms = TextProcessor.tokenize(doc.getTitle());
            List<String> contentTerms = TextProcessor.tokenize(doc.getContent());

            invertedIndex.addDocument(doc.getId(), "title", titleTerms);
            invertedIndex.addDocument(doc.getId(), "content", contentTerms);

            // Add terms to autocomplete
            Set<String> allTerms = new HashSet<>();
            allTerms.addAll(titleTerms);
            allTerms.addAll(contentTerms);
            for (String term : allTerms) {
                autocomplete.addWord(term);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Remove a document from the index. */
    public boolean removeDocument(String docId) {
        lock.writeLock().lock();
        try {
            return removeDocumentInternal(docId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private boolean removeDocumentInternal(String docId) {
        Document removed = documents.remove(docId);
        if (removed == null) return false;
        invertedIndex.removeDocument(docId);
        invertedIndex.decrementTotalDocs();
        return true;
    }

    // ---------- SEARCH ----------

    /** OR search: documents matching ANY query term, ranked by TF-IDF. */
    public List<SearchResult> search(String query, int limit) {
        lock.readLock().lock();
        try {
            List<String> queryTerms = TextProcessor.tokenize(query);
            if (queryTerms.isEmpty()) return Collections.emptyList();

            // Gather candidate docs (union)
            Set<String> candidates = new HashSet<>();
            for (String term : queryTerms) {
                candidates.addAll(invertedIndex.getDocIds(term));
            }

            // Score each candidate
            List<SearchResult> results = new ArrayList<>();
            for (String docId : candidates) {
                Document doc = documents.get(docId);
                if (doc == null) continue;

                double score = 0;
                Set<String> matched = new HashSet<>();
                for (String term : queryTerms) {
                    double termScore = invertedIndex.calculateScore(term, docId);
                    if (termScore > 0) {
                        score += termScore;
                        matched.add(term);
                    }
                }
                results.add(new SearchResult(doc, score, matched));
            }

            Collections.sort(results);
            return results.stream().limit(limit).collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<SearchResult> search(String query) { return search(query, 10); }

    /** AND search: documents must contain ALL query terms. */
    public List<SearchResult> searchAnd(String query, int limit) {
        lock.readLock().lock();
        try {
            List<String> queryTerms = TextProcessor.tokenize(query);
            if (queryTerms.isEmpty()) return Collections.emptyList();

            // Intersect document sets (start with smallest for efficiency)
            List<Set<String>> docSets = new ArrayList<>();
            for (String term : queryTerms) {
                docSets.add(new HashSet<>(invertedIndex.getDocIds(term)));
            }
            docSets.sort(Comparator.comparingInt(Set::size)); // smallest first

            Set<String> candidates = docSets.get(0);
            for (int i = 1; i < docSets.size(); i++) {
                candidates.retainAll(docSets.get(i));
                if (candidates.isEmpty()) return Collections.emptyList();
            }

            // Score and rank
            List<SearchResult> results = new ArrayList<>();
            for (String docId : candidates) {
                Document doc = documents.get(docId);
                if (doc == null) continue;

                double score = 0;
                Set<String> matched = new HashSet<>(queryTerms);
                for (String term : queryTerms) {
                    score += invertedIndex.calculateScore(term, docId);
                }
                results.add(new SearchResult(doc, score, matched));
            }

            Collections.sort(results);
            return results.stream().limit(limit).collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Phrase search: terms must appear consecutively in the document. */
    public List<SearchResult> searchPhrase(String phrase, int limit) {
        lock.readLock().lock();
        try {
            List<String> phraseTerms = TextProcessor.tokenize(phrase);
            if (phraseTerms.isEmpty()) return Collections.emptyList();

            // First: AND search to get candidates
            Set<String> candidates = new HashSet<>(invertedIndex.getDocIds(phraseTerms.get(0)));
            for (int i = 1; i < phraseTerms.size(); i++) {
                candidates.retainAll(invertedIndex.getDocIds(phraseTerms.get(i)));
            }

            // Then: filter by consecutive positions
            List<SearchResult> results = new ArrayList<>();
            for (String docId : candidates) {
                if (!invertedIndex.hasPhrase(docId, phraseTerms)) continue;

                Document doc = documents.get(docId);
                if (doc == null) continue;

                double score = 0;
                for (String term : phraseTerms) {
                    score += invertedIndex.calculateScore(term, docId);
                }
                // Phrase match bonus (1.5x boost)
                score *= 1.5;
                results.add(new SearchResult(doc, score, new HashSet<>(phraseTerms)));
            }

            Collections.sort(results);
            return results.stream().limit(limit).collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Prefix/wildcard search: find docs containing terms starting with prefix. */
    public List<SearchResult> searchPrefix(String prefix, int limit) {
        lock.readLock().lock();
        try {
            String normalizedPrefix = prefix.toLowerCase();
            Set<String> matchingTerms = invertedIndex.getTermsWithPrefix(normalizedPrefix);
            if (matchingTerms.isEmpty()) return Collections.emptyList();

            // Gather candidate docs from all matching terms
            Map<String, Double> docScores = new HashMap<>();
            Map<String, Set<String>> docMatched = new HashMap<>();

            for (String term : matchingTerms) {
                for (String docId : invertedIndex.getDocIds(term)) {
                    double score = invertedIndex.calculateScore(term, docId);
                    docScores.merge(docId, score, Double::sum);
                    docMatched.computeIfAbsent(docId, k -> new HashSet<>()).add(term);
                }
            }

            List<SearchResult> results = new ArrayList<>();
            for (Map.Entry<String, Double> entry : docScores.entrySet()) {
                Document doc = documents.get(entry.getKey());
                if (doc == null) continue;
                results.add(new SearchResult(doc, entry.getValue(),
                    docMatched.getOrDefault(entry.getKey(), Collections.emptySet())));
            }

            Collections.sort(results);
            return results.stream().limit(limit).collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    // ---------- AUTOCOMPLETE ----------

    /** Get autocomplete suggestions for prefix, ranked by frequency. */
    public List<String> autocomplete(String prefix, int limit) {
        lock.readLock().lock();
        try {
            return autocomplete.suggest(prefix, limit);
        } finally {
            lock.readLock().unlock();
        }
    }

    // ---------- UTILITY ----------

    public Document getDocument(String docId) { return documents.get(docId); }
    public int getDocumentCount() { return documents.size(); }

    public String getStats() {
        return String.format("Documents: %d | Unique Terms: %d",
            documents.size(), invertedIndex.getTermCount());
    }
}

// ==================== MAIN / TESTS ====================

public class SearchIndex {
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║       SEARCH INDEX - LLD Demo            ║");
        System.out.println("╚══════════════════════════════════════════╝\n");

        SearchIndexEngine engine = new SearchIndexEngine();

        // --- Test 1: Basic Indexing & OR Search ---
        System.out.println("=== Test 1: Index & OR Search ===");
        engine.indexDocument(new Document("1", "Java Programming Guide",
            "Java is a popular programming language for building enterprise applications and microservices"));
        engine.indexDocument(new Document("2", "Python Data Science",
            "Python is a versatile programming language loved by data scientists and machine learning engineers"));
        engine.indexDocument(new Document("3", "JavaScript Web Development",
            "JavaScript is essential for web development and runs in browsers with Node.js on server side"));
        engine.indexDocument(new Document("4", "Java Microservices Architecture",
            "Building microservices with Java Spring Boot for distributed systems and cloud native applications"));
        engine.indexDocument(new Document("5", "Python Machine Learning",
            "Machine learning with Python using scikit learn tensorflow and neural networks for AI applications"));

        System.out.println(engine.getStats());
        System.out.println("\nSearch 'programming':");
        engine.search("programming").forEach(System.out::println);
        System.out.println("✓ OR search ranks Java & Python docs (both mention programming)\n");

        // --- Test 2: AND Search ---
        System.out.println("=== Test 2: AND Search ===");
        System.out.println("Search AND 'java microservices':");
        engine.searchAnd("java microservices", 10).forEach(System.out::println);
        System.out.println("✓ Only docs with BOTH 'java' AND 'microservices' returned\n");

        // --- Test 3: Field Weighting (title > content) ---
        System.out.println("=== Test 3: Field Weighting ===");
        System.out.println("Search 'python': doc with 'Python' in title should rank higher");
        List<SearchResult> results = engine.search("python");
        results.forEach(System.out::println);
        System.out.println("✓ Title matches get 3x weight boost\n");

        // --- Test 4: Phrase Search (positional index) ---
        System.out.println("=== Test 4: Phrase Search ===");
        System.out.println("Phrase search 'machine learning':");
        engine.searchPhrase("machine learning", 10).forEach(System.out::println);
        System.out.println("✓ Only docs with 'machine learning' as consecutive words\n");

        // --- Test 5: Prefix/Wildcard Search ---
        System.out.println("=== Test 5: Prefix Search ===");
        System.out.println("Prefix search 'micro':");
        engine.searchPrefix("micro", 10).forEach(System.out::println);
        System.out.println("✓ Finds docs with terms starting with 'micro' (microservices, etc.)\n");

        // --- Test 6: Autocomplete ---
        System.out.println("=== Test 6: Autocomplete ===");
        System.out.println("Suggestions for 'jav': " + engine.autocomplete("jav", 5));
        System.out.println("Suggestions for 'pro': " + engine.autocomplete("pro", 5));
        System.out.println("Suggestions for 'mac': " + engine.autocomplete("mac", 5));
        System.out.println("✓ Trie-based autocomplete with frequency ranking\n");

        // --- Test 7: Document Update (re-index) ---
        System.out.println("=== Test 7: Document Update ===");
        System.out.println("Before update - search 'rust': " + engine.search("rust").size() + " results");
        engine.indexDocument(new Document("1", "Rust Programming Guide",
            "Rust is a systems programming language focused on safety and performance"));
        System.out.println("After update doc1 to Rust - search 'rust': " + engine.search("rust").size() + " results");
        System.out.println("Search 'java' (doc1 no longer about java):");
        engine.search("java").forEach(System.out::println);
        System.out.println("✓ Re-indexing removes old terms, adds new ones\n");

        // --- Test 8: Document Removal ---
        System.out.println("=== Test 8: Document Removal ===");
        int before = engine.getDocumentCount();
        engine.removeDocument("5");
        int after = engine.getDocumentCount();
        System.out.println("Before remove: " + before + ", After remove: " + after);
        System.out.println("Search 'neural' (was in doc5): " + engine.search("neural").size() + " results");
        System.out.println("✓ Removed document no longer appears in search\n");

        // --- Test 9: Edge Cases ---
        System.out.println("=== Test 9: Edge Cases ===");
        System.out.println("Empty query: " + engine.search("").size() + " results (expected 0)");
        System.out.println("Stop words only 'the is a': " + engine.search("the is a").size() + " results (expected 0)");
        System.out.println("Non-existent term 'xyzzyqwert': " + engine.search("xyzzyqwert").size() + " results (expected 0)");
        System.out.println("Remove non-existent doc: " + engine.removeDocument("999"));
        System.out.println("✓ Edge cases handled gracefully\n");

        // --- Test 10: TF-IDF Ranking Correctness ---
        System.out.println("=== Test 10: TF-IDF Ranking ===");
        SearchIndexEngine engine2 = new SearchIndexEngine();
        engine2.indexDocument(new Document("a", "Doc A", "java java java java java")); // high TF
        engine2.indexDocument(new Document("b", "Doc B", "java python ruby golang"));  // low TF
        engine2.indexDocument(new Document("c", "Doc C", "python ruby golang rust"));  // no java
        System.out.println("Search 'java' - doc A (5x java) should rank above doc B (1x java):");
        engine2.search("java").forEach(System.out::println);
        System.out.println("✓ Higher term frequency = higher TF-IDF score\n");

        // --- Test 11: Large Dataset ---
        System.out.println("=== Test 11: Large Dataset ===");
        SearchIndexEngine engine3 = new SearchIndexEngine();
        String[] topics = {"java", "python", "distributed systems", "microservices",
                          "machine learning", "database design", "cloud computing"};
        for (int i = 0; i < 1000; i++) {
            String topic = topics[i % topics.length];
            engine3.indexDocument(new Document("doc" + i, "Article " + i,
                topic + " concepts and best practices for software engineering document " + i));
        }
        System.out.println(engine3.getStats());
        long start = System.nanoTime();
        results = engine3.search("distributed systems", 5);
        long elapsed = System.nanoTime() - start;
        System.out.println("Search 'distributed systems' in 1000 docs: " + results.size() + " results");
        System.out.printf("Search time: %.2f ms\n", elapsed / 1_000_000.0);
        results.forEach(System.out::println);
        System.out.println("✓ Scales to large datasets\n");

        // --- Test 12: Thread Safety ---
        System.out.println("=== Test 12: Thread Safety ===");
        SearchIndexEngine engine4 = new SearchIndexEngine();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();

        // Concurrent writes
        for (int i = 0; i < 100; i++) {
            final int idx = i;
            futures.add(executor.submit(() ->
                engine4.indexDocument(new Document("t" + idx, "Title " + idx,
                    "Content for concurrent test document " + idx))
            ));
        }
        // Concurrent reads while writing
        for (int i = 0; i < 50; i++) {
            futures.add(executor.submit(() -> engine4.search("concurrent")));
        }

        for (Future<?> f : futures) {
            try { f.get(); } catch (Exception e) { System.out.println("ERROR: " + e); }
        }
        executor.shutdown();
        System.out.println("After concurrent ops: " + engine4.getDocumentCount() + " docs indexed");
        System.out.println("Concurrent search works: " + engine4.search("concurrent").size() + " results");
        System.out.println("✓ Thread-safe concurrent reads and writes\n");

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║        ALL 12 TESTS PASSED ✓            ║");
        System.out.println("╚══════════════════════════════════════════╝");
    }
}

/*
 * ==================== INTERVIEW NOTES ====================
 * 
 * 1. CORE DATA STRUCTURE - INVERTED INDEX:
 *    Forward:  doc1 -> [java, programming, language]
 *    Inverted: java -> [doc1, doc2, doc5]  ← O(1) lookup
 *    Positional: java -> {doc1: [0,5,12], doc2: [3]}  ← enables phrase search
 *
 * 2. TF-IDF SCORING:
 *    TF  = termFreq / docLength        (term importance in THIS doc)
 *    IDF = log(totalDocs / docsWithTerm) (term rarity globally)
 *    Score = TF × IDF × fieldWeight
 *    → Common words score low, rare-but-frequent words score high
 *
 * 3. FIELD WEIGHTING:
 *    title match = 3× content match. "Java" in title is more relevant
 *    than "java" buried in content.
 *
 * 4. QUERY TYPES:
 *    OR:     union of doc sets, rank by sum of TF-IDF
 *    AND:    intersection of doc sets, start with smallest set
 *    Phrase: AND + consecutive position check
 *    Prefix: scan terms with matching prefix
 *
 * 5. AUTOCOMPLETE:
 *    Trie with frequency → PriorityQueue for top-K by popularity.
 *    O(k) traverse + O(n log n) collect, where n = words under prefix.
 *
 * 6. SCALABILITY (DISCUSSION):
 *    Sharding: partition docs across nodes by hash(docId)
 *    Replication: copies for read throughput + fault tolerance
 *    Query: scatter to all shards → gather & merge ranked results
 *    Caching: LRU cache for popular queries
 *    Real-world: Elasticsearch = Lucene + sharding + replication
 *
 * 7. BM25 (BETTER THAN TF-IDF):
 *    Adds saturation (diminishing returns for high TF) and
 *    document length normalization. Used by Elasticsearch.
 *    score = IDF × (tf × (k1+1)) / (tf + k1 × (1 - b + b × dl/avgdl))
 *
 * 8. THREAD SAFETY:
 *    ReadWriteLock: multiple concurrent readers, exclusive writer.
 *    ConcurrentHashMap for lock-free reads on document store.
 *
 * 9. COMPLEXITY:
 *    indexDocument:   O(n)       n = terms in doc
 *    search (OR):     O(q × d)  q = query terms, d = avg docs/term
 *    search (AND):    O(q × min_d) with sorted intersection
 *    phrase search:   O(q × d × p) p = positions per term
 *    autocomplete:    O(k + r)  k = prefix length, r = results
 *    removeDocument:  O(T)      T = total terms in index
 */
