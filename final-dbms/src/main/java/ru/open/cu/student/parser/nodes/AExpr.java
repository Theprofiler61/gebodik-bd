package ru.open.cu.student.parser.nodes;

public class AExpr implements AstNode {
    public enum Op { EQ, GT, LT, GTE, LTE, NEQ, AND, OR, ADD, SUB, MUL, DIV }

    private final Op op;
    private final AstNode left;
    private final AstNode right;

    public AExpr(Op op, AstNode left, AstNode right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }

    public Op getOp() {
        return op;
    }

    public AstNode getLeft() {
        return left;
    }

    public AstNode getRight() {
        return right;
    }
}
