package ru.open.cu.student.catalog.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.catalog.model.TypeDefinition;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultCatalogManagerTest {
    @TempDir
    Path tempDir;

    private DefaultCatalogManager catalogManager;

    @BeforeEach
    void setUp() {
        catalogManager = new DefaultCatalogManager(tempDir);
    }

    @Test
    void testCreateTable() {
        List<ColumnDefinition> columns = Arrays.asList(
            new ColumnDefinition(0, 0, 1, "id", 0),
            new ColumnDefinition(0, 0, 2, "name", 1)
        );

        TableDefinition table = catalogManager.createTable("users", columns);

        assertNotNull(table);
        assertEquals("users", table.getName());
        assertEquals("TABLE", table.getType());
        assertTrue(table.getFileNode().endsWith(".dat"));
    }

    @Test
    void testGetTable() {
        List<ColumnDefinition> columns = Arrays.asList(
            new ColumnDefinition(0, 0, 1, "id", 0)
        );

        catalogManager.createTable("users", columns);
        TableDefinition table = catalogManager.getTable("users");

        assertNotNull(table);
        assertEquals("users", table.getName());
    }

    @Test
    void testGetTableNotFound() {
        TableDefinition table = catalogManager.getTable("nonexistent");
        assertNull(table);
    }

    @Test
    void testGetColumn() {
        List<ColumnDefinition> columns = Arrays.asList(
            new ColumnDefinition(0, 0, 1, "id", 0),
            new ColumnDefinition(0, 0, 2, "name", 1)
        );

        TableDefinition table = catalogManager.createTable("users", columns);
        ColumnDefinition column = catalogManager.getColumn(table, "name");

        assertNotNull(column);
        assertEquals("name", column.getName());
        assertEquals(1, column.getPosition());
    }

    @Test
    void testListTables() {
        List<ColumnDefinition> columns1 = Arrays.asList(
            new ColumnDefinition(0, 0, 1, "id", 0)
        );
        List<ColumnDefinition> columns2 = Arrays.asList(
            new ColumnDefinition(0, 0, 1, "id", 0)
        );

        catalogManager.createTable("users", columns1);
        catalogManager.createTable("orders", columns2);

        List<TableDefinition> tables = catalogManager.listTables();
        assertEquals(2, tables.size());
    }

    @Test
    void testGetTableColumns() {
        List<ColumnDefinition> columns = Arrays.asList(
            new ColumnDefinition(0, 0, 1, "id", 0),
            new ColumnDefinition(0, 0, 2, "name", 1)
        );

        TableDefinition table = catalogManager.createTable("users", columns);
        List<ColumnDefinition> tableColumns = catalogManager.getTableColumns(table);

        assertEquals(2, tableColumns.size());
        assertEquals("id", tableColumns.get(0).getName());
        assertEquals("name", tableColumns.get(1).getName());
    }

    @Test
    void testGetType() {
        TypeDefinition type = catalogManager.getType(1);
        assertNotNull(type);
        assertEquals("INT64", type.getName());
    }

    @Test
    void testGetTypeByName() {
        TypeDefinition type = catalogManager.getTypeByName("VARCHAR");
        assertNotNull(type);
        assertEquals("VARCHAR", type.getName());
        assertEquals(-1, type.getByteLength());
    }

    @Test
    void testCreateDuplicateTable() {
        List<ColumnDefinition> columns = Arrays.asList(
            new ColumnDefinition(0, 0, 1, "id", 0)
        );

        catalogManager.createTable("users", columns);
        
        assertThrows(IllegalArgumentException.class, () -> {
            catalogManager.createTable("users", columns);
        });
    }

    @Test
    void testFlush() {
        List<ColumnDefinition> columns = Arrays.asList(
            new ColumnDefinition(0, 0, 1, "id", 0)
        );

        catalogManager.createTable("users", columns);
        assertDoesNotThrow(() -> catalogManager.flush());
    }
}
