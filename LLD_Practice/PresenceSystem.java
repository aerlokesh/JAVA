import java.time.*;
import java.util.*;
import java.util.concurrent.*;

// ===== EXCEPTIONS =====

class UserNotRegisteredException extends Exception {
    public UserNotRegisteredException(String id) { super("User not registered: " + id); }
}

// ===== ENUMS =====

enum PresenceStatus { ONLINE, OFFLINE, AWAY, DO_NOT_DISTURB }

// ===== INTERFACE (Observer Pattern) =====

/**
 * Observer — gets notified when a user's presence changes
 * Subscribers (friends) implement this to get real-time updates
 */
interface PresenceListener {
    void onPresenceChange(String userId, PresenceStatus oldStatus, PresenceStatus newStatus);
}

// ===== DOMAIN CLASSES =====

/**
 * Tracks a single user's presence state
 */
class UserPresence {
    private final String userId;
    private PresenceStatus status;
    private LocalDateTime lastSeen;       // last activity timestamp
    private LocalDateTime lastHeartbeat;  // last heartbeat received
    private String deviceInfo;            // "mobile", "web", "desktop"
    
    public UserPresence(String userId) {
        this.userId = userId;
        this.status = PresenceStatus.OFFLINE;
        this.lastSeen = LocalDateTime.now();
    }
    
    public String getUserId() { return userId; }
    public PresenceStatus getStatus() { return status; }
    public LocalDateTime getLastSeen() { return lastSeen; }
    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public String getDeviceInfo() { return deviceInfo; }
    
    public void setStatus(PresenceStatus s) { this.status = s; }
    public void setLastSeen(LocalDateTime t) { this.lastSeen = t; }
    public void setLastHeartbeat(LocalDateTime t) { this.lastHeartbeat = t; }
    public void setDeviceInfo(String d) { this.deviceInfo = d; }
    
    @Override
    public String toString() {
        return userId + "[" + status + ", lastSeen=" + lastSeen 
            + (deviceInfo != null ? ", device=" + deviceInfo : "") + "]";
    }
}

// ===== SERVICE =====

/**
 * Presence System - Low Level Design (LLD)
 * 
 * PROBLEM: Design a presence system (like WhatsApp/Slack) that can:
 * 1. Track user online/offline/away/DND status
 * 2. Heartbeat mechanism — detect if user went offline
 * 3. Subscribe to friends' presence changes (Observer pattern)
 * 4. "Last seen" timestamp
 * 5. Auto-set AWAY after idle timeout
 * 6. Support multiple devices
 * 7. Bulk query: get presence of multiple users at once
 * 
 * KEY CONCEPTS:
 * - Heartbeat: client sends periodic "I'm alive" signal (every 30s)
 * - If no heartbeat in timeout period → mark OFFLINE
 * - Observer: friends subscribe to get notified on status change
 * 
 * PATTERNS: Observer (presence notifications), Strategy (could add custom status rules)
 */
class PresenceService {
    private final Map<String, UserPresence> presenceMap;          // userId → presence
    private final Map<String, Set<PresenceListener>> subscribers; // userId → who's watching
    private final long heartbeatTimeoutMs;                         // ms before marking offline
    private final long awayTimeoutMs;                              // ms before marking away
    
    public PresenceService(long heartbeatTimeoutMs, long awayTimeoutMs) {
        this.presenceMap = new ConcurrentHashMap<>();
        this.subscribers = new ConcurrentHashMap<>();
        this.heartbeatTimeoutMs = heartbeatTimeoutMs;
        this.awayTimeoutMs = awayTimeoutMs;
    }
    
    // ===== REGISTER =====
    
    /**
     * Register a user in the presence system
     * 
     * IMPLEMENTATION HINTS:
     * 1. Create UserPresence with OFFLINE status
     * 2. Store in presenceMap
     * 3. Init empty subscriber set
     */
    public void registerUser(String userId) {
        // TODO: Implement
        // HINT: presenceMap.put(userId, new UserPresence(userId));
        // HINT: subscribers.put(userId, ConcurrentHashMap.newKeySet());
        // HINT: System.out.println("  ✓ Registered: " + userId);
    }
    
    // ===== GO ONLINE / OFFLINE =====
    
    /**
     * Set user as online (e.g., app opened, connected to WebSocket)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get presence → throw if not registered
     * 2. Save old status
     * 3. Set status = ONLINE, update lastSeen and lastHeartbeat
     * 4. Set device info if provided
     * 5. Notify subscribers of change (if status actually changed)
     */
    public void setOnline(String userId, String device) throws UserNotRegisteredException {
        // TODO: Implement
        // HINT: UserPresence p = presenceMap.get(userId);
        // HINT: if (p == null) throw new UserNotRegisteredException(userId);
        // HINT: PresenceStatus oldStatus = p.getStatus();
        // HINT: p.setStatus(PresenceStatus.ONLINE);
        // HINT: p.setLastSeen(LocalDateTime.now());
        // HINT: p.setLastHeartbeat(LocalDateTime.now());
        // HINT: if (device != null) p.setDeviceInfo(device);
        // HINT: if (oldStatus != PresenceStatus.ONLINE) notifySubscribers(userId, oldStatus, PresenceStatus.ONLINE);
        // HINT: System.out.println("  🟢 " + userId + " online" + (device != null ? " (" + device + ")" : ""));
    }
    
    public void setOnline(String userId) throws UserNotRegisteredException {
        setOnline(userId, null);
    }
    
    /**
     * Set user as offline (e.g., app closed, disconnected)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get presence
     * 2. Save old status
     * 3. Set status = OFFLINE, update lastSeen
     * 4. Notify subscribers
     */
    public void setOffline(String userId) throws UserNotRegisteredException {
        // TODO: Implement
        // HINT: UserPresence p = presenceMap.get(userId);
        // HINT: if (p == null) throw new UserNotRegisteredException(userId);
        // HINT: PresenceStatus oldStatus = p.getStatus();
        // HINT: p.setStatus(PresenceStatus.OFFLINE);
        // HINT: p.setLastSeen(LocalDateTime.now());
        // HINT: if (oldStatus != PresenceStatus.OFFLINE) notifySubscribers(userId, oldStatus, PresenceStatus.OFFLINE);
        // HINT: System.out.println("  ⚫ " + userId + " offline");
    }
    
    /**
     * Set custom status (AWAY, DO_NOT_DISTURB)
     */
    public void setStatus(String userId, PresenceStatus status) throws UserNotRegisteredException {
        // TODO: Implement
        // HINT: UserPresence p = presenceMap.get(userId);
        // HINT: if (p == null) throw new UserNotRegisteredException(userId);
        // HINT: PresenceStatus oldStatus = p.getStatus();
        // HINT: p.setStatus(status);
        // HINT: if (oldStatus != status) notifySubscribers(userId, oldStatus, status);
        // HINT: System.out.println("  🔵 " + userId + " → " + status);
    }
    
    // ===== HEARTBEAT =====
    
    /**
     * Process heartbeat from client
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get presence → ignore if not registered
     * 2. Update lastHeartbeat to now
     * 3. If status was AWAY → set back to ONLINE (user active again)
     * 
     * In real system: client sends heartbeat every 30 seconds
     */
    public void heartbeat(String userId) {
        // TODO: Implement
        // HINT: UserPresence p = presenceMap.get(userId);
        // HINT: if (p == null) return;
        // HINT: p.setLastHeartbeat(LocalDateTime.now());
        // HINT: p.setLastSeen(LocalDateTime.now());
        // HINT: if (p.getStatus() == PresenceStatus.AWAY) {
        //     PresenceStatus old = p.getStatus();
        //     p.setStatus(PresenceStatus.ONLINE);
        //     notifySubscribers(userId, old, PresenceStatus.ONLINE);
        // }
    }
    
    /**
     * Check for stale heartbeats and mark users offline/away
     * 
     * IMPLEMENTATION HINTS:
     * 1. For each user in presenceMap:
     * 2. If status is ONLINE or AWAY:
     *    a. Calculate time since last heartbeat
     *    b. If > heartbeatTimeoutMs → mark OFFLINE, notify
     *    c. Else if > awayTimeoutMs and status==ONLINE → mark AWAY, notify
     * 
     * In real system: run this on a scheduled timer (every 10-30 seconds)
     */
    public void checkHeartbeats() {
        // TODO: Implement
        // HINT: LocalDateTime now = LocalDateTime.now();
        // HINT: for (UserPresence p : presenceMap.values()) {
        //     if (p.getStatus() == PresenceStatus.ONLINE || p.getStatus() == PresenceStatus.AWAY) {
        //         if (p.getLastHeartbeat() == null) continue;
        //         long elapsed = Duration.between(p.getLastHeartbeat(), now).toMillis();
        //         
        //         if (elapsed > heartbeatTimeoutMs) {
        //             PresenceStatus old = p.getStatus();
        //             p.setStatus(PresenceStatus.OFFLINE);
        //             p.setLastSeen(p.getLastHeartbeat());
        //             notifySubscribers(p.getUserId(), old, PresenceStatus.OFFLINE);
        //             System.out.println("  ⏰ " + p.getUserId() + " timed out → OFFLINE");
        //         } else if (elapsed > awayTimeoutMs && p.getStatus() == PresenceStatus.ONLINE) {
        //             p.setStatus(PresenceStatus.AWAY);
        //             notifySubscribers(p.getUserId(), PresenceStatus.ONLINE, PresenceStatus.AWAY);
        //             System.out.println("  ⏰ " + p.getUserId() + " idle → AWAY");
        //         }
        //     }
        // }
    }
    
    // ===== SUBSCRIBE (Observer Pattern) =====
    
    /**
     * Subscribe to a user's presence changes
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get subscriber set for the target user
     * 2. Add listener to the set
     */
    public void subscribe(String targetUserId, PresenceListener listener) {
        // TODO: Implement
        // HINT: Set<PresenceListener> subs = subscribers.get(targetUserId);
        // HINT: if (subs != null) subs.add(listener);
    }
    
    /**
     * Notify all subscribers of a presence change
     */
    private void notifySubscribers(String userId, PresenceStatus oldStatus, PresenceStatus newStatus) {
        // TODO: Implement
        // HINT: Set<PresenceListener> subs = subscribers.get(userId);
        // HINT: if (subs != null) {
        //     for (PresenceListener listener : subs) {
        //         listener.onPresenceChange(userId, oldStatus, newStatus);
        //     }
        // }
    }
    
    // ===== QUERIES =====
    
    /**
     * Get a user's current presence
     */
    public UserPresence getPresence(String userId) throws UserNotRegisteredException {
        // TODO: Implement
        // HINT: UserPresence p = presenceMap.get(userId);
        // HINT: if (p == null) throw new UserNotRegisteredException(userId);
        // HINT: return p;
        return null;
    }
    
    /**
     * Bulk query: get presence for multiple users
     * 
     * IMPLEMENTATION HINTS:
     * 1. For each userId, get from presenceMap
     * 2. Collect into result map (skip unregistered)
     */
    public Map<String, PresenceStatus> getPresenceBulk(List<String> userIds) {
        // TODO: Implement
        // HINT: Map<String, PresenceStatus> result = new LinkedHashMap<>();
        // HINT: for (String uid : userIds) {
        //     UserPresence p = presenceMap.get(uid);
        //     if (p != null) result.put(uid, p.getStatus());
        // }
        // HINT: return result;
        return null;
    }
    
    /**
     * Get all online users
     */
    public List<String> getOnlineUsers() {
        // TODO: Implement
        // HINT: List<String> result = new ArrayList<>();
        // HINT: for (UserPresence p : presenceMap.values()) {
        //     if (p.getStatus() == PresenceStatus.ONLINE) result.add(p.getUserId());
        // }
        // HINT: return result;
        return null;
    }
    
    public int getOnlineCount() {
        int count = 0;
        for (UserPresence p : presenceMap.values()) {
            if (p.getStatus() == PresenceStatus.ONLINE) count++;
        }
        return count;
    }
}

// ===== MAIN TEST CLASS =====

public class PresenceSystem {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Presence System LLD ===\n");
        
        // heartbeat timeout=2000ms, away timeout=1000ms (short for testing)
        PresenceService service = new PresenceService(2000, 1000);
        
        // Setup
        service.registerUser("alice");
        service.registerUser("bob");
        service.registerUser("charlie");
        
        // Track notifications received
        List<String> notifications = new ArrayList<>();
        PresenceListener tracker = (userId, oldStatus, newStatus) -> {
            String msg = "📢 " + userId + ": " + oldStatus + " → " + newStatus;
            notifications.add(msg);
            System.out.println("    " + msg);
        };
        
        // Test 1: Go online
        System.out.println("=== Test 1: Go Online ===");
        try {
            service.setOnline("alice", "mobile");
            service.setOnline("bob", "web");
            UserPresence ap = service.getPresence("alice");
            System.out.println("✓ Alice: " + (ap != null ? ap.getStatus() : "null") + " (expect ONLINE)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 2: Go offline
        System.out.println("=== Test 2: Go Offline ===");
        try {
            service.setOffline("bob");
            UserPresence bp = service.getPresence("bob");
            System.out.println("✓ Bob: " + (bp != null ? bp.getStatus() : "null") + " (expect OFFLINE)");
            System.out.println("  Last seen: " + (bp != null ? bp.getLastSeen() : "null"));
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 3: Set DND
        System.out.println("=== Test 3: Do Not Disturb ===");
        try {
            service.setStatus("alice", PresenceStatus.DO_NOT_DISTURB);
            UserPresence ap = service.getPresence("alice");
            System.out.println("✓ Alice: " + (ap != null ? ap.getStatus() : "null") + " (expect DO_NOT_DISTURB)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 4: Subscribe and get notifications (Observer)
        System.out.println("=== Test 4: Subscribe to Presence ===");
        notifications.clear();
        service.subscribe("alice", tracker);
        service.subscribe("bob", tracker);
        try {
            service.setOnline("alice");     // DND → ONLINE
            service.setOnline("bob");       // OFFLINE → ONLINE
            service.setOffline("alice");    // ONLINE → OFFLINE
            System.out.println("✓ Notifications received: " + notifications.size());
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 5: Heartbeat
        System.out.println("=== Test 5: Heartbeat ===");
        try {
            service.setOnline("alice");
            service.heartbeat("alice");
            UserPresence ap = service.getPresence("alice");
            System.out.println("✓ Alice heartbeat received, status: " + (ap != null ? ap.getStatus() : "null"));
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 6: Heartbeat timeout → AWAY then OFFLINE
        System.out.println("=== Test 6: Heartbeat Timeout ===");
        try {
            service.setOnline("charlie");
            service.heartbeat("charlie");
            
            // Wait for away timeout (1s)
            Thread.sleep(1100);
            service.checkHeartbeats();
            UserPresence cp = service.getPresence("charlie");
            System.out.println("✓ After 1.1s idle: " + (cp != null ? cp.getStatus() : "null") + " (expect AWAY)");
            
            // Wait for offline timeout (2s total)
            Thread.sleep(1100);
            service.checkHeartbeats();
            cp = service.getPresence("charlie");
            System.out.println("✓ After 2.2s idle: " + (cp != null ? cp.getStatus() : "null") + " (expect OFFLINE)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 7: Heartbeat revives from AWAY
        System.out.println("=== Test 7: Heartbeat Revives ===");
        try {
            service.setOnline("charlie");
            Thread.sleep(1100);
            service.checkHeartbeats(); // → AWAY
            
            service.heartbeat("charlie"); // should go back to ONLINE
            UserPresence cp = service.getPresence("charlie");
            System.out.println("✓ After heartbeat: " + (cp != null ? cp.getStatus() : "null") + " (expect ONLINE)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 8: Bulk query
        System.out.println("=== Test 8: Bulk Query ===");
        try {
            service.setOnline("alice");
            service.setOffline("bob");
            Map<String, PresenceStatus> bulk = service.getPresenceBulk(
                Arrays.asList("alice", "bob", "charlie"));
            System.out.println("✓ Bulk: " + (bulk != null ? bulk : "null (implement!)"));
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 9: Get online users
        System.out.println("=== Test 9: Online Users ===");
        List<String> online = service.getOnlineUsers();
        System.out.println("✓ Online: " + (online != null ? online : "null"));
        System.out.println("  Count: " + service.getOnlineCount());
        System.out.println();
        
        // Test 10: Exception — unregistered user
        System.out.println("=== Test 10: Exception - Not Registered ===");
        try {
            service.setOnline("unknown");
            System.out.println("✗ Should have thrown");
        } catch (UserNotRegisteredException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        }
        
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION:
 * =====================
 * 
 * 1. HEARTBEAT MECHANISM:
 *    Client sends "ping" every 30s via WebSocket
 *    Server checks: no heartbeat in 60s → OFFLINE
 *    No heartbeat in 30s → AWAY (idle)
 *    Heartbeat received → back to ONLINE
 * 
 * 2. OBSERVER PATTERN:
 *    Friends subscribe to your presence
 *    On status change → notify all subscribers
 *    In real system: push via WebSocket, not polling
 * 
 * 3. LAST SEEN:
 *    Updated on every activity (message, heartbeat, open app)
 *    WhatsApp-style: "last seen today at 2:30 PM"
 *    Privacy: user can hide last seen
 * 
 * 4. MULTIPLE DEVICES:
 *    User online on phone + laptop = show ONLINE
 *    If phone goes offline but laptop still on = still ONLINE
 *    All devices offline → OFFLINE
 *    Track per-device: Map<userId, Map<deviceId, heartbeat>>
 * 
 * 5. ARCHITECTURE:
 *    Client → WebSocket → Presence Service → Redis (in-memory store)
 *                                          → Pub/Sub (notify friends)
 *    Redis: fast reads for "is user online?"
 *    Pub/Sub: real-time notifications to subscribers
 * 
 * 6. SCALABILITY:
 *    - Redis for presence state (millions of users)
 *    - WebSocket servers behind LB (sticky sessions)
 *    - Pub/Sub (Redis/Kafka) for cross-server notifications
 *    - Fan-out: celebrity with 1M friends → expensive notifications
 *      Solution: pull-based for popular users, push for regular
 * 
 * 7. REAL-WORLD: WhatsApp, Slack, Discord, Teams
 * 
 * 8. API:
 *    POST /presence/online     — go online
 *    POST /presence/offline    — go offline
 *    POST /presence/heartbeat  — keep-alive
 *    GET  /presence/{userId}   — get status
 *    POST /presence/bulk       — get multiple
 *    WS   /presence/subscribe  — real-time updates
 */
