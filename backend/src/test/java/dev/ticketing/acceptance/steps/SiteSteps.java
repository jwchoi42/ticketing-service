package dev.ticketing.acceptance.steps;

import dev.ticketing.acceptance.client.SiteClient;
import dev.ticketing.acceptance.client.model.TestResponse;
import dev.ticketing.acceptance.context.TestContext;
import dev.ticketing.core.site.application.port.out.persistence.hierarchy.RecordAreaPort;
import dev.ticketing.core.site.application.port.out.persistence.hierarchy.RecordBlockPort;
import dev.ticketing.core.site.application.port.out.persistence.hierarchy.RecordSeatPort;
import dev.ticketing.core.site.application.port.out.persistence.hierarchy.RecordSectionPort;
import dev.ticketing.core.site.domain.hierarchy.Area;
import dev.ticketing.core.site.domain.hierarchy.Block;
import dev.ticketing.core.site.domain.hierarchy.Seat;
import dev.ticketing.core.site.domain.hierarchy.Section;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RequiredArgsConstructor
public class SiteSteps {

    private final SiteClient siteClient;
    private final TestContext testContext;
    private final RecordAreaPort recordAreaPort;
    private final RecordSectionPort recordSectionPort;
    private final RecordBlockPort recordBlockPort;
    private final RecordSeatPort recordSeatPort;

    private static final Map<String, String> AREA_NAME_MAP = Map.of(
            "INFIELD", "내야",
            "OUTFIELD", "외야");

    private static final Map<String, String> INFIELD_SECTION_NAME_MAP = Map.of(
            "HOME", "연고",
            "AWAY", "원정");

    private static final Map<String, String> OUTFIELD_SECTION_NAME_MAP = Map.of(
            "LEFT", "좌측",
            "RIGHT", "우측");

    @Given("경기장은 내야와 외야 영역으로 나뉘며, 내야 영역은 연고와 원정 진영으로, 외야 영역은 좌측과 우측 진영으로 구분되고, 각 진영은 {int}개의 구간과 구간별 {int}개의 좌석으로 구성됩니다.")
    public void setupSiteHierarchy(int blocksCount, int seatsPerBlock) {
        Area infield = recordAreaPort.recordArea(new Area("INFIELD"));
        Area outfield = recordAreaPort.recordArea(new Area("OUTFIELD"));

        List<Section> infieldSections = List.of(
                recordSectionPort.recordSection(new Section(infield.getId(), "HOME")),
                recordSectionPort.recordSection(new Section(infield.getId(), "AWAY")));
        List<Section> outfieldSections = List.of(
                recordSectionPort.recordSection(new Section(outfield.getId(), "LEFT")),
                recordSectionPort.recordSection(new Section(outfield.getId(), "RIGHT")));

        createBlocksAndSeatsForArea(infield, infieldSections, blocksCount, seatsPerBlock, INFIELD_SECTION_NAME_MAP);
        createBlocksAndSeatsForArea(outfield, outfieldSections, blocksCount, seatsPerBlock,
                OUTFIELD_SECTION_NAME_MAP);
    }

    //

    @When("영역 목록을 조회하면,")
    public void getAreaList() {
        TestResponse response = siteClient.getAreas();
        testContext.setResponse(response);
    }

    @Then("'내야'와 '외야' 영역으로 나뉘어야 한다.")
    public void verifyAreasContainInfieldAndOutfield() {
        TestResponse response = testContext.getResponse();
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

        List<String> names = response.jsonPath().getList("data.areas[*].name");
        assertThat(names).contains("INFIELD", "OUTFIELD");
    }

    //

    @When("'내야' 영역의 진영 목록을 조회하면,")
    public void getInfieldSectionList() {
        getSectionListByAreaName("INFIELD");
    }

    @Then("'연고'와 '원정' 진영으로 구분되어야 한다.")
    public void verifySectionsContainHomeAndAway() {
        TestResponse response = testContext.getResponse();
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

        List<String> names = response.jsonPath().getList("data.sections[*].name");
        assertThat(names).contains("HOME", "AWAY");
    }

    //

    @When("'외야' 영역의 진영 목록을 조회하면,")
    public void getOutfieldSectionList() {
        getSectionListByAreaName("OUTFIELD");
    }

    @Then("'좌측'과 '우측' 진영으로 구분되어야 한다.")
    public void verifySectionsContainLeftAndRight() {
        TestResponse response = testContext.getResponse();
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

        List<String> names = response.jsonPath().getList("data.sections[*].name");
        assertThat(names).contains("LEFT", "RIGHT");
    }

    //

    @When("임의의 진영의 구간 목록을 조회하면,")
    public void getBlockListForAnySection() {
        Long sectionId = findSectionId("INFIELD", "HOME");
        TestResponse response = siteClient.getBlocks(sectionId);
        testContext.setResponse(response);
    }

    @Then("{int}개의 구간이 있어야 한다.")
    public void verifyBlockCount(int expectedCount) {
        TestResponse response = testContext.getResponse();
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

        List<Object> blocks = response.jsonPath().getList("data.blocks[*]");
        assertThat(blocks).hasSize(expectedCount);
    }

    //

    @When("임의의 구간의 좌석 목록을 조회하면,")
    public void getSeatListForAnyBlock() {
        Long sectionId = findSectionId("INFIELD", "HOME");
        TestResponse blockResponse = siteClient.getBlocks(sectionId);
        Long blockId = Long.valueOf(blockResponse.jsonPath().get("data.blocks[0].id").toString());

        TestResponse response = siteClient.getSeats(blockId);
        testContext.setResponse(response);
    }

    @Then("{int}개의 좌석이 있어야 한다.")
    public void verifySeatCount(int expectedCount) {
        TestResponse response = testContext.getResponse();
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

        List<Object> seats = response.jsonPath().getList("data.seats[*]");
        assertThat(seats).hasSize(expectedCount);
    }

    //

    private void createBlocksAndSeatsForArea(Area area, List<Section> sections, int blocksCount, int seatsPerBlock,
            Map<String, String> sectionNameMap) {
        String areaName = AREA_NAME_MAP.get(area.getName());

        for (Section section : sections) {
            String sectionName = sectionNameMap.get(section.getName());
            createBlocksAndSeatsForSection(section, blocksCount, seatsPerBlock, areaName, sectionName);
        }
    }

    private void createBlocksAndSeatsForSection(Section section, int blocksCount, int seatsPerBlock, String areaName,
            String sectionName) {
        for (int i = 0; i < blocksCount; i++) {
            String blockName = String.format("%s-%s-%d", areaName, sectionName, i + 1);
            Block block = recordBlockPort.recordBlock(new Block(section.getId(), blockName));
            createSeatsForBlock(block, seatsPerBlock);
        }
    }

    private void createSeatsForBlock(Block block, int seatsPerBlock) {
        int rows = 10;
        int cols = 10;

        for (int r = 1; r <= rows; r++) {
            for (int c = 1; c <= cols; c++) {
                recordSeatPort.recordSeat(new Seat(block.getId(), r, c));
            }
        }
    }

    private void getSectionListByAreaName(String areaName) {
        TestResponse response = siteClient.getAreas();
        Long areaId = Long.valueOf(
                response.jsonPath().get("data.areas[?(@.name == '" + areaName + "')].id").toString());

        TestResponse sectionResponse = siteClient.getSections(areaId);
        testContext.setResponse(sectionResponse);
    }

    private Long findSectionId(String areaName, String sectionName) {
        TestResponse areaResponse = siteClient.getAreas();
        Long areaId = Long.valueOf(
                areaResponse.jsonPath().get("data.areas[?(@.name == '" + areaName + "')].id").toString());

        TestResponse sectionResponse = siteClient.getSections(areaId);
        return Long.valueOf(
                sectionResponse.jsonPath().get("data.sections[?(@.name == '" + sectionName + "')].id").toString());
    }
}
