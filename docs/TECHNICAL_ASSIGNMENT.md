# Technical assignment (educational “mini DBMS”)

This document is a sample technical assignment / checklist for building an educational DBMS “from scratch”.
It can be used as a reference for your own project.

The style is intentionally engineering-oriented: requirements + acceptance criteria.

## 1. Goal and scope

### 1.1. Goal

Implement an educational relational DBMS that demonstrates the full query path:

1) SQL input (client/server),
2) parsing and analysis,
3) plan construction,
4) execution,
5) page-level disk reads/writes,
6) index usage to accelerate lookups.

### 1.2. Scope

The project may intentionally simplify production concerns:

- no full transaction model,
- no advanced optimization or full SQL standard coverage,
- focus on readability and testability.

## 2. Functional requirements

### 2.1. Client–server

- The server accepts TCP connections.
- Exchange protocol: framed (length-prefixed).
- Request: SQL string (UTF-8).
- Response: JSON (success/error).

Acceptance criteria:

- The server starts, accepts connections, and closes resources correctly.
- The client can send a query and receive a response.
- Errors are returned in a unified format with a pipeline stage (lexer/parser/semantic/...).

### 2.2. CLI client

- Interactive mode.
- Quit command.
- Tabular rendering of results.
- Optional raw JSON mode.

### 2.3. SQL dialect (minimal)

Required statements:

- `CREATE TABLE`
- `INSERT INTO … VALUES …`
- `SELECT … FROM … [WHERE …]`
- `CREATE INDEX … USING …`

Supported types:

- `INT64`
- `VARCHAR`

Supported expressions:

- arithmetic (`+`, `-`, `*`, `/`) for numeric types
- comparisons (`=`, `!=`, `<`, `>`, `<=`, `>=`)
- boolean logic `AND`/`OR`

Acceptance criteria:

- the parser accepts valid queries and rejects invalid ones
- semantic analysis validates tables/columns/types
- `SELECT` returns correct results, `INSERT` persists data

### 2.4. SQL pipeline and plans

Stage requirements:

- Lexer: tokenization.
- Parser: AST.
- Semantic: typing and name validation.
- Planner: logical plan.
- Optimizer: physical plan (scan/index scan selection rules).
- Execution: executors (Volcano model).

Acceptance criteria:

- stages are separated and testable independently
- it is possible to identify the failing stage

### 2.5. Storage engine

Requirements:

- disk-backed heap pages of a fixed size
- `TID` (pageId + slotId)
- tuple serialization to/from bytes
- a page manager (read/write fixed-size pages)
- a buffer pool with an eviction policy (e.g., LRU/Clock)
- flushing dirty pages

Acceptance criteria:

- data persists on disk and is available after restart
- tests exist for page I/O and serialization

### 2.6. Indexes

At least two types:

- `HASH` для точечного поиска,
- `BTREE` (B+Tree) для диапазонов.

Requirements:

- the index stores (key → list of TIDs)
- the optimizer can select an index scan
- executors can fetch tuples by TID

Acceptance criteria:

- unit tests exist for index behavior
- an integration test exists: “create index → queries run via index scan”

## 3. Non-functional requirements

- Language: Java 17.
- Build: Gradle (wrapper).
- Code should be split into packages by responsibility.
- Test coverage: unit + at least one end-to-end scenario (over TCP).
- Documentation: README + architecture + examples.

## 4. Deliverables

Repository contents:

- source code
- build configuration (Gradle wrapper)
- tests
- documentation (`README.md` + `docs/…`)
- run scripts (optional)

## 5. Extensions (optional)

- `UPDATE`/`DELETE`, `DROP`.
- JOIN.
- Transactions (WAL, locking, MVCC).
- Cost-based optimizer.


