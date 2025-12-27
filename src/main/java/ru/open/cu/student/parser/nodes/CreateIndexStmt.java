package ru.open.cu.student.parser.nodes;

public class CreateIndexStmt implements AstNode {
    private final String indexName;
    private final String tableName;
    private final String columnName;
    private final String indexType;

    public CreateIndexStmt(String indexName, String tableName, String columnName, String indexType) {
        this.indexName = indexName;
        this.tableName = tableName;
        this.columnName = columnName;
        this.indexType = indexType;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getIndexType() {
        return indexType;
    }
}


