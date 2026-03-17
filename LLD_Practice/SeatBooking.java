import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

// ===== CUSTOM EXCEPTION CLASSES =====

/**
 * Exception thrown when seat is not found
 * WHEN TO THROW:
 * - Seat ID doesn't exist in system
 */
class SeatNotFoundException extends Exception {
    private String seatId;
    
    public SeatNotFoundException(String seatId) {
        super("Seat not found: " + seatId);
        this.seatId = seatId;
    }
    
    public String getSeatId() { return seatId; }
}

/**
 * Exception thrown when trying to book already reserved/booked seat
 * WHEN TO THROW:
 * - Seat status is not AVAILABLE
 * - Concurrent booking conflict
 */
class SeatAlreadyBookedException extends Exception {
    private String seatId;
    private SeatStatus currentStatus;
    
    public SeatAlreadyBookedException(String seatId, SeatStatus currentStatus) {
        super("Seat " + seatId + " already " + currentStatus);
        this.seatId = seatId;
        this.currentStatus = currentStatus;
    }
    
    public String getSeatId() { return seatId; }
    public SeatStatus getCurrentStatus() { return currentStatus; }
}

// ===== ENUMS =====

enum SeatStatus { 
    AVAILABLE,  // Seat is free to reserve
    RESERVED,   // Temporarily held (expires after timeout)
    BOOKED      // Confirmed booking (payment completed)
}

// ===== PRICING STRATEGY PATTERN =====

interface PricingStrategy {
    double calculate(double basePrice);
    String getName();
}

class StandardPricing implements PricingStrategy {
    @Override
    public double calculate(double basePrice) {
        return basePrice;
    }
    
    @Override
    public String getName() { return "Standard (1x)"; }
}

class PremiumPricing implements PricingStrategy {
    @Override
    public double calculate(double basePrice) {
        return basePrice * 2.0;
    }
    
    @Override
    public String getName() { return "Premium (2x)"; }
}

class WeekendPricing implements PricingStrategy {
    @Override
    public double calculate(double basePrice) {
        return basePrice * 1.5;
    }
    
    @Override
    public String getName() { return "Weekend (1.5x)"; }
}

// ===== SUPPORTING CLASSES =====

/**
 * Represents a seat in the booking system
 */
class Seat {
    private final String seatId;
    private SeatStatus status;
    private final double basePrice;
    private String reservedBy;
    private long reservationExpiry;
    
    public Seat(String seatId, double basePrice) {
        this.seatId = seatId;
        this.status = SeatStatus.AVAILABLE;
        this.basePrice = basePrice;
    }
    
    public synchronized SeatStatus getStatus() { return status; }
    public synchronized void setStatus(SeatStatus s) { this.status = s; }
    public String getSeatId() { return seatId; }
    public double getBasePrice() { return basePrice; }
    public synchronized String getReservedBy() { return reservedBy; }
    public synchronized void setReservedBy(String user) { this.reservedBy = user; }
    public synchronized long getReservationExpiry() { return reservationExpiry; }
    public synchronized void setReservationExpiry(long expiry) { this.reservationExpiry = expiry; }
    
    public synchronized boolean isReservationExpired() {
        return status == SeatStatus.RESERVED && System.currentTimeMillis() > reservationExpiry;
    }
    
    @Override
    public String toString() {
        return seatId + "[" + status + ", $" + basePrice + "]";
    }
}

/**
 * Seat Booking System - Low Level Design (LLD)
 * 
 * PROBLEM STATEMENT:
 * Design a seat booking system that can:
 * 1. Reserve seats temporarily (with timeout)
 * 2. Confirm bookings (convert reservation to confirmed)
 * 3. Handle concurrent booking attempts
 * 4. Auto-expire reservations
 * 5. Support different pricing strategies
 * 
 * REQUIREMENTS:
 * - Functional: Reserve, confirm, cancel, check availability
 * - Non-Functional: Thread-safe, handle race conditions, low latency
 * 
 * INTERVIEW HINTS:
 * - Discuss locking mechanisms (pessimistic vs optimistic)
 * - Talk about reservation timeout strategy
 * - Mention double-booking prevention
 * - Consider database transaction isolation levels
 * - Discuss idempotency for booking APIs
 */
class BookingSystem {
    private final ConcurrentHashMap<String, Seat> seats;
    private final ConcurrentHashMap<String, Lock> seatLocks;
    private final ScheduledExecutorService scheduler;
    
    public BookingSystem() {
        this.seats = new ConcurrentHashMap<>();
        this.seatLocks = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
    }
    
    /**
     * Add a seat to the system
     * 
     * @param seatId Seat identifier
     * @param basePrice Base price of the seat
     * @return Seat ID
     */
    public String addSeat(String seatId, double basePrice) {
        // TODO: Implement
        // HINT: Seat seat = new Seat(seatId, basePrice);
        // HINT: seats.put(seatId, seat);
        // HINT: return seatId;
        return null;
    }
    
    /**
     * Reserve a seat temporarily with timeout
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get seat and validate it exists
     * 2. Get or create lock for this seat (per-seat locking)
     * 3. Try to acquire lock with tryLock(timeout)
     * 4. Check if seat is AVAILABLE (or expired RESERVED)
     * 5. If reserved and expired, free it first
     * 6. Set status to RESERVED, store userId
     * 7. Calculate expiry time: now + timeoutSeconds
     * 8. Schedule auto-expiration task
     * 9. Calculate price using pricing strategy
     * 10. Release lock and return true
     * 
     * INTERVIEW DISCUSSION:
     * - How to prevent double booking?
     * - What if user never confirms?
     * - How to handle high concurrency?
     * 
     * @param seatId Seat to reserve
     * @param userId User making reservation
     * @param timeoutSeconds Reservation expires after this
     * @param pricing Pricing strategy to apply
     * @return true if reserved successfully
     * @throws SeatNotFoundException if seat doesn't exist
     * @throws SeatAlreadyBookedException if seat not available
     */
    public boolean reserveSeat(String seatId, String userId, int timeoutSeconds, PricingStrategy pricing) 
            throws SeatNotFoundException, SeatAlreadyBookedException, InterruptedException {
        // TODO: Implement
        // HINT: Seat seat = seats.get(seatId);
        // HINT: if (seat == null) throw new SeatNotFoundException(seatId);
        // HINT: Lock lock = seatLocks.computeIfAbsent(seatId, k -> new ReentrantLock());
        // HINT: if (!lock.tryLock(1, TimeUnit.SECONDS)) throw new SeatAlreadyBookedException(seatId, null);
        // HINT: try {
        //     // Check and handle expired reservation
        //     if (seat.isReservationExpired()) { seat.setStatus(SeatStatus.AVAILABLE); }
        //     if (seat.getStatus() != SeatStatus.AVAILABLE) 
        //         throw new SeatAlreadyBookedException(seatId, seat.getStatus());
        //     seat.setStatus(SeatStatus.RESERVED);
        //     seat.setReservedBy(userId);
        //     seat.setReservationExpiry(System.currentTimeMillis() + timeoutSeconds * 1000L);
        //     scheduleExpiration(seatId, timeoutSeconds);
        //     double price = pricing.calculate(seat.getBasePrice());
        //     return true;
        // } finally { lock.unlock(); }
        return false;
    }
    
    /**
     * Confirm booking for reserved seat
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get seat and validate exists
     * 2. Acquire lock for seat
     * 3. Validate seat is RESERVED
     * 4. Validate reservation hasn't expired
     * 5. Validate userId matches reservation
     * 6. Set status to BOOKED
     * 7. Return true
     * 
     * @param seatId Seat to confirm
     * @param userId User confirming (must match reservation)
     * @return true if confirmed
     * @throws SeatNotFoundException if seat doesn't exist
     * @throws InvalidBookingException if not reserved or wrong user
     */
    public boolean confirmBooking(String seatId, String userId) 
            throws SeatNotFoundException, InvalidBookingException, InterruptedException {
        // TODO: Implement
        // HINT: Get seat, acquire lock
        // HINT: Check seat.getStatus() == SeatStatus.RESERVED
        // HINT: Check !seat.isReservationExpired()
        // HINT: Check seat.getReservedBy().equals(userId)
        // HINT: seat.setStatus(SeatStatus.BOOKED);
        return false;
    }
    
    /**
     * Cancel reservation or booking
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get seat and lock
     * 2. If RESERVED or BOOKED, set to AVAILABLE
     * 3. Clear reservedBy
     * 
     * @param seatId Seat to cancel
     * @throws SeatNotFoundException if seat doesn't exist
     */
    public boolean cancelSeat(String seatId) throws SeatNotFoundException, InterruptedException {
        // TODO: Implement
        // HINT: Acquire lock, check status, set to AVAILABLE
        return false;
    }
    
    /**
     * Schedule automatic expiration of reservation
     * 
     * IMPLEMENTATION HINTS:
     * 1. Use scheduler.schedule() with delay
     * 2. In the task: check if still RESERVED, then free it
     * 3. Acquire lock before changing status
     * 
     * @param seatId Seat to expire
     * @param delaySeconds Delay before expiration
     */
    private void scheduleExpiration(String seatId, int delaySeconds) {
        // TODO: Implement
        // HINT: scheduler.schedule(() -> {
        //     Seat seat = seats.get(seatId);
        //     Lock lock = seatLocks.get(seatId);
        //     if (seat != null && lock != null) {
        //         lock.lock();
        //         try {
        //             if (seat.isReservationExpired()) {
        //                 seat.setStatus(SeatStatus.AVAILABLE);
        //                 seat.setReservedBy(null);
        //             }
        //         } finally { lock.unlock(); }
        //     }
        // }, delaySeconds, TimeUnit.SECONDS);
    }
    
    /**
     * Get available seats
     * 
     * @return List of available seat IDs
     */
    public List<String> getAvailableSeats() {
        // TODO: Implement
        // HINT: return seats.values().stream()
        //     .filter(s -> s.getStatus() == SeatStatus.AVAILABLE)
        //     .map(Seat::getSeatId)
        //     .collect(Collectors.toList());
        return new ArrayList<>();
    }
    
    /**
     * Display all seats and their status
     */
    public void displaySeats() {
        System.out.println("\n--- Seats Status ---");
        seats.forEach((id, seat) -> System.out.println("  " + seat));
        long available = seats.values().stream().filter(s -> s.getStatus() == SeatStatus.AVAILABLE).count();
        long reserved = seats.values().stream().filter(s -> s.getStatus() == SeatStatus.RESERVED).count();
        long booked = seats.values().stream().filter(s -> s.getStatus() == SeatStatus.BOOKED).count();
        System.out.println("Total: " + seats.size() + " (Available:" + available + 
                         ", Reserved:" + reserved + ", Booked:" + booked + ")");
    }
    
    /**
     * Shutdown scheduler gracefully
     */
    public void shutdown() {
        scheduler.shutdown();
    }
}

// ===== MAIN TEST CLASS =====

public class SeatBooking {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Seat Booking System Test Cases ===\n");
        
        BookingSystem system = new BookingSystem();
        
        // Add seats
        system.addSeat("A1", 10.0);
        system.addSeat("A2", 10.0);
        system.addSeat("B1", 15.0);
        system.addSeat("B2", 15.0);
        
        system.displaySeats();
        
        // Test Case 1: Reserve and Confirm
        System.out.println("\n=== Test Case 1: Reserve and Confirm ===");
        try {
            boolean reserved = system.reserveSeat("A1", "alice", 5, new StandardPricing());
            System.out.println("✓ Seat A1 reserved: " + reserved);
            
            boolean confirmed = system.confirmBooking("A1", "alice");
            System.out.println("✓ Booking confirmed: " + confirmed);
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        
        system.displaySeats();
        
        // Test Case 2: Premium Pricing
        System.out.println("\n=== Test Case 2: Premium Pricing ===");
        try {
            boolean reserved = system.reserveSeat("B1", "bob", 5, new PremiumPricing());
            System.out.println("✓ Seat B1 reserved with premium pricing: " + reserved);
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        
        // Test Case 3: Concurrent Booking Attempt
        System.out.println("\n=== Test Case 3: Concurrent Booking (same seat) ===");
        try {
            system.reserveSeat("A2", "charlie", 5, new StandardPricing());
            System.out.println("✓ Charlie reserved A2");
            
            // Dave tries to book same seat - should fail
            system.reserveSeat("A2", "dave", 5, new StandardPricing());
            System.out.println("✗ Should have thrown SeatAlreadyBookedException");
        } catch (SeatAlreadyBookedException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
            System.out.println("  Seat: " + e.getSeatId() + ", Status: " + e.getCurrentStatus());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        
        system.displaySeats();
        
        // Test Case 4: Reservation Expiration
        System.out.println("\n=== Test Case 4: Reservation Expiration (2s timeout) ===");
        try {
            system.reserveSeat("B2", "eve", 2, new WeekendPricing());
            System.out.println("✓ B2 reserved with 2s timeout");
            System.out.println("  Waiting 3 seconds for auto-expiration...");
            Thread.sleep(3000);
            
            // Should be available again
            system.reserveSeat("B2", "frank", 5, new StandardPricing());
            System.out.println("✓ B2 became available after expiration");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        
        system.displaySeats();
        
        // Test Case 5: Cancel Reservation
        System.out.println("\n=== Test Case 5: Cancel Reservation ===");
        try {
            String seatId = system.addSeat("C1", 20.0);
            system.reserveSeat("C1", "grace", 10, new StandardPricing());
            System.out.println("✓ C1 reserved");
            
            boolean cancelled = system.cancelSeat("C1");
            System.out.println("✓ C1 cancelled: " + cancelled);
            
            // Should be available now
            System.out.println("  Available seats: " + system.getAvailableSeats());
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        
        // Test Case 6: Exception - Seat Not Found
        System.out.println("\n=== Test Case 6: Exception - Seat Not Found ===");
        try {
            system.reserveSeat("INVALID", "user", 5, new StandardPricing());
            System.out.println("✗ Should have thrown SeatNotFoundException");
        } catch (SeatNotFoundException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        
        system.displaySeats();
        System.out.println("\n=== All Test Cases Complete! ===");
        
        system.shutdown();
    }
}

/**
 * INTERVIEW DISCUSSION TOPICS:
 * ============================
 * 
 * 1. CONCURRENCY CONTROL:
 *    Pessimistic Locking:
 *      - Lock row/record before modification
 *      - Prevents conflicts but can cause contention
 *      - Implementation: ReentrantLock per seat
 *    
 *    Optimistic Locking:
 *      - Check version/timestamp before update
 *      - Retry on conflict
 *      - Better throughput but may require retries
 *      - Implementation: Version number or timestamp
 *    
 *    Database Level:
 *      - SELECT FOR UPDATE (pessimistic)
 *      - Version column with WHERE clause (optimistic)
 * 
 * 2. RESERVATION TIMEOUT:
 *    Why needed?
 *      - Prevent seats being held indefinitely
 *      - Typical: 5-10 minutes for payment
 *    
 *    Implementation Options:
 *      - Background thread checking expiry
 *      - ScheduledExecutorService (used here)
 *      - Database TTL (Redis EXPIRE)
 *      - Lazy expiration on next access
 * 
 * 3. RACE CONDITIONS TO PREVENT:
 *    - Two users booking same seat simultaneously
 *    - Reservation expiring during confirm
 *    - Seat status changes between check and update
 *    
 *    Solutions:
 *      - Per-seat locks (fine-grained locking)
 *      - CAS (Compare-And-Swap) operations
 *      - Database transactions with proper isolation
 * 
 * 4. PRICING STRATEGIES:
 *    - Strategy Pattern for flexible pricing
 *    - Standard, Premium, Weekend, Holiday rates
 *    - Dynamic pricing based on demand
 *    - Discount codes and promotions
 * 
 * 5. SCALABILITY:
 *    Single Server:
 *      - In-memory state (fast but not persistent)
 *      - Works for small venues
 *    
 *    Distributed System:
 *      - Database for persistence
 *      - Redis for distributed locks
 *      - Message queue for async processing
 *      - Eventual consistency considerations
 * 
 * 6. DATABASE DESIGN:
 *    Tables:
 *      - seats (id, price, status)
 *      - bookings (id, seat_id, user_id, timestamp, status)
 *      - reservations (seat_id, user_id, expires_at)
 *    
 *    Indexes:
 *      - (seat_id, status) for availability queries
 *      - (user_id) for user's bookings
 *      - (expires_at) for cleanup
 * 
 * 7. ADVANCED FEATURES:
 *    - Hold multiple seats atomically
 *    - Seat selection recommendations (best available)
 *    - Accessibility requirements (wheelchair seats)
 *    - Group bookings with seat adjacency
 *    - Waitlist when sold out
 *    - Payment integration
 *    - Refund handling
 * 
 * 8. REAL-WORLD CONSIDERATIONS:
 *    - Payment gateway integration
 *    - Email confirmations
 *    - QR code generation for tickets
 *    - Seat maps and visualization
 *    - Mobile app synchronization
 *    - Audit trail for fraud prevention
 * 
 * 9. DESIGN PATTERNS:
 *    - Strategy Pattern: Pricing strategies
 *    - State Pattern: Seat status transitions
 *    - Singleton: Scheduler service
 *    - Factory: Create different seat types
 * 
 * 10. API DESIGN:
 *     POST /seats/{id}/reserve    - Reserve seat
 *     POST /bookings/confirm      - Confirm booking
 *     DELETE /bookings/{id}       - Cancel booking
 *     GET /seats/available        - Get available seats
 *     GET /users/{id}/bookings    - User's bookings
 */
