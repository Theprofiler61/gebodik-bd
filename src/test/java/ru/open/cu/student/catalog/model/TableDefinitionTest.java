package ru.open.cu.student.catalog.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TableDefinitionTest {
    @TempDir
    Path tempDir;

    @Test
    void testTableDefinitionCreation() {
        TableDefinition table = new TableDefinition(1, "users", "TABLE", "1.dat", 5);
        
        assertEquals(1, table.getOid());
        assertEquals("users", table.getName());
        assertEquals("TABLE", table.getType());
        assertEquals("1.dat", table.getFileNode());
        assertEquals(5, table.getPagesCount());
    }

    @Test
    void testSetPagesCount() {
        TableDefinition table = new TableDefinition(1, "users", "TABLE", "1.dat", 5);
        table.setPagesCount(10);
        
        assertEquals(10, table.getPagesCount());
    }

    @Test
    void testToBytesAndFromBytes() {
        TableDefinition original = new TableDefinition(1, "users", "TABLE", "1.dat", 5);
        byte[] data = original.toBytes();
        TableDefinition restored = TableDefinition.fromBytes(data);
        
        assertEquals(original.getOid(), restored.getOid());
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getType(), restored.getType());
        assertEquals(original.getFileNode(), restored.getFileNode());
        assertEquals(original.getPagesCount(), restored.getPagesCount());
    }

    @Test
    void testEqualsAndHashCode() {
        TableDefinition table1 = new TableDefinition(1, "users", "TABLE", "1.dat", 5);
        TableDefinition table2 = new TableDefinition(1, "users", "TABLE", "1.dat", 5);
        TableDefinition table3 = new TableDefinition(2, "users", "TABLE", "1.dat", 5);
        
        assertEquals(table1, table2);
        assertNotEquals(table1, table3);
        assertEquals(table1.hashCode(), table2.hashCode());
    }

    @Test
    void testToString() {
        TableDefinition table = new TableDefinition(1, "users", "TABLE", "1.dat", 5);
        String str = table.toString();
        
        assertTrue(str.contains("oid=1"));
        assertTrue(str.contains("name='users'"));
        assertTrue(str.contains("type='TABLE'"));
    }
}
