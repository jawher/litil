package litil.lexer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Parameterized.class)
public class BaseLexerStringsTest {
    private String input;
    private String expectedString;
    private Lexer lexer;

    private static String s(String s) {
        return "\"" + s + "\"";
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{{s("a"), "a"}, {s("abc"), "abc"}, {s("a1b2c3"), "a1b2c3"}, {s("abc def"), "abc def"},
                {s("a"), "a"}, {s("a"), "a"}, {s("1"), "1"},
                {s("\\\""), "\""}, {s("'"), "'"},
                {s("\\t"), "\t"}, {s("\\b"), "\b"}, {s("\\r"), "\r"}, {s("\\n"), "\n"}, {s("\\f"), "\f"}, {s("\\\\"), "\\"},
                {s(" "), " "},});
    }

    public BaseLexerStringsTest(String input, String expectedString) {
        this.input = input;
        this.expectedString = expectedString;
        lexer = new BaseLexer(new StringReader(input));
    }


    @Test
    public void testCharLex() {
        Token tk = lexer.pop();
        assertNotNull(tk);
        assertEquals(Token.Type.STRING, tk.type);
        assertEquals(expectedString, tk.text);
    }


}
