package dev.ticketing.core.site.adapter.in.web.hierarchy.model.response;

import dev.ticketing.core.site.domain.hierarchy.Seat;
import java.util.List;

public record SeatListResponse(List<SeatResponse> seats) {
    public static SeatListResponse from(List<Seat> seats) {
        return new SeatListResponse(seats.stream().map(SeatResponse::from).toList());
    }

    public record SeatResponse(Long id, Integer rowNumber, Integer seatNumber) {
        public static SeatResponse from(Seat seat) {
            return new SeatResponse(seat.getId(), seat.getRowNumber(), seat.getSeatNumber());
        }
    }
}
