package litil.eval;

import litil.TypeScope;
import litil.Utils;
import litil.ast.*;
import litil.lexer.*;
import litil.parser.LitilParser;
import litil.tc.HMTypeChecker;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

public class Evaluator {
    private int depth;
    private boolean dbgAp = false;

    private void dbgAp(String msg) {
        if (dbgAp) {
            System.out.println(Utils.tab(depth) + msg);
        }
    }

    public Object eval(AstNode node, ValScope scope) {
        if (node instanceof Expr) {
            if (node instanceof Expr.EName) {
                Object val = scope.get(((Expr.EName) node).name);
                if (val == null) {
                    throw new EvalException("Unknwon identifier " + node);
                } else {
                    return val;
                }
            } else if (node instanceof Expr.EAccessor) {
                Expr.EAccessor acc = (Expr.EAccessor) node;
                Object obj = eval(acc.obj, scope);
                if (!(obj instanceof Map)) {
                    throw new EvalException("Not a record " + acc.obj);
                } else {
                    Map<String, Object> fields = (Map<String, Object>) obj;
                    if(!fields.containsKey(acc.field)) {
                        throw new EvalException("No field "+acc.field+ "in "+obj);
                    }
                    return fields.get(acc.field);
                }
            } else if (node instanceof Expr.ENum) {
                return ((Expr.ENum) node).value;
            } else if (node instanceof Expr.EBool) {
                return ((Expr.EBool) node).value;
            } else if (node instanceof Expr.EChar) {
                return ((Expr.EChar) node).value;
            } else if (node instanceof Expr.EStr) {
                String s = ((Expr.EStr) node).value;

                List<Object> chars = new ArrayList<Object>();
                for (int i = 0; i < s.length(); i++) {
                    chars.add(s.charAt(i));

                }
                return makeList(chars);
            } else if (node == Expr.EUnit) {
                return node;
            } else if (node instanceof Expr.ETuple) {
                Expr.ETuple tuple = ((Expr.ETuple) node);
                List<Object> res = new ArrayList<Object>();
                for (Expr value : tuple.values) {
                    res.add(eval(value, scope));
                }
                return res;
            } else if (node instanceof Expr.EList) {
                Expr.EList list = ((Expr.EList) node);
                List<Object> res = new ArrayList<Object>();
                for (Expr value : list.values) {
                    res.add(eval(value, scope));
                }
                return makeList(res);
            } else if (node instanceof Expr.ERecord) {
                Expr.ERecord record = ((Expr.ERecord) node);
                Map<String, Object> res = new HashMap<String, Object>();
                for (Map.Entry<String, Expr> entry : record.values.entrySet()) {
                    res.put(entry.getKey(), eval(entry.getValue(), scope));
                }
                return res;
            } else if (node instanceof Expr.EThrow) {
                Object excpt = eval(((Expr.EThrow) node).exception, scope);
                throw new LitilException((ExceptionAdt) excpt);
            } else if (node instanceof Expr.ELam) {
                Expr.ELam lam = (Expr.ELam) node;
                Fn fn = null;
                for (int i = lam.args.size() - 1; i >= 0; i--) {
                    Named arg = lam.args.get(i);
                    if (i == lam.args.size() - 1) {//last argument
                        fn = new ChaininLastFn(arg.name, lam.instructions);
                    } else {
                        fn = new ChainingFn(arg.name, fn);
                    }

                }

                fn = new EnvCapturingFnWrapper(fn, lam, scope);

                return fn;
            } else if (node instanceof Expr.EAp) {
                ValScope ss = scope.child();
                Expr.EAp ap = (Expr.EAp) node;
                dbgAp("eval ap: " + ap.fn + "(" + ap.arg + ")");
                dbgAp("eval fn");
                depth++;
                Object fn = eval(ap.fn, ss);
                depth--;
                //System.out.println("\t\teval ap.fn(" + ap.fn + ") = " + fn + " in " + ss);
                if (fn instanceof Fn) {
                    dbgAp("eval arg");
                    depth++;
                    Object evalArg = eval(ap.arg, ss);
                    depth--;
                    //System.out.println("\t\teval ap.arg(" + ap.arg + ") = " + evalArg + " in " + ss);
                    Object res = ((Fn) fn).eval(evalArg, ss);
                    dbgAp("res: " + ap.fn + "(" + ap.arg + ")");
                    dbgAp("fn=" + fn);
                    dbgAp("arg=" + evalArg);
                    dbgAp("=>" + res);
                    dbgAp("in " + ss);
                    dbgAp("-------------------");
                    return res;
                } else if (fn instanceof Adt) {
                    Object evalArg = eval(ap.arg, ss);
                    if (evalArg == Expr.EUnit) {
                        return fn;
                    } else {
                        System.err.println("scope=" + scope);
                        throw new EvalException("Error while evaluating " + node + ": " + fn + " constructor doesn't take arguments");
                    }
                } else {
                    throw new EvalException("Error while evaluating " + node + ": " + fn + " is not a function");
                }
            } else if (node instanceof Expr.EIf) {
                Expr.EIf eif = (Expr.EIf) node;
                Object cond = eval(eif.cond, scope);
                if (cond instanceof Boolean) {
                    if ((Boolean) cond) {
                        Object val = null;
                        ValScope thenScope = scope.child();
                        for (Instruction instr : eif.thenInstructions) {
                            val = eval(instr, thenScope);
                        }
                        return val;
                    } else {
                        Object val = null;
                        ValScope elseScope = scope.child();
                        for (Instruction instr : eif.elseInstructions) {
                            val = eval(instr, elseScope);
                        }
                        return val;
                    }
                } else {
                    throw new EvalException("Error while evaluating " + node + ": the condition is not a boolean");
                }
            } else if (node instanceof Expr.PatterMatching) {
                Expr.PatterMatching patmat = (Expr.PatterMatching) node;
                Object input = eval(patmat.input, scope);

                for (Expr.PatterMatching.Case pcase : patmat.cases) {
                    ValScope thisScope = scope.child();
                    //System.out.println("match "+pcase);
                    Object res = null;
                    if (match(input, pcase.pattern, thisScope)) {
                        //System.out.println(thisScope);
                        for (Instruction instruction : pcase.outcome) {
                            res = eval(instruction, thisScope);
                        }
                        return res;
                    }
                }
                throw new RuntimeException("Match exception: " + patmat.input + "=" + input + " didn't match any case of " + patmat);
            } else if (node instanceof Expr.TryCatch) {
                Expr.TryCatch patmat = (Expr.TryCatch) node;
                System.err.println("TRY");
                try {
                    Object val = null;
                    ValScope tryScope = scope.child();
                    for (Instruction instr : patmat.tryBody) {
                        val = eval(instr, tryScope);
                    }
                    return val;
                } catch (LitilException e) {
                    ExceptionAdt input = e.exceptionAdt;
                    for (Expr.PatterMatching.Case pcase : patmat.catchCases) {
                        ValScope thisScope = scope.child();
                        //System.out.println("match "+pcase);
                        Object res = null;
                        if (match(input, pcase.pattern, thisScope)) {
                            //System.out.println(thisScope);
                            for (Instruction instruction : pcase.outcome) {
                                res = eval(instruction, thisScope);
                            }
                            return res;
                        }
                    }
                    throw new LitilException(e);
                }
            } else {
                throw new EvalException("Unhandled expr " + node);
            }

        } else if (node instanceof LetBinding) {
            LetBinding let = (LetBinding) node;
            ValScope letScope = scope.child();
            if (let.args.isEmpty()) {//var
                Object val = null;
                for (Instruction instr : let.instructions) {
                    val = eval(instr, letScope);
                }
                scope.define(let.name, val);
                return val;
            } else {
                //to do: replace free names with their values from scope

                Fn fn = null;
                for (int i = let.args.size() - 1; i >= 0; i--) {
                    Named arg = let.args.get(i);
                    if (i == let.args.size() - 1) {//last argument
                        fn = new ChaininLastFn(arg.name, let.instructions);
                    } else {
                        fn = new ChainingFn(arg.name, fn);
                    }

                }
                System.out.println("define let " + let);
                fn = new EnvCapturingFnWrapper(fn, let, scope);
                scope.define(let.name, fn);
                return fn;
            }
        } else if (node instanceof DestructuringLetBinding) {
            DestructuringLetBinding let = (DestructuringLetBinding) node;
            ValScope letScope = scope.child();
            if (let.args.isEmpty()) {//var
                Object val = null;
                for (Instruction instr : let.instructions) {
                    val = eval(instr, letScope);
                }
                define(let.main, val, scope);
                return val;
            } else {
                //to do: replace free names with their values from scope
                if (let.main instanceof Pattern.IdPattern) {
                    String name = ((Pattern.IdPattern) let.main).name;

                    Fn fn = null;
                    for (int i = let.args.size() - 1; i >= 0; i--) {
                        Pattern arg = let.args.get(i);
                        if (i == let.args.size() - 1) {//last argument
                            fn = new DestrChaininLastFn(arg, let.instructions);
                        } else {
                            fn = new DestrChainingFn(arg, fn);
                        }

                    }
                    System.out.println("define let " + let);
                    fn = new EnvCapturingFnWrapper(fn, let, scope);
                    scope.define(name, fn);
                    return fn;
                } else {
                    throw new EvalException("Function name can only be a name, whereas a pattern " + let.main + " was provided in " + let);
                }
            }
        } else if (node instanceof DataDecl) {
            DataDecl data = (DataDecl) node;
            for (DataDecl.TypeConstructor tycon : data.typeConstructors) {
                Fn fn = null;

                for (int i = tycon.types.size() - 1; i >= 0; i--) {
                    if (i == tycon.types.size() - 1) {//last argument
                        fn = new TyConChaininLastFn(tycon.name);
                    } else {
                        fn = new TyConChainingFn(tycon.name, fn);
                    }
                }

                if (fn == null) {
                    scope.define(tycon.name, new Adt(tycon.name, Collections.emptyList()));
                } else {
                    System.out.println("define tycon " + tycon + " = " + fn);
                    scope.define(tycon.name, fn);
                }
            }
            return null;
        } else if (node instanceof ExceptionDecl) {
            ExceptionDecl ex = (ExceptionDecl) node;
            Fn fn = null;

            for (int i = ex.types.size() - 1; i >= 0; i--) {
                if (i == ex.types.size() - 1) {//last argument
                    fn = new ExceptionChaininLastFn(ex.name);
                } else {
                    fn = new TyConChainingFn(ex.name, fn);
                }
            }

            if (fn == null) {
                scope.define(ex.name, new ExceptionAdt(ex.name, Collections.emptyList()));
            } else {
                System.out.println("define exception " + ex + " = " + fn);
                scope.define(ex.name, fn);
            }

            return null;
        } else if (node instanceof Program) {
            ValScope thisScope = scope.child();
            Object val = null;
            for (Instruction instr : ((Program) node).instructions) {
                //System.out.println("eval " + instr.repr(0));
                //System.out.println("in " + scope + "\n");
                val = eval(instr, thisScope);
                System.out.println(val + " :: " + instr.repr(0));
                //System.out.println("----------------------------------------------------------------");
            }
            return val;
        }
        throw new EvalException("Unhandled node " + node);
    }

    private void define(Pattern pat, Object val, ValScope scope) {
        if (pat instanceof Pattern.IdPattern) {
            scope.define(((Pattern.IdPattern) pat).name, val);
        } else if (pat instanceof Pattern.WildcardPattern) {
            // do nothing
        } else if (pat instanceof Pattern.TuplePattern) {
            Pattern.TuplePattern tup = (Pattern.TuplePattern) pat;
            if (val instanceof List) {
                List<Object> vals = (List<Object>) val;
                List<Pattern> items = tup.items;
                for (int i = 0, itemsSize = items.size(); i < itemsSize; i++) {
                    Pattern tpat = items.get(i);
                    Object tpatVal = vals.get(i);
                    define(tpat, tpatVal, scope);
                }
            } else {
                throw new RuntimeException("Invalid value " + val + " for pattern " + pat);
            }
        } else if (pat instanceof Pattern.TyConPattern) {
            Pattern.TyConPattern tyConPattern = (Pattern.TyConPattern) pat;
            if (val instanceof Adt) {
                List<Object> vals = ((Adt) val).args;
                for (int i = 0, valsSize = vals.size(); i < valsSize; i++) {
                    Object tpatVal = vals.get(i);
                    Pattern tpat = tyConPattern.patterns.get(i);
                    define(tpat, tpatVal, scope);
                }
            } else {
                throw new RuntimeException("Invalid value " + val + " for pattern " + pat);
            }
        } else {
            throw new RuntimeException("Unknow pattern type " + pat);
        }
    }

    private Adt makeList(List<Object> res) {
        if (res.isEmpty()) {
            return Prelude.Nil;
        } else {
            return Prelude.Cons(res.get(0), makeList(res.subList(1, res.size())));
        }
    }

    private boolean match(Object input, Pattern pattern, ValScope thisScope) {
        if (pattern instanceof Pattern.IdPattern) {
            thisScope.define(((Pattern.IdPattern) pattern).name, input);
            return true;
        } else if (pattern instanceof Pattern.WildcardPattern) {
            return true;
        } else if (pattern instanceof Pattern.TuplePattern) {
            Pattern.TuplePattern tupat = (Pattern.TuplePattern) pattern;
            if (input instanceof List) {
                List<Object> tuple = (List<Object>) input;
                if (tuple.size() != tupat.items.size()) {
                    return false;
                } else {
                    List<Pattern> items = tupat.items;
                    for (int i = 0, itemsSize = items.size(); i < itemsSize; i++) {
                        Pattern item = items.get(i);
                        if (!match(tuple.get(i), tupat.items.get(i), thisScope)) {
                            return false;
                        }
                    }
                    return true;
                }
            } else {
                return false;
            }
        } else if (pattern instanceof Pattern.TyConPattern) {
            Pattern.TyConPattern tyconPat = (Pattern.TyConPattern) pattern;
            if (input instanceof Adt) {
                Adt adt = (Adt) input;
                if (tyconPat.name.equals(adt.tag) && tyconPat.patterns.size() == adt.args.size()) {
                    List<Pattern> patterns = tyconPat.patterns;
                    for (int i = 0, patternsSize = patterns.size(); i < patternsSize; i++) {
                        Pattern subPat = patterns.get(i);
                        if (!match(adt.args.get(i), subPat, thisScope)) {
                            return false;
                        }
                    }
                    return true;
                } else {
                    //System.out.println("no match, cuz not same name or not same size between " + tyconPat + " and " + adt);
                    return false;
                }
            } else if (input instanceof ExceptionAdt) {
                ExceptionAdt adt = (ExceptionAdt) input;
                if (tyconPat.name.equals(adt.tag) && tyconPat.patterns.size() == adt.args.size()) {
                    List<Pattern> patterns = tyconPat.patterns;
                    for (int i = 0, patternsSize = patterns.size(); i < patternsSize; i++) {
                        Pattern subPat = patterns.get(i);
                        if (!match(adt.args.get(i), subPat, thisScope)) {
                            return false;
                        }
                    }
                    return true;
                } else {
                    //System.out.println("no match, cuz not same name or not same size between " + tyconPat + " and " + adt);
                    return false;
                }
            } else {
                return false;
            }
        } else {
            throw new RuntimeException("Unsupported pattern type " + pattern);
        }
    }

    private class EnvCapturingFnWrapper implements Fn {
        private Map<String, Object> capturedEnv = new HashMap<String, Object>();
        private final Fn fn;

        private EnvCapturingFnWrapper(Fn fn, LetBinding let, ValScope scope) {
            this.fn = fn;
            List<String> excluded = new ArrayList<String>(let.args.size() + 1);
            excluded.add(let.name);
            for (Named named : let.args) {
                excluded.add(named.name);
            }
            //System.out.println("capturing env for " + let);
            for (Instruction instruction : let.instructions) {
                visit(instruction, scope, capturedEnv, excluded);
            }
        }

        private EnvCapturingFnWrapper(Fn fn, DestructuringLetBinding let, ValScope scope) {
            this.fn = fn;
            Set<String> excluded = new HashSet<String>(let.args.size() + 1);
            excluded.add(((Pattern.IdPattern) let.main).name);
            for (Pattern pat : let.args) {
                excludePatternBoundNames(pat, excluded);
            }
            //System.out.println("capturing env for " + let);
            for (Instruction instruction : let.instructions) {
                visit(instruction, scope, capturedEnv, excluded);
            }
        }

        private EnvCapturingFnWrapper(Fn fn, Expr.ELam lam, ValScope scope) {
            this.fn = fn;
            List<String> excluded = new ArrayList<String>(lam.args.size());
            for (Named named : lam.args) {
                excluded.add(named.name);
            }
            //System.out.println("capturing env for " + let);
            for (Instruction instruction : lam.instructions) {
                visit(instruction, scope, capturedEnv, excluded);
            }
        }

        private void visit(Instruction node, ValScope scope, Map<String, Object> capturedEnv, Collection<String> exluded) {
            if (node instanceof Expr.EList) {
                for (Expr expr : ((Expr.EList) node).values) {
                    visit(expr, scope, capturedEnv, exluded);
                }
            } else if (node instanceof Expr.EIf) {
                Expr.EIf eif = (Expr.EIf) node;
                visit(eif.cond, scope, capturedEnv, exluded);
                for (Instruction instr : eif.thenInstructions) {
                    visit(instr, scope, capturedEnv, exluded);
                }
                for (Instruction instr : eif.elseInstructions) {
                    visit(instr, scope, capturedEnv, exluded);
                }
            } else if (node instanceof Expr.EName) {
                if (!exluded.contains(((Expr.EName) node).name)) {
                    capturedEnv.put(((Expr.EName) node).name, Evaluator.this.eval(node, scope));
                }
            } else if (node instanceof Expr.PatterMatching) {
                Expr.PatterMatching patmat = (Expr.PatterMatching) node;
                visit(patmat.input, scope, capturedEnv, exluded);
                for (Expr.PatterMatching.Case pcase : patmat.cases) {
                    //extend excluded
                    Set<String> caseExcluded = new HashSet<String>(exluded);
                    excludePatternBoundNames(pcase.pattern, caseExcluded);
                    for (Instruction instruction : pcase.outcome) {
                        visit(instruction, scope, capturedEnv, caseExcluded);
                    }
                }
            } else if (node instanceof Expr.EAp) {
                Expr.EAp eap = (Expr.EAp) node;
                visit(eap.fn, scope, capturedEnv, exluded);
                visit(eap.arg, scope, capturedEnv, exluded);
            } else if (node instanceof LetBinding) {
                exluded.add(((LetBinding) node).name);
            } else if (node instanceof DestructuringLetBinding) {
                excludePatternBoundNames(((DestructuringLetBinding) node).main, exluded);
            } else if (node instanceof Expr) {
                //do nothing, an expression (except if and match) doesn't define idnetifiers
            } else {
                throw new EvalException("Unhandled node " + node + "::" + node.getClass());
            }
        }

        private void excludePatternBoundNames(Pattern pattern, Collection<String> excluded) {
            if (pattern instanceof Pattern.IdPattern) {
                excluded.add(((Pattern.IdPattern) pattern).name);
            } else if (pattern instanceof Pattern.TuplePattern) {
                Pattern.TuplePattern tupat = (Pattern.TuplePattern) pattern;
                for (Pattern subPat : tupat.items) {
                    excludePatternBoundNames(subPat, excluded);
                }
            } else if (pattern instanceof Pattern.TyConPattern) {
                Pattern.TyConPattern tyconPat = (Pattern.TyConPattern) pattern;
                for (Pattern subPat : tyconPat.patterns) {
                    excludePatternBoundNames(subPat, excluded);
                }
            }
        }

        public Object eval(Object arg, ValScope scope) {
            for (Map.Entry<String, Object> e : capturedEnv.entrySet()) {
                scope.define(e.getKey(), e.getValue());
            }
            return fn.eval(arg, scope);
        }
    }

    private static class FnApArgs {
        public final Map<String, Object> args = new HashMap<String, Object>();
    }

    private class ChainingFn implements Fn {
        private String argName;
        private Fn next;

        private ChainingFn(String argName, Fn next) {
            this.argName = argName;
            this.next = next;
        }

        public Object eval(final Object oarg, ValScope scope) {
            return new Fn() {
                public Object eval(Object arg, ValScope scope) {
                    FnApArgs margs;
                    if (oarg instanceof FnApArgs) {
                        margs = (FnApArgs) oarg;
                        margs.args.put(argName, margs.args.get("*"));
                    } else {
                        margs = new FnApArgs();
                        margs.args.put(argName, oarg);
                    }
                    margs.args.put("*", arg);
                    return next.eval(margs, scope);
                }

                @Override
                public String toString() {
                    return "chain(" + argName + "=" + oarg + ")->(" + next + ")";
                }
            };
        }

        @Override
        public String toString() {
            return "chain(" + argName + ")->(" + next + ")";
        }
    }

    private class ChaininLastFn implements Fn {
        private String argName;
        private List<Instruction> body;

        private ChaininLastFn(String argName, List<Instruction> body) {
            this.argName = argName;
            this.body = body;
        }

        public Object eval(Object arg, ValScope scope) {
            if (arg instanceof FnApArgs) {
                FnApArgs margs = (FnApArgs) arg;
                margs.args.put(argName, margs.args.get("*"));
                for (Map.Entry<String, Object> e : margs.args.entrySet()) {
                    if ("*".equals(e.getKey())) {
                        scope.define(argName, e.getValue());
                    } else {
                        scope.define(e.getKey(), e.getValue());
                    }
                }
            } else {

                scope.define(argName, arg);
            }
            Object val = null;
            for (Instruction instruction : body) {
                val = Evaluator.this.eval(instruction, scope);
            }
            return val;
        }

        @Override
        public String toString() {
            return "ChainLast(" + argName + ")";
        }
    }

    private static class DestrFnApArgs {
        private static final class PatVal {
            public Pattern pat;
            public Object val;

            private PatVal(Pattern pat, Object val) {
                this.pat = pat;
                this.val = val;
            }
        }

        public final List<PatVal> args = new ArrayList<PatVal>();
        public Object freeVal;

    }

    private class DestrChainingFn implements Fn {
        private Pattern pat;
        private Fn next;

        private DestrChainingFn(Pattern pat, Fn next) {
            this.pat = pat;
            this.next = next;
        }

        public Object eval(final Object oarg, ValScope scope) {
            return new Fn() {
                public Object eval(Object arg, ValScope scope) {
                    DestrFnApArgs margs;
                    if (oarg instanceof DestrFnApArgs) {
                        margs = (DestrFnApArgs) oarg;
                        margs.args.add(new DestrFnApArgs.PatVal(pat, margs.freeVal));
                        margs.freeVal = arg;
                    } else {
                        margs = new DestrFnApArgs();
                        margs.args.add(new DestrFnApArgs.PatVal(pat, oarg));

                    }
                    margs.freeVal = arg;
                    return next.eval(margs, scope);
                }

                @Override
                public String toString() {
                    return "chain(" + pat + "=" + oarg + ")->(" + next + ")";
                }
            };
        }

        @Override
        public String toString() {
            return "chain(" + pat + ")->(" + next + ")";
        }
    }

    private class DestrChaininLastFn implements Fn {
        private Pattern pat;
        private List<Instruction> body;

        private DestrChaininLastFn(Pattern pat, List<Instruction> body) {
            this.pat = pat;
            this.body = body;
        }

        public Object eval(Object arg, ValScope scope) {
            if (arg instanceof DestrFnApArgs) {
                DestrFnApArgs margs = (DestrFnApArgs) arg;
                for (DestrFnApArgs.PatVal e : margs.args) {
                    define(e.pat, e.val, scope);
                }
                define(pat, margs.freeVal, scope);
            } else {
                define(pat, arg, scope);
            }
            Object val = null;
            for (Instruction instruction : body) {
                val = Evaluator.this.eval(instruction, scope);
            }
            return val;
        }

        @Override
        public String toString() {
            return "ChainLast(" + pat + ")";
        }
    }

    private static class TyConArgs {
        public final List<Object> args;
        private final String tag;

        private TyConArgs(List<Object> args, String tag) {
            this.args = args;
            this.tag = tag;
        }

        @Override
        public String toString() {
            return "TyConArgs{" +
                    "args=" + args +
                    ", tag=" + tag +
                    '}';
        }
    }

    private class TyConChainingFn implements Fn {
        private String tag;
        private Fn next;

        public TyConChainingFn(String tag, Fn next) {
            this.tag = tag;
            this.next = next;
        }

        public Object eval(final Object oarg, ValScope scope) {

            return new Fn() {
                public Object eval(Object arg, ValScope scope) {
                    if (oarg instanceof TyConArgs) {
                        TyConArgs targs = (TyConArgs) oarg;
                        targs.args.add(arg);
                        return next.eval(targs, scope);
                    } else {
                        List<Object> args = new ArrayList<Object>();
                        args.add(oarg);
                        args.add(arg);
                        return next.eval(new TyConArgs(args, tag), scope);
                    }
                }
            };
        }

        @Override
        public String toString() {
            return "chain(" + "" + ")->(" + next + ")";
        }
    }

    private class TyConChaininLastFn implements Fn {
        private final String tag;

        private TyConChaininLastFn(String tag) {
            this.tag = tag;
        }

        public Object eval(Object arg, ValScope scope) {
            List<Object> args = new ArrayList<Object>();
            if (arg instanceof TyConArgs) {
                TyConArgs targs = (TyConArgs) arg;
                args.addAll(targs.args);
            } else if (arg != null) {
                args.add(arg);
            }
            return new Adt(tag, args);
        }

        @Override
        public String toString() {
            return "tyConChainLast(" + tag + ")";
        }
    }

    private class ExceptionChaininLastFn implements Fn {
        private final String tag;

        private ExceptionChaininLastFn(String tag) {
            this.tag = tag;
        }

        public Object eval(Object arg, ValScope scope) {
            List<Object> args = new ArrayList<Object>();
            if (arg instanceof TyConArgs) {
                TyConArgs targs = (TyConArgs) arg;
                args.addAll(targs.args);
            } else if (arg != null) {
                args.add(arg);
            }
            return new ExceptionAdt(tag, args);
        }

        @Override
        public String toString() {
            return "exceptionChainLast(" + tag + ")";
        }
    }



    private static class ExceptionAdt {
        public final String tag;
        public final List<Object> args;

        private ExceptionAdt(String tag, List<Object> args) {
            this.tag = tag;
            this.args = args;
        }


        @Override
        public String toString() {
            return "Exception<" + tag + ">" + args + "";
        }
    }

    private static class LitilException extends RuntimeException {
        private final ExceptionAdt exceptionAdt;

        public LitilException(ExceptionAdt exceptionAdt) {
            super(exceptionAdt.toString());
            this.exceptionAdt = exceptionAdt;
        }

        public LitilException(LitilException e) {
            super(e.toString(), e);
            this.exceptionAdt = e.exceptionAdt;
        }
    }

    public static void main(String[] args) {
        Evaluator ev = new Evaluator();
        String pg0 = "let f x y = x + y\nlet res = f 2 3";
        String pg1 = "let f x y = x + y\nlet a = 2\nlet b=3\nlet g = f a b";
        String pg2 = "let f x y = x + y\nlet a = 2\nlet b=3\nlet g = f a\ng b";
        AstNode node = parseFile("emb-list.ltl");
        System.out.println(node);
        System.out.println("=============");
        ev.dbgAp = false;
        TypeScope typeEnv = Prelude.trootScope().child();
        new HMTypeChecker().analyze(node, typeEnv);
        System.out.println("-------------------");
        System.out.println(typeEnv);
        ValScope valScope = Prelude.rootScope().child();
        System.out.println("--" + ev.eval(node, valScope));
        System.out.println(valScope);
        System.out.println(valScope);
    }


    private static AstNode binap(String opName, Expr arg1, Expr arg2) {
        return new Expr.EAp(new Expr.EAp(new Expr.EName(opName), arg1), arg2);
    }

    private static AstNode parseFile(String file) {
        Reader reader = new InputStreamReader(Evaluator.class.getResourceAsStream(file));
        LitilParser p = new LitilParser(new LookaheadLexerWrapper(new StructuredLexer(new BaseLexer(reader), "  ")));
        p.debug = false;
        p.prtDbg = false;

        return p.program();

    }

    private static AstNode parseStr(String str) {
        Reader reader = new StringReader(str);
        LitilParser p = new LitilParser(new LookaheadLexerWrapper(new StructuredLexer(new BaseLexer(reader), "  ")));

        return p.program();
    }

}
