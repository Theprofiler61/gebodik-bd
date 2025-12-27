package ru.open.cu.student.protocol;

import java.util.List;

public final class JsonUtil {
    private JsonUtil() {
    }

    public static String quote(String s) {
        if (s == null) return "null";
        return "\"" + escape(s) + "\"";
    }

    public static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    public static String value(Object v) {
        if (v == null) return "null";
        if (v instanceof Boolean b) return b ? "true" : "false";
        if (v instanceof Number n) return n.toString();
        return quote(String.valueOf(v));
    }

    public static String stringArray(List<String> strings) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < strings.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(quote(strings.get(i)));
        }
        sb.append(']');
        return sb.toString();
    }

    public static String rowsArray(List<List<Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) sb.append(',');
            List<Object> row = rows.get(i);
            sb.append('[');
            for (int j = 0; j < row.size(); j++) {
                if (j > 0) sb.append(',');
                sb.append(value(row.get(j)));
            }
            sb.append(']');
        }
        sb.append(']');
        return sb.toString();
    }
}


