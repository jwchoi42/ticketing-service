package dev.ticketing.core.reservation.application.service.exception;

import org.springframework.http.HttpStatus;

public class ReservationHoldExpiredException extends ReservationException {

    public ReservationHoldExpiredException(final Long seatId) {
        super(String.format("Seat %d hold has expired", seatId), HttpStatus.CONFLICT);
    }
}
