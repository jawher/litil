package litil.cg.ast;

import litil.ast.Type;

import java.util.List;

public class BcNode {

    public static final class DeclField extends BcNode {
        public final String name;
        public final Type type;

        public DeclField(String name, Type type) {
            this.name = name;
            this.type = type;
        }
    }

    public static final class InitField extends BcNode {
        public final String name;
        public final Type type;
        public final List<BcNode> bc;

        public InitField(String name, Type type, List<BcNode> bc) {
            this.name = name;
            this.type = type;
            this.bc = bc;
        }
    }

    public static class Push extends BcNode {
        public final Object value;

        public Push(Object value) {
            this.value = value;
        }
    }
}
