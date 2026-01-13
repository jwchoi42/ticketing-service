package dev.ticketing.acceptance.steps;

import dev.ticketing.acceptance.client.AllocationClient;
import dev.ticketing.acceptance.client.AllocationStatusClient;
import dev.ticketing.acceptance.client.SiteClient;
import dev.ticketing.acceptance.client.model.TestResponse;
import dev.ticketing.acceptance.context.TestContext;
import dev.ticketing.core.site.application.service.AllocationStatusService;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@RequiredArgsConstructor
public class AllocationStatusSteps {

    private final AllocationStatusClient statusStreamClient;
    private final SiteClient siteClient;
    private final AllocationClient allocationClient;
    private final TestContext testContext;
    private final AllocationStatusService allocationStatusService;

    private Flux<ServerSentEvent<String>> eventStream;
    private final List<ServerSentEvent<String>> receivedEvents = new CopyOnWriteArrayList<>();
    private Long currentMatchId;
    private Long currentBlockId;
    private Long lastHeldSeatId;

    //

    private void setUpBlockId(String areaType, String sectionType, String blockName) {
        currentMatchId = testContext.getMatchId(0L);
        // blockName format: "내야-연고-1" -> parse the number from the end
        String[] parts = blockName.split("-");
        String blockNum = parts[parts.length - 1];
        int blockIndex = Integer.parseInt(blockNum) - 1;
        currentBlockId = getBlockIdByNumber(currentMatchId, areaType, sectionType, blockIndex);
    }

    //

    @When("주어진 경기의 {string} 영역, {string} 진영, {string} 구역의 좌석 현황을 조회하면,")
    public void subscribeAndVerifyInitial(String areaType, String sectionType, String blockName) {
        setUpBlockId(areaType, sectionType, blockName);
        subscribeCurrentBlockStream();
    }

    @Then("현재 해당 구역의 모든 좌석 상태를 수신한다.")
    public void verifyInitialEventReceived() {
        ServerSentEvent<String> initialEvent = receivedEvents.stream()
                .filter(e -> "snapshot".equals(e.event()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("snapshot 이벤트를 받지 못했습니다."));

        testContext.set("lastSnapShot", initialEvent.data());
        assertThat(initialEvent.data()).contains("\"status\":\"AVAILABLE\"");
    }

    //

    @Given("주어진 경기의 {string} 영역, {string} 진영, {string} 구역의 좌석 현황을 조회한 상태에서,")
    public void subscribeOnly(String areaType, String sectionType, String blockName) {
        setUpBlockId(areaType, sectionType, blockName);
        subscribeCurrentBlockStream();
    }

    @And("1초 후에, 해당 구역의 좌석 상태 변경 사항을 수신한다.")
    public void verifyUpdateEventAndCheck() {
        log.info("[TEST] 변경 사항 수신 확인 시작. 현재 이벤트수: {}", receivedEvents.size());
        // 스케줄러를 수동으로 트리거하여 변경 사항을 감지하도록 함
        await().atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    log.info("[TEST] checkForUpdates 호출 전 이벤트수: {}", receivedEvents.size());
                    allocationStatusService.checkForUpdates(); // 수동 트리거
                    log.info("[TEST] checkForUpdates 호출 후 이벤트수: {}", receivedEvents.size());
                    assertThat(receivedEvents.stream().anyMatch(e -> "changes".equals(e.event())))
                            .withFailMessage("changes 이벤트를 받지 못했습니다. 받은 이벤트: " + receivedEvents)
                            .isTrue();
                });
        verifyChangesReceived();
    }

    private void verifyChangesReceived() {
        boolean hasUpdate = receivedEvents.stream()
                .anyMatch(e -> "changes".equals(e.event()));

        assertThat(hasUpdate).withFailMessage("changes 이벤트를 받지 못했습니다.").isTrue();
    }

    @And("주어진 경기의 {string} 구역 {int}행 {int}열 좌석의 상태가 {string}인 것을 확인한다.")
    public void verifySpecificSeatStatus(String blockName, int row, int col, String status) {
        TestResponse blockResponse = siteClient.getSeats(currentBlockId);
        Long seatId = Long.valueOf(blockResponse.jsonPath()
                .get("data.seats[?(@.rowNumber == " + row + " && @.seatNumber == " + col + ")].id").toString());
        String mappedStatus = mapStatus(status);

        log.info("[TEST] 좌석 상태 확인: seatId={}, 예상 상태={}, 받은 이벤트 수={}", seatId, mappedStatus, receivedEvents.size());

        // 1. 먼저 changes 이벤트에서 확인
        boolean foundInChanges = receivedEvents.stream()
                .anyMatch(e -> "changes".equals(e.event()) &&
                        e.data() != null &&
                        e.data().contains("\"seatId\":" + seatId) &&
                        e.data().contains("\"status\":\"" + mappedStatus + "\""));

        if (foundInChanges) {
            log.info("[TEST] changes 이벤트에서 좌석 상태 확인됨: seatId={}, 상태={}", seatId, mappedStatus);
            return;
        }

        // 2. changes에 없으면 snapshot 이벤트(스냅샷)에서 확인
        String lastSnapShot = testContext.get("lastSnapShot");
        if (lastSnapShot == null) {
            ServerSentEvent<String> initialEvent = receivedEvents.stream()
                    .filter(e -> "snapshot".equals(e.event()))
                    .findFirst()
                    .orElse(null);
            if (initialEvent != null) {
                lastSnapShot = initialEvent.data();
            }
        }

        if (lastSnapShot != null) {
            // 스냅샷에서 해당 좌석의 상태 확인
            boolean foundInSnapshot = lastSnapShot.contains("\"id\":" + seatId) &&
                    lastSnapShot.contains("\"status\":\"" + mappedStatus + "\"");
            if (foundInSnapshot) {
                log.info("[TEST] snapshot 이벤트(스냅샷)에서 좌석 상태 확인됨: seatId={}, 상태={}", seatId, mappedStatus);
                return;
            }
        }

        log.error("[TEST] 좌석 상태 확인 실패: seatId={}, 예상 상태={}, 스냅샷={}", seatId, mappedStatus, lastSnapShot);
        throw new AssertionError("해당 좌석의 " + status + " 상태를 확인하지 못했습니다. Events: " + receivedEvents);
    }

    // ?

    @When("해당 구간의 좌석 현황 실시간 스트림을 구독하면")
    public void subscribeCurrentBlockStream() {
        receivedEvents.clear();
        eventStream = statusStreamClient.subscribeSeatStatusStream(currentMatchId, currentBlockId);

        // 비동기로 이벤트 수집 시작
        eventStream.take(Duration.ofSeconds(5))
                .doOnNext(receivedEvents::add)
                .subscribe();

        // snapshot 이벤트를 받을 때까지 대기
        await().atMost(Duration.ofSeconds(2))
                .pollInterval(Duration.ofMillis(100))
                .until(() -> receivedEvents.stream().anyMatch(e -> "snapshot".equals(e.event())));
    }

    private String mapStatus(String status) {
        return switch (status) {
            case "점유" -> "HOLD";
            case "이용 가능" -> "AVAILABLE";
            case "예약됨", "예약", "OCCUPIED" -> "OCCUPIED";
            default -> status;
        };
    }

    private Long getBlockIdByNumber(Long matchId, String areaType, String sectionType, int blockIndex) {
        String mappedArea = mapArea(areaType);
        String mappedSection = mapSection(sectionType);

        TestResponse response = siteClient.getAreas();
        Long areaId = Long.valueOf(response.jsonPath()
                .get("data.areas[?(@.name == '" + mappedArea + "')].id").toString());

        TestResponse sectionResponse = siteClient.getSections(areaId);
        Long sectionId = Long.valueOf(sectionResponse.jsonPath()
                .get("data.sections[?(@.name == '" + mappedSection + "')].id").toString());

        TestResponse blockResponse = siteClient.getBlocks(sectionId);
        return Long.valueOf(blockResponse.jsonPath().get("data.blocks[" + blockIndex + "].id").toString());
    }

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
