package dev.ticketing.core.site.application.port.out.persistence.allocation.status;

import dev.ticketing.core.site.domain.allocation.AllocationStatus;

import java.time.Duration;

/**
 * 좌석 상태 업데이트 Port
 */
public interface UpdateAllocationStatusPort {

    /**
     * 좌석 상태를 원자적으로 업데이트 (TTL 포함)
     * - Redis의 Lua Script를 사용하여 Race Condition 방지
     * - Write-Behind 패턴: HOLD 상태는 TTL 설정, OCCUPIED는 TTL 제거
     *
     * @param matchId        경기 ID
     * @param seatId         좌석 ID
     * @param expectedStatus 예상되는 현재 상태 (null이면 상태 체크 안 함)
     * @param newStatus      새로운 상태
     * @param ttl            TTL (null이면 TTL 설정 안 함, HOLD 상태는 필수)
     * @return 업데이트 성공 여부
     */
    boolean updateAllocationStatusAtomicWithTTL(
            Long matchId,
            Long seatId,
            AllocationStatus expectedStatus,
            AllocationStatus newStatus,
            Duration ttl);

    /**
     * 좌석 상태를 원자적으로 업데이트
     * - Redis의 SETNX 또는 Lua Script를 사용하여 Race Condition 방지
     *
     * @param matchId        경기 ID
     * @param seatId         좌석 ID
     * @param expectedStatus 예상되는 현재 상태 (null이면 상태 체크 안 함)
     * @param newStatus      새로운 상태
     * @return 업데이트 성공 여부
     */
    boolean updateAllocationStatusAtomic(Long matchId, Long seatId, AllocationStatus expectedStatus,
            AllocationStatus newStatus);

    /**
     * 좌석 상태를 강제로 업데이트 (상태 체크 없이)
     *
     * @param matchId   경기 ID
     * @param seatId    좌석 ID
     * @param newStatus 새로운 상태
     */
    void updateAllocationStatus(Long matchId, Long seatId, AllocationStatus newStatus);

    /**
     * 좌석 상태의 TTL 제거 (영구 저장)
     * - Write-Behind 패턴: HOLD → OCCUPIED 전환 시 사용
     *
     * @param matchId 경기 ID
     * @param seatId  좌석 ID
     */
    void removeTTL(Long matchId, Long seatId);
}
