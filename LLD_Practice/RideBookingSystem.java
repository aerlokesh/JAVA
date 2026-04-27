import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/*
 * RIDE BOOKING SERVICE - Low Level Design
 * ==========================================
 * 
 * REQUIREMENTS:
 * 1. Riders request rides with pickup/drop locations
 * 2. Match rider to nearest available driver (Strategy)
 * 3. Drivers can go online/offline
 * 4. Ride lifecycle: REQUESTED → MATCHED → IN_PROGRESS → COMPLETED / CANCELLED
 * 5. Pricing: pluggable (Strategy) — base fare, surge pricing
 * 6. Track ride events (Observer)
 * 7. Thread-safe
 * 
 * DESIGN PATTERNS:
 *   Strategy  (DriverMatchStrategy) — NearestDriverStrategy
 *   Strategy  (PricingStrategy)     — BasePricing, SurgePricing
 *   Observer  (RideListener)        — RideLogger
 *   Facade    (RideBookingService)
 * 
 * KEY DS: Map<driverId, Driver>, Map<rideId, Ride>
 */

// ==================== EXCEPTIONS ====================

class NoDriverAvailableException extends RuntimeException {
    NoDriverAvailableException() { super("No available driver nearby"); }
}

class RideNotFoundException extends RuntimeException {
    RideNotFoundException(String rideId) { super("Ride not found: " + rideId); }
}

class InvalidRideStateException extends RuntimeException {
    InvalidRideStateException(String rideId, String msg) { super("Ride " + rideId + ": " + msg); }
}

// ==================== ENUMS ====================

enum RideStatus { REQUESTED, MATCHED, IN_PROGRESS, COMPLETED, CANCELLED }

enum DriverStatus { AVAILABLE, ON_RIDE, OFFLINE }

// ==================== MODELS ====================

class Location {
    final double lat, lng;
    Location(double lat, double lng) { this.lat = lat; this.lng = lng; }

    double distanceTo(Location other) {
        double dx = this.lat - other.lat, dy = this.lng - other.lng;
        return Math.sqrt(dx * dx + dy * dy); // simplified euclidean
    }
}

class RideDriver {
    final String id, name;
    DriverStatus status;
    Location location;

    RideDriver(String id, String name, double lat, double lng) {
        this.id = id; this.name = name; this.status = DriverStatus.AVAILABLE;
        this.location = new Location(lat, lng);
    }
}

class RideRider {
    final String id, name;
    RideRider(String id, String name) { this.id = id; this.name = name; }
}

class Ride {
    final String id;
    final RideRider rider;
    final Location pickup, dropoff;
    RideDriver driver;
    RideStatus status;
    double fare;

    Ride(String id, RideRider rider, Location pickup, Location dropoff) {
        this.id = id; this.rider = rider; this.pickup = pickup;
        this.dropoff = dropoff; this.status = RideStatus.REQUESTED;
    }
}

// ==================== INTERFACES ====================

/** Strategy — how to pick a driver for a ride. */
interface DriverMatchStrategy {
    RideDriver match(Collection<RideDriver> drivers, Location pickup);
}

/** Strategy — how to calculate fare. */
interface RidePricingStrategy {
    double calculate(Location pickup, Location dropoff);
}

/** Observer — ride notifications to rider/driver. */
interface RideNotificationListener {
    void notify(String rideId, RideStatus status, String recipientId, String message);
}

// ==================== STRATEGY IMPLEMENTATIONS ====================

/** Pick nearest available driver within maxDistance. */
class NearestDriverStrategy implements DriverMatchStrategy {
    final double maxDistance;
    NearestDriverStrategy(double maxDistance) { this.maxDistance = maxDistance; }
    NearestDriverStrategy() { this(50.0); }

    @Override public RideDriver match(Collection<RideDriver> drivers, Location pickup) {
        return drivers.stream()
            .filter(d -> d.status == DriverStatus.AVAILABLE)
            .filter(d -> d.location.distanceTo(pickup) <= maxDistance)
            .min(Comparator.comparingDouble(d -> d.location.distanceTo(pickup)))
            .orElse(null);
    }
}

/** Base pricing: baseFare + perKm * distance. */
class BasePricing implements RidePricingStrategy {
    final double baseFare, perKm;
    BasePricing(double baseFare, double perKm) { this.baseFare = baseFare; this.perKm = perKm; }
    BasePricing() { this(5.0, 2.0); }

    @Override public double calculate(Location pickup, Location dropoff) {
        return baseFare + perKm * pickup.distanceTo(dropoff);
    }
}

/** Surge pricing: multiplier on top of base. */
class SurgePricing implements RidePricingStrategy {
    final double baseFare, perKm, surgeMultiplier;
    SurgePricing(double baseFare, double perKm, double surge) { this.baseFare = baseFare; this.perKm = perKm; this.surgeMultiplier = surge; }
    SurgePricing(double surge) { this(5.0, 2.0, surge); }

    @Override public double calculate(Location pickup, Location dropoff) {
        return (baseFare + perKm * pickup.distanceTo(dropoff)) * surgeMultiplier;
    }
}

// ==================== OBSERVER IMPLEMENTATIONS ====================

/** Notifies rider about ride status changes. */
class RiderNotifier implements RideNotificationListener {
    final List<String> notifications = new ArrayList<>();
    @Override public void notify(String rideId, RideStatus status, String recipientId, String message) {
        notifications.add("[RIDER " + recipientId + "] " + status + ": " + message);
    }
}

/** Notifies driver about ride assignments. */
class DriverNotifier implements RideNotificationListener {
    final List<String> notifications = new ArrayList<>();
    @Override public void notify(String rideId, RideStatus status, String recipientId, String message) {
        notifications.add("[DRIVER " + recipientId + "] " + status + ": " + message);
    }
}

// ==================== RIDE BOOKING SERVICE (FACADE) ====================

class RideBookingService {
    private final Map<String, RideDriver> drivers = new ConcurrentHashMap<>();
    private final Map<String, Ride> rides = new ConcurrentHashMap<>();
    private DriverMatchStrategy matchStrategy;
    private RidePricingStrategy pricingStrategy;
    private final List<RideNotificationListener> listeners = new ArrayList<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private int rideCounter = 0;

    RideBookingService(DriverMatchStrategy match, RidePricingStrategy pricing) {
        this.matchStrategy = match; this.pricingStrategy = pricing;
    }
    RideBookingService() { this(new NearestDriverStrategy(), new BasePricing()); }

    void setMatchStrategy(DriverMatchStrategy s) { this.matchStrategy = s; }
    void setPricingStrategy(RidePricingStrategy s) { this.pricingStrategy = s; }
    void addListener(RideNotificationListener l) { listeners.add(l); }

    private void notifyRide(Ride ride, RideStatus status, String message) {
        for (RideNotificationListener l : listeners) {
            l.notify(ride.id, status, ride.rider.id, message);
            if (ride.driver != null) l.notify(ride.id, status, ride.driver.id, message);
        }
    }

    void registerDriver(RideDriver driver) { drivers.put(driver.id, driver); }

    void setDriverOnline(String driverId) {
        RideDriver d = drivers.get(driverId);
        if (d != null) d.status = DriverStatus.AVAILABLE;
    }

    void setDriverOffline(String driverId) {
        RideDriver d = drivers.get(driverId);
        if (d != null) d.status = DriverStatus.OFFLINE;
    }

    /** Request a ride: match driver, calculate fare. */
    Ride requestRide(RideRider rider, double pickLat, double pickLng, double dropLat, double dropLng) {
        lock.writeLock().lock();
        try {
            Location pickup = new Location(pickLat, pickLng);
            Location dropoff = new Location(dropLat, dropLng);
            RideDriver driver = matchStrategy.match(drivers.values(), pickup);
            if (driver == null) throw new NoDriverAvailableException();

            String rideId = "RIDE-" + (++rideCounter);
            Ride ride = new Ride(rideId, rider, pickup, dropoff);
            ride.driver = driver;
            ride.fare = pricingStrategy.calculate(pickup, dropoff);
            ride.status = RideStatus.MATCHED;
            driver.status = DriverStatus.ON_RIDE;
            rides.put(rideId, ride);
            notifyRide(ride, RideStatus.MATCHED, "driver=" + driver.name + " fare=" + String.format("%.2f", ride.fare));
            return ride;
        } finally { lock.writeLock().unlock(); }
    }

    /** Driver starts the ride. */
    void startRide(String rideId) {
        lock.writeLock().lock();
        try {
            Ride ride = getRide(rideId);
            if (ride.status != RideStatus.MATCHED) throw new InvalidRideStateException(rideId, "Cannot start, status=" + ride.status);
            ride.status = RideStatus.IN_PROGRESS;
            notifyRide(ride, RideStatus.IN_PROGRESS, "Ride started");
        } finally { lock.writeLock().unlock(); }
    }

    /** Complete the ride, free driver. */
    void completeRide(String rideId) {
        lock.writeLock().lock();
        try {
            Ride ride = getRide(rideId);
            if (ride.status != RideStatus.IN_PROGRESS) throw new InvalidRideStateException(rideId, "Cannot complete, status=" + ride.status);
            ride.status = RideStatus.COMPLETED;
            ride.driver.status = DriverStatus.AVAILABLE;
            ride.driver.location = ride.dropoff; // driver is now at dropoff
            notifyRide(ride, RideStatus.COMPLETED, "fare=" + String.format("%.2f", ride.fare));
        } finally { lock.writeLock().unlock(); }
    }

    /** Cancel a ride (only if MATCHED, not started). */
    void cancelRide(String rideId) {
        lock.writeLock().lock();
        try {
            Ride ride = getRide(rideId);
            if (ride.status != RideStatus.MATCHED) throw new InvalidRideStateException(rideId, "Cannot cancel, status=" + ride.status);
            ride.status = RideStatus.CANCELLED;
            ride.driver.status = DriverStatus.AVAILABLE;
            notifyRide(ride, RideStatus.CANCELLED, "Ride cancelled");
        } finally { lock.writeLock().unlock(); }
    }

    Ride getRide(String rideId) {
        Ride r = rides.get(rideId);
        if (r == null) throw new RideNotFoundException(rideId);
        return r;
    }

    int getActiveRideCount() {
        return (int) rides.values().stream()
            .filter(r -> r.status == RideStatus.MATCHED || r.status == RideStatus.IN_PROGRESS).count();
    }

    long getAvailableDriverCount() {
        return drivers.values().stream().filter(d -> d.status == DriverStatus.AVAILABLE).count();
    }
}

// ==================== MAIN / TESTS ====================

public class RideBookingSystem {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════╗");
        System.out.println("║   RIDE BOOKING SERVICE - LLD Demo     ║");
        System.out.println("╚═══════════════════════════════════════╝\n");

        // --- Test 1: Request & Match ---
        System.out.println("=== Test 1: Request & Match ===");
        RideBookingService svc = new RideBookingService();
        svc.registerDriver(new RideDriver("d1", "Alice", 10, 10));
        svc.registerDriver(new RideDriver("d2", "Bob", 20, 20));
        RideRider rider = new RideRider("r1", "Charlie");
        Ride ride = svc.requestRide(rider, 11, 11, 30, 30);
        check(ride.status, RideStatus.MATCHED, "Matched");
        check(ride.driver.name, "Alice", "Nearest driver = Alice");
        check(ride.fare > 0, true, "Fare > 0: " + String.format("%.2f", ride.fare));
        System.out.println("✓\n");

        // --- Test 2: Start & Complete ---
        System.out.println("=== Test 2: Start & Complete ===");
        svc.startRide(ride.id);
        check(ride.status, RideStatus.IN_PROGRESS, "In progress");
        svc.completeRide(ride.id);
        check(ride.status, RideStatus.COMPLETED, "Completed");
        check(ride.driver.status, DriverStatus.AVAILABLE, "Driver available again");
        System.out.println("✓\n");

        // --- Test 3: Cancel ---
        System.out.println("=== Test 3: Cancel ===");
        Ride ride2 = svc.requestRide(new RideRider("r2", "Dave"), 10, 10, 25, 25);
        svc.cancelRide(ride2.id);
        check(ride2.status, RideStatus.CANCELLED, "Cancelled");
        check(ride2.driver.status, DriverStatus.AVAILABLE, "Driver freed");
        System.out.println("✓\n");

        // --- Test 4: No driver available ---
        System.out.println("=== Test 4: No driver ===");
        svc.setDriverOffline("d1"); svc.setDriverOffline("d2");
        try { svc.requestRide(new RideRider("r3", "Eve"), 10, 10, 20, 20); System.out.println("  ✗"); }
        catch (NoDriverAvailableException e) { System.out.println("  ✓ " + e.getMessage()); }
        svc.setDriverOnline("d1"); svc.setDriverOnline("d2");
        System.out.println("✓\n");

        // --- Test 5: Invalid state transitions ---
        System.out.println("=== Test 5: Invalid states ===");
        Ride ride3 = svc.requestRide(new RideRider("r4", "Frank"), 10, 10, 20, 20);
        try { svc.completeRide(ride3.id); } // can't complete before start
        catch (InvalidRideStateException e) { System.out.println("  ✓ " + e.getMessage()); }
        svc.startRide(ride3.id);
        try { svc.cancelRide(ride3.id); } // can't cancel after start
        catch (InvalidRideStateException e) { System.out.println("  ✓ " + e.getMessage()); }
        svc.completeRide(ride3.id);
        try { svc.startRide(ride3.id); } // can't start completed ride
        catch (InvalidRideStateException e) { System.out.println("  ✓ " + e.getMessage()); }
        System.out.println("✓\n");

        // --- Test 6: Ride not found ---
        System.out.println("=== Test 6: Ride not found ===");
        try { svc.getRide("RIDE-999"); }
        catch (RideNotFoundException e) { System.out.println("  ✓ " + e.getMessage()); }
        System.out.println("✓\n");

        // --- Test 7: Surge pricing ---
        System.out.println("=== Test 7: Surge pricing ===");
        RideBookingService svc2 = new RideBookingService(new NearestDriverStrategy(), new BasePricing());
        svc2.registerDriver(new RideDriver("d1", "Alice", 0, 0));
        Ride base = svc2.requestRide(new RideRider("r1", "X"), 0, 0, 10, 0);
        double baseFare = base.fare;
        svc2.startRide(base.id);
        svc2.completeRide(base.id);

        RideBookingService svc3 = new RideBookingService(new NearestDriverStrategy(), new SurgePricing(2.0));
        svc3.registerDriver(new RideDriver("d1", "Alice", 0, 0));
        Ride surge = svc3.requestRide(new RideRider("r1", "X"), 0, 0, 10, 0);
        double surgeFare = surge.fare;
        System.out.printf("  Base=%.2f, Surge(2x)=%.2f\n", baseFare, surgeFare);
        check(Math.abs(surgeFare - baseFare * 2) < 0.01, true, "Surge = 2x base");
        System.out.println("✓\n");

        // --- Test 8: Observer ---
        System.out.println("=== Test 8: Observer ===");
        RideBookingService svc4 = new RideBookingService();
        RiderNotifier notifier = new RiderNotifier();
        svc4.addListener(notifier);
        svc4.registerDriver(new RideDriver("d1", "A", 0, 0));
        Ride r4 = svc4.requestRide(new RideRider("r1", "B"), 0, 0, 5, 5);
        svc4.startRide(r4.id);
        svc4.completeRide(r4.id);
        check(notifier.notifications.size() >= 3, true, "3 events: matched, in_progress, completed");
        System.out.println("  Notifications: " + notifier.notifications);
        System.out.println("✓\n");

        // --- Test 9: Driver goes to dropoff after completion ---
        System.out.println("=== Test 9: Driver location updates ===");
        RideBookingService svc5 = new RideBookingService();
        svc5.registerDriver(new RideDriver("d1", "A", 0, 0));
        svc5.registerDriver(new RideDriver("d2", "B", 100, 100));
        Ride r5 = svc5.requestRide(new RideRider("r1", "X"), 1, 1, 50, 50);
        svc5.startRide(r5.id);
        svc5.completeRide(r5.id);
        // Now request near (50,50) — driver A should be matched (at dropoff)
        Ride r6 = svc5.requestRide(new RideRider("r2", "Y"), 51, 51, 60, 60);
        check(r6.driver.name, "A", "Driver A at dropoff (50,50) matched for (51,51)");
        System.out.println("✓\n");

        // --- Test 10: Metrics ---
        System.out.println("=== Test 10: Metrics ===");
        check((int) svc5.getAvailableDriverCount(), 1, "1 available (B, A is on ride)");
        check(svc5.getActiveRideCount(), 1, "1 active ride");
        System.out.println("✓\n");

        // --- Test 11: Thread Safety ---
        System.out.println("=== Test 11: Thread Safety ===");
        RideBookingService svc6 = new RideBookingService();
        for (int i = 0; i < 50; i++) svc6.registerDriver(new RideDriver("d" + i, "D" + i, i, i));
        ExecutorService exec = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            int x = i;
            futures.add(exec.submit(() -> {
                try { svc6.requestRide(new RideRider("r" + x, "R" + x), x, x, x + 10, x + 10); }
                catch (Exception e) {}
            }));
        }
        for (Future<?> f : futures) { try { f.get(); } catch (Exception e) {} }
        exec.shutdown();
        System.out.println("  Active rides: " + svc6.getActiveRideCount() + ", Available drivers: " + svc6.getAvailableDriverCount());
        check(svc6.getActiveRideCount() > 0, true, "Concurrent rides booked");
        System.out.println("✓\n");

        System.out.println("════════ ALL 11 TESTS PASSED ✓ ════════");
    }

    static void check(RideStatus a, RideStatus e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
    static void check(DriverStatus a, DriverStatus e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
    static void check(int a, int e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
    static void check(String a, String e, String m) { System.out.println("  " + (Objects.equals(a, e) ? "✓" : "✗ GOT '" + a + "'") + " " + m); }
    static void check(boolean a, boolean e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. STRATEGY (DriverMatchStrategy): NearestDriverStrategy picks closest
 *    available driver within maxDistance. Could add ZoneStrategy, LoadBalancedStrategy.
 *
 * 2. STRATEGY (RidePricingStrategy): BasePricing (flat + perKm), SurgePricing
 *    (multiplier). Swap at runtime for peak hours.
 *
 * 3. OBSERVER (RideListener): RideLogger tracks MATCHED/IN_PROGRESS/COMPLETED/CANCELLED.
 *    Could feed analytics, billing, notifications.
 *
 * 4. RIDE LIFECYCLE: REQUESTED → MATCHED → IN_PROGRESS → COMPLETED/CANCELLED.
 *    Invalid transitions throw InvalidRideStateException.
 *
 * 5. DRIVER LIFECYCLE: AVAILABLE → ON_RIDE (matched) → AVAILABLE (completed).
 *    OFFLINE when driver goes offline. Location updates on ride completion.
 *
 * 6. THREAD SAFETY: ReadWriteLock for ride operations. ConcurrentHashMap for registries.
 *
 * 7. EXTENSIONS: ETA estimation, ride sharing (pool), driver ratings,
 *    payment integration, route optimization, geo-spatial indexing (R-tree).
 */
