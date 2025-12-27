package ru.open.cu.student.server;

import ru.open.cu.student.pipeline.SqlPipeline;
import ru.open.cu.student.storage.engine.StorageEngine;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class DbServer implements AutoCloseable {
    private final StorageEngine storage;
    private final SqlPipeline pipeline;
    private final int port;
    private final ExecutorService sessionsPool;
    private final AtomicLong sessionSeq = new AtomicLong(0);

    private volatile boolean running;
    private ServerSocket serverSocket;
    private volatile int boundPort;

    public DbServer(StorageEngine storage, SqlPipeline pipeline, int port) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
        this.port = port;
        this.sessionsPool = Executors.newCachedThreadPool();
    }

    public void startBlocking() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        boundPort = serverSocket.getLocalPort();
        System.out.println("DB server listening on port " + boundPort);

        try {
            while (running) {
                Socket socket = serverSocket.accept();
                long sessionId = sessionSeq.incrementAndGet();
                sessionsPool.submit(new DbSession(sessionId, socket, storage, pipeline));
            }
        } catch (IOException e) {
            if (running) {
                throw e;
            }
        }
    }

    public int boundPort() {
        return boundPort;
    }

    @Override
    public void close() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Exception ignored) {
        }
        sessionsPool.shutdownNow();
        storage.close();
    }
}


