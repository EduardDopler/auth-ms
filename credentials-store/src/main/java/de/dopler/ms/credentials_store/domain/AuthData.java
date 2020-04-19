package de.dopler.ms.credentials_store.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import java.util.Set;

@RegisterForReflection
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
        return String.format("SecretHolder[uid=%s, secret=HIDDEN, groups=%s]", id, groups);
    }
}
