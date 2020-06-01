package de.dopler.ms.jwt_server.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class JwtResponse {

    public final long userId;
    public final String accessToken;
    public final long expiresAt;

    public JwtResponse(long userId, String accessToken, long expiresAt) {
        this.userId = userId;
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
    }
}
