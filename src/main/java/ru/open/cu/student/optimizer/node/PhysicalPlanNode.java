package ru.open.cu.student.optimizer.node;

public sealed interface PhysicalPlanNode permits
        PhysicalCreateTableNode,
        PhysicalCreateIndexNode,
        PhysicalInsertNode,
        PhysicalSeqScanNode,
        PhysicalIndexScanNode,
        PhysicalFilterNode,
        PhysicalProjectNode {
}


