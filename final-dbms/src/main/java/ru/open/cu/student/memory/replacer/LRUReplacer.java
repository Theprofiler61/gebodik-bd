package ru.open.cu.student.memory.replacer;

import ru.open.cu.student.memory.model.BufferSlot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LRUReplacer implements Replacer {
    private final Map<Integer, BufferSlot> lruMap;
    private final ReadWriteLock lock;

    public LRUReplacer() {
        this.lruMap = new LinkedHashMap<Integer, BufferSlot>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, BufferSlot> eldest) {
                return false;
            }
        };
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public void push(BufferSlot bufferSlot) {
        lock.writeLock().lock();
        try {
            if (!bufferSlot.isPinned()) {
                lruMap.put(bufferSlot.getPageId(), bufferSlot);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void delete(int pageId) {
        lock.writeLock().lock();
        try {
            lruMap.remove(pageId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public BufferSlot pickVictim() {
        lock.writeLock().lock();
        try {
            if (lruMap.isEmpty()) {
                return null;
            }

            Map.Entry<Integer, BufferSlot> eldest = lruMap.entrySet().iterator().next();
            BufferSlot victim = eldest.getValue();
            lruMap.remove(eldest.getKey());
            return victim;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
