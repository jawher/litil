package litil.ast;

import litil.TypeScope;
import litil.Utils;

import java.util.List;
import java.util.Map;

public abstract class Expr extends Instruction {
    public static final class EName extends Expr {
        public final String name;

        public EName(String name) {
            this.name = name;
        }

        public EName(String name, TypeScope scope) {
            this.name = name;
            this.scope = scope;
        }

        @Override
        public String repr(int indent) {
            return Utils.tab(indent) + name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final class EAccessor extends Expr {
        public final Expr obj;
        public final String field;

        public EAccessor(Expr obj, String field) {
            this.obj = obj;
            this.field = field;
        }

        @Override
        public String repr(int indent) {
            return Utils.tab(indent) + obj.repr(0) + "." + field;
        }

        @Override
        public String toString() {
            return repr(0);
        }
    }

    public static final class ENum extends Expr {
        public final long value;

        public ENum(long value) {
            this.value = value;
        }

        @Override
        public String repr(int indent) {
            return Utils.tab(indent) + value;
        }

        @Override
        public String toString() {
            return value + "";
        }
    }

    public static final class EStr extends Expr {
        public final String value;

        public EStr(String value) {
            this.value = value;
        }

        @Override
        public String repr(int indent) {
            return Utils.tab(indent) + value;
        }

        @Override
        public String toString() {
            return value + "";
        }
    }

    public static final class EChar extends Expr {
        public final String value;

        public EChar(String value) {
            this.value = value;
        }

        @Override
        public String repr(int indent) {
            return Utils.tab(indent) + value;
        }

        @Override
        public String toString() {
            return value + "";
        }
    }

    public static final Expr EUnit = new Expr() {


        @Override
        public String repr(int indent) {
            return Utils.tab(indent) + "()";
        }

        @Override
        public String toString() {
            return "()";
        }
    };

    public static final class EBool extends Expr {
        public final boolean value;

        public EBool(boolean value) {
            this.value = value;
        }

        @Override
        public String repr(int indent) {
            return Utils.tab(indent) + value;
        }

        @Override
        public String toString() {
            return value + "";
        }
    }

    public static final class EList extends Expr {
        public final List<Expr> values;

        public EList(List<Expr> values) {
            this.values = values;
        }

        @Override
        public String repr(int indent) {
            return Utils.tab(indent) + values;
        }


        @Override
        public String toString() {
            return values.toString();
        }
    }

    public static final class ETuple extends Expr {
        public final List<Expr> values;

        public ETuple(List<Expr> values) {
            this.values = values;
        }

        @Override
        public String repr(int indent) {
            StringBuilder res = new StringBuilder(Utils.tab(indent));
            res.append("(");
            boolean first = true;
            for (Expr value : values) {
                if (first) {
                    first = false;
                } else {
                    res.append(", ");
                }
                res.append(value.repr(0));
            }
            res.append(")");
            return res.toString();
        }

        @Override
        public String toString() {
            return repr(0);
        }
    }

    public static final class ERecord extends Expr {
        public final Map<String, Expr> values;

        public ERecord(Map<String, Expr> values) {
            this.values = values;
        }

        @Override
        public String repr(int indent) {
            StringBuilder res = new StringBuilder(Utils.tab(indent));
            res.append("{");
            boolean first = true;
            for (Map.Entry<String, Expr> value : values.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    res.append(", ");
                }
                res.append(value.getKey()).append(": ").append(value.getValue().repr(0));
            }
            res.append("}");
            return res.toString();
        }

        @Override
        public String toString() {
            return repr(0);
        }
    }

    public static final class EAp extends Expr {
        public final Expr fn;
        public final Expr arg;


        public EAp(Expr fn, Expr arg) {
            this.fn = fn;
            this.arg = arg;
        }

        public EAp(Expr fn, Expr arg, TypeScope scope) {
            this.fn = fn;
            this.arg = arg;
            this.scope = scope;
        }

        @Override
        public String repr(int indent) {
            return Utils.tab(indent) + toString();
        }

        @Override
        public String toString() {
            return "<" + fn + ">(" + arg + ")";
        }
    }

    public static final class ELam extends Expr {
        public final List<Named> args;
        public final Type type;
        public final List<Instruction> instructions;

        public ELam(List<Named> args, Type type, List<Instruction> instructions) {
            this.args = args;
            this.type = type;
            this.instructions = instructions;
        }

        @Override
        public String repr(int indent) {
            StringBuilder res = new StringBuilder(Utils.tab(indent));
            res.append("\\");
            for (Named arg : args) {
                res.append(" ").append(arg.name);
                if (arg.type != null) {
                    res.append(":").append(arg.type);
                }
            }
            if (type != null) {
                res.append(" : ").append(type);
            }
            res.append(" = \n");
            for (Instruction instruction : instructions) {
                res.append(instruction.repr(indent + 1)).append("\n");
            }
            return res.toString();
        }

        @Override
        public String toString() {
            StringBuilder res = new StringBuilder("\\");
            for (Named arg : args) {
                res.append(" ").append(arg.name);
                if (arg.type != null) {
                    res.append(":").append(arg.type);
                }
            }
            if (type != null) {
                res.append(" : ").append(type);
            }
            res.append(" = \n");
            for (Instruction instruction : instructions) {
                res.append("\t").append(instruction).append("\n");
            }
            return res.toString();
        }
    }

    public static final class EThrow extends Expr {
        public final Expr exception;

        public EThrow(Expr exception) {
            this.exception = exception;
        }

        @Override
        public String repr(int indent) {
            return Utils.tab(indent) + "throw" + exception.repr(0);
        }

        @Override
        public String toString() {
            return repr(0);
        }
    }

    public static final class EIf extends Expr {
        public final Expr cond;
        public final List<? extends Instruction> thenInstructions, elseInstructions;


        public EIf(Expr cond, List<? extends Instruction> thenInstructions, List<? extends Instruction> elseInstructions) {
            this.elseInstructions = elseInstructions;
            this.thenInstructions = thenInstructions;
            this.cond = cond;
        }

        @Override
        public String repr(int indent) {
            StringBuilder res = new StringBuilder(Utils.tab(indent) + "if ");
            res.append(cond).append(" then \n");

            for (Instruction instruction : thenInstructions) {
                res.append(instruction.repr(indent + 1)).append("\n");
            }
            res.append(Utils.tab(indent)).append("else\n");
            for (Instruction instruction : elseInstructions) {
                res.append(instruction.repr(indent + 1)).append("\n");
            }
            return res.toString();
        }

        @Override
        public String toString() {
            StringBuilder res = new StringBuilder("if ");
            res.append(cond).append(" then \n");

            for (Instruction instruction : thenInstructions) {
                res.append("\t").append(instruction).append("\n");
            }
            res.append("else\n");
            for (Instruction instruction : elseInstructions) {
                res.append("\t").append(instruction).append("\n");
            }
            return res.toString();
        }
    }

    public static final class PatterMatching extends Expr {
        public PatterMatching(Expr input, List<Case> cases) {
            this.input = input;
            this.cases = cases;
        }

        public static final class Case {
            public final Pattern pattern;
            public final List<Instruction> outcome;

            public Case(Pattern pattern, List<Instruction> outcome) {
                this.pattern = pattern;
                this.outcome = outcome;
            }

            @Override
            public String toString() {
                return pattern + " => " + outcome;
            }
        }

        public final Expr input;
        public final List<Case> cases;

        @Override
        public String repr(int indent) {
            StringBuilder res = new StringBuilder(Utils.tab(indent));
            res.append("match ").append(input);
            for (Case aCase : cases) {
                res.append("\n").append(Utils.tab(indent + 1)).append(aCase.pattern).append(" => ").append(aCase.outcome);

            }
            return res.toString();
        }
    }

    public static class TryCatch extends Expr {
        public final List<Instruction> tryBody;
        public final List<PatterMatching.Case> catchCases;

        public TryCatch(List<Instruction> tryBody, List<PatterMatching.Case> catchCases) {

            this.tryBody = tryBody;
            this.catchCases = catchCases;
        }

        @Override
        public String repr(int indent) {
            StringBuilder res = new StringBuilder(Utils.tab(indent));
            res.append("try");
            res.append("\n");

            for (Instruction instruction : tryBody) {
                res.append(instruction.repr(indent + 1)).append("\n");
            }
            res.append(Utils.tab(indent)).append("catch\n");
            for (PatterMatching.Case aCase : catchCases) {
                res.append("\n").append(Utils.tab(indent + 1)).append(aCase.pattern).append(" => ").append(aCase.outcome);

            }
            return res.toString();
        }

        @Override
        public String toString() {
            return repr(0);
        }
    }

}
