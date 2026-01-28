package dev.ticketing.core.site.application.port.in.allocation.status;

import dev.ticketing.core.site.domain.allocation.AllocationStatusSnapShot;

/**
 * GetAllocationStatusSnapShotUseCase - 좌석 현황 스냅샷 조회 포트
 * Application Layer는 기술 중립적이어야 하므로 도메인 타입만 반환
 */
public interface GetAllocationStatusSnapShotUseCase {
    AllocationStatusSnapShot getAllocationStatusSnapShotByMatchIdAndBlockId(Long matchId, Long blockId);
}
