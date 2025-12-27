package ru.open.cu.student.catalog.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColumnDefinitionTest {
    @Test
    void testColumnDefinitionCreation() {
        ColumnDefinition column = new ColumnDefinition(1, 10, 2, "name", 0);
        
        assertEquals(1, column.getOid());
        assertEquals(10, column.getTableOid());
        assertEquals(2, column.getTypeOid());
        assertEquals("name", column.getName());
        assertEquals(0, column.getPosition());
    }

    @Test
    void testToBytesAndFromBytes() {
        ColumnDefinition original = new ColumnDefinition(1, 10, 2, "name", 0);
        byte[] data = original.toBytes();
        ColumnDefinition restored = ColumnDefinition.fromBytes(data);
        
        assertEquals(original.getOid(), restored.getOid());
        assertEquals(original.getTableOid(), restored.getTableOid());
        assertEquals(original.getTypeOid(), restored.getTypeOid());
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getPosition(), restored.getPosition());
    }

    @Test
    void testEqualsAndHashCode() {
        ColumnDefinition column1 = new ColumnDefinition(1, 10, 2, "name", 0);
        ColumnDefinition column2 = new ColumnDefinition(1, 10, 2, "name", 0);
        ColumnDefinition column3 = new ColumnDefinition(2, 10, 2, "name", 0);
        
        assertEquals(column1, column2);
        assertNotEquals(column1, column3);
        assertEquals(column1.hashCode(), column2.hashCode());
    }

    @Test
    void testToString() {
        ColumnDefinition column = new ColumnDefinition(1, 10, 2, "name", 0);
        String str = column.toString();
        
        assertTrue(str.contains("oid=1"));
        assertTrue(str.contains("tableOid=10"));
        assertTrue(str.contains("name='name'"));
        assertTrue(str.contains("position=0"));
    }
}
