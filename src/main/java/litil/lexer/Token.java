package litil.lexer;

public class Token {
    public enum Type {
        NEWLINE, INDENT, DEINDENT, NAME, NUM, STRING, CHAR, SYM, BOOL, KEYWORD, EOF
    }
    public final Type type;
    public final String text;
    public final int row, col;

    public Token(Type type, String text, int row, int col) {
        this.type = type;
        this.text = text;
        this.row = row;
        this.col = col;
    }

    @Override
    public String toString() {
        return String.format("%s ('%s') @ %d:%d", type, text, row, col);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Token token = (Token) o;

        if (col != token.col) return false;
        if (row != token.row) return false;
        if (text != null ? !text.equals(token.text) : token.text != null) return false;
        if (type != token.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + row;
        result = 31 * result + col;
        return result;
    }
}
