import java.util.*;

// ===== EXCEPTIONS (keep only what matters) =====

class InvalidCardException extends Exception {
    public InvalidCardException(String msg) { super(msg); }
}

class InvalidPINException extends Exception {
    private int attemptsRemaining;
    public InvalidPINException(int attemptsRemaining) {
        super("Invalid PIN. Attempts remaining: " + attemptsRemaining);
        this.attemptsRemaining = attemptsRemaining;
    }
}

class CardBlockedException extends Exception {
    public CardBlockedException() { super("Card blocked: too many wrong PIN attempts"); }
}

class InsufficientBalanceException extends Exception {
    public InsufficientBalanceException(double requested, double available) {
        super(String.format("Insufficient: need $%.2f, have $%.2f", requested, available));
    }
}

class ATMCashException extends Exception {
    public ATMCashException(double amount) {
        super(String.format("ATM cannot dispense $%.0f", amount));
    }
}

class InvalidAmountException extends Exception {
    public InvalidAmountException(String msg) { super(msg); }
}

// ===== ENUMS =====

/**
 * ATM States (State Pattern)
 * IDLE → CARD_INSERTED → AUTHENTICATED → IDLE
 */
enum ATMState {
    IDLE, CARD_INSERTED, AUTHENTICATED, OUT_OF_SERVICE
}

/**
 * Cash denominations - ordered largest first for greedy algorithm
 */
enum Denomination {
    HUNDRED(100), FIFTY(50), TWENTY(20), TEN(10), FIVE(5);
    final int value;
    Denomination(int value) { this.value = value; }
}

// ===== DOMAIN CLASSES =====

/**
 * Bank Card
 * 
 * KEY INTERVIEW POINTS:
 * - Card number masked in display/logs (security)
 * - PIN hashed in production (bcrypt/PBKDF2)
 * - Blocked after N failed attempts
 */
class Card {
    String cardNumber, holderName, pin, accountId;
    boolean blocked;
    int failedAttempts;
    static final int MAX_ATTEMPTS = 3;

    public Card(String cardNumber, String holderName, String pin, String accountId) {
        this.cardNumber = cardNumber;
        this.holderName = holderName;
        this.pin = pin;
        this.accountId = accountId;
    }

    public String masked() {
        return "****" + cardNumber.substring(cardNumber.length() - 4);
    }
}

/**
 * Bank Account
 * 
 * KEY INTERVIEW POINTS:
 * - Separate from Card (1 account → many cards)
 * - Balance updates must be atomic (concurrency)
 * - Daily withdrawal limit tracking
 */
class Account {
    String id, holderName;
    double balance;
    double dailyLimit, withdrawnToday;

    public Account(String id, String holderName, double balance, double dailyLimit) {
        this.id = id;
        this.holderName = holderName;
        this.balance = balance;
        this.dailyLimit = dailyLimit;
    }
}

// ===== ATM SERVICE =====

/**
 * ATM System - Low Level Design (LLD)
 * 
 * PROBLEM: Design an ATM that handles card auth, balance inquiry,
 * cash withdrawal with denomination dispensing, and edge cases.
 * 
 * STATE TRANSITIONS:
 *   IDLE → insertCard() → CARD_INSERTED
 *   CARD_INSERTED → enterPIN() → AUTHENTICATED
 *   CARD_INSERTED → wrongPIN×3 → IDLE (card blocked)
 *   AUTHENTICATED → withdraw()/checkBalance() → AUTHENTICATED
 *   AUTHENTICATED → ejectCard() → IDLE
 * 
 * KEY PATTERNS:
 * - State Pattern: ATM states
 * - Greedy Algorithm: Denomination dispensing
 * - Chain of Responsibility: Alternative for denomination dispensing
 * 
 * KEY INTERVIEW TOPICS:
 * - Greedy vs DP for denomination ($175 = $100+$50+$20+$5)
 * - PIN security (hashing, HSM, constant-time compare)
 * - Concurrency (optimistic locking for balance updates)
 * - Atomicity (withdraw = debit account + dispense cash — both or neither)
 */
class ATMService {
    private ATMState state;
    private Map<String, Card> cards;        // cardNumber → Card
    private Map<String, Account> accounts;  // accountId → Account
    private Map<Denomination, Integer> cashInventory;
    private double totalCash;

    // Current session
    private Card currentCard;
    private Account currentAccount;

    public ATMService() {
        this.state = ATMState.IDLE;
        this.cards = new HashMap<>();
        this.accounts = new HashMap<>();
        this.cashInventory = new EnumMap<>(Denomination.class);
        this.totalCash = 0;
        for (Denomination d : Denomination.values()) cashInventory.put(d, 0);
    }

    // ===== SETUP =====
    public void addCard(Card card) { cards.put(card.cardNumber, card); }
    public void addAccount(Account acc) { accounts.put(acc.id, acc); }
    public void loadCash(Denomination denom, int count) {
        // HINT: cashInventory.merge(denom, count, Integer::sum);
        // HINT: totalCash += denom.value * count;
        // HINT: if (state == ATMState.OUT_OF_SERVICE && totalCash > 0) state = ATMState.IDLE;
        cashInventory.put(denom, count+cashInventory.getOrDefault(denom, 0));
        totalCash+=denom.value*count;
        if(state == ATMState.OUT_OF_SERVICE && totalCash>0) state=ATMState.IDLE;
    }

    // ===== CORE OPERATIONS =====

    /**
     * Insert card into ATM
     * 
     * HINTS:
     * 1. Check state == IDLE
     * 2. Look up card, check exists & not blocked
     * 3. Set currentCard, transition to CARD_INSERTED
     */
    public void insertCard(String cardNumber) throws InvalidCardException, CardBlockedException {
        // HINT: if (state != ATMState.IDLE)
        //     throw new InvalidCardException("ATM busy, state: " + state);
        // HINT: Card card = cards.get(cardNumber);
        // HINT: if (card == null) throw new InvalidCardException("Card not found");
        // HINT: if (card.blocked) throw new CardBlockedException();
        // HINT: currentCard = card;
        // HINT: state = ATMState.CARD_INSERTED;
        // HINT: System.out.println("  Card accepted: " + card.masked());
        if(state!=ATMState.IDLE) throw new InvalidCardException("ATM Busy "+state);
        Card card=cards.get(cardNumber);
        if(card==null) throw new InvalidCardException(cardNumber+" is not present");
        if(card.blocked) throw new CardBlockedException();
        currentCard=card;
        state=ATMState.CARD_INSERTED;
        System.out.println("  Card accepted: " + card.masked());
    }

    /**
     * Authenticate with PIN
     * 
     * HINTS:
     * 1. Check state == CARD_INSERTED
     * 2. If PIN wrong → increment failedAttempts
     *    - If failedAttempts >= MAX → block card, eject, throw CardBlockedException
     *    - Else throw InvalidPINException(remaining)
     * 3. If PIN correct → reset attempts, lookup account, transition to AUTHENTICATED
     * 
     * INTERVIEW: Discuss constant-time comparison, HSM, never log PINs
     */
    public void enterPIN(String pin) throws InvalidPINException, CardBlockedException {
        // HINT: if (!currentCard.pin.equals(pin)) {
        //     currentCard.failedAttempts++;
        //     int remaining = Card.MAX_ATTEMPTS - currentCard.failedAttempts;
        //     if (remaining <= 0) {
        //         currentCard.blocked = true;
        //         ejectCard();
        //         throw new CardBlockedException();
        //     }
        //     throw new InvalidPINException(remaining);
        // }
        // HINT: currentCard.failedAttempts = 0;
        // HINT: currentAccount = accounts.get(currentCard.accountId);
        // HINT: state = ATMState.AUTHENTICATED;
        // HINT: System.out.println("  Welcome, " + currentCard.holderName + "!");
        if(!currentCard.pin.equals(pin)){
            currentCard.failedAttempts++;
            int remaining=Card.MAX_ATTEMPTS-currentCard.failedAttempts;
            if(remaining<=0){
                currentCard.blocked=true;
                ejectCard();
                throw new CardBlockedException();
            }
            throw new InvalidPINException(remaining);
        }
        currentCard.failedAttempts=0;
        currentAccount=accounts.get(currentCard.accountId);
        state=ATMState.AUTHENTICATED;
        System.out.println("  Welcome, " + currentCard.holderName + "!");
    }

    /**
     * Check balance
     */
    public double checkBalance() {
        // HINT: return currentAccount.balance;
        return currentAccount.balance;
    }

    /**
     * Withdraw cash — THE KEY METHOD
     * 
     * HINTS:
     * 1. Validate: amount > 0, multiple of $5
     * 2. Check daily withdrawal limit
     * 3. Check account balance >= amount
     * 4. Calculate denominations (greedy algorithm)
     * 5. If can't make exact amount → throw ATMCashException
     * 6. Deduct from account, update ATM inventory
     * 7. Return denomination breakdown
     * 
     * INTERVIEW DISCUSSION:
     * - Greedy works for canonical denominations (100,50,20,10,5)
     * - For arbitrary denoms, need DP: dp[i] = min notes for amount i
     * - Chain of Responsibility: $100Handler → $50Handler → ...
     * - Atomicity: debit + dispense must both succeed or rollback
     */
    public Map<Denomination, Integer> withdraw(double amount)
            throws InvalidAmountException, InsufficientBalanceException, ATMCashException {
        // HINT: if (amount <= 0) throw new InvalidAmountException("Amount must be positive");
        // HINT: if (amount % 5 != 0) throw new InvalidAmountException("Must be multiple of $5");
        //
        // HINT: // Daily limit check
        // HINT: if (currentAccount.withdrawnToday + amount > currentAccount.dailyLimit)
        //     throw new InvalidAmountException(String.format(
        //         "Exceeds daily limit: $%.0f limit, $%.0f already withdrawn",
        //         currentAccount.dailyLimit, currentAccount.withdrawnToday));
        //
        // HINT: // Balance check
        // HINT: if (amount > currentAccount.balance)
        //     throw new InsufficientBalanceException(amount, currentAccount.balance);
        //
        // HINT: // Denomination calculation (GREEDY)
        // HINT: Map<Denomination, Integer> dispensed = calculateDenominations((int) amount);
        // HINT: if (dispensed == null) throw new ATMCashException(amount);
        //
        // HINT: // Commit: deduct balance + update ATM inventory
        // HINT: currentAccount.balance -= amount;
        // HINT: currentAccount.withdrawnToday += amount;
        // HINT: dispensed.forEach((d, c) -> {
        //     cashInventory.merge(d, -c, Integer::sum);
        //     totalCash -= d.value * c;
        // });
        //
        // HINT: return dispensed;
        if(amount<=0) throw new InvalidAmountException("Amount must be positive");
        if(amount%5!=0) throw new InvalidAmountException("amount must be multiple of 5");
        if(currentAccount.withdrawnToday+amount>currentAccount.dailyLimit) throw new InvalidAmountException("limit exceed");
        if(amount>currentAccount.balance) throw new InsufficientBalanceException(amount,currentAccount.balance);
        Map<Denomination,Integer> result=calculateDenominations((int)amount);
        if(result==null) throw new ATMCashException(amount);
        currentAccount.balance-=amount;
        currentAccount.withdrawnToday+=amount;
        result.forEach((d,c)->{
            cashInventory.put(d,cashInventory.getOrDefault(d, 0)-c);
            totalCash-=(d.value*c);
        });
        return result;
    }

    /**
     * GREEDY denomination algorithm — MOST ASKED IN INTERVIEWS
     * 
     * Algorithm:
     *   For each denomination (largest → smallest):
     *     notesNeeded = remaining / denom.value
     *     notesAvailable = cashInventory.get(denom)
     *     use = min(needed, available)
     *     remaining -= use * denom.value
     *   If remaining == 0 → success, else → can't dispense
     * 
     * TIME: O(number of denominations) = O(1) since fixed
     * 
     * WHY GREEDY WORKS HERE:
     *   Standard denominations are "canonical" — greedy always optimal
     *   Counter-example: denoms [1, 15, 25], amount 30
     *     Greedy: 25+1+1+1+1+1 = 6 notes ✗
     *     DP: 15+15 = 2 notes ✓
     */
    private Map<Denomination, Integer> calculateDenominations(int amount) {
        // HINT: Map<Denomination, Integer> result = new EnumMap<>(Denomination.class);
        // HINT: int remaining = amount;
        // HINT: for (Denomination d : Denomination.values()) {  // already largest-first
        //     int needed = remaining / d.value;
        //     int available = cashInventory.getOrDefault(d, 0);
        //     int use = Math.min(needed, available);
        //     if (use > 0) {
        //         result.put(d, use);
        //         remaining -= use * d.value;
        //     }
        // }
        // HINT: return remaining == 0 ? result : null;
        Map<Denomination,Integer> result=new HashMap<>();
        int remaining=amount;
        for(Denomination d:Denomination.values()){
            int needed=remaining/d.value;
            int available=cashInventory.getOrDefault(d, 0);
            int use=Math.min(available, needed);
            if(use>0){
                result.put(d, use);
                remaining-=(use*d.value);
            }
        }
        return remaining==0?result:null;
    }

    /**
     * Eject card and end session
     */
    public void ejectCard() {
        if (currentCard != null)
            System.out.println("  Card ejected. Goodbye, " + currentCard.holderName + "!");
        currentCard = null;
        currentAccount = null;
        state = ATMState.IDLE;
    }

    // ===== GETTERS =====
    public ATMState getState() { return state; }
    public double getTotalCash() { return totalCash; }

    public void displayStatus() {
        System.out.println("\n--- ATM Status ---");
        System.out.println("State: " + state + " | Cash: $" + String.format("%.0f", totalCash));
        cashInventory.forEach((d, c) ->
            System.out.println(String.format("  $%d × %d = $%d", d.value, c, d.value * c)));
    }
}

// ===== MAIN TEST CLASS =====

public class ATMSystem {
    public static void main(String[] args) {
        System.out.println("=== ATM System Test Cases ===\n");

        // Setup
        ATMService atm = new ATMService();
        atm.addAccount(new Account("ACC1", "Alice", 5000, 2000));
        atm.addAccount(new Account("ACC2", "Bob", 1500, 1000));
        atm.addAccount(new Account("ACC3", "Charlie", 100, 500));

        atm.addCard(new Card("4111111111111111", "Alice", "1234", "ACC1"));
        atm.addCard(new Card("4222222222222222", "Bob", "5678", "ACC2"));
        atm.addCard(new Card("4333333333333333", "Charlie", "9999", "ACC3"));

        atm.loadCash(Denomination.HUNDRED, 20);   // $2000
        atm.loadCash(Denomination.FIFTY, 30);      // $1500
        atm.loadCash(Denomination.TWENTY, 50);     // $1000
        atm.loadCash(Denomination.TEN, 40);        // $400
        atm.loadCash(Denomination.FIVE, 20);       // $100
        System.out.println("ATM loaded: $" + String.format("%.0f", atm.getTotalCash()));
        System.out.println();

        // Test 1: Successful Withdrawal
        System.out.println("=== Test 1: Withdraw $250 ===");
        try {
            atm.insertCard("4111111111111111");
            atm.enterPIN("1234");
            System.out.println("  Balance: $" + String.format("%.2f", atm.checkBalance()));
            Map<Denomination, Integer> notes = atm.withdraw(250);
            System.out.println("✓ Dispensed: " + formatNotes(notes));
            System.out.println("  New balance: $" + String.format("%.2f", atm.checkBalance()));
            atm.ejectCard();
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
            atm.ejectCard();
        }
        System.out.println();

        // Test 2: Denomination Breakdown ($175 = $100+$50+$20+$5)
        System.out.println("=== Test 2: Withdraw $175 (denomination test) ===");
        try {
            atm.insertCard("4111111111111111");
            atm.enterPIN("1234");
            Map<Denomination, Integer> notes = atm.withdraw(175);
            System.out.println("✓ Dispensed: " + formatNotes(notes));
            atm.ejectCard();
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
            atm.ejectCard();
        }
        System.out.println();

        // Test 3: Wrong PIN (3 attempts → blocked)
        System.out.println("=== Test 3: Wrong PIN → Card Blocked ===");
        try {
            atm.insertCard("4333333333333333");
            try { atm.enterPIN("0000"); } catch (InvalidPINException e) {
                System.out.println("  Attempt 1: " + e.getMessage());
            }
            try { atm.enterPIN("1111"); } catch (InvalidPINException e) {
                System.out.println("  Attempt 2: " + e.getMessage());
            }
            try { atm.enterPIN("2222"); } catch (CardBlockedException e) {
                System.out.println("✓ " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("  " + e.getMessage());
        }
        // Verify blocked card rejected
        try {
            atm.insertCard("4333333333333333");
            System.out.println("✗ Should have thrown");
        } catch (CardBlockedException e) {
            System.out.println("✓ Blocked card rejected");
        } catch (Exception e) {
            System.out.println("  " + e.getMessage());
        }
        System.out.println();

        // Test 4: Insufficient Balance
        System.out.println("=== Test 4: Insufficient Balance ===");
        // Charlie has $100, try to withdraw $500
        Card charlie2 = new Card("4444444444444444", "Charlie", "8888", "ACC3");
        atm.addCard(charlie2);
        try {
            atm.insertCard("4444444444444444");
            atm.enterPIN("8888");
            atm.withdraw(500);
            System.out.println("✗ Should have thrown");
        } catch (InsufficientBalanceException e) {
            System.out.println("✓ " + e.getMessage());
            atm.ejectCard();
        } catch (Exception e) {
            System.out.println("✗ Wrong: " + e.getMessage());
            atm.ejectCard();
        }
        System.out.println();

        // Test 5: Invalid Amount (not multiple of $5)
        System.out.println("=== Test 5: Invalid Amount ($73) ===");
        try {
            atm.insertCard("4111111111111111");
            atm.enterPIN("1234");
            atm.withdraw(73);
            System.out.println("✗ Should have thrown");
        } catch (InvalidAmountException e) {
            System.out.println("✓ " + e.getMessage());
            atm.ejectCard();
        } catch (Exception e) {
            System.out.println("✗ Wrong: " + e.getMessage());
            atm.ejectCard();
        }
        System.out.println();

        // Test 6: Daily Limit Exceeded
        System.out.println("=== Test 6: Daily Limit Exceeded ===");
        try {
            atm.insertCard("4222222222222222");
            atm.enterPIN("5678");
            atm.withdraw(500);
            System.out.println("  First $500 OK");
            atm.withdraw(600);  // Total $1100 > $1000 limit
            System.out.println("✗ Should have thrown");
        } catch (InvalidAmountException e) {
            System.out.println("✓ " + e.getMessage());
            atm.ejectCard();
        } catch (Exception e) {
            System.out.println("✗ Wrong: " + e.getMessage());
            atm.ejectCard();
        }
        System.out.println();

        // Test 7: Invalid Card
        System.out.println("=== Test 7: Invalid Card ===");
        try {
            atm.insertCard("9999999999999999");
            System.out.println("✗ Should have thrown");
        } catch (InvalidCardException e) {
            System.out.println("✓ " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong: " + e.getMessage());
        }
        System.out.println();

        // Final status
        atm.displayStatus();
        System.out.println("\n=== All Tests Complete! ===");
    }

    static String formatNotes(Map<Denomination, Integer> notes) {
        if (notes == null) return "null";
        StringBuilder sb = new StringBuilder();
        notes.forEach((d, c) -> { if (c > 0) sb.append(String.format("$%d×%d ", d.value, c)); });
        return sb.toString().trim();
    }
}

/**
 * ============================================
 * INTERVIEW CHEAT SHEET (discuss, don't code)
 * ============================================
 * 
 * 1. STATE PATTERN:
 *    IDLE → insertCard → CARD_INSERTED → enterPIN → AUTHENTICATED → eject → IDLE
 *    Advanced: Each state as a class implementing ATMStateHandler interface
 * 
 * 2. DENOMINATION DISPENSING:
 *    Greedy: Largest first. O(denoms). Works for canonical sets {100,50,20,10,5}
 *    DP: For arbitrary denoms. dp[i]=min notes for amount i. O(amount×denoms)
 *    Chain of Responsibility: $100Handler→$50Handler→... each handles its part
 * 
 * 3. SECURITY (mention, don't implement):
 *    - PIN: bcrypt hash, HSM, constant-time compare, never log
 *    - Card: EMV chip, mask numbers, session timeout
 *    - Block after N failed attempts
 * 
 * 4. CONCURRENCY (mention):
 *    - Multiple ATMs → same account: optimistic locking (version column)
 *    - CAS: UPDATE accounts SET balance=new WHERE id=? AND balance=old
 *    - Transaction atomicity: debit + dispense both succeed or rollback
 * 
 * 5. EXTENSIONS (if time/asked):
 *    - Deposit: validate notes → add to balance + ATM inventory
 *    - Transfer: atomic debit+credit (saga pattern in microservices)
 *    - Mini-statement: last N transactions from history list
 */
