import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

// ===== CUSTOM EXCEPTION CLASSES =====

class SeatNotFoundException extends Exception {
    public SeatNotFoundException(String msg) { super(msg); }
}

class SeatAlreadyBookedException extends Exception {
    public SeatAlreadyBookedException(String msg) { super(msg); }
}

// ===== ENUMS =====

enum SeatStatus { AVAILABLE, RESERVED, BOOKED }

// ===== SUPPORTING CLASSES =====

interface PricingStrategy {
    double calculate(double basePrice);
}

class StandardPricing implements PricingStrategy {
    @Override
    public double calculate(double price) { return price; }
}

class PremiumPricing implements PricingStrategy {
    @Override
    public double calculate(double price) { return price * 2; }
}

class Seat {
    private final String seatId;
    private SeatStatus status;
    private final double price;
    
    public Seat(double price) {
        this.seatId = "SEAT-" + UUID.randomUUID().toString().substring(0, 8);
        this.status = SeatStatus.AVAILABLE;
        this.price = price;
    }
    
    public synchronized SeatStatus getStatus() { return status; }
    public synchronized void setStatus(SeatStatus s) { this.status = s; }
    public String getSeatId() { return seatId; }
    public double getPrice() { return price; }
}

class MovieBookingSystem {
    private final ConcurrentHashMap<String, Seat> seats;
    private final ConcurrentHashMap<String, Lock> locks;
    private final ScheduledExecutorService scheduler;
    
    public MovieBookingSystem() {
        this.seats = new ConcurrentHashMap<>();
        this.locks = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }
    
    public String addSeat(double price) {
        Seat seat = new Seat(price);
        seats.put(seat.getSeatId(), seat);
        return seat.getSeatId();
    }
    
    /**
     * Reserve a seat temporarily
     * IMPLEMENTATION HINTS:
     * 1. Get seat and validate it exists and is AVAILABLE
     * 2. Get lock for this seat (use locks.computeIfAbsent)
     * 3. Try to acquire lock with tryLock()
     * 4. Double-check seat is still AVAILABLE
     * 5. Set status to RESERVED
     * 6. Schedule auto-expiration using scheduler.schedule()
     * 7. Return true if successful
     */
    public boolean reserveSeat(String seatId, String user, int timeoutSeconds, PricingStrategy pricing) {
        // TODO: Implement
        // HINT: Lock lock = locks.computeIfAbsent(seatId, k -> new ReentrantLock());
        // HINT: if (lock.tryLock(1, TimeUnit.SECONDS)) { ... }
        return false;
    }
    
    /**
     * Confirm booking for reserved seat
     * IMPLEMENTATION HINTS:
     * 1. Validate seat exists and is RESERVED
     * 2. Set status to BOOKED
     * 3. Return true if successful
     */
    public boolean confirmBooking(String seatId, String user) {
        // TODO: Implement
        // HINT: Check seat.getStatus() == SeatStatus.RESERVED
        return false;
    }
    
    public void displaySeats() {
        System.out.println("\n--- Seats ---");
        seats.forEach((id, s) -> System.out.println("  " + id + " → " + s.getStatus() + " ($" + s.getPrice() + ")"));
    }
}

public class SeatBooking {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Seat Booking Test Cases ===\n");
        
        MovieBookingSystem system = new MovieBookingSystem();
        String s1 = system.addSeat(10.0);
        String s2 = system.addSeat(20.0);
        
        system.displaySeats();
        
        // Test: Reserve and confirm
        try {
            boolean reserved = system.reserveSeat(s1, "alice", 5, new PremiumPricing());
            System.out.println("✓ Seat reserved: " + reserved);
            
            boolean confirmed = system.confirmBooking(s1, "alice");
            System.out.println("✓ Booking confirmed: " + confirmed);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        
        system.displaySeats();
        System.out.println("\n✓ Test complete!");
    }
}
