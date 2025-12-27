package ru.open.cu.student.semantic;

import ru.open.cu.student.catalog.manager.CatalogManager;
import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.catalog.model.TypeDefinition;
import ru.open.cu.student.index.IndexType;
import ru.open.cu.student.parser.nodes.*;
import ru.open.cu.student.semantic.expr.SemBinaryExpr;
import ru.open.cu.student.semantic.expr.SemColumnRef;
import ru.open.cu.student.semantic.expr.SemConst;
import ru.open.cu.student.semantic.expr.SemExpr;

import java.util.*;

public class SemanticAnalyzerImpl implements SemanticAnalyzer {
    @Override
    public QueryTree analyze(AstNode ast, CatalogManager catalog) {
        Objects.requireNonNull(ast, "ast");
        Objects.requireNonNull(catalog, "catalog");

        if (ast instanceof CreateTableStmt ct) {
            return analyzeCreateTable(ct, catalog);
        }
        if (ast instanceof InsertStmt ins) {
            return analyzeInsert(ins, catalog);
        }
        if (ast instanceof CreateIndexStmt ci) {
            return analyzeCreateIndex(ci, catalog);
        }
        if (ast instanceof SelectStmt sel) {
            return analyzeSelect(sel, catalog);
        }

        throw new IllegalArgumentException("Unsupported statement: " + ast.getClass().getSimpleName());
    }

    private QueryTree analyzeCreateTable(CreateTableStmt ct, CatalogManager catalog) {
        if (ct.getTableName() == null || ct.getTableName().isBlank()) {
            throw new IllegalArgumentException("Table name is empty");
        }
        if (catalog.getTable(ct.getTableName()) != null) {
            throw new IllegalArgumentException("Table already exists: " + ct.getTableName());
        }

        List<ColumnDefinition> columns = new ArrayList<>();
        int pos = 0;
        for (ColumnDef c : ct.getColumns()) {
            if (c.getName() == null || c.getName().isBlank()) {
                throw new IllegalArgumentException("Column name is empty");
            }
            TypeDefinition type = catalog.getTypeByName(normalizeTypeName(c.getTypeName()));
            if (type == null) {
                throw new IllegalArgumentException("Unknown type: " + c.getTypeName());
            }
            columns.add(new ColumnDefinition(0, 0, type.getOid(), c.getName(), pos++));
        }

        return new QueryTree.CreateTable(ct.getTableName(), columns);
    }

    private QueryTree analyzeInsert(InsertStmt ins, CatalogManager catalog) {
        TableDefinition table = catalog.getTable(ins.getTableName());
        if (table == null) {
            throw new IllegalArgumentException("Table not found: " + ins.getTableName());
        }

        List<ColumnDefinition> columns = new ArrayList<>(catalog.getTableColumns(table));
        columns.sort(Comparator.comparingInt(ColumnDefinition::getPosition));

        if (ins.getValues().size() != columns.size()) {
            throw new IllegalArgumentException("Column count mismatch: expected " + columns.size() + ", got " + ins.getValues().size());
        }

        List<Object> values = new ArrayList<>(columns.size());
        for (int i = 0; i < columns.size(); i++) {
            ColumnDefinition col = columns.get(i);
            TypeDefinition type = catalog.getType(col.getTypeOid());
            AstNode v = ins.getValues().get(i);
            if (!(v instanceof AConst c)) {
                throw new IllegalArgumentException("Only literal values supported in INSERT");
            }
            values.add(convertConst(c, type));
        }

        return new QueryTree.Insert(table, values);
    }

    private QueryTree analyzeCreateIndex(CreateIndexStmt ci, CatalogManager catalog) {
        TableDefinition table = catalog.getTable(ci.getTableName());
        if (table == null) {
            throw new IllegalArgumentException("Table not found: " + ci.getTableName());
        }

        ColumnDefinition column = catalog.getColumn(table, ci.getColumnName());
        if (column == null) {
            throw new IllegalArgumentException("Column not found: " + ci.getTableName() + "." + ci.getColumnName());
        }

        IndexType type;
        try {
            type = IndexType.valueOf(ci.getIndexType().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported index type: " + ci.getIndexType() + " (expected HASH or BTREE)");
        }

        return new QueryTree.CreateIndex(ci.getIndexName(), table, column, type);
    }

    private QueryTree analyzeSelect(SelectStmt select, CatalogManager catalog) {
        if (select.getFromClause() == null || select.getFromClause().isEmpty()) {
            throw new IllegalArgumentException("FROM clause is required");
        }
        if (select.getFromClause().size() != 1) {
            throw new IllegalArgumentException("Only single-table SELECT is supported");
        }

        Map<String, TableDefinition> aliasToTable = new HashMap<>();
        RangeVar rv = select.getFromClause().get(0);
        TableDefinition tbl = catalog.getTable(rv.getTableName());
        if (tbl == null) {
            throw new IllegalArgumentException("Table not found: " + rv.getTableName());
        }
        String alias = rv.getAlias() != null ? rv.getAlias() : tbl.getName();
        aliasToTable.put(alias, tbl);
        aliasToTable.put(tbl.getName(), tbl);

        List<QueryTree.SelectItem> explicitTargets = new ArrayList<>();
        boolean hasStar = false;
        int exprN = 0;
        for (ResTarget rt : select.getTargetList()) {
            if (rt.isStar()) {
                hasStar = true;
                continue;
            }
            exprN++;
            SemExpr expr = resolveExpr(rt.getExpr(), aliasToTable, catalog);
            String name = rt.getAlias();
            if (name == null || name.isBlank()) {
                name = defaultTargetName(rt.getExpr(), exprN);
            }
            explicitTargets.add(new QueryTree.SelectItem(name, expr));
        }

        List<QueryTree.SelectItem> targets;
        if (hasStar) {
            List<ColumnDefinition> all = new ArrayList<>(catalog.getTableColumns(tbl));
            all.sort(Comparator.comparingInt(ColumnDefinition::getPosition));
            targets = new ArrayList<>(all.size() + explicitTargets.size());
            for (ColumnDefinition c : all) {
                targets.add(new QueryTree.SelectItem(c.getName(), new SemColumnRef(tbl, c)));
            }
            targets.addAll(explicitTargets);
        } else {
            targets = explicitTargets;
        }

        SemExpr where = null;
        if (select.getWhereClause() != null) {
            where = resolveExpr(select.getWhereClause(), aliasToTable, catalog);
        }

        return new QueryTree.Select(tbl, targets, where);
    }

    private static final class ResolvedColumn {
        final TableDefinition table;
        final ColumnDefinition column;

        ResolvedColumn(TableDefinition t, ColumnDefinition c) {
            this.table = t;
            this.column = c;
        }
    }

    private ResolvedColumn resolveColumn(ColumnRef ref, Map<String, TableDefinition> aliasToTable, CatalogManager catalog) {
        if (ref.getTableName() != null) {
            TableDefinition tbl = aliasToTable.get(ref.getTableName());
            if (tbl == null) throw new IllegalArgumentException("Unknown table alias: " + ref.getTableName());
            ColumnDefinition col = catalog.getColumn(tbl, ref.getColumnName());
            if (col == null) throw new IllegalArgumentException("Column not found: " + ref.getTableName() + "." + ref.getColumnName());
            return new ResolvedColumn(tbl, col);
        }

        ResolvedColumn found = null;
        for (TableDefinition tbl : aliasToTable.values()) {
            ColumnDefinition col = catalog.getColumn(tbl, ref.getColumnName());
            if (col != null) {
                if (found != null) throw new IllegalArgumentException("Ambiguous column: " + ref.getColumnName());
                found = new ResolvedColumn(tbl, col);
            }
        }
        if (found == null) throw new IllegalArgumentException("Column not found: " + ref.getColumnName());
        return found;
    }

    private SemExpr resolveExpr(AstNode node, Map<String, TableDefinition> aliasToTable, CatalogManager catalog) {
        if (node instanceof AExpr ax) {
            SemBinaryExpr.Op op = mapOp(ax.getOp());
            SemExpr left = resolveExpr(ax.getLeft(), aliasToTable, catalog);
            SemExpr right = resolveExpr(ax.getRight(), aliasToTable, catalog);
            if (isArithmetic(op)) {
                validateArithmetic(left, right, catalog);
            } else if (isComparison(op)) {
                validateComparable(left, right, catalog);
            }
            return new SemBinaryExpr(op, left, right);
        }
        if (node instanceof ColumnRef cr) {
            ResolvedColumn rc = resolveColumn(cr, aliasToTable, catalog);
            return new SemColumnRef(rc.table, rc.column);
        }
        if (node instanceof AConst c) {
            Object v = c.getType() == AConst.ConstType.NUMBER ? Long.parseLong(c.getValue()) : c.getValue();
            return new SemConst(v);
        }
        throw new IllegalArgumentException("Unsupported expression node: " + node.getClass().getSimpleName());
    }

    private SemBinaryExpr.Op mapOp(AExpr.Op op) {
        return switch (op) {
            case EQ -> SemBinaryExpr.Op.EQ;
            case GT -> SemBinaryExpr.Op.GT;
            case LT -> SemBinaryExpr.Op.LT;
            case GTE -> SemBinaryExpr.Op.GTE;
            case LTE -> SemBinaryExpr.Op.LTE;
            case NEQ -> SemBinaryExpr.Op.NEQ;
            case AND -> SemBinaryExpr.Op.AND;
            case OR -> SemBinaryExpr.Op.OR;
            case ADD -> SemBinaryExpr.Op.ADD;
            case SUB -> SemBinaryExpr.Op.SUB;
            case MUL -> SemBinaryExpr.Op.MUL;
            case DIV -> SemBinaryExpr.Op.DIV;
        };
    }

    private boolean isArithmetic(SemBinaryExpr.Op op) {
        return op == SemBinaryExpr.Op.ADD || op == SemBinaryExpr.Op.SUB || op == SemBinaryExpr.Op.MUL || op == SemBinaryExpr.Op.DIV;
    }

    private boolean isComparison(SemBinaryExpr.Op op) {
        return op == SemBinaryExpr.Op.EQ || op == SemBinaryExpr.Op.NEQ
                || op == SemBinaryExpr.Op.GT || op == SemBinaryExpr.Op.GTE
                || op == SemBinaryExpr.Op.LT || op == SemBinaryExpr.Op.LTE;
    }

    private void validateComparable(SemExpr left, SemExpr right, CatalogManager catalog) {
        String lt = exprType(left, catalog);
        String rt = exprType(right, catalog);
        if (lt == null || rt == null) return;
        if (!lt.equals(rt)) {
            throw new IllegalArgumentException("Type mismatch in WHERE: " + lt + " vs " + rt);
        }
    }

    private void validateArithmetic(SemExpr left, SemExpr right, CatalogManager catalog) {
        String lt = exprType(left, catalog);
        String rt = exprType(right, catalog);
        if (lt == null || rt == null) return;
        if (!"INT64".equals(lt) || !"INT64".equals(rt)) {
            throw new IllegalArgumentException("Arithmetic expects INT64 operands, got " + lt + " and " + rt);
        }
    }

    private String exprType(SemExpr expr, CatalogManager catalog) {
        if (expr instanceof SemColumnRef cr) {
            TypeDefinition t = catalog.getType(cr.column().getTypeOid());
            return t == null ? null : t.getName();
        }
        if (expr instanceof SemConst c) {
            if (c.value() instanceof Long) return "INT64";
            if (c.value() instanceof String) return "VARCHAR";
        }
        if (expr instanceof SemBinaryExpr b) {
            if (b.op() == SemBinaryExpr.Op.AND || b.op() == SemBinaryExpr.Op.OR) return "BOOLEAN";
            if (isComparison(b.op())) return "BOOLEAN";
            if (isArithmetic(b.op())) return "INT64";
        }
        return null;
    }

    private String defaultTargetName(AstNode expr, int n) {
        if (expr instanceof ColumnRef cr) {
            return cr.getColumnName();
        }
        return "expr" + n;
    }

    private Object convertConst(AConst c, TypeDefinition expected) {
        if (expected == null) throw new IllegalArgumentException("Unknown expected type");
        String tn = expected.getName();
        if ("INT64".equalsIgnoreCase(tn)) {
            if (c.getType() != AConst.ConstType.NUMBER) {
                throw new IllegalArgumentException("Expected INT64, got " + c.getType());
            }
            return Long.parseLong(c.getValue());
        }
        if ("VARCHAR".equalsIgnoreCase(tn)) {
            if (c.getType() != AConst.ConstType.STRING) {
                throw new IllegalArgumentException("Expected VARCHAR, got " + c.getType());
            }
            return c.getValue();
        }
        throw new IllegalArgumentException("Unsupported type: " + tn);
    }

    private String normalizeTypeName(String typeName) {
        return typeName == null ? null : typeName.toUpperCase(Locale.ROOT);
    }
}
