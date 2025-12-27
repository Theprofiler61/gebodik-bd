package ru.open.cu.student.execution.executors;

import ru.open.cu.student.execution.Executor;
import ru.open.cu.student.execution.expr.SemExprEvaluator;
import ru.open.cu.student.semantic.expr.SemExpr;
import ru.open.cu.student.storage.Row;

import java.util.Objects;

public class FilterExecutor implements Executor {
    private final SemExpr predicate;
    private final Executor child;

    private boolean isOpen;

    public FilterExecutor(SemExpr predicate, Executor child) {
        this.predicate = Objects.requireNonNull(predicate, "predicate");
        this.child = Objects.requireNonNull(child, "child");
    }

    @Override
    public void open() {
        child.open();
        isOpen = true;
    }

    @Override
    public Object next() {
        if (!isOpen) {
            throw new IllegalStateException("Executor is not open");
        }
        while (true) {
            Object rowObj = child.next();
            if (rowObj == null) {
                return null;
            }
            Row row = (Row) rowObj;
            if (SemExprEvaluator.evalPredicate(predicate, row)) {
                return row;
            }
        }
    }

    @Override
    public void close() {
        try {
            child.close();
        } finally {
            isOpen = false;
        }
    }
}


