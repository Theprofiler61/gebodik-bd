package ru.open.cu.student.optimizer.node;

import ru.open.cu.student.catalog.model.TableDefinition;

import java.util.List;

public record PhysicalInsertNode(TableDefinition table, List<Object> values) implements PhysicalPlanNode {
}


