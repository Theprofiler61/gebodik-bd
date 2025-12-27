package ru.open.cu.student.client;

import java.util.List;

final class CliRenderer {
    private static final int MAX_COL_WIDTH = 42;

    private CliRenderer() {
    }

    static void printWelcome(String host, int port, String trustStorePath) {
        System.out.println();
        System.out.println(Ansi.cyan(Ansi.bold(bigBanner())));
        System.out.println(Ansi.dim("Подключение: ") + Ansi.bold(host + ":" + port));
        if (trustStorePath != null) {
            System.out.println(Ansi.dim("TrustStore:  ") + trustStorePath);
        }
        System.out.println(Ansi.dim("Команды: ") + Ansi.bold("\\h") + " help, " + Ansi.bold("\\q") + " exit, " + Ansi.bold("\\raw") + " toggle raw-json");
        System.out.println();
    }

    static String prompt(String host, int port, boolean continuation) {
        String base = continuation ? "... " : "db ";
        String p = base + Ansi.dim("(" + host + ":" + port + ")") + "> ";
        return Ansi.enabled() ? Ansi.green(p) : (base + "(" + host + ":" + port + ")> ");
    }

    static void printResponse(String json, long elapsedMs, boolean raw) {
        DbResponseView r;
        try {
            r = DbResponseView.parse(json);
        } catch (Exception e) {
            System.out.println(Ansi.yellow("! ") + "Не смог распарсить ответ сервера, печатаю как есть:");
            System.out.println(json);
            return;
        }

        if (!r.ok()) {
            String stage = r.errorStage() == null ? "UNKNOWN" : r.errorStage();
            String msg = r.errorMessage() == null ? "Неизвестная ошибка" : r.errorMessage();
            System.out.println(Ansi.red(Ansi.bold("✖ ОШИБКА")) + " " + Ansi.dim("[" + stage + "]") + " " + msg);
            if (raw) {
                System.out.println(Ansi.dim("raw: ") + json);
            }
            return;
        }

        List<String> cols = r.columns();
        List<List<Object>> rows = r.rows();
        if (cols == null || cols.isEmpty()) {
            System.out.println(Ansi.green(Ansi.bold("✔ OK")) + Ansi.dim(" (" + elapsedMs + " ms)"));
            if (raw) {
                System.out.println(Ansi.dim("raw: ") + json);
            }
            return;
        }

        System.out.println(PrettyTable.render(cols, rows, MAX_COL_WIDTH));
        System.out.println(Ansi.green(Ansi.bold("✔ OK")) + Ansi.dim("  rows=" + rows.size() + "  time=" + elapsedMs + " ms"));
        if (raw) {
            System.out.println(Ansi.dim("raw: ") + json);
        }
    }

    static void printHelp() {
        System.out.println();
        System.out.println(Ansi.blue(Ansi.bold(box(List.of(
                "Команды",
                "",
                "  \\q / exit / quit      выйти",
                "  \\h / help             помощь",
                "  \\raw                 показать/скрыть raw JSON ответа",
                "  \\color on|off         включить/выключить цвета"
        ), 2, 1))));
        System.out.println();
        System.out.println(Ansi.blue(Ansi.bold(box(List.of(
                "SQL",
                "",
                "  Запрос отправляется, когда в строке встретится ';'",
                "  Можно набирать много строк (будет prompt '...')"
        ), 2, 1))));
        System.out.println();
    }

    private static String bigBanner() {
        return box(List.of(
                "  ██████╗ ███████╗██████╗  ██████╗ ██████╗ ██╗██╗  ██╗",
                " ██╔════╝ ██╔════╝██╔══██╗██╔═══██╗██╔══██╗██║██║ ██╔╝",
                " ██║  ███╗█████╗  ██████╔╝██║   ██║██║  ██║██║█████╔╝ ",
                " ██║   ██║██╔══╝  ██╔══██╗██║   ██║██║  ██║██║██╔═██╗ ",
                " ╚██████╔╝███████╗██████╔╝╚██████╔╝██████╔╝██║██║  ██╗",
                "  ╚═════╝ ╚══════╝╚═════╝  ╚═════╝ ╚═════╝ ╚═╝╚═╝  ╚═╝",
                "",
                "            G E B O D I K   C L I   A C T I V A T E D"
        ), 2, 1);
    }

    private static String box(List<String> lines, int padX, int padY) {
        int max = 0;
        for (String l : lines) {
            int len = l == null ? 0 : l.length();
            if (len > max) max = len;
        }
        int innerW = max + padX * 2;
        String top = "╔" + "═".repeat(innerW) + "╗";
        String midSep = "║";
        String bot = "╚" + "═".repeat(innerW) + "╝";

        String padLeft = " ".repeat(padX);
        StringBuilder sb = new StringBuilder();
        sb.append(top).append('\n');
        for (int i = 0; i < padY; i++) {
            sb.append(midSep).append(" ".repeat(innerW)).append(midSep).append('\n');
        }
        for (String l : lines) {
            String line = l == null ? "" : l;
            sb.append(midSep)
                    .append(padLeft)
                    .append(line);
            int rest = max - line.length();
            if (rest > 0) sb.append(" ".repeat(rest));
            sb.append(padLeft)
                    .append(midSep)
                    .append('\n');
        }
        for (int i = 0; i < padY; i++) {
            sb.append(midSep).append(" ".repeat(innerW)).append(midSep).append('\n');
        }
        sb.append(bot);
        return sb.toString();
    }
}


