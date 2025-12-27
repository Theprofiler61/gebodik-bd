package ru.open.cu.student.planner.node;

import ru.open.cu.student.catalog.model.ColumnDefinition;

import java.util.List;

public record CreateTableNode(String tableName, List<ColumnDefinition> columns) implements LogicalPlanNode {
}


