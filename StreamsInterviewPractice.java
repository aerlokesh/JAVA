/**
 * StreamsInterviewPractice.java
 * 
 * Real-World & Interview-Focused Java Streams Practice
 * Based on Top Interview Questions from FAANG/Big Tech Companies
 * 
 * This file focuses on practical, interview-style problems that combine multiple stream operations.
 * Topics covered: Word frequency, trending topics, employee analytics, transaction processing, etc.
 * 
 * Instructions:
 * 1. Read each problem statement carefully
 * 2. Implement the solution using Java Streams API
 * 3. Run: javac StreamsInterviewPractice.java && java StreamsInterviewPractice
 * 4. All tests should pass
 */

import java.util.*;
import java.util.stream.Collectors;

public class StreamsInterviewPractice {
    
    // Helper classes for complex scenarios
    static class Employee {
        String name;
        String department;
        int salary;
        int age;
        String city;
        
        public Employee(String name, String department, int salary, int age, String city) {
            this.name = name;
            this.department = department;
            this.salary = salary;
            this.age = age;
            this.city = city;
        }
        
        public String getName() { return name; }
        public String getDepartment() { return department; }
        public int getSalary() { return salary; }
        public int getAge() { return age; }
        public String getCity() { return city; }
        
        @Override
        public String toString() {
            return name + " (" + department + ", $" + salary + ")";
        }
    }
    
    static class Transaction {
        String userId;
        String product;
        double amount;
        String category;
        
        public Transaction(String userId, String product, double amount, String category) {
            this.userId = userId;
            this.product = product;
            this.amount = amount;
            this.category = category;
        }
        
        public String getUserId() { return userId; }
        public String getProduct() { return product; }
        public double getAmount() { return amount; }
        public String getCategory() { return category; }
    }
    
    static class Tweet {
        String username;
        String content;
        int likes;
        List<String> hashtags;
        
        public Tweet(String username, String content, int likes, List<String> hashtags) {
            this.username = username;
            this.content = content;
            this.likes = likes;
            this.hashtags = hashtags;
        }
        
        public String getUsername() { return username; }
        public String getContent() { return content; }
        public int getLikes() { return likes; }
        public List<String> getHashtags() { return hashtags; }
    }
    
    // ==================== WORD & STRING PROCESSING ====================
    
    /**
     * INTERVIEW Q1: Word Frequency Map
     * Problem: Given a list of sentences, create a frequency map of all words (case-insensitive)
     * Companies: Google, Amazon, Facebook
     * 
     * Example: ["Hello World", "hello java"] -> {hello=2, world=1, java=1}
     */
    public static Map<String, Long> wordFrequencyMap(List<String> sentences) {
        return  sentences.stream().
        flatMap(x->Arrays.stream(x.split(" ")))
        .map(x->x.toLowerCase())
        .collect(Collectors.groupingBy(x->x,Collectors.counting()));

    }
    
    /**
     * INTERVIEW Q2: Find Top K Frequent Words
     * Problem: Return the K most frequent words, sorted by frequency (descending), then alphabetically
     * Companies: Amazon, Bloomberg, Microsoft
     * 
     * Example: sentences=["the day is sunny the the sunny is is"], k=4 
     *          -> [the, is, sunny, day]
     */
    public static List<String> topKFrequentWords(List<String> sentences, int k) {
        // TODO: Create frequency map -> sort by count desc, then alphabetically -> take k
        Map<String,Long> hm=sentences.stream()
        .flatMap(x->Arrays.stream(x.split(" ")))
        .map(String::toLowerCase)
        .collect(Collectors.groupingBy(x->x,Collectors.counting()));

        PriorityQueue<String> pq=new PriorityQueue<>((x,y)->{
            long fc=hm.get(x).compareTo(hm.get(y));
            return fc==0 ? x.compareTo(y): (int) fc;
        });

        for(String x:hm.keySet()){
            pq.offer(x);
            if(pq.size()>k) pq.poll();
        }
        

        return null;
    }
    
    /**
     * INTERVIEW Q3: Find Longest Word in Sentences
     * Problem: Find the longest word across all sentences
     * Companies: Google, Apple
     */
    public static String findLongestWord(List<String> sentences) {
        // TODO: Split -> flatMap -> find max by length
        return null;
    }
    
    /**
     * INTERVIEW Q4: Group Anagrams
     * Problem: Group words that are anagrams of each other
     * Companies: Facebook, Amazon, Google
     * 
     * Example: ["eat","tea","tan","ate","nat","bat"] 
     *          -> [[eat,tea,ate], [tan,nat], [bat]]
     */
    public static Map<String, List<String>> groupAnagrams(List<String> words) {
        // TODO: Sort chars of each word -> use as key -> group by
        return null;
    }
    
    /**
     * INTERVIEW Q5: Remove Duplicate Characters, Keep Order
     * Problem: Remove duplicate characters from string, preserving first occurrence order
     * Companies: Microsoft, Amazon
     * 
     * Example: "programming" -> "progamin"
     */
    public static String removeDuplicateChars(String str) {
        // TODO: Stream chars -> distinct -> collect to string
        return null;
    }
    
    // ==================== EMPLOYEE/ANALYTICS PROBLEMS ====================
    
    /**
     * INTERVIEW Q6: Department-wise Average Salary
     * Problem: Calculate average salary for each department
     * Companies: Goldman Sachs, Morgan Stanley, Amazon
     */
    public static Map<String, Double> avgSalaryByDepartment(List<Employee> employees) {
        // TODO: groupingBy department -> averagingInt salary
        return null;
    }
    
    /**
     * INTERVIEW Q7: Top N Highest Paid Employees
     * Problem: Get the top N employees by salary
     * Companies: Google, Apple, Microsoft
     */
    public static List<Employee> topNSalaries(List<Employee> employees, int n) {
        // TODO: Sort by salary desc -> limit n
        return null;
    }
    
    /**
     * INTERVIEW Q8: Find Employees with Salary Above Department Average
     * Problem: For each department, find employees earning above the department average
     * Companies: Amazon, Capital One
     */
    public static List<Employee> aboveDepartmentAverage(List<Employee> employees) {
        // TODO: Calculate dept avg -> filter employees above their dept avg
        return null;
    }
    
    /**
     * INTERVIEW Q9: Department with Highest Total Salary
     * Problem: Find which department has the highest total salary expenditure
     * Companies: Bloomberg, Oracle
     */
    public static String departmentWithHighestSalary(List<Employee> employees) {
        // TODO: groupBy dept -> sum salaries -> find max
        return null;
    }
    
    /**
     * INTERVIEW Q10: Partition Employees by Age (Senior/Junior)
     * Problem: Partition employees into senior (age >= 40) and junior (age < 40)
     * Companies: Microsoft, SAP
     */
    public static Map<Boolean, List<Employee>> partitionByAge(List<Employee> employees, int threshold) {
        // TODO: partitioningBy age >= threshold
        return null;
    }
    
    // ==================== TRANSACTION/FINANCIAL PROBLEMS ====================
    
    /**
     * INTERVIEW Q11: Total Revenue by Category
     * Problem: Calculate total transaction amount for each product category
     * Companies: Stripe, PayPal, Square
     */
    public static Map<String, Double> revenueByCategory(List<Transaction> transactions) {
        // TODO: groupBy category -> sum amounts
        return null;
    }
    
    /**
     * INTERVIEW Q12: Top Spending Users
     * Problem: Find top N users who spent the most money
     * Companies: Amazon, eBay, Walmart
     */
    public static List<String> topSpendingUsers(List<Transaction> transactions, int n) {
        // TODO: groupBy userId -> sum -> sort desc -> take n
        return null;
    }
    
    /**
     * INTERVIEW Q13: Average Transaction Amount per User
     * Problem: Calculate average transaction amount for each user
     * Companies: Visa, Mastercard
     */
    public static Map<String, Double> avgTransactionPerUser(List<Transaction> transactions) {
        // TODO: groupBy userId -> average amount
        return null;
    }
    
    /**
     * INTERVIEW Q14: Find High-Value Transactions
     * Problem: Get all transactions above a threshold, sorted by amount descending
     * Companies: JP Morgan, Bank of America
     */
    public static List<Transaction> highValueTransactions(List<Transaction> transactions, double threshold) {
        // TODO: filter -> sort by amount desc
        return null;
    }
    
    // ==================== SOCIAL MEDIA/TRENDING PROBLEMS ====================
    
    /**
     * INTERVIEW Q15: Trending Hashtags (Top K)
     * Problem: Find top K trending hashtags from tweets
     * Companies: Twitter, Meta, LinkedIn
     */
    public static List<String> trendingHashtags(List<Tweet> tweets, int k) {
        // TODO: flatMap hashtags -> count frequency -> sort desc -> take k
        return null;
    }
    
    /**
     * INTERVIEW Q16: Most Active Users
     * Problem: Find users who posted the most tweets
     * Companies: Twitter, Instagram, TikTok
     */
    public static List<String> mostActiveUsers(List<Tweet> tweets, int n) {
        // TODO: groupBy username -> count -> sort desc -> take n
        return null;
    }
    
    /**
     * INTERVIEW Q17: Average Likes per User
     * Problem: Calculate average likes per tweet for each user
     * Companies: Meta, Reddit
     */
    public static Map<String, Double> avgLikesPerUser(List<Tweet> tweets) {
        // TODO: groupBy username -> average likes
        return null;
    }
    
    /**
     * INTERVIEW Q18: Find Viral Tweets (Top Liked)
     * Problem: Get tweets with likes above threshold, sorted by likes
     * Companies: Twitter, YouTube
     */
    public static List<Tweet> viralTweets(List<Tweet> tweets, int minLikes) {
        // TODO: filter -> sort by likes desc
        return null;
    }
    
    // ==================== COLLECTION MANIPULATION ====================
    
    /**
     * INTERVIEW Q19: Find Missing Numbers in Sequence
     * Problem: Given list of numbers, find all missing numbers in range [1, max]
     * Companies: Microsoft, Google, Adobe
     * 
     * Example: [1,3,5,6] -> [2, 4] (missing in range 1-6)
     */
    public static List<Integer> findMissingNumbers(List<Integer> numbers) {
        // TODO: Find max -> generate range -> filter not in original list
        return null;
    }
    
    /**
     * INTERVIEW Q20: Find Duplicates
     * Problem: Find all duplicate elements in a list
     * Companies: Apple, Amazon, Microsoft
     */
    public static List<Integer> findDuplicates(List<Integer> numbers) {
        // TODO: groupBy -> filter count > 1 -> collect keys
        return null;
    }
    
    /**
     * INTERVIEW Q21: Find First Non-Repeating Element
     * Problem: Find the first element that appears only once
     * Companies: Amazon, Oracle, Cisco
     */
    public static Integer firstNonRepeating(List<Integer> numbers) {
        // TODO: Create frequency map -> find first with count==1
        return null;
    }
    
    /**
     * INTERVIEW Q22: Merge Overlapping Intervals
     * Problem: Given intervals, merge overlapping ones
     * Companies: Google, Facebook, LinkedIn
     * 
     * Example: [[1,3],[2,6],[8,10],[15,18]] -> [[1,6],[8,10],[15,18]]
     */
    public static List<int[]> mergeIntervals(List<int[]> intervals) {
        // TODO: Sort by start -> reduce to merge overlapping
        return null;
    }
    
    /**
     * INTERVIEW Q23: Second Highest Salary
     * Problem: Find the second highest unique salary
     * Companies: Oracle, SAP, Salesforce
     */
    public static Integer secondHighestSalary(List<Employee> employees) {
        // TODO: distinct salaries -> sort desc -> skip 1 -> findFirst
        return null;
    }
    
    // ==================== STRING ARRAY PROBLEMS ====================
    
    /**
     * INTERVIEW Q24: Group Strings by Length
     * Problem: Group strings by their length
     * Companies: Google, Amazon
     */
    public static Map<Integer, List<String>> groupByLength(List<String> strings) {
        // TODO: groupingBy String::length
        return null;
    }
    
    /**
     * INTERVIEW Q25: Find Common Elements in N Lists
     * Problem: Find elements that appear in all lists
     * Companies: Microsoft, Adobe
     */
    public static List<Integer> findCommonInAllLists(List<List<Integer>> lists) {
        // TODO: Convert to sets -> reduce with intersection
        return null;
    }
    
    /**
     * INTERVIEW Q26: Capitalize First Letter of Each Word
     * Problem: Convert "hello world" to "Hello World"
     * Companies: Apple, Amazon
     */
    public static String capitalizeWords(String sentence) {
        // TODO: Split -> map capitalize first char -> join
        return null;
    }
    
    /**
     * INTERVIEW Q27: Reverse Each Word in Sentence
     * Problem: Reverse individual words, keep order: "hello world" -> "olleh dlrow"
     * Companies: Google, Meta
     */
    public static String reverseEachWord(String sentence) {
        // TODO: Split -> map reverse -> join
        return null;
    }
    
    /**
     * INTERVIEW Q28: Remove Vowels from String
     * Problem: Remove all vowels (case-insensitive) from string
     * Companies: Amazon, Microsoft
     */
    public static String removeVowels(String str) {
        // TODO: Filter chars -> exclude vowels
        return null;
    }
    
    // ==================== NUMBER/MATH PROBLEMS ====================
    
    /**
     * INTERVIEW Q29: Find Prime Numbers in Range
     * Problem: Find all prime numbers between start and end
     * Companies: Google, Amazon, Apple
     */
    public static List<Integer> findPrimes(int start, int end) {
        // TODO: Generate range -> filter isPrime
        return null;
    }
    
    /**
     * INTERVIEW Q30: Sum of Even Fibonacci Numbers
     * Problem: Generate first N Fibonacci numbers and sum the even ones
     * Companies: Facebook, Microsoft
     */
    public static int sumEvenFibonacci(int n) {
        // TODO: Generate fibonacci -> filter even -> sum
        return 0;
    }
    
    /**
     * INTERVIEW Q31: Squared Sum of Odd Numbers
     * Problem: In given list, square all odd numbers and sum them
     * Companies: Oracle, SAP
     */
    public static int squaredSumOfOdds(List<Integer> numbers) {
        // TODO: filter odd -> map square -> sum
        return 0;
    }
    
    /**
     * INTERVIEW Q32: Product of Non-Zero Elements
     * Problem: Calculate product of all non-zero elements
     * Companies: Apple, Adobe
     */
    public static int productOfNonZero(List<Integer> numbers) {
        // TODO: filter != 0 -> reduce multiply
        return 1;
    }
    
    // ==================== MAP/REDUCE PATTERNS ====================
    
    /**
     * INTERVIEW Q33: Student Grade Statistics
     * Problem: Given student names and scores, find highest, lowest, and average score
     * Companies: Educational Tech Companies, Oracle
     */
    public static Map<String, Double> gradeStatistics(Map<String, Integer> studentScores) {
        // TODO: Get values -> calculate max, min, avg -> return as map
        return null;
    }
    
    /**
     * INTERVIEW Q34: Convert List to Map (Index as Key)
     * Problem: Convert ["apple", "banana", "cherry"] -> {0=apple, 1=banana, 2=cherry}
     * Companies: Google, LinkedIn
     */
    public static Map<Integer, String> listToIndexMap(List<String> items) {
        // TODO: Use IntStream.range -> toMap
        return null;
    }
    
    /**
     * INTERVIEW Q35: Flatten Nested Map
     * Problem: Flatten Map<String, List<Integer>> to List<Integer> of all values
     * Companies: Amazon, Netflix
     */
    public static List<Integer> flattenMapValues(Map<String, List<Integer>> nestedMap) {
        // TODO: Stream map values -> flatMap -> collect
        return null;
    }
    
    /**
     * INTERVIEW Q36: Invert Map (Swap Keys and Values)
     * Problem: Convert {a=1, b=2} -> {1=a, 2=b}
     * Companies: Google, Facebook
     */
    public static Map<Integer, String> invertMap(Map<String, Integer> map) {
        // TODO: Stream entries -> toMap with swapped key/value
        return null;
    }
    
    // ==================== COMPLEX FILTERING & SORTING ====================
    
    /**
     * INTERVIEW Q37: Filter Palindrome Strings
     * Problem: Find all palindrome strings from list
     * Companies: Amazon, Google
     */
    public static List<String> filterPalindromes(List<String> strings) {
        // TODO: filter isPalindrome
        return null;
    }
    
    /**
     * INTERVIEW Q38: Sort by Multiple Criteria
     * Problem: Sort employees by department (asc), then salary (desc), then name (asc)
     * Companies: Oracle, SAP, Workday
     */
    public static List<Employee> sortEmployees(List<Employee> employees) {
        // TODO: Use Comparator.comparing().thenComparing()
        return null;
    }
    
    /**
     * INTERVIEW Q39: Find Elements Present in First List but Not in Second
     * Problem: Set difference operation
     * Companies: Microsoft, Amazon
     */
    public static List<Integer> listDifference(List<Integer> list1, List<Integer> list2) {
        // TODO: filter -> not in list2 -> distinct
        return null;
    }
    
    /**
     * INTERVIEW Q40: Find Symmetric Difference (A ⊕ B)
     * Problem: Elements in either list but not in both
     * Companies: Google, LinkedIn
     */
    public static List<Integer> symmetricDifference(List<Integer> list1, List<Integer> list2) {
        // TODO: Combine both differences
        return null;
    }
    
    // ==================== ADVANCED SCENARIOS ====================
    
    /**
     * INTERVIEW Q41: Group Employees by City and Department
     * Problem: Create nested grouping: Map<City, Map<Department, List<Employee>>>
     * Companies: Google, LinkedIn
     */
    public static Map<String, Map<String, List<Employee>>> groupByCityAndDepartment(List<Employee> employees) {
        // TODO: groupingBy city -> groupingBy department
        return null;
    }
    
    /**
     * INTERVIEW Q42: Find Nth Highest Salary
     * Problem: Find the Nth highest unique salary (generalized version of Q23)
     * Companies: Microsoft, Oracle
     */
    public static Integer nthHighestSalary(List<Employee> employees, int n) {
        // TODO: distinct salaries -> sort desc -> skip(n-1) -> findFirst
        return null;
    }
    
    /**
     * INTERVIEW Q43: Calculate Moving Average
     * Problem: Calculate moving average of window size k
     * Companies: Trading Firms, FinTech
     * 
     * Example: [1,2,3,4,5], k=3 -> [2.0, 3.0, 4.0] (avg of [1,2,3], [2,3,4], [3,4,5])
     */
    public static List<Double> movingAverage(List<Integer> numbers, int windowSize) {
        // TODO: Use IntStream with sliding window
        return null;
    }
    
    /**
     * INTERVIEW Q44: Compress Consecutive Duplicates
     * Problem: Compress consecutive duplicates: "aaabbccaa" -> "a3b2c2a2"
     * Companies: Amazon, Google
     */
    public static String compressString(String str) {
        // TODO: Group consecutive chars -> count -> format
        return null;
    }
    
    /**
     * INTERVIEW Q45: Find Elements with Max Frequency
     * Problem: Find all elements that appear with the maximum frequency
     * Companies: Microsoft, Facebook
     * 
     * Example: [1,2,2,3,3,3,4] -> [3] (appears 3 times, the max)
     */
    public static List<Integer> findMostFrequent(List<Integer> numbers) {
        // TODO: Create frequency map -> find max frequency -> filter by max
        return null;
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Helper: Check if number is prime
     */
    private static boolean isPrime(int n) {
        if (n <= 1) return false;
        if (n <= 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;
        for (int i = 5; i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) return false;
        }
        return true;
    }
    
    /**
     * Helper: Check if string is palindrome
     */
    private static boolean isPalindrome(String str) {
        String cleaned = str.toLowerCase().replaceAll("[^a-z0-9]", "");
        return cleaned.equals(new StringBuilder(cleaned).reverse().toString());
    }
    
    /**
     * Helper: Sort characters in string (for anagram detection)
     */
    private static String sortChars(String str) {
        return str.chars()
                  .sorted()
                  .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                  .toString();
    }
    
    // ==================== TEST CASES ====================
    
    public static void main(String[] args) {
        int totalTests = 0;
        int passedTests = 0;
        
        System.out.println("=".repeat(70));
        System.out.println("JAVA STREAMS INTERVIEW PRACTICE - TEST SUITE");
        System.out.println("Real-World Problems from Top Tech Companies");
        System.out.println("=".repeat(70));
        
        // ==================== WORD & STRING PROCESSING TESTS ====================
        System.out.println("\n=== WORD & STRING PROCESSING ===");
        
        // Test 1: wordFrequencyMap
        System.out.println("\n[Test 1] Word Frequency Map");
        Map<String, Long> freq = wordFrequencyMap(Arrays.asList("Hello World", "hello java", "world"));
        totalTests++; 
        if (freq != null && freq.get("hello") != null && freq.get("hello") == 2 && freq.get("world") == 2) {
            System.out.println("  ✓ PASS: Word frequency => " + freq);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Expected {hello=2, world=2, java=1}");
        }
        
        // Test 2: topKFrequentWords
        System.out.println("\n[Test 2] Top K Frequent Words");
        List<String> topWords = topKFrequentWords(
            Arrays.asList("the day is sunny the the sunny is is"), 4);
        totalTests++;
        if (topWords != null && topWords.size() == 4 && topWords.get(0).equals("the")) {
            System.out.println("  ✓ PASS: Top 4 frequent words => " + topWords);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Top K frequent words");
        }
        
        // Test 3: findLongestWord
        System.out.println("\n[Test 3] Find Longest Word");
        String longest = findLongestWord(Arrays.asList("short text", "this is a longer sentence", "ok"));
        totalTests++;
        if ("sentence".equals(longest)) {
            System.out.println("  ✓ PASS: Longest word => " + longest);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Expected 'sentence', got: " + longest);
        }
        
        // Test 4: groupAnagrams
        System.out.println("\n[Test 4] Group Anagrams");
        Map<String, List<String>> anagrams = groupAnagrams(
            Arrays.asList("eat", "tea", "tan", "ate", "nat", "bat"));
        totalTests++;
        if (anagrams != null && anagrams.size() == 3) {
            System.out.println("  ✓ PASS: Grouped into 3 anagram sets");
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Group anagrams");
        }
        
        // Test 5: removeDuplicateChars
        System.out.println("\n[Test 5] Remove Duplicate Characters");
        String unique = removeDuplicateChars("programming");
        totalTests++;
        if ("progamin".equals(unique)) {
            System.out.println("  ✓ PASS: Removed duplicates => " + unique);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Expected 'progamin', got: " + unique);
        }
        
        // ==================== EMPLOYEE ANALYTICS TESTS ====================
        System.out.println("\n=== EMPLOYEE ANALYTICS ===");
        
        List<Employee> employees = Arrays.asList(
            new Employee("Alice", "Engineering", 120000, 35, "NYC"),
            new Employee("Bob", "Engineering", 100000, 28, "SF"),
            new Employee("Charlie", "HR", 80000, 45, "NYC"),
            new Employee("David", "Engineering", 110000, 32, "SF"),
            new Employee("Eve", "HR", 75000, 38, "NYC"),
            new Employee("Frank", "Sales", 90000, 42, "LA")
        );
        
        // Test 6: avgSalaryByDepartment
        System.out.println("\n[Test 6] Avg Salary by Department");
        Map<String, Double> avgSalaries = avgSalaryByDepartment(employees);
        totalTests++;
        if (avgSalaries != null && avgSalaries.get("Engineering") != null) {
            System.out.println("  ✓ PASS: Engineering avg => $" + avgSalaries.get("Engineering"));
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Avg salary by department");
        }
        
        // Test 7: topNSalaries
        System.out.println("\n[Test 7] Top 3 Highest Paid");
        List<Employee> top3 = topNSalaries(employees, 3);
        totalTests++;
        if (top3 != null && top3.size() == 3 && top3.get(0).getName().equals("Alice")) {
            System.out.println("  ✓ PASS: Top 3 => " + top3);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Top N salaries");
        }
        
        // Test 8: aboveDepartmentAverage
        System.out.println("\n[Test 8] Above Department Average");
        List<Employee> above = aboveDepartmentAverage(employees);
        totalTests++;
        if (above != null && above.size() > 0) {
            System.out.println("  ✓ PASS: " + above.size() + " employees above dept avg");
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Above department average");
        }
        
        // Test 9: departmentWithHighestSalary
        System.out.println("\n[Test 9] Department with Highest Total Salary");
        String richDept = departmentWithHighestSalary(employees);
        totalTests++;
        if ("Engineering".equals(richDept)) {
            System.out.println("  ✓ PASS: Richest department => " + richDept);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Expected 'Engineering', got: " + richDept);
        }
        
        // Test 10: partitionByAge
        System.out.println("\n[Test 10] Partition by Age (40+)");
        Map<Boolean, List<Employee>> partitioned = partitionByAge(employees, 40);
        totalTests++;
        if (partitioned != null && partitioned.get(true) != null && partitioned.get(true).size() == 2) {
            System.out.println("  ✓ PASS: 2 seniors, " + partitioned.get(false).size() + " juniors");
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Partition by age");
        }
        
        // ==================== TRANSACTION TESTS ====================
        System.out.println("\n=== TRANSACTION/FINANCIAL ===");
        
        List<Transaction> transactions = Arrays.asList(
            new Transaction("user1", "Laptop", 1200.0, "Electronics"),
            new Transaction("user2", "Book", 25.0, "Books"),
            new Transaction("user1", "Mouse", 30.0, "Electronics"),
            new Transaction("user3", "Phone", 800.0, "Electronics"),
            new Transaction("user2", "Notebook", 15.0, "Books"),
            new Transaction("user1", "Keyboard", 100.0, "Electronics")
        );
        
        // Test 11: revenueByCategory
        System.out.println("\n[Test 11] Total Revenue by Category");
        Map<String, Double> revenue = revenueByCategory(transactions);
        totalTests++;
        if (revenue != null && revenue.get("Electronics") == 2130.0) {
            System.out.println("  ✓ PASS: Electronics revenue => $" + revenue.get("Electronics"));
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Revenue by category");
        }
        
        // Test 12: topSpendingUsers
        System.out.println("\n[Test 12] Top Spending Users");
        List<String> topSpenders = topSpendingUsers(transactions, 2);
        totalTests++;
        if (topSpenders != null && topSpenders.size() == 2 && topSpenders.get(0).equals("user1")) {
            System.out.println("  ✓ PASS: Top spender => " + topSpenders.get(0));
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Top spending users");
        }
        
        // Test 13: avgTransactionPerUser
        System.out.println("\n[Test 13] Avg Transaction per User");
        Map<String, Double> avgTrans = avgTransactionPerUser(transactions);
        totalTests++;
        if (avgTrans != null && avgTrans.get("user1") != null) {
            System.out.println("  ✓ PASS: user1 avg => $" + avgTrans.get("user1"));
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Avg transaction per user");
        }
        
        // Test 14: highValueTransactions
        System.out.println("\n[Test 14] High-Value Transactions (>$500)");
        List<Transaction> highValue = highValueTransactions(transactions, 500.0);
        totalTests++;
        if (highValue != null && highValue.size() == 2) {
            System.out.println("  ✓ PASS: Found 2 high-value transactions");
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: High-value transactions");
        }
        
        // ==================== SOCIAL MEDIA TESTS ====================
        System.out.println("\n=== SOCIAL MEDIA/TRENDING ===");
        
        List<Tweet> tweets = Arrays.asList(
            new Tweet("alice", "Hello #java #coding", 100, Arrays.asList("java", "coding")),
            new Tweet("bob", "Love #java #programming", 150, Arrays.asList("java", "programming")),
            new Tweet("alice", "Great day #coding", 80, Arrays.asList("coding")),
            new Tweet("charlie", "#java is awesome", 200, Arrays.asList("java")),
            new Tweet("bob", "Working on #algorithms", 50, Arrays.asList("algorithms"))
        );
        
        // Test 15: trendingHashtags
        System.out.println("\n[Test 15] Trending Hashtags (Top 3)");
        List<String> trending = trendingHashtags(tweets, 3);
        totalTests++;
        if (trending != null && trending.size() == 3 && trending.get(0).equals("java")) {
            System.out.println("  ✓ PASS: Top trending => " + trending);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Trending hashtags");
        }
        
        // Test 16: mostActiveUsers
        System.out.println("\n[Test 16] Most Active Users (Top 2)");
        List<String> activeUsers = mostActiveUsers(tweets, 2);
        totalTests++;
        if (activeUsers != null && activeUsers.size() == 2) {
            System.out.println("  ✓ PASS: Most active => " + activeUsers);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Most active users");
        }
        
        // Test 17: avgLikesPerUser
        System.out.println("\n[Test 17] Average Likes per User");
        Map<String, Double> avgLikes = avgLikesPerUser(tweets);
        totalTests++;
        if (avgLikes != null && avgLikes.get("alice") == 90.0) {
            System.out.println("  ✓ PASS: alice avg likes => " + avgLikes.get("alice"));
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Average likes per user");
        }
        
        // Test 18: viralTweets
        System.out.println("\n[Test 18] Viral Tweets (>100 likes)");
        List<Tweet> viral = viralTweets(tweets, 100);
        totalTests++;
        if (viral != null && viral.size() == 2) {
            System.out.println("  ✓ PASS: Found 2 viral tweets");
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Viral tweets");
        }
        
        // ==================== COLLECTION MANIPULATION TESTS ====================
        System.out.println("\n=== COLLECTION MANIPULATION ===");
        
        // Test 19: findMissingNumbers
        System.out.println("\n[Test 19] Find Missing Numbers");
        List<Integer> missing = findMissingNumbers(Arrays.asList(1, 3, 5, 6));
        totalTests++;
        if (missing != null && missing.equals(Arrays.asList(2, 4))) {
            System.out.println("  ✓ PASS: Missing numbers => " + missing);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Find missing numbers");
        }
        
        // Test 20: findDuplicates
        System.out.println("\n[Test 20] Find Duplicates");
        List<Integer> dups = findDuplicates(Arrays.asList(1, 2, 3, 2, 4, 5, 1, 6));
        totalTests++;
        if (dups != null && dups.size() == 2) {
            System.out.println("  ✓ PASS: Duplicates => " + dups);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Find duplicates");
        }
        
        // Test 21: firstNonRepeating
        System.out.println("\n[Test 21] First Non-Repeating Element");
        Integer firstNon = firstNonRepeating(Arrays.asList(4, 5, 1, 2, 5, 4, 3));
        totalTests++;
        if (firstNon != null && firstNon == 1) {
            System.out.println("  ✓ PASS: First non-repeating => " + firstNon);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Expected 1, got: " + firstNon);
        }
        
        // Test 23: secondHighestSalary
        System.out.println("\n[Test 23] Second Highest Salary");
        Integer secondHighest = secondHighestSalary(employees);
        totalTests++;
        if (secondHighest != null && secondHighest == 110000) {
            System.out.println("  ✓ PASS: 2nd highest => $" + secondHighest);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Second highest salary");
        }
        
        // ==================== STRING ARRAY TESTS ====================
        System.out.println("\n=== STRING ARRAY PROBLEMS ===");
        
        // Test 24: groupByLength
        System.out.println("\n[Test 24] Group Strings by Length");
        Map<Integer, List<String>> grouped = groupByLength(Arrays.asList("a", "bb", "ccc", "dd"));
        totalTests++;
        if (grouped != null && grouped.get(2).size() == 2) {
            System.out.println("  ✓ PASS: Grouped by length");
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Group by length");
        }
        
        // Test 25: findCommonInAllLists
        System.out.println("\n[Test 25] Find Common in All Lists");
        List<Integer> common = findCommonInAllLists(Arrays.asList(
            Arrays.asList(1, 2, 3, 4),
            Arrays.asList(2, 3, 4, 5),
            Arrays.asList(3, 4, 5, 6)
        ));
        totalTests++;
        if (common != null && common.size() == 2) {
            System.out.println("  ✓ PASS: Common elements => " + common);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Common elements");
        }
        
        // Test 26: capitalizeWords
        System.out.println("\n[Test 26] Capitalize Words");
        String capitalized = capitalizeWords("hello world from java");
        totalTests++;
        if ("Hello World From Java".equals(capitalized)) {
            System.out.println("  ✓ PASS: Capitalized => " + capitalized);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Expected 'Hello World From Java'");
        }
        
        // Test 27: reverseEachWord
        System.out.println("\n[Test 27] Reverse Each Word");
        String reversed = reverseEachWord("hello world");
        totalTests++;
        if ("olleh dlrow".equals(reversed)) {
            System.out.println("  ✓ PASS: Reversed => " + reversed);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Expected 'olleh dlrow'");
        }
        
        // Test 28: removeVowels
        System.out.println("\n[Test 28] Remove Vowels");
        String noVowels = removeVowels("Hello World");
        totalTests++;
        if ("Hll Wrld".equals(noVowels)) {
            System.out.println("  ✓ PASS: No vowels => " + noVowels);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Expected 'Hll Wrld', got: " + noVowels);
        }
        
        // ==================== NUMBER/MATH TESTS ====================
        System.out.println("\n=== NUMBER/MATH PROBLEMS ===");
        
        // Test 29: findPrimes
        System.out.println("\n[Test 29] Find Primes in Range [10, 30]");
        List<Integer> primes = findPrimes(10, 30);
        totalTests++;
        if (primes != null && primes.equals(Arrays.asList(11, 13, 17, 19, 23, 29))) {
            System.out.println("  ✓ PASS: Primes => " + primes);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Find primes");
        }
        
        // Test 30: sumEvenFibonacci
        System.out.println("\n[Test 30] Sum of Even Fibonacci Numbers");
        int fibSum = sumEvenFibonacci(10);
        totalTests++;
        if (fibSum == 44) { // 2+8+34 = 44
            System.out.println("  ✓ PASS: Sum of even Fibonacci => " + fibSum);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Expected 44, got: " + fibSum);
        }
        
        // Test 31: squaredSumOfOdds
        System.out.println("\n[Test 31] Squared Sum of Odds");
        int oddSquares = squaredSumOfOdds(Arrays.asList(1, 2, 3, 4, 5));
        totalTests++;
        if (oddSquares == 35) { // 1+9+25 = 35
            System.out.println("  ✓ PASS: Squared sum of odds => " + oddSquares);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Expected 35, got: " + oddSquares);
        }
        
        // Test 32: productOfNonZero
        System.out.println("\n[Test 32] Product of Non-Zero");
        int product = productOfNonZero(Arrays.asList(2, 0, 3, 0, 4));
        totalTests++;
        if (product == 24) { // 2*3*4 = 24
            System.out.println("  ✓ PASS: Product of non-zero => " + product);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Expected 24, got: " + product);
        }
        
        // ==================== MAP/REDUCE TESTS ====================
        System.out.println("\n=== MAP/REDUCE PATTERNS ===");
        
        // Test 33: gradeStatistics
        System.out.println("\n[Test 33] Grade Statistics");
        Map<String, Integer> grades = new HashMap<>();
        grades.put("Alice", 85);
        grades.put("Bob", 92);
        grades.put("Charlie", 78);
        Map<String, Double> stats = gradeStatistics(grades);
        totalTests++;
        if (stats != null && stats.get("max") == 92.0) {
            System.out.println("  ✓ PASS: Max grade => " + stats.get("max"));
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Grade statistics");
        }
        
        // Test 34: listToIndexMap
        System.out.println("\n[Test 34] List to Index Map");
        Map<Integer, String> indexMap = listToIndexMap(Arrays.asList("apple", "banana", "cherry"));
        totalTests++;
        if (indexMap != null && "banana".equals(indexMap.get(1))) {
            System.out.println("  ✓ PASS: Index map => " + indexMap);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: List to index map");
        }
        
        // Test 35: flattenMapValues
        System.out.println("\n[Test 35] Flatten Nested Map");
        Map<String, List<Integer>> nested = new HashMap<>();
        nested.put("A", Arrays.asList(1, 2));
        nested.put("B", Arrays.asList(3, 4));
        List<Integer> flattened = flattenMapValues(nested);
        totalTests++;
        if (flattened != null && flattened.size() == 4) {
            System.out.println("  ✓ PASS: Flattened => " + flattened);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Flatten map values");
        }
        
        // Test 36: invertMap
        System.out.println("\n[Test 36] Invert Map");
        Map<String, Integer> original = new HashMap<>();
        original.put("a", 1);
        original.put("b", 2);
        Map<Integer, String> inverted = invertMap(original);
        totalTests++;
        if (inverted != null && "b".equals(inverted.get(2))) {
            System.out.println("  ✓ PASS: Inverted map => " + inverted);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Invert map");
        }
        
        // ==================== COMPLEX FILTERING TESTS ====================
        System.out.println("\n=== COMPLEX FILTERING & SORTING ===");
        
        // Test 37: filterPalindromes
        System.out.println("\n[Test 37] Filter Palindromes");
        List<String> palindromes = filterPalindromes(
            Arrays.asList("racecar", "hello", "level", "world", "noon"));
        totalTests++;
        if (palindromes != null && palindromes.size() == 3) {
            System.out.println("  ✓ PASS: Palindromes => " + palindromes);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Filter palindromes");
        }
        
        // Test 38: sortEmployees
        System.out.println("\n[Test 38] Sort by Multiple Criteria");
        List<Employee> sorted = sortEmployees(employees);
        totalTests++;
        if (sorted != null && sorted.get(0).getDepartment().equals("Engineering")) {
            System.out.println("  ✓ PASS: Multi-criteria sort completed");
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Sort employees");
        }
        
        // Test 39: listDifference
        System.out.println("\n[Test 39] List Difference (A - B)");
        List<Integer> diff = listDifference(
            Arrays.asList(1, 2, 3, 4, 5),
            Arrays.asList(3, 4, 5, 6, 7));
        totalTests++;
        if (diff != null && diff.equals(Arrays.asList(1, 2))) {
            System.out.println("  ✓ PASS: Difference => " + diff);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: List difference");
        }
        
        // Test 40: symmetricDifference
        System.out.println("\n[Test 40] Symmetric Difference (A ⊕ B)");
        List<Integer> symDiff = symmetricDifference(
            Arrays.asList(1, 2, 3, 4),
            Arrays.asList(3, 4, 5, 6));
        totalTests++;
        if (symDiff != null && symDiff.size() == 4) {
            System.out.println("  ✓ PASS: Symmetric diff => " + symDiff);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Symmetric difference");
        }
        
        // ==================== ADVANCED SCENARIOS TESTS ====================
        System.out.println("\n=== ADVANCED SCENARIOS ===");
        
        // Test 41: groupByCityAndDepartment
        System.out.println("\n[Test 41] Nested Grouping (City -> Department)");
        Map<String, Map<String, List<Employee>>> nestedGroup = groupByCityAndDepartment(employees);
        totalTests++;
        if (nestedGroup != null && nestedGroup.get("NYC") != null) {
            System.out.println("  ✓ PASS: Nested grouping completed");
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Nested grouping");
        }
        
        // Test 42: nthHighestSalary
        System.out.println("\n[Test 42] 3rd Highest Salary");
        Integer thirdHighest = nthHighestSalary(employees, 3);
        totalTests++;
        if (thirdHighest != null && thirdHighest == 90000) {
            System.out.println("  ✓ PASS: 3rd highest => $" + thirdHighest);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Nth highest salary");
        }
        
        // Test 45: findMostFrequent
        System.out.println("\n[Test 45] Find Most Frequent Elements");
        List<Integer> mostFreq = findMostFrequent(Arrays.asList(1, 2, 2, 3, 3, 3, 4));
        totalTests++;
        if (mostFreq != null && mostFreq.equals(Arrays.asList(3))) {
            System.out.println("  ✓ PASS: Most frequent => " + mostFreq);
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Most frequent elements");
        }
        
        // Print final results
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST RESULTS");
        System.out.println("=".repeat(70));
        System.out.println("Total Tests: " + totalTests);
        System.out.println("Passed: " + passedTests);
        System.out.println("Failed: " + (totalTests - passedTests));
        System.out.println("Success Rate: " + String.format("%.2f", (passedTests * 100.0 / totalTests)) + "%");
        System.out.println("=".repeat(70));
        
        if (passedTests == totalTests) {
            System.out.println("\n🎉 CONGRATULATIONS! All tests passed! 🎉");
            System.out.println("You're ready for FAANG interviews!");
        } else {
            System.out.println("\n⚠️  Keep practicing! Review the failed tests.");
            System.out.println("💡 Tip: Focus on combining multiple stream operations");
        }
        
        System.out.println("\n📚 Key Interview Topics Covered:");
        System.out.println("  ✓ Word frequency & text processing");
        System.out.println("  ✓ Employee analytics & aggregations");
        System.out.println("  ✓ Financial transactions & revenue analysis");
        System.out.println("  ✓ Social media trends & engagement metrics");
        System.out.println("  ✓ Collection manipulation & set operations");
        System.out.println("  ✓ Complex grouping & nested collectors");
    }
}
