package litil.lexer;

public interface LookaheadLexer extends Lexer {

    Token peek(int lookahead) throws LexingException;
}
