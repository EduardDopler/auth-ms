package de.dopler.ms.token_store;

import org.eclipse.jdt.annotation.NonNull;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.dopler.ms.token_store.TokenStoreSqlStatements.*;
import static java.sql.JDBCType.VARCHAR;

@ApplicationScoped
public class TokenStoreService {

    private static final Logger LOG = Logger.getLogger("TokenStoreService");

    private final DataSource dataSource;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public TokenStoreService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initStore() {
        var timingStart = Instant.now();
        try (Connection conn = dataSource.getConnection();
             Statement statement = conn.createStatement()) {
            conn.setAutoCommit(false);
            statement.execute(SQL_CREATE_TABLE);
            statement.execute(SQL_CREATE_INDEX_USER_ID_TOKEN_HASH);
            statement.execute(SQL_CREATE_INDEX_EXPIRES_AT);
            conn.commit();
        } catch (SQLException e) {
            LOG.errorf("initStore failed: %s", e.getMessage());
            throw new IllegalStateException("Initializing the store failed due to SQL exception");
        }
        var duration = timingStart.until(Instant.now(), ChronoUnit.MILLIS);
        LOG.infof("Database initialization succeeded after %d ms", duration);
    }

    public boolean put(long userId, @NonNull String tokenHash, @NonNull Set<String> groups,
            long expiresAt) {
        var expiresAtInstant = Instant.ofEpochSecond(expiresAt);
        var updatedRows = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(SQL_INSERT)) {
            conn.setAutoCommit(true);
            statement.setLong(1, userId);
            statement.setString(2, tokenHash);
            Array groupsArray = conn.createArrayOf(VARCHAR.name(), groups.toArray());
            statement.setArray(3, groupsArray);
            statement.setTimestamp(4, Timestamp.from(expiresAtInstant));
            updatedRows = statement.executeUpdate();
        } catch (SQLException e) {
            LOG.errorf("put failed for userId %d: %s", userId, e.getMessage());
            throw new IllegalStateException("put failed due to SQL exception");
        }
        return updatedRows == 1;
    }

    @NonNull
    public Optional<Set<String>> popGroups(long userId, @NonNull String tokenHash) {
        Set<String> groups = null;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement selectStatement = conn.prepareStatement(SQL_SELECT_GROUPS);
             PreparedStatement deleteStatement = conn.prepareStatement(
                     SQL_DELETE_BY_USER_ID_TOKEN_HASH)) {
            // we have multiple queries but still the results should not be rolled back if only
            // the delete statement fails: so set auto commit to true
            conn.setAutoCommit(true);
            // select
            selectStatement.setLong(1, userId);
            selectStatement.setString(2, tokenHash);
            try (ResultSet resultSet = selectStatement.executeQuery()) {
                if (resultSet.next()) {
                    groups = toStringSet(resultSet.getArray(1));
                }
            }
            if (groups == null) {
                return Optional.empty();
            }
            // delete
            deleteStatement.setLong(1, userId);
            deleteStatement.setString(2, tokenHash);
            if (deleteStatement.executeUpdate() == 0) {
                LOG.errorf("delete statement in popGroups failed for userId %d", userId);
            }
        } catch (SQLException e) {
            LOG.errorf("popGroups failed for userId %d: %s", userId, e.getMessage());
            throw new IllegalStateException("popGroups failed due to SQL exception");
        }
        return Optional.of(groups);
    }

    public int deleteForUser(long userId) {
        var updatedRows = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(SQL_DELETE_BY_USER_ID)) {
            conn.setAutoCommit(true);
            statement.setLong(1, userId);
            updatedRows = statement.executeUpdate();
        } catch (SQLException e) {
            LOG.errorf("deleteForUser failed: %s", e.getMessage());
            throw new IllegalStateException("deleteForUser failed due to SQL exception");
        }
        return updatedRows;
    }

    public int deleteExpired() {
        var updatedRows = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(SQL_DELETE_EXPIRED)) {
            conn.setAutoCommit(true);
            updatedRows = statement.executeUpdate();
        } catch (SQLException e) {
            LOG.errorf("deleteExpired failed: %s", e.getMessage());
            throw new IllegalStateException("deleteExpired failed due to SQL exception");
        }
        return updatedRows;
    }

    @NonNull
    private static Set<String> toStringSet(@NonNull Array sqlArray) throws SQLException {
        var objectArray = (Object[]) sqlArray.getArray();
        return Stream.of(objectArray).map(String.class::cast).collect(Collectors.toSet());
    }
}
