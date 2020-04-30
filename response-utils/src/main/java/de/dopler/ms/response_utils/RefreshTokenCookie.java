package de.dopler.ms.response_utils;

public class RefreshTokenCookie {

    public static final String NAME = "r_token";
    private static final String PATH = "/auth";
    private static final String TEMPLATE = "%s=%s;Path=%s;Max-Age=%d;HttpOnly;SameSite=Strict";

    public final String value;

    public RefreshTokenCookie(String refreshToken, long expiresAt) {
        value = String.format(TEMPLATE, NAME, refreshToken, PATH, expiresAt);
    }

    @Override
    public String toString() {
        return value;
    }
}
