package dev.ticketing.core.reservation.adapter.in.web.model;

import dev.ticketing.core.reservation.application.port.in.CreateReservationCommand;
import java.util.List;

public record CreateReservationRequest(
        Long userId,
        Long matchId,
        List<Long> seatIds) {
    public CreateReservationCommand toCommand() {
        return new CreateReservationCommand(userId, matchId, seatIds);
    }
}
