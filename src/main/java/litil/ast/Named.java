package litil.ast;

public class Named {
    public final String name;
    public final Type type;

    public Named(String name, Type type) {
        this.type = type;
        this.name = name;
    }

    public Named(String name) {
        this.name = name;
        this.type = null;
    }
}
