package ru.open.cu.student.catalog.operation;

import ru.open.cu.student.catalog.manager.CatalogManager;
import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.catalog.model.TypeDefinition;
import ru.open.cu.student.memory.buffer.BufferPoolManager;
import ru.open.cu.student.memory.manager.HeapPageFileManager;
import ru.open.cu.student.memory.manager.PageFileManager;
import ru.open.cu.student.memory.page.HeapPage;
import ru.open.cu.student.memory.page.Page;
import ru.open.cu.student.index.TID;
import ru.open.cu.student.storage.Row;
import ru.open.cu.student.storage.engine.BufferPoolRegistry;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DefaultOperationManager implements OperationManager {
    private final CatalogManager catalogManager;
    private final PageFileManager pageFileManager;
    private final Path catalogDir;
    private final BufferPoolRegistry bufferPools;

    public DefaultOperationManager(CatalogManager catalogManager, Path catalogDir) {
        this(catalogManager, catalogDir, new BufferPoolRegistry(10, new HeapPageFileManager()));
    }

    @Override
    public TID insert(String tableName, List<Object> values) {
        TableDefinition table = catalogManager.getTable(tableName);
        if (table == null) {
            throw new IllegalArgumentException("Table " + tableName + " not found");
        }

        List<ColumnDefinition> columns = catalogManager.getTableColumns(table);
        if (columns.size() != values.size()) {
            throw new IllegalArgumentException("Column count mismatch");
        }

        columns.sort((a, b) -> Integer.compare(a.getPosition(), b.getPosition()));

        byte[] tupleData = serializeTuple(values, columns);

        Path tableFile = catalogDir.resolve(table.getFileNode());
        BufferPoolManager bufferManager = bufferPools.get(tableFile);

        int pageId = 0;
        while (true) {
            Page page;
            boolean existing;
            try {
                page = bufferManager.getPage(pageId).getPage();
                existing = true;
            } catch (Exception e) {
                page = new HeapPage(pageId);
                existing = false;
            }

            int slotIdBefore = page.size();
            try {
                page.write(tupleData);

                if (existing) {
                    bufferManager.updatePage(pageId, page);
                    bufferManager.flushPage(pageId);
                } else {
                    pageFileManager.write(page, tableFile);
                }

                int newPagesCount = Math.max(table.getPagesCount(), pageId + 1);
                if (newPagesCount != table.getPagesCount()) {
                    table.setPagesCount(newPagesCount);
                    catalogManager.updateTable(table);
                }

                return new TID(pageId, slotIdBefore);
            } catch (IllegalArgumentException notEnoughSpace) {
                if (!existing && slotIdBefore == 0) {
                    throw new IllegalArgumentException("Tuple is too large to fit into a heap page", notEnoughSpace);
                }
                pageId++;
            }
        }
    }

    @Override
    public Row read(String tableName, TID tid) {
        Objects.requireNonNull(tid, "tid");
        TableDefinition table = catalogManager.getTable(tableName);
        if (table == null) {
            throw new IllegalArgumentException("Table " + tableName + " not found");
        }

        List<ColumnDefinition> allColumns = catalogManager.getTableColumns(table);
        allColumns.sort((a, b) -> Integer.compare(a.getPosition(), b.getPosition()));

        Path tableFile = catalogDir.resolve(table.getFileNode());
        BufferPoolManager bufferManager = bufferPools.get(tableFile);

        Page page = bufferManager.getPage(tid.pageId()).getPage();
        byte[] recordData = page.read(tid.slotId());
        List<Object> values = deserializeFullTuple(recordData, allColumns);
        return new Row(values);
    }

    private byte[] serializeTuple(List<Object> values, List<ColumnDefinition> columns) {
        ByteBuffer buffer = ByteBuffer.allocate(calculateTupleSize(values, columns));
        
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            ColumnDefinition column = columns.get(i);
            TypeDefinition type = catalogManager.getType(column.getTypeOid());
            
            if (type.getName().equals("INT64")) {
                buffer.putLong((Long) value);
            } else if (type.getName().equals("VARCHAR")) {
                String str = value == null ? "" : value.toString();
                byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
                buffer.putInt(strBytes.length);
                buffer.put(strBytes);
            }
        }
        
        return buffer.array();
    }

    private List<Object> deserializeFullTuple(byte[] data, List<ColumnDefinition> allColumns) {
        List<Object> out = new ArrayList<>(allColumns.size());
        ByteBuffer buffer = ByteBuffer.wrap(data);

        for (ColumnDefinition column : allColumns) {
            TypeDefinition type = catalogManager.getType(column.getTypeOid());
            if (type.getName().equals("INT64")) {
                out.add(buffer.getLong());
            } else if (type.getName().equals("VARCHAR")) {
                int length = buffer.getInt();
                byte[] strBytes = new byte[length];
                buffer.get(strBytes);
                out.add(new String(strBytes, StandardCharsets.UTF_8));
            } else {
                throw new IllegalStateException("Unsupported type: " + type.getName());
            }
        }
        return out;
    }

    private int calculateTupleSize(List<Object> values, List<ColumnDefinition> columns) {
        int size = 0;
        for (int i = 0; i < values.size(); i++) {
            ColumnDefinition column = columns.get(i);
            TypeDefinition type = catalogManager.getType(column.getTypeOid());
            
            if (type.getName().equals("INT64")) {
                size += 8;
            } else if (type.getName().equals("VARCHAR")) {
                String str = values.get(i) == null ? "" : values.get(i).toString();
                size += 4 + str.getBytes(StandardCharsets.UTF_8).length;
            }
        }
        return size;
    }

    public DefaultOperationManager(CatalogManager catalogManager, Path catalogDir, BufferPoolRegistry bufferPools) {
        this.catalogManager = Objects.requireNonNull(catalogManager, "catalogManager");
        this.catalogDir = Objects.requireNonNull(catalogDir, "catalogDir");
        this.bufferPools = Objects.requireNonNull(bufferPools, "bufferPools");
        this.pageFileManager = new HeapPageFileManager();
    }
}
