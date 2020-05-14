package de.dopler.ms.credentials_store;

import de.dopler.ms.credentials_store.domain.AuthData;
import org.eclipse.jdt.annotation.NonNull;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.dopler.ms.credentials_store.AuthStoreSqlStatements.*;
import static java.sql.JDBCType.VARCHAR;

@ApplicationScoped
public class AuthStoreService {

    private static final Logger LOG = Logger.getLogger("AuthStoreService");

    private static final String SQL_STATE_UNIQUE_VIOLATION = "23505";

    private final DataSource dataSource;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public AuthStoreService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initStore() {
        try (Connection conn = dataSource.getConnection();
             Statement statement = conn.createStatement()) {
            conn.setAutoCommit(false);
            statement.execute(SQL_CREATE_TABLE);
            statement.execute(SQL_CREATE_INDEX);
            conn.commit();
        } catch (SQLException e) {
            LOG.errorf("initStore failed: %s", e.getMessage());
            throw new IllegalStateException("Initializing the store failed due to SQL exception");
        }
    }

    @NonNull
    public Optional<Long> storeCredentials(@NonNull String username, @NonNull String hashedSecret) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(SQL_INSERT,
                     Statement.RETURN_GENERATED_KEYS)) {
            conn.setAutoCommit(true);
            statement.setString(1, username);
            statement.setString(2, hashedSecret);
            if (statement.executeUpdate() != 1) {
                return Optional.empty();
            }
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return Optional.of(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            // don't log UNIQUE violations
            if (e.getSQLState().equals(SQL_STATE_UNIQUE_VIOLATION)) {
                throw new IllegalArgumentException("conflict");
            }
            LOG.errorf("storeCredentials failed: %s", e.getMessage());
        }
        return Optional.empty();
    }

    @NonNull
    public Optional<AuthData> getAuthData(@NonNull String username) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(SQL_SELECT)) {
            conn.setAutoCommit(true);
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    var id = resultSet.getLong(1);
                    var secret = resultSet.getString(2);
                    var groups = toStringSet(resultSet.getArray(3));
                    return Optional.of(new AuthData(id, secret, groups));
                }
            }
        } catch (SQLException e) {
            LOG.errorf("getAuthData failed: %s", e.getMessage());
            throw new IllegalStateException("getId(username) failed due to SQL exception");
        }
        return Optional.empty();
    }

    public boolean updateUsername(long id, @NonNull String newUsername) {
        return updateStringColumn(id, newUsername, SQL_UPDATE_USERNAME);
    }

    public boolean updateSecret(long id, @NonNull String newSecret) {
        return updateStringColumn(id, newSecret, SQL_UPDATE_SECRET);
    }

    public boolean updateGroups(long id, @NonNull Set<String> newGroups) {
        var updatedRows = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(SQL_UPDATE_GROUPS)) {
            conn.setAutoCommit(true);
            Array groupsArray = conn.createArrayOf(VARCHAR.name(), newGroups.toArray());
            statement.setArray(1, groupsArray);
            statement.setLong(2, id);
            updatedRows = statement.executeUpdate();
        } catch (SQLException e) {
            LOG.errorf("updateGroups failed: %s", e.getMessage());
            throw new IllegalStateException("updateGroups failed due to SQL exception");
        }
        return updatedRows == 1;
    }

    private boolean updateStringColumn(long id, @NonNull String newValue,
            @NonNull String sqlStatement) {
        var updatedRows = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(sqlStatement)) {
            conn.setAutoCommit(true);
            statement.setString(1, newValue);
            statement.setLong(2, id);
            updatedRows = statement.executeUpdate();
        } catch (SQLException e) {
            // don't log UNIQUE violations
            if (e.getSQLState().equals(SQL_STATE_UNIQUE_VIOLATION)) {
                throw new IllegalArgumentException("conflict");
            }
            LOG.errorf("updateStringColumn failed: %s", e.getMessage());
            throw new IllegalStateException("updateStringColumn failed due to SQL exception");
        }
        return updatedRows == 1;
    }

    @NonNull
    private static Set<String> toStringSet(@NonNull Array sqlArray) throws SQLException {
        var objectArray = (Object[]) sqlArray.getArray();
        return Stream.of(objectArray).map(String.class::cast).collect(Collectors.toSet());
    }
}
