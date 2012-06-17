package litil.lexer;

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
}
