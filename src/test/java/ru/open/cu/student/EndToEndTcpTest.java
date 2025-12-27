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

class EndToEndTcpTest {

    @Test
    void tcp_pipeline_works_and_persists_between_restarts(@TempDir Path tempDir) throws Exception {
        List<String> logs = Collections.synchronizedList(new ArrayList<>());
        PipelineLogger logger = (stage, message) -> logs.add(stage + ":" + message);

        try (DbServerHandle h = startServer(tempDir, logger)) {
            assertTrue(query(h, "CREATE TABLE users (id INT64, name VARCHAR);").contains("\"ok\":true"));
            assertTrue(query(h, "INSERT INTO users VALUES (1, 'Ann');").contains("\"ok\":true"));
            assertTrue(query(h, "INSERT INTO users VALUES (2, 'Bob');").contains("\"ok\":true"));

            logs.clear();
            String beforeIdx = query(h, "SELECT name FROM users WHERE id = 1;");
            assertEquals("{\"ok\":true,\"columns\":[\"name\"],\"rows\":[[\"Ann\"]]}", beforeIdx);
            assertTrue(logs.stream().anyMatch(s -> s.contains("OPTIMIZER:") && s.contains("PhysicalSeqScanNode")));

            String mul = query(h, "SELECT 2*2 AS v FROM users;");
            assertEquals("{\"ok\":true,\"columns\":[\"v\"],\"rows\":[[4],[4]]}", mul);

            assertTrue(query(h, "CREATE INDEX idx_users_id ON users(id) USING HASH;").contains("\"ok\":true"));

            logs.clear();
            String afterIdx = query(h, "SELECT name FROM users WHERE id = 1;");
            assertEquals("{\"ok\":true,\"columns\":[\"name\"],\"rows\":[[\"Ann\"]]}", afterIdx);
            assertTrue(logs.stream().anyMatch(s -> s.contains("OPTIMIZER:") && s.contains("PhysicalIndexScanNode")));

            // Index stays up-to-date for inserts done AFTER index creation.
            assertTrue(query(h, "INSERT INTO users VALUES (3, 'Cat');").contains("\"ok\":true"));
            logs.clear();
            String afterInsert = query(h, "SELECT name FROM users WHERE id = 3;");
            assertEquals("{\"ok\":true,\"columns\":[\"name\"],\"rows\":[[\"Cat\"]]}", afterInsert);
            assertTrue(logs.stream().anyMatch(s -> s.contains("OPTIMIZER:") && s.contains("PhysicalIndexScanNode")));
        }

        logs.clear();
        try (DbServerHandle h2 = startServer(tempDir, logger)) {
            String resp = query(h2, "SELECT name FROM users WHERE id = 1;");
            assertEquals("{\"ok\":true,\"columns\":[\"name\"],\"rows\":[[\"Ann\"]]}", resp);
            assertTrue(logs.stream().anyMatch(s -> s.contains("OPTIMIZER:") && s.contains("PhysicalIndexScanNode")));

            logs.clear();
            String resp2 = query(h2, "SELECT name FROM users WHERE id = 3;");
            assertEquals("{\"ok\":true,\"columns\":[\"name\"],\"rows\":[[\"Cat\"]]}", resp2);
            assertTrue(logs.stream().anyMatch(s -> s.contains("OPTIMIZER:") && s.contains("PhysicalIndexScanNode")));
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
        }, "db-server-test");
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


