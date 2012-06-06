package litil;

import java.io.PrintStream;

public class Utils {
    public static String tab(int depth) {
        return ntimes(depth, "\t");
    }

    public static String ntimes(int count, String s) {
        StringBuilder res = new StringBuilder("");
        for (int i = 0; i < count; i++) {
            res.append(s);
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
