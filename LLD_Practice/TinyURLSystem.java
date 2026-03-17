import java.util.*;

// ===== CUSTOM EXCEPTION CLASSES =====

/**
 * Exception thrown when an invalid URL is provided
 * WHEN TO THROW: 
 * - URL is null or empty
 * - URL doesn't match valid URL pattern
 * - URL contains malicious content
 */
class InvalidURLException extends Exception {
    public InvalidURLException(String message) {
        super(message);
    }
    
    public InvalidURLException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * Exception thrown when a short URL is not found in the system
 * WHEN TO THROW:
 * - expand() is called with non-existent short URL
 * - Short code doesn't exist in shortToLongMap
 */
class URLNotFoundException extends Exception {
    private String shortURL;
    
    public URLNotFoundException(String shortURL) {
        super("Short URL not found: " + shortURL);
        this.shortURL = shortURL;
    }
    
    public String getShortURL() {
        return shortURL;
    }
}

/**
 * Exception thrown when a custom alias is already taken
 * WHEN TO THROW:
 * - shortenWithAlias() is called with an already-used alias
 * - Alias exists in shortToLongMap
 */
class AliasAlreadyExistsException extends Exception {
    private String alias;
    
    public AliasAlreadyExistsException(String alias) {
        super("Alias already exists: " + alias);
        this.alias = alias;
    }
    
    public String getAlias() {
        return alias;
    }
}

/**
 * Exception thrown when a URL has expired
 * WHEN TO THROW:
 * - expand() is called on an expired URL
 * - Current time > expiration timestamp
 */
class URLExpiredException extends Exception {
    private String shortURL;
    private long expirationTime;
    
    public URLExpiredException(String shortURL, long expirationTime) {
        super("URL has expired: " + shortURL + " (expired at: " + new Date(expirationTime) + ")");
        this.shortURL = shortURL;
        this.expirationTime = expirationTime;
    }
    
    public String getShortURL() {
        return shortURL;
    }
    
    public long getExpirationTime() {
        return expirationTime;
    }
}

/**
 * Exception thrown when a custom alias has invalid format
 * WHEN TO THROW:
 * - Alias contains special characters (only alphanumeric allowed)
 * - Alias is too short (< 3 chars) or too long (> 20 chars)
 * - Alias contains spaces or reserved words
 */
class InvalidAliasException extends Exception {
    private String alias;
    
    public InvalidAliasException(String alias, String reason) {
        super("Invalid alias '" + alias + "': " + reason);
        this.alias = alias;
    }
    
    public String getAlias() {
        return alias;
    }
}

/**
 * Exception thrown when unable to generate unique short code
 * WHEN TO THROW:
 * - generateShortCode() exceeds max retry attempts
 * - System is approaching capacity (too many collisions)
 */
class ShortCodeGenerationException extends Exception {
    private int retryAttempts;
    
    public ShortCodeGenerationException(int retryAttempts) {
        super("Failed to generate unique short code after " + retryAttempts + " attempts. System may be at capacity.");
        this.retryAttempts = retryAttempts;
    }
    
    public int getRetryAttempts() {
        return retryAttempts;
    }
}

/**
 * TinyURL System - Low Level Design (LLD)
 * 
 * PROBLEM STATEMENT:
 * Design a URL shortening service like TinyURL that can:
 * 1. Convert a long URL into a short URL
 * 2. Retrieve the original long URL from a short URL
 * 3. Handle collisions
 * 4. Optional: Support custom aliases, expiration, and analytics
 * 
 * REQUIREMENTS:
 * - Functional: shorten(), expand(), custom aliases
 * - Non-Functional: Fast lookups, handle collisions, scalable
 * 
 * INTERVIEW HINTS:
 * - Discuss trade-offs: Hash vs Base62 encoding vs Random generation
 * - Talk about distributed systems (mention consistent hashing, distributed cache)
 * - Mention database choice (NoSQL like Redis/DynamoDB for fast lookups)
 * - Discuss collision handling strategies
 * - Talk about URL validation and security (prevent malicious URLs)
 */
public class TinyURLSystem {
    
    // HINT 1: Choose appropriate data structures
    // Consider: HashMap for quick lookups (O(1) average case)
    // What are the key-value pairs? shortURL -> longURL
    private Map<String, String> shortToLongMap;
    
    // HINT 2: For reverse lookup (optional but useful for avoiding duplicates)
    // What if same long URL is shortened multiple times?
    private Map<String, String> longToShortMap;
    
    // HINT 3: For tracking URL creation time (useful for expiration feature)
    private Map<String, Long> expirationMap;
    
    // HINT 4: Constants for configuration
    // What should be the base URL? e.g., "http://tiny.url/"
    // What should be the length of short code? e.g., 6-8 characters
    private static final String BASE_URL = "http://tiny.url/";
    private static final int SHORT_CODE_LENGTH = 6;
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int MAX_RETRIES = 10;
    private static final Long DEFAULT_EXPIRATION = 10000000000L;

    
    // HINT 5: For generating unique codes
    private Random random;
    
    /**
     * Constructor - Initialize your data structures
     * HINT: What data structures need initialization?
     */
    public TinyURLSystem() {
        // TODO: Initialize maps and random generator
        shortToLongMap = new HashMap<>();
        longToShortMap = new HashMap<>();
        expirationMap = new HashMap<>();
        random = new Random();
    }
    
    /**
     * Shorten a long URL to a short URL
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate the input URL (not null, not empty) - throw InvalidURLException if invalid
     * 2. Check if this long URL was already shortened (use longToShortMap)
     * 3. Generate a unique short code - may throw ShortCodeGenerationException
     * 4. Store the mapping in both maps
     * 5. Return the complete short URL (BASE_URL + shortCode)
     * 
     * INTERVIEW DISCUSSION POINTS:
     * - What if shortCode already exists? (Collision handling)
     * - How to generate unique codes? (Random, Hash, Counter, Base62)
     * - Should same longURL return same shortURL or create new one?
     * 
     * @param longURL The original long URL to be shortened
     * @return The shortened URL
     * @throws InvalidURLException if the URL is null, empty, or malformed
     * @throws ShortCodeGenerationException if unable to generate unique code after max retries
     */
    public String shorten(String longURL) throws InvalidURLException, ShortCodeGenerationException {
        // HINT: Check if longURL is null or empty, throw new InvalidURLException("...")
        // HINT: Call generateShortCode() which may throw ShortCodeGenerationException
        if(longURL == null || longURL.isEmpty()) throw new InvalidURLException(longURL);
        if(longToShortMap.containsKey(longURL)) return BASE_URL + longToShortMap.get(longURL);
        String shortUrl;
        try{
            shortUrl = generateShortCode();
            longToShortMap.put(longURL, shortUrl);
            shortToLongMap.put(shortUrl, longURL);
        }catch(ShortCodeGenerationException shortCodeGenerationException){
            throw shortCodeGenerationException;
        }
        return BASE_URL+shortUrl;
    }
    
    /**
     * Shorten with custom alias (BONUS feature)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate custom alias (alphanumeric, length constraints) - throw InvalidAliasException
     * 2. Check if alias is already taken - throw AliasAlreadyExistsException
     * 3. Validate longURL - throw InvalidURLException
     * 4. If available, use the custom alias instead of generated code
     * 
     * @param longURL The original URL
     * @param customAlias User-provided custom short code
     * @return The shortened URL with custom alias
     * @throws InvalidURLException if the URL is invalid
     * @throws InvalidAliasException if alias format is invalid (special chars, wrong length)
     * @throws AliasAlreadyExistsException if alias is already taken
     */
    public String shortenWithAlias(String longURL, String customAlias) 
            throws InvalidURLException, InvalidAliasException, AliasAlreadyExistsException {
        // HINT: Validate alias using regex: customAlias.matches("[a-zA-Z0-9]+")
        // HINT: Check length: customAlias.length() >= 3 && customAlias.length() <= 20
        // HINT: Check if exists: if (shortToLongMap.containsKey(customAlias)) throw new AliasAlreadyExistsException(...)
        if(longURL == null || longURL.isEmpty()) throw new InvalidURLException("Long URL is null or empty");
        if(customAlias == null || !customAlias.matches("[a-zA-Z0-9]+")) throw new InvalidAliasException(customAlias, "Alias must be alphanumeric" );
        if(customAlias.length() <3 || customAlias.length()>20) throw new InvalidAliasException(customAlias, "Alias length must be between 3 and 20");
        if(shortToLongMap.containsKey(customAlias)) throw new AliasAlreadyExistsException(customAlias);
        shortToLongMap.put(customAlias, longURL);
        longToShortMap.put(longURL, customAlias);
        return BASE_URL+customAlias;
    }
    
    /**
     * Expand/Retrieve the original long URL from short URL
     * 
     * IMPLEMENTATION HINTS:
     * 1. Extract the short code from the full short URL
     * 2. Look up in shortToLongMap - throw URLNotFoundException if not found
     * 3. Check if URL has expired - throw URLExpiredException if expired
     * 4. Return the long URL
     * 
     * INTERVIEW DISCUSSION POINTS:
     * - Better to throw exception than return null (fail-fast principle)
     * - Caller can handle different error cases differently
     * 
     * @param shortURL The shortened URL
     * @return The original long URL
     * @throws URLNotFoundException if short URL doesn't exist in system
     * @throws URLExpiredException if URL has expired
     */
    public String expand(String shortURL) throws URLNotFoundException, URLExpiredException {
        // HINT: Extract code, check if exists: if (!shortToLongMap.containsKey(code)) throw new URLNotFoundException(...)
        // HINT: Check expiration: if (isExpired(shortURL)) throw new URLExpiredException(...)
        String shortCode = extractShortCode(shortURL);
        if(!shortToLongMap.containsKey(shortCode)) throw new URLNotFoundException(shortURL);
        if(isExpired(shortCode)) {
            Long expirationTime = expirationMap.get(shortCode);
            throw new URLExpiredException(shortURL, expirationTime);
        }
        return shortToLongMap.get(shortCode);
    }
    
    /**
     * Generate a unique short code
     * 
     * IMPLEMENTATION HINTS:
     * 1. Generate random string of length SHORT_CODE_LENGTH using CHARACTERS
     * 2. Check if code already exists in shortToLongMap (collision detection)
     * 3. If collision, regenerate (loop until unique code found)
     * 4. Set max retry limit (e.g., 10 attempts) - throw exception if exceeded
     * 
     * ALTERNATIVE APPROACHES TO DISCUSS IN INTERVIEW:
     * - Base62 encoding of counter/timestamp
     * - MD5/SHA hash + truncate (higher collision probability)
     * - Distributed ID generation (Snowflake algorithm)
     * 
     * @return A unique short code string
     * @throws ShortCodeGenerationException if unable to generate unique code after max retries
     */
    private String generateShortCode() throws ShortCodeGenerationException {
        // HINT: Set MAX_RETRIES = 10
        // HINT: Use loop with counter, throw exception if counter >= MAX_RETRIES
        // HINT: StringBuilder code = new StringBuilder();
        // HINT: for (int i = 0; i < SHORT_CODE_LENGTH; i++) code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));

        for(int attempt = 0; attempt < MAX_RETRIES; attempt++){
            StringBuilder code = new StringBuilder();
            for(int i=0;i<SHORT_CODE_LENGTH;i++){
                code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
            }
            String shortCode = code.toString();
            if(!shortToLongMap.containsKey(shortCode)){
                return shortCode;
            }
        }
        throw new ShortCodeGenerationException(MAX_RETRIES);
    }
    
    /**
     * BONUS: Set expiration time for a URL (in hours)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate that shortURL exists - throw URLNotFoundException if not
     * 2. Calculate expiration timestamp (current time + hours)
     * 3. Store in expirationMap: expirationMap.put(shortCode, System.currentTimeMillis() + hours * 3600000L)
     * 
     * @param shortURL The short URL
     * @param hours Number of hours until expiration
     * @throws URLNotFoundException if short URL doesn't exist
     */
    public void setExpiration(String shortURL, int hours) throws URLNotFoundException {
        // HINT: Extract code and check: if (!shortToLongMap.containsKey(code)) throw new URLNotFoundException(...)
        // HINT: long expirationTime = System.currentTimeMillis() + (hours * 3600000L);
        String shortCode = extractShortCode(shortURL);
        if(!shortToLongMap.containsKey(shortCode)) throw new URLNotFoundException(shortURL);
        long expirationTime = System.currentTimeMillis() + (hours * 3600000L);
        expirationMap.put(shortCode, expirationTime);
    }
    
    /**
     * BONUS: Check if a URL has expired
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get expiration timestamp from expirationMap
     * 2. Compare with current time
     * 3. Return true if expired
     * 
     * @param shortURL The short URL to check
     * @return true if expired, false otherwise
     */
    private boolean isExpired(String shortURL) {
        Long expirationTime = expirationMap.get(shortURL);
        if(expirationTime == null) return false;
        return expirationTime < System.currentTimeMillis();
    }
    
    /**
     * BONUS: Get analytics - total URLs shortened
     * 
     * @return Total number of active short URLs
     */
    public int getTotalURLs() {
        return shortToLongMap.size();
    }
    
    /**
     * Extract short code from full short URL
     * 
     * IMPLEMENTATION HINTS:
     * 1. Remove BASE_URL prefix from shortURL
     * 2. Return the remaining code
     * Example: "http://tiny.url/abc123" -> "abc123"
     * 
     * @param shortURL The full short URL
     * @return The short code extracted from URL
     */
    private String extractShortCode(String shortURL) {
        if(shortURL == null ||!shortURL.startsWith(BASE_URL)){
            throw new IllegalArgumentException("Invalid short URL: " + shortURL);
        }
        return shortURL.substring(BASE_URL.length());
    }
    
    /**
     * BONUS: Delete a short URL mapping
     * 
     * IMPLEMENTATION HINTS:
     * 1. Remove from both maps
     * 2. Remove from expiration map if exists
     * 
     * @param shortURL The short URL to delete
     * @return true if deleted, false if not found
     */
    public boolean delete(String shortURL) {
        if(!shortToLongMap.containsKey(shortURL)) return false;
        shortToLongMap.remove(shortURL);
        String longUrl = shortToLongMap.get(shortURL);
        if(longUrl!=null){
            longToShortMap.remove(shortURL);
        }
        expirationMap.remove(shortURL);
        return false;
    }
    
    // ===== TEST DRIVER =====
    public static void main(String[] args) {
        TinyURLSystem system = new TinyURLSystem();
        
        // Test Case 1: Basic shortening and expansion
        System.out.println("=== Test Case 1: Basic Functionality ===");
        try {
            String longURL1 = "https://www.amazon.com/dp/B08N5WRWNW";
            String shortURL1 = system.shorten(longURL1);
            System.out.println("Long URL: " + longURL1);
            System.out.println("Short URL: " + shortURL1);
            System.out.println("Expanded: " + system.expand(shortURL1));
            System.out.println("Match: " + longURL1.equals(system.expand(shortURL1)));
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 2: Multiple URLs
        System.out.println("=== Test Case 2: Multiple URLs ===");
        try {
            String longURL2 = "https://leetcode.com/problems/design-tinyurl";
            String shortURL2 = system.shorten(longURL2);
            System.out.println("Short URL 2: " + shortURL2);
            System.out.println("Expanded 2: " + system.expand(shortURL2));
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 3: Same URL shortened twice (should return same or different?)
        System.out.println("=== Test Case 3: Same URL Twice ===");
        try {
            String longURL1 = "https://www.amazon.com/dp/B08N5WRWNW";
            String shortURL1 = system.shorten(longURL1);
            String shortURL3 = system.shorten(longURL1);
            System.out.println("First short: " + shortURL1);
            System.out.println("Second short: " + shortURL3);
            System.out.println("Are they same? " + shortURL1.equals(shortURL3));
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 4: Custom alias (BONUS)
        System.out.println("=== Test Case 4: Custom Alias ===");
        try {
            String longURL4 = "https://github.com/amazon";
            String customShort = system.shortenWithAlias(longURL4, "amazon");
            System.out.println("Custom short URL: " + customShort);
            System.out.println("Expanded: " + system.expand(customShort));
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 5: Invalid/Non-existent URL
        System.out.println("=== Test Case 5: Non-existent URL ===");
        try {
            String result = system.expand("http://tiny.url/invalid");
            System.out.println("Non-existent URL result: " + result);
        } catch (Exception e) {
            System.out.println("Exception (expected): " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 6: Analytics
        System.out.println("=== Test Case 6: Analytics ===");
        System.out.println("Total URLs: " + system.getTotalURLs());
        System.out.println();
        
        // Test Case 7: Expiration (BONUS)
        System.out.println("=== Test Case 7: Expiration (Bonus) ===");
        try {
            String longURL5 = "https://temporary-link.com/resource";
            String shortURL5 = system.shorten(longURL5);
            system.setExpiration(shortURL5, 1); // 1 hour expiration
            System.out.println("Created URL with 1 hour expiration: " + shortURL5);
            System.out.println("Can expand now: " + system.expand(shortURL5));
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
        System.out.println();
        
        // ===== EXCEPTION HANDLING TEST CASES =====
        System.out.println("=== Test Case 8: Exception - Invalid URL ===");
        try {
            system.shorten(null);
            System.out.println("ERROR: Should have thrown InvalidURLException!");
        } catch (InvalidURLException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception type: " + e.getClass().getName());
        }
        System.out.println();
        
        System.out.println("=== Test Case 9: Exception - URL Not Found ===");
        try {
            system.expand("http://tiny.url/doesnotexist");
            System.out.println("ERROR: Should have thrown URLNotFoundException!");
        } catch (URLNotFoundException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
            System.out.println("  Short URL attempted: " + e.getShortURL());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception type: " + e.getClass().getName());
        }
        System.out.println();
        
        System.out.println("=== Test Case 10: Exception - Alias Already Exists ===");
        try {
            system.shortenWithAlias("https://example1.com", "test123");
            system.shortenWithAlias("https://example2.com", "test123"); // Duplicate alias
            System.out.println("ERROR: Should have thrown AliasAlreadyExistsException!");
        } catch (AliasAlreadyExistsException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
            System.out.println("  Conflicting alias: " + e.getAlias());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception type: " + e.getClass().getName());
        }
        System.out.println();
        
        System.out.println("=== Test Case 11: Exception - Invalid Alias Format ===");
        try {
            system.shortenWithAlias("https://example.com", "ab"); // Too short
            System.out.println("ERROR: Should have thrown InvalidAliasException!");
        } catch (InvalidAliasException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
            System.out.println("  Invalid alias: " + e.getAlias());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception type: " + e.getClass().getName());
        }
        System.out.println();
        
        System.out.println("=== Test Case 12: Exception - Special Characters in Alias ===");
        try {
            system.shortenWithAlias("https://example.com", "test@123"); // Special char
            System.out.println("ERROR: Should have thrown InvalidAliasException!");
        } catch (InvalidAliasException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception type: " + e.getClass().getName());
        }
        System.out.println();
        
        System.out.println("=== Test Case 13: Exception - URL Expiration ===");
        try {
            String tempURL = "https://short-lived.com/resource";
            String tempShort = system.shorten(tempURL);
            // Set expiration to -1 hour (already expired for testing)
            system.setExpiration(tempShort, -1);
            system.expand(tempShort); // Should throw URLExpiredException
            System.out.println("ERROR: Should have thrown URLExpiredException!");
        } catch (URLExpiredException e) {
            System.out.println("✓ Caught expected exception: " + e.getMessage());
            System.out.println("  Expired URL: " + e.getShortURL());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception type: " + e.getClass().getName());
        }
        System.out.println();
        
        System.out.println("=== All Exception Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION TOPICS:
 * ============================
 * 
 * 1. SCALABILITY:
 *    - How to handle millions of requests per second?
 *    - Database sharding strategies (range-based, hash-based)
 *    - Caching layer (Redis, Memcached)
 *    - Load balancing and horizontal scaling
 * 
 * 2. SHORT CODE GENERATION STRATEGIES:
 *    a) Random Generation (used here)
 *       - Pros: Simple, distributed-friendly
 *       - Cons: Collision probability increases over time
 *    
 *    b) Counter-based + Base62 Encoding
 *       - Pros: No collisions, predictable
 *       - Cons: Needs distributed counter (Zookeeper, Redis)
 *    
 *    c) Hash-based (MD5/SHA)
 *       - Pros: Deterministic
 *       - Cons: Collision handling needed, longer codes
 * 
 * 3. DATABASE CHOICE:
 *    - NoSQL (DynamoDB, MongoDB, Cassandra) for high throughput
 *    - Redis for caching hot URLs
 *    - SQL (PostgreSQL) for transactional consistency
 * 
 * 4. SYSTEM DESIGN COMPONENTS:
 *    - API Gateway / Load Balancer
 *    - Application Servers (REST API)
 *    - Cache Layer (Redis)
 *    - Database (NoSQL for URL mappings)
 *    - Analytics Service (track clicks)
 *    - Rate Limiting (prevent abuse)
 * 
 * 5. SECURITY CONSIDERATIONS:
 *    - Validate URLs (prevent XSS, malicious redirects)
 *    - Rate limiting per user/IP
 *    - CAPTCHA for anonymous users
 *    - Blacklist malicious domains
 * 
 * 6. ADVANCED FEATURES:
 *    - Custom aliases
 *    - URL expiration
 *    - Analytics (click tracking, geographic data)
 *    - QR code generation
 *    - User accounts and URL management
 *    - A/B testing support
 * 
 * 7. CAPACITY ESTIMATION:
 *    - Read:Write ratio (typically 100:1)
 *    - Storage: 6 chars * 62^6 = 56 billion unique URLs
 *    - Bandwidth calculation
 *    - Storage requirements
 * 
 * 8. API DESIGN:
 *    POST /shorten       - Create short URL
 *    GET  /{shortCode}   - Redirect to long URL
 *    POST /custom        - Create with custom alias
 *    DELETE /{shortCode} - Delete URL
 *    GET  /stats/{shortCode} - Get analytics
 */
