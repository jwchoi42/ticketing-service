package dev.ticketing.core.site.application.service.exception;

import org.springframework.http.HttpStatus;

public class SeatAlreadyHeldException extends SiteException {

    public SeatAlreadyHeldException(final Long matchId, final Long seatId) {
        super(String.format("Seat %d is already held for match %d", seatId, matchId), HttpStatus.CONFLICT);
    }
}
