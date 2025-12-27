package ru.open.cu.student.parser.nodes;

public class SelectStmt implements AstNode {
    private final java.util.List<ResTarget> targetList;
    private final java.util.List<RangeVar> fromClause;
    private final AstNode whereClause;

    public SelectStmt(java.util.List<ResTarget> targetList,
                      java.util.List<RangeVar> fromClause,
                      AstNode whereClause) {
        this.targetList = targetList;
        this.fromClause = fromClause;
        this.whereClause = whereClause;
    }

    public java.util.List<ResTarget> getTargetList() {
        return targetList;
    }

    public java.util.List<RangeVar> getFromClause() {
        return fromClause;
    }

    public AstNode getWhereClause() {
        return whereClause;
    }
}
