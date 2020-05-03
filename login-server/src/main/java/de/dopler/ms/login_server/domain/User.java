package de.dopler.ms.login_server.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import java.util.Set;

@RegisterForReflection
public class User {

    public final long id;
    public final Set<String> groups;

    @JsonbCreator
    public User(@JsonbProperty("id") long id, @JsonbProperty("groups") Set<String> groups) {
        this.id = id;
        this.groups = groups;
    }
}
