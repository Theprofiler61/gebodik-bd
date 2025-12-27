package ru.open.cu.student.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class PrettyTable {
    private PrettyTable() {
    }

    static String render(List<String> headers, List<List<Object>> rows, int maxWidthPerCol) {
        Objects.requireNonNull(headers, "headers");
        Objects.requireNonNull(rows, "rows");
        if (headers.isEmpty()) {
            return Ansi.dim("<no columns>");
        }

        int cols = headers.size();
        int[] widths = new int[cols];
        for (int c = 0; c < cols; c++) {
            widths[c] = clampWidth(visibleLen(headers.get(c)), maxWidthPerCol);
        }
        for (List<Object> r : rows) {
            for (int c = 0; c < cols && c < r.size(); c++) {
                String cell = cellToString(r.get(c));
                widths[c] = Math.max(widths[c], clampWidth(visibleLen(cell), maxWidthPerCol));
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(borderTop(widths)).append('\n');
        sb.append(rowLine(headers, widths, true)).append('\n');
        sb.append(borderMid(widths)).append('\n');
        for (List<Object> r : rows) {
            List<String> cells = new ArrayList<>(cols);
            for (int c = 0; c < cols; c++) {
                cells.add(c < r.size() ? cellToString(r.get(c)) : "");
            }
            sb.append(rowLine(cells, widths, false)).append('\n');
        }
        sb.append(borderBottom(widths));
        return sb.toString();
    }

    private static String cellToString(Object v) {
        if (v == null) return "NULL";
        return String.valueOf(v);
    }

    private static int clampWidth(int width, int maxWidthPerCol) {
        if (maxWidthPerCol <= 0) return width;
        return Math.min(width, maxWidthPerCol);
    }

    private static int visibleLen(String s) {
        return s == null ? 0 : s.length();
    }

    private static String pad(String s, int width) {
        if (s == null) s = "";
        if (visibleLen(s) > width) {
            if (width <= 1) return s.substring(0, width);
            return s.substring(0, Math.max(0, width - 1)) + "…";
        }
        StringBuilder sb = new StringBuilder(width);
        sb.append(s);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }

    private static String rowLine(List<String> cells, int[] widths, boolean header) {
        StringBuilder sb = new StringBuilder();
        sb.append('│');
        for (int c = 0; c < widths.length; c++) {
            String v = c < cells.size() ? cells.get(c) : "";
            String p = pad(v, widths[c]);
            if (header) p = Ansi.bold(p);
            sb.append(' ').append(p).append(' ').append('│');
        }
        return sb.toString();
    }

    private static String borderTop(int[] widths) {
        return border('┌', '┬', '┐', widths);
    }

    private static String borderMid(int[] widths) {
        return border('├', '┼', '┤', widths);
    }

    private static String borderBottom(int[] widths) {
        return border('└', '┴', '┘', widths);
    }

    private static String border(char left, char mid, char right, int[] widths) {
        StringBuilder sb = new StringBuilder();
        sb.append(left);
        for (int c = 0; c < widths.length; c++) {
            if (c > 0) sb.append(mid);
            sb.append("─".repeat(widths[c] + 2));
        }
        sb.append(right);
        return sb.toString();
    }
}


