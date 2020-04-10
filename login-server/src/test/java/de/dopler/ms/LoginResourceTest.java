package de.dopler.ms;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class LoginResourceTest {

    @Test
    public void loginEndpoint() {
        // @formatter:off
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/login")
        .then()
            .statusCode(200)
            .body(is("login"));
        // @formatter:on
    }
}
