package dev.ticketing.acceptance.steps;

import dev.ticketing.acceptance.client.MatchClient;
import dev.ticketing.acceptance.client.model.TestResponse;
import dev.ticketing.acceptance.context.TestContext;
import dev.ticketing.core.match.application.port.out.persistence.RecordMatchPort;
import dev.ticketing.core.match.domain.Match;


import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class MatchSteps {

    private final MatchClient matchClient;
    private final TestContext testContext;
    private final RecordMatchPort recordMatchPort;

    // 경기 목록 조회

    @Given("일시 {string}에 장소 {string}에서 연고 {string} 대 원정 {string}의 경기가 있다.")
    public void registerMatch(String dateTimeStr, String stadium, String homeTeam, String awayTeam) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, formatter);
        Match match = Match.create(stadium, homeTeam, awayTeam, dateTime);
        Match recorded = recordMatchPort.record(match);
        // alias 0L로 저장하여 기본 경기로 사용
        testContext.saveMatchId(0L, recorded.getId()); 
    }

    @When("경기 목록을 조회하면,")
    public void getMatches() {
        TestResponse response = matchClient.getMatches();
        testContext.setResponse(response);
    }

    @Then("{int}개의 경기가 조회되어야 한다.")
    public void verifyMatchesCount(int expectedCount) {
        TestResponse response = testContext.getResponse();
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<Object> matches = response.jsonPath().getList("data.matches");
        assertThat(matches).hasSize(expectedCount);
    }
}
