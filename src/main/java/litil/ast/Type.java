package litil.ast;

import java.util.*;

public abstract class Type {

    public AstNode ast;

    public static final class Variable extends Type {
        private static int idx = 1, nIdx = 0;
        public Type instance;
        private int id;
        private String name;

        public Variable() {
            id = idx++;
            name = "'" + String.valueOf((char) ('a' + (id % 26))) + (id / 26);
        }

        public String getName() {

            return name + (instance == null ? "" : "/" + instance);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Variable variable = (Variable) o;

            if (id != variable.id) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public String toString() {
            if (instance != null) {
                return instance.toString();
            } else {
                return getName();// "Var(" + id + ")";
            }
        }
    }

    public static class Oper extends Type {
        public static interface Repr {
            String repr(String name, List<? extends Type> args);
        }

        public final String name;
        public final List<? extends Type> args;
        public final Repr repr;

        public Oper(String name, List<? extends Type> args) {
            this.args = args;
            this.name = name;
            this.repr = null;
        }

        public Oper(String name) {
            this.args = Collections.emptyList();
            this.name = name;
            this.repr = null;
        }

        public Oper(String name, List<Type> args, Repr repr) {
            this.args = args;
            this.name = name;
            this.repr = repr;
        }


        @Override
        public String toString() {
            if (repr != null) {
                return repr.repr(name, args);
            } else if (args.isEmpty()) {
                return name;
            } else {
                StringBuilder res = new StringBuilder("(");
                res.append(name).append(":");
                for (Type type : args) {
                    res.append(" ").append(type);
                }
                res.append(")");
                return res.toString();
            }
        }
    }

    public static class RecordType extends Type {

        public final Map<String, Type> types;

        public RecordType(Map<String, Type> types) {
            this.types = types;
        }

        @Override
        public String toString() {
            return types.toString();
        }
    }

    public static class TyCon extends Type {

        public final String name;
        public final List<Type> types;
        public final Type dataType;

        public TyCon(String name, List<Type> types, Type dataType) {
            this.name = name;
            this.types = types;
            this.dataType = dataType;
        }

        public final Type makeFn() {
            return Function(types, dataType);
        }

        @Override
        public String toString() {
            return "("+name + " " + (types.isEmpty()?"":types) + "=>" + dataType+")";
        }
    }

    public static Type TyCon(DataDecl.TypeConstructor tycon, DataDecl dataDecl) {
        return new TyCon(tycon.name, tycon.types, dataDecl.type);
    }

    public static final Type EXCEPTION = new Oper("Exception", Collections.<Type>emptyList());

    public static class ExceptionCon extends Type {

        public final String name;
        public final List<Type> types;

        public ExceptionCon(String name, List<Type> types) {
            this.name = name;
            this.types = types;
        }

        public final Type makeFn() {
            return Function(types, EXCEPTION);
        }

        @Override
        public String toString() {
            return name + " " + types + "=>" + EXCEPTION;
        }
    }


    public static final Type INT = new Oper("Number", Collections.<Type>emptyList());
    public static final Type BOOL = new Oper("Boolean", Collections.<Type>emptyList());
    public static final Type CHAR = new Oper("Char", Collections.<Type>emptyList());
    public static final Type STR = List(CHAR);
    public static final Type UNIT = new Oper("()", Collections.<Type>emptyList());

    public static final Type Product(List<Type> types) {
        return new Oper("*", types, new Oper.Repr() {
            public String repr(String name, List<? extends Type> args) {
                StringBuilder res = new StringBuilder("(");
                boolean first = true;
                for (Type arg : args) {
                    if (first) {
                        first = false;
                    } else {
                        res.append(" * ");
                    }
                    res.append(arg);
                }
                res.append(")");

                return res.toString();
            }
        });
    }

    public static Type List(final Type type) {
        return new Oper("List", Arrays.asList(type), new Oper.Repr() {
            public String repr(String name, List<? extends Type> args) {
                return "[" + args.get(0) + "]";
            }
        });
    }

    public static final Type Function(final Type argType, final Type resType) {

        List<Type> types = new ArrayList<Type>();
        types.add(argType);
        types.add(resType);
        return new Oper("->", types, new Oper.Repr() {
            public String repr(String name, List<? extends Type> args) {
                Type argType = args.get(0);
                Type resType = args.get(1);
                StringBuilder res = new StringBuilder("");
                String argRepr = argType.toString();
                if (argRepr.contains("->")) {
                    res.append("(").append(argRepr).append(")");
                } else {
                    res.append(argRepr);
                }
                res.append(" -> ");
                String resRepr = resType.toString();
                if (resRepr.contains("->")) {
                    res.append("(").append(resRepr).append(")");
                } else {
                    res.append(resRepr);
                }
                return res.toString();
            }
        });
    }

    public static final Type Function(final List<? extends Type> argTypes, final Type resType) {

        List<Type> types = new ArrayList<Type>(argTypes);
        types.add(resType);
        Type res = null;
        for (int i = types.size() - 1; i >= 0; i--) {
            Type type = types.get(i);
            if (res == null) {
                res = type;
            } else {
                res = Function(type, res);
            }
        }
        return res;
    }
}
