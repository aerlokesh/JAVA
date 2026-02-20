import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

enum SeatStatus {
    AVAILABLE, RESERVED, BOOKED
}

interface PricingStrategy {
    double calculate(double price);
}

class PremiumPricing implements PricingStrategy {
    @Override
    public double calculate(double price) { return price * 2; }
}

class StandardPricing implements PricingStrategy {
    @Override
    public double calculate(double price) { return price; }
}

class Seat {
    private final String seatId;
    private SeatStatus seatStatus;
    private final double price;

    public Seat(double price) {
        seatId = "ST" + UUID.randomUUID().toString().substring(0, 8);
        this.seatStatus = SeatStatus.AVAILABLE;
        this.price = price;
    }

    public synchronized SeatStatus getStatus() { return seatStatus; }
    public synchronized void setStatus(SeatStatus s) { this.seatStatus = s; }
    public String getSeatId() { return this.seatId; }
    public double getPrice() { return this.price; }
}

class MovieBookingSystem {
    private final ConcurrentHashMap<String, Seat> seats = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public String addSeat(double price) {
        Seat seat = new Seat(price);
        seats.put(seat.getSeatId(), seat);
        return seat.getSeatId();
    }

    public boolean reserveSeat(String seatId, String user, int timeoutSeconds, PricingStrategy pricingStrategy) {
        Seat seat = seats.get(seatId);
        if (seat == null || seat.getStatus() != SeatStatus.AVAILABLE) return false;

        Lock lock = locks.computeIfAbsent(seatId, k -> new ReentrantLock());
        try {
            if (lock.tryLock(1, TimeUnit.SECONDS)) {
                try {
                    if (seat.getStatus() == SeatStatus.AVAILABLE) {
                        double finalPrice = pricingStrategy.calculate(seat.getPrice());
                        seat.setStatus(SeatStatus.RESERVED);
                        System.out.printf("⏳ [Hold] %s reserved by %s. Price: $%.2f%n", seatId, user, finalPrice);

                        // Auto-expire reservation after timeout
                        scheduler.schedule(() -> {
                            if (seat.getStatus() == SeatStatus.RESERVED) {
                                seat.setStatus(SeatStatus.AVAILABLE);
                                System.out.printf("⏰ [Expired] %s reservation expired%n", seatId);
                            }
                        }, timeoutSeconds, TimeUnit.SECONDS);
                        return true;
                    }
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    public boolean confirmBooking(String seatId, String user) {
        Seat seat = seats.get(seatId);
        if (seat == null || seat.getStatus() != SeatStatus.RESERVED) return false;
        seat.setStatus(SeatStatus.BOOKED);
        System.out.printf("✅ [Booked] %s confirmed by %s%n", seatId, user);
        return true;
    }

    public void displaySeats() {
        System.out.println("\n--- Seats ---");
        seats.forEach((id, s) -> System.out.println("  " + id + " → " + s.getStatus() + " ($" + s.getPrice() + ")"));
    }
}

public class SeatBooking {
    public static void main(String[] args) throws InterruptedException {
        MovieBookingSystem system = new MovieBookingSystem();
        String s1 = system.addSeat(10.0);
        String s2 = system.addSeat(20.0);

        system.displaySeats();

        // Reserve with premium pricing
        system.reserveSeat(s1, "alice", 5, new PremiumPricing());
        system.reserveSeat(s2, "bob", 5, new StandardPricing());

        // Confirm one
        system.confirmBooking(s1, "alice");

        system.displaySeats();
        System.out.println("✅ SeatBooking demo complete!");
    }
}