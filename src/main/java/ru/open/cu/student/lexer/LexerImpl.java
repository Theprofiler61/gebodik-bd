package ru.open.cu.student.lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.open.cu.student.lexer.Token.TokenType.*;

public class LexerImpl implements Lexer {
    private static final Map<String, Token.TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("SELECT", SELECT);
        KEYWORDS.put("FROM", FROM);
        KEYWORDS.put("WHERE", WHERE);
        KEYWORDS.put("AND", AND);
        KEYWORDS.put("OR", OR);
        KEYWORDS.put("AS", AS);

        KEYWORDS.put("CREATE", CREATE);
        KEYWORDS.put("TABLE", TABLE);
        KEYWORDS.put("INSERT", INSERT);
        KEYWORDS.put("INTO", INTO);
        KEYWORDS.put("VALUES", VALUES);
        KEYWORDS.put("INDEX", INDEX);
        KEYWORDS.put("ON", ON);
        KEYWORDS.put("USING", USING);
        KEYWORDS.put("HASH", HASH);
        KEYWORDS.put("BTREE", BTREE);
    }

    @Override
    public List<Token> tokenize(String sql) {
        List<Token> tokens = new ArrayList<>();
        if (sql == null) return tokens;
        int i = 0;
        int n = sql.length();
        while (i < n) {
            char c = sql.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == ',') {
                tokens.add(new Token(COMMA, ",", i++));
                continue;
            }
            if (c == ';') {
                tokens.add(new Token(SEMICOLON, ";", i++));
                continue;
            }
            if (c == '.') {
                tokens.add(new Token(DOT, ".", i++));
                continue;
            }
            if (c == '*') {
                tokens.add(new Token(STAR, "*", i++));
                continue;
            }
            if (c == '+') {
                tokens.add(new Token(PLUS, "+", i++));
                continue;
            }
            if (c == '-') {
                tokens.add(new Token(MINUS, "-", i++));
                continue;
            }
            if (c == '/') {
                tokens.add(new Token(SLASH, "/", i++));
                continue;
            }
            if (c == '(') {
                tokens.add(new Token(LPAREN, "(", i++));
                continue;
            }
            if (c == ')') {
                tokens.add(new Token(RPAREN, ")", i++));
                continue;
            }
            if (c == '\'' ) {
                int start = i++;
                StringBuilder sb = new StringBuilder();
                while (i < n) {
                    char ch = sql.charAt(i++);
                    if (ch == '\'') {
                        if (i < n && sql.charAt(i) == '\'') {
                            sb.append('\'');
                            i++;
                        } else {
                            break;
                        }
                    } else {
                        sb.append(ch);
                    }
                }
                if (i > n || sql.charAt(i - 1) != '\'') {
                    throw new IllegalArgumentException("Unterminated string literal at position " + start);
                }
                tokens.add(new Token(STRING, sb.toString(), start));
                continue;
            }
            if (Character.isDigit(c)) {
                int start = i;
                while (i < n && Character.isDigit(sql.charAt(i))) i++;
                tokens.add(new Token(NUMBER, sql.substring(start, i), start));
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                int start = i;
                i++;
                while (i < n) {
                    char ch = sql.charAt(i);
                    if (Character.isLetterOrDigit(ch) || ch == '_') {
                        i++;
                    } else {
                        break;
                    }
                }
                String word = sql.substring(start, i);
                Token.TokenType kw = KEYWORDS.get(word.toUpperCase());
                if (kw != null) {
                    tokens.add(new Token(kw, word, start));
                } else {
                    tokens.add(new Token(IDENT, word, start));
                }
                continue;
            }
            if (c == '=') {
                tokens.add(new Token(EQ, "=", i++));
                continue;
            }
            if (c == '!') {
                int start = i++;
                if (i < n && sql.charAt(i) == '=') {
                    i++;
                    tokens.add(new Token(NEQ, "!=", start));
                    continue;
                }
                throw new IllegalArgumentException("Unexpected '!' at position " + start);
            }
            if (c == '>') {
                int start = i++;
                if (i < n && sql.charAt(i) == '=') {
                    i++;
                    tokens.add(new Token(GTE, ">=", start));
                } else {
                    tokens.add(new Token(GT, ">", start));
                }
                continue;
            }
            if (c == '<') {
                int start = i++;
                if (i < n && sql.charAt(i) == '=') {
                    i++;
                    tokens.add(new Token(LTE, "<=", start));
                } else {
                    tokens.add(new Token(LT, "<", start));
                }
                continue;
            }
            throw new IllegalArgumentException("Unexpected character '" + c + "' at position " + i);
        }
        return tokens;
    }
}
