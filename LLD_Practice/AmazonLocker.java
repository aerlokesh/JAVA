import java.time.LocalDateTime;
import java.util.*;

// ===== CUSTOM EXCEPTION CLASSES =====

class NoCompartmentException extends Exception {
    public NoCompartmentException(String msg) { super(msg); }
}

class InvalidCodeException extends Exception {
    public InvalidCodeException(String msg) { super(msg); }
}

class ParcelNotFoundException extends Exception {
    public ParcelNotFoundException(String msg) { super(msg); }
}

// ===== ENUMS =====

enum CompartmentStatus { FREE, OCCUPIED }
enum Size { SMALL, MEDIUM, LARGE }

// ===== SUPPORTING CLASSES =====

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

class Locker {
    String lockerId;
    List<Compartment> compartments;
    Map<String, Assignment> assignments;
    
    Locker(List<Compartment> compartments) {
        this.lockerId = "LOCKER-" + UUID.randomUUID().toString().substring(0, 6);
        this.compartments = new ArrayList<>(compartments);
        this.assignments = new HashMap<>();
    }
    
    /**
     * Add parcel to locker
     * IMPLEMENTATION HINTS:
     * 1. Find free compartment of exact size
     * 2. If not found, find next larger size
     * 3. Mark compartment as OCCUPIED
     * 4. Generate unique code
     * 5. Create and store Assignment
     */
    public synchronized String addParcel(Parcel parcel) throws NoCompartmentException {
        // TODO: Implement
        // HINT: Use findFreeCompartment(parcel.size) first
        // HINT: Then try findLargerCompartment(parcel.size) as fallback
        return null;
    }
    
    /**
     * Pickup parcel using code
     * IMPLEMENTATION HINTS:
     * 1. Validate code exists in assignments
     * 2. Check if already picked up
     * 3. Check if expired
     * 4. Free the compartment
     * 5. Mark assignment as picked up
     */
    public synchronized void pickupParcel(String code) throws InvalidCodeException {
        // TODO: Implement
        // HINT: Assignment assignment = assignments.get(code);
        // HINT: if (assignment == null) throw new InvalidCodeException("Invalid code");
    }
    
    private Compartment findFreeCompartment(Size size) {
        // TODO: Implement
        // HINT: Stream filter by status==FREE and size match
        return null;
    }
    
    private Compartment findLargerCompartment(Size size) {
        // TODO: Implement
        // HINT: Filter FREE, size.ordinal() > requested size, get minimum
        return null;
    }
    
    private Compartment findCompartmentById(String id) {
        // TODO: Implement
        return null;
    }
    
    private String generateCode() {
        return "CODE" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
    
    public void displayStatus() {
        System.out.println("\n--- Locker: " + lockerId + " ---");
        long free = compartments.stream().filter(c -> c.status == CompartmentStatus.FREE).count();
        System.out.println("Compartments: " + compartments.size() + " (Free: " + free + ")");
        System.out.println("Active assignments: " + assignments.size());
    }
}

public class AmazonLocker {
    public static void main(String[] args) {
        System.out.println("=== Amazon Locker Test Cases ===\n");
        
        List<Compartment> compartments = Arrays.asList(
            new Compartment(Size.SMALL),
            new Compartment(Size.MEDIUM),
            new Compartment(Size.LARGE)
        );
        
        Locker locker = new Locker(compartments);
        locker.displayStatus();
        
        try {
            String code = locker.addParcel(new Parcel(Size.SMALL, "+1-555-0001"));
            System.out.println("✓ Parcel added, code: " + code);
            locker.pickupParcel(code);
            System.out.println("✓ Parcel picked up");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        
        locker.displayStatus();
    }
}
