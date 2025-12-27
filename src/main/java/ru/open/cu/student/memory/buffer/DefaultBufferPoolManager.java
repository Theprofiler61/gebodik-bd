package ru.open.cu.student.memory.buffer;

import ru.open.cu.student.memory.manager.PageFileManager;
import ru.open.cu.student.memory.model.BufferSlot;
import ru.open.cu.student.memory.page.Page;
import ru.open.cu.student.memory.replacer.Replacer;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DefaultBufferPoolManager implements BufferPoolManager {
    private final int poolSize;
    private final PageFileManager pageFileManager;
    private final Replacer replacer;
    private final Path filePath;
    private final Map<Integer, BufferSlot> bufferPool;
    private final ReadWriteLock lock;

    public DefaultBufferPoolManager(int poolSize, PageFileManager pageFileManager, Replacer replacer, Path filePath) {
        this.poolSize = poolSize;
        this.pageFileManager = pageFileManager;
        this.replacer = replacer;
        this.filePath = filePath;
        this.bufferPool = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    public DefaultBufferPoolManager(int poolSize, PageFileManager pageFileManager, Replacer replacer) {
        this(poolSize, pageFileManager, replacer, Path.of("data.bin"));
    }

    @Override
    public BufferSlot getPage(int pageId) {
        lock.writeLock().lock();
        try {
            BufferSlot slot = bufferPool.get(pageId);
            if (slot != null) {
                slot.incrementUsage();
                return slot;
            }

            if (bufferPool.size() >= poolSize) {
                evictPage();
            }

            Page page = pageFileManager.read(pageId, filePath);
            slot = new BufferSlot(pageId, page);
            bufferPool.put(pageId, slot);
            slot.incrementUsage();
            return slot;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void updatePage(int pageId, Page page) {
        lock.writeLock().lock();
        try {
            BufferSlot slot = bufferPool.get(pageId);
            if (slot == null) {
                throw new IllegalArgumentException("Page " + pageId + " not found in buffer pool");
            }
            slot.setPage(page);
            slot.setDirty(true);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void pinPage(int pageId) {
        lock.writeLock().lock();
        try {
            BufferSlot slot = bufferPool.get(pageId);
            if (slot == null) {
                throw new IllegalArgumentException("Page " + pageId + " not found in buffer pool");
            }
            slot.setPinned(true);
            replacer.delete(pageId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void flushPage(int pageId) {
        lock.writeLock().lock();
        try {
            BufferSlot slot = bufferPool.get(pageId);
            if (slot == null) {
                return;
            }
            if (slot.isDirty()) {
                pageFileManager.write(slot.getPage(), filePath);
                slot.setDirty(false);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void flushAllPages() {
        lock.writeLock().lock();
        try {
            for (BufferSlot slot : bufferPool.values()) {
                if (slot.isDirty()) {
                    pageFileManager.write(slot.getPage(), filePath);
                    slot.setDirty(false);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<BufferSlot> getDirtyPages() {
        lock.readLock().lock();
        try {
            return bufferPool.values().stream()
                    .filter(BufferSlot::isDirty)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    private void evictPage() {
        BufferSlot victim = replacer.pickVictim();
        if (victim == null) {
            throw new IllegalStateException("No victim available for eviction");
        }

        if (victim.isDirty()) {
            pageFileManager.write(victim.getPage(), filePath);
        }

        bufferPool.remove(victim.getPageId());
    }

    public void unpinPage(int pageId) {
        lock.writeLock().lock();
        try {
            BufferSlot slot = bufferPool.get(pageId);
            if (slot == null) {
                throw new IllegalArgumentException("Page " + pageId + " not found in buffer pool");
            }
            slot.setPinned(false);
            replacer.push(slot);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
