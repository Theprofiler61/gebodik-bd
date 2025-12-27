package ru.open.cu.student;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.open.cu.student.lexer.LexerImpl;
import ru.open.cu.student.parser.ParserImpl;
import ru.open.cu.student.semantic.QueryTree;
import ru.open.cu.student.semantic.SemanticAnalyzerImpl;
import ru.open.cu.student.semantic.expr.SemBinaryExpr;
import ru.open.cu.student.semantic.expr.SemConst;
import ru.open.cu.student.storage.engine.StorageEngine;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SemanticAnalyzerImplTest {
    private final LexerImpl lexer = new LexerImpl();
    private final ParserImpl parser = new ParserImpl();
    private final SemanticAnalyzerImpl sem = new SemanticAnalyzerImpl();

    @Test
    void semantic_create_table_ok(@TempDir Path dir) {
        try (StorageEngine engine = new StorageEngine(dir, 10)) {
            var ast = parser.parse(lexer.tokenize("CREATE TABLE users (id INT64, name VARCHAR);"));
            QueryTree qt = sem.analyze(ast, engine.catalog());
            assertInstanceOf(QueryTree.CreateTable.class, qt);
            QueryTree.CreateTable ct = (QueryTree.CreateTable) qt;
            assertEquals("users", ct.tableName());
            assertEquals(2, ct.columns().size());
        }
    }

    @Test
    void semantic_create_table_unknown_type_throws(@TempDir Path dir) {
        try (StorageEngine engine = new StorageEngine(dir, 10)) {
            var ast = parser.parse(lexer.tokenize("CREATE TABLE users (id NOPE);"));
            assertThrows(IllegalArgumentException.class, () -> sem.analyze(ast, engine.catalog()));
        }
    }

    @Test
    void semantic_select_unknown_table_throws(@TempDir Path dir) {
        try (StorageEngine engine = new StorageEngine(dir, 10)) {
            var ast = parser.parse(lexer.tokenize("SELECT id FROM users;"));
            assertThrows(IllegalArgumentException.class, () -> sem.analyze(ast, engine.catalog()));
        }
    }

    @Test
    void semantic_select_unknown_column_throws(@TempDir Path dir) {
        try (StorageEngine engine = new StorageEngine(dir, 10)) {
            engine.catalog().createTable("users", List.of(
                    new ru.open.cu.student.catalog.model.ColumnDefinition(0, 0, 1, "id", 0)
            ));
            var ast = parser.parse(lexer.tokenize("SELECT name FROM users;"));
            assertThrows(IllegalArgumentException.class, () -> sem.analyze(ast, engine.catalog()));
        }
    }

    @Test
    void semantic_where_type_mismatch_throws(@TempDir Path dir) {
        try (StorageEngine engine = new StorageEngine(dir, 10)) {
            engine.catalog().createTable("users", List.of(
                    new ru.open.cu.student.catalog.model.ColumnDefinition(0, 0, 1, "id", 0),
                    new ru.open.cu.student.catalog.model.ColumnDefinition(0, 0, 2, "name", 1)
            ));
            var ast = parser.parse(lexer.tokenize("SELECT name FROM users WHERE id = 'x';"));
            assertThrows(IllegalArgumentException.class, () -> sem.analyze(ast, engine.catalog()));
        }
    }

    @Test
    void semantic_select_star_expands_columns(@TempDir Path dir) {
        try (StorageEngine engine = new StorageEngine(dir, 10)) {
            engine.catalog().createTable("users", List.of(
                    new ru.open.cu.student.catalog.model.ColumnDefinition(0, 0, 1, "id", 0),
                    new ru.open.cu.student.catalog.model.ColumnDefinition(0, 0, 2, "name", 1)
            ));
            var ast = parser.parse(lexer.tokenize("SELECT * FROM users;"));
            QueryTree qt = sem.analyze(ast, engine.catalog());
            assertInstanceOf(QueryTree.Select.class, qt);
            QueryTree.Select sel = (QueryTree.Select) qt;
            assertEquals(2, sel.targets().size());
            assertEquals("id", sel.targets().get(0).name());
            assertEquals("name", sel.targets().get(1).name());
        }
    }

    @Test
    void semantic_select_arithmetic_targets_ok(@TempDir Path dir) {
        try (StorageEngine engine = new StorageEngine(dir, 10)) {
            engine.catalog().createTable("users", List.of(
                    new ru.open.cu.student.catalog.model.ColumnDefinition(0, 0, 1, "id", 0),
                    new ru.open.cu.student.catalog.model.ColumnDefinition(0, 0, 2, "name", 1)
            ));
            var ast = parser.parse(lexer.tokenize("SELECT 1+2, 2*id, name FROM users;"));
            QueryTree qt = sem.analyze(ast, engine.catalog());
            assertInstanceOf(QueryTree.Select.class, qt);
            QueryTree.Select sel = (QueryTree.Select) qt;
            assertEquals(3, sel.targets().size());
            assertEquals("expr1", sel.targets().get(0).name());
            assertEquals("expr2", sel.targets().get(1).name());
            assertEquals("name", sel.targets().get(2).name());
        }
    }

    @Test
    void semantic_select_constant_multiplication_2_times_2(@TempDir Path dir) {
        try (StorageEngine engine = new StorageEngine(dir, 10)) {
            engine.catalog().createTable("users", List.of(
                    new ru.open.cu.student.catalog.model.ColumnDefinition(0, 0, 1, "id", 0)
            ));
            var ast = parser.parse(lexer.tokenize("SELECT 2*2 AS v FROM users;"));
            QueryTree qt = sem.analyze(ast, engine.catalog());
            assertInstanceOf(QueryTree.Select.class, qt);
            QueryTree.Select sel = (QueryTree.Select) qt;
            assertEquals(1, sel.targets().size());
            assertEquals("v", sel.targets().get(0).name());

            assertInstanceOf(SemBinaryExpr.class, sel.targets().get(0).expr());
            SemBinaryExpr be = (SemBinaryExpr) sel.targets().get(0).expr();
            assertEquals(SemBinaryExpr.Op.MUL, be.op());
            assertInstanceOf(SemConst.class, be.left());
            assertInstanceOf(SemConst.class, be.right());
            assertEquals(2L, ((SemConst) be.left()).value());
            assertEquals(2L, ((SemConst) be.right()).value());
        }
    }
}


