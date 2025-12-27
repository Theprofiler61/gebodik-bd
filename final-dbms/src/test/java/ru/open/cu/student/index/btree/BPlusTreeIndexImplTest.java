package ru.open.cu.student.index.btree;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.open.cu.student.index.TID;
import ru.open.cu.student.memory.manager.HeapPageFileManager;
import ru.open.cu.student.memory.manager.PageFileManager;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BPlusTreeIndexImplTest {

    @Test
    void insert_and_search_works_for_many_keys_and_duplicates(@TempDir Path tempDir) {
        PageFileManager fm = new HeapPageFileManager();
        BPlusTreeIndex index = new BPlusTreeIndexImpl("idx_users_id_btree", "id", 3, fm, tempDir.resolve("idx_users_id.bpt"));

        int n = 2_000;
        Map<Long, List<TID>> expected = new HashMap<>();
        List<Long> keys = new ArrayList<>(n);
        for (long i = 0; i < n; i++) keys.add(i);
        Collections.shuffle(keys, new Random(1));

        // Insert shuffled keys + add duplicates for a subset.
        int slot = 0;
        for (long k : keys) {
            TID tid = new TID(1, (short) slot++);
            index.insert(k, tid);
            expected.computeIfAbsent(k, __ -> new ArrayList<>()).add(tid);
        }
        for (long k = 0; k < 100; k++) {
            TID tid = new TID(2, (short) slot++);
            index.insert(k, tid);
            expected.computeIfAbsent(k, __ -> new ArrayList<>()).add(tid);
        }

        for (Map.Entry<Long, List<TID>> e : expected.entrySet()) {
            List<TID> found = index.search(e.getKey());
            assertEquals(new HashSet<>(e.getValue()), new HashSet<>(found), "key=" + e.getKey());
        }
        assertTrue(index.getHeight() >= 1);
    }

    @Test
    void rangeSearch_respects_inclusive_flag(@TempDir Path tempDir) {
        PageFileManager fm = new HeapPageFileManager();
        BPlusTreeIndex index = new BPlusTreeIndexImpl("idx_rangeoree", "id", 3, fm, tempDir.resolve("idx_range.bpt"));

        for (long i = 0; i < 100; i++) {
            index.insert(i, new TID(0, (short) i));
        }

        List<TID> inclusive = index.rangeSearch(10L, 20L, true);
        assertEquals(11, inclusive.size());

        List<TID> exclusive = index.rangeSearch(10L, 20L, false);
        assertEquals(9, exclusive.size());

        // Sanity: bounds-only ranges.
        assertEquals(10, index.rangeSearch(null, 9L, true).size());
        assertEquals(10, index.rangeSearch(90L, null, true).size());
    }
}


