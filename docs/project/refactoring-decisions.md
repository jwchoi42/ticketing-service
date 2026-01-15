# 리팩토링 결정 사항

> refactoring-result-review.md 기반 의사결정 기록

---

## API 공통 응답 포맷

### 결정 사항
- HTTP 상태 코드는 응답 본문에 포함하지 **않음**
- `timestamp` 필드 추가
- 응답 본문에는 `message`, `data`만 포함
- `message`와 `data`는 생략 가능
- 성공: `SuccessResponse<T>`, 실패: `ErrorResponse` 분리 유지

### 응답 구조
```java
// 성공 응답
public record SuccessResponse<T>(
    @JsonInclude(JsonInclude.Include.NON_NULL) String message,
    @JsonInclude(JsonInclude.Include.NON_NULL) T data,
    LocalDateTime timestamp
) { }

// 실패 응답
public record ErrorResponse(
    @JsonInclude(JsonInclude.Include.NON_NULL) String message,
    LocalDateTime timestamp
) { }
```

### 사용 예시
```java
// 성공 (데이터 포함)
return SuccessResponse.of(userResponse);
// { "data": {...}, "timestamp": "2024-..." }

// 성공 (메시지만)
return SuccessResponse.of("회원가입이 완료되었습니다.");
// { "message": "...", "timestamp": "2024-..." }

// 성공 (둘 다)
return SuccessResponse.of("조회 성공", listData);
// { "message": "...", "data": [...], "timestamp": "2024-..." }

// 에러
return ErrorResponse.of("좌석이 이미 점유되었습니다.");
// { "message": "...", "timestamp": "2024-..." }
```

---

## 리팩토링 항목별 결정

### 🔴 Critical (보안)

| 항목 | 결정 | 비고 |
|------|------|------|
| 비밀번호 평문 저장 | **생략** | 추후 별도 작업 |
| 인증/인가 체계 | **생략** | 추후 별도 작업 |

---

### 🟠 High (아키텍처)

| 항목 | 결정 | 구현 방향 |
|------|------|----------|
| Cross-Domain 의존성 | **채택** | `TicketingOrchestrationService`로 조율 |
| Application 계층 SSE 노출 | **채택** | Adapter 계층으로 분리 |
| ConfirmSeatsUseCase 미구현 | **채택** | 구현 필요 |
| 트랜잭션 경계 문제 | **채택** | 외부 결제 완료 후 DB 트랜잭션 시작 |

---

### 🟡 Medium (코드 품질)

| 항목 | 결정 | 구현 방향 |
|------|------|----------|
| Exception 계층 불일치 | **채택** | `DomainException` 기본 클래스 도입 |
| ControllerAdvice 분산 | **채택** | 도메인별 + GlobalControllerAdvice |
| UseCase Domain 반환 | **채택** | DTO 반환으로 변경 |
| Request Validation | 생략 | 추후 별도 작업 |
| 마법 문자열/숫자 | 생략 | 추후 별도 작업 |

---

### 🟢 Low (개선)

| 항목 | 결정 | 구현 방향 |
|------|------|----------|
| 도메인 객체 생성 패턴 | **채택** | `occupy()`, `release()` 등 메서드 추가 |
| 로깅 일관성 | **채택** | `System.out.println` 제거 |
| API 응답 일관성 | **채택** | `SuccessResponse<T>`, `ErrorResponse` 수정 |
| 테스트 커버리지 | 생략 | 인수 테스트만 작성 |

---

## 구현 순서

```
1. common 패키지
   - ApiResponse<T> 통합
   - DomainException 기본 클래스
   - GlobalControllerAdvice

2. user 도메인
   - Exception 계층 정비
   - UseCase → DTO 반환
   - ControllerAdvice 정비

3. match 도메인
   - Exception 계층 정비
   - UseCase → DTO 반환
   - ControllerAdvice 정비

4. site 도메인
   - Exception 계층 정비
   - UseCase → DTO 반환
   - SSE Adapter 분리
   - ConfirmSeatsUseCase 구현
   - 도메인 메서드 추가 (occupy, release, hold)
   - 로깅 정리

5. reservation 도메인
   - Exception 계층 정비
   - UseCase → DTO 반환
   - Cross-Domain 의존성 제거

6. payment 도메인
   - Exception 계층 정비
   - UseCase → DTO 반환
   - 트랜잭션 경계 수정
   - Cross-Domain 의존성 제거

7. orchestration
   - TicketingOrchestrationService 생성
   - 도메인 간 조율 로직 집중
```

---

## Exception 계층 구조

```
common/exception/
├── DomainException.java (추상 기본 클래스)
└── GlobalControllerAdvice.java

core/user/application/service/exception/
├── UserException.java extends DomainException
├── DuplicateEmailException.java extends UserException
└── LoginFailureException.java extends UserException

core/match/application/service/exception/
├── MatchException.java extends DomainException
└── MatchNotFoundException.java extends MatchException

core/site/application/service/exception/
├── SiteException.java extends DomainException (기존)
├── SeatAlreadyHeldException.java extends SiteException
└── ...

core/reservation/application/service/exception/
├── ReservationException.java extends DomainException (기존)
└── ...

core/payment/application/service/exception/
├── PaymentException.java extends DomainException (기존)
└── ...
```
