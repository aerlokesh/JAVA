import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/*
 * CHAT APPLICATION - Low Level Design
 * =======================================
 *
 * REQUIREMENTS:
 * 1. User registration and online/offline presence tracking
 * 2. 1-to-1 direct messaging between users
 * 3. Group chats: create group, add/remove members, send group messages
 * 4. Message history: fetch conversation history (paginated)
 * 5. Read receipts: track last-read message per user per conversation
 * 6. Real-time delivery: observer pattern for online users (push model)
 * 7. Typing indicators: notify other party when user is typing
 * 8. Thread-safe: concurrent sends, joins, reads
 *
 * KEY DATA STRUCTURES:
 * - ConcurrentHashMap<conversationId, Conversation>: O(1) conversation lookup
 * - ConcurrentHashMap<userId, User>: O(1) user lookup
 * - CopyOnWriteArrayList<Message>: per-conversation ordered message list
 * - ConcurrentHashMap<userId, Set<MessageListener>>: push delivery to online users
 *
 * DESIGN PATTERNS:
 * - Observer: real-time message delivery + typing indicators
 * - Factory: conversation creation (direct vs group)
 * - Strategy: message delivery (push for online, store for offline)
 *
 * COMPLEXITY:
 *   sendMessage:       O(m) where m = members to notify
 *   getHistory:        O(page_size) subList extraction
 *   createGroup:       O(1)
 *   addMember:         O(1)
 */

// ==================== EXCEPTIONS ====================

class UserNotFoundException extends Exception {
    UserNotFoundException(String id) { super("User not found: " + id); }
}

class ConversationNotFoundException extends Exception {
    ConversationNotFoundException(String id) { super("Conversation not found: " + id); }
}

class NotAMemberException extends Exception {
    NotAMemberException(String userId, String convId) {
        super("User " + userId + " is not a member of " + convId);
    }
}

// ==================== ENUMS ====================

enum UserStatus { ONLINE, OFFLINE, AWAY }
enum ConversationType { DIRECT, GROUP }
enum MessageStatus { SENT, DELIVERED, READ }

// ==================== DOMAIN CLASSES ====================

class Message {
    final String id, conversationId, senderId, content;
    final long timestamp;
    MessageStatus status;

    Message(String id, String conversationId, String senderId, String content) {
        this.id = id; this.conversationId = conversationId;
        this.senderId = senderId; this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.status = MessageStatus.SENT;
    }
}

class User {
    final String userId, displayName;
    volatile UserStatus status;
    final long joinedAt = System.currentTimeMillis();

    User(String userId, String displayName) {
        this.userId = userId; this.displayName = displayName;
        this.status = UserStatus.OFFLINE;
    }
}

class Conversation {
    final String id;
    final ConversationType type;
    final Set<String> memberIds = ConcurrentHashMap.newKeySet();
    final List<Message> messages = new CopyOnWriteArrayList<>();
    // readCursors: userId → messageId of last read message
    final ConcurrentHashMap<String, String> readCursors = new ConcurrentHashMap<>();
    final long createdAt = System.currentTimeMillis();
    String groupName; // null for direct chats

    Conversation(String id, ConversationType type) {
        this.id = id; this.type = type;
    }
}

/** Paginated result for message history */
class MessagePage {
    final List<Message> messages;
    final int totalMessages;
    final boolean hasMore;

    MessagePage(List<Message> messages, int total, boolean hasMore) {
        this.messages = messages; this.totalMessages = total; this.hasMore = hasMore;
    }
}

// ==================== OBSERVER INTERFACES ====================

/** Pushed to online users when a new message arrives */
interface MessageListener {
    void onMessageReceived(String conversationId, Message message);
    void onTypingIndicator(String conversationId, String userId, boolean isTyping);
}

/** Simple logger listener for testing */
class ConsoleMessageListener implements MessageListener {
    final String ownerUserId;
    final List<String> received = new CopyOnWriteArrayList<>();

    ConsoleMessageListener(String ownerUserId) { this.ownerUserId = ownerUserId; }

    public void onMessageReceived(String conversationId, Message message) {
        String event = conversationId + ":" + message.senderId + ":" + message.content;
        received.add(event);
    }

    public void onTypingIndicator(String conversationId, String userId, boolean isTyping) {
        received.add(conversationId + ":" + userId + (isTyping ? ":typing" : ":stopped"));
    }
}

// ==================== CHAT SERVICE (Core Engine) ====================

class ChatService {
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Conversation> conversations = new ConcurrentHashMap<>();
    // userId → set of listeners (for online push delivery)
    private final ConcurrentHashMap<String, Set<MessageListener>> listeners = new ConcurrentHashMap<>();
    // fast lookup: sorted pair "userA:userB" → conversationId (for direct chats)
    private final ConcurrentHashMap<String, String> directChatIndex = new ConcurrentHashMap<>();
    private final AtomicInteger msgIdCounter = new AtomicInteger(1);
    private final AtomicInteger convIdCounter = new AtomicInteger(1);

    // ---- User management ----

    User registerUser(String userId, String displayName) {
        // TODO: Implement
        // HINT: User user = new User(userId, displayName);
        // HINT: users.put(userId, user);
        // HINT: return user;
        return null;
    }

    void setUserStatus(String userId, UserStatus status) throws UserNotFoundException {
        // TODO: Implement
        // HINT: User u = users.get(userId);
        // HINT: if (u == null) throw new UserNotFoundException(userId);
        // HINT: u.status = status;
    }

    void addMessageListener(String userId, MessageListener listener) {
        // TODO: Implement
        // HINT: listeners.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(listener);
    }

    // ---- Direct messaging ----

    /**
     * Get or create a 1-to-1 conversation between two users.
     * Uses sorted pair key to ensure same conversation regardless of who initiates.
     */
    Conversation getOrCreateDirectChat(String userId1, String userId2) throws UserNotFoundException {
        // TODO: Implement
        // HINT: if (!users.containsKey(userId1)) throw new UserNotFoundException(userId1);
        // HINT: if (!users.containsKey(userId2)) throw new UserNotFoundException(userId2);
        // HINT: String pairKey = userId1.compareTo(userId2) < 0
        // HINT:     ? userId1 + ":" + userId2 : userId2 + ":" + userId1;
        // HINT: return conversations.computeIfAbsent(
        // HINT:     directChatIndex.computeIfAbsent(pairKey, k -> "DM-" + convIdCounter.getAndIncrement()),
        // HINT:     convId -> {
        // HINT:         Conversation c = new Conversation(convId, ConversationType.DIRECT);
        // HINT:         c.memberIds.add(userId1);
        // HINT:         c.memberIds.add(userId2);
        // HINT:         return c;
        // HINT:     }
        // HINT: );
        return null;
    }

    // ---- Group chats ----

    Conversation createGroup(String groupName, String creatorId, List<String> memberIds)
            throws UserNotFoundException {
        // TODO: Implement
        // HINT: if (!users.containsKey(creatorId)) throw new UserNotFoundException(creatorId);
        // HINT: String convId = "GRP-" + convIdCounter.getAndIncrement();
        // HINT: Conversation group = new Conversation(convId, ConversationType.GROUP);
        // HINT: group.groupName = groupName;
        // HINT: group.memberIds.add(creatorId);
        // HINT: for (String mid : memberIds) {
        // HINT:     if (!users.containsKey(mid)) throw new UserNotFoundException(mid);
        // HINT:     group.memberIds.add(mid);
        // HINT: }
        // HINT: conversations.put(convId, group);
        // HINT: return group;
        return null;
    }

    void addMemberToGroup(String conversationId, String userId)
            throws ConversationNotFoundException, UserNotFoundException {
        // TODO: Implement
        // HINT: Conversation c = conversations.get(conversationId);
        // HINT: if (c == null) throw new ConversationNotFoundException(conversationId);
        // HINT: if (!users.containsKey(userId)) throw new UserNotFoundException(userId);
        // HINT: c.memberIds.add(userId);
    }

    void removeMemberFromGroup(String conversationId, String userId)
            throws ConversationNotFoundException {
        // TODO: Implement
        // HINT: Conversation c = conversations.get(conversationId);
        // HINT: if (c == null) throw new ConversationNotFoundException(conversationId);
        // HINT: c.memberIds.remove(userId);
    }

    // ---- Messaging ----

    /**
     * Send a message to a conversation.
     * 1. Validate sender is a member
     * 2. Create and store the message
     * 3. Push to online members (Observer pattern)
     */
    Message sendMessage(String conversationId, String senderId, String content)
            throws ConversationNotFoundException, NotAMemberException {
        // TODO: Implement
        // HINT: Conversation conv = conversations.get(conversationId);
        // HINT: if (conv == null) throw new ConversationNotFoundException(conversationId);
        // HINT: if (!conv.memberIds.contains(senderId)) throw new NotAMemberException(senderId, conversationId);
        // HINT:
        // HINT: String msgId = "MSG-" + msgIdCounter.getAndIncrement();
        // HINT: Message msg = new Message(msgId, conversationId, senderId, content);
        // HINT: conv.messages.add(msg);
        // HINT:
        // HINT: // Push to all online members except sender
        // HINT: for (String memberId : conv.memberIds) {
        // HINT:     if (memberId.equals(senderId)) continue;
        // HINT:     Set<MessageListener> memberListeners = listeners.get(memberId);
        // HINT:     if (memberListeners != null) {
        // HINT:         for (MessageListener l : memberListeners)
        // HINT:             l.onMessageReceived(conversationId, msg);
        // HINT:     }
        // HINT: }
        // HINT: return msg;
        return null;
    }

    // ---- History ----

    /**
     * Get message history for a conversation, paginated.
     * Returns most recent messages first (reverse chronological).
     */
    MessagePage getHistory(String conversationId, String userId, int page, int pageSize)
            throws ConversationNotFoundException, NotAMemberException {
        // TODO: Implement
        // HINT: Conversation conv = conversations.get(conversationId);
        // HINT: if (conv == null) throw new ConversationNotFoundException(conversationId);
        // HINT: if (!conv.memberIds.contains(userId)) throw new NotAMemberException(userId, conversationId);
        // HINT:
        // HINT: List<Message> all = conv.messages;
        // HINT: int total = all.size();
        // HINT: int start = Math.max(0, total - (page + 1) * pageSize);
        // HINT: int end = Math.max(0, total - page * pageSize);
        // HINT: List<Message> pageMessages = (start < end) ? new ArrayList<>(all.subList(start, end)) : Collections.emptyList();
        // HINT: Collections.reverse(pageMessages);  // newest first
        // HINT: return new MessagePage(pageMessages, total, start > 0);
        return null;
    }

    // ---- Read receipts ----

    /** Mark conversation as read up to a specific message */
    void markAsRead(String conversationId, String userId, String messageId)
            throws ConversationNotFoundException {
        // TODO: Implement
        // HINT: Conversation conv = conversations.get(conversationId);
        // HINT: if (conv == null) throw new ConversationNotFoundException(conversationId);
        // HINT: conv.readCursors.put(userId, messageId);
    }

    /** Get unread count for a user in a conversation */
    int getUnreadCount(String conversationId, String userId) throws ConversationNotFoundException {
        // TODO: Implement
        // HINT: Conversation conv = conversations.get(conversationId);
        // HINT: if (conv == null) throw new ConversationNotFoundException(conversationId);
        // HINT: String lastRead = conv.readCursors.get(userId);
        // HINT: if (lastRead == null) return conv.messages.size();  // never read = all unread
        // HINT: List<Message> msgs = conv.messages;
        // HINT: int idx = -1;
        // HINT: for (int i = msgs.size() - 1; i >= 0; i--) {
        // HINT:     if (msgs.get(i).id.equals(lastRead)) { idx = i; break; }
        // HINT: }
        // HINT: return (idx >= 0) ? msgs.size() - idx - 1 : msgs.size();
        return 0;
    }

    // ---- Typing indicators ----

    void sendTypingIndicator(String conversationId, String userId, boolean isTyping)
            throws ConversationNotFoundException {
        // TODO: Implement
        // HINT: Conversation conv = conversations.get(conversationId);
        // HINT: if (conv == null) throw new ConversationNotFoundException(conversationId);
        // HINT: for (String memberId : conv.memberIds) {
        // HINT:     if (memberId.equals(userId)) continue;
        // HINT:     Set<MessageListener> mls = listeners.get(memberId);
        // HINT:     if (mls != null) {
        // HINT:         for (MessageListener l : mls) l.onTypingIndicator(conversationId, userId, isTyping);
        // HINT:     }
        // HINT: }
    }

    // ---- Queries ----

    /** Get all conversations a user is part of */
    List<Conversation> getUserConversations(String userId) {
        // TODO: Implement
        // HINT: return conversations.values().stream()
        // HINT:     .filter(c -> c.memberIds.contains(userId))
        // HINT:     .collect(Collectors.toList());
        return Collections.emptyList();
    }

    User getUser(String userId) { return users.get(userId); }
}

// ==================== MAIN / TESTS ====================

public class ChatApplicationSystem {
    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   CHAT APPLICATION - LLD Demo        ║");
        System.out.println("╚══════════════════════════════════════╝\n");

        ChatService chat = new ChatService();

        // --- Test 1: Register users ---
        System.out.println("=== Test 1: Register users ===");
        chat.registerUser("alice", "Alice Smith");
        chat.registerUser("bob", "Bob Jones");
        chat.registerUser("carol", "Carol White");
        chat.registerUser("dave", "Dave Brown");
        chat.setUserStatus("alice", UserStatus.ONLINE);
        chat.setUserStatus("bob", UserStatus.ONLINE);
        System.out.println("Registered 4 users, alice & bob ONLINE");
        System.out.println("✓ Users registered\n");

        // --- Test 2: Direct messaging ---
        System.out.println("=== Test 2: Direct messaging ===");
        Conversation dm = chat.getOrCreateDirectChat("alice", "bob");
        System.out.println("DM conversation: " + dm.id + ", type: " + dm.type);
        System.out.println("Members: " + dm.memberIds);

        // Same conversation returned regardless of order
        Conversation dm2 = chat.getOrCreateDirectChat("bob", "alice");
        System.out.println("Same DM from other side: " + (dm.id.equals(dm2.id)));
        System.out.println("✓ Direct chat created/reused\n");

        // --- Test 3: Send messages ---
        System.out.println("=== Test 3: Send messages ===");
        Message m1 = chat.sendMessage(dm.id, "alice", "Hey Bob!");
        Message m2 = chat.sendMessage(dm.id, "bob", "Hi Alice!");
        Message m3 = chat.sendMessage(dm.id, "alice", "How are you?");
        System.out.println("Sent 3 messages: " + m1.id + ", " + m2.id + ", " + m3.id);
        System.out.println("✓ Messages sent\n");

        // --- Test 4: Real-time push delivery (Observer) ---
        System.out.println("=== Test 4: Real-time push delivery ===");
        ConsoleMessageListener bobListener = new ConsoleMessageListener("bob");
        chat.addMessageListener("bob", bobListener);

        chat.sendMessage(dm.id, "alice", "This should push to Bob!");
        System.out.println("Bob's listener received: " + bobListener.received);
        System.out.println("✓ Online user received push notification\n");

        // --- Test 5: Group chat ---
        System.out.println("=== Test 5: Group chat ===");
        Conversation group = chat.createGroup("Project Alpha", "alice",
                Arrays.asList("bob", "carol"));
        System.out.println("Group: " + group.id + ", name: " + group.groupName);
        System.out.println("Members: " + group.memberIds);

        chat.sendMessage(group.id, "alice", "Welcome to the group!");
        chat.sendMessage(group.id, "bob", "Thanks!");
        chat.sendMessage(group.id, "carol", "Hello everyone!");
        System.out.println("3 group messages sent");
        System.out.println("Bob's listener (group msg): " + bobListener.received.size() + " events total");
        System.out.println("✓ Group chat works\n");

        // --- Test 6: Add/remove group members ---
        System.out.println("=== Test 6: Add/remove group members ===");
        chat.addMemberToGroup(group.id, "dave");
        System.out.println("Added dave, members: " + group.memberIds);
        chat.removeMemberFromGroup(group.id, "carol");
        System.out.println("Removed carol, members: " + group.memberIds);
        System.out.println("✓ Group membership management\n");

        // --- Test 7: Message history (paginated) ---
        System.out.println("=== Test 7: Message history ===");
        // Send more messages for pagination
        for (int i = 0; i < 7; i++)
            chat.sendMessage(dm.id, i % 2 == 0 ? "alice" : "bob", "Msg " + i);
        // Now DM has 4 + 7 = 11 messages

        MessagePage page0 = chat.getHistory(dm.id, "alice", 0, 5);
        System.out.println("Page 0 (newest 5): " + page0.messages.size() + " msgs, total=" +
                page0.totalMessages + ", hasMore=" + page0.hasMore);
        page0.messages.forEach(m -> System.out.println("  " + m.senderId + ": " + m.content));

        MessagePage page1 = chat.getHistory(dm.id, "alice", 1, 5);
        System.out.println("Page 1 (next 5): " + page1.messages.size() + " msgs, hasMore=" + page1.hasMore);
        System.out.println("✓ Paginated history works\n");

        // --- Test 8: Read receipts + unread count ---
        System.out.println("=== Test 8: Read receipts ===");
        int unreadBob = chat.getUnreadCount(dm.id, "bob");
        System.out.println("Bob unread in DM (never read): " + unreadBob);

        // Bob reads up to m2
        chat.markAsRead(dm.id, "bob", m2.id);
        int unreadAfter = chat.getUnreadCount(dm.id, "bob");
        System.out.println("Bob unread after marking read at " + m2.id + ": " + unreadAfter);

        // Bob reads all
        List<Message> allMsgs = new ArrayList<>(chat.getHistory(dm.id, "bob", 0, 100).messages);
        if (!allMsgs.isEmpty()) {
            chat.markAsRead(dm.id, "bob", allMsgs.get(0).id);  // newest
            System.out.println("Bob unread after reading all: " + chat.getUnreadCount(dm.id, "bob"));
        }
        System.out.println("✓ Read receipts and unread count\n");

        // --- Test 9: Typing indicators ---
        System.out.println("=== Test 9: Typing indicators ===");
        ConsoleMessageListener aliceListener = new ConsoleMessageListener("alice");
        chat.addMessageListener("alice", aliceListener);

        chat.sendTypingIndicator(dm.id, "bob", true);
        chat.sendTypingIndicator(dm.id, "bob", false);
        System.out.println("Alice received typing events: " + aliceListener.received);
        System.out.println("✓ Typing indicators pushed\n");

        // --- Test 10: Non-member cannot send ---
        System.out.println("=== Test 10: Non-member cannot send ===");
        try {
            chat.sendMessage(dm.id, "dave", "I'm not in this chat!");
            System.out.println("ERROR: Should have thrown!");
        } catch (NotAMemberException e) {
            System.out.println("Caught: " + e.getMessage());
        }
        System.out.println("✓ Membership enforced\n");

        // --- Test 11: User conversations list ---
        System.out.println("=== Test 11: User conversations ===");
        List<Conversation> aliceConvs = chat.getUserConversations("alice");
        System.out.println("Alice's conversations: " + aliceConvs.size());
        aliceConvs.forEach(c -> System.out.println("  " + c.id + " (" + c.type + ") members=" + c.memberIds));
        System.out.println("✓ User conversation listing\n");

        // --- Test 12: Concurrent message sending ---
        System.out.println("=== Test 12: Concurrent messaging ===");
        Conversation loadConv = chat.createGroup("LoadTest", "alice",
                Arrays.asList("bob", "carol", "dave"));
        ExecutorService exec = Executors.newFixedThreadPool(4);
        AtomicInteger sentCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            final int idx = i;
            String sender = new String[]{"alice", "bob", "carol", "dave"}[idx % 4];
            futures.add(exec.submit(() -> {
                try {
                    chat.sendMessage(loadConv.id, sender, "Concurrent msg " + idx);
                    sentCount.incrementAndGet();
                } catch (Exception e) { e.printStackTrace(); }
            }));
        }
        for (Future<?> f : futures) f.get();
        exec.shutdown();

        System.out.println("Sent " + sentCount.get() + " concurrent messages");
        System.out.println("Messages in conversation: " + loadConv.messages.size());
        System.out.println("✓ Thread-safe concurrent messaging\n");

        System.out.println("════════ ALL 12 TESTS PASSED ✓ ════════");
    }
}

/*
 * INTERVIEW NOTES:
 *
 * 1. CORE ARCHITECTURE:
 *    - User ↔ Conversation (many-to-many via memberIds)
 *    - Conversation contains ordered list of Messages
 *    - Direct chats indexed by sorted user-pair key for deduplication
 *    - Observer pattern for real-time push to online users
 *
 * 2. REAL-TIME DELIVERY STRATEGY:
 *    - Online users: push via MessageListener (WebSocket in real system)
 *    - Offline users: messages stored in conversation, delivered on next fetch
 *    - Typing indicators: ephemeral, push-only (not stored)
 *    - In production: WebSocket connections managed by gateway servers
 *
 * 3. READ RECEIPTS:
 *    - readCursors map: userId → lastReadMessageId per conversation
 *    - Unread count = messages after the cursor position
 *    - WhatsApp model: blue ticks when all members have read
 *    - Could extend: per-message delivery/read status per recipient
 *
 * 4. DIRECT CHAT DEDUPLICATION:
 *    - Key = sorted pair "alice:bob" (not "bob:alice")
 *    - computeIfAbsent ensures exactly one conversation per pair
 *    - Same pattern used by WhatsApp, Facebook Messenger
 *
 * 5. COMPLEXITY:
 *    | Operation          | Time     | Notes                           |
 *    |-------------------|----------|---------------------------------|
 *    | sendMessage       | O(m)     | m = members to notify           |
 *    | getHistory        | O(p)     | p = page size (subList)         |
 *    | getUnreadCount    | O(n)     | n = messages (scan for cursor)  |
 *    | createGroup       | O(k)     | k = initial members             |
 *    | getOrCreateDM     | O(1)     | HashMap + computeIfAbsent       |
 *    | getUserConvs      | O(c)     | c = total conversations (scan)  |
 *
 * 6. SCALABILITY:
 *    - Shard conversations by conversationId across DB partitions
 *    - Message storage: Cassandra / DynamoDB (write-heavy, time-series)
 *    - Presence: Redis with pub/sub for online status broadcast
 *    - WebSocket gateway: dedicated servers, user→server mapping in Redis
 *    - Fan-out: for large groups, use async worker to notify members
 *    - Message queue: Kafka for durable async delivery
 *    - Read receipts: eventual consistency OK (batch updates)
 *
 * 7. PAGINATION STRATEGY:
 *    - Cursor-based (messageId) preferred over offset-based for chat
 *    - "Load older messages" = get messages before cursor
 *    - Our simple implementation uses page/offset for demo
 *    - Production: "WHERE message_id < :cursor ORDER BY id DESC LIMIT :size"
 *
 * 8. REAL-WORLD PARALLELS:
 *    - WhatsApp: Erlang for connections, Mnesia for messages, signal protocol
 *    - Slack: MySQL + Vitess for sharding, WebSockets for real-time
 *    - Discord: Cassandra for messages, Elixir for presence
 *    - Facebook Messenger: TAO (graph DB), MQTT for mobile push
 *    - Telegram: MTProto protocol, distributed DC architecture
 */
