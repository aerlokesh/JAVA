import java.util.*;

// ===== DOMAIN =====

class File {
    private final String name;
    private final String extension;
    private final long sizeBytes;
    private final boolean isDirectory;
    private final List<File> children;  // only for directories
    
    // File constructor
    public File(String name, long sizeBytes) {
        this.name = name;
        this.extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
        this.sizeBytes = sizeBytes;
        this.isDirectory = false;
        this.children = Collections.emptyList();
    }
    
    // Directory constructor
    public File(String name, List<File> children) {
        this.name = name;
        this.extension = "";
        this.sizeBytes = 0;
        this.isDirectory = true;
        this.children = new ArrayList<>(children);
    }
    
    public String getName() { return name; }
    public String getExtension() { return extension; }
    public long getSizeBytes() { return sizeBytes; }
    public boolean isDirectory() { return isDirectory; }
    public List<File> getChildren() { return children; }
    
    public void addChild(File child) { if (isDirectory) children.add(child); }
    
    @Override
    public String toString() { return (isDirectory ? "[DIR] " : "") + name + (isDirectory ? "" : " (" + sizeBytes + "B)"); }
}

// ===== FILTER INTERFACE (Strategy Pattern) =====

/**
 * Filter — each filter checks one condition
 * Composable: AND multiple filters together
 */
interface Filter {
    boolean matches(File file);
}

// ===== FILTER IMPLEMENTATIONS =====

/**
 * Filter by file extension (e.g., "xml", "java", "txt")
 */
class ExtensionFilter implements Filter {
    private final String extension;
    
    public ExtensionFilter(String extension) {
        this.extension = extension.toLowerCase();
    }
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Compare file extension (case-insensitive)
     */
    @Override
    public boolean matches(File file) {
        // TODO: Implement
        // HINT: return file.getExtension().equalsIgnoreCase(extension);
        return false;
    }
}

/**
 * Filter by minimum file size
 */
class MinSizeFilter implements Filter {
    private final long minBytes;
    
    public MinSizeFilter(long minBytes) {
        this.minBytes = minBytes;
    }
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Return true if file size >= minBytes
     */
    @Override
    public boolean matches(File file) {
        // TODO: Implement
        // HINT: return file.getSizeBytes() >= minBytes;
        return false;
    }
}

/**
 * Filter by maximum file size
 */
class MaxSizeFilter implements Filter {
    private final long maxBytes;
    
    public MaxSizeFilter(long maxBytes) {
        this.maxBytes = maxBytes;
    }
    
    @Override
    public boolean matches(File file) {
        // TODO: Implement
        // HINT: return file.getSizeBytes() <= maxBytes;
        return false;
    }
}

/**
 * Filter by name contains (substring match)
 */
class NameContainsFilter implements Filter {
    private final String substring;
    
    public NameContainsFilter(String substring) {
        this.substring = substring.toLowerCase();
    }
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Check if file name contains substring (case-insensitive)
     */
    @Override
    public boolean matches(File file) {
        // TODO: Implement
        // HINT: return file.getName().toLowerCase().contains(substring);
        return false;
    }
}

/**
 * Filter: only files (not directories)
 */
class FileOnlyFilter implements Filter {
    @Override
    public boolean matches(File file) {
        // TODO: Implement
        // HINT: return !file.isDirectory();
        return false;
    }
}

/**
 * Filter: only directories
 */
class DirectoryOnlyFilter implements Filter {
    @Override
    public boolean matches(File file) {
        // TODO: Implement
        // HINT: return file.isDirectory();
        return false;
    }
}

/**
 * AND filter — all sub-filters must match
 * Allows composing: find -name "*.xml" -size +1000
 */
class AndFilter implements Filter {
    private final List<Filter> filters;
    
    public AndFilter(Filter... filters) {
        this.filters = Arrays.asList(filters);
    }
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Return true only if ALL filters match
     */
    @Override
    public boolean matches(File file) {
        // TODO: Implement
        // HINT: for (Filter f : filters) {
        //     if (!f.matches(file)) return false;
        // }
        // HINT: return true;
        return false;
    }
}

/**
 * OR filter — any sub-filter matches
 */
class OrFilter implements Filter {
    private final List<Filter> filters;
    
    public OrFilter(Filter... filters) {
        this.filters = Arrays.asList(filters);
    }
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Return true if ANY filter matches
     */
    @Override
    public boolean matches(File file) {
        // TODO: Implement
        // HINT: for (Filter f : filters) {
        //     if (f.matches(file)) return true;
        // }
        // HINT: return false;
        return false;
    }
}

/**
 * NOT filter — inverts another filter
 */
class NotFilter implements Filter {
    private final Filter inner;
    
    public NotFilter(Filter inner) {
        this.inner = inner;
    }
    
    @Override
    public boolean matches(File file) {
        // TODO: Implement
        // HINT: return !inner.matches(file);
        return false;
    }
}

// ===== FIND COMMAND =====

/**
 * Linux Find Command - Low Level Design (LLD)
 * 
 * PROBLEM (Amazon Onsite): Implement a simplified Linux `find` command that:
 * 1. Recursively searches a directory tree
 * 2. Applies composable filters (extension, size, name, type)
 * 3. Returns matching files
 * 
 * Examples:
 *   find / -name "*.xml"                     → ExtensionFilter("xml")
 *   find / -name "*.xml" -size +1000         → AndFilter(ExtensionFilter, MinSizeFilter)
 *   find / -name "*.xml" -or -name "*.java"  → OrFilter(ExtensionFilter, ExtensionFilter)
 *   find / -type d                           → DirectoryOnlyFilter
 *   find / -not -name "*.log"                → NotFilter(ExtensionFilter)
 * 
 * KEY PATTERNS:
 * - Strategy: each Filter is a strategy
 * - Composite: AndFilter/OrFilter/NotFilter compose filters
 * - DFS: recursive directory traversal
 */
class FindCommand {
    
    /**
     * Find files matching filter in directory tree (DFS)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Create result list
     * 2. Call recursive helper: search(directory, filter, results)
     * 3. Return results
     */
    public List<File> find(File directory, Filter filter) {
        // TODO: Implement
        // HINT: List<File> results = new ArrayList<>();
        // HINT: search(directory, filter, results);
        // HINT: return results;
        return null;
    }
    
    /**
     * Recursive DFS search
     * 
     * IMPLEMENTATION HINTS:
     * 1. If current file matches filter → add to results
     * 2. If current file is directory → recurse into each child
     */
    private void search(File file, Filter filter, List<File> results) {
        // TODO: Implement
        // HINT: if (filter.matches(file)) results.add(file);
        // HINT: if (file.isDirectory()) {
        //     for (File child : file.getChildren()) {
        //         search(child, filter, results);
        //     }
        // }
    }
}

// ===== BUILDER (fluent API for composing filters) =====

class FindBuilder {
    private final List<Filter> filters = new ArrayList<>();
    
    public FindBuilder extension(String ext) { filters.add(new ExtensionFilter(ext)); return this; }
    public FindBuilder minSize(long bytes) { filters.add(new MinSizeFilter(bytes)); return this; }
    public FindBuilder maxSize(long bytes) { filters.add(new MaxSizeFilter(bytes)); return this; }
    public FindBuilder nameContains(String sub) { filters.add(new NameContainsFilter(sub)); return this; }
    public FindBuilder filesOnly() { filters.add(new FileOnlyFilter()); return this; }
    public FindBuilder dirsOnly() { filters.add(new DirectoryOnlyFilter()); return this; }
    
    public Filter build() {
        if (filters.isEmpty()) return f -> true; // match all
        if (filters.size() == 1) return filters.get(0);
        return new AndFilter(filters.toArray(new Filter[0]));
    }
}

// ===== MAIN TEST CLASS =====

public class LinuxFindCommand {
    public static void main(String[] args) {
        System.out.println("=== Linux Find Command LLD ===\n");
        
        // Build file system tree
        File root = new File("root", Arrays.asList(
            new File("src", Arrays.asList(
                new File("Main.java", 2048),
                new File("Utils.java", 512),
                new File("Config.xml", 1024),
                new File("test", Arrays.asList(
                    new File("MainTest.java", 1500),
                    new File("data.csv", 50000)
                ))
            )),
            new File("docs", Arrays.asList(
                new File("README.md", 300),
                new File("design.xml", 5000),
                new File("notes.txt", 100)
            )),
            new File("build", Arrays.asList(
                new File("output.jar", 100000),
                new File("app.log", 200000),
                new File("debug.log", 150000)
            )),
            new File("config.xml", 256),
            new File("Makefile", 800)
        ));
        
        FindCommand find = new FindCommand();
        
        // Test 1: Find by extension — find / -name "*.xml"
        System.out.println("=== Test 1: find -name '*.xml' ===");
        List<File> xmlFiles = find.find(root, new ExtensionFilter("xml"));
        System.out.println("✓ XML files: " + (xmlFiles != null ? xmlFiles.size() : 0) + " (expect 3)");
        if (xmlFiles != null) xmlFiles.forEach(f -> System.out.println("    " + f));
        System.out.println();
        
        // Test 2: Find by extension — find / -name "*.java"
        System.out.println("=== Test 2: find -name '*.java' ===");
        List<File> javaFiles = find.find(root, new ExtensionFilter("java"));
        System.out.println("✓ Java files: " + (javaFiles != null ? javaFiles.size() : 0) + " (expect 3)");
        if (javaFiles != null) javaFiles.forEach(f -> System.out.println("    " + f));
        System.out.println();
        
        // Test 3: Find by min size — find / -size +10000
        System.out.println("=== Test 3: find -size +10000 ===");
        List<File> bigFiles = find.find(root, new MinSizeFilter(10000));
        System.out.println("✓ Files > 10KB: " + (bigFiles != null ? bigFiles.size() : 0) + " (expect 4)");
        if (bigFiles != null) bigFiles.forEach(f -> System.out.println("    " + f));
        System.out.println();
        
        // Test 4: AND — find / -name "*.xml" -size +500
        System.out.println("=== Test 4: find -name '*.xml' AND -size +500 ===");
        List<File> bigXml = find.find(root, new AndFilter(
            new ExtensionFilter("xml"), new MinSizeFilter(500)));
        System.out.println("✓ XML > 500B: " + (bigXml != null ? bigXml.size() : 0) + " (expect 2)");
        if (bigXml != null) bigXml.forEach(f -> System.out.println("    " + f));
        System.out.println();
        
        // Test 5: OR — find / -name "*.xml" -or -name "*.java"
        System.out.println("=== Test 5: find -name '*.xml' OR '*.java' ===");
        List<File> xmlOrJava = find.find(root, new OrFilter(
            new ExtensionFilter("xml"), new ExtensionFilter("java")));
        System.out.println("✓ XML or Java: " + (xmlOrJava != null ? xmlOrJava.size() : 0) + " (expect 6)");
        System.out.println();
        
        // Test 6: NOT — find / -not -name "*.log"
        System.out.println("=== Test 6: find -not -name '*.log' ===");
        List<File> notLog = find.find(root, new AndFilter(
            new FileOnlyFilter(), new NotFilter(new ExtensionFilter("log"))));
        System.out.println("✓ Non-log files: " + (notLog != null ? notLog.size() : 0) + " (expect 8)");
        System.out.println();
        
        // Test 7: Directories only — find / -type d
        System.out.println("=== Test 7: find -type d ===");
        List<File> dirs = find.find(root, new DirectoryOnlyFilter());
        System.out.println("✓ Directories: " + (dirs != null ? dirs.size() : 0) + " (expect 5)");
        if (dirs != null) dirs.forEach(f -> System.out.println("    " + f));
        System.out.println();
        
        // Test 8: Name contains — find / -name "*Test*"
        System.out.println("=== Test 8: find -name '*Test*' ===");
        List<File> testFiles = find.find(root, new NameContainsFilter("Test"));
        System.out.println("✓ Contains 'Test': " + (testFiles != null ? testFiles.size() : 0) + " (expect 1)");
        System.out.println();
        
        // Test 9: Builder API — find / -name "*.java" -size +1000 (files only)
        System.out.println("=== Test 9: Builder API ===");
        Filter filter = new FindBuilder().extension("java").minSize(1000).filesOnly().build();
        List<File> builderResult = find.find(root, filter);
        System.out.println("✓ Java > 1KB: " + (builderResult != null ? builderResult.size() : 0) + " (expect 2)");
        if (builderResult != null) builderResult.forEach(f -> System.out.println("    " + f));
        System.out.println();
        
        // Test 10: Size range — find / -size +100 -size -2000
        System.out.println("=== Test 10: Size Range (100-2000 bytes) ===");
        List<File> rangeFiles = find.find(root, new AndFilter(
            new FileOnlyFilter(), new MinSizeFilter(100), new MaxSizeFilter(2000)));
        System.out.println("✓ Files 100-2000B: " + (rangeFiles != null ? rangeFiles.size() : 0));
        if (rangeFiles != null) rangeFiles.forEach(f -> System.out.println("    " + f));
        
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION:
 * =====================
 * 
 * 1. KEY PATTERNS:
 *    Strategy: each Filter is interchangeable
 *    Composite: AndFilter/OrFilter/NotFilter compose filters into tree
 *    DFS: recursive tree traversal on file system
 * 
 * 2. WHY THIS DESIGN:
 *    Open/Closed: add new filters without modifying FindCommand
 *    Single Responsibility: each filter does one thing
 *    Composable: AND/OR/NOT can build any boolean expression
 * 
 * 3. COMPLEXITY:
 *    Time: O(N) where N = total files in tree (visit each once)
 *    Space: O(D) for recursion stack (D = max depth)
 * 
 * 4. FOLLOW-UPS:
 *    - Add regex filter for name
 *    - Add date filter (modified before/after)
 *    - Add permission filter (readable, writable)
 *    - Limit search depth (-maxdepth)
 *    - Sort results (-sort by name, size, date)
 *    - Parallel search (fork-join for large trees)
 * 
 * 5. REAL find COMMAND:
 *    find /path -name "*.java" -size +1M -type f -mtime -7
 *    Each flag = one Filter, all combined with AND by default
 */
