package ru.open.cu.student.parser.nodes;

import java.util.List;

public class InsertStmt implements AstNode {
    private final String tableName;
    private final List<AstNode> values;

    public InsertStmt(String tableName, List<AstNode> values) {
        this.tableName = tableName;
        this.values = values;
    }

    public String getTableName() {
        return tableName;
    }

    public List<AstNode> getValues() {
        return values;
    }
}


