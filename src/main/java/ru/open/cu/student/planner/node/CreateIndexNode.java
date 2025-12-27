package ru.open.cu.student.planner.node;

import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.index.IndexType;

public record CreateIndexNode(String indexName, TableDefinition table, ColumnDefinition column, IndexType indexType)
        implements LogicalPlanNode {
}


