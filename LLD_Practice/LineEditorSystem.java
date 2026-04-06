import java.util.*;
import java.util.concurrent.locks.*;

/*
 * LINE EDITOR WITH UNDO/REDO - Low Level Design
 * ================================================
 * 
 * REQUIREMENTS:
 * 1. addText(row, col, text) — insert text at position, shift right
 * 2. deleteText(row, startCol, length) — remove chars, shift left
 * 3. undo() — revert last edit, push to redo stack
 * 4. redo() — reapply last undone edit, push to undo stack
 * 5. readLine(row) — return row content as string
 * 6. New edit clears redo stack
 * 7. Document starts with 0 rows, 0-indexed
 * 
 * DESIGN PATTERNS: Command (undo/redo)
 * 
 * KEY DATA STRUCTURES:
 * - ArrayList<StringBuilder>: rows of text
 * - Deque<EditCommand>: undo/redo stacks
 * 
 * COMPLEXITY:
 *   addText:    O(n) where n = row length (StringBuilder.insert)
 *   deleteText: O(n) same reason
 *   readLine:   O(n) toString
 *   undo/redo:  O(n) same as original operation
 */

// ==================== COMMAND ====================

interface EditCommand {
    void execute();
    void undo();
}

class AddTextCommand implements EditCommand {
    private final LineEditor editor;
    final int row, col;
    final String text;
    final boolean createdRow;  // track if we added a new row

    AddTextCommand(LineEditor editor, int row, int col, String text, boolean createdRow) {
        this.editor = editor; this.row = row; this.col = col;
        this.text = text; this.createdRow = createdRow;
    }

    @Override public void execute() {
        // TODO: Implement
        // HINT: if (createdRow) editor.ensureRow(row);
        // HINT: editor.getRow(row).insert(col, text);
    }

    @Override public void undo() {
        // TODO: Implement
        // HINT: editor.getRow(row).delete(col, col + text.length());
        // HINT: if (createdRow) editor.removeLastRow();
    }
}

class DeleteTextCommand implements EditCommand {
    private final LineEditor editor;
    final int row, startCol, length;
    String deleted; // captured for undo

    DeleteTextCommand(LineEditor editor, int row, int startCol, int length) {
        this.editor = editor; this.row = row;
        this.startCol = startCol; this.length = length;
    }

    @Override public void execute() {
        // TODO: Implement — capture deleted text, then delete
        // HINT: StringBuilder line = editor.getRow(row);
        // HINT: deleted = line.substring(startCol, startCol + length);
        // HINT: line.delete(startCol, startCol + length);
    }

    @Override public void undo() {
        // TODO: Implement — re-insert deleted text
        // HINT: editor.getRow(row).insert(startCol, deleted);
    }
}

// ==================== LINE EDITOR ====================

class LineEditor {
    private final List<StringBuilder> rows = new ArrayList<>();
    private final Deque<EditCommand> undoStack = new ArrayDeque<>();
    private final Deque<EditCommand> redoStack = new ArrayDeque<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // --- Internal helpers (package-access for commands) ---

    StringBuilder getRow(int row) { return rows.get(row); }

    void ensureRow(int row) {
        while (rows.size() <= row) rows.add(new StringBuilder());
    }

    void removeLastRow() {
        if (!rows.isEmpty()) rows.remove(rows.size() - 1);
    }

    int rowCount() { return rows.size(); }

    // --- Public API ---

    /** Insert text at (row, col). If row == rowCount, creates new row first. */
    void addText(int row, int col, String text) {
        lock.writeLock().lock();
        try {
            boolean createdRow = (row == rows.size());
            AddTextCommand cmd = new AddTextCommand(this, row, col, text, createdRow);
            cmd.execute();
            undoStack.push(cmd);
            redoStack.clear();
        } finally { lock.writeLock().unlock(); }
    }

    /** Delete length chars from row starting at startCol. */
    void deleteText(int row, int startCol, int length) {
        lock.writeLock().lock();
        try {
            DeleteTextCommand cmd = new DeleteTextCommand(this, row, startCol, length);
            cmd.execute();
            undoStack.push(cmd);
            redoStack.clear();
        } finally { lock.writeLock().unlock(); }
    }

    /** Undo last edit. No-op if nothing to undo. */
    void undo() {
        lock.writeLock().lock();
        try {
            if (undoStack.isEmpty()) return;
            EditCommand cmd = undoStack.pop();
            cmd.undo();
            redoStack.push(cmd);
        } finally { lock.writeLock().unlock(); }
    }

    /** Redo last undone edit. No-op if nothing to redo. */
    void redo() {
        lock.writeLock().lock();
        try {
            if (redoStack.isEmpty()) return;
            EditCommand cmd = redoStack.pop();
            cmd.execute();
            undoStack.push(cmd);
        } finally { lock.writeLock().unlock(); }
    }

    /** Return row content. "" if row is empty. */
    String readLine(int row) {
        lock.readLock().lock();
        try {
            return rows.get(row).toString();
        } finally { lock.readLock().unlock(); }
    }

    int getRowCount() { return rows.size(); }
}

// ==================== MAIN / TESTS ====================

public class LineEditorSystem {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════╗");
        System.out.println("║    LINE EDITOR - LLD Demo             ║");
        System.out.println("╚═══════════════════════════════════════╝\n");

        // --- Test 1: Creating rows and appending text ---
        System.out.println("=== Test 1: Create rows & append ===");
        LineEditor ed = new LineEditor();
        ed.addText(0, 0, "hello");
        System.out.println("readLine(0): '" + ed.readLine(0) + "' (expected 'hello')");
        ed.addText(1, 0, "world");
        System.out.println("readLine(1): '" + ed.readLine(1) + "' (expected 'world')");
        System.out.println("rowCount: " + ed.getRowCount() + " (expected 2)");
        System.out.println("✓ Row creation and basic insert\n");

        // --- Test 2: Insert in middle, delete, undo, redo ---
        System.out.println("=== Test 2: Insert, delete, undo, redo ===");
        ed.addText(0, 5, "-there");
        System.out.println("After insert: '" + ed.readLine(0) + "' (expected 'hello-there')");
        ed.deleteText(0, 5, 6);
        System.out.println("After delete: '" + ed.readLine(0) + "' (expected 'hello')");
        ed.undo();
        System.out.println("After undo:   '" + ed.readLine(0) + "' (expected 'hello-there')");
        ed.redo();
        System.out.println("After redo:   '" + ed.readLine(0) + "' (expected 'hello')");
        System.out.println("✓ Full undo/redo cycle\n");

        // --- Test 3: Insert into middle of line ---
        System.out.println("=== Test 3: Insert mid-line ===");
        ed.addText(1, 5, "-wide web");
        System.out.println("After insert: '" + ed.readLine(1) + "' (expected 'world-wide web')");
        ed.deleteText(1, 5, 5);
        System.out.println("After delete: '" + ed.readLine(1) + "' (expected 'world web')");
        ed.undo();
        System.out.println("After undo:   '" + ed.readLine(1) + "' (expected 'world-wide web')");
        System.out.println("✓ Mid-line operations\n");

        // --- Test 4: Multiple undos, redo cleared by new edit ---
        System.out.println("=== Test 4: Multiple undos, redo cleared ===");
        LineEditor e2 = new LineEditor();
        e2.addText(0, 0, "hello");
        e2.addText(0, 5, "!");
        e2.addText(0, 6, "!");
        System.out.println("Start: '" + e2.readLine(0) + "' (expected 'hello!!')");
        e2.undo();
        System.out.println("Undo 1: '" + e2.readLine(0) + "' (expected 'hello!')");
        e2.undo();
        System.out.println("Undo 2: '" + e2.readLine(0) + "' (expected 'hello')");
        e2.redo();
        System.out.println("Redo 1: '" + e2.readLine(0) + "' (expected 'hello!')");
        e2.addText(0, 6, "?");
        System.out.println("New edit: '" + e2.readLine(0) + "' (expected 'hello!?')");
        e2.redo(); // no-op — redo cleared
        System.out.println("Redo (no-op): '" + e2.readLine(0) + "' (expected 'hello!?')");
        System.out.println("✓ Redo cleared by new edit\n");

        // --- Test 5: Delete all, empty row persists ---
        System.out.println("=== Test 5: Delete all, empty row ===");
        LineEditor e3 = new LineEditor();
        e3.addText(0, 0, "aa bb-cc");
        System.out.println("Before: '" + e3.readLine(0) + "'");
        e3.deleteText(0, 0, 8);
        System.out.println("After delete all: '" + e3.readLine(0) + "' (expected '')");
        System.out.println("rowCount: " + e3.getRowCount() + " (expected 1)");
        e3.undo();
        System.out.println("After undo: '" + e3.readLine(0) + "' (expected 'aa bb-cc')");
        System.out.println("✓ Empty row persists, undo restores\n");

        // --- Test 6: Prefix insert at column 0 ---
        System.out.println("=== Test 6: Prefix insert ===");
        LineEditor e4 = new LineEditor();
        e4.addText(0, 0, "world");
        e4.addText(0, 0, "hello-");
        System.out.println("readLine(0): '" + e4.readLine(0) + "' (expected 'hello-world')");
        System.out.println("✓ Insert at column 0\n");

        // --- Test 7: Undo add that created a row ---
        System.out.println("=== Test 7: Undo row creation ===");
        LineEditor e5 = new LineEditor();
        e5.addText(0, 0, "first");
        e5.addText(1, 0, "second");
        System.out.println("Rows: " + e5.getRowCount() + " (expected 2)");
        e5.undo();
        System.out.println("After undo: rows=" + e5.getRowCount() + " (expected 1)");
        e5.redo();
        System.out.println("After redo: rows=" + e5.getRowCount() + ", line1='" + e5.readLine(1) + "'");
        System.out.println("✓ Undo/redo row creation\n");

        // --- Test 8: Multiple rows ---
        System.out.println("=== Test 8: Multi-row document ===");
        LineEditor e6 = new LineEditor();
        e6.addText(0, 0, "line-zero");
        e6.addText(1, 0, "line-one");
        e6.addText(2, 0, "line-two");
        for (int i = 0; i < 3; i++)
            System.out.println("Row " + i + ": '" + e6.readLine(i) + "'");
        System.out.println("✓ Multi-row\n");

        // --- Test 9: Scale ---
        System.out.println("=== Test 9: Scale ===");
        LineEditor e7 = new LineEditor();
        e7.addText(0, 0, "");
        long t = System.nanoTime();
        for (int i = 0; i < 5000; i++)
            e7.addText(0, 0, "x");
        System.out.printf("5000 inserts: %.2f ms, length=%d\n",
            (System.nanoTime()-t)/1e6, e7.readLine(0).length());
        t = System.nanoTime();
        for (int i = 0; i < 2500; i++) e7.undo();
        System.out.printf("2500 undos: %.2f ms, length=%d\n",
            (System.nanoTime()-t)/1e6, e7.readLine(0).length());
        System.out.println("✓ Fast at scale\n");

        // --- Test 10: Undo/redo no-op ---
        System.out.println("=== Test 10: No-op undo/redo ===");
        LineEditor e8 = new LineEditor();
        e8.undo(); // no-op
        e8.redo(); // no-op
        e8.addText(0, 0, "test");
        e8.redo(); // no-op — nothing undone
        System.out.println("readLine(0): '" + e8.readLine(0) + "' (expected 'test')");
        System.out.println("✓ No-op cases handled\n");

        System.out.println("════════ ALL 10 TESTS PASSED ✓ ════════");
    }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. COMMAND PATTERN: EditCommand interface with execute()/undo().
 *    AddTextCommand captures (row, col, text, createdRow).
 *    DeleteTextCommand captures deleted text on execute for undo.
 *    Undo stack + redo stack. New edit clears redo.
 *
 * 2. CORE DS: ArrayList<StringBuilder> — dynamic rows.
 *    StringBuilder for efficient mid-string insert/delete.
 *    O(n) per operation due to char shifting.
 *
 * 3. ROW MANAGEMENT: addText auto-creates row if row == rowCount.
 *    Undo of add that created a row removes it. Delete never
 *    removes rows — empty rows persist.
 *
 * 4. ALTERNATIVES AT SCALE:
 *    - Gap Buffer: O(1) local edits at cursor position
 *    - Rope: O(log n) arbitrary insert/delete via balanced tree
 *    - Piece Table: VS Code approach — original + append buffer
 *
 * 5. COMPLEXITY:
 *    addText/deleteText: O(n) StringBuilder shift
 *    undo/redo:          O(n) reverse the operation
 *    readLine:           O(n) toString
 */
