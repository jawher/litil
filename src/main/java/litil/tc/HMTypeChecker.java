package litil.tc;

import litil.TypeScope;
import litil.ast.*;

import java.util.*;

public class HMTypeChecker {
    private static int _id = 0;

    public Type analyze(AstNode node, TypeScope env) {
        return analyze(node, env, new HashSet<Type>());
    }

    public Type analyze(AstNode node, TypeScope env, Set<Type> nonGen) {
        int id = _id++;
        //System.out.println("(" + id + ")" + "analyze: " + node + "\n\tenv=" + env + "\n\tnonGen=" + nonGen);
        if (node instanceof Program) {
            for (Instruction inst : ((Program) node).instructions) {
                if (inst instanceof LetBinding) {
                    LetBinding let = (LetBinding) inst;
                    if (let.args.size() > 0) {
                        env.define(let.name, Type.Function(new Type.Variable(), new Type.Variable()));
                    }
                }

            }
            for (Instruction inst : ((Program) node).instructions) {
                try {
                    Type type = analyze(inst, env, nonGen);
                    //System.out.println(inst.repr(1) + "\n\n\t=>" + type + "\n-----------------");
                } catch (TypeError e) {
                    System.err.println("" + env);
                    throw new RuntimeException("Cannot type " + inst, e);
                }
            }
            return null;
        } else if (node instanceof Expr.ENum) {
            return Type.INT;
        } else if (node instanceof Expr.EBool) {
            return Type.BOOL;
        } else if (node instanceof Expr.EStr) {
            return Type.STR;
        } else if (node instanceof Expr.EChar) {
            return Type.CHAR;
        } else if (node == Expr.EUnit) {
            return Type.UNIT;
        } else if (node instanceof Expr.EList) {
            Type valueType = new Type.Variable();
            for (Expr expr : ((Expr.EList) node).values) {
                unify(valueType, analyze(expr, env, nonGen));
            }
            return Type.List(valueType);
        } else if (node instanceof Expr.ETuple) {
            List<Type> valuesTypes = new ArrayList<Type>();
            for (Expr expr : ((Expr.ETuple) node).values) {
                valuesTypes.add(analyze(expr, env, nonGen));
            }

            return Type.Product(valuesTypes);
        } else if (node instanceof Expr.ERecord) {
            Expr.ERecord rec = (Expr.ERecord) node;
            Map<String, Type> valuesTypes = new HashMap<String, Type>();
            for (Map.Entry<String, Expr> entry : rec.values.entrySet()) {
                valuesTypes.put(entry.getKey(), analyze(entry.getValue(), env, nonGen));
            }


            return new Type.RecordType(valuesTypes);
        } else if (node instanceof Expr.EName) {
            return getType(((Expr.EName) node).name, env, nonGen);
        } else if (node instanceof Expr.EAccessor) {
            Expr.EAccessor acc = (Expr.EAccessor) node;
            Type objType = analyze(acc.obj, env, nonGen);
            Type.Variable res = new Type.Variable();
            Type.RecordType desiredType = new Type.RecordType(Collections.<String, Type>singletonMap(acc.field, res));
            unify(objType, desiredType);
            return res;
        } else if (node instanceof Expr.EAp) {
            Expr.EAp ap = (Expr.EAp) node;
            //System.out.println("(" + id + ")" + "ap=" + ap);
            Type funcType = analyze(ap.fn, env, nonGen);

            //System.out.println("(" + id + ")" + "::" + ap.fn + " > " + funcType);

            Type argType = analyze(ap.arg, env, nonGen);
            //System.out.println("(" + id + ")" + "ap.arg=" + ap.arg+" > "+argType);
            Type.Variable resType = new Type.Variable();
            try {
                unify(Type.Function(argType, resType), funcType);
            } catch (TypeError e) {
                throw new TypeError("Cannot type function application " + ap, Type.Function(argType, resType), funcType, e);
            }
            //System.out.println("(" + id + ")" + " resType of "+ap+"=> " + resType);
            return resType;
        } else if (node instanceof Expr.EIf) {
            Expr.EIf eif = (Expr.EIf) node;
            Type condType = analyze(eif.cond, env, nonGen);
            if (!isException(condType)) {
                unify(condType, Type.BOOL);
            }

            Type thenType = null;
            for (Instruction instr : eif.thenInstructions) {
                thenType = analyze(instr, env, nonGen);
            }

            Type elseType = null;
            for (Instruction instr : eif.elseInstructions) {
                elseType = analyze(instr, env, nonGen);
            }

            if (!isException(thenType) && !isException(elseType)) {
                unify(thenType, elseType);
            }
            return isException(thenType) ? elseType : thenType;
        } else if (node instanceof Expr.PatterMatching) {
            Expr.PatterMatching patmat = (Expr.PatterMatching) node;
            //System.out.println("analyzing " + patmat);
            Type inputType = analyze(patmat.input, env, nonGen);
            Type resType = new Type.Variable();
            boolean allExceptions = true;
            for (Expr.PatterMatching.Case pcase : patmat.cases) {
                //System.out.println("\thandling " + pcase);
                //unify pattern type and inputType
                //unify outcome types
                TypeScope newScope = env.child();
                Type patType = visit(pcase.pattern, newScope, nonGen);
                //System.out.println("scope after visiting pattern: " + newScope);
                //System.out.println("and patType=" + patType + " for pat " + pcase.pattern);
                //System.out.println("***************");
                //System.err.println("in " + inputType.getClass());
                if (!isException(inputType)) {
                    unify(patType, inputType);
                }
                //System.out.println("patType=" + patType + ", inputType=" + inputType);
                //System.out.println("$$$$$$$$$$$$$$$$");
                Type caseType = null;
                for (Instruction instruction : pcase.outcome) {
                    caseType = analyze(instruction, newScope, nonGen);
                }
                if (!isException(caseType)) {
                    unify(caseType, resType);
                    resType = caseType;
                    allExceptions = false;
                }

                //System.out.println("caseType=" + caseType + ", resType=" + resType);
                //System.out.println("%%%%%%%%%%%%%%%%%");

            }
            //System.out.println("resType=" + resType + " for " + patmat);
            return allExceptions ? Type.EXCEPTION : resType;
        } else if (node instanceof Expr.EThrow) {
            Expr.EThrow ethrow = (Expr.EThrow) node;
            Type exType = analyze(ethrow.exception, env, nonGen);
            unify(exType, Type.EXCEPTION);
            return Type.EXCEPTION;
        } else if (node instanceof Expr.TryCatch) {
            Expr.TryCatch tc = (Expr.TryCatch) node;


            Type tryType = null;
            for (Instruction instr : tc.tryBody) {
                tryType = analyze(instr, env, nonGen);
            }
            tryType = prune(tryType);
            System.err.println("" + tryType.getClass());
            System.err.println("TRY TYPE = " + (tryType == Type.EXCEPTION));
            Type.Variable resType = new Type.Variable();
            if (!isException(tryType)) {
                System.err.println("NOOOO");
                unify(tryType, resType);
            }


            for (Expr.PatterMatching.Case pcase : tc.catchCases) {
                TypeScope newScope = env.child();
                Type patType = visit(pcase.pattern, newScope, nonGen);
                unify(patType, Type.EXCEPTION);
                Type caseType = null;
                for (Instruction instruction : pcase.outcome) {
                    caseType = analyze(instruction, newScope, nonGen);
                }

                unify(caseType, resType);

            }

            return resType;
        } else if (node instanceof Expr.ELam) {
            Expr.ELam lam = (Expr.ELam) node;

            TypeScope newEnv = env.child();
            Set<Type> newNonGen = new HashSet<Type>(nonGen);
            List<Type> argTypes = new ArrayList<Type>();

            for (Named arg : lam.args) {
                Type argType = new Type.Variable();
                argTypes.add(argType);
                newEnv.define(arg.name, argType);
                newNonGen.add(argType);
            }
            Type resultType = null;
            for (Instruction instr : lam.instructions) {
                resultType = analyze(instr, newEnv, newNonGen);
            }
            if (lam.type != null) {
                unify(resultType, lam.type);
            }
            return Type.Function(argTypes, resultType);
        } else if (node instanceof LetBinding) {
            LetBinding let = (LetBinding) node;
            TypeScope newEnv = env.child();
            Set<Type> newNonGen = new HashSet<Type>(nonGen);
            List<Type> argTypes = new ArrayList<Type>();
            Type.Variable letTypeVar = new Type.Variable();

            //should a function be polymorphic to itself ?
            newEnv.define(let.name, letTypeVar);
            newNonGen.add(letTypeVar);
            for (Named arg : let.args) {
                Type argType = new Type.Variable();
                argTypes.add(argType);
                newEnv.define(arg.name, argType);
                newNonGen.add(argType);
            }
            Type resultType = null;
            for (Instruction instr : let.instructions) {
                resultType = analyze(instr, newEnv, newNonGen);
            }
            if (let.type != null) {
                unify(resultType, let.type);
            }
            Type letType = Type.Function(argTypes, resultType);
            env.define(let.name, letType);
            return letType;
        } else if (node instanceof DestructuringLetBinding) {
            DestructuringLetBinding let = (DestructuringLetBinding) node;
            TypeScope newEnv = env.child();
            Set<Type> newNonGen = new HashSet<Type>(nonGen);
            List<Type> argTypes = new ArrayList<Type>();
            Type letTypeVar = visit(let.main, newEnv, newNonGen);
            newNonGen.add(letTypeVar);
            for (Pattern arg : let.args) {
                Type argType = visit(arg, newEnv, newNonGen);
                argTypes.add(argType);
            }
            Type resultType = null;
            for (Instruction instr : let.instructions) {
                resultType = analyze(instr, newEnv, newNonGen);
            }

            Type letType = Type.Function(argTypes, resultType);
            define(let.main, letType, env);
            return letType;
        } else if (node instanceof DataDecl) {
            DataDecl dataDecl = (DataDecl) node;
            for (DataDecl.TypeConstructor tyCon : dataDecl.typeConstructors) {
                env.define(tyCon.name, Type.TyCon(tyCon, dataDecl));
            }
            return dataDecl.type;
        } else if (node instanceof ExceptionDecl) {
            ExceptionDecl dataDecl = (ExceptionDecl) node;
            env.define(dataDecl.name, new Type.ExceptionCon(dataDecl.name, dataDecl.types));
            return Type.EXCEPTION;
        } else {
            throw new RuntimeException("Unhandled node type " + node.getClass());
        }
    }

    private boolean isException(Type type) {
        type = prune(type);
        return ((type instanceof Type.Oper) && ((Type.Oper) type).name.equals(((Type.Oper) Type.EXCEPTION).name));
    }

    private Type visit(Pattern pat, TypeScope scope, Set<Type> nonGen) {
        if (pat instanceof Pattern.IdPattern) {
            Type.Variable res = new Type.Variable();
            String name = ((Pattern.IdPattern) pat).name;
            scope.define(name, res);
            nonGen.add(res);
            return res;
        } else if (pat instanceof Pattern.WildcardPattern) {
            return new Type.Variable();
        } else if (pat instanceof Pattern.TuplePattern) {
            Pattern.TuplePattern tup = (Pattern.TuplePattern) pat;
            List<Type> types = new ArrayList<Type>();
            for (Pattern tpat : tup.items) {
                types.add(visit(tpat, scope, nonGen));
            }
            return Type.Product(types);
        } else if (pat instanceof Pattern.TyConPattern) {
            Pattern.TyConPattern tyConPattern = (Pattern.TyConPattern) pat;
            List<Type> patTypes = new ArrayList<Type>();
            for (Pattern pattern : tyConPattern.patterns) {
                Type patType = visit(pattern, scope, nonGen);
                patTypes.add(patType);
            }
            Type refConsType = scope.get(tyConPattern.name);
            if (refConsType == null) {
                throw new RuntimeException("Unknow type constructor " + tyConPattern.name);
            } else {
                if (refConsType instanceof Type.TyCon) {
                    Type.TyCon refCons = (Type.TyCon) fresh((Type.TyCon) refConsType, nonGen);


                    if (tyConPattern.patterns.size() != refCons.types.size()) {
                        throw new RuntimeException("Bad arity: case " + pat + " provided " + tyConPattern.patterns.size() + " arguments whereas " + tyConPattern.name + " takes " + (refCons.types.size()));
                    }
                    for (int i = 0; i < patTypes.size(); i++) {
                        unify(patTypes.get(i), refCons.types.get(i));
                    }

                    //System.out.println("data type of pattern " + pat + " = " + refCons.dataType);
                    return refCons.dataType;//FIXME: fresh ?
                } else if (refConsType instanceof Type.ExceptionCon) {
                    Type.ExceptionCon refCons = (Type.ExceptionCon) refConsType;


                    if (tyConPattern.patterns.size() != refCons.types.size()) {
                        throw new RuntimeException("Bad arity: case " + pat + " provided " + tyConPattern.patterns.size() + " arguments whereas " + tyConPattern.name + " takes " + (refCons.types.size()));
                    }
                    for (int i = 0; i < patTypes.size(); i++) {
                        unify(patTypes.get(i), refCons.types.get(i));
                    }

                    //System.out.println("data type of pattern " + pat + " = " + refCons.dataType);
                    return Type.EXCEPTION;//FIXME: fresh ?
                } else {
                    throw new RuntimeException("Invalid type constructor " + tyConPattern.name);
                }
            }
        } else {
            throw new RuntimeException("Unknow pattern type " + pat);
        }
    }

    private void define(Pattern pat, Type t, TypeScope scope) {
        Type type = prune(t);
        if (pat instanceof Pattern.IdPattern) {
            scope.define(((Pattern.IdPattern) pat).name, type);
        } else if (pat instanceof Pattern.WildcardPattern) {
            // do nothing
        } else if (pat instanceof Pattern.TuplePattern) {
            Pattern.TuplePattern tup = (Pattern.TuplePattern) pat;
            if (type instanceof Type.Oper) {
                List<? extends Type> types = ((Type.Oper) type).args;
                List<Pattern> items = tup.items;
                for (int i = 0, itemsSize = items.size(); i < itemsSize; i++) {
                    Pattern tpat = items.get(i);
                    Type tpatType = types.get(i);
                    define(tpat, tpatType, scope);
                }
            } else {
                throw new RuntimeException("Invalid type " + type + "::" + type.getClass() + " for pattern " + pat);
            }
        } else if (pat instanceof Pattern.TyConPattern) {
            Pattern.TyConPattern tyConPattern = (Pattern.TyConPattern) pat;
            if (type instanceof Type.TyCon) {
                Type.TyCon tyConType = (Type.TyCon) type;
                List<Type> types = tyConType.types;
                for (int i = 0, typesSize = types.size(); i < typesSize; i++) {
                    Type tpatType = types.get(i);
                    Pattern tpat = tyConPattern.patterns.get(i);
                    define(tpat, tpatType, scope);
                }
            } else {
                throw new RuntimeException("Invalid type " + type + " for pattern " + pat);
            }
        } else {
            throw new RuntimeException("Unknow pattern type " + pat);
        }
    }

    private Type getType(String name, TypeScope env, Set<Type> nonGen) {
        if (env.get(name) != null) {
            Type fresh = fresh(env.get(name), nonGen);
            //System.out.println("getType " + name + " :: " + env.get(name) + " => " + fresh);
            return fresh;
        } else {
            throw new RuntimeException("Unknown entity " + name);
        }
    }

    private Type fresh(Type type, Set<Type> nonGen) {
        return freshRec(type, nonGen, new HashMap<Type.Variable, Type.Variable>());
    }

    private Type freshRec(Type type, Set<Type> nonGen, Map<Type.Variable, Type.Variable> mappings) {
        Type t = prune(type);
        if (t instanceof Type.Variable) {
            Type.Variable v = (Type.Variable) t;
            if (isGeneric(v, nonGen)) {
                //System.out.println("freshrec " + type + " nonGenric=" + nonGen + " ; " + mappings);
                if (!mappings.containsKey(v)) {
                    Type.Variable variable = new Type.Variable();
                    //System.out.println("new var ! " + v + " => " + variable);
                    mappings.put(v, variable);
                }
                return mappings.get(v);
            } else {
                //System.out.println("freshrec " + type + " => self");
                return v;
            }
        } else if (t instanceof Type.Oper) {
            Type.Oper oper = (Type.Oper) t;
            List<Type> args = new ArrayList<Type>(oper.args.size());
            for (Type arg : oper.args) {
                args.add(freshRec(arg, nonGen, mappings));
            }
            return new Type.Oper(oper.name, args, oper.repr);
        } else if (t instanceof Type.TyCon) {
            Type.TyCon tycon = (Type.TyCon) t;
            List<Type> types = new ArrayList<Type>(tycon.types.size());
            for (Type tpe : tycon.types) {
                types.add(freshRec(tpe, nonGen, mappings));
            }
            Type dataType = freshRec(tycon.dataType, nonGen, mappings);
            return new Type.TyCon(tycon.name, types, dataType);
        } else if (t instanceof Type.RecordType) {
            Type.RecordType recType = (Type.RecordType) t;
            Map<String, Type> types = new HashMap<String, Type>(recType.types.size());
            for (Map.Entry<String, Type> entry : recType.types.entrySet()) {
                types.put(entry.getKey(), freshRec(entry.getValue(), nonGen, mappings));
            }
            return new Type.RecordType(types);
        } else if (t instanceof Type.ExceptionCon) {
            return t;
        } else {
            throw new RuntimeException("Wat ? " + type);
        }

    }

    private boolean isGeneric(Type.Variable v, Set<Type> nonGen) {
        return !occursIn(v, nonGen);
    }


    private Type prune(Type type) {
        if (type instanceof Type.Variable) {
            Type.Variable v = (Type.Variable) type;
            if (v.instance != null) {
                v.instance = prune(v.instance);
                ////System.out.println("prune "+type+" => "+v.instance);
                return v.instance;

            }
        }
        ////System.out.println("prune "+type+" => "+type);
        return type;
    }

    private void unify(Type t1, Type t2) {
        //System.out.println("unify " + t1 + ", " + t2 + " : " + Thread.currentThread().getStackTrace()[2].toString());
        Type type1 = prune(t1);
        Type type2 = prune(t2);
        //System.out.println("unify(p) " + type1 + ", " + type2);
        if (type1 instanceof Type.TyCon) {
            type1 = ((Type.TyCon) type1).makeFn();
        } else if (type1 instanceof Type.ExceptionCon) {
            type1 = ((Type.ExceptionCon) type1).makeFn();
        }

        if (type2 instanceof Type.TyCon) {
            type2 = ((Type.TyCon) type2).makeFn();
        } else if (type2 instanceof Type.ExceptionCon) {
            type2 = ((Type.ExceptionCon) type2).makeFn();
        }
        //System.out.println("unify(c) " + type1 + ", " + type2);

        if (type1 instanceof Type.Variable) {
            if (!type1.equals(type2)) {
                Type.Variable v = (Type.Variable) type1;
                if (occursInType(v, type2)) {
                    throw new TypeError("Recursive unification", t1, t2);
                }
                //System.err.println("!!");
                v.instance = type2;
            }
        } else if (type2 instanceof Type.Variable) {
            unify(type2, type1);
        } else if (type1 instanceof Type.Oper && type2 instanceof Type.Oper) {
            Type.Oper o1 = (Type.Oper) type1;
            Type.Oper o2 = (Type.Oper) type2;

            if (!o1.name.equals(o2.name) || o1.args.size() != o2.args.size()) {
                throw new TypeError("Type mismatch: ", t1, t2);
            } else {
                for (int i = 0; i < o1.args.size(); i++) {
                    unify(o1.args.get(i), o2.args.get(i));
                }
            }
        } else if (type1 instanceof Type.RecordType && type2 instanceof Type.RecordType) {
            Type.RecordType recType1 = (Type.RecordType) type1;
            Type.RecordType recType2 = (Type.RecordType) type2;
            for (Map.Entry<String, Type> entry : recType2.types.entrySet()) {
                if (!recType1.types.containsKey(entry.getKey())) {
                    throw new TypeError("Cannot unify, no field " + entry.getKey(), type1, type2);
                }
                unify(entry.getValue(), recType1.types.get(entry.getKey()));
            }
        } else {
            throw new RuntimeException("Wat ? " + t1 + ", " + t2);
        }

    }

    private boolean occursIn(Type.Variable v, Set<Type> nonGen) {
        for (Type type : nonGen) {
            if (occursInType(v, type)) {
                return true;
            }
        }
        return false;
    }

    private boolean occursInType(Type.Variable v, Type type2) {
        ////System.out.println("occurs? "+v+" in "+type2);
        Type pruned = prune(type2);
        if (pruned.equals(v)) {
            //System.out.println(v + " in " + type2);
            return true;
        } else if (pruned instanceof Type.Oper) {
            Type.Oper o = (Type.Oper) pruned;
            for (Type arg : o.args) {
                if (occursInType(v, arg)) {
                    //System.out.println(v + " in " + type2 + " args");
                    return true;
                }
            }
        }
        return false;
    }

}
