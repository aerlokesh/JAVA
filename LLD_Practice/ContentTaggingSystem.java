import java.time.*;
import java.util.*;

// ===== ENUMS =====

enum ItemType { JIRA_ISSUE, CONFLUENCE_PAGE, BITBUCKET_PR }

// ===== DOMAIN CLASSES =====

/**
 * Tag — scoped to a workspace. Normalized name for dedup.
 * Entity 1 from HLD
 */
class Tag {
    private final String tagId;
    private final String workspaceId;
    private final String name;           // normalized: lowercase, trimmed
    private final String displayName;    // original casing
    private int usageCount;
    private final LocalDateTime createdAt;
    
    public Tag(String workspaceId, String displayName) {
        this.tagId = "TAG-" + UUID.randomUUID().toString().substring(0, 6);
        this.workspaceId = workspaceId;
        this.name = displayName.toLowerCase().trim();
        this.displayName = displayName;
        this.usageCount = 0;
        this.createdAt = LocalDateTime.now();
    }
    
    public String getTagId() { return tagId; }
    public String getWorkspaceId() { return workspaceId; }
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public int getUsageCount() { return usageCount; }
    
    public void incrementUsage() { usageCount++; }
    public void decrementUsage() { if (usageCount > 0) usageCount--; }
    
    @Override
    public String toString() { return "#" + name + "(" + usageCount + ")"; }
}

/**
 * TagAssociation — links a tag to an item (JIRA issue, Confluence page, etc.)
 * Entity 2 from HLD
 */
class TagAssociation {
    private final String tagId;
    private final String itemId;
    private final ItemType itemType;
    private final String workspaceId;
    private final String taggedBy;
    private final LocalDateTime taggedAt;
    
    public TagAssociation(String tagId, String itemId, ItemType itemType, String workspaceId, String taggedBy) {
        this.tagId = tagId;
        this.itemId = itemId;
        this.itemType = itemType;
        this.workspaceId = workspaceId;
        this.taggedBy = taggedBy;
        this.taggedAt = LocalDateTime.now();
    }
    
    public String getTagId() { return tagId; }
    public String getItemId() { return itemId; }
    public ItemType getItemType() { return itemType; }
    public String getWorkspaceId() { return workspaceId; }
    public String getTaggedBy() { return taggedBy; }
    
    @Override
    public String toString() { return itemId + "[" + itemType + "] tagged=" + tagId; }
}

/**
 * Item — represents a taggable entity (JIRA issue, Confluence page, etc.)
 */
class TaggableItem {
    private final String itemId;
    private final ItemType itemType;
    private final String title;
    private final String workspaceId;
    
    public TaggableItem(String itemId, ItemType itemType, String title, String workspaceId) {
        this.itemId = itemId;
        this.itemType = itemType;
        this.title = title;
        this.workspaceId = workspaceId;
    }
    
    public String getItemId() { return itemId; }
    public ItemType getItemType() { return itemType; }
    public String getTitle() { return title; }
    public String getWorkspaceId() { return workspaceId; }
    
    @Override
    public String toString() { return itemId + "[" + itemType + ": " + title + "]"; }
}

// ===== SERVICE =====

/**
 * Atlassian Tagging System - Low Level Design (LLD)
 * Based on: Hello Interview Atlassian Tagging HLD
 * 
 * PROBLEM: Design a UNIFIED tagging system across Atlassian products:
 * 1. Add/remove tags on JIRA issues, Confluence pages, Bitbucket PRs
 * 2. Cross-product search — click tag → see ALL items across products
 * 3. Top-K popular tags dashboard (per workspace)
 * 4. Tag autocomplete (prefix search)
 * 5. Workspace scoping — tags in Workspace A ≠ Workspace B
 * 
 * KEY DATA STRUCTURES:
 * - Tags scoped by workspace: Map<workspaceId:tagName, Tag>
 * - Inverted index: tagId → Set<itemId> (cross-product search)
 * - Forward index: itemId → Set<tagId> (tags for an item)
 * - Associations: explicit many-to-many with metadata
 */
class TaggingService {
    // Composite key: "workspaceId:tagName" → Tag (workspace-scoped dedup)
    private final Map<String, Tag> tagsByKey;
    private final Map<String, Tag> tagsById;                     // tagId → Tag
    private final Map<String, TaggableItem> items;               // itemId → Item
    private final Map<String, Set<String>> tagToItems;           // tagId → Set<itemIds> (inverted index)
    private final Map<String, Set<String>> itemToTags;           // itemId → Set<tagIds> (forward index)
    private final List<TagAssociation> associations;             // all associations (for audit)
    
    public TaggingService() {
        this.tagsByKey = new HashMap<>();
        this.tagsById = new HashMap<>();
        this.items = new HashMap<>();
        this.tagToItems = new HashMap<>();
        this.itemToTags = new HashMap<>();
        this.associations = new ArrayList<>();
    }
    
    /** Register an item (JIRA issue, Confluence page, etc.) */
    public void registerItem(TaggableItem item) {
        items.put(item.getItemId(), item);
    }
    
    // ===== WORKSPACE-SCOPED TAG MANAGEMENT =====
    
    private String tagKey(String workspaceId, String tagName) {
        return workspaceId + ":" + tagName.toLowerCase().trim();
    }
    
    /**
     * Get or create a tag (auto-create, workspace-scoped)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Build composite key: workspaceId + ":" + normalized tagName
     * 2. If tag exists → return it
     * 3. If new → create Tag, store by key and by ID, init inverted index
     */
    private Tag getOrCreateTag(String workspaceId, String tagName) {
        // TODO: Implement
        // HINT: String key = tagKey(workspaceId, tagName);
        // HINT: return tagsByKey.computeIfAbsent(key, k -> {
        //     Tag tag = new Tag(workspaceId, tagName);
        //     tagsById.put(tag.getTagId(), tag);
        //     tagToItems.put(tag.getTagId(), new HashSet<>());
        //     return tag;
        // });
        return null;
    }
    
    // ===== ADD / REMOVE TAGS =====
    
    /**
     * Add a tag to an item (cross-product tagging)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get or create tag (workspace-scoped)
     * 2. Check if item already has this tag → skip if duplicate
     * 3. Add to inverted index: tagId → itemIds
     * 4. Add to forward index: itemId → tagIds
     * 5. Create TagAssociation record
     * 6. Increment tag usage count
     */
    public void addTag(String itemId, String workspaceId, String tagName, String userId) {
        // TODO: Implement
        // HINT: Tag tag = getOrCreateTag(workspaceId, tagName);
        //
        // HINT: // Check duplicate
        // HINT: Set<String> itemTags = itemToTags.computeIfAbsent(itemId, k -> new HashSet<>());
        // HINT: if (itemTags.contains(tag.getTagId())) return; // already tagged
        //
        // HINT: // Update indexes
        // HINT: tagToItems.get(tag.getTagId()).add(itemId);
        // HINT: itemTags.add(tag.getTagId());
        // HINT: tag.incrementUsage();
        //
        // HINT: // Record association
        // HINT: TaggableItem item = items.get(itemId);
        // HINT: ItemType type = item != null ? item.getItemType() : ItemType.JIRA_ISSUE;
        // HINT: associations.add(new TagAssociation(tag.getTagId(), itemId, type, workspaceId, userId));
        // HINT: System.out.println("  🏷️ " + itemId + " + #" + tag.getName());
    }
    
    /**
     * Remove a tag from an item
     * 
     * IMPLEMENTATION HINTS:
     * 1. Find tag by workspace + name
     * 2. Remove from inverted + forward indexes
     * 3. Decrement usage count
     */
    public void removeTag(String itemId, String workspaceId, String tagName) {
        // TODO: Implement
        // HINT: String key = tagKey(workspaceId, tagName);
        // HINT: Tag tag = tagsByKey.get(key);
        // HINT: if (tag == null) return;
        //
        // HINT: Set<String> itemIds = tagToItems.get(tag.getTagId());
        // HINT: if (itemIds != null) itemIds.remove(itemId);
        //
        // HINT: Set<String> tagIds = itemToTags.get(itemId);
        // HINT: if (tagIds != null) tagIds.remove(tag.getTagId());
        //
        // HINT: tag.decrementUsage();
        // HINT: System.out.println("  🏷️ " + itemId + " - #" + tag.getName());
    }
    
    // ===== CROSS-PRODUCT SEARCH =====
    
    /**
     * Find ALL items with a tag (across JIRA, Confluence, Bitbucket)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Find tag by workspace + name
     * 2. Get item IDs from inverted index
     * 3. Map to TaggableItem objects
     * 4. Return grouped by ItemType if needed
     */
    public List<TaggableItem> findByTag(String workspaceId, String tagName) {
        // TODO: Implement
        // HINT: String key = tagKey(workspaceId, tagName);
        // HINT: Tag tag = tagsByKey.get(key);
        // HINT: if (tag == null) return Collections.emptyList();
        //
        // HINT: Set<String> itemIds = tagToItems.getOrDefault(tag.getTagId(), Collections.emptySet());
        // HINT: List<TaggableItem> result = new ArrayList<>();
        // HINT: for (String id : itemIds) {
        //     TaggableItem item = items.get(id);
        //     if (item != null) result.add(item);
        // }
        // HINT: return result;
        return null;
    }
    
    /**
     * Find items by tag, filtered by ItemType
     */
    public List<TaggableItem> findByTag(String workspaceId, String tagName, ItemType type) {
        // TODO: Implement
        // HINT: List<TaggableItem> all = findByTag(workspaceId, tagName);
        // HINT: List<TaggableItem> filtered = new ArrayList<>();
        // HINT: for (TaggableItem item : all) {
        //     if (item.getItemType() == type) filtered.add(item);
        // }
        // HINT: return filtered;
        return null;
    }
    
    // ===== TOP-K POPULAR TAGS =====
    
    /**
     * Get top K most popular tags in a workspace
     * 
     * IMPLEMENTATION HINTS:
     * 1. Filter tags by workspaceId
     * 2. Sort by usageCount descending
     * 3. Return top K
     */
    public List<Tag> getTopKTags(String workspaceId, int k) {
        // TODO: Implement
        // HINT: List<Tag> workspaceTags = new ArrayList<>();
        // HINT: for (Tag t : tagsByKey.values()) {
        //     if (t.getWorkspaceId().equals(workspaceId)) workspaceTags.add(t);
        // }
        // HINT: workspaceTags.sort((a, b) -> b.getUsageCount() - a.getUsageCount());
        // HINT: return workspaceTags.subList(0, Math.min(k, workspaceTags.size()));
        return null;
    }
    
    // ===== AUTOCOMPLETE =====
    
    /**
     * Suggest tags matching prefix (per workspace)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Filter tags by workspace AND name starts with prefix
     * 2. Sort by usageCount (popular first)
     * 3. Return top N suggestions
     */
    public List<Tag> autocomplete(String workspaceId, String prefix, int limit) {
        // TODO: Implement
        // HINT: String normalized = prefix.toLowerCase().trim();
        // HINT: List<Tag> matches = new ArrayList<>();
        // HINT: for (Tag t : tagsByKey.values()) {
        //     if (t.getWorkspaceId().equals(workspaceId) && t.getName().startsWith(normalized)) {
        //         matches.add(t);
        //     }
        // }
        // HINT: matches.sort((a, b) -> b.getUsageCount() - a.getUsageCount());
        // HINT: return matches.subList(0, Math.min(limit, matches.size()));
        return null;
    }
    
    // ===== QUERIES =====
    
    /** Get all tags on an item */
    public List<Tag> getItemTags(String itemId) {
        // TODO: Implement
        // HINT: Set<String> tagIds = itemToTags.getOrDefault(itemId, Collections.emptySet());
        // HINT: List<Tag> result = new ArrayList<>();
        // HINT: for (String tid : tagIds) {
        //     Tag t = tagsById.get(tid);
        //     if (t != null) result.add(t);
        // }
        // HINT: return result;
        return null;
    }
    
    public Tag getTag(String workspaceId, String name) { return tagsByKey.get(tagKey(workspaceId, name)); }
}

// ===== MAIN TEST CLASS =====

public class ContentTaggingSystem {
    public static void main(String[] args) {
        System.out.println("=== Atlassian Tagging System LLD ===\n");
        
        TaggingService service = new TaggingService();
        
        // Register items across products
        service.registerItem(new TaggableItem("JIRA-101", ItemType.JIRA_ISSUE, "Login page broken", "ws1"));
        service.registerItem(new TaggableItem("JIRA-102", ItemType.JIRA_ISSUE, "Add dark mode", "ws1"));
        service.registerItem(new TaggableItem("CONF-201", ItemType.CONFLUENCE_PAGE, "Q3 Roadmap", "ws1"));
        service.registerItem(new TaggableItem("CONF-202", ItemType.CONFLUENCE_PAGE, "Oncall Runbook", "ws1"));
        service.registerItem(new TaggableItem("BB-301", ItemType.BITBUCKET_PR, "Fix login bug", "ws1"));
        service.registerItem(new TaggableItem("JIRA-401", ItemType.JIRA_ISSUE, "Other workspace bug", "ws2"));
        
        // Test 1: Add tags (cross-product)
        System.out.println("=== Test 1: Add Tags ===");
        service.addTag("JIRA-101", "ws1", "Bug", "alice");
        service.addTag("JIRA-101", "ws1", "oncall", "alice");
        service.addTag("JIRA-102", "ws1", "feature", "bob");
        service.addTag("CONF-201", "ws1", "q3-roadmap", "alice");
        service.addTag("CONF-201", "ws1", "feature", "alice");
        service.addTag("CONF-202", "ws1", "oncall", "bob");
        service.addTag("BB-301", "ws1", "Bug", "charlie");
        service.addTag("JIRA-401", "ws2", "Bug", "diana");  // different workspace!
        System.out.println();
        
        // Test 2: Cross-product search (click on #bug → see JIRA + Bitbucket)
        System.out.println("=== Test 2: Cross-Product Search (#bug in ws1) ===");
        List<TaggableItem> bugItems = service.findByTag("ws1", "bug");
        System.out.println("✓ #bug items: " + (bugItems != null ? bugItems.size() : 0) + " (expect 2: JIRA-101 + BB-301)");
        if (bugItems != null) bugItems.forEach(i -> System.out.println("    " + i));
        System.out.println();
        
        // Test 3: Cross-product search filtered by type
        System.out.println("=== Test 3: Search by Type (#bug, JIRA only) ===");
        List<TaggableItem> bugJira = service.findByTag("ws1", "bug", ItemType.JIRA_ISSUE);
        System.out.println("✓ #bug JIRA: " + (bugJira != null ? bugJira.size() : 0) + " (expect 1)");
        System.out.println();
        
        // Test 4: Workspace scoping (ws1 #bug ≠ ws2 #bug)
        System.out.println("=== Test 4: Workspace Scoping ===");
        List<TaggableItem> ws1Bug = service.findByTag("ws1", "bug");
        List<TaggableItem> ws2Bug = service.findByTag("ws2", "bug");
        System.out.println("✓ ws1 #bug: " + (ws1Bug != null ? ws1Bug.size() : 0) + " (expect 2)");
        System.out.println("✓ ws2 #bug: " + (ws2Bug != null ? ws2Bug.size() : 0) + " (expect 1)");
        System.out.println("  Workspaces are isolated ✓");
        System.out.println();
        
        // Test 5: Top-K popular tags
        System.out.println("=== Test 5: Top-K Tags (ws1) ===");
        List<Tag> topTags = service.getTopKTags("ws1", 5);
        System.out.println("✓ Top tags:");
        if (topTags != null) topTags.forEach(t -> System.out.println("    " + t));
        System.out.println();
        
        // Test 6: Autocomplete
        System.out.println("=== Test 6: Autocomplete ===");
        List<Tag> suggestions = service.autocomplete("ws1", "on", 5);
        System.out.println("✓ Prefix 'on': " + (suggestions != null ? suggestions : "null") + " (expect #oncall)");
        suggestions = service.autocomplete("ws1", "b", 5);
        System.out.println("✓ Prefix 'b': " + (suggestions != null ? suggestions : "null") + " (expect #bug)");
        System.out.println();
        
        // Test 7: Tags on an item
        System.out.println("=== Test 7: Item Tags ===");
        List<Tag> itemTags = service.getItemTags("JIRA-101");
        System.out.println("✓ JIRA-101 tags: " + (itemTags != null ? itemTags : "null") + " (expect #bug, #oncall)");
        System.out.println();
        
        // Test 8: Remove tag
        System.out.println("=== Test 8: Remove Tag ===");
        service.removeTag("JIRA-101", "ws1", "oncall");
        List<Tag> afterRemove = service.getItemTags("JIRA-101");
        System.out.println("✓ After remove: " + (afterRemove != null ? afterRemove : "null") + " (expect #bug only)");
        
        // Verify oncall search updated
        List<TaggableItem> oncallItems = service.findByTag("ws1", "oncall");
        System.out.println("✓ #oncall items: " + (oncallItems != null ? oncallItems.size() : 0) + " (expect 1: CONF-202)");
        System.out.println();
        
        // Test 9: Duplicate tag (idempotent)
        System.out.println("=== Test 9: Duplicate Tag (idempotent) ===");
        service.addTag("JIRA-101", "ws1", "Bug", "alice"); // already tagged
        Tag bugTag = service.getTag("ws1", "bug");
        System.out.println("✓ #bug count after dup: " + (bugTag != null ? bugTag.getUsageCount() : 0) + " (should NOT increase)");
        
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION (aligned with Atlassian HLD):
 * ===================================================
 * 
 * 1. KEY ENTITIES:
 *    Tag: workspace-scoped, normalized name, usage count
 *    TagAssociation: tag ↔ item link with metadata (who, when)
 *    TaggableItem: JIRA issue, Confluence page, Bitbucket PR
 * 
 * 2. WORKSPACE SCOPING:
 *    Composite key: "workspaceId:tagName"
 *    #sprint-23 in Workspace A ≠ #sprint-23 in Workspace B
 * 
 * 3. INVERTED INDEX (cross-product search):
 *    tagId → {JIRA-101, CONF-201, BB-301} ← items across ALL products
 *    Click #bug → see JIRA issues + Confluence pages + PRs
 * 
 * 4. NORMALIZATION:
 *    "Bug" → "bug", " OnCall " → "oncall"
 *    Prevents duplicates: #bug vs #Bug vs #BUG
 *    Store displayName for original casing
 * 
 * 5. TOP-K DASHBOARD:
 *    Simple: sort by usageCount → O(n log n)
 *    Better: Redis Sorted Set per workspace → O(log n) update, O(K) top-K
 *    Best: Count-Min Sketch for approximate streaming top-K
 * 
 * 6. AUTOCOMPLETE:
 *    Simple: prefix filter + sort by popularity (used here)
 *    Better: Trie with frequency weights
 *    Best: Elasticsearch completion suggester
 * 
 * 7. ARCHITECTURE:
 *    Write path: API → Tag Service → DB (associations) + Update inverted index
 *    Read path:  API → Tag Service → Inverted index → Item Service (hydrate)
 *    Top-K: Redis ZREVRANGE tag_counts:ws1 0 K-1
 * 
 * 8. API:
 *    POST /items/{id}/tags              — add tag
 *    DELETE /items/{id}/tags/{tag}       — remove tag
 *    GET /tags/{name}/items?ws=ws1      — cross-product search
 *    GET /tags/{name}/items?ws=ws1&type=JIRA — filtered search
 *    GET /tags/top?ws=ws1&k=10          — top-K dashboard
 *    GET /tags/autocomplete?ws=ws1&q=on — autocomplete
 *    GET /items/{id}/tags               — tags on item
 */
