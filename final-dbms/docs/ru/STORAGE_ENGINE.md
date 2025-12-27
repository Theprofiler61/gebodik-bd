# Storage engine

Этот документ описывает хранение данных: heap‑страницы, идентификаторы строк (TID), buffer pool и файловую раскладку.

## Основные понятия

### Heap table

Таблица хранится как последовательность heap‑страниц фиксированного размера. Каждая строка вставляется в свободный слот одной из страниц.

### TID (Tuple Identifier)

TID — адрес строки внутри heap‑хранилища:

- `pageId` — номер страницы,
- `slotId` — номер слота на странице.

В коде: `src/main/java/ru/open/cu/student/index/TID.java`.

## Heap pages

Файл: `src/main/java/ru/open/cu/student/memory/page/HeapPage.java`

Размер страницы — 8KB. Структура вдохновлена классической slot‑based схемой:

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

Преимущество такого формата — поддержка записей переменной длины и компактная компактизация.

## Tuple serializer

Файл: `src/main/java/ru/open/cu/student/memory/serializer/HeapTupleSerializer.java`

Сериализация/десериализация преобразует логическое представление строки в байтовый формат, пригодный для записи в heap‑страницы.

## PageFileManager

Файл: `src/main/java/ru/open/cu/student/memory/manager/HeapPageFileManager.java`

Задача:

- читать/писать страницы фиксированного размера,
- управлять файлами данных таблиц.

## Buffer pool

Файл: `src/main/java/ru/open/cu/student/memory/buffer/DefaultBufferPoolManager.java`

Buffer pool кэширует страницы в памяти и снижает число обращений к диску.

Ключевые моменты:

- стратегия вытеснения: LRU (`LRUReplacer`) или Clock (`ClockReplacer`);
- dirty pages записываются при вытеснении и/или при явном flush;
- потокобезопасность обеспечивается блокировками на уровне менеджера.

## Файловая раскладка

Storage engine хранит данные в каталоге `data/` (создаётся автоматически):

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

Метаданные каталога поддерживаются `CatalogManager` и `IndexManager`.

## Ограничения (осознанные)

- Нет WAL/redo‑логов и полноценной гарантии durability как в production СУБД.
- Конкурентный доступ на уровне хранения упрощён; сервер сериализует запросы.

