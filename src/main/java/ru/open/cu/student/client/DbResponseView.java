package ru.open.cu.student.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

record DbResponseView(
        boolean ok,
        List<String> columns,
        List<List<Object>> rows,
        String errorStage,
        String errorMessage
) {
    static DbResponseView parse(String json) {
        Object root = SimpleJson.parse(json);
        if (!(root instanceof Map<?, ?> m)) {
            throw new IllegalArgumentException("Response is not a JSON object");
        }

        boolean ok = boolField(m, "ok");
        if (ok) {
            List<String> columns = stringListField(m, "columns");
            List<List<Object>> rows = rowsField(m, "rows");
            return new DbResponseView(true, columns, rows, null, null);
        }

        Object err = m.get("error");
        if (!(err instanceof Map<?, ?> em)) {
            return new DbResponseView(false, List.of(), List.of(), "UNKNOWN", "Unknown error");
        }
        String stage = stringField(em, "stage");
        String msg = stringField(em, "message");
        return new DbResponseView(false, List.of(), List.of(), stage, msg);
    }

    private static boolean boolField(Map<?, ?> m, String key) {
        Object v = m.get(key);
        if (v instanceof Boolean b) return b;
        throw new IllegalArgumentException("Field '" + key + "' is not boolean");
    }

    private static String stringField(Map<?, ?> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof String s) return s;
        throw new IllegalArgumentException("Field '" + key + "' is not string");
    }

    private static List<String> stringListField(Map<?, ?> m, String key) {
        Object v = m.get(key);
        if (!(v instanceof List<?> list)) {
            throw new IllegalArgumentException("Field '" + key + "' is not array");
        }
        List<String> out = new ArrayList<>(list.size());
        for (Object o : list) {
            if (!(o instanceof String s)) {
                throw new IllegalArgumentException("Field '" + key + "' contains non-string");
            }
            out.add(s);
        }
        return out;
    }

    private static List<List<Object>> rowsField(Map<?, ?> m, String key) {
        Object v = m.get(key);
        if (!(v instanceof List<?> list)) {
            throw new IllegalArgumentException("Field '" + key + "' is not array");
        }
        List<List<Object>> out = new ArrayList<>(list.size());
        for (Object row : list) {
            if (!(row instanceof List<?> rowList)) {
                throw new IllegalArgumentException("Row is not array");
            }
            out.add(new ArrayList<>(rowList));
        }
        return out;
    }
}


