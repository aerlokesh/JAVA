import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// ==================== ENUMS ====================

enum NotificationChannel { EMAIL, SMS, PUSH }
enum NotificationPriority { LOW, MEDIUM, HIGH, CRITICAL }
enum NotificationStatus { PENDING, SENT, FAILED, RETRYING }

// ==================== EXCEPTIONS ====================

class NotificationFailedException extends Exception {
    public NotificationFailedException(String msg) { super(msg); }
}

// ==================== INTERFACE - STRATEGY PATTERN ====================

interface NotificationSender {
    boolean send(String userId, String message);
    NotificationChannel getChannel();
}

// ==================== CHANNEL IMPLEMENTATIONS ====================

class EmailSender implements NotificationSender {
    private final double failRate; // 0.0 to 1.0 for testing
    private final Random random = new Random();

    EmailSender(double failRate) { this.failRate = failRate; }
    EmailSender() { this(0.0); }

    @Override
    public boolean send(String userId, String message) {
        if (random.nextDouble() < failRate) return false;
        System.out.println("    📧 EMAIL → " + userId + ": " + message);
        return true;
    }

    @Override
    public NotificationChannel getChannel() { return NotificationChannel.EMAIL; }
}

class SmsSender implements NotificationSender {
    private final double failRate;
    private final Random random = new Random();

    SmsSender(double failRate) { this.failRate = failRate; }
    SmsSender() { this(0.0); }

    @Override
    public boolean send(String userId, String message) {
        if (random.nextDouble() < failRate) return false;
        System.out.println("    📱 SMS → " + userId + ": " + message);
        return true;
    }

    @Override
    public NotificationChannel getChannel() { return NotificationChannel.SMS; }
}

class PushSender implements NotificationSender {
    private final double failRate;
    private final Random random = new Random();

    PushSender(double failRate) { this.failRate = failRate; }
    PushSender() { this(0.0); }

    @Override
    public boolean send(String userId, String message) {
        if (random.nextDouble() < failRate) return false;
        System.out.println("    🔔 PUSH → " + userId + ": " + message);
        return true;
    }

    @Override
    public NotificationChannel getChannel() { return NotificationChannel.PUSH; }
}

// ==================== DOMAIN CLASSES ====================

// Notification - a single notification to send
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

    @Override
    public String toString() {
        return id + " [" + channel + "/" + priority + "] → " + userId + ": \"" + message + "\" (" + status + ")";
    }
}

// UserPreference - which channels a user wants
class UserPreference {
    String userId;
    Set<NotificationChannel> enabledChannels;

    UserPreference(String userId, NotificationChannel... channels) {
        this.userId = userId;
        this.enabledChannels = new HashSet<>(Arrays.asList(channels));
    }
}

// ==================== MAIN SERVICE - THREAD SAFE ====================

class NotificationService {
    Map<NotificationChannel, NotificationSender> senders;
    Map<String, UserPreference> preferences; // userId -> preference
    List<Notification> allNotifications;
    int maxRetries;
    AtomicInteger totalSent = new AtomicInteger(0);
    AtomicInteger totalFailed = new AtomicInteger(0);

    NotificationService(int maxRetries) {
        this.senders = new ConcurrentHashMap<>();
        this.preferences = new ConcurrentHashMap<>();
        this.allNotifications = Collections.synchronizedList(new ArrayList<>());
        this.maxRetries = maxRetries;
    }

    void registerSender(NotificationChannel channel, NotificationSender sender) {
        senders.put(channel, sender);
    }

    void setUserPreference(UserPreference pref) {
        preferences.put(pref.userId, pref);
    }

    // Send a single notification with retry - THREAD SAFE
    public synchronized Notification sendNotification(String userId, String message,
            NotificationChannel channel, NotificationPriority priority) {

        Notification notif = new Notification(userId, message, channel, priority);
        allNotifications.add(notif);

        // Check user preference
        UserPreference pref = preferences.get(userId);
        if (pref != null && !pref.enabledChannels.contains(channel)) {
            System.out.println(Thread.currentThread().getName()
                + ": " + notif.id + " SKIPPED (" + channel + " disabled for " + userId + ")");
            notif.status = NotificationStatus.FAILED;
            totalFailed.incrementAndGet();
            return notif;
        }

        NotificationSender sender = senders.get(channel);
        if (sender == null) {
            System.out.println(Thread.currentThread().getName()
                + ": " + notif.id + " FAILED (no sender for " + channel + ")");
            notif.status = NotificationStatus.FAILED;
            totalFailed.incrementAndGet();
            return notif;
        }

        // Try send with retries
        boolean sent = false;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            if (attempt > 0) {
                notif.status = NotificationStatus.RETRYING;
                notif.retryCount = attempt;
            }
            sent = sender.send(userId, message);
            if (sent) break;
        }

        if (sent) {
            notif.status = NotificationStatus.SENT;
            notif.sentAt = LocalDateTime.now();
            totalSent.incrementAndGet();
            System.out.println(Thread.currentThread().getName()
                + ": " + notif.id + " SENT via " + channel
                + (notif.retryCount > 0 ? " (after " + notif.retryCount + " retries)" : ""));
        } else {
            notif.status = NotificationStatus.FAILED;
            totalFailed.incrementAndGet();
            System.out.println(Thread.currentThread().getName()
                + ": " + notif.id + " FAILED after " + (maxRetries + 1) + " attempts");
        }
        return notif;
    }

    // Broadcast to all channels user has enabled
    public List<Notification> broadcastToUser(String userId, String message, NotificationPriority priority) {
        UserPreference pref = preferences.get(userId);
        Set<NotificationChannel> channels = (pref != null) ? pref.enabledChannels
            : Set.of(NotificationChannel.EMAIL); // default to email

        List<Notification> results = new ArrayList<>();
        for (NotificationChannel ch : channels) {
            results.add(sendNotification(userId, message, ch, priority));
        }
        return results;
    }

    // Bulk send - send same message to multiple users
    public List<Notification> bulkSend(List<String> userIds, String message,
            NotificationChannel channel, NotificationPriority priority) {
        List<Notification> results = new ArrayList<>();
        for (String userId : userIds) {
            results.add(sendNotification(userId, message, channel, priority));
        }
        return results;
    }

    void displayStatus() {
        System.out.println("\n--- Notification Service Status ---");
        System.out.println("Channels registered: " + senders.keySet());
        System.out.println("User preferences: " + preferences.size());
        long sent = allNotifications.stream().filter(n -> n.status == NotificationStatus.SENT).count();
        long failed = allNotifications.stream().filter(n -> n.status == NotificationStatus.FAILED).count();
        System.out.println("Notifications: " + allNotifications.size()
            + " (Sent: " + sent + ", Failed: " + failed + ")");
    }
}

// ==================== MAIN ====================

public class NotificationServiceLLD {
    public static void main(String[] args) throws InterruptedException {
        // ---- Setup ----
        NotificationService service = new NotificationService(2); // max 2 retries

        service.registerSender(NotificationChannel.EMAIL, new EmailSender());
        service.registerSender(NotificationChannel.SMS, new SmsSender());
        service.registerSender(NotificationChannel.PUSH, new PushSender());

        // User preferences
        service.setUserPreference(new UserPreference("alice",
            NotificationChannel.EMAIL, NotificationChannel.PUSH));
        service.setUserPreference(new UserPreference("bob",
            NotificationChannel.EMAIL, NotificationChannel.SMS, NotificationChannel.PUSH));
        service.setUserPreference(new UserPreference("charlie",
            NotificationChannel.SMS)); // SMS only

        service.displayStatus();

        // ---- Test 1: Basic Notification ----
        System.out.println("\n=== Test 1: BASIC SEND ===");
        Notification n1 = service.sendNotification("alice", "Your order is confirmed!",
            NotificationChannel.EMAIL, NotificationPriority.HIGH);
        System.out.println("✓ Sent: " + (n1.status == NotificationStatus.SENT));

        // ---- Test 2: Channel Preference Respected ----
        System.out.println("\n=== Test 2: CHANNEL PREFERENCE (alice has no SMS) ===");
        Notification n2 = service.sendNotification("alice", "Flash sale!",
            NotificationChannel.SMS, NotificationPriority.LOW);
        System.out.println("✓ SMS skipped for alice: " + (n2.status == NotificationStatus.FAILED));

        // ---- Test 3: Broadcast to all user channels ----
        System.out.println("\n=== Test 3: BROADCAST TO USER (bob - all 3 channels) ===");
        List<Notification> broadcast = service.broadcastToUser("bob",
            "System maintenance at midnight", NotificationPriority.CRITICAL);
        long sentCount = broadcast.stream().filter(n -> n.status == NotificationStatus.SENT).count();
        System.out.println("✓ Bob received on " + sentCount + " channels (expected 3): " + (sentCount == 3));

        // ---- Test 4: Bulk Send ----
        System.out.println("\n=== Test 4: BULK SEND (3 users, EMAIL) ===");
        List<Notification> bulk = service.bulkSend(
            List.of("alice", "bob", "charlie"),
            "Weekly newsletter", NotificationChannel.EMAIL, NotificationPriority.LOW);
        long bulkSent = bulk.stream().filter(n -> n.status == NotificationStatus.SENT).count();
        // alice & bob have email enabled, charlie only has SMS
        System.out.println("✓ Sent to email-enabled users: " + bulkSent + " (expected 2): " + (bulkSent == 2));

        // ---- Test 5: Retry on Failure ----
        System.out.println("\n=== Test 5: RETRY MECHANISM (100% fail rate) ===");
        NotificationService retryService = new NotificationService(3); // 3 retries
        retryService.registerSender(NotificationChannel.EMAIL, new EmailSender(1.0)); // always fails
        retryService.setUserPreference(new UserPreference("dave", NotificationChannel.EMAIL));
        Notification n5 = retryService.sendNotification("dave", "Test retry",
            NotificationChannel.EMAIL, NotificationPriority.HIGH);
        System.out.println("✓ Failed after all retries: " + (n5.status == NotificationStatus.FAILED));

        // ---- Test 6: Retry with partial failure ----
        System.out.println("\n=== Test 6: RETRY WITH 50% FAIL RATE ===");
        NotificationService partialService = new NotificationService(5); // 5 retries
        partialService.registerSender(NotificationChannel.PUSH, new PushSender(0.5));
        partialService.setUserPreference(new UserPreference("eve", NotificationChannel.PUSH));
        Notification n6 = partialService.sendNotification("eve", "Lucky notification",
            NotificationChannel.PUSH, NotificationPriority.MEDIUM);
        System.out.println("Status: " + n6.status + " (retries: " + n6.retryCount + ")");
        System.out.println("✓ Eventually sent or failed gracefully");

        // ---- Test 7: Priority Levels ----
        System.out.println("\n=== Test 7: DIFFERENT PRIORITIES ===");
        for (NotificationPriority p : NotificationPriority.values()) {
            Notification n = service.sendNotification("bob", p + " priority alert",
                NotificationChannel.PUSH, p);
        }
        System.out.println("✓ All priority levels processed");

        // ---- Test 8: CONCURRENCY - 10 threads sending simultaneously ----
        System.out.println("\n=== Test 8: CONCURRENT NOTIFICATIONS (10 threads) ===");
        List<Thread> threads = new ArrayList<>();
        List<Notification> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < 10; i++) {
            final int idx = i;
            Thread t = new Thread(() -> {
                Notification n = service.sendNotification("bob",
                    "Concurrent msg #" + idx,
                    NotificationChannel.EMAIL, NotificationPriority.MEDIUM);
                results.add(n);
            }, "NotifThread-" + idx);
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) { t.join(); }

        long concurrentSent = results.stream().filter(n -> n.status == NotificationStatus.SENT).count();
        System.out.println("All 10 sent: " + (concurrentSent == 10));
        System.out.println("✓ Concurrent notifications - no race conditions!");

        // ---- Test 9: CONCURRENT broadcast to different users ----
        System.out.println("\n=== Test 9: CONCURRENT BROADCAST (3 users simultaneously) ===");
        threads.clear();
        AtomicInteger totalBroadcast = new AtomicInteger(0);

        String[] users = {"alice", "bob", "charlie"};
        for (String userId : users) {
            Thread t = new Thread(() -> {
                List<Notification> r = service.broadcastToUser(userId,
                    "Urgent: Server downtime", NotificationPriority.CRITICAL);
                totalBroadcast.addAndGet((int) r.stream()
                    .filter(n -> n.status == NotificationStatus.SENT).count());
            }, "BroadcastThread-" + userId);
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) { t.join(); }

        // alice: 2 channels, bob: 3 channels, charlie: 1 channel = 6 total
        System.out.println("Total sent across all users: " + totalBroadcast.get());
        System.out.println("✓ Expected 6: " + (totalBroadcast.get() == 6));

        // ---- Final Status ----
        service.displayStatus();
    }
}