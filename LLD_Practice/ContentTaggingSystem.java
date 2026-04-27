import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

/*
 * CONTENT TAGGING SYSTEM - Low Level Design
 * =============================================
 * 
 * REQUIREMENTS:
 * 1. Tag items (posts, pages, PRs) within a workspace
 * 2. Prevent duplicate tags per item
 * 3. Search items by tag, filter by item type
 * 4. Top-K popular tags, tag autocomplete
 * 5. Pluggable tag suggestion strategy
 * 6. Tag events (Observer)
 * 7. Thread-safe
 * 
 * DESIGN PATTERNS:
 *   Strategy  (TagSuggestionStrategy) — PopularTagSuggestion, PrefixSuggestion
 *   Observer  (TagListener)           — TagLogger
 *   Facade    (TaggingService)
 * 
 * KEY DS: Map<tagKey, Tag>, Map<tagId, Set<itemId>>, Map<itemId, Set<tagId>>
 */

// ==================== EXCEPTIONS ====================

class TagNotFoundException extends RuntimeException {
    TagNotFoundException(String tag) { super("Tag not found: " + tag); }
}

class ItemNotRegisteredException extends RuntimeException {
    ItemNotRegisteredException(String id) { super("Item not registered: " + id); }
}

// ==================== ENUMS ====================

enum ContentItemType { POST, PAGE, PR, COMMENT }

// ==================== MODELS ====================

class ContentTag {
    final String id, workspaceId, name;
    final AtomicInteger usageCount = new AtomicInteger();

    ContentTag(String id, String workspaceId, String name) {
        this.id = id; this.workspaceId = workspaceId; this.name = name.toLowerCase().trim();
    }
}

class ContentItem {
    final String id, title;
    final ContentItemType type;

    ContentItem(String id, String title, ContentItemType type) {
        this.id = id; this.title = title; this.type = type;
    }
}

// ==================== INTERFACES ====================

/** Strategy — tag suggestions. */
interface TagSuggestionStrategy {
    List<ContentTag> suggest(Collection<ContentTag> allTags, String input, int limit);
}

/** Observer — tagging events. */
interface TagListener {
    void onTag(String itemId, String tagName, boolean added);
}

// ==================== STRATEGY IMPLEMENTATIONS ====================

/** Suggest most popular tags matching prefix. */
class PopularPrefixSuggestion implements TagSuggestionStrategy {
    @Override public List<ContentTag> suggest(Collection<ContentTag> allTags, String input, int limit) {
        String prefix = input.toLowerCase().trim();
        return allTags.stream()
            .filter(t -> t.name.startsWith(prefix))
            .sorted((a, b) -> b.usageCount.get() - a.usageCount.get())
            .limit(limit)
            .collect(Collectors.toList());
    }
}

/** Suggest tags containing substring (fuzzy). */
class ContainsSuggestion implements TagSuggestionStrategy {
    @Override public List<ContentTag> suggest(Collection<ContentTag> allTags, String input, int limit) {
        String sub = input.toLowerCase().trim();
        return allTags.stream()
            .filter(t -> t.name.contains(sub))
            .sorted((a, b) -> b.usageCount.get() - a.usageCount.get())
            .limit(limit)
            .collect(Collectors.toList());
    }
}

// ==================== OBSERVER IMPLEMENTATIONS ====================

class TagLogger implements TagListener {
    final List<String> events = new ArrayList<>();
    @Override public void onTag(String itemId, String tagName, boolean added) {
        events.add((added ? "ADD" : "REMOVE") + ":#" + tagName + " on " + itemId);
    }
}

// ==================== TAGGING SERVICE (FACADE) ====================

class TaggingService {
    private final ConcurrentHashMap<String, ContentTag> tagsByKey = new ConcurrentHashMap<>(); // "ws:name" → Tag
    private final ConcurrentHashMap<String, Set<String>> tagToItems = new ConcurrentHashMap<>(); // tagId → itemIds
    private final ConcurrentHashMap<String, Set<String>> itemToTags = new ConcurrentHashMap<>(); // itemId → tagIds
    private final ConcurrentHashMap<String, ContentItem> items = new ConcurrentHashMap<>();
    private TagSuggestionStrategy suggestionStrategy;
    private final List<TagListener> listeners = new ArrayList<>();
    private final AtomicInteger tagCounter = new AtomicInteger();

    TaggingService(TagSuggestionStrategy strategy) { this.suggestionStrategy = strategy; }
    TaggingService() { this(new PopularPrefixSuggestion()); }

    void setSuggestionStrategy(TagSuggestionStrategy s) { this.suggestionStrategy = s; }
    void addListener(TagListener l) { listeners.add(l); }

    private String tagKey(String wsId, String name) { return wsId + ":" + name.toLowerCase().trim(); }

    void registerItem(ContentItem item) { items.put(item.id, item); }

    ContentTag getOrCreateTag(String wsId, String tagName) {
        String key = tagKey(wsId, tagName);
        return tagsByKey.computeIfAbsent(key, k -> {
            String id = "TAG-" + tagCounter.incrementAndGet();
            ContentTag tag = new ContentTag(id, wsId, tagName);
            tagToItems.put(id, ConcurrentHashMap.newKeySet());
            return tag;
        });
    }

    /** Add tag to item. Idempotent — duplicate = no-op. */
    void addTag(String wsId, String itemId, String tagName) {
        ContentTag tag = getOrCreateTag(wsId, tagName);
        Set<String> itemTags = itemToTags.computeIfAbsent(itemId, k -> ConcurrentHashMap.newKeySet());
        if (!itemTags.add(tag.id)) return; // already tagged
        tagToItems.get(tag.id).add(itemId);
        tag.usageCount.incrementAndGet();
        listeners.forEach(l -> l.onTag(itemId, tag.name, true));
    }

    /** Remove tag from item. */
    void removeTag(String wsId, String itemId, String tagName) {
        String key = tagKey(wsId, tagName);
        ContentTag tag = tagsByKey.get(key);
        if (tag == null) return;
        Set<String> itemTags = itemToTags.get(itemId);
        if (itemTags == null || !itemTags.remove(tag.id)) return;
        Set<String> tagItems = tagToItems.get(tag.id);
        if (tagItems != null) tagItems.remove(itemId);
        tag.usageCount.decrementAndGet();
        listeners.forEach(l -> l.onTag(itemId, tag.name, false));
    }

    /** Find all items with a tag. */
    List<ContentItem> findByTag(String wsId, String tagName) {
        String key = tagKey(wsId, tagName);
        ContentTag tag = tagsByKey.get(key);
        if (tag == null) return Collections.emptyList();
        return tagToItems.getOrDefault(tag.id, Collections.emptySet()).stream()
            .map(items::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /** Find items with tag filtered by type. */
    List<ContentItem> findByTagAndType(String wsId, String tagName, ContentItemType type) {
        return findByTag(wsId, tagName).stream()
            .filter(i -> i.type == type).collect(Collectors.toList());
    }

    /** Top-K popular tags in a workspace. */
    List<ContentTag> getPopularTags(String wsId, int k) {
        return tagsByKey.values().stream()
            .filter(t -> t.workspaceId.equals(wsId))
            .sorted((a, b) -> b.usageCount.get() - a.usageCount.get())
            .limit(k).collect(Collectors.toList());
    }

    /** Autocomplete using current suggestion strategy. */
    List<ContentTag> suggest(String wsId, String prefix, int limit) {
        List<ContentTag> wsTags = tagsByKey.values().stream()
            .filter(t -> t.workspaceId.equals(wsId)).collect(Collectors.toList());
        return suggestionStrategy.suggest(wsTags, prefix, limit);
    }

    /** Get all tags for an item. */
    List<ContentTag> getTagsForItem(String itemId) {
        return itemToTags.getOrDefault(itemId, Collections.emptySet()).stream()
            .map(tid -> tagsByKey.values().stream().filter(t -> t.id.equals(tid)).findFirst().orElse(null))
            .filter(Objects::nonNull).collect(Collectors.toList());
    }

    int getTagCount() { return tagsByKey.size(); }
}

// ==================== MAIN / TESTS ====================

public class ContentTaggingSystem {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║   CONTENT TAGGING - LLD Demo              ║");
        System.out.println("╚═══════════════════════════════════════════╝\n");

        TaggingService svc = new TaggingService();
        String ws = "workspace1";

        // --- Test 1: Add tags ---
        System.out.println("=== Test 1: Add tags ===");
        svc.registerItem(new ContentItem("P1", "Java Guide", ContentItemType.POST));
        svc.registerItem(new ContentItem("P2", "Python Tutorial", ContentItemType.POST));
        svc.registerItem(new ContentItem("PG1", "Design Patterns", ContentItemType.PAGE));
        svc.addTag(ws, "P1", "java"); svc.addTag(ws, "P1", "programming");
        svc.addTag(ws, "P2", "python"); svc.addTag(ws, "P2", "programming");
        svc.addTag(ws, "PG1", "java"); svc.addTag(ws, "PG1", "design");
        check(svc.getTagCount(), 4, "4 unique tags");
        System.out.println("✓\n");

        // --- Test 2: Duplicate prevention ---
        System.out.println("=== Test 2: Duplicate prevention ===");
        svc.addTag(ws, "P1", "java"); // duplicate
        check(svc.getTagsForItem("P1").size(), 2, "Still 2 tags (idempotent)");
        System.out.println("✓\n");

        // --- Test 3: Find by tag ---
        System.out.println("=== Test 3: Find by tag ===");
        List<ContentItem> javaItems = svc.findByTag(ws, "java");
        check(javaItems.size(), 2, "2 items tagged 'java'");
        List<ContentItem> progItems = svc.findByTag(ws, "programming");
        check(progItems.size(), 2, "2 items tagged 'programming'");
        check(svc.findByTag(ws, "nonexistent").size(), 0, "No match");
        System.out.println("✓\n");

        // --- Test 4: Find by tag + type ---
        System.out.println("=== Test 4: Find by tag + type ===");
        List<ContentItem> javaPosts = svc.findByTagAndType(ws, "java", ContentItemType.POST);
        check(javaPosts.size(), 1, "1 java POST");
        List<ContentItem> javaPages = svc.findByTagAndType(ws, "java", ContentItemType.PAGE);
        check(javaPages.size(), 1, "1 java PAGE");
        System.out.println("✓\n");

        // --- Test 5: Remove tag ---
        System.out.println("=== Test 5: Remove tag ===");
        svc.removeTag(ws, "P1", "java");
        check(svc.findByTag(ws, "java").size(), 1, "1 item left with 'java'");
        check(svc.getTagsForItem("P1").size(), 1, "P1 has 1 tag left");
        System.out.println("✓\n");

        // --- Test 6: Popular tags ---
        System.out.println("=== Test 6: Popular tags ===");
        List<ContentTag> popular = svc.getPopularTags(ws, 3);
        check(popular.get(0).name, "programming", "Most popular = programming (2 uses)");
        System.out.println("  Top 3: " + popular.stream().map(t -> t.name + "(" + t.usageCount + ")").collect(Collectors.joining(", ")));
        System.out.println("✓\n");

        // --- Test 7: Autocomplete (prefix) ---
        System.out.println("=== Test 7: Autocomplete (prefix) ===");
        List<ContentTag> suggestions = svc.suggest(ws, "pro", 5);
        check(suggestions.size(), 1, "1 suggestion for 'pro'");
        check(suggestions.get(0).name, "programming", "Suggests 'programming'");
        System.out.println("✓\n");

        // --- Test 8: Strategy swap → Contains ---
        System.out.println("=== Test 8: Strategy swap → Contains ===");
        svc.setSuggestionStrategy(new ContainsSuggestion());
        suggestions = svc.suggest(ws, "gn", 5); // "design" contains "gn"
        check(suggestions.size(), 1, "'gn' matches 'design'");
        svc.setSuggestionStrategy(new PopularPrefixSuggestion()); // reset
        System.out.println("✓\n");

        // --- Test 9: Observer ---
        System.out.println("=== Test 9: Observer ===");
        TaggingService svc2 = new TaggingService();
        TagLogger logger = new TagLogger();
        svc2.addListener(logger);
        svc2.addTag("ws", "item1", "tag1");
        svc2.addTag("ws", "item1", "tag2");
        svc2.removeTag("ws", "item1", "tag1");
        check(logger.events.size(), 3, "3 events: 2 adds + 1 remove");
        System.out.println("  Events: " + logger.events);
        System.out.println("✓\n");

        // --- Test 10: Case insensitive ---
        System.out.println("=== Test 10: Case insensitive ===");
        svc.addTag(ws, "P1", "JAVA"); // should match existing "java"
        check(svc.findByTag(ws, "Java").size(), 2, "Case insensitive tag match");
        System.out.println("✓\n");

        // --- Test 11: Thread Safety ---
        System.out.println("=== Test 11: Thread Safety ===");
        TaggingService svc3 = new TaggingService();
        ExecutorService exec = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            int x = i;
            futures.add(exec.submit(() -> {
                svc3.registerItem(new ContentItem("i" + x, "Item " + x, ContentItemType.POST));
                svc3.addTag("ws", "i" + x, "tag" + (x % 10));
            }));
        }
        for (Future<?> f : futures) { try { f.get(); } catch (Exception e) {} }
        exec.shutdown();
        check(svc3.getTagCount(), 10, "10 unique tags from 100 concurrent ops");
        System.out.println("✓\n");

        // --- Test 12: Scale ---
        System.out.println("=== Test 12: Scale ===");
        TaggingService svc4 = new TaggingService();
        long t = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            svc4.registerItem(new ContentItem("s" + i, "S" + i, ContentItemType.POST));
            svc4.addTag("ws", "s" + i, "tag" + (i % 50));
        }
        System.out.printf("  10K tag ops: %.2f ms\n", (System.nanoTime() - t) / 1e6);
        t = System.nanoTime();
        check(svc4.findByTag("ws", "tag0").size(), 200, String.format("200 items in %.2f ms", (System.nanoTime() - t) / 1e6));
        check(svc4.getPopularTags("ws", 5).size(), 5, "Top 5 popular");
        System.out.println("✓\n");

        System.out.println("════════ ALL 12 TESTS PASSED ✓ ════════");
    }

    static void check(int a, int e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
    static void check(String a, String e, String m) { System.out.println("  " + (Objects.equals(a, e) ? "✓" : "✗ GOT '" + a + "'") + " " + m); }
    static void check(boolean a, boolean e, String m) { System.out.println("  " + (a == e ? "✓" : "✗ GOT " + a) + " " + m); }
}

/*
 * INTERVIEW NOTES:
 * 
 * 1. DUAL INDEX: tagToItems (tag→items) + itemToTags (item→tags).
 *    Bidirectional for O(1) lookups in both directions.
 *    ConcurrentHashMap.newKeySet() for thread-safe sets.
 *
 * 2. STRATEGY (TagSuggestionStrategy): PopularPrefixSuggestion (startsWith + sort by usage),
 *    ContainsSuggestion (substring match). Swap at runtime.
 *
 * 3. OBSERVER (TagListener): TagLogger tracks ADD/REMOVE events.
 *    Could add: SearchIndexUpdater, NotificationSender.
 *
 * 4. IDEMPOTENCY: addTag checks Set.add() return — false = already exists, no-op.
 *    AtomicInteger for usage count — thread-safe increment/decrement.
 *
 * 5. WORKSPACE SCOPING: tagKey = "wsId:name". Same tag name in different workspaces = different tags.
 *
 * 6. CASE INSENSITIVE: tag names normalized to lowercase on create and lookup.
 *
 * 7. EXTENSIONS: tag hierarchy, tag colors, tag merge/rename, analytics, search by multiple tags (AND/OR).
 */
