package litil.lexer;

public interface Lexer {
    Token pop() throws LexingException;

    String getCurrentLine();
}
