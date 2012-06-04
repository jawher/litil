package litil.parser;

import litil.ast.Type;
import litil.lexer.*;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

public class PrattTypeParser extends BaseParser {
    public PrattTypeParser(LookaheadLexer lexer) {
        super.lexer = lexer;
    }

    public static void main(String[] args) throws Exception {
        Reader reader = new InputStreamReader(LitilParser.class.getResourceAsStream("test.types"));
        PrattTypeParser p = new PrattTypeParser(new LookaheadLexerWrapper(new BaseLexer(reader)));
        p.debug = false;

        p.types();

    }

    private List<Type> types() {
        List<Type> res = new ArrayList<Type>();
        Type t = type();
        res.add(t);
        while (found(Token.Type.NEWLINE)) {
            res.add(type());
        }
        return res;
    }

    private Type type() {
        return type(0);
    }

    private static Map<String, Integer> LBP = new HashMap<String, Integer>();

    static {
        LBP.put("*", 20);
        LBP.put("(", 100);
        LBP.put(")", 1);
    }

    private int lbp(Token tk) {
        if (tk.type == Token.Type.NAME) {
            return 90;
        }
        Integer res = LBP.get(tk.text);
        if (res == null) {
            dbg("warn " + tk);
            return 0;
        } else {
            return res;
        }
    }

    private Type nud(Token tk) {
        dbg("nud " + tk);
        if (is(tk, Token.Type.NAME)) {
            return new Type.Oper(tk.text);
        } else if (is(tk, Token.Type.SYM, "(")) {
            if (found(Token.Type.SYM, ")")) {
                return Type.UNIT;
            }
            depth++;

            Type res = type(1);
            depth--;
            expect(Token.Type.SYM, ")");
            return res;
        } else {
            throw new IllegalArgumentException("Invalid symbol found " + tk);
        }

    }

    private Type led(Type left, Token tk) {
        dbg("led " + left + ", " + tk);
        int bp = lbp(tk);
        if (is(tk, Token.Type.SYM, "*")) {
            depth++;
            //Expr right = type(bp);
            depth--;
            //return new Expr.EAp(new Expr.EAp(new Expr.EName(tk.text), left), right);
       // } else if (left instanceof Expr.EName || left instanceof Expr.EAp) {
        //    return new Expr.EAp(left, nud(tk));
        //} else {
            throw error("Unexpected token " + tk);
        }
        return null;
    }

    private boolean dbg(int rbp, Token tk) {
        depth++;
        dbg(rbp + "<" + lbp(tk) + "? (tk=" + tk + ")");
        depth--;
        return true;
    }

    public Type type(int rbp) {
        dbg("type " + rbp);
        Token tk = lexer.pop();
        dbg("pop0 " + tk);
        depth++;
        Type left = nud(tk);
        depth--;
        while (dbg(rbp, lexer.peek(1)) && rbp < lbp(lexer.peek(1))) {
            tk = lexer.pop();
            dbg("pop " + tk);
            depth++;
            left = led(left, tk);
            depth--;
        }
        return left;
    }


    private int depth = 0;
    public boolean prtDbg = false;

    private String tab(int depth) {
        StringBuilder res = new StringBuilder("");
        for (int i = 0; i < depth; i++) {
            res.append("\t");
        }
        return res.toString();
    }

    private void dbg(String msg) {
        if (prtDbg) {
            System.err.println(tab(depth) + msg);
        }
    }


}
