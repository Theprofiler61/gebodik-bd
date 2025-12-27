package ru.open.cu.student.semantic.expr;

import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;

public record SemColumnRef(TableDefinition table, ColumnDefinition column) implements SemExpr {
}


