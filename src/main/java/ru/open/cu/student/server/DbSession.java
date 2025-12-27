package ru.open.cu.student.server;

import ru.open.cu.student.pipeline.PipelineException;
import ru.open.cu.student.pipeline.QueryResult;
import ru.open.cu.student.pipeline.SqlPipeline;
import ru.open.cu.student.protocol.DbResponse;
import ru.open.cu.student.protocol.FramedProtocol;
import ru.open.cu.student.storage.engine.StorageEngine;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DbSession implements Runnable {
    private static final Lock GLOBAL_EXECUTION_LOCK = new ReentrantLock(true);

    private final long sessionId;
    private final Socket socket;
    private final StorageEngine storage;
    private final SqlPipeline pipeline;

    public DbSession(long sessionId, Socket socket, StorageEngine storage, SqlPipeline pipeline) {
        this.sessionId = sessionId;
        this.socket = Objects.requireNonNull(socket, "socket");
        this.storage = Objects.requireNonNull(storage, "storage");
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
    }

    @Override
    public void run() {
        try (socket;
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            socket.setTcpNoDelay(true);
            System.out.println("Session #" + sessionId + " connected from " + socket.getRemoteSocketAddress());

            while (true) {
                String sql = FramedProtocol.readFrame(in);
                if (sql == null) {
                    break;
                }

                DbResponse response;
                try {
                    GLOBAL_EXECUTION_LOCK.lock();
                    try {
                        QueryResult result = pipeline.execute(sql, storage.catalog());
                        response = DbResponse.ok(result.columns(), result.rows());
                    } finally {
                        GLOBAL_EXECUTION_LOCK.unlock();
                    }
                } catch (PipelineException pe) {
                    response = DbResponse.error(pe.stage().name(), pe.getMessage());
                } catch (Exception e) {
                    response = DbResponse.error("SERVER", e.getMessage());
                }

                FramedProtocol.writeFrame(out, response.toJson());
            }
        } catch (Exception e) {
            System.err.println("Session #" + sessionId + " aborted: " + e.getMessage());
        } finally {
            System.out.println("Session #" + sessionId + " closed");
        }
    }
}


