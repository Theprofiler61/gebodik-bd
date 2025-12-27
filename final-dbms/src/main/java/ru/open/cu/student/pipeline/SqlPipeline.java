package ru.open.cu.student.pipeline;

import ru.open.cu.student.execution.Executor;
import ru.open.cu.student.execution.ExecutorFactory;
import ru.open.cu.student.execution.QueryExecutionEngine;
import ru.open.cu.student.lexer.Lexer;
import ru.open.cu.student.lexer.Token;
import ru.open.cu.student.optimizer.Optimizer;
import ru.open.cu.student.optimizer.node.PhysicalPlanNode;
import ru.open.cu.student.parser.Parser;
import ru.open.cu.student.parser.nodes.AstNode;
import ru.open.cu.student.planner.Planner;
import ru.open.cu.student.planner.node.LogicalPlanNode;
import ru.open.cu.student.semantic.QueryTree;
import ru.open.cu.student.semantic.SemanticAnalyzer;
import ru.open.cu.student.storage.Row;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SqlPipeline {
    private final Lexer lexer;
    private final Parser parser;
    private final SemanticAnalyzer semanticAnalyzer;
    private final Planner planner;
    private final Optimizer optimizer;
    private final ExecutorFactory executorFactory;
    private final QueryExecutionEngine executionEngine;
    private final PipelineLogger logger;

    public SqlPipeline(Lexer lexer,
                       Parser parser,
                       SemanticAnalyzer semanticAnalyzer,
                       Planner planner,
                       Optimizer optimizer,
                       ExecutorFactory executorFactory,
                       QueryExecutionEngine executionEngine,
                       PipelineLogger logger) {
        this.lexer = Objects.requireNonNull(lexer, "lexer");
        this.parser = Objects.requireNonNull(parser, "parser");
        this.semanticAnalyzer = Objects.requireNonNull(semanticAnalyzer, "semanticAnalyzer");
        this.planner = Objects.requireNonNull(planner, "planner");
        this.optimizer = Objects.requireNonNull(optimizer, "optimizer");
        this.executorFactory = Objects.requireNonNull(executorFactory, "executorFactory");
        this.executionEngine = Objects.requireNonNull(executionEngine, "executionEngine");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public QueryResult execute(String sql, ru.open.cu.student.catalog.manager.CatalogManager catalog) {
        try {
            List<Token> tokens = runStage(PipelineStage.LEXER, () -> lexer.tokenize(sql));
            logger.logObject(PipelineStage.LEXER, tokens);

            AstNode ast = runStage(PipelineStage.PARSER, () -> parser.parse(tokens));
            logger.log(PipelineStage.PARSER, AstPrinter.print(ast));

            QueryTree queryTree = runStage(PipelineStage.SEMANTIC, () -> semanticAnalyzer.analyze(ast, catalog));
            logger.logObject(PipelineStage.SEMANTIC, queryTree);

            LogicalPlanNode logicalPlan = runStage(PipelineStage.PLANNER, () -> planner.plan(queryTree));
            logger.logObject(PipelineStage.PLANNER, logicalPlan);

            PhysicalPlanNode physicalPlan = runStage(PipelineStage.OPTIMIZER, () -> optimizer.optimize(logicalPlan));
            logger.logObject(PipelineStage.OPTIMIZER, physicalPlan);

            Executor executor = runStage(PipelineStage.EXECUTOR_FACTORY, () -> executorFactory.createExecutor(physicalPlan));
            logger.log(PipelineStage.EXECUTOR_FACTORY, executor.getClass().getSimpleName());

            List<Object> results = runStage(PipelineStage.EXECUTION, () -> executionEngine.execute(executor));

            QueryResult out;
            if (queryTree instanceof QueryTree.Select sel) {
                List<String> columns = sel.targets().stream().map(t -> t.name()).toList();
                List<List<Object>> rows = new ArrayList<>();
                for (Object r : results) {
                    Row row = (Row) r;
                    rows.add(row.values());
                }
                out = new QueryResult(columns, rows);
            } else {
                out = QueryResult.empty();
            }
            logger.logObject(PipelineStage.RESULT, out);
            return out;
        } catch (PipelineException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new PipelineException(PipelineStage.EXECUTION, e);
        }
    }

    private <T> T runStage(PipelineStage stage, StageCall<T> call) {
        try {
            return call.call();
        } catch (PipelineException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new PipelineException(stage, e);
        }
    }

    @FunctionalInterface
    private interface StageCall<T> {
        T call();
    }
}


