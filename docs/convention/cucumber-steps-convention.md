---
trigger: model_decision
description: cucumber-steps-convention
---

Cucumber Steps 작성 규칙
===

Cucumber Steps 파일 작성 시 일관성과 가독성을 유지하기 위한 규칙입니다.

1. 기본 원칙
---

### 어노테이션 내용: 한국어 (feature 파일과 100% 일치)
- `@Given`, `@When`, `@Then` 어노테이션의 value는 **feature 파일의 내용과 글자도 바꾸지 않고 그대로** 사용합니다.
- **특별 중요**: 마침표(.), 쉼표(,), 조사(~하고, ~한다 등) 포함 모든 문자를 정확히 일치시켜야 합니다.
- feature 파일과 steps 파일 간의 명확한 매핑을 유지합니다.
- **임의로 편의상 수정하지 않습니다** - 수정이 필요하면 feature 파일을 먼저 수정합니다.

### 메서드 이름: 영어
- 메서드 이름은 **영어**로 작성합니다.
- 명확하고 일관된 네이밍을 사용합니다.
- camelCase를 사용합니다.

2. 예시
---

### ❌ 잘못된 예시
```java
// 어노테이션이 영어, 메서드 이름이 한국어
@When("Get area list")
public void 영역목록_조회() {
    // ...
}

// 어노테이션과 feature 파일 내용이 다름
@When("영역 리스트 조회")  // feature에는 "영역 목록을 조회하면,"
public void getAreaList() {
    // ...
}

// 쉼표 마침표를 쉼표로 바꿈
@Then("'내야'와 '외야' 영역으로 나뉘어야 하고,")  // feature에는 "나뉘어야 한다."
public void verifyAreas() {
    // ...
}

// 쉼표를 마침표로 바꿈
@When("영역 목록을 조회하면.")  // feature에는 "조회하면,"
public void getAreas() {
    // ...
}
```

### ✅ 올바른 예시
```java
// feature: When 영역 목록을 조회하면,
@When("영역 목록을 조회하면,")  // 쉼표까지 정확히 일치
public void getAreaList() {
    TestResponse response = siteClient.getAreas();
    testContext.setResponse(response);
}

// feature: Then '내야'와 '외야' 영역으로 나뉘어야 한다.
@Then("'내야'와 '외야' 영역으로 나뉘어야 한다.")  // 마침표까지 정확히 일치
public void verifyAreasContainInfieldAndOutfield() {
    TestResponse response = testContext.getResponse();
    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

    List<String> names = response.jsonPath().getList("data.areas.name");
    assertThat(names).contains("내야", "외야");
}

// feature: When '외야' 영역의 진영 목록을 조회하면,
@When("'외야' 영역의 진영 목록을 조회하면,")  // 쉼표까지 정확히 일치
public void getOutfieldSectionList() {
    getSectionListByAreaName("외야");
}
```

3. 메서드 네이밍 규칙
---

### Given 메서드
- **패턴**: `setup*`, `prepare*`, `register*`
- **목적**: 초기 데이터 설정 및 사전 조건 준비

```java
@Given("경기장은 내야와 외야 영역으로 나뉘며, 내야 영역은 연고와 원정 진영으로, 외야 영역은 좌측과 우측 진영으로 구분되고, 각 진영은 {int}개의 구간과 구간별 {int}개의 좌석으로 구성됩니다.")
public void setupSiteHierarchyWithAreasAndSections(int blocksCount, int seatsPerBlock) {
    // 사이트 계층 구조 설정
}
```

### When 메서드
- **패턴**: `get*`, `create*`, `update*`, `delete*`, `request*`
- **목적**: 액션 수행

```java
@When("영역 목록을 조회하면,")
public void getAreaList() {
    // 영역 목록 조회
}

@When("'내야' 영역의 진영 목록을 조회하면,")
public void getInfieldSectionList() {
    // 내야 진영 목록 조회
}

@When("임의의 진영의 구간 목록을 조회하면")
public void getBlockListForAnySection() {
    // 임의 진영의 구간 목록 조회
}
```

### Then 메서드
- **패턴**: `verify*`, `assert*`, `check*`, `expect*`
- **목적**: 결과 검증

```java
@Then("'내야'와 '외야' 영역으로 나뉘어야 하고,")
public void verifyAreasContainInfieldAndOutfield() {
    // 영역 검증
}

@Then("{int}개의 구간이 있어야 한다.")
public void verifyBlockCount(int expectedCount) {
    // 구간 개수 검증
}
```

4. Private Helper 메서드
---

### 네이밍 규칙
- **목적을 명확히** 표현하는 동사 + 명사 조합
- 일관된 접두어 사용: `create*`, `find*`, `build*`, `extract*`

### 예시
```java
// 생성 관련
private void createBlocksAndSeatsForArea(Area area, List<Section> sections,
                                         int blocksCount, int seatsPerBlock,
                                         Map<String, String> sectionNameMap) {
    // 영역별 블록과 좌석 생성
}

private void createSeatsForBlock(Block block, int seatsPerBlock) {
    // 블록별 좌석 생성
}

// 조회/검색 관련
private void getSectionListByAreaName(String areaName) {
    // 영역명으로 진영 목록 조회
}

private Long findSectionId(String areaName, String sectionName) {
    // 진영 ID 찾기
    return sectionId;
}
```

5. 상수 정의
---

### Map 상수
- **네이밍**: `대문자_스네이크_케이스` + `_MAP` 접미사
- **선언**: `private static final`
- **목적**: 도메인 용어 매핑 및 테스트 데이터 관리

```java
private static final Map<String, String> AREA_NAME_MAP = Map.of(
        "내야", "infield",
        "외야", "outfield");

private static final Map<String, String> INFIELD_SECTION_NAME_MAP = Map.of(
        "연고", "home",
        "원정", "away");

private static final Map<String, String> OUTFIELD_SECTION_NAME_MAP = Map.of(
        "좌측", "left",
        "우측", "right");
```

6. 코드 구조
---

### 파일 구조 순서
1. 상수 선언부 (static final Map 등)
2. Given 메서드들
3. When 메서드들
4. Then 메서드들
5. Private helper 메서드들

### 예시
```java
@Slf4j
@RequiredArgsConstructor
public class SiteSteps {

    // 1. 의존성 주입
    private final SiteClient siteClient;
    private final TestContext testContext;
    private final RecordAreaPort recordAreaPort;
    // ...

    // 2. 상수 선언
    private static final Map<String, String> AREA_NAME_MAP = Map.of(/*...*/);
    private static final Map<String, String> INFIELD_SECTION_NAME_MAP = Map.of(/*...*/);

    // 3. Given 메서드
    @Given("경기장은 내야와 외야 영역으로 나뉘며 ...")
    public void setupSiteHierarchyWithAreasAndSections(int blocksCount, int seatsPerBlock) {
        // ...
    }

    // 4. When 메서드
    @When("영역 목록을 조회하면,")
    public void getAreaList() {
        // ...
    }

    // 5. Then 메서드
    @Then("'내야'와 '외야' 영역으로 나뉘어야 하고,")
    public void verifyAreasContainInfieldAndOutfield() {
        // ...
    }

    // 6. Private helper 메서드
    private void createBlocksAndSeatsForArea(...) {
        // ...
    }
}
```

7. 공유 상태 관리 (TestContext)
---

시나리오 전반에 걸쳐 데이터를 공유하기 위해 `TestContext`를 사용합니다.

### ID 매핑 전략 (Alias -> Real ID)
시나리오에서 사용하는 가상 ID(Alias)와 실제 DB의 ID를 매핑하여 관리합니다. 이를 통해 여러 엔티티가 등장하는 복잡한 시나리오를 지원합니다.

- **Match**: `Map<Long, Long> matchIdMap` (예: 0L -> DB ID)
- **User**: `Map<String, Long> userEmailMap` (예: email -> DB ID)
- **Seat**: `List<Long> heldSeatIds` (점유된 여러 좌석 추적)

### 상태 저장
- Action 수행 후 응답(`TestResponse`)은 항상 `testContext.setResponse(response)`에 저장하여 다음 단계에서 검증할 수 있도록 합니다.

8. 브릿지 로직 (Bridge Logic)
---

Gherkin(인간 중심)의 표현을 API(기술 중심)의 식별자로 변환하는 로직을 Step Definition 내부에 구현합니다.

- **원칙**: 서비스 API는 최대한 단순하게(ID 기반) 유지하고, 복잡한 조회 및 매핑은 테스트 코드(Steps)에서 처리합니다.
- **예시**: 구역 이름과 행/열 정보를 받아 `SiteClient`로 `seatId`를 조회한 후, `AllocationClient`에는 `seatId`만 전달합니다.

```java
@When("사용자가 {string} 구역 {int}행 {int}열 좌석 점유를 요청하면")
public void requestHoldSeat(String blockName, int row, int col) {
    // 1. 매핑 로직 (Bridge)
    Long blockId = findBlockIdByName(blockName);
    Long seatId = findSeatId(blockId, row, col);

    // 2. 기술적 API 호출
    TestResponse response = allocationClient.holdSeat(matchId, seatId, userId);
    testContext.setResponse(response);
}
```

9. 체크리스트
---

Steps 파일 작성 시 다음을 확인하세요:

- [ ] **절대 필수**: `@Given`, `@When`, `@Then` 어노테이션의 내용이 feature 파일과 **한 글자도 빠짐없이** 정확히 일치하는가?
- [ ] **절대 필수**: 마침표(.), 쉼표(,), 조사(~하고, ~한다) 등이 feature 파일과 정확히 일치하는가?
- [ ] 메서드 이름이 영어로 작성되었는가?
- [ ] 메서드 이름이 명확하고 일관된 네이밍 규칙을 따르는가?
- [ ] Given 메서드가 `setup*`, `prepare*`, `register*` 패턴을 사용하는가?
- [ ] When 메서드가 `get*`, `create*`, `update*` 등의 액션 동사를 사용하는가?
- [ ] Then 메서드가 `verify*`, `assert*`, `check*` 패턴을 사용하는가?
- [ ] Private helper 메서드가 목적을 명확히 표현하는가?
- [ ] 상수가 `private static final` + 대문자 스네이크 케이스로 선언되었는가?
- [ ] 파일 구조가 정해진 순서(상수 → Given → When → Then → Private)를 따르는가?
- [ ] 중복 코드가 helper 메서드로 추출되었는가?

10. 참고 사항
---

### Feature 파일 작성
- Feature 파일 작성 규칙은 `test-data-convention.md` 참조
- 도메인 용어 및 테스트 데이터는 일관되게 사용

11. JsonPath 사용 규칙
---

### 반드시 RestAssured JsonPath 문법 사용
Cucumber Steps에서 API 응답을 파싱할 때는 **RestAssured의 JsonPath 문법**을 사용해야 합니다.

### ❌ Groovy find 문법 사용 금지
```java
// 잘못된 예시 - Groovy find 문법
Long areaId = Long.valueOf(response.jsonPath()
    .get("data.areas.find { it.name == 'INFIELD' }.id").toString());

Long sectionId = Long.valueOf(response.jsonPath()
    .get("data.sections.find { it.name == 'HOME' }.id").toString());
```

### ✅ RestAssured JsonPath 문법 사용
```java
// 올바른 예시 - RestAssured JsonPath 필터 문법 (GPath 기반)
// 조건에 맞는 항목을 찾을 때
Long areaId = Long.valueOf(response.jsonPath()
    .get("data.areas[?(@.name == 'INFIELD')].id").toString());

Long sectionId = Long.valueOf(response.jsonPath()
    .get("data.sections[?(@.name == 'HOME')].id").toString());

// 인덱스로 직접 접근하는 경우
Long blockId = Long.valueOf(response.jsonPath()
    .get("data.blocks[0].id").toString());

// 다중 조건 검색
Long seatId = Long.valueOf(response.jsonPath()
    .get("data.seats[?(@.rowNum == 1 && @.colNum == 2)].id").toString());
```

### JsonPath 문법 패턴

#### 1. 조건부 필터링 (배열에서 특정 조건 검색)
```java
// 패턴: [?(@.property == 'value')].field
// 설명: property가 'value'인 항목의 field를 반환
// 주의: RestAssured의 GPath는 자동으로 단일 값을 반환하므로 [0]을 붙이지 않음

Long id = Long.valueOf(response.jsonPath()
    .get("data.items[?(@.name == 'target')].id").toString());

// 다중 조건
Long seatId = Long.valueOf(response.jsonPath()
    .get("data.seats[?(@.rowNum == 1 && @.colNum == 2)].id").toString());
```

#### 2. 인덱스 접근
```java
// 패턴: [index].property
// 설명: 배열의 index 위치 요소의 property를 반환

Long firstId = Long.valueOf(response.jsonPath()
    .get("data.items[0].id").toString());
```

#### 3. 전체 리스트 조회
```java
// 패턴: [*].property 또는 property (List 반환)
// 설명: 모든 요소의 property를 리스트로 반환

List<String> names = response.jsonPath().getList("data.items[*].name");
// 또는
List<String> names = response.jsonPath().getList("data.items.name");
```

### 주의 사항
- **RestAssured vs Jayway JsonPath**: RestAssured는 Groovy의 GPath를 사용합니다. Jayway JsonPath와 문법이 다릅니다.
- **[0] 사용 금지**: 조건부 필터링 `[?(@.property == 'value')]` 뒤에 `[0]`을 붙이지 않습니다. GPath가 자동으로 단일 값을 처리합니다.
- **null 체크**: JsonPath가 null을 반환할 수 있으므로, `.toString()` 호출 전에 null 체크가 필요할 수 있습니다.
- **일관성 유지**: 프로젝트 전체에서 동일한 JsonPath 문법을 사용해야 합니다.

12. 관련 문서
---
- [테스트 데이터 작성 규칙](./test-data-convention.md)
- [코드 스타일 가이드](./code-style.md)
- [예외 처리 규칙](./exception-handling.md)
