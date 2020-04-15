package de.dopler.ms;

import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

public class LoginResourceTest {

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
