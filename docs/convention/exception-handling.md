---
trigger: model_decision
description: exception-handling-best-practices
---

예외 처리 규칙
===

원칙
---

범용 예외(`IllegalStateException`, `IllegalArgumentException` 등)를 직접 사용하지 않고, **도메인별 커스텀 예외**를 정의하여 사용합니다.

예외 클래스 구조
---

### Service Layer 패턴 (Best Practices)
- **Service(UseCase) 메서드는 `void` 또는 결과 객체를 반환**합니다.
- **`boolean`을 반환하여 성공/실패를 나타내지 않습니다.**
- 비즈니스 로직 검증 실패 시 **구체적인 도메인 예외를 직접 `throw`** 합니다.
- **Controller는 예외를 던지지 않음**: Controller는 UseCase를 호출하고 결과를 반환하는 역할만 수행합니다. Service에서 발생한 예외는 `@RestControllerAdvice`가 처리합니다.

### 예외 계층 구조

모든 도메인 예외는 `DomainException`을 상속하며, 도메인별 기본 예외 클래스를 거쳐 구체적인 예외로 세분화됩니다.

```
common/exception/
└── DomainException.java (추상 기본 클래스, HttpStatus 포함)

core/{domain}/application/service/exception/
├── {Domain}Exception.java extends DomainException (도메인별 기본 예외)
└── Specific{Situation}Exception.java extends {Domain}Exception
```

**DomainException 기본 클래스:**
```java
package dev.ticketing.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class DomainException extends RuntimeException {
    private final HttpStatus status;

    protected DomainException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    protected DomainException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
```

**도메인별 기본 예외:**
```java
package dev.ticketing.core.user.application.service.exception;

import dev.ticketing.common.exception.DomainException;
import org.springframework.http.HttpStatus;

public class UserException extends DomainException {
    public UserException(String message, HttpStatus status) {
        super(message, status);
    }
}
```

**구체적인 예외:**
```java
public class DuplicateEmailException extends UserException {
    public DuplicateEmailException(String email) {
        super("Email already exists: " + email, HttpStatus.CONFLICT);
    }
}
```

### 위치
```
src/main/java/dev/ticketing/common/exception/          # 공통 기본 클래스
src/main/java/dev/ticketing/core/{domain}/application/service/exception/  # 도메인별 예외
```

### 명명 규칙
- 예외 이름은 **도메인 상황을 명확히 표현**해야 합니다.
- 접미사는 항상 `Exception`을 사용합니다.
- 예시:
  - `SeatAlreadyHeldException` (좌석이 이미 점유됨)
  - `SeatNotAvailableException` (좌석을 사용할 수 없음)
  - `UnauthorizedSeatReleaseException` (권한 없는 좌석 해제 시도)
  - `DuplicateEmailException` (이메일 중복)
  - `LoginFailureException` (로그인 실패)

### 예외 클래스 구현

#### 기본 형태
```java
package dev.ticketing.core.site.application.service.exception;

public class SeatAlreadyHeldException extends RuntimeException {
    public SeatAlreadyHeldException(String message) {
        super(message);
    }

    public SeatAlreadyHeldException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

#### 추가 정보가 필요한 경우
```java
package dev.ticketing.core.site.application.service.exception;

import lombok.Getter;

@Getter
public class SeatNotAvailableException extends RuntimeException {
    private final Long seatId;
    private final String currentStatus;

    public SeatNotAvailableException(Long seatId, String currentStatus) {
        super(String.format("Seat %d is not available. Current status: %s", seatId, currentStatus));
        this.seatId = seatId;
        this.currentStatus = currentStatus;
    }
}
```

사용 예시
---

### ❌ 잘못된 예시
```java
if (allocation.getStatus() == AllocationStatus.OCCUPIED) {
    throw new IllegalStateException("Seat already occupied");
}
```

### ✅ 올바른 예시
```java
if (allocation.getStatus() == AllocationStatus.OCCUPIED) {
    throw new SeatAlreadyOccupiedException(
        String.format("Seat %d is already occupied for match %d", seatId, matchId)
    );
}
```

예외 처리 레이어
---

### Application Layer
- **비즈니스 로직 검증 실패 시** 도메인 예외 발생
- 예: `SeatAlreadyHeldException`, `UnauthorizedSeatReleaseException`

### Adapter Layer (Controller)
- Application Layer에서 발생한 예외를 **HTTP 응답으로 변환**
- `@ExceptionHandler`를 사용하는 전역 예외 처리 클래스는 **`ControllerAdvice`** 접미사를 사용합니다.

### ControllerAdvice 구조

**1. 도메인별 ControllerAdvice (필수)**
- 위치: `dev.ticketing.core.{domain}.adapter.in.web.{Domain}ControllerAdvice`
- 역할: 해당 도메인의 예외를 HTTP 응답으로 변환
- 반드시 `basePackages`를 지정하여 범위 제한

```java
@RestControllerAdvice(basePackages = "dev.ticketing.core.user")
public class UserControllerAdvice {

    @ExceptionHandler(LoginFailureException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleLoginFailure(LoginFailureException e) {
        return ErrorResponse.of(e.getMessage());
    }

    @ExceptionHandler(DuplicateEmailException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateEmail(DuplicateEmailException e) {
        return ErrorResponse.of(e.getMessage());
    }
}
```

**2. GlobalControllerAdvice (Fallback)**
- 위치: `dev.ticketing.common.web.advice.GlobalControllerAdvice`
- 역할:
  - 도메인별 ControllerAdvice가 처리하지 못한 `DomainException` 처리
  - 새로 추가된 예외가 아직 도메인 ControllerAdvice에 등록되지 않은 경우 처리
  - 예상치 못한 예외 (`Exception`) 처리

```java
@Slf4j
@RestControllerAdvice
public class GlobalControllerAdvice {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException e) {
        log.warn("Unhandled domain exception caught by GlobalControllerAdvice: {}", e.getClass().getSimpleName());
        return ResponseEntity
            .status(e.getStatus())
            .body(ErrorResponse.of(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpectedException(Exception e) {
        log.error("Unexpected error occurred", e);
        return ErrorResponse.of("Internal Server Error");
    }
}
```

**예외 처리 우선순위:**
1. 도메인별 ControllerAdvice (구체적인 예외 타입 매칭)
2. GlobalControllerAdvice의 `DomainException` 핸들러 (fallback)
3. GlobalControllerAdvice의 `Exception` 핸들러 (최종 fallback)

**주의사항:**
- 새로운 예외를 추가할 때는 **반드시 해당 도메인의 ControllerAdvice에 등록**합니다.
- GlobalControllerAdvice에서 warn 로그가 발생하면, 해당 예외를 도메인 ControllerAdvice에 추가해야 합니다.

도메인별 예외 목록
---

### Site (좌석 배정)
- `SeatAlreadyHeldException`: 좌석이 이미 점유된 경우
- `SeatAlreadyOccupiedException`: 좌석이 이미 확정된 경우
- `SeatNotAvailableException`: 좌석을 사용할 수 없는 경우
- `UnauthorizedSeatReleaseException`: 권한 없이 좌석 해제 시도
- `SeatNotFoundException`: 좌석을 찾을 수 없는 경우

### User
- `DuplicateEmailException`: 이메일 중복
- `LoginFailureException`: 로그인 실패
- `UserNotFoundException`: 사용자를 찾을 수 없는 경우

### Match
- `MatchNotFoundException`: 경기를 찾을 수 없는 경우

### Reservation
- `ReservationNotFoundException`: 예약을 찾을 수 없는 경우
- `ReservationAlreadyCancelledException`: 예약이 이미 취소된 경우

### Payment
- `PaymentNotFoundException`: 결제를 찾을 수 없는 경우
- `PaymentAlreadyCompletedException`: 결제가 이미 완료된 경우

체크리스트
---

새로운 예외를 추가할 때:

- [ ] 예외 이름이 도메인 상황을 명확히 표현하는가?
- [ ] 적절한 도메인 패키지에 위치하는가?
- [ ] 필요한 경우 추가 정보(seatId, userId 등)를 포함하는가?
- [ ] ControllerAdvice에 적절한 HTTP 상태 코드가 매핑되었는가?
- [ ] 범용 예외(`IllegalStateException` 등)를 사용하지 않았는가?
