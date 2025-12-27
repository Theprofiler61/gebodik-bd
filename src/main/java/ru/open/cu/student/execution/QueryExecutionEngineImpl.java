package ru.open.cu.student.execution;

import java.util.ArrayList;
import java.util.List;

public class QueryExecutionEngineImpl implements QueryExecutionEngine {
    @Override
    public List<Object> execute(Executor executor) {
        if (executor == null) throw new IllegalArgumentException("executor is null");

        List<Object> results = new ArrayList<>();
        try {
            executor.open();
            Object row;
            while ((row = executor.next()) != null) {
                results.add(row);
            }
        } finally {
            executor.close();
        }
        return results;
    }
}


