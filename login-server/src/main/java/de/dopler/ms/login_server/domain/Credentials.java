package de.dopler.ms.login_server.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;

@RegisterForReflection
public class Credentials {

    public final String uid;
    public final String secret;

    @JsonbCreator
    public Credentials(@JsonbProperty("uid") String uid, @JsonbProperty("secret") String secret) {
        this.uid = uid;
        this.secret = secret;
    }

    @Override
    public String toString() {
        return String.format("Credentials[uid=%s, secret=HIDDEN]", uid);
    }
}
