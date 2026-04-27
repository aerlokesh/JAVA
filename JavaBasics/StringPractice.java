
import java.util.*;


/**
 * StringPractice.java
 * 
 * A comprehensive Java practice file for mastering String manipulation.
 * Complete all the TODO methods below and run the main method to test your solutions.
 * 
 * Instructions:
 * 1. Read each method's description carefully
 * 2. Replace the "TODO: Implement this method" with your solution
 * 3. Run: javac StringPractice.java && java StringPractice
 * 4. Check if all test cases pass
 */

public class StringPractice {
    
    // ==================== BASIC STRING OPERATIONS ====================
    
    /**
     * Task 1: Get the length of a string
     * @param str Input string
     * @return Length of the string
     */
    public static int getStringLength(String str) {
        return str.length();
    }
    
    /**
     * Task 2: Get character at specific index
     * @param str Input string
     * @param index Index position
     * @return Character at the given index
     */
    public static char getCharAtIndex(String str, int index) {
        return str.charAt(index);
    }
    
    /**
     * Task 3: Extract substring from start to end index
     * @param str Input string
     * @param start Starting index (inclusive)
     * @param end Ending index (exclusive)
     * @return Substring from start to end
     */
    public static String extractSubstring(String str, int start, int end) {
        return str.substring(start, end);
    }
    
    /**
     * Task 4: Find the first occurrence of a character
     * @param str Input string
     * @param ch Character to find
     * @return Index of first occurrence, or -1 if not found
     */
    public static int findFirstOccurrence(String str, char ch) {
        return str.indexOf(ch, 0);
    }
    
    /**
     * Task 5: Find the last occurrence of a character
     * @param str Input string
     * @param ch Character to find
     * @return Index of last occurrence, or -1 if not found
     */
    public static int findLastOccurrence(String str, char x) {
        int ans=-1;
        char[] ch=str.toCharArray();
        for(int i=0;i<ch.length;i++){
            if(ch[i]==x) {
                ans=i;
            }
        }
        return ans;
    }
    
    // ==================== STRING COMPARISON ====================
    
    /**
     * Task 6: Compare two strings for equality (case-sensitive)
     * @param str1 First string
     * @param str2 Second string
     * @return true if equal, false otherwise
     */
    public static boolean areStringsEqual(String str1, String str2) {
        return str1.equals(str2);
    }
    
    /**
     * Task 7: Compare two strings ignoring case
     * @param str1 First string
     * @param str2 Second string
     * @return true if equal (ignoring case), false otherwise
     */
    public static boolean areStringsEqualIgnoreCase(String str1, String str2) {
        return str1.equalsIgnoreCase(str2);
    }
    
    /**
     * Task 8: Compare two strings lexicographically
     * @param str1 First string
     * @param str2 Second string
     * @return negative if str1 < str2, 0 if equal, positive if str1 > str2
     */
    public static int compareStrings(String str1, String str2) {
        return str1.compareTo(str2);
    }
    
    // ==================== STRING MODIFICATION ====================
    
    /**
     * Task 9: Convert string to uppercase
     * @param str Input string
     * @return String in uppercase
     */
    public static String convertToUpperCase(String str) {
        return str.toUpperCase();
    }
    
    /**
     * Task 10: Convert string to lowercase
     * @param str Input string
     * @return String in lowercase
     */
    public static String convertToLowerCase(String str) {
        return str.toLowerCase();
    }
    
    /**
     * Task 11: Remove leading and trailing whitespace
     * @param str Input string
     * @return Trimmed string
     */
    public static String trimWhitespace(String str) {
        char []c=str.toCharArray();
        StringBuilder sb=new StringBuilder();
        for (int i=0;i<c.length;i++) {
            if(c[i]!=' ') sb.append(c[i]);
        }
        return sb.toString().trim();
    }
    
    /**
     * Task 12: Replace all occurrences of a character
     * @param str Input string
     * @param oldChar Character to replace
     * @param newChar Replacement character
     * @return Modified string
     */
    public static String replaceCharacter(String str, char oldChar, char newChar) {
        // TODO: Implement this method
        return str.replace(oldChar, newChar);
    }
    
    /**
     * Task 13: Replace all occurrences of a substring
     * @param str Input string
     * @param target Substring to replace
     * @param replacement Replacement substring
     * @return Modified string
     */
    public static String replaceSubstring(String str, String target, String replacement) {
        return str.replace(target, replacement);
    }
    
    // ==================== STRING CHECKING ====================
    
    /**
     * Task 14: Check if string starts with a prefix
     * @param str Input string
     * @param prefix Prefix to check
     * @return true if string starts with prefix
     */
    public static boolean startsWith(String str, String prefix) {
        return str.startsWith(prefix);
    }
    
    /**
     * Task 15: Check if string ends with a suffix
     * @param str Input string
     * @param suffix Suffix to check
     * @return true if string ends with suffix
     */
    public static boolean endsWith(String str, String suffix) {
        return str.endsWith(suffix);
    }
    
    /**
     * Task 16: Check if string contains a substring
     * @param str Input string
     * @param substring Substring to find
     * @return true if substring is found
     */
    public static boolean containsSubstring(String str, String substring) {
        return str.contains(substring);
    }
    
    /**
     * Task 17: Check if string is empty
     * @param str Input string
     * @return true if string is empty or null
     */
    public static boolean isEmpty(String str) {
        return str==null || str.isBlank();
    }
    
    // ==================== STRING MANIPULATION ====================
    
    /**
     * Task 18: Reverse a string
     * @param str Input string
     * @return Reversed string
     */
    public static String reverseString(String str) {
        return new StringBuilder(str).reverse().toString();
    }
    
    /**
     * Task 19: Check if a string is a palindrome
     * @param str Input string
     * @return true if palindrome, false otherwise
     */
    public static boolean isPalindrome(String str) {
        return new StringBuilder(str).reverse().toString().equals(str);
    }
    
    /**
     * Task 20: Count occurrences of a character in a string
     * @param str Input string
     * @param ch Character to count
     * @return Number of occurrences
     */
    public static int countCharacter(String str, char ch) {
        int cc=0;
        for(char c:str.toCharArray()){
            if(c==ch){
                cc++;
            }
        }
        return cc;       
    }
    
    /**
     * Task 21: Count vowels in a string
     * @param str Input string
     * @return Number of vowels (a, e, i, o, u - case insensitive)
     */
    public static int countVowels(String str) {
        int cc=0;
        for(char c:str.toLowerCase().toCharArray()){
            if(c=='a' || c=='e' || c=='i' || c=='o'|| c=='u'){
                cc++;
            }
        }
        return cc;
    }
    
    /**
     * Task 22: Count consonants in a string
     * @param str Input string
     * @return Number of consonants
     */
    public static int countConsonants(String str) {
        int cc=0;
        for(Character c:str.toLowerCase().toCharArray()){
            if(c=='a' || c=='e' || c=='i' || c=='o'|| c=='u'){

            }else{
                cc++;
            }
        }
        return cc;
    }
    
    /**
     * Task 23: Count words in a string
     * @param str Input string
     * @return Number of words (separated by spaces)
     */
    public static int countWords(String str) {
        return str.split(" ").length;
    }
    
    // ==================== STRING SPLITTING & JOINING ====================
    
    /**
     * Task 24: Split string by delimiter
     * @param str Input string
     * @param delimiter Delimiter character
     * @return Array of split strings
     */
    public static String[] splitString(String str, String delimiter) {
        return str.split(delimiter);
    }
    
    /**
     * Task 25: Join array of strings with delimiter
     * @param arr Array of strings
     * @param delimiter Delimiter to use
     * @return Joined string
     */
    public static String joinStrings(String[] arr, String delimiter) {
        StringBuilder sb= new StringBuilder();
        for(int i=0;i<arr.length;i++){
            sb.append(arr[i]);
            if(i!=(arr.length-1)){
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }
    
    // ==================== ADVANCED STRING OPERATIONS ====================
    
    /**
     * Task 26: Remove all whitespace from string
     * @param str Input string
     * @return String without any whitespace
     */
    public static String removeAllWhitespace(String str) {
        String s[]=str.split(" ");
        StringBuilder sb = new StringBuilder("");
        for(String x:s){
            sb.append(x);
        }
        return sb.toString();
    }
    
    /**
     * Task 27: Capitalize first letter of each word
     * @param str Input string
     * @return String with each word capitalized
     */
    public static String capitalizeWords(String str) {
        String []s = str.split(" ");
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<s.length;i++){
            String x=s[i];
            sb.append(x.substring(0,1).toUpperCase()).append(x.substring(1));
            if(i!=s.length-1) sb.append(" ");
        }
        return sb.toString();
    }
    
    /**
     * Task 28: Check if two strings are anagrams
     * @param str1 First string
     * @param str2 Second string
     * @return true if anagrams, false otherwise
     */
    public static boolean areAnagrams(String str1, String str2) {
        if(str1.length()!=str2.length()) return false;
        char[] s1=str1.toCharArray();
        char[] s2=str2.toCharArray();
        Arrays.sort(s1);
        Arrays.sort(s2);
        for(int i=0;i<s1.length;i++){
            if(s1[i]!=s2[i]) return false;
        }
        return true;
    }
    
    /**
     * Task 29: Remove duplicate characters from string
     * @param str Input string
     * @return String with duplicates removed (keep first occurrence)
     */
    public static String removeDuplicates(String str) {
        HashSet<Character> hs=new HashSet<>();
        String x="";
        for(Character c:str.toCharArray()){
            if(!hs.contains(c)){
                x+=c;
                hs.add(c);
            }
        }
        return x;
    }
    
    /**
     * Task 30: Find the longest word in a string
     * @param str Input string
     * @return The longest word
     */
    public static String findLongestWord(String str) {
        String[] s=str.split(" ");
        int wl=0; String aa="";
        for(String x:s){
            if(wl<x.length()){
                wl=x.length();
                aa=x;
            }
        }
        return aa;
    }
    
    /**
     * Task 31: Reverse each word in a string
     * @param str Input string
     * @return String with each word reversed
     */
    public static String reverseWords(String str) {
        String[] ss=str.split(" ");
        StringBuilder aa=new StringBuilder();
        for(int i=0;i<ss.length;i++){
            ss[i]=new StringBuilder(ss[i]).reverse().toString();
            aa.append(ss[i]);
            if(i!=aa.length()-1) aa.append(" ");
        }
        return aa.toString().trim();
    }
    
    /**
     * Task 32: Check if string contains only digits
     * @param str Input string
     * @return true if string contains only digits
     */
    public static boolean isNumeric(String str) {
        // TODO: Implement this method
        for(char c:str.toCharArray()){
            if(!Character.isDigit(c)) return false;
        }
        return true;    
    }
    
    /**
     * Task 33: Check if string contains only alphabetic characters
     * @param str Input string
     * @return true if string contains only letters
     */
    public static boolean isAlphabetic(String str) {
        // TODO: Implement this method
        for(char c:str.toCharArray()){
            if(!Character.isAlphabetic(c)) return false;
        }
        return true;
    }
    
    /**
     * Task 34: Convert string to character array
     * @param str Input string
     * @return Character array
     */
    public static char[] toCharArray(String str) {
        // TODO: Implement this method
        return str.toCharArray();
    }
    
    /**
     * Task 35: Find first non-repeating character
     * @param str Input string
     * @return First non-repeating character, or '\0' if none
     */
    public static char findFirstNonRepeatingChar(String str) {
        // TODO: Implement this method
        HashMap<Character,Integer> hm=new HashMap<>();
        for(char c:str.toCharArray()){
            hm.put(c,hm.getOrDefault(c, 0)+1);
        }
        for(char c:str.toCharArray()){
            if(hm.get(c)==1) return c;
        }
        
        return '\0';
    }
    
    // ==================== STRING BUILDER OPERATIONS ====================
    
    /**
     * Task 36: Concatenate multiple strings efficiently using StringBuilder
     * @param strings Array of strings to concatenate
     * @return Concatenated string
     */
    public static String concatenateStrings(String[] strings) {
        StringBuilder sb=new StringBuilder();
        for(String s:strings) sb.append(s);
        return sb.toString();
    }
    
    /**
     * Task 37: Build a string with repeated character
     * @param ch Character to repeat
     * @param count Number of times to repeat
     * @return String with repeated character
     */
    public static String repeatCharacter(char ch, int count) {
        String s="";
        while(count -- > 0) s+=ch;
        return s;
    }
    
    // ==================== PATTERN MATCHING ====================
    
    /**
     * Task 38: Check if string matches a pattern (contains only alphanumeric)
     * @param str Input string
     * @return true if string is alphanumeric
     */
    public static boolean isAlphanumeric(String str) {
        for(Character c:str.toCharArray()){
            if(!Character.isAlphabetic(c) && !Character.isDigit(c)){
                return false;
            }
        }
        return true;
    }
    
    /**
     * Task 39: Extract all digits from a string
     * @param str Input string
     * @return String containing only digits
     */
    public static String extractDigits(String str) {
        String s="";
        for(Character c:str.toCharArray()){
            if(Character.isDigit(c)){
                s+=c;
            }
        }
        return s;
    }
    
    /**
     * Task 40: Count substring occurrences in a string
     * @param str Input string
     * @param substring Substring to count
     * @return Number of occurrences
     */
    public static int countSubstringOccurrences(String str, String substring) {
        int cnt=0;
        for(int i=0;i<str.length();i++){
            int ei=Math.min(i+substring.length(), str.length());
            String x=str.substring(i,ei);
            if(x.equals(substring)) cnt++;
        }
        return cnt;
    }
    
    // ==================== TEST CASES ====================
    
    public static void main(String[] args) {
        int totalTests = 0;
        int passedTests = 0;
        
        System.out.println("=".repeat(60));
        System.out.println("STRING PRACTICE - TEST SUITE");
        System.out.println("=".repeat(60));
        
        // Test 1: getStringLength
        System.out.println("\n[Test 1] getStringLength");
        totalTests++; if (testInt(getStringLength("Hello"), 5, "Length of 'Hello'")) passedTests++;
        totalTests++; if (testInt(getStringLength(""), 0, "Length of empty string")) passedTests++;
        totalTests++; if (testInt(getStringLength("Java Programming"), 16, "Length with space")) passedTests++;
        
        // Test 2: getCharAtIndex
        System.out.println("\n[Test 2] getCharAtIndex");
        totalTests++; if (testChar(getCharAtIndex("Hello", 0), 'H', "First character")) passedTests++;
        totalTests++; if (testChar(getCharAtIndex("Hello", 4), 'o', "Last character")) passedTests++;
        totalTests++; if (testChar(getCharAtIndex("Java", 2), 'v', "Middle character")) passedTests++;
        
        // Test 3: extractSubstring
        System.out.println("\n[Test 3] extractSubstring");
        totalTests++; if (testString(extractSubstring("Hello World", 0, 5), "Hello", "First word")) passedTests++;
        totalTests++; if (testString(extractSubstring("Hello World", 6, 11), "World", "Second word")) passedTests++;
        totalTests++; if (testString(extractSubstring("Programming", 3, 7), "gram", "Middle substring")) passedTests++;
        
        // Test 4: findFirstOccurrence
        System.out.println("\n[Test 4] findFirstOccurrence");
        totalTests++; if (testInt(findFirstOccurrence("Hello", 'l'), 2, "Find 'l' in 'Hello'")) passedTests++;
        totalTests++; if (testInt(findFirstOccurrence("Hello", 'H'), 0, "Find first char")) passedTests++;
        totalTests++; if (testInt(findFirstOccurrence("Hello", 'x'), -1, "Character not found")) passedTests++;
        
        // Test 5: findLastOccurrence
        System.out.println("\n[Test 5] findLastOccurrence");
        totalTests++; if (testInt(findLastOccurrence("Hello", 'l'), 3, "Find last 'l' in 'Hello'")) passedTests++;
        totalTests++; if (testInt(findLastOccurrence("Hello", 'o'), 4, "Find last char")) passedTests++;
        totalTests++; if (testInt(findLastOccurrence("Hello", 'x'), -1, "Character not found")) passedTests++;
        
        // Test 6: areStringsEqual
        System.out.println("\n[Test 6] areStringsEqual");
        totalTests++; if (testBoolean(areStringsEqual("Hello", "Hello"), true, "Same strings")) passedTests++;
        totalTests++; if (testBoolean(areStringsEqual("Hello", "hello"), false, "Different case")) passedTests++;
        totalTests++; if (testBoolean(areStringsEqual("Java", "Python"), false, "Different strings")) passedTests++;
        
        // Test 7: areStringsEqualIgnoreCase
        System.out.println("\n[Test 7] areStringsEqualIgnoreCase");
        totalTests++; if (testBoolean(areStringsEqualIgnoreCase("Hello", "hello"), true, "Ignore case")) passedTests++;
        totalTests++; if (testBoolean(areStringsEqualIgnoreCase("JAVA", "java"), true, "Ignore case uppercase")) passedTests++;
        totalTests++; if (testBoolean(areStringsEqualIgnoreCase("Java", "Python"), false, "Different strings")) passedTests++;
        
        // Test 8: compareStrings
        System.out.println("\n[Test 8] compareStrings");
        totalTests++; if (testComparison(compareStrings("apple", "banana"), -1, "apple < banana")) passedTests++;
        totalTests++; if (testComparison(compareStrings("banana", "apple"), 1, "banana > apple")) passedTests++;
        totalTests++; if (testComparison(compareStrings("test", "test"), 0, "Equal strings")) passedTests++;
        
        // Test 9: convertToUpperCase
        System.out.println("\n[Test 9] convertToUpperCase");
        totalTests++; if (testString(convertToUpperCase("hello"), "HELLO", "Lowercase to uppercase")) passedTests++;
        totalTests++; if (testString(convertToUpperCase("Java"), "JAVA", "Mixed case")) passedTests++;
        totalTests++; if (testString(convertToUpperCase("WORLD"), "WORLD", "Already uppercase")) passedTests++;
        
        // Test 10: convertToLowerCase
        System.out.println("\n[Test 10] convertToLowerCase");
        totalTests++; if (testString(convertToLowerCase("HELLO"), "hello", "Uppercase to lowercase")) passedTests++;
        totalTests++; if (testString(convertToLowerCase("Java"), "java", "Mixed case")) passedTests++;
        totalTests++; if (testString(convertToLowerCase("world"), "world", "Already lowercase")) passedTests++;
        
        // Test 11: trimWhitespace
        System.out.println("\n[Test 11] trimWhitespace");
        totalTests++; if (testString(trimWhitespace("  Hello  "), "Hello", "Leading and trailing spaces")) passedTests++;
        totalTests++; if (testString(trimWhitespace("\tJava\n"), "Java", "Tab and newline")) passedTests++;
        totalTests++; if (testString(trimWhitespace("NoSpace"), "NoSpace", "No whitespace")) passedTests++;
        
        // Test 12: replaceCharacter
        System.out.println("\n[Test 12] replaceCharacter");
        totalTests++; if (testString(replaceCharacter("Hello", 'l', 'L'), "HeLLo", "Replace 'l' with 'L'")) passedTests++;
        totalTests++; if (testString(replaceCharacter("Java", 'a', 'e'), "Jeve", "Replace 'a' with 'e'")) passedTests++;
        totalTests++; if (testString(replaceCharacter("test", 'x', 'y'), "test", "Replace non-existent char")) passedTests++;
        
        // Test 13: replaceSubstring
        System.out.println("\n[Test 13] replaceSubstring");
        totalTests++; if (testString(replaceSubstring("Hello World", "World", "Java"), "Hello Java", "Replace word")) passedTests++;
        totalTests++; if (testString(replaceSubstring("aaa", "a", "b"), "bbb", "Replace all occurrences")) passedTests++;
        totalTests++; if (testString(replaceSubstring("Java", "Python", "C++"), "Java", "Replace non-existent")) passedTests++;
        
        // Test 14: startsWith
        System.out.println("\n[Test 14] startsWith");
        totalTests++; if (testBoolean(startsWith("Hello World", "Hello"), true, "Starts with 'Hello'")) passedTests++;
        totalTests++; if (testBoolean(startsWith("Java Programming", "Java"), true, "Starts with 'Java'")) passedTests++;
        totalTests++; if (testBoolean(startsWith("Hello", "Hi"), false, "Does not start with 'Hi'")) passedTests++;
        
        // Test 15: endsWith
        System.out.println("\n[Test 15] endsWith");
        totalTests++; if (testBoolean(endsWith("Hello World", "World"), true, "Ends with 'World'")) passedTests++;
        totalTests++; if (testBoolean(endsWith("test.java", ".java"), true, "Ends with '.java'")) passedTests++;
        totalTests++; if (testBoolean(endsWith("Hello", "Hi"), false, "Does not end with 'Hi'")) passedTests++;
        
        // Test 16: containsSubstring
        System.out.println("\n[Test 16] containsSubstring");
        totalTests++; if (testBoolean(containsSubstring("Hello World", "lo Wo"), true, "Contains substring")) passedTests++;
        totalTests++; if (testBoolean(containsSubstring("Java Programming", "Program"), true, "Contains 'Program'")) passedTests++;
        totalTests++; if (testBoolean(containsSubstring("Hello", "xyz"), false, "Does not contain")) passedTests++;
        
        // Test 17: isEmpty
        System.out.println("\n[Test 17] isEmpty");
        totalTests++; if (testBoolean(isEmpty(""), true, "Empty string")) passedTests++;
        totalTests++; if (testBoolean(isEmpty(null), true, "Null string")) passedTests++;
        totalTests++; if (testBoolean(isEmpty("Hello"), false, "Non-empty string")) passedTests++;
        
        // Test 18: reverseString
        System.out.println("\n[Test 18] reverseString");
        totalTests++; if (testString(reverseString("Hello"), "olleH", "Reverse 'Hello'")) passedTests++;
        totalTests++; if (testString(reverseString("Java"), "avaJ", "Reverse 'Java'")) passedTests++;
        totalTests++; if (testString(reverseString("12345"), "54321", "Reverse numbers")) passedTests++;
        
        // Test 19: isPalindrome
        System.out.println("\n[Test 19] isPalindrome");
        totalTests++; if (testBoolean(isPalindrome("racecar"), true, "'racecar' is palindrome")) passedTests++;
        totalTests++; if (testBoolean(isPalindrome("madam"), true, "'madam' is palindrome")) passedTests++;
        totalTests++; if (testBoolean(isPalindrome("hello"), false, "'hello' is not palindrome")) passedTests++;
        
        // Test 20: countCharacter
        System.out.println("\n[Test 20] countCharacter");
        totalTests++; if (testInt(countCharacter("Hello", 'l'), 2, "Count 'l' in 'Hello'")) passedTests++;
        totalTests++; if (testInt(countCharacter("Mississippi", 's'), 4, "Count 's' in 'Mississippi'")) passedTests++;
        totalTests++; if (testInt(countCharacter("Java", 'x'), 0, "Count non-existent char")) passedTests++;
        
        // Test 21: countVowels
        System.out.println("\n[Test 21] countVowels");
        totalTests++; if (testInt(countVowels("Hello"), 2, "Vowels in 'Hello'")) passedTests++;
        totalTests++; if (testInt(countVowels("Programming"), 3, "Vowels in 'Programming'")) passedTests++;
        totalTests++; if (testInt(countVowels("AEIOUaeiou"), 10, "All vowels")) passedTests++;
        
        // Test 22: countConsonants
        System.out.println("\n[Test 22] countConsonants");
        totalTests++; if (testInt(countConsonants("Hello"), 3, "Consonants in 'Hello'")) passedTests++;
        totalTests++; if (testInt(countConsonants("Java"), 2, "Consonants in 'Java'")) passedTests++;
        totalTests++; if (testInt(countConsonants("aeiou"), 0, "No consonants")) passedTests++;
        
        // Test 23: countWords
        System.out.println("\n[Test 23] countWords");
        totalTests++; if (testInt(countWords("Hello World"), 2, "Two words")) passedTests++;
        totalTests++; if (testInt(countWords("Java Programming Language"), 3, "Three words")) passedTests++;
        totalTests++; if (testInt(countWords("OneWord"), 1, "Single word")) passedTests++;
        
        // Test 24: splitString
        System.out.println("\n[Test 24] splitString");
        totalTests++; if (testStringArray(splitString("a,b,c", ","), new String[]{"a", "b", "c"}, "Split by comma")) passedTests++;
        totalTests++; if (testStringArray(splitString("Hello World", " "), new String[]{"Hello", "World"}, "Split by space")) passedTests++;
        totalTests++; if (testStringArray(splitString("one-two-three", "-"), new String[]{"one", "two", "three"}, "Split by dash")) passedTests++;
        
        // Test 25: joinStrings
        System.out.println("\n[Test 25] joinStrings");
        totalTests++; if (testString(joinStrings(new String[]{"a", "b", "c"}, ","), "a,b,c", "Join with comma")) passedTests++;
        totalTests++; if (testString(joinStrings(new String[]{"Hello", "World"}, " "), "Hello World", "Join with space")) passedTests++;
        totalTests++; if (testString(joinStrings(new String[]{"one", "two", "three"}, "-"), "one-two-three", "Join with dash")) passedTests++;
        
        // Test 26: removeAllWhitespace
        System.out.println("\n[Test 26] removeAllWhitespace");
        totalTests++; if (testString(removeAllWhitespace("Hello World"), "HelloWorld", "Remove space")) passedTests++;
        totalTests++; if (testString(removeAllWhitespace("  Java  Programming  "), "JavaProgramming", "Remove multiple spaces")) passedTests++;
        totalTests++; if (testString(removeAllWhitespace("a b c d"), "abcd", "Remove all spaces")) passedTests++;
        
        // Test 27: capitalizeWords
        System.out.println("\n[Test 27] capitalizeWords");
        totalTests++; if (testString(capitalizeWords("hello world"), "Hello World", "Capitalize two words")) passedTests++;
        totalTests++; if (testString(capitalizeWords("java programming"), "Java Programming", "Capitalize two words")) passedTests++;
        totalTests++; if (testString(capitalizeWords("the quick brown fox"), "The Quick Brown Fox", "Capitalize sentence")) passedTests++;
        
        // Test 28: areAnagrams
        System.out.println("\n[Test 28] areAnagrams");
        totalTests++; if (testBoolean(areAnagrams("listen", "silent"), true, "'listen' and 'silent'")) passedTests++;
        totalTests++; if (testBoolean(areAnagrams("evil", "vile"), true, "'evil' and 'vile'")) passedTests++;
        totalTests++; if (testBoolean(areAnagrams("hello", "world"), false, "Not anagrams")) passedTests++;
        
        // Test 29: removeDuplicates
        System.out.println("\n[Test 29] removeDuplicates");
        totalTests++; if (testString(removeDuplicates("aabbcc"), "abc", "Remove duplicate pairs")) passedTests++;
        totalTests++; if (testString(removeDuplicates("programming"), "progamin", "Remove duplicates")) passedTests++;
        totalTests++; if (testString(removeDuplicates("abc"), "abc", "No duplicates")) passedTests++;
        
        // Test 30: findLongestWord
        System.out.println("\n[Test 30] findLongestWord");
        totalTests++; if (testString(findLongestWord("The quick brown fox"), "quick", "Longest word")) passedTests++;
        totalTests++; if (testString(findLongestWord("Java Programming Language"), "Programming", "Longest word")) passedTests++;
        totalTests++; if (testString(findLongestWord("a bb ccc"), "ccc", "Longest word")) passedTests++;
        
        // Test 31: reverseWords
        System.out.println("\n[Test 31] reverseWords");
        totalTests++; if (testString(reverseWords("Hello World"), "olleH dlroW", "Reverse words")) passedTests++;
        totalTests++; if (testString(reverseWords("Java Programming"), "avaJ gnimmargorP", "Reverse words")) passedTests++;
        totalTests++; if (testString(reverseWords("abc def"), "cba fed", "Reverse short words")) passedTests++;
        
        // Test 32: isNumeric
        System.out.println("\n[Test 32] isNumeric");
        totalTests++; if (testBoolean(isNumeric("12345"), true, "All digits")) passedTests++;
        totalTests++; if (testBoolean(isNumeric("123abc"), false, "Mixed alphanumeric")) passedTests++;
        totalTests++; if (testBoolean(isNumeric("abc"), false, "No digits")) passedTests++;
        
        // Test 33: isAlphabetic
        System.out.println("\n[Test 33] isAlphabetic");
        totalTests++; if (testBoolean(isAlphabetic("Hello"), true, "All letters")) passedTests++;
        totalTests++; if (testBoolean(isAlphabetic("Hello123"), false, "Mixed alphanumeric")) passedTests++;
        totalTests++; if (testBoolean(isAlphabetic("12345"), false, "No letters")) passedTests++;
        
        // Test 34: toCharArray
        System.out.println("\n[Test 34] toCharArray");
        totalTests++; if (testCharArray(toCharArray("Hello"), new char[]{'H', 'e', 'l', 'l', 'o'}, "Convert to char array")) passedTests++;
        totalTests++; if (testCharArray(toCharArray("Java"), new char[]{'J', 'a', 'v', 'a'}, "Convert to char array")) passedTests++;
        totalTests++; if (testCharArray(toCharArray(""), new char[]{}, "Empty string")) passedTests++;
        
        // Test 35: findFirstNonRepeatingChar
        System.out.println("\n[Test 35] findFirstNonRepeatingChar");
        totalTests++; if (testChar(findFirstNonRepeatingChar("aabbcde"), 'c', "First non-repeating")) passedTests++;
        totalTests++; if (testChar(findFirstNonRepeatingChar("aabbcc"), '\0', "All repeating")) passedTests++;
        totalTests++; if (testChar(findFirstNonRepeatingChar("abcdef"), 'a', "All non-repeating")) passedTests++;
        
        // Test 36: concatenateStrings
        System.out.println("\n[Test 36] concatenateStrings");
        totalTests++; if (testString(concatenateStrings(new String[]{"Hello", " ", "World"}), "Hello World", "Concatenate three strings")) passedTests++;
        totalTests++; if (testString(concatenateStrings(new String[]{"Java", "Programming"}), "JavaProgramming", "Concatenate two strings")) passedTests++;
        totalTests++; if (testString(concatenateStrings(new String[]{"a"}), "a", "Single string")) passedTests++;
        
        // Test 37: repeatCharacter
        System.out.println("\n[Test 37] repeatCharacter");
        totalTests++; if (testString(repeatCharacter('a', 5), "aaaaa", "Repeat 'a' 5 times")) passedTests++;
        totalTests++; if (testString(repeatCharacter('*', 3), "***", "Repeat '*' 3 times")) passedTests++;
        totalTests++; if (testString(repeatCharacter('x', 0), "", "Repeat 0 times")) passedTests++;
        
        // Test 38: isAlphanumeric
        System.out.println("\n[Test 38] isAlphanumeric");
        totalTests++; if (testBoolean(isAlphanumeric("Hello123"), true, "Mixed alphanumeric")) passedTests++;
        totalTests++; if (testBoolean(isAlphanumeric("Java2024"), true, "Alphanumeric")) passedTests++;
        totalTests++; if (testBoolean(isAlphanumeric("Hello World"), false, "Contains space")) passedTests++;
        
        // Test 39: extractDigits
        System.out.println("\n[Test 39] extractDigits");
        totalTests++; if (testString(extractDigits("abc123def456"), "123456", "Extract digits")) passedTests++;
        totalTests++; if (testString(extractDigits("Java2024"), "2024", "Extract year")) passedTests++;
        totalTests++; if (testString(extractDigits("NoDigits"), "", "No digits")) passedTests++;
        
        // Test 40: countSubstringOccurrences
        System.out.println("\n[Test 40] countSubstringOccurrences");
        totalTests++; if (testInt(countSubstringOccurrences("ababab", "ab"), 3, "Count 'ab' in 'ababab'")) passedTests++;
        totalTests++; if (testInt(countSubstringOccurrences("hello world", "o"), 2, "Count 'o' in 'hello world'")) passedTests++;
        totalTests++; if (testInt(countSubstringOccurrences("test", "xyz"), 0, "Substring not found")) passedTests++;
        
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
            System.out.println("You have successfully mastered Java String manipulation!");
        } else {
            System.out.println("\n⚠️  Keep practicing! Review the failed tests and try again.");
        }
    }
    
    // ==================== TEST HELPER METHODS ====================
    
    private static boolean testInt(int actual, int expected, String testName) {
        if (actual == expected) {
            System.out.println("  ✓ PASS: " + testName);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: " + expected + ", Got: " + actual);
            return false;
        }
    }
    
    private static boolean testChar(char actual, char expected, String testName) {
        if (actual == expected) {
            System.out.println("  ✓ PASS: " + testName);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: '" + expected + "', Got: '" + actual + "'");
            return false;
        }
    }
    
    private static boolean testString(String actual, String expected, String testName) {
        if (actual != null && actual.equals(expected)) {
            System.out.println("  ✓ PASS: " + testName);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: \"" + expected + "\", Got: \"" + actual + "\"");
            return false;
        }
    }
    
    private static boolean testBoolean(boolean actual, boolean expected, String testName) {
        if (actual == expected) {
            System.out.println("  ✓ PASS: " + testName);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected: " + expected + ", Got: " + actual);
            return false;
        }
    }
    
    private static boolean testComparison(int actual, int expected, String testName) {
        boolean pass = (expected < 0 && actual < 0) || (expected > 0 && actual > 0) || (expected == 0 && actual == 0);
        if (pass) {
            System.out.println("  ✓ PASS: " + testName);
            return true;
        } else {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Expected sign: " + (expected < 0 ? "negative" : expected > 0 ? "positive" : "zero") + 
                             ", Got: " + actual);
            return false;
        }
    }
    
    private static boolean testStringArray(String[] actual, String[] expected, String testName) {
        if (actual == null && expected == null) {
            System.out.println("  ✓ PASS: " + testName);
            return true;
        }
        if (actual == null || expected == null || actual.length != expected.length) {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Length mismatch or null array");
            return false;
        }
        for (int i = 0; i < actual.length; i++) {
            if (!actual[i].equals(expected[i])) {
                System.out.println("  ✗ FAIL: " + testName);
                System.out.println("    Expected: " + java.util.Arrays.toString(expected));
                System.out.println("    Got: " + java.util.Arrays.toString(actual));
                return false;
            }
        }
        System.out.println("  ✓ PASS: " + testName);
        return true;
    }
    
    private static boolean testCharArray(char[] actual, char[] expected, String testName) {
        if (actual == null && expected == null) {
            System.out.println("  ✓ PASS: " + testName);
            return true;
        }
        if (actual == null || expected == null || actual.length != expected.length) {
            System.out.println("  ✗ FAIL: " + testName);
            System.out.println("    Length mismatch or null array");
            return false;
        }
        for (int i = 0; i < actual.length; i++) {
            if (actual[i] != expected[i]) {
                System.out.println("  ✗ FAIL: " + testName);
                System.out.println("    Expected: " + java.util.Arrays.toString(expected));
                System.out.println("    Got: " + java.util.Arrays.toString(actual));
                return false;
            }
        }
        System.out.println("  ✓ PASS: " + testName);
        return true;
    }
}
