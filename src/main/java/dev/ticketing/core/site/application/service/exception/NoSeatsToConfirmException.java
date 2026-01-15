package dev.ticketing.core.site.application.service.exception;

import java.util.List;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class NoSeatsToConfirmException extends SiteException {

    private final Long userId;
    private final Long matchId;
    private final List<Long> requestedSeatIds;

    public NoSeatsToConfirmException(final Long userId, final Long matchId, final List<Long> requestedSeatIds) {
        super(String.format("No seats to confirm for user %d in match %d. Requested seats: %s",
                userId, matchId, requestedSeatIds), HttpStatus.BAD_REQUEST);
        this.userId = userId;
        this.matchId = matchId;
        this.requestedSeatIds = requestedSeatIds;
    }
}
