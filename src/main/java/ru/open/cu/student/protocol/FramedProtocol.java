package ru.open.cu.student.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class FramedProtocol {
    private FramedProtocol() {
    }

    public static String readFrame(DataInputStream in) throws IOException {
        try {
            int len = in.readInt();
            if (len < 0 || len > 64 * 1024 * 1024) {
                throw new IOException("Invalid frame length: " + len);
            }
            byte[] payload = new byte[len];
            in.readFully(payload);
            return new String(payload, StandardCharsets.UTF_8);
        } catch (EOFException eof) {
            return null;
        }
    }

    public static void writeFrame(DataOutputStream out, String payload) throws IOException {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
        out.flush();
    }
}


