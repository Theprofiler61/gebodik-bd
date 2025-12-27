# Протокол

Сервер и клиент взаимодействуют по TCP с простым framed‑протоколом и JSON‑ответами.

## Framed protocol

Реализация: `src/main/java/ru/open/cu/student/protocol/FramedProtocol.java`

Формат сообщения:

```
┌────────────┬─────────────────────────┐
│ Length (4B)│ Payload (UTF-8 bytes)   │
└────────────┴─────────────────────────┘
```

Где `Length` — длина payload в байтах (big‑endian), payload — UTF‑8 строка.

## Request (Client → Server)

Payload запроса: SQL‑строка.

## Response (Server → Client)

Payload ответа: JSON.

### Успешный ответ

```json
{
  "ok": true,
  "columns": ["id", "name"],
  "rows": [[1, "Alice"], [2, "Bob"]]
}
```

### Ошибка

```json
{
  "ok": false,
  "errorStage": "PARSER",
  "errorMessage": "Expected SELECT, CREATE or INSERT"
}
```

`errorStage` соответствует стадии pipeline (см. `PIPELINE.md`).

## CLI режимы вывода

CLI может отображать:

- “pretty” таблицы (читаемый вывод),
- raw JSON‑ответы (для отладки протокола).

Подробности запуска: `DEVELOPMENT.md`.

