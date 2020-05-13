package de.dopler.ms.token_store;

public final class TestTokenStoreSqlStatements {

    private TestTokenStoreSqlStatements() {
        // data class
    }

    //language=H2
    static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS tokens";

    //language=H2
    static final String SQL_DROP_INDEX_USER_ID_TOKEN_HASH = "DROP INDEX IF EXISTS idx_user_id";

    //language=H2
    static final String SQL_DROP_INDEX_EXPIRES_AT = "DROP INDEX IF EXISTS idx_expires_at";

    // @formatter:off
    //language=H2
    static final String SQL_SELECT_GROUPS_EXACTLY =
            "SELECT groups FROM tokens" +
                    "  WHERE user_id = ? AND token_hash = ? AND expires_at = ?";
    // @formatter:on
}
