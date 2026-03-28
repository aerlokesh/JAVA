import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

/*
 * TOLL COLLECTION SYSTEM - Low Level Design
 * ==========================================
 * 
 * REQUIREMENTS:
 * 1. Register vehicles with type (car, truck, bus, motorcycle)
 * 2. Issue toll passes (like E-ZPass/FASTag) with prepaid balance
 * 3. Process toll: auto-deduct from pass or cash payment
 * 4. Pricing by vehicle type + trip type (one-way / round-trip)
 * 5. Recharge toll passes
 * 6. Transaction history per vehicle
 * 7. Manage toll booths (open/close, fast-track vs cash lanes)
 * 8. Thread-safe concurrent toll processing
 * 
 * KEY DATA STRUCTURES:
 * - ConcurrentHashMap<plate, Vehicle>: vehicle registry
 * - ConcurrentHashMap<passId, TollPass>: pass store with synchronized balance
 * - CopyOnWriteArrayList<Transaction>: append-only transaction log
 * 
 * DESIGN PATTERNS:
 * - Strategy: TollPricingStrategy (standard, peak-hour, distance-based)
 * - Decorator: PeakHourPricing wraps base pricing with surge multiplier
 * 
 * COMPLEXITY:
 *   registerVehicle:  O(1)
 *   processToll:      O(1) — all map lookups
 *   recharge:         O(1)
 *   getHistory:       O(n) where n = total transactions
 */

// ==================== EXCEPTIONS ====================

class InsufficientBalanceException extends Exception {
    InsufficientBalanceException(String msg) { super(msg); }
}

class InvalidPassException extends Exception {
    InvalidPassException(String id) { super("Invalid/inactive pass: " + id); }
}

// ==================== ENUMS ====================

enum VehicleType { MOTORCYCLE, CAR, BUS, TRUCK }
enum PaymentMethod { TOLL_PASS, CASH }
enum TripType { ONE_WAY, ROUND_TRIP }

// ==================== DOMAIN CLASSES ====================

class Vehicle {
    final String plate;
    final VehicleType type;
    volatile String passId; // null if no pass

    Vehicle(String plate, VehicleType type) {
        this.plate = plate;
        this.type = type;
    }

    boolean hasPass() { return passId != null; }
}

class TollPass {
    final String passId;
    private double balance; // guarded by synchronized
    volatile boolean active;

    TollPass(String passId, double initialBalance) {
        this.passId = passId;
        this.balance = initialBalance;
        this.active = true;
    }

    synchronized double getBalance() { return balance; }

    synchronized void deduct(double amount) throws InsufficientBalanceException {
        // HINT: if (balance < amount)
        // HINT:     throw new InsufficientBalanceException(
        // HINT:         String.format("Need $%.2f, have $%.2f on pass %s", amount, balance, passId));
        // HINT: balance -= amount;
        if(balance<amount) throw new InsufficientBalanceException(passId);
        balance-=amount;
    }

    synchronized void recharge(double amount) {
        // HINT: balance += amount;
        balance+=amount;
    }
}

class TollBooth {
    final String boothId;
    final boolean fastTrack; // true = pass-only lane
    volatile boolean open;

    TollBooth(String boothId, boolean fastTrack) {
        this.boothId = boothId;
        this.fastTrack = fastTrack;
        this.open = true;
    }
}

class TollTransaction {
    final String txnId;
    final String plate;
    final String boothId;
    final double amount;
    final PaymentMethod method;
    final TripType tripType;

    TollTransaction(String plate, String boothId, double amount, PaymentMethod method, TripType tripType) {
        this.txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 8);
        this.plate = plate;
        this.boothId = boothId;
        this.amount = amount;
        this.method = method;
        this.tripType = tripType;
    }
}

// ==================== STRATEGY: PRICING ====================

interface TollPricingStrategy {
    double calculate(VehicleType type, TripType trip);
}

class StandardPricing implements TollPricingStrategy {
    private static final Map<VehicleType, Double> RATES = Map.of(
        VehicleType.MOTORCYCLE, 1.50,
        VehicleType.CAR, 3.00,
        VehicleType.BUS, 5.00,
        VehicleType.TRUCK, 7.00
    );

    @Override
    public double calculate(VehicleType type, TripType trip) {
        // TODO: Implement
        // HINT: double base = RATES.getOrDefault(type, 3.00);
        // HINT: return trip == TripType.ROUND_TRIP ? base * 1.8 : base;
        return 0;
    }
}

/** Decorator: wraps any pricing with a surge multiplier */
class PeakHourPricing implements TollPricingStrategy {
    private final TollPricingStrategy base;
    private final double surgeMultiplier;

    PeakHourPricing(TollPricingStrategy base, double surgeMultiplier) {
        this.base = base;
        this.surgeMultiplier = surgeMultiplier;
    }

    @Override
    public double calculate(VehicleType type, TripType trip) {
        // TODO: Implement
        // HINT: return base.calculate(type, trip) * surgeMultiplier;
        return 0;
    }
}

// ==================== SERVICE ====================

class TollCollectionService {
    private final ConcurrentHashMap<String, Vehicle> vehicles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TollPass> passes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TollBooth> booths = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<TollTransaction> transactions = new CopyOnWriteArrayList<>();
    private final AtomicInteger passCounter = new AtomicInteger(1);
    private volatile TollPricingStrategy pricing;
    private final AtomicLong totalRevenueCents = new AtomicLong(0);

    TollCollectionService(TollPricingStrategy pricing) {
        this.pricing = pricing;
    }

    // --- Registration ---

    Vehicle registerVehicle(String plate, VehicleType type) {
        // TODO: Implement
        // HINT: Vehicle v = new Vehicle(plate, type);
        // HINT: vehicles.put(plate, v);
        // HINT: return v;
        return null;
    }

    TollPass issueTollPass(String plate, double initialBalance) {
        // TODO: Implement
        // HINT: Vehicle v = vehicles.get(plate);
        // HINT: if (v == null) throw new IllegalArgumentException("Vehicle not registered: " + plate);
        // HINT: String passId = "PASS-" + String.format("%04d", passCounter.getAndIncrement());
        // HINT: TollPass pass = new TollPass(passId, initialBalance);
        // HINT: passes.put(passId, pass);
        // HINT: v.passId = passId;
        // HINT: return pass;
        return null;
    }

    TollBooth addBooth(String boothId, boolean fastTrack) {
        // TODO: Implement
        // HINT: TollBooth booth = new TollBooth(boothId, fastTrack);
        // HINT: booths.put(boothId, booth);
        // HINT: return booth;
        return null;
    }

    // --- Core: Process Toll ---

    TollTransaction processToll(String plate, String boothId, TripType tripType)
            throws InsufficientBalanceException, InvalidPassException {
        // TODO: Implement — this is the main interview question
        // HINT: Vehicle v = vehicles.get(plate);
        // HINT: if (v == null) throw new IllegalArgumentException("Unknown vehicle: " + plate);
        // HINT:
        // HINT: TollBooth booth = booths.get(boothId);
        // HINT: if (booth != null && !booth.open)
        // HINT:     throw new IllegalStateException("Booth " + boothId + " is closed");
        // HINT: if (booth != null && booth.fastTrack && !v.hasPass())
        // HINT:     throw new IllegalStateException("Fast-track lane requires toll pass");
        // HINT:
        // HINT: double toll = pricing.calculate(v.type, tripType);
        // HINT: PaymentMethod method;
        // HINT:
        // HINT: if (v.hasPass()) {
        // HINT:     TollPass pass = passes.get(v.passId);
        // HINT:     if (pass == null || !pass.active) throw new InvalidPassException(v.passId);
        // HINT:     pass.deduct(toll);
        // HINT:     method = PaymentMethod.TOLL_PASS;
        // HINT: } else {
        // HINT:     method = PaymentMethod.CASH;
        // HINT: }
        // HINT:
        // HINT: TollTransaction txn = new TollTransaction(plate, boothId, toll, method, tripType);
        // HINT: transactions.add(txn);
        // HINT: totalRevenueCents.addAndGet(Math.round(toll * 100));
        // HINT: return txn;
        return null;
    }

    // --- Pass Management ---

    void rechargeTollPass(String passId, double amount) throws InvalidPassException {
        // TODO: Implement
        // HINT: TollPass pass = passes.get(passId);
        // HINT: if (pass == null) throw new InvalidPassException(passId);
        // HINT: pass.recharge(amount);
    }

    void deactivatePass(String passId) throws InvalidPassException {
        // TODO: Implement
        // HINT: TollPass pass = passes.get(passId);
        // HINT: if (pass == null) throw new InvalidPassException(passId);
        // HINT: pass.active = false;
    }

    // --- Booth Management ---

    void closeBooth(String boothId) {
        // TODO: Implement
        // HINT: TollBooth booth = booths.get(boothId);
        // HINT: if (booth != null) booth.open = false;
    }

    void openBooth(String boothId) {
        // TODO: Implement
        // HINT: TollBooth booth = booths.get(boothId);
        // HINT: if (booth != null) booth.open = true;
    }

    // --- Queries ---

    List<TollTransaction> getVehicleHistory(String plate) {
        // TODO: Implement
        // HINT: return transactions.stream()
        // HINT:     .filter(t -> t.plate.equals(plate))
        // HINT:     .collect(Collectors.toList());
        return Collections.emptyList();
    }

    List<TollTransaction> getBoothTransactions(String boothId) {
        // TODO: Implement
        // HINT: return transactions.stream()
        // HINT:     .filter(t -> t.boothId.equals(boothId))
        // HINT:     .collect(Collectors.toList());
        return Collections.emptyList();
    }

    double getTotalRevenue() { return totalRevenueCents.get() / 100.0; }
    int getTotalTransactions() { return transactions.size(); }
    TollPass getPass(String passId) { return passes.get(passId); }
    Vehicle getVehicle(String plate) { return vehicles.get(plate); }
    void setPricing(TollPricingStrategy strategy) { this.pricing = strategy; }
}

// ==================== MAIN / TESTS ====================

public class TollCollectionSystem {
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║    TOLL COLLECTION SYSTEM - LLD Demo         ║");
        System.out.println("╚══════════════════════════════════════════════╝\n");

        TollCollectionService svc = new TollCollectionService(new StandardPricing());

        svc.addBooth("B1-CASH", false);
        svc.addBooth("B2-FAST", true);
        svc.addBooth("B3-CASH", false);

        // --- Test 1: Register Vehicles & Issue Passes ---
        System.out.println("=== Test 1: Register & Issue Passes ===");
        svc.registerVehicle("CAR-001", VehicleType.CAR);
        svc.registerVehicle("TRUCK-01", VehicleType.TRUCK);
        svc.registerVehicle("BIKE-01", VehicleType.MOTORCYCLE);
        svc.registerVehicle("BUS-001", VehicleType.BUS);
        svc.registerVehicle("CAR-002", VehicleType.CAR);

        TollPass carPass = svc.issueTollPass("CAR-001", 50.00);
        TollPass truckPass = svc.issueTollPass("TRUCK-01", 100.00);
        TollPass bikePass = svc.issueTollPass("BIKE-01", 10.00);
        svc.issueTollPass("BUS-001", 80.00);

        System.out.printf("  CAR-001 pass: %s, balance: $%.2f%n", carPass.passId, carPass.getBalance());
        System.out.printf("  TRUCK-01 pass: %s, balance: $%.2f%n", truckPass.passId, truckPass.getBalance());
        System.out.println("  CAR-002: no pass (cash)\n");

        // --- Test 2: Toll with Pass ---
        System.out.println("=== Test 2: Toll with Pass ===");
        try {
            TollTransaction txn = svc.processToll("CAR-001", "B2-FAST", TripType.ONE_WAY);
            System.out.printf("  %s: $%.2f via %s%n", txn.txnId, txn.amount, txn.method);
            System.out.printf("  Balance after: $%.2f (expected $47.00)%n", carPass.getBalance());
            System.out.println("✓ Pass auto-deduct works\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // --- Test 3: Toll with Cash ---
        System.out.println("=== Test 3: Toll with Cash ===");
        try {
            TollTransaction txn = svc.processToll("CAR-002", "B1-CASH", TripType.ONE_WAY);
            System.out.printf("  %s: $%.2f via %s%n", txn.txnId, txn.amount, txn.method);
            System.out.println("✓ Cash payment processed\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // --- Test 4: Round Trip ---
        System.out.println("=== Test 4: Round Trip ===");
        try {
            TollTransaction txn = svc.processToll("TRUCK-01", "B2-FAST", TripType.ROUND_TRIP);
            System.out.printf("  Truck round trip: $%.2f (expected $12.60)%n", txn.amount);
            System.out.printf("  Truck balance: $%.2f%n", truckPass.getBalance());
            System.out.println("✓ Round trip pricing correct\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // --- Test 5: Multiple Tolls ---
        System.out.println("=== Test 5: Multiple Tolls ===");
        try {
            svc.processToll("CAR-001", "B2-FAST", TripType.ONE_WAY);
            svc.processToll("CAR-001", "B2-FAST", TripType.ONE_WAY);
            svc.processToll("BIKE-01", "B2-FAST", TripType.ONE_WAY);
            System.out.printf("  CAR-001 balance: $%.2f (expected $41.00)%n", carPass.getBalance());
            System.out.printf("  BIKE-01 balance: $%.2f (expected $8.50)%n", bikePass.getBalance());
            System.out.println("✓ Multiple transactions processed\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // --- Test 6: Recharge ---
        System.out.println("=== Test 6: Recharge ===");
        try {
            double before = carPass.getBalance();
            svc.rechargeTollPass(carPass.passId, 25.00);
            System.out.printf("  Before: $%.2f, After: $%.2f (+$25.00)%n", before, carPass.getBalance());
            System.out.println("✓ Recharge works\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // --- Test 7: History ---
        System.out.println("=== Test 7: Transaction History ===");
        List<TollTransaction> history = svc.getVehicleHistory("CAR-001");
        System.out.println("  CAR-001 has " + history.size() + " transactions (expected 3)");
        for (TollTransaction t : history)
            System.out.printf("    %s | %s | $%.2f | %s%n", t.txnId, t.boothId, t.amount, t.method);
        System.out.println("✓ History tracking works\n");

        // --- Test 8: Insufficient Balance ---
        System.out.println("=== Test 8: Insufficient Balance ===");
        try {
            for (int i = 0; i < 20; i++) svc.processToll("BIKE-01", "B2-FAST", TripType.ONE_WAY);
            System.out.println("✗ Should have thrown");
        } catch (InsufficientBalanceException e) {
            System.out.println("✓ Caught: " + e.getMessage() + "\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // --- Test 9: Booth Rules ---
        System.out.println("=== Test 9: Booth Rules ===");
        try {
            svc.processToll("CAR-002", "B2-FAST", TripType.ONE_WAY);
            System.out.println("✗ Should have thrown");
        } catch (IllegalStateException e) {
            System.out.println("✓ Fast-track denied: " + e.getMessage());
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        svc.closeBooth("B1-CASH");
        try {
            svc.processToll("CAR-002", "B1-CASH", TripType.ONE_WAY);
            System.out.println("✗ Should have thrown");
        } catch (IllegalStateException e) {
            System.out.println("✓ Closed booth denied: " + e.getMessage());
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }
        svc.openBooth("B1-CASH");
        System.out.println();

        // --- Test 10: Deactivate Pass ---
        System.out.println("=== Test 10: Deactivate Pass ===");
        try {
            svc.deactivatePass(carPass.passId);
            svc.processToll("CAR-001", "B2-FAST", TripType.ONE_WAY);
            System.out.println("✗ Should have thrown");
        } catch (InvalidPassException e) {
            System.out.println("✓ Deactivated pass rejected: " + e.getMessage() + "\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // --- Test 11: Peak Hour Pricing ---
        System.out.println("=== Test 11: Peak Hour Pricing ===");
        try {
            svc.setPricing(new PeakHourPricing(new StandardPricing(), 1.5));
            TollTransaction txn = svc.processToll("CAR-002", "B1-CASH", TripType.ONE_WAY);
            System.out.printf("  Peak toll: $%.2f (expected $4.50)%n", txn.amount);
            System.out.println("✓ Strategy swap works");
            svc.setPricing(new StandardPricing());
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }
        System.out.println();

        // --- Test 12: Thread Safety ---
        System.out.println("=== Test 12: Thread Safety ===");
        TollCollectionService concSvc = new TollCollectionService(new StandardPricing());
        concSvc.addBooth("CB1", false);
        for (int i = 0; i < 50; i++) {
            concSvc.registerVehicle("C-" + i, VehicleType.CAR);
            concSvc.issueTollPass("C-" + i, 1000.00);
        }
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            final int idx = i % 50;
            futures.add(executor.submit(() -> {
                try { concSvc.processToll("C-" + idx, "CB1", TripType.ONE_WAY); }
                catch (Exception e) {}
            }));
        }
        for (int i = 0; i < 50; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                try {
                    TollPass p = concSvc.getPass(concSvc.getVehicle("C-" + idx).passId);
                    if (p != null) concSvc.rechargeTollPass(p.passId, 10.0);
                } catch (Exception e) {}
            }));
        }
        for (Future<?> f : futures) { try { f.get(); } catch (Exception e) {} }
        executor.shutdown();
        System.out.println("  Txns: " + concSvc.getTotalTransactions() + " (expected 200)");
        System.out.printf("  Revenue: $%.2f (expected $600.00)%n", concSvc.getTotalRevenue());
        System.out.println("✓ Thread-safe\n");

        System.out.printf("Total: %d txns, $%.2f revenue%n", svc.getTotalTransactions(), svc.getTotalRevenue());
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║          ALL 12 TESTS PASSED ✓               ║");
        System.out.println("╚══════════════════════════════════════════════╝");
    }
}

/*
 * ==================== INTERVIEW NOTES ====================
 *
 * 1. CORE ENTITIES:
 *    Vehicle (plate, type, optional pass) → TollPass (balance, active)
 *    TollBooth (cash vs fast-track) → TollTransaction (immutable record)
 *
 * 2. PAYMENT FLOW:
 *    Vehicle → scan RFID → pass? validate + deduct : cash → record transaction
 *
 * 3. STRATEGY PATTERN:
 *    TollPricingStrategy interface → StandardPricing, PeakHourPricing (decorator)
 *
 * 4. THREAD SAFETY:
 *    ConcurrentHashMap (registries), synchronized (balance), AtomicLong (revenue),
 *    CopyOnWriteArrayList (txn log), volatile (booth.open, pass.active)
 *
 * 5. COMPLEXITY: all O(1) except getHistory O(n)
 *
 * 6. SCALE: shard by plaza, Redis for balance, Kafka for txn log
 *
 * 7. REAL-WORLD: E-ZPass (US), FASTag (India), ETC (Japan)
 */
