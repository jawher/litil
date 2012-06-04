package litil.ast;

import java.util.List;

public class Pattern {
    public static class WildcardPattern extends Pattern {
        @Override
        public String toString() {
            return "_";
        }
    }

    public static class IdPattern extends Pattern {
        public final String name;

        public IdPattern(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "'" + name + "'";
        }
    }

    public static class TuplePattern extends Pattern {
        public final List<Pattern> items;

        public TuplePattern(List<Pattern> items) {
            this.items = items;
        }

        @Override
        public String toString() {
            return "(" + items + ')';
        }
    }

    public static class TyConPattern extends Pattern {

        public final String name;
        public final List<Pattern> patterns;

        public TyConPattern(String name, List<Pattern> patterns) {
            this.name = name;
            this.patterns = patterns;
        }


        @Override
        public String toString() {
            return name + " " + patterns;
        }
    }
}
