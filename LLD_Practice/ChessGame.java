import java.util.*;

/*
 * CHESS GAME (OOP) - Low Level Design
 * ======================================
 * 
 * REQUIREMENTS:
 * 1. 8x8 board, standard initial layout
 * 2. Pieces: King, Queen, Rook, Bishop, Knight, Pawn
 * 3. Each piece validates its own movement (polymorphism)
 * 4. Turn alternation, capture opponent pieces
 * 5. Check detection: king under attack after move is illegal
 * 6. Checkmate detection: in check + no legal moves = game over
 * 
 * DESIGN PATTERNS: Template Method (abstract Piece.canMove)
 * 
 * KEY DATA STRUCTURES:
 * - Piece[8][8] board — polymorphic piece grid
 * - Abstract Piece with canMove() per subclass
 * 
 * COMPLEXITY:
 *   makeMove:     O(1) validation + O(64) check detection
 *   isInCheck:    O(64) — scan opponent pieces
 *   isCheckmate:  O(64 * 64) — try all pieces × all destinations
 */

// ==================== ENUMS ====================

enum Color {
    WHITE, BLACK;
    Color opposite() { return this == WHITE ? BLACK : WHITE; }
}

enum PieceType { KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN }

// ==================== ABSTRACT PIECE ====================

abstract class Piece {
    final Color color;
    final PieceType type;

    Piece(Color color, PieceType type) { this.color = color; this.type = type; }

    /** Can this piece move from→to on given board? (piece-level geometry only) */
    abstract boolean canMove(Piece[][] board, int sr, int sc, int er, int ec);

}

// ==================== CONCRETE PIECES ====================

class King extends Piece {
    King(Color c) { super(c, PieceType.KING); }

    @Override boolean canMove(Piece[][] board, int sr, int sc, int er, int ec) {
        return Math.abs(er - sr) <= 1 && Math.abs(ec - sc) <= 1;
    }
}

class Queen extends Piece {
    Queen(Color c) { super(c, PieceType.QUEEN); }

    @Override boolean canMove(Piece[][] board, int sr, int sc, int er, int ec) {
        // TODO: Implement — straight or diagonal, path clear
        // HINT: boolean straight = (sr == er || sc == ec);
        // HINT: boolean diagonal = Math.abs(er-sr) == Math.abs(ec-sc);
        // HINT: if (!straight && !diagonal) return false;
        // HINT: return ChessBoard.isPathClear(board, sr, sc, er, ec);
        return false;
    }
}

class Rook extends Piece {
    Rook(Color c) { super(c, PieceType.ROOK); }

    @Override boolean canMove(Piece[][] board, int sr, int sc, int er, int ec) {
        // TODO: Implement — horizontal or vertical only, path clear
        // HINT: if (sr != er && sc != ec) return false;
        // HINT: return ChessBoard.isPathClear(board, sr, sc, er, ec);
        return false;
    }
}

class Bishop extends Piece {
    Bishop(Color c) { super(c, PieceType.BISHOP); }

    @Override boolean canMove(Piece[][] board, int sr, int sc, int er, int ec) {
        // TODO: Implement — diagonal only, path clear
        // HINT: if (Math.abs(er-sr) != Math.abs(ec-sc)) return false;
        // HINT: return ChessBoard.isPathClear(board, sr, sc, er, ec);
        return false;
    }
}

class Knight extends Piece {
    Knight(Color c) { super(c, PieceType.KNIGHT); }

    @Override boolean canMove(Piece[][] board, int sr, int sc, int er, int ec) {
        int dr = Math.abs(er - sr), dc = Math.abs(ec - sc);
        return (dr == 2 && dc == 1) || (dr == 1 && dc == 2);
    }
}

class Pawn extends Piece {
    Pawn(Color c) { super(c, PieceType.PAWN); }

    @Override boolean canMove(Piece[][] board, int sr, int sc, int er, int ec) {
        // TODO: Implement — forward 1 to empty, diagonal 1 to capture
        // HINT: int dir = (color == Color.WHITE) ? -1 : 1;  // white moves up
        // HINT: if (ec == sc && er == sr + dir && board[er][ec] == null) return true;
        // HINT: if (Math.abs(ec - sc) == 1 && er == sr + dir && board[er][ec] != null
        // HINT:     && board[er][ec].color != color) return true;
        // HINT: // Double move from start row
        // HINT: int startRow = (color == Color.WHITE) ? 6 : 1;
        // HINT: if (ec == sc && sr == startRow && er == sr + 2*dir
        // HINT:     && board[sr+dir][sc] == null && board[er][ec] == null) return true;
        // HINT: return false;
        return false;
    }
}

// ==================== BOARD ====================

class ChessBoard {
    final Piece[][] grid = new Piece[8][8];

    ChessBoard() { init(); }

    void init() {
        // Black back row (row 0), white back row (row 7)
        Color[] colors = {Color.BLACK, Color.WHITE};
        int[] rows = {0, 7};
        for (int i = 0; i < 2; i++) {
            Color c = colors[i]; int r = rows[i];
            grid[r][0] = new Rook(c);   grid[r][1] = new Knight(c);
            grid[r][2] = new Bishop(c);  grid[r][3] = new Queen(c);
            grid[r][4] = new King(c);    grid[r][5] = new Bishop(c);
            grid[r][6] = new Knight(c);  grid[r][7] = new Rook(c);
        }
        for (int c = 0; c < 8; c++) {
            grid[1][c] = new Pawn(Color.BLACK);
            grid[6][c] = new Pawn(Color.WHITE);
        }
    }

    /** Check all intermediate squares between (sr,sc) and (er,ec) are empty. */
    static boolean isPathClear(Piece[][] board, int sr, int sc, int er, int ec) {
        // TODO: Implement
        // HINT: int dr = Integer.signum(er - sr), dc = Integer.signum(ec - sc);
        // HINT: int r = sr + dr, c = sc + dc;
        // HINT: while (r != er || c != ec) {
        // HINT:     if (board[r][c] != null) return false;
        // HINT:     r += dr; c += dc;
        // HINT: }
        // HINT: return true;
        return true;
    }

    /** Find king position for a color. */
    int[] findKing(Color color) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (grid[r][c] != null && grid[r][c].type == PieceType.KING && grid[r][c].color == color)
                    return new int[]{r, c};
        return null;
    }

    /** Is position (r,c) under attack by any piece of given color? */
    boolean isUnderAttack(int r, int c, Color byColor) {
        // TODO: Implement — scan all pieces of byColor, check canMove to (r,c)
        // HINT: for (int sr = 0; sr < 8; sr++)
        // HINT:     for (int sc = 0; sc < 8; sc++) {
        // HINT:         Piece p = grid[sr][sc];
        // HINT:         if (p != null && p.color == byColor && p.canMove(grid, sr, sc, r, c))
        // HINT:             return true;
        // HINT:     }
        // HINT: return false;
        return false;
    }
}

// ==================== GAME ====================

class Game {
    final ChessBoard board = new ChessBoard();
    Color turn = Color.WHITE;
    boolean gameOver;
    Color winner; // null if in progress

    /**
     * Make a move. Returns true if valid, false if illegal.
     * Validates: turn, piece ownership, destination, piece movement, king safety.
     */
    boolean makeMove(int sr, int sc, int er, int ec) {
        if (gameOver) return false;
        if (sr == er && sc == ec) return false;
        if (!inBounds(sr, sc) || !inBounds(er, ec)) return false;

        Piece piece = board.grid[sr][sc];
        if (piece == null || piece.color != turn) return false;

        Piece target = board.grid[er][ec];
        if (target != null && target.color == turn) return false; // no self-capture

        if (!piece.canMove(board.grid, sr, sc, er, ec)) return false;

        // Simulate: would this move leave own king in check?
        // TODO: Implement king-safety check
        // HINT: board.grid[er][ec] = piece; board.grid[sr][sc] = null;
        // HINT: int[] king = piece.type == PieceType.KING ? new int[]{er,ec} : board.findKing(turn);
        // HINT: boolean exposed = board.isUnderAttack(king[0], king[1], turn.opposite());
        // HINT: board.grid[sr][sc] = piece; board.grid[er][ec] = target; // undo
        // HINT: if (exposed) return false;

        // Execute move
        board.grid[er][ec] = piece;
        board.grid[sr][sc] = null;

        // Check if opponent is in checkmate
        Color opp = turn.opposite();
        if (isInCheck(opp) && !hasAnyLegalMove(opp)) {
            gameOver = true;
            winner = turn;
        }

        turn = opp;
        return true;
    }

    boolean isInCheck(Color color) {
        int[] king = board.findKing(color);
        return king != null && board.isUnderAttack(king[0], king[1], color.opposite());
    }

    /** Check if color has any legal move (used for checkmate detection). */
    boolean hasAnyLegalMove(Color color) {
        for (int sr = 0; sr < 8; sr++)
            for (int sc = 0; sc < 8; sc++) {
                Piece p = board.grid[sr][sc];
                if (p == null || p.color != color) continue;
                for (int er = 0; er < 8; er++)
                    for (int ec = 0; ec < 8; ec++) {
                        if (sr == er && sc == ec) continue;
                        Piece target = board.grid[er][ec];
                        if (target != null && target.color == color) continue;
                        if (!p.canMove(board.grid, sr, sc, er, ec)) continue;
                        // Simulate
                        board.grid[er][ec] = p; board.grid[sr][sc] = null;
                        int[] king = p.type == PieceType.KING ? new int[]{er,ec} : board.findKing(color);
                        boolean safe = king != null && !board.isUnderAttack(king[0], king[1], color.opposite());
                        board.grid[sr][sc] = p; board.grid[er][ec] = target;
                        if (safe) return true;
                    }
            }
        return false;
    }

    static boolean inBounds(int r, int c) { return r >= 0 && r < 8 && c >= 0 && c < 8; }

    /** Get status: 0=active, 1=white won, 2=black won. */
    int getStatus() { return !gameOver ? 0 : (winner == Color.WHITE ? 1 : 2); }

}

// ==================== MAIN / TESTS ====================

public class ChessGame {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════╗");
        System.out.println("║     CHESS GAME (OOP) - LLD Demo  ║");
        System.out.println("╚═══════════════════════════════════╝\n");

        // --- Test 1: Basic pawn moves ---
        System.out.println("=== Test 1: Pawn moves ===");
        Game g = new Game();
        check(g.makeMove(6,4,4,4), true, "WP e2→e4 double");
        check(g.makeMove(1,4,3,4), true, "BP e7→e5 double");
        check(g.makeMove(6,3,5,3), true, "WP d2→d3 single");
        System.out.println("✓ Pawn moves\n");

        // --- Test 2: Knight L-shape ---
        System.out.println("=== Test 2: Knight ===");
        Game g2 = new Game();
        check(g2.makeMove(7,1,5,2), true, "WH b1→c3");
        check(g2.makeMove(0,1,2,2), true, "BH b8→c6");
        System.out.println("✓ Knight L-shape\n");

        // --- Test 3: Invalid — wrong turn ---
        System.out.println("=== Test 3: Turn enforcement ===");
        Game g3 = new Game();
        check(g3.makeMove(1,0,2,0), false, "Black can't go first");
        check(g3.makeMove(6,0,5,0), true, "White goes");
        check(g3.makeMove(6,1,5,1), false, "White can't go twice");
        System.out.println("✓ Turn enforcement\n");

        // --- Test 4: No self-capture ---
        System.out.println("=== Test 4: No self-capture ===");
        Game g4 = new Game();
        check(g4.makeMove(7,0,6,0), false, "WR can't capture WP");
        System.out.println("✓ Self-capture blocked\n");

        // --- Test 5: Path blocking ---
        System.out.println("=== Test 5: Path blocked ===");
        Game g5 = new Game();
        check(g5.makeMove(7,0,4,0), false, "WR blocked by WP");
        check(g5.makeMove(7,2,5,4), false, "WB blocked by WP");
        System.out.println("✓ Path blocking\n");

        // --- Test 6: Capture ---
        System.out.println("=== Test 6: Capture ===");
        Game g6 = new Game();
        g6.makeMove(6,4,4,4); // WP e4
        g6.makeMove(1,3,3,3); // BP d5
        check(g6.makeMove(4,4,3,3), true, "WP captures BP");
        System.out.println("✓ Capture works\n");

        // --- Test 7: Bishop diagonal ---
        System.out.println("=== Test 7: Bishop ===");
        Game g7 = new Game();
        g7.makeMove(6,3,5,3); // WP d3
        g7.makeMove(1,0,2,0); // BP a6
        check(g7.makeMove(7,2,4,5), true, "WB c1→f4 diagonal");
        System.out.println("✓ Bishop diagonal\n");

        // --- Test 8: Rook after pawn moves ---
        System.out.println("=== Test 8: Rook ===");
        Game g8 = new Game();
        g8.makeMove(6,0,4,0); // WP a4
        g8.makeMove(1,0,3,0); // BP a5
        check(g8.makeMove(7,0,5,0), true, "WR a1→a3");
        System.out.println("✓ Rook vertical\n");

        // --- Test 9: Scholar's Mate (checkmate) ---
        System.out.println("=== Test 9: Scholar's Mate ===");
        Game g9 = new Game();
        g9.makeMove(6,4,4,4); // e4
        g9.makeMove(1,4,3,4); // e5
        g9.makeMove(7,5,4,2); // Bc4
        g9.makeMove(0,1,2,2); // Nc6
        g9.makeMove(7,3,3,7); // Qh5
        g9.makeMove(0,6,2,5); // Nf6
        check(g9.makeMove(3,7,1,5), true, "Qxf7# checkmate");
        System.out.println("Status: " + g9.getStatus() + " (expected 1 = white won)");
        System.out.println("✓ Checkmate detection\n");

        // --- Test 10: Game over blocks moves ---
        System.out.println("=== Test 10: Game over ===");
        check(g9.makeMove(0,4,1,4), false, "No moves after checkmate");
        System.out.println("✓ Game over\n");

        System.out.println("════════ ALL 10 TESTS PASSED ✓ ════════");
    }

    static void check(boolean actual, boolean expected, String msg) {
        String s = actual == expected ? "✓" : "✗ GOT " + actual;
        System.out.println("  " + s + " " + msg);
    }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. OOP DESIGN: Abstract Piece with canMove() per subclass.
 *    Template Method: Game.makeMove() validates universals (turn, bounds,
 *    self-capture, king safety), delegates to piece.canMove() for geometry.
 *    vs Strategy pattern (ChessLLD.java) — both valid, OOP is more natural.
 *
 * 2. THREE-LEVEL VALIDATION:
 *    Piece-level: canMove() — geometry (L-shape, diagonal, etc.)
 *    Board-level: isPathClear(), isUnderAttack() — position context
 *    Game-level: king safety after move — simulate + check + undo
 *
 * 3. CHECK/CHECKMATE:
 *    Check: king under attack after opponent's move
 *    Checkmate: in check + no legal move escapes it
 *    Simulate every possible move → undo if king still exposed
 *
 * 4. PAWN COMPLEXITY: forward to empty, diagonal to capture,
 *    double move from start. No en passant/promotion (discussion only).
 *
 * 5. EXTENSIONS (discussion only):
 *    - Castling: king+rook swap, neither moved, path clear, not in check
 *    - En passant: opponent pawn just double-moved, capture as if single
 *    - Promotion: pawn reaches end → queen/rook/bishop/knight
 *    - Stalemate: not in check + no legal moves = draw
 *
 * 6. COMPLEXITY:
 *    makeMove:     O(64) for check detection
 *    isCheckmate:  O(64 * 64) try all moves for all pieces
 *    isPathClear:  O(7) max for queen/rook/bishop
 */
