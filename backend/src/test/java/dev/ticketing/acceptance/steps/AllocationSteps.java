package dev.ticketing.acceptance.steps;

import dev.ticketing.acceptance.client.AllocationClient;
import dev.ticketing.acceptance.client.SiteClient;
import dev.ticketing.acceptance.client.model.TestResponse;
import dev.ticketing.acceptance.context.TestContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RequiredArgsConstructor
public class AllocationSteps {

    private final AllocationClient allocationClient;
    private final SiteClient siteClient;
    private final TestContext testContext;

    @Given("사용자가 주어진 경기의 {string} 구역 {int}행 {int}열 좌석을 점유 중이다.")
    public void userHoldsSeat(String blockName, int row, int col) {
        requestHoldSeat(blockName, row, col);
        assertThat(testContext.getStatusCode()).isEqualTo(HttpStatus.OK.value());
    }

    @When("사용자가 주어진 경기의 {string} 구역 {int}행 {int}열 좌석 점유를 요청하면,")
    public void requestHoldSeat(String blockName, int row, int col) {
        Long matchId = testContext.getMatchId(0L);
        Long blockId = findBlockIdByName(blockName);
        Long seatId = findSeatId(blockId, row, col);
        
        testContext.addHeldSeatId(seatId);
        testContext.setLastHeldPosition(blockId, row, col);

        TestResponse response = allocationClient.holdSeat(matchId, seatId, testContext.getCurrentUserId());
        testContext.setResponse(response);
    }

    @When("사용자가 주어진 경기의 {string} 구역 {int}행 {int}열 좌석 반납 요청하면,")
    public void requestReleaseSeat(String blockName, int row, int col) {
        Long matchId = testContext.getMatchId(0L);
        Long blockId = findBlockIdByName(blockName);
        Long seatId = findSeatId(blockId, row, col);

        TestResponse response = allocationClient.releaseSeat(matchId, seatId, testContext.getCurrentUserId());
        testContext.setResponse(response);
    }

    @And("해당 좌석은 다시 점유 가능한 상태가 되어야 한다.")
    public void verifySeatIsAvailable() {
        Long matchId = testContext.getMatchId(0L);
        Long seatId = testContext.getLastHeldSeatId();

        TestResponse response = allocationClient.holdSeat(matchId, seatId, testContext.getCurrentUserId());
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    @Then("좌석 점유에 성공해야 한다.")
    public void verifyHoldSuccess() {
        assertThat(testContext.getStatusCode()).isEqualTo(HttpStatus.OK.value());
    }

    @Then("좌석 점유에 실패해야 한다.")
    public void verifyHoldFailure() {
        assertThat(testContext.getStatusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Then("좌석 반납에 성공해야 한다.")
    public void verifyReleaseSuccess() {
        assertThat(testContext.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
    }

    @Then("좌석 반납에 실패해야 한다.")
    public void verifyReleaseFailure() {
        assertThat(testContext.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @And("다른 사용자가 주어진 경기의 {string} 구역 {int}행 {int}열 좌석 점유를 요청하면,")
    public void otherUserRequestHold(String blockName, int row, int col) {
        Long matchId = testContext.getMatchId(0L);
        Long blockId = findBlockIdByName(blockName);
        Long seatId = findSeatId(blockId, row, col);
        TestResponse response = allocationClient.holdSeat(matchId, seatId, testContext.getCurrentUserId());
        testContext.setResponse(response);
    }

    @When("다른 사용자가 주어진 경기의 {string} 구역 {int}행 {int}열 좌석 반납 요청하면,")
    public void otherUserRequestRelease(String blockName, int row, int col) {
        Long matchId = testContext.getMatchId(0L);
        Long blockId = findBlockIdByName(blockName);
        Long seatId = findSeatId(blockId, row, col);
        TestResponse response = allocationClient.releaseSeat(matchId, seatId, testContext.getCurrentUserId());
        testContext.setResponse(response);
    }

    /**
     * 구역 이름을 기반으로 blockId를 찾습니다.
     */
    private Long findBlockIdByName(String blockName) {
        TestResponse areasResponse = siteClient.getAreas();
        List<Integer> areaIds = areasResponse.jsonPath().getList("data.areas[*].id");

        if (areaIds == null || areaIds.isEmpty()) {
            throw new RuntimeException("No areas found in response");
        }

        for (Integer areaId : areaIds) {
            TestResponse sectionsResponse = siteClient.getSections(Long.valueOf(areaId));
            List<Integer> sectionIds = sectionsResponse.jsonPath().getList("data.sections[*].id");

            if (sectionIds == null) {
                continue;
            }

            for (Integer sectionId : sectionIds) {
                TestResponse blocksResponse = siteClient.getBlocks(Long.valueOf(sectionId));
                Object blockIdObj = blocksResponse.jsonPath().get("data.blocks[?(@.name == '" + blockName + "')].id");
                if (blockIdObj != null) {
                    return Long.valueOf(blockIdObj.toString());
                }
            }
        }
        log.error("[ERROR] Block not found by name: {}", blockName);
        throw new RuntimeException("Block not found by name: " + blockName);
    }

    /**
     * Bridge Logic: 구역/행/열 위치 정보를 기반으로 실제 DB의 seatId를 찾아 반환합니다.
     */
    private Long findSeatId(Long blockId, int row, int col) {
        TestResponse response = siteClient.getSeats(blockId);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

        return Long.valueOf(response.jsonPath()
                .get("data.seats[?(@.rowNumber == " + row + " && @.seatNumber == " + col + ")].id")
                .toString());
    }
}
