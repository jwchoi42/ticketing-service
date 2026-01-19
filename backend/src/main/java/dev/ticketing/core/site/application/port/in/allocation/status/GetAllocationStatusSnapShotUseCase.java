package dev.ticketing.core.site.application.port.in.allocation.status;

import dev.ticketing.core.site.adapter.in.web.status.AllocationStatusSnapShot;

/**
 * GetAllocationStatusSnapShotUseCase - 좌석 현황 스냅샷 조회 포트
 */
public interface GetAllocationStatusSnapShotUseCase {
    AllocationStatusSnapShot getAllocationStatusSnapShot(Long matchId, Long blockId);
}
