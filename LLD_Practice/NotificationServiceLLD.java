import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// ===== CUSTOM EXCEPTION CLASSES =====

/**
 * Exception thrown when notification fails to send
 * WHEN TO THROW:
 * - Channel sender fails
 * - Max retries exceeded
 * - Invalid recipient
 */
class NotificationFailedException extends Exception {
    private String notificationId;
    private NotificationChannel channel;
    
    public NotificationFailedException(String notificationId, NotificationChannel channel, String message) {
        super("Notification " + notificationId + " failed via " + channel + ": " + message);
        this.notificationId = notificationId;
        this.channel = channel;
    }
    
    public String getNotificationId() { return notificationId; }
    public NotificationChannel getChannel() { return channel; }
}

/**
 * Exception thrown when notification channel is invalid
 * WHEN TO THROW:
 * - Channel not registered/supported
 * - User disabled channel in preferences
 */
class InvalidChannelException extends Exception {
    private NotificationChannel channel;
    
    public InvalidChannelException(NotificationChannel channel, String message) {
        super("Invalid channel " + channel + ": " + message);
        this.channel = channel;
    }
    
    public NotificationChannel getChannel() { return channel; }
}

// ===== ENUMS =====

enum NotificationChannel { 
    EMAIL,   // Email notifications
    SMS,     // Text message
    PUSH     // Mobile push notification
}

enum NotificationPriority { 
    LOW,       // Can be delayed (newsletters, updates)
    MEDIUM,    // Normal priority (order confirmations)
    HIGH,      // Important (security alerts)
    CRITICAL   // Immediate (system failures, fraud)
}

enum NotificationStatus { 
    PENDING,   // Not yet sent
    SENT,      // Successfully delivered
    FAILED,    // Failed after retries
    RETRYING   // Currently retrying
}

// ===== INTERFACE - STRATEGY PATTERN =====

/**
 * Strategy interface for different notification channels
 */
interface NotificationSender {
    boolean     send(String userId, String message);
    NotificationChannel getChannel();
}

// ===== CHANNEL IMPLEMENTATIONS =====

class EmailSender implements NotificationSender {
    /**
     * IMPLEMENTATION HINTS:
     * 1. Simulate email sending (in real: SMTP, SendGrid, AWS SES)
     * 2. Log the notification
     * 3. Return true for success
     * 4. In real system: handle email validation, templates, attachments
     */
    @Override
    public boolean send(String userId, String message) {
        // HINT: System.out.println("    📧 EMAIL → " + userId + ": " + message);
        // HINT: Simulate network call with success/failure
        // HINT: return true; (for now, always succeed)
        System.out.println("    📧 EMAIL → " + userId + ": " + message);
        return true;
    }
    
    @Override
    public NotificationChannel getChannel() { return NotificationChannel.EMAIL; }
}

class SmsSender implements NotificationSender {
    /**
     * IMPLEMENTATION HINTS:
     * 1. Simulate SMS sending (in real: Twilio, AWS SNS)
     * 2. Log the notification
     * 3. Handle phone number validation
     * 4. Return true for success
     */
    @Override
    public boolean send(String userId, String message) {
        // HINT: System.out.println("    📱 SMS → " + userId + ": " + message);
        // HINT: return true;
        System.out.println("    📱 SMS → " + userId + ": " + message);
        return true;
    }
    
    @Override
    public NotificationChannel getChannel() { return NotificationChannel.SMS; }
}

class PushSender implements NotificationSender {
    /**
     * IMPLEMENTATION HINTS:
     * 1. Simulate push notification (in real: FCM, APNs)
     * 2. Log the notification
     * 3. Handle device token validation
     * 4. Return true for success
     */
    @Override
    public boolean send(String userId, String message) {
        // HINT: System.out.println("    🔔 PUSH → " + userId + ": " + message);
        // HINT: return true;
        System.out.println("    🔔 PUSH → " + userId + ": " + message);
        return true;
    }
    
    @Override
    public NotificationChannel getChannel() { return NotificationChannel.PUSH; }
}

// ===== DOMAIN CLASSES =====

/**
 * Represents a notification
 */
class Notification {
    String id;
    String userId;
    String message;
    NotificationChannel channel;
    NotificationPriority priority;
    NotificationStatus status;
    int retryCount;
    LocalDateTime createdAt;
    LocalDateTime sentAt;
    
    public Notification(String userId, String message, NotificationChannel channel, NotificationPriority priority) {
        this.id = "NOTIF-" + UUID.randomUUID().toString().substring(0, 6);
        this.userId = userId;
        this.message = message;
        this.channel = channel;
        this.priority = priority;
        this.status = NotificationStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return id + "[" + channel + "," + priority + "," + status + ",retries=" + retryCount + "]";
    }
}

/**
 * User's notification preferences
 */
class UserPreference {
    String userId;
    Set<NotificationChannel> enabledChannels;
    
    public UserPreference(String userId, NotificationChannel... channels) {
        this.userId = userId;
        this.enabledChannels = new HashSet<>(Arrays.asList(channels));
    }
    
    public boolean isChannelEnabled(NotificationChannel channel) {
        return enabledChannels.contains(channel);
    }
}

/**
 * Notification Service - Low Level Design (LLD)
 * 
 * PROBLEM STATEMENT:
 * Design a multi-channel notification service that can:
 * 1. Send notifications via Email, SMS, Push
 * 2. Respect user preferences (opt-in/opt-out)
 * 3. Retry failed notifications
 * 4. Support priority levels
 * 5. Broadcast to multiple channels
 * 6. Bulk send to multiple users
 * 
 * REQUIREMENTS:
 * - Functional: Send, retry, broadcast, respect preferences
 * - Non-Functional: Reliable, handle failures, track metrics
 * 
 * INTERVIEW HINTS:
 * - Discuss observer pattern for notifications
 * - Talk about message queues for async processing
 * - Mention idempotency and deduplication
 * - Consider rate limiting per channel
 * - Discuss delivery guarantees (at-least-once, exactly-once)
 */
class NotificationService {
    private Map<NotificationChannel, NotificationSender> senders;
    private Map<String, UserPreference> userPreferences;
    private List<Notification> allNotifications;
    private int maxRetries;
    private AtomicInteger totalSent = new AtomicInteger(0);
    private AtomicInteger totalFailed = new AtomicInteger(0);
    
    public NotificationService(int maxRetries) {
        this.senders = new HashMap<>();
        this.userPreferences = new HashMap<>();
        this.allNotifications = new ArrayList<>();
        this.maxRetries = maxRetries;
    }
    
    /**
     * Register a notification sender for a channel
     * 
     * @param channel Channel type
     * @param sender Implementation for this channel
     */
    public void registerSender(NotificationChannel channel, NotificationSender sender) {
        // HINT: senders.put(channel, sender);
        // HINT: System.out.println("Registered: " + channel + " sender");
        senders.put(channel, sender);
        System.out.println("Registered: " + channel + " sender");
    }
    
    /**
     * Set user's notification preferences
     * 
     * @param preference User's channel preferences
     */
    public void setUserPreference(UserPreference preference) {
        // HINT: userPreferences.put(preference.userId, preference);
        userPreferences.put(preference.userId, preference);
    }
    
    /**
     * Send notification with retry logic
     * 
     * IMPLEMENTATION HINTS:
     * 1. Create Notification object with PENDING status
     * 2. Check user preferences - if channel disabled, throw InvalidChannelException
     * 3. Get appropriate sender for the channel
     * 4. Retry loop: attempt up to maxRetries times
     *    - Call sender.send()
     *    - If success: set status to SENT, update sentAt, break
     *    - If failure: increment retryCount, set status to RETRYING
     * 5. After retries: if still failed, set status to FAILED
     * 6. Store notification in allNotifications list
     * 7. Update totalSent or totalFailed counters
     * 8. Return the notification object
     * 
     * INTERVIEW DISCUSSION:
     * - How to handle retry backoff?
     * - Should retries be synchronous or async?
     * - How to prevent duplicate notifications?
     * 
     * @param userId User to notify
     * @param message Notification message
     * @param channel Channel to use
     * @param priority Priority level
     * @return Notification object with status
     * @throws InvalidChannelException if channel not enabled for user
     */
    public synchronized Notification sendNotification(String userId, String message,
            NotificationChannel channel, NotificationPriority priority) throws InvalidChannelException {
        // HINT: Notification notif = new Notification(userId, message, channel, priority);
        // HINT: UserPreference pref = userPreferences.get(userId);
        // HINT: if (pref != null && !pref.isChannelEnabled(channel)) 
        //           throw new InvalidChannelException(channel, "Channel disabled by user");
        // HINT: NotificationSender sender = senders.get(channel);
        // HINT: for (int attempt = 0; attempt <= maxRetries; attempt++) {
        //     if (sender.send(userId, message)) {
        //         notif.status = NotificationStatus.SENT;
        //         notif.sentAt = LocalDateTime.now();
        //         totalSent.incrementAndGet();
        //         break;
        //     } else {
        //         notif.retryCount++;
        //         notif.status = NotificationStatus.RETRYING;
        //     }
        // }
        // HINT: if (notif.status != NotificationStatus.SENT) {
        //     notif.status = NotificationStatus.FAILED;
        //     totalFailed.incrementAndGet();
        // }
        // HINT: allNotifications.add(notif);
        // HINT: return notif;
        Notification notif = new Notification(userId, message, channel, priority);
        UserPreference pref = userPreferences.get(userId);
        if(pref!=null && !pref.isChannelEnabled(channel)) throw new InvalidChannelException(channel, message);
        NotificationSender sender = senders.get(channel);
        for (int i = 0; i <= maxRetries; i++) {
            if(sender.send(userId, message)){
                notif.status=NotificationStatus.SENT;
                notif.sentAt=LocalDateTime.now();
                totalSent.incrementAndGet();
                break;
            }else{
                notif.retryCount++;
                notif.status=NotificationStatus.RETRYING;
            }
        }
        if(notif.status!=NotificationStatus.SENT){
            notif.status=NotificationStatus.FAILED;
            totalFailed.incrementAndGet();
        }
        allNotifications.add(notif);
        return notif;
    }
    
    /**
     * Broadcast to all user's enabled channels
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get user preferences
     * 2. If no preferences, send to all channels
     * 3. Loop through enabled channels
     * 4. Send notification to each channel
     * 5. Collect all Notification objects
     * 6. Return list of all notifications sent
     * 
     * @param userId User to notify
     * @param message Message to broadcast
     * @param priority Priority level
     * @return List of notifications (one per channel)
     */
    public List<Notification> broadcastToUser(String userId, String message, NotificationPriority priority) {
        // HINT: List<Notification> notifications = new ArrayList<>();
        // HINT: UserPreference pref = userPreferences.get(userId);
        // HINT: Set<NotificationChannel> channels = (pref != null) ? pref.enabledChannels : new HashSet<>(senders.keySet());
        // HINT: for (NotificationChannel channel : channels) {
        //     try {
        //         Notification n = sendNotification(userId, message, channel, priority);
        //         notifications.add(n);
        //     } catch (InvalidChannelException e) { /* skip disabled channel */ }
        // }
        // HINT: return notifications;
        List<Notification> notifications=new ArrayList<>();
        UserPreference userPreference=userPreferences.get(userId);
        Set<NotificationChannel> channels = (userPreference!=null)?userPreference.enabledChannels:new HashSet<>(senders.keySet());
        for(NotificationChannel channel:channels){
            try {
                Notification notification=sendNotification(userId, message, channel, priority);
                notifications.add(notification);
            } catch (InvalidChannelException e) {

            }
        }
        return notifications;
    }
    
    /**
     * Bulk send to multiple users
     * 
     * IMPLEMENTATION HINTS:
     * 1. Loop through all userIds
     * 2. Send notification to each user
     * 3. Collect all results (even failures)
     * 4. Return list of all notifications
     * 
     * @param userIds List of users to notify
     * @param message Message to send
     * @param channel Channel to use
     * @param priority Priority level
     * @return List of all notifications sent
     */
    public List<Notification> bulkSend(List<String> userIds, String message,
            NotificationChannel channel, NotificationPriority priority) {
        // HINT: List<Notification> results = new ArrayList<>();
        // HINT: for (String userId : userIds) {
        //     try {
        //         Notification n = sendNotification(userId, message, channel, priority);
        //         results.add(n);
        //     } catch (InvalidChannelException e) { /* add failed notification */ }
        // }
        // HINT: return results;
        List<Notification> results=new ArrayList<>();
        for(String userId:userIds){
            try{
                Notification notification=sendNotification(userId, message, channel, priority);
                results.add(notification);
            }catch(InvalidChannelException e){

            }
        }
        return results;
    }
    
    /**
     * Get notifications by user
     * 
     * @param userId User ID
     * @return List of user's notifications
     */
    public List<Notification> getUserNotifications(String userId) {
        // HINT: return allNotifications.stream()
        //     .filter(n -> n.userId.equals(userId))
        //     .collect(Collectors.toList());
        return allNotifications.stream().filter(x->x.userId.equals(userId)).collect(Collectors.toList());
    }
    
    /**
     * Get notifications by status
     * 
     * @param status Status to filter by
     * @return List of notifications with that status
     */
    public List<Notification> getNotificationsByStatus(NotificationStatus status) {
        // HINT: return allNotifications.stream()
        //     .filter(n -> n.status == status)
        //     .collect(Collectors.toList());
        return allNotifications.stream().filter(x->x.status==status).collect(Collectors.toList());
    }
    
    /**
     * Display service status
     */
    public void displayStatus() {
        System.out.println("\n--- Notification Service Status ---");
        System.out.println("Registered channels: " + senders.keySet());
        System.out.println("Users with preferences: " + userPreferences.size());
        System.out.println("Total notifications: " + allNotifications.size());
        System.out.println("Success: " + totalSent.get() + ", Failed: " + totalFailed.get());
        
        // Status breakdown
        for (NotificationStatus status : NotificationStatus.values()) {
            long count = allNotifications.stream().filter(n -> n.status == status).count();
            if (count > 0) {
                System.out.println("  " + status + ": " + count);
            }
        }
    }
}

// ===== MAIN TEST CLASS =====

public class NotificationServiceLLD {
    public static void main(String[] args) {
        System.out.println("=== Notification Service Test Cases ===\n");
        
        NotificationService service = new NotificationService(2);
        
        // Register senders
        service.registerSender(NotificationChannel.EMAIL, new EmailSender());
        service.registerSender(NotificationChannel.SMS, new SmsSender());
        service.registerSender(NotificationChannel.PUSH, new PushSender());
        
        // Set user preferences
        service.setUserPreference(new UserPreference("alice", 
            NotificationChannel.EMAIL, NotificationChannel.PUSH));
        service.setUserPreference(new UserPreference("bob", 
            NotificationChannel.SMS));
        
        // Test Case 1: Single Notification
        System.out.println("=== Test Case 1: Single Notification ===");
        try {
            Notification n = service.sendNotification("alice", "Your order has shipped!", 
                NotificationChannel.EMAIL, NotificationPriority.MEDIUM);
            System.out.println("✓ Notification sent: " + n);
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 2: Broadcast to User
        System.out.println("=== Test Case 2: Broadcast to All Channels ===");
        List<Notification> broadcasts = service.broadcastToUser("alice", 
            "Security alert: New login detected", NotificationPriority.HIGH);
        System.out.println("✓ Broadcast sent via " + broadcasts.size() + " channels");
        broadcasts.forEach(n -> System.out.println("  " + n));
        System.out.println();
        
        // Test Case 3: Bulk Send
        System.out.println("=== Test Case 3: Bulk Send to Multiple Users ===");
        List<Notification> bulk = service.bulkSend(
            Arrays.asList("alice", "bob", "charlie"),
            "Flash sale: 50% off!",
            NotificationChannel.PUSH,
            NotificationPriority.LOW
        );
        System.out.println("✓ Bulk sent to " + bulk.size() + " users");
        System.out.println();
        
        // Test Case 4: Priority Notifications
        System.out.println("=== Test Case 4: Different Priorities ===");
        try {
            service.sendNotification("bob", "Your package is out for delivery", 
                NotificationChannel.SMS, NotificationPriority.MEDIUM);
            service.sendNotification("bob", "Payment failed - update card", 
                NotificationChannel.SMS, NotificationPriority.CRITICAL);
            System.out.println("✓ Sent notifications with different priorities");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 5: Exception - Invalid Channel
        System.out.println("=== Test Case 5: Exception - Disabled Channel ===");
        try {
            // Alice has SMS disabled
            service.sendNotification("alice", "Test SMS", 
                NotificationChannel.SMS, NotificationPriority.LOW);
            System.out.println("✗ Should have thrown InvalidChannelException");
        } catch (InvalidChannelException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
            System.out.println("  Channel: " + e.getChannel());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 6: Get User Notifications
        System.out.println("=== Test Case 6: Get User Notifications ===");
        List<Notification> aliceNotifs = service.getUserNotifications("alice");
        System.out.println("Alice's notifications: " + aliceNotifs.size());
        aliceNotifs.forEach(n -> System.out.println("  " + n));
        System.out.println();
        
        // Test Case 7: Get by Status
        System.out.println("=== Test Case 7: Filter by Status ===");
        List<Notification> sent = service.getNotificationsByStatus(NotificationStatus.SENT);
        List<Notification> failed = service.getNotificationsByStatus(NotificationStatus.FAILED);
        System.out.println("Sent: " + sent.size() + ", Failed: " + failed.size());
        System.out.println();
        
        service.displayStatus();
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION TOPICS:
 * ============================
 * 
 * 1. DESIGN PATTERNS:
 *    Strategy Pattern:
 *      - Different senders for each channel
 *      - Easy to add new channels (Slack, WhatsApp, etc.)
 *    
 *    Observer Pattern:
 *      - Subscribe to events
 *      - Notify all observers on event
 *      - Decouples event source from handlers
 *    
 *    Template Method:
 *      - Common notification flow
 *      - Channel-specific implementation
 * 
 * 2. RETRY STRATEGIES:
 *    Simple Retry:
 *      - Fixed number of attempts (used here)
 *      - Immediate retry
 *    
 *    Exponential Backoff:
 *      - Wait 1s, 2s, 4s, 8s between retries
 *      - Reduces load on failing service
 *      - Industry standard
 *    
 *    Circuit Breaker:
 *      - Stop retrying if channel consistently fails
 *      - Auto-recover after timeout
 *      - Prevents cascade failures
 * 
 * 3. ASYNC VS SYNC:
 *    Synchronous:
 *      - Send immediately (used here for simplicity)
 *      - User waits for confirmation
 *      - Simple but blocks request
 *    
 *    Asynchronous:
 *      - Queue notification, return immediately
 *      - Background workers process queue
 *      - Better UX, more complex
 *      - Use: Kafka, RabbitMQ, AWS SQS
 * 
 * 4. MESSAGE QUEUE ARCHITECTURE:
 *    ```
 *    App → Queue → Workers → Channels
 *                      ↓
 *                   Database
 *    ```
 *    - Decouples sender from delivery
 *    - Scales workers independently
 *    - Guaranteed delivery with persistence
 *    - Dead letter queue for failures
 * 
 * 5. PRIORITY HANDLING:
 *    - Separate queues per priority
 *    - CRITICAL → immediate processing
 *    - HIGH → 1 minute delay max
 *    - MEDIUM → 5 minutes
 *    - LOW → best effort, can be batched
 * 
 * 6. USER PREFERENCES:
 *    - Opt-in/opt-out per channel
 *    - Quiet hours (don't disturb 10pm-8am)
 *    - Frequency limits (max 1 email/day)
 *    - Category preferences (marketing vs transactional)
 * 
 * 7. IDEMPOTENCY & DEDUPLICATION:
 *    - Same notification shouldn't be sent twice
 *    - Use idempotency key (hash of userId + message + timestamp)
 *    - Check recent notifications before sending
 *    - Redis SET with TTL for dedup
 * 
 * 8. MONITORING & METRICS:
 *    - Delivery rate per channel
 *    - Retry rate
 *    - Average time to deliver
 *    - Bounce rate (email)
 *    - Unsubscribe rate
 *    - Channel effectiveness
 * 
 * 9. ADVANCED FEATURES:
 *    - Template engine (personalized messages)
 *    - Multi-language support
 *    - Rich content (HTML emails, media in push)
 *    - Deep links in notifications
 *    - A/B testing for message variants
 *    - Scheduled notifications
 *    - Batch aggregation (digest emails)
 * 
 * 10. SCALABILITY:
 *     Single Service:
 *       - Works for small scale
 *       - Simple architecture
 *     
 *     Microservices:
 *       - Separate service per channel
 *       - Independent scaling
 *       - Fault isolation
 *     
 *     Cloud Services:
 *       - AWS SNS/SES
 *       - Twilio for SMS
 *       - Firebase for Push
 * 
 * 11. API DESIGN:
 *     POST /notifications              - Send single notification
 *     POST /notifications/broadcast    - Broadcast to user
 *     POST /notifications/bulk         - Bulk send
 *     GET  /notifications/{id}         - Get notification
 *     GET  /users/{id}/notifications   - User's notifications
 *     PUT  /users/{id}/preferences     - Update preferences
 *     POST /notifications/{id}/retry   - Manual retry
 */
