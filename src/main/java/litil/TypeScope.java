package litil;

import litil.ast.Type;

import java.util.HashMap;
import java.util.Map;

public class TypeScope {

    private final Map<String, Type> scope;
    private final TypeScope parent;

    private TypeScope(TypeScope parent, Map<String, Type> scope) {
        this.parent = parent;
        this.scope = scope;
    }

    public TypeScope(TypeScope parent) {
        this(parent, new HashMap<String, Type>());
    }

    public TypeScope() {
        this(null, new HashMap<String, Type>());
    }

    public void define(String name, Type type) {
        scope.put(name, type);
    }

    public Type get(String name) {
        Type res = scope.get(name);
        if (res == null && parent != null) {
            res = parent.get(name);
        }
        return res;
    }

    public TypeScope child() {
        return new TypeScope(this);
    }

    @Override
    public String toString() {
        return scope + (parent == null ? "-|" : "->" + parent);
    }
}
