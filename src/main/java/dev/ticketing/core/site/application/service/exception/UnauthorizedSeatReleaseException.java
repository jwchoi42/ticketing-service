package dev.ticketing.core.site.application.service.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedSeatReleaseException extends SiteException {

    public UnauthorizedSeatReleaseException(final Long matchId, final Long seatId, final Long userId) {
        super(String.format("User %d is not authorized to release seat %d for match %d", userId, seatId, matchId),
                HttpStatus.FORBIDDEN);
    }
}
