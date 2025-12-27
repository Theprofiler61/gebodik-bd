package ru.open.cu.student.pipeline;

import java.time.Instant;

public class StdoutPipelineLogger implements PipelineLogger {
    private final String prefix;

    public StdoutPipelineLogger(String prefix) {
        this.prefix = prefix == null ? "" : prefix;
    }

    @Override
    public void log(PipelineStage stage, String message) {
        System.out.println(Instant.now() + " " + prefix + "[" + stage + "] " + message);
    }
}


