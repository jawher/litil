package litil.lexer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LexerStage {
    private Map<Character, LexerStage> next = new HashMap<Character, LexerStage>();
    private boolean terminal;
    private String prefix;

    public LexerStage(List<String> words) {
        this(words, 0, "");
    }

    private LexerStage(List<String> words, int pos, String prefix) {
        this.prefix = prefix;
        for (String word : words) {
            if (word.startsWith(prefix)) {
                if (word.length() - 1 < pos) {
                    terminal = true;
                } else {
                    char c = word.charAt(pos);
                    if (!next.containsKey(c)) {
                        next.put(c, new LexerStage(words, pos + 1, prefix + c));
                    }
                }
            }
        }
    }

    public LexerStage next(Character character) {
        return next.get(character);
    }

    public boolean isTerminal() {
        return terminal;
    }

    public String getValue() {
        return prefix;
    }

    @Override
    public String toString() {
        return "LexerStage{" +
                "next=" + next +
                ", terminal=" + terminal +
                ", prefix='" + prefix + '\'' +
                '}';
    }

    public static void main(String[] args) {
        LexerStage ls = new LexerStage(Arrays.asList("-", "+", "--", "++", "->", "-+<", "<", ">", "=", "<=", ">="));
        System.out.println(ls);
        String input = "<$        ";
        int i = 0;
        LexerStage s = ls;
        while (s != null) {
            LexerStage n = s.next(input.charAt(i++));
            if (n == null) {
                break;
            } else {
                s = n;
            }
            System.out.println(i + "::" + s);
        }
        if(s.isTerminal()) {
            System.out.println("=> Accepted "+s.getValue());
        } else {
            System.out.println("Rejected");
        }
        System.out.println("$="+input.charAt(i));
    }
}
