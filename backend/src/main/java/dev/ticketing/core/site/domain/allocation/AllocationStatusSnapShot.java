package dev.ticketing.core.site.domain.allocation;

import java.util.List;

/**
 * AllocationStatusSnapShot: 전체 좌석 현황 스냅샷 (Domain 모델)
 * 특정 시점의 좌석 상태 목록을 나타내며, 향후 통계정보 등의 비즈니스 로직이 추가될 수 있음
 */
public record AllocationStatusSnapShot(List<AllocationStatus> allocationStatuses) {

    public static AllocationStatusSnapShot from(List<AllocationStatus> allocationStatuses) {
        return new AllocationStatusSnapShot(allocationStatuses);
    }
}
