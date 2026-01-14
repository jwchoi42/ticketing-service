package dev.ticketing.core.reservation.application.service.exception;

import org.springframework.http.HttpStatus;

import dev.ticketing.core.reservation.domain.ReservationStatus;

public class InvalidReservationStateException extends ReservationException {

    public InvalidReservationStateException(final Long reservationId, final ReservationStatus status) {
        super(String.format("Reservation %d is in invalid state: %s", reservationId, status), HttpStatus.CONFLICT);
    }
}
