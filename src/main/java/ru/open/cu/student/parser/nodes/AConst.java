package ru.open.cu.student.parser.nodes;

public class AConst implements AstNode {
    public enum ConstType { NUMBER, STRING }

    private final ConstType type;
    private final String value;

    public AConst(ConstType type, String value) {
        this.type = type;
        this.value = value;
    }

    public ConstType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}



