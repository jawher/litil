package litil.eval;

import litil.TypeScope;
import litil.ast.DataDecl;
import litil.ast.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Prelude {

    public static ValScope rootScope() {
        ValScope res = new ValScope();
        res.define("-/1", new Fn() {
            public Object eval(Object arg, ValScope scope) {
                return -(Long) arg;
            }
        });
        res.define("not", new Fn() {
            public Object eval(Object arg, ValScope scope) {
                return !(Boolean) arg;
            }
        });
        res.define("+", new Fn() {
            public Object eval(final Object arg1, ValScope scope) {
                Fn fn = new Fn() {
                    public Object eval(Object arg2, ValScope scope) {
                        return (Long) arg1 + (Long) arg2;
                    }

                    @Override
                    public String toString() {
                        return "+(int, int)";
                    }
                };
                return fn;
            }

            @Override
            public String toString() {
                return "+(int)";
            }
        });

        res.define("-", new Fn() {
            public Object eval(final Object arg1, ValScope scope) {
                Fn fn = new Fn() {
                    public Object eval(Object arg2, ValScope scope) {
                        return (Long) arg1 - (Long) arg2;
                    }

                    @Override
                    public String toString() {
                        return "-(int, int)";
                    }
                };
                return fn;
            }

            @Override
            public String toString() {
                return "+(int)";
            }
        });

        res.define("*", new Fn() {
            public Object eval(final Object arg1, ValScope scope) {
                Fn fn = new Fn() {
                    public Object eval(Object arg2, ValScope scope) {
                        return (Long) arg1 * (Long) arg2;
                    }

                    @Override
                    public String toString() {
                        return "*(int, int)";
                    }
                };
                return fn;
            }

            @Override
            public String toString() {
                return "+(int)";
            }
        });

        res.define("=", new Fn.BiFn() {
            @Override
            protected Object eval(Object arg1, Object arg2, ValScope scope) {
                return arg1.equals(arg2);
            }
        });


        res.define("print", new Fn() {
            public Object eval(Object arg, ValScope scope) {
                System.out.println(">> " + arg);
                return null;
            }
        });

        res.define("int2str", new Fn() {
            public Object eval(Object arg, ValScope scope) {
                return arg + "";
            }
        });

        res.define("<", new Fn.BiFn() {
            @Override
            protected Object eval(Object arg1, Object arg2, ValScope scope) {
                return (Long) arg1 < (Long) arg2;
            }
        });
        res.define(">", new Fn.BiFn() {
            @Override
            protected Object eval(Object arg1, Object arg2, ValScope scope) {
                return (Long) arg1 > (Long) arg2;
            }
        });

        res.define("%", new Fn.BiFn() {
            @Override
            protected Object eval(Object arg1, Object arg2, ValScope scope) {
                return (Long) arg1 % (Long) arg2;
            }
        });

        res.define("error", new Fn() {
            public Object eval(Object arg, ValScope scope) {
                throw new RuntimeException((String) arg);
            }
        });

        res.define("Cons", new Fn.BiFn() {
            @Override
            protected Object eval(Object arg1, Object arg2, ValScope scope) {
                return new Adt("Cons", Arrays.asList(arg1, arg2)) {
                    private List<Object> toList(Adt adt) {
                        List<Object> items = new ArrayList<Object>();
                        if ("Nil".equals(adt.tag)) {
                            return Collections.emptyList();
                        } else {
                            items.add(adt.args.get(0));
                            items.addAll(toList((Adt) adt.args.get(1)));
                            return items;
                        }
                    }

                    @Override
                    public String toString() {
                        return toList(this) + "";
                    }
                };
            }
        });

        res.define("Nil", new Adt("Nil", Collections.emptyList()) {

            @Override
            public String toString() {
                return "[]";
            }
        });

        return res;
    }

    public static TypeScope trootScope() {
        TypeScope res = new TypeScope();
        res.define("-/1", Type.Function(Type.INT, Type.INT));
        res.define("not", Type.Function(Type.BOOL, Type.BOOL));
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

        Type.Variable b = new Type.Variable();
        Type listType = Type.List(b);
        List<DataDecl.TypeConstructor> listConstructors = new ArrayList<DataDecl.TypeConstructor>();
        List<Type> listVars = Arrays.<Type>asList(b);
        listConstructors.add(new DataDecl.TypeConstructor("Cons", Arrays.asList(b, listType)));
        listConstructors.add(new DataDecl.TypeConstructor("Nil"));
        DataDecl list = new DataDecl("List", listType, Arrays.asList(b), listConstructors);

        for (DataDecl.TypeConstructor tyCon : listConstructors) {
            res.define(tyCon.name, Type.TyCon(tyCon, list));
        }
        return res;
    }

}
