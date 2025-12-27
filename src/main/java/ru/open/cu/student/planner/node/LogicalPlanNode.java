package ru.open.cu.student.planner.node;

public sealed interface LogicalPlanNode permits
        CreateTableNode,
        CreateIndexNode,
        InsertNode,
        SeqScanNode,
        FilterNode,
        ProjectNode {
}


