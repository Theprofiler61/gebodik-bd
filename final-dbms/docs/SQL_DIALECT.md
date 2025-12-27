# SQL dialect

This document describes the supported subset of SQL (the project’s dialect) and limitations.

## General rules

- String literals: single quotes `'...'`.
- Identifiers: Latin letters/digits/`_` (see the lexer for details).
- Semicolons `;` are allowed and recommended in interactive mode.

## Data types

Currently supported:

- `INT64` — 64-bit integer
- `VARCHAR` — variable-length string

## CREATE TABLE

Syntax:

```sql
CREATE TABLE table_name (
  col1 TYPE,
  col2 TYPE,
  ...
);
```

Example:

```sql
CREATE TABLE users (id INT64, name VARCHAR, age INT64);
```

## INSERT

Syntax:

```sql
INSERT INTO table_name VALUES (value1, value2, ...);
```

Example:

```sql
INSERT INTO users VALUES (1, 'Alice', 25);
```

## SELECT

Syntax:

```sql
SELECT targets FROM table_name [WHERE predicate];
```

Supported:

- `*` (all columns)
- a list of expressions: `col`, `col1 + col2`, `price * 2`
- aliases: `expr AS alias`
- `WHERE` predicates with `AND`/`OR`
- comparisons: `=`, `!=`, `<`, `>`, `<=`, `>=`

Examples:

```sql
SELECT * FROM users;
SELECT id, name FROM users WHERE id > 10 AND age <= 30;
SELECT name, age + 1 AS next_age FROM users WHERE name != 'Bob';
```

## CREATE INDEX

Syntax:

```sql
CREATE INDEX index_name ON table_name(column_name) USING type;
```

Where `type` is:

- `HASH` — for point lookups (predicate `=`)
- `BTREE` — for ranges (`<`, `<=`, `>`, `>=`) and point lookups (see `INDEXES.md`)

Example:

```sql
CREATE INDEX idx_users_id ON users(id) USING HASH;
```

## Limitations

Not supported in the current version:

- `UPDATE`, `DELETE`
- `DROP TABLE`, `DROP INDEX`
- `JOIN`, `GROUP BY`, `ORDER BY`, `LIMIT`
- transactions and a full concurrency model

See `ROADMAP.md` for the up-to-date plan.


