---
trigger: model_decision
description: seat-allocation-naming
---

좌석 배정 네이밍 규칙
===

좌석 배정 단계별 용어 정의
---

### 1. Hold (선점)
- **의미**: 사용자가 좌석을 임시로 점유하는 단계 (5분 TTL)
- **상태**: `HOLD`
- **메소드명**: `allocateSeat` / `holdSeat`
- **엔드포인트**: `POST /api/matches/{matchId}/allocation/seats/{seatId}/hold`

### 2. Release (해제)
- **의미**: 선점한 좌석을 다시 사용 가능한 상태로 되돌리는 단계
- **상태**: `AVAILABLE`
- **메소드명**: `releaseSeat`
- **엔드포인트**: `POST /api/matches/{matchId}/allocation/seats/{seatId}/release`

### 3. ⭐Confirm (확정) - 권장
- **의미**: 선점한 좌석을 최종 확정하여 예약으로 전환하는 단계
- **상태**: `HOLD` → `OCCUPIED` (예약 생성과 함께)
- **메소드명**: `confirmSeats`
- **Command**: `ConfirmSeatsCommand`
- **Request**: `ConfirmSeatsRequest`
- **Response**: `ConfirmSeatsResponse`
- **UseCase**: `ConfirmSeatsUseCase`
- **엔드포인트**: `POST /api/matches/{matchId}/allocation/seats/confirm`

### ❌ Complete (완료) - 사용 금지
- **문제점**: "완료"는 모호한 표현으로, 단순 프로세스가 끝났다는 의미만 전달
- **혼동 가능성**: 사용자가 선택을 "끝냈다"는 의미인지, 시스템 처리를 "완료"했다는 의미인지 불분명

네이밍 규칙
---

### UseCase
```java
// ✅ 올바른 예시
public interface ConfirmSeatsUseCase {
    List<Allocation> confirmSeats(ConfirmSeatsCommand command);
}

// ❌ 잘못된 예시
public interface CompleteSeatSelectionUseCase {
    List<Allocation> completeSeatSelection(CompleteSeatSelectionCommand command);
}
```

### Command
```java
// ✅ 올바른 예시
public record ConfirmSeatsCommand(
    Long matchId,
    List<Long> seatIds,
    Long userId
) {}

// ❌ 잘못된 예시
public record CompleteSeatSelectionCommand(
    Long matchId,
    List<Long> seatIds
) {}
```

### Request/Response
```java
// ✅ 올바른 예시
public record ConfirmSeatsRequest(Long userId, List<Long> seatIds) {}
public record ConfirmSeatsResponse(List<AllocationInfo> allocations) {}

// ❌ 잘못된 예시
public record CompleteSeatSelectionRequest(List<Long> seatIds) {}
public record CompleteSeatSelectionResponse(List<AllocationInfo> allocations) {}
```

### Controller Method
```java
// ✅ 올바른 예시
@PostMapping("/seats/confirm")
public SuccessResponse<ConfirmSeatsResponse> confirmSeats(
    @PathVariable Long matchId,
    @RequestBody ConfirmSeatsRequest request
) {
    var confirmedSeats = confirmSeatsUseCase.confirmSeats(request.toCommand(matchId));
    return SuccessResponse.of(ConfirmSeatsResponse.from(confirmedSeats));
}

// ❌ 잘못된 예시
@PostMapping("/seats/complete")
public SuccessResponse<CompleteSeatSelectionResponse> completeSeatSelection(
    @PathVariable Long matchId,
    @RequestBody CompleteSeatSelectionRequest request
) {
    // ...
}
```

전체 흐름 정리
---

```
1. AVAILABLE (사용 가능)
   ↓
2. HOLD (선점) - allocateSeat/holdSeat
   ↓
3. OCCUPIED (확정) - confirmSeats ⭐
   ↓
4. 예약 생성 (Reservation)
```

용어 비교표
---

| 단계 | 동사 | 명사 | 메소드명 | 상태 변화 |
|------|------|------|----------|-----------|
| 선점 | Hold | Hold | `holdSeat` / `allocateSeat` | AVAILABLE → HOLD |
| 해제 | Release | Release | `releaseSeat` | HOLD → AVAILABLE |
| 확정 | ⭐Confirm | ⭐Confirmation | ⭐`confirmSeats` | HOLD → OCCUPIED |
| 완료 | ❌Complete | ❌Completion | ❌`completeSeatSelection` | ❌사용 금지 |

체크리스트
---

좌석 배정 관련 코드 작성 시:

- [ ] "Complete" 대신 "Confirm"을 사용했는가?
- [ ] 메소드명이 `confirmSeats`인가? (단수 seat가 아닌 복수 seats)
- [ ] Command, Request, Response 이름이 일관되게 `ConfirmSeats*`로 시작하는가?
- [ ] 엔드포인트가 `/seats/confirm`인가?
- [ ] 상태 전환이 명확히 문서화되어 있는가?
