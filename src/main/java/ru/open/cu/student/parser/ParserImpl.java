package ru.open.cu.student.parser;

import ru.open.cu.student.lexer.Token;
import ru.open.cu.student.parser.nodes.*;

import java.util.ArrayList;
import java.util.List;

import static ru.open.cu.student.lexer.Token.TokenType.*;

public class ParserImpl implements Parser {
    private List<Token> tokens;
    private int pos;

    @Override
    public AstNode parse(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
        if (peekType(SELECT)) {
            return parseSelect();
        }
        if (peekType(CREATE)) {
            return parseCreate();
        }
        if (peekType(INSERT)) {
            return parseInsert();
        }
        throw error("Expected SELECT, CREATE or INSERT");
    }

    private AstNode parseCreate() {
        expect(CREATE);

        if (match(TABLE)) {
            return parseCreateTableRest();
        }
        if (match(INDEX)) {
            return parseCreateIndexRest();
        }
        throw error("Expected TABLE or INDEX after CREATE");
    }

    private CreateTableStmt parseCreateTableRest() {
        Token tableName = expect(IDENT);
        expect(LPAREN);
        List<ColumnDef> columns = new ArrayList<>();
        columns.add(parseColumnDef());
        while (match(COMMA)) {
            columns.add(parseColumnDef());
        }
        expect(RPAREN);
        if (peekType(SEMICOLON)) consume();
        return new CreateTableStmt(tableName.getText(), columns);
    }

    private ColumnDef parseColumnDef() {
        Token name = expect(IDENT);
        Token type = expect(IDENT);
        return new ColumnDef(name.getText(), type.getText());
    }

    private CreateIndexStmt parseCreateIndexRest() {
        Token indexName = expect(IDENT);
        expect(ON);
        Token tableName = expect(IDENT);
        expect(LPAREN);
        Token columnName = expect(IDENT);
        expect(RPAREN);
        expect(USING);

        Token typeTok = consume();
        if (!(typeTok.getType() == HASH || typeTok.getType() == BTREE || typeTok.getType() == IDENT)) {
            throw error("Expected index type (HASH|BTREE)");
        }
        if (peekType(SEMICOLON)) consume();
        return new CreateIndexStmt(indexName.getText(), tableName.getText(), columnName.getText(), typeTok.getText());
    }

    private InsertStmt parseInsert() {
        expect(INSERT);
        match(INTO);
        Token tableName = expect(IDENT);
        expect(VALUES);
        expect(LPAREN);
        List<AstNode> values = new ArrayList<>();
        values.add(parseLiteralConst());
        while (match(COMMA)) {
            values.add(parseLiteralConst());
        }
        expect(RPAREN);
        if (peekType(SEMICOLON)) consume();
        return new InsertStmt(tableName.getText(), values);
    }

    private SelectStmt parseSelect() {
        expect(SELECT);
        List<ResTarget> targets = parseTargetList();
        expect(FROM);
        List<RangeVar> from = parseFromList();
        AstNode where = null;
        if (match(WHERE)) {
            where = parseOrExpr();
        }
        if (peekType(SEMICOLON)) {
            consume();
        }
        return new SelectStmt(targets, from, where);
    }

    private List<ResTarget> parseTargetList() {
        List<ResTarget> list = new ArrayList<>();
        list.add(parseResTarget());
        while (match(COMMA)) {
            list.add(parseResTarget());
        }
        return list;
    }

    private ResTarget parseResTarget() {
        if (match(STAR)) {
            return ResTarget.star();
        }
        AstNode expr = parseOrExpr();
        String alias = null;
        if (match(AS)) {
            Token t = expect(IDENT);
            alias = t.getText();
        }
        return new ResTarget(expr, alias);
    }

    private List<RangeVar> parseFromList() {
        List<RangeVar> list = new ArrayList<>();
        list.add(parseRangeVar());
        while (match(COMMA)) {
            list.add(parseRangeVar());
        }
        return list;
    }

    private RangeVar parseRangeVar() {
        Token tbl = expect(IDENT);
        String alias = null;
        if (peekType(IDENT)) {
            alias = consume().getText();
        }
        return new RangeVar(tbl.getText(), alias);
    }

    private ColumnRef parseColumnRef() {
        Token first = expect(IDENT);
        if (match(DOT)) {
            Token second = expect(IDENT);
            return new ColumnRef(first.getText(), second.getText());
        }
        return new ColumnRef(null, first.getText());
    }

    private AstNode parseOrExpr() {
        AstNode left = parseAndExpr();
        while (true) {
            if (match(OR)) {
                AstNode right = parseAndExpr();
                left = new AExpr(AExpr.Op.OR, left, right);
            } else {
                return left;
            }
        }
    }

    private AstNode parseAndExpr() {
        AstNode left = parseCmpExpr();
        while (true) {
            if (match(AND)) {
                AstNode right = parseCmpExpr();
                left = new AExpr(AExpr.Op.AND, left, right);
            } else {
                return left;
            }
        }
    }

    private AstNode parseCmpExpr() {
        AstNode left = parseAddExpr();
        if (peekType(EQ) || peekType(GT) || peekType(LT) || peekType(GTE) || peekType(LTE) || peekType(NEQ)) {
            Token op = consume();
            AstNode right = parseAddExpr();
            return new AExpr(mapCmp(op), left, right);
        }
        return left;
    }

    private AstNode parseAddExpr() {
        AstNode left = parseMulExpr();
        while (true) {
            if (match(PLUS)) {
                AstNode right = parseMulExpr();
                left = new AExpr(AExpr.Op.ADD, left, right);
                continue;
            }
            if (match(MINUS)) {
                AstNode right = parseMulExpr();
                left = new AExpr(AExpr.Op.SUB, left, right);
                continue;
            }
            return left;
        }
    }

    private AstNode parseMulExpr() {
        AstNode left = parseSimplePrimary();
        while (true) {
            if (match(STAR)) {
                AstNode right = parseSimplePrimary();
                left = new AExpr(AExpr.Op.MUL, left, right);
                continue;
            }
            if (match(SLASH)) {
                AstNode right = parseSimplePrimary();
                left = new AExpr(AExpr.Op.DIV, left, right);
                continue;
            }
            return left;
        }
    }

    private AstNode parseSimplePrimary() {
        if (peekType(NUMBER)) {
            Token t = consume();
            return new AConst(AConst.ConstType.NUMBER, t.getText());
        }
        if (peekType(STRING)) {
            Token t = consume();
            return new AConst(AConst.ConstType.STRING, t.getText());
        }
        if (peekType(IDENT)) {
            return parseColumnRef();
        }
        if (match(LPAREN)) {
            AstNode inner = parseOrExpr();
            expect(RPAREN);
            return inner;
        }
        throw error("Unexpected token in expression");
    }

    private AstNode parseLiteralConst() {
        if (peekType(NUMBER)) {
            Token t = consume();
            return new AConst(AConst.ConstType.NUMBER, t.getText());
        }
        if (peekType(STRING)) {
            Token t = consume();
            return new AConst(AConst.ConstType.STRING, t.getText());
        }
        throw error("Expected literal (NUMBER or STRING)");
    }

    private AExpr.Op mapCmp(Token op) {
        switch (op.getType()) {
            case EQ: return AExpr.Op.EQ;
            case GT: return AExpr.Op.GT;
            case LT: return AExpr.Op.LT;
            case GTE: return AExpr.Op.GTE;
            case LTE: return AExpr.Op.LTE;
            case NEQ: return AExpr.Op.NEQ;
            default: throw error("Unsupported operator: " + op);
        }
    }

    private boolean match(Token.TokenType type) {
        if (peekType(type)) {
            pos++;
            return true;
        }
        return false;
    }

    private Token expect(Token.TokenType type) {
        if (!peekType(type)) {
            throw error("Expected " + type + ", found " + (eof() ? "<eof>" : tokens.get(pos).getType()));
        }
        return tokens.get(pos++);
    }

    private boolean peekType(Token.TokenType type) {
        return !eof() && tokens.get(pos).getType() == type;
    }

    private Token consume() {
        if (eof()) throw error("Unexpected <eof>");
        return tokens.get(pos++);
    }

    private boolean eof() {
        return pos >= tokens.size();
    }

    private IllegalArgumentException error(String msg) {
        return new IllegalArgumentException("Parse error at position " + (eof() ? -1 : tokens.get(pos).getPosition()) + ": " + msg);
    }
}
