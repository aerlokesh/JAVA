import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

enum SeatStatus{
    AVAILABLE,RESERVED,BOOKED
}

interface PricingStrategy{
    double calculate(double price);
}
class PremiumPricing implements PricingStrategy{
    @Override
    public double calculate(double price) {
        return price*2;
    }
}
class StandardPricing implements PricingStrategy{
    @Override
    public double calculate(double price) {
        return price;
    }
}
class Seat{
    private final String seatId;
    private SeatStatus seatStatus;
    private final double price;
    public Seat(double price){
        seatId = "ST"+UUID.randomUUID().toString().substring(0,8);
        this.price = price;
    }
    public synchronized SeatStatus getStatus(){ return seatStatus;}
    public synchronized void setStatus(SeatStatus seatStatus) {this.seatStatus = seatStatus;}
    public String getSeatId(){ return this.seatId;}
    public double getPrice(){return this.price;}
}
class MovieBookingSystem{
    private final ConcurrentHashMap<String,Seat> seats = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public String addSeat(double price){
        Seat seat = new Seat(price);
        seats.put(seat.getSeatId(),seat);
        return seat.getSeatId();
    }
    public boolean reserveSeat(String seatId,String user,int timeoutSeconds,PricingStrategy pricingStrategy){
        Seat seat = seats.get(seatId);
        if(seat == null || !seat.getStatus().equals(SeatStatus.AVAILABLE)) return false;
        Lock lock = locks.computeIfAbsent(seatId,k-> new ReentrantLock());
        try{
            if(lock.tryLock(1, TimeUnit.SECONDS)){
                try{
                    if(seat.getStatus() == SeatStatus.AVAILABLE){
                        double finalPrice = pricingStrategy.calculate(seat.getPrice());
                        seat.setStatus(SeatStatus.RESERVED);
                        System.out.printf("⏳ [Hold] %s reserved by %s. Price: $%.2f%n", seatId, user, finalPrice);

                    }
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
public class SeatBooking {
}
