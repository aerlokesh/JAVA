import java.util.*;
import java.util.concurrent.*;

// ===== EXCEPTIONS =====

class FlagNotFoundException extends Exception {
    public FlagNotFoundException(String name) { super("Flag not found: " + name); }
}

class InvalidFlagConfigException extends Exception {
    public InvalidFlagConfigException(String msg) { super("Invalid config: " + msg); }
}

// ===== ENUMS =====

enum FlagStatus { ENABLED, DISABLED }

enum VariantType { CONTROL, TREATMENT }

// ===== INTERFACE (Strategy Pattern) =====

/**
 * Targeting rule — decides if a user should see the flag
 */
interface TargetingRule {
    boolean matches(String userId, Map<String, String> attributes);
}

// ===== TARGETING RULES =====

/**
 * Whitelist specific users by ID
 */
class UserListRule implements TargetingRule {
    private final Set<String> userIds;
    
    public UserListRule(String... userIds) {
        this.userIds = new HashSet<>(Arrays.asList(userIds));
    }
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Return true if userId is in the set
     */
    @Override
    public boolean matches(String userId, Map<String, String> attributes) {
        // HINT: return userIds.contains(userId);
        return userIds.contains(userId);
    }
}

/**
 * Percentage rollout — deterministic via hash
 * KEY: Same user always gets same result (no randomness)
 */
class PercentageRule implements TargetingRule {
    private final int percentage; // 0-100
    
    public PercentageRule(int percentage) {
        this.percentage = percentage;
    }
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. hash = Math.abs(userId.hashCode()) % 100
     * 2. return hash < percentage
     * 
     * Same userId → same hash → same result every time
     */
    @Override
    public boolean matches(String userId, Map<String, String> attributes) {
        // HINT: return (Math.abs(userId.hashCode()) % 100) < percentage;
        return (Math.abs(userId.hashCode()) % 100) < percentage;
    }
}

/**
 * Attribute rule — match by country, plan, etc.
 */
class AttributeRule implements TargetingRule {
    private final String key;
    private final Set<String> values;
    
    public AttributeRule(String key, String... values) {
        this.key = key;
        this.values = new HashSet<>(Arrays.asList(values));
    }
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Get attribute value from map
     * 2. Check if it's in allowed values
     */
    @Override
    public boolean matches(String userId, Map<String, String> attributes) {
        // HINT: String val = attributes.get(key);
        // HINT: return val != null && values.contains(val);
        String val = attributes.get(key);
        return val!=null && values.contains(val);
    }
}

// ===== DOMAIN CLASSES =====

/**
 * Feature Flag — on/off toggle with targeting
 */
class FeatureFlag {
    private final String name;
    private FlagStatus status;
    private final List<TargetingRule> rules;
    
    public FeatureFlag(String name) {
        this.name = name;
        this.status = FlagStatus.DISABLED;
        this.rules = new ArrayList<>();
    }
    
    public String getName() { return name; }
    public FlagStatus getStatus() { return status; }
    public void setStatus(FlagStatus s) { this.status = s; }
    public void addRule(TargetingRule rule) { rules.add(rule); }
    public List<TargetingRule> getRules() { return rules; }
    
    @Override
    public String toString() { return name + "[" + status + ", rules=" + rules.size() + "]"; }
}

/**
 * Variant in an AB experiment
 */
class Variant {
    private final String name;
    private final VariantType type;
    private final int percentage;  // all variants must sum to 100
    
    public Variant(String name, VariantType type, int percentage) {
        this.name = name;
        this.type = type;
        this.percentage = percentage;
    }
    
    public String getName() { return name; }
    public VariantType getType() { return type; }
    public int getPercentage() { return percentage; }
    
    @Override
    public String toString() { return name + "(" + percentage + "%)"; }
}

/**
 * AB Experiment
 */
class Experiment {
    private final String id;
    private final String name;
    private boolean running;
    private final List<Variant> variants;
    private final Map<String, String> userAssignments;  // userId → variantName (cached)
    
    public Experiment(String id, String name) {
        this.id = id;
        this.name = name;
        this.running = false;
        this.variants = new ArrayList<>();
        this.userAssignments = new ConcurrentHashMap<>();
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isRunning() { return running; }
    public void setRunning(boolean r) { this.running = r; }
    public List<Variant> getVariants() { return variants; }
    public void addVariant(Variant v) { variants.add(v); }
    
    public String getCachedVariant(String userId) { return userAssignments.get(userId); }
    public void cacheVariant(String userId, String variant) { userAssignments.put(userId, variant); }
    public int getUserCount() { return userAssignments.size(); }
    
    @Override
    public String toString() { return id + "[" + name + ", running=" + running + ", users=" + userAssignments.size() + "]"; }
}

// ===== SERVICE =====

/**
 * Feature Flag & AB Testing Service - Low Level Design (LLD)
 * 
 * PROBLEM: Design a system for:
 * 1. Feature flags — on/off toggles with targeting (user list, %, attributes)
 * 2. AB experiments — assign users to variants deterministically
 * 
 * KEY CONCEPTS:
 * - Deterministic: same user always gets same result (hash-based)
 * - Targeting: rules evaluated in order, first match wins
 * - Bucketing: hash(userId + experimentId) % 100 → variant
 * 
 * PATTERNS: Strategy (targeting rules)
 */
class FeatureFlagService {
    private final Map<String, FeatureFlag> flags;
    private final Map<String, Experiment> experiments;
    
    public FeatureFlagService() {
        this.flags = new HashMap<>();
        this.experiments = new HashMap<>();
    }
    
    // ===== FLAGS =====
    
    /**
     * IMPLEMENTATION HINTS:
     * 1. Check duplicate → throw
     * 2. Create flag, store in map, return it
     */
    public FeatureFlag createFlag(String name) throws InvalidFlagConfigException {
        // HINT: if (flags.containsKey(name)) throw new InvalidFlagConfigException("Flag exists: " + name);
        // HINT: FeatureFlag flag = new FeatureFlag(name);
        // HINT: flags.put(name, flag);
        // HINT: return flag;
        if(flags.containsKey(name)) throw new InvalidFlagConfigException("Flag exists"+name);
        FeatureFlag flag = new FeatureFlag(name);
        flags.put(name, flag);
        return flag;
    }
    
    /**
     * Evaluate flag for a user
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get flag → throw FlagNotFoundException if missing
     * 2. If DISABLED → return false
     * 3. If ENABLED + no rules → return true (on for everyone)
     * 4. If ENABLED + rules → check each rule, return true on first match
     * 5. No match → return false
     */
    public boolean isEnabled(String flagName, String userId, Map<String, String> attributes) 
            throws FlagNotFoundException {
        // HINT: FeatureFlag flag = flags.get(flagName);
        // HINT: if (flag == null) throw new FlagNotFoundException(flagName);
        // HINT: if (flag.getStatus() == FlagStatus.DISABLED) return false;
        //
        // HINT: // ENABLED
        // HINT: if (flag.getRules().isEmpty()) return true;  // no rules = on for all
        // HINT: for (TargetingRule rule : flag.getRules()) {
        //     if (rule.matches(userId, attributes)) return true;
        // }
        // HINT: return false;
        FeatureFlag flag = flags.get(flagName);
        if(flag==null) throw new FlagNotFoundException(flagName);
        if(flag.getStatus()==FlagStatus.DISABLED) return false;
        if(flag.getRules().isEmpty()) return true;
        for(TargetingRule rule:flag.getRules()){
            if(rule.matches(userId, attributes)) return true;
        }
        return false;
    }
    
    /** Convenience: evaluate with no attributes */
    public boolean isEnabled(String flagName, String userId) throws FlagNotFoundException {
        return isEnabled(flagName, userId, Collections.emptyMap());
    }
    
    // ===== AB EXPERIMENTS =====
    
    /**
     * Create experiment with variants
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate percentages sum to 100
     * 2. Create Experiment, add variants
     * 3. Store and return
     */
    public Experiment createExperiment(String name, List<Variant> variants) throws InvalidFlagConfigException {
        // HINT: int total = variants.stream().mapToInt(Variant::getPercentage).sum();
        // HINT: if (total != 100) throw new InvalidFlagConfigException("Percentages must sum to 100, got " + total);
        // HINT: String id = "EXP-" + UUID.randomUUID().toString().substring(0, 6);
        // HINT: Experiment exp = new Experiment(id, name);
        // HINT: variants.forEach(exp::addVariant);
        // HINT: experiments.put(id, exp);
        // HINT: return exp;
        int total=variants.stream().mapToInt(Variant::getPercentage).sum();
        if(total!=100) throw new InvalidFlagConfigException("Percentages must sum to "+100);
        String id="EXP"+UUID.randomUUID().toString().substring(0,6);
        Experiment experiment=new Experiment(id, name);
        variants.forEach(experiment::addVariant);
        experiments.put(id, experiment);
        return experiment;
    }
    
    /**
     * Get variant for user (deterministic bucketing)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get experiment → check it's running
     * 2. Check cache → return if already assigned
     * 3. hash = Math.abs((experimentId + userId).hashCode()) % 100
     * 4. Walk variants with cumulative %, find bucket
     * 5. Cache and return
     * 
     * KEY INTERVIEW POINT: hash(expId + userId) makes assignment
     *   deterministic AND independent across experiments
     */
    public String getVariant(String experimentId, String userId) {
        // HINT: Experiment exp = experiments.get(experimentId);
        // HINT: if (exp == null || !exp.isRunning()) return null;
        //
        // HINT: String cached = exp.getCachedVariant(userId);
        // HINT: if (cached != null) return cached;
        //
        // HINT: int hash = Math.abs((experimentId + ":" + userId).hashCode()) % 100;
        // HINT: int cumulative = 0;
        // HINT: for (Variant v : exp.getVariants()) {
        //     cumulative += v.getPercentage();
        //     if (hash < cumulative) {
        //         exp.cacheVariant(userId, v.getName());
        //         return v.getName();
        //     }
        // }
        // HINT: String last = exp.getVariants().get(exp.getVariants().size()-1).getName();
        // HINT: exp.cacheVariant(userId, last);
        // HINT: return last;
        Experiment exp=experiments.get(experimentId);
        if(exp==null || !exp.isRunning()) return null;
        String cached=exp.getCachedVariant(userId);
        if(cached!=null) return cached;
        int hash=Math.abs((experimentId+":"+userId).hashCode())%100;
        int cumulative=0;
        for(Variant v:exp.getVariants()){
            cumulative+=v.getPercentage();
            if(hash<cumulative){
                exp.cacheVariant(userId,v.getName());
                return v.getName();
            }
        }
        String last=exp.getVariants().get(exp.getVariants().size()-1).getName();
        exp.cacheVariant(userId, last);
        return last;
    }
    
    // ===== HELPERS =====
    
    public FeatureFlag getFlag(String name) { return flags.get(name); }
    public Experiment getExperiment(String id) { return experiments.get(id); }
}

// ===== MAIN TEST CLASS =====

public class FeatureFlagSystem {
    public static void main(String[] args) {
        System.out.println("=== Feature Flag & AB Testing LLD ===\n");
        
        FeatureFlagService service = new FeatureFlagService();
        
        // Test 1: Simple flag (enabled for all)
        System.out.println("=== Test 1: Simple Flag ===");
        try {
            FeatureFlag flag = service.createFlag("dark_mode");
            flag.setStatus(FlagStatus.ENABLED);
            System.out.println("✓ Alice: " + service.isEnabled("dark_mode", "alice") + " (expect true)");
            System.out.println("✓ Bob:   " + service.isEnabled("dark_mode", "bob") + " (expect true)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 2: Whitelist rule
        System.out.println("=== Test 2: User Whitelist ===");
        try {
            FeatureFlag flag = service.createFlag("new_checkout");
            flag.setStatus(FlagStatus.ENABLED);
            flag.addRule(new UserListRule("alice", "charlie"));
            System.out.println("✓ Alice:   " + service.isEnabled("new_checkout", "alice") + " (expect true)");
            System.out.println("✓ Bob:     " + service.isEnabled("new_checkout", "bob") + " (expect false)");
            System.out.println("✓ Charlie: " + service.isEnabled("new_checkout", "charlie") + " (expect true)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 3: Percentage rollout
        System.out.println("=== Test 3: 50% Rollout ===");
        try {
            FeatureFlag flag = service.createFlag("new_search");
            flag.setStatus(FlagStatus.ENABLED);
            flag.addRule(new PercentageRule(50));
            int on = 0;
            for (int i = 0; i < 100; i++)
                if (service.isEnabled("new_search", "user" + i)) on++;
            System.out.println("✓ " + on + "/100 users got feature (expect ~50)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 4: Attribute rule (country=US)
        System.out.println("=== Test 4: Attribute Rule ===");
        try {
            FeatureFlag flag = service.createFlag("us_promo");
            flag.setStatus(FlagStatus.ENABLED);
            flag.addRule(new AttributeRule("country", "US"));
            
            Map<String, String> usUser = new HashMap<>(); usUser.put("country", "US");
            Map<String, String> ukUser = new HashMap<>(); ukUser.put("country", "UK");
            
            System.out.println("✓ US user: " + service.isEnabled("us_promo", "alice", usUser) + " (expect true)");
            System.out.println("✓ UK user: " + service.isEnabled("us_promo", "bob", ukUser) + " (expect false)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 5: Disabled flag
        System.out.println("=== Test 5: Disabled Flag ===");
        try {
            service.createFlag("kill_switch");
            System.out.println("✓ " + service.isEnabled("kill_switch", "alice") + " (expect false)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 6: AB Experiment (50/50)
        System.out.println("=== Test 6: AB Experiment ===");
        try {
            Experiment exp = service.createExperiment("button_color", Arrays.asList(
                new Variant("control", VariantType.CONTROL, 50),
                new Variant("treatment", VariantType.TREATMENT, 50)
            ));
            exp.setRunning(true);
            
            String v1 = service.getVariant(exp.getId(), "alice");
            String v2 = service.getVariant(exp.getId(), "bob");
            String v1Again = service.getVariant(exp.getId(), "alice");
            System.out.println("✓ Alice: " + v1);
            System.out.println("✓ Bob:   " + v2);
            System.out.println("  Deterministic: " + (v1 != null && v1.equals(v1Again)));
            
            // Check distribution
            Map<String, Integer> counts = new HashMap<>();
            for (int i = 0; i < 100; i++) {
                String v = service.getVariant(exp.getId(), "user" + i);
                if (v != null) counts.merge(v, 1, Integer::sum);
            }
            System.out.println("  Distribution: " + counts);
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 7: A/B/C multi-variant
        System.out.println("=== Test 7: A/B/C Experiment ===");
        try {
            Experiment exp = service.createExperiment("pricing_page", Arrays.asList(
                new Variant("control", VariantType.CONTROL, 34),
                new Variant("variant_a", VariantType.TREATMENT, 33),
                new Variant("variant_b", VariantType.TREATMENT, 33)
            ));
            exp.setRunning(true);
            
            Map<String, Integer> counts = new HashMap<>();
            for (int i = 0; i < 99; i++) {
                String v = service.getVariant(exp.getId(), "u" + i);
                if (v != null) counts.merge(v, 1, Integer::sum);
            }
            System.out.println("✓ 3-way split: " + counts);
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 8: Invalid percentages
        System.out.println("=== Test 8: Exception - Bad Percentages ===");
        try {
            service.createExperiment("bad", Arrays.asList(
                new Variant("a", VariantType.CONTROL, 60),
                new Variant("b", VariantType.TREATMENT, 60)));
            System.out.println("✗ Should have thrown");
        } catch (InvalidFlagConfigException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        }
        System.out.println();
        
        // Test 9: Flag not found
        System.out.println("=== Test 9: Exception - Flag Not Found ===");
        try {
            service.isEnabled("nope", "alice");
            System.out.println("✗ Should have thrown");
        } catch (FlagNotFoundException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        }
        
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION:
 * =====================
 * 
 * 1. FEATURE FLAGS vs AB TESTS:
 *    Flag: on/off toggle (rollout, kill switch)
 *    AB:   multiple variants, compare metrics statistically
 * 
 * 2. DETERMINISTIC BUCKETING:
 *    hash(userId + experimentId) % 100 → variant
 *    Same user → always same variant (no randomness)
 *    Different experiments → independent assignments
 * 
 * 3. GRADUAL ROLLOUT:
 *    1% → 5% → 25% → 50% → 100%
 *    Users at 5% still included at 25% (hash-based)
 * 
 * 4. ARCHITECTURE:
 *    App → SDK (local cache) → Flag Service → DB
 *    SDK syncs flags every 30-60s for <1ms eval
 * 
 * 5. REAL-WORLD: LaunchDarkly, Optimizely, Amazon Weblab
 * 
 * 6. API:
 *    POST /flags, GET /flags/{name}/evaluate?userId=X
 *    POST /experiments, GET /experiments/{id}/variant?userId=X
 */
