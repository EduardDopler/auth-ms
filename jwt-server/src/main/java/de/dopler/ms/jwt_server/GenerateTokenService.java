package de.dopler.ms.jwt_server;

import de.dopler.ms.jwt_server.domain.JwtResponse;
import io.smallrye.jwt.build.Jwt;
import org.eclipse.microprofile.jwt.Claims;
import org.jose4j.base64url.Base64Url;
import org.jose4j.lang.ByteUtil;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class GenerateTokenService {

    public static final int EXPIRATION_ACCESS_TOKEN = (int) Duration.ofMinutes(15).toSeconds();
    public static final int EXPIRATION_REFRESH_TOKEN = (int) Duration.ofDays(90).toSeconds();

    public static final String ISSUER = "http://dopler.de/ms/jwt-server";
    public static final String SUBJECT_ACCESS = "atok";
    public static final String SUBJECT_REFRESH = "rtok";

    private final TokenStoreService tokenStoreService;

    @Inject
    public GenerateTokenService(TokenStoreService tokenStoreService) {
        this.tokenStoreService = tokenStoreService;
    }

    public JwtResponse generateJwtTokens(String upn, Set<String> groups) {
        var accessClaims = buildAccessClaims(upn, groups);
        var refreshClaims = buildRefreshClaims(upn);

        var accessExpiration = accessTokenExpiration();
        var refreshExpiration = refreshTokenExpiration();
        var accessJti = randomJti();
        var refreshJti = randomJti();

        tokenStoreService.put(refreshJti, accessClaims, refreshExpiration);

        var accessToken = finalizeAndSignClaims(accessClaims, accessJti, accessExpiration);
        var refreshToken = finalizeAndSignClaims(refreshClaims, refreshJti, refreshExpiration);
        return new JwtResponse(accessToken, refreshToken, accessExpiration);
    }

    public Optional<JwtResponse> refreshJwtTokens(String refreshJti) {
        var accessClaims = tokenStoreService.pop(refreshJti);
        if (accessClaims == null) {
            return Optional.empty();
        }
        var upn = (String) accessClaims.get(Claims.upn.name());
        @SuppressWarnings("unchecked") var groups = (Set<String>) accessClaims.get(
                Claims.groups.name());

        return Optional.of(generateJwtTokens(upn, groups));
    }

    public long cleanupExpiredRefreshTokens() {
        return tokenStoreService.cleanup();
    }

    private static Map<String, Object> buildAccessClaims(String upn, Set<String> groups) {
        return buildClaims(upn, groups, SUBJECT_ACCESS);
    }

    private static Map<String, Object> buildRefreshClaims(String upn) {
        return buildClaims(upn, Collections.emptySet(), SUBJECT_REFRESH);
    }

    private static Map<String, Object> buildClaims(String upn, Set<String> groups, String subject) {
        Map<String, Object> claims = new HashMap<>(4, 1);
        claims.put(Claims.upn.name(), upn);
        claims.put(Claims.iss.name(), ISSUER);
        claims.put(Claims.sub.name(), subject);
        if (!groups.isEmpty()) {
            claims.put(Claims.groups.name(), groups);
        }
        return Map.copyOf(claims);
    }

    private static String finalizeAndSignClaims(Map<String, Object> claims, String tokenId,
            long expiredAt) {
        return Jwt.claims(claims).claim(Claims.jti.name(), tokenId).expiresAt(expiredAt).sign();
    }

    private static String randomJti() {
        return Base64Url.encode(ByteUtil.randomBytes(16));
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
