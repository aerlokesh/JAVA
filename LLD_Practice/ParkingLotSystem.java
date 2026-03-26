import java.util.*;
import java.util.concurrent.atomic.*;

// ===== CUSTOM EXCEPTION CLASSES =====

/**
 * Exception thrown when parking lot is full for a vehicle type
 * WHEN TO THROW:
 * - No available spot matching vehicle type on any floor
 */
class ParkingFullException extends Exception {
    private String vehicleType;
    public ParkingFullException(String vehicleType) {
        super("No available spot for vehicle type: " + vehicleType);
        this.vehicleType = vehicleType;
    }
    public String getVehicleType() { return vehicleType; }
}

/**
 * Exception thrown when a vehicle is not found in the lot
 * WHEN TO THROW:
 * - unpark() called with a license plate not in the lot
 * - Ticket lookup fails
 */
class VehicleNotFoundException extends Exception {
    private String licensePlate;
    public VehicleNotFoundException(String licensePlate) {
        super("Vehicle not found: " + licensePlate);
        this.licensePlate = licensePlate;
    }
    public String getLicensePlate() { return licensePlate; }
}

/**
 * Exception thrown when vehicle data is invalid
 * WHEN TO THROW:
 * - License plate is null/empty
 * - Vehicle type is null
 */
class InvalidVehicleException extends Exception {
    public InvalidVehicleException(String message) { super(message); }
}

// ===== ENUMS =====

/**
 * Vehicle types — determines which spot size is needed
 * MOTORCYCLE → COMPACT spot
 * CAR → REGULAR spot
 * TRUCK/BUS → LARGE spot
 */
enum VehicleType {
    MOTORCYCLE, CAR, TRUCK, BUS
}

/**
 * Spot sizes — ordered smallest to largest
 * COMPACT < REGULAR < LARGE
 * A vehicle can park in its size OR larger (motorcycle in any spot)
 */
enum SpotType {
    COMPACT, REGULAR, LARGE
}

// ===== DOMAIN CLASSES =====

/**
 * Vehicle — identified by license plate
 * 
 * KEY INTERVIEW POINTS:
 * - License plate is unique identifier
 * - VehicleType determines compatible SpotType
 */
class Vehicle {
    String licensePlate;
    VehicleType type;

    public Vehicle(String licensePlate, VehicleType type) {
        this.licensePlate = licensePlate;
        this.type = type;
    }

    @Override
    public String toString() {
        return type + "[" + licensePlate + "]";
    }
}

/**
 * Parking Spot — a single space on a floor
 * 
 * KEY INTERVIEW POINTS:
 * - Each spot has a type (COMPACT/REGULAR/LARGE)
 * - Spot can be available or occupied
 * - A vehicle can fit in its matching or larger spot
 */
class ParkingSpot {
    String spotId;
    SpotType type;
    int floor;
    Vehicle currentVehicle;

    public ParkingSpot(String spotId, SpotType type, int floor) {
        this.spotId = spotId;
        this.type = type;
        this.floor = floor;
    }

    public boolean isAvailable() { return currentVehicle == null; }

    /**
     * Can this spot fit the given vehicle type?
     * HINT: MOTORCYCLE fits anywhere, CAR fits REGULAR+LARGE, TRUCK/BUS needs LARGE
     */
    public boolean canFit(VehicleType vehicleType) {
        // HINT: switch on vehicleType:
        //   MOTORCYCLE → any spot (COMPACT, REGULAR, LARGE) → return true
        //   CAR → REGULAR or LARGE → return type == SpotType.REGULAR || type == SpotType.LARGE
        //   TRUCK, BUS → LARGE only → return type == SpotType.LARGE
        //   default → return false
        return switch (vehicleType) {
            case MOTORCYCLE -> true;
            case CAR -> type==SpotType.REGULAR || type==SpotType.LARGE;
            case TRUCK, BUS -> type==SpotType.LARGE;
            default -> false;
        };
    }

    public void park(Vehicle vehicle) { this.currentVehicle = vehicle; }
    public void unpark() { this.currentVehicle = null; }

    @Override
    public String toString() {
        return String.format("F%d-%s(%s)%s", floor, spotId, type,
            isAvailable() ? "[FREE]" : "[" + currentVehicle + "]");
    }
}

/**
 * Parking Ticket — issued when vehicle enters
 * 
 * KEY INTERVIEW POINTS:
 * - Ticket links vehicle → spot → time
 * - Entry time recorded for fee calculation
 * - Ticket ID is unique (atomic counter or UUID)
 */
class ParkingTicket {
    String ticketId;
    Vehicle vehicle;
    ParkingSpot spot;
    long entryTime;
    long exitTime;
    double fee;

    public ParkingTicket(String ticketId, Vehicle vehicle, ParkingSpot spot) {
        this.ticketId = ticketId;
        this.vehicle = vehicle;
        this.spot = spot;
        this.entryTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return String.format("Ticket[%s] %s → %s", ticketId, vehicle, spot.spotId);
    }
}

// ===== PARKING LOT SERVICE =====

/**
 * Parking Lot System - Low Level Design (LLD)
 * 
 * PROBLEM: Design a multi-floor parking lot that handles:
 * - Multiple vehicle types (motorcycle, car, truck)
 * - Multiple spot sizes (compact, regular, large)
 * - Park/unpark with ticket generation
 * - Fee calculation based on duration + vehicle type
 * 
 * SPOT ASSIGNMENT STRATEGY:
 *   Find first available spot that fits vehicle (smallest sufficient spot first)
 *   Floor 1 → Floor N, within floor: COMPACT → REGULAR → LARGE
 * 
 * KEY PATTERNS:
 * - Strategy Pattern: Fee calculation (hourly, flat, dynamic)
 * - Singleton Pattern: ParkingLot instance (one lot)
 * - Observer Pattern: Notify when lot full / spot freed
 * 
 * KEY INTERVIEW TOPICS:
 * - Concurrency: Multiple entry/exit gates → synchronized spot assignment
 * - Spot selection: Best-fit vs first-fit
 * - Fee strategies: Hourly, flat-rate, peak pricing
 * - Scalability: Distributed parking across multiple lots
 */
class ParkingLotService {
    private String name;
    private int numFloors;
    private List<List<ParkingSpot>> floors;          // floor index → list of spots
    private Map<String, ParkingTicket> activeTickets; // licensePlate → ticket
    private AtomicInteger ticketCounter;
    private Map<VehicleType, Double> hourlyRates;

    public ParkingLotService(String name, int numFloors, int compactPerFloor, int regularPerFloor, int largePerFloor) {
        this.name = name;
        this.numFloors = numFloors;
        this.floors = new ArrayList<>();
        this.activeTickets = new HashMap<>();
        this.ticketCounter = new AtomicInteger(1);

        this.hourlyRates = new EnumMap<>(VehicleType.class);
        hourlyRates.put(VehicleType.MOTORCYCLE, 10.0);
        hourlyRates.put(VehicleType.CAR, 20.0);
        hourlyRates.put(VehicleType.TRUCK, 30.0);
        hourlyRates.put(VehicleType.BUS, 40.0);

        initializeFloors(compactPerFloor, regularPerFloor, largePerFloor);
    }

    /**
     * Initialize parking floors with spots
     * 
     * HINTS:
     * 1. Loop through numFloors (1 to numFloors)
     * 2. For each floor, create compact, regular, large spots
     * 3. Spot ID format: "F{floor}-{type}{number}" e.g., "F1-C01", "F1-R01", "F1-L01"
     * 4. Add all spots for a floor to a list, then add that list to floors
     */
    private void initializeFloors(int compact, int regular, int large) {
        // HINT: for (int f = 1; f <= numFloors; f++) {
        //     List<ParkingSpot> floorSpots = new ArrayList<>();
        //     for (int i = 1; i <= compact; i++)
        //         floorSpots.add(new ParkingSpot(String.format("F%d-C%02d", f, i), SpotType.COMPACT, f));
        //     for (int i = 1; i <= regular; i++)
        //         floorSpots.add(new ParkingSpot(String.format("F%d-R%02d", f, i), SpotType.REGULAR, f));
        //     for (int i = 1; i <= large; i++)
        //         floorSpots.add(new ParkingSpot(String.format("F%d-L%02d", f, i), SpotType.LARGE, f));
        //     floors.add(floorSpots);
        // }
        for(int f=1;f<=numFloors;f++){
            List<ParkingSpot> l=new ArrayList<>();
            for(int i=0;i<compact;i++) l.add(new ParkingSpot(String.format("F%d-C%d",f,i), SpotType.COMPACT, f));
            for(int i=0;i<regular;i++) l.add(new ParkingSpot(String.format("F%d-C%d",f,i), SpotType.COMPACT, f));
            for(int i=0;i<large;i++) l.add(new ParkingSpot(String.format("F%d-C%d",f,i), SpotType.COMPACT, f));
            floors.add(l);
        }
    }

    // ===== CORE OPERATIONS =====

    /**
     * Park a vehicle in the lot
     * 
     * HINTS:
     * 1. Validate vehicle (not null, valid plate) → throw InvalidVehicleException
     * 2. Check if already parked → throw InvalidVehicleException("already parked")
     * 3. Find available spot using findAvailableSpot() → throw ParkingFullException if none
     * 4. Occupy the spot, create ticket, store in maps
     * 5. Return the ticket
     * 
     * INTERVIEW: Discuss synchronized/lock for concurrent gate entry
     */
    public ParkingTicket park(Vehicle vehicle) throws InvalidVehicleException, ParkingFullException {
        // HINT: if (vehicle == null || vehicle.licensePlate == null || vehicle.licensePlate.isEmpty())
        //     throw new InvalidVehicleException("Vehicle or license plate is invalid");
        // HINT: if (activeTickets.containsKey(vehicle.licensePlate))
        //     throw new InvalidVehicleException("Vehicle already parked: " + vehicle.licensePlate);
        // HINT: ParkingSpot spot = findAvailableSpot(vehicle.type);
        // HINT: if (spot == null) throw new ParkingFullException(vehicle.type.name());
        // HINT: spot.park(vehicle);
        // HINT: String ticketId = "T-" + ticketCounter.getAndIncrement();
        // HINT: ParkingTicket ticket = new ParkingTicket(ticketId, vehicle, spot);
        // HINT: activeTickets.put(vehicle.licensePlate, ticket);
        // HINT: ticketById.put(ticketId, ticket);
        // HINT: return ticket;
        if(vehicle==null || vehicle.licensePlate==null || vehicle.licensePlate.isEmpty()) throw new InvalidVehicleException("Vehicle or license plate invalid");
        if(activeTickets.containsKey(vehicle.licensePlate)) throw new InvalidVehicleException("Vehicle already parked");
        ParkingSpot spot=findAvailableSpot(vehicle.type);
        if(spot==null) throw new ParkingFullException(vehicle.type.name());
        spot.park(vehicle);
        String ticketId="T-"+ticketCounter.incrementAndGet();
        ParkingTicket ticket=new ParkingTicket(ticketId, vehicle, spot);
        activeTickets.put(vehicle.licensePlate, ticket);
        return ticket;
    }

    /**
     * Unpark a vehicle and calculate fee
     * 
     * HINTS:
     * 1. Look up ticket by license plate → throw VehicleNotFoundException if absent
     * 2. Set exit time, calculate fee based on duration and vehicle type
     * 3. Free the spot (spot.unpark()), remove from active tickets
     * 4. Return the ticket with fee info
     * 
     * INTERVIEW: Fee = ceil(hours) * hourlyRate[vehicleType]
     */
    public ParkingTicket unpark(String licensePlate) throws VehicleNotFoundException {
        // HINT: ParkingTicket ticket = activeTickets.get(licensePlate);
        // HINT: if (ticket == null) throw new VehicleNotFoundException(licensePlate);
        // HINT: ticket.exitTime = System.currentTimeMillis();
        // HINT: ticket.fee = calculateFee(ticket);
        // HINT: ticket.spot.unpark();
        // HINT: activeTickets.remove(licensePlate);
        // HINT: return ticket;
        ParkingTicket ticket=activeTickets.get(licensePlate);
        if(ticket==null) throw new VehicleNotFoundException(licensePlate);
        ticket.exitTime=System.currentTimeMillis();
        ticket.fee=calculateFee(ticket);
        ticket.spot.unpark();
        activeTickets.remove(licensePlate);
        return ticket;
    }

    /**
     * Find an available spot for a vehicle type
     * 
     * HINTS:
     * 1. Iterate floors (1 → N), then spots within floor
     * 2. Check: spot.isAvailable() && spot.canFit(vehicleType)
     * 3. Return first match (greedy/first-fit approach)
     * 4. Return null if none found
     * 
     * INTERVIEW: Best-fit would find smallest sufficient spot.
     * This implementation IS best-fit because spots are ordered COMPACT→REGULAR→LARGE
     */
    private ParkingSpot findAvailableSpot(VehicleType vehicleType) {
        // HINT: for (List<ParkingSpot> floor : floors) {
        //     for (ParkingSpot spot : floor) {
        //         if (spot.isAvailable() && spot.canFit(vehicleType)) return spot;
        //     }
        // }
        // HINT: return null;
        
        return floors.stream().flatMap(List::stream).filter(ParkingSpot::isAvailable).filter(spot->spot.canFit(vehicleType)).findFirst().orElse(null);
    }

    /**
     * Calculate parking fee
     * 
     * HINTS:
     * 1. Duration = exitTime - entryTime (milliseconds)
     * 2. Convert to hours (ceiling → minimum 1 hour)
     * 3. Fee = hours * hourlyRate for vehicle type
     * 
     * INTERVIEW: Discuss Strategy Pattern for different fee schemes
     * (flat-rate, hourly, dynamic/peak pricing, first-hour-free, etc.)
     */
    private double calculateFee(ParkingTicket ticket) {
        // HINT: long durationMs = ticket.exitTime - ticket.entryTime;
        // HINT: int hours = (int) Math.ceil(durationMs / 3600000.0);
        // HINT: hours = Math.max(1, hours); // minimum 1 hour charge
        // HINT: return hours * hourlyRates.get(ticket.vehicle.type);
        return Math.max((int)(Math.ceil(ticket.exitTime-ticket.entryTime)/60*60*1000),1) * hourlyRates.get(ticket.vehicle.type);
    }

    /**
     * Check if lot is full for a given vehicle type
     */
    public boolean isFull(VehicleType vehicleType) {
        // HINT: return findAvailableSpot(vehicleType) == null;
        return findAvailableSpot(vehicleType) == null;
    }

}

// ===== MAIN TEST CLASS =====

public class ParkingLotSystem {
    public static void main(String[] args) {
        System.out.println("=== Parking Lot System Test Cases ===\n");

        // Setup: 2 floors, 3 compact, 3 regular, 2 large per floor
        ParkingLotService lot = new ParkingLotService("City Mall Parking", 2, 3, 3, 2);
        System.out.println("Parking lot created: 2 floors, 3C+3R+2L per floor\n");

        // Test 1: Park a car
        System.out.println("=== Test 1: Park a Car ===");
        try {
            Vehicle car1 = new Vehicle("KA-01-1234", VehicleType.CAR);
            ParkingTicket ticket1 = lot.park(car1);
            System.out.println("✓ Parked: " + ticket1);
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();

        // Test 2: Park a motorcycle (should get compact spot)
        System.out.println("=== Test 2: Park a Motorcycle ===");
        try {
            Vehicle bike1 = new Vehicle("KA-02-5678", VehicleType.MOTORCYCLE);
            ParkingTicket ticket2 = lot.park(bike1);
            System.out.println("✓ Parked: " + ticket2);
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();

        // Test 3: Park a truck (needs large spot)
        System.out.println("=== Test 3: Park a Truck ===");
        try {
            Vehicle truck1 = new Vehicle("KA-03-9999", VehicleType.TRUCK);
            ParkingTicket ticket3 = lot.park(truck1);
            System.out.println("✓ Parked: " + ticket3);
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();

        // Test 4: Unpark and check fee
        System.out.println("=== Test 4: Unpark Car (fee check) ===");
        try {
            ParkingTicket exitTicket = lot.unpark("KA-01-1234");
            System.out.println("✓ Unparked: " + exitTicket.vehicle);
            System.out.println("  Spot freed: " + exitTicket.spot.spotId);
            System.out.println("  Fee: $" + String.format("%.2f", exitTicket.fee) + " (min 1 hour)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();

        // Test 5: Park multiple cars, check spot assignment
        System.out.println("=== Test 5: Park Multiple Cars ===");
        try {
            String[] plates = {"MH-01-1111", "MH-02-2222", "MH-03-3333", "MH-04-4444", "MH-05-5555", "MH-06-6666"};
            for (String plate : plates) {
                ParkingTicket t = lot.park(new Vehicle(plate, VehicleType.CAR));
                System.out.println("  Parked " + plate + " → " + t.spot.spotId);
            }
            System.out.println("✓ All 6 cars parked");
        } catch (Exception e) {
            System.out.println("  " + e.getMessage());
        }
        System.out.println();

        // Test 6: Try parking when full → ParkingFullException
        System.out.println("=== Test 6: Parking Full ===");
        try {
            // Fill remaining large spots with trucks
            lot.park(new Vehicle("TK-01-0001", VehicleType.TRUCK));
            lot.park(new Vehicle("TK-02-0002", VehicleType.TRUCK));
            lot.park(new Vehicle("TK-03-0003", VehicleType.TRUCK));
            // Now try one more truck
            lot.park(new Vehicle("TK-04-0004", VehicleType.TRUCK));
            System.out.println("✗ Should have thrown ParkingFullException");
        } catch (ParkingFullException e) {
            System.out.println("✓ " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong: " + e.getMessage());
        }
        System.out.println();

        // Test 7: Unpark vehicle not in lot → VehicleNotFoundException
        System.out.println("=== Test 7: Vehicle Not Found ===");
        try {
            lot.unpark("XX-99-0000");
            System.out.println("✗ Should have thrown VehicleNotFoundException");
        } catch (VehicleNotFoundException e) {
            System.out.println("✓ " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong: " + e.getMessage());
        }
        System.out.println();

        // Test 8: Park same vehicle twice → InvalidVehicleException
        System.out.println("=== Test 8: Duplicate Vehicle ===");
        try {
            lot.park(new Vehicle("KA-02-5678", VehicleType.MOTORCYCLE)); // already parked
            System.out.println("✗ Should have thrown InvalidVehicleException");
        } catch (InvalidVehicleException e) {
            System.out.println("✓ " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong: " + e.getMessage());
        }
        System.out.println();

        // Test 9: Invalid vehicle (null plate)
        System.out.println("=== Test 9: Invalid Vehicle ===");
        try {
            lot.park(new Vehicle(null, VehicleType.CAR));
            System.out.println("✗ Should have thrown InvalidVehicleException");
        } catch (InvalidVehicleException e) {
            System.out.println("✓ " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong: " + e.getMessage());
        }
        System.out.println();

        // Test 10: Unpark and re-park (spot reuse)
        System.out.println("=== Test 10: Spot Reuse After Unpark ===");
        try {
            ParkingTicket exitTicket = lot.unpark("MH-01-1111");
            System.out.println("  Freed spot: " + exitTicket.spot.spotId);
            ParkingTicket newTicket = lot.park(new Vehicle("NEW-01-0001", VehicleType.CAR));
            System.out.println("  New car parked at: " + newTicket.spot.spotId);
            System.out.println("✓ Spot reused: " + exitTicket.spot.spotId.equals(newTicket.spot.spotId));
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();

        System.out.println("=== All Tests Complete! ===");
    }
}

/**
 * ============================================
 * INTERVIEW CHEAT SHEET (discuss, don't code)
 * ============================================
 * 
 * 1. DESIGN PATTERNS:
 *    - Strategy Pattern: Fee calculation (HourlyFee, FlatFee, DynamicFee implement FeeStrategy)
 *    - Singleton: ParkingLot (one lot per building)
 *    - Observer: Notify display boards when spots change
 *    - Factory: VehicleFactory to create vehicles from type string
 * 
 * 2. SPOT ASSIGNMENT:
 *    First-fit: Iterate floors/spots, assign first match (used here)
 *    Best-fit: Find smallest spot that fits (also achieved here since spots ordered by size)
 *    Nearest-entry: Assign closest to entrance (use BFS/priority queue)
 *    Load-balanced: Distribute evenly across floors
 * 
 * 3. CONCURRENCY (mention):
 *    - Multiple entry/exit gates → concurrent park/unpark
 *    - synchronized block on spot assignment (or ReentrantLock)
 *    - ConcurrentHashMap for activeTickets
 *    - AtomicInteger for ticket counter (used here)
 *    - Optimistic locking: CAS on spot status
 * 
 * 4. FEE STRATEGIES (Strategy Pattern):
 *    interface FeeStrategy { double calculate(ParkingTicket ticket); }
 *    - HourlyFee: hours * rate (used here)
 *    - FlatFee: fixed amount per entry
 *    - DynamicFee: higher rate during peak hours
 *    - FirstHourFree: first hour free, then hourly
 *    - WeekendRate: different rates on weekends
 * 
 * 5. DATABASE DESIGN:
 *    Tables: vehicles, parking_spots, tickets, floors, fee_config
 *    - spots: id, floor_id, type, status (available/occupied)
 *    - tickets: id, vehicle_id, spot_id, entry_time, exit_time, fee
 *    - Indexes on: spot(status, type), ticket(vehicle_id, exit_time IS NULL)
 * 
 * 6. EXTENSIONS:
 *    - EV charging spots (special spot type + charging status)
 *    - Handicap spots (reserved, nearest to elevator)
 *    - Valet parking (queue-based, stack cars)
 *    - Reservation system (pre-book spot for time window)
 *    - Display boards per floor (Observer pattern)
 *    - Payment integration (pay at exit gate or machine)
 *    - License plate recognition (entry/exit automation)
 * 
 * 7. CAPACITY & SCALABILITY:
 *    - Multi-building: ParkingLotService per building, central registry
 *    - Real-time availability: Redis for spot counts
 *    - Analytics: Peak hours, avg duration, revenue per floor
 *    - Sensor integration: IoT sensors detect occupied/free
 * 
 * 8. API DESIGN:
 *    POST   /park              - Park vehicle, returns ticket
 *    POST   /unpark/{plate}    - Unpark, returns fee
 *    GET    /availability      - Available spots by type
 *    GET    /floors/{id}/spots - Floor-level spot details
 *    GET    /ticket/{id}       - Ticket lookup
 *    POST   /reserve           - Reserve spot (bonus)
 */
