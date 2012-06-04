package litil.lexer;

import java.util.*;

public class StructuredLexer implements Lexer {
    private final Lexer lexer;
    private final String indentUnit;

    private int lastIndent = 0;
    private Deque<Token> tokens = new LinkedList<Token>();

    public StructuredLexer(Lexer lexer, String indentUnit) {
        this.lexer = lexer;
        this.indentUnit = indentUnit;
    }

    public Token pop() throws LexingException {
        if (!tokens.isEmpty()) {
            return tokens.poll();
        }
        Token tk = lexer.pop();
        if (tk.type == Token.Type.INDENT) {
            int iSize = indentSize(tk.text);
            if (iSize == -1) {
                System.err.println(tk);
                throw new LexingException("Invalid line indentation width ("+tk.text.length()+", not a multiple of "+indentUnit.length()+")", getCurrentLine() , tk.row, tk.col);
            } else if (iSize - lastIndent > 1) {
                throw new LexingException("Invalid indent level (increased by "+(iSize - lastIndent)+")", getCurrentLine() , tk.row, tk.col);
            }
            lastIndent = iSize;
            return tk;
        } else if (tk.type == Token.Type.DEINDENT) {
            int diSize = indentSize(tk.text);
            if (diSize == -1) {
                throw new LexingException("Invalid line indentation width ("+tk.text.length()+", not a multiple of "+indentUnit.length()+")", getCurrentLine(), tk.row, tk.col);
            }

            if (lastIndent - diSize > 0) {
                //produce virtual deindents
                produceVirtualIndents(lastIndent, diSize, tk);
                lastIndent = diSize;
                return tokens.poll();
            }

        } else if (tk.type == Token.Type.EOF) {
            if (lastIndent > 1) {
                //produce virtual deindents

                produceVirtualIndents(lastIndent, 0, tk);
                lastIndent = 0;
                return tokens.poll();
            }
        }
        return tk;
    }

    private void produceVirtualIndents(int fromLevel, int toLevel, Token tk) {
        for (int i = 0; i < fromLevel - toLevel; i++) {
            int level = fromLevel - toLevel - i - 1;

            tokens.addLast(new Token(Token.Type.DEINDENT, genIndent(level), tk.row, tk.col));
        }
    }

    private String genIndent(int size) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < size; i++) {
            res.append(indentUnit);
        }
        return res.toString();
    }

    private int indentSize(String indent) {
        int i = 0;
        int uLength = indentUnit.length();
        if ((indent.length() < uLength && !indent.isEmpty()) || indent.length() % uLength != 0) {
            return -1;
        }
        while (i < indent.length() / uLength) {
            int j = indent.indexOf(indentUnit, i * uLength);
            if (j != i * uLength) {
                return -1;
            }
            i++;
        }
        return i;
    }

    public String getCurrentLine() {
        return lexer.getCurrentLine();
    }
}
