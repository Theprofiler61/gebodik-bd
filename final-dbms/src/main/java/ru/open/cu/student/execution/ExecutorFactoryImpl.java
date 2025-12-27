package ru.open.cu.student.execution;

import ru.open.cu.student.execution.executors.*;
import ru.open.cu.student.index.Index;
import ru.open.cu.student.index.IndexManager;
import ru.open.cu.student.index.IndexType;
import ru.open.cu.student.index.btree.BPlusTreeIndex;
import ru.open.cu.student.index.hash.HashIndex;
import ru.open.cu.student.optimizer.node.*;
import ru.open.cu.student.storage.HeapTable;
import ru.open.cu.student.storage.engine.StorageEngine;

import java.nio.file.Path;
import java.util.Objects;

public class ExecutorFactoryImpl implements ExecutorFactory {
    private final StorageEngine storage;
    private final IndexManager indexManager;

    public ExecutorFactoryImpl(StorageEngine storage, IndexManager indexManager) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.indexManager = Objects.requireNonNull(indexManager, "indexManager");
    }

    @Override
    public Executor createExecutor(PhysicalPlanNode plan) {
        Objects.requireNonNull(plan, "plan");

        if (plan instanceof PhysicalCreateTableNode ct) {
            return new CreateTableExecutor(storage.catalog(), ct.tableName(), ct.columns());
        }
        if (plan instanceof PhysicalCreateIndexNode ci) {
            return new CreateIndexExecutor(indexManager, ci.indexName(), ci.table(), ci.column(), ci.indexType());
        }
        if (plan instanceof PhysicalInsertNode ins) {
            return new InsertExecutor(storage.operations(), indexManager, ins.table(), ins.values());
        }
        if (plan instanceof PhysicalProjectNode pr) {
            return new ProjectExecutor(pr.targets(), createExecutor(pr.child()));
        }
        if (plan instanceof PhysicalFilterNode f) {
            return new FilterExecutor(f.predicate(), createExecutor(f.child()));
        }
        if (plan instanceof PhysicalSeqScanNode scan) {
            return new SeqScanExecutor(openHeapTable(scan.table()));
        }
        if (plan instanceof PhysicalIndexScanNode idx) {
            HeapTable table = openHeapTable(idx.table());
            Index index = indexManager.getIndex(idx.index().name());
            if (idx.index().type() == IndexType.HASH) {
                return new HashIndexScanExecutor((HashIndex) index, (Comparable<?>) idx.searchKey(), table);
            }
            if (idx.index().type() == IndexType.BTREE) {
                if (idx.hasRange()) {
                    return new BTreeIndexScanExecutor(
                            (BPlusTreeIndex) index,
                            (Comparable<?>) idx.rangeFrom(),
                            (Comparable<?>) idx.rangeTo(),
                            idx.inclusive(),
                            table
                    );
                }
                return new BTreeIndexScanExecutor((BPlusTreeIndex) index, (Comparable<?>) idx.searchKey(), table);
            }
            throw new UnsupportedOperationException("Unsupported index type: " + idx.index().type());
        }

        throw new UnsupportedOperationException("Unsupported physical plan node: " + plan.getClass().getSimpleName());
    }

    private HeapTable openHeapTable(ru.open.cu.student.catalog.model.TableDefinition table) {
        Path tableFile = storage.dataDir().resolve(table.getFileNode());
        return new HeapTable(storage.catalog(), table, storage.bufferPools().get(tableFile), tableFile);
    }
}


