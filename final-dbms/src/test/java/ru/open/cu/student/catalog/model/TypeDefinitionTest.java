package ru.open.cu.student.catalog.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TypeDefinitionTest {
    @Test
    void testTypeDefinitionCreation() {
        TypeDefinition type = new TypeDefinition(1, "INT64", 8);
        
        assertEquals(1, type.getOid());
        assertEquals("INT64", type.getName());
        assertEquals(8, type.getByteLength());
    }

    @Test
    void testVariableLengthType() {
        TypeDefinition type = new TypeDefinition(2, "VARCHAR", -1);
        
        assertEquals(2, type.getOid());
        assertEquals("VARCHAR", type.getName());
        assertEquals(-1, type.getByteLength());
    }

    @Test
    void testToBytesAndFromBytes() {
        TypeDefinition original = new TypeDefinition(1, "INT64", 8);
        byte[] data = original.toBytes();
        TypeDefinition restored = TypeDefinition.fromBytes(data);
        
        assertEquals(original.getOid(), restored.getOid());
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getByteLength(), restored.getByteLength());
    }

    @Test
    void testEqualsAndHashCode() {
        TypeDefinition type1 = new TypeDefinition(1, "INT64", 8);
        TypeDefinition type2 = new TypeDefinition(1, "INT64", 8);
        TypeDefinition type3 = new TypeDefinition(2, "INT64", 8);
        
        assertEquals(type1, type2);
        assertNotEquals(type1, type3);
        assertEquals(type1.hashCode(), type2.hashCode());
    }

    @Test
    void testToString() {
        TypeDefinition type = new TypeDefinition(1, "INT64", 8);
        String str = type.toString();
        
        assertTrue(str.contains("oid=1"));
        assertTrue(str.contains("name='INT64'"));
        assertTrue(str.contains("byteLength=8"));
    }
}
