package ru.open.cu.student.server;

import ru.open.cu.student.execution.ExecutorFactoryImpl;
import ru.open.cu.student.execution.QueryExecutionEngineImpl;
import ru.open.cu.student.lexer.LexerImpl;
import ru.open.cu.student.optimizer.OptimizerImpl;
import ru.open.cu.student.parser.ParserImpl;
import ru.open.cu.student.pipeline.SqlPipeline;
import ru.open.cu.student.pipeline.StdoutPipelineLogger;
import ru.open.cu.student.planner.PlannerImpl;
import ru.open.cu.student.semantic.SemanticAnalyzerImpl;
import ru.open.cu.student.storage.engine.StorageEngine;

import java.nio.file.Path;

public class DbServerMain {
    public static void main(String[] args) throws Exception {
        int port = intArg(args, "--port", 15432);
        int bufferPool = intArg(args, "--bufferPool", 10);
        Path dataDir = Path.of(stringArg(args, "--dataDir", "data"));

        StorageEngine storage = new StorageEngine(dataDir, bufferPool);

        SqlPipeline pipeline = new SqlPipeline(
                new LexerImpl(),
                new ParserImpl(),
                new SemanticAnalyzerImpl(),
                new PlannerImpl(),
                new OptimizerImpl(storage.indexes()),
                new ExecutorFactoryImpl(storage, storage.indexes()),
                new QueryExecutionEngineImpl(),
                new StdoutPipelineLogger("server ")
        );

        DbServer server = new DbServer(storage, pipeline, port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        server.startBlocking();
    }

    private static int intArg(String[] args, String key, int def) {
        String v = findArg(args, key);
        return v == null ? def : Integer.parseInt(v);
    }

    private static String stringArg(String[] args, String key, String def) {
        String v = findArg(args, key);
        return v == null ? def : v;
    }

    private static String findArg(String[] args, String key) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(key) && i + 1 < args.length) {
                return args[i + 1];
            }
        }
        return null;
    }
}


