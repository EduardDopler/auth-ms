package de.dopler.ms.jwt_server;

import de.dopler.ms.jwt_server.domain.TokenData;
import de.dopler.ms.jwt_server.services.external.TokenStoreService;
import de.dopler.ms.jwt_server.utils.GenerateTokenUtils;
import de.dopler.ms.response_utils.RefreshTokenCookie;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static de.dopler.ms.jwt_server.utils.GenerateTokenUtils.EXPIRATION_ACCESS_TOKEN;
import static de.dopler.ms.jwt_server.utils.GenerateTokenUtils.SUBJECT_ACCESS;
import static io.restassured.RestAssured.given;
import static io.restassured.matcher.RestAssuredMatchers.detailedCookie;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;

@QuarkusTest
class RefreshTokenResourceTest {

    private static final URI resourceBaseURI = URI.create("/auth/refresh");

    @Inject
    JWTAuthContextInfo authContextInfo;

    @InjectMock
    @RestClient
    TokenStoreService tokenStoreService;

    @InjectMock
    JsonWebToken jwt;

    private long userId;
    private Set<String> groups;

    @BeforeEach
    void setUp() {
        // tokenStoreService.store
        Mockito.when(tokenStoreService.store(Mockito.any(TokenData.class)))
                .thenReturn(javax.ws.rs.core.Response.noContent().build());

        // tokenStoreService.popGroups
        groups = Set.of("groupA", "groupB", "groupC");
        Mockito.when(tokenStoreService.popGroups(Mockito.anyLong(), Mockito.anyString()))
                .thenReturn(javax.ws.rs.core.Response.ok(groups).build());

        // JWT
        userId = new Random().nextLong();
        Mockito.when(jwt.getName()).thenReturn(String.valueOf(userId));
        Mockito.when(jwt.getSubject()).thenReturn(GenerateTokenUtils.SUBJECT_REFRESH);
        Mockito.when(jwt.getRawToken()).thenReturn("raw.token.value");
    }

    @Test
    void fromRefreshTokenEndpointReturnsCode200() {
        // @formatter:off
        givenPostToEndpoint().then()
            .statusCode(Status.OK.getStatusCode());
        // @formatter:on
    }

    @Test
    void fromRefreshTokenEndpointHasPrivateCacheControlHeader() {
        // @formatter:off
        givenPostToEndpoint().then()
            .header(HttpHeaders.CACHE_CONTROL, is(notNullValue()))
            .header(HttpHeaders.CACHE_CONTROL,
                    s -> Stream.of(s.split(",")).map(String::trim).toArray(),
                    arrayContainingInAnyOrder("private", "no-store", "no-cache"));
        // @formatter:on
    }

    @Test
    void fromRefreshTokenEndpointHasGoodSetCookieHeader() {
        // @formatter:off
        givenPostToEndpoint().then()
            // Set-Cookie header is set
            .cookie(RefreshTokenCookie.NAME)
            // a JWT has 3 parts, separated by 2 dots
            .cookie(RefreshTokenCookie.NAME, detailedCookie().value(
                    stringContainsInOrder(".", ".")))
            // cookie has these cookie properties set
            .cookie(RefreshTokenCookie.NAME, detailedCookie().httpOnly(true))
            .cookie(RefreshTokenCookie.NAME, detailedCookie().sameSite("Strict"))
            .cookie(RefreshTokenCookie.NAME, detailedCookie().path(is(not(equalTo("/")))))
            .cookie(RefreshTokenCookie.NAME, detailedCookie().maxAge(
                    is(equalTo(GenerateTokenUtils.EXPIRATION_REFRESH_TOKEN))));
        // @formatter:on
    }

    @Test
    void fromRefreshTokenEndpointHasGoodJwtResponseInBody() {
        int expectedExpiresAt = (int) Instant.now(Clock.systemDefaultZone())
                .plusSeconds(EXPIRATION_ACCESS_TOKEN)
                .getEpochSecond();
        var minExpiresAt = expectedExpiresAt - 20;
        var maxExpiresAt = expectedExpiresAt + 20;
        // @formatter:off
        givenPostToEndpoint().then()
            .body("userId", is(equalTo(userId)))
            .body("accessToken", stringContainsInOrder(".", "."))
            .body("refreshToken", is(nullValue()))
            .body("expiresAt", is(greaterThanOrEqualTo(minExpiresAt)))
            .body("expiresAt", is(lessThanOrEqualTo(maxExpiresAt)));
        // @formatter:on
    }

    @Test
    void fromRefreshTokenEndpointGeneratesValidAccessToken() {
        Response response = givenPostToEndpoint().then().extract().response();

        JWTParser jwtParser = new DefaultJWTParser(authContextInfo);
        JsonWebToken accessToken = null;
        try {
            accessToken = jwtParser.parse(response.path("accessToken"));
        } catch (ParseException e) {
            fail("invalid JWT instead of access token.", e);
        }

        assertThat(accessToken.getTokenID(), is(not(blankOrNullString())));

        long expiresAtInJsonResponse = response.jsonPath().getLong("expiresAt");
        long expiresAtInAccessToken = accessToken.getExpirationTime();
        assertThat("expiresAt values in JWT and JSON response object must be equal",
                expiresAtInJsonResponse, is(equalTo(expiresAtInAccessToken)));

        assertThat(accessToken.getIssuer(), is(equalTo(GenerateTokenUtils.ISSUER)));
        assertThat(accessToken.getSubject(), is(equalTo(GenerateTokenUtils.SUBJECT_ACCESS)));
        assertThat(Long.valueOf(accessToken.getName()), is(equalTo(userId)));
        assertThat(accessToken.getGroups(), containsInAnyOrder(groups.toArray()));
    }

    @Test
    void fromRefreshTokenEndpointReturnsCode400OnInvalidTokenSubject() {
        Mockito.when(jwt.getSubject()).thenReturn(SUBJECT_ACCESS);
        givenPostToEndpoint().then().statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void fromRefreshTokenEndpointReturnsCode400OnMissingTokenName() {
        Mockito.when(jwt.getName()).thenReturn(null);
        givenPostToEndpoint().then().statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void fromRefreshTokenEndpointReturnsCode400IfUserIdIsNoLong() {
        Mockito.when(jwt.getName()).thenReturn("invalid-user-id");
        givenPostToEndpoint().then().statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void fromRefreshTokenEndpointReturnsCode400AndDeleteCookieIfTokenStoreReturnsNoGroups() {
        Mockito.when(tokenStoreService.popGroups(Mockito.anyLong(), Mockito.anyString()))
                .thenReturn(javax.ws.rs.core.Response.status(Status.NOT_FOUND).build());

        // @formatter:off
        givenPostToEndpoint().then()
            .statusCode(Status.BAD_REQUEST.getStatusCode())
            // Set-Cookie header is set
            .cookie(RefreshTokenCookie.NAME)
            // a delete-cookie's value is an empty string
            .cookie(RefreshTokenCookie.NAME, detailedCookie().value(""))
            // cookie has these cookie properties set
            .cookie(RefreshTokenCookie.NAME, detailedCookie().httpOnly(true))
            .cookie(RefreshTokenCookie.NAME, detailedCookie().sameSite("Strict"))
            .cookie(RefreshTokenCookie.NAME, detailedCookie().path(is(not(equalTo("/")))))
            // a delete-cookie has a max-age of 0
            .cookie(RefreshTokenCookie.NAME, detailedCookie().maxAge(is(equalTo(0))));
        // @formatter:on
    }

    @Test
    void fromRefreshTokenEndpointReturnsCode500IfTokenStoreReturnsServerError() {
        Mockito.when(tokenStoreService.popGroups(Mockito.anyLong(), Mockito.anyString()))
                .thenReturn(javax.ws.rs.core.Response.status(Status.INTERNAL_SERVER_ERROR).build());

        givenPostToEndpoint().then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    private static Response givenPostToEndpoint() {
        return given().contentType(ContentType.JSON).when().post(resourceBaseURI);
    }
}
