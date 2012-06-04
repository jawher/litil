package litil.tc;

import litil.ast.Type;

public class TypeError extends RuntimeException {
    public final Type type1;
    public final Type type2;

    public TypeError(String msg, Type type1, Type type2) {
        super(msg+": cannot unify types "+type1+" and "+type2);
        this.type1 = type1;
        this.type2 = type2;
    }

    public TypeError(String msg, Type type1, Type type2, TypeError e) {
        super(msg+": cannot unify types "+type1+" and "+type2, e);
        this.type1 = type1;
        this.type2 = type2;
    }
}