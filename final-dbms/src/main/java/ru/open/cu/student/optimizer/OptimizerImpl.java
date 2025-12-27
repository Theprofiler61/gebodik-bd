package ru.open.cu.student.optimizer;

import ru.open.cu.student.index.IndexManager;
import ru.open.cu.student.optimizer.node.*;
import ru.open.cu.student.optimizer.strategy.RuleBasedScanStrategy;
import ru.open.cu.student.optimizer.strategy.ScanStrategy;
import ru.open.cu.student.planner.node.*;

import java.util.Objects;

public class OptimizerImpl implements Optimizer {
    private final ScanStrategy scanStrategy;

    public OptimizerImpl(IndexManager indexManager) {
        this(new RuleBasedScanStrategy(Objects.requireNonNull(indexManager, "indexManager")));
    }

    public OptimizerImpl(ScanStrategy scanStrategy) {
        this.scanStrategy = Objects.requireNonNull(scanStrategy, "scanStrategy");
    }

    @Override
    public PhysicalPlanNode optimize(LogicalPlanNode logicalPlan) {
        Objects.requireNonNull(logicalPlan, "logicalPlan");

        if (logicalPlan instanceof CreateTableNode ct) {
            return new PhysicalCreateTableNode(ct.tableName(), ct.columns());
        }
        if (logicalPlan instanceof CreateIndexNode ci) {
            return new PhysicalCreateIndexNode(ci.indexName(), ci.table(), ci.column(), ci.indexType());
        }
        if (logicalPlan instanceof InsertNode ins) {
            return new PhysicalInsertNode(ins.table(), ins.values());
        }
        if (logicalPlan instanceof ProjectNode pr) {

            System.out.println(new PhysicalProjectNode(pr.targets(), optimize(pr.child())));
            return new PhysicalProjectNode(pr.targets(), optimize(pr.child()));
        }
        if (logicalPlan instanceof FilterNode f) {
            if (f.child() instanceof SeqScanNode scan) {
                return scanStrategy.chooseScan(scan.table(), f.predicate());
            }
            return new PhysicalFilterNode(f.predicate(), optimize(f.child()));
        }
        if (logicalPlan instanceof SeqScanNode scan) {
            return new PhysicalSeqScanNode(scan.table());
        }

        throw new UnsupportedOperationException("Unsupported logical node: " + logicalPlan.getClass().getSimpleName());
    }
}


