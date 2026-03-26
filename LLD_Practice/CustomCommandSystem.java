import java.util.*;

// ===== EXCEPTIONS =====

class CommandNotFoundException extends Exception {
    public CommandNotFoundException(String name) { super("Command not found: " + name); }
}

class ActionFailedException extends Exception {
    public ActionFailedException(String msg) { super("Action failed: " + msg); }
}

// ===== ENUMS =====

enum ActionType { SMART_HOME, MUSIC, NEWS, WEATHER, REMINDER, CUSTOM_RESPONSE }

enum ExecutionMode { SEQUENTIAL, PARALLEL }

// ===== INTERFACE (Command + Strategy Pattern) =====

/**
 * Action — a single thing to do (turn on light, play music, etc.)
 */
interface Action {
    String execute() throws ActionFailedException;
    ActionType getType();
    String getDescription();
}

// ===== ACTION IMPLEMENTATIONS =====

class SmartHomeAction implements Action {
    private final String device;
    private final String command;  // "on", "off", "set 72°F"
    
    public SmartHomeAction(String device, String command) {
        this.device = device;
        this.command = command;
    }
    
    @Override
    public String execute() throws ActionFailedException {
        return "🏠 " + device + " → " + command;
    }
    
    @Override public ActionType getType() { return ActionType.SMART_HOME; }
    @Override public String getDescription() { return device + ": " + command; }
}

class MusicAction implements Action {
    private final String playlist;
    
    public MusicAction(String playlist) { this.playlist = playlist; }
    
    @Override
    public String execute() throws ActionFailedException {
        return "🎵 Playing: " + playlist;
    }
    
    @Override public ActionType getType() { return ActionType.MUSIC; }
    @Override public String getDescription() { return "Play " + playlist; }
}

class NewsAction implements Action {
    private final String category;  // "top", "tech", "sports"
    
    public NewsAction(String category) { this.category = category; }
    
    @Override
    public String execute() throws ActionFailedException {
        return "📰 News briefing: " + category;
    }
    
    @Override public ActionType getType() { return ActionType.NEWS; }
    @Override public String getDescription() { return "News: " + category; }
}

class WeatherAction implements Action {
    private final String location;
    
    public WeatherAction(String location) { this.location = location; }
    
    @Override
    public String execute() throws ActionFailedException {
        return "🌤️ Weather for " + location + ": 72°F, Sunny";
    }
    
    @Override public ActionType getType() { return ActionType.WEATHER; }
    @Override public String getDescription() { return "Weather: " + location; }
}

class CustomResponseAction implements Action {
    private final String response;
    
    public CustomResponseAction(String response) { this.response = response; }
    
    @Override
    public String execute() throws ActionFailedException {
        return "💬 " + response;
    }
    
    @Override public ActionType getType() { return ActionType.CUSTOM_RESPONSE; }
    @Override public String getDescription() { return "Say: " + response; }
}

class FailingAction implements Action {
    @Override
    public String execute() throws ActionFailedException {
        throw new ActionFailedException("Device unreachable");
    }
    @Override public ActionType getType() { return ActionType.SMART_HOME; }
    @Override public String getDescription() { return "Failing action"; }
}

// ===== DOMAIN CLASSES =====

/**
 * Custom Command — a named trigger phrase with a list of actions
 * "Alexa, good morning" → [turn on lights, play music, read news, tell weather]
 */
class CustomCommand {
    private final String name;             // trigger phrase: "good morning"
    private final String userId;
    private final List<Action> actions;
    private ExecutionMode mode;
    private boolean enabled;
    
    public CustomCommand(String name, String userId) {
        this.name = name.toLowerCase().trim();
        this.userId = userId;
        this.actions = new ArrayList<>();
        this.mode = ExecutionMode.SEQUENTIAL;
        this.enabled = true;
    }
    
    public String getName() { return name; }
    public String getUserId() { return userId; }
    public List<Action> getActions() { return Collections.unmodifiableList(actions); }
    public ExecutionMode getMode() { return mode; }
    public boolean isEnabled() { return enabled; }
    
    public void addAction(Action action) { actions.add(action); }
    public void removeAction(int index) { if (index >= 0 && index < actions.size()) actions.remove(index); }
    public void setMode(ExecutionMode m) { this.mode = m; }
    public void setEnabled(boolean e) { this.enabled = e; }
    
    @Override
    public String toString() {
        return "\"" + name + "\" [" + actions.size() + " actions, " + mode + ", " + (enabled ? "ON" : "OFF") + "]";
    }
}

/**
 * Execution result for a command run
 */
class CommandResult {
    private final String commandName;
    private final List<String> actionResults;
    private final List<String> errors;
    private final boolean allSucceeded;
    
    public CommandResult(String commandName, List<String> actionResults, List<String> errors) {
        this.commandName = commandName;
        this.actionResults = actionResults;
        this.errors = errors;
        this.allSucceeded = errors.isEmpty();
    }
    
    public String getCommandName() { return commandName; }
    public List<String> getActionResults() { return actionResults; }
    public List<String> getErrors() { return errors; }
    public boolean isAllSucceeded() { return allSucceeded; }
    
    @Override
    public String toString() {
        return commandName + "[" + actionResults.size() + " OK, " + errors.size() + " errors]";
    }
}

// ===== SERVICE =====

/**
 * Custom Command System - Low Level Design (LLD)
 * 
 * PROBLEM (Amazon Alexa SDE Question): Design a system where users can:
 * 1. Create custom voice commands (e.g., "good morning")
 * 2. Add multiple actions to a command (lights, music, news, weather)
 * 3. Execute all actions when command is triggered
 * 4. Handle action failures gracefully (continue other actions)
 * 5. Enable/disable commands
 * 6. List user's commands
 * 
 * PATTERNS:
 * - Command: each CustomCommand encapsulates a list of actions
 * - Strategy: each Action is a strategy implementation
 * - Composite: command groups multiple actions
 */
class CommandService {
    private final Map<String, Map<String, CustomCommand>> userCommands;  // userId → {cmdName → cmd}
    
    public CommandService() {
        this.userCommands = new HashMap<>();
    }
    
    /**
     * Create a custom command for a user
     * 
     * IMPLEMENTATION HINTS:
     * 1. Normalize command name (lowercase, trim)
     * 2. Get or create user's command map
     * 3. Check if command already exists → return existing or overwrite
     * 4. Store and return
     */
    public CustomCommand createCommand(String userId, String commandName) {
        // HINT: String normalized = commandName.toLowerCase().trim();
        // HINT: userCommands.computeIfAbsent(userId, k -> new HashMap<>());
        // HINT: CustomCommand cmd = new CustomCommand(normalized, userId);
        // HINT: userCommands.get(userId).put(normalized, cmd);
        // HINT: System.out.println("  ✓ Created command: " + cmd);
        // HINT: return cmd;
        String normalized = commandName.toLowerCase().trim();
        userCommands.computeIfAbsent(userId, k->new HashMap<>());
        CustomCommand cmd = new CustomCommand(normalized, userId);
        userCommands.get(userId).put(userId, cmd);
        System.out.println("  ✓ Created command: " + cmd);
        return cmd;
    }
    
    /**
     * Add an action to a command
     */
    public void addAction(String userId, String commandName, Action action) throws CommandNotFoundException {
        // TODO: Implement
        // HINT: CustomCommand cmd = getCommand(userId, commandName);
        // HINT: cmd.addAction(action);
        // HINT: System.out.println("    + " + action.getDescription());
    }
    
    /**
     * Execute a command — run all actions
     * 
     * IMPLEMENTATION HINTS:
     * 1. Get command → throw if not found
     * 2. Check if enabled → return empty result if disabled
     * 3. For each action:
     *    a. Try to execute
     *    b. On success → add result to results list
     *    c. On failure → add error to errors list (don't stop other actions!)
     * 4. Return CommandResult with results + errors
     */
    public CommandResult executeCommand(String userId, String commandName) throws CommandNotFoundException {
        // TODO: Implement
        // HINT: CustomCommand cmd = getCommand(userId, commandName);
        // HINT: if (!cmd.isEnabled()) {
        //     System.out.println("  ⏸️ Command disabled: " + commandName);
        //     return new CommandResult(commandName, Collections.emptyList(), Collections.emptyList());
        // }
        //
        // HINT: System.out.println("  🗣️ Executing: \"" + commandName + "\"");
        // HINT: List<String> results = new ArrayList<>();
        // HINT: List<String> errors = new ArrayList<>();
        //
        // HINT: for (Action action : cmd.getActions()) {
        //     try {
        //         String result = action.execute();
        //         results.add(result);
        //         System.out.println("    " + result);
        //     } catch (ActionFailedException e) {
        //         errors.add(action.getDescription() + ": " + e.getMessage());
        //         System.out.println("    ❌ " + action.getDescription() + " failed: " + e.getMessage());
        //     }
        // }
        //
        // HINT: return new CommandResult(commandName, results, errors);
        return null;
    }
    
    /**
     * Get a specific command
     */
    public CustomCommand getCommand(String userId, String commandName) throws CommandNotFoundException {
        // TODO: Implement
        // HINT: String normalized = commandName.toLowerCase().trim();
        // HINT: Map<String, CustomCommand> cmds = userCommands.get(userId);
        // HINT: if (cmds == null || !cmds.containsKey(normalized))
        //           throw new CommandNotFoundException(normalized);
        // HINT: return cmds.get(normalized);
        return null;
    }
    
    /**
     * Get all commands for a user
     */
    public List<CustomCommand> getUserCommands(String userId) {
        // TODO: Implement
        // HINT: Map<String, CustomCommand> cmds = userCommands.get(userId);
        // HINT: if (cmds == null) return Collections.emptyList();
        // HINT: return new ArrayList<>(cmds.values());
        return null;
    }
    
    /**
     * Enable/disable a command
     */
    public void setEnabled(String userId, String commandName, boolean enabled) throws CommandNotFoundException {
        // TODO: Implement
        // HINT: CustomCommand cmd = getCommand(userId, commandName);
        // HINT: cmd.setEnabled(enabled);
        // HINT: System.out.println("  " + (enabled ? "✅" : "⏸️") + " " + commandName + " " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Delete a command
     */
    public void deleteCommand(String userId, String commandName) throws CommandNotFoundException {
        // TODO: Implement
        // HINT: String normalized = commandName.toLowerCase().trim();
        // HINT: Map<String, CustomCommand> cmds = userCommands.get(userId);
        // HINT: if (cmds == null || !cmds.containsKey(normalized))
        //           throw new CommandNotFoundException(normalized);
        // HINT: cmds.remove(normalized);
        // HINT: System.out.println("  🗑️ Deleted: " + normalized);
    }
}

// ===== MAIN TEST CLASS =====

public class CustomCommandSystem {
    public static void main(String[] args) {
        System.out.println("=== Alexa Custom Command LLD ===\n");
        
        CommandService service = new CommandService();
        
        // Test 1: Create "good morning" command
        System.out.println("=== Test 1: Create Command ===");
        try {
            CustomCommand morning = service.createCommand("alice", "good morning");
            service.addAction("alice", "good morning", new SmartHomeAction("Bedroom lights", "on"));
            service.addAction("alice", "good morning", new SmartHomeAction("Thermostat", "set 72°F"));
            service.addAction("alice", "good morning", new MusicAction("Morning Jazz Playlist"));
            service.addAction("alice", "good morning", new NewsAction("top headlines"));
            service.addAction("alice", "good morning", new WeatherAction("Seattle"));
            System.out.println("✓ " + morning);
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 2: Execute command
        System.out.println("=== Test 2: Execute Command ===");
        try {
            CommandResult result = service.executeCommand("alice", "good morning");
            System.out.println("✓ " + (result != null ? result : "null"));
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 3: Create "good night" command
        System.out.println("=== Test 3: Another Command ===");
        try {
            service.createCommand("alice", "good night");
            service.addAction("alice", "good night", new SmartHomeAction("All lights", "off"));
            service.addAction("alice", "good night", new SmartHomeAction("Thermostat", "set 65°F"));
            service.addAction("alice", "good night", new SmartHomeAction("Door lock", "lock"));
            service.addAction("alice", "good night", new CustomResponseAction("Good night! Sweet dreams."));
            
            CommandResult result = service.executeCommand("alice", "good night");
            System.out.println("✓ " + (result != null ? result : "null"));
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 4: Action failure handling (continue other actions)
        System.out.println("=== Test 4: Partial Failure ===");
        try {
            service.createCommand("alice", "party mode");
            service.addAction("alice", "party mode", new SmartHomeAction("Party lights", "on"));
            service.addAction("alice", "party mode", new FailingAction()); // this will fail
            service.addAction("alice", "party mode", new MusicAction("Party Mix"));
            
            CommandResult result = service.executeCommand("alice", "party mode");
            if (result != null) {
                System.out.println("  Succeeded: " + result.getActionResults().size());
                System.out.println("  Failed: " + result.getErrors().size());
                System.out.println("  All OK: " + result.isAllSucceeded() + " (expect false)");
            }
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 5: Disable command
        System.out.println("=== Test 5: Disable/Enable ===");
        try {
            service.setEnabled("alice", "good morning", false);
            CommandResult result = service.executeCommand("alice", "good morning");
            System.out.println("✓ Disabled result: " + (result != null ? result.getActionResults().size() + " actions" : "null") + " (expect 0)");
            
            service.setEnabled("alice", "good morning", true);
            result = service.executeCommand("alice", "good morning");
            System.out.println("✓ Re-enabled: " + (result != null ? result.getActionResults().size() + " actions" : "null") + " (expect 5)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 6: List user commands
        System.out.println("=== Test 6: List Commands ===");
        List<CustomCommand> cmds = service.getUserCommands("alice");
        System.out.println("✓ Alice's commands: " + (cmds != null ? cmds.size() : 0));
        if (cmds != null) cmds.forEach(c -> System.out.println("    " + c));
        System.out.println();
        
        // Test 7: Different user
        System.out.println("=== Test 7: Different User ===");
        try {
            service.createCommand("bob", "movie time");
            service.addAction("bob", "movie time", new SmartHomeAction("Living room lights", "dim 20%"));
            service.addAction("bob", "movie time", new SmartHomeAction("TV", "on"));
            service.addAction("bob", "movie time", new CustomResponseAction("Enjoy your movie!"));
            service.executeCommand("bob", "movie time");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 8: Delete command
        System.out.println("=== Test 8: Delete Command ===");
        try {
            service.deleteCommand("alice", "party mode");
            System.out.println("✓ Deleted party mode");
            
            service.executeCommand("alice", "party mode"); // should throw
            System.out.println("✗ Should have thrown");
        } catch (CommandNotFoundException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 9: Command not found
        System.out.println("=== Test 9: Exception - Not Found ===");
        try {
            service.executeCommand("alice", "nonexistent");
            System.out.println("✗ Should have thrown");
        } catch (CommandNotFoundException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        }
        
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION:
 * =====================
 * 
 * 1. PATTERNS:
 *    Command Pattern: CustomCommand encapsulates actions to execute
 *    Strategy: each Action type is a strategy (SmartHome, Music, News, etc.)
 *    Composite: command groups multiple actions together
 * 
 * 2. FAILURE HANDLING:
 *    Key requirement: if one action fails, others still execute
 *    "Turn on lights" fails → still play music and read news
 *    Collect results + errors, report partial success
 * 
 * 3. EXECUTION MODES:
 *    Sequential: one after another (order matters)
 *    Parallel: all at once (faster, order doesn't matter)
 *    Could add: conditional (if weather is cold → turn on heater)
 * 
 * 4. EXTENSIBILITY:
 *    New action type? → implement Action interface
 *    New execution mode? → add to ExecutionMode enum
 *    Open/Closed: add actions without modifying CommandService
 * 
 * 5. USER ISOLATION:
 *    Each user has their own commands (Map<userId, Map<name, cmd>>)
 *    Alice's "good morning" ≠ Bob's "good morning"
 * 
 * 6. REAL-WORLD: Alexa Routines, Google Home Routines, IFTTT Applets
 * 
 * 7. API:
 *    POST /users/{id}/commands                    — create command
 *    POST /users/{id}/commands/{name}/actions      — add action
 *    POST /users/{id}/commands/{name}/execute       — trigger command
 *    PUT  /users/{id}/commands/{name}/enabled       — enable/disable
 *    DELETE /users/{id}/commands/{name}             — delete command
 *    GET  /users/{id}/commands                     — list commands
 */
