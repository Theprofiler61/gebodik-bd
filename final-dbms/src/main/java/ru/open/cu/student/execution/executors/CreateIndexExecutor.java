package ru.open.cu.student.execution.executors;

import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.execution.Executor;
import ru.open.cu.student.index.IndexDescriptor;
import ru.open.cu.student.index.IndexManager;
import ru.open.cu.student.index.IndexType;

import java.util.Objects;

public class CreateIndexExecutor implements Executor {
    private final IndexManager indexManager;
    private final String indexName;
    private final TableDefinition table;
    private final ColumnDefinition column;
    private final IndexType indexType;

    private boolean executed;

    public CreateIndexExecutor(IndexManager indexManager,
                               String indexName,
                               TableDefinition table,
                               ColumnDefinition column,
                               IndexType indexType) {
        this.indexManager = Objects.requireNonNull(indexManager, "indexManager");
        this.indexName = Objects.requireNonNull(indexName, "indexName");
        this.table = Objects.requireNonNull(table, "table");
        this.column = Objects.requireNonNull(column, "column");
        this.indexType = Objects.requireNonNull(indexType, "indexType");
    }

    @Override
    public void open() {
        executed = false;
    }

    @Override
    public Object next() {
        if (executed) return null;
        executed = true;

        IndexDescriptor d = new IndexDescriptor(indexName, table.getName(), column.getName(), indexType);
        indexManager.createIndex(d);
        return null;
    }

    @Override
    public void close() {
    }
}


