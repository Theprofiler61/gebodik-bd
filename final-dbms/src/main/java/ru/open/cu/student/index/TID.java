package ru.open.cu.student.index;

public record TID(int pageId, short slotId) {
    public TID(int pageId, int slotId) {
        this(pageId, (short) slotId);
    }
}


