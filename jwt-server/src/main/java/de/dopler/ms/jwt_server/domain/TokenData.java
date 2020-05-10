package de.dopler.ms.jwt_server.domain;

import java.util.Set;

public class TokenData {

    public final long userId;
    public final String tokenHash;
    public final Set<String> groups;
    public final long expiresAt;

    public TokenData(long userId, String tokenHash, Set<String> groups, long expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.groups = groups;
        this.expiresAt = expiresAt;
    }
}
