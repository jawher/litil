package litil.eval;

interface Fn {
    public Object eval(Object arg, ValScope scope);

    public static abstract class BiFn implements Fn {

        public Object eval(final Object arg1, ValScope scope) {
            return new Fn() {

                public Object eval(Object arg2, ValScope scope) {
                    return BiFn.this.eval(arg1, arg2, scope);
                }
            };
        }

        protected abstract Object eval(Object arg1, Object arg2, ValScope scope);
    }
}