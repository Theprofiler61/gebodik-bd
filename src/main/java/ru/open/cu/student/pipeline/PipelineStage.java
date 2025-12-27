package ru.open.cu.student.pipeline;

public enum PipelineStage {
    LEXER,
    PARSER,
    SEMANTIC,
    PLANNER,
    OPTIMIZER,
    EXECUTOR_FACTORY,
    EXECUTION,
    RESULT
}


