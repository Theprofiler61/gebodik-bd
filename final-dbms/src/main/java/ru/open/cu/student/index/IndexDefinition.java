package ru.open.cu.student.index;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public record IndexDefinition(String indexName, String tableName, String columnName, IndexType type) {

    public byte[] toBytes() {
        byte[] idx = indexName.getBytes(StandardCharsets.UTF_8);
        byte[] tbl = tableName.getBytes(StandardCharsets.UTF_8);
        byte[] col = columnName.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buf = ByteBuffer.allocate(
                4 + idx.length +
                        4 + tbl.length +
                        4 + col.length +
                        4
        );
        buf.putInt(idx.length).put(idx);
        buf.putInt(tbl.length).put(tbl);
        buf.putInt(col.length).put(col);
        buf.putInt(type.ordinal());
        return buf.array();
    }

    public static IndexDefinition fromBytes(byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data);
        String idx = readString(buf);
        String tbl = readString(buf);
        String col = readString(buf);
        int ord = buf.getInt();
        IndexType type = IndexType.values()[ord];
        return new IndexDefinition(idx, tbl, col, type);
    }

    private static String readString(ByteBuffer buf) {
        int len = buf.getInt();
        byte[] bytes = new byte[len];
        buf.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}


