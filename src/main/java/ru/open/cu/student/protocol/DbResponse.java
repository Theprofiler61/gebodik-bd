package ru.open.cu.student.protocol;

import java.util.List;

public record DbResponse(boolean ok, List<String> columns, List<List<Object>> rows, DbError error) {
    public static DbResponse ok(List<String> columns, List<List<Object>> rows) {
        return new DbResponse(true, columns, rows, null);
    }

    public static DbResponse error(String stage, String message) {
        return new DbResponse(false, List.of(), List.of(), new DbError(stage, message));
    }

    public String toJson() {
        if (ok) {
            return "{"
                    + "\"ok\":true,"
                    + "\"columns\":" + JsonUtil.stringArray(columns) + ","
                    + "\"rows\":" + JsonUtil.rowsArray(rows)
                    + "}";
        }
        return "{"
                + "\"ok\":false,"
                + "\"error\":{"
                + "\"stage\":" + JsonUtil.quote(error.stage()) + ","
                + "\"message\":" + JsonUtil.quote(error.message())
                + "}"
                + "}";
    }

    public record DbError(String stage, String message) {
    }
}


