package de.dopler.ms.login_server.domain;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;

public class Credentials {

    public final String username;
    public final String secret;

    @JsonbCreator
    public Credentials(@JsonbProperty("username") String username,
            @JsonbProperty("secret") String secret) {
        this.username = username;
        this.secret = secret;
    }

    @Override
    public String toString() {
        return String.format("Credentials[username=%s, secret=HIDDEN]", username);
    }
}
