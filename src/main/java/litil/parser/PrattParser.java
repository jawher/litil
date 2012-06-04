package litil.parser;

import litil.ast.*;
import litil.lexer.BaseLexer;
import litil.lexer.LookaheadLexerWrapper;
import litil.lexer.StructuredLexer;
import litil.lexer.Token;

import java.io.Reader;
import java.io.StringReader;
import java.util.*;

public class PrattParser extends BaseParser {
    private static Map<String, Integer> LBP = new HashMap<String, Integer>();

    public static void main(String[] args) throws Exception {
        LBP.put("=", 7);
        LBP.put("and", 5);
        LBP.put("or", 5);
        LBP.put("<", 7);
        LBP.put(">", 7);
        LBP.put("+", 10);
        LBP.put("-", 10);
        LBP.put("*", 20);
        LBP.put("/", 20);
        LBP.put("(", 100);
        LBP.put(")", 1);
        PrattParser p = new PrattParser();
        p.debug = true;

        Reader reader = (new StringReader("-a * -b"));

        p.lexer = new LookaheadLexerWrapper(new StructuredLexer(new BaseLexer(reader), "  "));
        //p.advance();
        Expr ast = p.expr(0);
        System.out.println(ast);
    }


    private int lbp(Token tk) {
        if(tk.type== Token.Type.NAME) {
            return 90;
        }else if(tk.type== Token.Type.NUM) {
            return 90;
        }
        Integer res = LBP.get(tk.text);
        if (res == null) {
            System.err.println(tab(depth) + "warn " + tk);
            return 0;
        } else {
            return res;
        }
    }

    private Expr nud(Token tk) {
        System.err.println(tab(depth) + "nud " + tk);
        if (is(tk, Token.Type.SYM, "-")) {
            depth++;
            Expr expr = expr(100);
            depth--;
            return new Expr.EAp(new Expr.EName("-/1"), expr);//unary minus
        } else if (is(tk, Token.Type.NUM)) {
            return new Expr.ENum(Integer.parseInt(tk.text));
        } else if (is(tk, Token.Type.NAME)) {
            return new Expr.EName(tk.text);
        } else if (is(tk, Token.Type.SYM, "(")) {
            depth++;
            Expr res = expr(1);
            depth--;
            expect(Token.Type.SYM, ")");
            return res;
        } else if (is(tk, Token.Type.KEYWORD, "if")) {
            depth++;
            Expr cond = expr(1);
            depth--;
            expect(Token.Type.KEYWORD, "then");
            depth++;
            Expr thenArm = expr(1);
            depth--;
            expect(Token.Type.KEYWORD, "else");
            depth++;
            Expr elseArm = expr(1);
            depth--;
            return new Expr.EIf(cond, Arrays.asList(thenArm), Arrays.asList(elseArm));
        } else {
            throw new IllegalArgumentException("Invalid symbol found " + tk);
        }

    }

    private Expr led(Expr left, Token tk) {
        System.err.println(tab(depth) + "led " + left + ", " + tk);
        int bp = lbp(tk);
        if (is(tk, Token.Type.SYM, "+") || is(tk, Token.Type.SYM, "-") || is(tk, Token.Type.SYM, "*") || is(tk, Token.Type.SYM, "/")
                || is(tk, Token.Type.SYM, "=") || is(tk, Token.Type.SYM, "%") || is(tk, Token.Type.SYM, "<") || is(tk, Token.Type.SYM, ">")
                || is(tk, Token.Type.KEYWORD, "and") || is(tk, Token.Type.KEYWORD, "or")) {
            depth++;
            Expr right = expr(bp);
            depth--;
            return new Expr.EAp(new Expr.EAp(new Expr.EName(tk.text), left), right);
        } else if(left instanceof Expr.EName || left instanceof Expr.EAp) {
            return new Expr.EAp(left, nud(tk));
        } else {
            throw error("Unexpected token " + tk);
        }
    }

    private boolean dbg(int rbp, Token tk) {
        System.err.println(tab(depth+1) + rbp + "<" + lbp(tk) + "? (tk=" + tk + ")");
        return true;
    }

    public Expr expr(int rbp) {
        System.err.println(tab(depth) + "expr " + rbp);
        Token tk = lexer.pop();
        depth++;
        Expr left = nud(tk);
        depth--;
        while (dbg(rbp, lexer.peek(1)) && rbp < lbp(lexer.peek(1))) {
            tk = lexer.pop();
            depth++;
            left = led(left, tk);
            depth--;
        }

        return left;
    }


    private int depth = 0;

    private String tab(int depth) {
        StringBuilder res = new StringBuilder("");
        for (int i = 0; i < depth; i++) {
            res.append("\t");
        }
        return res.toString();
    }

}
