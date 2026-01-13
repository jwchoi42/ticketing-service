package dev.ticketing.core.reservation.application.port.in;

import java.util.List;

public record CreateReservationCommand(
        Long userId,
        Long matchId,
        List<Long> seatIds) {
}
