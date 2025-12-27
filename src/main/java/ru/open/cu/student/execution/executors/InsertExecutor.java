package ru.open.cu.student.execution.executors;

import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.catalog.operation.OperationManager;
import ru.open.cu.student.execution.Executor;
import ru.open.cu.student.index.IndexManager;
import ru.open.cu.student.index.TID;

import java.util.List;
import java.util.Objects;

public class InsertExecutor implements Executor {
    private final OperationManager operationManager;
    private final IndexManager indexManager;
    private final TableDefinition table;
    private final List<Object> values;

    private boolean executed;

    public InsertExecutor(OperationManager operationManager, IndexManager indexManager, TableDefinition table, List<Object> values) {
        this.operationManager = Objects.requireNonNull(operationManager, "operationManager");
        this.indexManager = Objects.requireNonNull(indexManager, "indexManager");
        this.table = Objects.requireNonNull(table, "table");
        this.values = Objects.requireNonNull(values, "values");
    }

    @Override
    public void open() {
        executed = false;
    }

    @Override
    public Object next() {
        if (executed) return null;
        executed = true;
        TID tid = operationManager.insert(table.getName(), values);
        indexManager.onInsert(table.getName(), values, tid);
        return null;
    }

    @Override
    public void close() {
    }
}


