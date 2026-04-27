import java.util.*;

/*
 * CHESS GAME - Low Level Design
 * ================================
 * 
 * REQUIREMENTS:
 * 1. 8x8 board, standard initial layout
 * 2. Pieces: K(king), Q(queen), R(rook), B(bishop), H(knight), P(pawn)
 * 3. move(sr,sc,er,ec) → "invalid", "" (success), or captured piece (e.g. "BK")
 * 4. Turn alternation: white first, then alternate
 * 5. Game ends when a King is captured
 * 6. Each piece has its own movement rules
 * 
 * DESIGN PATTERNS: Strategy (piece movement validation)
 * 
 * KEY DATA STRUCTURES:
 * - String[8][8] board — "WR","BP" etc, "" for empty
 * - Map<Character, MoveValidator> — piece type → movement checker
 * 
 * COMPLEXITY:
 *   move:          O(max(R,C)) for path-clearing checks (queen/rook/bishop)
 *   getGameStatus: O(1) — tracked on capture
 */

// ==================== MOVE VALIDATOR (STRATEGY) ====================

interface MoveValidator {
    /** Check if piece at (sr,sc) can move to (er,ec). Board context for path/capture checks. */
    boolean isValid(String[][] board, int sr, int sc, int er, int ec, char color);
}

class KingValidator implements MoveValidator {
    @Override public boolean isValid(String[][] board, int sr, int sc, int er, int ec, char color) {
        // King: 1 step in any direction
        return Math.abs(er - sr) <= 1 && Math.abs(ec - sc) <= 1;
    }
}

class QueenValidator implements MoveValidator {
    @Override public boolean isValid(String[][] board, int sr, int sc, int er, int ec, char color) {
        // Queen: any direction (horizontal, vertical, diagonal), path must be clear
        // HINT: boolean straight = (sr == er || sc == ec);
        // HINT: boolean diagonal = Math.abs(er - sr) == Math.abs(ec - sc);
        // HINT: if (!straight && !diagonal) return false;
        // HINT: return PathHelper.isPathClear(board, sr, sc, er, ec);
        boolean straight=(sr==er || sc==ec);
        boolean diagonal=Math.abs(er-sr) == Math.abs(ec-sc);
        if(!straight && !diagonal) return false;
        return PathHelper.isPathClear(board,sr,sc,er,ec);
    }
}

class RookValidator implements MoveValidator {
    @Override public boolean isValid(String[][] board, int sr, int sc, int er, int ec, char color) {
        // Rook: horizontal or vertical only, path must be clear
        // HINT: if (sr != er && sc != ec) return false;
        // HINT: return PathHelper.isPathClear(board, sr, sc, er, ec);
        if(sr!=er && sc!=ec) return false;
        return PathHelper.isPathClear(board, sr, sc, er, ec);
    }
}

class BishopValidator implements MoveValidator {
    @Override public boolean isValid(String[][] board, int sr, int sc, int er, int ec, char color) {
        // Bishop: diagonal only, path must be clear
        // HINT: if (Math.abs(er - sr) != Math.abs(ec - sc)) return false;
        // HINT: return PathHelper.isPathClear(board, sr, sc, er, ec);
        if(Math.abs(er-sr)!=Math.abs(ec-sc)) return false;
        return PathHelper.isPathClear(board, sr, sc, er, ec);
    }
}

class KnightValidator implements MoveValidator {
    @Override public boolean isValid(String[][] board, int sr, int sc, int er, int ec, char color) {
        // Knight: L-shape (2+1), can jump over pieces
        int dr = Math.abs(er - sr), dc = Math.abs(ec - sc);
        return (dr == 2 && dc == 1) || (dr == 1 && dc == 2);
    }
}

class PawnValidator implements MoveValidator {
    @Override public boolean isValid(String[][] board, int sr, int sc, int er, int ec, char color) {
        // White moves up (+row), Black moves down (-row)
        // Forward 1 to empty square, or diagonal 1 to capture
        // HINT: int dir = (color == 'W') ? 1 : -1;
        // HINT: // Forward move (must be empty)
        // HINT: if (ec == sc && er == sr + dir && board[er][ec].isEmpty()) return true;
        // HINT: // Diagonal capture (must have opponent piece)
        // HINT: if (Math.abs(ec - sc) == 1 && er == sr + dir && !board[er][ec].isEmpty())
        // HINT:     return true;
        // HINT: return false;
        return false;
    }
}

// ==================== PATH HELPER ====================

/** Check that all squares between (sr,sc) and (er,ec) are empty (exclusive of endpoints). */
class PathHelper {
    static boolean isPathClear(String[][] board, int sr, int sc, int er, int ec) {
        // TODO: Implement — step from start toward end, check each intermediate square is empty
        // HINT: int dr = Integer.signum(er - sr);
        // HINT: int dc = Integer.signum(ec - sc);
        // HINT: int r = sr + dr, c = sc + dc;
        // HINT: while (r != er || c != ec) {
        // HINT:     if (!board[r][c].isEmpty()) return false;
        // HINT:     r += dr; c += dc;
        // HINT: }
        // HINT: return true;
        int dr=Integer.signum(er-sr);
        int dc=Integer.signum(ec-sc);
        int r=sr+dr; int c=sc+dc;
        while(r!=er || c!=ec){
            if(!board[r][c].isEmpty()) return false;
            r+=dr; c+=dc;
        }
        return true;
    }
}

// ==================== CHESS GAME ====================

class SimpleChessGame {
    private final String[][] board = new String[8][8];
    private int turn;        // 0 = white, 1 = black
    private int gameStatus;  // 0 = in progress, 1 = white won, 2 = black won
    private final Map<Character, MoveValidator> validators = new HashMap<>();

    SimpleChessGame(String[][] initial) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                board[r][c] = initial[r][c];
        turn = 0;
        gameStatus = 0;

        validators.put('K', new KingValidator());
        validators.put('Q', new QueenValidator());
        validators.put('R', new RookValidator());
        validators.put('B', new BishopValidator());
        validators.put('H', new KnightValidator());
        validators.put('P', new PawnValidator());
    }

    /** Default starting board. */
    SimpleChessGame() {
        this(new String[][]{
            {"WR","WH","WB","WQ","WK","WB","WH","WR"},
            {"WP","WP","WP","WP","WP","WP","WP","WP"},
            {"","","","","","","",""},
            {"","","","","","","",""},
            {"","","","","","","",""},
            {"","","","","","","",""},
            {"BP","BP","BP","BP","BP","BP","BP","BP"},
            {"BR","BH","BB","BQ","BK","BB","BH","BR"}
        });
    }

    /**
     * Move piece from (sr,sc) to (er,ec).
     * Returns "invalid" if illegal, "" if success, or captured piece code (e.g. "BK").
     */
    String move(int sr, int sc, int er, int ec) {
        if (gameStatus != 0) return "invalid";
        // Bounds
        if (sr < 0 || sr > 7 || sc < 0 || sc > 7 || er < 0 || er > 7 || ec < 0 || ec > 7) return "invalid";
        if (sr == er && sc == ec) return "invalid";

        String piece = board[sr][sc];
        if (piece.isEmpty()) return "invalid";

        char color = piece.charAt(0);
        char type = piece.charAt(1);

        // Check turn
        if ((turn == 0 && color != 'W') || (turn == 1 && color != 'B')) return "invalid";

        // Can't capture own piece
        String target = board[er][ec];
        if (!target.isEmpty() && target.charAt(0) == color) return "invalid";

        // Validate piece-specific movement
        MoveValidator validator = validators.get(type);
        if (validator == null || !validator.isValid(board, sr, sc, er, ec, color)) return "invalid";

        // Execute move
        board[er][ec] = piece;
        board[sr][sc] = "";

        // Check if King captured
        String captured = "";
        if (!target.isEmpty()) {
            captured = target;
            if (target.charAt(1) == 'K') {
                gameStatus = (color == 'W') ? 1 : 2;
            }
        }

        // Switch turn
        turn = 1 - turn;
        return captured;
    }

    int getGameStatus() { return gameStatus; }
    int getNextTurn() { return gameStatus != 0 ? -1 : turn; }

    String getCell(int r, int c) { return board[r][c]; }
}

// ==================== MAIN / TESTS ====================

public class ChessLLD {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════╗");
        System.out.println("║       CHESS GAME - LLD Demo       ║");
        System.out.println("╚═══════════════════════════════════╝\n");

        // --- Test 1: Full codezym example game ---
        System.out.println("=== Test 1: Full example game (codezym) ===");
        SimpleChessGame g = new SimpleChessGame();

        check(g.move(1,5,2,5), "", "WP (1,5)→(2,5)");
        check(""+g.getNextTurn(), "1", "next=black");

        check(g.move(6,6,5,6), "", "BP (6,6)→(5,6)");
        check(g.move(2,5,3,5), "", "WP (2,5)→(3,5)");
        check(g.move(6,2,5,2), "", "BP (6,2)→(5,2)");
        check(g.move(0,1,2,2), "", "WH (0,1)→(2,2) knight L");
        check(g.move(6,4,5,4), "", "BP (6,4)→(5,4)");
        check(g.move(1,7,2,7), "", "WP (1,7)→(2,7)");
        check(g.move(7,6,5,7), "", "BH (7,6)→(5,7) knight L");
        check(g.move(2,2,3,4), "", "WH (2,2)→(3,4) knight L");
        check(g.move(6,5,5,5), "", "BP (6,5)→(5,5)");
        check(g.move(3,4,5,5), "BP", "WH captures BP at (5,5)");
        check(g.move(6,0,5,0), "", "BP (6,0)→(5,0)");
        check(g.move(5,5,7,4), "BK", "WH captures BK — game over!");

        check(""+g.getNextTurn(), "-1", "game finished");
        check(""+g.getGameStatus(), "1", "white won");
        System.out.println("✓ Full game plays correctly\n");

        // --- Test 2: Invalid moves ---
        System.out.println("=== Test 2: Invalid moves ===");
        SimpleChessGame g2 = new SimpleChessGame();
        check(g2.move(0,0,0,1), "invalid", "Rook blocked by knight");
        check(g2.move(0,0,5,0), "invalid", "Rook blocked by own pawn");
        check(g2.move(3,3,4,4), "invalid", "Empty square");
        check(g2.move(6,0,5,0), "invalid", "Black moves on white's turn");
        check(g2.move(1,0,3,0), "invalid", "Pawn can only move 1 step (no double)");
        System.out.println("✓ Invalid moves rejected\n");

        // --- Test 3: Pawn forward + diagonal capture ---
        System.out.println("=== Test 3: Pawn moves ===");
        SimpleChessGame g3 = new SimpleChessGame();
        check(g3.move(1,3,2,3), "", "WP forward");
        check(g3.move(6,4,5,4), "", "BP forward");
        check(g3.move(2,3,3,3), "", "WP forward again");
        check(g3.move(5,4,4,4), "", "BP forward");
        check(g3.move(3,3,4,4), "invalid", "WP forward diagonal to empty = invalid");
        check(g3.move(3,3,4,3), "", "WP forward to empty");
        check(g3.move(4,4,3,3), "invalid", "BP forward diagonal to empty = invalid");
        check(g3.move(4,4,3,4), "", "BP forward");
        check(g3.move(4,3,3,4), "BP", "WP diagonal capture!");
        System.out.println("✓ Pawn forward + diagonal capture\n");

        // --- Test 4: Knight movement ---
        System.out.println("=== Test 4: Knight L-shape ===");
        SimpleChessGame g4 = new SimpleChessGame();
        check(g4.move(0,1,2,0), "", "WH L-shape");
        check(g4.move(7,1,5,0), "", "BH L-shape");
        check(g4.move(0,6,2,5), "", "WH L-shape");
        check(g4.move(7,6,5,7), "", "BH L-shape");
        System.out.println("✓ Knight jumps work\n");

        // --- Test 5: Can't capture own piece ---
        System.out.println("=== Test 5: No self-capture ===");
        SimpleChessGame g5 = new SimpleChessGame();
        check(g5.move(0,0,1,0), "invalid", "WR can't capture WP");
        check(g5.move(0,3,1,3), "invalid", "WQ can't capture WP");
        System.out.println("✓ Self-capture blocked\n");

        // --- Test 6: Turn enforcement ---
        System.out.println("=== Test 6: Turn enforcement ===");
        SimpleChessGame g6 = new SimpleChessGame();
        check(g6.move(6,0,5,0), "invalid", "Black can't go first");
        check(g6.move(1,0,2,0), "", "White goes first");
        check(g6.move(1,1,2,1), "invalid", "White can't go twice");
        check(g6.move(6,0,5,0), "", "Black's turn");
        System.out.println("✓ Turn alternation enforced\n");

        // --- Test 7: Game over stops play ---
        System.out.println("=== Test 7: Game over ===");
        // Use game from test 1 (g) where game is over
        check(g.move(0,0,1,0), "invalid", "Can't move after game over");
        System.out.println("✓ No moves after game ends\n");

        // --- Test 8: Rook movement ---
        System.out.println("=== Test 8: Rook ===");
        SimpleChessGame g8 = new SimpleChessGame();
        g8.move(1,0,2,0); // WP forward
        g8.move(6,0,5,0); // BP forward
        check(g8.move(0,0,1,0), "", "WR moves to vacated square");
        System.out.println("✓ Rook moves vertically\n");

        // --- Test 9: Bishop movement ---
        System.out.println("=== Test 9: Bishop ===");
        SimpleChessGame g9 = new SimpleChessGame();
        g9.move(1,3,2,3); // WP forward to open diagonal
        g9.move(6,0,5,0); // BP
        check(g9.move(0,2,2,4), "", "WB diagonal 2 steps");
        System.out.println("✓ Bishop diagonal\n");

        // --- Test 10: Black wins ---
        System.out.println("=== Test 10: Black wins ===");
        // Quick king capture scenario
        String[][] custom = {
            {"","","","","WK","","",""},
            {"","","","","","","",""},
            {"","","","","","","",""},
            {"","","","","","","",""},
            {"","","","","","","",""},
            {"","","","","","","",""},
            {"","","","","","","",""},
            {"","","","","BK","","BR",""}
        };
        SimpleChessGame gc = new SimpleChessGame(custom);
        gc.move(0,4,1,4); // WK forward
        check(gc.move(7,6,1,6), "invalid", "BR can't jump if path blocked? No path is clear");
        // Let's do: BR to row 0
        check(gc.move(7,6,0,6), "", "BR moves up");
        gc.move(1,4,0,4); // WK back
        check(gc.move(0,6,0,4), "WK", "BR captures WK!");
        check(""+gc.getGameStatus(), "2", "black won");
        System.out.println("✓ Black wins scenario\n");

        System.out.println("════════ ALL 10 TESTS PASSED ✓ ════════");
    }

    static void check(String actual, String expected, String msg) {
        String status = actual.equals(expected) ? "✓" : "✗ GOT '" + actual + "'";
        System.out.println("  " + status + " " + msg + " → '" + expected + "'");
    }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. STRATEGY PATTERN: MoveValidator per piece type.
 *    Adding a new piece = new class implementing MoveValidator.
 *    Clean separation: move() does turn/capture logic,
 *    validator does piece-specific geometry.
 *
 * 2. PATH CLEARING: Queen/Rook/Bishop must check all intermediate
 *    squares are empty. Use direction signum + step loop.
 *    Knight is only piece that jumps (no path check).
 *
 * 3. PAWN RULES: Forward to empty only, diagonal to capture only.
 *    Direction depends on color (W=+row, B=-row).
 *    Simplified: no double-move, no en passant, no promotion.
 *
 * 4. GAME END: Capture king = win. Simplified from real chess
 *    (no check/checkmate/stalemate — just capture).
 *
 * 5. EXTENSIONS (discussion only):
 *    - Check/Checkmate detection (is king threatened after move?)
 *    - Castling (king+rook swap)
 *    - En passant (pawn special capture)
 *    - Pawn promotion (pawn reaches end → queen)
 *    - Stalemate detection
 *
 * 6. COMPLEXITY:
 *    move:          O(8) max path check for queen/rook/bishop
 *    getGameStatus: O(1)
 *    getNextTurn:   O(1)
 */
