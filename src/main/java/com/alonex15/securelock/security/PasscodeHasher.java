package com.alonex15.securelock.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Hash de contraseñas: {@code SHA-256(salt + code)} con salt aleatoria por bloque.
 * No depende de clases de Minecraft, así que no cambia entre versiones.
 */
public final class PasscodeHasher {
    public static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private PasscodeHasher() {
    }

    public record Hashed(String salt, String hash) {
    }

    public static String newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return HEX.formatHex(salt);
    }

    public static Hashed hash(String code) {
        String salt = newSalt();
        return new Hashed(salt, hash(salt, code));
    }

    public static String hash(String salt, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            digest.update(code.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 es obligatorio en toda JVM
            throw new IllegalStateException(e);
        }
    }

    /** Comparación en tiempo constante para no filtrar información por temporización. */
    public static boolean verify(String salt, String expectedHash, String code) {
        if (salt == null || expectedHash == null || code == null) {
            return false;
        }
        byte[] expected = expectedHash.getBytes(StandardCharsets.US_ASCII);
        byte[] actual = hash(salt, code).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, actual);
    }

    /** Un código válido solo tiene dígitos y una longitud dentro de los límites configurados. */
    public static boolean isValidCode(String code, int minLength, int maxLength) {
        if (code == null || code.length() < minLength || code.length() > maxLength) {
            return false;
        }
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
