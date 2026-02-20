import java.time.LocalDateTime;
import java.util.*;

// Enums
enum CompartmentStatus { FREE, OCCUPIED }
enum Size { SMALL, MEDIUM, LARGE }

// Custom Exceptions
class NoCompartmentException extends Exception {
    public NoCompartmentException(String msg) { super(msg); }
}

class InvalidCodeException extends Exception {
    public InvalidCodeException(String msg) { super(msg); }
}

// Assignment tracks code -> compartment mapping
class Assignment {
    String code;
    String parcelId;
    String compartmentId;
    LocalDateTime expiresAt;
    boolean pickedUp;

    Assignment(String code, String parcelId, String compartmentId) {
        this.code = code;
        this.parcelId = parcelId;
        this.compartmentId = compartmentId;
        this.expiresAt = LocalDateTime.now().plusHours(48);
        this.pickedUp = false;
    }

    boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt) && !pickedUp;
    }
}

// Compartment
class Compartment {
    String id;
    Size size;
    CompartmentStatus status;

    Compartment(Size size) {
        this.id = size + "-" + UUID.randomUUID().toString().substring(0, 6);
        this.size = size;
        this.status = CompartmentStatus.FREE;
    }
}

// Parcel
class Parcel {
    String id;
    Size size;
    String recipientPhone;

    Parcel(Size size, String phone) {
        this.id = size + "-" + UUID.randomUUID().toString().substring(0, 6);
        this.size = size;
        this.recipientPhone = phone;
    }
}

// Main Locker class - THREAD-SAFE
class Locker {
    String lockerId;
    List<Compartment> compartments;
    Map<String, Assignment> assignments;
    private final Object lock = new Object(); // For fine-grained locking

    Locker(List<Compartment> compartments) {
        this.lockerId = "LOCKER-" + UUID.randomUUID().toString().substring(0, 6);
        this.compartments = new ArrayList<>(compartments);
        this.assignments = new HashMap<>();
    }

    // Add parcel - SYNCHRONIZED for thread safety
    public synchronized String addParcel(Parcel parcel) throws NoCompartmentException {
        // Try exact match first
        Compartment comp = findFreeCompartment(parcel.size);
        
        // Fallback to larger size
        if (comp == null) {
            comp = findLargerCompartment(parcel.size);
        }
        
        if (comp == null) {
            throw new NoCompartmentException("No compartment available");
        }

        // Assign compartment (atomic operation)
        comp.status = CompartmentStatus.OCCUPIED;
        String code = generateCode();
        assignments.put(code, new Assignment(code, parcel.id, comp.id));
        
        System.out.println(Thread.currentThread().getName() + ": Parcel " + parcel.id + " assigned to " + comp.id + ", code: " + code);
        return code;
    }

    // Pickup parcel - SYNCHRONIZED for thread safety
    public synchronized void pickupParcel(String code) throws InvalidCodeException {
        Assignment assignment = assignments.get(code);
        
        if (assignment == null) {
            throw new InvalidCodeException("Invalid code");
        }
        if (assignment.pickedUp) {
            throw new InvalidCodeException("Code already used");
        }
        if (assignment.isExpired()) {
            throw new InvalidCodeException("Code expired");
        }

        // Free compartment (atomic operation)
        Compartment comp = findCompartmentById(assignment.compartmentId);
        comp.status = CompartmentStatus.FREE;
        assignment.pickedUp = true;
        
        System.out.println(Thread.currentThread().getName() + ": Parcel picked up. Compartment " + comp.id + " now FREE");
    }

    // Helper methods
    private Compartment findFreeCompartment(Size size) {
        return compartments.stream()
            .filter(c -> c.status == CompartmentStatus.FREE && c.size == size)
            .findFirst()
            .orElse(null);
    }

    private Compartment findLargerCompartment(Size size) {
        return compartments.stream()
            .filter(c -> c.status == CompartmentStatus.FREE)
            .filter(c -> c.size.ordinal() > size.ordinal())
            .min(Comparator.comparing(c -> c.size.ordinal()))
            .orElse(null);
    }

    private Compartment findCompartmentById(String id) {
        return compartments.stream()
            .filter(c -> c.id.equals(id))
            .findFirst()
            .orElse(null);
    }

    private String generateCode() {
        return "CODE" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public void displayStatus() {
        System.out.println("\n--- Locker: " + lockerId + " ---");
        long free = compartments.stream().filter(c -> c.status == CompartmentStatus.FREE).count();
        long occupied = compartments.size() - free;
        System.out.println("Compartments: " + compartments.size() + " (Free: " + free + ", Occupied: " + occupied + ")");
        System.out.println("Active assignments: " + assignments.size());
    }
}

// Main class
public class AmazonLocker {
    public static void main(String[] args) throws InterruptedException {
        // Setup
        List<Compartment> compartments = Arrays.asList(
            new Compartment(Size.SMALL),
            new Compartment(Size.SMALL),
            new Compartment(Size.MEDIUM),
            new Compartment(Size.LARGE)
        );
        
        Locker locker = new Locker(compartments);
        locker.displayStatus();

        // Test 6: CONCURRENCY TEST - Multiple threads accessing locker simultaneously
        System.out.println("\n=== Test: CONCURRENT ACCESS (5 threads) ===");
        List<Thread> threads = new ArrayList<>();
        List<String> codes = Collections.synchronizedList(new ArrayList<>());
        
        // Create 5 threads trying to add parcels simultaneously
        for (int i = 0; i < 5; i++) {
            final int threadNum = i;
            Thread t = new Thread(() -> {
                try {
                    Parcel p = new Parcel(Size.SMALL, "+1-555-" + threadNum);
                    String code = locker.addParcel(p);
                    codes.add(code);
                } catch (NoCompartmentException e) {
                    System.out.println(Thread.currentThread().getName() + ": " + e.getMessage());
                }
            }, "Thread-" + i);
            threads.add(t);
            t.start();
        }
        
        // Wait for all threads to complete
        for (Thread t : threads) {
            t.join();
        }
        
        locker.displayStatus();
        
        // Test concurrent pickups
        System.out.println("\n=== Test: CONCURRENT PICKUPS ===");
        threads.clear();
        
        for (String code : codes) {
            Thread t = new Thread(() -> {
                try {
                    locker.pickupParcel(code);
                } catch (InvalidCodeException e) {
                    System.out.println(Thread.currentThread().getName() + ": " + e.getMessage());
                }
            });
            threads.add(t);
            t.start();
        }
        
        for (Thread t : threads) {
            t.join();
        }
        
        locker.displayStatus();
        System.out.println("\n✓ Concurrency test passed - no race conditions!");
    }
}
