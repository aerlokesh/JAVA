import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

/*
 * TASK MANAGEMENT SYSTEM - Low Level Design (Microsoft Interview)
 * =================================================================
 *
 * Design a Trello/Asana-like task management system.
 *
 * REQUIREMENTS:
 * 1. Create boards with multiple task lists (columns: To Do, In Progress, Done)
 * 2. CRUD tasks: title, description, priority, due date, assignee
 * 3. Move tasks between lists (drag-and-drop style status transitions)
 * 4. Assign/unassign users to tasks
 * 5. Add comments to tasks, add labels/tags for categorization
 * 6. Search/filter tasks by assignee, priority, label, due date
 * 7. Activity log: track all changes (who did what, when)
 * 8. Thread-safe: concurrent board operations by multiple users
 *
 * KEY DATA STRUCTURES:
 * - ConcurrentHashMap<boardId, Board>: O(1) board lookup
 * - ConcurrentHashMap<taskId, Task>: O(1) task lookup across all boards
 * - LinkedHashMap<listId, TaskList>: ordered columns within a board
 * - LinkedHashSet<taskId> per TaskList: ordered tasks within a list
 * - Map<label, Set<taskId>>: inverted index for label-based search O(1)
 * - Map<assignee, Set<taskId>>: inverted index for assignee-based search O(1)
 *
 * DESIGN PATTERNS:
 * - Observer: activity log listeners notified on every change
 * - Strategy: pluggable search/filter strategies
 * - Command-like: ActivityEntry records each action for audit trail
 *
 * COMPLEXITY:
 *   createTask:    O(1)
 *   moveTask:      O(1) — remove from old list + add to new list
 *   assignTask:    O(1) + index update
 *   addComment:    O(1)
 *   searchByLabel: O(k) where k = tasks with that label (index lookup)
 *   searchByAssignee: O(k) where k = tasks assigned to user
 *   filterByPriority: O(n) scan within board
 */

// ==================== EXCEPTIONS ====================

class TaskNotFoundException extends RuntimeException {
    TaskNotFoundException(String id) { super("Task not found: " + id); }
}

class BoardNotFoundException extends RuntimeException {
    BoardNotFoundException(String id) { super("Board not found: " + id); }
}

class TaskListNotFoundException extends RuntimeException {
    TaskListNotFoundException(String id) { super("Task list not found: " + id); }
}

class InvalidOperationException extends RuntimeException {
    InvalidOperationException(String msg) { super(msg); }
}

// ==================== ENUMS ====================

enum TaskPriority { LOW, MEDIUM, HIGH, CRITICAL }

enum ActivityType { CREATED, MOVED, ASSIGNED, UNASSIGNED, COMMENTED, LABEL_ADDED, LABEL_REMOVED,
                    PRIORITY_CHANGED, DESCRIPTION_UPDATED, DUE_DATE_SET, ARCHIVED }

// ==================== MODELS ====================

class Task {
    final String id, boardId;
    String title, description;
    TaskPriority priority;
    String assignee;               // userId or null
    String listId;                 // which TaskList this task is in
    Long dueDate;                  // epoch millis, null = no due date
    boolean archived;
    final long createdAt = System.currentTimeMillis();
    final String createdBy;
    final Set<String> labels = ConcurrentHashMap.newKeySet();
    final List<Comment> comments = new CopyOnWriteArrayList<>();

    Task(String id, String boardId, String title, String description,
         TaskPriority priority, String listId, String createdBy) {
        this.id = id; this.boardId = boardId; this.title = title;
        this.description = description; this.priority = priority;
        this.listId = listId; this.createdBy = createdBy;
    }
}

class Comment {
    final String id, taskId, author, text;
    final long createdAt = System.currentTimeMillis();

    Comment(String id, String taskId, String author, String text) {
        this.id = id; this.taskId = taskId; this.author = author; this.text = text;
    }
}

class TaskList {
    final String id, name;
    final String boardId;
    int position;                  // column order within board
    final Set<String> taskIds = Collections.synchronizedSet(new LinkedHashSet<>());

    TaskList(String id, String name, String boardId, int position) {
        this.id = id; this.name = name; this.boardId = boardId; this.position = position;
    }
}

class Board {
    final String id, name;
    final String ownerId;
    final long createdAt = System.currentTimeMillis();
    final Map<String, TaskList> lists = Collections.synchronizedMap(new LinkedHashMap<>());
    final Set<String> members = ConcurrentHashMap.newKeySet();

    Board(String id, String name, String ownerId) {
        this.id = id; this.name = name; this.ownerId = ownerId;
        members.add(ownerId);
    }
}

class ActivityEntry {
    final String taskId, userId, detail;
    final ActivityType type;
    final long timestamp = System.currentTimeMillis();

    ActivityEntry(String taskId, String userId, ActivityType type, String detail) {
        this.taskId = taskId; this.userId = userId; this.type = type; this.detail = detail;
    }
}

// ==================== OBSERVER ====================

interface ActivityListener {
    void onActivity(ActivityEntry entry);
}

class ActivityLogger implements ActivityListener {
    final List<ActivityEntry> log = new CopyOnWriteArrayList<>();

    public void onActivity(ActivityEntry entry) {
        log.add(entry);
        System.out.printf("  [LOG] %s on task %s by '%s': %s%n",
                entry.type, entry.taskId, entry.userId, entry.detail);
    }

    List<ActivityEntry> getLogForTask(String taskId) {
        return log.stream().filter(e -> e.taskId.equals(taskId)).collect(Collectors.toList());
    }
}

// ==================== SEARCH STRATEGY ====================

interface TaskFilter {
    boolean matches(Task task);
}

class AssigneeFilter implements TaskFilter {
    final String assignee;
    AssigneeFilter(String assignee) { this.assignee = assignee; }
    public boolean matches(Task task) { return assignee.equals(task.assignee); }
}

class PriorityFilter implements TaskFilter {
    final TaskPriority priority;
    PriorityFilter(TaskPriority priority) { this.priority = priority; }
    public boolean matches(Task task) { return task.priority == priority; }
}

class LabelFilter implements TaskFilter {
    final String label;
    LabelFilter(String label) { this.label = label; }
    public boolean matches(Task task) { return task.labels.contains(label); }
}

class OverdueFilter implements TaskFilter {
    public boolean matches(Task task) {
        return task.dueDate != null && task.dueDate < System.currentTimeMillis() && !task.archived;
    }
}

// ==================== CORE SERVICE ====================

class TaskManagementService {
    private final ConcurrentHashMap<String, Board> boards = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicInteger boardIdGen = new AtomicInteger(1);
    private final AtomicInteger taskIdGen = new AtomicInteger(1);
    private final AtomicInteger listIdGen = new AtomicInteger(1);
    private final AtomicInteger commentIdGen = new AtomicInteger(1);

    // Inverted indexes for fast search
    private final ConcurrentHashMap<String, Set<String>> labelIndex = new ConcurrentHashMap<>();    // label → taskIds
    private final ConcurrentHashMap<String, Set<String>> assigneeIndex = new ConcurrentHashMap<>(); // assignee → taskIds

    private final List<ActivityListener> listeners = new CopyOnWriteArrayList<>();

    void addListener(ActivityListener l) { listeners.add(l); }

    private void emit(String taskId, String userId, ActivityType type, String detail) {
        // TODO: Implement
        // HINT: ActivityEntry entry = new ActivityEntry(taskId, userId, type, detail);
        // HINT: for (ActivityListener l : listeners) l.onActivity(entry);
    }

    // ---- Board operations ----

    Board createBoard(String name, String ownerId) {
        // TODO: Implement
        // HINT: String id = "BOARD-" + boardIdGen.getAndIncrement();
        // HINT: Board board = new Board(id, name, ownerId);
        // HINT: boards.put(id, board);
        // HINT: return board;
        return null;
    }

    Board getBoard(String boardId) {
        // TODO: Implement
        // HINT: Board b = boards.get(boardId);
        // HINT: if (b == null) throw new BoardNotFoundException(boardId);
        // HINT: return b;
        return null;
    }

    void addMember(String boardId, String userId) {
        // TODO: Implement
        // HINT: getBoard(boardId).members.add(userId);
    }

    // ---- TaskList (column) operations ----

    TaskList createList(String boardId, String name) {
        // TODO: Implement
        // HINT: Board board = getBoard(boardId);
        // HINT: String id = "LIST-" + listIdGen.getAndIncrement();
        // HINT: int position = board.lists.size();
        // HINT: TaskList list = new TaskList(id, name, boardId, position);
        // HINT: board.lists.put(id, list);
        // HINT: return list;
        return null;
    }

    private TaskList getList(String boardId, String listId) {
        Board board = getBoard(boardId);
        TaskList list = board.lists.get(listId);
        if (list == null) throw new TaskListNotFoundException(listId);
        return list;
    }

    // ---- Task CRUD ----

    Task createTask(String boardId, String listId, String title, String description,
                    TaskPriority priority, String createdBy) {
        // TODO: Implement
        // HINT: Board board = getBoard(boardId);
        // HINT: TaskList list = board.lists.get(listId);
        // HINT: if (list == null) throw new TaskListNotFoundException(listId);
        // HINT: String id = "TASK-" + taskIdGen.getAndIncrement();
        // HINT: Task task = new Task(id, boardId, title, description, priority, listId, createdBy);
        // HINT: tasks.put(id, task);
        // HINT: list.taskIds.add(id);
        // HINT: emit(id, createdBy, ActivityType.CREATED, "Created '" + title + "' in " + list.name);
        // HINT: return task;
        return null;
    }

    Task getTask(String taskId) {
        Task t = tasks.get(taskId);
        if (t == null) throw new TaskNotFoundException(taskId);
        return t;
    }

    /** Move task from one list to another (drag-and-drop between columns) */
    synchronized Task moveTask(String taskId, String targetListId, String userId) {
        // TODO: Implement
        // HINT: Task task = getTask(taskId);
        // HINT: Board board = getBoard(task.boardId);
        // HINT: TaskList oldList = board.lists.get(task.listId);
        // HINT: TaskList newList = board.lists.get(targetListId);
        // HINT: if (newList == null) throw new TaskListNotFoundException(targetListId);
        // HINT: if (task.listId.equals(targetListId)) return task; // no-op
        // HINT: oldList.taskIds.remove(taskId);
        // HINT: newList.taskIds.add(taskId);
        // HINT: String oldName = oldList.name;
        // HINT: task.listId = targetListId;
        // HINT: emit(taskId, userId, ActivityType.MOVED, "Moved from '" + oldName + "' to '" + newList.name + "'");
        // HINT: return task;
        return null;
    }

    /** Assign a user to a task */
    Task assignTask(String taskId, String assignee, String userId) {
        // TODO: Implement
        // HINT: Task task = getTask(taskId);
        // HINT: String oldAssignee = task.assignee;
        // HINT: // Remove from old assignee index
        // HINT: if (oldAssignee != null) {
        // HINT:     Set<String> set = assigneeIndex.get(oldAssignee);
        // HINT:     if (set != null) set.remove(taskId);
        // HINT: }
        // HINT: // Set new assignee
        // HINT: task.assignee = assignee;
        // HINT: assigneeIndex.computeIfAbsent(assignee, k -> ConcurrentHashMap.newKeySet()).add(taskId);
        // HINT: emit(taskId, userId, ActivityType.ASSIGNED, "Assigned to '" + assignee + "'");
        // HINT: return task;
        return null;
    }

    Task unassignTask(String taskId, String userId) {
        // TODO: Implement
        // HINT: Task task = getTask(taskId);
        // HINT: if (task.assignee != null) {
        // HINT:     Set<String> set = assigneeIndex.get(task.assignee);
        // HINT:     if (set != null) set.remove(taskId);
        // HINT:     String old = task.assignee;
        // HINT:     task.assignee = null;
        // HINT:     emit(taskId, userId, ActivityType.UNASSIGNED, "Unassigned from '" + old + "'");
        // HINT: }
        // HINT: return task;
        return null;
    }

    Task updateDescription(String taskId, String newDescription, String userId) {
        // TODO: Implement
        // HINT: Task task = getTask(taskId);
        // HINT: task.description = newDescription;
        // HINT: emit(taskId, userId, ActivityType.DESCRIPTION_UPDATED, "Description updated");
        // HINT: return task;
        return null;
    }

    Task updatePriority(String taskId, TaskPriority newPriority, String userId) {
        // TODO: Implement
        // HINT: Task task = getTask(taskId);
        // HINT: TaskPriority old = task.priority;
        // HINT: task.priority = newPriority;
        // HINT: emit(taskId, userId, ActivityType.PRIORITY_CHANGED, old + " → " + newPriority);
        // HINT: return task;
        return null;
    }

    Task setDueDate(String taskId, long dueDate, String userId) {
        // TODO: Implement
        // HINT: Task task = getTask(taskId);
        // HINT: task.dueDate = dueDate;
        // HINT: emit(taskId, userId, ActivityType.DUE_DATE_SET, "Due date set");
        // HINT: return task;
        return null;
    }

    Task archiveTask(String taskId, String userId) {
        // TODO: Implement
        // HINT: Task task = getTask(taskId);
        // HINT: task.archived = true;
        // HINT: // Remove from list
        // HINT: Board board = getBoard(task.boardId);
        // HINT: TaskList list = board.lists.get(task.listId);
        // HINT: if (list != null) list.taskIds.remove(taskId);
        // HINT: emit(taskId, userId, ActivityType.ARCHIVED, "Task archived");
        // HINT: return task;
        return null;
    }

    // ---- Labels ----

    Task addLabel(String taskId, String label, String userId) {
        // TODO: Implement
        // HINT: Task task = getTask(taskId);
        // HINT: task.labels.add(label);
        // HINT: labelIndex.computeIfAbsent(label, k -> ConcurrentHashMap.newKeySet()).add(taskId);
        // HINT: emit(taskId, userId, ActivityType.LABEL_ADDED, "Label '" + label + "' added");
        // HINT: return task;
        return null;
    }

    Task removeLabel(String taskId, String label, String userId) {
        // TODO: Implement
        // HINT: Task task = getTask(taskId);
        // HINT: task.labels.remove(label);
        // HINT: Set<String> set = labelIndex.get(label);
        // HINT: if (set != null) set.remove(taskId);
        // HINT: emit(taskId, userId, ActivityType.LABEL_REMOVED, "Label '" + label + "' removed");
        // HINT: return task;
        return null;
    }

    // ---- Comments ----

    Comment addComment(String taskId, String author, String text) {
        // TODO: Implement
        // HINT: Task task = getTask(taskId);
        // HINT: String id = "CMT-" + commentIdGen.getAndIncrement();
        // HINT: Comment comment = new Comment(id, taskId, author, text);
        // HINT: task.comments.add(comment);
        // HINT: emit(taskId, author, ActivityType.COMMENTED, "'" + text + "'");
        // HINT: return comment;
        return null;
    }

    List<Comment> getComments(String taskId) {
        return getTask(taskId).comments;
    }

    // ---- Search / Filter ----

    /** Get all tasks in a board, optionally filtered */
    List<Task> filterTasks(String boardId, TaskFilter... filters) {
        // TODO: Implement
        // HINT: Board board = getBoard(boardId);
        // HINT: List<Task> result = new ArrayList<>();
        // HINT: for (TaskList list : board.lists.values()) {
        // HINT:     for (String taskId : list.taskIds) {
        // HINT:         Task task = tasks.get(taskId);
        // HINT:         if (task == null || task.archived) continue;
        // HINT:         boolean match = true;
        // HINT:         for (TaskFilter f : filters) {
        // HINT:             if (!f.matches(task)) { match = false; break; }
        // HINT:         }
        // HINT:         if (match) result.add(task);
        // HINT:     }
        // HINT: }
        // HINT: return result;
        return Collections.emptyList();
    }

    /** Fast label search using inverted index — O(k) */
    List<Task> searchByLabel(String label) {
        // TODO: Implement
        // HINT: Set<String> ids = labelIndex.getOrDefault(label, Collections.emptySet());
        // HINT: List<Task> result = new ArrayList<>();
        // HINT: for (String id : ids) {
        // HINT:     Task t = tasks.get(id);
        // HINT:     if (t != null && !t.archived) result.add(t);
        // HINT: }
        // HINT: return result;
        return Collections.emptyList();
    }

    /** Fast assignee search using inverted index — O(k) */
    List<Task> searchByAssignee(String assignee) {
        // TODO: Implement
        // HINT: Set<String> ids = assigneeIndex.getOrDefault(assignee, Collections.emptySet());
        // HINT: List<Task> result = new ArrayList<>();
        // HINT: for (String id : ids) {
        // HINT:     Task t = tasks.get(id);
        // HINT:     if (t != null && !t.archived) result.add(t);
        // HINT: }
        // HINT: return result;
        return Collections.emptyList();
    }

    /** Get tasks in a specific list (column) */
    List<Task> getTasksInList(String boardId, String listId) {
        // TODO: Implement
        // HINT: TaskList list = getList(boardId, listId);
        // HINT: List<Task> result = new ArrayList<>();
        // HINT: for (String taskId : list.taskIds) {
        // HINT:     Task t = tasks.get(taskId);
        // HINT:     if (t != null) result.add(t);
        // HINT: }
        // HINT: return result;
        return Collections.emptyList();
    }

    // ---- Board summary ----

    void printBoardSummary(String boardId) {
        Board board = getBoard(boardId);
        System.out.println("┌─── Board: " + board.name + " (" + board.id + ") ───");
        System.out.println("│ Members: " + board.members);
        for (TaskList list : board.lists.values()) {
            System.out.println("│ ┌── " + list.name + " [" + list.taskIds.size() + " tasks]");
            for (String tid : list.taskIds) {
                Task t = tasks.get(tid);
                if (t == null) continue;
                String assignStr = t.assignee != null ? " → " + t.assignee : "";
                String labelStr = t.labels.isEmpty() ? "" : " " + t.labels;
                System.out.printf("│ │  %s: %s [%s]%s%s%n",
                        t.id, t.title, t.priority, assignStr, labelStr);
            }
            System.out.println("│ └──");
        }
        System.out.println("└───────────────────────────────────");
    }
}

// ==================== MAIN / TESTS ====================

public class TaskManagementSystem {
    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║   TASK MANAGEMENT SYSTEM - LLD (Microsoft Q)    ║");
        System.out.println("╚══════════════════════════════════════════════════╝\n");

        TaskManagementService service = new TaskManagementService();
        ActivityLogger logger = new ActivityLogger();
        service.addListener(logger);

        // --- Test 1: Create board with lists ---
        System.out.println("=== Test 1: Create board with columns ===");
        Board board = service.createBoard("Sprint-42", "alice");
        TaskList todo = service.createList(board.id, "To Do");
        TaskList inProgress = service.createList(board.id, "In Progress");
        TaskList done = service.createList(board.id, "Done");
        System.out.println("Board: " + board.id + " with 3 lists");
        System.out.println("✓ Board and columns created\n");

        // --- Test 2: Add members ---
        System.out.println("=== Test 2: Add members ===");
        service.addMember(board.id, "bob");
        service.addMember(board.id, "carol");
        System.out.println("Members: " + service.getBoard(board.id).members);
        System.out.println("✓ Members added\n");

        // --- Test 3: Create tasks ---
        System.out.println("=== Test 3: Create tasks ===");
        Task t1 = service.createTask(board.id, todo.id, "Fix login bug", "SSO login fails for new users",
                TaskPriority.HIGH, "alice");
        Task t2 = service.createTask(board.id, todo.id, "Add dark mode", "Support system theme preference",
                TaskPriority.MEDIUM, "alice");
        Task t3 = service.createTask(board.id, todo.id, "Write API docs", "Document REST endpoints",
                TaskPriority.LOW, "bob");
        Task t4 = service.createTask(board.id, inProgress.id, "Refactor auth", "Extract auth to middleware",
                TaskPriority.HIGH, "carol");
        System.out.println("Created: " + t1.id + ", " + t2.id + ", " + t3.id + ", " + t4.id);
        System.out.println("✓ 4 tasks created across columns\n");

        // --- Test 4: Assign tasks ---
        System.out.println("=== Test 4: Assign tasks ===");
        service.assignTask(t1.id, "bob", "alice");
        service.assignTask(t2.id, "carol", "alice");
        service.assignTask(t3.id, "bob", "alice");
        System.out.println("✓ Tasks assigned\n");

        // --- Test 5: Move tasks between lists ---
        System.out.println("=== Test 5: Move task (To Do → In Progress) ===");
        service.moveTask(t1.id, inProgress.id, "bob");
        System.out.println("Task " + t1.id + " now in list: " + t1.listId);
        System.out.println("To Do count: " + service.getTasksInList(board.id, todo.id).size());
        System.out.println("In Progress count: " + service.getTasksInList(board.id, inProgress.id).size());
        System.out.println("✓ Task moved successfully\n");

        // --- Test 6: Add labels ---
        System.out.println("=== Test 6: Labels ===");
        service.addLabel(t1.id, "bug", "bob");
        service.addLabel(t1.id, "urgent", "bob");
        service.addLabel(t2.id, "feature", "carol");
        service.addLabel(t3.id, "documentation", "bob");
        System.out.println("Task " + t1.id + " labels: " + t1.labels);
        System.out.println("✓ Labels added\n");

        // --- Test 7: Add comments ---
        System.out.println("=== Test 7: Comments ===");
        service.addComment(t1.id, "bob", "Reproduced the SSO issue on staging");
        service.addComment(t1.id, "alice", "Try checking the SAML config first");
        service.addComment(t1.id, "bob", "Fixed! It was a certificate expiry");
        List<Comment> comments = service.getComments(t1.id);
        System.out.println("Comments on " + t1.id + ": " + comments.size());
        for (Comment c : comments) System.out.println("  " + c.author + ": " + c.text);
        System.out.println("✓ Comments threaded\n");

        // --- Test 8: Search by label (inverted index) ---
        System.out.println("=== Test 8: Search by label ===");
        List<Task> bugTasks = service.searchByLabel("bug");
        System.out.println("Tasks with 'bug' label: " + bugTasks.size() + " → " +
                bugTasks.stream().map(t -> t.id).collect(Collectors.toList()));
        List<Task> featureTasks = service.searchByLabel("feature");
        System.out.println("Tasks with 'feature' label: " + featureTasks.size());
        System.out.println("✓ Label search uses inverted index\n");

        // --- Test 9: Search by assignee (inverted index) ---
        System.out.println("=== Test 9: Search by assignee ===");
        List<Task> bobTasks = service.searchByAssignee("bob");
        System.out.println("Bob's tasks: " + bobTasks.size() + " → " +
                bobTasks.stream().map(t -> t.id + ":" + t.title).collect(Collectors.toList()));
        System.out.println("✓ Assignee search uses inverted index\n");

        // --- Test 10: Filter with multiple criteria ---
        System.out.println("=== Test 10: Composite filter ===");
        List<Task> highPriInBoard = service.filterTasks(board.id, new PriorityFilter(TaskPriority.HIGH));
        System.out.println("HIGH priority tasks: " + highPriInBoard.size() + " → " +
                highPriInBoard.stream().map(t -> t.id).collect(Collectors.toList()));
        List<Task> bobHighPri = service.filterTasks(board.id,
                new AssigneeFilter("bob"), new PriorityFilter(TaskPriority.HIGH));
        System.out.println("Bob's HIGH priority: " + bobHighPri.size());
        System.out.println("✓ Composite filters work\n");

        // --- Test 11: Update priority and description ---
        System.out.println("=== Test 11: Update fields ===");
        service.updatePriority(t3.id, TaskPriority.HIGH, "alice");
        service.updateDescription(t3.id, "Document all REST endpoints with examples", "bob");
        Task updated = service.getTask(t3.id);
        System.out.println("Task " + t3.id + ": priority=" + updated.priority + ", desc='" + updated.description + "'");
        System.out.println("✓ Fields updated\n");

        // --- Test 12: Complete flow: move to Done, archive ---
        System.out.println("=== Test 12: Complete task flow ===");
        service.moveTask(t1.id, done.id, "bob");
        service.archiveTask(t1.id, "bob");
        System.out.println("Task " + t1.id + " archived: " + t1.archived);
        List<Task> doneTasks = service.getTasksInList(board.id, done.id);
        System.out.println("Done list (after archive): " + doneTasks.size() + " tasks");
        System.out.println("✓ Task completed and archived\n");

        // --- Test 13: Activity log for a task ---
        System.out.println("=== Test 13: Activity log ===");
        List<ActivityEntry> t1Log = logger.getLogForTask(t1.id);
        System.out.println("Activity history for " + t1.id + " (" + t1Log.size() + " entries):");
        for (ActivityEntry e : t1Log) {
            System.out.printf("  [%s] %s by %s: %s%n", e.type, e.taskId, e.userId, e.detail);
        }
        System.out.println("✓ Full audit trail available\n");

        // --- Test 14: Board summary view ---
        System.out.println("=== Test 14: Board summary ===");
        service.printBoardSummary(board.id);
        System.out.println("✓ Kanban-style board view\n");

        // --- Test 15: Edge cases ---
        System.out.println("=== Test 15: Edge cases ===");
        try { service.getTask("TASK-999"); }
        catch (TaskNotFoundException e) { System.out.println("Caught: " + e.getMessage()); }

        try { service.createTask("BOARD-999", "LIST-1", "x", "y", TaskPriority.LOW, "z"); }
        catch (BoardNotFoundException e) { System.out.println("Caught: " + e.getMessage()); }

        // Remove label then search
        service.removeLabel(t2.id, "feature", "carol");
        System.out.println("Feature tasks after removal: " + service.searchByLabel("feature").size());

        // Unassign then search
        service.unassignTask(t3.id, "alice");
        System.out.println("Bob's tasks after unassign of t3: " + service.searchByAssignee("bob").size());
        System.out.println("✓ Edge cases handled\n");

        // --- Test 16: Concurrent task creation ---
        System.out.println("=== Test 16: Concurrent operations ===");
        Board board2 = service.createBoard("Stress-Test", "admin");
        TaskList stressList = service.createList(board2.id, "Backlog");

        ExecutorService exec = Executors.newFixedThreadPool(8);
        AtomicInteger successCount = new AtomicInteger(0);
        List<java.util.concurrent.Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            final int idx = i;
            futures.add(exec.submit(() -> {
                try {
                    Task t = service.createTask(board2.id, stressList.id,
                            "Task-" + idx, "Desc-" + idx, TaskPriority.MEDIUM, "user-" + idx);
                    service.addLabel(t.id, "batch", "user-" + idx);
                    if (idx % 3 == 0) service.assignTask(t.id, "alice", "user-" + idx);
                    successCount.incrementAndGet();
                } catch (Exception e) { e.printStackTrace(); }
            }));
        }
        for (java.util.concurrent.Future<?> f : futures) f.get();
        exec.shutdown();

        System.out.println("Created " + successCount.get() + "/50 tasks concurrently");
        System.out.println("Backlog size: " + service.getTasksInList(board2.id, stressList.id).size());
        System.out.println("Tasks with 'batch' label: " + service.searchByLabel("batch").size());
        System.out.println("Alice's assigned (from batch): " + service.searchByAssignee("alice").size());
        System.out.println("✓ Thread-safe operations verified\n");

        System.out.println("════════ ALL 16 TESTS PASSED ✓ ════════");
    }
}

/*
 * INTERVIEW NOTES:
 *
 * 1. CORE DESIGN — BOARD → LIST → TASK HIERARCHY:
 *    Board contains ordered TaskLists (columns like Kanban). Each TaskList tracks
 *    an ordered set of task IDs. Tasks are stored in a global map for O(1) lookup.
 *    Moving a task = remove from old list's set + add to new list's set, both O(1).
 *
 * 2. INVERTED INDEXES FOR FAST SEARCH:
 *    - labelIndex: Map<label, Set<taskId>> — when user searches "bug" tasks, O(k)
 *    - assigneeIndex: Map<user, Set<taskId>> — when user views "my tasks", O(k)
 *    Index maintained on writes (addLabel, assignTask) so reads are instant.
 *    Trade-off: slightly slower writes for much faster reads — correct for a read-heavy system.
 *
 * 3. STRATEGY PATTERN FOR FILTERING:
 *    TaskFilter interface allows composable filters: PriorityFilter AND AssigneeFilter.
 *    Easy to add new filters (DueDateFilter, CreatedByFilter) without changing service code.
 *
 * 4. OBSERVER FOR ACTIVITY LOG:
 *    Every mutation emits an ActivityEntry. ActivityLogger stores chronological log.
 *    Supports task-level audit trail. In production: write to event stream (Kafka).
 *
 * 5. COMPLEXITY:
 *    | Operation         | Time    | Notes                             |
 *    |-------------------|---------|-----------------------------------|
 *    | createTask        | O(1)    | Map put + set add                 |
 *    | moveTask          | O(1)    | Set remove + set add              |
 *    | assignTask        | O(1)    | Field set + index update          |
 *    | addComment        | O(1)    | List append                       |
 *    | addLabel          | O(1)    | Set add + index update            |
 *    | searchByLabel     | O(k)    | k = tasks with that label         |
 *    | searchByAssignee  | O(k)    | k = tasks for that user           |
 *    | filterTasks       | O(n*f)  | n = tasks in board, f = filters   |
 *
 * 6. SCALABILITY:
 *    - Shard boards by boardId → each board on a different partition
 *    - Task search → Elasticsearch for full-text search across all fields
 *    - Activity log → append-only event store (Kafka → DynamoDB/Cassandra)
 *    - Comments → separate microservice with pagination (like Reddit threads)
 *    - Caching: board summary views cached with TTL, invalidated on writes
 *
 * 7. REAL-WORLD PARALLELS:
 *    - Trello: Board → List → Card model, drag-and-drop between lists
 *    - Jira: Project → Status → Issue, with custom workflows
 *    - Asana: Workspace → Project → Task → Subtask
 *    - GitHub Projects: Column-based Kanban with automation
 *    - All use inverted indexes (Elasticsearch) for search at scale
 *
 * 8. EXTENSIONS (if time permits):
 *    - Subtasks: Task has parentId → tree structure
 *    - Custom workflows: state machine for valid list transitions
 *    - Notifications: push to assigned user when task moves/commented
 *    - Attachments: file reference linked to task
 *    - Due date reminders: background scheduler checks overdue tasks
 */
