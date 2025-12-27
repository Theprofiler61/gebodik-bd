package ru.open.cu.student;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.index.TID;
import ru.open.cu.student.storage.Row;
import ru.open.cu.student.storage.engine.StorageEngine;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OperationManagerTest {
    @Test
    void insert_returns_tid_and_read_returns_row(@TempDir Path dir) {
        try (StorageEngine engine = new StorageEngine(dir, 10)) {
            engine.catalog().createTable("users", List.of(
                    new ColumnDefinition(0, 0, 1, "id", 0),
                    new ColumnDefinition(0, 0, 2, "name", 1)
            ));

            TID tid = engine.operations().insert("users", List.of(1L, "Ann"));
            assertNotNull(tid);

            Row row = engine.operations().read("users", tid);
            assertEquals(List.of(1L, "Ann"), row.values());
        }
    }

    @Test
    void data_persists_and_tid_can_be_reused_after_restart(@TempDir Path dir) {
        TID tid;
        try (StorageEngine engine = new StorageEngine(dir, 10)) {
            engine.catalog().createTable("users", List.of(
                    new ColumnDefinition(0, 0, 1, "id", 0),
                    new ColumnDefinition(0, 0, 2, "name", 1)
            ));
            tid = engine.operations().insert("users", List.of(7L, "Bob"));
        }

        try (StorageEngine engine2 = new StorageEngine(dir, 10)) {
            Row row = engine2.operations().read("users", tid);
            assertEquals(List.of(7L, "Bob"), row.values());
        }
    }
}


