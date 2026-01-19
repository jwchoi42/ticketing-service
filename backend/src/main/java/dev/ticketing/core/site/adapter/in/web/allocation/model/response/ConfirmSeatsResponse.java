package dev.ticketing.core.site.adapter.in.web.allocation.model.response;

import dev.ticketing.core.site.domain.allocation.AllocationStatus;
import dev.ticketing.core.site.domain.allocation.Allocation;

import java.util.List;

/**
 * 좌석 선택 확정 응답
 */
public record ConfirmSeatsResponse(
        List<ConfirmedSeat> confirmedSeats
) {

    public record ConfirmedSeat(
            Long seatId,
            AllocationStatus status
    ) {
        public static ConfirmedSeat from(Allocation seat) {
            return new ConfirmedSeat(seat.getSeatId(), seat.getStatus());
        }
    }

    public static ConfirmSeatsResponse from(List<Allocation> seats) {
        List<ConfirmedSeat> confirmedSeats = seats.stream()
                .map(ConfirmedSeat::from)
                .toList();
        return new ConfirmSeatsResponse(confirmedSeats);
    }
}
