# Roadmap

This document is an extended TODO list. It is useful if you want to:

- continue development
- pick a focused topic (optimization, indexes, transactions)
- use this repository as a reference for your own educational project

## Indexes and optimization

- [ ] BTREE persistence: on-disk format, recovery without rebuild, restart tests.
- [ ] Expand plan selection: more rules for predicates and compound conditions.
- [ ] Cost-based optimizer: statistics, selectivity estimation, operator choice.
- [ ] Multi-column (composite) indexes.

## SQL dialect

- [ ] `UPDATE`
- [ ] `DELETE`
- [ ] `DROP TABLE`
- [ ] `DROP INDEX`
- [ ] `ORDER BY`
- [ ] `LIMIT`
- [ ] `JOIN` (start with nested loop join)

## Transactions and concurrency

- [ ] Remove global query serialization (server-level lock).
- [ ] Table/page/row-level locking.
- [ ] MVCC or a simplified isolation model (e.g., Read Committed).
- [ ] Transaction log (WAL) for durability.

## Storage engine

- [ ] Stronger flush/close guarantees, educational crash-recovery tests.
- [ ] Better free-space management and page compaction.
- [ ] Table statistics: row counts and value distributions.

## Tooling and DX

- [ ] Load test scenarios (benchmarks).
- [ ] Better error messages (SQL positions, context).
- [ ] CI pipeline (build + tests).


