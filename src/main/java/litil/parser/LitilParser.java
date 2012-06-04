package litil.parser;

import litil.TypeScope;
import litil.ast.*;
import litil.lexer.*;
import litil.tc.ExplicitTypeChecker;
import litil.tc.HMTypeChecker;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

public class LitilParser extends BaseParser {
    private static Map<String, Integer> LBP = new HashMap<String, Integer>();

    static {

        LBP.put("and", 5);
        LBP.put("or", 5);
        LBP.put("=", 7);
        LBP.put("<", 7);
        LBP.put(">", 7);
        LBP.put("::", 11);
        LBP.put("+", 15);
        LBP.put("-", 15);
        LBP.put("%", 20);
        LBP.put("*", 20);
        LBP.put("/", 20);
        LBP.put(".", 30);
        LBP.put("{", 90);
        LBP.put("}", 1);
        LBP.put("[", 90);
        LBP.put("]", 1);
        LBP.put("(", 100);
        LBP.put(")", 1);
    }

    public LitilParser(LookaheadLexer lexer) {
        super.lexer = lexer;
        prtDbg = false;
    }

    private static TypeScope trootScope() {
        TypeScope res = new TypeScope();
        res.define("+", Type.Function(Arrays.asList(Type.INT, Type.INT), Type.INT));
        res.define("add", Type.Function(Arrays.asList(Type.INT, Type.INT), Type.INT));
        //res.define("-", Type.Function(Arrays.asList(Type.INT, Type.INT), Type.INT));
        res.define("*", Type.Function(Arrays.asList(Type.INT, Type.INT), Type.INT));
        //res.define("/", Type.Function(Arrays.asList(Type.INT, Type.INT), Type.INT));
        //res.define("%", Type.Function(Arrays.asList(Type.INT, Type.INT), Type.INT));
        //res.define("=", Type.Function(Arrays.asList(Type.INT, Type.INT), Type.BOOL));

        //res.define("int2str", Type.Function(Type.INT, Type.STR));
        //res.define("print", Type.Function(Type.STR, Type.UNIT));
        return res;
    }

    public static void main(String[] args) throws Exception {


        Reader reader = new InputStreamReader(LitilParser.class.getResourceAsStream("../eval/emb-list.ltl"));
        LitilParser p = new LitilParser(new LookaheadLexerWrapper(new StructuredLexer(new BaseLexer(reader), "  ")));
        p.debug = false;


        Program ast = p.program();

        System.out.println(ast.repr(1));

        ExplicitTypeChecker exTc = new ExplicitTypeChecker();
        HMTypeChecker hmTc = new HMTypeChecker();
        TypeScope tenv = trootScope().child();
        for (Instruction instr : ast.instructions) {
            System.out.println(instr.repr(0));

            System.out.println(":: " + hmTc.analyze(instr, tenv));
            System.out.println("--------------");
        }

        System.out.println(tenv);

        //System.out.println(exTc.analyze(ast));
    }

    public Program program() {
        Program p = new Program();
        //p.scope = rootScope();
        p.instructions.addAll(body());
        return p;
    }

    private TypeScope rootScope() {
        TypeScope res = new TypeScope();
        res.define("+", Type.Function(Arrays.asList(Type.INT, Type.INT), Type.INT));
        res.define("-", Type.Function(Arrays.asList(Type.INT, Type.INT), Type.INT));
        res.define("*", Type.Function(Arrays.asList(Type.INT, Type.INT), Type.INT));
        res.define("/", Type.Function(Arrays.asList(Type.INT, Type.INT), Type.INT));
        res.define("%", Type.Function(Arrays.asList(Type.INT, Type.INT), Type.INT));
        res.define("=", Type.Function(Arrays.asList(Type.INT, Type.INT), Type.BOOL));

        res.define("int2str", Type.Function(Type.INT, Type.STR));
        res.define("print", Type.Function(Type.STR, Type.UNIT));
        return res;
    }

    private List<Instruction> body() {
        List<Instruction> res = new ArrayList<Instruction>();
        while (found(Token.Type.NEWLINE)) {
            res.add(instruction());
        }
        return res;
    }


    private Instruction instruction() {
        if (found(Token.Type.KEYWORD, "let")) {
            return destructuringLetBinding();
        } else if (found(Token.Type.KEYWORD, "data")) {
            return dataDecl();
        } else if (found(Token.Type.KEYWORD, "exception")) {
            return exceptionDecl();
        } else {
            return expr();
        }
    }

    private LetBinding letBinding() {
        expect(Token.Type.NAME);
        String functionName = token.text;
        Type functionReturnType = null;
        List<Named> args = params();
        List<Type> argTypes = new ArrayList<Type>(args.size());
        for (Named arg : args) {
            argTypes.add(arg.type);
        }
        if (found(Token.Type.SYM, ":")) {
            functionReturnType = type();
            Type letType = argTypes.isEmpty() ? functionReturnType : Type.Function(argTypes, functionReturnType);
        }
        expect(Token.Type.SYM, "=");
        List<Instruction> instructions = bloc();
        LetBinding letBinding = new LetBinding(functionName, functionReturnType, args, instructions);
        return letBinding;
    }

    // will eventually replace the basic let binding
    private DestructuringLetBinding destructuringLetBinding() {
        Pattern main = atomicPattern();
        List<Pattern> args = new ArrayList<Pattern>();
        while (!found(Token.Type.SYM, "=")) {
            args.add(atomicPattern());
        }


        List<Instruction> instructions = bloc();
        DestructuringLetBinding letBinding = new DestructuringLetBinding(main, args, instructions);
        return letBinding;
    }

    public ExceptionDecl exceptionDecl() {
        expect(Token.Type.NAME);
        String name = token.text;
        if (Character.isLowerCase(name.charAt(0))) {
            throw error("Illegal exception name (starts with a lower case): " + name);
        }

        List<Type> argTypes = new ArrayList<Type>();
        while (lookahead(Token.Type.NAME) || lookahead(Token.Type.SYM, "(") || lookahead(Token.Type.SYM, "[")) {
            Type argType = atomType(Collections.<String, Type.Variable>emptyMap(), true);
            argTypes.add(argType);
        }

        return new ExceptionDecl(name, argTypes);
    }

    public DataDecl dataDecl() {
        expect(Token.Type.NAME);
        String name = token.text;
        if (Character.isLowerCase(name.charAt(0))) {
            throw error("Illegal datatype name (starts with a lower case): " + name);
        }
        List<Type.Variable> vars = new ArrayList<Type.Variable>();
        Map<String, Type.Variable> mappings = new HashMap<String, Type.Variable>();
        while (found(Token.Type.NAME)) {
            Type.Variable var = varType(token.text, mappings, false);
            if (var == null) {
                throw error("datatype declaration can only be parameterized with vars");
            } else {
                vars.add(var);
            }
        }
        Type dataType = new Type.Oper(name, vars);
        expect(Token.Type.SYM, "=");

        List<DataDecl.TypeConstructor> tyCons = new ArrayList<DataDecl.TypeConstructor>();
        tyCons.add(typeConstructor(mappings));

        while (found(Token.Type.SYM, "|")) {
            tyCons.add(typeConstructor(mappings));
        }
        DataDecl res = new DataDecl(name, dataType, vars, tyCons);
        return res;
    }

    private DataDecl.TypeConstructor typeConstructor(Map<String, Type.Variable> mappings) {
        expect(Token.Type.NAME);
        String name = token.text;
        List<Type> argTypes = new ArrayList<Type>();
        while (lookahead(Token.Type.NAME) || lookahead(Token.Type.SYM, "(") || lookahead(Token.Type.SYM, "{") || lookahead(Token.Type.SYM, "[")) {
            Type argType = atomType(mappings, true);
            argTypes.add(argType);

        }
        return new DataDecl.TypeConstructor(name, argTypes);
    }

    private List<Instruction> bloc() {
        List<Instruction> instructions = new ArrayList<Instruction>();
        if (found(Token.Type.INDENT)) {
            instructions = body();
            expect(Token.Type.DEINDENT);
        } else {
            instructions.add(expr(0));
        }
        return instructions;
    }

    private List<Named> params() {
        List<Named> args = new ArrayList<Named>();
        while (lookahead(Token.Type.NAME) || lookahead(Token.Type.SYM, "(")) {
            args.add(param());
        }
        return args;
    }

    private Named param() {
        if (found(Token.Type.NAME)) {
            return new Named(token.text);
        } else if (found(Token.Type.SYM, "(")) {
            expect(Token.Type.NAME);
            String argName = token.text;
            expect(Token.Type.SYM, ":");
            Type argType = type();
            expect(Token.Type.SYM, ")");
            return new Named(argName, argType);
        } else {
            throw error("Invalid function argument: was expecting an argument name or a typed argument");
        }
    }

    private Type type() {
        return funcType(new HashMap<String, Type.Variable>(), false);
    }

    private Type type(Map<String, Type.Variable> mappings, boolean noNewVars) {
        return funcType(mappings, noNewVars);
    }

    private Type funcType(Map<String, Type.Variable> mappings, boolean noNewVars) {
        Type arg = prodType(mappings, noNewVars);
        if (found(Token.Type.SYM, "->")) {
            Type res = funcType(mappings, noNewVars);
            return Type.Function(arg, res);
        }
        return arg;
    }

    private Type prodType(Map<String, Type.Variable> mappings, boolean noNewVars) {
        List<Type> args = new ArrayList<Type>();
        args.add(paramType(mappings, noNewVars));
        while (found(Token.Type.SYM, "*")) {
            args.add(paramType(mappings, noNewVars));

        }
        return args.size() == 1 ? args.get(0) : Type.Product(args);
    }

    private Type paramType(Map<String, Type.Variable> mappings, boolean noNewVars) {
        if (found(Token.Type.NAME)) {
            String name = token.text;
            Type.Variable var = varType(name, mappings, noNewVars);
            if (var != null) {
                return var;
            } else {
                List<Type> args = new ArrayList<Type>();
                while (lookahead(Token.Type.SYM, "(") || lookahead(Token.Type.SYM, "[") || lookahead(Token.Type.NAME)) {
                    args.add(atomType(mappings, noNewVars));
                }
                if (args.isEmpty()) {
                    return typeFor(name);
                } else {
                    return new Type.Oper(name, args);
                }
            }
        } else {
            return atomType(mappings, noNewVars);
        }

    }

    private Type atomType(Map<String, Type.Variable> mappings, boolean noNewVars) {
        if (found(Token.Type.SYM, "(")) {
            Type res = type(mappings, noNewVars);
            expect(Token.Type.SYM, ")");
            return res;
        } else if (found(Token.Type.SYM, "[")) {
            Type res = type(mappings, noNewVars);
            expect(Token.Type.SYM, "]");
            return Type.List(res);
        } else if (found(Token.Type.SYM, "{")) {
            Map<String, Type> types = new HashMap<String, Type>();
            expect(Token.Type.NAME);
            String name = token.text;
            expect(Token.Type.SYM, ":");
            Type type = type(mappings, noNewVars);
            types.put(name, type);

            while (found(Token.Type.SYM, ",")) {
                expect(Token.Type.NAME);
                name = token.text;
                expect(Token.Type.SYM, ":");
                type = type(mappings, noNewVars);
                types.put(name, type);
            }

            expect(Token.Type.SYM, "}");
            return new Type.RecordType(types);
        } else if (found(Token.Type.NAME)) {
            String name = token.text;
            Type.Variable var = varType(name, mappings, noNewVars);
            if (var != null) {
                return var;
            } else {
                return typeFor(token.text);
            }
        } else {
            throw error("Unexpected token");
        }
    }

    private Type.Variable varType(String name, Map<String, Type.Variable> mappings, boolean noNewVars) {
        if (Character.isLowerCase(name.charAt(0))) {
            if (mappings.containsKey(name)) {
                return mappings.get(name);
            } else if (noNewVars) {
                throw error("Unbound variable " + name);
            } else {
                Type.Variable v = new Type.Variable();
                mappings.put(name, v);
                return v;
            }
        } else {
            return null;
        }
    }

    private Type typeFor(String name) {
        if ("Number".equals(name)) {
            return Type.INT;
        } else if ("String".equals(name)) {
            return Type.STR;
        } else if ("Char".equals(name)) {
            return Type.CHAR;
        } else if ("Bool".equals(name)) {
            return Type.BOOL;
        } else {
            return new Type.Oper(name);
        }
    }

    private Expr expr() {
        if (found(Token.Type.SYM, "\\")) {
            List<Named> params = params();
            if (params.isEmpty()) {
                throw error("Lambda expressions take at least one argument");
            }
            Type type = null;
            if (found(Token.Type.SYM, ":")) {
                type = type();
            }
            expect(Token.Type.SYM, "->");
            List<Instruction> body = bloc();
            return new Expr.ELam(params, type, body);
        } else if (found(Token.Type.KEYWORD, "if")) {
            Expr cond = expr(0);
            expect(Token.Type.KEYWORD, "then");
            List<Instruction> thenBody = bloc();
            System.err.println("@ " + cond + " " + thenBody);
            found(Token.Type.NEWLINE);
            expect(Token.Type.KEYWORD, "else");
            List<Instruction> elseBody = bloc();
            return new Expr.EIf(cond, thenBody, elseBody);
        } else if (found(Token.Type.KEYWORD, "match")) {
            Expr input = expr(0);
            expect(Token.Type.INDENT);
            List<Expr.PatterMatching.Case> cases = new ArrayList<Expr.PatterMatching.Case>();
            while (found(Token.Type.NEWLINE)) {
                cases.add(patMatchCase());
            }
            expect(Token.Type.DEINDENT);
            return new Expr.PatterMatching(input, cases);

        } else if (found(Token.Type.KEYWORD, "try")) {
            List<Instruction> tryBody = bloc();
            expect(Token.Type.NEWLINE);
            expect(Token.Type.KEYWORD, "catch");
            expect(Token.Type.INDENT);
            List<Expr.PatterMatching.Case> cases = new ArrayList<Expr.PatterMatching.Case>();
            while (found(Token.Type.NEWLINE)) {
                cases.add(patMatchCase());
            }
            expect(Token.Type.DEINDENT);
            return new Expr.TryCatch(tryBody, cases);
        } else {
            return expr(0);
        }
    }

    private Expr.PatterMatching.Case patMatchCase() {
        Pattern pat = pattern();
        expect(Token.Type.SYM, "=>");
        List<Instruction> instructions;
        if (lookahead(Token.Type.INDENT)) {
            instructions = bloc();
        } else {
            Expr outcome = expr(0);
            instructions = Arrays.<Instruction>asList(outcome);
        }
        List<Expr.PatterMatching.Case> cases = new ArrayList<Expr.PatterMatching.Case>();
        return new Expr.PatterMatching.Case(pat, instructions);
    }

    private Pattern pattern() {
        if (lookahead(Token.Type.SYM, "(")) {
            return maybeHeadTailPattern(maybeTuplePattern());
        } else if (lookahead(Token.Type.SYM, "[")) {
            return maybeHeadTailPattern(listPattern());
        } else if (found(Token.Type.NAME)) {
            String name = token.text;
            if (Character.isLowerCase(name.charAt(0))) {
                return maybeHeadTailPattern(new Pattern.IdPattern(name));
            } else {
                List<Pattern> patterns = new ArrayList<Pattern>();

                while (lookahead(Token.Type.SYM, "(") || lookahead(Token.Type.SYM, "[") || lookahead(Token.Type.NAME) || lookahead(Token.Type.SYM, "_")) {//first(pattern)
                    patterns.add(atomicPattern());
                }
                return new Pattern.TyConPattern(name, patterns);
            }
        } else if (found(Token.Type.SYM, "_")) {
            return maybeHeadTailPattern(new Pattern.WildcardPattern());
        } else {
            throw error("Unknow pattern format");
        }
    }

    private Pattern maybeHeadTailPattern(Pattern head) {
        if (found(Token.Type.SYM, "::")) {
            Pattern tail = pattern();
            return new Pattern.TyConPattern("Cons", Arrays.asList(head, tail));
        } else {
            return head;
        }
    }

    private Pattern atomicPattern() {
        if (lookahead(Token.Type.SYM, "(")) {
            return maybeTuplePattern();
        } else if (lookahead(Token.Type.SYM, "[")) {
            return listPattern();
        } else if (found(Token.Type.NAME)) {
            String name = token.text;
            if (Character.isLowerCase(name.charAt(0))) {
                return new Pattern.IdPattern(name);
            } else {
                return new Pattern.TyConPattern(name, Collections.<Pattern>emptyList());
            }
        } else if (found(Token.Type.SYM, "_")) {
            return new Pattern.WildcardPattern();
        } else {
            throw error("Unknow pattern format");
        }
    }

    private Pattern maybeTuplePattern() {
        expect(Token.Type.SYM, "(");
        List<Pattern> patterns = new ArrayList<Pattern>();
        patterns.add(pattern());
        while (found(Token.Type.SYM, ",")) {
            patterns.add(pattern());
        }
        expect(Token.Type.SYM, ")");
        if (patterns.size() == 1) {
            return patterns.get(0);
        } else {
            return new Pattern.TuplePattern(patterns);
        }
    }

    private Pattern listPattern() {
        expect(Token.Type.SYM, "[");
        if (found(Token.Type.SYM, "]")) {
            return makeList(Collections.<Pattern>emptyList());
        } else {
            List<Pattern> patterns = new ArrayList<Pattern>();
            patterns.add(pattern());
            while (found(Token.Type.SYM, ",")) {
                patterns.add(pattern());
            }
            expect(Token.Type.SYM, "]");
            return makeList(patterns);
        }

    }

    private Pattern makeList(List<Pattern> patterns) {
        if (patterns.isEmpty()) {
            return new Pattern.TyConPattern("Nil", Collections.<Pattern>emptyList());
        } else {
            return new Pattern.TyConPattern("Cons", Arrays.asList(patterns.get(0), makeList(patterns.subList(1, patterns.size()))));
        }
    }


    private int lbp(Token tk) {
        if (tk.type == Token.Type.NAME) {
            return 90;
        } else if (tk.type == Token.Type.NUM || tk.type == Token.Type.STRING || tk.type == Token.Type.CHAR || tk.type == Token.Type.BOOL) {
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

    private Expr nud(Token tk) {
        dbg("nud " + tk);
        if (is(tk, Token.Type.SYM, "-")) {
            depth++;
            Expr expr = expr(100);
            depth--;
            return new Expr.EAp(new Expr.EName("-/1"), expr);//unary minus
        } else if (is(tk, Token.Type.NUM)) {
            return new Expr.ENum(Integer.parseInt(tk.text));
        } else if (is(tk, Token.Type.BOOL)) {
            return new Expr.EBool(Boolean.parseBoolean(tk.text));
        } else if (is(tk, Token.Type.NAME)) {
            return new Expr.EName(tk.text);
        } else if (is(tk, Token.Type.STRING)) {
            return new Expr.EStr(tk.text);
        } else if (is(tk, Token.Type.CHAR)) {
            return new Expr.EChar(tk.text);
        } else if (is(tk, Token.Type.SYM, "(")) {
            if (found(Token.Type.SYM, ")")) {
                return Expr.EUnit;
            }
            depth++;

            List<Expr> res = new ArrayList<Expr>();
            res.add(expr(1));

            while (found(Token.Type.SYM, ",")) {
                res.add(expr(1));
            }
            depth--;
            expect(Token.Type.SYM, ")");

            return res.size() == 1 ? res.get(0) : new Expr.ETuple(res);
        } else if (is(tk, Token.Type.SYM, "[")) {


            List<Expr> res = new ArrayList<Expr>();

            if (found(Token.Type.SYM, "]")) {
                return makeList(res);
            } else {
                res.add(expr(1));
                while (found(Token.Type.SYM, ",")) {
                    res.add(expr(1));
                }
                expect(Token.Type.SYM, "]");
                return makeList(res);
            }

        } else if (is(tk, Token.Type.SYM, "{")) {

            depth++;

            Map<String, Expr> res = new HashMap<String, Expr>();
            expect(Token.Type.NAME);
            String name = token.text;
            expect(Token.Type.SYM, ":");
            Expr value = expr(1);
            res.put(name, value);
            while (found(Token.Type.SYM, ",")) {
                expect(Token.Type.NAME);
                name = token.text;
                expect(Token.Type.SYM, ":");
                value = expr(1);
                res.put(name, value);
            }
            depth--;
            expect(Token.Type.SYM, "}");

            return new Expr.ERecord(res);
        } else if (is(tk, Token.Type.SYM, "\\")) {
            List<Named> params = params();
            if (params.isEmpty()) {
                throw error("Lambda expressions take at least one argument");
            }
            Type type = null;
            if (found(Token.Type.SYM, ":")) {
                type = type();
            }
            expect(Token.Type.SYM, "->");
            depth++;
            Expr res = expr(1);
            depth--;
            return new Expr.ELam(params, type, Arrays.<Instruction>asList(res));
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
        } else if (is(tk, Token.Type.KEYWORD, "throw")) {
            depth++;
            Expr ex = expr(1);
            depth--;
            return new Expr.EThrow(ex);
        } else {
            if (is(tk, Token.Type.NEWLINE) || is(tk, Token.Type.INDENT) || is(tk, Token.Type.DEINDENT)) {
                throw error("Unfinished expression (A simple expression cannot have line breaks)");
            } else {
                throw error("Invalid symbol found in simple expression" + tk);
            }
        }

    }

    private Expr makeList(List<Expr> res) {
        if (res.isEmpty()) {
            return new Expr.EName("Nil");
        } else {
            return new Expr.EAp(new Expr.EAp(new Expr.EName("Cons"), res.get(0)), makeList(res.subList(1, res.size())));
        }
    }

    private Expr led(Expr left, Token tk) {
        dbg("led " + left + ", " + tk);
        int bp = lbp(tk);
        if (is(tk, Token.Type.SYM, ".")) {
            expect(Token.Type.NAME);
            return new Expr.EAccessor(left, token.text);
        } else if (is(tk, Token.Type.SYM, "+") || is(tk, Token.Type.SYM, "-") || is(tk, Token.Type.SYM, "*") || is(tk, Token.Type.SYM, "/")
                || is(tk, Token.Type.SYM, "=") || is(tk, Token.Type.SYM, "%") || is(tk, Token.Type.SYM, "<") || is(tk, Token.Type.SYM, ">")
                || is(tk, Token.Type.KEYWORD, "and") || is(tk, Token.Type.KEYWORD, "or")) {
            depth++;
            Expr right = expr(bp);
            depth--;
            return new Expr.EAp(new Expr.EAp(new Expr.EName(tk.text), left), right);
        } else if (is(tk, Token.Type.SYM, "::")) {
            depth++;
            Expr right = expr(bp-1);
            depth--;
            return new Expr.EAp(new Expr.EAp(new Expr.EName("Cons"), left), right);
        } else if (left instanceof Expr.EName || left instanceof Expr.EAp || left instanceof Expr.ELam) {
            return new Expr.EAp(left, nud(tk));
        } else {
            throw error("Unexpected token " + tk);
        }
    }

    private boolean dbg(int rbp, Token tk) {
        depth++;
        dbg(rbp + "<" + lbp(tk) + "? (tk=" + tk + ")");
        depth--;
        return true;
    }

    public Expr expr(int rbp) {
        dbg("expr " + rbp);
        Token tk = advance();
        dbg("pop0 " + tk);
        depth++;
        Expr left = nud(tk);
        depth--;
        while (dbg(rbp, lexer.peek(1)) && rbp < lbp(lexer.peek(1))) {
            tk = advance();
            dbg("pop " + tk);
            depth++;
            left = led(left, tk);
            depth--;
        }
        return left;
    }


    private int depth = 0;
    public boolean prtDbg = true;

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
