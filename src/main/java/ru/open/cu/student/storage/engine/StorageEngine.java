package ru.open.cu.student.storage.engine;

import ru.open.cu.student.catalog.manager.CatalogManager;
import ru.open.cu.student.catalog.manager.DefaultCatalogManager;
import ru.open.cu.student.catalog.operation.DefaultOperationManager;
import ru.open.cu.student.catalog.operation.OperationManager;
import ru.open.cu.student.index.DefaultIndexManager;
import ru.open.cu.student.index.IndexManager;
import ru.open.cu.student.memory.manager.HeapPageFileManager;
import ru.open.cu.student.memory.manager.PageFileManager;

import java.nio.file.Files;
import java.nio.file.Path;

public final class StorageEngine implements AutoCloseable {
    private final Path dataDir;
    private final PageFileManager pageFileManager;
    private final BufferPoolRegistry bufferPools;
    private final DefaultCatalogManager catalogManager;
    private final DefaultOperationManager operationManager;
    private final IndexManager indexManager;

    public StorageEngine(Path dataDir, int bufferPoolSizePerFile) {
        try {
            Files.createDirectories(dataDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create dataDir: " + dataDir, e);
        }
        this.dataDir = dataDir.toAbsolutePath().normalize();
        this.pageFileManager = new HeapPageFileManager();
        this.bufferPools = new BufferPoolRegistry(bufferPoolSizePerFile, pageFileManager);
        this.catalogManager = new DefaultCatalogManager(this.dataDir, bufferPools);
        this.operationManager = new DefaultOperationManager(catalogManager, this.dataDir, bufferPools);
        this.indexManager = new DefaultIndexManager(this.dataDir, catalogManager, bufferPools);
    }

    public Path dataDir() {
        return dataDir;
    }

    public CatalogManager catalog() {
        return catalogManager;
    }

    public OperationManager operations() {
        return operationManager;
    }

    public IndexManager indexes() {
        return indexManager;
    }

    public BufferPoolRegistry bufferPools() {
        return bufferPools;
    }

    @Override
    public void close() {
        bufferPools.flushAll();
        catalogManager.flush();
    }
}


