import java.util.*;

public class RegexEngine {

    // Entry point: attempts to match the pattern anywhere in the text
    public static boolean match(String pattern, String text) {
        // Anchored match (beginning of string)
        if (pattern.startsWith("^")) {
            return matchHere(pattern.substring(1), text); // Remove ^ and match from beginning
        }

        // Unanchored: try matching at every substring of text
        for (int i = 0; i <= text.length(); i++) {
            if (matchHere(pattern, text.substring(i))) {
                return true;
            }
        }
        return false;
    }

    // Recursive matcher that checks if pattern matches the beginning of text
    private static boolean matchHere(String pattern, String text) {
        //First, do easy checks: Pattern is empty or text is empty

        //Pattern is empty, so finished
        if (pattern.isEmpty()) return true;

        //Text is empty, so finished
        if (pattern.equals("$") && text.isEmpty()) return true;

        //Finding a [. So find the matching ] and return the set between them (STRING PARSING + SET THEORY)
        if(pattern.startsWith("[")){
            int closing = pattern.indexOf("]");
            if(closing == -1) throw new RuntimeException("Unclosed character class");
            
            String classToken = pattern.substring(0, closing + 1); // [abc] or [^a-z]
            String rest = pattern.substring(closing + 1);
            boolean negate = classToken.startsWith("[^");

            //Parse character set from inside brackets
            Set<Character> charSet = parseCharClass(classToken);

            //Function that handles repetition and character sets
            return matchClassAndRepeat(charSet, negate, rest, text);
        }

        //Checks for repeat characters in the pattern (e.g., a*, a+, a?)
        if(pattern.length() >= 2 && isRepeatSymbol(pattern.charAt(1))){
            char current = pattern.charAt(0);
            String rest = pattern.substring(2);

            switch(pattern.charAt(1)){
                case '*': return matchRepeat(current, rest, text, 0);
                case '+': return matchRepeat(current, rest, text, 1);
                case '?': return matchRepeat(current, rest, text, 0, 1);
            }
        }

        //Literal match: check if the first character of pattern matches the first character of text
        if(!text.isEmpty() && (pattern.charAt(0) == '.' || pattern.charAt(0) == text.charAt(0))){
            return matchHere(pattern.substring(1), text.substring(1));
        }

        // No match
        return false;
    }


    //AUTOMATA BEHAVIOR FOR REPETITION
    //If given -- or more characters, then set max to Integer.MAX_VALUE
    private static boolean matchRepeat(char c, String pattern, String text, int min){
        return matchRepeat(c, pattern, text, min, Integer.MAX_VALUE);
    }

    //Repeat matcher with bounded repetition
    private static boolean matchRepeat(char c, String pattern, String text, int min, int max){
        int count = 0;

        //Count how many chars in text match the current char (or '.')
        while(count < text.length() && (c == '.' || text.charAt(count) == c)){
            count++;
        }

        //Try all backtracking counts from min to max
        for(int i = min; i <= Math.min(count, max); i++){
            if(matchHere(pattern, text.substring(i))){
                return true;
            }
        }

        return false;
    }

    //Matches character set with character classes
    private static boolean matchClassAndRepeat(Set<Character> charSet, boolean negate, String pattern, String text){
        int repeatMin = 1;
        int repeatMax = 1;

        // Handle quantifiers after character class
        if(!pattern.isEmpty() && isRepeatSymbol(pattern.charAt(0))){
            char rep = pattern.charAt(0);
            pattern = pattern.substring(1);

            switch (rep){
                case '*': repeatMin = 0; repeatMax = Integer.MAX_VALUE; break;
                case '+': repeatMin = 1; repeatMax = Integer.MAX_VALUE; break;
                case '?': repeatMin = 0; repeatMax = 1; break;
            }
        }

        int count = 0;
        while(count < text.length()){
            boolean contains = charSet.contains(text.charAt(count));
            if(negate) contains = !contains;

            if(!contains) break;
            count++;
        }

        for(int i = repeatMin; i <= Math.min(count, repeatMax); i++){
            if(matchHere(pattern, text.substring(i))){
                return true;
            }
        }

        return false;
    }

    //Creates character set from a string to be used
    private static Set<Character> parseCharClass(String s){
        Set<Character> set = new HashSet<>();
        boolean negate = s.startsWith("[^");
        int i = negate ? 2 : 1;

        while(i < s.length() - 1){
            char start = s.charAt(i);

            //Handle range (e.g., a-z)
            if(i + 2 < s.length() - 1 && s.charAt(i + 1) == '-'){
                char end = s.charAt(i + 2);
                for(char c = start; c <= end; c++){
                    set.add(c);
                }
                i += 3;
            }else{
                set.add(start);
                i++;
            }
        }
        return set;
    }

    //Checks if a char is one of the repeat symbols (*, +, ?)
    private static boolean isRepeatSymbol(char c){
        return c == '*' || c == '+' || c == '?';
    }

    //MAIN
    public static void main(String[] args){
        System.out.println(match("^a[bc]*d$", "abcbcd")); //True
        System.out.println(match("a[xyz]+z", "axyzxyzxz")); //True
        System.out.println(match("^[A-Z]+[0-9]*$", "HELLO123")); //True
        System.out.println(match(".[aeiou]*.", "baiiiz"));  //True
        System.out.println(match("^[^0-9]*$", "NoDigitsHere")); //True
        System.out.println(match("h[ae]*llo", "hello")); //True
        System.out.println(match("^colou?r$", "color")); //True
        System.out.println(match("ab*bc", "abbc")); //True
        
        System.out.println(match("^a[bc]*d$", "abcecd")); //False
        System.out.println(match("^[A-Z]+[0-9]*$", "Hello123")); //False
        System.out.println(match("^h[ae]*llo$", "hxllo")); //False
    }
}