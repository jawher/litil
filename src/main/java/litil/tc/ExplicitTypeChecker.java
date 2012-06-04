package litil.tc;

import litil.ast.*;

import java.util.*;

public class ExplicitTypeChecker {
    public Type analyze(AstNode node) {
        if (node instanceof Program) {
            for (Instruction inst : ((Program) node).instructions) {
                System.out.println(node.repr(1) + "\n\n\t=>" + analyze(inst));
            }
            return null;
        } else if (node instanceof Expr.ENum) {
            return Type.INT;
        } else if (node instanceof Expr.EBool) {
            return Type.BOOL;
        } else if (node instanceof Expr.EStr) {
            return Type.STR;
        } else if (node instanceof Expr.EName) {
            Type res = node.scope.get(((Expr.EName) node).name);
            if (res == null) {
                throw new RuntimeException("Undeclared entity '" + node +
                        "'");
            }
            return res;
        } else if (node instanceof Expr.EAp) {
            Expr.EAp ap = (Expr.EAp) node;
            Type.Oper fnType;
            if (ap.fn instanceof Expr.EName) {
                fnType = (Type.Oper) ap.scope.get(((Expr.EName) ap.fn).name);
            } else {
                fnType = (Type.Oper) analyze(ap.fn);
            }
            if (fnType == null) {
                throw new RuntimeException("Undeclared entity '" + node + "'");
            }

            System.out.println("analyze " + ap.repr(1) + "\nfnType=>" + fnType.args);
            Type argType = analyze(ap.arg);
            unify(fnType.args.get(0), argType);

            Type.Oper res;
            if (fnType.args.size() == 2) {
                res = (Type.Oper) fnType.args.get(1);
            } else {
                res = new Type.Oper("->", fnType.args.subList(1, fnType.args.size()));

            }

            return res;
        } else if (node instanceof Expr.EIf) {
            Expr.EIf eif = (Expr.EIf) node;
            Type condType = analyze(eif.cond);
            unify(condType, Type.BOOL);

            Type thenType = null;
            for (Instruction instr : eif.thenInstructions) {
                thenType = analyze(instr);
            }

            Type elseType = null;
            for (Instruction instr : eif.elseInstructions) {
                elseType = analyze(instr);
            }

            unify(thenType, elseType);
            return thenType;
        } else if (node instanceof LetBinding) {
            LetBinding let = (LetBinding) node;
            if (let.type == null) {
                throw new RuntimeException("No declared return type for " + let);
            }
            List<Type> argTypes = new ArrayList<Type>();


            for (Named arg : let.args) {
                if (arg.type == null) {
                    throw new RuntimeException("No declared type for let arg " + arg);
                }
                argTypes.add(arg.type);
            }
            Type letType = argTypes.isEmpty() ? let.type : Type.Function(argTypes, let.type);

            Type resultType = null;
            for (Instruction instr : let.instructions) {
                resultType = analyze(instr);
                System.out.println("let instr " + instr + " => " + resultType);
            }
            if (let.type != null) {
                unify(resultType, let.type);
            }

            return letType;
        } else if (node instanceof DataDecl) {
            //what now ?
            return null;
        } else {
            throw new RuntimeException("Unhandled node type " + node.getClass());
        }
    }

    public void unify(Type type1, Type type2) {
        System.out.println("unify " + type1 + ", " + type2);
        if (type1 instanceof Type.Oper && type2 instanceof Type.Oper) {
            Type.Oper o1 = (Type.Oper) type1;
            Type.Oper o2 = (Type.Oper) type2;

            if (!o1.name.equals(o2.name) || o1.args.size() != o2.args.size()) {
                throw new RuntimeException("Type mismatch: " + o1 + "<>" + o2);
            } else {
                for (int i = 0; i < o1.args.size(); i++) {
                    unify(o1.args.get(i), o2.args.get(i));
                }
            }
        } else {
            throw new RuntimeException("Unknown types " + type1 + "; " + type2);
        }
    }
}
