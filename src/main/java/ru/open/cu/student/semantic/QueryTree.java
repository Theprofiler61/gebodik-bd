package ru.open.cu.student.semantic;

import ru.open.cu.student.catalog.model.ColumnDefinition;
import ru.open.cu.student.catalog.model.TableDefinition;
import ru.open.cu.student.index.IndexType;
import ru.open.cu.student.semantic.expr.SemExpr;

import java.util.List;

public sealed interface QueryTree permits
        QueryTree.CreateTable,
        QueryTree.CreateIndex,
        QueryTree.Insert,
        QueryTree.Select {

    QueryType type();

    record CreateTable(String tableName, List<ColumnDefinition> columns) implements QueryTree {
        @Override
        public QueryType type() {
            return QueryType.CREATE_TABLE;
        }
    }

    record CreateIndex(String indexName, TableDefinition table, ColumnDefinition column, IndexType indexType) implements QueryTree {
        @Override
        public QueryType type() {
            return QueryType.CREATE_INDEX;
        }
    }

    record Insert(TableDefinition table, List<Object> values) implements QueryTree {
        @Override
        public QueryType type() {
            return QueryType.INSERT;
        }
    }

    record SelectItem(String name, SemExpr expr) {
    }

    record Select(TableDefinition table, List<SelectItem> targets, SemExpr where) implements QueryTree {
        @Override
        public QueryType type() {
            return QueryType.SELECT;
        }
    }
}
