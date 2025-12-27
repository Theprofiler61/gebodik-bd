package ru.open.cu.student;

import org.junit.jupiter.api.Test;
import ru.open.cu.student.lexer.LexerImpl;
import ru.open.cu.student.lexer.Token;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LexerImplTest {
    @Test
    void tokenize_create_table_insert_select_create_index() {
        LexerImpl lexer = new LexerImpl();

        List<Token> t1 = lexer.tokenize("CREATE TABLE users (id INT64, name VARCHAR);");
        assertEquals(Token.TokenType.CREATE, t1.get(0).getType());
        assertEquals(Token.TokenType.TABLE, t1.get(1).getType());
        assertEquals("users", t1.get(2).getText());

        List<Token> t2 = lexer.tokenize("INSERT INTO users VALUES (1, 'Ann');");
        assertEquals(Token.TokenType.INSERT, t2.get(0).getType());
        assertEquals(Token.TokenType.INTO, t2.get(1).getType());
        assertEquals("users", t2.get(2).getText());
        assertTrue(t2.stream().anyMatch(t -> t.getType() == Token.TokenType.STRING && "Ann".equals(t.getText())));

        List<Token> t3 = lexer.tokenize("SELECT name FROM users WHERE id >= 1 AND name != 'x';");
        assertEquals(Token.TokenType.SELECT, t3.get(0).getType());
        assertTrue(t3.stream().anyMatch(t -> t.getType() == Token.TokenType.GTE));
        assertTrue(t3.stream().anyMatch(t -> t.getType() == Token.TokenType.NEQ));

        List<Token> tStar = lexer.tokenize("SELECT * FROM users;");
        assertTrue(tStar.stream().anyMatch(t -> t.getType() == Token.TokenType.STAR));

        List<Token> tExpr = lexer.tokenize("SELECT 1+2, 2*id FROM users;");
        assertTrue(tExpr.stream().anyMatch(t -> t.getType() == Token.TokenType.PLUS));
        assertTrue(tExpr.stream().anyMatch(t -> t.getType() == Token.TokenType.STAR));

        List<Token> t4 = lexer.tokenize("CREATE INDEX idx_users_id ON users(id) USING HASH;");
        assertEquals(Token.TokenType.CREATE, t4.get(0).getType());
        assertEquals(Token.TokenType.INDEX, t4.get(1).getType());
        assertTrue(t4.stream().anyMatch(t -> t.getType() == Token.TokenType.USING));
        assertTrue(t4.stream().anyMatch(t -> t.getType() == Token.TokenType.HASH));
    }

    @Test
    void tokenize_unterminated_string_throws() {
        LexerImpl lexer = new LexerImpl();
        assertThrows(IllegalArgumentException.class, () -> lexer.tokenize("SELECT 'oops"));
    }
}


