import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// ===== EXCEPTIONS =====

class WebhookNotFoundException extends Exception {
    public WebhookNotFoundException(String id) { super("Webhook not found: " + id); }
}

class InvalidWebhookException extends Exception {
    public InvalidWebhookException(String msg) { super("Invalid webhook: " + msg); }
}

// ===== ENUMS =====

enum DeliveryStatus { PENDING, SUCCESS, FAILED, RETRYING }

enum EventType { ORDER_CREATED, ORDER_UPDATED, PAYMENT_SUCCESS, PAYMENT_FAILED, USER_SIGNUP }

// ===== DOMAIN CLASSES =====

/**
 * A webhook subscription — URL that gets called when events happen
 */
class Webhook {
    private final String id;
    private final String url;                    // endpoint to POST to
    private final String secret;                  // for signing payloads (HMAC)
    private final Set<EventType> subscribedEvents;
    private boolean active;
    private final LocalDateTime createdAt;
    
    public Webhook(String url, String secret, EventType... events) {
        this.id = "WH-" + UUID.randomUUID().toString().substring(0, 6);
        this.url = url;
        this.secret = secret;
        this.subscribedEvents = new HashSet<>(Arrays.asList(events));
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }
    
    public String getId() { return id; }
    public String getUrl() { return url; }
    public String getSecret() { return secret; }
    public Set<EventType> getSubscribedEvents() { return subscribedEvents; }
    public boolean isActive() { return active; }
    public void setActive(boolean a) { this.active = a; }
    
    public boolean isSubscribedTo(EventType event) { return subscribedEvents.contains(event); }
    
    @Override
    public String toString() { return id + "[" + url + ", events=" + subscribedEvents + ", active=" + active + "]"; }
}

/**
 * An event payload to deliver
 */
class WebhookEvent {
    private final String id;
    private final EventType type;
    private final Map<String, String> payload;
    private final LocalDateTime timestamp;
    
    public WebhookEvent(EventType type, Map<String, String> payload) {
        this.id = "EVT-" + UUID.randomUUID().toString().substring(0, 6);
        this.type = type;
        this.payload = payload != null ? new HashMap<>(payload) : new HashMap<>();
        this.timestamp = LocalDateTime.now();
    }
    
    public String getId() { return id; }
    public EventType getType() { return type; }
    public Map<String, String> getPayload() { return Collections.unmodifiableMap(payload); }
    public LocalDateTime getTimestamp() { return timestamp; }
    
    @Override
    public String toString() { return id + "[" + type + ", " + payload + "]"; }
}

/**
 * Record of a delivery attempt
 */
class DeliveryAttempt {
    private final String id;
    private final String webhookId;
    private final String eventId;
    private DeliveryStatus status;
    private int attemptNumber;
    private final int maxRetries;
    private int responseCode;           // HTTP response code (200, 500, etc.)
    private String responseBody;
    private final LocalDateTime createdAt;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime nextRetryAt;
    
    public DeliveryAttempt(String webhookId, String eventId, int maxRetries) {
        this.id = "DLV-" + UUID.randomUUID().toString().substring(0, 6);
        this.webhookId = webhookId;
        this.eventId = eventId;
        this.status = DeliveryStatus.PENDING;
        this.attemptNumber = 0;
        this.maxRetries = maxRetries;
        this.createdAt = LocalDateTime.now();
    }
    
    public String getId() { return id; }
    public String getWebhookId() { return webhookId; }
    public String getEventId() { return eventId; }
    public DeliveryStatus getStatus() { return status; }
    public int getAttemptNumber() { return attemptNumber; }
    public int getMaxRetries() { return maxRetries; }
    public int getResponseCode() { return responseCode; }
    public String getResponseBody() { return responseBody; }
    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    
    public void setStatus(DeliveryStatus s) { this.status = s; }
    public void setResponseCode(int c) { this.responseCode = c; }
    public void setResponseBody(String b) { this.responseBody = b; }
    public void incrementAttempt() { this.attemptNumber++; this.lastAttemptAt = LocalDateTime.now(); }
    public void setNextRetryAt(LocalDateTime t) { this.nextRetryAt = t; }
    
    @Override
    public String toString() {
        return id + "[wh=" + webhookId + ", evt=" + eventId + ", " + status 
            + ", attempt=" + attemptNumber + "/" + maxRetries 
            + ", code=" + responseCode + "]";
    }
}

// ===== INTERFACE =====

/**
 * Simulates HTTP delivery — Strategy pattern for testability
 * In real system: actual HTTP POST call
 */
interface HttpClient {
    /** Returns HTTP status code (200=success, 500=failure) */
    int post(String url, String payload, String signature);
}

/**
 * Default: simulates delivery (90% success rate)
 */
class SimulatedHttpClient implements HttpClient {
    private final Random random = new Random();
    private final double successRate;
    
    public SimulatedHttpClient(double successRate) {
        this.successRate = successRate;
    }
    
    @Override
    public int post(String url, String payload, String signature) {
        // Simulate network call
        try { Thread.sleep(20); } catch (InterruptedException e) {}
        return random.nextDouble() < successRate ? 200 : 500;
    }
}

/**
 * Always succeeds — for predictable testing
 */
class AlwaysSuccessClient implements HttpClient {
    @Override
    public int post(String url, String payload, String signature) { return 200; }
}

/**
 * Always fails — for testing retries
 */
class AlwaysFailClient implements HttpClient {
    @Override
    public int post(String url, String payload, String signature) { return 500; }
}

// ===== SERVICE =====

/**
 * Webhook Delivery System - Low Level Design (LLD)
 * 
 * PROBLEM: Design a system that can:
 * 1. Register webhook endpoints (URL + subscribed events)
 * 2. Publish events → deliver to all matching webhooks
 * 3. Retry failed deliveries with exponential backoff
 * 4. Track delivery attempts and status
 * 5. Sign payloads with HMAC for security
 * 6. Deactivate failing webhooks after too many failures
 * 
 * KEY CONCEPTS:
 * - Fan-out: one event → delivered to N webhooks
 * - Retry with exponential backoff: 1s, 2s, 4s, 8s...
 * - Idempotency: include event ID so receiver can deduplicate
 * - Signing: HMAC(secret, payload) so receiver verifies authenticity
 * 
 * PATTERNS: Strategy (HttpClient), Observer (event → webhooks)
 */
class WebhookService {
    private final Map<String, Webhook> webhooks;               // webhookId → Webhook
    private final Map<String, DeliveryAttempt> deliveries;     // deliveryId → DeliveryAttempt
    private final Map<String, WebhookEvent> events;            // eventId → Event
    private HttpClient httpClient;
    private final int maxRetries;
    private final int maxConsecutiveFailures;                   // deactivate webhook after this many
    private final Map<String, Integer> consecutiveFailures;    // webhookId → failure count
    private final AtomicInteger totalDelivered;
    private final AtomicInteger totalFailed;
    
    public WebhookService(HttpClient httpClient, int maxRetries, int maxConsecutiveFailures) {
        this.webhooks = new ConcurrentHashMap<>();
        this.deliveries = new ConcurrentHashMap<>();
        this.events = new ConcurrentHashMap<>();
        this.httpClient = httpClient;
        this.maxRetries = maxRetries;
        this.maxConsecutiveFailures = maxConsecutiveFailures;
        this.consecutiveFailures = new ConcurrentHashMap<>();
        this.totalDelivered = new AtomicInteger(0);
        this.totalFailed = new AtomicInteger(0);
    }
    
    // ===== REGISTER WEBHOOK =====
    
    /**
     * Register a new webhook endpoint
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate URL is not empty
     * 2. Validate at least one event subscribed
     * 3. Create Webhook, store in map
     * 4. Init consecutive failures counter to 0
     * 5. Return webhook
     */
    public Webhook registerWebhook(String url, String secret, EventType... events) throws InvalidWebhookException {
        // TODO: Implement
        // HINT: if (url == null || url.isEmpty()) throw new InvalidWebhookException("URL required");
        // HINT: if (events.length == 0) throw new InvalidWebhookException("At least one event required");
        // HINT: Webhook wh = new Webhook(url, secret, events);
        // HINT: webhooks.put(wh.getId(), wh);
        // HINT: consecutiveFailures.put(wh.getId(), 0);
        // HINT: System.out.println("  ✓ Registered: " + wh);
        // HINT: return wh;
        return null;
    }
    
    /**
     * Deactivate a webhook
     */
    public void deactivateWebhook(String webhookId) throws WebhookNotFoundException {
        // TODO: Implement
        // HINT: Webhook wh = webhooks.get(webhookId);
        // HINT: if (wh == null) throw new WebhookNotFoundException(webhookId);
        // HINT: wh.setActive(false);
        // HINT: System.out.println("  🚫 Deactivated: " + webhookId);
    }
    
    // ===== PUBLISH EVENT =====
    
    /**
     * Publish an event — deliver to all matching active webhooks
     * 
     * IMPLEMENTATION HINTS:
     * 1. Create WebhookEvent, store in events map
     * 2. Find all active webhooks subscribed to this event type
     * 3. For each matching webhook, create DeliveryAttempt and call deliver()
     * 4. Return list of delivery attempts created
     */
    public List<DeliveryAttempt> publishEvent(EventType type, Map<String, String> payload) {
        // TODO: Implement
        // HINT: WebhookEvent event = new WebhookEvent(type, payload);
        // HINT: events.put(event.getId(), event);
        // HINT: System.out.println("  📤 Event published: " + event);
        //
        // HINT: List<DeliveryAttempt> attempts = new ArrayList<>();
        // HINT: for (Webhook wh : webhooks.values()) {
        //     if (wh.isActive() && wh.isSubscribedTo(type)) {
        //         DeliveryAttempt da = new DeliveryAttempt(wh.getId(), event.getId(), maxRetries);
        //         deliveries.put(da.getId(), da);
        //         deliver(da, wh, event);
        //         attempts.add(da);
        //     }
        // }
        // HINT: return attempts;
        return null;
    }
    
    // ===== DELIVER =====
    
    /**
     * Attempt to deliver a webhook with retry logic
     * 
     * IMPLEMENTATION HINTS:
     * 1. Increment attempt number
     * 2. Build payload string from event (JSON-like)
     * 3. Generate signature: simple hash of secret + payload (HMAC in real system)
     * 4. Call httpClient.post(url, payload, signature)
     * 5. If 2xx response:
     *    → Set status=SUCCESS, set responseCode
     *    → Reset consecutive failures for this webhook
     *    → Increment totalDelivered
     * 6. If non-2xx:
     *    → Increment consecutive failures
     *    → If attempts <= maxRetries: set status=RETRYING, schedule retry (exponential backoff)
     *    → Else: set status=FAILED, increment totalFailed
     *    → If consecutive failures >= max: deactivate webhook
     */
    private void deliver(DeliveryAttempt da, Webhook wh, WebhookEvent event) {
        // TODO: Implement
        // HINT: da.incrementAttempt();
        // HINT: da.setStatus(DeliveryStatus.RETRYING);
        //
        // HINT: String payloadStr = event.getType() + ":" + event.getPayload().toString();
        // HINT: String signature = "sha256=" + Math.abs((wh.getSecret() + payloadStr).hashCode());
        //
        // HINT: int responseCode = httpClient.post(wh.getUrl(), payloadStr, signature);
        // HINT: da.setResponseCode(responseCode);
        //
        // HINT: if (responseCode >= 200 && responseCode < 300) {
        //     da.setStatus(DeliveryStatus.SUCCESS);
        //     consecutiveFailures.put(wh.getId(), 0);
        //     totalDelivered.incrementAndGet();
        //     System.out.println("    ✅ " + da.getId() + " → " + wh.getUrl() + " [" + responseCode + "]");
        // } else {
        //     consecutiveFailures.merge(wh.getId(), 1, Integer::sum);
        //     
        //     if (da.getAttemptNumber() <= maxRetries) {
        //         da.setStatus(DeliveryStatus.RETRYING);
        //         // Exponential backoff: 2^attempt seconds
        //         long backoffMs = (long) Math.pow(2, da.getAttemptNumber()) * 1000;
        //         System.out.println("    🔄 Retry " + da.getAttemptNumber() + "/" + maxRetries 
        //             + " in " + backoffMs + "ms for " + da.getId());
        //         // In real system: schedule retry after backoff. Here: retry immediately for demo
        //         deliver(da, wh, event);
        //     } else {
        //         da.setStatus(DeliveryStatus.FAILED);
        //         totalFailed.incrementAndGet();
        //         System.out.println("    ❌ " + da.getId() + " failed after " + maxRetries + " retries");
        //     }
        //     
        //     // Auto-deactivate if too many consecutive failures
        //     if (consecutiveFailures.getOrDefault(wh.getId(), 0) >= maxConsecutiveFailures) {
        //         wh.setActive(false);
        //         System.out.println("    ⚠️ Webhook " + wh.getId() + " deactivated (too many failures)");
        //     }
        // }
    }
    
    // ===== QUERIES =====
    
    /**
     * Get delivery attempts for a specific event
     */
    public List<DeliveryAttempt> getDeliveriesForEvent(String eventId) {
        // TODO: Implement
        // HINT: List<DeliveryAttempt> result = new ArrayList<>();
        // HINT: for (DeliveryAttempt da : deliveries.values()) {
        //     if (da.getEventId().equals(eventId)) result.add(da);
        // }
        // HINT: return result;
        return null;
    }
    
    /**
     * Get delivery attempts for a specific webhook
     */
    public List<DeliveryAttempt> getDeliveriesForWebhook(String webhookId) {
        // TODO: Implement
        // HINT: List<DeliveryAttempt> result = new ArrayList<>();
        // HINT: for (DeliveryAttempt da : deliveries.values()) {
        //     if (da.getWebhookId().equals(webhookId)) result.add(da);
        // }
        // HINT: return result;
        return null;
    }
    
    /**
     * Get all failed deliveries
     */
    public List<DeliveryAttempt> getFailedDeliveries() {
        // TODO: Implement
        // HINT: List<DeliveryAttempt> result = new ArrayList<>();
        // HINT: for (DeliveryAttempt da : deliveries.values()) {
        //     if (da.getStatus() == DeliveryStatus.FAILED) result.add(da);
        // }
        // HINT: return result;
        return null;
    }
    
    public Webhook getWebhook(String id) { return webhooks.get(id); }
    
    public void setHttpClient(HttpClient client) { this.httpClient = client; }
    
    /**
     * Display status
     */
    public void displayStatus() {
        System.out.println("\n--- Webhook Service Status ---");
        System.out.println("Webhooks: " + webhooks.size() + " (active: " 
            + webhooks.values().stream().filter(Webhook::isActive).count() + ")");
        System.out.println("Events: " + events.size() + ", Deliveries: " + deliveries.size());
        System.out.println("Delivered: " + totalDelivered.get() + ", Failed: " + totalFailed.get());
    }
}

// ===== MAIN TEST CLASS =====

public class WebhookSystem {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Webhook Delivery System LLD ===\n");
        
        // Test 1: Register webhooks and deliver successfully
        System.out.println("=== Test 1: Successful Delivery ===");
        WebhookService service = new WebhookService(new AlwaysSuccessClient(), 3, 5);
        try {
            Webhook wh1 = service.registerWebhook("https://api.shop.com/webhooks", "secret123",
                EventType.ORDER_CREATED, EventType.PAYMENT_SUCCESS);
            Webhook wh2 = service.registerWebhook("https://analytics.io/events", "key456",
                EventType.ORDER_CREATED, EventType.USER_SIGNUP);
            
            Map<String, String> payload = new HashMap<>();
            payload.put("orderId", "ORD-001");
            payload.put("amount", "99.99");
            
            List<DeliveryAttempt> attempts = service.publishEvent(EventType.ORDER_CREATED, payload);
            System.out.println("✓ Delivered to " + (attempts != null ? attempts.size() : 0) + " webhooks");
            if (attempts != null) attempts.forEach(a -> System.out.println("  " + a));
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 2: Event only goes to subscribed webhooks
        System.out.println("=== Test 2: Event Filtering ===");
        try {
            List<DeliveryAttempt> attempts = service.publishEvent(EventType.USER_SIGNUP,
                Map.of("userId", "u123", "email", "alice@mail.com"));
            System.out.println("✓ USER_SIGNUP delivered to " + (attempts != null ? attempts.size() : 0) 
                + " webhooks (expect 1 — only analytics)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 3: Retry on failure
        System.out.println("=== Test 3: Retry on Failure ===");
        WebhookService failService = new WebhookService(new AlwaysFailClient(), 2, 10);
        try {
            Webhook wh = failService.registerWebhook("https://flaky.api.com/hook", "sec",
                EventType.PAYMENT_FAILED);
            
            List<DeliveryAttempt> attempts = failService.publishEvent(EventType.PAYMENT_FAILED,
                Map.of("orderId", "ORD-002", "reason", "card_declined"));
            System.out.println("✓ Delivery attempts:");
            if (attempts != null) {
                for (DeliveryAttempt a : attempts) {
                    System.out.println("  " + a);
                    System.out.println("  Status: " + a.getStatus() + ", Attempts: " + a.getAttemptNumber());
                }
            }
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 4: Auto-deactivate after consecutive failures
        System.out.println("=== Test 4: Auto-Deactivate ===");
        WebhookService autoDeact = new WebhookService(new AlwaysFailClient(), 0, 3); // 0 retries, deactivate after 3 failures
        try {
            Webhook wh = autoDeact.registerWebhook("https://dead-endpoint.com/hook", "s",
                EventType.ORDER_CREATED);
            String whId = wh.getId();
            
            // Send 3 events → 3 consecutive failures → should deactivate
            for (int i = 0; i < 3; i++) {
                autoDeact.publishEvent(EventType.ORDER_CREATED, Map.of("i", String.valueOf(i)));
            }
            
            Webhook afterFails = autoDeact.getWebhook(whId);
            System.out.println("✓ Active after 3 failures: " + (afterFails != null ? afterFails.isActive() : "null") 
                + " (expect false)");
            
            // 4th event should NOT be delivered (webhook deactivated)
            List<DeliveryAttempt> noDelivery = autoDeact.publishEvent(EventType.ORDER_CREATED, Map.of("x", "y"));
            System.out.println("  Deliveries for deactivated: " + (noDelivery != null ? noDelivery.size() : 0) + " (expect 0)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 5: Deactivate webhook manually
        System.out.println("=== Test 5: Manual Deactivate ===");
        try {
            WebhookService svc = new WebhookService(new AlwaysSuccessClient(), 0, 10);
            Webhook wh = svc.registerWebhook("https://temp.com/hook", "s", EventType.ORDER_CREATED);
            svc.deactivateWebhook(wh.getId());
            
            List<DeliveryAttempt> attempts = svc.publishEvent(EventType.ORDER_CREATED, Map.of("x", "y"));
            System.out.println("✓ Deliveries after deactivate: " + (attempts != null ? attempts.size() : 0) + " (expect 0)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 6: Query deliveries for event
        System.out.println("=== Test 6: Query Deliveries ===");
        List<DeliveryAttempt> failed = service.getFailedDeliveries();
        System.out.println("✓ Failed deliveries: " + (failed != null ? failed.size() : 0));
        System.out.println();
        
        // Test 7: Exception — empty URL
        System.out.println("=== Test 7: Exception - Invalid Webhook ===");
        try {
            service.registerWebhook("", "secret", EventType.ORDER_CREATED);
            System.out.println("✗ Should have thrown");
        } catch (InvalidWebhookException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        }
        System.out.println();
        
        // Test 8: Exception — no events
        System.out.println("=== Test 8: Exception - No Events ===");
        try {
            service.registerWebhook("https://x.com/hook", "secret");
            System.out.println("✗ Should have thrown");
        } catch (InvalidWebhookException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        }
        System.out.println();
        
        // Test 9: Exception — webhook not found
        System.out.println("=== Test 9: Exception - Not Found ===");
        try {
            service.deactivateWebhook("FAKE-ID");
            System.out.println("✗ Should have thrown");
        } catch (WebhookNotFoundException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        }
        System.out.println();
        
        // Display status
        service.displayStatus();
        
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION:
 * =====================
 * 
 * 1. DELIVERY GUARANTEES:
 *    At-least-once: retry until success (may deliver twice)
 *    Receiver must be idempotent (use event ID to deduplicate)
 *    Include event ID + timestamp in every delivery
 * 
 * 2. RETRY WITH EXPONENTIAL BACKOFF:
 *    Attempt 1: immediately
 *    Attempt 2: wait 2s
 *    Attempt 3: wait 4s
 *    Attempt 4: wait 8s
 *    Add jitter (random 0-1s) to prevent thundering herd
 * 
 * 3. PAYLOAD SIGNING (SECURITY):
 *    signature = HMAC-SHA256(secret, payload)
 *    Send in header: X-Webhook-Signature: sha256=abc123
 *    Receiver computes same HMAC, compares to verify
 *    Prevents spoofed webhook calls
 * 
 * 4. FAN-OUT:
 *    One event → delivered to N webhooks in parallel
 *    Use thread pool or message queue for async delivery
 * 
 * 5. CIRCUIT BREAKER:
 *    Track consecutive failures per webhook
 *    After N failures → deactivate (stop wasting resources)
 *    Notify webhook owner via email
 *    Allow manual reactivation
 * 
 * 6. ARCHITECTURE:
 *    Event Source → Queue (SQS/Kafka) → Delivery Workers → HTTP POST
 *                                            ↓
 *                                    Retry Queue (delayed)
 *                                            ↓
 *                                    Dead Letter Queue (failed)
 * 
 * 7. REAL-WORLD: Stripe, GitHub, Shopify, Twilio webhooks
 * 
 * 8. API:
 *    POST /webhooks              — register
 *    DELETE /webhooks/{id}       — deactivate
 *    POST /events                — publish event
 *    GET  /webhooks/{id}/deliveries — delivery history
 *    POST /webhooks/{id}/test    — send test event
 */
