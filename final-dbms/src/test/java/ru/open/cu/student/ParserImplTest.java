package ru.open.cu.student;

import org.junit.jupiter.api.Test;
import ru.open.cu.student.lexer.LexerImpl;
import ru.open.cu.student.parser.ParserImpl;
import ru.open.cu.student.parser.nodes.*;

import static org.junit.jupiter.api.Assertions.*;

class ParserImplTest {
    private final LexerImpl lexer = new LexerImpl();
    private final ParserImpl parser = new ParserImpl();

    @Test
    void parse_select_with_where_and_parentheses() {
        AstNode ast = parser.parse(lexer.tokenize("SELECT name FROM users WHERE (id >= 1 AND name != 'x') OR id = 2;"));
        assertInstanceOf(SelectStmt.class, ast);
        SelectStmt s = (SelectStmt) ast;
        assertEquals(1, s.getFromClause().size());
        assertNotNull(s.getWhereClause());
    }

    @Test
    void parse_select_star() {
        AstNode ast = parser.parse(lexer.tokenize("SELECT * FROM users;"));
        assertInstanceOf(SelectStmt.class, ast);
        SelectStmt s = (SelectStmt) ast;
        assertEquals(1, s.getTargetList().size());
        assertTrue(s.getTargetList().get(0).isStar());
        assertEquals(1, s.getFromClause().size());
        assertNull(s.getWhereClause());
    }

    @Test
    void parse_select_arithmetic_expressions() {
        AstNode ast = parser.parse(lexer.tokenize("SELECT 1+2, 2*id, name FROM users;"));
        assertInstanceOf(SelectStmt.class, ast);
        SelectStmt s = (SelectStmt) ast;
        assertEquals(3, s.getTargetList().size());
        assertEquals(1, s.getFromClause().size());
    }

    @Test
    void parse_select_constant_multiplication_2_times_2() {
        AstNode ast = parser.parse(lexer.tokenize("SELECT 2*2 FROM users;"));
        assertInstanceOf(SelectStmt.class, ast);
        SelectStmt s = (SelectStmt) ast;
        assertEquals(1, s.getTargetList().size());

        AstNode expr = s.getTargetList().get(0).getExpr();
        assertInstanceOf(AExpr.class, expr);
        AExpr ax = (AExpr) expr;
        assertEquals(AExpr.Op.MUL, ax.getOp());
        assertInstanceOf(AConst.class, ax.getLeft());
        assertInstanceOf(AConst.class, ax.getRight());
        assertEquals("2", ((AConst) ax.getLeft()).getValue());
        assertEquals("2", ((AConst) ax.getRight()).getValue());
    }

    @Test
    void parse_select_arithmetic_precedence_mul_over_add() {
        AstNode ast = parser.parse(lexer.tokenize("SELECT 2+2*2 FROM users;"));
        assertInstanceOf(SelectStmt.class, ast);
        SelectStmt s = (SelectStmt) ast;
        AstNode expr = s.getTargetList().get(0).getExpr();
        assertInstanceOf(AExpr.class, expr);
        AExpr add = (AExpr) expr;
        assertEquals(AExpr.Op.ADD, add.getOp());
        assertInstanceOf(AConst.class, add.getLeft());
        assertEquals("2", ((AConst) add.getLeft()).getValue());
        assertInstanceOf(AExpr.class, add.getRight());
        AExpr mul = (AExpr) add.getRight();
        assertEquals(AExpr.Op.MUL, mul.getOp());
    }

    @Test
    void parse_create_table() {
        AstNode ast = parser.parse(lexer.tokenize("CREATE TABLE users (id INT64, name VARCHAR);"));
        assertInstanceOf(CreateTableStmt.class, ast);
        CreateTableStmt ct = (CreateTableStmt) ast;
        assertEquals("users", ct.getTableName());
        assertEquals(2, ct.getColumns().size());
        assertEquals("id", ct.getColumns().get(0).getName());
    }

    @Test
    void parse_insert_into_optional() {
        AstNode a1 = parser.parse(lexer.tokenize("INSERT INTO users VALUES (1, 'Ann');"));
        AstNode a2 = parser.parse(lexer.tokenize("INSERT users VALUES (1, 'Ann');"));
        assertInstanceOf(InsertStmt.class, a1);
        assertInstanceOf(InsertStmt.class, a2);
        assertEquals("users", ((InsertStmt) a1).getTableName());
        assertEquals("users", ((InsertStmt) a2).getTableName());
    }

    @Test
    void parse_create_index_hash() {
        AstNode ast = parser.parse(lexer.tokenize("CREATE INDEX idx_users_id ON users(id) USING HASH;"));
        assertInstanceOf(CreateIndexStmt.class, ast);
        CreateIndexStmt ci = (CreateIndexStmt) ast;
        assertEquals("idx_users_id", ci.getIndexName());
        assertEquals("users", ci.getTableName());
        assertEquals("id", ci.getColumnName());
        assertEquals("HASH", ci.getIndexType().toUpperCase());
    }

    @Test
    void parse_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(lexer.tokenize("SELECT FROM users;")));
    }
}


