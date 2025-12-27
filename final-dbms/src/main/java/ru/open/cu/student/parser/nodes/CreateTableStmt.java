package ru.open.cu.student.parser.nodes;

import java.util.List;

public class CreateTableStmt implements AstNode {
    private final String tableName;
    private final List<ColumnDef> columns;

    public CreateTableStmt(String tableName, List<ColumnDef> columns) {
        this.tableName = tableName;
        this.columns = columns;
    }

    public String getTableName() {
        return tableName;
    }

    public List<ColumnDef> getColumns() {
        return columns;
    }
}


