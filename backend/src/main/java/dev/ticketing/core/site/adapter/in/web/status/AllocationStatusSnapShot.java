package dev.ticketing.core.site.adapter.in.web.status;

import dev.ticketing.core.site.domain.allocation.AllocationStatus;
import dev.ticketing.core.site.domain.allocation.Allocation;

import java.util.List;

/**
 * AllocationStatusSnapShot: 전체 좌석 현황 스냅샷
 */
public record AllocationStatusSnapShot(
        List<SeatResponse> seats) {

    public record SeatResponse(
            Long id,
            AllocationStatus status) {
        public static SeatResponse from(Allocation seat) {
            return new SeatResponse(seat.getSeatId(), seat.getStatus());
        }
    }

    public static AllocationStatusSnapShot from(List<Allocation> seats) {
        List<SeatResponse> seatResponses = seats.stream()
                .map(SeatResponse::from)
                .toList();
        return new AllocationStatusSnapShot(seatResponses);
    }
}
