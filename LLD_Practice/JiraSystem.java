import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/*
 * JIRA SYSTEM - Low Level Design
 * ==================================
 *
 * REQUIREMENTS:
 * 1. Create projects and tickets with title, description, status, priority, assignee
 * 2. Update ticket fields (description, status, priority, assignee)
 * 3. Handle conflicting updates — two users editing the same ticket concurrently
 * 4. Optimistic concurrency control via version number on each ticket
 * 5. On GET: track which version the user read (session-based read tracking)
 * 6. On UPDATE: compare user's read version vs current version — reject if stale
 * 7. Communicate conflict error with details (who changed, when, what version)
 * 8. Thread-safe: multiple users updating different/same tickets concurrently
 *
 * KEY DATA STRUCTURES:
 * - ConcurrentHashMap<ticketId, Ticket>: O(1) ticket lookup
 * - ConcurrentHashMap<sessionId:ticketId, ReadRecord>: tracks what version a session last read
 * - AtomicInteger version on Ticket: incremented on every successful update
 * - synchronized per-ticket updates: only one writer at a time per ticket
 *
 * DESIGN PATTERNS:
 * - Optimistic Locking: version-based conflict detection (like DB row versioning)
 * - Observer: notify watchers on ticket changes
 *
 * COMPLEXITY:
 *   createTicket:  O(1)
 *   getTicket:     O(1) + records read version
 *   updateTicket:  O(1) with version check — O(1) conflict detection
 *   searchByStatus/assignee: O(n) scan
 */

// ==================== EXCEPTIONS ====================

class StaleUpdateException extends Exception {
    final String ticketId;
    final int readVersion, currentVersion;
    final String lastModifiedBy;

    StaleUpdateException(String ticketId, int readVersion, int currentVersion, String lastModifiedBy) {
        super(String.format("Conflict on %s: you read v%d but current is v%d (last modified by '%s')",
                ticketId, readVersion, currentVersion, lastModifiedBy));
        this.ticketId = ticketId;
        this.readVersion = readVersion;
        this.currentVersion = currentVersion;
        this.lastModifiedBy = lastModifiedBy;
    }
}

class TicketNotFoundException extends Exception {
    TicketNotFoundException(String id) { super("Ticket not found: " + id); }
}

// ==================== ENUMS ====================

enum TicketStatus { OPEN, IN_PROGRESS, IN_REVIEW, DONE, CLOSED }
enum Priority { CRITICAL, HIGH, MEDIUM, LOW }

// ==================== DOMAIN CLASSES ====================

class Ticket {
    final String id, projectId;
    String title, description, assignee;
    TicketStatus status;
    Priority priority;
    final AtomicInteger version = new AtomicInteger(1);   // optimistic lock version
    volatile String lastModifiedBy;                        // who last updated
    volatile long lastModifiedAt;                          // when last updated
    final long createdAt = System.currentTimeMillis();
    final String createdBy;

    Ticket(String id, String projectId, String title, String description,
           Priority priority, String createdBy) {
        this.id = id; this.projectId = projectId;
        this.title = title; this.description = description;
        this.priority = priority; this.status = TicketStatus.OPEN;
        this.createdBy = createdBy; this.lastModifiedBy = createdBy;
        this.lastModifiedAt = createdAt;
    }
}

/** What a session saw when it last read a ticket */
class ReadRecord {
    final String sessionId, ticketId;
    final int versionRead;
    final long readAt;

    ReadRecord(String sessionId, String ticketId, int versionRead) {
        this.sessionId = sessionId; this.ticketId = ticketId;
        this.versionRead = versionRead; this.readAt = System.currentTimeMillis();
    }
}

/** Encapsulates the fields a user wants to update (null = no change) */
class TicketUpdate {
    final String sessionId, userId;
    String newTitle, newDescription, newAssignee;
    TicketStatus newStatus;
    Priority newPriority;

    TicketUpdate(String sessionId, String userId) {
        this.sessionId = sessionId; this.userId = userId;
    }
}

/** Snapshot returned to users — includes version for conflict awareness */
class TicketView {
    final String id, projectId, title, description, assignee, lastModifiedBy;
    final TicketStatus status;
    final Priority priority;
    final int version;

    TicketView(Ticket t) {
        this.id = t.id; this.projectId = t.projectId;
        this.title = t.title; this.description = t.description;
        this.assignee = t.assignee; this.status = t.status;
        this.priority = t.priority; this.version = t.version.get();
        this.lastModifiedBy = t.lastModifiedBy;
    }
}

// ==================== CHANGE LISTENER (Observer) ====================

interface TicketChangeListener {
    void onTicketChanged(String ticketId, String changedBy, int newVersion);
}

class ChangeLogger implements TicketChangeListener {
    public void onTicketChanged(String ticketId, String changedBy, int newVersion) {
        System.out.printf("  [NOTIFY] Ticket %s updated to v%d by '%s'%n", ticketId, newVersion, changedBy);
    }
}

// ==================== JIRA SERVICE (Core Engine) ====================

class JiraService {
    private final ConcurrentHashMap<String, Ticket> tickets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReadRecord> readTracker = new ConcurrentHashMap<>(); // key: sessionId:ticketId
    private final Set<String> projects = ConcurrentHashMap.newKeySet();
    private final AtomicInteger idCounter = new AtomicInteger(1);
    private final List<TicketChangeListener> listeners = new CopyOnWriteArrayList<>();

    void addListener(TicketChangeListener l) { listeners.add(l); }

    // ---- Project management ----

    void createProject(String projectId) { projects.add(projectId); }

    // ---- Ticket CRUD ----

    String createTicket(String projectId, String title, String description,
                        Priority priority, String createdBy) {
        // TODO: Implement
        // HINT: String ticketId = projectId + "-" + idCounter.getAndIncrement();
        // HINT: Ticket t = new Ticket(ticketId, projectId, title, description, priority, createdBy);
        // HINT: tickets.put(ticketId, t);
        // HINT: return ticketId;
        return null;
    }

    /**
     * GET a ticket — also records what version this session read.
     * This is the key to conflict detection: we know what version the user saw.
     */
    TicketView getTicket(String ticketId, String sessionId) throws TicketNotFoundException {
        // TODO: Implement
        // HINT: Ticket t = tickets.get(ticketId);
        // HINT: if (t == null) throw new TicketNotFoundException(ticketId);
        // HINT: int currentVersion = t.version.get();
        // HINT: String key = sessionId + ":" + ticketId;
        // HINT: readTracker.put(key, new ReadRecord(sessionId, ticketId, currentVersion));
        // HINT: return new TicketView(t);
        return null;
    }

    /**
     * UPDATE a ticket — performs OPTIMISTIC CONCURRENCY CHECK.
     *
     * Flow:
     *   1. Look up what version this session last read (from readTracker)
     *   2. Compare readVersion vs ticket's current version
     *   3. If readVersion < currentVersion → someone else modified it → REJECT with StaleUpdateException
     *   4. If versions match → apply update, increment version, notify listeners
     *
     * This is how the "error is communicated to the second person":
     *   - User A reads ticket (v1), User B reads ticket (v1)
     *   - User A updates → succeeds, ticket becomes v2
     *   - User B tries to update → their readVersion(1) < currentVersion(2)
     *   - → StaleUpdateException thrown with details about who changed it
     */
    TicketView updateTicket(String ticketId, TicketUpdate update)
            throws TicketNotFoundException, StaleUpdateException {
        // TODO: Implement
        // HINT: Ticket t = tickets.get(ticketId);
        // HINT: if (t == null) throw new TicketNotFoundException(ticketId);
        // HINT:
        // HINT: synchronized (t) {  // per-ticket lock — only one writer at a time
        // HINT:     // Step 1: Check what version this session read
        // HINT:     String key = update.sessionId + ":" + ticketId;
        // HINT:     ReadRecord record = readTracker.get(key);
        // HINT:     int readVersion = (record != null) ? record.versionRead : 0;
        // HINT:
        // HINT:     // Step 2: Optimistic concurrency check
        // HINT:     int currentVersion = t.version.get();
        // HINT:     if (readVersion < currentVersion) {
        // HINT:         throw new StaleUpdateException(ticketId, readVersion, currentVersion, t.lastModifiedBy);
        // HINT:     }
        // HINT:
        // HINT:     // Step 3: Apply changes (only non-null fields)
        // HINT:     if (update.newTitle != null) t.title = update.newTitle;
        // HINT:     if (update.newDescription != null) t.description = update.newDescription;
        // HINT:     if (update.newAssignee != null) t.assignee = update.newAssignee;
        // HINT:     if (update.newStatus != null) t.status = update.newStatus;
        // HINT:     if (update.newPriority != null) t.priority = update.newPriority;
        // HINT:
        // HINT:     // Step 4: Bump version + metadata
        // HINT:     int newVersion = t.version.incrementAndGet();
        // HINT:     t.lastModifiedBy = update.userId;
        // HINT:     t.lastModifiedAt = System.currentTimeMillis();
        // HINT:
        // HINT:     // Step 5: Update read tracker for this session (they now see latest)
        // HINT:     readTracker.put(key, new ReadRecord(update.sessionId, ticketId, newVersion));
        // HINT:
        // HINT:     // Step 6: Notify observers
        // HINT:     for (TicketChangeListener l : listeners) l.onTicketChanged(ticketId, update.userId, newVersion);
        // HINT:
        // HINT:     return new TicketView(t);
        // HINT: }
        return null;
    }

    /** Search tickets by status */
    List<TicketView> searchByStatus(String projectId, TicketStatus status) {
        // TODO: Implement
        // HINT: List<TicketView> results = new ArrayList<>();
        // HINT: for (Ticket t : tickets.values()) {
        // HINT:     if (t.projectId.equals(projectId) && t.status == status)
        // HINT:         results.add(new TicketView(t));
        // HINT: }
        // HINT: return results;
        return Collections.emptyList();
    }

    /** Search tickets by assignee */
    List<TicketView> searchByAssignee(String assignee) {
        // TODO: Implement
        // HINT: List<TicketView> results = new ArrayList<>();
        // HINT: for (Ticket t : tickets.values()) {
        // HINT:     if (assignee.equals(t.assignee))
        // HINT:         results.add(new TicketView(t));
        // HINT: }
        // HINT: return results;
        return Collections.emptyList();
    }

    int getTicketVersion(String ticketId) {
        Ticket t = tickets.get(ticketId);
        return t != null ? t.version.get() : -1;
    }
}

// ==================== MAIN / TESTS ====================

public class JiraSystem {
    public static void main(String[] args) throws Exception {
        System.out.println("╔═══════════════════════════════════════════════╗");
        System.out.println("║   JIRA SYSTEM - Optimistic Concurrency LLD   ║");
        System.out.println("╚═══════════════════════════════════════════════╝\n");

        JiraService jira = new JiraService();
        jira.addListener(new ChangeLogger());
        jira.createProject("PROJ");

        // --- Test 1: Create tickets ---
        System.out.println("=== Test 1: Create tickets ===");
        String t1 = jira.createTicket("PROJ", "Login bug", "Users can't login", Priority.HIGH, "alice");
        String t2 = jira.createTicket("PROJ", "Dark mode", "Add dark mode support", Priority.MEDIUM, "bob");
        System.out.println("Created: " + t1 + ", " + t2);
        System.out.println("✓ Tickets created with auto-incrementing IDs\n");

        // --- Test 2: Get ticket records read version ---
        System.out.println("=== Test 2: GET records read version ===");
        TicketView view = jira.getTicket(t1, "session-alice");
        System.out.println("Title: " + view.title + ", Version: " + view.version);
        System.out.println("✓ Alice's session recorded read at v" + view.version + "\n");

        // --- Test 3: Successful update (no conflict) ---
        System.out.println("=== Test 3: Successful update (no conflict) ===");
        TicketUpdate upd = new TicketUpdate("session-alice", "alice");
        upd.newDescription = "Users can't login after password reset";
        upd.newStatus = TicketStatus.IN_PROGRESS;
        TicketView updated = jira.updateTicket(t1, upd);
        System.out.println("Updated description: " + updated.description);
        System.out.println("Status: " + updated.status + ", Version: " + updated.version);
        System.out.println("✓ Update succeeds, version bumped to " + updated.version + "\n");

        // --- Test 4: CONFLICT DETECTION — the key scenario ---
        System.out.println("=== Test 4: CONFLICT DETECTION ===");
        System.out.println("Scenario: Alice & Bob both read ticket, Alice updates, Bob tries to update");

        // Step 1: Both Alice and Bob read the ticket (both see v2)
        TicketView aliceRead = jira.getTicket(t1, "session-alice-2");
        TicketView bobRead = jira.getTicket(t1, "session-bob");
        System.out.println("Alice reads v" + aliceRead.version + ", Bob reads v" + bobRead.version);

        // Step 2: Alice updates successfully → ticket becomes v3
        TicketUpdate aliceUpd = new TicketUpdate("session-alice-2", "alice");
        aliceUpd.newDescription = "Login fails for SSO users specifically";
        TicketView aliceResult = jira.updateTicket(t1, aliceUpd);
        System.out.println("Alice updates → v" + aliceResult.version + " ✓");

        // Step 3: Bob tries to update → CONFLICT (he read v2, but it's now v3)
        TicketUpdate bobUpd = new TicketUpdate("session-bob", "bob");
        bobUpd.newDescription = "Login fails on Chrome only";
        try {
            jira.updateTicket(t1, bobUpd);
            System.out.println("ERROR: Should have thrown StaleUpdateException!");
        } catch (StaleUpdateException e) {
            System.out.println("Bob's update REJECTED: " + e.getMessage());
            System.out.println("  → Bob read v" + e.readVersion + ", current is v" + e.currentVersion);
            System.out.println("  → Last modified by: " + e.lastModifiedBy);
        }
        System.out.println("✓ Conflict detected and communicated to Bob\n");

        // --- Test 5: Bob resolves conflict by re-reading and updating ---
        System.out.println("=== Test 5: Bob resolves conflict ===");
        TicketView bobReread = jira.getTicket(t1, "session-bob");
        System.out.println("Bob re-reads ticket: v" + bobReread.version + ", desc: '" + bobReread.description + "'");
        // Bob now sees Alice's change, decides to merge or overwrite
        TicketUpdate bobRetry = new TicketUpdate("session-bob", "bob");
        bobRetry.newDescription = "Login fails for SSO users on Chrome";  // merged description
        TicketView bobResult = jira.updateTicket(t1, bobRetry);
        System.out.println("Bob updates after re-read → v" + bobResult.version + " ✓");
        System.out.println("Merged description: " + bobResult.description);
        System.out.println("✓ Conflict resolved by read-then-retry\n");

        // --- Test 6: Multiple fields update ---
        System.out.println("=== Test 6: Multi-field update ===");
        jira.getTicket(t1, "session-carol");
        TicketUpdate multiUpd = new TicketUpdate("session-carol", "carol");
        multiUpd.newAssignee = "dave";
        multiUpd.newPriority = Priority.CRITICAL;
        multiUpd.newStatus = TicketStatus.IN_REVIEW;
        TicketView multi = jira.updateTicket(t1, multiUpd);
        System.out.println("Assignee: " + multi.assignee + ", Priority: " + multi.priority + ", Status: " + multi.status);
        System.out.println("✓ Multiple fields updated atomically\n");

        // --- Test 7: Search by status ---
        System.out.println("=== Test 7: Search by status ===");
        List<TicketView> openTickets = jira.searchByStatus("PROJ", TicketStatus.OPEN);
        List<TicketView> reviewTickets = jira.searchByStatus("PROJ", TicketStatus.IN_REVIEW);
        System.out.println("OPEN tickets: " + openTickets.size() + " (expected 1 — dark mode)");
        System.out.println("IN_REVIEW tickets: " + reviewTickets.size() + " (expected 1 — login bug)");
        System.out.println("✓ Status search works\n");

        // --- Test 8: Search by assignee ---
        System.out.println("=== Test 8: Search by assignee ===");
        List<TicketView> daveTickets = jira.searchByAssignee("dave");
        System.out.println("Dave's tickets: " + daveTickets.size() + " (expected 1)");
        System.out.println("✓ Assignee search works\n");

        // --- Test 9: Ticket not found ---
        System.out.println("=== Test 9: Ticket not found ===");
        try {
            jira.getTicket("PROJ-999", "session-x");
            System.out.println("ERROR: Should have thrown!");
        } catch (TicketNotFoundException e) {
            System.out.println("Caught: " + e.getMessage());
        }
        System.out.println("✓ Not found handled\n");

        // --- Test 10: Update without prior GET (no read record) ---
        System.out.println("=== Test 10: Update without prior GET ===");
        TicketUpdate blindUpd = new TicketUpdate("session-new", "eve");
        blindUpd.newDescription = "Blind update attempt";
        try {
            jira.updateTicket(t1, blindUpd);
            System.out.println("ERROR: Should have thrown!");
        } catch (StaleUpdateException e) {
            System.out.println("Caught: " + e.getMessage());
            System.out.println("  → readVersion=0 (no prior GET), currentVersion=" + e.currentVersion);
        }
        System.out.println("✓ Must GET before UPDATE\n");

        // --- Test 11: Concurrent updates — thread safety ---
        System.out.println("=== Test 11: Concurrent updates ===");
        String t3 = jira.createTicket("PROJ", "Perf issue", "Slow queries", Priority.LOW, "alice");
        ExecutorService exec = Executors.newFixedThreadPool(8);
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger conflicts = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            final int idx = i;
            futures.add(exec.submit(() -> {
                try {
                    String sid = "session-thread-" + idx;
                    jira.getTicket(t3, sid);                // read current version
                    Thread.sleep(ThreadLocalRandom.current().nextInt(5)); // simulate think time
                    TicketUpdate tu = new TicketUpdate(sid, "user-" + idx);
                    tu.newDescription = "Updated by thread " + idx;
                    jira.updateTicket(t3, tu);
                    successes.incrementAndGet();
                } catch (StaleUpdateException e) {
                    conflicts.incrementAndGet();           // expected for losers of the race
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        }
        for (Future<?> f : futures) f.get();
        exec.shutdown();

        System.out.println("20 concurrent updates: " + successes.get() + " succeeded, " + conflicts.get() + " conflicts");
        System.out.println("Final version: v" + jira.getTicketVersion(t3));
        System.out.println("✓ Thread-safe: successes + conflicts = " + (successes.get() + conflicts.get()) + "\n");

        // --- Test 12: Rapid sequential updates by same session ---
        System.out.println("=== Test 12: Rapid sequential updates (same user) ===");
        String t4 = jira.createTicket("PROJ", "UI glitch", "Button misaligned", Priority.LOW, "alice");
        for (int i = 0; i < 5; i++) {
            jira.getTicket(t4, "session-alice");
            TicketUpdate seq = new TicketUpdate("session-alice", "alice");
            seq.newDescription = "Iteration " + i;
            jira.updateTicket(t4, seq);
        }
        System.out.println("5 sequential updates by same session: v" + jira.getTicketVersion(t4) + " (expected 6)");
        System.out.println("✓ Same session can update repeatedly with re-reads\n");

        System.out.println("════════ ALL 12 TESTS PASSED ✓ ════════");
    }
}

/*
 * INTERVIEW NOTES:
 *
 * 1. CORE CONCEPT — OPTIMISTIC CONCURRENCY CONTROL:
 *    Each ticket has a version counter (AtomicInteger). On every GET, we record
 *    (sessionId, ticketId, versionRead) in a read-tracker. On UPDATE, we compare
 *    the session's readVersion against the ticket's current version. If they differ,
 *    another user modified the ticket in between → throw StaleUpdateException.
 *
 *    This is identical to how HTTP ETags work, how databases do optimistic locking
 *    (e.g., "UPDATE ... WHERE version = expected_version"), and how Confluence/Jira
 *    actually handle concurrent edits.
 *
 * 2. WHY OPTIMISTIC OVER PESSIMISTIC LOCKING:
 *    - Pessimistic (row-level lock on read): blocks other readers/writers. Bad for
 *      a system where users READ far more than they WRITE. Stale locks if user
 *      abandons the page without releasing.
 *    - Optimistic (version check on write): allows concurrent reads freely. Only
 *      checks for conflict at write time. Perfect for JIRA-like systems where
 *      conflicts are rare (most users edit different tickets).
 *
 * 3. ERROR COMMUNICATION FLOW:
 *    a) User opens ticket page → GET /ticket/PROJ-1 (records readVersion in tracker)
 *    b) User edits description, clicks Save → PUT /ticket/PROJ-1 with sessionId
 *    c) Server checks: readVersion == currentVersion?
 *       - YES → apply update, bump version, return success
 *       - NO  → return 409 Conflict with: {readVersion, currentVersion, lastModifiedBy}
 *    d) Client shows: "This ticket was updated by Alice at 3:45 PM. Reload to see changes."
 *    e) User reloads (GET again), sees new content, can merge or re-apply their edit
 *
 * 4. ALTERNATIVE: EXPLICIT VERSION IN REQUEST (no read-tracker table needed):
 *    - Client sends: PUT /ticket/PROJ-1 body: { description: "...", expectedVersion: 5 }
 *    - Server: if ticket.version != 5 → 409 Conflict
 *    - Pros: Stateless server, no session tracking needed
 *    - Cons: Client must remember to pass version (but it's in the GET response)
 *    - This is how most real systems work (ETag / If-Match headers in HTTP)
 *
 * 5. COMPLEXITY:
 *    | Operation      | Time     | Notes                           |
 *    |---------------|----------|---------------------------------|
 *    | createTicket  | O(1)     | HashMap put                     |
 *    | getTicket     | O(1)     | HashMap get + tracker put       |
 *    | updateTicket  | O(1)     | Version check + field updates   |
 *    | searchByStatus| O(n)     | Full scan (index needed at scale)|
 *
 * 6. SCALABILITY:
 *    - Shard tickets by projectId → each project on different DB partition
 *    - Read replicas for GET calls (most traffic is reads)
 *    - Search: Elasticsearch index for status/assignee/text search
 *    - ReadTracker: can use Redis with TTL (auto-expire old read records)
 *    - Version column in DB: "UPDATE tickets SET desc=?, version=version+1
 *      WHERE id=? AND version=?" — rows_affected=0 means conflict
 *
 * 7. REAL-WORLD PARALLELS:
 *    - Atlassian JIRA: Uses optimistic locking with version field
 *    - Confluence: Shows "page was edited by X" dialog on conflict
 *    - Google Docs: Uses OT/CRDT for real-time merge (beyond scope here)
 *    - HTTP ETag: Server returns ETag on GET, client sends If-Match on PUT
 *    - DynamoDB: Conditional writes with version attribute
 *    - Git: Entire model is optimistic — merge conflicts on push
 */
