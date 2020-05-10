package de.dopler.ms.login_server.domain;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import java.util.Set;

public class AuthData {

    public final long id;
    public final String secret;
    public final Set<String> groups;

    @JsonbCreator
    public AuthData(@JsonbProperty("id") long id, @JsonbProperty("secret") String secret,
            @JsonbProperty("groups") Set<String> groups) {
        this.id = id;
        this.secret = secret;
        this.groups = groups;
    }

    @Override
    public String toString() {
        return String.format("AuthData[id=%s, secret=HIDDEN, groups=%s]", id, groups);
    }
}
