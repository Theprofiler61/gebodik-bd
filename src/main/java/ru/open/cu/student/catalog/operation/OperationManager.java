package ru.open.cu.student.catalog.operation;

import ru.open.cu.student.index.TID;
import ru.open.cu.student.storage.Row;

import java.util.List;

public interface OperationManager {

    TID insert(String tableName, List<Object> values);

    Row read(String tableName, TID tid);

}
