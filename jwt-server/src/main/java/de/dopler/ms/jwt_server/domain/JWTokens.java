package de.dopler.ms.jwt_server.domain;

public class JWTokens {

    public final String accessToken;
    public final String refreshToken;
    public final long accessTokenExpiresAt;
    public final long refreshTokenExpiresAt;

    public JWTokens(String accessToken, String refreshToken, long accessTokenExpiresAt,
            long refreshTokenExpiresAt) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }
}
