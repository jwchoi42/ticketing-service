package dev.ticketing.core.site.application.port.in.allocation.status;

import dev.ticketing.core.site.domain.allocation.Allocation;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GetAllocationStatusChangesUseCase - 좌석 현황 변경분 조회 포트
 * 특정 시간 이후 변경된 좌석 상태만 조회 (서버 폴링용)
 */
public interface GetAllocationStatusChangesUseCase {
    List<Allocation> getChangesSince(Long matchId, Long blockId, LocalDateTime since);
}
