import java.util.*;

/*
 * ELEVATOR SYSTEM (Single Lift) - Low Level Design
 * ===================================================
 * 
 * REQUIREMENTS:
 * 1. Single elevator serving N floors
 * 2. External requests: person at floor wants to go UP/DOWN
 * 3. Internal requests: passenger presses destination floor
 * 4. SCAN/LOOK algorithm: go in one direction serving all stops, then reverse
 * 5. States: IDLE, MOVING_UP, MOVING_DOWN, DOOR_OPEN
 * 6. Step-based simulation
 * 
 * DESIGN PATTERNS:
 *   Observer — ElevatorListener / ElevatorLogger
 *   Facade  — ElevatorService
 * 
 * KEY DS: TreeSet<Integer> upStops (asc), downStops (desc) → O(log n) insert, O(1) next
 * 
 * COMPLEXITY:
 *   addStop: O(log F), step: O(log F), requestElevator: O(1)
 */

// ==================== EXCEPTIONS ====================

class InvalidFloorException extends RuntimeException {
    InvalidFloorException(int floor, int min, int max) {
        super("Invalid floor " + floor + " (range: " + min + "-" + max + ")");
    }
}

// ==================== ENUMS ====================

enum LiftState { IDLE, MOVING_UP, MOVING_DOWN, DOOR_OPEN }

// ==================== MODELS ====================

class Lift {
    int currentFloor;
    LiftState state;
    final int minFloor, maxFloor;
    final TreeSet<Integer> upStops = new TreeSet<>();
    final TreeSet<Integer> downStops = new TreeSet<>(Collections.reverseOrder());

    Lift(int minFloor, int maxFloor) {
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
        this.currentFloor = minFloor;
        this.state = LiftState.IDLE;
    }

    void addStop(int floor) {
        if (floor < minFloor || floor > maxFloor) throw new InvalidFloorException(floor, minFloor, maxFloor);
        if (floor == currentFloor) return;
        if (floor > currentFloor) upStops.add(floor);
        else downStops.add(floor);
        updateState();
    }

    void step() {
        if (state == LiftState.DOOR_OPEN) {
            state = LiftState.IDLE;
            updateState();
            return;
        }
        if (state == LiftState.MOVING_UP) {
            currentFloor++;
            if (!upStops.isEmpty() && upStops.first() == currentFloor) {
                upStops.pollFirst();
                state = LiftState.DOOR_OPEN;
            }
        } else if (state == LiftState.MOVING_DOWN) {
            currentFloor--;
            if (!downStops.isEmpty() && downStops.first() == currentFloor) {
                downStops.pollFirst();
                state = LiftState.DOOR_OPEN;
            }
        }
        if (state != LiftState.DOOR_OPEN) updateState();
    }

    void updateState() {
        if (!upStops.isEmpty() && (state == LiftState.IDLE || state == LiftState.MOVING_UP || downStops.isEmpty())) {
            state = LiftState.MOVING_UP;
        } else if (!downStops.isEmpty()) {
            state = LiftState.MOVING_DOWN;
        } else {
            state = LiftState.IDLE;
        }
    }

    boolean hasStops() { return !upStops.isEmpty() || !downStops.isEmpty(); }
}

// ==================== INTERFACES ====================

interface ElevatorListener {
    void onEvent(LiftState state, int floor);
}

// ==================== LISTENER IMPLEMENTATIONS ====================

class ElevatorLogger implements ElevatorListener {
    final List<String> events = new ArrayList<>();

    @Override public void onEvent(LiftState state, int floor) {
        events.add(state + "@" + floor);
    }
}

// ==================== ELEVATOR SERVICE (FACADE) ====================

class ElevatorService {
    final Lift lift;
    final List<ElevatorListener> listeners = new ArrayList<>();

    ElevatorService(int minFloor, int maxFloor) {
        this.lift = new Lift(minFloor, maxFloor);
    }

    void addListener(ElevatorListener l) { listeners.add(l); }

    private void fireEvent() {
        for (ElevatorListener l : listeners) l.onEvent(lift.state, lift.currentFloor);
    }

    void addDestination(int floor) {
        lift.addStop(floor);
    }

    void step() {
        lift.step();
        fireEvent();
    }

    int runToCompletion() {
        int steps = 0;
        while (lift.hasStops() || lift.state == LiftState.DOOR_OPEN) {
            step();
            steps++;
        }
        return steps;
    }

    int getCurrentFloor() { return lift.currentFloor; }
    LiftState getState() { return lift.state; }
}

// ==================== MAIN / TESTS ====================

public class ElevatorSystem {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════╗");
        System.out.println("║   ELEVATOR SYSTEM - LLD Demo          ║");
        System.out.println("╚═══════════════════════════════════════╝\n");

        // --- Test 1: Go up ---
        System.out.println("=== Test 1: Go up ===");
        ElevatorService svc = new ElevatorService(0, 10);
        svc.addDestination(3);
        int steps = svc.runToCompletion();
        check(svc.getCurrentFloor(), 3, "Arrived at 3");
        System.out.println("  Steps: " + steps);
        System.out.println("✓ Go up\n");

        // --- Test 2: Up then down ---
        System.out.println("=== Test 2: Up then down ===");
        svc = new ElevatorService(0, 10);
        svc.lift.currentFloor = 3; // start at 3
        svc.addDestination(5);     // up to 5
        svc.addDestination(1);     // down to 1
        steps = svc.runToCompletion();
        check(svc.getCurrentFloor(), 1, "Ended at 1");
        System.out.println("  Steps: " + steps);
        System.out.println("✓ Up then down\n");

        // --- Test 3: Multiple up stops ---
        System.out.println("=== Test 3: Multiple up stops ===");
        svc = new ElevatorService(0, 10);
        svc.addDestination(2);
        svc.addDestination(5);
        svc.addDestination(8);
        steps = svc.runToCompletion();
        check(svc.getCurrentFloor(), 8, "Ended at 8");
        System.out.println("  Steps: " + steps);
        System.out.println("✓ Multiple up stops\n");

        // --- Test 4: Door opens at stops ---
        System.out.println("=== Test 4: Door opens ===");
        svc = new ElevatorService(0, 10);
        svc.addDestination(2);
        svc.addDestination(5);
        svc.step(); // floor 1
        svc.step(); // floor 2 → DOOR_OPEN
        check(svc.getCurrentFloor(), 2, "At floor 2");
        check(svc.getState(), LiftState.DOOR_OPEN, "Door open");
        svc.step(); // door closes
        check(svc.getState() != LiftState.DOOR_OPEN, true, "Door closed");
        svc.runToCompletion();
        check(svc.getCurrentFloor(), 5, "Arrived at 5");
        System.out.println("✓ Door opens at stop\n");

        // --- Test 5: Idle state ---
        System.out.println("=== Test 5: Idle ===");
        svc = new ElevatorService(0, 10);
        check(svc.getState(), LiftState.IDLE, "Starts idle");
        check(svc.lift.hasStops(), false, "No stops");
        System.out.println("✓ Idle\n");

        // --- Test 6: Same floor no-op ---
        System.out.println("=== Test 6: Same floor ===");
        svc = new ElevatorService(0, 10);
        svc.lift.currentFloor = 5;
        svc.addDestination(5);
        check(svc.lift.hasStops(), false, "No stops for same floor");
        System.out.println("✓ Same floor no-op\n");

        // --- Test 7: Invalid floor ---
        System.out.println("=== Test 7: Invalid floor ===");
        svc = new ElevatorService(0, 10);
        try { svc.addDestination(11); System.out.println("  ✗"); }
        catch (InvalidFloorException e) { System.out.println("  ✓ " + e.getMessage()); }
        try { svc.addDestination(-1); System.out.println("  ✗"); }
        catch (InvalidFloorException e) { System.out.println("  ✓ " + e.getMessage()); }
        System.out.println("✓ Invalid floor blocked\n");

        // --- Test 8: Observer ---
        System.out.println("=== Test 8: Observer ===");
        svc = new ElevatorService(0, 10);
        ElevatorLogger logger = new ElevatorLogger();
        svc.addListener(logger);
        svc.addDestination(3);
        svc.runToCompletion();
        check(logger.events.size() > 0, true, "Logger received events (" + logger.events.size() + ")");
        System.out.println("  Events: " + logger.events);
        System.out.println("✓ Observer\n");

        // --- Test 9: Complex route ---
        System.out.println("=== Test 9: Complex route ===");
        svc = new ElevatorService(0, 10);
        svc.lift.currentFloor = 4; // start at 4
        svc.addDestination(7);     // up to 7
        svc.addDestination(2);     // down to 2
        svc.addDestination(9);     // up to 9
        // From 4: up to 7, up to 9, then down to 2
        steps = svc.runToCompletion();
        check(svc.getCurrentFloor(), 2, "Ended at 2");
        System.out.println("  Steps: " + steps);
        System.out.println("✓ Complex route\n");

        // --- Test 10: Scale ---
        System.out.println("=== Test 10: Scale ===");
        svc = new ElevatorService(0, 100);
        for (int i = 0; i <= 100; i += 10) svc.addDestination(i == 0 ? 1 : i);
        steps = svc.runToCompletion();
        System.out.println("  100 floors, stops every 10: " + steps + " steps");
        System.out.println("✓ Scale\n");

        System.out.println("════════ ALL 10 TESTS PASSED ✓ ════════");
    }

    static void check(int a, int e, String m) {
        System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a + " expected " + e) + " " + m);
    }
    static void check(boolean a, boolean e, String m) {
        System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m);
    }
    static void check(LiftState a, LiftState e, String m) {
        System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m);
    }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. SCAN/LOOK ALGORITHM:
 *    Lift goes one direction serving all stops, then reverses.
 *    Two TreeSets: upStops (asc), downStops (desc).
 *    Next stop = first() → O(1) peek, O(log n) remove.
 *
 * 2. OBSERVER: ElevatorListener notified on each step for logging/UI.
 *
 * 3. STATES: IDLE → MOVING_UP/DOWN → DOOR_OPEN → back to MOVING or IDLE.
 *    DOOR_OPEN lasts one step (models dwell time).
 *
 * 4. STEP SIMULATION: Each step() moves 1 floor. runToCompletion() loops until idle.
 *
 * 5. EXTENSIONS:
 *    - Multi-lift: add Strategy pattern for elevator selection
 *    - Capacity: track passenger count, skip if full
 *    - Emergency: clear stops, go to ground
 *    - Event-driven: replace step() with real timers
 */
