package dev.ticketing.core.reservation.adapter.in.web.model.response;

import dev.ticketing.core.reservation.domain.Reservation;
import dev.ticketing.core.reservation.domain.ReservationStatus;
import java.util.List;

public record ReservationResponse(
        Long id,
        Long userId,
        Long matchId,
        ReservationStatus status,
        List<Long> seatIds) {
    public static ReservationResponse from(final Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getUserId(),
                reservation.getMatchId(),
                reservation.getStatus(),
                reservation.getSeatIds());
    }
}
