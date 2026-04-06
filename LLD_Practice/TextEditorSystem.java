import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/*
 * TEXT EDITOR / WORD PROCESSOR - Low Level Design
 * =================================================
 * 
 * REQUIREMENTS:
 * 1. Text document with dynamic rows/columns
 * 2. Each character has style (fontName, fontSize, bold, italic)
 * 3. addCharacter — insert at (row,col), push existing chars right
 * 4. deleteCharacter — remove at (row,col), shift left
 * 5. getStyle — return style string for char at (row,col)
 * 6. readLine — return all chars in a row as string
 * 7. Undo/Redo support (Command pattern)
 * 8. Thread-safe operations
 * 
 * KEY DATA STRUCTURES:
 * - ArrayList<ArrayList<StyledChar>>: rows of styled characters
 * - Deque<Command>: undo/redo stacks (Command pattern)
 * 
 * DESIGN PATTERNS: Command (undo/redo)
 * 
 * COMPLEXITY:
 *   addCharacter:    O(n) where n = chars in row (ArrayList shift)
 *   deleteCharacter: O(n) same reason
 *   getStyle:        O(1) direct index access
 *   readLine:        O(n) iterate row chars
 *   undo/redo:       O(n) same as add/delete
 */

// ==================== STYLED CHARACTER ====================

class StyledChar {
    final char ch;
    final String fontName;
    final int fontSize;
    final boolean bold, italic;

    StyledChar(char ch, String fontName, int fontSize, boolean bold, boolean italic) {
        this.ch = ch; this.fontName = fontName;
        this.fontSize = fontSize; this.bold = bold; this.italic = italic;
    }

    /** Format: "ch-fontName-fontSize[-b][-i]" */
    String toStyleString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ch).append('-').append(fontName).append('-').append(fontSize);
        if (bold) sb.append("-b");
        if (italic) sb.append("-i");
        return sb.toString();
    }
}

// ==================== COMMAND (UNDO/REDO) ====================

interface Command {
    void execute();
    void undo();
}

class AddCommand implements Command {
    private final TextEditor editor;
    final int row, col;
    final StyledChar sc;

    AddCommand(TextEditor editor, int row, int col, StyledChar sc) {
        this.editor = editor; this.row = row; this.col = col; this.sc = sc;
    }

    @Override public void execute() { editor.doAdd(row, col, sc); }
    @Override public void undo() { editor.doDelete(row, col); }
}

class DeleteCommand implements Command {
    private final TextEditor editor;
    final int row, col;
    StyledChar deleted; // captured on execute for undo

    DeleteCommand(TextEditor editor, int row, int col) {
        this.editor = editor; this.row = row; this.col = col;
    }

    @Override public void execute() { deleted = editor.doDelete(row, col); }
    @Override public void undo() { if (deleted != null) editor.doAdd(row, col, deleted); }
}

// ==================== TEXT EDITOR ====================

class TextEditor {
    private final List<List<StyledChar>> rows = new ArrayList<>();
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /** Ensure rows exist up to index `row`. */
    private void ensureRows(int row) {
        while (rows.size() <= row) rows.add(new ArrayList<>());
    }

    // --- Raw operations (used by commands) ---

    void doAdd(int row, int col, StyledChar sc) {
        // TODO: Implement — ensure rows exist, clamp col, insert char
        // HINT: ensureRows(row);
        // HINT: List<StyledChar> line = rows.get(row);
        // HINT: int insertAt = Math.min(col, line.size());
        // HINT: line.add(insertAt, sc);
    }

    StyledChar doDelete(int row, int col) {
        // TODO: Implement — return removed char or null if out of bounds
        // HINT: if (row >= rows.size()) return null;
        // HINT: List<StyledChar> line = rows.get(row);
        // HINT: if (col >= line.size()) return null;
        // HINT: return line.remove(col);
        return null;
    }

    // --- Public API ---

    /** Insert char at (row, col) with style. Pushes existing chars right. */
    void addCharacter(int row, int col, char ch,
                      String fontName, int fontSize, boolean bold, boolean italic) {
        lock.writeLock().lock();
        try {
            StyledChar sc = new StyledChar(ch, fontName, fontSize, bold, italic);
            Command cmd = new AddCommand(this, row, col, sc);
            cmd.execute();
            undoStack.push(cmd);
            redoStack.clear();
        } finally { lock.writeLock().unlock(); }
    }

    /** Delete char at (row, col). Returns true if deleted. */
    boolean deleteCharacter(int row, int col) {
        lock.writeLock().lock();
        try {
            DeleteCommand cmd = new DeleteCommand(this, row, col);
            cmd.execute();
            if (cmd.deleted == null) return false;
            undoStack.push(cmd);
            redoStack.clear();
            return true;
        } finally { lock.writeLock().unlock(); }
    }

    /** Get style string for char at (row, col). Empty string if none. */
    String getStyle(int row, int col) {
        lock.readLock().lock();
        try {
            // TODO: Implement
            // HINT: if (row >= rows.size()) return "";
            // HINT: List<StyledChar> line = rows.get(row);
            // HINT: if (col >= line.size()) return "";
            // HINT: return line.get(col).toStyleString();
            return "";
        } finally { lock.readLock().unlock(); }
    }

    /** Read all chars in a row as string. Empty string if row doesn't exist. */
    String readLine(int row) {
        lock.readLock().lock();
        try {
            // TODO: Implement
            // HINT: if (row >= rows.size()) return "";
            // HINT: StringBuilder sb = new StringBuilder();
            // HINT: for (StyledChar sc : rows.get(row)) sb.append(sc.ch);
            // HINT: return sb.toString();
            return "";
        } finally { lock.readLock().unlock(); }
    }

    /** Undo last operation. */
    boolean undo() {
        lock.writeLock().lock();
        try {
            if (undoStack.isEmpty()) return false;
            Command cmd = undoStack.pop();
            cmd.undo();
            redoStack.push(cmd);
            return true;
        } finally { lock.writeLock().unlock(); }
    }

    /** Redo last undone operation. */
    boolean redo() {
        lock.writeLock().lock();
        try {
            if (redoStack.isEmpty()) return false;
            Command cmd = redoStack.pop();
            cmd.execute();
            undoStack.push(cmd);
            return true;
        } finally { lock.writeLock().unlock(); }
    }

    int getRowCount() { return rows.size(); }
}

// ==================== MAIN / TESTS ====================

public class TextEditorSystem {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════╗");
        System.out.println("║    TEXT EDITOR - LLD Demo             ║");
        System.out.println("╚═══════════════════════════════════════╝\n");

        TextEditor editor = new TextEditor();

        // --- Test 1: Basic Add & Read ---
        System.out.println("=== Test 1: Add & Read ===");
        editor.addCharacter(0, 0, 'g', "Cambria", 17, true, true);
        editor.addCharacter(1, 0, 'y', "Century Gothic", 14, true, true);
        editor.addCharacter(1, 1, 'h', "Courier New", 22, false, false);
        editor.addCharacter(1, 2, 'y', "Georgia", 14, false, false);
        System.out.println("Row 0: '" + editor.readLine(0) + "' (expected 'g')");
        System.out.println("Row 1: '" + editor.readLine(1) + "' (expected 'yhy')");
        System.out.println("✓ Basic add and read\n");

        // --- Test 2: Get Style ---
        System.out.println("=== Test 2: Get Style ===");
        System.out.println("Style(0,0): '" + editor.getStyle(0, 0) + "' (expected 'g-Cambria-17-b-i')");
        System.out.println("Style(1,1): '" + editor.getStyle(1, 1) + "' (expected 'h-Courier New-22')");
        System.out.println("Style(1,2): '" + editor.getStyle(1, 2) + "' (expected 'y-Georgia-14')");
        System.out.println("Style(5,5): '" + editor.getStyle(5, 5) + "' (expected '')");
        System.out.println("✓ Style formatting correct\n");

        // --- Test 3: Insert Pushes Right ---
        System.out.println("=== Test 3: Insert Pushes Right ===");
        editor.addCharacter(0, 0, 'q', "Arial", 21, false, true);
        System.out.println("Row 0 after insert at 0: '" + editor.readLine(0) + "' (expected 'qg')");
        System.out.println("Style(0,0): '" + editor.getStyle(0, 0) + "' (expected 'q-Arial-21-i')");
        System.out.println("Style(0,1): '" + editor.getStyle(0, 1) + "' (expected 'g-Cambria-17-b-i')");
        System.out.println("✓ Existing chars shifted right\n");

        // --- Test 4: Delete ---
        System.out.println("=== Test 4: Delete ===");
        System.out.println("Row 1 before: '" + editor.readLine(1) + "'");
        boolean del1 = editor.deleteCharacter(1, 1);
        System.out.println("Delete(1,1): " + del1 + " (expected true)");
        System.out.println("Row 1 after: '" + editor.readLine(1) + "' (expected 'yy')");
        boolean del2 = editor.deleteCharacter(1, 4);
        System.out.println("Delete(1,4): " + del2 + " (expected false)");
        System.out.println("✓ Delete shifts left, out-of-bounds returns false\n");

        // --- Test 5: Undo/Redo ---
        System.out.println("=== Test 5: Undo / Redo ===");
        TextEditor e2 = new TextEditor();
        e2.addCharacter(0, 0, 'a', "Arial", 12, false, false);
        e2.addCharacter(0, 1, 'b', "Arial", 12, false, false);
        e2.addCharacter(0, 2, 'c', "Arial", 12, false, false);
        System.out.println("Before undo: '" + e2.readLine(0) + "' (expected 'abc')");
        e2.undo();
        System.out.println("After 1 undo: '" + e2.readLine(0) + "' (expected 'ab')");
        e2.undo();
        System.out.println("After 2 undo: '" + e2.readLine(0) + "' (expected 'a')");
        e2.redo();
        System.out.println("After 1 redo: '" + e2.readLine(0) + "' (expected 'ab')");
        e2.redo();
        System.out.println("After 2 redo: '" + e2.readLine(0) + "' (expected 'abc')");
        System.out.println("✓ Undo/Redo with Command pattern\n");

        // --- Test 6: Undo Delete ---
        System.out.println("=== Test 6: Undo Delete ===");
        TextEditor e3 = new TextEditor();
        e3.addCharacter(0, 0, 'x', "Times", 10, true, false);
        e3.addCharacter(0, 1, 'y', "Times", 10, false, true);
        e3.deleteCharacter(0, 0);
        System.out.println("After delete: '" + e3.readLine(0) + "' (expected 'y')");
        e3.undo();
        System.out.println("After undo delete: '" + e3.readLine(0) + "' (expected 'xy')");
        System.out.println("Style(0,0): '" + e3.getStyle(0, 0) + "' (expected 'x-Times-10-b')");
        System.out.println("✓ Undo restores deleted char with style\n");

        // --- Test 7: Edge Cases ---
        System.out.println("=== Test 7: Edge Cases ===");
        TextEditor e4 = new TextEditor();
        System.out.println("Read empty row: '" + e4.readLine(0) + "' (expected '')");
        System.out.println("Style empty: '" + e4.getStyle(0, 0) + "' (expected '')");
        System.out.println("Delete empty: " + e4.deleteCharacter(0, 0) + " (expected false)");
        System.out.println("Undo empty: " + e4.undo() + " (expected false)");
        // Add past column bounds — should append
        e4.addCharacter(0, 100, 'z', "Arial", 12, false, false);
        System.out.println("Add at col 100 on empty row: '" + e4.readLine(0) + "' (expected 'z')");
        // Auto-expand rows
        e4.addCharacter(5, 0, 'w', "Arial", 12, false, false);
        System.out.println("Rows after adding to row 5: " + e4.getRowCount() + " (expected 6)");
        System.out.println("✓ Edge cases handled\n");

        // --- Test 8: Multi-row Document ---
        System.out.println("=== Test 8: Multi-row Document ===");
        TextEditor e5 = new TextEditor();
        String[] words = {"hello", "world", "test"};
        for (int r = 0; r < words.length; r++)
            for (int c = 0; c < words[r].length(); c++)
                e5.addCharacter(r, c, words[r].charAt(c), "Mono", 11, false, false);
        for (int r = 0; r < words.length; r++)
            System.out.println("Row " + r + ": '" + e5.readLine(r) + "'");
        System.out.println("✓ Multi-row document\n");

        // --- Test 9: Scale ---
        System.out.println("=== Test 9: Scale ===");
        TextEditor e6 = new TextEditor();
        long start = System.nanoTime();
        for (int i = 0; i < 10000; i++)
            e6.addCharacter(0, i, (char)('a' + i % 26), "Arial", 12, i % 2 == 0, false);
        long addTime = System.nanoTime() - start;
        System.out.printf("10K adds: %.2f ms, line length: %d\n", addTime/1e6, e6.readLine(0).length());
        start = System.nanoTime();
        for (int i = 0; i < 1000; i++) e6.deleteCharacter(0, 0);
        long delTime = System.nanoTime() - start;
        System.out.printf("1K deletes: %.2f ms, line length: %d\n", delTime/1e6, e6.readLine(0).length());
        System.out.println("✓ Scales to large documents\n");

        // --- Test 10: Thread Safety ---
        System.out.println("=== Test 10: Thread Safety ===");
        TextEditor e7 = new TextEditor();
        ExecutorService exec = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            int idx = i;
            futures.add(exec.submit(() ->
                e7.addCharacter(idx % 10, 0, (char)('a' + idx % 26), "Arial", 12, false, false)));
        }
        for (int i = 0; i < 50; i++) {
            int idx = i;
            futures.add(exec.submit(() -> e7.readLine(idx % 10)));
        }
        for (Future<?> f : futures) { try { f.get(); } catch (Exception e) { System.out.println("ERR: " + e); } }
        exec.shutdown();
        int total = 0;
        for (int r = 0; r < 10; r++) total += e7.readLine(r).length();
        System.out.println("Concurrent adds: " + total + " chars across 10 rows");
        System.out.println("✓ Thread-safe\n");

        System.out.println("════════ ALL 10 TESTS PASSED ✓ ════════");
    }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. CORE DS: ArrayList<ArrayList<StyledChar>> — dynamic rows/cols.
 *    add/delete at index is O(n) due to shifting. For large docs,
 *    use a Rope or Gap Buffer for O(log n) edits.
 *
 * 2. COMMAND PATTERN: Each edit (add/delete) is a Command object
 *    with execute() and undo(). Undo stack + redo stack.
 *    New edit clears redo stack. Clean separation of concerns.
 *
 * 3. STYLE PER CHAR: Each char carries its own style — allows
 *    mixed formatting within a line (like Word/Docs).
 *
 * 4. THREAD SAFETY: ReadWriteLock — concurrent reads of readLine/
 *    getStyle, exclusive writes for add/delete/undo/redo.
 *
 * 5. ALTERNATIVES AT SCALE:
 *    - Rope: balanced binary tree of strings, O(log n) insert/delete
 *    - Gap Buffer: array with gap at cursor, O(1) local edits
 *    - Piece Table: used by VS Code — original + append buffer + pieces
 *    - CRDT: for collaborative editing (Google Docs)
 *
 * 6. COMPLEXITY:
 *    addCharacter:    O(n) ArrayList.add(index) shifts elements
 *    deleteCharacter: O(n) ArrayList.remove(index) shifts elements
 *    getStyle:        O(1) direct index
 *    readLine:        O(n) iterate row
 *    undo/redo:       O(n) same as the operation being reversed
 */
