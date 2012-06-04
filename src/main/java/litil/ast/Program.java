package litil.ast;

import java.util.ArrayList;
import java.util.List;

public class Program extends AstNode {
    public final List<Instruction> instructions = new ArrayList<Instruction>();

    @Override
    public String repr(int indent) {
        StringBuilder res = new StringBuilder();
        for(Instruction instr: instructions) {
            res.append(instr.repr(indent)).append("\n");
        }
        return res.toString();
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        for(Instruction instr: instructions) {
            res.append(instr).append("\n");
        }
        return res.toString();
    }
}
