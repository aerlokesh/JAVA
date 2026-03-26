import java.time.*;
import java.util.*;

// ===== EXCEPTIONS =====

class InsufficientBalanceException extends Exception {
    public InsufficientBalanceException(String msg) { super("Insufficient balance: " + msg); }
}

class InvalidPassException extends Exception {
    public InvalidPassException(String id) { super("Invalid toll pass: " + id); }
}

// ===== ENUMS =====

enum VehicleType { CAR, TRUCK, BUS, MOTORCYCLE }

enum PaymentMethod { TOLL_PASS, CASH }

enum TripType { ONE_WAY, ROUND_TRIP }

// ===== DOMAIN CLASSES =====

class Vehicle {
    private final String licensePlate;
    private final VehicleType type;
    private String tollPassId;   // null if no pass
    
    public Vehicle(String licensePlate, VehicleType type) {
        this.licensePlate = licensePlate;
        this.type = type;
    }
    
    public String getLicensePlate() { return licensePlate; }
    public VehicleType getType() { return type; }
    public String getTollPassId() { return tollPassId; }
    public void setTollPassId(String id) { this.tollPassId = id; }
    public boolean hasTollPass() { return tollPassId != null; }
    
    @Override
    public String toString() { return licensePlate + "[" + type + (hasTollPass() ? ", pass=" + tollPassId : ", no pass") + "]"; }
}

class TollPass {
    private final String passId;
    private final String ownerId;
    private double balance;
    private boolean active;
    private final LocalDateTime issuedAt;
    
    public TollPass(String passId, String ownerId, double initialBalance) {
        this.passId = passId;
        this.ownerId = ownerId;
        this.balance = initialBalance;
        this.active = true;
        this.issuedAt = LocalDateTime.now();
    }
    
    public String getPassId() { return passId; }
    public String getOwnerId() { return ownerId; }
    public double getBalance() { return balance; }
    public boolean isActive() { return active; }
    
    public void setActive(boolean a) { this.active = a; }
    public void deduct(double amount) { this.balance -= amount; }
    public void recharge(double amount) { this.balance += amount; }
    
    @Override
    public String toString() { return passId + "[owner=" + ownerId + ", balance=$" + String.format("%.2f", balance) + ", active=" + active + "]"; }
}

class TollBooth {
    private final String boothId;
    private final String plazaId;
    private boolean isOpen;
    private boolean isFastTrack;  // E-ZPass only lane
    
    public TollBooth(String boothId, String plazaId, boolean isFastTrack) {
        this.boothId = boothId;
        this.plazaId = plazaId;
        this.isOpen = true;
        this.isFastTrack = isFastTrack;
    }
    
    public String getBoothId() { return boothId; }
    public String getPlazaId() { return plazaId; }
    public boolean isOpen() { return isOpen; }
    public boolean isFastTrack() { return isFastTrack; }
    public void setOpen(boolean o) { this.isOpen = o; }
    
    @Override
    public String toString() { return boothId + "[" + (isFastTrack ? "FAST" : "CASH") + ", " + (isOpen ? "OPEN" : "CLOSED") + "]"; }
}

class TollTransaction {
    private final String transactionId;
    private final String vehiclePlate;
    private final String boothId;
    private final double amount;
    private final PaymentMethod paymentMethod;
    private final LocalDateTime timestamp;
    
    public TollTransaction(String vehiclePlate, String boothId, double amount, PaymentMethod method) {
        this.transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 6);
        this.vehiclePlate = vehiclePlate;
        this.boothId = boothId;
        this.amount = amount;
        this.paymentMethod = method;
        this.timestamp = LocalDateTime.now();
    }
    
    public String getTransactionId() { return transactionId; }
    public String getVehiclePlate() { return vehiclePlate; }
    public String getBoothId() { return boothId; }
    public double getAmount() { return amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public LocalDateTime getTimestamp() { return timestamp; }
    
    @Override
    public String toString() { return transactionId + "[" + vehiclePlate + " at " + boothId + ", $" + String.format("%.2f", amount) + ", " + paymentMethod + "]"; }
}

// ===== INTERFACE (Strategy Pattern) =====

/**
 * Pricing strategy — different toll amounts based on vehicle type, time, etc.
 */
interface TollPricingStrategy {
    double calculateToll(VehicleType type, TripType trip);
}

/**
 * Standard pricing: fixed rate by vehicle type
 */
class StandardPricingStrategy implements TollPricingStrategy {
    private final Map<VehicleType, Double> rates;
    
    public StandardPricingStrategy() {
        rates = new HashMap<>();
        rates.put(VehicleType.MOTORCYCLE, 1.50);
        rates.put(VehicleType.CAR, 3.00);
        rates.put(VehicleType.BUS, 5.00);
        rates.put(VehicleType.TRUCK, 7.00);
    }
    
    @Override
    public double calculateToll(VehicleType type, TripType trip) {
        double base = rates.getOrDefault(type, 3.00);
        return trip == TripType.ROUND_TRIP ? base * 1.8 : base; // 10% discount for round trip
    }
}

// ===== SERVICE =====

/**
 * Highway Toll Collection System - Low Level Design (LLD)
 * 
 * PROBLEM (Amazon SDE Question): Design a highway toll collection system that:
 * 1. Register vehicles and issue toll passes (like E-ZPass/FASTag)
 * 2. Process toll payments (pass auto-deduct or cash)
 * 3. Different pricing by vehicle type
 * 4. Recharge toll passes
 * 5. Track transaction history
 * 6. Manage toll booths (open/close, fast-track vs cash)
 * 
 * PATTERNS: Strategy (pricing)
 */
class TollCollectionService {
    private final Map<String, Vehicle> vehicles;           // licensePlate → Vehicle
    private final Map<String, TollPass> tollPasses;        // passId → TollPass
    private final Map<String, TollBooth> booths;           // boothId → Booth
    private final List<TollTransaction> transactions;
    private TollPricingStrategy pricingStrategy;
    private double totalRevenue;
    
    public TollCollectionService(TollPricingStrategy pricingStrategy) {
        this.vehicles = new HashMap<>();
        this.tollPasses = new HashMap<>();
        this.booths = new HashMap<>();
        this.transactions = new ArrayList<>();
        this.pricingStrategy = pricingStrategy;
        this.totalRevenue = 0;
    }
    
    /**
     * Register a vehicle
     */
    public Vehicle registerVehicle(String plate, VehicleType type) {
        // TODO: Implement
        // HINT: Vehicle v = new Vehicle(plate, type);
        // HINT: vehicles.put(plate, v);
        // HINT: System.out.println("  ✓ Vehicle: " + v);
        // HINT: return v;
        return null;
    }
    
    /**
     * Issue a toll pass to a vehicle
     * 
     * IMPLEMENTATION HINTS:
     * 1. Create TollPass with initial balance
     * 2. Store in tollPasses map
     * 3. Link pass to vehicle
     * 4. Return pass
     */
    public TollPass issueTollPass(String plate, double initialBalance) {
        // TODO: Implement
        // HINT: Vehicle v = vehicles.get(plate);
        // HINT: if (v == null) return null;
        // HINT: String passId = "PASS-" + UUID.randomUUID().toString().substring(0, 6);
        // HINT: TollPass pass = new TollPass(passId, plate, initialBalance);
        // HINT: tollPasses.put(passId, pass);
        // HINT: v.setTollPassId(passId);
        // HINT: System.out.println("  ✓ Pass issued: " + pass);
        // HINT: return pass;
        return null;
    }
    
    /**
     * Add a toll booth
     */
    public TollBooth addBooth(String boothId, String plazaId, boolean fastTrack) {
        // TODO: Implement
        // HINT: TollBooth booth = new TollBooth(boothId, plazaId, fastTrack);
        // HINT: booths.put(boothId, booth);
        // HINT: return booth;
        return null;
    }
    
    /**
     * Process toll payment — the main flow
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get vehicle → validate exists
     * 2. Calculate toll using pricingStrategy
     * 3. If vehicle has toll pass:
     *    a. Get pass → validate active
     *    b. Check balance >= toll → throw InsufficientBalanceException if not
     *    c. Deduct from pass
     *    d. Create transaction with TOLL_PASS method
     * 4. If no pass:
     *    a. Create transaction with CASH method
     * 5. Add to transactions, update totalRevenue
     * 6. Return transaction
     */
    public TollTransaction processToll(String plate, String boothId, TripType tripType)
            throws InsufficientBalanceException, InvalidPassException {
        // TODO: Implement
        // HINT: Vehicle v = vehicles.get(plate);
        // HINT: if (v == null) return null;
        //
        // HINT: double toll = pricingStrategy.calculateToll(v.getType(), tripType);
        //
        // HINT: if (v.hasTollPass()) {
        //     TollPass pass = tollPasses.get(v.getTollPassId());
        //     if (pass == null || !pass.isActive()) throw new InvalidPassException(v.getTollPassId());
        //     if (pass.getBalance() < toll) throw new InsufficientBalanceException(
        //         "Need $" + String.format("%.2f", toll) + ", have $" + String.format("%.2f", pass.getBalance()));
        //     pass.deduct(toll);
        //     TollTransaction txn = new TollTransaction(plate, boothId, toll, PaymentMethod.TOLL_PASS);
        //     transactions.add(txn);
        //     totalRevenue += toll;
        //     System.out.println("  💳 " + txn + " (balance: $" + String.format("%.2f", pass.getBalance()) + ")");
        //     return txn;
        // } else {
        //     TollTransaction txn = new TollTransaction(plate, boothId, toll, PaymentMethod.CASH);
        //     transactions.add(txn);
        //     totalRevenue += toll;
        //     System.out.println("  💵 " + txn);
        //     return txn;
        // }
        return null;
    }
    
    /**
     * Recharge a toll pass
     */
    public void rechargeTollPass(String passId, double amount) throws InvalidPassException {
        // TODO: Implement
        // HINT: TollPass pass = tollPasses.get(passId);
        // HINT: if (pass == null) throw new InvalidPassException(passId);
        // HINT: pass.recharge(amount);
        // HINT: System.out.println("  🔋 Recharged " + passId + " +$" + String.format("%.2f", amount) + " → $" + String.format("%.2f", pass.getBalance()));
    }
    
    // ===== QUERIES =====
    
    /**
     * Get transaction history for a vehicle
     */
    public List<TollTransaction> getVehicleHistory(String plate) {
        // TODO: Implement
        // HINT: List<TollTransaction> result = new ArrayList<>();
        // HINT: for (TollTransaction t : transactions) {
        //     if (t.getVehiclePlate().equals(plate)) result.add(t);
        // }
        // HINT: return result;
        return null;
    }
    
    public Vehicle getVehicle(String plate) { return vehicles.get(plate); }
    public TollPass getTollPass(String id) { return tollPasses.get(id); }
    public double getTotalRevenue() { return totalRevenue; }
    public int getTotalTransactions() { return transactions.size(); }
}

// ===== MAIN TEST CLASS =====

public class TollCollectionSystem {
    public static void main(String[] args) {
        System.out.println("=== Highway Toll Collection System LLD ===\n");
        
        TollCollectionService service = new TollCollectionService(new StandardPricingStrategy());
        
        // Setup: booths
        service.addBooth("B1", "Plaza-North", false);  // cash lane
        service.addBooth("B2", "Plaza-North", true);   // fast track
        
        // Test 1: Register vehicles and issue passes
        System.out.println("=== Test 1: Register Vehicles ===");
        service.registerVehicle("CAR-001", VehicleType.CAR);
        service.registerVehicle("TRUCK-001", VehicleType.TRUCK);
        service.registerVehicle("BIKE-001", VehicleType.MOTORCYCLE);
        service.registerVehicle("BUS-001", VehicleType.BUS);
        service.registerVehicle("CAR-002", VehicleType.CAR);  // no pass
        System.out.println();
        
        // Test 2: Issue toll passes
        System.out.println("=== Test 2: Issue Toll Passes ===");
        TollPass carPass = service.issueTollPass("CAR-001", 50.00);
        TollPass truckPass = service.issueTollPass("TRUCK-001", 100.00);
        service.issueTollPass("BIKE-001", 20.00);
        System.out.println();
        
        // Test 3: Process toll with pass (auto-deduct)
        System.out.println("=== Test 3: Toll with Pass ===");
        try {
            service.processToll("CAR-001", "B2", TripType.ONE_WAY);  // $3.00
            if (carPass != null) System.out.println("  Balance: $" + String.format("%.2f", carPass.getBalance()) + " (expect $47.00)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 4: Process toll with cash (no pass)
        System.out.println("=== Test 4: Toll with Cash ===");
        try {
            service.processToll("CAR-002", "B1", TripType.ONE_WAY);  // $3.00 cash
            System.out.println("✓ Cash payment processed");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 5: Round trip pricing (10% discount)
        System.out.println("=== Test 5: Round Trip ===");
        try {
            service.processToll("TRUCK-001", "B2", TripType.ROUND_TRIP);  // $7 × 1.8 = $12.60
            if (truckPass != null) System.out.println("  Truck balance: $" + String.format("%.2f", truckPass.getBalance()));
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 6: Multiple transactions
        System.out.println("=== Test 6: Multiple Tolls ===");
        try {
            service.processToll("CAR-001", "B2", TripType.ONE_WAY);
            service.processToll("CAR-001", "B2", TripType.ONE_WAY);
            service.processToll("BIKE-001", "B2", TripType.ONE_WAY); // $1.50
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 7: Recharge pass
        System.out.println("=== Test 7: Recharge Pass ===");
        try {
            if (carPass != null) {
                service.rechargeTollPass(carPass.getPassId(), 25.00);
                System.out.println("✓ New balance: $" + String.format("%.2f", carPass.getBalance()));
            }
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 8: Transaction history
        System.out.println("=== Test 8: Vehicle History ===");
        List<TollTransaction> history = service.getVehicleHistory("CAR-001");
        System.out.println("✓ CAR-001 transactions: " + (history != null ? history.size() : 0));
        if (history != null) history.forEach(t -> System.out.println("    " + t));
        System.out.println();
        
        // Test 9: Exception — insufficient balance
        System.out.println("=== Test 9: Insufficient Balance ===");
        try {
            // Drain balance first
            TollPass bikePass = service.getTollPass(service.getVehicle("BIKE-001").getTollPassId());
            if (bikePass != null) {
                // Keep processing until insufficient
                for (int i = 0; i < 20; i++) {
                    service.processToll("BIKE-001", "B2", TripType.ONE_WAY);
                }
            }
            System.out.println("✗ Should have thrown");
        } catch (InsufficientBalanceException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 10: Revenue
        System.out.println("=== Test 10: Revenue ===");
        System.out.println("✓ Total revenue: $" + String.format("%.2f", service.getTotalRevenue()));
        System.out.println("  Total transactions: " + service.getTotalTransactions());
        
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION:
 * =====================
 * 
 * 1. KEY ENTITIES:
 *    Vehicle: plate, type, optional toll pass
 *    TollPass: balance, auto-deduct (like E-ZPass/FASTag)
 *    TollBooth: cash lane vs fast-track (pass only)
 *    Transaction: record of each toll payment
 * 
 * 2. PAYMENT FLOW:
 *    Vehicle enters toll → scan pass via RFID
 *    If pass: verify active + sufficient balance → deduct → open gate
 *    If no pass: pay cash at booth → open gate
 *    Record transaction either way
 * 
 * 3. PRICING STRATEGY:
 *    By vehicle type: motorcycle < car < bus < truck
 *    By trip type: round trip gets discount
 *    Could add: time-based (peak/off-peak), distance-based
 * 
 * 4. SCALABILITY:
 *    RFID scanning: < 100ms per vehicle
 *    Pre-paid passes: no real-time bank transaction needed
 *    Async: transaction recording can be async (eventual consistency)
 *    Low balance alerts: notify before balance hits zero
 * 
 * 5. FOLLOW-UPS:
 *    - Violation detection (pass through without paying)
 *    - Camera-based license plate recognition
 *    - Dynamic pricing (congestion-based)
 *    - Multi-plaza pass (works across highways)
 *    - Auto-recharge when balance low
 * 
 * 6. REAL-WORLD: E-ZPass (US), FASTag (India), M-Tag (Europe)
 * 
 * 7. API:
 *    POST /vehicles                    — register
 *    POST /toll-passes                 — issue pass
 *    POST /toll-passes/{id}/recharge   — recharge
 *    POST /tolls/process               — process toll payment
 *    GET  /vehicles/{plate}/history    — transaction history
 */
