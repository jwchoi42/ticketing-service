package dev.ticketing.core.reservation.application.service.exception;

import org.springframework.http.HttpStatus;

public class ReservationNotFoundException extends ReservationException {

    public ReservationNotFoundException(final Long reservationId) {
        super(String.format("Reservation not found: %d", reservationId), HttpStatus.NOT_FOUND);
    }
}
