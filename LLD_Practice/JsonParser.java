import java.util.*;

// ===== CUSTOM EXCEPTIONS =====

/**
 * Base exception for JSON parsing errors
 */
class JsonParseException extends Exception {
    private int position;
    
    public JsonParseException(String message, int position) {
        super(message + " at position " + position);
        this.position = position;
    }
    
    public JsonParseException(String message) {
        super(message);
    }
    
    public int getPosition() { return position; }
}

// ===== TOKEN TYPES =====

/**
 * Tokens produced by the lexer
 * 
 * INTERVIEW DISCUSSION:
 * - Lexer breaks input into tokens (lexical analysis)
 * - Parser consumes tokens to build structure (syntax analysis)
 * - Two-phase approach: separation of concerns
 */
enum TokenType {
    LEFT_BRACE,      // {
    RIGHT_BRACE,     // }
    LEFT_BRACKET,    // [
    RIGHT_BRACKET,   // ]
    COLON,           // :
    COMMA,           // ,
    STRING,          // "..."
    NUMBER,          // 123, -45.67, 1.2e-10
    TRUE,            // true
    FALSE,           // false
    NULL,            // null
    EOF              // End of input
}

/**
 * Represents a lexical token
 */
class Token {
    TokenType type;
    String value;
    int position;
    
    public Token(TokenType type, String value, int position) {
        this.type = type;
        this.value = value;
        this.position = position;
    }
    
    @Override
    public String toString() {
        return type + (value != null ? "(" + value + ")" : "") + "@" + position;
    }
}

// ===== JSON VALUE TYPES =====

/**
 * Base class for all JSON values
 * 
 * INTERVIEW DISCUSSION:
 * - Composite Pattern: JsonValue is base, JsonObject/JsonArray are composites
 * - Visitor Pattern: Could use for traversal/transformation
 */
abstract class JsonValue {
    public abstract String toJsonString();
    public abstract String toPrettyString(int indent);
    
    protected String getIndent(int level) {
        return "  ".repeat(level);
    }
    
    public boolean isObject() { return this instanceof JsonObject; }
    public boolean isArray() { return this instanceof JsonArray; }
    public boolean isString() { return this instanceof JsonString; }
    public boolean isNumber() { return this instanceof JsonNumber; }
    public boolean isBoolean() { return this instanceof JsonBoolean; }
    public boolean isNull() { return this instanceof JsonNull; }
    
    public JsonObject asObject() { return (JsonObject) this; }
    public JsonArray asArray() { return (JsonArray) this; }
    public String asString() { return ((JsonString) this).value; }
    public double asNumber() { return ((JsonNumber) this).value; }
    public boolean asBoolean() { return ((JsonBoolean) this).value; }
}

/**
 * JSON Object: { "key": value, ... }
 */
class JsonObject extends JsonValue {
    private Map<String, JsonValue> members;
    
    public JsonObject() {
        this.members = new LinkedHashMap<>();  // Preserve insertion order
    }
    
    public void put(String key, JsonValue value) {
        members.put(key, value);
    }
    
    public JsonValue get(String key) {
        return members.get(key);
    }
    
    public boolean has(String key) {
        return members.containsKey(key);
    }
    
    public Set<String> keys() {
        return members.keySet();
    }
    
    public int size() {
        return members.size();
    }
    
    @Override
    public String toJsonString() {
        if (members.isEmpty()) return "{}";
        
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, JsonValue> entry : members.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            sb.append(entry.getValue().toJsonString());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public String toPrettyString(int indent) {
        if (members.isEmpty()) return "{}";
        
        StringBuilder sb = new StringBuilder("{\n");
        boolean first = true;
        for (Map.Entry<String, JsonValue> entry : members.entrySet()) {
            if (!first) sb.append(",\n");
            sb.append(getIndent(indent + 1));
            sb.append("\"").append(entry.getKey()).append("\": ");
            sb.append(entry.getValue().toPrettyString(indent + 1));
            first = false;
        }
        sb.append("\n").append(getIndent(indent)).append("}");
        return sb.toString();
    }
}

/**
 * JSON Array: [ value1, value2, ... ]
 */
class JsonArray extends JsonValue {
    private List<JsonValue> elements;
    
    public JsonArray() {
        this.elements = new ArrayList<>();
    }
    
    public void add(JsonValue value) {
        elements.add(value);
    }
    
    public JsonValue get(int index) {
        return elements.get(index);
    }
    
    public int size() {
        return elements.size();
    }
    
    @Override
    public String toJsonString() {
        if (elements.isEmpty()) return "[]";
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(elements.get(i).toJsonString());
        }
        sb.append("]");
        return sb.toString();
    }
    
    @Override
    public String toPrettyString(int indent) {
        if (elements.isEmpty()) return "[]";
        
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) sb.append(",\n");
            sb.append(getIndent(indent + 1));
            sb.append(elements.get(i).toPrettyString(indent + 1));
        }
        sb.append("\n").append(getIndent(indent)).append("]");
        return sb.toString();
    }
}

/**
 * JSON String: "text"
 */
class JsonString extends JsonValue {
    String value;
    
    public JsonString(String value) {
        this.value = value;
    }
    
    @Override
    public String toJsonString() {
        return "\"" + escapeString(value) + "\"";
    }
    
    @Override
    public String toPrettyString(int indent) {
        return toJsonString();
    }
    
    private String escapeString(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}

/**
 * JSON Number: 123, -45.67, 1.2e-10
 */
class JsonNumber extends JsonValue {
    double value;
    
    public JsonNumber(double value) {
        this.value = value;
    }
    
    @Override
    public String toJsonString() {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
    
    @Override
    public String toPrettyString(int indent) {
        return toJsonString();
    }
}

/**
 * JSON Boolean: true, false
 */
class JsonBoolean extends JsonValue {
    boolean value;
    
    public JsonBoolean(boolean value) {
        this.value = value;
    }
    
    @Override
    public String toJsonString() {
        return String.valueOf(value);
    }
    
    @Override
    public String toPrettyString(int indent) {
        return toJsonString();
    }
}

/**
 * JSON Null: null
 */
class JsonNull extends JsonValue {
    @Override
    public String toJsonString() {
        return "null";
    }
    
    @Override
    public String toPrettyString(int indent) {
        return "null";
    }
}

// ===== LEXER (TOKENIZER) =====

/**
 * Lexer - Converts input string to stream of tokens
 * 
 * RESPONSIBILITIES:
 * - Scan input character by character
 * - Recognize JSON tokens: {, }, [, ], :, comma, string, number, boolean, null
 * - Skip whitespace
 * - Handle escape sequences in strings
 * 
 * INTERVIEW DISCUSSION:
 * - Finite State Machine for tokenization
 * - Look-ahead for multi-character tokens
 * - Error recovery strategies
 */
class Lexer {
    private String input;
    private int position;
    private int length;
    
    public Lexer(String input) {
        this.input = input;
        this.position = 0;
        this.length = input.length();
    }
    
    /**
     * Get next token from input
     * 
     * IMPLEMENTATION HINTS:
     * 1. Skip whitespace
     * 2. Check for EOF
     * 3. Match single-character tokens: { } [ ] : ,
     * 4. Match string (starts with ")
     * 5. Match number (starts with digit or -)
     * 6. Match keywords: true, false, null
     * 
     * @return Next token
     * @throws JsonParseException if invalid syntax
     */
    public Token nextToken() throws JsonParseException {
        skipWhitespace();
        
        if (position >= length) {
            return new Token(TokenType.EOF, null, position);
        }
        
        char ch = input.charAt(position);
        
        // Single-character tokens
        switch (ch) {
            case '{':
                return new Token(TokenType.LEFT_BRACE, "{", position++);
            case '}':
                return new Token(TokenType.RIGHT_BRACE, "}", position++);
            case '[':
                return new Token(TokenType.LEFT_BRACKET, "[", position++);
            case ']':
                return new Token(TokenType.RIGHT_BRACKET, "]", position++);
            case ':':
                return new Token(TokenType.COLON, ":", position++);
            case ',':
                return new Token(TokenType.COMMA, ",", position++);
            case '"':
                return readString();
            case '-':
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                return readNumber();
        }
        
        // Keywords: true, false, null
        if (startsWith("true")) {
            int start = position;
            position += 4;
            return new Token(TokenType.TRUE, "true", start);
        }
        if (startsWith("false")) {
            int start = position;
            position += 5;
            return new Token(TokenType.FALSE, "false", start);
        }
        if (startsWith("null")) {
            int start = position;
            position += 4;
            return new Token(TokenType.NULL, "null", start);
        }
        
        throw new JsonParseException("Unexpected character: " + ch, position);
    }
    
    /**
     * Read a JSON string token
     * Handles escape sequences: \", \\, \n, \r, \t, unicode
     */
    private Token readString() throws JsonParseException {
        int start = position;
        position++;  // Skip opening "
        
        StringBuilder sb = new StringBuilder();
        
        while (position < length) {
            char ch = input.charAt(position);
            
            if (ch == '"') {
                position++;  // Skip closing "
                return new Token(TokenType.STRING, sb.toString(), start);
            }
            
            if (ch == '\\') {
                position++;
                if (position >= length) {
                    throw new JsonParseException("Unterminated string escape", position);
                }
                
                char escaped = input.charAt(position);
                switch (escaped) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        // Unicode escape sequence (4 hex digits)
                        if (position + 4 >= length) {
                            throw new JsonParseException("Invalid unicode escape", position);
                        }
                        String hex = input.substring(position + 1, position + 5);
                        sb.append((char) Integer.parseInt(hex, 16));
                        position += 4;
                        break;
                    default:
                        throw new JsonParseException("Invalid escape sequence: \\" + escaped, position);
                }
            } else {
                sb.append(ch);
            }
            
            position++;
        }
        
        throw new JsonParseException("Unterminated string", start);
    }
    
    /**
     * Read a JSON number token
     * Supports: integers, decimals, scientific notation
     * Format: -?(0|[1-9][0-9]*)(\.[0-9]+)?([eE][+-]?[0-9]+)?
     */
    private Token readNumber() throws JsonParseException {
        int start = position;
        
        // Optional minus
        if (peek() == '-') position++;
        
        // Integer part
        if (peek() == '0') {
            position++;
        } else if (peek() >= '1' && peek() <= '9') {
            while (position < length && peek() >= '0' && peek() <= '9') {
                position++;
            }
        } else {
            throw new JsonParseException("Invalid number", position);
        }
        
        // Optional decimal part
        if (position < length && peek() == '.') {
            position++;
            if (position >= length || peek() < '0' || peek() > '9') {
                throw new JsonParseException("Invalid number: decimal must have digits", position);
            }
            while (position < length && peek() >= '0' && peek() <= '9') {
                position++;
            }
        }
        
        // Optional exponent
        if (position < length && (peek() == 'e' || peek() == 'E')) {
            position++;
            if (position < length && (peek() == '+' || peek() == '-')) {
                position++;
            }
            if (position >= length || peek() < '0' || peek() > '9') {
                throw new JsonParseException("Invalid number: exponent must have digits", position);
            }
            while (position < length && peek() >= '0' && peek() <= '9') {
                position++;
            }
        }
        
        String numStr = input.substring(start, position);
        return new Token(TokenType.NUMBER, numStr, start);
    }
    
    private void skipWhitespace() {
        while (position < length) {
            char ch = input.charAt(position);
            if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
                position++;
            } else {
                break;
            }
        }
    }
    
    private char peek() {
        return position < length ? input.charAt(position) : '\0';
    }
    
    private boolean startsWith(String str) {
        return position + str.length() <= length && 
               input.substring(position, position + str.length()).equals(str);
    }
}

// ===== PARSER =====

/**
 * Parser - Converts token stream to JSON value tree
 * 
 * GRAMMAR (Recursive Descent):
 *   value    → object | array | string | number | true | false | null
 *   object   → '{' (string ':' value (',' string ':' value)*)? '}'
 *   array    → '[' (value (',' value)*)? ']'
 * 
 * IMPLEMENTATION STRATEGY:
 * - Recursive descent parser
 * - One method per grammar rule
 * - Look-ahead with current token
 * 
 * INTERVIEW DISCUSSION:
 * - Why recursive descent? (Simple, mirrors grammar structure)
 * - Alternative: Table-driven (LR, LALR parsers)
 * - Error recovery: panic mode, synchronization points
 */
class Parser {
    private Lexer lexer;
    private Token currentToken;
    
    public Parser(String input) throws JsonParseException {
        this.lexer = new Lexer(input);
        this.currentToken = lexer.nextToken();
    }
    
    /**
     * Parse JSON input
     * 
     * @return Parsed JsonValue
     * @throws JsonParseException if syntax error
     */
    public JsonValue parse() throws JsonParseException {
        JsonValue value = parseValue();
        
        // Ensure no trailing content
        if (currentToken.type != TokenType.EOF) {
            throw new JsonParseException("Unexpected token after JSON value: " + currentToken.type, 
                                        currentToken.position);
        }
        
        return value;
    }
    
    /**
     * Parse any JSON value
     */
    private JsonValue parseValue() throws JsonParseException {
        switch (currentToken.type) {
            case LEFT_BRACE:
                return parseObject();
            case LEFT_BRACKET:
                return parseArray();
            case STRING:
                return parseString();
            case NUMBER:
                return parseNumber();
            case TRUE:
                return parseTrue();
            case FALSE:
                return parseFalse();
            case NULL:
                return parseNull();
            default:
                throw new JsonParseException("Expected value, got: " + currentToken.type, 
                                            currentToken.position);
        }
    }
    
    /**
     * Parse JSON object: { "key": value, ... }
     */
    private JsonObject parseObject() throws JsonParseException {
        expect(TokenType.LEFT_BRACE);
        JsonObject obj = new JsonObject();
        
        // Empty object
        if (currentToken.type == TokenType.RIGHT_BRACE) {
            advance();
            return obj;
        }
        
        // Parse members
        while (true) {
            // Expect string key
            if (currentToken.type != TokenType.STRING) {
                throw new JsonParseException("Expected string key in object", currentToken.position);
            }
            String key = currentToken.value;
            advance();
            
            // Expect colon
            expect(TokenType.COLON);
            
            // Parse value
            JsonValue value = parseValue();
            obj.put(key, value);
            
            // Check for comma or end
            if (currentToken.type == TokenType.COMMA) {
                advance();
                // Must have another member after comma
                if (currentToken.type == TokenType.RIGHT_BRACE) {
                    throw new JsonParseException("Trailing comma in object", currentToken.position);
                }
            } else if (currentToken.type == TokenType.RIGHT_BRACE) {
                advance();
                break;
            } else {
                throw new JsonParseException("Expected ',' or '}' in object", currentToken.position);
            }
        }
        
        return obj;
    }
    
    /**
     * Parse JSON array: [ value1, value2, ... ]
     */
    private JsonArray parseArray() throws JsonParseException {
        expect(TokenType.LEFT_BRACKET);
        JsonArray arr = new JsonArray();
        
        // Empty array
        if (currentToken.type == TokenType.RIGHT_BRACKET) {
            advance();
            return arr;
        }
        
        // Parse elements
        while (true) {
            JsonValue value = parseValue();
            arr.add(value);
            
            // Check for comma or end
            if (currentToken.type == TokenType.COMMA) {
                advance();
                // Must have another element after comma
                if (currentToken.type == TokenType.RIGHT_BRACKET) {
                    throw new JsonParseException("Trailing comma in array", currentToken.position);
                }
            } else if (currentToken.type == TokenType.RIGHT_BRACKET) {
                advance();
                break;
            } else {
                throw new JsonParseException("Expected ',' or ']' in array", currentToken.position);
            }
        }
        
        return arr;
    }
    
    private JsonString parseString() throws JsonParseException {
        String value = currentToken.value;
        advance();
        return new JsonString(value);
    }
    
    private JsonNumber parseNumber() throws JsonParseException {
        double value = Double.parseDouble(currentToken.value);
        advance();
        return new JsonNumber(value);
    }
    
    private JsonBoolean parseTrue() throws JsonParseException {
        advance();
        return new JsonBoolean(true);
    }
    
    private JsonBoolean parseFalse() throws JsonParseException {
        advance();
        return new JsonBoolean(false);
    }
    
    private JsonNull parseNull() throws JsonParseException {
        advance();
        return new JsonNull();
    }
    
    /**
     * Expect a specific token type and advance
     */
    private void expect(TokenType type) throws JsonParseException {
        if (currentToken.type != type) {
            throw new JsonParseException("Expected " + type + ", got " + currentToken.type, 
                                        currentToken.position);
        }
        advance();
    }
    
    /**
     * Advance to next token
     */
    private void advance() throws JsonParseException {
        currentToken = lexer.nextToken();
    }
}

// ===== JSON MAIN CLASS =====

/**
 * JSON Parser - Main API
 * 
 * USAGE:
 *   String json = "{\"name\":\"John\",\"age\":30}";
 *   JsonValue value = Json.parse(json);
 *   JsonObject obj = value.asObject();
 *   String name = obj.get("name").asString();
 */
class Json {
    /**
     * Parse JSON string to JsonValue
     * 
     * @param json JSON string
     * @return Parsed JsonValue
     * @throws JsonParseException if invalid JSON
     */
    public static JsonValue parse(String json) throws JsonParseException {
        if (json == null || json.trim().isEmpty()) {
            throw new JsonParseException("Empty JSON input");
        }
        
        Parser parser = new Parser(json);
        return parser.parse();
    }
    
    /**
     * Convert JsonValue back to JSON string (minified)
     */
    public static String stringify(JsonValue value) {
        return value.toJsonString();
    }
    
    /**
     * Convert JsonValue to pretty-printed JSON string
     */
    public static String prettyPrint(JsonValue value) {
        return value.toPrettyString(0);
    }
}

// ===== MAIN TEST CLASS =====

public class JsonParser {
    public static void main(String[] args) {
        System.out.println("=== JSON Parser Test Cases ===\n");
        
        // Test Case 1: Simple Object
        System.out.println("=== Test Case 1: Simple Object ===");
        try {
            String json = "{\"name\":\"John\",\"age\":30,\"city\":\"NYC\"}";
            System.out.println("Input: " + json);
            JsonValue value = Json.parse(json);
            System.out.println("Parsed: " + value.toJsonString());
            
            JsonObject obj = value.asObject();
            System.out.println("name: " + obj.get("name").asString());
            System.out.println("age: " + (int)obj.get("age").asNumber());
            System.out.println("✓ Simple object parsing working");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 2: Simple Array
        System.out.println("=== Test Case 2: Simple Array ===");
        try {
            String json = "[1,2,3,4,5]";
            System.out.println("Input: " + json);
            JsonValue value = Json.parse(json);
            System.out.println("Parsed: " + value.toJsonString());
            
            JsonArray arr = value.asArray();
            System.out.println("Length: " + arr.size());
            System.out.println("arr[0]: " + (int)arr.get(0).asNumber());
            System.out.println("✓ Simple array parsing working");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 3: Nested Structure
        System.out.println("=== Test Case 3: Nested Structure ===");
        try {
            String json = "{\"user\":{\"name\":\"Alice\",\"age\":25},\"scores\":[90,85,95]}";
            System.out.println("Input: " + json);
            JsonValue value = Json.parse(json);
            
            JsonObject obj = value.asObject();
            JsonObject user = obj.get("user").asObject();
            JsonArray scores = obj.get("scores").asArray();
            
            System.out.println("user.name: " + user.get("name").asString());
            System.out.println("scores[0]: " + (int)scores.get(0).asNumber());
            System.out.println("✓ Nested structure working");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 4: All Value Types
        System.out.println("=== Test Case 4: All Value Types ===");
        try {
            String json = "{\"str\":\"hello\",\"num\":42,\"bool\":true,\"null\":null,\"arr\":[1,2]}";
            System.out.println("Input: " + json);
            JsonValue value = Json.parse(json);
            
            JsonObject obj = value.asObject();
            System.out.println("string: " + obj.get("str").asString());
            System.out.println("number: " + (int)obj.get("num").asNumber());
            System.out.println("boolean: " + obj.get("bool").asBoolean());
            System.out.println("null: " + obj.get("null").isNull());
            System.out.println("array size: " + obj.get("arr").asArray().size());
            System.out.println("✓ All types working");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 5: Pretty Print
        System.out.println("=== Test Case 5: Pretty Print ===");
        try {
            String json = "{\"name\":\"Bob\",\"hobbies\":[\"reading\",\"coding\"],\"active\":true}";
            JsonValue value = Json.parse(json);
            System.out.println("Pretty printed:");
            System.out.println(Json.prettyPrint(value));
            System.out.println("✓ Pretty print working");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 6: Empty Structures
        System.out.println("=== Test Case 6: Empty Structures ===");
        try {
            JsonValue emptyObj = Json.parse("{}");
            JsonValue emptyArr = Json.parse("[]");
            System.out.println("Empty object: " + emptyObj.toJsonString());
            System.out.println("Empty array: " + emptyArr.toJsonString());
            System.out.println("✓ Empty structures working");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 7: Whitespace Handling
        System.out.println("=== Test Case 7: Whitespace Handling ===");
        try {
            String json = " { \"key\" : \"value\" } ";
            JsonValue value = Json.parse(json);
            System.out.println("Parsed: " + value.toJsonString());
            System.out.println("✓ Whitespace handling working");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 8: Escape Sequences
        System.out.println("=== Test Case 8: Escape Sequences ===");
        try {
            String json = "{\"text\":\"Hello\\nWorld\\t!\"}";
            JsonValue value = Json.parse(json);
            JsonObject obj = value.asObject();
            String text = obj.get("text").asString();
            System.out.println("Parsed text: [" + text + "]");
            System.out.println("Contains newline: " + text.contains("\n"));
            System.out.println("✓ Escape sequences working");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 9: Numbers (integer, decimal, scientific)
        System.out.println("=== Test Case 9: Number Formats ===");
        try {
            String json = "[42,-17,3.14,-2.5,1e10,1.5e-3]";
            JsonValue value = Json.parse(json);
            JsonArray arr = value.asArray();
            System.out.println("integer: " + (int)arr.get(0).asNumber());
            System.out.println("negative: " + (int)arr.get(1).asNumber());
            System.out.println("decimal: " + arr.get(2).asNumber());
            System.out.println("scientific: " + arr.get(4).asNumber());
            System.out.println("✓ Number formats working");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 10: Deeply Nested
        System.out.println("=== Test Case 10: Deeply Nested ===");
        try {
            String json = "{\"a\":{\"b\":{\"c\":{\"d\":\"deep\"}}}}";
            JsonValue value = Json.parse(json);
            JsonObject obj = value.asObject();
            String deep = obj.get("a").asObject()
                             .get("b").asObject()
                             .get("c").asObject()
                             .get("d").asString();
            System.out.println("Deep value: " + deep);
            System.out.println("✓ Deep nesting working");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
        
        // ===== ERROR CASES =====
        
        // Test Case 11: Invalid JSON - Missing Comma
        System.out.println("=== Test Case 11: Error - Missing Comma ===");
        try {
            String json = "{\"a\":1 \"b\":2}";
            Json.parse(json);
            System.out.println("✗ Should have thrown exception");
        } catch (JsonParseException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 12: Invalid JSON - Trailing Comma
        System.out.println("=== Test Case 12: Error - Trailing Comma ===");
        try {
            String json = "[1,2,3,]";
            Json.parse(json);
            System.out.println("✗ Should have thrown exception");
        } catch (JsonParseException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 13: Invalid JSON - Unterminated String
        System.out.println("=== Test Case 13: Error - Unterminated String ===");
        try {
            String json = "{\"key\":\"value}";
            Json.parse(json);
            System.out.println("✗ Should have thrown exception");
        } catch (JsonParseException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong exception: " + e.getMessage());
        }
        System.out.println();
        
        // Test Case 14: Complex Real-World Example
        System.out.println("=== Test Case 14: Real-World Example ===");
        try {
            String json = "{\n" +
                "  \"users\": [\n" +
                "    {\"id\": 1, \"name\": \"Alice\", \"active\": true},\n" +
                "    {\"id\": 2, \"name\": \"Bob\", \"active\": false}\n" +
                "  ],\n" +
                "  \"metadata\": {\n" +
                "    \"version\": 1.5,\n" +
                "    \"timestamp\": \"2024-01-01\"\n" +
                "  }\n" +
                "}";
            
            System.out.println("Parsing complex JSON...");
            JsonValue value = Json.parse(json);
            JsonObject root = value.asObject();
            JsonArray users = root.get("users").asArray();
            
            System.out.println("Number of users: " + users.size());
            System.out.println("First user: " + users.get(0).asObject().get("name").asString());
            System.out.println("Version: " + root.get("metadata").asObject().get("version").asNumber());
            
            System.out.println("\nPretty printed:");
            System.out.println(Json.prettyPrint(value));
            System.out.println("✓ Complex parsing working");
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
        
        System.out.println("=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION TOPICS:
 * ============================
 * 
 * 1. PARSING ARCHITECTURE:
 *    Two-Phase Approach:
 *      Phase 1: Lexer (Tokenization)
 *        - Input string → token stream
 *        - Handles whitespace, escape sequences
 *        - Recognizes literals, keywords
 *      
 *      Phase 2: Parser (Syntax Analysis)
 *        - Token stream → AST (Abstract Syntax Tree)
 *        - Validates grammar rules
 *        - Builds data structure
 *    
 *    Why Separate?
 *      - Separation of concerns
 *      - Easier to maintain/test
 *      - Can swap lexer/parser implementations
 * 
 * 2. PARSING TECHNIQUES:
 *    Recursive Descent (used here):
 *      - Pros: Simple, intuitive, mirrors grammar
 *      - Cons: Left recursion issues, limited error recovery
 *      - Best for: Simple grammars like JSON
 *    
 *    Table-Driven (LR, LALR):
 *      - Pros: Handles more complex grammars
 *      - Cons: Complex to implement, needs generator
 *      - Example: YACC, Bison
 *    
 *    Parser Combinators:
 *      - Functional approach
 *      - Compose small parsers into larger ones
 *      - Example: Parsec (Haskell), JParsec (Java)
 * 
 * 3. JSON SPECIFICATION (RFC 8259):
 *    Value Types:
 *      - Object: { "key": value, ... }
 *      - Array: [ value, ... ]
 *      - String: "text" (UTF-8)
 *      - Number: 123, -45.67, 1.2e10
 *      - Boolean: true, false
 *      - Null: null
 *    
 *    Restrictions:
 *      - Keys must be strings
 *      - No trailing commas
 *      - No comments (not in spec, but common extension)
 *      - No NaN or Infinity
 * 
 * 4. ERROR HANDLING:
 *    Syntax Errors:
 *      - Unexpected token
 *      - Missing required token
 *      - Unterminated string
 *      - Invalid number format
 *    
 *    Error Recovery:
 *      - Panic mode: skip to synchronization point
 *      - Error productions: accept common mistakes
 *      - Best effort: continue parsing to find more errors
 * 
 * 5. PERFORMANCE OPTIMIZATION:
 *    Lexer:
 *      - Use StringBuilder for strings
 *      - Avoid backtracking
 *      - Single-pass scanning
 *    
 *    Parser:
 *      - Avoid unnecessary allocations
 *      - Reuse token objects
 *      - Lazy evaluation where possible
 *    
 *    Memory:
 *      - Stream large JSON (SAX-like)
 *      - Don't load entire document in memory
 *      - Use iterators for arrays
 * 
 * 6. ALTERNATIVE APPROACHES:
 *    Jackson (Java):
 *      - Streaming API (JsonParser/JsonGenerator)
 *      - Data binding (POJO mapping)
 *      - Tree model (JsonNode)
 *    
 *    Gson (Google):
 *      - Simple API
 *      - Type adapters
 *      - Custom serialization
 *    
 *    org.json (reference):
 *      - Simple, lightweight
 *      - No external dependencies
 * 
 * 7. ADVANCED FEATURES:
 *    JSON Schema:
 *      - Validate structure
 *      - Type checking
 *      - Required fields, constraints
 *    
 *    JSON Patch (RFC 6902):
 *      - Describe changes to JSON
 *      - Operations: add, remove, replace, move, copy, test
 *    
 *    JSON Pointer (RFC 6901):
 *      - Reference specific value
 *      - Example: /users/0/name
 *    
 *    JSONPath:
 *      - Query language (like XPath for JSON)
 *      - Example: $.users[?(@.active)]
 * 
 * 8. STREAMING PARSERS:
 *    Pull Parser:
 *      - Application pulls events
 *      - Example: JsonParser.nextToken()
 *      - Memory efficient
 *    
 *    Push Parser (SAX-like):
 *      - Parser pushes events to handlers
 *      - Callback-based
 *      - Event-driven
 * 
 * 9. SECURITY CONSIDERATIONS:
 *    Denial of Service:
 *      - Deeply nested structures (stack overflow)
 *      - Large numbers (memory exhaustion)
 *      - Circular references
 *    
 *    Mitigation:
 *      - Limit nesting depth
 *      - Limit string/number size
 *      - Timeout for parsing
 *      - Validate before parsing
 * 
 * 10. TIME COMPLEXITY:
 *     Operation       | Complexity
 *     Lexing          | O(n) - single pass
 *     Parsing         | O(n) - visit each token once
 *     Stringify       | O(n) - visit each node
 *     Pretty Print    | O(n) - visit each node
 *     Path Lookup     | O(d) - d = depth
 *     
 *     Where n = input length/node count
 * 
 * 11. SPACE COMPLEXITY:
 *     Lexer:  O(1) - only current token
 *     Parser: O(d) - recursion depth
 *     AST:    O(n) - all nodes stored
 *     
 *     Streaming: O(1) if processing on-the-fly
 * 
 * 12. TESTING STRATEGY:
 *     Valid JSON:
 *       - All value types
 *       - Nested structures
 *       - Empty collections
 *       - Unicode, escape sequences
 *     
 *     Invalid JSON:
 *       - Syntax errors at various positions
 *       - Trailing commas
 *       - Unterminated strings/objects/arrays
 *       - Invalid numbers
 *     
 *     Edge Cases:
 *       - Very large numbers
 *       - Deep nesting
 *       - Special characters
 *       - Empty input
 * 
 * 13. COMMON INTERVIEW QUESTIONS:
 *     Q: Why separate lexer and parser?
 *     A: Separation of concerns, easier to maintain, modularity
 *     
 *     Q: How to handle large JSON files?
 *     A: Streaming parser, don't load entire structure
 *     
 *     Q: How to validate JSON schema?
 *     A: Additional validation pass after parsing
 *     
 *     Q: How to handle errors gracefully?
 *     A: Throw exceptions with position, provide context
 *     
 *     Q: How to optimize for performance?
 *     A: Avoid backtracking, single-pass, minimize allocations
 * 
 * 14. DESIGN PATTERNS:
 *     Composite Pattern:
 *       - JsonValue is component
 *       - JsonObject/JsonArray are composites
 *       - JsonString/Number/etc are leaves
 *     
 *     Visitor Pattern:
 *       - Traverse JSON tree
 *       - Apply operations (transform, validate)
 *     
 *     Builder Pattern:
 *       - Construct JSON programmatically
 *       - Fluent API: obj.put("key", val).put("key2", val2)
 *     
 *     Factory Pattern:
 *       - Create appropriate JsonValue subtype
 *       - Hide construction complexity
 * 
 * 15. REAL-WORLD USAGE:
 *     REST APIs:
 *       - Request/response format
 *       - Content-Type: application/json
 *     
 *     Configuration Files:
 *       - package.json (npm)
 *       - tsconfig.json (TypeScript)
 *       - settings.json (VS Code)
 *     
 *     Data Exchange:
 *       - NoSQL databases (MongoDB)
 *       - Message queues
 *       - Log aggregation
 * 
 * 16. JSON vs ALTERNATIVES:
 *     XML:
 *       - More verbose
 *       - Better for documents
 *       - Schema support (XSD)
 *     
 *     YAML:
 *       - More human-readable
 *       - Indentation-based
 *       - Superset of JSON
 *     
 *     Protocol Buffers:
 *       - Binary format
 *       - Faster, smaller
 *       - Requires schema
 *     
 *     MessagePack:
 *       - Binary JSON
 *       - Compact, fast
 *       - Drop-in replacement
 */
