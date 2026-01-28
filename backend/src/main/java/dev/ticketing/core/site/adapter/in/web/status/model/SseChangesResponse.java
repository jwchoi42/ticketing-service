package dev.ticketing.core.site.adapter.in.web.status.model;

import dev.ticketing.core.site.domain.allocation.AllocationState;
import dev.ticketing.core.site.domain.allocation.AllocationStatus;

import java.util.List;

/**
 * SSE Changes 응답 DTO
 * 변경된 배정 상태만 전송
 */
public record SseChangesResponse(List<ChangeInfo> changes) {

    public static SseChangesResponse from(List<AllocationStatus> allocationStatuses) {
        return new SseChangesResponse(
                allocationStatuses.stream().map(ChangeInfo::from).toList()
        );
    }

    public record ChangeInfo(Long seatId, AllocationState state) {
        public static ChangeInfo from(AllocationStatus status) {
            return new ChangeInfo(status.seatId(), status.state());
        }
    }
}
