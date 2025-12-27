package ru.open.cu.student.parser.nodes;

public class ResTarget implements AstNode {
    private final AstNode expr;
    private final String alias;
    private final boolean star;

    public ResTarget(AstNode expr, String alias) {
        this.expr = expr;
        this.alias = alias;
        this.star = false;
    }

    private ResTarget(ColumnRef columnRef, String alias, boolean star) {
        this.expr = columnRef;
        this.alias = alias;
        this.star = star;
    }

    public static ResTarget star() {
        return new ResTarget(null, null, true);
    }

    public AstNode getExpr() {
        return expr;
    }

    public String getAlias() {
        return alias;
    }

    public boolean isStar() {
        return star;
    }
}
