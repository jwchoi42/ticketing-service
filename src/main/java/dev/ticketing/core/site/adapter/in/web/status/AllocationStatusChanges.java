package dev.ticketing.core.site.adapter.in.web.status;

import dev.ticketing.core.site.domain.allocation.AllocationStatus;
import dev.ticketing.core.site.domain.allocation.Allocation;

import java.util.List;

/**
 * AllocationStatusChanges: 좌석 상태 변경 사항 (목록)
 */
public record AllocationStatusChanges(
        List<SeatChange> changes) {

    public record SeatChange(
            Long seatId,
            AllocationStatus status) {
        public static SeatChange from(Allocation allocation) {
            return new SeatChange(allocation.getSeatId(), allocation.getStatus());
        }
    }

    public static AllocationStatusChanges from(List<Allocation> allocations) {
        List<SeatChange> changes = allocations.stream()
                .map(SeatChange::from)
                .toList();
        return new AllocationStatusChanges(changes);
    }
}
