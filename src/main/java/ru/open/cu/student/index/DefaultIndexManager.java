package ru.open.cu.student.index;

import ru.open.cu.student.catalog.manager.CatalogManager;
import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.index.btree.BPlusTreeIndexImpl;
import ru.open.cu.student.index.hash.HashIndexImpl;
import ru.open.cu.student.memory.manager.HeapPageFileManager;
import ru.open.cu.student.memory.manager.PageFileManager;
import ru.open.cu.student.memory.page.HeapPage;
import ru.open.cu.student.memory.page.Page;
import ru.open.cu.student.storage.HeapTable;
import ru.open.cu.student.storage.Row;
import ru.open.cu.student.storage.engine.BufferPoolRegistry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultIndexManager implements IndexManager {
    private static final String META_FILE = "index_definitions.dat";
    private static final int DEFAULT_BTREE_ORDER = 3;

    private final CatalogManager catalog;
    private final BufferPoolRegistry bufferPools;
    private final PageFileManager pageFileManager;
    private final Path dataDir;
    private final Path metaFile;
    private final Path indexesDir;

    private final Map<String, IndexEntry> byName = new ConcurrentHashMap<>();
    private final Map<LookupKey, IndexEntry> byLookup = new ConcurrentHashMap<>();
    private final Map<String, List<IndexEntry>> byTable = new ConcurrentHashMap<>();

    public DefaultIndexManager(Path dataDir, CatalogManager catalog, BufferPoolRegistry bufferPools) {
        this.dataDir = dataDir.toAbsolutePath().normalize();
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.bufferPools = Objects.requireNonNull(bufferPools, "bufferPools");
        this.pageFileManager = new HeapPageFileManager();
        this.metaFile = this.dataDir.resolve(META_FILE);
        this.indexesDir = this.dataDir.resolve("indexes");

        initDirs();
        loadDefinitions();
    }

    @Override
    public Optional<IndexDescriptor> findIndex(String tableName, String columnName, IndexType type) {
        IndexEntry e = byLookup.get(new LookupKey(tableName, columnName, type));
        return e == null ? Optional.empty() : Optional.of(e.descriptor);
    }

    @Override
    public Index getIndex(String indexName) {
        IndexEntry e = byName.get(indexName);
        if (e == null) {
            throw new IllegalArgumentException("Index not found: " + indexName);
        }
        return e.index;
    }

    @Override
    public void createIndex(IndexDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");

        if (byName.containsKey(descriptor.name())) {
            throw new IllegalArgumentException("Index already exists: " + descriptor.name());
        }

        TableDefinition table = catalog.getTable(descriptor.tableName());
        if (table == null) {
            throw new IllegalArgumentException("Table not found: " + descriptor.tableName());
        }
        ColumnDefinition column = catalog.getColumn(table, descriptor.columnName());
        if (column == null) {
            throw new IllegalArgumentException("Column not found: " + descriptor.tableName() + "." + descriptor.columnName());
        }

        Index index = instantiateIndex(descriptor);
        persistDefinition(new IndexDefinition(descriptor.name(), descriptor.tableName(), descriptor.columnName(), descriptor.type()));

        buildIndex(table, column, index);

        register(descriptor, index, column.getPosition());
    }

    @Override
    public void onInsert(String tableName, List<Object> values, TID tid) {
        Objects.requireNonNull(tableName, "tableName");
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(tid, "tid");

        List<IndexEntry> entries = byTable.get(tableName);
        if (entries == null || entries.isEmpty()) return;

        for (IndexEntry e : entries) {
            int pos = e.columnPosition;
            if (pos < 0 || pos >= values.size()) continue;
            Object key = values.get(pos);
            if (!(key instanceof Comparable<?> c)) {
                throw new IllegalArgumentException("Index key is not Comparable: " + key);
            }
            e.index.insert(c, tid);
        }
    }

    private record LookupKey(String tableName, String columnName, IndexType type) {
    }

    private static final class IndexEntry {
        final IndexDescriptor descriptor;
        final Index index;
        final int columnPosition;

        private IndexEntry(IndexDescriptor descriptor, Index index, int columnPosition) {
            this.descriptor = descriptor;
            this.index = index;
            this.columnPosition = columnPosition;
        }
    }

    private void initDirs() {
        try {
            Files.createDirectories(dataDir);
            Files.createDirectories(indexesDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to init index dirs in: " + dataDir, e);
        }
    }

    private Index instantiateIndex(IndexDescriptor d) {
        if (d.type() == IndexType.HASH) {
            Path path = indexesDir.resolve(d.name() + ".idx");
            return new HashIndexImpl(d.name(), d.columnName(), pageFileManager, null, path);
        }
        if (d.type() == IndexType.BTREE) {
            Path path = indexesDir.resolve(d.name() + ".bpt");
            return new BPlusTreeIndexImpl(d.name(), d.columnName(), DEFAULT_BTREE_ORDER, pageFileManager, path);
        }
        throw new UnsupportedOperationException("Index type not implemented yet: " + d.type());
    }

    private void register(IndexDescriptor d, Index index, int columnPosition) {
        IndexEntry entry = new IndexEntry(d, index, columnPosition);
        byName.put(d.name(), entry);
        byLookup.put(new LookupKey(d.tableName(), d.columnName(), d.type()), entry);
        byTable.computeIfAbsent(d.tableName(), __ -> new ArrayList<>()).add(entry);
    }

    private void buildIndex(TableDefinition table, ColumnDefinition column, Index index) {
        Path tableFile = dataDir.resolve(table.getFileNode());
        HeapTable heapTable = new HeapTable(catalog, table, bufferPools.get(tableFile), tableFile);
        Iterator<TID> tids = heapTable.tidIterator();
        while (tids.hasNext()) {
            TID tid = tids.next();
            Row row = heapTable.read(tid);
            Object key = row.get(column.getPosition());
            if (key == null) continue;
            if (!(key instanceof Comparable<?> c)) {
                throw new IllegalArgumentException("Index key is not Comparable: " + key);
            }
            index.insert(c, tid);
        }
    }

    private void loadDefinitions() {
        if (!Files.exists(metaFile)) {
            return;
        }

        int pageId = 0;
        while (true) {
            try {
                Page page = pageFileManager.read(pageId, metaFile);
                for (int i = 0; i < page.size(); i++) {
                    IndexDefinition def = IndexDefinition.fromBytes(page.read(i));
                    IndexDescriptor desc = new IndexDescriptor(def.indexName(), def.tableName(), def.columnName(), def.type());

                    TableDefinition table = catalog.getTable(desc.tableName());
                    if (table == null) continue;
                    ColumnDefinition col = catalog.getColumn(table, desc.columnName());
                    if (col == null) continue;

                    Index index = instantiateIndex(desc);
                    // HASH indexes are persisted on disk by their implementation.
                    // BTREE implementation in this module is in-memory, so we rebuild it by scanning the table.
                    if (desc.type() == IndexType.BTREE) {
                        buildIndex(table, col, index);
                    }
                    register(desc, index, col.getPosition());
                }
                pageId++;
            } catch (Exception e) {
                break;
            }
        }
    }

    private void persistDefinition(IndexDefinition def) {
        try {
            if (!Files.exists(metaFile)) {
                Files.createFile(metaFile);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create index meta file: " + metaFile, e);
        }

        byte[] bytes = def.toBytes();
        int pageId = 0;
        while (true) {
            try {
                Page page = pageFileManager.read(pageId, metaFile);
                try {
                    page.write(bytes);
                    pageFileManager.write(page, metaFile);
                    return;
                } catch (IllegalArgumentException noSpace) {
                    pageId++;
                }
            } catch (Exception e) {
                Page newPage = new HeapPage(pageId);
                newPage.write(bytes);
                pageFileManager.write(newPage, metaFile);
                return;
            }
        }
    }
}


