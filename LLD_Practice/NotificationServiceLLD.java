import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

// ===== CUSTOM EXCEPTION CLASSES =====

class NotificationFailedException extends Exception {
    public NotificationFailedException(String msg) { super(msg); }
}

class InvalidChannelException extends Exception {
    public InvalidChannelException(String msg) { super(msg); }
}

// ===== ENUMS =====

enum NotificationChannel { EMAIL, SMS, PUSH }
enum NotificationPriority { LOW, MEDIUM, HIGH, CRITICAL }
enum NotificationStatus { PENDING, SENT, FAILED, RETRYING }

// ===== INTERFACE - STRATEGY PATTERN =====

interface NotificationSender {
    boolean send(String userId, String message);
    NotificationChannel getChannel();
}

// ===== CHANNEL IMPLEMENTATIONS =====

class EmailSender implements NotificationSender {
    @Override
    public boolean send(String userId, String message) {
        System.out.println("    📧 EMAIL → " + userId + ": " + message);
        return true;
    }
    @Override
    public NotificationChannel getChannel() { return NotificationChannel.EMAIL; }
}

class SmsSender implements NotificationSender {
    @Override
    public boolean send(String userId, String message) {
        System.out.println("    📱 SMS → " + userId + ": " + message);
        return true;
    }
    @Override
    public NotificationChannel getChannel() { return NotificationChannel.SMS; }
}

class PushSender implements NotificationSender {
    @Override
    public boolean send(String userId, String message) {
        System.out.println("    🔔 PUSH → " + userId + ": " + message);
        return true;
    }
    @Override
    public NotificationChannel getChannel() { return NotificationChannel.PUSH; }
}

// ===== DOMAIN CLASSES =====

class Notification {
    String id, userId, message;
    NotificationChannel channel;
    NotificationPriority priority;
    NotificationStatus status;
    int retryCount;
    LocalDateTime createdAt, sentAt;
    
    Notification(String userId, String message, NotificationChannel channel, NotificationPriority priority) {
        this.id = "NOTIF-" + UUID.randomUUID().toString().substring(0, 6);
        this.userId = userId;
        this.message = message;
        this.channel = channel;
        this.priority = priority;
        this.status = NotificationStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = LocalDateTime.now();
    }
}

class UserPreference {
    String userId;
    Set<NotificationChannel> enabledChannels;
    
    UserPreference(String userId, NotificationChannel... channels) {
        this.userId = userId;
        this.enabledChannels = new HashSet<>(Arrays.asList(channels));
    }
}

class NotificationService {
    Map<NotificationChannel, NotificationSender> senders;
    Map<String, UserPreference> preferences;
    List<Notification> allNotifications;
    int maxRetries;
    AtomicInteger totalSent = new AtomicInteger(0);
    AtomicInteger totalFailed = new AtomicInteger(0);
    
    NotificationService(int maxRetries) {
        senders = new HashMap<>();
        preferences = new HashMap<>();
        allNotifications = new ArrayList<>();
        this.maxRetries = maxRetries;
    }
    
    void registerSender(NotificationChannel channel, NotificationSender sender) {
        senders.put(channel, sender);
    }
    
    void setUserPreference(UserPreference pref) {
        preferences.put(pref.userId, pref);
    }
    
    /**
     * Send notification with retry
     * IMPLEMENTATION HINTS:
     * 1. Create Notification object
     * 2. Check user preferences (skip if channel disabled)
     * 3. Get appropriate sender
     * 4. Retry loop: attempt up to maxRetries times
     * 5. Update status based on result
     */
    public synchronized Notification sendNotification(String userId, String message,
            NotificationChannel channel, NotificationPriority priority) {
        // TODO: Implement
        // HINT: Check preferences.get(userId).enabledChannels.contains(channel)
        // HINT: for (int attempt = 0; attempt <= maxRetries; attempt++)
        return null;
    }
    
    /**
     * Broadcast to all user's enabled channels
     * IMPLEMENTATION HINTS:
     * 1. Get user preferences
     * 2. Send to each enabled channel
     * 3. Return list of all notifications
     */
    public List<Notification> broadcastToUser(String userId, String message, NotificationPriority priority) {
        // TODO: Implement
        return null;
    }
    
    /**
     * Bulk send to multiple users
     * IMPLEMENTATION HINTS:
     * 1. Loop through userIds
     * 2. Send notification to each
     * 3. Collect and return results
     */
    public List<Notification> bulkSend(List<String> userIds, String message,
            NotificationChannel channel, NotificationPriority priority) {
        // TODO: Implement
        return null;
    }
    
    void displayStatus() {
        System.out.println("\n--- Notification Service Status ---");
        System.out.println("Channels: " + senders.keySet());
        System.out.println("Sent: " + totalSent.get() + ", Failed: " + totalFailed.get());
    }
}

public class NotificationServiceLLD {
    public static void main(String[] args) {
        System.out.println("=== Notification Service Test Cases ===\n");
        
        NotificationService service = new NotificationService(2);
        service.registerSender(NotificationChannel.EMAIL, new EmailSender());
        service.registerSender(NotificationChannel.SMS, new SmsSender());
        service.registerSender(NotificationChannel.PUSH, new PushSender());
        
        service.setUserPreference(new UserPreference("alice", 
            NotificationChannel.EMAIL, NotificationChannel.PUSH));
        
        Notification n = service.sendNotification("alice", "Test notification",
            NotificationChannel.EMAIL, NotificationPriority.HIGH);
        System.out.println("✓ Notification sent");
        
        service.displayStatus();
    }
}
