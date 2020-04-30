package de.dopler.ms.jwt_server.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class RefreshTokenUtils {

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);

    private RefreshTokenUtils() {
        // utility class
    }

    public static String toSha256Hash(String token) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(
                    "NoSuchAlgorithmException in RefreshTokenUtils#toSha256Hash: " + e.getMessage());
        }
        var hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));

        var hexChars = new byte[hashBytes.length * 2];
        for (int j = 0; j < hashBytes.length; j++) {
            int v = hashBytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }
}
