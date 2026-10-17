/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.common.web.auth;

import org.jetbrains.annotations.Nullable;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Locale;

/**
 * Password hashing for web-accounts.
 * <p>
 * Uses PBKDF2-HMAC-SHA256, which is available in every JDK, so no additional dependency is needed.
 * The stored format is:
 * <pre>pbkdf2-sha256$&lt;iterations&gt;$&lt;salt-base64&gt;$&lt;hash-base64&gt;</pre>
 * <p>
 * This deliberately does not follow Dynmap's single-round {@code SHA-256(salt + password)}.
 */
public final class PasswordHash {

    private static final String ALGORITHM_ID = "pbkdf2-sha256";
    private static final String JCE_ALGORITHM = "PBKDF2WithHmacSHA256";

    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final int KEY_BYTES = KEY_BITS / 8;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    private PasswordHash() {
    }

    /**
     * Hashes a password with a fresh random salt.
     *
     * @param password the plain-text password
     * @return the hash in the format described above
     */
    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);

        byte[] hash = pbkdf2(password, salt, ITERATIONS, KEY_BITS);
        return ALGORITHM_ID + "$" + ITERATIONS + "$" + ENCODER.encodeToString(salt) + "$" + ENCODER.encodeToString(hash);
    }

    /**
     * Verifies a password against a stored hash. Uses a constant-time comparison.
     *
     * @param password the plain-text password (may be {@code null})
     * @param stored the stored hash, as produced by {@link #hash(String)}
     * @return {@code true} if the password matches, {@code false} otherwise
     *         (also {@code false} for {@code null} or malformed input)
     */
    public static boolean verify(@Nullable String password, @Nullable String stored) {
        if (password == null || stored == null) return false;

        String[] parts = stored.split("\\$");
        if (parts.length != 4) return false;
        if (!ALGORITHM_ID.equals(parts[0])) return false;

        int iterations;
        try {
            iterations = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ex) {
            return false;
        }
        if (iterations <= 0) return false;

        byte[] salt;
        byte[] expected;
        try {
            salt = DECODER.decode(parts[2]);
            expected = DECODER.decode(parts[3]);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        if (expected.length != KEY_BYTES) return false;

        byte[] actual = pbkdf2(password, salt, iterations, expected.length * 8);
        return MessageDigest.isEqual(expected, actual);
    }

    /**
     * The SHA-256 hex digest of a string. Used for short-lived values (registration passcodes,
     * session tokens) where a slow KDF would be pointless but plain-text storage is not wanted.
     */
    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    /**
     * Compares two strings in constant time. Used for hashes and passcodes.
     */
    public static boolean equalsConstantTime(@Nullable String a, @Nullable String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(
                a.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                b.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)
        );
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations, int keyBits) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyBits);
        try {
            return SecretKeyFactory.getInstance(JCE_ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("PBKDF2 is not available", ex);
        } finally {
            spec.clearPassword();
        }
    }

}
