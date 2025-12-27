package ru.open.cu.student.storage;

import ru.open.cu.student.catalog.manager.CatalogManager;
import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.catalog.model.TypeDefinition;
import ru.open.cu.student.index.TID;
import ru.open.cu.student.memory.buffer.BufferPoolManager;
import ru.open.cu.student.memory.page.Page;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public final class HeapTable implements Table {
    private final CatalogManager catalog;
    private final TableDefinition table;
    private final List<ColumnDefinition> columns;
    private final BufferPoolManager bufferManager;
    private final Path tableFile;

    public HeapTable(CatalogManager catalog, TableDefinition table, BufferPoolManager bufferManager, Path tableFile) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.table = Objects.requireNonNull(table, "table");
        this.bufferManager = Objects.requireNonNull(bufferManager, "bufferManager");
        this.tableFile = Objects.requireNonNull(tableFile, "tableFile");

        List<ColumnDefinition> cols = new ArrayList<>(catalog.getTableColumns(table));
        cols.sort(Comparator.comparingInt(ColumnDefinition::getPosition));
        this.columns = List.copyOf(cols);
    }

    public TableDefinition table() {
        return table;
    }

    public Path tableFile() {
        return tableFile;
    }

    public List<ColumnDefinition> columns() {
        return columns;
    }

    @Override
    public Row read(TID tid) {
        Objects.requireNonNull(tid, "tid");
        Page page = bufferManager.getPage(tid.pageId()).getPage();
        byte[] recordData = page.read(tid.slotId());
        return new Row(deserializeFullTuple(recordData));
    }

    public Iterator<Row> scanIterator() {
        return new SeqIterator();
    }

    public Iterator<TID> tidIterator() {
        return new TidIterator();
    }

    private final class SeqIterator implements Iterator<Row> {
        private int pageId = 0;
        private int slotId = 0;
        private int currentPageSlots = -1;
        private boolean finished = false;

        @Override
        public boolean hasNext() {
            if (finished) return false;
            advanceToNextAvailable();
            return !finished;
        }

        @Override
        public Row next() {
            if (!hasNext()) throw new NoSuchElementException();
            TID tid = new TID(pageId, slotId);
            Row row = read(tid);
            slotId++;
            return row;
        }

        private void advanceToNextAvailable() {
            while (true) {
                if (pageId >= table.getPagesCount()) {
                    finished = true;
                    return;
                }

                if (currentPageSlots < 0) {
                    try {
                        currentPageSlots = bufferManager.getPage(pageId).getPage().size();
                    } catch (Exception e) {
                        finished = true;
                        return;
                    }
                }

                if (slotId < currentPageSlots) {
                    return;
                }

                pageId++;
                slotId = 0;
                currentPageSlots = -1;
            }
        }
    }

    private final class TidIterator implements Iterator<TID> {
        private int pageId = 0;
        private int slotId = 0;
        private int currentPageSlots = -1;
        private boolean finished = false;

        @Override
        public boolean hasNext() {
            if (finished) return false;
            advanceToNextAvailable();
            return !finished;
        }

        @Override
        public TID next() {
            if (!hasNext()) throw new NoSuchElementException();
            TID tid = new TID(pageId, slotId);
            slotId++;
            return tid;
        }

        private void advanceToNextAvailable() {
            while (true) {
                if (pageId >= table.getPagesCount()) {
                    finished = true;
                    return;
                }

                if (currentPageSlots < 0) {
                    try {
                        currentPageSlots = bufferManager.getPage(pageId).getPage().size();
                    } catch (Exception e) {
                        finished = true;
                        return;
                    }
                }

                if (slotId < currentPageSlots) {
                    return;
                }

                pageId++;
                slotId = 0;
                currentPageSlots = -1;
            }
        }
    }

    private List<Object> deserializeFullTuple(byte[] data) {
        List<Object> out = new ArrayList<>(columns.size());
        ByteBuffer buffer = ByteBuffer.wrap(data);

        for (ColumnDefinition col : columns) {
            TypeDefinition type = catalog.getType(col.getTypeOid());
            if (type == null) {
                throw new IllegalStateException("Unknown type oid: " + col.getTypeOid());
            }
            switch (type.getName()) {
                case "INT64" -> out.add(buffer.getLong());
                case "VARCHAR" -> {
                    int length = buffer.getInt();
                    byte[] strBytes = new byte[length];
                    buffer.get(strBytes);
                    out.add(new String(strBytes, StandardCharsets.UTF_8));
                }
                default -> throw new IllegalStateException("Unsupported type: " + type.getName());
            }
        }

        return out;
    }
}


