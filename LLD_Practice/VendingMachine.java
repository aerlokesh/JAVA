import java.util.*;

// ===== CUSTOM EXCEPTION CLASSES =====

class InsufficientFundsException extends Exception {
    private double required, inserted;
    public InsufficientFundsException(double required, double inserted) {
        super(String.format("Insufficient funds: need $%.2f, inserted $%.2f", required, inserted));
        this.required = required; this.inserted = inserted;
    }
    public double getRequired() { return required; }
    public double getInserted() { return inserted; }
}

class ItemNotAvailableException extends Exception {
    private String itemCode;
    public ItemNotAvailableException(String itemCode) {
        super("Item not available: " + itemCode);
        this.itemCode = itemCode;
    }
    public String getItemCode() { return itemCode; }
}

class InvalidItemException extends Exception {
    public InvalidItemException(String message) { super(message); }
}

class InvalidCoinException extends Exception {
    private double amount;
    public InvalidCoinException(double amount) {
        super("Invalid coin/bill: $" + String.format("%.2f", amount));
        this.amount = amount;
    }
    public double getAmount() { return amount; }
}

class MachineNotReadyException extends Exception {
    private String state;
    public MachineNotReadyException(String state) {
        super("Machine not ready. Current state: " + state);
        this.state = state;
    }
    public String getState() { return state; }
}

// ===== ENUMS =====

/**
 * Vending machine states (State Pattern)
 * 
 * INTERVIEW DISCUSSION:
 * - Why State pattern? (Clean transitions, no messy if-else chains)
 * - State transitions: IDLE → HAS_MONEY → DISPENSING → IDLE
 */
enum MachineState {
    IDLE,        // Waiting for money
    HAS_MONEY,   // Money inserted, waiting for selection
    DISPENSING,   // Item being dispensed
    OUT_OF_SERVICE // Machine broken or empty
}

/**
 * Accepted coin/bill denominations
 */
enum Coin {
    NICKEL(0.05),
    DIME(0.10),
    QUARTER(0.25),
    HALF_DOLLAR(0.50),
    DOLLAR(1.00),
    FIVE_DOLLAR(5.00);

    final double value;
    Coin(double value) { this.value = value; }
}

// ===== DOMAIN CLASSES =====

/**
 * Represents a product in the vending machine
 */
class Product {
    String code;      // e.g., "A1", "B2"
    String name;
    double price;
    int quantity;

    public Product(String code, String name, double price, int quantity) {
        this.code = code;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    public boolean isAvailable() { return quantity > 0; }

    @Override
    public String toString() {
        return String.format("[%s] %s - $%.2f (qty: %d)", code, name, price, quantity);
    }
}

/**
 * Represents result of a vending transaction
 */
class DispenseResult {
    Product product;
    double change;
    List<Coin> changeCoins;

    public DispenseResult(Product product, double change, List<Coin> changeCoins) {
        this.product = product;
        this.change = change;
        this.changeCoins = changeCoins;
    }

    @Override
    public String toString() {
        return String.format("Dispensed: %s | Change: $%.2f %s", 
            product.name, change, changeCoins.isEmpty() ? "" : changeCoins);
    }
}

// ===== MAIN VENDING MACHINE CLASS =====

/**
 * Vending Machine - Low Level Design (LLD)
 * 
 * PROBLEM STATEMENT:
 * Design a vending machine that can:
 * 1. Display available products with prices
 * 2. Accept coins/bills and track inserted amount
 * 3. Dispense selected product if sufficient funds
 * 4. Return correct change using available denominations
 * 5. Handle edge cases: out of stock, insufficient funds, cancel
 * 6. Use State pattern for clean state management
 * 
 * STATE TRANSITIONS:
 *   IDLE → (insert money) → HAS_MONEY
 *   HAS_MONEY → (select item) → DISPENSING → IDLE
 *   HAS_MONEY → (cancel) → IDLE (return money)
 *   HAS_MONEY → (insert more) → HAS_MONEY
 * 
 * REQUIREMENTS:
 * - Functional: Insert money, select item, dispense, return change, cancel
 * - Non-Functional: Thread-safe, accurate money handling, state consistency
 * 
 * INTERVIEW HINTS:
 * - Discuss State pattern (avoid complex if-else chains)
 * - Talk about change-making algorithm (greedy approach)
 * - Mention floating point precision (use cents/integers in production)
 * - Consider inventory management and restocking
 * - Discuss concurrent access (multiple button presses)
 */
class VendingMachineService {
    private Map<String, Product> inventory;  // code -> Product
    private MachineState state;
    private double insertedAmount;
    private List<Coin> insertedCoins;         // Track coins for refund
    private Map<Coin, Integer> coinInventory;  // Coins available for change
    private List<String> transactionLog;

    public VendingMachineService() {
        this.inventory = new LinkedHashMap<>();  // Preserve insertion order
        this.state = MachineState.IDLE;
        this.insertedAmount = 0.0;
        this.insertedCoins = new ArrayList<>();
        this.coinInventory = new EnumMap<>(Coin.class);
        this.transactionLog = new ArrayList<>();
        // Initialize coin inventory
        for (Coin c : Coin.values()) coinInventory.put(c, 10);
    }

    /**
     * Add/restock a product
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate product code, name, price > 0, quantity >= 0
     * 2. If product exists, add to existing quantity
     * 3. If new product, create and add to inventory
     * 
     * @param code Product code (e.g., "A1")
     * @param name Product name
     * @param price Price
     * @param quantity Quantity to add
     * @throws InvalidItemException if parameters invalid
     */
    public void addProduct(String code, String name, double price, int quantity) throws InvalidItemException {
        // TODO: Implement
        // HINT: if (code == null || code.isEmpty()) throw new InvalidItemException("Code required");
        // HINT: if (price <= 0) throw new InvalidItemException("Price must be > 0");
        // HINT: if (inventory.containsKey(code)) {
        //     inventory.get(code).quantity += quantity;
        // } else {
        //     inventory.put(code, new Product(code, name, price, quantity));
        // }
    }

    /**
     * Display all products
     * 
     * @return List of product display strings
     */
    public List<String> displayProducts() {
        // TODO: Implement
        // HINT: return inventory.values().stream()
        //     .map(Product::toString)
        //     .collect(java.util.stream.Collectors.toList());
        return new ArrayList<>();
    }

    /**
     * Insert a coin/bill
     * 
     * IMPLEMENTATION HINTS:
     * 1. Check machine is not OUT_OF_SERVICE
     * 2. Add coin value to insertedAmount
     * 3. Track the coin in insertedCoins list
     * 4. Transition state to HAS_MONEY
     * 5. Add coin to coinInventory (for making change later)
     * 
     * @param coin The coin/bill inserted
     * @throws MachineNotReadyException if machine out of service
     */
    public void insertCoin(Coin coin) throws MachineNotReadyException {
        // TODO: Implement
        // HINT: if (state == MachineState.OUT_OF_SERVICE)
        //     throw new MachineNotReadyException(state.name());
        // HINT: insertedAmount += coin.value;
        // HINT: insertedCoins.add(coin);
        // HINT: coinInventory.merge(coin, 1, Integer::sum);
        // HINT: state = MachineState.HAS_MONEY;
        // HINT: System.out.println("  Inserted: $" + String.format("%.2f", coin.value) + 
        //     " | Total: $" + String.format("%.2f", insertedAmount));
    }

    /**
     * Select and dispense a product
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate state is HAS_MONEY
     * 2. Validate product exists and is in stock
     * 3. Check insertedAmount >= product.price
     * 4. Calculate change needed
     * 5. Calculate change coins (greedy algorithm)
     * 6. Decrement product quantity
     * 7. Reset insertedAmount and state to IDLE
     * 8. Log the transaction
     * 9. Return DispenseResult
     * 
     * INTERVIEW DISCUSSION:
     * - What if exact change can't be made? (Return all money, don't dispense)
     * - Greedy coin change works for standard denominations
     * - For arbitrary denominations, need DP approach
     * 
     * @param itemCode Product code to select
     * @return DispenseResult with product and change
     * @throws MachineNotReadyException if no money inserted
     * @throws ItemNotAvailableException if product not found or out of stock
     * @throws InsufficientFundsException if not enough money
     */
    public DispenseResult selectItem(String itemCode) 
            throws MachineNotReadyException, ItemNotAvailableException, InsufficientFundsException {
        // TODO: Implement
        // HINT: if (state != MachineState.HAS_MONEY)
        //     throw new MachineNotReadyException(state.name());
        // HINT: Product product = inventory.get(itemCode);
        // HINT: if (product == null || !product.isAvailable())
        //     throw new ItemNotAvailableException(itemCode);
        // HINT: if (insertedAmount < product.price - 0.001)
        //     throw new InsufficientFundsException(product.price, insertedAmount);
        // 
        // HINT: double changeNeeded = insertedAmount - product.price;
        // HINT: List<Coin> changeCoins = calculateChange(changeNeeded);
        // HINT: product.quantity--;
        // HINT: state = MachineState.DISPENSING;
        // 
        // HINT: DispenseResult result = new DispenseResult(product, changeNeeded, changeCoins);
        // HINT: transactionLog.add("SOLD: " + product.name + " | Change: $" + String.format("%.2f", changeNeeded));
        // 
        // HINT: // Reset
        // HINT: insertedAmount = 0;
        // HINT: insertedCoins.clear();
        // HINT: state = MachineState.IDLE;
        // HINT: return result;
        return null;
    }

    /**
     * Cancel transaction and return all inserted money
     * 
     * IMPLEMENTATION HINTS:
     * 1. Store current insertedAmount for return
     * 2. Reset insertedAmount to 0
     * 3. Clear insertedCoins
     * 4. Set state back to IDLE
     * 5. Return the refunded amount
     * 
     * @return Amount refunded
     */
    public double cancelAndRefund() {
        // TODO: Implement
        // HINT: double refund = insertedAmount;
        // HINT: // Remove inserted coins from coin inventory (give them back)
        // HINT: for (Coin c : insertedCoins) coinInventory.merge(c, -1, Integer::sum);
        // HINT: insertedAmount = 0;
        // HINT: insertedCoins.clear();
        // HINT: state = MachineState.IDLE;
        // HINT: return refund;
        return 0.0;
    }

    /**
     * Calculate change coins using greedy algorithm
     * 
     * IMPLEMENTATION HINTS:
     * 1. Sort coins by value (highest first)
     * 2. For each denomination:
     *    - While remaining >= coin.value AND coinInventory has this coin
     *    - Add coin to result, subtract from remaining
     *    - Decrement coinInventory for this coin
     * 3. If remaining > 0.001 after all coins, change can't be made exactly
     * 
     * TIME COMPLEXITY: O(denominations × change/smallest_coin)
     * 
     * @param amount Change amount needed
     * @return List of coins for change
     */
    private List<Coin> calculateChange(double amount) {
        // TODO: Implement
        // HINT: List<Coin> change = new ArrayList<>();
        // HINT: double remaining = amount;
        // HINT: Coin[] coins = Coin.values();
        // HINT: // Sort descending by value
        // HINT: Arrays.sort(coins, (a, b) -> Double.compare(b.value, a.value));
        // HINT: for (Coin coin : coins) {
        //     while (remaining >= coin.value - 0.001 && coinInventory.getOrDefault(coin, 0) > 0) {
        //         change.add(coin);
        //         remaining -= coin.value;
        //         coinInventory.merge(coin, -1, Integer::sum);
        //     }
        // }
        // HINT: return change;
        return new ArrayList<>();
    }

    /**
     * Get current inserted amount
     */
    public double getInsertedAmount() { return insertedAmount; }

    /**
     * Get current machine state
     */
    public MachineState getState() { return state; }

    /**
     * Get product by code
     */
    public Product getProduct(String code) { return inventory.get(code); }

    /**
     * Get transaction log
     */
    public List<String> getTransactionLog() { return new ArrayList<>(transactionLog); }

    /**
     * Display machine status
     */
    public void displayStatus() {
        System.out.println("\n--- Vending Machine Status ---");
        System.out.println("State: " + state);
        System.out.println("Inserted: $" + String.format("%.2f", insertedAmount));
        System.out.println("Products:");
        inventory.values().forEach(p -> System.out.println("  " + p));
        System.out.println("Transactions: " + transactionLog.size());
    }
}

// ===== MAIN TEST CLASS =====

public class VendingMachine {
    public static void main(String[] args) {
        System.out.println("=== Vending Machine Test Cases ===\n");

        VendingMachineService vm = new VendingMachineService();

        // Test Case 1: Stock Products
        System.out.println("=== Test Case 1: Stock Products ===");
        try {
            vm.addProduct("A1", "Cola", 1.50, 5);
            vm.addProduct("A2", "Pepsi", 1.50, 3);
            vm.addProduct("B1", "Chips", 1.00, 10);
            vm.addProduct("B2", "Candy", 0.75, 8);
            vm.addProduct("C1", "Water", 1.25, 7);
            System.out.println("✓ Stocked 5 products");
            vm.displayProducts().forEach(p -> System.out.println("  " + p));
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();

        // Test Case 2: Basic Purchase
        System.out.println("=== Test Case 2: Basic Purchase (Cola $1.50) ===");
        try {
            vm.insertCoin(Coin.DOLLAR);
            vm.insertCoin(Coin.QUARTER);
            vm.insertCoin(Coin.QUARTER);
            DispenseResult result = vm.selectItem("A1");
            System.out.println("✓ " + result);
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();

        // Test Case 3: Purchase with Change
        System.out.println("=== Test Case 3: Purchase with Change (Candy $0.75) ===");
        try {
            vm.insertCoin(Coin.DOLLAR);
            DispenseResult result = vm.selectItem("B2");
            System.out.println("✓ " + result);
            System.out.println("  Change returned: $" + String.format("%.2f", result.change));
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();

        // Test Case 4: Multiple Coins
        System.out.println("=== Test Case 4: Multiple Coins (Water $1.25) ===");
        try {
            vm.insertCoin(Coin.QUARTER);
            vm.insertCoin(Coin.QUARTER);
            vm.insertCoin(Coin.QUARTER);
            vm.insertCoin(Coin.QUARTER);
            vm.insertCoin(Coin.QUARTER);
            DispenseResult result = vm.selectItem("C1");
            System.out.println("✓ " + result);
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();

        // Test Case 5: Cancel and Refund
        System.out.println("=== Test Case 5: Cancel and Refund ===");
        try {
            vm.insertCoin(Coin.DOLLAR);
            vm.insertCoin(Coin.QUARTER);
            System.out.println("  Inserted: $" + String.format("%.2f", vm.getInsertedAmount()));
            double refund = vm.cancelAndRefund();
            System.out.println("✓ Refunded: $" + String.format("%.2f", refund));
            System.out.println("  State: " + vm.getState());
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();

        // Test Case 6: Display Products
        System.out.println("=== Test Case 6: Display After Purchases ===");
        vm.displayProducts().forEach(p -> System.out.println("  " + p));
        System.out.println();

        // Test Case 7: Restock
        System.out.println("=== Test Case 7: Restock ===");
        try {
            Product cola = vm.getProduct("A1");
            System.out.println("Cola before restock: qty=" + (cola != null ? cola.quantity : "N/A"));
            vm.addProduct("A1", "Cola", 1.50, 10);
            System.out.println("Cola after restock: qty=" + (cola != null ? cola.quantity : "N/A"));
            System.out.println("✓ Restocked");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();

        // ===== EXCEPTION TEST CASES =====

        // Test Case 8: Exception - Insufficient Funds
        System.out.println("=== Test Case 8: Exception - Insufficient Funds ===");
        try {
            vm.insertCoin(Coin.QUARTER);
            vm.selectItem("A1");  // Cola costs $1.50
            System.out.println("✗ Should have thrown InsufficientFundsException");
        } catch (InsufficientFundsException e) {
            System.out.println("✓ Caught: " + e.getMessage());
            vm.cancelAndRefund();  // Cleanup
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
            vm.cancelAndRefund();
        }
        System.out.println();

        // Test Case 9: Exception - Item Not Available
        System.out.println("=== Test Case 9: Exception - Item Not Available ===");
        try {
            vm.insertCoin(Coin.DOLLAR);
            vm.selectItem("Z9");  // Doesn't exist
            System.out.println("✗ Should have thrown ItemNotAvailableException");
        } catch (ItemNotAvailableException e) {
            System.out.println("✓ Caught: " + e.getMessage());
            vm.cancelAndRefund();
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
            vm.cancelAndRefund();
        }
        System.out.println();

        // Test Case 10: Exception - No Money Inserted
        System.out.println("=== Test Case 10: Exception - No Money Inserted ===");
        try {
            vm.selectItem("A1");
            System.out.println("✗ Should have thrown MachineNotReadyException");
        } catch (MachineNotReadyException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();

        // Test Case 11: Exception - Invalid Product
        System.out.println("=== Test Case 11: Exception - Invalid Product ===");
        try {
            vm.addProduct("", "NoCode", 1.0, 1);
            System.out.println("✗ Should have thrown InvalidItemException");
        } catch (InvalidItemException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();

        // Test Case 12: Out of Stock
        System.out.println("=== Test Case 12: Out of Stock ===");
        try {
            vm.addProduct("D1", "Rare Item", 0.25, 1);  // Only 1 in stock
            vm.insertCoin(Coin.QUARTER);
            vm.selectItem("D1");
            System.out.println("✓ First purchase OK");

            vm.insertCoin(Coin.QUARTER);
            vm.selectItem("D1");  // Should fail - out of stock
            System.out.println("✗ Should have thrown");
        } catch (ItemNotAvailableException e) {
            System.out.println("✓ Caught out of stock: " + e.getMessage());
            vm.cancelAndRefund();
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
            vm.cancelAndRefund();
        }
        System.out.println();

        // Test Case 13: Transaction Log
        System.out.println("=== Test Case 13: Transaction Log ===");
        List<String> log = vm.getTransactionLog();
        System.out.println("Transactions: " + log.size());
        log.forEach(entry -> System.out.println("  " + entry));
        System.out.println();

        vm.displayStatus();
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION TOPICS:
 * ============================
 * 
 * 1. STATE PATTERN:
 *    States: IDLE, HAS_MONEY, DISPENSING, OUT_OF_SERVICE
 *    Transitions:
 *      IDLE → insertCoin() → HAS_MONEY
 *      HAS_MONEY → selectItem() → DISPENSING → IDLE
 *      HAS_MONEY → cancel() → IDLE
 *      ANY → outOfService() → OUT_OF_SERVICE
 *    
 *    Why State Pattern?
 *      - Each state has different valid operations
 *      - Clean separation of concerns
 *      - Easy to add new states
 *      - Avoids complex if-else chains
 * 
 * 2. CHANGE-MAKING ALGORITHM:
 *    Greedy (used here):
 *      - Always pick largest denomination first
 *      - Works for standard US/EU denominations
 *      - O(denominations × amount/smallest)
 *    
 *    Dynamic Programming:
 *      - Needed for arbitrary denominations
 *      - Find minimum coins for exact change
 *      - O(amount × denominations)
 *    
 *    Edge Cases:
 *      - Can't make exact change → return all money
 *      - Coin inventory depleted
 * 
 * 3. MONEY HANDLING:
 *    Floating Point Issues:
 *      - 0.1 + 0.2 != 0.3 in floating point
 *      - Production: use integer cents (150 = $1.50)
 *      - Or use BigDecimal for exact arithmetic
 *    
 *    Coin Inventory:
 *      - Track available coins for making change
 *      - Inserted coins added to inventory
 *      - Change coins removed from inventory
 * 
 * 4. CONCURRENCY:
 *    - Synchronized methods for thread safety
 *    - Prevent simultaneous insert + select
 *    - Lock per machine instance
 * 
 * 5. DESIGN PATTERNS:
 *    State Pattern: Machine states
 *    Strategy Pattern: Different payment methods
 *    Observer Pattern: Notify on low stock
 *    Singleton: Machine controller
 *    Command Pattern: Encapsulate operations (undo/redo)
 * 
 * 6. REAL-WORLD FEATURES:
 *    - Card/NFC payment
 *    - Remote monitoring (IoT)
 *    - Temperature control (beverages)
 *    - Expiry date tracking
 *    - Sales analytics
 *    - Restocking alerts
 * 
 * 7. API DESIGN:
 *    GET  /products                    - Display products
 *    POST /coins                       - Insert coin
 *    POST /select/{code}               - Select item
 *    POST /cancel                      - Cancel and refund
 *    GET  /status                      - Machine status
 *    POST /admin/restock               - Restock products
 */
