package ru.open.cu.student.catalog.model;

import java.nio.ByteBuffer;
import java.util.Objects;

public class TypeDefinition {
    private final int oid;
    private final String name;
    private final int byteLength;

    public TypeDefinition(int oid, String name, int byteLength) {
        this.oid = oid;
        this.name = name;
        this.byteLength = byteLength;
    }

    public int getOid() {
        return oid;
    }

    public String getName() {
        return name;
    }

    public int getByteLength() {
        return byteLength;
    }

    public byte[] toBytes() {
        byte[] nameBytes = name.getBytes();
        int totalSize = 4 + 4 + 4 + nameBytes.length;
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        
        buffer.putInt(oid);
        buffer.putInt(nameBytes.length);
        buffer.put(nameBytes);
        buffer.putInt(byteLength);
        
        return buffer.array();
    }

    public static TypeDefinition fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        
        int oid = buffer.getInt();
        
        int nameLength = buffer.getInt();
        byte[] nameBytes = new byte[nameLength];
        buffer.get(nameBytes);
        String name = new String(nameBytes);
        
        int byteLength = buffer.getInt();
        
        return new TypeDefinition(oid, name, byteLength);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeDefinition that = (TypeDefinition) o;
        return oid == that.oid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(oid);
    }

    @Override
    public String toString() {
        return "TypeDefinition{" +
                "oid=" + oid +
                ", name='" + name + '\'' +
                ", byteLength=" + byteLength +
                '}';
    }
}