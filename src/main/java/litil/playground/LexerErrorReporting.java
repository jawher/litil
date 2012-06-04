package litil.playground;

import litil.lexer.*;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Hello world!
 */
public class LexerErrorReporting {


    private void structured(String file) {
        Lexer lex = new BaseLexer(new InputStreamReader(LexerErrorReporting.class.getResourceAsStream(file)));
        lexer = new LookaheadLexerWrapper(new StructuredLexer(lex, "  "));
        debug = true;
        System.err.println(body());
        expect(Token.Type.EOF);
    }

    private void structured2(String file) {
        Lexer lex = new BaseLexer(new InputStreamReader(App.class.getResourceAsStream(file)));
        StructuredLexer slex = new StructuredLexer(lex, "  ");
        Token tk = null;
        do {
            tk = slex.pop();

            System.out.println(tk);
        } while (tk.type != Token.Type.EOF);

    }

    private void base(String file) {
        Lexer lex = new BaseLexer(new InputStreamReader(App.class.getResourceAsStream(file)));

        Token tk = null;
        do {
            tk = lex.pop();

            System.out.println(tk);
        } while (tk.type != Token.Type.EOF);

    }


    private int depth = 0;

    private List<Object> bloc() {
        System.err.println("bloc at level " + depth);
        expect(Token.Type.INDENT);
        depth++;
        List<Object> res = body();
        depth--;
        expect(Token.Type.DEINDENT);
        return res;
    }


    private List<Object> body() {
        System.err.println("body at level " + depth);
        List<Object> res = new ArrayList<Object>();
        while (found(Token.Type.NEWLINE)) {
            res.add(instr());
        }

        return res;
    }

    private Object instr() {
        System.err.println("instr at level " + depth);
        expect(Token.Type.NAME);
        String nme = token.text;
        if (token.text.startsWith("if")) {
            List<Object> bloc = bloc();
            bloc.add(0, nme);
            return bloc;
        } else {
            return token.text;
        }
    }

    protected void expect(Token.Type key) {
        if (!found(key)) {
            throw error("Was expecting a token of type '" + key + "' but got " + lexer.peek(1));
        }
    }

    protected RuntimeException error(String msg) {
        Token where = token == null ? lexer.peek(1) : token;
        throw new RuntimeException(msg + " (line " + where.row +
                ")" + "\n" + "<line>" + "\n" + indent(where.col - 1) + "^\n+token=" + token);
    }

    protected String indent(int n) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < n; i++) {
            res.append(" ");
        }
        return res.toString();
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
        if (debug) {
            System.err.println("advance and got " + token);
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


    public static void main(String[] args) {
        //baseAndLookahead();
        for (int i = 1; i <= 10; i++) {
            try {
                System.out.println("----------------------------------");
                System.out.println("TEST "+i);
                new LexerErrorReporting().structured2("lexer/test" + i + ".err");
            } catch (LexingException e) {
                e.printStackTrace();
            }
        }

        for (int i = 1; i <= 3; i++) {
            try {
                System.out.println("----------------------------------");
                System.out.println("STEST "+i);
                new LexerErrorReporting().structured2("lexer/stest" + i + ".err");
            } catch (LexingException e) {
                e.printStackTrace();
            }
        }
    }
}
