package dev.ticketing.core.site.adapter.in.web.status;

import dev.ticketing.core.site.domain.allocation.AllocationStatus;

import java.util.List;

/**
 * AllocationStatusChanges: 좌석 상태 변경 사항 (목록)
 */
public record AllocationStatusChanges(
        List<AllocationStatus> changes) {

    public static AllocationStatusChanges from(List<AllocationStatus> allocations) {
        return new AllocationStatusChanges(allocations);
    }
}
