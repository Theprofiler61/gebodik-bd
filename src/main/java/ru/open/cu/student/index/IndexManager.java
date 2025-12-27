package ru.open.cu.student.index;

import java.util.Optional;

public interface IndexManager {
    Optional<IndexDescriptor> findIndex(String tableName, String columnName, IndexType type);

    Index getIndex(String indexName);

    void createIndex(IndexDescriptor descriptor);

    void onInsert(String tableName, java.util.List<Object> values, TID tid);
}


