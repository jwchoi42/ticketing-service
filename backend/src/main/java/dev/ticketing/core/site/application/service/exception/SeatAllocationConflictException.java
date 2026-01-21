package dev.ticketing.core.site.application.service.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a race condition is detected during seat allocation.
 * This occurs when another user acquires the seat between the check and the allocation attempt.
 */
public class SeatAllocationConflictException extends SiteException {

    public SeatAllocationConflictException(final Long matchId, final Long seatId) {
        super(String.format("Seat %d for match %d was acquired by another user", seatId, matchId), HttpStatus.CONFLICT);
    }
}
