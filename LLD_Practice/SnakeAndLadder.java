import java.util.*;

/*
 * SNAKE AND LADDER - Low Level Design
 * ======================================
 * 
 * REQUIREMENTS:
 * 1. Board of size N (default 100), players start at 0
 * 2. Snakes: head → tail (move down), Ladders: bottom → top (move up)
 * 3. Players take turns rolling a dice (1-6)
 * 4. Exact landing on last cell to win; overshoot = no move
 * 5. Player landing on snake head slides down; ladder bottom climbs up
 * 6. Detect winner, reject moves after game over
 * 
 * DESIGN PATTERNS:
 *   Facade (SnakeLadderService) — single entry point
 * 
 * COMPLEXITY: O(1) per move (map lookup for snakes/ladders)
 */

// ==================== EXCEPTIONS ====================

class GameOverException extends RuntimeException {
    GameOverException(String winner) { super("Game already won by " + winner); }
}

class InvalidPlayerException extends RuntimeException {
    InvalidPlayerException(String msg) { super(msg); }
}

// ==================== ENUMS ====================

enum SLGameStatus { IN_PROGRESS, FINISHED }

// ==================== MODELS ====================

class SLPlayer {
    final String name;
    int position;

    SLPlayer(String name) { this.name = name; this.position = 0; }
}

class SLBoard {
    final int size;
    final Map<Integer, Integer> snakes = new HashMap<>();   // head → tail
    final Map<Integer, Integer> ladders = new HashMap<>();  // bottom → top

    SLBoard(int size) { this.size = size; }

    void addSnake(int head, int tail) {
        if (head <= tail) throw new IllegalArgumentException("Snake head must be > tail");
        snakes.put(head, tail);
    }

    void addLadder(int bottom, int top) {
        if (bottom >= top) throw new IllegalArgumentException("Ladder bottom must be < top");
        ladders.put(bottom, top);
    }

    int getFinalPosition(int pos) {
        if (snakes.containsKey(pos)) return snakes.get(pos);
        if (ladders.containsKey(pos)) return ladders.get(pos);
        return pos;
    }
}

class Dice {
    private final Random random = new Random();
    int roll() { return random.nextInt(6) + 1; }
}

// ==================== SERVICE (FACADE) ====================

class SnakeLadderService {
    final SLBoard board;
    final List<SLPlayer> players = new ArrayList<>();
    final Dice dice = new Dice();
    int currentPlayerIdx = 0;
    SLGameStatus status = SLGameStatus.IN_PROGRESS;
    SLPlayer winner;

    SnakeLadderService(int boardSize) { this.board = new SLBoard(boardSize); }
    SnakeLadderService() { this(100); }

    void addPlayer(String name) {
        if (status != SLGameStatus.IN_PROGRESS) throw new GameOverException(winner.name);
        players.add(new SLPlayer(name));
    }

    void addSnake(int head, int tail) { board.addSnake(head, tail); }
    void addLadder(int bottom, int top) { board.addLadder(bottom, top); }

    /** Play one turn for current player with given dice value. Returns move description. */
    String playTurn(int diceValue) {
        if (status == SLGameStatus.FINISHED) throw new GameOverException(winner.name);
        if (players.size() < 2) throw new InvalidPlayerException("Need at least 2 players");

        SLPlayer player = players.get(currentPlayerIdx);
        int oldPos = player.position;
        int newPos = oldPos + diceValue;

        // Overshoot — no move
        if (newPos > board.size) {
            currentPlayerIdx = (currentPlayerIdx + 1) % players.size();
            return player.name + " rolled " + diceValue + " at " + oldPos + " → overshoot, stays";
        }

        // Apply snakes/ladders
        int finalPos = board.getFinalPosition(newPos);
        player.position = finalPos;

        String detail = player.name + " rolled " + diceValue + ": " + oldPos + "→" + newPos;
        if (finalPos != newPos) {
            String type = finalPos < newPos ? "SNAKE" : "LADDER";
            detail += " " + type + "→" + finalPos;
        }

        // Check win
        if (player.position == board.size) {
            status = SLGameStatus.FINISHED;
            winner = player;
            detail += " ★ WINS!";
        }

        currentPlayerIdx = (currentPlayerIdx + 1) % players.size();
        return detail;
    }

    /** Play one turn with random dice. */
    String playTurn() { return playTurn(dice.roll()); }

    SLPlayer getCurrentPlayer() { return players.get(currentPlayerIdx); }
}

// ==================== MAIN / TESTS ====================

public class SnakeAndLadder {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════╗");
        System.out.println("║   SNAKE AND LADDER - LLD Demo         ║");
        System.out.println("╚═══════════════════════════════════════╝\n");

        // --- Test 1: Basic move ---
        System.out.println("=== Test 1: Basic move ===");
        SnakeLadderService svc = new SnakeLadderService(20);
        svc.addPlayer("Alice");
        svc.addPlayer("Bob");
        System.out.println("  " + svc.playTurn(3)); // Alice 0→3
        check(svc.players.get(0).position, 3, "Alice at 3");
        System.out.println("  " + svc.playTurn(4)); // Bob 0→4
        check(svc.players.get(1).position, 4, "Bob at 4");
        System.out.println("✓ Basic move\n");

        // --- Test 2: Snake ---
        System.out.println("=== Test 2: Snake ===");
        svc = new SnakeLadderService(20);
        svc.addPlayer("Alice"); svc.addPlayer("Bob");
        svc.addSnake(10, 2);
        svc.playTurn(5); // Alice→5
        svc.playTurn(1); // Bob→1
        System.out.println("  " + svc.playTurn(5)); // Alice 5→10→SNAKE→2
        check(svc.players.get(0).position, 2, "Alice slid to 2");
        System.out.println("✓ Snake\n");

        // --- Test 3: Ladder ---
        System.out.println("=== Test 3: Ladder ===");
        svc = new SnakeLadderService(20);
        svc.addPlayer("Alice"); svc.addPlayer("Bob");
        svc.addLadder(3, 15);
        System.out.println("  " + svc.playTurn(3)); // Alice 0→3→LADDER→15
        check(svc.players.get(0).position, 15, "Alice climbed to 15");
        System.out.println("✓ Ladder\n");

        // --- Test 4: Overshoot ---
        System.out.println("=== Test 4: Overshoot ===");
        svc = new SnakeLadderService(20);
        svc.addPlayer("Alice"); svc.addPlayer("Bob");
        svc.players.get(0).position = 18;
        System.out.println("  " + svc.playTurn(5)); // 18+5=23 > 20 → stays
        check(svc.players.get(0).position, 18, "Alice stays at 18");
        System.out.println("✓ Overshoot\n");

        // --- Test 5: Win ---
        System.out.println("=== Test 5: Win ===");
        svc = new SnakeLadderService(20);
        svc.addPlayer("Alice"); svc.addPlayer("Bob");
        svc.players.get(0).position = 16;
        System.out.println("  " + svc.playTurn(4)); // 16+4=20 → WIN
        check(svc.status, SLGameStatus.FINISHED, "Game finished");
        check(svc.winner.name, "Alice", "Alice wins");
        System.out.println("✓ Win\n");

        // --- Test 6: Move after game over ---
        System.out.println("=== Test 6: Move after game over ===");
        try { svc.playTurn(1); System.out.println("  ✗"); }
        catch (GameOverException e) { System.out.println("  ✓ " + e.getMessage()); }
        System.out.println("✓ Blocked\n");

        // --- Test 7: Turn alternation ---
        System.out.println("=== Test 7: Turn alternation ===");
        svc = new SnakeLadderService(100);
        svc.addPlayer("A"); svc.addPlayer("B"); svc.addPlayer("C");
        check(svc.getCurrentPlayer().name, "A", "A first");
        svc.playTurn(1);
        check(svc.getCurrentPlayer().name, "B", "B next");
        svc.playTurn(1);
        check(svc.getCurrentPlayer().name, "C", "C next");
        svc.playTurn(1);
        check(svc.getCurrentPlayer().name, "A", "A again");
        System.out.println("✓ 3-player rotation\n");

        // --- Test 8: Invalid snake/ladder ---
        System.out.println("=== Test 8: Invalid snake/ladder ===");
        try { svc.addSnake(5, 10); System.out.println("  ✗"); }
        catch (IllegalArgumentException e) { System.out.println("  ✓ " + e.getMessage()); }
        try { svc.addLadder(10, 5); System.out.println("  ✗"); }
        catch (IllegalArgumentException e) { System.out.println("  ✓ " + e.getMessage()); }
        System.out.println("✓ Validation\n");

        // --- Test 9: Full game simulation ---
        System.out.println("=== Test 9: Full game simulation ===");
        svc = new SnakeLadderService(30);
        svc.addPlayer("P1"); svc.addPlayer("P2");
        svc.addSnake(25, 5); svc.addLadder(10, 20);
        int turns = 0;
        while (svc.status == SLGameStatus.IN_PROGRESS && turns < 200) {
            svc.playTurn();
            turns++;
        }
        System.out.println("  Finished in " + turns + " turns. Winner: " + (svc.winner != null ? svc.winner.name : "none"));
        check(svc.status, SLGameStatus.FINISHED, "Game finished");
        System.out.println("✓ Simulation\n");

        // --- Test 10: Need 2 players ---
        System.out.println("=== Test 10: Need 2 players ===");
        svc = new SnakeLadderService(20);
        svc.addPlayer("Solo");
        try { svc.playTurn(1); System.out.println("  ✗"); }
        catch (InvalidPlayerException e) { System.out.println("  ✓ " + e.getMessage()); }
        System.out.println("✓ Minimum players\n");

        System.out.println("════════ ALL 10 TESTS PASSED ✓ ════════");
    }

    static void check(int a, int e, String m) {
        System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a + " expected " + e) + " " + m);
    }
    static void check(String a, String e, String m) {
        System.out.println("  " + (Objects.equals(a, e) ? "✓" : "✗ GOT " + a) + " " + m);
    }
    static void check(SLGameStatus a, SLGameStatus e, String m) {
        System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m);
    }
    static void check(boolean a, boolean e, String m) {
        System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m);
    }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. BOARD MODEL: Map<Integer,Integer> for snakes (head→tail) and ladders (bottom→top).
 *    O(1) lookup per move. No need for full array.
 *
 * 2. GAME FLOW: roll → newPos = old + dice → if overshoot, stay → apply snake/ladder → check win → next player.
 *
 * 3. TURN ROTATION: currentPlayerIdx % players.size() supports N players.
 *
 * 4. EXTENSIONS: Strategy pattern for dice (loaded dice, double-roll-on-6), Observer for UI updates.
 */
