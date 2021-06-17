import java.util.Arrays;

public class Analyser {


    public static boolean isOperator(char input) {
        Character[] operators = new Character[]{'-', '+', '=', '<', '>', '?', '!', '%', '&', '*', '^', '|', '/'};
        return Arrays.stream(operators)
                .anyMatch(character -> input == character);
    }

    public static boolean isSeparator(char input) {
        Character[] separators = new Character[]{ '"', ',', ' ', '{', '}', '[', ']', ';', ':', '\t', '\n', '(', ')', '.', '\'', '@'};
        return Arrays.stream(separators)
                .anyMatch(character -> input == character);
    }

    static boolean isKeyword(String input) {
        String[] keywords = new String[]{"abstract", "assert", "boolean", "break", "byte", "switch",
                "enum", "extends", "final", "float", "for", "implements", "import", "short", "static", "strictfp", "instanceOf", "int", "interface", "long",
                "case", "try", "catch", "finally", "this", "throw", "throws", "transient", "void", "volatile", "while", "goto",
                "char", "class", "continue", "default", "do", "double", "if", "else",
                "native", "new", "package", "private", "protected", "public", "return",
                "super", "synchronized", "const", "true", "false"};
        return Arrays.asList(keywords)
                .contains(input);
    }


    public static boolean isDigit(char input) {
        return Character.isDigit(input);
    }

    public static boolean isLetter(char input) {
        return Character.isLetter(input) || input == '_' || input == '$';
    }
}