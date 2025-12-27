package ru.open.cu.student.pipeline;

public class PipelineException extends RuntimeException {
    private final PipelineStage stage;

    public PipelineException(PipelineStage stage, String message, Throwable cause) {
        super(formatMessage(stage, message, cause), cause);
        this.stage = stage;
    }

    public PipelineException(PipelineStage stage, Throwable cause) {
        this(stage, null, cause);
    }

    public PipelineStage stage() {
        return stage;
    }

    private static String formatMessage(PipelineStage stage, String message, Throwable cause) {
        String details = message;
        if ((details == null || details.isBlank()) && cause != null) {
            details = cause.getMessage();
        }
        if (details == null || details.isBlank()) {
            details = "Неизвестная ошибка";
        }

        return "Ошибка на этапе " + stageTitleRu(stage) + " (" + stage + "): " + details;
    }

    private static String stageTitleRu(PipelineStage stage) {
        if (stage == null) return "пайплайна";
        return switch (stage) {
            case LEXER -> "лексического анализа";
            case PARSER -> "синтаксического анализа";
            case SEMANTIC -> "семантического анализа";
            case PLANNER -> "планирования запроса";
            case OPTIMIZER -> "оптимизации плана";
            case EXECUTOR_FACTORY -> "подбора исполнителя";
            case EXECUTION -> "выполнения запроса";
            case RESULT -> "формирования результата";
        };
    }
}


