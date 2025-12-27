package ru.open.cu.student.optimizer.node;

import ru.open.cu.student.semantic.QueryTree;

import java.util.List;

public record PhysicalProjectNode(List<QueryTree.SelectItem> targets, PhysicalPlanNode child) implements PhysicalPlanNode {
}


