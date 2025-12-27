package ru.open.cu.student.planner.node;

import ru.open.cu.student.semantic.expr.SemExpr;

public record FilterNode(SemExpr predicate, LogicalPlanNode child) implements LogicalPlanNode {
}


