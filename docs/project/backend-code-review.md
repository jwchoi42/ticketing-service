# 백엔드 코드 분석 보고서

> 작성일: 2026-01-14
> 대상: `src/main/java/dev/ticketing` 백엔드 전체

---

## 1. 동시성 이슈 (Critical)

### 1.1 좌석 할당 시 Race Condition

**위치**: `AllocationService.java:56-79`

```java
Allocation allocation = loadAllocationPort.loadAllocationWithLock(matchId, seatId)
        .orElse(Allocation.available(seatId));  // ← 문제 지점
```

**문제점**:
- Allocation 레코드가 없을 때 `PESSIMISTIC_WRITE` 락이 작동하지 않음
- 두 사용자가 동시에 같은 좌석을 요청하면:
  1. 둘 다 `Optional.empty()` 반환
  2. 둘 다 새 Allocation 객체 생성
  3. **Unique Index 위반** 또는 **덮어쓰기** 발생

**권장 해결책**:
- `INSERT ... ON CONFLICT` (Upsert) 패턴 사용
- 또는 Redis 분산 락 (Redisson) 도입

### 1.2 ReservationService 중복 락 획득

**위치**: `ReservationService.java:39-61`

```java
// 첫 번째 순회: 검증
for (final Long seatId : seatIds) {
    loadAllocationPort.loadAllocationWithLock(matchId, seatId)...
}
// 두 번째 순회: 업데이트
for (final Long seatId : seatIds) {
    loadAllocationPort.loadAllocationWithLock(matchId, seatId)...  // 또 락 획득
}
```

**문제점**: 같은 트랜잭션 내에서 동일 레코드에 락을 두 번 획득 - 불필요한 오버헤드

---

## 2. 확장성 이슈 (Critical)

### 2.1 SSE 싱글 인스턴스 한계

**위치**: `AllocationStatusService.java:35`

```java
private final Map<String, List<SseEmitter>> allocationStatusEmitters = new ConcurrentHashMap<>();
```

**문제점**:
- SSE 연결이 JVM 메모리에만 저장됨
- **Scale-out 시**: 서버 A에서 좌석 변경 → 서버 B의 SSE 클라이언트에게 전달 불가
- 로드밸런서 환경에서 실시간 동기화 실패

**권장 해결책**: Redis Pub/Sub 또는 Kafka를 통한 이벤트 브로드캐스트

### 2.2 DB 폴링 기반 변경 감지

**위치**: `AllocationStatusService.java:96-150`

```java
@Scheduled(fixedRate = 1000)  // 매 1초마다
public void checkForUpdates() {
    allocationStatusEmitters.forEach((key, emitterList) -> {
        List<Allocation> changes = loadAllocationStatusPort
            .loadAllocationStatusesSince(matchId, blockId, lastCheckTime);  // DB 조회
    });
}
```

**문제점**:
- **연결된 블록 수 × 초당 1회** 쿼리 발생
- 100개 블록 연결 시 → 분당 6,000 쿼리
- 대규모 트래픽에서 DB 과부하

**권장 해결책**:
- CDC (Change Data Capture) 도입
- Allocation 저장 시 직접 이벤트 발행

### 2.3 서버 상태 변수 의존

**위치**: `AllocationStatusService.java:38`

```java
private LocalDateTime lastCheckTime = LocalDateTime.now().minusMinutes(10);
```

**문제점**:
- 서버 재시작 시 과거 10분 데이터로 초기화
- 멀티 인스턴스에서 각각 다른 시간 추적

---

## 3. 성능 이슈 (High)

### 3.1 N+1 쿼리 패턴

**위치**: `PaymentService.java:100-104`

```java
List<Allocation> allocations = loadAllocationPort.loadAllocationsByReservationId(reservation.getId());
for (final Allocation allocation : allocations) {
    final Allocation occupiedAllocation = allocation.occupy();
    recordAllocationPort.recordAllocation(occupiedAllocation);  // 개별 저장
}
```

**문제점**: 좌석 수만큼 UPDATE 쿼리 발생

**권장 해결책**: `@Modifying @Query`로 벌크 업데이트

### 3.2 인덱스 부족

**위치**: `AllocationEntity.java:23-26`

```java
@Table(name = "allocations", indexes = {
    @Index(name = "idx_match_seat_unique", columnList = "matchId, seatId", unique = true),
    @Index(name = "idx_allocations_updated_at", columnList = "updatedAt")
})
```

**누락된 인덱스**:
- `reservationId` 단독 인덱스 (조회 빈번)
- `(matchId, seatId, updatedAt)` 복합 인덱스 (폴링 최적화)

### 3.3 SeatRepository 인덱스 부재

**위치**: `SeatRepository.java:10`

```java
List<SeatEntity> findByBlockId(Long blockId);
```

**문제점**: `blockId`에 대한 인덱스가 SeatEntity에 없음 (매번 Full Scan)

---

## 4. 보안 이슈 (Critical)

### 4.1 평문 비밀번호 저장

**위치**: `User.java:32-34`

```java
public boolean matchPassword(final String password) {
    return this.password.equals(password);  // 평문 비교!
}
```

**문제점**:
- DB 탈취 시 모든 비밀번호 노출
- 법적 컴플라이언스 위반 (개인정보보호법)

**권장 해결책**: BCrypt, Argon2 등 해시 함수 사용

### 4.2 인증/인가 부재

**위치**: 전체 Controller

```java
// 예: AllocationController에서 userId를 클라이언트가 직접 전달
public void allocateSeat(@RequestBody AllocateSeatRequest request) {
    // request.userId()를 신뢰 - 위조 가능
}
```

**문제점**:
- 다른 사용자로 위장하여 좌석 점유/해제 가능
- JWT/Session 기반 인증 필요

---

## 5. 데이터 일관성 이슈 (High)

### 5.1 Hold 만료 자동 해제 부재

**위치**: 시스템 전체

**현재 동작**:
- 좌석 HOLD 시 `holdExpiresAt` 설정 (10분)
- 만료된 HOLD를 AVAILABLE로 변경하는 스케줄러 없음
- 새 사용자가 좌석 선택 시에만 체크

**문제점**:
- 사용자가 이탈 후 10분간 좌석이 "유령 점유" 상태
- 다른 사용자가 해당 좌석 선택해야만 해제됨

**권장 해결책**: `@Scheduled`로 만료 HOLD 정리 배치 작업

### 5.2 결제 실패 시 부분 롤백 위험

**위치**: `PaymentService.java:79-104`

```java
// 외부 API 호출 (트랜잭션 밖에서 실행됨)
final boolean success = paymentGatewayPort.executePayment(...);
if (success) {
    // Payment, Reservation, Allocation 모두 업데이트
}
```

**문제점**:
- 외부 결제 성공 후 DB 업데이트 실패 시 → 결제됐지만 좌석 미확정
- 보상 트랜잭션 (환불) 로직 없음

---

## 6. 기타 이슈 (Medium)

### 6.1 미구현 기능

**위치**: `AllocationService.java:107-123`

```java
@Override
public List<Allocation> confirmSeats(final ConfirmSeatsCommand command) {
    List<Allocation> confirmedSeats = new ArrayList<>();
    // TODO: Implement reservation persistence
    if (confirmedSeats.isEmpty()) {
        throw new NoSeatsToConfirmException(...);  // 항상 예외 발생
    }
}
```

### 6.2 프로덕션 위험 설정

**위치**: `application.yaml:12`

```yaml
jpa:
  hibernate:
    ddl-auto: create  # 서버 재시작 시 데이터 전체 삭제!
```

### 6.3 Redis 미활용

- `RedisConfig.java`가 설정되어 있으나 실제로 좌석 상태 캐싱/분산 락에 활용되지 않음

---

## 우선순위별 정리

| 우선순위 | 이슈 | 영향도 |
|---------|------|--------|
| P0 | 평문 비밀번호 | 보안 사고 |
| P0 | 인증/인가 부재 | 데이터 위변조 |
| P0 | Race Condition | 좌석 이중 예매 |
| P1 | SSE 싱글 인스턴스 | Scale-out 불가 |
| P1 | DB 폴링 부하 | 대규모 트래픽 장애 |
| P1 | Hold 만료 미처리 | 좌석 유령 점유 |
| P2 | N+1 쿼리 | 성능 저하 |
| P2 | 인덱스 부족 | 조회 성능 저하 |
| P3 | DDL Auto Create | 데이터 손실 |

---

## 개선 로드맵 제안

### Phase 1: 보안 필수 조치
1. 비밀번호 해싱 (BCrypt)
2. Spring Security + JWT 인증 도입
3. DDL Auto를 `validate`로 변경 + Flyway/Liquibase 마이그레이션

### Phase 2: 동시성/일관성
1. 좌석 할당 Upsert 패턴 적용
2. Hold 만료 스케줄러 구현
3. 결제 Saga 패턴 또는 Outbox 패턴 도입

### Phase 3: 확장성
1. Redis Pub/Sub 기반 SSE 브로드캐스트
2. 폴링 → 이벤트 드리븐 전환
3. 인덱스 최적화

### Phase 4: 성능
1. N+1 쿼리 벌크 처리
2. 캐싱 전략 수립
