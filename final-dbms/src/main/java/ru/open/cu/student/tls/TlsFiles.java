package ru.open.cu.student.tls;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Objects;

public final class TlsFiles {
    private TlsFiles() {
    }

    public record Contexts(SSLContext server, SSLContext client) {
    }

    public static Contexts loadOrCreate(Path keyStorePath, Path trustStorePath, char[] password, String commonName) {
        Objects.requireNonNull(keyStorePath, "keyStorePath");
        Objects.requireNonNull(trustStorePath, "trustStorePath");
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(commonName, "commonName");

        try {
            if (!Files.exists(keyStorePath) || !Files.exists(trustStorePath)) {
                ensureParentDir(keyStorePath);
                ensureParentDir(trustStorePath);
                generateWithKeytool(keyStorePath, trustStorePath, password, commonName);
            }

            KeyStore ks = readKeyStore(keyStorePath, password);
            KeyStore ts = readKeyStore(trustStorePath, password);
            return new Contexts(serverContext(ks, password), clientContext(ts));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load or create TLS stores", e);
        }
    }

    public static SSLContext loadClient(Path trustStorePath, char[] password) {
        try {
            KeyStore ts = readKeyStore(trustStorePath, password);
            return clientContext(ts);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load client truststore: " + trustStorePath, e);
        }
    }

    private static KeyStore readKeyStore(Path path, char[] password) throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        try (InputStream in = Files.newInputStream(path)) {
            ks.load(in, password);
        }
        return ks;
    }

    private static SSLContext serverContext(KeyStore keyStore, char[] password) throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, null);
        return ctx;
    }

    private static SSLContext clientContext(KeyStore trustStore) throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, tmf.getTrustManagers(), null);
        return ctx;
    }

    private static void ensureParentDir(Path path) throws Exception {
        Path p = path.toAbsolutePath().normalize().getParent();
        if (p != null) {
            Files.createDirectories(p);
        }
    }

    private static void generateWithKeytool(Path keyStorePath, Path trustStorePath, char[] password, String commonName) throws Exception {
        Path certFile = trustStorePath.toAbsolutePath().normalize().getParent() == null
                ? Path.of("tls-cert.pem")
                : trustStorePath.toAbsolutePath().normalize().getParent().resolve("tls-cert.pem");

        String pass = new String(password);

        runKeytool(new String[]{
                "-genkeypair",
                "-alias", "key",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "365",
                "-keystore", keyStorePath.toAbsolutePath().normalize().toString(),
                "-storetype", "JKS",
                "-storepass", pass,
                "-keypass", pass,
                "-dname", "CN=" + commonName,
                "-ext", "SAN=DNS:localhost,IP:127.0.0.1"
        });

        runKeytool(new String[]{
                "-exportcert",
                "-rfc",
                "-alias", "key",
                "-keystore", keyStorePath.toAbsolutePath().normalize().toString(),
                "-storepass", pass,
                "-file", certFile.toAbsolutePath().normalize().toString()
        });

        runKeytool(new String[]{
                "-importcert",
                "-noprompt",
                "-alias", "cert",
                "-file", certFile.toAbsolutePath().normalize().toString(),
                "-keystore", trustStorePath.toAbsolutePath().normalize().toString(),
                "-storetype", "JKS",
                "-storepass", pass
        });

        Files.deleteIfExists(certFile);
    }

    private static void runKeytool(String[] args) throws Exception {
        String javaHome = System.getProperty("java.home");
        Path keytool = Path.of(javaHome, "bin", "keytool");
        if (!Files.exists(keytool)) {
            keytool = Path.of(javaHome, "..", "bin", "keytool").normalize();
        }
        if (!Files.exists(keytool)) {
            throw new IllegalStateException("keytool not found under java.home=" + javaHome);
        }

        String[] cmd = new String[args.length + 1];
        cmd[0] = keytool.toAbsolutePath().normalize().toString();
        System.arraycopy(args, 0, cmd, 1, args.length);

        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        String out;
        try (InputStream in = p.getInputStream()) {
            out = new String(in.readAllBytes());
        }
        int code = p.waitFor();
        if (code != 0) {
            throw new IllegalStateException("keytool failed: exit=" + code + "\n" + out);
        }
    }
}


