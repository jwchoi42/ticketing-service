# 인수 테스트 최적화

## 문제 상황

`AcceptanceTestRunner` 실행 시 OOM(Out of Memory) 에러 발생

### 원인 분석

1. **개별 INSERT 10,000번 반복**
   - 4개 진영 × 25개 블록 × 100개 좌석 = 10,000개
   - 매 시나리오마다 TRUNCATE 후 재생성

2. **Steps 클래스 스코프 문제**
   - 스코프 어노테이션 없이 싱글톤으로 동작
   - 시나리오 간 상태(receivedEvents 등) 공유로 메모리 누적

3. **DataInitializer 충돌**
   - 애플리케이션 시작 시 25개 블록 자동 생성
   - 테스트 데이터와 충돌

## 해결 방안

### 1. Steps 클래스에 `@ScenarioScope` 추가

시나리오마다 새 인스턴스 생성으로 상태 격리

```java
@Slf4j
@ScenarioScope  // 추가
@RequiredArgsConstructor
public class AllocationStatusSteps {
    // ...
}
```

적용 대상:
- AllocationStatusSteps
- AllocationSteps
- ReservationSteps
- MatchSteps
- SiteSteps
- UserSteps
- PaymentSteps
- AdminSteps
- HealthCheckSteps

### 2. 좌석 구조 테스트 간 공유

`DatabaseCleanupHook`에서 좌석 관련 테이블 제외:

```java
private static final Set<String> EXCLUDED_TABLES = Set.of(
        "areas", "sections", "blocks", "seats"
);
```

`SiteSteps`에서 이미 존재하면 생성 스킵:

```java
List<Area> existingAreas = loadAreaPort.loadAllAreas();
if (!existingAreas.isEmpty()) {
    log.info("[BDD] 좌석 구조가 이미 존재함 - 생성 스킵");
    return;
}
```

### 3. 테스트 데이터 규모 축소

Feature 파일에서 블록 수 축소 (25개 → 2개):

```gherkin
# Before
각 진영은 25개의 구간과 구간별 100개의 좌석으로 구성됩니다.

# After
각 진영은 2개의 구간과 구간별 100개의 좌석으로 구성됩니다.
```

인수 테스트 목적은 기능 검증이므로 데이터 규모는 최소화

### 4. DataInitializer 테스트 환경 비활성화

```java
@Slf4j
@Component
@Profile("!test")  // 추가
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {
```

### 5. 테스트 프로파일 활성화

```java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")  // 추가
public class AcceptanceTestConfig {
```

## 결과

| 항목 | 이전 | 이후 |
|------|------|------|
| 상태 | OOM 발생 | 정상 통과 |
| 시간 | 7분+ | 2분 |
| 좌석 생성 | 시나리오당 10,000개 | 전체 800개 1회 |

## 교훈

1. **인수 테스트 vs 성능 테스트 구분**
   - 인수 테스트: 기능 검증 목적, 최소 데이터로 충분
   - 성능 테스트: 실제 규모 데이터 필요, 별도 환경에서 실행

2. **테스트 간 데이터 공유**
   - 불변 데이터(좌석 구조)는 공유 가능
   - 가변 데이터(예약, 결제 등)는 시나리오마다 초기화

3. **프로파일 활용**
   - 테스트 환경에서 불필요한 초기화 로직 비활성화
   - `@Profile("!test")`, `@ActiveProfiles("test")` 활용
