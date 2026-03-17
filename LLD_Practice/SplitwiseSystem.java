import java.util.*;

// ===== CUSTOM EXCEPTION CLASSES =====

/**
 * Exception thrown when a user is not found
 * WHEN TO THROW:
 * - User ID doesn't exist in the system
 * - Operations attempted on non-existent user
 */
class UserNotFoundException extends Exception {
    private String userId;
    
    public UserNotFoundException(String userId) {
        super("User not found: " + userId);
        this.userId = userId;
    }
    
    public String getUserId() {
        return userId;
    }
}

/**
 * Exception thrown when user already exists
 * WHEN TO THROW:
 * - Attempting to add user with duplicate ID
 */
class UserAlreadyExistsException extends Exception {
    private String userId;
    
    public UserAlreadyExistsException(String userId) {
        super("User already exists: " + userId);
        this.userId = userId;
    }
    
    public String getUserId() {
        return userId;
    }
}

/**
 * Exception thrown when an expense is invalid
 * WHEN TO THROW:
 * - Negative or zero amount
 * - Empty participant list
 * - Invalid split ratios/amounts
 */
class InvalidExpenseException extends Exception {
    public InvalidExpenseException(String message) {
        super(message);
    }
}

/**
 * Exception thrown when expense not found
 * WHEN TO THROW:
 * - Expense ID doesn't exist
 */
class ExpenseNotFoundException extends Exception {
    private String expenseId;
    
    public ExpenseNotFoundException(String expenseId) {
        super("Expense not found: " + expenseId);
        this.expenseId = expenseId;
    }
    
    public String getExpenseId() {
        return expenseId;
    }
}

/**
 * Exception thrown when settlement amount is invalid
 * WHEN TO THROW:
 * - Negative settlement amount
 * - Settlement exceeds owed amount
 */
class InvalidSettlementException extends Exception {
    public InvalidSettlementException(String message) {
        super(message);
    }
}

// ===== SUPPORTING CLASSES =====

/**
 * Represents a user in the system
 */
class User {
    private String userId;
    private String name;
    private String email;
    
    public User(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
    }
    
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    
    @Override
    public String toString() {
        return name + " (" + userId + ")";
    }
}

/**
 * Enum for expense split types
 */
enum SplitType {
    EQUAL,      // Split equally among all participants
    EXACT,      // Exact amounts specified for each participant
    PERCENTAGE  // Percentage-based split
}

/**
 * Represents an expense in the system
 */
class Expense {
    private String expenseId;
    private String description;
    private double totalAmount;
    private String paidBy;  // userId who paid
    private Map<String, Double> splits;  // userId -> amount they owe
    private SplitType splitType;
    private long timestamp;
    
    public Expense(String expenseId, String description, double totalAmount, 
                   String paidBy, Map<String, Double> splits, SplitType splitType) {
        this.expenseId = expenseId;
        this.description = description;
        this.totalAmount = totalAmount;
        this.paidBy = paidBy;
        this.splits = splits;
        this.splitType = splitType;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getExpenseId() { return expenseId; }
    public String getDescription() { return description; }
    public double getTotalAmount() { return totalAmount; }
    public String getPaidBy() { return paidBy; }
    public Map<String, Double> getSplits() { return splits; }
    public SplitType getSplitType() { return splitType; }
    public long getTimestamp() { return timestamp; }
}

/**
 * Splitwise System - Low Level Design (LLD)
 * 
 * PROBLEM STATEMENT:
 * Design an expense sharing application like Splitwise that can:
 * 1. Add users to the system
 * 2. Create expenses with different split types (equal, exact, percentage)
 * 3. Track balances between users
 * 4. Settle debts between users
 * 5. Show balance sheet and user transactions
 * 
 * REQUIREMENTS:
 * - Functional: Add users, create expenses, settle up, view balances
 * - Non-Functional: Fast balance lookups, accurate calculations, scalable
 * 
 * INTERVIEW HINTS:
 * - Discuss data structures: HashMap for O(1) lookups
 * - Talk about balance simplification (graph algorithms)
 * - Mention database choice (SQL for ACID, NoSQL for scale)
 * - Discuss precision issues with floating point numbers
 * - Consider multi-currency support
 */
public class SplitwiseSystem {
    
    // HINT 1: Store users by ID for quick lookup
    private Map<String, User> users;
    
    // HINT 2: Store all expenses by ID
    private Map<String, Expense> expenses;
    
    // HINT 3: Track balances: Map<userId, Map<otherUserId, amount>>
    // Positive amount = they owe you, Negative = you owe them
    private Map<String, Map<String, Double>> balances;
    
    // HINT 4: For generating unique IDs
    private int expenseCounter;
    
    /**
     * Constructor - Initialize data structures
     */
    public SplitwiseSystem() {
        // TODO: Initialize all maps and counter
        users = new HashMap<>();
        expenses = new HashMap<>();
        balances = new HashMap<>();
        expenseCounter = 0;
    }
    
    /**
     * Add a new user to the system
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate userId is not null/empty
     * 2. Check if user already exists - throw UserAlreadyExistsException
     * 3. Create User object and store in users map
     * 4. Initialize empty balance map for this user
     * 
     * @param userId Unique user identifier
     * @param name User's name
     * @param email User's email
     * @throws UserAlreadyExistsException if user already exists
     */
    public void addUser(String userId, String name, String email) throws UserAlreadyExistsException {
        // TODO: Implement
        // HINT: if (users.containsKey(userId)) throw new UserAlreadyExistsException(userId);
        // HINT: balances.put(userId, new HashMap<>());
    }
    
    /**
     * Create expense with EQUAL split
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate amount > 0
     * 2. Validate all users exist in system
     * 3. Calculate equal split: totalAmount / participants.size()
     * 4. Create splits map with equal amounts for each participant
     * 5. Update balances between paidBy user and each participant
     * 
     * @param description Expense description
     * @param totalAmount Total expense amount
     * @param paidBy User who paid
     * @param participants List of users sharing the expense
     * @return Generated expense ID
     * @throws UserNotFoundException if any user doesn't exist
     * @throws InvalidExpenseException if amount or participants invalid
     */
    public String addEqualExpense(String description, double totalAmount, 
                                  String paidBy, List<String> participants) 
            throws UserNotFoundException, InvalidExpenseException {
        // TODO: Implement
        // HINT: Validate totalAmount > 0
        // HINT: Check all users exist (including paidBy)
        // HINT: double perPerson = totalAmount / participants.size();
        // HINT: Create splits map and update balances
        return null;
    }
    
    /**
     * Create expense with EXACT amounts for each participant
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate all users exist
     * 2. Validate sum of exactAmounts equals totalAmount (with small epsilon for floating point)
     * 3. Create splits map from exactAmounts
     * 4. Update balances between paidBy and each participant
     * 
     * @param description Expense description
     * @param totalAmount Total expense amount
     * @param paidBy User who paid
     * @param exactAmounts Map of userId -> exact amount they owe
     * @return Generated expense ID
     * @throws UserNotFoundException if any user doesn't exist
     * @throws InvalidExpenseException if amounts don't match total
     */
    public String addExactExpense(String description, double totalAmount, 
                                  String paidBy, Map<String, Double> exactAmounts) 
            throws UserNotFoundException, InvalidExpenseException {
        // TODO: Implement
        // HINT: Calculate sum of exactAmounts.values()
        // HINT: Use Math.abs(sum - totalAmount) < 0.01 for comparison
        return null;
    }
    
    /**
     * Create expense with PERCENTAGE split
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate all users exist
     * 2. Validate percentages sum to 100 (with epsilon)
     * 3. Calculate amounts: totalAmount * (percentage / 100.0)
     * 4. Create splits map and update balances
     * 
     * @param description Expense description
     * @param totalAmount Total expense amount
     * @param paidBy User who paid
     * @param percentages Map of userId -> percentage they owe
     * @return Generated expense ID
     * @throws UserNotFoundException if any user doesn't exist
     * @throws InvalidExpenseException if percentages don't sum to 100
     */
    public String addPercentageExpense(String description, double totalAmount, 
                                       String paidBy, Map<String, Double> percentages) 
            throws UserNotFoundException, InvalidExpenseException {
        // TODO: Implement
        // HINT: Validate percentages sum to 100.0 (with epsilon)
        // HINT: For each user: amount = totalAmount * (percentage / 100.0)
        return null;
    }
    
    /**
     * Update balances between two users
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get or create balance entry for lender -> borrower
     * 2. Add amount to existing balance
     * 3. Also update reverse entry (borrower -> lender) with negative amount
     * 4. Handle case where balance becomes zero
     * 
     * @param lender User who is owed money
     * @param borrower User who owes money
     * @param amount Amount owed
     */
    private void updateBalance(String lender, String borrower, double amount) {
        // TODO: Implement
        // HINT: balances.get(lender).put(borrower, currentBalance + amount);
        // HINT: balances.get(borrower).put(lender, -(currentBalance + amount));
        // HINT: Remove entry if balance becomes 0 (Math.abs(balance) < 0.01)
    }
    
    /**
     * Settle up debt between two users
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate both users exist
     * 2. Get current balance between users
     * 3. Validate amount <= what is owed
     * 4. Update balances by subtracting settlement amount
     * 5. Record settlement as special expense (optional)
     * 
     * @param payer User making payment
     * @param receiver User receiving payment
     * @param amount Settlement amount
     * @throws UserNotFoundException if user doesn't exist
     * @throws InvalidSettlementException if amount invalid
     */
    public void settleUp(String payer, String receiver, double amount) 
            throws UserNotFoundException, InvalidSettlementException {
        // TODO: Implement
        // HINT: Check users exist
        // HINT: Get current owed amount
        // HINT: Validate amount <= owed (with epsilon)
        // HINT: Update balances: reduce debt by amount
    }
    
    /**
     * Get balance for a specific user (what they owe and are owed)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate user exists
     * 2. Get user's balance map
     * 3. Return copy to prevent external modification
     * 
     * @param userId User to get balances for
     * @return Map of userId -> amount (positive = they owe you, negative = you owe them)
     * @throws UserNotFoundException if user doesn't exist
     */
    public Map<String, Double> getUserBalance(String userId) throws UserNotFoundException {
        // TODO: Implement
        // HINT: Return new HashMap<>(balances.get(userId)) for safety
        return null;
    }
    
    /**
     * Get all balances in the system
     * 
     * IMPLEMENTATION HINTS:
     * 1. Iterate through all users' balances
     * 2. Only include non-zero balances
     * 3. Format for easy reading
     * 
     * @return List of balance strings
     */
    public List<String> getBalanceSheet() {
        // TODO: Implement
        // HINT: For each user, iterate their balances
        // HINT: Skip if balance is ~0 (Math.abs(amount) < 0.01)
        // HINT: Format: "User1 owes User2: $50.00"
        return null;
    }
    
    /**
     * Get all expenses involving a user
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate user exists
     * 2. Iterate through all expenses
     * 3. Check if user is paidBy or in splits
     * 4. Return list of matching expenses
     * 
     * @param userId User to get expenses for
     * @return List of expenses
     * @throws UserNotFoundException if user doesn't exist
     */
    public List<Expense> getUserExpenses(String userId) throws UserNotFoundException {
        // TODO: Implement
        // HINT: Check expenses where paidBy == userId OR splits.containsKey(userId)
        return null;
    }
    
    /**
     * Get total amount user has spent (paid for others)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Sum up positive balances (what others owe this user)
     * 2. This represents money they paid that hasn't been settled
     * 
     * @param userId User to calculate for
     * @return Total amount spent for others
     * @throws UserNotFoundException if user doesn't exist
     */
    public double getTotalSpent(String userId) throws UserNotFoundException {
        // TODO: Implement
        // HINT: Sum all positive values in balances.get(userId).values()
        return 0.0;
    }
    
    /**
     * Get total amount user owes to others
     * 
     * IMPLEMENTATION HINTS:
     * 1. Sum up negative balances (what this user owes others)
     * 2. Return absolute value
     * 
     * @param userId User to calculate for
     * @return Total amount owed
     * @throws UserNotFoundException if user doesn't exist
     */
    public double getTotalOwed(String userId) throws UserNotFoundException {
        // TODO: Implement
        // HINT: Sum all negative values in balances.get(userId).values()
        // HINT: Return Math.abs(sum)
        return 0.0;
    }
    
    /**
     * Get net balance for user (positive = owed to them, negative = they owe)
     * 
     * @param userId User to calculate for
     * @return Net balance
     * @throws UserNotFoundException if user doesn't exist
     */
    public double getNetBalance(String userId) throws UserNotFoundException {
        // TODO: Implement
        // HINT: Sum all values in balances.get(userId).values()
        return 0.0;
    }
    
    /**
     * Get expense by ID
     * 
     * @param expenseId Expense ID
     * @return Expense object
     * @throws ExpenseNotFoundException if expense doesn't exist
     */
    public Expense getExpense(String expenseId) throws ExpenseNotFoundException {
        // TODO: Implement
        // HINT: Check if expenses.containsKey(expenseId)
        return null;
    }
    
    /**
     * BONUS: Simplify debts using graph algorithms
     * Minimize number of transactions needed to settle all debts
     * 
     * IMPLEMENTATION HINTS:
     * 1. Calculate net balance for each user
     * 2. Separate into creditors (net positive) and debtors (net negative)
     * 3. Match largest creditor with largest debtor
     * 4. Continue until all debts settled
     * 
     * @return List of optimal transactions to settle all debts
     */
    public List<String> simplifyDebts() {
        // TODO: Implement (BONUS)
        // HINT: Use greedy approach - match highest debt with highest credit
        // HINT: This is similar to minimum cash flow problem
        return new ArrayList<>();
    }
    
    /**
     * Generate unique expense ID
     * 
     * @return Unique expense ID
     */
    private String generateExpenseId() {
        return "EXP" + (++expenseCounter);
    }
    
    // ===== TEST DRIVER =====
    public static void main(String[] args) {
        SplitwiseSystem splitwise = new SplitwiseSystem();
        
        System.out.println("=== Splitwise System Test Cases ===\n");
        
        // Test Case 1: Add Users
        System.out.println("=== Test Case 1: Add Users ===");
        try {
            splitwise.addUser("U1", "Alice", "alice@example.com");
            splitwise.addUser("U2", "Bob", "bob@example.com");
            splitwise.addUser("U3", "Charlie", "charlie@example.com");
            System.out.println("✓ Added 3 users successfully");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 2: Add Equal Expense
        System.out.println("=== Test Case 2: Equal Split Expense ===");
        try {
            String expId = splitwise.addEqualExpense(
                "Dinner at restaurant", 
                300.0, 
                "U1", 
                Arrays.asList("U1", "U2", "U3")
            );
            System.out.println("✓ Created expense: " + expId);
            System.out.println("  Each person owes: $100.00");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 3: View Balances
        System.out.println("=== Test Case 3: View Balance Sheet ===");
        try {
            List<String> balances = splitwise.getBalanceSheet();
            for (String balance : balances) {
                System.out.println(balance);
            }
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 4: Add Exact Expense
        System.out.println("=== Test Case 4: Exact Amount Expense ===");
        try {
            Map<String, Double> exactAmounts = new HashMap<>();
            exactAmounts.put("U1", 50.0);
            exactAmounts.put("U2", 100.0);
            exactAmounts.put("U3", 50.0);
            
            String expId = splitwise.addExactExpense(
                "Movie tickets", 
                200.0, 
                "U2", 
                exactAmounts
            );
            System.out.println("✓ Created expense: " + expId);
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 5: Add Percentage Expense
        System.out.println("=== Test Case 5: Percentage Split Expense ===");
        try {
            Map<String, Double> percentages = new HashMap<>();
            percentages.put("U1", 50.0);  // 50%
            percentages.put("U2", 30.0);  // 30%
            percentages.put("U3", 20.0);  // 20%
            
            String expId = splitwise.addPercentageExpense(
                "Grocery shopping", 
                500.0, 
                "U3", 
                percentages
            );
            System.out.println("✓ Created expense: " + expId);
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 6: View Updated Balances
        System.out.println("=== Test Case 6: Updated Balance Sheet ===");
        try {
            List<String> balances = splitwise.getBalanceSheet();
            for (String balance : balances) {
                System.out.println(balance);
            }
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 7: Settle Up
        System.out.println("=== Test Case 7: Settle Up Payment ===");
        try {
            splitwise.settleUp("U2", "U1", 100.0);
            System.out.println("✓ U2 paid U1: $100.00");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 8: View User Balance
        System.out.println("=== Test Case 8: Individual User Balance ===");
        try {
            Map<String, Double> userBalance = splitwise.getUserBalance("U1");
            System.out.println("Alice's balances:");
            for (Map.Entry<String, Double> entry : userBalance.entrySet()) {
                double amount = entry.getValue();
                String relation = amount > 0 ? "owes Alice" : "is owed by Alice";
                System.out.println("  " + entry.getKey() + " " + relation + ": $" + 
                                   String.format("%.2f", Math.abs(amount)));
            }
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 9: User Statistics
        System.out.println("=== Test Case 9: User Statistics ===");
        try {
            double totalSpent = splitwise.getTotalSpent("U1");
            double totalOwed = splitwise.getTotalOwed("U1");
            double netBalance = splitwise.getNetBalance("U1");
            
            System.out.println("Alice's Statistics:");
            System.out.println("  Total Lent: $" + String.format("%.2f", totalSpent));
            System.out.println("  Total Borrowed: $" + String.format("%.2f", totalOwed));
            System.out.println("  Net Balance: $" + String.format("%.2f", netBalance));
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 10: Exception - Duplicate User
        System.out.println("=== Test Case 10: Exception - Duplicate User ===");
        try {
            splitwise.addUser("U1", "Alice Clone", "alice2@example.com");
            System.out.println("✗ Should have thrown UserAlreadyExistsException");
        } catch (UserAlreadyExistsException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 11: Exception - User Not Found
        System.out.println("=== Test Case 11: Exception - User Not Found ===");
        try {
            splitwise.addEqualExpense("Test", 100.0, "U999", Arrays.asList("U1", "U2"));
            System.out.println("✗ Should have thrown UserNotFoundException");
        } catch (UserNotFoundException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 12: Exception - Invalid Expense
        System.out.println("=== Test Case 12: Exception - Invalid Expense Amount ===");
        try {
            splitwise.addEqualExpense("Invalid", -100.0, "U1", Arrays.asList("U1", "U2"));
            System.out.println("✗ Should have thrown InvalidExpenseException");
        } catch (InvalidExpenseException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        System.out.println("=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION TOPICS:
 * ============================
 * 
 * 1. DATA STRUCTURES:
 *    - HashMap for O(1) user and expense lookups
 *    - Nested HashMap for balance tracking: Map<userId, Map<userId, amount>>
 *    - Why not use a 2D array? (Sparse matrix, dynamic user addition)
 * 
 * 2. BALANCE TRACKING STRATEGY:
 *    - Store pairwise balances (redundant but faster lookups)
 *    - Alternative: Store net balances only (less storage, more calculation)
 *    - Trade-off: Space vs. computation complexity
 * 
 * 3. FLOATING POINT PRECISION:
 *    - Use epsilon (0.01) for equality comparisons
 *    - Alternative: Use BigDecimal for financial calculations
 *    - Or: Store amounts in cents (integers) to avoid floating point issues
 * 
 * 4. DEBT SIMPLIFICATION:
 *    - Minimum Cash Flow problem (NP-hard)
 *    - Greedy approach: Match highest creditor with highest debtor
 *    - Graph representation: Directed weighted graph
 *    - Algorithm: Similar to max-flow min-cut
 * 
 * 5. SCALABILITY:
 *    - Database: SQL for consistency (ACID properties)
 *    - Caching: Redis for balance lookups
 *    - Sharding: Partition by userId
 *    - Event sourcing: Store all transactions, calculate balances on-demand
 * 
 * 6. ADVANCED FEATURES:
 *    - Multi-currency support (exchange rates, base currency)
 *    - Recurring expenses (subscriptions)
 *    - Groups/Trips (aggregate multiple users)
 *    - Expense categories and analytics
 *    - Notifications (reminders for pending payments)
 *    - Payment integrations (Venmo, PayPal)
 * 
 * 7. CONCURRENCY:
 *    - Handle simultaneous expense additions
 *    - Use optimistic locking or versioning
 *    - Transaction isolation levels
 *    - Eventual consistency for balance calculations
 * 
 * 8. SYSTEM DESIGN COMPONENTS:
 *    - API Gateway / Load Balancer
 *    - Application Servers (REST APIs)
 *    - Database (PostgreSQL/MySQL for ACID)
 *    - Cache Layer (Redis for hot data)
 *    - Message Queue (Kafka for async processing)
 *    - Notification Service
 * 
 * 9. API DESIGN:
 *    POST   /users                    - Add user
 *    GET    /users/{id}               - Get user
 *    POST   /expenses                 - Create expense
 *    GET    /expenses/{id}            - Get expense
 *    GET    /users/{id}/balance       - Get user balance
 *    POST   /settlements              - Settle debt
 *    GET    /users/{id}/expenses      - Get user expenses
 *    GET    /balances                 - Get all balances
 * 
 * 10. DESIGN PATTERNS:
 *     - Strategy Pattern: Different split strategies (Equal, Exact, Percentage)
 *     - Factory Pattern: Expense creation
 *     - Observer Pattern: Notify users of expense updates
 *     - Repository Pattern: Data access abstraction
 */
