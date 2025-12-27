package ru.open.cu.student.pipeline;

import java.util.List;

public record QueryResult(List<String> columns, List<List<Object>> rows) {
    public static QueryResult empty() {
        return new QueryResult(List.of(), List.of());
    }
}


