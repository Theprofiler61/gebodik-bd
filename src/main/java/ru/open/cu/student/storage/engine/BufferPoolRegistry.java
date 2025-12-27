package ru.open.cu.student.storage.engine;

import ru.open.cu.student.memory.buffer.BufferPoolManager;
import ru.open.cu.student.memory.buffer.DefaultBufferPoolManager;
import ru.open.cu.student.memory.manager.PageFileManager;
import ru.open.cu.student.memory.replacer.LRUReplacer;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class BufferPoolRegistry {
    private final int poolSizePerFile;
    private final PageFileManager pageFileManager;
    private final Map<Path, BufferPoolManager> pools = new ConcurrentHashMap<>();

    public BufferPoolRegistry(int poolSizePerFile, PageFileManager pageFileManager) {
        if (poolSizePerFile <= 0) throw new IllegalArgumentException("poolSizePerFile must be > 0");
        this.poolSizePerFile = poolSizePerFile;
        this.pageFileManager = Objects.requireNonNull(pageFileManager, "pageFileManager");
    }

    public BufferPoolManager get(Path filePath) {
        Objects.requireNonNull(filePath, "filePath");
        Path key = filePath.toAbsolutePath().normalize();
        return pools.computeIfAbsent(key, p ->
                new DefaultBufferPoolManager(poolSizePerFile, pageFileManager, new LRUReplacer(), p)
        );
    }

    public void flushAll() {
        for (BufferPoolManager bpm : pools.values()) {
            bpm.flushAllPages();
        }
    }
}


