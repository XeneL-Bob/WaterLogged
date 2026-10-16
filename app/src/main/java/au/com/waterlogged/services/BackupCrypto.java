package au.com.waterlogged.services;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;

import javax.crypto.*;
import javax.crypto.spec.*;

/** Versioned, authenticated chunks keep full-resolution photo backups out of the Java heap. */
public final class BackupCrypto {
    private static final byte[] MAGIC = "WLOGBAK1".getBytes(StandardCharsets.US_ASCII);
    private static final int ITERATIONS = 600_000, CHUNK = 64 * 1024;
    public static final long MAX_BYTES = 256L * 1024 * 1024;

    private BackupCrypto() {}

    public static void encrypt(InputStream input, OutputStream output, char[] password)
            throws Exception {
        byte[] salt = new byte[16], prefix = new byte[8];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        random.nextBytes(prefix);
        byte[] header =
                ByteBuffer.allocate(36).put(MAGIC).put(salt).putInt(ITERATIONS).put(prefix).array();
        SecretKeySpec key = derive(password, salt);
        DataOutputStream out = new DataOutputStream(output);
        out.write(header);
        byte[] buffer = new byte[CHUNK];
        long total = 0;
        int counter = 0;
        try {
            while (true) {
                int count = 0, n;
                while (count < buffer.length
                        && (n = input.read(buffer, count, buffer.length - count)) != -1) count += n;
                total += count;
                if (total > MAX_BYTES)
                    throw new IOException("Backup exceeds the 256 MB pilot limit.");
                byte[] encrypted =
                        cipher(Cipher.ENCRYPT_MODE, key, prefix, counter++, header)
                                .doFinal(buffer, 0, count);
                out.writeInt(encrypted.length);
                out.write(encrypted);
                if (count == 0)
                    break; // An authenticated terminator makes truncated backups fail closed.
            }
            out.flush();
        } finally {
            Arrays.fill(buffer, (byte) 0);
        }
    }

    public static void decrypt(InputStream input, OutputStream output, char[] password)
            throws Exception {
        DataInputStream in = new DataInputStream(input);
        byte[] header = new byte[36];
        in.readFully(header);
        ByteBuffer parts = ByteBuffer.wrap(header);
        byte[] magic = new byte[8], salt = new byte[16], prefix = new byte[8];
        parts.get(magic).get(salt);
        int iterations = parts.getInt();
        parts.get(prefix);
        if (!Arrays.equals(magic, MAGIC) || iterations != ITERATIONS)
            throw new IOException("Unsupported backup format.");
        SecretKeySpec key = derive(password, salt);
        long total = 0;
        int counter = 0;
        boolean partial = false;
        while (true) {
            int count = in.readInt();
            if (count < 16 || count > CHUNK + 16) throw new IOException("Invalid backup chunk.");
            byte[] encrypted = new byte[count];
            in.readFully(encrypted);
            byte[] plain =
                    cipher(Cipher.DECRYPT_MODE, key, prefix, counter++, header).doFinal(encrypted);
            total += plain.length;
            if (total > MAX_BYTES) throw new IOException("Backup is too large.");
            if (plain.length == 0) {
                if (in.read() != -1) throw new IOException("Unexpected data after backup.");
                output.flush();
                return;
            }
            if (partial) throw new IOException("Invalid backup chunk order.");
            partial = plain.length < CHUNK;
            output.write(plain);
            Arrays.fill(plain, (byte) 0);
        }
    }

    private static SecretKeySpec derive(char[] password, byte[] salt) throws Exception {
        if (password == null || password.length < 8)
            throw new IllegalArgumentException("Use a backup password of at least 8 characters.");
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 256);
        byte[] raw = null;
        try {
            raw =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                            .generateSecret(spec)
                            .getEncoded();
            return new SecretKeySpec(raw, "AES");
        } finally {
            spec.clearPassword();
            if (raw != null) Arrays.fill(raw, (byte) 0);
        }
    }

    private static Cipher cipher(
            int mode, SecretKeySpec key, byte[] prefix, int counter, byte[] header)
            throws Exception {
        byte[] nonce = ByteBuffer.allocate(12).put(prefix).putInt(counter).array();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, key, new GCMParameterSpec(128, nonce));
        cipher.updateAAD(header);
        cipher.updateAAD(ByteBuffer.allocate(4).putInt(counter).array());
        return cipher;
    }
}
