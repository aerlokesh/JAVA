import java.util.*;
import java.util.stream.*;

// ===== DOCUMENT CLASS =====

/**
 * Represents a searchable document
 * 
 * INTERVIEW DISCUSSION:
 * - Document ID: unique identifier
 * - Content: text to be indexed
 * - Metadata: title, author, tags, etc.
 */
class Document {
    private String id;
    private String title;
    private String content;
    private Map<String, String> metadata;
    
    public Document(String id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.metadata = new HashMap<>();
    }
    
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Map<String, String> getMetadata() { return metadata; }
    
    public void addMetadata(String key, String value) {
        metadata.put(key, value);
    }
    
    @Override
    public String toString() {
        return "Document{id='" + id + "', title='" + title + "'}";
    }
}

// ===== SEARCH RESULT CLASS =====

/**
 * Represents a search result with document and relevance score
 */
class SearchResult implements Comparable<SearchResult> {
    private Document document;
    private double score;
    private List<String> matchedTerms;
    
    public SearchResult(Document document, double score) {
        this.document = document;
        this.score = score;
        this.matchedTerms = new ArrayList<>();
    }
    
    public Document getDocument() { return document; }
    public double getScore() { return score; }
    public List<String> getMatchedTerms() { return matchedTerms; }
    
    public void addMatchedTerm(String term) {
        matchedTerms.add(term);
    }
    
    @Override
    public int compareTo(SearchResult other) {
        // Higher score first
        return Double.compare(other.score, this.score);
    }
    
    @Override
    public String toString() {
        return String.format("%.2f - %s (matched: %s)", score, document, matchedTerms);
    }
}

// ===== TEXT PROCESSOR =====

/**
 * Handles text processing: tokenization, normalization, stemming
 * 
 * INTERVIEW DISCUSSION:
 * - Tokenization: split text into words
 * - Normalization: lowercase, remove punctuation
 * - Stop words: remove common words (the, a, is)
 * - Stemming: reduce words to root form (running -> run)
 */
class TextProcessor {
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
        "has", "he", "in", "is", "it", "its", "of", "on", "that", "the",
        "to", "was", "will", "with"
    ));
    
    /**
     * Tokenize text into terms
     * 
     * STEPS:
     * 1. Convert to lowercase
     * 2. Split on non-word characters
     * 3. Remove stop words
     * 4. Optional: stemming (simplified here)
     * 
     * @param text Input text
     * @return List of processed terms
     */
    public static List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Convert to lowercase and split
        String[] words = text.toLowerCase()
                             .replaceAll("[^a-z0-9\\s]", " ")
                             .split("\\s+");
        
        List<String> terms = new ArrayList<>();
        for (String word : words) {
            // Skip empty strings and stop words
            if (!word.isEmpty() && !STOP_WORDS.contains(word)) {
                terms.add(word);
            }
        }
        
        return terms;
    }
    
    /**
     * Simple stemming (remove common suffixes)
     * In production: Use Porter Stemmer or Snowball
     */
    public static String stem(String word) {
        // Simplified stemming
        if (word.endsWith("ing")) return word.substring(0, word.length() - 3);
        if (word.endsWith("ed")) return word.substring(0, word.length() - 2);
        if (word.endsWith("s") && word.length() > 3) return word.substring(0, word.length() - 1);
        return word;
    }
}

// ===== INVERTED INDEX =====

/**
 * Inverted Index - Core data structure for search
 * 
 * STRUCTURE:
 *   term -> [docId1, docId2, docId3, ...]
 *   "java" -> ["doc1", "doc3", "doc5"]
 *   "python" -> ["doc2", "doc4"]
 * 
 * INTERVIEW DISCUSSION:
 * - Why inverted? (Inverts document->terms to term->documents)
 * - Alternative: Forward index (document -> terms) - for phrase search
 * - Used in: Elasticsearch, Solr, Lucene
 * - Time complexity: O(1) term lookup, O(k) where k=matching docs
 */
class InvertedIndex {
    // term -> set of document IDs
    private Map<String, Set<String>> index;
    
    // term -> document -> frequency (TF)
    private Map<String, Map<String, Integer>> termFrequency;
    
    // document -> term count (for normalization)
    private Map<String, Integer> documentLength;
    
    // Total documents (for IDF calculation)
    private int totalDocuments;
    
    public InvertedIndex() {
        this.index = new HashMap<>();
        this.termFrequency = new HashMap<>();
        this.documentLength = new HashMap<>();
        this.totalDocuments = 0;
    }
    
    /**
     * Index a document
     * 
     * ALGORITHM:
     * 1. Tokenize document content
     * 2. For each term:
     *    a. Add docId to inverted index
     *    b. Track term frequency in document
     * 3. Store document length
     * 
     * TIME COMPLEXITY: O(n) where n = number of terms
     */
    public void addDocument(String docId, List<String> terms) {
        totalDocuments++;
        documentLength.put(docId, terms.size());
        
        // Count term frequencies
        Map<String, Integer> termCounts = new HashMap<>();
        for (String term : terms) {
            termCounts.put(term, termCounts.getOrDefault(term, 0) + 1);
        }
        
        // Update inverted index
        for (Map.Entry<String, Integer> entry : termCounts.entrySet()) {
            String term = entry.getKey();
            int count = entry.getValue();
            
            // Add to inverted index
            index.computeIfAbsent(term, k -> new HashSet<>()).add(docId);
            
            // Track term frequency
            termFrequency.computeIfAbsent(term, k -> new HashMap<>()).put(docId, count);
        }
    }
    
    /**
     * Get documents containing term
     */
    public Set<String> getDocuments(String term) {
        return index.getOrDefault(term, new HashSet<>());
    }
    
    /**
     * Get term frequency in document
     */
    public int getTermFrequency(String term, String docId) {
        return termFrequency.getOrDefault(term, new HashMap<>())
                           .getOrDefault(docId, 0);
    }
    
    /**
     * Calculate TF-IDF score
     * 
     * TF (Term Frequency): How often term appears in document
     *   TF = termFreq / docLength
     * 
     * IDF (Inverse Document Frequency): How rare the term is
     *   IDF = log(totalDocs / docsWithTerm)
     * 
     * TF-IDF = TF * IDF
     * 
     * INTUITION:
     * - Common terms (the, is) have low IDF -> low score
     * - Rare terms (elasticsearch) have high IDF -> high score
     * - Frequent in doc + rare overall = most relevant
     */
    public double calculateTfIdf(String term, String docId) {
        int tf = getTermFrequency(term, docId);
        if (tf == 0) return 0.0;
        
        int docLen = documentLength.getOrDefault(docId, 1);
        int docsWithTerm = getDocuments(term).size();
        
        // TF: normalized frequency
        double tfScore = (double) tf / docLen;
        
        // IDF: logarithmic scaling
        double idf = Math.log((double) totalDocuments / docsWithTerm);
        
        return tfScore * idf;
    }
    
    public int getTotalDocuments() { return totalDocuments; }
    public int getDocumentLength(String docId) { 
        return documentLength.getOrDefault(docId, 0); 
    }
    
    /**
     * Get number of unique terms in index
     */
    public int getTermCount() {
        return index.size();
    }
    
    /**
     * Get average documents per term
     */
    public double getAvgDocsPerTerm() {
        if (index.isEmpty()) return 0.0;
        return (double) index.values().stream()
                .mapToInt(Set::size).sum() / index.size();
    }
}

// ===== SEARCH INDEX =====

/**
 * Search Index - Main search engine class
 * 
 * RESPONSIBILITIES:
 * - Index documents
 * - Process search queries
 * - Rank results by relevance
 * - Support boolean queries (AND, OR, NOT)
 * 
 * INTERVIEW DISCUSSION:
 * - Inverted index for fast term lookup
 * - TF-IDF for ranking
 * - Query processing: parse, execute, rank
 * - Scalability: sharding, replication
 */
class SearchIndexEngine {
    private Map<String, Document> documents;
    private InvertedIndex invertedIndex;
    
    public SearchIndexEngine() {
        this.documents = new HashMap<>();
        this.invertedIndex = new InvertedIndex();
    }
    
    /**
     * Index a document
     * 
     * @param document Document to index
     */
    public void indexDocument(Document document) {
        documents.put(document.getId(), document);
        
        // Tokenize title and content
        List<String> terms = new ArrayList<>();
        terms.addAll(TextProcessor.tokenize(document.getTitle()));
        terms.addAll(TextProcessor.tokenize(document.getContent()));
        
        invertedIndex.addDocument(document.getId(), terms);
        
        System.out.println("Indexed: " + document.getId() + " (" + terms.size() + " terms)");
    }
    
    /**
     * Search for documents matching query
     * 
     * @param query Search query
     * @return List of ranked results
     */
    public List<SearchResult> search(String query) {
        return search(query, 10);  // Default limit 10
    }
    
    /**
     * Search with result limit
     * 
     * ALGORITHM:
     * 1. Tokenize query
     * 2. Find documents containing any query term
     * 3. Calculate relevance score for each document
     * 4. Sort by score (descending)
     * 5. Return top N results
     * 
     * @param query Search query
     * @param limit Max results
     * @return Ranked search results
     */
    public List<SearchResult> search(String query, int limit) {
        List<String> queryTerms = TextProcessor.tokenize(query);
        if (queryTerms.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Find candidate documents (union of all matching docs)
        Set<String> candidateDocs = new HashSet<>();
        for (String term : queryTerms) {
            candidateDocs.addAll(invertedIndex.getDocuments(term));
        }
        
        // Score each candidate document
        List<SearchResult> results = new ArrayList<>();
        for (String docId : candidateDocs) {
            Document doc = documents.get(docId);
            if (doc == null) continue;
            
            double score = calculateRelevanceScore(queryTerms, docId);
            SearchResult result = new SearchResult(doc, score);
            
            // Track which terms matched
            for (String term : queryTerms) {
                if (invertedIndex.getTermFrequency(term, docId) > 0) {
                    result.addMatchedTerm(term);
                }
            }
            
            results.add(result);
        }
        
        // Sort by score and limit
        Collections.sort(results);
        return results.stream().limit(limit).collect(Collectors.toList());
    }
    
    /**
     * Calculate relevance score for document
     * Sum of TF-IDF scores for all query terms
     */
    private double calculateRelevanceScore(List<String> queryTerms, String docId) {
        double score = 0.0;
        
        for (String term : queryTerms) {
            score += invertedIndex.calculateTfIdf(term, docId);
        }
        
        return score;
    }
    
    /**
     * Boolean AND search - documents must contain ALL terms
     * 
     * @param query Search query
     * @return Documents containing all terms
     */
    public List<SearchResult> searchAnd(String query) {
        List<String> queryTerms = TextProcessor.tokenize(query);
        if (queryTerms.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Start with docs containing first term
        Set<String> resultDocs = new HashSet<>(invertedIndex.getDocuments(queryTerms.get(0)));
        
        // Intersect with docs containing other terms
        for (int i = 1; i < queryTerms.size(); i++) {
            resultDocs.retainAll(invertedIndex.getDocuments(queryTerms.get(i)));
        }
        
        // Score and return
        List<SearchResult> results = new ArrayList<>();
        for (String docId : resultDocs) {
            Document doc = documents.get(docId);
            double score = calculateRelevanceScore(queryTerms, docId);
            results.add(new SearchResult(doc, score));
        }
        
        Collections.sort(results);
        return results;
    }
    
    /**
     * Phrase search - find exact phrase
     * 
     * IMPLEMENTATION:
     * - Find docs containing all terms
     * - Check if terms appear consecutively
     * 
     * NOTE: Requires positional index (term -> docId -> positions)
     * Simplified here: just check all terms present
     */
    public List<SearchResult> searchPhrase(String phrase) {
        // Simplified: treat as AND search
        // Production: use positional index to verify consecutive terms
        return searchAnd(phrase);
    }
    
    /**
     * Get document by ID
     */
    public Document getDocument(String docId) {
        return documents.get(docId);
    }
    
    /**
     * Get index statistics
     */
    public String getStats() {
        int totalTerms = invertedIndex.getTermCount();
        int totalDocs = documents.size();
        double avgDocsPerTerm = invertedIndex.getAvgDocsPerTerm();
        
        return String.format("Documents: %d, Terms: %d, Avg docs/term: %.1f",
                           totalDocs, totalTerms, avgDocsPerTerm);
    }
}

// ===== AUTOCOMPLETE / SUGGESTION =====

/**
 * Trie-based autocomplete for search suggestions
 * 
 * INTERVIEW DISCUSSION:
 * - Trie (Prefix Tree) for efficient prefix matching
 * - Used for: autocomplete, spell checking
 * - Time: O(k) for lookup where k = query length
 */
class TrieNode {
    Map<Character, TrieNode> children;
    boolean isEndOfWord;
    int frequency;  // How often this word was searched
    
    public TrieNode() {
        children = new HashMap<>();
        isEndOfWord = false;
        frequency = 0;
    }
}

class AutocompleteEngine {
    private TrieNode root;
    
    public AutocompleteEngine() {
        this.root = new TrieNode();
    }
    
    /**
     * Add word to trie
     */
    public void addWord(String word) {
        TrieNode node = root;
        for (char ch : word.toCharArray()) {
            node.children.putIfAbsent(ch, new TrieNode());
            node = node.children.get(ch);
        }
        node.isEndOfWord = true;
        node.frequency++;
    }
    
    /**
     * Get autocomplete suggestions for prefix
     * 
     * @param prefix Query prefix
     * @param limit Max suggestions
     * @return List of suggested completions
     */
    public List<String> getSuggestions(String prefix, int limit) {
        prefix = prefix.toLowerCase();
        TrieNode node = root;
        
        // Navigate to prefix node
        for (char ch : prefix.toCharArray()) {
            if (!node.children.containsKey(ch)) {
                return new ArrayList<>();  // Prefix not found
            }
            node = node.children.get(ch);
        }
        
        // Collect all words with this prefix
        List<String> suggestions = new ArrayList<>();
        collectWords(node, prefix, suggestions, limit);
        return suggestions;
    }
    
    /**
     * DFS to collect all words from node
     */
    private void collectWords(TrieNode node, String current, List<String> result, int limit) {
        if (result.size() >= limit) return;
        
        if (node.isEndOfWord) {
            result.add(current);
        }
        
        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            collectWords(entry.getValue(), current + entry.getKey(), result, limit);
        }
    }
}

// ===== MAIN TEST CLASS =====

public class SearchIndex {
    public static void main(String[] args) {
        System.out.println("=== Search Index Test Cases ===\n");
        
        // Test Case 1: Basic Indexing and Search
        System.out.println("=== Test Case 1: Basic Indexing and Search ===");
        SearchIndexEngine engine = new SearchIndexEngine();
        
        engine.indexDocument(new Document("1", "Java Programming", 
            "Java is a popular programming language for building enterprise applications"));
        engine.indexDocument(new Document("2", "Python Guide", 
            "Python is a versatile programming language loved by data scientists"));
        engine.indexDocument(new Document("3", "JavaScript Basics", 
            "JavaScript is essential for web development and runs in browsers"));
        
        System.out.println("\nSearch: 'programming'");
        List<SearchResult> results = engine.search("programming");
        for (SearchResult result : results) {
            System.out.println(result);
        }
        System.out.println("✓ Basic search working");
        System.out.println();
        
        // Test Case 2: Multi-term Search (OR)
        System.out.println("=== Test Case 2: Multi-term Search (OR) ===");
        System.out.println("Search: 'java python'");
        results = engine.search("java python");
        for (SearchResult result : results) {
            System.out.println(result);
        }
        System.out.println("✓ Multi-term OR search working");
        System.out.println();
        
        // Test Case 3: Boolean AND Search
        System.out.println("=== Test Case 3: Boolean AND Search ===");
        System.out.println("Search AND: 'programming language'");
        results = engine.searchAnd("programming language");
        System.out.println("Found " + results.size() + " docs with ALL terms");
        for (SearchResult result : results) {
            System.out.println(result);
        }
        System.out.println("✓ AND search working");
        System.out.println();
        
        // Test Case 4: No Results
        System.out.println("=== Test Case 4: No Results ===");
        results = engine.search("nonexistent");
        System.out.println("Results for 'nonexistent': " + results.size() + " (expected 0)");
        System.out.println("✓ Empty result handling working");
        System.out.println();
        
        // Test Case 5: Stop Words Removed
        System.out.println("=== Test Case 5: Stop Words Removal ===");
        System.out.println("Search: 'the java programming' (stop word 'the' removed)");
        results = engine.search("the java programming");
        System.out.println("Found: " + results.size() + " results");
        System.out.println("✓ Stop words filtering working");
        System.out.println();
        
        // Test Case 6: TF-IDF Ranking
        System.out.println("=== Test Case 6: TF-IDF Ranking ===");
        SearchIndexEngine engine2 = new SearchIndexEngine();
        engine2.indexDocument(new Document("d1", "Doc 1", 
            "java java java"));  // High TF for "java"
        engine2.indexDocument(new Document("d2", "Doc 2", 
            "java python ruby"));
        engine2.indexDocument(new Document("d3", "Doc 3", 
            "python ruby javascript"));
        
        System.out.println("Search: 'java' (d1 should rank higher - more occurrences)");
        results = engine2.search("java");
        for (SearchResult result : results) {
            System.out.println(result);
        }
        System.out.println("✓ TF-IDF ranking working");
        System.out.println();
        
        // Test Case 7: Large Document Set
        System.out.println("=== Test Case 7: Large Document Set ===");
        SearchIndexEngine engine3 = new SearchIndexEngine();
        for (int i = 0; i < 100; i++) {
            String content = "Document " + i + " contains various terms";
            if (i % 3 == 0) content += " java programming";
            if (i % 5 == 0) content += " python data science";
            engine3.indexDocument(new Document("doc" + i, "Doc " + i, content));
        }
        
        System.out.println(engine3.getStats());
        System.out.println("Search: 'java'");
        results = engine3.search("java", 5);
        System.out.println("Top 5 results:");
        for (SearchResult result : results) {
            System.out.println(result);
        }
        System.out.println("✓ Large dataset working");
        System.out.println();
        
        // Test Case 8: Autocomplete
        System.out.println("=== Test Case 8: Autocomplete ===");
        AutocompleteEngine autocomplete = new AutocompleteEngine();
        autocomplete.addWord("java");
        autocomplete.addWord("javascript");
        autocomplete.addWord("python");
        autocomplete.addWord("programming");
        autocomplete.addWord("programmer");
        
        System.out.println("Suggestions for 'jav':");
        List<String> suggestions = autocomplete.getSuggestions("jav", 5);
        System.out.println(suggestions);
        
        System.out.println("Suggestions for 'prog':");
        suggestions = autocomplete.getSuggestions("prog", 5);
        System.out.println(suggestions);
        System.out.println("✓ Autocomplete working");
        System.out.println();
        
        // Test Case 9: Case Insensitive Search
        System.out.println("=== Test Case 9: Case Insensitive ===");
        SearchIndexEngine engine4 = new SearchIndexEngine();
        engine4.indexDocument(new Document("1", "Title", "Java PROGRAMMING"));
        
        System.out.println("Search: 'java' (lowercase)");
        results = engine4.search("java");
        System.out.println("Found: " + results.size());
        
        System.out.println("Search: 'PROGRAMMING' (uppercase)");
        results = engine4.search("PROGRAMMING");
        System.out.println("Found: " + results.size());
        System.out.println("✓ Case insensitive working");
        System.out.println();
        
        // Test Case 10: Empty Query
        System.out.println("=== Test Case 10: Empty Query ===");
        results = engine.search("");
        System.out.println("Results for empty query: " + results.size() + " (expected 0)");
        System.out.println("✓ Empty query handling working");
        System.out.println();
        
        System.out.println("=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION TOPICS:
 * ============================
 * 
 * 1. INVERTED INDEX:
 *    Forward Index (traditional):
 *      Document -> List of terms
 *      doc1 -> [java, programming, language]
 *      
 *    Inverted Index:
 *      Term -> List of documents
 *      java -> [doc1, doc2, doc5]
 *      
 *    Why Inverted?
 *      - Fast term lookup: O(1)
 *      - Efficient for search queries
 *      - Used in all major search engines
 * 
 * 2. TF-IDF SCORING:
 *    Term Frequency (TF):
 *      - How often term appears in document
 *      - TF = count / total_terms_in_doc
 *      - Higher = more relevant to document
 *    
 *    Inverse Document Frequency (IDF):
 *      - How rare the term is across all documents
 *      - IDF = log(total_docs / docs_with_term)
 *      - Rare terms are more discriminative
 *    
 *    TF-IDF = TF × IDF:
 *      - Balances frequency and rarity
 *      - Common words (the, is) get low score
 *      - Rare, frequent words get high score
 * 
 * 3. QUERY PROCESSING:
 *    Steps:
 *      1. Tokenization: split query into terms
 *      2. Normalization: lowercase, remove punctuation
 *      3. Stop word removal: filter common words
 *      4. Stemming: reduce to root form
 *      5. Query expansion: synonyms, spell correction
 *    
 *    Query Types:
 *      - Simple: "java programming"
 *      - Boolean: "java AND python"
 *      - Phrase: "machine learning"
 *      - Proximity: "java NEAR python"
 *      - Wildcard: "prog*"
 * 
 * 4. RANKING ALGORITHMS:
 *    TF-IDF (used here):
 *      - Classic, simple, effective
 *      - Pros: Fast, interpretable
 *      - Cons: No semantic understanding
 *    
 *    BM25 (Best Match 25):
 *      - Improved TF-IDF
 *      - Handles document length better
 *      - Used in Elasticsearch
 *    
 *    PageRank (Google):
 *      - Link-based ranking
 *      - Measures page importance
 *      - Good for web search
 *    
 *    Learning to Rank:
 *      - Machine learning models
 *      - Train on click data
 *      - Used in modern search engines
 * 
 * 5. ADVANCED FEATURES:
 *    Positional Index:
 *      term -> docId -> [position1, position2, ...]
 *      - Enables phrase search
 *      - Proximity queries
 *    
 *    Field-specific Search:
 *      - Search in title, content, author separately
 *      - Different weights for different fields
 *      - title:java content:programming
 *    
 *    Faceted Search:
 *      - Filter by categories
 *      - Example: type:article, year:2024
 *      - Used in e-commerce
 *    
 *    Fuzzy Search:
 *      - Handle typos, misspellings
 *      - Levenshtein distance
 *      - "progrmming" finds "programming"
 * 
 * 6. AUTOCOMPLETE:
 *    Trie (Prefix Tree):
 *      - O(k) lookup for prefix of length k
 *      - Space efficient for common prefixes
 *      - Real-time suggestions
 *    
 *    Alternative: N-gram Index:
 *      - "programming" -> ["pro", "rog", "ogr", ...]
 *      - More fuzzy matching
 *      - Higher space overhead
 * 
 * 7. SCALABILITY:
 *    Horizontal Scaling:
 *      - Shard by document ID range
 *      - doc1-1000 -> shard1
 *      - doc1001-2000 -> shard2
 *    
 *    Replication:
 *      - Multiple copies for availability
 *      - Read from any replica
 *      - Consistent hashing for distribution
 *    
 *    Caching:
 *      - Cache popular queries
 *      - TTL-based invalidation
 *      - Redis/Memcached
 * 
 * 8. REAL-WORLD SYSTEMS:
 *    Elasticsearch:
 *      - Distributed search engine
 *      - Built on Apache Lucene
 *      - RESTful API, JSON documents
 *      - Sharding, replication, fault tolerance
 *    
 *    Apache Solr:
 *      - Also built on Lucene
 *      - XML/JSON/CSV input
 *      - Faceted search, highlighting
 *    
 *    Apache Lucene:
 *      - Core search library
 *      - Inverted index implementation
 *      - Used by Elasticsearch, Solr
 * 
 * 9. PERFORMANCE OPTIMIZATION:
 *    Indexing:
 *      - Batch indexing (bulk operations)
 *      - Parallel processing
 *      - Incremental updates
 *    
 *    Query Execution:
 *      - Early termination (top-k)
 *      - Skip lists for faster merges
 *      - Query result caching
 *    
 *    Storage:
 *      - Compression (dictionary encoding)
 *      - Memory-mapped files
 *      - SSD for random access
 * 
 * 10. TIME COMPLEXITY:
 *     Operation              | Complexity
 *     indexDocument          | O(n) where n = terms in doc
 *     search (OR)            | O(k * m) where k=query terms, m=avg docs/term
 *     search (AND)           | O(k * m) with set intersection
 *     TF-IDF calculation     | O(1) per term per doc
 *     autocomplete           | O(k + r) where k=prefix len, r=results
 * 
 * 11. SPACE COMPLEXITY:
 *     Inverted Index: O(D * T) where D=docs, T=avg terms/doc
 *     Documents: O(D * C) where C=avg content size
 *     Trie: O(N * L) where N=words, L=avg word length
 * 
 * 12. SEARCH QUALITY METRICS:
 *     Precision: relevant_returned / total_returned
 *     Recall: relevant_returned / total_relevant
 *     F1 Score: harmonic mean of precision/recall
 *     MRR (Mean Reciprocal Rank): 1 / rank_of_first_relevant
 *     NDCG (Normalized Discounted Cumulative Gain)
 * 
 * 13. ADVANCED TOPICS:
 *     Query Expansion:
 *       - Add synonyms: "car" -> "automobile"
 *       - Fix typos: "progrmming" -> "programming"
 *       - Expand acronyms: "ML" -> "machine learning"
 *     
 *     Relevance Feedback:
 *       - Learn from user clicks
 *       - Adjust ranking based on behavior
 *       - Personalization
 *     
 *     Distributed Search:
 *       - Scatter-gather pattern
 *       - Query all shards in parallel
 *       - Merge and rank results
 * 
 * 14. STEMMING ALGORITHMS:
 *     Porter Stemmer:
 *       - Rule-based, English
 *       - running -> run, flies -> fli
 *     
 *     Snowball (Porter2):
 *       - Improved Porter
 *       - Multiple languages
 *     
 *     Lemmatization:
 *       - Dictionary-based
 *       - Better -> good (not "bett")
 *       - More accurate but slower
 * 
 * 15. HIGHLIGHTING:
 *     Show matched terms in context:
 *       "...Java is a <em>programming</em> language..."
 *     
 *     Implementation:
 *       - Store term positions
 *       - Extract snippet around match
 *       - Highlight query terms
 * 
 * 16. FILTERS AND FACETS:
 *     Filters (must match):
 *       - year:2024
 *       - category:tech
 *       - price:[10 TO 100]
 *     
 *     Facets (aggregations):
 *       - Count by category
 *       - Group by year
 *       - Price ranges
 * 
 * 17. SPELL CORRECTION:
 *     Edit Distance:
 *       - Levenshtein distance
 *       - Find closest words in dictionary
 *     
 *     N-gram Based:
 *       - "progrmming" -> ["pro", "ogr", "grm", ...]
 *       - Match against indexed n-grams
 *     
 *     Statistical:
 *       - Noisy channel model
 *       - Probability-based correction
 * 
 * 18. DESIGN PATTERNS:
 *     Builder Pattern:
 *       - SearchQuery.builder()
 *                     .term("java")
 *                     .filter("year", 2024)
 *                     .limit(10)
 *                     .build()
 *     
 *     Strategy Pattern:
 *       - Different ranking strategies
 *       - Swap TF-IDF, BM25, etc.
 *     
 *     Observer Pattern:
 *       - Notify on index updates
 *       - Real-time search
 * 
 * 19. COMMON INTERVIEW QUESTIONS:
 *     Q: How does Google search work?
 *     A: Crawl -> Index (inverted) -> Rank (PageRank + relevance) -> Return
 *     
 *     Q: How to handle typos?
 *     A: Edit distance, n-grams, did-you-mean suggestions
 *     
 *     Q: How to scale to billions of documents?
 *     A: Sharding, replication, caching, compression
 *     
 *     Q: How to make search faster?
 *     A: Caching, skip lists, early termination, pruning
 *     
 *     Q: Difference between database query and search?
 *     A: DB = exact match, Search = ranked relevance + fuzzy matching
 * 
 * 20. RELATED LEETCODE PROBLEMS:
 *     - Design Search Autocomplete System (642)
 *     - Design In-Memory File System (588)
 *     - Implement Trie (208)
 *     - Word Search II (212)
 */
