package de.dopler.ms.token_store;

import de.dopler.ms.token_store.domain.TokenData;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.sql.*;
import java.time.Instant;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.dopler.ms.token_store.TestTokenStoreSqlStatements.*;
import static de.dopler.ms.token_store.TokenStoreSqlStatements.*;
import static io.restassured.RestAssured.given;
import static java.sql.JDBCType.VARCHAR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
class TokenStoreResourceTest {

    private static final URI RESOURCE_BASE_URI = URI.create("/auth/tokens");
    private static final String POP_GROUPS_PATH = "/{userId}";
    private static final String POP_GROUPS_QUERY_PARAM = "token-hash";
    private static final String DELETE_EXPIRED_PATH = "/expired";

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    DataSource dataSource;

    @BeforeEach
    void setUp() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement statement = conn.createStatement()) {
            conn.setAutoCommit(false);
            statement.execute(SQL_CREATE_TABLE);
            statement.execute(SQL_CREATE_INDEX_USER_ID_TOKEN_HASH);
            statement.execute(SQL_CREATE_INDEX_EXPIRES_AT);
            conn.commit();
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement statement = conn.createStatement()) {
            conn.setAutoCommit(false);
            statement.execute(SQL_DROP_TABLE);
            statement.execute(SQL_DROP_INDEX_USER_ID_TOKEN_HASH);
            statement.execute(SQL_DROP_INDEX_EXPIRES_AT);
            conn.commit();
        }
    }

    // #storeToken =================================================================================

    @Test
    void storeTokenEndpointReturnsCode204() {
        givenPostToEndpoint(tokenData()).then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @Test
    void storeTokenEndpointStoresTokenInStore() throws SQLException {
        var tokenData = tokenData();
        boolean foundBefore = findRowInDb(tokenData.userId, tokenData.tokenHash, tokenData.groups,
                tokenData.expiresAt);
        assertThat("SQL pre-condition failed", foundBefore, is(equalTo(false)));

        givenPostToEndpoint(tokenData);

        boolean foundAfter = findRowInDb(tokenData.userId, tokenData.tokenHash, tokenData.groups,
                tokenData.expiresAt);
        assertThat(foundAfter, is(equalTo(true)));
    }

    @Test
    void storeTokenEndpointReturnsCode400OnInvalidTokenData() {
        givenPostToEndpoint(tokenDataWithoutHash()).then()
                .statusCode(Status.BAD_REQUEST.getStatusCode());

        givenPostToEndpoint(tokenDataWithoutGroups()).then()
                .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void storeTokenEndpointHasPrivateCacheControlHeader() {
        // @formatter:off
        givenPostToEndpoint(tokenData()).then()
            .header(HttpHeaders.CACHE_CONTROL, is(notNullValue()))
            .header(HttpHeaders.CACHE_CONTROL,
                    s -> Stream.of(s.split(",")).map(String::trim).toArray(),
                    arrayContainingInAnyOrder("private", "no-store", "no-cache"));
        // @formatter:on
    }

    // #popGroups ==================================================================================

    @Test
    void popGroupsEndpointReturnsGroupsIfTokenDataFound() throws SQLException {
        var tokenData = tokenData();
        boolean inserted = insertRowToDb(tokenData.userId, tokenData.tokenHash, tokenData.groups,
                tokenData.expiresAt);
        assertThat("SQL pre-condition failed", inserted, is(equalTo(true)));

        // @formatter:off
        var response = givenGetFromEndpoint(tokenData.userId, tokenData.tokenHash).then()
            .extract().response();
        // @formatter:on
        assertThat(response.statusCode(), is(equalTo(Status.OK.getStatusCode())));

        var groups = response.body().as(new TypeRef<Set<String>>() {});
        assertThat(groups.toArray(), arrayContainingInAnyOrder(tokenData.groups.toArray()));
    }

    @Test
    void popGroupsEndpointReturns404IfTokenDataNotFound() throws SQLException {
        var tokenData = tokenData();
        boolean found = findRowInDb(tokenData.userId, tokenData.tokenHash, tokenData.groups,
                tokenData.expiresAt);
        assertThat("SQL pre-condition failed", found, is(equalTo(false)));

        givenGetFromEndpoint(tokenData.userId, tokenData.tokenHash).then()
                .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void popGroupsEndpointReturns404IfTokenDataIsFoundButExpired() throws SQLException {
        var expiredTokenData = expiredTokenData();
        boolean inserted = insertRowToDb(expiredTokenData.userId, expiredTokenData.tokenHash,
                expiredTokenData.groups, expiredTokenData.expiresAt);
        assertThat("SQL pre-condition failed", inserted, is(equalTo(true)));

        givenGetFromEndpoint(expiredTokenData.userId, expiredTokenData.tokenHash).then()
                .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void popGroupsEndpointRemovesTokenOnSuccess() throws SQLException {
        var tokenData = tokenData();
        boolean inserted = insertRowToDb(tokenData.userId, tokenData.tokenHash, tokenData.groups,
                tokenData.expiresAt);
        assertThat("SQL pre-condition failed", inserted, is(equalTo(true)));

        // first call will be successful
        // @formatter:off
        var response = givenGetFromEndpoint(tokenData.userId, tokenData.tokenHash).then()
                .extract().response();
        // @formatter:on
        assertThat(response.statusCode(), is(equalTo(Status.OK.getStatusCode())));

        var groups = response.body().as(new TypeRef<Set<String>>() {});
        assertThat(groups.toArray(), arrayContainingInAnyOrder(tokenData.groups.toArray()));

        // now the entry should be deleted from the store
        boolean found = findRowInDb(tokenData.userId, tokenData.tokenHash, tokenData.groups,
                tokenData.expiresAt);
        assertThat(found, is(equalTo(false)));

        // ... and the endpoint returns 404
        givenGetFromEndpoint(tokenData.userId, tokenData.tokenHash).then()
                .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void popGroupsEndpointReturnsCode400OnNullOrEmptyTokenHash() {
        givenGetFromEndpoint(new Random().nextLong(), null).then()
                .statusCode(Status.BAD_REQUEST.getStatusCode());
        givenGetFromEndpoint(new Random().nextLong(), "").then()
                .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void popGroupsEndpointHasPrivateCacheControlHeaderIfTokenDataFound() throws SQLException {
        var tokenData = tokenData();
        boolean inserted = insertRowToDb(tokenData.userId, tokenData.tokenHash, tokenData.groups,
                tokenData.expiresAt);
        assertThat("SQL pre-condition failed", inserted, is(equalTo(true)));

        // @formatter:off
        givenGetFromEndpoint(tokenData.userId, tokenData.tokenHash).then()
            .header(HttpHeaders.CACHE_CONTROL, is(notNullValue()))
            .header(HttpHeaders.CACHE_CONTROL,
                    s -> Stream.of(s.split(",")).map(String::trim).toArray(),
                    arrayContainingInAnyOrder("private", "no-store", "no-cache"));
        // @formatter:on
    }

    @Test
    void popGroupsEndpointHasPrivateCacheControlHeaderIfTokenDataNotFound() {
        var userId = new Random().nextLong();
        var tokenHash = UUID.randomUUID().toString();
        // @formatter:off
        givenGetFromEndpoint(userId, tokenHash).then()
            .header(HttpHeaders.CACHE_CONTROL, is(notNullValue()))
            .header(HttpHeaders.CACHE_CONTROL,
                    s -> Stream.of(s.split(",")).map(String::trim).toArray(),
                    arrayContainingInAnyOrder("private", "no-store", "no-cache"));
        // @formatter:on
    }

    // #deleteExpired ==============================================================================

    @Test
    void deleteExpiredEndpointDeletesExpiredTokenDataAndReturnsDeleteCount() throws SQLException {
        // insert 2 fresh and 2 expired token rows
        var freshTokenData1 = tokenData();
        boolean inserted1 = insertRowToDb(freshTokenData1.userId, freshTokenData1.tokenHash,
                freshTokenData1.groups, freshTokenData1.expiresAt);
        assertThat("SQL pre-condition failed", inserted1, is(equalTo(true)));
        var freshTokenData2 = tokenData();
        boolean inserted2 = insertRowToDb(freshTokenData2.userId, freshTokenData2.tokenHash,
                freshTokenData2.groups, freshTokenData2.expiresAt);
        assertThat("SQL pre-condition failed", inserted2, is(equalTo(true)));

        var expiredTokenData1 = expiredTokenData();
        boolean inserted3 = insertRowToDb(expiredTokenData1.userId, expiredTokenData1.tokenHash,
                expiredTokenData1.groups, expiredTokenData1.expiresAt);
        assertThat("SQL pre-condition failed", inserted3, is(equalTo(true)));
        var expiredTokenData2 = expiredTokenData();
        boolean inserted4 = insertRowToDb(expiredTokenData2.userId, expiredTokenData2.tokenHash,
                expiredTokenData2.groups, expiredTokenData2.expiresAt);
        assertThat("SQL pre-condition failed", inserted4, is(equalTo(true)));

        var expectedDeleted = "2";
        // @formatter:off
        givenDeleteFromEndpoint().then()
            .statusCode(Status.OK.getStatusCode())
            .header(HttpHeaders.CACHE_CONTROL, is(notNullValue()))
            .header(HttpHeaders.CACHE_CONTROL,
                    s -> Stream.of(s.split(",")).map(String::trim).toArray(),
                    arrayContainingInAnyOrder("private", "no-store", "no-cache"))
            .contentType(ContentType.TEXT)
            .body(is(equalTo(expectedDeleted)));
        // @formatter:on

        // now both expired entries should be deleted from the store
        boolean foundFresh1 = findRowInDb(freshTokenData1.userId, freshTokenData1.tokenHash,
                freshTokenData1.groups, freshTokenData1.expiresAt);
        assertThat(foundFresh1, is(equalTo(true)));
        boolean foundFresh2 = findRowInDb(freshTokenData2.userId, freshTokenData2.tokenHash,
                freshTokenData2.groups, freshTokenData2.expiresAt);
        assertThat(foundFresh2, is(equalTo(true)));
        // the other two should still be there
        boolean foundExpired1 = findRowInDb(expiredTokenData1.userId, expiredTokenData1.tokenHash,
                expiredTokenData1.groups, expiredTokenData1.expiresAt);
        assertThat(foundExpired1, is(equalTo(false)));
        boolean foundExpired2 = findRowInDb(expiredTokenData2.userId, expiredTokenData2.tokenHash,
                expiredTokenData2.groups, expiredTokenData2.expiresAt);
        assertThat(foundExpired2, is(equalTo(false)));
    }

    private boolean insertRowToDb(long userId, String tokenHash, Set<String> groups,
            long expiresAt) throws SQLException {
        Instant expiresAtInstant = Instant.ofEpochSecond(expiresAt);
        int updatedRows;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(SQL_INSERT)) {
            conn.setAutoCommit(true);
            statement.setLong(1, userId);
            statement.setString(2, tokenHash);
            Array groupsArray = conn.createArrayOf(VARCHAR.name(), groups.toArray());
            statement.setArray(3, groupsArray);
            statement.setTimestamp(4, Timestamp.from(expiresAtInstant));
            updatedRows = statement.executeUpdate();
        }
        return updatedRows == 1;
    }

    private boolean findRowInDb(long userId, String tokenHash, Set<String> groups,
            long expiresAt) throws SQLException {
        Instant expiresAtInstant = Instant.ofEpochSecond(expiresAt);
        long resultCount = 0;
        Set<String> resultGroups = null;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(SQL_SELECT_GROUPS_EXACTLY)) {
            conn.setAutoCommit(true);
            statement.setLong(1, userId);
            statement.setString(2, tokenHash);
            statement.setTimestamp(3, Timestamp.from(expiresAtInstant));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    resultCount++;
                    resultGroups = toStringSet(resultSet.getArray(1));
                }
            }
        }

        return resultCount == 1 && resultGroups.equals(groups);
    }

    private static Response givenPostToEndpoint(TokenData tokenData) {
        return given().contentType(ContentType.JSON).body(tokenData).when().post(RESOURCE_BASE_URI);
    }

    private static Response givenGetFromEndpoint(long userId, String tokenHash) {
        var uri = UriBuilder.fromUri(RESOURCE_BASE_URI).path(POP_GROUPS_PATH);
        if (tokenHash != null) {
            uri.queryParam(POP_GROUPS_QUERY_PARAM, tokenHash);
        }
        return given().contentType(ContentType.TEXT).when().get(uri.build(userId));
    }

    private static Response givenDeleteFromEndpoint() {
        var uri = UriBuilder.fromUri(RESOURCE_BASE_URI).path(DELETE_EXPIRED_PATH).build();
        return given().contentType(ContentType.TEXT).when().delete(uri);
    }

    private static TokenData tokenData() {
        var userId = new Random().nextLong();
        var tokenHash = UUID.randomUUID().toString();
        var groups = Set.of("group-1", "group-2");
        long expiresAt = Instant.now().plusSeconds(30).getEpochSecond();
        return new TokenData(userId, tokenHash, groups, expiresAt);
    }

    private static TokenData tokenDataWithoutHash() {
        var userId = new Random().nextLong();
        var groups = Set.of("group-1", "group-2");
        long expiresAt = Instant.now().plusSeconds(30).getEpochSecond();
        return new TokenData(userId, null, groups, expiresAt);
    }

    private static TokenData tokenDataWithoutGroups() {
        var userId = new Random().nextLong();
        var tokenHash = UUID.randomUUID().toString();
        long expiresAt = Instant.now().plusSeconds(30).getEpochSecond();
        return new TokenData(userId, tokenHash, null, expiresAt);
    }

    private static TokenData expiredTokenData() {
        var userId = new Random().nextLong();
        var groups = Set.of("group-1", "group-2");
        var tokenHash = UUID.randomUUID().toString();
        long expiresAt = Instant.now().minusSeconds(300).getEpochSecond();
        return new TokenData(userId, tokenHash, groups, expiresAt);
    }

    private static Set<String> toStringSet(Array sqlArray) throws SQLException {
        var objectArray = (Object[]) sqlArray.getArray();
        return Stream.of(objectArray).map(String.class::cast).collect(Collectors.toSet());
    }
}
