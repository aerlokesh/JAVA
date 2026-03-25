import java.util.*;
import java.util.stream.Collectors;

// ===== CUSTOM EXCEPTION CLASSES =====

class UserNotFoundException extends Exception {
    private String userId;
    public UserNotFoundException(String userId) {
        super("User not found: " + userId);
        this.userId = userId;
    }
    public String getUserId() { return userId; }
}

class UserAlreadyExistsException extends Exception {
    private String userId;
    public UserAlreadyExistsException(String userId) {
        super("User already exists: " + userId);
        this.userId = userId;
    }
    public String getUserId() { return userId; }
}

class InvalidExpenseException extends Exception {
    public InvalidExpenseException(String message) { super(message); }
}

class ExpenseNotFoundException extends Exception {
    private String expenseId;
    public ExpenseNotFoundException(String expenseId) {
        super("Expense not found: " + expenseId);
        this.expenseId = expenseId;
    }
    public String getExpenseId() { return expenseId; }
}

class InvalidSettlementException extends Exception {
    public InvalidSettlementException(String message) { super(message); }
}

// ===== STRATEGY PATTERN FOR SPLIT CALCULATION =====

/**
 * Strategy interface for splitting expenses
 * Similar to LoadBalancingStrategy in LoadBalancerSystem
 * Each split type is encapsulated in its own strategy class
 */
interface SplitStrategy {
    /**
     * Calculate how much each participant owes
     * @param totalAmount Total expense amount
     * @param participants List of participant user IDs
     * @param splitDetails Optional details (exact amounts or percentages) - can be null for EQUAL
     * @return Map of userId -> amount they owe
     * @throws InvalidExpenseException if split details are invalid
     */
    Map<String, Double> calculateSplit(double totalAmount, List<String> participants, 
                                        Map<String, Double> splitDetails) throws InvalidExpenseException;
    SplitType getSplitType();
}

/**
 * EQUAL SPLIT STRATEGY
 * Divides total amount equally among all participants
 * Similar to RoundRobinStrategy - simple, fair distribution
 */
class EqualSplitStrategy implements SplitStrategy {
    @Override
    public Map<String, Double> calculateSplit(double totalAmount, List<String> participants,
                                               Map<String, Double> splitDetails) throws InvalidExpenseException {
        if (participants == null || participants.isEmpty()) 
            throw new InvalidExpenseException("Participants list cannot be empty");
        
        double perPerson = totalAmount / participants.size();
        Map<String, Double> splits = new HashMap<>();
        for (String userId : participants) {
            splits.put(userId, perPerson);
        }
        return splits;
    }
    
    @Override
    public SplitType getSplitType() { return SplitType.EQUAL; }
}

/**
 * EXACT SPLIT STRATEGY
 * Each participant pays an exact specified amount
 * Similar to LeastConnectionsStrategy - considers actual values
 */
class ExactSplitStrategy implements SplitStrategy {
    @Override
    public Map<String, Double> calculateSplit(double totalAmount, List<String> participants,
                                               Map<String, Double> splitDetails) throws InvalidExpenseException {
        if (splitDetails == null || splitDetails.isEmpty()) 
            throw new InvalidExpenseException("Exact amounts cannot be empty");
        
        double sum = splitDetails.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(totalAmount - sum) > 0.01) 
            throw new InvalidExpenseException("Sum of exact amounts must equal total amount");
        
        return new HashMap<>(splitDetails);
    }
    
    @Override
    public SplitType getSplitType() { return SplitType.EXACT; }
}

/**
 * PERCENTAGE SPLIT STRATEGY
 * Each participant pays a percentage of the total
 * Similar to RandomStrategy - distributes based on configured weights
 */
class PercentageSplitStrategy implements SplitStrategy {
    @Override
    public Map<String, Double> calculateSplit(double totalAmount, List<String> participants,
                                               Map<String, Double> splitDetails) throws InvalidExpenseException {
        if (splitDetails == null || splitDetails.isEmpty()) 
            throw new InvalidExpenseException("Percentages cannot be empty");
        
        double percentageSum = splitDetails.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(percentageSum - 100.0) > 0.01) 
            throw new InvalidExpenseException("Percentages must sum to 100");
        
        Map<String, Double> splits = new HashMap<>();
        for (Map.Entry<String, Double> entry : splitDetails.entrySet()) {
            splits.put(entry.getKey(), totalAmount * entry.getValue() / 100.0);
        }
        return splits;
    }
    
    @Override
    public SplitType getSplitType() { return SplitType.PERCENTAGE; }
}

// ===== SUPPORTING CLASSES =====

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
    public String toString() { return name + " (" + userId + ")"; }
}

enum SplitType {
    EQUAL,
    EXACT,
    PERCENTAGE
}

class Expense {
    private String expenseId;
    private String description;
    private double totalAmount;
    private String paidBy;
    private Map<String, Double> splits;
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
 */
public class SplitwiseSystem {
    
    private Map<String, User> users;
    private Map<String, Expense> expenses;
    // balances: Map<userId, Map<otherUserId, amount>>
    // Positive = they owe you, Negative = you owe them
    private Map<String, Map<String, Double>> balances;
    private int expenseCounter;
    
    public SplitwiseSystem() {
        users = new HashMap<>();
        expenses = new HashMap<>();
        balances = new HashMap<>();
        expenseCounter = 0;
    }
    
    /**
     * Add a new user to the system
     */
    public void addUser(String userId, String name, String email) throws UserAlreadyExistsException {
        if (userId == null || userId.isEmpty()) return;
        if (users.containsKey(userId)) throw new UserAlreadyExistsException(userId);
        users.put(userId, new User(userId, name, email));
        balances.put(userId, new HashMap<>());
    }
    
    /**
     * UNIFIED addExpense using STRATEGY PATTERN
     * This is the core method - all split types delegate to the chosen SplitStrategy.
     * Similar to how LoadBalancer.routeRequest() delegates to LoadBalancingStrategy.
     *
     * @param description  Expense description
     * @param totalAmount  Total expense amount
     * @param paidBy       User ID of the payer
     * @param participants List of participant user IDs (used by EqualSplitStrategy)
     * @param splitDetails Map of userId -> detail value (exact amounts or percentages), can be null for EQUAL
     * @param strategy     The SplitStrategy to use for calculating splits
     * @return Expense ID
     */
    public String addExpense(String description, double totalAmount, String paidBy,
                             List<String> participants, Map<String, Double> splitDetails,
                             SplitStrategy strategy) 
            throws UserNotFoundException, InvalidExpenseException {
        // Validate common fields
        if (totalAmount <= 0) throw new InvalidExpenseException("Amount should be greater than 0");
        if (!users.containsKey(paidBy)) throw new UserNotFoundException(paidBy);
        
        // Validate all participant user IDs exist
        Set<String> allUserIds = new HashSet<>();
        if (participants != null) allUserIds.addAll(participants);
        if (splitDetails != null) allUserIds.addAll(splitDetails.keySet());
        for (String userId : allUserIds) {
            if (!users.containsKey(userId)) throw new UserNotFoundException(userId);
        }
        
        // DELEGATE split calculation to the strategy (Strategy Pattern!)
        Map<String, Double> splits = strategy.calculateSplit(totalAmount, participants, splitDetails);
        L
        // Create and store the expense
        String expenseId = "EXP_" + (++expenseCounter);
        Expense expense = new Expense(expenseId, description, totalAmount, paidBy, splits, strategy.getSplitType());
        expenses.put(expenseId, expense);
        
        // Update balances: each participant (except payer) owes the payer
        for (Map.Entry<String, Double> entry : splits.entrySet()) {
            if (entry.getKey().equals(paidBy)) continue;
            updateBalance(paidBy, entry.getKey(), entry.getValue());
        }
        return expenseId;
    }
    
    /**
     * Convenience: Create expense with EQUAL split (delegates to Strategy)
     */
    public String addEqualExpense(String description, double totalAmount, 
                                  String paidBy, List<String> participants) 
            throws UserNotFoundException, InvalidExpenseException {
        return addExpense(description, totalAmount, paidBy, participants, null, new EqualSplitStrategy());
    }
    
    /**
     * Convenience: Create expense with EXACT amounts (delegates to Strategy)
     */
    public String addExactExpense(String description, double totalAmount, 
                                  String paidBy, Map<String, Double> exactAmounts) 
            throws UserNotFoundException, InvalidExpenseException {
        List<String> participants = new ArrayList<>(exactAmounts.keySet());
        return addExpense(description, totalAmount, paidBy, participants, exactAmounts, new ExactSplitStrategy());
    }
    
    /**
     * Convenience: Create expense with PERCENTAGE split (delegates to Strategy)
     */
    public String addPercentageExpense(String description, double totalAmount, 
                                       String paidBy, Map<String, Double> percentages) 
            throws UserNotFoundException, InvalidExpenseException {
        List<String> participants = new ArrayList<>(percentages.keySet());
        return addExpense(description, totalAmount, paidBy, participants, percentages, new PercentageSplitStrategy());
    }
    
    /**
     * Update balances between two users
     * lender paid, borrower owes
     */
    private void updateBalance(String lender, String borrower, double amount) {
        Map<String, Double> lenderBalances = balances.get(lender);
        Map<String, Double> borrowerBalances = balances.get(borrower);

        double newLenderBal = lenderBalances.getOrDefault(borrower, 0.0) + amount;
        double newBorrowerBal = borrowerBalances.getOrDefault(lender, 0.0) - amount;

        if (Math.abs(newLenderBal) < 0.01) {
            lenderBalances.remove(borrower);
            borrowerBalances.remove(lender);
        } else {
            lenderBalances.put(borrower, newLenderBal);
            borrowerBalances.put(lender, newBorrowerBal);
        }
    }
    
    /**
     * Settle up debt between two users
     */
    public void settleUp(String payer, String receiver, double amount) 
            throws UserNotFoundException, InvalidSettlementException {
        if (!users.containsKey(payer)) throw new UserNotFoundException(payer);
        if (!users.containsKey(receiver)) throw new UserNotFoundException(receiver);
        if (amount <= 0) throw new InvalidSettlementException("Settlement amount must be positive");
        
        // payer owes receiver, so payer's balance for receiver is negative
        double currentOwed = balances.get(payer).getOrDefault(receiver, 0.0);
        double debt = -currentOwed;  // how much payer owes receiver (positive)
        
        if (debt < 0.01) throw new InvalidSettlementException("No debt to settle");
        if (amount - debt > 0.01) throw new InvalidSettlementException("Settlement amount exceeds owed debt");
        
        // Reduce debt: receiver is lender, payer is borrower, negative amount to reduce
        updateBalance(receiver, payer, -amount;
    }
    
    /**
     * Get balance for a specific user
     */
    public Map<String, Double> getUserBalance(String userId) throws UserNotFoundException {
        if (!users.containsKey(userId)) throw new UserNotFoundException(userId);
        return new HashMap<>(balances.get(userId));
    }
    
    /**
     * Get all balances in the system
     */
    public List<String> getBalanceSheet() {
        List<String> sheet = new ArrayList<>();
        for (Map.Entry<String, Map<String, Double>> userEntry : balances.entrySet()) {
            String userId = userEntry.getKey();
            String userName = users.get(userId).getName();
            for (Map.Entry<String, Double> balEntry : userEntry.getValue().entrySet()) {
                double amount = balEntry.getValue();
                if (Math.abs(amount) < 0.01) continue;
                String otherName = users.get(balEntry.getKey()).getName();
                if (amount < 0) {
                    // userId owes otherUser
                    sheet.add(userName + " owes " + otherName + ": $" + String.format("%.2f", Math.abs(amount)));
                }
            }
        }
        if (sheet.isEmpty()) sheet.add("No balances");
        return sheet;
    }
    
    /**
     * Get all expenses involving a user
     */
    public List<Expense> getUserExpenses(String userId) throws UserNotFoundException {
        if (!users.containsKey(userId)) throw new UserNotFoundException(userId);
        return expenses.values().stream()
            .filter(e -> e.getPaidBy().equals(userId) || e.getSplits().containsKey(userId))
            .collect(Collectors.toList());
    }
    
    /**
     * Get total amount user has lent (positive balances = others owe them)
     */
    public double getTotalSpent(String userId) throws UserNotFoundException {
        if (!users.containsKey(userId)) throw new UserNotFoundException(userId);
        return balances.get(userId).values().stream()
            .filter(v -> v > 0)
            .mapToDouble(Double::doubleValue)
            .sum();
    }
    
    /**
     * Get total amount user owes to others (absolute of negative balances)
     */
    public double getTotalOwed(String userId) throws UserNotFoundException {
        if (!users.containsKey(userId)) throw new UserNotFoundException(userId);
        return Math.abs(balances.get(userId).values().stream()
            .filter(v -> v < 0)
            .mapToDouble(Double::doubleValue)
            .sum());
    }
    
    /**
     * Get net balance for user
     */
    public double getNetBalance(String userId) throws UserNotFoundException {
        if (!users.containsKey(userId)) throw new UserNotFoundException(userId);
        return balances.get(userId).values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();
    }
    
    /**
     * Get expense by ID
     */
    public Expense getExpense(String expenseId) throws ExpenseNotFoundException {
        Expense expense = expenses.get(expenseId);
        if (expense == null) throw new ExpenseNotFoundException(expenseId);
        return expense;
    }
    
    /**
     * BONUS: Simplify debts - minimize number of transactions
     */
    public List<String> simplifyDebts() {
        // Calculate net balance for each user
        Map<String, Double> netBalances = new HashMap<>();
        for (Map.Entry<String, Map<String, Double>> entry : balances.entrySet()) {
            double net = entry.getValue().values().stream().mapToDouble(Double::doubleValue).sum();
            if (Math.abs(net) > 0.01) {
                netBalances.put(entry.getKey(), net);
            }
        }
        
        // Separate creditors (positive) and debtors (negative)
        List<Map.Entry<String, Double>> creditors = netBalances.entrySet().stream()
            .filter(e -> e.getValue() > 0.01)
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .collect(Collectors.toList());
        
        List<Map.Entry<String, Double>> debtors = netBalances.entrySet().stream()
            .filter(e -> e.getValue() < -0.01)
            .sorted(Comparator.comparingDouble(e -> e.getValue()))
            .collect(Collectors.toList());
        
        List<String> transactions = new ArrayList<>();
        int i = 0, j = 0;
        while (i < debtors.size() && j < creditors.size()) {
            double debt = Math.abs(debtors.get(i).getValue());
            double credit = creditors.get(j).getValue();
            double settle = Math.min(debt, credit);
            
            String debtorName = users.get(debtors.get(i).getKey()).getName();
            String creditorName = users.get(creditors.get(j).getKey()).getName();
            transactions.add(debtorName + " pays " + creditorName + ": $" + String.format("%.2f", settle));
            
            debtors.get(i).setValue(debtors.get(i).getValue() + settle);
            creditors.get(j).setValue(creditors.get(j).getValue() - settle);
            
            if (Math.abs(debtors.get(i).getValue()) < 0.01) i++;
            if (Math.abs(creditors.get(j).getValue()) < 0.01) j++;
        }
        
        if (transactions.isEmpty()) transactions.add("All settled up!");
        return transactions;
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
                System.out.println("  " + balance);
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
                System.out.println("  " + balance);
            }
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 7: Settle Up
        System.out.println("=== Test Case 7: Settle Up Payment ===");
        try {
            splitwise.settleUp("U2", "U1", 50.0);
            System.out.println("✓ U2 paid U1: $50.00");
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
                String otherName = entry.getKey();
                String relation = amount > 0 ? "owes Alice" : "is owed by Alice";
                System.out.println("  " + otherName + " " + relation + ": $" + 
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
        
        // Test Case 10: Simplify Debts
        System.out.println("=== Test Case 10: Simplify Debts ===");
        List<String> simplified = splitwise.simplifyDebts();
        for (String txn : simplified) {
            System.out.println("  " + txn);
        }
        System.out.println();
        
        // Test Case 11: Exception - Duplicate User
        System.out.println("=== Test Case 11: Exception - Duplicate User ===");
        try {
            splitwise.addUser("U1", "Alice Clone", "alice2@example.com");
            System.out.println("✗ Should have thrown UserAlreadyExistsException");
        } catch (UserAlreadyExistsException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 12: Exception - User Not Found
        System.out.println("=== Test Case 12: Exception - User Not Found ===");
        try {
            splitwise.addEqualExpense("Test", 100.0, "U999", Arrays.asList("U1", "U2"));
            System.out.println("✗ Should have thrown UserNotFoundException");
        } catch (UserNotFoundException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 13: Exception - Invalid Expense
        System.out.println("=== Test Case 13: Exception - Invalid Expense Amount ===");
        try {
            splitwise.addEqualExpense("Invalid", -100.0, "U1", Arrays.asList("U1", "U2"));
            System.out.println("✗ Should have thrown InvalidExpenseException");
        } catch (InvalidExpenseException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 14: Get Expense
        System.out.println("=== Test Case 14: Get Expense ===");
        try {
            Expense exp = splitwise.getExpense("EXP_1");
            System.out.println("✓ Expense: " + exp.getDescription() + " ($" + String.format("%.2f", exp.getTotalAmount()) + ")");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 15: Get User Expenses
        System.out.println("=== Test Case 15: User Expenses ===");
        try {
            List<Expense> userExpenses = splitwise.getUserExpenses("U1");
            System.out.println("Alice's expenses: " + userExpenses.size());
            for (Expense e : userExpenses) {
                System.out.println("  " + e.getDescription() + " ($" + String.format("%.2f", e.getTotalAmount()) + ", " + e.getSplitType() + ")");
            }
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 16: Strategy Pattern - Direct addExpense with pluggable strategy
        System.out.println("=== Test Case 16: STRATEGY PATTERN - Direct addExpense ===");
        try {
            // Use EqualSplitStrategy directly via addExpense
            SplitStrategy equalStrategy = new EqualSplitStrategy();
            String expId1 = splitwise.addExpense(
                "Cab ride (Equal Strategy)", 120.0, "U1",
                Arrays.asList("U1", "U2", "U3"), null, equalStrategy
            );
            System.out.println("✓ Equal strategy expense: " + expId1);
            
            // Use ExactSplitStrategy directly via addExpense
            SplitStrategy exactStrategy = new ExactSplitStrategy();
            Map<String, Double> exactDetails = new HashMap<>();
            exactDetails.put("U1", 30.0);
            exactDetails.put("U2", 70.0);
            String expId2 = splitwise.addExpense(
                "Snacks (Exact Strategy)", 100.0, "U1",
                new ArrayList<>(exactDetails.keySet()), exactDetails, exactStrategy
            );
            System.out.println("✓ Exact strategy expense: " + expId2);
            
            // Use PercentageSplitStrategy directly via addExpense
            SplitStrategy pctStrategy = new PercentageSplitStrategy();
            Map<String, Double> pctDetails = new HashMap<>();
            pctDetails.put("U1", 40.0);
            pctDetails.put("U2", 60.0);
            String expId3 = splitwise.addExpense(
                "Gifts (Percentage Strategy)", 200.0, "U2",
                new ArrayList<>(pctDetails.keySet()), pctDetails, pctStrategy
            );
            System.out.println("✓ Percentage strategy expense: " + expId3);
            
            System.out.println("✓ All strategies work via unified addExpense() method!");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}
