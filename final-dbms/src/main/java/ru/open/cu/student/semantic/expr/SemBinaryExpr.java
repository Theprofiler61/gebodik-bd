package ru.open.cu.student.semantic.expr;

public record SemBinaryExpr(Op op, SemExpr left, SemExpr right) implements SemExpr {
    public enum Op { EQ, GT, LT, GTE, LTE, NEQ, AND, OR, ADD, SUB, MUL, DIV }
}


