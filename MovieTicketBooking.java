import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

// ==================== ENUMS ====================

enum SeatType { REGULAR, PREMIUM, VIP }
enum ShowSeatStatus { AVAILABLE, LOCKED, BOOKED }
enum BookingStatus { CONFIRMED, CANCELLED }

// ==================== EXCEPTIONS ====================

class SeatNotAvailableException extends Exception {
    public SeatNotAvailableException(String msg) { super(msg); }
}

class InvalidBookingException extends Exception {
    public InvalidBookingException(String msg) { super(msg); }
}

// ==================== DOMAIN CLASSES ====================

// ShowSeat - smallest bookable unit
class ShowSeat {
    String id;
    int row;
    int col;
    SeatType type;
    ShowSeatStatus status;

    ShowSeat(int row, int col, SeatType type) {
        this.row = row;
        this.col = col;
        this.id = (char) ('A' + row) + "" + (col + 1); // A1, A2, B1, B2...
        this.type = type;
        this.status = ShowSeatStatus.AVAILABLE;
    }

    double getPrice() {
        return switch (type) {
            case REGULAR -> 10.0;
            case PREMIUM -> 15.0;
            case VIP     -> 25.0;
        };
    }

    @Override
    public String toString() {
        return id + "(" + type.name().charAt(0) + ")";
    }
}

// Movie - what's playing
class Movie {
    String id;
    String title;
    int durationMinutes;

    Movie(String title, int durationMinutes) {
        this.id = "MOV-" + UUID.randomUUID().toString().substring(0, 6);
        this.title = title;
        this.durationMinutes = durationMinutes;
    }
}

// Screen - physical hall in a theater
class Screen {
    String id;
    String name;
    List<ShowSeat> seats;

    Screen(String name, int rows, int cols) {
        this.id = "SCR-" + UUID.randomUUID().toString().substring(0, 6);
        this.name = name;
        this.seats = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                SeatType type = (r < 2) ? SeatType.VIP : (r < 4) ? SeatType.PREMIUM : SeatType.REGULAR;
                seats.add(new ShowSeat(r, c, type));
            }
        }
    }

    ShowSeat getSeat(String seatId) {
        return seats.stream().filter(s -> s.id.equals(seatId)).findFirst().orElse(null);
    }
}

// Show - a movie playing on a screen at a specific time
class Show {
    String id;
    Movie movie;
    Screen screen;
    LocalDateTime startTime;
    // Per-show seat status (independent of screen's default seats)
    Map<String, ShowSeatStatus> seatStatusMap;

    Show(Movie movie, Screen screen, LocalDateTime startTime) {
        this.id = "SHOW-" + UUID.randomUUID().toString().substring(0, 6);
        this.movie = movie;
        this.screen = screen;
        this.startTime = startTime;
        this.seatStatusMap = new HashMap<>();
        // Initialize all seats as AVAILABLE for this show
        screen.seats.forEach(s -> seatStatusMap.put(s.id, ShowSeatStatus.AVAILABLE));
    }

    List<ShowSeat> getAvailableSeats() {
        return screen.seats.stream()
            .filter(s -> seatStatusMap.get(s.id) == ShowSeatStatus.AVAILABLE)
            .collect(Collectors.toList());
    }

    String getShowInfo() {
        long available = seatStatusMap.values().stream().filter(s -> s == ShowSeatStatus.AVAILABLE).count();
        long booked = seatStatusMap.values().stream().filter(s -> s == ShowSeatStatus.BOOKED).count();
        return String.format("%s | %s | %s | Seats: %d avail, %d booked",
            movie.title, screen.name,
            startTime.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")),
            available, booked);
    }
}

// Booking - a confirmed reservation
class Booking {
    String id;
    String showId;
    String userId;
    List<String> seatIds;
    double totalAmount;
    BookingStatus status;
    LocalDateTime bookedAt;

    Booking(String showId, String userId, List<String> seatIds, double totalAmount) {
        this.id = "BKG-" + UUID.randomUUID().toString().substring(0, 6);
        this.showId = showId;
        this.userId = userId;
        this.seatIds = new ArrayList<>(seatIds);
        this.totalAmount = totalAmount;
        this.status = BookingStatus.CONFIRMED;
        this.bookedAt = LocalDateTime.now();
    }
}

// Theater - contains screens and manages shows
class Theater {
    String id;
    String name;
    String city;
    List<Screen> screens;

    Theater(String name, String city, List<Screen> screens) {
        this.id = "TH-" + UUID.randomUUID().toString().substring(0, 6);
        this.name = name;
        this.city = city;
        this.screens = new ArrayList<>(screens);
    }
}

// ==================== MAIN SERVICE - THREAD SAFE ====================

class BookingService {
    List<Theater> theaters;
    List<Show> shows;
    Map<String, Booking> bookings; // bookingId -> Booking

    BookingService() {
        this.theaters = new ArrayList<>();
        this.shows = new ArrayList<>();
        this.bookings = new HashMap<>();
    }

    void addTheater(Theater theater) { theaters.add(theater); }
    void addShow(Show show) { shows.add(show); }

    // Search shows by movie title
    List<Show> searchShows(String movieTitle) {
        return shows.stream()
            .filter(s -> s.movie.title.equalsIgnoreCase(movieTitle))
            .collect(Collectors.toList());
    }

    // Book seats - SYNCHRONIZED on the Show object to allow parallel bookings on different shows
    public Booking bookSeats(String showId, String userId, List<String> seatIds) throws SeatNotAvailableException {
        Show show = findShowById(showId);
        if (show == null) throw new SeatNotAvailableException("Show not found: " + showId);

        synchronized (show) {
            // 1. Validate all seats are AVAILABLE
            for (String seatId : seatIds) {
                ShowSeatStatus status = show.seatStatusMap.get(seatId);
                if (status == null) {
                    throw new SeatNotAvailableException("Seat " + seatId + " does not exist");
                }
                if (status != ShowSeatStatus.AVAILABLE) {
                    throw new SeatNotAvailableException("Seat " + seatId + " is " + status);
                }
            }

            // 2. Lock seats (atomic - inside synchronized)
            seatIds.forEach(id -> show.seatStatusMap.put(id, ShowSeatStatus.LOCKED));

            // 3. Calculate total
            double total = seatIds.stream()
                .map(id -> show.screen.getSeat(id))
                .mapToDouble(ShowSeat::getPrice)
                .sum();

            // 4. Mark as BOOKED (simulating payment success)
            seatIds.forEach(id -> show.seatStatusMap.put(id, ShowSeatStatus.BOOKED));

            // 5. Create booking
            Booking booking = new Booking(showId, userId, seatIds, total);
            bookings.put(booking.id, booking);

            System.out.println(Thread.currentThread().getName() + ": " + userId
                + " booked seats " + seatIds + " for " + show.movie.title
                + " | Booking: " + booking.id + " | $" + total);
            return booking;
        }
    }

    // Cancel booking - SYNCHRONIZED on the Show object
    public void cancelBooking(String bookingId) throws InvalidBookingException {
        Booking booking = bookings.get(bookingId);
        if (booking == null) throw new InvalidBookingException("Booking not found: " + bookingId);
        if (booking.status == BookingStatus.CANCELLED) throw new InvalidBookingException("Already cancelled");

        Show show = findShowById(booking.showId);

        synchronized (show) {
            // Free the seats
            booking.seatIds.forEach(id -> show.seatStatusMap.put(id, ShowSeatStatus.AVAILABLE));
            booking.status = BookingStatus.CANCELLED;

            System.out.println(Thread.currentThread().getName() + ": Booking " + bookingId
                + " cancelled. Seats " + booking.seatIds + " now AVAILABLE");
        }
    }

    // Helper
    private Show findShowById(String id) {
        return shows.stream().filter(s -> s.id.equals(id)).findFirst().orElse(null);
    }

    void displayStatus() {
        System.out.println("\n--- Booking Service Status ---");
        System.out.println("Theaters: " + theaters.size());
        System.out.println("Shows:");
        shows.forEach(s -> System.out.println("  " + s.getShowInfo()));
        long confirmed = bookings.values().stream().filter(b -> b.status == BookingStatus.CONFIRMED).count();
        long cancelled = bookings.values().stream().filter(b -> b.status == BookingStatus.CANCELLED).count();
        System.out.println("Bookings: " + bookings.size() + " (Confirmed: " + confirmed + ", Cancelled: " + cancelled + ")");
    }
}

// ==================== MAIN ====================

public class MovieTicketBooking {
    public static void main(String[] args) throws InterruptedException {
        // ---- Setup ----
        Screen screen1 = new Screen("Screen 1", 6, 8); // 6 rows x 8 cols = 48 seats
        Screen screen2 = new Screen("Screen 2", 4, 6); // 4 rows x 6 cols = 24 seats

        Theater theater = new Theater("PVR Cinemas", "Mumbai",
            Arrays.asList(screen1, screen2));

        Movie movie1 = new Movie("Interstellar", 169);
        Movie movie2 = new Movie("The Dark Knight", 152);

        Show show1 = new Show(movie1, screen1, LocalDateTime.now().plusHours(2));
        Show show2 = new Show(movie2, screen2, LocalDateTime.now().plusHours(3));

        BookingService service = new BookingService();
        service.addTheater(theater);
        service.addShow(show1);
        service.addShow(show2);

        service.displayStatus();

        // ---- Test 1: Basic Booking ----
        System.out.println("\n=== Test 1: BASIC BOOKING ===");
        try {
            Booking b1 = service.bookSeats(show1.id, "user-alice", Arrays.asList("A1", "A2"));
            Booking b2 = service.bookSeats(show1.id, "user-bob", Arrays.asList("C1", "C2", "C3"));
            service.displayStatus();
        } catch (SeatNotAvailableException e) {
            System.out.println("ERROR: " + e.getMessage());
        }

        // ---- Test 2: Double Booking Prevention ----
        System.out.println("\n=== Test 2: DOUBLE BOOKING PREVENTION ===");
        try {
            service.bookSeats(show1.id, "user-charlie", Arrays.asList("A1")); // Already booked by Alice
        } catch (SeatNotAvailableException e) {
            System.out.println("✓ Correctly prevented: " + e.getMessage());
        }

        // ---- Test 3: Cancellation & Re-booking ----
        System.out.println("\n=== Test 3: CANCEL & REBOOK ===");
        try {
            // Find Alice's booking
            String aliceBookingId = service.bookings.values().stream()
                .filter(b -> b.userId.equals("user-alice") && b.status == BookingStatus.CONFIRMED)
                .findFirst().get().id;

            service.cancelBooking(aliceBookingId);

            // Now another user can book those freed seats
            Booking b3 = service.bookSeats(show1.id, "user-dave", Arrays.asList("A1", "A2"));
            System.out.println("✓ Re-booking after cancel succeeded");
            service.displayStatus();
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }

        // ---- Test 4: CONCURRENCY - Multiple threads booking same seats ----
        System.out.println("\n=== Test 4: CONCURRENT BOOKING (5 threads racing for same seats) ===");
        List<Thread> threads = new ArrayList<>();
        List<Booking> successfulBookings = Collections.synchronizedList(new ArrayList<>());
        List<String> failures = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < 5; i++) {
            final String userId = "concurrent-user-" + i;
            Thread t = new Thread(() -> {
                try {
                    // All 5 threads race for seats B1, B2
                    Booking b = service.bookSeats(show2.id, userId, Arrays.asList("A1", "A2"));
                    successfulBookings.add(b);
                } catch (SeatNotAvailableException e) {
                    failures.add(userId + ": " + e.getMessage());
                }
            }, "BookThread-" + i);
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        System.out.println("Successful bookings: " + successfulBookings.size());
        System.out.println("Failed (expected 4): " + failures.size());
        failures.forEach(f -> System.out.println("  ✓ " + f));
        assert successfulBookings.size() == 1 : "Only 1 booking should succeed!";
        System.out.println("✓ Concurrency test passed - no double bookings!");

        // ---- Test 5: CONCURRENT BOOKINGS on DIFFERENT shows (should all succeed) ----
        System.out.println("\n=== Test 5: CONCURRENT BOOKINGS ON DIFFERENT SHOWS ===");
        Show show3 = new Show(movie1, screen1, LocalDateTime.now().plusHours(5));
        Show show4 = new Show(movie2, screen2, LocalDateTime.now().plusHours(6));
        service.addShow(show3);
        service.addShow(show4);

        threads.clear();
        List<Booking> parallelBookings = Collections.synchronizedList(new ArrayList<>());

        // Thread 1 books on show3
        threads.add(new Thread(() -> {
            try {
                parallelBookings.add(service.bookSeats(show3.id, "parallel-user-1", Arrays.asList("A1", "A2")));
            } catch (SeatNotAvailableException e) {
                System.out.println("Unexpected: " + e.getMessage());
            }
        }, "ParallelThread-1"));

        // Thread 2 books on show4 (different show, should NOT block)
        threads.add(new Thread(() -> {
            try {
                parallelBookings.add(service.bookSeats(show4.id, "parallel-user-2", Arrays.asList("A1", "A2")));
            } catch (SeatNotAvailableException e) {
                System.out.println("Unexpected: " + e.getMessage());
            }
        }, "ParallelThread-2"));

        for (Thread t : threads) { t.start(); }
        for (Thread t : threads) { t.join(); }

        System.out.println("Both bookings succeeded: " + (parallelBookings.size() == 2));
        System.out.println("✓ Different shows can be booked in parallel!");

        // ---- Final Status ----
        service.displayStatus();
    }
}