package ru.open.cu.student.optimizer.node;

import ru.open.cu.student.semantic.expr.SemExpr;

public record PhysicalFilterNode(SemExpr predicate, PhysicalPlanNode child) implements PhysicalPlanNode {
}


