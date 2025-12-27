package ru.open.cu.student.planner.node;

import ru.open.cu.student.semantic.QueryTree;

import java.util.List;

public record ProjectNode(List<QueryTree.SelectItem> targets, LogicalPlanNode child) implements LogicalPlanNode {
}


