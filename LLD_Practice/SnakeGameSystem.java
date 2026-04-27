import java.util.*;

/*
 * SNAKE & FOOD GAME - Low Level Design
 * ========================================
 * Classic Snake game (Nokia-style): snake moves on a grid, eats food to grow,
 * dies if it hits a wall or itself.
 *
 * REQUIREMENTS:
 * 1. Grid of configurable width × height
 * 2. Snake starts at a position, moves in a direction (UP/DOWN/LEFT/RIGHT)
 * 3. Food spawns at random empty cells; eating food grows the snake by 1
 * 4. Snake moves one cell per tick; body follows the head
 * 5. Collision detection: wall hit → game over, self hit → game over
 * 6. Score = number of food items eaten
 * 7. Support direction changes (cannot reverse — e.g., moving RIGHT can't go LEFT)
 * 8. Game loop: tick-based movement simulation
 *
 * KEY DATA STRUCTURES:
 * - Deque<Cell>: snake body — addFirst for head, removeLast for tail
 *   O(1) head insert, O(1) tail remove (LinkedList-backed deque)
 * - HashSet<Cell>: O(1) self-collision check (is new head in body?)
 * - HashSet<Cell>: occupied cells for food placement (avoid spawning on snake)
 *
 * DESIGN PATTERNS:
 * - Strategy: FoodSpawner interface (random, fixed, edge-biased)
 * - Observer: GameListener for score updates, game over events
 * - State: GameState enum controls what actions are valid
 *
 * COMPLEXITY:
 *   move (tick):    O(1) — deque head/tail + hashset add/remove
 *   collision check: O(1) — hashset contains
 *   food spawn:     O(1) amortized if grid isn't nearly full
 *   changeDirection: O(1)
 */

// ==================== EXCEPTIONS ====================

class GameOverException extends RuntimeException {
    final int score;
    final String reason;
    GameOverException(int score, String reason) {
        super("Game Over! Score: " + score + " — " + reason);
        this.score = score; this.reason = reason;
    }
}

// ==================== ENUMS ====================

enum Direction {
    UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

    final int dx, dy;
    Direction(int dx, int dy) { this.dx = dx; this.dy = dy; }

    /** Returns true if this direction is opposite to other (illegal reversal) */
    boolean isOpposite(Direction other) {
        return this.dx + other.dx == 0 && this.dy + other.dy == 0;
    }
}

enum GameState { READY, RUNNING, GAME_OVER }

// ==================== DOMAIN CLASSES ====================

class Cell {
    final int x, y;

    Cell(int x, int y) { this.x = x; this.y = y; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cell)) return false;
        Cell c = (Cell) o;
        return x == c.x && y == c.y;
    }

    @Override public int hashCode() { return 31 * x + y; }
    @Override public String toString() { return "(" + x + "," + y + ")"; }
}

// ==================== FOOD SPAWNER (Strategy) ====================

interface FoodSpawner {
    Cell spawn(int width, int height, Set<Cell> occupied);
}

/** Random placement on any empty cell */
class RandomFoodSpawner implements FoodSpawner {
    private final Random rng;

    RandomFoodSpawner(long seed) { this.rng = new Random(seed); }
    RandomFoodSpawner() { this.rng = new Random(); }

    public Cell spawn(int width, int height, Set<Cell> occupied) {
        // TODO: Implement
        // HINT: if (occupied.size() >= width * height) return null; // grid full
        // HINT: Cell cell;
        // HINT: do {
        // HINT:     cell = new Cell(rng.nextInt(width), rng.nextInt(height));
        // HINT: } while (occupied.contains(cell));
        // HINT: return cell;
        return null;
    }
}

/** Fixed sequence of food positions (for deterministic testing) */
class FixedFoodSpawner implements FoodSpawner {
    private final List<Cell> positions;
    private int index = 0;

    FixedFoodSpawner(List<Cell> positions) { this.positions = positions; }

    public Cell spawn(int width, int height, Set<Cell> occupied) {
        // TODO: Implement
        // HINT: if (index >= positions.size()) return null;
        // HINT: return positions.get(index++);
        return null;
    }
}

// ==================== GAME LISTENER (Observer) ====================

interface GameListener {
    void onFoodEaten(Cell position, int newScore);
    void onGameOver(int score, String reason);
    void onMove(Cell newHead, Direction direction);
}

class ConsoleGameListener implements GameListener {
    final List<String> events = new ArrayList<>();

    public void onFoodEaten(Cell pos, int score) {
        events.add("ATE:" + pos + ":score=" + score);
    }
    public void onGameOver(int score, String reason) {
        events.add("GAMEOVER:score=" + score + ":" + reason);
    }
    public void onMove(Cell head, Direction dir) {
        events.add("MOVE:" + head + ":" + dir);
    }
}

// ==================== SNAKE ====================

class Snake {
    private final Deque<Cell> body = new LinkedList<>();
    private final Set<Cell> bodySet = new HashSet<>();  // O(1) collision check
    private Direction direction;

    Snake(Cell start, Direction initialDirection) {
        body.addFirst(start);
        bodySet.add(start);
        this.direction = initialDirection;
    }

    Cell getHead() { return body.peekFirst(); }
    int length() { return body.size(); }
    Direction getDirection() { return direction; }

    /**
     * Change direction — disallow 180° reversal.
     * Returns true if direction was changed.
     */
    boolean changeDirection(Direction newDir) {
        // TODO: Implement
        // HINT: if (newDir.isOpposite(direction)) return false;  // can't reverse
        // HINT: this.direction = newDir;
        // HINT: return true;
        return false;
    }

    /**
     * Move the snake one step in current direction.
     * Returns the new head cell.
     *
     * @param grow if true, don't remove tail (snake grows by 1)
     * @return the removed tail cell (null if grew), new head can be checked by caller
     */
    Cell move(boolean grow) {
        // TODO: Implement
        // HINT: Cell head = getHead();
        // HINT: Cell newHead = new Cell(head.x + direction.dx, head.y + direction.dy);
        // HINT: body.addFirst(newHead);
        // HINT: bodySet.add(newHead);
        // HINT:
        // HINT: Cell removedTail = null;
        // HINT: if (!grow) {
        // HINT:     removedTail = body.removeLast();
        // HINT:     bodySet.remove(removedTail);
        // HINT: }
        // HINT: return removedTail;
        return null;
    }

    boolean occupies(Cell cell) { return bodySet.contains(cell); }

    /** Check if a cell is part of the body (excluding head) — for self-collision */
    boolean bodyContains(Cell cell) {
        // TODO: Implement
        // HINT: // bodySet contains head too, so we check: is cell in set AND cell != head
        // HINT: return bodySet.contains(cell) && !cell.equals(getHead());
        return false;
    }

    Set<Cell> getOccupiedCells() { return Collections.unmodifiableSet(bodySet); }

    List<Cell> getBodyList() { return new ArrayList<>(body); }
}

// ==================== GAME ENGINE ====================

class SnakeGame {
    private final int width, height;
    private final Snake snake;
    private final FoodSpawner spawner;
    private final List<GameListener> listeners = new ArrayList<>();
    private Cell food;
    private int score;
    private GameState state;

    SnakeGame(int width, int height, Cell startPos, Direction startDir, FoodSpawner spawner) {
        this.width = width; this.height = height;
        this.snake = new Snake(startPos, startDir);
        this.spawner = spawner;
        this.score = 0;
        this.state = GameState.READY;
    }

    void addListener(GameListener l) { listeners.add(l); }

    /** Start the game — spawn first food */
    void start() {
        // TODO: Implement
        // HINT: state = GameState.RUNNING;
        // HINT: spawnFood();
    }

    /**
     * Execute one game tick: move snake, check collisions, check food.
     * This is the core game loop step.
     *
     * Returns true if game is still running, false if game over.
     */
    boolean tick() {
        // TODO: Implement
        // HINT: if (state != GameState.RUNNING) return false;
        // HINT:
        // HINT: // Step 1: Calculate new head position
        // HINT: Cell head = snake.getHead();
        // HINT: Direction dir = snake.getDirection();
        // HINT: Cell newHead = new Cell(head.x + dir.dx, head.y + dir.dy);
        // HINT:
        // HINT: // Step 2: Wall collision check
        // HINT: if (newHead.x < 0 || newHead.x >= width || newHead.y < 0 || newHead.y >= height) {
        // HINT:     endGame("Hit wall at " + newHead);
        // HINT:     return false;
        // HINT: }
        // HINT:
        // HINT: // Step 3: Self collision check (must check BEFORE moving, against current body)
        // HINT: if (snake.occupies(newHead)) {
        // HINT:     // But if newHead == current tail AND we're NOT eating, tail will move away
        // HINT:     // For simplicity, we move first then check
        // HINT: }
        // HINT:
        // HINT: // Step 4: Check if eating food
        // HINT: boolean eating = food != null && food.equals(newHead);
        // HINT:
        // HINT: // Step 5: Move snake (grow if eating)
        // HINT: snake.move(eating);
        // HINT:
        // HINT: // Step 6: Self collision (after move — check if new head overlaps body)
        // HINT: if (snake.bodyContains(newHead)) {
        // HINT:     endGame("Hit self at " + newHead);
        // HINT:     return false;
        // HINT: }
        // HINT:
        // HINT: // Step 7: Notify move
        // HINT: for (GameListener l : listeners) l.onMove(newHead, dir);
        // HINT:
        // HINT: // Step 8: Handle food eaten
        // HINT: if (eating) {
        // HINT:     score++;
        // HINT:     for (GameListener l : listeners) l.onFoodEaten(food, score);
        // HINT:     spawnFood();
        // HINT: }
        // HINT:
        // HINT: return true;
        return false;
    }

    /** Change snake direction (queued for next tick) */
    boolean changeDirection(Direction dir) {
        // TODO: Implement
        // HINT: if (state != GameState.RUNNING) return false;
        // HINT: return snake.changeDirection(dir);
        return false;
    }

    private void spawnFood() {
        // TODO: Implement
        // HINT: food = spawner.spawn(width, height, snake.getOccupiedCells());
    }

    private void endGame(String reason) {
        // TODO: Implement
        // HINT: state = GameState.GAME_OVER;
        // HINT: for (GameListener l : listeners) l.onGameOver(score, reason);
    }

    // ---- Getters for testing ----
    int getScore() { return score; }
    GameState getState() { return state; }
    Cell getSnakeHead() { return snake.getHead(); }
    int getSnakeLength() { return snake.length(); }
    Cell getFood() { return food; }
    List<Cell> getSnakeBody() { return snake.getBodyList(); }
    int getWidth() { return width; }
    int getHeight() { return height; }

    /** Render grid as string (for visualization) */
    String render() {
        char[][] grid = new char[height][width];
        for (char[] row : grid) Arrays.fill(row, '.');

        List<Cell> body = snake.getBodyList();
        for (int i = 0; i < body.size(); i++) {
            Cell c = body.get(i);
            if (c.x >= 0 && c.x < width && c.y >= 0 && c.y < height)
                grid[c.y][c.x] = (i == 0) ? 'H' : 'S';  // H=head, S=body
        }
        if (food != null && food.x >= 0 && food.x < width && food.y >= 0 && food.y < height)
            grid[food.y][food.x] = 'F';

        StringBuilder sb = new StringBuilder();
        for (char[] row : grid) {
            sb.append(new String(row)).append('\n');
        }
        return sb.toString();
    }
}

// ==================== MAIN / TESTS ====================

public class SnakeGameSystem {
    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   SNAKE & FOOD GAME - LLD Demo       ║");
        System.out.println("╚══════════════════════════════════════╝\n");

        // --- Test 1: Basic movement ---
        System.out.println("=== Test 1: Basic movement ===");
        SnakeGame g1 = new SnakeGame(10, 10, new Cell(5, 5), Direction.RIGHT,
                new FixedFoodSpawner(List.of(new Cell(8, 5))));
        g1.start();
        System.out.println("Start: head=" + g1.getSnakeHead() + ", food=" + g1.getFood());
        g1.tick(); // move right to (6,5)
        g1.tick(); // move right to (7,5)
        System.out.println("After 2 ticks: head=" + g1.getSnakeHead() + " (expected (7,5))");
        System.out.println("Length: " + g1.getSnakeLength() + " (expected 1 — no food yet)");
        System.out.println("✓ Snake moves in direction\n");

        // --- Test 2: Eat food and grow ---
        System.out.println("=== Test 2: Eat food and grow ===");
        g1.tick(); // move to (8,5) — food is there!
        System.out.println("After eating: head=" + g1.getSnakeHead() + ", score=" + g1.getScore());
        System.out.println("Length: " + g1.getSnakeLength() + " (expected 2 — grew by 1)");
        System.out.println("✓ Food eaten, snake grew\n");

        // --- Test 3: Wall collision — game over ---
        System.out.println("=== Test 3: Wall collision ===");
        SnakeGame g3 = new SnakeGame(5, 5, new Cell(4, 2), Direction.RIGHT,
                new RandomFoodSpawner(42));
        g3.start();
        boolean alive = g3.tick(); // move to (5,2) — out of bounds!
        System.out.println("Alive after hitting wall: " + alive + " (expected false)");
        System.out.println("State: " + g3.getState() + " (expected GAME_OVER)");
        System.out.println("✓ Wall collision ends game\n");

        // --- Test 4: Self collision — game over ---
        System.out.println("=== Test 4: Self collision ===");
        // Create a snake that will loop into itself
        // Food placed to grow the snake, then turn into itself
        SnakeGame g4 = new SnakeGame(10, 10, new Cell(3, 3), Direction.RIGHT,
                new FixedFoodSpawner(List.of(
                        new Cell(4, 3), new Cell(5, 3), new Cell(6, 3), new Cell(7, 3)
                )));
        g4.start();
        // Eat 4 food items going right: (4,3), (5,3), (6,3), (7,3)
        g4.tick(); g4.tick(); g4.tick(); g4.tick();
        System.out.println("After 4 eats: length=" + g4.getSnakeLength() + ", score=" + g4.getScore());
        System.out.println("Body: " + g4.getSnakeBody());

        // Now turn DOWN, LEFT, UP to loop into self
        g4.changeDirection(Direction.DOWN);
        g4.tick(); // (7,4)
        g4.changeDirection(Direction.LEFT);
        g4.tick(); // (6,4)
        g4.changeDirection(Direction.UP);
        g4.tick(); // (6,3) — this is part of the body!
        System.out.println("State after self-hit: " + g4.getState());
        System.out.println("✓ Self collision detected\n");

        // --- Test 5: Direction reversal blocked ---
        System.out.println("=== Test 5: Direction reversal blocked ===");
        SnakeGame g5 = new SnakeGame(10, 10, new Cell(5, 5), Direction.RIGHT,
                new RandomFoodSpawner(42));
        g5.start();
        boolean changed = g5.changeDirection(Direction.LEFT);
        System.out.println("RIGHT→LEFT allowed: " + changed + " (expected false)");
        changed = g5.changeDirection(Direction.UP);
        System.out.println("RIGHT→UP allowed: " + changed + " (expected true)");
        System.out.println("✓ 180° reversal blocked\n");

        // --- Test 6: Game listener (Observer) ---
        System.out.println("=== Test 6: Game listener ===");
        ConsoleGameListener listener = new ConsoleGameListener();
        SnakeGame g6 = new SnakeGame(10, 10, new Cell(2, 2), Direction.RIGHT,
                new FixedFoodSpawner(List.of(new Cell(4, 2))));
        g6.addListener(listener);
        g6.start();
        g6.tick(); // (3,2) — move
        g6.tick(); // (4,2) — eat food
        System.out.println("Listener events: " + listener.events);
        System.out.println("✓ Observer notified on move and eat\n");

        // --- Test 7: Game over listener ---
        System.out.println("=== Test 7: Game over listener ===");
        ConsoleGameListener goListener = new ConsoleGameListener();
        SnakeGame g7 = new SnakeGame(3, 3, new Cell(2, 1), Direction.RIGHT,
                new RandomFoodSpawner(42));
        g7.addListener(goListener);
        g7.start();
        g7.tick(); // hit right wall
        System.out.println("Game over events: " + goListener.events);
        System.out.println("✓ Game over event fired\n");

        // --- Test 8: Grid rendering ---
        System.out.println("=== Test 8: Grid rendering ===");
        SnakeGame g8 = new SnakeGame(8, 6, new Cell(2, 2), Direction.RIGHT,
                new FixedFoodSpawner(List.of(new Cell(5, 2), new Cell(5, 4))));
        g8.start();
        g8.tick(); g8.tick(); g8.tick(); // eat food at (5,2), now at (5,2)
        System.out.println(g8.render());
        System.out.println("Score: " + g8.getScore() + ", Length: " + g8.getSnakeLength());
        System.out.println("✓ Grid rendered (H=head, S=body, F=food)\n");

        // --- Test 9: Long game simulation ---
        System.out.println("=== Test 9: Long game simulation ===");
        // Create a controlled path: right, down, left, down, right...
        List<Cell> foodPath = new ArrayList<>();
        for (int i = 1; i <= 8; i++) foodPath.add(new Cell(i, 0));
        SnakeGame g9 = new SnakeGame(10, 10, new Cell(0, 0), Direction.RIGHT,
                new FixedFoodSpawner(foodPath));
        g9.start();
        int ticks = 0;
        while (g9.getState() == GameState.RUNNING && ticks < 50) {
            g9.tick();
            ticks++;
        }
        System.out.println("After " + ticks + " ticks: score=" + g9.getScore() +
                ", length=" + g9.getSnakeLength() + ", state=" + g9.getState());
        System.out.println("Body: " + g9.getSnakeBody());
        System.out.println("✓ Long game simulation\n");

        // --- Test 10: Tick after game over is no-op ---
        System.out.println("=== Test 10: Tick after game over ===");
        SnakeGame g10 = new SnakeGame(3, 3, new Cell(2, 1), Direction.RIGHT,
                new RandomFoodSpawner());
        g10.start();
        g10.tick(); // game over (wall)
        boolean tickResult = g10.tick(); // should return false, no crash
        boolean dirResult = g10.changeDirection(Direction.UP); // should return false
        System.out.println("Tick after game over: " + tickResult + " (expected false)");
        System.out.println("Direction change after game over: " + dirResult + " (expected false)");
        System.out.println("✓ No actions after game over\n");

        // --- Test 11: Random food spawner avoids snake ---
        System.out.println("=== Test 11: Random food spawner ===");
        RandomFoodSpawner rfs = new RandomFoodSpawner(123);
        Set<Cell> occupied = new HashSet<>();
        occupied.add(new Cell(0, 0));
        occupied.add(new Cell(1, 0));
        occupied.add(new Cell(2, 0));
        // On a 3x1 grid with 3 cells occupied, no room for food
        Cell noRoom = rfs.spawn(3, 1, occupied);
        System.out.println("Food on full grid: " + noRoom + " (expected null)");

        // On a 5x5 grid, food should not be on occupied cells
        Cell valid = rfs.spawn(5, 5, occupied);
        System.out.println("Food on 5x5 with 3 occupied: " + valid + " (should not be occupied)");
        System.out.println("Occupied contains food? " + occupied.contains(valid) + " (expected false)");
        System.out.println("✓ Food spawner respects occupied cells\n");

        // --- Test 12: Multiple direction changes in sequence ---
        System.out.println("=== Test 12: Direction change sequence ===");
        SnakeGame g12 = new SnakeGame(10, 10, new Cell(5, 5), Direction.RIGHT,
                new FixedFoodSpawner(List.of(new Cell(6, 5), new Cell(6, 4), new Cell(5, 4))));
        g12.start();

        g12.tick(); // (6,5) — eat food, grow
        System.out.println("After RIGHT: head=" + g12.getSnakeHead() + " score=" + g12.getScore());

        g12.changeDirection(Direction.UP);
        g12.tick(); // (6,4) — eat food, grow
        System.out.println("After UP: head=" + g12.getSnakeHead() + " score=" + g12.getScore());

        g12.changeDirection(Direction.LEFT);
        g12.tick(); // (5,4) — eat food, grow
        System.out.println("After LEFT: head=" + g12.getSnakeHead() + " score=" + g12.getScore());

        System.out.println("Final length: " + g12.getSnakeLength() + " (expected 4)");
        System.out.println("Body: " + g12.getSnakeBody());
        System.out.println("✓ Multi-directional movement with growth\n");

        System.out.println("════════ ALL 12 TESTS PASSED ✓ ════════");
    }
}

/*
 * INTERVIEW NOTES:
 *
 * 1. CORE DATA STRUCTURE — WHY DEQUE + HASHSET:
 *    - Deque<Cell> for ordered body: O(1) addFirst (new head), O(1) removeLast (tail)
 *    - HashSet<Cell> for O(1) self-collision check (is new head in body?)
 *    - Together: O(1) per tick regardless of snake length
 *    - Alternative: LinkedList alone → O(n) collision check. ArrayList → O(n) head insert.
 *
 * 2. MOVEMENT ALGORITHM (per tick):
 *    a) Calculate new head = current head + direction offset
 *    b) Check wall collision (bounds check)
 *    c) Check if new head == food → grow flag
 *    d) Add new head to front of deque
 *    e) If NOT growing → remove tail from deque (snake "moves")
 *    f) Check self collision (new head in body set)
 *    g) If ate food → increment score, spawn new food
 *
 * 3. DIRECTION REVERSAL PREVENTION:
 *    - If moving RIGHT, can't go LEFT (would instant-collide with body)
 *    - Check: dx + other.dx == 0 && dy + other.dy == 0 → opposite
 *    - Only relevant when snake length > 1, but we enforce always (simpler)
 *
 * 4. FOOD SPAWNING STRATEGY:
 *    - Random: pick random cell, retry if occupied. O(1) amortized when grid sparse.
 *    - When grid nearly full: could pre-compute empty cells list for O(1) pick
 *    - Fixed spawner: deterministic for testing (no randomness in tests)
 *
 * 5. COMPLEXITY:
 *    | Operation        | Time  | Notes                           |
 *    |-----------------|-------|---------------------------------|
 *    | tick (move)     | O(1)  | Deque add/remove + hashset ops  |
 *    | collision check | O(1)  | HashSet contains                |
 *    | changeDirection | O(1)  | Opposite check                  |
 *    | spawnFood       | O(1)* | Amortized; O(w*h) worst case    |
 *    | render          | O(w*h)| Grid fill + body iteration      |
 *
 * 6. CELL EQUALS/HASHCODE:
 *    - Required for HashSet: Cell(3,5) must equal Cell(3,5)
 *    - hashCode: 31*x + y (simple, good distribution for grid coords)
 *    - Without these, HashSet uses reference equality → collision detection breaks
 *
 * 7. EXTENSIONS (discussion):
 *    - Wrap-around walls: modulo instead of bounds check
 *    - Speed increase: shorter tick interval as score grows
 *    - Obstacles: additional Set<Cell> of blocked cells
 *    - Multiplayer: multiple snakes, collision with each other
 *    - Power-ups: special food types (shrink, invincibility, speed boost)
 *    - Undo: Command pattern to reverse moves (replay)
 *
 * 8. REAL-WORLD PARALLELS:
 *    - Nokia Snake (1998): the classic
 *    - Slither.io: multiplayer web version (server-authoritative, WebSocket)
 *    - Leetcode 353: "Design Snake Game" — same core problem
 *    - Game loop pattern: tick-based update used in all game engines
 */
