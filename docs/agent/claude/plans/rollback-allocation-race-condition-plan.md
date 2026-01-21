Plan: Allocation 비관락 단순화

 목표

 Match open 시 모든 좌석에 AVAILABLE allocation이 생성되므로, AllocationService에서 upsert fallback
 로직을 제거하고 비관락(pessimistic lock)만 사용하도록 단순화한다.

 배경

 - 현재 MatchService.openMatch():109-115에서 이미 모든 좌석에 대해 AVAILABLE allocation을 생성함
 - 하지만 AllocationService.allocateSeat():99-112에서 allocation이 없을 경우 upsert fallback 로직이
 여전히 존재
 - engineering-note-allocation-record-strategy.md에서 권장한 "방안 B: Pre-populate" 방식의 Phase 4
 정리 작업에 해당

 수정 대상 파일

 1. AllocationService.java (핵심 변경)

 경로: backend/src/main/java/dev/ticketing/core/site/application/service/AllocationService.java

 변경 내용:
 - allocateSeat() 메서드에서 else 분기 (lines 99-112) 제거
 - allocation이 없으면 AllocationNotFoundException throw
 - SeatAllocationConflictException import 제거

 변경 전 (lines 73-112):
 Optional<Allocation> existingAllocation =
 loadAllocationPort.loadAllocationByMatchAndSeatWithLock(matchId, seatId);

 if (existingAllocation.isPresent()) {
     // ... 기존 로직 유지 ...
 } else {
     // No existing allocation - use atomic upsert to prevent race condition
     Optional<Allocation> result = recordAllocationPort.tryHoldSeat(userId, matchId, seatId,
 expiresAt);
     // ...
 }

 변경 후:
 Allocation allocation = loadAllocationPort.loadAllocationByMatchAndSeatWithLock(matchId, seatId)
         .orElseThrow(() -> new AllocationNotFoundException(matchId, seatId));

 if (allocation.getStatus() == AllocationStatus.OCCUPIED) {
     throw new SeatAlreadyOccupiedException(matchId, seatId);
 }

 if (allocation.getStatus() == AllocationStatus.HOLD &&
         allocation.getHoldExpiresAt() != null && allocation.getHoldExpiresAt().isAfter(now)) {
     if (allocation.getUserId() != null && allocation.getUserId().equals(userId)) {
         return;  // Already held by same user
     }
     throw new SeatAlreadyHeldException(matchId, seatId);
 }

 // AVAILABLE or expired HOLD - update it
 Allocation heldAllocation = allocation.hold(userId, matchId, expiresAt);
 Allocation saved = recordAllocationPort.recordAllocation(heldAllocation);

 2. RecordAllocationPort.java (인터페이스 정리)

 경로: backend/src/main/java/dev/ticketing/core/site/application/port/out/persistence/allocation/Reco
 rdAllocationPort.java

 변경 내용: tryHoldSeat() 메서드 제거

 3. AllocationPersistenceAdapter.java (구현체 정리)

 경로: backend/src/main/java/dev/ticketing/core/site/adapter/out/persistence/allocation/AllocationPer
 sistenceAdapter.java

 변경 내용: tryHoldSeat() 구현 제거

 4. AllocationRepository.java (Repository 정리)

 경로: backend/src/main/java/dev/ticketing/core/site/adapter/out/persistence/allocation/AllocationRep
 ository.java

 변경 내용: tryHoldSeat() native query 제거

 5. SeatAllocationConflictException.java (예외 클래스 제거)

 경로: backend/src/main/java/dev/ticketing/core/site/application/service/exception/SeatAllocationConf
 lictException.java

 변경 내용: 더 이상 사용되지 않으므로 파일 삭제

 6. 테스트 코드 업데이트

 경로: backend/src/test/java/dev/ticketing/integration/SeatAllocationConcurrencyTest.java

 변경 내용:
 - SeatAllocationConflictException import 제거
 - catch 블록에서 SeatAllocationConflictException 제거 (더 이상 발생하지 않음)
 - @BeforeEach에서 AVAILABLE allocation을 미리 생성:
 // 기존: allocation 삭제
 jdbcTemplate.update("DELETE FROM allocations WHERE match_id = ? AND seat_id = ?", matchId, seatId);

 // 변경: AVAILABLE allocation 생성
 jdbcTemplate.update("""
     INSERT INTO allocations (user_id, match_id, seat_id, status, hold_expires_at, updated_at)
     VALUES (NULL, ?, ?, 'AVAILABLE', NULL, NOW())
     ON CONFLICT (match_id, seat_id) DO UPDATE SET status = 'AVAILABLE', user_id = NULL
     """, matchId, seatId);

 검증 방법

 1. 기존 테스트 실행: ./gradlew test
 2. 동시성 테스트 확인: SeatAllocationConcurrencyTest 통과 확인
 3. 통합 테스트: Match open → seat hold 시나리오 테스트