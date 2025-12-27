package ru.open.cu.student.parser.nodes;

public class ColumnRef implements AstNode {
    private final String tableName;
    private final String columnName;

    public ColumnRef(String tableName, String columnName) {
        this.tableName = tableName;
        this.columnName = columnName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getColumnName() {
        return columnName;
    }
}
