package ru.open.cu.student.optimizer.node;

import ru.open.cu.student.catalog.model.ColumnDefinition;

import java.util.List;

public record PhysicalCreateTableNode(String tableName, List<ColumnDefinition> columns) implements PhysicalPlanNode {
}


