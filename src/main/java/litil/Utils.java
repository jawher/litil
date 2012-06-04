package litil;

import java.io.PrintStream;

public class Utils {
    public static String tab(int depth) {
        StringBuilder res = new StringBuilder("");
        for (int i = 0; i < depth; i++) {
            res.append("\t");
        }

        return res.toString();
    }

    public static PrintStream indenting(String pkg) {
        return new PrintStream(System.out) {
            @Override
            public void println(String s) {
                super.println(s);
            }
        };
    }
}
