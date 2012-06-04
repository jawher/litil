package litil.eval;

import java.util.*;

public class ValScope {

    private final Map<String, Object> scope;
    private final ValScope parent;

    private ValScope(ValScope parent, Map<String, Object> scope) {
        this.parent = parent;
        this.scope = scope;
    }

    public ValScope(ValScope parent) {
        this(parent, new HashMap<String, Object>());
    }

    public ValScope() {
        this(null, new HashMap<String, Object>());
    }

    public void define(String name, Object v) {
        scope.put(name, v);
    }

    public Object get(String name) {
        Object res = scope.get(name);
        if (res == null && parent != null) {
            res = parent.get(name);
        }
        return res;
    }


    public ValScope child() {
        return new ValScope(this);
    }

    @Override
    public String toString() {
        return scope.toString() + (parent != null ? "->" + parent : "-|");
    }
}
