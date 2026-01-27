package dev.ticketing.acceptance.steps;

import dev.ticketing.acceptance.client.AllocationClient;
import dev.ticketing.acceptance.client.ReservationClient;
import dev.ticketing.acceptance.client.SiteClient;
import dev.ticketing.acceptance.client.model.TestResponse;
import dev.ticketing.acceptance.context.TestContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.List;

import io.cucumber.spring.ScenarioScope;

import static org.assertj.core.api.Assertions.assertThat;

@ScenarioScope
@RequiredArgsConstructor
public class ReservationSteps {

    private final ReservationClient reservationClient;
    private final AllocationClient allocationClient;
    private final SiteClient siteClient;
    private final TestContext testContext;

    //

    @When("점유 중인 좌석에 대해 선택을 확정하면,")
    public void createReservation() {
        Long internalMatchId = testContext.getMatchIdMap().values().iterator().next();
        TestResponse response = reservationClient.createReservation(testContext.getCurrentUserId(), internalMatchId,
                testContext.getHeldSeatIds());
        testContext.setResponse(response);
    }

    @Then("예약이 접수되어야 한다.")
    public void verifyReservationCreated() {
        assertThat(testContext.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        Long reservationId = Long.valueOf(testContext.getResponse().jsonPath().get("data.id").toString());
        testContext.setLastReservationId(reservationId);
    }

    @And("예약 상태는 {string}이어야 한다.")
    public void verifyReservationStatus(String status) {
        TestResponse response = testContext.getResponse();
        String currentStatus = response.jsonPath().getString("data.status");
        assertThat(currentStatus).isEqualTo(status);
    }

    @And("배정 받은 각 좌석의 예약 번호와 생성된 예약 번호가 일치해야 한다.")
    public void verifyAllocationReservationIds() {
        TestResponse response = testContext.getResponse();

        // Verify that the reservation contains the held seats
        List<Integer> seatIds = response.jsonPath().getList("data.seatIds");
        assertThat(seatIds).hasSameSizeAs(testContext.getHeldSeatIds());

        // Verify the reservation was created with the correct seats
        for (Long heldSeatId : testContext.getHeldSeatIds()) {
            assertThat(seatIds.stream().map(Long::valueOf)).contains(heldSeatId);
        }
    }

    //

    @And("점유한 좌석에 대해 예약을 요청했다.")
    public void createReservationPrecondition() {
        createReservation();
        assertThat(testContext.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());

        Long reservationId = Long.valueOf(testContext.getResponse().jsonPath().get("data.id").toString());
        testContext.setLastReservationId(reservationId);
    }

    //

    private String mapArea(String type) {
        return switch (type) {
            case "내야" -> "INFIELD";
            case "외야" -> "OUTFIELD";
            default -> type;
        };
    }

    private String mapSection(String type) {
        return switch (type) {
            case "연고" -> "HOME";
            case "원정" -> "AWAY";
            case "좌측" -> "LEFT";
            case "우측" -> "RIGHT";
            default -> type;
        };
    }
}
