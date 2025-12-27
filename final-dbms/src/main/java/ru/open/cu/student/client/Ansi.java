package ru.open.cu.student.client;

final class Ansi {
    private Ansi() {
    }

    private static volatile Boolean forcedEnabled;

    static final String RESET = "\u001B[0m";
    static final String BOLD = "\u001B[1m";
    static final String DIM = "\u001B[2m";

    static final String RED = "\u001B[31m";
    static final String GREEN = "\u001B[32m";
    static final String YELLOW = "\u001B[33m";
    static final String BLUE = "\u001B[34m";
    static final String CYAN = "\u001B[36m";
    static final String GRAY = "\u001B[90m";

    static boolean enabled() {
        if (forcedEnabled != null) return forcedEnabled;
        return System.getenv("NO_COLOR") == null;
    }

    static void setEnabled(Boolean enabled) {
        forcedEnabled = enabled;
    }

    static String wrap(String prefix, String s) {
        if (!enabled()) return s;
        return prefix + s + RESET;
    }

    static String bold(String s) {
        return wrap(BOLD, s);
    }

    static String dim(String s) {
        return wrap(DIM, s);
    }

    static String green(String s) {
        return wrap(GREEN, s);
    }

    static String yellow(String s) {
        return wrap(YELLOW, s);
    }

    static String red(String s) {
        return wrap(RED, s);
    }

    static String cyan(String s) {
        return wrap(CYAN, s);
    }

    static String blue(String s) {
        return wrap(BLUE, s);
    }

    static String gray(String s) {
        return wrap(GRAY, s);
    }
}


