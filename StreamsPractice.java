/**
 * StreamsPractice.java
 * 
 * A comprehensive Java practice file for mastering Java Streams API.
 * Complete all the TODO methods below and run the main method to test your solutions.
 * 
 * Instructions:
 * 1. Read each method's description carefully
 * 2. Replace the "TODO: Implement this method" with your solution
 * 3. Run: javac StreamsPractice.java && java StreamsPractice
 * 4. Check if all test cases pass
 */

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class StreamsPractice {
    
    // ==================== BASIC STREAM OPERATIONS ====================
    
    /**
     * Task 1: Filter even numbers from a list
     * @param numbers List of integers
     * @return List of even numbers
     */
    public static List<Integer> filterEvenNumbers(List<Integer> numbers) {
        // TODO: Use stream().filter().collect()
        return numbers.stream().filter(x->x%2==0).collect(Collectors.toList());
    }
    
    /**
     * Task 2: Convert all strings to uppercase
     * @param strings List of strings
     * @return List of uppercase strings
     */
    public static List<String> toUpperCase(List<String> strings) {
        // TODO: Use stream().map().collect()
        return strings.stream().map(x->x.toUpperCase()).collect(Collectors.toList());
    }
    
    /**
     * Task 3: Get the sum of all numbers
     * @param numbers List of integers
     * @return Sum of all numbers
     */
    public static int sum(List<Integer> numbers) {
        // TODO: Use stream().mapToInt().sum() or reduce()
        
        return numbers.stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * Task 4: Count elements in the list
     * @param items List of items
     * @return Count of elements
     */
    public static long countElements(List<String> items) {
        // TODO: Use stream().count()
        
        return items.stream().count();
    }
    
    /**
     * Task 5: Get maximum value from list
     * @param numbers List of integers
     * @return Maximum value (or null if empty)
     */
    public static Integer findMax(List<Integer> numbers) {
        // TODO: Use stream().max()
        
        return numbers.stream().max(Comparator.naturalOrder()).orElseThrow();
    }
    
    // ==================== INTERMEDIATE OPERATIONS ====================
    
    /**
     * Task 6: Remove duplicates from list
     * @param items List with duplicates
     * @return List without duplicates
     */
    public static List<Integer> removeDuplicates(List<Integer> items) {
        // TODO: Use stream().distinct().collect()
        
        return items.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Task 7: Sort list in ascending order
     * @param numbers List of integers
     * @return Sorted list
     */
    public static List<Integer> sortAscending(List<Integer> numbers) {
        // TODO: Use stream().sorted().collect()
        return numbers.stream().sorted().collect(Collectors.toList());
    }
    
    /**
     * Task 8: Get first N elements
     * @param items List of items
     * @param n Number of elements to get
     * @return List of first n elements
     */
    public static List<String> getFirstN(List<String> items, int n) {
        // TODO: Use stream().limit(n).collect()
        return items.stream().limit(n).collect(Collectors.toList());
    }
    
    /**
     * Task 9: Skip first N elements
     * @param items List of items
     * @param n Number of elements to skip
     * @return List after skipping n elements
     */
    public static List<String> skipFirstN(List<String> items, int n) {
        // TODO: Use stream().skip(n).collect()
        return items.stream().skip(n).collect(Collectors.toList());
    }
    
    /**
     * Task 10: Check if any element matches condition
     * @param numbers List of integers
     * @param threshold Threshold value
     * @return true if any number > threshold
     */
    public static boolean anyGreaterThan(List<Integer> numbers, int threshold) {
        // TODO: Use stream().anyMatch()
        return numbers.stream().anyMatch(x-> x>threshold);
    }
    
    // ==================== MAPPING OPERATIONS ====================
    
    /**
     * Task 11: Get lengths of all strings
     * @param strings List of strings
     * @return List of string lengths
     */
    public static List<Integer> getStringLengths(List<String> strings) {
        // TODO: Use stream().map(String::length).collect()
        return strings.stream().map(String::length).collect(Collectors.toList());
    }
    
    /**
     * Task 12: Square all numbers
     * @param numbers List of integers
     * @return List of squared numbers
     */
    public static List<Integer> squareNumbers(List<Integer> numbers) {
        // TODO: Use stream().map(n -> n * n).collect()
        return numbers.stream().map(x->x*x).collect(Collectors.toList());
    }
    
    /**
     * Task 13: Extract first character from each string
     * @param strings List of strings
     * @return List of first characters
     */
    public static List<Character> getFirstCharacters(List<String> strings) {
        // TODO: Use stream().map().collect()
        return strings.stream().map(x->x.charAt(0)).collect(Collectors.toList());
    }
    
    /**
     * Task 14: Flatten list of lists
     * @param listOfLists List of integer lists
     * @return Flattened list
     */
    public static List<Integer> flattenLists(List<List<Integer>> listOfLists) {
        // TODO: Use stream().flatMap().collect()
        return listOfLists.stream().flatMap(List::stream).collect(Collectors.toList());
    }
    
    /**
     * Task 15: Split strings and flatten
     * @param strings List of comma-separated strings
     * @return List of all individual words
     */
    public static List<String> splitAndFlatten(List<String> strings) {
        // TODO: Use stream().flatMap(s -> Arrays.stream(s.split(","))).collect()
        return strings.stream().flatMap(x -> Arrays.stream(x.split(","))).collect(Collectors.toList());
    }
    
    // ==================== FILTERING OPERATIONS ====================
    
    /**
     * Task 16: Filter strings by length
     * @param strings List of strings
     * @param minLength Minimum length
     * @return Strings with length >= minLength
     */
    public static List<String> filterByLength(List<String> strings, int minLength) {
        // TODO: Use stream().filter().collect()
        return strings.stream().filter(x->x.length()>=minLength).collect(Collectors.toList());
    }
    
    /**
     * Task 17: Filter strings starting with prefix
     * @param strings List of strings
     * @param prefix Prefix to match
     * @return Strings starting with prefix
     */
    public static List<String> filterByPrefix(List<String> strings, String prefix) {
        // TODO: Use stream().filter(s -> s.startsWith(prefix)).collect()
        return strings.stream().filter(x->x.startsWith(prefix)).collect(Collectors.toList());
    }
    
    /**
     * Task 18: Filter numbers in range
     * @param numbers List of integers
     * @param min Minimum value (inclusive)
     * @param max Maximum value (inclusive)
     * @return Numbers in range [min, max]
     */
    public static List<Integer> filterInRange(List<Integer> numbers, int min, int max) {
        // TODO: Use stream().filter().collect()
        return numbers.stream().filter(x-> x>=min && x<=max).collect(Collectors.toList());
    }
    
    /**
     * Task 19: Check if all elements match condition
     * @param numbers List of integers
     * @return true if all numbers are positive
     */
    public static boolean allPositive(List<Integer> numbers) {
        // TODO: Use stream().allMatch(n -> n > 0)
        
        return numbers.stream().allMatch(n-> n>0);
    }
    
    /**
     * Task 20: Check if none match condition
     * @param numbers List of integers
     * @return true if no numbers are negative
     */
    public static boolean noneNegative(List<Integer> numbers) {
        return numbers.stream().noneMatch(n -> n<0);
    }
    
    // ==================== REDUCTION OPERATIONS ====================
    
    /**
     * Task 21: Calculate product of all numbers
     * @param numbers List of integers
     * @return Product of all numbers
     */
    public static int product(List<Integer> numbers) {
        // TODO: Use stream().reduce(1, (a, b) -> a * b)
        return numbers.stream().reduce(1, (x,y)->x*y);
    }
    
    /**
     * Task 22: Concatenate all strings
     * @param strings List of strings
     * @return Concatenated string
     */
    public static String concatenate(List<String> strings) {
        // TODO: Use stream().reduce("", (a, b) -> a + b)
        return strings.stream().reduce("", (x,y)->x+y);
    }
    
    /**
     * Task 23: Join strings with delimiter
     * @param strings List of strings
     * @param delimiter Delimiter
     * @return Joined string
     */
    public static String joinWithDelimiter(List<String> strings, String delimiter) {
        // TODO: Use Collectors.joining(delimiter)
        return strings.stream().collect(Collectors.joining(delimiter));
    }
    
    /**
     * Task 24: Get average of numbers
     * @param numbers List of integers
     * @return Average value
     */
    public static double average(List<Integer> numbers) {
        // TODO: Use stream().mapToInt().average().orElse(0.0)
        return numbers.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }
    
    /**
     * Task 25: Find minimum value
     * @param numbers List of integers
     * @return Minimum value (or null if empty)
     */
    public static Integer findMin(List<Integer> numbers) {
        // TODO: Use stream().min()
        return numbers.stream().min(Comparator.naturalOrder()).get();
    }
    
    // ==================== COLLECTORS ====================
    
    /**
     * Task 26: Convert list to set
     * @param items List with duplicates
     * @return Set without duplicates
     */
    public static Set<Integer> toSet(List<Integer> items) {
        // TODO: Use stream().collect(Collectors.toSet())
        return items.stream().collect(Collectors.toSet());
    }
    
    /**
     * Task 27: Group by string length
     * @param strings List of strings
     * @return Map with length as key and list of strings as value
     */
    public static Map<Integer, List<String>> groupByLength(List<String> strings) {
        // TODO: Use Collectors.groupingBy(String::length)
        return strings.stream().collect(Collectors.groupingBy(String::length));
    }
    
    /**
     * Task 28: Partition by condition (even/odd)
     * @param numbers List of integers
     * @return Map with true for even, false for odd
     */
    public static Map<Boolean, List<Integer>> partitionByEven(List<Integer> numbers) {
        // TODO: Use Collectors.partitioningBy(n -> n % 2 == 0)
        return numbers.stream().collect(Collectors.partitioningBy(x->x%2==0));
    }
    
    /**
     * Task 29: Count occurrences of each element
     * @param items List of items
     * @return Map with item as key and count as value
     */
    public static Map<String, Long> countOccurrences(List<String> items) {
        // TODO: Use Collectors.groupingBy(Function.identity(), Collectors.counting())
        return items.stream().collect(Collectors.groupingBy(Function.identity(),Collectors.counting()));
    }
    
    /**
     * Task 30: Create map from list (name -> length)
     * @param strings List of strings
     * @return Map with string as key and length as value
     */
    public static Map<String, Integer> stringToLengthMap(List<String> strings) {
        // TODO: Use Collectors.toMap(s -> s, String::length)
        return strings.stream().collect(Collectors.toMap(x -> x,String::length));
    }
    
    // ==================== NUMERIC STREAMS ====================
    
    /**
     * Task 31: Generate range of numbers
     * @param start Start value (inclusive)
     * @param end End value (exclusive)
     * @return List of numbers in range
     */
    public static List<Integer> generateRange(int start, int end) {
        // TODO: Use IntStream.range(start, end).boxed().collect()
        return IntStream.range(start, end).boxed().collect(Collectors.toList());
    }
    
    /**
     * Task 32: Sum of squares of first N natural numbers
     * @param n The count
     * @return Sum of squares
     */
    public static int sumOfSquares(int n) {
        // TODO: Use IntStream.rangeClosed(1, n).map(x -> x * x).sum()
        return IntStream.rangeClosed(1, n).map(x->x*x).sum();
    }
    
    /**
     * Task 33: Get sum of even numbers in range
     * @param start Start value
     * @param end End value
     * @return Sum of even numbers
     */
    public static int sumOfEvens(int start, int end) {
        // TODO: Use IntStream.rangeClosed().filter().sum()
        return IntStream.rangeClosed(start, end).filter(x->x%2==0).sum();
    }
    
    /**
     * Task 34: Generate list of N random numbers
     * @param n Count of numbers
     * @param bound Upper bound (exclusive)
     * @return List of random numbers
     */
    public static List<Integer> generateRandomNumbers(int n, int bound) {
        // TODO: Use new Random().ints(n, 0, bound).boxed().collect()
        return new Random().ints(n,0,bound).boxed().collect(Collectors.toList());
    }
    
    /**
     * Task 35: Calculate factorial using streams
     * @param n The number
     * @return Factorial of n
     */
    public static long factorial(int n) {
        // TODO: Use LongStream.rangeClosed(1, n).reduce(1, (a, b) -> a * b)
        return LongStream.rangeClosed(1, n).reduce(1, (x,y)->x*y);
    }
    
    // ==================== ADVANCED OPERATIONS ====================
    
    /**
     * Task 36: Get top N elements
     * @param numbers List of integers
     * @param n Count
     * @return Top n largest numbers
     */
    public static List<Integer> getTopN(List<Integer> numbers, int n) {
        // TODO: Use stream().sorted(Comparator.reverseOrder()).limit(n).collect()
        return numbers.stream().sorted(Comparator.reverseOrder()).limit(n).collect(Collectors.toList());
    }
    
    /**
     * Task 37: Get distinct sorted elements
     * @param numbers List of integers
     * @return Distinct sorted list
     */
    public static List<Integer> distinctSorted(List<Integer> numbers) {
        // TODO: Use stream().distinct().sorted().collect()
        return numbers.stream().distinct().sorted().collect(Collectors.toList());
    }
    
    /**
     * Task 38: Find first element matching condition
     * @param numbers List of integers
     * @param threshold Threshold value
     * @return First number > threshold (or null)
     */
    public static Integer findFirstGreaterThan(List<Integer> numbers, int threshold) {
        // TODO: Use stream().filter().findFirst().orElse(null)
        return numbers.stream().filter(x-> x>threshold).findFirst().orElse(null);
    }
    
    /**
     * Task 39: Check if list contains element
     * @param items List of strings
     * @param target Target string
     * @return true if contains target
     */
    public static boolean contains(List<String> items, String target) {
        // TODO: Use stream().anyMatch(s -> s.equals(target))
        return items.stream().anyMatch(x->x.equals(target));
    }
    
    /**
     * Task 40: Reverse list using streams
     * @param items List of items
     * @return Reversed list
     */
    public static List<String> reverseList(List<String> items) {
        // TODO: Create stream, collect to list, then reverse using Collections.reverse()
        // Or use IntStream to iterate backwards
        Collections.reverse(items);
        return items;
    }
    
    // ==================== STRING OPERATIONS ====================
    
    /**
     * Task 41: Get unique characters from strings
     * @param strings List of strings
     * @return Set of unique characters
     */
    public static Set<Character> uniqueCharacters(List<String> strings) {
        // TODO: Use flatMap with chars(), map to Character, collect to Set
        return strings.stream().flatMap(x->x.chars().mapToObj(c->(char)c)).collect(Collectors.toSet());
    }
    
    /**
     * Task 42: Filter empty strings
     * @param strings List of strings
     * @return List without empty strings
     */
    public static List<String> filterNonEmpty(List<String> strings) {
        // TODO: Use stream().filter(s -> !s.isEmpty()).collect()
        return strings.stream().filter(x->!x.isEmpty()).collect(Collectors.toList());
    }
    
    /**
     * Task 43: Convert to comma-separated string
     * @param items List of items
     * @return Comma-separated string
     */
    public static String toCommaSeparated(List<String> items) {
        // TODO: Use Collectors.joining(", ")
        return items.stream().collect(Collectors.joining(", "));
    }
    
    /**
     * Task 44: Count words in all strings
     * @param strings List of strings
     * @return Total word count
     */
    public static long countWords(List<String> strings) {
        // TODO: Use flatMap to split strings, then count
        return strings.stream().flatMap(x->Arrays.stream(x.split(" "))).count();
    }
    
    /**
     * Task 45: Get longest string
     * @param strings List of strings
     * @return Longest string (or null if empty)
     */
    public static String getLongest(List<String> strings) {
        // TODO: Use stream().max(Comparator.comparingInt(String::length))
        return strings.stream().max(Comparator.comparingInt(String::length)).get();
    }
    
    // ==================== COMPLEX OPERATIONS ====================
    
    /**
     * Task 46: Group and sum by category
     * @param items List of items (format: "category:value")
     * @return Map with category as key and sum of values
     */
    public static Map<String, Integer> groupAndSum(List<String> items) {
        // TODO: Split by ":", group by category, sum values
        return items.stream().map(x->x.split(":")).collect(Collectors.groupingBy(x->x[0],Collectors.summingInt(x->Integer.parseInt(x[1]))));
    }
    
    /**
     * Task 47: Get statistics (min, max, average, sum, count)
     * @param numbers List of integers
     * @return IntSummaryStatistics
     */
    public static IntSummaryStatistics getStatistics(List<Integer> numbers) {
        // TODO: Use stream().mapToInt().summaryStatistics()
        return numbers.stream().mapToInt(Integer::intValue).summaryStatistics();
    }
    
    /**
     * Task 48: Find common elements in two lists
     * @param list1 First list
     * @param list2 Second list
     * @return List of common elements
     */
    public static List<Integer> findCommon(List<Integer> list1, List<Integer> list2) {
        // TODO: Use stream().filter(list2::contains).distinct().collect()
        return list1.stream().filter(list2::contains).distinct().collect(Collectors.toList());
    }
    
    /**
     * Task 49: Sort strings by length then alphabetically
     * @param strings List of strings
     * @return Sorted list
     */
    public static List<String> sortByLengthThenAlpha(List<String> strings) {
        // TODO: Use Comparator.comparingInt(String::length).thenComparing(String::compareTo)
        return strings.stream().sorted(Comparator.comparingInt(String::length).thenComparing(String::compareTo)).collect(Collectors.toList());
    }
    
    /**
     * Task 50: Custom stream pipeline
     * Filter even numbers, square them, get distinct values, sort descending, take top 3
     * @param numbers List of integers
     * @return Processed list
     */
    public static List<Integer> customPipeline(List<Integer> numbers) {
        // TODO: Implement the described pipeline
        return numbers.stream().filter(x->x%2==0).map(x->x*x).distinct().sorted(Comparator.reverseOrder()).limit(3).collect(Collectors.toList());
    }
    
    // ==================== TEST CASES ====================
    
    public static void main(String[] args) {
        int totalTests = 0;
        int passedTests = 0;
        
        System.out.println("=".repeat(60));
        System.out.println("JAVA STREAMS PRACTICE - TEST SUITE");
        System.out.println("=".repeat(60));
        
        // Test 1: filterEvenNumbers
        System.out.println("\n[Test 1] filterEvenNumbers");
        List<Integer> result1 = filterEvenNumbers(Arrays.asList(1, 2, 3, 4, 5, 6));
        totalTests++; if (testList(result1, Arrays.asList(2, 4, 6), "Filter even numbers")) passedTests++;
        
        // Test 2: toUpperCase
        System.out.println("\n[Test 2] toUpperCase");
        List<String> result2 = toUpperCase(Arrays.asList("hello", "world"));
        totalTests++; if (testStringList(result2, Arrays.asList("HELLO", "WORLD"), "To uppercase")) passedTests++;
        
        // Test 3: sum
        System.out.println("\n[Test 3] sum");
        int result3 = sum(Arrays.asList(1, 2, 3, 4, 5));
        totalTests++; if (testInt(result3, 15, "Sum of numbers")) passedTests++;
        
        // Test 4: countElements
        System.out.println("\n[Test 4] countElements");
        long result4 = countElements(Arrays.asList("a", "b", "c", "d"));
        totalTests++; if (testLong(result4, 4L, "Count elements")) passedTests++;
        
        // Test 5: findMax
        System.out.println("\n[Test 5] findMax");
        Integer result5 = findMax(Arrays.asList(3, 7, 2, 9, 1));
        totalTests++; if (testInteger(result5, 9, "Find max")) passedTests++;
        
        // Test 6: removeDuplicates
        System.out.println("\n[Test 6] removeDuplicates");
        List<Integer> result6 = removeDuplicates(Arrays.asList(1, 2, 2, 3, 1, 4));
        totalTests++; if (testSet(new HashSet<>(result6), new HashSet<>(Arrays.asList(1, 2, 3, 4)), "Remove duplicates")) passedTests++;
        
        // Test 7: sortAscending
        System.out.println("\n[Test 7] sortAscending");
        List<Integer> result7 = sortAscending(Arrays.asList(5, 2, 8, 1, 9));
        totalTests++; if (testList(result7, Arrays.asList(1, 2, 5, 8, 9), "Sort ascending")) passedTests++;
        
        // Test 8: getFirstN
        System.out.println("\n[Test 8] getFirstN");
        List<String> result8 = getFirstN(Arrays.asList("a", "b", "c", "d", "e"), 3);
        totalTests++; if (testStringList(result8, Arrays.asList("a", "b", "c"), "Get first 3")) passedTests++;
        
        // Test 9: skipFirstN
        System.out.println("\n[Test 9] skipFirstN");
        List<String> result9 = skipFirstN(Arrays.asList("a", "b", "c", "d", "e"), 2);
        totalTests++; if (testStringList(result9, Arrays.asList("c", "d", "e"), "Skip first 2")) passedTests++;
        
        // Test 10: anyGreaterThan
        System.out.println("\n[Test 10] anyGreaterThan");
        boolean result10 = anyGreaterThan(Arrays.asList(1, 2, 3, 4, 5), 4);
        totalTests++; if (testBoolean(result10, true, "Any greater than 4")) passedTests++;
        
        // Test 11: getStringLengths
        System.out.println("\n[Test 11] getStringLengths");
        List<Integer> result11 = getStringLengths(Arrays.asList("hi", "hello", "hey"));
        totalTests++; if (testList(result11, Arrays.asList(2, 5, 3), "String lengths")) passedTests++;
        
        // Test 12: squareNumbers
        System.out.println("\n[Test 12] squareNumbers");
        List<Integer> result12 = squareNumbers(Arrays.asList(1, 2, 3, 4));
        totalTests++; if (testList(result12, Arrays.asList(1, 4, 9, 16), "Square numbers")) passedTests++;
        
        // Test 13: getFirstCharacters
        System.out.println("\n[Test 13] getFirstCharacters");
        List<Character> result13 = getFirstCharacters(Arrays.asList("apple", "banana", "cherry"));
        totalTests++; if (testCharList(result13, Arrays.asList('a', 'b', 'c'), "First characters")) passedTests++;
        
        // Test 14: flattenLists
        System.out.println("\n[Test 14] flattenLists");
        List<List<Integer>> nested = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5));
        List<Integer> result14 = flattenLists(nested);
        totalTests++; if (testList(result14, Arrays.asList(1, 2, 3, 4, 5), "Flatten lists")) passedTests++;
        
        // Test 15: splitAndFlatten
        System.out.println("\n[Test 15] splitAndFlatten");
        List<String> result15 = splitAndFlatten(Arrays.asList("a,b", "c,d", "e"));
        totalTests++; if (testStringList(result15, Arrays.asList("a", "b", "c", "d", "e"), "Split and flatten")) passedTests++;
        
        // Test 16: filterByLength
        System.out.println("\n[Test 16] filterByLength");
        List<String> result16 = filterByLength(Arrays.asList("hi", "hello", "hey", "world"), 4);
        totalTests++; if (testStringList(result16, Arrays.asList("hello", "world"), "Filter by length >= 4")) passedTests++;
        
        // Test 17: filterByPrefix
        System.out.println("\n[Test 17] filterByPrefix");
        List<String> result17 = filterByPrefix(Arrays.asList("apple", "apricot", "banana", "avocado"), "ap");
        totalTests++; if (testStringList(result17, Arrays.asList("apple", "apricot"), "Filter by prefix")) passedTests++;
        
        // Test 18: filterInRange
        System.out.println("\n[Test 18] filterInRange");
        List<Integer> result18 = filterInRange(Arrays.asList(1, 5, 10, 15, 20, 25), 10, 20);
        totalTests++; if (testList(result18, Arrays.asList(10, 15, 20), "Filter in range [10, 20]")) passedTests++;
        
        // Test 19: allPositive
        System.out.println("\n[Test 19] allPositive");
        boolean result19 = allPositive(Arrays.asList(1, 2, 3, 4, 5));
        totalTests++; if (testBoolean(result19, true, "All positive")) passedTests++;
        
        // Test 20: noneNegative
        System.out.println("\n[Test 20] noneNegative");
        boolean result20 = noneNegative(Arrays.asList(1, 2, 3, 4, 5));
        totalTests++; if (testBoolean(result20, true, "None negative")) passedTests++;
        
        // Test 21: product
        System.out.println("\n[Test 21] product");
        int result21 = product(Arrays.asList(2, 3, 4));
        totalTests++; if (testInt(result21, 24, "Product of numbers")) passedTests++;
        
        // Test 22: concatenate
        System.out.println("\n[Test 22] concatenate");
        String result22 = concatenate(Arrays.asList("Hello", " ", "World"));
        totalTests++; if (testString(result22, "Hello World", "Concatenate strings")) passedTests++;
        
        // Test 23: joinWithDelimiter
        System.out.println("\n[Test 23] joinWithDelimiter");
        String result23 = joinWithDelimiter(Arrays.asList("apple", "banana", "cherry"), ", ");
        totalTests++; if (testString(result23, "apple, banana, cherry", "Join with delimiter")) passedTests++;
        
        // Test 24: average
        System.out.println("\n[Test 24] average");
        double result24 = average(Arrays.asList(2, 4, 6, 8, 10));
        totalTests++; if (testDouble(result24, 6.0, "Average of numbers")) passedTests++;
        
        // Test 25: findMin
        System.out.println("\n[Test 25] findMin");
        Integer result25 = findMin(Arrays.asList(3, 7, 2, 9, 1));
        totalTests++; if (testInteger(result25, 1, "Find min")) passedTests++;
        
        // Test 26: toSet
        System.out.println("\n[Test 26] toSet");
        Set<Integer> result26 = toSet(Arrays.asList(1, 2, 2, 3, 1, 4));
        totalTests++; if (testSet(result26, new HashSet<>(Arrays.asList(1, 2, 3, 4)), "Convert to set")) passedTests++;
        
        // Test 27: groupByLength
        System.out.println("\n[Test 27] groupByLength");
        Map<Integer, List<String>> result27 = groupByLength(Arrays.asList("a", "bb", "ccc", "dd"));
        totalTests++; if (result27 != null && result27.get(2) != null && result27.get(2).size() == 2) {
            System.out.println("  ✓ PASS: Group by length");
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Group by length");
        }
        
        // Test 28: partitionByEven
        System.out.println("\n[Test 28] partitionByEven");
        Map<Boolean, List<Integer>> result28 = partitionByEven(Arrays.asList(1, 2, 3, 4, 5, 6));
        totalTests++; if (result28 != null && result28.get(true) != null && result28.get(true).size() == 3) {
            System.out.println("  ✓ PASS: Partition by even");
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Partition by even");
        }
        
        // Test 29: countOccurrences
        System.out.println("\n[Test 29] countOccurrences");
        Map<String, Long> result29 = countOccurrences(Arrays.asList("a", "b", "a", "c", "b", "a"));
        totalTests++; if (result29 != null && result29.get("a") == 3) {
            System.out.println("  ✓ PASS: Count occurrences");
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Count occurrences");
        }
        
        // Test 30: stringToLengthMap
        System.out.println("\n[Test 30] stringToLengthMap");
        Map<String, Integer> result30 = stringToLengthMap(Arrays.asList("hi", "hello", "hey"));
        totalTests++; if (result30 != null && result30.get("hello") == 5) {
            System.out.println("  ✓ PASS: String to length map");
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: String to length map");
        }
        
        // Test 31: generateRange
        System.out.println("\n[Test 31] generateRange");
        List<Integer> result31 = generateRange(1, 6);
        totalTests++; if (testList(result31, Arrays.asList(1, 2, 3, 4, 5), "Generate range [1, 6)")) passedTests++;
        
        // Test 32: sumOfSquares
        System.out.println("\n[Test 32] sumOfSquares");
        int result32 = sumOfSquares(5);
        totalTests++; if (testInt(result32, 55, "Sum of squares of 1-5")) passedTests++;
        
        // Test 33: sumOfEvens
        System.out.println("\n[Test 33] sumOfEvens");
        int result33 = sumOfEvens(1, 10);
        totalTests++; if (testInt(result33, 30, "Sum of evens [1, 10]")) passedTests++;
        
        // Test 34: generateRandomNumbers
        System.out.println("\n[Test 34] generateRandomNumbers");
        List<Integer> result34 = generateRandomNumbers(5, 100);
        totalTests++; if (result34 != null && result34.size() == 5) {
            System.out.println("  ✓ PASS: Generate 5 random numbers");
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Generate random numbers");
        }
        
        // Test 35: factorial
        System.out.println("\n[Test 35] factorial");
        long result35 = factorial(5);
        totalTests++; if (testLong(result35, 120L, "Factorial of 5")) passedTests++;
        
        // Test 36: getTopN
        System.out.println("\n[Test 36] getTopN");
        List<Integer> result36 = getTopN(Arrays.asList(5, 2, 8, 1, 9, 3), 3);
        totalTests++; if (testList(result36, Arrays.asList(9, 8, 5), "Get top 3")) passedTests++;
        
        // Test 37: distinctSorted
        System.out.println("\n[Test 37] distinctSorted");
        List<Integer> result37 = distinctSorted(Arrays.asList(5, 2, 8, 2, 1, 5));
        totalTests++; if (testList(result37, Arrays.asList(1, 2, 5, 8), "Distinct sorted")) passedTests++;
        
        // Test 38: findFirstGreaterThan
        System.out.println("\n[Test 38] findFirstGreaterThan");
        Integer result38 = findFirstGreaterThan(Arrays.asList(1, 2, 5, 8, 3), 4);
        totalTests++; if (testInteger(result38, 5, "Find first > 4")) passedTests++;
        
        // Test 39: contains
        System.out.println("\n[Test 39] contains");
        boolean result39 = contains(Arrays.asList("apple", "banana", "cherry"), "banana");
        totalTests++; if (testBoolean(result39, true, "Contains banana")) passedTests++;
        
        // Test 40: reverseList
        System.out.println("\n[Test 40] reverseList");
        List<String> result40 = reverseList(Arrays.asList("a", "b", "c"));
        totalTests++; if (testStringList(result40, Arrays.asList("c", "b", "a"), "Reverse list")) passedTests++;
        
        // Test 41: uniqueCharacters
        System.out.println("\n[Test 41] uniqueCharacters");
        Set<Character> result41 = uniqueCharacters(Arrays.asList("hello", "world"));
        totalTests++; if (result41 != null && result41.size() == 7) {
            System.out.println("  ✓ PASS: Unique characters");
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Unique characters");
        }
        
        // Test 42: filterNonEmpty
        System.out.println("\n[Test 42] filterNonEmpty");
        List<String> result42 = filterNonEmpty(Arrays.asList("hello", "", "world", ""));
        totalTests++; if (testStringList(result42, Arrays.asList("hello", "world"), "Filter non-empty")) passedTests++;
        
        // Test 43: toCommaSeparated
        System.out.println("\n[Test 43] toCommaSeparated");
        String result43 = toCommaSeparated(Arrays.asList("apple", "banana", "cherry"));
        totalTests++; if (testString(result43, "apple, banana, cherry", "Comma separated")) passedTests++;
        
        // Test 44: countWords
        System.out.println("\n[Test 44] countWords");
        long result44 = countWords(Arrays.asList("hello world", "foo bar", "test"));
        totalTests++; if (testLong(result44, 5L, "Count words")) passedTests++;
        
        // Test 45: getLongest
        System.out.println("\n[Test 45] getLongest");
        String result45 = getLongest(Arrays.asList("hi", "hello", "hey", "world"));
        totalTests++; if (testString(result45, "hello", "Get longest string")) passedTests++;
        
        // Test 46: groupAndSum
        System.out.println("\n[Test 46] groupAndSum");
        Map<String, Integer> result46 = groupAndSum(Arrays.asList("A:10", "B:20", "A:5", "B:15"));
        totalTests++; if (result46 != null && result46.get("A") == 15) {
            System.out.println("  ✓ PASS: Group and sum");
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Group and sum");
        }
        
        // Test 47: getStatistics
        System.out.println("\n[Test 47] getStatistics");
        IntSummaryStatistics result47 = getStatistics(Arrays.asList(1, 2, 3, 4, 5));
        totalTests++; if (result47 != null && result47.getMax() == 5) {
            System.out.println("  ✓ PASS: Get statistics");
            passedTests++;
        } else {
            System.out.println("  ✗ FAIL: Get statistics");
        }
        
        // Test 48: findCommon
        System.out.println("\n[Test 48] findCommon");
        List<Integer> result48 = findCommon(Arrays.asList(1, 2, 3, 4), Arrays.asList(3, 4, 5, 6));
        totalTests++; if (testList(result48, Arrays.asList(3, 4), "Find common elements")) passedTests++;
        
        // Test 49: sortByLengthThenAlpha
        System.out.println("\n[Test 49] sortByLengthThenAlpha");
        List<String> result49 = sortByLengthThenAlpha(Arrays.asList("banana", "apple", "kiwi", "pear"));
        totalTests++; if (testStringList(result49, Arrays.asList("kiwi", "pear", "apple", "banana"), "Sort by length then alpha")) passedTests++;
        
        // Test 50: customPipeline
        System.out.println("\n[Test 50] customPipeline");
        List<Integer> result50 = customPipeline(Arrays.asList(1, 2, 3, 4, 5, 6, 6, 8));
        totalTests++; if (testList(result50, Arrays.asList(64, 36, 16), "Custom pipeline")) passedTests++;
        
        // Print final results
        System.out.println("\n" + "=".repeat(60));
        System.out.println("TEST RESULTS");
        System.out.println("=".repeat(60));
        System.out.println("Total Tests: " + totalTests);
        System.out.println("Passed: " + passedTests);
        System.out.println("Failed: " + (totalTests - passedTests));
        System.out.println("Success Rate: " + String.format("%.2f", (passedTests * 100.0 / totalTests)) + "%");
        System.out.println("=".repeat(60));
        
        if (passedTests == totalTests) {
            System.out.println("\n🎉 CONGRATULATIONS! All tests passed! 🎉");
            System.out.println("You have successfully mastered Java Streams!");
        } else {
            System.out.println("\n⚠️  Keep practicing! Review the failed tests and try again.");
        }
    }
    
    // ==================== TEST HELPER METHODS ====================
    
    private static boolean testList(List<Integer> actual, List<Integer> expected, String testName) {
        if (actual != null && actual.equals(expected)) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: " + expected + ", Actual: " + actual);
            return false;
        }
    }
    
    private static boolean testStringList(List<String> actual, List<String> expected, String testName) {
        if (actual != null && actual.equals(expected)) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: " + expected + ", Actual: " + actual);
            return false;
        }
    }
    
    private static boolean testCharList(List<Character> actual, List<Character> expected, String testName) {
        if (actual != null && actual.equals(expected)) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: " + expected + ", Actual: " + actual);
            return false;
        }
    }
    
    private static boolean testInt(int actual, int expected, String testName) {
        if (actual == expected) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: " + expected + ", Actual: " + actual);
            return false;
        }
    }
    
    private static boolean testLong(long actual, long expected, String testName) {
        if (actual == expected) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: " + expected + ", Actual: " + actual);
            return false;
        }
    }
    
    private static boolean testInteger(Integer actual, Integer expected, String testName) {
        if ((actual == null && expected == null) || (actual != null && actual.equals(expected))) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: " + expected + ", Actual: " + actual);
            return false;
        }
    }
    
    private static boolean testBoolean(boolean actual, boolean expected, String testName) {
        if (actual == expected) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: " + expected + ", Actual: " + actual);
            return false;
        }
    }
    
    private static <T> boolean testSet(Set<T> actual, Set<T> expected, String testName) {
        if (actual != null && actual.equals(expected)) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: " + expected + ", Actual: " + actual);
            return false;
        }
    }
    
    private static boolean testString(String actual, String expected, String testName) {
        if ((actual == null && expected == null) || (actual != null && actual.equals(expected))) {
            System.out.println("  ✓ PASS: " + testName + " => Output: \"" + actual + "\"");
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: \"" + expected + "\", Actual: \"" + actual + "\"");
            return false;
        }
    }
    
    private static boolean testDouble(double actual, double expected, String testName) {
        if (Math.abs(actual - expected) < 0.001) {
            System.out.println("  ✓ PASS: " + testName + " => Output: " + actual);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: " + expected + ", Actual: " + actual);
            return false;
        }
    }
}
