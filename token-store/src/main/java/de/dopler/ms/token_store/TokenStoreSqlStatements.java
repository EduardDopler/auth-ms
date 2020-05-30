package de.dopler.ms.token_store;

public final class TokenStoreSqlStatements {

    private TokenStoreSqlStatements() {
        // data class
    }

    // @formatter:off
    //language=H2
    static final String SQL_CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS tokens (" +
                    "  id IDENTITY," +
                    "  user_id BIGINT NOT NULL," +
                    "  token_hash CHAR(64) NOT NULL," +
                    "  groups ARRAY NOT NULL DEFAULT ()," +
                    "  expires_at TIMESTAMP WITH TIME ZONE NOT NULL," +
                    "  last_mod TIMESTAMP WITH TIME ZONE AS CURRENT_TIMESTAMP());";

    //language=H2
    static final String SQL_CREATE_INDEX_USER_ID_TOKEN_HASH =
            "CREATE INDEX IF NOT EXISTS idx_user_id" +
                    "  ON tokens" +
                    "  (user_id, token_hash);";

    //language=H2
    static final String SQL_CREATE_INDEX_EXPIRES_AT =
            "CREATE INDEX IF NOT EXISTS idx_expires_at" +
                    "  ON tokens" +
                    "  (expires_at);";

    //language=H2
    static final String SQL_INSERT =
            "INSERT INTO tokens" +
                    "  (user_id, token_hash, groups, expires_at)" +
                    "  VALUES (?, ?, ?, ?);";

    //language=H2
    static final String SQL_SELECT_GROUPS =
            "SELECT groups FROM tokens" +
                    "  WHERE" +
                    "  user_id = ? AND token_hash = ? AND expires_at > CURRENT_TIMESTAMP(0);";

    //language=H2
    static final String SQL_DELETE_EXPIRED =
            "DELETE FROM tokens" +
                    "  WHERE" +
                    "  expires_at < CURRENT_TIMESTAMP(0);";

    //language=H2
    static final String SQL_DELETE_BY_USER_ID =
            "DELETE FROM tokens" +
                    "  WHERE" +
                    "  user_id = ?;";

    //language=H2
    static final String SQL_DELETE_BY_USER_ID_TOKEN_HASH =
            "DELETE FROM tokens" +
                    "  WHERE" +
                    "  user_id = ? AND token_hash = ?;";
    // @formatter:on
}
