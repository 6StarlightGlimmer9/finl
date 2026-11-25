package com.dimension.screenroom.util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class PasswordUtil {
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hash(String rawPassword) {
        String salt = new BigInteger(130, RANDOM).toString(36);
        return salt + "$" + sha256(salt + ":" + rawPassword);
    }

    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) return false;
        int index = storedHash.indexOf('$');
        if (index <= 0 || index == storedHash.length() - 1) return false;
        String salt = storedHash.substring(0, index);
        String expected = storedHash.substring(index + 1);
        String actual = sha256(salt + ":" + rawPassword);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    public static String hashToken(String rawToken) {
        return sha256(rawToken);
    }

    private static String sha256(String input) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) builder.append(String.format("%02x", value));
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
