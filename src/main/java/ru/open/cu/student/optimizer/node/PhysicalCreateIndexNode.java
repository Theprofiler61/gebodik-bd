package ru.open.cu.student.optimizer.node;

import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.index.IndexType;

public record PhysicalCreateIndexNode(String indexName, TableDefinition table, ColumnDefinition column, IndexType indexType)
        implements PhysicalPlanNode {
}


