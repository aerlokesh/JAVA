import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

// ===== CUSTOM EXCEPTION CLASSES =====

/**
 * Exception thrown when seat is not available for booking
 * WHEN TO THROW:
 * - Seat already booked/locked
 * - Concurrent booking conflict
 */
class SeatNotAvailableException extends Exception {
    private List<String> unavailableSeats;
    
    public SeatNotAvailableException(List<String> unavailableSeats) {
        super("Seats not available: " + unavailableSeats);
        this.unavailableSeats = unavailableSeats;
    }
    
    public List<String> getUnavailableSeats() { return unavailableSeats; }
}

/**
 * Exception thrown when booking operation is invalid
 * WHEN TO THROW:
 * - Invalid show ID
 * - Empty seat list
 * - Booking not found for cancellation
 */
class InvalidBookingException extends Exception {
    public InvalidBookingException(String message) {
        super(message);
    }
}

// ===== ENUMS =====

enum SeatType { 
    REGULAR,   // Standard seats
    PREMIUM,   // Better view/position
    VIP        // Best seats, extra amenities
}

enum ShowSeatStatus { 
    AVAILABLE,  // Seat is available to book
    LOCKED,     // Temporarily locked (in-progress booking)
    BOOKED      // Confirmed booking
}

enum BookingStatus { 
    CONFIRMED,  // Booking successful
    CANCELLED   // Booking cancelled
}

// ===== DOMAIN CLASSES =====

/**
 * Represents a seat in a show
 */
class ShowSeat {
    String id;           // e.g., "A1", "B5"
    int row;
    int col;
    SeatType type;
    ShowSeatStatus status;
    double basePrice;
    
    public ShowSeat(int row, int col, SeatType type) {
        this.row = row;
        this.col = col;
        this.id = (char)('A' + row) + "" + (col + 1);
        this.type = type;
        this.status = ShowSeatStatus.AVAILABLE;
        this.basePrice = getPriceForType(type);
    }
    
    private double getPriceForType(SeatType type) {
        return switch(type) {
            case REGULAR -> 10.0;
            case PREMIUM -> 15.0;
            case VIP -> 25.0;
        };
    }
    
    @Override
    public String toString() {
        return id + "[" + type + "," + status + ",$" + basePrice + "]";
    }
}

/**
 * Represents a movie
 */
class Movie {
    String id;
    String title;
    int durationMinutes;
    String genre;
    
    public Movie(String title, int durationMinutes, String genre) {
        this.id = "MOV-" + UUID.randomUUID().toString().substring(0, 6);
        this.title = title;
        this.durationMinutes = durationMinutes;
        this.genre = genre;
    }
    
    @Override
    public String toString() {
        return title + " (" + durationMinutes + " mins, " + genre + ")";
    }
}

/**
 * Represents a cinema screen
 */
class Screen {
    String id;
    String name;
    List<ShowSeat> seats;
    int totalSeats;
    
    public Screen(String name, int rows, int cols) {
        this.id = "SCR-" + UUID.randomUUID().toString().substring(0, 6);
        this.name = name;
        this.seats = new ArrayList<>();
        
        // Initialize seats with type based on row
        // First 2 rows: VIP, Next 2: PREMIUM, Rest: REGULAR
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                SeatType type = (r < 2) ? SeatType.VIP : 
                              (r < 4) ? SeatType.PREMIUM : 
                              SeatType.REGULAR;
                seats.add(new ShowSeat(r, c, type));
            }
        }
        this.totalSeats = rows * cols;
    }
    
    public ShowSeat getSeat(String seatId) {
        return seats.stream().filter(s -> s.id.equals(seatId)).findFirst().orElse(null);
    }
}

/**
 * Represents a theater/cinema location
 */
class Theater {
    String id;
    String name;
    String city;
    List<Screen> screens;
    
    public Theater(String name, String city, List<Screen> screens) {
        this.id = "TH-" + UUID.randomUUID().toString().substring(0, 6);
        this.name = name;
        this.city = city;
        this.screens = new ArrayList<>(screens);
    }
    
    @Override
    public String toString() {
        return name + ", " + city + " (" + screens.size() + " screens)";
    }
}

/**
 * Represents a movie show/screening
 */
class Show {
    String id;
    Movie movie;
    Screen screen;
    LocalDateTime startTime;
    Map<String, ShowSeatStatus> seatStatusMap;  // seatId -> status
    
    public Show(Movie movie, Screen screen, LocalDateTime startTime) {
        this.id = "SHOW-" + UUID.randomUUID().toString().substring(0, 6);
        this.movie = movie;
        this.screen = screen;
        this.startTime = startTime;
        this.seatStatusMap = new HashMap<>();
        
        // Initialize all seats as AVAILABLE
        screen.seats.forEach(s -> seatStatusMap.put(s.id, ShowSeatStatus.AVAILABLE));
    }
    
    public List<ShowSeat> getAvailableSeats() {
        return screen.seats.stream()
            .filter(s -> seatStatusMap.get(s.id) == ShowSeatStatus.AVAILABLE)
            .collect(Collectors.toList());
    }
    
    @Override
    public String toString() {
        long available = seatStatusMap.values().stream()
            .filter(status -> status == ShowSeatStatus.AVAILABLE)
            .count();
        return movie.title + " at " + startTime + " (" + available + "/" + screen.totalSeats + " available)";
    }
}

/**
 * Represents a confirmed booking
 */
class Booking {
    String id;
    String showId;
    String userId;
    List<String> seatIds;
    double totalAmount;
    BookingStatus status;
    LocalDateTime bookedAt;
    
    public Booking(String showId, String userId, List<String> seatIds, double totalAmount) {
        this.id = "BKG-" + UUID.randomUUID().toString().substring(0, 6);
        this.showId = showId;
        this.userId = userId;
        this.seatIds = new ArrayList<>(seatIds);
        this.totalAmount = totalAmount;
        this.status = BookingStatus.CONFIRMED;
        this.bookedAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return id + " [" + userId + ", " + seatIds.size() + " seats, $" + 
               String.format("%.2f", totalAmount) + ", " + status + "]";
    }
}

/**
 * Movie Ticket Booking System - Low Level Design (LLD)
 * 
 * PROBLEM STATEMENT:
 * Design a movie ticket booking system that can:
 * 1. Manage multiple theaters, screens, and shows
 * 2. Handle seat selection and booking
 * 3. Prevent double booking with concurrency control
 * 4. Support different seat types and pricing
 * 5. Allow booking cancellation
 * 6. Search for shows by movie/theater/time
 * 
 * REQUIREMENTS:
 * - Functional: Book seats, cancel booking, search shows, view availability
 * - Non-Functional: Thread-safe, consistent, scalable
 * 
 * INTERVIEW HINTS:
 * - Discuss locking strategies for concurrent bookings
 * - Talk about transaction management
 * - Mention seat selection algorithms
 * - Consider payment integration
 * - Discuss scalability with distributed systems
 */
class BookingService {
    private List<Theater> theaters;
    private List<Show> shows;
    private Map<String, Booking> bookings;  // bookingId -> Booking
    
    public BookingService() {
        this.theaters = new ArrayList<>();
        this.shows = new ArrayList<>();
        this.bookings = new HashMap<>();
    }
    
    /**
     * Add theater to the system
     */
    public void addTheater(Theater theater) {
        // TODO: Implement
        // HINT: theaters.add(theater);
        // HINT: System.out.println("Added theater: " + theater.name);
        theaters.add(theater);
        System.out.println("Added theater: " + theater.name);
    }
    
    /**
     * Add show to the system
     */
    public void addShow(Show show) {
        // TODO: Implement
        // HINT: shows.add(show);
        // HINT: System.out.println("Added show: " + show.movie.title + " at " + show.startTime);
    }
    
    /**
     * Book seats for a show
     * 
     * IMPLEMENTATION HINTS:
     * 1. Find show by ID, throw exception if not found
     * 2. Synchronize on show object (prevent concurrent bookings)
     * 3. Validate all requested seats exist
     * 4. Check all seats are AVAILABLE (not LOCKED or BOOKED)
     * 5. Lock all seats (set to LOCKED) - atomic operation
     * 6. Calculate total price (sum of seat prices)
     * 7. Create Booking object
     * 8. Mark all seats as BOOKED
     * 9. Store booking in bookings map
     * 10. Return Booking object
     * 
     * INTERVIEW DISCUSSION:
     * - Why synchronize on show? (prevent race conditions)
     * - What if payment fails? (need rollback mechanism)
     * - How to handle partial availability?
     * - Database transaction isolation level?
     * 
     * @param showId Show identifier
     * @param userId User making the booking
     * @param seatIds List of seat IDs to book
     * @return Confirmed booking
     * @throws InvalidBookingException if show not found or invalid input
     * @throws SeatNotAvailableException if any seat is unavailable
     */
    public Booking bookSeats(String showId, String userId, List<String> seatIds) 
            throws InvalidBookingException, SeatNotAvailableException {
        // TODO: Implement
        // HINT: Show show = findShowById(showId);
        // HINT: if (show == null) throw new InvalidBookingException("Show not found: " + showId);
        // HINT: if (seatIds == null || seatIds.isEmpty()) 
        //           throw new InvalidBookingException("Seat list cannot be empty");
        // HINT: synchronized(show) {
        //     // Validate all seats exist and are AVAILABLE
        //     List<String> unavailable = new ArrayList<>();
        //     for (String seatId : seatIds) {
        //         ShowSeat seat = show.screen.getSeat(seatId);
        //         if (seat == null || show.seatStatusMap.get(seatId) != ShowSeatStatus.AVAILABLE) {
        //             unavailable.add(seatId);
        //         }
        //     }
        //     if (!unavailable.isEmpty()) throw new SeatNotAvailableException(unavailable);
        //     
        //     // Lock all seats
        //     seatIds.forEach(id -> show.seatStatusMap.put(id, ShowSeatStatus.LOCKED));
        //     
        //     // Calculate total
        //     double total = seatIds.stream()
        //         .map(id -> show.screen.getSeat(id))
        //         .mapToDouble(seat -> seat.basePrice)
        //         .sum();
        //     
        //     // Create booking
        //     Booking booking = new Booking(showId, userId, seatIds, total);
        //     
        //     // Confirm seats as BOOKED
        //     seatIds.forEach(id -> show.seatStatusMap.put(id, ShowSeatStatus.BOOKED));
        //     
        //     // Store booking
        //     bookings.put(booking.id, booking);
        //     
        //     return booking;
        // }
        return null;
    }
    
    /**
     * Cancel booking
     * 
     * IMPLEMENTATION HINTS:
     * 1. Find booking, throw exception if not found
     * 2. Find associated show
     * 3. Synchronize on show
     * 4. Free all booked seats (set to AVAILABLE)
     * 5. Mark booking as CANCELLED
     * 6. In real system: process refund
     * 
     * @param bookingId Booking to cancel
     * @throws InvalidBookingException if booking not found
     */
    public void cancelBooking(String bookingId) throws InvalidBookingException {
        // TODO: Implement
        // HINT: Booking booking = bookings.get(bookingId);
        // HINT: if (booking == null) throw new InvalidBookingException("Booking not found: " + bookingId);
        // HINT: Show show = findShowById(booking.showId);
        // HINT: synchronized(show) {
        //     booking.seatIds.forEach(seatId -> 
        //         show.seatStatusMap.put(seatId, ShowSeatStatus.AVAILABLE));
        //     booking.status = BookingStatus.CANCELLED;
        // }
    }
    
    /**
     * Search shows by movie title
     * 
     * IMPLEMENTATION HINTS:
     * 1. Filter shows by movie title (case-insensitive)
     * 2. Return list of matching shows
     * 
     * @param movieTitle Movie title to search
     * @return List of shows for this movie
     */
    public List<Show> searchShowsByMovie(String movieTitle) {
        // TODO: Implement
        // HINT: return shows.stream()
        //     .filter(s -> s.movie.title.equalsIgnoreCase(movieTitle))
        //     .collect(Collectors.toList());
        return new ArrayList<>();
    }
    
    /**
     * Search shows by city
     * 
     * @param city City to search
     * @return List of shows in this city
     */
    public List<Show> searchShowsByCity(String city) {
        // TODO: Implement
        // HINT: Need to join shows with theaters
        // HINT: Filter theaters by city, then get their shows
        return new ArrayList<>();
    }
    
    /**
     * Get user's bookings
     * 
     * @param userId User ID
     * @return List of user's bookings
     */
    public List<Booking> getUserBookings(String userId) {
        // TODO: Implement
        // HINT: return bookings.values().stream()
        //     .filter(b -> b.userId.equals(userId))
        //     .collect(Collectors.toList());
        return new ArrayList<>();
    }
    
    /**
     * Get available seats for a show
     * 
     * @param showId Show ID
     * @return List of available seats
     * @throws InvalidBookingException if show not found
     */
    public List<ShowSeat> getAvailableSeats(String showId) throws InvalidBookingException {
        // TODO: Implement
        // HINT: Show show = findShowById(showId);
        // HINT: if (show == null) throw new InvalidBookingException("Show not found");
        // HINT: return show.getAvailableSeats();
        return new ArrayList<>();
    }
    
    /**
     * Find show by ID
     * 
     * @param showId Show ID
     * @return Show or null
     */
    private Show findShowById(String showId) {
        // TODO: Implement
        // HINT: return shows.stream()
        //     .filter(s -> s.id.equals(showId))
        //     .findFirst()
        //     .orElse(null);
        return null;
    }
    
    /**
     * Display service status
     */
    public void displayStatus() {
        System.out.println("\n--- Booking Service Status ---");
        System.out.println("Theaters: " + theaters.size());
        System.out.println("Shows: " + shows.size());
        System.out.println("Bookings: " + bookings.size());
        long confirmed = bookings.values().stream()
            .filter(b -> b.status == BookingStatus.CONFIRMED)
            .count();
        System.out.println("  Confirmed: " + confirmed + ", Cancelled: " + (bookings.size() - confirmed));
    }
}

// ===== MAIN TEST CLASS =====

public class MovieTicketBooking {
    public static void main(String[] args) {
        System.out.println("=== Movie Ticket Booking System Test Cases ===\n");
        
        // Setup
        Screen screen1 = new Screen("Screen 1", 6, 8);  // 6 rows, 8 seats per row = 48 seats
        Screen screen2 = new Screen("Screen 2", 5, 10); // 50 seats
        
        Theater theater1 = new Theater("PVR Cinemas", "Mumbai", Arrays.asList(screen1));
        Theater theater2 = new Theater("INOX", "Mumbai", Arrays.asList(screen2));
        
        Movie movie1 = new Movie("Interstellar", 169, "Sci-Fi");
        Movie movie2 = new Movie("The Dark Knight", 152, "Action");
        
        Show show1 = new Show(movie1, screen1, LocalDateTime.now().plusHours(2));
        Show show2 = new Show(movie1, screen1, LocalDateTime.now().plusHours(5));
        Show show3 = new Show(movie2, screen2, LocalDateTime.now().plusHours(3));
        
        BookingService service = new BookingService();
        service.addTheater(theater1);
        service.addTheater(theater2);
        service.addShow(show1);
        service.addShow(show2);
        service.addShow(show3);
        
        System.out.println("Setup complete: 2 theaters, 3 shows\n");
        
        // Test Case 1: Basic Booking
        System.out.println("=== Test Case 1: Basic Booking ===");
        try {
            List<String> seats = Arrays.asList("A1", "A2");
            Booking booking = service.bookSeats(show1.id, "alice", seats);
            System.out.println("✓ Booking successful: " + booking);
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 2: Multiple Seat Booking
        System.out.println("=== Test Case 2: Book Multiple Seats (4 seats) ===");
        try {
            List<String> seats = Arrays.asList("B1", "B2", "B3", "B4");
            Booking booking = service.bookSeats(show1.id, "bob", seats);
            System.out.println("✓ Booked 4 seats: " + booking);
            System.out.println("  Total amount: $" + String.format("%.2f", booking.totalAmount));
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 3: VIP Seats (Higher Price)
        System.out.println("=== Test Case 3: Book VIP Seats ===");
        try {
            List<String> vipSeats = Arrays.asList("A3", "A4");  // Row A = VIP
            Booking booking = service.bookSeats(show1.id, "charlie", vipSeats);
            System.out.println("✓ VIP seats booked");
            System.out.println("  Price: $" + String.format("%.2f", booking.totalAmount) + 
                             " (VIP rate $25/seat)");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 4: Check Available Seats
        System.out.println("=== Test Case 4: Check Availability ===");
        try {
            List<ShowSeat> available = service.getAvailableSeats(show1.id);
            System.out.println("Available seats: " + available.size() + "/" + screen1.totalSeats);
            System.out.println("Sample available: " + 
                             available.stream().limit(5).map(s -> s.id).collect(Collectors.joining(", ")));
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 5: Cancel Booking
        System.out.println("=== Test Case 5: Cancel Booking ===");
        try {
            // Book and then cancel
            Booking booking = service.bookSeats(show2.id, "david", Arrays.asList("C1", "C2"));
            System.out.println("✓ Booking created: " + booking.id);
            
            service.cancelBooking(booking.id);
            System.out.println("✓ Booking cancelled successfully");
            
            // Seats should be available again
            List<ShowSeat> available = service.getAvailableSeats(show2.id);
            System.out.println("  C1, C2 now available: " + 
                             available.stream().anyMatch(s -> s.id.equals("C1")));
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 6: Search Shows
        System.out.println("=== Test Case 6: Search Shows ===");
        List<Show> interstellarShows = service.searchShowsByMovie("Interstellar");
        System.out.println("Found " + interstellarShows.size() + " shows for 'Interstellar'");
        interstellarShows.forEach(s -> System.out.println("  " + s));
        System.out.println();
        
        // Test Case 7: User's Bookings
        System.out.println("=== Test Case 7: Get User Bookings ===");
        List<Booking> aliceBookings = service.getUserBookings("alice");
        System.out.println("Alice's bookings: " + aliceBookings.size());
        aliceBookings.forEach(b -> System.out.println("  " + b));
        System.out.println();
        
        // ===== EXCEPTION TEST CASES =====
        
        // Test Case 8: Exception - Seat Already Booked
        System.out.println("=== Test Case 8: Exception - Double Booking ===");
        try {
            // Try to book already booked seats
            service.bookSeats(show1.id, "eve", Arrays.asList("A1", "A2"));
            System.out.println("✗ Should have thrown SeatNotAvailableException");
        } catch (SeatNotAvailableException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
            System.out.println("  Unavailable seats: " + e.getUnavailableSeats());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 9: Exception - Invalid Show
        System.out.println("=== Test Case 9: Exception - Invalid Show ===");
        try {
            service.bookSeats("INVALID-SHOW", "user", Arrays.asList("A1"));
            System.out.println("✗ Should have thrown InvalidBookingException");
        } catch (InvalidBookingException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 10: Exception - Empty Seat List
        System.out.println("=== Test Case 10: Exception - Empty Seat List ===");
        try {
            service.bookSeats(show1.id, "user", new ArrayList<>());
            System.out.println("✗ Should have thrown InvalidBookingException");
        } catch (InvalidBookingException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        service.displayStatus();
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION TOPICS:
 * ============================
 * 
 * 1. CONCURRENCY CONTROL:
 *    Show-Level Locking:
 *      - synchronized(show) for booking
 *      - Prevents double booking
 *      - Coarse-grained (whole show locked)
 *    
 *    Seat-Level Locking:
 *      - Lock only specific seats
 *      - Better concurrency (fine-grained)
 *      - More complex (need lock per seat)
 *    
 *    Optimistic Locking:
 *      - Check version before update
 *      - Retry on conflict
 *      - Better for high contention
 * 
 * 2. BOOKING WORKFLOW:
 *    1. Select seats → LOCK
 *    2. Enter payment → still LOCKED
 *    3. Payment success → BOOKED
 *    4. Payment fail → AVAILABLE
 *    
 *    Timeout:
 *      - LOCKED seats auto-expire (5-10 mins)
 *      - Free seats if payment not completed
 * 
 * 3. SEAT SELECTION ALGORITHMS:
 *    Auto-Select:
 *      - Find N consecutive seats
 *      - Prefer middle rows
 *      - Group bookings together
 *    
 *    Manual Select:
 *      - User picks from available
 *      - Show visual seat map
 *      - Highlight best available
 * 
 * 4. PRICING:
 *    Base Pricing:
 *      - Different rates by seat type
 *      - VIP > Premium > Regular
 *    
 *    Dynamic Pricing:
 *      - Weekend vs weekday
 *      - Matinee discounts
 *      - Demand-based (surge pricing)
 *      - Early bird discounts
 * 
 * 5. DATABASE DESIGN:
 *    Tables:
 *      - theaters (id, name, city)
 *      - screens (id, theater_id, name, total_seats)
 *      - movies (id, title, duration, genre)
 *      - shows (id, movie_id, screen_id, start_time)
 *      - show_seats (show_id, seat_id, status)
 *      - bookings (id, show_id, user_id, seats, amount, status)
 *    
 *    Indexes:
 *      - (movie_id, start_time) for show search
 *      - (show_id, status) for seat availability
 *      - (user_id, status) for user bookings
 * 
 * 6. TRANSACTION MANAGEMENT:
 *    ACID Properties:
 *      - Atomicity: All seats booked or none
 *      - Consistency: No double booking
 *      - Isolation: Concurrent bookings don't interfere
 *      - Durability: Bookings persisted
 *    
 *    Isolation Levels:
 *      - SERIALIZABLE for booking (strongest)
 *      - READ_COMMITTED for search (balance)
 * 
 * 7. SCALABILITY:
 *    Single Database:
 *      - Works for small theaters
 *      - Connection pool sizing important
 *    
 *    Distributed:
 *      - Shard by theater/city
 *      - Redis for seat availability cache
 *      - Message queue for bookings
 *      - Eventual consistency considerations
 * 
 * 8. ADVANCED FEATURES:
 *    - Seat hold with timer
 *    - Group booking discounts
 *    - Loyalty points integration
 *    - Food/beverage add-ons
 *    - Seat upgrade options
 *    - Waitlist when sold out
 *    - Mobile ticket QR codes
 *    - Refund policies
 * 
 * 9. REAL-WORLD INTEGRATIONS:
 *    - Payment gateways (Stripe, PayPal)
 *    - Email/SMS for confirmations
 *    - Movie metadata APIs (TMDb, OMDb)
 *    - Theater POS systems
 *    - Analytics and reporting
 * 
 * 10. DESIGN PATTERNS:
 *     - Factory: Create different booking types
 *     - Strategy: Pricing strategies
 *     - State: Seat/booking states
 *     - Observer: Notify on booking events
 *     - Repository: Data access abstraction
 * 
 * 11. PERFORMANCE OPTIMIZATION:
 *     - Cache available seat count
 *     - Denormalize popular queries
 *     - Read replicas for search
 *     - Write master for bookings
 *     - CDN for static content (movie posters)
 * 
 * 12. API DESIGN:
 *     GET  /theaters?city={city}           - List theaters
 *     GET  /shows?movie={title}&city={city} - Search shows
 *     GET  /shows/{id}/seats               - Get available seats
 *     POST /bookings                       - Create booking
 *     DELETE /bookings/{id}                - Cancel booking
 *     GET  /users/{id}/bookings            - User's bookings
 *     POST /shows/{id}/seats/hold          - Temporarily hold seats
 */
