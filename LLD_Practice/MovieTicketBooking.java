import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

// ===== CUSTOM EXCEPTION CLASSES =====

class SeatNotAvailableException extends Exception {
    public SeatNotAvailableException(String msg) { super(msg); }
}

class InvalidBookingException extends Exception {
    public InvalidBookingException(String msg) { super(msg); }
}

// ===== ENUMS =====

enum SeatType { REGULAR, PREMIUM, VIP }
enum ShowSeatStatus { AVAILABLE, LOCKED, BOOKED }
enum BookingStatus { CONFIRMED, CANCELLED }

// ===== DOMAIN CLASSES =====

class ShowSeat {
    String id;
    int row, col;
    SeatType type;
    ShowSeatStatus status;
    
    ShowSeat(int row, int col, SeatType type) {
        this.row = row;
        this.col = col;
        this.id = (char)('A' + row) + "" + (col + 1);
        this.type = type;
        this.status = ShowSeatStatus.AVAILABLE;
    }
    
    double getPrice() {
        return switch(type) {
            case REGULAR -> 10.0;
            case PREMIUM -> 15.0;
            case VIP -> 25.0;
        };
    }
}

class Movie {
    String id, title;
    int durationMinutes;
    
    Movie(String title, int duration) {
        this.id = "MOV-" + UUID.randomUUID().toString().substring(0, 6);
        this.title = title;
        this.durationMinutes = duration;
    }
}

class Screen {
    String id, name;
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

class Show {
    String id;
    Movie movie;
    Screen screen;
    LocalDateTime startTime;
    Map<String, ShowSeatStatus> seatStatusMap;
    
    Show(Movie movie, Screen screen, LocalDateTime startTime) {
        this.id = "SHOW-" + UUID.randomUUID().toString().substring(0, 6);
        this.movie = movie;
        this.screen = screen;
        this.startTime = startTime;
        this.seatStatusMap = new HashMap<>();
        screen.seats.forEach(s -> seatStatusMap.put(s.id, ShowSeatStatus.AVAILABLE));
    }
    
    List<ShowSeat> getAvailableSeats() {
        return screen.seats.stream()
            .filter(s -> seatStatusMap.get(s.id) == ShowSeatStatus.AVAILABLE)
            .collect(Collectors.toList());
    }
}

class Booking {
    String id, showId, userId;
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

class Theater {
    String id, name, city;
    List<Screen> screens;
    
    Theater(String name, String city, List<Screen> screens) {
        this.id = "TH-" + UUID.randomUUID().toString().substring(0, 6);
        this.name = name;
        this.city = city;
        this.screens = new ArrayList<>(screens);
    }
}

class BookingService {
    List<Theater> theaters;
    List<Show> shows;
    Map<String, Booking> bookings;
    
    BookingService() {
        theaters = new ArrayList<>();
        shows = new ArrayList<>();
        bookings = new HashMap<>();
    }
    
    void addTheater(Theater t) { theaters.add(t); }
    void addShow(Show s) { shows.add(s); }
    
    /**
     * Book seats for a show
     * IMPLEMENTATION HINTS:
     * 1. Find show by ID
     * 2. Synchronize on show object
     * 3. Validate all seats are AVAILABLE
     * 4. Lock seats (set to LOCKED)
     * 5. Calculate total price
     * 6. Mark as BOOKED
     * 7. Create and store Booking
     */
    public Booking bookSeats(String showId, String userId, List<String> seatIds) 
            throws SeatNotAvailableException {
        // TODO: Implement
        // HINT: synchronized(show) { validate, lock, book }
        return null;
    }
    
    /**
     * Cancel booking
     * IMPLEMENTATION HINTS:
     * 1. Find booking
     * 2. Synchronize on show
     * 3. Free all seats (set to AVAILABLE)
     * 4. Mark booking as CANCELLED
     */
    public void cancelBooking(String bookingId) throws InvalidBookingException {
        // TODO: Implement
    }
    
    List<Show> searchShows(String movieTitle) {
        return shows.stream()
            .filter(s -> s.movie.title.equalsIgnoreCase(movieTitle))
            .collect(Collectors.toList());
    }
    
    private Show findShowById(String id) {
        return shows.stream().filter(s -> s.id.equals(id)).findFirst().orElse(null);
    }
    
    void displayStatus() {
        System.out.println("\n--- Booking Service Status ---");
        System.out.println("Theaters: " + theaters.size() + ", Shows: " + shows.size());
        System.out.println("Bookings: " + bookings.size());
    }
}

public class MovieTicketBooking {
    public static void main(String[] args) {
        System.out.println("=== Movie Ticket Booking Test Cases ===\n");
        
        Screen screen = new Screen("Screen 1", 6, 8);
        Theater theater = new Theater("PVR Cinemas", "Mumbai", Arrays.asList(screen));
        Movie movie = new Movie("Interstellar", 169);
        Show show = new Show(movie, screen, LocalDateTime.now().plusHours(2));
        
        BookingService service = new BookingService();
        service.addTheater(theater);
        service.addShow(show);
        
        try {
            Booking b = service.bookSeats(show.id, "alice", Arrays.asList("A1", "A2"));
            System.out.println("✓ Booking successful: " + b.id);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        
        service.displayStatus();
    }
}
