package ru.open.cu.student.parser.nodes;

public class RangeVar implements AstNode {
    private final String tableName;
    private final String alias;

    public RangeVar(String tableName, String alias) {
        this.tableName = tableName;
        this.alias = alias;
    }

    public String getTableName() {
        return tableName;
    }

    public String getAlias() {
        return alias;
    }
}



