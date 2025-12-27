# Development

## Build and test

```bash
./gradlew build
./gradlew test
```

## Running via Gradle

Server:

```bash
./gradlew run --args="--port 15432 --dataDir data --bufferPool 10"
```

CLI client:

```bash
./gradlew runCli --args="--host 127.0.0.1 --port 15432"
```

## Running via shell scripts

Server:

```bash
./run-server.sh
```

CLI:

```bash
./run-cli.sh
```

## Configuration via environment variables

### Server

| Variable | Default | Description |
|-----------|--------------|----------|
| `PORT` | `15432` | Server port |
| `DATA_DIR` | `data` | Data directory |
| `BUFFER_POOL` | `10` | Buffer pool size (pages per file) |

### Client

| Variable | Default | Description |
|-----------|--------------|----------|
| `HOST` | `127.0.0.1` | Server host |
| `PORT` | `15432` | Server port |

## CLI options

The CLI supports the following arguments:

| Option | Description |
|------|----------|
| `--host <host>` | Server host |
| `--port <port>` | Server port |
| `--sql "<SQL>"` | Execute a single query and exit |
| `--raw` | Print raw JSON responses |
| `--noColor` | Disable colored output |
| `--color` | Force-enable colors |

Examples:

```bash
./gradlew runCli --args='--sql "SELECT * FROM users;"'
./gradlew runCli --args="--raw --noColor"
```

## Interactive commands

| Command | Description |
|--------|----------|
| `\h` / `help` | Help |
| `\q` / `exit` / `quit` | Quit |
| `\raw` | Toggle raw JSON mode |
| `\color on|off` | Toggle colors |

## Notes

- For protocol debugging, use raw JSON mode (see the CLI help and `PROTOCOL.md`).
- For a “clean” run, you can delete the contents of `data/` (it will be recreated).


