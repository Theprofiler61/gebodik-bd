package ru.open.cu.student.execution.executors;

import ru.open.cu.student.execution.Executor;
import ru.open.cu.student.index.TID;
import ru.open.cu.student.index.hash.HashIndex;
import ru.open.cu.student.storage.Table;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class HashIndexScanExecutor implements Executor {
    private final HashIndex index;
    private final Comparable<?> searchKey;
    private final Table table;

    private Iterator<TID> tidIterator;
    private boolean isOpen;

    public HashIndexScanExecutor(HashIndex index, Comparable<?> searchKey, Table table) {
        this.index = Objects.requireNonNull(index, "index");
        this.searchKey = Objects.requireNonNull(searchKey, "searchKey");
        this.table = Objects.requireNonNull(table, "table");
    }

    @Override
    public void open() {
        List<TID> tids = index.search(searchKey);
        this.tidIterator = (tids == null ? List.<TID>of() : tids).iterator();
        this.isOpen = true;
    }

    @Override
    public Object next() {
        if (!isOpen) {
            throw new IllegalStateException("Executor is not open");
        }
        if (tidIterator == null || !tidIterator.hasNext()) {
            return null;
        }
        return table.read(tidIterator.next());
    }

    @Override
    public void close() {
        this.isOpen = false;
        this.tidIterator = null;
    }
}


