package dev.ticketing.core.site.application.port.out.persistence.allocation.status;

import dev.ticketing.core.site.domain.allocation.AllocationStatus;

/**
 * 좌석 상태 변경 이벤트 발행 Port
 */
public interface PublishAllocationEventPort {

    /**
     * 좌석 상태 변경 이벤트를 Redis Stream에 발행
     *
     * @param matchId 경기 ID
     * @param blockId 구간 ID
     * @param seatId  좌석 ID
     * @param status  변경된 좌석 상태
     */
    void publishAllocationStatusChangeEvent(Long matchId, Long blockId, Long seatId, AllocationStatus status);
}
