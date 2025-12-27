package ru.open.cu.student.pipeline;

public interface PipelineLogger {
    void log(PipelineStage stage, String message);

    default void logObject(PipelineStage stage, Object obj) {
        log(stage, String.valueOf(obj));
    }
}


