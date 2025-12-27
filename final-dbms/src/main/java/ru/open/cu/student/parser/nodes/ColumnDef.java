package ru.open.cu.student.parser.nodes;

public class ColumnDef implements AstNode {
    private final String name;
    private final String typeName;

    public ColumnDef(String name, String typeName) {
        this.name = name;
        this.typeName = typeName;
    }

    public String getName() {
        return name;
    }

    public String getTypeName() {
        return typeName;
    }
}


