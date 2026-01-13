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

### 위치
```
src/main/java/dev/ticketing/core/{domain}/application/service/exception/
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

### 예외 핸들러(`ControllerAdvice`) 예시
```java
@RestControllerAdvice
public class AllocationControllerAdvice {

    @ExceptionHandler(SeatAlreadyHeldException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleSeatAlreadyHeld(SeatAlreadyHeldException e) {
        return ErrorResponse.of(409, e.getMessage());
    }

    @ExceptionHandler(SeatNotAvailableException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleSeatNotAvailable(SeatNotAvailableException e) {
        return ErrorResponse.of(409, e.getMessage());
    }

    @ExceptionHandler(UnauthorizedSeatReleaseException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleUnauthorizedRelease(UnauthorizedSeatReleaseException e) {
        return ErrorResponse.of(403, e.getMessage());
    }
}
```

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
