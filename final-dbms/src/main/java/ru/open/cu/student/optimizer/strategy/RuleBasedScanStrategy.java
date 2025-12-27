package ru.open.cu.student.optimizer.strategy;

import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.index.IndexDescriptor;
import ru.open.cu.student.index.IndexManager;
import ru.open.cu.student.index.IndexType;
import ru.open.cu.student.optimizer.node.PhysicalFilterNode;
import ru.open.cu.student.optimizer.node.PhysicalIndexScanNode;
import ru.open.cu.student.optimizer.node.PhysicalPlanNode;
import ru.open.cu.student.optimizer.node.PhysicalSeqScanNode;
import ru.open.cu.student.semantic.expr.SemBinaryExpr;
import ru.open.cu.student.semantic.expr.SemColumnRef;
import ru.open.cu.student.semantic.expr.SemConst;
import ru.open.cu.student.semantic.expr.SemExpr;

import java.util.Objects;
import java.util.Optional;

public class RuleBasedScanStrategy implements ScanStrategy {
    private final IndexManager indexManager;

    public RuleBasedScanStrategy(IndexManager indexManager) {
        this.indexManager = Objects.requireNonNull(indexManager, "indexManager");
    }

    @Override
    public PhysicalPlanNode chooseScan(TableDefinition table, SemExpr predicate) {
        Objects.requireNonNull(table, "table");

        if (predicate == null) {
            return new PhysicalSeqScanNode(table);
        }

        Optional<PhysicalIndexScanNode> idx = tryIndexScan(predicate, table);
        if (idx.isPresent()) {
            return idx.get();
        }
        return new PhysicalFilterNode(predicate, new PhysicalSeqScanNode(table));
    }

    private Optional<PhysicalIndexScanNode> tryIndexScan(SemExpr predicate, TableDefinition scanTable) {
        if (!(predicate instanceof SemBinaryExpr be)) return Optional.empty();

        if (be.op() == SemBinaryExpr.Op.AND || be.op() == SemBinaryExpr.Op.OR) {
            return Optional.empty();
        }

        ExtractedComparison cmp = extractComparison(be);
        if (cmp == null) return Optional.empty();

        SemColumnRef colRef = cmp.columnRef;
        if (colRef.table() == null || colRef.column() == null) return Optional.empty();
        if (!sameTable(colRef.table(), scanTable)) return Optional.empty();

        String tableName = scanTable.getName();
        String columnName = colRef.column().getName();

        if (cmp.op == SemBinaryExpr.Op.EQ) {
            Optional<IndexDescriptor> hash = indexManager.findIndex(tableName, columnName, IndexType.HASH);
            if (hash.isPresent()) {
                return Optional.of(new PhysicalIndexScanNode(scanTable, hash.get(), cmp.value, null, null, true));
            }
            Optional<IndexDescriptor> bt = indexManager.findIndex(tableName, columnName, IndexType.BTREE);
            if (bt.isPresent()) {
                return Optional.of(new PhysicalIndexScanNode(scanTable, bt.get(), cmp.value, cmp.value, cmp.value, true));
            }
            return Optional.empty();
        }

        Optional<IndexDescriptor> bt = indexManager.findIndex(tableName, columnName, IndexType.BTREE);
        if (bt.isEmpty()) return Optional.empty();

        Object from = null;
        Object to = null;
        boolean inclusive = false;

        switch (cmp.op) {
            case GT -> {
                from = cmp.value;
                inclusive = false;
            }
            case GTE -> {
                from = cmp.value;
                inclusive = true;
            }
            case LT -> {
                to = cmp.value;
                inclusive = false;
            }
            case LTE -> {
                to = cmp.value;
                inclusive = true;
            }
            default -> {
                return Optional.empty();
            }
        }

        return Optional.of(new PhysicalIndexScanNode(scanTable, bt.get(), null, from, to, inclusive));
    }

    private boolean sameTable(ru.open.cu.student.catalog.model.TableDefinition a, ru.open.cu.student.catalog.model.TableDefinition b) {
        if (a == null || b == null) return false;
        if (a.getOid() == b.getOid()) return true;
        return Objects.equals(a.getName(), b.getName());
    }

    private static final class ExtractedComparison {
        final SemBinaryExpr.Op op;
        final SemColumnRef columnRef;
        final Object value;

        private ExtractedComparison(SemBinaryExpr.Op op, SemColumnRef columnRef, Object value) {
            this.op = op;
            this.columnRef = columnRef;
            this.value = value;
        }
    }

    private ExtractedComparison extractComparison(SemBinaryExpr be) {
        SemExpr l = be.left();
        SemExpr r = be.right();
        SemBinaryExpr.Op op = be.op();

        if (l instanceof SemColumnRef cr && r instanceof SemConst c) {
            return new ExtractedComparison(op, cr, c.value());
        }
        if (l instanceof SemConst c && r instanceof SemColumnRef cr) {
            return new ExtractedComparison(reverse(op), cr, c.value());
        }
        return null;
    }

    private SemBinaryExpr.Op reverse(SemBinaryExpr.Op op) {
        return switch (op) {
            case GT -> SemBinaryExpr.Op.LT;
            case GTE -> SemBinaryExpr.Op.LTE;
            case LT -> SemBinaryExpr.Op.GT;
            case LTE -> SemBinaryExpr.Op.GTE;
            default -> op;
        };
    }
}


