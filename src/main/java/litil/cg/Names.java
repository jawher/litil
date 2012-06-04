package litil.cg;

import java.util.HashMap;
import java.util.Map;

public class Names {
    private final Map<String, String> mapping = new HashMap<String, String>();
    private final Names parent;

    private Names(Names parent) {
        this.parent = parent;
    }

    public Names() {
        this.parent = null;
    }


    public final void map(String name, String to) {
        mapping.put(name, to);
    }

    public final String get(String name) {
        if (mapping.containsKey(name)) {
            return mapping.get(name);
        } else if (parent != null) {
            return parent.get(name);
        } else {
            return null;
        }
    }

    public Names child() {
        return new Names(this);
    }


}
