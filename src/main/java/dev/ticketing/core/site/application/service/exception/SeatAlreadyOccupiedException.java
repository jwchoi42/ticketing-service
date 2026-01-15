package dev.ticketing.core.site.application.service.exception;

import org.springframework.http.HttpStatus;

public class SeatAlreadyOccupiedException extends SiteException {

    public SeatAlreadyOccupiedException(final Long matchId, final Long seatId) {
        super(String.format("Seat %d is already occupied for match %d", seatId, matchId), HttpStatus.CONFLICT);
    }
}
