package litil.lexer;

public class LexingException extends RuntimeException {
    public final String line;
    public final int row;
    public final int col;

    public LexingException(String error, String line, int row, int col) {
        super(error + " @ " + row + ":" + col + "\n" + line + "\n" + nspaces(col) + "^");
        this.line = line;
        this.row = row;
        this.col = col;
    }

    private static  String nspaces(int n) {
        StringBuilder res = new StringBuilder("");
        for (int i = 0; i < n-1; i++) {
            res.append(" ");
        }
        return res.toString();
    }
}