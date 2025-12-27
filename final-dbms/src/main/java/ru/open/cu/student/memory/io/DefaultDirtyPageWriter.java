package ru.open.cu.student.memory.io;

import ru.open.cu.student.memory.buffer.BufferPoolManager;
import ru.open.cu.student.memory.model.BufferSlot;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DefaultDirtyPageWriter implements DirtyPageWriter {
    private final BufferPoolManager bufferPoolManager;
    private final long backgroundWriterIntervalMs;
    private final long checkPointIntervalMs;
    private final int batchSize;
    private ScheduledExecutorService backgroundWriterExecutor;
    private ScheduledExecutorService checkPointerExecutor;

    public DefaultDirtyPageWriter(BufferPoolManager bufferPoolManager, 
                                  long backgroundWriterIntervalMs, 
                                  long checkPointIntervalMs,
                                  int batchSize) {
        this.bufferPoolManager = bufferPoolManager;
        this.backgroundWriterIntervalMs = backgroundWriterIntervalMs;
        this.checkPointIntervalMs = checkPointIntervalMs;
        this.batchSize = batchSize;
    }

    @Override
    public void startBackgroundWriter() {
        if (backgroundWriterExecutor == null || backgroundWriterExecutor.isShutdown()) {
            backgroundWriterExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "background-writer");
                thread.setDaemon(true);
                return thread;
            });

            backgroundWriterExecutor.scheduleAtFixedRate(
                    this::flushDirtyPagesBatch,
                    backgroundWriterIntervalMs,
                    backgroundWriterIntervalMs,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    @Override
    public void startCheckPointer() {
        if (checkPointerExecutor == null || checkPointerExecutor.isShutdown()) {
            checkPointerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "check-pointer");
                thread.setDaemon(true);
                return thread;
            });

            checkPointerExecutor.scheduleAtFixedRate(
                    this::performCheckpoint,
                    checkPointIntervalMs,
                    checkPointIntervalMs,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    private void flushDirtyPagesBatch() {
        try {
            List<BufferSlot> dirtyPages = bufferPoolManager.getDirtyPages();
            int limit = Math.min(dirtyPages.size(), batchSize);

            for (int i = 0; i < limit; i++) {
                BufferSlot slot = dirtyPages.get(i);
                bufferPoolManager.flushPage(slot.getPageId());
            }
        } catch (Exception e) {
            System.err.println("Error during background flush: " + e.getMessage());
        }
    }

    private void performCheckpoint() {
        try {
            bufferPoolManager.flushAllPages();
        } catch (Exception e) {
            System.err.println("Error during checkpoint: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (backgroundWriterExecutor != null && !backgroundWriterExecutor.isShutdown()) {
            backgroundWriterExecutor.shutdown();
            try {
                if (!backgroundWriterExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    backgroundWriterExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                backgroundWriterExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (checkPointerExecutor != null && !checkPointerExecutor.isShutdown()) {
            checkPointerExecutor.shutdown();
            try {
                if (!checkPointerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    checkPointerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                checkPointerExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
