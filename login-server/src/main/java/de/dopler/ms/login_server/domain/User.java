package de.dopler.ms.login_server.domain;

import java.util.Set;

public class User {

    public final long id;
    public final Set<String> groups;

    public User(long id, Set<String> groups) {
        this.id = id;
        this.groups = groups;
    }
}
