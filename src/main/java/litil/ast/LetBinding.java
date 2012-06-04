package litil.ast;

import litil.Utils;

import java.util.List;

public class LetBinding extends Instruction {
    public final String name;
    public final Type type;
    public final List<Named> args;
    public final List<Instruction> instructions;

    public LetBinding(String name, Type type, List<Named> args, List<Instruction> instructions) {
        this.type = type;
        this.args = args;
        this.name = name;
        this.instructions = instructions;
    }

    @Override
    public String repr(int indent) {
        StringBuilder res = new StringBuilder(Utils.tab(indent));
        res.append("let ");
        res.append(name);
        for (Named arg : args) {
            res.append(" ").append(arg.name);
            if(arg.type!=null) {
                res.append(":").append(arg.type);
            }
        }
        if(type!=null) {
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
        StringBuilder res = new StringBuilder("let ");
        res.append(name);
        for (Named arg : args) {
            res.append(" ").append(arg.name);
            if(arg.type!=null) {
                res.append(":").append(arg.type);
            }
        }
        if(type!=null) {
            res.append(" : ").append(type);
        }
        res.append(" = \n");
        for (Instruction instruction : instructions) {
            res.append("\t").append(instruction).append("\n");
        }
        return res.toString();
    }
}
