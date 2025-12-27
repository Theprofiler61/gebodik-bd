package ru.open.cu.student.execution.executors;

import ru.open.cu.student.execution.Executor;
import ru.open.cu.student.execution.expr.SemExprEvaluator;
import ru.open.cu.student.semantic.QueryTree;
import ru.open.cu.student.storage.Row;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProjectExecutor implements Executor {
    private final List<QueryTree.SelectItem> targets;
    private final Executor child;

    private boolean isOpen;

    public ProjectExecutor(List<QueryTree.SelectItem> targets, Executor child) {
        this.targets = Objects.requireNonNull(targets, "targets");
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
        Object rowObj = child.next();
        if (rowObj == null) {
            return null;
        }
        Row row = (Row) rowObj;

        List<Object> out = new ArrayList<>(targets.size());
        for (QueryTree.SelectItem t : targets) {
            out.add(SemExprEvaluator.evalValue(t.expr(), row));
        }
        return new Row(out);
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


