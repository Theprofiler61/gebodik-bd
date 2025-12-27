package ru.open.cu.student.index.hash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.open.cu.student.index.TID;
import ru.open.cu.student.memory.manager.HeapPageFileManager;
import ru.open.cu.student.memory.manager.PageFileManager;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HashIndexImplTest {
    @Test
    void insert_and_search_returns_all_tids_for_key(@TempDir Path tempDir) {
        PageFileManager fm = new HeapPageFileManager();
        Path idxPath = tempDir.resolve("idx_users_id.idx");

        HashIndex index = new HashIndexImpl("idx_users_id", "id", fm, null, idxPath);

        TID t1 = new TID(1, 10);
        TID t2 = new TID(1, 11);
        index.insert(42L, t1);
        index.insert(42L, t2);

        List<TID> found = index.search(42L);
        assertEquals(2, found.size());
        assertTrue(found.contains(t1));
        assertTrue(found.contains(t2));
    }

    @Test
    void scan_all_returns_all_inserted_tids(@TempDir Path tempDir) {
        PageFileManager fm = new HeapPageFileManager();
        Path idxPath = tempDir.resolve("idx_scan.idx");

        HashIndex index = new HashIndexImpl("idx_scan", "id", fm, null, idxPath);

        int n = 200;
        Set<TID> expected = new HashSet<>();
        for (int i = 0; i < n; i++) {
            TID tid = new TID(0, i);
            expected.add(tid);
            index.insert((long) i, tid);
        }

        List<TID> all = index.scanAll();
        assertEquals(n, all.size());
        assertEquals(expected, new HashSet<>(all));
        assertEquals(n, index.getRecordCount());
    }

    @Test
    void overflow_triggers_split_and_bucket_count_grows(@TempDir Path tempDir) {
        PageFileManager fm = new HeapPageFileManager();
        Path idxPath = tempDir.resolve("idx_split.idx");

        HashIndex index = new HashIndexImpl("idx_split", "id", fm, null, idxPath);
        int beforeMaxBucket = index.getMaxBucket();

        for (int i = 0; i < 500; i++) {
            index.insert((long) i, new TID(2, i));
        }

        assertTrue(index.getMaxBucket() >= beforeMaxBucket);
        assertTrue(index.getNumBuckets() >= 16);
        assertEquals(500, index.getRecordCount());
    }

    @Test
    void search_is_correct_after_splits(@TempDir Path tempDir) {
        PageFileManager fm = new HeapPageFileManager();
        Path idxPath = tempDir.resolve("idx_split_search.idx");

        HashIndex index = new HashIndexImpl("idx_split_search", "id", fm, null, idxPath);

        int n = 2_000; // big enough to trigger multiple splits
        for (int i = 0; i < n; i++) {
            index.insert((long) i, new TID(3, i));
        }

        for (int i = 0; i < n; i++) {
            List<TID> found = index.search((long) i);
            assertEquals(1, found.size(), "key=" + i);
            assertEquals(new TID(3, i), found.get(0), "key=" + i);
        }
    }
}


