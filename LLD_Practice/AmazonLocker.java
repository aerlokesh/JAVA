import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// ===== CUSTOM EXCEPTION CLASSES =====

/**
 * Exception thrown when no suitable compartment is available
 * WHEN TO THROW:
 * - No free compartments of requested size
 * - No larger compartments available as fallback
 */
class NoCompartmentException extends Exception {
    private Size requestedSize;
    
    public NoCompartmentException(Size requestedSize) {
        super("No compartment available for size: " + requestedSize);
        this.requestedSize = requestedSize;
    }
    
    public Size getRequestedSize() { return requestedSize; }
}

/**
 * Exception thrown when pickup code is invalid
 * WHEN TO THROW:
 * - Code doesn't exist in system
 * - Code has expired
 * - Parcel already picked up
 */
class InvalidCodeException extends Exception {
    private String code;
    
    public InvalidCodeException(String code, String reason) {
        super("Invalid code '" + code + "': " + reason);
        this.code = code;
    }
    
    public String getCode() { return code; }
}

/**
 * Exception thrown when parcel is not found
 * WHEN TO THROW:
 * - Parcel ID doesn't exist
 */
class ParcelNotFoundException extends Exception {
    private String parcelId;
    
    public ParcelNotFoundException(String parcelId) {
        super("Parcel not found: " + parcelId);
        this.parcelId = parcelId;
    }
    
    public String getParcelId() { return parcelId; }
}

// ===== ENUMS =====

enum CompartmentStatus { 
    FREE,      // Available for new parcel
    OCCUPIED   // Currently holding a parcel
}

enum Size { 
    SMALL, 
    MEDIUM, 
    LARGE;
    
    // Helper method for size comparison
    public boolean canFit(Size other) {
        return this.ordinal() >= other.ordinal();
    }
}

// ===== SUPPORTING CLASSES =====

/**
 * Represents a parcel delivery assignment
 */
class Assignment {
    String code;           // Pickup code (e.g., "CODE-ABC123")
    String parcelId;       // Parcel identifier
    String compartmentId;  // Compartment holding the parcel
    LocalDateTime assignedAt;
    LocalDateTime expiresAt;
    boolean pickedUp;
    
    public Assignment(String code, String parcelId, String compartmentId) {
        this.code = code;
        this.parcelId = parcelId;
        this.compartmentId = compartmentId;
        this.assignedAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusHours(48); // 48-hour pickup window
        this.pickedUp = false;
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt) && !pickedUp;
    }
    
    public long getHoursRemaining() {
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).toHours();
    }
}

/**
 * Represents a storage compartment in the locker
 */
class Compartment {
    String id;
    Size size;
    CompartmentStatus status;
    
    public Compartment(String id, Size size) {
        this.id = id;
        this.size = size;
        this.status = CompartmentStatus.FREE;
    }
    
    @Override
    public String toString() {
        return id + "[" + size + "," + status + "]";
    }
}

/**
 * Represents a parcel to be stored
 */
class Parcel {
    String id;
    Size size;
    String recipientPhone;
    String recipientName;
    
    public Parcel(String id, Size size, String recipientPhone, String recipientName) {
        this.id = id;
        this.size = size;
        this.recipientPhone = recipientPhone;
        this.recipientName = recipientName;
    }
    
    @Override
    public String toString() {
        return "Parcel{" + id + ", " + size + ", to:" + recipientName + "}";
    }
}

/**
 * Amazon Locker System - Low Level Design (LLD)
 * 
 * PROBLEM STATEMENT:
 * Design an Amazon Locker system that can:
 * 1. Allocate parcels to compartments based on size
 * 2. Generate unique pickup codes
 * 3. Handle parcel pickup with code verification
 * 4. Support size-based fallback (use larger compartment if exact size unavailable)
 * 5. Handle expired parcels
 * 6. Track compartment availability
 * 
 * REQUIREMENTS:
 * - Functional: Add parcel, pickup parcel, check availability
 * - Non-Functional: Thread-safe, efficient allocation, handle expiration
 * 
 * INTERVIEW HINTS:
 * - Discuss resource allocation algorithms
 * - Talk about state management (finite state machine)
 * - Mention real-world considerations (physical integration, OTP generation)
 * - Consider notification system integration
 * - Discuss how to handle expired parcels (return to warehouse)
 */
class Locker {
    private String lockerId;
    private String location;
    private List<Compartment> compartments;
    private Map<String, Assignment> assignments;  // code -> assignment
    private Map<String, String> parcelToCode;     // parcelId -> code
    
    public Locker(String lockerId, String location, List<Compartment> compartments) {
        this.lockerId = lockerId;
        this.location = location;
        this.compartments = new ArrayList<>(compartments);
        this.assignments = new HashMap<>();
        this.parcelToCode = new HashMap<>();
    }
    
    /**
     * Add parcel to locker - allocates compartment and generates code
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate parcel is not null
     * 2. Check if parcel already assigned (use parcelToCode)
     * 3. Find free compartment of exact size
     * 4. If not found, try finding larger compartment (size fallback)
     * 5. If still not found, throw NoCompartmentException
     * 6. Mark compartment as OCCUPIED
     * 7. Generate unique pickup code
     * 8. Create Assignment and store in both maps
     * 9. Return the pickup code
     * 
     * INTERVIEW DISCUSSION:
     * - How to optimize compartment search?
     * - Should we prefer exact size or minimize waste?
     * - How to handle priority parcels?
     * 
     * @param parcel The parcel to store
     * @return Pickup code for the parcel
     * @throws NoCompartmentException if no suitable compartment available
     */
    public synchronized String addParcel(Parcel parcel) throws NoCompartmentException {
        // HINT: Use findFreeCompartment(parcel.size) first for exact match
        // HINT: If null, use findLargerCompartment(parcel.size) for fallback
        // HINT: if (compartment == null) throw new NoCompartmentException(parcel.size);
        // HINT: compartment.status = CompartmentStatus.OCCUPIED;
        // HINT: String code = generateCode();
        // HINT: Assignment assignment = new Assignment(code, parcel.id, compartment.id);
        // HINT: assignments.put(code, assignment);
        // HINT: parcelToCode.put(parcel.id, code);
        // HINT: return code;
        Compartment freeCompartment = findFreeCompartment(parcel.size);
        if(freeCompartment == null) freeCompartment = findLargerCompartment(parcel.size);
        if(freeCompartment == null) throw new NoCompartmentException(parcel.size);
        freeCompartment.status = CompartmentStatus.OCCUPIED;
        String code = generateCode();
        Assignment assignment = new Assignment(code, parcel.id, freeCompartment.id);
        assignments.put(code, assignment);
        parcelToCode.put(parcel.id,code);
        return code;
    }
    
    /**
     * Pickup parcel using code
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate code exists in assignments map
     * 2. Get assignment, check if already picked up
     * 3. Check if assignment has expired
     * 4. Find and free the compartment
     * 5. Mark assignment as picked up
     * 6. Return parcel info or throw exception
     * 
     * @param code The pickup code
     * @return Assignment details
     * @throws InvalidCodeException if code invalid, expired, or already used
     */
    public synchronized Assignment pickupParcel(String code) throws InvalidCodeException {
        // HINT: Assignment assignment = assignments.get(code);
        // HINT: if (assignment == null) throw new InvalidCodeException(code, "Code not found");
        // HINT: if (assignment.pickedUp) throw new InvalidCodeException(code, "Already picked up");
        // HINT: if (assignment.isExpired()) throw new InvalidCodeException(code, "Code expired");
        // HINT: Compartment comp = findCompartmentById(assignment.compartmentId);
        // HINT: comp.status = CompartmentStatus.FREE;
        // HINT: assignment.pickedUp = true
        // HINT: return assignment;
        Assignment assignment = assignments.get(code);
        if(assignment == null) throw new InvalidCodeException(code, "Code not found");;
        if(assignment.pickedUp) throw new InvalidCodeException(code, "Already picked up");
        if(assignment.isExpired()) throw new InvalidCodeException(code, "Code expired");
        Compartment compartment = findCompartmentById(assignment.compartmentId);
        compartment.status=CompartmentStatus.FREE;
        assignment.pickedUp=true;
        return assignment;
    }
    
    /**
     * Find free compartment of exact size
     * 
     * IMPLEMENTATION HINTS:
     * 1. Stream through compartments
     * 2. Filter by: status == FREE && size == requestedSize
     * 3. Return first match or null
     * 
     * @param size Required size
     * @return Free compartment or null
     */
    private Compartment findFreeCompartment(Size size) {
        // HINT: return compartments.stream()
        //           .filter(c -> c.status == CompartmentStatus.FREE && c.size == size)
        //           .findFirst()
        //           .orElse(null);
        return compartments.stream().filter(c->c.status==CompartmentStatus.FREE && c.size==size).findFirst().orElse(null);
    }
    
    /**
     * Find free compartment larger than requested size
     * 
     * IMPLEMENTATION HINTS:
     * 1. Stream through compartments
     * 2. Filter by: status == FREE && size.ordinal() > requestedSize.ordinal()
     * 3. Sort by size (ascending) to get smallest larger compartment
     * 4. Return first match or null
     * 
     * @param size Minimum size needed
     * @return Free larger compartment or null
     */
    private Compartment findLargerCompartment(Size size) {
        // HINT: Use size.ordinal() comparison
        // HINT: Sort by .comparing(c -> c.size.ordinal())
        // HINT: Return smallest available larger size
        return  compartments.stream().filter(c->c.status==CompartmentStatus.FREE && c.size.ordinal() > size.ordinal())
        .sorted(Comparator.comparingInt(c -> c.size.ordinal())).findFirst().orElse(null);:
    }
    
    /**
     * Find compartment by ID
     * 
     * @param compartmentId Compartment ID
     * @return Compartment or null
     */
    private Compartment findCompartmentById(String compartmentId) {
        // HINT: compartments.stream().filter(c -> c.id.equals(compartmentId)).findFirst().orElse(null);
        return compartments.stream().filter(c->c.id.equals(compartmentId)).findFirst().orElse(null);
    }
    
    /**
     * Generate unique pickup code
     * 
     * IMPLEMENTATION HINTS:
     * 1. Use UUID for uniqueness
     * 2. Format as "CODE-XXXXXX" (6 chars)
     * 3. Ensure code doesn't already exist (collision check)
     * 
     * @return Unique pickup code
     */
    private String generateCode() {
        // HINT: String code = "CODE-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        // HINT: Optional: Check if code exists in assignments and regenerate if collision
        for(int i=0;i<10;i++){
            String code="CODE-"+UUID.randomUUID().toString().substring(0,6).toUpperCase();
            if(assignments.containsKey(code)) continue;
            return code;
        }
        return null;
    }
    
    /**
     * Get available compartments by size
     * 
     * @param size Size to check
     * @return Count of free compartments
     */
    public long getAvailableCount(Size size) {
        // HINT: return compartments.stream()
        //           .filter(c -> c.status == CompartmentStatus.FREE && c.size == size)
        //           .count();
        return compartments.stream().filter(c->c.status==CompartmentStatus.FREE && c.size==size).count();
    }
    
    /**
     * Clean up expired assignments
     * 
     * IMPLEMENTATION HINTS:
     * 1. Find all expired assignments
     * 2. Free their compartments
     * 3. Remove from assignments map
     * 4. In real system: notify customer, return parcel to warehouse
     * 
     * @return Number of expired assignments cleaned
     */
    public synchronized int cleanupExpired() {
        // HINT: assignments.values().stream().filter(Assignment::isExpired)
        // HINT: For each expired: free compartment, remove from maps
        List<String> expiredKeys=assignments.entrySet().stream().filter(x->x.getValue().isExpired()).map(Map.Entry::getKey).collect(Collectors.toList());
        for(String key:expiredKeys){
            Assignment expired=assignments.remove(key);
            if(expired!=null){
                findCompartmentById(expired.compartmentId).status=CompartmentStatus.FREE;
            }
        }
        return expiredKeys.size();
    }
    
    /**
     * Display locker status
     */
    public void displayStatus() {
        System.out.println("\n--- Locker: " + lockerId + " at " + location + " ---");
        System.out.println("Total Compartments: " + compartments.size());
        for (Size size : Size.values()) {
            long free = compartments.stream()
                .filter(c -> c.status == CompartmentStatus.FREE && c.size == size)
                .count();
            long total = compartments.stream()
                .filter(c -> c.size == size)
                .count();
            System.out.println("  " + size + ": " + free + "/" + total + " free");
        }
        System.out.println("Active assignments: " + assignments.size());
    }
}

// ===== MAIN TEST CLASS =====

public class AmazonLocker {
    public static void main(String[] args) {
        System.out.println("=== Amazon Locker System Test Cases ===\n");
        
        // Create locker with compartments (enough for all test cases)
        List<Compartment> compartments = Arrays.asList(
            new Compartment("S1", Size.SMALL),
            new Compartment("S2", Size.SMALL),
            new Compartment("S3", Size.SMALL),
            new Compartment("M1", Size.MEDIUM),
            new Compartment("M2", Size.MEDIUM),
            new Compartment("M3", Size.MEDIUM),
            new Compartment("L1", Size.LARGE),
            new Compartment("L2", Size.LARGE)
        );
        
        Locker locker = new Locker("LOCKER-001", "Seattle Downtown", compartments);
        locker.displayStatus();
        
        // Test Case 1: Basic Parcel Delivery
        System.out.println("\n=== Test Case 1: Basic Parcel Delivery ===");
        try {
            Parcel p1 = new Parcel("PKG-001", Size.SMALL, "+1-555-0001", "Alice");
            String code1 = locker.addParcel(p1);
            System.out.println("✓ Parcel added: " + p1);
            System.out.println("  Pickup code: " + code1);
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        
        // Test Case 2: Multiple Parcels
        System.out.println("\n=== Test Case 2: Multiple Parcels ===");
        try {
            Parcel p2 = new Parcel("PKG-002", Size.MEDIUM, "+1-555-0002", "Bob");
            Parcel p3 = new Parcel("PKG-003", Size.LARGE, "+1-555-0003", "Charlie");
            
            String code2 = locker.addParcel(p2);
            String code3 = locker.addParcel(p3);
            
            System.out.println("✓ Added 2 more parcels");
            System.out.println("  Codes: " + code2 + ", " + code3);
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        
        locker.displayStatus();
        
        // Test Case 3: Successful Pickup
        System.out.println("\n=== Test Case 3: Successful Pickup ===");
        try {
            Parcel p4 = new Parcel("PKG-004", Size.SMALL, "+1-555-0004", "David");
            String code = locker.addParcel(p4);
            
            Assignment assignment = locker.pickupParcel(code);
            System.out.println("✓ Parcel picked up successfully");
            System.out.println("  Parcel ID: " + assignment.parcelId);
            System.out.println("  Picked up: " + assignment.pickedUp);
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        
        locker.displayStatus();
        
        // Test Case 4: Size Fallback (use larger compartment)
        System.out.println("\n=== Test Case 4: Size Fallback ===");
        try {
            // Fill small compartments first
            locker.addParcel(new Parcel("PKG-S1", Size.SMALL, "+1-555-1001", "User1"));
            locker.addParcel(new Parcel("PKG-S2", Size.SMALL, "+1-555-1002", "User2"));
            
            // This should use MEDIUM compartment as fallback
            String code = locker.addParcel(new Parcel("PKG-S3", Size.SMALL, "+1-555-1003", "User3"));
            System.out.println("✓ Small parcel assigned to larger compartment");
            System.out.println("  Code: " + code);
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        
        locker.displayStatus();
        
        // Test Case 5: Check Availability
        System.out.println("\n=== Test Case 5: Check Availability ===");
        System.out.println("SMALL available: " + locker.getAvailableCount(Size.SMALL));
        System.out.println("MEDIUM available: " + locker.getAvailableCount(Size.MEDIUM));
        System.out.println("LARGE available: " + locker.getAvailableCount(Size.LARGE));
        System.out.println();
        
        // ===== EXCEPTION TEST CASES =====
        
        // Test Case 6: Exception - No Compartment Available
        System.out.println("=== Test Case 6: Exception - No Compartment ===");
        try {
            // Try to add when all compartments full
            for (int i = 0; i < 10; i++) {
                try {
                    locker.addParcel(new Parcel("PKG-FILL-" + i, Size.LARGE, "+1-555-" + i, "User" + i));
                } catch (NoCompartmentException e) {
                    System.out.println("✓ Caught expected exception: " + e.getMessage());
                    System.out.println("  Requested size: " + e.getRequestedSize());
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 7: Exception - Invalid Code
        System.out.println("=== Test Case 7: Exception - Invalid Code ===");
        try {
            locker.pickupParcel("INVALID-CODE");
            System.out.println("✗ Should have thrown InvalidCodeException");
        } catch (InvalidCodeException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
            System.out.println("  Invalid code: " + e.getCode());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 8: Exception - Already Picked Up
        System.out.println("=== Test Case 8: Exception - Already Picked Up ===");
        try {
            Parcel p = new Parcel("PKG-DUP", Size.MEDIUM, "+1-555-9999", "TestUser");
            String code = locker.addParcel(p);
            locker.pickupParcel(code);  // First pickup - OK
            locker.pickupParcel(code);  // Second pickup - should fail
            System.out.println("✗ Should have thrown InvalidCodeException");
        } catch (InvalidCodeException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        locker.displayStatus();
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION TOPICS:
 * ============================
 * 
 * 1. RESOURCE ALLOCATION:
 *    - Best fit vs First fit vs Worst fit
 *    - Should we defragment (merge small parcels)?
 *    - How to handle priority deliveries?
 *    - Pre-reservation for scheduled deliveries
 * 
 * 2. STATE MANAGEMENT:
 *    - Finite State Machine: FREE → OCCUPIED → FREE
 *    - Transition rules and validation
 *    - What happens during power outage?
 * 
 * 3. SECURITY:
 *    - OTP generation (SMS/Email)
 *    - Code expiration (48 hours)
 *    - Failed pickup attempts (lock after N tries)
 *    - CCTV integration
 * 
 * 4. SCALABILITY:
 *    - Database: Store compartment state persistently
 *    - Distributed lockers: Location-based routing
 *    - Real-time availability updates
 *    - Integration with delivery tracking system
 * 
 * 5. REAL-WORLD CONSIDERATIONS:
 *    - Physical integration (IoT devices, door locks)
 *    - Network failures (offline mode, sync when back online)
 *    - Customer notifications (SMS, app push)
 *    - Return-to-warehouse workflow for expired parcels
 *    - Access logging and audit trails
 * 
 * 6. ADVANCED FEATURES:
 *    - Temperature-controlled compartments (food, medicine)
 *    - Parcel tracking (delivered → in locker → picked up)
 *    - Multiple pickup attempts with OTP
 *    - Extended storage (pay for extra days)
 *    - Compartment cleaning status
 *    - Load balancing across nearby lockers
 * 
 * 7. DESIGN PATTERNS:
 *    - State Pattern: Compartment states
 *    - Strategy Pattern: Allocation algorithms
 *    - Observer Pattern: Notification on delivery/pickup
 *    - Factory Pattern: Different locker types
 * 
 * 8. API DESIGN:
 *    POST /lockers/{lockerId}/parcels    - Add parcel
 *    POST /lockers/{lockerId}/pickup     - Pickup with code
 *    GET  /lockers/{lockerId}/availability - Check free compartments
 *    GET  /lockers/nearby?lat=X&lng=Y    - Find nearby lockers
 *    POST /parcels/{id}/extend           - Extend pickup window
 */
