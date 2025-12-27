package ru.open.cu.student.optimizer.node;

import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.index.IndexDescriptor;

public record PhysicalIndexScanNode(
        TableDefinition table,
        IndexDescriptor index,
        Object searchKey,
        Object rangeFrom,
        Object rangeTo,
        boolean inclusive
) implements PhysicalPlanNode {
    public boolean hasRange() {
        return rangeFrom != null || rangeTo != null;
    }
}


