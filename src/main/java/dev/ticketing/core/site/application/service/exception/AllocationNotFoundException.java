package dev.ticketing.core.site.application.service.exception;

import org.springframework.http.HttpStatus;

public class AllocationNotFoundException extends SiteException {

    public AllocationNotFoundException(final Long matchId, final Long seatId) {
        super(String.format("Allocation not found for seat %d in match %d", seatId, matchId), HttpStatus.NOT_FOUND);
    }
}
