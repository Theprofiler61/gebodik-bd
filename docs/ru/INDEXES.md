# Индексы

В проекте реализованы два типа индексов:

- `HASH` — для точечных запросов `=`,
- `BTREE` — B+Tree для точечных и диапазонных запросов.

Ключевая цель модуля — показать:

- как индекс строится и хранится,
- как оптимизатор выбирает `IndexScan`,
- как executor извлекает строки по `TID`.

## Hash index

Реализация: `src/main/java/ru/open/cu/student/index/hash/HashIndexImpl.java`

Свойства:

- быстрый точечный поиск,
- ориентирован на предикаты вида `WHERE col = const`,
- персистентность реализована на стороне индекса (индекс хранит собственный файл).

## B+Tree (BTREE)

Реализация: `src/main/java/ru/open/cu/student/index/btree/BPlusTreeIndexImpl.java`

Свойства:

- поддерживает точечный поиск и range‑scan (`<`, `<=`, `>`, `>=`),
- используется оптимизатором для диапазонных условий,
- текущая версия имеет учебные ограничения по персистентности (см. ниже).

### Ограничение по персистентности BTREE

В `DefaultIndexManager` BTREE индекс **пересобирается** сканом таблицы при старте:

- это упрощает реализацию восстановления,
- но делает старт дороже и не демонстрирует полноценный persistent‑index.

План работ: `ROADMAP.md` (раздел про storage/indexes).

## Как выбирается IndexScan

Текущая стратегия — rule‑based (файл: `optimizer/strategy/RuleBasedScanStrategy.java`):

- Для `col = const`:
  - если есть `HASH` индекс — выбирается он,
  - иначе, если есть `BTREE` — выбирается он.
- Для сравнений `col < const`, `col <= const`, `col > const`, `col >= const`:
  - при наличии `BTREE` выбирается range‑scan по дереву,
  - иначе выполняется `SeqScan`.

## Как исполняется IndexScan

Физический узел: `optimizer/node/PhysicalIndexScanNode.java`

Исполнители:

- `HashIndexScanExecutor`
- `BTreeIndexScanExecutor`

Оба возвращают поток `TID`, а затем извлекают строки из heap‑таблицы по адресу.

