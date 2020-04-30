package de.dopler.ms.jwt_server.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class JwtResponse {

    public final String accessToken;
    public final long expiresAt;

    public JwtResponse(String accessToken, long expiresAt) {
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
    }
}
