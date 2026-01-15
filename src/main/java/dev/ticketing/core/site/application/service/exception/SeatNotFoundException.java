package dev.ticketing.core.site.application.service.exception;

import org.springframework.http.HttpStatus;

public class SeatNotFoundException extends SiteException {

    public SeatNotFoundException(final Long seatId) {
        super(String.format("Seat not found: %d", seatId), HttpStatus.NOT_FOUND);
    }
}
