package ru.open.cu.student.index.btree;

import ru.open.cu.student.index.IndexType;
import ru.open.cu.student.index.TID;
import ru.open.cu.student.memory.manager.PageFileManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * In-memory B+Tree index implementation used by the educational DBMS.
 *
 * Notes:
 * - It supports point search and range search.
 * - Persistence is not implemented here; the index is (re)built by scanning the table when created.
 */
public class BPlusTreeIndexImpl implements BPlusTreeIndex {
    private final String indexName;
    private final String columnName;
    private final int order;
    @SuppressWarnings("unused")
    private final PageFileManager pageManager;
    @SuppressWarnings("unused")
    private final Path indexPath;

    private int rootPageId;
    private int height;

    private final Map<Integer, BPlusTreeNode> nodes = new HashMap<>();
    private int nextPageId = 0;

    public BPlusTreeIndexImpl(String indexName, String columnName, int order, PageFileManager pageManager) {
        this(indexName, columnName, order, pageManager, defaultIndexPath(indexName));
    }

    public BPlusTreeIndexImpl(String indexName, String columnName, int order, PageFileManager pageManager, Path indexPath) {
        this.indexName = Objects.requireNonNull(indexName, "indexName");
        this.columnName = Objects.requireNonNull(columnName, "columnName");
        this.order = order;
        this.pageManager = Objects.requireNonNull(pageManager, "pageManager");
        this.indexPath = Objects.requireNonNull(indexPath, "indexPath");

        if (order < 2) {
            throw new IllegalArgumentException("order must be >= 2");
        }

        initStorageDir();
        BPlusTreeNode root = newLeafNode();
        this.rootPageId = root.pageId;
        this.height = 1;
    }

    @Override
    public void insert(Comparable<?> key, TID tid) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(tid, "tid");

        BPlusTreeNode leaf = findLeaf(key);
        leafInsert(leaf, key, tid);
        if (leaf.numKeys > maxKeys()) {
            splitLeaf(leaf);
        }
    }

    @Override
    public List<TID> search(Comparable<?> key) {
        Objects.requireNonNull(key, "key");
        BPlusTreeNode leaf = findLeaf(key);
        int idx = leafKeyIndex(leaf, key);
        if (idx < 0) return List.of();
        @SuppressWarnings("unchecked")
        List<TID> tids = (List<TID>) leaf.pointers[idx];
        return new ArrayList<>(tids);
    }

    @Override
    public List<TID> rangeSearch(Comparable<?> from, Comparable<?> to, boolean inclusive) {
        if (from != null && to != null) {
            int cmp = compare(from, to);
            if (cmp > 0) return List.of();
        }

        List<TID> out = new ArrayList<>();

        BPlusTreeNode leaf;
        int startIdx;
        if (from == null) {
            leaf = leftmostLeaf();
            startIdx = 0;
        } else {
            leaf = findLeaf(from);
            startIdx = firstKeyGreaterOrEqual(leaf, from, inclusive);
        }

        while (leaf != null) {
            for (int i = startIdx; i < leaf.numKeys; i++) {
                Comparable<?> k = leaf.keys[i];
                if (k == null) continue;

                if (to != null) {
                    int c = compare(k, to);
                    if (c > 0 || (c == 0 && !inclusive)) {
                        return out;
                    }
                }

                if (from != null) {
                    int c = compare(k, from);
                    if (c < 0 || (c == 0 && !inclusive)) {
                        continue;
                    }
                }

                @SuppressWarnings("unchecked")
                List<TID> tids = (List<TID>) leaf.pointers[i];
                out.addAll(tids);
            }
            startIdx = 0;
            leaf = (leaf.rightSiblingPageId == -1) ? null : readNode(leaf.rightSiblingPageId);
        }

        return out;
    }

    @Override
    public List<TID> searchGreaterThan(Comparable<?> value, boolean inclusive) {
        Objects.requireNonNull(value, "value");
        return rangeSearch(value, null, inclusive);
    }

    @Override
    public List<TID> searchLessThan(Comparable<?> value, boolean inclusive) {
        Objects.requireNonNull(value, "value");
        return rangeSearch(null, value, inclusive);
    }

    @Override
    public List<TID> scanAll() {
        return rangeSearch(null, null, true);
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
        return IndexType.BTREE;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getOrder() {
        return order;
    }

    private BPlusTreeNode readNode(int pageId) {
        BPlusTreeNode node = nodes.get(pageId);
        if (node == null) {
            throw new IllegalStateException("Node not found: pageId=" + pageId);
        }
        return node;
    }

    private void writeNode(BPlusTreeNode node) {
        nodes.put(node.pageId, node);
    }

    private BPlusTreeNode findLeaf(Comparable<?> key) {
        BPlusTreeNode cur = readNode(rootPageId);
        while (!cur.isLeaf) {
            int childIdx = internalChildIndex(cur, key);
            int childPageId = (Integer) cur.pointers[childIdx];
            cur = readNode(childPageId);
        }
        return cur;
    }

    private static final class BPlusTreeNode {
        int pageId;
        int parentPageId = -1;
        boolean isLeaf;
        int numKeys;

        Comparable<?>[] keys;
        Object[] pointers;

        @SuppressWarnings("unused")
        int leftSiblingPageId = -1;
        int rightSiblingPageId = -1;
    }

    private static Path defaultIndexPath(String indexName) {
        return Paths.get("build", "indexes", indexName + ".bpt");
    }

    private void initStorageDir() {
        try {
            Path parent = indexPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create index directory for " + indexPath, e);
        }
    }

    private int maxKeys() {
        return 2 * order - 1;
    }

    private int maxChildren() {
        return 2 * order;
    }

    private BPlusTreeNode newLeafNode() {
        BPlusTreeNode n = new BPlusTreeNode();
        n.pageId = allocatePageId();
        n.isLeaf = true;
        n.numKeys = 0;
        n.keys = new Comparable<?>[maxKeys() + 1];
        n.pointers = new Object[maxKeys() + 1];
        writeNode(n);
        return n;
    }

    private BPlusTreeNode newInternalNode() {
        BPlusTreeNode n = new BPlusTreeNode();
        n.pageId = allocatePageId();
        n.isLeaf = false;
        n.numKeys = 0;
        n.keys = new Comparable<?>[maxKeys() + 1];
        n.pointers = new Object[maxChildren() + 1];
        writeNode(n);
        return n;
    }

    private int allocatePageId() {
        return nextPageId++;
    }

    private int compare(Comparable<?> a, Comparable<?> b) {
        @SuppressWarnings({"rawtypes", "unchecked"})
        int c = ((Comparable) a).compareTo(b);
        return c;
    }

    private int leafKeyIndex(BPlusTreeNode leaf, Comparable<?> key) {
        for (int i = 0; i < leaf.numKeys; i++) {
            if (leaf.keys[i] != null && compare(leaf.keys[i], key) == 0) return i;
        }
        return -1;
    }

    private int firstKeyGreaterOrEqual(BPlusTreeNode leaf, Comparable<?> key, boolean inclusive) {
        for (int i = 0; i < leaf.numKeys; i++) {
            Comparable<?> k = leaf.keys[i];
            if (k == null) continue;
            int c = compare(k, key);
            if (c > 0) return i;
            if (c == 0) return inclusive ? i : i + 1;
        }
        return leaf.numKeys;
    }

    private void leafInsert(BPlusTreeNode leaf, Comparable<?> key, TID tid) {
        int idx = leafKeyIndex(leaf, key);
        if (idx >= 0) {
            @SuppressWarnings("unchecked")
            List<TID> tids = (List<TID>) leaf.pointers[idx];
            tids.add(tid);
            return;
        }

        int insertPos = 0;
        while (insertPos < leaf.numKeys && compare(leaf.keys[insertPos], key) < 0) {
            insertPos++;
        }
        for (int i = leaf.numKeys; i > insertPos; i--) {
            leaf.keys[i] = leaf.keys[i - 1];
            leaf.pointers[i] = leaf.pointers[i - 1];
        }
        leaf.keys[insertPos] = key;
        List<TID> tids = new ArrayList<>();
        tids.add(tid);
        leaf.pointers[insertPos] = tids;
        leaf.numKeys++;
    }

    private int internalChildIndex(BPlusTreeNode internal, Comparable<?> key) {
        int i = 0;
        while (i < internal.numKeys) {
            Comparable<?> k = internal.keys[i];
            if (k != null && compare(key, k) < 0) {
                return i;
            }
            i++;
        }
        return internal.numKeys;
    }

    private BPlusTreeNode leftmostLeaf() {
        BPlusTreeNode cur = readNode(rootPageId);
        while (!cur.isLeaf) {
            int childPageId = (Integer) cur.pointers[0];
            cur = readNode(childPageId);
        }
        return cur;
    }

    private void splitLeaf(BPlusTreeNode leaf) {
        int splitPoint = (leaf.numKeys + 1) / 2;
        BPlusTreeNode newLeaf = newLeafNode();

        newLeaf.rightSiblingPageId = leaf.rightSiblingPageId;
        newLeaf.leftSiblingPageId = leaf.pageId;
        leaf.rightSiblingPageId = newLeaf.pageId;
        if (newLeaf.rightSiblingPageId != -1) {
            BPlusTreeNode right = readNode(newLeaf.rightSiblingPageId);
            right.leftSiblingPageId = newLeaf.pageId;
            writeNode(right);
        }

        int j = 0;
        for (int i = splitPoint; i < leaf.numKeys; i++, j++) {
            newLeaf.keys[j] = leaf.keys[i];
            newLeaf.pointers[j] = leaf.pointers[i];
            newLeaf.numKeys++;
            leaf.keys[i] = null;
            leaf.pointers[i] = null;
        }
        leaf.numKeys = splitPoint;

        Comparable<?> separator = newLeaf.keys[0];
        insertIntoParent(leaf, separator, newLeaf);

        writeNode(leaf);
        writeNode(newLeaf);
    }

    private void insertIntoParent(BPlusTreeNode left, Comparable<?> key, BPlusTreeNode right) {
        if (left.parentPageId == -1) {
            BPlusTreeNode newRoot = newInternalNode();
            newRoot.keys[0] = key;
            newRoot.numKeys = 1;
            newRoot.pointers[0] = left.pageId;
            newRoot.pointers[1] = right.pageId;
            left.parentPageId = newRoot.pageId;
            right.parentPageId = newRoot.pageId;
            this.rootPageId = newRoot.pageId;
            this.height++;
            writeNode(left);
            writeNode(right);
            writeNode(newRoot);
            return;
        }

        BPlusTreeNode parent = readNode(left.parentPageId);
        int insertKeyPos = 0;
        while (insertKeyPos < parent.numKeys && compare(parent.keys[insertKeyPos], key) < 0) {
            insertKeyPos++;
        }

        for (int i = parent.numKeys; i > insertKeyPos; i--) {
            parent.keys[i] = parent.keys[i - 1];
        }
        for (int i = parent.numKeys + 1; i > insertKeyPos + 1; i--) {
            parent.pointers[i] = parent.pointers[i - 1];
        }

        parent.keys[insertKeyPos] = key;
        parent.pointers[insertKeyPos + 1] = right.pageId;
        parent.numKeys++;
        right.parentPageId = parent.pageId;
        writeNode(right);
        writeNode(parent);

        if (parent.numKeys > maxKeys()) {
            splitInternal(parent);
        }
    }

    private void splitInternal(BPlusTreeNode node) {
        int mid = node.numKeys / 2;
        Comparable<?> promote = node.keys[mid];

        BPlusTreeNode right = newInternalNode();
        right.parentPageId = node.parentPageId;

        int rKeyIdx = 0;
        for (int i = mid + 1; i < node.numKeys; i++) {
            right.keys[rKeyIdx++] = node.keys[i];
            right.numKeys++;
            node.keys[i] = null;
        }

        int rPtrIdx = 0;
        for (int i = mid + 1; i <= node.numKeys; i++) {
            right.pointers[rPtrIdx] = node.pointers[i];
            if (right.pointers[rPtrIdx] != null) {
                int childId = (Integer) right.pointers[rPtrIdx];
                BPlusTreeNode child = readNode(childId);
                child.parentPageId = right.pageId;
                writeNode(child);
            }
            rPtrIdx++;
            node.pointers[i] = null;
        }

        node.keys[mid] = null;
        node.numKeys = mid;

        writeNode(node);
        writeNode(right);

        insertIntoParent(node, promote, right);
    }
}


