package dev.ticketing.core.site.adapter.in.web.status.model;

import dev.ticketing.core.site.domain.allocation.AllocationState;
import dev.ticketing.core.site.domain.allocation.AllocationStatus;
import dev.ticketing.core.site.domain.hierarchy.Seat;

import java.util.List;

/**
 * SSE Snapshot 응답 DTO
 * 좌석 정보(정적)와 배정 상태(동적)를 함께 전송
 */
public record SseSnapshotResponse(
        List<SeatInfo> seats,
        List<AllocationInfo> allocationStatuses
) {
    public static SseSnapshotResponse of(List<Seat> seats, List<AllocationStatus> allocationStatuses) {
        return new SseSnapshotResponse(
                seats.stream().map(SeatInfo::from).toList(),
                allocationStatuses.stream().map(AllocationInfo::from).toList()
        );
    }

    public record SeatInfo(Long id, Integer rowNumber, Integer seatNumber) {
        public static SeatInfo from(Seat seat) {
            return new SeatInfo(seat.getId(), seat.getRowNumber(), seat.getSeatNumber());
        }
    }

    public record AllocationInfo(Long seatId, AllocationState state) {
        public static AllocationInfo from(AllocationStatus status) {
            return new AllocationInfo(status.seatId(), status.state());
        }
    }
}
