package de.dopler.ms.token_store.domain;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import java.util.Set;

public class TokenData {

    public final long userId;
    public final String tokenHash;
    public final Set<String> groups;
    public final long expiresAt;

    @JsonbCreator
    public TokenData(@JsonbProperty("userId") long userId,
            @JsonbProperty("tokenHash") String tokenHash,
            @JsonbProperty("groups") Set<String> groups,
            @JsonbProperty("expiresAt") long expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.groups = groups;
        this.expiresAt = expiresAt;
    }
}
