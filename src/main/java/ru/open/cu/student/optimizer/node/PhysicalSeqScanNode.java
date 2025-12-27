package ru.open.cu.student.optimizer.node;

import ru.open.cu.student.catalog.model.TableDefinition;

public record PhysicalSeqScanNode(TableDefinition table) implements PhysicalPlanNode {
}


