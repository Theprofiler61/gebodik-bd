package ru.open.cu.student.client;

import ru.open.cu.student.protocol.FramedProtocol;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class CliMain {
    public static void main(String[] args) throws Exception {
        String host = stringArg(args, "--host", "127.0.0.1");
        int port = intArg(args, "--port", 15432);
        String oneSql = stringArg(args, "--sql", null);
        boolean raw = boolFlag(args, "--raw");
        if (boolFlag(args, "--noColor")) {
            Ansi.setEnabled(false);
        }
        if (boolFlag(args, "--color")) {
            Ansi.setEnabled(true);
        }

        if (oneSql != null) {
            sendAndPrint(host, port, oneSql, raw);
            return;
        }

        CliRenderer.printWelcome(host, port, null);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        while (true) {
            System.out.print(CliRenderer.prompt(host, port, sb.length() > 0));
            String line = br.readLine();
            if (line == null) break;
            if (line.equalsIgnoreCase("\\q") || line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                break;
            }
            if (line.equalsIgnoreCase("\\h") || line.equalsIgnoreCase("help")) {
                CliRenderer.printHelp();
                continue;
            }
            if (line.equalsIgnoreCase("\\raw")) {
                raw = !raw;
                System.out.println(Ansi.dim("raw-json: ") + (raw ? Ansi.green("ON") : Ansi.yellow("OFF")));
                continue;
            }
            if (line.startsWith("\\color")) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) {
                    if (parts[1].equalsIgnoreCase("on")) {
                        Ansi.setEnabled(true);
                        System.out.println(Ansi.dim("color: ") + Ansi.green("ON"));
                        continue;
                    }
                    if (parts[1].equalsIgnoreCase("off")) {
                        Ansi.setEnabled(false);
                        System.out.println("color: OFF");
                        continue;
                    }
                }
                System.out.println("usage: \\color on|off");
                continue;
            }
            sb.append(line).append('\n');
            if (line.contains(";")) {
                String sql = sb.toString();
                sb.setLength(0);
                sendAndPrint(host, port, sql, raw);
            }
        }
    }

    private static void sendAndPrint(String host, int port, String sql, boolean raw) {
        try (Socket socket = new Socket(host, port);
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            sendAndPrintOnSocket(in, out, sql, raw);
        } catch (SocketException se) {
            System.out.println(Ansi.red(Ansi.bold("✖ CLIENT")) + " " + Ansi.dim("[NET]") + " " + se.getMessage());
            System.out.println(Ansi.dim("Подсказка: проверь, что сервер запущен и порт верный."));
        } catch (Exception e) {
            System.out.println(Ansi.red(Ansi.bold("✖ CLIENT")) + " " + Ansi.dim("[ERR]") + " " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
    }

    private static void sendAndPrintOnSocket(DataInputStream in, DataOutputStream out, String sql, boolean raw) throws Exception {
        long start = System.nanoTime();
        FramedProtocol.writeFrame(out, sql);
        String resp = FramedProtocol.readFrame(in);
        long ms = (System.nanoTime() - start) / 1_000_000L;
        if (resp == null) {
            System.out.println(Ansi.yellow("<no response>"));
        } else {
            CliRenderer.printResponse(resp, ms, raw);
        }
    }

    private static int intArg(String[] args, String key, int def) {
        String v = findArg(args, key);
        return v == null ? def : Integer.parseInt(v);
    }

    private static String stringArg(String[] args, String key, String def) {
        String v = findArg(args, key);
        return v == null ? def : v;
    }

    private static boolean boolFlag(String[] args, String key) {
        for (String a : args) {
            if (a.equals(key)) return true;
        }
        return false;
    }

    private static String findArg(String[] args, String key) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(key) && i + 1 < args.length) {
                return args[i + 1];
            }
        }
        return null;
    }
}


