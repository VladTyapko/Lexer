import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class Lexer {
    File file;
    StringBuilder buffer = new StringBuilder();
    ArrayList<Token> tokens;
    StatusHelper statusHelper;
    boolean isCharacter = false;

    public Lexer(String filename) {
        file = new File(filename);
        tokens = new ArrayList<>();
        statusHelper = new StatusHelper();
    }

    public ArrayList<Token> tokenize() throws IOException {
        BufferedInputStream bufferedInputStream = null;
        try {
            bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
            int characterCode = bufferedInputStream.read();
            while (characterCode != -1) {
                char character = (char) characterCode;
                characterAnalyser(character);

                characterCode = bufferedInputStream.read();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            bufferedInputStream.close();
        }
        return tokens;
    }

    private void characterAnalyser(char input) {

        if (isCharacter()) {
            processCharacter(input);
        } else if (isComment()) {
            processComment(input);
        } else if (isString()) {
            processString(input);
        } else if (Analyser.isLetter(input)) {
            processLetter(input);
        } else if (Analyser.isDigit(input)) {
            processDigit(input);
        }
        else if (Analyser.isSeparator(input)) {
            processSeparator(input);
        } else if (Analyser.isOperator(input)) {
            processOperator(input);
        }

    }

    private void processCharacter(char character) {
        Status currentState = statusHelper.getCurrenStatus();
        if (character == '\'') {
            if (buffer.length() == 1 || isUnicode() || currentState == Status.SPECIFIC_CHAR) {
                if (new String(buffer).equals("\\")) {
                    tokens.add(createToken(TokenType.ERROR, new String(buffer)));
                }
                tokens.add(createToken(TokenType.CHARACTER, new String(buffer)));
            } else if (buffer.length() > 1) {
                if (isCharacterNumber()) {
                    tokens.add(createToken(TokenType.CHARACTER, new String(buffer)));
                } else {
                    tokens.add(createToken(TokenType.ERROR, new String(buffer)));
                }
            }
            initBuffer();
            statusHelper.setCurrenStatus(Status.START);
            isCharacter = false;
        } else if (character == 'u' && currentState == Status.BACKSLASH) {
            statusHelper.setCurrenStatus(Status.UNICODE_CHAR);
            buffer.append(character);
        } else if (currentState == Status.BACKSLASH && isSpecificSymbol(character)) {
            statusHelper.setCurrenStatus(Status.SPECIFIC_CHAR);
            buffer.append(character);
        } else if (character == '\\') {
            statusHelper.setCurrenStatus(Status.BACKSLASH);
            buffer.append(character);
        } else {
            buffer.append(character);
        }
    }

    private boolean isCharacterNumber() {
        return new String(buffer).matches("\\\\[1-3][0-6][0-7]");
    }

    private boolean isSpecificSymbol(char input) {
        Character[] symbols = {'n', 't', 'r', 'f', 'b', '\\'};
        return Arrays.asList(symbols).contains(input);
    }

    private boolean isUnicode() {
        return new String(buffer).matches("\\\\[u|U][1-9]{4}");
    }

    private boolean isCharacter() {
        return isCharacter;
    }

    private void processComment(char character) {
        Status currentState = statusHelper.getCurrenStatus();
        Status prevState = statusHelper.getPreviousStatus();

        if (currentState == Status.MULTILINE_COMMENT || prevState == Status.MULTILINE_COMMENT) {
            if (character == '*') {
                statusHelper.setCurrenStatus(Status.END_COMMENT);
            } else if (character == '/') {
                if (currentState == Status.END_COMMENT) {
                    buffer.append(character);
                    tokens.add(createToken(TokenType.COMMENT, new String(buffer)));
                    statusHelper.setCurrenStatus(Status.START);
                    initBuffer();
                    return;
                }
            }
        } else {
            if (character == '\n') {
                buffer.append(character);
                tokens.add(createToken(TokenType.COMMENT, new String(buffer)));
                statusHelper.setCurrenStatus(Status.START);
                statusHelper.setPreviousStatus(Status.START);
                initBuffer();
                return;
            }
        }
        buffer.append(character);
    }

    private boolean isComment() {
        return statusHelper.getCurrenStatus() == Status.INLINE_COMMENT
                || statusHelper.getPreviousStatus() == Status.INLINE_COMMENT
                || statusHelper.getCurrenStatus() == Status.MULTILINE_COMMENT
                || statusHelper.getPreviousStatus() == Status.MULTILINE_COMMENT
                || statusHelper.getCurrenStatus() == Status.END_COMMENT;
    }


    private void processOperator(char operator) {
        Status currenStatus = statusHelper.getCurrenStatus();


        if (operator == '-') {
            if (currenStatus != Status.NUMBER) {
                statusHelper.setCurrenStatus(Status.MINUS);
                buffer.append(operator);
            } else {
                statusHelper.setCurrenStatus(Status.OPERATOR);
                tokens.add(createToken(TokenType.OPERATOR, String.valueOf(operator)));
                initBuffer();
            }
        } else if (operator == '/') {
            if (statusHelper.getCurrenStatus() == Status.BACKSLASH) {
                statusHelper.setCurrenStatus(Status.INLINE_COMMENT);
            } else {
                statusHelper.setCurrenStatus(Status.BACKSLASH);
                buffer.append(operator);
            }
        } else if (operator == '*') {
            if (statusHelper.getCurrenStatus() == Status.BACKSLASH) {
                statusHelper.setCurrenStatus(Status.MULTILINE_COMMENT);
                buffer.append(operator);
            }
        } else if (currenStatus == Status.OPERATOR) {
            statusHelper.setCurrenStatus(Status.SECOND_OPERATOR);
            buffer.append(operator);
            tokens.add(createToken(TokenType.OPERATOR, new String(buffer)));
            initBuffer();
        } else {
            statusHelper.setCurrenStatus(Status.OPERATOR);
            buffer.append(operator);
        }

    }


    private boolean isString() {
        return statusHelper.getCurrenStatus() == Status.STRING;
    }

    private void processString(char input) {
        if (input == '"') {
            statusHelper.setCurrenStatus(Status.START);
            tokens.add(createToken(TokenType.STRING, new String(buffer)));
            initBuffer();
        } else {
            buffer.append(input);
        }
    }

    private void processLetter(char letter) {
        Status currenStatus = statusHelper.getCurrenStatus();
        if (currenStatus == Status.ZERO && (letter == 'X' || letter == 'x')) {
            statusHelper.setCurrenStatus(Status.NUMBER);
        } else if (currenStatus == Status.NUMBER && (letter == 'E' || letter == 'e')) {
            statusHelper.setCurrenStatus(Status.EXPONENTIAL);
        } else {
            statusHelper.setCurrenStatus(Status.LETTER);
        }
        buffer.append(letter);
    }

    private void processDigit(char number) {
        Status currenStatus = statusHelper.getCurrenStatus();
        if (currenStatus == Status.LETTER) {
            statusHelper.setCurrenStatus(Status.LETTER);
        } else if (number == '0') {
            statusHelper.setCurrenStatus(Status.ZERO);
        } else if (currenStatus == Status.EXPONENTIAL) {
            statusHelper.setCurrenStatus(Status.EXPONENTIAL);
        } else {
            statusHelper.setCurrenStatus(Status.NUMBER);
        }

        buffer.append(number);
    }


    private void processSeparator(char separator) {
        String word = new String(buffer);
        Status currenStatus = statusHelper.getCurrenStatus();

        if (occurrencesCount('.') > 1) {
            statusHelper.setCurrenStatus(Status.START);
            tokens.add(createToken(TokenType.ERROR, new String(buffer)));
            initBuffer();
            return;
        } else if (separator == '"' && currenStatus != Status.STRING) {
            statusHelper.setCurrenStatus(Status.STRING);
            return;
        } else if (separator == '\'') {
            statusHelper.setCurrenStatus(Status.CHARACTER);
            isCharacter = true;
            return;
        } else if (separator == '.' && (currenStatus == Status.NUMBER || currenStatus == Status.ZERO)) {
            buffer.append(separator);
            statusHelper.setCurrenStatus(Status.DOT);
            return;
        } else if (Analyser.isKeyword(word)) {
            tokens.add(createToken(TokenType.KEYWORD, word));
        } else if (currenStatus == Status.NUMBER || currenStatus == Status.DOT || currenStatus == Status.ZERO) {
            tokens.add(createToken(TokenType.NUMBER, word));
        } else if (currenStatus == Status.EXPONENTIAL) {
            if (isCorrectExponential()) {
                tokens.add(createToken(TokenType.EXPONENTIAL, new String(buffer)));
            } else {
                tokens.add(createToken(TokenType.ERROR, new String(buffer)));
            }
        } else if (currenStatus == Status.OPERATOR || currenStatus == Status.MINUS || currenStatus == Status.BACKSLASH) {
            tokens.add(createToken(TokenType.OPERATOR, new String(buffer)));
        }
        else if (!word.equals("")) {
            tokens.add(createToken(TokenType.IDENTIFIER, word));
        }
        tokens.add(createToken(TokenType.SEPARATOR, String.valueOf(separator)));
        statusHelper.setCurrenStatus(Status.SEPARATOR);
        initBuffer();
    }

    private boolean isCorrectExponential() {
        return new String(buffer).matches("\\d+\\.\\d+[e|E]\\d{1,255}");
    }

    private long occurrencesCount(char character) {
        return buffer.toString()
                .chars()
                .filter(ch -> ch == character)
                .count();
    }

    public void initBuffer() {
        buffer = new StringBuilder();
    }

    private Token createToken(TokenType type, String value) {
        return new Token(type, value);
    }

    public void printSortedTokens() {
        Set<Token> tokens1 = new HashSet<>(tokens);
        tokens1.stream()
                .sorted((o1, o2) -> {
                    {
                        if (o1.getType().ordinal() == o2.getType().ordinal()) {
                            return 0;
                        } else if (o1.getType().ordinal() > o2.getType().ordinal()) {
                            return 1;
                        } else {
                            return -1;
                        }
                    }
                })
                .forEach(System.out::println);
    }
}