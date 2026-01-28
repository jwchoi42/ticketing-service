package dev.ticketing.acceptance.steps;

import dev.ticketing.acceptance.client.AdminClient;
import dev.ticketing.acceptance.client.AllocationClient;
import dev.ticketing.acceptance.client.SiteClient;
import dev.ticketing.acceptance.client.UserClient;
import dev.ticketing.acceptance.client.model.TestResponse;
import dev.ticketing.acceptance.context.TestContext;
import dev.ticketing.core.user.application.port.out.RecordUserPort;
import dev.ticketing.core.user.domain.User;
import dev.ticketing.core.user.domain.UserRole;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.util.List;

import io.cucumber.spring.ScenarioScope;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ScenarioScope
@RequiredArgsConstructor
public class AdminSteps {

    private final AdminClient adminClient;
    private final UserClient userClient;
    private final AllocationClient allocationClient;
    private final SiteClient siteClient;
    private final TestContext testContext;
    private final RecordUserPort recordUserPort;

    private Long lastCreatedMatchId;

    @Given("관리자가 로그인하고,")
    public void adminLoggedIn() {
        // Create admin user directly via port (bypassing API)
        User adminUser = User.builder()
                .email("admin@email.com")
                .password("admin")
                .role(UserRole.ADMIN)
                .build();
        User savedAdmin = recordUserPort.record(adminUser);
        testContext.saveUserId("admin@email.com", savedAdmin.getId());
        testContext.setCurrentUserEmail("admin@email.com");
    }

    @Given("일반 사용자가 이메일 {string}로 로그인하고,")
    public void normalUserLoggedIn(String email) {
        userClient.signUp(email, "password");
        TestResponse response = userClient.logIn(email, "password");
        testContext.setResponse(response);
        assertThat(testContext.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        Long userId = Long.valueOf(testContext.getStringFromJsonPath("data.id"));
        testContext.saveUserId(email, userId);
        testContext.setCurrentUserEmail(email);
    }

    @When("관리자가 장소 {string}, 연고 {string}, 원정 {string}, 일시 {string}로 경기를 생성하면,")
    public void adminCreatesMatch(String stadium, String homeTeam, String awayTeam, String dateTime) {
        Long userId = testContext.getCurrentUserId();
        TestResponse response = adminClient.createMatch(userId, stadium, homeTeam, awayTeam, dateTime);
        testContext.setResponse(response);
        if (response.getStatusCode() == HttpStatus.CREATED.value()) {
            lastCreatedMatchId = Long.valueOf(response.jsonPath().getString("data.id"));
            testContext.saveMatchId(0L, lastCreatedMatchId);
        }
    }

    @When("일반 사용자가 장소 {string}, 연고 {string}, 원정 {string}, 일시 {string}로 경기를 생성하면,")
    public void normalUserCreatesMatch(String stadium, String homeTeam, String awayTeam, String dateTime) {
        Long userId = testContext.getCurrentUserId();
        TestResponse response = adminClient.createMatch(userId, stadium, homeTeam, awayTeam, dateTime);
        testContext.setResponse(response);
    }

    @Then("경기 생성에 성공한다.")
    public void verifyMatchCreationSuccess() {
        assertThat(testContext.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    @And("생성된 경기의 상태는 {string}이다.")
    public void verifyMatchStatus(String expectedStatus) {
        String status = testContext.getStringFromJsonPath("data.status");
        assertThat(status).isEqualTo(expectedStatus);
    }

    @When("관리자가 해당 경기의 장소를 {string}으로 수정하면,")
    public void adminUpdatesMatchStadium(String newStadium) {
        Long userId = testContext.getCurrentUserId();
        // Get current match info and update stadium
        TestResponse response = adminClient.updateMatch(
                userId, lastCreatedMatchId, newStadium, "LG 트윈스", "두산 베어스", "2026-05-01T18:30:00");
        testContext.setResponse(response);
    }

    @Then("경기 수정에 성공한다.")
    public void verifyMatchUpdateSuccess() {
        assertThat(testContext.getStatusCode()).isEqualTo(HttpStatus.OK.value());
    }

    @And("수정된 경기의 장소는 {string}이다.")
    public void verifyMatchStadium(String expectedStadium) {
        String stadium = testContext.getStringFromJsonPath("data.stadium");
        assertThat(stadium).isEqualTo(expectedStadium);
    }

    @When("관리자가 해당 경기를 삭제하면,")
    public void adminDeletesMatch() {
        Long userId = testContext.getCurrentUserId();
        TestResponse response = adminClient.deleteMatch(userId, lastCreatedMatchId);
        testContext.setResponse(response);
    }

    @Then("경기 삭제에 성공한다.")
    public void verifyMatchDeletionSuccess() {
        assertThat(testContext.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
    }

    @When("관리자가 해당 경기를 오픈하면,")
    public void adminOpensMatch() {
        Long userId = testContext.getCurrentUserId();
        TestResponse response = adminClient.openMatch(userId, lastCreatedMatchId);
        testContext.setResponse(response);
    }

    @When("관리자가 해당 경기를 다시 오픈하면,")
    public void adminOpensMatchAgain() {
        Long userId = testContext.getCurrentUserId();
        TestResponse response = adminClient.openMatch(userId, lastCreatedMatchId);
        testContext.setResponse(response);
    }

    @Then("경기 오픈에 성공한다.")
    public void verifyMatchOpenSuccess() {
        assertThat(testContext.getStatusCode()).isEqualTo(HttpStatus.OK.value());
    }

    @And("오픈된 경기의 상태는 {string}이다.")
    public void verifyOpenedMatchStatus(String expectedStatus) {
        String status = testContext.getStringFromJsonPath("data.status");
        assertThat(status).isEqualTo(expectedStatus);
    }

    @Then("경기 오픈에 실패한다.")
    public void verifyMatchOpenFailure() {
        assertThat(testContext.getStatusCode()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Then("권한 오류가 발생한다.")
    public void verifyUnauthorizedError() {
        assertThat(testContext.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @When("DRAFT 경기의 {string} 구역 {int}행 {int}열 좌석 점유를 요청하면,")
    public void userRequestsSeatAllocationForDraftMatch(String blockName, int row, int col) {
        Long matchId = testContext.getMatchId(0L);
        Long blockId = findBlockIdByName(blockName);
        Long seatId = findSeatId(blockId, row, col);

        TestResponse response = allocationClient.holdSeat(matchId, seatId, testContext.getCurrentUserId());
        testContext.setResponse(response);
    }

    @Then("경기 미오픈 오류가 발생한다.")
    public void verifyMatchNotOpenError() {
        assertThat(testContext.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

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

    private Long findSeatId(Long blockId, int row, int col) {
        TestResponse response = siteClient.getSeats(blockId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());

        return Long.valueOf(response.jsonPath()
                .get("data.seats[?(@.rowNumber == " + row + " && @.seatNumber == " + col + ")].id")
                .toString());
    }
}
