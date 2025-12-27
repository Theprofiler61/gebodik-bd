package ru.open.cu.student.execution.executors;

import ru.open.cu.student.catalog.manager.CatalogManager;
import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.execution.Executor;

import java.util.List;
import java.util.Objects;

public class CreateTableExecutor implements Executor {
    private final CatalogManager catalogManager;
    private final String tableName;
    private final List<ColumnDefinition> columns;

    private boolean executed;

    public CreateTableExecutor(CatalogManager catalogManager, String tableName, List<ColumnDefinition> columns) {
        this.catalogManager = Objects.requireNonNull(catalogManager, "catalogManager");
        this.tableName = Objects.requireNonNull(tableName, "tableName");
        this.columns = Objects.requireNonNull(columns, "columns");
    }

    @Override
    public void open() {
        executed = false;
    }

    @Override
    public Object next() {
        if (executed) return null;
        executed = true;
        catalogManager.createTable(tableName, columns);
        return null;
    }

    @Override
    public void close() {
    }
}


