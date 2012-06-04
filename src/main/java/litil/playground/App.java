package litil.playground;

import litil.lexer.*;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hello world!
 */
public class App {

    private static void baseAndLookahead() {
        Lexer lex = new BaseLexer(new InputStreamReader(App.class.getResourceAsStream("lexer.example")));
        LookaheadLexer lalex = new LookaheadLexerWrapper(new BaseLexer(new InputStreamReader(App.class.getResourceAsStream("lexer.example"))));
        int i = 1;
        Token tk = null;
        do {
            tk = lex.pop();
            System.out.println(tk + " :: " + (tk.equals(lalex.peek(i++))));
        } while (tk.type != Token.Type.EOF);
    }


    private static void structured(String file) {
        Lexer lex = new BaseLexer(new InputStreamReader(App.class.getResourceAsStream(file)));
        StructuredLexer slex = new StructuredLexer(lex, "  ");

        Token tk = null;
        do {
            tk = slex.pop();

            System.out.println(tk);
        } while (tk.type != Token.Type.EOF);

    }

    private static void structured2(String file) {
        Lexer lex = new BaseLexer(new InputStreamReader(App.class.getResourceAsStream(file)));
        StructuredLexer slex = new StructuredLexer(lex, "  ");

        Token tk;
        int lastRow = 0;
        do {
            tk = slex.pop();
            if (tk.row != lastRow) {
                System.out.println(ntimes("_", 80));
                lastRow = tk.row;
            } else {
                System.out.println(ntimes("_", 10));
            }
            //System.out.println(tk);
            System.out.println(slex.getCurrentLine() + "\n" + ntimes(" ", tk.col) + "^ " + tk);
        } while (tk.type != Token.Type.EOF);

    }

    private static void structured3(String file) {
        Lexer lex = new BaseLexer(new InputStreamReader(App.class.getResourceAsStream(file)));
        StructuredLexer slex = new StructuredLexer(lex, "  ");

        Token tk = null;
        int lastRow = 0;
        List<Token> lineTokens = new ArrayList<Token>();
        String currentLine = null;
        do {
            tk = slex.pop();
            if (tk.row != lastRow) {

                if (currentLine != null) {
                    System.out.println(ntimes("_", 80));
                    printLinetokens(currentLine, lineTokens);
                }
                lastRow = tk.row;
                currentLine = slex.getCurrentLine();
                lineTokens.clear();
            }
            lineTokens.add(tk);
        } while (tk.type != Token.Type.EOF);

    }

    private static void printLinetokens(String line, List<Token> tokens) {
        String prefix = "|";
        List<StringBuilder> lines = new ArrayList<StringBuilder>();

        Collections.reverse(tokens);
        for (Token token : tokens) {

            String tokenText = prefix + token.text;
            insert(tokenText, token.col, lines);
        }

        System.out.println(line);
        for (StringBuilder l : lines) {
            System.out.println(l);
        }
    }

    private static void insert(String text, int pos, List<StringBuilder> lines) {
        boolean inserted = false;
        List<StringBuilder> linesToUpdate = new ArrayList<StringBuilder>();
        for (StringBuilder l : lines) {
            if (insertIfPossible(text, pos, l)) {
                inserted = true;
                break;
            } else {
                linesToUpdate.add(l);
            }

        }
        if (!inserted) {
            StringBuilder l = new StringBuilder();
            lines.add(l);
            insertIfPossible(text, pos, l);
        }

        for (StringBuilder upd : linesToUpdate) {
            insertIfPossible("|", pos-1, upd);
        }

    }

    private static boolean insertIfPossible(String text, int pos, StringBuilder l) {
        //System.err.println("ins "+text+"("+text.length()+") at "+pos+" in '"+l+"'("+l.length());
        if (l.length() < pos) {
            l.append(ntimes(" ", pos - l.length()));
            l.append(text);
            return true;
        } else {
            if (l.length() <= pos + text.length() + 1) {
                l.append(ntimes(" ", pos + text.length() + 2 - l.length()));
                //System.err.println("\textend line to "+l+"("+l.length());
            }
            String part = l.substring(pos, pos + text.length() + 1);
            if (part.matches(" +")) {
                l.replace(pos, pos + text.length() + 1, text);
                return true;
            } else {
                return false;
            }
        }

    }

    private static String ntimes(String s, int n) {
        StringBuilder res = new StringBuilder("");
        for (int i = 0; i < n - 1; i++) {
            res.append(s);
        }
        return res.toString();
    }


    public static void main(String[] args) {
        //baseAndLookahead();
        structured2("eval/s99.jml");
    }
}
