package au.com.waterlogged.services;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.*;
import java.util.*;

public class BackupCryptoTest {
    @Test
    public void encryptedChunksRoundTripAndUseFreshRandomness() throws Exception {
        byte[] original = new byte[150000];
        new Random(23).nextBytes(original);
        byte[] first = encrypt(original), second = encrypt(original);
        assertFalse(Arrays.equals(first, second));
        ByteArrayOutputStream plain = new ByteArrayOutputStream();
        BackupCrypto.decrypt(
                new ByteArrayInputStream(first), plain, "test-long-password".toCharArray());
        assertArrayEquals(original, plain.toByteArray());
    }

    @Test
    public void wrongPasswordTamperingAndTruncationAreRejected() throws Exception {
        byte[] archive =
                encrypt("private fishing marks".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        reject(archive, "wrong-password");
        byte[] changed = archive.clone();
        changed[changed.length - 1] ^= 1;
        reject(changed, "test-long-password");
        reject(Arrays.copyOf(archive, archive.length - 20), "test-long-password");
        byte[] extra = Arrays.copyOf(archive, archive.length + 1);
        reject(extra, "test-long-password");
    }

    private byte[] encrypt(byte[] raw) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BackupCrypto.encrypt(
                new ByteArrayInputStream(raw), out, "test-long-password".toCharArray());
        return out.toByteArray();
    }

    private void reject(byte[] raw, String password) throws Exception {
        try {
            BackupCrypto.decrypt(
                    new ByteArrayInputStream(raw),
                    new ByteArrayOutputStream(),
                    password.toCharArray());
            fail("Damaged or unauthenticated backup was accepted");
        } catch (IOException | java.security.GeneralSecurityException expected) {
        }
    }
}
