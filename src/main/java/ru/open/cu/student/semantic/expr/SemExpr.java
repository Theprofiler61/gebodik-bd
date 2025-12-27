package ru.open.cu.student.semantic.expr;

public sealed interface SemExpr permits SemConst, SemColumnRef, SemBinaryExpr {
}


