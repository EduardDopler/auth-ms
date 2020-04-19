package de.dopler.ms.credentials_store;

public final class AuthStoreSqlStatements {

    private AuthStoreSqlStatements() {
        // data class
    }

    // @formatter:off
    //language=H2
    static final String SQL_CREATE_TABLE =
            "CREATE TABLE credentials (" +
                    "  id IDENTITY," +
                    "  uid VARCHAR(254) UNIQUE," +
                    "  secret CHAR(60)," +
                    "  groups ARRAY NOT NULL DEFAULT ()," +
                    "  last_mod TIMESTAMP WITH TIME ZONE AS CURRENT_TIMESTAMP());";

    //language=H2
    static final String SQL_CREATE_INDEX =
            "CREATE UNIQUE INDEX idx_uid_secret" +
                    "  ON credentials" +
                    "  (uid, secret);";

    //language=H2
    static final String SQL_INSERT =
            "INSERT INTO credentials" +
                    "  (uid, secret)" +
                    "  VALUES (?, ?);";

    //language=H2
    static final String SQL_SELECT =
            "SELECT id, secret, groups FROM credentials" +
                    "  WHERE" +
                    "  uid = ?;";

    //language=H2
    static final String SQL_UPDATE_UID =
            "UPDATE credentials" +
                    "  SET uid = ?" +
                    "  WHERE" +
                    "  id = ?;";

    //language=H2
    static final String SQL_UPDATE_SECRET =
            "UPDATE credentials" +
                    "  SET secret = ?" +
                    "  WHERE" +
                    "  id = ?;";

    //language=H2
    static final String SQL_UPDATE_GROUPS =
            "UPDATE credentials" +
                    "  SET groups = ?" +
                    "  WHERE" +
                    "  id = ?;";
    // @formatter:on
}
