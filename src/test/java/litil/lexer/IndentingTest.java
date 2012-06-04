package litil.lexer;

import org.junit.Test;

import java.io.StringReader;

import static org.junit.Assert.*;

public class IndentingTest {
    private Lexer setup(String input) {
        return new BaseLexer(new StringReader(input));
    }

    @Test
    public void testTrimNewLinesInTheBeginning() {
        Lexer lex = setup("\n\n\na");
        Token tk = lex.pop();
        assertNotNull(tk);
        assertEquals(Token.Type.NAME, tk.type);//not newline
    }

    @Test
    public void testTrimNewLinesInTheBeginningWithEmptyFile() {
        Lexer lex = setup("\n\n\n");
        Token tk = lex.pop();
        assertNotNull(tk);
        assertEquals(Token.Type.EOF, tk.type);//not newline

    }

    @Test
    public void testTrimNewLinesInTheEnd() {
        Lexer lex = setup("a\n\n\n");
        Token tk = lex.pop();
        assertNotNull(tk);
        assertEquals(Token.Type.NAME, tk.type);

        tk = lex.pop();
        assertNotNull(tk);
        assertEquals(Token.Type.EOF, tk.type);

    }

    public void testNewLines() {
        Lexer lex = setup("a\nb\nc");

        //a
        Token tk = lex.pop();
        assertNotNull(tk);
        assertEquals(Token.Type.NAME, tk.type);

        //newline
        tk = lex.pop();
        assertNotNull(tk);
        assertEquals(Token.Type.NEWLINE, tk.type);

        //b
        tk = lex.pop();
        assertNotNull(tk);
        assertEquals(Token.Type.NAME, tk.type);

        //newline
        tk = lex.pop();
        assertNotNull(tk);
        assertEquals(Token.Type.NEWLINE, tk.type);

        //c
        tk = lex.pop();
        assertNotNull(tk);
        assertEquals(Token.Type.NAME, tk.type);

        //eof
        tk = lex.pop();
        assertNotNull(tk);
        assertEquals(Token.Type.EOF, tk.type);
    }

    @Test
    public void testIndentInTheBeginning() {
        Lexer lex = setup("  a");
        Token tk = lex.pop();
        assertNotNull(tk);
        assertEquals(Token.Type.INDENT, tk.type);
        assertEquals("  ", tk.text);

        //a
        tk = lex.pop();
        assertNotNull(tk);
        assertEquals(Token.Type.NAME, tk.type);
    }

    @Test
    public void testIndentInTheBeginningWithEmptyLinesBefore() {
        Lexer lex = setup("  \n    \n  a");
        Token tk = lex.pop();
        assertNotNull(tk);
        assertEquals(Token.Type.INDENT, tk.type);
        assertEquals("  ", tk.text);

        //a
        tk = lex.pop();
        assertNotNull(tk);
        assertEquals(Token.Type.NAME, tk.type);
    }
}
