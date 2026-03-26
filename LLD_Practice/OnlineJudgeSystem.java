import java.time.*;
import java.util.*;

// ===== EXCEPTIONS =====

class ProblemNotFoundException extends Exception {
    public ProblemNotFoundException(String id) { super("Problem not found: " + id); }
}

class UserNotFoundException2 extends Exception {
    public UserNotFoundException2(String id) { super("User not found: " + id); }
}

// ===== ENUMS =====

enum Difficulty { EASY, MEDIUM, HARD }

enum Verdict { ACCEPTED, WRONG_ANSWER, TIME_LIMIT_EXCEEDED, RUNTIME_ERROR, COMPILATION_ERROR }

enum Language { JAVA, PYTHON, CPP, JAVASCRIPT }

// ===== INTERFACE =====

/**
 * Strategy: different ways to judge code
 * In real system: runs in sandboxed container with resource limits
 */
interface CodeJudge {
    Verdict judge(Problem problem, String code, Language language);
}

// ===== DOMAIN CLASSES =====

class Problem {
    private final String id;
    private final String title;
    private final String description;
    private final Difficulty difficulty;
    private final List<TestCase> testCases;
    private int acceptedCount;
    private int totalSubmissions;
    
    public Problem(String id, String title, String description, Difficulty difficulty) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.testCases = new ArrayList<>();
        this.acceptedCount = 0;
        this.totalSubmissions = 0;
    }
    
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Difficulty getDifficulty() { return difficulty; }
    public List<TestCase> getTestCases() { return testCases; }
    public int getAcceptedCount() { return acceptedCount; }
    public int getTotalSubmissions() { return totalSubmissions; }
    
    public void addTestCase(TestCase tc) { testCases.add(tc); }
    public void incrementAccepted() { acceptedCount++; }
    public void incrementTotal() { totalSubmissions++; }
    
    public double getAcceptanceRate() {
        return totalSubmissions == 0 ? 0 : (double) acceptedCount / totalSubmissions * 100;
    }
    
    @Override
    public String toString() {
        return id + ". " + title + " [" + difficulty + ", " 
            + String.format("%.1f", getAcceptanceRate()) + "% acceptance]";
    }
}

class TestCase {
    private final String input;
    private final String expectedOutput;
    private final boolean hidden;   // hidden test cases not shown to user
    
    public TestCase(String input, String expectedOutput, boolean hidden) {
        this.input = input;
        this.expectedOutput = expectedOutput;
        this.hidden = hidden;
    }
    
    public String getInput() { return input; }
    public String getExpectedOutput() { return expectedOutput; }
    public boolean isHidden() { return hidden; }
}

class Submission {
    private final String id;
    private final String problemId;
    private final String userId;
    private final String code;
    private final Language language;
    private Verdict verdict;
    private int testsPassed;
    private int totalTests;
    private long executionTimeMs;
    private final LocalDateTime submittedAt;
    
    public Submission(String problemId, String userId, String code, Language language) {
        this.id = "SUB-" + UUID.randomUUID().toString().substring(0, 6);
        this.problemId = problemId;
        this.userId = userId;
        this.code = code;
        this.language = language;
        this.verdict = null;
        this.submittedAt = LocalDateTime.now();
    }
    
    public String getId() { return id; }
    public String getProblemId() { return problemId; }
    public String getUserId() { return userId; }
    public String getCode() { return code; }
    public Language getLanguage() { return language; }
    public Verdict getVerdict() { return verdict; }
    public int getTestsPassed() { return testsPassed; }
    public int getTotalTests() { return totalTests; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    
    public void setVerdict(Verdict v) { this.verdict = v; }
    public void setTestsPassed(int p) { this.testsPassed = p; }
    public void setTotalTests(int t) { this.totalTests = t; }
    public void setExecutionTimeMs(long ms) { this.executionTimeMs = ms; }
    
    @Override
    public String toString() {
        return id + "[" + problemId + ", " + verdict + ", " + testsPassed + "/" + totalTests 
            + ", " + executionTimeMs + "ms, " + language + "]";
    }
}

class OJUser {
    private final String userId;
    private final String name;
    private final Set<String> solvedProblems;   // problem IDs
    private int totalSubmissions;
    
    public OJUser(String userId, String name) {
        this.userId = userId;
        this.name = name;
        this.solvedProblems = new LinkedHashSet<>();
        this.totalSubmissions = 0;
    }
    
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public Set<String> getSolvedProblems() { return Collections.unmodifiableSet(solvedProblems); }
    public int getSolvedCount() { return solvedProblems.size(); }
    public int getTotalSubmissions() { return totalSubmissions; }
    
    public void addSolved(String problemId) { solvedProblems.add(problemId); }
    public void incrementSubmissions() { totalSubmissions++; }
    
    @Override
    public String toString() { return name + "[solved=" + solvedProblems.size() + ", submissions=" + totalSubmissions + "]"; }
}

// ===== JUDGE IMPLEMENTATIONS =====

/**
 * Simulated judge: checks if code contains the expected output string
 * In real system: compile → run in sandbox → compare output per test case
 */
class SimulatedJudge implements CodeJudge {
    @Override
    public Verdict judge(Problem problem, String code, Language language) {
        // Simulate: if code contains "return" → ACCEPTED, else WRONG_ANSWER
        if (code.contains("COMPILE_ERROR")) return Verdict.COMPILATION_ERROR;
        if (code.contains("TLE")) return Verdict.TIME_LIMIT_EXCEEDED;
        if (code.contains("return") || code.contains("print")) return Verdict.ACCEPTED;
        return Verdict.WRONG_ANSWER;
    }
}

// ===== SERVICE =====

/**
 * Online Judge System - Low Level Design (LLD)
 * 
 * PROBLEM: Design a LeetCode-like system that can:
 * 1. Add problems with test cases (visible + hidden)
 * 2. Submit code solutions
 * 3. Judge submissions against test cases
 * 4. Track user progress (solved problems, submissions)
 * 5. Get problem list (filter by difficulty, sort by acceptance rate)
 * 6. Get submission history
 * 7. Leaderboard (most problems solved)
 * 
 * KEY FLOW:
 *   User submits code → Judge runs against test cases → Verdict
 *   ACCEPTED only if ALL test cases pass
 * 
 * PATTERNS: Strategy (judge implementation)
 */
class OnlineJudgeService {
    private final Map<String, Problem> problems;
    private final Map<String, OJUser> users;
    private final Map<String, Submission> submissions;
    private final Map<String, List<String>> userSubmissions;     // userId → [submissionIds]
    private final Map<String, List<String>> problemSubmissions;  // problemId → [submissionIds]
    private CodeJudge judge;
    
    public OnlineJudgeService(CodeJudge judge) {
        this.problems = new LinkedHashMap<>();
        this.users = new HashMap<>();
        this.submissions = new HashMap<>();
        this.userSubmissions = new HashMap<>();
        this.problemSubmissions = new HashMap<>();
        this.judge = judge;
    }
    
    // ===== PROBLEM MANAGEMENT =====
    
    /**
     * Add a problem
     * 
     * IMPLEMENTATION HINTS:
     * 1. Create Problem, store in map
     * 2. Init empty submission list for this problem
     * 3. Return problem
     */
    public Problem addProblem(String id, String title, String desc, Difficulty difficulty, List<TestCase> testCases) {
        // TODO: Implement
        // HINT: Problem p = new Problem(id, title, desc, difficulty);
        // HINT: testCases.forEach(p::addTestCase);
        // HINT: problems.put(id, p);
        // HINT: problemSubmissions.put(id, new ArrayList<>());
        // HINT: System.out.println("  ✓ Problem added: " + p);
        // HINT: return p;
        return null;
    }
    
    /**
     * Register a user
     */
    public OJUser registerUser(String userId, String name) {
        // TODO: Implement
        // HINT: OJUser user = new OJUser(userId, name);
        // HINT: users.put(userId, user);
        // HINT: userSubmissions.put(userId, new ArrayList<>());
        // HINT: return user;
        return null;
    }
    
    // ===== SUBMIT & JUDGE =====
    
    /**
     * Submit a solution and judge it
     * 
     * IMPLEMENTATION HINTS:
     * 1. Validate problem exists → throw ProblemNotFoundException
     * 2. Validate user exists → throw UserNotFoundException2
     * 3. Create Submission
     * 4. Call judge.judge(problem, code, language) → get verdict
     * 5. Set submission verdict, testsPassed, totalTests, executionTime
     * 6. If ACCEPTED: mark problem as solved for user, increment problem acceptedCount
     * 7. Increment problem totalSubmissions and user totalSubmissions
     * 8. Store submission, add to user + problem history
     * 9. Return submission
     */
    public Submission submit(String userId, String problemId, String code, Language language) 
            throws ProblemNotFoundException, UserNotFoundException2 {
        // TODO: Implement
        // HINT: Problem problem = problems.get(problemId);
        // HINT: if (problem == null) throw new ProblemNotFoundException(problemId);
        // HINT: OJUser user = users.get(userId);
        // HINT: if (user == null) throw new UserNotFoundException2(userId);
        //
        // HINT: Submission sub = new Submission(problemId, userId, code, language);
        // HINT: long start = System.currentTimeMillis();
        // HINT: Verdict verdict = judge.judge(problem, code, language);
        // HINT: sub.setExecutionTimeMs(System.currentTimeMillis() - start);
        // HINT: sub.setVerdict(verdict);
        // HINT: sub.setTotalTests(problem.getTestCases().size());
        // HINT: sub.setTestsPassed(verdict == Verdict.ACCEPTED ? problem.getTestCases().size() : 0);
        //
        // HINT: if (verdict == Verdict.ACCEPTED) {
        //     user.addSolved(problemId);
        //     problem.incrementAccepted();
        // }
        // HINT: problem.incrementTotal();
        // HINT: user.incrementSubmissions();
        //
        // HINT: submissions.put(sub.getId(), sub);
        // HINT: userSubmissions.get(userId).add(sub.getId());
        // HINT: problemSubmissions.get(problemId).add(sub.getId());
        //
        // HINT: System.out.println("  📝 " + sub);
        // HINT: return sub;
        return null;
    }
    
    // ===== QUERIES =====
    
    /**
     * Get all problems, optionally filtered by difficulty
     * 
     * IMPLEMENTATION HINTS:
     * 1. Stream problems, filter by difficulty if not null
     * 2. Return as list
     */
    public List<Problem> getProblems(Difficulty difficulty) {
        // TODO: Implement
        // HINT: if (difficulty == null) return new ArrayList<>(problems.values());
        // HINT: List<Problem> result = new ArrayList<>();
        // HINT: for (Problem p : problems.values()) {
        //     if (p.getDifficulty() == difficulty) result.add(p);
        // }
        // HINT: return result;
        return null;
    }
    
    public List<Problem> getProblems() { return getProblems(null); }
    
    /**
     * Get user's submission history for a problem
     */
    public List<Submission> getUserSubmissions(String userId, String problemId) {
        // TODO: Implement
        // HINT: List<Submission> result = new ArrayList<>();
        // HINT: List<String> subIds = userSubmissions.getOrDefault(userId, Collections.emptyList());
        // HINT: for (String id : subIds) {
        //     Submission s = submissions.get(id);
        //     if (s != null && s.getProblemId().equals(problemId)) result.add(s);
        // }
        // HINT: return result;
        return null;
    }
    
    /**
     * Get leaderboard — users sorted by problems solved (desc)
     * 
     * IMPLEMENTATION HINTS:
     * 1. Sort users by solvedCount descending
     * 2. Return top N
     */
    public List<OJUser> getLeaderboard(int limit) {
        // TODO: Implement
        // HINT: List<OJUser> sorted = new ArrayList<>(users.values());
        // HINT: sorted.sort((a, b) -> b.getSolvedCount() - a.getSolvedCount());
        // HINT: return sorted.subList(0, Math.min(limit, sorted.size()));
        return null;
    }
    
    /**
     * Get user's solved problems count by difficulty
     */
    public Map<Difficulty, Integer> getUserStats(String userId) {
        // TODO: Implement
        // HINT: OJUser user = users.get(userId);
        // HINT: if (user == null) return Collections.emptyMap();
        // HINT: Map<Difficulty, Integer> stats = new LinkedHashMap<>();
        // HINT: for (Difficulty d : Difficulty.values()) stats.put(d, 0);
        // HINT: for (String pid : user.getSolvedProblems()) {
        //     Problem p = problems.get(pid);
        //     if (p != null) stats.merge(p.getDifficulty(), 1, Integer::sum);
        // }
        // HINT: return stats;
        return null;
    }
    
    public Problem getProblem(String id) { return problems.get(id); }
    public OJUser getUser(String id) { return users.get(id); }
}

// ===== MAIN TEST CLASS =====

public class OnlineJudgeSystem {
    public static void main(String[] args) {
        System.out.println("=== Online Judge (LeetCode) LLD ===\n");
        
        OnlineJudgeService oj = new OnlineJudgeService(new SimulatedJudge());
        
        // Setup: Add problems
        System.out.println("=== Setup: Add Problems ===");
        oj.addProblem("1", "Two Sum", "Find two numbers that add to target", Difficulty.EASY,
            Arrays.asList(new TestCase("[2,7,11,15], 9", "[0,1]", false),
                          new TestCase("[3,2,4], 6", "[1,2]", true)));
        
        oj.addProblem("2", "LRU Cache", "Design LRU Cache", Difficulty.MEDIUM,
            Arrays.asList(new TestCase("put(1,1),get(1)", "1", false),
                          new TestCase("put(1,1),put(2,2),get(1)", "1", true)));
        
        oj.addProblem("3", "Median of Two Arrays", "Find median of sorted arrays", Difficulty.HARD,
            Arrays.asList(new TestCase("[1,3],[2]", "2.0", false),
                          new TestCase("[1,2],[3,4]", "2.5", true)));
        
        // Setup: Register users
        oj.registerUser("alice", "Alice");
        oj.registerUser("bob", "Bob");
        oj.registerUser("charlie", "Charlie");
        System.out.println();
        
        // Test 1: Submit — ACCEPTED
        System.out.println("=== Test 1: Submit — Accepted ===");
        try {
            Submission s = oj.submit("alice", "1", "int[] twoSum() { return new int[]{0,1}; }", Language.JAVA);
            System.out.println("✓ " + (s != null ? s : "null"));
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 2: Submit — WRONG_ANSWER
        System.out.println("=== Test 2: Submit — Wrong Answer ===");
        try {
            Submission s = oj.submit("bob", "1", "int[] twoSum() { // no return }", Language.JAVA);
            System.out.println("✓ " + (s != null ? s : "null"));
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 3: Submit — COMPILATION_ERROR
        System.out.println("=== Test 3: Submit — Compile Error ===");
        try {
            Submission s = oj.submit("bob", "1", "COMPILE_ERROR bad syntax", Language.JAVA);
            System.out.println("✓ " + (s != null ? s : "null"));
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 4: Submit — TLE
        System.out.println("=== Test 4: Submit — Time Limit Exceeded ===");
        try {
            Submission s = oj.submit("charlie", "3", "while(true) TLE {}", Language.CPP);
            System.out.println("✓ " + (s != null ? s : "null"));
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 5: Multiple submissions, solve multiple problems
        System.out.println("=== Test 5: Solve Multiple Problems ===");
        try {
            oj.submit("alice", "2", "class LRUCache { return cache.get(key); }", Language.JAVA);
            oj.submit("alice", "3", "double findMedian() { return 2.0; }", Language.JAVA);
            oj.submit("bob", "1", "def twoSum(): return [0,1]", Language.PYTHON);
            oj.submit("charlie", "1", "print([0,1])", Language.PYTHON);
            oj.submit("charlie", "2", "print(cache)", Language.PYTHON);
            
            OJUser alice = oj.getUser("alice");
            System.out.println("✓ Alice solved: " + (alice != null ? alice.getSolvedCount() : 0) + " (expect 3)");
        } catch (Exception e) {
            System.out.println("✗ " + e.getMessage());
        }
        System.out.println();
        
        // Test 6: Acceptance rate
        System.out.println("=== Test 6: Acceptance Rate ===");
        Problem p1 = oj.getProblem("1");
        System.out.println("✓ Two Sum: " + (p1 != null ? String.format("%.1f", p1.getAcceptanceRate()) + "%" : "null")
            + " (" + (p1 != null ? p1.getAcceptedCount() + "/" + p1.getTotalSubmissions() : "?") + ")");
        System.out.println();
        
        // Test 7: Filter by difficulty
        System.out.println("=== Test 7: Filter by Difficulty ===");
        List<Problem> easyProblems = oj.getProblems(Difficulty.EASY);
        List<Problem> hardProblems = oj.getProblems(Difficulty.HARD);
        System.out.println("✓ EASY: " + (easyProblems != null ? easyProblems.size() : 0));
        System.out.println("✓ HARD: " + (hardProblems != null ? hardProblems.size() : 0));
        System.out.println();
        
        // Test 8: User submission history
        System.out.println("=== Test 8: Submission History ===");
        List<Submission> bobHistory = oj.getUserSubmissions("bob", "1");
        System.out.println("✓ Bob's submissions for Two Sum: " + (bobHistory != null ? bobHistory.size() : 0));
        if (bobHistory != null) bobHistory.forEach(s -> System.out.println("    " + s));
        System.out.println();
        
        // Test 9: Leaderboard
        System.out.println("=== Test 9: Leaderboard ===");
        List<OJUser> leaderboard = oj.getLeaderboard(5);
        System.out.println("✓ Leaderboard:");
        if (leaderboard != null) {
            int rank = 1;
            for (OJUser u : leaderboard) System.out.println("    #" + rank++ + " " + u);
        }
        System.out.println();
        
        // Test 10: User stats by difficulty
        System.out.println("=== Test 10: User Stats ===");
        Map<Difficulty, Integer> stats = oj.getUserStats("alice");
        System.out.println("✓ Alice's stats: " + (stats != null ? stats : "null"));
        System.out.println();
        
        // Test 11: Exception — problem not found
        System.out.println("=== Test 11: Exception - Problem Not Found ===");
        try {
            oj.submit("alice", "999", "code", Language.JAVA);
            System.out.println("✗ Should have thrown");
        } catch (ProblemNotFoundException e) {
            System.out.println("✓ Caught: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong: " + e.getMessage());
        }
        System.out.println();
        
        // Test 12: Exception — user not found
        System.out.println("=== Test 12: Exception - User Not Found ===");
        try {
            oj.submit("unknown", "1", "code", Language.JAVA);
            System.out.println("✗ Should have thrown");
        } catch (UserNotFoundException2 e) {
            System.out.println("✓ Caught: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Wrong: " + e.getMessage());
        }
        
        System.out.println("\n=== All Test Cases Complete! ===");
    }
}

/**
 * INTERVIEW DISCUSSION:
 * =====================
 * 
 * 1. CORE FLOW:
 *    User → submit(code) → Judge → run against test cases → Verdict
 *    ACCEPTED only if ALL test cases pass (visible + hidden)
 * 
 * 2. JUDGING IN REAL SYSTEM:
 *    Sandboxed execution (Docker container per submission)
 *    Time limit: kill process after N seconds → TLE
 *    Memory limit: cgroup limits → MLE
 *    Security: no network, no filesystem, no system calls
 *    Language support: compile → run → capture stdout → compare
 * 
 * 3. TEST CASES:
 *    Visible: shown to user (sample input/output)
 *    Hidden: not shown — prevents hardcoding
 *    Edge cases: empty input, max size, overflow
 * 
 * 4. VERDICTS:
 *    AC: all test cases pass
 *    WA: output doesn't match expected
 *    TLE: exceeded time limit
 *    MLE: exceeded memory limit
 *    RE: runtime error (NPE, array out of bounds)
 *    CE: compilation error
 * 
 * 5. SCALABILITY:
 *    Queue submissions → worker pool judges in parallel
 *    Each worker: Docker container with resource limits
 *    Pre-compile test harness per language
 *    Cache compiled binaries for re-submissions
 * 
 * 6. REAL-WORLD: LeetCode, Codeforces, HackerRank, CodeChef
 * 
 * 7. API:
 *    GET  /problems?difficulty=EASY     — list problems
 *    GET  /problems/{id}                — problem details
 *    POST /problems/{id}/submit         — submit solution
 *    GET  /submissions/{id}             — submission result
 *    GET  /users/{id}/submissions       — submission history
 *    GET  /leaderboard                  — top solvers
 *    GET  /users/{id}/stats             — solved by difficulty
 */
