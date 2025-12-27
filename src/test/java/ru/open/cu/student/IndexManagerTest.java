package ru.open.cu.student;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.index.IndexDescriptor;
import ru.open.cu.student.index.IndexType;
import ru.open.cu.student.index.TID;
import ru.open.cu.student.index.hash.HashIndex;
import ru.open.cu.student.storage.engine.StorageEngine;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IndexManagerTest {
    @Test
    void create_index_builds_from_existing_data_and_insert_updates(@TempDir Path dir) {
        try (StorageEngine engine = new StorageEngine(dir, 10)) {
            engine.catalog().createTable("users", List.of(
                    new ColumnDefinition(0, 0, 1, "id", 0),
                    new ColumnDefinition(0, 0, 2, "name", 1)
            ));

            TID t1 = engine.operations().insert("users", List.of(1L, "Ann"));
            TID t2 = engine.operations().insert("users", List.of(2L, "Bob"));

            engine.indexes().createIndex(new IndexDescriptor("idx_users_id", "users", "id", IndexType.HASH));
            assertTrue(engine.indexes().findIndex("users", "id", IndexType.HASH).isPresent());

            HashIndex idx = (HashIndex) engine.indexes().getIndex("idx_users_id");
            assertTrue(idx.search(1L).contains(t1));
            assertTrue(idx.search(2L).contains(t2));

            TID t3 = engine.operations().insert("users", List.of(3L, "Cat"));
            engine.indexes().onInsert("users", List.of(3L, "Cat"), t3);
            assertTrue(idx.search(3L).contains(t3));
        }
    }

    @Test
    void index_definitions_reload_on_restart(@TempDir Path dir) {
        TID t1;
        try (StorageEngine engine = new StorageEngine(dir, 10)) {
            engine.catalog().createTable("users", List.of(
                    new ColumnDefinition(0, 0, 1, "id", 0),
                    new ColumnDefinition(0, 0, 2, "name", 1)
            ));
            t1 = engine.operations().insert("users", List.of(10L, "Ann"));
            engine.indexes().createIndex(new IndexDescriptor("idx_users_id", "users", "id", IndexType.HASH));
        }

        try (StorageEngine engine2 = new StorageEngine(dir, 10)) {
            assertTrue(engine2.indexes().findIndex("users", "id", IndexType.HASH).isPresent());
            HashIndex idx = (HashIndex) engine2.indexes().getIndex("idx_users_id");
            assertTrue(idx.search(10L).contains(t1));
        }
    }
}


