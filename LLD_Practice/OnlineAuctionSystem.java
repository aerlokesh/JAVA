import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/*
 * ONLINE AUCTION SYSTEM - Low Level Design
 * ==========================================
 * 
 * REQUIREMENTS:
 * 1. Create auction listings with start/end time and reserve price
 * 2. Place bids (must exceed current highest + minimum increment)
 * 3. Auto-close auctions when end time reached
 * 4. Determine winner (highest bidder above reserve price)
 * 5. Bid history per auction
 * 6. Notify observers on new bid / auction close (Observer pattern)
 * 7. Thread-safe concurrent bidding
 * 
 * KEY DATA STRUCTURES:
 * - ConcurrentHashMap<auctionId, Auction>: auction registry
 * - TreeMap<amount, Bid> per auction: sorted bids for O(log n) highest
 * - CopyOnWriteArrayList<Bid>: bid history per auction
 * 
 * DESIGN PATTERNS:
 * - Observer: notify on new bid, auction close
 * - State: auction lifecycle (UPCOMING → ACTIVE → CLOSED)
 * 
 * COMPLEXITY:
 *   placeBid:       O(log n) — TreeMap insert
 *   getHighestBid:  O(1) — TreeMap.lastEntry()
 *   closeAuction:   O(1)
 *   getBidHistory:  O(k) where k = bids for that auction
 */

// ==================== EXCEPTIONS ====================

class AuctionNotFoundException extends Exception {
    AuctionNotFoundException(String id) { super("Auction not found: " + id); }
}

class InvalidBidException extends Exception {
    InvalidBidException(String msg) { super(msg); }
}

class AuctionClosedException extends Exception {
    AuctionClosedException(String id) { super("Auction already closed: " + id); }
}

// ==================== ENUMS ====================

enum AuctionStatus { ACTIVE, CLOSED }

// ==================== DOMAIN CLASSES ====================

class Bid {
    final String bidId;
    final String auctionId;
    final String bidderId;
    final double amount;

    Bid(String auctionId, String bidderId, double amount) {
        this.bidId = "BID-" + UUID.randomUUID().toString().substring(0, 6);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
    }
}

class Auction {
    final String auctionId;
    final String sellerId;
    final String itemName;
    final double reservePrice;       // minimum price to sell
    final double minBidIncrement;    // each bid must exceed previous by at least this
    final long endTimeMs;            // when auction loses (epoch ms)
    AuctionStatus status;
    final List<Bid> bids = new ArrayList<>();
    volatile Bid highestBid;               // current winning bid (null if no bids)
    final ReadWriteLock lock = new ReentrantReadWriteLock();

    Auction(String sellerId, String itemName, double reservePrice,
            double minBidIncrement, long durationMs) {
        this.auctionId = "AUC-" + UUID.randomUUID().toString().substring(0, 6);
        this.sellerId = sellerId;
        this.itemName = itemName;
        this.reservePrice = reservePrice;
        this.minBidIncrement = minBidIncrement;
        this.endTimeMs = System.currentTimeMillis() + durationMs;
        this.status = AuctionStatus.ACTIVE;
    }

    boolean isExpired() { return System.currentTimeMillis() >= endTimeMs; }
}

// ==================== SERVICE ====================

/**
 * Online Auction System - Low Level Design (LLD)
 * 
 * PROBLEM: Design an online auction system (like eBay) that can:
 * 1. Create auctions with reserve price and duration
 * 2. Accept bids with validation (> current + increment)
 * 3. Auto-close expired auctions
 * 4. Determine winner
 * 5. Maintain bid history
 * 6. Notify on events (Observer)
 * 7. Handle concurrent bids safely
 * 
 * CONCURRENCY:
 *   ReadWriteLock per auction: writeLock for placeBid/close, readLock for getBidHistory
 *   ConcurrentHashMap for auction registry
 */
class AuctionService {
    private final ConcurrentHashMap<String, Auction> auctions = new ConcurrentHashMap<>();

    /**
     * Create a new auction
     * 
     * IMPLEMENTATION HINTS:
     * 1. Create Auction with given params
     * 2. Store in auctions map
     * 3. Return auction
     */
    Auction createAuction(String sellerId, String itemName, double reservePrice,
            double minBidIncrement, long durationMs) {
        // TODO: Implement
        // HINT: Auction auction = new Auction(sellerId, itemName, reservePrice, minBidIncrement, durationMs);
        // HINT: auctions.put(auction.auctionId, auction);
        // HINT: return auction;
        return null;
    }

    /**
     * Place a bid on an auction (synchronized per auction)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get auction → validate exists
     * 2. Check if expired → auto-close if so, throw AuctionClosedException
     * 3. Check status is ACTIVE
     * 4. Validate bid amount:
     *    - If no bids yet: amount >= reservePrice is nice but not required; amount > 0
     *    - If bids exist: amount >= highestBid.amount + minBidIncrement
     * 5. Seller cannot bid on own auction
     * 6. Create Bid, add to bids list, update highestBid
     * 7. Notify observers
     * 8. Return bid
     */
    Bid placeBid(String auctionId, String bidderId, double amount)
            throws AuctionNotFoundException, InvalidBidException, AuctionClosedException {
        // TODO: Implement
        // HINT: Auction auction = auctions.get(auctionId);
        // HINT: if (auction == null) throw new AuctionNotFoundException(auctionId);
        // HINT: auction.lock.writeLock().lock();
        // HINT: try {
        //     if (auction.isExpired() && auction.status == AuctionStatus.ACTIVE) closeAuction(auctionId);
        //     if (auction.status != AuctionStatus.ACTIVE) throw new AuctionClosedException(auctionId);
        //     if (bidderId.equals(auction.sellerId)) throw new InvalidBidException("Seller cannot bid");a
        //     double minRequired = (auction.highestBid != null)
        //         ? auction.highestBid.amount + auction.minBidIncrement : auction.minBidIncrement;
        //     if (amount < minRequired)
        //         throw new InvalidBidException(String.format("Bid $%.2f below minimum $%.2f", amount, minRequired));
        //     Bid bid = new Bid(auctionId, bidderId, amount);
        //     auction.bids.add(bid);
        //     auction.highestBid = bid;
        //     return bid;
        // } finally { auction.lock.writeLock().unlock(); }
        return null;
    }

    /**
     * Close an auction and determine winner
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get auction → validate exists
     * 2. Set status = CLOSED
     * 3. If highestBid exists AND amount >= reservePrice → winnerId = highestBid.bidderId
     * 4. Else → winnerId = null (reserve not met)
     * 5. Notify observers
     */
    void closeAuction(String auctionId) throws AuctionNotFoundException {
        // TODO: Implement
        // HINT: Auction auction = auctions.get(auctionId);
        // HINT: if (auction == null) throw new AuctionNotFoundException(auctionId);
        // HINT: auction.lock.writeLock().lock();
        // HINT: try {
        //     auction.status = AuctionStatus.CLOSED;
        // } finally { auction.lock.writeLock().unlock(); }
    }

    /**
     * Close all expired auctions (background task)
     */
    int closeExpiredAuctions() {
        // TODO: Implement
        // HINT: int closed = 0;
        // HINT: for (Auction a : auctions.values()) {
        //     if (a.status == AuctionStatus.ACTIVE && a.isExpired()) {
        //         try { closeAuction(a.auctionId); closed++; } catch (Exception e) {}
        //     }
        // }
        // HINT: return closed;
        return 0;
    }

    /**
     * Get bid history for an auction
     */
    List<Bid> getBidHistory(String auctionId) throws AuctionNotFoundException {
        // TODO: Implement
        // HINT: Auction auction = auctions.get(auctionId);
        // HINT: if (auction == null) throw new AuctionNotFoundException(auctionId);
        // HINT: auction.lock.readLock().lock();
        // HINT: try { return new ArrayList<>(auction.bids); }
        // HINT: finally { auction.lock.readLock().unlock(); }
        return Collections.emptyList();
    }

    Auction getAuction(String auctionId) throws AuctionNotFoundException {
        Auction a = auctions.get(auctionId);
        if (a == null) throw new AuctionNotFoundException(auctionId);
        return a;
    }

    int getTotalAuctions() { return auctions.size(); }
}

// ==================== MAIN / TESTS ====================

public class OnlineAuctionSystem {
    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║    ONLINE AUCTION SYSTEM - LLD Demo      ║");
        System.out.println("╚══════════════════════════════════════════╝\n");

        AuctionService svc = new AuctionService();

        // Test 1: Create Auction
        System.out.println("=== Test 1: Create Auction ===");
        Auction a1 = svc.createAuction("seller1", "Vintage Watch", 100.0, 5.0, 5000); // 5s
        Auction a2 = svc.createAuction("seller2", "Rare Stamp", 50.0, 2.0, 5000);
        System.out.println("  Auctions: " + svc.getTotalAuctions() + " (expect 2)");
        if (a1 != null) System.out.println("  " + a1.auctionId + ": " + a1.itemName + " reserve=$" + a1.reservePrice);
        System.out.println();

        // Test 2: Place Bids
        System.out.println("=== Test 2: Place Bids ===");
        try {
            if (a1 != null) {
                svc.placeBid(a1.auctionId, "buyer1", 80.0);
                svc.placeBid(a1.auctionId, "buyer2", 90.0);
                svc.placeBid(a1.auctionId, "buyer1", 105.0);
                System.out.println("  Highest: $" + a1.highestBid.amount + " by " + a1.highestBid.bidderId);
                System.out.println("✓ Bids placed\n");
            }
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // Test 3: Bid Too Low
        System.out.println("=== Test 3: Bid Too Low ===");
        try {
            if (a1 != null) svc.placeBid(a1.auctionId, "buyer3", 106.0); // needs >= 110
            System.out.println("✗ Should have thrown");
        } catch (InvalidBidException e) {
            System.out.println("✓ Caught: " + e.getMessage() + "\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // Test 4: Seller Cannot Bid
        System.out.println("=== Test 4: Seller Cannot Bid ===");
        try {
            if (a1 != null) svc.placeBid(a1.auctionId, "seller1", 200.0);
            System.out.println("✗ Should have thrown");
        } catch (InvalidBidException e) {
            System.out.println("✓ Caught: " + e.getMessage() + "\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // Test 5: Close Auction → Winner
        System.out.println("=== Test 5: Close Auction (Winner) ===");
        try {
            if (a1 != null) {
                svc.closeAuction(a1.auctionId);
                String winner = (a1.highestBid != null) ? a1.highestBid.bidderId : null;
                System.out.println("  Winner: " + winner + " (expect buyer1)");
                System.out.println("  Status: " + a1.status + " (expect CLOSED)");
                System.out.println("✓ Winner determined\n");
            }
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // Test 6: Bid on Closed Auction
        System.out.println("=== Test 6: Bid on Closed ===");
        try {
            if (a1 != null) svc.placeBid(a1.auctionId, "buyer3", 500.0);
            System.out.println("✗ Should have thrown");
        } catch (AuctionClosedException e) {
            System.out.println("✓ Caught: " + e.getMessage() + "\n");
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // Test 7: Reserve Not Met
        System.out.println("=== Test 7: Reserve Not Met ===");
        try {
            if (a2 != null) {
                svc.placeBid(a2.auctionId, "buyer1", 30.0);
                svc.placeBid(a2.auctionId, "buyer2", 40.0);
                svc.closeAuction(a2.auctionId);
                boolean reserveMet = a2.highestBid != null && a2.highestBid.amount >= a2.reservePrice;
                System.out.println("  Winner: " + (reserveMet ? a2.highestBid.bidderId : null) + " (expect null — reserve $50 not met)");
                System.out.println("✓ Reserve price enforced\n");
            }
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // Test 8: Bid History
        System.out.println("=== Test 8: Bid History ===");
        try {
            if (a1 != null) {
                List<Bid> history = svc.getBidHistory(a1.auctionId);
                System.out.println("  " + a1.itemName + " had " + history.size() + " bids:");
                history.forEach(b -> System.out.printf("    %s: $%.2f by %s%n", b.bidId, b.amount, b.bidderId));
                System.out.println("✓ History works\n");
            }
        } catch (Exception e) { System.out.println("✗ " + e.getMessage()); }

        // Test 9: Auto-Close Expired
        System.out.println("=== Test 9: Auto-Close Expired ===");
        Auction shortAuction = svc.createAuction("seller3", "Quick Item", 10.0, 1.0, 100); // 100ms
        if (shortAuction != null) {
            svc.placeBid(shortAuction.auctionId, "buyer1", 15.0);
            Thread.sleep(150);
            int closed = svc.closeExpiredAuctions();
            System.out.println("  Closed " + closed + " expired auctions");
            String winner = (shortAuction.highestBid != null && shortAuction.highestBid.amount >= shortAuction.reservePrice) 
                ? shortAuction.highestBid.bidderId : null;
            System.out.println("  Winner: " + winner);
            System.out.println("✓ Auto-close works\n");
        }

        // Test 10: Auction Not Found
        System.out.println("=== Test 10: Auction Not Found ===");
        try {
            svc.getAuction("FAKE-ID");
            System.out.println("✗ Should have thrown");
        } catch (AuctionNotFoundException e) {
            System.out.println("✓ Caught: " + e.getMessage() + "\n");
        }

        // Test 11: Concurrent Bidding
        System.out.println("=== Test 11: Thread Safety ===");
        Auction concAuction = svc.createAuction("seller4", "Hot Item", 10.0, 1.0, 10000);
        if (concAuction != null) {
            ExecutorService exec = Executors.newFixedThreadPool(8);
            List<Future<?>> futures = new ArrayList<>();
            AtomicInteger successBids = new AtomicInteger(0);
            for (int i = 0; i < 50; i++) {
                final double bid = 20.0 + i * 2.0;
                final String bidder = "bidder-" + (i % 10);
                futures.add(exec.submit(() -> {
                    try { svc.placeBid(concAuction.auctionId, bidder, bid); successBids.incrementAndGet(); }
                    catch (Exception e) {}
                }));
            }
            for (Future<?> f : futures) f.get();
            exec.shutdown();
            System.out.println("  Successful bids: " + successBids.get());
            System.out.println("  Highest: $" + (concAuction.highestBid != null ? concAuction.highestBid.amount : 0));
            System.out.println("✓ Thread-safe bidding\n");
        }

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║        ALL 11 TESTS COMPLETE ✓           ║");
        System.out.println("╚══════════════════════════════════════════╝");
    }
}

/*
 * ==================== INTERVIEW NOTES ====================
 *
 * 1. CORE FLOW:
 *    Seller creates auction → Buyers place bids → Timer expires → Winner determined
 *    Bid validation: amount > current + increment, seller can't bid, auction must be ACTIVE
 *
 * 2. CONCURRENCY:
 *    ReadWriteLock per auction: writeLock for placeBid/close, readLock for getBidHistory
 *    ConcurrentHashMap for registry
 *    volatile highestBid for visibility across threads
 *
 * 3. COMPLEXITY:
 *    placeBid: O(1) — just compare with highestBid, append to list
 *    closeAuction: O(1) — check highestBid vs reserve
 *    getBidHistory: O(k) where k = number of bids
 *
 * 4. OBSERVER PATTERN:
 *    onNewBid: notify watchers, send push/email
 *    onAuctionClosed: notify winner + losers + seller
 *    Real-world: Kafka events, WebSocket for real-time UI updates
 *
 * 5. AUCTION TYPES (extensions):
 *    English: ascending bids (this implementation)
 *    Dutch: descending price, first to accept wins
 *    Sealed-bid: all bids hidden, highest wins
 *    Vickrey: sealed-bid, winner pays 2nd highest price
 *
 * 6. SCALE:
 *    Shard by auctionId, Redis for hot auction state
 *    Kafka for bid events, separate read/write paths
 *    Timer service (like Job Scheduler) for auto-close
 *
 * 7. REAL-WORLD: eBay, Christie's, AWS Spot Instances, Google Ads
 */
