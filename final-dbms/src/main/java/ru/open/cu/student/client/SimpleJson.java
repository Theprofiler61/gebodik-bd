package ru.open.cu.student.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SimpleJson {
    private SimpleJson() {
    }

    static Object parse(String json) {
        if (json == null) throw new IllegalArgumentException("json is null");
        Parser p = new Parser(json);
        Object v = p.parseValue();
        p.skipWs();
        if (!p.eof()) {
            throw new IllegalArgumentException("Trailing characters at position " + p.pos);
        }
        return v;
    }

    private static final class Parser {
        private final String s;
        private int pos;

        private Parser(String s) {
            this.s = s;
        }

        private boolean eof() {
            return pos >= s.length();
        }

        private char peek() {
            return eof() ? '\0' : s.charAt(pos);
        }

        private char next() {
            if (eof()) throw error("Unexpected end of input");
            return s.charAt(pos++);
        }

        private void expect(char c) {
            char got = next();
            if (got != c) {
                throw error("Expected '" + c + "', got '" + got + "'");
            }
        }

        private void skipWs() {
            while (!eof()) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                    pos++;
                } else {
                    return;
                }
            }
        }

        private IllegalArgumentException error(String msg) {
            return new IllegalArgumentException("JSON parse error at position " + pos + ": " + msg);
        }

        private Object parseValue() {
            skipWs();
            char c = peek();
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            if (c == '-' || (c >= '0' && c <= '9')) return parseNumber();
            throw error("Unexpected character '" + c + "'");
        }

        private Map<String, Object> parseObject() {
            expect('{');
            skipWs();
            Map<String, Object> out = new LinkedHashMap<>();
            if (peek() == '}') {
                pos++;
                return out;
            }
            while (true) {
                skipWs();
                String key = parseString();
                skipWs();
                expect(':');
                Object value = parseValue();
                out.put(key, value);
                skipWs();
                char c = next();
                if (c == '}') {
                    return out;
                }
                if (c != ',') {
                    throw error("Expected ',' or '}', got '" + c + "'");
                }
            }
        }

        private List<Object> parseArray() {
            expect('[');
            skipWs();
            List<Object> out = new ArrayList<>();
            if (peek() == ']') {
                pos++;
                return out;
            }
            while (true) {
                Object v = parseValue();
                out.add(v);
                skipWs();
                char c = next();
                if (c == ']') {
                    return out;
                }
                if (c != ',') {
                    throw error("Expected ',' or ']', got '" + c + "'");
                }
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (eof()) throw error("Unterminated string");
                char c = next();
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (eof()) throw error("Unterminated escape");
                    char e = next();
                    switch (e) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u': {
                            if (pos + 4 > s.length()) throw error("Bad unicode escape");
                            int code = Integer.parseInt(s.substring(pos, pos + 4), 16);
                            sb.append((char) code);
                            pos += 4;
                            break;
                        }
                        default:
                            throw error("Bad escape '\\" + e + "'");
                    }
                } else {
                    sb.append(c);
                }
            }
        }

        private Boolean parseBoolean() {
            if (s.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (s.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw error("Invalid boolean literal");
        }

        private Object parseNull() {
            if (s.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw error("Invalid null literal");
        }

        private Number parseNumber() {
            int start = pos;
            if (peek() == '-') pos++;
            while (!eof()) {
                char c = peek();
                if (c >= '0' && c <= '9') {
                    pos++;
                } else {
                    break;
                }
            }
            boolean isFloat = false;
            if (!eof() && peek() == '.') {
                isFloat = true;
                pos++;
                while (!eof()) {
                    char c = peek();
                    if (c >= '0' && c <= '9') pos++; else break;
                }
            }
            if (!eof()) {
                char c = peek();
                if (c == 'e' || c == 'E') {
                    isFloat = true;
                    pos++;
                    if (peek() == '+' || peek() == '-') pos++;
                    while (!eof()) {
                        char d = peek();
                        if (d >= '0' && d <= '9') pos++; else break;
                    }
                }
            }
            String num = s.substring(start, pos);
            try {
                if (isFloat) return Double.parseDouble(num);
                return Long.parseLong(num);
            } catch (NumberFormatException e) {
                throw error("Invalid number: " + num);
            }
        }
    }
}


