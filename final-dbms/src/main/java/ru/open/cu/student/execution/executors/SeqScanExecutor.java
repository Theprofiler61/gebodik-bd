package ru.open.cu.student.execution.executors;

import ru.open.cu.student.execution.Executor;
import ru.open.cu.student.storage.HeapTable;
import ru.open.cu.student.storage.Row;

import java.util.Iterator;
import java.util.Objects;

public class SeqScanExecutor implements Executor {
    private final HeapTable table;

    private boolean isOpen;
    private Iterator<Row> it;

    public SeqScanExecutor(HeapTable table) {
        this.table = Objects.requireNonNull(table, "table");
    }

    @Override
    public void open() {
        this.it = table.scanIterator();
        this.isOpen = true;
    }

    @Override
    public Object next() {
        if (!isOpen) {
            throw new IllegalStateException("Executor is not open");
        }
        if (it == null || !it.hasNext()) {
            return null;
        }
        return it.next();
    }

    @Override
    public void close() {
        this.isOpen = false;
        this.it = null;
    }
}


