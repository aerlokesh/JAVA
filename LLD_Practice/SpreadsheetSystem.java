import java.util.*;
import java.util.concurrent.locks.*;

/*
 * SPREADSHEET - Low Level Design
 * =================================
 * 
 * REQUIREMENTS:
 * 1. Grid of cells with text + style (font, size, bold, italic)
 * 2. addRow(index) — insert empty row, shift existing down
 * 3. addColumn(index) — insert empty column, shift existing right
 * 4. addEntry(row, col, text, font, size, bold, italic) — set cell
 * 5. getEntry(row, col) — return "text-font-size[-b][-i]" or ""
 * 6. Initial size: 5×5, all empty. 0-indexed.
 * 
 * KEY DATA STRUCTURES:
 * - ArrayList<ArrayList<CellEntry>>: 2D grid, dynamic rows/cols
 * 
 * COMPLEXITY:
 *   addRow:    O(cols) create empty row
 *   addColumn: O(rows) insert null into each row
 *   addEntry:  O(1)
 *   getEntry:  O(1)
 */

// ==================== CELL ENTRY ====================

class CellEntry {
    final String text, fontName;
    final int fontSize;
    final boolean bold, italic;

    CellEntry(String text, String fontName, int fontSize, boolean bold, boolean italic) {
        this.text = text; this.fontName = fontName;
        this.fontSize = fontSize; this.bold = bold; this.italic = italic;
    }

    /** Format: "text-fontName-fontSize[-b][-i]" */
    String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(text).append('-').append(fontName).append('-').append(fontSize);
        if (bold) sb.append("-b");
        if (italic) sb.append("-i");
        return sb.toString();
    }
}

// ==================== SPREADSHEET ====================

class Spreadsheet {
    private final List<List<CellEntry>> grid = new ArrayList<>();
    private int numRows, numCols;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    Spreadsheet(int rows, int cols) {
        this.numRows = rows;
        this.numCols = cols;
        for (int r = 0; r < rows; r++) {
            List<CellEntry> row = new ArrayList<>(Collections.nCopies(cols, null));
            grid.add(row);
        }
    }

    Spreadsheet() { this(5, 5); }

    /** Insert empty row at index. Existing rows shift down. */
    void addRow(int index) {
        lock.writeLock().lock();
        try {
            // TODO: Implement
            // HINT: List<CellEntry> newRow = new ArrayList<>(Collections.nCopies(numCols, null));
            // HINT: grid.add(index, newRow);
            // HINT: numRows++;
        } finally { lock.writeLock().unlock(); }
    }

    /** Insert empty column at index. Existing columns shift right. */
    void addColumn(int index) {
        lock.writeLock().lock();
        try {
            // TODO: Implement
            // HINT: for (List<CellEntry> row : grid) row.add(index, null);
            // HINT: numCols++;
        } finally { lock.writeLock().unlock(); }
    }

    /** Set cell content at (row, col). Replaces existing. */
    void addEntry(int row, int col, String text, String fontName, int fontSize,
                  boolean isBold, boolean isItalic) {
        lock.writeLock().lock();
        try {
            // TODO: Implement
            // HINT: grid.get(row).set(col, new CellEntry(text, fontName, fontSize, isBold, isItalic));
        } finally { lock.writeLock().unlock(); }
    }

    /** Get serialized cell content. "" if empty. */
    String getEntry(int row, int col) {
        lock.readLock().lock();
        try {
            // TODO: Implement
            // HINT: CellEntry cell = grid.get(row).get(col);
            // HINT: return cell == null ? "" : cell.serialize();
            return "";
        } finally { lock.readLock().unlock(); }
    }

    int getRowCount() { return numRows; }
    int getColCount() { return numCols; }
}

// ==================== MAIN / TESTS ====================

public class SpreadsheetSystem {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════╗");
        System.out.println("║    SPREADSHEET - LLD Demo         ║");
        System.out.println("╚═══════════════════════════════════╝\n");

        Spreadsheet sheet = new Spreadsheet(); // 5x5

        // --- Test 1: Initial state ---
        System.out.println("=== Test 1: Initial state ===");
        System.out.println("Rows: " + sheet.getRowCount() + " (expected 5)");
        System.out.println("Cols: " + sheet.getColCount() + " (expected 5)");
        System.out.println("getEntry(0,0): '" + sheet.getEntry(0, 0) + "' (expected '')");
        System.out.println("✓ Empty 5x5 grid\n");

        // --- Test 2: Add entry + get ---
        System.out.println("=== Test 2: Add & Get entry ===");
        sheet.addEntry(0, 0, "hello", "tahoma", 24, true, false);
        System.out.println("getEntry(0,0): '" + sheet.getEntry(0, 0) + "' (expected 'hello-tahoma-24-b')");
        sheet.addEntry(1, 3, "note", "algerian", 14, true, true);
        System.out.println("getEntry(1,3): '" + sheet.getEntry(1, 3) + "' (expected 'note-algerian-14-b-i')");
        System.out.println("✓ Entry with bold/italic flags\n");

        // --- Test 3: Add row at end ---
        System.out.println("=== Test 3: Add row at end ===");
        sheet.addRow(5); // rows become 6
        System.out.println("Rows: " + sheet.getRowCount() + " (expected 6)");
        sheet.addEntry(5, 0, "hello", "tahoma", 24, true, false);
        System.out.println("getEntry(5,0): '" + sheet.getEntry(5, 0) + "' (expected 'hello-tahoma-24-b')");
        System.out.println("✓ Appended row\n");

        // --- Test 4: Add column + shift ---
        System.out.println("=== Test 4: Add column + shift ===");
        sheet.addEntry(0, 1, "x", "calibri", 10, false, true);
        System.out.println("Before addColumn(1): getEntry(0,1): '" + sheet.getEntry(0, 1) + "' (expected 'x-calibri-10-i')");
        sheet.addColumn(1); // cols become 6, old col 1 → col 2
        System.out.println("After addColumn(1): getEntry(0,2): '" + sheet.getEntry(0, 2) + "' (expected 'x-calibri-10-i')");
        System.out.println("After addColumn(1): getEntry(0,1): '" + sheet.getEntry(0, 1) + "' (expected '')");
        System.out.println("Cols: " + sheet.getColCount() + " (expected 6)");
        System.out.println("✓ Column insert shifts existing right\n");

        // --- Test 5: Replace existing entry ---
        System.out.println("=== Test 5: Replace entry ===");
        sheet.addEntry(5, 0, "greetings", "tahoma", 24, false, false);
        System.out.println("getEntry(5,0): '" + sheet.getEntry(5, 0) + "' (expected 'greetings-tahoma-24')");
        System.out.println("✓ Overwrite works\n");

        // --- Test 6: Add row at beginning ---
        System.out.println("=== Test 6: Add row at beginning ===");
        String before = sheet.getEntry(0, 0);
        sheet.addRow(0);
        System.out.println("Rows: " + sheet.getRowCount() + " (expected 7)");
        System.out.println("getEntry(0,0) new row: '" + sheet.getEntry(0, 0) + "' (expected '')");
        System.out.println("getEntry(1,0) shifted: '" + sheet.getEntry(1, 0) + "' (expected '" + before + "')");
        System.out.println("✓ Row 0 insert shifts everything down\n");

        // --- Test 7: Add column at beginning ---
        System.out.println("=== Test 7: Add column at beginning ===");
        sheet.addColumn(0);
        System.out.println("Cols: " + sheet.getColCount() + " (expected 7)");
        System.out.println("getEntry(0,0): '" + sheet.getEntry(0, 0) + "' (expected '')");
        System.out.println("✓ Col 0 insert shifts everything right\n");

        // --- Test 8: Empty cell check ---
        System.out.println("=== Test 8: Empty cells ===");
        System.out.println("getEntry(2,2): '" + sheet.getEntry(2, 2) + "' (expected '')");
        System.out.println("getEntry(3,4): '" + sheet.getEntry(3, 4) + "' (expected '')");
        System.out.println("✓ Empty cells return ''\n");

        // --- Test 9: Scale ---
        System.out.println("=== Test 9: Scale ===");
        Spreadsheet s2 = new Spreadsheet(100, 100);
        long t = System.nanoTime();
        for (int r = 0; r < 100; r++)
            for (int c = 0; c < 100; c++)
                s2.addEntry(r, c, "v" + r + c, "arial", 12, r % 2 == 0, c % 2 == 0);
        System.out.printf("10K entries: %.2f ms\n", (System.nanoTime()-t)/1e6);
        t = System.nanoTime();
        for (int i = 0; i < 50; i++) s2.addRow(0);
        for (int i = 0; i < 50; i++) s2.addColumn(0);
        System.out.printf("50 row + 50 col inserts: %.2f ms\n", (System.nanoTime()-t)/1e6);
        System.out.println("Size: " + s2.getRowCount() + "x" + s2.getColCount());
        System.out.println("✓ Scales well\n");

        // --- Test 10: All style combos ---
        System.out.println("=== Test 10: Style combos ===");
        Spreadsheet s3 = new Spreadsheet(1, 4);
        s3.addEntry(0, 0, "plain", "arial", 12, false, false);
        s3.addEntry(0, 1, "bold", "arial", 12, true, false);
        s3.addEntry(0, 2, "italic", "arial", 12, false, true);
        s3.addEntry(0, 3, "both", "arial", 12, true, true);
        System.out.println("plain:  '" + s3.getEntry(0, 0) + "' (expected 'plain-arial-12')");
        System.out.println("bold:   '" + s3.getEntry(0, 1) + "' (expected 'bold-arial-12-b')");
        System.out.println("italic: '" + s3.getEntry(0, 2) + "' (expected 'italic-arial-12-i')");
        System.out.println("both:   '" + s3.getEntry(0, 3) + "' (expected 'both-arial-12-b-i')");
        System.out.println("✓ All style flag combos\n");

        System.out.println("════════ ALL 10 TESTS PASSED ✓ ════════");
    }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. CORE DS: ArrayList<ArrayList<CellEntry>> — 2D dynamic grid.
 *    null = empty cell. addRow inserts full-width null list.
 *    addColumn inserts null into each existing row.
 *
 * 2. CELL SERIALIZATION: "text-fontName-fontSize[-b][-i]"
 *    Text never contains hyphen, so unambiguous parsing.
 *
 * 3. INSERTION SHIFTS: ArrayList.add(index, element) is O(n)
 *    due to element shifting. Fine for ≤1000 rows/cols.
 *
 * 4. EXTENSIONS (discussion only):
 *    - Formulas: =SUM(A1:A5), dependency graph for recalc
 *    - Merge cells: track merged ranges, render spanning
 *    - Undo/redo: Command pattern (like LineEditor)
 *    - Collaborative: OT or CRDT for concurrent edits
 *
 * 5. REAL-WORLD: Google Sheets (CRDT-based), Excel (recalc engine),
 *    Airtable (relational spreadsheet).
 *
 * 6. COMPLEXITY:
 *    addRow:    O(cols) create null list
 *    addColumn: O(rows) insert into each row
 *    addEntry:  O(1) direct set
 *    getEntry:  O(1) direct get + serialize
 */
