package ru.open.cu.student.planner.node;

import ru.open.cu.student.catalog.model.TableDefinition;

import java.util.List;

public record InsertNode(TableDefinition table, List<Object> values) implements LogicalPlanNode {
}


