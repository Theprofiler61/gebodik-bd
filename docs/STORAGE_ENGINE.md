# Storage engine

This document describes data storage: heap pages, tuple identifiers (TID), the buffer pool, and the on-disk layout.

## Core concepts

### Heap table

A table is stored as a sequence of fixed-size heap pages. Each inserted row is placed into a free slot on a page.

### TID (Tuple Identifier)

TID is an address of a tuple within the heap storage:

- `pageId` — page number
- `slotId` — slot number within the page

In code: `src/main/java/ru/open/cu/student/index/TID.java`.

## Heap pages

File: `src/main/java/ru/open/cu/student/memory/page/HeapPage.java`

The page size is 8KB. The structure follows a classic slot-based layout:

```
┌────────────────────────────────────────────────────┐
│ Header                                              │
├────────────────────────────────────────────────────┤
│ Slot array (растёт вниз)                            │
├────────────────────────────────────────────────────┤
│ Free space                                          │
├────────────────────────────────────────────────────┤
│ Tuples (растут вверх)                               │
└────────────────────────────────────────────────────┘
```

This layout supports variable-length tuples and allows compacting/reclaiming space.

## Tuple serializer

File: `src/main/java/ru/open/cu/student/memory/serializer/HeapTupleSerializer.java`

Serialization/deserialization converts the logical tuple representation into bytes suitable for writing into heap pages.

## PageFileManager

File: `src/main/java/ru/open/cu/student/memory/manager/HeapPageFileManager.java`

Responsibilities:

- read/write fixed-size pages
- manage table data files

## Buffer pool

File: `src/main/java/ru/open/cu/student/memory/buffer/DefaultBufferPoolManager.java`

The buffer pool caches pages in memory to reduce disk I/O.

Key points:

- eviction strategy: LRU (`LRUReplacer`) or Clock (`ClockReplacer`)
- dirty pages are written on eviction and/or explicit flush
- thread-safety via locks at the manager level

## On-disk layout

The storage engine keeps its files under `data/` (created automatically):

```
data/
├── table_definitions.dat
├── column_definitions.dat
├── index_definitions.dat
├── <tableId>.dat
└── indexes/
    ├── <hashIndexName>.idx
    └── <btreeIndexName>.bpt   (см. ограничения по персистентности)
```

Catalog metadata is maintained by `CatalogManager` and `IndexManager`.

## Limitations (intentional)

- No WAL/redo logging and no production-grade durability guarantees.
- Concurrency is simplified; the server serializes queries.


