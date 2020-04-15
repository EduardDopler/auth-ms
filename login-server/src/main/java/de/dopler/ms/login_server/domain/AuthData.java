package de.dopler.ms.login_server.domain;

import java.util.Set;

public class AuthData {

    public final long id;
    public final String secret;
    public final Set<String> groups;

    public AuthData(long id, String secret, Set<String> groups) {
        this.id = id;
        this.secret = secret;
        this.groups = groups;
    }

    @Override
    public String toString() {
        return String.format("SecretHolder[uid=%s, secret=HIDDEN, groups=%s]", id, groups);
    }
}
