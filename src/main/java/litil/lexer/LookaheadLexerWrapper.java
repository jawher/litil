package litil.lexer;

import java.util.ArrayList;
import java.util.List;

public class LookaheadLexerWrapper implements LookaheadLexer {
    private static final class TokenAndLine {
        public final Token token;
        public final String line;

        private TokenAndLine(Token token, String line) {
            this.token = token;
            this.line = line;
        }
    }

    public final Lexer delegate;
    private List<TokenAndLine> tokens = new ArrayList<TokenAndLine>();
    private String currentLine = null;

    public LookaheadLexerWrapper(Lexer delegate) {
        this.delegate = delegate;
    }

    public Token pop() {
        if (!tokens.isEmpty()) {
            TokenAndLine res = tokens.get(0);
            tokens = tokens.subList(1, tokens.size());
            currentLine = res.line;
            return res.token;
        } else {
            Token res = delegate.pop();
            currentLine = delegate.getCurrentLine();
            return res;
        }
    }

    public Token peek(int lookahead) throws LexingException {
        if (lookahead < 1) {
            throw new IllegalArgumentException("Invalid look ahead value (must be greater than 1)");
        }
        int size = tokens.size();
        if (size < lookahead) {
            for (int i = 0; i < lookahead - size; i++) {
                tokens.add(new TokenAndLine(delegate.pop(), delegate.getCurrentLine()));
            }
        }
        return tokens.get(lookahead - 1).token;
    }

    public String getCurrentLine() {
        return currentLine;
    }
}
