package ru.open.cu.student.execution.executors;

import ru.open.cu.student.execution.Executor;
import ru.open.cu.student.index.TID;
import ru.open.cu.student.index.btree.BPlusTreeIndex;
import ru.open.cu.student.storage.Table;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class BTreeIndexScanExecutor implements Executor {
    private final BPlusTreeIndex index;
    private final Comparable<?> rangeFrom;
    private final Comparable<?> rangeTo;
    private final boolean inclusive;
    private final Table table;

    private Iterator<TID> tidIterator;
    private boolean isOpen;

    public BTreeIndexScanExecutor(BPlusTreeIndex index, Comparable<?> value, Table table) {
        this(index, value, value, true, table);
    }

    public BTreeIndexScanExecutor(BPlusTreeIndex index, Comparable<?> from, Comparable<?> to, boolean inclusive, Table table) {
        this.index = Objects.requireNonNull(index, "index");
        this.rangeFrom = from;
        this.rangeTo = to;
        this.inclusive = inclusive;
        this.table = Objects.requireNonNull(table, "table");
    }

    @Override
    public void open() {
        List<TID> tids;
        if (rangeFrom != null && rangeTo != null && inclusive && rangeFrom.equals(rangeTo)) {
            tids = index.search(rangeFrom);
        } else {
            tids = index.rangeSearch(rangeFrom, rangeTo, inclusive);
        }
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


