package de.dopler.ms.jwt_server;

import de.dopler.ms.jwt_server.domain.User;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static de.dopler.ms.jwt_server.GenerateTokenService.EXPIRATION_ACCESS_TOKEN;
import static de.dopler.ms.jwt_server.utils.ResponseUtils.REFRESH_TOKEN_COOKIE_NAME;
import static io.restassured.RestAssured.given;
import static io.restassured.matcher.RestAssuredMatchers.detailedCookie;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;

@QuarkusTest
class GenerateTokenResourceTest {

    private static final URI resourceBaseURI = URI.create("/auth/generate");

    @Inject
    private JWTAuthContextInfo authContextInfo;

    @Test
    void forUserEndpointReturnsCode200() {
        // @formatter:off
        givenPostToEndpoint(randomUser()).then()
            .statusCode(Status.OK.getStatusCode());
        // @formatter:on
    }

    @Test
    void forUserEndpointHasPrivateCacheControlHeader() {
        String[] neededValues = {"private", "no-store", "no-cache"};
        // @formatter:off
        givenPostToEndpoint(randomUser()).then()
            .header(HttpHeaders.CACHE_CONTROL,
                    s -> Stream.of(s.split(",")).map(String::trim).toArray(),
                    arrayContainingInAnyOrder(neededValues));
        // @formatter:on
    }

    @Test
    void forUserEndpointHasGoodSetCookieHeader() {
        // @formatter:off
        givenPostToEndpoint(randomUser()).then()
            // Set-Cookie header is set
            .cookie(REFRESH_TOKEN_COOKIE_NAME)
            // a JWT has 3 parts, separated by 2 dots
            .cookie(REFRESH_TOKEN_COOKIE_NAME, detailedCookie().value(
                    stringContainsInOrder(".", ".")))
            // cookie has these cookie properties set
            .cookie(REFRESH_TOKEN_COOKIE_NAME, detailedCookie().httpOnly(true))
            .cookie(REFRESH_TOKEN_COOKIE_NAME, detailedCookie().sameSite("Strict"))
            .cookie(REFRESH_TOKEN_COOKIE_NAME, detailedCookie().path(is(not(equalTo("/")))))
            .cookie(REFRESH_TOKEN_COOKIE_NAME, detailedCookie().maxAge(
                    is(equalTo(GenerateTokenService.EXPIRATION_REFRESH_TOKEN))));
        // @formatter:on
    }

    @Test
    void forUserEndpointHasGoodJwtResponseInBody() {
        int expectedExpiresAt = (int) Instant.now(Clock.systemDefaultZone())
                .plusSeconds(EXPIRATION_ACCESS_TOKEN)
                .getEpochSecond();
        var minExpiresAt = expectedExpiresAt - 20;
        var maxExpiresAt = expectedExpiresAt + 20;
        // @formatter:off
        givenPostToEndpoint(randomUser()).then()
            .body("accessToken", stringContainsInOrder(".", "."))
            .body("refreshToken", is(nullValue()))
            .body("expiresAt", is(greaterThanOrEqualTo(minExpiresAt)))
            .body("expiresAt", is(lessThanOrEqualTo(maxExpiresAt)));
        // @formatter:on
    }

    @Test
    void forUserEndpointGeneratesValidAccessToken() {
        User inputUser = randomUser();
        Response response = givenPostToEndpoint(inputUser).then().extract().response();

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

        assertThat(accessToken.getIssuer(), is(equalTo(GenerateTokenService.ISSUER)));
        assertThat(accessToken.getSubject(), is(equalTo(GenerateTokenService.SUBJECT_ACCESS)));
        assertThat(accessToken.getName(), is(equalTo(inputUser.id)));
        assertThat(accessToken.getGroups(), containsInAnyOrder(inputUser.groups.toArray()));
    }

    @Test
    void forUserEndpointReturnsCode400OnInvalidUser() {
        User userWithNullId = new User(null, Set.of("group1"));
        User userWithNullGroups = new User(UUID.randomUUID().toString(), null);
        // @formatter:off
        givenPostToEndpoint(userWithNullId).then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
        givenPostToEndpoint(userWithNullGroups).then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
        // post without body
        given().contentType(ContentType.JSON).when().post(resourceBaseURI).then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
        // @formatter:on
    }

    private static Response givenPostToEndpoint(User user) {
        return given().contentType(ContentType.JSON).body(user).when().post(resourceBaseURI);
    }

    private static User randomUser() {
        return new User(UUID.randomUUID().toString(), Set.of("group1", "group2", "group3"));
    }
}
