# Примеры использования

Ниже — несколько коротких сценариев, которые можно повторить локально.

## Сценарий 1: Минимальный “таблица → данные → select”

Терминал 1:

```bash
./run-server.sh
```

Терминал 2:

```bash
./run-cli.sh
```

В CLI:

```sql
CREATE TABLE users (id INT64, name VARCHAR, age INT64);
INSERT INTO users VALUES (1, 'Alice', 25);
INSERT INTO users VALUES (2, 'Bob', 30);
SELECT * FROM users;
SELECT name, age FROM users WHERE age > 25;
```

## Сценарий 2: Индекс для точечного поиска

```sql
CREATE TABLE orders (id INT64, customer_id INT64, amount INT64);
INSERT INTO orders VALUES (1, 100, 500);
INSERT INTO orders VALUES (2, 101, 750);
INSERT INTO orders VALUES (3, 100, 300);

-- Без индекса будет SeqScan
SELECT * FROM orders WHERE customer_id = 100;

-- Создаём индекс
CREATE INDEX idx_orders_customer ON orders(customer_id) USING HASH;

-- С индексом оптимизатор выберет IndexScan
SELECT * FROM orders WHERE customer_id = 100;
```

## Сценарий 3: Диапазон по BTREE

```sql
CREATE TABLE products (id INT64, price INT64);
INSERT INTO products VALUES (1, 10);
INSERT INTO products VALUES (2, 20);
INSERT INTO products VALUES (3, 30);

CREATE INDEX idx_products_price ON products(price) USING BTREE;

-- Диапазонный запрос
SELECT * FROM products WHERE price >= 15 AND price <= 25;
```

