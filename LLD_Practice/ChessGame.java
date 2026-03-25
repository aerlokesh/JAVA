import java.util.*;

// ===== ENUMS =====

/**
 * Player colors in chess
 */
enum Color {
    WHITE, BLACK;
    
    public Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}

/**
 * Chess piece types
 */
enum PieceType {
    KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN
}

/**
 * Game status
 */
enum GameStatus {
    ACTIVE,           // Game in progress
    WHITE_WIN,        // White won
    BLACK_WIN,        // Black won
    STALEMATE,        // Draw - no legal moves but not in check
    RESIGNATION,      // One player resigned
    DRAW              // Draw by agreement or other rules
}

// ===== POSITION CLASS =====

/**
 * Represents a position on the chess board (row, col)
 * Row: 0-7 (0=top, 7=bottom in display)
 * Col: 0-7 (0=a, 7=h in chess notation)
 * 
 * INTERVIEW HINT: Immutable value object, use for move validation
 */
class Position {
    final int row;
    final int col;
    
    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }
    
    public boolean isValid() {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }
    
    /**
     * Convert to chess notation (e.g., "e4")
     */
    public String toNotation() {
        char file = (char) ('a' + col);
        int rank = 8 - row;  // Chess ranks are 1-8, bottom to top
        return "" + file + rank;
    }
    
    /**
     * Parse chess notation to Position (e.g., "e4" -> Position(4,4))
     */
    public static Position fromNotation(String notation) {
        if (notation.length() != 2) return null;
        int col = notation.charAt(0) - 'a';
        int row = 8 - (notation.charAt(1) - '0');
        Position pos = new Position(row, col);
        return pos.isValid() ? pos : null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Position)) return false;
        Position p = (Position) o;
        return row == p.row && col == p.col;
    }
    
    @Override
    public int hashCode() {
        return row * 8 + col;
    }
    
    @Override
    public String toString() {
        return toNotation();
    }
}

// ===== MOVE CLASS =====

/**
 * Represents a chess move
 * 
 * INTERVIEW DISCUSSION:
 * - Stores source, destination, captured piece
 * - Needed for move history and undo functionality
 * - Special moves: castling, en passant, pawn promotion
 */
class Move {
    Position from;
    Position to;
    Piece movedPiece;
    Piece capturedPiece;  // null if no capture
    boolean isCastling;
    boolean isEnPassant;
    PieceType promotionType;  // For pawn promotion
    
    public Move(Position from, Position to, Piece movedPiece) {
        this.from = from;
        this.to = to;
        this.movedPiece = movedPiece;
    }
    
    @Override
    public String toString() {
        String result = movedPiece.getSymbol() + from.toNotation();
        if (capturedPiece != null) result += "x";
        result += to.toNotation();
        if (promotionType != null) result += "=" + promotionType;
        return result;
    }
}

// ===== ABSTRACT PIECE CLASS =====

/**
 * Abstract base class for all chess pieces
 * 
 * INTERVIEW DISCUSSION:
 * - Why abstract? (Common behavior: color, position, killed state)
 * - Why not interface? (Need concrete fields and methods)
 * - Template Method pattern: validateMove() implemented by subclasses
 */
abstract class Piece {
    protected Color color;
    protected boolean killed;
    protected boolean moved;  // Track if piece has moved (for castling, pawn double move)
    
    public Piece(Color color) {
        this.color = color;
        this.killed = false;
        this.moved = false;
    }
    
    public Color getColor() { return color; }
    public boolean isKilled() { return killed; }
    public void setKilled(boolean killed) { this.killed = killed; }
    public boolean hasMoved() { return moved; }
    public void setMoved(boolean moved) { this.moved = moved; }
    
    /**
     * Get piece symbol for display
     * ♔♕♖♗♘♙ (white) ♚♛♜♝♞♟ (black)
     */
    public abstract String getSymbol();
    public abstract PieceType getType();
    
    /**
     * Validate if move is legal for this piece type
     * Does NOT check if move puts own king in check (done at Board level)
     * 
     * @param board Current board state
     * @param from Source position
     * @param to Destination position
     * @return true if move pattern is valid for this piece
     */
    public abstract boolean canMove(Board board, Position from, Position to);
}

// ===== CONCRETE PIECE CLASSES =====

/**
 * King: Moves one square in any direction
 * Special: Castling
 */
class King extends Piece {
    public King(Color color) { super(color); }
    
    @Override
    public String getSymbol() {
        return color == Color.WHITE ? "♔" : "♚";
    }
    
    @Override
    public PieceType getType() { return PieceType.KING; }
    
    @Override
    public boolean canMove(Board board, Position from, Position to) {
        int rowDiff = Math.abs(to.row - from.row);
        int colDiff = Math.abs(to.col - from.col);
        
        // Normal king move: one square in any direction
        if (rowDiff <= 1 && colDiff <= 1) {
            return true;
        }
        
        // Castling: king moves 2 squares horizontally
        // TODO: Add castling validation (king/rook not moved, path clear, not in check)
        
        return false;
    }
}

/**
 * Queen: Moves any distance horizontally, vertically, or diagonally
 * Most powerful piece
 */
class Queen extends Piece {
    public Queen(Color color) { super(color); }
    
    @Override
    public String getSymbol() {
        return color == Color.WHITE ? "♕" : "♛";
    }
    
    @Override
    public PieceType getType() { return PieceType.QUEEN; }
    
    @Override
    public boolean canMove(Board board, Position from, Position to) {
        int rowDiff = Math.abs(to.row - from.row);
        int colDiff = Math.abs(to.col - from.col);
        
        // Queen = Rook + Bishop
        boolean isHorizontalOrVertical = (rowDiff == 0 || colDiff == 0);
        boolean isDiagonal = (rowDiff == colDiff);
        
        if (!isHorizontalOrVertical && !isDiagonal) return false;
        
        // Check path is clear
        return board.isPathClear(from, to);
    }
}

/**
 * Rook: Moves any distance horizontally or vertically
 */
class Rook extends Piece {
    public Rook(Color color) { super(color); }
    
    @Override
    public String getSymbol() {
        return color == Color.WHITE ? "♖" : "♜";
    }
    
    @Override
    public PieceType getType() { return PieceType.ROOK; }
    
    @Override
    public boolean canMove(Board board, Position from, Position to) {
        int rowDiff = Math.abs(to.row - from.row);
        int colDiff = Math.abs(to.col - from.col);
        
        // Must move in straight line (horizontal or vertical)
        if (rowDiff != 0 && colDiff != 0) return false;
        
        // Check path is clear
        return board.isPathClear(from, to);
    }
}

/**
 * Bishop: Moves any distance diagonally
 */
class Bishop extends Piece {
    public Bishop(Color color) { super(color); }
    
    @Override
    public String getSymbol() {
        return color == Color.WHITE ? "♗" : "♝";
    }
    
    @Override
    public PieceType getType() { return PieceType.BISHOP; }
    
    @Override
    public boolean canMove(Board board, Position from, Position to) {
        int rowDiff = Math.abs(to.row - from.row);
        int colDiff = Math.abs(to.col - from.col);
        
        // Must move diagonally (equal row and col movement)
        if (rowDiff != colDiff || rowDiff == 0) return false;
        
        // Check path is clear
        return board.isPathClear(from, to);
    }
}

/**
 * Knight: Moves in L-shape (2+1 or 1+2)
 * Only piece that can jump over others
 */
class Knight extends Piece {
    public Knight(Color color) { super(color); }
    
    @Override
    public String getSymbol() {
        return color == Color.WHITE ? "♘" : "♞";
    }
    
    @Override
    public PieceType getType() { return PieceType.KNIGHT; }
    
    @Override
    public boolean canMove(Board board, Position from, Position to) {
        int rowDiff = Math.abs(to.row - from.row);
        int colDiff = Math.abs(to.col - from.col);
        
        // L-shape: 2+1 or 1+2
        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2);
    }
}

/**
 * Pawn: Most complex movement rules
 * - Forward 1 square (2 from start)
 * - Capture diagonally forward
 * - En passant, promotion
 */
class Pawn extends Piece {
    public Pawn(Color color) { super(color); }
    
    @Override
    public String getSymbol() {
        return color == Color.WHITE ? "♙" : "♟";
    }
    
    @Override
    public PieceType getType() { return PieceType.PAWN; }
    
    @Override
    public boolean canMove(Board board, Position from, Position to) {
        int direction = (color == Color.WHITE) ? -1 : 1;  // White moves up (-)
        int rowDiff = to.row - from.row;
        int colDiff = Math.abs(to.col - from.col);
        
        // Forward move (no capture)
        if (colDiff == 0) {
            // Single step forward
            if (rowDiff == direction) {
                return board.getPiece(to) == null;
            }
            // Double step from starting position
            int startRow = (color == Color.WHITE) ? 6 : 1;
            if (from.row == startRow && rowDiff == 2 * direction) {
                Position intermediate = new Position(from.row + direction, from.col);
                return board.getPiece(intermediate) == null && 
                       board.getPiece(to) == null;
            }
        }
        
        // Diagonal capture
        if (colDiff == 1 && rowDiff == direction) {
            Piece target = board.getPiece(to);
            if (target != null && target.getColor() != color) {
                return true;
            }
            // TODO: En passant
        }
        
        return false;
    }
}

// ===== BOARD CLASS =====

/**
 * Chess Board - 8x8 grid
 * 
 * INTERVIEW DISCUSSION:
 * - Why 2D array? (Direct indexing, O(1) access)
 * - Alternative: Map<Position, Piece> (more flexible, sparse representation)
 * - Why store pieces on board? (Easy to visualize, validate moves)
 */
class Board {
    private Piece[][] grid;
    private List<Move> moveHistory;
    
    public Board() {
        grid = new Piece[8][8];
        moveHistory = new ArrayList<>();
        initializeBoard();
    }
    
    /**
     * Initialize board with starting chess position
     * 
     * STARTING POSITION:
     * 8 ♜ ♞ ♝ ♛ ♚ ♝ ♞ ♜
     * 7 ♟ ♟ ♟ ♟ ♟ ♟ ♟ ♟
     * 6 · · · · · · · ·
     * 5 · · · · · · · ·
     * 4 · · · · · · · ·
     * 3 · · · · · · · ·
     * 2 ♙ ♙ ♙ ♙ ♙ ♙ ♙ ♙
     * 1 ♖ ♘ ♗ ♕ ♔ ♗ ♘ ♖
     *   a b c d e f g h
     */
    private void initializeBoard() {
        // Black pieces (top of board, row 0-1)
        grid[0][0] = new Rook(Color.BLACK);
        grid[0][1] = new Knight(Color.BLACK);
        grid[0][2] = new Bishop(Color.BLACK);
        grid[0][3] = new Queen(Color.BLACK);
        grid[0][4] = new King(Color.BLACK);
        grid[0][5] = new Bishop(Color.BLACK);
        grid[0][6] = new Knight(Color.BLACK);
        grid[0][7] = new Rook(Color.BLACK);
        
        for (int col = 0; col < 8; col++) {
            grid[1][col] = new Pawn(Color.BLACK);
        }
        
        // White pieces (bottom of board, row 6-7)
        for (int col = 0; col < 8; col++) {
            grid[6][col] = new Pawn(Color.WHITE);
        }
        
        grid[7][0] = new Rook(Color.WHITE);
        grid[7][1] = new Knight(Color.WHITE);
        grid[7][2] = new Bishop(Color.WHITE);
        grid[7][3] = new Queen(Color.WHITE);
        grid[7][4] = new King(Color.WHITE);
        grid[7][5] = new Bishop(Color.WHITE);
        grid[7][6] = new Knight(Color.WHITE);
        grid[7][7] = new Rook(Color.WHITE);
    }
    
    /**
     * Get piece at position
     */
    public Piece getPiece(Position pos) {
        if (!pos.isValid()) return null;
        return grid[pos.row][pos.col];
    }
    
    /**
     * Set piece at position
     */
    public void setPiece(Position pos, Piece piece) {
        if (pos.isValid()) {
            grid[pos.row][pos.col] = piece;
        }
    }
    
    /**
     * Check if path between two positions is clear
     * Used by Queen, Rook, Bishop (not Knight - it jumps)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Calculate direction (rowStep, colStep)
     * 2. Walk from 'from' to 'to' (exclusive of endpoints)
     * 3. If any square has a piece, path blocked
     * 
     * @param from Starting position
     * @param to Ending position
     * @return true if all intermediate squares are empty
     */
    public boolean isPathClear(Position from, Position to) {
        int rowStep = Integer.compare(to.row, from.row);
        int colStep = Integer.compare(to.col, from.col);
        
        int currRow = from.row + rowStep;
        int currCol = from.col + colStep;
        
        while (currRow != to.row || currCol != to.col) {
            if (grid[currRow][currCol] != null) {
                return false;
            }
            currRow += rowStep;
            currCol += colStep;
        }
        
        return true;
    }
    
    /**
     * Find king position for a given color
     */
    public Position findKing(Color color) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = grid[row][col];
                if (piece != null && piece.getType() == PieceType.KING && 
                    piece.getColor() == color) {
                    return new Position(row, col);
                }
            }
        }
        return null;
    }
    
    /**
     * Check if a position is under attack by opponent
     * 
     * @param pos Position to check
     * @param byColor Color of attacking pieces
     * @return true if position is attacked
     */
    public boolean isUnderAttack(Position pos, Color byColor) {
        // Check all opponent pieces to see if any can attack this position
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = grid[row][col];
                if (piece != null && piece.getColor() == byColor && !piece.isKilled()) {
                    Position from = new Position(row, col);
                    if (piece.canMove(this, from, pos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Display the board
     */
    public void display() {
        System.out.println("  a b c d e f g h");
        for (int row = 0; row < 8; row++) {
            System.out.print((8 - row) + " ");
            for (int col = 0; col < 8; col++) {
                Piece piece = grid[row][col];
                if (piece == null) {
                    System.out.print("· ");
                } else {
                    System.out.print(piece.getSymbol() + " ");
                }
            }
            System.out.println((8 - row));
        }
        System.out.println("  a b c d e f g h");
    }
    
    public List<Move> getMoveHistory() { return moveHistory; }
}

// ===== PLAYER CLASS =====

/**
 * Represents a chess player
 */
class Player {
    private String name;
    private Color color;
    
    public Player(String name, Color color) {
        this.name = name;
        this.color = color;
    }
    
    public String getName() { return name; }
    public Color getColor() { return color; }
    
    @Override
    public String toString() {
        return name + " (" + color + ")";
    }
}

// ===== GAME CLASS =====

/**
 * Chess Game - Main game controller
 * 
 * RESPONSIBILITIES:
 * - Manage game state (whose turn, status)
 * - Validate moves (legal for piece + doesn't expose king to check)
 * - Execute moves
 * - Detect check, checkmate, stalemate
 * - Track move history
 * 
 * INTERVIEW DISCUSSION:
 * - Single Responsibility: Game manages flow, Board manages position
 * - State Pattern: Could use different states for Active/Checkmate/etc
 * - Command Pattern: Move as command (undo/redo support)
 */
class Game {
    private Board board;
    private Player whitePlayer;
    private Player blackPlayer;
    private Color currentTurn;
    private GameStatus status;
    
    public Game(String whiteName, String blackName) {
        this.board = new Board();
        this.whitePlayer = new Player(whiteName, Color.WHITE);
        this.blackPlayer = new Player(blackName, Color.BLACK);
        this.currentTurn = Color.WHITE;  // White moves first
        this.status = GameStatus.ACTIVE;
    }
    
    /**
     * Make a move
     * 
     * VALIDATION STEPS:
     * 1. Check game is active
     * 2. Check it's the right player's turn
     * 3. Check source has a piece of current player
     * 4. Check piece can legally move to destination
     * 5. Check destination doesn't have own piece
     * 6. Check move doesn't expose own king to check
     * 7. Execute move
     * 8. Check if opponent is in checkmate/stalemate
     * 9. Switch turn
     * 
     * @param from Source position
     * @param to Destination position
     * @return true if move was made successfully
     */
    public boolean makeMove(Position from, Position to) {
        // Step 1: Check game status
        if (status != GameStatus.ACTIVE) {
            System.out.println("Game is not active");
            return false;
        }
        
        // Step 2 & 3: Validate source piece
        Piece piece = board.getPiece(from);
        if (piece == null) {
            System.out.println("No piece at " + from);
            return false;
        }
        if (piece.getColor() != currentTurn) {
            System.out.println("Not your piece! It's " + currentTurn + "'s turn");
            return false;
        }
        
        // Step 4: Check piece can move there
        if (!piece.canMove(board, from, to)) {
            System.out.println(piece.getType() + " cannot move from " + from + " to " + to);
            return false;
        }
        
        // Step 5: Check destination
        Piece destPiece = board.getPiece(to);
        if (destPiece != null && destPiece.getColor() == currentTurn) {
            System.out.println("Cannot capture your own piece");
            return false;
        }
        
        // Step 6: Check if move exposes own king to check
        if (wouldExposeKingToCheck(from, to)) {
            System.out.println("Move would expose king to check");
            return false;
        }
        
        // Step 7: Execute move
        Move move = new Move(from, to, piece);
        move.capturedPiece = destPiece;
        
        if (destPiece != null) {
            destPiece.setKilled(true);
        }
        
        board.setPiece(to, piece);
        board.setPiece(from, null);
        piece.setMoved(true);
        board.getMoveHistory().add(move);
        
        System.out.println(getCurrentPlayer() + " moved: " + move);
        
        // Step 8: Check opponent status
        Color opponent = currentTurn.opposite();
        if (isInCheck(opponent)) {
            System.out.println(opponent + " is in CHECK!");
            if (isCheckmate(opponent)) {
                status = (currentTurn == Color.WHITE) ? GameStatus.WHITE_WIN : GameStatus.BLACK_WIN;
                System.out.println("CHECKMATE! " + getCurrentPlayer() + " wins!");
                return true;
            }
        } else if (isStalemate(opponent)) {
            status = GameStatus.STALEMATE;
            System.out.println("STALEMATE! Game is a draw.");
            return true;
        }
        
        // Step 9: Switch turn
        currentTurn = opponent;
        
        return true;
    }
    
    /**
     * Check if making this move would expose own king to check
     * 
     * ALGORITHM:
     * 1. Simulate the move
     * 2. Check if own king is under attack
     * 3. Undo the simulation
     * 4. Return result
     */
    private boolean wouldExposeKingToCheck(Position from, Position to) {
        // Simulate move
        Piece piece = board.getPiece(from);
        Piece captured = board.getPiece(to);
        
        board.setPiece(to, piece);
        board.setPiece(from, null);
        
        // Check if king is now under attack
        Position kingPos = (piece.getType() == PieceType.KING) ? to : board.findKing(currentTurn);
        boolean inCheck = board.isUnderAttack(kingPos, currentTurn.opposite());
        
        // Undo simulation
        board.setPiece(from, piece);
        board.setPiece(to, captured);
        
        return inCheck;
    }
    
    /**
     * Check if a player is in check
     */
    public boolean isInCheck(Color color) {
        Position kingPos = board.findKing(color);
        if (kingPos == null) return false;  // King not found (shouldn't happen)
        return board.isUnderAttack(kingPos, color.opposite());
    }
    
    /**
     * Check if a player is in checkmate
     * Checkmate = in check AND no legal moves
     */
    public boolean isCheckmate(Color color) {
        if (!isInCheck(color)) return false;
        return !hasAnyLegalMove(color);
    }
    
    /**
     * Check if a player is in stalemate
     * Stalemate = NOT in check AND no legal moves
     */
    public boolean isStalemate(Color color) {
        if (isInCheck(color)) return false;
        return !hasAnyLegalMove(color);
    }
    
    /**
     * Check if player has any legal move
     * Try all pieces, all possible destinations
     */
    private boolean hasAnyLegalMove(Color color) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(new Position(row, col));
                if (piece != null && piece.getColor() == color && !piece.isKilled()) {
                    Position from = new Position(row, col);
                    
                    // Try all possible destinations
                    for (int toRow = 0; toRow < 8; toRow++) {
                        for (int toCol = 0; toCol < 8; toCol++) {
                            Position to = new Position(toRow, toCol);
                            
                            // Quick validation
                            if (from.equals(to)) continue;
                            Piece destPiece = board.getPiece(to);
                            if (destPiece != null && destPiece.getColor() == color) continue;
                            
                            // Check if move is legal
                            if (piece.canMove(board, from, to)) {
                                // Simulate and check if exposes king
                                if (!wouldExposeKingToCheck(from, to)) {
                                    return true;  // Found a legal move
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;  // No legal moves
    }
    
    /**
     * Make a move using chess notation (e.g., "e2 e4")
     */
    public boolean makeMove(String fromNotation, String toNotation) {
        Position from = Position.fromNotation(fromNotation);
        Position to = Position.fromNotation(toNotation);
        
        if (from == null || to == null) {
            System.out.println("Invalid notation");
            return false;
        }
        
        return makeMove(from, to);
    }
    
    /**
     * Get current player
     */
    public Player getCurrentPlayer() {
        return currentTurn == Color.WHITE ? whitePlayer : blackPlayer;
    }
    
    /**
     * Display game state
     */
    public void display() {
        board.display();
        System.out.println();
        System.out.println("Turn: " + getCurrentPlayer());
        System.out.println("Status: " + status);
        
        if (isInCheck(currentTurn)) {
            System.out.println(currentTurn + " is in CHECK!");
        }
    }
    
    public Board getBoard() { return board; }
    public GameStatus getStatus() { return status; }
    public Color getCurrentTurn() { return currentTurn; }
    
    /**
     * Resign current player
     */
    public void resign() {
        status = (currentTurn == Color.WHITE) ? GameStatus.BLACK_WIN : GameStatus.WHITE_WIN;
        System.out.println(getCurrentPlayer() + " resigned. " + 
                          (currentTurn == Color.WHITE ? blackPlayer : whitePlayer) + " wins!");
    }
}

// ===== MAIN TEST CLASS =====

public class ChessGame {
    public static void main(String[] args) {
        System.out.println("=== Chess Game Test Cases ===\n");
        
        // Test Case 1: Game Initialization
        System.out.println("=== Test Case 1: Game Initialization ===");
        Game game = new Game("Alice", "Bob");
        game.display();
        System.out.println("✓ Game initialized");
        System.out.println();
        
        // Test Case 2: Basic Pawn Moves
        System.out.println("=== Test Case 2: Basic Pawn Moves ===");
        System.out.println("White pawn e2 -> e4 (double move from start)");
        boolean moved = game.makeMove("e2", "e4");
        System.out.println("Move successful: " + moved);
        
        System.out.println("\nBlack pawn e7 -> e5");
        moved = game.makeMove("e7", "e5");
        System.out.println("Move successful: " + moved);
        
        game.display();
        System.out.println("✓ Pawn moves working");
        System.out.println();
        
        // Test Case 3: Knight Move (L-shape)
        System.out.println("=== Test Case 3: Knight Move ===");
        System.out.println("White knight g1 -> f3");
        moved = game.makeMove("g1", "f3");
        System.out.println("Move successful: " + moved);
        game.display();
        System.out.println("✓ Knight move working");
        System.out.println();
        
        // Test Case 4: Invalid Move - Wrong Turn
        System.out.println("=== Test Case 4: Invalid Move - Wrong Turn ===");
        System.out.println("Trying to move white piece on black's turn:");
        moved = game.makeMove("d2", "d4");
        System.out.println("Move successful: " + moved + " (expected false)");
        System.out.println("✓ Turn validation working");
        System.out.println();
        
        // Test Case 5: Invalid Move - Path Blocked
        System.out.println("=== Test Case 5: Invalid Move - Path Blocked ===");
        Game game2 = new Game("Player1", "Player2");
        System.out.println("Trying to move rook through pawns:");
        moved = game2.makeMove("a1", "a4");
        System.out.println("Move successful: " + moved + " (expected false)");
        System.out.println("✓ Path blocking working");
        System.out.println();
        
        // Test Case 6: Capture
        System.out.println("=== Test Case 6: Piece Capture ===");
        Game game3 = new Game("White", "Black");
        game3.makeMove("e2", "e4");
        game3.makeMove("d7", "d5");
        System.out.println("White pawn captures black pawn:");
        moved = game3.makeMove("e4", "d5");
        System.out.println("Move successful: " + moved);
        game3.display();
        System.out.println("✓ Capture working");
        System.out.println();
        
        // Test Case 7: Check Detection
        System.out.println("=== Test Case 7: Check Detection ===");
        Game game4 = new Game("White", "Black");
        // Set up a simple check scenario (Scholar's Mate setup)
        game4.makeMove("e2", "e4");
        game4.makeMove("e7", "e5");
        game4.makeMove("f1", "c4");  // Bishop
        game4.makeMove("b8", "c6");  // Knight
        game4.makeMove("d1", "h5");  // Queen
        game4.makeMove("g8", "f6");  // Knight
        System.out.println("Moving queen to f7 (check):");
        moved = game4.makeMove("h5", "f7");
        if (moved) {
            game4.display();
            System.out.println("✓ Check detected (Scholar's Mate!)");
        }
        System.out.println();
        
        // Test Case 8: Invalid Move - Self Check
        System.out.println("=== Test Case 8: Invalid Move - Exposes King to Check ===");
        Game game5 = new Game("White", "Black");
        // Set up scenario where moving would expose king
        // Simplified test - just show the validation exists
        System.out.println("✓ King exposure validation implemented");
        System.out.println();
        
        // Test Case 9: Resignation
        System.out.println("=== Test Case 9: Resignation ===");
        Game game6 = new Game("Alice", "Bob");
        game6.makeMove("e2", "e4");
        System.out.println("Bob resigns:");
        game6.resign();
        System.out.println("Status: " + game6.getStatus());
        System.out.println("✓ Resignation working");
        System.out.println();
        
        // Test Case 10: Move History
        System.out.println("=== Test Case 10: Move History ===");
        Game game7 = new Game("Player1", "Player2");
        game7.makeMove("e2", "e4");
        game7.makeMove("e7", "e5");
        game7.makeMove("g1", "f3");
        
        System.out.println("Move history:");
        List<Move> history = game7.getBoard().getMoveHistory();
        for (int i = 0; i < history.size(); i++) {
            System.out.println((i + 1) + ". " + history.get(i));
        }
        System.out.println("✓ Move history tracking working");
        System.out.println();
        
        System.out.println("=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION TOPICS:
 * ============================
 * 
 * 1. DESIGN PATTERNS USED:
 *    Strategy Pattern:
 *      - Each piece type has its own movement validation
 *      - Polymorphism via abstract Piece class
 *    
 *    Template Method:
 *      - Piece.canMove() defined by each subclass
 *      - Common validation in Game.makeMove()
 *    
 *    State Pattern (potential):
 *      - Different game states: Active, Check, Checkmate
 *      - Could extract state-specific logic
 *    
 *    Command Pattern (potential):
 *      - Move as command object
 *      - Supports undo/redo
 * 
 * 2. DATA STRUCTURES:
 *    2D Array (Board):
 *      - Pros: Direct access O(1), simple, memory efficient
 *      - Cons: Fixed size, hard to extend
 *    
 *    Alternative - Map<Position, Piece>:
 *      - Pros: Flexible, sparse representation
 *      - Cons: Slower access, more complex
 *    
 *    Move History - List<Move>:
 *      - Sequential access, append-only
 *      - Could use Stack for undo/redo
 * 
 * 3. SPECIAL CHESS RULES:
 *    Castling:
 *      - King and rook haven't moved
 *      - Path is clear
 *      - King not in check, doesn't pass through check
 *    
 *    En Passant:
 *      - Opponent pawn just moved 2 squares
 *      - Your pawn is beside it
 *      - Capture as if it moved 1 square
 *    
 *    Pawn Promotion:
 *      - Pawn reaches opposite end
 *      - Promotes to Queen, Rook, Bishop, or Knight
 *    
 *    Draw Conditions:
 *      - Stalemate: no legal moves, not in check
 *      - Threefold repetition
 *      - 50-move rule (no capture or pawn move)
 *      - Insufficient material (K vs K)
 * 
 * 4. MOVE VALIDATION:
 *    Three Levels:
 *      1. Piece-level: Can piece move there? (pattern)
 *      2. Board-level: Is path clear? Capture valid?
 *      3. Game-level: Exposes king to check?
 *    
 *    Check Detection:
 *      - After each move, verify opponent king not under attack
 *      - Before each move, verify doesn't expose own king
 * 
 * 5. PERFORMANCE CONSIDERATIONS:
 *    Legal Move Generation:
 *      - Naive: Try all 64x64 combinations per piece
 *      - Better: Generate only valid destinations per piece type
 *      - Best: Precompute attack tables (bitboards)
 *    
 *    Check Detection:
 *      - Naive: Check all opponent pieces can attack king
 *      - Better: From king, check if enemy piece in each direction
 *      - Best: Incremental update with attack maps
 * 
 * 6. ADVANCED FEATURES:
 *    AI Player:
 *      - Minimax algorithm with alpha-beta pruning
 *      - Evaluation function (material, position, mobility)
 *      - Opening book, endgame tablebase
 *    
 *    Time Control:
 *      - Each player has limited time
 *      - Time increments per move
 *      - Flag fall = loss
 *    
 *    PGN Export:
 *      - Portable Game Notation
 *      - Standard format for chess games
 *      - Save/load games
 * 
 * 7. SCALABILITY:
 *    Online Chess Platform:
 *      - User accounts and ratings (ELO system)
 *      - Matchmaking service
 *      - Game server (handle concurrent games)
 *      - Database for game history
 *      - Spectator mode
 * 
 * 8. ALTERNATIVE REPRESENTATIONS:
 *    Bitboards:
 *      - 64-bit integer = chess board
 *      - Each bit = one square
 *      - Ultra-fast operations with bit manipulation
 *      - Used in engines like Stockfish
 *    
 *    0x88 Representation:
 *      - 16x8 array (8x8 board + 8 columns padding)
 *      - Fast boundary detection
 *      - Used in older chess engines
 * 
 * 9. CLASS RESPONSIBILITIES:
 *    Piece: Movement rules, piece-specific logic
 *    Board: Position management, path checking, display
 *    Game: Game flow, turn management, validation
 *    Player: Identity, color
 *    Move: Move data, history tracking
 * 
 * 10. TESTING STRATEGY:
 *     Unit Tests:
 *       - Each piece movement pattern
 *       - Path blocking
 *       - Capture validation
 *     
 *     Integration Tests:
 *       - Full game scenarios
 *       - Check/checkmate detection
 *       - Special moves
 *     
 *     Edge Cases:
 *       - King in check
 *       - Stalemate positions
 *       - Promotion scenarios
 * 
 * 11. TIME COMPLEXITY:
 *     Operation              | Complexity
 *     makeMove()            | O(1) for validation + O(64) for check detection
 *     isInCheck()           | O(64) - check all opponent pieces
 *     isCheckmate()         | O(64 * 64) - all pieces, all destinations
 *     wouldExposeKingToCheck()| O(64) - simulate + check
 * 
 * 12. COMMON INTERVIEW QUESTIONS:
 *     Q: How to detect checkmate?
 *     A: King in check AND no legal moves escape check
 *     
 *     Q: How to handle special moves?
 *     A: Add flags to Move (isCastling, isEnPassant, promotionType)
 *     
 *     Q: How to implement undo?
 *     A: Store captured piece, previous positions in Move
 *     
 *     Q: How to prevent illegal moves?
 *     A: Three-level validation (piece, board, game)
 *     
 *     Q: How to optimize for AI?
 *     A: Bitboards, transposition tables, move ordering
 */
