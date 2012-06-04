package litil.lexer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
modified version of lexer that
- handles one line comments (starting with --)
- produce NEWLINE token in the beggininng of newlines, except between consecutive DEINDENTS
 */
public class BaseLexer implements Lexer {
    private final BufferedReader reader;
    private Integer nextChar;
    private static final List<String> SYMBOLS = Arrays.asList("->", ".", "+", "-", "*", "/", "(", ")", "=", "%", "<", ">", ":", ",", "[", "]", "|", "_", "=>", "\\", "--", "::", "{", "}");
    private LexerStage rootStage = new LexerStage(SYMBOLS);
    private static final List<String> BOOLS = Arrays.asList("true", "false");
    private static final List<String> KEYWORDS = Arrays.asList("let", "if", "then", "else", "and", "or", "data", "match", "exception", "try", "catch", "throw");
    private int row = 1, col = 0;
    private String currentLine = null;
    private String nextLine = null;
    private int lastIndentLength = -1;
    private boolean newLine = true;

    public BaseLexer(Reader reader) {
        this.reader = new BufferedReader(reader);
    }

    public Token pop() throws LexingException {
        if (col != 0) {
            consumeWhite();
        }
        int peek = peek();
        if (newLine && peek != -1) {
            newLine = false;
            return new Token(Token.Type.NEWLINE, "\\n2", row, 1);
        }
        while ((peek = peek()) != -1) {
            if (Character.isDigit(peek)) {
                return readNum();
            } else if (rootStage.next((char) peek) != null) {
                return readSymOrComment();
            } else if (Character.isJavaIdentifierStart(peek)) {
                return readBoolOrKwOrName();
            } else if (peek == '"') {
                return readString();
            } else if (peek == '\'') {
                return readChar();
            } else if (peek == '\n' || peek == ' ') {
                Token res = readIndentOrNewLine();
                if (res != null) {
                    return res;
                }
            } else {
                throw new LexingException("Illegal character in input: " + peek, getCurrentLine(), row, col + 1);
            }
        }
        return new Token(Token.Type.EOF, "$", row, col);
    }

    private void consumeWhite() {
        while (Character.isWhitespace(peek()) && peek() != '\n') {
            advance();
        }
    }

    private Token readIndentOrNewLine() {
        StringBuilder white;
        do {
            if (peek() == '\n') {
                advance();//eat the \n
            }

            white = new StringBuilder();
            while (peek() == ' ') {
                white.append((char) advance());
            }
            if (peek() == '-') {
                advance();
                if (peek() == '-') {
                    while (peek() != '\n' && peek() != -1) {
                        advance();
                    }
                } else {
                    throw new LexingException("Illegal start of line (was expecting a second '-' for a line comment)", getCurrentLine(), row, col + 1);
                }
            }
        } while (peek() == '\n');

        String indent = white.toString();
        int lastIndentLengthCopy = lastIndentLength;
        lastIndentLength = indent.length();

        if (indent.length() > Math.max(lastIndentLengthCopy, 0) && peek() != -1) {
            newLine = true;
            return new Token(Token.Type.INDENT, indent, row, 1);
        } else if (indent.length() < lastIndentLengthCopy) {//we need to make sure to return deindent tokens, even if we reached the end
            newLine = true;
            return new Token(Token.Type.DEINDENT, indent, row, 1);
        } else if (/*lastIndentLengthCopy >= 0  why ? &&  */peek() != -1) {
            return new Token(Token.Type.NEWLINE, "\\n1", row + 1, 1);
        } else {
            return null;
        }
    }

    private Token readSymOrComment() {
        LexerStage cs = rootStage.next((char) advance());
        int thisCol = col;
        while (cs.next((char) peek()) != null) {
            cs = cs.next((char) advance());
        }
        if (cs.isTerminal()) {
            String value = cs.getValue();
            if ("--".equals(value)) {
                while (peek() != -1 && peek() != '\n') {
                    advance();
                }
                return pop();
            } else {
                return new Token(Token.Type.SYM, value, row, thisCol);
            }
        } else {
            throw new LexingException("Illegal character after the starting of a symbol '" + cs.getValue() + "'", getCurrentLine(), row, col + 1);
        }
    }

    private Token readBoolOrKwOrName() {
        StringBuilder res = new StringBuilder();
        res.append((char) advance());
        int thisCol = col;
        while (Character.isJavaIdentifierPart(peek())) {
            res.append((char) advance());
        }
        if (BOOLS.contains(res.toString())) {
            return new Token(Token.Type.BOOL, res.toString(), row, thisCol);
        } else if (KEYWORDS.contains(res.toString())) {
            return new Token(Token.Type.KEYWORD, res.toString(), row, thisCol);
        } else {
            return new Token(Token.Type.NAME, res.toString(), row, thisCol);
        }
    }

    private Token readNum() {
        StringBuilder res = new StringBuilder();
        res.append((char) advance());
        int thisCol = col;
        while (Character.isDigit(peek())) {
            res.append((char) advance());
        }
        return new Token(Token.Type.NUM, res.toString(), row, thisCol);
    }

    private Map<Character, Character> escapes = new HashMap<Character, Character>() {
        {
            put('t', '\t');
            put('b', '\b');
            put('r', '\r');
            put('n', '\n');
            put('f', '\f');
        }
    };

    private Token readString() {
        advance();//eat the opening "
        int thisCol = col;
        StringBuilder res = new StringBuilder();
        boolean escape = false;
        while (peek() != '\n' && peek() != -1) {
            char c = (char) advance();
            if (c == '"' && !escape) {
                return new Token(Token.Type.STRING, res.toString(), row, thisCol);
            } else if (c == '\\' && !escape) {
                escape = true;
            } else if (escape) {
                if (escapes.containsKey(c)) {
                    res.append(escapes.get(c));
                } else if (c == '"' || c == '\\') {
                    res.append(c);
                } else {
                    throw new LexingException("Invalid escape sequence '\\" + c + "'", getCurrentLine(), row, col - 1);
                }
                escape = false;
            } else {
                res.append(c);
            }
        }
        throw new LexingException("Unclosed string", getCurrentLine(), row, col + 1);
    }

    private Token readChar() {
        advance();//eat the opening '
        int thisCol = col;
        if (peek() == '\n' || peek() == -1) {
            throw new LexingException("Unclosed char", getCurrentLine(), row, col);
        }
        char c = (char) advance();
        if (c == '\'') {
            throw new LexingException("Empty char literal", getCurrentLine(), row, col);
        } else if (c == '\\') {
            if (escapes.containsKey((char) peek())) {
                String res = String.valueOf(escapes.get((char) advance()));
                if (peek() != '\'') {
                    throw new LexingException("Unclosed char literal", getCurrentLine(), row, col);
                }
                advance();
                return new Token(Token.Type.CHAR, res, row, thisCol);
            } else if (peek() == '\'' || peek() == '\\') {
                String res = String.valueOf((char) advance());
                if (peek() != '\'') {
                    throw new LexingException("Invalid char literal", getCurrentLine(), row, col);
                }
                advance();
                return new Token(Token.Type.CHAR, res, row, thisCol);
            } else {
                throw new LexingException("Invalid escape sequence", getCurrentLine(), row, col + 1);
            }
        } else {
            String res = String.valueOf(c);
            if (peek() != '\'') {
                throw new LexingException("Unclosed char literal", getCurrentLine(), row, col + 1);
            }
            advance();
            return new Token(Token.Type.CHAR, res, row, thisCol);
        }

    }

    private String readLine() {
        try {
            String s = reader.readLine();
            if (s != null) {
                s += "\n";
            }
            return s;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public String getCurrentLine() {
        if (currentLine == null) {
            return "<empty>";
        } else {
            return currentLine.replace("\n", "");
        }
    }

    private int peek() {
        if (nextChar == null) {
            if (currentLine == null) {
                currentLine = readLine();
                if (currentLine == null) {
                    nextChar = -1;
                } else {
                    //need to check if col<currentLine.length ?
                    nextChar = (int) currentLine.charAt(col);
                }
            } else {
                if (nextLine != null) {
                    currentLine = nextLine;
                    nextLine = null;
                    row++;
                    col = 1;
                    return peek();
                } else {
                    if (col >= currentLine.length()) {
                        nextLine = readLine();
                        if (nextLine == null) {
                            nextChar = -1;
                        } else {
                            nextChar = (int) nextLine.charAt(0);
                        }
                    } else {
                        nextChar = (int) currentLine.charAt(col);
                    }
                }
            }


        }
        return nextChar;
    }

    private int advance() {
        int res = peek();
        if (res != -1) {
            nextChar = null;
            col++;
        }
        return res;
    }

}
