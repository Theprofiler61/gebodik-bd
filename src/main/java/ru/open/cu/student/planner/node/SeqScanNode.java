package ru.open.cu.student.planner.node;

import ru.open.cu.student.catalog.model.TableDefinition;

public record SeqScanNode(TableDefinition table) implements LogicalPlanNode {
}


