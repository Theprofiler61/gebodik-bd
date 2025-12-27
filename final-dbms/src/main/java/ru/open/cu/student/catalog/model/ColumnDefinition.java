package ru.open.cu.student.catalog.model;

import java.nio.ByteBuffer;
import java.util.Objects;

public class ColumnDefinition {
    private final int oid;
    private final int tableOid;
    private final int typeOid;
    private final String name;
    private final int position;

    public ColumnDefinition(int oid, int tableOid, int typeOid, String name, int position) {
        this.oid = oid;
        this.tableOid = tableOid;
        this.typeOid = typeOid;
        this.name = name;
        this.position = position;
    }

    public int getOid() {
        return oid;
    }

    public int getTableOid() {
        return tableOid;
    }

    public int getTypeOid() {
        return typeOid;
    }

    public String getName() {
        return name;
    }

    public int getPosition() {
        return position;
    }

    public byte[] toBytes() {
        byte[] nameBytes = name.getBytes();
        int totalSize = 4 + 4 + 4 + 4 + 4 + nameBytes.length;
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        
        buffer.putInt(oid);
        buffer.putInt(tableOid);
        buffer.putInt(typeOid);
        buffer.putInt(nameBytes.length);
        buffer.put(nameBytes);
        buffer.putInt(position);
        
        return buffer.array();
    }

    public static ColumnDefinition fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        
        int oid = buffer.getInt();
        int tableOid = buffer.getInt();
        int typeOid = buffer.getInt();
        
        int nameLength = buffer.getInt();
        byte[] nameBytes = new byte[nameLength];
        buffer.get(nameBytes);
        String name = new String(nameBytes);
        
        int position = buffer.getInt();
        
        return new ColumnDefinition(oid, tableOid, typeOid, name, position);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColumnDefinition that = (ColumnDefinition) o;
        return oid == that.oid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(oid);
    }

    @Override
    public String toString() {
        return "ColumnDefinition{" +
                "oid=" + oid +
                ", tableOid=" + tableOid +
                ", typeOid=" + typeOid +
                ", name='" + name + '\'' +
                ", position=" + position +
                '}';
    }
}