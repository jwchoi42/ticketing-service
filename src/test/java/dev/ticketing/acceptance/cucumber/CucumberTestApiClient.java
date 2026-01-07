package dev.ticketing.acceptance.cucumber;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CucumberTestApiClient {

    private final String baseUrl;
    private final CucumberTestContext cucumberTestContext;

    public void performHealthCheck() {
        Response response = RestAssured
                .given()
                .baseUri(baseUrl)
                .when()
                .get("/actuator/health");

        cucumberTestContext.setResponse(response);
    }
}
