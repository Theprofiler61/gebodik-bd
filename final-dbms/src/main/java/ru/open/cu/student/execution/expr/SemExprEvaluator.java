package ru.open.cu.student.execution.expr;

import ru.open.cu.student.semantic.expr.SemBinaryExpr;
import ru.open.cu.student.semantic.expr.SemColumnRef;
import ru.open.cu.student.semantic.expr.SemConst;
import ru.open.cu.student.semantic.expr.SemExpr;
import ru.open.cu.student.storage.Row;

public final class SemExprEvaluator {
    private SemExprEvaluator() {
    }

    public static boolean evalPredicate(SemExpr expr, Row row) {
        Object v = evalValue(expr, row);
        if (v instanceof Boolean b) {
            return b;
        }
        throw new IllegalArgumentException("WHERE expression is not boolean: " + v);
    }

    public static Object evalValue(SemExpr expr, Row row) {
        if (expr instanceof SemConst c) {
            return c.value();
        }
        if (expr instanceof SemColumnRef c) {
            return row.get(c.column().getPosition());
        }
        if (expr instanceof SemBinaryExpr b) {
            return switch (b.op()) {
                case AND -> (Boolean) evalValue(b.left(), row) && (Boolean) evalValue(b.right(), row);
                case OR -> (Boolean) evalValue(b.left(), row) || (Boolean) evalValue(b.right(), row);
                case EQ -> compare(evalValue(b.left(), row), evalValue(b.right(), row)) == 0;
                case NEQ -> compare(evalValue(b.left(), row), evalValue(b.right(), row)) != 0;
                case GT -> compare(evalValue(b.left(), row), evalValue(b.right(), row)) > 0;
                case GTE -> compare(evalValue(b.left(), row), evalValue(b.right(), row)) >= 0;
                case LT -> compare(evalValue(b.left(), row), evalValue(b.right(), row)) < 0;
                case LTE -> compare(evalValue(b.left(), row), evalValue(b.right(), row)) <= 0;
                case ADD -> add(evalValue(b.left(), row), evalValue(b.right(), row));
                case SUB -> sub(evalValue(b.left(), row), evalValue(b.right(), row));
                case MUL -> mul(evalValue(b.left(), row), evalValue(b.right(), row));
                case DIV -> div(evalValue(b.left(), row), evalValue(b.right(), row));
            };
        }
        throw new IllegalArgumentException("Unsupported expr: " + expr);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int compare(Object l, Object r) {
        if (l == null && r == null) return 0;
        if (l == null) return -1;
        if (r == null) return 1;
        if (l instanceof Comparable c) {
            return c.compareTo(r);
        }
        throw new IllegalArgumentException("Not comparable: " + l.getClass());
    }

    private static Object add(Object l, Object r) {
        Long a = asLongOrNull(l);
        Long b = asLongOrNull(r);
        if (a == null || b == null) return null;
        return a + b;
    }

    private static Object sub(Object l, Object r) {
        Long a = asLongOrNull(l);
        Long b = asLongOrNull(r);
        if (a == null || b == null) return null;
        return a - b;
    }

    private static Object mul(Object l, Object r) {
        Long a = asLongOrNull(l);
        Long b = asLongOrNull(r);
        if (a == null || b == null) return null;
        return a * b;
    }

    private static Object div(Object l, Object r) {
        Long a = asLongOrNull(l);
        Long b = asLongOrNull(r);
        if (a == null || b == null) return null;
        if (b == 0L) {
            throw new IllegalArgumentException("Division by zero");
        }
        return a / b;
    }

    private static Long asLongOrNull(Object v) {
        if (v == null) return null;
        if (v instanceof Long l) return l;
        throw new IllegalArgumentException("Expected INT64, got " + v.getClass().getSimpleName());
    }
}


