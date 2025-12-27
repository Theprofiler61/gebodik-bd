package ru.open.cu.student;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.open.cu.student.execution.ExecutorFactoryImpl;
import ru.open.cu.student.execution.QueryExecutionEngineImpl;
import ru.open.cu.student.lexer.LexerImpl;
import ru.open.cu.student.optimizer.OptimizerImpl;
import ru.open.cu.student.parser.ParserImpl;
import ru.open.cu.student.pipeline.PipelineLogger;
import ru.open.cu.student.pipeline.SqlPipeline;
import ru.open.cu.student.planner.PlannerImpl;
import ru.open.cu.student.protocol.FramedProtocol;
import ru.open.cu.student.semantic.SemanticAnalyzerImpl;
import ru.open.cu.student.server.DbServer;
import ru.open.cu.student.storage.engine.StorageEngine;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IndexingBTreeEndToEndTest {

    @Test
    void btree_index_is_used_for_range_predicates(@TempDir Path tempDir) throws Exception {
        List<String> logs = Collections.synchronizedList(new ArrayList<>());
        PipelineLogger logger = (stage, message) -> logs.add(stage + ":" + message);

        try (DbServerHandle h = startServer(tempDir, logger)) {
            assertTrue(query(h, "CREATE TABLE users (id INT64, name VARCHAR);").contains("\"ok\":true"));
            for (int i = 0; i < 200; i++) {
                assertTrue(query(h, "INSERT INTO users VALUES (" + i + ", 'u" + i + "');").contains("\"ok\":true"));
            }

            assertTrue(query(h, "CREATE INDEX idx_users_id_btree ON users(id) USING BTREE;").contains("\"ok\":true"));

            logs.clear();
            String resp = query(h, "SELECT name FROM users WHERE id >= 199;");
            assertTrue(resp.contains("\"ok\":true"));
            assertTrue(logs.stream().anyMatch(s -> s.contains("OPTIMIZER:") && s.contains("PhysicalIndexScanNode")),
                    "Expected optimizer to choose index scan, logs:\n" + String.join("\n", logs));

            // Index stays up-to-date for inserts done AFTER index creation.
            assertTrue(query(h, "INSERT INTO users VALUES (1000, 'later');").contains("\"ok\":true"));
            logs.clear();
            String resp2 = query(h, "SELECT name FROM users WHERE id >= 1000;");
            assertTrue(resp2.contains("\"ok\":true"));
            assertTrue(logs.stream().anyMatch(s -> s.contains("OPTIMIZER:") && s.contains("PhysicalIndexScanNode")),
                    "Expected optimizer to choose index scan for post-index insert, logs:\n" + String.join("\n", logs));
        }

        // Restart: BTREE index is rebuilt from the table and used again.
        logs.clear();
        try (DbServerHandle h2 = startServer(tempDir, logger)) {
            String resp3 = query(h2, "SELECT name FROM users WHERE id >= 1000;");
            assertTrue(resp3.contains("\"ok\":true"));
            assertTrue(logs.stream().anyMatch(s -> s.contains("OPTIMIZER:") && s.contains("PhysicalIndexScanNode")),
                    "Expected optimizer to choose index scan after restart, logs:\n" + String.join("\n", logs));
        }
    }

    private static DbServerHandle startServer(Path dataDir, PipelineLogger logger) throws Exception {
        StorageEngine storage = new StorageEngine(dataDir, 10);
        SqlPipeline pipeline = new SqlPipeline(
                new LexerImpl(),
                new ParserImpl(),
                new SemanticAnalyzerImpl(),
                new PlannerImpl(),
                new OptimizerImpl(storage.indexes()),
                new ExecutorFactoryImpl(storage, storage.indexes()),
                new QueryExecutionEngineImpl(),
                logger
        );

        DbServer server = new DbServer(storage, pipeline, 0);
        Thread t = new Thread(() -> {
            try {
                server.startBlocking();
            } catch (Exception ignored) {
            }
        }, "db-server-test-btree");
        t.setDaemon(true);
        t.start();

        for (int i = 0; i < 200; i++) {
            int p = server.boundPort();
            if (p > 0) {
                return new DbServerHandle(server, t, p);
            }
            Thread.sleep(10);
        }
        server.close();
        throw new IllegalStateException("Server didn't start");
    }

    private static String query(DbServerHandle h, String sql) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", h.port);
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            FramedProtocol.writeFrame(out, sql);
            String resp = FramedProtocol.readFrame(in);
            assertNotNull(resp);
            return resp;
        }
    }

    private static final class DbServerHandle implements AutoCloseable {
        final DbServer server;
        final Thread thread;
        final int port;

        DbServerHandle(DbServer server, Thread thread, int port) {
            this.server = server;
            this.thread = thread;
            this.port = port;
        }

        @Override
        public void close() {
            server.close();
            try {
                thread.join(500);
            } catch (InterruptedException ignored) {
            }
        }
    }
}


