package dev.ticketing.core.reservation.application.service.exception;

import org.springframework.http.HttpStatus;

public class ReservationSeatNotHeldException extends ReservationException {

    public ReservationSeatNotHeldException(final Long seatId, final Long userId) {
        super(String.format("Seat %d is not held by user %d", seatId, userId), HttpStatus.CONFLICT);
    }
}
