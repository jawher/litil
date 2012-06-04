package litil.ast;

import litil.Utils;

import java.util.Collections;
import java.util.List;

public class ExceptionDecl extends Instruction {
    public final String name;
    public final List<Type> types;

    public ExceptionDecl(String name, List<Type> types) {
        this.name = name;
        this.types = types;
    }

    @Override
    public String repr(int indent) {
        StringBuilder res = new StringBuilder(Utils.tab(indent));
        res.append("exception ").append(name);
        for (Type t : types) {
            res.append(" ").append(t);
        }
        return res.toString();
    }
}
