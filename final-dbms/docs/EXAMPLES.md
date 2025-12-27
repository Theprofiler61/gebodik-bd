# Usage examples

Below are a few short scenarios you can reproduce locally.

## Scenario 1: Minimal “table → data → select”

Terminal 1:

```bash
./run-server.sh
```

Terminal 2:

```bash
./run-cli.sh
```

In the CLI:

```sql
CREATE TABLE users (id INT64, name VARCHAR, age INT64);
INSERT INTO users VALUES (1, 'Alice', 25);
INSERT INTO users VALUES (2, 'Bob', 30);
SELECT * FROM users;
SELECT name, age FROM users WHERE age > 25;
```

## Scenario 2: Index for point lookups

```sql
CREATE TABLE orders (id INT64, customer_id INT64, amount INT64);
INSERT INTO orders VALUES (1, 100, 500);
INSERT INTO orders VALUES (2, 101, 750);
INSERT INTO orders VALUES (3, 100, 300);

-- Without an index this is SeqScan
SELECT * FROM orders WHERE customer_id = 100;

-- Create an index
CREATE INDEX idx_orders_customer ON orders(customer_id) USING HASH;

-- With an index the optimizer will choose IndexScan
SELECT * FROM orders WHERE customer_id = 100;
```

## Scenario 3: BTREE range scan

```sql
CREATE TABLE products (id INT64, price INT64);
INSERT INTO products VALUES (1, 10);
INSERT INTO products VALUES (2, 20);
INSERT INTO products VALUES (3, 30);

CREATE INDEX idx_products_price ON products(price) USING BTREE;

-- Range predicate
SELECT * FROM products WHERE price >= 15 AND price <= 25;
```


