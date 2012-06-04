package litil.parser;

import litil.lexer.LookaheadLexer;
import litil.lexer.Token;

public class BaseParser {
    protected LookaheadLexer lexer;
    protected Token token;
    public boolean debug = false;

    private void dbg(Token.Type k, String v, Token tk) {
        if (debug) {
            String w = Thread.currentThread().getStackTrace()[2].getMethodName();
            StackTraceElement caller = Thread.currentThread().getStackTrace()[3];
            StackTraceElement bcaller = Thread.currentThread().getStackTrace()[4];
            if ("expect".equals(caller.getMethodName())) {
                w = Thread.currentThread().getStackTrace()[3].getMethodName();
                caller = Thread.currentThread().getStackTrace()[4];
                bcaller = Thread.currentThread().getStackTrace()[5];
            }
            System.err.println(w + "(" + k + (v == null ? "" : "<" + v + ">") + ") and got " + tk + " at " + caller);//+"\n\tat "+bcaller);
        }
    }

    protected boolean is(Token tk, Token.Type key) {
        return tk.type == key;
    }

    protected boolean is(Token tk, Token.Type key, String value) {
        return (tk.type == key && tk.text.equals(value));
    }

    protected boolean lookahead(Token.Type key) {
        Token tk = lexer.peek(1);
        dbg(key, null, tk);
        return tk.type == key;
    }

    protected boolean lookahead(Token.Type key, String value) {
        Token tk = lexer.peek(1);
        dbg(key, value, tk);
        return tk.type == key && tk.text.equals(value);
    }

    protected Token advance() {
        token = lexer.pop();
        if(debug) {
            System.err.println("advance and got "+token);
        }
        return token;
    }

    protected boolean found(Token.Type key) {
        Token tk = lexer.peek(1);
        dbg(key, null, tk);
        if (tk.type == key) {
            lexer.pop();
            token = tk;
            return true;
        } else {
            return false;
        }
    }

    

    protected boolean found(Token.Type key, String value) {
        Token tk = lexer.peek(1);
        dbg(key, value, tk);
        if (tk.type == key && tk.text.equals(value)) {
            lexer.pop();
            token = tk;
            return true;
        } else {
            return false;
        }
    }

    protected void check(Token.Type key, String value) {
        dbg(key, value, token);
        if (token.type != key || !token.text.equals(value)) {
            throw error("Was expecting '" + value + "'(" + key + ")");
        }
    }

    protected void check(Token.Type key) {
        dbg(key, null, token);
        if (token.type != key) {
            throw error("Was expecting a token of type '" + key + "' but got " + token);
        }
    }

    protected void expect(Token.Type key) {
        if (!found(key)) {
            throw error("Was expecting a token of type '" + key + "' but got " + lexer.peek(1));
        }
    }

    protected void expect(Token.Type key, String value) {
        if (!found(key, value)) {
            throw error("Was expecting '" + value + "'(" + key + ")");
        }
    }

    protected RuntimeException error(String msg) {
        Token where = token == null ? lexer.peek(1) : token;
        System.err.println("error "+token);
        throw new RuntimeException(msg + " (line " + where.row +
                ")" + "\n" + lexer.getCurrentLine() + "\n" + indent(where.col - 1) + "^\n+token="+token);
    }

    protected String indent(int n) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < n; i++) {
            res.append(" ");
        }
        return res.toString();
    }
}