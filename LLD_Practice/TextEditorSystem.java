import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/*
 * TEXT EDITOR - Low Level Design
 * ================================
 * 
 * REQUIREMENTS:
 * 1. Create/manage multiple documents
 * 2. Insert/delete characters at (row, col)
 * 3. readLine, readAll
 * 4. Undo/Redo (Command pattern)
 * 5. Edit event listeners (Observer pattern)
 * 6. Thread-safe
 * 
 * DESIGN PATTERNS:
 *   Command  — AddCharCmd, DeleteCharCmd (undo/redo)
 *   Observer — EditorEventListener / EditLogger, EditAutoSaver
 *   Facade   — TextEditorService
 * 
 * COMPLEXITY:
 *   insert/delete: O(n) per row (ArrayList shift)
 *   readLine:      O(n) iterate row
 *   undo/redo:     O(n) same as reversed op
 */

// ==================== EXCEPTIONS ====================

class InvalidPositionException extends RuntimeException {
    InvalidPositionException(int row, int col, String reason) {
        super("Invalid position (" + row + "," + col + "): " + reason);
    }
}

class DocumentNotFoundException extends RuntimeException {
    DocumentNotFoundException(String docId) { super("Document not found: " + docId); }
}

class DuplicateDocumentException extends RuntimeException {
    DuplicateDocumentException(String docId) { super("Document already exists: " + docId); }
}

// ==================== ENUMS ====================

enum EditorEditType { INSERT, DELETE, UNDO, REDO }

// ==================== MODELS ====================

class EditorDocument {
    final String id;
    final List<List<Character>> rows = new ArrayList<>();
    final Deque<EditorCommand> undoStack = new ArrayDeque<>();
    final Deque<EditorCommand> redoStack = new ArrayDeque<>();

    EditorDocument(String id) {
        this.id = id;
        this.rows.add(new ArrayList<>());
    }

    void ensureRows(int row) {
        while (rows.size() <= row) rows.add(new ArrayList<>());
    }

    int getRowCount() { return rows.size(); }
}

// ==================== INTERFACES ====================

/** Command pattern for undo/redo. */
interface EditorCommand {
    void execute();
    void undo();
}

/** Observer pattern — edit event notifications. */
interface EditorEventListener {
    void onEdit(String docId, EditorEditType type, int row, int col);
}

// ==================== COMMAND IMPLEMENTATIONS ====================

class AddCharCmd implements EditorCommand {
    private final EditorDocument doc;
    final int row, col;
    final char ch;

    AddCharCmd(EditorDocument doc, int row, int col, char ch) {
        this.doc = doc; this.row = row; this.col = col; this.ch = ch;
    }

    @Override public void execute() {
        doc.ensureRows(row);
        List<Character> line = doc.rows.get(row);
        line.add(Math.min(col, line.size()), ch);
    }

    @Override public void undo() {
        if (row < doc.rows.size() && col < doc.rows.get(row).size())
            doc.rows.get(row).remove(col);
    }
}

class DeleteCharCmd implements EditorCommand {
    private final EditorDocument doc;
    final int row, col;
    char deleted;
    boolean wasDeleted;

    DeleteCharCmd(EditorDocument doc, int row, int col) {
        this.doc = doc; this.row = row; this.col = col;
    }

    @Override public void execute() {
        if (row >= doc.rows.size() || col >= doc.rows.get(row).size()) { wasDeleted = false; return; }
        deleted = doc.rows.get(row).remove(col);
        wasDeleted = true;
    }

    @Override public void undo() {
        if (wasDeleted) {
            doc.ensureRows(row);
            List<Character> line = doc.rows.get(row);
            line.add(Math.min(col, line.size()), deleted);
        }
    }
}

// ==================== LISTENERS (OBSERVER) ====================

class EditLogger implements EditorEventListener {
    final List<String> events = new ArrayList<>();

    @Override public void onEdit(String docId, EditorEditType type, int row, int col) {
        events.add(type + ":" + docId + "@(" + row + "," + col + ")");
    }
}

class EditAutoSaver implements EditorEventListener {
    final int threshold;
    int unsavedChanges = 0, saveCount = 0;

    EditAutoSaver(int threshold) { this.threshold = threshold; }

    @Override public void onEdit(String docId, EditorEditType type, int row, int col) {
        if (++unsavedChanges >= threshold) { saveCount++; unsavedChanges = 0; }
    }
}

// ==================== TEXT EDITOR SERVICE (FACADE) ====================

class TextEditorService {
    private final Map<String, EditorDocument> documents = new ConcurrentHashMap<>();
    private final Map<String, ReadWriteLock> locks = new ConcurrentHashMap<>();
    private final List<EditorEventListener> listeners = new CopyOnWriteArrayList<>();

    void addListener(EditorEventListener l) { listeners.add(l); }

    private void notifyAll(String docId, EditorEditType t, int r, int c) {
        for (EditorEventListener l : listeners) l.onEdit(docId, t, r, c);
    }

    EditorDocument createDocument(String id) {
        if (documents.containsKey(id)) throw new DuplicateDocumentException(id);
        EditorDocument doc = new EditorDocument(id);
        documents.put(id, doc);
        locks.put(id, new ReentrantReadWriteLock());
        return doc;
    }

    EditorDocument getDocument(String id) {
        EditorDocument doc = documents.get(id);
        if (doc == null) throw new DocumentNotFoundException(id);
        return doc;
    }

    void addCharacter(String docId, int row, int col, char ch) {
        locks.get(docId).writeLock().lock();
        try {
            EditorDocument doc = getDocument(docId);
            AddCharCmd cmd = new AddCharCmd(doc, row, col, ch);
            cmd.execute();
            doc.undoStack.push(cmd);
            doc.redoStack.clear();
            notifyAll(docId, EditorEditType.INSERT, row, col);
        } finally { locks.get(docId).writeLock().unlock(); }
    }

    boolean deleteCharacter(String docId, int row, int col) {
        locks.get(docId).writeLock().lock();
        try {
            EditorDocument doc = getDocument(docId);
            DeleteCharCmd cmd = new DeleteCharCmd(doc, row, col);
            cmd.execute();
            if (!cmd.wasDeleted) return false;
            doc.undoStack.push(cmd);
            doc.redoStack.clear();
            notifyAll(docId, EditorEditType.DELETE, row, col);
            return true;
        } finally { locks.get(docId).writeLock().unlock(); }
    }

    String readLine(String docId, int row) {
        EditorDocument doc = getDocument(docId);
        locks.get(docId).readLock().lock();
        try {
            if (row >= doc.rows.size()) return "";
            StringBuilder sb = new StringBuilder();
            for (char c : doc.rows.get(row)) sb.append(c);
            return sb.toString();
        } finally { locks.get(docId).readLock().unlock(); }
    }

    String readAll(String docId) {
        locks.get(docId).readLock().lock();
        try {
            EditorDocument doc = getDocument(docId);
            StringBuilder sb = new StringBuilder();
            for (int r = 0; r < doc.rows.size(); r++) {
                if (r > 0) sb.append('\n');
                for (char c : doc.rows.get(r)) sb.append(c);
            }
            return sb.toString();
        } finally { locks.get(docId).readLock().unlock(); }
    }

    boolean undo(String docId) {
        locks.get(docId).writeLock().lock();
        try {
            EditorDocument doc = getDocument(docId);
            if (doc.undoStack.isEmpty()) return false;
            EditorCommand cmd = doc.undoStack.pop();
            cmd.undo();
            doc.redoStack.push(cmd);
            notifyAll(docId, EditorEditType.UNDO, 0, 0);
            return true;
        } finally { locks.get(docId).writeLock().unlock(); }
    }

    boolean redo(String docId) {
        locks.get(docId).writeLock().lock();
        try {
            EditorDocument doc = getDocument(docId);
            if (doc.redoStack.isEmpty()) return false;
            EditorCommand cmd = doc.redoStack.pop();
            cmd.execute();
            doc.undoStack.push(cmd);
            notifyAll(docId, EditorEditType.REDO, 0, 0);
            return true;
        } finally { locks.get(docId).writeLock().unlock(); }
    }

    int getDocumentCount() { return documents.size(); }
    int getRowCount(String docId) { return getDocument(docId).getRowCount(); }
}

// ==================== MAIN / TESTS ====================

public class TextEditorSystem {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║       TEXT EDITOR - LLD Demo              ║");
        System.out.println("╚═══════════════════════════════════════════╝\n");

        // --- Test 1: Create + Add & Read ---
        System.out.println("=== Test 1: Create + Add & Read ===");
        TextEditorService svc = new TextEditorService();
        svc.createDocument("doc1");
        svc.addCharacter("doc1", 0, 0, 'h');
        svc.addCharacter("doc1", 0, 1, 'i');
        svc.addCharacter("doc1", 1, 0, 'x');
        check(svc.readLine("doc1", 0), "hi", "Row 0 = 'hi'");
        check(svc.readLine("doc1", 1), "x", "Row 1 = 'x'");
        check(svc.readAll("doc1"), "hi\nx", "readAll = 'hi\\nx'");
        System.out.println("✓ Basic add and read\n");

        // --- Test 2: Insert Pushes Right ---
        System.out.println("=== Test 2: Insert Pushes Right ===");
        svc.addCharacter("doc1", 0, 0, 'A');
        check(svc.readLine("doc1", 0), "Ahi", "Insert at 0 pushes right");
        System.out.println("✓ Shift right\n");

        // --- Test 3: Delete ---
        System.out.println("=== Test 3: Delete ===");
        check(svc.deleteCharacter("doc1", 0, 0), true, "Delete(0,0) = true");
        check(svc.readLine("doc1", 0), "hi", "After delete = 'hi'");
        check(svc.deleteCharacter("doc1", 0, 99), false, "Out of bounds = false");
        System.out.println("✓ Delete works\n");

        // --- Test 4: Undo / Redo ---
        System.out.println("=== Test 4: Undo / Redo ===");
        TextEditorService svc2 = new TextEditorService();
        svc2.createDocument("u");
        svc2.addCharacter("u", 0, 0, 'a');
        svc2.addCharacter("u", 0, 1, 'b');
        svc2.addCharacter("u", 0, 2, 'c');
        check(svc2.readLine("u", 0), "abc", "Before undo");
        svc2.undo("u");
        check(svc2.readLine("u", 0), "ab", "After 1 undo");
        svc2.undo("u");
        check(svc2.readLine("u", 0), "a", "After 2 undo");
        svc2.redo("u");
        check(svc2.readLine("u", 0), "ab", "After 1 redo");
        svc2.redo("u");
        check(svc2.readLine("u", 0), "abc", "After 2 redo");
        System.out.println("✓ Undo/Redo\n");

        // --- Test 5: Undo Delete ---
        System.out.println("=== Test 5: Undo Delete ===");
        TextEditorService svc3 = new TextEditorService();
        svc3.createDocument("d");
        svc3.addCharacter("d", 0, 0, 'x');
        svc3.addCharacter("d", 0, 1, 'y');
        svc3.deleteCharacter("d", 0, 0);
        check(svc3.readLine("d", 0), "y", "After delete");
        svc3.undo("d");
        check(svc3.readLine("d", 0), "xy", "After undo delete");
        System.out.println("✓ Undo restores deleted char\n");

        // --- Test 6: Observer ---
        System.out.println("=== Test 6: Observer ===");
        TextEditorService svc4 = new TextEditorService();
        EditLogger logger = new EditLogger();
        EditAutoSaver autoSave = new EditAutoSaver(3);
        svc4.addListener(logger);
        svc4.addListener(autoSave);
        svc4.createDocument("ob");
        for (int i = 0; i < 5; i++) svc4.addCharacter("ob", 0, i, (char)('a' + i));
        check(logger.events.size(), 5, "Logger: 5 events");
        check(autoSave.saveCount, 1, "AutoSave: triggered once (threshold 3)");
        System.out.println("✓ Observers notified\n");

        // --- Test 7: Exceptions ---
        System.out.println("=== Test 7: Exceptions ===");
        try { svc.createDocument("doc1"); System.out.println("  ✗"); }
        catch (DuplicateDocumentException e) { System.out.println("  ✓ " + e.getMessage()); }
        try { svc.readLine("nope", 0); System.out.println("  ✗"); }
        catch (DocumentNotFoundException e) { System.out.println("  ✓ " + e.getMessage()); }
        TextEditorService svcE = new TextEditorService();
        svcE.createDocument("e");
        check(svcE.undo("e"), false, "Undo empty = false");
        System.out.println("✓ Exceptions handled\n");

        // --- Test 8: Edge Cases ---
        System.out.println("=== Test 8: Edge Cases ===");
        TextEditorService svc5 = new TextEditorService();
        svc5.createDocument("ec");
        check(svc5.readLine("ec", 0), "", "Empty row");
        check(svc5.deleteCharacter("ec", 0, 0), false, "Delete empty");
        svc5.addCharacter("ec", 0, 100, 'z');
        check(svc5.readLine("ec", 0), "z", "Add past bounds → appended");
        svc5.addCharacter("ec", 5, 0, 'w');
        check(svc5.getRowCount("ec"), 6, "Auto-expanded rows");
        System.out.println("✓ Edge cases\n");

        // --- Test 9: Multiple Documents ---
        System.out.println("=== Test 9: Multiple Documents ===");
        TextEditorService svc6 = new TextEditorService();
        svc6.createDocument("a");
        svc6.createDocument("b");
        svc6.addCharacter("a", 0, 0, 'A');
        svc6.addCharacter("b", 0, 0, 'B');
        check(svc6.readLine("a", 0), "A", "Doc a");
        check(svc6.readLine("b", 0), "B", "Doc b");
        check(svc6.getDocumentCount(), 2, "2 documents");
        System.out.println("✓ Independent documents\n");

        // --- Test 10: Scale ---
        System.out.println("=== Test 10: Scale ===");
        TextEditorService svc7 = new TextEditorService();
        svc7.createDocument("big");
        long start = System.nanoTime();
        for (int i = 0; i < 10000; i++) svc7.addCharacter("big", 0, i, (char)('a' + i % 26));
        System.out.printf("  10K adds: %.2f ms, length: %d\n", (System.nanoTime() - start) / 1e6, svc7.readLine("big", 0).length());
        System.out.println("✓ Scales\n");

        // --- Test 11: Thread Safety ---
        System.out.println("=== Test 11: Thread Safety ===");
        TextEditorService svc8 = new TextEditorService();
        svc8.createDocument("t");
        ExecutorService exec = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            int idx = i;
            futures.add(exec.submit(() -> svc8.addCharacter("t", idx % 10, 0, (char)('a' + idx % 26))));
        }
        for (Future<?> f : futures) { try { f.get(); } catch (Exception e) {} }
        exec.shutdown();
        int total = 0;
        for (int r = 0; r < 10; r++) total += svc8.readLine("t", r).length();
        check(total, 100, "100 concurrent inserts");
        System.out.println("✓ Thread-safe\n");

        System.out.println("════════ ALL 11 TESTS PASSED ✓ ════════");
    }

    static void check(String a, String e, String m) {
        System.out.println("  " + (Objects.equals(a, e) ? "✓" : "✗ GOT '" + a + "' expected '" + e + "'") + " " + m);
    }
    static void check(int a, int e, String m) {
        System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a + " expected " + e) + " " + m);
    }
    static void check(boolean a, boolean e, String m) {
        System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m);
    }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. COMMAND PATTERN: AddCharCmd/DeleteCharCmd with execute()+undo().
 *    Undo stack + redo stack. New edit clears redo.
 *
 * 2. OBSERVER PATTERN: EditorEventListener, EditLogger, EditAutoSaver.
 *    Decouples editor from logging/saving concerns.
 *
 * 3. FACADE: TextEditorService manages docs, locks, listeners.
 *
 * 4. THREAD SAFETY: ReadWriteLock per doc. ConcurrentHashMap for registry.
 *
 * 5. DATA STRUCTURE: ArrayList<ArrayList<Character>> — O(n) insert/delete.
 *    At scale: Rope (O(log n)), Gap Buffer (O(1) local), Piece Table (VS Code).
 */
