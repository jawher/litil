package litil.ast;

import litil.Utils;

import java.util.List;

public class DestructuringLetBinding extends Instruction {
    public final Pattern main;
    public final List<Pattern> args;
    public final List<Instruction> instructions;

    public DestructuringLetBinding(Pattern main, List<Pattern> args, List<Instruction> instructions) {
        this.main = main;
        this.args = args;
        this.instructions = instructions;
    }

    @Override
    public String repr(int indent) {
        StringBuilder res = new StringBuilder(Utils.tab(indent));
        res.append("let ");
        res.append(main);

        for (Pattern arg : args) {
            res.append(" ").append(arg);
        }
        res.append(" = \n");
        for (Instruction instruction : instructions) {
            res.append(instruction.repr(indent + 1)).append("\n");
        }
        return res.toString();
    }

    @Override
    public String toString() {
        return repr(0);
    }
}
