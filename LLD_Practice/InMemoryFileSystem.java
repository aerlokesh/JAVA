import java.util.*;
import java.util.stream.Collectors;

// ===== CUSTOM EXCEPTION CLASSES =====

/**
 * Exception thrown when path is not found
 * WHEN TO THROW:
 * - File or directory doesn't exist at given path
 * - Parent directory doesn't exist
 */
class PathNotFoundException extends Exception {
    private String path;
    
    public PathNotFoundException(String path) {
        super("Path not found: " + path);
        this.path = path;
    }
    
    public String getPath() { return path; }
}

/**
 * Exception thrown when path already exists
 * WHEN TO THROW:
 * - Creating file/directory that already exists
 * - Name collision
 */
class PathAlreadyExistsException extends Exception {
    private String path;
    
    public PathAlreadyExistsException(String path) {
        super("Path already exists: " + path);
        this.path = path;
    }
    
    public String getPath() { return path; }
}

/**
 * Exception thrown when operation is invalid for the node type
 * WHEN TO THROW:
 * - Listing contents of a file (not a directory)
 * - Writing content to a directory
 * - Creating child inside a file
 */
class InvalidOperationException extends Exception {
    public InvalidOperationException(String message) {
        super(message);
    }
}

/**
 * Exception thrown when path format is invalid
 * WHEN TO THROW:
 * - Empty path
 * - Path with invalid characters
 * - Relative path when absolute expected
 */
class InvalidPathException extends Exception {
    public InvalidPathException(String message) {
        super(message);
    }
}

// ===== ENUMS =====

/**
 * Type of file system entry
 */
enum FSNodeType {
    FILE,
    DIRECTORY
}

// ===== DOMAIN CLASSES =====

/**
 * Represents a node (file or directory) in the file system
 * 
 * INTERVIEW DISCUSSION:
 * - Why use Composite pattern? (Files and directories share same interface)
 * - Why TreeMap for children? (Alphabetical ordering, O(log n) lookup)
 * - How to represent file content? (String for simplicity, byte[] in real systems)
 */
class FSNode {
    String name;
    FSNodeType type;
    String content;                      // Only for files
    Map<String, FSNode> children;        // Only for directories (name -> child)
    FSNode parent;
    long createdAt;
    long modifiedAt;
    long size;                           // File: content length, Dir: number of children
    
    /**
     * Create a file node
     */
    public static FSNode createFile(String name, FSNode parent) {
        FSNode node = new FSNode();
        node.name = name;
        node.type = FSNodeType.FILE;
        node.content = "";
        node.children = null;
        node.parent = parent;
        node.createdAt = System.currentTimeMillis();
        node.modifiedAt = node.createdAt;
        node.size = 0;
        return node;
    }
    
    /**
     * Create a directory node
     */
    public static FSNode createDirectory(String name, FSNode parent) {
        FSNode node = new FSNode();
        node.name = name;
        node.type = FSNodeType.DIRECTORY;
        node.content = null;
        node.children = new TreeMap<>();  // TreeMap for alphabetical order
        node.parent = parent;
        node.createdAt = System.currentTimeMillis();
        node.modifiedAt = node.createdAt;
        node.size = 0;
        return node;
    }
    
    public boolean isFile() { return type == FSNodeType.FILE; }
    public boolean isDirectory() { return type == FSNodeType.DIRECTORY; }
    
    public String getPath() {
        if (parent == null) return "/";
        String parentPath = parent.getPath();
        return parentPath.equals("/") ? "/" + name : parentPath + "/" + name;
    }
    
    @Override
    public String toString() {
        if (isFile()) return name + " (file, " + size + " bytes)";
        return name + "/ (dir, " + (children != null ? children.size() : 0) + " items)";
    }
}

// ===== MAIN FILE SYSTEM CLASS =====

/**
 * In-Memory File System - Low Level Design (LLD)
 * 
 * PROBLEM STATEMENT:
 * Design an in-memory file system that can:
 * 1. Create files and directories
 * 2. Read and write file content
 * 3. List directory contents
 * 4. Delete files and directories
 * 5. Move/copy files
 * 6. Search for files by name/pattern
 * 7. Support path navigation (absolute paths)
 * 
 * REQUIREMENTS:
 * - Functional: mkdir, touch, write, read, ls, rm, mv, cp, find
 * - Non-Functional: Fast lookups, handle deep hierarchies, memory efficient
 * 
 * INTERVIEW HINTS:
 * - Discuss Composite pattern (file/dir share same interface)
 * - Talk about tree traversal (DFS for search, BFS for listing)
 * - Mention path parsing and validation
 * - Consider permissions, symlinks, hard links
 * - Discuss real FS concepts: inodes, blocks, journaling
 */
class FileSystem {
    private FSNode root;
    
    public FileSystem() {
        this.root = FSNode.createDirectory("/", null);
    }
    
    /**
     * Create a directory (like mkdir -p)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Parse the path into components: "/a/b/c" → ["a", "b", "c"]
     * 2. Start from root, traverse each component
     * 3. For each component: if exists and is dir, continue; if doesn't exist, create it
     * 4. If any component is a FILE, throw InvalidOperationException
     * 5. Return the final created/existing directory node
     * 
     * @param path Absolute path to create (e.g., "/home/user/docs")
     * @throws InvalidPathException if path is invalid
     * @throws InvalidOperationException if path component is a file
     */
    public void mkdir(String path) throws InvalidPathException, InvalidOperationException {
        // TODO: Implement
        // HINT: String[] parts = parsePath(path);
        // HINT: FSNode current = root;
        // HINT: for (String part : parts) {
        //     if (current.children.containsKey(part)) {
        //         FSNode child = current.children.get(part);
        //         if (child.isFile()) throw new InvalidOperationException(part + " is a file, not directory");
        //         current = child;
        //     } else {
        //         FSNode newDir = FSNode.createDirectory(part, current);
        //         current.children.put(part, newDir);
        //         current = newDir;
        //     }
        // }
    }
    
    /**
     * Create an empty file (like touch)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Parse path to get parent directory path and file name
     * 2. Navigate to parent directory (must exist)
     * 3. Check if file already exists
     * 4. Create file node and add to parent's children
     * 
     * @param path Absolute path to file (e.g., "/home/user/file.txt")
     * @throws InvalidPathException if path is invalid
     * @throws PathNotFoundException if parent directory doesn't exist
     * @throws PathAlreadyExistsException if file already exists
     */
    public void createFile(String path) throws InvalidPathException, PathNotFoundException, PathAlreadyExistsException {
        // TODO: Implement
        // HINT: String[] parts = parsePath(path);
        // HINT: String fileName = parts[parts.length - 1];
        // HINT: FSNode parentDir = navigateToParent(path);
        // HINT: if (parentDir.children.containsKey(fileName)) throw new PathAlreadyExistsException(path);
        // HINT: FSNode file = FSNode.createFile(fileName, parentDir);
        // HINT: parentDir.children.put(fileName, file);
    }
    
    /**
     * Write content to a file (overwrite)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Navigate to the file node
     * 2. Verify it's a file (not directory)
     * 3. Set content and update size/modifiedAt
     * 
     * @param path Path to file
     * @param content Content to write
     * @throws PathNotFoundException if file doesn't exist
     * @throws InvalidOperationException if path is a directory
     */
    public void writeFile(String path, String content) 
            throws PathNotFoundException, InvalidOperationException, InvalidPathException {
        // TODO: Implement
        // HINT: FSNode node = navigateTo(path);
        // HINT: if (node.isDirectory()) throw new InvalidOperationException("Cannot write to directory: " + path);
        // HINT: node.content = content;
        // HINT: node.size = content.length();
        // HINT: node.modifiedAt = System.currentTimeMillis();
    }
    
    /**
     * Append content to a file
     * 
     * IMPLEMENTATION HINTS:
     * 1. Navigate to file
     * 2. Append content to existing content
     * 3. Update size and modifiedAt
     * 
     * @param path Path to file
     * @param content Content to append
     * @throws PathNotFoundException if file doesn't exist
     * @throws InvalidOperationException if path is a directory
     */
    public void appendFile(String path, String content) 
            throws PathNotFoundException, InvalidOperationException, InvalidPathException {
        // TODO: Implement
        // HINT: FSNode node = navigateTo(path);
        // HINT: if (node.isDirectory()) throw new InvalidOperationException("Cannot append to directory");
        // HINT: node.content += content;
        // HINT: node.size = node.content.length();
        // HINT: node.modifiedAt = System.currentTimeMillis();
    }
    
    /**
     * Read file content
     * 
     * IMPLEMENTATION HINTS:
     * 1. Navigate to file
     * 2. Verify it's a file
     * 3. Return content
     * 
     * @param path Path to file
     * @return File content
     * @throws PathNotFoundException if file doesn't exist
     * @throws InvalidOperationException if path is a directory
     */
    public String readFile(String path) 
            throws PathNotFoundException, InvalidOperationException, InvalidPathException {
        // TODO: Implement
        // HINT: FSNode node = navigateTo(path);
        // HINT: if (node.isDirectory()) throw new InvalidOperationException("Cannot read directory as file");
        // HINT: return node.content;
        return null;
    }
    
    /**
     * List directory contents (like ls)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Navigate to directory
     * 2. Verify it's a directory
     * 3. Return sorted list of children names
     * 4. Append "/" to directory names for clarity
     * 
     * @param path Path to directory
     * @return List of names in directory
     * @throws PathNotFoundException if directory doesn't exist
     * @throws InvalidOperationException if path is a file
     */
    public List<String> ls(String path) 
            throws PathNotFoundException, InvalidOperationException, InvalidPathException {
        // TODO: Implement
        // HINT: FSNode node = navigateTo(path);
        // HINT: if (node.isFile()) return Arrays.asList(node.name); // ls on file returns the file name
        // HINT: return node.children.values().stream()
        //     .map(child -> child.isDirectory() ? child.name + "/" : child.name)
        //     .collect(Collectors.toList());
        return new ArrayList<>();
    }
    
    /**
     * Delete file or directory (like rm -rf)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Navigate to parent directory
     * 2. Remove the child from parent's children map
     * 3. Cannot delete root
     * 
     * @param path Path to delete
     * @throws PathNotFoundException if path doesn't exist
     * @throws InvalidOperationException if trying to delete root
     */
    public void delete(String path) 
            throws PathNotFoundException, InvalidOperationException, InvalidPathException {
        // TODO: Implement
        // HINT: if (path.equals("/")) throw new InvalidOperationException("Cannot delete root directory");
        // HINT: FSNode node = navigateTo(path);
        // HINT: FSNode parent = node.parent;
        // HINT: parent.children.remove(node.name);
        // HINT: parent.modifiedAt = System.currentTimeMillis();
    }
    
    /**
     * Move file/directory (like mv)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Navigate to source node
     * 2. Navigate to destination parent
     * 3. Remove from source parent
     * 4. Add to destination parent
     * 5. Update node's parent reference and name if needed
     * 
     * @param srcPath Source path
     * @param destPath Destination path
     * @throws PathNotFoundException if source or dest parent doesn't exist
     */
    public void move(String srcPath, String destPath) 
            throws PathNotFoundException, InvalidPathException, InvalidOperationException, PathAlreadyExistsException {
        // TODO: Implement
        // HINT: FSNode srcNode = navigateTo(srcPath);
        // HINT: FSNode destParent = navigateToParent(destPath);
        // HINT: String newName = parsePath(destPath)[parsePath(destPath).length - 1];
        // HINT: if (destParent.children.containsKey(newName)) throw new PathAlreadyExistsException(destPath);
        // HINT: srcNode.parent.children.remove(srcNode.name);
        // HINT: srcNode.name = newName;
        // HINT: srcNode.parent = destParent;
        // HINT: destParent.children.put(newName, srcNode);
    }
    
    /**
     * Search for files/dirs by name (like find -name)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Start DFS from given directory
     * 2. Check each node's name against search query
     * 3. Collect matching paths
     * 4. Support simple wildcard: "*" matches any substring
     * 
     * @param startPath Directory to search from
     * @param name Name or pattern to search
     * @return List of matching paths
     * @throws PathNotFoundException if start path doesn't exist
     */
    public List<String> find(String startPath, String name) 
            throws PathNotFoundException, InvalidPathException {
        // TODO: Implement
        // HINT: FSNode startNode = navigateTo(startPath);
        // HINT: List<String> results = new ArrayList<>();
        // HINT: findHelper(startNode, name, results);
        // HINT: return results;
        return new ArrayList<>();
    }
    
    /**
     * DFS helper for find
     * 
     * IMPLEMENTATION HINTS:
     * 1. Check if current node name matches
     * 2. If directory, recurse into each child
     * 3. Add matching paths to results list
     */
    private void findHelper(FSNode node, String name, List<String> results) {
        // TODO: Implement
        // HINT: if (node.name.equals(name) || name.equals("*")) results.add(node.getPath());
        // HINT: if (node.isDirectory() && node.children != null) {
        //     for (FSNode child : node.children.values()) {
        //         findHelper(child, name, results);
        //     }
        // }
    }
    
    /**
     * Get file/directory info
     * 
     * @param path Path to get info for
     * @return Info string
     * @throws PathNotFoundException if path doesn't exist
     */
    public String getInfo(String path) throws PathNotFoundException, InvalidPathException {
        // TODO: Implement
        // HINT: FSNode node = navigateTo(path);
        // HINT: return node.toString() + " at " + node.getPath();
        return null;
    }
    
    /**
     * Get total size of directory (recursive)
     * 
     * IMPLEMENTATION HINTS:
     * 1. If file, return file size
     * 2. If directory, recursively sum children sizes
     * 
     * @param path Path to calculate size for
     * @return Total size in bytes
     * @throws PathNotFoundException if path doesn't exist
     */
    public long getSize(String path) throws PathNotFoundException, InvalidPathException {
        // TODO: Implement
        // HINT: FSNode node = navigateTo(path);
        // HINT: return calculateSize(node);
        return 0;
    }
    
    private long calculateSize(FSNode node) {
        // TODO: Implement
        // HINT: if (node.isFile()) return node.size;
        // HINT: long total = 0;
        // HINT: for (FSNode child : node.children.values()) total += calculateSize(child);
        // HINT: return total;
        return 0;
    }
    
    // ===== PATH HELPERS =====
    
    /**
     * Parse path into components
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate path starts with "/"
     * 2. Split by "/" and filter empty strings
     * 3. Return array of path components
     * 
     * Example: "/home/user/file.txt" → ["home", "user", "file.txt"]
     * 
     * @param path Absolute path
     * @return Array of path components
     * @throws InvalidPathException if path is invalid
     */
    private String[] parsePath(String path) throws InvalidPathException {
        // TODO: Implement
        // HINT: if (path == null || path.isEmpty()) throw new InvalidPathException("Path cannot be empty");
        // HINT: if (!path.startsWith("/")) throw new InvalidPathException("Path must be absolute: " + path);
        // HINT: if (path.equals("/")) return new String[0];
        // HINT: return Arrays.stream(path.split("/")).filter(s -> !s.isEmpty()).toArray(String[]::new);
        return new String[0];
    }
    
    /**
     * Navigate to node at given path
     * 
     * IMPLEMENTATION HINTS:
     * 1. Parse path into components
     * 2. Start from root
     * 3. For each component, look up in current directory's children
     * 4. If any component not found, throw PathNotFoundException
     * 
     * @param path Absolute path
     * @return FSNode at the path
     * @throws PathNotFoundException if path doesn't exist
     */
    private FSNode navigateTo(String path) throws PathNotFoundException, InvalidPathException {
        // TODO: Implement
        // HINT: if (path.equals("/")) return root;
        // HINT: String[] parts = parsePath(path);
        // HINT: FSNode current = root;
        // HINT: for (String part : parts) {
        //     if (!current.isDirectory() || !current.children.containsKey(part))
        //         throw new PathNotFoundException(path);
        //     current = current.children.get(part);
        // }
        // HINT: return current;
        return null;
    }
    
    /**
     * Navigate to parent directory of given path
     * 
     * IMPLEMENTATION HINTS:
     * 1. Parse path and take all components except last
     * 2. Navigate to that path
     * 3. Verify it's a directory
     * 
     * @param path Absolute path
     * @return Parent directory FSNode
     * @throws PathNotFoundException if parent doesn't exist
     */
    private FSNode navigateToParent(String path) throws PathNotFoundException, InvalidPathException {
        // TODO: Implement
        // HINT: String[] parts = parsePath(path);
        // HINT: if (parts.length <= 1) return root;
        // HINT: String parentPath = "/" + String.join("/", Arrays.copyOf(parts, parts.length - 1));
        // HINT: FSNode parent = navigateTo(parentPath);
        // HINT: if (!parent.isDirectory()) throw new PathNotFoundException("Parent is not a directory: " + parentPath);
        // HINT: return parent;
        return null;
    }
    
    /**
     * Display tree structure (like tree command)
     */
    public void printTree() {
        System.out.println("/");
        printTreeHelper(root, "");
    }
    
    private void printTreeHelper(FSNode node, String indent) {
        if (node.children == null) return;
        List<FSNode> children = new ArrayList<>(node.children.values());
        for (int i = 0; i < children.size(); i++) {
            FSNode child = children.get(i);
            boolean isLast = (i == children.size() - 1);
            System.out.println(indent + (isLast ? "└── " : "├── ") + child);
            if (child.isDirectory()) {
                printTreeHelper(child, indent + (isLast ? "    " : "│   "));
            }
        }
    }
}

// ===== MAIN TEST CLASS =====

public class InMemoryFileSystem {
    public static void main(String[] args) {
        System.out.println("=== In-Memory File System Test Cases ===\n");
        
        FileSystem fs = new FileSystem();
        
        // Test Case 1: Create Directories
        System.out.println("=== Test Case 1: Create Directories (mkdir) ===");
        try {
            fs.mkdir("/home");
            fs.mkdir("/home/user");
            fs.mkdir("/home/user/documents");
            fs.mkdir("/home/user/pictures");
            fs.mkdir("/etc/config");
            System.out.println("✓ Created directory structure");
            fs.printTree();
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 2: Create Files
        System.out.println("=== Test Case 2: Create Files (touch) ===");
        try {
            fs.createFile("/home/user/documents/readme.txt");
            fs.createFile("/home/user/documents/notes.md");
            fs.createFile("/home/user/pictures/photo.jpg");
            fs.createFile("/etc/config/settings.conf");
            System.out.println("✓ Created 4 files");
            fs.printTree();
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 3: Write and Read File
        System.out.println("=== Test Case 3: Write and Read File ===");
        try {
            fs.writeFile("/home/user/documents/readme.txt", "Hello, World!\nThis is a test file.");
            String content = fs.readFile("/home/user/documents/readme.txt");
            System.out.println("✓ Content: " + content);
            System.out.println("  Info: " + fs.getInfo("/home/user/documents/readme.txt"));
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 4: Append to File
        System.out.println("=== Test Case 4: Append to File ===");
        try {
            fs.appendFile("/home/user/documents/readme.txt", "\nAppended line.");
            String content = fs.readFile("/home/user/documents/readme.txt");
            System.out.println("✓ After append: " + content);
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 5: List Directory
        System.out.println("=== Test Case 5: List Directory (ls) ===");
        try {
            List<String> homeContents = fs.ls("/home/user");
            System.out.println("/home/user contents: " + homeContents);
            
            List<String> docsContents = fs.ls("/home/user/documents");
            System.out.println("/home/user/documents contents: " + docsContents);
            
            List<String> rootContents = fs.ls("/");
            System.out.println("/ contents: " + rootContents);
            System.out.println("✓ Directory listing working");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 6: Delete File
        System.out.println("=== Test Case 6: Delete File ===");
        try {
            fs.delete("/home/user/documents/notes.md");
            System.out.println("✓ Deleted notes.md");
            List<String> after = fs.ls("/home/user/documents");
            System.out.println("  Documents now: " + after);
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 7: Move File
        System.out.println("=== Test Case 7: Move File (mv) ===");
        try {
            fs.createFile("/home/user/documents/temp.txt");
            fs.writeFile("/home/user/documents/temp.txt", "Temporary data");
            
            fs.move("/home/user/documents/temp.txt", "/home/user/pictures/moved.txt");
            System.out.println("✓ Moved temp.txt → pictures/moved.txt");
            
            String content = fs.readFile("/home/user/pictures/moved.txt");
            System.out.println("  Content preserved: " + content);
            
            List<String> docs = fs.ls("/home/user/documents");
            List<String> pics = fs.ls("/home/user/pictures");
            System.out.println("  Documents: " + docs);
            System.out.println("  Pictures: " + pics);
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 8: Find Files
        System.out.println("=== Test Case 8: Find Files ===");
        try {
            List<String> found = fs.find("/", "readme.txt");
            System.out.println("Found 'readme.txt': " + found);
            
            List<String> allFiles = fs.find("/home", "*");
            System.out.println("All items under /home: " + allFiles);
            System.out.println("✓ Find working");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 9: Get Size
        System.out.println("=== Test Case 9: Directory Size ===");
        try {
            long totalSize = fs.getSize("/");
            long homeSize = fs.getSize("/home");
            long fileSize = fs.getSize("/home/user/documents/readme.txt");
            
            System.out.println("Total size of /: " + totalSize + " bytes");
            System.out.println("Size of /home: " + homeSize + " bytes");
            System.out.println("Size of readme.txt: " + fileSize + " bytes");
            System.out.println("✓ Size calculation working");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 10: Final Tree
        System.out.println("=== Test Case 10: Final Tree ===");
        fs.printTree();
        System.out.println();
        
        // ===== EXCEPTION TEST CASES =====
        
        // Test Case 11: Exception - Path Not Found
        System.out.println("=== Test Case 11: Exception - Path Not Found ===");
        try {
            fs.readFile("/nonexistent/file.txt");
            System.out.println("✗ Should have thrown PathNotFoundException");
        } catch (PathNotFoundException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 12: Exception - File Already Exists
        System.out.println("=== Test Case 12: Exception - File Already Exists ===");
        try {
            fs.createFile("/home/user/documents/readme.txt");
            System.out.println("✗ Should have thrown PathAlreadyExistsException");
        } catch (PathAlreadyExistsException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 13: Exception - Write to Directory
        System.out.println("=== Test Case 13: Exception - Write to Directory ===");
        try {
            fs.writeFile("/home/user", "data");
            System.out.println("✗ Should have thrown InvalidOperationException");
        } catch (InvalidOperationException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 14: Exception - Delete Root
        System.out.println("=== Test Case 14: Exception - Delete Root ===");
        try {
            fs.delete("/");
            System.out.println("✗ Should have thrown InvalidOperationException");
        } catch (InvalidOperationException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 15: Exception - Invalid Path
        System.out.println("=== Test Case 15: Exception - Invalid Path ===");
        try {
            fs.mkdir("relative/path");
            System.out.println("✗ Should have thrown InvalidPathException");
        } catch (InvalidPathException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        System.out.println("=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION TOPICS:
 * ============================
 * 
 * 1. DATA STRUCTURE:
 *    Tree Structure:
 *      - Root directory as tree root
 *      - Each directory has Map<name, child>
 *      - TreeMap for alphabetical ordering
 *    
 *    Why Tree?
 *      - Natural hierarchy representation
 *      - Path traversal = tree traversal
 *      - O(depth) for path resolution
 * 
 * 2. DESIGN PATTERNS:
 *    Composite Pattern:
 *      - File and Directory share FSNode
 *      - Uniform treatment of files and dirs
 *      - ls() works on both (file returns itself)
 *    
 *    Iterator Pattern:
 *      - Traverse directory contents
 *      - DFS for recursive search
 *      - BFS for level-order listing
 * 
 * 3. PATH RESOLUTION:
 *    Absolute vs Relative:
 *      - /home/user (absolute)
 *      - ../docs (relative - needs CWD tracking)
 *    
 *    Special paths:
 *      - "." current directory
 *      - ".." parent directory
 *      - "~" home directory
 * 
 * 4. REAL FILE SYSTEM CONCEPTS:
 *    Inodes:
 *      - Metadata about file (size, permissions, timestamps)
 *      - Separate from file content
 *    
 *    Blocks:
 *      - Fixed-size storage units
 *      - File content split across blocks
 *    
 *    Journaling:
 *      - Log operations before executing
 *      - Recovery after crash
 *    
 *    Permissions:
 *      - Read/Write/Execute
 *      - Owner/Group/Others (Unix)
 * 
 * 5. ADVANCED FEATURES:
 *    - Symbolic links (soft links)
 *    - Hard links (multiple names for same inode)
 *    - File permissions (rwx)
 *    - File watching/notifications
 *    - Disk quotas
 *    - Mount points
 *    - File locking
 *    - Copy-on-write
 * 
 * 6. THREAD SAFETY:
 *    - ReadWriteLock per directory
 *    - Lock ordering to prevent deadlock
 *    - Atomic rename/move operations
 * 
 * 7. SCALABILITY:
 *    - B-tree for large directories
 *    - Extent-based allocation
 *    - Distributed file systems (HDFS, GFS)
 *    - Object storage (S3)
 * 
 * 8. LEETCODE REFERENCE:
 *    LeetCode 588 - Design In-Memory File System
 *    - mkdir, addContentToFile, readContentFromFile, ls
 * 
 * 9. API DESIGN:
 *    POST   /fs/mkdir?path=...            - Create directory
 *    POST   /fs/files?path=...            - Create file
 *    PUT    /fs/files?path=...            - Write file
 *    GET    /fs/files?path=...            - Read file
 *    GET    /fs/ls?path=...               - List directory
 *    DELETE /fs?path=...                  - Delete
 *    POST   /fs/move?src=...&dest=...     - Move
 *    GET    /fs/find?path=...&name=...    - Search
 */
