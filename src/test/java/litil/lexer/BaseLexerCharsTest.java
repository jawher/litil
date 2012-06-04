package litil.lexer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class BaseLexerCharsTest {
    private String input;
    private String expectedChar;
    private Lexer lexer;

    @Parameterized.Parameters
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{{"'a'", "a"}, {"'1'", "1"}, {"'\"'", "\""}, {"'\\''", "'"}, {"'\"'", "\""},
                {"'\\t'", "\t"}, {"'\\b'", "\b"}, {"'\\r'", "\r"}, {"'\\n'", "\n"}, {"'\\f'", "\f"}, {"'\\\\'", "\\"},
                {"' '", " "},});
    }

    public BaseLexerCharsTest(String input, String expectedChar) {
        this.input = input;
        this.expectedChar = expectedChar;
        lexer = new BaseLexer(new StringReader(input));
    }


    @Test
    public void testCharLex() {
        Token tk = lexer.pop();
        assertNotNull(tk);
        assertEquals(Token.Type.CHAR, tk.type);
        assertEquals(expectedChar, tk.text);
    }


}
