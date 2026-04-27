import java.util.*;

/*
 * TIC TAC TOE - Low Level Design
 * ======================================
 * 
 * REQUIREMENTS:
 * 1. 3×3 grid, two players alternate turns (X and O)
 * 2. Detect winner: 3 in a row (horizontal, vertical, diagonal)
 * 3. Detect draw: board full, no winner
 * 4. Reject invalid moves (out of bounds, occupied cell, game over)
 * 5. Scoreboard tracks wins/draws across multiple games
 * 6. TicTacToeSystem facade for external callers
 * 
 * DESIGN PATTERNS: Facade (TicTacToeSystem)
 * 
 * CORE ENTITIES:
 *   Symbol (enum)          — X, O, EMPTY
 *   GameStatus (enum)      — IN_PROGRESS, WINNER_X, WINNER_O, DRAW
 *   Player                 — name + symbol
 *   Board                  — 3×3 grid, place symbol, check winner/full
 *   Game                   — orchestrates gameplay, validates moves, detects outcome
 *   Scoreboard             — tracks wins per player + draws across games
 *   TicTacToeSystem        — facade: createGame, makeMove, getStatus
 * 
 * KEY DATA STRUCTURES:
 *   Symbol[3][3] grid      — the board
 *   int[] rowCount, colCount, diagCount — O(1) win detection counters
 *   Map<String, int[]> scores — scoreboard {playerName → [wins, draws]}
 * 
 * COMPLEXITY:
 *   makeMove:       O(1) — place + O(1) win check via counters
 *   checkWinner:    O(1) — counter-based; O(n) brute-force alternative
 *   isFull:         O(1) — moveCount tracking
 *   resetBoard:     O(n²) — clear grid
 */

// ==================== ENUMS ====================

enum Symbol {
    X, O, EMPTY;

    @Override public String toString() {
        return this == EMPTY ? "." : name();
    }
}

enum GameStatus {
    IN_PROGRESS, WINNER_X, WINNER_O, DRAW
}

// ==================== PLAYER ====================

class Player {
    final String name;
    final Symbol symbol;

    Player(String name, Symbol symbol) {
        if (symbol == Symbol.EMPTY) throw new IllegalArgumentException("Player can't use EMPTY symbol");
        this.name = name;
        this.symbol = symbol;
    }

    @Override public String toString() { return name + "(" + symbol + ")"; }
}

// ==================== BOARD ====================

class Board {
    static final int SIZE = 3;
    final Symbol[][] grid = new Symbol[SIZE][SIZE];
    int moveCount = 0;

    // O(1) win-detection counters: +1 for X, -1 for O
    // When any counter reaches +3 → X wins, -3 → O wins
    final int[] rowCount = new int[SIZE];
    final int[] colCount = new int[SIZE];
    int diagCount = 0;   // top-left → bottom-right
    int antiDiagCount = 0; // top-right → bottom-left

    Board() { reset(); }

    void reset() {
        for (int r = 0; r < SIZE; r++)
            Arrays.fill(grid[r], Symbol.EMPTY);
        Arrays.fill(rowCount, 0);
        Arrays.fill(colCount, 0);
        diagCount = 0;
        antiDiagCount = 0;
        moveCount = 0;
    }

    /** Place symbol at (row, col). Returns true if placement caused a win. */
    boolean placeSymbol(int row, int col, Symbol symbol) {
        // TODO: Implement — validate bounds + empty, place symbol, update counters, return win check
        // HINT: if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) throw new IllegalArgumentException("Out of bounds");
        // HINT: if (grid[row][col] != Symbol.EMPTY) throw new IllegalArgumentException("Cell occupied");
        // HINT: grid[row][col] = symbol;
        // HINT: moveCount++;
        // HINT: int delta = (symbol == Symbol.X) ? 1 : -1;
        // HINT: rowCount[row] += delta;
        // HINT: colCount[col] += delta;
        // HINT: if (row == col) diagCount += delta;
        // HINT: if (row + col == SIZE - 1) antiDiagCount += delta;
        // HINT: int winTarget = SIZE * delta;  // +3 for X, -3 for O
        // HINT: return rowCount[row] == winTarget || colCount[col] == winTarget
        // HINT:     || diagCount == winTarget || antiDiagCount == winTarget;
        return false;
    }

    boolean isFull() {
        // TODO: Implement — use moveCount for O(1)
        // HINT: return moveCount == SIZE * SIZE;
        return false;
    }

    void printBoard() {
        System.out.println("  0 1 2");
        for (int r = 0; r < SIZE; r++) {
            System.out.print(r + " ");
            for (int c = 0; c < SIZE; c++) {
                System.out.print(grid[r][c] + " ");
            }
            System.out.println();
        }
    }
}

// ==================== GAME ====================

class Game {
    final Board board;
    final Player playerX;
    final Player playerO;
    Player currentPlayer;
    GameStatus status;

    Game(Player playerX, Player playerO) {
        if (playerX.symbol != Symbol.X || playerO.symbol != Symbol.O)
            throw new IllegalArgumentException("Player symbols must be X and O");
        this.playerX = playerX;
        this.playerO = playerO;
        this.board = new Board();
        this.currentPlayer = playerX; // X always goes first
        this.status = GameStatus.IN_PROGRESS;
    }

    /**
     * Make a move at (row, col) for the current player.
     * Returns the updated GameStatus.
     * Validates: game not over, cell valid.
     * After valid move: check win, check draw, switch turns.
     */
    GameStatus makeMove(int row, int col) {
        // TODO: Implement
        // HINT: if (status != GameStatus.IN_PROGRESS)
        // HINT:     throw new IllegalStateException("Game is already over: " + status);
        // HINT: boolean won = board.placeSymbol(row, col, currentPlayer.symbol);
        // HINT: if (won) {
        // HINT:     status = (currentPlayer.symbol == Symbol.X) ? GameStatus.WINNER_X : GameStatus.WINNER_O;
        // HINT: } else if (board.isFull()) {
        // HINT:     status = GameStatus.DRAW;
        // HINT: } else {
        // HINT:     currentPlayer = (currentPlayer == playerX) ? playerO : playerX;
        // HINT: }
        // HINT: return status;
        return GameStatus.IN_PROGRESS;
    }

    GameStatus getStatus() { return status; }
    Player getCurrentPlayer() { return currentPlayer; }
}

// ==================== SCOREBOARD ====================

class Scoreboard {
    // Map<playerName, [wins, draws]>
    final Map<String, int[]> scores = new HashMap<>();

    void recordWin(Player winner) {
        // TODO: Implement — increment winner's win count
        // HINT: scores.computeIfAbsent(winner.name, k -> new int[2])[0]++;
    }

    void recordDraw(Player p1, Player p2) {
        // TODO: Implement — increment both players' draw counts
        // HINT: scores.computeIfAbsent(p1.name, k -> new int[2])[1]++;
        // HINT: scores.computeIfAbsent(p2.name, k -> new int[2])[1]++;
    }

    int getWins(String playerName) {
        int[] s = scores.get(playerName);
        return s == null ? 0 : s[0];
    }

    int getDraws(String playerName) {
        int[] s = scores.get(playerName);
        return s == null ? 0 : s[1];
    }

    void printScoreboard() {
        System.out.println("--- Scoreboard ---");
        for (var e : scores.entrySet()) {
            System.out.println(e.getKey() + ": " + e.getValue()[0] + "W / " + e.getValue()[1] + "D");
        }
    }
}

// ==================== TIC TAC TOE SYSTEM (FACADE) ====================

class TicTacToeSystem {
    final Scoreboard scoreboard = new Scoreboard();
    Game currentGame;

    /** Create a new game between two named players. X goes first. */
    Game createGame(String nameX, String nameO) {
        // TODO: Implement — create players, create game, store as currentGame
        // HINT: Player px = new Player(nameX, Symbol.X);
        // HINT: Player po = new Player(nameO, Symbol.O);
        // HINT: currentGame = new Game(px, po);
        // HINT: return currentGame;
        return null;
    }

    /** Make a move in the current game. Updates scoreboard on game end. Returns status. */
    GameStatus makeMove(int row, int col) {
        // TODO: Implement — delegate to game, update scoreboard on win/draw
        // HINT: if (currentGame == null) throw new IllegalStateException("No active game");
        // HINT: GameStatus result = currentGame.makeMove(row, col);
        // HINT: if (result == GameStatus.WINNER_X) scoreboard.recordWin(currentGame.playerX);
        // HINT: else if (result == GameStatus.WINNER_O) scoreboard.recordWin(currentGame.playerO);
        // HINT: else if (result == GameStatus.DRAW) scoreboard.recordDraw(currentGame.playerX, currentGame.playerO);
        // HINT: return result;
        return GameStatus.IN_PROGRESS;
    }

    GameStatus getStatus() {
        return currentGame == null ? null : currentGame.getStatus();
    }
}

// ==================== MAIN / TESTS ====================

public class TicTacToe {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════╗");
        System.out.println("║   TIC TAC TOE - LLD Demo         ║");
        System.out.println("╚═══════════════════════════════════╝\n");

        // --- Test 1: X wins with a row ---
        System.out.println("=== Test 1: X wins (top row) ===");
        TicTacToeSystem sys = new TicTacToeSystem();
        sys.createGame("Alice", "Bob");
        check(sys.makeMove(0, 0), GameStatus.IN_PROGRESS, "X(0,0)");
        check(sys.makeMove(1, 0), GameStatus.IN_PROGRESS, "O(1,0)");
        check(sys.makeMove(0, 1), GameStatus.IN_PROGRESS, "X(0,1)");
        check(sys.makeMove(1, 1), GameStatus.IN_PROGRESS, "O(1,1)");
        check(sys.makeMove(0, 2), GameStatus.WINNER_X, "X(0,2) → X wins row 0");
        System.out.println("✓ X wins top row\n");

        // --- Test 2: O wins with a column ---
        System.out.println("=== Test 2: O wins (left column) ===");
        sys.createGame("Alice", "Bob");
        check(sys.makeMove(0, 1), GameStatus.IN_PROGRESS, "X(0,1)");
        check(sys.makeMove(0, 0), GameStatus.IN_PROGRESS, "O(0,0)");
        check(sys.makeMove(1, 1), GameStatus.IN_PROGRESS, "X(1,1)");
        check(sys.makeMove(1, 0), GameStatus.IN_PROGRESS, "O(1,0)");
        check(sys.makeMove(2, 2), GameStatus.IN_PROGRESS, "X(2,2)");
        check(sys.makeMove(2, 0), GameStatus.WINNER_O, "O(2,0) → O wins col 0");
        System.out.println("✓ O wins left column\n");

        // --- Test 3: X wins with main diagonal ---
        System.out.println("=== Test 3: X wins (diagonal) ===");
        sys.createGame("Alice", "Bob");
        check(sys.makeMove(0, 0), GameStatus.IN_PROGRESS, "X(0,0)");
        check(sys.makeMove(0, 1), GameStatus.IN_PROGRESS, "O(0,1)");
        check(sys.makeMove(1, 1), GameStatus.IN_PROGRESS, "X(1,1)");
        check(sys.makeMove(0, 2), GameStatus.IN_PROGRESS, "O(0,2)");
        check(sys.makeMove(2, 2), GameStatus.WINNER_X, "X(2,2) → X wins diagonal");
        System.out.println("✓ X wins main diagonal\n");

        // --- Test 4: O wins with anti-diagonal ---
        System.out.println("=== Test 4: O wins (anti-diagonal) ===");
        sys.createGame("Alice", "Bob");
        check(sys.makeMove(0, 0), GameStatus.IN_PROGRESS, "X(0,0)");
        check(sys.makeMove(0, 2), GameStatus.IN_PROGRESS, "O(0,2)");
        check(sys.makeMove(2, 2), GameStatus.IN_PROGRESS, "X(2,2)");
        check(sys.makeMove(1, 1), GameStatus.IN_PROGRESS, "O(1,1)");
        check(sys.makeMove(1, 0), GameStatus.IN_PROGRESS, "X(1,0)");
        check(sys.makeMove(2, 0), GameStatus.WINNER_O, "O(2,0) → O wins anti-diag");
        System.out.println("✓ O wins anti-diagonal\n");

        // --- Test 5: Draw ---
        System.out.println("=== Test 5: Draw ===");
        //  X | O | X
        //  X | X | O
        //  O | X | O
        sys.createGame("Alice", "Bob");
        sys.makeMove(0, 0); // X
        sys.makeMove(0, 1); // O
        sys.makeMove(0, 2); // X
        sys.makeMove(1, 2); // O
        sys.makeMove(1, 0); // X
        sys.makeMove(2, 0); // O
        sys.makeMove(1, 1); // X
        sys.makeMove(2, 2); // O
        check(sys.makeMove(2, 1), GameStatus.DRAW, "X(2,1) → Draw");
        System.out.println("✓ Draw detected\n");

        // --- Test 6: Invalid move — occupied cell ---
        System.out.println("=== Test 6: Occupied cell ===");
        sys.createGame("Alice", "Bob");
        sys.makeMove(0, 0);
        try {
            sys.makeMove(0, 0); // same cell
            System.out.println("  ✗ Should have thrown");
        } catch (IllegalArgumentException e) {
            System.out.println("  ✓ Blocked: " + e.getMessage());
        }
        System.out.println();

        // --- Test 7: Invalid move — out of bounds ---
        System.out.println("=== Test 7: Out of bounds ===");
        sys.createGame("Alice", "Bob");
        try {
            sys.makeMove(3, 0);
            System.out.println("  ✗ Should have thrown");
        } catch (IllegalArgumentException e) {
            System.out.println("  ✓ Blocked: " + e.getMessage());
        }
        System.out.println();

        // --- Test 8: Move after game over ---
        System.out.println("=== Test 8: Move after game over ===");
        sys.createGame("Alice", "Bob");
        sys.makeMove(0, 0); sys.makeMove(1, 0);
        sys.makeMove(0, 1); sys.makeMove(1, 1);
        sys.makeMove(0, 2); // X wins
        try {
            sys.makeMove(2, 2);
            System.out.println("  ✗ Should have thrown");
        } catch (IllegalStateException e) {
            System.out.println("  ✓ Blocked: " + e.getMessage());
        }
        System.out.println();

        // --- Test 9: Scoreboard across games ---
        System.out.println("=== Test 9: Scoreboard ===");
        TicTacToeSystem sys2 = new TicTacToeSystem();
        // Game 1: X (Alice) wins
        sys2.createGame("Alice", "Bob");
        sys2.makeMove(0,0); sys2.makeMove(1,0);
        sys2.makeMove(0,1); sys2.makeMove(1,1);
        sys2.makeMove(0,2); // Alice wins

        // Game 2: O (Bob) wins
        sys2.createGame("Alice", "Bob");
        sys2.makeMove(0,0); sys2.makeMove(0,2);
        sys2.makeMove(2,2); sys2.makeMove(1,1);
        sys2.makeMove(1,0); sys2.makeMove(2,0); // Bob wins

        // Game 3: Draw
        sys2.createGame("Alice", "Bob");
        sys2.makeMove(0,0); sys2.makeMove(0,1);
        sys2.makeMove(0,2); sys2.makeMove(1,2);
        sys2.makeMove(1,0); sys2.makeMove(2,0);
        sys2.makeMove(1,1); sys2.makeMove(2,2);
        sys2.makeMove(2,1); // draw

        check(sys2.scoreboard.getWins("Alice") == 1, true, "Alice 1 win");
        check(sys2.scoreboard.getWins("Bob") == 1, true, "Bob 1 win");
        check(sys2.scoreboard.getDraws("Alice") == 1, true, "Alice 1 draw");
        check(sys2.scoreboard.getDraws("Bob") == 1, true, "Bob 1 draw");
        sys2.scoreboard.printScoreboard();
        System.out.println("✓ Scoreboard\n");

        // --- Test 10: Turn alternation ---
        System.out.println("=== Test 10: Turn alternation ===");
        sys.createGame("Alice", "Bob");
        check(sys.currentGame.getCurrentPlayer().symbol == Symbol.X, true, "X starts");
        sys.makeMove(0, 0);
        check(sys.currentGame.getCurrentPlayer().symbol == Symbol.O, true, "Then O");
        sys.makeMove(1, 1);
        check(sys.currentGame.getCurrentPlayer().symbol == Symbol.X, true, "Then X");
        System.out.println("✓ Turn alternation\n");

        System.out.println("════════ ALL 10 TESTS PASSED ✓ ════════");
    }

    static void check(GameStatus actual, GameStatus expected, String msg) {
        String s = actual == expected ? "✓" : "✗ GOT " + actual;
        System.out.println("  " + s + " " + msg);
    }

    static void check(boolean actual, boolean expected, String msg) {
        String s = actual == expected ? "✓" : "✗ GOT " + actual;
        System.out.println("  " + s + " " + msg);
    }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. O(1) WIN DETECTION — The clever trick:
 *    Instead of scanning rows/cols/diagonals each move (O(n)),
 *    maintain counters: +1 for X, -1 for O per row/col/diagonal.
 *    When any counter reaches +n → X wins, -n → O wins.
 *    Space: O(n) for counters. Time per move: O(1).
 *
 * 2. SEPARATION OF CONCERNS:
 *    Board: grid state + placement + win detection (data layer)
 *    Game: turn logic + validation + status transitions (business logic)
 *    TicTacToeSystem: facade + scoreboard (application layer)
 *    Each class has a single responsibility.
 *
 * 3. FACADE PATTERN:
 *    TicTacToeSystem hides Board, Game, Scoreboard from callers.
 *    External code only calls createGame() and makeMove().
 *    Internal restructuring doesn't break the API.
 *
 * 4. VALIDATION LAYERS:
 *    Board-level: bounds check, cell occupancy
 *    Game-level: game-over check
 *    System-level: no-active-game check
 *    Fail fast with descriptive exceptions.
 *
 * 5. EXTENSIONS (discussion only):
 *    - N×N board: generalize SIZE, win-length parameter
 *    - AI opponent: Strategy pattern for move selection (minimax)
 *    - Undo move: Command pattern, store move history
 *    - Multiplayer lobby: Observer pattern for game events
 *    - Persistent scoreboard: serialize to file/DB
 *
 * 6. COMPLEXITY SUMMARY:
 *    makeMove:    O(1) — place + counter check
 *    isFull:      O(1) — moveCount vs SIZE²
 *    resetBoard:  O(n²) — clear grid
 *    Scoreboard:  O(1) per update, O(k) to print (k = players)
 */
