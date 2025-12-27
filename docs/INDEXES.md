# Indexes

The project implements two index types:

- `HASH` — for point lookups (`=`)
- `BTREE` — a B+Tree for point and range queries

The main goal of this module is to demonstrate:

- how an index is built and stored
- how the optimizer chooses `IndexScan`
- how executors fetch rows using `TID`

## Hash index

Implementation: `src/main/java/ru/open/cu/student/index/hash/HashIndexImpl.java`

Properties:

- fast point lookup
- designed for predicates like `WHERE col = const`
- persistence is implemented inside the index (it maintains its own file)

## B+Tree (BTREE)

Implementation: `src/main/java/ru/open/cu/student/index/btree/BPlusTreeIndexImpl.java`

Properties:

- supports point lookups and range scans (`<`, `<=`, `>`, `>=`)
- used by the optimizer for range predicates
- the current version has intentional educational limitations around persistence (see below)

### BTREE persistence limitation

In `DefaultIndexManager`, BTREE indexes are **rebuilt** by scanning the table on startup:

- this simplifies recovery
- but makes startup more expensive and does not demonstrate a fully persistent index

Planned work: `ROADMAP.md` (storage/indexes section).

## How IndexScan is selected

The current strategy is rule-based (see `optimizer/strategy/RuleBasedScanStrategy.java`):

- For `col = const`:
  - if a `HASH` index exists, it is selected
  - otherwise, if a `BTREE` index exists, it is selected
- For comparisons `col < const`, `col <= const`, `col > const`, `col >= const`:
  - if a `BTREE` index exists, a range scan is selected
  - otherwise, `SeqScan` is used

## How IndexScan is executed

Physical node: `optimizer/node/PhysicalIndexScanNode.java`

Executors:

- `HashIndexScanExecutor`
- `BTreeIndexScanExecutor`

Both produce a stream of `TID`s and then fetch tuples from the heap table by address.


