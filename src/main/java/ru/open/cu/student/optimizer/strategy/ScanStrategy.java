package ru.open.cu.student.optimizer.strategy;

import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.optimizer.node.PhysicalPlanNode;
import ru.open.cu.student.semantic.expr.SemExpr;

public interface ScanStrategy {
    PhysicalPlanNode chooseScan(TableDefinition table, SemExpr predicate);
}


