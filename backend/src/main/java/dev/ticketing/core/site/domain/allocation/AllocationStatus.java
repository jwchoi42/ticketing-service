package dev.ticketing.core.site.domain.allocation;

import java.time.LocalDateTime;

/**
 * AllocationStatus: 할당 정황(상태 정보)을 담는 조회 전용 모델 (Read Model)
 * 기존 Enum에서 객체로 격상됨
 */
public record AllocationStatus(
        Long id,
        Long matchId,
        Long blockId,
        Long seatId,
        AllocationState state,
        LocalDateTime holdExpiresAt,
        LocalDateTime updatedAt
) {
    public static AllocationStatus from(Allocation allocation) {
        return new AllocationStatus(
                allocation.getId(),
                allocation.getMatchId(),
                allocation.getBlockId(),
                allocation.getSeatId(),
                allocation.getState(),
                allocation.getHoldExpiresAt(),
                allocation.getUpdatedAt()
        );
    }
}
