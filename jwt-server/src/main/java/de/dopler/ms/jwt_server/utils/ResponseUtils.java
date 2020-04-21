package de.dopler.ms.jwt_server.utils;

import de.dopler.ms.jwt_server.services.GenerateTokenService;

import javax.ws.rs.core.CacheControl;

public final class ResponseUtils {

    private static final String CACHE_CONTROL_ALL_OFF = "private, no-store, no-cache";

    private ResponseUtils() {
        // utility-class
    }

    public static final String REFRESH_TOKEN_COOKIE_NAME = "r_token";
    private static final String REFRESH_TOKEN_COOKIE_PATH = "/auth";
    public static final String REFRESH_TOKEN_COOKIE_TEMPLATE = "%s=%s;Path=%s;Max-Age=%d;" +
            "HttpOnly;SameSite=Strict";
    public static final String REFRESH_TOKEN_DELETE_COOKIE = String.format(
            REFRESH_TOKEN_COOKIE_TEMPLATE, REFRESH_TOKEN_COOKIE_NAME, "", REFRESH_TOKEN_COOKIE_PATH,
            0);

    public static String cookieForRefreshToken(String refreshToken) {
        return String.format(REFRESH_TOKEN_COOKIE_TEMPLATE, REFRESH_TOKEN_COOKIE_NAME, refreshToken,
                REFRESH_TOKEN_COOKIE_PATH, GenerateTokenService.EXPIRATION_REFRESH_TOKEN);
    }

    public static CacheControl disableCache() {
        return CacheControl.valueOf(CACHE_CONTROL_ALL_OFF);
    }
}
