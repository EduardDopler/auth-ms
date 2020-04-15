package de.dopler.ms.login_server;

import de.dopler.ms.login_server.domain.AuthData;
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

import static de.dopler.ms.login_server.AuthStoreSqlStatements.*;
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
            LOG.error(e.getMessage());
            throw new IllegalStateException("Initializing the store failed due to SQL exception");
        }
    }

    @NonNull
    public Optional<Long> storeCredentials(@NonNull String uid, @NonNull String hashedSecret) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(SQL_INSERT,
                     Statement.RETURN_GENERATED_KEYS)) {
            conn.setAutoCommit(true);
            statement.setString(1, uid);
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
            LOG.error(e.getMessage());
        }
        return Optional.empty();
    }

    @NonNull
    public Optional<AuthData> getId(@NonNull String uid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(SQL_SELECT)) {
            conn.setAutoCommit(true);
            statement.setString(1, uid);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    var id = resultSet.getLong(1);
                    var secret = resultSet.getString(2);
                    var groups = toStringSet(resultSet.getArray(3));
                    return Optional.of(new AuthData(id, secret, groups));
                }
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage());
            throw new IllegalStateException("getId(uid) failed due to SQL exception");
        }
        return Optional.empty();
    }

    public boolean updateUid(@NonNull Long id, @NonNull String newUid) {
        return updateStringColumn(id, newUid, SQL_UPDATE_UID);
    }

    public boolean updateSecret(@NonNull Long id, @NonNull String newSecret) {
        return updateStringColumn(id, newSecret, SQL_UPDATE_SECRET);
    }

    public boolean updateGroups(@NonNull Long id, @NonNull Set<String> newGroups) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(SQL_UPDATE_GROUPS)) {
            conn.setAutoCommit(true);
            Array groupsArray = conn.createArrayOf(VARCHAR.name(), newGroups.toArray());
            statement.setArray(1, groupsArray);
            statement.setLong(2, id);
            if (statement.executeUpdate() == 1) {
                return true;
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage());
        }
        return false;
    }

    private boolean updateStringColumn(@NonNull Long id, @NonNull String newValue,
            @NonNull String sqlUpdateUid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(sqlUpdateUid)) {
            conn.setAutoCommit(true);
            statement.setString(1, newValue);
            statement.setLong(2, id);
            if (statement.executeUpdate() == 1) {
                return true;
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage());
        }
        return false;
    }

    @NonNull
    private static Set<String> toStringSet(@NonNull Array sqlArray) throws SQLException {
        var objectArray = (Object[]) sqlArray.getArray();
        return Stream.of(objectArray).map(String.class::cast).collect(Collectors.toSet());
    }
}
