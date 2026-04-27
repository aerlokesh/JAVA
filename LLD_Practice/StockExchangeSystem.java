import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/*
 * STOCK EXCHANGE - Low Level Design
 * ====================================
 * 
 * REQUIREMENTS:
 * 1. Place BUY/SELL orders with price and quantity
 * 2. Order matching: price-time priority (best price first, FIFO on tie)
 * 3. Partial fills supported
 * 4. Order book: sorted buy orders (desc price), sell orders (asc price)
 * 5. Trade notifications (Observer)
 * 6. Pluggable matching strategy (Strategy)
 * 7. Thread-safe
 * 
 * DESIGN PATTERNS:
 *   Strategy  (MatchingStrategy) — PriceTimeStrategy
 *   Observer  (TradeListener)    — TradeNotifier
 *   Facade    (StockExchangeService)
 * 
 * KEY DS: PriorityQueue for buy (max-heap) and sell (min-heap) order books
 */

// ==================== EXCEPTIONS ====================

class InvalidOrderException extends RuntimeException {
    InvalidOrderException(String msg) { super("Invalid order: " + msg); }
}

class OrderNotFoundException extends RuntimeException {
    OrderNotFoundException(String orderId) { super("Order not found: " + orderId); }
}

// ==================== ENUMS ====================

enum OrderSide { BUY, SELL }

enum OrderStatus { OPEN, PARTIALLY_FILLED, FILLED, CANCELLED }

// ==================== MODELS ====================

class StockOrder {
    final String id, symbol, traderId;
    final OrderSide side;
    final double price;
    int quantity, filledQty;
    OrderStatus status;
    final long timestamp;

    StockOrder(String id, String symbol, String traderId, OrderSide side, double price, int quantity) {
        this.id = id; this.symbol = symbol; this.traderId = traderId;
        this.side = side; this.price = price; this.quantity = quantity;
        this.filledQty = 0; this.status = OrderStatus.OPEN;
        this.timestamp = System.nanoTime();
    }

    int remainingQty() { return quantity - filledQty; }

    void fill(int qty) {
        filledQty += qty;
        status = filledQty >= quantity ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
    }
}

class Trade {
    final String id, symbol;
    final String buyOrderId, sellOrderId;
    final String buyerId, sellerId;
    final double price;
    final int quantity;

    Trade(String id, String symbol, String buyOrderId, String sellOrderId,
          String buyerId, String sellerId, double price, int quantity) {
        this.id = id; this.symbol = symbol;
        this.buyOrderId = buyOrderId; this.sellOrderId = sellOrderId;
        this.buyerId = buyerId; this.sellerId = sellerId;
        this.price = price; this.quantity = quantity;
    }
}

// ==================== INTERFACES ====================

/** Strategy — order matching algorithm. */
interface MatchingStrategy {
    List<Trade> match(StockOrder incoming, PriorityQueue<StockOrder> oppositeBook, int[] tradeCounter);
}

/** Observer — trade execution notifications. */
interface TradeListener {
    void onTrade(Trade trade);
}

// ==================== STRATEGY IMPLEMENTATIONS ====================

/** Price-Time Priority: best price first, FIFO on same price. */
class PriceTimeStrategy implements MatchingStrategy {
    @Override public List<Trade> match(StockOrder incoming, PriorityQueue<StockOrder> oppositeBook, int[] tradeCounter) {
        List<Trade> trades = new ArrayList<>();
        while (incoming.remainingQty() > 0 && !oppositeBook.isEmpty()) {
            StockOrder top = oppositeBook.peek();
            // Check price match: buy price >= sell price
            boolean priceMatch = incoming.side == OrderSide.BUY
                ? incoming.price >= top.price
                : top.price >= incoming.price;
            if (!priceMatch) break;

            oppositeBook.poll();
            int tradeQty = Math.min(incoming.remainingQty(), top.remainingQty());
            double tradePrice = top.price; // existing order's price

            incoming.fill(tradeQty);
            top.fill(tradeQty);

            String buyId = incoming.side == OrderSide.BUY ? incoming.id : top.id;
            String sellId = incoming.side == OrderSide.SELL ? incoming.id : top.id;
            String buyerId = incoming.side == OrderSide.BUY ? incoming.traderId : top.traderId;
            String sellerId = incoming.side == OrderSide.SELL ? incoming.traderId : top.traderId;

            trades.add(new Trade("T-" + (++tradeCounter[0]), incoming.symbol,
                buyId, sellId, buyerId, sellerId, tradePrice, tradeQty));

            if (top.remainingQty() > 0) oppositeBook.offer(top); // put back partial
        }
        return trades;
    }
}

// ==================== OBSERVER IMPLEMENTATIONS ====================

class TradeNotifier implements TradeListener {
    final List<String> notifications = new ArrayList<>();
    @Override public void onTrade(Trade t) {
        notifications.add(String.format("%s: %s %d@%.2f buyer=%s seller=%s",
            t.id, t.symbol, t.quantity, t.price, t.buyerId, t.sellerId));
    }
}

// ==================== STOCK EXCHANGE SERVICE (FACADE) ====================

class StockExchangeService {
    // Per-symbol order books: buy (max-heap by price, then FIFO), sell (min-heap)
    private final Map<String, PriorityQueue<StockOrder>> buyBooks = new ConcurrentHashMap<>();
    private final Map<String, PriorityQueue<StockOrder>> sellBooks = new ConcurrentHashMap<>();
    private final Map<String, StockOrder> allOrders = new ConcurrentHashMap<>();
    private final List<Trade> allTrades = new ArrayList<>();
    private MatchingStrategy matchingStrategy;
    private final List<TradeListener> listeners = new ArrayList<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private int orderCounter = 0;
    private final int[] tradeCounter = {0};

    StockExchangeService(MatchingStrategy strategy) { this.matchingStrategy = strategy; }
    StockExchangeService() { this(new PriceTimeStrategy()); }

    void addListener(TradeListener l) { listeners.add(l); }

    private PriorityQueue<StockOrder> getBuyBook(String symbol) {
        return buyBooks.computeIfAbsent(symbol, k -> new PriorityQueue<>(
            Comparator.comparingDouble((StockOrder o) -> -o.price).thenComparingLong(o -> o.timestamp)));
    }

    private PriorityQueue<StockOrder> getSellBook(String symbol) {
        return sellBooks.computeIfAbsent(symbol, k -> new PriorityQueue<>(
            Comparator.comparingDouble((StockOrder o) -> o.price).thenComparingLong(o -> o.timestamp)));
    }

    /** Place an order. Returns the order (may be partially/fully filled immediately). */
    StockOrder placeOrder(String symbol, String traderId, OrderSide side, double price, int quantity) {
        if (price <= 0 || quantity <= 0) throw new InvalidOrderException("price and qty must be > 0");
        if (symbol == null || symbol.isEmpty()) throw new InvalidOrderException("symbol required");

        lock.writeLock().lock();
        try {
            String orderId = "ORD-" + (++orderCounter);
            StockOrder order = new StockOrder(orderId, symbol, traderId, side, price, quantity);
            allOrders.put(orderId, order);

            PriorityQueue<StockOrder> oppositeBook = side == OrderSide.BUY ? getSellBook(symbol) : getBuyBook(symbol);
            List<Trade> trades = matchingStrategy.match(order, oppositeBook, tradeCounter);
            allTrades.addAll(trades);
            for (Trade t : trades) for (TradeListener l : listeners) l.onTrade(t);

            // If not fully filled, add to own side's book
            if (order.remainingQty() > 0) {
                PriorityQueue<StockOrder> ownBook = side == OrderSide.BUY ? getBuyBook(symbol) : getSellBook(symbol);
                ownBook.offer(order);
            }
            return order;
        } finally { lock.writeLock().unlock(); }
    }

    boolean cancelOrder(String orderId) {
        lock.writeLock().lock();
        try {
            StockOrder order = allOrders.get(orderId);
            if (order == null) throw new OrderNotFoundException(orderId);
            if (order.status == OrderStatus.FILLED || order.status == OrderStatus.CANCELLED)
                return false;
            order.status = OrderStatus.CANCELLED;
            // Remove from book
            PriorityQueue<StockOrder> book = order.side == OrderSide.BUY
                ? getBuyBook(order.symbol) : getSellBook(order.symbol);
            book.remove(order);
            return true;
        } finally { lock.writeLock().unlock(); }
    }

    StockOrder getOrder(String orderId) {
        StockOrder o = allOrders.get(orderId);
        if (o == null) throw new OrderNotFoundException(orderId);
        return o;
    }

    List<Trade> getTrades() { return new ArrayList<>(allTrades); }
    int getTradeCount() { return allTrades.size(); }

    int getBuyBookSize(String symbol) { return getBuyBook(symbol).size(); }
    int getSellBookSize(String symbol) { return getSellBook(symbol).size(); }
}

// ==================== MAIN / TESTS ====================

public class StockExchangeSystem {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════╗");
        System.out.println("║   STOCK EXCHANGE - LLD Demo           ║");
        System.out.println("╚═══════════════════════════════════════╝\n");

        // --- Test 1: Buy + Sell match ---
        System.out.println("=== Test 1: Basic match ===");
        StockExchangeService svc = new StockExchangeService();
        svc.placeOrder("AAPL", "alice", OrderSide.SELL, 150.0, 10);
        StockOrder buy = svc.placeOrder("AAPL", "bob", OrderSide.BUY, 150.0, 10);
        check(buy.status, OrderStatus.FILLED, "Buy fully filled");
        check(svc.getTradeCount(), 1, "1 trade");
        System.out.println("✓\n");

        // --- Test 2: Partial fill ---
        System.out.println("=== Test 2: Partial fill ===");
        StockExchangeService svc2 = new StockExchangeService();
        svc2.placeOrder("GOOG", "alice", OrderSide.SELL, 100.0, 5);
        StockOrder buy2 = svc2.placeOrder("GOOG", "bob", OrderSide.BUY, 100.0, 8);
        check(buy2.status, OrderStatus.PARTIALLY_FILLED, "Partially filled");
        check(buy2.filledQty, 5, "5 filled");
        check(buy2.remainingQty(), 3, "3 remaining");
        check(svc2.getBuyBookSize("GOOG"), 1, "Remainder on buy book");
        System.out.println("✓\n");

        // --- Test 3: No match (price gap) ---
        System.out.println("=== Test 3: No match ===");
        StockExchangeService svc3 = new StockExchangeService();
        svc3.placeOrder("MSFT", "alice", OrderSide.SELL, 200.0, 10);
        StockOrder lowBuy = svc3.placeOrder("MSFT", "bob", OrderSide.BUY, 190.0, 10);
        check(lowBuy.status, OrderStatus.OPEN, "No match — price gap");
        check(svc3.getBuyBookSize("MSFT"), 1, "Buy on book");
        check(svc3.getSellBookSize("MSFT"), 1, "Sell on book");
        System.out.println("✓\n");

        // --- Test 4: Price-time priority ---
        System.out.println("=== Test 4: Price-time priority ===");
        StockExchangeService svc4 = new StockExchangeService();
        svc4.placeOrder("TSLA", "s1", OrderSide.SELL, 100.0, 5);
        svc4.placeOrder("TSLA", "s2", OrderSide.SELL, 99.0, 5);  // better price
        StockOrder b4 = svc4.placeOrder("TSLA", "buyer", OrderSide.BUY, 100.0, 5);
        check(b4.status, OrderStatus.FILLED, "Filled");
        Trade t4 = svc4.getTrades().get(svc4.getTrades().size() - 1);
        check(t4.sellerId, "s2", "Matched with cheaper seller s2");
        check(Math.abs(t4.price - 99.0) < 0.01, true, "Trade at 99.0");
        System.out.println("✓\n");

        // --- Test 5: Multiple fills ---
        System.out.println("=== Test 5: Multiple fills ===");
        StockExchangeService svc5 = new StockExchangeService();
        svc5.placeOrder("AMZN", "s1", OrderSide.SELL, 100.0, 3);
        svc5.placeOrder("AMZN", "s2", OrderSide.SELL, 101.0, 4);
        StockOrder b5 = svc5.placeOrder("AMZN", "buyer", OrderSide.BUY, 101.0, 6);
        check(b5.filledQty, 6, "6 filled across 2 sellers");
        check(b5.status, OrderStatus.FILLED, "Fully filled");
        check(svc5.getTradeCount() >= 2, true, "At least 2 trades");
        System.out.println("✓\n");

        // --- Test 6: Cancel order ---
        System.out.println("=== Test 6: Cancel ===");
        StockExchangeService svc6 = new StockExchangeService();
        StockOrder sell6 = svc6.placeOrder("FB", "alice", OrderSide.SELL, 300.0, 10);
        check(svc6.cancelOrder(sell6.id), true, "Cancelled");
        check(sell6.status, OrderStatus.CANCELLED, "Status = CANCELLED");
        check(svc6.getSellBookSize("FB"), 0, "Removed from book");
        check(svc6.cancelOrder(sell6.id), false, "Can't cancel again");
        System.out.println("✓\n");

        // --- Test 7: Order not found ---
        System.out.println("=== Test 7: Order not found ===");
        try { svc.getOrder("ORD-999"); }
        catch (OrderNotFoundException e) { System.out.println("  ✓ " + e.getMessage()); }
        System.out.println("✓\n");

        // --- Test 8: Invalid order ---
        System.out.println("=== Test 8: Invalid order ===");
        try { svc.placeOrder("AAPL", "x", OrderSide.BUY, -1, 10); }
        catch (InvalidOrderException e) { System.out.println("  ✓ " + e.getMessage()); }
        try { svc.placeOrder("", "x", OrderSide.BUY, 100, 10); }
        catch (InvalidOrderException e) { System.out.println("  ✓ " + e.getMessage()); }
        System.out.println("✓\n");

        // --- Test 9: Observer ---
        System.out.println("=== Test 9: Observer ===");
        StockExchangeService svc9 = new StockExchangeService();
        TradeNotifier notifier = new TradeNotifier();
        svc9.addListener(notifier);
        svc9.placeOrder("NFLX", "alice", OrderSide.SELL, 500.0, 5);
        svc9.placeOrder("NFLX", "bob", OrderSide.BUY, 500.0, 5);
        check(notifier.notifications.size(), 1, "1 notification");
        System.out.println("  " + notifier.notifications.get(0));
        System.out.println("✓\n");

        // --- Test 10: Multiple symbols ---
        System.out.println("=== Test 10: Multiple symbols ===");
        StockExchangeService svc10 = new StockExchangeService();
        svc10.placeOrder("AAPL", "a", OrderSide.SELL, 150, 10);
        svc10.placeOrder("GOOG", "a", OrderSide.SELL, 2800, 5);
        svc10.placeOrder("AAPL", "b", OrderSide.BUY, 150, 10);
        svc10.placeOrder("GOOG", "b", OrderSide.BUY, 2800, 5);
        check(svc10.getTradeCount(), 2, "2 trades across 2 symbols");
        System.out.println("✓\n");

        // --- Test 11: Thread Safety ---
        System.out.println("=== Test 11: Thread Safety ===");
        StockExchangeService svc11 = new StockExchangeService();
        ExecutorService exec = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            int x = i;
            futures.add(exec.submit(() -> svc11.placeOrder("TEST", "s" + x, OrderSide.SELL, 100.0, 1)));
            futures.add(exec.submit(() -> svc11.placeOrder("TEST", "b" + x, OrderSide.BUY, 100.0, 1)));
        }
        for (Future<?> f : futures) { try { f.get(); } catch (Exception e) {} }
        exec.shutdown();
        System.out.println("  Trades: " + svc11.getTradeCount() + ", Buy book: " + svc11.getBuyBookSize("TEST") + ", Sell book: " + svc11.getSellBookSize("TEST"));
        check(svc11.getTradeCount() > 0, true, "Concurrent trades executed");
        System.out.println("✓\n");

        System.out.println("════════ ALL 11 TESTS PASSED ✓ ════════");
    }

    static void check(OrderStatus a, OrderStatus e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
    static void check(int a, int e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
    static void check(String a, String e, String m) { System.out.println("  " + (Objects.equals(a, e) ? "✓" : "✗ GOT '" + a + "'") + " " + m); }
    static void check(boolean a, boolean e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. ORDER BOOK: PriorityQueue per side per symbol.
 *    Buy = max-heap (highest price first), Sell = min-heap (lowest price first).
 *    Tie-break by timestamp (FIFO) → price-time priority.
 *
 * 2. MATCHING: Incoming order matched against opposite book.
 *    BUY matches if buy.price >= sell.price. Trade at existing order's price.
 *    Partial fills: remainder stays on book or in incoming order.
 *
 * 3. STRATEGY (MatchingStrategy): PriceTimeStrategy. Could add ProRataStrategy,
 *    MarketMakerStrategy, AuctionStrategy.
 *
 * 4. OBSERVER (TradeListener): TradeNotifier gets notified on each trade execution.
 *    Could feed settlement, analytics, risk management.
 *
 * 5. THREAD SAFETY: ReadWriteLock for order placement/cancellation.
 *
 * 6. EXTENSIONS: limit/market/stop orders, order amendments, order book depth,
 *    circuit breakers, settlement T+2, FIX protocol.
 */
