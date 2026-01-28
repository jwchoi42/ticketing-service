package dev.ticketing.acceptance.steps;

import dev.ticketing.acceptance.client.PaymentClient;
import dev.ticketing.acceptance.client.ReservationClient;
import dev.ticketing.acceptance.client.model.TestResponse;
import dev.ticketing.acceptance.context.TestContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import io.cucumber.spring.ScenarioScope;

import static org.assertj.core.api.Assertions.assertThat;

@ScenarioScope
@RequiredArgsConstructor
public class PaymentSteps {

    private final PaymentClient paymentClient;
    private final ReservationClient reservationClient;
    private final TestContext testContext;
    private Long lastPaymentId;

    @When("예약에 대해 결제를 요청하면")
    public void requestPayment() {
        Long reservationId = testContext.getLastReservationId();
        TestResponse response = paymentClient.requestPayment(reservationId, 15000, "CARD"); // Example amount
        testContext.setResponse(response);
    }

    @Then("결제 정보가 생성되어야 한다.")
    public void verifyPaymentCreated() {
        assertThat(testContext.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        lastPaymentId = Long.valueOf(testContext.getResponse().jsonPath().get("data.id").toString());
    }

    @When("결제를 완료하면")
    public void confirmPayment() {
        String paymentKey = UUID.randomUUID().toString();
        String orderId = UUID.randomUUID().toString();
        TestResponse response = paymentClient.confirmPayment(lastPaymentId, paymentKey, orderId, 15000);
        testContext.setResponse(response);
    }

    @And("결제 상태는 {string} 상태여야 한다.")
    public void verifyPaymentStatus(String status) {
        TestResponse response = testContext.getResponse();
        String currentStatus = response.jsonPath().getString("data.status");
        assertThat(currentStatus).isEqualTo(status);
    }

    @And("예약 상태는 {string} 상태여야 한다.")
    public void verifyReservationStatusChanges(String status) {
        // Refresh reservation
        Long reservationId = testContext.getLastReservationId();
        TestResponse response = reservationClient.getReservation(reservationId);
        testContext.setResponse(response);

        TestResponse currentResponse = testContext.getResponse();
        String currentStatus = currentResponse.jsonPath().getString("data.status");
        assertThat(currentStatus).isEqualTo(status);
    }
}
