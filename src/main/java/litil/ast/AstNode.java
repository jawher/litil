package litil.ast;

import litil.TypeScope;

public abstract class AstNode {
    public TypeScope scope;
    public abstract String repr(int indent);

    @Override
    public String toString() {
        return repr(0);
    }
}
