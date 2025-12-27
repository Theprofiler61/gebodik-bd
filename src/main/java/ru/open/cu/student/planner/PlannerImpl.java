package ru.open.cu.student.planner;

import ru.open.cu.student.planner.node.*;
import ru.open.cu.student.semantic.QueryTree;

import java.util.Objects;

public class PlannerImpl implements Planner {
    @Override
    public LogicalPlanNode plan(QueryTree queryTree) {
        Objects.requireNonNull(queryTree, "queryTree");

        return switch (queryTree.type()) {
            case CREATE_TABLE -> {
                QueryTree.CreateTable ct = (QueryTree.CreateTable) queryTree;
                yield new CreateTableNode(ct.tableName(), ct.columns());
            }
            case CREATE_INDEX -> {
                QueryTree.CreateIndex ci = (QueryTree.CreateIndex) queryTree;
                yield new CreateIndexNode(ci.indexName(), ci.table(), ci.column(), ci.indexType());
            }
            case INSERT -> {
                QueryTree.Insert ins = (QueryTree.Insert) queryTree;
                yield new InsertNode(ins.table(), ins.values());
            }
            case SELECT -> {
                QueryTree.Select sel = (QueryTree.Select) queryTree;
                LogicalPlanNode child = new SeqScanNode(sel.table());
                if (sel.where() != null) {
                    child = new FilterNode(sel.where(), child);
                }
                yield new ProjectNode(sel.targets(), child);
            }
        };
    }
}


