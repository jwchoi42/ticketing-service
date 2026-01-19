package dev.ticketing.core.reservation.application.service.exception;

import org.springframework.http.HttpStatus;

import dev.ticketing.common.exception.DomainException;

/**
 * Reservation 도메인 최상위 예외
 */
public class ReservationException extends DomainException {

    public ReservationException(final String message, final HttpStatus status) {
        super(message, status);
    }
}
