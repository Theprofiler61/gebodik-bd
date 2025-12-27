package ru.open.cu.student.catalog.model;

import java.nio.ByteBuffer;
import java.util.Objects;

public class TableDefinition {
    private final int oid;
    private final String name;
    private final String type;
    private final String fileNode;
    private int pagesCount;

    public TableDefinition(int oid, String name, String type, String fileNode, int pagesCount) {
        this.oid = oid;
        this.name = name;
        this.type = type;
        this.fileNode = fileNode;
        this.pagesCount = pagesCount;
    }

    public int getOid() {
        return oid;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getFileNode() {
        return fileNode;
    }

    public int getPagesCount() {
        return pagesCount;
    }

    public void setPagesCount(int pagesCount) {
        this.pagesCount = pagesCount;
    }

    public byte[] toBytes() {
        byte[] nameBytes = name.getBytes();
        byte[] typeBytes = type.getBytes();
        byte[] fileNodeBytes = fileNode.getBytes();
        
        int totalSize = 4 + 4 + 4 + 4 + 4 + nameBytes.length + typeBytes.length + fileNodeBytes.length;
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        
        buffer.putInt(oid);
        buffer.putInt(nameBytes.length);
        buffer.put(nameBytes);
        buffer.putInt(typeBytes.length);
        buffer.put(typeBytes);
        buffer.putInt(fileNodeBytes.length);
        buffer.put(fileNodeBytes);
        buffer.putInt(pagesCount);
        
        return buffer.array();
    }

    public static TableDefinition fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        
        int oid = buffer.getInt();
        
        int nameLength = buffer.getInt();
        byte[] nameBytes = new byte[nameLength];
        buffer.get(nameBytes);
        String name = new String(nameBytes);
        
        int typeLength = buffer.getInt();
        byte[] typeBytes = new byte[typeLength];
        buffer.get(typeBytes);
        String type = new String(typeBytes);
        
        int fileNodeLength = buffer.getInt();
        byte[] fileNodeBytes = new byte[fileNodeLength];
        buffer.get(fileNodeBytes);
        String fileNode = new String(fileNodeBytes);
        
        int pagesCount = buffer.getInt();
        
        return new TableDefinition(oid, name, type, fileNode, pagesCount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableDefinition that = (TableDefinition) o;
        return oid == that.oid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(oid);
    }

    @Override
    public String toString() {
        return "TableDefinition{" +
                "oid=" + oid +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", fileNode='" + fileNode + '\'' +
                ", pagesCount=" + pagesCount +
                '}';
    }
}