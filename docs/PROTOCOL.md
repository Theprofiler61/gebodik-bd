# Protocol

The server and the client communicate over TCP using a simple framed protocol with JSON responses.

## Framed protocol

Implementation: `src/main/java/ru/open/cu/student/protocol/FramedProtocol.java`

Message layout:

```
┌────────────┬─────────────────────────┐
│ Length (4B)│ Payload (UTF-8 bytes)   │
└────────────┴─────────────────────────┘
```

`Length` is the payload size in bytes (big-endian). Payload is a UTF-8 string.

## Request (Client → Server)

Request payload: the SQL string.

## Response (Server → Client)

Response payload: JSON.

### Success response

```json
{
  "ok": true,
  "columns": ["id", "name"],
  "rows": [[1, "Alice"], [2, "Bob"]]
}
```

### Error response

```json
{
  "ok": false,
  "errorStage": "PARSER",
  "errorMessage": "Expected SELECT, CREATE or INSERT"
}
```

`errorStage` corresponds to the pipeline stage (see `PIPELINE.md`).

## CLI output modes

The CLI can render:

- “pretty” tables (human-readable)
- raw JSON responses (useful for protocol debugging)

Run details: `DEVELOPMENT.md`.


