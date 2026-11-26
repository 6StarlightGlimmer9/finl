package com.dimension.screenroom.util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * 本地演示库的 users.password 使用明文密码，注册与登录不再调用下列密码哈希方法。
 * hash / matches 仅用于兼容旧库可能残留的 salt$hash 值；hashToken 仍用于 Bearer Token 的 SHA-256 摘要。
 */
public final class PasswordUtil {
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /** 兼容旧库遗留哈希；勿用于新密码。 */
    public static String hash(String rawPassword) {
        String salt = new BigInteger(130, RANDOM).toString(36);
        return salt + "$" + sha256(salt + ":" + rawPassword);
    }

    /** 兼容旧库遗留哈希；勿用于新密码。 */
    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) return false;
        int index = storedHash.indexOf('$');
        if (index <= 0 || index == storedHash.length() - 1) return false;
        String salt = storedHash.substring(0, index);
        String expected = storedHash.substring(index + 1);
        String actual = sha256(salt + ":" + rawPassword);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    /** Bearer Token 仍使用 SHA-256 摘要；与账号密码明文无关。 */
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
