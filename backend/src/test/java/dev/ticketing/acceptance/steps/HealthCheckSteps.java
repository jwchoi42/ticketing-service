package dev.ticketing.acceptance.steps;

import dev.ticketing.acceptance.client.HealthCheckClient;
import dev.ticketing.acceptance.client.model.TestResponse;
import dev.ticketing.acceptance.context.TestContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class HealthCheckSteps {

    private final HealthCheckClient healthClient;
    private final TestContext testContext;

    @When("스프링 부트 액추에이터로 시스템 상태를 확인하면,")
    public void performHealthCheck() {
        TestResponse response = healthClient.performHealthCheck();
        testContext.setResponse(response);
    }

    @Then("스프링 애플리케이션 상태가 정상이어야 하고,")
    public void verifyApplicationHealth() {
        TestResponse response = testContext.getResponse();
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("status")).isEqualTo("UP");
    }

    @Then("데이터베이스도 정상적으로 연결되있어야 한다.")
    public void verifyDatabaseHealth() {
        TestResponse response = testContext.getResponse();
        assertThat(response.jsonPath().getString("components.db.status")).isEqualTo("UP");
    }
}
