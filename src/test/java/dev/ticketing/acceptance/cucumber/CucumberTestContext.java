package dev.ticketing.acceptance.cucumber;

import io.restassured.response.Response;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CucumberTestContext {

    private Response response;

    public int getStatusCode() {
        return response.getStatusCode();
    }

    public <T> T getBody(Class<T> classType) {
        return response.as(classType);
    }

    public String getStringFromJsonPath(String jsonPath) {
        return response.jsonPath().getString(jsonPath);
    }
}
