package ru.open.cu.student.catalog.manager;

import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.catalog.model.TypeDefinition;
import ru.open.cu.student.memory.buffer.BufferPoolManager;
import ru.open.cu.student.memory.buffer.DefaultBufferPoolManager;
import ru.open.cu.student.memory.manager.HeapPageFileManager;
import ru.open.cu.student.memory.manager.PageFileManager;
import ru.open.cu.student.memory.page.HeapPage;
import ru.open.cu.student.memory.page.Page;
import ru.open.cu.student.memory.replacer.LRUReplacer;
import ru.open.cu.student.storage.engine.BufferPoolRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultCatalogManager implements CatalogManager {
    private final Map<String, TableDefinition> tables;
    private final Map<Integer, List<ColumnDefinition>> tableColumns;
    private final Map<Integer, TypeDefinition> types;
    private final Map<String, TypeDefinition> typesByName;
    private final AtomicInteger nextTableOid;
    private final AtomicInteger nextColumnOid;
    private final AtomicInteger nextTypeOid;
    private final BufferPoolManager tableBufferManager;
    private final BufferPoolManager columnBufferManager;
    private final BufferPoolManager typeBufferManager;
    private final PageFileManager pageFileManager;
    private final Path catalogDir;

    public DefaultCatalogManager(Path catalogDir) {
        this(catalogDir, null);
    }

    public DefaultCatalogManager(Path catalogDir, BufferPoolRegistry bufferPools) {
        this.catalogDir = catalogDir;
        this.tables = new ConcurrentHashMap<>();
        this.tableColumns = new ConcurrentHashMap<>();
        this.types = new ConcurrentHashMap<>();
        this.typesByName = new ConcurrentHashMap<>();
        this.nextTableOid = new AtomicInteger(1);
        this.nextColumnOid = new AtomicInteger(1);
        this.nextTypeOid = new AtomicInteger(1);
        this.pageFileManager = new HeapPageFileManager();
        Path tableFile = catalogDir.resolve("table_definitions.dat");
        Path columnFile = catalogDir.resolve("column_definitions.dat");
        Path typeFile = catalogDir.resolve("types_definitions.dat");

        if (bufferPools != null) {
            this.tableBufferManager = bufferPools.get(tableFile);
            this.columnBufferManager = bufferPools.get(columnFile);
            this.typeBufferManager = bufferPools.get(typeFile);
        } else {
            this.tableBufferManager = new DefaultBufferPoolManager(10, pageFileManager, new LRUReplacer(), tableFile);
            this.columnBufferManager = new DefaultBufferPoolManager(10, pageFileManager, new LRUReplacer(), columnFile);
            this.typeBufferManager = new DefaultBufferPoolManager(10, pageFileManager, new LRUReplacer(), typeFile);
        }

        initializeDefaultTypes();
        loadCatalog();
    }

    private void initializeDefaultTypes() {
        TypeDefinition intType = new TypeDefinition(1, "INT64", 8);
        TypeDefinition varcharType = new TypeDefinition(2, "VARCHAR", -1);
        
        types.put(1, intType);
        types.put(2, varcharType);
        typesByName.put("INT64", intType);
        typesByName.put("VARCHAR", varcharType);
        
        nextTypeOid.set(3);
    }

    private void loadCatalog() {
        try {
            if (!Files.exists(catalogDir)) {
                Files.createDirectories(catalogDir);
            }
            
            loadTypes();
            loadTables();
            loadColumns();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load catalog", e);
        }
    }

    private void loadTypes() {
        try {
            if (!Files.exists(catalogDir.resolve("types_definitions.dat"))) {
                return;
            }
            
            int pageId = 0;
            while (true) {
                try {
                    var slot = typeBufferManager.getPage(pageId);
                    Page page = slot.getPage();
                    
                    int recordCount = page.size();
                    for (int i = 0; i < recordCount; i++) {
                        byte[] recordData = page.read(i);
                        TypeDefinition type = TypeDefinition.fromBytes(recordData);
                        types.put(type.getOid(), type);
                        typesByName.put(type.getName(), type);
                        nextTypeOid.set(Math.max(nextTypeOid.get(), type.getOid() + 1));
                    }
                    
                    pageId++;
                } catch (Exception e) {
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading types: " + e.getMessage());
        }
    }

    private void loadTables() {
        try {
            if (!Files.exists(catalogDir.resolve("table_definitions.dat"))) {
                return;
            }
            
            int pageId = 0;
            while (true) {
                try {
                    var slot = tableBufferManager.getPage(pageId);
                    Page page = slot.getPage();
                    
                    int recordCount = page.size();
                    for (int i = 0; i < recordCount; i++) {
                        byte[] recordData = page.read(i);
                        TableDefinition table = TableDefinition.fromBytes(recordData);
                        tables.put(table.getName(), table);
                        nextTableOid.set(Math.max(nextTableOid.get(), table.getOid() + 1));
                    }
                    
                    pageId++;
                } catch (Exception e) {
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading tables: " + e.getMessage());
        }
    }

    private void loadColumns() {
        try {
            if (!Files.exists(catalogDir.resolve("column_definitions.dat"))) {
                return;
            }
            
            int pageId = 0;
            while (true) {
                try {
                    var slot = columnBufferManager.getPage(pageId);
                    Page page = slot.getPage();
                    
                    int recordCount = page.size();
                    for (int i = 0; i < recordCount; i++) {
                        byte[] recordData = page.read(i);
                        ColumnDefinition column = ColumnDefinition.fromBytes(recordData);
                        
                        tableColumns.computeIfAbsent(column.getTableOid(), k -> new ArrayList<>()).add(column);
                        nextColumnOid.set(Math.max(nextColumnOid.get(), column.getOid() + 1));
                    }
                    
                    pageId++;
                } catch (Exception e) {
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading columns: " + e.getMessage());
        }
    }

    @Override
    public TableDefinition createTable(String name, List<ColumnDefinition> columns) {
        if (tables.containsKey(name)) {
            throw new IllegalArgumentException("Table " + name + " already exists");
        }

        int tableOid = nextTableOid.getAndIncrement();
        String fileNode = tableOid + ".dat";
        
        TableDefinition table = new TableDefinition(tableOid, name, "TABLE", fileNode, 0);
        tables.put(name, table);
        
        for (ColumnDefinition column : columns) {
            int columnOid = nextColumnOid.getAndIncrement();
            ColumnDefinition newColumn = new ColumnDefinition(
                columnOid, tableOid, column.getTypeOid(), column.getName(), column.getPosition()
            );
            tableColumns.computeIfAbsent(tableOid, k -> new ArrayList<>()).add(newColumn);
            saveColumn(newColumn);
        }
        
        saveTable(table);
        
        try {
            Path tableFile = catalogDir.resolve(fileNode);
            Files.createFile(tableFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create table file", e);
        }
        
        return table;
    }

    @Override
    public TableDefinition getTable(String tableName) {
        return tables.get(tableName);
    }

    @Override
    public void updateTable(TableDefinition table) {
        if (table == null) {
            throw new IllegalArgumentException("table is null");
        }
        tables.put(table.getName(), table);
        saveTable(table);
    }

    @Override
    public ColumnDefinition getColumn(TableDefinition table, String columnName) {
        List<ColumnDefinition> columns = tableColumns.get(table.getOid());
        if (columns == null) {
            return null;
        }
        
        return columns.stream()
                .filter(col -> col.getName().equals(columnName))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<TableDefinition> listTables() {
        return new ArrayList<>(tables.values());
    }

    @Override
    public List<ColumnDefinition> getTableColumns(TableDefinition table) {
        return tableColumns.getOrDefault(table.getOid(), new ArrayList<>());
    }

    @Override
    public TypeDefinition getType(int typeOid) {
        return types.get(typeOid);
    }

    @Override
    public TypeDefinition getTypeByName(String typeName) {
        return typesByName.get(typeName);
    }

    private void saveTable(TableDefinition table) {
        try {
            Path tableFile = catalogDir.resolve("table_definitions.dat");
            if (!Files.exists(tableFile)) {
                Files.createFile(tableFile);
            }
            
            int pageId = 0;
            while (true) {
                try {
                    var slot = tableBufferManager.getPage(pageId);
                    Page page = slot.getPage();
                    
                    if (page.size() < getMaxRecordsPerPage()) {
                        byte[] tableData = table.toBytes();
                        page.write(tableData);
                        tableBufferManager.updatePage(pageId, page);
                        return;
                    }
                    pageId++;
                } catch (Exception e) {
                    Page newPage = new HeapPage(pageId);
                    byte[] tableData = table.toBytes();
                    newPage.write(tableData);
                    pageFileManager.write(newPage, catalogDir.resolve("table_definitions.dat"));
                    return;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save table", e);
        }
    }

    private void saveColumn(ColumnDefinition column) {
        try {
            Path columnFile = catalogDir.resolve("column_definitions.dat");
            if (!Files.exists(columnFile)) {
                Files.createFile(columnFile);
            }
            
            int pageId = 0;
            while (true) {
                try {
                    var slot = columnBufferManager.getPage(pageId);
                    Page page = slot.getPage();
                    
                    if (page.size() < getMaxRecordsPerPage()) {
                        byte[] columnData = column.toBytes();
                        page.write(columnData);
                        columnBufferManager.updatePage(pageId, page);
                        return;
                    }
                    pageId++;
                } catch (Exception e) {
                    Page newPage = new HeapPage(pageId);
                    byte[] columnData = column.toBytes();
                    newPage.write(columnData);
                    pageFileManager.write(newPage, catalogDir.resolve("column_definitions.dat"));
                    return;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save column", e);
        }
    }

    private int getMaxRecordsPerPage() {
        return 50;
    }

    public void flush() {
        tableBufferManager.flushAllPages();
        columnBufferManager.flushAllPages();
        typeBufferManager.flushAllPages();
    }
}
