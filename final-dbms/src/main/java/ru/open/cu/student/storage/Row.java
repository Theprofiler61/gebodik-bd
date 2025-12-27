package ru.open.cu.student.storage;

import java.util.List;

public record Row(List<Object> values) {
    public Object get(int index) {
        return values.get(index);
    }
}


