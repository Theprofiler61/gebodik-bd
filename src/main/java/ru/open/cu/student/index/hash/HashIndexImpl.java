package ru.open.cu.student.index.hash;

import ru.open.cu.student.catalog.operation.OperationManager;
import ru.open.cu.student.index.IndexType;
import ru.open.cu.student.index.TID;
import ru.open.cu.student.memory.manager.PageFileManager;
import ru.open.cu.student.memory.page.HeapPage;
import ru.open.cu.student.memory.page.Page;

import java.util.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HashIndexImpl implements HashIndex {
    private final String indexName;
    private final String columnName;
    private final PageFileManager pageManager;
    @SuppressWarnings("unused")
    private final OperationManager operationManager;
    private final Path indexPath;

    private record MetaPage(
            int numBuckets,
            int maxBucket,
            int lowmask,
            int highmask,
            int splitPointer,
            long recordCount
    ) {
    }

    private MetaPage metaPage;
    private final Map<Integer, BucketPage> bucketCache = new HashMap<>();

    private static final int MAX_RECORDS_PER_PAGE = 32;

    public HashIndexImpl(String indexName, String columnName,
                         PageFileManager pageManager,
                         OperationManager operationManager) {
        this(indexName, columnName, pageManager, operationManager, defaultIndexPath(indexName));
    }

    public HashIndexImpl(String indexName, String columnName,
                         PageFileManager pageManager,
                         OperationManager operationManager,
                         Path indexPath) {
        this.indexName = indexName;
        this.columnName = columnName;
        this.pageManager = pageManager;
        this.operationManager = operationManager;
        this.indexPath = Objects.requireNonNull(indexPath, "indexPath");

        this.metaPage = new MetaPage(
                16,
                15,
                0xF,
                0xF,
                0,
                0L
        );

        initOnDiskIfNeeded();
    }

    @Override
    public void insert(Comparable<?> key, TID tid) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(tid, "tid");

        int hash = hashFunction(key);
        int bucketId = computeBucket(hash);

        byte[] keyBytes = encodeKey(key);
        BucketRecord record = new BucketRecord(hash, keyBytes, tid);

        boolean overflowCreated = insertIntoBucketChain(bucketId, record);
        metaPage = new MetaPage(
                metaPage.numBuckets,
                metaPage.maxBucket,
                metaPage.lowmask,
                metaPage.highmask,
                metaPage.splitPointer,
                metaPage.recordCount + 1
        );
        writeMetaPage();

        if (overflowCreated) {
            performSplit();
        }
    }

    @Override
    public List<TID> search(Comparable<?> key) {
        Objects.requireNonNull(key, "key");
        List<TID> results = new ArrayList<>();

        int hash = hashFunction(key);
        int bucketId = computeBucket(hash);
        byte[] keyBytes = encodeKey(key);

        int pageId = primaryPageId(bucketId);
        while (pageId != -1) {
            BucketPage page = readAnyBucketPage(pageId);
            for (BucketRecord r : page.records) {
                if (r.hash == hash && Arrays.equals(r.keyBytes, keyBytes)) {
                    results.add(r.tid);
                }
            }
            pageId = page.nextOverflowPageId;
        }

        return results;
    }

    @Override
    public List<TID> scanAll() {
        List<TID> allResults = new ArrayList<>();

        for (int bucketId = 0; bucketId <= metaPage.maxBucket; bucketId++) {
            int pageId = primaryPageId(bucketId);
            while (pageId != -1) {
                BucketPage page = readAnyBucketPage(pageId);
                for (BucketRecord r : page.records) {
                    allResults.add(r.tid);
                }
                pageId = page.nextOverflowPageId;
            }
        }

        return allResults;
    }

    @Override
    public String getName() {
        return indexName;
    }

    @Override
    public String getColumnName() {
        return columnName;
    }

    @Override
    public IndexType getType() {
        return IndexType.HASH;
    }

    @Override
    public int getNumBuckets() {
        return metaPage.numBuckets;
    }

    @Override
    public long getRecordCount() {
        return metaPage.recordCount;
    }

    @Override
    public int getMaxBucket() {
        return metaPage.maxBucket;
    }

    private int hashFunction(Comparable<?> key) {
        return Objects.requireNonNull(key, "key").hashCode();
    }

    private int computeBucket(int hash) {
        int bucket = hash & metaPage.highmask;
        if (bucket > metaPage.maxBucket) {
            bucket = hash & metaPage.lowmask;
        }
        return bucket;
    }

    private void performSplit() {
        int splitBucketId = metaPage.splitPointer;
        int newBucketId = metaPage.maxBucket + 1;

        int newLowmask = metaPage.lowmask;
        int newHighmask = metaPage.highmask;
        if (newBucketId > metaPage.highmask) {
            newLowmask = metaPage.highmask;
            newHighmask = (metaPage.highmask << 1) | 1;
        }

        int newSplitPointer = metaPage.splitPointer + 1;
        if (newSplitPointer > newLowmask) {
            newSplitPointer = 0;
            newLowmask = newHighmask;
        }

        MetaPage updatedMeta = new MetaPage(
                metaPage.numBuckets + 1,
                metaPage.maxBucket + 1,
                newLowmask,
                newHighmask,
                newSplitPointer,
                metaPage.recordCount
        );

        List<BucketRecord> records = readAllRecordsFromBucket(splitBucketId);

        writeAnyBucketPage(primaryPageId(splitBucketId), new BucketPage(-1, new ArrayList<>()));

        writeAnyBucketPage(primaryPageId(newBucketId), new BucketPage(-1, new ArrayList<>()));

        this.metaPage = updatedMeta;
        writeMetaPage();

        for (BucketRecord r : records) {
            int bucketId = computeBucket(r.hash);
            insertIntoBucketChain(bucketId, r);
        }

        this.metaPage = updatedMeta;

        bucketCache.remove(primaryPageId(splitBucketId));
        bucketCache.remove(primaryPageId(newBucketId));
    }

    private static class BucketPage {
        int nextOverflowPageId = -1;
        List<BucketRecord> records = new ArrayList<>();

        BucketPage(int nextOverflowPageId, List<BucketRecord> records) {
            this.nextOverflowPageId = nextOverflowPageId;
            this.records = records;
        }
    }

    private record BucketRecord(
            int hash,
            byte[] keyBytes,
            TID tid
    ) {
    }

    private static Path defaultIndexPath(String indexName) {
        return Paths.get("build", "indexes", indexName + ".idx");
    }

    private void initOnDiskIfNeeded() {
        try {
            Path parent = indexPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create index directory for " + indexPath, e);
        }

        if (Files.exists(indexPath)) {
            try {
                Page meta = pageManager.read(0, indexPath);
                this.metaPage = decodeMeta(meta);
                return;
            } catch (Exception ignored) {
            }
        }

        writeMetaPage();
        for (int bucketId = 0; bucketId < 16; bucketId++) {
            writeAnyBucketPage(primaryPageId(bucketId), new BucketPage(-1, new ArrayList<>()));
        }
    }

    private int primaryPageId(int bucketId) {
        return 1 + bucketId;
    }

    private Page buildHeapPageFromBucket(int pageId, BucketPage bucketPage) {
        HeapPage page = new HeapPage(pageId);
        page.write(encodeInt(bucketPage.nextOverflowPageId));
        for (BucketRecord r : bucketPage.records) {
            page.write(encodeBucketRecord(r));
        }
        return page;
    }

    private BucketPage decodeBucketPage(Page page) {
        if (page.size() == 0) {
            return new BucketPage(-1, new ArrayList<>());
        }
        int next = decodeInt(page.read(0));
        List<BucketRecord> records = new ArrayList<>();
        for (int i = 1; i < page.size(); i++) {
            records.add(decodeBucketRecord(page.read(i)));
        }
        records.sort((a, b) -> {
            int c = Integer.compare(a.hash, b.hash);
            if (c != 0) return c;
            return compareBytes(a.keyBytes, b.keyBytes);
        });
        return new BucketPage(next, records);
    }

    private BucketPage readAnyBucketPage(int pageId) {
        BucketPage cached = bucketCache.get(pageId);
        if (cached != null) return cached;
        Page p = pageManager.read(pageId, indexPath);
        BucketPage decoded = decodeBucketPage(p);
        bucketCache.put(pageId, decoded);
        return decoded;
    }

    private void writeAnyBucketPage(int pageId, BucketPage page) {
        Page p = buildHeapPageFromBucket(pageId, page);
        pageManager.write(p, indexPath);
        bucketCache.put(pageId, page);
    }

    private void writeMetaPage() {
        HeapPage page = new HeapPage(0);
        page.write(encodeMeta(metaPage));
        pageManager.write(page, indexPath);
    }

    private MetaPage decodeMeta(Page meta) {
        if (meta.size() == 0) return this.metaPage;
        byte[] bytes = meta.read(0);
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        int numBuckets = buf.getInt();
        int maxBucket = buf.getInt();
        int lowmask = buf.getInt();
        int highmask = buf.getInt();
        int splitPointer = buf.getInt();
        long recordCount = buf.getLong();
        return new MetaPage(numBuckets, maxBucket, lowmask, highmask, splitPointer, recordCount);
    }

    private byte[] encodeMeta(MetaPage meta) {
        ByteBuffer buf = ByteBuffer.allocate(4 + 4 + 4 + 4 + 4 + 8);
        buf.putInt(meta.numBuckets);
        buf.putInt(meta.maxBucket);
        buf.putInt(meta.lowmask);
        buf.putInt(meta.highmask);
        buf.putInt(meta.splitPointer);
        buf.putLong(meta.recordCount);
        return buf.array();
    }

    private static byte[] encodeInt(int v) {
        return ByteBuffer.allocate(4).putInt(v).array();
    }

    private static int decodeInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    private byte[] encodeBucketRecord(BucketRecord r) {
        int keyLen = r.keyBytes.length;
        ByteBuffer buf = ByteBuffer.allocate(4 + 4 + keyLen + 4 + 2);
        buf.putInt(r.hash);
        buf.putInt(keyLen);
        buf.put(r.keyBytes);
        buf.putInt(r.tid.pageId());
        buf.putShort(r.tid.slotId());
        return buf.array();
    }

    private BucketRecord decodeBucketRecord(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        int hash = buf.getInt();
        int keyLen = buf.getInt();
        byte[] keyBytes = new byte[keyLen];
        buf.get(keyBytes);
        int pageId = buf.getInt();
        short slotId = buf.getShort();
        return new BucketRecord(hash, keyBytes, new TID(pageId, slotId));
    }

    private static int compareBytes(byte[] a, byte[] b) {
        int min = Math.min(a.length, b.length);
        for (int i = 0; i < min; i++) {
            int c = Byte.compare(a[i], b[i]);
            if (c != 0) return c;
        }
        return Integer.compare(a.length, b.length);
    }

    private byte[] encodeKey(Comparable<?> key) {
        if (key instanceof Integer v) {
            ByteBuffer buf = ByteBuffer.allocate(1 + 4);
            buf.put((byte) 1).putInt(v);
            return buf.array();
        }
        if (key instanceof Long v) {
            ByteBuffer buf = ByteBuffer.allocate(1 + 8);
            buf.put((byte) 2).putLong(v);
            return buf.array();
        }
        if (key instanceof String s) {
            byte[] utf8 = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            ByteBuffer buf = ByteBuffer.allocate(1 + 4 + utf8.length);
            buf.put((byte) 3).putInt(utf8.length).put(utf8);
            return buf.array();
        }
        String s = key.getClass().getName() + ":" + key;
        byte[] utf8 = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(1 + 4 + utf8.length);
        buf.put((byte) 3).putInt(utf8.length).put(utf8);
        return buf.array();
    }

    private boolean insertIntoBucketChain(int bucketId, BucketRecord record) {
        int pageId = primaryPageId(bucketId);
        BucketPage page = readAnyBucketPage(pageId);

        while (true) {
            if (page.records.size() < MAX_RECORDS_PER_PAGE) {
                page.records.add(record);
                page.records.sort((a, b) -> {
                    int c = Integer.compare(a.hash, b.hash);
                    if (c != 0) return c;
                    return compareBytes(a.keyBytes, b.keyBytes);
                });
                writeAnyBucketPage(pageId, page);
                return false;
            }

            if (page.nextOverflowPageId == -1) {
                int newOverflowId = allocateNewPageId();
                BucketPage overflow = new BucketPage(-1, new ArrayList<>());
                overflow.records.add(record);
                writeAnyBucketPage(newOverflowId, overflow);

                page.nextOverflowPageId = newOverflowId;
                writeAnyBucketPage(pageId, page);
                return true;
            }

            pageId = page.nextOverflowPageId;
            page = readAnyBucketPage(pageId);
        }
    }

    private List<BucketRecord> readAllRecordsFromBucket(int bucketId) {
        List<BucketRecord> out = new ArrayList<>();
        int pageId = primaryPageId(bucketId);
        while (pageId != -1) {
            BucketPage page = readAnyBucketPage(pageId);
            out.addAll(page.records);
            pageId = page.nextOverflowPageId;
        }
        return out;
    }

    private int allocateNewPageId() {
        try {
            if (!Files.exists(indexPath)) {
                writeMetaPage();
            }
            long size = Files.size(indexPath);
            return (int) (size / HeapPage.PAGE_SIZE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to allocate new page for index " + indexPath, e);
        }
    }
}
