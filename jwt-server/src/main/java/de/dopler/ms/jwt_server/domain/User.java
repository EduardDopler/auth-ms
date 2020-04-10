package de.dopler.ms.jwt_server.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import java.util.Set;

@RegisterForReflection
public class User {

    public final Long id;
    public final Set<String> groups;

    @JsonbCreator
    public User(@JsonbProperty("id") Long id, @JsonbProperty("groups") Set<String> groups) {
        this.id = id;
        this.groups = groups;
    }
}
