package ru.open.cu.student.storage;

import ru.open.cu.student.index.TID;

public interface Table {
    Object read(TID tid);
}


