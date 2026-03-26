import java.time.*;
import java.util.*;
import java.util.concurrent.*;

// ===== EXCEPTIONS =====

class TopicNotFoundException extends Exception {
    public TopicNotFoundException(String name) { super("Topic not found: " + name); }
}

class SubscriberNotFoundException extends Exception {
    public SubscriberNotFoundException(String id) { super("Subscriber not found: " + id); }
}

// ===== INTERFACE =====

/**
 * Subscriber — receives messages from topics it's subscribed to
 */
interface Subscriber {
    String getId();
    void onMessage(String topic, Message message);
}

// ===== DOMAIN CLASSES =====

class Message {
    private final String id;
    private final String body;
    private final Map<String, String> headers;
    private final LocalDateTime timestamp;
    
    public Message(String body) {
        this.id = "MSG-" + UUID.randomUUID().toString().substring(0, 6);
        this.body = body;
        this.headers = new HashMap<>();
        this.timestamp = LocalDateTime.now();
    }
    
    public Message(String body, Map<String, String> headers) {
        this(body);
        if (headers != null) this.headers.putAll(headers);
    }
    
    public String getId() { return id; }
    public String getBody() { return body; }
    public Map<String, String> getHeaders() { return Collections.unmodifiableMap(headers); }
    public LocalDateTime getTimestamp() { return timestamp; }
    
    @Override
    public String toString() { return id + "[" + body + "]"; }
}

class Topic {
    private final String name;
    private final Set<String> subscriberIds;   // who's subscribed
    private final List<Message> messageHistory; // retained messages
    private final int retentionLimit;           // max messages to keep
    
    public Topic(String name, int retentionLimit) {
        this.name = name;
        this.subscriberIds = ConcurrentHashMap.newKeySet();
        this.messageHistory = new ArrayList<>();
        this.retentionLimit = retentionLimit;
    }
    
    public String getName() { return name; }
    public Set<String> getSubscriberIds() { return subscriberIds; }
    public List<Message> getMessageHistory() { return Collections.unmodifiableList(messageHistory); }
    
    public void addSubscriber(String subId) { subscriberIds.add(subId); }
    public void removeSubscriber(String subId) { subscriberIds.remove(subId); }
    
    public void addMessage(Message msg) {
        messageHistory.add(msg);
        if (messageHistory.size() > retentionLimit) messageHistory.remove(0);
    }
    
    @Override
    public String toString() { return name + "[subs=" + subscriberIds.size() + ", msgs=" + messageHistory.size() + "]"; }
}

/**
 * Simple subscriber that collects messages (for testing)
 */
class PrintSubscriber implements Subscriber {
    private final String id;
    private final List<Message> received;
    
    public PrintSubscriber(String id) {
        this.id = id;
        this.received = new ArrayList<>();
    }
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Add message to received list
     * 2. Print notification
     */
    @Override
    public void onMessage(String topic, Message message) {
        // TODO: Implement
        // HINT: received.add(message);
        // HINT: System.out.println("    📩 " + id + " got [" + topic + "]: " + message.getBody());
    }
    
    @Override
    public String getId() { return id; }
    public List<Message> getReceived() { return received; }
    public int getReceivedCount() { return received.size(); }
}

/**
 * Filtered subscriber — only receives messages matching a filter
 */
class FilteredSubscriber implements Subscriber {
    private final String id;
    private final String filterKey;
    private final String filterValue;
    private final List<Message> received;
    
    public FilteredSubscriber(String id, String filterKey, String filterValue) {
        this.id = id;
        this.filterKey = filterKey;
        this.filterValue = filterValue;
        this.received = new ArrayList<>();
    }
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Check if message header contains filterKey with matching filterValue
     * 2. If matches → add to received, print
     * 3. If not → skip silently
     */
    @Override
    public void onMessage(String topic, Message message) {
        // TODO: Implement
        // HINT: String val = message.getHeaders().get(filterKey);
        // HINT: if (filterValue.equals(val)) {
        //     received.add(message);
        //     System.out.println("    📩 " + id + " got [" + topic + "] (filtered): " + message.getBody());
        // }
    }
    
    @Override
    public String getId() { return id; }
    public List<Message> getReceived() { return received; }
}

// ===== SERVICE =====

/**
 * Pub-Sub System - Low Level Design (LLD)
 * 
 * PROBLEM: Design a publish-subscribe messaging system that can:
 * 1. Create topics
 * 2. Subscribe/unsubscribe to topics
 * 3. Publish messages to topics → fan-out to all subscribers
 * 4. Support filtered subscribers (receive only matching messages)
 * 5. Message history with retention
 * 6. Dead letter handling for failed deliveries
 * 
 * KEY CONCEPTS:
 * - Decoupling: publishers don't know about subscribers
 * - Fan-out: one message → N subscribers
 * - At-least-once: retry delivery on failure
 * 
 * PATTERNS: Observer (topic → subscribers), Strategy (filter logic)
 */
class PubSubService {
    private final Map<String, Topic> topics;
    private final Map<String, Subscriber> subscribers;
    private int totalPublished;
    private int totalDelivered;
    
    public PubSubService() {
        this.topics = new ConcurrentHashMap<>();
        this.subscribers = new ConcurrentHashMap<>();
        this.totalPublished = 0;
        this.totalDelivered = 0;
    }
    
    // ===== TOPIC MANAGEMENT =====
    
    /**
     * Create a new topic
     * 
     * IMPLEMENTATION HINTS:
     * 1. Check if topic already exists
     * 2. Create Topic with retention limit
     * 3. Store in map
     */
    public Topic createTopic(String name, int retentionLimit) {
        // TODO: Implement
        // HINT: if (topics.containsKey(name)) return topics.get(name); // idempotent
        // HINT: Topic topic = new Topic(name, retentionLimit);
        // HINT: topics.put(name, topic);
        // HINT: System.out.println("  ✓ Topic created: " + name);
        // HINT: return topic;
        return null;
    }
    
    public Topic createTopic(String name) { return createTopic(name, 100); }
    
    // ===== SUBSCRIBE / UNSUBSCRIBE =====
    
    /**
     * Subscribe to a topic
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get topic → throw if not found
     * 2. Register subscriber in subscribers map
     * 3. Add subscriber ID to topic's subscriber set
     */
    public void subscribe(String topicName, Subscriber subscriber) throws TopicNotFoundException {
        // TODO: Implement
        // HINT: Topic topic = topics.get(topicName);
        // HINT: if (topic == null) throw new TopicNotFoundException(topicName);
        // HINT: subscribers.put(subscriber.getId(), subscriber);
        // HINT: topic.addSubscriber(subscriber.getId());
        // HINT: System.out.println("  ✓ " + subscriber.getId() + " subscribed to " + topicName);
    }
    
    /**
     * Unsubscribe from a topic
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get topic → throw if not found
     * 2. Remove subscriber ID from topic
     */
    public void unsubscribe(String topicName, String subscriberId) throws TopicNotFoundException {
        // TODO: Implement
        // HINT: Topic topic = topics.get(topicName);
        // HINT: if (topic == null) throw new TopicNotFoundException(topicName);
        // HINT: topic.removeSubscriber(subscriberId);
        // HINT: System.out.println("  ✓ " + subscriberId + " unsubscribed from " + topicName);
    }
    
    // ===== PUBLISH =====
    
    /**
     * Publish a message to a topic → deliver to all subscribers
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get topic → throw if not found
     * 2. Add message to topic's history
     * 3. For each subscriber ID in topic:
     *    a. Get Subscriber object
     *    b. Call subscriber.onMessage(topicName, message)
     *    c. Increment totalDelivered
     * 4. Increment totalPublished
     * 5. Return the message
     */
    public Message publish(String topicName, Message message) throws TopicNotFoundException {
        // TODO: Implement
        // HINT: Topic topic = topics.get(topicName);
        // HINT: if (topic == null) throw new TopicNotFoundException(topicName);
        //
        // HINT: topic.addMessage(message);
        // HINT: totalPublished++;
        // HINT: System.out.println("  📤 Published to " + topicName + ": " + message);
        //
        // HINT: for (String subId : topic.getSubscriberIds()) {
        //     Subscriber sub = subscribers.get(subId);
        //     if (sub != null) {
        //         sub.onMessage(topicName, message);
        //         totalDelivered++;
        //     }
        // }
        // HINT: return message;
        return null;
    }
    
    /** Convenience: publish with just a body string */
    public Message publish(String topicName, String body) throws TopicNotFoundException {
        return publish(topicName, new Message(body));
    }
    
    // ===== QUERIES =====
    
    /**
     * Get message history for a topic
     */
    public List<Message> getHistory(String topicName) throws TopicNotFoundException {
        // TODO: Implement
        // HINT: Topic topic = topics.get(topicName);
        // HINT: if (topic == null) throw new TopicNotFoundException(topicName);
        // HINT: return topic.getMessageHistory();
        return null;
    }
    
    /**
     * Get subscriber count for a topic
     */
    public int getSubscriberCount(String topicName) throws TopicNotFoundException {
        // TODO: Implement
        // HINT: Topic topic = topics.get(topicName);
        // HINT: if (topic == null) throw new TopicNotFoundException(topicName);
        // HINT: return topic.getSubscriberIds().size();
        return 0;
    }
    
    public Topic getTopic(String name) { return topics.get(name); }
    public int getTotalPublished() { return totalPublished; }
    public int getTotalDelivered() { return totalDelivered; }
    
    public void displayStatus() {
        System.out.println("\n--- Pub/Sub Status ---");
        System.out.println("Topics: " + topics.size() + ", Subscribers: " + subscribers.size());
        System.out.println("Published: " + totalPublished + ", Delivered: " + totalDelivered);
        topics.values().forEach(t -> System.out.println("  " + t));
    }
}

// ===== MAIN TEST CLASS =====

public class PubSubSystem {
    public static void main(String[] args) {
        System.out.println("=== Pub/Sub System LLD ===\n");
        
        PubSubService service = new PubSubService();
        
        // Test 1: Create topics
        System.out.println("=== Test 1: Create Topics ===");
        service.createTopic("orders");
        service.createTopic("payments");
        service.createTopic("notifications");
        System.out.println();
        
        // Test 2: Subscribe
        System.out.println("=== Test 2: Subscribe ===");
        PrintSubscriber orderProcessor = new PrintSubscriber("order-processor");
        PrintSubscriber analytics = new PrintSubscriber("analytics");
        PrintSubscriber emailService = new PrintSubscriber("email-service");
        try {
            service.subscribe("orders", orderProcessor);
            service.subscribe("orders", analytics);       // both listen to orders
            service.subscribe("payments", analytics);     // analytics also listens to payments
            service.subscribe("notifications", emailService);
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 3: Publish → fan-out to subscribers
        System.out.println("=== Test 3: Publish (Fan-out) ===");
        try {
            service.publish("orders", "New order: ORD-001");
            System.out.println("✓ order-processor received: " + orderProcessor.getReceivedCount());
            System.out.println("✓ analytics received: " + analytics.getReceivedCount());
            System.out.println("✓ email-service received: " + emailService.getReceivedCount() + " (expect 0 — not subscribed to orders)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 4: Publish to another topic
        System.out.println("=== Test 4: Publish to Payments ===");
        try {
            service.publish("payments", "Payment received: $99.99");
            System.out.println("✓ analytics total: " + analytics.getReceivedCount() + " (expect 2 — orders + payments)");
            System.out.println("✓ order-processor total: " + orderProcessor.getReceivedCount() + " (expect 1 — only orders)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 5: Unsubscribe
        System.out.println("=== Test 5: Unsubscribe ===");
        try {
            service.unsubscribe("orders", "analytics");
            service.publish("orders", "New order: ORD-002");
            System.out.println("✓ analytics total: " + analytics.getReceivedCount() + " (expect still 2 — unsubscribed)");
            System.out.println("✓ order-processor total: " + orderProcessor.getReceivedCount() + " (expect 2)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 6: Filtered subscriber
        System.out.println("=== Test 6: Filtered Subscriber ===");
        try {
            FilteredSubscriber highPriority = new FilteredSubscriber("urgent-handler", "priority", "HIGH");
            service.subscribe("notifications", highPriority);
            
            Map<String, String> lowHeaders = new HashMap<>(); lowHeaders.put("priority", "LOW");
            Map<String, String> highHeaders = new HashMap<>(); highHeaders.put("priority", "HIGH");
            
            service.publish("notifications", new Message("Low priority alert", lowHeaders));
            service.publish("notifications", new Message("URGENT: Server down!", highHeaders));
            service.publish("notifications", new Message("Another high alert", highHeaders));
            
            System.out.println("✓ urgent-handler received: " + highPriority.getReceived().size() + " (expect 2 — only HIGH)");
            System.out.println("✓ email-service received: " + emailService.getReceivedCount() + " (expect 3 — all notifications)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 7: Message history
        System.out.println("=== Test 7: Message History ===");
        try {
            List<Message> history = service.getHistory("orders");
            System.out.println("✓ Orders history: " + (history != null ? history.size() : 0) + " messages");
            if (history != null) history.forEach(m -> System.out.println("    " + m));
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 8: Subscriber count
        System.out.println("=== Test 8: Subscriber Count ===");
        try {
            System.out.println("✓ orders subs: " + service.getSubscriberCount("orders") + " (expect 1 — analytics left)");
            System.out.println("✓ notifications subs: " + service.getSubscriberCount("notifications") + " (expect 2)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 9: Topic not found
        System.out.println("=== Test 9: Exception - Topic Not Found ===");
        try {
            service.publish("nonexistent", "test");
            System.out.println("✗ Should have thrown");
        } catch (TopicNotFoundException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        }
        System.out.println();
        
        // Test 10: Subscribe to non-existent topic
        System.out.println("=== Test 10: Exception - Subscribe to Missing Topic ===");
        try {
            service.subscribe("fake-topic", new PrintSubscriber("x"));
            System.out.println("✗ Should have thrown");
        } catch (TopicNotFoundException e) {
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
 * 1. PUB/SUB vs QUEUE:
 *    Pub/Sub: one message → many subscribers (fan-out)
 *    Queue:   one message → one consumer (load balancing)
 *    
 *    Pub/Sub: "broadcast" — all subscribers get every message
 *    Queue:   "work distribution" — each message processed once
 * 
 * 2. DELIVERY GUARANTEES:
 *    At-most-once:  fire and forget (fastest, may lose)
 *    At-least-once: retry until ACK (may duplicate)
 *    Exactly-once:  dedup + idempotency (hardest)
 * 
 * 3. MESSAGE ORDERING:
 *    Per-topic ordering (Kafka partitions)
 *    Per-key ordering (same user → same partition)
 *    Global ordering: very hard at scale
 * 
 * 4. CONSUMER GROUPS:
 *    Multiple instances of same service → each gets subset
 *    Like Kafka consumer groups
 *    Load balancing within group, fan-out across groups
 * 
 * 5. PERSISTENCE:
 *    In-memory: fast but lost on crash (used here)
 *    Disk: Kafka (append-only log), durable
 *    Retention: keep last N messages or last T hours
 * 
 * 6. ARCHITECTURE:
 *    Publisher → Broker (topic) → Subscribers
 *    Broker handles: routing, persistence, delivery, retry
 *    
 *    Kafka: partitioned log, consumer offsets
 *    SQS/SNS: managed queue + pub/sub
 *    Redis Pub/Sub: fast, no persistence
 * 
 * 7. REAL-WORLD: Kafka, RabbitMQ, AWS SNS/SQS, Redis Pub/Sub, Google Pub/Sub
 * 
 * 8. API:
 *    POST /topics                    — create topic
 *    POST /topics/{name}/subscribe   — subscribe
 *    DELETE /topics/{name}/subscribe — unsubscribe
 *    POST /topics/{name}/publish     — publish message
 *    GET  /topics/{name}/messages    — get history
 */
