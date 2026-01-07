package dev.ticketing.acceptance.steps;

import dev.ticketing.acceptance.cucumber.CucumberTestApiClient;
import dev.ticketing.acceptance.cucumber.CucumberTestContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class HealthCheckSteps {

    private final CucumberTestApiClient cucumberTestApiClient;
    private final CucumberTestContext cucumberTestContext;

    @When("스프링 부트 액추에이터로 시스템 상태를 확인하면,")
    public void performHealthCheck() {
        cucumberTestApiClient.performHealthCheck();
    }

    @Then("스프링 애플리케이션 상태가 정상이어야 하고,")
    public void verifyApplicationIsHealthy() {
        assertThat(cucumberTestContext.getStringFromJsonPath("status")).isEqualTo("UP");
    }

    @And("데이터베이스도 정상적으로 연결되있어야 한다.")
    public void verifyDatabaseIsHealthy() {
        assertThat(cucumberTestContext.getStringFromJsonPath("components.db.status")).isEqualTo("UP");
    }
}
