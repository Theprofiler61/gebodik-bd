package ru.open.cu.student.execution;

public interface Executor {
    void open();

    Object next();

    void close();
}


