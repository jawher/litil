package litil.tc;

import litil.TypeScope;
import litil.ast.*;

import java.util.ArrayList;
import java.util.List;

public class MonomorphicTypeChecker {
    public Type analyze(AstNode node, TypeScope scope) {
        //System.out.println("analyze " + node + "(" + node.getClass() + ")" + "\n\tin " + scope);

        if (node instanceof Program) {
            for (Instruction inst : ((Program) node).instructions) {
                if(inst instanceof LetBinding) {
                    LetBinding let = (LetBinding) inst;
                    if(let.args.size()>0) {
                        scope.define(let.name, Type.Function(new Type.Variable(), new Type.Variable()));
                    }
                }

            }
            for (Instruction inst : ((Program) node).instructions) {
                Type type = analyze(inst, scope);
                System.out.println(inst.repr(1) + "\n\n\t=>" + type+"\n-----------------");
            }
            return null;
        } else if (node instanceof Expr.ENum) {
            return Type.INT;
        } else if (node instanceof Expr.EBool) {
            return Type.BOOL;
        } else if (node instanceof Expr.EStr) {
            return Type.STR;
        } else if (node instanceof Expr.EName) {
            Type res = scope.get(((Expr.EName) node).name);
            if (res == null) {
                throw new RuntimeException("Undeclared entity '" + node +
                        "'");
            }
            return res;
        } else if (node instanceof Expr.EAp) {
            Expr.EAp ap = (Expr.EAp) node;
            Type funcType = analyze(ap.fn, scope);
            Type argType = analyze(ap.arg, scope);

            Type.Variable resType = new Type.Variable();
            unify(Type.Function(argType, resType), funcType);
            return resType;
        } else if (node instanceof Expr.EIf) {
            Expr.EIf eif = (Expr.EIf) node;
            Type condType = analyze(eif.cond, scope);
            unify(condType, Type.BOOL);

            Type thenType = null;
            for (Instruction instr : eif.thenInstructions) {
                thenType = analyze(instr, scope);
            }

            Type elseType = null;
            for (Instruction instr : eif.elseInstructions) {
                elseType = analyze(instr, scope);
            }

            unify(thenType, elseType);
            return thenType;
        } else if (node instanceof LetBinding) {
            LetBinding let = (LetBinding) node;
            TypeScope ss = new TypeScope(scope);
            List<Type> argTypes = new ArrayList<Type>();
            ss.define(let.name, new Type.Variable());

            for (Named arg : let.args) {
                Type argType = new Type.Variable();
                argTypes.add(argType);
                ss.define(arg.name, argType);
            }
            Type resultType = null;
            for (Instruction instr : let.instructions) {
                resultType = analyze(instr, ss);
            }
            if (let.type != null) {
                unify(resultType, let.type);
            }
            Type letType = Type.Function(argTypes, resultType);
            scope.define(let.name, letType);
            return letType;
        } else {
            throw new RuntimeException("Unhandled node type " + node.getClass());
        }
    }

    public void unify(Type t1, Type t2) {
        System.out.println("unify " + t1 + ", " + t2);
        Type type1 = prune(t1);
        Type type2 = prune(t2);

        if (type1 instanceof Type.Variable) {
            if (!type1.equals(type2)) {
                Type.Variable v = (Type.Variable) type1;
                //if (occursInType(v, type2)) {
                //    throw new TypeError("Recursive unification");
                //}
                //System.err.println("!!");
                v.instance = type2;
            }
        } else if (type1 instanceof Type.Oper && type2 instanceof Type.Variable) {
            unify(type2, type1);
        } else if (type1 instanceof Type.Oper && type2 instanceof Type.Oper) {
            Type.Oper o1 = (Type.Oper) type1;
            Type.Oper o2 = (Type.Oper) type2;

            if (!o1.name.equals(o2.name) || o1.args.size() != o2.args.size()) {
                throw new TypeError("Type mismatch", o1, o2);
            } else {
                for (int i = 0; i < o1.args.size(); i++) {
                    unify(o1.args.get(i), o2.args.get(i));
                }
            }
        } else {
            throw new TypeError("Wat ? ", t1, t2);
        }
    }

    private Type prune(Type type) {
        if (type instanceof Type.Variable) {
            Type.Variable v = (Type.Variable) type;
            if (v.instance != null) {
                v.instance = prune(v.instance);
                //System.out.println("prune "+type+" => "+v.instance);
                return v.instance;

            }
        }
        //System.out.println("prune "+type+" => "+type);
        return type;
    }
}
