package ru.open.cu.student.pipeline;

import ru.open.cu.student.parser.nodes.*;

import java.util.StringJoiner;

public final class AstPrinter {
    private AstPrinter() {
    }

    public static String print(AstNode node) {
        if (node == null) return "<null>";

        if (node instanceof SelectStmt s) {
            StringJoiner t = new StringJoiner(", ");
            for (ResTarget rt : s.getTargetList()) {
                t.add(print(rt));
            }
            StringJoiner f = new StringJoiner(", ");
            for (RangeVar rv : s.getFromClause()) {
                f.add(print(rv));
            }
            return "SelectStmt{targetList=[" + t + "], from=[" + f + "], where=" + print(s.getWhereClause()) + "}";
        }
        if (node instanceof ResTarget rt) {
            if (rt.isStar()) {
                return "ResTarget{*}";
            }
            return "ResTarget{" + print(rt.getExpr()) + (rt.getAlias() != null ? (" AS " + rt.getAlias()) : "") + "}";
        }
        if (node instanceof RangeVar rv) {
            return "RangeVar{" + rv.getTableName() + (rv.getAlias() != null ? (" " + rv.getAlias()) : "") + "}";
        }
        if (node instanceof ColumnRef cr) {
            return "ColumnRef{" + (cr.getTableName() != null ? cr.getTableName() + "." : "") + cr.getColumnName() + "}";
        }
        if (node instanceof AConst c) {
            return "AConst{" + c.getType() + ":" + c.getValue() + "}";
        }
        if (node instanceof AExpr e) {
            return "AExpr{" + e.getOp() + ", " + print(e.getLeft()) + ", " + print(e.getRight()) + "}";
        }
        if (node instanceof CreateTableStmt ct) {
            StringJoiner cols = new StringJoiner(", ");
            for (ColumnDef cd : ct.getColumns()) {
                cols.add(cd.getName() + " " + cd.getTypeName());
            }
            return "CreateTableStmt{" + ct.getTableName() + " (" + cols + ")}";
        }
        if (node instanceof InsertStmt ins) {
            StringJoiner vals = new StringJoiner(", ");
            for (AstNode v : ins.getValues()) {
                vals.add(print(v));
            }
            return "InsertStmt{table=" + ins.getTableName() + ", values=[" + vals + "]}";
        }
        if (node instanceof CreateIndexStmt ci) {
            return "CreateIndexStmt{name=" + ci.getIndexName() +
                    ", table=" + ci.getTableName() +
                    ", column=" + ci.getColumnName() +
                    ", type=" + ci.getIndexType() +
                    "}";
        }

        return node.getClass().getSimpleName() + "{}";
    }
}


