package ru.open.cu.student.memory.replacer;

import ru.open.cu.student.memory.model.BufferSlot;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClockReplacer implements Replacer {
    private final List<BufferSlot> clockList;
    private final Map<Integer, Integer> pageIdToIndex;
    private int clockHand;
    private final ReadWriteLock lock;

    public ClockReplacer() {
        this.clockList = new ArrayList<>();
        this.pageIdToIndex = new HashMap<>();
        this.clockHand = 0;
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public void push(BufferSlot bufferSlot) {
        lock.writeLock().lock();
        try {
            if (bufferSlot.isPinned()) {
                return;
            }

            Integer index = pageIdToIndex.get(bufferSlot.getPageId());
            if (index != null && index < clockList.size()) {
                clockList.set(index, bufferSlot);
            } else {
                clockList.add(bufferSlot);
                pageIdToIndex.put(bufferSlot.getPageId(), clockList.size() - 1);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void delete(int pageId) {
        lock.writeLock().lock();
        try {
            Integer index = pageIdToIndex.remove(pageId);
            if (index != null && index < clockList.size()) {
                clockList.remove(index.intValue());
                rebuildIndex();
                if (clockHand >= clockList.size() && !clockList.isEmpty()) {
                    clockHand = 0;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public BufferSlot pickVictim() {
        lock.writeLock().lock();
        try {
            if (clockList.isEmpty()) {
                return null;
            }

            int attempts = 0;
            int maxAttempts = clockList.size() * 2;

            while (attempts < maxAttempts) {
                if (clockHand >= clockList.size()) {
                    clockHand = 0;
                }

                BufferSlot slot = clockList.get(clockHand);

                if (slot.getUsageCount() == 0) {
                    BufferSlot victim = clockList.remove(clockHand);
                    pageIdToIndex.remove(victim.getPageId());
                    rebuildIndex();
                    if (clockHand >= clockList.size() && !clockList.isEmpty()) {
                        clockHand = 0;
                    }
                    return victim;
                } else {
                    slot.incrementUsage();
                    clockHand++;
                }

                attempts++;
            }

            if (!clockList.isEmpty()) {
                BufferSlot victim = clockList.remove(0);
                pageIdToIndex.remove(victim.getPageId());
                rebuildIndex();
                clockHand = 0;
                return victim;
            }

            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void rebuildIndex() {
        pageIdToIndex.clear();
        for (int i = 0; i < clockList.size(); i++) {
            pageIdToIndex.put(clockList.get(i).getPageId(), i);
        }
    }
}
