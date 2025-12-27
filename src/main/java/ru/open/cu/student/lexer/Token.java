package ru.open.cu.student.lexer;

public class Token {
    public enum TokenType {
        SELECT, FROM, WHERE, AND, OR, AS,
        CREATE, TABLE, INSERT, INTO, VALUES,
        INDEX, ON, USING,
        HASH, BTREE,

        IDENT, NUMBER, STRING,
        COMMA, SEMICOLON, DOT,
        LPAREN, RPAREN,
        EQ, GT, LT, GTE, LTE, NEQ,
        STAR,
        PLUS, MINUS, SLASH
    }

    private final TokenType type;
    private final String text;
    private final int position;

    public Token(TokenType type, String text, int position) {
        this.type = type;
        this.text = text;
        this.position = position;
    }

    public TokenType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return type + (text != null ? "(" + text + ")" : "");
    }
}
