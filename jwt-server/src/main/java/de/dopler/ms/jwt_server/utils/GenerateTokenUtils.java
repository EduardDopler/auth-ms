package de.dopler.ms.jwt_server.utils;

import de.dopler.ms.jwt_server.domain.JWTokens;
import io.smallrye.jwt.build.Jwt;
import org.eclipse.microprofile.jwt.Claims;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

public final class GenerateTokenUtils {

    public static final int EXPIRATION_ACCESS_TOKEN = (int) Duration.ofMinutes(15).toSeconds();
    public static final int EXPIRATION_REFRESH_TOKEN = (int) Duration.ofDays(90).toSeconds();

    public static final String ISSUER = "http://dopler.de/ms/jwt-server";
    public static final String SUBJECT_ACCESS = "atok";
    public static final String SUBJECT_REFRESH = "rtok";

    public static JWTokens generateJwtTokens(long userId, Set<String> groups) {
        var accessExpiration = accessTokenExpiration();
        var refreshExpiration = refreshTokenExpiration();

        var accessClaims = buildAccessClaims(userId, accessExpiration, groups);
        var refreshClaims = buildRefreshClaims(userId, refreshExpiration);

        var accessToken = Jwt.claims(accessClaims).sign();
        var refreshToken = Jwt.claims(refreshClaims).sign();
        return new JWTokens(accessToken, refreshToken, accessExpiration, refreshExpiration);
    }

    private static Map<String, Object> buildAccessClaims(long userId, long accessExpiration,
            Set<String> groups) {
        // @formatter:off
        return Map.of(
                Claims.upn.name(), Long.toString(userId),
                Claims.iss.name(), ISSUER,
                Claims.sub.name(), SUBJECT_ACCESS,
                Claims.exp.name(), accessExpiration,
                Claims.groups.name(), groups);
        // @formatter:on
    }

    private static Map<String, Object> buildRefreshClaims(long userId, long refreshExpiration) {
        // @formatter:off
        return Map.of(
                Claims.upn.name(), Long.toString(userId),
                Claims.iss.name(), ISSUER,
                Claims.sub.name(), SUBJECT_REFRESH,
                Claims.exp.name(), refreshExpiration);
        // @formatter:on
    }

    private static long accessTokenExpiration() {
        return Instant.now(Clock.systemDefaultZone())
                .plusSeconds(EXPIRATION_ACCESS_TOKEN)
                .getEpochSecond();
    }

    private static long refreshTokenExpiration() {
        return Instant.now(Clock.systemDefaultZone())
                .plusSeconds(EXPIRATION_REFRESH_TOKEN)
                .getEpochSecond();
    }
}
