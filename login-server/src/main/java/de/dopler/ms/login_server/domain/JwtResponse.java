package de.dopler.ms.login_server.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;

@RegisterForReflection
public class JwtResponse {

    public final String accessToken;
    public final long expiresAt;

    @JsonbCreator
    public JwtResponse(@JsonbProperty("accessToken") String accessToken,
            @JsonbProperty("expiresAt") long expiresAt) {
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
    }
}
