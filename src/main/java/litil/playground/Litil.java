package litil.playground;

import litil.TypeScope;
import litil.ast.AstNode;
import litil.ast.Type;
import litil.eval.Evaluator;
import litil.eval.Prelude;
import litil.lexer.BaseLexer;
import litil.lexer.LookaheadLexerWrapper;
import litil.lexer.StructuredLexer;
import litil.parser.LitilParser;
import litil.tc.HMTypeChecker;
import litil.tc.TypeError;

import java.io.*;
import java.util.Arrays;

public class Litil {
    public static void main(String[] args) {
        String filename = "/Users/jawher/Dropbox/coding/jaml/target/classes/litil/eval/s99.ltl";
        boolean tc = true;
        boolean ev = true;
        AstNode node = null;
        try {
            node = parseFile(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        //System.out.println(node);
        System.out.println("=============");

        if (tc) {
            System.out.println("Type checking ...");
            try {
                TypeScope typeEnv = trootScope().child();
                new HMTypeChecker().analyze(node, typeEnv);
                System.out.println("All is good");
            } catch (TypeError e) {
                System.err.println(e.getMessage());
                return;
            }
        }

        if (ev) {
            System.out.println("Evaluating ...");
            Evaluator evaluator = new Evaluator();
            System.out.println("-------------------");

            System.out.println("--" + evaluator.eval(node, Prelude.rootScope()));
        }
    }

    private static AstNode parseFile(String file) throws FileNotFoundException {
        Reader reader = new FileReader(file);
        LitilParser p = new LitilParser(new LookaheadLexerWrapper(new StructuredLexer(new BaseLexer(reader), "  ")));
        p.debug = false;
        p.prtDbg = false;

        return p.program();
    }

    private static TypeScope trootScope() {
        TypeScope res = new TypeScope();
        res.define("-/1", Type.Function(Type.INT, Type.INT));
        res.define("+", Type.Function(Arrays.asList(Type.INT, Type.INT), Type.INT));
        res.define("-", Type.Function(Arrays.asList(Type.INT, Type.INT), Type.INT));
        res.define("*", Type.Function(Arrays.asList(Type.INT, Type.INT), Type.INT));
        res.define("/", Type.Function(Arrays.asList(Type.INT, Type.INT), Type.INT));
        res.define("%", Type.Function(Arrays.asList(Type.INT, Type.INT), Type.INT));

        res.define(">", Type.Function(Arrays.asList(Type.INT, Type.INT), Type.BOOL));
        res.define("<", Type.Function(Arrays.asList(Type.INT, Type.INT), Type.BOOL));

        Type.Variable a = new Type.Variable();
        res.define("=", Type.Function(Arrays.asList(a, a), Type.BOOL));

        res.define("int2str", Type.Function(Type.INT, Type.STR));
        res.define("print", Type.Function(Type.STR, Type.UNIT));
        res.define("error", Type.Function(Type.STR, new Type.Variable()));
        return res;
    }

}
