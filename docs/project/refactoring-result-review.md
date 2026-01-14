# 프로젝트 리팩토링 제안서

> 프로젝트 전반 분석을 통해 도출된 리팩토링 포인트 정리

---

## 요약

| 우선순위 | 카테고리 | 문제 수 | 영향도 |
|---------|---------|--------|--------|
| 🔴 Critical | 보안 | 2 | 높음 |
| 🟠 High | 아키텍처 | 4 | 높음 |
| 🟡 Medium | 코드 품질 | 5 | 중간 |
| 🟢 Low | 개선사항 | 4 | 낮음 |

---

## 🔴 Critical (보안 문제)

### 1. 비밀번호 평문 저장

**현재 코드** (`User.java:32-34`):
```java
public boolean matchPassword(final String password) {
    return this.password.equals(password);  // 평문 비교
}
```

**문제점**:
- DB에 비밀번호가 평문으로 저장됨
- 데이터 유출 시 모든 사용자 비밀번호 노출

**개선 방향**:
```java
// PasswordEncoder 도입 (BCrypt 등)
public boolean matchPassword(String rawPassword, PasswordEncoder encoder) {
    return encoder.matches(rawPassword, this.password);
}
```

---

### 2. 인증/인가 체계 부재

**현재 코드** (`UserController.java:37-40`):
```java
public SuccessResponse<UserResponse> signUp(@RequestBody SignUpRequest request) {
    User user = signUpUseCase.signUp(command);
    return SuccessResponse.of(UserResponse.from(user));  // 토큰 없음
}
```

**문제점**:
- 로그인 후 세션/JWT 토큰 발급 없음
- API 호출 시 사용자 인증 불가
- `userId`를 클라이언트가 직접 전달하는 구조 (위변조 가능)

**개선 방향**:
- Spring Security + JWT 도입
- `@AuthenticationPrincipal`로 현재 사용자 주입

---

## 🟠 High (아키텍처 문제)

### 3. Cross-Domain 의존성 (강결합)

**현재 코드** (`PaymentService.java:44-48`):
```java
// Cross-domain ports - 다른 도메인의 Port를 직접 의존
private final LoadReservationPort loadReservationPort;      // Reservation 도메인
private final RecordReservationPort recordReservationPort;  // Reservation 도메인
private final LoadAllocationPort loadAllocationPort;        // Site 도메인
private final RecordAllocationPort recordAllocationPort;    // Site 도메인
```

**동일 패턴** (`ReservationService.java:26-27`):
```java
private final LoadAllocationPort loadAllocationPort;   // Site 도메인
private final RecordAllocationPort recordAllocationPort;  // Site 도메인
```

**문제점**:
- 도메인 간 강결합 → 변경 시 연쇄 영향
- 순환 의존성 위험
- 단위 테스트 어려움

**개선 방향**:
```
Option A: 이벤트 기반 아키텍처
  Payment → PaymentConfirmedEvent 발행
  Reservation/Site → 이벤트 구독하여 상태 변경

Option B: Application Service 분리
  PaymentFacadeService (조율자)
    → PaymentService (Payment 도메인만)
    → ReservationService (Reservation 도메인만)
    → AllocationService (Site 도메인만)

Option C: Domain Service 도입
  공통 도메인 로직을 별도 서비스로 추출
```

---

### 4. Application 계층에 웹 기술 노출

**현재 코드** (`AllocationStatusService.java:16, 35`):
```java
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

private final Map<String, List<SseEmitter>> allocationStatusEmitters = new ConcurrentHashMap<>();
```

**문제점**:
- Application 계층이 `SseEmitter` (웹 기술)에 의존
- 헥사고날 아키텍처 원칙 위반
- 다른 전송 방식(WebSocket 등) 전환 시 Application 수정 필요

**개선 방향**:
```
Application 계층:
  - AllocationStatusPublisher (Port 인터페이스)
  - void publishStatusChange(AllocationStatusEvent event)

Adapter 계층:
  - SseAllocationStatusAdapter implements AllocationStatusPublisher
  - SseEmitter 관리는 여기서
```

---

### 5. ConfirmSeatsUseCase 미구현

**현재 코드** (`AllocationService.java:135-150`):
```java
@Override
@Transactional
public List<Allocation> confirmSeats(ConfirmSeatsCommand command) {
    // TODO: Implement reservation persistence
    List<Allocation> confirmedSeats = new ArrayList<>();
    if (confirmedSeats.isEmpty()) {
        throw new NoSeatsToConfirmException(userId, matchId, seatIds);
    }
    return confirmedSeats;  // 항상 예외 발생
}
```

**문제점**:
- 좌석 확정 기능이 작동하지 않음
- 예약 플로우 완성 불가

**개선 방향**:
- 구현하거나, 사용하지 않는다면 제거

---

### 6. 트랜잭션 경계 문제

**현재 코드** (`PaymentService.confirmPayment()`):
```java
@Transactional
public Payment confirmPayment(final ConfirmPaymentCommand command) {
    // 1. Payment 상태 변경
    // 2. 외부 결제 게이트웨이 호출 (네트워크 I/O)
    // 3. Reservation 상태 변경
    // 4. 여러 Allocation 상태 변경
}
```

**문제점**:
- 하나의 트랜잭션에서 여러 도메인 + 외부 API 호출
- 외부 API 실패 시 전체 롤백
- 외부 API 성공 후 DB 실패 시 불일치 발생

**개선 방향**:
```
1. 외부 API 호출을 트랜잭션 밖으로:
   - 먼저 외부 결제 완료
   - 이후 DB 트랜잭션 시작

2. Saga 패턴:
   - 각 단계를 별도 트랜잭션으로
   - 실패 시 보상 트랜잭션 실행
```

---

## 🟡 Medium (코드 품질)

### 7. Exception 계층 불일치

**현재 구조**:
```
Site: SiteException (기본 클래스 있음)
  ├── SeatAlreadyHeldException
  ├── SeatNotFoundException
  └── ...

User: 기본 클래스 없음
  ├── DuplicateEmailException extends RuntimeException
  └── LoginFailureException extends RuntimeException

Payment: PaymentException (기본 클래스 있음)
Reservation: ReservationException (기본 클래스 있음)
```

**개선 방향**:
```java
// 공통 기본 예외
public abstract class DomainException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;
}

// 각 도메인별 기본 예외
public class UserException extends DomainException { }
public class SiteException extends DomainException { }
```

---

### 8. ControllerAdvice 분산 및 중복

**현재 구조**:
- `UserControllerAdvice` - User 도메인 예외 처리
- `AllocationControllerAdvice` - Site 도메인 예외 처리 (basePackages 미지정!)
- Payment, Reservation 예외 처리 누락 가능

**문제점**:
- `AllocationControllerAdvice`에 `basePackages` 미지정 → 전역 적용
- 글로벌 예외 처리 없음 (500 에러 등)

**개선 방향**:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ErrorResponse handleDomainException(DomainException e) {
        return ErrorResponse.of(e.getStatus().value(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return ErrorResponse.of(500, "Internal Server Error");
    }
}
```

---

### 9. UseCase가 Domain 엔티티 직접 반환

**현재 코드** (`UserController.java:39-40`):
```java
User user = signUpUseCase.signUp(command);  // Domain 반환
return SuccessResponse.of(UserResponse.from(user));  // Controller가 변환
```

**문제점**:
- Controller가 Domain 엔티티에 직접 의존
- Domain 변경 시 Controller도 수정 필요

**개선 방향**:
```
Option A: UseCase가 DTO 반환
  UserResponse signUp(SignUpCommand command);

Option B: Mapper를 Adapter 계층에 배치
  Controller → Mapper → UseCase (Domain) → Mapper → Response
```

---

### 10. Request Validation 부재

**현재 코드** (`SignUpRequest.java` - 추정):
```java
public record SignUpRequest(String email, String password) { }
// @NotBlank, @Email 등 없음
```

**개선 방향**:
```java
public record SignUpRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password
) { }

// Controller
public SuccessResponse<UserResponse> signUp(@Valid @RequestBody SignUpRequest request)
```

---

### 11. 마법 문자열/숫자

**현재 코드**:
```java
// PaymentService.java:93, 106
PaymentStatus.FAILED, "TOSS_PAYMENTS", command.paymentKey(), ...

// AllocationStatusService.java:51
SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

// AllocationStatusService.java:96
@Scheduled(fixedRate = 1000)
```

**개선 방향**:
```java
// 상수화
public class PaymentConstants {
    public static final String GATEWAY_TOSS = "TOSS_PAYMENTS";
}

// 설정 외부화
@Value("${sse.timeout:#{T(java.lang.Long).MAX_VALUE}}")
private long sseTimeout;

@Value("${allocation.poll-interval-ms:1000}")
private long pollIntervalMs;
```

---

## 🟢 Low (개선사항)

### 12. 도메인 객체 생성 패턴 개선

**현재 코드** (반복되는 패턴):
```java
// PaymentService.java:126-134
Allocation occupiedAllocation = Allocation.withId(
    allocation.getId(),
    allocation.getUserId(),
    allocation.getMatchId(),
    allocation.getSeatId(),
    allocation.getReservationId(),
    AllocationStatus.OCCUPIED,
    null,
    LocalDateTime.now());
```

**개선 방향**:
```java
// 도메인 메서드 추가
public class Allocation {
    public Allocation occupy() {
        return Allocation.withId(
            this.id, this.userId, this.matchId, this.seatId,
            this.reservationId, AllocationStatus.OCCUPIED,
            null, LocalDateTime.now()
        );
    }

    public Allocation hold(Long userId, LocalDateTime expiresAt) { ... }
    public Allocation release() { ... }
}

// 사용
Allocation occupied = allocation.occupy();
```

---

### 13. 로깅 일관성

**현재 코드**:
```java
// AllocationStatusService.java - System.out.println과 log 혼용
System.out.println("[DEBUG] 스케줄러 실행됨: emitters 수=" + allocationStatusEmitters.size());
log.info("스케줄러 실행됨: emitters 수={}", allocationStatusEmitters.size());
```

**개선 방향**:
- `System.out.println` 전부 제거
- 로그 레벨 적절히 사용 (DEBUG/INFO/WARN/ERROR)

---

### 14. API 응답 일관성

**현재 구조**:
```java
SuccessResponse<T>  // 성공
ErrorResponse       // 실패
```

**개선 방향**:
```java
// 통합 응답 구조
public record ApiResponse<T>(
    boolean success,
    T data,
    ErrorInfo error,
    LocalDateTime timestamp
) { }
```

---

### 15. 테스트 커버리지 확장

**현재**: BDD 인수 테스트만 존재

**개선 방향**:
- 단위 테스트 추가 (Service, Domain)
- 통합 테스트 추가 (Repository)
- 예외 케이스 테스트

---

## 리팩토링 우선순위 로드맵

```
Phase 1 (즉시): 보안
  ├── 비밀번호 암호화 (BCrypt)
  └── JWT 인증 도입

Phase 2 (단기): 아키텍처 정비
  ├── SSE 로직 Adapter 계층으로 이동
  ├── ConfirmSeatsUseCase 구현 또는 제거
  └── Exception 계층 통합

Phase 3 (중기): 도메인 분리
  ├── Cross-Domain 의존성 해소 (이벤트 기반)
  ├── 트랜잭션 경계 재설계
  └── Facade/Orchestrator 도입

Phase 4 (장기): 품질 개선
  ├── Request Validation 추가
  ├── 도메인 메서드 리팩토링
  └── 테스트 커버리지 확장
```

---

## 헥사고날 아키텍처 준수도 평가

### 준수 현황

**매우 우수한 점:**
1. ✅ **포트-어댑터 명확 분리**: in/out port 인터페이스 정의, 구현체와 분리
2. ✅ **의존성 역전**: 고수준 모듈이 저수준 모듈에 의존하지 않음
3. ✅ **도메인 순수성**: Domain 레이어에 외부 라이브러리 의존 없음 (Lombok @Getter만 사용)
4. ✅ **UseCase 중심**: 비즈니스 로직이 명확한 UseCase 인터페이스로 표현
5. ✅ **계층 명확성**: domain → application(port) → adapter(controller/persistence) 명확

### 의존성 방향 (의존성 역전 원칙)
```
Controller → UseCase(Interface) ← Service(Impl) → Port(Interface) ← Adapter(Impl)
```

### 개선 필요 사항
1. ⚠️ **Cross-Domain 의존성**: PaymentService가 다른 도메인의 포트를 직접 의존
2. ⚠️ **Application 계층 웹 기술 노출**: AllocationStatusService의 SseEmitter
3. ⚠️ **UseCase → Domain 반환**: Controller가 Domain 엔티티 직접 참조

---

## 파일 통계

| 범주 | 개수 | 위치 |
|------|------|------|
| 도메인 모델 | 9 | `*/domain/` |
| UseCase (In-Port) | 13 | `*/application/port/in/` |
| Port 인터페이스 (Out-Port) | 22 | `*/application/port/out/` |
| Service 구현 | 7 | `*/application/service/` |
| Controller | 6+ | `*/adapter/in/web/` |
| Repository | 11 | `*/adapter/out/persistence/` |
| Entity | 8 | `*/adapter/out/persistence/entity/` |
| Exception 클래스 | 17+ | `*/exception/` |
| **총 Java 파일** | **136** | `src/main/java/` |
