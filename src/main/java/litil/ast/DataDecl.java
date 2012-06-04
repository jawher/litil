package litil.ast;

import litil.Utils;

import java.util.Collections;
import java.util.List;

public class DataDecl extends Instruction {
    public static final class TypeConstructor {
        public final String name;
        public final List<Type> types;

        public TypeConstructor(String name, List<Type> types) {
            this.name = name;
            this.types = types;
        }

        public TypeConstructor(String name) {
            this.name = name;
            this.types = Collections.emptyList();
        }
    }

    public final String name;
    public final Type type;
    public final List<Type.Variable> typesVariables;
    public final List<TypeConstructor> typeConstructors;

    public DataDecl(String name, Type type, List<Type.Variable> typesVariables, List<TypeConstructor> typeConstructors) {
        this.name = name;
        this.type = type;
        this.typesVariables = typesVariables;
        this.typeConstructors = typeConstructors;
    }

    @Override
    public String repr(int indent) {
        StringBuilder res = new StringBuilder(Utils.tab(indent));
        res.append("data ").append(name);
        for (Type.Variable typesVariable : typesVariables) {
            res.append(typesVariable).append(" ");
        }
        res.append(" = ");
        boolean first = true;
        for (TypeConstructor typeConstructor : typeConstructors) {
            if (first) {
                first = false;
            } else {
                res.append(" | ");
            }
            res.append(typeConstructor.name);
            for (Type type : typeConstructor.types) {
                res.append(" ").append(type);
            }
        }
        return res.toString();
    }
}
