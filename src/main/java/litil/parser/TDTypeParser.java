package litil.parser;

import litil.ast.Type;
import litil.lexer.BaseLexer;
import litil.lexer.LookaheadLexer;
import litil.lexer.LookaheadLexerWrapper;
import litil.lexer.Token;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TDTypeParser extends BaseParser {
    public TDTypeParser(LookaheadLexer lexer) {
        super.lexer = lexer;
    }

    public static void main(String[] args) throws Exception {
        Reader reader = new InputStreamReader(LitilParser.class.getResourceAsStream("test.types"));
        TDTypeParser p = new TDTypeParser(new LookaheadLexerWrapper(new BaseLexer(reader)));
        p.debug = false;

        p.types();

    }

    private List<Type> types() {
        List<Type> res = new ArrayList<Type>();
        Type t = type();
        res.add(t);
        System.err.println(t);
        while (found(Token.Type.NEWLINE)) {
            t = type();
            res.add(t);
            System.err.println(t + "::" + t.getClass());
        }

        return res;
    }

    private Type type() {
        return funcType(new HashMap<String, Type.Variable>());
    }

    private Type type(Map<String, Type.Variable> mappings) {
        return funcType(mappings);
    }

    private Type funcType(Map<String, Type.Variable> mappings) {
        Type arg = prodType(mappings);
        if (found(Token.Type.SYM, "->")) {
            Type res = funcType(mappings);
            return Type.Function(arg, res);
        }
        return arg;
    }

    private Type prodType(Map<String, Type.Variable> mappings) {
        List<Type> args = new ArrayList<Type>();
        args.add(paramType(mappings));
        while (found(Token.Type.SYM, "*")) {
            args.add(paramType(mappings));

        }
        return args.size() == 1 ? args.get(0) : Type.Product(args);
    }

    private Type paramType(Map<String, Type.Variable> mappings) {
        if (found(Token.Type.NAME)) {
            String name = token.text;

            if (Character.isLowerCase(name.charAt(0))) {
                Type.Variable res;
                if (mappings.containsKey(name)) {
                    res = mappings.get(name);
                } else {
                    Type.Variable v = new Type.Variable();
                    mappings.put(name, v);
                    res = v;
                }
                if (lookahead(Token.Type.SYM, "(") || lookahead(Token.Type.SYM, "[") || lookahead(Token.Type.NAME)) {
                    throw error("Invalid type declaration: variables cannot be parameterized");
                } else {
                    return res;
                }
            } else {
                List<Type> args = new ArrayList<Type>();
                while (lookahead(Token.Type.SYM, "(") || lookahead(Token.Type.SYM, "[") || lookahead(Token.Type.NAME)) {
                    args.add(atomType(mappings));
                }
                if (args.isEmpty()) {
                    return typeFor(name);
                } else {
                    return new Type.Oper(name, args);
                }
            }
        } else {
            return atomType(mappings);
        }

    }

    private Type atomType(Map<String, Type.Variable> mappings) {
        if (found(Token.Type.SYM, "(")) {
            Type res = type(mappings);
            expect(Token.Type.SYM, ")");
            return res;
        } else if (found(Token.Type.SYM, "[")) {
            Type res = type(mappings);
            expect(Token.Type.SYM, "]");
            return Type.List(res);
        } else if (found(Token.Type.NAME)) {
            String name = token.text;
            if (Character.isLowerCase(name.charAt(0))) {
                if (mappings.containsKey(name)) {
                    return mappings.get(name);
                } else {
                    Type.Variable v = new Type.Variable();
                    mappings.put(name, v);
                    return v;
                }
            } else {
                return typeFor(token.text);
            }
        } else {
            throw error("Unexpected token");
        }
    }

    private Type typeFor(String name) {
        if("Number".equals(name)) {
            return Type.INT;
        } else if("String".equals(name)) {
            return Type.STR;
        } else if("Char".equals(name)) {
            return Type.CHAR;
        } else if("Bool".equals(name)) {
            return Type.BOOL;
        } else {
            return new Type.Oper("_"+name);
        }
    }


}
