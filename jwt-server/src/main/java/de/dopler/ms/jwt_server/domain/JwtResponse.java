package de.dopler.ms.jwt_server.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class JwtResponse {

    public final String accessToken;
    public final String refreshToken;
    public final long expiresAt;

    public JwtResponse(String accessToken, String refreshToken, long expiresAt) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
    }
}
