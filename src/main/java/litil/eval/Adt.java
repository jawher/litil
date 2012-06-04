package litil.eval;

import java.util.List;
import java.util.regex.Pattern;

class Adt {
    public final String tag;
    public final List<Object> args;

    public Adt(String tag, List<Object> args) {
        this.tag = tag;
        this.args = args;
    }


    @Override
    public String toString() {
        return "Adt<" + tag + ">" + args + "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Adt adt = (Adt) o;

        if (args != null ? !args.equals(adt.args) : adt.args != null) return false;
        if (tag != null ? !tag.equals(adt.tag) : adt.tag != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = tag != null ? tag.hashCode() : 0;
        result = 31 * result + (args != null ? args.hashCode() : 0);
        return result;
    }
}