# SQL‑диалект

Этот документ описывает поддерживаемый поднабор SQL (диалект проекта) и ограничения.

## Общие правила

- Строковые литералы: одинарные кавычки `'...'`.
- Идентификаторы: латиница/цифры/`_` (см. лексер).
- Точка с запятой `;` допускается и рекомендуется в интерактивном режиме.

## Типы данных

На текущем этапе поддерживаются:

- `INT64` — 64‑битное целое число
- `VARCHAR` — строка переменной длины

## CREATE TABLE

Синтаксис:

```sql
CREATE TABLE table_name (
  col1 TYPE,
  col2 TYPE,
  ...
);
```

Пример:

```sql
CREATE TABLE users (id INT64, name VARCHAR, age INT64);
```

## INSERT

Синтаксис:

```sql
INSERT INTO table_name VALUES (value1, value2, ...);
```

Пример:

```sql
INSERT INTO users VALUES (1, 'Alice', 25);
```

## SELECT

Синтаксис:

```sql
SELECT targets FROM table_name [WHERE predicate];
```

Поддерживается:

- `*` (все колонки),
- список выражений: `col`, `col1 + col2`, `price * 2`,
- алиасы: `expr AS alias`,
- `WHERE` с логикой `AND`/`OR`,
- сравнения: `=`, `!=`, `<`, `>`, `<=`, `>=`.

Примеры:

```sql
SELECT * FROM users;
SELECT id, name FROM users WHERE id > 10 AND age <= 30;
SELECT name, age + 1 AS next_age FROM users WHERE name != 'Bob';
```

## CREATE INDEX

Синтаксис:

```sql
CREATE INDEX index_name ON table_name(column_name) USING type;
```

Где `type`:

- `HASH` — для точечных запросов (предикат `=`),
- `BTREE` — для диапазонов (`<`, `<=`, `>`, `>=`) и точечных запросов (см. `INDEXES.md`).

Пример:

```sql
CREATE INDEX idx_users_id ON users(id) USING HASH;
```

## Ограничения

В текущей версии отсутствуют:

- `UPDATE`, `DELETE`,
- `DROP TABLE`, `DROP INDEX`,
- `JOIN`, `GROUP BY`, `ORDER BY`, `LIMIT`,
- транзакции и конкурентная модель исполнения.

Актуальный список планов и задач развития: `ROADMAP.md`.

