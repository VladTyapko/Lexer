import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        Lexer testedInstance = new Lexer("/home/vladislav/IdeaProjects/Lexer/src/main/resources/input.txt");
        try {
            testedInstance.tokenize().forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println();
        System.out.println();
        System.out.println("********************************************");
        System.out.println();
        System.out.println();

        testedInstance.printSortedTokens();
    }
}